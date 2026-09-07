# Consolidated glossary

This page decodes the abbreviations and identifier families used across governance, architecture, delivery, insurance and code.

## First rule: prefixes live in namespaces

The same letter can mean different things depending on punctuation and context.

| Example | Meaning |
|---|---|
| `S08` | lifecycle/delivery stage 08 |
| `S-08` | architecture seam 08 |
| `SF1` | Stage Fit code 1 |
| `S08-G1` | gate criterion 1 of stage 08 |
| `G1` | automatic T4 trigger 1 when used in review-gate context |
| `SC1` | Scope Fit code 1 |
| `SC-W3-1` | standing constraint 1 for workstream 3 |
| `P1` | delivery priority 1 unless explicitly inside a board checklist |

## Governance and work identifiers

| Prefix / term | Expansion | Meaning |
|---|---|---|
| `AIGEM` | AI Governance & Execution Model | Governs admission, scope/stage fit, priority, review and execution eligibility |
| `SUG` | Suggestion | Intake record for a new idea, finding, request or proposed change |
| `CR` | Change Request | Formal change to scope, architecture, stage, governance or other controlled baseline |
| `ADR` | Architecture Decision Record | Formal record of a consequential architecture decision, alternatives and consequences |
| `DEC` | Decision | Broader decision record; may be business/domain/governance rather than architecture-only |
| `PLAN` | Implementation Plan | Files/components, approach, tests, dependencies, rollback and out-of-scope for a governed change |
| `GOV` | Governance work/decision | Governance-specific work item or decision |
| `RISK` | Risk record | Tracked project/architecture/operational risk |
| `DEP` | Dependency | Something another work item/gate depends on |
| `GAP` | Gap | Missing design, evidence, functionality or control to close |
| `TD` | Technical Debt | Known engineering compromise/deferred correction |
| `READY` | Ready queue | Approved and dependency-ready work available to pick up |
| `BLOCKED` | Blocked work | Valid work that cannot progress until a named dependency clears |
| `PARKED` | Deferred work | Valid work intentionally held until a target stage/unpark trigger |
| `REJECTED` | Rejected work | Deliberately not being done, with reason |
| `CANDIDATE` | Candidate | Proposed/assembled decision or transition not yet fully ratified |
| `ADMIT` | Admit | Put work into the appropriate current-stage backlog |
| `ADMIT-BYPASS` | Human override admission | Human explicitly overrides normal process; bypass must still be recorded |
| `PARK` | Park | Preserve the item for later with target and trigger |
| `ESCALATE` | Escalate | Raise to change control/human decision rather than silently admit or reject |

## Risk, priority, stage and scope

| Prefix | Meaning | Question |
|---|---|---|
| `T1–T4` | review/risk tier | How much review and sign-off does this change require? |
| `G1–G10` | automatic T4 trigger family | Does the proposed delta touch a critical category such as auth, PII, money, consent or production topology? |
| `P1–P5` | delivery priority | How urgently should this work be done at the current stage? |
| `SF0–SF4` | Stage Fit | Does this work belong now, next, later, or never? |
| `SC0–SC4` | Scope Fit | Is this explicitly/implicitly our problem, adjacent value, out-of-scope, or externally mandated? |
| `L0–L10` | canonical AIGEM lifecycle | Discovery through Operate & Evolve |
| `S00…` | detailed project stages | Delivery decomposition such as S08 Engineering Foundation |
| `GATE-S08` | stage gate | Evidence gate that must pass before leaving S08 |
| `S08-G1` | stage-gate criterion | One exit criterion within a stage gate |
| `E0–E4` | evidence level | Strength/maturity of evidence used by the lifecycle/stage-gate model |

## Architecture and domain identifiers

| Prefix / term | Expansion | Meaning |
|---|---|---|
| `INV-*` | Invariant | Rule that must always remain true regardless of calling path |
| `SC-W3-*` | Standing Constraint for WS-3 | Platform-level non-negotiable rule for workstream 3 |
| `S-*` | Seam | Named interaction/interface between components with explicit semantics |
| `IF-*` | Interface | Workstream or major boundary contract |
| `FF-*` | Fitness Function | Automated architecture/control test, normally enforced in CI |
| `NFR-*` | Non-Functional Requirement | Latency, availability, capacity, security, DR, observability etc. |
| `TB-*` | Trust Boundary | Crossing between zones/components with different trust assumptions |
| `ARCH-*` | Architecture rule/decision identifier | Architecture position/rule indexed in architecture material |
| `AC-*` | Context-dependent | Usually acceptance criteria; in the actor-model documents also used for actor rules |
| `ID-*` | Identity/domain rule identifier | Identity/actor modelling rule family in architecture documents |
| `CAP-*` | Capability identifier | Business/platform capability reference |
| `CTRL-*` | Control identifier | Compliance/security/business control reference |
| `VR-*` | Validation Rule | Executable validation/decision rule in journey documentation |
| `UC-*` | Use Case | Named use case / flow |

