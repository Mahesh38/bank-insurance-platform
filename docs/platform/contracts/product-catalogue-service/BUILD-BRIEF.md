# product-catalogue-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Rajal (product) and Mahesh (architecture) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Scenario 09](../../api-reference/scenarios/09-pre-quote-reference-data.md) · [Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

**Skeleton** (6 main / 2 test classes, placeholder `V1`). 0 / 5 operations. Maturity ≈ 5 %.

## 2. Scope for this build

Effective-dated insurer / product reads with L1 + Valkey L2 cache, product-class list (replaces
the BFF static list), insurer criteria form via the Hub, two-stage eligibility check
(`CATALOGUE.ELIGIBILITY` first, insurer criteria second, both must pass), outbox.
Every product is `NOT_ELIGIBLE` until Rajal publishes band values — by design, not a defect.
Product master **content** (which insurers and products exist) is data the builder does not invent:
seed only via a migration fixture marked test-only, or leave empty (CAT-5).

## 3. Operations

[openapi.yaml](./openapi.yaml): `listOfferings`, `listProductClasses`, `getProduct`,
`getInsurerCriteriaForm`, `checkEligibility`.

## 4. Data owned

Schema `catalogue` — design [`07-catalogue.sql`](../../data-architecture/schemas/07-catalogue.sql):
`insurer`, `product` (INSERT-only for published rows, `ux_product_version`, `ck_product_lob`),
`outbox_event`; `eligibility_band` **superseded** (LLD §5.4, CAT-2) — do not build against it.

## 5. Rule sets

`CATALOGUE.ELIGIBILITY` (`FIRST`; refusals first; `CAT-ELIG-000` no band row → `NOT_ELIGIBLE /
CAT-CONFIG-MISSING`; catch-all `ELIGIBLE`); parameters `precedence`, `insurerCriteria.productClasses`
(seed `SAVINGS`, `ULIP`). **No seed exists** and it is not in ADR-024 §7 (CAT-1): test with a
sample rule set; production resolves → fail closed.

## 6. Events

Produced: `ProductVersionPublished` (LLD §8 — not in the catalogue, CAT-3), `AuditRecorded` on
product publish. Consumed: `RuleSetPublished`.

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists — stub contract |
|---|---|---|
| administration-config | 300 ms, cached | Sample `CATALOGUE.ELIGIBILITY` test resource |
| integration-hub `runProviderQuery` (`ELIGIBILITY_CRITERIA_GET` / `_SUBMIT`) | catalogue → Hub 14 s | Stub per [hub openapi](../integration-hub-service/openapi.yaml): accepted true / false, `503`, `504` |
| Valkey L2 (S-25) | 50 ms, a miss is a read | In-memory cache in tests |

Inbound: BFF, suitability, quotation — 500 ms, 1 retry; `eligibility:check` with stage 2 up to 15 s.

## 8. Invariants

Published rows never updated (new version) · `INV-LOB-02` lob ≠ product class · no thresholds in
code (`INV-CFG-01`) · insurer called only after the bank says yes · regulated decision never served
from a cache past TTL.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-CAT-01 | Product with no band row → `NOT_ELIGIBLE`, reason `CAT-CONFIG-MISSING`, insurer not called | LLD §5.1 |
| AT-CAT-02 | Stage 1 `NOT_ELIGIBLE` → stage 2 `NOT_EVALUATED`; Hub stub receives no call | LLD §5 precedence |
| AT-CAT-03 | TERM stage 1 `ELIGIBLE` → `ELIGIBLE` without Hub; SAVINGS without `insurerCriteriaValues` → `INSURER_CRITERIA_REQUIRED` | LLD §5.2 |
| AT-CAT-04 | Hub `accepted=false` → `NOT_ELIGIBLE / INSURER_CRITERIA_NOT_MET`; Hub down → `503`, never eligible by default | LLD §9 |
| AT-CAT-05 | Rule set unresolvable → `422 CONFIGURATION_UNRESOLVABLE` | ADR-024 |
| AT-CAT-06 | `listOfferings` honours `effective_from ≤ at < effective_to`; partner `insurerId` sees only that insurer | LLD §3 |
| AT-CAT-07 | Group B insurer returned with `insurerGroup: B` but not quotable | LLD §3 |
| AT-CAT-08 | Aurora down → display reads served from L1/L2 within TTL; `eligibility:check` → `503` | LLD §9 |
| AT-CAT-09 | `lob=HEALTH` → `422 UNSUPPORTED_LOB` | LLD §5.3 |
| AT-CAT-10 | Conformance test passes | ADR-027 |

## 10. Build order

**Tier 4.** Needs administration-config and Hub (both stubbable). Consumers: BFF, suitability,
quotation. Startable now.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| CAT-1 | Add `CATALOGUE.ELIGIBILITY` to ADR-024 §7 and seed it (content approver Rajal) | Mahesh | Real eligibility |
| CAT-2 | Drop / repurpose `catalogue.eligibility_band`; add `catalogue.outbox_event` in DDL | Aarti | Migration parity |
| CAT-3 | `ProductVersionPublished` in the events catalogue | Events owner | Publishing it |
| CAT-4 | Band values per product (age, sum assured, occupation); confirm Savings/ULIP need insurer criteria | Rajal | Go-live (all `NOT_ELIGIBLE` meanwhile) |
| CAT-5 | Product master data source (insurers / products / versions) and its load path | Rajal | Any offering at go-live |
