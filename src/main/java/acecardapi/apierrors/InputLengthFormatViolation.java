/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.apierrors;

public class InputLengthFormatViolation extends FieldViolation {

  public InputLengthFormatViolation(String field) {
    super(false, "input_length_format_violation");
    this.field = field;
  }
}
