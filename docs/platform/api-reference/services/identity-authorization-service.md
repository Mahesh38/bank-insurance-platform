# identity-authorization-service — business identity and PDP (#3, WS-2)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | The **source of truth for business authorization**. Maps a provider login to a platform `businessUserId`; answers *"may this subject do this action on this resource, now?"* (Policy Decision Point, default-deny); lets a **bank admin create partner users directly** ([E5](../../architecture-review/DECISIONS-2026-10-22.md#owner-decisions)) and provisions them into the IdP through an outbox |
| **Module** | `services/identity-authorization-service` · port 8083 · `service-id: authz` · layer L5 · INTERNAL |
| **Base path** | `/internal/v1` — cluster-private, and **caller-authenticated**: every request needs `X-Internal-Service-Key` (one key per calling service, `identity.internal-auth.callers`); missing or wrong key → `401 UNAUTHORIZED` (READINESS-REVIEW §3.7) |
| **Callers** | BFF (resolve at login; decisions — target on every regulated action) · domain services (target: re-check regulated actions, `INV-ACT-01`) · admin surface via BFF (partner users) |
| **Owns data** | Aurora schema **`identity`** (Flyway `V1__identity_authorization_schema.sql`, `V2__seed_role_permission_catalog.sql`, `V3__partner_user_approve_permission.sql`, `V4__outbox_claim_lease_and_dead_letter.sql`, `V5__partner_onboarding_and_lead_exception_approve.sql`) · role `app_identity` |
| **Calls out** | `identity-provider-adapter-service` `POST /internal/v1/identities` (provisioning outbox; connect 1 s / read 3 s, `identity.provider-adapter.connect-timeout` / `read-timeout`) |
| **Startup guard** | Refuses to start under `prod` / `uat` with an H2 datasource (`ProductionDatasourceGuard`); `application-prod.yml` takes the PostgreSQL datasource from `AUTHZ_DATASOURCE_*` with no default |
| **Code status** | **IMPLEMENTED** — resolve, decide, partner create (E5; approve only when configured), provisioning outbox |
| **Contract** | **Authoritative (ADR-027):** [`contracts/identity-authorization-service/openapi.yaml`](../../contracts/identity-authorization-service/openapi.yaml) · [LLD](../../contracts/identity-authorization-service/LLD.md). Where this page and the contract differ, the contract wins. Partner create returns `status: PENDING` (ACTIVE only after provisioning); approve is `409` unless `identity.partner-onboarding.approval-required=true`; HLD `/internal/v1/authorize` superseded. Fix-pending: per-caller `X-Internal-Service-Key`, maker/checker from the authenticated principal |
| **Never** | Trust an IdP group or claim as a permission; allow on timeout |

---

## 1. API index

| Method | Path | Status | Caller | Purpose |
|---|---|---|---|---|
| `POST` | `/internal/v1/identities/resolve` | IMPLEMENTED | BFF at login | Provider identity → business user (creates bank employees on first login) |
| `POST` | `/internal/v1/authorization/decisions` | IMPLEMENTED | BFF / domain services | Allow / deny with reason and matched policy |
| `POST` | `/internal/v1/partner-users` | IMPLEMENTED | Bank admin via BFF | Create a partner user and queue IdP provisioning at once (E5); `PENDING` until provisioned |
| `POST` | `/internal/v1/approval-requests/{approvalId}/approve` | IMPLEMENTED, **disabled by default** | Bank admin (checker) via BFF | Only with `identity.partner-onboarding.approval-required=true`; otherwise `409 ILLEGAL_TRANSITION` |
| `POST` | `/internal/v1/authorize` | CONTRACTED (HLD §5.3 name) | — | Same intent as `/authorization/decisions`; code path wins |

---

## 2. Endpoints

### 2.0 Caller authentication and the actor

| Caller id | Key (env) | May call | May set `X-Actor-Id` |
|---|---|---|---|
| `workforce-access-bff` | `IDENTITY_AUTHZ_BFF_INTERNAL_KEY` | every endpoint | **yes** — the only caller allowed to, taking it from a vault-resolved session (`session.businessUserId`) |
| `lead-service` | `IDENTITY_AUTHZ_LEAD_INTERNAL_KEY` | every endpoint (uses `/authorization/decisions`) | no → `403 FORBIDDEN` on partner-user endpoints |

`InternalServiceAuthFilter` compares the presented key in constant time with every configured key
and binds the matching caller id. Production has no default keys (`application-prod.yml`). On the
maker / checker endpoints `ActorPrincipalResolver` then requires the caller to be listed in
`identity.internal-auth.actor-callers` and `X-Actor-Id` to be a business-user UUID — missing or
not a UUID → `401 UNAUTHORIZED`.

