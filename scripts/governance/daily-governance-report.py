#!/usr/bin/env python3
"""Daily AIGEM sign-off report: run every governance check, refresh the generated views,
and list every decision that is waiting on a named human.

    python3 scripts/governance/daily-governance-report.py                  # print the report
    python3 scripts/governance/daily-governance-report.py --write \\
        --pr-body /tmp/body.md --pr-title /tmp/title.txt                   # what the daily workflow runs

What it changes (with --write), and nothing else:
  * docs/context/BOOT.md generated block      (build-boot-capsule.py)
  * docs/application-lifecycle-bible/backlog/ (generate-backlog.py)
  * docs/governance/autopilot/DAILY-SIGNOFF.md (this report)
  * docs/context/DOC-MAP.yaml                 (build-doc-map.py, so the report stays routed)

What it never changes: CURRENT-STATE.yaml, GATE-EVIDENCE.yaml, any register, any change
request. `state_as_of` is a human attestation (R12 at the Governance Sync) and is listed as
an action, never bumped. Board concurrence below is an automated consistency verdict on the
daily update; it is not a board approval and never satisfies a T4 human sign-off
(09-AI_EXECUTION_RULES.md, AGENTS.md section 2).

Workflow: .github/workflows/governance-daily.yml. Runbook: RUNBOOK.md section 3, Daily.
"""

from __future__ import annotations

import argparse
import datetime as dt
import os
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

try:
    import yaml
except ImportError as exc:  # pragma: no cover
    print(f"daily-governance-report needs PyYAML: {exc}", file=sys.stderr)
    raise SystemExit(2)

ROOT = Path(__file__).resolve().parents[2]
STATE = ROOT / "docs/governance/state/CURRENT-STATE.yaml"
EVIDENCE = ROOT / "docs/governance/state/GATE-EVIDENCE.yaml"
DECISIONS = ROOT / "docs/governance/registers/DECISION-REGISTER.md"
DEPENDENCIES = ROOT / "docs/governance/registers/DEPENDENCY-REGISTER.md"
SUGGESTIONS = ROOT / "docs/governance/registers/SUGGESTION-REGISTER.md"
REPORT = ROOT / "docs/governance/autopilot/DAILY-SIGNOFF.md"
PY = sys.executable

DATE = re.compile(r"\d{4}-\d{2}-\d{2}")
APPROVED = {"APPROVED", "APPROVED_WITH_CONDITIONS"}
# Words in an approver cell that mean a signature is still owed, even on an APPROVED row.
OUTSTANDING = re.compile(r"\b(outstanding|pending)\b", re.I)
STATE_SYNC_DAYS = 7  # the weekly Governance Sync (RUNBOOK section 3)


# --------------------------------------------------------------------------- checks

@dataclass
class Check:
    key: str
    title: str
    cmd: list[str]
    ok_below: int = 1  # exit codes below this pass; freshness passes on 0 and 1
    code: int | None = None
    output: str = ""

    @property
    def ran(self) -> bool:
        return self.code is not None and self.code not in (126, 127)

    @property
    def passed(self) -> bool:
        return self.ran and self.code < self.ok_below


def checks() -> list[Check]:
    gov = "scripts/governance"
    ctx = "scripts/context"
    return [
        Check("freshness", "State freshness (FreshnessCheck)",
              ["java", f"{gov}/FreshnessCheck.java", "--no-color"], ok_below=2),
        Check("freshness-tests", "FreshnessCheck fixture tests", ["bash", f"{gov}/test-freshness-check.sh"]),
        Check("ci", "Schemas, records, links, BOOT and DOC-MAP", [PY, f"{gov}/ci-checks.py", "--quiet"]),
        Check("context", "Context module validation", [PY, f"{ctx}/validate-context.py"]),
        Check("context-tests", "Context portability tests", [PY, f"{ctx}/test_context.py"]),
        Check("autopilot-tests", "Autopilot safety tests", [PY, f"{gov}/test_autopilot.py"]),
        Check("autopilot", "Autopilot status (proposal-only policy)", [PY, f"{gov}/autopilot.py", "status"]),
        Check("report-tests", "Daily report parser tests", [PY, f"{gov}/test_daily_governance_report.py"]),
    ]


