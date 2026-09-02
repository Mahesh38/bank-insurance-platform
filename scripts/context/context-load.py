#!/usr/bin/env python3
"""Resolve a task to its context capsule — the exact, ordered, budgeted read list.

docs/ holds ~4.3 MB across 427 files. Exploring it costs more context than the work
does. This resolves a task to a named capsule in docs/context/AGENT-CONTEXT-INDEX.yaml
and prints only what to read.

  python3 scripts/context/context-load.py list
  python3 scripts/context/context-load.py show architecture-review
  python3 scripts/context/context-load.py resolve "is the payment callback secure"
  python3 scripts/context/context-load.py find "premium field mapping"
  python3 scripts/context/context-load.py validate     # CI: paths, anchors, budgets

Needs PyYAML only. This tool ROUTES; it never decides. docs/governance/ always wins.
"""

from __future__ import annotations

import argparse
import re
import signal
import sys
from pathlib import Path

# `context-load.py list | head` is a normal thing to do; do not traceback on it.
if hasattr(signal, "SIGPIPE"):
    signal.signal(signal.SIGPIPE, signal.SIG_DFL)

try:
    import yaml
except ImportError as exc:  # pragma: no cover
    print(f"context-load needs PyYAML: {exc}", file=sys.stderr)
    print("  pip install pyyaml", file=sys.stderr)
    raise SystemExit(2)

ROOT = Path(__file__).resolve().parents[2]
INDEX = ROOT / "docs/context/AGENT-CONTEXT-INDEX.yaml"
MANIFEST = ROOT / "docs/context/context-manifest.yaml"
DOC_MAP = ROOT / "docs/context/DOC-MAP.yaml"
REL = "scripts/context/context-load.py"

# Kept byte-identical in behaviour to ci-checks.py::slug so an anchor that passes
# the link check also passes here.
def slug(text: str) -> str:
    text = re.sub(r"`", "", text.strip().lower())
    text = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", text)
    text = re.sub(r"[^\w\s-]", "", text)
    return text.replace(" ", "-")


def section_bytes(path: Path, anchor: str) -> int | None:
    """Byte size of one heading section, or None when the anchor does not exist."""
    lines = path.read_text(encoding="utf-8", errors="ignore").splitlines(keepends=True)
    start = level = None
    for i, line in enumerate(lines):
        match = re.match(r"(#{1,6})\s+(.*)", line)
        if not match:
            continue
        if start is None:
            if slug(match.group(2)) == anchor:
                start, level = i, len(match.group(1))
            continue
        if len(match.group(1)) <= level:
            return sum(len(x.encode()) for x in lines[start:i])
    if start is None:
        return None
    return sum(len(x.encode()) for x in lines[start:])


def entry_bytes(entry: dict) -> tuple[int, str | None]:
    """(bytes, error) for one load entry. Directories are summed one level deep."""
    raw = entry.get("path")
    if not raw:
        return 0, None
    target = ROOT / raw
    if not target.exists():
        return 0, f"missing path: {raw}"
    if target.is_dir():
        return sum(f.stat().st_size for f in target.glob("*") if f.is_file()), None
    anchor = entry.get("anchor")
    if not anchor:
        return target.stat().st_size, None
    size = section_bytes(target, anchor)
    if size is None:
        return target.stat().st_size, f"no such heading: {raw}#{anchor}"
    return size, None


def load_index() -> dict:
    return yaml.safe_load(INDEX.read_text(encoding="utf-8"))


def capsule_cost(capsule: dict) -> int:
    return sum(entry_bytes(e)[0] for e in capsule.get("load") or [])


def fmt(entry: dict) -> str:
    path = entry.get("path")
    if not path:
        return f"capsule:{entry['capsule']}"
    return f"{path}#{entry['anchor']}" if entry.get("anchor") else path


