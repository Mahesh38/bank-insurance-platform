#!/usr/bin/env python3
"""
AIGEM CI checks — the governance artefacts must enforce what the documents claim.

CI-only. Agents are NOT required to run this: their mandatory check is
`java scripts/governance/FreshnessCheck.java`, which needs nothing beyond the
documented JDK 21 + Git baseline. This script needs PyYAML and jsonschema and
therefore runs in CI, not on a developer's or an agent's critical path.

Checks:
  1. every JSON Schema is a valid draft 2020-12 schema
  2. state/CURRENT-STATE.yaml validates against current-state.schema.json
  3. every fenced yaml block tagged `# schema: <name>` validates against that schema
     (this is what stops the templates drifting away from the schemas again)
  4. the reviewVerdict definition inlined in implementation-plan.schema.json is
     identical to review-verdict.schema.json
  5. routing in CURRENT-STATE.yaml is closed over the canonical work-type enum
  6. every internal markdown link and heading anchor resolves
  7. the priority formula and the action matrix agree to within one band in every
     cell (Rule PRI-4), given the PRI-8 blocking floors

Usage:  python3 scripts/governance/ci-checks.py [--quiet]
Exit:   0 all checks pass · 1 one or more failures
"""

from __future__ import annotations

import argparse
import glob
import json
import os
import re
import sys

try:
    import yaml
    import jsonschema
except ImportError as exc:  # pragma: no cover
    print(f"ci-checks needs PyYAML and jsonschema: {exc}")
    print("  pip install pyyaml jsonschema")
    sys.exit(1)

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
GOV = os.path.join(ROOT, "docs", "governance")
SCHEMAS = os.path.join(GOV, "schemas")

failures: list[str] = []
checked = 0


def fail(msg: str) -> None:
    failures.append(msg)
    print(f"  FAIL  {msg}")


def ok(msg: str, quiet: bool) -> None:
    global checked
    checked += 1
    if not quiet:
        print(f"  ok    {msg}")


def load_schema(name: str) -> dict:
    with open(os.path.join(SCHEMAS, f"{name}.schema.json"), encoding="utf-8") as fh:
        return json.load(fh)


def rel(path: str) -> str:
    return os.path.relpath(path, ROOT)


# --- 1. schemas are well-formed -------------------------------------------------
def check_schemas(quiet: bool) -> None:
    print("\n[1] JSON Schemas are valid draft 2020-12")
    for path in sorted(glob.glob(os.path.join(SCHEMAS, "*.schema.json"))):
        try:
            schema = json.load(open(path, encoding="utf-8"))
            jsonschema.Draft202012Validator.check_schema(schema)
            ok(rel(path), quiet)
        except Exception as exc:
            fail(f"{rel(path)}: {exc}")


# --- 2. the state file validates ------------------------------------------------
def check_state(quiet: bool) -> None:
    print("\n[2] CURRENT-STATE.yaml validates")
    path = os.path.join(GOV, "state", "CURRENT-STATE.yaml")
    try:
        doc = yaml.safe_load(open(path, encoding="utf-8"))
    except Exception as exc:
        fail(f"{rel(path)} is not parseable: {exc}")
        return
    errs = sorted(
        jsonschema.Draft202012Validator(load_schema("current-state")).iter_errors(doc),
        key=lambda e: list(e.path),
    )
    if errs:
        for e in errs[:10]:
            fail(f"{rel(path)} {list(e.path)}: {e.message}")
    else:
        ok(rel(path), quiet)


# --- 3. tagged yaml blocks in markdown validate ---------------------------------
BLOCK = re.compile(r"```yaml\n(#\s*schema:\s*([a-z-]+)\n.*?)```", re.S)


