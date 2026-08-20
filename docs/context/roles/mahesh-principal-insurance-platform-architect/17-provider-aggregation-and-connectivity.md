# 17 — Mahesh Provider Aggregation and Connectivity Doctrine

## 1. The permanent principle

> **`PR-01` — 1SB is a provider route, not a domain dependency.**
>
> Today it may happen to be the only provider route. Tomorrow it is one of several. Eventually the
> bank's own aggregation platform can absorb the aggregation responsibility entirely — **without
> forcing Quote, Proposal, Journey, Customer, Lead, Payment or Policy to change.**

This is `TI-19`, and it is the reason this file exists. The failure it prevents is quiet and
cumulative: 1SB does not get *decided* into the domain model, it *seeps* into it — a job id here, a
status enum there, a poll loop shaped like one vendor's contract — until removing it is a platform
rewrite rather than an adapter swap.

**The framing rule that follows from it (`PR-02`):** do not design the platform *around 1SB*. Design
a **bank insurance aggregation layer**, with 1SB as the **first provider route**. That makes 1SB an
implementation choice for R0/R1 rather than part of the bank's permanent domain model.

---

## 2. Target relationship

**Today**

```text
Life Quote / Proposal → Bank Aggregation Layer → 1SB Adapter → 1SilverBullet → { HDFC, ICICI, Bajaj }
```

**Future**

```text
Life Quote / Proposal → Bank Aggregation Layer → Provider Router
                                                      ├── 1SB Adapter    → 1SB → many insurers
                                                      ├── HDFC Adapter   → HDFC direct
                                                      ├── ICICI Adapter  → ICICI direct
                                                      └── Bajaj Adapter  → Bajaj direct
```

**Rule PR-03 — both models run simultaneously, permanently.** There is no big-bang migration from
*everything → 1SB* to *everything → direct*. The steady state is mixed:

```text
HDFC Life → Direct     ICICI Life → Direct     Bajaj Life → 1SB     Insurer X → 1SB
```

**And the platform does not care.** Any design that requires a coordinated cut-over has failed
`PR-03`, and Mahesh says so before the migration is planned rather than during it.

**Rule PR-04 — over time this becomes the bank's own aggregator.** Not named *Aggregator Service*,
which is ambiguous — the capability is **Insurance Aggregation & Provider Connectivity**
(`CAP-403`), whose job is stated once and precisely:

> Receive the bank's canonical insurance request, determine where it must go, fan out where
> necessary, translate it to provider-specific formats, manage provider communication, and return a
> canonical bank response.

---

## 3. `CAP-403` — the capability contract

```yaml
capability_contract:
  id: CAP-403
  name: "Insurance Aggregation & Provider Connectivity"
  plane: 4
  exists_because: >
    To decouple the bank's insurance domains from aggregators and insurers, so that provider
    topology (one aggregator, several direct insurers, or any mixture) is a routing decision rather
    than an architectural one. Without it, provider identity, protocol, credentials, execution
    model and error vocabulary leak into Quote, Proposal, Journey and Product.
  owns:
    - "Canonical provider contracts (IF-1)"
    - "Provider routing and routing policy"
    - "Multi-provider fan-out and result normalisation"
    - "Provider adapters and protocol transformation"
    - "Provider authentication, credentials, certificates and rotation"
    - "Timeout, retry, circuit breaker, bulkhead and rate limiting per provider"
    - "Provider callbacks and the provider ingress"
    - "Provider reference mapping"
    - "Idempotency toward providers"
    - "Error normalisation"
    - "Provider capability registry"
    - "Provider-specific observability"
  does_not_own:
    - "Customer journey state — CAP-106 / CAP-201"
    - "Suitability — CAP-202"
    - "Bank product selection — CAP-105"
    - "Customer consent — CAP-104"
    - "Business policy about which product should be recommended — Product/Rajal"
    - "Insurer underwriting decisions — the insurer (TI-03)"
  used_by: ["CAP-203 Quotation", "CAP-204 Proposal / Case Management", "CAP-301 Payment (provider leg)", "policy issuance interaction"]
  communicates_with:
    - {target: "1SB", style: async-poll, on_failure: "Poll to recover; never auto-retry a submit (OR-09)"}
    - {target: "Direct insurer APIs", style: sync | async-poll | callback, on_failure: "Per-provider breaker; partial fan-out success is success"}
  sharing: SHARED_CONTROL_PLANE_ISOLATED_RUNTIME
  sharing_rationale: >
    Canonical standards, provider registry, credential framework, security, observability and error
    standards must be identical everywhere or the abstraction is not one abstraction. Runtime is
    isolated per LOB cell so that Life quote volume cannot consume the capacity Health integration
    depends on.
  invariants: [TI-02, TI-04, TI-19, TI-20, TI-21, TI-22, "INV-ACL-01"]
  horizon: "H0 as a seam; H2+ as a multi-route aggregator"
  r0_mapping: "#14 Integration Hub + #15 1SB Adapter"
```

