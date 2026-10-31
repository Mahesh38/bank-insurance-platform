# Story — WS2-LOGIN-S06 Start unlock for a Bank RM or Insurance RM

```yaml
story:
  id: WS2-LOGIN-S06
  title: "Start unlock for a Bank RM or Insurance RM"
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

> As a **Bank RM or Insurance RM who cannot sign in yet**, I need to **start unlock with my login id and a new captcha**, so that **a locked or first-time account can receive an unlock OTP without being logged in**.

`POST /api/v1/auth/unlock`. Body: `clientType`, `userType`, `loginId`, `captchaId`, `captchaValue`. No password field. The login page does not carry the login id into this screen.

## 2. Acceptance criteria

```gherkin
AC1
  Given a locked or first-time Bank RM or Insurance RM and a matching captcha
  When unlock is submitted
  Then outcome is OTP_SENT with purpose UNLOCK
  And no session is created

AC2
  Given an account that is already active, not first-time, and not locked
  When unlock is submitted
  Then outcome is NOT_REQUIRED and the body has no screen copy

AC3
  Given an inactive account
  When unlock is submitted
  Then the response is VAL-007

AC4
  Given an unknown Insurance email
  When unlock is submitted
  Then the response is VAL-006

AC5
  Given a wrong or reused captcha
  When unlock is submitted
  Then the response is VAL-004 and the account is unchanged
```

- [x] AC1
- [x] AC2
- [x] AC3
- [x] AC4
- [x] AC5

## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | Already-active unlock has no screen | Amit |
| Component | Unlock does not set a session cookie | Amit |
| Integration | Both user types reach OTP only when assess returns UNLOCK_OK | Swapnali |
| E2E | Unlock screen asks for login id again | Swapnali |

- [x] `LoginJourneyServiceTest.alreadyActiveUnlockHasNoScreen`
- [x] `PlatformLoginServiceTest.alreadyActiveUnlockChangesNothing`
- [x] `PlatformLoginServiceTest.firstTimeBankActivatesWithoutAPasswordChangeAndInsuranceMustSetOne`

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

- [x] Unlock does not log the user in
- [x] Password is not accepted on this operation
- [ ] Unlock OTP is not delivered by SMS or email

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| Flow | Spec | `LOGIN-BRD-V31-API-FLOWS.md` §6–§8 |
| Service tests | Unit | `LoginJourneyServiceTest`, `PlatformLoginServiceTest` |

## 8. Notes

Completion is S04 plus S18. Bank first-time activation does not set a password. Insurance first-time continues to S09.
