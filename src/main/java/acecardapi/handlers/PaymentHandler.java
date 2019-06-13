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
import com.stripe.model.Charge;
import com.stripe.model.Source;
import io.reactiverse.pgclient.PgConnection;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.Tuple;
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
  private String[] stripeChargeAttributes = new String[]{"amount", "return_url"};

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

  public void createStripeCharge(RoutingContext context) {

    System.out.println(context.getBodyAsJson());

    context.response().setStatusCode(200).end();

    try {
      String source = context.getBodyAsJson().getJsonObject("data").getJsonObject("object").getString("id");
      int amount = context.getBodyAsJson().getJsonObject("data").getJsonObject("object").getInteger("amount");

      Map<String, Object> params = new HashMap<String, Object>();
      params.put("amount", amount);
      params.put("currency", "eur");
      params.put("source", source);

      Charge charge = Charge.create(params);

      context.response()
        .setStatusCode(201)
        .putHeader("Cache-Control", "no-store, no-cache")
        .putHeader("X-Content-Type-Options", "nosniff")
        .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
        .putHeader("X-Download-Options", "noopen")
        .putHeader("X-XSS-Protection", "1; mode=block")
        .putHeader("X-FRAME-OPTIONS", "DENY")
        .putHeader("content-type", "application/json; charset=utf-8")
        .end();

    } catch (Exception e) {
      raise500(context, e);
    }

  }

}
