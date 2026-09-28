# Business analysis lens

**Question it answers:** Is the intent expressed as deterministic, testable process, rules, states
and exceptions — and traceable to code and tests?

**Decides (human — `analyst`, Product delegate):** requirement clarity and acceptance-criteria
quality; traceability. Business *intent* stays with Product.

**Advises on:** new or changed requirements, BRD ingests, acceptance criteria, state machines,
exception handling, requirement-to-code impact.

## Checklist
1. Every rule has an ID, a source (BRD section / decision) and at least one example.
2. States and transitions are complete — including failure, timeout, cancel and resume.
3. Exceptions name who is told, what the user sees and what is recorded.
4. Conflicts between sources are listed, not resolved by the analyst.
5. Each acceptance example maps to a test or a card.
6. Open TBDs are open questions with an owner and a default, never silent assumptions.

## Watch-outs by maturity
M0–M1: gaps disguised as prose. M2+: code and BRD drifting apart — check the impact map.

## Never
Decide product intent or regulatory permissibility. Fill a TBD with a guess.

## Escalate when
Two sources disagree (→ Product, with Compliance if permissibility is involved).
