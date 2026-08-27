# DEC-20260825-01 — Lead language, lifecycle, evidence split, off-platform book, day-one visibility, issuance modes, isolation, PPHI

**Status:** `AI-DRAFTED` — persona consensus recorded. **Mandatory human T4 signatures are outstanding.** Silence does not approve this file.  
**Date:** 2026-08-25  
**Revised:** 2026-08-25 — stakeholder pull into R0 (`CR-013`, `ADR-014`). Timing is **R0 now**. Parking withdrawn. **Compliance conditions only.**  
**Workstream:** WS-3 — AU Bank Insurance Distribution Platform  
**Stage:** S08 — Engineering Foundation (GATE-S08 `OPEN`)  
**Origin:** human:Mahesh intake, then human override: *use every persona and take the decisions; write one decision file*, then stakeholder: *include in R0 and work immediately; nothing parked; compliance calls only*  
**Bypass:** `SUG-20260825-df1` + `SUG-20260825-r0s` · `ADMIT-BYPASS` · [09 §8](./09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process)  
**Bypass risk (one sentence):** AI persona verdicts are not human T4 signatures; GATE-S08 CI criteria are not waived; Shailja's conditions in `CR-013` §5 still bind.  
**Freshness:** `CURRENT-STATE.yaml` `state_as_of` is 15 days old (FreshnessCheck WARN). Review due 2026-09-09.

