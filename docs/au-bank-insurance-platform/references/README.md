# References (non-binding)

**Up:** [docs index](../../README.md) → [AU Bank platform](../README.md) → **references**

This folder points at materials that inform the AU Bank product reset.  
**Nothing here is approved scope** until listed as Working/Accepted in the [Decision Log](../DECISION-LOG.md) or [Working Decisions](../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md).

**Where each input was logged:** [05-figma-and-artefact-intake.md](../05-figma-and-artefact-intake.md) is the intake record for everything listed below — Figma walkthrough status and the uploaded baseline documents.

## In-repo research (engineering / 1SB package)

| Link | Use in discovery |
|------|------------------|
| [../../1sb-insurance-integration/README.md](../../1sb-insurance-integration/README.md) | Package index |
| [../../1sb-insurance-integration/01-executive-overview.md](../../1sb-insurance-integration/01-executive-overview.md) | Aggregator mental model |
| [../../1sb-insurance-integration/journeys/universal-lob-journey.md](../../1sb-insurance-integration/journeys/universal-lob-journey.md) | Stage ideas to challenge with Figma |
| [../../1sb-insurance-integration/02-rm-assisted-bank-checklist.md](../../1sb-insurance-integration/02-rm-assisted-bank-checklist.md) | Checklist → discovery questions |
| [../../1sb-insurance-integration/service-ssot/00-po-architect-design-session.md](../../1sb-insurance-integration/service-ssot/00-po-architect-design-session.md) | Prior PO↔Architect decisions — **reopen for AU Bank** |

## Stakeholder architecture input (transcribed, non-binding)

| Link | Notes |
|------|-------|
| [2026-08-20-north-star-architecture-brainstorming-notes.md](./2026-08-20-north-star-architecture-brainstorming-notes.md) | `VIN-001` — North Star capability model, LOB/journey segregation, orchestration and shared-capability notes. **Reference only.** Reconciled against accepted decisions in [`10-north-star-capability-model.md §9`](../../context/roles/mahesh-principal-insurance-platform-architect/10-north-star-capability-model.md) |
| [2026-08-20-insurance-aggregation-and-provider-connectivity-notes.md](./2026-08-20-insurance-aggregation-and-provider-connectivity-notes.md) | `VIN-002` — bank insurance aggregation layer, provider routing, canonical contract scoping, fan-out isolation, callback ingress. **Reference only.** Reconciled in [`17-provider-aggregation-and-connectivity.md §17`](../../context/roles/mahesh-principal-insurance-platform-architect/17-provider-aggregation-and-connectivity.md) |
| [2026-09-04-universal-insurance-suitability-specification.md](./2026-09-04-universal-insurance-suitability-specification.md) | `SUG-20260904-uis` — 7-layer cross-insurer suitability model (KLI, HDFC, Bajaj, LIC, Bharti AXA, IPRU), 206-product catalogue attributes, 8,811-case source-fidelity suite. **Research only.** Does **not** supersede [`suitability-rule-pack.md`](../rule-packs/suitability-rule-pack.md). Engine/catalogue implementation is parked as `SUG-20260904-eng`. |

## External

| Link | Notes |
|------|-------|
| [Figma — For Client Review](https://www.figma.com/proto/JyLGAaO88ELjnyVF2FQ3Bx/For-Client-Review?node-id=208-9666&page-id=208%3A2982) | UX **reference only** (D-012) — not SoT |
| [1SB Insurance Gateway API](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/insurance-gateway-api) | Vendor capability reference |

## Local artefact dirs

| Path | Purpose |
|------|---------|
| [`../artefacts/` — Uploads](../artefacts/README.md#uploads) | Drop zone for baseline docs you upload — **11 PDFs ingested** into [knowledge-base/](../knowledge-base/README.md) |
| [`../artefacts/` — Figma](../artefacts/README.md#figma) | Optional Figma exports — 🔴 **empty**, prototype is login-gated (D-012) |

## Cross-cutting architecture (outside this folder)

| Link | Use in discovery |
|------|------------------|
| [`../../platform/architecture-review/README.md`](../../platform/architecture-review/README.md) | Target AWS/EKS platform architecture — **recommendation**, tracked as `ARCH-xxx` |
| [`../../platform/authentication-authorization/README.md`](../../platform/authentication-authorization/README.md) | Approved workforce identity/authorization baseline |
