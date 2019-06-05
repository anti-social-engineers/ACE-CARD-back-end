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

import java.util.concurrent.atomic.AtomicBoolean;

public class AuthorizationHandler implements Handler<RoutingContext> {

  private String[] roles;

  public AuthorizationHandler(String[] authRoles) {
    this.roles = authRoles;
  }

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

      context.response()
        .putHeader("content-type", "application/json; charset=utf-8")
        .setStatusCode(403)
        .end();

    } else {
      // Authorized request

      context.next();

    }
  }
}
