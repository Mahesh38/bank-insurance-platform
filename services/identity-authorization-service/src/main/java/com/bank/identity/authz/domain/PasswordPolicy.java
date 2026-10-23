package com.bank.identity.authz.domain;

import java.util.Locale;

/**
 * Login BRD v3.1 §14. The design checklist is the user-visible rule. This check is the server copy
 * of that checklist, including “must differ from the previous password”.
 */
public final class PasswordPolicy {

  public enum Violation {
    OK,
    MISMATCH,
    LENGTH,
    UPPER,
    LOWER,
    NUMBER,
    SPECIAL,
    SPACE,
    DEFAULT,
    EMAIL,
    PREVIOUS
  }

  private PasswordPolicy() {}

  public static Violation check(
      String password, String confirm, String email, boolean sameAsPrevious) {
    if (password == null || confirm == null || !password.equals(confirm)) {
      return Violation.MISMATCH;
    }
    if ("123456".equals(password)) {
      return Violation.DEFAULT;
    }
    if (password.length() < 8 || password.length() > 16) {
      return Violation.LENGTH;
    }
    if (password.chars().anyMatch(Character::isWhitespace)) {
      return Violation.SPACE;
    }
    if (email != null
        && !email.isBlank()
        && password.toLowerCase(Locale.ROOT).contains(email.toLowerCase(Locale.ROOT))) {
      return Violation.EMAIL;
    }
    boolean upper = false;
    boolean lower = false;
    boolean digit = false;
    boolean special = false;
    for (int i = 0; i < password.length(); i++) {
      char c = password.charAt(i);
      if (Character.isUpperCase(c)) {
        upper = true;
      } else if (Character.isLowerCase(c)) {
        lower = true;
      } else if (Character.isDigit(c)) {
        digit = true;
      } else {
        special = true;
      }
    }
    if (!upper) {
      return Violation.UPPER;
    }
    if (!lower) {
      return Violation.LOWER;
    }
    if (!digit) {
      return Violation.NUMBER;
    }
    if (!special) {
      return Violation.SPECIAL;
    }
    if (sameAsPrevious) {
      return Violation.PREVIOUS;
    }
    return Violation.OK;
  }
}
