#!/usr/bin/env python3
"""Generate a standard Spring Boot microservice skeleton from the service catalogue.

Usage:
  python3 scripts/scaffold/create-microservice.py --all-skeletons
  python3 scripts/scaffold/create-microservice.py --module customer-service
  python3 scripts/scaffold/create-microservice.py --list

Catalogue: docs/platform/engineering/backend-service-catalog.yaml
Template:  templates/microservice-skeleton/
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

try:
    import yaml
except ImportError:
    print("PyYAML is required. Install with: pip install pyyaml", file=sys.stderr)
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[2]
CATALOGUE = ROOT / "docs/platform/engineering/backend-service-catalog.yaml"
TEMPLATE_DIR = ROOT / "templates/microservice-skeleton"
SERVICES_DIR = ROOT / "services"
SETTINGS = ROOT / "settings.gradle.kts"


def load_catalogue() -> list[dict]:
    with CATALOGUE.open(encoding="utf-8") as handle:
        data = yaml.safe_load(handle)
    return data["services"]


def package_to_path(package: str) -> str:
    return package.replace(".", "/")


def render(template: str, mapping: dict[str, str]) -> str:
    result = template
    for key, value in mapping.items():
        result = result.replace(f"{{{{{key}}}}}", value)
    missing = re.findall(r"\{\{(\w+)\}\}", result)
    if missing:
        raise KeyError(f"Unresolved template placeholders: {missing}")
    return result


def service_exists(module: str) -> bool:
    return (SERVICES_DIR / module).is_dir()


def write_file(path: Path, content: str, *, force: bool = False) -> bool:
    if path.exists() and not force:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    return True


def build_mapping(entry: dict) -> dict[str, str]:
    java_package = f"com.bank.{entry['package']}"
    boundary = entry.get("boundary", "INTERNAL")
    persistence = entry.get("persistence", "jpa")
    env_prefix = entry["module"].upper().replace("-", "_")
    return {
        "MODULE": entry["module"],
        "SERVICE_NAME": entry["name"],
        "CONTEXT_ID": entry["context_id"],
        "PACKAGE": java_package,
        "PACKAGE_PATH": package_to_path(java_package),
        "APPLICATION_CLASS": entry["application_class"],
        "SERVICE_ID": entry["service_id"],
        "PORT": str(entry["port"]),
        "LAYER": entry["layer"],
        "BOUNDARY": boundary,
        "GITLAB_GROUP": entry["gitlab_group"],
        "DATASTORE": entry.get("datastore", "TBD"),
        "ENV_PREFIX": env_prefix,
        "PERSISTENCE": persistence,
        "SCHEMA_NAME": entry["module"].replace("-", "_"),
    }


def generate_service(entry: dict, *, force: bool = False) -> list[Path]:
    if service_exists(entry["module"]) and not force:
        print(f"skip (exists): {entry['module']}")
        return []

    mapping = build_mapping(entry)
    created: list[Path] = []

    for template_path in sorted(TEMPLATE_DIR.rglob("*")):
        if template_path.is_dir():
            continue
        rel = template_path.relative_to(TEMPLATE_DIR)
        rel_str = str(rel)
        if mapping["PERSISTENCE"] == "stateless" and "db/migration" in rel_str:
            continue
        if mapping["PERSISTENCE"] == "stateless" and rel.name.endswith(".jpa"):
            continue
        if mapping["PERSISTENCE"] == "jpa" and rel.name.endswith(".stateless"):
            continue

        target_rel = rel_str
        if target_rel.endswith(".jpa") or target_rel.endswith(".stateless"):
            target_rel = target_rel.rsplit(".", 1)[0]

        content = render(template_path.read_text(encoding="utf-8"), mapping)
        target_rel = render(target_rel, mapping)
        target = SERVICES_DIR / mapping["MODULE"] / target_rel
        if write_file(target, content, force=force):
            created.append(target)

    return created


def sync_settings(entries: list[dict]) -> None:
    modules = sorted(
        f'services:{e["module"]}'
        for e in entries
        if (SERVICES_DIR / e["module"]).is_dir()
    )
    libs = [
        "libs:bank-common-error",
        "libs:bank-common-security",
        "libs:bank-common-audit",
        "libs:bank-common-observability",
        "libs:bank-common-secrets",
    ]
    all_modules = libs + modules
    body = ",\n    ".join(f'"{m}"' for m in all_modules)
    content = f'''rootProject.name = "1sb-insurance-platform"

include(
    {body}
)
'''
    SETTINGS.write_text(content, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--module", help="Generate one module by name")
    parser.add_argument("--all-skeletons", action="store_true", help="Generate all catalogue entries marked skeleton")
    parser.add_argument("--list", action="store_true", help="List catalogue entries")
    parser.add_argument("--sync-settings", action="store_true", help="Rewrite settings.gradle.kts from catalogue + existing dirs")
    parser.add_argument("--force", action="store_true", help="Overwrite existing files")
    args = parser.parse_args()

    entries = load_catalogue()

    if args.list:
        for entry in entries:
            flag = "EXISTS" if service_exists(entry["module"]) else entry.get("status", "?")
            print(f"{entry['context_id']:>14}  {entry['module']:<40} {flag}")
        return 0

    targets: list[dict] = []
    if args.all_skeletons:
        targets = [e for e in entries if e.get("status") == "skeleton"]
    elif args.module:
        match = [e for e in entries if e["module"] == args.module]
        if not match:
            print(f"Unknown module: {args.module}", file=sys.stderr)
            return 1
        targets = match
    elif args.sync_settings:
        sync_settings(entries)
        print(f"Updated {SETTINGS}")
        return 0
    else:
        parser.print_help()
        return 1

    total = 0
    for entry in targets:
        module_dir = SERVICES_DIR / entry["module"]
        if module_dir.exists() and args.force:
            import shutil
            shutil.rmtree(module_dir)
        created = generate_service(entry, force=args.force)
        if created:
            print(f"created {entry['module']} ({len(created)} files)")
            total += len(created)
        else:
            print(f"unchanged {entry['module']}")

    sync_settings(entries)
    print(f"Done. {total} files written. settings.gradle.kts synced.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
