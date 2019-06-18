/*

  Copyright 2019 Aaron Beetstra, team Anti-Social Engineers

  MainVerticle class for logic.

 */

package acecardapi;

import acecardapi.auth.IReactiveAuth;
import acecardapi.auth.PBKDF2Strategy;
import acecardapi.auth.ReactiveAuth;
import acecardapi.handlers.*;
import acecardapi.utils.RedisUtils;
import com.stripe.Stripe;
import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.sentry.Sentry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;

import java.util.HashSet;
import java.util.Set;

import static acecardapi.utils.RedisUtils.attemptReconnectRedis;

public class MainVerticle extends AbstractVerticle {

  private static final int KB = 1024;
  private static final int MB = 1024 * KB;

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    /*
    Setup Sentry for Debugging
     */
    if (config().getBoolean("debug.enabled", false)) {
      System.out.println("Sentry enabled!");
      String dsn = config().getString("debug.dsn");
      Sentry.init(dsn);
    }

    /*
    Setup Sentry for Debugging
    */
    if (config().getBoolean("stripe.enabled", false)) {
      Stripe.apiKey = config().getString("stripe.apikey");
    }

    // Create the router
    final Router router = Router.router(vertx);

    // Create the database client
    PgPoolOptions options = new PgPoolOptions()
      .setPort(config().getInteger("db.port", 5432))
      .setHost(config().getString("db.host", "127.0.0.1"))
      .setDatabase(config().getString("db.name", "ase_acecard_test_database"))
      .setUser(config().getString("db.user", "acecard"))
      .setPassword(config().getString("db.pass", "acecard"))
      .setMaxSize(config().getInteger("db.maxc", 1))
      .setIdleTimeout(config().getInteger("db.idle_timeout", 0))
      .setTcpKeepAlive(true);

    // Create the pooled client
    PgPool dbClient = PgClient.pool(vertx, options);


    /*
    Setup Email Client
     */
    MailConfig config = new MailConfig();
    config.setHostname(config().getString("mail.host", ""));
    config.setPort(config().getInteger("mail.port", 587));
    config.setStarttls(StartTLSOptions.REQUIRED);
    config.setUsername(config().getString("mail.user", ""));
    config.setPassword(config().getString("mail.pass", ""));
    MailClient mailClient = MailClient.createNonShared(vertx, config);


    // Create the authentication provider
    ReactiveAuth authProvider = IReactiveAuth.create(vertx, dbClient);
    authProvider.setAuthenticationQuery("SELECT id, password, password_salt FROM users WHERE email = $1");
    authProvider.setHashStrategy(new PBKDF2Strategy(vertx));


    /*
    Setup Redis for backend
     */

    //TODO SETTINGS
    Redis.createClient(vertx, new RedisOptions()).connect(connectRes -> {
      if (connectRes.succeeded()) {
        RedisUtils.backEndRedis = connectRes.result();
        RedisUtils.backEndRedis.exceptionHandler(e -> attemptReconnectRedis(vertx, 0, new RedisOptions(), true));
      } else {
        System.out.println("!!! Redis is down !!!");
      }
    });

    /*
    Setup Redis for frontend realtime notifcations
     */
    RedisOptions frontEndRedisOptions = new RedisOptions()
      .setEndpoint(SocketAddress.inetSocketAddress(config().getInteger("realtime.redis.port", 6379), config().getString("realtime.redis.host", "127.0.0.1")))
      .setPassword(config().getString("realtime.auth", null));
    Redis.createClient(vertx, frontEndRedisOptions).connect(connectRes -> {
      if (connectRes.succeeded()) {
        RedisUtils.frontEndRedis = connectRes.result();
        RedisUtils.frontEndRedis.exceptionHandler(e -> attemptReconnectRedis(vertx, 0, frontEndRedisOptions, false));
      } else {
        System.out.println("!!! [FRONTEND] Redis is down !!!");
      }
    });

