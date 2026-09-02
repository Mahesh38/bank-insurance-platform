# Source — Insurance Aggregation and Provider Connectivity Notes

**Intake ID:** `VIN-002`
**Date received:** 2026-08-20
**Provided by:** Repository owner (Mahesh38 · mh.narkar@gmail.com)
**Original medium:** External design/brainstorming session, supplied to the repository as text
**Relationship to `VIN-001`:** continuation — refines the provider-integration half of the North Star
**Status:** **REFERENCE — non-binding.** Transcribed for grounding under
[`09-target-state-architecture-doctrine.md §10`](../../context/roles/mahesh-principal-insurance-platform-architect/09-target-state-architecture-doctrine.md) rule `VI-01`.
**Reconciliation against accepted repository decisions:**
[`17-provider-aggregation-and-connectivity.md §12`](../../context/roles/mahesh-principal-insurance-platform-architect/17-provider-aggregation-and-connectivity.md)

> **Standing of this document.** Stakeholder architecture notes, transcribed substantively as
> supplied. Not approved scope, not an ADR. Where a note conflicts with an accepted decision, the
> conflict is recorded and routed through change control — it is not silently absorbed.

---

## Framing

> Do **not** design the bank platform "around 1SB." Design a **Bank Insurance Aggregation Layer**,
> with 1SB as the **first provider route**.
>
> That way, 1SB is an implementation choice for R0/R1 rather than part of the bank's permanent
> domain model.

This is a foundational architecture decision to take now, because otherwise 1SB can accidentally
become embedded throughout Quote, Proposal, Journey and Product services.

---

## 1. The target relationship

**Today**

```text
Life Quote / Proposal
        │
        ▼
Bank Aggregation Layer
        │
        ▼
   1SB Adapter
        │
        ▼
     1SilverBullet
        │
   ┌────┼────┐
   ▼    ▼    ▼
 HDFC ICICI Bajaj
```

**Future**

```text
Life Quote / Proposal
        │
        ▼
Bank Aggregation Layer
        │
        ▼
 Provider Router
        │
   ┌────┼────────────┬──────────────┐
   ▼    ▼            ▼              ▼
 1SB   HDFC        ICICI          Bajaj
Adapter Adapter     Adapter         Adapter
   │      │            │              │
   ▼      ▼            ▼              ▼
 1SB    Direct       Direct         Direct
```

**Both models run simultaneously.** There is no big-bang migration from "everything → 1SB" to
"everything → direct insurers". Instead:

```text
HDFC Life      → Direct
ICICI Life     → Direct
Bajaj Life     → 1SB
Insurer X      → 1SB
```

**The platform shouldn't care.**

## 2. This is effectively the bank's own aggregator

Over time this layer becomes the bank's own insurance aggregation platform. Avoid calling it simply
*Aggregator Service* — that becomes ambiguous. Prefer **Insurance Provider Aggregation Platform** or
**Insurance Integration & Aggregation Layer**.

Its job:

> Receive the bank's canonical insurance request, determine where it must go, fan out where
> necessary, translate it to provider-specific formats, manage provider communication, and return a
> canonical bank response.

## 3. Do not mix this with Journey Orchestration

There are now two kinds of orchestration and they must remain separate.

**Business Journey Orchestration** — *what should happen next in the customer's insurance journey?*

```text
Suitability → Quote → Proposal → Requirements → Payment → Issuance
```

That is the **Life Journey Orchestrator**.

**Provider Aggregation / Integration Orchestration** — *which provider should receive this request
and how should we communicate with it?*

```text
Quote request → Provider Router → HDFC Direct / ICICI Direct / 1SB
```

That is the **Insurance Aggregation Layer**.

**Do not merge these.** Otherwise one service will eventually contain customer journey state +
provider routing + retry + canonical transformation + insurer credentials, which becomes a major
monolith.

## 4. Bank canonical model becomes extremely important

The Life Quote Service should never construct `1SBQuoteRequest`. It should create
`BankLifeQuoteRequest`:

```json
{
  "journeyId": "J100",
  "lob": "LIFE",
  "productType": "TERM",
  "customerProfile": {},
  "coverage": {},
  "products": []
}
```

```text
Life Quote Service → Bank Canonical Quote Request → Aggregation Layer

Today:   Canonical Request → 1SB Adapter  → 1SB Request Format
Future:  Canonical Request → HDFC Adapter → HDFC Request Format

Reverse: HDFC response → HDFC Adapter → Bank Canonical Quote
         1SB response  → 1SB Adapter  → Bank Canonical Quote
```

The Quote Service sees exactly the same contract. **That is the anti-corruption layer you want.**

## 5. Do not create one giant canonical insurance object

Avoid:

```text
InsuranceRequest {
    every Life field
    every Health field
    every Motor field
    every Proposal field
    every Quote field
}
```

Canonical models should be **operation + LOB oriented**:

