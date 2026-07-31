# BRD — P0 Capabilities with Acceptance Criteria

**Product:** AU Bank Insurance Distribution Platform  
**Release:** R0  
**Version:** 0.2 (align to Working Decisions Draft v1 — Life; three journeys; ETB)  
**Note:** Replaces template BR shells for P0 only. P1/P2 capabilities listed as stubs.  
**SSOT:** [../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md](../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) · Chapter map: [BRD-OVERVIEW.md](./BRD-OVERVIEW.md)

---

## Priority & MoSCoW

| Tag | Meaning |
|-----|---------|
| Must | R0 cannot go live without |
| Should | Strongly desired in R0; may slip with sponsor OK |
| Could | Nice in R0 |
| Won’t | Explicitly not R0 |

---

## BR-SEC — Identity, access & audit (Must)

### BR-SEC-010 — RM authentication
**AC:**
1. Unauthenticated RM cannot access journey APIs/UI.  
2. Authenticated session exposes stable `actorId` used in audit.  
3. Logout / expiry invalidates session.

### BR-SEC-020 — Role-based access
**AC:**
1. Roles at minimum: `RM`, `OPS_READONLY`, `ADMIN_CONFIG` (names flexible).  
2. RM can create/progress own leads; cannot change distributor config.  
3. Denied actions return 403 with stable error code.

### BR-SEC-030 — Audit trail
**AC:**
1. Material actions emit audit events: lead create, consent, suitability, quote, proposal, payment, status check.  
2. Event includes actorId, action, journey/lead id, timestamp, outcome.  
3. Proposal/payment events include distributorId (from secrets) and agentId when applicable.  
4. Audit write failure does not silently succeed the business action without logging a platform alert (policy: prefer fail-open on audit only if Infosec agrees — **confirm**).

### BR-SEC-040 — PII protection
**AC:**
1. App logs never contain plaintext PAN, full mobile, email, DOB, full payment URL.  
2. Automated test proves masking on sample payloads.

---

## BR-CUST — Customer management (Must)

### BR-CUST-010 — Search existing customer
**AC:**
1. RM can search by bank-approved keys (e.g. CIF / mobile / account — **confirm**).  
2. No match → clear empty state; no fabricated customer.  
3. Match displays name + key identifiers permitted by policy.

### BR-CUST-020 — Prefill
**AC:**
1. Selected customer prefills journey party fields available from CIF.  
2. Prefill source and timestamp recorded on journey.  
3. RM can correct editable fields per policy; corrections audited.

---

## BR-LEAD — Lead management (Must)

### BR-LEAD-010 — Create lead
**AC:**
1. RM can create lead linked to customer + LOB.  
2. System assigns unique `leadId` / journey id.  
3. Lead owned by creating RM (reassignment = Should).

### BR-LEAD-020 — Resume lead
**AC:**
1. RM can open in-progress lead and land on last incomplete stage.  
2. Abandoned leads can be reopened per rules (expiry = configurable — **confirm TTL**).

### BR-LEAD-030 — Lead status
**AC:**
1. Status reflects value-stream stage (Consent…Policy).  
2. Status history queryable for audit.

---

## BR-CONSENT — Consent (Must)

### BR-CONSENT-010 — Capture
**AC:**
1. Journey cannot enter Suitability/Quote without valid consent (if D-005/A6 hold).  
2. Consent stores: version/text id, timestamp, channel, actor, customer acknowledgement method.  
3. Evidence retrievable by journey id.

### BR-CONSENT-020 — Validity
**AC:**
1. Expired/withdrawn consent blocks progression with clear RM message.  
2. Re-consent creates new version; old retained.

### BR-CONSENT-030 — Withdrawal
**AC:**
1. Withdrawal recorded with time + actor.  
2. Open marketing/sale steps halt per Compliance policy.

*Rule values (TTL, wording ids) = GAP-006.*

---

## BR-SUIT — Suitability & recommendation (Must)

### BR-SUIT-010 — Capture assessment
**AC:**
1. RM/customer completes suitability questionnaire for first LOB.  
2. Answers + computed outcome stored with version.  
3. Incomplete suitability cannot proceed to Product/Quote.

### BR-SUIT-020 — Recommendation
**AC:**
1. System records recommended product category/LOB outcome.  
2. If RM overrides, reason code mandatory + audited (if overrides allowed — **confirm**).

### BR-SUIT-030 — Ineligibility
**AC:**
1. Failed eligibility stops quote with explanation suitable for RM.  
2. Event audited.

*Questionnaire content = GAP-007.*

---

## BR-PROD — Product catalogue & matrix (Must — read path)

### BR-PROD-010 — Eligible products
**AC:**
1. Given customer + suitability outcome, matrix returns eligible products for first LOB.  
2. Zero products → explicit empty state (no quote call).  
3. Product list shows bank product name + insurer label.

### BR-PROD-020 — Catalogue seed
**AC:**
1. R0 products/insurers loadable via config/admin seed (full admin UI = Won’t).  
2. Effective dating respected if provided.

---

## BR-QUOTE / BR-COMP — Quote & comparison (Must)

