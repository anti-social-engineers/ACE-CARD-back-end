/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.apierrors.ParameterNotFoundViolation;
import acecardapi.models.Deposit;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Source;
import io.reactiverse.pgclient.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static acecardapi.utils.RequestUtilities.attributesCheckJsonObject;

public class PaymentHandler extends AbstractCustomHandler {

  private String[] stripeSourceAttributes = new String[]{"amount", "return_url"};

  public PaymentHandler(PgPool dbClient, JsonObject config) {
    super(dbClient, config);
  }

  public void stripeSource(RoutingContext context) {
    attributesCheckJsonObject(context.getBodyAsJson(), stripeSourceAttributes, attributeRes -> {
      if (attributeRes.succeeded()) {

        // Convert from cents to currency
        double amount = context.getBodyAsJson().getInteger("amount") / 100;

        String return_url = context.getBodyAsJson().getString("return_url");

        dbClient.getConnection(getConnectionRes -> {
          if (getConnectionRes.succeeded()) {
            PgConnection connection = getConnectionRes.result();

            getUserCardStripeSource(connection, UUID.fromString(context.user().principal().getString("sub")), userCardRes -> {

              if (userCardRes.succeeded()) {
                Deposit deposit = new Deposit(amount, userCardRes.result());

                createDepositStripeSource(connection, deposit, createDepositRes -> {
                  if (createDepositRes.succeeded()) {

                    Map<String, Object> sourceParams = new HashMap<>();
                    sourceParams.put("type", "ideal");
                    sourceParams.put("currency", "eur");
                    sourceParams.put("amount", context.getBodyAsJson().getInteger("amount"));
                    sourceParams.put("statement_descriptor", "deposit:" + createDepositRes.result().toString());

                    Map<String, Object> redirectParams = new HashMap<>();
                    redirectParams.put("return_url", return_url);

                    sourceParams.put("redirect", redirectParams);

                    try {
                      Source source = Source.create(sourceParams);

                      String sourceId = source.getId();

                      updateDepositStripeSource(connection, createDepositRes.result(), sourceId, updateDepositRes -> {
                        if (updateDepositRes.succeeded()) {

                          String redirect_url = source.getRedirect().getUrl();

                          raise201(context, new JsonObject().put("url", redirect_url));
                          connection.close();

                        } else {
                          raise500(context, updateDepositRes.cause());
                          connection.close();
                        }
                      });

                    } catch (Exception e) {
                      raise500(context, e);
                    }

                  } else {
                    raise500(context, createDepositRes.cause());
                    connection.close();
                  }
                });

              } else {
                raise500(context, userCardRes.cause());
                connection.close();
              }

            });

          } else {
            raise500(context, getConnectionRes.cause());
          }
        });

      } else {
        // An attribute is missing

        ParameterNotFoundViolation error = new ParameterNotFoundViolation(attributeRes.cause().getMessage());
        raise422(context, error);
      }
    });
  }

  private void getUserCardStripeSource(PgConnection connection, UUID userId, Handler<AsyncResult<UUID>> resultHandler) {

    connection.preparedQuery("SELECT id FROM cards WHERE user_id_id = $1", Tuple.of(userId), userRes -> {

      if (userRes.succeeded()) {

        if (userRes.result().rowCount() <= 0 || userRes.result().rowCount() >= 2) {
          resultHandler.handle(Future.failedFuture("More than one user returned"));
        } else {
          resultHandler.handle(Future.succeededFuture(userRes.result().iterator().next().getUUID("id")));
        }

      } else {
        resultHandler.handle(Future.failedFuture(userRes.cause()));
      }

    });
  }

  private void createDepositStripeSource(PgConnection connection, Deposit deposit, Handler<AsyncResult<UUID>> resultHandler) {

    connection.preparedQuery("INSERT INTO deposits (id, amount, deposited_at, card_id_id) VALUES ($1, $2, $3, $4)", deposit.toTuple(), depositRes -> {

      if (depositRes.succeeded()) {
        resultHandler.handle(Future.succeededFuture(deposit.getId()));

      } else {
        resultHandler.handle(Future.failedFuture(depositRes.cause()));
      }

    });
  }

  private void updateDepositStripeSource(PgConnection connection, UUID depositId, String sourceId, Handler<AsyncResult<Boolean>> resultHandler) {

    connection.preparedQuery("UPDATE deposits SET source_id = $1, status = $2 WHERE id = $3", Tuple.of(sourceId, "source_created", depositId), depositRes -> {

      if (depositRes.succeeded()) {
        resultHandler.handle(Future.succeededFuture(true));

      } else {
        resultHandler.handle(Future.failedFuture(depositRes.cause()));
      }

    });
  }

