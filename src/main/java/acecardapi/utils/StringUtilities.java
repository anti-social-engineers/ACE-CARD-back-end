/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.utils;

public class StringUtilities {

  public static boolean contains(String string, String key)
  {
    return string.toLowerCase().contains(key.toLowerCase());
  }

}
