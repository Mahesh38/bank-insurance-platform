# 16 — Mahesh HLD Authoring and Update Protocol

## 1. Purpose

This file makes updating the platform's high-level design a **mechanical** act rather than a
re-derivation. It documents which artefact is authoritative, the canvas contract for the diagram,
copy-paste templates, the change procedure and the consistency checks.

**Rule HA-01 — the diagram is last, not first.** `NS-01` governs: capability → ownership →
deployment → diagram. When asked to "update the HLD" for a target-state change, Mahesh first checks
whether the capability contracts in [`10`](./10-north-star-capability-model.md) actually decide what
the diagram would assert. If they do not, the answer is the capability work, and the diagram follows
in the same change.

`VIN-001` states the payoff directly: *once those boundaries are absolutely clear, a much smaller
architecture diagram will almost draw itself.*

---

## 2. The artefact family

| Artefact | Status | Owns |
|---|---|---|
| [`docs/platform/ws3-platform/03-solution-architecture-r0.md`](../../../platform/ws3-platform/03-solution-architecture-r0.md) | **Authoritative** | R0 service set, build order, seam catalogue, resilience policy, fitness functions, availability/DR |
| [`00-WS3-ARCHITECTURE-REGISTRATION.md`](../../../platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md) | **Authoritative** | Structural constraints `SC-W3-*`, registration |
| [`01-domain-model-and-invariants.md`](../../../platform/ws3-platform/01-domain-model-and-invariants.md) | **Authoritative** | `INV-*` invariants, aggregates |
| [`04-security-architecture.md`](../../../platform/ws3-platform/04-security-architecture.md) · [`05-nfr-catalogue.md`](../../../platform/ws3-platform/05-nfr-catalogue.md) | **Authoritative** | Trust boundaries, threat model, NFR numbers |
| [`architecture-review/`](../../../platform/architecture-review/README.md) 01–08 | **Authoritative** | Target service catalogue, communication patterns, data architecture, `ARCH-*` decision log |
| Persona files [`09`](./09-target-state-architecture-doctrine.md)–[`15`](./15-actor-identity-and-authorization.md), [`17`](./17-provider-aggregation-and-connectivity.md) | **Grounding** | Horizons, invariants, capability model, segregation, orchestration and provider-aggregation doctrine |
| **`docs/hdl.svg`** | **Rendering** | Nothing. It *depicts* the target state, release-coded `R0`…`RN` |
| **`docs/architecture/r0-reference-architecture.svg`** | **Rendering** | Nothing. It *depicts* the R0 slice of the same picture |
| [`docs/architecture/R0-HLD.md`](../../../architecture/R0-HLD.md) | **Compiled narrative** | Nothing new. Walks the R0 rendering for humans (domain, APIs, waves). If it disagrees with the rows above, those rows win (`HA-02`) |
| [`docs/architecture/R0-LLD.md`](../../../architecture/R0-LLD.md) | **S09 platform pack** | Maps already-accepted R0 decisions onto AWS resources for the CTO / platform team. Must not invent a service the rows above do not name |
| **`docs/architecture/r0-lld.svg`** | **Rendering** | Nothing. It *depicts* `R0-LLD.md`: VPC tiers, two-hop reverse proxy, EKS namespaces, data stores, PVC/cache/do-not-provision |
| **`docs/architecture/r0-platform-*.png`** (5 files) | **Rendering · generated** | Nothing. They *depict* `R0-LLD.md` §2.1 / §11.1 / §12.1 for the AWS platform team, in AWS and Kubernetes icon notation. **Build output** — the source is [`diagrams/r0_platform_topology.py`](../../../architecture/diagrams/README.md) and an edit to a PNG that is not an edit to that file is a defect |

**Rule HA-02b — the two renderings are one artefact in two cuts.** `hdl.svg` answers *where is
this going*; `r0-reference-architecture.svg` answers *what are we building now*. They must use the
same layer vocabulary (`LY-1`), the same context names and numbers (`NC-1`) and the same release
facts. A context that is `R0` on one file and `R1` on the other is a defect in whichever file
disagrees with `03-solution-architecture-r0.md §3` — not a difference of view.

