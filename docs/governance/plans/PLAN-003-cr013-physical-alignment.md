# PLAN-003 — Align R0 physical pack with CR-013

```yaml
# schema: implementation-plan
id: PLAN-003
work_item: DATA-002
origin: SUG-20260825-aln
workstream: WS-3
risk_tier: T3
author: "agent:cursor-grok (persona: Aarti)"
date: "2026-08-25"

objective: >
  After this change the DATA-001 physical pack expresses CR-013 / ADR-014 facts
  (Lead archive, issuanceMode, off-platform Policy ingest, isolated MIS path) and
  ADR-012 per-producer outbox, still as design DDL only.

problem: >
  DATA-001 DDL has no ARCHIVED, no issuance_mode, policy.lead_id NOT NULL with no
  source, no policy state history, pack text that says Reporting is out of R0, and
  outbox_event only on identity. origin/main admitted those capabilities on 2026-08-25
  (CR-013, DEC-20260825-01, ADR-014). Building W1/W3/W4 against the current pack
  would be an incorrect domain model.

proposed_solution: >
  Publish the alignment review now (already done this turn). Implement the DDL/docs
  delta in a later commit after rebase onto origin/main: extend opportunity, proposal,
  policy; add ingest and state-history tables; add outbox_event to publishing schemas;
  design the isolated read path and grants; rewrite Reporting-out-of-R0 sentences.
  Joint Aarti/Mahesh still chooses the archive mechanism (DEC §12). No Flyway apply.
  No S07-G5 signature.

alternatives:
  - option: "Treat DATA-001 as sufficient and only rename Opportunity to Lead in prose"
    rejected_because: "Missing CHECKs and nullable lead_id are integrity failures, not naming."
  - option: "Implement the DDL in the same turn the gap was raised"
    rejected_because: "09 — a suggestion is never implemented in the turn it is raised."
  - option: "Stand up a second Aurora cluster or isolation microservice in S08"
    rejected_because: "ADR-008 / D5 / SUG-20260825-wl1 REJECTED."
  - option: "Mint Leads for off-platform sales so lead_id stays NOT NULL"
    rejected_because: "ADR-005 / D3 / C-ING-1."

affected_components:
  - "docs/platform/data-architecture/"
  - "docs/governance/registers/SUGGESTION-REGISTER.md"
  - "docs/governance/registers/DECISION-REGISTER.md"

files_expected:
  - docs/platform/data-architecture/DATA-002-cr013-alignment.md
  - docs/platform/data-architecture/DB-DEC-0002-cr013-alignment-review.md
  - docs/platform/data-architecture/DATA-002.work-item.yaml
  - docs/governance/plans/PLAN-003-cr013-physical-alignment.md
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/governance/registers/DECISION-REGISTER.md
  - docs/platform/data-architecture/README.md
  - docs/context/AGENT-CONTEXT-INDEX.yaml
  - docs/application-lifecycle-bible/evidence/S07-solution-architecture-evidence.md

data_changes: "none in this plan revision — review + schedule only; schema delta is a later DATA-002 commit"
api_changes: none
security_impact: "PII — ingest trust boundary called out; no runtime change"
compliance_impact: "retention / reporting — gaps vs C-RET-1, C-ING-1, C-ISO-1, C-ISS-1 named; no runtime change"
backward_compatibility: compatible
performance_impact: none
operational_impact: "W1/W3/W4 cannot consume DATA-001 as-is; sequence unchanged"

testing:
  unit: []
  integration: []
  other:
    - "python3 scripts/context/context-load.py validate"
    - "python3 scripts/context/build-doc-map.py"
    - "java scripts/governance/FreshnessCheck.java (exit 0 or 1 warn only)"
    - "python3 scripts/governance/ci-checks.py"
    - "Confirm 04-opportunity.sql has no ARCHIVED; 09/11 lack issuance_mode; only identity has outbox_event"

rollback: >
  Revert the documentation commit. No database has been migrated by this change.

dependencies:
  - "DATA-001 pack"
  - "origin/main CR-013 / DEC-20260825-01 / ADR-014"
  - "ADR-012"

acceptance_criteria:
  - "AC-check Alignment report lists keep / must-create / must-not-create against CR-013"
  - "AC-triage SUG-20260825-aln admitted as DATA-002; no DDL implemented in the raise turn"
  - "AC-verdict DB-DEC-0002 is CHANGES_REQUIRED; S07-G5 remains OPEN"
  - "Later commit (not this plan revision): AC-1..AC-6 on DATA-002.work-item.yaml"

out_of_scope:
  - "Writing or applying the CR-013 DDL in this revision"
  - "Choosing partition vs table vs dump for Lead archive"
  - "Proven restore / purge execution"
  - "Second cluster or new isolation service"
  - "Human signatures"
  - "Changing CURRENT-STATE.yaml stage fields"

assumptions:
  - "CR-013 content on origin/main is the governing R0 scope even though this branch has not rebased"
  - "Human T4 on CR-013 does not block designing the physical delta"

risks:
  - risk: "Blind merge of origin/main drops DATA-001 OPEN-I1 pointers"
    mitigation: "Rebase with an explicit keep-both resolution before DDL work"
  - risk: "Readers treat CHANGES_REQUIRED as a block on S08 floor"
    mitigation: "S08 CI gates are unchanged; this is S07-E04/W1-W4 data design debt"

estimate: M
reviews: []
```
