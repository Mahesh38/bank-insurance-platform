# Phase 4 — Remaining P0: payment session & application status

**Parent plan:** [../ACTION-PLAN.md](../ACTION-PLAN.md) · **Status:** [STATUS.md](./STATUS.md) — both stories **Done**
**Working sequence:** [../WORK-SEQUENCE.md](../WORK-SEQUENCE.md) · **Definition of Done:** [../ROLE-GUIDELINES-AND-DOD.md](../ROLE-GUIDELINES-AND-DOD.md)

---

## What this phase delivered

The P0 Term-path stories that [phase-3](../phase-3/README.md) did not cover — closing the
journey from proposal submission through payment to application status.

```text
phase-3 ends here                    phase-4
─────────────────────                ───────────────────────────────
FUNC-006 poll proposal  ──►  FUNC-007 payment session  ──►  FUNC-009 application status
```

| Story | What it builds | Assignment | Review |
|-------|----------------|-----------|--------|
| **FUNC-007** | `POST /v1/payments` → `OneSbPaymentAdapter` → persisted session. Payability check via `JobStorePort` → 409 `PROPOSAL_NOT_PAYABLE`. Non-HTTPS upstream URL → 502. `paymentUrl` never logged or audited. | [ASSIGNMENT](./FUNC-007-ASSIGNMENT.md) | [REVIEW](./FUNC-007-REVIEW.md) |
| **FUNC-009** | `GET /v1/status/{applicationNumber}` → `OneSbStatusAdapter` → normalised `BankApplicationStatus`. Raw 1SB status never leaves the service (audit-only). | [ASSIGNMENT](./FUNC-009-ASSIGNMENT.md) | [REVIEW](./FUNC-009-REVIEW.md) |

---

## ⚠️ Two variances recorded honestly

Both are documented rather than silently absorbed — read them before treating this phase as
a template.

1. **Scope variance vs. ACTION-PLAN.** [../ACTION-PLAN.md](../ACTION-PLAN.md) describes
   Phase 4 as *"hardening & consumer enablement"* (E2E, OpenAPI, bank consumer spike). What
   was actually delivered here is the remaining **functional P0 stories**. The hardening
   scope was not done and remains open.

2. **Process variance vs. WORK-SEQUENCE §3.** No QA Engineer / QA Lead cycle ran this phase
   — this was a single-agent branch, so it had **Tech Lead review only**. The full Functional
   P0 sequence expects a dual TL + QA Lead approval, as happened throughout
   [phase-3](../phase-3/README.md).

Gradle checks (`:services:1sb-integration-service:test`, `:libs:bank-common-error:test`),
`ArchitectureTest`, and coverage gates were green for every commit on the branch.

---

## Deferred out of this phase

| Item | Tracked as | Where |
|------|-----------|-------|
| **FUNC-008** payment intimation — port method stubbed, not implemented | `TD-022` | [../TECH-DEBT.md](../TECH-DEBT.md) |
| Status-snapshot persistence — deliberate scope decision, not an oversight | documented in [STATUS.md](./STATUS.md) | — |

New shared error code `PROPOSAL_NOT_PAYABLE` was added to `bank-common-error`.

---

## Context you need before reading a story

| Question | Where it's answered |
|----------|--------------------|
| Full acceptance criteria | [../PRODUCT-BACKLOG.md](../PRODUCT-BACKLOG.md) |
| How 1SB's payment model works, and why the bank uses its own PG | [../../field-guides/payment.md](../../field-guides/payment.md) |
| Status value mapping (1SB → bank) | [../../field-guides/application-status.md](../../field-guides/application-status.md) |
| Persistence contract used for payment sessions | [../../architecture/bank-persistence-service.md](../../architecture/bank-persistence-service.md) |
| Test rules that gate review | [../TESTING-RULES.md](../TESTING-RULES.md) |

**Previous:** [phase-3](../phase-3/README.md) · **Backlog:** [../PRODUCT-BACKLOG.md](../PRODUCT-BACKLOG.md)