def check_tagged_blocks(quiet: bool) -> None:
    print("\n[3] Tagged `# schema:` blocks validate against their schema")
    found = 0
    for md in sorted(glob.glob(os.path.join(GOV, "**", "*.md"), recursive=True)):
        text = open(md, encoding="utf-8").read()
        # A ````markdown fence contains an ILLUSTRATION of the contract, not a record.
        # Blank it out so documenting the marker does not trip the check that enforces it.
        text = re.sub(r"````.*?````", "", text, flags=re.S)
        for i, match in enumerate(BLOCK.finditer(text)):
            found += 1
            body, name = match.group(1), match.group(2)
            label = f"{rel(md)} block {i + 1} (# schema: {name})"
            try:
                schema = load_schema(name)
            except FileNotFoundError:
                fail(f"{label}: no such schema")
                continue
            try:
                doc = yaml.safe_load(body)
            except Exception as exc:
                fail(f"{label}: YAML does not parse: {exc}")
                continue
            errs = sorted(
                jsonschema.Draft202012Validator(schema).iter_errors(doc),
                key=lambda e: list(e.path),
            )
            if errs:
                for e in errs[:4]:
                    fail(f"{label} {list(e.path)}: {e.message[:140]}")
            else:
                ok(label, quiet)
    if found == 0:
        fail("no tagged yaml blocks found — the extractor contract is broken")


# --- 4. the inlined verdict definition has not drifted --------------------------
def check_inline_drift(quiet: bool) -> None:
    print("\n[4] Inlined reviewVerdict matches review-verdict.schema.json")
    standalone = load_schema("review-verdict")
    inline = load_schema("implementation-plan")["$defs"]["reviewVerdict"]
    strip = lambda d: {k: v for k, v in d.items()
                       if k not in ("$schema", "$id", "title", "description", "$comment")}
    if strip(standalone) != strip(inline):
        a, b = strip(standalone), strip(inline)
        diff = [k for k in set(a) | set(b) if a.get(k) != b.get(k)]
        fail(f"implementation-plan $defs.reviewVerdict has drifted; differing keys: {diff}")
    else:
        ok("definitions are identical", quiet)


# --- 5. routing is closed over the work-type enum -------------------------------
def check_routing(quiet: bool) -> None:
    print("\n[5] Routing is closed over the canonical work-type enum")
    state = yaml.safe_load(open(os.path.join(GOV, "state", "CURRENT-STATE.yaml"), encoding="utf-8"))
    types = set(load_schema("work-item")["properties"]["type"]["enum"])
    routing = state.get("routing") or {}
    unknown = sorted(set(routing) - types)
    unrouted = sorted(types - set(routing))
    if unknown:
        fail(f"routing keys that are not work types: {unknown}")
    if unrouted:
        fail(f"work types with no route: {unrouted}")
    for key, dest in routing.items():
        if not isinstance(dest, list) or not dest:
            fail(f"routing[{key}] must be a non-empty list of destinations")
    if not unknown and not unrouted:
        ok(f"all {len(types)} work types routed", quiet)


# --- 6. links and anchors -------------------------------------------------------
def slug(text: str) -> str:
    text = re.sub(r"`", "", text.strip().lower())
    text = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", text)
    text = re.sub(r"[^\w\s-]", "", text)
    return text.replace(" ", "-")


def anchors_of(path: str) -> set[str]:
    out = set()
    for line in open(path, encoding="utf-8"):
        m = re.match(r"(#{1,6})\s+(.*)", line)
        if m:
            out.add(slug(m.group(2)))
    return out


def check_links(quiet: bool) -> None:
    print("\n[6] Internal links and heading anchors resolve")
    files = sorted(glob.glob(os.path.join(GOV, "**", "*.md"), recursive=True))
    files += [os.path.join(ROOT, f) for f in ("AGENTS.md", "README.md")]
    bad = 0
    for path in files:
        if not os.path.exists(path):
            continue
        base = os.path.dirname(path)
        text = re.sub(r"<!--.*?-->", "", open(path, encoding="utf-8").read(), flags=re.S)
        for m in re.finditer(r"\[[^\]]*\]\(([^)#\s]*)(#[^)\s]*)?\)", text):
            link, anchor = m.group(1), (m.group(2) or "")[1:]
            if link.startswith(("http", "mailto")):
                continue
            target = os.path.normpath(os.path.join(base, link)) if link else path
            if not os.path.exists(target):
                fail(f"{rel(path)} -> {link} (missing)")
                bad += 1
                continue
            if anchor and target.endswith(".md") and anchor not in anchors_of(target):
                fail(f"{rel(path)} -> {link}#{anchor} (no such heading)")
                bad += 1
    if bad == 0:
        ok(f"{len(files)} files, all links and anchors resolve", quiet)


