# Story — WS2-LOGIN-S17 Assess whether unlock may start

```yaml
story:
  id: WS2-LOGIN-S17
  title: "Assess whether unlock may start"
  epic: WS2-LOGIN
  stage: L4
  workstream: WS-2
  work_type: SEC
  priority: P2
  size: S
  owner_persona: "Amit / Engineering"
  status: IMPLEMENTED
  traces_to:
    - SUG-20261028-1lg
    - Login BRD v3.1
```

## 1. Story

The BFF needs an unlock decision before it issues an unlock OTP, so that an already usable account gets no screen and an inactive account is refused.

This operation is not called by the app. The BFF calls it with `X-Internal-Service-Key`.
A missing or unknown key is `401 UNAUTHORIZED`. The privilege is `LOGIN_ORCHESTRATE` except where the story says otherwise.


`POST /internal/v1/login/unlock/assess`. Body: `userType`, `loginId`.

## 2. Acceptance criteria

```gherkin
AC1
  Given a locked account or first_login true
  When assess is called
  Then the route is UNLOCK_OK

AC2
  Given an active account that is not first-time and not locked
  When assess is called
  Then the route is NOT_REQUIRED and no field changes

AC3
  Given an inactive account
  When assess is called
  Then the route is INACTIVE

AC4
  Given no business_user
  When assess is called
  Then the route is UNKNOWN
```

- [x] AC1
- [x] AC2
- [x] AC3
- [x] AC4


## 3. Test approach

| Level | What is tested | Owner |
|---|---|---|
| Unit | Route and counter rules | Amit |
| Component | Service key and privilege | Amit |
| Integration | BFF calls this only on the path named above | Swapnali |
| E2E | Covered through the public story, not by a direct app call | Swapnali |

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

- [x] App cannot call this path
- [x] Password or network credential is not logged
- [ ] §19 audit event is not written by the login handlers (C8)
- [x] Contract row is in the service LLD

## 7. Evidence produced

| Artefact | Level | Location |
|---|---|---|
| LLD | Spec | `identity-authorization-service/LLD.md` §2 |
| Flow | Spec | `LOGIN-BRD-V31-API-FLOWS.md` §6–§8 |

## 8. Notes

Assess does not log the user in and does not write last successful login.
