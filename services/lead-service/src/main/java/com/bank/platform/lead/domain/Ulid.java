package com.bank.platform.lead.domain;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Locale;

/**
 * Crockford Base32 ULID (26 characters). Time-sortable, opaque, non-sequential
 * ({@code ID-01}, {@code D-020}, {@code BR-LEAD-001}/{@code BR-LEAD-002}).
 *
 * <p>Does not encode CIF, branch, product, PAN or any other business attribute.
 * A later MIS display label, if Product ever wants one, is a reporting projection
 * — never a second identity.
 */
public final class Ulid {

    static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int LENGTH = 26;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Ulid() {}

    public static String mint() {
        return mint(Clock.systemUTC());
    }

    public static String mint(Clock clock) {
        long timestamp = clock.instant().toEpochMilli();
        if (timestamp < 0) {
            throw new IllegalArgumentException("ULID timestamp must be non-negative");
        }
        char[] buffer = new char[LENGTH];
        encodeTimestamp(timestamp, buffer);
        byte[] entropy = new byte[10];
        RANDOM.nextBytes(entropy);
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
        if (index < LENGTH && bitCount > 0) {
            buffer[index] = ALPHABET.charAt((int) ((bits << (5 - bitCount)) & 31));
        }
    }
}
