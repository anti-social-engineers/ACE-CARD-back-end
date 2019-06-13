/*
 * Copyright 2019 Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.apierrors.InputFormatViolation;
import acecardapi.apierrors.ParameterNotFoundViolation;
import acecardapi.models.Account;
import acecardapi.models.Deposit;
import acecardapi.models.Payment;
import acecardapi.models.Users;
import io.reactiverse.pgclient.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import static acecardapi.utils.RequestUtilities.singlePathParameterCheck;

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

        connection.preparedQuery("SELECT is_activated, credits FROM cards WHERE user_id_id=$1", Tuple.of(userId), res -> {
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
      cardRow.getBoolean("is_activated"),
      cardRow.getNumeric("credits").doubleValue()
    );

    raise200(context, acc.toJson());
  }

  public void userPayments(RoutingContext context) {


    if (!singlePathParameterCheck("sorting", context.request()))
      raise422(context, new ParameterNotFoundViolation("sorting"));
    else if (context.request().getParam("sorting").equals("desc") || context.request().getParam("sorting").equals("asc"))
    {
      if (singlePathParameterCheck("cursor", context.request())) {

        try {
          OffsetDateTime.parse(context.request().getParam("cursor"));
          processUserPayments(context, true);
        } catch (DateTimeParseException e) {
          raise422(context, new InputFormatViolation("cursor"));
        }

      } else {
        processUserPayments(context, false);
      }
    } else {
      raise422(context, new InputFormatViolation("sorting"));
    }
  }

  private void processUserPayments(RoutingContext context, boolean has_cursor) {

    if (has_cursor) {
      processUserPaymentsQuery(context, processUserPaymentsCursorQuery(context.request().getParam("sorting")), true);
    } else {
      processUserPaymentsQuery(context, processUserPaymentsQuery(context.request().getParam("sorting")), false);
    }

  }

  private void processUserPaymentsQuery(RoutingContext context, String query, boolean has_cursor) {

    dbClient.getConnection(getConnRes -> {
      if (getConnRes.succeeded()) {

        PgConnection connection = getConnRes.result();

        connection.preparedQuery("SELECT id FROM cards WHERE user_id_id = $1", Tuple.of(UUID.fromString(context.user().principal().getString("sub"))), cardRes -> {

          if (cardRes.succeeded()) {

            if (cardRes.result().rowCount() <= 0 || cardRes.result().rowCount() > 1) {
              connection.close();
            } else {

              UUID cardId = cardRes.result().iterator().next().getUUID("id");

              connection.preparedQuery(query, processUserPaymentsTuple(has_cursor, cardId, context.request().getParam("cursor")), paymentRes -> {

                 if (paymentRes.succeeded()) {

                   PgRowSet rows = paymentRes.result();

                   JsonArray jsonArray = new JsonArray();

                   for (Row row: rows) {
                     Payment payment = new Payment(row.getUUID("id"),
                       row.getNumeric("amount").doubleValue(),
                       row.getOffsetDateTime("paid_at"),
                       row.getString("club_name"));

                     jsonArray.add(payment.toJsonObject(true));
                   }

                   // Remove the last element from the list if row count > max return size

                   raise200(context, paymentsDepositsResponseHandler(rows, jsonArray, "payments"));


                 } else {
                   raise500(context, paymentRes.cause());
                   connection.close();
                 }

                });

            }

          } else {
            raise500(context, cardRes.cause());
            connection.close();
          }
        });

      } else {
        raise500(context, getConnRes.cause());
      }
    });

  }

  private String processUserPaymentsQuery(String order) {

    // We want to send back our LIMIT, but we also need to know the next item after limit
    int limit = config.getInteger("queries.max_return_size", 25) + 1;

    if (order.equals("desc")) {
      return "SELECT pa.id, pa.amount, pa.paid_at, cl.club_name FROM payments as pa INNER JOIN clubs as cl ON pa.club_id = cl.id WHERE pa.card_id_id = $1 ORDER BY pa.paid_at DESC LIMIT " + limit;
    } else {
      return "SELECT pa.id, pa.amount, pa.paid_at, cl.club_name FROM payments as pa INNER JOIN clubs as cl ON pa.club_id = cl.id WHERE pa.card_id_id = $1 ORDER BY pa.paid_at ASC LIMIT " + limit;
    }

  }

  private String processUserPaymentsCursorQuery(String order) {

    // We want to send back our LIMIT, but we also need to know the next item after limit
    int limit = config.getInteger("queries.max_return_size", 25) + 1;

    if (order.equals("desc")) {
      return "SELECT pa.id, pa.amount, pa.paid_at, cl.club_name FROM payments as pa INNER JOIN clubs as cl ON pa.club_id = cl.id WHERE pa.card_id_id = $1 AND pa.paid_at <= $2 ORDER BY pa.paid_at DESC LIMIT " + limit;
    } else {
      return "SELECT pa.id, pa.amount, pa.paid_at, cl.club_name FROM payments as pa INNER JOIN clubs as cl ON pa.club_id = cl.id WHERE pa.card_id_id = $1 AND pa.paid_at > $2 ORDER BY pa.paid_at ASC LIMIT " + limit;
    }

  }

  private Tuple processUserPaymentsTuple(boolean has_cursor, UUID cardId, String cursor) {

    if (has_cursor) {
      return Tuple.of(cardId, OffsetDateTime.parse(cursor));
    } else {
      return Tuple.of(cardId);
    }
  }

  public void userDeposits(RoutingContext context) {

    if (!singlePathParameterCheck("sorting", context.request()))
      raise422(context, new ParameterNotFoundViolation("sorting"));
    else if (context.request().getParam("sorting").equals("desc") || context.request().getParam("sorting").equals("asc"))
    {
      if (singlePathParameterCheck("cursor", context.request())) {

        try {
          OffsetDateTime.parse(context.request().getParam("cursor"));
          processUserDeposits(context, true);
        } catch (DateTimeParseException e) {
          raise422(context, new InputFormatViolation("cursor"));
        }

      } else {
        processUserDeposits(context, false);
      }
    } else {
      raise422(context, new InputFormatViolation("sorting"));
    }
  }

  private void processUserDeposits(RoutingContext context, boolean has_cursor) {

    if (has_cursor) {
      processUserDepositsQuery(context, processUserDepositsCursorQuery(context.request().getParam("sorting")), true);
    } else {
      processUserDepositsQuery(context, processUserDepositsQuery(context.request().getParam("sorting")), false);
    }

  }

  private void processUserDepositsQuery(RoutingContext context, String query, boolean has_cursor) {

    dbClient.getConnection(getConnRes -> {
      if (getConnRes.succeeded()) {

        PgConnection connection = getConnRes.result();

        connection.preparedQuery("SELECT id FROM cards WHERE user_id_id = $1", Tuple.of(UUID.fromString(context.user().principal().getString("sub"))), cardRes -> {

          if (cardRes.succeeded()) {

            if (cardRes.result().rowCount() <= 0 || cardRes.result().rowCount() > 1) {
              connection.close();
            } else {

              UUID cardId = cardRes.result().iterator().next().getUUID("id");

              connection.preparedQuery(query, processUserDepositsTuple(has_cursor, cardId, context.request().getParam("cursor")), depositsRes -> {

                if (depositsRes.succeeded()) {

                  PgRowSet rows = depositsRes.result();

                  JsonArray jsonArray = new JsonArray();

                  for (Row row: rows) {
                    Deposit deposit = new Deposit(row.getUUID("id"),
                      row.getNumeric("amount").doubleValue(),
                      row.getOffsetDateTime("deposited_at"));

                    jsonArray.add(deposit.toJsonObject());
                  }

                  raise200(context, paymentsDepositsResponseHandler(rows, jsonArray, "deposits"));

                } else {
                  raise500(context, depositsRes.cause());
                  connection.close();
                }

              });

            }

          } else {
            raise500(context, cardRes.cause());
            connection.close();
          }
        });

      } else {
        raise500(context, getConnRes.cause());
      }
    });

  }

  private String processUserDepositsQuery(String order) {

    // We want to send back our LIMIT, but we also need to know the next item after limit
    int limit = config.getInteger("queries.max_return_size", 25) + 1;

    if (order.equals("desc")) {
      return "SELECT id, amount, deposited_at FROM deposits  WHERE card_id_id = $1 ORDER BY deposited_at DESC LIMIT " + limit;
    } else {
      return "SELECT id, amount, deposited_at FROM deposits  WHERE card_id_id = $1 ORDER BY deposited_at ASC LIMIT " + limit;
    }

  }

  private String processUserDepositsCursorQuery(String order) {

    // We want to send back our LIMIT, but we also need to know the next item after limit
    int limit = config.getInteger("queries.max_return_size", 25) + 1;

    if (order.equals("desc")) {
      return "SELECT id, amount, deposited_at FROM deposits  WHERE card_id_id = $1 AND deposited_at <= $2 ORDER BY deposited_at DESC LIMIT " + limit;
    } else {
      return "SELECT id, amount, deposited_at FROM deposits  WHERE card_id_id = $1 AND deposited_at > $2 ORDER BY deposited_at DESC LIMIT " + limit;
    }
  }

  private Tuple processUserDepositsTuple(boolean has_cursor, UUID cardId, String cursor) {

    if (has_cursor) {
      return Tuple.of(cardId, OffsetDateTime.parse(cursor));
    } else {
      return Tuple.of(cardId);
    }
  }

  private JsonObject paymentsDepositsResponseHandler(PgRowSet rows, JsonArray jsonArray, String type) {

    String next_cursor = null;

    if (rows.rowCount() == config.getInteger("queries.max_return_size", 25) + 1) {
      next_cursor = jsonArray.getJsonObject(jsonArray.size() - 1).getString("time");
    }

    if (next_cursor != null) {
      jsonArray.remove(jsonArray.size() -1);

      return new JsonObject().put(type, jsonArray).put("next_cursor", next_cursor);
    } else {

      return new JsonObject().put(type, jsonArray).put("next_cursor", (String) null);
    }
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