# --- 7. the priority model is self-consistent ------------------------------------
def check_priority_calibration(quiet: bool) -> None:
    """Rule PRI-4 says the score may differ from the matrix default by at most one band.
    Before Rule PRI-8's blocking floors existed, SF1+MUST scored two bands below its
    matrix default and the check fired on correctly classified work. This asserts the
    two routes still agree, so a change to the weights cannot silently break it."""
    print("\n[7] Priority formula agrees with the action matrix (PRI-4 / PRI-8)")
    band = lambda s: "P1" if s >= 24 else "P2" if s >= 17 else "P3" if s >= 11 else "P4" if s >= 5 else "P5"
    order = {"P1": 1, "P2": 2, "P3": 3, "P4": 4, "P5": 5}
    N = {"MUST": 4, "SHOULD": 2, "COULD": 1, "NOT-NOW": 0}
    S = {"SF0": 4, "SF1": 3, "SF2": 1, "SF3": 0}
    # PRI-8 floors: an SF0 item blocks by definition; an on-stage MUST blocks its deliverable
    floor = lambda sf, n: 2 if sf == "SF0" else (1 if sf == "SF1" and n == "MUST" else 0)
    # 00 section 6, taking the lower-urgency end where the matrix gives a range
    matrix = {
        ("SF0", "MUST"): "P1", ("SF0", "SHOULD"): "P2", ("SF0", "COULD"): "P3",
        ("SF1", "MUST"): "P2", ("SF1", "SHOULD"): "P3", ("SF1", "COULD"): "P3", ("SF1", "NOT-NOW"): "P4",
        ("SF2", "MUST"): "P3", ("SF2", "SHOULD"): "P4", ("SF2", "COULD"): "P5", ("SF2", "NOT-NOW"): "P4",
        ("SF3", "MUST"): "P4", ("SF3", "SHOULD"): "P4", ("SF3", "COULD"): "P5", ("SF3", "NOT-NOW"): "P5",
    }
    worst = 0
    for (sf, n), default in sorted(matrix.items()):
        b = floor(sf, n)
        for risk in (0, 1):
            for effort in (0, 1, 2):
                score = 2 * N[n] + 2 * S[sf] + 2 * b + 2 * risk - effort
                got = band(score)
                # caps PRI-2 / PRI-3
                if sf == "SF3":
                    got = max(got, "P4" if n in ("MUST", "SHOULD") else "P5", key=lambda x: order[x])
                if sf == "SF2":
                    got = max(got, "P3", key=lambda x: order[x])
                gap = abs(order[got] - order[default])
                worst = max(worst, gap)
                if gap > 1:
                    fail(f"{sf}+{n}: matrix {default}, formula {got} (R={risk}, E={effort}) — "
                         f"PRI-4 would fire on a correct classification")
    if worst <= 1:
        ok(f"all {len(matrix)} matrix cells within one band (worst gap {worst})", quiet)


# --- 8. gate criteria agree between the state file and 04-STAGE_GATES ------------
#
# The exit criteria are written out in full in two places an agent reads as authority:
# state/CURRENT-STATE.yaml (machine-readable, pipeline step 1) and 04-STAGE_GATES.md
# (the human-readable gate table). Nothing previously compared them, so editing the
# criterion text or its state in one file and forgetting the other left CI green while
# agents read the stale copy — the exact drift 17-DRIFT_CONTROL is about, unguarded in
# the framework's own files.
# Criterion ids are "4.1" for WS-1 and "A.1" for WS-2 — both are compared.
GATE_TABLE_ROW = re.compile(r"^\|\s*([A-Za-z0-9]+\.[A-Za-z0-9]+)\s*\|\s*(.+?)\s*\|(.+)\|\s*$", re.M)

