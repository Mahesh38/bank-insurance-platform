# Decision rights — one table

**D** decides · **V** may veto (binding, only within the stated jurisdiction) · **A** must be asked
for advice · blank = informed at most. AI lenses never hold D or V; they advise in the role's
voice. Type-2 decisions inside a lane are decided by the doer (advice process) and are not listed.

| Decision | Product | Architecture | Engineering | Security | Data | Quality | Compliance | Reliability | Delivery |
|---|---|---|---|---|---|---|---|---|---|
| Outcome order, Now/Next/Later, release content | **D** | A | A | | | A | A | | A |
| Business rules, journey behaviour, acceptance | **D** | A | | A | | A | **V** (permissibility) | | |
| Resolving a requirement conflict (e.g. BRD C1–C7) | **D** | A | | | | A | **V** (permissibility) | | |
| Service boundaries, public/bank contracts (Type-1) | A | **D** | A | A | A | | | A | |
| New runtime component / infrastructure (Type-1) | | **D** | A | A | A | | | **V** (operability) | A (cost/time) |
| Implementation patterns, coding standards | | A | **D** | A | | A | | | |
| Access, secrets, crypto, exposure (G1, G3, G4, G8) | | A | | **D / V** | | | A | A | |
| PII / data sharing (G2) | A | | | **V** | A | | **D / V** | | |
| Money movement, reconciliation (G5) | **D** | A | | A | | A | **V** | | |
| Consent, retention, deletion (G6) | A | | | A | A | | **D / V** | | |
| Data migration / backfill on real data (G7) | | A | A | A | **D / V** | A | A | A | |
| Test strategy, evidence sufficiency for release | A | | A | | | **D** | A | A | |
| Promotion to bank-UAT | A | | **D** (lane lead) | V if G1–G4/G8 changed | | A | | A | A |
| Promotion to prod | **D** | A | A | **V** | A | **V** | **V** | **V** | A |
| Timing of any decision above (not its content) | | | | | | | | | **D** |
| Lanes, WIP limits, cadence, flow thresholds | A | | A | | | | | | **D** |

Two decisions sit outside the table because no single role column holds them:

- **Material risk acceptance** — the accountable human risk owner only. Every role above may be
  asked for advice; none may accept on the owner's behalf, and no AI ever does.
- **Adding, removing or weakening a guardrail** — the guardrail's owner role (in
  [`../state/guardrails.yaml`](../state/guardrails.yaml)) decides; every other role is asked;
  the author of the change may not approve it.

Vetoes are binding inside their jurisdiction and cannot be outvoted. Deadlock between two vetoes
follows the conflict ladder's tie-breaker table ([`../docs/05-decisions.md` §6](../docs/05-decisions.md#6-conflict-ladder)).
