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
DOC_MAP_BUILDER = ROOT / "scripts/context/build-doc-map.py"
CONTEXT_LOAD = ROOT / "scripts/context/context-load.py"


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


class DocumentRoutingTests(unittest.TestCase):
    """The doc map's whole value is that it is COMPLETE and TRUE.

    A map that misses a document sends the agent back to exploring, and a map that
    is stale quotes byte costs that no longer hold — both are worse than no map,
    because the agent trusts them first and fails second."""

    def test_every_tracked_document_is_routed(self) -> None:
        result = run(DOC_MAP_BUILDER, "--check")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

        tracked = subprocess.run(
            ["git", "ls-files", "docs"], cwd=ROOT, capture_output=True, text=True, check=True
        ).stdout.split()
        import yaml  # local: the builder already hard-requires it

        mapped = {
            row["path"]
            for row in yaml.safe_load(
                (ROOT / "docs/context/DOC-MAP.yaml").read_text(encoding="utf-8")
            )["documents"]
        }
        self.assertEqual(set(tracked) - mapped, set(), "documents with no routing row")

    def test_find_locates_a_long_tail_document(self) -> None:
        """A field guide is three link-hops from the index; `find` must reach it in one."""
        result = run(CONTEXT_LOAD, "find", "coverage thresholds", "--limit", "5")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("COVERAGE.md", result.stdout)
        self.assertIn("capsule:", result.stdout)

    def test_find_reports_a_miss_instead_of_guessing(self) -> None:
        result = run(CONTEXT_LOAD, "find", "zzzznotarealtopiczzzz")
        self.assertEqual(1, result.returncode)
        self.assertIn("no document matched", result.stdout)


if __name__ == "__main__":
    unittest.main()
