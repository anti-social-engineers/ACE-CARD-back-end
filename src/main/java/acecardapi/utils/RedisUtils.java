/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.utils;

import io.sentry.Sentry;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;

import java.time.OffsetDateTime;
import java.util.UUID;

import static acecardapi.utils.StringUtilities.doubleToCurrencyString;

public class RedisUtils {

  public static Redis backEndRedis;
  public static Redis frontEndRedis;
  private static int MAX_REDIS_RECONNECT_ATTEMPTS = 30;

  public static void attemptReconnectBackendRedis(Vertx vertx, int retry, RedisOptions redisOptions){
    System.out.println("REDIS DOWN, RECONNECTING...");

    if (retry > MAX_REDIS_RECONNECT_ATTEMPTS) {
      // CONTACT DEVELOPERS
      Sentry.capture("Warning! Backend Redis is down! Attempted to reconnect, but failed " + MAX_REDIS_RECONNECT_ATTEMPTS + " times.");
    } else {

      // Wait 30 Seconds before attempting a reconnect
      long backoff = (long) 30000;
      Sentry.capture("Warning! Backend Redis is down! Attempting to reconnect, attempt: " + retry);
      vertx.setTimer(backoff, timer -> {
          Redis.createClient(vertx, redisOptions).connect(connectRes -> {
            if (connectRes.succeeded()) {
              RedisUtils.backEndRedis = connectRes.result();
              RedisUtils.backEndRedis.exceptionHandler(e -> attemptReconnectBackendRedis(vertx, 0, redisOptions));
            } else {
              attemptReconnectBackendRedis(vertx, retry + 1, redisOptions);
            }
          });
        });
    }
  }

  public static void realTimeRedisLPUSH(RedisClient redisClient, UUID userId, String type, Double amount, Double newCredits, OffsetDateTime datetime, Handler<AsyncResult<Boolean>> resultHandler) {

    try {

      JsonObject jsonObject = new JsonObject()
        .put("type", "notification")
        .put("name",  type)
        .put("amount", doubleToCurrencyString(amount))
        .put("updated_balance", doubleToCurrencyString(newCredits))
        .put("datetime", datetime.toString()
        );

      redisClient.lpush(userId.toString(), jsonObject.toString(), res -> {

        if (res.succeeded()) {
          resultHandler.handle(Future.succeededFuture(true));
        } else {
          resultHandler.handle(Future.failedFuture(res.cause()));
        }

      });
    } catch (Exception e) {
      System.out.println(e.toString());
      resultHandler.handle(Future.failedFuture(e));
    }
  }
}
