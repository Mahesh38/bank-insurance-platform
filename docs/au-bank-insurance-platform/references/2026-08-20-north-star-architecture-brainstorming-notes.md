# Source — Target Insurance Platform: North Star Architecture Brainstorming Notes

**Intake ID:** `VIN-001`
**Date received:** 2026-08-20
**Provided by:** Repository owner (Mahesh38 · mh.narkar@gmail.com)
**Original medium:** External design/brainstorming session, supplied to the repository as text
**Status:** **REFERENCE — non-binding.** Transcribed for grounding under
[`09-target-state-architecture-doctrine.md §9`](../../context/roles/mahesh-principal-insurance-platform-architect/09-target-state-architecture-doctrine.md) rule `VI-01`.
**Reconciliation against accepted repository decisions:**
[`10-north-star-capability-model.md §9`](../../context/roles/mahesh-principal-insurance-platform-architect/10-north-star-capability-model.md)

> **Standing of this document.** These are stakeholder architecture notes, transcribed
> substantively as supplied. They are **not** approved scope, not an ADR, and they do not
> supersede any `DEC-*` row, ratified scope document or accepted ADR. Where a note conflicts with
> an accepted decision, the conflict is recorded in the reconciliation table and routed through
> change control — it is not silently absorbed.

---

## Framing instruction

> Before another architecture diagram, you need a **North Star capability model** that answers, for
> every major area:
>
> - Why does this capability exist?
> - What does it own?
> - What does it **not** own?
> - Who uses it?
> - What systems does it communicate with?
> - Why should it be shared or LOB-specific?
>
> Only after those answers are defensible should we decide whether something becomes a separate
> microservice.

---

## 1. Start with the actual North Star

- The platform is a bank-owned insurance distribution platform.
- It should support **Life first, then Health, then General/Motor** without redesigning the foundation.
- It should support **RM-assisted, customer DIY, hybrid, call-center-assisted and certified-SP-assisted** journeys.
- A customer should be able to **start in one channel and continue in another**.
- The bank should control which insurers and which insurer products it distributes.
- The bank owns the distribution journey; the insurer owns insurance risk/underwriting.
- AWS hosts the platform and application workloads remain private behind controlled public entry points.

## 2. Do not equate "capability" with "microservice"

- Customer, Consent, Lead, Journey, Payment, etc. are first business capabilities / bounded contexts.
- We should not decide today that every capability requires one microservice.
- Example: Party + Customer may initially be one service.
- Proposal + insurer requirement tracking may initially be one service.
- Architecture should establish **ownership boundaries first**; deployable boundaries can evolve later.

## 3. Customer / Party capability — why does it exist?

- We need one representation of the person irrespective of whether they are currently a bank customer.
- A DIY visitor can exist before the platform knows their CIF / customer ID.
- Later they may authenticate and link themselves to an existing bank relationship.
- Therefore conceptually: **Party = person**, while **Bank Customer = relationship of that party with the bank**.
- It should maintain **references**, not blindly replicate all CBS customer data.
- It communicates with bank customer systems / CBS through a controlled **bank integration layer**.
- It is **shared across all LOBs** — the same person buying Life and Health is still the same customer.

## 4. Lead / Opportunity — why does it exist?

- A Lead represents a **sales/business opportunity**, not an insurance transaction.
- It can originate from: RM, Branch, Bulk upload, Campaign, B2C, Call center, Renewal, Lapse recovery, Abandoned journey recovery.
- One opportunity may produce **more than one journey** over time.
- It carries sales context: source, branch, assignment, priority, LOB interest, campaign.
- It should **not** contain Quote / Proposal / Payment logic.
- Lead is **shared** — lead generation and assignment mechanics are largely the same across Life/Health.

## 5. Lead Work Management / Assignment — different from Lead itself

- Somebody needs to determine **who should act** on a Lead.
- Handles: queues, RM assignment, branch assignment, call-center assignment, priority, SLA, callbacks, escalations, follow-up, disposition.
- Example: Life quote abandoned for 24 hours → Call Center Life Recovery Queue.
- **Do not put this inside Journey or Quote.**
- Becomes extremely important when the call center arrives.

## 6. Journey — what exactly is it?

- Journey is the **execution of an insurance purchase/renewal process**.
- It represents: where the customer is, what has completed, what comes next, whether the journey is active/abandoned/completed, which actor/channel is currently assisting.
- Journey should **survive channel changes**.
- Example: Customer starts on mobile → Call center assists → RM continues → Customer pays on web. All of that should remain **the same Journey ID**.

## 7. Journey Registry vs Journey Execution

