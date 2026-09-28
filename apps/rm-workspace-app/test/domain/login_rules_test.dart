/// Login BRD v1.0 rules — remaining attempts, lock, OTP, partner password.
library;

import 'package:flutter_test/flutter_test.dart';
import 'package:rm_workspace_app/data/login_fake.dart';
import 'package:rm_workspace_app/domain/login.dart';

void main() {
  group('PartnerPasswordPolicy', () {
    test('VAL-020 mismatch', () {
      expect(
        PartnerPasswordPolicy.validate(
          password: 'Partner1!',
          confirm: 'Partner2!',
          corporateEmail: 'partner.rm@insurer.example',
        ),
        LoginMessages.passwordMismatch,
      );
    });

    test('VAL-019 policy', () {
      expect(
        PartnerPasswordPolicy.validate(
          password: 'short1!',
          confirm: 'short1!',
          corporateEmail: 'partner.rm@insurer.example',
        ),
        LoginMessages.passwordPolicy,
      );
    });

    test('VAL-021 contains email local part', () {
      expect(
        PartnerPasswordPolicy.validate(
          password: 'Partner1!',
          confirm: 'Partner1!',
          corporateEmail: 'partner1@insurer.example',
        ),
        LoginMessages.passwordContainsUser,
      );
    });

    test('accepts a compliant password', () {
      expect(
        PartnerPasswordPolicy.validate(
          password: 'GoodPass1!',
          confirm: 'GoodPass1!',
          corporateEmail: 'partner.rm@insurer.example',
        ),
        isNull,
      );
    });
  });

  group('FakeLoginDirectory', () {
    test('VAL-006 then VAL-007 then VAL-008 lock after three failures', () {
      final dir = FakeLoginDirectory();
      LoginOutcome r = dir.submitCredentials(
        type: LoginUserType.bankRm,
        identifier: 'RM1001',
        password: 'wrong',
        captcha: kDemoCaptcha,
      );
      expect(r.message, LoginMessages.incorrectPasswordRemaining(2));

      r = dir.submitCredentials(
        type: LoginUserType.bankRm,
        identifier: 'RM1001',
        password: 'wrong',
        captcha: kDemoCaptcha,
      );
      expect(r.message, LoginMessages.incorrectPasswordRemaining(1));

      r = dir.submitCredentials(
        type: LoginUserType.bankRm,
        identifier: 'RM1001',
        password: 'wrong',
        captcha: kDemoCaptcha,
      );
      expect(r.message, LoginMessages.lockedFailedAttempts);

      r = dir.submitCredentials(
        type: LoginUserType.bankRm,
        identifier: 'RM1001',
        password: 'BankPass1!',
        captcha: kDemoCaptcha,
      );
      expect(r.message, LoginMessages.lockedFailedAttempts);
    });

    test('VAL-009 wrong captcha does not consume a password attempt', () {
      final dir = FakeLoginDirectory();
      final r = dir.submitCredentials(
        type: LoginUserType.bankRm,
        identifier: 'RM1001',
        password: 'wrong',
        captcha: 'NOPE',
      );
      expect(r.message, LoginMessages.wrongCaptcha);
      expect(dir.find(LoginUserType.bankRm, 'RM1001')!.failedPasswordAttempts, 0);
    });

    test('VAL-010 inactivity lock', () {
      final dir = FakeLoginDirectory();
      final r = dir.submitCredentials(
        type: LoginUserType.bankRm,
        identifier: 'RM-INACTIVE',
        password: 'BankPass1!',
        captcha: kDemoCaptcha,
      );
      expect(r.message, LoginMessages.lockedInactivity);
    });

    test('BR-LOGIN-003 OTP then sign-in session', () {
      final dir = FakeLoginDirectory();
      final creds = dir.submitCredentials(
        type: LoginUserType.bankRm,
        identifier: 'RM1001',
        password: 'BankPass1!',
        captcha: kDemoCaptcha,
      );
      expect(creds.ok, isTrue);
      expect(creds.session, isNotNull);

      final bad = dir.verifyOtp(creds.session!, '000000');
      expect(bad.message, LoginMessages.incorrectOtpRemaining(4));

      final ok = dir.verifyOtp(creds.session!, kDemoOtp);
      expect(ok.ok, isTrue);
    });

    test('five incorrect OTPs return to login and do not lock the account', () {
      final dir = FakeLoginDirectory();
      final creds = dir.submitCredentials(
        type: LoginUserType.bankRm,
        identifier: 'RM1001',
        password: 'BankPass1!',
        captcha: kDemoCaptcha,
      );
      LoginOutcome r = creds;
      for (var i = 0; i < 5; i++) {
        r = dir.verifyOtp(creds.session!, '000000');
      }
      expect(r.message, LoginMessages.otpMaxAttempts);
      expect(dir.find(LoginUserType.bankRm, 'RM1001')!.state,
          LoginAccountState.active);
    });

    test('BR-LOGIN-005 first-time partner must use Unlock User', () {
      final dir = FakeLoginDirectory();
      final denied = dir.submitCredentials(
        type: LoginUserType.insurancePartnerRm,
        identifier: 'partner.rm@insurer.example',
        password: 'anything1!',
        captcha: kDemoCaptcha,
      );
      expect(denied.ok, isFalse);

      final unlock = dir.startUnlock(
        type: LoginUserType.insurancePartnerRm,
        identifier: 'partner.rm@insurer.example',
        captcha: kDemoCaptcha,
      );
      expect(unlock.ok, isTrue);
      final verified = dir.verifyOtp(unlock.session!, kDemoOtp);
      expect(verified.ok, isTrue);
      final next = dir.afterUnlockOtp(unlock.session!.account,
          partnerWantsNewPassword: false);
      expect(next.account, isNotNull);
      expect(next.message, isNull);

      final set = dir.setPartnerPassword(
        account: next.account!,
        password: 'GoodPass1!',
        confirm: 'GoodPass1!',
      );
      expect(set, isNull);

      final login = dir.submitCredentials(
        type: LoginUserType.insurancePartnerRm,
        identifier: 'partner.rm@insurer.example',
        password: 'GoodPass1!',
        captcha: kDemoCaptcha,
      );
      expect(login.ok, isTrue);
    });
  });
}
