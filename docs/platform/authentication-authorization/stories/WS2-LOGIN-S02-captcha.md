# Story — WS2-LOGIN-S02 Issue a single-use login captcha

```yaml
story:
  id: WS2-LOGIN-S02
  title: "Issue a single-use login captcha"
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
    - SUG-20261008-otp
```

## 1. Story

> As a **Bank RM or Insurance RM**, I need a **single-use captcha before sign-in, unlock, or forgot password**, so that **a script cannot spend a password or an unlock attempt without solving it**.

`POST /api/v1/auth/captcha`. In-process on the BFF. No session is created.

## 2. Acceptance criteria

```gherkin
AC1
  Given a caller with a valid CSRF token on web
  When they POST /api/v1/auth/captcha
  Then outcome is CAPTCHA with captchaId, display, and expiresInSeconds 300

AC2
  Given a captcha that is wrong, expired, missing, or already used
  When it is presented to login, unlock, or forgot-password
  Then the response is MESSAGE VAL-004
  And the password is not checked and the failure counter does not move

AC3
  Given a captcha that matched
  When the same captchaId is presented again
  Then it is rejected as already used
```

- [x] AC1
- [x] AC2
- [x] AC3

## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | Wrong captcha stops before the password | Amit |
| Component | TTL 5 minutes and single use | Amit |
| Integration | Captcha required on login, unlock, and forgot-password | Swapnali |
| E2E | Refresh captcha replaces the previous challenge | Swapnali |

- [x] `LoginJourneyServiceTest.wrongCaptchaDoesNotCheckThePassword`
- [ ] Image captcha vendor is parked (`SUG-20261008-otp`)

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

- [x] AC met in `LoginJourneyController.captcha` and `LoginJourneyService.issueCaptcha`
- [x] Captcha answer is not logged
- [ ] Captcha is text in the JSON body, not an image
- [ ] A second BFF instance does not see the hash

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| Flow | Spec | `docs/journey-execution/flows/LOGIN-BRD-V31-API-FLOWS.md` §1, §15 |
| Service test | Unit | `LoginJourneyServiceTest` |

## 8. Notes

The display string is returned once because no captcha image provider is named. That is current behaviour, not the target production ceremony.
