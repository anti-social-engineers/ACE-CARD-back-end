/*
 * Copyright 2019 Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT license.
 *
 */

package acecardapi.apierrors;

public class PathParameterViolation extends FieldViolation {

  public PathParameterViolation(String field) {
    super(false, "path_parameter_invalid");
    this.field = field;
  }
}
