/*

  Copyright 2019 Aaron Beetstra, team Anti-Social Engineers

  MainVerticle class for logic.

 */

package acecardapi;

import acecardapi.auth.IReactiveAuth;
import acecardapi.auth.PBKDF2Strategy;
import acecardapi.auth.ReactiveAuth;
import acecardapi.handlers.LoginHandler;
import acecardapi.handlers.UserHandler;
import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CSRFHandler;

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
    authProvider.setAuthenticationQuery("SELECT password, password_salt FROM users WHERE email = $1");
    authProvider.setHashStrategy(new PBKDF2Strategy(vertx));

    /*

    Handlers

     */

    // UserHandler
    UserHandler userHandler = new UserHandler(dbClient, authProvider);
    // LoginHandler
    LoginHandler loginHandler = new LoginHandler(dbClient, authProvider);


    /*

    Routes

     */

    // Enable request body reading
    router.route("/api/users/*").handler(BodyHandler.create());

    // Get all users
    router.get("/api/users").handler(userHandler::getUsers);
    // Create a user
    router.post("/api/users").handler(userHandler::createUser);

    // Enable request body reading
    router.route("/api/login/*").handler(BodyHandler.create());
    // Login request
    router.post("/api/login").handler(loginHandler::login);

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