**Rule PR-05 — the `does_not_own` list is the load-bearing half.** Every item on it is something an
integration layer will be offered under delivery pressure, usually phrased as "the adapter already
knows this". An aggregation layer that acquires suitability, product selection or journey state has
become a second business platform with insurer credentials attached.

---

## 4. Two orchestrations that never merge

**`TI-20`.** There are now two kinds of orchestration, and they answer different questions:

| | **Business Journey Orchestration** | **Provider Aggregation Orchestration** |
|---|---|---|
| Answers | *What should happen next in the customer's journey?* | *Which provider gets this request, and how do we talk to it?* |
| Shape | Suitability → Quote → Proposal → Requirements → Payment → Issuance | Request → Provider Router → HDFC direct / ICICI direct / 1SB |
| Owner | `CAP-201` Journey Execution (per cell) | `CAP-403` |
| Knows about | Business stages, gates, customer state | Providers, protocols, credentials, retries |
| Must never know | A provider name, a protocol, a credential | A journey stage, a suitability verdict, a consent record |

**Rule PR-06 — never merge them.** The merged service ends up holding customer journey state **and**
provider routing **and** retry policy **and** canonical transformation **and** insurer credentials.
That is not a large service; it is the platform's single point of coupling, with the highest-value
secrets in it.

**Rule PR-07 — the direction of ignorance is asymmetric and deliberate.** The LOB service knows it
asked for a quote. It does not know whether that quote came from 1SB or from HDFC direct, and
nothing in its contract, its data model or its error handling lets it find out.

---

## 5. Canonical contracts

**Rule PR-08 — the LOB service constructs bank objects, never provider objects.** `Life Quote`
builds `BankLifeQuoteRequest`, never `1SBQuoteRequest`:

```text
Life Quote Service → BankLifeQuoteRequest → Aggregation Layer
                                                 ├─ today  → 1SB Adapter  → 1SB format
                                                 └─ future → HDFC Adapter → HDFC format

Response reverses:  provider response → adapter → Bank Canonical Quote
```

The Quote Service sees exactly the same contract in both cases. **This is the anti-corruption layer**
(`AP-04`, `INV-ACL-01`), stated as a contract rather than as a package rule.

### 5.1 No universal insurance object

**`TI-21` / Rule PR-09 — canonical is operation-scoped and LOB-scoped, never universal.**

The trap:

```text
InsuranceRequest {  every Life field · every Health field · every Motor field
                    every Quote field · every Proposal field  }
```

That object is unusable in practice: every consumer must know which of its two hundred fields apply,
every LOB change touches every LOB's payload, and no schema validation means anything.

The model instead:

```text
LifeQuoteRequest      LifeQuoteResponse       LifeProposalRequest    LifeProposalResponse
HealthQuoteRequest    HealthQuoteResponse     MotorQuoteRequest      MotorQuoteResponse
```

with genuinely common **primitives** shared across them:

```text
Party · Address · Nominee · Money · Coverage · DocumentReference · ProviderReference
```

> **Canonical does not mean one JSON that supports every insurance product in India. It means one
> stable bank-owned contract for a particular business capability.**

**Rule PR-10 — a primitive is shared only if it is semantically identical everywhere.** `Money` and
`Address` qualify. `Coverage` needs checking against Health and Motor before it is promoted — a
primitive that quietly grows LOB-conditional fields has become the god object one level down.

**Rule PR-11 — canonical contracts are versioned public contracts** with consumer-driven contract
tests (`FF-15`). They are the platform's most valuable asset under `PR-01`: they are what makes a
provider swap invisible.

---

## 6. The Provider Router

**Rule PR-12 — routing is a keyed lookup against effective configuration, not a conditional.**

```text
Request → Product / Provider Context → Routing Policy → Integration Route
```

The routing key is composite:

