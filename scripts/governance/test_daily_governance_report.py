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


briefs = daily.decision_briefs


def criterion(cid, state="MET", level="E4", verifier="ci", verified="2026-09-20", evidence=("x",), owner="Amit"):
    return {"id": cid, "state": state, "required_evidence_level": level, "verifier": verifier,
            "last_verified_at": verified, "evidence": list(evidence), "owner": owner}


def signoff(criteria, missing=("Architect",), to="S09 — Platform"):
    return {"workstream": "WS-3", "gate": "GATE-S08", "from": "S08", "to": to,
            "missing": list(missing), "stream": {"criteria": criteria}}


class DecisionBriefs(unittest.TestCase):
    def test_gate_all_met_recent_ci_is_approve_with_expiry_condition(self):
        b = briefs.gate_brief(signoff([criterion("G1"), criterion("G2")]), TODAY, "")
        self.assertEqual(b.suggestion, "APPROVE WITH CONDITIONS")  # RG-8 sign-by date is always stated
        self.assertIn("2026-10-20", b.conditions[0])

    def test_gate_weak_evidence_is_named(self):
        b = briefs.gate_brief(signoff([criterion("G1"), criterion("G8", level="E2", verifier="document")]), TODAY, "")
        self.assertIn("G8 (E2, document)", " ".join(b.conditions))

    def test_gate_expired_or_missing_evidence_needs_validation(self):
        old = briefs.gate_brief(signoff([criterion("G1", verified="2026-08-01")]), TODAY, "")
        self.assertEqual(old.suggestion, "APPROVE AFTER VALIDATION")
        bare = briefs.gate_brief(signoff([criterion("G1", evidence=())]), TODAY, "")
        self.assertEqual(bare.suggestion, "APPROVE AFTER VALIDATION")

    def test_gate_with_unmet_criterion_is_not_approvable(self):
        b = briefs.gate_brief(signoff([criterion("G1"), criterion("G2", state="OPEN")]), TODAY, "")
        self.assertEqual(b.suggestion, "DO NOT APPROVE YET")

    def test_gate_names_what_blocks_the_next_stage(self):
        deps = "| DEP-cst | `GATE-S09` entry | `blocked_by` | Cost envelope | DECISION | OPEN | x |"
        b = briefs.gate_brief(signoff([criterion("G1")]), TODAY, deps)
        self.assertIn("DEP-cst", " ".join(b.cons))

    def test_gate_tells_each_authority_what_to_check(self):
        b = briefs.gate_brief(signoff([criterion("G1", owner="Amit / Engineering")],
                                      missing=("Engineering", "Architect")), TODAY, "")
        self.assertIn("**Engineering (Amit)**: check G1", b.context)
        self.assertIn("**Architect (Mahesh)**: no criterion owned", b.context)

    def test_counter_signature_on_an_approved_cr(self):
        cr = {"id": "CR-999", "date": "2026-08-10", "summary": "s", "decision": "APPROVED 2026-08-10",
              "owed": "Mahesh (Solution Architect) — PO + QA Lead counter-signature outstanding"}
        b = briefs.cr_brief(cr, TODAY)
        self.assertEqual(b.suggestion, "COUNTER-SIGN")
        self.assertEqual(b.owner, "Swapnali (QA Lead), Rajal (PO)")  # Mahesh already signed
        self.assertIn("RG-8", " ".join(b.conditions))                  # 50 days old
        self.assertIn("no separate CR file", b.record_in)

    def test_transcribed_cr_leans_to_ratify_with_owed_authorities_as_conditions(self):
        cr = {"id": "CR-999", "date": "2026-09-23", "summary": "s",
              "decision": "CANDIDATE — L1 files transcribed under ADMIT-BYPASS",
              "owed": "Architecture + Product human ratification outstanding"}
        b = briefs.cr_brief(cr, TODAY)
        self.assertEqual(b.suggestion, "RATIFY WITH CONDITIONS")
        self.assertIn("Mahesh (Architecture), Rajal (Product)", b.conditions[0])

    def test_unused_cr_without_drafts_is_validated_first(self):
        cr = {"id": "CR-999", "date": "2026-09-25", "summary": "s", "decision": "PENDING RATIFICATION",
              "owed": "Product pending"}
        self.assertEqual(briefs.cr_brief(cr, TODAY).suggestion, "APPROVE AFTER VALIDATION")

    def test_verdict_pack_positions_and_conditions_drive_the_cr_brief(self):
        import tempfile
        with tempfile.TemporaryDirectory() as tmp:
            pack = Path(tmp) / "CR-999" / "verdicts"
            pack.mkdir(parents=True)
            (pack / "board-4-security-deepali.md").write_text(
                "# Draft\n> Draft: `APPROVE-WITH-MODIFICATION`\n\n## 5. Conditions\n\n"
                "1. **Pin actions.** Pin to SHAs so a tag\n   cannot move under us.\n2. Rotate keys.\n\n## 6. End\n")
            (pack / "board-6-compliance-shailja.md").write_text(
                "Draft verdict: REJECT\n\n## Conditions\n\n| # | Condition |\n|---|---|\n"
                "| **CMP-C1** | Retain seven years. More text here. |\n")
            original = briefs.CR_DIR
            briefs.CR_DIR = Path(tmp)
            try:
                drafts = briefs.verdict_pack("CR-999")
                cr = {"id": "CR-999", "date": "2026-09-25", "summary": "s", "decision": "PENDING RATIFICATION",
                      "owed": "Security and Compliance outstanding"}
                b = briefs.cr_brief(cr, TODAY)
            finally:
                briefs.CR_DIR = original
        self.assertEqual([d["seat"] for d in drafts], ["Deepali (Security)", "Shailja (Compliance)"])
        self.assertEqual(drafts[0]["conditions"], ["Pin actions.", "Rotate keys."])
        self.assertEqual(drafts[1]["conditions"], ["CMP-C1: Retain seven years."])
        self.assertEqual(b.suggestion, "REJECT OR REWORK")  # one REJECT outweighs any number of approvals

    def test_dependency_rule_redates_then_escalates(self):
        dep = {"id": "DEP-1", "what": "w", "owner": "o", "due": "2026-09-25", "impact": "i"}
        self.assertEqual(briefs.dependency_brief({**dep, "days": 4}).suggestion, "RE-DATE")
        self.assertEqual(briefs.dependency_brief({**dep, "days": 11}).suggestion, "ESCALATE")

    def test_state_briefs_name_newer_crs_and_the_counter_signature(self):
        state = {"state_as_of": "2026-09-13", "review_due": "2026-10-11",
                 "ratified_by": "Mahesh, 2026-08-10 — PO counter-signature outstanding"}
        out = briefs.state_briefs(state, TODAY, [{"id": "CR-016", "date": "2026-09-23"}],
                                  ["docs/x.md  15d  (limit 14, owner: Tech Lead)"])
        self.assertEqual([b.suggestion for b in out],
                         ["RE-CONFIRM WITH CONDITIONS", "COUNTER-SIGN", "REVIEW AND TOUCH"])
        self.assertIn("CR-016 (2026-09-23)", " ".join(out[0].conditions))
        self.assertEqual(out[2].owner, "Tech Lead")

    def test_rendered_brief_is_a_draft_with_tick_boxes_and_a_filing_place(self):
        b = briefs.dependency_brief({"id": "DEP-1", "what": "w", "owner": "o", "due": "2026-09-25",
                                     "impact": "i", "days": 11})
        text = "\n".join(briefs.render([b], "3"))
        self.assertIn("Suggestions are drafts, not signatures", text)
        self.assertIn("AIGEM suggestion (draft): ESCALATE", text)
        self.assertIn("- [ ] Escalate to: …", text)
        self.assertIn("**File the signature in:**", text)
        self.assertNotRegex(text, r"(?i)\bapproved by aigem\b")


    def test_pr_body_shrinks_only_when_it_must(self):
        sizes = {"full": 70_000, "compact": 40_000, "omit": 10_000}
        self.assertEqual(len(daily.fit_pr_body(lambda m: "x" * sizes[m])), 40_000)
        self.assertEqual(len(daily.fit_pr_body(lambda m: "x" * {"full": 5, "compact": 3, "omit": 1}[m])), 5)
        self.assertEqual(len(daily.fit_pr_body(lambda m: "x" * {"full": 90_000, "compact": 70_000, "omit": 1}[m])), 1)

    def test_compact_briefs_keep_the_decision_and_point_to_the_full_text(self):
        b = briefs.Brief("cr", "CR-1", "t", "Rajal (Product)", "COUNTER-SIGN", "context", "because",
                         pros=["p"], cons=["c"], conditions=["k"], options=["Counter-sign"], record_in="here")
        full = "\n".join(briefs.render([b] * 30, "3"))
        compact = "\n".join(briefs.render([b] * 30, "3", compact=True))
        self.assertLess(len(compact), len(full))
        self.assertIn("- [ ] Counter-sign", compact)
        self.assertIn("DAILY-SIGNOFF.md", compact)
        self.assertNotIn("Reasons to reject", compact)


if __name__ == "__main__":
    unittest.main(verbosity=1)
