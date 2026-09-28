# Checkpoint: <capability path> → <dev | bank-UAT | prod>

Evaluated on <date> by <who/agent>. A checkpoint is a query over evidence (docs/06-assurance.md §5).
Result: **READY** · **NOT READY — missing items below**

| Evidence | Required for | Status | Link |
|---|---|---|---|
| Main green incl. guardrail checks | all | | |
| Contracts published for bank-touched APIs | uat, prod | | |
| Scans clean at HIGH+ | uat, prod | | |
| Open decisions with blocks_promotion_to ≤ this env resolved | uat, prod | | |
| External dependencies satisfied or declared-stubbed | uat, prod | | |
| R2 changes approved by control owners (author ≠ approver) | uat, prod | | |
| SLOs, dashboards, alerts, runbooks for the path | prod | | |
| DR / restore exercised for the path | prod | | |
| Compliance evidence for G2/G5/G6/G10 on the path | prod | | |
| Performance at expected load | prod | | |

**Human sign-off** (prod: product, compliance, security, reliability — never an AI):
