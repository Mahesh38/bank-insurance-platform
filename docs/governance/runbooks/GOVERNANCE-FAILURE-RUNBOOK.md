# Governance Failure Runbook

**Owner:** Shivanshi / SRE + Kalpana / R12  
**CR-010 OPS-C2**  
**signature_status:** `AI-DRAFTED`

## A. FreshnessCheck failed

```bash
java scripts/governance/FreshnessCheck.java
```

| Exit | Meaning | Action |
|---|---|---|
| 0 | Fresh | Proceed |
| 1 | WARN | Disclose in the reply. Do not ignore. Typical: `state_as_of` weekly lag, or DEPENDENCY-REGISTER > 14d. Review and touch, or schedule Governance Sync. **Agents may still admit work.** |
| 2 | HALT | **Do not admit new work** (Rule CS-1). Park/reject only. Page Kalpana / R12 to refresh `CURRENT-STATE.yaml` (dates only unless Architect + PO move stage fields). |

Never hand-edit generated `BOOT.md`. After a legitimate state edit: `python3 scripts/context/build-boot-capsule.py`.

## B. Backlog drift detected

CI step `generate-backlog.py` + `git diff --exit-code` on `docs/application-lifecycle-bible/backlog/`.

**Fix:** regenerate and commit. **Never hand-edit** the generated backlog. Same rule as BOOT.md and DOC-MAP.yaml.