- A distinction worth preserving.
- **Shared Journey Registry** answers: Who is this journey for? Which LOB? What stage? Who owns it now? Where should it be routed?
- **Detailed execution** belongs inside the relevant LOB.
- Therefore: `Journey Registry → LOB Router → Life/Health/General Journey Execution`.
- Prevents the shared Journey service from becoming a giant state machine containing every insurance type.

## 8. LOB is an isolation boundary, not one giant service

- Life, Health and General should be independently **deployable, scalable, releasable, monitorable, failure-isolated**.
- Life traffic should not force us to scale Health.
- A Health release should not break Life.
- Therefore each LOB becomes an **execution cell**.
- But a cell does **not** mean one life-service — it means a collection of LOB-specific capabilities.

## 9. What should ideally exist inside each LOB cell?

- Initially: Journey Execution, Suitability/Eligibility rules, Quotation, Proposal/Case Management, LOB/provider integrations.
- Not necessarily five microservices immediately.
- These are the areas where Life, Health and Motor materially differ.

## 10. Suitability / Need Analysis — why is it LOB-sensitive?

- Life asks about income, dependents, cover amount, tenure, objectives.
- Health asks about family members, health conditions, desired sum insured, geography.
- Motor deals with vehicle-related eligibility rather than Life-style suitability.
- There may be a **common framework** for questionnaires/rules, but the **rules and models belong to the LOB**.
- Suitability is a bank/distributor responsibility before presenting appropriate products.
- It is **different from insurer underwriting**.

## 11. Product Governance — this must be shared

- Insurer Catalogue and Bank Catalogue are **not** the same thing.
- An insurer may have 100 products; the bank might approve only 8.
- We need to know: insurer, LOB, insurer product, bank-approved offering, effective dates, enabled channels, ETB/NTB availability, integration route, status, eligibility metadata.
- A major **shared configuration/governance capability**.
- LOB services consume this configuration but should not individually maintain their own insurer master.

## 12. Quotation — strong LOB boundary

- Life, Health and Motor quotes have radically different data and pricing constructs.
- Quote stays **inside the LOB cell**.
- Quote owns: quote request, selected eligible products, multi-quote orchestration, canonical quote representation, quote expiration, quote status/comparison.
- It should **not** understand raw insurer payloads — provider-specific transformation belongs to integrations.

## 13. Proposal — another strong LOB boundary

- Proposal data varies enormously across Life/Health/General.
- Proposal owns: application data, nominees, insured persons, declarations, insurer questionnaires, proposal submission, insurer proposal references, outstanding insurer requirements.
- Belongs **inside the LOB cell**.
- Also where bank-side tracking of underwriting requirements can initially live.

## 14. Underwriting — explicitly define what we do NOT own

- The bank platform **does not underwrite insurance**.
- Insurer owns: risk decision, medical assessment, loading, exclusion, rejection, postponement, final acceptance.
- Platform owns: insurer status, requirements, pending documents, medical request, counteroffer, next action, communication back to customer/RM.
- Therefore call it **Proposal / Case Management**, not a bank Underwriting Engine.

## 15. Consent — why does it deserve its own capability?

- Because consent is **evidence, not a Boolean**.
- The system must know: who consented, what data may be used, for which purpose, with which parties, which consent-text/version, when, how it was captured, whether it was revoked/expired.
- All LOBs need it → Consent is **shared**.
- Whether it is physically an independent service on day one can be decided later.

## 16. Bank integration should be separated from insurance-provider integration

- LOB services should not directly implement CBS / mobile banking / CRM-specific protocols everywhere.
- Create a **bank integration boundary** for: CBS/customer lookup, account verification, RM/branch context, other bank systems.
- Separately have **insurer/provider integration boundaries**.
- Conceptually: `Insurance Platform → Bank Integration → CBS/Bank systems` and `LOB Cell → Provider Integration → Aggregator/Insurer`.
- Prevents bank-system change from leaking into Life Quote / Health Proposal code.

## 17. Insurance/provider integration needs a shared framework but isolated runtime

- **Share:** canonical contracts, authentication framework, credential handling, timeouts, retries, circuit breakers, error model, observability, idempotency, certificate handling.
- **Runtime can become:** Life Integration, Health Integration, General Integration.
- Gives common engineering without creating one massive Integration Hub bottleneck.

## 18. Payment — shared platform capability

- Payment mechanics are fundamentally common across LOBs.
- LOB determines **when** payment is required and what amount/context applies.
- Payment platform owns: payment session, link, gateway integration, callback, status, reconciliation, uncertain payment resolution, refund reference where applicable.
- Life/Health/General reuse it.
- Because everyone depends on it, Payment becomes a **highly resilient shared platform capability**.

## 19. Issuance needs careful separation

