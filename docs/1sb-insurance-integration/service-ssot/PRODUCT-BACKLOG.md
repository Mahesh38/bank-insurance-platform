# Product Backlog — 1SB Integration Service

**Status:** Approved baseline (PO + Architect)  
**Priority legend:** P0 = go-live Term · P1 = Health/Motor + hardening · P2 = later LOBs / ops polish  
**Story types:** `FUNC` functional · `NFR` non-functional · `COMP` compliance · `TECH` technical enabler · `SHARED` shared JAR

All stories below are for **this service only**.

---

## Epic map

| Epic | Priority | Type mix | Outcome |
|------|----------|----------|---------|
| E00 Foundation & shared libs | P0 | TECH/SHARED/NFR | Service boots, secure, observable |
| E01 1SB connectivity | P0 | TECH/NFR/COMP | Auth, client, error map, audit hook |
| E02 Job & polling infra | P0 | TECH/NFR | Async quote/proposal jobs |
| E03 Master data | P0 | FUNC | Lookup + cache |
| E04 Term quote | P0 | FUNC | Multi/single quote + poll |
| E05 Term proposal | P0 | FUNC | Dynamic schema + submit + poll |
| E06 Payment | P0 | FUNC | Payment URL (+ intimation stub) |
| E07 Status & requirements | P0/P1 | FUNC | Status P0; requirements/docs P1 |
| E08 Audit & compliance controls | P0 | COMP/NFR | Logging, masking, attribution |
| E09 Health LOB | P1 | FUNC | Same APIs, Health handler |
| E10 Motor LOB | P1 | FUNC | Same APIs, Motor handler + lookers |
| E11 Resilience & scale | P1 | NFR | Idempotency Redis, CB, flags |
| E12 Later LOBs | P2 | FUNC | Saving/Annuity/Pension |
| E13 Replaceability proof | P2 | TECH | Fake adapter / routing flag |

---

## Definition of Done (all stories)

- [ ] Unit tests for happy path + validation + mapped upstream error  
- [ ] No PII in logs (automated check or review evidence)  
- [ ] Audit event emitted where story requires  
- [ ] OpenAPI updated for any public API change  
- [ ] ArchUnit boundaries still green  
- [ ] Story AC scenarios pass in CI (WireMock) and/or 1SB sandbox where marked  

---

## P0 backlog (implement in order)

### E00 — Foundation & shared JARs

#### SHARED-001 · bank-common-error
**Type:** SHARED · **Priority:** P0  
**As** a bank platform engineer, **I want** a shared RFC7807/problem error model, **so that** all services return consistent errors.

**AC:**
- Given a validation failure, When API returns error, Then body includes `type`, `title`, `status`, `detail`, `code`, `retryable`, optional `errors[]`.
- Given upstream business error, When mapped, Then `code=UPSTREAM_BUSINESS_ERROR` and `upstreamCode` present.
- Library published to internal artifact repo (or monorepo module) and consumed by this service.

#### SHARED-002 · bank-common-security
**Type:** SHARED · **Priority:** P0  
**AC:** JWT validation utility extracts `actorId`/roles; unauthorized → 401; forbidden → 403. No business logic in lib.

#### SHARED-003 · bank-common-audit
**Type:** SHARED · **Priority:** P0  
**AC:** `AuditEvent` schema fields match architecture §8.3; publisher interface; service can emit JSON events without knowing sink details.

#### SHARED-004 · bank-common-observability
**Type:** SHARED · **Priority:** P0  
**AC:** Standard metric names + MDC keys (`jobId`, `lob`, `actorId`); tracing baggage helpers documented.

#### TECH-001 · Service scaffold
**Type:** TECH · **Priority:** P0  
**AC:** Spring Boot app boots; `/actuator/health` up; packages match architecture §3; ArchUnit skeleton enforces `adapter.onesb` isolation.

#### TECH-002 · Secrets & config
**Type:** TECH/COMP · **Priority:** P0  
**AC:** 1SB key/secret/distributorId loaded from secret manager; not in git; missing secret fails fast at startup.

#### TECH-003 · DB migrations
**Type:** TECH · **Priority:** P0  
**AC:** Flyway creates `integration_job`, `integration_job_offer`, `job_poll_attempt`, `raw_payload`, `audit_event`, `payment_session` per architecture §9.

---

### E01 — 1SB connectivity

#### TECH-004 · OneSbHttpClient
**Type:** TECH/NFR · **Priority:** P0  
**AC:**
- Given valid credentials, When call 1SB sandbox health/master, Then Basic Auth applied.
- Given 401 from 1SB, When received, Then map to `UPSTREAM_AUTH_FAILURE`, **no automatic retry**, metric+alert.
- Timeouts configurable; default connect/read per architecture NFR table.

