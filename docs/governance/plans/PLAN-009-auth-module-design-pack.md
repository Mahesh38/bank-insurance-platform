# PLAN-009 — Auth module (Login BRD) R0 design pack

```yaml
# schema: implementation-plan
id: PLAN-009
work_item: EPIC-006
origin: SUG-20261005-amp
workstream: WS-2
risk_tier: T3
author: "agent:cursor (persona: Mahesh)"
date: "2026-10-05"

objective: >
  After this change engineers have a BRD-traced authentication and authorization
  module design pack — HLD, sequences, API LLD/OpenAPI extensions, flows and
  algorithms — constrained to R0, reusing ARCH-029 / ADR-022, with Login BRD
  conflicts named OPEN (especially ID-11 ceremony).

problem: >
  Architect was asked to design the authentication and authorisation module from
  the Login BRD (DOC-005) from scratch: HLD, sequences, API, flows and algorithms.
  ARCH-029 already answers Keycloak-collapse and publishes OIDC/token-hiding
  contracts. Without a BRD-traced pack, GATE-IAM-P1 A.1 invents Captcha/OTP/Unlock
  or adds a forbidden BFF login-password field. Evidence:
  Login_Module_BRD_Detailed_CONTEXT.md; LOGIN-BFF-FIGMA-EVALUATION §4; ADR-022.

proposed_solution: >
  Admit EPIC-006 on the WS-2 Mahesh lane (ARCH-029 becomes a child). Publish
  docs/platform/authentication-authorization/20..23 and extend the three existing
  OpenAPI files. Map BRD §4.1 to IN / DEFER / OPEN. Do not rewrite ARCH-029.
  Do not add POST /credentials. Do not implement services. Do not claim T4.

alternatives:
  - option: "Redesign auth from scratch including a new public password-collecting BFF"
    rejected_because: "ID-11 / OPEN-AUTH-CEREMONY is A3_JOINT_REVIEW; standing constraint forbids tokens on the device; LOGIN-BFF-FIGMA §4 forbids assuming option B."
  - option: "Treat SUG-20261005-amp as a duplicate of SUG-20261002-iap"
    rejected_because: "iap is Keycloak-collapse justification; this input is the Lead-style BRD module pack."
  - option: "Wait until Deepali closes ID-11 before writing any BRD algorithms"
    rejected_because: "OTP, lock, Unlock User and partner password are ceremony-agnostic MUST rules; only the login-password host is blocked."
  - option: "Implement the full BRD including Captcha admin and dashboard"
    rejected_because: "BRD §4.2 and BOOT out_of_scope_now."

affected_components:
  - docs/platform/authentication-authorization (HLD, sequences, API LLD, algorithms, OpenAPI, work items)
  - docs/architecture/README.md
  - docs/platform/README.md
  - docs/governance plans and registers
  - docs/governance/state/CURRENT-STATE.yaml (EPIC counter only)

files_expected:
  - docs/platform/authentication-authorization/20-auth-module-hld.md
  - docs/platform/authentication-authorization/21-auth-module-sequences.md
  - docs/platform/authentication-authorization/22-auth-module-api-lld.md
  - docs/platform/authentication-authorization/23-auth-module-flows-and-algorithms.md
  - docs/platform/authentication-authorization/workforce-access-bff.openapi.yaml
  - docs/platform/authentication-authorization/identity-provider-adapter.openapi.yaml
  - docs/platform/authentication-authorization/identity-authorization.openapi.yaml
  - docs/platform/authentication-authorization/EPIC-006.work-item.yaml
  - docs/platform/authentication-authorization/ARCH-030.work-item.yaml
  - docs/platform/authentication-authorization/ARCH-031.work-item.yaml
  - docs/platform/authentication-authorization/ARCH-032.work-item.yaml
  - docs/platform/authentication-authorization/DOC-024.work-item.yaml
  - docs/governance/plans/PLAN-009-auth-module-design-pack.md
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/architecture/README.md
  - docs/governance/state/CURRENT-STATE.yaml
  - docs/context/DOC-MAP.yaml

data_changes: none
api_changes: "additive documentation of unpublished captcha/OTP/unlock/partner-password paths; no login-password body; no runtime"
security_impact: "documents platform OTP, lock and Unlock User; does not change enforcement; ID-11 remains open; Deepali owns Board 4"
compliance_impact: "audit events listed per BRD §10; no consent change"
backward_compatibility: "compatible — additive docs; ARCH-029 OIDC paths unchanged in request shape"
performance_impact: none
operational_impact: none

testing:
  unit: []
  integration: []
  other:
    - "Cross-doc consistency: HLD R0 cut ↔ algorithms ↔ OpenAPI operation set"
    - "Every BRD §4.1 row classified IN / DEFER / OPEN"
    - "BFF OpenAPI has no password property on LoginRequest"
    - "python3 scripts/context/build-doc-map.py after new docs"
    - "No CURRENT-STATE stage field edit"

rollback: >
  Revert the documentation commit. No runtime or data impact.

dependencies:
  - ARCH-029
  - ADR-020
  - ADR-022
  - ADR-017
assumptions: []
risks:
  - risk: "Readers treat PENDING_OTP as already implemented in workforce-access-bff"
    mitigation: "HLD §5 states UC-01 runtime still mints the old session until a GATE slice"
  - risk: "Someone implements option B from the sequence diagram"
    mitigation: "Diagram labelled OPEN / FORBIDDEN until ID-11; OpenAPI omits the path"

acceptance_criteria:
  - "AC-1 HLD boundaries + R0 cut + OPEN conflicts"
  - "AC-2 sequences for login OTP / lock / unlock / partner password"
  - "AC-3 OpenAPI extensions present; no BFF login-password"
  - "AC-4 algorithms trace to BR-* / KBR-* / VAL-*"
  - "AC-5 EPIC counter only in CURRENT-STATE"

out_of_scope:
  - "Runtime BFF / adapter / PDP / Flutter code"
  - "Public login-password API"
  - "Forgot Password / mPIN / Bank RM password reset"
  - "Physical schema"
  - "Human T4 signatures"
  - "Silent Product or Security decisions"

estimate: L

reviews: []
variance_log: []
```

## Board notes (agent self-review — not T4)

| Board | Provisional | Note |
|---|---|---|
| Architecture | self_review | Smallest additive pack; reuses ARCH-029; ceremony conflict raised not resolved |
| Product | needed | OPEN-AUTH-DASHBOARD and Unlock intent UX (table 14) are Product |
| Security | needed at T3 human | Platform OTP + lock + Unlock. ID-11 / Captcha / session limits stay Deepali |
| Compliance | needed at T3 human | Audit catalogue; partner password not stored in platform plaintext |
| QA | advisory | Algorithms become AC sources for GATE-IAM-P1 A.1 |
| SRE | N/A | Docs only |
| Engineering | advisory | Extended OpenAPI is the A.1 scaffold input |
