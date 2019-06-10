/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi;

import io.vertx.junit5.Timeout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static acecardapi.utils.AceCardDecrypter.decrypt;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDecrypter {

  public TestDecrypter() {
  }

  @Test
  @DisplayName("Test whether the value is decrypted correctly.")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_decryption() {

    String encryptedValue = "TVobdap635jvPSW9KShf1ZuBDLDuh8a7syXEjl58+Vs=0000";
    String encryptionKey = "C*F-JaNdRgUjXn2r5u8x/A?D(G+KbPeS";
    String expectedValue = "1234567812345678";

    String decryptedValue = decrypt(encryptedValue, encryptionKey);

    assertEquals(expectedValue, decryptedValue);

  }
}
