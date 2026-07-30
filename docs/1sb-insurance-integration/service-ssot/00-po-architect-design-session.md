# PO ↔ System Architect Design Session

**Participants:** Product Owner, System Architect  
**Topic:** Design of `1sb-integration-service` (integration only)  
**Outcome:** Accepted recommendations below are binding for implementation

---

## 1. Discussion summary

### Product Owner position
- Ship a **thin integration service**, not the bancassurance platform.
- First revenue unlock = **Term Life** quote → proposal → payment URL → status.
- Health and Motor next, without forcing bank apps to re-integrate.
- Compliance-grade **audit + agent attribution** are go-live gates, not “later”.
- Bank apps must never learn 1SB protocol (auth, poll, dynamic forms).

### System Architect position
- One deployable service; **Case 2** orchestration (`Service` → `LobHandler`).
- Bank-canonical API; 1SB shapes only inside `adapter.onesb.*`.
- Shared workflow (job store, poller, errors, idempotency); LOB-specific mappers.
- Extract cross-cutting concerns into **shared JARs** where other bank services will reuse them.
- Enforce boundaries with ArchUnit; raw 1SB payloads encrypted at rest for audit only.

### Agreements (accepted)

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | Scope = integration service only | Avoid platform scope creep |
| D2 | Case 2: `QuoteService` then LOB handler | Shared workflow, clean LOB extension |
| D3 | One bank API per capability + `lob` discriminator | Stable consumer contract |
| D4 | Separate handlers/schemas per LOB | Term ≠ Health ≠ Motor payloads |
| D5 | Async jobs owned by this service | Hide 1SB poll from callers |
| D6 | `agentId` mandatory on proposal submit | Bancassurance attribution |
| D7 | `distributorId` from secrets/config, never from caller | Prevent tenant spoofing |
| D8 | PII never in logs; hash/mask only | Infosec + privacy |
| D9 | Term P0 → Health/Motor P1 → Saving/Annuity/Pension P2 | Value vs complexity |
| D10 | JVM Spring Boot 3.x + Java 21 as default stack | Bank estate fit (adjust only if platform standard differs) |
| D11 | `consentRef` WARN in P0, mandatory from P1 | Unblock Term pilot; tighten later |
| D12 | Idempotency-Key on all mutating APIs | Safe retries from bank apps |
| D13 | Shared JARs for error/security/audit/idempotency/observability | Reuse across bank services |
| D14 | No 1SB field names in public DTOs | Replaceability |

---

## 2. What we are building (PO view)

### Vision
Single reliable, auditable conduit from bank systems to 1SB.

### Goals
1. Term quote + proposal live path  
2. Hide 1SB complexity from bank consumers  
3. Compliance-grade audit trail  
4. Replaceable adapter (1SB today)  
5. Additive LOB delivery  

### Explicit non-goals
CIF, RM UI, suitability engine, full journey UX, payment money movement, claims/renewals, multi-aggregator routing (beyond port readiness).

---

## 3. What we are building (Architect view)

### Components inside one service
- API controllers (Quote, Proposal, Payment, Status, Jobs, Masters)
- Application services (orchestrators)
- LOB handlers (Term/Health/Motor…)
- Ports + 1SB adapter (HTTP client, poller, mappers)
- Job/correlation store, idempotency, audit, secrets, metrics

### Design principles (binding)
See architecture doc §1. Short form:

**SOLID + DRY + KISS**
- **DRY:** one orchestrator/poller/HTTP client/error model; LOB-only code in handlers — do not duplicate infrastructure per LOB; do not merge different LOB payloads into one mega-DTO
- **KISS:** integration service only; Case 2 is enough; Term first; hide 1SB poll behind simple REST+jobId; no journey/CIF/UI here

**Domain rules**
1. Bank-canonical first  
2. Orchestration ≠ LOB mapping  
3. Open for new LOB via handler registration  
4. Idempotent mutating APIs  
5. Partial multi-quote success is first-class  
6. Dynamic forms are data  
7. Secrets from vault  
8. Append-only audit  
9. Normalize errors at adapter edge  
10. ArchUnit: no 1SB types outside adapter  

---

## 4. Default answers to blocker questions

| Question | Default |
|----------|---------|
| Bank→service auth | JWT via API gateway (`actorId` claim); mTLS if bank standard requires |
| Idempotency store | Redis preferred; in-process OK only for single-instance sandbox |
| Job store | PostgreSQL |
| Secrets | Vault / cloud SM → injected at runtime |
| Quote p95 SLA | ≤ 5s including poll (sandbox/nominal) |
| Poll strategy | Service-side poll; caller polls `GET /jobs/{jobId}` or quote GET |
| consentRef | WARN P0; required P1+ |
| LOB flags | Env vars `LOB_TERM_ENABLED`, etc. |
| Master cache | In-process TTL 4h; Redis when multi-instance |
| Stack | Java 21, Spring Boot 3.3+, WebClient, Flyway, Micrometer/OTEL |

---

## 5. Handoff rule for developers

1. Pick the next **P0** story from [PRODUCT-BACKLOG.md](./PRODUCT-BACKLOG.md).  
2. Implement against [architecture SSOT](../architecture/1sb-integration-service-architecture.md).  
3. Meet story **Acceptance Criteria** + epic **Definition of Done**.  
4. Do not invent new public endpoints without updating this SSOT.  
5. If 1SB sandbox differs from docs, capture fixture + update field guide; do not leak raw 1SB into public API.
