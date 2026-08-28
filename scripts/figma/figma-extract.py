#!/usr/bin/env python3
"""Extract a Figma file into raw JSON, readable content, design tokens and a context file.

The Figma REST API is the only source for screen content: a shared prototype link renders
pixels, not structure. This pulls the full node tree once and derives everything else from
that single fetch, so a re-run is cheap and deterministic.

  export FIGMA_TOKEN=figd_...                       # never commit this
  python3 scripts/figma/figma-extract.py --file-key JyLGAaO88ELjnyVF2FQ3Bx
  python3 scripts/figma/figma-extract.py --file-key <key> --node-id 868:21686
  python3 scripts/figma/figma-extract.py --file-key <key> --images   # +PNG renders
  python3 scripts/figma/figma-extract.py --file-key <key> --from-json dump.json

`--from-json` re-derives every output from a previously saved API response instead of
calling the API. Use it to refresh the derived files, or where egress to api.figma.com
is blocked and someone else can supply the raw dump.

Outputs (under --out, default docs/design/figma/<file-key>/):
  raw/file.json          full document tree, unabridged
  raw/nodes-<id>.json    per-node subtree when --node-id is given
  raw/images.json        node id -> rendered PNG URL, when --images
  content.md             every text string, grouped by page and frame
  tokens.json            colours, typography, effects and grids actually used
  inventory.json         page/section/frame skeleton with ids and sizes
  CONTEXT.md             the human-readable brief: what this file contains

Needs the stdlib only. The token is read from FIGMA_TOKEN and is never written to disk.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from collections import Counter, OrderedDict
from pathlib import Path

API = "https://api.figma.com"
# Figma renders at most this many node ids per /v1/images call.
IMAGE_BATCH = 50


# --------------------------------------------------------------------------- fetch


def _get(path: str, token: str, retries: int = 4) -> dict:
    """GET an API path, retrying transient failures with exponential backoff."""
    url = f"{API}{path}"
    delay = 2
    for attempt in range(retries + 1):
        req = urllib.request.Request(url, headers={"X-Figma-Token": token})
        try:
            with urllib.request.urlopen(req, timeout=180) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", "replace")[:400]
            # 429 and 5xx are worth another go; 401/403/404 are not.
            if exc.code in (429, 500, 502, 503, 504) and attempt < retries:
                wait = int(exc.headers.get("Retry-After") or delay)
                print(f"  {exc.code} — retrying in {wait}s", file=sys.stderr)
                time.sleep(wait)
                delay *= 2
                continue
            raise SystemExit(
                f"Figma API {exc.code} on {path}\n{body}\n\n"
                "401/403 means the token is invalid, expired, or lacks file_content:read.\n"
                "404 means the file key is wrong or the token's account cannot see the file."
            )
        except urllib.error.URLError as exc:
            if attempt < retries:
                print(f"  {exc.reason} — retrying in {delay}s", file=sys.stderr)
                time.sleep(delay)
                delay *= 2
                continue
            raise SystemExit(
                f"Cannot reach {API}: {exc.reason}\n\n"
                "If this is a sandbox, api.figma.com is probably not on the egress allowlist."
            )
    raise SystemExit("unreachable")


# ---------------------------------------------------------------------------- walk


def walk(node: dict, path: tuple = ()):
    """Yield (ancestor_names, node) for every node in the tree, depth first."""
    yield path, node
    here = path + (node.get("name", ""),)
    for child in node.get("children") or []:
        yield from walk(child, here)


# -------------------------------------------------------------------------- derive


def rgba(colour: dict, opacity: float | None = None) -> str:
    """Render a Figma colour as #rrggbb, or rgba() when it is not fully opaque."""
    r, g, b = (round(colour.get(k, 0) * 255) for k in "rgb")
    alpha = colour.get("a", 1) * (1 if opacity is None else opacity)
    if alpha >= 0.999:
        return f"#{r:02x}{g:02x}{b:02x}"
    return f"rgba({r}, {g}, {b}, {round(alpha, 3)})"


def extract_content(document: dict) -> str:
    """Every visible text string, grouped by page then by top-level frame."""
    lines = ["# Design content", ""]
    lines.append("Every text string in the file, in document order. Generated — do not edit.")
    lines.append("")
    for page in document.get("children") or []:
        strings = [
            (p, n) for p, n in walk(page)
            if n.get("type") == "TEXT" and (n.get("characters") or "").strip()
        ]
        if not strings:
            continue
        lines += [f"## {page.get('name', '(unnamed page)')}", "", f"`{page['id']}` — {len(strings)} text nodes", ""]
        current = None
        for ancestors, node in strings:
            # ancestors[0] is the page; ancestors[1] is the frame or section it sits in.
            frame = ancestors[1] if len(ancestors) > 1 else "(page root)"
            if frame != current:
                lines += ["", f"### {frame}", ""]
                current = frame
            text = " / ".join(node["characters"].strip().splitlines())
            lines.append(f"- `{node['id']}` {text}")
        lines.append("")
    return "\n".join(lines)


