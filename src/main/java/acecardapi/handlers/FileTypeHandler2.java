/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */


package acecardapi.handlers;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public class FileTypeHandler2 implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {
    System.out.println("Here....");

    try {

      MultiMap attributes = context.request().formAttributes();
      System.out.println(attributes);

      System.out.println("Done...");

      context.next();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
