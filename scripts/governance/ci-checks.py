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
  5. workstream routing is closed over the canonical work-type enum and every path exists
  6. every internal markdown link and heading anchor under docs/ resolves
  7. the priority formula and the action matrix agree to within one band in every
     cell (Rule PRI-4), given the PRI-8 blocking floors
  8. semantic state references and narrative/machine versions agree

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


def check_gate_evidence(quiet: bool) -> None:
    print("\n[2b] GATE-EVIDENCE.yaml validates and matches current-state gate summaries")
    evidence_path = os.path.join(GOV, "state", "GATE-EVIDENCE.yaml")
    state_path = os.path.join(GOV, "state", "CURRENT-STATE.yaml")
    evidence = yaml.safe_load(open(evidence_path, encoding="utf-8"))
    state = yaml.safe_load(open(state_path, encoding="utf-8"))
    errors = sorted(
        jsonschema.Draft202012Validator(load_schema("gate-evidence")).iter_errors(evidence),
        key=lambda error: list(error.path),
    )
    for error in errors[:20]:
        fail(f"{rel(evidence_path)} {list(error.path)}: {error.message}")
    if errors:
        return

    summaries = {item["id"]: item for item in state.get("workstreams", [])}
    drift = 0
    for ledger in evidence.get("workstreams", []):
        summary = summaries.get(ledger["id"])
        if not summary:
            fail(f"gate evidence refers to unknown workstream {ledger['id']}")
            drift += 1
            continue
        gate = summary.get("current_gate") or {}
        if gate.get("id") != ledger.get("gate_id"):
            fail(f"{ledger['id']} gate id drift: state={gate.get('id')} evidence={ledger.get('gate_id')}")
            drift += 1
        summary_criteria = {str(item.get("id")): item.get("state") for item in gate.get("exit_criteria", [])}
        evidence_criteria = {str(item.get("id")): item.get("state") for item in ledger.get("criteria", [])}
        if summary_criteria != evidence_criteria:
            fail(f"{ledger['id']} criterion-state drift: state={summary_criteria} evidence={evidence_criteria}")
            drift += 1
    if not errors and drift == 0:
        ok(f"{len(evidence.get('workstreams', []))} gate ledgers validate and match current state", quiet)


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
    print("\n[5] Workstream routing is closed and every destination exists")
    state = yaml.safe_load(open(os.path.join(GOV, "state", "CURRENT-STATE.yaml"), encoding="utf-8"))
    types = set(load_schema("work-item")["properties"]["type"]["enum"])
    routing = state.get("routing") or {}
    registered = {w.get("id") for w in state.get("workstreams", []) if isinstance(w, dict)}
    missing_workstreams = sorted(registered - set(routing))
    if missing_workstreams:
        fail(f"active workstreams with no routing table: {missing_workstreams}")

    valid_tables = 0
    for workstream, table in sorted(routing.items()):
        if not isinstance(table, dict):
            fail(f"routing[{workstream}] must be a work-type map")
            continue
        unknown = sorted(set(table) - types)
        unrouted = sorted(types - set(table))
        if unknown:
            fail(f"routing[{workstream}] keys that are not work types: {unknown}")
        if unrouted:
            fail(f"routing[{workstream}] work types with no route: {unrouted}")
        for work_type, destinations in table.items():
            if not isinstance(destinations, list) or not destinations:
                fail(f"routing[{workstream}][{work_type}] must be a non-empty list")
                continue
            for destination in destinations:
                target = os.path.normpath(os.path.join(ROOT, destination))
                if not os.path.exists(target):
                    fail(f"routing[{workstream}][{work_type}] -> {destination} (missing)")
        if not unknown and not unrouted:
            valid_tables += 1

    if not missing_workstreams and valid_tables == len(routing):
        ok(f"{len(routing)} routing tables × {len(types)} work types; all paths resolve", quiet)


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
    files = sorted(glob.glob(os.path.join(ROOT, "docs", "**", "*.md"), recursive=True))
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


