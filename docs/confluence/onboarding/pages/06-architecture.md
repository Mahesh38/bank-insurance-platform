# Architecture

> **Teaching pack** · `SUG-20261005-cfp` · not a source of truth.
>
> **Confluence parent:** AU Bank Insurance Platform — Start here  
> **This page:** child #6  
> **Nest under this page:** [How a request travels](./06a-hops.md) · [Bounded contexts and services](./06b-contexts.md)

## Two pictures, on purpose

| Picture | Question | Do not use it for |
|---------|----------|-------------------|
| North Star (`docs/hdl.svg`) | Where is the platform going, which release? | Starting R1–RN work |
| R0 reference architecture (`docs/architecture/r0-reference-architecture.svg`) | What are we building **now**? | Claiming Health / DIY / NTB are in flight |

R0 builds the **seams** later releases need, and nothing more. If you only publish the North Star, people ask why Health is not built. If you only publish R0, people ask why we have a Hub and canonical contracts for one product. You need both.

Those SVGs are **renderings**. [`R0-HLD.md`](../../../architecture/R0-HLD.md) walks the R0 picture in prose. [`03-solution-architecture-r0.md`](../../../platform/ws3-platform/03-solution-architecture-r0.md) owns the service set.

## Teaching view of the hop

![How a request travels](../diagrams/05-hop-architecture.png)

Full page: [How a request travels](./06a-hops.md).

**Inbound** to the RM app is Amazon API Gateway (`ADR-018`). **Outbound** to 1SB, SMS and bank APIs is **Apigee**; 1SB allowlists Apigee IPs (`ADR-020`). There is no public ALB in front of API Gateway.

## Teaching view of the contexts

![Bounded contexts](../diagrams/06-bounded-contexts.png)

Full page: [Bounded contexts and services](./06b-contexts.md).

`#n` is the identity. The name is a label (`NC-1`). `#5` is spoken as **Lead** (Opportunity is the durable-demand alias). `#10` and `#11` are **LOB-owned** — Health will get its own instances, not `if (lob == HEALTH)` inside Life.

## Stack (R0)

| Layer | Choice |
|-------|--------|
| App | Flutter NIP-APP (separate repo) |
| BFF / services | Java 21, Spring Boot, Gradle monorepo |
| Identity | Token-hiding BFF, Keycloak behind adapter, `identity-authorization-service` as PDP |
| Data | One Aurora PostgreSQL cluster, schema per context (`ADR-008`); DynamoDB for journey/session/jobs |
| Messaging | Outbox in front of MSK (`ADR-009`–`013` robustness round) |
| Provider | Integration Hub → `1sb-integration-service` (no Flyway, no JPA) → Apigee → 1SB |
| Compute | AWS EKS, attached to existing bank TGW / DX — do not clone the network |

## What this Java repo actually runs today

| Service | Port | Role |
|---------|------|------|
| `1sb-integration-service` | 8080 | Bank-facing 1SB adapter. **No datasource** |
| `bank-persistence-service` | 8081 | Platform-common DB (Flyway + JPA + `/internal/v1`) |
| `workforce-access-bff` | 8084 | Token-hiding BFF for workforce login |

**Next:** [How a request travels](./06a-hops.md) · [Sequence diagrams](./07-sequences.md)
