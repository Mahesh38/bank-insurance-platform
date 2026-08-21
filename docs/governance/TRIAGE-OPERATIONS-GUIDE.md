# Triage Operations Guide — what happens *after* the verdict

What every role does once a triage verdict lands: ADMIT, PARK, REJECT, ESCALATE or ADMIT-BYPASS — who acts, what they must write, and what closes the loop.

**Layer:** L2 — operating guide, derived
**Status:** Explanatory. **Binding on nobody.**
**Custodian:** Delivery Lead (Kalpana / R12), with the Architect (Mahesh / R2) as framework custodian
**Audience:** every human and every AI agent who receives, raises, reviews, parks, unparks, rejects,
escalates or overrides work in this repository

---

> **Read this first.**
>
> This document **adds no rule**. Every rule here already exists in the numbered governance files.
> Where this guide and a numbered file disagree, **the numbered file wins**, always. What this
> guide does is answer the question the numbered files scatter across eleven documents:
>
> *"The triage produced a verdict. Now what? Who does what, when, and how do I know it's finished?"*
>
> [`RUNBOOK.md`](./RUNBOOK.md) tells you the **cadence** — what happens weekly, at a gate, monthly.
> [`09-AI_EXECUTION_RULES.md`](./09-AI_EXECUTION_RULES.md) tells an **agent** what it may and may
> not do. Neither walks a person from *"someone had an idea"* to *"the loop is closed."*
> That walk is this document.

---

## Contents

