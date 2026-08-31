"""M2.7 rehearsal guards for migrate-repositories.sh — CR-017 / AC-6."""
from __future__ import annotations

import os
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "migrate-repositories.sh"


def _run(env: dict[str, str], **kwargs) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["bash", str(SCRIPT)],
        capture_output=True,
        text=True,
        env={**os.environ, **env},
        **kwargs,
    )


class MigrateRehearseTests(unittest.TestCase):
    def test_refuses_without_src(self) -> None:
        env = {k: v for k, v in os.environ.items() if k != "SRC"}
        env.pop("OUT", None)
        proc = subprocess.run(
            ["bash", str(SCRIPT)],
            capture_output=True,
            text=True,
            env=env,
        )
        self.assertNotEqual(proc.returncode, 0)
        self.assertIn("SRC", proc.stderr)

    def test_rehearse_cannot_push(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            proc = _run(
                {
                    "SRC": tmp,
                    "OUT": str(Path(tmp) / "out"),
                    "REHEARSE": "1",
                    "PUSH": "1",
                    "COMPANY_GIT_NAME": "Platform Engineer",
                    "COMPANY_GIT_EMAIL": "dev@au.bank.in",
                }
            )
        self.assertEqual(proc.returncode, 1)
        self.assertIn("cannot PUSH", proc.stdout + proc.stderr)

    def test_production_mode_refuses_without_signed_gates(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            src = Path(tmp) / "src"
            src.mkdir()
            subprocess.check_call(["git", "init"], cwd=src, stdout=subprocess.DEVNULL)
            (src / "README.md").write_text("src\n")
            subprocess.check_call(["git", "add", "README.md"], cwd=src)
            env = {
                **os.environ,
                "GIT_AUTHOR_NAME": "Platform Engineer",
                "GIT_AUTHOR_EMAIL": "dev@au.bank.in",
                "GIT_COMMITTER_NAME": "Platform Engineer",
                "GIT_COMMITTER_EMAIL": "dev@au.bank.in",
            }
            subprocess.check_call(
                ["git", "-c", "commit.gpgsign=false", "commit", "-m", "seed"],
                cwd=src,
                env=env,
                stdout=subprocess.DEVNULL,
            )
            proc = _run(
                {
                    "SRC": str(src),
                    "OUT": str(Path(tmp) / "out"),
                    "REHEARSE": "0",
                    "PUSH": "0",
                    "COMPANY_GIT_NAME": "Platform Engineer",
                    "COMPANY_GIT_EMAIL": "dev@au.bank.in",
                    "EVIDENCE_DIR": str(Path(tmp) / "no-evidence"),
                }
            )
        self.assertEqual(proc.returncode, 1)
        combined = proc.stdout + proc.stderr
        self.assertIn("REFUSING TO RUN", combined)
        self.assertIn("C-SEC-1", combined)

    def test_rehearse_writes_one_company_commit_per_project(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            src = Path(tmp) / "src"
            for rel in (
                "apps/rm-workspace-app",
                "services/demo",
                "libs/demo",
                "docs/ok",
                "scripts",
                ".claude",
            ):
                (src / rel).mkdir(parents=True)
            (src / "apps/rm-workspace-app/pubspec.yaml").write_text("name: rm\n")
            (src / "services/demo/README.md").write_text("svc\n")
            (src / "libs/demo/README.md").write_text("lib\n")
            (src / "docs/ok/note.md").write_text("# governance\n")
            (src / "scripts/hello.sh").write_text("echo ok\n")
            (src / "AGENTS.md").write_text("# agents\n")
            (src / "CLAUDE.md").write_text("# claude\n")
            (src / ".claude/skills").mkdir(parents=True)
            (src / ".claude/skills/.keep").write_text("x\n")
            subprocess.check_call(["git", "init"], cwd=src, stdout=subprocess.DEVNULL)
            subprocess.check_call(["git", "add", "-A"], cwd=src)
            env = {
                **os.environ,
                "GIT_AUTHOR_NAME": "Platform Engineer",
                "GIT_AUTHOR_EMAIL": "dev@au.bank.in",
                "GIT_COMMITTER_NAME": "Platform Engineer",
                "GIT_COMMITTER_EMAIL": "dev@au.bank.in",
            }
            subprocess.check_call(
                ["git", "-c", "commit.gpgsign=false", "commit", "-m", "seed"],
                cwd=src,
                env=env,
                stdout=subprocess.DEVNULL,
            )
            out = Path(tmp) / "out"
            proc = _run(
                {
                    "SRC": str(src),
                    "OUT": str(out),
                    "REHEARSE": "1",
                    "PUSH": "0",
                    "COMPANY_GIT_NAME": "Platform Engineer",
                    "COMPANY_GIT_EMAIL": "dev@au.bank.in",
                }
            )
            self.assertEqual(proc.returncode, 0, proc.stdout + proc.stderr)
            for name in ("frontend", "backend", "platform-governance"):
                repo = out / name
                count = subprocess.check_output(
                    ["git", "-C", str(repo), "rev-list", "--count", "HEAD"],
                    text=True,
                ).strip()
                self.assertEqual(count, "1", name)
                ident = subprocess.check_output(
                    ["git", "-C", str(repo), "log", "-1", "--format=%an <%ae>"],
                    text=True,
                ).strip()
                self.assertEqual(ident, "Platform Engineer <dev@au.bank.in>")


if __name__ == "__main__":
    unittest.main()
