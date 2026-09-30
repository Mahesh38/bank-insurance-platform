# Critical path and enabler map (CR-010 K-C2)

**Owner:** Kalpana / Delivery, with Amit + Shivanshi  
**signature_status:** `AI-DRAFTED — sizing is Delivery's; this is the acyclic map, not FRI-001 funding`

Source list: [S04 evidence §4.5](../../application-lifecycle-bible/evidence/S04-product-definition-evidence.md#45-technical-enablers-made-visible--closes-d4-s04-vt-06) (19 enablers).

## Effort bands (Delivery working sizes)

XS < 1 engineer-day · S 1–3 d · M 3–10 d · L > 10 d. These are planning sizes, not commitments.

| # | Enabler | Size | Predecessors | Gate |
|---|---|---|---|---|
| 1 | Application CI every module | S | — | S08-G1 **MET** |
| 2 | Branch protection | S | 1 | S08-G2 OPEN (repo-admin) |
| 3 | JaCoCo / QA-001 | M | 1 | S08-G3 **MET** |
| 4 | ArchUnit + static analysis | S | 1 | S08-G4 **MET** |
| 5 | Secret/SAST/SCA/image/SBOM | M | 1 | S08-G5 **MET** |
| 6 | Testcontainers PostgreSQL | M | 1 | S08-G6 **MET** |
| 7 | WireMock 1SB harness | M | 6 | S08-G6 **MET** |
| 8 | Contract tests integration↔persistence | M | 6, 7 | S08-G6 **MET** |
| 9 | E2E harness (WS-1 4.1) | L | 7, 8, **1SB sandbox** | BLOCKED external |
| 10 | PII-in-logs test | S | 1 | S08-G7 **MET** |
| 11 | Performance harness (WS-1 4.6) | M | 9 | BLOCKED on 4.1 |
| 12 | Terraform ap-south-1 | L | 2 | S09 |
| 13 | Three environments | L | 12 | S09 |
| 14 | AWS Secrets Manager real | M | 13 | S09 (TD-006) |
| 15 | Observability | M | 13 | S09 |
| 16 | Deploy pipeline + rollback drill | M | 13 | S09 |
| 17 | S3 Object Lock 7-year | M | 13 | S09 |
| 18 | Flutter RM R0 screens | L | S08 PASS, GAP-006/007 | S11 |
| 19 | Design system from S05 | M | — (parallel) | S11 |

## Critical path (no cycles)

`1 → 6 → 7 → 8 → 9 → 11` is the WS-1 evidence spine (blocked at 9 on 1SB).  
`1 → 2 → 12 → 13 → {14,15,16,17}` is the S09 spine (blocked on DEP-20260824-cst at S09 **entry**).  
`18` cannot start until S08 PASS **and** GAP-006/007 (C5). No edge from 18 back to 1–17.

FRI-001 funding (K-C1) remains blocked on GAP-010.
