# 00 — How To Use This Bible

**Purpose:** get any reader — human or agent — from "I have no context" to "I know exactly what
I am supposed to do and how I will know it is done" in under five minutes.

---

## 1. The five-minute orientation

Answer four questions in order. Each has exactly one authoritative source.

### Q1 — "Where are we?"

```bash
cat docs/governance/state/CURRENT-STATE.yaml   # the machine truth
```

Read `workstreams[].lifecycle.current_phase` for your workstream. Then check the position banner
in [`README.md`](./README.md) for the S-stage rendering.

**If `state_as_of` is older than `review_due`, stop.** AIGEM Rule CS-1 applies: no context, no
verdict. Report the staleness rather than guessing.

### Q2 — "Which stage does my work belong to?"

Use this decision path:

```
Is it about whether to fund or justify the product?          → S00
Is it about what problem exists, or who has it?              → S01
Is it about a legal, regulatory or risk obligation?          → S02
Is it about defining required behaviour precisely?           → S03
Is it about what ships in which release?                     → S04
Is it about what a human sees or does?                       → S05
Is it about domain concepts, their state or their rules?     → S06
Is it about component structure, contracts or NFR targets?   → S07
Is it about building, testing or proving code?               → S08
Is it about running, deploying, observing or recovering?     → S09
Is it about talking to an external system?                   → S10
Is it about making ONE journey work end to end?              → S11
Is it about proving correctness, safety or compliance?       → S12
Is it about generalising to more journeys, LOBs or channels? → S13
Is it about launching?                                       → S14
Is it about operating what is launched?                      → S15
```

If the answer is "two of these", the work item is too big — split it, one item per stage.

### Q3 — "What am I supposed to do?"

Open `stages/Sxx-*.md`. It has one shape every time:

1. **Entry criteria** — is this stage even allowed to start?
2. **Epics** — the 4–8 outcomes this stage must produce, each with a named owner persona
3. **Stories** — under each epic, with acceptance criteria
4. **Validation tests (VT)** — how the stage proves itself
5. **Exit gate** — the criteria, the evidence artefact for each, and the approvers
6. **Current position** — what this repository has actually done in this stage
7. **Premature here** — what to park

Find your persona in the epic ownership column. Those are your epics.

### Q4 — "When am I done?"

The *Exit gate* section. Each criterion names an **evidence artefact**. You are done when the
artefact exists and the named approvers have signed — not when the work feels finished.

> **The evidence rule, stated once:** a criterion about system behaviour is never closed by a
> document asserting that behaviour. It is closed by a CI run, a test report, a scan output, a
> restore test, a dashboard, or a signed review of one of those.

---

## 2. Doing work inside a stage

```
1. Triage the item through AIGEM first          → docs/governance/README.md
   (stage fit, scope fit, necessity, priority)
2. If ADMITTED, locate it in this bible          → its stage, epic and story
3. Write or update the story's acceptance criteria so they are observable
4. Build. Produce the evidence as you go, not afterwards
5. Run the seven-board review at the tier the change warrants  → 11-REVIEW_GATES
6. Attach evidence to the gate criterion it serves
```

Step 1 is not optional and this bible does not replace it. An item that fails AIGEM triage does
not get in through a stage file.

---

## 3. Proposing a stage transition

Only humans may declare a transition ([04-STAGE_GATES §5](../governance/04-STAGE_GATES.md)). The
procedure:

```
1. Delivery Lead (Kalpana) marks the gate CANDIDATE
2. For each exit criterion, attach the evidence artefact — link it, do not describe it
3. Required approvers per the stage file review and sign
4. Any REWORK → gate returns to OPEN with named blocking items
5. All APPROVE → gate PASSED; Architect + PO update CURRENT-STATE.yaml
6. Run the AIGEM unpark sweep — parked items are RE-TRIAGED, never auto-admitted
7. Record the transition in registers/DECISION-REGISTER.md
```

An agent that believes a gate is complete **produces the evidence table and says so**. It does
not edit `current_phase`.

---

## 4. When reality and this bible disagree

It will happen. The rule is:

| Situation | Do this |
|---|---|
| A stage is missing an epic the work clearly needs | Add it via CR against this bible; do not do the work unlabelled |
| A gate criterion cannot be evidenced as written | Re-state the criterion via CR. Do **not** close it with a weaker artefact |
| AIGEM and this bible conflict | AIGEM wins. Raise a CR against this bible |
| The position banner is wrong | `CURRENT-STATE.yaml` is the truth; fix the banner |
| A story was dropped | Mark it `WITHDRAWN` with a reason. Never delete, never renumber |

---

## 5. Anti-patterns this bible exists to prevent

Each of these has already happened in this repository at least once.

| Anti-pattern | What it looks like | The rule that stops it |
|---|---|---|
| **Documentation as delivery** | A 250-file docs tree read as programme maturity while `services/` tells a different story | Rule SM-2: gate criteria cite Evidence-class artefacts |
| **Governing the supplier, not the product** | The integration adapter becomes the de-facto programme | WS-3 registration; the platform is the primary workstream |
| **Assertion as evidence** | "Tests pass" with no CI run to point at | Rule GS-1 in [`04-GATE-AND-SIGNOFF-MODEL`](./04-GATE-AND-SIGNOFF-MODEL.md) |
| **Freeze labels that freeze nothing** | P0 "build freeze" gaps open while building continues | Rule SM-4: an open P0 business gap blocks S11 entry |
| **Breadth before depth** | Expanding to more LOBs before one journey works end to end | S13 entry requires S12 passed |
| **Skipping a stage because it is late** | "We are past foundation, no point now" | Rule SM-3: stages may be entered late, never skipped |

---

## 6. Rules for AI agents

Agents work in this repository constantly. Specific obligations:

1. **Read `docs/governance/state/CURRENT-STATE.yaml` before anything else.** Every classification
   depends on it. If it is stale or missing, halt and report.
2. **Triage through AIGEM before consulting this bible.** This bible tells you what a stage
   requires; AIGEM tells you whether your item is admissible at all.
3. **Never edit `current_phase`, `stage_status`, or a gate's `PASSED` state.** Human only, no
   exceptions.
4. **Never mark a gate criterion closed without linking its evidence artefact.** An `APPROVED`
   with an empty `evidence[]` is recorded as `NOT_RUN` (AIGEM Rule RG-3).
5. **Do not treat this bible as authority in a triage record** until CR-010 is ratified. Cite
   AIGEM.
6. **When simulating a review board, load that board's persona** and answer only that board's
   checklist. Do not blend boards — the value is in the different questions.
7. **You cannot satisfy a T4 human sign-off.** Security and Risk & Compliance at T4 require a
   human signature, and no aggregate overrides it.

---

## 7. Maintenance

| Trigger | Action | Owner |
|---|---|---|
| Stage gate passed | Update the position banner in README; update `CURRENT-STATE.yaml` | Kalpana → Mahesh + Rajal ratify |
| A stage's epics change materially | CR against this bible; regenerate `backlog/jira-import.csv` | Rajal |
| AIGEM changes in a way that affects mapping | Re-check [`02-STAGE-MODEL §1`](./02-STAGE-MODEL.md); CR if the mapping breaks | Mahesh |
| Quarterly | Review whether any stage's exit criteria have become unevidenceable | Swapnali + Kalpana |

**Staleness limit:** the position banner must never be more than one gate transition behind
`CURRENT-STATE.yaml`. Past that it does not merely age — it misleads, because people trust it.