def extract_tokens(document: dict, styles: dict) -> dict:
    """Colours, type styles, effects and grids actually used, ranked by frequency."""
    fills, strokes, type_styles, effects = Counter(), Counter(), Counter(), Counter()
    for _, node in walk(document):
        for paint in node.get("fills") or []:
            if paint.get("type") == "SOLID" and paint.get("visible", True):
                fills[rgba(paint["color"], paint.get("opacity"))] += 1
        for paint in node.get("strokes") or []:
            if paint.get("type") == "SOLID" and paint.get("visible", True):
                strokes[rgba(paint["color"], paint.get("opacity"))] += 1
        style = node.get("style") or {}
        if style.get("fontFamily"):
            key = (
                f"{style.get('fontFamily')} {style.get('fontWeight', '')} "
                f"{style.get('fontSize', '')}/{style.get('lineHeightPx', '')}"
            ).strip()
            type_styles[key] += 1
        for effect in node.get("effects") or []:
            if effect.get("visible", True):
                effects[effect.get("type", "?")] += 1
    return {
        "fills": OrderedDict(fills.most_common()),
        "strokes": OrderedDict(strokes.most_common()),
        "typography": OrderedDict(type_styles.most_common()),
        "effects": OrderedDict(effects.most_common()),
        "publishedStyles": styles,
    }


def extract_inventory(document: dict) -> list:
    """Page -> top-level frame/section skeleton, with ids and pixel sizes."""
    pages = []
    for page in document.get("children") or []:
        entries = []
        for child in page.get("children") or []:
            box = child.get("absoluteBoundingBox") or {}
            entries.append({
                "id": child["id"],
                "name": child.get("name"),
                "type": child.get("type"),
                "width": box.get("width"),
                "height": box.get("height"),
                "childCount": len(child.get("children") or []),
            })
        pages.append({"id": page["id"], "name": page.get("name"), "children": entries})
    return pages


def write_context(meta: dict, inventory: list, tokens: dict, document: dict, out: Path) -> str:
    """The orientation brief: what this file is, what is in it, what was pulled."""
    counts = Counter(n.get("type") for _, n in walk(document))
    total = sum(counts.values())
    text_nodes = counts.get("TEXT", 0)
    # A depth-limited dump leaves containers with an empty children list. Say so loudly:
    # a reader cannot otherwise tell "empty frame" from "not fetched".
    truncated = sum(
        1 for _, n in walk(document)
        if n.get("type") in ("SECTION", "FRAME", "GROUP", "COMPONENT", "INSTANCE")
        and n.get("children") == []
    )
    lines = [
        f"# {meta.get('name', 'Figma file')} — design context",
        "",
        "Generated by `scripts/figma/figma-extract.py`. Do not edit by hand.",
        "",
        "## Source",
        "",
        "| Field | Value |",
        "|---|---|",
        f"| File | {meta.get('name')} |",
        f"| File key | `{meta['fileKey']}` |",
        f"| Last modified | {meta.get('lastModified')} |",
        f"| Version | `{meta.get('version')}` |",
        f"| Editor type | {meta.get('editorType')} |",
        f"| Access role | {meta.get('role')} / {meta.get('linkAccess')} |",
        f"| Extracted | {meta['extractedAt']} |",
        f"| Extracted node | {meta.get('nodeId') or 'whole file'} |",
        "",
        "## Scale",
        "",
        f"- **{total} nodes**, {len(inventory)} pages, {text_nodes} text nodes",
        f"- Node types: {', '.join(f'{k} {v}' for k, v in counts.most_common())}",
        f"- {len(tokens['fills'])} distinct fill colours, {len(tokens['typography'])} type styles",
        "",]
    if truncated:
        lines += [
            f"> **This extract is incomplete.** {truncated} containers have an empty child list,",
            "> meaning the source dump was depth-limited. Text, colours and typography below cover",
            "> only what was fetched. Re-run without `--from-json` for the full tree.",
            "",]
    lines += [
        "## Pages",
        "",
        "| Page | id | Top-level items |",
        "|---|---|---|",
    ]
    for page in inventory:
        lines.append(f"| {page['name']} | `{page['id']}` | {len(page['children'])} |")
    lines += ["", "## Top-level frames and sections", ""]
    for page in inventory:
        if not page["children"]:
            continue
        lines += [f"### {page['name']} (`{page['id']}`)", "",
                  "| id | Type | Size | Name |", "|---|---|---|---|"]
        for c in page["children"]:
            size = f"{int(c['width'])}x{int(c['height'])}" if c.get("width") else "—"
            lines.append(f"| `{c['id']}` | {c['type']} | {size} | {c['name']} |")
        lines.append("")
    top_fills = list(tokens["fills"])[:12]
    top_type = list(tokens["typography"])[:12]
    lines += [
        "## Most-used tokens",
        "",
        "Full set in `tokens.json`.",
        "",
        f"- Fills: {', '.join(f'`{c}`' for c in top_fills) or '—'}",
        "",
        f"- Typography: {', '.join(f'`{t}`' for t in top_type) or '—'}",
        "",
        "## Files here",
        "",
        "| File | What it is |",
        "|---|---|",
        "| `raw/file.json` | Full document tree, unabridged, straight from the API |",
        "| `content.md` | Every text string, grouped by page and frame |",
        "| `tokens.json` | Colours, typography, effects and grids actually used |",
        "| `inventory.json` | Page/section/frame skeleton with ids and sizes |",
        "| `raw/images.json` | Node id to rendered PNG URL (only with `--images`) |",
        "",
        "Figma is a **UX reference, not a source of truth** for requirements. Anything",
        "load-bearing must land in the requirement and architecture documents first.",
        "",
    ]
    return "\n".join(lines)


