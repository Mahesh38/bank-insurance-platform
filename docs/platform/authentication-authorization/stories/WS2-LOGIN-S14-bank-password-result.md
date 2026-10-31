# Story — WS2-LOGIN-S14 Record the Bank RM directory password result

```yaml
story:
  id: WS2-LOGIN-S14
  title: "Record the Bank RM directory password result"
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

The BFF needs to record whether AD-verify accepted the Bank RM, so that the failure counter and the password lock live on the business user and the network password is not stored.

This operation is not called by the app. The BFF calls it with `X-Internal-Service-Key`.
A missing or unknown key is `401 UNAUTHORIZED`. The privilege is `LOGIN_ORCHESTRATE` except where the story says otherwise.


`POST /internal/v1/login/bank-password-result`. Body: `businessUserId`, `accepted`.

## 2. Acceptance criteria

```gherkin
AC1
  Given AD-verify returned authenticated true and accountActive true
  When this operation is called with accepted true
  Then the route stays READY and the failure counter is not incremented

AC2
  Given AD-verify returned authenticated false
  When this operation is called with accepted false
  Then the counter increments
  And two attempts left is the source of VAL-008, one left is VAL-009, and the third lock is VAL-010

AC3
  Given the account is already password-locked
  When a later correct directory password is reported
  Then the lock is not cleared and another failure is not added

AC4
  Given the directory account was inactive
  When the BFF handles that result
  Then this operation is not called
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

- [x] `LoginJourneyServiceTest.thirdBankPasswordFailureLocksAndACorrectPasswordOnlyIssuesAnOtp`

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
| Controller | Code | `IdentityAuthorizationController.bankPasswordResult` |

## 8. Notes

The request body is a boolean. It does not contain the network password.
