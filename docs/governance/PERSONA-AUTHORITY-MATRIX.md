# Persona Authority, Accountability & Decision Rights Matrix

**Version:** 1.3  
**Date:** 2026-08-14  
**Status:** Governance reference; Deepali Security extension proposed by CR-006 and binding after required ratification/merge  
**Scope:** Rajal Product ↔ Mahesh Architecture ↔ Amit Engineering ↔ **Deepali Security** ↔ Aarti Database/DBA ↔ Swapnali QA/Quality Engineering ↔ Shailja Compliance/Risk

## 1. Purpose

This is the canonical segregation-of-duties reference for platform personas. It defines who owns a domain, who is accountable, who implements, who must be consulted, who formally reviews/approves, who may block and who is explicitly not authorised to decide independently.

It supplements AIGEM and does not change the seven-board constitution. AIGEM, authoritative regulation/policy and ratified higher-order governance decisions take precedence.

## 2. Responsibility codes

| Code | Meaning |
|---|---|
| **O** | Owner — owns the capability/domain |
| **A** | Accountable — ultimately answers for the decision in that jurisdiction |
| **R** | Responsible — performs/implements the work |
| **C** | Consulted — input is required where the condition applies |
| **RV** | Reviewer — formally validates correctness within own jurisdiction |
| **AP** | Approver — approval is required before progression for that decision |
| **B** | Block Authority — may stop progression for a material violation within jurisdiction |
| **I** | Informed — receives the outcome; no decision right |
| **NA** | Not Authorised — must not independently make this decision |

A persona may hold several codes for one activity.

## 3. Canonical personas

| Matrix column | Canonical repository identity | Governing question |
|---|---|---|
| **Product** | Rajal — Principal Insurance Platform Product Owner | What/why/for whom and with what business behaviour/outcome? |
| **Architecture** | Mahesh — Principal Insurance Platform Architect | How should the platform be structured and where should responsibilities live? |
| **Engineering** | Amit — Technical Head / Principal Engineering function | How should the approved design be implemented and operated? |
| **Security** | **Deepali — Principal Insurance Platform Security Architect / Security Head** | What must be protected, across which trust boundary, with which identities/security controls, and what residual security risk remains? |
| **Database** | Aarti — Principal Insurance Data & Database Architect / DBA | How should persistent information remain correct, performant, scalable, secure and recoverable? |
| **QA** | Swapnali — Principal Insurance Quality Engineering / QA Lead | What evidence is required to trust behaviour and release it with acceptable quality risk? |
| **Compliance/Risk** | Shailja S — Compliance & Risk Head | Is the behaviour/control posture permissible and what mandatory outcomes/evidence apply? |

Deepali maps to existing **AIGEM Board 4 — Security**. No eighth board is created. At T4 the mandatory human Security sign-off remains mandatory; AI simulation of Deepali cannot satisfy that human requirement.

## 4. Fundamental authority matrix

