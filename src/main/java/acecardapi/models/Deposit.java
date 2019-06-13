/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.models;

import io.reactiverse.pgclient.Tuple;
import io.vertx.core.json.JsonObject;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Deposit {

  private UUID id;
  private Double amount;
  private OffsetDateTime deposited_at;
  private UUID cardId;

  public Deposit(Double amount, UUID cardId) {
    this.id = UUID.randomUUID();
    this.amount = amount;
    this.deposited_at = OffsetDateTime.now();
    this.cardId = cardId;
  }

  public Deposit(UUID id, Double amount, OffsetDateTime deposited_at) {
    this.id = id;
    this.amount = amount;
    this.deposited_at = deposited_at;
  }

  public JsonObject toJsonObject() {
    return new JsonObject()
      .put("id", id.toString())
      .put("amount", amount.toString())
      .put("time", deposited_at.toString());
  }

  public Tuple toTuple() {
    return Tuple.of(id, amount, deposited_at, cardId);
  }

  public UUID getId() {
    return id;
  }
}

