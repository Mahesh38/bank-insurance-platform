# Diagrams as code

The platform-team views in [`../`](../README.md) are **generated**, not drawn. The source is
[`r0_platform_topology.py`](./r0_platform_topology.py); the PNGs beside it are build output.

```bash
pip install -r docs/architecture/diagrams/requirements.txt
apt-get install -y graphviz            # provides `dot`
python3 docs/architecture/diagrams/r0_platform_topology.py
```

## Why this toolchain

| | |
|---|---|
| **Library** | [`mingrammer/diagrams`](https://diagrams.mingrammer.com) — Python, renders through Graphviz |
| **Icons** | The official AWS, Kubernetes, Argo and Flutter icon sets are **bundled inside the pip wheel** (which is why it is ~34 MB). Nothing is fetched at render time and no image is vendored into this repository |
| **Output** | PNG. Graphviz can emit SVG, but it references the icon files by absolute local path, so the SVG is not portable — do not switch the output format without solving that |

The reason it is code rather than a `.drawio` or Lucid file is `HA-03`: the picture has to change in
the same commit as its source, and a reviewer has to be able to see *what* changed. A binary
canvas file gives a reviewer "the diagram changed"; a diff of this file gives them the sentence.

## What each file answers

| Output | Question |
|---|---|
| `r0-platform-topology.png` | What runs where — zones, subnets, namespaces, the two-hop proxy |
| `r0-platform-az.png` | Which availability zone each resource sits in |
| `r0-platform-dr.png` | What exists in `ap-south-2`, and what deliberately does not |
| `r0-platform-sequence.png` | In what order the platform team builds it |
| `r0-platform-payment.png` | The C4 payment path — the hop people get wrong |

## The rule these files live under

They are **renderings** (`HA-02`). They own nothing. Every element is named by
[`../R0-LLD.md`](../R0-LLD.md); where a diagram and that file disagree, the diagram is the defect.

Two practical notes for anyone editing the source:

- The invisible edges in `topology()` are **layout control only**, and are commented as such.
  Graphviz places sibling clusters side by side, which turns six namespaces into a 7,000-pixel
  strip; the invisible edges impose a reading order. They carry no architectural meaning and must
  never be read as dependencies.
- The C4 payment path is deliberately in its own file. Drawn on top of the placement picture it
  runs device → PG → callback → back into the VPC, and that one loop makes both diagrams
  unreadable.
