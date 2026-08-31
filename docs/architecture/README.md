# Architecture diagrams

Two diagrams, deliberately. They answer different questions and **neither replaces the other**.

| Artefact | Question it answers | Status |
|---------|--------------------|--------|
| [`../hdl.svg`](../hdl.svg) | *Where is this platform going, and what arrives in which release?* | Target state (North Star). AI-drafted, **T4 Architecture sign-off outstanding** |
| [`r0-reference-architecture.svg`](./r0-reference-architecture.svg) | *What are we building right now, and how does the R0 journey actually run?* | R0 executable architecture — the admitted scope. **Rendering**; owns nothing |
| [`R0-HLD.md`](./R0-HLD.md) | *Walk the R0 picture in prose: domain, ten boundaries, communication, APIs, business logic, waves vs stages vs releases* | Stakeholder HLD. Compiled view of the authoritative `ws3-platform/` sources. AI-drafted, T4 outstanding |
| [`R0-LLD.md`](./R0-LLD.md) | *What AWS resources, VPC, reverse proxies, PVCs, databases and caches does the platform team provision for R0?* | S09 requirements pack for the CTO and AWS platform team. AI-drafted; Security / Database / SRE reviews outstanding |
| [`r0-lld.svg`](./r0-lld.svg) | *Where does each R0 service sit on AWS, and what must not be provisioned?* | Rendering of `R0-LLD.md`. Owns nothing (`HA-02`) |
| [`r0-platform-topology.svg`](./r0-platform-topology.svg) | *What runs where — zones, subnets, namespaces, the two-hop proxy?* | Rendering of `R0-LLD.md`. **Generated** from [`diagrams/`](./diagrams/README.md). Owns nothing (`HA-02`) |
| [`r0-platform-az.svg`](./r0-platform-az.svg) | *Which availability zone does each resource sit in?* | Rendering of `R0-LLD.md` §2.1 |
| [`r0-platform-dr.svg`](./r0-platform-dr.svg) | *What exists in `ap-south-2`, and what deliberately does not?* | Rendering of `R0-LLD.md` §11.1 |
| [`r0-platform-sequence.svg`](./r0-platform-sequence.svg) | *In what order does the platform team build it?* | Rendering of `R0-LLD.md` §12.1 |
| [`r0-platform-payment.svg`](./r0-platform-payment.svg) | *Where does the money actually go?* | Rendering of `R0-LLD.md` §3 and §11 |

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
this folder's HLD SVG: the HLD walks the R0 picture for humans (APIs, saga, *what to do when*); the LLD
narrows the target-state AWS review to the R0 bill of materials — VPC, two-hop reverse proxy,
no PVCs on business services, one Aurora cluster. *(The "no Kafka, no shared Redis" half of that
narrowing was superseded on 2026-08-24 — see the robustness round below.)*

`SUG-20260820-ls1` added [`r0-lld.svg`](./r0-lld.svg), the rendering of that LLD. Colour is
**trust zone / subnet**, not build wave. Dashed boxes are DO NOT PROVISION.

## Still missing

An **R0 → R1 → R2 transition and dependency map**. The North Star shows the destination and the
roadmap band shows what lands when, but neither tells delivery the *order* in which components
must appear, or which target components are prerequisites for others. That is the next diagram,
and it is Kalpana's (R12) input as much as Architecture's. Tracked as parked `SUG-20260820-r1t`.

## Revision — 2026-08-20 platform-request round

`SUG-20260820-pt9` added a third H0 rendering for the AWS platform / landing-zone team — first
as a hand-authored `r0-platform-topology.svg`, superseded the same day by the generated set below
(`SUG-20260820-ic3`). It exists because three of the six questions that team actually asks were not answered anywhere:

| Question | Where it was answered before | Where it is answered now |
|---|---|---|
| What kind of application is this? | `R0-HLD.md` §1–§3 | unchanged — restated on the picture |
| What services do we need? | `R0-LLD.md` §12 · `03-solution-architecture-r0.md` §3 | unchanged |
| How many reverse proxies, and where? | `R0-LLD.md` §3 | unchanged — drawn as a per-hop responsibility table |
| **Which availability zone does each resource sit in?** | nowhere — the sources said "3 AZs" and "min 2 AZ" and stopped | **`R0-LLD.md` §2.1**, rendered as the AZ placement matrix |
| **What DR services do we provision, and are they running?** | nowhere as a resource list — §11 gave the posture only | **`R0-LLD.md` §11.1**, rendered as `D1`…`D12` |
| **When is each resource needed?** | nowhere — no map from an AWS resource to an S09 story or a build wave | **`R0-LLD.md` §12.1**, rendered as bands `P0`…`P8` plus the wave-precondition table |