def cmd_list(index: dict) -> int:
    tier0 = index["tier_0"]
    t0 = sum(entry_bytes(e)[0] for e in tier0["load"])
    print(f"TIER 0 (always) — {t0:,} B")
    for entry in tier0["load"]:
        print(f"  {fmt(entry)}")
    print(f"\n{len(index['capsules'])} capsules — read tier 0 + ONE capsule, rarely two (rule CTX-9)\n")
    print(f"  {'capsule':<24} {'cost':>9} {'budget':>9}  intent")
    for capsule in index["capsules"]:
        cost = capsule_cost(capsule)
        print(f"  {capsule['id']:<24} {cost:>8,}B {capsule['budget_bytes']:>8,}B  {capsule['intent'][:64]}")
    print(f"\n  unmatched input -> `{index['fallback']['capsule']}`")
    return 0


def cmd_show(index: dict, capsule_id: str) -> int:
    capsule = next((c for c in index["capsules"] if c["id"] == capsule_id), None)
    if capsule is None:
        print(f"no such capsule: {capsule_id}", file=sys.stderr)
        print("  known: " + ", ".join(c["id"] for c in index["capsules"]), file=sys.stderr)
        return 1

    cost = capsule_cost(capsule)
    print(f"CAPSULE  {capsule['id']}")
    print(f"INTENT   {capsule['intent']}")
    if capsule.get("persona"):
        persona = next((p for p in index["personas"] if p["id"] == capsule["persona"]), None)
        if persona:
            print(f"PERSONA  {capsule['persona']} -> {persona['card']}")
            print(f"         package {persona['package']} — open ONLY on a named condition (rule CTX-2)")
    print(f"COST     {cost:,} B of a {capsule['budget_bytes']:,} B budget")

    print("\nREAD, IN ORDER — always:")
    for entry in capsule.get("load") or []:
        size, error = entry_bytes(entry)
        flag = f"  !! {error}" if error else ""
        print(f"  [{size:>7,}B] {fmt(entry)}{flag}")
        print(f"             {entry.get('why', '')}")

    conditional = capsule.get("then_only_if") or []
    if conditional:
        print("\nREAD ONLY IF THE CONDITION HOLDS:")
        for entry in conditional:
            size, error = entry_bytes(entry) if entry.get("path") else (0, None)
            tag = f"[{size:>7,}B] " if entry.get("path") else "[ capsule ] "
            flag = f"  !! {error}" if error else ""
            print(f"  {tag}{fmt(entry)}{flag}")
            print(f"             if: {entry['condition']}")

    print(f"\nOUTPUT   {capsule.get('output', '')}")
    for rule in capsule.get("never") or []:
        print(f"NEVER    {rule}")
    print("\nTIER 0 first if you have not read it: " +
          ", ".join(e["path"] for e in index["tier_0"]["load"]))
    return 0


def cmd_resolve(index: dict, text: str) -> int:
    lowered = text.lower()
    scored = []
    for capsule in index["capsules"]:
        hits = [t for t in capsule.get("triggers") or [] if t.lower() in lowered]
        if hits:
            # Total matched length is the evidence; longest single trigger breaks ties.
            # "should we add pods when the quote api is slow" is a scaling question that
            # happens to contain a governance phrase, not the other way round.
            scored.append((sum(len(h) for h in hits), max(len(h) for h in hits), capsule, hits))
    if not scored:
        fallback = index["fallback"]
        print(f"no trigger matched -> fallback capsule `{fallback['capsule']}`")
        print(f"  {' '.join(fallback['note'].split())}\n")
        return cmd_show(index, fallback["capsule"])

    scored.sort(key=lambda s: (s[0], s[1]), reverse=True)
    best = scored[0]
    print(f"matched on: {', '.join(repr(h) for h in best[3])}")
    if len(scored) > 1:
        others = ", ".join(f"{s[2]['id']} ({', '.join(s[3])})" for s in scored[1:4])
        print(f"also plausible: {others}")
    if best[2]["id"] != "triage":
        print("note: an input that PROPOSES a change is triaged before it is implemented "
              "(BOOT.md section 1) — `context-load.py show triage`.")
    print()
    return cmd_show(index, best[2]["id"])


# --- find: the long tail, without exploring it --------------------------------
# `resolve` answers "what does this KIND of task always need". `find` answers the
# other question — "which single document holds this specific fact". Before DOC-MAP
# existed the only way to answer it was to walk README hubs or grep 4.3 MB, and both
# spend the context window that the capsule system exists to protect.

