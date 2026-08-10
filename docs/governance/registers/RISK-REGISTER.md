# Risk Register

Open risks with owners, triggers, and responses. A risk is not a problem — it is a **problem
with a probability**. Risks feed the `R` factor in priority scoring
([05 §4](../05-PRIORITY_MODEL.md#4-the-scoring-model)) and the Risk & Compliance board.

**Owner:** Delivery Lead (register) · named owner per risk
**Reviewed:** at every stage gate

---

## 1. Scoring

`Exposure = Likelihood × Impact`, each 1–3.

| Likelihood | | Impact | |
|---|---|---|---|
| 1 | Unlikely in this stage | 1 | Contained; recoverable in hours |
| 2 | Plausible | 2 | Material; delays a gate or degrades a journey |
| 3 | Expected without action | 3 | Severe; data loss, breach, regulatory finding, or go-live block |

| Exposure | Response |
|----------|----------|
| 1–2 | Accept and monitor |
| 3–4 | Mitigate this stage |
| 6 | Mitigate now — usually a P1/P2 work item |
| 9 | Escalate to the PO and Architect immediately |

---

## 2. Open risks

| ID | Risk | WS | L | I | Exp | Owner | Response | Trigger to escalate |
|----|------|----|---|---|-----|-------|----------|---------------------|
| RISK-002 | External UAT dependency (DEP-002) has no owner or date | WS-1 | 3 | 2 | 6 | PO | Name an owner and a date; raise the chase as its own item | Phase 4 gate reaches CANDIDATE with 4.3 open |
| RISK-003 | Bank AD technology unconfirmed (DEP-010) | WS-2 | 2 | 3 | 6 | PO + Architect | Adapter design must remain federation-agnostic; do not pre-commit | Phase 1 gate PASSED with no confirmation |
| RISK-004 | In-memory idempotency is unsafe multi-instance (TD-010) | WS-1 | 2 | 3 | 6 | Tech Lead | Single-instance constraint documented; Redis scheduled Phase 5.4 | Any plan to run > 1 instance |
| RISK-005 | AWS Secrets Manager provider is a stub; prod profile fails fast (TD-006) | WS-1 | 2 | 3 | 6 | Platform | Keep fail-fast; schedule at Phase 6 | AWS deployment target confirmed |
| RISK-006 | Service coverage is on an interim floor (QA-001 partial) | WS-1 | 2 | 2 | 4 | QA Lead | Close or waive with expiry at gate 4.7 | Phase 4 gate CANDIDATE with 4.7 unresolved |
| RISK-007 | Raw payload capture incomplete for status / master-data (TD-023) | WS-1 | 2 | 2 | 4 | Tech Lead | Scope decided by compliance review 4.4 | Compliance requires full capture |
| RISK-008 | No QA Engineer / QA Lead cycle ran for Phase 4 stories (single-agent branch) | WS-1 | 2 | 2 | 4 | QA Lead | Recorded variance in `phase-4/STATUS.md`; QA pass before UAT sign-off | UAT exposure without a QA cycle |
| RISK-009 | 1SB sandbox instability could stall E2E in CI (gate 4.1) | WS-1 | 2 | 2 | 4 | Eng | Gated nightly fallback already sanctioned by ACTION-PLAN 4.1 | E2E flakiness blocks the pipeline |
| RISK-010 | Governance adopted mid-flight; historical work never passed AIGEM gates | Both | 3 | 1 | 3 | Delivery Lead | Deliberate ([19 §5](../19-PORTING_GUIDE.md#5-bootstrapping-into-an-existing-project-mid-flight)) — forward-only adoption | An incident traces to un-triaged historical work |

## 3. Accepted risks

Risks knowingly carried, with the acceptance recorded so they are not re-raised as findings.

| ID | Risk | Accepted by | Until | Why acceptable |
|----|------|-------------|-------|----------------|
| → [RISK-004](#2-open-risks) | In-memory idempotency | Tech Lead | Phase 5.4 | Single instance in UAT; scale-out is gated |
| → [RISK-010](#2-open-risks) | Forward-only governance adoption | Delivery Lead | — | Backfilling costs days and changes no shipped code |

## 4. Closed risks

| ID | Risk | Closed | How |
|----|------|--------|-----|
| RISK-001 | Current-state file unratified; agents may triage against a wrong stage | 2026-08-10 | Ratified by the Solution Architect (GOV-004); `provisional: false`. `FreshnessCheck` now halts if the state goes stale, so the risk cannot silently return |

---

## 5. Raising a risk

```yaml
risk:
  id: RISK-011
  statement: "If X happens, then Y, causing Z"     # not "X is bad"
  workstream: WS-1
  likelihood: 2
  impact: 3
  exposure: 6
  owner: "named person or role"
  response: MITIGATE          # ACCEPT | MITIGATE | TRANSFER | AVOID
  mitigation: "the work item that reduces it"
  escalation_trigger: "the observable event that makes this urgent"
  review_at: "Phase 4 gate"
```

A risk without an owner and an escalation trigger is a worry, not a risk. Worries do not belong
in the register.
