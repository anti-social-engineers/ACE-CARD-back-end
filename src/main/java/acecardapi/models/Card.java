/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.models;

import acecardapi.auth.ReactiveAuth;
import io.reactiverse.pgclient.Tuple;
import io.vertx.ext.auth.AuthProvider;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

public class Card {

  private UUID id;
  private OffsetDateTime requested_at;
  private UUID card_owner;
  private String pin;
  private String card_code;
  private String hashedPin;
  private String salt;

  ReactiveAuth authProvider;

  public Card(UUID userId) {
    this.id = UUID.randomUUID();
    this.requested_at = OffsetDateTime.now();
    this.card_owner = userId;
  }

  public Card(String card_code, ReactiveAuth authProvider) {
    this.card_code = card_code;
    Random random = new Random();
    this.pin = String.format("%04d", random.nextInt(10000));
    this.authProvider = authProvider;
    hashPin();
  }

  private void hashPin() {
    String salt = authProvider.generateSalt();
    String hashedPin = authProvider.computeHash(this.pin, salt);
    this.salt = salt;
    this.hashedPin = hashedPin;
  }

  public String getHashedPin() {
    return hashedPin;
  }

  public String getSalt() {
    return getSalt();
  }

  public Tuple toTuple() {
    return Tuple.of(id, requested_at, card_owner);
  }

  public String getCard_code() {
    return card_code;
  }

  public void setCard_code(String card_code) {
    this.card_code = card_code;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public OffsetDateTime getRequested_at() {
    return requested_at;
  }

  public void setRequested_at(OffsetDateTime requested_at) {
    this.requested_at = requested_at;
  }

  public UUID getCard_owner() {
    return card_owner;
  }

  public void setCard_owner(UUID card_owner) {
    this.card_owner = card_owner;
  }

  public String getPin() {
    return pin;
  }

  public void setPin(String pin) {
    this.pin = pin;
  }
}

