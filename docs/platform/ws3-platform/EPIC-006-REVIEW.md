# EPIC-006 — AIGEM seven-board review (PLAN-009)

**Work item:** EPIC-006 · **Origin:** `SUG-20261005-lbf` · **Tier:** T3  
**Stage:** WS-3 S08 Foundation (SF5 lane `amit-nip-bff-lead`)  
**Freshness:** FRESH 2026-10-05 ([BOOT.md](../../context/BOOT.md) §5)  
**Self-review:** the implementing agent also sat the boards (`self_review: true`, [11 §2](../../governance/11-REVIEW_GATES.md#2-who-may-sit-on-a-board)). T3 permits provisional agent verdicts and **requires at least one human board** before the gate may close as `APPROVED`. No T4 human seat is claimed.

T4 triggers ([11 §3](../../governance/11-REVIEW_GATES.md#3-proportionality--which-boards-are-mandatory) RG-5) were walked and **did not fire** as control-behaviour changes:

| Trigger | Finding |
|---|---|
| G1 | Opaque BFF session remains the credential (ADR-015). New Lead paths sit behind that session. Spring Security `permitAll` on `/api/v1/**` is an implementation smell, not a new identity plane. |
| G2 | Public bodies stay masked (`maskedMobile`, `maskedCif`). No PAN/DOB/address on confirm. |
| G3–G4 | No new secret/crypto. Apigee token stays in customer-service. |
| G5–G7 | No money, consent, or Flyway. |
| G8 | Additive paths on the existing BFF; no topology change. |
| G9 | Login contract unchanged; Lead paths are new, not a break of a consumed bank caller. |
| G10 | SP-cert stub is not IRDAI evidence. |

RG-6: remain **T3**. Security may escalate a single board to T4 without a CR; this review does not.

Mandatory at T3: all seven boards. Depth: Security `SECURITY-L1` (feature). Architecture `A2_NOTIFY` (public BFF + session enforcement; T3 human seat still required).

As-built evidence: 62 module tests green; Jacoco lead 81.6% line / customer 75.0% / BFF 58.5% (service floor 50%); `python3 scripts/governance/ci-checks.py` passed locally after triage/DOC-MAP refresh.

---

## Round 3 — implementation re-review against CODE-REVIEW-STANDARD (2026-10-07)

**Standard:** [CODE-REVIEW-STANDARD.md](../../governance/CODE-REVIEW-STANDARD.md) · **Work:** `SUG-20261007-crs`  
**Reviewer:** Amit / Engineering (agent) · **Type:** implementation review, not T4  
**SHA reviewed:** post-should-fix `87eac30` plus this round's OBS wrap (`HolderAccessTokenPort`)

| Aspect | Verdict | Notes |
|---|---|---|
| Coverage on push (§1) | HOLD for measured numbers in this round's evidence log | Floors: libs 80/70; 1SB 90/70; BFF/customer 50% line, branch ungated. Author must publish measured % (PR template). |
| Structure / ENG-1–3 | PASS | Apigee holder in `libs/bank-common-apigee`; CBS in customer adapter; 1SB token stays stub until `DEP-20260914-apg`. |
| SEC-C2 / C3 / C4 / C9 | PASS | CBS translate has no `q`/URI; `findById` encode+URI; BFF `/api/v1/**` `authenticated()`; session handle not in credentials. |
| OBS-1 / OBS-9 (must) | PASS after this round | Token mint `IllegalStateException` is wrapped to `UPSTREAM_UNAVAILABLE` at `HolderAccessTokenPort`. HTTP still uses auto-config `PlatformErrorAdvice` + `ErrorRecorder`. BFF session invalid/expired already `ServiceErrors` → `BffExceptionHandler`. |
| OBS-2 / OBS-4 / OBS-10 | PASS | 401/400 CBS tests assert no `q`; Apigee 401 message asserts no secret/body/URI. |
| OBS-3 / SEC-C8 | PASS for this stage | `SESSION_INVALID` / `SESSION_EXPIRED` / remint `UPSTREAM_UNAVAILABLE` are the auditable events. No second audit topic (ADR-012 store is not this PR). |
| OBS-8 dashboards | N/A (correctly not now) | BOOT.md WS-1: dashboards/SLOs Phase 6. `bank.error.count` is the metric bar. |
| G1/G3 (round 1 text) | Stale vs this SHA | Round 1 said `permitAll` and “Apigee token stays in customer-service” only. This SHA: default-deny filter + live customer `/token`; 1SB `token-mode=http` refused until mapping. |

**Must-fix opened this round:** wrap Apigee mint failure as catalogue `UPSTREAM_UNAVAILABLE` (OBS-1/OBS-9) — **done in the same change**.  
**Should-fix:** none new. Parked remain: `DEP-20260914-apg` credentials/Bearer mapping; `SUG-20261007-ird` IRDAI SoR; `SUG-atk` Valkey cluster cache.  
**Verdict:** `CHANGES_REQUESTED` until Jacoco numbers for this SHA are pasted; then `APPROVE` as implementation review **if** floors hold. Not Deepali T4. Not Board 7 production readiness.

---

## Board 1 — Architecture (Mahesh)

**Question:** Does this belong here, shaped like this?  
**Severity:** `A2` (in-memory Lead + interceptor-not-filter-chain) · **Authority class:** `A2_NOTIFY`

```yaml
# schema: review-verdict
board: ARCHITECTURE
plan: PLAN-009
work_item: EPIC-006
reviewer: "Mahesh — Principal Insurance Platform Architect (agent)"
reviewer_type: AGENT
self_review: true
round: 1
date: "2026-10-05"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "Do not treat in-memory Lead or stub CBS/HR as the system of record; Flyway Lead DDL stays SUG-20260825-db1; live CBS stays S09/S11"
should_fix:
  - "Fold session enforcement into Spring Security default-deny (SEC-C4) so Lead paths cannot go anonymous if the MVC interceptor is unregistered — SUG-20261005-sdn"
evidence:
  - "A1: Lead aggregate in lead-service; BFF is L4 facade; Customer #4 owns Apigee/CBS token — no BFF JDBC, no 1SB types on BFF"
  - "A2: exception evaluation on Start Onboarding / assign orchestration matches D-019; ULID mint in Lead #5 matches D-020"
  - "A3: BFF→Lead/Customer HTTP ports are directional; stub adapters are ConditionalOnProperty; application does not import adapter packages (ArchUnit)"
  - "A4: standing constraints held — bank apps never call 1SB or DB; no PII log statements in new types; Flutter still never receives OAuth"
  - "A5: no new ADR; ADR-015/017/020 reused; D-020/D-021 are Product working decisions, not architecture ADRs"
  - "A6: no new broker, cache, or database"
  - "A7: in-memory store is discarded on restart — replacement cost is the parked physical pack, not a migration of stub data"
  - "A8: SF5 scaffold at S08, not S11 live CBS; quote/proposal/payment correctly absent"
  - "A9: one BFF module + two existing skeletons; no eighth service"
  - "A10: replace interceptors/stubs without changing public resource names"
  - "RG-5 G1/G8 considered; T4 Architecture signature not manufactured"
notes: >
  Authority class A2_NOTIFY because public BFF attack-surface grew. Human Board 1
  may countersign; this agent verdict is not that signature.
```

---

## Board 2 — Technical (Amit)

**Question:** Will this work, and can we live with it?

```yaml
# schema: review-verdict
board: TECHNICAL
plan: PLAN-009
work_item: EPIC-006
reviewer: "Amit — Technical Head (agent)"
reviewer_type: AGENT
self_review: true
round: 1
date: "2026-10-05"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "PLAN-009 files_expected must name as-built types: BffSessionInterceptor, CustomerLookupController, LeadClockConfig, gateways, and the three test classes that prove AC-1..AC-6"
should_fix:
  - "StubLeadGateway mints its own Crockford buffer instead of sharing lead-service Ulid — SUG-20261005-uld"
evidence:
  - "T1: Java 21 / Spring Boot; LeadApplicationService single constructor + Clock bean (contextLoads green after NoSuchMethodException)"
  - "T2: RFC 7807 via ServiceErrors; missing session SESSION_INVALID; unknown id RESOURCE_NOT_FOUND; UNCERTIFIED SP_CERTIFICATION_REQUIRED; hold CONFLICT on assign"
  - "T3: ApigeeAccessTokenHolder lock + 30s skew; InMemoryLeadRepository ConcurrentHashMap; 16-thread token test"
  - "T4: login paths unchanged; Lead paths additive"
  - "T5: catalogues are in-process fixtures; no second framework"
  - "T6: BFF assign orchestrates evaluate-then-assign (LeadFacade) rather than duplicating exception rules"
  - "T7: files_expected under-lists as-built controllers — condition above"
  - "T8: revert is sufficient; in-memory holds no durable customer data"
notes: >
  Coverage floors held on the three modules. ENG-4 OpenAPI was updated in the same
  change. No TODO without a work-item id was added.
```

---

## Board 3 — Product (Rajal)

**Question:** Is this the thing we asked for — and only that?

```yaml
# schema: review-verdict
board: PRODUCT
plan: PLAN-009
work_item: EPIC-006
reviewer: "Rajal — Product Owner (agent)"
reviewer_type: AGENT
self_review: true
round: 1
date: "2026-10-05"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "Human Product owner countersigns D-020 (ULID identity) and D-021 (Lead-owned dashboard stages; vertical optional; India +91 mobile rule) — agent recorded them as working decisions"
should_fix: []
evidence:
  - "P1: Screens 1/5/7 country-code, org lookup, assign+exceptionRequired, customer search are present on /api/v1"
  - "P2: REST names are plural collections (country-codes, branches, verticals, relationship-managers) matching ADR-017 / SUG-20260907-std, not /country-code or /api/branch"
  - "P3: quote/proposal/payment/campaign/meeting SMS not bundled"
  - "P4: EPIC-006 AC-1..AC-6 are observable (regex, nested collections, exceptionRequired, 26-char ULID, single-flight token, active-leads)"
  - "P5: intended RM experience — NIP-APP can drive search→create→assign against the BFF"
  - "P6: S11 live CBS and PDP named not_included"
  - "P7: PLAN-009 out_of_scope matches the as-built stubs"
  - "P8: actor is workforce session; LOB LIFE; productClass TERM/SAVINGS/ULIP per CR-015"
  - "P9: BFF speaks bank names; no 1SB wire codes on public bodies"
  - "P10: display-label RR-2024-001 parked (OPEN-LEAD-DISPLAY)"
  - "P11: HOLD customer returns exceptionRequired without assign; BLOCK fails onboarding"
  - "P12: evidence path is MockMvc + unit tests, not a live assisted sale KPI"
notes: >
  Combining Start Onboarding evaluation into POST /leads/{id}/assignment matches
  the stakeholder Assign API. Create still does not evaluate (D-019).
```

---

## Board 4 — Security (Deepali)

**Question:** What does this expose, what can be abused, and are the required controls/evidence sufficient?  
**Depth:** SECURITY-L1 · **Severity:** `S2` (permitAll + interceptor; org catalogue unscoped)

```yaml
# schema: review-verdict
board: SECURITY
plan: PLAN-009
work_item: EPIC-006
reviewer: "Deepali — Security Architect (agent)"
reviewer_type: AGENT
self_review: true
round: 1
date: "2026-10-05"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "Human Board 4 must sit before any promotion of Lead /api/v1 paths beyond stub/local; this AGENT verdict does not satisfy T3's required human board (11 §2) nor a T4 signature"
  - "Unauthenticated Lead/reference calls must remain 401; LeadModuleApiTest.unauthenticatedLeadApisAreRejected is the fail-closed bar"
  - "Do not set CUSTOMER_DOWNSTREAM_MODE=http or LEAD_DOWNSTREAM_MODE=http against live CBS until PDP object-level grants exist (EPIC-006 not_included)"
should_fix:
  - "Replace Spring Security permitAll on /api/v1/** with default-deny plus authenticated session filter (SEC-C4) — SUG-20261005-sdn"
evidence:
  - "S1: BffSessionInterceptor requires opaque session on /api/v1 except /auth; Lead book-scope 404s foreign ids; org lookups are session-gated but not branch-scoped (PDP parked)"
  - "S2: confirm/search return masked fields only; customerId tokens BLOCK/HOLD are synthetic; no PAN/DOB/address in public records"
  - "S3: Apigee token never on BFF; stub token client; no hard-coded production secret"
  - "S4: no new crypto; existing TokenVaultCipher for workforce session unchanged"
  - "S5: countryCode/nationalNumber/productClass/by validated; unknown by is INVALID_REQUEST"
  - "S6: attack surface grows by session-gated Lead/org/customer collections on the existing BFF — necessary for NIP-APP"
  - "S7: IDOR mitigated on leads (absent not forbidden); CSRF ignored only when Authorization or X-Session-Handle present — cookie-only still CSRF-protected"
  - "S8: actor from session businessUserId; no access-token in public bodies"
  - "S9: no new runtime dependency beyond existing Spring stack"
  - "S10: missing/expired session fail-closed (SESSION_INVALID / SESSION_EXPIRED)"
  - "S11: CBS trust is stub; live Apigee product onboarding is out of scope"
  - "S12: blast radius of a stolen session handle is that RM's book plus org fixtures; stub store is process-local"
  - "RG-5 G1/G3/G8 did not fire as control-behaviour change; T4 human Security sign-off not claimed"
notes: >
  Binding veto is not exercised. Non-bypassable list in Deepali 08 §4 is not met.
  SEC-C4 default-deny is a parked hardening item, not an exploitable bypass while
  the interceptor remains registered on /api/v1/**.
```

---

## Board 5 — QA (Swapnali)

**Question:** How will we know it works — and know when it breaks?

```yaml
# schema: review-verdict
board: QA
plan: PLAN-009
work_item: EPIC-006
reviewer: "Swapnali — QA Lead (agent)"
reviewer_type: AGENT
self_review: true
round: 1
date: "2026-10-05"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "Do not treat 62 green tests as live CBS, PDP, or assisted-sale evidence; they prove the stub slice only"
should_fix:
  - "Add BFF MockMvc for UNCERTIFIED assignee (SP_CERTIFICATION_REQUIRED) — SUG-20261005-uld"
evidence:
  - "Q1: AC-1 country regex; AC-2 nested org; AC-3 exceptionRequired on HOLD; AC-4 ULID 26; AC-5 16-thread token; AC-6 confirm + active-leads — all have tests"
  - "Q2: unit (Ulid, Facade, Catalog, token holder) + Spring MockMvc (BFF, LeadController)"
  - "Q3: unauthenticated 401; unknown customer 404; HOLD short-circuit; BLOCK onboarding; unfinished dedupe CONFLICT"
  - "Q4: Jacoco verification passed — lead 81.6% line, customer 75.0%, BFF 58.5% (scaffold floor 50%; branch not gated on services/*)"
  - "Q5: existing LoginService / TokenVault / BffExceptionHandler tests still in the 37 BFF tests"
  - "Q6: fixtures use CUST-3210 / synthetic mobiles; no real PAN"
  - "Q7: MockMvc is demonstrable to a PO; not a device walkthrough"
  - "Q8: @Tag FUNC-029/030/031; no live network in unit tests"
notes: >
  Unexecuted live CBS is not passed. Q0 hold is not raised on a stub SF5 slice.
```

---

## Board 6 — Risk & Compliance (Shailja S)

**Question:** Can we defend this to a regulator?  
**Severity:** `R3` (stub SP-cert and in-memory are not control evidence)

```yaml
# schema: review-verdict
board: RISK_COMPLIANCE
plan: PLAN-009
work_item: EPIC-006
reviewer: "Shailja S — Compliance & Risk (agent)"
reviewer_type: AGENT
self_review: true
round: 1
date: "2026-10-05"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "In-memory Lead and stub SP-certification are not the audit or IRDAI record; create/assign attribution to bank-persistence remains S11"
should_fix: []
evidence:
  - "R1: IRDAI SP certification is stubbed (UNCERTIFIED token); not claimed as control evidence"
  - "R2: consent not in this slice; no quote/proposal"
  - "R3: session actor / X-Actor-Id on internal Lead calls; no durable audit event store in this change"
  - "R4: no new retention store"
  - "R5: no money movement"
  - "R6: if stub org/CBS were used in a real branch, wrong assignment — mitigated by stub default and PLAN-009 risk"
  - "R7: SUG-20261005-lbf → EPIC-006 → PLAN-009 → tests; D-020/D-021 in DECISION-LOG"
  - "R8: no new disclosure obligation"
  - "T4 Risk & Compliance human sign-off not claimed"
notes: >
  No consent, suitability, or payment path. Standing constraints on those
  journeys are untouched.
```

---

## Board 7 — Operations (Shivanshi)

**Question:** Can we run, observe, and recover this?  
**Severity:** `O3` (stub default; no new page)

```yaml
# schema: review-verdict
board: OPERATIONS
plan: PLAN-009
work_item: EPIC-006
reviewer: "Shivanshi — SRE / Operations (agent)"
reviewer_type: AGENT
self_review: true
round: 1
date: "2026-10-05"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "LEAD_DOWNSTREAM_MODE=http and CUSTOMER_DOWNSTREAM_MODE=http are not a production promotion; no new alert, SLO, or runbook is claimed"
should_fix: []
evidence:
  - "O1: existing BFF/lead/customer deployables; new env LEAD_DOWNSTREAM_MODE / CUSTOMER_DOWNSTREAM_MODE default stub; no Flyway"
  - "O2: no new dashboard or correlation contract; existing actuator health"
  - "O3: nothing pages"
  - "O4: process restart loses in-memory leads; token holder is single-instance lock (not a cluster lock)"
  - "O5: git revert; no data rewrite"
  - "O6: catalogues in-process; no scaling recommendation (no business-load change measured)"
  - "O7: no runbook update in this change"
  - "O8: additive APIs; login rolling-compatible"
notes: >
  PLAN-009 board notes had marked SRE N/A; T3 makes this board mandatory.
  No pod-count advice is offered — there is no named bottleneck.
```

---

## Aggregation

T3 self-review **cannot** close as `APPROVED` without a human board ([11 §2](../../governance/11-REVIEW_GATES.md#2-who-may-sit-on-a-board), approval-gate schema). No board returned `REWORK` or `REJECTED`. Binding Security/Compliance vetoes were not exercised. The gate therefore **escalates to the named human Board 4 (Deepali)** as the T3 human seat; Board 1 (Mahesh) and Board 3 (Rajal D-020/D-021 countersign) should sit with it.

```yaml
# schema: approval-gate
plan: PLAN-009
work_item: EPIC-006
risk_tier: T3
round: 1
verdicts:
  ARCHITECTURE:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Mahesh — Principal Insurance Platform Architect (agent)"
    self_review: true
    conditions:
      - "Do not treat in-memory Lead or stub CBS/HR as the system of record; Flyway Lead DDL stays SUG-20260825-db1; live CBS stays S09/S11"
    evidence:
      - "A1-A10; RG-5 G1/G8 considered; T4 Architecture signature not manufactured"
  TECHNICAL:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Amit — Technical Head (agent)"
    self_review: true
    conditions:
      - "PLAN-009 files_expected must name as-built types: BffSessionInterceptor, CustomerLookupController, LeadClockConfig, gateways, and the three test classes that prove AC-1..AC-6"
    evidence:
      - "T1-T8: 62 tests; Clock constructor fix; token single-flight; revert sufficient"
  PRODUCT:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Rajal — Product Owner (agent)"
    self_review: true
    conditions:
      - "Human Product owner countersigns D-020 (ULID identity) and D-021 (Lead-owned dashboard stages; vertical optional; India +91 mobile rule) — agent recorded them as working decisions"
    evidence:
      - "P1-P12: REST collections; assign+exceptionRequired; quote/payment not bundled"
  SECURITY:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Deepali — Security Architect (agent)"
    self_review: true
    conditions:
      - "Human Board 4 must sit before any promotion of Lead /api/v1 paths beyond stub/local; this AGENT verdict does not satisfy T3's required human board (11 §2) nor a T4 signature"
      - "Unauthenticated Lead/reference calls must remain 401; LeadModuleApiTest.unauthenticatedLeadApisAreRejected is the fail-closed bar"
      - "Do not set CUSTOMER_DOWNSTREAM_MODE=http or LEAD_DOWNSTREAM_MODE=http against live CBS until PDP object-level grants exist (EPIC-006 not_included)"
    evidence:
      - "S1-S12 SECURITY-L1; session fail-closed; CBS token not on BFF; T4 not claimed"
  QA:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Swapnali — QA Lead (agent)"
    self_review: true
    conditions:
      - "Do not treat 62 green tests as live CBS, PDP, or assisted-sale evidence; they prove the stub slice only"
    evidence:
      - "Q1-Q8: AC-to-test map; Jacoco floors held; login regression still in suite"
  RISK_COMPLIANCE:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Shailja S — Compliance & Risk (agent)"
    self_review: true
    conditions:
      - "In-memory Lead and stub SP-certification are not the audit or IRDAI record; create/assign attribution to bank-persistence remains S11"
    evidence:
      - "R1-R8: no consent/payment; attribution fields present; T4 not claimed"
  OPERATIONS:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Shivanshi — SRE / Operations (agent)"
    self_review: true
    conditions:
      - "LEAD_DOWNSTREAM_MODE=http and CUSTOMER_DOWNSTREAM_MODE=http are not a production promotion; no new alert, SLO, or runbook is claimed"
    evidence:
      - "O1-O8: stub default; in-memory loss on restart; no scale advice"
result: ESCALATED
conditions_folded_into_ac:
  - "In-memory Lead / stub CBS are not SoR (ARCHITECTURE)"
  - "PLAN-009 files_expected lists as-built types (TECHNICAL)"
  - "Human Product countersign D-020 and D-021 (PRODUCT)"
  - "Human Board 4 sits before promoting Lead /api/v1 beyond stub (SECURITY)"
  - "Unauthenticated Lead APIs remain 401 (SECURITY)"
  - "No live CBS http mode until PDP (SECURITY)"
  - "Tests are stub evidence only (QA)"
  - "In-memory is not the audit/IRDAI record (RISK_COMPLIANCE)"
  - "http downstream mode is not a production promotion (OPERATIONS)"
should_fix_registered_as: ["SUG-20261005-sdn", "SUG-20261005-uld"]
vetoes: none
human_signoffs: []
```

**Gate result:** `ESCALATED` (T3 self-review). Named humans to sit: **Deepali (Board 4)** required; **Mahesh (Board 1)** and **Rajal (D-020/D-021)** recommended. No agent `APPROVED` the gate. No T4 signature is manufactured.

**Not claimed:** live CBS · PDP grants · Flyway Lead schema · IRDAI SP evidence · GATE-S08 PASS.

---

## Round 2 — 2026-10-07 coding-practices re-review

**Ask:** re-review as-built Java against AIGEM seven boards plus ENG/SEC, SOLID, KISS, naming, and formatting before merge.  
**Freshness:** FRESH 2026-10-07 ([BOOT.md](../../context/BOOT.md) §5; `state_as_of` 2026-09-30).  
**Capsules:** `board-review` ([11 §3](../../governance/11-REVIEW_GATES.md#3-proportionality--which-boards-are-mandatory), [11 §15](../../governance/11-REVIEW_GATES.md#15-running-the-board-as-a-single-agent), [AUTHORITY-QUICK-CARD.md](../../context/personas/AUTHORITY-QUICK-CARD.md)) and `code-change` ([amit-engineering.card.md](../../context/personas/amit-engineering.card.md), [ROLE-GUIDELINES-AND-DOD.md](../../1sb-insurance-integration/service-ssot/ROLE-GUIDELINES-AND-DOD.md), [TESTING-RULES.md](../../1sb-insurance-integration/service-ssot/TESTING-RULES.md)). Standards: [ENGINEERING-AND-SECURE-CODING-STANDARDS.md](../../governance/ENGINEERING-AND-SECURE-CODING-STANDARDS.md), [PR-REVIEW-CHECKLIST.md](../../governance/PR-REVIEW-CHECKLIST.md), [ORG-STANDARDS.md](../../governance/ORG-STANDARDS.md) AP-1.  
**Self-review:** `self_review: true`. T3 still needs a human board. No T4 signature is manufactured. RG-5 walk from round 1 is unchanged.

**Must-fix applied this round:** Spotless ratchet (ENG-6) on the new Lead / Customer / BFF types — 2-space Google style. Behaviour unchanged. Checkstyle Main green after apply.

**Evidence this round:** 62 tests green (lead 15 · customer 10 · BFF 37); Jacoco lead 85% line / customer 81% / BFF 61% (service floor 50%); `spotlessCheck` + `checkstyleMain` green on the three modules.

---

### Board 1 — Architecture (Mahesh)

```yaml
# schema: review-verdict
board: ARCHITECTURE
plan: PLAN-009
work_item: EPIC-006
reviewer: "Mahesh — Principal Insurance Platform Architect (agent)"
reviewer_type: AGENT
self_review: true
round: 2
date: "2026-10-07"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "Do not treat in-memory Lead or stub CBS/HR as the system of record; Flyway Lead DDL stays SUG-20260825-db1; live CBS stays S09/S11"
should_fix:
  - "Fold session enforcement into Spring Security default-deny (SEC-C4) — SUG-20261005-sdn"
evidence:
  - "A1: hexagonal ports held — LeadGateway/CustomerGateway/AccessTokenPort/CustomerInquiryPort; adapters ConditionalOnProperty; no BFF JDBC; no 1SB types"
  - "A2: Lead aggregate and exception rules stay in lead-service; BFF LeadFacade only orchestrates evaluate-then-assign (D-019)"
  - "A3: coupling is directional HTTP; stub adapters are the default so Flutter can integrate without live CBS"
  - "A4: standing constraints held — no PII log statements in new types; Flutter still never receives Apigee OAuth"
  - "A5: no new ADR; D-020/D-021 remain Product working decisions"
  - "A6: no new broker, cache, or database"
  - "A7: in-memory discard on restart; stub createdBy drift is identity-test debt (SUG-20261005-uld), not a migration of stub data"
  - "A8: SF5 scaffold at S08; quote/proposal/payment still absent"
  - "A9: one BFF + two existing skeletons; no eighth service"
  - "A10: replace interceptors/stubs without changing public resource names"
notes: >
  Hexagonal shape is the right pattern for this slice. Authority class remains
  A2_NOTIFY. This agent verdict is not the human Board 1 signature.
```

---

### Board 2 — Technical (Amit)

```yaml
# schema: review-verdict
board: TECHNICAL
plan: PLAN-009
work_item: EPIC-006
reviewer: "Amit — Technical Head (agent)"
reviewer_type: AGENT
self_review: true
round: 2
date: "2026-10-07"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "ENG-6 Spotless/Checkstyle stay green on the three modules (applied this round)"
should_fix:
  - "Share Ulid and persist createdBy on StubLeadGateway; WireMock Http*Gateway — SUG-20261005-uld recurrence 2"
  - "Move product-classes off CustomerLookupController; compile country regex once; drop duplicate SEARCH_BY — SUG-20261007-srp"
evidence:
  - "T1: Java 21 / Spring Boot; single-constructor services; Clock injected (LeadClockConfig, ApigeeClientConfig)"
  - "T2: RFC 7807 via ServiceErrors; SESSION_INVALID / RESOURCE_NOT_FOUND / SP_CERTIFICATION_REQUIRED / CONFLICT / ILLEGAL_TRANSITION"
  - "T3: ApigeeAccessTokenHolder lock + 30s skew; InMemoryLeadRepository ConcurrentHashMap; 16-thread token test"
  - "T4: login paths unchanged; Lead paths additive"
  - "T5: KISS held — in-process catalogues, stub default, no second framework. CountryCodeCatalog compiles Pattern per call (parked)"
  - "T6: DRY mostly held; SEARCH_BY duplicated; StubLeadGateway re-implements Ulid and book rules (parked). LeadFacade does not duplicate exception evaluation"
  - "T7: PLAN-009 files_expected now names gateways, facade, catalogues and AC test classes"
  - "T8: revert is sufficient; in-memory holds no durable customer data"
  - "ENG-1..ENG-10: module layout, ports, OpenAPI, tests, Spotless/Checkstyle, no new floating dep, TODOs carry ids"
  - "SOLID: S — Lead vs Customer vs reference catalogues (product-classes SRP slip parked); O — ports + ConditionalOnProperty; L — Http/Stub honour the same gateway; I — narrow AccessTokenPort/CustomerInquiryPort/LeadGateway; D — application depends on ports"
  - "Naming: Java types PascalCase; REST collections plural on public BFF except stakeholder Assign POST /leads/{id}/assignment; internal /assignments"
  - "Formatting: Spotless 2-space ratchet applied; checkstyleMain green"
notes: >
  No merge-blocking design defect after Spotless. AP-1 tie-break (KISS over
  speculative reuse) is why stub/Ulid sharing stays parked.
```

---

### Board 3 — Product (Rajal)

```yaml
# schema: review-verdict
board: PRODUCT
plan: PLAN-009
work_item: EPIC-006
reviewer: "Rajal — Product Owner (agent)"
reviewer_type: AGENT
self_review: true
round: 2
date: "2026-10-07"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "Human Product owner countersigns D-020 (ULID identity) and D-021 (Lead-owned dashboard stages; vertical optional; India +91 mobile rule)"
should_fix: []
evidence:
  - "P1-P12 from round 1 still hold against the as-built controllers and LeadModuleApiTest"
  - "P3/P6: quote/proposal/payment/campaign still absent"
  - "P4: AC-1..AC-6 observable; AC-7/AC-8 remain pending-human"
  - "P9: public bodies stay bank names (maskedMobile, productClass TERM/SAVINGS/ULIP)"
notes: >
  Formatting and parked SRP cleanups do not change Product behaviour.
```

---

### Board 4 — Security (Deepali)

```yaml
# schema: review-verdict
board: SECURITY
plan: PLAN-009
work_item: EPIC-006
reviewer: "Deepali — Security Architect (agent)"
reviewer_type: AGENT
self_review: true
round: 2
date: "2026-10-07"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "Human Board 4 must sit before any promotion of Lead /api/v1 paths beyond stub/local; this AGENT verdict does not satisfy T3's required human board (11 §2) nor a T4 signature"
  - "Unauthenticated Lead/reference calls must remain 401; LeadModuleApiTest.unauthenticatedLeadApisAreRejected is the fail-closed bar"
  - "Do not set CUSTOMER_DOWNSTREAM_MODE=http or LEAD_DOWNSTREAM_MODE=http against live CBS until PDP object-level grants exist"
should_fix:
  - "Replace Spring Security permitAll on /api/v1/** with default-deny plus authenticated session filter (SEC-C4) — SUG-20261005-sdn"
evidence:
  - "S1-S12 from round 1 re-walked against BffSecurityConfig, BffSessionInterceptor, LeadApplicationService.visibleTo, CustomerSearchService"
  - "SEC-C1: no secrets in new source; stub token is stub-apigee-token"
  - "SEC-C2: no phone/PAN/email log statements in new types; confirm/search stay masked"
  - "SEC-C3: countryCode/nationalNumber/productClass/by validated; unknown by is INVALID_REQUEST"
  - "SEC-C4: interceptor 401s; filter-chain permitAll remains parked debt"
  - "SEC-C9: Apigee token never on BFF; Flutter still sees only the opaque session"
  - "SEC-C10: no new crypto; TokenVaultCipher unchanged"
notes: >
  Binding veto is not exercised. Spotless did not change the trust boundary.
```

---

### Board 5 — QA (Swapnali)

```yaml
# schema: review-verdict
board: QA
plan: PLAN-009
work_item: EPIC-006
reviewer: "Swapnali — QA Lead (agent)"
reviewer_type: AGENT
self_review: true
round: 2
date: "2026-10-07"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "Do not treat 62 green tests as live CBS, PDP, or assisted-sale evidence; they prove the stub slice only"
should_fix:
  - "Add BFF MockMvc for UNCERTIFIED assignee and WireMock for Http*Gateway — SUG-20261005-uld"
evidence:
  - "Q1: AC-1..AC-6 still mapped to tests after format"
  - "Q2: unit + MockMvc; Http gateways remain untested (http mode is not the default)"
  - "Q3: 401, unknown customer 404, HOLD short-circuit, BLOCK onboarding, unfinished CONFLICT"
  - "Q4: Jacoco 2026-10-07 — lead 85% line, customer 81%, BFF 61% (floor 50%)"
  - "Q5: login / TokenVault tests still in the 37 BFF tests"
  - "Q6: fixtures CUST-3210 / synthetic mobiles; no real PAN"
  - "Q8: @Tag FUNC-029/030/031; R2 same-PR tests present for application and api packages"
notes: >
  Q0 hold is not raised on a stub SF5 slice. Unexecuted live CBS is not passed.
```

---

### Board 6 — Risk & Compliance (Shailja S)

```yaml
# schema: review-verdict
board: RISK_COMPLIANCE
plan: PLAN-009
work_item: EPIC-006
reviewer: "Shailja S — Compliance & Risk (agent)"
reviewer_type: AGENT
self_review: true
round: 2
date: "2026-10-07"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "In-memory Lead and stub SP-certification are not the audit or IRDAI record; create/assign attribution to bank-persistence remains S11"
should_fix: []
evidence:
  - "R1-R8 from round 1 unchanged — no consent, payment, or new retention store"
  - "Formatting and parked SRP items do not create a disclosure obligation"
notes: >
  T4 Risk & Compliance human sign-off is not claimed.
```

---

### Board 7 — Operations (Shivanshi)

```yaml
# schema: review-verdict
board: OPERATIONS
plan: PLAN-009
work_item: EPIC-006
reviewer: "Shivanshi — SRE / Operations (agent)"
reviewer_type: AGENT
self_review: true
round: 2
date: "2026-10-07"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "LEAD_DOWNSTREAM_MODE=http and CUSTOMER_DOWNSTREAM_MODE=http are not a production promotion; no new alert, SLO, or runbook is claimed"
should_fix: []
evidence:
  - "O1-O8 from round 1 unchanged — stub default; in-memory loss on restart; token holder is process-local"
  - "O6: no business-load change measured; no pod-count advice"
notes: >
  Style-only Java rewrite does not change deployability.
```

---

### Aggregation (round 2)

T3 self-review **still cannot** close as `APPROVED` without a human board ([11 §2](../../governance/11-REVIEW_GATES.md#2-who-may-sit-on-a-board)). No board returned `REWORK` or `REJECTED`. The only merge-blocking engineering finding (ENG-6 Spotless) was fixed. Remaining findings are parked should_fix.

```yaml
# schema: approval-gate
plan: PLAN-009
work_item: EPIC-006
risk_tier: T3
round: 2
verdicts:
  ARCHITECTURE:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Mahesh — Principal Insurance Platform Architect (agent)"
    self_review: true
    conditions:
      - "Do not treat in-memory Lead or stub CBS/HR as the system of record"
    evidence:
      - "A1-A10 re-walked against as-built ports; T4 Architecture signature not manufactured"
  TECHNICAL:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Amit — Technical Head (agent)"
    self_review: true
    conditions:
      - "ENG-6 Spotless/Checkstyle stay green on the three modules"
    evidence:
      - "T1-T8; SOLID/KISS/naming; 62 tests; Spotless applied"
  PRODUCT:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Rajal — Product Owner (agent)"
    self_review: true
    conditions:
      - "Human Product owner countersigns D-020 and D-021"
    evidence:
      - "P1-P12 unchanged; no Product behaviour change this round"
  SECURITY:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Deepali — Security Architect (agent)"
    self_review: true
    conditions:
      - "Human Board 4 must sit before promoting Lead /api/v1 beyond stub"
      - "Unauthenticated Lead APIs remain 401"
      - "No live CBS http mode until PDP"
    evidence:
      - "S1-S12 and SEC-C1..C10 re-walked; T4 not claimed"
  QA:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Swapnali — QA Lead (agent)"
    self_review: true
    conditions:
      - "Tests are stub evidence only"
    evidence:
      - "Q1-Q8; 62 green; Jacoco floors held after format"
  RISK_COMPLIANCE:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Shailja S — Compliance & Risk (agent)"
    self_review: true
    conditions:
      - "In-memory is not the audit/IRDAI record"
    evidence:
      - "R1-R8 unchanged; T4 not claimed"
  OPERATIONS:
    decision: APPROVED_WITH_CONDITIONS
    reviewer_type: AGENT
    reviewer: "Shivanshi — SRE / Operations (agent)"
    self_review: true
    conditions:
      - "http downstream mode is not a production promotion"
    evidence:
      - "O1-O8 unchanged; no scale advice"
result: ESCALATED
conditions_folded_into_ac:
  - "In-memory Lead / stub CBS are not SoR (ARCHITECTURE)"
  - "ENG-6 Spotless/Checkstyle green (TECHNICAL)"
  - "Human Product countersign D-020 and D-021 (PRODUCT)"
  - "Human Board 4 sits before promoting Lead /api/v1 beyond stub (SECURITY)"
  - "Unauthenticated Lead APIs remain 401 (SECURITY)"
  - "No live CBS http mode until PDP (SECURITY)"
  - "Tests are stub evidence only (QA)"
  - "In-memory is not the audit/IRDAI record (RISK_COMPLIANCE)"
  - "http downstream mode is not a production promotion (OPERATIONS)"
should_fix_registered_as: ["SUG-20261005-sdn", "SUG-20261005-uld", "SUG-20261007-srp"]
vetoes: none
human_signoffs: []
```

**Gate result (round 2):** `ESCALATED` (T3 self-review). Named humans to sit: **Deepali (Board 4)** required; **Mahesh (Board 1)** and **Rajal (D-020/D-021)** recommended. Agent does not approve the merge. No T4 signature is manufactured.

**Coding-practices summary (not a board):** hexagonal ports are in place; SOLID holds with one parked SRP slip; KISS wins over sharing Ulid into the BFF stub; REST/Java names match ADR-017 plus the stakeholder Assign path; formatting now matches the Spotless ratchet.

---

## Owner ratification — 2026-10-07 (not a third agent round)

Mahesh (`repository_owner`) directed: Board 4 "sit before Lead `/api/v1` promotion beyond stub/local" **Approved**; SEC-C4 default-deny **approved**; SUG-srp **unparked**; SUG-atk **unpark**; add live Apigee `/token` and CBS inquiry; IRDAI SP-cert as control evidence is needed.

This is a governance §8 human override. It does **not** manufacture a Deepali T4 signature, live Apigee credentials, or an IRDAI register. The round-2 AGENT Security verdict stays on the record. The gate remains `ESCALATED` (Board 1 / Product countersign still recommended; T4 not claimed). Remaining blockers: `DEP-20260914-apg` host/credentials; `SUG-20261007-ird` IRDAI SoR; `SUG-20261006-atk` Valkey remainder.

```yaml
# schema: review-verdict
board: SECURITY
plan: PLAN-009
work_item: EPIC-006
reviewer: "Mahesh — repository owner (HUMAN); not Deepali"
reviewer_type: HUMAN
self_review: false
date: "2026-10-07"
decision: APPROVED_WITH_CONDITIONS
must_fix: []
conditions:
  - "Lead /api/v1 stays stub/local until live CBS credentials and PDP exist; this ratification is not a promotion"
  - "Unauthenticated Lead/reference calls must remain 401"
  - "Do not store AU-Bima-Platform client secret in git or in Valkey"
  - "FixtureSpCertification is not IRDAI control evidence (SUG-20261007-ird remains parked)"
should_fix:
  - "Cluster Valkey access-token cache after ADR-011 amendment (SUG-20261006-atk remainder)"
evidence:
  - "Owner 2026-10-07: Sit before any Lead /api/v1 promotion beyond stub/local -- Approved"
  - "Owner 2026-10-07: Spring Security default-deny on /api/v1/** (SEC-C4) approved"
  - "BffSecurityConfig authenticated /api/v1/** except /auth and health"
  - "HttpApigeeTokenClient reads APIGEE_CLIENT_ID/SECRET from env"
  - "SpCertificationPort fail-closed fixture; IRDAI SoR not fabricated"
notes: >
  Satisfies T3's required human board for this item as repository_owner.
  Does not satisfy T4 Security sign-off. Does not convert missing credentials
  or IRDAI evidence into approved facts.
```

