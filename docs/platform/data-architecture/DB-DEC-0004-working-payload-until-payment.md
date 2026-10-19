# DB-DEC-0004 — Working payload until payment, then structured

Draft Aarti decision record. Format:
[`04-operating-and-review-contract.md` §4](../../context/roles/principal-insurance-data-database-architect/04-operating-and-review-contract.md#4-standard-aarti--dba-decision-output).

Joint with Mahesh: orchestrator boundary and SoT *timing* (`INV-JRN-02`, `OR-01`).
Joint with Deepali: encrypted working envelopes (`DR-PII-01`).

```yaml
database_decision:
  id: DB-DEC-0004
  subject: "Journey orchestrator tables + quote/proposal JSON until Payment RECONCILED, then structured"
  stage: "S08 design delta / S09 apply parked / S11 owning-service runtime"
  accountable_database_authority: "Aarti — Principal Insurance Data & Database Architect / DBA"
  business_owner: "Rajal — Product (quote/proposal remain the same business objects; persistence timing changes)"
  architecture_owner: "Mahesh — mandatory joint review (INV-JRN-02, OR-01, no cross-schema write)"
  engineering_owner: "Amit — Journey / Quotation / Proposal repositories at S11; Flyway apply at S09"
  compliance_reviewer: "Shailja — working envelopes are not sold-policy evidence; structured rows after RECONCILED are"
  security_reviewer: "Deepali — payload_enc BYTEA + encryption_key_id; no plaintext JSONB of members/nominee/form"
  decision: >
    Journey Orchestration writes only schema journey (journey, journey_ref,
    journey_transition, outbox_event, idempotency_record) via app_journey.
    Quotes and proposals persist as encrypted JSON working envelopes in the
    owning schemas (quotation.quote_working, proposal.proposal_working) until
    Payment reaches RECONCILED. Owning services then INSERT the existing
    structured quote/offer/proposal/UW rows and freeze the envelope.
    Payment, policy, consent and suitability stay structured from the first write.
    Journey never stores the JSON and never materializes another context's rows.
  status: APPROVED_WITH_OBSERVATIONS
  database_severity: D1
  rationale: >
    The request is a SoT timing change, not an ownership change. INV-JRN-02 and
    OR-01 forbid putting eligibility, premium, UW or form JSON on journey.
    Plaintext JSONB would make members and nominee queryable (DR-PII-01,
    INV-PRP-05). Extracted control columns sit beside the envelope so INV-QUO-01,
    INV-QUO-05 and INV-PAY-03 can run without decrypt. Materialize after
    RECONCILED is eventual (ADR-012); there is no 2PC. Abandoned journeys never
    materialize and are not a sold-policy record.
  data_ownership: >
    Unchanged: one writer per schema. Quotation owns both working and structured
    quote rows. Proposal owns both working and structured proposal/UW rows.
    Journey owns stage and refs only. Payment never SELECTs *_working.
  integrity_controls:
    - "quote_working / proposal_working PK = quote_id / proposal_id"
    - "partial unique open-journey indexes on working tables"
    - "CHECK on lob and state; selected_* CHECK on quote_working"
    - "fn_freeze_working_payload rejects payload_enc UPDATE after materialized_at"
    - "structured quote / offer / proposal / UW are INSERT-only (grants)"
    - "Journey transition and outbox INSERT-only / no DELETE"
    - "Idempotency row in the same transaction as the owning write"
  transaction_consistency: >
    ACID inside one schema. Advance command: HTTP to callee, then one local
    journey transaction (stage + ref + transition + outbox + idempotency).
    Materialize is a later owning-schema transaction on PaymentReconciled.
    No 2PC. Retry of the command is idempotent.
  performance_implications: >
    Working-table indexes follow journey_id and open state. Payment reads
    selected_* via Quotation API, not by decrypting the envelope. Structured
    tables stay empty until a sold path — R0 write amplification is one
    working row then one structured copy.
  security_lifecycle_implications: >
    payload_enc BYTEA + encryption_key_id. Deepali owns the CMK hierarchy
    (S09-E04-S03). This pack specifies ciphertext placement, not key policy.
    job_retention may purge working rows past retain_until; it cannot DELETE
    structured sold-policy rows.
  availability_recovery_implications: >
    Same Aurora cluster as DB-DEC-0001. Working rows are reconstructible from
    the owning service + outbox; they are not the issuance gate. Restore
    unproven (D2 until S09) — inherited observation, not new.
  migration_implications: >
    Design DDL only. Apply in owning-service Flyway at S09
    (quotation / proposal / journey). Do not apply from this change.
    Existing structured table definitions remain the post-RECONCILED SoR.
  analytics_implications: >
    No analytical query against payload_enc. MIS / sold-policy reporting reads
    structured tables after materialize. Working rows of abandoned journeys
    are not a reporting source.
  alternatives_rejected:
    - "JSON on journey.journey — INV-JRN-02 / OR-01 / SC-W3-6"
    - "DynamoDB for quote/proposal working documents — DB-DEC-0001; no volume evidence"
    - "Journey materializes quotation/proposal rows — ownership defect; no cross-schema grant"
    - "Plaintext JSONB working column — DR-PII-01 / INV-PRP-05"
    - "2PC across journey + quotation + proposal — ADR-012 outbox; INV-IDM-01 retry"
    - "JSON for payment / policy / consent / suitability — those are money or evidence"
  required_actions:
    - "Human Aarti signs this pack + DB-DEC-0004 (design only; not S07-G5 close)"
    - "Mahesh joint-reviews orchestrator boundary and SoT timing"
    - "Deepali reviews payload_enc placement vs CMK plan"
    - "Amit implements repositories + Flyway at S09/S11 from the design scripts (parked)"
    - "Shivanshi consumes unchanged RPO/RTO; no new restore class"
  human_approval_required: true
  revisit_trigger: >
    A measured need to query inside the working envelope before payment;
    a second LOB whose offer shape cannot fit payload_schema_version;
    Journey service attempting a cross-schema write; restore drill misses RTO.
  evidence:
    - "docs/platform/data-architecture/06-journey-orchestrator-and-working-payload.md"
    - "docs/platform/data-architecture/schemas/08-quotation.sql"
    - "docs/platform/data-architecture/schemas/09-proposal.sql"
    - "docs/platform/data-architecture/schemas/12-journey.sql"
    - "docs/platform/data-architecture/schemas/90-routines.sql"
    - "docs/platform/data-architecture/schemas/91-grants.sql"
    - "docs/context/roles/mahesh-principal-insurance-platform-architect/13-orchestration-doctrine.md (OR-01)"
    - "docs/platform/ws3-platform/01-domain-model-and-invariants.md (INV-JRN-02)"
```

### Observations (non-blocking for design review)

1. **SoT timing, not ownership.** Structured `quote` / `offer` / `proposal` remain the sold-policy record. Working rows are a pre-payment envelope.
2. **Payment stays structured and independent.** It is on the journey (`PAYMENT_PENDING` → `PAYMENT_SETTLED`, `journey_ref.paymentId`) and is not a journey table. `app_journey` has no grant on `payment`. `RECONCILED` remains the issuance gate (`INV-POL-01`). This pack does not put money on JSON.
3. **Journey service is a skeleton.** This pack is the persistence contract. Implementing `journey-orchestration-service` is a later S11 item, not this change.
4. **Restore unproven.** Inherited from `DB-DEC-0001`. Severity stays `D1` for design.
