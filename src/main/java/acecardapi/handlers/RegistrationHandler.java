/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import acecardapi.apierrors.InputFormatViolation;
import acecardapi.apierrors.UniqueViolation;
import acecardapi.auth.ReactiveAuth;
import acecardapi.models.Users;
import acecardapi.utils.RandomToken;
import io.reactiverse.pgclient.PgException;
import io.reactiverse.pgclient.PgPool;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;

import java.util.UUID;

public class RegistrationHandler extends AbstractCustomHandler {

  private ReactiveAuth authProvider;
  private RedisClient redisClient;
  private MailClient mailClient;

  public RegistrationHandler(PgPool dbClient, JsonObject config, ReactiveAuth authProvider, RedisClient redisClient,
                             MailClient mailClient) {
    super(dbClient, config);
    this.authProvider = authProvider;
    this.redisClient = redisClient;
    this.mailClient = mailClient;
  }

  public void registerUser(RoutingContext context) {

    try {

      Users users = Json.decodeValue(context.getBodyAsString(), Users.class);

      this.createUser(context, users);

    } catch (Exception e) {

      if (e.getMessage().contains("email_format_invalid")) {

        // Email address has invalid format

        InputFormatViolation error = new InputFormatViolation("email_address");
        context.response().setStatusCode(422).end(Json.encodePrettily(error.errorJson()));

      }

      context.response().setStatusCode(500).end("");

    }

  }

  private void createUser(RoutingContext context, Users users) {

    // Generate the salt, and combine it with the password to create the hashed password
    String salt = authProvider.generateSalt();
    String hash = authProvider.computeHash(users.getPassword(), salt);

    users.setPassword(hash);
    users.setPassword_salt(salt);

    dbClient.preparedQuery("INSERT INTO users (id, email, password, password_salt) VALUES ($1, $2, $3, $4)", users.toTuple(), res -> {
      if (res.succeeded()) {

        RandomToken token = new RandomToken(32);

        generateRedisKey(token, users.getId(), redisKeyResult -> {

          if (redisKeyResult.succeeded()) {
            String key = redisKeyResult.result();

            MailMessage message = buildRegistrationMail(users.getEmail(), key);

            context.response().putHeader("content-type", "application/json; charset=utf-8").setStatusCode(201).end();
            // TODO: Response pas na mail?
            mailClient.sendMail(message, result -> {
              if (result.succeeded()) {

                System.out.println(result.result());
                return;


              } else {

                // Account created, but mail did not get send.
                result.cause().printStackTrace();
                System.out.println(result.cause().toString());
                return;

              }
            });

          } else {
            context.response().putHeader("content-type", "application/json; charset=utf-8").setStatusCode(500).end();
          }
        });
      } else {

        if (res.cause() instanceof PgException) {
          String error_Code = ((PgException) res.cause()).getCode();

          // PostgreSQL error 23505 (e.g. unique constraint failure)
          if (error_Code.equals("23505")) {

            UniqueViolation error = new UniqueViolation("email_address");

            context.response().setStatusCode(409).putHeader("content-type", "application/json; charset=utf-8").end(Json.encode(error.errorJson()));
            return;
          }
        }
        System.out.println(res.cause().toString());
        context.response().setStatusCode(400).putHeader("content-type", "application/json; charset=utf-8").end(Json.encode("Something went wrong."));
      }
    });
  }

  private void generateRedisKey(RandomToken token, UUID userId, Handler<AsyncResult<String>> resultHandler) {

    String tokenValue = token.nextString();

    redisClient.exists(tokenValue, redisExist -> {
      if (redisExist.succeeded()) {
        if (redisExist.result() == 1) {
          generateRedisKey(token, userId, resultHandler);
        }
        else {
          insertRedisKey(tokenValue, userId, resultHandler);
        }
      } else {
        resultHandler.handle(Future.failedFuture(""));
      }
    });
  }

  private void insertRedisKey(String tokenValue, UUID userId, Handler<AsyncResult<String>> resultHandler) {

    redisClient.set(tokenValue, userId.toString(), res -> {
      if (res.succeeded()) {
        redisClient.expire(tokenValue, this.config.getLong("registration.code_expire_time", 32400L), expireRes -> {
          if (expireRes.succeeded()) {
            resultHandler.handle(Future.succeededFuture(tokenValue));
          } else {
            resultHandler.handle(Future.failedFuture(""));
          }
        });
      } else {
        resultHandler.handle(Future.failedFuture(""));
      }
    });

  }

  private MailMessage buildRegistrationMail(String destinationMail, String registrationKey) {

    String html = String.format("" +
      "Beste klant, <br/><br/>" +
      "Bedankt voor het registreren van uw account. <br/>" +
      "U moet uw account nog activeren, dit kunt u doen door op de onderstaande link te klikken: <br/>" +
      "%s", registrationKey);

    MailMessage message = new MailMessage();
    message.setFrom("noreply@aceofclubs.nl");
    message.setTo(destinationMail);
    message.setSubject("Email verificatie - ACE Card.");
    message.setHtml(html);

    return message;
  }
}
