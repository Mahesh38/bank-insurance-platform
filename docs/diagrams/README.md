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

| Diagram | View | Source |
|---|---|---|
| [`../hdl.svg`](../hdl.svg) | **Static** — R0 target state: estate, waves, data, trust zones, the S08/S09 paved road | hand-authored |
| [`r0-money-path.svg`](./r0-money-path.svg) | **Dynamic** — proposal → UW → payment → reconciliation → issuance → SOLD, twenty steps and the five failure branches | [`src/r0_money_path.py`](./src/r0_money_path.py) |

### Planned — the rest of the set

| # | Diagram | Why it is needed | Answers |
|---|---|---|---|
| D3 | **Journey state machine** — every state, legal transition, terminal state, compensation | The pack describes the happy path in prose; nothing defines the states | S06-G2, S06-G3 |
| D4 | **Quote path flow** — lookup → suitability C1 → consent C2 → quote via Hub, with the hard-gate rejections | The lawful-gate half of the journey, upstream of the money path | S06-G6 |
| D5 | **Threat model per trust boundary** — data-flow decomposition, per-boundary threats, residual rating | **S07-G3 requires it**; a flat threat table cannot satisfy an E2 human signature | S07-G3 |
| D6 | **Logical data model + ownership** — aggregate per context, golden source vs approved copy, retention class | Ownership tables are not a data architecture | S07-G5 |
| D7 | **Deployment / environment topology** — VPC, subnets, AZs, DR pair, promotion path | Nine prose lines today | S07-G1 |
| D8 | **Evidence & audit chain** — what is written, by whom, when, and how a journey is reconstructed | S06-G7 is an **E3** criterion: reconstruction must be demonstrated | S06-G7 |

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