# 04's status cells are prose ("Met — TermJourneyE2EIT ..."), so compare the state word
# rather than the sentence. These are the words that map onto the YAML enum.
STATE_WORDS = {
    "MET": "MET",
    "PARTIAL": "PARTIAL",
    "OPEN": "OPEN",
    "WAIVED": "WAIVED",
}


def _declared_state(status_cell: str) -> str | None:
    """First recognised state word in a 04 status cell, ignoring markdown emphasis."""
    for word in re.findall(r"[A-Za-z]+", status_cell.upper()):
        if word in STATE_WORDS:
            return STATE_WORDS[word]
    return None


def check_gate_criteria_agree(quiet: bool) -> None:
    print("\n[8] Gate criteria agree between CURRENT-STATE.yaml and 04-STAGE_GATES.md")

    state_path = os.path.join(GOV, "state", "CURRENT-STATE.yaml")
    gates_path = os.path.join(GOV, "04-STAGE_GATES.md")
    state = yaml.safe_load(open(state_path, encoding="utf-8"))
    gates_md = open(gates_path, encoding="utf-8").read()

    table_rows = {}
    for cid, criterion, rest in GATE_TABLE_ROW.findall(gates_md):
        # rest is "evidence | status"; the status is the final cell.
        cells = [c.strip() for c in rest.split("|")]
        table_rows[cid] = (criterion, cells[-1] if cells else "")

    compared = 0
    for ws in state.get("workstreams", []):
        gate = (ws.get("current_gate") or {})
        for entry in gate.get("exit_criteria", []) or []:
            cid = str(entry.get("id", "")).strip()
            if cid not in table_rows:
                fail(f"{rel(state_path)}: criterion {cid} has no row in {rel(gates_path)}")
                continue
            compared += 1
            _, status_cell = table_rows[cid]
            declared = _declared_state(status_cell)
            if declared is None:
                fail(f"{rel(gates_path)}: criterion {cid} status cell names no state "
                     f"(expected one of {sorted(STATE_WORDS)}): {status_cell[:60]!r}")
            elif declared != entry.get("state"):
                fail(f"criterion {cid}: state file says {entry.get('state')}, "
                     f"04-STAGE_GATES.md says {declared} — the two authorities disagree")

    # The reverse direction: a criterion in the gate table that the state file never lists
    # would be invisible to every agent, since the pipeline reads the YAML.
    state_ids = {
        str(e.get("id", "")).strip()
        for ws in state.get("workstreams", [])
        for e in (ws.get("current_gate") or {}).get("exit_criteria", []) or []
    }
    current_phase_ids = {cid for cid in table_rows if cid.split(".")[0] in
                         {sid.split(".")[0] for sid in state_ids}}
    for cid in sorted(current_phase_ids - state_ids):
        fail(f"{rel(gates_path)}: criterion {cid} is missing from CURRENT-STATE.yaml — "
             f"agents read the YAML, so this criterion would not be enforced")

    if compared == 0:
        fail("no gate criteria compared — the check is not doing anything")
    elif not failures:
        ok(f"{compared} criteria agree across both authorities", quiet)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--quiet", action="store_true", help="print only failures")
    args = ap.parse_args()

    print("AIGEM CI checks")
    check_schemas(args.quiet)
    check_state(args.quiet)
    check_tagged_blocks(args.quiet)
    check_inline_drift(args.quiet)
    check_routing(args.quiet)
    check_links(args.quiet)
    check_priority_calibration(args.quiet)
    check_gate_criteria_agree(args.quiet)

    print()
    if failures:
        print(f"FAILED — {len(failures)} problem(s), {checked} check(s) passed")
        return 1
    print(f"PASSED — {checked} checks")
    return 0


if __name__ == "__main__":
    sys.exit(main())
