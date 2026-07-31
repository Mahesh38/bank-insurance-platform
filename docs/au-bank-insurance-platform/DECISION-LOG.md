# Decision log — AU Bank Insurance Platform

| ID | Date | Decision | Rationale | Decided by | Status | Links |
|----|------|----------|-----------|------------|--------|-------|
| D-000 | 2026-07-31 | Product documentation reset under `docs/au-bank-insurance-platform/` | Prior 1SB docs are research/integration, not bank product SoT | Product (agent-assisted) | Accepted | [README](./README.md) |
| D-001 | 2026-07-31 | Prefer bank-owned capabilities with pluggable integrations | Avoid coupling business to 1SB forever | Product (opening) | Accepted | [PO opening](./06-PO-OPENING-POSITION.md) |
| D-002 | 2026-07-31 | Capture BRD overview checklist (Login → Commission/MIS) as BA checklist | Align BA work to PO module headings | Product | Accepted | [BRD Overview](./requirements/BRD-OVERVIEW.md) |
| D-003 | 2026-07-31 | **Working Decisions Draft v1** adopted as working SSOT for MVP scope | Discovery clarifications freeze LOB, journeys, ETB, sale, insurer strategy | Business / Product (draft) | **Working — Pending formal validation** | [Working Decisions](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) |
| D-004 | 2026-07-31 | MVP LOB = Life only (Term, ULIP, Savings/Investment) | Phase 1 focus | Business (draft) | Working | WD §1 |
| D-005 | 2026-07-31 | Day 1 journeys = RM-assisted + Self-service + Hybrid | Seamless mode switching required | Business (draft) | Working | WD §2 |
| D-006 | 2026-07-31 | MVP customers = ETB only (any AU Bank relationship) | NTB deferred | Business (draft) | Working | WD §3 |
| D-007 | 2026-07-31 | “Policy Sold” = issued + bank confirmation + reconcilable + ops-trackable | Quotes/proposals/payments alone ≠ sale | Business (draft) | Working | WD §4 |
| D-008 | 2026-07-31 | Insurers: Group A (1SB integrated) vs Group B (catalogue + redirect) | Dual commercial model | Business (draft) | Working | WD §§5–6 |
| D-009 | 2026-07-31 | Need analysis + suitability mandatory before quote; no bypass | Regulatory / suitability / audit | Business (draft) | Working | WD §8 |
| D-010 | 2026-07-31 | Consent mandatory; exact sequencing pending regulatory R&D | Compliance > UX optimisation | Business (draft) | Working | WD §9 |
| D-011 | 2026-07-31 | Bank PG only; Lead in Insurance Platform (→ Sampath later); SSO + bank notifications | Platform dependencies | Business (draft) | Working | WD §14 |
| D-012 | 2026-07-31 | Figma = reference only, not SoT | Incomplete / mixed MVP+future | Business (draft) | Working | WD §15 |
| D-013 | 2026-07-31 | 1SB = current integration layer / accelerator; core capabilities must not be tightly coupled | Replaceable by another aggregator or bank-owned layer | Business (draft) | Working | WD §§18–19 |
| D-014 | 2026-07-31 | Until compliance validated, engineering uses configurable policy-driven controls | Avoid hard-coded regulatory assumptions | Business (draft) | Working | WD §16 |

## Open decisions (from Working Decisions)

| Topic | Status |
|-------|--------|
| Exact IRDAI consent model | Pending validation |
| RBI + IRDAI compliance mapping | Pending validation |
| Corporate Agency obligations | Pending validation |
| Insurance advisor identity model | Pending validation |
| PII retention period | Pending validation |
| Data residency requirements | Pending validation |
| Audit log retention | Pending validation |
| Executive sponsor name | Pending confirmation |
| Branch kiosk journey | Pending business decision |