### 2.1 `POST /internal/v1/identities/resolve`

| Field | Type | Validation |
|---|---|---|
| `provider` | string | `@NotBlank` — `BANK_AD` or `KEYCLOAK` (set by the BFF) |
| `providerSubject` | string | `@NotBlank` — employee id for AD, Keycloak `sub` for partners |
| `userType` | `BANK_EMPLOYEE` \| `INSURER_REPRESENTATIVE` | `@NotNull` |
| `username` | string | `@NotBlank` |
| `email` | string | optional |
| `employeeId` | string | optional |

```json
{ "provider": "BANK_AD", "providerSubject": "EMP001", "userType": "BANK_EMPLOYEE", "username": "EMP001", "email": "emp001@example.test", "employeeId": "EMP001" }
```

Response `200`:

```json
{ "businessUserId": "7f0c2a6e-…", "status": "ACTIVE", "policyVersion": 1, "userType": "BANK_EMPLOYEE", "insurerCode": null }
```

**Behind the scenes** (one `@Transactional` on `identity`):

1. `SELECT … FROM business_user WHERE provider = ? AND provider_subject = ?` (unique index).
2. Found → refresh `username`, `email`, `employee_id` and return it (status unchanged — a
   `SUSPENDED` or `DISABLED` user stays so, and the BFF refuses login).
3. Not found and `userType = BANK_EMPLOYEE` → `INSERT business_user` with `status = ACTIVE`
   (just-in-time provisioning: AD already proved the person is an active employee).
