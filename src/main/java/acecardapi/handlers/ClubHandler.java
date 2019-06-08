/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.apierrors.InputFormatViolation;
import acecardapi.apierrors.ParameterNotFoundViolation;
import acecardapi.models.ClubVisitor;
import io.reactiverse.pgclient.*;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.UUID;

import static acecardapi.utils.AceCardDecrypter.decrypt;
import static acecardapi.utils.NumberUtilities.*;
import static acecardapi.utils.RequestUtilities.attributesCheckJsonObject;

public class ClubHandler extends AbstractCustomHandler{

  private String[] requiredAttributesProcessCardPayment = new String[]{"club_id", "card_code", "card_pin", "amount"};

  public ClubHandler(PgPool dbClient, JsonObject config) {
    super(dbClient, config);
  }

  public void scanCard(RoutingContext context) {
     /*
     Function which handles the scanning of a Ace Card.
     */

    JsonObject jsonInput = context.getBodyAsJson();

    if (jsonInput == null || jsonInput.isEmpty() || jsonInput.getString("card_code", null) == null) {

      context.response()
        .setStatusCode(422)
        .putHeader("Cache-Control", "no-store, no-cache")
        .putHeader("X-Content-Type-Options", "nosniff")
        .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
        .putHeader("X-Download-Options", "noopen")
        .putHeader("X-XSS-Protection", "1; mode=block")
        .putHeader("X-FRAME-OPTIONS", "DENY")
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(new ParameterNotFoundViolation("card_code").errorJson()));
    } else {

      String cardCode = jsonInput.getString("card_code");

      String decryptedCardCode = decrypt(cardCode, config.getString("card.encryptionkey", ""));

      // Check if a card with this code exists
      dbClient.getConnection(receivedConnection -> {
        if (receivedConnection.succeeded()) {

          PgConnection connection = receivedConnection.result();

          connection.preparedQuery("SELECT user_id_id FROM cards WHERE card_code=$1 AND is_activated=$2 AND is_blocked=$3", Tuple.of(decryptedCardCode, true, false), res -> {

            if(res.succeeded()) {

              if(res.result().rowCount() == 0) {
                // Card not found

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

                connection.close();

              } else {

                Row cardRow = res.result().iterator().next();
                UUID userId = cardRow.getUUID("user_id_id");

                connection.preparedQuery("SELECT first_name, last_name, date_of_birth, image_id FROM users WHERE id=$1", Tuple.of(userId), res2 -> {

                  if(res2.succeeded()) {

                    Row userRow = res2.result().iterator().next();

                    connection.preparedQuery("SELECT description FROM penalties WHERE recipient_id_id=$1 ORDER BY date_received DESC LIMIT 3", Tuple.of(userId), res3 -> {

                      if(res3.succeeded()) {

                        ArrayList<String> flags = new ArrayList<>();

                        PgRowSet penaltyRows = res3.result();

                        for (Row row: penaltyRows)
                        {
                          flags.add(row.getString("description"));
                        }

                        ClubVisitor visitor = new ClubVisitor(
                          userRow.getString("first_name"),
                          userRow.getString("last_name"),
                          userRow.getLocalDate("date_of_birth"),
                          userRow.getUUID("image_id"),
                          config.getString("http.image_dir", "static/images/"),
                          flags
                        );

                        context.response()
                          .setStatusCode(200)
                          .putHeader("Cache-Control", "no-store, no-cache")
                          .putHeader("X-Content-Type-Options", "nosniff")
                          .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
                          .putHeader("X-Download-Options", "noopen")
                          .putHeader("X-XSS-Protection", "1; mode=block")
                          .putHeader("X-FRAME-OPTIONS", "DENY")
                          .putHeader("content-type", "application/json; charset=utf-8")
                          .end(Json.encodePrettily(visitor.toJson()));
                        connection.close();


                      } else {
                        raise500(context, res3.cause());
                        connection.close();
                      }

                    });

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
          // Unable to get a DB connection, might be down...

          raise500(context, receivedConnection.cause());
        }
      });

    }
  }

  public void processCardPayment(RoutingContext context) {

    // Check of club id == jwt club id - check
    // Check of alle params aanwezig zijn
    // Convert card code
    // CHECK REDIS OP ATTEMPTS
    // Haal Salt e.d. op
    // Check of user Authorized is

    // Daarna transaction voor het aanmaken van de payment en afhalen van het bedrag

    attributesCheckJsonObject(context.getBodyAsJson(), requiredAttributesProcessCardPayment, attCheckRes -> {

      if (attCheckRes.succeeded()) {

        JsonObject jsonBody = context.getBodyAsJson();

        doubleHasNADecimals(2, jsonBody.getDouble("amount"), correctDecimalsRes -> {
          if (correctDecimalsRes.succeeded()) {

            String cardCode = jsonBody.getString("card_code");

            String decryptedCardCode = decrypt(cardCode, config.getString("card.encryptionkey", ""));

            dbClient.getConnection(getConnectionRes -> {

              if (getConnectionRes.succeeded()) {
                PgConnection connection = getConnectionRes.result();

                // Retrieve card data
                connection.preparedQuery("SELECT credits, pin, pin_salt, is_blocked WHERE card_code = $1", Tuple.of(decryptedCardCode), cardRes -> {

                  if (cardRes.succeeded()) {

                    PgRowSet result = cardRes.result();

                    if (result.rowCount() <= 0 || result.rowCount() >= 2) {
                      // No card found
                      raise404(context);
                      connection.close();
                    } else {

                      if

                    }

                  } else {

                    //TODO: TEST OF JE HIER NIET KOMT ALS CARDCODE NIET GEVONDEN WORDT
                    raise500(context, cardRes.cause());
                    connection.close();
                  }

                });
              } else {
                raise500(context, getConnectionRes.cause());
              }

            });
          } else {
            InputFormatViolation error = new InputFormatViolation("amount");
            raise422(context, error);
          }
        });


      } else {
        ParameterNotFoundViolation error = new ParameterNotFoundViolation(attCheckRes.cause().getMessage());
        raise422(context, error);
      }

    });
  }
}
