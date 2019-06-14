/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import com.google.gson.JsonSyntaxException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class StripeSignatureHandler implements Handler<RoutingContext> {

  private String stripeSigningKey;

  public StripeSignatureHandler(String stripeSecret) {
    // Each Stripe endpoint has a different secret
    this.stripeSigningKey = stripeSecret;
  }

  /**
   Handler to verify the signature of Stripe webhooks
   @param context contains information about the request
   @return void
   */
  @Override
  public void handle(RoutingContext context) {
    String payload = context.getBodyAsString();
    String sigHeader = context.request().getHeader("Stripe-Signature");
    Event event = null;

    try {
      event = Webhook.constructEvent(
        payload, sigHeader, this.stripeSigningKey
      );
    } catch (JsonSyntaxException | SignatureVerificationException e) {
      // Invalid payload or Invalid signature
      raise400(context);
      return;
    }

    System.out.println("Continuing...");
    context.next();

  }

  void raise400(RoutingContext context) {
    context.response()
      .setStatusCode(400)
      .putHeader("Cache-Control", "no-store, no-cache")
      .putHeader("X-Content-Type-Options", "nosniff")
      .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
      .putHeader("X-Download-Options", "noopen")
      .putHeader("X-XSS-Protection", "1; mode=block")
      .putHeader("X-FRAME-OPTIONS", "DENY")
      .putHeader("content-type", "application/json; charset=utf-8")
      .end();
  }
}
