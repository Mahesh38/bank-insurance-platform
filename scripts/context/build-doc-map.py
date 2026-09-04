#!/usr/bin/env python3
"""Generate docs/context/DOC-MAP.yaml — one routed row for every document in docs/.

The capsule index (AGENT-CONTEXT-INDEX.yaml) routes a TASK to the handful of files
that task always needs. That is tier 1, and it is deliberately small. But docs/ holds
~4.3 MB across 444 files, and the long tail — a single field guide, one extracted
schema, one stage's evidence pack — is exactly the granular detail an agent must not
lose. Before this map existed, 96 of those files sat three or more link-hops from the
index and 20 were unreachable altogether: present in the repository, invisible in
practice.

This map closes that gap without spending context. Every tracked document gets one
row carrying where it sits, which capsule owns it, which persona speaks for it, what
question it answers, and what it costs to open. An agent never reads this file whole;
it queries it:

    python3 scripts/context/context-load.py find "premium field mapping"

Routing rules are DATA, declared under `doc_routing:` in AGENT-CONTEXT-INDEX.yaml, so
adding a folder is an index edit and not a code change. Descriptions are extracted
from each document's own title and lead sentence, so they cannot drift into fiction —
a wrong description here is a wrong heading there.

    python3 scripts/context/build-doc-map.py            # write the map
    python3 scripts/context/build-doc-map.py --check    # CI: fail if stale

This tool ROUTES; it never decides. docs/governance/ always wins on conflict.
"""

from __future__ import annotations

import argparse
import fnmatch
import re
import subprocess
import sys
from pathlib import Path

try:
    import yaml
except ImportError as exc:  # pragma: no cover
    print(f"build-doc-map needs PyYAML: {exc}", file=sys.stderr)
    print("  pip install pyyaml", file=sys.stderr)
    raise SystemExit(2)

ROOT = Path(__file__).resolve().parents[2]
INDEX = ROOT / "docs/context/AGENT-CONTEXT-INDEX.yaml"
OUT = ROOT / "docs/context/DOC-MAP.yaml"

# Lines that are document furniture rather than description. A lead sentence must
# tell an agent what the document answers; "**Status:** Draft v1" does not.
FURNITURE = re.compile(
    r"^\*\*(status|document status|layer|owner|version|up|last updated|date|"
    r"applies to|audience|supersedes|related|see also|parent|source|maintainer|"
    r"binding|authority|scope|stage|workstream|reviewed|approved|headings)\b",
    re.I,
)
# Some generated extracts open with `Headings: [...]` rather than prose.
FURNITURE_PLAIN = re.compile(r"^(headings|tags|keywords|api)\s*:", re.I)
ZERO_WIDTH = re.compile(r"[\u200b-\u200f\ufeff]|\\u200b")
BREADCRUMB = re.compile(r"^\*\*Up:\*\*|^\[.*\]\(.*\)\s*(→|>)\s*", re.I)
DESC_MAX = 150


def tracked_docs() -> list[str]:
    out = subprocess.check_output(["git", "ls-files", "docs"], cwd=ROOT, text=True)
    # Unmerged index stages (merge conflicts) list the same path three times.
    # Dedup so a regenerate during a merge cannot write duplicate DOC-MAP rows.
    return sorted({p for p in out.split("\n") if p.strip()})


def humanise(path: Path) -> str:
    stem = re.sub(r"[_-]+", " ", path.stem)
    stem = re.sub(r"\s+v?\d+(\.\d+)*\s*$", "", stem)
    stem = re.sub(r"\s+[0-9a-f]{4}$", "", stem)  # content-hash suffixes on uploads
    return re.sub(r"\s+", " ", stem).strip()


