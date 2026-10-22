# Readiness review — can we start building every service? (2026-10-21)

[← API reference home](./README.md)

**Status:** AI-DRAFTED review of branch `claude/api-reference-docs` (commits `fff6d4e` → `118c6bc`)
plus the design pack it depends on. Six parallel reviews (identity & access · lead & customer ·
journey & decisioning · quote-to-policy · integration & events · cross-cutting) scored every
service on the same ten dimensions; the serious findings below were re-checked by hand. Freshness:
`FreshnessCheck` = **WARN** (`state_as_of` 21 days old; `04-STAGE_GATES.md` 28 d vs a 14 d limit).

> **Update 2026-10-22 (`SUG-20261021-rul`).** Decisions D1–D9 are taken
> ([decision record](../architecture-review/DECISIONS-2026-10-21.md); D7 audit is *Proposed*); every
> service has a checked-in OpenAPI + LLD ([contracts](../contracts/rule-engine/LLD.md) folder); the
> configuration-driven rule engine, the events library and the review's code fixes are built and
> green. **Still blocking S11:** the human Compliance signature on GAP-006/007
> ([sign-off packet](../../au-bank-insurance-platform/rule-packs/SIGNOFF-PACKET-GAP-006-007.md)),
> GATE-S08 approval and S09 infrastructure. The scorecard below is the 2026-10-21 baseline.

## 1. Verdict

**No — we are not 100 % ready to start developing all services, flows and business logic.**
We *are* ready to continue the services that already run (identity, BFF, lead, 1SB adapter,
persistence) and to build the quote/proposal persistence further, Term first.

Three things stand between "today" and "build everything":

| # | Kind | What | Who can clear it |
|---|---|---|---|
| G | **Governance (hard gate)** | S11 entry needs GATE-S05/S06/S08/S09/S10 passed **and GAP-006 / GAP-007 closed** — "Non-waivable by any authority including Product" ([DEC-20260816-06](../../governance/registers/DECISION-REGISTER.md)). GATE-S08 is a candidate with no approvals; S09 (IaC: Aurora, DynamoDB, MSK, Valkey, S3) has not started ([S11 stage](../../application-lifecycle-bible/stages/S11-vertical-slice.md)) | Shailja (E2 signature on the consent + suitability rule packs), Mahesh (GATE-S08), Shivanshi (S09) |
| D | **Design decisions not yet taken** | Rule-engine evaluation model, exception-engine owner, customer identity, event specification, audit store, post-payment proposal lifecycle, suitability outcome vocabulary (§5) | Mahesh with Rajal / Aarti / Shailja / Deepali |
| C | **Contracts not ratified** | 14 of 21 services have **no checked-in OpenAPI or LLD** — only PROPOSED shapes in this portal. Every HLD/LLD says "AI-DRAFTED, human signature outstanding" | Owners per service (§3) |

What *is* in good shape: the bounded contexts and one-writer-per-store rule hold in the DDL (no
cross-schema FKs); the sync seam catalogue (S-01…S-26) is complete with budgets; fail-closed PDP,
idempotency and outbox patterns are designed and partly built; the Postgres-vs-DynamoDB split is
decided (§4).

## 2. Scorecard — 21 services × 10 dimensions

`R` ready · `P` partial · `M` missing · `C` conflict between sources · `–` not applicable.
Dimensions: **1** flow & sequence · **2** API contract · **3** validation logic · **4** rule engine /
business rules · **5** DB design (design DDL vs code) · **6** DTOs & DTO validation · **7** DTO →
entity mapping & invariants · **8** architecture fit · **9** store choice · **10** could a developer
build it from the docs without inventing behaviour?