def run(cmd: list[str]) -> tuple[int, str]:
    try:
        proc = subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True, timeout=600)
    except FileNotFoundError as exc:
        return 127, str(exc)
    except subprocess.TimeoutExpired:
        return 124, "timed out after 600s"
    return proc.returncode, (proc.stdout + proc.stderr).strip()


GENERATED = ["docs/context/BOOT.md", "docs/application-lifecycle-bible/backlog", "docs/context/DOC-MAP.yaml"]


def regenerate_views() -> tuple[list[str], list[str]]:
    """Refresh the generated, non-authoritative views. Returns (failures, files that drifted)."""
    failures = []
    for cmd in (
        [PY, "scripts/context/build-boot-capsule.py"],
        [PY, "scripts/lifecycle/generate-backlog.py"],
        [PY, "scripts/context/build-doc-map.py"],
    ):
        code, out = run(cmd)
        if code != 0:
            failures.append(f"`{' '.join(cmd[1:])}` exited {code}: {out.splitlines()[-1] if out else ''}")
    _, changed = run(["git", "status", "--porcelain", "--", *GENERATED])
    return failures, [line[3:] for line in changed.splitlines() if line.strip()]


# --------------------------------------------------------------------------- parsers

def table_rows(text: str, heading: str) -> list[list[str]]:
    """Rows of the first markdown table under `heading` (header and rule rows dropped)."""
    start = text.find(heading)
    if start < 0:
        return []
    rows, seen_table = [], False
    for line in text[start + len(heading):].splitlines():
        if line.startswith("## "):
            break
        if line.startswith("|"):
            seen_table = True
            cells = [c.strip() for c in line.strip().strip("|").split("|")]
            rows.append(cells)
        elif seen_table and line.strip():
            break
    return [r for r in rows[2:] if r and not set(r[0]) <= set("-: ")]


def plain(cell: str) -> str:
    cell = re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", cell)
    return re.sub(r"[*`]", "", cell).strip()


def pending_change_requests(text: str) -> list[dict]:
    """Change requests that are not approved, or approved with a signature still owed."""
    out = []
    for row in table_rows(text, "## 3. Change requests"):
        if len(row) < 6:
            continue
        cr_id, _, _, summary, decision, approvers = row[:6]
        decision_plain = plain(decision)
        if decision_plain.upper().startswith("APPROVED") and not OUTSTANDING.search(approvers):
            continue
        out.append({
            "id": plain(cr_id),
            "summary": shorten(plain(summary), 140),
            "decision": shorten(decision_plain, 60),
            "owed": shorten(plain(approvers), 220),
        })
    return out


def overdue_external_dependencies(text: str, today: dt.date) -> list[dict]:
    """External dependencies whose required-by date has passed, recomputed daily."""
    out = []
    for row in table_rows(text, "## 2. External dependencies"):
        if len(row) < 6:
            continue
        dep_id, what, owner, required_by, _, state = row[:6]
        match = DATE.search(required_by)
        if not match or plain(state).upper() not in {"OPEN", "BLOCKED", "IN-FLIGHT"}:
            continue
        due = dt.date.fromisoformat(match.group())
        if due < today:
            out.append({
                "id": plain(dep_id).lstrip("→ ").strip(),
                "what": shorten(plain(what), 110),
                "owner": plain(owner),
                "due": due.isoformat(),
                "days": (today - due).days,
            })
    return out


def escalated_suggestions(text: str) -> list[dict]:
    out = []
    for row in table_rows(text, "## 2. Register"):
        if len(row) >= 10 and plain(row[9]).upper() == "ESCALATED":
            out.append({"id": plain(row[0]), "summary": shorten(plain(row[3]), 140)})
    return out


