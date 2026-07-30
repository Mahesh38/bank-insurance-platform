# Functional · NFR · Compliance · Shared JARs Map

Quick reference so developers know **what kind of work** each item is and **where the detailed design lives**.

---

## 1. Functional aspects (what the service *does*)

| Capability | Bank API (canonical) | 1SB side | Backlog |
|------------|----------------------|----------|---------|
| Master/enum lookup | `POST /v1/master-data/lookup` | `/v1/master/lookup` | FUNC-001 |
| Create quote | `POST /v1/quotes` | LOB `.../v1/quote` | FUNC-002 |
| Get quote result | `GET /v1/quotes/{jobId}` | quote poll | FUNC-003 |
| Proposal schema | `GET /v1/proposals/schema` | GET proposal form | FUNC-004 |
| Submit proposal | `POST /v1/proposals` | POST proposal | FUNC-005 |
| Proposal result | `GET /v1/proposals/{jobId}` | proposal poll | FUNC-006 |
| Payment URL | `POST /v1/payments` | `/v1/payment/url` | FUNC-007 |
| Payment intimation | `POST /v1/payments/.../intimation` | payment intimation | FUNC-008 |
| Application status | `GET /v1/status/{applicationNumber}` | prostat / LOB status | FUNC-009 |
| Requirements | `GET /v1/requirements/{applicationNumber}` | getReq | FUNC-010 |
| Documents | upload/download APIs | docupload/docdownload | FUNC-011 |
| Health LOB parity | same APIs + `lob=HEALTH` | lifehealth | FUNC-012 |
| Motor LOB parity | same APIs + `lob=MOTOR` | motor + lookers | FUNC-013 |

**Functional design rule:** orchestration in `*Service`, LOB differences only in handlers/mappers.

---

## 2. Non-functional requirements (how well it must work)

| NFR | Target / rule | Detail location |
|-----|---------------|-----------------|
| Availability | Aim ≥ 99.5% monthly (exclude pure 1SB outages if correctly surfaced) | Architecture §7.1 |
| Latency | Quote p95 ≤ 5s (incl. poll); status p95 ≤ 1s | Architecture §7.2 |
| Timeouts | Per-call connect/read; poll max attempts | Architecture §7.3 |
| Retry | No blind retry on proposal submit; poll backoff; 401 no retry | Architecture §7.4 |
| Idempotency | `Idempotency-Key` on mutating APIs | Architecture §7.4, NFR-001 |
| Concurrency | Dedicated polling executor; job ownership | Architecture §7.5 |
| Security | TLS, JWT/mTLS inbound, Basic outbound, vault secrets, IP egress | Architecture §7.6 |
| Retention | Audit/raw payload retention policy (default 7y configurable) | Architecture §7.7 |
| Observability | Metrics, traces, structured logs, alerts | Architecture §7.8 |
| Scalability | Stateless app + Redis/DB; horizontal ready by P1 | Architecture §7.9 |
| Resilience | Circuit breaker / bulkhead by P1 | NFR-004 |

---

## 3. Compliance-related controls (must be true in this service)

| Control | Requirement | Backlog |
|---------|-------------|---------|
| Agent attribution | `agentId` mandatory on proposal | COMP-004, FUNC-005 |
| Distributor integrity | `distributorId` from config/secrets only | COMP-004, TECH-002 |
| Consent trace | `consentRef` logged; mandatory from P1 | COMP-005 |
| PII minimisation | No customer DB; pass-through only | Design session D8 |
| Log masking | No plaintext PII in logs | COMP-002 |
| Audit trail | Append-only events for state-changing + outbound calls | COMP-001, SHARED-003 |
| Raw evidence | Encrypted raw 1SB payloads for dispute/audit | COMP-003 |
| Non-repudiation | Persist bank id ↔ 1SB application/proposal refs | TECH-003/FUNC-005 |
| Secrets | No credentials in git/images | TECH-002 |
| Transport security | HTTPS only to 1SB; validate paymentUrl HTTPS | FUNC-007 |
| Data residency / perimeter | Deploy inside IP-whitelisted egress network | Runbook (ops) |

Regulatory **journey** obligations (suitability, disclosures UI, cooling-off UX) remain with bank apps; this service provides the **evidence trail and enforcement points** at the integration boundary.

---

## 4. Shared JARs (reuse across bank services)

| Artifact | Reuse why | Keep out of JAR |
|----------|-----------|-----------------|
| `bank-common-error` | Consistent problem details across estate | LOB business codes specific only to insurance can extend |
| `bank-common-security` | Same JWT/principal extraction | 1SB Basic Auth client |
| `bank-common-audit` | Same audit schema/publisher SPI | 1SB operation enum values can be service-specific extensions |
| `bank-common-idempotency` | Same Idempotency-Key filter/store SPI | Quote polling logic |
| `bank-common-observability` | Metric/MDC conventions | 1SB path maps |

**Service-local (do NOT share prematurely):** LOB handlers, 1SB mappers, poller, proposal schema cache, motor lookers, job domain tables.

Decision matrix detail: Architecture §4.

---

## 5. Design principles checklist (PR review)

- [ ] **DRY:** no duplicated poll/HTTP/auth/error logic per LOB (reuse services + shared JARs)  
- [ ] **DRY:** LOB payload differences stay in separate handlers (no forced mega-DTO)  
- [ ] **KISS:** change stays inside integration scope (no CIF/UI/journey creep)  
- [ ] **KISS:** simplest working path (Term proven before parallel LOB complexity)  
- [ ] Case 2: Service orchestrates → handler maps  
- [ ] No 1SB types outside `adapter.onesb.*`  
- [ ] Public DTO uses bank names  
- [ ] New LOB = new handler + registry entry only  
- [ ] Mutating API idempotent  
- [ ] Audit + masking for outbound calls  
- [ ] Tests include validation-without-upstream and mapped upstream error  

---

## 6. Single source of truth links

- Decisions: [00-po-architect-design-session.md](./00-po-architect-design-session.md)  
- Backlog: [PRODUCT-BACKLOG.md](./PRODUCT-BACKLOG.md)  
- Deep design: [../architecture/1sb-integration-service-architecture.md](../architecture/1sb-integration-service-architecture.md)
