/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.models;

import io.vertx.core.json.JsonObject;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Deposit {

  private UUID id;
  private Double amount;
  private OffsetDateTime paid_at;

  public Deposit(UUID id, Double amount, OffsetDateTime paid_at) {
    this.id = id;
    this.amount = amount;
    this.paid_at = paid_at;
  }

  public JsonObject toJsonObject() {
    return new JsonObject()
      .put("id", id.toString())
      .put("amount", amount.toString())
      .put("time", paid_at.toString());
  }

}

