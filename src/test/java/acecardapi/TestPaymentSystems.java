package acecardapi;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestPaymentSystems {

  private WebClient client;

  public TestPaymentSystems(Vertx vertx) {
    this.client =  WebClient.create(vertx);
  }

  @BeforeAll
  @DisplayName("Deploy the main verticle")
  static void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  @DisplayName("Should start a Web Server on port 8888")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_server_started(Vertx vertx, VertxTestContext testContext) {
    client
      .get(8888, "localhost", "/api/users")
      .send(res -> {
        if (res.succeeded()) {
          testContext.completeNow();
        }
      });
  }

  @Test
  @DisplayName("[GET] Unauthorized (NO JWT)")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_unauthorized_jwt(Vertx vertx, VertxTestContext testContext) {
    client
      .get(8888, "localhost", "/api/club/payment")
      .send(res -> {
        if (res.succeeded()) {
          HttpResponse response = res.result();

          assertEquals(401, response.statusCode());
          testContext.completeNow();
        }
      });
  }

  @Test
  @DisplayName("[POST] Payment with no valid JSON body")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_422_payment(Vertx vertx, VertxTestContext testContext) {
    client
      .post(8888, "localhost", "/api/login")
      .sendJsonObject(new JsonObject()
        .put("email", "owner@aceofclubs.nl")
        .put("password", "helloworld123"), res -> {
        if (res.succeeded()) {
          HttpResponse response = res.result();

          client
            .post(8888, "localhost", "/api/club/payment")
            .putHeader("Authorization", "Bearer" + response.bodyAsJsonObject().getString("jsonWebToken"))
            .sendJsonObject(new JsonObject(), res2 -> {

              HttpResponse response2 = res2.result();

              assertEquals(422, response2.statusCode());
              testContext.completeNow();
            });
        }
      });
  }

  @Test
  @DisplayName("[POST] Payment with valid JSON body, sufficient funds")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_201_payment_sufficient(Vertx vertx, VertxTestContext testContext) {
    client
      .post(8888, "localhost", "/api/login")
      .sendJsonObject(new JsonObject()
        .put("email", "owner@aceofclubs.nl")
        .put("password", "helloworld123"), res -> {
        if (res.succeeded()) {
          HttpResponse response = res.result();

          client
            .post(8888, "localhost", "/api/club/payment")
            .putHeader("Authorization", "Bearer" + response.bodyAsJsonObject().getString("jsonWebToken"))
            .sendJsonObject(new JsonObject()
              .put("club_id", "2d466140-f70b-4ac3-8156-ee922657bacd")
              .put("card_code", "cB7K+6hwm+dZCBmoNT76N7CPONRFTepfWql3jQ7n9+g=0000")
              .put("card_pin", "5900")
              .put("amount", 10), res2 -> {

              HttpResponse response2 = res2.result();

              assertEquals(201, response2.statusCode());
              testContext.completeNow();
            });
        }
      });
  }

  @Test
  @DisplayName("[POST] Payment with valid JSON body, insufficient funds")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_400_payment_insufficient(Vertx vertx, VertxTestContext testContext) {
    client
      .post(8888, "localhost", "/api/login")
      .sendJsonObject(new JsonObject()
        .put("email", "owner@aceofclubs.nl")
        .put("password", "helloworld123"), res -> {
        if (res.succeeded()) {
          HttpResponse response = res.result();

          client
            .post(8888, "localhost", "/api/club/payment")
            .putHeader("Authorization", "Bearer" + response.bodyAsJsonObject().getString("jsonWebToken"))
            .sendJsonObject(new JsonObject()
              .put("club_id", "2d466140-f70b-4ac3-8156-ee922657bacd")
              .put("card_code", "WNDBvlAKKzxuiNTaWLBvzI54Hiw4EuapQBz4y4HTnaU=0000")
              .put("card_pin", "5900")
              .put("amount", 10), res2 -> {

              HttpResponse response2 = res2.result();

              assertEquals(400, response2.statusCode());
              testContext.completeNow();
            });
        }
      });
  }

  @Test
  @DisplayName("[POST] Payment with valid JSON body, blocked card")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_403_payment_blocked(Vertx vertx, VertxTestContext testContext) {
    client
      .post(8888, "localhost", "/api/login")
      .sendJsonObject(new JsonObject()
        .put("email", "owner@aceofclubs.nl")
        .put("password", "helloworld123"), res -> {
        if (res.succeeded()) {
          HttpResponse response = res.result();

          response.bodyAsJsonObject().getString("jsonWebToken");

          client
            .post(8888, "localhost", "/api/club/payment")
            .putHeader("Authorization", "Bearer" + response.bodyAsJsonObject().getString("jsonWebToken"))
            .sendJsonObject(new JsonObject()
              .put("club_id", "2d466140-f70b-4ac3-8156-ee922657bacd")
              .put("card_code", "nu3lP0Ez0Kyv/pivqMzo+3JZZHF0V9N2HWlEn458414=0000")
              .put("card_pin", "5900")
              .put("amount", 10), res2 -> {

              HttpResponse response2 = res2.result();

              assertEquals(403, response2.statusCode());
              assertEquals(response2.bodyAsJsonObject().getString("error_type"), "blocked");
              testContext.completeNow();
            });
        }
      });
  }

  @Test
  @DisplayName("[POST] Payment with valid JSON body, invalid PIN")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_401_payment_invalid_PIN(Vertx vertx, VertxTestContext testContext) {
    client
      .post(8888, "localhost", "/api/login")
      .sendJsonObject(new JsonObject()
        .put("email", "owner@aceofclubs.nl")
        .put("password", "helloworld123"), res -> {
        if (res.succeeded()) {
          HttpResponse response = res.result();

          client
            .post(8888, "localhost", "/api/club/payment")
            .putHeader("Authorization", "Bearer" + response.bodyAsJsonObject().getString("jsonWebToken"))
            .sendJsonObject(new JsonObject()
              .put("club_id", "2d466140-f70b-4ac3-8156-ee922657bacd")
              .put("card_code", "cB7K+6hwm+dZCBmoNT76N7CPONRFTepfWql3jQ7n9+g=0000")
              .put("card_pin", "5901")
              .put("amount", 10), res2 -> {

              HttpResponse response2 = res2.result();

              assertEquals(401, response2.statusCode());
              assertEquals(response2.bodyAsJsonObject().getString("error_type"), "authorisation_violation");
              testContext.completeNow();
            });
        }
      });
  }

}

