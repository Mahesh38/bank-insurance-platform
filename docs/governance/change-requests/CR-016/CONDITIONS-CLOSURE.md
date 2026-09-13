# CR-016 — Conditions closure pack

**Purpose:** Understand every board HOLD / `must_fix` / watch item from the
[verdicts pack](./verdicts/README.md) and attach a **concrete solution**.  
**Date:** 2026-09-13  
**Owner (Architecture):** Mahesh  
**Status:** Solutions authored and wired into operating artefacts. Human countersigns for R1/R12 remain — those are *acts*, not missing designs.

> We do **not** “move ahead” by ignoring HOLDs. Where a HOLD is a human signature, the solution is a **ready-to-sign brief** plus binding interim controls so agents cannot create the failure mode the board named while the signature is pending.

---

## 1. HOLD reasons (blocking)

| ID | Who | Why they HOLD | What would unblock | Solution artefact |
|---|---|---|---|---|
| **H1** | Rajal (Product) | Architecture approved *lanes*, not a changed North Star *outcome*. Product must confirm objective continuity (or amend it). Service-green must not redefine journey success. | Human R1 countersign (or dated amendment of objective text) | [countersign-rajal-north-star-continuity.md](./countersign-rajal-north-star-continuity.md) |
| **H2** | Kalpana (Delivery) | Must not rewrite `CURRENT-STATE.yaml` topology while H1 is open; parallel boards can create **false green** vs critical path; waits need DL ageing. | Human R12 countersign **after** H1 (or explicit R1 deferral) + transcription checklist complete | [countersign-kalpana-transcription-checklist.md](./countersign-kalpana-transcription-checklist.md) |

---

## 2. Approval conditions (`must_fix`) — all boards

| ID | Board | Condition | Solution | Status |
|---|---|---|---|---|
| **C1** | Architecture | `SWS-*` never carries independent `stage_status` / stage-fit target | Rule **WS-NS-1** reinforced; interim dual-topology routing rule **WS-NS-3** | ✅ in [WORKSTREAM-STRATEGY](../../workstreams/WORKSTREAM-STRATEGY.md) |
| **C2** | Architecture | Standing constraints unchanged (no Hub bypass, no bank→1SB/DB, no Flutter tokens) | Rule **WS-NS-7**; board header reminder | ✅ strategy + boards |
| **C3** | Technical | Active/Completed rows cite governed work-item IDs | Rule **WS-NS-4**; board template columns | ✅ strategy + boards |
| **C4** | Technical | No implementation PRs solely to “populate” a board | Rule **WS-NS-5** (claim gate) | ✅ strategy |
| **C5** | Technical | Cross-service Done needs named DEP / contract path | [SYNC-CHECK-EVIDENCE-BAR](../../workstreams/SYNC-CHECK-EVIDENCE-BAR.md) | ✅ |
| **C6** | Product | North Star outcome text unchanged unless Rajal edits it | H1 brief freezes `R0-ASSISTED-LIFE-SALE` wording | ✅ brief ready |
| **C7** | Product | Customer/RM-visible behaviour needs Product-admitted work item | Rule **WS-NS-4** + Product brief §3 | ✅ |
| **C8** | Product | Programme priority overrides local SWS priority | [PROGRAMME-ROLLUP](../../workstreams/PROGRAMME-ROLLUP.md) §3 | ✅ |
| **C9** | Product | CR-016 does not expand LOB/channel/DIY scope | Explicit non-goal in strategy §8 + Product brief | ✅ |
| **C10** | Security | No PII / secrets on progress boards | Rule **WS-NS-6** | ✅ |
| **C11** | Security | Identity/payment/consent contract behaviour changes escalate to Board 4 | SYNC-CHECK bar §4 | ✅ |
| **C12** | QA | Publish sync-check evidence bar before scaled cross-SWS Done | SYNC-CHECK-EVIDENCE-BAR | ✅ |
| **C13** | QA | Behaviour-change Completed rows link test report or AC id | WS-NS-4 + SYNC-CHECK bar | ✅ |
| **C14** | QA | Board hygiene ≠ GATE-S08 evidence | PROGRAMME-ROLLUP §2 + QA note | ✅ |
| **C15** | Compliance | Boards non-authoritative for regulatory evidence | Rule **WS-NS-8** | ✅ |
| **C16** | Operations | Board updates in same implementing PR | Already strategy §6.4; reinforced | ✅ |
| **C17** | Operations | No Board 7 readiness claim from board hygiene | PROGRAMME-ROLLUP §2 | ✅ |
| **C18** | Operations | Shared env / sandbox contention unchanged | Claim gate WS-NS-5 + roll-up §5 | ✅ |
| **C19** | Delivery | Forecast rolls up through programme milestones, not board counts | PROGRAMME-ROLLUP | ✅ |
| **C20** | Delivery | Waiting rows use DL0–DL3 ageing | PROGRAMME-ROLLUP §4 | ✅ |
| **C21** | Delivery | Transcription is human, regenerates BOOT, marks no gate MET | Kalpana checklist | ✅ ready for human |

