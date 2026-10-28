# integration-hub-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Mahesh (architecture) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Domain to Hub](../../api-reference/interactions/domain-to-integration-hub.md) · [Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

**Skeleton** (6 main / 2 test classes; no migration). 0 / 3 operations. `HUB.ROUTING` is seeded in
the rule engine. The `hub` schema was **accepted in round 2 (E3)**; its design DDL is being added by
the DDL owner. Maturity ≈ 5 %.

## 2. Scope for this build

Canonical provider-neutral API; routing with `HUB.ROUTING` (`FIRST`, provider fixed on the first
row of a key); attribution injection; per-provider bulkhead + breaker; single adapter call with
**no retry on submit**; `SUBMISSION_UNCERTAIN` handling; canonical ↔ adapter mapping (LLD §5);
`hub.provider_request` idempotency; outbox for `AuditRecorded`.

## 3. Operations

[openapi.yaml](./openapi.yaml): `submitProviderRequest` (`QUOTE`, `PROPOSAL_SUBMIT`),
`pollProviderRequest`, `runProviderQuery` (`APPLICATION_STATUS`, eligibility criteria, form schema,
masters, product content, ULIP funds).

## 4. Data owned

Schema `hub` (E3): `provider_request` (`idempotency_key UNIQUE`, `request_hash`, provider,
`routing_version`, `adapter_job_id`, state) and `outbox_event` — shape in LLD §7; follow the design
DDL file once it appears in [schemas/](../../data-architecture/schemas/README.md). Role `app_hub`.
No customer PII. Retention 90 days (purge job).

## 5. Rule sets

`HUB.ROUTING` — inputs `operation`, `lob`, `productClass`, `insurerCode?`; `NO_ROUTE` →
`422 CONFIGURATION_UNRESOLVABLE`; MULTI quote must resolve to one provider in R0; submit / status
reuse the recorded provider. Attribution `distributorId` per (lob, insurer) from secrets/config.

## 6. Events

Produced: `AuditRecorded` (`PROVIDER_REQUEST_DISPATCHED` / `_FAILED`, `configVersionRef =
HUB.ROUTING@v`). Consumed: `RuleSetPublished`. Domain events stay the callers'.

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists — stub contract |
|---|---|---|
| 1sb-integration-service `/v1/*` | submit 2 s / 25 s **no retry**; poll 1 s / 2 s, 1 retry; query 2 s / 12 s, 1 retry for pure reads | Built; tests use WireMock fixtures per [1SB openapi](../1sb-integration-service/openapi.yaml) |
| 1SB adapter `GET /v1/jobs?idempotencyKey=` | — | **Not in the 1SB contract** (HUB-2): until it exists, `SUBMISSION_UNCERTAIN` resolves to `FAILED` + ops task after the reconciliation window |
| administration-config | 300 ms, cached, fail closed | `RuleSetSeeds` (`HUB.ROUTING`) |
| direct-insurer adapter | same port | Deferred — no route in R0 |

## 8. Invariants

`INV-ACL-01` provider vocabulary stays in the adapter (ArchUnit: no `adapter.onesb.*` types) ·
`INV-DIS-01` attribution injected, caller value rejected (`422 ATTRIBUTION_NOT_CALLER_SUPPLIED` +
security audit) · no automatic retry on submit · one key → one provider · fail closed on configuration ·
nothing dispatched unrecorded (DB down → `503` before any adapter call).

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-HUB-01 | QUOTE LIFE/TERM routes to `ONE_SB` (`HUB-TC-01`); HEALTH → `422 CONFIGURATION_UNRESOLVABLE` (`HUB-TC-02`) | `HUB.ROUTING` seed |
| AT-HUB-02 | Same key + same body → stored `202`, adapter not called; different body → `409 IDEMPOTENCY_CONFLICT`; row `DISPATCHING` → `409 REQUEST_IN_PROGRESS` | LLD §4.1 |
| AT-HUB-03 | `HUB.ROUTING` republished between two attempts with the same key → second goes to the recorded provider | LLD §7 |
| AT-HUB-04 | Any attribution field in the body → `422 ATTRIBUTION_NOT_CALLER_SUPPLIED` + audit | `INV-DIS-01` |
| AT-HUB-05 | Adapter times out at 25 s on submit → row `SUBMISSION_UNCERTAIN`, `202`, no resend | LLD §4.1 step 8 |
| AT-HUB-06 | `PARTIAL` adapter job → `PARTIALLY_COMPLETED` with offers and `partialErrors` (success) | LLD §4.2 |
| AT-HUB-07 | Mapping table LLD §5.1–§5.5 reproduced (e.g. `SAVINGS` → `SAVING`, `HALF_YEARLY` → `HALF-YEARLY`, `reqId` dropped) | LLD §5 |
| AT-HUB-08 | `PROPOSAL_SUBMIT` with a `quoteProviderRequestId` from another journey → `404` | LLD §4.1 step 3 |
| AT-HUB-09 | Breaker open for one provider → `503` for that provider only | LLD §13 |
| AT-HUB-10 | Missing agent on `PROPOSAL_SUBMIT` → `422 AGENT_ATTRIBUTION_MISSING` | LLD §4.1 step 4 |
| AT-HUB-11 | Poll by a service other than the caller → `404` | LLD §4.2 |
| AT-HUB-12 | ArchUnit: no `com.bank.insurance.onesb..` import | `INV-ACL-01` |

## 10. Build order

**Tier 4.** Real dependency: 1SB adapter (exists) + persistence (exists). Callers: catalogue,
quotation, proposal, policy-issuance. Startable now.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| HUB-1 | Design DDL file for schema `hub` + grants (E3 accepted) | Aarti | Migration parity (build from LLD §7 meanwhile) |
| HUB-2 | Adapter lookup by idempotency key for `SUBMISSION_UNCERTAIN` (needs a 1SB contract change) | 1SB adapter owner | Automatic reconciliation of uncertain submits |
| HUB-3 | Bank `insurerCode` = 1SB `manufacturerId`, or the mapping table | 1SB adapter owner | Correct proposal routing per insurer |
| HUB-4 | Adapter read timeout ≤ 20 s (today 30 s) to keep budgets nested | 1SB adapter owner | Budget nesting |
| HUB-5 | Attribution source per (lob, insurer): `distributorId` secrets, agent mapping from principal | Mahesh / Deepali | Real submits |
