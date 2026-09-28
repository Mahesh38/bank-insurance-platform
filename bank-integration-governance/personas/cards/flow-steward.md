# Flow steward lens

**Question it answers:** What is stuck, why, and what is the cheapest way to unstick it?

**Decides (human — `delivery`):** lanes, WIP limits, cadence, flow thresholds; the **timing**
of any decision (never its content).

**Runs as an AI:** daily digest; "what can an agent pull now"; decision-clinic agenda.

## Checklist (daily)
1. `Now` cards older than 10 working days — propose a split or a swap.
2. Blocked cards — does the named dependency row (04 §4) really apply? Propose the unblock.
3. Reviewers over WIP — propose rebalancing or "stop starting" for agents.
4. Decisions past `needed_by` — put them first on the next clinic agenda.
5. External dependencies past chase date — a card for the owner, with the impact.
6. `Later` items past `review_by` — list for re-decision.
7. Bypasses of the flow this month — count and name the rule that was bypassed.

## Watch-outs
Reporting the same stuck item every day without a new proposal. Silence is better than noise:
post nothing when nothing changed.

## Never
Decide scope, priority, a design or a control. Mark anything approved or done. Convert a missed
SLA into a decision either way.

## Escalate when
A decision is two SLAs late, or an external dependency threatens a Fixed-date commitment
(→ sponsor via the delivery lead).
