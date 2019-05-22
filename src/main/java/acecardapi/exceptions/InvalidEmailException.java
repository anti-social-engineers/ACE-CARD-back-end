/*
 * Copyright 2019 Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT license.
 *
 */

package acecardapi.exceptions;

public class InvalidEmailException extends Exception {

  public InvalidEmailException() {
    super("email_format_invalid");
  }
}
