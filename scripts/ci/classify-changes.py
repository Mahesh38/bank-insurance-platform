#!/usr/bin/env python3
"""Classify a git diff so CI jobs run only for the change they protect.

INFRA-001 / SUG-20260930-cif / PLAN-006.

Required status checks (S08-G2 / T-F01 / A-F05) must still *report*. This script
never skips a workflow; it tells each job whether to do expensive work or to
short-circuit to success.

Outputs (GitHub Actions `name=value` or JSON):

  docs_only, java, build, dependencies, docker, ci_app, ci_security
  run_tests, run_codeql, run_sca, run_image_scan, run_sbom
  scope          all | modules | none
  modules        comma-separated Gradle paths (empty when scope is all/none)
  gradle_tasks   space-separated Gradle task selectors
  codeql_tasks   space-separated :module:classes :module:testClasses selectors
  assemble_tasks space-separated assemble selectors
  image_matrix   JSON array of {image, dockerfile} objects
  reason         one-line explanation
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[2]

ZERO_SHA = "0" * 40

PHASE1_IMAGES: tuple[dict[str, str], ...] = (
    {
        "image": "bank-persistence-service",
        "dockerfile": "services/bank-persistence-service/Dockerfile",
        "module": "services:bank-persistence-service",
    },
    {
        "image": "1sb-integration-service",
        "dockerfile": "services/1sb-integration-service/Dockerfile",
        "module": "services:1sb-integration-service",
    },
    {
        "image": "bank-insurance-combined",
        "dockerfile": "Dockerfile",
        "module": "",
    },
)

DOC_EXACT = {
    "AGENTS.md",
    "CLAUDE.md",
    "README.md",
    "LICENSE",
    "mkdocs.yml",
    "requirements-docs.txt",
}

ROOT_BUILD_FILES = {
    "build.gradle.kts",
    "settings.gradle.kts",
    "gradle.properties",
    "gradlew",
    "gradlew.bat",
}


def load_gradle_modules(root: Path) -> frozenset[str]:
    text = (root / "settings.gradle.kts").read_text(encoding="utf-8")
    return frozenset(re.findall(r'"(libs:[^"]+|services:[^"]+)"', text))


def norm(path: str) -> str:
    p = path.replace("\\", "/")
    while p.startswith("./"):
        p = p[2:]
    return p


def is_doc(path: str) -> bool:
    p = norm(path)
    if p in DOC_EXACT:
        return True
    if p.startswith("docs/") or p.startswith("scripts/docs/") or p.startswith(".claude/"):
        return True
    if p.endswith(".md"):
        return True
    return False


def is_ci_app(path: str) -> bool:
    p = norm(path)
    return p == ".github/workflows/application-ci.yml" or p.startswith(".github/actions/")


def is_ci_security(path: str) -> bool:
    p = norm(path)
    return p in {
        ".github/workflows/security-scanning.yml",
        ".gitleaks.toml",
        ".trivyignore",
    }


def is_docker(path: str) -> bool:
    p = norm(path)
    name = Path(p).name
    return (
        name == "Dockerfile"
        or name.startswith("Dockerfile.")
        or name == ".dockerignore"
        or name.startswith("docker-compose")
    )


def is_build(path: str) -> bool:
    p = norm(path)
    if p in ROOT_BUILD_FILES:
        return True
    if p.startswith("gradle/") or p.startswith("config/"):
        return True
    name = Path(p).name
    return name.endswith(".gradle.kts") or name == "gradle.lockfile" or name == ".editorconfig"


def is_dependency_manifest(path: str) -> bool:
    p = norm(path)
    name = Path(p).name
    return (
        name == "gradle.lockfile"
        or name.endswith(".gradle.kts")
        or name == "libs.versions.toml"
        or p.startswith("gradle/wrapper/")
        or p in {"gradle/wrapper/gradle-wrapper.properties", "gradle.properties"}
    )


def is_java_source(path: str) -> bool:
    p = norm(path)
    if is_doc(p) or is_docker(p):
        return False
    if p.endswith((".java", ".kt")):
        return True
    return "/src/" in f"/{p}"


def is_codeql_input(path: str) -> bool:
    p = norm(path)
    if is_java_source(p):
        return True
    name = Path(p).name
    if name == "gradle.lockfile":
        return False
    if name.endswith(".gradle.kts") or p in ROOT_BUILD_FILES or p.startswith("gradle/") or p.startswith("config/"):
        return True
    return False


def module_for(path: str, modules: frozenset[str]) -> str | None:
    p = norm(path)
    for prefix in ("libs/", "services/"):
        if p.startswith(prefix):
            parts = p.split("/")
            if len(parts) >= 2:
                candidate = f"{parts[0]}:{parts[1]}"
                if candidate in modules:
                    return candidate
    return None


def fans_out_to_all(path: str, modules: frozenset[str]) -> bool:
    p = norm(path)
    if p in ROOT_BUILD_FILES or p.startswith("gradle/") or p.startswith("config/"):
        return True
    if p.startswith("libs/") and not is_doc(p):
        return True
    return False


def git_changed_files(base: str, head: str) -> list[str]:
    if not base or set(base) == {"0"}:
        cmd = ["git", "diff-tree", "--no-commit-id", "--name-only", "-r", head]
    else:
        cmd = ["git", "diff", "--name-only", "--diff-filter=ACDMRT", f"{base}...{head}"]
    result = subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True, check=False)
    if result.returncode != 0:
        raise SystemExit(f"git diff failed: {result.stderr.strip() or result.stdout.strip()}")
    return [norm(line) for line in result.stdout.splitlines() if line.strip()]


def classify(
    files: Iterable[str],
    *,
    modules: frozenset[str] | None = None,
    force_all: bool = False,
    run_heavy: bool = True,
) -> dict[str, object]:
    known = modules if modules is not None else load_gradle_modules(ROOT)
    paths = [norm(p) for p in files]

    if force_all:
        image_matrix = [
            {"image": row["image"], "dockerfile": row["dockerfile"]} for row in PHASE1_IMAGES
        ]
        return _result(
            docs_only=False,
            java=True,
            build=True,
            dependencies=True,
            docker=True,
            ci_app=True,
            ci_security=True,
            run_tests=True,
            run_codeql=True,
            run_sca=True,
            run_image_scan=True,
            run_sbom=True,
            scope="all",
            module_list=[],
            image_matrix=image_matrix,
            reason="forced full run (schedule, workflow_dispatch, or empty/unknown diff)",
        )

    if not paths:
        return _result(
            docs_only=True,
            java=False,
            build=False,
            dependencies=False,
            docker=False,
            ci_app=False,
            ci_security=False,
            run_tests=False,
            run_codeql=False,
            run_sca=False,
            run_image_scan=False,
            run_sbom=False,
            scope="none",
            module_list=[],
            image_matrix=[],
            reason="empty diff — nothing to compile or scan",
        )

    docs = all(is_doc(p) for p in paths)
    java_source = any(is_java_source(p) for p in paths)
    build = any(is_build(p) for p in paths)
    dependencies = any(is_dependency_manifest(p) for p in paths)
    docker = any(is_docker(p) for p in paths)
    ci_app = any(is_ci_app(p) for p in paths)
    ci_security = any(is_ci_security(p) for p in paths)
    needs_codeql = any(is_codeql_input(p) for p in paths)

    changed_modules: set[str] = set()
    full = False
    for p in paths:
        if fans_out_to_all(p, known):
            full = True
        mod = module_for(p, known)
        if mod and (is_java_source(p) or is_build(p)):
            changed_modules.add(mod)
        if is_ci_app(p) and p.endswith("application-ci.yml"):
            full = True

    run_tests = java_source or build or any(p.endswith("application-ci.yml") for p in paths)
    if run_tests and (full or len(changed_modules) >= 3 or not changed_modules):
        scope = "all"
        module_list: list[str] = []
    elif run_tests and changed_modules:
        scope = "modules"
        module_list = sorted(changed_modules)
    else:
        scope = "none"
        module_list = []

    run_codeql = needs_codeql
    run_sca = dependencies or ci_security
    run_sbom = (dependencies or ci_security) and run_heavy

    images: list[dict[str, str]] = []
    need_all_images = any(fans_out_to_all(p, known) for p in paths) or ci_security
    if need_all_images and (java_source or docker or dependencies or ci_security):
        images = [{"image": row["image"], "dockerfile": row["dockerfile"]} for row in PHASE1_IMAGES]
    else:
        wanted: set[str] = set()
        for p in paths:
            if p == "Dockerfile" or p == ".dockerignore":
                wanted.add("bank-insurance-combined")
            for row in PHASE1_IMAGES:
                module_dir = row["module"].replace(":", "/")
                if row["dockerfile"] == p or (module_dir and (p == module_dir or p.startswith(module_dir + "/"))):
                    wanted.add(row["image"])
                    if row["image"] != "bank-insurance-combined":
                        wanted.add("bank-insurance-combined")
        images = [
            {"image": row["image"], "dockerfile": row["dockerfile"]}
            for row in PHASE1_IMAGES
            if row["image"] in wanted
        ]

    run_image_scan = bool(images) and run_heavy and (docker or java_source or dependencies or ci_security)

    if docs and not (java_source or build or docker or ci_app or ci_security or dependencies):
        reason = "documentation-only diff — skip Java tests, CodeQL, SCA, image scan and SBOM"
        return _result(
            docs_only=True,
            java=False,
            build=False,
            dependencies=False,
            docker=False,
            ci_app=ci_app,
            ci_security=ci_security,
            run_tests=False,
            run_codeql=False,
            run_sca=False,
            run_image_scan=False,
            run_sbom=False,
            scope="none",
            module_list=[],
            image_matrix=[],
            reason=reason,
        )

    reason_parts = []
    if docs and (java_source or docker or build):
        reason_parts.append("mixed docs + code")
    if scope == "modules":
        reason_parts.append("module-scoped Gradle: " + ",".join(module_list))
    elif scope == "all" and run_tests:
        reason_parts.append("full Gradle (shared/root/CI change or ≥3 services)")
    if run_codeql:
        reason_parts.append("CodeQL")
    else:
        reason_parts.append("no CodeQL")
    if run_image_scan:
        reason_parts.append("image-scan " + ",".join(i["image"] for i in images))
    else:
        reason_parts.append("no image-scan")
    if not run_heavy and (images or dependencies):
        reason_parts.append("heavy scans deferred to pre-merge/main/schedule")

    return _result(
        docs_only=False,
        java=java_source,
        build=build,
        dependencies=dependencies,
        docker=docker,
        ci_app=ci_app,
        ci_security=ci_security,
        run_tests=run_tests,
        run_codeql=run_codeql,
        run_sca=run_sca,
        run_image_scan=run_image_scan,
        run_sbom=run_sbom,
        scope=scope,
        module_list=module_list,
        image_matrix=images,
        reason="; ".join(reason_parts) if reason_parts else "classified",
    )


def _gradle_tasks(scope: str, module_list: list[str]) -> str:
    if scope == "none":
        return ""
    if scope == "all":
        return "build test jacocoTestReport jacocoTestCoverageVerification"
    names = (
        "build",
        "test",
        "jacocoTestReport",
        "jacocoTestCoverageVerification",
        "checkstyleMain",
        "checkstyleTest",
        "spotlessCheck",
    )
    return " ".join(f":{mod}:{name}" for mod in module_list for name in names)


def _codeql_tasks(scope: str, module_list: list[str]) -> str:
    if scope == "none":
        return ""
    if scope == "all":
        return "classes testClasses"
    return " ".join(f":{mod}:{name}" for mod in module_list for name in ("classes", "testClasses"))


def _assemble_tasks(scope: str, module_list: list[str], *, run_sca: bool = False) -> str:
    if scope == "all":
        return "assemble"
    if scope == "modules":
        return " ".join(f":{mod}:assemble" for mod in module_list)
    if run_sca:
        return "assemble"
    return ""


def _result(
    *,
    docs_only: bool,
    java: bool,
    build: bool,
    dependencies: bool,
    docker: bool,
    ci_app: bool,
    ci_security: bool,
    run_tests: bool,
    run_codeql: bool,
    run_sca: bool,
    run_image_scan: bool,
    run_sbom: bool,
    scope: str,
    module_list: list[str],
    image_matrix: list[dict[str, str]],
    reason: str,
) -> dict[str, object]:
    return {
        "docs_only": _b(docs_only),
        "java": _b(java),
        "build": _b(build),
        "dependencies": _b(dependencies),
        "docker": _b(docker),
        "ci_app": _b(ci_app),
        "ci_security": _b(ci_security),
        "run_tests": _b(run_tests),
        "run_codeql": _b(run_codeql),
        "run_sca": _b(run_sca),
        "run_image_scan": _b(run_image_scan),
        "run_sbom": _b(run_sbom),
        "scope": scope,
        "modules": ",".join(module_list),
        "gradle_tasks": _gradle_tasks(scope, module_list),
        "codeql_tasks": _codeql_tasks(scope, module_list),
        "assemble_tasks": _assemble_tasks(scope, module_list, run_sca=run_sca),
        "image_matrix": json.dumps(image_matrix, separators=(",", ":")),
        "reason": reason,
    }


def _b(value: bool) -> str:
    return "true" if value else "false"


def emit_github(result: dict[str, object], sink) -> None:
    for key, value in result.items():
        text = str(value).replace("\n", " ").replace("\r", "")
        sink.write(f"{key}={text}\n")


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="", help="git base SHA (PR base or push.before)")
    parser.add_argument("--head", default="HEAD", help="git head SHA")
    parser.add_argument("--files", nargs="*", help="explicit file list (skips git)")
    parser.add_argument("--force-all", action="store_true")
    parser.add_argument(
        "--heavy",
        choices=("true", "false"),
        default="true",
        help="false defers image-scan/SBOM (feature-branch noise); required checks still classify",
    )
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--github-output", action="store_true")
    parser.add_argument("--root", default=str(ROOT))
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    root = Path(args.root)
    known = load_gradle_modules(root)
    force_all = args.force_all
    if args.files is not None and args.files:
        files = args.files
    elif force_all:
        files = []
    else:
        files = git_changed_files(args.base, args.head)
        if not files and (not args.base or args.base == ZERO_SHA):
            force_all = True
    result = classify(
        files,
        modules=known,
        force_all=force_all,
        run_heavy=args.heavy == "true",
    )
    if args.json:
        json.dump(result, sys.stdout, indent=2)
        sys.stdout.write("\n")
    elif args.github_output:
        emit_github(result, sys.stdout)
    else:
        for key, value in result.items():
            print(f"{key}={value}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
