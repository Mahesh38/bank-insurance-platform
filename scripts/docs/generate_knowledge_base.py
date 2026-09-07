#!/usr/bin/env python3
"""Generate read-only Knowledge Hub views from repository source-of-truth files.

The generated Markdown is intentionally not committed. Run this before MkDocs so the UI reflects
whatever is currently in CURRENT-STATE.yaml, governance registers and settings.gradle.kts.
"""
from __future__ import annotations

import re
from collections import Counter
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]
DOCS = ROOT / "docs"
OUT = DOCS / "knowledge-base" / "generated"


def rel_doc(path: Path) -> str:
    return path.relative_to(DOCS).as_posix()


def md_link(label: str, target_from_docs: str) -> str:
    # Generated files are docs/knowledge-base/generated/*, therefore ../../ reaches docs/.
    return f"[{label}](../../{target_from_docs})"


def clean(value) -> str:
    if value is None:
        return "—"
    return str(value).replace("\n", " ").strip()


def write(name: str, text: str) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / name).write_text(text.rstrip() + "\n", encoding="utf-8")


def generate_current_state() -> None:
    source = DOCS / "governance" / "state" / "CURRENT-STATE.yaml"
    data = yaml.safe_load(source.read_text(encoding="utf-8"))
    lines = [
        "# Live Current State",
        "",
        "!!! info \"Generated view\"",
        "    This page is generated from `governance/state/CURRENT-STATE.yaml`. It is a navigation/readability view, not a new source of truth.",
        "",
        f"**Governance:** {clean(data.get('governance_version'))}  ",
        f"**State as of:** {clean(data.get('state_as_of'))}  ",
        f"**Review due:** {clean(data.get('review_due'))}  ",
        f"**Ratified by:** {clean(data.get('ratified_by'))}  ",
        "",
        f"Source: {md_link('CURRENT-STATE.yaml', 'governance/state/CURRENT-STATE.yaml')}",
        "",
        "## Workstreams",
        "",
        "| Workstream | Stage / phase | Status | Current gate | Gate state |",
        "|---|---|---|---|---|",
    ]

    workstreams = data.get("workstreams") or []
    for ws in workstreams:
        lifecycle = ws.get("lifecycle") or {}
        gate = ws.get("current_gate") or {}
        lines.append(
            f"| **{clean(ws.get('id'))} · {clean(ws.get('name'))}** | "
            f"{clean(lifecycle.get('canonical_stage'))}<br>{clean(lifecycle.get('current_phase'))} | "
            f"{clean(lifecycle.get('stage_status'))} | {clean(gate.get('id'))} | {clean(gate.get('state'))} |"
        )

    for ws in workstreams:
        wid = clean(ws.get("id"))
        lines += ["", f"## {wid} · {clean(ws.get('name'))}", ""]
        lifecycle = ws.get("lifecycle") or {}
        objective = ws.get("current_objective") or {}
        deliverable = ws.get("current_deliverable") or {}
        scope = ws.get("current_scope") or {}
        gate = ws.get("current_gate") or {}
        lines += [
            f"**Stage:** {clean(lifecycle.get('canonical_stage'))}  ",
            f"**Phase:** {clean(lifecycle.get('current_phase'))}  ",
            f"**Next:** {clean(lifecycle.get('next_stage'))}  ",
            f"**Objective:** `{clean(objective.get('id'))}` — {clean(objective.get('description'))}",
            "",
            "### Current deliverable",
            "",
            clean(deliverable.get("description")),
            "",
            "### Scope snapshot",
            "",
        ]
        ins = scope.get("in_scope") or []
        outs = scope.get("out_of_scope") or []
        never = scope.get("never") or []
        lines += [f"**In scope:** {len(ins)} items · **Deferred/out:** {len(outs)} items · **Never constraints:** {len(never)}", ""]
        if ins:
            lines.append("**In scope**")
            lines.extend(f"- {clean(item)}" for item in ins)
            lines.append("")
        if outs:
            lines.append("**Deferred / out of scope**")
            for item in outs:
                if isinstance(item, dict):
                    lines.append(f"- {clean(item.get('item'))} — revisit: **{clean(item.get('revisit_at'))}**")
                else:
                    lines.append(f"- {clean(item)}")
            lines.append("")
        if never:
            lines.append("**Never / standing constraints**")
            lines.extend(f"- {clean(item)}" for item in never)
            lines.append("")

        criteria = gate.get("exit_criteria") or []
        counts = Counter(clean(c.get("state")) for c in criteria)
        lines += [
            "### Current gate",
            "",
            f"**{clean(gate.get('id'))} · {clean(gate.get('state'))}** — " + ", ".join(f"{k}: {v}" for k, v in sorted(counts.items())),
            "",
            "| Criterion | State | Owner |",
            "|---|---|---|",
        ]
        for criterion in criteria:
            lines.append(
                f"| `{clean(criterion.get('id'))}` {clean(criterion.get('criterion'))} | "
                f"**{clean(criterion.get('state'))}** | {clean(criterion.get('owner'))} |"
            )
        if not criteria:
            lines.append("| — | No criteria recorded | — |")

    write("current-state.md", "\n".join(lines))


