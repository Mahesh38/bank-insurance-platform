package com.bank.platform.consent.domain;

import java.util.regex.Pattern;

/**
 * Last line of defence before a log line. Consent evidence fields must not be logged at all; this
 * scrubber catches a value that still reached a message (CNS-R20, INV-LOG-01).
 */
public final class SensitiveText {

  private static final Pattern MOBILE = Pattern.compile("\\b[6-9]\\d{9}\\b");
  private static final Pattern PAN = Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]\\b");
  private static final Pattern AADHAAR = Pattern.compile("\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\b");
  private static final Pattern EMAIL =
      Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
  private static final Pattern IPV4 = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

  private SensitiveText() {}

  public static String scrub(String raw) {
    if (raw == null || raw.isEmpty()) {
      return raw;
    }
    String value = EMAIL.matcher(raw).replaceAll("[redacted]");
    value = AADHAAR.matcher(value).replaceAll("[redacted]");
    value = PAN.matcher(value).replaceAll("[redacted]");
    value = MOBILE.matcher(value).replaceAll("[redacted]");
    value = IPV4.matcher(value).replaceAll("[redacted]");
    return value;
  }
}
