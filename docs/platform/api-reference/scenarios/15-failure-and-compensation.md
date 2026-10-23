# Scenario 15 — When things fail: compensation, `UNCERTAIN`, manual intervention

[← API reference home](../README.md) · Prev: [14](./14-provider-status-sync.md)

**Principle:** timeouts resolve *availability* questions; they never resolve *financial
correctness* questions. Nothing on the money or evidence path is decided by a timer.

## 1. Failure matrix

| # | Failure | What the platform does | Automatic? | Manual owner |
|---|---|---|---|---|
| F-01 | Suitability unavailable at quote | **Fail closed** — quote refused; journey holds | n/a | — |
| F-02 | Consent captured, journey abandoned | Consent kept to its own expiry and retention; journey `ABANDONED`; **no deletion** | Yes | — |
| F-03 | Quote expires mid-proposal | Proposal held in `DRAFT`; journey back to `QUOTING`; captured answers re-applied | Yes | — |
| F-04 | Proposal submitted, insurer never acknowledges | Poll by reference; budget exhausted → `SUBMISSION_FAILED` + ops task. **Never resubmit blind** | Partial | Operations |
| F-05 | Paid, not issued | Journey `COMPENSATING` → bounded issuance retry → `REFUND_REQUIRED` + `REFUND_INITIATED` (maker-checked above threshold) | Retry yes, refund no | Operations + Finance |
| F-06 | Issued, confirmation missing | `CONFIRMATION_OVERDUE` → re-check → `ISSUANCE_DISPUTED` | Partial | Operations + insurer |
| F-07 | Captured, reconciliation break | `RECONCILIATION_BREAK`; out of `SOLD`; manual reconciliation | No | Finance |
| F-08 | PG response lost | `UNCERTAIN`; new attempts blocked; next settlement decides | Yes | — |
| F-09 | Hub cannot route a product | Fail fast with a routing error; stage unchanged | Yes | Product (Group B redirect) |
| F-10 | Audit emission fails | Outbox retries; business commits; journey cannot reach `SOLD` until evidence lands | Yes | — |

## 2. Journey compensation states

`COMPENSATING` exits only to `SOLD`, `COMPENSATED` or `MANUAL_INTERVENTION` — never silently
closed (`INV-JRN-04`). Compensation bookkeeping (`failurePoint`, `attempts`, `ownerTaskId`) lives
in `journey.compensation`; the money itself stays in `payment`.

## 3. Infrastructure failures and their effect

| Dependency down | Effect |
|---|---|
| Valkey | Sessions unreadable → users cannot authenticate (availability, not correctness) |
| PDP | Every regulated action denied (fail closed) |
| Configuration | Actions needing an unresolvable rule refused |
| CBS / Apigee | Customer search `503` "do not proceed" (unless an own lead exists) |
| 1SB / Apigee | New quotes / proposals fail with `503`; already-polled results still readable from `bank_persistence`; breaker per provider |
| MSK | Outbox rows accumulate; nothing lost; alert on outbox age; `SOLD` delayed |
| Aurora | Owning services fail closed; Multi-AZ failover; never serve stale suitability / certification from cache |

## 4. What the RM sees

A typed error with an `incidentId` (read it to support), never a stack trace. Money states
(`UNCERTAIN`, `RECONCILIATION_BREAK`) are shown as "payment being confirmed — do not retry".

## 5. Pages

[Domain model §5.1](../../ws3-platform/01-domain-model-and-invariants.md) · [Error model](../conventions/error-model.md) · [Support runbook](../../../journey-execution/08-SUPPORT-RUNBOOK.md) · [Error and degraded-state catalogue](../../../journey-execution/04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md)
