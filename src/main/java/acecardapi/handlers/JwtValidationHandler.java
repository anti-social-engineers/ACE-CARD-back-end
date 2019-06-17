package acecardapi.handlers;

import acecardapi.utils.RedisUtils;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;
import io.vertx.redis.client.RedisAPI;

import java.util.Arrays;

public class JwtValidationHandler implements Handler<RoutingContext> {

  public JwtValidationHandler() {
  }

  @Override
  public void handle(RoutingContext context) {

    RedisAPI redisClient = RedisAPI.api(RedisUtils.backEndRedis);

   redisClient.exists(Arrays.asList("invalidated:" + context.request().getHeader("Authorization")) ,res -> {

     if(res.succeeded()) {

       if(res.result().toInteger() == 1) {
         context.response()
           .setStatusCode(401)
           .putHeader("content-type", "application/json; charset=utf-8")
           .putHeader("Cache-Control", "no-store, no-cache")
           .putHeader("X-Content-Type-Options", "nosniff")
           .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
           .putHeader("X-Download-Options", "noopen")
           .putHeader("X-XSS-Protection", "1; mode=block")
           .putHeader("X-FRAME-OPTIONS", "DENY")
           .putHeader("content-type", "application/json; charset=utf-8")
           .end();
       }
       else {
         context.next();
       }
     } else {
       context.next();
     }
   });
  }
}
