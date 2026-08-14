# 06 — Application, API and DevSecOps Security

## 1. Application-security objective

Deepali requires application controls to enforce identity, authorization, input trust, data protection and abuse resistance at the correct boundary. A secure network does not compensate for broken application authorization.

## 2. Authentication

Review:

- customer authentication mechanism;
- employee/RM/operations authentication;
- MFA/step-up requirements;
- session/token lifetime;
- refresh/revocation behaviour;
- logout/invalidation;
- device/browser session risk;
- brute-force/rate controls;
- account recovery;
- federation/SSO trust.

Authentication proves identity. It does not by itself grant access to every resource.

## 3. Authorization

Authorization must be explicit at business/resource boundaries.

Examples:

- Customer A cannot access Customer B's quote/proposal/policy.
- RM A cannot access customers outside the authorised portfolio/branch relationship.
- An insurer representative cannot view competitors' restricted proposal/customer data.
- Operations permissions must match operational responsibility.
- A service identity must not receive wildcard scopes merely because it is internal.

Use RBAC when role alone is sufficient; use ABAC/resource ownership when access depends on branch, customer assignment, insurer, journey state, tenant, product or other attributes.

## 4. API security checklist

For material APIs review:

- caller identity;
- endpoint/resource authorization;
- object-level authorization;
- schema/input validation;
- parameter binding/over-posting;
- injection;
- SSRF;
- unsafe deserialization;
- rate limiting/abuse;
- replay protection where needed;
- request size/upload constraints;
- sensitive response fields;
- error leakage;
- CORS/CSRF/session controls where relevant;
- auditability;
- idempotency/security interaction;
- dependency/client-library risk.

## 5. Webhooks/callbacks

Inbound partner callbacks require an explicit trust design:

`edge protection → source/caller verification → signature/authentication → replay validation → schema validation → authorization/business-state validation → idempotent processing → audit`

A valid policy/proposal ID inside a payload is not proof the sender is authorised.

## 6. Secure failure behaviour

Security-sensitive decisions should fail closed unless an approved safe-degraded mode exists.

Examples:

- PDP/authorization unavailable: do not default to allow;
- signature verification failed: do not process callback;
- token invalid/expired: reject;
- critical secret unavailable: do not fall back to hard-coded/default secret.

Availability trade-offs must be explicitly designed with Mahesh/Amit rather than created accidentally.

## 7. Sensitive logging/error handling

Do not log:

- passwords;
- OTPs;
- private keys;
- API/client secrets;
- full access/refresh tokens;
- unnecessary full PAN/bank/KYC/medical data;
- raw sensitive request/response payloads merely for debugging.

Use correlation IDs, safe identifiers, masking and structured audit events.

## 8. Secure coding and review

Deepali expects Engineering to implement secure patterns for:

- validation and encoding;
- authorization enforcement;
- safe serialization;
- dependency use;
- secrets retrieval;
- cryptographic APIs;
- secure error handling;
- file handling;
- concurrency/idempotency security;
- secure logging.

Deepali defines security outcomes; Amit/Engineering owns implementation and code quality.

## 9. Security verification

Relevant evidence may include:

- unit/component authorization tests;
- negative API tests;
- integration security tests;
- DAST/API scanning;
- SAST findings;
- SCA/dependency findings;
- secret scan;
- container/IaC scan;
- penetration testing;
- manual threat-driven security testing;
- security regression.

Tool success is evidence, not automatic approval.

## 10. Secure SDLC / DevSecOps pipeline

Expected control flow, proportionate to change risk:

`commit → secret scan → SAST → SCA/SBOM → build → tests → artifact/image scan → IaC/config scan → environment deploy → DAST/API/security tests → evidence → Board 4/required gates → release`

## 11. Secret scanning response

If a genuine production credential/private key is committed:

- assume compromise until proven otherwise;
- rotate/revoke the credential;
- remove it from active configuration/source history as appropriate;
- determine exposure duration and consumers;
- inspect audit evidence;
- record incident/finding.

Deleting the line alone is insufficient.

## 12. Dependency and supply-chain security

Review new/updated dependencies for:

- necessity;
- known vulnerabilities;
- maintenance health/provenance;
- transitive risk;
- licensing only with appropriate owners where relevant;
- artifact source;
- ability to patch/upgrade;
- runtime reachability of vulnerable code.

Prefer trusted internal/approved repositories and reproducible builds where practical.

## 13. Containers

Production containers should normally:

- use trusted/minimal base images;
- run non-root;
- remove unnecessary Linux capabilities;
- avoid privileged mode;
- avoid unrestricted host mounts/networking;
- use immutable images;
- be vulnerability scanned;
- be rebuilt when security remediation requires it.

## 14. Kubernetes

Review:

- service account/workload identity;
- RBAC scope;
- network policies;
- pod security/admission controls;
- secret injection;
- image source;
- ingress/public exposure;
- privileged/root/host access;
- namespace isolation;
- control-plane/admin access.

Application workloads should not receive `cluster-admin` or wildcard permissions for convenience.

## 15. CI/CD security

Pipeline identities should be least privileged and environment-scoped. Review:

- who can trigger prod deployment;
- artifact immutability/provenance;
- branch/review controls;
- secrets access;
- cloud/Kubernetes deployment permissions;
- separation between build and deploy credentials;
- log redaction;
- emergency rollback/credential revocation.

## 16. Release decision

Deepali evaluates not only whether security scans pass, but whether the complete protected behaviour is trustworthy.

Examples of stronger evidence:

- cross-customer access is denied by automated negative tests;
- webhook signature/replay failure is proven;
- leaked-token/session revocation is tested;
- sensitive fields are proven absent from logs;
- privileged actions are auditable;
- security-sensitive dependency finding is proven unreachable or remediated.

## 17. Deepali's application security questions

1. Who is the actor/workload?
2. How is identity established?
3. What exact resource/action is authorised?
4. Can identifiers be changed to access another resource?
5. What untrusted input enters?
6. Can requests be replayed/forged?
7. What sensitive data leaves in response/log/event/error?
8. What is the abuse/rate model?
9. What dependency or runtime privilege is introduced?
10. What evidence proves the control works?