```text
LOB + Product + Provider + Operation + Channel(optional) + effective date
```

| LOB | Product | Insurer | Operation | Route |
|---|---|---|---|---|
| Life | Term | HDFC | Quote | `DIRECT_HDFC` |
| Life | Term | HDFC | Proposal | `DIRECT_HDFC` |
| Life | ULIP | HDFC | Quote | `1SB` |
| Life | Term | ICICI | Quote | `DIRECT_ICICI` |
| Health | X | Insurer A | Quote | `1SB` |

**Rule PR-13 — operation is part of the key; Quote and Proposal may route differently.** There are
real distribution arrangements where quoting goes through an aggregator while proposal submission
goes direct, or the reverse. A router keyed on `insurer == HDFC → direct` cannot express that, and
retrofitting the operation dimension later means re-testing every route.

**Rule PR-14 — this makes migration incremental at the finest useful grain.** Operation-by-operation
migration per product is available, which is what makes `PR-03` practical rather than theoretical:
move HDFC Term quoting to direct, leave HDFC Term proposals on 1SB, observe, then move the rest.

**Rule PR-15 — the caller cannot observe the route.** Not in the response shape, not in the error
codes, not in the identifiers, not in the latency contract. If a caller can tell, `PR-01` is already
compromised.

---

## 7. Product Governance owns the routes

**Rule PR-16 — routing configuration lives with the bank-approved product offering (`CAP-105`), not
inside the aggregation layer's code.** A Bank Product Offering carries:

```yaml
bank_product_offering:
  insurerId: "..."
  productId: "..."
  lob: LIFE | HEALTH | GENERAL
  quoteRoute:     DIRECT_HDFC | 1SB | ...
  proposalRoute:  DIRECT_HDFC | 1SB | ...
  issuanceRoute:  DIRECT_HDFC | 1SB | ...
  effectiveFrom: "..."
  effectiveTo: "..."
```

This is `TI-17` (the offering is configuration) meeting `PR-01` (the route is a property of the
offering). Two consequences worth stating:

1. **Changing where a product's quotes go is a governed configuration change**, with versioning,
   effective dates, audit, maker-checker where Shailja requires it, and rollback (`SC-14`).
2. **In-flight journeys keep the route they started under** (`SC-15`, `OR-22`). A journey that quoted
   through 1SB must not submit its proposal through a direct adapter mid-flight because a
   configuration became effective — the provider references would not resolve.

---

## 8. Fan-out: where the bank becomes a real aggregator

Today the fan-out happens inside 1SB. Later it happens inside the bank:

```text
Life Quote → Aggregation Layer → ┌─ HDFC direct  → Canonical Quote A ─┐
                                 ├─ ICICI direct → Canonical Quote B ─┼→ Quote Comparison Result
                                 └─ Bajaj direct → Canonical Quote C ─┘
```

**Rule PR-17 — partial success is success** (`OR-23`, `INV-QUO-02`). Three insurers answering and one
timing out is a result with three offers and one unavailable provider — not a failed quote request.
Whether a specific partial result is *presentable* is a business rule owned by Product, and the
architecture must be able to express either answer.

**Rule PR-18 — fan-out isolation is per provider and mandatory.** Each provider gets:

| Control | Why |
|---|---|
| Independent timeout | One slow insurer must not set the customer's wait |
| Circuit breaker | A failing insurer stops being asked |
| Concurrency bulkhead | One provider cannot consume the shared connection budget |
| Rate limiting | Respect provider quotas without dropping other providers |
| Credential isolation | A compromised provider credential has a bounded blast radius |
| Retry policy | Bounded, and never on a submit (`OR-09`) |
| Error mapping | Provider errors become bank errors before they leave the adapter |
| Telemetry | Per-provider latency, error and timeout rates (`SC-17`) |

**This is a primary justification for the aggregation layer's existence.** Without it, fan-out
isolation would have to be re-implemented inside every LOB service that talks to a provider.

**Rule PR-19 — ordering of results is never provider-driven.** Offer ordering is Product's decision
(`QR-07`: disclosed customer-relevant basis only, never commission or commercial arrangement). The
aggregation layer returns a set; it does not rank it.

---

## 9. Normalising provider execution models

Providers differ in *how* they respond, not just *what* they return:

```text
Sync      POST /quote → quote returned
Polling   POST /quote → jobId ; GET /quote/{jobId} → PROCESSING → COMPLETED
Callback  POST /quote → accepted ; later: webhook → bank
```

