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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ReactiveUser extends AbstractUser {

  private ReactiveAuth authProvider;
  private String email;
  private JsonObject principal;
  private UUID id;

  private String rolePrefix;

  public ReactiveUser() {
  }

  ReactiveUser(String email, ReactiveAuth authProvider, String rolePrefix, UUID userID) {
    this.email = email;
    this.authProvider = authProvider;
    this.rolePrefix = rolePrefix;
    this.id = userID;
  }

  @Override
  public void doIsPermitted(String permissionOrRole, Handler<AsyncResult<Boolean>> resultHandler) {
    throw new UnsupportedOperationException("doIsPermitted is not yet implemented.");
//    if (permissionOrRole != null && permissionOrRole.startsWith(rolePrefix)) {
//      hasRoleOrPermission(permissionOrRole.substring(rolePrefix.length()), authProvider.getRolesQuery(), resultHandler);
//    } else {
//      hasRoleOrPermission(permissionOrRole, authProvider.getPermissionsQuery(), resultHandler);
//    }
  }

  @Override
  public JsonObject principal() {
    if (principal == null) {
      principal = new JsonObject().put("email", this.email).put("id", this.id.toString());
    }
    return principal;
  }

  @Override
  public void setAuthProvider(AuthProvider authProvider) {
    if (authProvider instanceof ReactiveAuth) {
      this.authProvider = (ReactiveAuth)authProvider;
    } else {
      throw new IllegalArgumentException("Not a JDBCAuthImpl");
    }
  }

  @Override
  public void writeToBuffer(Buffer buff) {
    super.writeToBuffer(buff);
    byte[] bytes = email.getBytes(StandardCharsets.UTF_8);
    buff.appendInt(bytes.length);
    buff.appendBytes(bytes);

    bytes = rolePrefix.getBytes(StandardCharsets.UTF_8);
    buff.appendInt(bytes.length);
    buff.appendBytes(bytes);
  }

  @Override
  public int readFromBuffer(int pos, Buffer buffer) {
    pos = super.readFromBuffer(pos, buffer);
    int len = buffer.getInt(pos);
    pos += 4;
    byte[] bytes = buffer.getBytes(pos, pos + len);
    email = new String(bytes, StandardCharsets.UTF_8);
    pos += len;

    len = buffer.getInt(pos);
    pos += 4;
    bytes = buffer.getBytes(pos, pos + len);
    rolePrefix = new String(bytes, StandardCharsets.UTF_8);
    pos += len;

    return pos;
  }

//  private void hasRoleOrPermission(String roleOrPermission, String query, Handler<AsyncResult<Boolean>> resultHandler) {
//    authProvider.executeQuery(query, new JsonArray().add(email), resultHandler, rs -> {
//      boolean has = false;
//      for (JsonArray result : rs.getResults()) {
//        String theRoleOrPermission = result.getString(0);
//        if (roleOrPermission.equals(theRoleOrPermission)) {
//          resultHandler.handle(Future.succeededFuture(true));
//          has = true;
//          break;
//        }
//      }
//      if (!has) {
//        resultHandler.handle(Future.succeededFuture(false));
//      }
//    });
//  }
}
