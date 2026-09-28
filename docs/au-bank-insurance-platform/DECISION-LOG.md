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
