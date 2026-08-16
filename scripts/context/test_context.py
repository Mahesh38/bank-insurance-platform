#!/usr/bin/env python3
"""Regression tests for portable context scaffolding and validation."""

from __future__ import annotations

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCAFFOLD = ROOT / "scripts/context/new-project-context.py"


def run(*args: object, cwd: Path = ROOT) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, *(str(arg) for arg in args)],
        cwd=cwd,
        capture_output=True,
        text=True,
        check=False,
    )


class PortableContextTests(unittest.TestCase):
    def test_scaffold_is_repository_relative_and_valid(self) -> None:
        with tempfile.TemporaryDirectory(prefix="context-module-") as temporary:
            repository = Path(temporary) / "claims-platform"
            output = repository / "docs/context"
            scaffold = run(
                SCAFFOLD,
                "--id", "claims-modernisation",
                "--name", "Claims Modernisation",
                "--domain", "property and casualty claims",
                "--repository-root", repository,
                "--output", output,
            )
            self.assertEqual(0, scaffold.returncode, scaffold.stderr)

            manifest = output / "context-manifest.yaml"
            rendered = manifest.read_text(encoding="utf-8")
            self.assertIn('problem_statement: "docs/context/problem-statement.md"', rendered)
            self.assertNotIn(Path(temporary).as_posix(), rendered)
            copied_validator = repository / "scripts/context/validate-context.py"
            copied_schema = output / "schemas/context-manifest.schema.json"
            self.assertTrue(copied_validator.is_file())
            self.assertTrue((repository / "scripts/context/new-project-context.py").is_file())
            self.assertTrue(copied_schema.is_file())

            validation = run(
                copied_validator,
                "--manifest", manifest,
                "--schema", copied_schema,
                "--repository-root", repository,
                cwd=repository,
            )
            self.assertEqual(0, validation.returncode, validation.stdout + validation.stderr)
            self.assertIn("CONTEXT VALID", validation.stdout)

    def test_scaffold_refuses_output_outside_repository(self) -> None:
        with tempfile.TemporaryDirectory(prefix="context-boundary-") as temporary:
            base = Path(temporary)
            result = run(
                SCAFFOLD,
                "--id", "boundary-test",
                "--name", "Boundary Test",
                "--domain", "test",
                "--repository-root", base / "repository",
                "--output", base / "outside/context",
            )
            self.assertNotEqual(0, result.returncode)
            self.assertIn("output must be inside repository root", result.stderr)


if __name__ == "__main__":
    unittest.main()
