# Carry-over — open AIGEM items and where each lands

Snapshot of the product repository at `cd58970`, 2026-09-28. Nothing open is dropped; each item
gets a new home. Items already done stay done, with their evidence archived.

## 1. Gates and criteria

| AIGEM gate / criterion | State | New home |
|---|---|---|
| **GATE-S08** (WS-3) G1–G10 | 10/10 MET, `CANDIDATE`, no approvals | Engineering foundation **complete** (`DEC-MIG-02`). Criteria become guardrails GR-ENG-01/02, GR-DAT-01 and lane maturity (platform `ci_cd: M4`) |
| GATE-S09 (never opened) | — | Outcome `O-PL-1` (Now), `O-PL-2` (Next) |
| **GATE-P4** 4.1 sandbox E2E in CI | BLOCKED (GATE-4.1-SANDBOX-E2E) | Card under `O-IH-1`; if sandbox unstable → gated nightly (ASM-003 already sanctions it) |
| 4.2 OpenAPI + consumer collection | PARTIAL | Card under `O-IH-1`, **start now** |
| 4.3 bank caller in UAT | BLOCKED (DEP-001, DEP-002) | `O-IH-1` done signal; external `DEP-UAT-SLOT` |
| 4.4 compliance review of audit schema | OPEN | Card under `O-IH-1`, **start now**; outcome also scopes TD-023 |
| 4.5 runbook (secrets rotation, IP allowlist, 1SB 401/5xx) | OPEN | Card under `O-IH-1`, **start now** |
| 4.6 performance smoke p95 quote | BLOCKED (DEP-003, SOFT) | Card under `O-IH-1`; build standalone if E2E harness is late |
| 4.7 coverage / QA-001 | MET | Guardrail GR-ENG-02 |
| **GATE-IAM-P1** A.1–A.3 | OPEN | Outcome `O-WI-1` (Now) |
| A.4–A.6 | OPEN | Outcome `O-WI-2` (Next) — none depends on AD technology |
| S11 entry: GAP-006 / GAP-007 closed (DEC-20260816-06, non-waivable) | open | **Kept hard** as a promotion rule: `DEC-OPEN-GAP006-007` blocks bank-UAT for the consent/suitability path. It no longer freezes intake |
| WS-1 Phase 5 unfreeze (DEC-20260816-05) | frozen | `DEC-OPEN-LOB-UNFREEZE`; Health/Motor in `Later` |

## 2. Parked backlog (7 items) → tracker `Later` / `Next`

| Item | New placement | Why |
|---|---|---|
| TD-014 E2E integration ↔ persistence | Done (cleared under S08-G6) | Close with archive link |
| TD-006 Secrets Manager stub | **Checkpoint** item: required for UAT promotion; card in `platform` Next | Was "P4 now / P1 at target" — a checkpoint says the same thing without a prediction |
| TD-010 Redis idempotency | **Checkpoint** item: required before >1 instance in a shared environment | ASM-002 |
| TD-022 FUNC-008 payment intimation | `integration-hub` Later, review_by 2026-12-31 | R0 payment path owner decides pull |
| TD-023 raw payload capture breadth | Card after 4.4 compliance review decides scope | DEP-008 |
| TD-009 missing domain ports | `integration-hub` Enabler, Next | Needed by EPIC-002 typed payloads |
| TD-007 ArchUnit `allowEmptyShould` | `integration-hub` Enabler, pull_when LOB packages populated | DEP-011 |

Known-debt ids (TD-006/007/009/010/022/023, QA-014) are listed in the integration-hub pack so
agents still do not re-report them.

## 3. Suggestion register (69 rows)

Not migrated row by row. At M1:
- rows with verdict `ADMIT`/`ADMITTED`/`ADMIT-BYPASS` whose work is not finished → a card each;
- `PARKED` rows → `Later` with `review_by` = cutover + 90 days;
- `REJECT(ED)` rows → stay in the archive; anyone can reopen with new information.

## 4. Decisions, dependencies, risks, assumptions

| Source | New home |
|---|---|
| 21 Proposed / AI-drafted ADR & DB-DEC rows | `DEC-OPEN-ADR-BATCH` + specific rows in `state/decisions.yaml`; ADR log stays in the product repo |
| GOV-001…009, CR-009, CR-016 ratification | Superseded by `DEC-MIG-01`; archived with their provenance intact |
| BRD conflicts C1–C7, questions Q1–Q10 | Answered ones (C1/C2 → D-015, C3 → D-016) closed; open ones in `state/decisions.yaml` |
| 5 external dependencies (all past 2026-09-18) | `state/dependencies.yaml` with new chase dates |
| DEP-001/003/005/009 stage-coupled edges | Retired (listed in `state/dependencies.yaml` `retired_edges`) |
| RISK-003…015 | Risk register stays in the product repo; risks with a mitigation action get an Enabler card |
| ASM-002…019 | Assumption register stays; those with a validation date become open questions |
| Known open debt, fact 7 | Pack "Known debt" sections |

## 5. Human signatures already given

Every approval recorded under AIGEM (CR-009 by the R2 holder with §9.1 provenance, S08 criterion
closures by named seats, ADR-019 acceptance) remains valid **as recorded** — including its
provenance caveats. FLOW does not re-sign or re-interpret them.
