package acecardapi.handlers;

import io.reactiverse.pgclient.PgPool;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;

public class LogoutHandler extends AbstractCustomHandler {

  private RedisClient redisClient;

  public LogoutHandler(PgPool dbClient, JsonObject config, RedisClient redisClient) {
    super(dbClient, config);
    this.redisClient = redisClient;
  }

  public void logout(RoutingContext context)
  {
    String jwt = context.request().getHeader("Authorization");
    String redisKey = "invalidated:" + jwt;

    redisClient.set(redisKey, "", res -> {
      if (res.succeeded()) {
        redisClient.expire(redisKey, config.getLong("jwt.exptime",  3600L), expireRedisRes -> {
        });
      }
    });
    raise200(context);
  }
}
