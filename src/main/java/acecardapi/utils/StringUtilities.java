/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.utils;

import java.text.DecimalFormat;

public class StringUtilities {

  public static boolean contains(String string, String key) {
    return string.toLowerCase().contains(key.toLowerCase());
  }

  /**
   Function which convert a double to a currency value with 2 decimals behind the point
   @param value double to be converted to string currency
   @return string
   */
  public static String doubleToCurrencyString(double value) {
    DecimalFormat decimalFormat = new DecimalFormat("0.00");

    return decimalFormat.format(value);
  }

}
