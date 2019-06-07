/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.models;


import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.owasp.encoder.Encode;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

public class CardRequest {

  private UUID cardId;
  private OffsetDateTime cardRequestedAt;
  private String email;
  private String first_name;
  private String last_name;
  private LocalDate dob;
  private String gender;
  private String image;

  public CardRequest(UUID cardId, OffsetDateTime cardRequestedAt, String email, String first_name, String last_name, LocalDate dob, String gender, UUID imageId, String imagePath) {
    this.cardId = cardId;
    this.cardRequestedAt = cardRequestedAt;
    this.email = email;
    this.first_name = first_name;
    this.last_name = last_name;
    this.dob = dob;
    this.gender = gender;

    if (imageId != null) {
      this.image = imagePath + imageId.toString();
    } else {
      this.image = "";
    }
  }

  public JsonObject toJson() {
    /*
     Function which retruns the object to a JsonObject
     which can be used as API response.
     @return JsonObect
     */

    return new JsonObject()
      .put("card", cardId.toString())
      .put("requested_at", cardRequestedAt.toString())
      .put("email", Encode.forHtml(email))
      .put("first_name", Encode.forHtml(first_name))
      .put("surname", Encode.forHtml(last_name))
      .put("date_of_birth", Encode.forHtml(dob.toString()))
      .put("gender", Encode.forHtml(gender))
      .put("image_path", image);
  }
}

