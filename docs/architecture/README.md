# Architecture diagrams

Two diagrams, deliberately. They answer different questions and **neither replaces the other**.

| Artefact | Question it answers | Status |
|---------|--------------------|--------|
| [`../hdl.svg`](../hdl.svg) | *Where is this platform going, and what arrives in which release?* | Target state (North Star). AI-drafted, **T4 Architecture sign-off outstanding** |
| [`r0-reference-architecture.svg`](./r0-reference-architecture.svg) | *What are we building right now, and how does the R0 journey actually run?* | R0 executable architecture — the admitted scope. **Rendering**; owns nothing |
| [`R0-HLD.md`](./R0-HLD.md) | *Walk the R0 picture in prose: domain, ten boundaries, communication, APIs, business logic, waves vs stages vs releases* | Stakeholder HLD. Compiled view of the authoritative `ws3-platform/` sources. AI-drafted, T4 outstanding |
| [`R0-LLD.md`](./R0-LLD.md) | *What AWS resources, VPC, reverse proxies, PVCs, databases and caches does the platform team provision for R0?* | S09 requirements pack for the CTO and AWS platform team. AI-drafted; Security / Database / SRE reviews outstanding |

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

## The convention both diagrams follow

They are one artefact in two cuts, and three rules keep them that way. All three live in
[`16-hld-authoring-and-update-protocol.md §4.0`](../context/roles/mahesh-principal-insurance-platform-architect/16-hld-authoring-and-update-protocol.md).