- Do **not** put all insurer issuance behaviour into a generic Policy Service.
- Provider-specific issuance interaction remains inside the respective LOB/provider integration.
- Example: `Life Proposal → Life Integration → HDFC/1SB → Policy Issued`.
- When issuance succeeds, create a canonical event: **`PolicyIssued`**.

## 20. Policy Portfolio / Registry — shared

- After issuance the bank needs **one customer insurance portfolio**.
- Shared Policy Registry maintains the canonical bank-side policy view: customer, insurer, LOB, product, policy number, premium, coverage, start/end dates, policy status, document reference, renewal date.
- Gives RM/customer one cross-LOB portfolio.
- It should **not** implement insurer-specific issuance protocols.

## 21. Renewal and Lapse are new opportunities, not reopening old journeys

- Original sale: `Opportunity O1 → Journey J1 → Policy P1`.
- Renewal: `P1 → Renewal Opportunity O2 → Journey J2`.
- Lapse recovery: `P1 → Lapse Recovery Opportunity O3 → Journey J3`.
- Keeps historic journeys **immutable and understandable**.

## 22. Abandoned journey recovery is another Opportunity / Work Management use case

- Customer reaches Quote and leaves.
- Journey keeps its existing state.
- Engagement system detects inactivity.
- Work Management creates a recovery task.
- Call center / SP / RM can **resume the same journey**.
- No duplicate proposal/journey should be created merely because another actor took over.

## 23. Call Center and Certified SP should be actors, not separate business architectures

- Don't build a `call-center-quote-service`.
- The **same Life Quote Service** should be usable by Customer, RM, Call center and Certified SP.
- Their allowed actions differ through **authorization/certification**.
- Example: a non-certified agent may assist navigation and arrange a callback; a certified actor may perform regulated sales activities permitted by the approved business/compliance model.

## 24. Actor Capability / Authorization becomes important

- **Authentication** tells us who you are.
- **Authorization** must understand: role, actor type, certification, LOB, branch, assigned customer/lead, journey, current stage, requested action.
- Therefore permission is a **backend business control**, not a UI hide/show rule.

## 25. Documents should be shared, workflows should remain domain-owned

- S3/document infrastructure does not need to be rebuilt for every LOB.
- Shared Document capability manages: storage, metadata, access, scanning, retention, versioning.
- **Life Proposal decides which document is required**; the Document platform decides how it is securely stored/retrieved.

## 26. Notification should be shared

- SMS, email, push and (future) WhatsApp should not be independently implemented by every LOB.
- Domain generates an intent/event: `PaymentLinkReady`, `MedicalRequired`, `PolicyIssued`.
- Notification delivers using the appropriate configured channel.
- Notification failure should generally **not corrupt business state**.

## 27. Engagement / Recovery should be separate from Notification

- **Notification sends a message. Engagement decides whether and when to engage.**
- Example: 30 min inactivity → push; 6 hours → WhatsApp; 24 hours → call-center work item.
- This distinction matters as the sales organisation matures.

## 28. Communication / Interaction Timeline

- Particularly valuable for hybrid/call-center journeys.
- We should be able to see: Customer started, Quote viewed, RM assisted, call center contacted, Certified SP joined, proposal submitted, customer paid.
- Gives servicing, sales, operations, compliance and audit a **single chronology**.

## 29. Audit is not the same as Logging

- Logs answer: *"What happened technically?"*
- Audit answers: *"Who did what business action, when, under what context, with what evidence?"*
- Audit must cover critical business events and be tamper-resistant/append-only per approved retention requirements.
- **Preserve the strong audit model already designed in R0.**

## 30. Events become the communication backbone where synchronous coupling is unnecessary

- Don't make every downstream capability part of one synchronous transaction.
- Example: `PolicyIssued` can trigger Policy Portfolio, Notification, Reporting, Audit, Renewal scheduling, Commission/Finance.
- Synchronous where an immediate result is required; durable asynchronous where eventual processing is acceptable.
- **Continue with the existing transactional-outbox approach initially**; event-bus/Kafka adoption can happen when justified.

## 31. Observability is a cross-cutting platform capability

- Every transaction traceable using: `correlationId`, `traceId`, `journeyId`, `opportunityId`, `proposalId`, `paymentId`, `policyId`, LOB, provider, channel, actorType.
- Technical metrics must connect with business metrics.
- Example: Health Quote latency, Life Proposal success, HDFC timeout rate, B2C abandonment, Call-center recovery conversion.
- **Each LOB needs independent dashboards and SLOs.**

## 32. Failure isolation must exist at both LOB and provider level

- Health failure must not stop Life. One insurer's failure should ideally not stop another's.
- Use provider bulkheads, bounded connection pools, timeouts, circuit breakers, bounded retries, queue/backpressure where appropriate.
- Independence should be **demonstrable operationally**, not just shown as separate diagram boxes.

