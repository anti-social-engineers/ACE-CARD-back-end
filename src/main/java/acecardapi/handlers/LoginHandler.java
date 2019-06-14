/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.apierrors.EmailNotVerifiedViolation;
import acecardapi.apierrors.InputFormatViolation;
import acecardapi.auth.ReactiveAuth;
import acecardapi.models.JwtToken;
import acecardapi.models.Users;
import io.reactiverse.pgclient.*;
import io.sentry.Sentry;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

public class LoginHandler extends AbstractCustomHandler{

  private ReactiveAuth dbAuth;
  private JWTAuth jwtProvider;

  public LoginHandler(PgPool dbClient, JsonObject config, ReactiveAuth dbAuth, JWTAuth jwtProvider) {
    super(dbClient, config);
    this.dbAuth = dbAuth;
    this.jwtProvider = jwtProvider;
  }

  public void login(RoutingContext context) {

    Users users = null;

    try{
      users = Json.decodeValue(context.getBodyAsString(), Users.class);
    } catch (Exception e) {

      if (e.getMessage().contains("email_format_invalid")) {

        // Email address has invalid format

        InputFormatViolation error = new InputFormatViolation("email_address");
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

      raise500(context, e);

      return;

    }

    // Verify if email is activated

    dbAuth.authenticate(users.toJsonObjectLogin(), userRes -> {

      if(userRes.succeeded()) {

        User logged_user = userRes.result();

        UUID id = UUID.fromString(logged_user.principal().getString("id"));


        dbClient.getConnection(getConnectionRes -> {

          if (getConnectionRes.succeeded()) {

            PgConnection connection = getConnectionRes.result();

            connection.preparedQuery("SELECT is_email_verified, role, image_id FROM users WHERE id=$1", Tuple.of(id), res -> {
              if (res.succeeded()) {
                PgRowSet result = res.result();

                if (result.rowCount() == 0 || result.rowCount() >= 2) {
                  // No user or more than 1 user was found with this id, should be impossible to end up here since we
                  // verify the JWT signature, but we need to handle it just in case.

                  context.response()
                    .putHeader("Cache-Control", "no-store, no-cache")
                    .putHeader("X-Content-Type-Options", "nosniff")
                    .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
                    .putHeader("X-Download-Options", "noopen")
                    .putHeader("X-XSS-Protection", "1; mode=block")
                    .putHeader("X-FRAME-OPTIONS", "DENY")
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .setStatusCode(500)
                    .end();

                  connection.close();

                } else {

                  // Check if the user has activated their email:
                  Row row = result.iterator().next();

                  if (!row.getBoolean("is_email_verified")) {
                    // TODO: Handle sending new activation email or something....

                    EmailNotVerifiedViolation error = new EmailNotVerifiedViolation("email_not_activated");
                    context.response()
                      .putHeader("Cache-Control", "no-store, no-cache")
                      .putHeader("X-Content-Type-Options", "nosniff")
                      .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
                      .putHeader("X-Download-Options", "noopen")
                      .putHeader("X-XSS-Protection", "1; mode=block")
                      .putHeader("X-FRAME-OPTIONS", "DENY")
                      .putHeader("content-type", "application/json; charset=utf-8")
                      .setStatusCode(403)
                      .end(Json.encodePrettily(error.errorJson()));

                    connection.close();

                  } else {

                    String profile_image;
                    var db_profile_image = row.getUUID("image_id");
                    if (db_profile_image != null) {
                      profile_image = db_profile_image.toString();
                    } else
                      profile_image = "";

                    if (row.getString("role").equals("club_employee")) {

                      connection.preparedQuery("SELECT * FROM clubs WHERE owner_id=$1", Tuple.of(id), clubRes -> {

                        if (clubRes.succeeded()) {

                          PgRowSet clubResults = clubRes.result();

                          if (clubResults.rowCount() <= 0) {
                            raise500(context, new Exception("User has club_employee role, but no clubs are" +
                              " associated with this user."));
                          } else {

                            JsonArray clubs = new JsonArray();
                            for (Row clubRow: clubResults) {

                              clubs.add(clubRow.getUUID("id").toString());

                            }

                            String token = generateJwt(id.toString(), row.getString("role"), profile_image, clubs);

                            context.response()
                              .setStatusCode(200)
                              .putHeader("Cache-Control", "no-store, no-cache")
                              .putHeader("X-Content-Type-Options", "nosniff")
                              .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
                              .putHeader("X-Download-Options", "noopen")
                              .putHeader("X-XSS-Protection", "1; mode=block")
                              .putHeader("X-FRAME-OPTIONS", "DENY")
                              .putHeader("content-type", "application/json; charset=utf-8")
                              .end(Json.encodePrettily(new JwtToken(token)));

                            connection.close();

                          }

                        } else {
                          raise500(context, clubRes.cause());

                          connection.close();
                        }

                      });

                    } else {
                      String token = generateJwt(id.toString(), row.getString("role"), profile_image);

                      context.response()
                        .setStatusCode(200)
                        .putHeader("Cache-Control", "no-store, no-cache")
                        .putHeader("X-Content-Type-Options", "nosniff")
                        .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
                        .putHeader("X-Download-Options", "noopen")
                        .putHeader("X-XSS-Protection", "1; mode=block")
                        .putHeader("X-FRAME-OPTIONS", "DENY")
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(new JwtToken(token)));

                      connection.close();
                    }
                  }
                }
              } else {
                // Return a 500 error (Something went wrong connecting with the db/executing the query)
                raise500(context, res.cause());

                connection.close();
              }
            });
          } else {
            raise500(context, getConnectionRes.cause());
          }
        });

      }

      else {
        context.response()
          .putHeader("content-type", "application/json; charset=utf-8")
          .setStatusCode(401)
          .putHeader("Cache-Control", "no-store, no-cache")
          .putHeader("X-Content-Type-Options", "nosniff")
          .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
          .putHeader("X-Download-Options", "noopen")
          .putHeader("X-XSS-Protection", "1; mode=block")
          .putHeader("X-FRAME-OPTIONS", "DENY")
          .putHeader("content-type", "application/json; charset=utf-8")
          .end();
      }
    });
  }

  private String generateJwt(String userId, String role, String profileImage) {
    return jwtProvider.generateToken(new JsonObject()
        .put("sub", userId)
        .put("permissions", new JsonArray().add(role))
        .put("profile_image", profileImage),
      new JWTOptions().setExpiresInSeconds(config.getInteger("jwt.exptime", 32400)));
  }

  private String generateJwt(String userId, String role, String profileImage, JsonArray clubs) {
    return jwtProvider.generateToken(new JsonObject()
        .put("sub", userId)
        .put("permissions", new JsonArray().add(role))
        .put("profile_image", profileImage)
        .put("clubs", clubs),
      new JWTOptions().setExpiresInSeconds(config.getInteger("jwt.exptime", 32400)));
  }

}
