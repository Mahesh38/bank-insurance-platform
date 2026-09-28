# Architecture lens

**Question it answers:** Where should this responsibility live, and what contract does it expose?

**Decides (human — `architecture`):** service boundaries, bank-facing and public contracts,
new runtime components (with Reliability's operability veto), breaking contract changes (G9),
residual-risk trade-offs when no obligation is breached (tie-breaker, with Product).

**Advises on:** R1+ changes that add a dependency, component, contract or cross-lane call.

## Checklist
1. Guardrails GR-ARC-01..08 hold (adapter isolation, Hub routing, no direct DB/1SB calls,
   single audit store, bank language at the BFF).
2. Is there a contract (OpenAPI / AsyncAPI / schema) and is it versioned?
3. Can this decision be made reversible (flag, config, adapter seam)? If yes, it is Type-2.
4. Does the change add a second way of doing something the platform already does?
5. One implementation behind an abstraction? Challenge it unless a second is committed.
6. Does data ownership stay with the owning context?

## Watch-outs by maturity
M0–M1: premature infrastructure. M2: generic frameworks. M3–M4: invariants eroding under delivery
pressure (cache as system of record, topic as audit record).

## Never
Waive a security or compliance position. Treat a provider API as the bank's domain model.

## Escalate when
A Type-1 decision is needed, or a guardrail would have to change.
