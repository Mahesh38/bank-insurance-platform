#!/usr/bin/env python3
"""Unit tests for S08-G9 pipeline feedback measurement helpers."""

from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("measure-pipeline-feedback.py")
SPEC = importlib.util.spec_from_file_location("measure_pipeline_feedback", SCRIPT)
assert SPEC and SPEC.loader
mod = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(mod)


def run(
    rid: int,
    conclusion: str,
    event: str,
    sha: str,
    created: str,
    duration_s: int,
) -> dict:
    # updated_at = created + duration
    from datetime import datetime, timedelta, timezone

    start = datetime.fromisoformat(created.replace("Z", "+00:00"))
    end = start + timedelta(seconds=duration_s)
    return {
        "id": rid,
        "conclusion": conclusion,
        "event": event,
        "created_at": created,
        "run_started_at": created,
        "updated_at": end.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "head_sha": sha,
        "display_title": f"run-{rid}",
        "html_url": f"https://example.test/{rid}",
        "head_branch": "main",
    }


class MeasurePipelineFeedbackTests(unittest.TestCase):
    def test_percentile_p95(self) -> None:
        values = [float(i) for i in range(1, 21)]  # 1..20
        self.assertAlmostEqual(mod.percentile(values, 95), 19.05, places=2)

    def test_pass_when_fast_and_no_flakes(self) -> None:
        runs = []
        for i in range(60):
            runs.append(
                run(
                    i,
                    "success",
                    "pull_request" if i < 25 else "push",
                    f"sha-{i:03d}",
                    f"2026-09-01T00:{i % 60:02d}:00Z" if i < 60 else "2026-09-01T00:00:00Z",
                    duration_s=90,
                )
            )
        # Fix timestamps to be unique/monotonic
        from datetime import datetime, timedelta, timezone

        base = datetime(2026, 9, 1, tzinfo=timezone.utc)
        for i, r in enumerate(runs):
            start = base + timedelta(minutes=i)
            end = start + timedelta(seconds=90)
            r["created_at"] = start.strftime("%Y-%m-%dT%H:%M:%SZ")
            r["run_started_at"] = r["created_at"]
            r["updated_at"] = end.strftime("%Y-%m-%dT%H:%M:%SZ")

        report = mod.measure(runs, repo="example/repo", total_count=60)
        self.assertEqual("PASS", report["verdict"])
        self.assertLess(report["s08_vt_08_feedback_time"]["p95_seconds_for_gate"], 600)
        self.assertEqual(0.0, report["s08_vt_09_flake_rate"]["flake_rate_pct_for_gate"])

    def test_flake_detected_same_sha_green_and_red(self) -> None:
        from datetime import datetime, timedelta, timezone

        base = datetime(2026, 9, 1, tzinfo=timezone.utc)
        runs = []
        for i in range(50):
            start = base + timedelta(minutes=i)
            end = start + timedelta(seconds=60)
            sha = "same-sha" if i < 2 else f"sha-{i:03d}"
            conclusion = "failure" if i == 0 else "success"
            runs.append(
                {
                    "id": i,
                    "conclusion": conclusion,
                    "event": "pull_request",
                    "created_at": start.strftime("%Y-%m-%dT%H:%M:%SZ"),
                    "run_started_at": start.strftime("%Y-%m-%dT%H:%M:%SZ"),
                    "updated_at": end.strftime("%Y-%m-%dT%H:%M:%SZ"),
                    "head_sha": sha,
                    "display_title": f"run-{i}",
                    "html_url": f"https://example.test/{i}",
                    "head_branch": "feature",
                }
            )
        report = mod.measure(runs, repo="example/repo", total_count=50)
        self.assertEqual("FAIL", report["verdict"])
        self.assertGreaterEqual(
            report["s08_vt_09_flake_rate"]["most_recent_50_concluded"]["flake_rate_pct"],
            1.0,
        )


if __name__ == "__main__":
    unittest.main()
