# Diagrams as code

The platform-team views in [`../`](../README.md) are **generated**, not drawn:

```bash
pip install -r docs/architecture/diagrams/requirements.txt
python3 docs/architecture/diagrams/r0_platform_views.py
```

Each run writes an `.svg` (the deliverable) and a `.png` companion beside it.

| Source | What it is |
|---|---|
| [`svgcanvas.py`](./svgcanvas.py) | A ~200-line canvas: nodes at coordinates, groups as rectangles, connectors as axis-aligned segments. No layout engine |
| [`r0_platform_views.py`](./r0_platform_views.py) | The five views — every coordinate chosen, every connector routed |

## Why there is no layout engine here

The first version of these used [`mingrammer/diagrams`](https://diagrams.mingrammer.com), which
renders through Graphviz. Graphviz decides positions for you, and that is exactly the problem:

- Nodes land where the ranking algorithm puts them, so alignment changes between renders.
- Edges are splines — they curve, they cross, and they take diagonal paths.
- `splines=ortho`, its 90-degree mode, **detaches edge labels** from their edges and routes lines
  straight through cluster borders. It was tried and rejected on those grounds, not assumed to fail.

So the layout engine was removed and the icons kept. `svgcanvas.py` places every element at a
coordinate this file chooses and routes every connector as segments this file chooses. That is more
typing, and it is the only way to get alignment that holds still.

## The two mechanics worth knowing before editing

**Bottom ports clear the caption.** A node's caption hangs below its icon. A connector leaving the
bottom therefore starts below the *text*, not below the icon — otherwise the line is drawn straight
through the node's own label. `Node.port("B")` handles this; it is why `Node` remembers
`label_bottom`.

**Corridors are named, not guessed.** Where a connector needs to travel past other content it is
given an explicit `lane=`, and the lanes are constants at the top of the module (`LANE_EGRESS`,
the `COL` list). Two connectors sharing a corridor is a layout decision, so it is written down.

Edge labels sit on a white plate rather than relying on the SVG `paint-order` property, which
cairosvg and older librsvg ignore — the label would render as a white smear in exactly the tools a
platform team is most likely to open the file in.

## Icons

The official AWS, Kubernetes, Flutter and Argo icon sets ship **inside the `diagrams` pip wheel**
(which is why it is ~34 MB). `svgcanvas.icon()` reads them from there and embeds them as base64, so
the SVG is self-contained: no external request at view time, and no image vendored into this
repository. Set `DIAGRAM_ICON_ROOT` to override where they are read from.

## What each file answers

| Output | Question |
|---|---|
| `r0-platform-topology` | What runs where — zones, subnets, namespaces, the two-hop proxy |
| `r0-platform-az` | Which availability zone each resource sits in |
| `r0-platform-dr` | What exists in `ap-south-2`, and what deliberately does not |
| `r0-platform-sequence` | In what order the platform team builds it |
| `r0-platform-payment` | The C4 payment path — the hop people get wrong |

## The rule these files live under

They are **renderings** (`HA-02`). They own nothing. Every element is named by
[`../R0-LLD.md`](../R0-LLD.md); where a diagram and that file disagree, the diagram is the defect.
They are also **build output** — editing an `.svg` or `.png` instead of the source is the same
defect class as editing a compiled artefact.
