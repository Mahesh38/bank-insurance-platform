# Architecture Review — AU Bank Insurance Distribution Platform

**Up:** [docs index](../../README.md) → [platform](../README.md) → **architecture review**
**Role:** Solution Architect independent review
**Input:** All existing documentation in `docs/au-bank-insurance-platform/` (business/product SSOT) and `docs/1sb-insurance-integration/` (prior engineering spike — one adapter microservice)
**Mandate for this review (explicit constraints given by the requester):**

1. Target architecture must be **microservices** end to end.
2. Target cloud is **AWS only** — no other cloud/on-prem assumption.
3. Compute must be **elastic** — workloads run on **Kubernetes (Amazon EKS)**.
4. Cover **service decomposition**, **sync vs. async communication**, **database design**, and a **delivery timeline** for an **enterprise-grade** platform.

**Status:** Architecture *recommendation* — not yet approved by PO/Compliance/Sponsor. It sits alongside, and does not overwrite, the business SSOT in `docs/au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md` and `DECISION-LOG.md` (D-001…D-014). Where this review makes a technology choice (AWS, EKS, specific AWS managed services), it is a **new architecture decision**, tracked separately in [08-architecture-decision-log.md](./08-architecture-decision-log.md) using an `ARCH-xxx` ID range so it never collides with the existing `D-xxx` / `DOC-xxx` business decision IDs.

---

## Why this folder exists

The repository currently holds two layers of prior work:

| Layer | Location | What it actually is |
|-------|----------|----------------------|
| Business/product SSOT | `docs/au-bank-insurance-platform/` | Charter, BRD, capability map, working decisions for the **full Insurance Distribution Platform** (Lead → Consent → Suitability → Catalogue → Quote → Proposal → UW tracking → Payment → Policy → Reporting) |
| Engineering spike | `docs/1sb-insurance-integration/` + `services/1sb-integration-service`, `services/bank-persistence-service` | A **thin integration adapter** for one aggregator (1SilverBullet), explicitly scoped as "not the full platform" (see `knowledge-base/08-integration-strategy.md`) |

No document in the repository yet says: how many services the *whole platform* should have, which AWS building blocks host them, which calls are synchronous vs. asynchronous, what the data architecture looks like end to end, or how long the build takes. That is the gap this review closes.

## How to read this review

| # | Document | Answers |
|---|----------|---------|
| 1 | [01-executive-summary-and-approach.md](./01-executive-summary-and-approach.md) | What I read, how I framed the problem, top-line recommendation |
| 2 | [02-target-microservices-architecture.md](./02-target-microservices-architecture.md) | How many services, what each owns, MVP vs. target-state sequencing |
| 3 | [03-communication-patterns.md](./03-communication-patterns.md) | Sync vs. async per interaction, event catalog, API Gateway / mesh design |
| 4 | [04-aws-infrastructure-architecture.md](./04-aws-infrastructure-architecture.md) | EKS layout, elasticity/autoscaling, full AWS service mapping |
| 5 | [05-data-architecture.md](./05-data-architecture.md) | Database-per-service design, what engine per service, retention |
| 6 | [06-security-compliance-and-nfrs.md](./06-security-compliance-and-nfrs.md) | Security controls, IRDAI/RBI posture, availability/latency/DR targets |
| 7 | [07-delivery-roadmap-and-estimate.md](./07-delivery-roadmap-and-estimate.md) | Phased plan, team shape, timeline estimate and its assumptions |
| 8 | [08-architecture-decision-log.md](./08-architecture-decision-log.md) | `ARCH-xxx` decisions this review commits to, and why |
| 9 | [09-hld-review-question-responses.md](./09-hld-review-question-responses.md) | Responses to the internal review questions on `docs/hdl.svg` — lead system, telecaller/DIY, journey vs lead, quote caching, LOB isolation, retention, post-proposal requirements, post-issuance BMS (analysis only, no decisions) |

## One-paragraph summary

Build the platform as roughly **16 domain-aligned microservices** grouped into five layers (Channel/Edge, Core Sales & Advisory, Fulfilment, Integration, Platform/Cross-cutting), each with **its own datastore** (Aurora PostgreSQL, DynamoDB, or Redis, chosen per access pattern — not one shared persistence service), running on **Amazon EKS** with Karpenter/HPA-driven elasticity, talking to each other **synchronously for anything a human is waiting on** (BFF → Suitability, Catalogue, Journey orchestration) and **asynchronously via Amazon MSK (Kafka) for everything that is a side effect of a completed step** (audit, notifications, reporting, reconciliation). The existing `1sb-integration-service` becomes one adapter behind an **Integration Hub** service, exactly as `knowledge-base/08-integration-strategy.md` already frames it — this review does not discard that work, it places it correctly inside the bigger picture. Estimated MVP (Life-only, ETB, Group A + B, RM + self + hybrid) build time: **~8–10 months** with 4–5 parallel squads after a ~6-week foundation phase — see the roadmap doc for assumptions and how that number moves with team size.
