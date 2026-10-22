# identity-authorization-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Deepali (security) and Amit (engineering) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Service page](../../api-reference/services/identity-authorization-service.md) · [Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

| Area | State |
|---|---|
| Code | **Implemented** (23 main / 12 test classes): resolve, PDP decisions, partner maker-checker, provisioning outbox dispatcher (lease + `SKIP LOCKED`, V4) |
| Contract operations | 4 / 4 coded (`resolveIdentity`, `decide`, `submitPartnerUser`, `approvePartnerUser`) |
| Round 2 (in flight in the working tree) | **E5** direct partner creation by a bank admin (`partner_user.create`), Keycloak provisioning at creation, maker-checker kept behind `identity.partner-onboarding.approval-required` (default `false`); **E6** permission `lead.exception.approve` (migration `V5__partner_onboarding_and_lead_exception_approve`) |
| Gaps | ArchUnit test absent (LLD §8 #7); role seeding / HR sync open; PDP actions used by the new services are not all in the permission catalogue (AUTHZ-2) |

Maturity estimate: ≈ 65 %.

## 2. Scope for this build

1. Finish E5 exactly as decided: creation requires `partner_user.create` (bank admin roles only);
   with `approval-required=false` the user is provisioned at creation and becomes `ACTIVE` only when
   the IdP account exists; with `true` the existing approval flow runs unchanged. Contract update
   first if the response changes (contract owner).
2. E6: `lead.exception.approve`, branch-scoped, granted only to bank-employee roles; an
   `INSURER_REPRESENTATIVE` is always denied.
3. ArchUnit test; markers; conformance test; PostgreSQL proof of the dispatcher claim
   (`FOR UPDATE SKIP LOCKED` with two concurrent claimers).
4. Add PDP **actions** for new services only when their contract/LLD names them (AUTHZ-2); which
   roles hold them is data approved by Deepali — do not invent grants.

## 3. Operations

[openapi.yaml](./openapi.yaml): `resolveIdentity`, `decide`, `submitPartnerUser`, `approvePartnerUser`.

## 4. Data owned

Schema `identity` — design [`01-identity.sql`](../../data-architecture/schemas/01-identity.sql);
Flyway V1–V4 (+ V5 in flight): `business_user`, `role`, `permission`, `role_permission`,
`user_role`, `entitlement`, `certification`, `user_branch_assignment`, `approval_request`,
`outbox_event` (internal work queue, not on MSK), plus organisation tables in the design.

## 5. Rule sets

None — authorization policy is data in schema `identity` (LLD §4). E5 flag
`identity.partner-onboarding.approval-required` is a Spring property (owner decision), default `false`.

## 6. Events

`IDENTITY_PROVISIONING_REQUESTED` on the local outbox — drained by the service's own dispatcher, not
published (catalogue "Not on the bus"). `AuditRecorded` for partner creation / approval / status
change (ADR-026, E2). Consumed: none.

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists |
|---|---|---|
| identity-provider-adapter `provisionIdentity` / `setIdentityEnabled` | connect 1 s / read 3 s (LLD §7); adapter's own Keycloak budget 2 s / 3 s (E8) | Built |

## 8. Invariants

Default deny; first match wins · IdP group never a permission · partners never self-create ·
maker ≠ checker (when approval is on) · approval single-use · partner `ACTIVE` only after the IdP
account exists · only actor-callers (BFF) may assert `X-Actor-Id` · `INV-ACT-01` (certification at
the instant) · `INV-ACT-02` (partner reps never regulated actions) · E6 (partners never approve exceptions).

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-AUTHZ-01 | Unknown subject → `200 {allowed:false, reasonCode: UNKNOWN_SUBJECT}` | LLD §3.2 |
| AT-AUTHZ-02 | RM with expired SP certification asking `lead.assign` → deny `MANDATORY_CERTIFICATION_INVALID` | `INV-ACT-01` |
| AT-AUTHZ-03 | Insurer partner rep asking any regulated action → deny | `INV-ACT-02` |
| AT-AUTHZ-04 | Insurer partner rep asking `lead.exception.approve` → deny; bank admin in branch → allow; bank admin outside branch → `BRANCH_OUT_OF_SCOPE` | E6 |
| AT-AUTHZ-05 | Missing / unknown `X-Internal-Service-Key` → `401`; a non-actor-caller asserting `X-Actor-Id` → `403` | LLD §2 |
| AT-AUTHZ-06 | `approval-required=false`: admin with `partner_user.create` creates a partner → provisioning queued at creation; user `ACTIVE` only after provision succeeds | E5 |
| AT-AUTHZ-07 | Creation by an actor without `partner_user.create` (e.g. RM) → `403` | E5 |
| AT-AUTHZ-08 | `approval-required=true`: create → `PENDING`; approve by maker → `403`; approve by checker → provisioning | E5, LLD §3.3 |
| AT-AUTHZ-09 | Two dispatchers on PostgreSQL claim disjoint rows; no double provisioning; row parked after max attempts | LLD §3.3, ADR-025 |
| AT-AUTHZ-10 | ArchUnit rules active | playbook §3.1 |
| AT-AUTHZ-11 | Conformance test passes | ADR-027 |

## 10. Build order

**Tier 1** (PDP). Every mutating service depends on it at go-live; every builder stubs it until then
with fixtures from this [openapi.yaml](./openapi.yaml) (`allowed:true/false` + `reasonCode`, and timeout).

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| AUTHZ-1 | Role seeding / HR sync for JIT bank employees | Aarti / Deepali | Go-live (every RM starts default-deny) |
| AUTHZ-2 | PDP action catalogue for the new services (`journey.start`, `suitability.perform`, `rule-set.approve`, quote / proposal / payment / refund actions) and the roles granted | Deepali, Mahesh | Real PDP calls from those services (they stub until listed) |
| AUTHZ-3 | Which hierarchy level approves which lead exception rule (`LEAD.EXCEPTION` `approverRoles`) is configuration in lead-service; the PDP only checks the permission | BRD owner | Lead exception approvals |
| AUTHZ-4 | Caller auth → mTLS | Shivanshi | Go-live |
