package acecardapi.handlers;

import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.Row;
import io.reactiverse.pgclient.Tuple;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

public class ProfileImageAuthorizationHandler implements Handler<RoutingContext> {

  PgPool dbClient;

  public ProfileImageAuthorizationHandler(PgPool dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void handle(RoutingContext context) {
    isRole(context, "sysop", sysopRes -> {
      if (sysopRes.succeeded()) {
        if (sysopRes.result()) {
          context.next();
        } else {
          isRole(context, "club_employee", clubRes -> {
            if (clubRes.succeeded()) {
              if (clubRes.result()) {
                context.next();
              } else {

                // The role must be 'user', users may only request their own image!
                // To remove an additional db call we will first check if the image is present inside the jwt token.
                String requestedFileUUID = context.request().path().substring(context.request().path().lastIndexOf("/") + 1);

                if (context.user().principal().getValue("profile_image").equals(requestedFileUUID)) {
                  context.next();
                } else {
                  dbClient.preparedQuery("SELECT image_id FROM users WHERE id=$1", Tuple.of(UUID.fromString(context.user().principal().getString("sub"))), res -> {
                    if (res.succeeded()) {

                      Row row = res.result().iterator().next();
                      UUID imageId = row.getUUID("image_id");
                      if (imageId != null && imageId.toString().equals(requestedFileUUID)) {

                        // They are requesting their own profile image
                        context.next();

                      } else {

                        // They are requesting an image they do not have access to

                        context.response()
                          .putHeader("content-type", "application/json; charset=utf-8")
                          .putHeader("Cache-Control", "no-store, no-cache")
                          .putHeader("X-Content-Type-Options", "nosniff")
                          .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
                          .putHeader("X-Download-Options", "noopen")
                          .putHeader("X-XSS-Protection", "1; mode=block")
                          .putHeader("X-FRAME-OPTIONS", "DENY")
                          .setStatusCode(403)
                          .end();
                      }

                    } else {
                      raise500(context);
                    }
                  });
                }
              }
            } else {
              raise500(context);
            }
          });
        }
      } else {
        raise500(context);
      }
    });
  }

  private void isRole(RoutingContext context, String role, Handler<AsyncResult<Boolean>> resultHandler) {
        context.user().isAuthorized(role, authRes -> {
      if (authRes.succeeded()) {
        boolean isSysop = authRes.result();

        if (isSysop) {
          resultHandler.handle(Future.succeededFuture(true));
        } else {
          resultHandler.handle(Future.succeededFuture(false));
        }
      }
      else {
        resultHandler.handle(Future.failedFuture(""));
      }
    });

  }

  private void raise500(RoutingContext context) {
    context.response()
      .setStatusCode(500)
      .putHeader("Cache-Control", "no-store, no-cache")
      .putHeader("X-Content-Type-Options", "nosniff")
      .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
      .putHeader("X-Download-Options", "noopen")
      .putHeader("X-XSS-Protection", "1; mode=block")
      .putHeader("X-FRAME-OPTIONS", "DENY")
      .putHeader("content-type", "application/json; charset=utf-8")
      .end();
  }
}
