# Story — WS2-LOGIN-S20 Store an Insurance RM platform password

```yaml
story:
  id: WS2-LOGIN-S20
  title: "Store an Insurance RM platform password"
  epic: WS2-LOGIN
  stage: L4
  workstream: WS-2
  work_type: SEC
  priority: P2
  size: M
  owner_persona: "Amit / Engineering"
  status: IMPLEMENTED
  traces_to:
    - SUG-20261028-1lg
    - Login BRD v3.1
```

## 1. Story

The BFF needs the new Insurance RM password stored after the rules pass, so that first-time, forgot, and expiry continuations change the hash without opening a session.

This operation is not called by the app. The BFF calls it with `X-Internal-Service-Key`.
A missing or unknown key is `401 UNAUTHORIZED`. The privilege is `LOGIN_ORCHESTRATE` except where the story says otherwise.


`POST /internal/v1/login/password`. Body: `businessUserId`, `newPassword`, `confirmPassword`, `purpose`. Password fields are omitted from `toString`.

## 2. Acceptance criteria

```gherkin
AC1
  Given purpose FIRST_TIME and a password that passes the rules
  When set is called
  Then the result is SET
  And the hash is PBKDF2, first_login is false, and a PENDING Insurance RM becomes ACTIVE

AC2
  Given purpose FORGOT and the account is not locked
  When set is called
  Then the result is SET and the previous hash is replaced

AC3
  Given purpose FORGOT and the account is locked
  When set is called
  Then the result is LOCKED_UNCHANGED and the hash is unchanged

AC4
  Given purpose EXPIRY and a password that passes the rules, including difference from the current hash
  When set is called
  Then the result is SET and password_changed_at is now

AC5
  Given the new password fails confirm, the default value 123456, length, whitespace, email inclusion, character class, or equality with the current hash
  When set is called
  Then the first failure is returned and the hash is unchanged
```

- [x] AC1
- [x] AC2
- [x] AC3
- [x] AC4
- [x] AC5


## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | Route and counter rules | Amit |
| Component | Service key and privilege | Amit |
| Integration | BFF calls this only on the path named above | Swapnali |
| E2E | Covered through the public story, not by a direct app call | Swapnali |

- [x] `PlatformLoginServiceTest.expiredPasswordDoesNotCountAsAFailureAndMustDifferFromThePrevious`
- [x] `PlatformLoginServiceTest.firstTimeBankActivatesWithoutAPasswordChangeAndInsuranceMustSetOne`
- [x] `InternalCallerAuthenticationTest` posts `/internal/v1/login/password` without a valid caller

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

- [x] App cannot call this path
- [x] Password or network credential is not logged
- [ ] §19 audit event is not written by the login handlers (C8)
- [x] Contract row is in the service LLD

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| Flow | Spec | `LOGIN-BRD-V31-API-FLOWS.md` §12 |
| Caller test | Component | `InternalCallerAuthenticationTest` |

## 8. Notes

Bank RM never calls this operation. The plaintext is not stored.