# ---------------------------------------------------------------------------- main


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--file-key", required=True, help="Figma file key, from the /design/<key>/ URL")
    ap.add_argument("--node-id", action="append", default=[],
                    help="also pull this node subtree, e.g. 868:21686 (repeatable)")
    ap.add_argument("--out", help="output directory (default docs/design/figma/<file-key>)")
    ap.add_argument("--images", action="store_true", help="also resolve PNG render URLs for top-level frames")
    ap.add_argument("--scale", default="2", help="image scale when --images (default 2)")
    ap.add_argument("--from-json", help="derive from a saved /v1/files response instead of calling the API")
    args = ap.parse_args()

    token = os.environ.get("FIGMA_TOKEN", "").strip()
    if not token and not args.from_json:
        raise SystemExit("FIGMA_TOKEN is not set. export FIGMA_TOKEN=figd_... and re-run.")
    if args.images and not token:
        raise SystemExit("--images needs FIGMA_TOKEN; rendering is an API call.")

    out = Path(args.out) if args.out else Path("docs/design/figma") / args.file_key
    (out / "raw").mkdir(parents=True, exist_ok=True)

    if args.from_json:
        print(f"-> reading {args.from_json} (no API call)")
        data = json.loads(Path(args.from_json).read_text())
    else:
        print(f"-> file {args.file_key}")
        data = _get(f"/v1/files/{urllib.parse.quote(args.file_key)}", token)
        (out / "raw" / "file.json").write_text(json.dumps(data, indent=2, ensure_ascii=False))
    document = data["document"]

    for node_id in args.node_id if not args.from_json else []:
        print(f"-> node {node_id}")
        node_data = _get(
            f"/v1/files/{urllib.parse.quote(args.file_key)}/nodes"
            f"?ids={urllib.parse.quote(node_id)}", token)
        safe = node_id.replace(":", "-")
        (out / "raw" / f"nodes-{safe}.json").write_text(json.dumps(node_data, indent=2, ensure_ascii=False))

    inventory = extract_inventory(document)
    tokens = extract_tokens(document, data.get("styles") or {})
    meta = {
        "fileKey": args.file_key,
        "name": data.get("name"),
        "lastModified": data.get("lastModified"),
        "version": data.get("version"),
        "role": data.get("role"),
        "editorType": data.get("editorType"),
        "linkAccess": data.get("linkAccess"),
        "nodeId": ", ".join(args.node_id),
        "extractedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
    }

    (out / "inventory.json").write_text(json.dumps({"meta": meta, "pages": inventory}, indent=2, ensure_ascii=False))
    (out / "tokens.json").write_text(json.dumps(tokens, indent=2, ensure_ascii=False))
    (out / "content.md").write_text(extract_content(document))
    (out / "CONTEXT.md").write_text(write_context(meta, inventory, tokens, document, out))

    if args.images:
        ids = [c["id"] for page in inventory for c in page["children"]]
        urls: dict = {}
        for i in range(0, len(ids), IMAGE_BATCH):
            batch = ids[i:i + IMAGE_BATCH]
            print(f"-> images {i + 1}-{i + len(batch)} of {len(ids)}")
            got = _get(
                f"/v1/images/{urllib.parse.quote(args.file_key)}"
                f"?ids={urllib.parse.quote(','.join(batch))}&format=png&scale={args.scale}", token)
            urls.update(got.get("images") or {})
        (out / "raw" / "images.json").write_text(json.dumps(urls, indent=2))
        print(f"   {sum(1 for v in urls.values() if v)} of {len(ids)} rendered")
        print("   NOTE: these URLs are short-lived — download them now if you need the PNGs.")

    total = sum(1 for _ in walk(document))
    print(f"\nwrote {out}/ — {total} nodes, {len(inventory)} pages, "
          f"{len(tokens['fills'])} fills, {len(tokens['typography'])} type styles")
    return 0


if __name__ == "__main__":
    sys.exit(main())
