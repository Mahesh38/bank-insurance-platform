#!/usr/bin/env python3
"""Measure Application CI feedback time (p95) and flake rate for S08-G9 / S08-VT-08/09.

Reproducible verifier for GATE-S08 criterion S08-G9:

  * Pipeline feedback under 10 minutes at p95 (S08-VT-08: ≥ 20 runs)
  * Flaky failure rate under 1% (S08-VT-09: ≥ 50 runs)
  * Evidence artefact: pipeline metrics over ≥ 50 runs (S08-G9 E4)

Usage:

  # Refresh from GitHub Actions API (requires `gh` auth) and write evidence JSON:
  python3 scripts/governance/measure-pipeline-feedback.py --fetch --write

  # Re-check a committed snapshot offline (CI-friendly):
  python3 scripts/governance/measure-pipeline-feedback.py --from-file \\
      scripts/governance/evidence/S08-G9-pipeline-feedback.json --assert

Exit codes: 0 pass · 1 thresholds failed · 2 usage / I/O error.
"""

from __future__ import annotations

import argparse
import json
import math
import subprocess
import sys
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_OUT = ROOT / "scripts/governance/evidence/S08-G9-pipeline-feedback.json"
WORKFLOW = "application-ci.yml"
P95_LIMIT_SECONDS = 10 * 60
FLAKE_LIMIT_PCT = 1.0
MIN_FEEDBACK_RUNS = 20
MIN_FLAKE_RUNS = 50


