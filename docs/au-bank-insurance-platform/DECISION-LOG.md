# Decision log — AU Bank Insurance Platform

**Canonical ID scheme:** `D-xxx` = business / scope decisions (aligns to Session template + Working Decisions mapping).  
**Process / documentation decisions:** `DOC-xxx` (do not reuse D-xxx).  
**Status legend:** `Working` = agreed in discovery, pending sponsor formal validation · `Accepted` = process decision · `Pending` = open.

**SSOT narrative:** [07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md)

---

## Business / scope decisions (`D-xxx`)

| ID | Date | Decision | Rationale | Decided by | Status | Links |
|----|------|----------|-----------|------------|--------|-------|
| D-001 | 2026-07-31 | MVP LOB = **Life only** (Term, ULIP, Savings/Investment; future life as required) | Phase 1 focus; Health/Motor/Travel out | Business (draft) | Working | WD §1 |
| D-002 | 2026-07-31 | Day 1 channels = **RM-assisted + Self-service + Hybrid** (seamless mode switch) | Bank requires all three from Day 1 | Business (draft) | Working | WD §2 |
| D-003 | 2026-07-31 | Aggregator for MVP = **1SB** (single aggregator; multi-agg later extensibility only) | Commercial onboarding complete; UAT provisioning | Business (draft) | Working | WD §§13, 19 |
| D-004 | 2026-07-31 | Replaceability **required** — no tight coupling of core capabilities to 1SB | Survive aggregator change | Business (draft) | Working | WD §§18–19 |
| D-005 | 2026-07-31 | Need analysis + suitability **mandatory** before quote; no bypass | Regulatory / suitability / audit | Business (draft) | Working | WD §8 |
| D-006 | 2026-07-31 | Payment on **customer device** via **AU Bank PG** only; no payment on RM device | Liability + bank rails | Business (draft) | Working | WD §§6, 14 |
| D-007 | 2026-07-31 | **Sold** = policy issued + bank confirmation + reconcilable + ops-trackable | Quote/proposal/payment alone ≠ sale | Business (draft) | Working | WD §4 |
| D-008 | 2026-07-31 | Insurance advisor / agent identity model | Expected individual identifier; validate vs IRDAI + insurer onboarding | Compliance + Ops | **Pending** | WD §10 |
| D-009 | 2026-07-31 | MVP segment = **ETB only** (any AU Bank relationship); NTB deferred | Avoid NTB KYC/onboarding in MVP | Business (draft) | Working | WD §3 |
| D-010 | 2026-07-31 | Insurers: **Group A** (1SB in-platform) vs **Group B** (catalogue + redirect) | Dual commercial model | Business (draft) | Working | WD §§5–6 |
| D-011 | 2026-07-31 | Consent **mandatory**; exact sequencing pending regulatory R&D | Compliance > UX optimisation | Business (draft) | Working | WD §9 |
| D-012 | 2026-07-31 | Figma = **reference only**, not SoT | Mixed MVP + concept + future | Business (draft) | Working | WD §15 |
| D-013 | 2026-07-31 | Platform deps: SSO, bank notifications, AU Bank PG; Lead in-platform → Sampath later | Bank systems of record | Business (draft) | Working | WD §14 |
| D-014 | 2026-07-31 | Until compliance validated, engineering uses **configurable policy-driven controls** | Avoid hard-coded regulatory assumptions | Business (draft) | Working | WD §16 |
| D-015 | 2026-09-28 | Suitability lock, Eligible Lead and reassignment block occur on **successful final BI** | Rajal Q3 on the 2026-09 pack (C1/C2) | Rajal | Working | [alignment §8](./requirements/BRD-ALIGNMENT-2026-09-28.md#8-rajal-answers-2026-09-28) |
| D-016 | 2026-09-28 | Insurance RM is selected at **lead creation**. Every Bank SP is an RM; not every RM is an SP | Rajal Q4. SP is a certification, not a second actor | Rajal | Working | [alignment §8](./requirements/BRD-ALIGNMENT-2026-09-28.md#8-rajal-answers-2026-09-28) |
| D-017 | 2026-09-28 | Product intent: Compare/Buy of a product **outside** the suitable set is allowed | Rajal Q5. **Board 6 must confirm** before implementation; standing suitability gate stays until then | Rajal (intent) · Shailja (permissibility outstanding) | **Pending compliance** | [alignment §8](./requirements/BRD-ALIGNMENT-2026-09-28.md#8-rajal-answers-2026-09-28) |
| D-018 | 2026-09-30 | Lead may be **created by any allowed workforce creator** (Lead BRD §5: Bank SP, Bank Non-SP, Insurance RM / FLS). After create it **must** be assigned to a Bank RM who holds a currently valid SP certification for the LOB. DIY customer, MIS, admin and implicit BFF mint remain forbidden | Closes `OPEN-LEAD-ACTOR` on EPIC-005. Amends `INV-LED-04` / `ADR-005` create-actor clause via `ADR-021`. **Board 6 must confirm** Insurance RM (IPR) create vs solicitation before that path is runtime-enabled | Rajal (behaviour) · Mahesh (structure) · Shailja (IPR permissibility outstanding) | Working · **IPR path pending compliance** | `SUG-20260930-ola` · [ADR-021](../platform/architecture-review/08-architecture-decision-log.md) · Lead BRD §5/§8 |
| D-019 | 2026-10-02 | Lead journey sequence (amended 2026-10-03 — **BRD wins**): (1) search + product class → (2) **dedupe** in creator’s bucket (`user + Customer ID + product type`; BI release) → (3) **create / Save Lead** (no exception evaluation on Save) → (4) **Start Onboarding** runs validation/exception (Block \| Hold-for-approval \| Pass) → (5) **assignment** of mandatory certified-SP AU Bank RM + optional meeting (Lead BRD Screen 7). Further journey processing only by AU employee SP or Insurance RM/FLS. Dedupe UI when BI absent: **Continue or Cancel only** (Table 18 — no delete/replace) | Closes `OPEN-LEAD-DUP-DELETE` and `OPEN-LEAD-VAL-TIMING` in favour of Lead BRD + Exception BRD. Refines `D-018` / `SUG-20261002-lfs` | Rajal | Working | `SUG-20261002-lfs` · `SUG-20261003-brf` · EPIC-005 |
| D-020 | 2026-10-05 | Lead identity is a **bank-minted Crockford ULID** (`leadId`, 26 chars). It is opaque, time-sortable and **never encodes** CIF, branch, product, PAN or actor. It is minted once on successful create and is immutable for the whole buying journey (`BR-LEAD-001/002`, `ID-01`). A sequential display label such as Figma `RR 2024-001` is **not** a second identity in R0 (`OPEN-LEAD-DISPLAY` remains omitted). If MIS/support later need a human label, it is a reporting projection, not aggregate identity | Future-proofing without leaking attributes into the key. Time-order supports inbox and support lookup. Encoding branch/product would break reassignment and PII isolation | Rajal | Working | `SUG-20261005-lbf` · EPIC-006 · `ID-01` |
| D-021 | 2026-10-05 | **Journey stages:** Lead owns pre-BI working states (dashboard **New** = `NEW`/`ASSIGNED`/`CONTACTED` + configurable activity status), **Quote Generated** (`QUALIFIED` / first BI), **Closed** (`DISQUALIFIED`) and Policy Issued as an outcome (`CONVERTED`/`ARCHIVED`). Proposal Form Pending through Policy Declined are **Journey / Proposal / Policy projections**, never Lead transitions. **Vertical** is an optional bank-master filter between Branch and SP (Lead BRD Table 9), not a mandatory assignment field when Branch→SP mapping is complete. **Mobile search** in R0 is India `+91`, 10 digits `^[6-9]\\d{9}$` via `GET /country-codes` | Closes `OPEN-LEAD-STAGE`. Stops UW-queue states being copied onto the Lead aggregate (Journey-owns-refs) | Rajal | Working | `SUG-20261005-lbf` · EPIC-006 · Lead BRD §9.1 / §14.1 |
| D-022 | 2026-10-21 | **Suitability question sets and matrices are per product class.** Each class asks only the inputs its outcome rules consume: **Term** = protection set (income, dependants, existing cover, liabilities, premium, tobacco, occupation, prior decline, replacement) — **no investment horizon, no risk attitude**; Savings adds objective + horizon; ULIP adds objective + horizon + risk attitude. C1 applies to every class: no quote without a permitting evaluation for that class. Sets and matrices are versioned configuration by `(lob, productClass)` | Completes `BR-SUIT-002` ("a separate Term question set will be documented later"); removes irrelevant questions from the Term conversation without weakening the gate | Mahesh (repository owner, HUMAN) · Shailja / bank Compliance (content sign-off outstanding) | Working | `SUG-20261021-sui` · [rule pack §2.3](./rule-packs/suitability-rule-pack.md#23-question-sets-by-product-class-v11-amendment--d-022) |
| D-023 | 2026-10-21 | **Customer identity:** `customerId` is a platform ULID (PK of `customer.customer`, minted by customer-service on first resolve); the core-banking **CIF** is the business key, stored only in `customer` (encrypted + HMAC lookup, unique). Every other context references `customerId` only. | Two ids exist and must not be confused: our reference key and the bank's CIF | Mahesh (owner) | Accepted | [decisions](../platform/architecture-review/DECISIONS-2026-10-21.md) |
| D-024 | 2026-10-21 | **Consent timing:** consent is checked — and captured only if missing or expired — **after suitability, before multi-quote** (checkpoint `BEFORE_MULTI_QUOTE`: CNS-DP, CNS-SOL, new CNS-QSH); valid **90 days** by default; checkpoints, purposes and validity are configuration (`CONSENT.POLICY`, ADR-024). CNS-SHR stays before proposal submit. **DPDP permissibility awaits Compliance.** | Owner process: suitability first, then consent before data goes to insurers for quotes; reuse avoids re-prompting | Mahesh (owner) | Accepted — DPDP permissibility pending Compliance | [decisions](../platform/architecture-review/DECISIONS-2026-10-21.md) |
| D-025 | 2026-10-21 | **Post-payment record:** at `PaymentReconciled` the sale is written to structured Postgres — all offers shown, the selected one, every proposal question and answer (`proposal.proposal_answer`), payment type and time; afterwards insurer-decision statuses are **appended** (`policy_status_history`) from 1SB polling or callback until issued / declined, customer notified; insurer decision TAT **7 days** (IRDAI Master Circular 2024) is configuration (`PROPOSAL.TAT`). Quote stays `SELECTED` until materialise. | The full sale must be reconstructable from structured data once money has moved; statuses append, never overwrite | Mahesh (owner) | Accepted — TAT figure pending Compliance | [decisions](../platform/architecture-review/DECISIONS-2026-10-21.md) |
| D-026 | 2026-10-22 | **Payment only after insurer underwriting approval** (`UW_APPROVED → AWAITING_PAYMENT`); amends D-025. | Money is taken only for a proposal the insurer has accepted | Mahesh (owner) | Accepted | [round 2](../platform/architecture-review/DECISIONS-2026-10-22.md) |
| D-027 | 2026-10-22 | **Partner users are created directly by a bank admin** (no approval step; maker-checker kept behind configuration, default off). **Lead exception approvals only by bank employees in the SP hierarchy**; partner users never approve. | Admin owns partner onboarding; exception authority stays with the bank | Mahesh (owner) | Accepted | [round 2](../platform/architecture-review/DECISIONS-2026-10-22.md) |
| D-028 | 2026-10-22 | **Every business value is configuration**, scoped per line of business where it varies (lead expiry e.g. LIFE 90 days; exception thresholds; snapshot freshness; TAT; timeouts). Compliance posture for open questions: strict default, relaxable by configuration. | Future change without code; nothing crosses compliance | Mahesh (owner) | Accepted | [round 2](../platform/architecture-review/DECISIONS-2026-10-22.md) |
| D-029 | 2026-10-31 | **Workforce login has exactly two user types: a bank employee and a non-bank employee.** Screen labels stay **AU Bank RM** and **Insurance Partner**. API values are `BANK_RM` and `INSURANCE_RM`. Domain values are `BANK_EMPLOYEE` and `INSURER_REPRESENTATIVE`. Insurance RM, FLS, insurer partner representative and IPR are names for the non-bank employee. Specified Person is a certification on a bank employee, not a login type. Customer login is outside this workforce login. No third login user type is added. | The Login BRD v3.1 screen and `WorkforceLoginUser` already have two values. This records the owner's names so a job title is not modeled as another person who can sign in. | Repository owner | Accepted | Login BRD §5 · [API flows](../journey-execution/flows/LOGIN-BRD-V31-API-FLOWS.md) · [auth SSOT](../platform/authentication-authorization/README.md) · [screens](../figma/login-brd-v3.1/README.md) |

---

## Documentation / process decisions (`DOC-xxx`)

| ID | Date | Decision | Status | Links |
|----|------|----------|--------|-------|
| DOC-001 | 2026-07-31 | Product documentation reset under `docs/au-bank-insurance-platform/` | Accepted | [README](./README.md) |
| DOC-002 | 2026-07-31 | Prefer bank-owned capabilities with pluggable integrations (opening stance) | Accepted | [PO opening](./06-po-opening-position.md) |
| DOC-003 | 2026-07-31 | BRD overview headings (Login → Commission/MIS) are PO-approved chapter map | Accepted | [BRD Overview](./requirements/BRD-OVERVIEW.md) |
| DOC-004 | 2026-07-31 | Working Decisions Draft v1 adopted as **working** SSOT for MVP scope (pending formal sponsor validation) | Working | [Working Decisions](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) |
| DOC-005 | 2026-09-28 | September 2026 Rajal detailed module BRD pack is the **working module-behaviour SSOT** for the chapters it covers. Programme `D-xxx` in Working Decisions stand until Rajal overturns them. TBDs and cross-BRD conflicts stay open — [alignment](./requirements/BRD-ALIGNMENT-2026-09-28.md). Application code not started. | Working | [brd-detailed/](./requirements/brd-detailed/README.md) · `SUG-20260928-brd` |
| DOC-006 | 2026-09-28 | **This git repository is Java + docs only.** Client application source (NIP-APP / Flutter) is not maintained here. `ADR-015` is **not** overturned — one workforce client still exists; its source lives elsewhere. BFF contracts are evaluated against [`docs/figma/`](../figma/README.md) as the frontend reference. Figma remains not behaviour SSOT (`D-012`). Login first-module work continues on `workforce-access-bff`, not an in-repo app. | Working | `SUG-20260928-jvo` · [Login BFF vs Figma](./requirements/LOGIN-BFF-FIGMA-EVALUATION.md) |

---

## Open validation items (not closed)

| Topic | Status | Tracks |
|-------|--------|--------|
| Exact IRDAI consent model | Pending validation | D-011, GAP-006 |
| RBI + IRDAI compliance mapping | Pending validation | WD §16 |
| Corporate Agency obligations | Pending validation | WD §16 |
| Insurance advisor identity model | Pending validation | D-008, GAP-014 |
| PII retention period | Pending validation | GAP-017 |
| Data residency requirements | Pending validation | GAP-017 |
| Audit log retention | Pending validation | GAP-017 |
| Executive sponsor **name** | Pending confirmation | GAP-010 (role known) |
| Branch kiosk journey | Pending business decision | WD §20 |

---

## Supersession note

Earlier drafts of this file used `D-001`… for documentation meta-decisions. Those IDs **collided** with Session / Working Decisions business IDs. Meta items are now `DOC-xxx`. Cite **only** this file + Working Decisions for `D-xxx`.
