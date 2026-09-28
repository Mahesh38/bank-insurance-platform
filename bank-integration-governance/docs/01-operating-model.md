# 01 — Operating model

## 1. The five principles

| # | Principle | Replaces (AIGEM) | What it means in practice |
|---|---|---|---|
| P1 | **Guardrails, not gates** | Stage fit, scope fit, action matrix | A short list of invariants ([02](./02-guardrails.md)) is enforced by code and CI. Anything that does not break a guardrail may start. |
| P2 | **Gate the release, never the start** | Stage gates, freeze rule, "written before built" | Building behind a stub, feature flag or synthetic data is always allowed. Evidence and sign-off attach to *promotion* into an environment with real customers, money or PII ([06](./06-assurance.md)). |
| P3 | **Parallel by default** | One in-flight item per lane, SF5 as an exception | Lanes are capabilities, not stages. Cross-lane dependencies are broken with published contracts, stubs and simulators ([04](./04-flow.md)). |
| P4 | **Decide at the edge, escalate by exception** | Seven boards, CR for most changes | Reversible decisions: the doer decides after advice. Irreversible or control-changing decisions: a named human, with a clock and a default path ([05](./05-decisions.md)). |
| P5 | **Governance is a product** | Governance as binding law, amended by CR | Rules have metrics and an owner; the monthly retro changes them. A rule bypassed more than 10% of the time is rewritten, not enforced harder ([08](./08-cadence-and-metrics.md), [09](./09-changing-the-governance.md)). |

## 2. The loop

```text
 ┌─────────────────────────────────────────────────────────────────────────┐
 │  OUTCOMES  Now / Next / Later per capability lane      state/outcomes.yaml │
 └───────────────┬─────────────────────────────────────────────────────────┘
                 │ replenish weekly · reprioritise any day (PO)
                 ▼
 INPUT ──► INTAKE (03, 90 s) ──┬─ fold into current change   (tiny, same blast radius)
                               ├─ card on a lane             (Now / Next / Later)
                               ├─ open question / decision   (owner, SLA, default)
                               └─ decline, with one line of why
                 │
                 ▼
 BUILD ──► PR with risk label (R0/R1/R2) ──► advisors in parallel ──► owner merges
                 │                                    (06 §3)
                 ▼
 PROMOTE ──► dev ──► bank-UAT ──► prod      checkpoints evaluated on demand (06 §5)
                 │
                 ▼
 LEARN ──► flow + quality + governance-cost metrics ──► monthly retro edits the rules
```

## 3. What is deliberately gone

| Gone | Why |
|---|---|
| A single "current stage" per workstream | Capabilities mature independently. Maturity is reported per capability ([07](./07-roadmap-and-maturity.md)), never used to refuse work. |
| "Never implement a suggestion in the turn it is raised" | Replaced by the **fold-in rule**: tiny, same-change, no new dependency/decision/control → just do it and say so in the PR. Everything else becomes a card — still never lost. |
| Stage-relative P1–P5 "now" and "at target" | Replaced by horizon (Now/Next/Later) + class of service. One judgement instead of two predictions. |
| Seven review boards and a plan review | Replaced by risk-labelled review of the actual change, with advisors in parallel. Plans are written only for Type-1 decisions. |
| A freshness check that halts admission | Staleness is a warning and a card for the owner. It never stops work. |
| Markdown registers as the work ledger | Work items live in the tracker (GitHub Issues/Projects or Jira). This repo holds the model, guardrails, decisions and context — things that change slowly. |
| Bilateral persona protocols | One handoff shape and one conflict ladder for every pair ([05 §6](./05-decisions.md#6-conflict-ladder)). |

## 4. What is deliberately kept hard

Agility is not the absence of control. These stay non-negotiable and are, if anything, easier to
see because there is less around them:

1. The guardrails in [`state/guardrails.yaml`](../state/guardrails.yaml) — customer-device payment,
   consent before proposal, suitability before quote, no PII in logs, India-region data, adapter
   isolation, append-only evidence, and the rest.
2. **Control changes** (G1–G10) need named human approval before release, and the author of a
   control change cannot be its approver.
3. **AI never approves** a control change, a Type-1 decision, a regulatory interpretation, a
   production go-live or a material risk acceptance. It drafts, assembles evidence and recommends.
4. Evidence before "Done" — scaled to risk rather than uniform.
5. Historical evidence is immutable. The AIGEM registers are archived read-only, not deleted
   ([migration](../migration/README.md)).

## 5. Vocabulary

| Term | Meaning |
|---|---|
| **Lane** | A capability-aligned stream of work with one human lane lead and any number of AI executors. Listed in [`state/lanes.yaml`](../state/lanes.yaml). |
| **Outcome** | A measurable result a lane is working towards; lives on the Now/Next/Later roadmap. Replaces "stage objective". |
| **Card** | A unit of work in the tracker. Has a lane, horizon, class of service and an outcome link. |
| **Horizon** | `Now` (being pulled), `Next` (ready to pull when capacity frees), `Later` (valuable, not yet; has a review date). |
| **Class of service** | `Expedite`, `Fixed-date`, `Standard`, `Enabler`, `Explore` — how a card is scheduled ([04 §2](./04-flow.md#2-classes-of-service)). |
| **Risk label** | `R0` routine, `R1` notable, `R2` control change — decides who must review a change ([06](./06-assurance.md)). |
| **Type-1 / Type-2 decision** | Irreversible-or-costly vs reversible ([05](./05-decisions.md)). |
| **Checkpoint** | The evidence required to promote into an environment. Evaluated on demand, not scheduled. |
| **Advisor** | An AI persona lens asked for advice on a change. Advice, not a verdict. |
| **Guardrail** | An invariant that must never be violated, preferably enforced by code. |