STOPWORDS = {
    "the", "a", "an", "of", "for", "to", "in", "on", "is", "are", "and", "or", "what",
    "which", "how", "do", "does", "we", "i", "it", "this", "that", "with", "be", "our",
    "where", "when", "why", "should", "can", "was", "were", "as", "at", "by", "from",
}


def load_doc_map() -> dict:
    if not DOC_MAP.exists():
        raise SystemExit(
            "docs/context/DOC-MAP.yaml is missing — run: python3 scripts/context/build-doc-map.py"
        )
    return yaml.safe_load(DOC_MAP.read_text(encoding="utf-8"))


def terms_of(text: str) -> list[str]:
    words = re.findall(r"[a-z0-9][a-z0-9\-]{1,}", text.lower())
    return [w for w in words if w not in STOPWORDS] or words


def cmd_find(index: dict, text: str, limit: int, capsule_filter: str | None) -> int:
    doc_map = load_doc_map()
    terms = terms_of(text)
    if not terms:
        print("nothing to search for", file=sys.stderr)
        return 2

    scored = []
    for row in doc_map["documents"]:
        if capsule_filter and capsule_filter not in (row.get("capsules") or []):
            continue
        path = row["path"]
        meta = f"{path} {row.get('title') or ''} {row.get('answers') or ''}".lower()
        # Metadata is the strong signal: a term in the path or title means the document
        # is ABOUT the thing. Body matches only break ties between metadata hits.
        score = 0
        matched = set()
        for term in terms:
            if term in meta:
                score += 10
                matched.add(term)
            if term in path.lower():
                score += 6
        if not score:
            continue
        scored.append((score, len(matched), row))

    # Body scan, restricted to plausible candidates so a query never reads the corpus.
    if len(scored) < limit:
        for row in doc_map["documents"]:
            if capsule_filter and capsule_filter not in (row.get("capsules") or []):
                continue
            if any(r["path"] == row["path"] for _, _, r in scored):
                continue
            file_path = ROOT / row["path"]
            if file_path.suffix.lower() not in {".md", ".yaml", ".yml", ".json"}:
                continue
            if not file_path.exists() or file_path.stat().st_size > 400_000:
                continue
            body = file_path.read_text(encoding="utf-8", errors="ignore").lower()
            matched = {t for t in terms if t in body}
            if len(matched) == len(terms) and matched:
                scored.append((len(matched), len(matched), row))

    if not scored:
        print(f"no document matched {text!r}.")
        print("Widen the terms, or resolve the task to a capsule instead:")
        print(f'  python3 {REL} resolve "{text}"')
        return 1

    scored.sort(key=lambda s: (s[0], s[1], -s[2]["bytes"]), reverse=True)
    shown = scored[:limit]
    print(f"{len(scored)} document(s) matched {text!r} — showing {len(shown)}\n")
    for score, _, row in shown:
        capsules = ", ".join(row.get("capsules") or []) or "-"
        print(f"  {row['path']}  ({row['bytes']:,}B)")
        if row.get("title"):
            print(f"      {row['title']}")
        if row.get("answers"):
            print(f"      {row['answers']}")
        persona = row.get("persona") or "none"
        print(f"      authority: {row['authority']} · capsule: {capsules} · persona: {persona}")
        print()
    print("Read the capsule first (rule CTX-1), then open only the row you need.")
    print("Cite what you read by path and anchor (rule CTX-6).")
    return 0


