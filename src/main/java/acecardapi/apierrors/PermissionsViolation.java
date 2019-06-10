/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.apierrors;

import io.vertx.core.json.JsonObject;

public class PermissionsViolation extends ApiError {

  private String reason;

  public PermissionsViolation(String reason) {
    super(false, "permission_denied");
    this.reason = reason;
  }

  public JsonObject errorJson() {
    JsonObject jsonObj = super.errorJson();
    jsonObj.put("detail", reason);

    return jsonObj;
  }

}
