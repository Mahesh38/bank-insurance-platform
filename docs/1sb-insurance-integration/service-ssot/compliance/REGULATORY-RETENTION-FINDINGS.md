# Regulatory findings — retention, masking and log residency

**Purpose:** input to compliance questions **Q5** (masking sufficiency) and **Q6** (retention),
raised in the [4.4 review pack](./COMPLIANCE-REVIEW-PACK.md)
**Prepared by:** Engineering, 2026-08-14 · **To be confirmed by:** Risk & Compliance head

> ## ⚠️ This is not a legal opinion
>
> Engineering read the public regulatory material and summarised what appears to apply. Every
> figure below needs the Compliance head's confirmation before it is implemented. Two specific
> reasons not to act on this document alone:
>
> 1. **Retention regimes stack.** Where IRDAI, PMLA and the bank's own policy all apply, the
>    longest wins. Engineering cannot see the bank's internal policy.
> 2. **Which regime applies to which table is a judgement.** The central question below —
>    whether `audit_event` is an *ICT log* or an *insurance record* — changes the answer from
>    180 days to 10 years. That classification is Compliance's to make, not Engineering's.

---

## 1. The finding that matters most

**Our 7-year default for raw payloads is probably too short, and the reasoning behind it was
never regulatory.**

| Source | Requirement | Applies to |
|---|---|---|
| **IRDAI (Maintenance of Information by Regulated Entities…) Regulations, 2025** | **Minimum 10 years** from the date of the last transaction | Insurance records held by insurers and intermediaries |
| **IRDAI Information and Cyber Security Guidelines, 2023** | **Rolling 180 days**, maintained *and monitored*, stored **within India** | ICT infrastructure and application logs |
| **PMLA / PML (Maintenance of Records) Rules** | **5 years** from the date of the transaction | Transaction and KYC records |

The 7 years in `RawPayloadRetentionProperties` sits between the 5-year PMLA floor and the
10-year IRDAI figure. It satisfies neither cleanly.

**Engineering's reading, for Compliance to accept or correct:** raw payloads are evidence of an
insurance transaction — the actual request and response that produced a quote, a proposal, or a
payment. That places them under the **10-year** record regime, not the 180-day log regime.
Changing the default from 7 to 10 years is a one-line configuration change; the difficult part
is that rows already written carry a `retain_until` computed at 7 years.

---

## 2. The classification question

This is the question that decides Q6, and Engineering cannot answer it:

> **Is `audit_event` an ICT/application log (180-day rolling) or an insurance record
> (10-year minimum)?**

The argument each way:

| Reading | Consequence |
|---|---|
| **ICT log** — it records system events: calls made, statuses checked | 180 days rolling, monitored, India-resident |
| **Insurance record** — it is the evidence of who sold what to whom, and of consent | 10 years from last transaction |

Engineering leans to the second for the business events (`PROPOSAL_SUBMITTED`,
`PAYMENT_URL_RETRIEVED`, `CONSENT_REF_MISSING`) and the first for the transport events
(`ONESB_OUTBOUND_CALL`). **If that split is right, the two need different retention** — and the
current schema has no column to distinguish them. That would be a schema change, so it is worth
settling before the table is populated in earnest rather than after.

---

## 3. What this says about the current state

### 3.1 The audit trail failed both regimes until this change

Before audit persistence landed (RISK-012), audit events reached the **application log only**.
Under either reading that was non-compliant:

- as an **insurance record** — no durable store at all, so the 10-year requirement was
  unmeetable;
- as an **ICT log** — the 180-day requirement is "maintained *and* monitored", and typical
  container log rotation is measured in days.

That is now fixed for the WS-1 integration service: events are appended to `audit_event` via the
persistence service. **Retention of that table is still undefined** — see Q6.

### 3.2 Data residency is an open question nobody has asked

Both IRDAI instruments require records and logs to be held **within India**. This repository
carries a `render.yaml` deploying to Render, whose region is not pinned to India in that file,
and the AWS architecture note does not state a region either.

**This is outside the 4.4 review's scope but is raised here because it is the kind of thing that
is expensive to discover late.** It needs an owner. Engineering has not verified where any
deployed instance currently stores data.