**Index:** [DECISION-REGISTER.md §8](./registers/DECISION-REGISTER.md#8-lead-domain-persona-consensus--2026-08-25)  
**Scope CR:** [`CR-013`](./change-requests/CR-013-r0-lead-mis-admin-scope.md) · **ADR:** `ADR-014`

---

## 1. What this file is

One place that states **what we decided** and **why**. After the stakeholder R0 pull, this file locks **design and R0 scope**. Build is admitted, not parked. Only Shailja's conditions in `CR-013` §5 may hold content.

## 2. What this file is not

- Not a human T4 Architecture, Security, or Risk & Compliance signature.
- Not a waiver of GATE-S08 CI / coverage / secrets criteria.
- Not a reversal of `ADR-005` (single origination, RM-only). `ADR-007`'s **layer** stands; its **UI deferral** is withdrawn by `ADR-014`.
- Not a declaration that the platform is PPHI-compliant.

---

## 3. The decisions in one table

| ID | Decision | Owning authority | Build when |
|---|---|---|---|
| **D1** | People, Product, UI and RM language say **Lead**. Architecture keeps **Opportunity** only as the durable-demand alias. Identifiers stay `leadId` / `INV-LED-*` / `CAP-102`. | Rajal (language) + Mahesh (aggregate alias) + R11 (one spoken term) | R0 — adapted now |
| **D2** | After convert **and** reconciled payment **and** issued policy, the working object leaves Lead. Seven-year evidence lives on Payment, Policy (including issuance history), Consent, Suitability and Audit — not on every working Lead. | Mahesh (split) + Shailja (horizon) + Aarti (physical archive) + Rajal (when "sold" is true) | R0 W1 Lead schema |
| **D3** | Off-platform / insurer-portal sales enter as **Policy ingest + book flag**, never as `lead.create`. | Rajal (need) + Mahesh (boundary) + Shailja / Deepali (upload trust) | R0 W3 |
| **D4** | Day-one admin panel and MIS reports are **in R0**, on the isolated read path. Configuration **layer** still ships in W0b. | Rajal (need) + Mahesh (isolation) + Kalpana (sequence) | R0 W4 |
| **D5** | Operations, MIS, reconciliation and admin jobs **must not** share the Lead OLTP writer or block an RM request. | Already decided — Shivanshi + Aarti + Mahesh + Deepali | Standing rule; no new isolation service |
| **D6** | **STP / non-STP / Insta** are `issuanceMode` on Proposal (inherited by Policy), not Lead states. R0 Term value is configuration, not a code branch. | Rajal (R0 value, later) + Mahesh (dimension) + R11 (states) | R0 W3 schema |
| **D7** | PPHI means IRDAI (Protection of Policyholder's Interests, Operations and Allied Matters of Insurers) Regulations, 2024. Standing hard gates are **necessary**. They are **not** a compliance pass. | Shailja (permissibility) + Deepali (security outcome) + Mahesh (seams) | R0 condition C-PPHI-1 |

---

## 4. D1 — The word is Lead

**We will** call the working origination object **Lead** in Product copy, journeys, acceptance criteria, RM training and Flutter strings. Architecture documents use **Lead** as the primary name and may keep `(Opportunity)` once, as the durable-demand alias.

**We will not** rename `leadId`, `INV-LED-01`–`07`, or `CAP-102`. We will not reopen `ADR-005`'s single-origination rule. We will not let a partner, BFF or MIS job create a Lead.

**Why.** Rajal owns the word the bank already uses. R11 needs one spoken term so AC and state tables do not say Lead in one place and Opportunity in another. Mahesh's 2026-08-20 reason still holds: a working Lead that is archived after conversion is *thin*; renewal, lapse and cross-sell mint a **new Lead and a new Journey**, they do not reopen the old inbox. That rule is easier to teach if people say Lead for the inbox and keep Opportunity only as the "this demand can happen again" alias. Amit: identifier churn buys nothing and breaks every citation.

**Forbids.** A second spoken name in RM-facing artefacts. Any rewrite of invariant IDs. Treating "we say Lead now" as permission to add campaign/bulk origination in R0.

**Revisit if.** Rajal's human confirmation of the screen word disagrees, or a regulator requires the legal term "opportunity" on a disclosure (Shailja).

---

## 5. D2 — Lead is the inbox; Payment and Policy are the evidence

**We will** treat this sequence as the only legal completion:

```text
Lead QUALIFIED
  → Journey sold only when Payment is RECONCILED and Policy is ACTIVE
    (INV-JRN-05, INV-POL-01)
  → Lead CONVERTED (terminal — INV-LED-01)
  → working access moves to Policy (and Payment)
```

**We will** keep, for the regulated horizon:

| SoT | What is kept |
|---|---|
| Payment | Attempts, capture, reconciliation, refund |
| Policy | Issued record, document references, **historic issuance state transitions** |
| Consent / Suitability / Audit | Append-only evidence already required by C2 / C1 / C7 |
| `leadId` on every downstream aggregate | Opaque origin pointer (INV-LED-06) — retained even if the working Lead row is archived |

**We will not** keep every working Lead row for seven years as the compliance strategy. We will not delete a Lead without an audit record (`ID-04`). We will not infer Policy Sold from quote, proposal or payment alone.

**Why.** The Lead module must stay small and fast for RMs. Compliance does not need the inbox; it needs money, issuance, and the history of the issued policy. The domain model already ends Lead at `CONVERTED` and owns money/issuance elsewhere. `02-information-model.md` §4.2 now splits attribution (`RET-7Y`, C-RET-1) from working inbox fields (`RET-WORKING-LEAD`). Shailja owns the exact horizon number. Aarti owns the physical archive/purge (partition, job, restore). Joint Mahesh↔Aarti review is mandatory before any purge path is designed (shared store / source-of-truth change). Until the human Board 6 signature, implementers treat attribution fields as `RET-7Y` and do not purge working fields.

**Shailja (draft, not a pass).** A shorter class for a *converted / expired / disqualified working Lead* is permissible in principle **if** Payment, Policy (with transition history), Consent, Suitability and Audit remain on their published classes (`RET-7Y`, `RET-7Y-IMMUTABLE`, `RET-POLICY+7Y`). The human Board 6 signature sets the number. Until that signature, implementers treat Lead as `RET-7Y`.

**Forbids.** Using Lead as the 7-year bag. Purging `leadId` references. Building the archive job in S08.

**Revisit if.** Shailja's human ruling keeps all Lead attributes at `RET-7Y`, or Aarti finds the purge unsafe on the single R0 Aurora cluster (`ADR-008`).

---

## 6. D3 — Off-platform sales are ingested policies, not Leads

**We will** accept, **in R0**, an MIS-operated ingest of policies (and their payment/issuance facts) that were sold offline or on an insurer portal because the product is not yet on the platform (insurer API missing, 1SB not configured, or equivalent).

**We will** mark each such record `source = OFF_PLATFORM` (name illustrative) and keep a product-onboarding gap: sold but not sellable on-platform.

**We will not** call `opportunity.create` for those rows. MIS is not a Specified Person. An uploaded sale must not look like an assisted platform conversion.

**Why.** Rajal needs the full book and the onboarding backlog. Mahesh: a second **Lead** origination path is exactly what `ADR-005` forbids — ingest is Policy, not `lead.create`. Shailja: an off-platform sale still carries solicitation and record-keeping duty — ingesting the **policy and payment**, not inventing a Lead, is the honest record (C-ING-1). Deepali: file upload is a trust boundary (authz, malware, maker-checker, no PII in logs). Amit: idempotent file ingest, one writer (Policy). Kalpana: sequence is W3 on Policy after the foundation floor; she does not supply a different answer.

**Forbids.** Bulk Lead import in R0. Treating MIS as an RM. Counting off-platform rows in platform conversion KPIs.

---

## 7. D4 — Admin panel and MIS reports are R0 surfaces on an isolated read path

**We will** give business, from R0, an **Administration UI** (`#19`) and **MIS reports** (`#18`): Leads created, qualified, converted; payments reconciled; policies issued or failed; on-platform vs off-platform sold; products sold but not yet onboarded. Those surfaces read Journey / Payment / Policy (and their events) from a replica, extract, or reporting store.

**We will not** put those queries on the Lead / RM Aurora writer (`D5`, C-ISO-1). `ADR-007` stands for the configuration **layer** (W0b). Its **UI deferral is withdrawn** (`ADR-014`). Hardcoding rules because a screen is unfinished remains forbidden (`CF-5`).

**Why.** Stakeholders require day-one visibility. Isolation was already designed. Pulling the surfaces into R0 does not move the writer-protection rule and does not waive GATE-S08 CI / coverage / secrets.

**Forbids.** Admin/MIS on the Lead writer. Hardcoding rules because "the admin screen is not ready" (`ADR-007` / `CF-5`).

---

## 8. D5 — Operations work does not sit on the RM / Lead database writer

**We will** keep Reporting, MIS ingest, reconciliation batches and admin jobs on the async / analytical path already specified: Kafka to Reporting only; S3 / Athena / Redshift for analytics; batch on a different capacity class; OpenSearch is not the regulatory archive (`ADR-013`).

**We will not** add a new isolation service in S08 to re-prove this. `SUG-20260825-wl1` remains REJECTED as new work.

**Why.** Already the communication and data architecture. Shivanshi: name the bottleneck before adding pods — an MIS job on the Lead writer *is* the bottleneck. Aarti: one R0 Aurora cluster (`ADR-008`) makes noisy-neighbour real; isolation is **workload and schema**, not a second cluster today. Deepali: a reporting query is not an excuse to read RESTRICTED attributes into a dashboard index.

**Forbids.** Synchronous Reporting calls from Journey or Lead. MIS/reconciliation using the Opportunity/Lead writer. Answering a regulatory query from OpenSearch.

**Revisit if.** Measured E3 evidence that a reporting or ingest job blocks an RM request.

---

## 9. D6 — STP, non-STP and Insta are issuance modes

**We will** persist `issuanceMode` ∈ { `STP`, `NON_STP`, `INSTA` } on Proposal; Policy inherits it. Lead does not change shape per mode.

**We will** keep one journey saga. Non-STP uses the existing UW-tracking machine. STP and Insta shorten or skip UW **wait**, they do not skip suitability, consent, customer-device payment, or RECONCILED-before-issue.

**We will not** guess which of the three R0 Term is. That seed is Rajal's, recorded as configuration when the catalogue is known. Until then the field exists and is mandatory, non-null, and resolved from configuration (`ADR-006` / `ADR-007` pattern).

**Why.** Same reason `lob` is present from release 1: cheap now, a migration later. R11: three modes are decision-table rows, not three products. Amit: one handler with a mode, not three services. Mahesh: capability ≠ context — issuance mode is not a new bounded context.

**Forbids.** Encoding STP vs Insta as Lead states. A code `if (insurer == X)` instead of configuration. Using Insta as a waiver of C1/C2/C4/C7.

---

## 10. D7 — PPHI applies; we are not "compliant" until Board 6 says so

**We will** treat PPHI as the IRDAI (Protection of Policyholder's Interests, Operations and Allied Matters of Insurers) Regulations, 2024 and relevant master circulars, already listed in Shailja's regulatory registry.

**We will** treat these standing constraints as **necessary** PPHI-adjacent controls: no quote without a valid suitability assessment; no proposal without an unexpired consent grant; payment only on the customer's device; no policy against a payment that is not `RECONCILED`; no PII in logs; India-region residency.

**We will not** have Architecture, Engineering, Delivery or Product declare the platform PPHI-compliant. Shailja's human T4 is the only permissibility signature. The control-to-seam map is the W2 spike (`SUG-20260825-pp1`).

**Why.** Shailja owns interpretation. Deepali owns the security outcome of those controls; she does not reinterpret the regulation. Mahesh owns which seam enforces which gate. Swapnali owns evidence sufficiency and will not mark unexecuted mapping as passed.

**Forbids.** An `A`-rating or a date downgrading a future `R0`. Shipping W2 Consent without the mapping spike being opened.

---

## 11. Persona roll-call

Every row is an **AI card verdict**. `AP` = accountable for that decision's *content*. `C` = consulted. `NA` = must not decide this alone. Human signature column stays empty.

| Persona | Seat | On this file | Verdict (AI) | Human signature |
|---|---|---|---|---|
| **Rajal** | Board 3 · R1 | D1 language `AP`; D2 "sold" meaning `AP`; D3 book need `AP`; D4 R0 admin/MIS surfaces `AP`; D6 R0 Term value `AP` (deferred to catalogue seed) | Agree | *outstanding* |
| **R11 BA** | Product delegate | D1/D2/D6 analysis quality `AP` | Agree — one spoken term, one completion sequence, three issuance rows | *n/a (no board seat)* |
| **Mahesh** | Board 1 · R2 | D1 alias; D2/D3/D4/D5/D6 structure `AP` | Agree. Severity `A2` if D1 were a silent ADR reversal; this file is not that | *outstanding (T4)* |
| **Amit** | Board 2 · R3 | Implementation feasibility `C` | Agree — no identifier rename; one issuance handler; ingest is Policy | *outstanding* |
| **Deepali** | Board 4 · R8 | D3 upload boundary; D5/D7 security outcome `AP` | Agree, `S2`. File ingest is a new trust boundary in R0. T4 not satisfied by this row | *outstanding (T4)* |
| **Aarti** | DBA specialist | D2 physical archive; D5 OLTP isolation `AP` (joint with Mahesh) | Agree, `D2`. No purge on the R0 cluster until a written job + restore test. Schema-per-context stands | *outstanding* |
| **Swapnali** | Board 5 · R7 | Evidence sufficiency `C` | Agree. Critical-journey gates stay non-bypassable. Unexecuted PPHI map is not passed | *outstanding* |
| **Shailja S** | Board 6 · R9 | D2 horizon; D7 permissibility `AP` / `B` | **Draft only.** Split is acceptable in principle; **not** a PPHI pass. `R` rating withheld until the mapping spike | *outstanding (T4)* |
| **Shivanshi** | Board 7 · R10 | D4/D5 operability `AP` | Agree, `O2` if isolation is ignored. No new platform tier in S08 | *outstanding* |
| **Kalpana** | R12 | Sequence and recording `AP`; **content `NA`** | Sequence accepted: S08 floor → W0b config layer → W1 Lead schema (D2) → W2 PPHI map (D7) → W3 issuance + off-platform ingest (D3/D6) → W4 admin/MIS on the isolated path (D4). She does not supply a different answer | *outstanding* |

Unresolved material conflict: **none among the AI cards.** Residual human conflict is expected on D1 (Rajal vs remaining Opportunity citations) and D2/D7 (Shailja's horizon and PPHI map). Those escalate to the accountable humans; they are not averaged here.

---

## 12. What we deliberately did not decide

| Not decided | Why | Owner |
|---|---|---|
| The exact Lead disposal horizon (90 days vs other) | Retention is Shailja's number | Shailja + human |
| Whether R0 Term is STP, non-STP or Insta | Catalogue fact we will not invent | Rajal / Bancassurance |
| Exact MIS report catalog and row-level access | Surfaces are in R0; the catalogue is Product + Security | Rajal + Deepali + Shailja |
| Physical archive mechanism (partition vs table vs dump) | W1 joint Aarti/Mahesh design | Aarti + Mahesh |
| That the platform is PPHI-compliant | Human Board 6 only | Shailja |

---

## 13. Constrains future work

Triage must treat as already decided (re-litigate only with new evidence, [14 §6](./14-CHANGE_CONTROL.md#6-reversing-a-rejection)):

- RM-facing language is Lead (`D1`).
- A second R0 origination path is still forbidden (`ADR-005` + `D3`).
- Admin UI and MIS **on the Lead writer** are still forbidden (`D5` / C-ISO-1). The R0 admin/MIS **surfaces** are admitted (`ADR-014`).
- A new OLTP-isolation microservice in S08 is still forbidden (`D5` / `SUG-20260825-wl1`).
- Declaring PPHI-compliant from Architecture or Delivery is still forbidden (`D7`).

Build items `SUG-20260825-lt1`, `of1`, `st1`, `pp1` are **ADMITTED** into R0 (`CR-013`). They are not parked.

---

## 14. Revalidation triggers

- Human Rajal rejects Lead as the screen word.
- Human Shailja rejects a shorter Lead class or finds a PPHI gap that changes R0 behaviour.
- Aarti finds archive/purge unsafe on the single Aurora cluster.
- Measured OLTP contention from a reporting or ingest job (`D5` reopen).
- An approved CR removes #18 or #19 from R0, or puts them back on the Lead writer.
