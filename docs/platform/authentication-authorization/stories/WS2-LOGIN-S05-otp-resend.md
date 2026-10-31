# Story — WS2-LOGIN-S05 Resend an OTP without resetting attempts

```yaml
story:
  id: WS2-LOGIN-S05
  title: "Resend an OTP without resetting attempts"
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

> As a **Bank RM or Insurance RM waiting on an OTP**, I need to **ask for a new code**, so that **I can finish the challenge without resetting the attempts I already used**.

`POST /api/v1/auth/otp/resend` with `challengeId`. Same operation for every OTP purpose.

## 2. Acceptance criteria

```gherkin
AC1
  Given a live challenge younger than 2 minutes
  When the caller resends
  Then outcome is WAIT with resendAvailableInSeconds
  And the current code still verifies

AC2
  Given a live challenge at least 2 minutes old and fewer than 3 resends used
  When the caller resends
  Then outcome is OTP_SENT
  And the previous code no longer verifies
  And attempts used are unchanged

AC3
  Given three resends already used
  When the caller resends again
  Then the response is VAL-017 and the challenge is deleted
```

- [x] AC1
- [x] AC2
- [x] AC3

## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | Wait, invalidate previous code, fourth resend ends the journey | Amit |
| Component | Resend does not create a session | Amit |
| Integration | Missing challenge returns VAL-015 | Swapnali |
| E2E | Resend control on the OTP screen | Swapnali |

- [x] `LoginJourneyServiceTest.resendWaitsThenInvalidatesThePreviousCodeWithoutResettingAttempts`
- [x] `LoginJourneyServiceTest.fourthResendEndsTheJourney`

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

- [x] Limits are 2 minutes, 3 resends, 10 minute challenge TTL
- [x] No session cookie
- [ ] Replacement code is not sent by SMS or email

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| Flow | Spec | `LOGIN-BRD-V31-API-FLOWS.md` §11, §15 |
| Service test | Unit | `LoginJourneyServiceTest` |

## 8. Notes

The BFF LLD records three resends. The fourth request is the one that returns `VAL-017`.