def describe_data(path: Path) -> tuple[str, str]:
    """Non-prose artefacts still need a findable name and an honest one-liner.

    Nothing here is invented: YAML and CSV are described by their own leading comment
    or header row, and binary artefacts are described by what they are."""
    suffix = path.suffix.lower()
    title = humanise(path)
    try:
        if suffix in {".yaml", ".yml"}:
            for line in path.read_text(encoding="utf-8", errors="ignore").splitlines():
                stripped = line.strip()
                if stripped.startswith("#") and len(stripped) > 3:
                    return title, stripped.lstrip("# ").strip()[:DESC_MAX]
                if stripped and not stripped.startswith("#"):
                    return title, f"YAML record; first key `{stripped.split(':')[0]}`"
        if suffix == ".csv":
            header = path.read_text(encoding="utf-8", errors="ignore").splitlines()[:1]
            if header:
                return title, "CSV columns: " + header[0].strip()[:DESC_MAX]
        if suffix == ".json":
            for line in path.read_text(encoding="utf-8", errors="ignore").splitlines():
                if '"description"' in line or '"title"' in line:
                    return title, line.strip().rstrip(",")[:DESC_MAX]
    except (OSError, IndexError):
        pass
    labels = {
        ".pdf": "PDF source artefact — intake evidence; cite the derived document, not this file",
        ".svg": "Rendered diagram — read the companion .md for the binding text",
        ".png": "Image artefact",
        ".jpg": "Image artefact",
    }
    return title, labels.get(suffix, f"{suffix.lstrip('.').upper() or 'Binary'} artefact")


def describe(path: Path) -> tuple[str, str]:
    """(title, answers) taken from the document itself — never invented here."""
    if path.suffix.lower() not in {".md", ".markdown"}:
        return describe_data(path)
    try:
        text = path.read_text(encoding="utf-8", errors="ignore")
    except OSError:
        return "", ""

    title = ""
    lines = text.splitlines()
    start = 0
    for i, line in enumerate(lines):
        match = re.match(r"#\s+(.*)", line)
        if match:
            title = ZERO_WIDTH.sub("", match.group(1))
            title = re.sub(r"\s+", " ", title).strip().strip("#").strip()
            start = i + 1
            break

    answers = ""
    in_fence = False
    for line in lines[start:]:
        stripped = line.strip()
        if stripped.startswith("```"):
            in_fence = not in_fence
            continue
        if in_fence or not stripped:
            continue
        stripped = re.sub(r"^>\s*", "", stripped).strip()
        if not stripped:
            continue
        if stripped.startswith(("#", "|", "---", "===", "<!--", ":", "<")):
            continue
        if re.match(r"^[-*+]\s|^\d+\.\s", stripped):
            continue
        if FURNITURE.match(stripped) or FURNITURE_PLAIN.match(stripped) or BREADCRUMB.match(stripped):
            continue
        answers = stripped
        break

    # No lead prose — common for generated extracts. The first section heading is a
    # true statement about the document, so use it rather than leaving the row blank.
    if not answers:
        for line in lines[start:]:
            match = re.match(r"##\s+(.*)", line)
            if match:
                answers = "Sections: " + re.sub(r"\s+", " ", match.group(1)).strip()
                break

    # Records whose whole body is a fenced YAML block (plans, verdicts, ADR records)
    # carry their summary in a named key rather than in prose.
    if not answers:
        for i, line in enumerate(lines[start:]):
            match = re.match(r"(objective|summary|intent|goal|description|problem):\s*(.*)", line)
            if not match:
                continue
            value = match.group(2).strip()
            if value in {">", "|", ">-", "|-", ""}:
                folded = []
                for nxt in lines[start + i + 1:]:
                    if not nxt.startswith((" ", "\t")) or not nxt.strip():
                        break
                    folded.append(nxt.strip())
                value = " ".join(folded)
            if value:
                answers = f"{match.group(1)}: {value}"
                break

    # Strip markdown so the row stays greppable as plain words.
    answers = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", answers)
    answers = re.sub(r"[*`_]", "", answers)
    answers = ZERO_WIDTH.sub("", answers)
    answers = re.sub(r"\s+", " ", answers).strip()
    if len(answers) > DESC_MAX:
        cut = answers[:DESC_MAX]
        answers = cut[: cut.rfind(" ")] if " " in cut else cut
        answers += "…"
    return title, answers