def parse_gradle_modules(text: str) -> list[str]:
    return re.findall(r'"((?:libs|services):[^"\n]+)"', text)


def generate_service_inventory() -> None:
    settings = ROOT / "settings.gradle.kts"
    modules = parse_gradle_modules(settings.read_text(encoding="utf-8"))
    lines = [
        "# Live Module & Service Inventory",
        "",
        "!!! info \"Generated view\"",
        "    Modules come directly from `settings.gradle.kts`. Source/controller counts describe repository structure only; they do **not** mean a feature or stage is complete.",
        "",
        f"**Registered modules:** {len(modules)} · **Services:** {sum(m.startswith('services:') for m in modules)} · **Shared libraries:** {sum(m.startswith('libs:') for m in modules)}",
        "",
        "| Module | Type | Java source files | Controllers | Mapped endpoints | README |",
        "|---|---|---:|---:|---:|---|",
    ]
    mapping_re = re.compile(r"@(Get|Post|Put|Delete|Patch)Mapping\\b")
    for module in modules:
        parts = module.split(":", 1)
        path = ROOT / parts[0] / parts[1]
        java_files = list(path.rglob("*.java")) if path.exists() else []
        controllers = [p for p in java_files if p.name.endswith("Controller.java")]
        endpoint_count = 0
        for p in controllers:
            try:
                endpoint_count += len(mapping_re.findall(p.read_text(encoding="utf-8", errors="ignore")))
            except OSError:
                pass
        readme = path / "README.md"
        # Files outside docs/ are not rendered by MkDocs; point to repository source instead.
        readme_cell = "yes" if readme.exists() else "—"
        lines.append(
            f"| `{module}` | {parts[0][:-1] if parts[0].endswith('s') else parts[0]} | {len(java_files)} | {len(controllers)} | {endpoint_count} | {readme_cell} |"
        )

    lines += [
        "",
        "## How to use this page",
        "",
        "- **Module exists** means Gradle includes it.",
        "- **Controller/API exists** means code exposes a route.",
        "- **Implemented** requires tests and acceptance evidence.",
        "- **READY/DONE** is a governance/backlog status, not inferred from source counts.",
        "- **Stage complete** requires its gate evidence and human sign-off where required.",
        "",
        "For intended R0 contracts, use the " + md_link("R0 HLD", "architecture/R0-HLD.md") + ". For 1SB contracts, use the " + md_link("1SB service SSOT", "1sb-insurance-integration/service-ssot/README.md") + ".",
    ]
    write("service-inventory.md", "\n".join(lines))


def table_rows(markdown: str) -> list[list[str]]:
    rows: list[list[str]] = []
    for raw in markdown.splitlines():
        line = raw.strip()
        if not (line.startswith("|") and line.endswith("|")):
            continue
        cells = [c.strip() for c in line.strip("|").split("|")]
        if not cells or all(re.fullmatch(r":?-{3,}:?", c or "") for c in cells):
            continue
        rows.append(cells)
    return rows


