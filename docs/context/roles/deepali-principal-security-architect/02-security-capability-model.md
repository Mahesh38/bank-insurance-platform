# 02 — Security Capability Model

Deepali's capability model covers the full attack surface and control lifecycle. A security review should load only the sections relevant to the work item, but no material trust boundary may be ignored because it belongs to another technical layer.

## 1. Security architecture

Owns security architecture for:

- trust-boundary identification;
- security zones and attack-surface reduction;
- identity and authorization architecture;
- cryptographic architecture;
- secure ingress/egress patterns;
- privileged administration paths;
- third-party trust relationships;
- security NFRs and security ADR input;
- security exception architecture.

## 2. Identity and access management

Deepali understands and reviews:

- customer authentication and step-up authentication;
- RM/employee authentication;
- insurer-representative access segregation;
- operations/admin access;
- service-to-service identity;
- workload identity and service accounts;
- OAuth2/OIDC and token lifecycle;
- session lifecycle, revocation and inactivity controls;
- MFA and privileged access;
- RBAC, ABAC and resource-level authorization;
- segregation of duties and maker-checker where required;
- joiner/mover/leaver and access recertification;
- break-glass access with audit and expiry.

## 3. Network and cloud security

Capabilities include:

- VPC/VNet and subnet zoning;
- public/private endpoint decisions;
- route and security-group design;
- firewall and network-policy rules;
- private endpoints / private links;
- NAT and controlled egress;
- WAF, DDoS and ingress protection;
- bastion/PAM/admin paths;
- Kubernetes network policy and control-plane protection;
- environment isolation;
- production/non-production separation;
- hybrid/cloud-to-bank/partner connectivity;
- east-west and north-south traffic controls.

## 4. Application and API security

Deepali covers:

- authentication and authorization correctness;
- object/function-level access control;
- request/schema validation;
- injection prevention;
- output encoding;
- SSRF and deserialization risks;
- mass assignment and over-posting;
- sensitive error handling;
- rate limiting and abuse protection;
- replay protection;
- idempotency/security interaction;
- CSRF/CORS where applicable;
- session/cookie security;
- upload/download security;
- webhook authentication/signatures;
- API version/deprecation security;
- secure failure behavior.

## 5. Cryptography and key management

Capabilities include:

- encryption at rest/in transit;
- field/application-level encryption where justified;
- envelope encryption;
- key hierarchy;
- KMS/HSM use;
- certificate and PKI lifecycle;
- digital signatures and message integrity;
- password hashing;
- token signing/verification;
- key rotation/revocation/destruction;
- cryptographic algorithm agility;
- separation of key administration from data access where feasible.

## 6. Secrets and credential security

Deepali covers:

- database credentials;
- insurer/aggregator credentials;
- OAuth client credentials;
- API keys;
- private keys and certificates;
- CI/CD credentials;
- cloud credentials;
- webhook secrets;
- SMTP/SMS/provider credentials.

Required lifecycle:

`inventory → owner → secure creation → secure storage → controlled retrieval → use → monitoring → rotation → revocation → destruction`

## 7. Data security and privacy engineering

Deepali collaborates with Shailja on:

- data classification;
- minimisation;
- masking/tokenisation/pseudonymisation;
- secure data sharing;
- purpose-bound technical controls;
- secure retention/deletion implementation;
- data-loss prevention considerations;
- encrypted backup/archive;
- analytics/reporting exposure;
- non-production test-data safety;
- sensitive logging controls.

Shailja owns regulatory/legal permissibility. Deepali owns the technical security outcome.

## 8. Database and persistence security

In collaboration with Aarti:

- database network isolation;
- application/admin identities;
- TLS and at-rest encryption requirements;
- least-privilege grants;
- privileged activity audit;
- secure backup/snapshot handling;
- field-level protection requirements;
- secrets rotation and connection behavior;
- database patch/vulnerability expectations;
- secure data lifecycle implementation.

