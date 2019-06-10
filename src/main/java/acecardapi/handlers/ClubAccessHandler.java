/*
 * Copyright 2019 Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT license.
 *
 */


package acecardapi.handlers;

import  acecardapi.apierrors.ParameterNotFoundViolation;
import acecardapi.apierrors.PermissionsViolation;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import static acecardapi.utils.RequestUtilities.attributesCheckJsonObject;

public class ClubAccessHandler implements Handler<RoutingContext> {

  private String[] requiredAttributes = new String[]{"club_id"};

  @Override
  public void handle(RoutingContext context) {
    // Check the club id is valid (e.g. is the user making the request allowed to access club resources)

    attributesCheckJsonObject(context.getBodyAsJson(), requiredAttributes, attCheckRes -> {
      if (attCheckRes.succeeded()) {

        String clubId = context.getBodyAsJson().getString("club_id");

        if (context.user().principal().getJsonArray("clubs").contains(clubId)) {
          context.next();
        } else {
          // User is making a request for a club to which they do not have access.

          PermissionsViolation error = new PermissionsViolation("You do not have access to this club.");

          context.response()
            .setStatusCode(403)
            .putHeader("Cache-Control", "no-store, no-cache")
            .putHeader("X-Content-Type-Options", "nosniff")
            .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
            .putHeader("X-Download-Options", "noopen")
            .putHeader("X-XSS-Protection", "1; mode=block")
            .putHeader("X-FRAME-OPTIONS", "DENY")
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(error.errorJson()));
        }

      } else {
        ParameterNotFoundViolation error = new ParameterNotFoundViolation(attCheckRes.cause().getMessage());
        context.response()
          .setStatusCode(422)
          .putHeader("Cache-Control", "no-store, no-cache")
          .putHeader("X-Content-Type-Options", "nosniff")
          .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
          .putHeader("X-Download-Options", "noopen")
          .putHeader("X-XSS-Protection", "1; mode=block")
          .putHeader("X-FRAME-OPTIONS", "DENY")
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(error.errorJson()));
      }
    });
  }
}