| Service | Code | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| [workforce-access-bff](./services/workforce-access-bff.md) | implemented | R | C | R | P | – | R | P | P | R | P |
| [identity-provider-adapter](./services/identity-provider-adapter-service.md) | implemented | R | P | R | R | – | R | R | P | R | P |
| [identity-authorization](./services/identity-authorization-service.md) | implemented | C | P | R | R | P | R | P | C | P | P |
| [customer-service](./services/customer-service.md) | partial | P | M | P | P | M | P | M | C | P | M |
| [lead-service](./services/lead-service.md) | partial | P | C | P | **M** | C | P | P | P | R | P |
| [consent-service](./services/consent-service.md) | skeleton | C | P | P | P | C | C | P | P | R | **M** |
| [suitability-service](./services/suitability-service.md) | skeleton | P | P | P | **C** | C | P | C | P | R | P (Term only) |
| [journey-orchestration](./services/journey-orchestration-service.md) | skeleton | P | P | P | – | C | C | P | R | R | P |
| [product-catalogue](./services/product-catalogue-service.md) | skeleton | P | P | M | C | P | M | M | P | R | P |
| [administration-config](./services/administration-config-service.md) | skeleton | P | P | M | **P** | C | M | M | P | R | P |
| [quotation-service](./services/quotation-service.md) | persistence built | R | P | P | C | C | P | C | P | R | P |
| [proposal-service](./services/proposal-service.md) | persistence built | R | P | P | C | C | P | C | P | R | P |
| [payment-service](./services/payment-service.md) | skeleton | R | P | R | P | R | P | P | P | R | P |
| [policy-issuance](./services/policy-issuance-service.md) | skeleton | P | P | P | P | **M** | P | M | M | R | P |
| [integration-hub](./services/integration-hub-service.md) | skeleton | P | P | P | C | – | P | P | C | R | P |
| [1sb-integration](./services/1sb-integration-service.md) | implemented | R | C | R | P | R | R | R | P | R | **R** |
| [direct-insurer-adapter](./services/direct-insurer-adapter-service.md) | deferred | M | M | M | M | – | M | M | P | R | out of R0 |
| [bank-persistence](./services/bank-persistence-service.md) | implemented | R | R | P | R | C | P | C | P | R | R |
| [audit-compliance](./services/audit-compliance-service.md) | skeleton | P | P | P | P | C | M | M | M | **C** | **M** |
| [notification](./services/notification-service.md) | skeleton | P | P | P | P | R | P | M | P | R | P |
| [reporting-mis](./services/reporting-mis-service.md) | skeleton | M | P | M | M | M | M | M | P | P | **M** |

Reading it: **flow (1) and store choice (9) are the strongest columns**. **Rules (4), DB (5) and
mapping (7) are the weakest**: the docs say *what* happens but often not the exact data shape and
rule content a developer needs. Only the 1SB adapter and bank-persistence are fully buildable from
docs today.

## 3. Dimension by dimension

### 3.1 Flow and sequences
Every R0 scenario 01–15 exists in this portal; 01–13 have sequence diagrams. **No diagram** for:
proposal materialisation and the post-payment proposal lifecycle; quote expiry (who sets `EXPIRED`
and frees the open-quote marker); TTL restore when a payment ends without `RECONCILED`; the repair
sweep; the read path switching from DynamoDB to Aurora; outbox publisher → MSK → consumer → DLQ;
audit ingest → S3 WORM → `SOLD` completeness; Hub dispatch/poll with routing; consent withdrawal →
journey halt; suitability caution acknowledgement, replacement disclosure, PDF, reuse; lead →
exception engine and AUBIMA hold release; customer resolve/lookup/snapshot; configuration
seed/publish/cache invalidation; partner suspend → session revocation; reporting ingest.
**Ordering conflict:** consent (`CNS-SOL`) must exist before the suitability questionnaire
(consent rule pack §2, `SUIT-R21`) but the journey stage machine orders
`SUITABILITY_COMPLETE → CONSENT_CAPTURED` ([journey §2](./services/journey-orchestration-service.md)).

### 3.2 API contracts
- Checked-in machine-readable contracts exist only for the BFF lead phase
  (`ws3-platform/nip-bff-lead-phase.openapi.yaml`) and lead-service
  (`ws3-platform/lead-service-internal.openapi.yaml`). **Both disagree with code**: BFF OpenAPI has
  no `/auth/**` paths, says cookie `NIPSESSION` / header `X-CSRF-Token` (code: `WORKFORCE_SESSION` /
  `X-XSRF-TOKEN`); lead OpenAPI says `/{leadId}:start-onboarding`, `422 VALIDATION_BLOCKED`,
  `409 LEAD_DUPLICATE_UNFINISHED` with `existingLeadId`, a close body — code differs on each.
- 1SB adapter, identity, customer, bank-persistence: prose only (springdoc at runtime).
- Journey, consent, suitability, catalogue, config, quotation, proposal, payment, policy, hub,
  audit, notification, reporting: **PROPOSED shapes only** in this portal.
- PDP path: HLD `POST /internal/v1/authorize` vs code `/internal/v1/authorization/decisions`.

