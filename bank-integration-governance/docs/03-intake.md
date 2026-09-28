# 03 — Intake: triage in ninety seconds

Every input — requirement, bug, idea, review comment, scan finding, AI suggestion — gets an
outcome and leaves a trace. That part of AIGEM was right. What changes is the cost: four questions
instead of a ten-step pipeline, and the answer is usually a card, not a register row.

## 1. The four questions

```text
Q1  Does it break a guardrail?                          (state/guardrails.yaml)
      yes → DECLINE this path; propose the compliant alternative. Done.

Q2  Is it tiny and inside the change I am already making?
      all of: same PR / same files · ≤ ~30 changed lines · no new dependency,
      contract, decision or control (not R2) · does not widen the PR's purpose
      yes → FOLD IN. Do it now; one line in the PR description ("fold-in: …"). Done.

Q3  Does it need a decision someone has not made?
      yes → OPEN QUESTION in state/decisions.yaml with owner, needed-by date and a
            proceed-while-pending default (05). Any buildable part still becomes a card.

Q4  Otherwise → CARD in the tracker:
      lane · outcome it serves · horizon (Now / Next / Later) · class of service ·
      risk label guess (R0/R1/R2) · "blocked by" only if genuinely unbreakable (04 §4)
```

Anything that is none of these — duplicate, already done, or not worth doing — is **DECLINED**
with one line of reason on the card or comment. Declined is closed, not deleted; it can be
reopened by anyone with new information, without ceremony.

## 2. Horizon, not stage

| Horizon | Means | Who moves cards here | Automatic behaviour |
|---|---|---|---|
| **Now** | Being pulled this week | Lane lead (within WIP) | Ages are tracked; >10 working days without movement raises a flow-steward nudge |
| **Next** | Ready to pull when capacity frees; ordered | Product owner, lane lead | Re-ordered at weekly replenishment — or any day by the PO |
| **Later** | Valuable, not yet | Anyone | Carries a `review_by` date (default +90 days). At that date it is re-decided or declined. Nothing sits in Later forever. |

"Parked with an unpark trigger" becomes "Later with a review date, and optionally a `pull_when`
signal". The review date is the backstop; the signal is a shortcut.

## 3. When the answer is "not now"

Say **why** in one of three words, because each has a different fix:

| Reason | Meaning | Fix |
|---|---|---|
| `value` | Something else matters more right now | Wait for replenishment; PO can reorder any day |
| `information` | We would be guessing (requirements unsettled, provider behaviour unknown) | Open question or an `Explore` card to get the information cheaply |
| `capacity` | Worth doing, nobody free to review it | Stays in Next; the flow steward reports review capacity as the constraint |

AIGEM's single answer — "premature for this stage" — conflated all three, which is why the same
items kept returning.

## 4. What agents do mid-task

1. Notice something outside the current card.
2. Run Q1–Q4 in your head; it takes seconds.
3. Fold-in → do it and note it. Card / question → create it (or draft it in the PR description
   if you cannot write to the tracker), then **carry on with the current card**. Say so:
   `Captured as <card/question>. Continuing <current card>.`
4. Never widen the current PR's purpose. Never silently drop the observation.

This preserves AIGEM's best behaviour — agents do not wander — without making a two-line fix
cost a register entry, a triage record and a separate PR.

## 5. Intake for requirement changes

Requirements here change often and legitimately (see [00 F10](./00-diagnosis.md#f10--requirement-churn-is-handled-as-change-control)).
Handle a changed requirement like any other input, plus:

- **Record the change where behaviour is owned** (the BRD / decision log in the product repo), not
  in a governance CR.
- **Find the blast radius** with the requirement-to-code map; affected cards get a comment, not a
  re-triage.
- **Prefer configuration over code** for rules still in motion (product decision D-014 already
  requires policy-driven controls until compliance is validated).
- **Conflicts between sources** (e.g. BRD C1–C7) become open questions with a default. Build the
  default behind a flag or configuration; do not stop.

## 6. Templates

- Card: [`templates/intake-card.md`](../templates/intake-card.md)
- Open question / decision: [`templates/decision-record.md`](../templates/decision-record.md)
