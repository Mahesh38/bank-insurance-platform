"""M5.2 preflight and empty-remote push guards — CR-017 / AC-6."""
from __future__ import annotations

import os
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "migrate-repositories.sh"

COMPANY = {
    "COMPANY_GIT_NAME": "Platform Engineer",
    "COMPANY_GIT_EMAIL": "dev@au.bank.in",
}

_MIGRATE_KEYS = {
    "REHEARSE",
    "PUSH",
    "PREFLIGHT",
    "SRC",
    "OUT",
    "SEALED",
    "EVIDENCE_DIR",
    "GITLAB_FRONTEND_URL",
    "GITLAB_BACKEND_URL",
    "GITLAB_GOVERNANCE_URL",
}


def _run(env: dict[str, str]) -> subprocess.CompletedProcess[str]:
    base = {k: v for k, v in os.environ.items() if k not in _MIGRATE_KEYS}
    return subprocess.run(
        ["bash", str(SCRIPT)],
        capture_output=True,
        text=True,
        env={**base, **env},
    )


def _git(cwd: Path, *args: str, env: dict[str, str] | None = None) -> None:
    subprocess.check_call(
        ["git", *args],
        cwd=cwd,
        env=env or os.environ,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )


def _company_env() -> dict[str, str]:
    return {
        **os.environ,
        "GIT_AUTHOR_NAME": COMPANY["COMPANY_GIT_NAME"],
        "GIT_AUTHOR_EMAIL": COMPANY["COMPANY_GIT_EMAIL"],
        "GIT_COMMITTER_NAME": COMPANY["COMPANY_GIT_NAME"],
        "GIT_COMMITTER_EMAIL": COMPANY["COMPANY_GIT_EMAIL"],
    }


def _seed_src(tmp: Path) -> Path:
    origin = tmp / "origin.git"
    subprocess.check_call(["git", "init", "--bare", str(origin)], stdout=subprocess.DEVNULL)
    src = tmp / "src"
    for rel in (
        "apps/rm-workspace-app",
        "services/demo",
        "libs/demo",
        "docs/ok",
        "scripts",
    ):
        (src / rel).mkdir(parents=True)
    (src / "apps/rm-workspace-app/pubspec.yaml").write_text("name: rm\n")
    (src / "services/demo/README.md").write_text("svc\n")
    (src / "libs/demo/README.md").write_text("lib\n")
    (src / "docs/ok/note.md").write_text("# governance\n")
    (src / "scripts/hello.sh").write_text("echo ok\n")
    (src / "AGENTS.md").write_text("# agents\n")
    _git(src.parent, "init", str(src))
    _git(src, "add", "-A")
    _git(src, "-c", "commit.gpgsign=false", "commit", "-m", "seed", env=_company_env())
    _git(src, "remote", "add", "origin", str(origin))
    _git(src, "push", "origin", "HEAD:main")
    _git(src, "tag", "pre-gitlab-migration")
    _git(src, "push", "origin", "refs/tags/pre-gitlab-migration")
    return src


def _evidence(tmp: Path) -> Path:
    ev = tmp / "evidence"
    ev.mkdir()
    (ev / "C-SEC-1.signed").write_text("signed\n")
    (ev / "C-CMP-1.signed").write_text("signed\n")
    (ev / "finding-B.resolved").write_text("retired\n")
    return ev


def _bare(tmp: Path, name: str, nonempty: bool = False) -> Path:
    path = tmp / f"{name}.git"
    subprocess.check_call(["git", "init", "--bare", str(path)], stdout=subprocess.DEVNULL)
    if nonempty:
        work = tmp / f"{name}-work"
        work.mkdir()
        _git(work.parent, "init", str(work))
        (work / "x").write_text("old\n")
        _git(work, "add", "x")
        _git(work, "-c", "commit.gpgsign=false", "commit", "-m", "existing", env=_company_env())
        _git(work, "remote", "add", "origin", str(path))
        _git(work, "push", "origin", "HEAD:main")
    return path


