package acecardapi.handlers;

import acecardapi.utils.RedisUtils;
import io.reactiverse.pgclient.PgPool;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;

import java.util.Arrays;

import static acecardapi.utils.RedisUtils.getRedisConnection;

public class LogoutHandler extends AbstractCustomHandler {

  public LogoutHandler(PgPool dbClient, JsonObject config) {
    super(dbClient, config);
  }

  public void logout(RoutingContext context)
  {
    String jwt = context.request().getHeader("Authorization");
    String redisKey = "invalidated:" + jwt;

    getRedisConnection(true, redisConnectionRes -> {
      if (redisConnectionRes.succeeded()) {

        Redis redisConnection = redisConnectionRes.result();

        RedisAPI redisClient = RedisAPI.api(redisConnection);

        redisClient.set(Arrays.asList(redisKey, "logged_out"),res -> {
          if (res.succeeded()) {
            redisClient.expire(redisKey, config.getLong("jwt.exptime",  3600L).toString(), expireRedisRes -> {
              redisConnection.close();
            });
          } else {
            redisConnection.close();
          }
        });
      }
    });
    raise200(context);
  }
}
