# Story — WS2-LOGIN-S12 Preview the account before a password is counted

```yaml
story:
  id: WS2-LOGIN-S12
  title: "Preview the account before a password is counted"
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

The BFF needs the account route before it checks a password, so that an unknown, inactive, first-time, or locked user is stopped without incrementing the failure counter.

This operation is not called by the app. The BFF calls it with `X-Internal-Service-Key`.
A missing or unknown key is `401 UNAUTHORIZED`. The privilege is `LOGIN_ORCHESTRATE` except where the story says otherwise.


`POST /internal/v1/login/preview` on `identity-authorization-service`. Body: `userType`, `loginId`. Bank RM is sent as `BANK_EMPLOYEE`. Insurance RM is sent as `INSURER_REPRESENTATIVE`.

## 2. Acceptance criteria

```gherkin
AC1
  Given no business_user for the login id
  When preview is called
  Then the route is UNKNOWN

AC2
  Given a user who is not active, except a pending first-time Insurance RM
  When preview is called
  Then the route is INACTIVE

AC3
  Given first_login is true
  When preview is called
  Then the route is FIRST_TIME
  And a later password check is not started by this call

AC4
  Given a password lock
  When preview is called
  Then the route is LOCKED_PASSWORD

AC5
  Given the later of last successful login and lock_cleared_at plus 30 days is not after now
  When preview is called for a user who is not first-time and not already locked
  Then the account locks as INACTIVITY and the route is LOCKED_INACTIVITY

AC6
  Given none of those stops
  When preview is called
  Then the route is READY
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
| Unit | Route and counter rules | Amit |
| Component | Service key and privilege | Amit |
| Integration | BFF calls this only on the path named above | Swapnali |
| E2E | Covered through the public story, not by a direct app call | Swapnali |

- [x] `PlatformLoginServiceTest.inactivityLocksAndSuccessfulLoginResetsItOnlyAfterComplete`
- [x] `PlatformLoginServiceTest.unknownInsuranceEmailAndInactiveUserStop`

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
| LLD | Spec | `docs/platform/contracts/identity-authorization-service/LLD.md` §2 |
| Service test | Unit | `PlatformLoginServiceTest` |

## 8. Notes

A user with neither last-login nor `lock_cleared_at` is not inactivity-locked. Unlock does not write last successful login.