---

## 3. Cross-cutting concerns → controls

| Concern | Failure mode if ignored | Control |
|---|---|---|
| Dual topology | Stage-fit against wrong ID | **WS-NS-3**: until transcription, lifecycle SF/gates → WS-3 carrier; execution ownership → `SWS-*` under ADR-020 |
| False parallel green | 21 greens, journey red | Programme roll-up is the only forecast green; SWS green is local only |
| Honor-system sync | Fake Done | Evidence bar: DEP id + artefact path + consumer proof or dated waiver |
| Trust-boundary drift | “My SWS owns adapter” → bypass | WS-NS-7 standing constraints on every board |
| Second backlog | Free-text shadow work | WS-NS-4 work-item IDs mandatory |
| Skeleton fan-out | Noise PRs on empty modules | WS-NS-5 claim only with READY work-item |
| GOV capacity | Hygiene displaces GATE-S08 | Roll-up tracks open waits; CR already names S08-G10 deferral — close H1/H2 fast |

---

## 4. What is still human (cannot be “solved” by docs alone)

| Act | Who | Why docs are not enough |
|---|---|---|
| Countersign H1 | Rajal | Product authority over outcome text (PA / R1) |
| Countersign H2 + state transcription | Kalpana | Agents must not edit stage topology; R12 owns transcription |
| Adopt sync-check bar in live PRs | Swapnali + owning engineers | Evidence sufficiency is exercised, not only published |

Until H1 and H2 land, **agents may use `SWS-*` boards under the interim rules**, but must not:

- claim GATE-S08 progress from board counts,
- triage lifecycle stage-fit against an `SWS-*`,
- rewrite `CURRENT-STATE.yaml` `workstreams:`.

---

## 5. Closure checklist

- [x] Conditions matrix published (this file)
- [x] Sync-check evidence bar published
- [x] Programme roll-up published
- [x] Strategy rules WS-NS-1…8 hardened
- [x] Board hygiene template updated
- [x] Rajal countersign brief ready
- [x] Kalpana transcription checklist ready
- [ ] **Rajal human signature on H1**
- [ ] **Kalpana human signature on H2 + transcription PR**
- [ ] First real cross-SWS Done uses SYNC-CHECK bar (prove in a later delivery PR)

---

## 6. Index of solution artefacts

| Artefact | Path |
|---|---|
| This matrix | `docs/governance/change-requests/CR-016/CONDITIONS-CLOSURE.md` |
| Sync-check evidence bar | `docs/governance/workstreams/SYNC-CHECK-EVIDENCE-BAR.md` |
| Programme roll-up | `docs/governance/workstreams/PROGRAMME-ROLLUP.md` |
| Rajal brief | `docs/governance/change-requests/CR-016/countersign-rajal-north-star-continuity.md` |
| Kalpana checklist | `docs/governance/change-requests/CR-016/countersign-kalpana-transcription-checklist.md` |
| Operating strategy | `docs/governance/workstreams/WORKSTREAM-STRATEGY.md` |
| Board index | `docs/governance/workstreams/service-progress/README.md` |
