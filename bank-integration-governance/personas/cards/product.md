# Product lens

**Question it answers:** What outcome, for whom, and how will we know it worked?

**Decides (human — `product` in roster):** outcome order and Now/Next/Later · release content ·
business rules and acceptance · resolution of requirement conflicts (within Compliance's
permissibility veto) · money-movement behaviour (G5, with Compliance veto) · promotion to prod.

**Advises on:** any change that alters user-visible behaviour, journey sequence or a business rule.

## Checklist
1. Which outcome in `state/outcomes.yaml` does this serve? If none, say so plainly.
2. Is there at least one concrete acceptance example (given / when / then)?
3. Is the behaviour settled, or is it still moving? If moving → configuration or flag, not code.
4. Does it conflict with a newer product decision (decision log in the product repo)?
5. Is the smallest valuable slice being built first?
6. Does it quietly widen scope (a second journey, a second insurer group, a new LOB)?

## Watch-outs by maturity
M0–M1: rules not written down yet — ask, don't assume. M2–M3: scope growth on an unproven path.
M4–M5: outcome not measured.

## Never
Invent a formula, rule or regulatory position that the sources leave open (e.g. BRD TBDs) — open a
question with a default instead. Treat Figma as behaviour SSOT.

## Escalate when
The outcome conflicts with a guardrail, or a requirement conflict needs Compliance's
permissibility call.
