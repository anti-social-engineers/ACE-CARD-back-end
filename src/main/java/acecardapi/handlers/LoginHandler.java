/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.apierrors.EmailNotVerifiedViolation;
import acecardapi.apierrors.InputFormatViolation;
import acecardapi.auth.ReactiveAuth;
import acecardapi.models.JwtToken;
import acecardapi.models.Users;
import io.reactiverse.pgclient.*;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

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
        context.response()
          .setStatusCode(422)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(error.errorJson()));

      }

      context.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end("Something went wrong...");

      return;

    }

    // Verify if email is activated

    dbAuth.authenticate(users.toJsonObjectLogin(), userRes -> {
      // Important note: This is reactive programming, e.g. async. Use Futures etc. to handle that.

      if(userRes.succeeded()) {

        User logged_user = userRes.result();

        UUID id = UUID.fromString(logged_user.principal().getString("id"));

        // Check if the user has a verified email:
        dbClient.preparedQuery("SELECT is_email_verified, role, image_id FROM users WHERE id=$1", Tuple.of(id), res -> {
          if (res.succeeded()) {
            PgRowSet result = res.result();

            if (result.rowCount() == 0 || result.rowCount() >= 2) {
              // No user or more than 1 user was found with this id, should be impossible to end up here since we
              // verify the JWT signature, but we need to handle it just in case.

              context.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .setStatusCode(500)
                .end();

            } else {

              // Check if the user has activated their email:
              Row row = result.iterator().next();

              if (!row.getBoolean("is_email_verified")) {
                // TODO: Handle sending new activation email or something....

                EmailNotVerifiedViolation error = new EmailNotVerifiedViolation("email_not_activated");
                context.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .setStatusCode(403)
                  .end(Json.encodePrettily(error.errorJson()));

              } else {

                String profile_image;
                var db_profile_image = row.getUUID("image_id");
                if (db_profile_image != null) {
                  profile_image = db_profile_image.toString();
                } else
                  profile_image = "";

                String token = jwtProvider.generateToken(new JsonObject()
                    .put("sub", logged_user.principal().getString("id"))
                    .put("permissions", new JsonArray().add(row.getString("role")))
                    .put("profile_image", profile_image),
                  new JWTOptions().setExpiresInSeconds(config.getInteger("jwt.exptime", 32400)));

                context.response()
                  .setStatusCode(200)
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(Json.encodePrettily(new JwtToken(token)));
              }
            }
          } else {
            // Return a 500 error (Something went wrong connecting with the db/executing the query)
            context.response()
              .putHeader("content-type", "application/json; charset=utf-8")
              .setStatusCode(500)
              .end();

          }
        });
      }

      else {
        context.response()
          .putHeader("content-type", "application/json; charset=utf-8")
          .setStatusCode(401)
          .end("Unauthroized");
      }
    });
  }

}
