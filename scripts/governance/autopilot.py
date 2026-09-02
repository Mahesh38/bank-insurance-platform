#!/usr/bin/env python3
"""Proposal-only AIGEM controller: status, next safe work and transition packages."""

from __future__ import annotations

import argparse
import json
import sys
from copy import deepcopy
from pathlib import Path

try:
    import jsonschema
    import yaml
except ImportError as exc:  # pragma: no cover
    print(f"autopilot needs PyYAML and jsonschema: {exc}", file=sys.stderr)
    raise SystemExit(2)

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_EVIDENCE = ROOT / "docs/governance/state/GATE-EVIDENCE.yaml"
DEFAULT_SCHEMA = ROOT / "docs/governance/schemas/gate-evidence.schema.json"
PRIORITY = {"P1": 1, "P2": 2, "P3": 3, "P4": 4, "P5": 5}
EFFORT = {"XS": 1, "S": 2, "M": 3, "L": 4, "XL": 5}


class AutopilotRefusal(RuntimeError):
    """A safety or evidence precondition prevents the requested proposal."""


def validate_policy(policy: dict) -> None:
    expected = {
        "mode": "proposal-only",
        "human_pass_required": True,
        "silence_approves": False,
        "automatic_waivers": False,
    }
    if policy != expected:
        raise AutopilotRefusal("unsafe policy: controller only runs in proposal-only mode")


def load_bundle(evidence_path: Path = DEFAULT_EVIDENCE, schema_path: Path = DEFAULT_SCHEMA) -> dict:
    bundle = yaml.safe_load(evidence_path.read_text(encoding="utf-8"))
    schema = json.loads(schema_path.read_text(encoding="utf-8"))
    jsonschema.Draft202012Validator(schema).validate(bundle)
    validate_policy(bundle["policy"])
    return bundle


def workstream(bundle: dict, workstream_id: str) -> dict:
    for item in bundle["workstreams"]:
        if item["id"] == workstream_id:
            return item
    raise AutopilotRefusal(f"unknown workstream: {workstream_id}")


def select_next(
    bundle: dict,
    workstream_id: str | None = None,
    include_manual: bool = False,
) -> dict | None:
    candidates = []
    for stream in bundle["workstreams"]:
        if workstream_id and stream["id"] != workstream_id:
            continue
        for criterion in stream["criteria"]:
            if criterion["state"] not in {"OPEN", "PARTIAL"}:
                continue
            if not include_manual and criterion["execution_mode"] != "AUTOMATABLE":
                continue
            candidates.append((
                PRIORITY[criterion["priority"]],
                -criterion["enables"],
                EFFORT[criterion["effort"]],
                stream["id"],
                criterion["id"],
                criterion,
            ))
    if not candidates:
        return None
    _, _, _, stream_id, _, criterion = sorted(candidates)[0]
    selected = deepcopy(criterion)
    selected["workstream"] = stream_id
    return selected


def candidate_proposal(bundle: dict, workstream_id: str) -> dict:
    stream = workstream(bundle, workstream_id)
    incomplete = [
        f"{criterion['id']}={criterion['state']}"
        for criterion in stream["criteria"]
        if criterion["state"] not in {"MET", "WAIVED"}
    ]
    if incomplete:
        raise AutopilotRefusal("gate evidence incomplete: " + ", ".join(incomplete))

    approvals = {approval["authority"]: approval for approval in stream["approvals"]
                 if approval["reviewer_type"] == "HUMAN" and approval["decision"] in {
                     "APPROVED", "APPROVED_WITH_CONDITIONS"
                 }}
    missing = [authority for authority in stream["required_approvers"] if authority not in approvals]

    return {
        "proposal_type": "STAGE_TRANSITION_CANDIDATE",
        "workstream": stream["id"],
        "gate_id": stream["gate_id"],
        "from": stream["current_stage"],
        "to": stream["next_stage"],
        "state": "CANDIDATE",
        "human_approval_required": True,
        "may_mark_passed": False,
        "criteria": [{"id": item["id"], "state": item["state"], "evidence": item["evidence"]}
                     for item in stream["criteria"]],
        "missing_human_approvals": missing,
        "next_action": "request named human verdicts" if missing else "authorised humans may merge a separate state-transition change",
    }


def print_status(bundle: dict) -> None:
    for stream in bundle["workstreams"]:
        counts: dict[str, int] = {}
        for criterion in stream["criteria"]:
            counts[criterion["state"]] = counts.get(criterion["state"], 0) + 1
        summary = " ".join(f"{state}={count}" for state, count in sorted(counts.items()))
        print(f"{stream['id']} {stream['gate_id']} {stream['state']} {summary}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--evidence", type=Path, default=DEFAULT_EVIDENCE)
    parser.add_argument("--schema", type=Path, default=DEFAULT_SCHEMA)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("status")
    next_parser = sub.add_parser("next")
    next_parser.add_argument("--workstream")
    next_parser.add_argument(
        "--include-manual",
        action="store_true",
        help="include human-required and external-required coordination work",
    )
    proposal = sub.add_parser("propose-transition")
    proposal.add_argument("--workstream", required=True)
    proposal.add_argument("--output", type=Path)
    args = parser.parse_args()

    try:
        bundle = load_bundle(args.evidence, args.schema)
        if args.command == "status":
            print_status(bundle)
        elif args.command == "next":
            selected = select_next(bundle, args.workstream, args.include_manual)
            print(yaml.safe_dump(selected, sort_keys=False).strip() if selected else "NO_READY_WORK")
        else:
            result = candidate_proposal(bundle, args.workstream)
            rendered = yaml.safe_dump(result, sort_keys=False)
            if args.output:
                args.output.write_text(rendered, encoding="utf-8")
                print(f"wrote candidate proposal: {args.output}")
            else:
                print(rendered.strip())
    except (AutopilotRefusal, jsonschema.ValidationError) as exc:
        print(f"REFUSED: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
