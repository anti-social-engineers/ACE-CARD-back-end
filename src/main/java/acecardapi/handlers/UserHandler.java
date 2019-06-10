/*
 * Copyright 2019 Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.models.Account;
import acecardapi.models.Users;
import io.reactiverse.pgclient.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

public class UserHandler extends AbstractCustomHandler{

  public UserHandler(PgPool dbClient, JsonObject config) {
    super(dbClient, config);
  }

  public void getUserData(RoutingContext context) {

    dbClient.getConnection(getConn -> {
      if (getConn.succeeded()) {

        UUID userId = UUID.fromString(context.user().principal().getString("sub"));

        // First: Check if the user has a card or not
        PgConnection connection = getConn.result();

        connection.preparedQuery("SELECT is_activated FROM cards WHERE user_id_id=$1", Tuple.of(userId), res -> {
          if (res.succeeded()) {

            if(res.result().rowCount() == 0) {

              // User exists, but does not yet have an ace card

              connection.preparedQuery("SELECT email FROM users WHERE id=$1", Tuple.of(userId), res2 -> {

                if (res2.succeeded()) {
                  Row row2 = res2.result().iterator().next();
                  getUserDataNoCard(context, row2.getString("email"));
                  connection.close();
                } else {
                  raise500(context, res2.cause());
                  connection.close();
                }
              });

            } else {

              // User has an ace card (or requested one)

              connection.preparedQuery("SELECT email, first_name, last_name, gender, date_of_birth, role, image_id FROM users WHERE id=$1", Tuple.of(userId), res2 -> {
                if (res2.succeeded()) {
                  Row row1 = res.result().iterator().next();
                  Row row2 = res2.result().iterator().next();

                  getUserDataWithCard(context, row1, row2);
                  connection.close();

                } else {
                  raise500(context, res2.cause());
                  connection.close();
                }
              });
            }

          } else {
            raise500(context, res.cause());
            connection.close();
          }
        });

      } else {
        raise500(context, getConn.cause());
      }
    });

  }

  private void getUserDataNoCard(RoutingContext context, String email) {
    Account acc = new Account(email, false);

    raise200(context, acc.toJson());
  }

  private void getUserDataWithCard(RoutingContext context, Row cardRow, Row userRow) {
    Account acc = new Account(
      userRow.getString("first_name"),
      userRow.getString("last_name"),
      userRow.getString("email"),
      userRow.getLocalDate("date_of_birth"),
      userRow.getString("gender"),
      userRow.getString("role"),
      userRow.getUUID("image_id"),
      config.getString("http.image_dir", "static/images/"),
      true,
      cardRow.getBoolean("is_activated")
    );

    raise200(context, acc.toJson());
  }

  public void getUsers(RoutingContext context) {

    dbClient.query("SELECT * FROM users", ar -> {
      if (ar.succeeded()) {
        PgRowSet result = ar.result();

        JsonArray jsonArray = new JsonArray();

        for (Row row: result) {

          Users users = new Users(row.getUUID("id"), row.getString("email"));

          jsonArray.add(users.toJsonObject());
        }

        raise200(context, jsonArray);

      } else {

        raise200(context, new JsonArray());

      }
    });
  }

}
