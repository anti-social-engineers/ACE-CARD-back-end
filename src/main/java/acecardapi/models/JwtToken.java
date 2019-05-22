/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.models;

public class JwtToken {

  private String jsonWebToken;

  public JwtToken(String jwtToken) {
    this.jsonWebToken = jwtToken;
  }

  public String getJsonWebToken() {
    return jsonWebToken;
  }
}
