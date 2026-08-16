#!/usr/bin/env python3
"""Scaffold a project-specific instance of the portable context framework."""

from __future__ import annotations

import argparse
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TEMPLATE = ROOT / "docs/context/framework/templates/project"
FRAMEWORK = ROOT / "docs/context/framework"
SCHEMA = ROOT / "docs/context/schemas/context-manifest.schema.json"
TOOLS = (
    ROOT / "scripts/context/new-project-context.py",
    ROOT / "scripts/context/validate-context.py",
)


def project_id(value: str) -> str:
    if not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", value):
        raise argparse.ArgumentTypeError("use lowercase kebab-case")
    return value


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--id", required=True, type=project_id)
    parser.add_argument("--name", required=True)
    parser.add_argument("--domain", required=True)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument(
        "--repository-root",
        type=Path,
        default=Path.cwd(),
        help="repository root used to create portable relative manifest paths (default: cwd)",
    )
    args = parser.parse_args()

    repository_root = args.repository_root.resolve()
    output = args.output.resolve()
    try:
        context_path = output.relative_to(repository_root).as_posix()
    except ValueError:
        parser.error(
            f"output must be inside repository root {repository_root}; "
            "use --repository-root for another repository"
        )
    if output.exists() and any(output.iterdir()):
        parser.error(f"output must not already contain files: {output}")

    tool_directory = repository_root / "scripts/context"
    for source in TOOLS:
        destination = tool_directory / source.name
        if source.resolve() == destination.resolve():
            continue
        if destination.exists() and destination.read_bytes() != source.read_bytes():
            parser.error(f"refusing to overwrite a different context tool: {destination}")

    output.mkdir(parents=True, exist_ok=True)

    replacements = {
        "{{PROJECT_ID}}": args.id,
        "{{PROJECT_NAME}}": args.name,
        "{{PROJECT_DOMAIN}}": args.domain,
        "{{CONTEXT_PATH}}": context_path,
    }

    for source in TEMPLATE.rglob("*"):
        if not source.is_file():
            continue
        destination = output / source.relative_to(TEMPLATE)
        destination.parent.mkdir(parents=True, exist_ok=True)
        text = source.read_text(encoding="utf-8")
        for marker, value in replacements.items():
            text = text.replace(marker, value)
        destination.write_text(text, encoding="utf-8")

    shutil.copytree(FRAMEWORK, output / "framework", dirs_exist_ok=True)
    schema_directory = output / "schemas"
    schema_directory.mkdir(parents=True, exist_ok=True)
    shutil.copy2(SCHEMA, schema_directory / SCHEMA.name)
    tool_directory.mkdir(parents=True, exist_ok=True)
    for source in TOOLS:
        destination = tool_directory / source.name
        if source.resolve() != destination.resolve():
            shutil.copy2(source, destination)
    print(f"created context module at {output}")
    print(f"next: edit {output / 'problem-statement.md'}")
    print(f"validate: {tool_directory / 'validate-context.py'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
