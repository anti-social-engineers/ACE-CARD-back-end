/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.apierrors.ApiError;
import acecardapi.apierrors.FieldViolation;
import acecardapi.apierrors.InputFormatViolation;
import acecardapi.apierrors.ParameterNotFoundViolation;
import acecardapi.models.Address;
import acecardapi.models.Card;
import io.reactiverse.pgclient.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public class CardHandler extends AbstractCustomHandler{

  String[] requiredCardAttributes = new String[]{"address", "address_number", "address_annex", "city", "postalcode",
    "first_name", "last_name", "gender", "dob"};

  public CardHandler(PgPool dbClient, JsonObject config) {
    super(dbClient, config);
  }

  public void requestCard(RoutingContext context) {

    MultiMap attributes = context.request().formAttributes();

    // Check if all required fields are present:
    checkIfAttributesPresentCardRequest(attributes, attributeRes -> {

      if (attributeRes.succeeded()) {

        Set<FileUpload> uploads = context.fileUploads();

        checkIfProfileImagePresentCardRequest(uploads, fileRes -> {

          if (fileRes.succeeded()) {

            FileUpload file = fileRes.result();

            streetNumberIsInt(attributes.get("address_number"), intRes -> {

              if (intRes.succeeded()) {

                int address_number = intRes.result();
                String street = attributes.get("address");
                String annex = attributes.get("address_annex");
                String city = attributes.get("city");
                String postalcode = attributes.get("postalcode");

                Address address = new Address(
                  street,
                  address_number,
                  annex,
                  city,
                  postalcode
                );

                Card card = new Card(
                  UUID.fromString(context.user().principal().getString("sub"))
                );

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
                                  raise500(context);
                                } else {
                                  Row row = result.iterator().next();

                                  // Go to the next step: The user
                                  connection.close();
                                  processRequestCard(context, connection, row.getUUID("id"), card, file);
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
              } else {
                InputFormatViolation error = new InputFormatViolation("address_number");
                raise422(context, error);
              }
            });
          } else {
            InputFormatViolation error = new InputFormatViolation("profile_image");
            raise422(context, error);
          }
        });
      } else {
        ParameterNotFoundViolation error = new ParameterNotFoundViolation(attributeRes.result());
        raise422(context, error);
      }
    });
  }

  private void processRequestCard(RoutingContext context, PgConnection connection, UUID addressId, Card card,
                                  FileUpload profileImage) {

    // First we need to update the user
    updateUserEntry(context, connection, addressId, card, updateUserRes -> {
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
                raise500(context);
              }
            });

          } else {
            raise500(context);
            connection.close();
          }
        });
      } else {
        raise500(context);
        connection.close();
      }
    });
  }

  private void updateUserEntry(RoutingContext context, PgConnection connection, UUID addressId, Card card, Handler<AsyncResult<Boolean>> resultHandler) {
    // Transaction to update the user and create a card
    // TODO: ADD IMAGE (Add image here already, but only move it after all other operations succeeded)

    MultiMap attributes = context.request().formAttributes();
    String first_name = attributes.get("first_name");
    String last_name = attributes.get("last_name");
    String gender = attributes.get("gender");
    LocalDate dob = LocalDate.parse(attributes.get("dob"));
    UUID userId = UUID.fromString(context.user().principal().getString("sub"));

    connection.preparedQuery("UPDATE users SET first_name=$1, last_name=$2, gender=$3, date_of_birth=$4, address_id=$5 WHERE id=$6", Tuple.of(first_name, last_name, gender, dob, addressId, userId), res -> {
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

  private void checkIfAttributesPresentCardRequest(MultiMap attributes, Handler<AsyncResult<String>> resultHandler) {

    for (int i = 0; i < requiredCardAttributes.length; i++) {

      if (!attributes.contains(requiredCardAttributes[i])) {
        System.out.println("Attribute missing...");
        resultHandler.handle(Future.failedFuture(requiredCardAttributes[i]));
        break;
      }
    }
    resultHandler.handle(Future.succeededFuture(""));
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
      resultHandler.handle(Future.succeededFuture());
    } else {
      resultHandler.handle(Future.failedFuture("profile_image"));
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

  private void raise422(RoutingContext context, ApiError error) {

    context.response()
      .setStatusCode(422)
      .putHeader("Cache-Control", "no-store, no-cache")
      .putHeader("X-Content-Type-Options", "nosniff")
      .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
      .putHeader("X-Download-Options", "noopen")
      .putHeader("X-XSS-Protection", "1; mode=block")
      .putHeader("X-FRAME-OPTIONS", "DENY")
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(error.errorJson()));
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

