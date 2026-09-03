# CR-014 — Admit Life LOB (Term + Savings + ULIP) into WS-1 1SB adapter, with packaging, typed payloads and resilience

**Date:** 2026-09-03  
**Type:** SCOPE (with ARCH, NFR and ENG consequences)  
**Raised by:** Stakeholder decision → recorded by agent under human override ([09 §8](../09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process))  
**Workstream:** WS-1 (primary) · WS-3 catalogue/journey surfaces **unchanged**  
**Stage:** WS-1 Phase 4 — Hardening & consumer enablement (L7)  
**Decision:** **CANDIDATE — transcribed into WS-1 scope artefacts. Human T4 Architecture / Security / Risk & Compliance / Product signatures outstanding.**  
**Origin:** `SUG-20260903-lif` `ADMIT-BYPASS`  
**Epic:** [`EPIC-002`](../../1sb-insurance-integration/service-ssot/work-items/EPIC-002.work-item.yaml)

---

## 1. Current position

WS-1 Phase 4 hardens the **Term** vertical slice. `CURRENT-STATE.yaml` lists Savings / Annuity / Pension as `out_of_scope` until Phase 6+, circuit breaker until Phase 5.5, and Health/Motor until Phase 5. `Lob` already enumerates `SAVING` and `ULIP`, and `lob/life/saving` is a scaffold-only package.

Stakeholders have decided that the **single 1SB integration place** must cover the **Life** line of business — Term, Savings and ULIP — and that adapter engineering standards (bank models out of the 1SB service, typed JSON payloads, packaging / SOLID / DRY, documented poll-retry / circuit-breaker resilience) are in the same priority package. Parking on stage-fit is withdrawn for this package by explicit stakeholder direction.

---

## 2. Proposed change

Admit into **WS-1 current scope**, and schedule as `EPIC-002`:

| # | Change |
|---|--------|
| 1 | **Life LOB API coverage** in `1sb-integration-service`: Term (existing) + Savings + ULIP quote → proposal → poll paths, reusing shared orchestration (Case 2 / DRY). |
| 2 | **Bank-owned models leave the 1SB app service** — move bank API / domain models that are not 1SB-wire shapes into a shared library package (anti-corruption boundary preserved: 1SB types stay in `adapter.onesb.*`). |
| 3 | **Typed JSON payloads** for outbound 1SB requests/responses — no `Map`/`LinkedHashMap` assembly of command values for serialisation; Jackson-mapped request/response types. |
| 4 | **Package segregation** and design-practice pass (SOLID, DRY, established patterns) for the 1SB integration module, with an end-to-end engineering review checklist. |
| 5 | **Resilience policy as code + config**: publish and enforce poll attempt limits, backoff, stop conditions, HTTP retry rules (401 never retried), and **circuit breaker / bulkhead** for 1SB calls (`NFR-004` pulled forward). |

---

## 3. What this CR does not do

- Edit `current_phase` or `stage_status` (stage fields stay human).
- Admit **Health / Motor** LOB handlers (remain Phase 5).
- Admit **Annuity / Pension** LOB handlers (remain parked).
- Expand **WS-3 R0 product catalogue** or RM journey to Savings/ULIP sales — those stay R1 in WS-3 until a separate Product CR. This CR makes the **supplier adapter** Life-ready; it does not authorise R0 assisted sales of Savings/ULIP.
- Waive GATE-P4 Term UAT exit criteria, coverage gates, or ArchUnit standing constraints.
- Introduce Redis multi-instance idempotency (`NFR-005` / TD-010 stay Phase 5.4).
- Approve human T4 board signatures by AI simulation.

---

## 4. Driver

Stakeholder decision (2026-09-03): one silver-bullet integration application must work for Term, Savings and ULIP so Life LOB integration is complete; bank models must not live inside the 1SB app service; payloads must be typed models not ad-hoc maps; packaging and Java design standards apply; resilience (poll/retry stop criteria and circuit breakers) must be explicit. Priority: do **not** auto-park — **admit with actions**.

---

## 5. Relationship to parked items

| Parked item | Effect of this CR |
|---|---|
| E12 Saving / Annuity / Pension | **Split.** Savings (+ ULIP, newly explicit) → admitted under `EPIC-002`. Annuity / Pension remain parked as E12. |
| NFR-004 Circuit breaker (Phase 5.5) | **Pulled forward** into `EPIC-002` (requires this CR per [14 §1](../14-CHANGE_CONTROL.md)). |
| SUG-20260821-jx2 ULIP/Savings journey specs (WS-3) | **Unchanged / still parked** for WS-3 journey surfaces. Adapter work is not a JES unpark. |
| WS-3 out_of_scope “ULIP and Savings/Endowment product classes” | **Unchanged.** Catalogue/journey remain R1. |

---

## 6. Impact

- `docs/governance/state/CURRENT-STATE.yaml` — WS-1 `current_scope` only (not stage fields)
- `docs/1sb-insurance-integration/service-ssot/PRODUCT-BACKLOG.md`
- `docs/1sb-insurance-integration/service-ssot/ACTION-PLAN.md`
- `docs/governance/registers/PARKED-BACKLOG.md` (E12 split)
- `docs/governance/registers/SUGGESTION-REGISTER.md` (`SUG-20260903-lif`)
- `EPIC-002` + child work items
- BOOT regenerated from state

GATE-P4 Term criteria remain. This CR adds WS-1 adapter scope; it is not a waiver of sandbox E2E / coverage / consumer UAT for Term.

---

## 7. Authority

| Role | Action |
|---|---|
| Stakeholder / human override | Authorised immediate admission; bypass of SF3 parking ([09 §8](../09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process)) |
| Rajal (R1) | Scope content — human counter-signature outstanding |
| Mahesh (R2) | Structure / packaging / anti-corruption — T4 outstanding |
| Amit (R3) | Engineering standards, typed payloads, LOB handlers |
| Deepali (R8) | Resilience / outbound trust — T4 outstanding if CB changes auth path behaviour |
| Shivanshi (R10) | Poll/retry/CB operational policy |
| Swapnali (R7) | Life LOB test evidence |
| Shailja (R9) | Confirm no new PII/log or consent obligation beyond existing Term path — human call |

**Bypass risk (one sentence):** boards that would normally gate a Phase-6 LOB pull-forward and a Phase-5.5 circuit breaker did not sit before transcription; human T4 remains mandatory before production use of Savings/ULIP or CB-backed egress.

---

## 8. Admitted actions (work breakdown)

See `EPIC-002`. Ordered for enablement:

1. DOC field guides for Savings + ULIP (prerequisite for handlers)
2. REFACTOR bank-model extraction + typed 1SB payloads (enables clean LOB handlers)
3. FUNC Savings handlers → FUNC ULIP handlers
4. NFR resilience policy + circuit breaker
5. QA Life LOB regression + Term non-regression
