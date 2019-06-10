/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.apierrors.*;
import acecardapi.auth.ReactiveAuth;
import acecardapi.models.ClubVisitor;
import acecardapi.models.Payment;
import io.reactiverse.pgclient.*;
import io.reactiverse.pgclient.data.Numeric;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;

import java.util.ArrayList;
import java.util.UUID;

import static acecardapi.utils.AceCardDecrypter.decrypt;
import static acecardapi.utils.NumberUtilities.*;
import static acecardapi.utils.RequestUtilities.attributesCheckJsonObject;

public class ClubHandler extends AbstractCustomHandler{

  private RedisClient redisClient;
  private ReactiveAuth authProvider;

  private String[] requiredAttributesProcessCardPayment = new String[]{"club_id", "card_code", "card_pin", "amount"};

  public ClubHandler(PgPool dbClient, JsonObject config, RedisClient redisClient, ReactiveAuth authProvider) {
    super(dbClient, config);
    this.redisClient = redisClient;
    this.authProvider = authProvider;
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

      String decryptedCardCode = decrypt(cardCode, config.getString("card.encryptionkey", "C*F-JaNdRgUjXn2r5u8x/A?D(G+KbPeS"));

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

  public void cardPayment(RoutingContext context) {

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

            String decryptedCardCode = decrypt(cardCode, config.getString("card.encryptionkey", "C*F-JaNdRgUjXn2r5u8x/A?D(G+KbPeS"));

            String redisKey = "pin_attempt:" + decryptedCardCode;

            redisClient.get(redisKey, redisKeyRes -> {
              if (redisKeyRes.succeeded()) {


                if (redisKeyRes.result() == null || Integer.parseInt(redisKeyRes.result()) < config.getInteger("card.max_tries", 3)) {

                  processCardPayment(context, jsonBody, decryptedCardCode, redisKeyRes.result(), redisKey);

                } else {

                  raise429(context, new TooManyFailedAttemptsViolation());

                }

              } else {
                raise500(context, redisKeyRes.cause());
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

  private void processCardPayment(RoutingContext context, JsonObject requestBody, String decryptedCardCode, String attempts, String attemptsCode) {

    dbClient.getConnection(getConnectionRes -> {

      if (getConnectionRes.succeeded()) {
        PgConnection connection = getConnectionRes.result();

        // Retrieve card data
        connection.preparedQuery("SELECT id, credits, pin, pin_salt, is_blocked FROM cards WHERE card_code=$1", Tuple.of(decryptedCardCode), cardRes -> {

          if (cardRes.succeeded()) {

            PgRowSet result = cardRes.result();

            if (result.rowCount() <= 0 || result.rowCount() >= 2) {
              // No card found
              raise404(context);
              connection.close();

            } else {

              Row row = result.iterator().next();

              if (row.getBoolean("is_blocked")) {
                BlockedViolation error = new BlockedViolation("Card is suspended.");
                raise403(context, error);
                connection.close();
              } else if (!checkPIN(requestBody.getString("card_pin"), row.getString("pin_salt"), row.getString("pin"))) {
                AuthorisationViolation error = new AuthorisationViolation("PIN is invalid.");
                raise401(context, error);

                addPINAttempt(attempts, attemptsCode);

              } else if (requestBody.getDouble("amount") > row.getNumeric("credits").doubleValue()){
                CreditsViolaton error = new CreditsViolaton("Not enough credits.");
                raise400(context, error);
                connection.close();
              } else {

                Payment payment = new Payment(requestBody.getDouble("amount"), row.getUUID("id"), UUID.fromString(requestBody.getString("club_id")));

                processCardPaymentTransaction(connection, row.getUUID("id"), row.getNumeric("credits").doubleValue() - requestBody.getDouble("amount"), payment, transactionRes -> {

                  if (transactionRes.succeeded()) {

                    context.response()
                      .setStatusCode(201)
                      .putHeader("Cache-Control", "no-store, no-cache")
                      .putHeader("X-Content-Type-Options", "nosniff")
                      .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
                      .putHeader("X-Download-Options", "noopen")
                      .putHeader("X-XSS-Protection", "1; mode=block")
                      .putHeader("X-FRAME-OPTIONS", "DENY")
                      .putHeader("content-type", "application/json; charset=utf-8")

                      .end(Json.encodePrettily(payment.toJsonObject()));

                    connection.close();

                  } else {
                    raise500(context, transactionRes.cause());
                    connection.close();
                  }

                });
              }

            }

          } else {
            raise500(context, cardRes.cause());
            connection.close();
          }

        });
      } else {
        raise500(context, getConnectionRes.cause());
      }

    });
  }

  private boolean checkPIN(String inputPIN, String salt, String hashedPin) {
    String hashedInputPIN = authProvider.computeHash(inputPIN, salt);
    return hashedPin.equals(hashedInputPIN);
  }

  private void processCardPaymentTransaction(PgConnection connection, UUID cardId, Double newCreditLevel, Payment payment, Handler<AsyncResult<Double>> resultHandler) {

    PgTransaction transaction = connection.begin();

    transaction.preparedQuery("UPDATE cards SET credits=$1 WHERE id=$2", Tuple.of(Numeric.create(newCreditLevel), cardId), cardRes -> {
      if (cardRes.succeeded()) {
        transaction.preparedQuery("INSERT INTO payments (id, amount, paid_at, card_id_id, club_id) VALUES ($1, $2, $3, $4, $5)", payment.toTuple(), paymentRes -> {
          if (paymentRes.succeeded()) {
            transaction.commit(transactionRes -> {
              if (transactionRes.succeeded()) {
                resultHandler.handle(Future.succeededFuture(newCreditLevel));
              } else {
                resultHandler.handle(Future.failedFuture(transactionRes.cause()));
              }
            });
          } else {
            resultHandler.handle(Future.failedFuture(paymentRes.cause()));
          }
        });
      } else {
        resultHandler.handle(Future.failedFuture(cardRes.cause()));
      }
    });
  }

  private void addPINAttempt(String attempts, String attemptsCode) {

    int attemptsNumber;

    if (attempts != null)
     attemptsNumber = Integer.valueOf(attempts) + 1;
    else
      attemptsNumber = 1;

    redisClient.set(attemptsCode, Integer.toString(attemptsNumber), redisSetRes -> {
      if (redisSetRes.succeeded()) {
        redisClient.expire(attemptsCode, config.getLong("card.failed_attempts_expiration", 3600L), expireRedisRes -> {
        });
      }
    });
  }

}
