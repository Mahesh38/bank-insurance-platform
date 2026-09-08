# Current State Dashboard

> **Generated, committed view.** CI regenerates this file from `governance/state/CURRENT-STATE.yaml` and fails if it drifts. The YAML remains authoritative.

**Governance:** AIGEM 1.4  
**State as of:** 2026-08-10  
**Review due:** 2026-09-09  
**Ratified by:** Mahesh (Solution Architect), 2026-08-10 — PO counter-signature outstanding

Authority: [CURRENT-STATE.yaml](../../governance/state/CURRENT-STATE.yaml)

## Workstreams

| Workstream | Current stage / phase | Status | Gate | Gate state |
|---|---|---|---|---|
| **WS-3 · AU Bank Insurance Distribution Platform** | S08 — Engineering Foundation<br>Foundation Recovery Increment — S08 with S09 overlapped | IN_PROGRESS | `GATE-S08` | **OPEN** |
| **WS-1 · 1SB Insurance Integration** | L7 — Hardening<br>Phase 4 — Hardening & consumer enablement | IN_PROGRESS | `GATE-P4` | **BLOCKED** |
| **WS-2 · Workforce Authentication & Authorization** | L4/L6 — Foundation into first vertical slice<br>Phase 1 — Foundation implementation | IN_PROGRESS | `GATE-IAM-P1` | **OPEN** |

## WS-3 · AU Bank Insurance Distribution Platform

**Stage:** S08 — Engineering Foundation  
**Phase:** Foundation Recovery Increment — S08 with S09 overlapped  
**Next:** S09 — Platform & Environment Foundation  
**Objective:** `R0-ASSISTED-TERM-SALE` — One RM sells one Term Life policy to one ETB customer from one Group A insurer, end to end, through a real interface, with consent and suitability evidence, payment on the customer's own device, an issued and reconciled policy, and a complete audit trail.

**Gate:** `GATE-S08` · **OPEN** — OPEN: 10

| Criterion | State | Owner |
|---|---|---|
| `S08-G1` CI builds and tests every module on every PR | **OPEN** | Amit / Engineering |
| `S08-G2` Merge to main impossible without a green pipeline | **OPEN** | Amit / Engineering |
| `S08-G3` Coverage thresholds enforced; QA-001 closed | **OPEN** | Swapnali / QA |
| `S08-G4` ArchUnit and static analysis enforced | **OPEN** | Amit / Engineering |
| `S08-G5` Secret, SAST, SCA and image scanning in the pipeline | **OPEN** | Deepali / Security |
| `S08-G6` Test infrastructure operational at every pyramid level | **OPEN** | Swapnali / QA |
| `S08-G7` No PII in logs, proven by automated test | **OPEN** | Deepali / Security |
| `S08-G8` Engineering and secure coding standards published and adopted | **OPEN** | Amit / Engineering |
| `S08-G9` Pipeline feedback under 10 minutes at p95; flake under 1% | **OPEN** | Shivanshi / SRE |
| `S08-G10` A new engineer can build, test and ship in under a week | **OPEN** | Amit / Engineering |

## WS-1 · 1SB Insurance Integration

**Stage:** L7 — Hardening  
**Phase:** Phase 4 — Hardening & consumer enablement  
**Next:** Phase 5 — Expand LOBs (Health → Motor)  
**Objective:** `P4-UAT-SIGNOFF` — Term path signed off for UAT use by at least one bank caller, while EPIC-002 delivers Life LOB adapter coverage (Term + Savings + ULIP) and adapter standards under CR-014 (does not replace Term UAT exit criteria).

**Gate:** `GATE-P4` · **BLOCKED** — BLOCKED: 4, OPEN: 2, PARTIAL: 1

| Criterion | State | Owner |
|---|---|---|
| `4.1` Sandbox E2E suite for the Term path runs in CI (or gated nightly) | **BLOCKED** | Amit / Engineering + R10 / Operations |
| `4.2` OpenAPI published to internal portal; consumer collection available | **PARTIAL** | — |
| `4.3` At least one bank caller exercises quote + proposal against UAT | **BLOCKED** | Rajal / Product |
| `4.4` Compliance review of audit schema and log samples | **OPEN** | — |
| `4.5` Runbook: secrets rotation, IP whitelist, 1SB 401/5xx incident | **OPEN** | — |
| `4.6` Performance smoke: p95 quote under nominal concurrency | **BLOCKED** | Amit / Engineering + R10 / Operations |
| `4.7` Coverage gates green; QA-001 closed or explicitly waived with expiry | **BLOCKED** | Swapnali / QA |

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
