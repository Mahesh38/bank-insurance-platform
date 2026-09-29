#!/usr/bin/env python3
"""Tests for daily-governance-report.py: parsers, human-action detection and concurrence."""

from __future__ import annotations

import datetime as dt
import importlib.util
import sys
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("daily", HERE / "daily-governance-report.py")
daily = importlib.util.module_from_spec(spec)
sys.modules["daily"] = daily
spec.loader.exec_module(daily)

TODAY = dt.date(2026, 9, 29)

DECISIONS = """
## 3. Change requests

| ID | Date | Type | Summary | Decision | Approvers |
|----|------|------|---------|----------|-----------|
| CR-001 | 2026-08-10 | STAGE | Add 4.7 | **APPROVED** 2026-08-10 | Mahesh — PO counter-signature outstanding |
| CR-009 | 2026-08-14 | GOV | Recalibrate | **APPROVED** 2026-08-14 | Mahesh / Architect, in full |
| CR-012 | 2026-08-24 | ARCH | R0 robustness | **PENDING RATIFICATION** | Security acceptance outstanding; see [`v`](../x.md) |
| CR-016 | 2026-09-23 | GOV | Parallel lanes | **CANDIDATE** | Architecture + Product outstanding |

### CR-001 — detail
| not | a | CR | row | at | all |
"""

DEPENDENCIES = """
## 2. External dependencies

| ID | Dependency | Owner | Required by | Age | State | Impact if late |
|----|------------|-------|-------------|-----|-------|----------------|
| → [DEP-002](#1-edges) | UAT slot | Rajal / Product | **2026-09-18** | OVERDUE | OPEN | 4.3 |
| → [DEP-cst](#1-edges) | Cost envelope | Kalpana | **2026-10-05** | — | OPEN | S09 |
| → [DEP-old](#1-edges) | Closed thing | Amit | 2026-08-01 | — | RESOLVED | — |

> note
"""

SUGGESTIONS = """
## 2. Register

| ID | Date | Source | Summary | SF | SC | Necessity | Type | P now / target | Action | Ref |
|----|------|--------|---------|----|----|-----------|------|----------------|--------|-----|
| SUG-1 | 2026-09-01 | human | Needs a CR | SF1 | SC4 | MUST | GOV | P2 / P1 | ESCALATED | — |
| SUG-2 | 2026-09-01 | human | Fine | SF1 | SC0 | MUST | GOV | P2 / P1 | ADMITTED | — |
"""

BUNDLE = {
    "workstreams": [
        {"id": "WS-3", "gate_id": "GATE-S08", "state": "CANDIDATE", "current_stage": "S08", "next_stage": "S09",
         "required_approvers": ["Architect", "QA"],
         "approvals": [{"authority": "QA", "reviewer_type": "HUMAN", "decision": "APPROVED"}],
         "criteria": []},
        {"id": "WS-1", "gate_id": "GATE-P4", "state": "BLOCKED", "required_approvers": ["Architect"],
         "approvals": [],
         "criteria": [{"id": "4.1", "owner": "Amit",
                       "blockers": [{"id": "B-1", "owner": "Amit", "follow_up": "2026-09-15"},
                                    {"id": "B-2", "follow_up": "2026-10-15"}]}]},
    ]
}


