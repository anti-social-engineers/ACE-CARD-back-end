/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.apierrors;

public class TooManyFailedAttemptsViolation extends ApiError {

  public TooManyFailedAttemptsViolation() {
    super(false, "too_many_failed_attempts");
  }
}
