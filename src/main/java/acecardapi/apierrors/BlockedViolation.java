/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.apierrors;

import io.vertx.core.json.JsonObject;

public class BlockedViolation extends ApiError {

  private String reason;

  public BlockedViolation(String reason) {
    super(false, "blocked");
    this.reason = reason;
  }

  public JsonObject errorJson() {
    JsonObject jsonObj = super.errorJson();
    jsonObj.put("detail", reason);

    return jsonObj;
  }

}