def gate_actions(bundle: dict, today: dt.date) -> tuple[list[dict], list[dict]]:
    """(gates awaiting human verdicts, criterion blockers past their follow-up date)."""
    signoffs, blockers = [], []
    for stream in bundle.get("workstreams", []):
        given = {a["authority"] for a in stream.get("approvals", [])
                 if a.get("reviewer_type") == "HUMAN" and a.get("decision") in APPROVED}
        missing = [a for a in stream.get("required_approvers", []) if a not in given]
        if stream.get("state") == "CANDIDATE" and missing:
            signoffs.append({
                "workstream": stream["id"], "gate": stream["gate_id"],
                "from": stream.get("current_stage", ""), "to": stream.get("next_stage", ""),
                "missing": missing,
            })
        for crit in stream.get("criteria", []):
            for blk in crit.get("blockers", []) or []:
                follow = blk.get("follow_up")
                if follow and dt.date.fromisoformat(str(follow)) < today:
                    blockers.append({
                        "gate": stream["gate_id"], "criterion": crit["id"], "blocker": blk["id"],
                        "owner": blk.get("owner", crit.get("owner", "")),
                        "follow_up": str(follow), "days": (today - dt.date.fromisoformat(str(follow))).days,
                    })
    return signoffs, blockers


def non_human_approvals(bundle: dict) -> list[str]:
    """Approvals recorded without a HUMAN reviewer — an agent may never supply one."""
    return [f"{s['id']} {a.get('authority')}" for s in bundle.get("workstreams", [])
            for a in s.get("approvals", []) if a.get("reviewer_type") != "HUMAN"]


def state_actions(state: dict, today: dt.date) -> list[str]:
    actions = []
    as_of = dt.date.fromisoformat(str(state["state_as_of"]))
    due = dt.date.fromisoformat(str(state["review_due"]))
    age = (today - as_of).days
    if age > STATE_SYNC_DAYS:
        actions.append(f"**Kalpana / R12** — re-confirm `CURRENT-STATE.yaml` at the Governance Sync: "
                       f"`state_as_of` is {as_of} ({age} days old, weekly cadence).")
    left = (due - today).days
    if left < 0:
        actions.append(f"**Kalpana / R12** — `review_due` {due} has passed ({-left} days). "
                       f"Agents may not admit new work until the state is re-ratified (Rule CS-1).")
    elif left <= 14:
        actions.append(f"**Kalpana / R12** — `review_due` is {due} ({left} days). Schedule the re-ratification.")
    if OUTSTANDING.search(str(state.get("ratified_by", ""))):
        actions.append(f"**Rajal / Product** — counter-sign the state ratification: "
                       f"{shorten(str(state['ratified_by']), 160)}")
    return actions


def freshness_findings(output: str) -> tuple[list[str], list[str]]:
    halts = [l.split("HALT", 1)[1].strip() for l in output.splitlines() if l.strip().startswith("HALT")]
    warns = [l.split("WARN", 1)[1].strip() for l in output.splitlines() if l.strip().startswith("WARN")]
    return halts, warns


def section_ok(output: str, section: str) -> bool:
    """True when a FreshnessCheck section printed no HALT line."""
    block = output.split(section, 1)[1] if section in output else ""
    block = re.split(r"\n\S", block, maxsplit=1)[0]
    return section in output and "HALT" not in block


def shorten(text: str, limit: int) -> str:
    text = re.sub(r"\s+", " ", text).strip()
    return text if len(text) <= limit else text[: limit - 1].rstrip() + "…"


# --------------------------------------------------------------------------- concurrence

@dataclass
class Verdict:
    board: str
    concur: bool
    basis: str
    reasons: list[str] = field(default_factory=list)