def generate_decisions() -> None:
    source = DOCS / "governance" / "registers" / "DECISION-REGISTER.md"
    rows = table_rows(source.read_text(encoding="utf-8"))
    interesting = []
    for cells in rows:
        first = cells[0] if cells else ""
        if re.fullmatch(r"(?:ADR|CR|GOV)-\d+|DB-DEC-\d+", first):
            interesting.append(cells)
    lines = [
        "# Live Decision Index",
        "",
        "!!! info \"Generated view\"",
        "    This is a compact index extracted from the Decision Register. Always open the authoritative record before relying on a decision.",
        "",
        f"Source: {md_link('DECISION-REGISTER.md', 'governance/registers/DECISION-REGISTER.md')}",
        "",
        f"**Indexed decision rows:** {len(interesting)}",
        "",
        "| ID | Summary | Status / decision |",
        "|---|---|---|",
    ]
    for cells in interesting:
        ident = cells[0]
        # Register sections use different shapes. Prefer the longest descriptive middle cell and the cell that looks like a status.
        candidates = [c for c in cells[1:] if c]
        summary = max(candidates, key=len) if candidates else "—"
        status_tokens = [c for c in candidates if re.search(r"APPROV|PROPOS|PENDING|CANDIDATE|REJECT|ACCEPT|DRAFT|DECID", c, re.I)]
        status = status_tokens[0] if status_tokens else (candidates[-1] if candidates else "—")
        summary = summary.replace("|", "\\|")
        status = status.replace("|", "\\|")
        lines.append(f"| `{ident}` | {summary} | {status} |")
    write("decision-index.md", "\n".join(lines))


def generate_backlog() -> None:
    parked = DOCS / "governance" / "registers" / "PARKED-BACKLOG.md"
    suggestions = DOCS / "governance" / "registers" / "SUGGESTION-REGISTER.md"
    ptext = parked.read_text(encoding="utf-8")
    stext = suggestions.read_text(encoding="utf-8")
    parked_ids = sorted(set(re.findall(r"SUG-[A-Za-z0-9-]+", ptext)))
    all_sugs = sorted(set(re.findall(r"SUG-[A-Za-z0-9-]+", stext)))
    verdicts = Counter(re.findall(r"\b(ADMIT-BYPASS|ADMITTED|PARKED|REJECTED|ESCALATED|CLOSED|CANDIDATE)\b", stext))
    lines = [
        "# Live Backlog & Suggestion Index",
        "",
        "!!! info \"Generated view\"",
        "    Counts are derived from the governance registers. Use the registers themselves for the full record, rationale, target stage and unpark trigger.",
        "",
        f"**Suggestion IDs seen:** {len(all_sugs)}  ",
        f"**Parked IDs referenced:** {len(parked_ids)}  ",
        "",
        "## Register status tokens",
        "",
        "| Token | Occurrences |",
        "|---|---:|",
    ]
    for token, count in sorted(verdicts.items()):
        lines.append(f"| `{token}` | {count} |")
    lines += [
        "",
        "## Authoritative queues",
        "",
        f"- {md_link('Suggestion Register', 'governance/registers/SUGGESTION-REGISTER.md')} — every triaged input and its disposition.",
        f"- {md_link('Parked Backlog', 'governance/registers/PARKED-BACKLOG.md')} — future-stage items with target/unpark information.",
        f"- {md_link('Backlog Rules', 'governance/08-BACKLOG_RULES.md')} — READY/BLOCKED/PARKED/IDEAS/REJECTED/ESCALATED semantics.",
        "",
        "## Recently referenced parked IDs",
        "",
    ]
    for sid in parked_ids[-50:]:
        lines.append(f"- `{sid}`")
    if not parked_ids:
        lines.append("- None found")
    write("backlog-index.md", "\n".join(lines))


def main() -> None:
    generate_current_state()
    generate_service_inventory()
    generate_decisions()
    generate_backlog()
    print(f"Generated Knowledge Hub views in {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
