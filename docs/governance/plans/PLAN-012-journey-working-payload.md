# PLAN-012 — Journey orchestrator and JSON-until-payment

> **Superseded in part (2026-10-21).** The store chosen here — Aurora `quotation.quote_working` /
> `proposal.proposal_working` with `payload_enc BYTEA` — was replaced by DynamoDB tables
> `{env}-quotation-working` / `{env}-proposal-working` under
> [`DB-DEC-0005`](../../platform/data-architecture/DB-DEC-0005-dynamodb-working-payload.md) (owner HUMAN
> ratification). JSON-until-payment, ownership, materialise on `RECONCILED` and the journey/payment
> design are unchanged. After payment, proposal answers and status changes are structured rows
> (`proposal.proposal_answer`, `proposal.proposal_status_history` — `D-025`). Read "DynamoDB working
> item" wherever this plan says `quote_working` / `proposal_working`.

```yaml
# schema: implementation-plan
id: PLAN-012
work_item: DATA-005
origin: SUG-20261019-jwp
workstream: WS-3
risk_tier: T3
author: "agent:cursor (persona: Aarti)"
date: "2026-10-19"

objective: >
  After this change engineers have the Journey orchestrator table contract and
  design DDL for quote/proposal encrypted JSON working envelopes that materialize
  to structured rows only after Payment RECONCILED, without applying Flyway or
  putting JSON on journey.

problem: >
  DATA-001 quotation/proposal/journey DDL writes structured quote/offer/proposal
  from the first persist. The owner directed unstructured JSON until payment
  completes, then structured. The Journey service is a skeleton. Without this
  pack S11 invents JSON-on-journey (INV-JRN-02 / OR-01) or a second store.

proposed_solution: >
  Admit DATA-005 on the Aarti design lane. Add quotation.quote_working and
  proposal.proposal_working (payload_enc + extracted control columns). Add
  journey_transition and journey.outbox_event. Publish 06-journey-orchestrator-
  and-working-payload.md (hexagon, advance-command, materialize sequence).
  Freeze working payloads after materialized_at. Grant app_quotation /
  app_proposal / app_journey. Record DB-DEC-0004. Leave Flyway apply and
  service implementation parked at S09/S11.

alternatives:
  - option: "Store the working JSON on journey.journey"
    rejected_because: "INV-JRN-02 / OR-01 / SC-W3-6 — orchestrator coordinates, never decides."
  - option: "DynamoDB document store for in-flight quote/proposal"
    rejected_because: "DB-DEC-0001 rejected a second engine at R0; no volume evidence."
  - option: "Let Journey materialize structured quote/proposal rows"
    rejected_because: "Cross-schema write; no grant; ownership defect."
  - option: "Plaintext JSONB working column"
    rejected_because: "Members, nominee and form PII must not be queryable (DR-PII-01, INV-PRP-05)."
  - option: "Apply Flyway and implement the skeleton service in this turn"
    rejected_because: "DR-MIG-04; apply is S09; Journey runtime is S11."

affected_components:
  - docs/platform/data-architecture
  - docs/governance/plans
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/architecture/README.md
  - docs/context/AGENT-CONTEXT-INDEX.yaml
  - docs/context/DOC-MAP.yaml

files_expected:
  - docs/platform/data-architecture/06-journey-orchestrator-and-working-payload.md
  - docs/platform/data-architecture/DB-DEC-0004-working-payload-until-payment.md
  - docs/platform/data-architecture/DATA-005.work-item.yaml
  - docs/platform/data-architecture/schemas/08-quotation.sql
  - docs/platform/data-architecture/schemas/09-proposal.sql
  - docs/platform/data-architecture/schemas/10-payment.sql
  - docs/platform/data-architecture/schemas/12-journey.sql
  - docs/platform/data-architecture/schemas/00-cluster-bootstrap.sql
  - docs/platform/data-architecture/schemas/90-routines.sql
  - docs/platform/data-architecture/schemas/91-grants.sql
  - docs/platform/data-architecture/01-physical-design.md
  - docs/platform/data-architecture/02-operations-and-troubleshooting.md
  - docs/platform/data-architecture/README.md
  - docs/governance/plans/PLAN-012-journey-working-payload.md
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/architecture/README.md
  - docs/context/AGENT-CONTEXT-INDEX.yaml
  - docs/context/DOC-MAP.yaml

data_changes: "design DDL only — not applied"
api_changes: none
security_impact: "additional ⚑ ciphertext columns (quote_working.payload_enc, proposal_working.payload_enc); no new plaintext PII"
compliance_impact: "working envelopes are not sold-policy evidence; structured rows after RECONCILED remain the record"
backward_compatibility: "compatible — design artefacts; runtime Flyway unchanged"
performance_impact: none
operational_impact: none

testing:
  unit: []
  integration: []
  other:
    - "PostgreSQL parse of 08-quotation.sql, 09-proposal.sql, 10-payment.sql, 12-journey.sql, 90-routines.sql"
    - "Cross-doc: 06 sequence ↔ DDL tables ↔ grants ↔ freeze routine"
    - "python3 scripts/context/build-doc-map.py after new docs"
    - "python3 scripts/governance/ci-checks.py"
    - "No CURRENT-STATE stage field edit"

rollback: >
  Revert the documentation commit. No runtime or data impact.

dependencies:
  - DATA-001
  - DB-DEC-0001
  - ADR-012
  - INV-JRN-02
  - OR-01
assumptions: []
risks:
  - risk: "S11 implements JSON on journey despite this pack"
    mitigation: "AC-1 names the five legal tables; ArchUnit / schema grants refuse quotation to app_journey"
  - risk: "Payment decrypts the working envelope to read premium"
    mitigation: "selected_* extracted columns; INV-PAY-03 via Quotation API"
  - risk: "Abandoned working rows treated as sold-policy evidence"
    mitigation: "materialize only after RECONCILED; retain_until + job_retention purge"

acceptance_criteria:
  - "AC-1 Journey tables are stage + refs + transition + outbox + idempotency"
  - "AC-2 Advance calls owning service then one local journey transaction"
  - "AC-3 Pre-payment write path is encrypted working envelopes"
  - "AC-4 Materialize after RECONCILED freezes payload_enc"
  - "AC-5 selected_* control columns exist for payment"
  - "AC-6 no stage-field edit and no manufactured T4 signature"
  - "AC-7 Payment is on the journey (stage + paymentId) and independent (own schema, own outbox, no JSON envelope)"
  - "AC-8 Postgres BYTEA transactions not Dynamo; unconverted purge; sold path is 7y"

out_of_scope:
  - "Runtime Journey / Quotation / Proposal / Payment code"
  - "Flyway apply"
  - "JSON on journey or DynamoDB"
  - "Human T4 / T3 signatures"

estimate: M

reviews: []
variance_log:
  - date: "2026-10-19"
    change: "Added 06 §6 and payment.outbox_event + app_payment grants; files_expected now includes 10-payment.sql; AC-7"
    reason: "Owner follow-up: payment is part of the journey though independent. Absorbed SUG-20261019-pay. Not a new decision — Payment was already structured; the pack now states the independence contract."
    re_review: "none — design DDL only; no T4 claim"
  - date: "2026-10-19"
    change: "Added 06 §10 transaction/store/retention; journey.retain_until/archived_at; purge does not delete paid journeys; AC-8"
    reason: "Owner follow-up SUG-20261019-txn. Dynamo remains rejected (DB-DEC-0001). Unconverted path is RET-WORKING."
    re_review: "none — design DDL only; Shailja still owns OPEN-RET-LEAD-STUB"
```
