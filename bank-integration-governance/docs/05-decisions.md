# 05 — Decisions

Decisions, not code, were the constraint under AIGEM ([00 F3](./00-diagnosis.md#f3--decisions-are-the-real-bottleneck-and-nothing-measures-them)).
This page makes decisions cheap where they can be, clocked where they cannot, and never the reason
a lane stops building.

## 1. Two types

| | **Type-2 — reversible** | **Type-1 — irreversible or costly to reverse** |
|---|---|---|
| Test | Can we change our mind within a sprint without data loss, customer impact, regulatory exposure or contract breakage? | No, or not cheaply |
| Examples | Library choice inside a service; internal package layout; error message wording; lane WIP; a reversible default for an open BRD conflict; test strategy for a card | A guardrail; a public/bank-facing contract; data model of an append-only evidence store; cloud account/region topology; a regulatory interpretation; a production go-live; spend beyond budget |
| Decider | **The doer** (lane lead or the agent's human) after the advice process | The **named owner role** from [`personas/decision-rights.md`](../personas/decision-rights.md) |
| Record | 5-line decision note in the PR or ADR-lite | Decision record ([template](../templates/decision-record.md)) in the product repo's ADR log, indexed in [`state/decisions.yaml`](../state/decisions.yaml) |
| SLA | Same day | **5 working days** from "ready for decision" (Expedite: 1 day) |
| While pending | n/a | Work proceeds on `default_while_pending` |

When unsure which type: **Type-1**, but try to make it Type-2 first — a feature flag, a config
switch or an adapter seam often turns an irreversible choice into a reversible one. That is the
cheapest architecture work available.

## 2. The advice process (Type-2)

1. The doer writes the decision in five lines: *context · options · choice · why · how to reverse*.
2. The doer asks advice from whoever is affected — relevant AI advisors run in parallel (seconds);
   humans are asked only if the decision touches their lane or guardrail.
3. The doer decides. Advice is considered, not obeyed. Disagreement is recorded, not blocking.
4. Anyone may reopen a Type-2 decision with new information; reopening is not a CR.

## 3. Clocks and defaults (Type-1)

Every open Type-1 decision in [`state/decisions.yaml`](../state/decisions.yaml) carries:

```yaml
id: DEC-OPEN-…
question: "…"
owner: security            # a role id; the human is resolved from roster.yaml
needed_by: 2026-10-05
default_while_pending: "…" # what we build meanwhile, safely
blocks_promotion_to: uat   # which checkpoint cannot pass without it (dev|uat|prod|none)
```

- **Rule DC-1 — Building never waits on a decision.** Build the default. The decision gates
  *promotion*, at the checkpoint named in `blocks_promotion_to`.
- **Rule DC-2 — A missed SLA escalates automatically** to the sponsor at the next decision clinic.
  It never converts into approval, and it never converts into rejection. It becomes visible.
- **Rule DC-3 — Batch decisions.** A twice-weekly 15-minute **decision clinic** burns down the
  queue in `needed_by` order. Most Type-1 decisions need five minutes of the right person's time,
  not a board.
- **Rule DC-4 — A decision drafted by AI is labelled as such** until a human decides. "AI-drafted,
  proposed default X" is useful; it is never "decided".

## 4. Human-only decisions

These can never be taken by an AI agent, regardless of evidence quality, and cannot be delegated
to one:

1. Approving an `R2` control change for release ([06 §2](./06-assurance.md#2-control-changes--the-only-thing-that-always-needs-named-humans)).
2. Adding, removing or weakening a guardrail.
3. A final regulatory or legal interpretation.
4. Accepting material risk, or any exception with residual risk.
5. Authorising promotion to `prod` or to any environment with real customer data.
6. Committing spend or signing an external commitment.

## 5. One person, many hats

The programme currently has very few humans holding many roles. That is legitimate and is how
most teams start. The model handles it with three rules instead of seven boards:

- **HAT-1** A human who holds several roles decides once, stating the roles: *"Approved as
  Architecture and Product."* One act, not one per seat.
- **HAT-2** Segregation of duties applies only where it protects something: the **author** of an
  `R2` control change may not be its **approver**, and the person accepting a material risk may
  not be the person who created it. If only one human is available, the control change waits for
  a second human or a named deputy — this is the one place where waiting is correct.
- **HAT-3** Deputies are named in [`personas/roster.yaml`](../personas/roster.yaml). An absent
  owner's decision goes to the deputy after the SLA, not to a queue.

## 6. Conflict ladder

One ladder for every pair of roles — no bilateral protocols.

1. **Separate outcome from mechanism.** Each side states the outcome it needs, not the
   implementation it prefers. Most conflicts end here.
2. **Take the stricter outcome if both can hold.** A conflict is real only if the outcomes are
   mutually exclusive.
3. **Try to make it reversible.** Flag, config, adapter seam — choose one now, keep the other
   reachable.
4. **Named tie-breaker** (kept from AIGEM PA-2):

| Conflict | Tie-breaker |
|---|---|
| A binding regulatory obligation is involved | Compliance & Risk owner decides permissibility; Security implements within it |
| Residual-risk or design trade-off, no obligation breached | Architecture owner, jointly with Product where the outcome is affected |
| Material organisational risk acceptance | The accountable human risk owner only |
| Priority / sequencing | Product owner |
| Timing of any of the above | Delivery lead may force *when*, never *what* |

5. **Record both positions verbatim** in the decision record. The overruled view is kept — it is
   what an auditor will ask for.

## 7. One handoff shape

Every cross-role ask — human or AI — uses the same six lines:

```text
ASK        what you need from whom
CONTEXT    link to the card / PR / pack
OPTIONS    2–3, with the default marked
RECOMMEND  which, and why
NEEDED BY  date, and what it blocks (build never; promotion to …)
REVERSIBLE yes/no — and how
```
