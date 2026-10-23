# Scenario 14 — Keeping provider state in sync (polling, status refresh, requirements)

[← API reference home](../README.md) · Prev: [13](./13-policy-issuance-and-sold.md) · Next: [15](./15-failure-and-compensation.md)

**Status:** adapter polling and status **IMPLEMENTED**; Hub and domain-side sync **PROPOSED**.

**R0 never receives provider webhooks.** Every provider state change is learnt by **polling** —
quote and proposal jobs by the adapter, application / UW / issuance status by scheduled re-checks.

## 1. Three synchronisation loops

| Loop | Who runs it | Cadence | Writes | Ends when |
|---|---|---|---|---|
| **Job poll** (quote, proposal) | 1SB adapter `AsyncJobPoller` on `pollingExecutor` | 1 s × 2 → 30 s cap, ≤ 20 attempts | `job_poll_attempt` per try; final status + offers / application number on the job | `isPollComplete`, or `TIMEOUT` / `FAILED` |
| **Result pull** (adapter → domain) | Quotation / Proposal via the Hub | On RM poll (S-11/S-12 budgets) or a scheduled sweep | Domain working item (DynamoDB `quotation-working`, `proposal-working`) | Terminal canonical state |
| **Application status sync** (UW, requirements, issuance) | Proposal (UW) and Policy (issuance) via Hub `APPLICATION_STATUS` → adapter `GET /v1/status/{applicationNumber}` | Scheduled while non-terminal (S-16 re-check) | Proposal UW state + `uw_requirement`; Policy state | `UW_APPROVED`/`DECLINED`; `ISSUED`/`CONFIRMED`; SLA breach → overdue / disputed |

## 2. Status normalisation

The adapter turns 1SB `applicationStatus` + manufacturer sub-status into `BankApplicationStatus`;
the domain maps that to its own state machine. Raw provider strings are kept only in the encrypted
raw payload store and audit metadata.

| `BankApplicationStatus` (implemented) | Proposal state (PROPOSED) | Policy state (PROPOSED) |
|---|---|---|
| `PROPOSAL` | `SUBMITTED` | — |
| `UW_IN_PROGRESS` | `UNDER_WRITING` | — |
| `UW_REQUIREMENTS` | `REQUIREMENTS_PENDING` (+ requirements) | — |
| `CUSTOMER_DECISION` | `UW_COUNTER_OFFER` | — |
| `PAYMENT` | `UW_APPROVED` → `AWAITING_PAYMENT` | — |
| `ISSUED` | `PAID` → `CONVERTED` | `ISSUED` (+ policy number) |
| `CLOSED` | `UW_DECLINED` / `WITHDRAWN` per sub-status | `ISSUANCE_REJECTED` |
| `UNKNOWN` | unchanged; alert | unchanged |

The exact sub-status mapping table is WS-1's (`application-status` field guide) and must be
confirmed with the insurer per product.

## 3. Idempotency and duplicates

- A status poll that returns the same status is a no-op (no new transition, no new event).
- A status change emits exactly one domain event from the owning service's outbox.
- Multi-pod job ownership (`owned_by_instance`, optimistic `version`) is Phase 5.4 (parked):
  today each pod polls only the jobs it created.

## 4. Pages

[1SB adapter §2, §3.8](../services/1sb-integration-service.md#38-get-v1statusapplicationnumber) · [Integration Hub §4](../services/integration-hub-service.md#4-synchronisation-with-providers-who-polls-what) · [1SB adapter to 1SB](../interactions/onesb-adapter-to-1sb.md) · [Application status field guide](../../../1sb-insurance-integration/field-guides/application-status.md)
