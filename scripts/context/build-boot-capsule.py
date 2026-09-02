#!/usr/bin/env python3
"""Regenerate the volatile block of docs/context/BOOT.md from CURRENT-STATE.yaml.

BOOT.md is the tier-0 context capsule: the smallest file an agent can read and still
satisfy the ten-fact knowledge contract (RUNBOOK section 8.1). Everything volatile in it
is GENERATED from docs/governance/state/CURRENT-STATE.yaml so the capsule can never
disagree with the canonical state file.

  python3 scripts/context/build-boot-capsule.py           # rewrite the block in place
  python3 scripts/context/build-boot-capsule.py --check   # exit 1 if it is stale (CI)

Needs PyYAML only. CURRENT-STATE.yaml stays the single source of truth; this script never
writes to it.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

try:
    import yaml
except ImportError as exc:  # pragma: no cover
    print(f"build-boot-capsule needs PyYAML: {exc}", file=sys.stderr)
    raise SystemExit(2)

ROOT = Path(__file__).resolve().parents[2]
STATE = ROOT / "docs/governance/state/CURRENT-STATE.yaml"
BOOT = ROOT / "docs/context/BOOT.md"

BEGIN = "<!-- BEGIN GENERATED: current-state -->"
END = "<!-- END GENERATED: current-state -->"


def bullets(items: list, key: str, extra: str | None = None, limit: int | None = None) -> list[str]:
    out = []
    for item in (items or [])[:limit]:
        if isinstance(item, dict):
            text = str(item.get(key, "")).strip()
            if extra and item.get(extra):
                text = f"{text} — revisit at {item[extra]}"
        else:
            text = str(item).strip()
        if text:
            out.append(f"- {text}")
    return out


def gate_lines(gate: dict) -> list[str]:
    criteria = gate.get("exit_criteria") or []
    open_states = {"OPEN", "BLOCKED", "PARTIAL", "FAILED"}
    still_open = [c for c in criteria if isinstance(c, dict) and str(c.get("state", "")).upper() in open_states]
    lines = [
        f"**Open gate:** `{gate.get('id')}` · state `{gate.get('state')}` · "
        f"{len(still_open)} of {len(criteria)} exit criteria still open"
    ]
    for c in still_open:
        blockers = c.get("blockers") or []
        suffix = f" · blocked by {', '.join(blockers)}" if blockers else ""
        owner = f" · {c['owner']}" if c.get("owner") else ""
        lines.append(f"- `{c.get('id')}` **{c.get('state')}** — {c.get('criterion')}{owner}{suffix}")
    return lines


def render(state: dict) -> str:
    lines: list[str] = []
    lines.append(
        f"> Generated from [`CURRENT-STATE.yaml`](../governance/state/CURRENT-STATE.yaml) by "
        f"`scripts/context/build-boot-capsule.py`. Do not hand-edit this block."
    )
    lines.append("")
    lines.append(
        f"**{state.get('governance_version')}** · state as of **{state.get('state_as_of')}** · "
        f"review due **{state.get('review_due')}** · "
        f"provisional: **{'yes' if state.get('provisional') else 'no'}**"
    )
    lines.append("")
    lines.append(
        "> **Fact 9 — freshness.** Past `review_due`, an agent may park and reject but "
        "**must not admit new work** (Rule CS-1). Run `java scripts/governance/FreshnessCheck.java` "
        "and act on the exit code: `0` fresh · `1` warn, disclose it · `2` do not admit."
    )

    for ws in state.get("workstreams") or []:
        lifecycle = ws.get("lifecycle") or {}
        objective = ws.get("current_objective") or {}
        scope = ws.get("current_scope") or {}
        gate = ws.get("current_gate") or {}

        lines.append("")
        lines.append(f"### {ws.get('id')} — {ws.get('name')}")
        lines.append("")
        lines.append(f"**Stage:** {lifecycle.get('canonical_stage')} · `{lifecycle.get('stage_status')}`  ")
        lines.append(f"**Phase:** {lifecycle.get('current_phase')}  ")
        lines.append(f"**Next:** {lifecycle.get('next_stage')}")
        lines.append("")
        description = " ".join(str(objective.get("description", "")).split())
        lines.append(f"**Objective** (`{objective.get('id')}`): {description}")
        lines.append("")
        lines.extend(gate_lines(gate))

        out_of_scope = bullets(scope.get("out_of_scope"), "item", extra="revisit_at")
        if out_of_scope:
            lines.append("")
            lines.append("**Out of scope now — do not propose, do not build:**")
            lines.extend(out_of_scope)
        never = bullets(scope.get("never"), "item")
        if never:
            lines.append("")
            lines.append("**Never in this workstream:**")
            lines.extend(never)

    constraints = state.get("standing_constraints") or []
    lines.append("")
    lines.append("### Standing constraints — never violate, in any workstream")
    lines.append("")
    lines.extend(bullets(constraints, "constraint"))

    debt = state.get("known_open_debt") or []
    lines.append("")
    lines.append(
        "### Known open debt — **fact 7: do not re-report these**"
    )
    lines.append("")
    lines.append(
        "`" + "` · `".join(str(d) for d in debt) + "`  "
        if debt else "_none recorded_  "
    )
    lines.append(
        "Detail: [`01-CURRENT_STATE.md` section 6]"
        "(../governance/01-CURRENT_STATE.md#6-known-open-debt-affecting-triage)."
    )
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="exit 1 if BOOT.md is stale")
    args = parser.parse_args()

    state = yaml.safe_load(STATE.read_text(encoding="utf-8"))
    text = BOOT.read_text(encoding="utf-8")

    if BEGIN not in text or END not in text:
        print(f"BOOT.md is missing the {BEGIN} / {END} markers", file=sys.stderr)
        return 2

    head, rest = text.split(BEGIN, 1)
    _, tail = rest.split(END, 1)
    updated = f"{head}{BEGIN}\n\n{render(state)}\n\n{END}{tail}"

    if updated == text:
        print("BOOT capsule is current")
        return 0
    if args.check:
        print(
            "BOOT capsule is STALE — CURRENT-STATE.yaml changed.\n"
            "  run: python3 scripts/context/build-boot-capsule.py",
            file=sys.stderr,
        )
        return 1
    BOOT.write_text(updated, encoding="utf-8")
    print(f"BOOT capsule regenerated — {len(updated):,} bytes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
