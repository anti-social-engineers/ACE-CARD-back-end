/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.apierrors.ApiError;
import io.reactiverse.pgclient.PgPool;
import io.sentry.Sentry;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

abstract class AbstractCustomHandler {

  PgPool dbClient;
  JsonObject config;

  AbstractCustomHandler(PgPool dbClient, JsonObject config) {
    this.dbClient = dbClient;
    this.config = config;
  }

  void raise200(RoutingContext context, JsonArray jsonArray) {

    context.response()
      .setStatusCode(200)
      .putHeader("Cache-Control", "no-store, no-cache")
      .putHeader("X-Content-Type-Options", "nosniff")
      .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
      .putHeader("X-Download-Options", "noopen")
      .putHeader("X-XSS-Protection", "1; mode=block")
      .putHeader("X-FRAME-OPTIONS", "DENY")
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(jsonArray));
  }

  void raise200(RoutingContext context, JsonObject jsonObject) {

    context.response()
      .setStatusCode(200)
      .putHeader("Cache-Control", "no-store, no-cache")
      .putHeader("X-Content-Type-Options", "nosniff")
      .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
      .putHeader("X-Download-Options", "noopen")
      .putHeader("X-XSS-Protection", "1; mode=block")
      .putHeader("X-FRAME-OPTIONS", "DENY")
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(jsonObject));
  }

  void raise400(RoutingContext context, ApiError error) {

    context.response()
      .setStatusCode(400)
      .putHeader("Cache-Control", "no-store, no-cache")
      .putHeader("X-Content-Type-Options", "nosniff")
      .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
      .putHeader("X-Download-Options", "noopen")
      .putHeader("X-XSS-Protection", "1; mode=block")
      .putHeader("X-FRAME-OPTIONS", "DENY")
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(error.errorJson()));
  }

  void raise401(RoutingContext context, ApiError error) {

    context.response()
      .setStatusCode(401)
      .putHeader("Cache-Control", "no-store, no-cache")
      .putHeader("X-Content-Type-Options", "nosniff")
      .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
      .putHeader("X-Download-Options", "noopen")
      .putHeader("X-XSS-Protection", "1; mode=block")
      .putHeader("X-FRAME-OPTIONS", "DENY")
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(error.errorJson()));
  }

  void raise403(RoutingContext context, ApiError error) {

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

  void raise404(RoutingContext context) {

    context.response()
      .setStatusCode(404)
      .putHeader("Cache-Control", "no-store, no-cache")
      .putHeader("X-Content-Type-Options", "nosniff")
      .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
      .putHeader("X-Download-Options", "noopen")
      .putHeader("X-XSS-Protection", "1; mode=block")
      .putHeader("X-FRAME-OPTIONS", "DENY")
      .putHeader("content-type", "application/json; charset=utf-8")
      .end();
  }

  void raise422(RoutingContext context, ApiError error) {

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

  void raise429(RoutingContext context, ApiError error) {

    context.response()
      .setStatusCode(429)
      .putHeader("Cache-Control", "no-store, no-cache")
      .putHeader("X-Content-Type-Options", "nosniff")
      .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
      .putHeader("X-Download-Options", "noopen")
      .putHeader("X-XSS-Protection", "1; mode=block")
      .putHeader("X-FRAME-OPTIONS", "DENY")
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(error.errorJson()));
  }

  void raise500(RoutingContext context, Throwable e) {

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

    // Error logging
    if (config.getBoolean("debug.enabled", false))
      Sentry.capture(e);
  }
}
