# 02 — The Stage Model: Business Idea to Mature Enterprise Platform

**Layer:** L2 — delivery-execution decomposition beneath AIGEM's L1 lifecycle model
**Owner:** Rajal (Product) + Mahesh (Architecture), jointly
**Status:** proposed; binding after CR-010 ratification

---

## 1. Relationship to AIGEM — read this before using the model

AIGEM's [`03-LIFECYCLE.md`](../governance/03-LIFECYCLE.md) defines eleven canonical stages,
**L0–L10**. Those remain the authority for **triage verdicts**. When an agent or a human asks
*"is this suggestion premature?"*, the answer comes from AIGEM's stage-fit codes (SF0–SF4)
evaluated against the L-stage in `CURRENT-STATE.yaml`. Nothing here changes that.

This model does a different job. AIGEM answers **"may we do this now?"** It deliberately does not
answer **"what exactly are we supposed to produce in this stage, who signs it off, and how do we
prove it is finished?"** That is what the sixteen S-stages below provide: a decomposition of each
L-stage into named epics, stories, acceptance criteria, evidence, and validation tests.

> **Rule SM-1 — AIGEM governs admission; the S-model governs completion.**
> On any conflict, AIGEM wins. An S-stage never admits work that AIGEM would park, and never
> parks work AIGEM would admit. If the two disagree, that is a defect in this document and is
> raised as a CR against it.

### Mapping

| AIGEM L-stage | S-stages | Why the split |
|---|---|---|
| L0 Discovery | S00, S01 | Funding the idea and mapping the domain are different decisions with different approvers |
| L1 Business Design | S02, S03, S04, S05 | Regulatory framing, requirements, product slicing and experience design each carry their own gate in a regulated business |
| L2 Domain / Aggregate Design | S06 | 1:1 |
| L3 Technical / Solution Design | S07 | 1:1 — security architecture folded in, not deferred |
| L4 Foundation | **S08, S09** | **The split this repository most needed.** Engineering foundation (code can be built safely) and platform foundation (code can be run safely) are separate capabilities with separate owners, and collapsing them let both go missing |
| L5 Connectivity / Integration | S10 | 1:1 |
| L6 Vertical Slice (MVP) | S11 | 1:1 |
| L7 Hardening | S12 | 1:1 |
| L8 Expansion | S13 | 1:1 |
| L9 Production Readiness | S14 | 1:1 |
| L10 Operate & Evolve | S15 | 1:1 |

---

## 2. The sixteen stages

```
   BUSINESS                    DESIGN                      FOUNDATION
 ┌──────────┬──────────┐ ┌──────────┬──────────┐  ┌──────────┬──────────┐
 │   S00    │   S01    │ │ S02  S03 │ S04  S05 │  │   S06    │   S07    │
 │ Ideation │Discovery │ │ Reg. BRD │ Prod  UX │  │  Domain  │ Solution │
 └──────────┴──────────┘ └──────────┴──────────┘  └──────────┴──────────┘
                                                          │
        ┌─────────────────────────────────────────────────┘
        ▼
 ┌──────────┬──────────┐ ┌──────────┬──────────┐  ┌──────────┬──────────┐
 │   S08    │   S09    │ │   S10    │   S11    │  │   S12    │   S13    │
 │Engineer. │ Platform │ │Integrat. │  Vertical│  │ Harden & │  Expand  │
 │Foundation│Foundation│ │          │  Slice   │  │ Certify  │  & Scale │
 └──────────┴──────────┘ └──────────┴──────────┘  └──────────┴──────────┘
                                                          │
        ┌─────────────────────────────────────────────────┘
        ▼
 ┌──────────┬──────────┐
 │   S14    │   S15    │
 │  Go-Live │ Operate  │
 └──────────┴──────────┘
```

| # | Stage | Central question | Primary owner | Premature here |
|---|---|---|---|---|
| **S00** | Ideation & Business Case | Should we fund this at all? | Sponsor + Rajal | Any architecture |
| **S01** | Discovery & Capability Definition | What problem, for whom, and what capabilities does it need? | Rajal | Technology selection |
| **S02** | Regulatory, Risk & Compliance Framing | What are we legally obliged to do, and what may we never do? | Shailja | Implementation of controls |
| **S03** | Business Requirements & Process Design | What behaviour do we promise, precisely enough to test? | Rajal + BA | Service decomposition |
| **S04** | Product Definition & Release Slicing | What is in R0, and what is deliberately not? | Rajal | Building anything |
| **S05** | Experience Design & Service Blueprint | What does the human actually see and do? | Digital + Rajal | Production-grade UI code |
| **S06** | Domain & Information Architecture | What are the concepts, their lifecycles and invariants? | Mahesh + Aarti | Messaging, caching, tuning |
| **S07** | Solution & Security Architecture | How will it be built, and how will it be safe? | Mahesh + Deepali | Capacity planning |
| **S08** | **Engineering Foundation** | **Can we build, test and prove code safely?** | Amit + Swapnali | Feature breadth |
| **S09** | **Platform & Environment Foundation** | **Can we run, observe and recover it safely?** | Shivanshi + Deepali | Autoscaling, multi-region |
| **S10** | Integration & Connectivity | Can we talk to the outside world reliably? | Amit + Mahesh | Multi-LOB expansion |
| **S11** | Vertical Slice (MVP) | Does one complete business journey work end to end? | Rajal + Amit | A second journey |
| **S12** | Hardening & Certification | Is it correct, safe, compliant and provable? | Swapnali + Deepali + Shailja | New features |
| **S13** | Expansion & Scale | Does it generalise without rework? | Rajal + Mahesh | Rearchitecting what works |
| **S14** | Production Readiness & Go-Live | Can we launch and survive launch? | Shivanshi + Kalpana | Broad new scope |
| **S15** | Operate, Evolve & Continuous Assurance | Is it healthy, improving and still compliant? | Shivanshi + Rajal | Unbounded rewrites |

