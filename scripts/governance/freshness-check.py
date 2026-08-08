#!/usr/bin/env python3
"""
AIGEM freshness check — is the governance state fresh enough for agents to trust?

Run: weekly (Delivery Lead), at every stage gate, and by every AI agent at session start.
See docs/governance/RUNBOOK.md section 4.5.

Exit codes:
  0  fresh                — agents may run the full pipeline
  1  warnings             — agents proceed, but must state the warning in replies
  2  HALT-class staleness — agents must not ADMIT new work (may still PARK / REJECT)

Standard library only. No install step, no network.
"""

from __future__ import annotations

import argparse
import datetime as dt
import os
import re
import subprocess
import sys

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
STATE_FILE = "docs/governance/state/CURRENT-STATE.yaml"

# (path, owner, max_age_days, severity)
#   HALT — staleness blocks ADMIT decisions (see RUNBOOK section 4.5)
#   WARN — agents proceed but must disclose
ARTEFACTS = [
    (STATE_FILE,                                        "Delivery Lead", 30, "HALT"),
    ("docs/governance/01-CURRENT_STATE.md",             "Delivery Lead", 30, "HALT"),
    ("docs/governance/04-STAGE_GATES.md",               "Architect",     14, "WARN"),
    ("docs/governance/02-PROJECT_SCOPE.md",             "Product Owner", 90, "WARN"),
    ("docs/governance/registers/SUGGESTION-REGISTER.md","triage owner",   7, "WARN"),
    ("docs/governance/registers/PARKED-BACKLOG.md",     "Delivery Lead", 90, "WARN"),
    ("docs/governance/registers/DEPENDENCY-REGISTER.md","Tech Lead",     14, "WARN"),
    ("docs/governance/registers/RISK-REGISTER.md",      "Delivery Lead", 90, "WARN"),
    ("docs/governance/registers/ASSUMPTION-REGISTER.md","authors",       90, "WARN"),
    ("docs/1sb-insurance-integration/service-ssot/TECH-DEBT.md", "Tech Lead", 90, "WARN"),
]

GREEN, YELLOW, RED, GREY, RESET = "\033[32m", "\033[33m", "\033[31m", "\033[90m", "\033[0m"


def colour(enabled: bool):
    if enabled and sys.stdout.isatty() and os.environ.get("TERM") != "dumb":
        return GREEN, YELLOW, RED, GREY, RESET
    return "", "", "", "", ""


def scalar(text: str, key: str) -> str | None:
    """Read a top-level scalar from the state file without a YAML dependency."""
    m = re.search(rf"^{re.escape(key)}:\s*(.+?)\s*(?:#.*)?$", text, re.M)
    if not m:
        return None
    return m.group(1).strip().strip("\"'") or None


def parse_date(value: str | None) -> dt.date | None:
    if not value:
        return None
    try:
        return dt.date.fromisoformat(value)
    except ValueError:
        return None


def last_touched(path: str) -> dt.date | None:
    """Last commit date for the path; falls back to filesystem mtime."""
    full = os.path.join(REPO_ROOT, path)
    if not os.path.exists(full):
        return None
    try:
        out = subprocess.run(
            ["git", "-C", REPO_ROOT, "log", "-1", "--format=%cI", "--", path],
            capture_output=True, text=True, timeout=15,
        )
        stamp = out.stdout.strip()
        if stamp:
            return dt.datetime.fromisoformat(stamp).date()
    except (OSError, ValueError, subprocess.SubprocessError):
        pass
    return dt.date.fromtimestamp(os.path.getmtime(full))