- **`NC-1` — the `#n` is the identity, the name is a label.** Every element carries its bounded
  context number from [`business-problem-statement.md §6`](../context/business-problem-statement.md#6-business-capabilities--bounded-contexts)
  *on the element*, with the canonical register name. Seven contexts are drawn under a different
  name at target state — `#4`, `#7`, `#8`, `#9`, `#10`, `#11`, `#13` — and those are written
  `#n Canonical Name → target name` so the evolution is visible rather than read as a different
  service. `#5` was the dual form until `OPEN-D10` closed on 2026-08-20; it is now `Opportunity` in
  both files and in the register.
- **`LY-1` — the ten boundaries are the layer model, not a drawing style.** Both files lay out in
  the same bands, in the same order, so the two can be read side by side. A band that is thin in
  R0 is still drawn and labelled — the R0 file shows one BFF and says the other three are R1/R2.
- **`LB-R1` — line of business is rendered in three classes**, described below.

The two files deliberately colour by different axes — the North Star by **release** (its question
is *when*), the R0 view by **build wave** (its question is *in what order*) — and both legend it.
The LOB encoding is the one thing identical in both, so it reads straight across.

## Reading the line of business

The question these diagrams get asked is *which of these boxes do I get a second copy of when
Health arrives*. [`01-domain-model-and-invariants.md §2.5`](../platform/ws3-platform/01-domain-model-and-invariants.md)
answers it in `LB-4` and `LB-5`, and the diagrams now show it:

| Class | Marking | R0 members | What it means |
|---|---|---|---|
| **LOB-owned execution** | inside the rose cell | `#10` Quotation · `#11` Proposal & UW | Health and General each get their **own** instance. The boundary is frozen: a Life quote and a Health quote do not share a field shape |
| **LOB-partitioned shared** | `LIFE` tag on the node | `#2` · `#3` (PDP grants) · `#6` · `#7` · `#8` · `#9` · `#14` · `#19` | One codebase, one deployment. Behaviour resolved from configuration keyed by `(lob, …)` per `CF-2` — never forked, never branched on an `lob` literal |
| **LOB-agnostic shared** | no tag, outside every cell | `#4` · `#5` · `#12` · `#13` · `#16` · `#17` | One instance for every line of business, forever |

> Partitioning the **rules** is what makes the Health cell possible; duplicating the **evidence**
> is what makes it unauditable. That distinction is the whole of `LB-5`, and it is why the middle
> row exists instead of a simple shared/not-shared split.

## Revision — 2026-08-20 HLD review round

Both diagrams were reconciled in the same change as their sources (`HA-03`, `HA-06`) under
`SUG-20260820-hr0`:

- the R0 view gains the **two-actor model** (Bank RM as the certified Specified Person; the
  assist-only Insurance Partner Representative), the **Opportunity (#5)** origination point and the
  **Configuration (#19)** layer that ships in R0 without an admin UI;
- the North Star's channels-and-actors band no longer draws *Certified SP* as an actor — a
  certification is an attribute on the RM principal, and the freed slot now carries the IPR.

Decisions: [`ADR-004` … `ADR-007`](../platform/architecture-review/08-architecture-decision-log.md).

## Revision — 2026-08-20 diagram alignment round

`SUG-20260820-al7` verified the two files against each other and against their sources, and closed
the gaps the previous round left open:

- the North Star had `#5 Opportunity` and `#19 Configuration` chipped **R1**. Both ship in R0 —
  `03-solution-architecture-r0.md §3` places them in Wave 1 and Wave 0b — and the North Star's own
  roadmap band already said so about `#5`. Corrected, and the R0/R1 split stated on `#19`: the
  configuration **layer** is R0, its maker-checker governance and admin UI are R1;
- the North Star asserted **15** fitness functions; the catalogue has been `FF-01…FF-21` since the
  review round added `FF-16…FF-21`. Corrected;
- seven contexts were drawn under two names with no mapping, four of them with no `#n` on the
  element at all. `NC-1` above now governs, and both files comply;
- the R0 view was laid out by build wave and journey flow while the North Star was laid out in ten
  boundaries, so it could not be read as a release-zero cut of the target. **It has been redrawn on
  the same ten bands**, with what arrives later drawn greyed and carrying its release;
- nothing distinguished LOB-owned from shared. The three-class encoding above is now rendered in
  both files, with the Life cell boxed and coloured.

The root cause was recorded too: the authoring protocol still described `hdl.svg` as the R0 file
and carried a canvas contract for one diagram, so there was no convention covering two. It now
covers both, and its consistency checklist fails when the files disagree about a release chip or a
context name.

**One divergence was left open, and has since been decided.** `OPEN-A1` asked whether R0 starts
with a cluster per service or schemas in one cluster. See the round below.

## Revision — 2026-08-20 decision round

Two open questions closed by Mahesh, recorded as `SUG-20260820-dc4`:

**`OPEN-A1` — data topology.** `ARCH-004` bundled three claims and only two are principles.
Ownership of an authoritative datum, and per-service credentials and schema ownership, stay
**invariant**. A physical cluster per service is a **decision**, and it is not adopted: R0 runs one
Aurora cluster with a schema per bounded context, and the first physical split follows the
**LOB-cell / shared-platform seam** rather than the service boundary. Recorded as
[`ADR-008`](../platform/architecture-review/08-architecture-decision-log.md); both diagrams now say
the same thing, and the R0 file's `OPEN-A1` note is gone.

This is a real trade, not a tidy-up. One cluster is one blast radius, and isolation that was going
to be physical is now carried entirely by per-context credentials and grants — which is why
**Aarti's Database approval and Deepali's Security review are required and outstanding**. An
architect's decision is not a DBA's sign-off.

**`OPEN-D10` — the name of context #5.** It is **Opportunity**. A lead records that someone might
buy and stops meaning anything at conversion; an opportunity is the durable demand object behind a
new sale, a renewal, a lapse recovery, a cross-sell and an abandoned-journey recovery. That is
exactly the R2 rule both diagrams draw — a renewal or lapse creates a NEW opportunity and a NEW
journey, never reopening an old one — which reads as a contradiction under the name *Lead* and as
the model under the name *Opportunity*. Identifiers did not follow: `leadId`, `INV-LED-*` and
`CAP-102` keep their tokens.

> `CURRENT-STATE.yaml` `current_scope.in_scope` and `WS-3-PLATFORM-CHARTER.md` still read *"Lead
> service (context #5)"*. Those are human-owned scope text and were not edited. Kalpana / R12 to
> transcribe, with Rajal's Product confirmation of the label.

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

## Revision — 2026-08-20 stakeholder HLD / LLD pack

`SUG-20260820-hl1` added the two prose artefacts above. They do not replace
[`03-solution-architecture-r0.md`](../platform/ws3-platform/03-solution-architecture-r0.md) or
this folder's SVG: the HLD walks the SVG for humans (APIs, saga, *what to do when*); the LLD
narrows the target-state AWS review to the R0 bill of materials — VPC, two-hop reverse proxy,
no PVCs on business services, one Aurora cluster, no Kafka, no shared Redis.

## Still missing

An **R0 → R1 → R2 transition and dependency map**. The North Star shows the destination and the
roadmap band shows what lands when, but neither tells delivery the *order* in which components
must appear, or which target components are prerequisites for others. That is the next diagram,
and it is Kalpana's (R12) input as much as Architecture's. Tracked as parked `SUG-20260820-r1t`.
