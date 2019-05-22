/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.handlers;

import io.reactiverse.pgclient.PgPool;
import io.vertx.core.json.JsonObject;

abstract class AbstractCustomHandler {

  PgPool dbClient;
  JsonObject config;

  AbstractCustomHandler(PgPool dbClient, JsonObject config) {
    this.dbClient = dbClient;
    this.config = config;
  }
}
