# Story — WS2-LOGIN-S08 Send the OTP after Insurance RM password expiry

```yaml
story:
  id: WS2-LOGIN-S08
  title: "Send the OTP after Insurance RM password expiry"
  epic: WS2-LOGIN
  stage: L4
  workstream: WS-2
  work_type: SEC
  priority: P2
  size: S
  owner_persona: "Rajal / Product"
  status: IMPLEMENTED
  traces_to:
    - SUG-20261028-1lg
    - Login BRD v3.1
```

## 1. Story

> As an **Insurance RM whose platform password has expired**, I need to **ask for an OTP with the expiry ticket from sign-in**, so that **a code is sent only after I choose to continue**.

`POST /api/v1/auth/password-expired/send-otp` with `expiryTicket`. Bank RM does not use this operation. The network password does not expire here.

## 2. Acceptance criteria

```gherkin
AC1
  Given sign-in returned PASSWORD_EXPIRED with an expiry ticket
  When this operation is called with that ticket
  Then outcome is OTP_SENT with purpose EXPIRY
  And no session is created

AC2
  Given a missing, expired, or already used ticket
  When this operation is called
  Then the challenge is not issued

AC3
  Given the user has not called this operation
  When they only hold the expiry ticket
  Then set-password rejects that ticket
```

- [x] AC1
- [x] AC2
- [x] AC3

## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | Expiry ticket does not send an OTP until asked | Amit |
| Component | Ticket TTL 10 minutes | Amit |
| Integration | Set-password refuses the raw expiry ticket | Swapnali |
| E2E | Expired-password screen has an explicit send action | Swapnali |

- [x] `LoginJourneyServiceTest.expiredPasswordIssuesATicketAndDoesNotSendAnOtpUntilAsked`

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

- [x] OTP waits for this call (`LoginJourneyController.sendExpiredOtp`)
- [x] Failure counter was not incremented at sign-in
- [ ] OTP is not sent by SMS or email

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| Flow | Spec | `LOGIN-BRD-V31-API-FLOWS.md` §10 |
| Service test | Unit | `LoginJourneyServiceTest` |

## 8. Notes

Expiry is 60 days from `password_changed_at`, checked only after the hash matches. A null timestamp is not expiry.
