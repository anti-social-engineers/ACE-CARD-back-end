/*
 * Copyright 2019 Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT license.
 *
 */

package acecardapi.apierrors;

import io.vertx.core.json.JsonObject;

public class InputFormatViolation extends FieldViolation {

  public InputFormatViolation(String field) {
    super(false, "input_format_violation");
    this.field = field;
  }
}
