/*

  Copyright 2019 Aaron Beetstra, team Anti-Social Engineers

  MainVerticle class for logic.

 */

package acecardapi;

import acecardapi.auth.IReactiveAuth;
import acecardapi.auth.PBKDF2Strategy;
import acecardapi.auth.ReactiveAuth;
import acecardapi.handlers.*;
import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

import java.util.HashSet;
import java.util.Set;

public class MainVerticle extends AbstractVerticle {

  private static final int KB = 1024;
  private static final int MB = 1024 * KB;

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    // Create the router
    final Router router = Router.router(vertx);

    // Create the database client
    PgPoolOptions options = new PgPoolOptions()
      .setPort(config().getInteger("db.port", 5432))
      .setHost(config().getString("db.host", "127.0.0.1"))
      .setDatabase(config().getString("db.name", "ase_acecard_test_database"))
      .setUser(config().getString("db.user", "acecard"))
      .setPassword(config().getString("db.pass", "acecard"))
      .setMaxSize(config().getInteger("db.maxc", 5));

    // Create the pooled client
    PgPool dbClient = PgClient.pool(vertx, options);


    /*
    Setup Email Client
     */
    MailConfig config = new MailConfig();
    config.setHostname(config().getString("mail.host", "'"));
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
    Setup Redis
     */
    RedisClient redisClient = RedisClient.create(vertx,
      new RedisOptions().setHost(config().getString("redis.host", "127.0.0.1")));


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
    RegistrationHandler registrationHandler = new RegistrationHandler(dbClient, config(), authProvider, redisClient, mailClient);
    // LoginHandler
    LoginHandler loginHandler = new LoginHandler(dbClient, config(), authProvider, jwtProvider);
    // ActivationHandler
    ActivationHandler activationHandler = new ActivationHandler(dbClient, config(), redisClient);
    // CardHandler
    CardHandler cardHandler = new CardHandler(dbClient, config());
    // ClubHandler
    ClubHandler clubHandler = new ClubHandler(dbClient, config());

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

    // Protected apis (All these endpoints require JWT)
    // TODO: Beautify? - Cookie handler only on TokenHeader?
    router.route().handler(CookieHandler.create());
//    router.route().handler(new TokenHeaderHandler());
    JWTAuthHandler jwtAuthHandler = JWTAuthHandler.create(jwtProvider);
    router.route("/api/users/*").handler(jwtAuthHandler);
    router.route("/api/account/*").handler(jwtAuthHandler);
    router.route("/static/*").handler(jwtAuthHandler);
    router.route("/api/acecard/*").handler(jwtAuthHandler);
    router.route("/api/club/*").handler(jwtAuthHandler);

    //// Handle register/login endpoints ////
    router.route("/api/register").handler(BodyHandler.create(false));
    router.route("/api/login").handler(BodyHandler.create(false));
    router.post("/api/register").handler(registrationHandler::registerUser);
    router.post("/api/login").handler(loginHandler::login);
    router.get("/api/activate/:activationkey").handler(activationHandler::activateUser);

    //// User Management ////
    router.route("/api/users").handler(new AuthorizationHandler(new String[]{"sysop"}));
    router.get("/api/users").handler(userHandler::getUsers);

    //// Account information ////
    router.get("/api/account").handler(userHandler::getUserData);


    //// Club endpoints ////
    router.post("/api/club/scan").handler(BodyHandler.create(false));
    router.route("/api/club/scan").handler(new AuthorizationHandler(new String[]{"sysop", "club_employee"}));
    router.post("/api/club/scan").handler(clubHandler::scanCard);

    //// Ace Card ////
    router.post("/api/acecard").handler(BodyHandler.create()
      .setUploadsDirectory(config().getString("http.image_dir", "static/images/"))
      .setBodyLimit(config().getInteger("http.max_image_mb", 1) * MB));
    router.post("/api/acecard").handler(cardHandler::requestCard);
//    router.post("/api/acecard").handler(ctx -> {
//      MultiMap attributes = ctx.request().formAttributes();
//      // do something with the form data
//      Set<FileUpload> uploads = ctx.fileUploads();
//      for (FileUpload file : uploads
//           ) {
//        System.out.println(file.contentType());
//        System.out.println(file.size());
//        System.out.println(file.name());
//        System.out.println(file.fileName());
//        System.out.println(file.uploadedFileName());
//        System.out.println(file.contentType());
//      }
//      System.out.println(uploads);
//      System.out.println(attributes);
//
//      ctx.response().end();
//    });

    //// Serving profile image  ////
    router.route("/static/images/*").handler(new ProfileImageAuthorizationHandler(dbClient));
    router.route("/static/images/*").handler(StaticHandler.create().setWebRoot("static/images"));

    // HttpServer options

    if (config().getBoolean("http.ssl", false)) {
      PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
        .setKeyPath("ssl/privKey.pem")
        .setCertPath("ssl/fullchain.pem");

      HttpServerOptions httpServerOptions = new HttpServerOptions()
        .setSsl(true)
        .setKeyCertOptions(pemKeyCertOptions)
        .removeEnabledSecureTransportProtocol("TLSv1")
        .removeEnabledSecureTransportProtocol("TLSv1.1");

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
