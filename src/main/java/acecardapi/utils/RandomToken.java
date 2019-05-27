/*
 * Original https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string/41156#41156
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers modified version
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.utils;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;

public class RandomToken {

  private static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private static final String lower = "abcdefghijklmnopqrstuvwxyz";

  private static final String digits = "0123456789";

  private static final String alphanum = upper + lower + digits;

  private final Random random;

  private final char[] chars;

  private final char[] buf;

  public RandomToken(int length, Random random, String symbols) {
    if (length < 1) throw new IllegalArgumentException();
    if (symbols.length() < 2) throw new IllegalArgumentException();
    this.random = Objects.requireNonNull(random);
    this.chars = symbols.toCharArray();
    this.buf = new char[length];
  }

  public RandomToken(int length, Random random) {
    this(length, random, alphanum);
  }

  public RandomToken(int length) {
    this(length, new SecureRandom());
  }

  public String nextString() {
    for (int idx = 0; idx < buf.length; ++idx)
      buf[idx] = chars[random.nextInt(chars.length)];
    return new String(buf);
  }

}
