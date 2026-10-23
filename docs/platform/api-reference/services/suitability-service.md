# suitability-service — Need analysis and suitability (#7)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | Holds the **C1 hard gate**: *no quote for class X without a valid, unexpired evaluation whose outcome for X is allowed by `SUITABILITY.GATE`* (`INV-QUO-01` as amended by D4; the single `ELIGIBLE` outcome is superseded by the per-class record). Captures the need-analysis questionnaire (save / resume, single active editor), runs the product **mapping** against versioned rules, records the recommended product set, and locks the assessment at final BI. The framework is shared; the **rules are per LOB and come from configuration** (`#19`), never from `if` statements |
| **Module** | `services/suitability-service` · port 8093 · `service-id: suitability` · layer L5 · INTERNAL |
| **Base path** | `/internal/v1/assessments` (HLD §5.3) |
| **Callers** | Journey (S-07 evaluate, 3 s) via the BFF · Quotation (S-08 gate read, **500 ms, no retry, fail closed**) |
| **Calls out** | administration-config (questionnaire + mapping rule pack, S-21) · product-catalogue (eligible products, mapping output codes) |
| **Owns data** | Aurora schema **`suitability`** · role `app_suitability` |
| **Code status** | **SKELETON** (`GET /internal/v1/suitability/info`). All business endpoints below are CONTRACTED (paths, HLD §5.3) with **PROPOSED** bodies drafted from the Suitability BRD, the domain model and the DDL |
| **Contract** | [`contracts/suitability-service/openapi.yaml`](../../contracts/suitability-service/openapi.yaml) + [LLD](../../contracts/suitability-service/LLD.md) — authoritative (ADR-027), supersedes §2 below: rule sets `SUITABILITY.LIFE.{TERM,SAVINGS,ULIP}` + `SUITABILITY.GATE` (ADR-024), per-class outcome record (D4), `COMPLETED` immutable — a correction is a new superseding assessment (INV-SUI-01 wins over §2.2), gate read `GET /internal/v1/evaluations:usable` (refusal `403 SUITABILITY_REQUIRED`, unavailable → caller `503`, fail closed) |

> **Per-product-class suitability (decided 2026-10-21, `D-022`).** Each product class has its **own
> question set and matrix**, resolved from configuration by `(lob, productClass)` — the seller
> never picks it. **Term** asks the protection set (income, dependants, existing cover,
> liabilities, premium, tobacco, occupation, prior decline, replacement) and **does not ask
> investment horizon or risk attitude**; Savings adds objective and horizon; ULIP adds objective,
> horizon and risk attitude. C1 still applies to every class: a Term quote needs a Term evaluation
> that permits Term. A Term evaluation can never authorise a ULIP quote (`SUIT-R01c`). Rules:
> [rule pack §2.3](../../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#23-question-sets-by-product-class-v11-amendment--d-022).
> Still open: `OPEN-SUI-IMMUTABLE` (same id vs new id on a saved edit) and Compliance sign-off of
> each set's content (`OPEN-SUIT-01`).

### Question sets at a glance

| Class | Asked (besides 4 derived CBS inputs) | Not asked | Outcome rules |
|---|---|---|---|
| `TERM` | SQ-01 income, SQ-02 dependants, SQ-03 existing cover, SQ-04 liabilities, SQ-08 premium, SQ-09 tobacco, SQ-10 occupation, SQ-11 prior decline, SQ-12 replacement | objective, horizon, risk attitude | SUIT-R09–R12: protection gap + affordability |
| `SAVINGS` | Term set + SQ-05 objective, SQ-06 horizon | risk attitude | SUIT-R13–R15 |
| `ULIP` | SQ-01, SQ-05, SQ-06, SQ-07 risk attitude, SQ-08–SQ-12 | dependants, existing cover, liabilities | SUIT-R16–R19 |

---

## 1. API index

| Method | Path | Status | Caller | Purpose |
|---|---|---|---|---|
| `POST` | `/internal/v1/assessments` | CONTRACTED · body PROPOSED | Journey | Start an assessment for a lead (acquires edit lock) |
| `PUT` | `/internal/v1/assessments/{suitabilityId}/answers` | PROPOSED | Journey | Save a screen / answer set (new encrypted version) |
| `POST` | `/internal/v1/assessments/{suitabilityId}/evaluate` | PROPOSED | Journey | Run mapping → `COMPLETED` (`PRODUCTS`) or `NO_PRODUCT_FOUND` |
| `GET` | `/internal/v1/assessments/{suitabilityId}` | CONTRACTED | Journey, BFF | Status, outcome, recommended products (no answers) |
| `GET` | `/internal/v1/assessments/{suitabilityId}/validity?customerId&lob` | PROPOSED (the S-08 read) | Quotation | Is it usable for a quote **right now**? |
| `POST` | `/internal/v1/assessments/{suitabilityId}/lock` | PROPOSED | Quotation on final BI | Lock against further edits; record `locked_quote_id` |
| `POST` | `/internal/v1/assessments/{suitabilityId}/override` | DEFERRED (`OPEN-D2`, GAP-007) | — | Compliance-owned override path |

---

## 2. Endpoints (PROPOSED shapes)

### 2.1 `POST /internal/v1/assessments`

Headers: `X-Actor-Id`, `Idempotency-Key` (server-derived: `journey:{journeyId}:suitability`).

```json
{ "leadId": "01JQ…", "journeyId": "01JQ…", "customerId": "CUST-0433", "lob": "LIFE", "productClass": "TERM" }
```

Validation: all ids present; `lob = LIFE`; `productClass` ∈ `TERM` | `SAVINGS` | `ULIP` (taken from the lead, never from the client);
actor must be the lead's accountable SP or an allowed process-further actor (`INV-ACT-01`,
`D-019`) — PDP `suitability.perform`; lead not on exception hold.

