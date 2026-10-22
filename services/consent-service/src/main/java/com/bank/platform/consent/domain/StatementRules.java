package com.bank.platform.consent.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/** Statement identity, immutability and MAJOR/MINOR comparison (CNS-R01–R09). */
public final class StatementRules {

  public static final String R0_LANGUAGE = "en-IN";
  private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)");

  private StatementRules() {}

  public static String sha256(String text) {
    if (text == null) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "statement text is required");
    }
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is required", ex);
    }
  }

  public static void requireLanguage(String language, boolean englishSourceExists) {
    if (language == null || language.isBlank()) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "language is required");
    }
    if (R0_LANGUAGE.equals(language)) {
      return;
    }
    if ("hi-IN".equals(language)) {
      if (!englishSourceExists) {
        throw new ConsentRefusal(
            ConsentCodes.STATEMENT_NOT_PUBLISHED,
            "Hindi statement requires the English source version");
      }
      return;
    }
    throw new ConsentRefusal(
        ConsentCodes.INVALID_REQUEST, "language is not published for this release");
  }

  public static int major(String version) {
    var matcher = VERSION.matcher(version == null ? "" : version.trim());
    if (!matcher.matches()) {
      throw new ConsentRefusal(
          ConsentCodes.INVALID_REQUEST, "statement version must be MAJOR.MINOR");
    }
    return Integer.parseInt(matcher.group(1));
  }

  /** True when the published version is a material change over the captured one. */
  public static boolean isMajorUpgrade(String capturedVersion, String currentVersion) {
    return major(currentVersion) > major(capturedVersion);
  }
}
