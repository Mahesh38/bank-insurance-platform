# Architecture diagrams

Two diagrams, deliberately. They answer different questions and **neither replaces the other**.

| Diagram | Question it answers | Status |
|---------|--------------------|--------|
| [`../hdl.svg`](../hdl.svg) | *Where is this platform going, and what arrives in which release?* | Target state (North Star). AI-drafted, **T4 Architecture sign-off outstanding** |
| [`r0-reference-architecture.svg`](./r0-reference-architecture.svg) | *What are we building right now, and how does the R0 journey actually run?* | R0 executable architecture — the admitted scope |

## Why both

A single diagram cannot carry both jobs without misleading someone. If only the target were
published, the reasonable question becomes *"if this is our architecture, why has the team not
built Health, the call centre or renewals?"*. If only R0 were published, the equally reasonable
question becomes *"why are we spending effort on a Journey Registry, canonical provider contracts
and per-provider bulkheads for a single Term Life product?"*.

The answer to both is the same and it only shows up when the two are read together: **R0 builds
the seams that R1–RN need, and nothing more.**

## Reading the North Star

`hdl.svg` is an **architecture intent artefact, not a scope document**. Every element carries the
release in which it first exists:

- **R0** — inside WS-3 `current_scope` in
  [`CURRENT-STATE.yaml`](../governance/state/CURRENT-STATE.yaml). This is the only admitted scope.
- **R1 · R2 · R3 · R4** — correspond to `out_of_scope` entries in the same file and carry that
  entry's `revisit_at`. They are recorded deferrals, not plans.
- **RN** — steady state. No governance record at all; direction of travel only.

Nothing on the diagram may be cited as authority to start work. Each element still requires AIGEM
triage, an owner, and — where consequential — an ADR and the applicable board reviews. See
[`governance/09-AI_EXECUTION_RULES.md`](../governance/09-AI_EXECUTION_RULES.md).

## Revision — 2026-08-20 HLD review round

Both diagrams were reconciled in the same change as their sources (`HA-03`, `HA-06`) under
`SUG-20260820-hr0`:

- the R0 view gains the **two-actor model** (Bank RM as the certified Specified Person; the
  assist-only Insurance Partner Representative), the **Opportunity (#5)** origination point and the
  **Configuration (#19)** layer that ships in R0 without an admin UI;
- the North Star's channels-and-actors band no longer draws *Certified SP* as an actor — a
  certification is an attribute on the RM principal, and the freed slot now carries the IPR.

Decisions: [`ADR-004` … `ADR-007`](../platform/architecture-review/08-architecture-decision-log.md).

## Authority

These are **rendered views**, not sources of truth. Where a diagram and a document disagree, the
document wins:

- [`platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md`](../platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md)
  — bounded contexts, interfaces IF-1/IF-2/IF-3, standing constraints SC-W3-1…7
- [`platform/ws3-platform/03-solution-architecture-r0.md`](../platform/ws3-platform/03-solution-architecture-r0.md)
  — R0 contexts and seams
- [`platform/ws3-platform/01-domain-model-and-invariants.md`](../platform/ws3-platform/01-domain-model-and-invariants.md)
  — the domain invariants the journey spine renders
- [`governance/state/CURRENT-STATE.yaml`](../governance/state/CURRENT-STATE.yaml) — scope and stage

## Still missing

An **R0 → R1 → R2 transition and dependency map**. The North Star shows the destination and the
roadmap band shows what lands when, but neither tells delivery the *order* in which components
must appear, or which target components are prerequisites for others. That is the next diagram,
and it is Kalpana's (R12) input as much as Architecture's.
