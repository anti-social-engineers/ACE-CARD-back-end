/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import io.reactiverse.pgclient.PgConnection;
import io.reactiverse.pgclient.PgPool;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class CardHandler extends AbstractCustomHandler{

  public CardHandler(PgPool dbClient, JsonObject config) {
    super(dbClient, config);
  }

  public void requestCard(RoutingContext context) {

    MultiMap attributes = context.request().formAttributes();

    // TODO: CLOSE CONNECTION
    dbClient.getConnection(getConnection -> {
      if (getConnection.succeeded()) {
        PgConnection connection = getConnection.result();

        // Eerst address (insert or get)
        // Dan User (update)
        // Dan Cards (Insert)

        connection.query("SELECT * FROM users WHERE id='julien'", ar2 -> {
          if (ar1.succeeded()) {
            connection.query("SELECT * FROM users WHERE id='paulo'", ar3 -> {
              // Do something with rows and return the connection to the pool
              connection.close();
            });
          } else {
            // Return the connection to the pool
            connection.close();
          }
        });
      } else {

      }
    });

  }

  private void raise500(RoutingContext context) {
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
  }

}