**Rule HA-02 — the diagram is never the source of truth.** It may not assert anything the
authoritative documents do not say. If a reviewer's question can only be answered by reading the
SVG, the source documents have a gap and that gap is the finding.

**Rule HA-03 — source first, diagram second, in the same change.** Never the diagram alone: a
diagram that leads its sources is how a programme starts believing in services nobody specified.

---

## 3. Horizon rendering

**Rule HA-04 — one diagram, one horizon.**

| File | Horizon | Edited when |
|---|---|---|
| `docs/architecture/r0-reference-architecture.svg` | **H0 — R0 as designed** | R0 scope, service set, seams or controls change |
| `docs/architecture/r0-lld.svg` | **H0 — R0 AWS deployment** | R0 AWS BOM, VPC, proxy, data topology or do-not-provision list change |
| `docs/architecture/r0-platform-*.png` | **H0 — R0 AWS landing-zone request** | AZ placement, the DR resource list, the proxy/egress chain, the S09 provisioning sequence, or the wave-precondition map changes. Regenerate, never hand-edit |
| `docs/hdl.svg` | **North Star — target state, release-coded `R0`…`RN`** | A target-state boundary, capability or release phasing changes |
| `docs/hld-h1.svg`, `docs/hld-h2.svg`, … | Intermediate horizons | Created on demand, same canvas contract |

> Until 2026-08-20 `hdl.svg` was H0 and this table said so. `SUG-20260820-n5t` moved the R0 view
> to `docs/architecture/r0-reference-architecture.svg` and gave `hdl.svg` the North Star. The two
> files then drifted for want of a convention that covered both, which is what `NC-1`, `LY-1` and
> `LB-R1` below exist to stop (`SUG-20260820-al7`).

**Rule HA-05 — never mix built and aspirational without visual distinction.** If a horizon diagram
shows both, unbuilt elements carry a dashed stroke plus a status badge. `TP-03` is what this rule
enforces.

**Rule HA-06 — when a horizon diagram changes the R0 delta, reconcile `hdl.svg` in the same
change.** A target-state diagram that silently contradicts the R0 diagram leaves two answers in the
repository, and the reader has no way to know which is current.

**Rule HA-09 — the three H0 renderings answer three different questions and must not merge.**
`r0-reference-architecture.svg` answers *what are we building* (layout: `LY-1` bands, colour: build
wave). `r0-lld.svg` answers *where does it sit on AWS* (layout: trust zone, colour: trust zone).
the `r0-platform-*.png` set answers *what do we ask the AWS platform team to provision, and when*
(notation: official AWS and Kubernetes icons, one file per question). All three take their `#n`
identities from `NC-1` and their service set from `03-solution-architecture-r0.md §3`; a service
present in one and absent from another is a defect in whichever file disagrees with that source.
The platform view is the only one that may state an AZ count, a DR resource or a sequence band,
and it may state none of them that `R0-LLD.md` does not already say (`HA-02`, `HA-03`).

**Rule HA-10 — notation follows the audience, and a generated diagram is preferred to a drawn one.**
The two architecture renderings are hand-authored SVG because their audience is this programme and
their vocabulary (`LY-1` bands, wave colours, `LB-R1` classes) is ours. The platform set is
generated in AWS and Kubernetes icon notation because its audience reads AWS diagrams all day and
should not have to translate a labelled rectangle before they can review it. Where a diagram can be
generated from code, generate it: a reviewer of a hand-drawn canvas learns only that the picture
changed, while a diff of the generating source tells them which sentence changed. A generated
diagram is build output — hand-editing the output instead of the source is the same defect class as
editing a compiled artefact.

**Rule HA-07 — the North Star view is drawn in planes, not in boxes-and-arrows.** The five-plane
model (`10 §4`) is the stakeholder artefact. Drawing the North Star as a component diagram
reintroduces exactly the service-count conversation the capability model exists to prevent.

---

## 4. Canvas contract

Documented so an edit is a local change, not a re-layout. §4.0 binds **both** files; §4.1 and
§4.1b give each file its geometry; §4.2–§4.5 are shared.

### 4.0 The three conventions that bind both files

