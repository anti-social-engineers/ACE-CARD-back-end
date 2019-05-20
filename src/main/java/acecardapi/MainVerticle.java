/*

  Copyright 2019 Aaron Beetstra, team Anti-Social Engineers

  MainVerticle class for logic.

 */

package acecardapi;

import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.reactiverse.pgclient.PgRowSet;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

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


//    // Test DB
//    dbClient.query("SELECT * FROM users", ar -> {
//      if (ar.succeeded()) {
//        PgRowSet result = ar.result();
//        System.out.println("Got " + result.size() + " rows ");
//      } else {
//        System.out.println("Failure: " + ar.cause().getMessage());
//      }
//
//      // Now close the pool
//      dbClient.close();
//    });

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
