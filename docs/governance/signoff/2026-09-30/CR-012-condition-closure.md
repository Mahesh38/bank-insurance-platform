# CR-012 — Condition Closure Report (AIGEM-ACCEPTED 2026-09-30)

**Change request:** [`CR-012-r0-platform-robustness.md`](../../change-requests/CR-012-r0-platform-robustness.md)  
**AIGEM suggestion:** APPROVE WITH CONDITIONS.  
**signature_status:** `AI-DRAFTED — T4 Architecture/Security/Compliance and SRE/DBA/Delivery signatures outstanding. No residual-risk acceptance fabricated.`

ADRs 009–013 remain Proposed (`A3_JOINT_REVIEW`). This report implements internally achievable conditions and documents the rest.

## Completed in-repo

| Condition | Evidence |
|---|---|
| SEC-OPEN-7 carries a **date**, not a stage | Review date **2026-10-28**; owner Shivanshi / SRE + Deepali; alert→drop is unsigned (SECURITY-CONTROLS) |
| SEC-OPEN-8 scoped to **enumerated mTLS destinations**, not a pattern | 1SB only, recorded as destination list; general "mTLS is exempt" is forbidden |
| Two evidence exclusions are **standing constraints** | CURRENT-STATE.yaml standing_constraints (ADR-012 / ADR-013 sentences) |
| FF-26 and FF-28 named as GATE-S09 evidence | CI-EVIDENCE + this report; a red result blocks S09 (not waived) |
| Runbooks per new tier (design) | OPERATIONAL-RUNBOOK sections: broker, cache, search, firewall, connectivity |
| Three drills scheduled at **start** of S09, not the end | OPERATIONAL-RUNBOOK drill calendar (NFR-NET-01, NFR-EVT-03, NFR-CAC-01/02) — dates are plan placeholders, not executed drills |
| Outbox physical design (Aarti condition 1) | [`OUTBOX-PHYSICAL-DESIGN.md`](../../controls/OUTBOX-PHYSICAL-DESIGN.md) |
| NFR-THR-06 recomputed with publisher/consumers at KEDA max | [`SCALABILITY-NFR-THR-06.md`](../../controls/SCALABILITY-NFR-THR-06.md) |
| DR references | [`DISASTER-RECOVERY.md`](../../controls/DISASTER-RECOVERY.md) |
| Cost assumption notes | [`COST-ASSUMPTIONS.md`](../../controls/COST-ASSUMPTIONS.md) — envelope **not** approved |
| Firewall allowlist curator named | Shivanshi / SRE; every destination change is a PR (SECURITY-CONTROLS) |
| No prod control lowered for RISK-012 | Restated; not a Security acceptance |

## Partial

| Condition | Remaining |
|---|---|
| DLQ retention/IAM before W3 | Documented requirement; no MSK in this repo yet |
| Valkey AUTH rotation in NFR-SEC-07 drill | Documented; drill not executed |
| Cache TTL/maxmemory before uat | Documented defaults; not applied in AWS |
| ASM-011 proven by NFR-EVT-03 | Drill not executed (S09) |
| Alert routing for outbox/lag/firewall/cache | Runbook named the signals; no production alerts exist |
| Broker/cache upgrade windows with Amit | To agree before W3 |

## Blocked external (unchanged, OPEN)

| Condition | Dependency |
|---|---|
| Bank-side VPN contact + first response | DEP-20260824-dx1 — Bank / Network Team |
| Elastic IP / Apigee list confirmed in writing | DEP-20260824-eip — 1SB + AU Bank PG |
| NFR-OPEN-6 cost envelope before uat apply | DEP-20260824-cst |
| DX order / firewall / prefixes | Network Team, External Firewall Changes, VPN Provisioning |

No UAT date is communicated by this PR.

Human ticks (not completed here):

- [ ] Deepali — Approve with conditions (may not be done by an agent; SEC-OPEN-7/8 are *her* acceptances)
- [ ] Shailja — Approve with conditions
- [ ] Shivanshi — Approve with conditions
- [ ] Aarti — Approve with conditions
- [ ] Kalpana — Approve with conditions
- [ ] Mahesh — T4 Architecture
