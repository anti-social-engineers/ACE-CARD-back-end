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
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

import java.util.Set;

public class FileTypeHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {

    context.request().setExpectMultipart(true);

    var attributes = context.request().getFormAttribute("file1");
    System.out.println(attributes);

    context.next();

  }
}
