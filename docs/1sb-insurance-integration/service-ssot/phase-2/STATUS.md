# Phase 2 — Status

**Branch:** `cursor/phase1-foundations-c259`  
**Kickoff:** [TL-KICKOFF.md](./TL-KICKOFF.md)

| Task | Owner | Status | Commit |
|------|-------|--------|--------|
| P2-A1 COMP-002 PII masking | Dev A | Done | `6f0b7ed` |
| P2-A2 TECH-004 OneSbHttpClient | Dev A | Done | `7e971ff` |
| P2-A3 TECH-005 Error normalisation | Dev A | Done | `3c934f7` |
| P2-A4 COMP-001 Outbound audit | Dev A | Pending | |
| P2-B1 NFR-001 Idempotency filter | Dev B | Done | `596a3e0` |
| P2-B2 TECH-006 Job store | Dev B | Done | `aa73b6f` |
| P2-B3 TECH-007 Async poller | Dev B | In progress | |

## TL reviews

| Iteration | Date | Outcome |
|-----------|------|---------|
| — | — | Kickoff published; awaiting Dev delivery |

## Dev B notes

- **P2-B1:** `IdempotencyPort` + in-memory store; filter on POST/PUT/PATCH `/v1/**`; `MISSING_IDEMPOTENCY_KEY` added to `ErrorCodes`. Redis swap deferred (TD-010).
- **P2-B2:** `JobStatus` → `PENDING|RUNNING|COMPLETED|PARTIAL|FAILED|TIMEOUT`; `failJob(POLL_TIMEOUT)` → `TIMEOUT`; poll-attempt HTTP on persistence (TD-015 partial).
- **P2-B3:** Using `OneSbPollPort` + temporary RestClient adapter until Dev A ships `OneSbHttpClient`.

## Dev A notes

- **P2-A1:** `PiiMasker` utility landed (`6f0b7ed`).
- **P2-A2:** `OneSbHttpClient` + `adapter.onesb.config` (Basic Auth, timeouts, `get`/`post`/`exchange`); WireMock 200/401; HTTP/1.1 for WireMock compatibility.
- **P2-A3:** `OneSbErrorNormaliser` maps 401 / business 4xx (`errors[]`) / 5xx → bank `ServiceException`; wired into client.
