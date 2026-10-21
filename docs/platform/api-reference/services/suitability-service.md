# suitability-service — Need analysis and suitability (#7)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | Holds the **C1 hard gate**: *no quote without a valid, unexpired, `ELIGIBLE` suitability assessment* (`INV-QUO-01`). Captures the need-analysis questionnaire (save / resume, single active editor), runs the product **mapping** against versioned rules, records the recommended product set, and locks the assessment at final BI. The framework is shared; the **rules are per LOB and come from configuration** (`#19`), never from `if` statements |
| **Module** | `services/suitability-service` · port 8093 · `service-id: suitability` · layer L5 · INTERNAL |
| **Base path** | `/internal/v1/assessments` (HLD §5.3) |
| **Callers** | Journey (S-07 evaluate, 3 s) via the BFF · Quotation (S-08 gate read, **500 ms, no retry, fail closed**) |
| **Calls out** | administration-config (questionnaire + mapping rule pack, S-21) · product-catalogue (eligible products, mapping output codes) |
| **Owns data** | Aurora schema **`suitability`** · role `app_suitability` |
| **Code status** | **SKELETON** (`GET /internal/v1/suitability/info`). All business endpoints below are CONTRACTED (paths, HLD §5.3) with **PROPOSED** bodies drafted from the Suitability BRD, the domain model and the DDL |

> **Open item (K-9).** The Suitability BRD says Term leads **skip** the Savings/ULIP questionnaire
> (`BR-SUIT-002`, "a Term lead has no row"), while C1 / `INV-QUO-01` require an `ELIGIBLE`
> assessment before **any** quote. How Term satisfies C1 (a lighter Term need-analysis assessment, or
> a Compliance ruling) is undecided — owners Rajal (Product) and Shailja (Compliance). Do not build
> either answer without that decision. A second open item, `OPEN-SUI-IMMUTABLE`, is whether a saved
> edit of a completed assessment keeps the same id (BRD) or mints a new one (`INV-SUI-01`).

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
{ "leadId": "01JQ…", "journeyId": "01JQ…", "customerId": "CUST-0433", "lob": "LIFE", "productClass": "ULIP" }
```

Validation: all ids present; `lob = LIFE`; `productClass` ∈ `SAVINGS` | `ULIP` (and Term per K-9);
actor must be the lead's accountable SP or an allowed process-further actor (`INV-ACT-01`,
`D-019`) — PDP `suitability.perform`; lead not on exception hold.

Behind the scenes: resolve the questionnaire version from configuration (fail closed if
unresolvable → `422 CONFIGURATION_UNRESOLVABLE`); one current assessment per lead (partial unique on
`lead_id` for non-`SUPERSEDED`/`ABANDONED`) — an existing one is returned (resume) rather than a
second created; set `edit_lock_actor_id` / `edit_lock_until`; `INSERT suitability` +
`idempotency_record` in one transaction.

Response `201`:

```json
{ "suitabilityId": "01JR…", "state": "IN_PROGRESS", "questionnaireVersion": "LIFE-ULIP-v3",
  "currentAnswerSetVersion": 0, "lastScreenId": null, "editLockUntil": "2026-10-21T10:30:00Z" }
```

Errors: `403 SP_CERTIFICATION_REQUIRED`, `409 CONFLICT` (another editor holds the lock), `409
CONFLICT` (exception hold), `422 CONFIGURATION_UNRESOLVABLE`.

### 2.2 `PUT /internal/v1/assessments/{suitabilityId}/answers`

```json
{ "screenId": "S6", "answers": { "policyFor": "SELF", "lifeStage": "MARRIED_WITH_CHILDREN",
  "riskPreference": "MODERATE", "primaryGoal": "WEALTH_CREATION", "occupation": "SALARIED",
  "annualIncomeInr": 1500000, "tobacco": false, "existingLifeCover": true,
  "premiumAmountInr": 100000, "premiumFrequency": "ANNUAL", "premiumPayingTerm": 10, "policyTerm": 20 } }
```

Behind the scenes: caller must hold the edit lock; validate against the questionnaire version;
encrypt the whole answer document (⚑ RESTRICTED); `INSERT suitability_answer_set` (version + 1);
if the assessment was `COMPLETED`, mark the current mapping run `INVALID`, set `IN_PROGRESS`, clear
`selected_product_code` (BRD §14.2). Not allowed after `LOCKED` → `409 ASSESSMENT_IMMUTABLE`.
Response `200 { suitabilityId, state, currentAnswerSetVersion, lastScreenId }`. **Answers are never
returned in plaintext by any read endpoint.**

### 2.3 `POST /internal/v1/assessments/{suitabilityId}/evaluate`

Behind the scenes (`S-07`, 3 s):

1. Load latest answer set; decrypt in process.
2. Resolve the mapping rule pack (version) for `(lob, productClass)` from configuration — fail
   closed.
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
