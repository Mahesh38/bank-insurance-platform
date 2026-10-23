# direct-insurer-adapter-service — Low-level design (DEFERRED)

**Status:** AI-DRAFTED 2026-10-21 — **DEFERRED, out of R0**, awaiting Mahesh (architecture) review.
Direct insurer APIs are R1+; multi-provider routing is out of scope until a second provider is
committed (BOOT).

[Port shape (OpenAPI)](./openapi.yaml) · [API reference page](../../api-reference/services/direct-insurer-adapter-service.md) ·
[Integration Hub LLD](../integration-hub-service/LLD.md) ·
[1SB adapter page](../../api-reference/services/1sb-integration-service.md)

## What it is

A second provider adapter: a direct connection to one insurer's own APIs. It implements **the same
adapter port as the 1SB adapter** — the bank-facing `/v1/*` operations the Integration Hub calls — so
the Hub's callers (Quotation, Proposal, Policy) never change. That is the test of the Hub design
(`S07-VT-08`).

| | |
|---|---|
| Module | `services/direct-insurer-adapter-service` · port 8105 · `service-id: direct-insurer` · SKELETON |
| Caller | Integration Hub only |
| Selected by | A `HUB.ROUTING` rule with a lower `priority` than the aggregator rule, e.g. `insurerCode = INS07 → DIRECT_INS07` ([Hub LLD §3](../integration-hub-service/LLD.md#3-routing--rule-set-hubrouting-hit-policy-first)) — a configuration version, no code change in the Hub |
| Owns | Nothing. Jobs and encrypted raw payloads via `bank-persistence-service` (same pattern as 1SB; `integration_job.external_provider` distinguishes it) |

## The port it implements

| Operation | Path | Notes |
|---|---|---|
| Quote submit / poll | `POST /v1/quotes`, `GET /v1/quotes/{jobId}` | Async job; `PARTIAL` is success |
| Proposal submit / poll | `POST /v1/proposals`, `GET /v1/proposals/{jobId}` | **No automatic retry on submit** |
| Application status | `GET /v1/status/{applicationNumber}` | Normalised to `BankApplicationStatus` |
| Job by idempotency key | `GET /v1/jobs?idempotencyKey=` | Needed by the Hub for `SUBMISSION_UNCERTAIN`; to be added to the 1SB adapter too (Hub O-2) |

Eligibility criteria, proposal schema, masters, product content and ULIP funds are added per
insurer only when that insurer offers them; the Hub's `NO_ROUTE` covers an operation an adapter
lacks. The OpenAPI fixes the shape only — there is no further contract beyond this shared port.

## Rules carried over unchanged

`distributorId` from secrets, never a request field (`COMP-004`); provider types confined to
`adapter.<insurer>.*` (ArchUnit); every request / response stored encrypted for 7 years;
submit-path budget ≤ 20 s inside the Hub's 25 s (Hub LLD §8); insurer callbacks (webhooks) are R1 —
R0-style polling first.

## Open items

| # | Item | Owner |
|---|---|---|
| D-1 | Admission: which insurer, when (needs evidence of commitment) | Mahesh / Rajal |
| D-2 | Insurer API, Apigee exposure and credentials | Bank / insurer |
