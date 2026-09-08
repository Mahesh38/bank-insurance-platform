# Bank Insurance Platform Knowledge Hub

> **Purpose:** one navigable entry point for understanding the repository without creating a second source of truth.

**Primary UI:** GitHub or GitLab Markdown. No separate documentation server is required. This hub indexes and explains the existing authoritative documentation; if a summary disagrees with authority, the authoritative source wins.

<div class="kb-grid">
  <div class="kb-card">
    <h3>New to the project?</h3>
    <p>Start with the business problem, current state, R0 architecture and your role-specific path.</p>
    <p><a href="role-guides.md">Start by role →</a></p>
  </div>
  <div class="kb-card">
    <h3>What is happening now?</h3>
    <p>See the committed, CI-synchronized view of workstreams, stages and gate criteria.</p>
    <p><a href="generated/current-state.md">Current state →</a></p>
  </div>
  <div class="kb-card">
    <h3>What is parked or decided?</h3>
    <p>Navigate straight to the authoritative governance registers.</p>
    <p><a href="generated/backlog-index.md">Backlog →</a> · <a href="generated/decision-index.md">Decisions →</a></p>
  </div>
  <div class="kb-card">
    <h3>What does this abbreviation mean?</h3>
    <p>Decode AIGEM, T1–T4, G1–G10, P1–P5, C1–C8, SF, SC, INV, SUG, CR, ADR, seams, gates and domain terminology.</p>
    <p><a href="glossary.md">Open glossary →</a></p>
  </div>
  <div class="kb-card">
    <h3>Where is the documentation?</h3>
    <p>Navigate governance, business, architecture, journey, 1SB, identity, data and role documents.</p>
    <p><a href="repository-map.md">Repository map →</a></p>
  </div>
  <div class="kb-card">
    <h3>What modules exist now?</h3>
    <p>See the Gradle-derived module inventory without confusing module presence with implementation completion.</p>
    <p><a href="generated/service-inventory.md">Service inventory →</a></p>
  </div>
</div>

## The five-layer mental model

| Layer | Question | Start here |
|---|---|---|
| **Business** | What problem are we solving and what behaviour do we promise? | [Business problem](../context/business-problem-statement.md) · [Business documentation](../au-bank-insurance-platform/README.md) |
| **Domain** | Which bounded context owns each piece of truth and which rules must always hold? | [Domain model & invariants](../platform/ws3-platform/01-domain-model-and-invariants.md) |
| **Architecture** | How do those owners communicate and where do they run? | [R0 HLD](../architecture/R0-HLD.md) · [R0 solution architecture](../platform/ws3-platform/03-solution-architecture-r0.md) |
| **Governance** | Should this work be done now, later, or not at all; and who must approve it? | [Runbook](../governance/RUNBOOK.md) · [AIGEM](../governance/README.md) |
| **Code** | What modules are registered today? | [Module/service inventory](generated/service-inventory.md) · repository `services/`, `libs/`, `apps/` |

## Git-native dashboards vs curated explanations

The Knowledge Hub deliberately has both:

- **Committed generated dashboards** are derived from repository sources and rendered immediately by GitHub/GitLab. CI regenerates them and fails if they drift.
- **Curated pages** explain concepts, terminology, reading order and relationships between sources.
- **Authoritative registers/documents** remain the actual source of truth and are linked directly from the dashboards.

Generated pages are never approval records and never change lifecycle state.

## Do not confuse documentation status with implementation status

A target architecture may describe a service before its feature is implemented or evidenced.

- **Accepted / ratified** — binding decision.
- **Proposed / Candidate / AI-DRAFTED** — useful design material; approval may still be outstanding.
- **Admitted** — work is allowed into the appropriate backlog; not necessarily implemented.
- **Ready** — approved, dependency-ready work can be picked up.
- **Implemented** — code exists.
- **Evidenced / Done** — acceptance and Definition of Done evidence exist.

## Fastest onboarding path

1. [Repository map](repository-map.md) — understand the major folders and authorities.
2. [Glossary](glossary.md) — remove the abbreviation barrier.
3. [Current state](generated/current-state.md) — know where each workstream and gate is today.
4. [Backlog & parking](generated/backlog-index.md) — understand where admitted, blocked and parked work lives.
5. [Decision dashboard](generated/decision-index.md) — know where CRs, ADRs and decisions live and how to interpret status.
6. [Business problem statement](../context/business-problem-statement.md) — understand why the platform exists.
7. [R0 HLD](../architecture/R0-HLD.md) — understand the end-to-end R0 journey.
8. [Domain model & invariants](../platform/ws3-platform/01-domain-model-and-invariants.md) — learn ownership and hard rules.
9. [Service inventory](generated/service-inventory.md) and [API/service explanation](api-service-catalogue.md) — distinguish code structure from intended contracts and completion evidence.
10. [Role guide](role-guides.md) — follow the reading sequence for your role.

## Portal authority rule

This Knowledge Hub is a **navigation and explanation layer only**. It must never silently override:

1. governance authority for process;
2. business working decisions / decision log for product truth;
3. authentication/authorization SSOT for identity;
4. 1SB service SSOT for adapter engineering;
5. ratified architecture decisions for technical structure.

When in doubt, follow the linked source rather than this summary.
