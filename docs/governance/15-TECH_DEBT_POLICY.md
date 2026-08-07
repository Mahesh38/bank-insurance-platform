# 15 — Technical Debt Policy

**Layer:** L1 (policy) + L3 (ledger)
**Ledger:** [service-ssot/TECH-DEBT.md](../1sb-insurance-integration/service-ssot/TECH-DEBT.md)
**Owner:** Tech Lead

---

## 1. Definition

> **Debt is a shortcut you chose, knowing the cost.**

If you did not know, it is a **bug**. If there is no cost, it is not debt — it is a decision.
The distinction is not pedantic: debt gets an expiry date and an owner, bugs get a root cause,
and decisions get an ADR.

| It is… | When |
|--------|------|
| `DEBT` | Deliberate, recorded at the time, with a known remediation |
| `BUG` | Unintended deviation from an approved specification |
| `ARCH` | The design itself is wrong — the shortcut is not the problem |
| Not debt | A choice that is simply correct for the current stage (in-memory idempotency on a single instance is *right*, not debt — until scale-out is on the roadmap) |

That last row matters. **Stage-appropriate simplicity is not debt.** Logging it as debt creates
guilt-driven backlog noise and hides real debt.

---

## 2. Taking on debt

Permitted, in one of two ways:

| Route | When | Requires |
|-------|------|----------|
| **Planned** | The plan states the shortcut up front | Recorded in the plan; Technical board acknowledges it |
| **Discovered mid-implementation** | A cleaner path proves costlier than the item's value | Logged before merge; TL approves |

Every debt entry requires, without exception:

```yaml
debt:
  id: TD-024
  severity: P1                 # P0 blocks now · P1 this stage · P2 before prod · P3 hygiene
  title: "In-memory idempotency store; not multi-instance safe"
  origin: "FUNC-007 implementation, plan PLAN-011"
  why_taken: "Redis adapter is Phase 5.4 work; single instance in UAT makes it safe today"
  cost_if_unpaid: "Duplicate payment sessions once the service scales horizontally"
  remediation: "Replace with Redis-backed store (TD-010)"
  owner: "Tech Lead"
  expiry: "Phase 5 gate"       # a stage or a date — never 'someday'
  blocks: ["horizontal scale-out"]
```

> **Rule TD-1 — No expiry, no debt entry.** An item without an expiry is not tracked debt; it is
> an accepted permanent condition, and should be recorded as a decision (ADR) instead. Being
> honest about which one it is prevents a ledger full of items nobody intends to fix.

---

## 3. Severity

| Severity | Meaning | Handling |
|----------|---------|----------|
| **P0** | Blocks the current stage or multi-service reuse | Fix in this stage; cannot pass the gate with it open |
| **P1** | Fix within the current stage | Scheduled like any P2 work item |
| **P2** | Must be repaid before production | Expiry = production readiness gate |
| **P3** | Hygiene | Repaid opportunistically when touching the same code |

Severity is not priority — it is the *deadline class*. A P2 debt item's `priority_now` still
comes from [05](./05-PRIORITY_MODEL.md), and will typically be P3/P4 until its expiry stage
approaches, at which point it becomes P1/P2 automatically.

---

## 4. Expiry enforcement

```text
At every stage gate:
  For each open debt item whose expiry is this stage or earlier:
     ├─ repaid            → close with the actual remedy
     ├─ still needed      → the shortcut is now permanent: convert to an ADR
     │                      (an accepted condition), and close the debt entry
     └─ neither           → BLOCKS THE GATE unless a waiver is approved (14 §1)
```

A waiver requires: a new expiry, a named owner, and a recorded reason. Waiving twice is a
signal the item is not actually going to be repaid — resolve it honestly by converting it to an
ADR or scheduling it as P1 work.

---

## 5. Repayment budget

Debt repayment competes with features unless it has protected capacity.

| Stage | Suggested repayment capacity |
|-------|------------------------------|
| Foundation / Connectivity (L4–L5) | 0–10% — the codebase is young; prefer getting the shape right |
| Vertical slice (L6) | 10% — repay only what blocks the slice |
| **Hardening (L7)** | **25–30% — this is the designated repayment stage** |
| Expansion (L8) | 15% — repay what the second journey exposes |
| Production readiness (L9) | 20% — all P2 debt must clear |
| Operate (L10) | 15–20% steady state |

Hardening is where debt is meant to be repaid. Debt discovered at L6 that is not blocking the
slice should be logged and left — repaying it early is itself a form of stage drift.

---

## 6. Debt discovered by an agent

```text
Is it a deliberate shortcut recorded at the time?
  ├─ YES  → it is already in the ledger. Do not re-report. Link to it.
  └─ NO   → is behaviour wrong against an approved specification?
             ├─ YES → BUG (06 §3), with the violated spec named
             └─ NO  → is it merely simpler than a later stage will need?
                       ├─ YES → NOT debt. Stage-appropriate. Say so and move on.
                       └─ NO  → new TD-### with all fields from §2
```

Agents must check [01 §6](./01-CURRENT_STATE.md#6-known-open-debt-affecting-triage) before
raising debt. TD-006, TD-007, TD-009, TD-010, TD-014, TD-022, TD-023 and QA-001 are known —
re-reporting them is noise, and noise is how real findings get ignored.

---

## 7. Debt and the review boards

| Board | Debt question |
|-------|---------------|
| Architecture | Does this shortcut create a *migration* problem, or only a *cleanup* problem? Migration debt is far more expensive |
| Technical | Is the remediation actually known, or are we hoping? |
| Security | Is there a debt-shaped exposure window? Security debt gets the shortest expiry |
| Risk & Compliance | Is any regulatory obligation being deferred? **Compliance debt is not permitted** — it is a violation with a delay |
| QA | Does the shortcut reduce testability, hiding future defects? |
| Ops | Does it increase operational load or incident likelihood? |

Two hard rules: **compliance debt is never accepted**, and **security debt carries an expiry no
later than the next gate**.

---

## 8. Health signals

Tracked in [18](./18-GOVERNANCE_METRICS.md):

| Signal | Healthy | Concerning |
|--------|---------|------------|
| Open P0/P1 debt | 0 / ≤ 3 | P0 open at a gate |
| Average debt age | < 1 stage | > 2 stages |
| Waiver rate | < 10% | > 25% — expiries are fiction |
| Debt created per stage | Flat or falling | Rising through hardening |
| Repayment vs creation ratio | ≥ 1 during hardening | < 0.5 anywhere |
| Items re-parked twice | 0 | > 2 |
