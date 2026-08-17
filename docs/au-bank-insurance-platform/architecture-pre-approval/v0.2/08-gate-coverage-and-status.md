# 08 · Gate Coverage & Approval Status

**AU Bank Insurance Distribution Platform — Architecture Pre-Approval Pack v0.2**

> ### ⛔ DRAFT FOR REVIEW · NOT APPROVED · NOT AUTHORISED FOR DEVELOPMENT

| | |
|---|---|
| **Workstream** | WS-3 · stage S08 (S09 overlapped) |
| **Risk tier** | T4 · **Evidence level** E1 |
| **Owner** | Mahesh — Principal Insurance Platform Architect |
| **Provenance** | **AI-DRAFTED**, unsigned |

---

## 1. Why this document replaces the progress summary

v0.1 reported **"architecture drafting completion: 100%"** and *"7 of 7 documents prepared"* — while
the pack contained eight documents and its own dashboard said "8 of 8".

Beyond the arithmetic, the measure was wrong. **A pack is not measured by how much of it was
written.** It is measured by which gate criteria it can close, at what evidence level, and which
ones it structurally cannot close no matter how well it is drafted. That is this document.

![Gate coverage](../../../diagrams/gate-coverage.svg)

---

## 2. S06 — Domain & Information Architecture

| # | Criterion | Level | State | Evidence, or what is missing |
|---|---|---|---|---|
| S06-G1 | Bounded context map with relationships and justifications | E2 | ✅ **Met** | [`domain-ownership.svg`](../../../diagrams/domain-ownership.svg) + [`hdl.svg`](../../../hdl.svg) — 19 contexts, owns / never-owns each |
| S06-G2 | Aggregates with state machines and invariants | E2 | ✅ **Met** | [`journey-state-machine.svg`](../../../diagrams/journey-state-machine.svg) — 17 states, 6 forbidden transitions |
| S06-G3 | Journey saga designed including compensations | E2 | ✅ **Met** | same diagram, compensation table — an `S-19` task per failing transition |
| S06-G4 | Logical data model per context | E2 | ⛔ **Open** | not in this pack. **An ownership table is not a data model** — Aarti's artefact |
| S06-G5 | Data ownership matrix complete | E1 | ✅ **Met** | [doc 03 §2](./03-domain-and-ownership-model.md) — golden source and "never owns" for all 19 |
| S06-G6 | Compliance hard-gate enforcement points named | E2 | ✅ **Met** | [`quote-path.svg`](../../../diagrams/quote-path.svg) — C1 at `#7`/`#10`, C2 at `#6`/`#11`, with `FF-08`/`FF-09` |
| S06-G7 | Audit model proven to reconstruct a journey | **E3** | ⛔ **Open** | **E3 means executed.** A diagram cannot close it; a reconstruction run can |
| S06-G8 | Canonical model is provider-neutral | E2 | ⚠️ **Partial** | asserted at the `#14` seam; the existing ArchUnit provider-isolation rule needs citing as evidence |

## 3. S07 — Solution & Security Architecture

| # | Criterion | Level | State | Evidence, or what is missing |
|---|---|---|---|---|
| S07-G1 | Target architecture documented and reviewed | E2 | ⚠️ **Partial** | documented ([`hdl.svg`](../../../hdl.svg) + this pack). **The review verdict is what is missing** |
| S07-G2 | ADRs recorded for all significant decisions | E1 | ⛔ **Open** | [doc 04 §9](./04-high-level-design.md) carries 13 decisions with **no ADR IDs**; `ARCH-001…022` exist and are not extended (`AP-7`) |
| S07-G3 | Threat model complete per trust boundary | E2 | 🔴 **Human-only** | [`trust-zones.svg`](../../../diagrams/trust-zones.svg) supplies the boundaries. **The threat model is Deepali's and no AI output can substitute** |
| S07-G4 | Security architecture approved | E2 | 🔴 **Human-only** | **nothing in this pack can satisfy this, including this line** |
| S07-G5 | Data architecture approved incl. backup and retention | E2 | ⛔ **Open** | needs the logical model, classification and retention schedule — then Aarti signs |
| S07-G6 | NFR sheet with numbers, each verifiable | E2 | ⚠️ **Partial** | [`05-nfr-catalogue.md`](../../../platform/ws3-platform/05-nfr-catalogue.md) has IDs, methods and verification stages. **Unsigned** |
| S07-G7 | R0 build order defined — minimum service set | E2 | ✅ **Met** | 12 services + 1 app, waved W1–W4 — [doc 02](./02-business-capability-map.md), [`dependency-map.svg`](../../../diagrams/dependency-map.svg) |
| S07-G8 | Fitness functions defined for automatable constraints | E1 | ⚠️ **Partial** | `FF-01…FF-15` referenced throughout; **the consolidated list belongs in one citable place** |