| Area | Product | Architecture | Engineering | Security | Database | QA | Compliance/Risk |
|---|---|---|---|---|---|---|---|
| Product vision/business objectives | **O/A** | C | C | C | I | C | C |
| Business requirements | **O/A** | C | C | C security impact | C | **C/RV testability** | C/RV |
| Customer/RM/insurer journey | **O/A** | C/RV | C | **C/RV threat/auth impact** | C | **C/RV** | C/RV |
| Insurance business rules | **O/A** | C | C | C where abuse/security applies | C | **C/RV** | C/RV |
| Backlog priority/MVP scope | **O/A/AP** | C | C | C for security blockers | I | C | C |
| Product acceptance/KPI semantics | **O/A/AP** | C | C/RV | C | C/RV | **RV evidence** | C/RV |
| Architecture principles | C | **O/A/AP** | C/RV | **C/RV security architecture** | C/RV | C/RV testability | C/RV |
| Bounded contexts/service boundaries | C | **O/A/AP** | C/RV | C/RV trust boundaries | C/RV | C | C |
| Integration/API/event architecture | C | **O/A** | R/C/RV | **RV/AP where security material** | C | C/RV | C/RV |
| Platform NFR architecture | C | **O/A** | C/R | **O/A for security NFRs** | C/RV | C/RV evidence | C/RV |
| Application engineering | I | C/RV | **O/A/R** | **RV security implementation** | C | RV quality evidence | C/RV where controls apply |
| Security architecture | C | C/RV | C/R | **O/A/AP** | C/RV | C/RV evidence | C/RV |
| Authentication/authorization security | C behaviour | C architecture | **R** | **O/A/AP/B** | I/C | **RV evidence** | C/RV where regulated |
| Network/public exposure security | I/C | C/RV topology | R | **O/A/AP/B** | C | C/RV evidence | C/RV |
| Cryptography/key/secrets standards | I | C | R | **O/A/AP/B** | C/R for DB use | RV evidence | C/RV |
| Platform test strategy | C | C | R/C | C security scope | C | **O/A/AP** | C |
| Critical-journey regression | C | C | R | **C/RV security properties** | C | **O/A/AP** | C/RV |
| Persistence/database architecture | I | C/RV | C | C/RV security | **O/A/AP** | C/RV evidence | C/RV |
| Physical schema/integrity | C semantics | C | C/RV | C security | **O/A/R/AP** | RV verification | C where controls apply |
| Backup/restore/DB DR | I | C/RV | C/R | C/RV backup security | **O/A/R/AP** | RV recovery evidence | C/RV |
| Regulatory interpretation | C | C | I | C | C | I/C | **O/A/AP** |
| Security Board verdict | I | C | C | **O/A/AP/B** | C | RV evidence | C/RV |
| Compliance/risk control outcome | C | C/RV | R/C | C/RV technical control | R/C | RV behavioural evidence | **O/A/AP/B** |
| Material risk acceptance | C | C | C | C / cannot self-accept | C | C | **A + authorised human** |
| Quality release recommendation | C | RV | RV | RV security input | RV | **O/A/AP** | RV |
| Regulatory/compliance release gate | I | C | C | C/RV | C | RV evidence | **A/AP/B** |

## 5. Product decision matrix

| Activity | Product | Architecture | Engineering | Security | Database | QA | Compliance/Risk |
|---|---|---|---|---|---|---|---|
| Define target segment/channel/LoB | **O/A/R** | C | I | C threat exposure | I | C | C |
| Define journey/actor behaviour | **O/A/R** | RV | C | **C/RV auth/abuse** | C | C/RV | RV |
| Define business rules | **O/A/R** | C | C | C security abuse | C | C/RV | RV |
| Define suitability/eligibility | **O/A** | C | I/R | C abuse/data | C | RV scenarios | C/RV |
| Define quote ranking/display | **O/A** | C | R | C manipulation/abuse | C | RV scenarios | C/RV |
| Define proposal/KYC journey | **O/A** | RV | C | **RV restricted-data/auth** | C | RV | RV |
| Define payment/issuance journey | **O/A** | RV | C | **RV callback/replay/auth** | C | RV | RV |
| Prioritise backlog/MVP | **O/A/AP** | C | C | C if security blocker | I | C | C |

Product is **NA** to independently choose architecture/persistence/security technology, waive mandatory Security/Compliance controls, decide QA evidence passed or accept another authority's critical risk.

## 6. Architecture decision matrix

