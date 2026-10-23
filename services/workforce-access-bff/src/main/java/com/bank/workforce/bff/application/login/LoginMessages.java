package com.bank.workforce.bff.application.login;

/** Login BRD v3.1 §17 design sentences. A withdrawn row is not returned. */
public final class LoginMessages {

  private LoginMessages() {}

  public record Text(String code, String title, String message, String field) {}

  public static Text wrongCaptcha() {
    return new Text("VAL-004", null, "Wrong captcha. Try again.", "captcha");
  }

  public static Text unknownEmployee() {
    return new Text("VAL-005", null, "Invalid Employee ID. Please check and try again.", "loginId");
  }

  public static Text unknownPartner() {
    return new Text(
        "VAL-006", null, "We couldn't find a partner account with this email ID.", "loginId");
  }

  public static Text inactiveLogin() {
    return new Text(
        "VAL-007",
        "Your account is not active",
        "Your account is not active. Please contact support to get access to Insurance Hub.",
        null);
  }

  public static Text passwordAttempts(int remaining) {
    if (remaining >= 2) {
      return new Text(
          "VAL-008", null, "Incorrect password. You have 2 attempts remaining.", "password");
    }
    return new Text(
        "VAL-009", null, "Incorrect password. You have 1 attempt remaining.", "password");
  }

  public static Text lockedPassword() {
    return new Text(
        "VAL-010",
        "Your account has been locked",
        "Your account has been locked. You entered an incorrect password 3 times. Please use Unlock User to verify with an OTP and continue.",
        null);
  }

  public static Text lockedInactivity() {
    return new Text(
        "VAL-011",
        "Your account is locked due to inactivity",
        "Your account is locked due to inactivity. You haven't signed in for 30 days. Please use Unlock User to verify with an OTP and continue.",
        null);
  }

  public static Text firstTime() {
    return new Text(
        "VAL-012",
        "Activate your account",
        "Activate your account. This is your first login to the Insurance Platform. Please use Unlock User to activate your account and continue.",
        null);
  }

  public static Text incorrectOtp() {
    return new Text("VAL-014", null, "Incorrect OTP. Please check and try again.", "otp");
  }

  public static Text expiredOtp() {
    return new Text("VAL-015", null, "The OTP has expired. Please request a new OTP.", "otp");
  }

  public static Text otpAttemptLimit() {
    return new Text(
        "VAL-016",
        "Too many incorrect attempts",
        "Too many incorrect attempts. Maximum attempts exceeded. Please restart the journey. Your account has not been locked.",
        null);
  }

  public static Text resendLimit() {
    return new Text(
        "VAL-017",
        "Resend limit reached",
        "Resend limit reached. Maximum resend limit reached. Please restart the journey to get a new OTP.",
        null);
  }

  public static Text passwordMismatch() {
    return new Text("VAL-019", null, "Both passwords must match", "confirmPassword");
  }

  public static Text previousPassword() {
    return new Text(
        "VAL-020", null, "New password must differ from the previous password.", "newPassword");
  }

  public static Text forgotUnknown() {
    return new Text(
        "VAL-023",
        null,
        "We could not find an active Insurance RM account with this email ID.",
        "email");
  }

  public static Text unavailable() {
    return new Text(
        "VAL-024", null, "We are unable to process your request. Please try again later.", null);
  }

  public static Text forgotLocked() {
    return new Text(
        "FORGOT_LOCKED",
        "Unlock your account first",
        "Your account is locked. Unlock it with an OTP first. Resetting your password won't unlock your account.",
        null);
  }

  public static Text forgotInactive() {
    return new Text(
        "FORGOT_INACTIVE",
        "Your account is not active",
        "We can't reset the password for this account. Please contact support.",
        null);
  }

  public static Text checklist(String violation) {
    String message =
        switch (violation == null ? "" : violation) {
          case "LENGTH" -> "8 to 16 characters";
          case "UPPER" -> "At least one uppercase letter";
          case "LOWER" -> "At least one lowercase letter";
          case "NUMBER" -> "At least one number";
          case "SPECIAL" -> "At least one special character";
          case "SPACE" -> "No spaces";
          case "EMAIL" -> "Doesn't contain your email ID";
          case "DEFAULT" -> "Not a default password (e.g. 123456)";
          default -> null;
        };
    if (message == null) {
      return unavailable();
    }
    return new Text(violation, null, message, "newPassword");
  }

  public static Text forPreview(boolean bankEmployee, String route) {
    return switch (route) {
      case "UNKNOWN" -> bankEmployee ? unknownEmployee() : unknownPartner();
      case "INACTIVE" -> inactiveLogin();
      case "FIRST_TIME" -> firstTime();
      case "LOCKED_PASSWORD" -> lockedPassword();
      case "LOCKED_INACTIVITY" -> lockedInactivity();
      default -> unavailable();
    };
  }

  public static Text forPasswordFailure(String route, int attemptsRemaining) {
    if ("LOCKED_PASSWORD".equals(route) || attemptsRemaining <= 0) {
      return lockedPassword();
    }
    if ("LOCKED_INACTIVITY".equals(route)) {
      return lockedInactivity();
    }
    if ("INACTIVE".equals(route)) {
      return inactiveLogin();
    }
    return passwordAttempts(attemptsRemaining);
  }
}
