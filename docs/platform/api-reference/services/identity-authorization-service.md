# identity-authorization-service — business identity and PDP (#3, WS-2)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | The **source of truth for business authorization**. Maps a provider login to a platform `businessUserId`; answers *"may this subject do this action on this resource, now?"* (Policy Decision Point, default-deny); runs maker-checker for partner-user creation and provisions approved partners into the IdP through an outbox |
| **Module** | `services/identity-authorization-service` · port 8083 · `service-id: authz` · layer L5 · INTERNAL |
| **Base path** | `/internal/v1` — cluster-private |
| **Callers** | BFF (resolve at login; decisions — target on every regulated action) · domain services (target: re-check regulated actions, `INV-ACT-01`) · admin surface via BFF (partner users) |
| **Owns data** | Aurora schema **`identity`** (Flyway `V1__identity_authorization_schema.sql`, `V2__seed_role_permission_catalog.sql`) · role `app_identity` |
| **Calls out** | `identity-provider-adapter-service` `POST /internal/v1/identities` (provisioning outbox) |
| **Code status** | **IMPLEMENTED** — resolve, decide, partner submit/approve, provisioning outbox |
| **Never** | Trust an IdP group or claim as a permission; allow on timeout |

---

## 1. API index

| Method | Path | Status | Caller | Purpose |
|---|---|---|---|---|
| `POST` | `/internal/v1/identities/resolve` | IMPLEMENTED | BFF at login | Provider identity → business user (creates bank employees on first login) |
| `POST` | `/internal/v1/authorization/decisions` | IMPLEMENTED | BFF / domain services | Allow / deny with reason and matched policy |
| `POST` | `/internal/v1/partner-users` | IMPLEMENTED | Admin (maker) via BFF | Submit a partner-user creation for approval |
| `POST` | `/internal/v1/approval-requests/{approvalId}/approve` | IMPLEMENTED | Admin (checker) via BFF | Approve; activates the user and queues IdP provisioning |
| `POST` | `/internal/v1/authorize` | CONTRACTED (HLD §5.3 name) | — | Same intent as `/authorization/decisions`; code path wins |

---

## 2. Endpoints

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
   (§2.3–§2.4). Today this is an unmapped `IllegalStateException` → **500** (known gap; target a
   typed `403`).

> A freshly created bank employee has **no roles**. Every decision for them is `DEFAULT_DENY`
> until a `user_role` row (and, for regulated actions, a valid `certification`) exists. Seeding
> or syncing roles from bank HR is open WS-2 work.

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
| 3 | Explicit `DENY` entitlement matching action + scope | `EXPLICIT_DENY` / `entitlement:<scope>` |
| 4 | Resource branch not in the subject's active branches (unless a `GLOBAL *` grant) | `BRANCH_OUT_OF_SCOPE` / `branch-boundary` |
| 5 | Partner on a `lead.*` / `proposal.*` action and resource not partner-visible | `RESOURCE_NOT_SHARED_WITH_PARTNER` / `partner-sharing` |
| 6 | `regulatedAction` and bank employee without a valid mandatory certification | `MANDATORY_CERTIFICATION_INVALID` / `certification-gate` |
| 7 | Direct `GRANT` entitlement matching action + scope | **allow** `DIRECT_SCOPED_GRANT` |
| 8 | Role grants the permission (or `*`) | **allow** `ROLE_GRANT` / `role-permission` |
| 9 | Nothing matched | `DEFAULT_DENY` / `default-deny` |

Facts are loaded per call from `business_user`, `user_role` ⨝ `role_permission`, `entitlement`,
`user_branch_assignment` and `certification` — each filtered by `valid_from <= now < valid_until`.

**Caller contract (S-02):** 300 ms, **no retry**, **fail closed** — a timeout or error is a deny
(`403 AUTHORIZATION_UNAVAILABLE`), never a degraded allow. Map deny reasons to the registry:
`DEFAULT_DENY`, `EXPLICIT_DENY`, `ACCOUNT_SUSPENDED`, `CROSS_INSURER_DENIED`,
`OUT_OF_BRANCH_SCOPE`, `SP_CERTIFICATION_REQUIRED`.

### 2.3 `POST /internal/v1/partner-users` (maker)

Headers: `X-Actor-Id` (maker, required). Body: `username` (`@NotBlank`), `email` (`@Email`),
`firstName`, `lastName`, `insurerCode` (`@NotBlank`).
Behind the scenes, one transaction: reject duplicate username → `INSERT business_user`
(`INSURER_REPRESENTATIVE`, `PENDING`) → `INSERT approval_request` (`PARTNER_USER_CREATE`, `PENDING`,
maker). Response `202 { "userId", "approvalId", "status": "PENDING" }`.

### 2.4 `POST /internal/v1/approval-requests/{approvalId}/approve` (checker)

Header `X-Actor-Id` (checker). One transaction:
`SELECT … FOR UPDATE` the approval → must be `PENDING` → **checker ≠ maker** → user → `ACTIVE` →
approval `APPROVED` → `INSERT outbox_event (IDENTITY_PROVISIONING_REQUESTED, {businessUserId})`.
Response `200 { "approvalId", "targetStatus": "ACTIVE" }`.

Then, asynchronously, `ProvisioningOutboxDispatcher` (every 5 s, `identity.provisioning.enabled=true`)
picks unpublished events with `attempts < 10`, calls
`POST {idp-adapter}/internal/v1/identities`, stores the returned `provider_subject` on the user and
sets `published_at`. A failure increments `attempts` and records `last_error` (retry, idempotent —
the adapter finds an existing user by `business_user_id`). This is gate criterion `A.6`.

---

## 3. Data owned — schema `identity`

| Table | Purpose |
|---|---|
| `business_user` | Platform identity: `provider`, `provider_subject` (unique pair), `user_type`, `status` (`PENDING`·`ACTIVE`·`SUSPENDED`·`DISABLED`·`EXPIRED`), `insurer_code`, `policy_version`, validity window |
| `branch`, `insurer` | Reference |
| `user_branch_assignment` | Branch scope, effective-dated |
| `organization_relationship` | Manager ↔ subordinate |
| `certification` | SP / IRDAI certification, `mandatory_for_selling`, validity |
| `role`, `permission`, `role_permission` | Seeded catalogue (V2): roles `GENERAL_BANK_USER`, `RELATIONSHIP_MANAGER`, `PARTNER_SR`, `PARTNER_MANAGER`, `PARTNER_ADMIN`, `BANK_ADMIN`, `OPERATIONS`; permissions such as `lead.create`, `lead.assign`, `suitability.perform`, `quote.create`, `proposal.submit` (regulated), `policy.view` |
| `user_role` | Role assignment, effective-dated, `assigned_by` |
| `entitlement` | Scoped `GRANT` / `DENY` (`GLOBAL`·`BRANCH`·`INSURER`·`RESOURCE`) with reason and approver |
| `approval_request` | Maker-checker |
| `bulk_import`, `bulk_import_row` | Partner bulk onboarding (designed) |
| `outbox_event` | Provisioning requests |

## 4. Sources

- `services/identity-authorization-service/src/main/java/com/bank/identity/authz/**`
- `src/main/resources/db/migration/V1__identity_authorization_schema.sql` · `V2__seed_role_permission_catalog.sql`
- [Authentication & authorization](../../authentication-authorization/README.md) · [Security architecture](../../ws3-platform/04-security-architecture.md) · [`01-identity.sql`](../../data-architecture/schemas/01-identity.sql)