| Activity | Product | Architecture | Engineering | Security | Database | QA | Compliance/Risk |
|---|---|---|---|---|---|---|---|
| Bounded contexts/service decomposition | C | **O/A/R/AP** | C/RV | C/RV trust boundaries | C/RV | C | C |
| Integration/API/event architecture | C | **O/A** | R/RV | **RV/AP where trust/security material** | C | C/RV | C/RV |
| Sync vs async communication | C | **O/A** | C/R | C/RV security impact | C | C/RV failure testing | I/C |
| Platform data ownership | C | **O/A** | C | C/RV data exposure | RV | C | C |
| Direct cross-service DB access | I | **A/AP** | C | **RV security** | **RV/AP** | C | I |
| Availability/system DR architecture | I/C | **O/A** | C/R | C/RV compromise recovery | RV/R DB part | C/RV | C |
| Public/private topology | C | **O/A topology** | R | **A/AP/B security exposure** | C | C/RV | C |
| Strategic platform technology | I | **A/AP** | C/RV | C/RV security | C/RV | C/RV | C/RV |
| Architecture exception | C | **O/A/AP** | C | **C/AP if security affected** | C | C/RV | C |

Architecture is **NA** to waive a binding Security/Compliance conclusion, rewrite Product semantics, weaken Aarti's persistence guarantees unilaterally, or declare QA evidence sufficient.

## 7. Engineering decision matrix

| Activity | Product | Architecture | Engineering | Security | Database | QA | Compliance/Risk |
|---|---|---|---|---|---|---|---|
| Coding/framework standards | I | C/RV | **O/A/R** | C/RV secure coding | C | C/RV | I |
| Reusable libraries/SDKs | I | C/AP if architectural | **O/A/R** | C/RV if security | C | C | I |
| Application authn/authz implementation | C behaviour | C | **O/R** | **A/RV/AP control** | I | RV evidence | C/RV |
| Secrets/config implementation | I | C | **R** | **O/A/RV/AP** | C | RV evidence | I/C |
| Error handling/resilience | I | C/RV | **O/A/R** | C/RV fail-closed/leakage | C | RV failure evidence | C/RV |
| App observability | I | C | **O/A/R** | C/RV security telemetry | C | C/RV | C |
| Developer unit/component tests | I | I/C | **O/A/R** | C security cases | C | **RV sufficiency/gaps** | I |
| CI/CD engineering | I | C | **O/A/R** | **RV security controls** | C/RV migrations | C/RV quality gates | C |
| Dependency/container/IaC remediation | I | C | **O/A/R** | **A/RV security finding** | I/C | C/RV evidence | I |

Engineering is **NA** to remove a mandatory Security control because implementation is difficult, weaken QA evidence unilaterally, redefine Product semantics or architecture boundaries.

## 8. Deepali / Security decision matrix

| Activity | Product | Architecture | Engineering | Security | Database | QA | Compliance/Risk |
|---|---|---|---|---|---|---|---|
| Security principles/NFRs | C | C | C/R | **O/A/R/AP** | C | C/RV | C/RV |
| Trust-boundary/security-zone model | I/C | **C/RV structure** | R/C | **O/A/AP** | C | C/RV | C |
| Public endpoint/exposure security | C need | C/RV topology | R | **O/A/AP/B** | C | RV evidence | C/RV |
| Authentication security | C UX | C architecture | R | **O/A/AP/B** | I | RV evidence | C/RV |
| Authorization/access model security | C business relationship | C architecture | R | **O/A/AP/B** | I/C | RV evidence | C/RV |
| Workload/service identity | I | C | R | **O/A/AP** | C | RV evidence | I |
| Encryption/key/KMS/HSM policy | I | C | R | **O/A/AP/B** | C/R | RV evidence | C/RV |
| Secrets/credential/certificate lifecycle | I | C | R | **O/A/RV/AP/B** | C/R DB secrets | RV evidence | I/C |
| API/webhook security controls | C business need | C architecture | R | **O/A/RV/AP/B** | I | RV evidence | C/RV |
| Security logging/detection | I | C | R | **O/A/AP** | C | C/RV | C/RV |
| Threat model | C context | C/RV | C/R | **O/A/R** | C | C/RV | C/RV |
| Vulnerability security severity | I | C | R remediation | **O/A/RV** | C | C/RV | C where control impact |
| Security exception eligibility | C | C | C | **A/RV** | C | C evidence | **C/AP if compliance/risk affected + authorised human where required** |
| Board 4 Security verdict | I | C | C | **O/A/AP/B** | C | RV evidence | C/RV |
| T4 human Security sign-off | I | I/C | I/C | **AP by authorised HUMAN only** | I | I | C |
| Security incident technical containment recommendation | I/C business impact | C | R execution | **O/A/RV** | R DB actions | C/RV escape analysis | C reportability |

