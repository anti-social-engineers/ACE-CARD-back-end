/*
 * Copyright 2019 Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT license.
 *
 */

package acecardapi.apierrors;

import io.vertx.core.json.JsonObject;

public class ApiError {

  private Boolean success;
  private String type;

  public ApiError(Boolean success, String type) {
    this.success = success;
    this.type = type;
  }

  public JsonObject errorJson() {
    JsonObject jsonObj = new JsonObject();
    jsonObj.put("success", this.success);
    jsonObj.put("error_type", this.type);
    return jsonObj;
  }
}
