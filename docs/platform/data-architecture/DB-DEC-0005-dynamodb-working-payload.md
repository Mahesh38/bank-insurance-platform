# DB-DEC-0005 — DynamoDB for quote and proposal working state until payment

**Supersedes in part:** [`DB-DEC-0004`](./DB-DEC-0004-working-payload-until-payment.md) (the *store* of the
working envelope only — the JSON-until-payment pattern, ownership and materialisation trigger are
unchanged) and the "R0 Dynamo access list is empty" ruling of
[`04-store-segregation-and-roadmap.md` §3.2](./04-store-segregation-and-roadmap.md#32-dynamodb--r0-access-list-amended-by-db-dec-0005)
for exactly two tables.
**Origin:** [`SUG-20261021-ddb`](../../governance/registers/SUGGESTION-REGISTER.md#sug-20261021-ddb--dynamodb-for-quote-and-proposal-working-state) — owner override, `ADMIT-BYPASS`.
**Design detail:** [`schemas/20-dynamodb-working-payload.md`](./schemas/20-dynamodb-working-payload.md).

## Ratification

| Role | Who | Type | Verdict | Date |
|---|---|---|---|---|
| Repository owner / Architecture | Mahesh | `reviewer_type: HUMAN` · `authority: repository_owner` | **APPROVED** — "make use of DynamoDB where it is required, specially in the quote and proposals before payment; once payment is done all this are moved to Postgres in structured format rather than JSON" | 2026-10-21 |
| Database (Aarti persona) | agent | `AGENT` | `APPROVED_WITH_OBSERVATIONS` draft (below) | 2026-10-21 |
| Security (Deepali persona) | agent | `AGENT` | Review of IAM / KMS / VPC-endpoint surface **outstanding** | — |
| Compliance (Shailja persona) | agent | `AGENT` | Retention of abandoned working items (TTL) vs evidence classes **outstanding** | — |

The owner ratification is the human approval this repository requires. It does not create
bank InfoSec approval for a new AWS data service in the landing zone, an RIA, or IAM sign-off —
those are external and still have to be obtained (S09).

```yaml
database_decision:
  id: DB-DEC-0005
  subject: "Quote and proposal working state in DynamoDB before payment; structured Aurora PostgreSQL after RECONCILED"
  stage: "S08 design delta / S09 provision / S11 owning-service runtime"
  accountable_database_authority: "Aarti — Principal Insurance Data & Database Architect / DBA (AGENT draft)"
  architecture_owner: "Mahesh — repository owner, HUMAN ratification 2026-10-21"
  engineering_owner: "Amit — quotation-service and proposal-service repositories at S11"
  security_reviewer: "Deepali — per-service IAM, KMS CMK, client-side envelope encryption, VPC gateway endpoint"
  compliance_reviewer: "Shailja — TTL ageing of never-paid working items (RET-WORKING)"
  decision: >
    quotation-service and proposal-service each own ONE DynamoDB table per
    environment (quotation-working, proposal-working) in ap-south-1. Every
    pre-payment write of a quote (request, all insurer offers, selection) and of a
    proposal (form answers, nominee, UW requirements, counter-offer) goes to that
    table as one encrypted envelope item plus plaintext control attributes needed
    by gates and by Payment. Idempotency and outbox items live in the same table
    and are written in the same TransactWriteItems call as the business change.
    On PaymentReconciled the owning service reads the item, INSERTs the existing
    structured rows (quotation.quote + offer; proposal.proposal + uw_requirement +
    uw_document_ref) in ONE Aurora transaction with ON CONFLICT DO NOTHING, then
    marks the item materialized and sets a short TTL. Aurora is the record from
    that moment. Never-paid items expire by DynamoDB TTL (RET-WORKING).
    Everything else stays where it was: journey, payment, policy, consent,
    suitability, lead, customer, catalogue, identity, administration and audit
    remain Aurora PostgreSQL. Sessions remain Valkey.
  status: APPROVED   # HUMAN owner; AGENT DBA verdict APPROVED_WITH_OBSERVATIONS
  database_severity: D1
  rationale: >
    Owner direction. Technical fit: the pre-payment quote/proposal is a
    single-key, document-shaped, high-churn object (re-quotes, polling updates,
    page-by-page form saves) that is read by id, never joined, and mostly
    discarded. That access pattern suits a key-value store and keeps the
    working-set churn and TOAST bloat off the shared Aurora cluster. Gates,
    money and evidence stay relational: suitability, consent, payment and the
    sold-policy quote/proposal rows keep FK/CHECK/uniqueness guarantees in Aurora.
  data_ownership: >
    Unchanged: one writer per store. quotation-service is the only principal on
    quotation-working and the only writer of schema quotation; proposal-service
    likewise for proposal-working and schema proposal. Payment reads selected_*
    only through the Quotation API. Journey has no DynamoDB grant.
  integrity_controls:
    - "Conditional writes on version (optimistic lock) for every update"
    - "One open quote / proposal per journey: marker item JOURNEY#id + OPEN_QUOTE / OPEN_PROPOSAL, attribute_not_exists in the same transaction"
    - "Unique application number: marker item APPNO#n, attribute_not_exists"
    - "Atomic selection: selected_* attributes and payloadEnc updated in one UpdateItem / transaction (INV-QUO-05)"
    - "Frozen after materialize: every update carries ConditionExpression attribute_not_exists(materializedAt)"
    - "Structured Aurora rows INSERT-only (unchanged grants); ON CONFLICT (pk) DO NOTHING makes materialize idempotent"
    - "Suitability and consent ids checked BEFORE the first put (C1 / C2) — unchanged"
  transaction_consistency: >
    ACID inside one DynamoDB table via TransactWriteItems (business item + IDEM
    item + OUTBOX item + marker items, <= 100 items, 4 MB). Strongly consistent
    reads for gates, selection and materialize. No transaction spans DynamoDB and
    Aurora: materialize commits Aurora first (the record), then marks the Dynamo
    item; a crash between the two is repaired by re-delivery (idempotent) and by
    the daily reconciled-but-not-materialized sweep. No 2PC.
  performance_implications: >
    On-demand capacity at R0 (one assisted sale at a time). Item limit 400 KB:
    envelope budget 350 KB; documents and anything larger go to S3 with a
    reference. Hot partitions are impossible at R0 volume (key = ULID).
  security_lifecycle_implications: >
    Table SSE with a customer-managed KMS key per table (India). payloadEnc is
    additionally client-side encrypted (envelope encryption, per-service KMS key)
    so table-level read access does not reveal members, nominee or form PII.
    IRSA role per service scoped to its own table ARN and GSIs; no Scan; no
    dynamodb:* wildcard. Access only through a VPC gateway endpoint whose policy
    allows only these tables. Streams consumer role is the service's own.
  availability_recovery_implications: >
    PITR on (35 days) + on-demand backup before each release. Optional global
    table replica in ap-south-2 for DR (India only) — Shivanshi decides at S09.
    Loss of an un-paid working item costs a re-quote, not money or evidence.
    A paid item is protected: TTL is removed while a payment is in flight.
  migration_implications: >
    Remove quote_working / proposal_working from the Aurora design DDL and their
    freeze trigger, purge clauses and grants. Structured tables unchanged.
    Provision tables via Terraform at S09; local dev uses DynamoDB Local.
  analytics_implications: >
    No analytics on DynamoDB. MIS reads structured Aurora rows after materialize
    (or events). Never-paid quotes are visible to MIS only through events
    (QuoteRequested / QuoteCompleted), as before.
  alternatives_rejected:
    - "Aurora BYTEA working envelope (DB-DEC-0004) — superseded by owner decision"
    - "DynamoDB for journey / payment / audit — not requested; those are stage, money and evidence and stay relational"
    - "Dual-write Dynamo + Aurora in one request — no shared transaction; replaced by event-driven materialize"
    - "Plaintext JSON attributes — PII would be readable by anyone with table read (DR-PII-01)"
    - "Keeping the Dynamo item as the sold-policy record — structured Aurora rows are the record after RECONCILED"
  required_actions:
    - "Deepali: IAM policy, KMS key policy, VPC endpoint policy review (S09)"
    - "Shailja: confirm RET-WORKING TTL (default 90 days) for never-paid working items"
    - "Shivanshi: Terraform tables, PITR, alarms; decide global-table DR"
    - "Amit: DynamoDB repositories + materialize consumer + reconciliation sweep at S11; Testcontainers/DynamoDB Local tests"
    - "Bank cloud team: confirm DynamoDB is permitted in the AU landing zone SCPs (it is listed as AWS PaaS, not a bank 'standard DB')"
  human_approval_required: true   # satisfied by Mahesh 2026-10-21 for the design; external approvals above remain
  revisit_trigger: >
    A need to query inside working envelopes; envelope size approaching 350 KB;
    bank InfoSec refusal of DynamoDB in the landing zone; materialize lag breaching
    the reconciliation SLA.
  evidence:
    - "docs/platform/data-architecture/schemas/20-dynamodb-working-payload.md"
    - "docs/platform/data-architecture/06-journey-orchestrator-and-working-payload.md"
    - "docs/platform/data-architecture/schemas/08-quotation.sql"
    - "docs/platform/data-architecture/schemas/09-proposal.sql"
```

## Observations (AGENT DBA, non-blocking)

1. **This is a second store engine, and that has a cost.** Two backup models, two IAM surfaces,
   two sets of alarms, and a materialisation step that can lag. The design keeps the blast radius
   to two tables owned by two services, and keeps every gate, every rupee and every piece of
   evidence relational.
2. **The TTL is the dangerous attribute.** A TTL that fires while a payment is in flight would
   delete the quote the money is for. The design removes `ttl` when Payment reads the selected
   offer and only restores it when the payment ends without `RECONCILED`.
3. **Aurora wins every disagreement.** Once a structured row exists, the DynamoDB item is
   bookkeeping. Support and MIS must never read DynamoDB to answer "what was sold".
