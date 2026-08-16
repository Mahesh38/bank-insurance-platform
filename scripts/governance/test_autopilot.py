#!/usr/bin/env python3

from __future__ import annotations

import copy
import importlib.util
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("autopilot.py")
SPEC = importlib.util.spec_from_file_location("aigem_autopilot", SCRIPT)
assert SPEC and SPEC.loader
autopilot = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(autopilot)


class AutopilotSafetyTests(unittest.TestCase):
    def setUp(self) -> None:
        self.bundle = autopilot.load_bundle()

    def test_blocked_work_is_never_selected(self) -> None:
        selected = autopilot.select_next(self.bundle, "WS-1")
        self.assertIsNotNone(selected)
        self.assertNotEqual("BLOCKED", selected["state"])
        self.assertEqual("AUTOMATABLE", selected["execution_mode"])
        self.assertEqual("4.2", selected["id"])

    def test_manual_work_requires_explicit_selection(self) -> None:
        selected = autopilot.select_next(self.bundle, "WS-1", include_manual=True)
        self.assertIsNotNone(selected)
        self.assertEqual("4.4", selected["id"])
        self.assertEqual("HUMAN_REQUIRED", selected["execution_mode"])

    def test_incomplete_gate_refuses_transition(self) -> None:
        with self.assertRaises(autopilot.AutopilotRefusal):
            autopilot.candidate_proposal(self.bundle, "WS-1")

    def test_complete_gate_still_only_proposes_candidate(self) -> None:
        complete = copy.deepcopy(self.bundle)
        stream = complete["workstreams"][0]
        for criterion in stream["criteria"]:
            criterion["state"] = "MET"
            criterion["evidence"] = [f"evidence:{criterion['id']}"]
            criterion["blockers"] = []
        proposal = autopilot.candidate_proposal(complete, "WS-1")
        self.assertEqual("CANDIDATE", proposal["state"])
        self.assertTrue(proposal["human_approval_required"])
        self.assertFalse(proposal["may_mark_passed"])
        self.assertTrue(proposal["missing_human_approvals"])

    def test_unsafe_policy_is_rejected(self) -> None:
        unsafe = copy.deepcopy(self.bundle)
        unsafe["policy"]["silence_approves"] = True
        with self.assertRaises(autopilot.AutopilotRefusal):
            autopilot.validate_policy(unsafe["policy"])


if __name__ == "__main__":
    unittest.main()
