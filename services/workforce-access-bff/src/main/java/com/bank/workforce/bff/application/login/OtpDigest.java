package com.bank.workforce.bff.application.login;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** One-way storage for captcha text and OTP codes. Plaintext is never written down. */
final class OtpDigest {

  private static final SecureRandom RANDOM = new SecureRandom();

  private OtpDigest() {}

  static String newSalt() {
    byte[] salt = new byte[16];
    RANDOM.nextBytes(salt);
    return Base64.getEncoder().encodeToString(salt);
  }

  static String hash(String salt, String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest((salt + ":" + value).getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(digest);
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 is unavailable", impossible);
    }
  }

  static boolean matches(String value, String salt, String expectedHash) {
    return MessageDigest.isEqual(
        hash(salt, value).getBytes(StandardCharsets.UTF_8),
        expectedHash.getBytes(StandardCharsets.UTF_8));
  }

  static String sha256(String value) {
    return hash("captcha", value);
  }
}
