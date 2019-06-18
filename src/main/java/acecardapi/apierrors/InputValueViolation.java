/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.apierrors;

public class InputValueViolation extends FieldViolation {

  public InputValueViolation(String field) {
    super(false, "input_value_violation");
    this.field = field;
  }
}
