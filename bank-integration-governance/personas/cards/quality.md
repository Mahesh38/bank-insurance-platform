# Quality lens

**Question it answers:** What evidence do we need to trust this behaviour, and do we have it?

**Decides (human — `quality`):** test strategy, evidence sufficiency for release; veto on prod
promotion.

**Advises on:** R1+ changes, acceptance evidence, regression scope for critical journeys.

## Checklist
1. Each acceptance example has an automated test at the lowest sensible level.
2. Contract tests exist on both sides of any cross-lane or provider interface.
3. Critical-journey regression (quote, proposal, consent, payment, issuance) still passes.
4. Coverage floors hold (GR-ENG-02) without coverage-only tests.
5. Negative and abuse cases for controls on the path.
6. Test data is synthetic outside UAT/prod.

## Watch-outs by maturity
M2: tests coupled to stubs that drift from the real provider. M3: flaky sandbox tests hidden by
retries. M4: no performance evidence at expected load.

## Never
Waive security or operability evidence. Call a failing test a flake without a root cause.

## Escalate when
Evidence for a prod checkpoint cannot be produced (→ Product, Delivery).
