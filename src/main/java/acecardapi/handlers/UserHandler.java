/*
 * Copyright 2019 Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.models.Users;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgRowSet;
import io.reactiverse.pgclient.Row;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class UserHandler extends AbstractCustomHandler{

  public UserHandler(PgPool dbClient, JsonObject config) {
    super(dbClient, config);
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
          ).putHeader("Access-Control-Allow-Origin", "*")
          .putHeader("Access-Control-Allow-Methods","GET, POST, OPTIONS").end(Json.encodePrettily(jsonArray));

      } else {
        System.out.println("Failure: " + ar.cause().getMessage());

        context.response()
          .putHeader("content-type", "application/json; charset=utf-8"
          ).end("Sorry! Ik heb nog geen response....");

      }
    });
  }
}
