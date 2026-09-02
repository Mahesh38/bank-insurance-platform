# 09 — Jira Model

**Owner:** Kalpana (Delivery) · Rajal (backlog content)
**Generated artefact:** [`backlog/jira-import.csv`](./backlog/jira-import.csv)

---

## 1. Hierarchy

```
Initiative        AU Bank Insurance Distribution Platform          (one)
   └── Stage      S00 … S15                                        (16 — Jira Version/Fix Version)
         └── Epic S08-E01 …                                        (Jira Epic)
               └── Story S08-E01-S02 …                             (Jira Story)
                     └── Sub-task  implementation, test, docs      (optional, team's choice)
         └── Gate  S08-G1 …                                        (Jira Task, linked to the stage epic)
         └── Test  S08-VT-01 …                                     (Jira Test / Task)
```

**Stages map to Fix Versions rather than Epics** so that a single Jira board can filter by stage,
and so a story can move between epics without losing its stage identity.

---

## 2. Required fields

| Field | Values | Why |
|---|---|---|
| `Summary` | `S08-E01-S02 — Enforce JaCoCo thresholds in CI` | ID first makes search and traceability trivial |
| `Issue Type` | Epic / Story / Task / Test / Bug / Spike | — |
| `Fix Version` | `S08 Engineering Foundation` | Stage membership |
| `Epic Link` | `S08-E01` | Epic membership |
| `Priority` | P1–P5 | **AIGEM priority only** — never a persona-local severity |
| `Labels` | `stage-s08`, `ws-3`, `foundation-recovery` | Filtering |
| `Component` | Bounded context or service | Ownership routing |
| `Assignee persona` | Rajal / Mahesh / Amit / Deepali / Shailja / Swapnali / Shivanshi / Aarti / Kalpana | Maps to the authority matrix |
| `Work Type` | One of AIGEM's 16 canonical types | Routing per `CURRENT-STATE.yaml` |
| `Acceptance Criteria` | Given/When/Then or checklist | DoR requirement |
| `Evidence Required` | E0–E4 level + artefact type | Closes the gate-evidence loop |
| `Traces To` | Requirement, obligation, gap or debt ID | Regulatory traceability |
| `Review Tier` | T1–T4 | Determines mandatory boards |
| `Security Impact` | none / low / medium / high | Board 4 trigger |
| `Compliance Impact` | none / low / medium / high | Board 6 trigger |

> **Rule JM-1 — Priority is AIGEM's P1–P5 and nothing else.** Deepali's `S0–S3`, Shivanshi's
> `O0–O3`, Kalpana's `DL0–DL3`, Shailja's `R0–R3` and Rajal's local `P0–P2` are **separate
> fields**, never overloaded onto Jira Priority. Overloading them is how a "critical" security
> finding becomes indistinguishable from a "critical" delivery date.

---

## 3. Workflow

```
   BACKLOG ──► TRIAGED ──► READY ──► IN PROGRESS ──► IN REVIEW ──► VERIFYING ──► DONE
      │            │          │                          │             │
      │            ▼          │                          ▼             ▼
      │         PARKED        │                       REWORK       FAILED-VERIFY
      │            │          │                          │             │
      └────────────┴──────────┴──────────────────────────┴─────────────┘
                                    (re-enter at the appropriate state)
```

| State | Entry condition | Exit condition |
|---|---|---|
| `BACKLOG` | Created | Triaged through AIGEM |
| `TRIAGED` | Stage fit, scope fit, necessity, priority recorded | ADMIT verdict |
| `PARKED` | SF2/SF3 verdict | `unpark_trigger` fires **and** re-triage admits it |
| `READY` | Definition of Ready met | Capacity available |
| `IN PROGRESS` | Assigned and started | Implementation complete |
| `IN REVIEW` | PR raised; boards convened at the item's tier | All mandatory boards APPROVE |
| `VERIFYING` | Merged; evidence being produced | QA confirms evidence sufficiency |
| `DONE` | Definition of Done met, evidence linked | — |

> **Rule JM-2 — `VERIFYING` is a real state and QA owns its exit.** Merging is not done. The most
> common way evidence goes missing is a workflow that jumps from `IN REVIEW` to `DONE` on merge,
> leaving the artefact nobody produced invisible.

**`PARKED` mirrors AIGEM's parked backlog** and carries the three mandatory fields:
`target_stage`, `unpark_trigger`, `future_necessity`. A park without them is not parked, it is
lost.

---

## 4. Boards

| Board | Filter | Audience |
|---|---|---|
| **Delivery** | `fixVersion = current stage AND status != Done` | Team standup |
| **Gate** | `issueType = Task AND labels = gate-criterion AND fixVersion = current stage` | Gate review |
| **Triage** | `status IN (Backlog, Triaged)` | Weekly triage |
| **Parked** | `status = Parked` | Unpark sweep at every transition |
| **Compliance** | `labels = compliance-control` | Shailja's control tracking |
| **Risk** | `labels IN (security-finding, risk)` | Deepali + Shailja |
| **Debt** | `labels = tech-debt` | Amit's ledger |

---

## 5. Traceability queries a regulator's question becomes

| Question | Jira query |
|---|---|
| "Show me every change implementing the suitability control" | `"Traces To" ~ "C1" OR labels = control-c1` |
| "What evidence closed stage S08?" | `fixVersion = "S08" AND labels = gate-criterion` |
| "Which requirements have no test?" | `issueType = Story AND "Traces To" is not EMPTY AND issueFunction not in linkedIssuesOf("issueType = Test")` |
| "What was deliberately not done, and why?" | `status = Parked OR resolution = Withdrawn` |
| "Who approved this stage transition?" | Gate sign-off record linked from the stage epic |

That fifth row is why IDs are immutable and withdrawn items are never deleted.

---

## 6. Importing

```bash
python3 scripts/lifecycle/generate-backlog.py
```

Then in Jira: **Settings → System → External System Import → CSV**, map the columns, and import
Epics before Stories so `Epic Link` resolves.

### Round-tripping

The stage Markdown under `stages/` is the **source of truth for structure**. `BACKLOG.yaml` and
`jira-import.csv` are generated views. Jira is
the source of truth for **state** (what is in progress, who owns it, what is blocked). Do not try
to sync state back into YAML — that fight is unwinnable and produces two wrong answers.

When the structure changes: edit the relevant stage Markdown, regenerate, review the diff, and
import only the new rows. Never edit generated backlog files directly.

Stage progression is separate from structure generation. The proposal-only controller reads
`docs/governance/state/GATE-EVIDENCE.yaml`; it may prepare a `CANDIDATE` package but cannot mark a
stage `PASSED` or sync Jira state into governance YAML.

---

## 7. Estimation and sizing

| Size | Meaning | Rule |
|---|---|---|
| XS | < ½ day | — |
| S | ~1 day | — |
| M | 2–3 days | — |
| L | ~1 week | Consider splitting |
| XL | > 1 week | **Must** be split, or it is a spike |

> **Rule JM-3 — An item that cannot be sized is a spike, not a big story.** Spikes are
> time-boxed, produce a decision or an ADR, and never produce production code.

Foundation work (S08, S09) is systematically under-estimated because it has no visible feature
output. Budget for it explicitly rather than expecting it to fit around delivery — that
expectation is precisely how it came to be missing here.