## 4. The honest tally

| | Count | |
|---|---|---|
| ✅ Met | **6 of 16** | evidence cited and openable |
| ⚠️ Partial | **4 of 16** | one identified thing away each |
| ⛔ Open | **4 of 16** | belong to Aarti, or need an executed run |
| 🔴 Unclosable by any AI | **2 of 16** | S07-G3 and S07-G4 — human Security signature |

**Approval recorded: none. By any board. This is the accurate number and it is zero.**

## 5. What the pack contains

| Documents | 8 Markdown · diffable, line-commentable, rendered on GitHub |
| --- | --- |
| Diagrams | 11 SVG, generated from 4 committed Python sources |
| Authoritative HLD | [`docs/hdl.svg`](../../../hdl.svg) — hand-authored, referenced not replaced |
| Review record | [360° stakeholder review](../reviews/PR-55-360-STAKEHOLDER-REVIEW-v1.0.md) — 68 findings, 11 corrections |

## 6. What remains before approval

| Activity | Named human | Blocks |
|---|---|---|
| Sign the consent and suitability rule packs | **Shailja** | **S11 entry — non-waivable (DEC-20260816-06)** |
| Threat model per trust boundary, then sign it | **Deepali** | S07-G3, S07-G4 |
| Logical data model, classification, retention schedule | **Aarti** | S06-G4, S07-G5 |
| Prove journey reconstruction from audit evidence | Aarti + Swapnali | S06-G7 (E3) |
| Write ADRs for the 13 design decisions | Mahesh | S07-G2 |
| Consolidate the fitness-function list | Mahesh + Swapnali | S07-G8 |
| Sign the NFR sheet | Mahesh + Shivanshi + Rajal | S07-G6 |
| Confirm R0 scope and republish `R0-SCOPE` v0.4 | **Rajal** | condition C4 |
| Set dependency dates and escalation triggers | **Kalpana** | AR-02 |
| Seat a Finance signatory | Kalpana | D-08, AR-04 |
| Seat an Executive Sponsor | — | **GAP-010 · `FRI-001` funding** |
| Declare the canonical approval artefact across PRs #32 / #54 / #55 / #56 | Mahesh | everything downstream |

## 7. Approval status

| State | Meaning |
|---|---|
| **Draft for Review** | prepared for stakeholder challenge |
| **Not Approved** | no board verdict recorded, by any board |
| **Not Authorised for Development** | development authorisation is outside this pack |

## 8. Signature ledger

As [doc 01 §8](./01-solution-vision.md#8-signature-ledger). All seats `Pending`; Finance and
Executive Sponsor **unfilled**.

## 9. Version history

| Version | Date | Change | State |
|---|---|---|---|
| 0.1 | 2026-08-17 | "Architecture Work Progress Summary" (DOCX) — reported 100% drafting, and 7-of-7 against 8 documents | Superseded |
| 0.2 | 2026-08-17 | Replaced the drafting percentage with per-criterion gate coverage; corrected the count; separated "open" from "unclosable by an AI". Answers F-38, C-7 | **Draft for review** |
