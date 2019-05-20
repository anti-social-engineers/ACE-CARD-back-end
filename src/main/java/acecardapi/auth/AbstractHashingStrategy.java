/*
* Copyright 2019 Aaron Beetstra, modified version
*/

package acecardapi.auth;

import io.reactiverse.pgclient.Row;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.auth.PRNG;

public abstract class AbstractHashingStrategy implements IHashStrategy {

  private final PRNG random;
  protected JsonArray nonces;

  AbstractHashingStrategy(Vertx vertx) {
    random = new PRNG(vertx);
  }

  @Override
  public String generateSalt() {
    byte[] salt = new byte[32];
    random.nextBytes(salt);

    return bytesToHex(salt);
  }

  private final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

  String bytesToHex(byte[] bytes) {
    char[] chars = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      int x = 0xFF & bytes[i];
      chars[i * 2] = HEX_CHARS[x >>> 4];
      chars[1 + i * 2] = HEX_CHARS[0x0F & x];
    }
    return new String(chars);
  }

  @Override
  public String getHashedStoredPwd(Row row) {
    return row.getString("password");
  }

  @Override
  public String getSalt(Row row) {
    return row.getString("password_salt");
  }

  @Override
  public void setNonces(JsonArray nonces) {
    //TODO: FIX TO USE ROW
    this.nonces = nonces;
  }
}
