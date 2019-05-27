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
public class TestMainVerticle {

  WebClient client;

  public TestMainVerticle(Vertx vertx) {
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
  @DisplayName("[GET] /api/users should return a 401 if no JWT is provided")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_401_users(Vertx vertx, VertxTestContext testContext) {
    client
      .get(8888, "localhost", "/api/users")
      .send(res -> {
        if (res.succeeded()) {
          HttpResponse<Buffer> response = res.result();

          assertEquals(401, response.statusCode());
          testContext.completeNow();
        }
      });
  }

  @Test
  @DisplayName("[POST] /api/register with invalid email should return a 422")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_422_register(Vertx vertx, VertxTestContext testContext) {
    client
      .post(8888, "localhost", "/api/register")
      .sendJsonObject(new JsonObject()
        .put("email", "InvalidEMal")
        .put("password", "A-Bad-Password"), res -> {
        if (res.succeeded()) {
          HttpResponse<Buffer> response = res.result();

          assertEquals(422, response.statusCode());
          testContext.completeNow();
        }
      });
  }
}