Behind the scenes: resolve **the question set for this product class** and its version from configuration (fail closed if
unresolvable → `422 CONFIGURATION_UNRESOLVABLE`); one current assessment per lead (partial unique on
`lead_id` for non-`SUPERSEDED`/`ABANDONED`) — an existing one is returned (resume) rather than a
second created; set `edit_lock_actor_id` / `edit_lock_until`; `INSERT suitability` +
`idempotency_record` in one transaction.

Response `201`:

```json
{ "suitabilityId": "01JR…", "state": "IN_PROGRESS", "productClass": "TERM", "questionnaireVersion": "LIFE-TERM-PROTECTION-v1.0",
  "currentAnswerSetVersion": 0, "lastScreenId": null, "editLockUntil": "2026-10-21T10:30:00Z" }
```

Errors: `403 SP_CERTIFICATION_REQUIRED`, `409 CONFLICT` (another editor holds the lock), `409
CONFLICT` (exception hold), `422 CONFIGURATION_UNRESOLVABLE`.

### 2.2 `PUT /internal/v1/assessments/{suitabilityId}/answers`

Term (protection set — no horizon, no risk attitude):

```json
{ "screenId": "TERM-1", "answers": { "SQ-01": 1500000, "SQ-02": 2, "SQ-03": 500000, "SQ-04": 3000000,
  "SQ-08": 25000, "SQ-09": false, "SQ-10": "SALARIED", "SQ-11": "NO", "SQ-12": false } }
```

ULIP (adds objective, horizon, risk attitude):

```json
{ "screenId": "ULIP-2", "answers": { "SQ-01": 1500000, "SQ-05": "WEALTH_GROWTH", "SQ-06": "Y10_20",
  "SQ-07": "MODERATE", "SQ-08": 100000, "SQ-09": false, "SQ-10": "SALARIED", "SQ-11": "NO", "SQ-12": false } }
```

An answer outside the class's set is rejected (`422 VALIDATION_ERROR`); a missing one is reported
by `evaluate` as `422 SUITABILITY_INCOMPLETE`.

Behind the scenes: caller must hold the edit lock; validate against the questionnaire version;
encrypt the whole answer document (⚑ RESTRICTED); `INSERT suitability_answer_set` (version + 1);
if the assessment was `COMPLETED`, mark the current mapping run `INVALID`, set `IN_PROGRESS`, clear
`selected_product_code` (BRD §14.2). Not allowed after `LOCKED` → `409 ASSESSMENT_IMMUTABLE`.
Response `200 { suitabilityId, state, currentAnswerSetVersion, lastScreenId }`. **Answers are never
returned in plaintext by any read endpoint.**

