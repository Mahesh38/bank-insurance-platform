# Project Role Context

**Parent:** [`docs/context/`](../README.md)

**Machine index:** [`../context-manifest.yaml`](../context-manifest.yaml)

**Authority:** [`PERSONA-AUTHORITY-MATRIX.md`](../../governance/PERSONA-AUTHORITY-MATRIX.md)

This folder contains project/domain role instances. The manifest is the canonical machine index;
this page is the human navigation view. Role context cannot enlarge its own authority.

## Canonical packages

| Decision lens | Package |
|---|---|
| Product — Rajal / R1 | [`principal-insurance-platform-product-owner/`](./principal-insurance-platform-product-owner/README.md) |
| Business Analysis / R11 | [`principal-insurance-platform-business-analyst/`](./principal-insurance-platform-business-analyst/README.md) |
| Architecture — Mahesh / R2 | [`mahesh-principal-insurance-platform-architect/`](./mahesh-principal-insurance-platform-architect/README.md) |
| Engineering — Amit / R3 | [`amit-technical-head.md`](./amit-technical-head.md) |
| Security — Deepali / R8 | [`deepali-principal-security-architect/`](./deepali-principal-security-architect/README.md) |
| Database — Aarti | [`principal-insurance-data-database-architect/`](./principal-insurance-data-database-architect/README.md) |
| Quality — Swapnali / R7 | [`swapnali-qa-lead/`](./swapnali-qa-lead/README.md) |
| Compliance/Risk — Shailja / R9 | [`shailja-s-compliance-risk-head/`](./shailja-s-compliance-risk-head/README.md) |
| SRE/Operations — Shivanshi / R10 | [`shivanshi-sre/`](./shivanshi-sre/README.md) |
| Delivery — Kalpana / R12 | [`kalpana-delivery-head/`](./kalpana-delivery-head/README.md) |

The manifest records whether each governance mapping is `active`, `candidate` or `retired` and
links the governing decision. “Canonical package” means one maintained context source; it does
not convert a candidate governance assignment into ratification.

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
