# Canonical model — the bank-owned domain language

**Up:** [docs index](../../README.md) → [1SB integration](../README.md) → **canonical model**

---

## Why this exists

1SB payloads are large because one schema serves many insurers and many lines of business.
If those shapes reach bank APIs, the bank inherits 1SB's coupling — and replacing 1SB later
becomes a rewrite instead of an adapter swap.

The canonical model is the answer: **a bank-owned, LOB-agnostic vocabulary**, split into
small bounded contexts. 1SB field names appear **only inside adapters**.

**Masters:** quote-category 1SB lookup is documented as gateway-common; proposal-category
lookup is documented as insurer-specific (`manufacturerId`). The Integration Hub may
ingest those feeds into **Hub masters** (common vs insurer-keyed). BFF/UI receive Hub
codes and labels, never 1SB wire values. See [building-blocks.md](../field-guides/building-blocks.md)
and [replaceable-middleware.md](../architecture/replaceable-middleware.md).

| Document | What it covers |
|----------|---------------|
| [contexts.md](./contexts.md) | The bounded contexts and the language each one owns |
| [simplifying-payloads.md](./simplifying-payloads.md) | How to split large 1SB payloads into small per-context APIs |

---

## Where this is enforced

| Concern | Document |
|---------|----------|
| Why replaceability matters | [../architecture/replaceable-middleware.md](../architecture/replaceable-middleware.md) |
| Module boundaries and ports | [../architecture/1sb-integration-service-architecture.md](../architecture/1sb-integration-service-architecture.md) |
| The 1SB shapes being translated *from* | [../reference/extracted-schemas/README.md](../reference/extracted-schemas/README.md) |
| Journey stages the contexts map onto | [../journeys/universal-lob-journey.md](../journeys/universal-lob-journey.md) |
