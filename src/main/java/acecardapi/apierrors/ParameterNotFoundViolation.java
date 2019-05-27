/*
 * Copyright 2019 Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT license.
 *
 */

package acecardapi.apierrors;

public class ParameterNotFoundViolation extends FieldViolation {

  public ParameterNotFoundViolation(String field) {
    super(false, "parameter_not_found");
    this.field = field;
  }
}
