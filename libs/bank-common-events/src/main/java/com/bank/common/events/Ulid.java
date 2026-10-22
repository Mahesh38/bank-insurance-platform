package com.bank.common.events;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Locale;

/**
 * Crockford Base32 ULID (26 characters) for {@code eventId} and {@code outbox_id} (ADR-025 §6).
 * Copied from lead-service's {@code Ulid}, plus monotonic minting: two ids minted in the same
 * millisecond by one JVM sort in mint order, so {@code ORDER BY created_at, outbox_id} keeps the
 * write order of events committed in one transaction.
 */
public final class Ulid {

  static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
  private static final int LENGTH = 26;
  private static final int ENTROPY_BYTES = 10;
  private static final SecureRandom RANDOM = new SecureRandom();

  private static long lastTimestamp = -1;
  private static final byte[] LAST_ENTROPY = new byte[ENTROPY_BYTES];

  private Ulid() {}

  public static String mint() {
    return mint(Clock.systemUTC());
  }

  public static String mint(Clock clock) {
    long timestamp = clock.instant().toEpochMilli();
    if (timestamp < 0) {
      throw new IllegalArgumentException("ULID timestamp must be non-negative");
    }
    byte[] entropy = new byte[ENTROPY_BYTES];
    synchronized (Ulid.class) {
      if (timestamp <= lastTimestamp && increment(LAST_ENTROPY)) {
        timestamp = lastTimestamp;
      } else {
        RANDOM.nextBytes(LAST_ENTROPY);
        lastTimestamp = timestamp;
      }
      System.arraycopy(LAST_ENTROPY, 0, entropy, 0, ENTROPY_BYTES);
    }
    char[] buffer = new char[LENGTH];
    encodeTimestamp(timestamp, buffer);
    encodeEntropy(entropy, buffer);
    return new String(buffer);
  }

  public static boolean isValid(String value) {
    if (value == null || value.length() != LENGTH) {
      return false;
    }
    String normalised = value.toUpperCase(Locale.ROOT);
    for (int i = 0; i < LENGTH; i++) {
      if (ALPHABET.indexOf(normalised.charAt(i)) < 0) {
        return false;
      }
    }
    return true;
  }

  /** Adds one to the 80-bit entropy; false on overflow (caller then draws fresh entropy). */
  private static boolean increment(byte[] entropy) {
    for (int i = entropy.length - 1; i >= 0; i--) {
      entropy[i]++;
      if (entropy[i] != 0) {
        return true;
      }
    }
    return false;
  }

  private static void encodeTimestamp(long timestamp, char[] buffer) {
    long remaining = timestamp;
    for (int i = 9; i >= 0; i--) {
      buffer[i] = ALPHABET.charAt((int) (remaining & 31));
      remaining >>>= 5;
    }
  }

  private static void encodeEntropy(byte[] entropy, char[] buffer) {
    long bits = 0;
    int bitCount = 0;
    int index = 10;
    for (byte b : entropy) {
      bits = (bits << 8) | (b & 0xff);
      bitCount += 8;
      while (bitCount >= 5 && index < LENGTH) {
        buffer[index++] = ALPHABET.charAt((int) ((bits >>> (bitCount - 5)) & 31));
        bitCount -= 5;
      }
    }
  }
}
