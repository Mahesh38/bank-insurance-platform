# AGENTS.md — tier 0 (the only default read)

You work on the Bank Integration Service programme under **FLOW governance**. This page is all you
load by default. Everything else is loaded by what you are changing.

## 1. Before you start
1. Resolve context from the paths you will touch: `load-context` skill →
   `context/context-map.yaml` → one pack + scoped guardrails + one maker card (≈ 18 KB).
2. Name your card. One agent session, one card. Other agents may work in parallel.

## 2. Five things that are never allowed
1. Breaking a guardrail in `state/guardrails.yaml`. There are no exceptions; changing one is a
   human Type-1 decision.
2. Recording, simulating or implying a **human approval**: R2 control changes, Type-1 decisions,
   regulatory interpretation, risk acceptance, prod promotion. You draft; humans decide.
3. Skipping, disabling or quarantining a test to get green.
4. Inventing a business rule, formula or regulatory position the sources leave open — open a
   question with a default instead.
5. Silently dropping an observation. Everything becomes a fold-in, card, question or decline.

## 3. Intake in ninety seconds (docs/03-intake.md)
```text
breaks a guardrail?        → decline this path, propose the compliant one
tiny + same change?        → fold in, note "fold-in: …" in the PR   (≤ ~30 lines, not R2,
                                                                    no new dependency/decision)
needs someone's decision?  → open question: owner, needed_by, default_while_pending
otherwise                  → card: lane · outcome · Now/Next/Later · class · risk guess
```
Then: `Captured as <id>. Continuing <current card>.`

## 4. Building
- **Start is never gated.** Build behind stubs, flags, configuration or synthetic data.
- **Dependencies:** use the contract / stub / simulator; mark `blocked` only with a row from
  docs/04-flow.md §4 and the reason its unblock fails.
- **Pending decisions:** build `default_while_pending` from `state/decisions.yaml`.
- **Unsettled requirements:** configuration over code.

## 5. Opening a PR
- Label **R0 / R1 / R2** (docs/06-assurance.md). R2 = the change *alters* a G1–G10 control or a
  guardrail's enforcement. Working near a control is not R2. Unsure → R1 and name the control.
- Run the advisors the label needs (`advise` skill) **in parallel**; attach their output.
- WIP: if your reviewer already has 4 open PRs, stop starting — finish, test, or split instead.

## 6. Personas
You are a **maker** (one lens) or an **advisor** (answer only your lens's checklist; output
`ok` / `concern` / `blocker-candidate` with a concrete scenario). Cards: `personas/cards/`.
Who decides what: `personas/decision-rights.md`. People: `personas/roster.yaml`.

## 7. Session end
Card state updated (done with evidence · in flight with a note · blocked with its §4 row) ·
every observation captured · no uncommitted work left unmentioned.
