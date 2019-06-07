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
import acecardapi.models.Address;
import acecardapi.models.Card;
import acecardapi.models.CardRequest;
import acecardapi.utils.DTuple;
import io.reactiverse.pgclient.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static acecardapi.utils.RequestUtilities.attributesCheckJsonObject;
import static acecardapi.utils.RequestUtilities.attributesCheckMultiMap;
import static java.util.Map.entry;

public class CardHandler extends AbstractCustomHandler{

  ReactiveAuth authProvider;

  private String[] requiredCardAttributes = new String[]{"address", "address_number", "address_annex", "city", "postalcode",
    "first_name", "last_name", "gender", "dob"};

  private String[] requiredLinkAttributes = new String[]{"card_code", "email"};

  private Map<String, DTuple<Integer, Integer>> attributeCardLengthMap = Map.ofEntries(
    entry("address", new DTuple<Integer, Integer>(3, 255)),
    entry("address_number", new DTuple<Integer, Integer>(1, 4)),
    entry("address_annex", new DTuple<Integer, Integer>(0, 1)),
    entry("city", new DTuple<Integer, Integer>(2, 255)),
    entry("postalcode", new DTuple<Integer, Integer>(6, 6)),
    entry("first_name", new DTuple<Integer, Integer>(2, 255)),
    entry("last_name", new DTuple<Integer, Integer>(2, 255)),
    entry("gender", new DTuple<Integer, Integer>(1, 1)),
    entry("dob", new DTuple<Integer, Integer>(10, 10))
    );

  private String[] allowedContentTypes = new String[]{"image/png", "image/jpeg"};

  public CardHandler(PgPool dbClient, JsonObject config, ReactiveAuth authProvider) {
    super(dbClient, config);
    this.authProvider = authProvider;
  }

