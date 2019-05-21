/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.apierrors.InputFormatViolation;
import acecardapi.auth.ReactiveAuth;
import acecardapi.models.JwtToken;
import acecardapi.models.Users;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgRowSet;
import io.reactiverse.pgclient.Tuple;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.web.RoutingContext;

public class LoginHandler extends AbstractCustomHandler{

  private ReactiveAuth dbAuth;
  private JWTAuth jwtProvider;

  public LoginHandler(PgPool dbClient, JsonObject config, ReactiveAuth dbAuth, JWTAuth jwtProvider) {
    super(dbClient, config);
    this.dbAuth = dbAuth;
    this.jwtProvider = jwtProvider;
  }

  public void login(RoutingContext context) {

    Users users = null;

    try{
      users = Json.decodeValue(context.getBodyAsString(), Users.class);
    } catch (Exception e) {

      if (e.getMessage().contains("email_format_invalid")) {

        // Email address has invalid format

        InputFormatViolation error = new InputFormatViolation("email_address");
        context.response().setStatusCode(422).end(Json.encodePrettily(error.errorJson()));

      }

      context.response().setStatusCode(500)
        .putHeader("content-type", "application/json; charset=utf-8"
        ).end("Something went wrong...");

      return;

    }

    // Verify if email is activated

    dbAuth.authenticate(users.toJsonObjectLogin(), res -> {

      // todo: handle is_email_verified

      if(res.succeeded()) {
        User logged_user = res.result();

        System.out.println(logged_user.principal());

        String token = jwtProvider.generateToken(new JsonObject()
          .put("sub", logged_user.principal().getString("id")),
          new JWTOptions().setExpiresInSeconds(config.getInteger("jwt.exptime")));

        context.response().setStatusCode(200).end(Json.encodePrettily(new JwtToken(token)));
      }

      else {
        context.response()
          .putHeader("content-type", "application/json; charset=utf-8"
          ).setStatusCode(401).end("Unauthroized");

      }
    });
  }

}
