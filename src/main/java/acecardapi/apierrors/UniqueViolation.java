/*
 * Copyright 2019 Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT license.
 *
 */

package acecardapi.apierrors;

public class UniqueViolation extends FieldViolation {

  public UniqueViolation(String field) {
    super(false, "unique_violation");
    this.field = field;
  }
}
