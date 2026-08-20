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
| **`docs/hdl.svg`** | **Rendering** | Nothing. It *depicts* the R0 design |

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
| `docs/hdl.svg` | **H0 — R0 as designed** | R0 scope, service set, seams or controls change |
| `docs/hld-h1.svg`, `docs/hld-h2.svg`, … | H1 / H2 target states | Created on demand, same canvas contract |
| `docs/hld-north-star.svg` | The five-plane capability view | Created when the capability model is complete enough to be worth drawing |

**Rule HA-05 — never mix built and aspirational without visual distinction.** If a horizon diagram
shows both, unbuilt elements carry a dashed stroke plus a status badge. `TP-03` is what this rule
enforces.

**Rule HA-06 — when a horizon diagram changes the R0 delta, reconcile `hdl.svg` in the same
change.** A target-state diagram that silently contradicts the R0 diagram leaves two answers in the
repository, and the reader has no way to know which is current.

**Rule HA-07 — the North Star view is drawn in planes, not in boxes-and-arrows.** The five-plane
model (`10 §4`) is the stakeholder artefact. Drawing the North Star as a component diagram
reintroduces exactly the service-count conversation the capability model exists to prevent.

---

## 4. Canvas contract for `docs/hdl.svg`

Documented so an edit is a local change, not a re-layout.

### 4.1 Geometry

| Band | `y` range | Contents |
|---|---|---|
| Title | 20 – 65 | Title (25 px bold) + subtitle (12.5 px) |
| Journey ribbon | 80 – 124 | Section label at `y=80`; chevrons `y=86–124`, 163 wide, 25 px gap, notch 12 px |
| Channels / externals | 150 – 235 | Bank AD `x=60 w=240` · Flutter `x=360 w=340` · Customer device `x=1480 w=270` |
| Edge | 270 – 340 | Edge `x=360 w=340` · Payment gateway `x=1480 w=270` |
| **AWS boundary** | **375 – 1205** | `x=40 w=1360`, dashed `#94a3b8`, fill `#f8fafc`, label at `y=398` |
| WS-2 identity box | 420 – 660 | `x=70 w=260` |
| Service row A | 420 – 490 | `x=420 (w=240)` · `730` · `885` · `1040` · `1195` (w=140) |
| Journey Orchestration | 565 – 635 | `x=420 w=915` — full-width, spans the row |
| Service row B | 680 – 750 | `x=730` · `885` · `1040` · `1195` (w=140) |
| Integration row | 820 – 890 | Hub `x=730 w=450` · 1SB adapter `x=1230 w=160` |
| Platform row | 935 – 1005 | Audit `x=420 w=450` · Notification `x=900 w=400` |
| Legend | 700 – 1005 | `x=70 w=260` |
| Data band | 1040 – 1180 | `x=70 w=1275` |
| Right externals | 420, 820 | `x=1480 w=270` — CBS, 1SilverBullet |
| Foundation strip (`IF-3`) | 1205 – 1360 | S08 / S09 |
| Footer | ~1380 | Provenance, signature status, date |

Canvas `1800 × 1400`. Standard node `w=140 h=70`; wide node `w=240`; spanning nodes as above.
Horizontal gutter between 140-wide nodes: 15 px.

### 4.2 Colour tokens

| Token | Fill | Stroke | Meaning |
|---|---|---|---|
| Wave 1 | `#dbeafe` | `#2563eb` | Journey spine — build first |
| Wave 2 | `#fef3c7` | `#d97706` | Consent · suitability · quote |
| Wave 3 | `#dcfce7` | `#16a34a` | Money · policy · audit |
| Wave 4 | `#f3e8ff` | `#9333ea` | BFF · Flutter · notification |
| WS-1 supplier | `#e2e8f0` | `#475569` | Exists today |
| WS-2 enabler | `#ecfdf5` | `#059669` | Workforce identity |
| External | `#ffffff` | `#64748b` dashed `3 2` | Outside the platform |
| Control badge | `#dc2626` | — | Non-waivable `C1` · `C2` · `C4` |
| Journey-step badge | `#0f172a` circle | — | Ribbon step number |
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

### 4.5 Footer contract

The footer carries provenance and must be kept truthful:

- **signature status** — currently `AI-DRAFTED in the Architecture lane (Mahesh — Board 1); mandatory
  human T4 Architecture sign-off outstanding`;
- **derivation** — which source documents the render was built from;
- **deferrals** — what is deliberately absent (`#5` Lead, `#1` Customer BFF, `#18` Reporting, `#19`
  Admin UI at S13);
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
| 4 | Every seam id (`S-01`–`S-19`) matches the seam catalogue, including its style |
| 5 | Every invariant reference (`INV-*`) and constraint (`SC-W3-*`) exists and says what the label claims |
| 6 | Every fitness-function reference (`FF-01`–`FF-15`) exists |
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
