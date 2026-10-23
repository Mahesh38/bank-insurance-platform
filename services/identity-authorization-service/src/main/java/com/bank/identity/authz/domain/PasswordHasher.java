package com.bank.identity.authz.domain;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** One-way platform password storage. The plaintext never leaves the caller. */
public final class PasswordHasher {

  private static final int ITERATIONS = 120_000;
  private static final int SALT_BYTES = 16;
  private static final int KEY_BITS = 256;
  private final SecureRandom random = new SecureRandom();

  public String hash(String password) {
    byte[] salt = new byte[SALT_BYTES];
    random.nextBytes(salt);
    byte[] derived = derive(password, salt, ITERATIONS);
    return "pbkdf2$"
        + ITERATIONS
        + "$"
        + Base64.getEncoder().encodeToString(salt)
        + "$"
        + Base64.getEncoder().encodeToString(derived);
  }

  public boolean matches(String password, String stored) {
    if (password == null || stored == null || stored.isBlank()) {
      return false;
    }
    String[] parts = stored.split("\\$");
    if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
      return false;
    }
    int iterations;
    try {
      iterations = Integer.parseInt(parts[1]);
    } catch (NumberFormatException malformed) {
      return false;
    }
    byte[] salt = Base64.getDecoder().decode(parts[2]);
    byte[] expected = Base64.getDecoder().decode(parts[3]);
    byte[] actual = derive(password, salt, iterations);
    if (actual.length != expected.length) {
      return false;
    }
    int diff = 0;
    for (int i = 0; i < actual.length; i++) {
      diff |= actual[i] ^ expected[i];
    }
    return diff == 0;
  }

  private static byte[] derive(String password, byte[] salt, int iterations) {
    try {
      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException impossible) {
      throw new IllegalStateException("PBKDF2 is unavailable", impossible);
    }
  }
}