### BR-QUOTE-010 — Create quote
**AC:**
1. RM submits quote request for selected products/mode (single/multi — **confirm**).  
2. API returns bank `quoteJobId` immediately; aggregator protocol hidden.  
3. Idempotent create with Idempotency-Key (Must for APIs).

### BR-QUOTE-020 — Quote result
**AC:**
1. RM sees PENDING / PARTIAL / COMPLETED / FAILED / TIMEOUT in bank language.  
2. Offers list premium/key benefits fields normalised.  
3. Partial insurer failures do not wipe successful offers.

### BR-COMP-010 — Compare & select
**AC:**
1. RM can compare ≥2 offers when available.  
2. Selection stored; required before proposal.  
3. Expired quote cannot proceed (message + refresh path).

---

## BR-PROP — Proposal (Must)

### BR-PROP-010 — Schema
**AC:**
1. Platform fetches dynamic proposal schema for selected offer.  
2. UI renders fields without hardcoding insurer-only static screens as sole path.  
3. Prefill from customer/suitability/quote where mapped.

### BR-PROP-020 — Save / resume
**AC:**
1. Partially filled proposal can be saved and resumed.  
2. Validation errors field-level where schema allows.

### BR-PROP-030 — Submit
**AC:**
1. Missing `agentId` → reject with `AGENT_ATTRIBUTION_MISSING`; no insurer call.  
2. Successful submit returns bank proposal job/application refs.  
3. Audit includes agentId + distributorId.

---

## BR-UW — Underwriting tracking (Must — lite)

### BR-UW-010 — Status
**AC:**
1. RM can refresh normalised application status + manufacturer substatus.  
2. 404/not found handled cleanly.  
3. Pending requirements list = Should (full docs upload = Could/P1).

---

## BR-PAY — Payment (Must)

### BR-PAY-010 — Payment session
**AC:**
1. For payable application, RM/customer can create payment session.  
2. `paymentUrl` is HTTPS; returned to client; **not** written to logs (ref only).  
3. Non-payable → clear business error (e.g. 409).

### BR-PAY-020 — Outcome
**AC:**
1. Platform records payment status (initiated/success/failure).  
2. Failure allows retry per rules.  
3. Audit `PAYMENT_URL_RETRIEVED` / equivalent with distributorId.

---

## BR-POL — Policy (Must — visibility)

### BR-POL-010 — Issuance visibility
**AC:**
1. When insurer issues policy, RM/customer can see policy number + status.  
2. Document/download link if provided by hub = Should.  
3. Journey marked Policy Issued.

---

## BR-RM — RM workspace (Must — minimum)

### BR-RM-010 — Pipeline
**AC:**
1. RM sees list of own leads/journeys with stage + last update.  
2. Filter by status at minimum.  
3. Click-through resumes journey.

### BR-RM-020 — Tasks
**AC:**
1. Should: surface next action (e.g. “Awaiting payment”, “UW requirements”).  
2. Won’t: full enterprise task engine in R0.

---

## BR-INT — Integration Hub (Must)

### BR-INT-010 — Adapter boundary
**AC:**
1. Channel/L2 services call bank-canonical APIs only.  
2. 1SB payloads confined to adapter module(s).  
3. Contract tests exist for ports with fake adapter.

### BR-INT-020 — Phase A coverage
**AC:**
1. Hub supports masters (as needed), quote, proposal, payment URL, application status for first LOB.  
2. Upstream errors normalised to bank error model (retryable flag).

### BR-INT-030 — Raw evidence
**AC:**
1. Should: encrypted raw REQ/RES stored for dispute (align COMP-003 patterns).  
2. Retention configurable (default 7 years draft).

---

## BR-REP — Reporting (Should / minimum Must)

### BR-REP-010 — Pilot funnel
**AC:**
1. Counts available: leads created, consents, suitability completed, quotes, proposals, payments, policies.  
2. Export or simple dashboard for pilot ops.  
3. Full BI suite = Won’t for R0.

---

## BR-COMM — Communications (Should)

### BR-COMM-010 — Stage notifications
**AC:**
1. Configurable triggers for: quote ready, payment link, policy issued (minimum set TBD).  
2. Template ids versioned.  
3. Failure to send does not corrupt journey state; alert ops.

---

## Explicit Won’t (R0)

| Capability | Reason |
|------------|--------|
| BR-SERV Renewals full suite | R2+ |
| Claims | Out of platform scope |
| Direct insurer adapters | Phase B/C |
| Full self-serve journey | After RM pilot |
| Unlimited LOBs | One LOB only |

---

## Traceability example

| AC | BG | Journey | Process |
|----|----|---------|---------|
| BR-CONSENT-010 | BG-006 | CJ-04 / RMJ-04 / JRN-004 | BP-004 |
| BR-QUOTE-010 | BG-001/003 | CJ-07 / RMJ-07 / JRN-007 | BP-007 |
| BR-PROP-030 | BG-006 | CJ-09 / RMJ-08 | BP-008 |
| BR-PAY-010 | BG-001/005 | CJ-11 / RMJ-10 | BP-010 |

---

## Revision plan

| Rev | Trigger |
|-----|---------|
| 0.2 | After Session 1 (LOB, done definition) |
| 0.3 | After Consent/Suitability rule packs |
| 1.0 | Design freeze sign-off |
