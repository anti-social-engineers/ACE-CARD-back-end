/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.apierrors;

import io.vertx.core.json.JsonObject;

public class FileFormatViolation extends FieldViolation {

  private String nowAllowedFormat;

  public FileFormatViolation(String field, String format) {
    super(false, "file_format_violation");
    this.field = field;
    this.nowAllowedFormat = format;
  }

  @Override
  public JsonObject errorJson() {
    JsonObject jsonObject = super.errorJson();
    jsonObject.put("format", this.nowAllowedFormat);
    return jsonObject;
  }
}
