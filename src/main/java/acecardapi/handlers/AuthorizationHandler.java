/*
 * Copyright 2019 Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.apierrors.PermissionsViolation;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import java.util.concurrent.atomic.AtomicBoolean;

public class AuthorizationHandler implements Handler<RoutingContext> {

  private String[] roles;

  public AuthorizationHandler(String[] authRoles) {
    this.roles = authRoles;
  }

  /**
   Function which handles authorization to endpoints based on a provided list
   @param context contains information about the request
   @return void
   */
  @Override
  public void handle(RoutingContext context) {

    // Need to use an atomic boolean because it we are going to need to change it inside a async call later.
    AtomicBoolean isAuthorized = new AtomicBoolean(false);

    for (int i = 0; i < roles.length; i++) {

      if (isAuthorized.get()) {
        break;
      }
      context.user().isAuthorized(roles[i], authRes -> {
        if (authRes.succeeded()) {
          isAuthorized.set(authRes.result());
        }
      });
    }

    if(!isAuthorized.get()) {
      // Unauthorized request

      PermissionsViolation error = new PermissionsViolation("You do not have enough permissions to access this endpoint.");

      context.response()
        .setStatusCode(403)
        .putHeader("content-type", "application/json; charset=utf-8")
        .putHeader("Cache-Control", "no-store, no-cache")
        .putHeader("X-Content-Type-Options", "nosniff")
        .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
        .putHeader("X-Download-Options", "noopen")
        .putHeader("X-XSS-Protection", "1; mode=block")
        .putHeader("X-FRAME-OPTIONS", "DENY")
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(error.errorJson()));

    } else {
      // Authorized request

      context.next();

    }
  }
}
