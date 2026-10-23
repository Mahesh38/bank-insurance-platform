# Domain services → Integration Hub → provider adapters (IF-1, seams S-09 … S-16)

[← API reference home](../README.md)

| | |
|---|---|
| **From → to** | Quotation, Proposal, Payment (adapter capabilities only), Policy → `integration-hub-service` → `1sb-integration-service` **or** `direct-insurer-adapter-service` |
| **Style** | Quote and proposal: **async-poll**. Status, masters, schemas, eligibility: sync. Issuance confirm: sync + scheduled re-check |
| **Language** | Bank-canonical on both sides of the Hub; provider vocabulary only inside the adapter |
| **Idempotency** | Server-derived from the caller's aggregate (`quote:{quoteId}:submit`), forwarded unchanged to the adapter, which enforces it (`IdempotencyFilter`) |
| **Code status** | Adapter side **IMPLEMENTED**; Hub **SKELETON** (contract PROPOSED) |

## 1. Which operation goes where

| Caller | Operation | Hub → 1SB adapter | 1SB adapter → 1SB (via Apigee) | Style |
|---|---|---|---|---|
| Quotation | Quote | `POST /v1/quotes` | `POST /insurance/lifeterm/v1/quote` (or `lifesave`) | async; adapter polls `…/quote/poll/{reqId}` |
| Quotation | Quote result | `GET /v1/quotes/{jobId}` | (served from `bank_persistence`) | read |
| Quotation / Catalogue | Eligibility criteria | `GET`/`POST /v1/quotes/criteria` | gate-criteria form APIs | sync |
| Quotation | ULIP funds | `POST /v1/ulip/funds/list` · `/performance` | `/insurance/lifesave/v1/fund/*` | sync |
| Any (bank masters feed) | Master lookup | `POST /v1/master-data/lookup` | `/insurance/life{term,save}/v1/master/lookup` | sync, cached |
| Catalogue / Quotation | Product content | `GET /v1/products/ui-data` | `/insurance/lifeterm/v1/master/getproductuidata` | sync |
| Proposal | Form schema | `GET /v1/proposals/schema` | proposal form GET | sync, cached |
| Proposal | Submit | `POST /v1/proposals` | `POST /insurance/lifeterm/v1/proposal` | async; **no retry on submit** |
| Proposal | Submit result | `GET /v1/proposals/{jobId}` | (from `bank_persistence`) | read |
| Proposal / Policy | Application status | `GET /v1/status/{applicationNumber}` | `POST /LifeTerm/prostat/` | sync, scheduled |

## 2. The dispatch algorithm (Hub)

```text
hub_dispatch(canonical_request, principal):
 1  reject any attribution field in the body             → 422 ATTRIBUTION_NOT_CALLER_SUPPLIED
 2  provider  := routing_policy(lob, productClass, insurerCode)   (config, S-21, fail closed)
 3  distributorId := secrets/config(lob, insurer)         ← never from caller
    agentId       := principal.agentId                    ← mandatory on proposal submit
    spLicenceId   := principal.certification.licenceId
    unresolvable  → 422 CONFIGURATION_UNRESOLVABLE
 4  per-provider bulkhead / breaker
 5  call that adapter's bank-facing API with the forwarded Idempotency-Key
 6  translate adapter result → canonical (PARTIAL is success; FAILED/TIMEOUT mapped to codes)
```

## 3. How the three ids relate

```text
quotation-working QUOTE#{quoteId}  (bank, ULID, DynamoDB) ← the RM's handle
        └─ provider_job_id ──────► bank_persistence.integration_job.job_id   (adapter, UUID)
                                         └─ external_req_id ──► 1SB reqId   (provider — never returned upward)
        journey_id is stored on both the quote and the adapter job for tracing
```

## 4. Failures

| Situation | Behaviour |
|---|---|
| Submit times out (no response) | **Do not resubmit.** The adapter job exists (`PENDING`/`RUNNING`); poll it to learn the truth |
| Provider returns per-insurer errors | `PARTIAL` → domain `PARTIALLY_QUOTED`; RM sees offers **and** failures |
| Poll budget exhausted | Adapter `TIMEOUT` → domain `TIMED_OUT`; journey `ABANDONED` with re-quote |
| No route for a product (F-09) | Fail fast; journey stays; Product decides Group B redirect |
| Provider breaker open | `503 UPSTREAM_UNAVAILABLE` for that provider only; others unaffected |

## 5. Sources

- [Integration Hub](../services/integration-hub-service.md) · [1SB adapter](../services/1sb-integration-service.md) · [External call catalogue §2](../../../journey-execution/05-EXTERNAL-CALL-CATALOGUE.md) · [WS-3 registration IF-1](../../ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md)