#### TECH-005 · Error normalisation
**Type:** FUNC/TECH · **Priority:** P0  
**AC:** 1SB `errors[]` → bank `ServiceError`; controllers never return raw 1SB error JSON.

#### COMP-001 · Outbound call audit hook
**Type:** COMP · **Priority:** P0  
**AC:** Every outbound 1SB call emits audit with operation, latency, upstream status, requestHash (masked), outcome.

---

### E02 — Job & polling

#### TECH-006 · Job store port + impl
**Type:** TECH · **Priority:** P0  
**AC:** Create job with bank `jobId` before 1SB call; store `reqId` as external ref; status transitions `PENDING|RUNNING|COMPLETED|PARTIAL|FAILED|TIMEOUT`.

#### TECH-007 · Async poller
**Type:** TECH/NFR · **Priority:** P0  
**AC:**
- Polls quote/proposal until complete or max attempts.
- Backoff configurable; does not block Tomcat request thread (async executor / virtual threads).
- Exhaustion → job `TIMEOUT`, caller sees retryable error.

#### NFR-001 · Idempotency filter
**Type:** NFR · **Priority:** P0  
**AC:** Mutating endpoints require/honour `Idempotency-Key` (or generate+document if optional for GET-only); replay returns first result; no duplicate 1SB submit.

---

### E03 — Master data

#### FUNC-001 · Master lookup API
**Type:** FUNC · **Priority:** P0  
**Story:** As a bank app, I want `POST /v1/master-data/lookup` so I can fill enums without calling 1SB.

**AC:**
- Given `lob=TERM` + entityIds, When called, Then returns normalised enum lists.
- Cache hit within TTL → no 1SB call.
- 1SB down + stale cache → return stale with header/flag; no cache → 503.

---

### E04 — Term quote

#### FUNC-002 · Create Term quote job
**Type:** FUNC · **Priority:** P0  
**`POST /v1/quotes` with `lob=TERM`**

**AC:**
- Given valid Term multi-quote request, When posted, Then job created and 1SB Term quote called via `QuoteService` → `TermQuoteHandler`.
- Given missing required fields, When posted, Then 422, **no** 1SB call.
- Given 1SB pending, When polled internally, Then job completes with offers.
- Given poll timeout, Then `QUOTE_TIMEOUT` retryable.
- Given per-insurer errors with some success, Then job `PARTIAL` with offers + failures.
- Audit `QUOTE_CREATED` / completion events emitted.
- Response never includes raw 1SB `reqId` as primary id (bank `jobId` only; external refs internal).

#### FUNC-003 · Get quote job result
**Type:** FUNC · **Priority:** P0  
**`GET /v1/quotes/{jobId}`**

**AC:** Returns status + offers when complete; 404 unknown; in-progress returns status without fabricating offers.

---

### E05 — Term proposal

#### FUNC-004 · Get proposal schema
**Type:** FUNC · **Priority:** P0  
**`GET /v1/proposals/schema`**

**AC:** Returns dynamic schema for product/manufacturer/version; 410/appropriate error if quote expired (if detectable); upstream 5xx → 502 retryable.

#### FUNC-005 · Submit Term proposal
**Type:** FUNC · **Priority:** P0  
**`POST /v1/proposals`**

**AC:**
- Missing `agentId` → 422 `AGENT_ATTRIBUTION_MISSING`, no 1SB call.
- Missing `consentRef` → WARN audit (P0); still allow submit.
- Success → 201 with bank proposal/job id + normalised status.
- Same Idempotency-Key → original result, no duplicate 1SB submit.
- 1SB business reject → 422 `PROPOSAL_REJECTED` / upstream mapping, audit emitted.

#### FUNC-006 · Get proposal job result
**Type:** FUNC · **Priority:** P0  
**AC:** Poll result includes applicationNo when available; status normalised.

---

### E06 — Payment

#### FUNC-007 · Create payment session / URL
**Type:** FUNC · **Priority:** P0  
**`POST /v1/payments`**

**AC:**
- Given payable application, When called, Then returns HTTPS `paymentUrl` + expiry/ref.
- paymentUrl not written to logs (ref only).
- Non-payable status → 409 `PROPOSAL_NOT_PAYABLE`.
- Audit `PAYMENT_URL_RETRIEVED`.

#### FUNC-008 · Payment intimation
**Type:** FUNC · **Priority:** P1 (stub interface P0 optional)  
**AC:** Accepts bank PG receipt; maps to 1SB intimation; retryable on upstream failure; idempotent.

