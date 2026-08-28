# Figma extraction

Extracted design data for the **For Client Review** file
(`JyLGAaO88ELjnyVF2FQ3Bx`), the UX reference behind the Batch 1 journeys.

Figma is a **UX reference only, not a source of truth** (D-012). Anything load-bearing
belongs in the requirement and architecture documents first — see
[`references/README.md`](../../au-bank-insurance-platform/references/README.md).

## Status: incomplete

`JyLGAaO88ELjnyVF2FQ3Bx/` currently holds a **depth-limited skeleton** — 203 nodes,
16 pages, 168 top-level frames, but **2 text strings**. 184 containers have an empty
child list, meaning their contents were never fetched. Page `868:21686` ("Batch 1 VD")
resolves to three sections — Create New Lead, Needs Assessment, Select Product
Manually — all three empty.

It was derived from the pre-existing [`docs/figma_file_structure.json`](../../figma_file_structure.json),
not from a fresh pull. Screen content, real colours, typography and component structure
are **not** in here yet.

## Why it is incomplete

`api.figma.com` is not on the egress allowlist for the Claude Code sandbox — CONNECT is
refused with a 403 by the policy proxy, so no API call can be made from a session. The
whole domain is blocked, not just the API host.

## Completing it

Run this anywhere with network access to `api.figma.com` and a token that can read the
file (scope `file_content:read`):

```bash
export FIGMA_TOKEN=figd_...        # never commit this; it is a bearer credential
python3 scripts/figma/figma-extract.py \
    --file-key JyLGAaO88ELjnyVF2FQ3Bx \
    --node-id 868:21686 \
    --images
```

That overwrites everything under `JyLGAaO88ELjnyVF2FQ3Bx/` with the full tree, and adds
`raw/file.json` plus `raw/images.json`. Rendered-image URLs expire within ~30 days —
download them in the same run if the PNGs are needed.

To re-derive the readable files from a raw dump someone else pulled, without any API call:

```bash
python3 scripts/figma/figma-extract.py --file-key JyLGAaO88ELjnyVF2FQ3Bx \
    --from-json path/to/file.json
```

## What each file is

| File | What it is |
|---|---|
| `CONTEXT.md` | Orientation brief: source metadata, scale, page and frame inventory, top tokens |
| `content.md` | Every text string, grouped by page and frame |
| `inventory.json` | Page/section/frame skeleton with ids and pixel sizes |
| `tokens.json` | Fills, strokes, typography and effects actually used, by frequency |
| `raw/file.json` | Full unabridged API response (only after a live run) |

All five are **generated**. Fix the extractor, not the output.