### 2.3 `POST /internal/v1/assessments/{suitabilityId}/evaluate`

Behind the scenes (`S-07`, 3 s):

1. Load latest answer set; decrypt in process.
2. Resolve the matrix for `(lob, productClass)` from configuration — fail closed. Every input of
   that class's set must be present (`SUIT-R01a`).
3. Derive `proposer_age_years` (age, not DOB) and the non-PII mapping inputs.
4. Ask product-catalogue for eligible products in the matrix (Group A insurer, R0 classes).
5. `INSERT suitability_mapping_run` (`run_state = CURRENT`, `mapping_result` `PRODUCTS` | `NONE`,
   `recommended_products[]`); previous current run → `SUPERSEDED`.
6. Header → `COMPLETED` + `outcome = ELIGIBLE` (or `NO_PRODUCT_FOUND` / `NOT_ELIGIBLE`),
   `valid_until` from configuration.
7. Outbox `SuitabilityCompleted {suitabilityId, customerId, lob, outcome, validUntil}` → Journey,
   Audit. All in one transaction.

Response `200`:

```json
{ "suitabilityId": "01JR…", "state": "COMPLETED", "outcome": "ELIGIBLE", "mappingRuleVersion": "ULIP-MAP-2026-10",
  "recommendedProducts": ["INS01-ULIP-GROWTH", "INS01-ULIP-SECURE"], "validUntil": "2026-11-20T00:00:00Z" }
```

### 2.4 `GET /internal/v1/assessments/{suitabilityId}/validity` — the C1 read (S-08)

Query: `customerId`, `lob`. Returns `200 { "usable": true|false, "reason": "…", "validUntil": "…" }`.
`usable` only when **same customer and lob**, state `COMPLETED` (or `OVERRIDDEN` if Compliance ever
allows it), `outcome = ELIGIBLE`, and `now < valid_until`.
**Caller contract:** 500 ms, no retry; timeout, error, `404` or `usable: false` ⇒ Quotation refuses
with `403 SUITABILITY_REQUIRED` and records the quote as `REJECTED`. **A cache miss is a refusal,
never an allow.**

### 2.5 `POST /internal/v1/assessments/{suitabilityId}/lock`

Body `{ "quoteId": "01JS…" }`. Sets `bi_locked_at`, `locked_quote_id`, state `LOCKED`
(Suitability BRD §15). Idempotent for the same quote.

---

## 3. Data — schema `suitability`

| Table | Content |
|---|---|
| `suitability` | Header: ids, `lob`, `product_class`, `questionnaire_version`, `mapping_rule_version`, `state`, `outcome`, `current_answer_set_version`, `current_mapping_run_id`, `selected_product_code`, `last_screen_id`, edit lock, `bi_locked_at`, `locked_quote_id`, override columns, `evidence_document_ref`, `config_version_ref`, `version` |
| `suitability_answer_set` | INSERT-only; `answers_enc` (AES), `answer_set_version`, `last_screen_id` |
| `suitability_mapping_run` | INSERT-only; age, premium inputs, `mapping_result`, `recommended_products[]`, `run_state` |
| `idempotency_record`, `outbox_event` | Platform pattern |

DDL [`06-suitability.sql`](../../data-architecture/schemas/06-suitability.sql) · ER
[03 Lead and Suitability e2e §5](../../data-architecture/03-lead-suitability-e2e.md#5-suitability--table-catalogue).

## 4. State model

`IN_PROGRESS` → `COMPLETED` (mapping `PRODUCTS`) | `NO_PRODUCT_FOUND` → `LOCKED` (final BI).
Domain extras: `SUPERSEDED`, `EXPIRED`, `ABANDONED`, `OVERRIDDEN`
([domain model §4.3](../../ws3-platform/01-domain-model-and-invariants.md)).

## 5. Sources

- [R0 HLD §5.3](../../../architecture/R0-HLD.md#53-internal-service-apis-cluster-private) · [Domain model §4.3, §6.1](../../ws3-platform/01-domain-model-and-invariants.md) · [03 Lead + Suitability e2e](../../data-architecture/03-lead-suitability-e2e.md) · `services/suitability-service/README.md`
