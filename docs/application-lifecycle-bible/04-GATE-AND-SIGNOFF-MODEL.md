# 04 — Gate and Sign-off Model

**Owner:** Kalpana (Delivery, orchestration) · Mahesh + Rajal (transition authority)
**Extends:** [`04-STAGE_GATES.md`](../governance/04-STAGE_GATES.md) and
[`11-REVIEW_GATES.md`](../governance/11-REVIEW_GATES.md), which remain canonical

---

## 1. Two different gates, routinely confused

| | **Review gate** (AIGEM Board) | **Stage gate** (this model) |
|---|---|---|
| Reviews | One implementation plan | An entire stage |
| Asks | "Is this change correct?" | "Is this stage complete?" |
| Frequency | Per work item | Per stage transition |
| Output | APPROVE / REWORK / REJECTED | PASSED / OPEN / BLOCKED |
| Authority | Seven boards | Architect + PO, plus named specialists |
| Defined in | [`11-REVIEW_GATES`](../governance/11-REVIEW_GATES.md) | Each `stages/Sxx-*.md` |

Both are required. Passing every review gate in a stage does not pass the stage gate — the stage
gate asks whether the *set* of delivered work adds up to the stage's outcome, which is a question
no individual plan review ever asks.

---

## 2. Evidence: the rule the whole model rests on

> **Rule GS-1 — Evidence is an artefact someone else can independently open.**
> A link to a CI run, a test report, a scan output, a signed document, a merged PR, a dashboard, a
> restore-test record. "Confirmed verbally", "the team says it works", "see the STATUS file", and
> "the code does this" are **not evidence**.

### Evidence strength ladder

Where a criterion could be met at more than one level, the stage file names the level required.

| Level | Kind | Example | Acceptable for |
|---|---|---|---|
| **E4** | Executed and reproducible | CI job link with a run ID, re-runnable | Any criterion, including behaviour |
| **E3** | Executed, point-in-time | Penetration test report, DR exercise record, load-test output | Behaviour where continuous execution is impractical |
| **E2** | Reviewed and signed | Compliance sign-off on an audit schema, architecture review verdict | Definition and decision criteria |
| **E1** | Existence of an artefact | A runbook exists, an ADR exists | Definition criteria **only** |
| **E0** | Assertion | "It works" | **Never** |

> **Rule GS-2 — A behaviour criterion requires E3 or E4.** A definition criterion may close at E1
> or E2. Confusing the two is how "hardened" gets claimed for an untested system.

### The evidence table

Every gate transition produces one. This is the artefact the regulator reads.

| # | Criterion | Required level | Evidence | Verified by | Date |
|---|---|---|---|---|---|
| S08-G1 | Application CI builds and tests every module on every PR | E4 | `.github/workflows/build.yml` run #412 | Swapnali | 2026-09-01 |
| S08-G4 | No critical SCA findings unremediated | E4 | Dependency scan report, run #412 | Deepali | 2026-09-01 |

---

## 3. Gate states

| State | Meaning | What may be admitted |
|---|---|---|
| `OPEN` | Stage in progress | Normal AIGEM triage |
| `CANDIDATE` | All criteria claim done; evidence under review | **Freeze:** SF0 and P1-override only |
| `PASSED` | Approvers signed | Unpark sweep runs; next stage opens |
| `BLOCKED` | A criterion cannot be met — external dependency, missing decision, or missing prerequisite stage | Blocking item becomes P1; other work continues in dependency order |

### `BLOCKED` is underused and matters here

