# workforce-access-bff — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Deepali (security) and Amit (engineering) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Service page](../../api-reference/services/workforce-access-bff.md) · [Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

| Area | State |
|---|---|
| Code | **Implemented** (36 main / 20 test classes); all 20 contract operations coded |
| Gaps (LLD §9) | CSRF rejection is a bare Spring 403 (decided `403 CSRF_REJECTED`); missing query params / enum mismatches answer Spring 400 without the envelope; `X-Correlation-Id` optional in code; `Idempotency-Key` optional on `POST /leads`; reads have no PDP call; product classes static; organisation directory is a fixture |
| Budgets | idp-adapter read 6 s in code; **E8 sets the BFF → adapter budget to 4.5 s** |
| Contract coverage | Lead phase only — **no routes for the sale journey** (journey, consent, suitability, quote, proposal, payment) |

Maturity estimate: ≈ 75 % of the current contract; the journey routes are not contracted (BFF-1).

## 2. Scope for this build

1. Close the fix-pendings above (CSRF problem+json; envelope on every 4xx; correlation id).
2. E8: idp-adapter client connect / read as properties with env overrides; read **4.5 s**
   (above the adapter's AD 2 s + 3 s).
3. `GET /catalogue/product-classes` → product-catalogue `listProductClasses` (stub until built).
4. Add `x-implementation-status` markers and the conformance test.
5. **Not in scope until contracted:** any journey/consent/suitability/quote/proposal/payment route
   (BFF-1); PDP on reads (BFF-3).

## 3. Operations

All 20 in [openapi.yaml](./openapi.yaml) (`getCsrfToken`, `login`, `oidcCallback`,
`exchangeNativeCompletion`, `getSession`, `logout`, `getPipeline`, `searchCustomers`, `getCustomer`,
`getActiveLeads`, `createLead`, `getLead`, `assignLead`, `listCountryCodes`, `validateMobileNumber`,
`listProductClasses`, `listBranches`, `listVerticals`, `listSpecifiedPersons`,
`listRelationshipManagers`) — coded; work is the gaps in §2.

## 4. Data owned

Valkey session vault only (`workforce:pending:*` 5 min, `workforce:completion:*` 1 min,
`workforce:session:*` 8 h, AES-256-GCM). No Aurora, no DynamoDB.

## 5. Rule sets

None. Lead exception rules are lead-service's (`LEAD.EXCEPTION`).

## 6. Events

None produced or consumed. Login / denial evidence is audit: the BFF has no outbox; the owning
services write `AuditRecorded` (ADR-026). Whether login events need a BFF-side audit path is BFF-4.

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists |
|---|---|---|
| identity-provider-adapter (S-26) | connect 1 s / **read 4.5 s** (E8) | Built; local AD stub mode |
| identity-authorization resolve / PDP (S-02) | 500 ms / 2 s; PDP 300 ms, no retry | Built |
| lead-service (S-20) | 500 ms / 2 s | Built |
| customer-service (S-01) | 500 ms / 4 s | Built (partial); its target masking changes the response shape — follow its contract |
| product-catalogue `listProductClasses` | 500 ms, 1 retry (catalogue LLD §3) | Stub returning the contract fixture `TERM, SAVINGS, ULIP` from catalogue's YAML |

## 8. Invariants

Actor never from a body (`X-Actor-Id` = session business user) · provider tokens never leave the
vault · PDP fail closed · redaction boundary (`bank.error.boundary: PUBLIC`) · `distributorId`
never accepted (`INV-DIS-01`) · exception rules precede assignment (`D-019`).

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-BFF-01 | BANK_AD login happy path → `200`, session stored encrypted, cookie for WEB / handle for NATIVE | LLD §3.2 |
| AT-BFF-02 | AD verdict not authenticated → `401 AUTHENTICATION_FAILED` | LLD §3.2 step 4 |
| AT-BFF-03 | idp-adapter answers after 4.5 s → `503 IDENTITY_PROVIDER_UNAVAILABLE` within budget | E8 |
| AT-BFF-04 | Cookie-transport mutation without `X-XSRF-TOKEN` → `403 CSRF_REJECTED` problem+json | LLD §9 #2 |
| AT-BFF-05 | PDP timeout on `POST /leads` → `403 AUTHORIZATION_UNAVAILABLE`; lead-service not called | `INV-ACT-01`, LLD §5 |
| AT-BFF-06 | Each PDP reason in LLD §5 maps to its code (`RESOURCE_NOT_SHARED_WITH_PARTNER` → `404`) | LLD §5 |
| AT-BFF-07 | Downstream problem+json is propagated with the same code and `incidentId`; no `origin` / `diagnostic` outward | error model, LLD §7 |
| AT-BFF-08 | Assign on an exception hold → `200` lead unchanged, `exceptionRequired = true` | `D-019`, LLD §3.6 |
| AT-BFF-09 | Missing required query param → platform envelope (not Spring default) | LLD §9 #5 |
| AT-BFF-10 | Partner login for an un-provisioned / non-ACTIVE partner → refresh token revoked, `401` | LLD §3.3 |
| AT-BFF-11 | Body field `distributorId` / actor on create lead is rejected or ignored per contract; `X-Actor-Id` = session id | `INV-DIS-01` |
| AT-BFF-12 | Conformance test passes for every `BUILT` operation | ADR-027 |

## 10. Build order

**Tier 1** (identity edge). Upstream: idp-adapter, identity-authorization, lead, customer (all exist).
Catalogue is stubbed. Startable now.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| BFF-1 | **Contract the BFF routes for the sale journey** (journey, need analysis, consent status, quotes, proposal, payment, policy) — today the app has no API to drive journey-orchestration | Mahesh (contract), Flutter team | Go-live of the RM journey through the app; not the domain services |
| BFF-2 | `Idempotency-Key` required on every mutation once NIP-APP sends it | Flutter team | Contract tightening |
| BFF-3 | PDP on reads (pipeline, get, active-leads) | Deepali | Security sign-off |
| BFF-4 | Login / logout / denial audit path (ADR-026 accepted, BFF has no store) | Mahesh, Deepali | Audit completeness for access events |
| BFF-5 | Auth paths use `SCHEMA_INVALID`; registry has more precise codes (`INVALID_STATE`, `CODE_ALREADY_CONSUMED`, `RETURN_LOCATION_NOT_ALLOWED`) | Deepali | Reviewed contract change |
| BFF-6 | Organisation directory is a fixture; bank HR mapping is the source | Rajal, Aarti | Go-live |