  public void requestCard(RoutingContext context) {

    MultiMap attributes = context.request().formAttributes();

    // Check if all required fields are present:
    attributesCheckMultiMap(attributes, requiredCardAttributes, attributeRes -> {

      if (attributeRes.succeeded()) {

        // Check of alle attributen de min/max lengte hebben:
        checkIfStringsValid(attributes, attributeLengthRes -> {

          if (attributeLengthRes.succeeded()) {

            Set<FileUpload> uploads = context.fileUploads();

            // Check of de correcte file is geupload (correcte parameter):
            checkIfProfileImagePresentCardRequest(uploads, fileRes -> {

              if (fileRes.succeeded()) {

                FileUpload file = fileRes.result();

                // Check of de content-type van de file wel png of jpg is (Als er hier door komen maakt het niet
                // veel uit, files in de upload directory worden niet uitgevoerd)
                checkFileContentType(file, fileContentRes -> {

                  if (fileContentRes.succeeded()) {

                    // Controleren of de
                    streetNumberIsInt(attributes.get("address_number"), intRes -> {

                      if (intRes.succeeded()) {

                        int address_number = intRes.result();
                        String street = attributes.get("address");
                        String annex = attributes.get("address_annex");
                        String city = attributes.get("city");
                        String postalcode = attributes.get("postalcode");
                        UUID userId = UUID.fromString(context.user().principal().getString("sub"));

                        Address address = new Address(
                          street,
                          address_number,
                          annex,
                          city,
                          postalcode
                        );

                        Card card = new Card(
                          userId
                        );

                        dbClient.getConnection(getConnection -> {
                          if (getConnection.succeeded()) {
                            PgConnection connection = getConnection.result();

                            connection.preparedQuery("SELECT id FROM cards WHERE EXISTS (SELECT * FROM users WHERE users.id = cards.user_id_id and users.id = $1)", Tuple.of(userId), hasCardRes -> {

                              if (hasCardRes.succeeded()) {

                                if (hasCardRes.result().rowCount() <= 0) {

                                  connection.preparedQuery("INSERT INTO addresses (id, address, address_num, address_annex, city, postalcode, country) VALUES ($1, $2, $3, $4, $5, $6, $7)", address.toTupleWithId(), query_address -> {
                                    if (query_address.succeeded()) {

                                      processRequestCard(context, connection, address.getId(), card, file);

                                    } else {

                                      if (query_address.cause() instanceof PgException) {
                                        String error_Code = ((PgException) query_address.cause()).getCode();

                                        // PostgreSQL error 23505 (e.g. unique constraint failure)
                                        if (error_Code.equals("23505")) {

                                          connection.preparedQuery("SELECT id FROM addresses WHERE address=$1 AND address_num=$2 AND address_annex=$3 AND city=$4 AND postalcode=$5 AND country=$6", address.toTupleNoId(), query_get_address -> {

                                            if (query_get_address.succeeded()) {

                                              PgRowSet result = query_get_address.result();

                                              if (result.rowCount() != 1) {
                                                connection.close();
                                                raise500(context, query_address.cause());
                                              } else {
                                                Row row = result.iterator().next();

                                                // Go to the next step: The user
                                                processRequestCard(context, connection, row.getUUID("id"), card, file);
                                              }
                                            } else {
                                              raise500(context, query_address.cause());
                                              connection.close();
                                            }
                                          });
                                        } else {
                                          raise500(context, query_address.cause());
                                          connection.close();
                                        }
                                      }
                                    }
                                  });
                                } else {
                                  // Already has an ace card
                                  context.response()
                                    .setStatusCode(409)
                                    .putHeader("Cache-Control", "no-store, no-cache")
                                    .putHeader("X-Content-Type-Options", "nosniff")
                                    .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
                                    .putHeader("X-Download-Options", "noopen")
                                    .putHeader("X-XSS-Protection", "1; mode=block")
                                    .putHeader("X-FRAME-OPTIONS", "DENY")
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end();
                                  connection.close();
                                }
                              } else {
                                raise500(context, hasCardRes.cause());
                                connection.close();
                              }
                            });
                          } else {
                            raise500(context, getConnection.cause());
                          }
                        });
                      } else {
                        InputFormatViolation error = new InputFormatViolation("address_number");
                        raise422(context, error);
                      }
                    });
                  } else {
                    FileFormatViolation error = new FileFormatViolation("profile_image", fileContentRes.cause().getMessage());
                    raise422(context, error);
                  }
                });
              } else {
                ParameterNotFoundViolation error = new ParameterNotFoundViolation("profile_image");
                raise422(context, error);
              }
            });
          } else {
            InputLengthFormatViolation error = new InputLengthFormatViolation(attributeLengthRes.cause().getMessage());
            raise422(context, error);
          }
        });
      } else {
        ParameterNotFoundViolation error = new ParameterNotFoundViolation(attributeRes.cause().getMessage());
        raise422(context, error);
      }
    });
  }

  private void processRequestCard(RoutingContext context, PgConnection connection, UUID addressId, Card card,
                                  FileUpload profileImage) {

    // First we need to update the user
    updateUserEntry(context, connection, addressId, profileImage, updateUserRes -> {
      if (updateUserRes.succeeded()) {

        // User has been updated, now we need to create his card:
        createCardEntry(connection, card, createCardRes -> {
          if (createCardRes.succeeded()) {

            // First return the connection to the pool:
            connection.close();

            // And finally move the profile image to the correct directory:
            moveFile(profileImage, imageMoveRes -> {
              if (imageMoveRes.succeeded()) {

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

              } else {
                raise500(context, imageMoveRes.cause());
              }
            });

          } else {
            raise500(context, createCardRes.cause());
            connection.close();
          }
        });
      } else {
        raise500(context, updateUserRes.cause());
        connection.close();
      }
    });
  }