def concurrence(results: dict[str, Check], regen_failures: list[str], manufactured: list[str]) -> list[Verdict]:
    def need(*keys: str) -> list[str]:
        return [f"{results[k].title} failed (exit {results[k].code})" for k in keys if not results[k].passed]

    fresh = results["freshness"].output
    tests = ("freshness-tests", "context-tests", "autopilot-tests", "report-tests")
    board = [
        ("Mahesh — Architecture (Board 1)", "schemas, state references and links validate", need("ci")),
        ("Amit — Engineering (Board 2)", "generated views regenerate cleanly and the context module validates",
         regen_failures + need("context")),
        ("Rajal — Product (Board 3)", "every workstream's objective, scope and gate resolve",
         [] if section_ok(fresh, "State structure") else ["State structure has a HALT"]),
        ("Deepali — Security (Board 4)", "autopilot is proposal-only and its safety tests pass",
         need("autopilot", "autopilot-tests")),
        ("Swapnali — QA (Board 5)", "every governance tool test suite passes", need(*tests)),
        ("Shailja — Compliance (Board 6)", "register IDs are unique and no approval lacks a human reviewer",
         ([] if section_ok(fresh, "ID uniqueness") else ["register ID collision"])
         + [f"non-human approval recorded: {m}" for m in manufactured]),
        ("Shivanshi — Operations (Board 7)", "every check executed on the runner",
         [f"{c.title} did not run: {c.output[:120]}" for c in results.values() if not c.ran]),
        ("Kalpana — Delivery (R12)", "state is not at halt-class staleness", need("freshness")),
    ]
    return [Verdict(name, not reasons, basis, reasons) for name, basis, reasons in board]


# --------------------------------------------------------------------------- report