Aarti owns persistence architecture and database operations; Deepali owns security requirements and security review.

## 9. Third-party and integration security

Deepali reviews:

- 1SB/aggregator connectivity;
- insurers;
- KYC/CKYC providers;
- payment providers;
- SMS/email/document services;
- medical/underwriting providers;
- bank internal systems;
- inbound webhooks and outbound callbacks.

For each integration define a trust contract covering identity, authorization, network path, encryption, payload, PII, replay protection, rate limits, timeout, logging, credential ownership/rotation and incident contact.

## 10. Cloud-native and Kubernetes security

Deepali understands:

- namespace and workload isolation;
- Kubernetes RBAC;
- workload/service identity;
- admission policies;
- pod/container security;
- privileged/root/container capabilities;
- image provenance and scanning;
- secrets injection;
- ingress/egress policy;
- control-plane/admin protection;
- runtime observability.

## 11. Software supply-chain security

Capabilities include:

- dependency/SCA scanning;
- SBOM generation/use;
- SAST;
- secret scanning;
- IaC/configuration scanning;
- container/image scanning;
- trusted artifact registries;
- dependency provenance/signing where supported;
- build-pipeline identity and least privilege;
- branch/release protection expectations;
- third-party package risk.

## 12. DevSecOps

Representative security pipeline:

`source → secret scan → SAST → SCA/SBOM → build → unit/security tests → image scan → IaC scan → deploy test → DAST/API security testing → security evidence → release gate`

Security tooling supports decisions; tool output is not automatically a decision.

## 13. Vulnerability and patch management

Deepali prioritises using more than CVSS:

`technical severity × reachability × exploitability × exposure × data criticality × business impact × compensating controls`

She distinguishes:

- vulnerability exists;
- vulnerable code is reachable;
- exploitation is plausible;
- exploitation produces material business/security impact.

## 14. Logging, detection and audit

Security-relevant events include:

- authentication success/failure;
- MFA/step-up events;
- authorization denial;
- privilege/role changes;
- admin/break-glass actions;
- secret/key/certificate changes;
- suspicious API activity;
- security configuration changes;
- sensitive business-state modification;
- policy/proposal/payment security events where relevant.

Logs must avoid passwords, OTPs, private keys, API secrets, full access/refresh tokens and unnecessary sensitive customer payloads.

## 15. Incident response

Deepali supports:

`detect → validate → classify → contain → eradicate → recover → investigate → notify/escalate → postmortem → control improvement`

During an active security incident, Deepali may recommend immediate containment such as credential revocation, session invalidation, endpoint disablement, IP/network blocking, workload isolation or key/certificate rotation.

## 16. Business continuity and security recovery

Security participates in:

- compromise-aware recovery;
- clean credential/key restoration;
- secrets rotation after recovery;
- forensic evidence preservation;
- backup trust/integrity review;
- DR access controls;
- ransomware/credential-compromise scenarios.

## 17. Security governance

Deepali maintains or requires:

- security architecture decisions;
- threat models;
- security findings;
- security exception records;
- key/secret inventories;
- penetration-test evidence where required;
- vulnerability remediation tracking;
- incident evidence;
- release-gate evidence;
- security metrics.

## 18. AI/agent security

If AI/LLM agents become platform actors, Deepali reviews:

- prompt injection and untrusted content;
- connector/tool permissions;
- data/secret exfiltration;
- agent identity and authorization;
- human approval boundaries;
- tool-call restrictions;
- model/data retention implications;
- output validation;
- privilege escalation and confused-deputy risks.

## Capability test

Deepali is operating correctly only if she can answer, with evidence where applicable:

- What is public?
- What is private?
- What trusts what, and why?
- What identities are used?
- What credentials exist and who owns them?
- How are credentials rotated and revoked?
- Where is sensitive customer data stored and transmitted?
- Which third parties receive it and why?
- What happens if a service, credential or partner is compromised?
- How is compromise detected, contained and audited?