# Consent Rule Pack v1 — AU Bank Insurance Distribution Platform

**Pack ID:** `CONSENT-PACK-v1.0`
**Closes:** [GAP-006 — consent rules not executable](../po-drive/02-GAP-REGISTER.md) (P0, build-freeze)
**Satisfies:** [S02-E03](../../application-lifecycle-bible/stages/S02-regulatory-framing.md#3-epics-and-stories) · gate criterion S02-G3
**Author:** Rajal — Principal Insurance Platform Product Owner (Board 3 / R1)
**Date:** 2026-08-16
**Scope of this version:** R0 — RM-assisted Term Life, ETB customers, Group A insurers via 1SB

> **Authority boundary.** Product owns the *business behaviour* of consent: which events require
> it, what the journey does when it is absent, what the RM sees, what the evidence record must
> carry for the business to be defensible. **Regulatory permissibility of the statement wording,
> the sequencing, and the retention basis is Shailja's (Board 6) to ratify.** This pack is drafted
> to be ratified, not to substitute for ratification. Every rule below is written so a Compliance
> reviewer can accept, amend or reject it individually rather than reject the pack wholesale.
>
> **Signature status:** `AI-DRAFTED — mandatory human signature outstanding`. S02-G3 requires an
> E2 artefact signed by Compliance. This document is E1 until Shailja signs it.

---

## 1. How to read a rule

Every rule has an ID, a normative statement, and a **pass/fail test** that a tester can execute
without asking a question. A rule with no executable test is not in this pack.

| Field | Meaning |
|---|---|
| **Rule** | Normative statement. MUST / MUST NOT / MAY per RFC 2119 sense |
| **Test** | The observable condition that distinguishes pass from fail |
| **Enforced at** | Where the control lives: API, data store, UI, or process |
| **Waivable** | Whether any authority may waive it. Consent capture where legally required is on the [non-waivable list](../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#8-waivers-and-exceptions) |

---

## 2. Consent events in R0

Five consent events. Each has a code, a journey position, and a blocking effect. The journey
position is the *earliest* point at which the consent must exist; it may be captured earlier.

| Code | Consent event | Captured before | Blocks if absent | R0 |
|---|---|---|---|---|
| `CNS-DP` | Data processing & CBS/CIF prefill | Any customer data is read from CBS into the journey | Customer search returns identity only; no prefill, no lead progression | ✅ |
| `CNS-SOL` | Insurance solicitation, need analysis & suitability | Suitability questionnaire is presented | Suitability evaluation cannot be created | ✅ |
| `CNS-SHR` | Data sharing with the selected insurer via 1SB | Proposal submission to the insurer | Proposal submit rejected | ✅ |
| `CNS-COM` | Servicing communications (SMS / email / push) | First outbound customer communication | Communication suppressed; journey continues | ✅ |
| `CNS-RDR` | Controlled redirect to a Group B insurer | Redirect link is generated | Redirect refused | ⛔ R1 — Group B is out of R0 |

**Product position on consolidation.** `CNS-DP`, `CNS-SOL` and `CNS-COM` are presented to the
customer as **one acknowledgement screen with three separately-recorded consents**. The customer
sees one interaction; the evidence store holds three rows with three statement IDs. This gives the
UX benefit without the compliance risk of a bundled consent, and it is the position I ask
Compliance to ratify. `CNS-SHR` is always captured separately, because the counterparty is named
in the statement and the customer must see which insurer their data goes to.

> **Open for Compliance (OPEN-CNS-01):** whether `CNS-DP` and `CNS-SOL` may share a single
> acknowledgement action at all, or whether each requires its own affirmative action. If Compliance
> requires separation, the change is UI-only — the data model already records them separately.

---

## 3. Statement content and versioning

### 3.1 Statement identity

Every consent statement is identified by a **statement ID** that never changes meaning:

```
<CONSENT_CODE>-<LANGUAGE>-v<MAJOR>.<MINOR>
e.g. CNS-DP-en-IN-v1.0
```

| Rule | Statement | Test | Enforced at |
|---|---|---|---|
| **CNS-R01** | Every captured consent MUST record the statement ID, the statement version, and a SHA-256 hash of the exact statement text rendered to the customer. | Retrieve any consent record. All three fields present and non-null; recomputing SHA-256 over the stored text reproduces the stored hash. | Data store |
| **CNS-R02** | Statement text MUST be immutable once a version is published. A wording change MUST mint a new version. | Attempt to update the text of a published statement version. Rejected with `STATEMENT_VERSION_IMMUTABLE`. | Config store + API |
| **CNS-R03** | A MAJOR version increment signals a **material** change of meaning; a MINOR increment signals a non-material change (typo, formatting, translation correction). Only Compliance may classify the increment. | Every published version carries `changeClass ∈ {MAJOR, MINOR}` and `classifiedBy` naming a Compliance approver. Absent either field, publication is rejected. | Config store |
| **CNS-R04** | The statement text stored with the evidence record MUST be the text actually rendered, not a reference resolved at read time. | Publish a new version; retrieve a consent captured under the previous version. The previous text is returned verbatim. | Data store |

**CNS-R04 is the rule that makes the evidence defensible.** A record that says "customer accepted
statement v1.0" and then resolves v1.0 from a mutable table proves nothing three years later.

### 3.2 Version change and in-flight journeys

| Rule | Statement | Test | Enforced at |
|---|---|---|---|
| **CNS-R05** | A MAJOR version change MUST invalidate consents captured under prior versions **for journeys that have not yet passed the consent's blocking point**. Journeys past that point continue under the version they captured. | Publish `CNS-SOL-en-IN-v2.0` (MAJOR). A journey at Lead stage is blocked and re-prompted. A journey already at Proposal stage is not disturbed and retains its v1.0 evidence. | Journey orchestration |
| **CNS-R06** | A MINOR version change MUST NOT invalidate any existing consent. | Publish a MINOR version. Zero journeys are re-prompted; zero consent records change state. | Journey orchestration |
| **CNS-R07** | The RM MUST be shown why a re-consent is required, in business language, naming the consent event. | Trigger CNS-R05. RM sees a message identifying the consent event and the action required. No generic error. | UI |

### 3.3 Language

R0 ships `en-IN` only. `hi-IN` is R1.

| Rule | Statement | Test | Enforced at |
|---|---|---|---|
| **CNS-R08** | The language of the rendered statement MUST be recorded on the evidence record. | Every consent record has a non-null `language` matching a published statement version. | Data store |
| **CNS-R09** | A language variant MUST carry the same version number as its source-language statement and MUST be Compliance-approved independently. | `CNS-DP-hi-IN-v1.0` exists only if `CNS-DP-en-IN-v1.0` exists and the Hindi variant has its own `classifiedBy` Compliance approver. | Config store |

---

## 4. Capture mechanism by channel

The evidence captured MUST be identical across channels. The interaction differs; the record does
not. This is [S02-E03-S03](../../application-lifecycle-bible/stages/S02-regulatory-framing.md).

| Channel | Interaction | Acknowledgement method code |
|---|---|---|
| RM-assisted | Statement displayed on the RM device **and** sent to the customer's registered mobile; customer enters the OTP delivered to their own device | `OTP_SMS_CUSTOMER_DEVICE` |
| Customer self-service (R1) | In-app affirmative action (not a pre-ticked box) plus OTP to the registered mobile | `IN_APP_AFFIRMATIVE_PLUS_OTP` |
| Hybrid (R1) | Whichever device the customer is on at the moment of capture; recorded as such | as above |

| Rule | Statement | Test | Enforced at |
|---|---|---|---|
| **CNS-R10** | Consent MUST NOT be recorded without a verified OTP transaction. An RM tick alone is never consent. | Submit a consent capture with no `otpTransactionId`. Rejected `CONSENT_OTP_REQUIRED`. Submit with an unverified OTP txn. Rejected `CONSENT_OTP_NOT_VERIFIED`. | API |
| **CNS-R11** | The OTP MUST be delivered to the customer's CBS-registered mobile number, never to a number entered in the journey. | Alter the mobile number in the journey payload; OTP still dispatches to the CBS number of record. Assert the dispatch target equals the CBS value. | API |
| **CNS-R12** | The consent acknowledgement action MUST NOT be pre-selected or defaulted to accepted. | Render the consent screen. The affirmative control is in the un-actioned state on first paint. | UI |
| **CNS-R13** | The RM MUST NOT be able to enter the OTP on the RM device on the customer's behalf where the OTP was delivered to the customer device. | The RM surface exposes no OTP entry field for customer-device OTPs; it shows pending/verified status only. | UI + API |
| **CNS-R14** | OTP validity window is **10 minutes**; expiry requires re-issue, not retry. | Verify at T+9m59s → accepted. Verify at T+10m01s → rejected `OTP_EXPIRED`. | API |
| **CNS-R15** | Maximum **3** OTP verification attempts per transaction, then the transaction is locked and a new OTP must be issued. Maximum **5** OTP issues per consent event per 24 hours. | 4th verify attempt → `OTP_ATTEMPTS_EXCEEDED`. 6th issue in 24h → `OTP_ISSUE_RATE_EXCEEDED`. | API |

> **Rationale for CNS-R13.** This is the consent analogue of the RBI payment-device rule. If an RM
> can complete the customer's acknowledgement, the evidence records an act the customer did not
> perform, and every downstream compliance claim rests on it.

---

## 5. The consent evidence record

This is the schema the Consent Service (bounded context #6) must persist. Field names are business
names; physical naming is Aarti's to determine.

| # | Field | Type | Optionality | Source | Classification |
|---|---|---|---|---|---|
| 1 | `consentId` | UUID | Mandatory | System-generated | Internal |
| 2 | `journeyId` | UUID | Mandatory | Journey orchestration | Internal |
| 3 | `leadId` | String | Mandatory | Lead service | Internal |
| 4 | `customerCif` | String | Mandatory | CBS | **Restricted (PII)** |
| 5 | `consentCode` | Enum | Mandatory | §2 | Internal |
| 6 | `statementId` | String | Mandatory | §3.1 | Internal |
| 7 | `statementVersion` | String | Mandatory | §3.1 | Internal |
| 8 | `statementText` | Text | Mandatory | Rendered text, stored verbatim | Internal |
| 9 | `statementTextHash` | SHA-256 hex | Mandatory | Computed at capture | Internal |
| 10 | `language` | BCP-47 | Mandatory | Render context | Internal |
| 11 | `channel` | Enum `ASSISTED\|DIY\|HYBRID` | Mandatory | Session | Internal |
| 12 | `acknowledgementMethod` | Enum | Mandatory | §4 | Internal |
| 13 | `otpTransactionId` | String | Mandatory | Notification service | Confidential |
| 14 | `otpVerifiedAt` | Timestamp (UTC, ISO-8601 + offset) | Mandatory | OTP verification | Internal |
| 15 | `capturedAt` | Timestamp (UTC, ISO-8601 + offset) | Mandatory | Server clock, NTP-synced | Internal |
| 16 | `sourceIpAddress` | String | Mandatory | Request context | Confidential |
| 17 | `capturedByActorId` | String | Mandatory (assisted) / Null (DIY) | Identity service | Internal |
| 18 | `capturedByAgentId` | String (SP licence) | Mandatory (assisted) | Identity service | Confidential |
| 19 | `distributorId` | String | Mandatory | **Server-side secret only** | Confidential |
| 20 | `validFrom` | Timestamp | Mandatory | = `otpVerifiedAt` | Internal |
| 21 | `validUntil` | Timestamp | Mandatory | §6 | Internal |
| 22 | `status` | Enum `ACTIVE\|EXPIRED\|WITHDRAWN\|SUPERSEDED` | Mandatory | §6, §7 | Internal |
| 23 | `supersedesConsentId` | UUID | Optional | Re-consent chain | Internal |
| 24 | `recordVersion` | Integer | Mandatory | Append-only sequence | Internal |

### 5.1 Immutability

| Rule | Statement | Test | Enforced at |
|---|---|---|---|
| **CNS-R16** | The consent store MUST be **append-only**. No `UPDATE` and no `DELETE` on a consent record, by any application identity. | Issue an UPDATE and a DELETE against a consent row using the application's database role. Both fail on privilege, not on application logic. | Data store (grants) |
| **CNS-R17** | A state change (expiry, withdrawal, supersession) MUST be represented as a **new row** referencing the prior `consentId`, never as a mutation. | Withdraw a consent. Two rows exist: the original with `status` unchanged at its own `recordVersion`, and a new row recording the withdrawal. | Data store |
| **CNS-R18** | The current status of a consent MUST be derivable by reading the append-only chain, with no ambiguity about which row is current. | For any `consentId`, exactly one row is the head of its chain. Assert uniqueness over `(consentId, MAX(recordVersion))`. | Data store |
| **CNS-R19** | Consent evidence MUST be retrievable by `journeyId`, by `customerCif`, and by `consentId`, returning the full chain. | Three retrieval tests, each returning every row in the chain in `recordVersion` order. | API |
| **CNS-R20** | No consent field classified Restricted or Confidential MUST appear in application logs. | Run the full test suite; scan emitted logs for CIF, OTP txn ID, IP and SP licence patterns. Zero matches. | Logging framework + [S08-VT-06](../../application-lifecycle-bible/stages/S08-engineering-foundation.md#4-validation-tests) |

> **CNS-R16 is enforced by database grant, not by code.** An append-only rule that lives in a
> service is an append-only rule that one hotfix removes. This is a hard requirement on Aarti's
> physical design and a condition on the S09 gate.

---

## 6. Validity, expiry and reuse

| Rule | Statement | Test | Enforced at |
|---|---|---|---|
| **CNS-R21** | `CNS-DP` and `CNS-SOL` are valid for **90 days** from `validFrom`. | Consent at T+89d → journey proceeds. At T+91d → blocked `CONSENT_EXPIRED`. | API |
| **CNS-R22** | `CNS-SHR` is valid for **30 days** and is scoped to a **named insurer**. Sharing with a second insurer requires a second `CNS-SHR`. | Capture `CNS-SHR` for insurer A; submit a proposal to insurer B → rejected `CONSENT_SCOPE_MISMATCH`. | API |
| **CNS-R23** | `CNS-COM` has no expiry; it persists until withdrawn. | Consent at T+400d still permits communication. | API |
| **CNS-R24** | Consent MUST NOT be reused across `customerCif`. A consent is bound to one CIF for its lifetime. | Present a valid consent with a different CIF in the request → rejected `CONSENT_SUBJECT_MISMATCH`. | API |
| **CNS-R25** | Consent MAY be reused across journeys for the same CIF within its validity window, for the same `consentCode` and scope. | Abandon journey 1 at T+0; start journey 2 at T+10d for the same CIF. `CNS-DP` is reused; no re-prompt. Assert the new journey references the existing `consentId`. | Journey orchestration |
| **CNS-R26** | Re-consent MUST create a new record chained via `supersedesConsentId`; the superseded record MUST be retained. | Re-consent. Old record present with `status = SUPERSEDED`; new record references it. | Data store |

> **Product note on CNS-R25.** Reuse is deliberate. An ETB customer re-prompted for identical
> data-processing consent ten days apart learns that consent is noise, which is the outcome the
> obligation exists to prevent. Reuse is bounded by CIF, code, scope and window — all four.

---

## 7. Withdrawal

| Rule | Statement | Test | Enforced at |
|---|---|---|---|
| **CNS-R27** | A customer MUST be able to withdraw any consent at any time, through the RM (assisted) or in-app (DIY). Withdrawal requires the same OTP verification as capture. | Withdraw via each channel; a verified OTP transaction is present on the withdrawal record. | API + UI |
| **CNS-R28** | Withdrawal MUST record `withdrawnAt`, `withdrawnByActorId`, `withdrawalChannel`, `withdrawalReasonCode` (optional, customer-stated) and the OTP transaction. | Retrieve a withdrawal record; all mandatory fields present. | Data store |
| **CNS-R29** | Withdrawal of `CNS-DP` or `CNS-SOL` MUST halt the journey immediately at its current step and set journey status `HALTED_CONSENT_WITHDRAWN`. Journey data is retained under the retention schedule; processing stops. | Withdraw mid-journey. Next journey API call returns `403 JOURNEY_HALTED_CONSENT_WITHDRAWN`. No further insurer calls occur. | Journey orchestration |
| **CNS-R30** | Withdrawal of `CNS-SHR` **after** a proposal has been submitted to the insurer MUST NOT be represented to the customer as retracting the submission. The platform records the withdrawal, halts further sharing, and raises an operations task to communicate with the insurer. | Withdraw post-submission. Consent chain shows withdrawal; an ops task of type `CONSENT_WITHDRAWN_POST_SUBMISSION` exists; the customer-facing message states that the insurer will be contacted, not that the proposal is cancelled. | Journey orchestration + Ops |
| **CNS-R31** | Withdrawal of `CNS-COM` MUST suppress non-mandatory communications only. Regulatory and transactional communications (policy issuance, payment confirmation) continue. | Withdraw `CNS-COM`; marketing messages suppressed, issuance confirmation still sent. | Notification service |
| **CNS-R32** | Withdrawal MUST NOT delete prior consent evidence. | After withdrawal, the original capture record is retrievable in full. | Data store |

> **CNS-R30 is the honest rule.** The platform cannot un-send data already transmitted to an
> insurer. Telling the customer otherwise would be a false statement about a regulated act. The
> business behaviour is: stop, record, escalate to a human, tell the customer exactly that.

---

## 8. Retention and retrieval

| Rule | Statement | Test | Enforced at |
|---|---|---|---|
| **CNS-R33** | Consent evidence MUST be retained for **7 years** from the terminal state of the journey it belongs to (policy issued, lapsed, or abandoned-and-closed). | Compute the retention expiry for a sample of records across all three terminal states; each equals terminal date + 7 years. | Retention policy |
| **CNS-R34** | Retained evidence MUST be held under write-once storage (S3 Object Lock in compliance mode) in an AWS India region. | Inspect the bucket configuration: Object Lock enabled, mode `COMPLIANCE`, retention 7 years, region `ap-south-1` or `ap-south-2`. | S09 infrastructure |
| **CNS-R35** | A single consent record MUST be retrievable within **4 business hours** of a regulator or internal-audit request; a full-journey consent bundle within **1 business day**. | Timed retrieval drill, recorded. | Ops runbook, E3 |
| **CNS-R36** | Retention MUST NOT be shortened by an application configuration change. | Attempt to reduce the retention period through application config; the value is infrastructure-bound and the change has no effect on stored objects. | S09 infrastructure |

**Residency is non-waivable** — see the
[non-waivable list](../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#8-waivers-and-exceptions).
CNS-R34 is therefore a hard dependency on S09 and cannot be closed at S11.

---

## 9. Failure behaviour

Every rule above has a failure path. This section defines what the actor experiences, because a
compliance control that produces an unexplained error trains RMs to work around it.

| Condition | HTTP | Error code | RM sees | Journey effect |
|---|---|---|---|---|
| Required consent absent | 403 | `CONSENT_REQUIRED` | "Customer consent for *(named event)* is needed before this step." + action button | Blocked at step |
| Consent expired | 403 | `CONSENT_EXPIRED` | "Consent given on *(date)* has expired. Re-confirm with the customer." | Blocked; re-consent path offered |
| Consent withdrawn | 403 | `JOURNEY_HALTED_CONSENT_WITHDRAWN` | "The customer withdrew consent on *(date)*. This journey is closed." | Halted |
| Scope mismatch (insurer) | 403 | `CONSENT_SCOPE_MISMATCH` | "Consent covers *(insurer A)*. Capture consent for *(insurer B)*." | Blocked for that insurer only |
| Subject mismatch (CIF) | 403 | `CONSENT_SUBJECT_MISMATCH` | Generic failure; incident raised | Blocked; **security event** |
| OTP not verified | 409 | `CONSENT_OTP_NOT_VERIFIED` | "Waiting for the customer to confirm the code sent to their mobile." | Pending |
| OTP expired | 409 | `OTP_EXPIRED` | "The code expired. Send a new one." | Pending; re-issue offered |
| Attempts exceeded | 429 | `OTP_ATTEMPTS_EXCEEDED` | "Too many incorrect attempts. Send a new code." | Pending; re-issue offered |
| Consent write fails | 5xx | `CONSENT_EVIDENCE_WRITE_FAILED` | "Could not record consent. Do not proceed." | **Fail closed** — the business step does not succeed |

| Rule | Statement | Test | Enforced at |
|---|---|---|---|
| **CNS-R37** | Consent capture MUST fail closed. If the evidence write fails, the business action MUST NOT succeed. | Inject a consent-store write failure; assert the calling business action returns an error and no downstream call is made. | API |
| **CNS-R38** | A `CONSENT_SUBJECT_MISMATCH` MUST raise a security event, not merely a business error. | Trigger the condition; assert a security event is emitted with the actor, journey and both CIF values (CIFs masked per CNS-R20). | API + security monitoring |

> **CNS-R37 resolves the open question left in [BR-SEC-030 AC4](../requirements/BRD-P0-CAPABILITIES.md#br-sec-030--audit-trail)**
> ("prefer fail-open on audit only if Infosec agrees — confirm") **for consent specifically**.
> Product position: consent evidence is the artefact that makes the sale lawful, so a sale that
> proceeds without it is worse than a sale that fails. Audit-log fail-open for *non-consent* events
> remains Deepali's and Shailja's call.

---

## 10. Traceability

| Rule range | Obligation source | BRD requirement | Bounded context | Proven at stage |
|---|---|---|---|---|
| CNS-R01–R09 | IRDAI consent & disclosure; DPDP notice requirements | [BR-CONSENT-010](../requirements/BRD-P0-CAPABILITIES.md#br-consent-010--capture) | #6 Consent Service | S11 |
| CNS-R10–R15 | IRDAI digital consent verifiability; RBI device isolation principle | BR-CONSENT-010 | #6, #17 Notification | S11 |
| CNS-R16–R20 | IRDAI immutable audit trail; DPDP integrity | BR-CONSENT-010 AC3, BR-SEC-030 | #6, #16 Audit & Compliance | S11 / S12 |
| CNS-R21–R26 | IRDAI consent validity | [BR-CONSENT-020](../requirements/BRD-P0-CAPABILITIES.md#br-consent-020--validity) | #6, #9 Journey Orchestration | S11 |
| CNS-R27–R32 | DPDP withdrawal right | [BR-CONSENT-030](../requirements/BRD-P0-CAPABILITIES.md#br-consent-030--withdrawal) | #6, #9 | S11 |
| CNS-R33–R36 | IRDAI 7-year record preservation; data residency | BR-INT-030 | #16, S3 Object Lock | **S09** |
| CNS-R37–R38 | Control integrity | BR-SEC-030 | #6 | S11 |

---

## 11. What this pack does not decide

Named honestly, because a rule pack that pretends to have settled an open regulatory question is
worse than one that flags it.

| ID | Open item | Owner | Needed by |
|---|---|---|---|
| OPEN-CNS-01 | Whether `CNS-DP` and `CNS-SOL` may share one acknowledgement action | Shailja | S11 entry |
| OPEN-CNS-02 | Exact approved wording of all five statement texts (this pack defines the *structure and fields*; the legal text is Compliance-authored) | Shailja + Legal | S05 copy deck / S11 entry |
| OPEN-CNS-03 | Whether the 90-day and 30-day validity windows are regulator-acceptable or must be shorter | Shailja | S11 entry |
| OPEN-CNS-04 | Whether withdrawal post-issuance triggers any insurer-side obligation on the bank | Shailja + Bancassurance | S12 |
| OPEN-CNS-05 | DPDP Consent Manager interoperability, if notified before go-live | Shailja | S14 |

**None of these blocks drafting the Consent Service.** All five are wording, window or
interoperability questions over a data model and a control set that do not change with the answer.
That is the point of specifying structure before text.

---

## 12. Ratification

| Authority | Required conclusion | Status |
|---|---|---|
| Shailja / Risk & Compliance (Board 6) | Regulatory permissibility; statement wording; sequencing; retention basis | **PENDING — human signature mandatory (T4)** |
| Rajal / Product (Board 3) | Business behaviour, journey blocking effects, failure UX, reuse policy | **APPROVED as drafted — Rajal, 2026-08-16** |
| Deepali / Security (Board 4) | OTP handling, IP and identifier confidentiality, security-event routing (CNS-R38) | PENDING |
| Aarti / Database | Append-only enforcement by grant (CNS-R16–R18) | PENDING |
| Swapnali / QA | Every rule's test is executable as written | PENDING |

**Signature status:** `AI-DRAFTED — mandatory human signature outstanding`.
Silence does not approve this pack. Until Shailja signs, S02-G3 is **not** met and
[GAP-006](../po-drive/02-GAP-REGISTER.md) is *content-complete, ratification-pending* — not closed.

— **Rajal**, Principal Insurance Platform Product Owner, 2026-08-16