## 33. Configuration is going to become one of the biggest capabilities

- Insurance distribution is configuration-heavy.
- Controlled configuration for: insurers, products, channels, branches, eligibility, provider routes, feature activation, dates, product versions.
- Configuration changes require: versioning, maker/checker where required, audit, effective dates, rollback.
- Hard-coded product/provider behaviour will become a future bottleneck.

## 34. Data ownership should be strong; physical DB separation should be pragmatic

- No service should directly query another service's tables. **Domain ownership is non-negotiable.**
- But "database per service" doesn't necessarily mean 40 separate Aurora clusters.
- Start with logical isolation and strong credentials/schema ownership where appropriate.
- Physically split heavy workloads when scale/security/RTO justify it.

## 35. The platform needs an explicit operational plane

- CI/CD, IaC, secrets/KMS, monitoring, logging, tracing, dashboards, alerts, SLOs, backup/restore, DR, security scans, vulnerability management, runbooks.
- These aren't "supporting details" — they're part of the target architecture for a bank platform.

## 36. Release strategy should follow business maturity, not architecture completeness

- R0 does not need every North Star capability.
- R0 proves **Life end-to-end**.
- Subsequent releases add Life scale, DIY/hybrid, call center, renewal/lapse.
- Only after the shared platform is proven should **Health** be plugged into the same architecture.
- **General/Motor** follows afterward.
- The architecture should make those additions possible **without forcing us to build them prematurely**.

---

## The simplest way to explain the target system to stakeholders

The whole North Star reduces to **five ideas**.

### 1. Shared Customer & Sales Platform

```text
Customer
Lead / Opportunity
Assignment
Consent
Product Governance
Journey Registry
```

> Who is the customer, why are we contacting them, what are we allowed to do, and where are they in the journey?

### 2. Independent Insurance LOB Engines

```text
Life                     Health                   General
 ├── Journey              ├── Journey              ├── Journey
 ├── Quote                ├── Quote                ├── Quote
 ├── Proposal / Case      ├── Proposal / Case      ├── Proposal / Case
 └── Integration          └── Integration          └── Integration
```

> How does this specific insurance business actually work?

### 3. Shared Transaction & Portfolio Capabilities

```text
Payment
Documents
Policy Portfolio
Notification
Audit
```

> Which capabilities should not be rebuilt three times?

### 4. External Integration Boundaries

```text
Bank Integration  →  CBS / Bank systems
LOB Integration   →  1SB / Insurers
```

> How do we isolate external-system complexity from the business platform?

### 5. Platform Engineering & Governance

```text
Security
Authorization
Observability
Events
Configuration
CI/CD
DR
SRE
Audit
```

> How do we keep it secure, manageable, scalable and operable?

---

## Stated next step

> The next discussion should probably **not** be "which boxes do we add?" It should be to take these
> capabilities one by one — starting with **Customer → Opportunity/Lead → Journey → LOB Cell** — and
> establish the exact ownership, input/output and communication boundary for each. Once those four
> are absolutely clear, a much smaller architecture diagram will almost draw itself.

---

## Where this source is used

| Consumer | Use |
|---|---|
| [`09-target-state-architecture-doctrine.md`](../../context/roles/mahesh-principal-insurance-platform-architect/09-target-state-architecture-doctrine.md) | Horizons, invariants, variation axes, intake record `VIN-001` |
| [`10-north-star-capability-model.md`](../../context/roles/mahesh-principal-insurance-platform-architect/10-north-star-capability-model.md) | Capability definition contract, five-plane model, capability catalogue, reconciliation table |
| [`11-line-of-business-segregation.md`](../../context/roles/mahesh-principal-insurance-platform-architect/11-line-of-business-segregation.md) | LOB cell model, shared-vs-LOB test, LOB onboarding |
| [`12-journey-segregation.md`](../../context/roles/mahesh-principal-insurance-platform-architect/12-journey-segregation.md) | Opportunity/Journey/Policy lifecycle, registry vs execution, channel continuity, actor model |
| [`13-orchestration-doctrine.md`](../../context/roles/mahesh-principal-insurance-platform-architect/13-orchestration-doctrine.md) | Routing, events, work management, engagement vs notification |
| [`14-shared-capability-doctrine.md`](../../context/roles/mahesh-principal-insurance-platform-architect/14-shared-capability-doctrine.md) | Shared capability qualification, integration boundaries, configuration, documents, data ownership |
| [`15-actor-identity-and-authorization.md`](../../context/roles/mahesh-principal-insurance-platform-architect/15-actor-identity-and-authorization.md) | Bank AD invariant, actor capability model, certification-aware authorization |
