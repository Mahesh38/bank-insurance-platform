#!/usr/bin/env python3
"""Unit tests for scripts/ci/classify-changes.py — INFRA-001 / PLAN-006."""

from __future__ import annotations

import json
import unittest
from pathlib import Path

import importlib.util

ROOT = Path(__file__).resolve().parents[2]
_SPEC = importlib.util.spec_from_file_location(
    "classify_changes", Path(__file__).resolve().parent / "classify-changes.py"
)
assert _SPEC and _SPEC.loader
cc = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(cc)


MODULES = cc.load_gradle_modules(ROOT)


def c(*files: str, heavy: bool = True, force_all: bool = False) -> dict[str, str]:
    return cc.classify(files, modules=MODULES, force_all=force_all, run_heavy=heavy)


class DocsOnlyTests(unittest.TestCase):
    def test_markdown_under_docs_skips_java_codeql_and_images(self) -> None:
        result = c("docs/governance/01-CURRENT_STATE.md", "docs/context/BOOT.md")
        self.assertEqual(result["docs_only"], "true")
        self.assertEqual(result["run_tests"], "false")
        self.assertEqual(result["run_codeql"], "false")
        self.assertEqual(result["run_sca"], "false")
        self.assertEqual(result["run_image_scan"], "false")
        self.assertEqual(result["run_sbom"], "false")
        self.assertEqual(result["scope"], "none")
        self.assertEqual(result["image_matrix"], "[]")

    def test_service_readme_alone_is_docs(self) -> None:
        result = c("services/lead-service/README.md")
        self.assertEqual(result["docs_only"], "true")
        self.assertEqual(result["run_codeql"], "false")
        self.assertEqual(result["run_image_scan"], "false")

    def test_agents_and_root_readme_are_docs(self) -> None:
        result = c("AGENTS.md", "README.md")
        self.assertEqual(result["docs_only"], "true")
        self.assertEqual(result["run_tests"], "false")


class JavaModuleTests(unittest.TestCase):
    def test_single_service_is_module_scoped(self) -> None:
        result = c("services/lead-service/src/main/java/com/bank/Foo.java")
        self.assertEqual(result["docs_only"], "false")
        self.assertEqual(result["run_tests"], "true")
        self.assertEqual(result["run_codeql"], "true")
        self.assertEqual(result["scope"], "modules")
        self.assertEqual(result["modules"], "services:lead-service")
        self.assertIn(":services:lead-service:test", result["gradle_tasks"])
        self.assertNotIn(":services:quotation-service:", result["gradle_tasks"])
        # lead-service is not a Phase-1 scanned image
        self.assertEqual(result["run_image_scan"], "false")
        self.assertEqual(result["image_matrix"], "[]")

    def test_persistence_java_scans_persistence_and_combined_images(self) -> None:
        result = c(
            "services/bank-persistence-service/src/main/java/com/bank/Entity.java",
            heavy=True,
        )
        self.assertEqual(result["run_codeql"], "true")
        self.assertEqual(result["run_image_scan"], "true")
        images = {row["image"] for row in json.loads(result["image_matrix"])}
        self.assertEqual(images, {"bank-persistence-service", "bank-insurance-combined"})
        self.assertNotIn("1sb-integration-service", images)

    def test_image_scan_deferred_when_not_heavy(self) -> None:
        result = c(
            "services/bank-persistence-service/src/main/java/com/bank/Entity.java",
            heavy=False,
        )
        self.assertEqual(result["run_tests"], "true")
        self.assertEqual(result["run_codeql"], "true")
        self.assertEqual(result["run_image_scan"], "false")
        self.assertEqual(result["run_sbom"], "false")

    def test_shared_lib_fans_out_to_full_build_and_all_images(self) -> None:
        result = c("libs/bank-common-error/src/main/java/com/bank/common/error/Codes.java")
        self.assertEqual(result["scope"], "all")
        self.assertEqual(result["run_tests"], "true")
        self.assertEqual(result["run_codeql"], "true")
        images = {row["image"] for row in json.loads(result["image_matrix"])}
        self.assertEqual(
            images,
            {"bank-persistence-service", "1sb-integration-service", "bank-insurance-combined"},
        )

    def test_root_gradle_is_full_build(self) -> None:
        result = c("build.gradle.kts")
        self.assertEqual(result["scope"], "all")
        self.assertEqual(result["run_tests"], "true")
        self.assertEqual(result["run_codeql"], "true")
        self.assertEqual(result["run_sca"], "true")
        self.assertEqual(result["run_sbom"], "true")

    def test_three_services_collapse_to_full(self) -> None:
        result = c(
            "services/lead-service/src/main/java/A.java",
            "services/consent-service/src/main/java/B.java",
            "services/payment-service/src/main/java/C.java",
        )
        self.assertEqual(result["scope"], "all")


