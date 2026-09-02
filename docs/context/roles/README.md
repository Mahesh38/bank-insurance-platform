# Project Role Context

**Parent:** [`docs/context/`](../README.md)

**Machine index:** [`../context-manifest.yaml`](../context-manifest.yaml)

**Authority:** [`PERSONA-AUTHORITY-MATRIX.md`](../../governance/PERSONA-AUTHORITY-MATRIX.md)

This folder contains project/domain role instances — **tier 2**. These are the full packages,
28 KB to 244 KB each.

> **Do not load a package to adopt a persona.** Read its
> [decision card](../personas/README.md) (3–6 KB) and open one package file only when a card's
> *Load deeper* row matches the question in front of you.

The manifest is the canonical machine index; this page is the human navigation view. Role context
cannot enlarge its own authority.

## Canonical packages

| Decision lens | Card (tier 1) | Package (tier 2) |
|---|---|---|
| Product — Rajal / R1 | [card](../personas/rajal-product.card.md) | [`principal-insurance-platform-product-owner/`](./principal-insurance-platform-product-owner/README.md) |
| Business Analysis / R11 | [card](../personas/ba-r11-business-analysis.card.md) | [`principal-insurance-platform-business-analyst/`](./principal-insurance-platform-business-analyst/README.md) |
| Architecture — Mahesh / R2 | [card](../personas/mahesh-architecture.card.md) | [`mahesh-principal-insurance-platform-architect/`](./mahesh-principal-insurance-platform-architect/README.md) |
| Engineering — Amit / R3 | [card](../personas/amit-engineering.card.md) | [`amit-technical-head.md`](./amit-technical-head.md) |
| Security — Deepali / R8 | [card](../personas/deepali-security.card.md) | [`deepali-principal-security-architect/`](./deepali-principal-security-architect/README.md) |
| Database — Aarti | [card](../personas/aarti-database.card.md) | [`principal-insurance-data-database-architect/`](./principal-insurance-data-database-architect/README.md) |
| Quality — Swapnali / R7 | [card](../personas/swapnali-qa.card.md) | [`swapnali-qa-lead/`](./swapnali-qa-lead/README.md) |
| Compliance/Risk — Shailja / R9 | [card](../personas/shailja-compliance.card.md) | [`shailja-s-compliance-risk-head/`](./shailja-s-compliance-risk-head/README.md) |
| SRE/Operations — Shivanshi / R10 | [card](../personas/shivanshi-sre.card.md) | [`shivanshi-sre/`](./shivanshi-sre/README.md) |
| Delivery — Kalpana / R12 | [card](../personas/kalpana-delivery.card.md) | [`kalpana-delivery-head/`](./kalpana-delivery-head/README.md) |

The manifest records whether each governance mapping is `active`, `candidate` or `retired` and
links the governing decision. “Canonical package” means one maintained context source; it does
not convert a candidate governance assignment into ratification.

### Candidate / supporting compliance package

| Role | Package | Note |
|---|---|---|
| Compliance Officer — Vaishnavi | [`vaishnavi-compliance-officer.md`](./vaishnavi-compliance-officer.md) | Supporting pack from the process-realignment war-room branch. **Board 6 / R9 remains Shailja.** Vaishnavi's file is working material for IRDAI CA0515 licence-holder review scope; it does not create a second compliance board or replace Shailja's card. |

> Vaishnavi's pack differs from the other packages: sections 1–2 are role-derived rather than
> self-reported, and §7 lists what must be confirmed with her directly. A compliance verdict on a
> regulated item remains binding and cannot be waived
> ([14 §1](../../governance/14-CHANGE_CONTROL.md#1-what-needs-a-change-request)).

## Shared protocols

Load [`shared/cross-persona-operating-model.md`](./shared/cross-persona-operating-model.md) for a
consequential multi-role decision, plus only the affected specialist protocol:

- [Product / Architecture / Compliance](./shared/product-architecture-compliance-decision-protocol.md)
- [Architecture / Compliance](./shared/architect-compliance-decision-protocol.md)
- [Security cross-persona](./shared/security-cross-persona-decision-protocol.md)
- [SRE cross-persona](./shared/sre-cross-persona-decision-protocol.md)
- [Delivery cross-persona](./shared/delivery-cross-persona-decision-protocol.md)

## Maintenance rules

- One canonical package per role; aliases are redirects only.
- New role packages require a named consumer, distinct decision surface and authority boundary.
- Project/domain facts belong in overlays, not the reusable framework.
- Do not load every persona by default; use the manifest loading profile and affected domains.
- Mandatory human signatures remain human even when an AI simulates the reasoning checklist.
