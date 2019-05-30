package acecardapi.handlers;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ProfileImageAuthorizationHandler implements Handler<RoutingContext> {
  @Override
  public void handle(RoutingContext context) {
    isRole(context, "sysop", sysopRes -> {
      if (sysopRes.succeeded()) {
        if (sysopRes.result()) {
          context.next();
        } else {
          // TODO: CLUB ROLE
          isRole(context, "someclubrole", clubRes -> {
            if (clubRes.succeeded()) {
              if (clubRes.result()) {
                context.response().setStatusCode(100).end("I have not yet been implemented... (Club requesting img.)");
              } else {
                // The role must be 'user', users may only request their own image!
                String requestedFileWithExtension = context.request().path().substring(context.request().path().lastIndexOf("/") + 1);

                if(requestedFileWithExtension.contains(".")) {
                  String requestFile = requestedFileWithExtension.substring(0, requestedFileWithExtension.lastIndexOf('.'));

                  if (requestFile.equals(context.user().principal().getString("sub"))) {
                    context.next();
                  } else {
                    // Unauthorized - Requesting a file which does not have their ID
                    context.response()
                      .setStatusCode(403)
                      .putHeader("content-type", "application/json; charset=utf-8")
                      .end();
                  }

                } else {
                  // A non-legit file was requested, raise 400.
                  context.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end();
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
      .putHeader("content-type", "application/json; charset=utf-8")
      .end();
  }
}