**Rule PR-20 — the caller sees one interaction contract regardless of provider execution model.**
The aggregation layer normalises sync, polling and callback providers behind a stable bank contract.

**Rule PR-21 — the platform's own asynchrony is a platform decision, not an inherited one.** 1SB's
poll model is a provider protocol detail confined to its adapter (`OR-25`). If the platform exposes
an async-poll contract to Quotation, that must be because *the platform* chose it — otherwise
onboarding a synchronous direct insurer arrives as a journey change instead of an adapter.

**Rule PR-22 — a provider's execution model is registry data, not code structure** (§11). Adding a
callback-style insurer must not require restructuring the adapter framework.

---

## 10. Provider references and identifiers

**`TI-22` / Rule PR-23 — a provider identifier never becomes a platform primary identifier.**

The mapping the aggregation layer owns:

```text
bankJourneyId · bankQuoteId · bankProposalId
        ↕
providerId · providerQuoteRef · providerProposalRef · providerTransactionRef · providerPolicyRef
```

**Never let `1SBJobId` become the primary identifier throughout the application.** This is the single
most common way `PR-01` is lost in practice — not through a design decision, but because a vendor
id was convenient in one table and then flowed outward through events, logs, APIs and reports.

**Rule PR-24 — provider references are opaque to business services.** A LOB service may *store* a
provider reference for support and reconciliation. It may not parse it, validate its format, sort by
it, or branch on it.

**Rule PR-25 — the reference map is durable and reconstructable.** It is what allows an in-flight
case to be recovered, a reconciliation break to be investigated, and a provider migration to be
audited. It survives adapter replacement.

---

## 11. Adapters as plugins, and the capability registry

**Rule PR-26 — adapters implement capability-scoped ports, not one universal interface.**

```text
QuoteProvider     → quote()
ProposalProvider  → submit() · status()
PolicyProvider    → issuanceStatus()
```

Capability-scoped ports mean a provider that supports quoting but not policy download implements one
port and not the other — which is honest, and which a single fat interface would force it to fake.

**Rule PR-27 — provider capability is configuration, never scattered conditionals.**

| Capability | HDFC | Provider B |
|---|---|---|
| quote | ✓ | ✓ |
| proposal | ✓ | ✓ |
| webhook | ✓ | ✕ |
| polling | — | ✓ |
| medical tracking | ✓ | ✕ |
| policy download | ✓ | ✕ |

The registry answers *can this provider do this operation* before the router selects it, and it is
what lets the platform degrade a capability for one insurer without a code path per insurer.

**Rule PR-28 — provider types stay inside `adapter.<provider>.*`** (`INV-ACL-01`, `FF-01`). The
existing rule already covers `adapter.onesb.*`; every new adapter inherits it, with its own ArchUnit
rule and `allowEmptyShould(false)`.

---

## 12. Provider authentication and credentials

```text
Today:  Bank → 1SB authentication
Future: HDFC → OAuth  ·  ICICI → mTLS + API key  ·  Bajaj → token exchange  ·  others → other
```

**Rule PR-29 — the LOB service never holds, sees or refreshes a provider credential.** Provider
adapters own provider authentication, through a common framework: Credential Manager · Token Manager
· Certificate Manager · Secrets Manager · KMS · rotation.

**Rule PR-30 — per-provider credential isolation is a blast-radius control** (`PR-18`), and it is
Deepali's review, not Mahesh's alone. One provider's credential compromise must not expose another's.

**Rule PR-31 — certificate and token lifecycle is designed, not discovered.** Expiry, rotation and
renewal failure each need a defined behaviour and an alert. An expired provider certificate that
first announces itself as a quote outage is an operability defect, not an incident.

---

## 13. Provider callback ingress

Direct integration introduces inbound traffic from insurers. **This is the largest new attack
surface the target state adds**, and it is why it gets its own controlled entry point.

```text
Insurer
   ↓
Provider Callback Gateway          ← one controlled ingress
   ↓
Authentication / mTLS / signature validation
   ↓
Provider Adapter
   ↓
Canonical Provider Event           ← e.g. ProposalRequirementUpdated
   ↓
LOB Service
```

**`TI-23` / Rule PR-32 — all provider callbacks enter through one controlled provider ingress.** Do
not expose `/life/hdfc/callback`, `/life/icici/callback`, `/health/x/callback` into business services.

