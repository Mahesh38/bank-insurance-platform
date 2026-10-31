# Story — WS2-LOGIN-S13 Verify a Bank RM password with the bank directory

```yaml
story:
  id: WS2-LOGIN-S13
  title: "Verify a Bank RM password with the bank directory"
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

The BFF needs the bank directory to accept or reject a Bank RM network password, so that the platform never stores that password and never binds LDAP from the cluster.

This operation is not called by the app. The BFF calls it with `X-Internal-Service-Key`.
A missing or unknown key is `401 UNAUTHORIZED`. The privilege is `LOGIN_ORCHESTRATE` except where the story says otherwise.


`POST /internal/v1/auth/ad-verify` on `identity-provider-adapter-service`. Body: `employeeId`, `password`. Response: `authenticated`, `accountActive`, `employeeId`, `username`, `email`. Privilege is the adapter internal service key, not `LOGIN_ORCHESTRATE`.

## 2. Acceptance criteria

```gherkin
AC1
  Given a Bank RM preview route of READY
  When the password matches an active directory account
  Then authenticated is true and accountActive is true

AC2
  Given the directory account is inactive
  When verify is called
  Then accountActive is false
  And the BFF returns VAL-007 without recording a password failure

AC3
  Given the password does not match
  When verify is called
  Then authenticated is false
  And the BFF records the failure through S14

AC4
  Given an Insurance RM sign-in
  When the BFF checks the password
  Then this operation is not called

AC5
  Given the request is logged
  When toString runs
  Then the password is absent
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

- [x] `AdVerifyControllerTest` posts `/internal/v1/auth/ad-verify`
- [x] `LoginJourneyServiceTest.inactiveDirectoryAccountDoesNotCountAPasswordFailure`
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
| Controller | Code | `services/identity-provider-adapter-service/.../IdentityProviderController.java` |
| HTTP test | Component | `AdVerifyControllerTest` |
| Flow | Spec | `LOGIN-BRD-V31-API-FLOWS.md` §4 |

## 8. Notes

Production reachability is the existing bank AD-verify API through Apigee private (`ADR-020`). The adapter does not return a session to the app.
