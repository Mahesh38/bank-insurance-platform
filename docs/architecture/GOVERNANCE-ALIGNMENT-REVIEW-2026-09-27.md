# Governance alignment review — BRD · PRD · Figma · Architecture · Decisions

22 conflicts, mismatches and gaps between the business SSOT, BRD/PRD, Figma and the R0 architecture and decision layer, each with the human who decides it.

**Date:** 2026-09-27 · **Reviewer:** Mahesh lens (Board 1, R2) — **AI-DRAFTED, not a T4 sign-off**
**Freshness:** `FreshnessCheck` = FRESH (exit 0) · state as of 2026-09-13, review due 2026-10-11
**Scope:** WS-3 R0 (`R0-ASSISTED-LIFE-SALE`), with WS-1 / WS-2 where they touch it
**Purpose:** list every conflict, mismatch and gap between the business SSOT, the product
requirements, the Figma prototype and the architecture/decision layer, so each can be walked and
decided one at a time. **Nothing here is a decision.** Each row names the human who decides.

## Sources read

| Layer | Documents |
|---|---|
| Business SSOT | [`07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md`](../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) · [`DECISION-LOG.md`](../au-bank-insurance-platform/DECISION-LOG.md) · [`po-drive/02-GAP-REGISTER.md`](../au-bank-insurance-platform/po-drive/02-GAP-REGISTER.md) |
| BRD / PRD | [`BRD-OVERVIEW.md`](../au-bank-insurance-platform/requirements/BRD-OVERVIEW.md) · [`BRD-P0-CAPABILITIES.md`](../au-bank-insurance-platform/requirements/BRD-P0-CAPABILITIES.md) · [`PRD-R0-DISTRIBUTION-PLATFORM.md`](../au-bank-insurance-platform/requirements/PRD-R0-DISTRIBUTION-PLATFORM.md) · [`R0-SCOPE.md`](../au-bank-insurance-platform/requirements/R0-SCOPE.md) |
| Rule packs | [`consent-rule-pack.md`](../au-bank-insurance-platform/rule-packs/consent-rule-pack.md) · [`suitability-rule-pack.md`](../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md) |
| Figma | [`docs/figma/`](../figma/) (7 wireframes, 6 visual-design boards) · [`figma_file_structure.json`](../figma_file_structure.json) · [`05-figma-and-artefact-intake.md`](../au-bank-insurance-platform/05-figma-and-artefact-intake.md) · [S05 evidence](../application-lifecycle-bible/evidence/S05-experience-evidence.md) |
| Architecture | [`R0-HLD.md`](./R0-HLD.md) · [`R0-LLD.md`](./R0-LLD.md) · [`03-solution-architecture-r0.md`](../platform/ws3-platform/03-solution-architecture-r0.md) · [`01-domain-model-and-invariants.md`](../platform/ws3-platform/01-domain-model-and-invariants.md) · [`05-nfr-catalogue.md`](../platform/ws3-platform/05-nfr-catalogue.md) · [`09-nip-bff-customer-search-contract.md`](../platform/ws3-platform/09-nip-bff-customer-search-contract.md) · [`DATA-002-cr013-alignment.md`](../platform/data-architecture/DATA-002-cr013-alignment.md) |
| Governance | [`CURRENT-STATE.yaml`](../governance/state/CURRENT-STATE.yaml) · [`DECISION-REGISTER.md`](../governance/registers/DECISION-REGISTER.md) · [`DEC-20260825-01`](../governance/DEC-20260825-01-lead-domain-decisions.md) |

## Severity used here

| Tag | Meaning |
|---|---|
| **CRITICAL** | A regulatory control, the R0 objective, or the build's authority is contradicted. Blocks S11 or the pilot |
| **HIGH** | Teams will build different things from different documents |
| **MEDIUM** | Priority/ownership mismatch; resolvable by re-tagging or a one-line decision |
| **LOW** | Stale text; trust and hygiene |

