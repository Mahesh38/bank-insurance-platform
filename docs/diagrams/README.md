# Platform diagrams

Hand-authored architecture diagrams, and generators for the ones that are large enough
that hand-placing every coordinate stops being useful.

**The bar is set by [`../hdl.svg`](../hdl.svg)** — the R0 high-level design. Everything here is
built to read as part of that one set, and anything that does not meet its standard does not
belong in this folder.

## What that standard actually is

`hdl.svg` is worth studying before adding a diagram, because it does five things that generated
diagrams routinely do not:

1. **Every box makes a falsifiable claim.** `300 ms · no retry · FAIL CLOSED (S-02)`, not
   "handles authorization". If a box's second line could be pasted onto any other project's
   diagram unchanged, it says nothing.
2. **Every claim is anchored to an identifier** — `#n` bounded contexts from
   [`business-problem-statement.md §6`](../context/business-problem-statement.md), `W1–W4` build
   waves, `C1/C2/C4` non-waivable controls, `S-nn` specs, `INV-*` invariants, `FF-nn` fitness
   functions, `ARCH-nnn` ADRs, `IF-n` workstream interfaces.
3. **It commits to reality** — `ap-south-1`, warm standby in `ap-south-2`, EKS, Aurora,
   DynamoDB, S3 Object Lock, Keycloak, `≥ 2 pods across ≥ 2 AZs`. A technology-free HLD is a
   vision document wearing an HLD's title.
4. **It distinguishes what exists from what is to be built** — `WS-1 · EXISTS TODAY` on the 1SB
   adapter; the note that `bank-persistence-service` stays scoped to the integration job store and
   is *not* extended to the R0 business contexts.
5. **Edges are typed and the mechanism is named on them** — solid sync, dashed async, with
   `transactional outbox (S-17), at-least-once, no Kafka in R0` written on the edge itself.

Trust boundaries are drawn as boundaries (the customer device is its own zone carrying **C4**),
and the legend earns its space.

## Contents

Twelve diagrams, four generator scripts, one shared design system ([`src/svgkit.py`](./src/svgkit.py)).

| Diagram | View | Source |
|---|---|---|
| [`../hdl.svg`](../hdl.svg) | **Authoritative** R0 high-level design — estate, waves, data, zones, the S08/S09 paved road | hand-authored |
| [`solution-vision.svg`](./solution-vision.svg) | problems → five commitments → outcomes with measures → R0/R1/R2 boundary | `build_structure.py` |
| [`capability-map.svg`](./capability-map.svg) | every capability by group, owning context, wave and honest R0 position | `build_structure.py` |
| [`domain-ownership.svg`](./domain-ownership.svg) | 19 bounded contexts — what each owns and what it must **never** own | `build_structure.py` |
| [`system-context.svg`](./system-context.svg) | externals: what crosses each edge, under which contract, and how it fails | `build_structure.py` |
| [`quote-path.svg`](./quote-path.svg) | lookup → suitability (C1) → consent (C2) → quote, and the five refusals | `build_flow.py` |
| [`r0-money-path.svg`](./r0-money-path.svg) | proposal → payment → reconciliation → issuance → SOLD, and five failure branches | `r0_money_path.py` |
| [`journey-state-machine.svg`](./journey-state-machine.svg) | 17 states, terminal states, 6 forbidden transitions, compensation per failure | `build_flow.py` |
| [`trust-zones.svg`](./trust-zones.svg) | five zones and every boundary crossing with what proves the control | `build_flow.py` |
| [`risk-register.svg`](./risk-register.svg) | L×I exposure grid on the repo's own scale, criticals, closed risks, assumptions | `build_governance.py` |
| [`dependency-map.svg`](./dependency-map.svg) | dependencies with one named owner, a date and an escalation trigger | `build_governance.py` |
| [`approval-model.svg`](./approval-model.svg) | the actual AIGEM seven-board model — replaces the v0.1 five-step CTO flow | `build_governance.py` |
| [`gate-coverage.svg`](./gate-coverage.svg) | S06/S07 criterion by criterion: met, partial, open, unclosable by any AI | `build_governance.py` |

These are the visual half of the [architecture pre-approval pack v0.2](../au-bank-insurance-platform/architecture-pre-approval/README.md).

### Still to build

| Diagram | Why | Gate |
|---|---|---|
| **Logical data model per context** | ownership tables are not a data model; blocks Aarti's sign-off | S06-G4, S07-G5 |
| **Threat model per trust boundary** | `trust-zones.svg` gives the boundaries; the model itself is Deepali's | S07-G3 |
| **Deployment / environment topology** | VPC, subnets, AZs, DR pair, promotion path | S07-G1 |
| **Evidence & audit chain** | S06-G7 is **E3** — reconstruction must be demonstrated, not drawn | S06-G7 |

## Regenerating

```bash
python3 docs/diagrams/src/r0_money_path.py > docs/diagrams/r0-money-path.svg
```

No dependencies, deterministic output. To preview a change:

```bash
/opt/pw-browsers/chromium-1194/chrome-linux/chrome --headless --disable-gpu --no-sandbox \
  --window-size=1900,1500 --screenshot=/tmp/preview.png file://$PWD/docs/diagrams/r0-money-path.svg
```

## Why SVG with a committed generator, and not DOCX-embedded PNG

A PNG in a Word file is reviewable exactly once. SVG diffs, scales, renders inline on GitHub, and
can be regenerated from a source file when a decision changes — so a reviewer's comment can be
answered with a one-line edit rather than a re-render of nineteen binaries. See finding **F-61**
in [`PR-55-360-STAKEHOLDER-REVIEW-v1.0.md`](../au-bank-insurance-platform/architecture-pre-approval/reviews/PR-55-360-STAKEHOLDER-REVIEW-v1.0.md).

## Status

`r0-money-path.svg` is **AI-drafted, v0.1, not approved and not authorised for development.**
It is a companion to the hand-authored `hdl.svg`, not a replacement for it, and it does not carry
any human signature.
