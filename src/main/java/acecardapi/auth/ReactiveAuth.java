/*
 * Copyright 2014 Red Hat, Inc.
 * Copyright 2019 Aaron Beetstra, modified version
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package acecardapi.auth;

import io.reactiverse.pgclient.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

import java.util.function.Consumer;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class ReactiveAuth implements AuthProvider, IReactiveAuth {

  private PgPool client;
  private String authenticateQuery = DEFAULT_AUTHENTICATE_QUERY;
  private String rolesQuery = DEFAULT_ROLES_QUERY;
  private String permissionsQuery = DEFAULT_PERMISSIONS_QUERY;
  private String rolePrefix = DEFAULT_ROLE_PREFIX;
  private IHashStrategy strategy;

  public ReactiveAuth(Vertx vertx, PgPool client) {
    this.client = client;
    // default strategy
    strategy = IHashStrategy.createSHA512(vertx);
  }

  @Override
  public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {

    String username = authInfo.getString("email");
    if (username == null) {
      resultHandler.handle(Future.failedFuture("authInfo must contain email in 'email' field"));
      return;
    }
    String password = authInfo.getString("password");
    if (password == null) {
      resultHandler.handle(Future.failedFuture("authInfo must contain password in 'password' field"));
      return;
    }


    executeAuthQuery(authenticateQuery, authInfo.getString("email"), resultHandler, rs -> {

      switch (rs.rowCount()) {
        case 0: {

          // Unknown user/password
          resultHandler.handle(Future.failedFuture("Invalid email/password"));
          break;

        }
        case 1: {
          Row row = rs.iterator().next();
          String hashedStoredPwd = strategy.getHashedStoredPwd(row);
          String salt = strategy.getSalt(row);
          // extract the version (-1 means no version)
          int version = -1;
          int sep = hashedStoredPwd.lastIndexOf('$');
          if (sep != -1) {
            try {
              version = Integer.parseInt(hashedStoredPwd.substring(sep + 1));
            } catch (NumberFormatException e) {
              // the nonce version is not a number
              resultHandler.handle(Future.failedFuture("Invalid nonce version: " + version));
              return;
            }
          }
          String hashedPassword = strategy.computeHash(password, salt, version);
          if (IHashStrategy.isEqual(hashedStoredPwd, hashedPassword)) {
            resultHandler.handle(Future.succeededFuture(new ReactiveUser(username, this, rolePrefix)));
          } else {
            resultHandler.handle(Future.failedFuture("Invalid email/password"));
          }
          break;
        }
        default: {
          // More than one row returned!
          resultHandler.handle(Future.failedFuture("Failure in authentication"));
          break;
        }
      }
    });
  }

  @Override
  public ReactiveAuth setAuthenticationQuery(String authenticationQuery) {
    this.authenticateQuery = authenticationQuery;
    return this;
  }

  @Override
  public ReactiveAuth setRolesQuery(String rolesQuery) {
    this.rolesQuery = rolesQuery;
    return this;
  }

  @Override
  public ReactiveAuth setPermissionsQuery(String permissionsQuery) {
    this.permissionsQuery = permissionsQuery;
    return this;
  }

  @Override
  public ReactiveAuth setRolePrefix(String rolePrefix) {
    this.rolePrefix = rolePrefix;
    return this;
  }

  @Override
  public ReactiveAuth setHashStrategy(IHashStrategy strategy) {
    this.strategy = strategy;
    return this;
  }

  @Override
  public String computeHash(String password, String salt, int version) {
    return strategy.computeHash(password, salt, version);
  }

  @Override
  public String generateSalt() {
    return strategy.generateSalt();
  }

  @Override
  public ReactiveAuth setNonces(JsonArray nonces) {
    strategy.setNonces(nonces);
    return this;
  }

  String getRolesQuery() {
    return rolesQuery;
  }

  String getPermissionsQuery() {
    return permissionsQuery;
  }

  private <T> void executeAuthQuery(String query, String name, Handler<AsyncResult<T>> resultHandler,
                            Consumer<PgRowSet> resultSetConsumer) {
    client.getConnection(res -> {
      if (res.succeeded()) {
        PgConnection connection = res.result();

        // TODO: SQL Injection
        connection.preparedQuery(query, Tuple.of(name), queryRes -> {
          if (queryRes.succeeded()) {
            PgRowSet rs = queryRes.result();
            resultSetConsumer.accept(rs);
          } else {
            resultHandler.handle(Future.failedFuture(queryRes.cause()));
          }
          connection.close();
        });
      } else {
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });

  }
}
