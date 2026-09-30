# Evidence Collection Runbook

**Owner:** Swapnali / QA + criterion owners  
**CR-010 Q-C1 / OPS-C9**  
**signature_status:** `AI-DRAFTED`

## Rules

1. E4 requires a **run ID**. E3 requires an **executed report**. E2 requires a **named human artefact**. A workflow *file* is none of these (Q-C1).
2. When attaching evidence, set `last_verified_at` on that criterion (OPS-C9). A permanently null timestamp is decoration.
3. Agents may record PARTIAL when evidence is assembled. Agents may mark CANDIDATE when all criteria are MET (SG-2). Agents never mark MET, WAIVED, or PASSED unless a named human verifier already did — this PR does not.
4. Do not close GATE-P4 4.1/4.3/4.6 on documentation. Those remain blocked on 1SB sandbox, UAT slot, and perf harness.
5. Autopilot proposals live under `docs/governance/autopilot/proposals/` and are not approvals.

## How to attach

Edit `docs/governance/state/GATE-EVIDENCE.yaml` in a dedicated PR owned by the criterion owner. Cite the run URL or report path. Do not invent IDs.