4. Not found and `INSURER_REPRESENTATIVE` → refused: partners must be created and approved first
   (§2.3–§2.4). Returns **`403 FORBIDDEN`** ("Partner identity must be approved before first
   login") — fixed under `SUG-20261021-gap`; it used to be an unmapped 500.

> A freshly created bank employee has **no roles**. Every decision for them is `DEFAULT_DENY`
> until a `user_role` row (and, for regulated actions, a valid `certification`) exists. Seeding
> or syncing roles from bank HR is open WS-2 work. Locally, grant a test RM a role and branch with
> [`scripts/local/grant-local-rm.sql`](../../../../scripts/local/grant-local-rm.sql) after the first
> login — since the BFF now asks the PDP before lead create/assign, an ungranted user gets `403`.

### 2.2 `POST /internal/v1/authorization/decisions`

| Field | Type | Validation |
|---|---|---|
| `subjectId` | UUID | `@NotNull` — the business user id |
| `action` | string | `@NotBlank` — a permission code, e.g. `lead.assign`, `quote.create`, `proposal.submit` |
| `resource` | object | `@NotNull` |
| `resource.type`, `resource.id` | string | Used for `RESOURCE`-scoped entitlements |
| `resource.branchCode` | string | Branch boundary |
| `resource.insurerCode` | string | Tenant boundary for partners |
| `resource.ownerId`, `resource.assignedUserIds` | string / string[] | Context |
| `resource.partnerVisible` | boolean | Partner sharing gate |
| `resource.regulatedAction` | boolean | Triggers the certification gate |

```json
{
  "subjectId": "7f0c2a6e-…",
  "action": "proposal.submit",
  "resource": { "type": "PROPOSAL", "id": "01JQ…", "branchCode": "MUM001", "insurerCode": "INS01",
                "ownerId": "7f0c2a6e-…", "assignedUserIds": [], "partnerVisible": false, "regulatedAction": true }
}
```

Response `200` (a deny is a `200` with `allowed: false` — the caller converts it to its own `403`):

```json
{ "allowed": false, "reasonCode": "MANDATORY_CERTIFICATION_INVALID", "matchedPolicy": "certification-gate", "policyVersion": 1 }
```

**Evaluation order** (`AuthorizationPolicyEvaluator`, first match wins):

| # | Check | Deny reason / policy |
|---|---|---|
| 1 | Subject `ACTIVE` and inside `valid_from`/`valid_until` | `ACCOUNT_NOT_ACTIVE` / `lifecycle` |
| 2 | Partner touching another insurer's resource | `CROSS_INSURER_ACCESS` / `tenant-boundary` |
| 2a | Partner on an action prohibited to partner representatives — `identity.authorization.partner-representative.prohibited-actions` (seed `partner_user.create`, `partner_user.bulk_import`) or `prohibited-action-suffixes` (seed `.approve`); runs before any grant, so no role, entitlement or `*` overrides it (E5 / E6) | `PARTNER_ACTION_PROHIBITED` / `partner-prohibited-action` |
| 3 | Explicit `DENY` entitlement matching action + scope | `EXPLICIT_DENY` / `entitlement:<scope>` |
| 4 | Resource branch not in the subject's active branches (unless a `GLOBAL *` grant) | `BRANCH_OUT_OF_SCOPE` / `branch-boundary` |
| 5 | Partner on a `lead.*` / `proposal.*` action and resource not partner-visible | `RESOURCE_NOT_SHARED_WITH_PARTNER` / `partner-sharing` |
| 6 | `regulatedAction` and bank employee without a valid mandatory certification | `MANDATORY_CERTIFICATION_INVALID` / `certification-gate` |
| 7 | Direct `GRANT` entitlement matching action + scope | **allow** `DIRECT_SCOPED_GRANT` |
| 8 | Role grants the permission (or `*`) | **allow** `ROLE_GRANT` / `role-permission` |
| 9 | Nothing matched | `DEFAULT_DENY` / `default-deny` |

Facts are loaded per call from `business_user`, `user_role` ⨝ `role_permission`, `entitlement`,
`user_branch_assignment` and `certification` — each filtered by `valid_from <= now < valid_until`.

A `subjectId` that is not a known business user is answered **`allowed: false`,
`reasonCode: UNKNOWN_SUBJECT`** (it used to be an unmapped 500).

**Caller contract (S-02):** 300 ms, **no retry**, **fail closed** — a timeout or error is a deny
(`403 AUTHORIZATION_UNAVAILABLE`), never a degraded allow. The BFF's `PolicyDecisionClient` maps
reason codes to the registry:

| `reasonCode` | Error code returned to the app |
|---|---|
| `ACCOUNT_NOT_ACTIVE` | `ACCOUNT_SUSPENDED` |
| `CROSS_INSURER_ACCESS` | `CROSS_INSURER_DENIED` |
| `EXPLICIT_DENY` | `EXPLICIT_DENY` |
| `BRANCH_OUT_OF_SCOPE` | `OUT_OF_BRANCH_SCOPE` |
| `RESOURCE_NOT_SHARED_WITH_PARTNER` | `RESOURCE_NOT_FOUND` (do not confirm the resource exists) |
| `MANDATORY_CERTIFICATION_INVALID` | `SP_CERTIFICATION_REQUIRED` |
| `DEFAULT_DENY`, `UNKNOWN_SUBJECT`, `PARTNER_ACTION_PROHIBITED`, anything else | `DEFAULT_DENY` |

**`lead.exception.approve` (E6).** Lead exceptions are approved only by bank employees in the SP
hierarchy. The permission is seeded (V5) for `BANK_ADMIN` only — the catalogue has no
branch-manager / approver role yet — and is branch-scoped: callers send the lead's `branchCode`.
Which hierarchy level approves which rule is `LEAD.EXCEPTION` `approverRoles` (lead-service); this
PDP check is the second, independent gate. A partner representative is denied
`PARTNER_ACTION_PROHIBITED` even with an explicit grant.

### 2.3 `POST /internal/v1/partner-users` (bank admin creates — [E5](../../architecture-review/DECISIONS-2026-10-22.md#owner-decisions))

Headers: `X-Internal-Service-Key` (BFF) and `X-Actor-Id` (creator business-user UUID, required — §2.0).
Body: `username` (`@NotBlank`), `email` (`@Email`), `firstName`, `lastName`, `insurerCode` (`@NotBlank`).
Behind the scenes, one transaction: **PDP check** — the creator must be allowed `partner_user.create`
on a `partner_user` resource with the request's `insurerCode`. Only `BANK_ADMIN` holds it (V5 removed
it from `PARTNER_ADMIN`), and the PDP refuses it to every partner representative (`403 FORBIDDEN`);
other denials → `403` `DEFAULT_DENY` / `CROSS_INSURER_DENIED` / `EXPLICIT_DENY` /
`ACCOUNT_SUSPENDED`. Then: reject duplicate username (`409 CONFLICT`) → `INSERT business_user`
(`INSURER_REPRESENTATIVE`, `PENDING`, `created_by` = creator) →
`INSERT outbox_event (IDENTITY_PROVISIONING_REQUESTED, {businessUserId, requestedBy})` →
`INSERT outbox_event (PARTNER_USER_CREATED, {businessUserId, insurerCode, createdBy, approvalRequired, approvalId})`
(audit of who created whom). Response
`202 { "userId", "approvalId": null, "status": "PENDING", "approvalRequired": false }`.

**Maker-checker kept behind configuration.** With `identity.partner-onboarding.approval-required=true`
(env `IDENTITY_PARTNER_ONBOARDING_APPROVAL_REQUIRED`, default `false`) creation inserts an
`approval_request` (`PARTNER_USER_CREATE`, `PENDING`, maker) **instead of** the provisioning event
and returns the `approvalId`; provisioning waits for §2.4.

