/// In-memory login directory for the RM app demonstration.
///
/// This is **not** bank AD and **not** the production Captcha/OTP gateway.
/// Bank RM passwords are checked here only so the Login BRD screens can be
/// exercised; the BRD still forbids storing or resetting the bank password in
/// the platform (`BR-LOGIN-002`).
library;

import '../domain/login.dart';

enum LoginAccountState { active, locked, disabled, passwordNeverCreated }

class LoginAccount {
  LoginAccount({
    required this.type,
    required this.identifier,
    required this.maskedMobile,
    required this.maskedEmail,
    this.password,
    this.state = LoginAccountState.active,
    this.failedPasswordAttempts = 0,
    DateTime? lastSuccessfulLoginAt,
  }) : lastSuccessfulLoginAt = lastSuccessfulLoginAt;

  final LoginUserType type;
  final String identifier;
  final String maskedMobile;
  final String maskedEmail;
  String? password;
  LoginAccountState state;
  int failedPasswordAttempts;
  DateTime? lastSuccessfulLoginAt;
}

class OtpSession {
  OtpSession({
    required this.account,
    required this.code,
    required this.expiresAt,
    this.forUnlock = false,
    this.attempts = 0,
    this.resends = 0,
  });

  final LoginAccount account;
  final bool forUnlock;
  String code;
  DateTime expiresAt;
  int attempts;
  int resends;
}

class LoginOutcome {
  const LoginOutcome._(this.ok, this.message, {this.session, this.account});

  final bool ok;
  final String? message;
  final OtpSession? session;
  final LoginAccount? account;

  factory LoginOutcome.error(String message) =>
      LoginOutcome._(false, message);
  factory LoginOutcome.otp(OtpSession session) =>
      LoginOutcome._(true, null, session: session);
  factory LoginOutcome.unlocked(LoginAccount account, String message) =>
      LoginOutcome._(true, message, account: account);
  factory LoginOutcome.needPassword(LoginAccount account) =>
      LoginOutcome._(true, null, account: account);
}

/// Demonstration Captcha and OTP. Production values come from IS-approved
/// services (`SEC-010`). Not secrets.
const kDemoCaptcha = 'A7K2';
const kDemoOtp = '123456';

class FakeLoginDirectory {
  FakeLoginDirectory({DateTime Function()? clock}) : _clock = clock ?? DateTime.now {
    _accounts.addAll([
      LoginAccount(
        type: LoginUserType.bankRm,
        identifier: 'RM1001',
        maskedMobile: maskMobile('9876543210'),
        maskedEmail: maskEmail('rm.demo@aubank.example'),
        password: 'BankPass1!',
        lastSuccessfulLoginAt: _clock(),
      ),
      LoginAccount(
        type: LoginUserType.bankRm,
        identifier: 'RM-INACTIVE',
        maskedMobile: maskMobile('9876500001'),
        maskedEmail: maskEmail('rm.inactive@aubank.example'),
        password: 'BankPass1!',
        state: LoginAccountState.locked,
        lastSuccessfulLoginAt:
            _clock().subtract(const Duration(days: 31)),
      ),
      LoginAccount(
        type: LoginUserType.insurancePartnerRm,
        identifier: 'partner.rm@insurer.example',
        maskedMobile: maskMobile('9876500002'),
        maskedEmail: maskEmail('partner.rm@insurer.example'),
        password: null,
        state: LoginAccountState.passwordNeverCreated,
      ),
      LoginAccount(
        type: LoginUserType.insurancePartnerRm,
        identifier: 'partner.active@insurer.example',
        maskedMobile: maskMobile('9876500003'),
        maskedEmail: maskEmail('partner.active@insurer.example'),
        password: 'Partner1!',
        lastSuccessfulLoginAt: _clock(),
      ),
    ]);
  }

  final DateTime Function() _clock;
  final List<LoginAccount> _accounts = [];

  LoginAccount? find(LoginUserType type, String identifier) {
    final needle = identifier.trim().toLowerCase();
    for (final a in _accounts) {
      if (a.type == type && a.identifier.toLowerCase() == needle) return a;
    }
    return null;
  }

