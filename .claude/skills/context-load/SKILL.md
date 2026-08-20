---
name: context-load
description: Resolve a task to the exact set of repository documents worth reading, before reading anything. Use at the start of any non-trivial request in this repository — a review, a design question, a code change, a scaling or incident question, a compliance or delivery question — and whenever you catch yourself about to explore docs/, grep across the repository, open a whole persona package, or read a large document to find one section. Also use when you need to adopt a persona (architect, product owner, security, SRE, QA, DBA, compliance, delivery, BA, engineering) without loading its full package.
---

# Context Load

`docs/` holds ~4.3 MB across 427 files. Exploring it costs more context than the work does, and
guessing lands on the wrong file. This repository routes instead: every task type resolves to a
named **capsule** with an exact, ordered, budgeted read list.

## Run this first

```bash
python3 scripts/context/context-load.py resolve "<the user's request, verbatim>"
```

It prints the persona card to adopt, the files to read in order with their real byte cost, the
conditional reads and their conditions, the required output shape, and the `NEVER` rules for that
task. Read exactly that. Nothing else.

```bash
python3 scripts/context/context-load.py list              # all 19 capsules with cost vs budget
python3 scripts/context/context-load.py show <capsule>    # one capsule's read plan
```

## The three tiers

| Tier | What | Cost | When |
|---|---|---|---|
| **0** | `docs/context/BOOT.md` | ~17 KB | Every session, before anything else |
| **1** | One capsule + the persona card it names | ~6–20 KB | Once the task is known |
| **2** | One named file inside a role package or SSOT | as needed | Only when a card's *Load deeper* row matches |

`BOOT.md` is generated from `CURRENT-STATE.yaml` and CI fails when it drifts, so it is safe to
trust: it already answers the ten facts, the stage posture, the open gates, the standing
constraints and the known debt. Do not read `CURRENT-STATE.yaml`, `RUNBOOK.md` or
`01-CURRENT_STATE.md` to re-derive what BOOT.md states.

## Reading discipline

| | |
|---|---|
| `CTX-1` | Load the capsule, not the folder. Never list a directory to discover what is in it. |
| `CTX-2` | Never load a persona package when the card answers the question. 3–6 KB versus up to 244 KB. |
| `CTX-3` | An anchored entry means **that section only**. `RUNBOOK.md` is 60 KB; section 8 is 6 KB. |
| `CTX-4` | `then_only_if` entries are conditional. Load one only when its condition is true right now. |
| `CTX-5` | Registers are grep targets, not reads. Search for the ID; do not load 34 KB to append a row. |
| `CTX-6` | Cite what you read, by path and anchor. An uncited conclusion is an assumption. |
| `CTX-7` | If the capsule was insufficient, say which question it failed, open **one** more file, and propose the index fix as a suggestion — triaged, not applied in that turn. |
| `CTX-8` | Generated files are regenerated, never hand-edited. |
| `CTX-9` | Two capsules maximum per turn. Needing three means the task is not decomposed — say so. |

## Adopting a persona

The capsule names the card. Read the card, answer from it, and open a package file only when a
*Load deeper* row matches. Who decides what, in one table:
`docs/context/personas/AUTHORITY-QUICK-CARD.md`.

A card lets you **reason** as a persona. It never lets you produce that persona's mandatory human
sign-off — T4 Architecture, Security and Risk & Compliance approvals stay human.

## Keeping the routes true

```bash
python3 scripts/context/context-load.py validate    # paths, anchors, budgets, persona agreement
python3 scripts/context/build-boot-capsule.py       # regenerate BOOT.md after a state change
```

Both run in `scripts/governance/ci-checks.py`. A stale route costs more than no route: the agent
trusts it, fails, and explores anyway. If you move or rename a document, fix
`docs/context/AGENT-CONTEXT-INDEX.yaml` in the same change.

## Never

- Explore `docs/` before running `resolve`.
- Load a persona package to adopt a persona.
- Read a whole file when the index gave you an anchor.
- Add a capsule in the turn the need for it is discovered — that is a suggestion, and suggestions
  are triaged first.