Architecture-only items also carry Mahesh's `A0`–`A3`. A Compliance or Security question is
**never** rated down by an `A` tag.

---

## Summary

| # | Finding | Type | Severity | Decides |
|---|---|---|---|---|
| GA-01 | Day-1 three journeys still P0 in BRD/PRD/R0-SCOPE; governance says assisted-only R0 | Scope | CRITICAL | Rajal |
| GA-02 | Group B redirect is P0 in PRD/R0-SCOPE; R1 in governance and architecture | Scope | HIGH | Rajal |
| GA-03 | CR-015 (Term + Savings/ULIP) not carried through HLD body, solution architecture, S04/S05, app | Architecture | CRITICAL · `A1` | Mahesh + Rajal |
| GA-04 | Benefit Illustration (eBI), CIS and ULIP specifics absent from every design document | Regulatory gap | CRITICAL | Rajal + Shailja |
| GA-05 | "RM shares payment link" (WD, PRD, BRD) vs "no payment link into an RM session" (INV-PAY-01) | Regulatory conflict | CRITICAL | Rajal + Deepali |
| GA-06 | Suitability override: BRD allows, domain model carries `OVERRIDDEN`, rule pack forbids | Regulatory conflict | CRITICAL · `A1` | Shailja → Mahesh |
| GA-07 | Figma product-first paths (manual selection, PLP projections, Pitch Deck) precede suitability/consent | Design vs regulation | CRITICAL | Rajal + Shailja |
| GA-08 | Figma shows full customer PII before any consent; BRD consent point weaker than rule pack | Design vs regulation | HIGH | Rajal + Shailja |
| GA-09 | Build proceeds on AI-drafted decisions: 19 of 20 ADRs Proposed, 4 CRs CANDIDATE, sponsor unnamed | Governance | CRITICAL | Humans named below |
| GA-10 | BRD-OVERVIEW R0 chapters with no PRD, BRD-P0 or design coverage | Scope gap | HIGH | Rajal |
| GA-11 | No agreed SSOT precedence: WD "wins", BOOT "governance wins", architecture calls R0-SCOPE "old" | Governance | HIGH | Rajal + Mahesh |
| GA-12 | Admin UI and MIS: Won't/P1/P2 in BRD/PRD/S05, but R0 by CR-013 / ADR-014 | Scope | MEDIUM | Rajal |
| GA-13 | Customer communications: R0 in BRD, P1 in PRD, OTP + payment link only in state | Scope | MEDIUM | Rajal |
| GA-14 | Actor model: IPR has no business requirement; Figma "assign to insurance RM", "FLS", "New Customer" | Architecture vs product | HIGH · `A2` | Rajal + Mahesh |
| GA-15 | Customer search keys differ across Figma, BRD and API contract | Design | LOW | Rajal |
| GA-16 | Figma depicts Health, a customer web session and document upload — all outside R0 | Design | MEDIUM | Rajal |
| GA-17 | Audit-write failure policy unresolved and overdue (S03-OPEN-02, due 2026-09-12) | Security | HIGH | Deepali + Shailja |
| GA-18 | NFRs: PRD 99.5 % vs NFR catalogue 99.9 %; 7-year retention built before GAP-017 closes | NFR | MEDIUM | Rajal + Shivanshi + Shailja |
| GA-19 | Architecture internal: MSK in R0 vs WS-1 "Kafka out"; Athena/Redshift in DEC-20260825 vs LLD | Architecture | MEDIUM · `A2` | Mahesh |
| GA-20 | "Never issued unless RECONCILED" vs INSTA/STP issuance and insurer-controlled issuance | Architecture vs business | HIGH · `A1` | Mahesh + Rajal |
| GA-21 | Lead naming and deletion: ADR-005 "opportunity", BRD "Lead Deletion" vs archive + 7-year | Domain | MEDIUM | Rajal + Aarti |
| GA-22 | Stale status text across intake log, S05, BRD-P0, requirements README | Hygiene | LOW | Rajal |

