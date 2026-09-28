/// SCR-01 Login — Login BRD v1.0 (Rajal pack, `DOC-005`).
///
/// Demonstration only: Captcha and OTP are fixed demo values. Production
/// Captcha, SMS/email gateways and session timeout are Information Security
/// confirmations (`SEC-009`, `SEC-010`). Bank RM passwords are not stored by
/// the platform (`BR-LOGIN-002`); the fake directory exists so the screens
/// can be exercised.
library;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app.dart';
import '../data/login_fake.dart';
import '../design/components.dart';
import '../design/tokens.dart';
import '../domain/login.dart';
import '../guards/journey_guard.dart';

enum _LoginStep { credentials, otp, unlock, unlockOtp, createPassword }

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final FakeLoginDirectory _directory = FakeLoginDirectory();
  final _identifier = TextEditingController(text: 'RM1001');
  final _password = TextEditingController();
  final _captcha = TextEditingController();
  final _otp = TextEditingController();
  final _newPassword = TextEditingController();
  final _confirmPassword = TextEditingController();

  LoginUserType _type = LoginUserType.bankRm;
  _LoginStep _step = _LoginStep.credentials;
  OtpSession? _session;
  LoginAccount? _partnerForPassword;
  String? _error;
  String? _info;
  bool _busy = false;
  bool _hidePassword = true;
  bool _partnerResetIntent = false;

  @override
  void dispose() {
    _identifier.dispose();
    _password.dispose();
    _captcha.dispose();
    _otp.dispose();
    _newPassword.dispose();
    _confirmPassword.dispose();
    super.dispose();
  }

  void _setType(LoginUserType type) {
    setState(() {
      _type = type;
      _error = null;
      _info = null;
      _identifier.text =
          type == LoginUserType.bankRm ? 'RM1001' : 'partner.active@insurer.example';
    });
  }

  Future<void> _onSignIn() async {
    if (_busy) return;
    setState(() {
      _busy = true;
      _error = null;
      _info = null;
    });
    final result = _directory.submitCredentials(
      type: _type,
      identifier: _identifier.text,
      password: _password.text,
      captcha: _captcha.text,
    );
    setState(() {
      _busy = false;
      if (!result.ok) {
        _error = result.message;
        return;
      }
      _session = result.session;
      _otp.clear();
      _step = _LoginStep.otp;
    });
  }

  Future<void> _onVerifyOtp() async {
    final session = _session;
    if (session == null || _busy) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    final result = _directory.verifyOtp(session, _otp.text);
    if (!result.ok) {
      setState(() {
        _busy = false;
        _error = result.message;
        if (result.message == LoginMessages.otpMaxAttempts) {
          _step = _LoginStep.credentials;
          _session = null;
        }
      });
      return;
    }
    if (session.forUnlock) {
      final next = _directory.afterUnlockOtp(
        session.account,
        partnerWantsNewPassword: _partnerResetIntent,
      );
      setState(() {
        _busy = false;
        if (!next.ok) {
          _error = next.message;
          return;
        }
        if (next.message == null && next.account != null) {
          _partnerForPassword = next.account;
          _newPassword.clear();
          _confirmPassword.clear();
          _step = _LoginStep.createPassword;
          return;
        }
        _info = next.message;
        _session = null;
        _step = _LoginStep.credentials;
      });
      return;
    }
    setState(() => _busy = false);
    if (!mounted) return;
    JourneyScope.of(context).signIn();
    goReplace(context, AppRoute.pipeline);
  }

  void _openUnlock() {
    setState(() {
      _step = _LoginStep.unlock;
      _error = null;
      _info = null;
      _captcha.clear();
    });
  }

  void _onGetUnlockOtp() {
    if (_busy) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    final result = _directory.startUnlock(
      type: _type,
      identifier: _identifier.text,
      captcha: _captcha.text,
    );
    setState(() {
      _busy = false;
      if (!result.ok) {
        _error = result.message;
        return;
      }
      _session = result.session;
      _otp.clear();
      _step = _LoginStep.unlockOtp;
    });
  }

  void _onSetPartnerPassword() {
    final account = _partnerForPassword;
    if (account == null) return;
    final error = _directory.setPartnerPassword(
      account: account,
      password: _newPassword.text,
      confirm: _confirmPassword.text,
    );
    setState(() {
      if (error != null) {
        _error = error;
        return;
      }
      _info =
          'Password created. Please log in with your Corporate Email ID and new password.';
      _step = _LoginStep.credentials;
      _password.clear();
      _newPassword.clear();
      _confirmPassword.clear();
      _partnerForPassword = null;
      _partnerResetIntent = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return AppScreen(
      title: 'AU Bank — Insurance Platform',
      screenId: 'SCR-01',
      child: switch (_step) {
        _LoginStep.credentials => _credentials(),
        _LoginStep.otp || _LoginStep.unlockOtp => _otpPane(
            title: _step == _LoginStep.unlockOtp
                ? 'Unlock User — verify OTP'
                : 'Verify OTP',
          ),
        _LoginStep.unlock => _unlockPane(),
        _LoginStep.createPassword => _createPasswordPane(),
      },
    );
  }

  Widget _credentials() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SectionHeading(
          'Sign in',
          subtitle:
              'Login BRD v1.0. Bank RM uses Employee ID and the bank system '
              'password (not stored here). Insurance Partner RM uses Corporate '
              'Email ID and a platform password. Forgot Password is not offered. '
              'Captcha and OTP services in production are confirmed by Information Security.',
        ),
        AppCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Wrap(
                spacing: AppSpace.x2,
                children: [
                  for (final t in LoginUserType.values)
                    ChoiceChip(
                      label: Text(t.tabLabel),
                      selected: _type == t,
                      onSelected: (_) => _setType(t),
                    ),
                ],
              ),
              const SizedBox(height: AppSpace.x4),
              _field(
                key: const Key('login-identifier'),
                label: _type.identifierLabel,
                controller: _identifier,
              ),
              _field(
                key: const Key('login-password'),
                label: 'Password',
                controller: _password,
                obscure: _hidePassword,
                trailing: IconButton(
                  tooltip: _hidePassword ? 'Show password' : 'Hide password',
                  onPressed: () =>
                      setState(() => _hidePassword = !_hidePassword),
                  icon: Icon(_hidePassword
                      ? Icons.visibility_outlined
                      : Icons.visibility_off_outlined),
                ),
              ),
              Text('Demonstration Captcha: $kDemoCaptcha',
                  style: AppType.caption
                      .copyWith(color: AppColor.textSecondary)),
              _field(
                key: const Key('login-captcha'),
                label: 'Security Captcha',
                controller: _captcha,
              ),
              if (_error != null) ...[
                const SizedBox(height: AppSpace.x3),
                Text(_error!,
                    style: AppType.body
                        .copyWith(color: AppColor.statusError)),
              ],
              if (_info != null) ...[
                const SizedBox(height: AppSpace.x3),
                Text(_info!,
                    style: AppType.body
                        .copyWith(color: AppColor.statusSuccess)),
              ],
              const SizedBox(height: AppSpace.x4),
              AppButton(
                label: 'Sign In',
                icon: Icons.login,
                loading: _busy,
                onPressed: _onSignIn,
              ),
              const SizedBox(height: AppSpace.x3),
              Wrap(
                spacing: AppSpace.x3,
                children: [
                  TextButton(
                    onPressed: _openUnlock,
                    child: const Text('Unlock User'),
                  ),
                  TextButton(
                    onPressed: () => setState(
                        () => _info = LoginMessages.itWillProvideHelp),
                    child: const Text('Get Help'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _otpPane({required String title}) {
    final session = _session;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeading(
          title,
          subtitle:
              'The same six-digit OTP is sent to the registered mobile number '
              'and email ID. Valid for 10 minutes. Five attempts. Account is '
              'not locked for incorrect OTP. Demonstration OTP is $kDemoOtp.',
        ),
        AppCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (session != null) ...[
                LabelledValue('Mobile', session.account.maskedMobile),
                LabelledValue('Email', session.account.maskedEmail),
                const SizedBox(height: AppSpace.x3),
              ],
              _field(
                key: const Key('login-otp'),
                label: 'OTP',
                controller: _otp,
                keyboard: TextInputType.number,
                maxLength: 6,
              ),
              if (_error != null)
                Text(_error!,
                    style: AppType.body
                        .copyWith(color: AppColor.statusError)),
              const SizedBox(height: AppSpace.x3),
              Text(
                'Resend OTP is available 2 minutes after generation, maximum three resends (Login BRD).',
                style: AppType.caption.copyWith(color: AppColor.textSecondary),
              ),
              const SizedBox(height: AppSpace.x4),
              AppButton(
                label: 'Verify',
                loading: _busy,
                onPressed: _onVerifyOtp,
              ),
              TextButton(
                onPressed: () => setState(() {
                  _step = _LoginStep.credentials;
                  _session = null;
                  _error = null;
                }),
                child: const Text('Back to login'),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _unlockPane() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SectionHeading(
          'Unlock User',
          subtitle:
              'Single account-recovery function. Bank RM password is never '
              'created or reset on this platform. Partner first-time, forgotten '
              'and expired passwords continue to Create Password after OTP.',
        ),
        AppCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              LabelledValue('User type', _type.tabLabel),
              _field(
                key: const Key('unlock-identifier'),
                label: _type.identifierLabel,
                controller: _identifier,
              ),
              Text('Demonstration Captcha: $kDemoCaptcha',
                  style: AppType.caption
                      .copyWith(color: AppColor.textSecondary)),
              _field(
                key: const Key('unlock-captcha'),
                label: 'Security Captcha',
                controller: _captcha,
              ),
              if (_type == LoginUserType.insurancePartnerRm)
                CheckboxListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text(
                      'I forgot my password or it has expired — create a new one'),
                  value: _partnerResetIntent,
                  onChanged: (v) =>
                      setState(() => _partnerResetIntent = v ?? false),
                ),
              if (_error != null)
                Text(_error!,
                    style: AppType.body
                        .copyWith(color: AppColor.statusError)),
              const SizedBox(height: AppSpace.x3),
              AppButton(
                label: 'Get OTP',
                loading: _busy,
                onPressed: _onGetUnlockOtp,
              ),
              TextButton(
                onPressed: () => setState(
                    () => _info = LoginMessages.itWillProvideGuide),
                child: const Text('Download Unlock Guide'),
              ),
              TextButton(
                onPressed: () => setState(() {
                  _step = _LoginStep.credentials;
                  _error = null;
                }),
                child: const Text('Back to login'),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _createPasswordPane() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SectionHeading(
          'Create password',
          subtitle:
              '8–20 characters with uppercase, lowercase, number and special '
              'character. Must not contain the Corporate Email ID.',
        ),
        AppCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _field(
                key: const Key('login-new-password'),
                label: 'New Password',
                controller: _newPassword,
                obscure: _hidePassword,
              ),
              _field(
                key: const Key('login-confirm-password'),
                label: 'Confirm Password',
                controller: _confirmPassword,
                obscure: _hidePassword,
              ),
              if (_error != null)
                Text(_error!,
                    style: AppType.body
                        .copyWith(color: AppColor.statusError)),
              const SizedBox(height: AppSpace.x4),
              AppButton(
                label: 'Set Password',
                onPressed: _onSetPartnerPassword,
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _field({
    Key? key,
    required String label,
    required TextEditingController controller,
    bool obscure = false,
    Widget? trailing,
    TextInputType? keyboard,
    int? maxLength,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpace.x3),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: AppType.caption),
          const SizedBox(height: AppSpace.x1),
          TextField(
            key: key,
            controller: controller,
            obscureText: obscure,
            keyboardType: keyboard,
            maxLength: maxLength,
            inputFormatters: maxLength == 6
                ? [FilteringTextInputFormatter.digitsOnly]
                : null,
            decoration: InputDecoration(
              border: const OutlineInputBorder(),
              isDense: true,
              suffixIcon: trailing,
              counterText: '',
            ),
          ),
        ],
      ),
    );
  }
}
