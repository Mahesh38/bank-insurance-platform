# GATE-P4 — Evidence table

**Workstream:** WS-1 — 1SB Insurance Integration
**Gate:** `GATE-P4` · Phase 4 (Hardening & consumer enablement) → Phase 5
**Prepared by:** agent, 2026-08-14, against `CURRENT-STATE.yaml` (FRESH — `FreshnessCheck` exit 0)
**Purpose:** [04 §5](../../../governance/04-STAGE_GATES.md#5-who-may-declare-a-transition) —
*"An agent that believes a gate is complete produces the evidence table and says so."*

## Verdict: the gate is **not** ready for `CANDIDATE`, and cannot be marked `PASSED` here

Two separate reasons, and both hold independently:

1. **Evidence.** Five of seven exit criteria have **no evidence artefact at all**; two are
   partial. `CANDIDATE` requires an artefact per criterion ([04 §2](../../../governance/04-STAGE_GATES.md#2-gate-anatomy)) —
   "a CI run, a test report, a signed document, a merged PR, a dashboard link". Nothing can be
   attached for 4.1, 4.3, 4.4 or 4.6 today because the underlying work does not exist.
2. **Authority.** An AI agent **may never** mark a gate `PASSED`
   ([04 §5](../../../governance/04-STAGE_GATES.md#5-who-may-declare-a-transition)) — that is
   reserved to **Architect + PO jointly**. An agent may mark `CANDIDATE`, but only *with* the
   evidence table above, which is precisely what reason 1 blocks.

Marking this gate closed would be recording a stage transition that the evidence does not
support. That is the failure mode `13-DEFINITION_OF_DONE` and `17-DRIFT_CONTROL` exist to
prevent, so it is not done here.

---

## 1. Criterion-by-criterion evidence

| # | Exit criterion | Evidence required | Evidence that exists | State | Blocked on |
|---|----------------|-------------------|----------------------|-------|------------|
| 4.1 | Sandbox E2E suite for the Term path runs in CI (or gated nightly) | Green CI job link + suite location | **None.** No E2E suite exists in the repo. TD-014 (WireMock/full E2E integration ↔ persistence) is parked with its **unpark trigger already fired**. | ❌ Open | Build work + 1SB sandbox credentials (CONFIRM-01 open) |
| 4.2 | OpenAPI published to internal portal; consumer collection available | Portal URL + collection file | **Partial.** OpenAPI is *generated* (`OpenApiConfig` in both services, `79c65f4`). No portal publication, no consumer collection file. | 🟡 Partial | Internal portal (external to this repo) |
| 4.3 | ≥ 1 bank caller exercises quote + proposal against UAT | Consumer confirmation + UAT trace/correlation IDs | **None.** Requires a third party to run traffic. | ❌ Open | **External** — bank consumer team + working UAT egress |
| 4.4 | Compliance review of audit schema + log samples | Signed review note in `service-ssot/` | **None.** No review note exists. | ❌ Open | **Human board** — Shailja / Board 6 |
| 4.5 | Runbook: secrets rotation, IP whitelist, 1SB 401/5xx incident | Runbook document | **[OPERATIONS-RUNBOOK.md](./OPERATIONS-RUNBOOK.md)** — drafted this session, covering §2 rotation, §3 whitelist, §4 401, §5 5xx, mapped to Board 7 O1–O8. | 🟡 Partial — artefact exists, **unsigned** | **Human board** — Shivanshi / Board 7 verdict |
| 4.6 | Performance smoke: p95 quote under nominal concurrency | Measurement report + threshold | **None.** No perf harness (QA-011 / k6 not started); no agreed threshold. | ❌ Open | Harness + a deployed environment to measure |
| 4.7 | Coverage gates green; QA-001 closed or explicitly waived with expiry | JaCoCo report + TECH-DEBT entry | **Partial.** Libs at 80/70. Services still on the **interim** floor. QA-001 neither closed nor waived. | 🟡 Partial | Test work, or a dated waiver from Architect + PO |

**Score: 0 of 7 criteria closed.** 3 partial, 4 fully open.

---

## 2. Why the gate cannot simply be closed

The four fully-open criteria are not blocked on effort that could be spent here:

- **4.3 requires a third party.** No amount of work in this repository produces a bank caller
  exercising UAT. It is the definition of the objective (`P4-UAT-SIGNOFF`) and it is external.
- **4.4 and 4.5 require human sign-off** from named board personas (Shailja / Board 6,
  Shivanshi / Board 7). An agent simulating a board **does not** produce a binding verdict —
  recording one would be fabricating a mandatory sign-off, exactly the breach CR-009's ratification
  note calls out and refuses.
- **4.1 and 4.6 require a working environment** — 1SB sandbox credentials and IP whitelist, both
  recorded as still open in `CONFIRM-01 §D`.

Per [RUNBOOK §9](../../../governance/RUNBOOK.md#9-escalation): *"A gate criterion cannot be met →
**Architect + PO** — waiver or move the criterion — **before** `CANDIDATE`."* That escalation is
the correct next step for 4.3 in particular, and it is a human decision.

---

## 3. Shortest honest path to `CANDIDATE`

Ordered by dependency, not by preference:

| Step | Criterion | Owner | Note |
|------|-----------|-------|------|
| 1 | Close **CONFIRM-01 §D** (credentials + IP whitelist confirmed live) | PO + 1SB RM | Hard prerequisite for 4.1, 4.3, 4.6 — nothing upstream-facing can be proven without it |
| 2 | Board 7 verdict on **[OPERATIONS-RUNBOOK.md](./OPERATIONS-RUNBOOK.md)** → closes **4.5** | Shivanshi | Artefact ready now; no further engineering needed |
| 3 | Triage **TD-014** (trigger fired) and build the E2E suite → closes **4.1** | R1/R2 then Dev | See §4 |
| 4 | Compliance review of audit schema + log samples → closes **4.4** | Shailja | Can run in parallel with 3 |
| 5 | Close QA-001 or issue a **dated waiver** → closes **4.7** | QA Lead + Architect/PO | CR-001 made 4.7 binding — the gate cannot pass on an "interim" floor |
| 6 | Perf smoke with an agreed threshold → closes **4.6** | QA + Eng | Needs step 1 |
| 7 | Publish OpenAPI + consumer collection → closes **4.2** | Dev + Platform | Needs the internal portal |
| 8 | Bank caller exercises quote + proposal → closes **4.3** | Bank consumer team | Needs 1, 2, 7 |

Only step 2 is ready to complete today.

---

## 4. Open items surfaced, not actioned

Recorded here so they are not lost, and deliberately **not** acted on — one work item in flight
([RUNBOOK §8.4](../../../governance/RUNBOOK.md#84-the-five-behaviours-that-matter-most)):

- **TD-014's unpark trigger has fired.** Flagged in
  [`state/REVIEW-LOG.md`](../../../governance/state/REVIEW-LOG.md) during the CR-009 sweep and in
  `PARKED-BACKLOG.md` §1. It overlaps criterion 4.1 directly. Unparking is a **triage decision for
  R1/R2**, not something this evidence pass may decide.
- **Approver drift (corrected).** `04-STAGE_GATES.md` §6 listed 4.5's approver as "Ops" — a role
  with no persona — while `CURRENT-STATE.yaml` had already been updated to
  "Shivanshi / SRE (4.5)" by CR-008. The state file is authoritative; the gate document has been
  aligned to it in this change. The state file's own comment records that the unnamed approver
  "is why 4.5 could not close".
- **Phase 4 STATUS.md variance.** No QA Engineer / QA Lead cycle was run for FUNC-007/FUNC-009
  (TL review only) — a recorded variance from WORK-SEQUENCE §3. It bears on 4.7's credibility and
  should be considered when QA-001 is closed or waived.

---

## 5. What this document is not

It is not a `CANDIDATE` declaration, and it is not an approval. It is the evidence artefact that
lets **Architect + PO** decide whether to waive, move, or hold criteria — the decision rights
[04 §5](../../../governance/04-STAGE_GATES.md#5-who-may-declare-a-transition) reserves to them.
