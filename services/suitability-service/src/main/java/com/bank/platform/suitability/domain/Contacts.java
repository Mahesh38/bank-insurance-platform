package com.bank.platform.suitability.domain;

import java.util.Locale;

/**
 * Registered and Life Assured contact checks. Registered contacts are the only link destination.
 */
public final class Contacts {

  private Contacts() {}

  public static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  public static boolean mobile(String raw) {
    return canonicalMobile(raw) != null;
  }

  public static boolean email(String raw) {
    return canonicalEmail(raw) != null;
  }

  public static String requireMobile(String raw) {
    String digits = canonicalMobile(raw);
    if (digits == null) {
      throw SuitabilityRefusal.validation(
          new JourneyModel.FieldError("VAL-006", "registeredMobile", BrdCopy.VAL_006));
    }
    return digits;
  }

  public static String requireEmail(String raw) {
    String value = canonicalEmail(raw);
    if (value == null) {
      throw SuitabilityRefusal.validation(
          new JourneyModel.FieldError("VAL-007", "registeredEmail", BrdCopy.VAL_007));
    }
    return value;
  }

  public static String maskMobile(String digits) {
    return "+91 " + digits.substring(0, 2) + "*****" + digits.substring(7);
  }

  public static String maskEmail(String value) {
    int at = value.indexOf('@');
    return value.substring(0, 1) + "***" + value.substring(at);
  }

  static String canonicalMobile(String raw) {
    if (blank(raw)) {
      return null;
    }
    String digits = raw.replaceAll("\\D", "");
    if (digits.length() == 12 && digits.startsWith("91")) {
      digits = digits.substring(2);
    }
    if (digits.length() == 11 && digits.startsWith("0")) {
      digits = digits.substring(1);
    }
    if (!digits.matches("^[6-9][0-9]{9}$")) {
      return null;
    }
    return digits;
  }

  static String canonicalEmail(String raw) {
    if (blank(raw)) {
      return null;
    }
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if (value.length() > 120 || !value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
      return null;
    }
    return value;
  }
}
