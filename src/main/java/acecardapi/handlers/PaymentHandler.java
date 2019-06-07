/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import com.stripe.model.Charge;
import io.reactiverse.pgclient.PgPool;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.HashMap;
import java.util.Map;

public class PaymentHandler extends AbstractCustomHandler {

  public PaymentHandler(PgPool dbClient, JsonObject config) {
    super(dbClient, config);
  }

  public void createStripeCharge(RoutingContext context) {
    // todo: add check if fields are present
    try {
      String source = context.getBodyAsJson().getString("source");
      String amount = context.getBodyAsJson().getString("amount");

      Map<String, Object> params = new HashMap<String, Object>();
      params.put("amount", amount);
      params.put("currency", "eur");
      params.put("source", "source");

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