    /*
    Setup JWT
     */
    JWTAuth jwtProvider = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setPublicKey(config().getString("jwt.publickey", "AVeryBadPublicKey<3"))
        .setSymmetric(true)));


    /*
    Handlers
     */

    // UserHandler
    UserHandler userHandler = new UserHandler(dbClient,config());
    // RegistrationHandler
    RegistrationHandler registrationHandler = new RegistrationHandler(dbClient, config(), authProvider, mailClient);
    // LoginHandler
    LoginHandler loginHandler = new LoginHandler(dbClient, config(), authProvider, jwtProvider);
    // ActivationHandler
    ActivationHandler activationHandler = new ActivationHandler(dbClient, config());
    // CardHandler
    CardHandler cardHandler = new CardHandler(dbClient, config(), authProvider);
    // ClubHandler
    ClubHandler clubHandler = new ClubHandler(dbClient, config(), authProvider);
    // ClubHandler
    DepositHandler depositHandler = new DepositHandler(dbClient, config());
    //LogoutHandler
    LogoutHandler logoutHandler = new LogoutHandler(dbClient, config());
    // JwtValidationHandler (Check if JWT has not already been logged out)
    JwtValidationHandler jwtValidationHandler = new JwtValidationHandler();

    /*
    Routes
     */

    // CORS
    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add("x-requested-with");
    allowedHeaders.add("Access-Control-Allow-Origin");
    allowedHeaders.add("origin");
    allowedHeaders.add("Content-Type");
    allowedHeaders.add("accept");
    allowedHeaders.add("Authorization");

    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);

    router.route().handler(CorsHandler.create("*")
      .allowedHeaders(allowedHeaders)
      .allowedMethods(allowedMethods));

    JWTAuthHandler jwtAuthHandler = JWTAuthHandler.create(jwtProvider);

    //Setup default handlers, order should be: jwtAuthHandler --> BodyHandler --> ANY OTHER HANDLER
    router.route("/api/users/*").handler(jwtAuthHandler);
    router.route("/api/users/*").handler(jwtValidationHandler);
    router.route("/api/account/*").handler(jwtAuthHandler);
    router.route("/api/account/*").handler(jwtValidationHandler);
    router.route("/static/*").handler(jwtAuthHandler);
    router.route("/static/*").handler(jwtValidationHandler);
    router.route("/api/acecard").handler(jwtAuthHandler);
    router.post("/api/acecard").handler(BodyHandler.create()
      .setUploadsDirectory(config().getString("http.temp_dir", "static/temp/"))
      .setBodyLimit(config().getInteger("http.max_image_mb", 1) * MB)
      .setDeleteUploadedFilesOnEnd(true));
    router.route("/api/acecard").handler(jwtValidationHandler);
    router.route("/api/club/*").handler(jwtAuthHandler);
    router.route("/api/club/*").handler(BodyHandler.create(false));
    router.route("/api/club/*").handler(jwtValidationHandler);
    router.route("/api/administration/*").handler(jwtAuthHandler);
    router.post("/api/administration/link").handler(BodyHandler.create(false));
    router.route("/api/administration/*").handler(jwtValidationHandler);
    router.route("/api/deposits/*").handler(jwtAuthHandler);
    router.post("/api/deposits/create").handler(BodyHandler.create(false));
    router.route("/api/deposits/*").handler(jwtValidationHandler);
    router.route("/api/logout/").handler(jwtAuthHandler);
    router.route("/api/logout/").handler(jwtValidationHandler);

    //// Handle register/login endpoints ////
    router.route("/api/register").handler(BodyHandler.create(false));
    router.route("/api/login").handler(BodyHandler.create(false));
    router.post("/api/register").handler(registrationHandler::registerUser);
    router.post("/api/login").handler(loginHandler::login);
    router.get("/api/activate/:activationkey").handler(activationHandler::activateUser);

    //// logout endpoints ////
    router.post("/api/logout").handler(logoutHandler::logout);

    //// User Management ////
    router.route("/api/users").handler(new AuthorizationHandler(new String[]{"sysop"}));
    router.get("/api/users").handler(userHandler::getUsers);

    //// Account & User information ////
    router.get("/api/account").handler(userHandler::getUserData);
    router.get("/api/account/payments/:sorting").handler(userHandler::userPayments);
    router.get("/api/account/payments/:sorting/:cursor").handler(userHandler::userPayments);
    router.get("/api/account/deposits/:sorting").handler(userHandler::userDeposits);
    router.get("/api/account/deposits/:sorting/:cursor").handler(userHandler::userDeposits);
    router.get("/api/account/graphs/payments").handler(userHandler::paymentGraphData);

    //// Club endpoints ////
    router.route("/api/club/scan").handler(new AuthorizationHandler(new String[]{"sysop", "club_employee"}));
    router.post("/api/club/scan").handler(clubHandler::scanCard);
    router.route("/api/club/payment").handler(new AuthorizationHandler(new String[]{"club_employee"}));
    router.route("/api/club/payment").handler(new ClubAccessHandler());
    router.post("/api/club/payment").handler(clubHandler::cardPayment);

    //// Ace Card ////
    router.post("/api/acecard").handler(cardHandler::requestCard);

    //// Serving profile image  ////
    router.route("/static/images/*").handler(new ProfileImageAuthorizationHandler(dbClient));
    router.route("/static/images/*").handler(StaticHandler.create().setWebRoot("static/images"));

    //// Admin Endpoints ////
    router.route("/api/administration/*").handler(new AuthorizationHandler(new String[]{"sysop"}));
    router.get("/api/administration/openrequests").handler(cardHandler::requestRequestedCards);
    router.post("/api/administration/link").handler(cardHandler::linkCardUser);


    //// Payment Endpoints ////
    router.post("/api/deposits/create").handler(depositHandler::stripeSource);

    //// Payment Webhooks ////
    router.post("/api/webhooks/deposits").handler(BodyHandler.create(false));
    router.post("/api/webhooks/deposits").handler(new StripeSignatureHandler(config().getString("stripe.source_chargeable_secret", "")));
    router.post("/api/webhooks/deposits").handler(depositHandler::chargeableSourceWebhook);
    router.post("/api/webhooks/deposits/succeeded").handler(BodyHandler.create(false));
    router.post("/api/webhooks/deposits/succeeded").handler(new StripeSignatureHandler(config().getString("stripe.charge_succeeded_secret", "")));
    router.post("/api/webhooks/deposits/succeeded").handler(depositHandler::succeededChargeWebhook);
    router.post("/api/webhooks/deposits/failed").handler(BodyHandler.create(false));
    router.post("/api/webhooks/deposits/failed").handler(new StripeSignatureHandler(config().getString("stripe.source_failed_secret", "")));
    router.post("/api/webhooks/deposits/failed").handler(depositHandler::failedSourceWebhook);


    // HttpServer options

    if (config().getBoolean("http.ssl", false)) {
      PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
        .setKeyPath("ssl/privKey.pem")
        .setCertPath("ssl/fullchain.pem");

      HttpServerOptions httpServerOptions = new HttpServerOptions()
        .setSsl(true)
        .setKeyCertOptions(pemKeyCertOptions)
        .removeEnabledSecureTransportProtocol("TLSv1")
        .removeEnabledSecureTransportProtocol("TLSv1.1")
        .addEnabledCipherSuite("ECDHE-RSA-AES128-GCM-SHA256")
        .addEnabledCipherSuite("ECDHE-ECDSA-AES128-GCM-SHA256")
        .addEnabledCipherSuite("ECDHE-RSA-AES256-GCM-SHA384")
        .addEnabledCipherSuite("CDHE-ECDSA-AES256-GCM-SHA384");

      // Create the HttpServer
      vertx.createHttpServer(httpServerOptions).requestHandler(router).listen(
        config().getInteger("http.port", 443),
        result -> {
          if (result.succeeded())
            startFuture.complete();
          else
            startFuture.fail(result.cause());
        }
      );
    } else {

      // Create the HttpServer
      vertx.createHttpServer().requestHandler(router).listen(
        config().getInteger("http.port", 8888),
        result -> {
          if (result.succeeded())
            startFuture.complete();
          else
            startFuture.fail(result.cause());
        }
      );
    }

  }
}