**Rule PR-33 — the LOB service receives a bank event, never a provider payload.** `Raw HDFC callback
→ HDFC Adapter → ProposalRequirementUpdated`. Life Proposal consumes the canonical event.

**Rule PR-34 — inbound callbacks carry the same guarantees as any untrusted input:** authenticated
(mTLS and/or signature), replay-protected, deduplicated, at-least-once tolerated, idempotent on the
consumer, rate-limited, and never trusted for attribution (`TI-10`). The existing payment-callback
seam (`S-14`) is the pattern; insurer callbacks extend it rather than inventing a second one.

**Rule PR-35 — a missing callback is resolved by polling or reconciliation, never by assumption**
(`OR-10`). Every callback-dependent flow declares its fallback.

---

## 14. Control plane and data plane

`VIN-002 §17` resolves the topology question left open at
[`09 §5.1`](./09-target-state-architecture-doctrine.md), and this is the answer Mahesh now carries:

```text
                    SHARED — Integration Control Plane
        canonical standards · provider registry · credential framework
        security · observability · error standards · routing policy
                                    │
        ┌───────────────────────────┼───────────────────────────┐
   LIFE CELL                   HEALTH CELL                 GENERAL CELL
   Life Quote                  Health Quote                GI Quote
   Life Proposal               Health Proposal             GI Proposal
   Life Aggregation Runtime    Health Aggregation Runtime  GI Aggregation Runtime
   Adapters                    Adapters                    Adapters
```

**Rule PR-36 — share the control plane, isolate the data plane.** The standards, registry, credential
framework, security model, observability contract and error taxonomy are **one** thing, versioned
once. The runtime that executes provider calls is **per cell**, so Life quote volume cannot consume
the capacity Health integration depends on.

This is `LS-04` at the integration layer: cell isolation moves risk onto shared components unless the
shared component is a *control plane* rather than a *shared runtime*.

**Rule PR-37 — a per-cell runtime is not a forked framework** (`SC-05`). Three runtimes running the
same versioned framework is isolation. Three frameworks is three platforms.

**Rule PR-38 — the split is evidence-gated, and `SC-W3-5` governs until it is taken.** At H0–H1 there
is one Integration Hub and one cell; a per-cell runtime split requires the ADR named at `09 §5.1`,
with Deepali on credential isolation and Shivanshi on isolation evidence.

---

## 15. R0 posture — design the seam, not the aggregator

**Rule PR-39 — do not overbuild the future aggregator.** `AP-09` and `HR-03` apply with full force:
routing tables, capability registries, fan-out engines and callback gateways are H2+ mechanisms.
Building them now costs delivery time and adds failure modes for a platform with one provider.

R0 is practically this:

```text
Life Quote → Provider Gateway → Provider Router → 1SB Adapter → 1SB
```

with the router holding exactly one route. **The whole value is the boundary, not the machinery.**

### 15.1 What R0 must get right, because it is expensive later

| # | H0 obligation | Why it cannot wait |
|---|---|---|
| 1 | Quotation and Proposal construct **bank canonical** requests only | Retrofitting a canonical model after two LOBs exist means re-testing every journey |
| 2 | **No 1SB type, enum, status or path outside `adapter.onesb.*`** | `INV-ACL-01` / `FF-01` already enforce it — keep it green |
| 3 | **No provider identifier is a platform primary identifier** (`TI-22`) | Vendor ids propagate through events, logs, APIs and reports; extraction later is a data migration |
| 4 | A route is **looked up**, even with one route | A conditional becomes a routing table; a hard-coded call becomes a rewrite |
| 5 | Provider credentials sit **only** in the adapter | Credential sprawl is a security finding, not a refactor |
| 6 | The platform's async-poll contract is a **platform choice**, documented as such | Otherwise onboarding a sync insurer is a journey change (`PR-21`) |
| 7 | Provider errors are **normalised at the adapter** | Vendor error codes in business logic are the hardest leak to find |

**Rule PR-40 — the acceptance test for R0's integration boundary.** *Could a second provider be added
without changing Quotation, Proposal, Journey, Customer, Lead, Payment or Policy?* If the honest
answer is no, `PR-01` is not yet true, and that is a finding worth raising while it is still cheap.

---

## 16. Proving it, rather than asserting it

`TI-18` applies here too: an abstraction claimed but not tested is an abstraction that has already
drifted. `S07-VT-08` already poses the question — *replace 1SB with another aggregator; is the change
confined to the adapter layer?* — and these make it answerable.

