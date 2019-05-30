/*
 * Copyright 2019 Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class AuthorizationHandler implements Handler<RoutingContext> {

  private String role;

  public AuthorizationHandler(String authRole) {
    this.role = authRole;
  }

  @Override
  public void handle(RoutingContext context) {
    context.user().isAuthorized(this.role, authRes -> {
      if (authRes.succeeded()) {
        boolean isSysop = authRes.result();

        if (isSysop) {
          context.next();
        } else {

          // Unauthorized request

          context.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .setStatusCode(403)
            .end();

        }
      }
    });
  }
}
