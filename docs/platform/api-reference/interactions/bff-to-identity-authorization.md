# BFF (and domain services) → identity-authorization (resolve + PDP, seam S-02)

[← API reference home](../README.md)

| | |
|---|---|
| **From → to** | `workforce-access-bff` (`IdentityAuthorizationClient`) → `identity-authorization-service` `/internal/v1/*`; target: also every service doing a regulated action (re-check, `ID-08`) |
| **Style** | Synchronous HTTP |
| **Caller auth** | `X-Internal-Service-Key` on every call — the BFF presents `IDENTITY_AUTHZ_BFF_INTERNAL_KEY` (`workforce.downstream.authorization-service-internal-key`), lead-service its own `LEAD_AUTHZ_INTERNAL_KEY` (`lead.authorization.internal-key`). Missing / wrong → `401 UNAUTHORIZED`. Only the BFF's key may carry `X-Actor-Id` (maker / checker) |
| **Budget** | Decisions: **300 ms, no retry, fail closed** (S-02). Resolve: login path |
| **Data behind it** | Aurora `identity` schema (business users, roles, entitlements, branches, certifications) |
| **Code status** | Resolve **IMPLEMENTED** and called at every login. Decisions **IMPLEMENTED** and called by the BFF before lead create and lead assign (`PolicyDecisionClient`, `SUG-20261021-gap`); lead-service re-checks create and assign itself (`PdpLeadAuthorizationAdapter`, `INV-ACT-01`); not yet called for other actions |

## 1. Two questions, two endpoints

| Question | Endpoint | When |
|---|---|---|
| *"Which platform user is this provider login?"* | `POST /internal/v1/identities/resolve` | Once per login, after the IdP proved who the person is |
| *"May this user do this action on this resource right now?"* | `POST /internal/v1/authorization/decisions` | Before every regulated action (target): lead create/assign, suitability, quote, proposal submit, payment initiate, partner reads |

**The IdP proves identity; the PDP decides permission.** A Keycloak group or a claim is never a
permission (standing constraint).

## 2. Resolve (implemented)

BFF builds the request from the provider session:

| Field | Bank AD login | Partner login |
|---|---|---|
| `provider` | `BANK_AD` | `KEYCLOAK` |
| `providerSubject` | employee id | Keycloak `sub` |
| `userType` | `BANK_EMPLOYEE` | `INSURER_REPRESENTATIVE` |
| `username`, `email` | from AD-verify (or the employee id) | from userinfo |
| `employeeId` | claim `employee_id` | claim `employee_id` / `emp_id` if present |

PDP: find by `(provider, providerSubject)` → refresh profile; else create **bank employees** as
`ACTIVE` (just-in-time); refuse unknown partners. BFF then requires `status = ACTIVE` (and for AD
`userType = BANK_EMPLOYEE`) before minting a session. The resolved `businessUserId`,
`policyVersion` and `insurerCode` are stored in the session and become `X-Actor-Id` downstream.

## 3. Decide (wired for lead create / assign)

```text
BFF action handler:
  decision = POST /internal/v1/authorization/decisions
               { subjectId: session.businessUserId, action: "lead.assign",
                 resource: { type: "LEAD", id, branchCode, insurerCode, ownerId,
                             assignedUserIds, partnerVisible, regulatedAction } }
  timeout 300 ms / error / allowed=false  →  403 (DEFAULT_DENY | EXPLICIT_DENY |
                                               OUT_OF_BRANCH_SCOPE | CROSS_INSURER_DENIED |
                                               SP_CERTIFICATION_REQUIRED | AUTHORIZATION_UNAVAILABLE)
  allowed                                 →  call the domain service
Domain service (regulated actions): re-check the same decision itself — a BFF allow is not trusted
across the hop (INV-ACT-01)  ← implemented in lead-service; target for the other domain services
```

As built (`PolicyDecisionClient`): own JDK HTTP client, connect + read **300 ms**
(`workforce.authorization.decision-timeout`), no retry; `lead.create` sends
`{type: "lead", branchCode: body.branchId, insurerCode: session.insurerCode, partnerVisible: true}`;
`lead.assign` sends `{type: "lead", id: leadId}`. `regulatedAction` is `false` for both (the
permission catalogue does not mark them regulated).

The response's `policyVersion` lets a caller detect that the user's grants changed since login.
Evaluation order and reason codes: [PDP page §2.2](../services/identity-authorization-service.md#22-post-internalv1authorizationdecisions).

## 4. Failure mapping

| Outcome | BFF action |
|---|---|
| PDP returns problem+json | `ErrorPropagation` keeps incident id + origin; the refusal reaches the RM as the PDP's code |
| PDP unreachable | `503 IDENTITY_PROVIDER_UNAVAILABLE` at login; for decisions: **deny** (`403 AUTHORIZATION_UNAVAILABLE`) |
| Partner not yet approved | `403 FORBIDDEN` from resolve, propagated to the app |
| Subject unknown to the PDP | `allowed: false`, `UNKNOWN_SUBJECT` → `403 DEFAULT_DENY` |

## 5. Sources

- `services/workforce-access-bff/src/main/java/com/bank/workforce/bff/client/IdentityAuthorizationClient.java`
- `services/workforce-access-bff/src/main/java/com/bank/workforce/bff/client/PolicyDecisionClient.java`
- [identity-authorization-service](../services/identity-authorization-service.md) · [WS-3 registration IF-2](../../ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md)