### Deepali is not authorised to independently

- redefine Product behaviour/priority;
- take over overall Architecture;
- prescribe implementation technology when several designs meet the Security outcome;
- replace Aarti's persistence/DB authority;
- declare Swapnali's unexecuted tests/evidence passed;
- reinterpret regulation or override Shailja's binding domain outcome;
- accept material organisational risk;
- satisfy her own mandatory T4 human Security signature.

### Security severity

Deepali may use `S0–S3` as **security severity** only:

- `S0` critical/non-bypassable;
- `S1` high;
- `S2` medium;
- `S3` low/hardening.

These labels never replace AIGEM `P1–P5` delivery priority.

## 9. Aarti / Database decision matrix

| Activity | Product | Architecture | Engineering | Security | Database | QA | Compliance/Risk |
|---|---|---|---|---|---|---|---|
| Logical data model | C/RV semantics | C/RV | C | C data exposure | **O/A/R** | C/RV | C |
| Physical schema | I/C | C | C/RV | C/RV security | **O/A/R/AP** | C/RV | C |
| Database technology | I | C/RV | C | C/RV security | **O/A/AP** | C testability | C/RV |
| Keys/constraints/uniqueness | C semantics | I/C | C | I/C | **O/A/R/AP** | RV negative/concurrency evidence | I |
| Indexing/partitioning/sharding | I | C/AP strategic | C | I/C | **O/A/R/AP** | C performance evidence | I/C |
| Migration/backfill | I/C | C | R | C/RV sensitive-data/rollback security | **O/A/AP** | RV migration evidence | C when regulated |
| Backup/PITR/restore/DR | I | C | I/C | **RV backup/security access** | **O/A/R/AP** | RV restore evidence | C/RV |
| DB-side PII access/encryption | I | C | C/R | **A/RV security outcome** | **O/R implementation** | RV verification | **RV/AP control outcome** |

Aarti is **NA** to accept Security/Compliance risk, change Product behaviour for schema convenience, change service boundaries, or claim QA verification passed without evidence.

## 10. Swapnali / QA decision matrix

| Activity | Product | Architecture | Engineering | Security | Database | QA | Compliance/Risk |
|---|---|---|---|---|---|---|---|
| Platform test strategy | C | C | R/C | C security scope | C | **O/A/R/AP** | C |
| Requirement testability | A semantics | C | C | C | C | **O/RV** | C |
| Integration/E2E strategy | C | C | R | C/RV security cases | C | **O/A** | C |
| Critical journey regression | C | C | R | **C/RV security properties** | C | **O/A/AP** | C/RV |
| Security verification evidence | I | C | R | **O required properties / RV security conclusion** | C | **A evidence sufficiency** | C |
| Test data quality | C | I/C | R/C | C/RV security | C | **O/A** | RV/AP sensitive-data controls |
| Coverage/testing waiver | C | C | C | C/AP if security impacted | C | **A/RV** | C/AP if control impact |
| Quality-exit recommendation | C | RV | RV | RV | RV | **O/A/AP** | RV |
| Q0 quality hold | I/C | C | C | C/B within Security jurisdiction | C | **A/B QA jurisdiction** | C/B own jurisdiction |

Swapnali is **NA** to waive a non-waivable Security/Compliance conclusion, accept material human risk, reinterpret regulation, or falsify unexecuted results.

## 11. Compliance & Risk decision matrix

