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
import acecardapi.models.Users;
import io.reactiverse.pgclient.PgException;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgRowSet;
import io.reactiverse.pgclient.Row;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.auth.User;
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

      Users users = Json.decodeValue(context.getBodyAsString(), Users.class);

      this.registerUser(context, users);

    } catch (Exception e) {

      if (e.getMessage().contains("email_format_invalid")) {

        // Email address has invalid format

        InputFormatViolation error = new InputFormatViolation("email_address");
        context.response().setStatusCode(422).end(Json.encode(error.errorJson()));

      }

      context.response().setStatusCode(500).end();

    }

  }

  private void registerUser(RoutingContext context, Users users) {

    String salt = dbAuth.generateSalt();
    String hash = dbAuth.computeHash(users.getPassword(), salt);

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


  public void getUsers(RoutingContext context) {

    dbClient.query("SELECT * FROM users", ar -> {
      if (ar.succeeded()) {
        PgRowSet result = ar.result();
        System.out.println("Got " + result.size() + " rows ");

        JsonArray jsonArray = new JsonArray();

        for (Row row: result) {
          System.out.println(row.toString());

          Users users = new Users(row.getUUID("id"), row.getString("email"));

          jsonArray.add(users.toJsonObject());
        }

        context.response()
          .putHeader("content-type", "application/json; charset=utf-8"
          ).end(Json.encodePrettily(jsonArray));

      } else {
        System.out.println("Failure: " + ar.cause().getMessage());

        context.response()
          .putHeader("content-type", "application/json; charset=utf-8"
          ).end("Sorry! Ik heb nog geen response....");

      }
    });
  }

  public void loginUser(RoutingContext context) {

    Users users = Json.decodeValue(context.getBodyAsString(), Users.class);

    dbAuth.authenticate(users.toJsonObjectLogin(), res -> {
      if (res.succeeded()) {
        // TODO: JWT
        User logged_user = res.result();

        context.response().setStatusCode(200).end();

      } else {
        System.out.println(res.cause().toString());
        context.response().setStatusCode(401).end();
      }
    });

  }

}
