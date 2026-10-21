# Scenario 07 — Journey starts; need analysis and suitability (C1)

[← API reference home](../README.md) · Prev: [06](./06-lead-onboarding-and-assignment.md) · Next: [08](./08-consent-otp.md)

**Status: CONTRACTED / PROPOSED** — journey-orchestration and suitability-service are skeletons.
Paths follow HLD §5.1/§5.3; bodies are the PROPOSED shapes on the
[suitability page](../services/suitability-service.md).

> **Per-class suitability (`D-022`):** the question set and matrix follow the lead's product class.
> A Term journey asks the protection set — no investment horizon, no risk attitude. C1 applies to all
> classes. See the [suitability page](../services/suitability-service.md).

## 1. Sequence

```mermaid
sequenceDiagram
    autonumber
    participant App as NIP-APP
    participant BFF as BFF
    participant PDP as identity-authorization
    participant JO as journey-orchestration
    participant CS as customer-service
    participant SU as suitability-service
    participant CFG as administration-config
    participant CAT as product-catalogue
    App->>BFF: start journey (after assignment; lead ASSIGNED, exception clear)
    BFF->>PDP: decide suitability.perform (SP certified for LIFE, now)
    BFF->>JO: POST /internal/v1/journeys {leadId, customerId, lob}
    JO->>CS: snapshot (S-04, 2 s, 1 retry)
    JO->>JO: INSERT journey INITIATED + transition + outbox (one txn)
    JO-->>BFF: {journeyId}
    App->>BFF: POST /api/v1/journeys/{journeyId}/need-analysis {screen answers}
    BFF->>JO: need-analysis
    JO->>SU: POST /internal/v1/assessments (start / resume; edit lock)
    SU->>CFG: question set for (LIFE, productClass) + version (S-21, fail closed)
    JO->>SU: PUT …/answers (new encrypted answer-set version per screen)
    JO->>JO: stage NEED_ANALYSIS · ref suitabilityId
    App->>BFF: POST /api/v1/journeys/{journeyId}/suitability (evaluate)
    BFF->>JO: suitability
    JO->>SU: POST …/{suitabilityId}/evaluate (S-07, 3 s)
    SU->>CFG: mapping rule pack
    SU->>CAT: eligible products in the R0 matrix
    SU->>SU: INSERT mapping_run · COMPLETED + ELIGIBLE (or NO_PRODUCT_FOUND) · outbox SuitabilityCompleted
    SU-->>JO: {outcome, recommendedProducts, validUntil}
    JO->>JO: stage SUITABILITY_COMPLETE (only on ELIGIBLE)
    JO-->>BFF: stage + recommended products
    BFF-->>App: 200
```

## 2. Rules the RM will hit

| Rule | Effect |
|---|---|
| Only the accountable SP / allowed process-further actor may perform | `403 SP_CERTIFICATION_REQUIRED` (PDP + service re-check) |
| One current assessment per lead; one active editor | Second editor → `409 CONFLICT` |
| Answers are RESTRICTED | Stored encrypted per version; never returned in plaintext |
| Editing a completed assessment | New answer-set version; mapping invalidated; back to `IN_PROGRESS` |
| `NO_PRODUCT_FOUND` / `NOT_ELIGIBLE` | Journey cannot reach `SUITABILITY_COMPLETE`; **no quote is possible** |
| Rules unresolvable | `422 CONFIGURATION_UNRESOLVABLE` — never a hard-coded fallback |
| Override | Not in R0 (Compliance decision `OPEN-D2`) |
| Final BI | Quotation locks the assessment (`LOCKED`) |

## 3. Data written

| Store | Rows |
|---|---|
| `journey` | `journey` (`INITIATED` → `NEED_ANALYSIS` → `SUITABILITY_COMPLETE`), `journey_ref (suitabilityId)`, transitions, outbox |
| `customer` | `customer_snapshot` for this journey |
| `suitability` | `suitability` header, `suitability_answer_set` (encrypted, per save), `suitability_mapping_run`, outbox `SuitabilityCompleted`, idempotency |
| Audit | `SuitabilityCompleted` evidence via outbox → MSK → audit |

## 4. Pages

[Suitability service](../services/suitability-service.md) · [Journey service](../services/journey-orchestration-service.md) · [Journey pattern](../interactions/journey-orchestration-pattern.md) · [Configuration resolution](../interactions/configuration-resolution.md)
