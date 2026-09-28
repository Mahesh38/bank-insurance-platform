# Personas — lenses, not people; knowledge lives elsewhere

## 1. What changed and why

| AIGEM | FLOW | Why |
|---|---|---|
| 10 named personas, each a person-shaped package of 40–316 KB | 10 **role lenses**, each a card ≤ 2.5 KB | A lens says *how to look and what may be decided*; it does not need to carry the domain |
| Domain knowledge inside persona packages | Domain knowledge in **capability context packs** ([`../context/`](../context/README.md)) | To know about consent you load the consent pack, not a person. Any lens can use any pack |
| Names are identities ("Mahesh", "Deepali") | Names are **data** in [`roster.yaml`](./roster.yaml) — role → human holder, deputy | People change seats; the lens does not. One human can hold several roles cleanly ([05 §5](../docs/05-decisions.md#5-one-person-many-hats)) |
| A persona is a board seat that approves | A persona is an **advisor** (AI) or a **decision role** (human) | AI advises; humans decide. The split is structural, not a caveat |
| Five bilateral protocols + an operating model | One handoff shape + one conflict ladder ([05 §6–7](../docs/05-decisions.md#6-conflict-ladder)) | O(n) instead of O(n²) |
| 40 KB authority matrix | One table: [`decision-rights.md`](./decision-rights.md) | Most cells in the old matrix were `C` (consulted); only decisions and blocks matter |

## 2. How an agent uses a persona

An agent always works in one of two modes:

- **Maker** — doing a card. Wear **one** maker lens (usually `engineering`, `analyst`,
  `architecture` or `reliability`), load the lane's context pack, build.
- **Advisor** — asked to review a change or a decision. Wear the requested lens, answer **only**
  its checklist, output `ok` / `concern` / `blocker-candidate` with a concrete scenario.

A maker may call several advisors **in parallel** (the [`advise`](../.claude/skills/advise/SKILL.md)
skill). Advisors do not talk to each other; the maker (or the human decider) reconciles.

## 3. The cards

| Lens | Card | Human decision role (see roster) |
|---|---|---|
| Product | [product.md](./cards/product.md) | Product owner |
| Business analysis | [analyst.md](./cards/analyst.md) | Principal BA (Product delegate) |
| Architecture | [architecture.md](./cards/architecture.md) | Architecture lead |
| Engineering | [engineering.md](./cards/engineering.md) | Engineering lead |
| Security | [security.md](./cards/security.md) | Security lead |
| Data | [data.md](./cards/data.md) | Data & database lead |
| Quality | [quality.md](./cards/quality.md) | QA lead |
| Compliance & risk | [compliance.md](./cards/compliance.md) | Compliance & risk lead |
| Reliability | [reliability.md](./cards/reliability.md) | SRE lead |
| Flow steward | [flow-steward.md](./cards/flow-steward.md) | Delivery lead |

## 4. Card contract

Every card has exactly these sections, and `validate.py` enforces the size budget:

```text
Question it answers · Decides (human holder) · Advises on · Checklist (≤ 8 lines)
Watch-outs by maturity · Never · Escalate when
```

Deep material from the old persona packages is not copied. Where it holds real domain knowledge,
it moves into a context pack during migration
([`../migration/artefact-disposition.md`](../migration/artefact-disposition.md)).
