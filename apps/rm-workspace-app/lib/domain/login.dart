/// Login module rules from the September 2026 Login BRD (`BR-LOGIN-*`, `VAL-*`).
///
/// Pure functions and messages. No Flutter, no I/O. The fake directory and the
/// login screens apply these; they do not invent a second copy of the text.
library;

enum LoginUserType { bankRm, insurancePartnerRm }

extension LoginUserTypeLabel on LoginUserType {
  String get tabLabel => switch (this) {
        LoginUserType.bankRm => 'Bank RM',
        LoginUserType.insurancePartnerRm => 'Insurance Partner',
      };

  String get identifierLabel => switch (this) {
        LoginUserType.bankRm => 'Employee ID',
        LoginUserType.insurancePartnerRm => 'Corporate Email ID',
      };
}

/// User-facing catalogue — Login BRD §8. Do not paraphrase in the UI.
abstract final class LoginMessages {
  static const enterIdentifier = 'Please enter Employee ID / Corporate Email ID.';
  static const enterPassword = 'Please enter your password.';
  static const enterCaptcha = 'Please enter the security Captcha.';
  static const invalidEmployeeId =
      'Invalid Employee ID. Please check and try again.';
  static const invalidPartnerAccount =
      'Invalid account. Please check and try again.';
  static const wrongCaptcha = 'Wrong Captcha. Please check and try again.';
  static const lockedFailedAttempts =
      'Your account has been locked due to multiple incorrect password attempts. Please use Unlock User.';
  static const lockedInactivity =
      'Your account is locked. Please use Unlock User to continue.';
  static const enterOtp = 'Please enter the six-digit OTP.';
  static const otpExpired = 'The OTP has expired. Please request a new OTP.';
  static const otpMaxAttempts =
      'You have exceeded the maximum OTP verification attempts. Please log in again.';
  static const otpMaxResends =
      'You have reached the maximum OTP resend limit. Please return to login and try again.';
  static const alreadyActive =
      'Your account is already active. Please proceed to login.';
  static const notActive =
      'Your account is not active. Please contact support.';
  static const passwordPolicy =
      'Password must be 8 to 20 characters and contain uppercase, lowercase, number and special character.';
  static const passwordMismatch =
      'New Password and Confirm Password do not match.';
  static const passwordContainsUser =
      'Password must not contain your name or corporate email ID.';
  static const bankUnlocked =
      'Your account has been unlocked successfully. Please log in using your existing bank credentials.';
  static const partnerUnlocked =
      'Your account has been unlocked successfully. Please log in using your existing password.';
  static const itWillProvideGuide =
      'Unlock User guide content will be provided by IT.';
  static const itWillProvideHelp =
      'Support contact will be provided by IT.';

  static String incorrectPasswordRemaining(int remaining) {
    if (remaining == 1) {
      return 'The password entered is incorrect. You have 1 attempt remaining.';
    }
    return 'The password entered is incorrect. You have $remaining attempts remaining.';
  }

  static String incorrectOtpRemaining(int remaining) {
    if (remaining == 1) {
      return 'The OTP entered is incorrect. You have 1 attempt remaining.';
    }
    return 'The OTP entered is incorrect. You have $remaining attempts remaining.';
  }
}

abstract final class LoginLimits {
  static const maxPasswordFailures = 3;
  static const inactivityLockDays = 30;
  static const otpValidity = Duration(minutes: 10);
  static const otpMaxAttempts = 5;
  static const otpResendDelay = Duration(minutes: 2);
  static const otpMaxResends = 3;
  static const partnerPasswordExpiry = Duration(days: 60);
  static const passwordMin = 8;
  static const passwordMax = 20;
}

/// Partner platform password — Login BRD Screen 5 / VAL-019…021.
abstract final class PartnerPasswordPolicy {
  static final _special = RegExp(r'[^A-Za-z0-9]');

  static String? validate({
    required String password,
    required String confirm,
    required String corporateEmail,
  }) {
    if (password != confirm) return LoginMessages.passwordMismatch;
    if (password.length < LoginLimits.passwordMin ||
        password.length > LoginLimits.passwordMax ||
        !password.contains(RegExp(r'[A-Z]')) ||
        !password.contains(RegExp(r'[a-z]')) ||
        !password.contains(RegExp(r'[0-9]')) ||
        !_special.hasMatch(password)) {
      return LoginMessages.passwordPolicy;
    }
    final local = corporateEmail.split('@').first.toLowerCase();
    final lower = password.toLowerCase();
    if (local.isNotEmpty && lower.contains(local.toLowerCase())) {
      return LoginMessages.passwordContainsUser;
    }
    if (lower.contains(corporateEmail.toLowerCase())) {
      return LoginMessages.passwordContainsUser;
    }
    return null;
  }
}

bool looksLikeEmail(String value) =>
    RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(value.trim());

String maskEmail(String email) {
  final parts = email.split('@');
  if (parts.length != 2 || parts[0].isEmpty) return '****';
  final local = parts[0];
  final shown = local.substring(0, 1);
  return '$shown****@${parts[1]}';
}

String maskMobile(String mobile) {
  if (mobile.length < 4) return '****';
  return '******${mobile.substring(mobile.length - 4)}';
}
