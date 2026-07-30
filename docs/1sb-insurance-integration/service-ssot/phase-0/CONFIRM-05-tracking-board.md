# CONFIRM-05 — Tracking Board Setup

**Phase:** 0.5  
**Status:** `PENDING` — Jira/Linear tickets not yet created  
**Owner:** PO / Scrum Master  
**SSOT link:** [ACTION-PLAN.md row 0.5](../ACTION-PLAN.md) · [PRODUCT-BACKLOG.md](../PRODUCT-BACKLOG.md)

---

## Purpose

Create a 1:1 ticket mapping from every story ID in `PRODUCT-BACKLOG.md` into the team's tracking tool (Jira or Linear) so implementation can be tracked against the SSOT backlog without drift.

---

## Backlog seed file

Full importable story list with checkboxes:

**[p0-story-board.md](./p0-story-board.md)**

That file contains:
- All P0 story IDs grouped by epic
- Suggested ticket title per story
- Import instructions for Jira CSV and Linear
- `Status` column: `Not Created` → update to `Created` + ticket ID once done

---

## Story ID → Ticket mapping table

Once tickets are created, record the mapping here so the SSOT stays the reference.

| Story ID | Suggested Title | Tool | Ticket ID | Status |
|----------|----------------|------|-----------|--------|
| SHARED-001 | bank-common-error RFC7807 problem model | TBD | — | Not Created |
| SHARED-002 | bank-common-security JWT validation utility | TBD | — | Not Created |
| SHARED-003 | bank-common-audit AuditEvent schema + publisher | TBD | — | Not Created |
| SHARED-004 | bank-common-observability metric names + MDC | TBD | — | Not Created |
| TECH-001 | Scaffold service + ArchUnit | TBD | — | Not Created |
| TECH-002 | Secrets & config wiring; fail-fast | TBD | — | Not Created |
| TECH-003 | Flyway DB migrations | TBD | — | Not Created |
| TECH-004 | OneSbHttpClient | TBD | — | Not Created |
| TECH-005 | Error normalisation mapper | TBD | — | Not Created |
| COMP-001 | Outbound audit hook | TBD | — | Not Created |
| TECH-006 | Job store port + impl | TBD | — | Not Created |
| TECH-007 | Async poller | TBD | — | Not Created |
| NFR-001 | Idempotency filter | TBD | — | Not Created |
| FUNC-001 | Master lookup API | TBD | — | Not Created |
| FUNC-002 | Create Term quote job | TBD | — | Not Created |
| FUNC-003 | Get Term quote result | TBD | — | Not Created |
| FUNC-004 | Get proposal schema | TBD | — | Not Created |
| FUNC-005 | Submit Term proposal (agentId enforced) | TBD | — | Not Created |
| FUNC-006 | Get proposal job result | TBD | — | Not Created |
| FUNC-007 | Create payment session | TBD | — | Not Created |
| FUNC-009 | Application status | TBD | — | Not Created |
| COMP-002 | PII masking in logs | TBD | — | Not Created |
| COMP-003 | Raw payload encryption at rest | TBD | — | Not Created |
| COMP-004 | Agent & distributor attribution | TBD | — | Not Created |
| NFR-002 | Health & readiness probes | TBD | — | Not Created |
| NFR-003 | Metrics & alerts baseline | TBD | — | Not Created |

---

## Tracking tool setup checklist

| # | Item | Owner | Status |
|---|------|-------|--------|
| C5-1 | Tool chosen (Jira / Linear / other) | PO | **PENDING** |
| C5-2 | Project / workspace created | PO | **PENDING** |
| C5-3 | Epic labels created matching E00–E13 | PO | **PENDING** |
| C5-4 | Priority labels: P0 / P1 / P2 | PO | **PENDING** |
| C5-5 | Story type labels: FUNC / NFR / COMP / TECH / SHARED | PO | **PENDING** |
| C5-6 | P0 tickets created (25 stories) — see p0-story-board.md | PO / SM | **PENDING** |
| C5-7 | Story IDs added to ticket description/label for SSOT traceability | PO / SM | **PENDING** |
| C5-8 | Mapping table above updated with real ticket IDs | PO / SM | **PENDING** |
| C5-9 | Board link shared with engineering team | PO | **PENDING** |

---

## Ordering rule (non-negotiable)

Tickets must be ordered and pulled in this sequence (matches PRODUCT-BACKLOG.md):

```
Sprint 1: SHARED-001..004, TECH-001..003
Sprint 2: TECH-004..007, COMP-001..002, NFR-001..003
Sprint 3: FUNC-001..003 (masters + Term quote)
Sprint 4: FUNC-004..006 (Term proposal)
Sprint 5: FUNC-007, FUNC-009, COMP-003..004
Sprint 6: Sandbox E2E Term path hardening
Sprint 7+: P1 Health → Motor
```

Do not create sub-tasks that re-order P0 stories within the sequence without Architect + PO sign-off.

---

## Exit criteria (0.5)

- [ ] Tracking tool selected and shared
- [ ] All 25 P0 story tickets created with correct story ID label
- [ ] Tickets linked 1:1 to story IDs (mapping table above complete)
- [ ] Board visible to all team members
- [ ] Sprint 1 tickets have acceptance criteria copied from PRODUCT-BACKLOG.md
