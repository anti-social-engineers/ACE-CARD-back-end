/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;

public class RequestUtilities {

  public static void attributesCheckMultiMap(MultiMap attributes, String[] requiredAttributes, Handler<AsyncResult<String>> resultHandler) {

    /*
    Function to check whether all required attributes are present on MutliMap.
     */

    boolean failed = false;

    for (int i = 0; i < requiredAttributes.length; i++) {

      if (!attributes.contains(requiredAttributes[i])) {
        failed = true;
        resultHandler.handle(Future.failedFuture(requiredAttributes[i]));
        break;
      }
    }
    if (!failed)
      resultHandler.handle(Future.succeededFuture(""));
  }

  public static void attributesCheckJsonObject(JsonObject attributes, String[] requiredAttributes, Handler<AsyncResult<String>> resultHandler) {

    /*
    Function to check whether all required attributes are present on JsonObject.
     */

    boolean failed = false;

    for (int i = 0; i < requiredAttributes.length; i++) {

      if (!attributes.containsKey(requiredAttributes[i])) {
        failed = true;
        resultHandler.handle(Future.failedFuture(requiredAttributes[i]));
        break;
      }
    }
    if (!failed)
      resultHandler.handle(Future.succeededFuture(""));
  }

}