  private void updateUserEntry(RoutingContext context, PgConnection connection, UUID addressId, FileUpload file, Handler<AsyncResult<Boolean>> resultHandler) {
    // Transaction to update the user and create a card

    MultiMap attributes = context.request().formAttributes();
    String first_name = attributes.get("first_name");
    String last_name = attributes.get("last_name");
    String gender = attributes.get("gender");
    LocalDate dob = LocalDate.parse(attributes.get("dob"));
    UUID userId = UUID.fromString(context.user().principal().getString("sub"));
    Path uploadedDir = Paths.get(file.uploadedFileName());
    UUID imageId = UUID.fromString(uploadedDir.getFileName().toString());

    connection.preparedQuery("UPDATE users SET first_name=$1, last_name=$2, gender=$3, date_of_birth=$4, address_id=$5, image_id=$6 WHERE id=$7", Tuple.of(first_name, last_name, gender, dob, addressId, imageId, userId), res -> {
      if(res.succeeded()) {
        resultHandler.handle(Future.succeededFuture(true));
      } else {
        resultHandler.handle(Future.failedFuture("Updating User entry failed: " + res.cause().toString()));
      }
    });
  }


  private void createCardEntry(PgConnection connection, Card card, Handler<AsyncResult<Boolean>> resultHandler) {
    connection.preparedQuery("INSERT INTO cards (id, requested_at, user_id_id) VALUES ($1, $2, $3)", card.toTuple(), res -> {
      if(res.succeeded()) {
        resultHandler.handle(Future.succeededFuture(true));
      } else {
        resultHandler.handle(Future.failedFuture("Creating AceCard entry failed: " + res.cause().toString()));
      }
    });
  }

  private void checkIfProfileImagePresentCardRequest(Set<FileUpload> uploads, Handler<AsyncResult<FileUpload>> resultHandler) {
    // Check if the required file is present.

    FileUpload correctFile = null;

    for (FileUpload file: uploads) {
      if (file.name().equals("profile_image")) {
        correctFile = file;
        break;
      }
    }

    if (correctFile != null) {
      resultHandler.handle(Future.succeededFuture(correctFile));
    } else {
      resultHandler.handle(Future.failedFuture("profile_image"));
    }

  }

  private void checkIfStringsValid(MultiMap attributes, Handler<AsyncResult<String>> resultHandler) {

    boolean failure = false;
    String inspecting = "";
    String key = "";

    for (Map.Entry<String, DTuple<Integer, Integer>> entry : attributeCardLengthMap.entrySet()) {

      inspecting = attributes.get(entry.getKey());

      if(inspecting.length() < entry.getValue().frst || inspecting.length() > entry.getValue().scnd) {
        failure = true;
        key = entry.getKey();
        break;
      }
    }

    if (failure) {
      resultHandler.handle(Future.failedFuture(key));
    } else {
      resultHandler.handle(Future.succeededFuture(""));
    }
  }

  private void streetNumberIsInt(String number, Handler<AsyncResult<Integer>> resultHandler) {
    try {
      int address_number  = Integer.parseInt(number);
      resultHandler.handle(Future.succeededFuture(address_number));
    } catch (NumberFormatException e) {
      resultHandler.handle(Future.failedFuture(""));
    }
  }

  private void checkFileContentType(FileUpload file, Handler<AsyncResult<String>> resultHandler) {

    boolean invalidContentType = true;

    for (int i = 0; i < allowedContentTypes.length; i++) {

      if(file.contentType().equals(allowedContentTypes[i])) {
        invalidContentType = false;
        break;
      }
    }

    if(invalidContentType) {
      resultHandler.handle(Future.failedFuture(file.contentType()));
    } else {
      resultHandler.handle(Future.succeededFuture(file.contentType()));
    }
  }

