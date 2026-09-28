# 09 — Changing the governance

The governance must be at least as easy to change as the code it governs. AIGEM required a CR,
Architecture + Product boards, a version bump and a decision-register row for any change to a
governance file (`00-GOVERNANCE.md` §9) — and then ran its most important flow fix (CR-016) under a
bypass because that path was too slow.

## 1. How a rule changes

| Change | Path |
|---|---|
| Wording, examples, typo, a clearer table | Any PR, one review (R0) |
| A threshold (WIP limit, SLA, alarm level, budget) | PR + delivery lead review; takes effect on merge; revisited at the next retro |
| A process rule (intake, flow, assurance, cadence) | PR + delivery lead + one other role owner; **7-day objection window** — merge if no objection with a reason |
| A guardrail, a control (G1–G10), a human-only decision, the decision-rights table | **Type-1**: named owner(s) per [05 §4](./05-decisions.md#4-human-only-decisions); author ≠ approver |

**Rule GOV-1.** Every rule change states the metric it expects to move and is checked at the next
retro. A rule that moved nothing is a candidate for deletion.

**Rule GOV-2.** Adding a rule requires naming one to simplify or delete, or explaining in the PR why
the total cost of the rules is still acceptable. The size budget in `validate.py` makes this
concrete.

**Rule GOV-3.** A persona is added only if an existing card cannot absorb the lens and a real
human role will own its decisions. Personas are lenses; knowledge goes into context packs.

## 2. Versioning

This repository is versioned by git tags (`v2.0`, `v2.1`, …) with a one-paragraph `CHANGELOG`
entry per tag. No version string is duplicated inside documents.

## 3. Governance work is work

Governance changes are `Enabler` cards on the `governance` lane and draw on the same ≥ 20%
enabler allocation as other enabling work ([04 §2](./04-flow.md#2-classes-of-service)). This keeps
AIGEM's sound rule GC-1 ("the framework competes for capacity with the work it governs") without
the CR ceremony.
