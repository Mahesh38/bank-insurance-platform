# CR-013 — Pull Lead language, lifecycle, off-platform ingest, admin/MIS, issuance modes and PPHI mapping into R0

**Date:** 2026-08-25  
**Type:** SCOPE (with ARCH and COMP consequences)  
**Raised by:** Stakeholder decision → recorded by Mahesh (Board 1) under human override  
**Workstream:** WS-3  
**Stage:** S08 — Engineering Foundation  
**Decision:** **CANDIDATE — transcribed into scope artefacts. Human T4 Architecture / Security / Risk & Compliance signatures outstanding.**  
**Origin:** `SUG-20260825-r0s` `ADMIT-BYPASS`  
**Design file:** [`DEC-20260825-01`](../DEC-20260825-01-lead-domain-decisions.md) (timing superseded: build is R0, not parked)  
**Architecture:** `ADR-014`

---

## 1. Current position

The 2026-08-25 intake was first parked (`SUG-20260825-lt1` / `of1` / `st1` / `pp1`) as SF3. Stakeholders have now decided those capabilities **are R0** and must be adapted immediately. Parking on stage-fit is withdrawn. **Compliance is the only remaining gate** on content.

## 2. Proposed change

Admit into R0, and adapt the binding documents now:

| # | Change |
|---|---|
| 1 | Spoken, Product, UI and architecture **primary name is Lead**. Opportunity remains a durable-demand alias only. Identifiers stay `leadId` / `INV-LED-*`. |
| 2 | Lead is the working inbox. After convert + `Payment.RECONCILED` + `Policy.ACTIVE`, Lead archives. Seven-year SoT is Payment, Policy (with issuance history), Consent, Suitability, Audit, plus Lead attribution fields. |
| 3 | Off-platform / insurer-portal sales enter as Policy ingest (`source=OFF_PLATFORM`), never `lead.create`. |
| 4 | Administration UI and MIS reports are **in R0**, on an isolated read path. |
| 5 | `issuanceMode` ∈ {`STP`,`NON_STP`,`INSTA`} on Proposal/Policy. |
| 6 | PPHI (IRDAI 2024) control mapping is an R0 Compliance condition, not a later park. |

`ADR-005` single-origination (RM-only Lead create) **stands**. `ADR-007` configuration **layer** stands; the **UI deferral** is withdrawn by `ADR-014`. Campaign/bulk Lead create stays out.

## 3. What this CR does not do

- Edit `current_phase` or `stage_status`.
- Declare PPHI-compliant.
- Waive suitability, consent, customer-device payment, or RECONCILED-before-issue.
- Allow MIS to create Leads.
- Put Reporting queries on the Lead Aurora writer.

## 4. Driver

Stakeholder decision: the bank already works in Lead language; the working inbox must stay light; the full book (on- and off-platform) and day-one admin/MIS visibility are required for R0 operations; STP/non-STP/Insta and PPHI must be in the same slice.

## 5. Compliance call (Shailja — AI draft, Board 6)

**Verdict:** `APPROVED_WITH_CONDITIONS` · severity withheld pending human T4 (`R` not a pass).

| Condition | Control | Must be true before |
|---|---|---|
| **C-RET-1** | Working Lead rows may leave `RET-7Y` only after a terminal state. `leadId`, `accountableSpId`, `accountableSpCertRef`, `convertedJourneyId`, `lob` stay `RET-7Y` (CTRL-11, C7). | First Lead persist |
| **C-RET-2** | Payment, Policy + `stateHistory[]`, Consent, Suitability, Audit stay on published 7-year / policy+7Y classes. Archive ≠ delete without an audit record (`ID-04`). | First archive job |
| **C-ING-1** | Off-platform ingest writes Policy (+ payment/issuance facts). Maker-checker. No PII in logs. Not `lead.create` (PPHI record-keeping + INV-LED-04). | First MIS upload |
| **C-ISO-1** | Admin/MIS/reconciliation never use the Lead/RM OLTP writer (CTRL-03 minimisation in analytics). | First R0 report |
| **C-ISS-1** | `STP` / `INSTA` do not skip suitability, consent, customer-device payment, or RECONCILED-before-issue. | First proposal |
| **C-PPHI-1** | Control-to-seam map for IRDAI PPHI 2024 exists before the first regulated customer action. Human Board 6 signs permissibility. | W2 Consent / first regulated action |

AI must not generate `TEMPORARY_EXCEPTION_APPROVED`. Human T4 remains mandatory ([11 §9](../11-REVIEW_GATES.md#9-board-6--risk--compliance)).

## 6. Impact

- `CURRENT-STATE.yaml` `current_scope` (not stage fields)  
- `WS-3-PLATFORM-CHARTER.md` §3  
- `R0-SCOPE.md`  
- `ws3-platform/01`, `02`, `03`  
- `ADR-014`  
- Unpark `SUG-20260825-lt1` / `of1` / `st1` / `pp1` → `ADMITTED`  
- `DEC-20260825-01` timing: R0, not parked  
- BOOT regenerated  

GATE-S08 criteria are unchanged. This CR adds R0 product scope, not a waiver of CI/coverage/secrets gates.

## 7. Authority

| Role | Action |
|---|---|
| Stakeholder / human:Mahesh | Authorised immediate R0 inclusion; bypass of SF3 parking |
| Rajal | Scope content (transcribed; human counter-signature outstanding) |
| Mahesh | Structure (`ADR-014`) — T4 outstanding |
| Shailja | Conditions in §5 — T4 outstanding |
| Deepali | Upload + isolation trust boundary — T4 outstanding |
| Aarti | Archive/purge physical design — joint with Mahesh |
| Shivanshi | Isolated report/ingest capacity |
| Kalpana | Sequence recorded; she did not supply a different answer (PA-1) |
