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

public class TokenHeaderHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {
    // Check if there already is an authorization header
    if (context.request().headers().contains("Authorization")) {
      context.next();
    } else {
      // Check if there is a Authorization cookie, if so add it as a header
      var cookie = context.getCookie("Authorization");
      if (cookie != null) {
        // The Auth header is not set, but a cookie is present. Add the cookie value as Authorization header.
        context.request().headers().add("Authorization", String.format("Bearer %s", cookie.getValue()));
        context.next();
      } else {
        context.next();
      }
    }
  }
}
