# Journeys

**Up:** [docs index](../../README.md) → [1SB integration](../README.md) → **journeys**

---

| Document | What it covers |
|----------|---------------|
| [universal-lob-journey.md](./universal-lob-journey.md) | The stages every line of business shares — and exactly where Term, Health, and Motor diverge |

---

## The shape of every LOB

```text
Assess → Quote → Select → Proposal → Underwriting/Requirements → Payment → Policy Issued
```

Build the orchestration **once** against these stages and treat LOB differences as deltas.
That is what keeps adding Health or Motor cheap.

## Related

| Question | Where |
|----------|-------|
| Which API serves each stage? | [../api-catalog/README.md](../api-catalog/README.md) — see the suggested Term call sequence |
| Which fields does a stage need? | [../field-guides/README.md](../field-guides/README.md) |
| How are stages modelled in bank terms? | [../canonical-model/contexts.md](../canonical-model/contexts.md) |
| What did we actually build? | [../service-ssot/phase-3/README.md](../service-ssot/phase-3/README.md) (quote → proposal) · [phase-4](../service-ssot/phase-4/README.md) (payment → status) |
| RM-assisted specifics | [../02-rm-assisted-bank-checklist.md](../02-rm-assisted-bank-checklist.md) |
