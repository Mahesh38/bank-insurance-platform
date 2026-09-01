#!/usr/bin/env python3
"""Refuse a GitLab import that still carries personal-forge or AI-vendor identity.

CR-017 / ADR-020 / AC-6. Stdlib only.

Exit 0 = clean. Exit 1 = hits. Exit 2 = usage / IO error.
"""
from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path

TREE_PATTERNS: list[tuple[str, re.Pattern[str]]] = [
    ("personal-gmail", re.compile(r"mh\.narkar@gmail\.com|mh,narkar@gmail\.com", re.I)),
    ("personal-github-login", re.compile(r"\bmahesh38\b", re.I)),
    ("anthropic-noreply", re.compile(r"noreply@anthropic\.com", re.I)),
    ("claude-session", re.compile(r"Claude-Session:|claude\.ai/code/session", re.I)),
    ("cursor-agent", re.compile(r"cursoragent@cursor\.com|cursor\.com/agents", re.I)),
    ("github-noreply-user", re.compile(r"users\.noreply\.github\.com", re.I)),
    ("claude-coauthor", re.compile(r"Co-Authored-By:\s*Claude|Generated with Claude", re.I)),
]

GIT_EMAIL_FORBIDDEN = re.compile(
    r"gmail\.com|anthropic\.com|cursor\.com|users\.noreply\.github\.com|"
    r"noreply@github\.com",
    re.I,
)
GIT_NAME_FORBIDDEN = re.compile(r"^(Claude|Cursor Agent|GitHub)$", re.I)
GIT_MESSAGE_FORBIDDEN = re.compile(
    r"Merge pull request|Claude-Session:|Co-Authored-By:\s*Claude|"
    r"noreply@anthropic\.com|cursoragent@|\bmahesh38\b|Generated with Claude",
    re.I,
)

SKIP_DIR_NAMES = {
    ".git",
    "node_modules",
    "build",
    ".gradle",
    "__pycache__",
}
SKIP_FILE_NAMES = {
    "identity-guard.py",
    "identity_guard.py",
    "test_identity_guard.py",
}
SKIP_SUFFIXES = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".woff", ".woff2", ".pdf", ".pyc"}


def _iter_text_files(root: Path) -> list[Path]:
    files: list[Path] = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIR_NAMES]
        for name in filenames:
            path = Path(dirpath) / name
            if path.name in SKIP_FILE_NAMES:
                continue
            if path.suffix.lower() in SKIP_SUFFIXES:
                continue
            files.append(path)
    return files


def scan_tree(root: Path) -> list[str]:
    hits: list[str] = []
    for path in _iter_text_files(root):
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except OSError as exc:
            hits.append(f"unreadable {path}: {exc}")
            continue
        rel = path.relative_to(root)
        for label, pattern in TREE_PATTERNS:
            if pattern.search(text):
                hits.append(f"tree:{label}:{rel}")
    return hits


def scan_git(repo: Path) -> list[str]:
    hits: list[str] = []
    try:
        log = subprocess.check_output(
            ["git", "-C", str(repo), "log", "--format=%an <%ae>%n%cn <%ce>%n%s%n%b"],
            text=True,
            stderr=subprocess.STDOUT,
        )
    except (subprocess.CalledProcessError, FileNotFoundError) as exc:
        return [f"git-log-failed:{exc}"]
    for i, line in enumerate(log.splitlines(), start=1):
        if GIT_EMAIL_FORBIDDEN.search(line) or GIT_MESSAGE_FORBIDDEN.search(line):
            hits.append(f"git:{i}:{line[:200]}")
            continue
        # "%an <%ae>" / "%cn <%ce>" — reject vendor display names even on a bank email.
        name = line.split(" <", 1)[0].strip()
        if name and GIT_NAME_FORBIDDEN.search(name):
            hits.append(f"git:{i}:forbidden-name:{name}")
    return hits


def validate_company_identity(name: str | None, email: str | None) -> list[str]:
    hits: list[str] = []
    if not name or not name.strip():
        hits.append("company-identity:COMPANY_GIT_NAME is empty")
    if not email or "@" not in (email or ""):
        hits.append("company-identity:COMPANY_GIT_EMAIL is missing or has no @")
        return hits
    if GIT_EMAIL_FORBIDDEN.search(email):
        hits.append(f"company-identity:forbidden-domain:{email}")
    domain = os.environ.get("COMPANY_EMAIL_DOMAIN", "").strip().lower()
    if domain and not email.lower().endswith("@" + domain.lstrip("@")):
        hits.append(f"company-identity:email-not-in-COMPANY_EMAIL_DOMAIN ({domain})")
    return hits


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tree", type=Path, help="Working tree to scan")
    parser.add_argument("--git", type=Path, help="Git repository whose log to scan")
    parser.add_argument(
        "--company-identity",
        action="store_true",
        help="Validate COMPANY_GIT_NAME / COMPANY_GIT_EMAIL from the environment",
    )
    args = parser.parse_args(argv)
    if not args.tree and not args.git and not args.company_identity:
        parser.error("specify --tree, --git and/or --company-identity")

    hits: list[str] = []
    if args.company_identity:
        hits.extend(
            validate_company_identity(
                os.environ.get("COMPANY_GIT_NAME"),
                os.environ.get("COMPANY_GIT_EMAIL"),
            )
        )
    if args.tree:
        if not args.tree.is_dir():
            print(f"not a directory: {args.tree}", file=sys.stderr)
            return 2
        hits.extend(scan_tree(args.tree))
    if args.git:
        if not (args.git / ".git").exists() and not (args.git / "HEAD").exists():
            print(f"not a git repo: {args.git}", file=sys.stderr)
            return 2
        hits.extend(scan_git(args.git))

    if hits:
        print(f"{len(hits)} identity hit(s):", file=sys.stderr)
        for hit in hits:
            print(f"  {hit}", file=sys.stderr)
        return 1
    print("identity-guard: clean")
    return 0


if __name__ == "__main__":
    sys.exit(main())
