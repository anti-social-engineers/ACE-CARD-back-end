/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;

import java.time.OffsetDateTime;
import java.util.UUID;

import static acecardapi.utils.StringUtilities.doubleToCurrencyString;

public class RedisUtils {

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
