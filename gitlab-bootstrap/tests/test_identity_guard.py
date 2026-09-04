"""Tests for identity-guard.py — CR-017 / AC-6."""
from __future__ import annotations

import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "identity-guard.py"
sys.path.insert(0, str(ROOT / "scripts"))
import identity_guard  # noqa: E402


class TreeScanTests(unittest.TestCase):
    def test_clean_tree_is_ok(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, "README.md").write_text("# platform\nSee https://github.com/gradle/gradle/\n")
            self.assertEqual(identity_guard.scan_tree(Path(tmp)), [])

    def test_personal_email_is_a_hit(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, "note.md").write_text("Provided by Mahesh38 · mh.narkar@gmail.com\n")
            hits = identity_guard.scan_tree(Path(tmp))
            self.assertTrue(any("personal-gmail" in h for h in hits))
            self.assertTrue(any("personal-github-login" in h for h in hits))

    def test_lowercase_login_is_a_hit(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, "state.yaml").write_text('repository: "mahesh38/bank-insurance-platform"\n')
            hits = identity_guard.scan_tree(Path(tmp))
            self.assertTrue(any("personal-github-login" in h for h in hits))

    def test_m2_evidence_is_a_hit_if_present(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            ev = Path(tmp, "m2-evidence")
            ev.mkdir()
            (ev / "scan.json").write_text('"Email": "mh.narkar@gmail.com"\n')
            hits = identity_guard.scan_tree(Path(tmp))
            self.assertTrue(any("personal-gmail" in h for h in hits))

    def test_claude_session_is_a_hit(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, "msg.txt").write_text(
                "Claude-Session: https://claude.ai/code/session_01abc\n"
            )
            hits = identity_guard.scan_tree(Path(tmp))
            self.assertTrue(any("claude-session" in h for h in hits))

    def test_generated_with_claude_is_a_hit(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, "note.md").write_text("Generated with Claude Code\n")
            hits = identity_guard.scan_tree(Path(tmp))
            self.assertTrue(any("claude-coauthor" in h for h in hits))

    def test_cursor_agents_url_is_a_hit(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, "note.md").write_text("See https://cursor.com/agents/abc\n")
            hits = identity_guard.scan_tree(Path(tmp))
            self.assertTrue(any("cursor-agent" in h for h in hits))


class CompanyIdentityTests(unittest.TestCase):
    def test_gmail_rejected(self) -> None:
        hits = identity_guard.validate_company_identity("Dev", "mh.narkar@gmail.com")
        self.assertTrue(any("forbidden-domain" in h for h in hits))

    def test_bank_email_accepted(self) -> None:
        hits = identity_guard.validate_company_identity("Platform Engineer", "dev@au.bank.in")
        self.assertEqual(hits, [])

    def test_domain_env_enforced(self) -> None:
        old = os.environ.get("COMPANY_EMAIL_DOMAIN")
        os.environ["COMPANY_EMAIL_DOMAIN"] = "au.bank.in"
        try:
            hits = identity_guard.validate_company_identity("Dev", "dev@example.com")
            self.assertTrue(any("COMPANY_EMAIL_DOMAIN" in h for h in hits))
        finally:
            if old is None:
                os.environ.pop("COMPANY_EMAIL_DOMAIN", None)
            else:
                os.environ["COMPANY_EMAIL_DOMAIN"] = old


class GitLogTests(unittest.TestCase):
    def test_orphan_company_commit_is_clean(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            subprocess.check_call(["git", "init"], cwd=tmp, stdout=subprocess.DEVNULL)
            Path(tmp, "README.md").write_text("ok\n")
            env = {
                **os.environ,
                "GIT_AUTHOR_NAME": "Platform Engineer",
                "GIT_AUTHOR_EMAIL": "dev@au.bank.in",
                "GIT_COMMITTER_NAME": "Platform Engineer",
                "GIT_COMMITTER_EMAIL": "dev@au.bank.in",
            }
            subprocess.check_call(["git", "add", "README.md"], cwd=tmp)
            subprocess.check_call(
                ["git", "commit", "-m", "Initial commit"],
                cwd=tmp,
                env=env,
                stdout=subprocess.DEVNULL,
            )
            self.assertEqual(identity_guard.scan_git(Path(tmp)), [])

    def test_gmail_author_is_a_hit(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            subprocess.check_call(["git", "init"], cwd=tmp, stdout=subprocess.DEVNULL)
            Path(tmp, "README.md").write_text("ok\n")
            env = {
                **os.environ,
                "GIT_AUTHOR_NAME": "Mahesh38",
                "GIT_AUTHOR_EMAIL": "mh.narkar@gmail.com",
                "GIT_COMMITTER_NAME": "Mahesh38",
                "GIT_COMMITTER_EMAIL": "mh.narkar@gmail.com",
            }
            subprocess.check_call(["git", "add", "README.md"], cwd=tmp)
            subprocess.check_call(
                ["git", "commit", "-m", "wip"],
                cwd=tmp,
                env=env,
                stdout=subprocess.DEVNULL,
            )
            hits = identity_guard.scan_git(Path(tmp))
            self.assertTrue(hits)

    def test_merge_pull_request_message_is_a_hit(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            subprocess.check_call(["git", "init"], cwd=tmp, stdout=subprocess.DEVNULL)
            Path(tmp, "README.md").write_text("ok\n")
            env = {
                **os.environ,
                "GIT_AUTHOR_NAME": "Platform Engineer",
                "GIT_AUTHOR_EMAIL": "dev@au.bank.in",
                "GIT_COMMITTER_NAME": "Platform Engineer",
                "GIT_COMMITTER_EMAIL": "dev@au.bank.in",
            }
            subprocess.check_call(["git", "add", "README.md"], cwd=tmp)
            subprocess.check_call(
                ["git", "commit", "-m", "Merge pull request #82 from someone/branch"],
                cwd=tmp,
                env=env,
                stdout=subprocess.DEVNULL,
            )
            hits = identity_guard.scan_git(Path(tmp))
            self.assertTrue(any("Merge pull request" in h for h in hits))

    def test_cursor_agent_committer_is_a_hit(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            subprocess.check_call(["git", "init"], cwd=tmp, stdout=subprocess.DEVNULL)
            Path(tmp, "README.md").write_text("ok\n")
            env = {
                **os.environ,
                "GIT_AUTHOR_NAME": "Cursor Agent",
                "GIT_AUTHOR_EMAIL": "cursoragent@cursor.com",
                "GIT_COMMITTER_NAME": "Cursor Agent",
                "GIT_COMMITTER_EMAIL": "cursoragent@cursor.com",
            }
            subprocess.check_call(["git", "add", "README.md"], cwd=tmp)
            subprocess.check_call(
                ["git", "commit", "-m", "wip"],
                cwd=tmp,
                env=env,
                stdout=subprocess.DEVNULL,
            )
            hits = identity_guard.scan_git(Path(tmp))
            self.assertTrue(hits)


class CliTests(unittest.TestCase):
    def test_script_exits_1_on_tree_hit(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, "x.md").write_text("Mahesh38\n")
            proc = subprocess.run(
                [sys.executable, str(SCRIPT), "--tree", tmp],
                capture_output=True,
                text=True,
            )
            self.assertEqual(proc.returncode, 1)


if __name__ == "__main__":
    unittest.main()
