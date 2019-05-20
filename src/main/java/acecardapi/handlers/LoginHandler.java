package acecardapi.handlers;

import acecardapi.auth.ReactiveAuth;
import acecardapi.auth.ReactiveUser;
import acecardapi.models.Users;
import io.reactiverse.pgclient.PgPool;
import io.vertx.core.json.Json;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

public class LoginHandler {

  private PgPool dbClient;
  private ReactiveAuth dbAuth;

  public LoginHandler(PgPool dbClient, ReactiveAuth dbAuth) {
    this.dbClient = dbClient;
    this.dbAuth = dbAuth;
  }

  public void login(RoutingContext context) {

    Users users = null;

    try{
      users = Json.decodeValue(context.getBodyAsString(), Users.class);
    } catch (Exception e) {
      // TODO: Handle invalid email - Failed to decode: email_format_invalid
      System.out.println(e.getMessage().toString());

      context.response().setStatusCode(400)
        .putHeader("content-type", "application/json; charset=utf-8"
        ).end("Sorry! Ik heb nog geen response....");

      return;

    }

    dbAuth.authenticate(users.toJsonObjectLogin(), res -> {
      if(res.succeeded()) {
        User logged_user = res.result();

        // Get the user email
        System.out.println(logged_user.principal());

        context.response().setStatusCode(200).end();
      }
    });

    System.out.println(users.getEmail());

    context.response()
      .putHeader("content-type", "application/json; charset=utf-8"
      ).end("Sorry! Ik heb nog geen response....");

  }

}