### 3.3 Validation logic and DTOs
Implemented services carry bean validation on request records (BFF, identity, lead, 1SB). Gaps:
customer formats (CIF, PAN, mobile) are "contracted at the BFF, not coded"; lead close
`reasonCode`/remarks (VAL-017…019) not coded; suitability answers are untyped (`SQ-06` enum vs band);
consent DTO vocabularies disagree three ways (`purposes`, `channel`); journey `channel: ASSISTED`
vs DDL `RM_ASSISTED`; `questionnaire_version VARCHAR(20)` cannot hold
`LIFE-TERM-PROTECTION-v1.0` (25 chars). Validation error codes in rule packs
(`SUITABILITY_INCOMPLETE` and six others) are missing from the [error model](./conventions/error-model.md).

### 3.4 Rule engine — the largest gap
- **Authoring and versioning: designed** — effective-dated, INSERT-only configuration,
  `config_version_ref` on records (`INV-CFG-01..03`, `schemas/13-administration.sql`).
- **Evaluation: designed nowhere.** No ADR says how a rule is expressed (decision table, DMN,
  expression language, code), how precedence and reason codes are computed, or whether
  `SUIT-ALGO-LIFE-v1.0` lives in code or configuration. `configuration_record` has no
  `product_class` (or generic key) column, although resolution is keyed by `(lob, productClass)`.
- **Content sign-off: missing.** Consent and suitability packs are "CONTENT-COMPLETE,
  RATIFICATION-PENDING"; no exception, eligibility, product or validation rule pack exists.
- **Suitability:** Term is implementable (SQ-01–04, 08–12; SUIT-R09–R12). Savings/ULIP conflict
  between the arithmetic model (rule pack §3–4) and the BRD's insurer-wise grid (`OPEN-SUIT-06`);
  ULIP does not ask SQ-02/03/04 yet the outcome record makes `protectionGap` mandatory.
  Outcome vocabulary conflicts: rule pack `SUITABLE | SUITABLE_WITH_CAUTION | NOT_SUITABLE |
  INSUFFICIENT_DATA` per class vs DDL `ck_suit_outcome IN ('ELIGIBLE','NOT_ELIGIBLE')` and
  `INV-QUO-01 outcome = ELIGIBLE`.
- **Lead exception rules:** `LeadApplicationService.evaluate` is a stub; `EH-BR-RULE-001…011` have
  no thresholds; no service owns the engine; the BRD's "missing configuration → Block" is the
  opposite of the stub's default `PASS`.
- **Hub routing:** `HUB_ROUTING` is named, with no value schema or seed.
- **Product eligibility:** two sources (`catalogue.eligibility_band` vs 1SB
  `ELIGIBILITY_CRITERIA_GET`), precedence undefined.

### 3.5 DB design
- Design DDL vs Flyway: identity, bank-persistence (tables), quotation, proposal **match**.
  **lead_lms drifts materially** (V2/V3 omit `source`, `created_by_actor_type`,
  `lead_generator_principal_id`, `config_versions`, `expires_at`, `archived_at`,
  `accountable_sp_cert_ref`, `exception_ref`, partial unique `ux_lead_dedupe_open`; add
  `assigned_rm_id`, `exception_hold`, `exception_outcome`; `customer_id VARCHAR(64)` vs `CHAR(26)`).
  Without `expires_at` the `EXPIRED` state is unreachable.
- Skeleton services (consent, customer, suitability, catalogue, payment, policy, admin) migrate only
  a placeholder table.
- **Not insertable / incomplete as designed:** suitability (no column for per-class outcomes,
  reason codes, algorithm version, `valid_until`, PDF ref, usage); consent (`otp_txn_id`,
  `captured_at NOT NULL` at `REQUESTED`; not append-only; no outbox); policy (no outbox, no state
  history, **no `app_policy` grants**); grants also missing for `app_customer`, `app_catalogue`;
  journey `party_snapshot_ref NOT NULL` vs "journey created and holds at `INITIATED`".
- Retention column `retain_until` exists only in bank_persistence, lead and journey.
- Audit store: HLD "#16 Audit → DynamoDB + S3 WORM" vs DDL/portal `bank_persistence.audit_event`.
  bank-persistence V3 lacks the design's UNIQUE `(journey, sequence)`, `ck_audit_capacity` and
  immutability triggers.
- Every service tests on H2 (PostgreSQL mode); `BYTEA`/`JSONB`/partial indexes/procedures are not
  proven until Testcontainers PostgreSQL replaces H2 for migration tests.

### 3.6 DTO → entity mapping and invariants
- Lead: `Lead.assign` has **no state guard** — a `DISQUALIFIED` lead can be set to `ASSIGNED`
  (breaks `BR-CLOSE-001`); `applyExceptionOutcome` **overwrites a hold** when onboarding is
  re-run, so a real engine returning `PASS` would clear an `APPROVAL_REQUIRED` hold without AUBIMA
  (BRD §4.2). Both are code defects on this branch — fix first.
