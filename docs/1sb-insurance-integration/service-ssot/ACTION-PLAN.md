# Action Plan — Progressing the 1SB Integration Service

**Purpose:** Ordered actions so the team can move from docs → working Term path in sandbox → Health/Motor → production readiness, without blockers.  
**Scope:** `1sb-integration-service` only  
**SSOT:** [README.md](./README.md) · [PRODUCT-BACKLOG.md](./PRODUCT-BACKLOG.md) · [architecture](../architecture/1sb-integration-service-architecture.md)

Principles that govern this plan: **SOLID + DRY + KISS** (see architecture §1). Prefer proving Term end-to-end before parallel LOB work.

---

## Phase overview

```text
0. Access & alignment
1. Foundations (shared JARs + scaffold)
2. 1SB connectivity + job/poll infra
3. Term vertical slice (masters → quote → proposal → payment → status)
4. Hardening (compliance, NFR, sandbox E2E)
5. Expand LOBs (Health → Motor)
6. Production readiness
```

Each phase has: **owner focus**, **exit criteria**, **parallelizable work**, and **dependencies**.

---

## Phase 0 — Access & alignment (before code)

| # | Action | Owner | Exit criteria |
|---|--------|-------|---------------|
| 0.1 | Confirm 1SB sandbox URL, API key/secret, `distributorId`, IP whitelist — see **[phase-0/CONFIRM-01-onesb-access.md](./phase-0/CONFIRM-01-onesb-access.md)** | Platform / 1SB RM | Credentials in vault path; sandbox curl works from bank egress |
| 0.2 | Confirm insurers/products for distributor (multi-entry catalog) — see **[phase-0/CONFIRM-02-term-products.md](./phase-0/CONFIRM-02-term-products.md)** · data gaps: [PHASE-0-DATA-AND-GAPS.md](./phase-0/PHASE-0-DATA-AND-GAPS.md) · catalog: [config/catalog/products.example.yaml](../../../config/catalog/products.example.yaml) | Product + 1SB | ≥1 enabled product for chosen first LOB; catalog supports multiple insurers/products |
| 0.3 | Align bank→service auth (JWT claims / mTLS) — see **[phase-0/CONFIRM-03-inbound-auth.md](./phase-0/CONFIRM-03-inbound-auth.md)** · design rules: [phase-0/COUPLING-AND-REPLACEABILITY.md §2.2](./phase-0/COUPLING-AND-REPLACEABILITY.md) | Security + Architect | Auth mode config complete; sample token available; `agentId`/`distributorId` rules confirmed |
| 0.4 | Kickoff: walk SSOT (decisions, backlog order, DRY/KISS) — see **[phase-0/CONFIRM-04-ssot-kickoff.md](./phase-0/CONFIRM-04-ssot-kickoff.md)** | Tech lead | Team sign-off on Case 2 + Term-first; no open design blockers |
| 0.5 | Create tracking board from [PRODUCT-BACKLOG.md](./PRODUCT-BACKLOG.md) P0 stories — see **[phase-0/CONFIRM-05-tracking-board.md](./phase-0/CONFIRM-05-tracking-board.md)** · seed: [phase-0/p0-story-board.md](./phase-0/p0-story-board.md) | PO / Scrum master | Jira/Linear tickets linked 1:1 to story IDs |
| 0.6 | 1SB Pune office visit — close credential/catalog/payment/console gaps face-to-face — see **[phase-0/CONFIRM-06-1sb-pune-visit-agenda.md](./phase-0/CONFIRM-06-1sb-pune-visit-agenda.md)** | PO + Architect | CONFIRM-01/02 flip to CONFIRMED (or every remaining gap has a named 1SB owner + date) |

**Exit:** Team can call 1SB sandbox; board ready; no credential blockers.

---

## Phase 1 — Foundations (DRY shared libs + KISS scaffold)

**Backlog:** SHARED-001…004, TECH-001…003

| # | Action | Notes |
|---|--------|-------|
| 1.1 | Create/publish `bank-common-error`, `security`, `audit`, `observability` modules | DRY across bank services |
| 1.2 | Scaffold Spring Boot service + package layout + ArchUnit | Per architecture §3 |
| 1.3 | Wire secrets + Flyway schema (`integration_job`, audit, raw_payload, …) | Fail fast if secrets missing |
| 1.4 | Actuator health + baseline metrics/MDC | NFR-002 / NFR-003 start |

**Exit criteria:**
- App boots locally/CI with empty 1SB calls mocked
- DB migrates cleanly
- ArchUnit green
- No credentials in git

**Parallel:** OpenAPI stub for `/v1/quotes` etc. (contract first) while libs land.

---

## Phase 2 — Connectivity + async infra

**Backlog:** TECH-004…007, COMP-001…002, NFR-001

| # | Action | Notes |
|---|--------|-------|
| 2.1 | Implement `OneSbHttpClient` (Basic Auth, timeouts, 401 no-retry) | Single HTTP stack (DRY) |
| 2.2 | Error normalisation mapper | Bank problem JSON only |
| 2.3 | Outbound audit hook + PII masking tests | Compliance gate |
| 2.4 | Job store + AsyncPoller | Shared by quote & proposal (DRY) |
| 2.5 | Idempotency-Key filter on mutating APIs | Safe bank retries |

**Exit criteria:**
- Sandbox master lookup or probe call succeeds via client
- Poller unit-tested with WireMock pending→complete
- Masking test proves no PAN/mobile/name in logs

---

## Phase 3 — Term vertical slice (KISS: one LOB fully working)

**Backlog:** FUNC-001…007, FUNC-009 (status), COMP-003…004

Implement **in this order** (each depends on previous):

