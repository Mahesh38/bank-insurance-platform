# Current State Dashboard

> **Generated, committed view.** CI regenerates this file from `governance/state/CURRENT-STATE.yaml` and fails if it drifts. The YAML remains authoritative.

**Governance:** AIGEM 1.4  
**State as of:** 2026-09-14  
**Review due:** 2026-10-11  
**Ratified by:** Mahesh (Solution Architect), 2026-08-10 — PO counter-signature outstanding; stage, scope and objective values re-confirmed unchanged at the 2026-09-11 R12 refresh

Authority: [CURRENT-STATE.yaml](../../governance/state/CURRENT-STATE.yaml)

## Workstreams

| Workstream | Current stage / phase | Status | Gate | Gate state |
|---|---|---|---|---|
| **WS-3 · AU Bank Insurance Distribution Platform** | S09 — Platform & Environment Foundation<br>Foundation Recovery Increment — S09 platform & environment foundation | IN_PROGRESS | `GATE-S09` | **OPEN** |
| **WS-1 · 1SB Insurance Integration** | L7 — Hardening<br>Phase 4 — Hardening & consumer enablement | IN_PROGRESS | `GATE-P4` | **BLOCKED** |
| **WS-2 · Workforce Authentication & Authorization** | L4/L6 — Foundation into first vertical slice<br>Phase 1 — Foundation implementation | IN_PROGRESS | `GATE-IAM-P1` | **OPEN** |

## WS-3 · AU Bank Insurance Distribution Platform

**Stage:** S09 — Platform & Environment Foundation  
**Phase:** Foundation Recovery Increment — S09 platform & environment foundation  
**Next:** S10 — Integration & Connectivity  
**Objective:** `R0-ASSISTED-LIFE-SALE` — One RM sells a complete Life insurance policy — Term or Savings/ULIP — to one ETB customer from one Group A insurer, end to end, through a real interface, with consent and suitability evidence, payment on the customer's own device, an issued and reconciled policy, and a complete audit trail. R0 includes both Term and Savings/ULIP assisted paths (CR-015). DIY and hybrid stay sequenced behind the assisted journey.

**Gate:** `GATE-S09` · **OPEN** — OPEN: 13

| Criterion | State | Owner |
|---|---|---|
| `S09-G1` All infrastructure defined as code; no console-created production resource | **OPEN** | Shivanshi / SRE |
| `S09-G2` Dev, UAT and production provisioned from the same modules | **OPEN** | Shivanshi / SRE |
| `S09-G3` Environment recreatable from code | **OPEN** | Shivanshi / SRE |
| `S09-G4` Automated deployment with tested rollback | **OPEN** | Shivanshi / SRE + Amit / Engineering |
| `S09-G5` Secrets management operational; TD-006 closed | **OPEN** | Deepali / Security + Shivanshi / SRE |
| `S09-G6` Observability operational: metrics, logs, traces correlated | **OPEN** | Shivanshi / SRE |
| `S09-G7` Backup automated and restore proven against RTO/RPO | **OPEN** | Aarti / Database + Shivanshi / SRE |
| `S09-G8` 7-year immutable retention implemented | **OPEN** | Shivanshi / SRE + Shailja / Compliance |
| `S09-G9` Data residency attested | **OPEN** | Shivanshi / SRE + Shailja / Compliance |
| `S09-G10` Encryption at rest and in transit verified | **OPEN** | Deepali / Security |
| `S09-G11` Network segmentation and least-privilege IAM enforced | **OPEN** | Deepali / Security |
| `S09-G12` IaC scanning in the pipeline | **OPEN** | Deepali / Security + Shivanshi / SRE |
| `S09-G13` No PII in aggregated logs | **OPEN** | Deepali / Security |

## WS-1 · 1SB Insurance Integration

**Stage:** L7 — Hardening  
**Phase:** Phase 4 — Hardening & consumer enablement  
**Next:** Phase 5 — Expand LOBs (Health → Motor)  
**Objective:** `P4-UAT-SIGNOFF` — Term path signed off for UAT use by at least one bank caller, while EPIC-002 delivers Life LOB adapter coverage (Term + Savings + ULIP) and adapter standards under CR-014 (does not replace Term UAT exit criteria).

**Gate:** `GATE-P4` · **BLOCKED** — BLOCKED: 3, MET: 1, OPEN: 2, PARTIAL: 1

| Criterion | State | Owner |
|---|---|---|
| `4.1` Sandbox E2E suite for the Term path runs in CI (or gated nightly) | **BLOCKED** | Amit / Engineering + R10 / Operations |
| `4.2` OpenAPI published to internal portal; consumer collection available | **PARTIAL** | — |
| `4.3` At least one bank caller exercises quote + proposal against UAT | **BLOCKED** | Rajal / Product |
| `4.4` Compliance review of audit schema and log samples | **OPEN** | — |
| `4.5` Runbook: secrets rotation, IP whitelist, 1SB 401/5xx incident | **OPEN** | — |
| `4.6` Performance smoke: p95 quote under nominal concurrency | **BLOCKED** | Amit / Engineering + R10 / Operations |
| `4.7` Coverage gates green; QA-001 closed or explicitly waived with expiry | **MET** | Swapnali / QA |

## WS-2 · Workforce Authentication & Authorization

**Stage:** L4/L6 — Foundation into first vertical slice  
**Phase:** Phase 1 — Foundation implementation  
**Next:** Phase 2 — Bank AD federation + production IdP decision  
**Objective:** `IAM-P1` — Provider-neutral workforce identity: token-hiding BFF session, Keycloak behind an adapter, business authorization service as the PDP.

**Gate:** `GATE-IAM-P1` · **OPEN** — OPEN: 6

| Criterion | State | Owner |
|---|---|---|
| `A.1` BFF token-hiding proven: Flutter never receives OAuth tokens | **OPEN** | — |
| `A.2` Keycloak isolated behind identity-provider-adapter-service | **OPEN** | — |
| `A.3` identity-authorization-service is the PDP; default-deny verified | **OPEN** | — |
| `A.4` Maker-checker enforced for bulk and privileged changes | **OPEN** | — |
| `A.5` Auth and admin events retained per policy; retention configurable | **OPEN** | — |
| `A.6` Provisioning outbox delivers reliably (retry, idempotency) | **OPEN** | — |
