# Security Compliance Validation Report — 2026-09-30

**Boards:** Deepali (R8), Shailja (R9) — drafted for their review.  
**signature_status:** `AI-DRAFTED — T4 Security and Compliance signatures outstanding`

## CODEOWNERS

Root [`CODEOWNERS`](../../../../CODEOWNERS) requires `@Mahesh38` on:

- `docs/governance/state/`
- `docs/governance/change-requests/`
- `.github/workflows/`
- `scripts/governance/autopilot.py` and `docs/governance/autopilot/`

This is merge protection, not a persona substitute. A CODEOWNERS approval is not a T4 signature.

## Protected governance files

| Path | Protection |
|---|---|
| `CURRENT-STATE.yaml` | Agent rule: no `current_phase` / `stage_status`. CODEOWNERS. Autopilot write refused. |
| `GATE-EVIDENCE.yaml` | Proposal-only policy. Owner string fix for 4.5 only in this PR. |
| Change requests | CODEOWNERS. Autopilot write refused. |
| Workflows | CODEOWNERS. Concurrency + failure alarm added. |

## Autopilot output restrictions

`resolve_proposal_output()` allows only `docs/governance/autopilot/proposals/`. Refuses `..`, symlinks, and any path under state/ or change-requests/. Tests in `test_autopilot.py`. Policy remains `proposal-only`, `silence_approves: false`, `automatic_waivers: false`.

## Approval boundary enforcement

`candidate_proposal()` always sets `may_mark_passed: false` and lists `missing_human_approvals`. There is no CLI flag to mark PASSED, populate an approval, or extend a waiver. Attempts to write a proposal outside the legal directory raise `GOVERNANCE ALARM`.

## Human signoff requirements

T4 Architecture, Security, and Risk & Compliance cannot be satisfied by this PR. Remaining checklist: [`README.md` §7](./README.md).

## SEC-OPEN dates (CR-012)

| Item | Date | Owner | Status |
|---|---|---|---|
| SEC-OPEN-7 IPS alert→drop review | **2026-10-28** | Deepali + Shivanshi | Open — **not accepted** by this agent |
| SEC-OPEN-8 mTLS inspection exemption | Destinations: **1SB only** | Deepali | Open — **not accepted**; must not be generalised |

## Partial / blocked security conditions

SEC-C3 full-history secret scan attestation, SEC-C4 Render.com PII fact, SEC-C6 SHA-pin, SEC-C7 pip hashes, SEC-C10 human threat-model signature — not claimed done.
