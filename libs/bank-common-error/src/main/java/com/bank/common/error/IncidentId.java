package com.bank.common.error;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Generates the identifier that names one failure across every hop and every log line.
 *
 * <p>{@code correlationId} groups a request; an incident id identifies <em>this failure</em>. A
 * request that fails twice has one correlation id and two incident ids.
 *
 * <p>It is generated once, at the point of first failure, and then only ever copied — see
 * {@code docs/journey-execution/07-PLATFORM-ERROR-CONTRACT.md §4.1}. It is the token an RM reads
 * off the screen and L1 support pastes into the log platform.
 *
 * <p>Format: 26 characters, Crockford base-32, time-ordered — a 48-bit millisecond timestamp
 * followed by 80 bits of randomness. Time-ordered because support searches logs by "around when it
 * happened", and a sortable id makes that a range scan rather than a full scan. It carries no
 * customer data by construction.
 */
public final class IncidentId {

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int LENGTH = 26;

    private IncidentId() {}

    /** A new incident id for a failure happening now. */
    public static String generate() {
        return generate(Instant.now());
    }

    /** A new incident id stamped at {@code at}. Package-visible timestamp for deterministic tests. */
    public static String generate(Instant at) {
        byte[] entropy = new byte[10];
        RANDOM.nextBytes(entropy);

        char[] out = new char[LENGTH];
        long timestamp = at.toEpochMilli();

        // 48 bits of timestamp -> the first 10 characters, most significant first.
        for (int i = 9; i >= 0; i--) {
            out[i] = ALPHABET[(int) (timestamp & 0x1F)];
            timestamp >>>= 5;
        }
        // 80 bits of entropy -> the remaining 16 characters.
        int bitBuffer = 0;
        int bitCount = 0;
        int index = 10;
        for (byte b : entropy) {
            bitBuffer = (bitBuffer << 8) | (b & 0xFF);
            bitCount += 8;
            while (bitCount >= 5 && index < LENGTH) {
                bitCount -= 5;
                out[index++] = ALPHABET[(bitBuffer >>> bitCount) & 0x1F];
            }
        }
        while (index < LENGTH) {
            out[index++] = ALPHABET[0];
        }
        return new String(out);
    }

    /** True when {@code value} is shaped like an incident id. */
    public static boolean isValid(String value) {
        if (value == null || value.length() != LENGTH) {
            return false;
        }
        for (int i = 0; i < LENGTH; i++) {
            if (indexOf(value.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(char c) {
        for (int i = 0; i < ALPHABET.length; i++) {
            if (ALPHABET[i] == c) {
                return i;
            }
        }
        return -1;
    }
}
