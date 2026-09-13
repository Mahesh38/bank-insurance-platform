# Board 1 — Architecture · Draft verdict on CR-015

**Board:** 1 — Architecture · **AIGEM role:** R2
**Reviewer:** Mahesh — Principal Insurance Platform Architect
**Reviewer type:** AGENT (AI simulation of Mahesh)
**Self-review:** false
**Change request:** [CR-015](../../CR-015-ws3-r0-savings-ulip-journey.md)
**Epic:** [EPIC-004](../../../../platform/ws3-platform/EPIC-004.work-item.yaml)
**Date:** 2026-09-12

> ## Draft: `APPROVE-WITH-CONDITIONS`
> **Architecture severity:** `A2` — manageable increment of an existing Life cell; no new
> bounded context; S08 vs S11 split is the structural control.
>
> **`signature_status: AI-DRAFTED — mandatory human T4 signature outstanding`**
> [11-REVIEW_GATES.md §2](../../../11-REVIEW_GATES.md#2-who-may-sit-on-a-board) is binding.
> This AI simulation does not supply the Architecture signature. Listing my name as an
> approver on the CR YAML does not sit Board 1.

---

## 1. Decision requested of Architecture

| # | Decision | Draft |
|---|---|---|
| **D-1** | Catalogue matrix Life × Group A × {Term, Savings, ULIP} in the current increment | Recommend APPROVE |
| **D-2** | Docs/JES/OpenAPI at S08; Flutter/BFF implementation at S11 | Recommend APPROVE — same split as EPIC-003 |
| **D-3** | No new bounded context, no Flutter→1SB, no caller-supplied `distributorId` | Recommend APPROVE — standing constraints |
| **D-4** | Edit `current_phase` / `stage_status` / GATE-S08 | Recommend REJECT |
| **D-5** | Treat CANDIDATE transcription as T4 Architecture approval | Recommend REJECT |

---

## 2. What I reviewed

CR-015 · EPIC-004 · EPIC-002 / EPIC-003 `not_included` · WS-3 charter §2.2 / §3 ·
[Mahesh card](../../../../context/personas/mahesh-architecture.card.md) · standing
constraints in `CURRENT-STATE.yaml`.

---

## 3. Findings

CR-014 already placed Term + Savings + ULIP behind the Integration Hub anti-corruption
boundary. CR-015 does not open a second adapter, a second LOB cell, or a second catalogue
engine. It widens the **assisted journey consumer** of that adapter.

The 7-layer / 206-product engine (`SUG-20260904-eng`) stays parked — that would be a
structure change this increment does not need. Savings/ULIP JES is a carve from
`SUG-20260821-jx2`, not a new journey engine.

S08 feature-breadth remains forbidden. `EPIC-004` `FUNC-021` is correctly `BLOCKED` on
GATE-S08, GAP-006 and GAP-007.

---

## 4. Conditions (become AC if a human adopts this draft)

1. Human T4 Architecture signature required before production use of Savings/ULIP sales.
2. No stage-field edits. GATE-S08 stays OPEN until its owners declare criteria MET.
3. Provider traffic still routes only through the Integration Hub.
4. Deepali / Shailja sit before production if the Savings/ULIP journey adds PII or
   suitability surfaces beyond the Term pack — Architecture does not waive those boards.
5. `EPIC-004` documentation-complete at S08 does **not** authorise S11 screen implementation.

---

## 5. Signature status

`AI-DRAFTED`. Board 1 has not sat. Agent must not treat this as APPROVED.
