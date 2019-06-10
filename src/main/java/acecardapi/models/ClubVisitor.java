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
import java.util.ArrayList;
import java.util.UUID;

public class ClubVisitor {

  private String first_name;
  private String last_name;
  private LocalDate dob;
  private String image;
  private ArrayList<String> flags;

  public ClubVisitor(String first_name, String last_name, LocalDate dob, UUID imageId, String imagePath, ArrayList<String> penalties) {
    this.first_name = first_name;
    this.last_name = last_name;
    this.dob = dob;

    if(imageId != null) {
      this.image = imagePath + imageId.toString();
    } else {
      this.image = "";
    }
    this.flags = penalties;
  }

  public JsonObject toJson() {
    /*
     Function which retruns the object to a JsonObject
     which can be used as API response.
     @return JsonObect
     */

    JsonArray penaltieJsonArray = new JsonArray();

    for (String penalty: flags) {
      penaltieJsonArray.add(Encode.forHtml(penalty));
    }

    return new JsonObject()
      .put("name", Encode.forHtml(first_name))
      .put("surname", Encode.forHtml(last_name))
      .put("dob", Encode.forHtml(dob.toString()))
      .put("image_path", image)
      .put("flags", penaltieJsonArray);

  }
}

