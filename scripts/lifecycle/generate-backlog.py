#!/usr/bin/env python3
"""Generate the machine-readable backlog and a Jira import CSV from the stage files.

The stage markdown under docs/application-lifecycle-bible/stages/ is the single source
of truth for lifecycle structure. This script parses it and emits:

  * backlog/BACKLOG.yaml    — structured stages, epics, stories, tests and gates
  * backlog/jira-import.csv — Jira CSV import (Epics first, then Stories, Tests, Gates)

Regenerate after any structural change to a stage file:

    python3 scripts/lifecycle/generate-backlog.py

The stage files are authoritative for structure; Jira is authoritative for state.
Do not attempt to sync state back into the YAML.
"""

from __future__ import annotations

import argparse
import csv
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
BIBLE = REPO_ROOT / "docs" / "application-lifecycle-bible"
STAGES_DIR = BIBLE / "stages"

# --- patterns ---------------------------------------------------------------

STAGE_H1 = re.compile(r"^#\s+(S\d{2})\s+[—-]\s+(.+?)\s*$")
EPIC_H3 = re.compile(r"^###\s+(S\d{2}-E\d{2})\s+[—-]\s+(.+?)\s*$")
STORY_ROW = re.compile(r"^\|\s*(S\d{2}-E\d{2}-S\d{2})\s*\|")
TEST_ROW = re.compile(r"^\|\s*(S\d{2}-VT-\d{2})\s*\|")
GATE_ROW = re.compile(r"^\|\s*(S\d{2}-G\d{1,2})\s*\|")
AIGEM_LINE = re.compile(r"\*\*AIGEM stage:\*\*\s*(L\d+)")
OWNER_LINE = re.compile(r"\*\*Owner:\*\*\s*(.+?)(?:\s*$|\s*·\s*\*\*)")

# Priority defaults per stage. Foundation and compliance-bearing stages carry the
# highest default because the realignment sequences them first; every value is a
# starting point that Product may override in Jira.
STAGE_PRIORITY = {
    "S00": "P3", "S01": "P3", "S02": "P1", "S03": "P2", "S04": "P2",
    "S05": "P2", "S06": "P2", "S07": "P2", "S08": "P1", "S09": "P1",
    "S10": "P2", "S11": "P1", "S12": "P2", "S13": "P3", "S14": "P2",
    "S15": "P3",
}


def clean(text: str) -> str:
    """Strip markdown emphasis, links and stray whitespace from a cell."""
    text = re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", text)   # links -> label
    text = text.replace("**", "").replace("`", "")
    text = re.sub(r"(?<!\w)\*(.+?)\*(?!\w)", r"\1", text)  # emphasis
    return re.sub(r"\s+", " ", text).strip()


def split_row(line: str) -> list[str]:
    """Split a markdown table row into cleaned cells."""
    return [clean(c) for c in line.strip().strip("|").split("|")]


@dataclass
class Story:
    id: str
    title: str
    acceptance_criteria: str


@dataclass
class Epic:
    id: str
    title: str
    owner: str
    stories: list[Story] = field(default_factory=list)


@dataclass
class Stage:
    id: str
    name: str
    aigem_stage: str = ""
    owner: str = ""
    epics: list[Epic] = field(default_factory=list)
    tests: list[dict] = field(default_factory=list)
    gates: list[dict] = field(default_factory=list)