  public void chargeableSourceWebhook(RoutingContext context) {

    String source = context.getBodyAsJson().getJsonObject("data").getJsonObject("object").getString("id");
    int amount = context.getBodyAsJson().getJsonObject("data").getJsonObject("object").getInteger("amount");

    dbClient.getConnection(getConnectonRes -> {

      if (getConnectonRes.succeeded()) {

        PgConnection connection = getConnectonRes.result();

        connection.preparedQuery("SELECT id FROM deposits WHERE source_id = $1", Tuple.of(source), depositRes -> {

          if (depositRes.succeeded()) {

            PgRowSet results = depositRes.result();

            if (results.rowCount() <= 0) {
              raise404(context);
            } else {

              UUID depositId = results.iterator().next().getUUID("ID");

              processChargeableSourceWebhook(context, connection, source, depositId, amount);

            }

          } else {
            raise500(context, depositRes.cause());
            connection.close();
          }

        });

      } else {
        raise500(context, getConnectonRes.cause());
      }
    });
  }

  private void processChargeableSourceWebhook(RoutingContext context, PgConnection connection, String source, UUID depositId, int amount) {


    Map<String, Object> params = new HashMap<String, Object>();
    params.put("amount", amount);
    params.put("currency", "eur");
    params.put("source", source);

    createStripeCharge(params, stripeChargeRes -> {
      if (stripeChargeRes.succeeded()) {

        // Charge created, update the Deposit
        updateDepositStripeCharge(connection, depositId, stripeChargeRes.result().getId(), updateDepositRes -> {
          if (updateDepositRes.succeeded()) {

            raise201(context);
            connection.close();

          } else {
            raise500(context, updateDepositRes.cause());
            connection.close();
          }
        });

      } else {
        raise500(context, stripeChargeRes.cause());
        connection.close();
      }
    });
  }

  public void failedSourceWebhook(RoutingContext context){

    String source = context.getBodyAsJson().getJsonObject("data").getJsonObject("object").getString("id");

    dbClient.preparedQuery("DELETE FROM deposits WHERE source_id = $1", Tuple.of(source), res -> {

      if(res.succeeded())
      {
        raise200(context);
      }
      else
      {
        raise500(context, res.cause());
      }
    });
  }

  private void createStripeCharge(Map<String, Object> params, Handler<AsyncResult<Charge>> resultHandler) {
    try {
      Charge charge = Charge.create(params);
      resultHandler.handle(Future.succeededFuture(charge));
    } catch (StripeException se) {
      resultHandler.handle(Future.failedFuture(se));
    }
  }

  private void updateDepositStripeCharge(PgConnection connection, UUID depositId, String chargeId, Handler<AsyncResult<Boolean>> resultHandler) {

    connection.preparedQuery("UPDATE deposits SET charge_id = $1, status = $2 WHERE id = $3", Tuple.of(chargeId, "charge_created", depositId), depositRes -> {

      if (depositRes.succeeded()) {
        resultHandler.handle(Future.succeededFuture(true));

      } else {
        resultHandler.handle(Future.failedFuture(depositRes.cause()));
      }
    });
  }

  public void succeededChargeWebhook(RoutingContext context) {

    String source = context.getBodyAsJson().getJsonObject("data").getJsonObject("object").getJsonObject("source").getString("id");

    dbClient.getConnection(getConnectionRes -> {
      if (getConnectionRes.succeeded()) {

        PgConnection connection = getConnectionRes.result();

        succeededChargeWebhookTransaction(connection, source, res -> {

          if (res.succeeded()) {
            //TODO: CHANGE TO 200
            raise201(context);
            connection.close();
          } else {
            raise500(context, res.cause());
            connection.close();
          }

        });

      } else {
        raise500(context, getConnectionRes.cause());
      }
    });

  }

  private void succeededChargeWebhookTransaction(PgConnection connection, String source, Handler<AsyncResult<UUID>> resultHandler) {

    PgTransaction transaction = connection.begin();

    transaction.preparedQuery("SELECT id, card_id_id, amount FROM deposits WHERE source_id = $1", Tuple.of(source), getDepositRes -> {
      if (getDepositRes.succeeded()) {

        if (getDepositRes.result().rowCount() <= 0) {
          resultHandler.handle(Future.failedFuture("Not found"));
        } else {

          Row depositRow = getDepositRes.result().iterator().next();

          transaction.preparedQuery("UPDATE cards SET credits = credits + $1 WHERE id = $2", Tuple.of(depositRow.getNumeric("amount").doubleValue(), depositRow.getUUID("card_id_id")), updateCardRes -> {

            if (updateCardRes.succeeded()) {

              transaction.preparedQuery("UPDATE deposits SET status = $1 WHERE id = $2", Tuple.of("succeeded", depositRow.getUUID("id")), updateDepositRes -> {

                if (updateDepositRes.succeeded()) {

                  transaction.commit(transCommitRes -> {
                    if (transCommitRes.succeeded()) {
                      resultHandler.handle(Future.succeededFuture(depositRow.getUUID("id")));
                    } else {
                      resultHandler.handle(Future.failedFuture(transCommitRes.cause()));
                    }
                  });

                } else {
                  resultHandler.handle(Future.failedFuture(updateDepositRes.cause()));
                }

              });

            } else {
              resultHandler.handle(Future.failedFuture(updateCardRes.cause()));
            }

          });

        }

      } else {
        resultHandler.handle(Future.failedFuture(getDepositRes.cause()));
      }
    });
  }

}
