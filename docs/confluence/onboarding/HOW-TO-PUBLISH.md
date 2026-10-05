# How to publish this pack to Confluence

You cannot push these files into Confluence from git automatically. Create the pages once, then re-attach pictures if a diagram regenerates.

**Work item:** `SUG-20261005-cfp`

## Before you start

1. Pick the Confluence **space** (typically the programme / bancassurance space).
2. Decide the **space-level parent**. If the space already has a "Home" page, the main page of this pack sits **under that Home**. If the space is empty, this pack's main page *is* the space home.
3. You need permission to create pages and attach files.

## Step 1 — create the MAIN page

1. Create a page titled exactly: **AU Bank Insurance Platform — Start here**
2. Paste the body of [`pages/00-home.md`](./pages/00-home.md) (skip the first `#` heading — Confluence already has the title).
3. Attach these files from `diagrams/`:
   - `00-page-tree.png`
   - `01-as-is-vs-target.png`
   - `02-r0-outcome.png`
   - `07-workstreams.png`
4. Insert each PNG **above** the matching heading. Prefer PNG over SVG in Confluence (wider viewer support).
5. Add a **Children Display** macro (excerpt: none, depth: 2) at the bottom so the tree stays live.

## Step 2 — create the ten child pages

Create each child **from the main page** (ellipsis → *Create nested page*, or create then *Move*).

Paste the matching `pages/0N-*.md` body. Attach the PNGs named in that file.

Recommended create order: 1 Why → 2 What → 3 Who → 4 Use cases → 5 Journey → 6 Architecture → 7 Sequences → 8 Rules → 9 Repo → 10 Read next.

## Step 3 — nest the grandchildren

On **Architecture**, create nested pages:

- How a request travels → attach `05-hop-architecture.png`
- Bounded contexts and services → attach `06-bounded-contexts.png`

On **Sequence diagrams**, create nested pages:

- Sequence — RM login → `10-seq-rm-login.png`
- Sequence — Lead create and assign → `11-seq-lead.png`
- Sequence — Quote via 1SB → `12-seq-quote.png`
- Sequence — Payment and issuance → `13-seq-payment.png`

Confirm the tree with *Page tree* macro or the screenshot in [`PAGE-TREE.md`](./PAGE-TREE.md). If 6.1 or 7.1 sits next to "Start here", it is in the wrong place — *Move* it under Architecture / Sequence diagrams.

## Step 4 — mermaid (optional)

Each sequence page also contains a ```mermaid fenced block. If the space has **Mermaid Charts for Confluence** (or the `/mermaid` macro):

1. Keep the PNG as the picture everyone sees.
2. Paste the mermaid source into a Mermaid macro **under** an "Editable source" heading so the next editor can change the flow without Photoshop.

If there is no mermaid app, delete the mermaid section when pasting. The PNG is enough.

## Step 5 — official architecture pictures (optional, Architecture page)

The teaching diagrams are simplified. On the Architecture page you may also attach the real renderings (they are large):

| File in this repo | What it is |
|-------------------|------------|
| `docs/hdl.svg` | North Star — target state by release. Not R0 scope. |
| `docs/architecture/r0-reference-architecture.svg` | R0 executable architecture |
| `docs/architecture/R0-HLD.md` | Stakeholder walkthrough of that picture |

Do **not** present `hdl.svg` as "what we are building this quarter". Only its R0 band is admitted scope.

## Refreshing after a change

```bash
python3 scripts/confluence/render_onboarding_diagrams.py
```

Re-attach the changed PNGs on the Confluence pages. Do not hand-edit the SVG.

## What not to do

- Do not mark these pages as SSOT, BRD, or approved architecture.
- Do not copy DIY / Health / Motor / NTB into the R0 pages. Those are later releases.
- Do not paste `CURRENT-STATE.yaml` stage fields as if an agent had advanced a gate.
