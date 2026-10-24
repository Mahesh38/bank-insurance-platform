# Architecture Pre-Approval Pack

**Programme:** AU Small Finance Bank — Insurance Distribution Platform
**Workstream:** WS-3 (primary) · canonical stage **S08 — Engineering Foundation**, S09 overlapped

> ### ⛔ NOT APPROVED · NOT AUTHORISED FOR DEVELOPMENT
> Nothing in this folder carries a human signature. Security (Board 4) and Risk & Compliance
> (Board 6) verdicts at T4 are **human-only** and cannot be satisfied by any AI-generated content
> here, including the review record.

## What this pack is

A stakeholder-review presentation of the platform architecture: eight documents and eleven
diagrams that take a reviewer from "why does this exist" to "which gate criteria can actually be
closed, and by whom".

**It is not the architecture SSOT.** It presents one. Where this pack and the baseline below
differ, the baseline wins and the difference is a defect here.

| Authoritative source | What it holds |
|---|---|
| [`docs/hdl.svg`](../../hdl.svg) | **The R0 high-level design** — hand-authored, and the visual standard this pack is built to |
| [`platform/architecture-review/01–08`](../../platform/architecture-review/README.md) | Target architecture, AWS infrastructure, data architecture, NFRs, **ADRs ARCH-001…ARCH-022** |
| [`platform/ws3-platform/00–05`](../../platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md) | WS-3 registration, domain model, information model, R0 build order, security architecture, NFR catalogue |
| [S06](../../application-lifecycle-bible/evidence/S06-domain-architecture-evidence.md) · [S07](../../application-lifecycle-bible/evidence/S07-solution-architecture-evidence.md) evidence | The retroactive gate position |

## Current version — v0.2

| # | Document | Covers | Gate criteria |
|---|---|---|---|
| 01 | [Solution Vision](./v0.2/01-solution-vision.md) | why, for whom, the five commitments, where R0 stops | S07-G1 ⚠️ |
| 02 | [Business Capability Map](./v0.2/02-business-capability-map.md) | every capability → owning context → wave → honest R0 position | S07-G7 ✅ · S06-G5 ✅ |
| 03 | [Domain & Ownership Model](./v0.2/03-domain-and-ownership-model.md) | 19 contexts, owns / never owns, the state machine, boundary decisions | S06-G1/2/3/5/6 ✅ |
| 04 | [High-Level Design](./v0.2/04-high-level-design.md) | context, components **incl. what already exists**, hosting, both journeys, integrations, NFRs, decisions | S07-G1 ⚠️ · G7 ✅ |
| 05 | [Security & Compliance Design Review](./v0.2/05-security-design-review.md) | zones, crossings, **regulatory obligation map**, controls, open security risks | S07-G3/G4 🔴 human |
| 06 | [Risks, Dependencies & Assumptions](./v0.2/06-risks-dependencies-assumptions.md) | scored risks with one owner each, dependencies with dates, assumptions with consequences | — |
| 07 | [Architecture Review Record](./v0.2/07-architecture-review-record.md) | the seven-board model, decisions routed to who holds them, empty discussion record | — |
| 08 | [Gate Coverage & Approval Status](./v0.2/08-gate-coverage-and-status.md) | criterion-by-criterion: met, partial, open, unclosable | all |

### Diagrams

All eleven live in [`docs/diagrams/`](../../diagrams/README.md), generated from committed Python
sources so a decision change is a one-line edit and a re-run.

| Diagram | View |
|---|---|
| [`solution-vision`](../../diagrams/solution-vision.svg) | problems → commitments → outcomes → release boundary |
| [`capability-map`](../../diagrams/capability-map.svg) | capabilities by group, wave and R0 position |
| [`domain-ownership`](../../diagrams/domain-ownership.svg) | 19 contexts — owns / never owns |
| [`system-context`](../../diagrams/system-context.svg) | externals: what crosses, under what contract, how it fails |
| [`quote-path`](../../diagrams/quote-path.svg) | lookup → C1 → C2 → quote, and the five refusals |
| [`r0-money-path`](../../diagrams/r0-money-path.svg) | proposal → payment → reconciliation → issuance → SOLD, and five failure branches |
| [`journey-state-machine`](../../diagrams/journey-state-machine.svg) | 17 states, terminal states, forbidden transitions, compensations |
| [`trust-zones`](../../diagrams/trust-zones.svg) | five zones and every boundary crossing |
| [`risk-register`](../../diagrams/risk-register.svg) | L×I exposure grid, criticals, assumptions |
| [`dependency-map`](../../diagrams/dependency-map.svg) | dependencies with owners, dates, critical path |
| [`approval-model`](../../diagrams/approval-model.svg) | the actual AIGEM seven-board model |
| [`gate-coverage`](../../diagrams/gate-coverage.svg) | S06/S07 criteria coverage |

## Review

[**360° stakeholder review of v0.1**](./reviews/PR-55-360-STAKEHOLDER-REVIEW-v1.0.md) — twelve
lenses, 68 findings, 11 factual corrections. v0.2 was rebuilt against it; each document's version
history names the findings it answers.

## What changed from v0.1 → v0.2

| | v0.1 | v0.2 |
|---|---|---|
| Format | 8 DOCX + 11 PNG (binary) | 8 Markdown + 11 SVG from committed sources — diffable and line-commentable |
| Existing estate | invisible — read as greenfield | every component mapped to what exists in `services/` and `apps/` |
| `ARCH-004` vs `bank-persistence-service` | unmentioned contradiction | reconciled explicitly as **B-04**, with an ADR requested |
| Decisions | 12, no ADR IDs, most "Pending" | cited where already decided (DEC-20260816-03/-07/-12); ADR gap declared |
| Regulations | **none named** | REG-1…REG-7 mapped to controls and owners |
| NFRs | restated numbers | cited by ID from the NFR catalogue; RPO conflict surfaced |
| Journey | 14 prose steps | state machine + two flow diagrams with failure branches |
| Risks | `R-01…R-18`, group ownership, no dates | scored on the repo scale, one owner each, escalation triggers |
| Approval model | five-step CTO flow | the seven-board AIGEM model; CTO authority raised as a question |
| Progress | "100% drafting complete" | per-criterion gate coverage: 6 met, 4 partial, 4 open, **2 unclosable by any AI** |
| Signatories | role labels | named humans, plus the two **unfilled seats** (Finance, Executive Sponsor) |

## Open before this pack can be approved

1. **Declare the canonical approval artefact** across PRs #32, #54, #55 and #56 — three formats,
   three locations, no declaration.
2. **Shailja** signs the consent and suitability rule packs — non-waivable S11 entry.
3. **Deepali** produces the per-boundary threat model, then signs S07-G3/G4.
4. **Aarti** produces the logical data model, classification and retention schedule.
5. **Kalpana** sets dependency dates; **seat Finance**; escalate **GAP-010** (no Executive Sponsor,
   `FRI-001` funding blocked).
