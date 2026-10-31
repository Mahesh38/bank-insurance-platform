# Story — WS2-LOGIN-S04 Verify a login, unlock, forgot, or expiry OTP

```yaml
story:
  id: WS2-LOGIN-S04
  title: "Verify a workforce OTP challenge"
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

> As a **Bank RM or Insurance RM**, I need to **submit the one-time code for the challenge I was given**, so that **login opens a session and every other purpose finishes without a session**.

`POST /api/v1/auth/otp/verify` with `challengeId` and `otp`. One operation for purposes `LOGIN`, `UNLOCK`, `FORGOT`, and `EXPIRY`.

## 2. Acceptance criteria

```gherkin
AC1
  Given a LOGIN challenge whose six digits match and are inside 10 minutes
  When the caller verifies it
  Then outcome is AUTHENTICATED
  And WEB receives Set-Cookie WORKFORCE_SESSION (HttpOnly, Secure, SameSite Strict)
  And NATIVE receives sessionHandle and no cookie
  And the body contains no OAuth token
  And a second verify of the same challenge is VAL-015

AC2
  Given an UNLOCK challenge that matches
  When the caller verifies it
  Then a Bank RM who was first-time receives COMPLETED and no session
  And an Insurance RM who was first-time receives CONTINUATION purpose FIRST_TIME and a reset token
  And a locked user receives COMPLETED and no session

AC3
  Given a FORGOT or EXPIRY challenge that matches
  When the caller verifies it
  Then outcome is CONTINUATION with a single-use reset token
  And no session is created

AC4
  Given a wrong code with attempts left
  When the caller verifies it
  Then the response is VAL-014 and the challenge remains

AC5
  Given the fifth wrong code
  When the caller verifies it
  Then the response is VAL-016, the challenge is deleted, and the account is unchanged
```

- [x] AC1
- [x] AC2
- [x] AC3
- [x] AC4
- [x] AC5

## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | One session, second submit refused, five wrong codes | Amit |
| Component | Cookie only for WEB and only on AUTHENTICATED | Amit |
| Integration | Reset token rejected if it is still the expiry ticket | Swapnali |
| E2E | Login OTP then workspace; unlock OTP stays on the login screen | Swapnali |

- [x] `LoginJourneyServiceTest.loginOtpCreatesOneSessionAndASecondSubmitDoesNot`
- [x] `LoginJourneyServiceTest.fiveWrongOtpsEndTheChallengeWithoutASession`

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

- [x] Session minted only for LOGIN success (`LoginJourneyController.verifyOtp`)
- [x] OTP omitted from `OtpVerifyRequest.toString`
- [ ] The code is not delivered by SMS or email
- [ ] Login success does not emit `LoginSucceeded` (C8)

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| Flow | Spec | `LOGIN-BRD-V31-API-FLOWS.md` §4–§11 |
| Service test | Unit | `LoginJourneyServiceTest` |

## 8. Notes

Compare is constant-time on `salt$hash`. The code is not in the response. Masked mobile and email were returned when the challenge was issued.
