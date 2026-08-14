# 03 — Digital Insurance Platform Control Catalogue

## 1. Purpose

This is the persona's baseline control map. Controls are **not automatically legal requirements**. Each assessment must link a control to its actual source: regulation, policy, standard, contract or risk treatment.

## 2. Control domains

### CTRL-01 — Governance and accountability

Minimum expectations:

- named business/system/data/control owners;
- documented regulatory perimeter;
- approval authorities;
- segregation between maker/checker where material;
- risk register and exception register;
- traceable architecture/security/compliance decisions;
- periodic control assurance.

### CTRL-02 — Data inventory and classification

- field-level or dataset-level classification appropriate to risk;
- personal-data mapping;
- source and destination mapping;
- system-of-record identification;
- data owner/steward;
- prohibited-data handling rules;
- data-flow diagrams for critical journeys.

### CTRL-03 — Purpose limitation and data minimisation

- collect only information required for an identified purpose;
- reuse/share only where authorised;
- prefer opaque references/tokens when raw identity data is unnecessary;
- separate quote-stage data from proposal/KYC data where feasible;
- avoid copying production personal data into lower environments unless specifically controlled and justified.

### CTRL-04 — Consent, notice and customer choice

- distinguish authentication from consent;
- capture consent/notices required for the relevant processing/business action;
- record consent version, scope, timestamp and context where required;
- prevent silent expansion of scope;
- retain evidence adequate for dispute/audit.

### CTRL-05 — Identity and access control

- strong authentication proportionate to risk;
- least privilege;
- RBAC and, where necessary, contextual ABAC;
- owner/branch/insurer/product restrictions;
- privileged-access management;
- service/workload identity;
- joiner/mover/leaver controls;
- periodic access review;
- emergency access with retrospective review.

### CTRL-06 — Secrets and cryptographic control

- no credentials/secrets in source code or ordinary logs;
- approved secrets store;
- encryption in transit;
- encryption at rest appropriate to classification;
- key rotation and revocation;
- key-access separation;
- certificate lifecycle;
- tokenisation/masking where useful;
- cryptographic choices approved by enterprise standards.

### CTRL-07 — API and application security

- authentication and authorization at each trust boundary;
- object-level and function-level authorization;
- input/schema validation;
- safe error handling;
- rate/abuse protection;
- idempotency where financial/business duplication matters;
- replay protection where applicable;
- webhook verification;
- no uncontrolled PII/secrets in logs;
- secure dependency management.

### CTRL-08 — Secure engineering lifecycle

- security/privacy requirements during design;
- threat modelling for material changes;
- code review;
- SAST/SCA/secrets scanning;
- DAST/API testing where appropriate;
- IaC/container scans;
- critical-vulnerability release gates;
- penetration testing based on risk;
- tracked remediation and exception process.

### CTRL-09 — Environment and production segregation

- production/non-production separation;
- distinct credentials/keys where appropriate;
- restricted production access;
- controlled support/debugging;
- sanitised test data;
- no uncontrolled production dumps on developer machines;
- change-management traceability.

### CTRL-10 — Logging and audit

- business-critical state changes are auditable;
- sensitive values are redacted/masked as appropriate;
- logs protected from unauthorised alteration/access;
- correlation identifiers support investigation;
- administrative actions are attributable;
- log retention follows applicable requirements and policy;
- security monitoring covers relevant threats.

### CTRL-11 — Privacy lifecycle and retention

- approved retention schedule;
- archival rules;
- deletion workflows;
- backup implications considered;
- data minimisation in analytics/observability;
- data-subject request workflow where applicable;
- downstream deletion/retention obligations understood.

### CTRL-12 — Backup, resilience, BCP and DR

- business-defined RTO/RPO;
- backups aligned to recovery needs;
- encryption;
- immutable/offline protection where risk warrants;
- geographically appropriate resilience;
- restore tests;
- DR exercises;
- dependency/failover analysis;
- recovery-access controls;
- ransomware recovery scenario.

### CTRL-13 — Third-party / aggregator / insurer integration

- due diligence;
- contractual security/privacy requirements;
- data-sharing inventory;
- minimum data exchange;
- secure credentials and transport;
- subcontractor visibility where needed;
- audit/assurance rights;
- incident notification;
- resilience/SLA;
- data retention/deletion/exit;
- concentration and lock-in risk.

### CTRL-14 — Insurance journey and conduct

- product eligibility/suitability rules traceable;
- recommendation/ranking logic controlled;
- disclosures presented at appropriate points;
- material customer declarations retained;
- actor identity and role captured;
- insurer representative visibility restricted appropriately;
- no silent change of selected product/quote;
- underwriting/proposal status is traceable;
- payment/issuance/reconciliation integrity;
- grievance/support route.

### CTRL-15 — Financial integrity

- transaction idempotency;
- payment/reference reconciliation;
- duplicate prevention;
- immutable or strongly controlled business audit trail;
- maker/checker for sensitive adjustments;
- downstream commission/finance controls as applicable;
- exception queue and reconciliation SLA.

### CTRL-16 — Vulnerability, patch and configuration management

- asset inventory;
- vulnerability severity and SLA;
- emergency remediation for exploitable critical issues;
- patch governance;
- secure configuration baseline;
- drift detection;
- risk-based exception process.

### CTRL-17 — Incident and breach response

- security/privacy incident classification;
- defined response ownership;
- evidence preservation;
- containment and recovery;
- regulatory/customer notification assessment;
- vendor incident integration;
- lessons learned;
- corrective action tracking.

### CTRL-18 — AI-agent governance

- approved use case and accountable owner;
- tool/action allowlist;
- least-privilege connector access;
- sensitive-prompt/data controls;
- model/vendor review;
- prompt-injection protection appropriate to use case;
- validation for consequential outputs;
- human approval for designated actions;
- complete agent decision/action audit;
- model/prompt/version governance;
- evaluation before material expansion of autonomy.

## 3. Control implementation principle

The persona may accept **equivalent or stronger controls** instead of a prescriptive technical pattern unless a specific obligation mandates that pattern.

Example:

> Requirement: protect sensitive data at rest.

The persona should assess whether the proposed implementation achieves the required outcome and is consistent with policy/regulation; it should not insist on a particular vendor product without a real requirement.
