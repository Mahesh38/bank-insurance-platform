# Template — Review Verdict

One record per board, per plan. Boards, checklists, and aggregation rules:
[11-REVIEW_GATES.md](../11-REVIEW_GATES.md).

**Rule RG-3 — no evidence, no verdict.** `APPROVED` with an empty `evidence[]` is recorded as
`NOT_RUN`.

---

```yaml
review:
  board: SECURITY                 # ARCHITECTURE | TECHNICAL | PRODUCT | SECURITY | QA | RISK_COMPLIANCE | OPERATIONS
  plan: PLAN-011
  work_item: NFR-011
  reviewer: "Security Architect"
  reviewer_type: HUMAN            # HUMAN | AGENT
  self_review: false              # true if the reviewer also authored the plan
  date: 2026-08-12

  decision: APPROVED_WITH_CONDITIONS
  # APPROVED | APPROVED_WITH_CONDITIONS | REWORK | REJECTED | NOT_APPLICABLE

  must_fix: []                    # blocking — required for REWORK
  conditions:                     # become acceptance criteria on approval
    - "Redis keys must be hashed; no application number or PAN in key names — assert in test"
  should_fix:                     # non-blocking; each triaged as a fresh SUG-####
    - "Consider TLS to Redis in UAT ahead of production"

  evidence:                       # which checks were actually performed
    - "S1: no authorization change — filter runs pre-authorization, unchanged"
    - "S2: reviewed key construction in plan §files_expected; AC-3 covers PII"
    - "S3: Redis credentials via secrets SPI per plan §operational_impact"
    - "S5: no new external input path"
    - "S6: attack surface grows by one outbound connection to a private Redis"
    - "S10: fail-closed confirmed in plan §proposed_solution and AC-2"

  notes: >
    Fail-closed is the right default here. Flagging TLS as a should-fix rather than a
    condition because UAT Redis is private-subnet only.
```

---

## Board checklist stubs

Copy the relevant block; answer each numbered check with a finding, not a tick.

```yaml
# ARCHITECTURE (11 §4)
A1_boundaries:            # respected?
A2_responsibility:        # correct component?
A3_coupling:              # introduced? justified? directional?
A4_principles:            # violates a principle or standing constraint?
A5_adr:                   # needed / exists?
A6_infrastructure:        # unnecessary infrastructure?
A7_migration_risk:        # future migration problem?
A8_stage_fit:             # fits the current stage?
A9_smallest_change:       # smallest structural change?
A10_replacement_cost:     # cost to replace later?

# TECHNICAL (11 §5)
T1_feasibility:  T2_error_paths:  T3_concurrency:  T4_compatibility:
T5_complexity:   T6_duplication:  T7_files_plausible:  T8_rollback_real:

# PRODUCT (11 §6)
P1_satisfies:  P2_matches_expectation:  P3_unrequested_behaviour:  P4_ac_correct:
P5_experience_change:  P6_scope_creep:  P7_out_of_scope_honest:

# SECURITY (11 §7)
S1_authz_change:  S2_pii:  S3_secrets:  S4_encryption:  S5_input_validation:
S6_attack_surface:  S7_owasp:  S8_auditability:  S9_dependencies:  S10_fails_closed:

# QA (11 §8)
Q1_testable_ac:  Q2_levels:  Q3_negative_cases:  Q4_coverage_gates:
Q5_regression_risk:  Q6_test_data:  Q7_demonstrable:  Q8_testing_rules:

# RISK & COMPLIANCE (11 §9)
R1_regulatory:  R2_consent:  R3_audit_attribution:  R4_retention:
R5_financial_controls:  R6_operational_risk:  R7_traceability:  R8_reporting:

# OPERATIONS (11 §10)
O1_deployability:  O2_observability:  O3_alerting:  O4_failure_modes:
O5_rollback:  O6_capacity_cost:  O7_runbook:  O8_rolling_deploy:
```

---

## Aggregated gate record

```yaml
approval_gate:
  plan: PLAN-011
  risk_tier: T3
  round: 1
  verdicts:
    ARCHITECTURE:    { decision: APPROVED,                 reviewer_type: AGENT }
    TECHNICAL:       { decision: APPROVED,                 reviewer_type: AGENT }
    PRODUCT:         { decision: APPROVED,                 reviewer_type: AGENT }
    SECURITY:        { decision: APPROVED_WITH_CONDITIONS, reviewer_type: HUMAN }
    QA:              { decision: APPROVED_WITH_CONDITIONS, reviewer_type: AGENT }
    RISK_COMPLIANCE: { decision: NOT_APPLICABLE,           reviewer_type: AGENT,
                       reason: "no regulated data, consent, or retention impact" }
    OPERATIONS:      { decision: APPROVED,                 reviewer_type: AGENT }

  result: APPROVED
  conditions_folded_into_ac:
    - "Redis keys hashed; no PII in key names (SECURITY)"
    - "Negative test for Redis unavailability (QA)"
  should_fix_registered_as: [SUG-0053, SUG-0054]
  vetoes: none
  human_signoffs: ["Security Architect"]
  approved_on: 2026-08-12
  expires: "Phase 4 gate"        # approvals expire at the next stage boundary (11 §14)
```
