/*
 * Copyright 2019 Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT license.
 *
 */

package acecardapi.models;

import acecardapi.exceptions.InvalidEmailException;
import io.reactiverse.pgclient.Tuple;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.mailencoder.EmailAddress;

import java.util.UUID;

public class User {

  private UUID id;

  private String email;

  private String password;

  private String password_salt;

  public User(String email, String password, String password_salt) {


    this.email = email;
    this.password = password;
    this.password_salt = password_salt;
  }

  public User() {
    //TODO: Check if exists?
    this.id = UUID.randomUUID();
  }

  public String getName() {
    return email;
  }

  public String getPassword() {
    return password;
  }

  public String getPassword_salt() {
    return password_salt;
  }

  public void setEmail(String email) throws InvalidEmailException {

   try {

     EmailAddress address = new EmailAddress(email);

   } catch (IllegalArgumentException e) {

     throw new InvalidEmailException();

   }

    this.email = email;

  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setPassword_salt(String password_salt) {
    this.password_salt = password_salt;
  }

  public JsonArray toJsonArray() {
    return new JsonArray().add(id).add(email).add(password).add(password_salt);
  }

  public Tuple toTuple() {
    return Tuple.of(id, email, password, password_salt);
  }

  public JsonObject toJsonObjectLogin() {
    return new JsonObject().put("email", email).put("password", password);
  }
}
