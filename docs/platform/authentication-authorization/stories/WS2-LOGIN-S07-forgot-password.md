# Story — WS2-LOGIN-S07 Start Insurance RM forgot password

```yaml
story:
  id: WS2-LOGIN-S07
  title: "Start Insurance RM forgot password"
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

> As an **Insurance RM who has forgotten the platform password**, I need to **submit my corporate email and a captcha**, so that **an eligible account can start a reset without revealing a password and without a session**.

`POST /api/v1/auth/forgot-password`. Body: `clientType`, `email`, `captchaId`, `captchaValue`. There is no Bank RM operation.

## 2. Acceptance criteria

```gherkin
AC1
  Given an active or first-time Insurance RM and a matching captcha
  When forgot-password is submitted
  Then outcome is OTP_SENT with purpose FORGOT
  And the current password hash is unchanged

AC2
  Given an email that is not a business user
  When forgot-password is submitted
  Then the response is VAL-023

AC3
  Given a locked Insurance RM
  When forgot-password is submitted
  Then the response is FORGOT_LOCKED and no OTP is sent

AC4
  Given an inactive Insurance RM
  When forgot-password is submitted
  Then the response is FORGOT_INACTIVE

AC5
  Given a Bank RM employee id
  When it is sent to this operation
  Then this operation does not reset a network password
```

- [x] AC1
- [x] AC2
- [x] AC3
- [x] AC4
- [x] AC5

## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | Locked account does not send an OTP | Amit |
| Component | Email value is not taken from the sign-in form | Amit |
| Integration | Unknown, inactive, and locked responses | Swapnali |
| E2E | Forgot password is absent on the Bank RM toggle | Swapnali |

- [x] `LoginJourneyServiceTest.forgotPasswordWhileLockedDoesNotSendAnOtp`

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

- [x] No session and no password field
- [x] Lock is not cleared
- [ ] OTP is not sent by email
- [ ] Bank RM password reset remains absent

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| Flow | Spec | `LOGIN-BRD-V31-API-FLOWS.md` §9 |
| Service test | Unit | `LoginJourneyServiceTest` |

## 8. Notes

A locked user unlocks first (S06). Forgot password does not clear `PASSWORD` or `INACTIVITY`.
