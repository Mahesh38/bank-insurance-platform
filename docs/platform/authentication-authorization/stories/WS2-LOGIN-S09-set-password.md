# Story — WS2-LOGIN-S09 Set or reset an Insurance RM platform password

```yaml
story:
  id: WS2-LOGIN-S09
  title: "Set or reset an Insurance RM platform password"
  epic: WS2-LOGIN
  stage: L4
  workstream: WS-2
  work_type: SEC
  priority: P2
  size: M
  owner_persona: "Rajal / Product"
  status: IMPLEMENTED
  traces_to:
    - SUG-20261028-1lg
    - Login BRD v3.1
```

## 1. Story

> As an **Insurance RM continuing from first-time unlock, forgot password, or expiry**, I need to **submit a new platform password and its confirmation**, so that **the next sign-in uses that password and this call does not open a session**.

`POST /api/v1/auth/password` with `resetToken`, `newPassword`, `confirmPassword`. The token's purpose is `FIRST_TIME`, `FORGOT`, or `EXPIRY`. Bank RM has no set-password step.

## 2. Acceptance criteria

```gherkin
AC1
  Given a single-use reset token inside 10 minutes and a password that passes every rule
  When set-password is submitted
  Then outcome is PASSWORD_SET
  And no session cookie or sessionHandle is returned
  And the next sign-in must still pass captcha, password, and a login OTP

AC2
  Given new and confirm differ
  When set-password is submitted
  Then the response is VAL-019 and the hash is unchanged

AC3
  Given the new value is exactly 123456, outside 8–16 characters, contains whitespace, contains the registered email, or misses uppercase, lowercase, digit, or a non-alphanumeric character
  When set-password is submitted
  Then the first failing rule is returned and the hash is unchanged

AC4
  Given the new password matches the current hash
  When set-password is submitted
  Then the response is VAL-020

AC5
  Given purpose FORGOT while the account is locked
  When set-password is submitted
  Then the response is FORGOT_LOCKED and the hash is unchanged
```

- [x] AC1
- [x] AC2
- [x] AC3
- [x] AC4
- [x] AC5

## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | Confirm mismatch uses the design sentence | Amit |
| Component | Rule order stops at the first failure | Amit |
| Integration | Locked forgot path leaves the hash unchanged | Swapnali |
| E2E | After PASSWORD_SET the app returns to sign-in | Swapnali |

- [x] `LoginJourneyServiceTest.passwordMismatchUsesTheDesignSentence`
- [x] `PlatformLoginServiceTest.expiredPasswordDoesNotCountAsAFailureAndMustDifferFromThePrevious`

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

- [x] Plaintext is not stored or logged (`SetPasswordRequest.toString` is `SetPasswordRequest[resetToken]`)
- [x] Storage is PBKDF2-HMAC-SHA256 via S20
- [ ] Success audit event is not written (C8)
- [x] No session

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| Flow | Spec | `LOGIN-BRD-V31-API-FLOWS.md` §7, §9, §10, §12 |
| Service tests | Unit | `LoginJourneyServiceTest`, `PlatformLoginServiceTest` |

## 8. Notes

The checklist shown while typing is a client concern. The server checks at submit and returns the first failure.