A criterion that cannot be closed by effort is not `OPEN`. WS-1 Phase 4 criterion 4.1 ("E2E suite
runs in CI") sat `OPEN` while no CI existed — implying someone could close it by trying harder.

> **Rule GS-3 — If a criterion's blocker is a missing prerequisite, mark it `BLOCKED` and name
> the prerequisite.** `BLOCKED` makes the real dependency schedulable. `OPEN` hides it.

---

## 4. Transition procedure

```
1. Kalpana (R12) marks the gate CANDIDATE and freezes non-SF0 admissions
2. For each exit criterion: attach evidence at the required level
3. Named approvers review — in role, against their own checklist only
4. Any REWORK → gate returns to OPEN with named blocking items (P1/P2)
5. All APPROVE → Mahesh + Rajal mark PASSED
6. Update docs/governance/state/CURRENT-STATE.yaml (human only)
7. Run the AIGEM unpark sweep — parked items are RE-TRIAGED, not auto-admitted
8. Record in registers/DECISION-REGISTER.md
9. Update the position banner in README.md
```

Step 7 is deliberate. A suggestion parked six months ago may have been superseded, made obsolete,
or already solved. Auto-promotion would reintroduce exactly the premature work parking prevented.

### Who may do what

| Actor | Mark CANDIDATE | Mark PASSED | Edit `CURRENT-STATE.yaml` |
|---|---|---|---|
| AI agent | ✅ with a full evidence table | ❌ never | ❌ never |
| Kalpana (Delivery) | ✅ | ❌ | ❌ |
| Mahesh + Rajal jointly | ✅ | ✅ | ✅ |

Kalpana marking `CANDIDATE` is **readiness for decision, not the decision**.

---

## 5. Approver map by stage

Derived from [`PERSONA-AUTHORITY-MATRIX`](../governance/PERSONA-AUTHORITY-MATRIX.md). **AP** =
approval required to pass · **B** = may block within jurisdiction · **RV** = reviews, does not
block · **H** = human signature mandatory, no AI substitution.

| Stage | Rajal | Mahesh | Amit | Deepali | Shailja | Swapnali | Shivanshi | Aarti | Kalpana |
|---|---|---|---|---|---|---|---|---|---|
| S00 Ideation | **AP** | RV | — | RV | **AP** | — | — | — | RV |
| S01 Discovery | **AP** | RV | — | RV | RV | RV | — | — | RV |
| S02 Regulatory Framing | RV | RV | — | **AP** | **AP/B/H** | RV | RV | RV | RV |
| S03 Business Requirements | **AP** | RV | — | RV | **AP/B** | **AP** testability | — | RV | RV |
| S04 Product Definition | **AP** | RV | RV | RV | RV | RV | RV | — | **AP** feasibility |
| S05 Experience Design | **AP** | RV | RV | **RV/B** auth UX | **RV/B** disclosure | RV | — | — | RV |
| S06 Domain Architecture | **AP** semantics | **AP** | RV | RV | RV | RV | RV | **AP** | RV |
| S07 Solution Architecture | RV | **AP** | RV | **AP/B/H** | **AP/B** | RV | **AP** | **AP** | RV |
| **S08 Engineering Foundation** | RV | **AP** | **AP** | **AP/B** | RV | **AP/B** | **AP** | RV | RV |
| **S09 Platform Foundation** | RV | **AP** | RV | **AP/B/H** | **AP** residency/retention | RV | **AP/B** | **AP** backup/DR | RV |
| S10 Integration | RV | **AP** | **AP** | **AP/B** | **AP** third-party | RV | **AP** | RV | RV |
| S11 Vertical Slice | **AP** | **AP** | **AP** | **AP/B** | **AP/B/H** | **AP/B** | **AP** | RV | **AP** |
| S12 Hardening | **AP** | **AP** | RV | **AP/B/H** | **AP/B/H** | **AP/B** | **AP** | RV | RV |
| S13 Expansion | **AP** | **AP** | **AP** | **AP/B** | **AP** | **AP/B** | **AP** | RV | **AP** |
| S14 Go-Live | **AP** | **AP** | RV | **AP/B/H** | **AP/B/H** | **AP/B** | **AP/B** | **AP** | **AP** |
| S15 Operate | **AP** | RV | RV | **AP** | **AP** | RV | **AP/B** | RV | RV |

**Reading it:** at S14, a go-live cannot proceed without human signatures from Security and
Compliance, and Shivanshi can block on operational readiness regardless of how much business
pressure exists. That is the design, not an obstacle to route around.

---

## 6. Sign-off record format

One per gate transition, stored in `stages/signoffs/Sxx-GATE-SIGNOFF-<date>.md` using
[`templates/GATE-SIGNOFF.md`](./templates/GATE-SIGNOFF.md):

```yaml
gate_signoff:
  stage: S08
  gate_id: GATE-S08
  transition: "S08 → PASSED"
  date: 2026-10-15
  marked_candidate_by: "Kalpana / Delivery"
  candidate_date: 2026-10-08

  criteria:
    - id: S08-G1
      criterion: "Application CI builds and tests every module on every PR"
      required_level: E4
      evidence: "https://github.com/.../actions/runs/1234567"
      state: MET
      verified_by: "Swapnali / QA"

  approvals:
    - persona: "Mahesh / Architecture"
      reviewer_type: HUMAN
      decision: APPROVED
      evidence: ["reviewed A1-A10 against the pipeline definition"]
      date: 2026-10-14

  conditions_carried_forward:
    - "SAST findings backlog triaged into RISK register by 2026-10-31"

  parked_items_released: [TD-014, QA-001]
  next_stage: S09
```

> **Rule GS-4 — Conditions become acceptance criteria.** An `APPROVED_WITH_CONDITIONS` verdict
> whose conditions are not tracked to closure is an unconditional approval with extra words.

---

## 7. Gate anti-patterns

| Anti-pattern | Why it happens | Countermeasure |
|---|---|---|
| **Rubber-stamping** | Seven boards on trivial changes trains everyone to skim | Proportional tiers T1–T4 ([`11-REVIEW_GATES §3`](../governance/11-REVIEW_GATES.md)) |
| **Evidence substitution** | A document asserting behaviour closes a behaviour criterion | Rule GS-2 |
| **Gate drift** | Criteria added mid-stage without change control | New criteria require a CR (this is what CR-001 corrected) |
| **Perpetual CANDIDATE** | Gate marked candidate, evidence never assembled | Kalpana's decision-forcing power (Rule PA-1): required-by date, then OVERDUE, then escalation |
| **Silent non-response** | A board never replies and the gate stalls invisibly | Response clock + `NO_RESPONSE` recorded against a named persona (Rule RG-7) |
| **Approval by exhaustion** | Third rework round, everyone gives up | Two rounds maximum, then mandatory escalation |
| **Stale approval** | Approval given, context changed, work proceeds | Approvals expire at 30 days or on changed context (Rule RG-8) |

---

## 8. Waivers and exceptions

Some criteria will genuinely need to be waived. That is legitimate; doing it invisibly is not.

A waiver requires **all six** of:

1. The criterion, and precisely what is not met
2. The **risk accepted**, in business terms
3. A **compensating control**, if any exists
4. A named **risk owner** — a human, never a persona-in-general and never an agent
5. An **expiry date** — no open-ended waivers
6. A **remediation backlog item** with an ID

Non-waivable, by any authority, at any tier:

- A mandatory human T4 Security or Compliance sign-off
- A binding regulatory obligation (Shailja's jurisdiction — a waiver here is a legal decision,
  not a delivery one)
- Data residency for regulated data
- Consent capture where consent is legally required
- The suitability hard-gate before quote

> **Rule GS-5 — A waiver with no expiry is a scope change wearing a disguise.** Route it through
> [`14-CHANGE_CONTROL`](../governance/14-CHANGE_CONTROL.md) instead.