  public void requestRequestedCards(RoutingContext context) {

    gatherUserCardData(res -> {

      if (res.succeeded()) {

        JsonArray jsonArray = res.result();

        context.response()
          .setStatusCode(200)
          .putHeader("Cache-Control", "no-store, no-cache")
          .putHeader("X-Content-Type-Options", "nosniff")
          .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
          .putHeader("X-Download-Options", "noopen")
          .putHeader("X-XSS-Protection", "1; mode=block")
          .putHeader("X-FRAME-OPTIONS", "DENY")
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(jsonArray));

      } else {
        // DB issue
        raise500(context, res.cause());
      }

    });

  }

  private void gatherUserCardData(Handler<AsyncResult<JsonArray>> resultHandler) {

    dbClient.query("SELECT ca.id, ca.requested_at, u.email, u.first_name, u.last_name, u.date_of_birth, u.gender, u.image_id FROM cards as ca, users as u WHERE ca.is_activated = false AND ca.user_id_id = u.id ORDER BY ca.requested_at ASC LIMIT 10", res -> {
      if (res.succeeded()) {

        PgRowSet results = res.result();
        JsonArray jsonArray = new JsonArray();

        for (Row row: results) {

          CardRequest cardRequest = new CardRequest(
            row.getUUID("id"),
            row.getOffsetDateTime("requested_at"),
            row.getString("email"),
            row.getString("first_name"),
            row.getString("last_name"),
            row.getLocalDate("date_of_birth"),
            row.getString("gender"),
            row.getUUID("image_id"),
            config.getString("http.image_dir", "static/images/")
          );

          jsonArray.add(cardRequest.toJson());

        }
        resultHandler.handle(Future.succeededFuture(jsonArray));
      } else {
        resultHandler.handle(Future.failedFuture(""));
      }
    });

  }

  public void linkCardUser(RoutingContext context) {

    attributesCheckJsonObject(context.getBodyAsJson(), requiredLinkAttributes, attributeCheckRes -> {
      if (attributeCheckRes.succeeded()) {

        processLinkCardUser(context);

      } else {
        // An attribute is missing:
        ParameterNotFoundViolation error = new ParameterNotFoundViolation(attributeCheckRes.cause().getMessage());
        raise422(context, error);
      }
    });
  }

  private void processLinkCardUser(RoutingContext context) {
    //TODO: GEEN CHECK OF USER AL GEEN CARD HEEFT?

    JsonObject jsonBody = context.getBodyAsJson();
    Card card = new Card(jsonBody.getString("card_code"), authProvider);
    String email = jsonBody.getString("email");

    // First get the card from the user
    // Then update it
    dbClient.getConnection(getConnectionRes -> {
      if (getConnectionRes.succeeded()) {

        PgConnection connection = getConnectionRes.result();

        connection.preparedQuery("SELECT id FROM users WHERE email=$1", Tuple.of(email), userRes -> {

          if (userRes.succeeded()) {

            Row userRow = userRes.result().iterator().next();
            UUID userId = userRow.getUUID("id");

            connection.preparedQuery("UPDATE CARDS SET card_code=$1, pin=$2, pin_salt=$3 WHERE user_id_id=$4", Tuple.of(card.getCard_code(), card.getHashedPin(), card.getSalt(), userId), cardRes -> {
              if (cardRes.succeeded()) {

                // Generate json response:
                JsonObject jsonObject = new JsonObject()
                  .put("pin", card.getPin());

                context.response()
                  .setStatusCode(201)
                  .putHeader("Cache-Control", "no-store, no-cache")
                  .putHeader("X-Content-Type-Options", "nosniff")
                  .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
                  .putHeader("X-Download-Options", "noopen")
                  .putHeader("X-XSS-Protection", "1; mode=block")
                  .putHeader("X-FRAME-OPTIONS", "DENY")
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(Json.encodePrettily(jsonObject));

                connection.close();

              } else {
                raise500(context, cardRes.cause());
                connection.close();
              }
            });


          } else {
            raise500(context, userRes.cause());
            connection.close();
          }
        });
      } else {
        // DB error
        raise500(context, getConnectionRes.cause());
      }
    });

  }

  private void moveFile(FileUpload file, Handler<AsyncResult<Boolean>> resultHandler) {
    // Move the uploaded file to it's permanent directory

    Path uploadedDir = Paths.get(file.uploadedFileName());

    Path destDir = Paths.get(config.getString("http.image_dir", "static/images/") + uploadedDir.getFileName());

    try {
      Files.move(uploadedDir, destDir, StandardCopyOption.REPLACE_EXISTING);
      resultHandler.handle(Future.succeededFuture(true));
    } catch (IOException e) {
      resultHandler.handle(Future.failedFuture("Moving profile image failed: " + e.toString()));
    }
  }
}
