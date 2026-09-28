---
name: flow-intake
description: Route any new input — requirement, bug, idea, review comment, scan finding, or something you noticed mid-task — to one of four outcomes (fold-in, card, open question, decline) in under ninety seconds, then return to the current card. Use whenever something arrives that is not the card you are working on, when asked "should we do X" or "is this the right time for X", and when a requirement changes.
---

# Flow intake

Source rules: `docs/03-intake.md`. Guardrails: `state/guardrails.yaml`.

## Run
```text
1. Guardrail?   Would doing this break any guardrail in scope?  → DECLINE this path; propose
                the compliant alternative; name the guardrail id.
2. Fold-in?     All true: same PR/files · ≤ ~30 changed lines · no new dependency, contract,
                decision or control (not R2) · does not widen the PR's purpose
                → do it now; add "fold-in: <what>" to the PR description.
3. Decision?    Needs a choice nobody has made → add to state/decisions.yaml (or draft it in
                the PR if you cannot write there): owner role, type 1/2, needed_by,
                default_while_pending, blocks_promotion_to. Buildable parts still become cards.
4. Card         lane (context-map) · outcome id or "none" + why · horizon Now/Next/Later
                (Later needs review_by) · class Expedite/Fixed-date/Standard/Enabler/Explore ·
                risk guess R0/R1/R2 · blocked only with a docs/04-flow.md §4 row.
   Decline      duplicate / done / not worth it → one line of why; closed, not deleted.
```

## "Not now" needs a reason word
`value` (reorder later) · `information` (open question or Explore card) · `capacity` (stays in Next).

## Answer shape
```text
INTAKE  "<input>"
Outcome: FOLD-IN | CARD <lane>/<horizon>/<class>/<risk> | QUESTION <DEC-id> (default: …) | DECLINE (<why>)
Captured as <id>. Continuing <current card>.
```

Never implement a card-sized item inside the current PR. Never let an input vanish.