  LoginOutcome submitCredentials({
    required LoginUserType type,
    required String identifier,
    required String password,
    required String captcha,
  }) {
    if (identifier.trim().isEmpty) {
      return LoginOutcome.error(LoginMessages.enterIdentifier);
    }
    if (password.isEmpty) {
      return LoginOutcome.error(LoginMessages.enterPassword);
    }
    if (captcha.trim().isEmpty) {
      return LoginOutcome.error(LoginMessages.enterCaptcha);
    }
    if (type == LoginUserType.insurancePartnerRm &&
        !looksLikeEmail(identifier)) {
      return LoginOutcome.error(LoginMessages.invalidPartnerAccount);
    }
    if (captcha.trim() != kDemoCaptcha) {
      return LoginOutcome.error(LoginMessages.wrongCaptcha);
    }

    final account = find(type, identifier);
    if (account == null) {
      return LoginOutcome.error(type == LoginUserType.bankRm
          ? LoginMessages.invalidEmployeeId
          : LoginMessages.invalidPartnerAccount);
    }
    if (account.state == LoginAccountState.disabled) {
      return LoginOutcome.error(LoginMessages.notActive);
    }
    final last = account.lastSuccessfulLoginAt;
    if (last != null &&
        _clock().difference(last) >=
            const Duration(days: LoginLimits.inactivityLockDays)) {
      account.state = LoginAccountState.locked;
      return LoginOutcome.error(LoginMessages.lockedInactivity);
    }
    if (account.state == LoginAccountState.passwordNeverCreated) {
      return LoginOutcome.error(
          'First-time password must be created through Unlock User.');
    }
    if (account.state == LoginAccountState.locked) {
      final byFailures =
          account.failedPasswordAttempts >= LoginLimits.maxPasswordFailures;
      return LoginOutcome.error(byFailures
          ? LoginMessages.lockedFailedAttempts
          : LoginMessages.lockedInactivity);
    }
    if (account.password != password) {
      account.failedPasswordAttempts++;
      final remaining =
          LoginLimits.maxPasswordFailures - account.failedPasswordAttempts;
      if (remaining <= 0) {
        account.state = LoginAccountState.locked;
        return LoginOutcome.error(LoginMessages.lockedFailedAttempts);
      }
      return LoginOutcome.error(
          LoginMessages.incorrectPasswordRemaining(remaining));
    }

    account.failedPasswordAttempts = 0;
    return LoginOutcome.otp(_newOtp(account));
  }

  LoginOutcome verifyOtp(OtpSession session, String otp) {
    if (otp.trim().length != 6 || int.tryParse(otp.trim()) == null) {
      return LoginOutcome.error(LoginMessages.enterOtp);
    }
    if (_clock().isAfter(session.expiresAt)) {
      return LoginOutcome.error(LoginMessages.otpExpired);
    }
    if (otp.trim() != session.code) {
      session.attempts++;
      final remaining = LoginLimits.otpMaxAttempts - session.attempts;
      if (remaining <= 0) {
        return LoginOutcome.error(LoginMessages.otpMaxAttempts);
      }
      return LoginOutcome.error(LoginMessages.incorrectOtpRemaining(remaining));
    }
    if (session.forUnlock) {
      return LoginOutcome.otp(session);
    }
    session.account.lastSuccessfulLoginAt = _clock();
    session.account.failedPasswordAttempts = 0;
    session.account.state = LoginAccountState.active;
    return LoginOutcome.unlocked(session.account, '');
  }

  LoginOutcome resendOtp(OtpSession session) {
    if (session.resends >= LoginLimits.otpMaxResends) {
      return LoginOutcome.error(LoginMessages.otpMaxResends);
    }
    session.resends++;
    session.code = kDemoOtp;
    session.expiresAt = _clock().add(LoginLimits.otpValidity);
    session.attempts = 0;
    return LoginOutcome.otp(session);
  }

  LoginOutcome startUnlock({
    required LoginUserType type,
    required String identifier,
    required String captcha,
  }) {
    if (identifier.trim().isEmpty) {
      return LoginOutcome.error(LoginMessages.enterIdentifier);
    }
    if (captcha.trim().isEmpty) {
      return LoginOutcome.error(LoginMessages.enterCaptcha);
    }
    if (captcha.trim() != kDemoCaptcha) {
      return LoginOutcome.error(LoginMessages.wrongCaptcha);
    }
    final account = find(type, identifier);
    if (account == null) {
      return LoginOutcome.error(type == LoginUserType.bankRm
          ? LoginMessages.invalidEmployeeId
          : LoginMessages.invalidPartnerAccount);
    }
    if (account.state == LoginAccountState.disabled) {
      return LoginOutcome.error(LoginMessages.notActive);
    }
    return LoginOutcome.otp(_newOtp(account, forUnlock: true));
  }

  /// After a successful Unlock OTP. Bank RM never creates a password here.
  LoginOutcome afterUnlockOtp(
    LoginAccount account, {
    required bool partnerWantsNewPassword,
  }) {
    if (account.type == LoginUserType.bankRm) {
      account.state = LoginAccountState.active;
      account.failedPasswordAttempts = 0;
      return LoginOutcome.unlocked(account, LoginMessages.bankUnlocked);
    }
    if (account.state == LoginAccountState.passwordNeverCreated ||
        partnerWantsNewPassword) {
      return LoginOutcome.needPassword(account);
    }
    if (account.state == LoginAccountState.active) {
      return LoginOutcome.error(LoginMessages.alreadyActive);
    }
    account.state = LoginAccountState.active;
    account.failedPasswordAttempts = 0;
    return LoginOutcome.unlocked(account, LoginMessages.partnerUnlocked);
  }

  String? setPartnerPassword({
    required LoginAccount account,
    required String password,
    required String confirm,
  }) {
    final error = PartnerPasswordPolicy.validate(
      password: password,
      confirm: confirm,
      corporateEmail: account.identifier,
    );
    if (error != null) return error;
    account.password = password;
    account.state = LoginAccountState.active;
    account.failedPasswordAttempts = 0;
    return null;
  }

  OtpSession _newOtp(LoginAccount account, {bool forUnlock = false}) =>
      OtpSession(
        account: account,
        code: kDemoOtp,
        expiresAt: _clock().add(LoginLimits.otpValidity),
        forUnlock: forUnlock,
      );
}
