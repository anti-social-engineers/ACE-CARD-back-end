/*

  Copyright 2019 Aaron Beetstra, team Anti-Social Engineers

  MainVerticle class for logic.

 */

package acecardapi;

import acecardapi.auth.IReactiveAuth;
import acecardapi.auth.PBKDF2Strategy;
import acecardapi.auth.ReactiveAuth;
import acecardapi.handlers.LoginHandler;
import acecardapi.handlers.RegistrationHandler;
import acecardapi.handlers.UserHandler;
import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    // Create the router
    final Router router = Router.router(vertx);

    // Create the database client
    PgPoolOptions options = new PgPoolOptions()
      .setPort(config().getInteger("db.port", 5432))
      .setHost(config().getString("db.host", "127.0.0.1"))
      .setDatabase(config().getString("db.name", "asecard"))
      .setUser(config().getString("db.user", "asecard"))
      .setPassword(config().getString("db.pass", "asecard"))
      .setMaxSize(config().getInteger("db.maxc", 5));

    // Create the pooled client
    PgPool dbClient = PgClient.pool(vertx, options);


    // Create the authentication provider
    ReactiveAuth authProvider = IReactiveAuth.create(vertx, dbClient);
    authProvider.setAuthenticationQuery("SELECT id, password, password_salt FROM users WHERE email = $1");
    authProvider.setHashStrategy(new PBKDF2Strategy(vertx));


    /*
    Setup Redis
     */
    RedisClient redisClient = RedisClient.create(vertx,
      new RedisOptions().setHost("127.0.0.1"));

    redisClient.set("testKeyOne", "ARANDOMVALUE", r -> {
      if (r.succeeded()) {
        System.out.println("KeyStored");
        redisClient.get("testKeyOne", r2 -> {
          if (r2.succeeded()) {
            System.out.println(r2.result());
          }
          else {
            System.out.println(r2.cause().toString());
          }
        });
      }
      else {
        System.out.println(r.cause().toString());
        System.out.println("FAILURE");
      }
    });


    /*
    Setup JWT
     */
    JWTAuth jwtProvider = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("HS256")
        .setPublicKey(config().getString("jwt.publickey", "keyboard cat"))
        .setSymmetric(true)));


    /*
    Handlers
     */

    // UserHandler
    UserHandler userHandler = new UserHandler(dbClient,config());
    // RegistrationHandler
    RegistrationHandler registrationHandler = new RegistrationHandler(dbClient, config(), authProvider);
    // LoginHandler
    LoginHandler loginHandler = new LoginHandler(dbClient, config(), authProvider, jwtProvider);


    /*
    Routes
     */

    // Protected apis (All these endpoints require JWT)
    // TODO: Beautify?
    router.route("/api/users/*").handler(JWTAuthHandler.create(jwtProvider));

    //// Handle register/login endpoints ////
    router.route("/api/register").handler(BodyHandler.create());
    router.route("/api/login").handler(BodyHandler.create());
    router.route("/api/register").handler(registrationHandler::registerUser);
    router.route("/api/login").handler(loginHandler::login);

    //// User Management ////
    router.get("/api/users").handler(userHandler::getUsers);


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