def main() -> int:
    ap = argparse.ArgumentParser(description="AIGEM governance freshness check")
    ap.add_argument("--today", help="override today's date (YYYY-MM-DD), for testing")
    ap.add_argument("--quiet", action="store_true", help="print only problems")
    ap.add_argument("--no-color", action="store_true")
    args = ap.parse_args()

    g, y, r, grey, reset = colour(not args.no_color)
    today = parse_date(args.today) or dt.date.today()

    halts: list[str] = []
    warns: list[str] = []

    print(f"AIGEM freshness check — {today.isoformat()}\n")

    # --- state file contents -------------------------------------------------
    state_path = os.path.join(REPO_ROOT, STATE_FILE)
    if not os.path.exists(state_path):
        print(f"{r}HALT{reset}  {STATE_FILE} is missing.")
        print("      Agents must halt triage entirely (Rule CS-1). Restore it before any work.")
        return 2

    text = open(state_path, encoding="utf-8").read()
    state_as_of = parse_date(scalar(text, "state_as_of"))
    review_due = parse_date(scalar(text, "review_due"))
    provisional = (scalar(text, "provisional") or "").lower() == "true"
    ratified = scalar(text, "ratified_by")

    print("State file")
    if state_as_of is None:
        halts.append("state_as_of is missing or unparseable")
        print(f"  {r}HALT{reset}  state_as_of missing or unparseable")
    else:
        age = (today - state_as_of).days
        mark = r + "HALT" + reset if age > 30 else (y + "WARN" + reset if age > 14 else g + "OK  " + reset)
        print(f"  {mark}  state_as_of {state_as_of} ({age} days old)")
        if age > 30:
            halts.append(f"state_as_of is {age} days old (limit 30)")
        elif age > 14:
            warns.append(f"state_as_of is {age} days old — refresh at the next Governance Sync")

    if review_due is None:
        warns.append("review_due is not set")
        print(f"  {y}WARN{reset}  review_due not set")
    elif today > review_due:
        halts.append(f"review_due passed on {review_due}")
        print(f"  {r}HALT{reset}  review_due passed on {review_due}")
    else:
        print(f"  {g}OK  {reset}  review_due {review_due} ({(review_due - today).days} days remaining)")

    if provisional or not ratified or ratified.lower() == "null":
        warns.append("state is provisional — ADMIT restricted (01 section 7)")
        print(f"  {y}WARN{reset}  state is provisional / unratified — ADMIT restricted to "
              f"work an authority document already lists")
    else:
        print(f"  {g}OK  {reset}  ratified by {ratified}")

    # --- artefact ages -------------------------------------------------------
    print("\nArtefacts")
    for path, owner, limit, severity in ARTEFACTS:
        touched = last_touched(path)
        if touched is None:
            warns.append(f"{path} is missing")
            print(f"  {y}WARN{reset}  {path:<58} missing  (owner: {owner})")
            continue
        age = (today - touched).days
        if age > limit:
            if severity == "HALT":
                halts.append(f"{path} is {age} days old (limit {limit}, owner {owner})")
                print(f"  {r}HALT{reset}  {path:<58} {age:>4}d  (limit {limit}, owner: {owner})")
            else:
                warns.append(f"{path} is {age} days old (limit {limit}, owner {owner})")
                print(f"  {y}WARN{reset}  {path:<58} {age:>4}d  (limit {limit}, owner: {owner})")
        elif not args.quiet:
            print(f"  {g}OK  {reset}  {path:<58} {age:>4}d  {grey}(limit {limit}){reset}")

    # --- verdict -------------------------------------------------------------
    print()
    if halts:
        print(f"{r}VERDICT: HALT-CLASS STALENESS{reset}")
        for h in halts:
            print(f"  - {h}")
        print("\nAgents: do NOT admit new work. You may still PARK and REJECT, and you must "
              "state the staleness in every reply.")
        print("Delivery Lead: refresh docs/governance/state/CURRENT-STATE.yaml "
              "(RUNBOOK section 4).")
        return 2
    if warns:
        print(f"{y}VERDICT: WARNINGS{reset}")
        for w in warns:
            print(f"  - {w}")
        print("\nAgents: proceed, and state the warning in replies that depend on it.")
        return 1
    print(f"{g}VERDICT: FRESH{reset} — governance state is current; run the full pipeline.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