def render(today: dt.date, results: dict[str, Check], verdicts: list[Verdict], state: dict,
           signoffs: list[dict], blockers: list[dict], crs: list[dict], deps: list[dict],
           escalations: list[dict], regen_failures: list[str],
           drifted: list[str] | None = None) -> tuple[str, str]:
    unanimous = all(v.concur for v in verdicts)
    fresh_code = results["freshness"].code
    halts, warns = freshness_findings(results["freshness"].output)
    state_items = state_actions(state, today)
    human_count = (len(signoffs) + len(blockers) + len(crs) + len(deps) + len(escalations)
                   + len(state_items) + len(halts) + len(warns))
    dissent = [v.board.split(" —")[0] for v in verdicts if not v.concur]
    verdict_word = "UNANIMOUS CONCUR" if unanimous else f"DISSENT ({', '.join(dissent)})"
    fresh_word = {0: "FRESH", 1: "WARN", 2: "HALT"}.get(fresh_code, f"ERROR ({fresh_code})")
    title = (f"AIGEM daily sign-off {today} — {verdict_word} · freshness {fresh_word} · "
             f"{human_count} human action{'s' if human_count != 1 else ''}")

    L: list[str] = []
    add = L.append
    add(f"# AIGEM daily sign-off — {today}")
    add("")
    add("> Generated by [`daily-governance-report.py`](../../../scripts/governance/daily-governance-report.py) "
        "from the scheduled `governance-daily` workflow. **Do not hand-edit** — the next run overwrites it.")
    add("> Automated concurrence is a consistency verdict on this update, **not** a board approval, and it "
        "never satisfies a T4 human sign-off ([09](../09-AI_EXECUTION_RULES.md)). Merging this report "
        "records that a human read it; it approves nothing listed below.")
    add("")
    add("| | |")
    add("|---|---|")
    add(f"| AIGEM concurrence | **{verdict_word}** |")
    add(f"| Freshness | **{fresh_word}** (FreshnessCheck exit {fresh_code}) |")
    add(f"| State as of / review due | {state['state_as_of']} / {state['review_due']} |")
    add(f"| Human actions listed | **{human_count}** |")
    add("")

    add("## 1. Human sign-off for this report")
    add("")
    add("The reviewer ticks these on the pull request, then approves and merges it.")
    add("")
    add("- [ ] I read section 2 and every item there has an owner who knows it is theirs today")
    add("- [ ] Any dissent in section 3 is understood, and either fixed or raised as a `SUG-` row")
    add("- [ ] Regenerated views in the diff (`BOOT.md`, `DOC-MAP.yaml`, lifecycle backlog) match the state file")
    add("- [ ] **Kalpana / R12** — freshness verdict noted; if `WARN`/`HALT`, the Governance Sync is scheduled")
    add("")

    add("## 2. Waiting on a human — move these ahead")
    add("")
    add("Nothing here can be closed by an agent. Each line names the authority who owns it.")
    add("")
    add("### 2.1 Stage gates at CANDIDATE awaiting human verdicts")
    add("")
    if signoffs:
        add("| Workstream | Gate | Transition | Human verdicts still owed |")
        add("|---|---|---|---|")
        for s in signoffs:
            add(f"| {s['workstream']} | `{s['gate']}` | {s['from']} → {s['to']} | {', '.join(s['missing'])} |")
        add("")
        add("Evidence: [`GATE-EVIDENCE.yaml`](../state/GATE-EVIDENCE.yaml) · "
            "`python3 scripts/governance/autopilot.py propose-transition --workstream <WS>`")
    else:
        add("_None._")
    add("")

    add("### 2.2 Change requests awaiting ratification or a signature")
    add("")
    if crs:
        add("| CR | Summary | Decision | Owed |")
        add("|---|---|---|---|")
        for c in crs:
            add(f"| {c['id']} | {cell(c['summary'])} | {cell(c['decision'])} | {cell(c['owed'])} |")
        add("")
        add("Source: [`DECISION-REGISTER.md` §3](../registers/DECISION-REGISTER.md)")
    else:
        add("_None._")
    add("")

    add("### 2.3 External dependencies past their required-by date")
    add("")
    if deps:
        add("| Dependency | What is owed | Chase owner | Required by | Overdue |")
        add("|---|---|---|---|---|")
        for d in deps:
            add(f"| {d['id']} | {cell(d['what'])} | {cell(d['owner'])} | {d['due']} | **{d['days']}d** |")
        add("")
        add("Rule DEP-3: a past follow-up date is a hope, not a tracked dependency — re-date it against "
            "its owner or record the refusal. Source: [`DEPENDENCY-REGISTER.md` §2](../registers/DEPENDENCY-REGISTER.md)")
    else:
        add("_None._")
    add("")

    add("### 2.4 Gate-criterion blockers past their follow-up date")
    add("")
    if blockers:
        add("| Gate | Criterion | Blocker | Owner | Follow-up | Overdue |")
        add("|---|---|---|---|---|---|")
        for b in blockers:
            add(f"| `{b['gate']}` | {b['criterion']} | `{b['blocker']}` | {cell(b['owner'])} | {b['follow_up']} | **{b['days']}d** |")
    else:
        add("_None._")
    add("")

    add("### 2.5 State file, ratification and stale artefacts")
    add("")
    items = state_items + [f"HALT — {h}" for h in halts] + [f"WARN — {w}" for w in warns]
    add("\n".join(f"- {i}" for i in items) if items else "_None._")
    add("")

    add("### 2.6 Suggestions escalated to a human decision")
    add("")
    add("\n".join(f"- `{e['id']}` — {e['summary']}" for e in escalations) if escalations else "_None._")
    add("")

    add("## 3. AIGEM board concurrence (automated)")
    add("")
    add("Each seat concurs only when the checks in its own domain pass. One dissent blocks unanimity.")
    add("")
    add("| Seat | Verdict | Basis | Dissent reason |")
    add("|---|---|---|---|")
    for v in verdicts:
        add(f"| {v.board} | {'CONCUR' if v.concur else '**DISSENT**'} | {v.basis} | "
            f"{cell('; '.join(v.reasons)) if v.reasons else '—'} |")
    add("")

    add("## 4. Checks run")
    add("")
    add("| Check | Command | Exit | Result |")
    add("|---|---|---|---|")
    for c in results.values():
        shown = " ".join(Path(p).name if i == 0 else p for i, p in enumerate(c.cmd))
        add(f"| {c.title} | `{shown}` | {c.code} | {'pass' if c.passed else '**fail**'} |")
    if regen_failures:
        add("")
        add("Regeneration failures:")
        add("\n".join(f"- {f}" for f in regen_failures))
    add("")

    add("## 5. What this run changed, and what it may not")
    add("")
    add("- **Changed:** this report, the generated `BOOT.md` block, `DOC-MAP.yaml` and the generated "
        "lifecycle backlog — all derived from the state file.")
    add("- **Never changed:** `CURRENT-STATE.yaml` (including `state_as_of`), `GATE-EVIDENCE.yaml`, "
        "registers and change requests. Those are updated by their owners in their own pull requests.")
    if drifted:
        add("- **Drift found on `main` and regenerated here:** " + ", ".join(f"`{d}`" for d in drifted)
            + ". Someone changed the state file without regenerating its views.")
    add("")
    return title, "\n".join(L)


