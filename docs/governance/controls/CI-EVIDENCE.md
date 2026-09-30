# CI evidence and GATE-S09 fitness-function references

**signature_status:** `AI-DRAFTED`

## Application CI (Q-C2 / S08-G1)

`.github/workflows/application-ci.yml` runs on every pull request and every branch push, no path filter, concurrency `application-ci-${{ github.ref }}` with `cancel-in-progress: true`. S08-G1 is already **MET** in `GATE-EVIDENCE.yaml` with run IDs (tip recorded 2026-09-13). This PR does not re-declare it.

## Governance CI

`.github/workflows/governance.yml` now has a matching concurrency group. Failure routes to Shivanshi / SRE.

## GATE-S09 machine checks (CR-012)

When S09 evidence is collected, a red result **blocks** the gate:

| FF | Meaning |
|---|---|
| FF-22 | Egress inspection coverage |
| FF-23 | Cache is not idempotency/SoR |
| FF-24 | (related cache/config TTL) |
| FF-26 | No regulatory evidence only in a topic — also a standing constraint |
| FF-27 / FF-28 | OpenSearch is not evidence; audit pipe cannot be reached from the log pipe |

They are not MET in this PR. Listing them here prevents them from slipping into "backlog items".
