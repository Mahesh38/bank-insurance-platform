# direct-insurer-adapter-service — Direct insurer adapter (seam)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | The second provider adapter: a **direct** connection to one insurer's own APIs, behind the **same** Integration Hub canonical contract as the 1SB adapter. Its existence proves the Hub abstraction (`replaceable middleware` Phase B — "dual-run ready"). It translates canonical requests to that insurer's wire protocol and back, exactly as the 1SB adapter does |
| **Module** | `services/direct-insurer-adapter-service` · port 8105 · `service-id: direct-insurer` · layer L5 · INTERNAL |
| **Contract** | [`openapi.yaml`](../../contracts/direct-insurer-adapter-service/openapi.yaml) · [LLD](../../contracts/direct-insurer-adapter-service/LLD.md) — authoritative per [ADR-027](../../architecture-review/ADR-027-openapi-contract-authority.md); **DEFERRED** — fixes the Hub → adapter port shape only |
| **Callers** | **Integration Hub only** — never a domain service, never the BFF |
| **Owns data** | None. When admitted: HTTP to `bank-persistence-service` for jobs and encrypted raw payloads (same pattern as 1SB); S3 for raw bytes |
| **Code status** | **SKELETON, DEFERRED** — direct insurer APIs are R1+; multi-aggregator routing is out of scope "until evidence of a second aggregator commitment" (BOOT) |

## 1. When the Hub routes here

The Hub's routing policy (rule set `HUB.ROUTING`, ADR-024, hit policy `FIRST`) maps `(lob, productClass, insurerCode)` to
a provider. A row `LIFE / TERM / INS07 → DIRECT_INS07` sends that insurer's traffic here while
every other insurer stays on 1SB. Quotation, Proposal, Payment and Policy do not change — that is
the test of the design (`S07-VT-08`).

## 2. Contract it must implement

The same bank-facing operations the 1SB adapter exposes (see
[1SB adapter §1](./1sb-integration-service.md#1-api-index)) for the operations the insurer supports:
async quote with job id and poll, proposal schema / submit / poll, application status. Rules that
carry over unchanged: no automatic retry on submit; `PARTIAL` is success; `distributorId` injected
from secrets, never from a caller; provider types confined to `adapter.<insurer>.*` (ArchUnit);
every request/response stored encrypted; statuses normalised to `BankApplicationStatus`.

Insurer callbacks (webhooks) are **R1** ("callback gateway"); R0 polls.

## 3. Sources

- [Replaceable middleware](../../../1sb-insurance-integration/architecture/replaceable-middleware.md) · [Integration Hub](./integration-hub-service.md) · [R0 HLD Boundary 6](../../../architecture/R0-HLD.md#boundary-6--aggregation-and-provider-connectivity)
