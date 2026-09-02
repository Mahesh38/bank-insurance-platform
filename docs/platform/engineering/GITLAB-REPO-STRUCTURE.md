# GitLab repository structure — backend services

**Owner:** Mahesh — Principal Insurance Platform Architect  
**Purpose:** Map Gradle modules to GitLab groups/projects ahead of the GitHub → GitLab migration  
**Status:** Proposed — for stakeholder review before cutover

## Proposed GitLab hierarchy

```text
au-bank-insurance-platform/          (top-level group)
├── platform-common/                 shared libs + persistence
│   ├── bank-common-error
│   ├── bank-common-security
│   ├── bank-common-audit
│   ├── bank-common-observability
│   ├── bank-common-secrets
│   └── bank-persistence-service
├── ws2-iam/                         workforce identity plane
│   ├── identity-provider-adapter-service
│   ├── identity-authorization-service
│   └── workforce-access-bff
├── ws3-domain/                      core sales & advisory bounded contexts
│   ├── customer-service              (#4)
│   ├── lead-service                  (#5)
│   ├── consent-service               (#6)
│   ├── suitability-service           (#7)
│   ├── product-catalogue-service     (#8)
│   ├── journey-orchestration-service (#9)
│   ├── quotation-service             (#10)
│   ├── proposal-service              (#11)
│   ├── payment-service               (#12)
│   └── policy-issuance-service       (#13)
├── ws3-integration/                 provider connectivity
│   ├── integration-hub-service       (#14)
│   ├── 1sb-integration-service       (#15)
│   └── direct-insurer-adapter-service
└── ws3-platform/                    cross-cutting platform services
    ├── audit-compliance-service      (#16)
    ├── notification-service          (#17)
    ├── reporting-mis-service         (#18)
    └── administration-config-service (#19)
```

## Monorepo today, multi-repo on GitLab (recommended)

During migration the **monorepo remains the build source of truth** until CI is split per group.
Each GitLab project can mirror one Gradle module path using `sparse-checkout` or a future
extract script. The catalogue [`backend-service-catalog.yaml`](./backend-service-catalog.yaml)
is the join key between architecture `#n`, Gradle `:services:{module}` and GitLab group.

## Ownership matrix (initial)

| GitLab group | Primary owner persona | Example modules |
|---|---|---|
| `platform-common` | Amit (Engineering) + Aarti (Database) | persistence, shared libs |
| `ws2-iam` | Deepali (Security) + Amit | authz, idp-adapter, workforce BFF |
| `ws3-domain` | Rajal (Product) + Mahesh (Architecture) | consent, journey, quotation, … |
| `ws3-integration` | Mahesh + Amit | integration-hub, 1sb, direct adapter |
| `ws3-platform` | Shivanshi (SRE) + Shailja (Compliance) | audit, notification, reporting, admin |

## CI policy sketch (per group)

| Policy | `platform-common` | `ws3-domain` | `ws3-integration` |
|---|---|---|---|
| Required reviewers | Engineering + DBA | Product + Architecture | Architecture + Security |
| Deploy to UAT | platform team | journey owner | integration owner |
| Secret scanning | mandatory | mandatory | mandatory |
| ArchUnit | libs + persistence rules | hex + no provider leakage | adapter isolation rules |

## Edge / frontend (out of this increment)

| Component | GitLab group (proposed) | Notes |
|---|---|---|
| Customer BFF | `ws3-edge` (future) | Created manually after backend skeletons |
| RM Workspace BFF | `ws3-edge` (future) | Created manually after backend skeletons |
| Flutter client | `ws3-mobile` (future) | Out of scope for backend skeleton pass |

## Port allocation (local dev)

Ports `8080`–`8084` are reserved for implemented services. WS-3 skeletons use `8090`–`8105` per
[`backend-service-catalog.yaml`](./backend-service-catalog.yaml).

## Next steps after approval

1. Create GitLab groups matching the hierarchy above.
2. Assign group-level maintainer roles per ownership matrix.
3. Wire CI templates from `templates/microservice-skeleton/` into each project.
4. Split deploy pipelines by group once S09 platform foundation is green.