### 3.3 The 180-day "monitored" wording

The IRDAI cyber guidelines say logs must be *maintained and monitored*, not merely stored.
Storage we have. Monitoring — alerting on the audit stream — is Phase 6 scope (dashboards,
alerting, SLOs are `out_of_scope` for Phase 4). Worth Compliance knowing that the monitoring
half is scheduled rather than done.

---

## 4. On masking (Q5)

Neither instrument prescribes a specific masking algorithm — they require that personal data be
protected, with the detail left to the entity's own policy. So **Q5 cannot be answered by
citing a regulation**; it needs the Compliance head's judgement against the bank's data
protection policy and, increasingly, the DPDP Act.

The two choices most worth challenging, because both trade privacy for operational convenience:

| Choice | Rationale offered | The objection |
|---|---|---|
| Mobile keeps the **last 4 digits** (`******3210`) | Support staff can confirm they have the right customer | 4 digits plus a name and a city can be re-identifying |
| PAN keeps the **last 5 characters** (`*****1234F`) | Enough to correlate two records without exposing the holder | The last 5 of a PAN is the 4-digit serial plus the check letter — narrower than it looks |

Engineering's view: the mobile rule is defensible, the PAN rule is the one to look at hardest.
If Compliance wants either tightened, it is a change in one class (`PiiMasker`) with tests, and
the generated samples in [`audit-log-samples.md`](./audit-log-samples.md) will show the new
behaviour on the next build.

---

## 5. What Engineering proposes, pending confirmation

| # | Proposal | Blocked on |
|---|---|---|
| 1 | Raise the raw-payload retention default **7 → 10 years** | Compliance confirming the record classification |
| 2 | Define retention for `audit_event`, possibly split business vs transport events | Compliance answering §2 |
| 3 | Define retention for `integration_job`, offers and poll attempts — currently none | Compliance |
| 4 | Confirm the deployment region satisfies India residency | Ops + Compliance; needs an owner |
| 5 | Leave masking as-is unless Compliance objects, with the PAN rule flagged for scrutiny | Compliance |

None of these are implemented. Item 1 is a one-line change and is deliberately **not** made
pre-emptively — changing a retention figure on Engineering's reading of a regulation is exactly
the kind of unilateral compliance decision that should not happen.

---

## 6. Sources

Public secondary sources, read 2026-08-14. **Primary texts should be checked before anything is
implemented** — these are summaries, and summaries of regulation drift.

- IRDAI (Maintenance of Information by Regulated Entities and Sharing of Information by
  Authority) Regulations, 2025 — [summary](https://taxguru.in/corporate-law/irdai-maintenance-information-regulated-entities-sharing-information-authority-regulations-2025.html) ·
  [IRDAI document portal](https://irdai.gov.in/document-detail?documentId=604674)
- IRDAI Information and Cyber Security Guidelines, 2023 —
  [IRDAI](https://irdai.gov.in/document-detail?documentId=3314780) ·
  [summary](https://taxguru.in/corporate-law/irdai-information-cyber-security-guidelines-2023.html) ·
  [180-day logging note](https://www.cybernx.com/logging-solution-irdai-guidelines/)
- RBI Master Direction on IT Governance, Risk, Controls and Assurance Practices, 2023 (effective
  1 April 2024) — [RBI Master Directions](https://www.rbi.org.in/scripts/BS_ViewMasDirections.aspx?id=12562) ·
  [full text PDF](https://fidcindia.org.in/wp-content/uploads/2023/11/RBI-IT-MASTER-DIRECTIONS-07-11-23.pdf)
- PMLA, 2002 and the PML (Maintenance of Records) Rules, 2005 —
  [FIU-IND](https://fiuindia.gov.in/files/AML_Legislation/pmla_2002.html) ·
  [retention summary](https://consentos.in/learn/kyc-record-retention-period/)

> The RBI Master Direction was searched for a specific audit-log retention period and no explicit
> figure was found in the accessible material. It is recorded here as **unresolved**, not as
> "no requirement" — the primary text needs checking by someone who can read it authoritatively.