```text
3.1 Masters (FUNC-001)
3.2 Term quote create + get (FUNC-002, FUNC-003)
3.3 Term proposal schema + submit + get (FUNC-004…006)
3.4 Payment URL (FUNC-007)
3.5 Application status (FUNC-009)
```

| # | Action | Exit criteria |
|---|--------|---------------|
| 3.1 | Master lookup + cache | Bank app can load enums |
| 3.2 | `QuoteService` → `TermQuoteHandler` | Multi-quote returns offers or PARTIAL; timeout mapped |
| 3.3 | Dynamic proposal get/submit | `agentId` enforced; idempotent submit |
| 3.4 | Payment session | HTTPS URL returned; URL not logged |
| 3.5 | Status normalisation | Bank stages mapped from 1SB enums |

**Exit criteria (phase):**  
Happy-path **Term** journey works against **1SB sandbox** with audit events for quote, proposal, payment.

**Do not start Health/Motor until this exit is met** (KISS).

---

## Phase 4 — Hardening & consumer enablement

| # | Action | Owner |
|---|--------|-------|
| 4.1 | Sandbox E2E suite in CI (or gated nightly if sandbox flaky) | QA + Eng |
| 4.2 | Publish OpenAPI to internal portal; sample Postman/Bruno collection | Eng |
| 4.3 | Bank consumer spike: one BFF/app calls quote+proposal against UAT | Bank app team |
| 4.4 | Compliance review of audit schema + log samples | Compliance |
| 4.5 | Runbook: secrets rotation, IP whitelist, incident on 1SB 401/5xx | Ops |
| 4.6 | Performance smoke: p95 quote under nominal concurrent jobs | Eng |

**Exit:** Term path signed off for UAT use by at least one bank caller.

---

## Phase 5 — Expand LOBs (reuse orchestrators — DRY)

**Backlog:** FUNC-012 (Health), FUNC-013 (Motor), FUNC-010…011, FUNC-008, NFR-004…005, COMP-005, FUNC-014

| # | Action | Rule |
|---|--------|------|
| 5.1 | Health handler + tests (same APIs, `lob=HEALTH`) | No changes to `QuoteService` orchestration |
| 5.2 | Motor handler + lookers | Separate schemas; still shared poller/HTTP |
| 5.3 | Requirements + docs + payment intimation | As product needs |
| 5.4 | Redis idempotency / multi-instance job ownership | Before scale-out |
| 5.5 | Circuit breaker; `consentRef` mandatory | P1 compliance tighten |

**Exit:** Health and Motor sandbox paths green; Term regression still green.

---

## Phase 6 — Production readiness

| # | Action |
|---|--------|
| 6.1 | Prod credentials, IP whitelist, distributorId, TLS egress verified |
| 6.2 | Dashboards + alerts (auth failure, poll timeout, upstream 5xx, p95) |
| 6.3 | Retention job for raw payloads; backup/restore checked |
| 6.4 | Go-live checklist signed (security, compliance, product) |
| 6.5 | Hypercare: error budget, 1SB escalation contact, rollback plan |

**Later (P2):** Saving/Annuity/Pension, provider routing flag, API versioning freeze — only after prod Term/Health/Motor stable.

---

## RACI (lightweight)

| Workstream | Responsible | Accountable | Consulted |
|------------|-------------|-------------|-----------|
| Backlog priority | PO | PO | Architect |
| Architecture / Case 2 / DRY-KISS | Architect | Architect | Tech lead |
| Implementation | Devs | Tech lead | Architect |
| 1SB access / product enablement | PO + 1SB RM | PO | Architect |
| Security / secrets / egress | Security / Platform | Security | Architect |
| Compliance audit evidence | Eng + Compliance | Compliance | PO |
| Bank consumer integration | Bank app team | Their TL | This service TL |

---

## Weekly operating rhythm

1. **Pull next P0 story** strictly from backlog order (no skipping Term for Motor).  
2. **Demo** each vertical slice (quote, then proposal, then payment) with sandbox evidence.  
3. **Update SSOT** if 1SB sandbox differs from docs (fixtures + field guide).  
4. **PR checklist** includes DRY/KISS + ArchUnit + no PII in logs.  
5. **Blocker rule:** if waiting on 1SB, park LOB work and advance SHARED/TECH/NFR stories.

---

## Immediate next 10 actions (start tomorrow)

1. Obtain sandbox credentials + whitelist confirmation — work through [phase-0/CONFIRM-01-onesb-access.md](./phase-0/CONFIRM-01-onesb-access.md)  
2. Create vault entries + local `.env.example` without secrets — template at [`config/onesb/.env.example`](../../../config/onesb/.env.example) ✅  
3. Stand up repo module structure (service + common libs)  
4. SHARED-001 error model + TECH-001 scaffold  
5. TECH-002 secrets + TECH-003 Flyway  
6. TECH-004 HTTP client + sandbox probe  
7. TECH-006/007 job + poller with WireMock  
8. FUNC-001 masters  
9. FUNC-002/003 Term quote  
10. Schedule first sandbox Term demo with PO  

---

## Success checkpoints

| Checkpoint | Evidence |
|------------|----------|
| CP1 Foundations | CI green, health up, migrations apply |
| CP2 Connectivity | Sandbox call + audit event sample |
| CP3 Term quote | Offers returned for test customer |
| CP4 Term proposal+pay+status | applicationNo + paymentUrl + status mapped |
| CP5 UAT consumer | Bank app completed one Term path |
| CP6 Health/Motor | Handlers live; Term regression green |
| CP7 Prod | Go-live checklist complete |

When in doubt: **KISS** (finish Term), **DRY** (reuse orchestrator/HTTP/poller), **SSOT** (do not invent APIs outside the backlog).