def cell(text: str) -> str:
    return text.replace("|", "\\|")


def pr_body(report: str, blob_url: str) -> str:
    """The report with repository-relative links made absolute, so they work in a PR body."""
    body = report.replace("](../../../", f"]({blob_url}").replace("](../", f"]({blob_url}docs/governance/")
    return (body + "\n---\n_Opened by the scheduled `governance-daily` workflow. "
            "A human reviews, ticks section 1, approves and merges. The workflow never approves._\n")


def blob_url(state: dict) -> str:
    server = os.environ.get("GITHUB_SERVER_URL", "https://github.com")
    repo = os.environ.get("GITHUB_REPOSITORY") or state.get("project", {}).get("repository", "")
    return f"{server}/{repo}/blob/main/"


# --------------------------------------------------------------------------- main

def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--today", type=dt.date.fromisoformat, default=dt.datetime.now(dt.timezone.utc).date())
    ap.add_argument("--write", action="store_true", help="regenerate views and write the report")
    ap.add_argument("--pr-body", type=Path, help="write the pull-request body here")
    ap.add_argument("--pr-title", type=Path, help="write the pull-request title here")
    args = ap.parse_args(argv)

    regen_failures, drifted = regenerate_views() if args.write else ([], [])

    results = {c.key: c for c in checks()}
    for c in results.values():
        freshness_cmd = c.cmd + (["--today", args.today.isoformat()] if c.key == "freshness" else [])
        c.code, c.output = run(freshness_cmd)

    state = yaml.safe_load(STATE.read_text(encoding="utf-8"))
    bundle = yaml.safe_load(EVIDENCE.read_text(encoding="utf-8"))
    signoffs, blockers = gate_actions(bundle, args.today)
    verdicts = concurrence(results, regen_failures, non_human_approvals(bundle))
    title, report = render(
        args.today, results, verdicts, state, signoffs, blockers,
        pending_change_requests(DECISIONS.read_text(encoding="utf-8")),
        overdue_external_dependencies(DEPENDENCIES.read_text(encoding="utf-8"), args.today),
        escalated_suggestions(SUGGESTIONS.read_text(encoding="utf-8")),
        regen_failures,
        drifted,
    )

    if args.write:
        REPORT.write_text(report + "\n", encoding="utf-8")
        run(["git", "add", "--intent-to-add", str(REPORT.relative_to(ROOT))])  # DOC-MAP routes tracked files only
        code, out = run([PY, "scripts/context/build-doc-map.py"])  # route the new report bytes
        if code != 0:
            print(out, file=sys.stderr)
            return 1
    else:
        print(report)
    if args.pr_body:
        args.pr_body.write_text(pr_body(report, blob_url(state)), encoding="utf-8")
    if args.pr_title:
        args.pr_title.write_text(title + "\n", encoding="utf-8")
    print(title, file=sys.stderr)
    # The report is the deliverable even on dissent; the workflow still opens the PR so a
    # human sees it. Only a failure to produce the report fails the run.
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