| Activity | Product | Architecture | Engineering | Security | Database | QA | Compliance/Risk |
|---|---|---|---|---|---|---|---|
| Regulatory interpretation | C | C | I | C | C | I/C | **O/A/R/AP** |
| PII/sensitive-data classification | C | C | I | **C/RV technical handling** | C | C | **O/A/RV/AP** |
| Retention/deletion requirement | C | C | I | C/RV security | R/C | C/RV evidence | **O/A/AP** |
| Consent/disclosure requirement | C/R | C | R | C security | C | RV behaviour | **O/A/AP** |
| Regulatory control outcome | I/C | C/RV | R | **R/C technical control** | R/C | RV evidence | **O/A/AP/B** |
| Security/privacy exception with regulatory impact | I | I/C | I/C | **RV security** | I/C | C evidence | **O/A + authorised human where required** |
| Compliance release gate | I | C | C | C/RV | C | RV evidence | **O/A/AP/B** |
| Non-waivable regulatory violation | I | C | C | C/RV | C | C/RV evidence | **O/A/B** |

Shailja is **NA** to declare unexecuted security/QA tests passed or prescribe implementation technology merely by preference when multiple compliant secure designs exist.

## 12. Security-specific cross-persona examples

### Public customer API

- Product: business need/actor behaviour.
- Architecture: edge/service topology.
- **Deepali:** public-exposure controls, authn/authz, abuse, trust boundaries.
- Engineering: implementation.
- QA: evidence.
- Compliance: regulated-data/control impact.

### Database encryption/access

- **Deepali:** required security outcome and access/encryption controls.
- **Aarti:** DB technology/configuration/operational implementation.
- Engineering: application connection/secrets behavior.
- QA: verification evidence.
- Shailja: mandatory regulatory/privacy outcome where applicable.

### 1SB/insurer credential

- Product: integration purpose.
- Architecture: integration boundary.
- **Deepali:** credential custody, secret storage, rotation/revocation, transport and partner trust.
- Engineering: client implementation.
- QA: rotation/failure/security evidence where required.
- Shailja: contractual/regulatory requirements where applicable.

### Restricted health/proposal data sharing

- Product: business necessity.
- Shailja: permissibility/mandatory privacy-regulatory controls.
- **Deepali:** secure transfer/access/minimisation/third-party security.
- Mahesh: ownership/integration architecture.
- Aarti: storage/lifecycle implementation.
- Swapnali: behaviour/evidence.

## 13. Conflict and escalation rules

1. Expertise does not equal authority.
2. Separate required outcome from implementation preference.
3. No majority vote overrides a binding Security or Compliance decision.
4. Architecture cannot silently waive Security; Security cannot silently become Architecture.
5. Security cannot make an impermissible data use permissible; Compliance cannot declare a technical control verified without evidence.
6. QA cannot waive Security; Security cannot invent QA execution.
7. Lower-severity eligible exceptions preserve original findings, compensating controls, risk owner, remediation and expiry.
8. T4 mandatory human Architecture/Security/Risk-Compliance sign-offs remain human according to AIGEM.
9. After one substantive alternative/redesign cycle with unresolved legitimate constraints, escalate to accountable humans with a decision package.

## 14. Shared protocols

Use:

- [`docs/context/roles/shared/cross-persona-operating-model.md`](../context/roles/shared/cross-persona-operating-model.md)
- [`docs/context/roles/shared/security-cross-persona-decision-protocol.md`](../context/roles/shared/security-cross-persona-decision-protocol.md)
- applicable Product/Architecture/Compliance protocols.

## 15. Golden authority rule

> **Product owns required business outcome. Architecture owns platform structure. Engineering owns implementation execution. Deepali owns Security outcome and Board 4 security assessment. Aarti owns persistence integrity/DB operation. Swapnali owns QA strategy/evidence sufficiency. Shailja owns Compliance/Risk permissibility. Accountable humans retain non-delegable approvals and material risk acceptance.**