| # | Check | Mechanism | From |
|---|---|---|---|
| 1 | Provider types confined to `adapter.<provider>.*` | ArchUnit per adapter, `allowEmptyShould(false)` | `FF-01`, today |
| 2 | No provider identifier in a platform primary-key or public-contract position | Schema and contract assertion over canonical models | **new, proposed** |
| 3 | No provider name, path or enum outside the adapter package | Source scan over `application.*` and `domain.*` | **new, proposed** |
| 4 | Canonical contract compatibility at every seam | Consumer-driven contract tests | `FF-15`, today |
| 5 | A stub second provider routes end to end without touching a business service | Integration test with a fake adapter | **new, proposed** — the direct test of `PR-40` |
| 6 | Per-provider isolation holds under a forced provider failure | Fault-injection exercise | `LS-03`, Shivanshi |
| 7 | Callback ingress rejects unauthenticated, replayed and malformed callbacks | Negative security tests | **new, proposed** — Deepali |

**Rule PR-41 — check 5 is the one that matters.** Everything else is a proxy. A fake provider that
routes end to end without a business-service change is `PR-01` demonstrated rather than believed —
and it is buildable at H0, when there is exactly one real provider to compare against.

---

## 17. Reconciliation of `VIN-002` against accepted decisions

| `VIN-002` | Claim | Repository position | Verdict |
|---|---|---|---|
| Framing, §20 | **1SB is a provider route, not a domain dependency** | `INV-ACL-01`, `FF-01`, `ARCH-006`, `replaceable-middleware.md`, `S07-VT-08` all already imply it | **Agrees** — promoted to `TI-19`, the permanent principle asked for |
| §1, §3 | Bank aggregation layer between LOB services and providers | `#14 Integration Hub` + `#15 1SB Adapter`, `SC-W3-5` | **Agrees** — `CAP-403` names and scopes what already exists |
| §1 | Mixed routing runs permanently; no big-bang migration | `08-integration-strategy.md` Phases A–D read as sequential phases | **Refines** — phases describe posture, not a cut-over; `PR-03` makes coexistence the steady state |
| §3 | Journey orchestration and provider orchestration never merge | `SC-W3-6`, `INV-JRN-02`, `FF-04` restrict Journey to stage + references | **Agrees** — promoted to `TI-20`; `FF-04` already tests half of it |
| §4 | LOB services build bank canonical requests only | `AP-04`, `INV-ACL-01` | **Agrees** |
| §5 | Canonical is operation + LOB scoped, never one universal object | Canonical model package exists; no anti-god-object rule recorded | **Extends** → `TI-21`, `PR-09`/`PR-10` |
| §6, §8 | Router keyed on LOB + Product + Provider + **Operation** + Channel + effective date | `#14` owns `RoutingPolicy` per LOB/product | **Extends** — adds Operation and Channel to the key; enables operation-level migration |
| §7 | Product Governance owns `quoteRoute` / `proposalRoute` / `issuanceRoute` | `CAP-105`, `ARCH-010`, `TI-17` | **Extends** — routing becomes a property of the bank product offering |
| §9 | Bank-side multi-provider fan-out | Multi-quote exists via 1SB; bank-side fan-out not recorded | **Extends** — H2+ capability |
| §10 | Per-provider timeout, breaker, bulkhead, rate limit, credential isolation, error mapping, telemetry | Per-provider bulkheads are an architecture property (`§5.3`) | **Agrees / extends** — adds rate limiting and credential isolation explicitly |
| §11 | Normalise sync / polling / callback provider execution models | R0 assumes the 1SB async-poll model throughout | **Extends** — and flags `PR-21` as an H0 obligation |
| §12 | Provider references never become platform primary identifiers | Not previously stated as a rule | **Extends** → `TI-22`; proposed fitness check 2 |
| §13 | Provider auth framework: credential, token, certificate, KMS, rotation | Secrets Manager + KMS + per-service credentials exist | **Extends** — per-provider isolation and rotation design |
| §14 | One controlled provider callback ingress | `S-14` covers the payment-gateway callback only | **Extends** → `TI-23`; **Deepali review required** |
| §15 | Adapters as plugins on capability-scoped ports | Hexagonal ports/adapters already the pattern (`§6`) | **Agrees** |
| §16 | Provider capability registry as configuration | Not recorded | **Extends** → `PR-27` |
| §17 | Shared integration **control plane**, isolated per-cell **data plane** | `SC-W3-5` — all provider traffic through the Integration Hub | **Resolves the `09 §5.1` tension.** The control-plane/data-plane split preserves the control while removing the bottleneck. `SC-W3-5` still governs until the ADR is taken |
| §18 | Design the seam at R0; do not build the aggregator | `AP-09`, `HR-03`, R0 build order | **Agrees** — same conclusion, independently reached |
| §19 | New capability with an explicit `does NOT own` list | No such capability recorded | **Extends** → `CAP-403` |

