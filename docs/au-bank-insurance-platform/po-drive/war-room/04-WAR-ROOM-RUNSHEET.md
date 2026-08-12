# 04 — War Room Run Sheet

**Called by:** Platform Product Owner
**Purpose:** Agree the problem, approve or reject the realignment, leave with a sprint plan
**Proposed date:** _____________ (recommend within 5 working days of pack circulation)
**Duration:** 120 minutes — hard stop
**Format:** One session, all stakeholders, decisions recorded live in this document

---

## 1. Attendees

| Role | Name | Required? | Why they must be in the room |
|------|------|:---------:|------------------------------|
| Product Owner | Rajal | **Mandatory** | Proposer; owns scope and requirement sign-off |
| Solution Architect | Mahesh | **Mandatory** | Co-approver of `CR-002` ([14 §1](../../../governance/14-CHANGE_CONTROL.md#1-what-needs-a-change-request)); owns the canonical model |
| Technical Head | Amit | **Mandatory** | Owns engineering capacity and the staffing split |
| Delivery Lead | | **Mandatory** | Owns `CURRENT-STATE.yaml`, the sprint calendar, and ways of working |
| Tech Lead | | **Mandatory** | Owns Track B execution and the debt ledger |
| QA Lead | | **Mandatory** | AC testability; gate 4.7 / QA-001; counter-signature outstanding on CR-001 |
| Security Architect | | **Mandatory** | WS-2 gate A.1–A.3; **cannot be waived** for T4 items ([14 §1](../../../governance/14-CHANGE_CONTROL.md#1-what-needs-a-change-request)) |
| Compliance | | **Mandatory** | Consent (`GAP-006`), suitability (`GAP-007`), audit schema (gate 4.4), IRDAI CA0515 evidence |
| Business Analyst | | **Mandatory** | Owns requirements, AC and the RTM — the critical path of Track A |
| Ops / DevOps | | Recommended | Environments (PZ.3), runbook (gate 4.5) |
| UX / Digital | | Recommended | Figma-to-journey reconciliation (`GAP-009`) |
| Executive Sponsor | **unnamed** | **Recommended — or named in this session** | `GAP-010` is open; PZ.1 cannot close without a named sponsor |

> If Security Architect or Compliance cannot attend, **the session still runs**, but decisions
> D-WR-04, D-WR-05 and D-WR-07 are recorded as *provisional pending their written verdict on
> [05-STAKEHOLDER-REVIEW-SHEET.md](./05-STAKEHOLDER-REVIEW-SHEET.md)*. They are never assumed.

---

## 2. Pre-reads (circulate 48 hours ahead)

| Everyone | [README](./README.md) · [01-PROCESS-GAP-ANALYSIS](./01-PROCESS-GAP-ANALYSIS.md) (all) · [02-REALIGNMENT-PROPOSAL](./02-REALIGNMENT-PROPOSAL.md) §§1–4, §10 |
|---|---|
| Architect, Tech Head, Tech Lead | + [02](./02-REALIGNMENT-PROPOSAL.md) §§5–7 · [03-DELIVERY-MODEL](./03-DELIVERY-MODEL-AND-FLOW-PLAN.md) (all) |
| QA, Security, Compliance | + [03](./03-DELIVERY-MODEL-AND-FLOW-PLAN.md) §1 (S1/S5), §6 · your row in [05](./05-STAKEHOLDER-REVIEW-SHEET.md) |
| Delivery Lead | + [02 §9](./02-REALIGNMENT-PROPOSAL.md#9-what-changes-in-the-repository-if-cr-002-is-approved) — you execute it |

**Come with your row in [05](./05-STAKEHOLDER-REVIEW-SHEET.md) already filled in.** The session
is for resolving disagreement, not for first-time reading.

---

## 3. Agenda

| # | Item | Time | Lead | Output |
|:-:|------|:----:|------|--------|
| 1 | **Framing** — this is a process correction, not a performance review. Ground rules (§5) | 5 min | PO | Room agrees the tone |
| 2 | **The evidence** — [01](./01-PROCESS-GAP-ANALYSIS.md) findings F1–F4, walked through with the dates on screen | 15 min | PO | **D-WR-01** |
| 3 | **Challenge the evidence** — anything factually wrong, missing context, or unfair? | 10 min | All | Corrections captured in [01](./01-PROCESS-GAP-ANALYSIS.md) |
| 4 | **The options** — A do nothing · B stop & reset · C dual track · D dual track without the seam | 10 min | PO | **D-WR-02** |
| 5 | **The seam** — Rule S-1 and the three supporting rules; how it is enforced in PR review | 15 min | Architect | **D-WR-03**, **D-WR-04** |
| 6 | **Governance changes** — WS-0 workstream, DoR/DoD amendments, routing | 10 min | Delivery Lead | **D-WR-05** |
| 7 | **Flow sequence** — the map, and the lead-before-quote correction | 15 min | PO + Architect | **D-WR-06** |
| 8 | **Staffing** — the 40/60 split and the four non-negotiables | 15 min | Tech Head | **D-WR-07** |
| 9 | **Track B exit** — what happens if gate 4.3 stays blocked externally | 5 min | Tech Lead | **D-WR-08** |
| 10 | **Sponsor & cadence** — name the sponsor, confirm steering rhythm | 5 min | PO | **D-WR-09** |
| 11 | **Sprint 1 plan** — commit the first sprint in the room | 10 min | Delivery Lead | Sprint backlog |
| 12 | **Close** — read back all nine decisions, owners, dates | 5 min | PO | Signed [05](./05-STAKEHOLDER-REVIEW-SHEET.md) |

---

## 4. Decisions the room must take

Fill this table **in the session**. A decision with no owner and no date is not a decision.

| ID | Decision | Options | PO recommends | Verdict | Owner | Date |
|----|----------|---------|---------------|:-------:|-------|------|
| **D-WR-01** | Do we accept findings F1–F4 as the agreed statement of the problem? | Accept / Accept with corrections / Reject | **Accept with corrections** — the pack already corrects the PO's own "no BRDs" claim | | | |
| **D-WR-02** | Which recovery option? | A do nothing · B stop & reset · C **dual track** · D dual track without seam | **C** | | | |
| **D-WR-03** | Do we create workstream **WS-0 — Distribution Platform** in governance? | Yes / No | **Yes** — without it, corrective work is unadmittable ([01 §7](./01-PROCESS-GAP-ANALYSIS.md#7-finding-f3--the-product-is-not-a-tracked-workstream)) | | | |
| **D-WR-04** | Do we adopt **Rule S-1** (Track B cap) and S-2/S-3/S-4? | Adopt all / Adopt subset / Reject | **Adopt all** — S-1 is the proposal; without it this is option D | | | |
| **D-WR-05** | Do we adopt the DoR and DoD amendments (requirement ID at ready; RTM row at done)? | Yes / Yes with wording change / No | **Yes** | | | |
| **D-WR-06** | Do we accept the flow map **and** the corrected sequence — lead → consent → suitability → catalogue → quote? | Accept / Amend | **Accept** — D-005 makes suitability a pre-quote gate | | | |
| **D-WR-07** | Do we accept the ~40/60 capacity split and the four staffing non-negotiables? | Accept / Amend ratio / Reject | **Accept**, especially "Architect sits in Track A" | | | |
| **D-WR-08** | If gate 4.3 (bank caller UAT) stays externally blocked, what does Track B do? | Wait / Invent work / **Release capacity to Track A** | **Release capacity to Track A**; escalate `DEP-002` / `RISK-002` | | | |
| **D-WR-09** | Who is the Executive Sponsor, and what is the steering cadence? | Name + cadence | Name today; **bi-weekly** steering | | | |

**If D-WR-02 = A (do nothing):** the room must additionally record a **formal waiver of the Wave
0 rule** ([01 §6](./01-PROCESS-GAP-ANALYSIS.md#6-finding-f2--we-are-building-without-the-authorisation-to-build))
and accept the audit exposure by name. That waiver is itself a `CR` requiring Architect + PO +
Compliance ([14 §1](../../../governance/14-CHANGE_CONTROL.md#1-what-needs-a-change-request)).
Doing nothing is a decision with paperwork, not the absence of one.

---

## 5. Ground rules

1. **No names attached to the gaps.** The evidence is dates and documents. Anyone who turns this
   into an individual's failure gets stopped by the chair — the sequencing was a collective
   decision and the repair is a collective one.
2. **Disagree with the evidence, not with the discomfort.** If a finding is factually wrong, say
   which line and what the correct fact is; it will be changed in the document.
3. **Silence is not consent.** Every mandatory role gives an explicit verdict in
   [05](./05-STAKEHOLDER-REVIEW-SHEET.md). "No objection" is a verdict; being quiet is not.
4. **Objections need an alternative.** "This won't work" is incomplete without "instead, do X".
5. **Nothing is decided outside this room and then announced in it.** If a decision has already
   been taken elsewhere, table it at item 1.
6. **Hard stop at 120 minutes.** Anything unresolved becomes an open item with an owner and a
   date — not an extension.

---

## 6. Outputs — the session is not finished until these exist

- [ ] Nine decisions in §4, each with a verdict, an owner and a date
- [ ] Eight sign-offs in [05-STAKEHOLDER-REVIEW-SHEET.md](./05-STAKEHOLDER-REVIEW-SHEET.md)
- [ ] `CR-002` moved from `PENDING` to `APPROVED` / `REJECTED` / `DEFERRED` in the
      [decision register](../../../governance/registers/DECISION-REGISTER.md)
- [ ] Sprint 1 backlog committed for both tracks
- [ ] Executive Sponsor named (`GAP-010`) or an owner and date for naming them
- [ ] Open items list with owners and dates
- [ ] Steering committee update drafted — one page, from [01 §10](./01-PROCESS-GAP-ANALYSIS.md#10-conclusion)

---

## 7. Immediately after the session (Delivery Lead, within 2 working days)

Only if `CR-002` is approved. All eight items are **human-only actions** — an agent may raise a
change request, never approve or apply one ([14 §3, Rule CC-1](../../../governance/14-CHANGE_CONTROL.md#3-procedure)).

Execute [02 §9](./02-REALIGNMENT-PROPOSAL.md#9-what-changes-in-the-repository-if-cr-002-is-approved)
items 1–8, then:

```bash
java scripts/governance/FreshnessCheck.java   # must exit 0 after the state edits
```

and confirm in the next stand-up that the board reflects the new flow structure.

---

## 8. Open items

| # | Item | Owner | Due | Status |
|:-:|------|-------|-----|--------|
| | | | | |