```text
LifeQuoteRequest      LifeQuoteResponse
LifeProposalRequest   LifeProposalResponse
HealthQuoteRequest    HealthQuoteResponse
MotorQuoteRequest     MotorQuoteResponse
```

Potentially common primitives: `Party` · `Address` · `Nominee` · `Money` · `Coverage` ·
`DocumentReference` · `ProviderReference`.

So canonical does **not** mean *one JSON that supports every insurance product in India*. It means
**one stable bank-owned contract for a particular business capability**.

## 6. The Router is the key future capability

| LOB | Product | Insurer | Operation | Route |
|---|---|---|---|---|
| Life | Term | HDFC | Quote | `DIRECT_HDFC` |
| Life | Term | HDFC | Proposal | `DIRECT_HDFC` |
| Life | ULIP | HDFC | Quote | `1SB` |
| Life | Term | ICICI | Quote | `DIRECT_ICICI` |
| Health | X | Insurer A | Quote | `1SB` |

```text
Request → Product / Provider Context → Routing Policy → Integration Route
```

The calling service doesn't know whether the request went direct or through 1SB.

## 7. Product Governance controls routing

A Bank Product Offering might contain:

```text
insurerId · productId · lob
quoteRoute · proposalRoute · issuanceRoute
effectiveFrom · effectiveTo
```

```text
HDFC TERM PRODUCT A       HDFC SAVINGS PRODUCT B
quoteRoute    = DIRECT_HDFC   quoteRoute    = 1SB
proposalRoute = DIRECT_HDFC   proposalRoute = 1SB
```

You could migrate **operation by operation**.

## 8. Don't assume Quote and Proposal share a route

There could be situations where Quote → Aggregator while Proposal → Direct insurer, or the reverse,
depending on the insurer/distribution setup.

Routing should therefore be based on **LOB + Product + Provider + Operation + Channel (perhaps) +
effective configuration** — not simply `insurer == HDFC → direct`.

## 9. Multi-quote is where the bank becomes a true aggregator

Currently:

```text
Bank → 1SB → { HDFC, ICICI, Bajaj }
```

Later:

```text
Life Quote → Aggregation Layer → { HDFC direct, ICICI direct, Bajaj direct }
           → Canonical Quote A / B / C → Quote Comparison Result
```

That's when the bank genuinely owns aggregation.

## 10. Provider fan-out needs isolation

One insurer must never hold up the whole quote process indefinitely.

```text
             Quote Aggregator
          /        |         \
       HDFC      ICICI      Bajaj
       700ms      1.1s      TIMEOUT

Result:  HDFC ✓   ICICI ✓   Bajaj unavailable
(not: Entire Quote Request FAILED)
```

— depending on the business rules. Each provider therefore needs: independent timeout · circuit
breaker · concurrency bulkhead · rate limiting · credential isolation · retry policy · error
mapping · telemetry.

**This is a major reason to have the bank aggregation layer.**

## 11. Support both synchronous and asynchronous providers

```text
Sync:      POST /quote → Quote immediately
Polling:   POST /quote → jobId ; GET /quote/{jobId} → PROCESSING → COMPLETED
Callback:  POST /quote → accepted ; later Webhook → Bank
```

The canonical caller should not need to understand every variation. The aggregation layer normalizes
provider interaction patterns and exposes a stable bank contract.

```text
              Provider Execution Model
           ┌─────────┼─────────┐
           ▼         ▼         ▼
        Sync      Polling   Callback
```

## 12. Provider references must be normalized

Keep mappings:

```text
bankJourneyId · bankQuoteId · bankProposalId
providerId · providerQuoteRef · providerProposalRef
providerTransactionRef · providerPolicyRef
```

**Never let `1SBJobId` become the primary identifier throughout your application.** Otherwise
removing 1SB later becomes painful.

## 13. Authentication is another reason for a proper abstraction

```text
Today:   Bank → 1SB authentication
Future:  HDFC → OAuth perhaps · ICICI → mTLS + API key · Bajaj → token exchange · other → another
```

The LOB Quote Service should not care. Provider adapters own provider authentication through a
common security framework: Credential Manager · Token Manager · Certificate Manager · Secrets
Manager · KMS · Rotation.

## 14. Callbacks enter through one controlled provider ingress

Do **not** expose `/life/hdfc/callback`, `/life/icici/callback`, `/health/x/callback` directly into
business services without controls.

```text
Insurer
   ↓
Provider Callback Gateway
   ↓
Authentication / mTLS / signature validation
   ↓
Provider Adapter
   ↓
Canonical Provider Event
   ↓
LOB Service
```

Example: `Raw HDFC Callback → HDFC Adapter → ProposalRequirementUpdated`. The Life Proposal service
receives the **bank event**, not HDFC's raw object.

## 15. Think of adapters as plugins

```text
Insurance Aggregation Platform
       ├── 1SB Adapter
       ├── HDFC Adapter
       ├── ICICI Adapter
       ├── Bajaj Adapter
       └── Future Provider Adapter
```

