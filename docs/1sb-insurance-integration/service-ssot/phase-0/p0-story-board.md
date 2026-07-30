# P0 Story Board — Importable Backlog Seed

**Source:** [PRODUCT-BACKLOG.md](../PRODUCT-BACKLOG.md)  
**Purpose:** Seed list for creating Jira / Linear tickets, one ticket per row.  
**Status column:** Update to `Created` and add the ticket ID once the ticket exists.

> Import instructions:
> - Jira CSV import: use Story ID as `Custom Field (External ID)`, Suggested Title as `Summary`, Epic as `Epic Link`.
> - Linear: create as issues under the project; paste Story ID in the description or as an identifier label.
> - Each `[ ]` checkbox below represents one ticket to create.

---

## Epic E00 — Foundation & Shared JARs

| Story ID | Suggested Ticket Title | Priority | Status |
|----------|----------------------|----------|--------|
| SHARED-001 | Implement bank-common-error RFC7807 problem model | P0 | Not Created |
| SHARED-002 | Implement bank-common-security JWT validation utility | P0 | Not Created |
| SHARED-003 | Implement bank-common-audit AuditEvent schema + publisher | P0 | Not Created |
| SHARED-004 | Implement bank-common-observability metric names + MDC keys | P0 | Not Created |
| TECH-001 | Scaffold 1sb-integration-service Spring Boot app + ArchUnit | P0 | Not Created |
| TECH-002 | Wire secrets & config (vault/SM); fail-fast on missing secret | P0 | Not Created |
| TECH-003 | Flyway DB migrations: all P0 tables | P0 | Not Created |

### Checklist

- [ ] SHARED-001 — bank-common-error RFC7807 problem model
- [ ] SHARED-002 — bank-common-security JWT validation utility
- [ ] SHARED-003 — bank-common-audit AuditEvent schema + publisher
- [ ] SHARED-004 — bank-common-observability metric names + MDC keys
- [ ] TECH-001 — Service scaffold + ArchUnit skeleton
- [ ] TECH-002 — Secrets & config wiring; fail-fast guard
- [ ] TECH-003 — Flyway migrations (integration_job, audit_event, …)

---

## Epic E01 — 1SB Connectivity

| Story ID | Suggested Ticket Title | Priority | Status |
|----------|----------------------|----------|--------|
| TECH-004 | Implement OneSbHttpClient (Basic Auth, timeouts, no-retry on 401) | P0 | Not Created |
| TECH-005 | Implement 1SB error normalisation mapper | P0 | Not Created |
| COMP-001 | Outbound 1SB call audit hook with PII masking | P0 | Not Created |

### Checklist

- [ ] TECH-004 — OneSbHttpClient
- [ ] TECH-005 — Error normalisation mapper
- [ ] COMP-001 — Outbound audit hook

---

## Epic E02 — Job & Polling Infrastructure

| Story ID | Suggested Ticket Title | Priority | Status |
|----------|----------------------|----------|--------|
| TECH-006 | Implement job store port + PostgreSQL impl (status transitions) | P0 | Not Created |
| TECH-007 | Implement async poller with configurable backoff | P0 | Not Created |
| NFR-001 | Idempotency-Key filter on all mutating endpoints | P0 | Not Created |

### Checklist

- [ ] TECH-006 — Job store port + impl
- [ ] TECH-007 — Async poller
- [ ] NFR-001 — Idempotency filter

---

## Epic E03 — Master Data

| Story ID | Suggested Ticket Title | Priority | Status |
|----------|----------------------|----------|--------|
| FUNC-001 | POST /v1/master-data/lookup with in-process cache + stale fallback | P0 | Not Created |

### Checklist

- [ ] FUNC-001 — Master lookup API

---

## Epic E04 — Term Quote

| Story ID | Suggested Ticket Title | Priority | Status |
|----------|----------------------|----------|--------|
| FUNC-002 | POST /v1/quotes (lob=TERM) — create quote job + poll | P0 | Not Created |
| FUNC-003 | GET /v1/quotes/{jobId} — return quote result / status | P0 | Not Created |

### Checklist

- [ ] FUNC-002 — Create Term quote job
- [ ] FUNC-003 — Get Term quote result

---

## Epic E05 — Term Proposal

| Story ID | Suggested Ticket Title | Priority | Status |
|----------|----------------------|----------|--------|
| FUNC-004 | GET /v1/proposals/schema — dynamic proposal form | P0 | Not Created |
| FUNC-005 | POST /v1/proposals — submit Term proposal (agentId enforced) | P0 | Not Created |
| FUNC-006 | GET /v1/proposals/{jobId} — proposal job result | P0 | Not Created |

### Checklist

- [ ] FUNC-004 — Get proposal schema
- [ ] FUNC-005 — Submit Term proposal
- [ ] FUNC-006 — Get proposal job result

---

## Epic E06 — Payment

| Story ID | Suggested Ticket Title | Priority | Status |
|----------|----------------------|----------|--------|
| FUNC-007 | POST /v1/payments — create payment session / return HTTPS URL | P0 | Not Created |

### Checklist

- [ ] FUNC-007 — Create payment session

---

## Epic E07 — Status (P0 portion)

| Story ID | Suggested Ticket Title | Priority | Status |
|----------|----------------------|----------|--------|
| FUNC-009 | GET /v1/status/{applicationNumber} — normalised status mapping | P0 | Not Created |

### Checklist

- [ ] FUNC-009 — Application status

---

## Epic E08 — Compliance & NFR Controls

| Story ID | Suggested Ticket Title | Priority | Status |
|----------|----------------------|----------|--------|
| COMP-002 | PII masking in logs (name/mobile/email/PAN/DOB) + unit tests | P0 | Not Created |
| COMP-003 | Raw payload encryption at rest (AES-GCM, vault key) | P0 | Not Created |
| COMP-004 | Agent & distributor attribution enforcement | P0 | Not Created |
| NFR-002 | Liveness/readiness probes; graceful 1SB dependency handling | P0 | Not Created |
| NFR-003 | Metrics + alert hooks baseline (request, latency, upstream error, poll timeout) | P0 | Not Created |

### Checklist

- [ ] COMP-002 — PII masking in logs
- [ ] COMP-003 — Raw payload encryption at rest
- [ ] COMP-004 — Agent & distributor attribution
- [ ] NFR-002 — Health & readiness probes
- [ ] NFR-003 — Metrics & alerts baseline

---

## Summary counts

| Priority | Total stories | Tickets created | Remaining |
|----------|--------------|-----------------|-----------|
| P0 | 25 | 0 | 25 |
| P1 | 6 | 0 | 6 |
| P2 | 6 | 0 | 6 |

_Update this table as tickets are created._
