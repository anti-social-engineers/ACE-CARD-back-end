/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.models;

import io.reactiverse.pgclient.Tuple;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Card {

  private UUID id;
  private OffsetDateTime requested_at;
  private UUID card_owner;



  public Card(UUID userId) {
    this.id = UUID.randomUUID();
    this.requested_at = OffsetDateTime.now();
    this.card_owner = userId;
  }

  public Tuple toTuple() {
    return Tuple.of(id, requested_at, card_owner);
  }

  public UUID getId() {
    return id;
  }

  public OffsetDateTime getRequested_at() {
    return requested_at;
  }

  public UUID getCard_owner() {
    return card_owner;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public void setRequested_at(OffsetDateTime requested_at) {
    this.requested_at = requested_at;
  }

  public void setCard_owner(UUID card_owner) {
    this.card_owner = card_owner;
  }
}

