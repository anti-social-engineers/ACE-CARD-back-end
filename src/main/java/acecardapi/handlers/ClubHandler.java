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
import acecardapi.utils.RedisUtils;
import io.reactiverse.pgclient.*;
import io.reactiverse.pgclient.data.Numeric;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;
import io.vertx.redis.client.RedisAPI;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static acecardapi.utils.AceCardDecrypter.decrypt;
import static acecardapi.utils.NumberUtilities.*;
import static acecardapi.utils.RedisUtils.realTimeRedisLPUSH;
import static acecardapi.utils.RequestUtilities.attributesCheckJsonObject;

public class ClubHandler extends AbstractCustomHandler{

  private ReactiveAuth authProvider;

  private String[] requiredAttributesProcessCardPayment = new String[]{"club_id", "card_code", "card_pin", "amount"};

  public ClubHandler(PgPool dbClient, JsonObject config, ReactiveAuth authProvider) {
    super(dbClient, config);
    this.authProvider = authProvider;
  }

  /**
   Function which handles scanning of an ACE-card
   @param context contains information about the request
   @return void
   */
  public void scanCard(RoutingContext context) {
    JsonObject jsonInput = context.getBodyAsJson();

    if (jsonInput == null || jsonInput.isEmpty() || jsonInput.getString("card_code", null) == null) {

      raise422(context, new ParameterNotFoundViolation("card_code"));

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

                raise404(context);

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

  /**
   Function which handles payment of an ACE-card
   @param context contains information about the request
   @return void
   */
  public void cardPayment(RoutingContext context) {
    // Process a physical payment at a club using an ACE card

    attributesCheckJsonObject(context.getBodyAsJson(), requiredAttributesProcessCardPayment, attCheckRes -> {

      if (attCheckRes.succeeded()) {

        JsonObject jsonBody = context.getBodyAsJson();

        if (doubleMinMaxValue(jsonBody.getDouble("amount"), 0.01, 500000)) {
          doubleHasNADecimals(2, jsonBody.getDouble("amount"), correctDecimalsRes -> {
            if (correctDecimalsRes.succeeded()) {

              String cardCode = jsonBody.getString("card_code");

              String decryptedCardCode = decrypt(cardCode, config.getString("card.encryptionkey", "C*F-JaNdRgUjXn2r5u8x/A?D(G+KbPeS"));

              String redisKey = "pin_attempt:" + decryptedCardCode;

              RedisAPI redisClient = RedisAPI.api(RedisUtils.backEndRedis);

              redisClient.get(redisKey, redisKeyRes -> {
                if (redisKeyRes.succeeded()) {

                  String attempts = null;
                  if (redisKeyRes.result() != null) {
                    attempts = redisKeyRes.result().toString();
                  }

                  if (attempts == null || Integer.parseInt(attempts) < config.getInteger("card.max_tries", 3)) {

                    processCardPayment(context, redisClient, jsonBody, decryptedCardCode, attempts, redisKey);

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
          // Violation in min/max value
         raise422(context, new InputValueViolation("amount"));
        }
      } else {
        ParameterNotFoundViolation error = new ParameterNotFoundViolation(attCheckRes.cause().getMessage());
        raise422(context, error);
      }

    });
  }

  /**
   Function which further processes a payment with an ACE-card
   @param context contains information about the request
   @param redisClient API for talking with redis
   @param requestBody JsonObject which contains the request body as json
   @param decryptedCardCode the card_code but decrypted
   @param attempts how many attempts have already been made
   @param attemptsCode the code used to register attempts on
   @return void
   */
  private void processCardPayment(RoutingContext context, RedisAPI redisClient, JsonObject requestBody, String decryptedCardCode, String attempts, String attemptsCode) {

    dbClient.getConnection(getConnectionRes -> {

      if (getConnectionRes.succeeded()) {
        PgConnection connection = getConnectionRes.result();

        PgTransaction transaction = connection.begin();

        // Retrieve card data
        transaction.preparedQuery("SELECT id, credits, pin, pin_salt, is_blocked, user_id_id FROM cards WHERE card_code=$1", Tuple.of(decryptedCardCode), cardRes -> {

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
                connection.close();

                addPINAttempt(attempts, attemptsCode, redisClient);

              } else if (requestBody.getDouble("amount") > row.getNumeric("credits").doubleValue()){
                CreditsViolaton error = new CreditsViolaton("Not enough credits.");
                raise400(context, error);
                connection.close();
              } else {

                Payment payment = new Payment(requestBody.getDouble("amount"), row.getUUID("id"), UUID.fromString(requestBody.getString("club_id")));

                processCardPaymentTransaction(transaction, row.getUUID("id"), row.getNumeric("credits").doubleValue() - requestBody.getDouble("amount"), payment, transactionRes -> {

                  if (transactionRes.succeeded()) {

                    raise201(context, payment.toJsonObject(false));

                    connection.close();

                    RedisAPI realTimeRedisClient = RedisAPI.api(RedisUtils.frontEndRedis);

                    realTimeRedisLPUSH(realTimeRedisClient, row.getUUID("user_id_id"), "transaction", requestBody.getDouble("amount"), row.getNumeric("credits").doubleValue() - requestBody.getDouble("amount"), OffsetDateTime.now(), redisRes -> {
                      System.out.println(redisRes.result());
                    });

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

  /**
   Function which check whether a provided PIN equals the actual pin
   @param inputPIN the PIN as provided by the user
   @param salt the salt used for the PIN
   @param hashedPin the actual PIN hashed+salted
   @return void
   */
  private boolean checkPIN(String inputPIN, String salt, String hashedPin) {
    String hashedInputPIN = authProvider.computeHash(inputPIN, salt);
    return hashedPin.equals(hashedInputPIN);
  }

  /**
   Function which starts a transaction for inserting the payment
   @param transaction the current transaction
   @param cardId uuid of the card
   @param newCreditLevel the new level of credits
   @param payment a payment object
   @param resultHandler handler for async processing
   @return void
   */
  private void processCardPaymentTransaction(PgTransaction transaction, UUID cardId, Double newCreditLevel, Payment payment, Handler<AsyncResult<Double>> resultHandler) {

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

  /**
   Function which adds an additional attempt
   @param attempts the current amount of attempts
   @param attemptsCode the code used to track attempts
   @return void
   */
  private void addPINAttempt(String attempts, String attemptsCode, RedisAPI redisClient) {

    int attemptsNumber;

    if (attempts != null) {
      attemptsNumber = Integer.valueOf(attempts) + 1;
    }
    else
      attemptsNumber = 1;

    redisClient.set(Arrays.asList(attemptsCode, Integer.toString(attemptsNumber)),redisSetRes -> {
      if (redisSetRes.succeeded()) {
        redisClient.expire(attemptsCode, config.getLong("card.failed_attempts_expiration", 3600L).toString(), expireRedisRes -> {
        });
      }
    });
  }

}
