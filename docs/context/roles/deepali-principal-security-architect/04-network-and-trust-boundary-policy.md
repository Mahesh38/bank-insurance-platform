# 04 — Network and Trust Boundary Policy

## 1. Governing rule

> **Same VPC/VNet/subnet/cluster does not mean trusted.**

Network location is one control signal. Every material interaction still requires an explicit caller identity, authorization model, encrypted transport where required, restricted network path and auditability proportional to risk.

## 2. Security zones

A typical platform may separate:

- **Edge/Public zone** — CDN, DDoS, WAF, public load balancer/API gateway;
- **Application zone** — customer/RM-facing application services with no direct public IP unless explicitly required;
- **Integration zone** — controlled ingress/egress to bank systems, 1SB, insurers and providers;
- **Data zone** — databases, brokers, caches and object stores;
- **Management zone** — controlled admin/PAM/bastion and platform management;
- **Security zone** — KMS/HSM, secrets systems, security tooling/logging where architecture supports it.

The exact topology is owned with Mahesh; Deepali owns the required security boundaries and exposure constraints.

## 3. Internet-facing components

Potentially acceptable public entry points include only those required by the business architecture, normally behind approved edge controls:

`Internet → DDoS/CDN where applicable → WAF → public LB/API Gateway/Ingress → authenticated/authorised application path`

Examples:

- customer web/mobile API edge;
- approved partner API endpoint;
- webhook/callback receiver;
- approved authentication endpoint.

A public API edge does **not** imply the underlying microservice requires a public IP.

## 4. Components normally private

Default to no direct internet exposure for:

- production databases;
- Redis/cache;
- Kafka/message brokers;
- internal microservices;
- internal admin APIs;
- service registry/config plane;
- Kubernetes API/control plane;
- Vault/secrets stores;
- KMS/HSM management path;
- CI/CD agents/runners;
- monitoring/admin consoles;
- Elasticsearch/OpenSearch management endpoints;
- database administration interfaces.

Exceptions require a documented threat model, necessity, compensating controls and explicit Security approval within governance.

## 5. Ingress rules

For every inbound path define:

- source population/network;
- destination endpoint;
- caller identity;
- authentication method;
- authorization policy;
- TLS/mTLS expectations;
- rate/abuse controls;
- schema/input validation;
- replay/signature controls where applicable;
- logging/audit;
- failure behavior;
- owner and revocation procedure.

## 6. Egress rules

Outbound access should be controlled rather than unrestricted by default for sensitive workloads.

For partner/provider egress define:

- approved destination/FQDN/private endpoint;
- protocol/port;
- credential/certificate used;
- data classes transmitted;
- timeout/retry behavior;
- DNS/proxy/NAT path;
- observability;
- emergency block/revocation mechanism.

## 7. Partner connectivity

1SB, insurers and external providers are separate trust domains even when connected through private networking.

Acceptable patterns may include:

- private link/private endpoint;
- leased/private bank connectivity;
- VPN;
- controlled internet egress with TLS/mTLS and allowlisting where suitable.

Private transport reduces exposure but does not replace application identity/authorization.

## 8. Service-to-service security

Prefer workload/service identity over implicit subnet trust.

Expected controls may include:

- workload identity/service account;
- mTLS where warranted;
- scoped OAuth/token or service authorization;
- Kubernetes network policy/security groups;
- least-privilege destination access;
- request/audit correlation.

## 9. Administrative access

Production administration should use controlled paths such as:

`Corporate identity → MFA → PAM/JIT approval → bastion/private management path → target`

Avoid:

- shared admin accounts;
- permanent broad production access;
- internet-exposed DB/admin ports;
- unmanaged developer IP allowlists as a permanent control;
- secrets copied to local machines.

Break-glass access must be attributable, time-bounded and reviewed.

## 10. Environment isolation

Production security must not depend on development/test controls.

Require appropriate separation for:

- credentials/keys;
- data;
- cloud accounts/subscriptions/projects where policy dictates;
- network paths;
- admin permissions;
- CI/CD deployment identities.

Production PII must not flow to non-production merely for test convenience.

## 11. Kubernetes/container network posture

Review:

- ingress class and public exposure;
- namespace/workload isolation;
- NetworkPolicy/egress control;
- service accounts and workload identity;
- host networking/host paths;
- privileged containers;
- control-plane exposure;
- service mesh policy where used.

## 12. Decision checklist

Before approving a network path Deepali asks:

1. Why must these two endpoints communicate?
2. Is the destination public because it must be, or because it is easy?
3. Which identity authenticates the caller?
4. What exact actions may it perform?
5. What data crosses the boundary?
6. Is transport protected?
7. Can the caller move laterally after compromise?
8. Can the connection be revoked quickly?
9. How is activity detected/audited?
10. What is the blast radius if the source or destination is compromised?

## 13. Example decisions

### Public PostgreSQL for developer convenience

**Decision:** `REJECTED`.

Use corporate identity + MFA + PAM/bastion/private access rather than direct internet exposure.

### 1SB outside bank VPC

**Decision:** not inherently a problem. Treat 1SB as an external trust domain and secure the integration path. Same-VPC placement is not required to establish trust.

### Public customer quote endpoint

**Decision:** may be acceptable at the gateway/edge with DDoS/WAF, authentication/abuse controls as appropriate, validation and private downstream services. The quote microservice/database need not become public.