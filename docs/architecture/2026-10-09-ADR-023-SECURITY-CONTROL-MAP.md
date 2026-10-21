# ADR-023 security control map — External NLB + Istio without weakening posture

**Status:** Binding companion to [`ADR-023`](../platform/architecture-review/08-architecture-decision-log.md) (Accepted 2026-10-09)  
**Purpose:** Prove every control formerly on API Gateway (or required by identity ADRs) is **re-homed**, not dropped.

| ID | Former / required control | New home under ADR-023 | Verify how |
|----|---------------------------|------------------------|------------|
| S1 | Bank SaaS WAF/DDoS in front | Cloudflare + F5-XC unchanged | Origin points at NLB only via F5 |
| S2 | Limit who can hit AWS entry | NLB SG = F5 (+CF) egress CIDRs only | SG audit; no 0.0.0.0/0 |
| S3 | Bypass resistance | F5 shared-secret header and/or mTLS to Istio Gateway | Negative test: direct NLB without secret → 403 |
| S4 | Private workloads | Pods/Keycloak/data in private subnets; private EKS API | No public NodePort/LB on domain Services |
| S5 | Keycloak not on internet | No Istio public route to Keycloak | External scan / Gateway route inventory |
| S6 | Token-hiding / no device OAuth tokens | BFF + Valkey session vault unchanged | Browser storage test; ARCH-019 |
| S7 | Business authZ | PDP only; mesh authz ≠ insurance rules | ArchUnit / contract tests |
| S8 | PG callback isolation (TB-6) | Istio route `/callbacks/pg/*` + PG IP allowlist + signature verify | Allowlist + forged-signature tests |
| S9 | Size / throttle before overload | Envoy limits + Valkey per-principal counters | Load/abuse tests |
| S10 | Edge access logs | NLB + Envoy → Firehose/OpenSearch (`ADR-013`) | Log presence in OS; not used as evidence SoT |
| S11 | East–west isolation | Istio STRICT mTLS + retain NetworkPolicy | PeerAuthentication audit |

**Forbidden regressions**

- Exposing Keycloak, Aurora, MSK, or OpenSearch publicly  
- Putting OAuth tokens in Flutter  
- Replacing PDP with Istio AuthorizationPolicy for SP/insurer rules  
- Opening NLB SG to the world “temporarily”  
- Skipping PG IP allowlist because “F5 is enough”  
- Removing NetworkPolicy on day one of mesh STRICT  

**Owners:** Mahesh (structure) · bank platform / Shivanshi (NLB+Istio ops) · WS-2 (BFF/Keycloak invariants).