def parse_stage(path: Path) -> Stage | None:
    lines = path.read_text(encoding="utf-8").splitlines()

    stage: Stage | None = None
    epic: Epic | None = None

    for line in lines:
        if m := STAGE_H1.match(line):
            stage = Stage(id=m.group(1), name=clean(m.group(2)))
            continue
        if stage is None:
            continue

        if m := AIGEM_LINE.search(line):
            stage.aigem_stage = m.group(1)
        if not stage.owner and (m := OWNER_LINE.search(line)):
            stage.owner = clean(m.group(1))

        if m := EPIC_H3.match(line):
            # "Title · *Owner* · **closes GAP-006**" -> title, owner
            rest = m.group(2)
            parts = [p.strip() for p in rest.split("·")]
            title = clean(parts[0])
            owner = clean(parts[1]) if len(parts) > 1 else stage.owner
            epic = Epic(id=m.group(1), title=title, owner=owner)
            stage.epics.append(epic)
            continue

        if STORY_ROW.match(line) and epic is not None:
            cells = split_row(line)
            if len(cells) >= 3:
                epic.stories.append(
                    Story(id=cells[0], title=cells[1], acceptance_criteria=cells[2])
                )
            continue

        if TEST_ROW.match(line):
            cells = split_row(line)
            if len(cells) >= 4:
                stage.tests.append({
                    "id": cells[0],
                    "validates": cells[1],
                    "method": cells[2],
                    "pass_condition": cells[-1],
                })
            continue

        if GATE_ROW.match(line):
            cells = split_row(line)
            if len(cells) >= 4:
                stage.gates.append({
                    "id": cells[0],
                    "criterion": cells[1],
                    "evidence_level": cells[2],
                    "evidence_artefact": cells[3],
                })

    return stage


def yaml_escape(value: str) -> str:
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'


def write_yaml(stages: list[Stage], out: Path) -> None:
    lines = [
        "# Application Lifecycle Backlog — GENERATED FILE, DO NOT EDIT BY HAND.",
        "#",
        "# Source of truth: docs/application-lifecycle-bible/stages/*.md",
        "# Regenerate:      python3 scripts/lifecycle/generate-backlog.py",
        "#",
        "# Structure lives here; state lives in Jira. Do not sync state back.",
        "",
        'schema_version: "1.0"',
        'framework: "Application Lifecycle Bible 1.0"',
        'status: "proposed — binding after CR-010 ratification"',
        "",
        "stages:",
    ]
    for st in stages:
        lines += [
            f"  - id: {st.id}",
            f"    name: {yaml_escape(st.name)}",
            f"    aigem_stage: {yaml_escape(st.aigem_stage)}",
            f"    owner: {yaml_escape(st.owner)}",
            f"    default_priority: {STAGE_PRIORITY.get(st.id, 'P3')}",
        ]
        lines.append("    epics:")
        if not st.epics:
            lines[-1] = "    epics: []"
        for ep in st.epics:
            lines += [
                f"      - id: {ep.id}",
                f"        title: {yaml_escape(ep.title)}",
                f"        owner: {yaml_escape(ep.owner)}",
            ]
            lines.append("        stories:")
            if not ep.stories:
                lines[-1] = "        stories: []"
            for s in ep.stories:
                lines += [
                    f"          - id: {s.id}",
                    f"            title: {yaml_escape(s.title)}",
                    f"            acceptance_criteria: {yaml_escape(s.acceptance_criteria)}",
                ]
        lines.append("    validation_tests:")
        if not st.tests:
            lines[-1] = "    validation_tests: []"
        for t in st.tests:
            lines += [
                f"      - id: {t['id']}",
                f"        validates: {yaml_escape(t['validates'])}",
                f"        method: {yaml_escape(t['method'])}",
                f"        pass_condition: {yaml_escape(t['pass_condition'])}",
            ]
        lines.append("    exit_gate:")
        if not st.gates:
            lines[-1] = "    exit_gate: []"
        for g in st.gates:
            lines += [
                f"      - id: {g['id']}",
                f"        criterion: {yaml_escape(g['criterion'])}",
                f"        evidence_level: {yaml_escape(g['evidence_level'])}",
                f"        evidence_artefact: {yaml_escape(g['evidence_artefact'])}",
            ]
        lines.append("")

    out.write_text("\n".join(lines) + "\n", encoding="utf-8")


