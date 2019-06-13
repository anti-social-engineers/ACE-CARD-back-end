/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.apierrors.ParameterNotFoundViolation;
import acecardapi.apierrors.PathParameterViolation;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgRowSet;
import io.reactiverse.pgclient.Tuple;
import io.sentry.Sentry;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;

import java.util.UUID;

public class ActivationHandler extends AbstractCustomHandler {

  private RedisClient redisClient;

  public ActivationHandler(PgPool dbClient, JsonObject config, RedisClient redisClient) {
    super(dbClient, config);
    this.redisClient = redisClient;
  }


  /**
   Function which handles activating an account
   @param context contains information about the request
   @return void
   */
  public void activateUser(RoutingContext context) {

    String activationKey = context.request().getParam("activationkey");

    // Check if the key is the correct length
    if (activationKey.length() != 32) {
      context.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(new PathParameterViolation("activationkey").errorJson()));

    } else {


      redisClient.get(activationKey, res -> {
        if (res.succeeded()) {

          String stringId = res.result();

          if (stringId == null) {
            context.response()
              .setStatusCode(404)
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(Json.encodePrettily(new ParameterNotFoundViolation("activationkey").errorJson()));
          } else {

            UUID userId = UUID.fromString(res.result());

            activateDatabaseUser(userId, updateResult -> {
              if (updateResult.succeeded()) {

                // Account updated to have activated email
                context.response()
                  .setStatusCode(200)
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end();

                // Delete the key afterwards
                redisClient.unlink(activationKey, unlinkRes -> {
                  if (unlinkRes.succeeded()) {
                    System.out.println("Unlinking success");
                  } else {
                    System.out.println("Unlinking failed, matters not: it will expire...");
                  }
                });

              } else {
                context.response()
                  .setStatusCode(500)
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end();

                if (config.getBoolean("debug.enabled", false))
                  Sentry.capture(updateResult.cause());
              }
            });
          }

        } else {

          // Redis down

          context.response()
            .setStatusCode(500)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end();

          if (config.getBoolean("debug.enabled", false))
            Sentry.capture(res.cause());
        }
      });
    }

  }

  /**
   Function which handles activating an user in the database
   @param userId the uuid of the user
   @param resultHandler handler for async processing
   @return void
   */
  private void activateDatabaseUser(UUID userId, Handler<AsyncResult<UUID>> resultHandler) {

    dbClient.preparedQuery("UPDATE users SET is_email_verified = true WHERE id = $1", Tuple.of(userId), res -> {
      if (res.succeeded()) {

        PgRowSet result = res.result();

        System.out.println(result);

        if (result.rowCount() != 1) {

          // 0 or more than 1 result returned

          resultHandler.handle(Future.failedFuture(""));

        } else {

            resultHandler.handle(Future.succeededFuture(userId));

        }

      } else {

        // Db down
        resultHandler.handle(Future.failedFuture(""));
      }
    });

  }

}