### Common invariant families

| Example | Domain |
|---|---|
| `INV-QUO-*` | Quotation |
| `INV-PRP-*` | Proposal |
| `INV-PAY-*` | Payment |
| `INV-POL-*` | Policy |
| `INV-JRN-*` | Journey |
| `INV-CNS-*` | Consent |
| `INV-SUI-*` | Suitability |
| `INV-LED-*` | Lead |
| `INV-AUD-*` | Audit |
| `INV-DIS-*` | Distributor / attribution |
| `INV-ACT-*` | Actor/action |
| `INV-CFG-*` | Configuration |

## Workstreams, waves and releases

| Term | Meaning |
|---|---|
| `WS-1` | 1SB Insurance Integration supplier workstream |
| `WS-2` | Workforce Authentication & Authorization enabler workstream |
| `WS-3` | AU Bank Insurance Distribution Platform primary workstream |
| `W0…W4` | Architecture build waves inside R0; not lifecycle stages |
| `R0, R1, R2…RN` | Product/architecture releases/horizons when used in architecture context |
| `R1…R13` | Governance role IDs when used in the Runbook/role context; context matters |

## Application and architecture terms

| Term | Expansion / meaning |
|---|---|
| `NIP` | New Insurance Platform in ADR-015 / NIP-APP naming; one newer ARB dossier uses “National Insurance Platform”, which should be normalized |
| `NIP-APP` | Unified workforce Flutter application for RM, partner representative, admin and operations perspectives |
| `BFF` | Backend for Frontend |
| `HLD` | High-Level Design |
| `LLD` | Low-Level Design |
| `ARB` | Architecture Review Board |
| `SSOT` | Single Source of Truth |
| `SoT` | Source of Truth |
| `IaC` | Infrastructure as Code |
| `CI/CD` | Continuous Integration / Continuous Delivery or Deployment |
| `E2E` | End-to-End |
| `DR` | Disaster Recovery |
| `RTO` | Recovery Time Objective |
| `RPO` | Recovery Point Objective |

## Identity and security terms

| Term | Expansion |
|---|---|
| `IAM` | Identity and Access Management |
| `IdP` | Identity Provider |
| `PDP` | Policy Decision Point |
| `PEP` | Policy Enforcement Point |
| `RBAC` | Role-Based Access Control |
| `ABAC` | Attribute-Based Access Control |
| `MFA` | Multi-Factor Authentication |
| `PKCE` | Proof Key for Code Exchange |
| `mTLS` | Mutual TLS |
| `SAST` | Static Application Security Testing |
| `SCA` | Software Composition Analysis |
| `SBOM` | Software Bill of Materials |

## Insurance and banking terms

| Term | Expansion / meaning |
|---|---|
| `RM` | Relationship Manager |
| `SP` | IRDAI Specified Person qualification/certificate; an authorization attribute, not a separate actor |
| `IPR` | Insurance Partner Representative |
| `SR` | Insurer Sales Representative |
| `LOB` / `LoB` | Line of Business |
| `ETB` | Existing to Bank |
| `NTB` | New to Bank |
| `UW` | Underwriting |
| `MIS` | Management Information System / management reporting |
| `PII` | Personally Identifiable Information |
| `PPHI` | Protection of Policyholders' Interests |
| `PG` | Payment Gateway |
| `CBS` | Core Banking System |
| `CIF` | Customer Information File |
| `1SB` | 1SilverBullet integration/aggregator middleware |
| `STP` | Straight-Through Processing |
| `NON_STP` | Non-Straight-Through Processing |
| `INSTA` | Instant issuance mode used by the R0 domain model |

## How to read a dense repository sentence

Example:

> `SUG-20260825-pp1 is SF1/SC0, T4, P1, routed to CR-013 C-PPHI-1; ADR-014 must preserve C1/C2/C4 and INV-JRN-05.`

Decode it as:

- `SUG-...` — a recorded suggestion.
- `SF1` — belongs to the current stage.
- `SC0` — explicitly in scope.
- `T4` — critical review tier.
- `P1` — urgent/current blocker.
- `CR-013` — formal change request 13.
- `C-PPHI-1` — named compliance condition inside that CR.
- `ADR-014` — architecture decision record 14.
- `C1/C2/C4` — standing R0 controls.
- `INV-JRN-05` — Journey invariant governing when the journey may reach SOLD.