`HA-03` was followed: all three gaps were closed in `R0-LLD.md` first, and the SVG renders them.
No AWS service appears that `R0-LLD.md` §1.1/§1.2 did not already name, and the DO NOT PROVISION
list is carried through unchanged — the point of the file is to make the R0 boundary *easier* to
hold under a landing-zone conversation, not to widen it.

`HA-09` in [`16-hld-authoring-and-update-protocol.md §3`](../context/roles/mahesh-principal-insurance-platform-architect/16-hld-authoring-and-update-protocol.md#3-horizon-rendering)
now states why three H0 renderings coexist and what each may assert.

**Still unsigned.** The pack is `AI-DRAFTED`. Deepali (Security), Aarti (Database), Shivanshi (SRE)
and Shailja (Compliance) reviews are outstanding, and the mandatory human T4 Architecture sign-off
is outstanding. Two decisions inside it belong to named humans and are tagged as such: Aurora
Global versus backup-restore for DR (Aarti), and the CBS / Bank AD connectivity pattern (Shivanshi
with the bank network team).

## Revision — 2026-08-20 icon-notation round

`SUG-20260820-ic3` replaced the hand-authored `r0-platform-topology.svg` with five **generated**
diagrams that use the official AWS and Kubernetes icons. The content did not change; the notation
did, and the change was requested for a reason worth recording:

> *"I'm not looking for all those boxes. I'm looking for the actual images or the logos — when you
> are using a Kubernetes cluster it should show that this is the Kubernetes cluster."*

That is a fair reading of the audience. A landing-zone conversation happens with people who read
AWS diagrams all day, and a wall of labelled rectangles asks them to translate before they can
review. The SVG was also authored by hand, which made it slow to change and impossible to diff
usefully — the exact failure `HA-03` exists to prevent.

**What replaced it:** a generated set built with [`mingrammer/diagrams`](https://diagrams.mingrammer.com)
and Graphviz (itself superseded later the same day — see the next section). The icon sets ship inside
the pip wheel, so no image is vendored here and nothing is fetched at render time.

**Why five files and not one.** The first attempt drew everything on one canvas and was
unreadable: eleven services in one row made it 7,000 pixels wide, and the C4 payment path — which
runs device → PG → callback → back into the VPC — looped backwards across the whole picture. Each
file now answers exactly one of the questions the platform team asks. The tabular content that the
old SVG carried (the AZ matrix, `D1…D12`, `P0…P8`) was never diagram content in the first place;
it lives in `R0-LLD.md` §2.1, §11.1 and §12.1, which is where it belongs and where it already was.

**One defect worth naming**, because it is the kind that survives review: the first render placed an
Aurora *writer* in all three availability zones. The zone test was `"A" in zone`, and `"A"` is in
`"AVAILABILITY"`. A diagram that asserts a Multi-AZ topology it does not have is worse than no
diagram, which is the whole of `HA-02` in one bug.

## Revision — 2026-08-20 orthogonal-layout round

`SUG-20260820-lay4` kept the icons and threw away the layout engine. The five views are the same
five views; what changed is that nothing in them is positioned by an algorithm any more.

The reason was a fair reading of the first attempt:

> *"They are not well aligned, not well positioned, and not correctly linked. The links move
> randomly here and there, crossing and curving. It should be straight lines, diverted at ninety
> degrees only."*

That is the standard failure mode of a layered layout engine, not a tuning problem. Graphviz's
`splines=ortho` was tried first and rejected on evidence: it produces the 90-degree lines, but it
detaches edge labels from their edges and routes connectors straight through cluster borders, and
node positions are still the engine's choice rather than a deliberate grid.

**What replaced it:** [`diagrams/svgcanvas.py`](./diagrams/README.md), a small canvas where every
element sits at a coordinate the source file names and every connector is a run of axis-aligned
segments the source file routes. Corridors are constants (`COL`, `LANE_EGRESS`), so two connectors
sharing a lane is a recorded decision rather than an accident. Output is now **SVG** — self-contained,
with the icons embedded as base64 — plus a PNG companion for tools that will not take an SVG.

**Two defects this surfaced**, both of the kind that only a rendered look will catch (continued
below the robustness-round entry):

- Vertical connectors were being drawn straight **through their own node's caption**, because a
  bottom port started at the icon's edge and the label hangs below it. Fixed in the canvas rather
  than per diagram: `Node.port("B")` now clears the label block.
- Edge labels relied on the SVG `paint-order` property for their white halo. cairosvg and older
  librsvg ignore it, which renders the label as a white smear — in exactly the viewers a platform
  team is most likely to use. Labels now sit on a real plate.

## Revision — 2026-08-24 R0 robustness round

`SUG-20260824-gp1` … `gp5`, under
[`CR-012`](../governance/change-requests/CR-012-r0-platform-robustness.md), admitted five
infrastructure layers into R0 (`ADR-009` … `ADR-013`). **Every diagram in this folder changed in the
same commit as its source**, per `HA-03` — and the reason for saying so explicitly is that four of
these files carried a *"not in R0"* claim that is now false, which is the most dangerous kind of
stale diagram: it does not look out of date, it looks like a decision.

| File | What changed |
|---|---|
| [`r0-lld.svg`](./r0-lld.svg) | The `DO NOT PROVISION` box lost MSK and ElastiCache and gained the things still refused (MSK Replicator, cache-as-idempotency); the cache box turned solid; MSK and OpenSearch replaced two boxes in the private-data band; the egress band became the inspection VPC |
| [`r0-reference-architecture.svg`](./r0-reference-architecture.svg) | Boundary 9 now reads *outbox in front of a broker*, not *no broker*; the footer's "No Kafka, no event bus, no Redis idempotency" line became what is actually still refused; the fitness-function count moved to 28 |
| [`../hdl.svg`](../hdl.svg) | The `R0` box in boundary 9 says outbox → MSK; the `RN` box is no longer "introduce a managed bus" — MSK is in R0, so what remains RN is cross-region and replay-heavy consumption |
| generated set ([`diagrams/`](./diagrams/README.md)) | Inspection VPC and Transit Gateway in the topology; quorum services in the AZ view; `D13`–`D16` in the DR view; `P1`/`P3`/`P6`/`P8` in the sequence view |

**What did not change, and is worth stating because it is the part a reader will assume moved:** the
ten boundaries, the journey spine, the two actors, the wave colouring, the LOB encoding, the service
count (fourteen plus one app) and `ADR-008`'s one-cluster data topology. The robustness round is
strictly *beneath* the R0 slice.

## Revision — 2026-08-25 Lead-domain R0 pull (`CR-013`, `ADR-014`)

Label and release-chip reconciliation only. No new bounded context, no new AWS hop.

| File | What changed |
|---|---|
| [`../hdl.svg`](../hdl.svg) | Spoken name of `#5` is **Lead**. `#18` and the `#19` admin UI chips move to **R0**. Campaign/bulk Lead create stays R1. Opportunity remains the durable-demand alias. |
| [`r0-reference-architecture.svg`](./r0-reference-architecture.svg) | `#5 Lead`. `#18` and Admin BFF promoted from R1 ghosts to **W4**. `#19` no longer says "NO ADMIN UI IN R0". |
| [`r0-lld.svg`](./r0-lld.svg) | Namespace label `#5 Lead`. Topology unchanged. |
| generated set | `#5 Lead` on the topology view. Regenerated; not hand-edited. |
| [`R0-HLD.md`](./R0-HLD.md) | Compiled narrative matches the same cut. |

## Revision — 2026-08-25 RM/admin web hosting (`SUG-20260825-ll1`)

`CR-013` / `ADR-014` put the Admin BFF and `#18` on the HLD. This revision puts them on the S09 pack the platform team provisions from. No new bounded context, no new AWS hop, **no PVC**.

| File | What changed |
|---|---|
| [`R0-LLD.md`](./R0-LLD.md) | New §3.1: RM/admin web are image-baked pods in `ns:edge` on the internal ALB. Z0/Z2, W4 and OUT OF SCOPE match `ADR-014`. Warehouse (Glue ETL) stays out; `#18` is the isolated read path. |
| [`R0-HLD.md`](./R0-HLD.md) | Boundary 1: Flutter native + desktop web in `ns:edge`; admin/ops on a separate hostname. |
| [`r0-lld.svg`](./r0-lld.svg) | Four `ns:edge` pods (`rm-web`, `#2`, Admin BFF, `admin-web`). Z0 gains admin/ops. `#18` on `ns:jobs`. Glue/Athena still DO NOT PROVISION. |
| generated set | Topology: RM native, RM desktop, IPR, admin/ops devices; four edge pods; `#18` in `ns:jobs`. Regenerated from [`diagrams/r0_platform_views.py`](./diagrams/r0_platform_views.py). Not hand-edited. |

## Revision — 2026-08-25 one NIP-APP (`ADR-015`, PROPOSED)

Taken architecture decision (human:Mahesh). **Human T4 Architecture sign-off is outstanding.** Do not rewrite the `ADR-014` / `CR-013` history above — `ADR-015` **retracts the hostname / pod split**.

| File | What changed |
|---|---|
| [`R0-LLD.md`](./R0-LLD.md) | §3.1: one Flutter **NIP-APP** (web + APK + IPA). `ns:edge` = `nip-web` + `#2` NIP BFF **only**. One hostname. Store listing is EKS + Play + App Store. |
| [`R0-HLD.md`](./R0-HLD.md) | Boundary 1 and 3: NIP-APP and NIP BFF. RM / IPR / admin/ops are **roles**. |
| [`r0-lld.svg`](./r0-lld.svg) | Z0 is one NIP-APP. `ns:edge` is two pods. |
| [`r0-reference-architecture.svg`](./r0-reference-architecture.svg) | Channel is NIP-APP. `#2` is NIP BFF. Admin BFF is **not** a second R0 BFF. |
| [`../hdl.svg`](../hdl.svg) | `#2` NIP BFF; Admin BFF folded to roles on NIP-APP (`HA-06`). |
| generated set | Topology devices and `ns:edge` regenerated from [`diagrams/r0_platform_views.py`](./diagrams/r0_platform_views.py). Not hand-edited. |

## Revision — 2026-08-27 platform topology & enterprise delivery alignment (`ADR-016`, `SUG-20260827-tpo`)

Alignment with bank enterprise architecture directives:

| File | What changed |
|---|---|
| [`diagrams/r0_platform_views.py`](./diagrams/r0_platform_views.py) | Replaced in-cluster Argo CD with **GitLab CI/CD** with logo; replaced AWS Network Firewall with **F5 BIG-IP / Firewall** with logo; added **Ansible for automated DR drills / sanity testing**; highlighted **Terraform IaC**. |
| [`r0-platform-topology.svg`](./r0-platform-topology.svg) | Rendered with GitLab CI/CD, F5 BIG-IP / Firewall, Ansible automation, and Terraform IaC. |
| [`r0-platform-sequence.svg`](./r0-platform-sequence.svg) | Rendered `P1` (inspection VPC with F5), `P7` (GitLab CI/CD + Terraform IaC), and `P8` (Ansible automated DR & sanity proof). |
| [`r0-platform-dr.svg`](./r0-platform-dr.svg) | Rendered `D11` / `D12` with Ansible automated execution. |
| [`R0-LLD.md`](./R0-LLD.md) | BOM #22, #29, and provisioning sequence updated with F5, GitLab, Terraform, and Ansible automation. |
| [`ARB-ARCHITECTURE-DOSSIER.md`](./ARB-ARCHITECTURE-DOSSIER.md) | Updated with Terraform IaC + Ansible automation defense. |

## Revision — 2026-08-31 edge correction (`ADR-018`, `SUG-20260831-alb`)

Two assumptions in the 2026-08-25/27 perimeter were wrong against the existing AU Bank estate:

| Correction | Was | Is |
|---|---|---|
| Public / External ALB in front of API Gateway | Drawn as a hop | **Withdrawn.** API Gateway is the first AWS hop. The only ALB is the Internal ALB. |
| Cloudflare and F5 placement | Drawn as if on AWS / in a VPC | **Bank-enterprise SaaS.** Outside the AWS Cloud box, outside every VPC. F5 is F5-XC (Distributed Cloud), not an in-VPC BIG-IP. |

| File | What changed |
|---|---|
| [`diagrams/r0_platform_views.py`](./diagrams/r0_platform_views.py) | SaaS group outside the AWS region box; External ALB removed; inspection VPC restored to Network Firewall (`ADR-010`). |
| generated set | Topology, AZ, DR, sequence re-rendered. Not hand-edited. |
| [`R0-HLD.md`](./R0-HLD.md) · [`R0-LLD.md`](./R0-LLD.md) · [`ARB-ARCHITECTURE-DOSSIER.md`](./ARB-ARCHITECTURE-DOSSIER.md) | Ingress hop and BOM #7 / #29 aligned to `ADR-018`. |
| [`r0-reference-architecture.svg`](./r0-reference-architecture.svg) · [`../hdl.svg`](../hdl.svg) | Edge band labels. |

## Revision — 2026-08-31 attach to existing bank network; Apigee stays off the pictures (`SUG-20260831-apg`)

Human Architecture owner: keep Apigee **off every diagram** until `SPIKE-001` returns. Amazon API Gateway remains. Network pack attaches to the existing `AU-CTO-NETWORK` TGW / DX Gateway; do not clone Public VPC + IGW + peering.

| File | What changed |
|---|---|
| [`R0-LLD.md`](./R0-LLD.md) | §1.3 / §2 / §2.2 / §13 / BOM #25 #27 / P1: attach, do not duplicate; no workload IGW; candidate bank API plane not drawn. |
| [`R0-HLD.md`](./R0-HLD.md) · [`ARB-ARCHITECTURE-DOSSIER.md`](./ARB-ARCHITECTURE-DOSSIER.md) | Spoke-attach language; overlay not named on the picture. |
| [`diagrams/r0_platform_views.py`](./diagrams/r0_platform_views.py) | TGW labelled as existing hub we attach to; NOT IN R0 lists a second TGW/DX and Public VPC+IGW — **does not name the parked overlay**. |
| [`r0-lld.svg`](./r0-lld.svg) | Stale CloudFront+WAF caption corrected; TGW labelled as existing hub. |
