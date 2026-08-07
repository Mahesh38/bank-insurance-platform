# Assumption Register

Beliefs the plan rests on that have not been verified. Every assumption has an expiry, a
validation method, and — critically — a **pre-computed consequence if it turns out to be
false**, so invalidation triggers a known action instead of a debate.

**Owner:** whoever relies on the assumption
**Model:** [16 §4](../16-DECISION_MODEL.md#4-evidence-standard)

---

## 1. Open assumptions

| ID | Assumption | Used by | Validation | Expiry | Status | If invalidated |
|----|------------|---------|------------|--------|--------|----------------|
| ASM-001 | WS-1 is in Phase 4 (Hardening); Phases 0–3 are complete | All WS-1 triage | PO + Architect ratify [01](../01-CURRENT_STATE.md) §3/§4 | Phase 4 gate | OPEN | Every stage-fit verdict since adoption is re-run; parked targets re-based |
| ASM-002 | The service runs single-instance through Phase 4 | TD-010, RISK-004 | Confirm with Ops at the gate review | Phase 4 gate | OPEN | TD-010 becomes P1, not P4; Redis work pulls into Phase 4 via CR |
| ASM-003 | 1SB sandbox is stable enough for CI-gated E2E | Gate 4.1 | First E2E run over one week | Gate 4.1 delivery | OPEN | Fall back to gated nightly (already sanctioned by ACTION-PLAN 4.1) |
| ASM-004 | At least one bank app team is available for UAT integration this stage | Gate 4.3, DEP-002 | PO confirms a named team and slot | Phase 4 gate | OPEN | 4.3 becomes externally blocked; gate needs a waiver or the criterion moves to Phase 5 |
| ASM-005 | 7-year retention for auth/admin events is the correct regime | WS-2 A.5, CMP-3 | Compliance confirmation | WS-2 Phase 1 gate | OPEN | Retention config changes; data already written may need remediation |
| ASM-006 | No AWS deployment target before Phase 6 | TD-006, RISK-005 | Platform roadmap confirmation | Phase 5 gate | OPEN | TD-006 jumps to P1; secrets provider work pulls forward |
| ASM-007 | Health and Motor reuse `QuoteService` orchestration unchanged | Phase 5 planning, TD-009 | First Health handler spike | Phase 5 entry | OPEN | TD-009 (domain ports) becomes a prerequisite, not a deferral; Phase 5 sizing grows |
| ASM-008 | Compliance will accept audit coverage limited to quote/proposal/payment paths | TD-023, gate 4.4 | Compliance review (gate 4.4) | Gate 4.4 | OPEN | TD-023 becomes P1 COMP work in Phase 4, not a Phase 5 deferral |

## 2. Validated

| ID | Assumption | Validated | Evidence |
|----|------------|-----------|----------|
| — | — | — | — |

## 3. Invalidated

| ID | Assumption | Invalidated | Consequence taken |
|----|------------|-------------|-------------------|
| — | — | — | — |

---

## 4. Using assumptions in triage

An assumption may serve as evidence only at tier **E5** — expert reasoning with a named
mechanism ([16 §4](../16-DECISION_MODEL.md#4-evidence-standard)). Rule EV-1 therefore applies:

> A **MUST** claim resting only on an unvalidated assumption downgrades to **SHOULD** until the
> assumption is validated.

When an assumption is invalidated, [16 §7](../16-DECISION_MODEL.md#7-revalidation-triggers)
requires re-validating **every item and plan that cites it** — which is why `used_by` is
mandatory rather than nice to have.