def cmd_validate(index: dict) -> int:
    problems: list[str] = []

    for entry in index["tier_0"]["load"]:
        _, error = entry_bytes(entry)
        if error:
            problems.append(f"tier_0: {error}")
    t0 = sum(entry_bytes(e)[0] for e in index["tier_0"]["load"])
    if t0 > index["tier_0"]["budget_bytes"]:
        problems.append(f"tier_0 costs {t0:,}B over its {index['tier_0']['budget_bytes']:,}B budget")

    for value in index.get("precedence") or []:
        if not (ROOT / value).exists():
            problems.append(f"precedence: missing {value}")

    manifest = yaml.safe_load(MANIFEST.read_text(encoding="utf-8"))
    manifest_roles = {r["id"] for r in manifest.get("roles") or []}
    persona_ids = set()
    for persona in index["personas"]:
        pid = persona["id"]
        if pid in persona_ids:
            problems.append(f"duplicate persona id: {pid}")
        persona_ids.add(pid)
        if persona["manifest_role"] not in manifest_roles:
            problems.append(
                f"persona {pid}: manifest_role {persona['manifest_role']!r} "
                "is not a role in context-manifest.yaml"
            )
        for key in ("card", "package"):
            if not (ROOT / persona[key]).exists():
                problems.append(f"persona {pid}: missing {key} {persona[key]}")
    unrouted = sorted(manifest_roles - {p["manifest_role"] for p in index["personas"]})
    if unrouted:
        problems.append(f"manifest roles with no persona card: {unrouted}")

    capsule_ids = {c["id"] for c in index["capsules"]}
    if index["fallback"]["capsule"] not in capsule_ids:
        problems.append(f"fallback names unknown capsule {index['fallback']['capsule']!r}")

    seen_ids: set[str] = set()
    triggers: dict[str, str] = {}
    for capsule in index["capsules"]:
        cid = capsule["id"]
        if cid in seen_ids:
            problems.append(f"duplicate capsule id: {cid}")
        seen_ids.add(cid)
        if capsule.get("persona") and capsule["persona"] not in persona_ids:
            problems.append(f"{cid}: unknown persona {capsule['persona']!r}")
        for trigger in capsule.get("triggers") or []:
            owner = triggers.get(trigger)
            if owner and owner != cid:
                problems.append(f"trigger {trigger!r} is claimed by both {owner} and {cid}")
            triggers[trigger] = cid
        for entry in (capsule.get("load") or []) + (capsule.get("then_only_if") or []):
            if entry.get("capsule"):
                if entry["capsule"] not in capsule_ids:
                    problems.append(f"{cid}: unknown capsule reference {entry['capsule']!r}")
                continue
            _, error = entry_bytes(entry)
            if error:
                problems.append(f"{cid}: {error}")
        for entry in capsule.get("load") or []:
            if not entry.get("why"):
                problems.append(f"{cid}: load entry {fmt(entry)} has no `why`")
        for entry in capsule.get("then_only_if") or []:
            if not entry.get("condition"):
                problems.append(f"{cid}: then_only_if entry {fmt(entry)} has no `condition`")
        cost = capsule_cost(capsule)
        if cost > capsule["budget_bytes"]:
            problems.append(
                f"{cid}: always-load costs {cost:,}B, over its {capsule['budget_bytes']:,}B budget "
                "— split the capsule or anchor the reads"
            )

    if problems:
        print("CONTEXT INDEX INVALID")
        for problem in problems:
            print(f"  - {problem}")
        return 1
    total = sum(capsule_cost(c) for c in index["capsules"])
    print(
        f"CONTEXT INDEX VALID — {len(seen_ids)} capsules, {len(persona_ids)} personas, "
        f"{len(triggers)} triggers; tier 0 {t0:,}B; "
        f"mean capsule {total // len(seen_ids):,}B"
    )
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("list", help="every capsule with its real cost and budget")
    show = sub.add_parser("show", help="the exact read list for one capsule")
    show.add_argument("capsule")
    resolve = sub.add_parser("resolve", help="match free text to a capsule")
    resolve.add_argument("text", nargs="+")
    find = sub.add_parser("find", help="which document holds this specific fact")
    find.add_argument("text", nargs="+")
    find.add_argument("--limit", type=int, default=6, help="rows to show (default 6)")
    find.add_argument("--capsule", help="restrict to documents this capsule may open")
    sub.add_parser("validate", help="paths, anchors, budgets and manifest agreement")
    args = parser.parse_args()

    index = load_index()
    if args.command == "list":
        return cmd_list(index)
    if args.command == "show":
        return cmd_show(index, args.capsule)
    if args.command == "resolve":
        return cmd_resolve(index, " ".join(args.text))
    if args.command == "find":
        return cmd_find(index, " ".join(args.text), args.limit, args.capsule)
    return cmd_validate(index)


if __name__ == "__main__":
    raise SystemExit(main())
