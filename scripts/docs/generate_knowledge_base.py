#!/usr/bin/env python3
"""Generate committed, read-only Knowledge Hub dashboards from repository sources.

The generated Markdown under docs/knowledge-base/generated/ is intentionally committed so GitHub
and GitLab can render the Knowledge Hub without any hosting. CI regenerates the pages and fails if
the committed copies drift from their authoritative sources.
"""
from __future__ import annotations

import re
from collections import Counter
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]
DOCS = ROOT / "docs"
OUT = DOCS / "knowledge-base" / "generated"


def md_link(label: str, target_from_docs: str) -> str:
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
    workstreams = data.get("workstreams") or []

    lines = [
        "# Current State Dashboard",
        "",
        "> **Generated, committed view.** CI regenerates this file from `governance/state/CURRENT-STATE.yaml` and fails if it drifts. The YAML remains authoritative.",
        "",
        f"**Governance:** {clean(data.get('governance_version'))}  ",
        f"**State as of:** {clean(data.get('state_as_of'))}  ",
        f"**Review due:** {clean(data.get('review_due'))}  ",
        f"**Ratified by:** {clean(data.get('ratified_by'))}",
        "",
        f"Authority: {md_link('CURRENT-STATE.yaml', 'governance/state/CURRENT-STATE.yaml')}",
        "",
        "## Workstreams",
        "",
        "| Workstream | Current stage / phase | Status | Gate | Gate state |",
        "|---|---|---|---|---|",
    ]

    for ws in workstreams:
        lifecycle = ws.get("lifecycle") or {}
        gate = ws.get("current_gate") or {}
        lines.append(
            f"| **{clean(ws.get('id'))} · {clean(ws.get('name'))}** | "
            f"{clean(lifecycle.get('canonical_stage'))}<br>{clean(lifecycle.get('current_phase'))} | "
            f"{clean(lifecycle.get('stage_status'))} | `{clean(gate.get('id'))}` | **{clean(gate.get('state'))}** |"
        )

    for ws in workstreams:
        lifecycle = ws.get("lifecycle") or {}
        objective = ws.get("current_objective") or {}
        gate = ws.get("current_gate") or {}
        criteria = gate.get("exit_criteria") or []
        counts = Counter(clean(c.get("state")) for c in criteria)
        summary = ", ".join(f"{state}: {count}" for state, count in sorted(counts.items())) or "no criteria recorded"

        lines += [
            "",
            f"## {clean(ws.get('id'))} · {clean(ws.get('name'))}",
            "",
            f"**Stage:** {clean(lifecycle.get('canonical_stage'))}  ",
            f"**Phase:** {clean(lifecycle.get('current_phase'))}  ",
            f"**Next:** {clean(lifecycle.get('next_stage'))}  ",
            f"**Objective:** `{clean(objective.get('id'))}` — {clean(objective.get('description'))}",
            "",
            f"**Gate:** `{clean(gate.get('id'))}` · **{clean(gate.get('state'))}** — {summary}",
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
    services = [m for m in modules if m.startswith("services:")]
    libraries = [m for m in modules if m.startswith("libs:")]

    lines = [
        "# Module & Service Inventory",
        "",
        "> **Generated, committed view.** CI regenerates this file from `settings.gradle.kts` and fails if it drifts. A registered module is not proof that its feature or lifecycle stage is complete.",
        "",
        f"**Registered modules:** {len(modules)} · **Services:** {len(services)} · **Shared libraries:** {len(libraries)}",
        "",
        "## Services",
        "",
        "| Gradle module | Repository path |",
        "|---|---|",
    ]
    for module in services:
        name = module.split(":", 1)[1]
        lines.append(f"| `{module}` | `services/{name}/` |")

    lines += [
        "",
        "## Shared libraries",
        "",
        "| Gradle module | Repository path |",
        "|---|---|",
    ]
    for module in libraries:
        name = module.split(":", 1)[1]
        lines.append(f"| `{module}` | `libs/{name}/` |")

    lines += [
        "",
        "## Read this correctly",
        "",
        "- **Registered** means the module participates in the Gradle build.",
        "- **Implemented** requires code plus tests/acceptance evidence.",
        "- **READY/DONE** is a governance/backlog state, not inferred from module presence.",
        "- **Stage complete** requires the relevant gate evidence and human sign-off where required.",
        "",
        "For intended R0 boundaries use the " + md_link("R0 HLD", "architecture/R0-HLD.md") + ". For 1SB adapter contracts use the " + md_link("1SB service SSOT", "1sb-insurance-integration/service-ssot/README.md") + ".",
    ]
    write("service-inventory.md", "\n".join(lines))


def generate_backlog() -> None:
    lines = [
        "# Backlog & Parking Dashboard",
        "",
        "> **Git-native navigation view.** The registers below are already Markdown and therefore render directly in GitHub/GitLab. This page intentionally links to authority instead of copying the queue into a second source of truth.",
        "",
        "## Live queues",
        "",
        f"- {md_link('Suggestion Register', 'governance/registers/SUGGESTION-REGISTER.md')} — all triaged inputs and dispositions.",
        f"- {md_link('Parked Backlog', 'governance/registers/PARKED-BACKLOG.md')} — deferred work, target stage and unpark trigger.",
        f"- {md_link('Dependency Register', 'governance/registers/DEPENDENCY-REGISTER.md')} — blocking/ordering dependencies.",
        f"- {md_link('Risk Register', 'governance/registers/RISK-REGISTER.md')} — known risks and ownership.",
        f"- {md_link('Backlog Rules', 'governance/08-BACKLOG_RULES.md')} — READY/BLOCKED/PARKED/IDEAS/REJECTED/ESCALATED semantics.",
        "",
        "## Status meanings",
        "",
        "| State | Meaning |",
        "|---|---|",
        "| `ADMIT` / `ADMITTED` | Valid for the current-stage backlog; not automatically implemented. |",
        "| `ADMIT-BYPASS` | Human-authorized process bypass recorded for traceability. |",
        "| `READY` | Approved and dependency-ready for pickup. |",
        "| `BLOCKED` | Valid work that cannot proceed until a named blocker clears. |",
        "| `PARKED` | Valid work deliberately deferred with a target/unpark trigger. |",
        "| `IDEAS` / `P5` | Future value without a committed stage. |",
        "| `REJECTED` | Deliberately not being done; reason remains recorded. |",
        "| `ESCALATED` | Requires change control or accountable human decision. |",
    ]
    write("backlog-index.md", "\n".join(lines))


def generate_decisions() -> None:
    lines = [
        "# Decision Dashboard",
        "",
        "> **Git-native navigation view.** Open the authoritative record before relying on a decision. A document existing in the repository does not mean it is ratified.",
        "",
        "## Decision sources",
        "",
        f"- {md_link('Decision Register', 'governance/registers/DECISION-REGISTER.md')} — consolidated governance decision index.",
        f"- {md_link('Change Requests', 'governance/change-requests/')} — controlled change packages (`CR-*`).",
        f"- {md_link('Architecture Decision Log', 'platform/architecture-review/08-architecture-decision-log.md')} — architecture decisions (`ADR-*`).",
        f"- {md_link('Business Decision Log', 'au-bank-insurance-platform/DECISION-LOG.md')} — product/business decisions.",
        "",
        "## Identifier families",
        "",
        "| Prefix | Meaning |",
        "|---|---|",
        "| `ADR-*` | Architecture Decision Record. |",
        "| `CR-*` | Change Request. |",
        "| `DEC-*` | Business/domain/governance decision package. |",
        "| `GOV-*` | Governance decision. |",
        "| `DB-DEC-*` | Database/data architecture decision. |",
        "| `RISK-*` | Risk statement/ownership; not itself an approval. |",
        "",
        "## Status rule",
        "",
        "Always read the status inside the record. `PROPOSED`, `CANDIDATE` or `AI-DRAFTED` is not equivalent to `APPROVED`/`RATIFIED`; T4 records may still require mandatory human signatures.",
    ]
    write("decision-index.md", "\n".join(lines))


def main() -> None:
    generate_current_state()
    generate_service_inventory()
    generate_backlog()
    generate_decisions()
    print(f"Generated Knowledge Hub views in {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
