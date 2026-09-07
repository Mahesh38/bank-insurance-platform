# GitLab repository structure — backend services

**Owner:** Mahesh — Principal Insurance Platform Architect  
**Purpose:** Map Gradle modules to GitLab groups/projects for the bank GitLab estate  
**Status:** Proposed — for stakeholder review before cutover  
**Bank DevOps create-from-scratch request (CI vs CD, roles, security, coverage):** [`GITLAB-BANK-DEVOPS-PROVISIONING.md`](./GITLAB-BANK-DEVOPS-PROVISIONING.md)  
**Shareable Word work-order to send DevOps:** [`AU-SFB-NIP-GitLab-DevOps-Work-Order.docx`](./AU-SFB-NIP-GitLab-DevOps-Work-Order.docx)

## Proposed GitLab hierarchy

```text
au-bank-insurance-platform/          (top-level group)
├── platform/                        CI templates + governance docs (Wave 0)
│   ├── ci-templates
│   └── nip-governance
├── backend/                         Wave 0 Gradle monorepo
│   └── nip-backend
├── frontend/                        Wave 0 Flutter NIP-APP
│   └── nip-app
├── infra/                           bank DevOps only — Terraform / Terragrunt / CD
├── platform-common/                 shared libs + persistence
│   ├── bank-common-error
│   ├── bank-common-domain
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
├── ws3-platform/                    cross-cutting platform services
│   ├── audit-compliance-service      (#16)
│   ├── notification-service          (#17)
│   ├── reporting-mis-service         (#18)
│   └── administration-config-service (#19)
└── ws3-edge/                        customer channel (R1 — empty)
    └── customer-bff
```

## Wave 0 monorepo, Wave 1 multi-repo (recommended)

Until shared libraries publish to the bank Maven registry, **`backend/nip-backend` is the
build source of truth**. Each GitLab project in the groups below is either empty (extract
target) or, after Wave 1, one Gradle module. The catalogue
[`backend-service-catalog.yaml`](./backend-service-catalog.yaml) is the join key between
architecture `#n`, Gradle `:services:{module}` and GitLab group.

Application teams commit source and CI includes. Bank DevOps owns group creation, runners,
Terragrunt/Terraform, AWS and CD loc — see [`GITLAB-BANK-DEVOPS-PROVISIONING.md`](./GITLAB-BANK-DEVOPS-PROVISIONING.md).

## Ownership matrix (initial)

| GitLab group | Primary owner persona | Example modules |
|---|---|---|
| `platform` | Shivanshi (SRE) + Amit | ci-templates, nip-governance |
| `backend` / `frontend` | Amit (Engineering) | Wave 0 `nip-backend`, `nip-app` |
| `infra` | Bank DevOps + Shivanshi | Terragrunt/Terraform, CD loc — application team Reporter only |
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

## Edge / frontend

| Component | GitLab location | Notes |
|---|---|---|
| Workforce / NIP BFF | `ws2-iam/workforce-access-bff` | Token-hiding BFF; Flutter talks only to this. Implemented module |
| NIP-APP (Flutter) | `frontend/nip-app` | One client for web + Android + iOS; RM/ISR/admin/ops are roles (ADR-015). Wave 0 |
| Customer BFF | `ws3-edge/customer-bff` | Empty until R1 — do not wire CI/CD |
| Second admin/ops app | — | Rejected — do not create |

## Port allocation (local dev)

Ports `8080`–`8084` are reserved for implemented services. WS-3 skeletons use `8090`–`8105` per
[`backend-service-catalog.yaml`](./backend-service-catalog.yaml).

## Next steps after approval

Bank DevOps executes [`GITLAB-BANK-DEVOPS-PROVISIONING.md`](./GITLAB-BANK-DEVOPS-PROVISIONING.md):

1. Create GitLab groups matching the hierarchy above.
2. Assign roles per that document’s access matrix (application team is not Owner; infra is Reporter-only for them).
3. Wire CI templates from `platform/ci-templates` into Wave 0 projects.
4. Split deploy pipelines by group once S09 platform foundation is green.
