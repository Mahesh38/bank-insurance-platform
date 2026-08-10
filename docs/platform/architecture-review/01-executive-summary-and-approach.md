# 01 — Executive Summary & Approach

## What I read, and what I concluded from it

### The business ask (from `docs/au-bank-insurance-platform/`)

AU Bank wants a **bank-owned, insurer-agnostic Insurance Distribution Platform**, not a single integration script. The charter (`00-project-charter.md`) is explicit: own the journey **lead → need analysis → consent → suitability → quote → proposal → payment → policy issuance**, for **Life only** (Term, ULIP, Savings/Investment) in the MVP, for **existing bank customers (ETB)**, across **RM-assisted, self-service, and hybrid** channels from day one, using **1SilverBullet (1SB)** as today's aggregator but **never locking the bank's own APIs to 1SB's wire format** (D-003, D-004, A12).

The capability map (`knowledge-base/03-capability-map.md`) and information model (`knowledge-base/07-information-model-and-rules.md`) give the actual domain boundaries: Identity & Access, Customer, Lead, Consent, Suitability, Product Catalogue, Quote, Proposal, Underwriting (tracking, not underwriting itself), Payments, Policy, Communications, Reporting, Administration, Integration Hub, Audit & Compliance. The PO note on that map is important and I've honored it: *"Domains ≠ mandated microservice count. They are ownership boundaries for SoR and APIs."* Deciding the actual service count and their AWS/Kubernetes shape is exactly the architecture work this review does.

### The prior engineering work (from `docs/1sb-insurance-integration/`)

A real, well-designed microservice already exists: `1sb-integration-service` (bank-facing 1SB adapter, hexagonal/ports-and-adapters, SOLID/DRY/KISS, Case-2 orchestration pattern), paired with `bank-persistence-service` (a shared platform persistence service consumed over internal HTTP by the integration service and, later, an `audit-consumer-service`). This is good work and I am **not** redesigning it. But `knowledge-base/08-integration-strategy.md` is explicit that this spike is a **candidate adapter slice for Phase A**, not the platform. I've treated it as the reference implementation for exactly one box in the target architecture: the **Integration Hub** layer.

One thing I am flagging, not silently inheriting: `bank-persistence-service` as *"one DB service that every consumer talks to over HTTP"* is a reasonable shortcut for two services in a spike. It stops being a microservices architecture the moment 10+ business-domain services (Customer, Lead, Consent, Suitability, Catalogue, Payment, Policy…) all funnel their writes through one shared persistence deployable — that's a distributed monolith with extra network hops, not database-per-service. Section [05](./05-data-architecture.md) addresses this directly: the *pattern* (a service owns its schema; nobody else touches its tables) stays, but it's applied **per business domain**, not centralized into one service for the whole platform. The one place a shared internal persistence facade genuinely earns its keep is the **audit/job-correlation domain**, which is what it was designed for.

### The explicit constraints given for this review

The requester was specific, and I've treated these as hard architecture constraints rather than options to weigh:

1. **Microservices, not a modular monolith.** Even though nothing in the business docs mandates a service count, "everything has to be microservices" is a given for this exercise.
2. **AWS only.** Every managed-service choice in this review assumes AWS; no multi-cloud abstraction layer, no on-prem fallback.
3. **Kubernetes for elasticity.** Amazon EKS is the compute substrate for all long-running services; elasticity comes from EKS-native autoscaling (Karpenter, HPA, KEDA), not from hand-rolled scaling scripts.
4. **Enterprise-grade.** Multi-AZ by default, defense-in-depth security, immutable audit trail, DR posture, and observability are treated as day-1 requirements, not later hardening — consistent with what the business docs already demand (compliance attribution, IRDAI, 7-year-class audit retention expectations implied by the 1SB spike's own NFR doc).

### What "good" looks like here, concretely

Success criteria I designed against, pulled directly from the charter and capability map:

- An RM or ETB customer completes a **Life sale** (assisted/self/hybrid) end to end through policy issuance.
- Compliance can reconstruct **which agent, which distributor, which consent, which suitability decision** for any transaction.
- The bank can **swap or add an aggregator/insurer** (1SB → direct insurer → hybrid) by changing one Integration Hub adapter — zero changes to channel apps or core domain services.
- Product can add a new Life variant or a Group B redirect flow **without standing up a new "platform."**
- The system is **elastic**: it absorbs RM-hour peaks and campaign-driven customer traffic without manual capacity planning, and scales back down (cost control).

### How this document set is organized

Each subsequent document answers one piece of the requester's question list directly:

| Requester's question | Where it's answered |
|---|---|
| "What all things need to be done, as an architect?" | This document + [02](./02-target-microservices-architecture.md) |
| "How many services, all microservices" | [02-target-microservices-architecture.md](./02-target-microservices-architecture.md) |
| "Use AWS only, elastic, Kubernetes" | [04-aws-infrastructure-architecture.md](./04-aws-infrastructure-architecture.md) |
| "How will services communicate, sync vs async, where performance matters" | [03-communication-patterns.md](./03-communication-patterns.md) |
| "How will the database look" | [05-data-architecture.md](./05-data-architecture.md) |
| "How much time to develop, enterprise-level" | [07-delivery-roadmap-and-estimate.md](./07-delivery-roadmap-and-estimate.md) |

## Key judgment calls (read before the rest)

1. **Sequence the platform, don't build 16 services on day 1.** [02](./02-target-microservices-architecture.md) gives a target-state count (~16) and an MVP subset (~9–10), because standing up every bounded context before there's a working end-to-end journey would repeat the exact anti-pattern the 1SB spike's own KISS rules warn against ("Start Term only").
2. **Journey Orchestration is the platform's spine.** The capability map lists domains but no single owner for the cross-domain "journey" state machine (`Journey { stage, externalRefs, partySnapshot }` already defined in `canonical-model/contexts.md`). I've made this an explicit service — without it, every BFF ends up re-implementing journey state, which breaks replaceability.
3. **Database-per-service, not shared persistence-for-everything.** Addressed above and in [05](./05-data-architecture.md).
4. **1SB stays an adapter, never the system of record.** Directly carried forward from `replaceable-middleware.md` D14/D4 and `08-integration-strategy.md` — this review reinforces rather than reinvents that decision.
5. **AWS region: India (`ap-south-1`, Mumbai)** is assumed for data residency, given AU Bank is an Indian scheduled bank and the docs flag IRDAI/RBI compliance and "data residency requirements" as pending validation (`DECISION-LOG.md` open items). This is called out as an assumption to confirm with compliance, not a silent default — see [06](./06-security-compliance-and-nfrs.md).
