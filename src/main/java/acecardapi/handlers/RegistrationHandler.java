/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.apierrors.InputFormatViolation;
import acecardapi.apierrors.UniqueViolation;
import acecardapi.auth.ReactiveAuth;
import acecardapi.models.Users;
import io.reactiverse.pgclient.PgException;
import io.reactiverse.pgclient.PgPool;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;

public class RegistrationHandler extends AbstractCustomHandler {

  private ReactiveAuth authProvider;

  public RegistrationHandler(PgPool dbClient, JsonObject config, ReactiveAuth authProvider) {
    super(dbClient, config);
    this.authProvider = authProvider;
  }

  public void registerUser(RoutingContext context) {

    try {

      Users users = Json.decodeValue(context.getBodyAsString(), Users.class);

      this.createUser(context, users);

    } catch (Exception e) {

      if (e.getMessage().contains("email_format_invalid")) {

        // Email address has invalid format

        InputFormatViolation error = new InputFormatViolation("email_address");
        context.response().setStatusCode(422).end(Json.encodePrettily(error.errorJson()));

      }

      context.response().setStatusCode(500).end();

    }

  }

  private void createUser(RoutingContext context, Users users) {

    // Generate the salt, and combine it with the password to create the hashed password
    String salt = authProvider.generateSalt();
    String hash = authProvider.computeHash(users.getPassword(), salt);

    users.setPassword(hash);
    users.setPassword_salt(salt);

    dbClient.preparedQuery("INSERT INTO users (id, email, password, password_salt) VALUES ($1, $2, $3, $4)", users.toTuple(), res -> {
      if (res.succeeded()) {
        context.response().putHeader("content-type", "application/json; charset=utf-8").setStatusCode(201).end();
      }

      else {

        if (res.cause() instanceof PgException) {
          String error_Code = ((PgException) res.cause()).getCode();

          // PostgreSQL error 23505 (e.g. unique constraint failure)
          if (error_Code.equals("23505")) {

            UniqueViolation error = new UniqueViolation("email_address");

            context.response().setStatusCode(409).putHeader("content-type", "application/json; charset=utf-8").end(Json.encode(error.errorJson()));
          }
        }
        context.response().setStatusCode(400).putHeader("content-type", "application/json; charset=utf-8").end(Json.encode("Something went wrong."));
      }
    });
  }
}
