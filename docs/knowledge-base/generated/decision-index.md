# Decision Dashboard

> **Git-native navigation view.** Open the authoritative record before relying on a decision. A document existing in the repository does not mean it is ratified.

## Decision sources

- [Decision Register](../../governance/registers/DECISION-REGISTER.md) — consolidated governance decision index.
- [Change Requests](../../governance/change-requests/) — controlled change packages (`CR-*`).
- [Architecture Decision Log](../../platform/architecture-review/08-architecture-decision-log.md) — architecture decisions (`ADR-*`).
- [Business Decision Log](../../au-bank-insurance-platform/DECISION-LOG.md) — product/business decisions.

## Identifier families

| Prefix | Meaning |
|---|---|
| `ADR-*` | Architecture Decision Record. |
| `CR-*` | Change Request. |
| `DEC-*` | Business/domain/governance decision package. |
| `GOV-*` | Governance decision. |
| `DB-DEC-*` | Database/data architecture decision. |
| `RISK-*` | Risk statement/ownership; not itself an approval. |

## Status rule

Always read the status inside the record. `PROPOSED`, `CANDIDATE` or `AI-DRAFTED` is not equivalent to `APPROVED`/`RATIFIED`; T4 records may still require mandatory human signatures.
