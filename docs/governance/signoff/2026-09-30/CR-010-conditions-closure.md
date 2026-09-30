# CR-010 — Conditions Closure Report (AIGEM-ACCEPTED 2026-09-30)

**Change request:** [`CR-010-context-module-and-safe-autopilot.md`](../../change-requests/CR-010-context-module-and-safe-autopilot.md)  
**AIGEM suggestion:** APPROVE WITH CONDITIONS (Rajal / Product; 42 drafted conditions).  
**signature_status:** `AI-DRAFTED — nine human verdicts plus T4 Security/Compliance/Architecture outstanding`

This report closes **repository-owned** conditions. It is not a board approval. External, funding, and human-signature conditions stay open.

## Completed in this change

| ID | Condition | Evidence |
|---|---|---|
| SEC-C1 / E-01 | Constrain `autopilot.py --output`; refuse `..`, symlinks, state/CR trees; test it | `scripts/governance/autopilot.py` `resolve_proposal_output`; `test_autopilot.py` |
| R-C1 | Detection, not only prohibition | `GOVERNANCE ALARM` + `alarms.jsonl` on refusal; proposal writes logged |
| SEC-C2 | CODEOWNERS on state, CRs, workflows | `/CODEOWNERS` |
| OPS-C1 | Named recipient for scheduled governance failure | `governance.yml` `alarm-on-failure`; `governance-daily.yml` failure step; runbook |
| OPS-C2 | Freshness and backlog-drift runbook entries | [`GOVERNANCE-FAILURE-RUNBOOK.md`](../../runbooks/GOVERNANCE-FAILURE-RUNBOOK.md) |
| OPS-C3 / E-04 | Concurrency group, cancel-in-progress | `governance.yml` (application-ci already had it) |
| OPS-C4 | Criterion 4.5 owner = Shivanshi / SRE | `GATE-EVIDENCE.yaml` |
| Q-C3 | SUIT-R40 structural test | `SuitR40NoBypassTest` + ArchUnit name rule in `suitability-service` |
| R-C2 | Suitability-gate risk with **named human** owner | RISK-016, owner **Shailja S** |
| DB-C1 | Reconstruction columns including `retain_until` and `sequence_no` | Flyway `V3__audit_event_reconstruction_columns.sql`; design DDL updated |
| DB-C2 | INSERT/SELECT writer role documented | [`AUDIT-STRUCTURE.md`](../../controls/AUDIT-STRUCTURE.md); `91-grants.sql` already INSERT/SELECT |
| K-C2 | 19 enablers sized + mapped, acyclic critical path | [`CRITICAL-PATH.md`](../../controls/CRITICAL-PATH.md) |
| K-C3 | `services/` feature freeze published with adjudicator | [`FEATURE-FREEZE.md`](../../controls/FEATURE-FREEZE.md) |
| K-C4 | Board response clock | required-by **2026-10-14** on the verdict index |
| C7 / §2 | Automation never marks PASSED / never signs | Unchanged; tests assert `may_mark_passed: false` |
| CI evidence / gate refs | Documented | [`CI-EVIDENCE.md`](../../controls/CI-EVIDENCE.md) |

## Already true (verified, not re-done)

| ID | Note |
|---|---|
| C1 / C2 / C3 / C8 | 16-stage model, WS-3 transcribed, WS-1 supplier, 16-type routing |
| C4 | Assisted-first in CURRENT-STATE / charter (R0-SCOPE v0.4 still a Product publish) |
| C5 / C6 | S11 freeze on GAP-006/007; Phase 5 not started — still binding, still unsigned |
| Q-C1 | Policy restated; no E3/E4 MET on a definition artefact in this PR |
| Q-C2 | application-ci runs on every PR; S08-G1 already MET with run IDs in GATE-EVIDENCE |
| E-02 / E-03 | Autopilot tests on fixtures; application-ci path filter already removed |
| SEC-C5 | Render.com standing constraint already in CURRENT-STATE |
| SEC-C8 / S08-G5 | security-scanning.yml already MET |
| SEC-C9 / S08-G7 | NoPiiInEmittedLogsTest already MET |
| DB-C5 | Testcontainers / S08-G6 already MET |
| OPS-C5 / OPS-C9 / S08-G9 | p95 measured; last_verified_at populated on closed S08 rows |
| K-C5 | GATE-P4 blocked criteria already use BLOCKED |

## Partially completed

| ID | Remaining |
|---|---|
| SEC-C6 | SHA-pin Actions — required before S09 deployment identity; not done in this PR |
| SEC-C7 | `--require-hashes` for pip — documented, not wired |
| SEC-C3 | Historical secret scan over **full** git history — scheduled job exists; this PR did not claim a clean human-attested run |
| DB-C3 / DB-C4 / DB-C9 | NFR-DR-03 restatement, JSONB cutover, payment-reconciliation runbook sentence — documented, not a production restore |
| R-C5 | This pack carries `signature_status`; not every historical AI artefact was backfilled |
| C4 | `R0-SCOPE.md` v0.4 republish is still Product's |

## Blocked by external / human / funding

| ID | Blocker |
|---|---|
| K-C1 | GAP-010 — no named executive sponsor for FRI-001 |
| R-C4 / C5 | Shailja human signature on rule packs; GAP-006 / GAP-007 |
| SEC-C4 | Whether Render.com processed real PII — unknown; not invented |
| SEC-C10 / SEC-C11 | Human Security signature; customer-identity / Aadhaar |
| OPS-C6 | CAP-A1…A7 business baseline; 1SB / PG contractual limits |
| OPS-C8 | Q4 tax-season peak vs production window — Product + Delivery |
| DB-C6 / DB-C7 / DB-C8 | Payment store, S09 restore drill, per-context physical schema at W2 |
| Q-C4 | QA-001 already closed; remaining package floors are QA-014 (human) |
| All verdict signatures | Nine seats outstanding |

## Non-waivable conditions (restated, not signed)

C5 (no S11 while GAP-006/007 open) · C7 (no automated PASSED/approval) · R-C1 · R-C2 · R-C3 (residency) · Q-C1 · K-C1.
