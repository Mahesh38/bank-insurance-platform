# Security controls (CR-010 / CR-012)

**Owner:** Deepali / Security  
**signature_status:** `AI-DRAFTED — T4 outstanding; no residual risk accepted`

## Controls implemented 2026-09-30

| Control | Mechanism |
|---|---|
| Protected paths | `/CODEOWNERS` |
| Autopilot write confinement | `resolve_proposal_output` + tests |
| Approval boundary | proposal-only policy; no PASSED flag |
| Alarm | `GOVERNANCE ALARM` + `alarms.jsonl` |
| Workflow failure owner | Shivanshi / SRE |
| Render.com | standing constraint — never a PII path |
| SEC-OPEN-7 | review date **2026-10-28**; IPS remains alert-mode until Deepali accepts drop |
| SEC-OPEN-8 | exemption destinations = `{1SB}`; not "all mTLS" |
| Allowlist curator | Shivanshi / SRE; destination+code in one PR |

## Compliance constraints (CR-012 Board 6)

Standing constraints (now in CURRENT-STATE.yaml):

1. No regulatory evidence exists only in a topic.
2. OpenSearch holds no regulatory evidence.

Topic retention extension requires a Board 6 conversation. Audit consumer IAM stays INSERT-only (`91-grants.sql`). Enterprise SIEM migration re-signs the exclusion.