def check_semantic_state(quiet: bool) -> None:
    print("\n[8] Semantic state references agree")
    state_path = os.path.join(GOV, "state", "CURRENT-STATE.yaml")
    state = yaml.safe_load(open(state_path, encoding="utf-8"))
    bad = 0

    for workstream in state.get("workstreams", []):
        workstream_id = workstream.get("id", "?")
        for authority in workstream.get("authority", []):
            if not os.path.exists(os.path.join(ROOT, authority)):
                fail(f"{workstream_id} authority path missing: {authority}")
                bad += 1

    for name, path in (state.get("registers") or {}).items():
        if not os.path.exists(os.path.join(ROOT, path)):
            fail(f"registers.{name} path missing: {path}")
            bad += 1

    narrative = open(os.path.join(GOV, "01-CURRENT_STATE.md"), encoding="utf-8").read()
    match = re.search(r"\| Governance version \| (AIGEM [0-9.]+) \|", narrative)
    machine_version = state.get("governance_version")
    if not match:
        fail("01-CURRENT_STATE.md has no machine-comparable governance version row")
        bad += 1
    elif match.group(1) != machine_version:
        fail(f"governance version drift: narrative={match.group(1)} machine={machine_version}")
        bad += 1

    if bad == 0:
        ok("authority/register paths exist and governance versions match", quiet)


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


def check_governance_cost(quiet: bool) -> None:
    """Report the documentation-to-code ratio (18 section 2, Cost of governance).

    This is a REPORT, not a gate. It never fails the build: there is no correct
    ratio, and a threshold that blocks a merge would just be routed around. What
    it does is make the number impossible to not notice.

    The framework it measures was, at CR-009, twice the size of the product it
    governed, across 61 consecutive commits with no product code, while every
    other check in this file passed. No mechanical check can decide whether that
    is right — but no team should discover it from `git log` a month later."""
    global checked
    print("\n[9] Cost of governance — documentation vs product code")

    def count(paths: list[str], exts: tuple[str, ...]) -> int:
        total = 0
        for base in paths:
            root = os.path.join(ROOT, base)
            for dirpath, dirnames, filenames in os.walk(root):
                dirnames[:] = [d for d in dirnames if d not in {".git", "build", "node_modules", ".gradle"}]
                for name in filenames:
                    if name.endswith(exts):
                        try:
                            with open(os.path.join(dirpath, name), encoding="utf-8", errors="ignore") as fh:
                                total += sum(1 for _ in fh)
                        except OSError:
                            pass
        return total

    docs = count(["docs"], (".md",))
    code = count(["services", "libs"], (".java", ".kt", ".kts", ".dart", ".sql"))
    if code == 0:
        ok("no product code found — ratio not meaningful", quiet)
        return

    ratio = docs / code
    governance = count(["docs/governance", "docs/context/roles"], (".md",))
    gov_share = governance / docs if docs else 0.0

    detail = (f"docs {docs:,} / code {code:,} = {ratio:.2f}  "
              f"(governance+personas {governance:,} = {gov_share:.0%} of docs)")

    if ratio > 1.5:
        print(f"  WARN  {detail}")
        print(f"        Documentation is {ratio:.1f}x the product code.")
        print( "        Read with 'gate criteria closed per week' (18 section 2, Rule GM-1):")
        print( "        a high ratio with a flat closure rate is the signature of a framework")
        print( "        optimising itself instead of the delivery it exists to serve.")
        # deliberately not a failure — see docstring
        checked += 1
    else:
        ok(detail, quiet)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--quiet", action="store_true", help="print only failures")
    args = ap.parse_args()

    print("AIGEM CI checks")
    check_schemas(args.quiet)
    check_state(args.quiet)
    check_gate_evidence(args.quiet)
    check_tagged_blocks(args.quiet)
    check_inline_drift(args.quiet)
    check_routing(args.quiet)
    check_links(args.quiet)
    check_priority_calibration(args.quiet)
    check_semantic_state(args.quiet)
    check_governance_cost(args.quiet)

    print()
    if failures:
        print(f"FAILED — {len(failures)} problem(s), {checked} check(s) passed")
        return 1
    print(f"PASSED — {checked} checks")
    return 0


if __name__ == "__main__":
    sys.exit(main())
