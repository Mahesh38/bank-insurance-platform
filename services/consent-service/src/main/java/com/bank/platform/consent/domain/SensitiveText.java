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
  private static final Pattern IPV4 = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
  private static final int MAX_LOCAL = 64;
  private static final int MAX_DOMAIN = 253;

  private SensitiveText() {}

  public static String scrub(String raw) {
    if (raw == null || raw.isEmpty()) {
      return raw;
    }
    String value = scrubEmails(raw);
    value = AADHAAR.matcher(value).replaceAll("[redacted]");
    value = PAN.matcher(value).replaceAll("[redacted]");
    value = MOBILE.matcher(value).replaceAll("[redacted]");
    value = IPV4.matcher(value).replaceAll("[redacted]");
    return value;
  }

  /** Linear scan. A regex for this shape backtracks on long runs of punctuation. */
  private static String scrubEmails(String raw) {
    StringBuilder out = new StringBuilder(raw.length());
    int index = 0;
    while (index < raw.length()) {
      int at = raw.indexOf('@', index);
      if (at < 0) {
        out.append(raw, index, raw.length());
        break;
      }
      int localStart = at;
      while (localStart > index
          && at - localStart < MAX_LOCAL
          && isLocal(raw.charAt(localStart - 1))) {
        localStart--;
      }
      int domainEnd = at + 1;
      int lastDot = -1;
      while (domainEnd < raw.length()
          && domainEnd - at <= MAX_DOMAIN
          && isDomain(raw.charAt(domainEnd))) {
        if (raw.charAt(domainEnd) == '.') {
          lastDot = domainEnd;
        }
        domainEnd++;
      }
      if (localStart < at && tldOk(raw, lastDot, domainEnd)) {
        out.append(raw, index, localStart).append("[redacted]");
        index = domainEnd;
      } else {
        out.append(raw, index, at + 1);
        index = at + 1;
      }
    }
    return out.toString();
  }

  private static boolean tldOk(String raw, int lastDot, int domainEnd) {
    int length = domainEnd - lastDot - 1;
    if (lastDot < 0 || length < 2 || length > 24) {
      return false;
    }
    for (int i = lastDot + 1; i < domainEnd; i++) {
      char c = raw.charAt(i);
      boolean letter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
      if (!letter) {
        return false;
      }
    }
    return true;
  }

  private static boolean isLocal(char c) {
    return isDomain(c) || c == '_' || c == '%' || c == '+';
  }

  private static boolean isDomain(char c) {
    return (c >= 'a' && c <= 'z')
        || (c >= 'A' && c <= 'Z')
        || (c >= '0' && c <= '9')
        || c == '.'
        || c == '-';
  }
}
