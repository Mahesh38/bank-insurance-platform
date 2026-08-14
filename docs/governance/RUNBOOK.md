# AIGEM Operating Runbook

**This is the only governance file most people need to read.**
Everything else is reference. If you read one page, read your role card in §6.

**Version:** 1.1 · **Owner:** **Kalpana — Delivery Head / Delivery Lead (R12)** · **Original ratification:** 2026-08-10, Mahesh (Solution Architect) · **R12 assignment:** CR-007  
**Framework:** [docs/governance/](./README.md) · **Live state:** [state/CURRENT-STATE.yaml](./state/CURRENT-STATE.yaml) · **Delivery control:** [DELIVERY-CONTROL-SYSTEM.md](./DELIVERY-CONTROL-SYSTEM.md)

---

## Contents

| § | Section | Read if you are… |
|---|---------|------------------|
| 1 | [Why this exists](#1-why-this-exists) | everyone, once |
| 2 | [Stakeholder roster](#2-stakeholder-roster--who-owns-what) | everyone, once |
| 3 | [Cadence master table](#3-cadence-master-table) | everyone, weekly |
| 4 | [Maintenance & staleness matrix](#4-maintenance--staleness-matrix) | **artefact owners — this is the critical table** |
| 5 | [Constant vs project-specific](#5-constant-vs-project-specific) | anyone starting a new repo |
| 6 | [Role cards](#6-role-cards) | **you — find your card** |
| 7 | [Ceremonies](#7-ceremonies) | anyone attending them |
| 8 | [What the AI agent must know](#8-what-the-ai-agent-must-know-about-this-project) | **everyone — this is what makes agents useful** |
| 9 | [Escalation](#9-escalation) | when stuck |
| 10 | [Adoption: first 14 days](#10-adoption-first-14-days) | now |

---

## 1. Why this exists

The framework answers *how to decide*. This runbook answers *who does what, how often, and
what breaks if they don't*.

Three failure modes it prevents:

| Failure | Symptom | Control here |
|---------|---------|--------------|
| Nobody knows their job | The framework is read once and never used | Role cards (§6) — one card, one screen, exact actions |
| The state file goes stale | The AI confidently triages against a stage we left two months ago | Maintenance matrix (§4) + freshness check + hard halt rule |
| The governance becomes documentation | Registers stop being updated; parking becomes deletion | Cadence table (§3) with named owners and time budgets |

**Total governance overhead per person:** 5 minutes daily, 30 minutes weekly, 90 minutes per
stage gate. If it costs more than that, the process is wrong — say so
([18 §4](./18-GOVERNANCE_METRICS.md#4-reading-the-numbers)).

---

## 2. Stakeholder roster — who owns what

Personas: [docs/context/roles/](../context/roles/README.md)

| # | Role | Named | Owns in AIGEM | Board seat ([11](./11-REVIEW_GATES.md)) | Can veto | Can approve a gate |
|---|------|-------|---------------|------------------------------------------|:--------:|:------------------:|
| R1 | **Product Owner** | **Rajal** | Scope, necessity disputes, priority ties, backlog | Product | — | ✅ (with Architect) |
| R2 | **Solution Architect** | **Mahesh** | Framework custodian, stage transitions, ADRs, standing constraints | Architecture | ADR-overridable | ✅ (with PO) |
| R3 | **Technical Head** | **Amit** | Engineering standards, CI gates, infra, debt ledger oversight, SLAs | Technical + Operations | — | ✅ for technical exit |
| R4 | **Team Lead / Tech Lead** | per workstream | Work breakdown, task assignment, technical verdicts, debt entries | Technical | — | ✅ marks items Done |
| R5 | **Developer** | per task | One work item at a time, plan adherence, same-PR tests | — | — | — |
| R6 | **QA Engineer** | per story | Scenario packs, defect clarity, retest | — | — | — |
| R7 | **QA Lead** | — | Test strategy, coverage gates, quality sign-off | QA | — | ✅ for quality exit |
| R8 | **Security Architect** | — | Security verdicts, secrets, PII, attack surface | Security | ✅ **binding** | ✅ for T4 |
| R9 | **Risk & Compliance** | — | Regulatory verdicts, consent, retention, audit | Risk & Compliance | ✅ **binding** | ✅ for regulated items |
| R10 | **DevOps / SRE** | — | Deployability, observability, runbooks, rollback | Operations | — | — |
| R11 | **Business Analyst** | — | Requirement clarity, acceptance criteria quality | Product (delegate) | — | — |
| R12 | **Delivery Head / Delivery Lead** | **Kalpana** | **This runbook, CURRENT-STATE.yaml, register hygiene, gate cadence, metrics + integrated delivery orchestration** | — | — | marks gate `CANDIDATE` only |
| R13 | **AI Agent** | Claude / Cursor / Copilot | Running the pipeline, producing records, executing approved plans | Simulates boards ([11 §2](./11-REVIEW_GATES.md#2-who-may-sit-on-a-board)) | — | ❌ **never** |

> **Canonical Delivery identity:** **Kalpana is R12.** `Delivery Head`, `Delivery Lead`, `Program Delivery Director`, `Enterprise Delivery Head` and `R12` are aliases for this one persona. Do not create a second Delivery agent. R12 is an orchestration role across the existing seven boards; marking a gate `CANDIDATE` is preparation for the authorised decision-makers, **not gate approval**.

**Small team?** Roles collapse, ownership does not. Minimum viable assignment:
PO → R1+R11 · **Kalpana → R12** · Architect → R2+R8 · Tech Head → R3+R4+R10 · QA Lead → R6+R7 ·
Compliance → R9 (part-time, gate-only).

---

## 3. Cadence master table

Everything anyone does, in one table. Find your role in the last column.

### Event-triggered (no schedule — happens when it happens)

| Trigger | Action | Who | Time | Output |
|---------|--------|-----|------|--------|
| Any new input arrives | Triage: steps 0–5 of the pipeline | Whoever receives it (usually the **agent**) | 2–10 min | `SUG-####` row |
| Verdict = ADMIT | Classify, score, map dependencies, route to a backlog | Agent + **TL** | 15 min | Backlog entry |
| Verdict = PARK | Register with target stage + unpark trigger | Agent | 2 min | Parked row |
| Verdict = ESCALATE | Raise `CR-###`, stop | Agent → **PO + Architect** | 20 min | Change request |
| Work item picked up | Pre-flight check ([12 §6](./12-DEFINITION_OF_READY.md#6-agent-pre-flight)) | **Dev / agent** | 3 min | — |
| Plan written (T2+) | Board review for the risk tier | **Boards** | 20–60 min | Verdicts |
| Drift signal fires | Stop, classify, revert or log variance | **Dev / agent** | 5 min | Variance or `SUG-####` |
| Work item finished | DoD evidence table, register closure | **Dev / agent → TL** | 10 min | Evidence + closed `SUG` |
| Debt taken | Ledger entry with owner + expiry | **TL** | 5 min | `TD-###` |
| Assumption invalidated | Re-validate every item citing it | **Owner** | 15 min | Updated items |
| Incident | Search registers for items that would have prevented it | **Kalpana / R12** | 30 min | Calibration note |
| Material release/initiative admitted | Establish milestones, dependencies, critical path, forecast assumptions | **Kalpana / R12** + owners | varies | DCS delivery view |
| Delivery decision approaches required-by date | Escalate decision owner with options, impact and consequence of delay | **Kalpana / R12** | 10–20 min | Decision/escalation record |

### Daily — 5 minutes

| Action | Who | Output |
|--------|-----|--------|
| Confirm the item you are working on is still head of the ordered READY queue | Dev, TL, agent | — |
| Register any suggestion raised yesterday that is not yet written down | everyone | `SUG-####` rows |
| Check nothing is `IN-FLIGHT` and untouched for > 2 days | TL | Unblock or re-park |
| For material delivery periods: review critical path, new blockers and decisions consuming slack | **Kalpana / R12** | Intervention/escalation only if needed |

### Weekly — 30 minutes (the **Governance Sync**, §7.1)

| Action | Who | Output |
|--------|-----|--------|
| Refresh gate criteria states in [04](./04-STAGE_GATES.md) + the state file | **Kalpana / R12** | Updated `CURRENT-STATE.yaml` |
| Run the freshness check (§4.3) | **Kalpana / R12** | Stale-artefact list |
| Review new `SUG` rows; confirm or correct verdicts | **PO + Architect** | Corrected rows |
| Review BLOCKED items; chase external dependencies | **TL + PO** | Owner + date per `EXTERNAL` edge |
| Re-order the READY queue | **TL** | Ordered queue |
| Confirm one-active-item discipline held | **TL** | — |
| Review critical path, ageing dependencies/decisions, milestone evidence and forecast confidence | **Kalpana / R12** + relevant owners | Updated DCS delivery view |

### Per work item

| Action | Who |
|--------|-----|
| Definition of Ready check before start | Dev / agent |
| Plan + board verdicts before code (T2+) | Author + boards |
| Drift checks during, pre-PR check before opening | Dev / agent |
| DoD evidence + governance closure at the end | Dev / agent → TL |

### Per stage gate — 90 minutes (the **Gate Review**, §7.2)

| Action | Who | Output |
|--------|-----|--------|
| Attach evidence to every exit criterion | **Criterion owners** | Evidence table |
| Mark gate `CANDIDATE`; freeze non-P1 admissions | **Kalpana / R12** | Gate candidate state |
| Approve or reject the transition | **Architect + PO** (+ QA Lead, Compliance, Ops as listed) | Signed transition |
| **Unpark sweep** — re-triage every item whose trigger fired | **Kalpana / R12 + TL** | Promoted / re-parked / closed |
| **Recompute priorities** for all READY and PARKED items | **PO + TL** | Updated priorities |
| Debt expiry enforcement | **TL** | Repaid / waived / converted to ADR |
| Assumption validation pass | **Owners** | Validated / invalidated |
| Gate scorecard | **Kalpana / R12** | [18 §3](./18-GOVERNANCE_METRICS.md#3-gate-scorecard) |
| Update `CURRENT-STATE.yaml` to the new stage | **Architect + PO** | New state |

> **`CANDIDATE` is not approval.** Kalpana/R12 prepares and orchestrates the decision; the listed Product, Architecture and specialist/human authorities retain the actual transition/sign-off rights.

> **Priority recomputation at every stage change is not optional.** Priority is stage-relative
([05 §1](./05-PRIORITY_MODEL.md#1-the-governing-insight)); a stage change silently invalidates
every stored `priority_now` in the repository.

### Monthly — 45 minutes (**Register Hygiene**, §7.3)

| Action | Who |
|--------|-----|
| Merge duplicate suggestions; update `recurrence_count` | Kalpana / R12 |
| Age the parked backlog (AS-2 / AS-3) | Kalpana / R12 |
| Reconcile parked backlog against the debt ledger — nothing in both | TL |
| Review metric trends, not values | Kalpana / R12 + Tech Head |
| Confirm every `EXTERNAL` dependency still has a live owner and date | PO |
| Review delivery forecast accuracy, blocked-time and decision/dependency ageing | Kalpana / R12 | DCS calibration |

### Quarterly — 60 minutes

| Action | Who |
|--------|-----|
| Review [ORG-STANDARDS.md](./ORG-STANDARDS.md) against actual org policy | Architect + Security + Compliance |
| Close ratification-backlog items | Architect |
| Review whether the framework itself needs a `GOV` change request | Architect + PO |
| Calibration: were our parks and rejections right? | PO + Architect + Kalpana / R12 |
| Calibration: were delivery forecasts, fast-track choices and dependency escalations accurate? | Kalpana / R12 + Tech Head | DCS improvements |

---

## 4. Maintenance & staleness matrix

**This is the table that keeps the AI correct.** Every artefact has an owner, an update
trigger, and a staleness limit. Past the limit, the artefact is not merely old — it is
actively misleading, because agents will trust it.

### 4.1 Live project state (L3) — highest decay rate

| Artefact | Owner | Update trigger | Cadence | Max staleness | What breaks when stale |
|----------|-------|----------------|---------|---------------|------------------------|
| **[state/CURRENT-STATE.yaml](./state/CURRENT-STATE.yaml)** | **Kalpana / R12** | Stage change · scope change · gate criterion state change | Reviewed **weekly**, updated on every trigger | **30 days** | 🔴 **Everything.** Agents triage against the wrong stage. Rule CS-1 halts triage past `review_due` |
| [01-CURRENT_STATE.md](./01-CURRENT_STATE.md) | Kalpana / R12 | With the YAML | Weekly | 30 days | Standing constraints and known-debt lists go wrong; agents re-report known debt |
| [02-PROJECT_SCOPE.md](./02-PROJECT_SCOPE.md) | **PO (Rajal)** | Approved `CR-###` only | Per CR + confirmed per gate | 1 stage | 🔴 Scope verdicts (SC0–SC4) become unreliable — the main creep defence |
| [04-STAGE_GATES.md](./04-STAGE_GATES.md) | **Architect (Mahesh)** | Criterion state change | Weekly | 14 days | SF0 claims cannot be verified; "this is blocking" becomes unfalsifiable |
| [03-LIFECYCLE.md](./03-LIFECYCLE.md) §6 stage map | Architect | New phase or workstream | Per stage | 1 stage | Stage-fit maps to the wrong canonical stage |

### 4.2 Registers

| Register | Owner | Update trigger | Cadence | Max staleness | What breaks |
|----------|-------|----------------|---------|---------------|-------------|
| [SUGGESTION-REGISTER](./registers/SUGGESTION-REGISTER.md) | Whoever triages | **Every input** | Continuous | 7 days | Duplicate work; ideas lost; the model's memory fails |
| [PARKED-BACKLOG](./registers/PARKED-BACKLOG.md) | Kalpana / R12 | Every park + every gate sweep | Per park, swept per gate | 2 gates | 🔴 Parking becomes deletion — the promise this model makes |
| [DEPENDENCY-REGISTER](./registers/DEPENDENCY-REGISTER.md) | **TL** | Every triage with edges | Weekly | 14 days | Work starts out of order; rework |
| [RISK-REGISTER](./registers/RISK-REGISTER.md) | Kalpana / R12 | New risk · gate review | Per gate | 1 stage | Risk factor `R` in scoring is wrong |
| [ASSUMPTION-REGISTER](./registers/ASSUMPTION-REGISTER.md) | Assumption author | Validation · expiry date | Per gate | Its own `expiry` | MUST claims resting on dead assumptions (EV-1) |
| [DECISION-REGISTER](./registers/DECISION-REGISTER.md) | **Architect** | Every ADR · CR · stage transition | Per event | — | Closed decisions get re-litigated |

The DCS may present integrated views over these registers, but it must not create private duplicate sources of truth.

### 4.3 Existing repository artefacts (unchanged owners)

| Artefact | Owner | Cadence | Max staleness |
|----------|-------|---------|---------------|
| [PRODUCT-BACKLOG.md](../1sb-insurance-integration/service-ssot/PRODUCT-BACKLOG.md) | PO | Per admitted item | 1 stage |
| [TECH-DEBT.md](../1sb-insurance-integration/service-ssot/TECH-DEBT.md) | TL | Per debt taken/closed; expiry enforced per gate | 1 stage |
| [TEST-BACKLOG.md](../1sb-insurance-integration/service-ssot/TEST-BACKLOG.md) | QA Lead | Per QA item | 1 stage |
| [ACTION-PLAN.md](../1sb-insurance-integration/service-ssot/ACTION-PLAN.md) | Architect + PO | Per stage | 1 stage |
| Phase `STATUS.md` | TL | Per work item close | 7 days |

### 4.4 Framework files (L1/L2) — low decay

| Artefact | Owner | Trigger | Cadence |
|----------|-------|---------|---------|
| `00`, `03`, `05`–`19` (L1) | Architect | `GOV` change request only | As needed |
| [ORG-STANDARDS.md](./ORG-STANDARDS.md) (L2) | Architect + Security + Compliance | Org policy change | Quarterly review |
| This runbook | Kalpana / R12 | Role or cadence change | Quarterly review |
| [DELIVERY-CONTROL-SYSTEM.md](./DELIVERY-CONTROL-SYSTEM.md) | Kalpana / R12 | Delivery-orchestration change; constitutional changes require `GOV` CR | Quarterly review |

### 4.5 The staleness alarm

```bash
java scripts/governance/FreshnessCheck.java     # JDK 21 + Git only; no build, no deps
./gradlew governanceFreshness                  # equivalent, via the wrapper
```

Run it: weekly (Kalpana / R12), at every gate, and **automatically by any agent at session
start**. Exit codes: `0` fresh · `1` warnings · `2` halt-class staleness.

It **fails closed**: anything it cannot fully parse or resolve is a halt, never a silent pass.
That is deliberate — a checker that shrugs at a malformed state file is worse than none, because
it manufactures confidence. Its own behaviour is covered by fixtures:

```bash
bash scripts/governance/test-freshness-check.sh   # 24 cases, incl. every CS-1 halt
```

**Agent behaviour on staleness** — this is the rule that protects every downstream decision:

| Condition | Agent must |
|-----------|-----------|
| `CURRENT-STATE.yaml` missing, malformed, or using YAML the reader does not implement | **HALT triage.** Report. Do not guess (Rule CS-1) |
| `state_as_of` older than `review_due`, or either missing | **HALT ADMIT decisions.** May still PARK and REJECT. Say the state is stale in every reply |
| A required structure is absent — no workstreams, no lifecycle, no gate criteria | **HALT.** Context cannot be resolved, so no verdict is sound |
| `provisional: true` (unratified) | May PARK/REJECT freely; ADMIT only what an authority document already lists; else ESCALATE ([01 §7](./01-CURRENT_STATE.md#7-ratification-status)) |
| A sequential ID counter is behind its register | **HALT.** The next mint would collide and corrupt backlinks |
| A register ID is defined twice | **HALT.** Two items share an identity — usually a merge of parallel branches |
| A gate criterion has no state | Treat as OPEN; do not claim SF0 against it |
| Suggestion register untouched > 7 days | Warn once; check for unregistered work in recent commits |

### 4.6 What runs where

| Check | Runtime | Who runs it | When |
|-------|---------|-------------|------|
| `FreshnessCheck.java` | **JDK 21 + Git** — the documented baseline | Agents, Kalpana / R12, CI, Gradle | Session start · weekly · every gate · every PR |
| `test-freshness-check.sh` | Bash + JDK | CI | Every PR touching governance |
| `ci-checks.py` — schemas, tagged records, links, calibration | Python + PyYAML + jsonschema | **CI only** | Every PR touching governance; weekly |

The split is the point: the **mandatory** check has no dependencies beyond what this repository
already requires, so "run the freshness check first" is an instruction an agent can always
follow. Schema validation is richer but optional tooling, so it lives in CI and never blocks
someone whose machine lacks Python.

---

## 5. Constant vs project-specific

When this model moves to another repository, this table tells you exactly what to change.

| Constant across every repo (copy unchanged) | Project-specific (rewrite every time) |
|---|---|
| The 10-step pipeline | Which lifecycle stage you are in |
| SF0–SF4 stage-fit codes and their tests | The phase list and stage map ([03 §6](./03-LIFECYCLE.md#6-project-stage-map-l3)) |
| SC0–SC4 scope-fit codes | The actual in-scope / out-of-scope lists |
| MUST/SHOULD/COULD/NOT-NOW/REJECT + the MUST test | Which items are MUST *here* |
| `SCORE = 2N+2S+2B+2R+D−E`, bands, caps PRI-1…PRI-7 | Nothing — the maths is universal |
| The 8 hard P1 overrides | Nothing |
| The 11 dependency types and 6 relations | The actual edges |
| The 16 work types and the classification tree | Which backlog file each type routes to |
| Epic/story test; the 2-trigger epic rule | Your epics |
| The 7 boards and their checklists | Who sits on each board |
| Risk tiers T1–T4 and the proportionality matrix | Your T4 triggers if your regime differs |
| DoR (R1–R15) and universal DoD (D1–D12) | Per-work-type DoD additions; your CI commands |
| The 14 drift signals and the return protocol | Nothing |
| Templates and JSON schemas | Nothing |
| Register **formats** | Register **contents** |
| Role definitions and cadences (this runbook §3, §6) | Names against roles; meeting times |
| The metric definitions ([18](./18-GOVERNANCE_METRICS.md)) | Your targets, if regulated differently |
| **Never**: standing constraints as a concept | **Always**: the constraints themselves |
| L2 [ORG-STANDARDS.md](./ORG-STANDARDS.md) | Per organization, not per repo — adapt once, reuse |

**Rule of thumb:** if it names a phase, a service, a person, a file path, or a backlog, it is
project-specific. Everything else travels. Full procedure:
[19-PORTING_GUIDE.md](./19-PORTING_GUIDE.md).

---

## 6. Role cards

One card per role. Find yours. Everything you must do is on it.

---

### R1 · Product Owner — Rajal

**Your one job:** keep *scope* and *necessity* true, so the AI's SC and necessity verdicts mean
something.

**You own:** [02-PROJECT_SCOPE.md](./02-PROJECT_SCOPE.md) · [PRODUCT-BACKLOG.md](../1sb-insurance-integration/service-ssot/PRODUCT-BACKLOG.md) · necessity disputes · priority ties · `CR` approval (with Architect)

| When | Do | Time |
|------|----|------|
| Per input (disputed) | Settle MUST vs SHOULD when the agent and TL disagree | 5 min |
| Per ADMIT | Confirm the backlog entry reflects real product intent | 5 min |
| Weekly | Review new `SUG` rows; correct wrong scope verdicts | 15 min |
| Weekly | Confirm every `EXTERNAL` dependency has a named owner and a date | 5 min |
| Per gate | Recompute priorities with the TL; approve the transition | 45 min |
| Per gate | Confirm §6 of 02-PROJECT_SCOPE still matches reality | 10 min |
| Per CR | Approve or reject scope changes; record the reason | 20 min |
| Monthly | Rule AS-2 review: parked items that survived two gates | 15 min |

**Only you can:** change scope · overrule a necessity classification · accept an SC4 mandate
into scope · approve a rejection reversal.

**Never:** approve a scope change verbally · let a parked item age without a decision · answer
"is this in scope?" without updating the scope file when the answer surprises you.

**Your 60-second check:** *Can I point at the document that says this is in scope? If not, it
isn't — or I need to change the document.*

---

### R2 · Solution Architect — Mahesh

**Your one job:** own the framework and the stage. You are the only person who can say "we have
moved stage", and the only one who can say "that violates a standing constraint".

**You own:** [04-STAGE_GATES.md](./04-STAGE_GATES.md) · [03-LIFECYCLE.md](./03-LIFECYCLE.md) §6 · [DECISION-REGISTER](./registers/DECISION-REGISTER.md) · standing constraints · all L1 framework files · the Architecture board

| When | Do | Time |
|------|----|------|
| Per T3/T4 plan | Architecture board verdict, checks A1–A10, with evidence | 20 min |
| Per architectural decision | Write the ADR; index it | 40 min |
| Weekly | Update gate criterion states in 04 | 10 min |
| Weekly | Review `SUG` rows classified SF3/SF4 — is the target stage right? | 10 min |
| Per gate | Approve the transition (with PO); sign the new state | 45 min |
| Per gate | Convert expired-but-permanent debt into ADRs | 15 min |
| Quarterly | Review ORG-STANDARDS; close ratification items | 60 min |
| On cycle detection | Resolve dependency cycles ([07 §6](./07-DEPENDENCY_MODEL.md#6-cycles)) | 30 min |

**Only you can:** declare a stage transition (with PO) · change a standing constraint · override
an Architecture REWORK (via a recorded ADR) · amend L1 framework files.

**Never:** let an agent edit stage fields · approve a new runtime component without an ADR
stating its operational cost · resolve a board conflict verbally.

**Your 60-second check:** *Does this belong here, shaped like this, at this stage — and if we
are wrong, what does it cost to undo?*

---

### R3 · Technical Head — Amit

**Your one job:** make the gates real. Coverage, CI, infra and SLAs are what turn "Done" from an
assertion into evidence.

**You own:** engineering standards · CI gates · infra · Technical + Operations boards · debt-ledger oversight · [18 metrics](./18-GOVERNANCE_METRICS.md) review

| When | Do | Time |
|------|----|------|
| Per T3/T4 plan | Technical board verdict (T1–T8) | 20 min |
| Per plan with `operational_impact` | Operations board verdict (O1–O8) | 15 min |
| Weekly | Confirm CI gates are green and are actually enforcing | 10 min |
| Per gate | Technical exit sign-off; confirm no open P0/P1 debt | 30 min |
| Per gate | Enforce debt expiry with the TL | 20 min |
| Monthly | Review metric **trends** with Kalpana / R12 | 30 min |
| Quarterly | Confirm coverage floors are still rising, not "interim" forever | 20 min |

**Only you can:** change CI gate thresholds · accept an infrastructure dependency · set the
engineering standards the Technical board cites.

**Never:** allow "interim" to mean permanent without a dated schedule · approve a plan whose
rollback is untested · let a P0 debt item cross a gate.

**Your 60-second check:** *If this fails at 2am, can someone see it, and can someone undo it?*

---

### R4 · Team Lead / Tech Lead

**Your one job:** turn admitted work into ordered, unambiguous tasks — and be the only person
who marks things Done.

**You own:** work breakdown · assignment · [DEPENDENCY-REGISTER](./registers/DEPENDENCY-REGISTER.md) · [TECH-DEBT.md](../1sb-insurance-integration/service-ssot/TECH-DEBT.md) · the READY queue · Technical board (day to day)

| When | Do | Time |
|------|----|------|
| Per ADMIT | Classify, break down, size, assign one owner | 15 min |
| Per work item | Confirm DoR before anyone starts | 5 min |
| Per plan (T2) | Technical verdict | 10 min |
| Per completion | Verify DoD evidence; mark Done | 10 min |
| Daily | Check nothing is in flight and untouched > 2 days | 3 min |
| Weekly | Re-order the READY queue; update dependency edges | 15 min |
| Per debt taken | Ledger entry with owner, severity, expiry | 5 min |
| Per gate | Debt expiry pass; unpark sweep with Kalpana / R12 | 30 min |

**Only you can:** move an item to Done · assign ownership · open and close debt entries.

**Never:** assign work that fails DoR · accept "coding finished" as complete · approve while
must-fix comments are open · let a TODO ship without a work item ID.

**Your 60-second check:** *Can the next person act on this without asking me a question?*

---

### R5 · Developer

**Your one job:** finish the item you started, exactly as planned.

**You own:** your one in-flight work item · its tests · its evidence

| When | Do | Time |
|------|----|------|
| Before starting | Pre-flight check ([12 §6](./12-DEFINITION_OF_READY.md#6-agent-pre-flight)) | 3 min |
| Before coding (T2+) | Read the plan: `files_expected`, `out_of_scope`, AC | 5 min |
| During | Drift check before each edit outside `files_expected` | 30 s |
| During | Notice something? Write `SUG-####`, keep going | 2 min |
| Before PR | Pre-PR drift check ([17 §5](./17-DRIFT_CONTROL.md#5-pre-pr-drift-check)) | 5 min |
| At the end | DoD evidence table | 10 min |
| Daily | Confirm your item is still the right one | 1 min |

**Never:** hold two items at once · fix something adjacent "while you're here" · add a
dependency not in the plan · change tests to make them pass · widen the plan after the fact to
match what you already changed.

**Your 60-second check:** *Every file I touched — is it in `files_expected`, or did I log a
variance?*

**Your one phrase:** "Noted as `SUG-00NN`. Continuing with `<item>`."

---

### R6 · QA Engineer

**Your one job:** prove behaviour against AC — including the negative cases nobody wants to
write.

| When | Do | Time |
|------|----|------|
| Per functional story | Scenario pack mapped 1:1 to AC | 30 min |
| Per story | Execute on an agreed SHA; record pass/fail/blocked | varies |
| Per defect | File with expected vs actual, IDs, severity | 10 min |
| Per fix | Retest on the new SHA + adjacent smoke | 20 min |
| Per gate | Phase regression evidence | 60 min |

**Never:** happy-path only when the AC lists error, timeout, or idempotency cases · close a bug
without retest on the fix commit · use real PII in fixtures.

**Your 60-second check:** *Which AC does each scenario prove — and which AC has no scenario?*

---

### R7 · QA Lead

**Your one job:** make quality gateable, and answer "what is still untested?" without anyone
having to ask you.

**You own:** [TESTING-RULES.md](../1sb-insurance-integration/service-ssot/TESTING-RULES.md) · [TEST-BACKLOG.md](../1sb-insurance-integration/service-ssot/TEST-BACKLOG.md) · [COVERAGE.md](../1sb-insurance-integration/service-ssot/COVERAGE.md) · the QA board

| When | Do | Time |
|------|----|------|
| Per T2+ plan | QA board verdict (Q1–Q8) | 15 min |
| Per functional story | Quality sign-off or "not ready", in writing | 15 min |
| Weekly | Groom the test backlog | 15 min |
| Per gate | Quality gate verdict; confirm coverage floors | 30 min |
| Per waiver | Co-approve with TL; **always** set an expiry | 10 min |

**Never:** give a verbal LGTM · approve while coverage verification is red without a dated
waiver · raise a gate the team has no backlog path to meet.

**Your 60-second check:** *Is every AC observable, and is the negative case covered?*

---

### R8 · Security Architect

**Your one job:** the binding veto. Nothing that changes who can do what, or what data is
exposed, ships without you.

| When | Do | Time |
|------|----|------|
| Per plan with `security_impact` ≠ none | Security board verdict (S1–S10) | 20 min |
| **Every T4 plan** | Verdict — **must be human, no agent substitute** | 30 min |
| Per security debt | Set an expiry no later than the next gate | 5 min |
| Per gate | Confirm no expired security debt crosses | 15 min |
| Per dependency finding | Reachability assessment → O2 override or not | 20 min |

**Never:** approve without listing which of S1–S10 you actually checked · allow a security
shortcut past one gate · let an agent's `AGENT` verdict stand in for yours at T4.

**Your 60-second check:** *What does this expose, and does it fail closed?*

---

### R9 · Risk & Compliance

**Your one job:** the second binding veto. Can we defend this to a regulator?

| When | Do | Time |
|------|----|------|
| Per plan with `compliance_impact` ≠ none | Board verdict (R1–R8) | 20 min |
| **Every T4 plan** | Verdict — **must be human** | 30 min |
| Per SC4 escalation | Confirm the mandate is real and state the obligation | 20 min |
| Per gate | Audit/consent/retention evidence review | 30 min |
| Quarterly | Confirm retention regimes and reporting obligations | 30 min |

**Never:** accept compliance debt — it is a violation with a delay ([15 §7](./15-TECH_DEBT_POLICY.md#7-debt-and-the-review-boards)).

**Your 60-second check:** *If a regulator asks who did this, when, and under what consent — can
we answer from the system?*

---

### R10 · DevOps / SRE

**Your one job:** everything we ship must be deployable, observable, and reversible.

| When | Do | Time |
|------|----|------|
| Per plan with `operational_impact` | Operations board verdict (O1–O8) | 15 min |
| Per new component | Confirm the runbook exists before it ships | 20 min |
| Per gate | Runbook, alert, and rollback evidence | 30 min |
| Per alert added | Test it fires at least once | 15 min |

**Never:** approve a plan whose rollback is "revert the commit" when data has been written.

**Your 60-second check:** *Can I see it, page on it, and roll it back?*

---

### R11 · Business Analyst

**Your one job:** acceptance criteria that are observable, binary, and bounded.

| When | Do | Time |
|------|----|------|
| Per requirement | Write AC in Given/When/Then; include one negative case | 20 min |
| Per story | Confirm AC quality bar ([12 §3](./12-DEFINITION_OF_READY.md#3-acceptance-criteria-quality-bar)) | 10 min |
| Weekly | Check no READY item has vague AC ("etc.", "and so on") | 10 min |

**Never:** write an AC containing "improved", "faster", or "cleaner".

**Your 60-second check:** *Could two developers implement this differently and both be right?*

---

### R12 · Delivery Head / Delivery Lead — Kalpana

**Your one job:** keep the state/registers true **and** keep the integrated path from admitted work to production executable, predictable and honest. **You are the reason the AI knows where it is and the reason leadership can see what actually controls delivery.**

**Canonical identity:** `Kalpana = Delivery Head = Delivery Lead = Program Delivery Director = Enterprise Delivery Head = R12`. Never create a second Delivery persona.

**You own:** [state/CURRENT-STATE.yaml](./state/CURRENT-STATE.yaml) · [01-CURRENT_STATE.md](./01-CURRENT_STATE.md) · [PARKED-BACKLOG](./registers/PARKED-BACKLOG.md) · [RISK-REGISTER](./registers/RISK-REGISTER.md) · this runbook · [DELIVERY-CONTROL-SYSTEM.md](./DELIVERY-CONTROL-SYSTEM.md) · gate cadence · delivery/governance metrics · integrated milestones · critical path · dependency/decision ageing · forecast confidence · release orchestration

| When | Do | Time |
|------|----|------|
| **Weekly** | Run the freshness check; fix what it flags | 10 min |
| **Weekly** | Update gate criterion states + `state_as_of` | 10 min |
| Weekly | Chair the Governance Sync (§7.1) | 30 min |
| Weekly | Review critical path, blockers, ageing dependencies/decisions and forecast confidence | 20–30 min |
| Per material initiative/release | Establish outcome milestones, workstreams, dependency graph, critical path and forecast assumptions | proportional |
| Per delivery decision dependency | Make authority owner, required-by date, options and consequence of delay explicit | 10–20 min |
| Per gate | Mark `CANDIDATE`; run the unpark sweep; produce the scorecard | 60 min |
| Per release | Integrate Product/Architecture/Engineering/Security/DB/QA/Compliance/Ops readiness and coordinate the **approved** deployment | proportional |
| Hypercare | Track stability, incidents, reconciliation/supportability and exit criteria | proportional |
| Monthly | Register hygiene (§7.3) + forecast/dependency calibration | 45 min |
| Per incident | Search registers for items that would have prevented it | 30 min |
| Quarterly | Calibration review: were our parks, rejections, forecasts and accelerations right? | 60 min |

**Only you can (within R12 jurisdiction):** mark a gate `CANDIDATE` · declare an artefact stale under governance rules · run the unpark sweep · own the integrated delivery forecast/critical-path view and orchestration cadence.

**You cannot:** approve the stage transition because you marked it `CANDIDATE` · redefine Product scope/priority · override Architecture · downgrade Security/Compliance findings · weaken DB integrity/recovery · declare QA evidence passed · fabricate a human sign-off or accept material risk outside delegated authority.

**Never:** let `state_as_of` pass `review_due` silently · sweep a parked item straight into ADMIT without re-triage · delete a register row · keep a forecast green because stakeholders prefer it · use schedule pressure to erase another persona's authority · create a parallel “Delivery Head” persona alongside R12.

**Your 60-second check:** *If an agent read the state file right now, would it work on the right thing — and can I name the three items most capable of moving the production date?*

---

### R13 · AI Agent

**Your one job:** run the pipeline faithfully, and go back to the task you were on.

**Your card is §8.** Read it in full — it is the longest for a reason.

---

## 7. Ceremonies

Three recurring sessions. Nothing else is required. DCS may add focused delivery intervention only when risk warrants it; it does not create meetings for their own sake.

### 7.1 Governance Sync — weekly, 30 min

**Chair:** **Kalpana / R12** · **Attend:** PO, Architect, TL (+ Tech Head monthly)

```text
00:00  Freshness check output — what is stale, who fixes it        (5 min)
00:05  Gate criteria: what moved this week, what is stuck          (5 min)
00:10  New SUG rows: confirm or correct the verdicts               (10 min)
00:20  BLOCKED items + external dependencies: owner and date each  (5 min)
00:25  READY queue re-ordered; next three items named              (5 min)
```

**Outputs:** updated `CURRENT-STATE.yaml` · corrected verdicts · ordered queue.
**If it runs long, the problem is the backlog, not the meeting.**

The DCS delivery view (critical path, due decisions, forecast change) may be attached as a concise readout; it must not turn the Governance Sync into a status meeting.

### 7.2 Gate Review — per stage transition, 90 min

**Chair:** **Kalpana / R12** · **Decide:** Architect + PO (+ QA Lead, Security, Compliance, Ops as
the gate lists)

```text
00:00  Evidence walk: each exit criterion, with its artefact       (30 min)
00:30  Decision: PASSED / back to OPEN with named blockers         (10 min)
00:40  UNPARK SWEEP — re-triage every item whose trigger fired     (25 min)
01:05  Priority recomputation for READY + PARKED                   (10 min)
01:15  Debt expiry: repay, waive with a date, or convert to ADR    (10 min)
01:25  Scorecard + new state signed                                 (5 min)
```

**This is the most important meeting in the model.** It is where parking proves it is not
deletion, and where the AI's next stage of thinking is set.

**Chairing/preparing the gate does not make Kalpana an approver.** R12 may mark `CANDIDATE`; the listed decision authorities approve/reject the transition.

### 7.3 Register Hygiene — monthly, 45 min

**Owner:** **Kalpana / R12**, alone or with the TL. Not a meeting unless something is wrong.

```text
Duplicates merged · parked items aged (AS-2/AS-3) · debt ledger reconciled ·
external owners confirmed · metric trends reviewed · runbook corrections applied
```

---

## 8. What the AI agent must know about this project

The rest of the runbook keeps humans honest. **This section is what makes the agent useful.**

### 8.1 The knowledge contract — ten facts before acting

An agent may not triage, plan, or write code until it can state all ten. If it cannot, it reads
the state file; if the state file cannot answer, it **asks a human**.

| # | The agent must be able to state | Source |
|---|--------------------------------|--------|
| 1 | Which **workstream** this input belongs to | `CURRENT-STATE.yaml` `workstreams[]` |
| 2 | The **current phase** and canonical stage of that workstream | `lifecycle.current_phase` |
| 3 | The **current objective** — one sentence | `current_objective.description` |
| 4 | The **open gate** and which criteria are still failing | `current_gate.exit_criteria` |
| 5 | What is **explicitly out of scope** right now, and when each is revisited | `current_scope.out_of_scope` |
| 6 | The **standing constraints** — what is never allowed | `standing_constraints` |
| 7 | The **known debt** it must not re-report | `known_open_debt` |
| 8 | Where an ADMIT of this type would be **routed** | `routing` |
| 9 | Whether state is **provisional or stale** | `provisional`, `state_as_of` |
| 10 | **What it was doing before this input arrived** | Its own session |

Fact 10 is the one agents lose, and it is the one that matters most. Everything else determines
the verdict; fact 10 determines whether the project gets finished.

### 8.2 Session start — every session, no exceptions

```text
[ ] java scripts/governance/FreshnessCheck.java      → act on exit code
[ ] Read docs/governance/state/CURRENT-STATE.yaml     → facts 1–9
[ ] Read registers/PARKED-BACKLOG.md                  → do not re-propose parked items
[ ] Read 01-CURRENT_STATE.md §6                       → do not re-report known debt
[ ] Name the one work item you are picking up
[ ] Confirm it is head of the ordered READY queue; if not, say so and ask
```

### 8.3 How the agent's thinking must change at each stage

This is the practical answer to *"how should the AI think at this stage of the project?"* The
**same suggestion gets a different verdict** depending on the row.

| Stage | The agent's default posture | Bias toward | Reject on sight | Typical wrong instinct to suppress |
|-------|----------------------------|-------------|-----------------|-------------------------------------|
| **L0–L1 Discovery / Business design** | Ask, don't build | Clarity, written rules | Any code, any technology choice | "Let me scaffold something to explore" |
| **L2 Domain design** | Model concepts, nothing else | Invariants, state models, language | Persistence tuning, messaging, caching, observability stacks | "This aggregate should publish events" |
| **L3 Technical design** | Decide and record | Contracts, boundaries, ADRs | Capacity planning, production config | "Let's just start coding and see" |
| **L4 Foundation** | Build the floor, thinly | Scaffold, CI, arch tests, secrets | Feature breadth, generic frameworks | "Extract a reusable framework now" |
| **L5 Connectivity** | Talk to the outside safely | Clients, auth, error normalisation, async infra | Second LOB, expansion | "Add retries everywhere just in case" |
| **L6 Vertical slice / MVP** | One path, all the way through | Depth over breadth | Generalisation, second journey, abstractions with one implementation | "Let's make this generic for the next LOB" |
| **L7 Hardening ← WS-1 is here** | Prove it, don't extend it | Tests, evidence, compliance review, runbooks, perf measurement, **debt repayment** | New features, new LOBs, new infrastructure | "While hardening, let me also add…" |
| **L8 Expansion** | Reuse, don't rebuild | Second/third journey through the *same* orchestration | Rearchitecting the thing that works | "Now that we have two, let's abstract" |
| **L9 Production readiness** | Assume it will break | Dashboards, alerts, DR, retention, autoscaling, go-live checklist | Broad new scope | "One more feature before go-live" |
| **L10 Operate & evolve** | Measure, then change | SLOs, incident learning, evidence-driven improvement | Unbounded rewrites | "Let's modernise the stack" |

**Read your workstream's row before every triage.** The right answer at L6 is often the wrong
answer at L7, and the agent's instincts do not change on their own — the row does it for them.

### 8.4 The five behaviours that matter most

Everything else in the framework supports these:

1. **Triage before implementing.** Never act on a suggestion in the turn it is raised.
2. **One item in flight.** Only the 8 evidenced P1 overrides interrupt.
3. **Park with a target and a trigger.** Never "we should do this someday".
4. **Return to the task, out loud.** End every triage with "Continuing with `<item>`."
5. **Report honestly.** Partial is partial; red gates are red; drift is reported, not hidden.

### 8.5 Standard replies — use verbatim

| Situation | Say |
|-----------|-----|
| Suggestion mid-task | "Noted as `SUG-00NN` (parked, Phase 5). Continuing `FUNC-011`." |
| User suggests something adjacent | "Valid change — triaging as `SUG-00NN`. It's SF2 for this stage, so I'd finish `FUNC-011` first unless you want to switch." |
| Genuine P1 | "Interrupting `FUNC-011` for a P1 (O2: reachable vulnerability in the payment path). Snapshot recorded; returning after." |
| Drifted | "I drifted: changed `bank-common-error` while fixing the status mapper. Reverting, registering as `SUG-00NN`, finishing the mapper." |
| State is stale | "`CURRENT-STATE.yaml` is 41 days old (`review_due` 2026-09-07). I can park and reject against it, but not admit new work. **Kalpana / R12** needs to refresh it." |
| Asked to skip the process | "Understood — doing it directly. Recording the bypass and its one risk: no Security board on an auth-path change." |
| Item bigger than planned | "Larger than the plan — needs a second component. Stopping to re-review rather than expanding scope." |

### 8.6 Session end

```text
[ ] Current item: done / in-flight with a snapshot / blocked with a blocker ID
[ ] Every suggestion raised this session is in the register
[ ] Drift incidents and their resolution recorded
[ ] Evidence attached for anything claimed Done
[ ] Registers and backlog updated; TODOs carry IDs
[ ] Uncommitted work committed or explicitly flagged
```

---

## 9. Escalation

| Situation | Escalate to | Within |
|-----------|-------------|--------|
| Agent and TL disagree on necessity | **PO (Rajal)** | Same day |
| Stage fit disputed | **Architect (Mahesh)** | Same day |
| Boards conflict (e.g. Security vs Product) | **Architect + PO** → recorded as an ADR | 2 days |
| Plan reaches rework round 3 | **Architect + PO** — the problem is the item, not the plan | Immediately |
| SC4 external mandate identified | **PO + Compliance + Architect** | Same day |
| State file stale > 30 days | **Kalpana / R12**, then **Tech Head** if unfixed | Same day |
| A gate criterion cannot be met | **Architect + PO** — waiver or move the criterion | Before CANDIDATE |
| Security or Compliance veto disputed | **Nowhere.** The veto is binding | — |
| An incident traces to a parked or rejected item | **Kalpana / R12** → calibration review | 1 week |
| A decision/dependency will exhaust critical-path slack | **Kalpana / R12 → owning authority**, then accountable humans if unresolved | Before required-by date |
| Nobody owns an artefact in §4 | **Tech Head (Amit)** | Immediately |

Delivery escalation makes the consequence/time boundary explicit; it does not transfer the underlying authority to R12.

---

## 10. Adoption: first 14 days

| Day | Action | Owner | Done when |
|-----|--------|-------|-----------|
| 1 | **Assign the existing Delivery Lead role (R12) to Kalpana; all Delivery aliases resolve to her.** | Governance / Tech Head | ✅ Included in CR-007 / PR #44; binding effect follows required ratification/merge |
| 1 | ~~Ratify [01 §3–§4](./01-CURRENT_STATE.md#3-snapshot); clear `provisional`~~ | Architect | ✅ **Done 2026-08-10** (GOV-004). PO counter-signature outstanding |
| 2 | Fill any remaining unassigned owners in §2 and §4 | Kalpana / R12 + relevant authority | No required owner is unassigned |
| 2 | Name owners + dates for the two open `EXTERNAL` dependencies (DEP-002, DEP-010) | PO | Register updated |
| 3 | Each role reads its card in §6 — nothing else | Everyone | 10 minutes each |
| 3 | Run the freshness check; fix everything it reports | Kalpana / R12 | Exit code 0 |
| 4 | Run the adoption smoke test ([19 §6](./19-PORTING_GUIDE.md#6-adoption-smoke-test)) with an agent | Architect | 5/5 expected verdicts |
| 5 | First Governance Sync (§7.1) | Kalpana / R12 | Held, 30 min |
| 5–14 | **Triage new inputs only. Do not backfill.** | Everyone | Suggestion register is filling |
| 10 | Sweep TD-014 — its unpark trigger has already fired | TL | Promoted or re-parked with a reason |
| 10 | Close or waive QA-001 — criterion 4.7 is now binding (CR-001) | QA Lead + TL | Closed, or waived with a dated expiry |
| 14 | First scorecard → baseline metrics | Kalpana / R12 | [18 §3](./18-GOVERNANCE_METRICS.md#3-gate-scorecard) filled |

**Success at day 14 looks like:** the state file is ratified and fresh, every role has run their
cadence once, the suggestion register has real rows, and at least one suggestion has been parked
with a target stage instead of being built or forgotten.

---

## 11. If this runbook is wrong

It will be, in places. The cadences are estimates and the roles may not match your team.

Fix it: raise a `GOV` change request ([14](./14-CHANGE_CONTROL.md)), or tell **Kalpana / R12**.
A runbook people work around is worse than no runbook — the whole point is that it describes
what actually happens.