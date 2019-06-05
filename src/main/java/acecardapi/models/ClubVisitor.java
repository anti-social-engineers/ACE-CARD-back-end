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

    JsonArray penaltieJsonArray = new JsonArray();

    for (String penalty: flags) {
      penaltieJsonArray.add(penalty);
    }

    return new JsonObject()
      .put("name", first_name)
      .put("surname", last_name)
      .put("dob", dob.toString())
      .put("image_path", image)
      .put("flags", penaltieJsonArray);

  }
}

