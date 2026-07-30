# CONFIRM-04 — SSOT Kickoff Agenda

**Phase:** 0.4  
**Status:** `PENDING` — kickoff not yet scheduled  
**Owner:** Tech Lead  
**SSOT link:** [ACTION-PLAN.md row 0.4](../ACTION-PLAN.md) · [00-po-architect-design-session.md](../00-po-architect-design-session.md)

---

## Purpose

A single kickoff session to walk the team through the SSOT (decisions, backlog order, DRY/KISS principles) and remove open design blockers before implementation begins.  
**Exit criterion:** Team agrees Case 2 + **Life-first** (Saving + Term subtypes); Saving E38 as first vertical unless Term products arrive first; no open design blockers remain.

---

## Suggested attendees

| Role | Name | Sign-off |
|------|------|----------|
| Tech Lead (facilitator) | TBD | ⬜ Pending |
| System Architect | TBD | ⬜ Pending |
| Product Owner | TBD | ⬜ Pending |
| Lead Developer(s) | TBD | ⬜ Pending |
| Security representative | TBD | ⬜ Pending |
| QA Lead | TBD | ⬜ Pending |

---

## Agenda (suggested 90 min)

| # | Topic | Duration | Facilitator | Exit check |
|---|-------|----------|-------------|------------|
| 1 | Intro & goals of kickoff | 5 min | Tech Lead | — |
| 2 | SSOT walk: [00-po-architect-design-session.md](../00-po-architect-design-session.md) — binding decisions D1–D14 | 15 min | Architect | Team can articulate D7 (distributorId) and D6 (agentId) |
| 3 | Architecture walk: Case 2 (Service → LobHandler), ports/adapters, package layout | 20 min | Architect | Team understands why Health/Motor don't touch QuoteService |
| 4 | Backlog order walk: [PRODUCT-BACKLOG.md](../PRODUCT-BACKLOG.md) P0 sequence | 15 min | Tech Lead | Team agrees SHARED/TECH → Term → Health/Motor; no skipping |
| 5 | DRY/KISS discussion: what "one poller, one HTTP client" means in practice | 10 min | Architect | No one proposes per-LOB polling infra |
| 6 | Phase 0 confirmations: 0.1/0.2/0.3 status review | 10 min | PO | Blockers identified and parked if not resolved |
| 7 | Open design questions | 10 min | Tech Lead | List of open items with owners captured below |
| 8 | Confirm tracking board approach (0.5) | 5 min | PO | Story IDs from backlog → Jira/Linear agreed |

---

## Pre-read pack (must read before attending)

- [ ] [00-po-architect-design-session.md](../00-po-architect-design-session.md)
- [ ] [ACTION-PLAN.md](../ACTION-PLAN.md)
- [ ] [PRODUCT-BACKLOG.md](../PRODUCT-BACKLOG.md)
- [ ] [architecture/1sb-integration-service-architecture.md](../../architecture/1sb-integration-service-architecture.md)
- [ ] [architecture/replaceable-middleware.md](../../architecture/replaceable-middleware.md)

---

## Key design points the team must agree before ending the session

### Case 2 orchestration (not Case 1 or Case 3)

```
Bank Request
    │
    ▼
QuoteService (shared orchestrator: job create, poller dispatch, response map)
    │
    ▼
LobHandler (Term / Health / Motor — LOB-specific mapping only)
    │
    ▼
OneSbHttpClient (single HTTP stack)
```

- **Must not** create separate orchestration paths per LOB.
- **Must not** create per-LOB HTTP clients.

### Term-first (no Health/Motor until Term sandbox is green)

As per KISS principle and ACTION-PLAN.md Phase 3:  
> *Do not start Health/Motor until Term exit is met.*

The team must explicitly agree this in the session.

### No platform creep

Out of scope (must not be built in this service):

- CIF / RM UI
- Suitability engine
- Payment money movement / ledger
- Claims or renewals
- Multi-aggregator routing (port-ready, not implemented)

### LOB feature flags

LOBs enabled/disabled via config: `LOB_TERM_ENABLED`, `LOB_HEALTH_ENABLED`, `LOB_MOTOR_ENABLED`.  
Disabling a LOB returns `LOB_NOT_SUPPORTED` — no code removal required.

---

## Open design questions log (fill during session)

| # | Question | Raised by | Owner | Resolution | Status |
|---|----------|-----------|-------|------------|--------|
| Q1 | _(add during session)_ | — | — | — | ⬜ Open |

---

## Post-kickoff sign-off

By signing off, attendees confirm:
1. Case 2 architecture is accepted.
2. Term-first delivery order is accepted.
3. Binding decisions D1–D14 are understood and will be followed.
4. No open design blockers remain (or blockers are parked with explicit owner + deadline).

| Role | Name | Date | Sign-off |
|------|------|------|----------|
| Tech Lead | | | ⬜ Pending |
| Architect | | | ⬜ Pending |
| PO | | | ⬜ Pending |
| Dev Lead | | | ⬜ Pending |

---

## Checklist (exit criteria for 0.4)

- [ ] Session scheduled with all required attendees
- [ ] Pre-read pack distributed at least 2 days before session
- [ ] Case 2 + Term-first confirmed by team (sign-off table above)
- [ ] No open design blockers (or each blocker has owner + resolution date)
- [ ] Tracking board approach confirmed (feeds 0.5)
- [ ] Action items from session captured in TODO-TRACKER.md
