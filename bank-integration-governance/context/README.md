# Context architecture

How an AI agent gets exactly the context it needs — cheaply, predictably, and without a
governance document telling it where every other governance document is.

## 1. Design rules

| # | Rule | Replaces |
|---|---|---|
| CX-1 | **Route by what is being changed, not by how the request is phrased.** The files a card touches (or will touch) select the context. | Intent-matched capsules (41 ids) that resolve differently for the same change |
| CX-2 | **Separate lens from knowledge.** Persona cards say how to look; capability packs say what is true. | Persona packages carrying domain knowledge (up to 316 KB) |
| CX-3 | **Budgets are hard.** Tier 0 ≤ 4 KB · a pack ≤ 10 KB · a card ≤ 2.5 KB · default load ≤ 25 KB. `validate.py` fails the build on overflow. | Budgets per capsule, often exceeded by reference chains |
| CX-4 | **Prefer generated facts to written facts.** Contracts, ArchUnit rules, CI config and test names are truth; a pack links to them rather than restating them. | Hand-maintained narrative that drifts |
| CX-5 | **Staleness warns, never halts.** Each pack has `verified_on` and an owner; past 30 days it warns in CI and the flow steward raises a card. | `FreshnessCheck` exit 2 barring all admission |
| CX-6 | **One fact, one home.** Packs link to the source of a fact; they do not copy it. If two packs need a fact, it belongs to the more central one. | BOOT + capsule + card + package + register restating the same constraint |

## 2. Tiers

```text
Tier 0  AGENTS.md                         ≤ 4 KB   always      guardrail summary, how to intake, how to load
Tier 1  context/packs/<lane>.md           ≤ 10 KB  by path     what is true in this capability now
        state/guardrails.yaml (filtered)           by lane     only guardrails in scope
Tier 2  personas/cards/<lens>.md          ≤ 2.5 KB by mode     how to look, what may be decided
Tier 3  source documents (product repo)   on demand            ADRs, BRD chapters, contracts, rule packs
```

A typical build card loads: AGENTS.md (4) + one pack (≤ 10) + its guardrails (≈ 2) + one maker card
(2.5) ≈ **18 KB**, then opens specific Tier-3 sources only when a question needs them. A review adds
≈ 2.5 KB per advisor lens, each in its own parallel context.

## 3. Routing

[`context-map.yaml`](./context-map.yaml) maps product-repository path globs to a lane and pack.
The [`load-context`](../.claude/skills/load-context/SKILL.md) skill resolves:

```text
changed or target paths ──► lane(s) ──► pack(s) + scoped guardrails + maker card
no paths yet (a question)  ──► keyword fallback in context-map.yaml ──► same
several lanes              ──► each pack's "Summary" section only, then the full pack for the
                               lane owning most of the change
```

## 4. Pack contract

Every pack in [`packs/`](./packs/) has these sections and front matter; the template is
[`packs/_TEMPLATE.md`](./packs/_TEMPLATE.md).

```yaml
lane: integration-hub
owner: architecture          # role id; human via roster
verified_on: 2026-09-28
```

`Summary` (5 lines) · `Outcome now` · `Contracts` · `Invariants in play` (guardrail ids only) ·
`Open decisions` (ids from state/decisions.yaml) · `Known debt` (ids — do not re-report) ·
`Where truth lives` (links to Tier-3 sources) · `Gotchas`.

## 5. What migrates in, what does not

From the old context layer: capsule contents that are *facts about a capability* move into that
capability's pack (linked, not copied); persona-package knowledge that is domain knowledge moves
the same way; everything procedural (how to triage, how to review, boot rituals) is dropped —
this repository's docs replace it. See
[`../migration/artefact-disposition.md`](../migration/artefact-disposition.md).
