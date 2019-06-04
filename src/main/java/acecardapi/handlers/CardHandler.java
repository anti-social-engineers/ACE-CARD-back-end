/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.models.Address;
import acecardapi.models.Card;
import io.reactiverse.pgclient.*;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.time.LocalDate;
import java.util.UUID;

public class CardHandler extends AbstractCustomHandler{

  public CardHandler(PgPool dbClient, JsonObject config) {
    super(dbClient, config);
  }

  public void requestCard(RoutingContext context) {

    MultiMap attributes = context.request().formAttributes();

    Address address = new Address(attributes.get("address"),
      attributes.get("address_number"),
      attributes.get("address_annex"),
      attributes.get("city"),
      attributes.get("postalcode"));

    Card card = new Card(UUID.fromString(context.user().principal().getString("sub")));

    // TODO: CLOSE CONNECTION - Function to check if all fields are present - ONLY USER WITH NO CARD CAN ACCESS THIS ENDPOINT
    dbClient.getConnection(getConnection -> {
      if (getConnection.succeeded()) {
        PgConnection connection = getConnection.result();

        // Eerst address (insert or get)
        // Dan User (update)
        // Dan Cards (Insert)

        connection.preparedQuery("INSERT INTO addresses (id, address, address_num, address_annex, city, postalcode, country) VALUES ($1, $2, $3, $4, $5, $6, $7)", address.toTupleWithId(), query_address -> {
          if (query_address.succeeded()) {

            connection.close();
            buildCardAndUser(context, address.getId(), card);

          } else {

            if (query_address.cause() instanceof PgException) {
              String error_Code = ((PgException) query_address.cause()).getCode();

              // PostgreSQL error 23505 (e.g. unique constraint failure)
              if (error_Code.equals("23505")) {

                connection.preparedQuery("SELECT id FROM addresses WHERE address=$1 AND address_num=$2 AND address_annex=$3 AND city=$4 AND postalcode=$5 AND country=$6", address.toTupleNoId(), query_get_address -> {

                  if(query_get_address.succeeded()) {

                    PgRowSet result = query_get_address.result();

                    if (result.rowCount() != 1) {
                      connection.close();
                      raise500(context);
                    } else {
                      Row row = result.iterator().next();

                      // Go to the next step: The user
                      connection.close();
                      buildCardAndUser(context, row.getUUID("id"), card);
                    }
                  } else {

                    System.out.println(query_get_address.cause().toString());

                    connection.close();
                    raise500(context);
                  }
                });
              } else {
                connection.close();
                raise500(context);
              }
            }
          }
        });
      } else {
        raise500(context);
      }
    });
  }

  private void buildCardAndUser(RoutingContext context, UUID addressId, Card card) {
    // Transaction to update the user and create a card

    MultiMap attributes = context.request().formAttributes();
    String first_name = attributes.get("first_name");
    String last_name = attributes.get("last_name");
    String gender = attributes.get("gender");
    LocalDate dob = LocalDate.parse(attributes.get("dob"));
    UUID userId = UUID.fromString(context.user().principal().getString("sub"));

    System.out.println("???");

    dbClient.begin(res -> {

      if (res.succeeded()) {

        PgTransaction tx = res.result();

//        tx.query("UPDATE users SET first_name=$1, last_name=$2, gender=$3, date_of_birth=$4, address_id=$5 WHERE id=$6", Tuple.of(first_name, last_name, gender, dob, addressId, userId), ar -> {});
//        tx.query("INSERT INTO users (id, email, password, password_salt) VALUES ('60a2bf4d-ea0a-4355-9777-49e9e642c69a', 'kyrumx@gmail.com', 'kaas', 'kaas')", ar -> {});
        tx.preparedQuery("INSERT INTO users (id, email, password, password_salt) VALUES ($1, $2, $3, $4)", Tuple.of(UUID.fromString("60a2bf4d-ea0a-4355-9777-49e9e642c69a"), "somemail@gmail.com", "random", "salt"), ar -> {
          System.out.println(ar.cause().toString());
        });
//        tx.query("INSERT INTO cards (id, requested_at, user_id_id) VALUES ($1, $2, $3)", card.toTuple(), ar -> {});

        tx.commit(res2 -> {
          if (res2.succeeded()) {
            System.out.println("Transaction succeeded");
            context.response().setStatusCode(200).end();
          } else {
            System.out.println("Transaction failed " + res2.cause().getMessage());
            context.response().setStatusCode(500).end();
          }
        });

      } else {
        raise500(context);
      }
    });
  }

  private void raise500(RoutingContext context) {
    //TODO: DELETE IMAGE?

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