def route(path: str, rules: list[dict]) -> dict | None:
    """First matching rule wins — rules are ordered most-specific first."""
    for rule in rules:
        for pattern in rule.get("match", []):
            if fnmatch.fnmatch(path, pattern):
                return rule
    return None


def build() -> tuple[str, list[str]]:
    index = yaml.safe_load(INDEX.read_text(encoding="utf-8"))
    rules = index.get("doc_routing", {}).get("rules", [])
    if not rules:
        raise SystemExit("AGENT-CONTEXT-INDEX.yaml has no doc_routing.rules — cannot build the map")

    rows, unrouted = [], []
    for rel in tracked_docs():
        rule = route(rel, rules)
        if rule is None:
            unrouted.append(rel)
            continue
        abs_path = ROOT / rel
        title, answers = describe(abs_path)
        rows.append(
            {
                "path": rel,
                "bucket": rule["bucket"],
                "authority": rule["authority"],
                "capsules": list(rule.get("capsules", [])),
                "persona": rule.get("persona"),
                "title": title,
                "answers": answers,
                "bytes": abs_path.stat().st_size if abs_path.exists() else 0,
            }
        )

    header = f"""# =============================================================================
# DOC-MAP — every document in docs/, routed. GENERATED — do not hand-edit.
#
#   Regenerate:  python3 scripts/context/build-doc-map.py
#   Query:       python3 scripts/context/context-load.py find "<what you need>"
#
# Why this exists: the capsule index routes a TASK to its always-load set. This map
# routes a DOCUMENT, so the long tail of field guides, schemas and evidence packs is
# one hop from the index instead of three. Nothing here overrides docs/governance/.
#
# `authority` is what the document can settle, not how good it is. `capsules` name
# the task capsules that legitimately open it. `answers` is lifted from the document's
# own lead sentence — if it reads wrong, fix the document, then regenerate.
#
# Routing rules live in AGENT-CONTEXT-INDEX.yaml under `doc_routing:`.
# =============================================================================

schema_version: "1.0"
generated_by: "scripts/context/build-doc-map.py"
source_of_rules: "docs/context/AGENT-CONTEXT-INDEX.yaml#doc_routing"
document_count: {len(rows)}
total_bytes: {sum(r['bytes'] for r in rows)}

documents:
"""
    # Emitted by hand rather than by safe_dump: the dumper wraps long scalars across
    # lines, and a wrapped `answers:` is no longer greppable. Every row here stays on
    # one line per field so `grep` and `find` see whole sentences.
    def scalar(value: str) -> str:
        return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'

    lines = []
    for row in rows:
        lines.append(f"  - path: {row['path']}")
        lines.append(f"    bucket: {row['bucket']}")
        lines.append(f"    authority: {row['authority']}")
        lines.append(f"    capsules: [{', '.join(row['capsules'])}]")
        lines.append(f"    persona: {row['persona'] or 'null'}")
        lines.append(f"    title: {scalar(row['title'])}")
        lines.append(f"    answers: {scalar(row['answers'])}")
        lines.append(f"    bytes: {row['bytes']}")
    return header + "\n".join(lines) + "\n", unrouted


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true", help="fail if the committed map is stale")
    args = ap.parse_args()

    content, unrouted = build()
    if unrouted:
        print(f"{len(unrouted)} document(s) match no doc_routing rule:", file=sys.stderr)
        for path in unrouted[:20]:
            print(f"  {path}", file=sys.stderr)
        print("Add a rule to AGENT-CONTEXT-INDEX.yaml#doc_routing.", file=sys.stderr)
        return 1

    if args.check:
        current = OUT.read_text(encoding="utf-8") if OUT.exists() else ""
        if current != content:
            print("docs/context/DOC-MAP.yaml is stale — run: python3 scripts/context/build-doc-map.py",
                  file=sys.stderr)
            return 1
        print(f"DOC-MAP current — {content.count('- path:')} documents routed")
        return 0

    OUT.write_text(content, encoding="utf-8")
    print(f"wrote {OUT.relative_to(ROOT)} — {content.count('- path:')} documents routed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
