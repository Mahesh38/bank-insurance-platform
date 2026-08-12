# Platform — cross-cutting architecture & specifications

**Parent:** [`docs/README.md`](../README.md)
**Scope:** Concerns that apply to **every service** in the platform, not to any one module.

---

## What belongs here

A document belongs in `platform/` when it is true of the platform as a whole — service
decomposition, infrastructure, security posture, identity. Anything scoped to a single
module belongs with that module instead.

| | `platform/` | Module folders |
|---|---|---|
| **Applies to** | Every service | One service / adapter |
| **Example** | "All services authenticate via the workforce BFF" | "The 1SB quote endpoint polls with backoff" |
| **Today** | architecture review, workforce authN/authZ | [`1sb-insurance-integration/`](../1sb-insurance-integration/README.md) |

---

## Contents

| Folder | What it is | Status |
|--------|-----------|--------|
| **[architecture-review/](./architecture-review/README.md)** | Independent Solution Architect review — target-state **AWS/EKS microservices** architecture: ~16 services, sync/async patterns, data architecture, security/NFRs, delivery estimate | ⚠️ **Recommendation** — not yet approved by PO/Compliance/Sponsor |
| **[authentication-authorization/](./authentication-authorization/README.md)** | **Workforce authentication & authorization SSOT** — token-hiding BFF, provider abstraction (Keycloak → Cognito/AD), RBAC + ABAC + relationship policies | ✅ **Approved** architecture and implementation baseline |

**Stakeholder review starts here:**
[Stakeholder LLD and Architecture Approval Baseline](./architecture-review/09-stakeholder-lld-approval-baseline.md).
It contains the complete platform diagram, five layer drill-downs, implemented component LLDs,
service/data/cache/communication matrices, open approval decisions, and the controlled evolution
workflow. Its current status is **Draft**, so proposed target components are not build authority.

**These two carry different weight.** The auth SSOT is binding on implementations today.
The architecture review is a proposal whose technology choices are tracked as `ARCH-xxx`
decisions in
[08-architecture-decision-log.md](./architecture-review/08-architecture-decision-log.md).

---

## Which services implement what

The workforce auth SSOT is the contract for three services in this repository:

| Service | Role | Implementation notes |
|---------|------|---------------------|
| [`workforce-access-bff`](../../services/workforce-access-bff/README.md) | Token-hiding BFF — the only thing Flutter talks to | Never returns OAuth tokens to the client |
| [`identity-provider-adapter-service`](../../services/identity-provider-adapter-service/README.md) | Isolates provider-specific behaviour | Keycloak today; swappable |
| [`identity-authorization-service`](../../services/identity-authorization-service/README.md) | Business source of truth for roles, scopes, grants | PDP — default-deny |

Run the local identity stack with:

```bash
docker compose --env-file .env.identity -f docker-compose.identity.yml up --build
```

---

## How this relates to the rest of `docs/`

```text
au-bank-insurance-platform/     ── what to build (business SSOT)
            │
            ▼
platform/architecture-review/   ── how the whole platform should be built (proposal)
            │
            ├──► platform/authentication-authorization/   cross-cutting identity (approved)
            │
            └──► 1sb-insurance-integration/               one module inside that picture
```

The architecture review explicitly **places** the 1SB adapter inside the target
architecture — as one adapter behind an Integration Hub — rather than replacing it. See
[02-target-microservices-architecture.md](./architecture-review/02-target-microservices-architecture.md).

---

## Related

- Security & NFR posture: [06-security-compliance-and-nfrs.md](./architecture-review/06-security-compliance-and-nfrs.md)
- Inbound auth for the 1SB module (JWT + mTLS): [`CONFIRM-03-inbound-auth.md`](../1sb-insurance-integration/service-ssot/phase-0/CONFIRM-03-inbound-auth.md)
- Business decisions this architecture must respect: [`DECISION-LOG.md`](../au-bank-insurance-platform/DECISION-LOG.md)