class Parsers(unittest.TestCase):
    def test_change_requests_pending_or_owed(self):
        ids = [c["id"] for c in daily.pending_change_requests(DECISIONS)]
        self.assertEqual(ids, ["CR-001", "CR-012", "CR-016"])  # CR-009 fully approved; detail table ignored

    def test_overdue_dependencies_recomputed_from_date(self):
        deps = daily.overdue_external_dependencies(DEPENDENCIES, TODAY)
        self.assertEqual([(d["id"], d["days"]) for d in deps], [("DEP-002", 11)])

    def test_escalated_suggestions(self):
        self.assertEqual([e["id"] for e in daily.escalated_suggestions(SUGGESTIONS)], ["SUG-1"])

    def test_gate_signoffs_and_blockers(self):
        signoffs, blockers = daily.gate_actions(BUNDLE, TODAY)
        self.assertEqual(signoffs[0]["missing"], ["Architect"])
        self.assertEqual([b["blocker"] for b in blockers], ["B-1"])

    def test_agent_approval_is_flagged(self):
        bundle = {"workstreams": [{"id": "WS-9", "approvals": [{"authority": "Security", "reviewer_type": "AI"}]}]}
        self.assertEqual(daily.non_human_approvals(bundle), ["WS-9 Security"])
        self.assertEqual(daily.non_human_approvals(BUNDLE), [])

    def test_state_actions(self):
        state = {"state_as_of": "2026-09-13", "review_due": "2026-10-11",
                 "ratified_by": "Mahesh — PO counter-signature outstanding"}
        text = " ".join(daily.state_actions(state, TODAY))
        self.assertIn("16 days old", text)
        self.assertIn("12 days", text)
        self.assertIn("Rajal", text)
        fresh = {"state_as_of": "2026-09-28", "review_due": "2026-12-31", "ratified_by": "Mahesh and Rajal"}
        self.assertEqual(daily.state_actions(fresh, TODAY), [])


def check(key: str, code: int, output: str = "") -> "daily.Check":
    c = daily.Check(key, key, ["x"], ok_below=2 if key == "freshness" else 1)
    c.code, c.output = code, output
    return c


FRESH_OUT = "State structure\n  OK    WS-3\n\nID uniqueness\n  OK    all unique\n"


class Concurrence(unittest.TestCase):
    def results(self, **codes: int) -> dict:
        keys = ["freshness", "freshness-tests", "ci", "context", "context-tests",
                "autopilot-tests", "autopilot", "report-tests"]
        return {k: check(k, codes.get(k.replace("-", "_"), 0), FRESH_OUT if k == "freshness" else "")
                for k in keys}

    def test_unanimous_when_all_pass_and_freshness_warns(self):
        verdicts = daily.concurrence(self.results(freshness=1), [], [])
        self.assertTrue(all(v.concur for v in verdicts))

    def test_halt_makes_delivery_dissent(self):
        verdicts = {v.board.split(" ")[0]: v for v in daily.concurrence(self.results(freshness=2), [], [])}
        self.assertFalse(verdicts["Kalpana"].concur)
        self.assertTrue(verdicts["Mahesh"].concur)

    def test_missing_tool_makes_operations_dissent(self):
        verdicts = {v.board.split(" ")[0]: v for v in daily.concurrence(self.results(ci=127), [], [])}
        self.assertFalse(verdicts["Shivanshi"].concur)
        self.assertFalse(verdicts["Mahesh"].concur)

    def test_manufactured_approval_makes_compliance_dissent(self):
        verdicts = {v.board.split(" ")[0]: v for v in daily.concurrence(self.results(), [], ["WS-9 Security"])}
        self.assertFalse(verdicts["Shailja"].concur)

    def test_report_never_claims_approval(self):
        results = self.results(freshness=1)
        verdicts = daily.concurrence(results, [], [])
        state = {"state_as_of": "2026-09-13", "review_due": "2026-10-11", "ratified_by": "x outstanding",
                 "project": {"repository": "o/r"}}
        signoffs, blockers = daily.gate_actions(BUNDLE, TODAY)
        title, report = daily.render(TODAY, results, verdicts, state, signoffs, blockers,
                                     daily.pending_change_requests(DECISIONS),
                                     daily.overdue_external_dependencies(DEPENDENCIES, TODAY), [], [])
        self.assertIn("UNANIMOUS CONCUR", title)
        self.assertIn("never satisfies a T4 human sign-off", report)
        self.assertIn("| WS-3 | `GATE-S08` | S08 → S09 | Architect |", report)
        body = daily.pr_body(report, "https://github.com/o/r/blob/main/")
        self.assertIn("(https://github.com/o/r/blob/main/docs/governance/state/GATE-EVIDENCE.yaml)", body)
        self.assertNotIn("](../", body)


if __name__ == "__main__":
    unittest.main(verbosity=1)