**Rule NC-1 — the `#n` is the identity; the name is a label.** Every element that renders a
bounded context carries its number from
[`business-problem-statement.md §6`](../../business-problem-statement.md#6-business-capabilities--bounded-contexts),
on the element itself — never only in a caption or a roadmap paragraph. The canonical register
name is always shown. Where the target state renames, widens or splits the context, render it as
`#n Canonical Name → target name`, so the evolution is visible rather than read as a different
service. An element with a name and no number, or a target name and no canonical name, is a
defect.

The seven that currently differ, and are therefore always written in the arrow form:

| # | Canonical register name | Target-state name | Why it differs |
|---|---|---|---|
| 4 | Customer | Party / Customer | `partyId` generalises beyond a bank customer once NTB exists |
| 7 | Suitability & Recommendation | Suitability framework + LOB rule pack | Framework shared; questions and rules per LOB |
| 8 | Product Catalogue | Product Governance & Catalogue | Insurer ≠ InsurerProduct ≠ DistributionAgreement ≠ BankProductOffering |
| 9 | Journey Orchestration | Journey Registry + LOB Router | Registry splits from execution at R1 |
| 10 | Quotation | *LOB* Quote | Per-LOB from R1; the boundary is frozen |
| 11 | Proposal & UW-Tracking | *LOB* Proposal / Case Mgmt | Per-LOB from R1; the boundary is frozen |
| 13 | Policy & Issuance | Policy Portfolio / Registry | Issuance coordination moves into the cell; the registry stays shared |

Context **#5** was in that table until `OPEN-D10` closed on 2026-08-20. It is now named
**Opportunity** in the register and at target state alike, so it has no arrow form and both files
write `#5 Opportunity` plainly
([`ADR-005` `naming_resolution`](../../../platform/architecture-review/08-architecture-decision-log.md)).
Identifiers did not follow the name: `leadId`, `INV-LED-*` and `CAP-102` keep their tokens, because
an ID is opaque and rewriting them breaks every citation to buy nothing.

**Rule LY-1 — the ten boundaries are the layer vocabulary for every horizon.** The North Star's
boundaries are not a drawing style; they are the layer model. Every horizon diagram lays its
content out in the same bands, in the same order, so a reader can put the two files side by side
and see the release cut rather than two designs:

| Band | Boundary | Owns |
|---|---|---|
| 1 | Channels & actors | Every human and system that starts or continues a journey |
| 2 | Edge | The only public entry point |
| 3 | Experience / BFF | Channel-shaped aggregation, session and token custody |
| 4 | Shared platform | Concepts that mean the same thing for every LOB |
| 5 | LOB execution cells | Where the insurance business genuinely differs |
| 6 | Aggregation & provider connectivity | Bank-canonical request in, provider protocol out |
| 7 | External boundaries | Bank systems and insurance providers, kept separate |
| 8 | Data & persistence | Durable state, evidence, documents |
| 9 | Event & messaging | Asynchronous propagation of domain facts |
| 10 | Platform engineering & governance | How the platform is built, shipped, secured, observed |

A band that is thin or empty at a horizon is **drawn and labelled as such** — "R0 holds one BFF;
Customer and Operations BFFs are R1" — never omitted. An omitted band reads as a boundary that
does not exist.

**Rule LB-R1 — LOB ownership is rendered in three classes, never two.** `LB-4` and `LB-5` draw the
line and the diagram must show it, because "which of these boxes do I get a second copy of when
Health arrives" is the single most common question asked of these files:

| Class | Meaning | Token | R0 members |
|---|---|---|---|
| **LOB-owned execution** | Inside a cell. A second LOB gets its own instance. The boundary is frozen (`10`, `11`) | LOB cell fill + cell container | `#10` Quotation · `#11` Proposal & UW |
| **LOB-partitioned shared** | One codebase, one deployment; behaviour resolved from configuration keyed by `(lob, …)` per `CF-2` | `LIFE` tag on the node | `#2` · `#3` PDP grants · `#6` · `#7` · `#8` · `#9` · `#14` · `#19` |
| **LOB-agnostic shared** | Single instance for every LOB, forever. Duplicating these is the failure mode the shared plane exists to prevent (`LB-5`) | no tag | `#4` · `#5` · `#12` · `#13` · `#16` · `#17` |

Partitioning the *rules* is what makes the Health cell possible; duplicating the *evidence* is what
makes it unauditable. A node moved between these classes needs a source change first (`HA-03`).

### 4.1 Geometry — `docs/architecture/r0-reference-architecture.svg`

Canvas `1800 × 2740`. Laid out top to bottom in the `LY-1` bands. Each band is a container with a
black header bar carrying `BOUNDARY n` and the boundary name on the left and a one-line posture
note on the right. **Bands are separated by a 34 px gutter, and that gutter is where edges live** —
see §4.4.

| Band | `y` range | Contents |
|---|---|---|
| Title | 20 – 62 | Title (25 px bold) + subtitle (12 px) |
| Journey ribbon | 74 – 126 | Section label `y=82`; chevrons `y=88–126`, 174 wide, 14 px gap, notch 12 px |
| Reading rule | 138 – 172 | How to read this file against the North Star. Amber `#fffbeb` / `#b45309` |
| **B1** Channels & actors | 186 – 318 | Flutter `x=60` · IPR `x=380` · Customer device `x=700` (`w=300`) · R1 and R2 ghosts `x=1020`, `1380` (`w=340`) |
| **B2** Edge | 352 – 470 | Edge `x=60 w=980` · callback-ingress ghost `x=1060 w=660` |
| **B3** Experience / BFF | 504 – 634 | `#2` `x=60 w=420` · ghosts `x=500`, `920` (`w=400`), `1340` (`w=380`) |
| **B4** Shared platform | 668 – 1092 | WS-2 group `x=60 w=300`; `#9` spans `x=380 w=1340`; two rows of six `w=210` at `x=380 · 606 · 832 · 1058 · 1284 · 1510`; `#19` spans `x=60 w=1660` at the foot |
| **B5** LOB execution cells | 1126 – 1376 | Life cell `x=60 w=700`; Health `x=780 w=460`; General `x=1260 w=460` |
| **B6** Aggregation & providers | 1410 – 1544 | `#14` `x=60 w=900` · `#15` `x=980 w=300` · R1 ghost `x=1300 w=420` |
| **B7** External boundaries | 1578 – 1702 | Four external nodes `w=400`, gutter 20, from `x=60` |
| Cross-boundary seams | 1712 – 1758 | The edges that span more than one band, named rather than drawn (§4.4) |
| **B8** Data & persistence | 1786 – 1938 | Four cylinders `w=380` at `x=70 · 490 · 910 · 1330` |
| **B9** Event & messaging | 1972 – 2096 | Outbox `x=60 w=830` · sync/async rule `x=910 w=810` |
| **B10** Platform engineering (`IF-3`) | 2130 – 2278 | S08 `x=60 w=830` · S09 `x=910 w=810` |
| Legend | 2312 – 2648 | `x=60 w=520` |
| LOB reading panel | 2312 – 2648 | `x=610 w=560` — the three `LB-R1` classes stated in words |
| Deferred panel | 2312 – 2648 | `x=1200 w=520` — what R0 does not contain, and the release each arrives in |
| Footer | 2690 – 2716 | Provenance, signature status, sources, date |

Content inset `x=60`, content width `1700`; band container `x=40 w=1720`. Standard node `w=210
h=86`; wide and spanning nodes as above. Node badge order, left to right along the top edge: wave
badge (`x+7`), LOB tag (`x+39`), control pill (`x+w-57`), journey-step circle (centre `x+w-17`).

### 4.1b Geometry — `docs/hdl.svg`

Canvas `1800 × 4120`. Same `LY-1` bands, plus the target-state-only bands the R0 cut has no need
of. Every element carries a release chip (`§4.2`) naming the release in which it **first exists**.

| Band | Contents |
|---|---|
| Title + reading rule | What this file is, and that only the `R0` band is admitted scope |
| Release colour model | `R0`…`RN`, each with its one-line definition |
| Canonical domain model | The journey spine, cardinality, who owns which half, channel and actor |
| **B1**…**B10** | The ten boundaries, each with an `OWNS` / `DOES NOT OWN` statement |
| Release roadmap | What lands when, per release, with unpark triggers |
| Architecture invariants | The 14 that are true in R0 and still true in RN |
| Footer | Provenance, signature status, sources, date |

### 4.2 Colour tokens

| Token | Fill | Stroke | Meaning |
|---|---|---|---|
| Wave 0b | `#e0f2fe` | `#0284c7` | Configuration layer — built before W1 |
| Wave 1 | `#dbeafe` | `#2563eb` | Journey spine — build first |
| Wave 2 | `#fef3c7` | `#d97706` | Consent · suitability · quote |
| Wave 3 | `#dcfce7` | `#16a34a` | Money · policy · audit |
| Wave 4 | `#f3e8ff` | `#9333ea` | BFF · Flutter · notification |
| WS-1 supplier | `#e2e8f0` | `#475569` | Exists today |
| WS-2 enabler | `#ecfdf5` | `#059669` | Workforce identity |
| External | `#ffffff` | `#64748b` dashed `3 2` | Outside the platform |
| Control badge | `#dc2626` | — | Non-waivable `C1` · `C2` · `C4` |
| Journey-step badge | `#0f172a` circle | — | Ribbon step number |
| **LOB cell** | `#fff1f2` | `#e11d48`, `stroke-width` 2.5 | `LB-R1` class 1 — the container around LOB-owned execution |
| **LOB tag** | `#e11d48` square tag `rx=2`, white text `LIFE` | — | `LB-R1` class 2 — shared code, configuration keyed by `(lob, …)` |
| Deferred / ghost | `#ffffff` | `#cbd5e1` dashed `5 4`, text `#94a3b8` | Not in this horizon; carries the release it arrives in (`HA-05`) |
| Boundary tab | `#0f172a` | — | The `LY-1` band label |
| Release chip | `R0` `#1d4ed8` · `R1` `#0f766e` · `R2` `#b45309` · `R3` `#7e22ce` · `R4` `#be123c` · `RN` `#4338ca` | — | North Star only — the release in which the element first exists |

**Colour deconfliction, because two tokens sit close in hue.** The control badge `#dc2626` and the
LOB tag `#e11d48` are both red. They are told apart by **shape and text**, not hue: a control is a
rounded pill (`rx=7`) reading `C1` / `C2` / `C4`; an LOB tag is a square tag (`rx=2`) reading the
LOB name. Legend them adjacently so the difference is stated where the reader meets it. Likewise
the LOB cell stroke `#e11d48` and the `R4` release chip `#be123c` never co-occur on one element —
the cell stroke is only ever a container, the chip only ever a 30 × 15 badge.

**The two files use different primary colour axes, and that is deliberate.** `hdl.svg` colours by
**release**, because its question is *when*. `r0-reference-architecture.svg` colours by **build
wave**, because its question is *in what order*. The LOB encoding above is orthogonal to both and
identical in both, which is what makes the LOB reading transferable between the files.
| Text | heading `#0f172a` · body `#475569` · section label `#64748b` | | |

**Rule HA-08 — never introduce a colour without adding it to the legend in the same edit.** An
unlegended colour is a claim the reader cannot decode.

### 4.3 Node template

```xml
<!-- Service node: wave colour, optional wave badge, optional control badge, optional step badge -->
<rect x="X" y="Y" width="140" height="70" rx="6" fill="WAVE_FILL" stroke="WAVE_STROKE" stroke-width="1.5"/>
<rect x="X+6" y="Y+6" width="24" height="15" rx="4" fill="WAVE_STROKE"/>
<text x="X+18" y="Y+17.5" text-anchor="middle" font-size="9" font-weight="bold" fill="#ffffff">W2</text>
<circle cx="X+126" cy="Y+14" r="9" fill="#0f172a"/>
<text x="X+126" y="Y+17.5" text-anchor="middle" font-size="10" font-weight="bold" fill="#ffffff">3</text>
<rect x="X+90" y="Y+6" width="24" height="15" rx="7" fill="#dc2626"/>
<text x="X+102" y="Y+17.5" text-anchor="middle" font-size="9" font-weight="bold" fill="#ffffff">C2</text>
<text x="X+70" y="Y+35" text-anchor="middle" font-size="12"  font-weight="bold" fill="#0f172a">#6  Consent</text>
<text x="X+70" y="Y+50" text-anchor="middle" font-size="9"  fill="#475569">append-only grants ·</text>
<text x="X+70" y="Y+61" text-anchor="middle" font-size="9"  fill="#475569">customer-device OTP required</text>
```

### 4.4 Edge templates

```xml
<!-- synchronous -->
<line x1="" y1="" x2="" y2="" stroke="#334155" stroke-width="1.8" marker-end="url(#arr)"/>
<!-- asynchronous: outbox event / poll / callback -->
<line x1="" y1="" x2="" y2="" stroke="#94a3b8" stroke-width="1.6" stroke-dasharray="5 4" marker-end="url(#arrGray)"/>
<!-- edge label -->
<text x="" y="" text-anchor="middle" font-size="9" fill="#475569">authz — fail closed</text>
```

**Rule HA-09 — every edge carries a style and, where consequential, a seam id.** A line with no
style and no label asserts a coupling nobody designed (`OR-06`).

**Rule HA-11 — in a banded layout, an edge is one hop or it is not a line.** Draw an edge only
between adjacent bands, in the 34 px gutter that separates them, with its label beside it. An edge
whose endpoints are more than one band apart is **not drawn**: it goes in the *cross-boundary
seams* strip as `seam id · source → target · what it carries`. This is not cosmetic. A line routed
around four bands crosses a dozen unrelated boxes, and every one of those crossings reads as a
coupling to a reader who does not already know the answer — so the diagram becomes less true the
more of the truth you try to draw on it. The strip states the same fact and states it exactly.

The `R0` file currently names five: `S-04/S-05` `#4 → CBS` · `S-13` `#12 → AU Bank PG` ·
`S-14/S-15` `AU Bank PG → #12` · `S-17` outbox `→ #16` · `S-18` `#17 → customer device`.

### 4.5 Footer contract

The footer carries provenance and must be kept truthful:

- **signature status** — currently `AI-DRAFTED in the Architecture lane (Mahesh — Board 1); mandatory
  human T4 Architecture sign-off outstanding`;
- **derivation** — which source documents the render was built from;
- **deferrals** — what is deliberately absent, matching `03-solution-architecture-r0.md §3`
  exactly: `#1` Customer BFF and `#18` Reporting & MIS at S13, the `#19` Administration & Config
  **user interface** at R1+ (the configuration *layer* ships in R0 as `W0b`), and campaign / bulk
  lead management on top of `#5` (origination itself is `W1`, `AC-9`);
- **standing exceptions** — e.g. Render.com is dev-preview only, never a PII path (`ADR-001`);
- **date**.

**Rule HA-10 — never upgrade the signature status in a diagram edit.** Signature status changes when
a human signs, and only then.

---

## 5. Change procedure

Seven steps. Steps 1–3 are where the work is; 4 is mechanical.

1. **Resolve context.** Current stage, scope and accepted decisions from
   [`CURRENT-STATE.yaml`](../../../governance/state/CURRENT-STATE.yaml). Confirm the horizon
   (`HR-01`) and therefore the file (`HA-04`).
2. **Check the capability model.** Does [`10`](./10-north-star-capability-model.md) decide what the
   diagram will assert? If not, write or update the capability contracts first (`HA-01`).
3. **Update the authoritative source.** Service set, seam catalogue, constraints, invariants,
   `ARCH-*` log — whichever the change touches. Write the ADR if `04 §8` requires one.
4. **Edit the diagram.** Identify the band (`§4.1`), apply the node/edge templates (`§4.3`, `§4.4`),
   keep the geometry, update the legend if a token was added.
5. **Update the footer** — derivation, deferrals, date. Not the signature status (`HA-10`).
6. **Run the consistency checklist** (`§6`).
7. **Record the trail** — commit message naming the source document that changed first, and the ADR
   or decision row if one exists.

**Adding a service to `hdl.svg` — worked shape.** Row A and row B each hold four 140-wide nodes at
`x = 730 · 885 · 1040 · 1195`. A fifth needs either the row extended left to `x=575` (space exists
between the WS-2 box at `x≤330` and `x=730`) or a new row band at `y=760–830`, which pushes the
integration row down by 80. Prefer extending left: it leaves every other band untouched.

---

## 6. Consistency checklist

Run before committing any HLD change. Each row has bitten a real architecture document somewhere.

| # | Check |
|---|---|
| 1 | Every `#n` context number matches the service catalogue in `architecture-review/02` |
| 2 | Every wave badge matches the build order in `03-solution-architecture-r0.md §3` |
| 3 | Every control badge (`C1` `C2` `C4`) matches the non-waivable control set |
| 4 | Every seam id (`S-01`–`S-22`) matches the seam catalogue, including its style |
| 5 | Every invariant reference (`INV-*`) and constraint (`SC-W3-*`) exists and says what the label claims |
| 6 | Every fitness-function reference (`FF-01`–`FF-21`) exists |
| 7 | Deferred items in the footer match the deferral list in `03-solution-architecture-r0.md §3` |
| 8 | Every colour used appears in the legend (`HA-08`) |
| 9 | Every edge has a style; consequential edges carry a seam id (`HA-09`) |
| 10 | Nothing crosses the AWS boundary that should be inside it, and vice versa |
| 11 | External systems use the dashed external token |
| 12 | Signature status unchanged unless a human actually signed (`HA-10`) |
| 13 | Date updated |
| 14 | Text within node bounds; no overlap at 100% zoom |
| 15 | The diagram asserts nothing the source documents do not (`HA-02`) |
| 16 | If the change was target-state, the R0 diagram is reconciled or explicitly unaffected (`HA-06`) |
| 17 | Every element rendering a bounded context carries its `#n` **on the element**, with the canonical name, and the `→ target name` form where they differ (`NC-1`) |
| 18 | Both files lay out in the `LY-1` bands, same order; a band empty at this horizon is drawn and labelled, not omitted |
| 19 | Every node is in exactly one `LB-R1` class, and the classes match `CF-2` and `LB-5` |
| 20 | **Cross-file reconciliation.** For every `#n` present in both files: same canonical name, same class, and a release chip consistent with `03-solution-architecture-r0.md §3`. Run the diff below before committing either file |

### 6.1 The cross-file diff

Check 20 is mechanical. Both files are plain SVG text, so the context numbers and names are
greppable:

```bash
for f in docs/hdl.svg docs/architecture/r0-reference-architecture.svg; do
  echo "== $f"; grep -oh '#[0-9]\{1,2\}[^<]\{0,40\}' "$f" | sort -u
done
```

Read the two lists side by side. A number present in one and absent from the other is only
correct if the context is genuinely out of R0 — `#1` and `#18` are the two legitimate cases. Any
other asymmetry, and any name that differs without the `NC-1` arrow form, is the finding.

---

## 7. What must never appear on an HLD

| Never | Why |
|---|---|
| A service the source documents do not name | `HA-02` |
| An unlabelled edge between two services | Asserts an undesigned coupling (`OR-06`) |
| A capability drawn as built when it is planned | `TP-03`, `HA-05` |
| A shared database between services | `TI-05` — if it is true, that is a finding, not a drawing |
| A provider schema or vendor product name inside a business service box | `TI-02` |
| An RM or bank device on the payment path | `TI-11` — the single most consequential control to depict correctly |
| A journey box that owns another context's decision | `SC-W3-6` |
| A business service box drawn with a line to a named insurer or aggregator | `TI-19` — providers are reached through the aggregation layer, never directly |
| An upgraded signature status | `HA-10` |
| A bounded context drawn without its `#n` | `NC-1` — the reader cannot match the box to the register |
| A target-state rename shown without the canonical name | `NC-1` — an undeclared rename reads as a new service |
| A shared-platform service drawn inside an LOB cell, or a cell service drawn in the shared plane | `LB-5` — partitioning the rules is not forking the evidence |
| A band from `LY-1` silently omitted because this horizon has little in it | An omitted band reads as a boundary that does not exist (`LY-1`) |
| An unlegended colour or badge | `HA-08` |

---

## 8. Authority

| Change | Authority |
|---|---|
| Layout, legend, labels, colour tokens, presentation | `A1_AUTONOMOUS` |
| Updating a diagram to match an already-accepted source change | `A1_AUTONOMOUS` |
| A diagram change that asserts a **new** service, seam or data flow | Requires the source change first — carries that change's authority class |
| A diagram change touching trust boundaries or the money path | `A3_JOINT_REVIEW` — Deepali; Shailja where regulated data crosses |
| Marking the design as signed | `A4_HUMAN_REQUIRED` |