- bank-persistence: `AuditEventController` calls `save()` on an entity with an assigned id and no
  `Persistable`, so a replayed `eventId` becomes an **UPDATE** of an evidence row (H2) or a
  permission error (prod, UPDATE revoked) instead of "duplicate = success"; V3 audit columns
  (`acting_capacity`, `sequence_no`, `prior_state`) are not in the DTO, so `INV-ACT-04` cannot be
  written.
- Envelope → structured columns (quote/proposal) is **not fully specified**: `AVAILABLE` (API) is not
  a legal `offer_state`; `INVALID` offers violate `ck_offer_positive`; `frequency`/`ppt` vs
  `premium_frequency`/`premium_paying_term`; `issuanceMode` has no column; proposal
  `config_version_ref` is not a control attribute; `form_values_ref` has no durable target after the
  frozen DynamoDB item expires ([schemas/20 §6](../data-architecture/schemas/20-dynamodb-working-payload.md)).
- Customer: no domain model; CBS fields pass through; no HMAC/encryption.

### 3.7 Architecture — used at its best?
**Mostly well chosen, unevenly applied.**

| Area | Assessment |
|---|---|
| Bounded contexts, schema-per-service, no cross-schema FK | Good in design. Audit does not own its store. |
| Hexagonal + ArchUnit | 17/21 services have the test; **BFF, identity-authorization, identity-provider-adapter, bank-persistence have none**. Rules are two, copy-pasted, `allowEmptyShould(true)`, no cross-context isolation rule. |
| Shared libs | Under-used: `bank-common-security/-secrets/-domain` used by 1SB only; `bank-common-test` by two services. |
| Outbox / events | **Two patterns** (Postgres `outbox_event`; DynamoDB-stream items) and **no publisher, no topic list, no envelope schema, no DLQ spec, no Kafka client anywhere**. Consent, policy, catalogue have no outbox although they emit events. |
| Fail-closed security | PDP built and called by BFF and lead. **identity-authorization has no caller authentication**: any in-cluster caller can resolve, create or approve partners with a free-text `X-Actor-Id`. No prod-profile guard against the zero key, `memory` session store, `stub` downstreams or H2. |
| Resilience | Budgets tabulated; code has no resilience4j, one hand-rolled breaker (1SB), customer-service read timeout 30 s vs S-05 3 s. Keycloak and provisioning clients have no timeout. |
| Multi-instance | No ShedLock / `SKIP LOCKED` / leader election; several in-memory stores; identity outbox dispatcher can double-provision. |
| Config versioning | Designed; only quotation/proposal carry it in code. |

## 4. Postgres vs DynamoDB — yes, it is decided

[`DB-DEC-0005`](../data-architecture/DB-DEC-0005-dynamodb-working-payload.md) (owner-ratified): only
**pre-payment quote and proposal working state** goes to DynamoDB; everything else is relational.

| Data | Store | Why |
|---|---|---|
| Working quote (request, members, all offers, selection), its idempotency / outbox / open-quote marker | DynamoDB `{env}-quotation-working` | Single-key, high-churn, mostly abandoned; TTL ageing |
| Working proposal (form, nominee, UW requirements), app-number / open-proposal markers | DynamoDB `{env}-proposal-working` | Same |
| Structured `quote` + `offer`, `proposal` + `uw_requirement` + `uw_document_ref` | Aurora (INSERT-only) after `RECONCILED` | The sold record; FK/CHECK |
| Journey, consent, suitability, lead, customer, catalogue, config, identity, payment, policy | Aurora, schema per service | Gates, money, evidence |
| Adapter jobs, raw provider payloads, audit events | Aurora `bank_persistence` (audit: see conflict) | Evidence, 7 years |
| UW documents, policy PDFs | S3 (Object Lock for policy) | Size, WORM |
| Sessions, catalogue L2 cache | Valkey | ADR-011 |

**What is not finished:**
1. **Stale documents still say the opposite**:
   - [`R0-LLD.md`](../../architecture/R0-LLD.md) l.95, `R0-HLD.md`, and `03-solution-architecture-r0.md` put journey, audit and sessions in DynamoDB.
   - `DB-DEC-0001` and `DB-DEC-0003` have no supersession banner.
   - `05-e2e-er-and-field-map.md` (l.11, l.1056 and the `quote_working` ER), `04` roadmap text, `DATA-004`/`DATA-005`/`PLAN-012`, and this portal's README l.247 also still say the opposite.
