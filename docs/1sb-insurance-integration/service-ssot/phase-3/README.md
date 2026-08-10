# Phase 3 — Functional P0: the Term quote → proposal path

**Parent plan:** [../ACTION-PLAN.md](../ACTION-PLAN.md) · **Status:** [STATUS.md](./STATUS.md) — all six stories **Done**
**Working sequence:** [../WORK-SEQUENCE.md](../WORK-SEQUENCE.md) · **Definition of Done:** [../ROLE-GUIDELINES-AND-DOD.md](../ROLE-GUIDELINES-AND-DOD.md)

---

## What this phase delivered

The first end-to-end functional slice: a bank app can look up master data, request a Term
quote, read the offers, fetch the proposal schema, submit a proposal, and read the result.

```text
FUNC-001 ──► FUNC-002 ──► FUNC-003 ──► FUNC-004 ──► FUNC-005 ──► FUNC-006
master      create        poll quote    proposal      submit       poll proposal
lookup      quote         result        schema        proposal     result
```

Phase 3 stops at proposal submission. **Payment and application status are
[phase-4](../phase-4/README.md)** (FUNC-007, FUNC-009).

---

## Documents in this folder

Every story has an **ASSIGNMENT** (what the Tech Lead asked for, with acceptance criteria)
and a **REVIEW** (the TL + QA Lead verdict). Read them as a pair.

| Story | What it builds | Assignment | Review |
|-------|----------------|-----------|--------|
| **FUNC-001** | `POST /v1/master-data/lookup` — TTL cache, stale fallback, `X-Master-Cache` | [ASSIGNMENT](./FUNC-001-ASSIGNMENT.md) | [REVIEW](./FUNC-001-REVIEW.md) |
| **FUNC-002** | Term quote create — WireMock AC proof, `QUOTE_COMPLETED` / `PARTIAL` | [ASSIGNMENT](./FUNC-002-ASSIGNMENT.md) | [REVIEW](./FUNC-002-REVIEW.md) |
| **FUNC-003** | Get quote job result — status + offers; `TIMEOUT` → 200; 404 `RESOURCE_NOT_FOUND` | [ASSIGNMENT](./FUNC-003-ASSIGNMENT.md) | [REVIEW](./FUNC-003-REVIEW.md) |
| **FUNC-004** | Get proposal schema — pass-through + cache; 410 `QUOTE_EXPIRED` | [ASSIGNMENT](./FUNC-004-ASSIGNMENT.md) | [REVIEW](./FUNC-004-REVIEW.md) |
| **FUNC-005** | Submit Term proposal — AC-1…AC-5 + R6 idempotency-key rule | [ASSIGNMENT](./FUNC-005-ASSIGNMENT.md) | [REVIEW](./FUNC-005-REVIEW.md) |
| **FUNC-006** | Get proposal job result — `applicationNumber` when available; persist + poll wiring | [ASSIGNMENT](./FUNC-006-ASSIGNMENT.md) | [REVIEW](./FUNC-006-REVIEW.md) |

All six reached **TL + QA Lead APPROVE**. Per-story outcomes and commits are in
[STATUS.md](./STATUS.md).

---

## Context you need before reading a story

| Question | Where it's answered |
|----------|--------------------|
| What is the story's full acceptance criteria? | [../PRODUCT-BACKLOG.md](../PRODUCT-BACKLOG.md) |
| Why do ports/adapters exist at all? | [../../architecture/replaceable-middleware.md](../../architecture/replaceable-middleware.md) |
| What does the 1SB endpoint actually return? | [../../api-catalog/README.md](../../api-catalog/README.md) · [../../reference/extracted-schemas/](../../reference/extracted-schemas/) |
| Which fields are mandatory, and when? | [../../field-guides/term-quote.md](../../field-guides/term-quote.md) · [../../field-guides/proposal-and-dynamic-forms.md](../../field-guides/proposal-and-dynamic-forms.md) |
| What must a test look like to pass review? | [../TESTING-RULES.md](../TESTING-RULES.md) |
| What was deferred out of this phase? | [../TECH-DEBT.md](../TECH-DEBT.md) |

---

## Carried forward

- **Insurer sub-status** was explicitly deferred from FUNC-006 → **FUNC-009** ([phase-4](../phase-4/README.md)).
- **Payment ports** were tracked as `TD-009` in [../TECH-DEBT.md](../TECH-DEBT.md) and closed by FUNC-007 in phase-4.
- **Redis-backed master cache** remains open as `TD-010`.

**Previous:** [phase-2](../phase-2/TL-KICKOFF.md) · **Next:** [phase-4](../phase-4/README.md)