### 2.4 `POST /internal/v1/approval-requests/{approvalId}/approve` (checker — disabled by default)

With `approval-required=false` (the default) → **`409 ILLEGAL_TRANSITION`** ("approval not
required"). Otherwise: headers `X-Internal-Service-Key` (BFF) and `X-Actor-Id` (checker UUID, §2.0).
One transaction: `SELECT … FOR UPDATE` the approval → must be `PENDING` → **checker ≠ maker**
(`403 FORBIDDEN`) → **PDP check** `partner_user.approve` on the target's insurer (same denial
mapping as §2.3) → user `policy_version + 1`, **status stays `PENDING`** → approval `APPROVED` →
`INSERT outbox_event (IDENTITY_PROVISIONING_REQUESTED, {businessUserId, requestedBy})`.
Response `200 { "approvalId", "targetStatus": "PENDING" }`.

`partner_user.approve` was seeded by `V3` for `PARTNER_ADMIN` and `BANK_ADMIN`; `V5` leaves it with
`BANK_ADMIN` only (AI-drafted role mapping, awaiting Rajal / Deepali review).

The partner becomes **`ACTIVE` only when provisioning succeeds**: `ProvisioningOutboxDispatcher`
(`identity.provisioning.enabled=true` — on by default under `prod`, every `poll-interval`) **claims** up to `batch-size` due rows
in a short transaction — `SELECT … FOR UPDATE SKIP LOCKED` on PostgreSQL, plain `FOR UPDATE` on H2 —
and leases them (`claimed_until = now + lease`, default 2 min) so a second pod does not
double-provision. It then calls `POST {idp-adapter}/internal/v1/identities` outside the lock, stores
the returned `provider_subject`, sets the user `ACTIVE` and the row `published_at`. A failure
increments `attempts`, records `last_error` and releases the lease (retry, idempotent — the adapter
finds an existing user by `business_user_id`); after `max-attempts` (default 10) the row is
**dead-lettered** (`dead_lettered_at`) and logged at `ERROR` for an operator. This is gate
criterion `A.6`.

---

## 3. Data owned — schema `identity`

| Table | Purpose |
|---|---|
| `business_user` | Platform identity: `provider`, `provider_subject` (unique pair), `user_type`, `status` (`PENDING`·`ACTIVE`·`SUSPENDED`·`DISABLED`·`EXPIRED`), `insurer_code`, `policy_version`, validity window, `created_by` (V5, E5) |
| `branch`, `insurer` | Reference |
| `user_branch_assignment` | Branch scope, effective-dated |
| `organization_relationship` | Manager ↔ subordinate |
| `certification` | SP / IRDAI certification, `mandatory_for_selling`, validity |
| `role`, `permission`, `role_permission` | Seeded catalogue (V2): roles `GENERAL_BANK_USER`, `RELATIONSHIP_MANAGER`, `PARTNER_SR`, `PARTNER_MANAGER`, `PARTNER_ADMIN`, `BANK_ADMIN`, `OPERATIONS`; permissions such as `lead.create`, `lead.assign`, `suitability.perform`, `quote.create`, `proposal.submit` (regulated), `policy.view`, `lead.exception.approve` (V5, `BANK_ADMIN` only — E6) |
| `user_role` | Role assignment, effective-dated, `assigned_by` |
| `entitlement` | Scoped `GRANT` / `DENY` (`GLOBAL`·`BRANCH`·`INSURER`·`RESOURCE`) with reason and approver |
| `approval_request` | Maker-checker — used only when `identity.partner-onboarding.approval-required=true` |
| `bulk_import`, `bulk_import_row` | Partner bulk onboarding (designed) |
| `outbox_event` | Provisioning requests and `PARTNER_USER_CREATED` audit events; `claimed_until` (in-flight lease), `dead_lettered_at` (parked after `max-attempts`) — V4 |

## 4. Sources

- `services/identity-authorization-service/src/main/java/com/bank/identity/authz/**`
- `src/main/resources/db/migration/V1__identity_authorization_schema.sql` · `V2__seed_role_permission_catalog.sql` · `V3__partner_user_approve_permission.sql` · `V4__outbox_claim_lease_and_dead_letter.sql` · `V5__partner_onboarding_and_lead_exception_approve.sql`
- [Owner decisions 2026-10-22 (E5, E6, E8)](../../architecture-review/DECISIONS-2026-10-22.md#owner-decisions)
- [Authentication & authorization](../../authentication-authorization/README.md) · [Security architecture](../../ws3-platform/04-security-architecture.md) · [`01-identity.sql`](../../data-architecture/schemas/01-identity.sql)