All implementing something conceptually like `QuoteProvider` · `ProposalProvider` · `PolicyProvider`
— not necessarily literally one Java interface for everything, but the principle is valuable.

```text
QuoteProvider     → quote()
ProposalProvider  → submit() · status()
PolicyProvider    → issuanceStatus()
```

Then a provider capability registry says which provider supports what.

## 16. The Provider Capability Registry will be needed

Not every insurer supports every feature.

```text
HDFC                      Provider B
 quote            ✓        quote      ✓
 proposal         ✓        proposal   ✓
 webhook          ✓        webhook    ✕
 medical tracking ✓        polling    ✓
 policy download  ✓
```

Maintain this as **configuration** rather than scattered `if` statements.

## 17. How this interacts with LOB cells

Preserve the LOB isolation principle.

```text
                    SHARED
          Integration Control Plane
          ──────────────────────────
          Canonical Standards
          Provider Registry
          Credential Framework
          Security
          Observability
          Error Standards
                       ↓
 LIFE CELL          HEALTH CELL        GENERAL CELL
    │                   │                  │
 Life Quote          Health Quote       GI Quote
 Life Proposal       Health Proposal    GI Proposal
    │                   │                  │
 Life Aggregation    Health Aggreg.     GI Aggreg.
 Runtime             Runtime            Runtime
    │                   │                  │
 Adapters            Adapters           Adapters
```

Because if Life generates huge quote traffic, it shouldn't consume all capacity from Health
integration. So again: **shared framework/control plane, isolated runtime/data plane.**

## 18. For R0, don't overbuild the future aggregator

Design the seams now; do not implement twenty things that aren't needed. R0 could practically be:

```text
Life Quote → Life Provider Gateway → Provider Router → 1SB Adapter → 1SB
```

That's enough. But the internal boundary is: **Life Quote does NOT know 1SB.**

Later:

```text
Provider Router
   ├── 1SB
   └── HDFC Direct
```

No Life Quote rewrite. **That's architectural success.**

## 19. Add a new major capability to the target capability notes

**Insurance Aggregation & Provider Connectivity**

**Purpose** — decouple the bank's insurance domains from aggregators and insurers.

**Owns**

- canonical provider contracts;
- provider routing;
- multi-provider fan-out;
- provider adapters;
- protocol transformation;
- provider authentication;
- timeout / retry / circuit breaker;
- rate limiting;
- callbacks;
- provider reference mapping;
- idempotency;
- error normalization;
- provider capability registry;
- provider-specific observability.

**Does NOT own**

- customer journey state;
- suitability;
- bank product selection;
- customer consent;
- business policy about which product should be recommended;
- insurer underwriting decisions.

**That boundary is extremely important.**

## 20. The future architecture

```text
Customer / RM / Call Center
          ↓
      Journey
          ↓
       LOB Cell
          ↓
 ┌──────────────────────┐
 │ Quote                │
 │ Proposal / Case      │
 └──────────┬───────────┘
            ↓
 ┌──────────────────────────────┐
 │ Bank Insurance Aggregation   │
 │                              │
 │ Canonical contracts          │
 │ Routing                      │
 │ Fan-out                      │
 │ Provider adapters            │
 │ Resilience                   │
 └────────────┬─────────────────┘
              │
     ┌────────┼─────────┐
     ▼        ▼         ▼
    1SB      HDFC      ICICI
     │       direct    direct
     ▼
 multiple insurers
```

---

## The permanent principle

> **1SB is a provider route, not a domain dependency.**

Today it may happen to be the only provider route. Tomorrow it can be one of several. Eventually the
bank's aggregation platform can completely replace its aggregation responsibility **without forcing
Quote, Proposal, Journey, Customer, Lead, Payment or Policy to change.**

That should become one of the permanent principles in the target architecture.

---

## Where this source is used

| Consumer | Use |
|---|---|
| [`09-target-state-architecture-doctrine.md`](../../context/roles/mahesh-principal-insurance-platform-architect/09-target-state-architecture-doctrine.md) | Invariants `TI-19`–`TI-22`; intake record `VIN-002`; the `SC-W3-5` reconciliation resolved by the control-plane/data-plane split |
| [`10-north-star-capability-model.md`](../../context/roles/mahesh-principal-insurance-platform-architect/10-north-star-capability-model.md) | `CAP-403` Insurance Aggregation & Provider Connectivity; `CAP-105` routing ownership |
| [`11-line-of-business-segregation.md`](../../context/roles/mahesh-principal-insurance-platform-architect/11-line-of-business-segregation.md) | Per-cell aggregation runtime |
| [`13-orchestration-doctrine.md`](../../context/roles/mahesh-principal-insurance-platform-architect/13-orchestration-doctrine.md) | The two-orchestration separation |
| [`17-provider-aggregation-and-connectivity.md`](../../context/roles/mahesh-principal-insurance-platform-architect/17-provider-aggregation-and-connectivity.md) | The full doctrine and the `VIN-002` reconciliation table |
