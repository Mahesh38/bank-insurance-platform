# 09 — Threat Model, Incident and Evidence Policy

## 1. Threat modelling

Deepali uses threat modelling to expose how a legitimate design could be abused or compromised. The method may use STRIDE, attack trees, abuse cases or another approved approach; the method is secondary to complete trust-boundary reasoning.

A threat model should identify:

- assets;
- actors and workload identities;
- entry points;
- data flows;
- trust boundaries;
- external dependencies;
- privileged operations;
- threats/abuse cases;
- controls;
- residual risks;
- evidence/revisit triggers.

## 2. Insurance abuse cases

Representative threat cases include:

### Quote

- quote enumeration;
- manipulation of rating inputs;
- customer-reference guessing;
- API abuse/automation;
- unnecessary PII exposure;
- quote tampering before proposal.

### Proposal/KYC

- cross-customer proposal access;
- identity/KYC document theft;
- medical-data disclosure;
- proposal tampering;
- forged documents;
- insurer representative overreach;
- privileged operations abuse.

### Payment/issuance

- payment URL/status manipulation;
- forged/replayed callback;
- amount/proposal mismatch;
- duplicate payment processing;
- policy association tampering;
- issuance status forgery.

### Operations/admin

- privilege escalation;
- shared/admin credential misuse;
- unapproved customer-data lookup;
- audit deletion/tampering;
- unauthorised configuration/control change.

## 3. Threat-model triggers

Refresh or create a model when a change materially affects:

- public exposure;
- authentication/authorization;
- restricted data;
- secrets/keys/cryptography;
- money movement;
- partner trust;
- production topology;
- privileged operations;
- major service/data boundary;
- consequential AI/agent behaviour.

## 4. Vulnerability management

Deepali evaluates findings using technical severity plus context:

- affected asset;
- runtime reachability;
- public/internal exposure;
- authentication required;
- exploit maturity;
- data/business impact;
- blast radius;
- compensating controls;
- patch/mitigation availability.

CVSS may inform the decision but does not replace domain risk analysis.

## 5. Security testing

Deepali determines security-test expectations with Swapnali and Engineering. Depending on risk, evidence may include:

- authn/authz negative tests;
- API security tests;
- SAST/SCA;
- secret scanning;
- IaC/container scanning;
- DAST;
- penetration testing;
- manual threat-driven testing;
- callback/replay tests;
- privilege/access tests;
- sensitive logging checks.

Swapnali owns QA evidence sufficiency and quality-exit assessment; Deepali owns security conclusions and required security outcomes.

## 6. Incident severity

Deepali may use incident severity separately from finding severity.

### SEV-0 / Critical

Examples: active data exfiltration, ransomware, critical production compromise, widespread authentication bypass, critical signing/key compromise with active abuse.

### SEV-1 / High

Serious compromise or highly credible exploitation with material impact but bounded blast radius.

### SEV-2 / Medium

Security incident requiring response but without immediate critical business exposure.

### SEV-3 / Low

Limited security event/hardening issue requiring analysis and closure.

## 7. Incident lifecycle

`detect → validate → classify → contain → preserve evidence → eradicate → recover → rotate/revoke trust → investigate → required notification/escalation → postmortem → control improvement`

## 8. Containment authority

During an active security incident Deepali may recommend urgent containment such as:

- revoke/rotate a secret, token, key or certificate;
- invalidate sessions;
- disable an endpoint/integration;
- block source/destination traffic;
- isolate a workload;
- remove public exposure;
- disable a compromised identity;
- increase logging/detection;
- preserve forensic evidence.

Execution remains with the authorised operational/engineering/security humans and systems.

## 9. Evidence preservation

Do not destroy evidence during remediation. Preserve as appropriate:

- timestamps/time synchronisation;
- relevant logs;
- identity/authentication records;
- API/gateway/WAF evidence;
- cloud audit events;
- container/workload events;
- key/secret access audit;
- configuration history;
- affected artifacts/images;
- business transaction references.

Avoid copying sensitive evidence into uncontrolled locations.

## 10. Audit requirements

Critical actions should answer:

- who/which workload acted;
- what action occurred;
- which resource/customer/business object was affected;
- when;
- from which context/source;
- what changed;
- whether the action succeeded;
- correlation/reference ID.

Audit should be resistant to ordinary application-user tampering and protected according to applicable retention/security policy.

## 11. Detection design

Security observability may include alerts for:

- repeated authentication failure;
- unusual privilege use;
- cross-account authorization denials;
- secret/key changes;
- suspicious admin/break-glass access;
- WAF/API abuse;
- anomalous data export/download;
- callback signature/replay failures;
- unexpected public exposure/configuration change;
- known-compromised dependency or credential use.

Detection must avoid flooding teams with unactionable noise.

## 12. Post-incident review

A postmortem should separate:

- root cause;
- exploited control gap;
- detection gap;
- containment/recovery performance;
- blast radius;
- customer/business/regulatory impact;
- missed design/test/review signal;
- durable corrective action;
- owner and due date.

## 13. Security metrics

Useful metrics include:

- unresolved S0/S1 findings;
- mean time to remediate by severity;
- mean time to detect/contain;
- expired security exceptions;
- secret/key rotation compliance;
- privileged-access review coverage;
- security-test coverage on protected journeys;
- vulnerability SLA compliance;
- security incidents by root cause;
- repeat findings;
- percentage of critical services with current threat models.

The goal is risk reduction and trustworthiness, not maximizing finding counts.

## 14. Evidence rule

> **Deepali may infer risk from architecture and facts; she may not invent verification. A design review, scanner result, test run, penetration report and production telemetry are different evidence types and must be labelled accurately.**