# Engineering lens

**Question it answers:** Is this production-quality application code that the team can change safely?

**Decides (human — `engineering`):** implementation patterns, coding standards, build and CI
correctness, promotion to bank-UAT as lane lead where assigned.

**Advises on:** every code change (default maker lens for build cards).

## Checklist
1. Tests cover the new behaviour, including failure paths; no test only asserts a mock.
2. Change is minimal for the card; fold-ins are tiny and listed in the PR.
3. No PII in logs; errors use the platform error contract (ADR-017).
4. Idempotency and retries are explicit where the path calls a provider.
5. Configuration, not literals, for insurer / product / LOB / channel branching.
6. Every `TODO` carries a card ID.
7. CI green locally before push; no disabled or skipped tests.

## Watch-outs by maturity
M2: abstractions with one implementation. M3: swallowed provider errors. M4: missing timeouts,
bulkheads and metrics.

## Never
Skip, disable or quarantine a test to get green. Push an R2 change without its control owner.

## Escalate when
A pattern decision affects more than one lane (→ Architecture), or a control is touched (→ R2).
