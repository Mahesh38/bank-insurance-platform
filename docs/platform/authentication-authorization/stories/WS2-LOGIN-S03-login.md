# Story — WS2-LOGIN-S03 Sign in with captcha and password

```yaml
story:
  id: WS2-LOGIN-S03
  title: "Sign in with captcha and password"
  epic: WS2-LOGIN
  stage: L4
  workstream: WS-2
  work_type: SEC
  priority: P2
  size: L
  owner_persona: "Rajal / Product"
  status: IMPLEMENTED
  traces_to:
    - SUG-20261028-1lg
    - Login BRD v3.1
```

## 1. Story

> As a **Bank RM or Insurance RM**, I need to **submit my login id and password with a captcha**, so that **a correct credential starts a login OTP and no session exists until that OTP is verified**.

`POST /api/v1/auth/login`.

| User | `userType` | Login ID | Password checked by |
|---|---|---|---|
| Bank RM | `BANK_RM` | Employee ID | S13 AD-verify, then S14 |
| Insurance RM | `INSURANCE_RM` | Corporate email | S15 platform hash |

Body: `clientType`, `userType`, `loginId`, `password`, `captchaId`, `captchaValue`.

## 2. Acceptance criteria

```gherkin
AC1
  Given a READY Bank RM and a matching captcha
  When AD-verify accepts the network password and the directory account is active
  Then outcome is OTP_SENT with challengeId, maskedMobile, and maskedEmail
  And no WORKFORCE_SESSION cookie and no sessionHandle are set

AC2
  Given a READY Insurance RM and a matching captcha
  When the platform password matches and is inside 60 days
  Then outcome is OTP_SENT
  And no AD-verify call is made

AC3
  Given captcha failure, unknown id, first login, inactive, or a lock
  When login is submitted
  Then outcome is MESSAGE with VAL-004, VAL-005, VAL-006, VAL-012, VAL-007, VAL-010, or VAL-011
  And the password is not counted when the stop is before the password check

AC4
  Given a wrong password on a READY account
  When the attempt is the first, second, or third
  Then the message is VAL-008, VAL-009, or VAL-010
  And the third failure locks the account as PASSWORD

AC5
  Given an Insurance RM whose password matches and password_changed_at plus 60 days is not after now
  When login is submitted
  Then outcome is PASSWORD_EXPIRED with an expiry ticket
  And the failure counter does not move and no OTP is sent

AC6
  Given a body that still uses IAM-001 fields without the BRD fields
  When login is submitted
  Then the response is 422 VALIDATION_ERROR
```

- [x] AC1
- [x] AC2
- [x] AC3
- [x] AC4
- [x] AC5
- [x] AC6

## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | Route order, lock on third failure, expiry ticket | Amit |
| Component | Bank path calls AD-verify; Insurance path does not | Amit |
| Integration | 422 on a retired IAM-001 body; 403 without CSRF | Swapnali |
| E2E | Both toggles on the NIP login screen | Swapnali |

- [x] `LoginJourneyServiceTest` covers captcha stop, first-time stop, third Bank failure, inactive directory
- [x] `PlatformLoginServiceTest` covers Insurance lock, inactivity, expiry, unknown email

Negative cases that must keep failing:

- [x] Unknown employee and first-time stop before the password
- [x] Inactive directory account does not count a password failure
- [x] Sign-in command `toString` does not print the password

## 4. Compliance and security impact

```yaml
security_impact: high
compliance_impact: medium
controls_touched: [C5, C8, C10]
pii_involved: true
review_tier: T3
```

C5: passwords, OTP codes, and captcha answers are redacted from request `toString` and are not
logged. C8: these handlers do not yet emit the §19 audit events; that gap stays on the flow
document and is not closed by this story. C10: the browser session cookie is `HttpOnly`,
`Secure`, `SameSite=Strict`. No OAuth token is returned to the app. A T4 Security signature is
not claimed here.

## 5. Definition of Ready

- [x] Traces to Login BRD v3.1 and `SUG-20261028-1lg`
- [x] Acceptance criteria observable and testable
- [x] Security and compliance impact stated
- [x] Dependencies identified (BFF, and the internal operation named in the story)
- [x] Test approach names the existing test class
- [x] Test data is synthetic (employee id, corporate email, no production credential)
- [x] Sized (not XL)

## 6. Definition of Done

- [x] No session on this operation (`LoginJourneyController.login`)
- [x] Password redacted from logs
- [ ] SMS or email OTP is not sent; the code stays in process (`SUG-20261008-otp`)
- [ ] LoginSucceeded / LoginFailed audit events are not written (C8)
- [x] Contract: `docs/platform/contracts/workforce-access-bff/openapi.yaml`

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| Flow | Spec | `LOGIN-BRD-V31-API-FLOWS.md` §3–§5, §10 |
| Service tests | Unit | `LoginJourneyServiceTest`, `PlatformLoginServiceTest` |

## 8. Notes

Preview applies the 30-day inactivity rule before it returns a route. A null `password_changed_at` is not treated as expired. Employee status is the platform `business_user` row plus AD-verify. There is no CBS call on this path.