### 17.1 Open items this reconciliation creates

Added to the register in [`10 §9.1`](./10-north-star-capability-model.md):

| # | Item | Owner | Type |
|---|---|---|---|
| 8 | Routing key gains Operation and Channel; `#14`'s `RoutingPolicy` definition updated | Mahesh | ADR |
| 9 | `quoteRoute` / `proposalRoute` / `issuanceRoute` added to the bank product offering model | Mahesh + Rajal + Aarti | Model change |
| 10 | Provider callback ingress: authentication, signature, replay protection, rate limiting | **Deepali** | Security design, before any direct integration |
| 11 | Per-provider credential isolation and rotation design | **Deepali** | Security design |
| 12 | Three proposed fitness functions (no provider id in primary/public position · no provider name outside adapter · fake-provider end-to-end route) | Mahesh + Swapnali | Fitness functions, buildable at H0 |
| 13 | Whether a partial fan-out result is presentable to a customer, and on what basis | **Rajal** + Shailja | Product/compliance rule |
| 14 | Confirmation that the platform's async-poll contract is a platform choice, not inherited from 1SB | Mahesh | H0 design constraint |

---

## 18. Anti-patterns

| Anti-pattern | Consequence | Correct move |
|---|---|---|
| Designing the platform around 1SB's contract | The aggregator becomes the domain model | `PR-02` |
| `1SBJobId` as a platform identifier | Removing 1SB becomes a data migration across events, logs and reports | `TI-22`, `PR-23` |
| Merging journey and provider orchestration | One service holds journey state, routing, retries and insurer credentials | `TI-20`, `PR-06` |
| One universal `InsuranceRequest` | Unusable schema; every LOB change touches every LOB | `TI-21`, `PR-09` |
| Routing on `insurer == HDFC → direct` | Cannot express operation-level routing; blocks incremental migration | `PR-13` |
| Routing rules compiled into the aggregation layer | A distribution change becomes a release | `PR-16` |
| Provider callbacks wired straight into business services | Untrusted insurer input reaching domain logic | `TI-23`, `PR-32` |
| Provider credentials outside the adapter | Credential sprawl; no blast-radius control | `PR-29` |
| Failing the whole fan-out on one provider timeout | Customer sees nothing when three insurers answered | `PR-17` |
| Shared provider connection pool across LOBs | Life volume starves Health integration | `PR-36` |
| Forking the integration framework per cell | Three platforms wearing one name | `PR-37` |
| Building the router, registry and fan-out engine at R0 | Machinery with one provider to route to | `PR-39` |
| Exposing the route in responses, errors or latency | The caller starts depending on it | `PR-15` |
| Ranking fan-out results in the aggregation layer | Ordering is a regulated Product decision | `PR-19`, `QR-07` |
| Asserting replaceability without testing it | Drift discovered during the migration | `PR-41` |

---

## 19. Authority

| Decision | Authority |
|---|---|
| Canonical contract design, adapter ports, error normalisation, reference mapping | `A1_AUTONOMOUS` — Mahesh, ADR when durable |
| Routing key structure and routing policy model | `A2_NOTIFY` + ADR |
| Per-cell aggregation runtime split | `A2_NOTIFY` + ADR amending `SC-W3-5`; Deepali + Shivanshi |
| Provider callback ingress, provider authentication, credential isolation, rotation | `A3_JOINT_REVIEW` — **Deepali / Security Board** |
| Onboarding a direct insurer: data crossings, contracts, regulated outsourcing | `A3_JOINT_REVIEW` — **Shailja** |
| Route configuration for a bank product offering | **Product / Bancassurance**, governed under `SC-14`/`SC-16` |
| Whether a partial fan-out result is presentable | **Rajal** + Shailja |
| Strategic commitment to displace 1SB | `A4_HUMAN_REQUIRED` — irreversible vendor decision beyond delegated authority |
