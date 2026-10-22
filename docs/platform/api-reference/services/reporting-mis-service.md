# reporting-mis-service — Reporting and MIS (#18)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | Funnel, sold, on- vs off-platform and attribution reports — on an **isolated read path** (replica / S3 extract / Athena–Redshift read models built from events). It must never run MIS queries on the Lead or Payment writer: year-end reporting must not kill origination (`C-ISO-1`, `CR-013`) |
| **Module** | `services/reporting-mis-service` · port 8103 · `service-id: reporting` · layer L5 · INTERNAL |
| **Contract** | [`openapi.yaml`](../../contracts/reporting-mis-service/openapi.yaml) · [LLD](../../contracts/reporting-mis-service/LLD.md) — authoritative per [ADR-027](../../architecture-review/ADR-027-openapi-contract-authority.md); **NOT READY** — paths and inputs only; read-model store undecided |
| **Callers** | BFF admin / MIS routes (R0 W4) |
| **Calls out** | Its own reader store only |
| **Owns data** | Isolated reader (replica or extract). **Never** granted `app_lead_lms`, `app_payment` or any writer role |
| **Code status** | **SKELETON** (`GET /internal/v1/reporting/info`). Report APIs **PROPOSED**; read-model design is `DATA-002` W4 |

## 1. Proposed APIs

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/internal/v1/reports/funnel?from&to&branch&lob` | Lead → quote → proposal → paid → issued counts |
| `GET` | `/internal/v1/reports/sold?from&to&insurer` | Sold policies (journey `SOLD`), on- vs off-platform |
| `GET` | `/internal/v1/reports/attribution?spId&from&to` | Accountable SP attribution (`C-RET-1`) |

Permissions: `report.branch.read` / `report.insurer.read` (PDP). Partner reps see insurer-scoped
data only. Rows are fed by events (`OpportunityCreated`, `QuoteCompleted`, `PaymentReconciled`,
`JourneySold`, off-platform `Policy` ingest) — never by querying OLTP schemas directly.

## 2. Sources

- [DATA-002 CR-013 alignment](../../data-architecture/DATA-002-cr013-alignment.md) · [Store segregation §3.3](../../data-architecture/04-store-segregation-and-roadmap.md)
