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

### Finding 1 — the audit trail was not durable · **RESOLVED 2026-08-14**

> **Status: fixed.** The Solution Architect directed on 2026-08-14 that audit events must be
> persisted. They now are — see §1.1 for what was built and what it means for **Q1**.
>
> The original finding is kept below because Compliance is still asked to rule on the period
> during which the trail was **not** durable, and on whether the current design is sufficient.

The design says `audit_event` is an "immutable compliance audit log". The table exists, the
persistence service exposes `POST /internal/v1/audit-events`, and the schema is sound.

**Nothing wrote to it.**

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
[registers](../../../governance/registers/RISK-REGISTER.md).

### 1.1 What was built, and what Compliance is now asked instead

Audit events are appended to `audit_event` over HTTP, alongside the existing log line.

| Aspect | Decision | Rationale |
|---|---|---|
| Transport now | **REST** to `POST /internal/v1/audit-events` | The endpoint and schema already existed; a synchronous in-VPC HTTP call adds ~1 hop and no new infrastructure |
| Transport later | Deliberately **undecided** | Selected by `insurance.audit.sinks`, so a Kafka sink is an added class plus a config value — no service code changes |
| Both sinks by default | `LOG,PERSISTENCE` | The log line is what an operator greps during an incident, and survives persistence being the broken thing. The persisted row is what an auditor reads two years later |
| On failure | **Best-effort** — logged, never thrown | A customer's proposal must not fail because an audit sink is down. The cost is silent evidence loss, which is why the log sink is retained |
| PII in metadata | **Masked on the way out** | The schema said metadata must not carry PII; that was a rule with no mechanism. Now `PiiMasker` runs on every value before transport |
| Unimplemented sink | **Fails startup** | Selecting `KAFKA` refuses to boot rather than accepting events and discarding them, mirroring the AWS Secrets Manager fail-fast (TD-006) |

Evidence: `AuditPersistenceIT` drives a real quote through the controller and asserts rows arrive
at the persistence endpoint, that a persistence outage does not fail the quote, and that no
plaintext date of birth reaches the trail.

**Q1 is therefore no longer "can the gate pass without persistence".** What remains for
Compliance:

1. Is the **best-effort** trade-off acceptable for an evidentiary record, or must audit writes be
   transactional with the business operation? Engineering's view: best-effort is right for
   Phase 4 — a hard dependency would let a persistence outage stop customer journeys — but this
   is a Compliance judgement, and the honest cost is that evidence can be lost silently.
2. Does the period during which the trail was **not** durable need remediation, disclosure, or a
   recorded acceptance? Engineering cannot determine how long any deployed instance ran in that
   state.

### Finding 2 — table-level immutability is documented, not enforced · **P2**

`V1__init_schema.sql` says `audit_event` is "INSERT-only for service account". There is no
`GRANT`/`REVOKE` in the migration and no separate role — the application connects with an owner
account that can `UPDATE` and `DELETE` freely. The immutability is a convention.

**What Compliance needs to decide:** whether database-enforced insert-only permissions are
required before production, and whether that is a Phase 4 or Phase 6 control.

### Finding 3 — audit coverage is incomplete · **P1** *(raised from P2 — ASM-008 invalidated)*

Two separate gaps, and the second was found only while implementing Finding 1.

**3a — raw payload capture.** Bodies are captured for **quote, proposal and payment**. They are
**not** captured for **status and master-data**, because those port signatures carry no `jobId`
to attach evidence to. Tracked as **TD-023**.

**3b — five declared audit actions are never emitted.** `AuditActions` defines thirteen actions;
the code emits eight. The compliance pack previously described the vocabulary as if all thirteen
were live, which was wrong:

| Action | Emitted? | Why it matters |
|---|---|---|
| `QUOTE_JOB_CREATED` | ❌ **never** | Overlaps `QUOTE_CREATED`; probably a redundant constant |
| `PROPOSAL_STATUS_UPDATED` | ❌ **never** | A proposal reaching a terminal state leaves no audit record |
| `PAYMENT_SESSION_CREATED` | ❌ **never** | The session **is** created and persisted — just not audited |
| `DOCUMENT_UPLOADED` | ❌ never | No document feature yet — legitimately unemitted |
| `PAYMENT_INTIMATION_SENT` | ❌ never | FUNC-008 not implemented (TD-022) — legitimately unemitted |

The first three are genuine coverage gaps in delivered functionality. The last two are ahead of
their features.

> **ASM-008 is INVALIDATED.** The Solution Architect ruled on 2026-08-14 that coverage must
> extend beyond quote/proposal/payment — *"there are many things right from the consent and
> other stuff… from the evidence point of view, we need that."* TD-023 is raised **P2 → P1** and
> pulled into Phase 4.
>
> **What remains for Compliance is not *whether* but *what*.** Engineering can enumerate what is
> auditable; only Compliance can say which events are evidentiary. The proposed list is §3.1.

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
**Compliance should confirm explicitly that recording-without-blocking is acceptable for UAT**
(Q4), because the alternative — hard-failing the submission — is a Phase 5 change that would
need to be pulled forward via change control.

