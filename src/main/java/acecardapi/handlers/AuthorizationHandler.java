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

import java.lang.reflect.Array;
import java.util.ArrayList;

public class AuthorizationHandler implements Handler<RoutingContext> {

  private String[] roles;
  private boolean isAuthorized = false;

  public AuthorizationHandler(String[] authRoles) {
    this.roles = authRoles;
  }

  @Override
  public void handle(RoutingContext context) {

    for (int i = 0; i < roles.length; i++) {

      if (isAuthorized) {
        break;
      }
      context.user().isAuthorized(roles[i], authRes -> {
        if (authRes.succeeded()) {
          isAuthorized = authRes.result();
        }
      });
    }
    if(!isAuthorized) {
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
