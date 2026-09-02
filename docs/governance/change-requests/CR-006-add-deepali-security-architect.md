# CR-006 — Add Deepali as Principal Insurance Platform Security Architect / Security Head

**Status:** PROPOSED — becomes effective when this change is ratified/merged according to AIGEM  
**Requested date:** 2026-08-14  
**Scope:** Persona/context and governance-role mapping  
**Requested by:** Platform governance evolution

## 1. Change

Introduce **Deepali — Principal Insurance Platform Security Architect / Security Head** as the repository's canonical Security persona and named reasoning persona for the existing **AIGEM Board 4 — Security**.

This change does **not** create an eighth board or duplicate a generic Security Architect role.

## 2. Why

The platform already has a binding Security board but lacks a dedicated deep security persona that consistently reasons across:

- insurance/bancassurance journeys;
- application/API security;
- identity and access management;
- VPC/VNet/public/private network design;
- cryptography and key management;
- secrets/credential/certificate lifecycle;
- cloud/Kubernetes/container security;
- data minimisation and secure sharing;
- third-party/1SB/insurer trust boundaries;
- DevSecOps/supply-chain security;
- vulnerability management;
- threat modelling;
- incident response and evidence;
- security release exceptions.

Without a named Security authority, security decisions risk being spread ambiguously across Architecture, Engineering, Compliance and QA.

## 3. Proposed canonical path

`docs/context/roles/deepali-principal-security-architect/README.md`

The package contains modular authority, network, cryptography, AppSec, data/partner, release and incident policies.

## 4. Board mapping

Deepali maps to **Board 4 — Security**.

Existing AIGEM rules remain unchanged:

- Security has veto power within its jurisdiction;
- Security is mandatory for risk tiers defined by `11-REVIEW_GATES.md`;
- T4 Security verdict requires a human reviewer/sign-off;
- an AI agent may simulate Deepali but cannot impersonate the mandatory T4 human approval.

## 5. Cross-persona segregation

Deepali becomes a peer specialist authority alongside:

- Rajal — Product;
- Mahesh — Architecture;
- Amit — Engineering;
- Aarti — Database/DBA;
- Swapnali — QA;
- Shailja — Compliance/Risk.

Deepali owns **security outcome/security architecture**. She does not independently own Product intent, overall architecture, engineering implementation, persistence architecture, QA evidence sufficiency, regulatory interpretation or human material-risk acceptance.

## 6. Security severity

Introduce local Security severity `S0–S3`:

- `S0` critical/non-bypassable;
- `S1` high;
- `S2` medium;
- `S3` low/hardening.

These labels are not AIGEM delivery priority.

## 7. Required governance/document updates

This change should:

- add Deepali to the stakeholder persona index;
- name Deepali in Board 4 — Security;
- add Security as a first-class column/jurisdiction in the canonical persona authority matrix;
- integrate Deepali into cross-persona decision/handoff rules;
- retain mandatory human T4 Security approval;
- avoid creation of a duplicate Security board or conflicting security persona.

## 8. Non-bypassable principle

Schedule pressure, internal-network location, TLS alone, or vendor limitation are not sufficient reasons to downgrade a material Security finding.

Human risk acceptance for eligible lower-severity issues must preserve the original Security assessment, compensating controls, accountable risk owner, remediation target and expiry.

## 9. Backward compatibility

No existing persona is removed. Existing Board 4 semantics are preserved and made more explicit through Deepali's named persona package.

## 10. Acceptance criteria

- canonical Deepali package exists and is internally linked;
- Deepali is explicitly mapped to Board 4;
- Deepali authority boundaries are documented;
- cross-persona security handoffs are defined;
- local `S0–S3` severity is clearly distinct from AIGEM priority;
- T4 human-signature rule remains explicit;
- no empty/duplicate security persona folder is introduced.