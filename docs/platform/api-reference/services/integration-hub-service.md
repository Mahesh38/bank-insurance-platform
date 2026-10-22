# integration-hub-service — Integration Hub (#14)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | The **only** door from platform domain services to insurance providers. Accepts a **bank-canonical** provider request, decides **which provider adapter** serves it (1SB today; a direct insurer or a bank aggregator later), injects attribution server-side, applies per-provider resilience, and returns bank-language results. Its whole purpose is that adding or replacing a provider is **a new adapter behind the routing policy — never a change to Quotation, Proposal, Payment or Policy** (`IF-1`, `S07-VT-08`, `TI-19`) |
| **Module** | `services/integration-hub-service` · port 8100 · `service-id: integration-hub` · layer L5 · INTERNAL |
| **Contract** | [`openapi.yaml`](../../contracts/integration-hub-service/openapi.yaml) · [LLD](../../contracts/integration-hub-service/LLD.md) — authoritative per [ADR-027](../../architecture-review/ADR-027-openapi-contract-authority.md); AI-DRAFTED; routing `HUB.ROUTING`, canonical ↔ adapter mapping and nested timeouts are defined there and supersede the PROPOSED bodies below |
| **Base path** | `/internal/v1/provider-requests` (HLD §5.3) |
| **Callers** | Quotation, Proposal, Payment (adapter capabilities only), Policy — **and nothing else**. The BFF and Flutter never call it; for reference data the BFF goes through the owning domain service |
| **Calls out** | `1sb-integration-service` `/v1/*` · `direct-insurer-adapter-service` (when admitted) · administration-config (routing policy, distributor/agent resolution, S-21) |
| **Owns data** | **PROPOSED (2026-10-21):** Aurora schema `hub` — `provider_request` (idempotency key UNIQUE, provider, adapter job id, routing version) + `outbox_event`; DDL addition and owner yes pending ([LLD §7](../../contracts/integration-hub-service/LLD.md#7-where-idempotency-and-correlation-live--decision-proposed)). Routing is rule set `HUB.ROUTING` (ADR-024, hit policy `FIRST`). Provider job state stays in `bank_persistence` (adapter-owned) |
| **Code status** | **SKELETON** (`GET /internal/v1/integration-hub/info`). Contract **CONTRACTED** at the level of `IF-1` + HLD §5.3; request/response bodies below are **PROPOSED** from the IF-1 rules, the adapter's implemented contract and [replaceable middleware](../../../1sb-insurance-integration/architecture/replaceable-middleware.md) |

---

## 1. Why the Hub exists — the hop rule

```text
NIP-APP → BFF → domain service (Quotation / Proposal / Payment / Policy)
                       → Integration Hub  ──routing policy──►  1SB adapter      → Apigee → 1SB
                                                         └──►  direct-insurer adapter → Apigee → insurer API
                                                         └──►  (future) bank aggregator adapter
```

| Rule | Consequence |
|---|---|
| No platform service calls an adapter directly | All provider traffic is observable, attributable and throttled in one place |
| Provider vocabulary terminates inside the adapter (`INV-ACL-01`) | Hub and above speak `insurerCode`, `quoteId`, `mode`, `bankStatus` — never `manufacturerId`, `reqId`, `applicationStatus` |
| `distributorId` and agent identity injected here, at the last moment before dispatch (`INV-DIS-01`) | A caller-supplied value is **rejected**, not ignored |
| The Hub's id is never a provider id; the adapter's `jobId` is never a domain id | Domain keeps `quoteId`/`proposalId`; Hub correlates to adapter `jobId`; adapter correlates to 1SB `reqId` |
| One failing provider must not starve the others | Per-provider bulkhead, breaker and connection budget |

---

## 2. API index

| Method | Path | Status | Caller | Purpose |
|---|---|---|---|---|
| `POST` | `/internal/v1/provider-requests` | CONTRACTED · body PROPOSED | Quotation, Proposal, Payment, Policy | Submit a canonical provider operation (async ones return `202`) |
| `GET` | `/internal/v1/provider-requests/{providerRequestId}` | CONTRACTED · body PROPOSED | Same | Status + canonical result |
| `POST` | `/internal/v1/provider-queries` | PROPOSED | Quotation, Proposal, Catalogue | Synchronous reads: masters, product UI data, eligibility criteria, proposal form schema, ULIP funds, application status |

### 2.1 Canonical operations and how they route today

| Canonical operation | Style | Seam | 1SB adapter call | Direct-insurer adapter |
|---|---|---|---|---|
| `QUOTE` | async-poll | S-09 / S-10 / S-11 | `POST /v1/quotes` → poll `GET /v1/quotes/{jobId}` | per insurer API |
| `ELIGIBILITY_CRITERIA_GET` / `_SUBMIT` | sync | S-10 | `GET` / `POST /v1/quotes/criteria` | — |
| `PROPOSAL_FORM_SCHEMA` | sync (cached) | S-10 | `GET /v1/proposals/schema` | — |
| `PROPOSAL_SUBMIT` | async-poll, **no retry on submit** | S-12 | `POST /v1/proposals` → poll `GET /v1/proposals/{jobId}` | — |
| `APPLICATION_STATUS` | sync + scheduled | S-12 / S-16 | `GET /v1/status/{applicationNumber}` | — |
| `UW_REQUIREMENTS` | sync | S-10 | `GET /v1/requirements/{applicationNumber}` (contracted) | — |
| `MASTER_LOOKUP` | sync (cached) | S-10 | `POST /v1/master-data/lookup` | — |
| `PRODUCT_CONTENT` | sync | S-10 | `GET /v1/products/ui-data` | — |
| `ULIP_FUNDS` | sync | S-10 | `POST /v1/ulip/funds/list` · `/performance` | — |
| `ISSUANCE_CONFIRM` | sync + re-check | S-16 | status call (policy number) | — |

The money path (premium collection) is **AU Bank PG via payment-service**, not a provider
operation. The 1SB payment-URL capability is not routed in R0.

---

## 3. Endpoints (PROPOSED shapes)

### 3.1 `POST /internal/v1/provider-requests`

Headers: `Idempotency-Key` (server-derived by the caller, e.g. `quote:{quoteId}:submit`),
`X-Correlation-Id`, `X-Journey-Id`, `X-Actor-Id` (the principal — used to resolve agent / SP
licence, never trusted as attribution itself).

```json
{
  "operation": "QUOTE",
  "lob": "LIFE",
  "productClass": "TERM",
  "callerRef": { "context": "quotation", "aggregateId": "01JT…" },
  "journeyId": "01JQ…",
  "routing": { "insurerCodes": ["INS01", "INS02"], "mode": "MULTI" },
  "body": {
    "category": "SUM_ASSURED", "requestedCover": 10000000,
    "members": [ { "role": "LIFE_ASSURED", "sequence": 1, "dob": "1990-04-12", "gender": "MALE",
                   "tobacco": false, "annualIncome": 1500000, "pincode": "400001" } ],
    "preferences": { "paymentFrequency": "YEARLY" }
  }
}
```

Validation: `operation` known; `lob` present; `body` matches the canonical schema for the
operation; **reject** any `distributorId`, `agentId`, `spLicenceId`, `distribution.*` in the body
(`422 ATTRIBUTION_NOT_CALLER_SUPPLIED`, security event).

**Behind the scenes**

```text
1  Idempotency: same key + same body → return the stored providerRequestId/state
2  Routing policy (S-21, 300 ms, fail closed): resolve provider for (lob, productClass, insurerCode)
     from administration-config, e.g. LIFE/TERM/INS01 → ONE_SB
     no route → 422 CONFIGURATION_UNRESOLVABLE (F-09: journey stays at its stage; Product decides
     a Group-B redirect)
3  Attribution (hub_dispatch, external-call catalogue §2.1):
     distributorId := secrets/config(lob, insurer)        ← never from the caller
     agentId       := principal's mapped agent code       ← mandatory on PROPOSAL_SUBMIT
     spLicenceId   := principal certification licence id
     any unresolvable → 422 CONFIGURATION_UNRESOLVABLE    ← never a default
4  Per-provider bulkhead + breaker (Resilience4j-style); open breaker → 503 UPSTREAM_UNAVAILABLE
5  Translate canonical → that adapter's bank-facing contract (still bank language) and call it:
     QUOTE → POST 1sb /v1/quotes {lob: TERM, mode, members, distribution{agentId}, journeyId}
             Idempotency-Key forwarded
6  Record providerRequestId ↔ (provider, adapter jobId) — in memory/config cache only; durable
   correlation lives on the adapter job (bank_persistence.integration_job.journey_id) and on the
   caller's aggregate (provider_job_id)
7  202 { providerRequestId, state: IN_PROGRESS, provider: "ONE_SB" }
```

> **Superseded 2026-10-21:** the contract now proposes `hub.provider_request` as the durable link (LLD §7). Previous text: the Hub owns no table, so the durable link between `quoteId` and the adapter `jobId` is held by
> the **caller** (the quote's DynamoDB item, attribute `providerJobId`) and by the **adapter job** (`journey_id`). If
> the Hub ever needs its own correlation store, that is a new schema and an ADR — not a silent
> Dynamo table (store-ownership §2.3).

### 3.2 `GET /internal/v1/provider-requests/{providerRequestId}`

The Hub polls (or proxies a poll to) the adapter and maps results to canonical form:

| Adapter job status | Canonical state | Notes |
|---|---|---|
| `PENDING`, `RUNNING` | `IN_PROGRESS` | |
| `COMPLETED` | `COMPLETED` | offers / application number |
| `PARTIAL` | `PARTIALLY_COMPLETED` | **success** — offers plus per-insurer errors |
| `FAILED` | `FAILED` | `failureReason` mapped to a registry code |
| `TIMEOUT` | `TIMED_OUT` | Caller decides re-quote / ops task |

```json
{ "providerRequestId": "01JX…", "operation": "QUOTE", "state": "PARTIALLY_COMPLETED", "provider": "ONE_SB",
  "result": { "offers": [ { "insurerCode": "INS01", "productCode": "TERM-PLUS", "premiumAmount": 12300.00,
              "premiumFrequency": "YEARLY", "sumAssured": 10000000, "outOfBound": false } ],
              "partialErrors": [ { "insurerCode": "INS02", "code": "UPSTREAM_BUSINESS_ERROR" } ] } }
```

### 3.3 `POST /internal/v1/provider-queries` (sync reads)

`{ "operation": "MASTER_LOOKUP", "lob": "LIFE", "productClass": "TERM", "body": { "entities": ["OCCUPATION"] } }`
→ `200 { "result": { "OCCUPATION": [ {"code": "SALARIED", "label": "Salaried"} ] } }` —
**bank codes**, mapped from provider codes inside the Hub/adapter. 1SB master lookup is a provider
**feed** for the bank's own masters, never the UI's dropdown contract (standing constraint).

---

## 4. Synchronisation with providers (who polls what)

| Concern | Owner | Mechanism |
|---|---|---|
| 1SB async job → final result | **1SB adapter** | `AsyncJobPoller` (1 s × 2 → 30 s, 20 attempts) writes offers/status to `bank_persistence` |
| Adapter job → canonical state | **Hub** | `GET` adapter job on caller poll (or a scheduled sweep when built) |
| Canonical state → domain aggregate | **Quotation / Proposal** | Poll the Hub (S-11/S-12) or consume a Hub completion event |
| Insurer UW / issuance status after submit | **Proposal / Policy** via Hub `APPLICATION_STATUS` | Scheduled re-check (S-16) until terminal |
| Inbound provider webhook | — | **None in R0.** Providers are polled; no public webhook route (external-call catalogue §1) |

Full sequence: [Provider sync](../scenarios/14-provider-status-sync.md).

## 5. Events

R0: none required — the Hub is synchronous to its callers and stateless. Domain events
(`QuoteCompleted`, `ProposalSubmitted`, …) are raised by the **owning** domain service from its own
outbox, not by the Hub. Provider-call audit is emitted by the adapter (`QUOTE_CREATED`, …) into
`bank_persistence.audit_event`.

## 6. Sources

- [WS-3 registration — IF-1](../../ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md) · [R0 HLD Boundary 6, §5.3](../../../architecture/R0-HLD.md#boundary-6--aggregation-and-provider-connectivity) · [Replaceable middleware](../../../1sb-insurance-integration/architecture/replaceable-middleware.md) · [External call catalogue §2](../../../journey-execution/05-EXTERNAL-CALL-CATALOGUE.md) · [Canonical model](../../../1sb-insurance-integration/canonical-model/README.md)