---

### E07 — Status (P0) / Requirements (P1)

#### FUNC-009 · Application status
**Type:** FUNC · **Priority:** P0  
**`GET /v1/status/{applicationNumber}`** (or POST-equivalent if query body needed)

**AC:** Maps 1SB statuses to bank stage enum; returns manufacturer substatus for RM display; 404 if not found.

#### FUNC-010 · Requirements list
**Type:** FUNC · **Priority:** P1  
**AC:** Returns normalised requirements; drives bank tasking outside this service.

#### FUNC-011 · Document upload/download proxy
**Type:** FUNC · **Priority:** P1  
**AC:** Upload metadata + content to 1SB; download URLs time-limited; URLs not logged.

---

### E08 — Compliance & NFR controls (parallel with P0)

#### COMP-002 · PII masking in logs
**Type:** COMP/NFR · **Priority:** P0  
**AC:** Name, mobile, email, PAN/ID, DOB never appear plaintext in app logs; unit test with sample payloads.

#### COMP-003 · Raw payload encryption at rest
**Type:** COMP · **Priority:** P0  
**AC:** `raw_payload` blobs encrypted (AES-GCM); key from vault; retain_until set (default 7 years policy configurable).

#### COMP-004 · Agent & distributor attribution
**Type:** COMP · **Priority:** P0  
**AC:** distributorId only from config; agentId required on proposal; both present on relevant audit events.

#### NFR-002 · Health & readiness
**Type:** NFR · **Priority:** P0  
**AC:** Liveness/readiness probes; optional 1SB reachability on readiness (configurable); dependency down does not crash process.

#### NFR-003 · Metrics & alerts baseline
**Type:** NFR · **Priority:** P0  
**AC:** Metrics for request count, latency, upstream errors, poll timeouts, auth failures; alert hooks documented.

---

## P1 backlog

#### FUNC-012 · Health quote/proposal/payment/status
**Priority:** P1 · **AC:** All Term functional ACs pass with `lob=HEALTH` and Health handler; unsupported fields validated; `LOB_NOT_SUPPORTED` for disabled LOB.

#### FUNC-013 · Motor quote/proposal/payment/status + lookers
**Priority:** P1 · **AC:** Motor handlers; vehicle looker endpoints or master port methods; New vs Roll-Over validation.

#### NFR-004 · Circuit breaker / bulkhead
**Priority:** P1 · **AC:** 1SB consecutive failures open breaker; callers get 503 with retryable; Term handler unaffected by Motor CB if bulkheaded.

#### NFR-005 · Redis idempotency + multi-instance job ownership
**Priority:** P1 · **AC:** Safe horizontal scale; no duplicate poll workers fighting (ownership column / lock).

#### COMP-005 · consentRef mandatory
**Priority:** P1 · **AC:** Missing consentRef → 422; audit always has consentRef on proposal.

#### FUNC-014 · LOB feature flags
**Priority:** P1 · **AC:** Disable Health/Motor via config without redeploy of code paths (restart OK for MVP flags).

---

## P2 backlog

#### FUNC-015 · Saving / Annuity / Pension handlers  
#### FUNC-016 · OTP / CKYC / penny-drop / SP-data adapters (as required by products)  
#### TECH-008 · Provider routing flag (`ONE_SB` only initially)  
#### TECH-009 · Retention cleanup job for expired raw payloads  
#### TECH-010 · API versioning policy (`/v1` freeze rules)  
#### NFR-006 · Load test vs sandbox/prod-like and tune poll pools  

---

## Story → module mapping (no ambiguity)

| Story area | Package / module |
|------------|------------------|
| Controllers | `api.*` |
| Quote/Proposal/Payment/Status services | `application.*` |
| Term/Health/Motor handlers | `lob.*` |
| 1SB client/mappers/poller | `adapter.onesb.*` |
| Job/audit/payments tables | `adapter.persistence.*` |
| Shared error/security/audit | `bank-common-*` JARs |

---

## Suggested sprint slicing (not calendar)

1. SHARED-001..004 + TECH-001..003  
2. TECH-004..007 + COMP-001..002 + NFR-001..003  
3. FUNC-001..003 (masters + Term quote)  
4. FUNC-004..006 (Term proposal)  
5. FUNC-007 + FUNC-009 + COMP-003..004  
6. Sandbox E2E Term path + harden  
7. P1 Health → Motor → requirements/docs  

Developers should take stories **top-down within P0**; do not start Health before Term quote/proposal path is green in sandbox.