def parse_ts(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def duration_seconds(run: dict[str, Any]) -> float:
    start = parse_ts(run.get("run_started_at") or run["created_at"])
    end = parse_ts(run["updated_at"])
    return max(0.0, (end - start).total_seconds())


def percentile(values: list[float], p: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    if len(ordered) == 1:
        return ordered[0]
    rank = (p / 100.0) * (len(ordered) - 1)
    lo = math.floor(rank)
    hi = math.ceil(rank)
    if lo == hi:
        return ordered[lo]
    return ordered[lo] + (ordered[hi] - ordered[lo]) * (rank - lo)


def fetch_runs(repo: str | None = None) -> tuple[str, int, list[dict[str, Any]]]:
    if not repo:
        repo = subprocess.check_output(
            ["gh", "repo", "view", "--json", "nameWithOwner", "-q", ".nameWithOwner"],
            text=True,
        ).strip()
    runs: list[dict[str, Any]] = []
    page = 1
    total = None
    while True:
        raw = subprocess.check_output(
            [
                "gh",
                "api",
                f"repos/{repo}/actions/workflows/{WORKFLOW}/runs"
                f"?per_page=100&status=completed&page={page}",
            ],
            text=True,
        )
        data = json.loads(raw)
        if total is None:
            total = int(data["total_count"])
        batch = data.get("workflow_runs") or []
        if not batch:
            break
        for item in batch:
            runs.append(
                {
                    "id": item["id"],
                    "conclusion": item["conclusion"],
                    "event": item["event"],
                    "created_at": item["created_at"],
                    "updated_at": item["updated_at"],
                    "run_started_at": item.get("run_started_at") or item["created_at"],
                    "head_sha": item["head_sha"],
                    "display_title": item.get("display_title") or item.get("name"),
                    "html_url": item.get("html_url"),
                    "head_branch": item.get("head_branch"),
                }
            )
        if len(runs) >= total or len(batch) < 100:
            break
        page += 1
        if page > 50:
            break
    return repo, total or len(runs), runs


def measure(runs: list[dict[str, Any]], *, repo: str, total_count: int) -> dict[str, Any]:
    """Compute S08-G9 metrics.

    Flake definition (S08-VT-09): a ``failure`` on a ``head_sha`` that also has at least
    one ``success`` in the sample — same commit green and red. Legitimate red builds on
    broken commits are not flakes.
    """
    concluded = [r for r in runs if r["conclusion"] in {"success", "failure", "timed_out"}]
    pr_runs = [r for r in concluded if r["event"] == "pull_request"]
    # Prefer chronological most-recent for the VT sample windows.
    pr_sorted = sorted(pr_runs, key=lambda r: r["created_at"], reverse=True)
    all_sorted = sorted(concluded, key=lambda r: r["created_at"], reverse=True)

    def summarise(sample: list[dict[str, Any]]) -> dict[str, Any]:
        durs = [duration_seconds(r) for r in sample]
        return {
            "n": len(sample),
            "conclusions": dict(Counter(r["conclusion"] for r in sample)),
            "duration_seconds": {
                "min": min(durs) if durs else None,
                "median": percentile(durs, 50),
                "p95": percentile(durs, 95),
                "max": max(durs) if durs else None,
            },
            "p95_minutes": (percentile(durs, 95) / 60.0) if durs else None,
        }

    def flake_stats(sample: list[dict[str, Any]]) -> dict[str, Any]:
        by_sha: dict[str, list[dict[str, Any]]] = defaultdict(list)
        for run in sample:
            by_sha[run["head_sha"]].append(run)
        flake_shas = []
        flaky_failures = []
        for sha, group in by_sha.items():
            conclusions = {r["conclusion"] for r in group}
            if "success" in conclusions and "failure" in conclusions:
                flake_shas.append(sha)
                flaky_failures.extend(r for r in group if r["conclusion"] == "failure")
        n = len(sample)
        rate = (100.0 * len(flaky_failures) / n) if n else None
        return {
            "n": n,
            "unique_head_shas": len(by_sha),
            "flake_head_shas": len(flake_shas),
            "flaky_failure_runs": len(flaky_failures),
            "flake_rate_pct": rate,
            "raw_failure_runs": sum(1 for r in sample if r["conclusion"] == "failure"),
            "definition": (
                "flaky_failure = failure conclusion on a head_sha that also has ≥1 success "
                "in the sample (same commit both green and red)"
            ),
        }

    feedback_all = summarise(concluded)
    feedback_pr = summarise(pr_runs)
    feedback_pr20 = summarise(pr_sorted[:20])
    flake_all = flake_stats(concluded)
    flake_50 = flake_stats(all_sorted[:50])

    p95_primary = feedback_pr20["duration_seconds"]["p95"]
    if p95_primary is None:
        p95_primary = feedback_all["duration_seconds"]["p95"]
    # Use the larger of PR-20 and all-completed p95 for the gate assertion — both must be < 10m.
    p95_candidates = [
        v
        for v in (
            feedback_pr20["duration_seconds"]["p95"],
            feedback_pr["duration_seconds"]["p95"],
            feedback_all["duration_seconds"]["p95"],
        )
        if v is not None
    ]
    p95_gate = max(p95_candidates) if p95_candidates else None

    flake_rate = flake_50["flake_rate_pct"]
    if flake_50["n"] < MIN_FLAKE_RUNS:
        flake_rate = flake_all["flake_rate_pct"]

    vt08_n = feedback_pr20["n"] if feedback_pr20["n"] >= MIN_FEEDBACK_RUNS else feedback_all["n"]
    vt09_n = flake_50["n"] if flake_50["n"] >= MIN_FLAKE_RUNS else flake_all["n"]

    thresholds = {
        "p95_under_10_minutes": p95_gate is not None and p95_gate < P95_LIMIT_SECONDS,
        "flake_under_1_percent": flake_rate is not None and flake_rate < FLAKE_LIMIT_PCT,
        "feedback_sample_ge_20": vt08_n >= MIN_FEEDBACK_RUNS,
        "flake_sample_ge_50": vt09_n >= MIN_FLAKE_RUNS,
    }
    passed = all(thresholds.values())

    tip = all_sorted[0] if all_sorted else None
    return {
        "schema_version": "1.0",
        "criterion": "S08-G9",
        "validations": ["S08-VT-08", "S08-VT-09"],
        "workflow": WORKFLOW,
        "workflow_name": "Application CI",
        "repository": repo,
        "measured_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "api_total_count": total_count,
        "fetched_completed_runs": len(runs),
        "sample_concluded_runs": len(concluded),
        "conclusion_counts": dict(Counter(r["conclusion"] for r in runs)),
        "event_counts": dict(Counter(r["event"] for r in runs)),
        "s08_vt_08_feedback_time": {
            "method": "Measure PR feedback time across ≥ 20 runs; pass p95 under 10 minutes",
            "primary_sample": "most_recent_20_pull_request",
            "most_recent_20_pull_request": feedback_pr20,
            "all_pull_request": feedback_pr,
            "all_concluded": feedback_all,
            "p95_seconds_for_gate": p95_gate,
            "p95_minutes_for_gate": (p95_gate / 60.0) if p95_gate is not None else None,
            "limit_minutes": 10,
        },
        "s08_vt_09_flake_rate": {
            "method": "Measure flaky failures across ≥ 50 runs; pass under 1%",
            "primary_sample": "most_recent_50_concluded",
            "most_recent_50_concluded": flake_50,
            "all_concluded": flake_all,
            "flake_rate_pct_for_gate": flake_rate,
            "limit_pct": FLAKE_LIMIT_PCT,
        },
        "thresholds": thresholds,
        "verdict": "PASS" if passed else "FAIL",
        "tip_run": (
            {
                "id": tip["id"],
                "conclusion": tip["conclusion"],
                "event": tip["event"],
                "created_at": tip["created_at"],
                "duration_seconds": duration_seconds(tip),
                "html_url": tip.get("html_url"),
                "head_sha": tip["head_sha"],
            }
            if tip
            else None
        ),
        # Compact run table kept for audit; durations only, no secrets.
        "runs": [
            {
                "id": r["id"],
                "conclusion": r["conclusion"],
                "event": r["event"],
                "created_at": r["created_at"],
                "duration_seconds": round(duration_seconds(r), 1),
                "head_sha": r["head_sha"],
            }
            for r in concluded
        ],
    }


def assert_thresholds(report: dict[str, Any]) -> int:
    thresholds = report.get("thresholds") or {}
    ok = report.get("verdict") == "PASS" and all(thresholds.values())
    vt08 = report["s08_vt_08_feedback_time"]
    vt09 = report["s08_vt_09_flake_rate"]
    print(
        f"S08-G9 measure: verdict={report.get('verdict')} "
        f"p95={vt08.get('p95_minutes_for_gate'):.3f}m "
        f"flake={vt09.get('flake_rate_pct_for_gate'):.4f}% "
        f"N_feedback={vt08['most_recent_20_pull_request']['n']} "
        f"N_flake={vt09['most_recent_50_concluded']['n']} "
        f"api_total={report.get('api_total_count')}"
    )
    for key, value in thresholds.items():
        print(f"  {'OK' if value else 'FAIL'}  {key}")
    return 0 if ok else 1


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--fetch", action="store_true", help="Fetch runs via gh api")
    parser.add_argument("--from-file", type=Path, help="Load a prior evidence JSON")
    parser.add_argument("--write", action="store_true", help="Write evidence JSON to --out")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    parser.add_argument("--repo", help="owner/name override for gh api")
    parser.add_argument(
        "--assert",
        dest="do_assert",
        action="store_true",
        help="Exit non-zero if thresholds are not met",
    )
    args = parser.parse_args(argv)

    if args.fetch:
        repo, total, runs = fetch_runs(args.repo)
        report = measure(runs, repo=repo, total_count=total)
    elif args.from_file:
        path = args.from_file
        if not path.is_file():
            print(f"missing evidence file: {path}", file=sys.stderr)
            return 2
        payload = json.loads(path.read_text(encoding="utf-8"))
        if "s08_vt_08_feedback_time" in payload:
            report = payload
        else:
            runs = payload.get("runs") or []
            report = measure(
                runs,
                repo=payload.get("repository") or "unknown",
                total_count=int(payload.get("api_total_count") or len(runs)),
            )
    else:
        parser.error("provide --fetch or --from-file")

    if args.write:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        # Drop the bulky runs table from the committed snapshot? Keep it — auditability.
        args.out.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print(f"wrote {args.out} ({report['sample_concluded_runs']} concluded runs)")

    if args.do_assert:
        return assert_thresholds(report)

    vt08 = report["s08_vt_08_feedback_time"]
    vt09 = report["s08_vt_09_flake_rate"]
    print(json.dumps(
        {
            "verdict": report["verdict"],
            "p95_minutes": vt08.get("p95_minutes_for_gate"),
            "flake_rate_pct": vt09.get("flake_rate_pct_for_gate"),
            "thresholds": report["thresholds"],
            "api_total_count": report.get("api_total_count"),
            "sample_concluded_runs": report.get("sample_concluded_runs"),
        },
        indent=2,
    ))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