| Part | What it answers |
|---|---|
| [0. How to use this guide](#part-0--how-to-use-this-guide) | The 30-second map; what this is not |
| [1. Before the verdict](#part-1--before-the-verdict-intake-and-triage) | Who receives an input, what they owe, what a valid triage record is |
| [2. The six verdicts at a glance](#part-2--the-six-verdicts-at-a-glance) | One table: verdict → bucket → owner → next action → closure |
| [3. ADMIT — the full track](#part-3--admit--the-full-track) | Twelve steps from verdict to Done, each with an owner |
| [4. PARK — the discipline that makes parking honest](#part-4--park--the-discipline-that-makes-parking-honest) | Fields, sweeps, unparking, re-park limits, pulling forward |
| [5. REJECT — permanent, not eternal](#part-5--reject--permanent-not-eternal) | What a rejection must contain; how it is reversed |
| [6. ESCALATE — change control end to end](#part-6--escalate--change-control-end-to-end) | The CR lifecycle, approvers, and the five follow-through actions |
| [7. ADMIT-BYPASS — when a human overrides the process](#part-7--admit-bypass--when-a-human-overrides-the-process) | Who may invoke it, what it never bypasses, how it is closed out |
| [8. The P1 interrupt path](#part-8--the-p1-interrupt-path) | The only route that skips the matrix |
| [9. Role playbooks](#part-9--role-playbooks) | One card per role: "when triage produces X, you do Y" |
| [10. Use cases, worked end to end](#part-10--use-cases-worked-end-to-end) | Twenty concrete scenarios with the actual actions |
| [11. Quick reference](#part-11--quick-reference) | Clocks, artefacts, state machine, authority limits, warning metrics |
| [12. Copy-paste forms](#part-12--copy-paste-forms) | Every record shape you need, ready to fill |
| [Glossary](#glossary) | Every code and abbreviation in one place |

---

# Part 0 — How to use this guide

## 0.1 The 30-second map

```text
                          SOMETHING ARRIVES
              (idea · bug · review comment · scan finding ·
               requirement · regulation · "can we just…")
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │  TRIAGE  (steps 0–5)   │  2–10 minutes. Always run.
                    │  Who: whoever received │  Output: SUG-<date>-<xxx>
                    └────────────┬───────────┘
                                 │
        ┌──────────────┬─────────┼─────────┬──────────────┬───────────────┐
        ▼              ▼         ▼         ▼              ▼               ▼
     ADMIT          PARK      REJECT   ESCALATE     ADMIT-BYPASS      DUPLICATE
   (current       (future    (never)   (CR-###)     (human said       (link it,
     stage)        stage)                            skip it)          count it)
        │              │         │         │              │               │
        ▼              ▼         ▼         ▼              ▼               ▼
   Part 3         Part 4     Part 5     Part 6         Part 7      §1.6 / Part 5
```

Everything in the diagram above has an **owner**, a **register line**, and a **condition that
closes it**. Parts 3–7 give you all three for each branch.

## 0.2 What this guide is not

- It is **not** a new stage, board, template or required artefact. Adding one of those is `GOV`
  work and would need a change request ([`14 §1`](./14-CHANGE_CONTROL.md#1-what-needs-a-change-request)).
- It is **not** a substitute for reading the numbered file when you are about to *make* a decision.
  Use this to know *which* file, then read that file.
- It is **not** authority. Nothing here lets anyone approve anything they could not approve before.
  See [`PERSONA-AUTHORITY-MATRIX.md`](./PERSONA-AUTHORITY-MATRIX.md) and the
  [Authority Quick Card](../context/personas/AUTHORITY-QUICK-CARD.md).

## 0.3 The five sentences that carry the whole framework

1. **Every input ends somewhere** — admitted, parked, rejected, escalated or duplicated, never
   dropped ([`08 §1`](./08-BACKLOG_RULES.md#1-one-rule-above-all)).
2. **A suggestion is never implemented in the turn it is raised** ([`09 §7`](./09-AI_EXECUTION_RULES.md#7-prohibited-behaviours)).
3. **Stage before merit** — a MUST at the wrong stage is a PARK, not a P1 ([`00 §2`](./00-GOVERNANCE.md#2-principles)).
4. **Park with a target stage and an unpark trigger, or you have deleted the idea**
   ([`03 §3`](./03-LIFECYCLE.md#sf3-carries-three-mandatory-fields)).
5. **Done means proven, not finished** ([`13 §1`](./13-DEFINITION_OF_DONE.md#1-principle)).

---

# Part 1 — Before the verdict: intake and triage

You cannot act on a verdict you produced badly. This part is short because
[`09 §2`](./09-AI_EXECUTION_RULES.md#2-the-mandatory-sequence) already owns it — but the downstream
parts assume these fields exist, so they are summarised here.

## 1.1 What counts as an input

Anything that could become work:

| Source | Examples |
|---|---|
| A human asks | "Can we add X", "this is broken", "we should really…" |
| An agent notices | duplicated code, a missing test, a better structure, a risk |
| A tool reports | SAST/SCA finding, failing pipeline, coverage drop, secret scan hit |
| A reviewer comments | a PR review, a board's `should_fix` item |
| The outside world | an IRDAI circular, a partner API change, a bank security policy |
| A gate | an exit criterion that cannot be met as written |

**Not an input:** a question with no change implied. *"Answering is not implementing"*
([Rule AE-1](./09-AI_EXECUTION_RULES.md#1-when-this-applies)) — explaining, analysing and
recommending are free.

## 1.2 Who receives it, and what they owe

| Who received it | What they owe, immediately |
|---|---|
| **AI agent** | Run steps 0–5, write the `SUG-` row, say one line, **return to the work item it was doing** |
| **Any human** | Either triage it themselves, or hand it to whoever is running the lane — with the words, not a paraphrase |
| **Tech Lead** | For ADMIT: classify, score, map dependencies, route to a backlog (~15 min) |
| **Delivery Lead (R12)** | Nothing per input — but owns that the registers stay honest (§4.5, §11.5) |

Time budget: **2–10 minutes** per input ([`RUNBOOK §3`](./RUNBOOK.md#3-cadence-master-table)).
Triage latency target is **under one working day** ([`18 §2 Flow`](./18-GOVERNANCE_METRICS.md#flow)).

## 1.3 The five cheap steps

Steps 2–5 are cheap and always run. **Most inputs stop at step 5** and never reach the expensive half.

| Step | Question | File | Output |
|---|---|---|---|
| **0** | Where are we? Stage, objective, gate, scope, in-flight item | [`state/CURRENT-STATE.yaml`](./state/CURRENT-STATE.yaml), [`BOOT.md`](../context/BOOT.md) | context block |
| **1** | Is this new? | [`SUGGESTION-REGISTER.md`](./registers/SUGGESTION-REGISTER.md), [`TECH-DEBT.md`](../1sb-insurance-integration/service-ssot/TECH-DEBT.md) | `SUG-<YYYYMMDD>-<3 chars>` or a duplicate link |
| **2** | Does it fit the **stage**? | [`03 §3`](./03-LIFECYCLE.md#3-stage-fit-codes-sf) | `SF0`…`SF4` |
| **3** | Does it fit the **scope**? | [`02 §3`](./02-PROJECT_SCOPE.md#3-scope-fit-codes-l1--generic) | `SC0`…`SC4` |
| **4** | How **necessary** is it? | [`16 §2`](./16-DECISION_MODEL.md#2-necessity-levels) | MUST/SHOULD/COULD/NOT-NOW/REJECT + evidence tier + confidence |
| **5** | **Verdict** | [`00 §6`](./00-GOVERNANCE.md#6-the-action-matrix) | ADMIT · PARK · REJECT · ESCALATE |

### The scope filter runs *before* the matrix

| Scope fit | Effect — regardless of how necessary it is |
|---|---|
| `SC0` explicit in scope | Proceed to the matrix |
| `SC1` derived — an in-scope thing is *wrong* without it | Proceed; **name the beneficiary** in `serves` |
| `SC2` adjacent value | **Force PARK → Ideas.** Never ADMIT |
| `SC3` out of scope | **REJECT** |
| `SC4` externally mandated (law, regulator, security policy, contract) | **ESCALATE.** Never silently admitted *or* rejected |

### The action matrix

| | **MUST** | **SHOULD** | **COULD** | **NOT-NOW** |
|---|---|---|---|---|
| **SF0** prerequisite | ADMIT · P1 | ADMIT · P2 | ADMIT · P3 | *invalid* |
| **SF1** on-stage | ADMIT · P1–P2 | ADMIT · P2–P3 | ADMIT · P3 | PARK · P4 |
| **SF2** adjacent | ADMIT **if absorbable**, else PARK · ≤P3 | PARK · P4 | PARK · P5 | PARK · P4 |
| **SF3** premature | PARK · P4 | PARK · P4 | PARK · P5 | PARK · P5 |
| **SF4** stage-invalid | REJECT | REJECT | REJECT | REJECT |

**The SF2 absorption test** — admit only if **all four** hold: small (one story or less, fits
inside the current plan) · no new dependency · no new decision · gate-neutral. Fail one → PARK.
When in doubt, PARK: *parking costs a register line; wrongly absorbing costs a stage*
([`03 §3`](./03-LIFECYCLE.md#the-sf2-absorption-test)).

## 1.4 A triage record is invalid without these

| Condition | Mandatory fields | Enforced by |
|---|---|---|
| Always | `id`, `raised_at`, `raised_by`, verbatim `input`, context, SF, SC, necessity, `action`, `resumed` | [`schemas/triage-record.schema.json`](./schemas/triage-record.schema.json) |
| `SF2` | the four absorption-test answers | [`03 §3`](./03-LIFECYCLE.md#the-sf2-absorption-test) |
| `SF3` | `target_stage`, `unpark_trigger`, `future_necessity` | [`03 §3`](./03-LIFECYCLE.md#sf3-carries-three-mandatory-fields) |
| `SC1` | `serves: [work item IDs]`, `failure_without_it`, `minimal` | [Rule SC-1](./02-PROJECT_SCOPE.md#sc1-is-the-dangerous-one) |
| `NOT-NOW` | `future_necessity`, `target_stage`, `binds_when` | [`16 §2`](./16-DECISION_MODEL.md#not-now-carries-three-fields) |
| `REJECT` | a real argument **and** `reopen_if` | [`16 §2`](./16-DECISION_MODEL.md#reject-requires-an-argument) |
| `ADMIT` | type, risk tier, priority now **and** at target, dependencies | [`06`](./06-WORK_CLASSIFICATION.md), [`05`](./05-PRIORITY_MODEL.md), [`07`](./07-DEPENDENCY_MODEL.md) |
| `ADMIT-BYPASS` | who authorised, what was skipped, the risk in one sentence | [`09 §8`](./09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process) |

> **The single most common invalid record** is an `SF3` park with no `unpark_trigger`. It reads
> like scheduling and behaves like deletion.

## 1.5 The answer shape — use it verbatim

```text
SUG-20260821-k3p · "Use Redis for idempotency"

Stage:      Phase 4 (Hardening) — this belongs to Phase 5.4
Scope:      In scope (SC0), not in this increment
Necessity:  SHOULD now → MUST before scale-out
Verdict:    PARK → Phase 5, unparks at the Phase 4 gate
Priority:   P4 now · P2 at target
Recorded:   docs/governance/registers/PARKED-BACKLOG.md

Continuing with FUNC-011.
```

**The last line is not optional.** It is the mechanism that makes triage cheap instead of
distracting: the person raising the idea sees it captured, so nobody needs to act now to avoid
losing it.

## 1.6 Three things that stop triage before a verdict

| Situation | What happens |
|---|---|
| **The state file is stale** (past `review_due`, `FreshnessCheck` exit 2) | You may **park and reject**. You may **not admit new work** (Rule CS-1). Say so, and name **Kalpana / R12** as the person who refreshes it |
| **It is a duplicate** | Do **not** open a new row. Link the original and increment `recurrence_count`. At `recurrence_count ≥ 3` it is re-triaged as a matter of course — repeated independent rediscovery is evidence ([AS-5](./05-PRIORITY_MODEL.md#7-anti-starvation)) |
| **It is a hard P1 override** | It bypasses the matrix entirely. Go to [Part 8](#part-8--the-p1-interrupt-path) |

---

# Part 2 — The six verdicts at a glance

This is the table most people want. Everything after it is detail.

| Verdict | Means | Bucket | Recorded in | Who acts next | Their first action | Clock | Closed when |
|---|---|---|---|---|---|---|---|
| **ADMIT** | Work enters a backlog for the **current** stage | READY / BLOCKED | [Suggestion register](./registers/SUGGESTION-REGISTER.md) + the routed backlog | Agent + **Tech Lead** | Classify → score → dependencies → route (~15 min) | Enters the ordered queue | `SUG` = `CLOSED-DELIVERED`, DoD evidence attached |
| **PARK** | Real work, wrong stage | PARKED | [Parked backlog](./registers/PARKED-BACKLOG.md) §1/§2 | Agent writes it; **Delivery Lead** owns the sweep | Write `target_stage` + `unpark_trigger` + `future_necessity` (~2 min) | Swept at every gate, every approved scope change, and on AS-2/AS-3 | Promoted at its trigger, re-targeted with a reason, or closed `SUPERSEDED`/`WONT-DO` |
| **REJECT** | Will not be done | REJECTED | Suggestion register, closed | Nobody — the reason is the artefact | Write the argument **and** `reopen_if` | None | Permanent, unless reopened with **new evidence** (§5.4) |
| **ESCALATE** | Exceeds agent/team authority | ESCALATED | [`CR-###`](./change-requests/) | **PO + Architect** (+ Compliance if regulatory) | Raise the CR; run impact analysis; **implement nothing** | Same day for SC4 | CR decided; on APPROVED, the five follow-through actions in §6.4 |
| **ADMIT-BYPASS** | A human overrode the process | Same as ADMIT | Suggestion register, `ADMIT-BYPASS` | The human who authorised it, plus the executor | Do it; record who authorised, what was skipped, and the risk | Counted in the bypass-rate metric | The skipped controls are either run retrospectively or the residual risk is explicitly accepted |
| **DUPLICATE** | Already tracked | — | Link on the existing row | Whoever triaged | Link + `recurrence_count += 1` | — | Follows the original |

> **The verdict determines the bucket mechanically.** There is no discretionary step between
> "verdict" and "bucket" ([`08 §2`](./08-BACKLOG_RULES.md#2-the-six-buckets)). If you find yourself
> choosing, your verdict is wrong, not your bucket.

---

# Part 3 — ADMIT — the full track

ADMIT is the *most* expensive verdict, and the one people most often think means "start now."
It does not. **ADMIT means the item has earned a place in the queue for this stage.** Whether it
is *next* is decided at step 3.10, by the ordering rules — not by whoever is enthusiastic.

```text
ADMIT ─► classify ─► score ─► dependencies ─► break down ─► route
          │                                                    │
          ▼                                                    ▼
        plan (T2+) ─► boards ─► APPROVED ─► DoR ─► order ─► implement ─► DoD ─► close
                        │                                        ▲
                        └─ REWORK (max 2 rounds) ─────────────────┘
```

## 3.1 Classify — what kind of work is this?

| | |
|---|---|
| **Who** | Agent proposes, **Tech Lead** confirms |
| **Input** | The triage record |
| **Output** | `type` (one of 16), `also[]`, `risk_tier` T1–T4 |
| **File** | [`06-WORK_CLASSIFICATION.md`](./06-WORK_CLASSIFICATION.md) §2–§3 |
| **Done when** | Type and tier are on the record — because **the tier decides which boards are mandatory** |

**Type matters because it decides the destination** and the mandatory boards:

| Type | Goes to | Mandatory boards |
|---|---|---|
| `FUNC` | Owning workstream product backlog | Product, Technical, QA |
| `BUG` | Backlog (defects) | Technical, QA |
| `NFR` | Product backlog (NFR) | Technical, Architecture, Ops |
| `ARCH` | ADR + backlog | **Architecture**, Technical |
| `SEC` | Backlog + risk register | **Security**, Architecture |
| `COMP` | Backlog + risk register | **Risk & Compliance**, Security |
| `INFRA` / `OPS` | Backlog | Ops, Architecture / Ops |
| `DEBT` / `REFACTOR` | Debt ledger / backlog | Technical (+ QA for refactor) |
| `QA` | Test backlog | QA |
| `SPIKE` | Backlog | Technical + requester |
| `DOC` | Applied in place, recorded in the suggestion register | Author + one reviewer |
| `MIGRATION` | Backlog + ADR | Architecture, Technical, Ops, Risk |
| `GOV` | Change control **and** the owning backlog | Architecture, Product |
| `IDEA` | Parked backlog → Ideas | — |

**Risk tier** ([`10 §2`](./10-IMPLEMENTATION_PLAN_TEMPLATE.md#2-when-a-plan-is-required)):

| Tier | Examples | Plan | Boards |
|---|---|---|---|
| **T1** Trivial | Docs, comments, test-only additions, a config typo | Not required — the triage record is enough | Technical only |
| **T2** Standard | A story inside existing architecture; a bug with a known cause | **Short-form plan** | Technical, Product, QA (+ Arch/Sec/Risk/Ops *if* that impact ≠ none) |
| **T3** Significant | New component, new public API, new data structure, new dependency, security surface | **Full plan** | All seven |
| **T4** Critical | Regulatory, PII, money movement, authn/authz, migration, production topology | **Full plan + human sign-off** | All seven, **Security and Compliance must be HUMAN** |

**T4 is about what the change *does*, not what it is *near*** ([Rule RG-5](./11-REVIEW_GATES.md#automatic-t4-triggers--the-change-test)).
Renaming a variable inside the payment service is not T4. Changing who may call it is.
When genuinely ambiguous: **tier T3, record which trigger you considered and why it did not fire**,
and let Security or Compliance escalate it — that escalation is one board's single call and needs
no CR ([Rule RG-6](./11-REVIEW_GATES.md#automatic-t4-triggers--the-change-test)).

**Most common failure:** classifying "the design is wrong" as a `BUG`. A bug requires a *correct*
specification that the code violates ([Rule WC-1](./06-WORK_CLASSIFICATION.md#disambiguation-rules)).

## 3.2 Score — what priority, now and at target?

| | |
|---|---|
| **Who** | Agent computes, Tech Lead sanity-checks, **PO** breaks ties |
| **Output** | `priority_now`, `priority_at_target`, factors, score, caps applied |
| **File** | [`05-PRIORITY_MODEL.md`](./05-PRIORITY_MODEL.md) |

1. **Hard P1 overrides first** (§3 of that file) — if one applies with evidence, it is P1 and every
   cap is ignored.
2. Otherwise `SCORE = 2N + 2S + 2B + 2R + D − E`.
3. Then apply the caps — **stage fit dominates merit**:

| Cap | If | Then |
|---|---|---|
| PRI-2 | `SF3` premature | `priority_now` ≤ **P4** (MUST at target) or ≤ **P5** |
| PRI-3 | `SF2` adjacent | `priority_now` ≤ **P3** |
| PRI-5 | `SC2` adjacent value | `priority_now` = **P5**, bucket = Ideas |
| PRI-6 | `confidence < C3` | Cannot be P1/P2 **as implementation** — convert to a `SPIKE`, which itself may be P1/P2 |
| PRI-7 | A hard override | Ignores every cap above |

> **Both numbers, always.** `priority_now` **and** `priority_at_target`. The same item is P4 during
> hardening and P1 at production readiness — that is not indecision, that is the model working.

**Most common failure:** assigning P1 to something `SF3`. If it is genuinely urgent it is not
premature — go back and re-examine the stage fit.

## 3.3 Map dependencies

| | |
|---|---|
| **Who** | Agent + Tech Lead; **Delivery Lead** owns cross-item ageing |
| **Output** | edges, `state` (READY / BLOCKED / PARKED-DEPENDENT), `enablement_count`, `earliest_start` |
| **File** | [`07-DEPENDENCY_MODEL.md`](./07-DEPENDENCY_MODEL.md) |

Eleven edge types exist; the two people forget are:

- **`DECISION`** — *a decision is work*. If an item is blocked by an unmade decision, raise the ADR
  as its own item with its own priority. Never let a design question masquerade as an
  implementation task.
- **`EXTERNAL`** — a third party. Per DEP-3, **the chase is a separate item that is not itself
  blocked**. Somebody owns chasing the bank app team; that is not the same work as the story.

**Three landing states:**

| State | Meaning | What happens |
|---|---|---|
| `READY` | No unsatisfied HARD edge | Enters the ordered queue |
| `BLOCKED` | Approved, but a hard dependency is open | Records blocker ID + owner + follow-up date. **Does not consume a WIP slot.** Chasing the blocker is separate owned work |
| `PARKED-DEPENDENT` | Blocked by a **parked** item | **Inherits the park, not the priority** — it returns when its blocker does |

## 3.4 Break it down

| | |
|---|---|
| **Who** | Tech Lead |
| **Rule** | One owner + one acceptance outcome = a **story**. Two or more epic triggers = an **epic** |
| **File** | [`06 §5`](./06-WORK_CLASSIFICATION.md#5-work-breakdown--epic--story--task--spike) |

An epic must declare `completion_definition` **and** `not_included`. `not_included` is the field
that stops an epic from quietly absorbing every adjacent idea for the rest of its life.

If confidence is below `C3`, the first child is a **SPIKE**, not an implementation task
([Rule PRI-6](./05-PRIORITY_MODEL.md#5-caps--stage-fit-dominates)). A spike's Done is a *written
answer* plus triaged follow-on items — **its prototype code is not merged to main**
([`13 §3`](./13-DEFINITION_OF_DONE.md#3-additional-dod-by-work-type)).

## 3.5 Route it — AIGEM never keeps a parallel backlog

The item now lives in the **existing** backlog for its type (§3.1 table). Two links are mandatory
and are the whole of traceability:

- backlog entry carries `origin: SUG-<id>`
- suggestion register row carries `admitted_as: <work item id>`

Machine authority for routing is `state/CURRENT-STATE.yaml → routing[workstream][type]`.

## 3.6 Write the plan (T2 and above)

| | |
|---|---|
| **Who** | The implementing owner (human or agent) |
| **Output** | An implementation plan — short form at T2, full form at T3/T4 |
| **File** | [`10-IMPLEMENTATION_PLAN_TEMPLATE.md`](./10-IMPLEMENTATION_PLAN_TEMPLATE.md) |

> **Rule IP-1 — no plan, no code**, above T1. *A plan written after the code is not a plan, it is a
> summary; label it honestly and expect a Technical rework verdict.*

**`files_expected` and `out_of_scope` are the drift contract.** Write `out_of_scope` *before*
starting, and name in it the three most tempting adjacent changes — while your judgement is still
uncontaminated by momentum ([`17 §7`](./17-DRIFT_CONTROL.md#7-prevention)).

## 3.7 Run the boards

| | |
|---|---|
| **Who** | The mandatory boards for the tier (§3.1), **in role, one at a time** |
| **Output** | One verdict per board, each with non-empty `evidence[]` |
| **File** | [`11-REVIEW_GATES.md`](./11-REVIEW_GATES.md) |

| Board | Named persona | Asks |
|---|---|---|
| 1 Architecture | **Mahesh** (R2) | *Does this belong here, shaped like this?* |
| 2 Technical | **Amit** (R3) / Tech Lead | *Will this work, and can we live with it?* |
| 3 Product | **Rajal** (R1) | *Is this the thing we asked for — and only that?* |
| 4 Security | **Deepali** (R8) | *What does this expose, and what can be abused?* — **veto** |
| 5 QA | **Swapnali** (R7) | *How will we know it works — and know when it breaks?* |
| 6 Risk & Compliance | **Shailja S** (R9) | *Can we defend this to a regulator?* — **veto** |
| 7 Operations | **Shivanshi** (R10) | *Can we run, observe and recover this?* |

**Rules that decide whether a verdict counts:**

- **Rule RG-3 — no evidence, no verdict.** `APPROVED` with an empty `evidence[]` is recorded as
  `NOT_RUN`. Applies to humans and agents alike.
- An **agent** may simulate a board: `reviewer_type: AGENT`. It fully satisfies T1–T2,
  provisionally T3, and **never** the mandatory human sign-offs at T4.
- An agent that **authored** the plan and reviews it must mark `self_review: true`. A self-reviewed
  T3 plan needs at least one human board.
- **Security and Risk & Compliance at T4 must be `HUMAN`.** No aggregate override, no majority.
- A solo agent simulating seven boards reviews **sequentially, in role**, and does not carry the
  previous board's conclusion into the next. The value is in the different questions.

## 3.8 Handle what the boards return

| Returned | What it means | What happens next | Who |
|---|---|---|---|
| `APPROVED` | No objections | Counts toward the gate | — |
| `APPROVED_WITH_CONDITIONS` | Fine, provided X | **The conditions become acceptance criteria** and are verified at DoD. They are not advice | Plan author appends them |
| `REWORK` | Must change before approval | Plan returns to the author with the **union of every board's `must_fix[]`**; re-review by the objecting boards only | Author |
| `REJECTED` | Wrong *in kind*, not in detail | **Back to pipeline step 2** — stage, scope or necessity was probably misjudged | Whoever triaged |
| `NOT_APPLICABLE` | This board has no interest | Recorded with a one-line reason | Board |
| *(silence)* | Nothing | **`NO_RESPONSE`** — see below | Delivery Lead |

**The gate is APPROVED only when** every mandatory board returned APPROVED / APPROVED_WITH_CONDITIONS
(or a justified NOT_APPLICABLE), **no** board returned REWORK or REJECTED, **every T4 human
sign-off is present**, and **all conditions are recorded as acceptance criteria**.

**Vetoes and conflicts:**

| Situation | Resolution |
|---|---|
| Security or Compliance says REWORK/REJECTED | **Binding.** No aggregate or majority override, ever |
| Architecture says REWORK | Overridable only by a recorded ADR signed by a human architect — and never over a separate binding Security/Compliance conclusion |
| Product says REWORK | Behaviour/scope/acceptance is corrected, or consciously changed by the authorised Product owner. Engineering cannot silently override it |
| Two boards conflict | Use the relevant shared protocol; name the **one** owning authority per decision; **no majority voting**. Unresolved after one substantive alternatives cycle → accountable humans |

**Silence never approves** ([Rule RG-7](./11-REVIEW_GATES.md#121-board-response-clock)):

| Tier | Board must respond within | On expiry |
|---|---|---|
| T1–T2 | 1 working day | Escalate to the board's named persona |
| T3 | 2 working days | Escalate to the named persona, notify R12 |
| T4 | 3 working days | Escalate to the accountable human owner(s), notify R12 |

`NO_RESPONSE` is recorded **against the board**, never counts toward the gate, and never satisfies a
T4 human sign-off. Its purpose is to convert an invisible permanent stop into a visible assignable
one. Repeated `NO_RESPONSE` is a **staffing signal, not a discipline one** — the board is unstaffed,
over-triggered, or reviewing work it has no interest in. Fix the cause; do not shorten the window.

**The rework loop has a hard limit:**

```text
Round 1  REWORK → revise → re-review by the objecting boards only
Round 2  REWORK → revise → re-review
Round 3  ── not permitted ──►  ESCALATE to Product + Architecture (+ the binding domain owner)
```

A third round means **the problem is misunderstood, not the plan**. The item usually needs
splitting, a spike, or re-triage.

## 3.9 Definition of Ready

Approved is not Ready. An item is READY only when **all fifteen** of
[`12 §2`](./12-DEFINITION_OF_READY.md#2-the-checklist) hold. The ones that actually fail in practice:

| # | Criterion | Typical failure |
|---|---|---|
| R3 | Stage fit is SF0/SF1 (or an absorbed SF2) **against the current stage** | The stage moved under it |
| R6 | `priority_now` computed against the **current** stage | Stale priority after a stage change |
| R7 | AC written, observable, testable | "Improve performance" |
| R8 | No unsatisfied HARD edge | The blocker was never actually closed |
| R10 | Plan exists **and is approved** (T2+) | Approved 40 days ago — expired (§3.9.1) |
| R12 | Test approach agreed; test data available and **PII-free** | "Ready except for the tests" — that is not ready |
| R15 | `out_of_scope` stated | The item has no boundary |

**AC quality bar:** observable · binary · behavioural · bounded (happy path + at least one error
path + stated edges) · independent. The pattern worth copying is the **negative, verifiable
compliance criterion**:

```text
AC-3  Given any payment response,
      When logs and audit events are inspected,
      Then paymentUrl appears in neither.
```

### 3.9.1 Approvals expire

> **Rule RG-8.** An approval expires at **30 calendar days**, or sooner if the context that
> justified it changed: a stage transition, an approved CR touching the plan's scope, a change to a
> standing constraint, or a material plan edit.

Elapsed time inside one long-running stage is **not** by itself expiry. Re-review after expiry is
scoped to the boards whose **inputs** changed; a board whose jurisdiction did not move re-affirms
with a one-line evidence entry.

> **Rule DR-1 — READY expires at a stage boundary.** When the stage changes, every READY item is
> re-checked against R3 and R6.

## 3.10 Order — is it actually next?

Order is **computed, not chosen** ([`07 §5`](./07-DEPENDENCY_MODEL.md#5-execution-ordering)):

```text
1. Build the graph from blocked_by + requires + decision edges
2. Topologically sort
3. Take the READY set (all predecessors Done)
4. Sort by:  a. priority_now   b. score   c. enablement count (desc)
             d. effort (asc)   e. raised_at (asc — anti-starvation)
5. Take the head. That is the next item.
6. On completion, recompute — never reuse a stale queue.
```

**Rule AE-2 — one `IN-FLIGHT` item per executor.** This is a per-owner WIP limit, not a repository
mutex: independent, dependency-safe lanes may run in parallel. When an item becomes BLOCKED,
snapshot it with blocker + owner + follow-up date, move it out of IN-FLIGHT, and take the next
eligible READY item.

Before the first line of code, the agent pre-flight ([`12 §6`](./12-DEFINITION_OF_READY.md#6-agent-pre-flight)):
can you state the item ID and type, the AC *from the item* rather than from memory of the
conversation, the plan and its verdicts, that it is head of the queue, the files you expect to
touch, and what evidence will prove it Done? Any unchecked box → stop and resolve it.

## 3.11 Implement — and watch for drift

Fourteen drift signals are checked continuously ([`17 §2`](./17-DRIFT_CONTROL.md#2-drift-signals)).
The critical ones: touching anything in `out_of_scope` (**D4**), changing a public contract or
error code not in the plan (**D5**), and changing tests to make them pass rather than to reflect
new behaviour (**D14**). **D12 — the phrase "while I'm here" appearing in your own reasoning — is a
hard stop**, and it is not a joke: that thought reliably precedes the worst drift.

When a signal fires, **stop and classify**:

| Classification | Response |
|---|---|
| **NECESSARY-INSIDE** — needed for an AC, inside approved components | Log a variance. Continue |
| **NECESSARY-OUTSIDE** — the item cannot complete without crossing a boundary | **Re-review by the affected boards before continuing.** If review is unavailable now: revert the excursion, mark the item BLOCKED, name the blocker. Never "proceed and ask later" |
| **ADJACENT-VALUE** — genuinely useful, not required here | **Revert it.** Write a `SUG-`. Continue the original item |
| **INCIDENTAL** — formatting, tidy-up, opportunistic rename | **Revert.** No register entry unless it recurs |

> **Rule DC-1 — when in doubt, revert and register.**
> **Rule DC-2 — drift is never resolved by widening the plan after the fact.** Editing
> `files_expected` to match what you already changed is not a variance log; it is a cover-up, and it
> destroys the only measurement of plan accuracy the model has.

**If you notice drift late:** stop · snapshot honestly · separate (keep what serves the AC, extract
or revert the rest — `git stash`, a scratch branch or a patch file all work; **losing the work is
not required, removing it from *this* item is**) · register each extracted piece · re-anchor by
re-reading the objective out loud · resume from the last completed plan step · **report it in one
line**. Reporting drift is correct behaviour and is not punished. Quietly shipping a 400-line diff
for a 40-line story is not.

**Run the pre-PR drift check** before opening a PR ([`17 §5`](./17-DRIFT_CONTROL.md#5-pre-pr-drift-check)).

## 3.12 Done — and close the loop

**Done means proven, not finished.** Every criterion is satisfied by an *artefact*.

The twelve universal criteria are in [`13 §2`](./13-DEFINITION_OF_DONE.md#2-universal-dod-every-work-item);
work-type additions are in §3 of that file (a `BUG` needs a **regression test that fails before the
fix**; an `NFR` needs its target **measured**, not asserted; a `SPIKE` needs a written answer and
un-merged prototype).

**The step teams skip is governance closure** — and it is exactly why the same suggestion arrives
four times:

```text
[ ] Origin SUG-#### marked CLOSED-DELIVERED, linked to the PR
[ ] Parked items this change unblocked have been re-triaged
[ ] Dependency edges this change satisfied are marked resolved (DEP-###)
[ ] New risks discovered are in the risk register (RISK-###)
[ ] Assumptions validated or invalidated (ASM-###)
[ ] Suggestions raised during implementation are registered
[ ] Gate criteria this change advances are updated in 04-STAGE_GATES.md
[ ] Metrics inputs recorded: rework rounds, drift incidents, plan variances
```

**Who declares Done:** Tech Lead for `FUNC` (after QA sign-off) and `BUG`; Tech Lead + the relevant
board reviewer for `SEC`/`COMP`; QA Lead for `QA`; Architect + PO for `GOV`.
**An AI agent never declares Done unilaterally** — it produces the evidence table and requests
closure. *An honest partial report is worth more than a premature Done.*

**Partial outcomes** ([`13 §6`](./13-DEFINITION_OF_DONE.md#6-partial-and-blocked-completion)):

| Situation | Correct handling |
|---|---|
| Some AC pass, others blocked externally | Item stays open; split the passing part out only if independently shippable |
| Implementation complete, QA cycle not run | **Not Done.** Record the variance explicitly — never silently |
| Done except a follow-up improvement | **Done.** The improvement is a new `SUG-` |
| Done but a debt item was created | **Done**, provided the debt is registered with owner, severity and expiry |
| Gate criterion met but evidence not captured | **Not Done.** The evidence *is* the criterion |

## 3.13 Three things ADMIT does *not* mean

1. **It does not mean "start now."** Order decides that (§3.10).
2. **It does not mean "approved."** Boards decide that (§3.7).
3. **It does not mean the scope is settled.** If something in `out_of_scope` turns out to be
   required — **stop**. New work item, fresh triage. That is the single most common creep vector
   ([`14 §4`](./14-CHANGE_CONTROL.md#4-changing-an-approved-plan)).

---

# Part 4 — PARK — the discipline that makes parking honest

Parking is the verdict the framework exists for, and the one most easily corrupted. A park is a
**promise to come back**. Without the mechanics below it is a polite rejection with extra steps —
and the metrics will say so (§4.11).

## 4.1 What a park is, and is not

| A park **is** | A park **is not** |
|---|---|
| Real work, at the wrong stage | A soft no |
| Scheduled — it has a stage and a trigger | "Someday" |
| Permanent in the register — never deleted | Deleted, ever, without a recorded rejection reason |
| Re-triaged from scratch when it returns | Auto-admitted at its trigger |
| Off-limits to re-propose while parked | Forgotten |

## 4.2 The moment of parking — what must be written

The row goes in [`registers/PARKED-BACKLOG.md`](./registers/PARKED-BACKLOG.md), and it is invalid
without all of:

| Field | Example | Why |
|---|---|---|
| `target_stage` | `Phase 5.4` | Where it becomes on-stage |
| `unpark_trigger` | `"Phase 4 gate PASSED"` / `"before horizontal scale-out"` | The **observable** event. Not a date, not a mood |
| `future_necessity` | `MUST` | What it will be worth then — this drives the cap and the promotion argument |
| `priority now / at target` | `P4 / P2` | Both, always |
| `parked because` | "In-memory is correct for single-instance UAT" | The argument a future reader needs |

Two sections exist, and the difference matters:

- **§1 Parked — scheduled work.** Real items with triggers. These return.
- **§2 Parked — stage-deferred by nature.** Work every platform needs, deliberately scheduled to
  production readiness (dashboards, DR testing, retention jobs, prod credentials). **Listed so
  agents recognise them as already decided, not as gaps to re-report.**
- **§3 Ideas.** `SC2` — outside scope, plausible value, nothing depends on them. Reviewed at gate
  sweeps; closed as `LAPSED` after three gates.

Time cost of a park: **about two minutes** ([`RUNBOOK §3`](./RUNBOOK.md#3-cadence-master-table)).

## 4.3 What each role does the moment something is parked

| Role | Action |
|---|---|
| **Whoever triaged** (usually the agent) | Write the row with all five fields; say the one-line acknowledgement; **return to the work item you were on, by name** |
| **The person who raised it** | Nothing is required. If you disagree with the target stage, that is a stage-fit dispute → **Architect (Mahesh), same day** |
| **Tech Lead** | Reconcile against the debt ledger — **no item may be in both** the parked backlog and `TECH-DEBT.md` |
| **Delivery Lead (R12)** | Own the sweep calendar. Parking creates a future obligation and R12 holds it |
| **Everyone else** | **Do not re-propose it, and do not re-report it as a gap.** Section 5 of [`BOOT.md`](../context/BOOT.md) and §2 of the parked backlog exist precisely so this noise stops |

## 4.4 Living with a parked item

The two behaviours that keep the register useful:

1. **Do not re-propose parked items.** Check the parked backlog at session start. A re-proposal is
   a `DUPLICATE` with `recurrence_count += 1`, not a new row.
2. **Do not re-report known debt.** `TD-006`, `TD-007`, `TD-009`, `TD-010`, `TD-014`, `TD-022`,
   `TD-023`, `QA-001` are known. *Re-reporting them is noise, and noise is how real findings get
   ignored.*

Note the exception that proves the rule: `recurrence_count ≥ 3` from **independent** sources is
treated as **evidence** and forces re-evaluation (AS-5 adds `+1` to the R factor on next scoring).
So repeated rediscovery is not ignored — it is metered.

## 4.5 The sweep — how parked work actually comes back

> Parked work returns through a **defined sweep**, never through someone remembering.

**Five triggers:**

| # | Trigger | Who runs it |
|---|---|---|
| 1 | A stage gate is `PASSED` — sweep every item whose `unpark_trigger` names that transition | Delivery Lead, at the Gate Review |
| 2 | A **scope change is approved** by CR — sweep items rejected or parked on scope grounds | Delivery Lead + PO |
| 3 | `recurrence_count` reaches 3 — the idea keeps arriving | Whoever triaged the third occurrence |
| 4 | Aging rules **AS-2 / AS-3** fire | Delivery Lead |
| 5 | A dependency that caused `PARKED-DEPENDENT` becomes Done | The owner who closed the dependency (a DoD closure step) |

**The procedure, per candidate item:**

```text
1. Re-run pipeline steps 2–7 against the NEW current state.
   Do NOT auto-admit. Six months of delivery may have solved it,
   obsoleted it, or turned it into a different problem.

2. Outcomes:
     still SF3     → re-park with a NEW target stage and a reason for the roll
     now SF0/SF1   → ADMIT: score fresh, plan, review  → Part 3
     now SF4/SC3   → close as SUPERSEDED or WONT-DO, with a reason

3. Record the sweep on the item: sweep date, previous target, new state.
   Record the pass in the sweep log at the bottom of PARKED-BACKLOG.md.
```

**Cadence** ([`RUNBOOK §3`](./RUNBOOK.md#3-cadence-master-table), [`08 §7`](./08-BACKLOG_RULES.md#7-hygiene)):

| When | What | Owner |
|---|---|---|
| Every stage gate | Unpark sweep · aging review · metrics snapshot | Delivery Lead |
| Every stage gate | Parked backlog reconciled with the debt ledger | Tech Lead |
| Every approved scope change | Sweep items whose block was scope | Delivery Lead + PO |
| Monthly | Duplicate merge pass on the suggestion register | Delivery Lead |
| Every stage change | **Priorities recomputed for READY *and* PARKED items** — not optional; priority is stage-relative, so a stage change silently invalidates every stored `priority_now` | PO + Tech Lead |

## 4.6 Unparking — what promotion actually costs

An unparked item is **not** an approved item. It re-enters at pipeline step 2 and walks the whole
of Part 3: classify → score → dependencies → break down → route → plan → boards → DoR → order.
Its old priority is discarded and recomputed against the *current* stage.

Worked, from the live register — **TD-014** (WireMock / full E2E for integration ↔ persistence):
parked at Phase 1, target Phase 4, trigger "overlaps gate criterion 4.1". The trigger **has fired**
and the row is flagged `⚠️ Now eligible`. The correct next action is not "someone should get to
that" — it is: **the next gate sweep promotes it into the Phase 4 backlog alongside criterion 4.1,
or re-parks it with a reason.** Those are the only two legal outcomes.

## 4.7 Re-parking has a limit

| Rule | Effect |
|---|---|
| **AS-1** | Every P4 item is re-triaged at its `unpark_trigger`, and **unconditionally at every gate sweep** |
| **AS-2** | A P4 item that has survived **two** gate transitions without being pulled forward is **force-reviewed by the PO**: promote, re-target, or close as `SUPERSEDED`/`WONT-DO` with a reason |
| **AS-3** | A P5 idea untouched for **three** gate transitions is closed as `LAPSED` — still readable, no longer in the queue |
| **AS-4** | Aging is reported in the metrics. **A growing P4 backlog is a scope signal, not a hygiene problem** |
| **08 §5** | An item re-parked **twice** is force-reviewed by the PO: either it is genuinely a later-stage must, or **it is an idea pretending to be scheduled work** |

## 4.8 Pulling a parked item forward early

You cannot simply decide to. **Pulling a parked item forward into an earlier stage requires a
`CR-###` approved by PO + Architect** ([`14 §1`](./14-CHANGE_CONTROL.md#1-what-needs-a-change-request)).

What does **not** need a CR: normal triage outcomes, priority recomputation at a stage change,
**re-parking with a new target**, adding clarifying acceptance criteria, recording a variance
inside `files_expected`.

## 4.9 `PARKED-DEPENDENT` — the inherited park

An item blocked by a parked item **inherits the park, not the priority**. It does not get its own
independent trigger; it returns when its blocker does. When the blocker is finally Done, the
`PARKED-DEPENDENT` items are swept — and that sweep is a **DoD closure obligation** of the item
that unblocked them (§3.12), not a favour.

## 4.10 The Ideas bucket

`SC2` — adjacent value, outside scope, nothing depends on it. **Never ADMIT, whatever the
necessity** (cap PRI-5 forces P5). Reviewed only at gate sweeps. Exits by: a scope change (CR), or
`LAPSED` after three gates. The bucket exists so a decent idea is **not re-litigated from scratch**
every quarter — its value is the record, not the queue position.

## 4.11 Is your parking honest? Four numbers say so

| Metric | Target | What a bad number means |
|---|---|---|
| **Park accuracy** — parked items later admitted at their target stage ÷ all unparked | **> 60%** | Low ⇒ **parking is being used as a polite rejection** |
| Premature-admission rate — admitted items later found to be SF3 | < 5% | This is the expensive error |
| Aging P4 backlog | Falling or flat | Growing ⇒ a **scope** conversation is overdue |
| **Incident preventability** — incidents traceable to a parked or rejected item | **0** | **Any occurrence forces a calibration review** |

---

# Part 5 — REJECT — permanent, not eternal

REJECT is the strongest verdict and needs the strongest justification. It is also the one most
often written lazily, which is what makes the same argument come back every quarter.

## 5.1 What a rejection must contain

| Field | Requirement |
|---|---|
| The argument | One of: contradicts a standing constraint · solves a problem the roadmap has decided not to have · superseded by a recorded decision (**cite the ADR**) · duplicates an existing item (**link it** — that is `DUPLICATE`, a softer close) |
| `reopen_if` | **Mandatory.** The observable condition that would make this a different question |
| Evidence tier | E1 (strongest) … E7 (weakest). A rejection resting on E7 is fragile and will be re-argued |

**Not valid grounds:** *"unnecessary complexity"* without saying what complexity and why it exceeds
the benefit · *"not how we do things"* without naming a principle · personal preference ·
*"I don't think we need it"*. If no stage claims it but the value is plausible, it is `SC2` → Ideas,
**not** `SF4` → REJECT.

> `reopen_if` converts **"no"** into **"not unless"** — which is both more honest and much less
> likely to be re-argued.

Example from the template:

```yaml
action:    REJECT
reason:    >
  No throughput requirement exists and none is measured. A reactive rewrite would
  change the concurrency model of delivered, approved Term-path code during hardening.
reopen_if: "a measured p95 or throughput target fails (E3 evidence)"
```

## 5.2 Who may reject, and what a rejection binds

Anyone running triage may record a REJECT — it is a *triage outcome*, not an approval. But:

- **`SC4` (externally mandated) may never be rejected by an agent or a team.** *"An agent that
  identifies an SC4 item writes the triage record, opens a `CR-###`, and stops. It does not
  implement, and it does not reject on the grounds that it's out of scope."*
- A rejected entry is **never deleted** ([`09 §7`](./09-AI_EXECUTION_RULES.md#7-prohibited-behaviours), prohibition 10).
- **Reversing a REJECTED decision requires a `CR-###`** approved by the original approver or the PO.

## 5.3 How a rejection is reopened

| Valid grounds | Not valid |
|---|---|
| Scope changed via an approved CR | "Asking again" |
| The stage advanced and it is now on-stage | "A different agent thinks so" |
| New regulatory or security obligation | "It's a best practice" |
| The original rejection rested on an assumption now proven false | Time passing |
| `recurrence_count ≥ 3` **with independent sources** | A single repeat |

**Procedure:** link back to the original `SUG-`, state the **new evidence**, raise the CR. The
history stays visible — *that is what stops the same argument being re-run every quarter.*

## 5.4 When a rejection turns out to be wrong

The most valuable feedback loop in the model: **after any incident, search the suggestion register
for items that would have prevented it — including rejected ones.**

> *A rejection that an incident proves wrong is not an embarrassment — it is calibration data.*

Owners: **Kalpana / R12 + Shivanshi / R10** run the calibration review within one week
([`RUNBOOK §9`](./RUNBOOK.md#9-escalation)). The outcome updates the rejection-reversal and
incident-preventability metrics, and — because `Incident preventability` has a target of **zero** —
any occurrence at all forces the review.

---

# Part 6 — ESCALATE — change control end to end

ESCALATE means: **this decision exceeds the authority of whoever is holding it.** The correct
behaviour is to *stop*, not to guess well.

## 6.1 What must be escalated

| Trigger | Approvers | Clock |
|---|---|---|
| `SC4` — externally mandated (law, regulator, security policy, contract, platform decision) | **PO + Compliance + Architect** | Same day |
| Add or remove something from project scope | PO + Architect | — |
| **Pull a parked item forward** into an earlier stage | PO + Architect | — |
| Change a stage's exit criteria | Architect + PO + affected boards | — |
| Declare a stage transition without meeting a criterion (**waiver**) | Architect + PO + Compliance if regulatory | Before `CANDIDATE` |
| Change a standing constraint | Architect (+ ADR) | — |
| **Reverse a REJECTED decision** | Original approver or PO | — |
| Change an approved plan materially | Affected boards only (lightweight CR) | — |
| Change any `docs/governance/**` file (`GOV`) | Architect + PO | — |
| Waive a review board for a tier that requires it | Architect — **never** for T4 Security/Compliance | — |
| Add, merge or retire a persona | Architect + PO + the affected board's owner | — |
| Plan reaches **rework round 3** | Architect + PO | Immediately |
| Boards conflict and stay conflicted | Architect + PO → recorded decision | 2 days |

## 6.2 The CR lifecycle

```text
1. Raise CR-###.  ── Implement NOTHING the CR contemplates. ──
2. Impact analysis: run pipeline steps 2–8 AS IF the change were approved,
   so approvers see the downstream consequences, not just the request.
3. Route to the approvers above. Regulatory drivers additionally route to Compliance.
4. Decision recorded in registers/DECISION-REGISTER.md.
5. APPROVED → the five follow-through actions (§6.4)
   REJECTED → §6.5
```

**The CR must carry a `driver`.** One of: regulatory/legal mandate · security finding · external
dependency change · validated assumption failure · business priority change · new evidence.
**"It would be better" is not a driver.**

It must also carry an `impact` block naming: scope, whether a gate date moves, which items become
blocked/unblocked/invalid, **which parked items this makes eligible**, effort, and
`risk_if_rejected` — plus `alternatives_considered`, including "do nothing" and its consequence.

> **Rule CC-1 — an agent may raise a CR; it may never approve one.** Not even its own, not even
> when the change seems obviously correct, not even under time pressure.

## 6.3 What the raiser does while the CR is pending

| Do | Do not |
|---|---|
| Complete the impact analysis so approvers can decide once | Start any part of the change |
| Return to your in-flight item and say so | Hold your lane idle waiting |
| Record the item as `ESCALATED` in the suggestion register | Re-raise it as a different suggestion |
| Chase the decision if it approaches a required-by date (that chase is R12's escalation duty) | Interpret silence as approval |

## 6.4 On APPROVED — the five actions that must follow

This is where change control most often stops half-done. **All five, every time**
([`14 §3`](./14-CHANGE_CONTROL.md#3-procedure)):

| # | Action | Owner |
|---|---|---|
| a | Update the affected document (`02` scope / `04` gates / `01` state / the governance file) | Custodian of that file |
| b | Update `state/CURRENT-STATE.yaml` | **Human** — agents never edit stage fields |
| c | **Run the unpark sweep** — scope changes routinely free parked work | Delivery Lead |
| d | **Recompute priorities** for READY and PARKED items | PO + Tech Lead |
| e | Create or re-triage the work items the CR enables | Agent + Tech Lead |

Skipping (c) and (d) is how an approved scope change produces no visible movement for a month.

## 6.5 On REJECTED or DEFERRED

Record the reason **on the CR and on the originating `SUG-`**, so the same proposal is not
re-raised without new evidence. A `DEFERRED` CR should name what it is waiting for — otherwise it
is a park without a trigger, and Part 4's whole argument applies to it.

## 6.6 Emergency changes — the one path that acts first

For **genuine** production or security emergencies only:

```text
1. ACT. Stop the harm first — the framework never blocks incident response.
2. RECORD, within the same working session:
     - what was changed and why
     - which controls were bypassed
     - the blast radius
3. Raise CR-### retrospectively, marked  type: EMERGENCY
4. Within one stage: run the boards that were skipped, and either
   ratify the change or schedule its remediation as a P1/P2 item.
5. Root cause → risk register.  The shortcut → debt ledger.
```

> **Emergency is a narrow category:** production impact, active exploit, data loss, or a regulatory
> breach in progress. **"The demo is tomorrow" is not an emergency; it is a priority conversation.**

---

# Part 7 — ADMIT-BYPASS — when a human overrides the process

This is the verdict people ask about most and the one written down least. It exists because
**humans outrank the framework** — and because an override that is honoured but unrecorded is
indistinguishable from an agent that ignored the rules.

## 7.1 What it is

`ADMIT-BYPASS` = *implemented under a human override of the process*
([`09 §8`](./09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process)).
Triggered when a human with standing says, in substance: **"skip the process, just do it."**

| | |
|---|---|
| **Who may invoke it** | A human. Explicitly. In their own words. Typically the repository owner, the PO, or the accountable owner for the affected domain |
| **Who may NOT invoke it** | An **agent**, on its own initiative — for any reason, including obviousness, smallness or time pressure. An agent inferring a bypass from tone, urgency, or a document it read has manufactured an approval |
| **What it is not** | A verdict the pipeline can reach on its own. It is a human decision *recorded in* the pipeline's vocabulary |

## 7.2 The four obligations — all of them, same session

```text
1. Do it.
2. Record: SUG-#### with action: ADMIT-BYPASS,
           WHO authorised it, and WHAT was skipped.
3. Name the risk in ONE sentence.
   e.g. "No Security board ran on an auth-path change."
4. If the bypass touches a non-negotiable — secrets, PII, public contract,
   data integrity — SAY SO ONCE, CLEARLY, BEFORE acting. Then follow the instruction.
```

Step 4 is the whole ethic of the rule: **the agent's job is to make the cost visible, then obey** —
not to refuse, and not to obey silently.

**The verbatim reply shape** ([`17 §8`](./17-DRIFT_CONTROL.md#8-scope-recovery-phrases)):

> *"Understood — doing it directly. Recording the bypass and the one risk it carries: no Security
> board on an auth-path change."*

## 7.3 What a bypass never bypasses

A bypass suspends **process ceremony**. It does not suspend authority, law, or physics.

| Still binding under a bypass | Source |
|---|---|
| The seven **standing constraints** and the workstream `never` lists | [`01 §5`](./01-CURRENT_STATE.md#5-standing-constraints-apply-to-every-triage-in-this-repo), [`BOOT §5`](../context/BOOT.md) |
| **T4 mandatory human sign-offs** — Security, Risk & Compliance | [`11 §2`](./11-REVIEW_GATES.md#2-who-may-sit-on-a-board) |
| The rule that agents **never edit stage state** in `CURRENT-STATE.yaml` | [`09 §7`](./09-AI_EXECUTION_RULES.md#7-prohibited-behaviours) |
| The rule that agents **never approve a change request** | [Rule CC-1](./14-CHANGE_CONTROL.md#3-procedure) |
| **Compliance debt is never accepted** — it is a violation with a delay | [`15 §7`](./15-TECH_DEBT_POLICY.md#7-debt-and-the-review-boards) |
| Regulatory obligations, consent, retention, audit, attribution | [`11 §9`](./11-REVIEW_GATES.md#9-board-6--risk--compliance) |
| **Nothing is Done without evidence** | [`13`](./13-DEFINITION_OF_DONE.md) |

A useful framing from the live register: a bypass of *documentation queue ordering* "does not
bypass any Architecture, Security, Compliance, QA, SRE, Database or mandatory-human decision."
**State the boundary of your bypass explicitly** — that sentence is what makes it auditable.

## 7.4 What happens *after* a bypass — the part everyone forgets

A bypass is not closed by shipping. It leaves two open obligations:

| Obligation | Who | When |
|---|---|---|
| **Run the skipped controls, or accept the residual risk explicitly.** If a board would have been mandatory for the tier, either run it retrospectively or have the accountable human record acceptance | The board owner, or the authorising human | Within the current stage — the same discipline as an emergency CR ([`14 §5`](./14-CHANGE_CONTROL.md#5-emergency-changes)) |
| **Feed the metric.** `ADMIT-BYPASS ÷ admitted items` = **bypass rate**, target **< 10%** | Delivery Lead, at the scorecard | Every gate |

**And read the metric correctly:**

> *A rising bypass rate is a **process signal, not a discipline problem**. It means the ceremony
> exceeds the value for that class of work. **Fix the process** — usually by lowering the tier —
> **do not exhort people.***

That sentence is the reason `ADMIT-BYPASS` is a first-class verdict instead of a shameful
exception. Bypasses are *data about the framework*.

## 7.5 Closing a bypass out — a checklist

```text
[ ] SUG-#### row exists with action: ADMIT-BYPASS
[ ] The authorising human is named (e.g. human:Mahesh), not "the user"
[ ] What was skipped is stated (which boards, which queue position, which plan)
[ ] The risk is stated in one sentence, and the boundary of the bypass is stated
[ ] If a non-negotiable was touched, the warning was given BEFORE acting
[ ] Ref column links to the artefact that was actually produced
[ ] Skipped mandatory boards: run retrospectively, or risk explicitly accepted by the owner
[ ] Any shortcut taken → TD-### with owner, severity, expiry
[ ] Counted in this gate's bypass rate
```

## 7.6 The honest caveat about capacity

Every bypass consumes the same capacity as governed work. The live register states this plainly on
its bypass rows — for example: *"this user-directed documentation work consumes repository capacity
that could otherwise advance the open `GATE-P4`… It does **not** change, waive or mark any gate
criterion complete."*

That is the right sentence to write. A bypass may skip ceremony; **it may never imply a gate moved.**

---

# Part 8 — The P1 interrupt path

This is the only route that skips the action matrix entirely
([`05 §3`](./05-PRIORITY_MODEL.md#3-hard-p1-overrides)).

## 8.1 The eight classes

| # | Override | Test |
|---|---|---|
| **O1** | Build / pipeline failure | The default or active branch does not build, or the gate job is red |
| **O2** | Security vulnerability | **Reachable and exploitable in code we ship** — not a theoretical CVE in an unused path |
| **O3** | Incorrect domain model | Implemented behaviour contradicts an approved state model, invariant or business rule |
| **O4** | Missing mandatory API | A contract the **current deliverable** promises is absent or non-conformant |
| **O5** | Regulatory violation | Breaches a stated regulatory, consent, retention, audit or attribution requirement |
| **O6** | Data corruption / loss | Any path that can corrupt, lose or leak persisted or in-flight data |
| **O7** | Blocking dependency | Directly blocks the item **currently in flight** |
| **O8** | Acceptance criteria failure | An item marked Done does not satisfy its AC |

## 8.2 The protocol

```text
1. State the override class AND its evidence, out loud, BEFORE switching.
2. Snapshot the in-flight item: what is done, what remains, where the code sits.
3. Handle the P1 through the pipeline — a P1 is FAST-TRACKED, NOT UN-GOVERNED.
   It still needs a plan and the boards mandatory for its tier.
4. Return to the snapshotted item. Say that you are returning.
```

Verbatim: *"Interrupting FUNC-011 for a P1 (O2: reachable vulnerability in the payment path).
Snapshot recorded; returning after."*

## 8.3 Two rules that keep the override honest

- **Evidence or it is downgraded.** An override claim must cite the specific evidence — the failing
  job, the CVE **and the reachable call path**, the approved model it contradicts, the AC ID. An
  unevidenced claim is downgraded to normal scoring and **logged as a false-P1**.
  *If everything is P1, the queue is unordered again.* Target false-P1 rate: **< 5%**.
- **O2 nuance.** A vulnerability that is real but **unreachable at the current stage** (e.g. a
  production-only component not yet deployed) is P1 *for its stage*: record it, **do not preempt**
  current work, and state the reachability finding explicitly.

## 8.4 What is *not* an interrupt

Not an elegant refactor. Not a missing test on unrelated code. Not a newer library version. Not
"while we're in here". Not a demo tomorrow.

For an agent's own mid-task discovery, the routing is
([`09 §5`](./09-AI_EXECUTION_RULES.md#5-handling-your-own-suggestions)):

```text
Is it O3/O7/O8 for the CURRENT item?  → It IS part of the current item. Handle it,
                                         note it in the plan's variance log.
Is it O1/O2/O5/O6 for the SYSTEM?     → Interrupt per §8.2.
Anything else?                        → SUG-####, one line to the user, RETURN.
```

---

# Part 9 — Role playbooks

One card each. *"Triage produced X — what do I do?"*

## 9.1 Anyone raising an idea (RM, engineer, stakeholder, executive)

| | |
|---|---|
| **Your job** | Say it plainly, once, and let it be recorded verbatim |
| **You will get back** | A six-line answer: stage, scope, necessity, verdict, priority, where it is recorded |
| **If ADMIT** | It is in the queue. **Not necessarily next** — order is computed |
| **If PARK** | Check the `target_stage` and `unpark_trigger` are ones you recognise. If not, that is a real objection — raise it now, not in three months |
| **If REJECT** | Read `reopen_if`. That is your route back, and it needs **new evidence** |
| **If ESCALATE** | You will be asked to help write the `driver` and `evidence` |
| **Disagree with the verdict?** | Necessity → **PO (Rajal)**, same day. Stage fit → **Architect (Mahesh)**, same day |
| **Want to override?** | Say so explicitly. You will get an `ADMIT-BYPASS` record and one sentence of risk — see [Part 7](#part-7--admit-bypass--when-a-human-overrides-the-process) |

## 9.2 The AI agent (R13)

| | |
|---|---|
| **Every session start** | `FreshnessCheck` → `BOOT.md` → parked backlog → known debt → name your one work item |
| **Every input** | Steps 0–5, register row, six-line answer, **return to the item by name** |
| **Never** | Implement in the turn a suggestion is raised · hold two in-flight items · park without a trigger · claim a P1 without evidence · edit stage fields · approve a CR · self-approve a board that requires a human · delete a parked or rejected entry · mark Done without evidence |
| **May** | Explain, analyse, recommend freely (Rule AE-1); simulate boards at T1–T2 fully and T3 provisionally, marked `reviewer_type: AGENT`; raise a CR; draft the reasoning and assemble evidence for a human sign-off |
| **Batching** | Collect suggestions during a long task and write them to the register in one pass at the end — but keep the running list visible and **never let it change what you build** |
| **Session end** | Item state recorded · every suggestion registered · drift recorded · evidence attached · registers updated · TODOs carry IDs · work committed or explicitly flagged |
| **Healthy signal** | **Zero registered suggestions in a session is a warning sign**, not a success — it means you are not looking, or you are quietly acting on what you see |

## 9.3 Tech Lead (R4) / Technical Head — Amit (R3)

| Verdict | Your action |
|---|---|
| **ADMIT** | Classify (type + tier), sanity-check the score, map dependencies, break it down, route it to the right backlog — ~15 minutes |
| **PARK** | Reconcile the parked backlog against the debt ledger: **no item in both** |
| Debt taken | `TD-###` with owner, severity, **expiry** — no expiry, no debt entry |
| Boards | Technical verdict on every tier, with evidence |
| Item finished | Verify the DoD evidence table; **you declare `FUNC` Done, after QA sign-off** |
| Nobody owns an artefact | Escalate to **Amit (R3)** immediately |

## 9.4 Product Owner — Rajal (R1)

| Trigger | Your action |
|---|---|
| Necessity disputed | You decide, same day |
| Priority tie | You break it |
| A P4 item survives **two gate transitions** | **Force review**: promote, re-target, or close with a reason (AS-2) |
| An item is **re-parked twice** | Force review: genuine later-stage must, or an idea pretending to be scheduled work |
| Scope change CR | You approve jointly with the Architect |
| `SC4` mandate | You + Compliance + Architect, same day |
| Reversing a rejection | You or the original approver |
| Board 3 verdict | Is this the thing we asked for — **and only that**? P3/P6/P9/P10 are your anti-gold-plating checks |

## 9.5 Solution Architect — Mahesh (R2)

| Trigger | Your action |
|---|---|
| Stage fit disputed | You decide, same day |
| Boards conflict | You + PO, recorded decision, 2 days |
| Rework round 3 | You + PO, **immediately** — the item is wrong, not the plan |
| A gate criterion cannot be met | You + PO: waiver (CR) or move the criterion — **before `CANDIDATE`** |
| `GOV` change | You + PO approve; version bump; decision register entry; **name the cost** — which gate criterion this governance work defers |
| Stage transition | **Only a human declares one.** Never an agent |
| Board 1 verdict | Boundaries, coupling, standing constraints, ADR, **the smallest structural change** |

## 9.6 Security — Deepali (R8) and Risk & Compliance — Shailja S (R9)

| | |
|---|---|
| **Your verdict is a veto** | `REWORK`/`REJECTED` from either of you is binding. No aggregate, no majority override |
| **At T4 you must be human** | An AI simulation of your board cannot satisfy the sign-off. It may draft the reasoning and assemble the evidence |
| **You may escalate a tier** | T3 → T4 is your single call and needs no CR |
| **Compliance debt** | **Never accepted.** It is a violation with a delay |
| **Security debt** | Expiry **no later than the next gate** |
| **After a bypass** | Decide whether the skipped control is run retrospectively or the residual risk is formally accepted — and by whom |

## 9.7 QA Lead — Swapnali (R7)

| Trigger | Your action |
|---|---|
| ADMIT of any `FUNC`/`BUG` | Board 5: is each AC observable and testable? negative and boundary cases? PII-free test data? |
| DoR check | R12 — test approach agreed and data available. *"Ready except for the tests" is not ready* |
| DoD | You sign off quality before the Tech Lead declares `FUNC` Done |
| **Never** | Assume unexecuted results; waive a non-waivable Security/Compliance conclusion |

## 9.8 SRE / Operations — Shivanshi (R10)

| Trigger | Your action |
|---|---|
| Plan has material `operational_impact` | Board 7, O1–O8, with evidence — 15–30 min |
| New production component or provider | Establish the operational contract: deployability, telemetry, runbook, limits, failure/recovery |
| Material campaign or volume change | Reassess business load, transaction amplification, the **actual** bottleneck, provider/DB limits and scale policy |
| Incident | Detect, assess, contain, restore, validate |
| Incident closes | With **R12**: search the registers for items that would have prevented it; calibrate |
| Asked to scale | Name the business load, the amplification, the real bottleneck, the next downstream limit, the safe range and the recovery behaviour. **More pods are not a diagnosis** |

## 9.9 Delivery Lead — Kalpana (R12)

You own the machinery that makes parking honest. Nobody else's failure shows up as fast as yours.

| Cadence | Your action |
|---|---|
| Per gate | **Unpark sweep** · aging review · metrics snapshot · scorecard |
| Per approved scope change | Sweep items whose block was scope |
| Monthly | Duplicate merge pass on the suggestion register (45 min) |
| Weekly | Governance Sync (30 min) |
| State file stale > 30 days | **It is yours to refresh** — escalate to the Tech Head if unfixed |
| A board misses its response window | Record `NO_RESPONSE`, ask the named persona directly with the deadline restated, then escalate to the accountable human |
| A decision or dependency will exhaust critical-path slack | Escalate to the owning authority with options, impact and the consequence of delay |
| An incident traces to a parked or rejected item | Calibration review with R10, within a week |
| **You may never** | Decide the *content* of a specialist decision, or convert `CANDIDATE` into approval |

---

# Part 10 — Use cases, worked end to end

Each case: what arrived → what the verdict was → **who does what next** → when it is closed.

## UC-01 · A stakeholder asks for a feature during hardening

**Input:** "Can we add Health insurance quoting? The RMs keep asking."

| | |
|---|---|
| Triage | `SF3` premature (Phase 5 owns LOB expansion) · `SC0` in scope for a later phase · necessity `NOT-NOW`, `future_necessity: MUST` |
| Verdict | **PARK** → Phase 5, unparks at "Phase 4 gate PASSED" · P4 now / P2 at target |
| Agent | Writes the parked row with all five fields; says the six-line answer; returns to the in-flight item |
| Stakeholder | Reads `target_stage`. If Phase 5 is unacceptable to the business, that is a **priority conversation with the PO**, and if it wins, a **CR to pull it forward** |
| Delivery Lead | The item is now on the Phase 4 gate sweep list |
| Closed when | The Phase 4 gate passes and the sweep re-triages it — fresh, against the state as it is *then* |

## UC-02 · An agent notices duplicated code mid-task

**Input:** while fixing a payment status mapper, the agent sees the mapper duplicated in two modules.

| | |
|---|---|
| Test first | Is it O3/O7/O8 for the current item? No. Is it O1/O2/O5/O6 for the system? No |
| Verdict | **PARK** — `SF2`, fails the absorption test (a shared mapper needs a new common enum → new decision) |
| Agent | `SUG-` row · one line: *"Noted as SUG-20260821-x4m (parked, Phase 5). Continuing FUNC-011."* · **does not touch the second module** |
| If it had already started | Return protocol: stop · snapshot · **keep only what serves the AC, revert or extract the rest** · register · re-anchor · resume · report in one line |
| Closed when | Phase 5 sweep re-triages it — probably as an `SF1` `REFACTOR` once a second LOB gives the abstraction a real second implementation |

## UC-03 · A SAST scan reports a reachable vulnerability in shipped code

| | |
|---|---|
| Triage | **O2 hard override** — reachable and exploitable in code we ship |
| Verdict | **ADMIT · P1**, bypassing the matrix and every cap |
| Agent | States the class **and the evidence (CVE + the reachable call path)** out loud · snapshots the in-flight item · switches |
| Still required | A plan, and the boards mandatory for the tier. **A P1 is fast-tracked, not un-governed.** Type `SEC` → Security board is mandatory, Architecture too |
| Tier | Almost certainly **T4** if it changes an authn/authz control → **human** Security sign-off |
| Closed when | Fixed with a negative test proving the denied path, `SEC` DoD met, risk register updated, agent **returns to the snapshotted item and says so** |

## UC-04 · The same scan reports a CVE in a dependency that is never called

| | |
|---|---|
| Triage | Not O2 — **unreachable**. Classified `DEBT`/`INFRA`, with the reachability finding stated explicitly |
| Verdict | ADMIT at normal priority, or PARK with a target — depending on stage fit |
| Note | *"A vulnerability that is real but unreachable at the current stage is P1 for its stage."* Record it, do not preempt |

## UC-05 · An IRDAI circular imposes a new disclosure obligation

| | |
|---|---|
| Triage | `SC4` — externally mandated. **The scope filter fires before anything else** |
| Verdict | **ESCALATE.** Not admitted, not rejected, not parked |
| Agent | Writes the triage record, opens `CR-###` with `driver: regulatory mandate` and the circular reference as `evidence`, runs impact analysis (steps 2–8 as if approved), **and stops** |
| Approvers | **PO + Compliance + Architect**, same day |
| On APPROVED | All five actions: update `02-PROJECT_SCOPE.md`; update `CURRENT-STATE.yaml` (**human**); unpark sweep; recompute priorities; create the work items |
| Closed when | The enabled work items are themselves Done, with compliance evidence captured |

## UC-06 · A parked item's trigger fires (live example: TD-014)

**State:** TD-014 parked at Phase 1, target Phase 4, trigger "overlaps gate criterion 4.1". The
register flags it `⚠️ Now eligible`.

| | |
|---|---|
| Who acts | **Delivery Lead**, at the next gate sweep |
| Procedure | Re-run steps 2–7 **against the current state**. Do not auto-admit |
| Legal outcomes | (a) **Promote** into the Phase 4 backlog alongside criterion 4.1 → then all of [Part 3](#part-3--admit--the-full-track); or (b) **re-park with a reason**. There is no third option, and "leave it flagged" is not one |
| Record | Sweep date, previous target, new state — on the item and in the sweep log |

## UC-07 · The repository owner says "just do it, skip the process"

| | |
|---|---|
| Verdict | **ADMIT-BYPASS** |
| Agent | (1) Does it. (2) `SUG-` row: `action: ADMIT-BYPASS`, `raised_by: human:<name>`, what was skipped. (3) One sentence of risk. (4) If it touches secrets/PII/a public contract/data integrity — **says so once, clearly, before acting**, then proceeds |
| Says | *"Understood — doing it directly. Recording the bypass and the one risk it carries: no Architecture board ran on a new public contract."* |
| Boundary | State what the bypass does **not** cover: no Security, Compliance, QA or mandatory-human decision is bypassed |
| Aftermath | Skipped mandatory boards run retrospectively **or** the residual risk is explicitly accepted by the accountable human. Counted in the bypass rate |
| If bypasses become frequent | That is a **process signal**: lower the tier, do not exhort |

## UC-08 · Production is down at 02:00

| | |
|---|---|
| Path | **Emergency**, not triage. *Act. Stop the harm first — the framework never blocks incident response* |
| Same session | Record what changed, which controls were bypassed, and the blast radius |
| Then | Retrospective `CR-###` marked `type: EMERGENCY` |
| Within one stage | Run the skipped boards; ratify or schedule remediation as P1/P2 |
| Then | Root cause → risk register. Shortcut → debt ledger |
| And | **R12 + R10** search the registers for a parked or rejected item that would have prevented it. Incident preventability has a target of **zero**, so any hit forces a calibration review |

## UC-09 · The same idea arrives for the third time, from a third person

| | |
|---|---|
| Rule | Not a new row. Link the original, `recurrence_count += 1` |
| At 3 | **`recurrence_count ≥ 3` is itself an unpark/reopen trigger**, and AS-5 grants `+1` on the R factor at next scoring |
| Action | Re-triage against current state. It may now be `SF1`. If it is still rejected, the reason has to answer the three independent sources, not repeat the first refusal |
| Signal | A high recurrence rate means rejections are not being read, or are wrong |

## UC-10 · A board returns REWORK twice

| | |
|---|---|
| Round 1 | Author revises; **only the objecting boards** re-review |
| Round 2 | Same |
| Round 3 | **Not permitted.** Escalate to Architect + PO immediately |
| Why | *"A third round is a signal that the problem, not the plan, is misunderstood"* — the item usually needs splitting, a spike, or full re-triage |
| Also | Two rework rounds is a **revalidation trigger for the item's classification**, not just its plan |

## UC-11 · A board never responds

| | |
|---|---|
| T3 example | 2 working days pass with silence |
| Action | **R12 records `NO_RESPONSE`** on the gate record — this is **not** a verdict — asks the named persona directly with the deadline restated, and notifies |
| Still silent | Escalate to the accountable human for that board |
| Throughout | **The gate stays NOT APPROVED.** Silence never approves, and never satisfies a T4 human sign-off |
| Repeat offences | A **staffing signal**: the board is unstaffed, over-triggered by the proportionality matrix, or reviewing work it has no interest in. Fix the cause; do not shorten the window |

## UC-12 · The state file is past `review_due` and new work arrives

| | |
|---|---|
| Check | `java scripts/governance/FreshnessCheck.java` → exit **2** |
| Allowed | **Park and reject** |
| Not allowed | **Admit new work** (Rule CS-1) |
| Say | *"`CURRENT-STATE.yaml` is past `review_due`. I can park and reject against it, but not admit new work. **Kalpana / R12** needs to refresh it."* |
| Escalation | Stale > 30 days → R12, then the Tech Head if unfixed. Same day |

## UC-13 · Mid-implementation, something in `out_of_scope` turns out to be required

| | |
|---|---|
| Rule | **Stop.** New work item, fresh triage. *"This is the single most common creep vector"* |
| Not allowed | Editing `files_expected` or `out_of_scope` to match what you already did (Rule DC-2 — that is a cover-up) |
| Say | *"This is larger than the plan: it needs a second component. Stopping to re-review rather than expanding scope."* |
| Then | Either the affected boards re-review before you continue, or the item goes **BLOCKED** with a named blocker. **Never "proceed and ask later"** |

## UC-14 · The item is blocked by the bank's app team

| | |
|---|---|
| Edge | `EXTERNAL` |
| State | **BLOCKED** — records blocker ID, owner and follow-up date; **releases the WIP slot** |
| Key rule | **The chase is a separate work item, and it is not itself blocked** (DEP-3). Somebody owns chasing |
| Executor | Takes the next eligible READY item — does not idle |
| Escalation | If it will exhaust critical-path slack, **R12** escalates to the owning authority with options, impact and consequence of delay, before the required-by date |

## UC-15 · A deliberate shortcut is taken to hit the gate

| | |
|---|---|
| Type | `DEBT` — **not** `BUG`. A bug is unintended; debt is a known, accepted deviation at merge time |
| Required | `TD-###` with owner, severity and **expiry**. **Rule TD-1 — no expiry, no debt entry**; without one it is not tracked debt |
| Security debt | Expiry **no later than the next gate** |
| Compliance debt | **Not permitted at all** |
| DoD | The item can still be **Done**, provided the debt is registered properly |
| Do not | Re-report known debt: TD-006/007/009/010/014/022/023, QA-001 |

## UC-16 · Someone proposes a change to the governance framework itself

| | |
|---|---|
| Type | `GOV` — **and it queues like any other work** (Rule BR-4) |
| Required | A `SUG-`, a triage verdict, a priority, a backlog entry, a place in the queue, **and it consumes its owner's single in-flight slot** |
| CR | Yes — Architect + PO, version bump, decision register entry |
| Extra field | The CR `impact` block must state **which gate criterion or delivery outcome this governance work defers** |
| Exempt (hygiene, not change) | Fixing a broken link or typo, appending a register row during normal triage, recording a decision already made, freshness acknowledgements |
| The test | *If it adds a rule, a persona, a board, a document or a required artefact, it is `GOV` work and it queues. If it only records something the framework already decided, it is hygiene.* |

## UC-17 · Someone proposes adding a new persona

| | |
|---|---|
| Status | **The roster is closed at nine named human accountability personas (CR-009)** |
| Required | All four admission tests must hold: PR-1 a real identified human occupies the role today · PR-2 no existing persona can hold the jurisdiction · PR-3 it maps to an existing AIGEM role or board · PR-4 **something is merged or retired in the same CR** |
| Rule CC-2 | **No net persona growth.** "This role exists in mature organisations" is not a driver |
| Rule CC-3 | Personas are for **accountability, not coverage**. Expertise no human owns belongs in a role's *package*, not a new persona |
| Preferred, in order | Extend an existing package → add a module → add a bilateral protocol (only on **evidenced** repeated deadlock) → add a persona (last resort) |

## UC-18 · A review board's `should_fix` comment

| | |
|---|---|
| Rule | `should_fix` items are **triaged as fresh `SUG-`s** — they are suggestions and get exactly the same treatment as any other |
| Contrast | `must_fix` blocks approval. **Conditions** from `APPROVED_WITH_CONDITIONS` become **acceptance criteria** and are verified at DoD |
| Do not | Silently fold a `should_fix` into the current diff. That is drift signal D8 |

## UC-19 · A plan approved five weeks ago is about to be picked up

| | |
|---|---|
| Rule RG-8 | **Approval expired at 30 calendar days.** Re-run the boards |
| Scoped | Only the boards whose **inputs changed**. A board whose jurisdiction did not move re-affirms with a one-line evidence entry |
| Also expires immediately if | A stage transition happened, an approved CR touched the plan's scope, a standing constraint changed, or the plan was materially edited |
| And | Rule DR-1 — READY itself expires at a stage boundary; re-check R3 and R6 |

## UC-20 · An assumption the plan rested on turns out to be false

| | |
|---|---|
| Trigger | **Assumption invalidated** is a revalidation trigger |
| Action | Re-validate **every item and plan citing that `ASM-###`** — within ~15 minutes, by the owner |
| Then | Items whose necessity rested on it are re-triaged; rejections that rested on it are now reopenable on valid grounds |
| Register | `ASSUMPTION-REGISTER.md` updated; DoD closure requires assumptions to be marked validated or invalidated |

---

# Part 11 — Quick reference

## 11.1 Every verdict's obligations, on one page

| | ADMIT | PARK | REJECT | ESCALATE | ADMIT-BYPASS |
|---|---|---|---|---|---|
| Register row | ✅ | ✅ | ✅ | ✅ | ✅ |
| Extra register | routed backlog | parked backlog | — | `CR-###` | routed backlog |
| Mandatory extra fields | type, tier, P now + target, deps | target stage, unpark trigger, future necessity | argument + `reopen_if` | driver, evidence, impact, alternatives | who authorised, what was skipped, the risk |
| Needs a plan | T2+ | — | — | impact analysis | per the bypass |
| Needs boards | per tier | — | — | approvers per `14 §1` | retrospectively, or explicit risk acceptance |
| Who can reverse it | Boards / re-triage | Sweep, or CR to pull forward | **CR only** | The approvers | — |
| Recurring obligation | until DoD | **every gate sweep** | none | until decided | until the skipped control is closed out |
| Metric it feeds | admission rate, cycle time | park accuracy, aging | reversal rate, recurrence | overdue decisions | **bypass rate** |

## 11.2 Clocks

| Event | Window | Source |
|---|---|---|
| Triage latency: input → verdict | < 1 working day | [`18 §2`](./18-GOVERNANCE_METRICS.md#flow) |
| Board response, T1–T2 | 1 working day | [`11 §12.1`](./11-REVIEW_GATES.md#121-board-response-clock) |
| Board response, T3 | 2 working days | ″ |
| Board response, T4 | 3 working days | ″ |
| Gate cycle: plan submitted → gate closed | < 3 working days | [`18 §2`](./18-GOVERNANCE_METRICS.md#flow) |
| Approval validity | 30 calendar days, or until context changes | [Rule RG-8](./11-REVIEW_GATES.md#14-post-approval) |
| Necessity dispute → PO | same day | [`RUNBOOK §9`](./RUNBOOK.md#9-escalation) |
| Stage-fit dispute → Architect | same day | ″ |
| `SC4` mandate → PO + Compliance + Architect | same day | ″ |
| Boards conflict → Architect + PO | 2 days | ″ |
| Rework round 3 → Architect + PO | immediately | ″ |
| State file stale > 30 days → R12 | same day | ″ |
| Incident traced to a parked/rejected item → calibration | 1 week | ″ |

## 11.3 What gets written where

| Action | Artefact | Location |
|---|---|---|
| Any input triaged | Triage record | [`registers/SUGGESTION-REGISTER.md`](./registers/SUGGESTION-REGISTER.md) |
| Parked | Row with target + trigger | [`registers/PARKED-BACKLOG.md`](./registers/PARKED-BACKLOG.md) |
| Rejected | Closed record with reason + `reopen_if` | Suggestion register |
| Escalated | `CR-###` | [`change-requests/`](./change-requests/) |
| Decided | Decision entry | [`registers/DECISION-REGISTER.md`](./registers/DECISION-REGISTER.md) |
| Admitted | Backlog entry with `origin: SUG-####` | Routed per [`08 §3`](./08-BACKLOG_RULES.md#3-routing-table-l3) |
| Planned | Implementation plan | Plan file or PR body |
| Reviewed | One verdict per board, with evidence | Plan `reviews[]` |
| Implemented | Commits referencing the work item ID | Git |
| Validated | Evidence artefacts | Per [`13`](./13-DEFINITION_OF_DONE.md) |
| Debt taken | `TD-###` with owner, severity, expiry | [`TECH-DEBT.md`](../1sb-insurance-integration/service-ssot/TECH-DEBT.md) |
| Risk found | `RISK-###` | [`registers/RISK-REGISTER.md`](./registers/RISK-REGISTER.md) |
| Assumption made | `ASM-###` | [`registers/ASSUMPTION-REGISTER.md`](./registers/ASSUMPTION-REGISTER.md) |
| Dependency | `DEP-###` | [`registers/DEPENDENCY-REGISTER.md`](./registers/DEPENDENCY-REGISTER.md) |

## 11.4 The item state machine

```text
TRIAGED ──► READY ──► IN-FLIGHT ──► IN-REVIEW ──► DONE
   │          ▲            │
   │          │            └──► BLOCKED ──┘
   ├──► PARKED ┘ (on unpark trigger, RE-TRIAGE — never auto-admit)
   ├──► ESCALATED ──► (CR approved) ──► TRIAGED
   └──► CLOSED: REJECTED | DUPLICATE | SUPERSEDED | LAPSED | WONT-DO
```

| State | Invariant that must hold |
|---|---|
| `TRIAGED` | Has a `SUG-` with SF, SC, necessity, type, priority |
| `READY` | Meets all 15 DoR criteria; approved plan at T2+ |
| `IN-FLIGHT` | **Exactly one per owner** |
| `BLOCKED` | Names blocker ID, owner, follow-up date; **does not consume a WIP slot** |
| `IN-REVIEW` | Board verdicts pending or conditions outstanding |
| `DONE` | Meets DoD **with evidence** |
| `PARKED` | Has `target_stage` **and** `unpark_trigger` |
| Closed states | Have a reason; `DUPLICATE`/`SUPERSEDED` also have a link |

## 11.5 Authority — the things nobody may do

| Nobody may | Not even |
|---|---|
| Delete a parked or rejected register entry | With a recorded rejection reason it is *closed*, not deleted |
| Override a Security or Compliance veto by aggregate or majority | The Architect, the PO, or both together |
| Satisfy a **T4 human sign-off** with an AI simulation | Marked `reviewer_type: AGENT` and obviously correct |
| Approve their own CR as an agent | Its own, obviously correct, or under time pressure |
| Edit stage fields in `CURRENT-STATE.yaml` as an agent | To fix an obvious error — say it instead |
| Record `APPROVED` with an empty `evidence[]` | Human or agent — it is recorded as `NOT_RUN` |
| Treat board silence as assent | At any tier |
| Accept **compliance debt** | It is a violation with a delay |
| Convert `CANDIDATE` into gate approval (R12) | R12 marks `CANDIDATE` only |
| Widen `files_expected` after the fact to cover a drift | That is a cover-up, not a variance log |
| Declare a stage transition without meeting the criteria | Only via an approved waiver CR |

## 11.6 Numbers that say the process is failing

| Metric | Target | If it is wrong |
|---|---|---|
| **Gate criteria closed per week** | **> 0, every week** | **Zero for two consecutive weeks raises `INTERVENE` on the governance system itself** (Rule GM-1) — no other metric outranks this |
| Triage coverage | > 95% | Inputs are being answered in conversation and forgotten |
| **Bypass rate** | < 10% | **The ceremony exceeds the value. Fix the process, usually by lowering the tier** |
| Admission rate | 20–40% | > 60% ⇒ the gate is not filtering. < 10% ⇒ probably over-rejecting |
| **Park accuracy** | > 60% | Low ⇒ parking is a polite rejection |
| False-P1 rate | < 5% | Override inflation — the queue is unordered again |
| Rework rounds per plan | < 0.5 | Plans are being written too early |
| Board `NO_RESPONSE` | 0 | Any repeat is a **staffing** signal |
| Drift incidents per PR | < 0.5 | Plans are not being kept open while working |
| Plan accuracy (`files_expected` ∩ changed ÷ changed) | > 0.85 | Plans are aspirational |
| **Suggestions registered per session** | **> 0 is healthy** | **Zero is a warning** — nobody is looking, or people are quietly acting on what they see |
| Incident preventability | 0 | Any occurrence forces a calibration review |

---

# Part 12 — Copy-paste forms

## 12.1 Short-form triage record — covers most inputs

```yaml
SUG-20260821-a1b: "<the input, verbatim — do not paraphrase it into something more reasonable>"
context:      WS-1 · Phase 4 · working on FUNC-011
stage/scope:  SF2 (Phase 5 — needed when the second LOB exists) / SC0
necessity:    SHOULD · confidence C4 · X2 fails (one implementation today)
action:       PARK → Phase 5, unparks at the Phase 4 gate
priority:     P4 now / P3 at target
resumed:      FUNC-011
```

## 12.2 Parked backlog row

```markdown
| SUG-20260821-a1b | <one-line item> | WS-1 | Phase 4 | Phase 5.4 | Phase 4 gate PASSED | MUST | P4 / P2 | <why parking is correct today> |
```

Columns: `ID | Item | WS | Parked at | Target stage | Unpark trigger | Future necessity | P now / target | Parked because`

## 12.3 Rejection record

```yaml
SUG-20260821-c3d: "<the input, verbatim>"
context:      WS-1 · Phase 4
stage/scope:  SF4 / SC3
necessity:    REJECT
evidence:     E7 (preference) — no measured problem; X9 fails
action:       REJECT
reason: >
  <what it contradicts, or which decision supersedes it — cite the ADR or constraint>
reopen_if:    "<the observable condition that would make this a different question>"
resumed:      FUNC-011
```

## 12.4 Bypass record

```yaml
SUG-20260821-e5f: "<what the human asked for, verbatim>"
raised_by:    human:<name>
action:       ADMIT-BYPASS
authorised_by: human:<name> — explicit instruction to proceed directly
bypass:
  skipped:    "<which boards / which queue position / which plan step>"
  boundary:   >
    Limited to <X>. Does NOT bypass any Architecture, Security, Compliance, QA,
    SRE, Database or mandatory-human decision.
  risk:       "<one sentence — e.g. no Security board ran on an auth-path change>"
  capacity:   >
    Consumes capacity while <GATE-ID> remains open. Does not change, waive or
    mark any gate criterion complete.
  follow_up:  "<run board X retrospectively | risk accepted by <human> on <date>>"
ref:          <link to the artefact actually produced>
```

## 12.5 Change request skeleton

```yaml
change_request:
  id: CR-011
  raised_by: "agent:claude"
  date: 2026-08-21
  type: SCOPE            # SCOPE | STAGE | PLAN | CONSTRAINT | REVERSAL | WAIVER | GOV | EMERGENCY
  current_position: >    # what the approved documents say TODAY. Quote them.
  proposed_change: >     # precisely what changes, in which document, with replacement text
  driver: >              # regulatory · security finding · external change · assumption failure ·
                         # business priority change · new evidence.  "Better" is NOT a driver.
  evidence: []
  impact:
    scope:        ""
    stage:        "does this move a gate date?"
    dependencies: "which items become blocked, unblocked, or invalid"
    parked_items: "which parked items this makes eligible"
    effort:       "S | M | L | XL"
    risk_if_rejected: ""
  alternatives_considered:
    - option: "do nothing"
      consequence: ""
  decision: PENDING
  approvers: []
  conditions: []
```

## 12.6 Board verdict

```yaml
review:
  board: SECURITY
  reviewer: "Deepali / Security Architect"
  reviewer_type: HUMAN        # HUMAN | AGENT
  self_review: false
  plan: PLAN-011
  decision: APPROVED_WITH_CONDITIONS
  must_fix: []
  conditions:
    - "paymentUrl must not appear in audit payloads — assert in test"
  should_fix: []
  evidence:                   # NEVER empty — an empty evidence[] is recorded as NOT_RUN
    - "checked S1–S12 against plan security impact and files_expected"
  notes: ""
  date: 2026-08-21
```

## 12.7 Done evidence table

```text
Work item:      NFR-011  (origin SUG-20260821-a1b, plan PLAN-011)
AC:             AC-1 ✅  AC-2 ✅  AC-3 ✅   (+ SEC condition C1 ✅)
Tests:          12 added · unit + integration · negative cases included
Gates:          test ✅  jacoco verification ✅  ArchUnit ✅
Drift:          files_expected honoured; 1 variance logged
Debt:           none created
Registers:      SUG-… CLOSED-DELIVERED · DEP-031 resolved · no new risks
Docs:           OpenAPI updated
Gate impact:    advances Phase 4 exit criterion 4.1
Requesting:     Tech Lead closure
```

## 12.8 Gate sweep record

```markdown
| Date | Gate / trigger | Items swept | Promoted | Re-parked | Closed |
|------|----------------|-------------|----------|-----------|--------|
| 2026-09-05 | GATE-P4 PASSED | 11 | 2 (TD-014, TD-022) | 8 | 1 (E13 — SUPERSEDED) |
```

---

# Glossary

| Term | Meaning |
|---|---|
| **AIGEM** | The governance framework in `docs/governance/` — the triage layer in front of the existing delivery process |
| **Input** | Anything arriving that could become work |
| **Triage record** | The pipeline's output for one input. Always produced. `SUG-<YYYYMMDD>-<3 chars>` |
| **Work item** | An admitted input with an ID, a type, a priority and a home |
| **ADMIT / PARK / REJECT / ESCALATE** | The four verdicts the matrix can produce |
| **ADMIT-BYPASS** | A fifth outcome — a human overrode the process; recorded, not reached by the matrix |
| **SF0–SF4** | Stage fit: prerequisite · on-stage · adjacent · premature · stage-invalid |
| **SC0–SC4** | Scope fit: explicit · derived · adjacent value · out of scope · externally mandated |
| **Necessity** | MUST / SHOULD / COULD / NOT-NOW / REJECT |
| **E1–E7** | Evidence tiers, E1 strongest |
| **C1–C5** | Confidence levels; below C3 ⇒ spike, not implementation |
| **P1–P5** | **The only delivery priority scale.** Always stage-relative |
| **T1–T4** | Risk tier; decides plan form and mandatory boards |
| **O1–O8** | The hard P1 override classes |
| **D1–D14** | Drift signals |
| **X1–X9** | Anti-over-engineering tests ([`16 §6`](./16-DECISION_MODEL.md#6-anti-over-engineering-tests)) |
| **AS-1…AS-5** | Anti-starvation rules for parked and idea items |
| **PRI-1…PRI-8** | Priority caps and consistency rules |
| **DoR / DoD** | Definition of Ready ([`12`](./12-DEFINITION_OF_READY.md)) / Definition of Done ([`13`](./13-DEFINITION_OF_DONE.md)) |
| **Board** | One of the seven review perspectives; Security and Risk & Compliance hold vetoes |
| **`CR-###`** | Change request — the only route through change control |
| **`TD-###`** | Tech debt ledger entry; invalid without an expiry |
| **Sweep** | The scheduled re-triage of parked items at a gate or scope change |
| **Unpark trigger** | The **observable** event that returns a parked item to triage |
| **Variance vs drift** | A variance is logged and continues; drift crosses a boundary and stops |
| **`NO_RESPONSE`** | A board missed its window. Not a verdict, never assent, always a named stop |

---

## Source index — everything in this guide traces to these

| Topic | File |
|---|---|
| Action matrix, principles, non-negotiables | [`00-GOVERNANCE.md`](./00-GOVERNANCE.md) |
| Current stage, standing constraints, known debt | [`01-CURRENT_STATE.md`](./01-CURRENT_STATE.md) |
| Scope fit SC0–SC4 | [`02-PROJECT_SCOPE.md`](./02-PROJECT_SCOPE.md) |
| Stage fit SF0–SF4, absorption test | [`03-LIFECYCLE.md`](./03-LIFECYCLE.md) |
| Stage gates and exit criteria | [`04-STAGE_GATES.md`](./04-STAGE_GATES.md) |
| Priority, P1 overrides, caps, anti-starvation | [`05-PRIORITY_MODEL.md`](./05-PRIORITY_MODEL.md) |
| Work types, risk tiers, breakdown | [`06-WORK_CLASSIFICATION.md`](./06-WORK_CLASSIFICATION.md) |
| Dependencies and execution order | [`07-DEPENDENCY_MODEL.md`](./07-DEPENDENCY_MODEL.md) |
| Buckets, routing, unparking, traceability | [`08-BACKLOG_RULES.md`](./08-BACKLOG_RULES.md) |
| The agent contract, bypass rule | [`09-AI_EXECUTION_RULES.md`](./09-AI_EXECUTION_RULES.md) |
| Implementation plans | [`10-IMPLEMENTATION_PLAN_TEMPLATE.md`](./10-IMPLEMENTATION_PLAN_TEMPLATE.md) |
| The seven boards, verdicts, aggregation, response clock | [`11-REVIEW_GATES.md`](./11-REVIEW_GATES.md) |
| Definition of Ready | [`12-DEFINITION_OF_READY.md`](./12-DEFINITION_OF_READY.md) |
| Definition of Done, governance closure | [`13-DEFINITION_OF_DONE.md`](./13-DEFINITION_OF_DONE.md) |
| Change control, personas, emergencies, reversals | [`14-CHANGE_CONTROL.md`](./14-CHANGE_CONTROL.md) |
| Tech debt policy | [`15-TECH_DEBT_POLICY.md`](./15-TECH_DEBT_POLICY.md) |
| Necessity, evidence, confidence, revalidation | [`16-DECISION_MODEL.md`](./16-DECISION_MODEL.md) |
| Drift signals, classification, return protocol | [`17-DRIFT_CONTROL.md`](./17-DRIFT_CONTROL.md) |
| Metrics and the gate scorecard | [`18-GOVERNANCE_METRICS.md`](./18-GOVERNANCE_METRICS.md) |
| Cadence, roles, ceremonies, escalation | [`RUNBOOK.md`](./RUNBOOK.md) |
| Delivery orchestration (D0–D6) | [`DELIVERY-CONTROL-SYSTEM.md`](./DELIVERY-CONTROL-SYSTEM.md) |
| Who decides, who cannot | [`PERSONA-AUTHORITY-MATRIX.md`](./PERSONA-AUTHORITY-MATRIX.md) · [Authority Quick Card](../context/personas/AUTHORITY-QUICK-CARD.md) |
| The ten facts, agent posture per stage | [`../context/BOOT.md`](../context/BOOT.md) |
| The compressed pipeline | [`.claude/skills/aigem-triage/SKILL.md`](../../.claude/skills/aigem-triage/SKILL.md) |