CSV_COLUMNS = [
    "Issue Key", "Summary", "Issue Type", "Fix Version", "Epic Link", "Epic Name",
    "Priority", "Assignee Persona", "Labels", "Acceptance Criteria",
    "Evidence Required", "Description",
]


def write_csv(stages: list[Stage], out: Path) -> None:
    rows: list[dict] = []
    for st in stages:
        version = f"{st.id} {st.name}"
        prio = STAGE_PRIORITY.get(st.id, "P3")
        label = f"stage-{st.id.lower()}"

        for ep in st.epics:
            rows.append({
                "Issue Key": ep.id, "Summary": f"{ep.id} — {ep.title}",
                "Issue Type": "Epic", "Fix Version": version, "Epic Link": "",
                "Epic Name": ep.id, "Priority": prio, "Assignee Persona": ep.owner,
                "Labels": f"{label} lifecycle-epic", "Acceptance Criteria": "",
                "Evidence Required": "",
                "Description": f"Epic of stage {st.id} ({st.name}). See "
                               f"docs/application-lifecycle-bible/stages/.",
            })
            for s in ep.stories:
                rows.append({
                    "Issue Key": s.id, "Summary": f"{s.id} — {s.title}",
                    "Issue Type": "Story", "Fix Version": version,
                    "Epic Link": ep.id, "Epic Name": "", "Priority": prio,
                    "Assignee Persona": ep.owner,
                    "Labels": f"{label} lifecycle-story",
                    "Acceptance Criteria": s.acceptance_criteria,
                    "Evidence Required": "",
                    "Description": s.acceptance_criteria,
                })

        for t in st.tests:
            rows.append({
                "Issue Key": t["id"], "Summary": f"{t['id']} — {t['validates']}",
                "Issue Type": "Test", "Fix Version": version, "Epic Link": "",
                "Epic Name": "", "Priority": prio, "Assignee Persona": "Swapnali / QA",
                "Labels": f"{label} validation-test",
                "Acceptance Criteria": t["pass_condition"],
                "Evidence Required": "E3/E4",
                "Description": f"Method: {t['method']}. Pass: {t['pass_condition']}",
            })

        for g in st.gates:
            rows.append({
                "Issue Key": g["id"], "Summary": f"{g['id']} — {g['criterion']}",
                "Issue Type": "Task", "Fix Version": version, "Epic Link": "",
                "Epic Name": "", "Priority": "P1", "Assignee Persona": "Kalpana / Delivery",
                "Labels": f"{label} gate-criterion",
                "Acceptance Criteria": g["criterion"],
                "Evidence Required": g["evidence_level"],
                "Description": f"Evidence artefact: {g['evidence_artefact']}",
            })

    with out.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=CSV_COLUMNS)
        writer.writeheader()
        writer.writerows(rows)
    return len(rows)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stages-dir", type=Path, default=STAGES_DIR)
    parser.add_argument("--out-dir", type=Path, default=BIBLE / "backlog")
    args = parser.parse_args()

    stage_files = sorted(args.stages_dir.glob("S*.md"))
    if not stage_files:
        print(f"error: no stage files found in {args.stages_dir}", file=sys.stderr)
        return 1

    stages = [s for f in stage_files if (s := parse_stage(f))]
    args.out_dir.mkdir(parents=True, exist_ok=True)

    write_yaml(stages, args.out_dir / "BACKLOG.yaml")
    row_count = write_csv(stages, args.out_dir / "jira-import.csv")

    epics = sum(len(s.epics) for s in stages)
    stories = sum(len(e.stories) for s in stages for e in s.epics)
    tests = sum(len(s.tests) for s in stages)
    gates = sum(len(s.gates) for s in stages)

    print(f"stages: {len(stages)}  epics: {epics}  stories: {stories}  "
          f"validation tests: {tests}  gate criteria: {gates}")
    print(f"wrote {args.out_dir/'BACKLOG.yaml'}")
    print(f"wrote {args.out_dir/'jira-import.csv'} ({row_count} rows)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
