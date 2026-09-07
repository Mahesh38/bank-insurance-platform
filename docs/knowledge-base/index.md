# Bank Insurance Platform Knowledge Hub

> **Purpose:** one navigable entry point for understanding the repository without creating a second source of truth.

This portal **indexes and explains** the existing authoritative documentation. If a portal summary disagrees with an authoritative source, the authoritative source wins.

<div class="kb-grid">
  <div class="kb-card">
    <h3>New to the project?</h3>
    <p>Start with the business problem, current state, R0 architecture and your role-specific path.</p>
    <p><a href="role-guides/">Start by role →</a></p>
  </div>
  <div class="kb-card">
    <h3>What is happening now?</h3>
    <p>Understand stages, gates, current workstreams, admitted work, parked work and unresolved decisions.</p>
    <p><a href="status-and-decisions/">Current state & decisions →</a></p>
  </div>
  <div class="kb-card">
    <h3>What does this abbreviation mean?</h3>
    <p>Decode AIGEM, T1–T4, G1–G10, P1–P5, C1–C8, SF, SC, INV, SUG, CR, ADR, seams, gates and domain terminology.</p>
    <p><a href="glossary/">Open glossary →</a></p>
  </div>
  <div class="kb-card">
    <h3>Where is the documentation?</h3>
    <p>Navigate governance, business, architecture, journey, 1SB, identity, data and role documents.</p>
    <p><a href="repository-map/">Repository map →</a></p>
  </div>
  <div class="kb-card">
    <h3>Services & APIs</h3>
    <p>See target bounded contexts, currently implemented backend modules and documented API contracts.</p>
    <p><a href="api-service-catalogue/">API/service catalogue →</a></p>
  </div>
</div>

## The five-layer mental model

| Layer | Question | Start here |
|---|---|---|
| **Business** | What problem are we solving and what behaviour do we promise? | [Business problem](../context/business-problem-statement.md) · [Business documentation](../au-bank-insurance-platform/README.md) |
| **Domain** | Which bounded context owns each piece of truth and which rules must always hold? | [Domain model & invariants](../platform/ws3-platform/01-domain-model-and-invariants.md) |
| **Architecture** | How do those owners communicate and where do they run? | [R0 HLD](../architecture/R0-HLD.md) · [R0 solution architecture](../platform/ws3-platform/03-solution-architecture-r0.md) |
| **Governance** | Should this work be done now, later, or not at all; and who must approve it? | [Runbook](../governance/RUNBOOK.md) · [AIGEM](../governance/README.md) |
| **Code** | What actually exists today? | Repository `settings.gradle.kts`, `services/`, `libs/`, `apps/` and service READMEs |

## Do not confuse documentation status with implementation status

A recurring source of misunderstanding in this repository is that a target architecture may describe a service before the service exists in code.

Use these labels when reading:

- **Accepted / ratified** — a binding decision.
- **Proposed / Candidate / AI-DRAFTED** — useful design material, but human approval may still be outstanding.
- **Admitted** — work is allowed into the appropriate backlog; this does not mean implemented.
- **Ready** — approved, dependency-ready work can be picked up.
- **Implemented** — code exists.
- **Evidenced / Done** — the stated acceptance and Definition of Done evidence exist.

## Fastest onboarding path

1. [Repository map](repository-map.md) — understand what every major folder is for.
2. [Glossary](glossary.md) — remove the abbreviation barrier.
3. [Current state & decisions](status-and-decisions.md) — know where each workstream is today.
4. [Business problem statement](../context/business-problem-statement.md) — understand why the platform exists.
5. [R0 HLD](../architecture/R0-HLD.md) — understand the end-to-end R0 journey.
6. [Domain model & invariants](../platform/ws3-platform/01-domain-model-and-invariants.md) — learn ownership and hard rules.
7. [API/service catalogue](api-service-catalogue.md) — distinguish target services from currently implemented modules.
8. [Role guide](role-guides.md) — follow the reading sequence for Developer, Architect, QA, Security, DBA, SRE/DevOps, Product or Delivery.

## Portal authority rule

This Knowledge Hub is a **navigation and explanation layer only**. It must never silently override:

1. governance authority for process,
2. business working decisions / decision log for product truth,
3. authentication/authorization SSOT for identity,
4. 1SB service SSOT for adapter engineering,
5. ratified architecture decisions for technical structure.

When in doubt, follow the linked source rather than this summary.
