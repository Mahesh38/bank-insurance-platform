#!/usr/bin/env python3
"""Validate a portable context module manifest and every referenced repository path."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

try:
    import jsonschema
    import yaml
except ImportError as exc:  # pragma: no cover
    print(f"context validation needs PyYAML and jsonschema: {exc}", file=sys.stderr)
    print("  pip install pyyaml jsonschema", file=sys.stderr)
    raise SystemExit(2)

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_MANIFEST = ROOT / "docs/context/context-manifest.yaml"
DEFAULT_SCHEMA = ROOT / "docs/context/schemas/context-manifest.schema.json"


def resolve_repo_path(value: str, root: Path) -> Path:
    path = Path(value)
    if path.is_absolute() or ".." in path.parts:
        raise ValueError(f"path must be repository-root relative: {value}")
    return root / path


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--schema", type=Path, default=DEFAULT_SCHEMA)
    parser.add_argument(
        "--root",
        "--repository-root",
        dest="root",
        type=Path,
        default=ROOT,
        help="repository root used to resolve manifest paths",
    )
    args = parser.parse_args()

    root = args.root.resolve()
    manifest_path = args.manifest.resolve()
    schema_path = args.schema.resolve()
    manifest = yaml.safe_load(manifest_path.read_text(encoding="utf-8"))
    schema = json.loads(schema_path.read_text(encoding="utf-8"))

    errors = sorted(
        jsonschema.Draft202012Validator(schema).iter_errors(manifest),
        key=lambda error: list(error.path),
    )
    failures = [f"schema {list(error.path)}: {error.message}" for error in errors]

    def require_path(label: str, value: str) -> None:
        try:
            target = resolve_repo_path(value, root)
        except ValueError as exc:
            failures.append(f"{label}: {exc}")
            return
        if not target.exists():
            failures.append(f"{label}: missing {value}")

    project = manifest.get("project") or {}
    authority = manifest.get("authority") or {}
    require_path("project.problem_statement", project.get("problem_statement", ""))
    require_path("authority.canonical_authority", authority.get("canonical_authority", ""))
    for index, value in enumerate(authority.get("precedence") or []):
        require_path(f"authority.precedence[{index}]", value)

    layer_ids: set[str] = set()
    for index, layer in enumerate(manifest.get("layers") or []):
        layer_id = layer.get("id", "")
        if layer_id in layer_ids:
            failures.append(f"duplicate layer id: {layer_id}")
        layer_ids.add(layer_id)
        require_path(f"layers[{index}].path", layer.get("path", ""))

    role_ids: set[str] = set()
    aliases: dict[str, str] = {}
    for index, role in enumerate(manifest.get("roles") or []):
        role_id = role.get("id", "")
        if role_id in role_ids:
            failures.append(f"duplicate role id: {role_id}")
        role_ids.add(role_id)
        require_path(f"roles[{index}].package", role.get("package", ""))
        require_path(f"roles[{index}].decision_ref", role.get("decision_ref", ""))
        for alias in role.get("aliases") or []:
            key = alias.casefold()
            previous = aliases.get(key)
            if previous and previous != role_id:
                failures.append(f"alias {alias!r} belongs to both {previous} and {role_id}")
            aliases[key] = role_id

    profile_ids: set[str] = set()
    for profile in manifest.get("loading_profiles") or []:
        profile_id = profile.get("id", "")
        if profile_id in profile_ids:
            failures.append(f"duplicate loading profile id: {profile_id}")
        profile_ids.add(profile_id)
        for reference in profile.get("include") or []:
            kind, target = reference.split(":", 1)
            known = layer_ids if kind == "layer" else role_ids
            if target not in known:
                failures.append(f"profile {profile_id}: unknown {reference}")

    portable = manifest.get("portability") or {}
    require_path("portability.scaffold_template", portable.get("scaffold_template", ""))

    if failures:
        print("CONTEXT INVALID")
        for failure in failures:
            print(f"  - {failure}")
        return 1

    print(
        "CONTEXT VALID — "
        f"project={project.get('id')} layers={len(layer_ids)} roles={len(role_ids)} "
        f"profiles={len(profile_ids)} aliases={len(aliases)}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