class M52PreflightTests(unittest.TestCase):
    def test_preflight_cannot_push(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            proc = _run(
                {
                    "SRC": tmp,
                    "PREFLIGHT": "1",
                    "PUSH": "1",
                    "REHEARSE": "0",
                    **COMPANY,
                }
            )
        self.assertEqual(proc.returncode, 1)
        self.assertIn("cannot PUSH", proc.stdout + proc.stderr)

    def test_preflight_cannot_rehearse(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            proc = _run(
                {
                    "SRC": tmp,
                    "PREFLIGHT": "1",
                    "REHEARSE": "1",
                    **COMPANY,
                }
            )
        self.assertEqual(proc.returncode, 1)
        self.assertIn("cannot combine with REHEARSE", proc.stdout + proc.stderr)

    def test_preflight_stops_without_signed_gates(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            src = Path(tmp) / "src"
            src.mkdir()
            _git(src.parent, "init", str(src))
            (src / "README.md").write_text("x\n")
            _git(src, "add", "README.md")
            _git(src, "-c", "commit.gpgsign=false", "commit", "-m", "seed", env=_company_env())
            proc = _run(
                {
                    "SRC": str(src),
                    "PREFLIGHT": "1",
                    "REHEARSE": "0",
                    "EVIDENCE_DIR": str(Path(tmp) / "none"),
                    **COMPANY,
                }
            )
        self.assertEqual(proc.returncode, 1)
        combined = proc.stdout + proc.stderr
        self.assertIn("C-SEC-1", combined)
        self.assertIn("REFUSING TO RUN", combined)

    def test_preflight_passes_when_gates_present(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            src = _seed_src(root)
            ev = _evidence(root)
            proc = _run(
                {
                    "SRC": str(src),
                    "PREFLIGHT": "1",
                    "EVIDENCE_DIR": str(ev),
                    **COMPANY,
                }
            )
        self.assertEqual(proc.returncode, 0, proc.stdout + proc.stderr)
        self.assertIn("PREFLIGHT PASS", proc.stdout + proc.stderr)

    def test_push_refuses_nonempty_remote(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            src = _seed_src(root)
            ev = _evidence(root)
            fe = _bare(root, "frontend")
            be = _bare(root, "backend", nonempty=True)
            gov = _bare(root, "gov")
            proc = _run(
                {
                    "SRC": str(src),
                    "OUT": str(root / "out"),
                    "PUSH": "1",
                    "REHEARSE": "0",
                    "EVIDENCE_DIR": str(ev),
                    "GITLAB_FRONTEND_URL": f"file://{fe}",
                    "GITLAB_BACKEND_URL": f"file://{be}",
                    "GITLAB_GOVERNANCE_URL": f"file://{gov}",
                    **COMPANY,
                }
            )
            self.assertEqual(proc.returncode, 1, proc.stdout + proc.stderr)
            self.assertIn("not empty", proc.stdout + proc.stderr)
            self.assertTrue(be.exists(), f"bare missing: {be}")
            count = subprocess.check_output(
                ["git", "--git-dir", str(be), "rev-list", "--count", "refs/heads/main"],
                text=True,
            ).strip()
            self.assertEqual(count, "1")

    def test_push_to_empty_file_remotes_is_one_root_commit(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            src = _seed_src(root)
            ev = _evidence(root)
            fe = _bare(root, "frontend")
            be = _bare(root, "backend")
            gov = _bare(root, "gov")
            proc = _run(
                {
                    "SRC": str(src),
                    "OUT": str(root / "out"),
                    "PUSH": "1",
                    "REHEARSE": "0",
                    "EVIDENCE_DIR": str(ev),
                    "GITLAB_FRONTEND_URL": f"file://{fe}",
                    "GITLAB_BACKEND_URL": f"file://{be}",
                    "GITLAB_GOVERNANCE_URL": f"file://{gov}",
                    **COMPANY,
                }
            )
            self.assertEqual(proc.returncode, 0, proc.stdout + proc.stderr)
            for bare in (fe, be, gov):
                count = subprocess.check_output(
                    ["git", "--git-dir", str(bare), "rev-list", "--count", "refs/heads/main"],
                    text=True,
                ).strip()
                self.assertEqual(count, "1", bare)
                subject = subprocess.check_output(
                    ["git", "--git-dir", str(bare), "log", "-1", "--format=%s", "refs/heads/main"],
                    text=True,
                ).strip()
                self.assertEqual(subject, "Initial commit")
                ident = subprocess.check_output(
                    ["git", "--git-dir", str(bare), "log", "-1", "--format=%an <%ae>", "refs/heads/main"],
                    text=True,
                ).strip()
                self.assertEqual(ident, "Platform Engineer <dev@au.bank.in>")


if __name__ == "__main__":
    unittest.main()
