/*
 * Copyright 2019 Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.apierrors.InputFormatViolation;
import acecardapi.apierrors.UniqueViolation;
import acecardapi.auth.ReactiveAuth;
import acecardapi.models.User;
import io.reactiverse.pgclient.PgException;
import io.reactiverse.pgclient.PgPool;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

public class UserHandler {

  private PgPool dbClient;
  private ReactiveAuth dbAuth;

  public UserHandler(PgPool sqlClient, ReactiveAuth authProvider) {
    this.dbClient = sqlClient;
    this.dbAuth = authProvider;
  }

  public void createUser(RoutingContext context) {

    try {

      User user = Json.decodeValue(context.getBodyAsString(), User.class);

      this.registerUser(context, user);

    } catch (Exception e) {

      if (e.getMessage().contains("email_format_invalid")) {

        // Email address has invalid format

        InputFormatViolation error = new InputFormatViolation("email_address");
        context.response().setStatusCode(422).end(Json.encode(error.errorJson()));

      }

      context.response().setStatusCode(500).end();

    }

  }

  private void registerUser(RoutingContext context, User user) {

    String salt = dbAuth.generateSalt();
    String hash = dbAuth.computeHash(user.getPassword(), salt);

    user.setPassword(hash);
    user.setPassword_salt(salt);

    dbClient.preparedQuery("INSERT INTO users (id, email, password, password_salt) VALUES ($1, $2, $3, $4)", user.toTuple(), res -> {
      if (res.succeeded()) {
        context.response().setStatusCode(201).end();
      }

      else {

        if (res.cause() instanceof PgException) {
          String error_Code = ((PgException) res.cause()).getCode();

          // PostgreSQL error 23505 (e.g. unique constraint failure)
          if (error_Code.equals("23505")) {

            UniqueViolation error = new UniqueViolation("email_address");

            context.response().setStatusCode(409).end(Json.encode(error.errorJson()));
          }

          else {

            context.response().setStatusCode(400).end(Json.encode("Something went wrong."));

          }
        }
        context.response().setStatusCode(400).end(Json.encode("Something went wrong."));
      }
    });

  }

  public void loginUser(RoutingContext context) {

    User user = Json.decodeValue(context.getBodyAsString(), User.class);

    dbAuth.authenticate(user.toJsonObjectLogin(), res -> {
      if (res.succeeded()) {
        // TODO: JWT
        io.vertx.ext.auth.User logged_user = res.result();

        context.response().setStatusCode(200).end();

      } else {
        System.out.println(res.cause().toString());
        context.response().setStatusCode(401).end();
      }
    });

  }

}
