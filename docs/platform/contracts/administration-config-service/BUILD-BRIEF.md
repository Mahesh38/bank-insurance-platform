# administration-config-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Mahesh (architecture) and Amit (engineering) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Rule-engine guide](../rule-engine/LLD.md) · [ADR-024](../../architecture-review/ADR-024-configuration-driven-rule-engine.md) ·
[Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

| Area | State |
|---|---|
| Code | **Implemented** rule-set store (`services/administration-config-service`, 23 main / 4 test classes): controller, lifecycle, maker-checker, test-case gate, resolve, seed import as DRAFT |
| Contract operations | 9 / 9 coded; YAML has no `x-implementation-status` markers yet |
| Persistence | Flyway `V2__administration_rule_set_version`, vendor `V3` immutability trigger (PostgreSQL; H2 comment only) |
| Events | `RuleSetPublished` written to `administration.outbox_event`; **no relay wired** (no `bank-common-events` dependency) and the event is **not in the catalogue** |
| Security | Approver roles asserted by `X-Actor-Roles` (interim, LLD §10 #1) |
| Error codes | `RULE_SET_INVALID`, `RULE_SET_TEST_FAILED`, `MAKER_CHECKER_VIOLATION` — being moved from a service-local provider into the shared catalogue in round 2 |

Maturity estimate: ≈ 75 %. This is the **first real dependency of every deciding service**.

## 2. Scope for this build

1. Mark the 9 operations `BUILT` and add the conformance test (playbook §5).
2. Wire the outbox relay (`JdbcOutboxRelay`, `@Scheduled drain`) and emit `RuleSetPublished` in the
   `event-envelope/v1` shape once the catalogue owner adds its schema (LLD §10 #4).
3. Prove the immutability trigger and resolve query on PostgreSQL 16 (playbook §10).
4. Keep `X-Actor-Roles` until the PDP permission `rule-set.approve` exists, then switch (open item CFG-1).
5. Import the round-2 seeds when the rule-engine owner adds them (`LEAD.POLICY`, `CUSTOMER.POLICY`,
   E7) — as DRAFT only. Never submit or publish a seed.
6. **Not in scope:** the generic `configuration_record` / `config:resolve` design (superseded by rule
   sets, LLD §1); consent statements and notification templates storage (open item CFG-2).

## 3. Operations

All in [openapi.yaml](./openapi.yaml): `createRuleSetDraft`, `listRuleSetVersions`,
`getRuleSetVersion`, `replaceRuleSetDraft`, `submitRuleSetVersion`, `approveRuleSetVersion`,
`rejectRuleSetVersion`, `simulateRuleSetVersion`, `resolveRuleSet` — all coded; work is markers,
relay, PostgreSQL proof and the PDP switch.

## 4. Data owned

Schema `administration` — design [`13-administration.sql`](../../data-architecture/schemas/13-administration.sql):
`rule_set_version` (built), `outbox_event` (built, ADR-025 columns), `idempotency_record` (design),
`configuration_record` (design only, superseded for rule-shaped configuration).

## 5. Rule sets

Evaluates none for business; uses the engine to **validate**, **run test cases** (approve,
simulate) and **simulate**. Stores every seed in
[`rulesets/`](../../../../libs/bank-rule-engine/src/main/resources/rulesets/index.txt).

## 6. Events

| Direction | Event | Notes |
|---|---|---|
| Produced | `RuleSetPublished` | Topic per ADR-025 `{env}.admin-config.rule-set.v1`; consumers: every deciding service (cache invalidation). Needs catalogue entry first |
| Produced | `AuditRecorded` | Publish / reject are regulated configuration actions (ADR-026, E2) |
| Consumed | none | |

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists |
|---|---|---|
| identity-authorization PDP `rule-set.approve` (target) | 300 ms, fail closed | Keep `X-Actor-Roles` (current code); do not invent a permission name in the PDP — CFG-1 |

Inbound: every deciding service resolves with 300 ms, no retry, 60 s L1 cache.

## 8. Invariants

Published version immutable (`SUIT-R07`, trigger + domain) · maker ≠ checker · approver role per
classification · no publish with a failing test case · no compiled-in default · seeds never
auto-published · `INV-CFG-02` (never update in place).

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-CFG-01 | POST a document with a schema error → `422 RULE_SET_INVALID`, one `errors[]` entry per JSON path, nothing stored | LLD §5, ADR-024 §2 |
| AT-CFG-02 | Approve by the maker → `403 MAKER_CHECKER_VIOLATION` | ADR-024 §5.2 |
| AT-CFG-03 | Approve a `COMPLIANCE` set without `COMPLIANCE_APPROVER` → `403 FORBIDDEN` | ADR-024 §5.2 |
| AT-CFG-04 | Approve with one failing test case → `422 RULE_SET_TEST_FAILED`; status stays `PENDING_APPROVAL` | ADR-024 §5.3 |
| AT-CFG-05 | On PostgreSQL, `UPDATE`/`DELETE` of a `PUBLISHED` row raises (trigger `trg_rule_set_version_immutable`) | `SUIT-R07`, LLD §7 |
| AT-CFG-06 | Resolve with a `{lob, productClass}` version and a wildcard version → specific wins; a future `effectiveFrom` version is not returned before its time | LLD §3 Resolve |
| AT-CFG-07 | Resolve with no version in force → `CONFIGURATION_UNRESOLVABLE` | ADR-024 §5.4 |
| AT-CFG-08 | Startup imports every seed as `DRAFT` with maker `system:seed-import`; a second start inserts nothing; nothing is published | LLD §3 Seed import |
| AT-CFG-09 | Approve writes `RuleSetPublished` to the outbox in the same transaction; relay publishes one envelope keyed by `ruleSetId` | ADR-025 §3 |
| AT-CFG-10 | Two concurrent approves → one `200`, one `409 ILLEGAL_TRANSITION` | LLD §7 |
| AT-CFG-11 | Reject without remarks → `422 MISSING_REQUIRED_FIELD` | LLD §5 |
| AT-CFG-12 | Conformance test passes for all 9 operations | ADR-027 |

## 10. Build order

**Tier 0** — no upstream dependency; build and deploy first. Every deciding service stubs it with
[`RuleSetSeeds`](../../../../libs/bank-rule-engine/src/main/java/com/bank/rules/engine/RuleSetSeeds.java)
until it is reachable.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| CFG-1 | PDP permission `rule-set.approve` (and maker permission) to replace `X-Actor-Roles` | Deepali | Go-live (security), not start |
| CFG-2 | Where consent **statements** (C7: status `APPROVED`) and **notification templates** live — the contract only stores `rule-set/v1` documents; consent and notification LLDs resolve them "from configuration" | Mahesh | Consent capture and notification sends at go-live; both can stub the port |
| CFG-3 | `CONFIGURATION_UNRESOLVABLE` 422 (registry) vs 503 (ADR-024) | Mahesh | Nothing (registry followed) |
| CFG-4 | `RuleSetPublished` schema in the events catalogue | Events owner | Relay publish |
| CFG-5 | Seed content approvals (each seed's `governance` block); new round-2 seeds `LEAD.POLICY`, `CUSTOMER.POLICY`; seeds for `JOURNEY.POLICY`, `CATALOGUE.ELIGIBILITY`, `AUDIT.SOLD.COMPLETENESS`, `NOTIFICATION.TEMPLATES` do not exist | Shailja, Rajal, BRD owner, Mahesh | Go-live of each deciding service |
| CFG-6 | `RETIRED` has no endpoint (retire = publish successor) | Mahesh | Nothing |
| CFG-7 | Admin UI / BFF route for authors and checkers | Delivery | Operating the store without API tools |