---

## 3. Stage anatomy — what every stage file contains

Each `stages/Sxx-*.md` file is written to the same shape, so a reader who learns one has learned
all sixteen:

```yaml
stage:
  id: S08
  name: "Engineering Foundation"
  aigem_stage: "L4 — Foundation"
  central_question: "Can we build, test and prove code safely?"

  entry_criteria: []        # what must be true before this stage may start
  epics: []                 # 4–8 epics, each with stories
  validation_tests: []      # VT-xxx — how you PROVE the stage is complete
  exit_gate:
    criteria: []            # checkable, each with a named evidence artefact
    approvers: []           # personas whose sign-off is required
  premature_here: []        # what belongs to a later stage
  current_position: ""      # where THIS repository stands, with evidence
```

### The four artefact classes

Every stage produces artefacts in four classes. A stage that produces only the first is
documentation theatre; a stage that produces only the third is undisciplined engineering.

| Class | Question it answers | Examples |
|---|---|---|
| **Decision** | What did we choose, and why? | ADR, decision-register entry, approved CR |
| **Definition** | What are we obliged to deliver? | BRD, PRD, rule pack, NFR sheet, threat model |
| **Construction** | What did we build? | Code, IaC, pipelines, dashboards, migrations |
| **Evidence** | How do we know it works? | CI run, scan report, test report, restore test, sign-off |

> **Rule SM-2 — Every exit criterion cites an Evidence-class artefact.**
> A Definition artefact may *satisfy* a criterion only when the criterion is itself about
> definition (e.g. "the consent rule pack exists and is approved"). No criterion about system
> behaviour may be closed by a document that asserts the behaviour.

---

## 4. Identifier scheme

Stable IDs are what make this importable into Jira and traceable in a regulatory audit.

```
S08                    Stage
S08-E02                Epic 2 of stage S08
S08-E02-S03            Story 3 of that epic
S08-E02-S03-AC1        Acceptance criterion 1 of that story
S08-VT-04              Validation test 4 for stage S08
S08-G1                 Exit gate criterion 1 for stage S08
```

Jira mapping: **Stage → Jira Version/Fix Version**, **Epic → Jira Epic**, **Story → Jira Story**,
**Validation test → Jira Test issue (or Xray/Zephyr test)**, **Gate criterion → Jira Gate task
with the stage epic as parent**. Full mapping in [`09-JIRA-MODEL.md`](./09-JIRA-MODEL.md).

These IDs are **immutable once published**. A story that is dropped is marked `WITHDRAWN`, never
deleted and never renumbered — a regulator asking "what happened to S12-E03-S02" must get an
answer.

---

## 5. Movement rules

### 5.1 Stages overlap; gates do not

Stages are not a waterfall. S06 domain design and S08 engineering foundation can and should run
concurrently. What may never overlap is a **gate**: you cannot pass S11's gate while S08's gate is
open, because S11's evidence is produced by S08's machinery.

### 5.2 Dependency, not sequence

| Stage | Hard prerequisites (gate-level) |
|---|---|
| S03 | S02 passed — you cannot write testable requirements for a regulated product before you know the obligations |
| S06 | S03 passed |
| S07 | S06 passed |
| S08 | S07 passed (architecture must exist before you build the pipeline that enforces it) |
| S09 | S07 passed |
| S10 | S08 **and** S09 passed |
| S11 | S08, S09, S10 passed; S05 passed; **no open P0 business gap** |
| S12 | S11 passed |
| S13 | S12 passed |
| S14 | S12 passed; S13 passed for everything in the launch scope |
| S15 | S14 passed |

### 5.3 The back-fill rule

> **Rule SM-3 — A stage may be entered late, but never skipped.**
> When work has already been done at a later stage than the foundation supports — exactly this
> repository's situation — the earlier stage is entered **retroactively**: its epics are executed
> against the code that already exists, and its gate is assessed against the current system, not
> a hypothetical one. This is called **underpinning**, and it is the subject of
> [`03-REALIGNMENT-PLAN.md`](./03-REALIGNMENT-PLAN.md).
>
> Retroactive entry costs more than entering in order — retrofitting CI onto 20,000 untested
> lines is harder than growing it alongside 200. That cost is the price of the original ordering
> and it is paid, not avoided.

### 5.4 Freeze semantics

When a stage gate is `CANDIDATE`, AIGEM's freeze rule applies: only SF0 and P1-override work is
admitted ([04-STAGE_GATES §3](../governance/04-STAGE_GATES.md)). This model adds one more:

> **Rule SM-4 — An open P0 business gap freezes entry to S11 and everything after it.**
> Not S08, not S09 — foundation work continues, because foundation work is what makes the gap
> closable. But no vertical slice, no hardening and no launch proceeds over an open P0.

---

## 6. Reading your position off this model

Someone joining the programme should be able to answer three questions in under five minutes.

**"Where are we?"** → [`README.md`](./README.md) position banner, backed by
[`01-POSITION-ASSESSMENT.md §5`](./01-POSITION-ASSESSMENT.md).

**"What am I supposed to be doing?"** → open `stages/Sxx-*.md` for the current stage; find your
persona in the epic ownership column; your stories are listed with acceptance criteria.

**"How do I know when this stage is done?"** → the same file's *Exit gate* section: every
criterion, its evidence artefact, and who signs.

If any of those three takes longer than five minutes, this bible has failed at its only job and
the fix is a CR against it — not a workaround.
