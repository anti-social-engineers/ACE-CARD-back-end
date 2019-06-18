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
import acecardapi.utils.RedisUtils;
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
import io.vertx.redis.client.RedisAPI;

import java.util.Arrays;
import java.util.UUID;

public class ActivationHandler extends AbstractCustomHandler {

  public ActivationHandler(PgPool dbClient, JsonObject config) {
    super(dbClient, config);
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


      RedisAPI redisClient = RedisAPI.api(RedisUtils.backEndRedis);

      redisClient.get(activationKey, res -> {
        if (res.succeeded()) {

          String stringId = null;
          if (res.result() != null) {
            stringId = res.result().toString();
          }

          if (stringId == null) {
            raise404(context, new ParameterNotFoundViolation("activationkey"));
          } else {

            UUID userId = UUID.fromString(stringId);

            activateDatabaseUser(userId, updateResult -> {
              if (updateResult.succeeded()) {

                // Account updated to have activated email
                raise200(context);

                // Delete the key afterwards
                redisClient.del(Arrays.asList(activationKey),unlinkRes -> {
                  if (unlinkRes.succeeded()) {
                    System.out.println("Unlinking success");
                  } else {
                    System.out.println("Unlinking failed, matters not: it will expire...");
                  }
                });

              } else {
                raise500(context, updateResult.cause());
              }
            });
          }

        } else {

          // Redis error
          raise500(context, res.cause());
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