**Proposed walk order:** GA-09 → GA-11 (who decides and which document wins), then GA-01/02/03
(what R0 is), then the regulatory set GA-04/05/06/07/08, then the rest.

---

## Findings

### GA-01 · Day-1 three journeys vs assisted-first R0 — CRITICAL

| Says three journeys in R0 | Says assisted only |
|---|---|
| D-002 and WD §2 — "all three journeys from Day 1" | [DEC-20260816-03](../governance/registers/DECISION-REGISTER.md#7-product-decisions--ws-3-realignment-increment-cr-010) — R0 assisted-first; DIY R1, hybrid R2 |
| BRD-OVERVIEW tag legend — "R0 = Life; RM + self + hybrid" | CURRENT-STATE `out_of_scope` — DIY R1, hybrid R2, Customer BFF R1 |
| BRD-P0 "Explicit Won't" footer — "Day 1 Must: RM-assisted, self-service and hybrid" | R0-HLD §2.1 — the customer is never a session on the platform in R0 |
| PRD §2, §3, stories 10–13, **PRD-F-18 / F-19 = P0** | S05 inventory — DIY screens ⛔ R1 |
| R0-SCOPE v0.4 §1 goal, §2 A2, §5 success metric "RM / self / hybrid" | R0-SCOPE v0.4 banner (top of the same file) |

**Problem.** DEC-20260816-03 made condition C4 "republish R0-SCOPE". Only the banner was changed;
the body, BRD-P0 and PRD still carry P0 self-service and hybrid. A tester working from the PRD
fails R0 for a scope that governance removed.
**Proposed resolution.** Rajal republishes BRD-P0 (0.4), PRD (0.3) and R0-SCOPE body: F-18/F-19 → R1/R2
with the revisit triggers; D-002 annotated "superseded for R0 by DEC-20260816-03", original text kept.

### GA-02 · Group B redirect priority — HIGH

PRD story 5 and **PRD-F-20 = P0**; R0-SCOPE §1, §3 "Redirect (Group B)", §5 "Group B redirect path
usable"; D-010. Against: CURRENT-STATE out-of-scope "Group B — revisit R1"; consent pack `CNS-RDR`
⛔ R1; S05 Group B screens ⛔ R1; R0-HLD §7 roadmap R1.
**Proposed resolution.** Same republish as GA-01. Decide whether catalogue *visibility* of Group B
products (no redirect) is wanted in R0 — the WD §7 catalogue says the platform "still knows
product availability".

### GA-03 · CR-015 Savings/ULIP not carried through the design — CRITICAL · `A1`

CR-015 (2026-09-12) put Term **and** Savings/ULIP in R0. The objective and R0-HLD §0 (line 45)
say so. The rest does not:

| Document | Still says |
|---|---|
| [R0-HLD §2.3](./R0-HLD.md#23-line-of-business-is-first-class-from-release-1) | "R0 sells `lob = LIFE`, `productClass = TERM`" |
| R0-HLD §3 Ten boundaries | "1SilverBullet — Group A, **Term only**" |
| R0-HLD §7 roadmap | R1 includes "ULIP/Savings" |
| [03-solution-architecture-r0 §2–§3](../platform/ws3-platform/03-solution-architecture-r0.md) | `productClass = TERM`; Wave W1 "Product Catalogue (Term only)" |
| 01-domain-model §2.5 | `productClass = TERM` |
| S04 evidence | R0 population rule "one Group A / Life / Term slot" |
| S05 evidence | "Scope: the R0 assisted Term journey"; 18-screen inventory is Term-shaped |
| DEC-20260816-12 · `apps/rm-workspace-app` README | "R0 assisted Term journey" |

**Problem.** Savings/ULIP is not "Term with a different product code": it adds benefit illustration,
fund selection and risk profile (ULIP), maturity/payout options, riders, and a different suitability
outcome class set. None is designed (see GA-04). The HLD contradicts itself between §0 and §2.3.
**Proposed resolution.** Mahesh: HLD/solution-architecture delta for `SAVINGS_ENDOWMENT` and `ULIP`
(catalogue matrix, quote/proposal schema variance, product-class LOB cell). Rajal: S04 matrix slot
and S05 screen inventory for the Savings path. Until then, CR-015 is transcribed in scope but not in
design.

### GA-04 · Benefit Illustration, CIS and ULIP specifics missing — CRITICAL

BRD-OVERVIEW §7.10 "eBI sharing and acceptance" = **R0**; §8.3 consent on "eBI, CIS, Proposal form";
Figma quote-confirmation shows a "Benefit Illustration" button and "shared with the customer via
email". **No match** for eBI / benefit illustration / CIS in BRD-P0, PRD, R0-HLD, R0-LLD, the
solution architecture, S03/S05 evidence or either rule pack.

**Problem.** For a savings or ULIP sale a customer-acknowledged illustration and key-feature disclosure
are the kind of evidence the "complete audit trail" in the objective rests on. Whether each is
mandatory, and in what form, is Shailja's call — but today it is not even a named open item.
**Proposed resolution.** Rajal raises BR-QUOTE-030 (illustration) and BR-PROP-040 (CIS acknowledgement);
Shailja rules on obligation and wording; Mahesh places the artefact (Quotation or Proposal context,
evidence store, customer-device acknowledgement — ties to GA-05).

### GA-05 · Who shares the payment link — CRITICAL

| Business layer | Architecture layer |
|---|---|
| WD §2.3 example "RM shares payment link" | [INV-PAY-01](../platform/ws3-platform/01-domain-model-and-invariants.md): "No API path issues a payment link into an RM session" |
| PRD persona RM "share payment/insurer links"; story 8 "I share a payment link" | Standing constraint in CURRENT-STATE |
| BR-PAY-010 AC1–2 "RM/customer can create payment session… `paymentUrl` returned to client" | S05 SCR-16 "renders no payment surface, ever"; FF-14 negative test |
| WS-1 PRODUCT-BACKLOG — adapter returns `paymentUrl` to the caller | R0-HLD §1 row 6 — link issued to the customer device |

**Problem.** Both sides mean "customer pays on own device", but BR-PAY-010 as written makes the RM
BFF a holder of the URL — the exact path INV-PAY-01 forbids. QA will write opposite tests.
**Proposed resolution.** Restate BR-PAY-010: "RM *requests dispatch*; the platform sends the link to
the CBS-registered mobile; the RM sees status and a reference, never the URL". WS-1's `paymentUrl`
stays internal to #12 Payment. Deepali confirms; Rajal edits the BRD, PRD and WD example wording.

### GA-06 · Suitability override — CRITICAL · `A1`

- BR-SUIT-020 AC2: "If RM overrides, reason code mandatory + audited (if overrides allowed — **confirm**)".
- [R0-HLD §2.1](./R0-HLD.md#21-two-actors-one-of-them-sells) IPR predicate: `suitabilityState IN (COMPLETED, OVERRIDDEN)`; the domain model carries an `OVERRIDDEN` state.
- [INV-QUO-01](../platform/ws3-platform/01-domain-model-and-invariants.md) — **the C1 quote hard-gate itself** — lets a quote leave `REQUESTED` on an assessment in `COMPLETED` **or `OVERRIDDEN`**; the §4.3 state machine has `COMPLETED → OVERRIDDEN: override(actor, reason)`; INV-SUI-02 defines the override record.
- [Suitability pack §7.1](../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md): **SUIT-R38** — `NOT_SUITABLE` must not be overridden by any actor in R0; no path, no flag. **SUIT-R39** — same for `INSUFFICIENT_DATA`. GAP-007: "no override in R0".

**Problem.** A transition the rules forbid exists in the state machine, in the IPR visibility
predicate and in the C1 gate. That is how a bypass button appears later.
**Proposed resolution.** Shailja ratifies SUIT-R38/R39; then Mahesh removes `OVERRIDDEN` from the R0
state set and the IPR predicate (or documents it as an R1+ reserved value that no R0 transition
reaches), and Rajal deletes the BR-SUIT-020 AC2 `confirm`.

### GA-07 · Figma paths that precede suitability and consent — CRITICAL

| Figma section | What it shows | Conflicts with |
|---|---|---|
| *Lead Creation With Manual Product Selection* | RM picks Term / Savings / ULIP at lead creation, then suitability | Only mild — suitability still precedes the plan list. Needs a rule that manual selection cannot pre-filter out suitable classes |
| *PLP – RM Browsing* / *PDP – RM Browsing* | Plans with "You invest ₹24 L → You get ₹1.01 Cr", "Best Returns" | D-005 / SUIT-R20 if these are personalised figures; QR-07 ranking basis |
| *PLP* filter chips "Highest Corpus · Claims Trust · **Most Sold**" | Ranking by popularity | [QR-07 / DEC-20260816-08](../governance/registers/DECISION-REGISTER.md#7-product-decisions--ws-3-realignment-increment-cr-010): disclosed customer-relevant basis only |
| *Pitch Deck* (create + preview) | Select customer → generate personalised deck ("₹38.4 Lakhs at age 60") → **send to customer** → "lead created" | No need analysis, no suitability, no `CNS-COM` before outbound communication; not in BRD; not in S05 ("zero unrequested screens") |

**Problem.** Figma is reference only (D-012), but the prototype is what stakeholders have seen. The
pitch deck in particular is a personalised illustration sent before suitability — the pattern the
suitability gate exists to stop.
**Proposed resolution.** Rajal + Shailja decide per row: (a) generic, non-personalised browsing
allowed with no figures tied to the customer; (b) "Most Sold" permitted only if disclosed as the
basis, or removed; (c) Pitch Deck parked to R1 behind a consent + suitability precondition, or rejected.

### GA-08 · Consent point: Figma and BRD weaker than the rule pack — HIGH

- Figma *Confirm Customer Details* shows DOB, mobile, email, address, occupation and income
  immediately after search; no consent screen appears anywhere in the Figma lead or buying flow.
- BR-CONSENT-010 AC1: consent before **Suitability/Quote**.
- Consent pack: `CNS-DP` before **any** CBS data is read into the journey; [09 search contract](../platform/ws3-platform/09-nip-bff-customer-search-contract.md) — PAN, DOB, address and income unlock only on `SCR-07` after `CNS-DP`.
- WD §2.1 typical flow "RM → Need Analysis → Quote → …" omits consent.

**Proposed resolution.** Rajal raises BR-CONSENT-010 to the pack's binding points (DP before prefill,
SOL before questionnaire, SHR before submit); Figma flagged as non-conformant; Shailja's OPEN-CNS-01
(one acknowledgement vs three) still decides the screen count.

### GA-09 · The build runs on unratified decisions — CRITICAL

| Item | State |
|---|---|
| ADR-001 … ADR-020 | 19 Proposed; only ADR-019 Accepted. Thirteen are `A3_JOINT_REVIEW`, ADR-002 `A4_HUMAN_REQUIRED` |
| CR-010, CR-013, CR-015, CR-016 | CANDIDATE — "transcribed under ADMIT-BYPASS"; human T4 outstanding |
| CR-002, CR-008, CR-011, CR-012 | PENDING RATIFICATION (CR-012 = MSK, cache, egress, search — five specialists outstanding) |
| DB-DEC-0001 / 0002 | AI-drafted; Aarti's signature outstanding |
| D-001 … D-014 | "Working", pending sponsor validation since 2026-07-31 |
| GAP-010 sponsor | Unnamed; target 2026-08-29 passed → `FRI-001` funding **BLOCKED** |
| GAP-006 / GAP-007 | Content complete; Shailja's E2 signature outstanding → **S11 cannot start** (DEC-20260816-06) |

**Problem.** Scope (CR-013, CR-015) and topology (CR-012) have been transcribed and are being built
while the humans who own them have not signed. This is the root cause of GA-01/03: documents were
edited partially because nobody closed the decision.
**Proposed resolution.** Not an architecture call. Kalpana (R12) runs a decision-forcing session per
CR-009: sponsor name, Shailja's two packs, CR-015 (Rajal + Mahesh human verdicts), CR-012.
Mahesh can prepare the Board 1 evidence; he cannot supply the signature.

### GA-10 · BRD-OVERVIEW R0 chapters with no downstream coverage — HIGH

BRD-OVERVIEW is "the binding chapter map"; these sub-headings are tagged R0 there but appear in no
PRD requirement, no BRD-P0 capability and no design document:

| BRD § | Sub-heading (tag) | Figma | Architecture |
|---|---|---|---|
| 1.2 / 1.3 | Forgot password (R0/R1\*), account lock/unlock (R0) | Login board shows 2FA, forgot password, **MPIN** | ADR-020 AD-verify via bank API; nothing on lock, MPIN or 2FA |
| 3.3 | Lead follow-up & meeting reminder (R0) | — | — |
| 3.5 | Individual reassignment (R0) | "Assign lead to insurance RM" | ADR-005: only `BANK_RM` originates; reassignment undefined |
| 3.7 | Duplicate flagging, expiry, **deletion** (R0) | — | See GA-21 |
| 7.4 / 7.7 / 7.8 | Downloads, modify quote (SA/PT/PPT/mode), add-on riders (R0) | Riders (WOP, CI, cancer), term/frequency edits | Not in quote design |
| 7.10 / 8.3 | eBI, CIS acceptance (R0) | BI button | See GA-04 |
| 8.2 / 8.4 | Document waiver (R0/R1), ACR (R0/R1) | Document upload screens | — |
| 9.x | PTL/RAG, pre-issuance verification, video/PDF to insurer (R0) | *PIVV*, *Verification Overview* | DATA-002: "do not invent PTL/RAG" |
| 10.1 / 10.2.i | Dual mandate, internal fund transfer (R0) | — | DATA-002: IFT out; AU Bank PG only |
| 10.3 / 11.3 | Payment and policy communications (R0) | — | See GA-13 |

**Proposed resolution.** Rajal decides per row: in the R0 slice (then BRD-P0 + design), or re-tag
R1 in BRD-OVERVIEW with change-control, as the document itself requires. Pre-issuance verification
(§9.2) deserves a Compliance view before deferral.

### GA-11 · No agreed precedence between documents — HIGH

- WD header: "If older docs disagree, **this file wins**" (dated 2026-07-31).
- BOOT: "`docs/governance/` always wins over this capsule".
- CURRENT-STATE `authority` lists BRD-P0 as an authority for WS-3.
- DATA-002 §2: "Do **not** invent from the old R0-SCOPE one-pager" — architecture treating the PO's
  current v0.4 scope document as stale.
- Requirements README: BRD-OVERVIEW is "binding TOC"; BRD-P0 "integration-era; align to BRD overview".

**Proposed resolution.** One precedence line, owned by Rajal and Mahesh jointly, e.g.
`CURRENT-STATE + ratified DEC/CR > WD > R0-SCOPE > BRD-P0 > PRD > BRD-OVERVIEW tags > Figma`,
written into the requirements README and the WD header.

### GA-12 · Admin UI and MIS priority — MEDIUM

BR-PROD-020 "full admin UI = Won't"; PRD-F-17 P2; PRD-F-15 P1 (P0 counts only); S05 admin UI ⛔ R1;
GAP-022 open — against CR-013 / ADR-014: admin UI (maker-checker) and MIS in R0 W4; CURRENT-STATE
contexts #18 and #19 in scope. S05 has no admin or MIS screens beyond `SCR-18`.
**Proposed resolution.** Rajal updates PRD/BRD-P0 to the CR-013 position and adds admin/MIS to the
S05 inventory — or reverses CR-013. It cannot stay both.

### GA-13 · Customer communications — MEDIUM

BRD §10.3 and §11.3 R0 (payment, mandate and policy communications to customers); PRD-F-16 P1;
BR-COMM-010 Should; CURRENT-STATE: #17 Notification "beyond OTP and payment-link delivery" is R1.
R0-HLD: notification failure "never blocks the journey", while the consent OTP and payment link
are both C-control dependencies.
**Proposed resolution.** Rajal states the R0 minimum (OTP, payment link, policy-issued SMS?);
Mahesh separates "gate-critical dispatch" (OTP, link — must succeed or the step waits) from
"informational" (never blocks).

### GA-14 · Actor model ahead of the business requirements — HIGH · `A2`

- ADR-004 / R0-HLD §2.1: two on-platform actors, Bank RM and **Insurance Partner Representative**;
  actor vocabulary closed at `BANK_RM · INSURER_PARTNER_REP · SERVICE`.
- BRD/PRD personas: RM, Customer, Ops, Compliance, Platform admin. **IPR appears nowhere** in the
  business layer (only BRD §2.3 "partner hierarchy").
- BR-SEC-020 roles `RM · OPS_READONLY · ADMIN_CONFIG`; ADR-015 roles RM / IPR / admin / ops.
- Figma: "Assign lead to insurance RM (branch · insurance partner · RM)" after quote creation;
  a *For FLS Journey* board; a "+ New Customer" action (NTB is out, D-009).

**Proposed resolution.** Rajal confirms the IPR as a business actor with its own BR set (what an
insurer RM may see and do), defines FLS, and removes "New Customer". Mahesh confirms "assign to
insurance RM" is an IPR *visibility grant*, not a change of the accountable SP.

### GA-15 · Customer search keys — LOW

Figma "Search by Name, Phone no, Cust ID"; BRD §4.1 "Cust ID, Mobile No, PAN etc."; BR-CUST-010
"confirm"; 09 contract: Customer ID, PAN, Mobile only — **name excluded** (`OPEN-LEAD-NAME`).
**Proposed resolution.** Rajal closes `OPEN-LEAD-NAME`; Figma annotated.

### GA-16 · Figma depicts out-of-scope surfaces — MEDIUM

"Select a product: **Life | Health**" tab (D-001: Life only); customer-side *Buy-Savings Plan-Customer*
screens on an `insurance.aubank.in` browser session with document upload and PIVV (R0-HLD: customer
never holds a session in R0); Savings-only PLP/PDP while the S05 build is Term-only (GA-03).
**Proposed resolution.** Apply the S05-OPEN-01 mapping: tag every Figma frame R0 / R1 / R2+ /
concept, so the prototype stops being read as the R0 target.

### GA-17 · Audit-write failure policy overdue — HIGH

BR-SEC-030 AC4 "prefer fail-open on audit only if Infosec agrees — **confirm**". S03 resolved it for
consent only; **S03-OPEN-02** (non-consent audit writes) was due 2026-09-12 with Deepali + Shailja
and is open. R0-HLD makes `SOLD` wait for audit (`INV-JRN-05`), which is fail-closed at the end but
silent on every earlier step.
**Proposed resolution.** Deepali + Shailja decide; the architecture already supports either via the
outbox.

### GA-18 · NFR and retention numbers — MEDIUM

PRD-N-01 "99.5 % monthly for RM workspace" vs [NFR-AVL-01](../platform/ws3-platform/05-nfr-catalogue.md)
99.9 % for sale-path services including the BFF. GAP-017 (SLA, retention, RTO) is **open**, WD §16
lists retention and residency as "pending validation", yet the S09 deliverable is "7-year write-once
retention" and PRD-N-04 says "draft 7 years".
**Proposed resolution.** Rajal aligns PRD-N-01 to the catalogue (or the catalogue is lowered with
Shivanshi's agreement). Building to 7 years is the safe default, but Shailja must sign the number
before purge logic exists (DEC-20260825 D2 already says so).

### GA-19 · Architecture internal inconsistencies — MEDIUM · `A2`

- ADR-012 (CR-012): **MSK is the R0 event backbone** — vs WS-1 out-of-scope "Kafka / event backbone —
  revisit at Integration architecture stage", and BOOT L4 posture "reject generic frameworks".
- [DEC-20260825-01](../governance/DEC-20260825-01-lead-domain-decisions.md) "S3 / Athena / Redshift for
  analytics" — vs R0-LLD "DO NOT PROVISION Glue ETL / Athena / Redshift / QuickSight".

**Proposed resolution (Mahesh).** State that ADR-012 governs WS-3 and WS-1 stays outbox-only until
its own stage; amend DEC-20260825 to "R1+ analytics" so it matches the LLD.

### GA-20 · Reconciliation before issuance vs how insurers issue — HIGH · `A1`

Standing constraint and R0-HLD §1 row 8: "a policy is never issued against a payment that is not
RECONCILED". But the insurer, not the bank, issues; ADR-014 adds `issuanceMode = INSTA / STP`, where
issuance can follow payment within seconds, before bank-side reconciliation runs. BR-POL-010 marks
the journey "Policy Issued" when the insurer issues, with no reconciliation clause; D-007 counts
**Sold** only after reconciliation.
**Proposed resolution.** Reword the constraint to what the bank controls: "the platform never
*records a policy as Sold* (and never triggers an issuance request) until payment is RECONCILED;
an insurer-side issuance observed earlier is held as `ISSUED_PENDING_RECON`". Rajal confirms the
business meaning; Mahesh amends the invariant.

### GA-21 · Lead naming and deletion — MEDIUM

ADR-005 text "The **opportunity** is the single origination point"; schema file
`04-opportunity.sql` — vs ADR-014 / DEC-20260825 D1: Lead is the name, Opportunity an alias only.
BRD §3.7 "Lead **Deletion**" (R0) — vs D2: archive after sold, "never delete a Lead without an audit
record", attribution fields `RET-7Y`.
**Proposed resolution.** Amend ADR-005 wording to "Lead"; Rajal restates §3.7 as
"disqualify / expire / archive" — no hard delete in R0; Aarti owns purge.

### GA-22 · Stale status text — LOW

- `05-figma-and-artefact-intake.md`: Figma "Inventory status: **Not started**" — S05 closed GAP-009 on mapping.
- S05 §2: "No `pubspec.yaml` exists anywhere" — `apps/rm-workspace-app/pubspec.yaml` exists (DEC-20260816-12).
- Requirements README "v0.2"; BRD-P0 is 0.3; PRD 0.2 still pre-DEC-20260816-03.

**Proposed resolution.** Housekeeping edits after GA-01/03 land, so they are done once.

---

## What is aligned (no action)

- Life-only LOB; ETB-only; Sold = issued + confirmed + reconcilable + trackable — consistent everywhere.
- Suitability hard gate before quote — BRD, PRD, rule pack, HLD (C1), S05 and the app agree.
- 1SB behind Integration Hub and adapter, replaceable — WD §18, BR-INT-010, ADRs, ArchUnit.
- `distributorId` from secrets only; `agentId` mandatory at proposal submit.
- QR-07 ranking rule exists and is implemented; only the Figma chips conflict (GA-07).

## Next step

Walk the findings in the proposed order. Each agreed outcome becomes a triaged item in
[`SUGGESTION-REGISTER.md`](../governance/registers/SUGGESTION-REGISTER.md) or a CR — not an edit made
in the discussion turn.