2. **The materialisation contract has holes:**
   - `PaymentReconciled` carries only `paymentId, settlementRef` ([events](./interactions/outbox-and-events.md) l.65), and `payment.payment` has no `quote_id`, so Quotation cannot find its item.
   - The quote turns `CONVERTED` when the proposal is created ([quotation §2](./services/quotation-service.md)), but Payment's `/selected` requires `SELECTED`, so payment would always fail.
   - Post-payment proposal states (`CONVERTED`, `ISSUANCE_FAILED`, `REFUND_REQUIRED`) have no writable store: the DynamoDB item is frozen and Aurora is INSERT-only.
   - TTL restore is undefined for `REJECTED`, `UNCERTAIN` and `RECONCILIATION_BREAK`.
   - There is no Payment list API for the repair sweep.
   - DynamoDB Streams keep records for 24 h, so a longer publisher outage loses events.

## 5. Decisions needed before "build everything" (owner list)

| # | Decision | Recommended | Owner |
|---|---|---|---|
| D1 | Rule-evaluation model (format, code vs config, precedence, reason codes, test fixtures) | ADR: versioned decision tables in `administration` with JSON-Schema per domain; algorithm version in code; reason codes persisted | Mahesh + Rajal |
| D2 | Exception engine owner and port contract | `ExceptionEvaluationPort {customerId, productClass, branch} → {outcome, ruleIds}`; unavailable / no config = BLOCK; thresholds signed by BRD owner | Mahesh + Exception BRD owner |
| D3 | Customer identity (`customerId` = local ULID or CBS id) | Local ULID; CIF only in `customer` (hashed lookup) | Mahesh + Aarti |
| D4 | Suitability outcome record and Savings/ULIP model (`OPEN-SUIT-06`) | Rule pack per-class record canonical; rewrite DDL-06 and `INV-QUO-01`; build Term first | Rajal + Shailja |
| D5 | Consent before suitability; consent append-only with outbox | Re-sequence journey: CNS-DP/SOL before need analysis, CNS-SHR before proposal submit | Rajal + Shailja |
| D6 | Event specification (envelope, schemas, topics/keys, DLQ, one publisher library for both outbox patterns, UUID vs ULID) | One spec + shared relay lib with `SKIP LOCKED` | Mahesh + Shivanshi |
| D7 | Audit store and transport | `bank_persistence.audit_event` + S3 WORM; durable HTTP/outbox write from adapters; amend HLD | Mahesh + Shailja |
| D8 | Quote `CONVERTED` timing; `quoteId`/`proposalId` on `PaymentReconciled`; post-payment proposal mutability; `form_values_ref` target | Quote stays `SELECTED` until materialise; add ids + `payment.quote_id`; `proposal_state_history`; S3 evidence object for form values | Mahesh + Aarti |
| D9 | Contract authority | One checked-in OpenAPI per service, CI diff vs springdoc; OpenAPI wins on the wire | Mahesh |

## 6. Readiness plan

| Wave | Can start | Condition |
|---|---|---|
| **Now** (fixes, no new decisions) | Lead `assign`/onboarding guards; audit insert-only + V4 constraints; PDP caller auth; prod-profile guards; customer timeouts and 503 mapping; ArchUnit in the four missing services; lead DDL reconciliation; stale-doc banners; BFF/lead OpenAPI aligned to code | None |
| **After D3, D6, D7, D9** | Journey skeleton (stage machine, refs, outbox), customer resolve/snapshot, Hub contract and routing schema, event relay, audit-compliance | Decisions recorded |
| **After D1, D4, D5 + GAP-006/007 E2 signatures** | Consent, suitability (Term first), administration-config evaluator | Shailja's signature (non-waivable) |
| **After D8 + S10 PG contract** | Quotation/proposal command APIs on the built persistence, payment, policy issuance | S09 infra for real DynamoDB/Aurora/MSK |
| Later | Reporting-MIS (read model undecided), direct-insurer adapter (deferred) | — |

## 7. Traceability and document status
`docs/journey-execution/06-TRACEABILITY.md` traces login/session only (27 of 30 rows have no
test; the requirement column is empty). There is no BRD → API → table → test chain for the R0
sale. DOC-MAP labels the 31 `ws3-platform/` documents `approved-spec` while each says
"AI-DRAFTED" in its own status line — readers will over-trust them. Rule packs:
content-complete, ratification pending. DBA pack: AI-drafted; only `DB-DEC-0005` has a human
ratification.
