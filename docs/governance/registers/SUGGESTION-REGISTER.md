# Suggestion Register

**Every input that could become work gets a row here — admitted, parked, rejected, or escalated.**
Nothing is dropped. Nothing is deleted.

**Owner:** whichever agent or person triaged the input
**ID format:** `SUG-<YYYYMMDD>-<3 chars from 0-9a-z>` — collision-resistant, no shared counter.
Rules: [../state/CURRENT-STATE.yaml](../state/CURRENT-STATE.yaml) `id_allocation`
**Rules:** [08-BACKLOG_RULES.md](../08-BACKLOG_RULES.md) · [09-AI_EXECUTION_RULES.md](../09-AI_EXECUTION_RULES.md)

---

## How to add a row

1. Mint an ID: `SUG-<today>-<3 random chars>`, e.g. `SUG-20260812-a1b`. No counter to
   increment and no merge conflict when two branches triage at once.
2. Run pipeline steps 2–5 ([09 §2](../09-AI_EXECUTION_RULES.md#2-the-mandatory-sequence)).
3. Add the summary row below.
4. For anything beyond a trivial reject, add a detail block in §3 using
   [../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).
5. **Check for duplicates first** (Rule CS-2). A repeat is linked and increments
   `recurrence_count` — it is not a new row.

---

## 1. Status vocabulary

| Status | Meaning |
|--------|---------|
| `ADMITTED` | Entered a backlog for the current stage |
| `ADMIT-BYPASS` | Implemented under a human override of the process ([09 §8](../09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process)) |
| `PARKED` | Real work, later stage — see [PARKED-BACKLOG.md](./PARKED-BACKLOG.md) |
| `ESCALATED` | Awaiting a human decision as `CR-###` |
| `REJECTED` | Will not be done; reason recorded |
| `DUPLICATE` | Already tracked; linked |
| `SUPERSEDED` | Overtaken by another decision; linked |
| `LAPSED` | Idea closed by aging (AS-3) |
| `CLOSED-DELIVERED` | Admitted and shipped; linked to the PR |

---

## 2. Register

| ID | Date | Source | Summary | SF | SC | Necessity | Type | P now / target | Action | Ref |
|----|------|--------|---------|----|----|-----------|------|----------------|--------|-----|
| SUG-20260810-b4d | 2026-08-10 | human:Mahesh (persona-system assessment) | Reviewer personas for the four board roles with no persona (QA, Security, Risk & Compliance, Operations) | SF1 | SC1 | SHOULD | DOC | P3 / P3 | ADMITTED | [§3](#sug-20260810-b4d--reviewer-personas-for-the-four-personaless-boards) |
| SUG-20260810-c7f | 2026-08-10 | human:Mahesh (persona-system assessment) | Remove unevidenced architectural guarantee and unsourced NFR figures from the three baseline personas | SF2 | SC1 | SHOULD | DOC | P3 / P3 | ADMITTED | [§3](#sug-20260810-c7f--remove-unevidenced-claims-from-the-three-baseline-personas) |
| SUG-20260810-k2m | 2026-08-10 | human:Mahesh (persona-system assessment) | "Role-contract v1" — replace prose personas with a compact structured contract format | SF3 | SC2 | COULD | DOC | P5 / P4 | PARKED | [PARKED §3](./PARKED-BACKLOG.md#3-ideas--no-committed-stage) |
| SUG-20260810-p9q | 2026-08-10 | human:Mahesh (persona-system assessment) | Persona evals — 8–15 scored test prompts per persona | SF3 | SC2 | COULD | QA | P5 / P4 | PARKED | [PARKED §3](./PARKED-BACKLOG.md#3-ideas--no-committed-stage) |
| SUG-20260810-z8n | 2026-08-10 | human:Mahesh (persona-system assessment) | Personas for the three proposed job titles with no review board (Team Lead, Java Developer, Agent/ML Engineer) | SF3 | SC2 | COULD | DOC | P5 / P4 | PARKED | [PARKED §3](./PARKED-BACKLOG.md#3-ideas--no-committed-stage) |
| SUG-20260810-t3w | 2026-08-10 | human:Mahesh (persona-system assessment) | Add a `system-state-manifest.md` and a shared project constitution in `AGENTS.md` | — | — | — | GOV | — | SUPERSEDED | [§3](#sug-20260810-t3w--system-state-manifest--shared-constitution) |
| SUG-20260810-v6x | 2026-08-10 | human:Mahesh (persona-system assessment) | "persona-engine-v3" — mandatory analysis phases, keyword mode detection, simulated specialists, compulsory self-criticism | SF4 | SC2 | REJECT | DOC | — | REJECTED | [§3](#sug-20260810-v6x--persona-engine-v3-template) |

<!--
Row format:
| SUG-0001 | 2026-08-08 | agent:claude | Redis-backed idempotency store | SF2 | SC0 | SHOULD | NFR | P4 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#sug-0001) |
-->

---

## 3. Detail records

Detail blocks live here for every non-trivial triage. Format:
[../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).

<!-- ### SUG-0001 · <title>  ... full triage record ... -->

### Batch note — persona-system assessment, 2026-08-10

One input (an assessment of the repository's persona/role-definition system) decomposed into
seven suggestions, because its proposals have genuinely different verdicts. Shared context for
all seven:

```yaml
context:
  workstream: none — see the gap note below
  reference_workstream: WS-1        # the dominant delivery workstream, used for stage posture
  current_phase: "Phase 4 — Hardening & consumer enablement"
  canonical_stage: "L7 — Hardening"
  current_objective: "Term path signed off for UAT use by at least one bank caller"
  state_as_of: "2026-08-10"
  state_provisional: false
  freshness_check: "exit 0 — FRESH"
  active_work_item: none            # no item was in flight when this input arrived
```

> **Gap for the Delivery Lead — not minted as work.** `CURRENT-STATE.yaml` defines two
> workstreams (WS-1, WS-2), but `GOV` and `DOC` are canonical work types
> ([06 §2](../06-WORK_CLASSIFICATION.md#2-work-types)) with routing entries, and changes to
> `docs/governance/**` and `docs/context/**` belong to neither. Rule LC-1 says stage fit is
> always evaluated against the input's *own* workstream; there is no workstream to evaluate
> against here. This batch used WS-1's L7 posture as the reference and says so explicitly
> rather than inventing a stage. A third workstream (or an explicit "repository governance is
> assessed against the most advanced delivery workstream" rule) would close it. Human decision.

**The finding that drove the two ADMITs.**
[11 §1](../11-REVIEW_GATES.md#1-the-board) defines seven boards; [11 §15](../11-REVIEW_GATES.md#15-running-the-board-as-a-single-agent)
tells a solo agent to adopt the matching persona from [`docs/context/roles/`](../../context/roles/README.md)
for each board. Personas exist for three:

| Board | Persona |
|-------|---------|
| Architecture | ✅ Mahesh — Solution Architect |
| Technical | ✅ Amit — Technical Head |
| Product | ✅ Rajal — Product Owner |
| QA | ❌ none |
| Security | ❌ none |
| Risk & Compliance | ❌ none |
| Operations | ❌ none |

`GATE-P4`'s approver list is `["Architect", "PO", "QA Lead", "Compliance (4.4)", "Ops (4.5)"]`
([CURRENT-STATE.yaml](../state/CURRENT-STATE.yaml)) — three of the five named approvers sit on
boards with no persona. That is the named beneficiary the SC1 test requires, and it is a
narrower, better-evidenced target than the seven job titles the assessment proposed.

---

### SUG-20260810-b4d · Reviewer personas for the four personaless boards

```yaml
id: SUG-20260810-b4d
raised_at: "2026-08-10"
raised_by: "human:Mahesh"
source: "assessment of the persona/role-definition system"
input: >
  Create missing personas, recommended order: QA Lead, QA Engineer, Java Developer, Team Lead,
  DevOps/SRE Lead, Security/Compliance Lead, Agent/ML Engineer. "Create a persona only when it
  has distinct ownership, tools, evidence, or approval authority."

stage_fit:
  code: SF1
  rationale: >
    Serves the current stage's deliverable: GATE-P4 criteria 4.4 (compliance review), 4.5
    (operations runbook) and 4.7 (coverage gates / QA-001) are approved by QA Lead, Compliance
    and Ops — the three board roles with no persona. Not SF0: the gate is not blocked, because
    HUMAN reviewers may sit any board (11 §2), and at T4 Security and Risk & Compliance require
    a human regardless.

scope:
  code: SC1
  business_scope: "not explicit"
  serves: ["GATE-P4 4.4", "GATE-P4 4.5", "GATE-P4 4.7"]
  failure_without_it: >
    11 §15's instruction cannot be followed for four of seven boards, so QA, Security,
    Risk & Compliance and Operations verdicts are produced in a blended generic voice — on
    exactly the boards the open gate names as approvers.
  minimal: true          # four personas in the EXISTING format; no new format, no eval
                         # harness, no job titles without a board
  authority: "11-REVIEW_GATES.md §1 and §15; CURRENT-STATE.yaml current_gate.approvers"

necessity:
  now: SHOULD
  future_necessity: SHOULD
  binds_when: "a T3+ plan is board-reviewed by a solo agent"
  evidence_tier: E2
  evidence:
    - "11-REVIEW_GATES.md §15 — explicit instruction to adopt the matching persona"
    - "CURRENT-STATE.yaml GATE-P4 approvers include QA Lead, Compliance, Ops"
    - "docs/context/roles/ contains 3 of the 7 board roles"
  confidence: C4
  anti_over_engineering:
    X1_named_consumer: true      # the four boards
    X9_problem_observed: true    # 4 of 7 boards have no persona today

action: ADMIT
action_rationale: >
  On-stage, minimal, with a named beneficiary. Scoped DOWN from the assessment's seven titles
  to the four that map to an actual review board. QA Lead and QA Engineer collapse to one QA
  board persona; "Security/Compliance Lead" splits into TWO, because Security and Risk &
  Compliance are separate boards with separate checklists and separate T4 human sign-offs.

classification:
  type: DOC
  also: [GOV]
  breakdown: STORY
  risk_tier: T1                  # non-binding documentation, no code, no contract
  destination: "docs/context/roles/ + roles/README.md panel table"

priority:
  now: P3
  at_target: P3
  factors: { N: 2, S: 3, B: 1, R: 1, D: 0, E: 1 }
  score: 13
  matrix_default: P3
  consistency: OK
  rationale: "Belongs to this stage, not gate-blocking — humans can sit the boards today."

dependencies:
  edges: ["enables SUG-20260810-c7f (same files, same reviewer-facing purpose)"]
  state: READY
  earliest_start: "after the in-flight Phase 4 gate work"

breakdown:
  children:
    - "QA reviewer persona (QA board)"
    - "Security reviewer persona (Security board)"
    - "Risk & Compliance reviewer persona (Risk & Compliance board)"
    - "Operations reviewer persona (Operations board)"
  not_included:
    - "Team Lead, Java Developer, Agent/ML Engineer — no board → SUG-20260810-z8n"
    - "Any change to the persona FORMAT → SUG-20260810-k2m"
    - "Agentic-AI evolution overlays for the new personas"

outcome:
  registered_in: "this register"
  status: ADMITTED-QUEUED
  note: >
    Queued, NOT implemented in the turn it was raised (09 §2 / AGENTS.md core rule). It is not
    head of the READY queue — the Phase 4 gate criteria are.
```

### SUG-20260810-c7f · Remove unevidenced claims from the three baseline personas

```yaml
id: SUG-20260810-c7f
raised_at: "2026-08-10"
raised_by: "human:Mahesh"
source: "assessment of the persona/role-definition system"
input: >
  Replace the unrealistic "zero code changes" guarantee with "no core-domain contract changes;
  adapter and mapping changes remain expected." Separate verified requirements from example
  metrics — 15-minute cache TTL, 50,000-user batches, five-second timeout, 99.9% uptime,
  sub-200ms latency appear as assumed facts. Replace simulated seniority with evidence
  requirements.

verified_against_the_files:
  - "mahesh-solution-architect.md:84 — 'require **zero code changes**' (stated flat)"
  - "amit-technical-head.md:73 — 'up to 50,000 IP records' (stated flat)"
  - "amit-technical-head.md:69 — 15-minute TTL (already hedged with 'e.g.')"
  - "amit-technical-head.md:90 — 99.9% uptime, <200ms, 5s timeout (already hedged with 'e.g.')"
  - "amit:11 / mahesh:11 / rajal:11 — 18 / 16 / 14 years of experience"
correction_to_the_input: >
  The assessment overstates part of its case: three of the five figures it cites are already
  written as "e.g." examples, not asserted facts. The flat, uncaveated claims are the decoupling
  guarantee and the 50,000-record figure. The item is admitted on those; the hedged figures get
  a source or an explicit "illustrative, not a committed target" label, which is cheaper than
  the rewrite the assessment implies.

stage_fit:
  code: SF2
  absorption_test:
    small: true              # three files, text only
    no_new_dependency: true
    no_new_decision: true    # labelling a figure unverified is not choosing a target
    gate_neutral: true
  rationale: "All four hold → admit rather than park."

scope:
  code: SC1
  serves: ["GATE-P4 4.6", "11-REVIEW_GATES §15"]
  failure_without_it: >
    An Architecture board that adopts Mahesh's persona (as 11 §15 directs) inherits a
    decoupling guarantee the code does not provide, and could approve a plan on the strength of
    it. Gate criterion 4.6 sets a p95 target by measurement; a persona asserting <200ms and
    99.9% as givens is a competing, unsourced target on the same question.
  minimal: true
  authority: "docs/context/README.md — context is non-binding and must not carry targets"

necessity:
  now: SHOULD
  evidence_tier: E1          # the file text itself
  confidence: C5

action: ADMIT
classification: { type: DOC, breakdown: TASK, risk_tier: T1 }

priority:
  now: P3
  at_target: P3
  factors: { N: 2, S: 1, B: 1, R: 2, D: 0, E: 0 }
  score: 12
  matrix_default: P3
  consistency: OK
  rationale: >
    R=2 (material): an incorrect architectural guarantee sitting in a document the review-gate
    model directs agents to adopt feeds an approval mechanism.

scope_boundary:
  in: >
    (a) restate the decoupling guarantee as "no core-domain contract changes; adapter and
    mapping changes remain expected"; (b) source every numeric figure to an SSOT or label it
    illustrative.
  out: >
    Removing the biography/tenure lines is SC2 — it is a voice preference with no named
    beneficiary. Carry it only if the human wants it; it does not justify the edit on its own.

outcome:
  registered_in: "this register"
  status: ADMITTED-QUEUED
```

### SUG-20260810-t3w · `system-state-manifest` + shared constitution

```yaml
id: SUG-20260810-t3w
input: >
  Keep current, universally binding facts in AGENTS.md (module topology, doc hierarchy, build
  commands, security rules, approval boundaries). Create a governed system-state-manifest.md
  with as_of date, commit SHA, implemented vs approved-target vs proposed architecture, active
  decisions and ADR links, open assumptions, owner, last verification date.

action: SUPERSEDED
duplicate_of: "AIGEM adoption (2026-08-07) + docs restructure (PR #30)"
reason: >
  Both already exist and are enforced, not merely written:
    - AGENTS.md is the constitution — governance entry point, module topology, build and
      verification commands, standing rules, mutation and approval boundaries.
    - state/CURRENT-STATE.yaml carries state_as_of, review_due, ratified_by, per-workstream
      scope, standing_constraints, known_open_debt, routing and registers.
    - FreshnessCheck.java enforces staleness and ID uniqueness and returns exit 2 —
      "do not admit new work" — which a hand-maintained manifest cannot do.
    - Source precedence exists: docs/README.md "which document wins", and
      docs/context/README.md marks the whole context tree non-binding with "on conflict, the
      SSOT is correct."
    - Open assumptions have their own register (registers/ASSUMPTION-REGISTER.md, ASM-###);
      decisions have DECISION-REGISTER.md and templates/ADR.md.
stale_premises_in_the_input:
  - >
    The assessment cites docs/contextRoles/* paths and a "19 bounded contexts" mandate in the
    problem statement. That folder no longer exists (now docs/context/roles/), and the phrase
    "19 bounded contexts" does not appear anywhere in
    docs/context/business-problem-statement.md. The nearest text is line 312, which lists
    "PostgreSQL database-per-service" among architecture decisions to uphold.
  - >
    The current-vs-target confusion it describes is therefore narrower than stated, and what
    remains of it is already handled by the non-binding marker on docs/context/.
residual: >
  One genuine gap survives and is recorded above: no workstream covers GOV/DOC work.
```

### SUG-20260810-v6x · "persona-engine-v3" template

```yaml
id: SUG-20260810-v6x
input: >
  A hyper-detailed persona template with mandatory analysis phases, mode detection, handoffs,
  self-criticism, token budgeting, and internally instantiated specialists ("internally
  instantiate a Cyber Security Auditor"). The assessment itself recommends against adopting it
  unchanged; it is triaged so the decision is recorded and not re-argued.

stage_fit: { code: SF4 }
scope:     { code: SC2 }
necessity: { now: REJECT, evidence_tier: E7, confidence: C4 }

action: REJECT
reason: >
  Superseded by existing decisions, which is valid SF4 ground (03 §3):
    - 11 §15 already specifies sequential, in-role board review with a per-board checklist and
      an explicit "do not carry the previous board's conclusion into the next" — a stronger
      and already-ratified version of "simulate a specialist."
    - 11 §2 already answers the independence objection: an agent verdict on its own plan is
      marked self_review: true, a self-reviewed T3 plan needs a human board, and Security and
      Risk & Compliance at T4 require HUMAN with no aggregate override. A simulated specialist
      is not an independent review, and the framework already says so with teeth.
    - Compulsory self-criticism duplicates REVIEW-VERDICT's evidence[] requirement, where an
      APPROVED verdict with empty evidence is invalid (Rule RG-3).
    - Keyword mode detection contradicts 05 §3, which gates interruption on evidenced override
      classes rather than on words in the prompt.
  Adding a fourth mechanism over three that already work is the structure the anti-
  over-engineering tests (16 §6) exist to stop.
reopen_if: >
  Board-verdict quality is measured as inconsistent AND a controlled comparison shows the
  longer template produces better verdicts (E3 evidence, not preference).
```

---

## 4. Seeded from existing artefacts

AIGEM was adopted mid-flight. Rather than backfilling every past decision
([19 §5](../19-PORTING_GUIDE.md#5-bootstrapping-into-an-existing-project-mid-flight)), the
already-deferred items in [TECH-DEBT.md](../../1sb-insurance-integration/service-ssot/TECH-DEBT.md)
were seeded directly into [PARKED-BACKLOG.md](./PARKED-BACKLOG.md) as pre-existing parked work.
They keep their `TD-###` IDs; no `SUG-####` was minted retrospectively.

**Do not re-triage or re-report these** — they are known
([01 §6](../01-CURRENT_STATE.md#6-known-open-debt-affecting-triage)).

---

## 5. Register row convention (machine-enforced)

> **A table row whose first cell is a bare ID is that ID's DEFINITION.** Exactly one definition
> may exist per ID, across every register. `FreshnessCheck` enforces this and halts on a
> duplicate — that is how a cross-branch ID collision is caught after a merge.
>
> Cross-reference rows — the same item shown again in another view, such as an external
> dependency also listed under its edge, or an open risk repeated under accepted risks — must
> **point at** the definition rather than restate the bare ID — for example a leading cell
> of `→ [DEP-002](./DEPENDENCY-REGISTER.md#1-edges)` instead of a bare `DEP-002`.
