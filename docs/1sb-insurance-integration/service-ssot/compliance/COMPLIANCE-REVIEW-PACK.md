# Compliance review pack — audit schema and log samples

**For:** WS-1 Phase 4 exit criterion **4.4** — "Compliance review of audit schema and log samples"
([04-STAGE_GATES.md §6](../../../governance/04-STAGE_GATES.md#6-project-gates-l3))
**Prepared by:** Engineering · **To be reviewed and signed by:** Risk & Compliance
**Evidence required by the gate:** a signed review note in `service-ssot/`
**Status:** ⏳ **Awaiting compliance review — not signed**

> **This pack is the input to the review, not the review.** Criterion 4.4 is met by a
> Compliance sign-off, and a Compliance verdict is a **mandatory human** verdict that no agent
> and no other role may substitute
> ([11-REVIEW_GATES §2](../../../governance/11-REVIEW_GATES.md#2-who-may-sit-on-a-board)).
> Everything below is assembled so that review can happen in one sitting, including the parts
> that are inconvenient.

---

## 1. Read this first — three findings that affect the decision

Engineering raises these rather than letting the review discover them. **Finding 1 is material
and may be a reason to withhold sign-off.**

### Finding 1 — the audit trail is not currently durable · **P1**

The design says `audit_event` is an "immutable compliance audit log". The table exists, the
persistence service exposes `POST /internal/v1/audit-events`, and the schema is sound.

**Nothing writes to it.**

The only `AuditEventPublisher` implementation wired into `1sb-integration-service` is
`LoggingAuditEventPublisher`, which writes structured lines to the **application log**. The
persistence audit API was documented under TD-021 for an *audit-consumer service* that was
explicitly scoped as "Phase 2+ / separate story" and does not exist.

So today the audit trail's durability, immutability, and retention are whatever the log
infrastructure provides — typically rotation measured in days or weeks, mutable, and not
designed to satisfy a 7-year evidentiary requirement.

| Property claimed | Actually provided today |
|---|---|
| Append-only, immutable | Application log — as mutable as the log store |
| 7-year retention | Log retention, whatever it is configured to be |
| Queryable by actor / resource / journey | Only by log search, if the log store indexes it |
| Survives a container restart | Only if logs ship off-host |

**What Compliance needs to decide:** whether a Phase 4 gate can pass with the audit trail in
this state, or whether persisting audit events is a prerequisite. Engineering's view: this is a
real gap, it is not difficult to close (an HTTP adapter against the existing endpoint, mirroring
`HttpRawPayloadStoreAdapter`), and it should be closed before UAT exposes real customer data.

Recorded as **RISK-012** and **SUG-20260813-a1c** — see
[registers](../../../governance/registers/RISK-REGISTER.md). It is deliberately **not** fixed in
the same change as this pack: implementing audit persistence is new scope and needs its own
triage, plan and review, not a quiet inclusion.

### Finding 2 — table-level immutability is documented, not enforced · **P2**

`V1__init_schema.sql` says `audit_event` is "INSERT-only for service account". There is no
`GRANT`/`REVOKE` in the migration and no separate role — the application connects with an owner
account that can `UPDATE` and `DELETE` freely. The immutability is a convention.

**What Compliance needs to decide:** whether database-enforced insert-only permissions are
required before production, and whether that is a Phase 4 or Phase 6 control.

### Finding 3 — raw payload capture is incomplete · **P2**

Raw 1SB request/response bodies are captured for **quote, proposal and payment**. They are
**not** captured for **status and master-data** calls, because those port signatures carry no
`jobId` to attach evidence to. Tracked as **TD-023**.

This is the subject of **ASM-008** — the standing assumption that Compliance will accept audit
coverage limited to the quote/proposal/payment paths. **This review is where that assumption is
validated or invalidated.** If invalidated, TD-023 becomes P1 COMP work inside Phase 4.

---

## 2. The audit event schema

Table `audit_event` in `bank-persistence-service` (`V1__init_schema.sql`). Emitted shape is
`com.bank.common.audit.AuditEvent`.

| Column | Type | Null? | Purpose | PII? |
|---|---|---|---|---|
| `event_id` | VARCHAR(36) PK | no | Idempotent identity; UUID if not supplied | no |
| `event_time` | TIMESTAMPTZ | no | When the action occurred, UTC | no |
| `action` | VARCHAR(100) | no | Controlled vocabulary — §3 | no |
| `actor_id` | VARCHAR(100) | no | Who acted: employee id, or a service name | **indirect** |
| `actor_type` | VARCHAR(20) | no | `USER` / `SERVICE` / `SYSTEM` | no |
| `resource_type` | VARCHAR(50) | no | What was acted on (`QUOTE_JOB`, `APPLICATION`, …) | no |
| `resource_id` | VARCHAR(100) | no | Which instance | no |
| `outcome` | VARCHAR(20) | no | `SUCCESS` / `FAILURE` | no |
| `lob` | VARCHAR(20) | yes | Line of business | no |
| `journey_id` | VARCHAR(36) | yes | Correlates one customer journey end to end | **indirect** |
| `distributor_id` | VARCHAR(50) | yes | Distributor identity, injected from secrets | no |
| `agent_id` | VARCHAR(50) | yes | Selling agent — attribution evidence | **indirect** |
| `trace_id` | VARCHAR(64) | yes | Correlates to application logs | no |
| `metadata` | TEXT (JSONB in Postgres) | yes | Action-specific fields — §4 | **must not be** |

Indexes: `(resource_type, resource_id, event_time)`, `(actor_id, event_time)`,
`(journey_id, event_time)` — i.e. the three questions an auditor asks: what happened to this
application, what did this employee do, and what happened in this customer's journey.

**Note the "indirect" entries.** No direct identifier (name, PAN, mobile, email, DOB) is stored,
but `actor_id`, `agent_id` and `journey_id` identify people and sessions by reference. Whether
that constitutes personal data under the applicable regime is a Compliance determination, and it
affects the retention answer in §6.

---

## 3. Audit action vocabulary

From `com.bank.common.audit.AuditActions`. Changing any value is a breaking contract change.

| Action | Emitted when | Compliance relevance |
|---|---|---|
| `QUOTE_JOB_CREATED` | A quote request is accepted | Start of a journey |
| `QUOTE_CREATED` | Quote submitted upstream | — |
| `QUOTE_COMPLETED` | Offers received; carries `offerCount`, `partialErrorCount` | What the customer was shown |
| `PROPOSAL_SUBMITTED` | Proposal sent to the insurer | **The contractual act** |
| `PROPOSAL_STATUS_UPDATED` | Proposal status changed | — |
| `CONSENT_REF_MISSING` | Proposal submitted **without** a consent reference | **Consent evidence — see below** |
| `PAYMENT_URL_RETRIEVED` | Payment URL obtained | Money movement initiated |
| `PAYMENT_SESSION_CREATED` | Payment session persisted | — |
| `PAYMENT_INTIMATION_SENT` | Payment confirmed to the insurer | **Not implemented — TD-022 / FUNC-008** |
| `APPLICATION_STATUS_CHECKED` | Status read for an application | Customer-data access |
| `DOCUMENT_UPLOADED` | Document uploaded | — |
| `ONESB_OUTBOUND_CALL` | **Every** outbound 1SB call | Complete upstream call record |
| `PAYLOAD_RETENTION_DELETED` | A raw payload passed its retention date | Deletion evidence |

**`CONSENT_REF_MISSING` is a soft gate.** A proposal without a `consentRef` is **allowed** and
recorded, not rejected. Making consent mandatory is currently out of scope until Phase 5.5
([CURRENT-STATE.yaml](../../../governance/state/CURRENT-STATE.yaml) `out_of_scope`).
**Compliance should confirm explicitly that recording-without-blocking is acceptable for UAT**,
because the alternative — hard-failing the submission — is a Phase 5 change that would need to
be pulled forward via change control.

---

## 4. PII handling

**Standing constraint: no PII in logs**
([CURRENT-STATE.yaml](../../../governance/state/CURRENT-STATE.yaml) `standing_constraints`).

### What is never recorded in an audit event or a log

Name, date of birth, PAN, mobile number, email address — in any field, and in free text.

### How it is enforced

`com.bank.insurance.onesb.observability.PiiMasker` applies two layers:

1. **Field-name matching** — known PII field names are masked by type, across naming
   conventions (`firstName`, `first_name`, `customerName`, …), recursively through nested JSON.
2. **Pattern matching** — PAN, Indian mobile, email and ISO dates are masked wherever they
   appear, including inside free-text strings such as upstream error messages. This is the layer
   that catches PII arriving in a field nobody anticipated.

| Value | Masked as | Note |
|---|---|---|
| `Asha` | `A***` | First initial only |
| `9876543210` | `******3210` | Last 4 retained for support lookups |
| `asha.rao@example.com` | `a***@e***.com` | Local part and domain both reduced |
| `ABCDE1234F` | `*****1234F` | Leading letters become `*` so the result cannot itself match a PAN pattern |
| `1990-01-15` | `****-**-**` | Removed entirely |

### Outbound call bodies

`ONESB_OUTBOUND_CALL` records `requestHash` — SHA-256 of the **masked** body, never the
plaintext. Two identical submissions are provably identical without either being readable. This
is asserted by `OneSbHttpClientTest` and `OneSbHttpClientHashMaskedBodyTest` across every body
shape (POJO, pre-serialised JSON, non-JSON text, null).

### Log samples

**[audit-log-samples.md](./audit-log-samples.md)** — a full Term journey as it appears in the
log, plus before/after masking of a realistic proposal body.

Those samples are **generated by the code, not written by hand** (`AuditLogSampleTest`), and the
committed file is compared against freshly generated output on every build. If masking behaviour
changes, the build fails and this pack must be re-reviewed. A hand-written sample would have let
the document and the behaviour drift apart silently.

---

## 5. Raw payload capture (COMP-003)

Full 1SB request and response bodies are retained as dispute and audit evidence — the one place
where **unmasked** customer data is deliberately stored.

| Property | Implementation |
|---|---|
| Storage | `raw_payload` table, `bank-persistence-service` |
| Encryption | **AES-256-GCM**, fresh random 96-bit IV per row, stored `IV ‖ ciphertext ‖ tag` |
| Key | `RAW_PAYLOAD_ENCRYPTION_KEY`, base64, exactly 32 bytes; service **refuses to start** without a valid key |
| Key identity | `encryption_key_id` per row (`raw-payload-key-v1`) so rotation is traceable |
| Retention | `retain_until` per row; default **7 years**, configurable (`raw-payload.retention.years`) |
| Deletion evidence | `PAYLOAD_RETENTION_DELETED` audit action |
| Capture failure | Best-effort by design — never fails the customer's transaction (NFR-002) |

### Gaps Compliance should weigh

- **Coverage** — quote, proposal, payment only. Status and master-data are not captured
  (Finding 3, TD-023, ASM-008).
- **No retention job exists.** `retain_until` is written on every row; nothing deletes expired
  rows yet. Retention jobs are Phase 6 scope. Today the field is a promise, not a mechanism.
- **No key rotation path.** Rows are encrypted with the key active when written, and there is no
  re-encryption job or key-version fallback. Rotating the key makes existing payloads
  undecryptable — documented in [OPERATIONS-RUNBOOK §2.5](../OPERATIONS-RUNBOOK.md) as a
  change-control item rather than an ops action.
- **Best-effort capture means gaps are possible.** If persistence is unavailable, the
  transaction proceeds and the evidence is lost, silently. That trade-off favours the customer
  over the evidence trail; Compliance should confirm it is the right way round.

---

## 6. Retention

| Data | Retention | Mechanism | State |
|---|---|---|---|
| `raw_payload` | 7 years default, configurable | `retain_until` per row | Field written; **no deletion job** (Phase 6) |
| `audit_event` | Not defined | — | **Not persisted at all today** (Finding 1) |
| Application logs | Whatever the log platform provides | Log platform | Outside this repository |
| `integration_job`, offers, poll attempts | Not defined | — | No policy set |

**Compliance is asked to state the required retention period for each row above**, so it can be
implemented rather than assumed. Only the raw-payload figure currently has a number, and it was
an engineering default (7 years), not a compliance instruction.

---

## 7. What the reviewer is asked to decide

| # | Question | Bears on |
|---|---|---|
| 1 | Can Phase 4 pass with audit events in application logs only, or must they be persisted first? | **Finding 1 · gate-blocking** |
| 2 | Are database-enforced insert-only permissions required before production? | Finding 2 |
| 3 | Is audit coverage limited to quote/proposal/payment acceptable? | Finding 3 · **validates or invalidates ASM-008** |
| 4 | Is recording a missing `consentRef` (rather than rejecting the proposal) acceptable for UAT? | §3 |
| 5 | Is the masking rule set sufficient — particularly retaining the last 4 mobile digits and last 5 PAN characters? | §4 |
| 6 | Is 7 years correct for raw payloads, and what retention applies to audit events and job records? | §6 |
| 7 | Is best-effort raw-payload capture (customer transaction over evidence) the right trade-off? | §5 |
| 8 | Does storing `actor_id` / `agent_id` / `journey_id` constitute personal data under the applicable regime? | §2 |

---

## 8. Sign-off

Criterion 4.4 is met when this section is completed by Risk & Compliance. An agent may not
complete it, and neither may Engineering or the Architect
([11 §2](../../../governance/11-REVIEW_GATES.md#2-who-may-sit-on-a-board) — Security and Risk &
Compliance verdicts at T4 require a human, with no aggregate override).

```text
reviewer:        __________________________  (Risk & Compliance)
date:            __________________________
verdict:         APPROVED / APPROVED WITH CONDITIONS / REWORK

answers to the eight questions in section 7:
  1. ______________________________________________________________
  2. ______________________________________________________________
  3. ______________________________________________________________
  4. ______________________________________________________________
  5. ______________________________________________________________
  6. ______________________________________________________________
  7. ______________________________________________________________
  8. ______________________________________________________________

conditions carried forward as acceptance criteria (11 section 13):
  ________________________________________________________________

ASM-008 (audit coverage limited to quote/proposal/payment):
  VALIDATED / INVALIDATED
```

A verdict of APPROVED WITH CONDITIONS requires those conditions to be carried forward as
acceptance criteria before the gate may close.

---

## 9. Supporting material

| Item | Where |
|---|---|
| Generated log samples | [audit-log-samples.md](./audit-log-samples.md) |
| Audit schema DDL | `services/bank-persistence-service/src/main/resources/db/migration/V1__init_schema.sql` |
| Audit event model | `libs/bank-common-audit/.../AuditEvent.java`, `AuditActions.java` |
| Masking implementation | `.../observability/PiiMasker.java`, tested by `PiiMaskerTest` |
| Encryption implementation | `services/bank-persistence-service/.../crypto/RawPayloadEncryptionService.java` |
| Incident handling | [OPERATIONS-RUNBOOK.md](../OPERATIONS-RUNBOOK.md) |
| Open assumption | ASM-008, [assumption register](../../../governance/registers/ASSUMPTION-REGISTER.md) |
| Raised findings | RISK-012, [risk register](../../../governance/registers/RISK-REGISTER.md) |