class DockerAndScaTests(unittest.TestCase):
    def test_dockerfile_only_does_not_run_codeql_or_java_tests(self) -> None:
        result = c("services/1sb-integration-service/Dockerfile")
        self.assertEqual(result["run_tests"], "false")
        self.assertEqual(result["run_codeql"], "false")
        self.assertEqual(result["run_image_scan"], "true")
        images = {row["image"] for row in json.loads(result["image_matrix"])}
        self.assertEqual(images, {"1sb-integration-service", "bank-insurance-combined"})

    def test_lockfile_runs_sca_and_sbom_not_codeql(self) -> None:
        result = c("services/lead-service/gradle.lockfile")
        self.assertEqual(result["run_sca"], "true")
        self.assertEqual(result["run_sbom"], "true")
        self.assertEqual(result["run_codeql"], "false")
        self.assertEqual(result["run_tests"], "true")  # lockfile is a build file

    def test_unrelated_service_dockerfile_does_not_scan_phase1_images(self) -> None:
        result = c("services/lead-service/Dockerfile")
        self.assertEqual(result["run_image_scan"], "false")
        self.assertEqual(result["image_matrix"], "[]")
        self.assertEqual(result["run_codeql"], "false")


class WorkflowAndForceTests(unittest.TestCase):
    def test_application_ci_yaml_triggers_full_tests(self) -> None:
        result = c(".github/workflows/application-ci.yml")
        self.assertEqual(result["ci_app"], "true")
        self.assertEqual(result["run_tests"], "true")
        self.assertEqual(result["scope"], "all")

    def test_security_workflow_triggers_security_jobs_not_java_tests(self) -> None:
        result = c(".github/workflows/security-scanning.yml")
        self.assertEqual(result["run_tests"], "false")
        self.assertEqual(result["ci_security"], "true")
        self.assertEqual(result["run_codeql"], "false")
        self.assertEqual(result["run_sca"], "true")
        self.assertEqual(result["run_image_scan"], "true")
        self.assertEqual(result["run_sbom"], "true")
        self.assertEqual(result["assemble_tasks"], "assemble")

    def test_force_all_runs_everything(self) -> None:
        result = c(force_all=True)
        self.assertEqual(result["run_tests"], "true")
        self.assertEqual(result["run_codeql"], "true")
        self.assertEqual(result["run_sca"], "true")
        self.assertEqual(result["run_image_scan"], "true")
        self.assertEqual(result["run_sbom"], "true")
        self.assertEqual(result["scope"], "all")
        self.assertEqual(len(json.loads(result["image_matrix"])), 3)

    def test_mixed_docs_and_java_is_not_docs_only(self) -> None:
        result = c(
            "docs/README.md",
            "services/quotation-service/src/main/java/Q.java",
        )
        self.assertEqual(result["docs_only"], "false")
        self.assertEqual(result["run_codeql"], "true")
        self.assertEqual(result["modules"], "services:quotation-service")


class ModuleInventoryTests(unittest.TestCase):
    def test_settings_modules_are_discovered(self) -> None:
        self.assertIn("services:1sb-integration-service", MODULES)
        self.assertIn("libs:bank-common-error", MODULES)
        self.assertGreaterEqual(len(MODULES), 20)


class WorkflowFailClosedTests(unittest.TestCase):
    """PLAN-006: cancelled classify must not fail required checks (PR #130 race)."""

    def test_required_jobs_fail_closed_only_on_classify_failure(self) -> None:
        for rel in (
            ".github/workflows/application-ci.yml",
            ".github/workflows/security-scanning.yml",
        ):
            text = (ROOT / rel).read_text(encoding="utf-8")
            self.assertNotIn(
                "needs.classify.result != 'success'",
                text,
                f"{rel} still fail-closes on cancelled/skipped classify",
            )
            self.assertIn("needs.classify.result == 'failure'", text)

    def test_image_scan_requires_successful_classify(self) -> None:
        text = (ROOT / ".github/workflows/security-scanning.yml").read_text(encoding="utf-8")
        self.assertIn(
            "needs.classify.result == 'success' && needs.classify.outputs.run_image_scan == 'true'",
            text,
        )


if __name__ == "__main__":
    unittest.main()
