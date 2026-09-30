# Autopilot proposals directory

**Control:** CR-010 SEC-C1 / E-01 / R-C1  
**Date:** 2026-09-30  
**signature_status:** `AI-DRAFTED — repository control implemented; human Security signature outstanding`

This directory is the **only** legal write target for `scripts/governance/autopilot.py --output`.

## Rules

1. Autopilot may write a `STAGE_TRANSITION_CANDIDATE` YAML file here.
2. Autopilot may never write under `docs/governance/state/` or `docs/governance/change-requests/`.
3. Paths containing `..` or resolving through a symlink are refused.
4. Every write and every refusal is appended to `alarms.jsonl` (gitignored) and printed as a `GOVERNANCE ALARM` on stderr.
5. A proposal here is **not** an approval. `may_mark_passed` is always `false`. Human PASS is a separate state-transition change ([04-STAGE_GATES.md §5](../../04-STAGE_GATES.md#5-who-may-declare-a-transition)).

A file existing in this directory records that automation *proposed*. It never records that a board *approved*.
