# Persona Decision Cards — tier-1 context

**A card is how you adopt a persona. A package is where you go when the card sends you.**

The persona packages under [`../roles/`](../roles/README.md) hold the full reasoning for each
role — 28 KB to 244 KB each. Loading one to answer a scoped question spends most of a context
window before any thinking happens. Each card here compresses one persona into the part that
changes behaviour: what it **owns**, what it **must never decide alone**, the boundaries it
crosses most, its severity scale, its output shape, and a table mapping each kind of question to
the *one* package file that answers it.

| | Card | Package | Ratio |
|---|---|---|---|
| Mahesh — Architecture | 6 KB | 244 KB | **41×** |
| Deepali — Security | 3 KB | 75 KB | **23×** |
| Shivanshi — SRE | 4 KB | 72 KB | **19×** |
| Shailja — Compliance | 3 KB | 65 KB | **20×** |
| Rajal — Product | 4 KB | 67 KB | **19×** |

## The cards

| Persona | Seat | Card |
|---|---|---|
| Rajal — Principal Insurance Platform Product Owner | Board 3 · `R1` | [rajal-product](rajal-product.card.md) |
| Principal Insurance Platform Business Analyst | `R11` | [ba-r11-business-analysis](ba-r11-business-analysis.card.md) |
| Mahesh — Principal Insurance Platform Architect | Board 1 · `R2` | [mahesh-architecture](mahesh-architecture.card.md) |
| Amit — Technical Head / Principal Engineering | Board 2 · `R3` | [amit-engineering](amit-engineering.card.md) |
| Deepali — Principal Security Architect | Board 4 · `R8` | [deepali-security](deepali-security.card.md) |
| Aarti — Principal Data & Database Architect | Specialist | [aarti-database](aarti-database.card.md) |
| Swapnali — Principal Quality Engineering / QA Lead | Board 5 · `R7` | [swapnali-qa](swapnali-qa.card.md) |
| Shailja S — Compliance & Risk Head | Board 6 · `R9` | [shailja-compliance](shailja-compliance.card.md) |
| Shivanshi — Principal SRE / Reliability Head | Board 7 · `R10` | [shivanshi-sre](shivanshi-sre.card.md) |
| Kalpana — Principal Delivery Head | `R12` | [kalpana-delivery](kalpana-delivery.card.md) |

**Who decides what, in one table:** [AUTHORITY-QUICK-CARD.md](AUTHORITY-QUICK-CARD.md).

## How to use one

1. Resolve the task: `python3 scripts/context/context-load.py resolve "<the request>"`.
   The capsule names the persona and loads the card for you.
2. Read the card. Answer from it if it is sufficient — it usually is.
3. Open a package file **only** when a *Load deeper* row matches the question in front of you.
   Open one file, not the package.
4. Cite the paths you actually read. An uncited conclusion is an assumption.

## What a card is not

- **Not binding.** Cards are non-binding context. The canonical authority is
  [`PERSONA-AUTHORITY-MATRIX.md`](../../governance/PERSONA-AUTHORITY-MATRIX.md), and
  `docs/governance/` wins on any conflict.
- **Not an approval.** A card lets an agent *reason* as a persona. It never lets an agent
  produce that persona's mandatory human sign-off — T4 Architecture, Security and Risk &
  Compliance approvals stay human.
- **Not a new persona.** Every card names an existing seat. Never instantiate a second
  architect, delivery lead, SRE or product owner because a card was convenient.
- **Not a package summary.** A card carries decision-changing content only. When a question
  needs depth, the card sends you to exactly one file.

## Keeping them honest

Cards are derived. When a package or the authority matrix changes, the card changes in the same
change — a stale card is worse than no card. CI checks that every card exists and that every
persona in [`AGENT-CONTEXT-INDEX.yaml`](../AGENT-CONTEXT-INDEX.yaml) matches a role in
[`context-manifest.yaml`](../context-manifest.yaml):

```bash
python3 scripts/context/context-load.py validate
```