### 3.1 Proposed coverage additions (ASM-008 invalidated — Q3)

The Architect ruled that coverage must extend beyond quote/proposal/payment. Engineering's
proposed list, for Compliance to confirm, add to, or trim. **Nothing here is implemented yet** —
it is TD-023 scope.

| Event | Today | Proposed | Evidentiary value |
|---|---|---|---|
| Proposal reaches a terminal state | ❌ not audited | Emit `PROPOSAL_STATUS_UPDATED` | Closes the loop on the contractual act |
| Payment session created | ❌ not audited | Emit `PAYMENT_SESSION_CREATED` | The session record exists but leaves no trail |
| Consent **captured** (not just missing) | only absence is recorded | New action — record consent *presence* and its reference | Proving consent was obtained is the point; recording only its absence is a strange asymmetry |
| Status check raw payload | ❌ not captured | Extend TD-023 | Customer-data access evidence |
| Master-data lookup raw payload | ❌ not captured | Extend TD-023 — or argue it is not evidentiary | Reference data, arguably not customer evidence |
| Idempotent replay | ❌ not audited | New action | Distinguishes a genuine retry from a duplicate submission in a dispute |
| Authorization denial | ❌ not audited | New action | "Who tried and was refused" is usually an audit requirement |
| `QUOTE_JOB_CREATED` | declared, never emitted | **Remove** — duplicates `QUOTE_CREATED` | Reduces a vocabulary that overstates coverage |

The last row matters beyond tidiness: a declared-but-unemitted action makes the audit vocabulary
look more complete than it is, which is how this pack came to describe five actions that never
fire.

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

Status as of **2026-08-14**, after the Solution Architect's direction. Three of the eight are
resolved or partly resolved; five still need a named human.

| # | Question | Status | Who decides |
|---|---|---|---|
| 1 | Audit events in logs only, or persisted? | ✅ **Answered — persist.** Built (§1.1). Two sub-questions remain: is *best-effort* acceptable for an evidentiary record, and does the non-durable period need remediation or disclosure? | Architect (done) → **Compliance** for the two sub-questions |
| 2 | Are database-enforced insert-only permissions required before production? | ⏳ **Referred to the DBA** — see [DBA-CONSULTATION.md](./DBA-CONSULTATION.md) | **DBA** → Compliance ratifies |
| 3 | Is coverage limited to quote/proposal/payment acceptable? | ✅ **Answered — no.** ASM-008 invalidated; TD-023 → P1. **What to add is still open** — proposed list in §3.1 | Architect (done) → **Compliance** confirms the list |
| 4 | Is recording (not rejecting) a missing `consentRef` acceptable for UAT? | ❌ **Open** | **Compliance** |
| 5 | Is the masking rule set sufficient? | ❌ **Open.** No regulation prescribes an algorithm — this needs the bank's data-protection policy. Evidence in [REGULATORY-RETENTION-FINDINGS.md §4](./REGULATORY-RETENTION-FINDINGS.md); the **PAN rule is the one to scrutinise** | **Compliance** |
| 6 | Is 7 years correct, and what retention applies elsewhere? | ⚠️ **Probably not.** IRDAI 2025 sets a **10-year** minimum on insurance records; PMLA sets 5; IRDAI cyber guidelines set a 180-day rolling window for *ICT logs*. **Which regime `audit_event` falls under is the open question** — see [findings §2](./REGULATORY-RETENTION-FINDINGS.md) | **Compliance** |
| 7 | Is best-effort raw-payload capture the right trade-off? | 🟡 **Provisionally yes** (Architect, 2026-08-14: *"Yes. It looks like most of it is correct, but get it reviewed by the compliance head."*) | **Compliance** confirms |
| 8 | Do `actor_id` / `agent_id` / `journey_id` constitute personal data? | ❌ **Open** — Architect explicitly referred this out | **Compliance** |

### 7.1 Two questions Engineering added

Both surfaced while implementing the above; neither was in the original eight.

| # | Question | Why |
|---|---|---|
| 9 | Should **business** audit events (proposal, payment, consent) and **transport** events (`ONESB_OUTBOUND_CALL`) carry different retention? | If yes, the schema needs a column to distinguish them — cheaper to settle before the table fills. [Findings §2](./REGULATORY-RETENTION-FINDINGS.md) |
| 10 | Where is audit and payload data physically stored? | Both IRDAI instruments require **India residency**. `render.yaml` pins no region and the AWS note names none. Nobody currently owns this. [Findings §3.2](./REGULATORY-RETENTION-FINDINGS.md) |

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
