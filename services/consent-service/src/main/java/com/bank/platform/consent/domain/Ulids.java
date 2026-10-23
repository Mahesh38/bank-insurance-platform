package com.bank.platform.consent.domain;

import java.security.SecureRandom;
import java.time.Clock;

/** Crockford Base32 ULID. Opaque; it does not encode CIF or any other customer attribute. */
public final class Ulids {

  private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
  private static final int LENGTH = 26;
  private static final SecureRandom RANDOM = new SecureRandom();

  private Ulids() {}

  public static boolean isValid(String value) {
    if (value == null || value.length() != LENGTH) {
      return false;
    }
    for (int i = 0; i < value.length(); i++) {
      if (ALPHABET.indexOf(value.charAt(i)) < 0) {
        return false;
      }
    }
    return true;
  }

  public static String mint(Clock clock) {
    long timestamp = clock.instant().toEpochMilli();
    char[] buffer = new char[LENGTH];
    long remaining = timestamp;
    for (int i = 9; i >= 0; i--) {
      buffer[i] = ALPHABET.charAt((int) (remaining & 31));
      remaining >>>= 5;
    }
    byte[] entropy = new byte[10];
    RANDOM.nextBytes(entropy);
    long bits = 0;
    int bitCount = 0;
    int index = 10;
    for (byte value : entropy) {
      bits = (bits << 8) | (value & 0xff);
      bitCount += 8;
      while (bitCount >= 5 && index < LENGTH) {
        buffer[index] = ALPHABET.charAt((int) ((bits >>> (bitCount - 5)) & 31));
        index++;
        bitCount -= 5;
      }
    }
    if (index < LENGTH && bitCount > 0) {
      buffer[index] = ALPHABET.charAt((int) ((bits << (5 - bitCount)) & 31));
    }
    return new String(buffer);
  }
}
