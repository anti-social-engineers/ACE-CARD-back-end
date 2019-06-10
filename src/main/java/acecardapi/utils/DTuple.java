/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.utils;

public class DTuple<A, B> {
  public final A frst;
  public final B scnd;
  public DTuple(A frst, B scnd) {
    this.frst = frst;
    this.scnd = scnd;
  }
}
