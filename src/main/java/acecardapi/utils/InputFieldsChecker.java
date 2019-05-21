/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.utils;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;

public final class InputFieldsChecker {

  private InputFieldsChecker() {
  }

  public static boolean AllFieldsArePresent(JsonObject jsonObject, ArrayList<String> fields) {

    for (String key: fields) {

      if (!jsonObject.containsKey(key))
        return false;
    }
    return true;

  }
}

