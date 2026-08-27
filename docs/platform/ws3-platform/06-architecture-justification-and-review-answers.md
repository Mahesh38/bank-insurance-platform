# WS-3 — Architecture Justification & Review Answers

**Persona:** Mahesh — Principal Insurance Platform Architect (Board 1)  
**Audience:** Architecture / Product / Engineering / Data / Security reviewers preparing to defend the platform shape  
**Status:** AI-DRAFTED · mandatory human Architecture signature outstanding

**Revision 2026-08-25 — alignment round** (`SUG-20260818-4c3`): this pack was drafted on
2026-08-18 and is reconciled here against everything ratified since. Three classes of change:

1. **Renderings.** The pack originally carried two of its own SVGs. `HA-04` ("one diagram, one
   horizon") assigns both horizons to files that already exist and are current, so this document
   cites them instead of shipping a second answer for each — see the companion list below and
   [`16-hld-authoring-and-update-protocol.md §2–§3`](../../context/roles/mahesh-principal-insurance-platform-architect/16-hld-authoring-and-update-protocol.md).
2. **The five layers admitted by [`CR-012`](../../governance/change-requests/CR-012-r0-platform-robustness.md) / `ADR-009`…`ADR-013`.** §3, §5, §6, §7 and §9
   are rewritten: R0 now has a managed cache tier, an event backbone, centralised egress
   inspection, hybrid bank connectivity and an operational search pipe. The caching answer in
   particular is no longer "no cache in R0" — it is "a cache tier exists, and here is what it is
   forbidden to hold".
3. **Facts that moved under the pack.** `#5` is **Opportunity**, the single origination point in
   W1, not a thin deferred Lead (`ADR-005`, `AC-8`, `AC-9`); `#19` Configuration is a **W0b
   service**, not versioned artefacts (`CF-5`); the count is **fourteen services plus one app**;
   the actor model is **two actors** with the IPR assisting; persistence is **one Aurora cluster,
   schema per context** (`ADR-008`).

**Companion artefacts:**
- Normative R0 solution: [`03-solution-architecture-r0.md`](./03-solution-architecture-r0.md) — **authoritative; if this document disagrees with it, it wins (`HA-02`)**  
- Visual HLD (R0 slice): [`../../architecture/r0-reference-architecture.svg`](../../architecture/r0-reference-architecture.svg) — the H0 rendering  
- Visual HLD (full platform · release-coded `R0`…`RN`): [`../../hdl.svg`](../../hdl.svg) — the North Star rendering  
- Compiled R0 narrative and S09 AWS pack: [`../../architecture/R0-HLD.md`](../../architecture/R0-HLD.md) · [`../../architecture/R0-LLD.md`](../../architecture/R0-LLD.md)  
- Decision log (`ARCH-*`, `ADR-001`…`ADR-013`): [`../architecture-review/08-architecture-decision-log.md`](../architecture-review/08-architecture-decision-log.md)  
- Target catalogue: [`../architecture-review/02-target-microservices-architecture.md`](../architecture-review/02-target-microservices-architecture.md)  
- Data engines: [`../architecture-review/05-data-architecture.md`](../architecture-review/05-data-architecture.md)  
- Integration roadmap: [`../../au-bank-insurance-platform/knowledge-base/08-integration-strategy.md`](../../au-bank-insurance-platform/knowledge-base/08-integration-strategy.md)  
- Product charter: [`../../governance/workstreams/WS-3-PLATFORM-CHARTER.md`](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md)

> **How to use this in a review.** Start with §1 (what we are building toward). Use §2 when asked
> "why so many services?". Use §3 for the per-service catalogue (functions / APIs / logic). Use §4
> for "why not merge X and Y?". Use §5–§7 for datastore, caching and direct-insurer questions.
> Use §8 and [`hdl.svg`](../../hdl.svg) when someone says "this only shows R0".
>
> **This document explains and defends; it decides nothing.** Every number, name, release and
> engine below is read from an authoritative source and cited. Where you find a disagreement, the
> source is right and this file is the defect (`HA-02`).

---

## 1. What we are achieving with this architecture

### 1.1 The business problem, restated as an architecture obligation

AU Bank holds IRDAI Composite Corporate Agent licence CA0515. Today, much of the sale leaves the
bank's visibility at redirect. The platform exists so the bank **retains control and evidence across
the whole sale**: lead → need analysis → suitability → consent → quote → proposal → payment on the
customer's device → issued & reconciled policy → audit trail.

That obligation forces architecture properties that a single "insurance microservice" cannot
satisfy:

| Obligation | Architecture property it forces |
|---|---|
| Suitability is a hard gate before quote | A Suitability context whose decision Quotation must verify — not a UI checkbox |
| Consent is evidenced, OTP-verified, immutable | A Consent context with append-only storage and its own retention/CMK posture |
| Payment never runs on an RM device | Payment owns link issuance; Notification delivers to the customer; no API path can issue a payment session into an RM session |
| "Sold" = issued + reconciled + auditable | Policy issues only against `RECONCILED` payment; Journey cannot reach `SOLD` until Audit confirms |
| 1SB is replaceable | Quotation/Proposal/Payment/Policy never call 1SB; they call the Integration Hub in bank-canonical language |
| Attribution is bank-owned | Hub injects `distributorId`; caller-supplied values are rejected |
| Only a certified Specified Person may sell | Certification is an attribute on the RM principal evaluated per regulated action, not a role checked at login (`AC-1`, INV-ACT-01) — and an assisting Insurance Partner Rep is structurally incapable of originating (INV-ACT-02) |
| Health and General follow Life on the same platform | `lob` is mandatory and non-null on every entity, configuration record, audit event and authorization request from release 1 (`LB-1`…`LB-5`). Retrofitting it is a migration across every table on the sale path |
| A compliance rule change must not need a deployment | Configuration (`#19`) is a **service** built in W0b, before anything that reads it (`CF-1`…`CF-5`) |

If we collapse these into fewer deployables without preserving the **boundaries**, we keep the
microservice count down and lose the controls. The control is the product.

### 1.2 Three nested targets (do not confuse them)

| Horizon | What "done" means | Service count |
|---|---|---|
| **R0 (assisted Term slice)** | One RM sells one Term Life policy to one ETB customer from one Group A insurer, end to end, evidenced | **Fourteen deployable services plus one app** (+ WS-1 + WS-2 consumed) — the list is [`03-solution-architecture-r0.md §3`](./03-solution-architecture-r0.md), not this table |
| **R1 (scale the proven journey)** | DIY + Customer BFF, Group B redirect, ULIP/Savings, richer notification/MIS/admin | Adds ~4–5 contexts and deepens existing ones |
| **Target platform (R2+)** | Full 19-context catalogue: renewals/servicing, NTB, non-life (when WS-1 Phase 5 unfreezes), direct insurer adapters, control tower | Full catalogue in [`02-target-microservices-architecture.md`](../architecture-review/02-target-microservices-architecture.md) |

R0 is a **release slice**, not a permanent architecture.
[`r0-reference-architecture.svg`](../../architecture/r0-reference-architecture.svg) is the R0 cut,
coloured by **build wave**; [`hdl.svg`](../../hdl.svg) is the same ten boundaries and the same
context numbers coloured by **release**, so a reviewer can put the two side by side and read the
cut rather than two designs (`HA-02b`). Neither is a licence to build R1+ now.

**R0 is thin in services and no longer thin underneath them.** [`CR-012`](../../governance/change-requests/CR-012-r0-platform-robustness.md)
admitted five infrastructure layers — hybrid bank connectivity (`ADR-009`), centralised egress
inspection (`ADR-010`), a managed cache tier (`ADR-011`), an event backbone (`ADR-012`) and an
operational search pipe (`ADR-013`) — as **infrastructure beneath an unchanged R0 slice**. No
bounded context, no service, no journey step, no gate criterion and no actor changed. When a
reviewer asks why a fourteen-service pilot needs three stateful managed tiers, the answer is §6.4:
four of the five are decisions that get materially more expensive after W0b and W4, not capacity
we need at ~100 journey starts per hour.

### 1.3 What success looks like architecturally

1. **Replaceability** — swap 1SB for a direct insurer without rewriting Quotation or Proposal.  
2. **Non-bypassable controls** — C1 (suitability), C2 (consent), C4 (customer-device payment) are
   enforced in services, not only in screens.  
3. **Evidence** — every regulated decision leaves an immutable audit trail.  
4. **Independent change** — a Payment reconciliation fix does not require redeploying Suitability.  
5. **Honest sequencing** — foundation (S08/S09) before Wave 1; Wave 1 before money path; money path
   before claiming "platform ready".

---

## 2. Why we need these services (the design rule)

### 2.1 The rule we actually use

> **One bounded context → one write model → one owning service.**  
> Split further only when lifecycle, scaling profile, team or release cadence genuinely diverge.  
> Merge only when two contexts share one lifecycle, one failure domain and one change cadence —
> **and** merging does not weaken a regulatory hard gate.

Source: [`02-target-microservices-architecture.md`](../architecture-review/02-target-microservices-architecture.md) § Design rule.

### 2.2 What a "service" is allowed to own

| Owns | Does not own |
|---|---|
| One canonical aggregate (or a tightly coupled pair under one SoR) | Another context's business decision |
| Its own schema, credential and migration history — and at R0 that schema sits in the **shared Aurora cluster** (`ADR-008`) | Any other context's tables. Ownership is the invariant; cluster topology is a decision |
| Bank-canonical API | Raw 1SB / insurer wire format |
| Enforcement of its invariants | Journey stage of the whole sale |
| Its own cache key prefix and ACL user, if it uses the tier (`ADR-011`) | Anything durable in the cache — no evidence, no idempotency, no system of record |

Journey Orchestration is the deliberate exception: it owns **stage + references**, never the
authoritative quote/proposal/payment/policy decision (SC-W3-6 / INV-JRN-02).

### 2.3 Why "fewer services" is not automatically better here

Bancassurance failure modes are **cross-context**: payment authorised but issuance delayed;
suitability expired between quote and proposal; consent revoked; PG callback missing;
insurer returns `PARTIAL` quotes. Those failures need:

- clear ownership of each decision,
- compensating actions owned by Journey,
- independent deployability of the money path vs the advisory path,
- independent write models per context (Consent must stay append-only even if Quotation is
  rebuilt) — which is a schema-and-credential property under `ADR-008`, not a cluster count.

A merged "Sales Service" that does suitability + consent + quote + proposal looks tidy on a slide
and fails the first compliance reconstruction: *"show me the immutable consent grant that
authorised this proposal, separate from the quote job that may have been retried three times."*

---

## 3. Service catalogue — functions, APIs, business logic

Legend for **Release**: solid R0 · deepen in R1 · R2+ only. The **Wave** column is the R0 build
order from [`03-solution-architecture-r0.md §3`](./03-solution-architecture-r0.md) — `W0b` first,
because every wave beneath it resolves its rules from it.  
API verbs are **platform-canonical** (not OpenAPI-final). Exact paths land with each service's
contract pack; this table is the review-facing inventory.

Names and numbers follow `NC-1`: the `#n` is the identity and the name is the canonical register
name. Note in particular that **`#5` is Opportunity**, not Lead — `OPEN-D10` closed that rename on
2026-08-20 (`ADR-005`). The `leadId` token and the `INV-LED-*` invariants keep their names because
an identifier is opaque and rewriting them breaks every citation to buy nothing.

### 3.0 Configuration — the service that is built first

| Service | Release | Wave | Functions / business logic | Representative APIs | Why separate |
|---|---|---|---|---|---|
| **#19 Configuration** (Administration & Config, backend) | R0 | **W0b** | Rules, journey step definitions, field validations, document checklists, product eligibility and role permissions, versioned and effective-dated, resolved by `(lob, …)` | `GET /config/{domain}?lob=&version=`, publish/version APIs | Every wave below reads from it. Built after its readers is how hardcoded branches get written and never removed (`CF-5`). **The earlier "versioned artefacts consumed at startup" trade is withdrawn** — it made the deployment pipeline the rule-change mechanism. **No admin UI in R0**, and its absence is deliberate rather than blocking |

### 3.1 Edge & identity

| Service | Release | Wave | Functions / business logic | Representative APIs | Why separate |
|---|---|---|---|---|---|
| **#2 NIP BFF** | R0 | W4 | Token-hiding session for NIP-APP; aggregates every workforce role's screens (RM, IPR, admin/ops); never holds domain decisions. Session state lives in the shared cache tier (`ADR-011`), not in the pod | `POST /session/login`, `GET /workspace/journey/{id}`, mutation proxies with `Idempotency-Key` | Flutter must not see OAuth tokens (ARCH-019); BFF is the only public app-facing surface. **One BFF per channel, never per channel × LOB and never per role** (`ADR-015`) |
| **#1 Customer BFF** | R1 | — | Same pattern for DIY customer app | Customer session + journey proxies | Different principal, different threat model, different app — do not overload NIP BFF |
| **#3 Identity & Access (WS-2)** | R0 (workforce) / R1+ (retail) | consumed | Provider adapter + PDP (RBAC/ABAC); **Specified Person certification** — certificate number, LOB scope, validity window, status — surfaced as principal attributes and evaluated per regulated action (`ARCH-022`, INV-ACT-01) | `POST /auth/token-exchange` (adapter-internal); `POST /authorize` (PDP) | Fail-closed authz; IdP is never business SoR. Certification is an **attribute**, not an actor row and not a channel (`AC-1`, `AC-2`) |

**Two actors, and only one of them sells** ([`03-solution-architecture-r0.md §2.1`](./03-solution-architecture-r0.md)).
The **Bank RM** is the certified Specified Person and holds the sole origination right. The
**Insurance Partner Representative** is an insurer employee who assists — no origination, no
regulated action, own-insurer product view, gated read (INV-ACT-02, `S-22`). The **customer is not
an on-platform actor in R0**: their device receives a consent OTP and a payment link and reaches no
platform service. A reviewer who reads "two actors" as "two apps" has the wrong model — the IPR
works inside the RM's journey under a different capability set, which is why it is an authorization
question and not a second BFF.

### 3.2 Sales & advisory

| Service | Release | Wave | Functions / business logic | Representative APIs | Why separate |
|---|---|---|---|---|---|
| **#5 Opportunity** | **R0 — full origination** | **W1** | **The single origination point (`AC-8`).** Creates the demand object that carries the accountable SP, the `lob` and the origination record; nothing downstream may exist without one | `POST /opportunities`, `PATCH /opportunities/{id}`, `POST /opportunities/{id}/assign` | Un-deferred from S13 to reconcile with `CURRENT-STATE.yaml` `in_scope` (`AC-9`). Beginning a journey from a customer lookup creates a **second way into the funnel**, which is precisely what `AC-8` forbids. What stays deferred to S13 is campaign and bulk sales-management breadth *on top of* origination — not origination itself |
| **#4 Customer** | R0 | W1 | ETB CIF lookup via bank EBS APIs, profile snapshot, prefill; never writes CBS directly. Reached over the Transit Gateway (`ADR-009`) — `uat` and `prod` may not stub it | `GET /customers/by-cif`, `GET /customers/by-mobile`, `GET /customers/{id}/snapshot` | EBS facade + PII snapshot ownership; freshness rules differ from Opportunity |
| **#6 Consent** | R0 | W2 | Versioned grants; customer-device OTP evidence; append-only | `POST /consents`, `POST /consents/{id}/otp/verify`, `GET /consents/{id}` | Regulatory evidence store; different retention/CMK; must fail closed for proposal |
| **#7 Suitability** | R0 | W2 | Need analysis; product eligibility; assessment id with TTL; PDF evidence | `POST /suitability/assessments`, `GET /suitability/assessments/{id}`, `GET …/eligible-products` | Hard gate C1 — must be independently enforceable and auditable |
| **#8 Product Catalogue** | R0 Term-matrix / R1 ULIP+Group B | W1 | Products, insurers, eligibility matrix, documents | `GET /products`, `GET /insurers`, `GET /eligibility` | Low-write / high-read — the one place an L2 read-through cache earns its keep (`ADR-011`, §6). Admin-owned content later |
| **#9 Journey Orchestration** | R0 | W1 | R0 state machine; saga / compensation; holds refs only. Step definitions and transitions resolved from `#19` by `(lob, journeyType, version)` — not compiled in | `POST /journeys`, `POST /journeys/{id}/advance`, `GET /journeys/{id}` | Without this, every BFF reinvents the sale spine (ARCH-005). The Journey Registry / LOB Router split is R1 |
| **#10 Quotation** | R0 | W2 | Create quote, poll offers, select offer, enforce suitability gate | `POST /quotes`, `GET /quotes/{id}`, `POST /quotes/{id}/select` | Bursty fan-out / poll workload; short-lived jobs — different lifecycle from Proposal. **LOB-owned execution**: a second LOB gets its own instance (`LB-R1`) |
| **#11 Proposal & UW** | R0 thin / R1 ACR depth | W3 | Prefill, submit, UW status, requirements tracking | `POST /proposals`, `GET /proposals/{id}`, `POST /proposals/{id}/submit` | Days/weeks-long case file; documents; relational integrity. **LOB-owned execution** alongside `#10` |

**Which boxes get a second copy when Health arrives** is the question this table is most often asked,
and `LB-R1` answers it in three classes, never two: `#10` and `#11` are **LOB-owned execution** and
are duplicated per cell; `#2`, `#3` PDP grants, `#6`, `#7`, `#8`, `#9`, `#14` and `#19` are
**LOB-partitioned shared** — one codebase, one deployment, behaviour resolved from configuration
keyed by `(lob, …)`; `#4`, `#5`, `#12`, `#13`, `#16` and `#17` are **LOB-agnostic shared** and are
never duplicated. Duplicating that last class is the failure mode the shared plane exists to
prevent (`LB-5`).

### 3.3 Fulfilment

| Service | Release | Wave | Functions / business logic | Representative APIs | Why separate |
|---|---|---|---|---|---|
| **#12 Payment** | R0 | W3 | Session create (customer device), callback ingest, reconciliation, `UNCERTAIN` handling | `POST /payments`, `POST /payments/callbacks/pg`, `POST /payments/reconcile` | Money path + RBI device isolation; dedicated CMK; no degraded mode |
| **#13 Policy & Issuance** | R0 visibility / R1 servicing views | W3 | Confirm issuance, store policy refs/PDFs, dispute states | `GET /policies/{id}`, `POST /policies/{id}/confirm`, document URLs | "Sold" definition lives here; must not issue against unreconciled payment |

### 3.4 Integration (supplier)

| Service | Release | Wave | Functions / business logic | Representative APIs | Why separate |
|---|---|---|---|---|---|
| **#14 Integration Hub** | R0 | **W1** | Routing policy; inject attribution; per-provider bulkhead; canonical↔adapter handoff | Internal: `POST /provider/quotes`, `…/proposals`, `…/status` | Adding a provider must not touch Quotation/Proposal code (SC-W3-5). Built in W1 — *before* four services could otherwise depend on the adapter directly |
| **#15 1SB Adapter (WS-1)** | R0 (exists) | consumed | 1SB wire protocol, job store (`onesb` schema in the shared cluster), raw payload archive | Existing integration APIs behind Hub | Provider vocabulary terminates in `adapter.onesb.*` (INV-ACL-01). Its outbound mTLS session is **passed intact** through the inspection VPC rather than decrypted (`ADR-010`) |
| **Direct Insurer Adapter(s)** | S13 / Phase B–C | — | Per-insurer ACL; own caching where the insurer has no middleware cache | Same Hub contract as 1SB | Proves replaceability; never called by domain services. Deferred because it is not on the critical path to one proven journey |

### 3.5 Cross-cutting

| Service | Release | Wave | Functions / business logic | Representative APIs | Why separate |
|---|---|---|---|---|---|
| **#16 Audit & Compliance** | R0 | W3 | Append-only evidence; reconstruct consent/suitability/attribution per txn. Fed by the **transactional outbox**, which stays the source of truth even now that a broker exists (`ADR-012`) | `POST /audit-events` (ingest), `GET /audit-events?journeyId=` | Library shapes events; service stores and queries them immutably. `UPDATE`/`DELETE` rejected (`FF-10`); 7-year WORM |
| **#17 Notification** | R0 OTP+pay-link / R1 breadth | W4 | SMS/email delivery; never blocks journey | Consumes topics; delivery log API | Failure domain isolated from money/advisory path |
| **#18 Reporting & MIS** | S13 / R1 full MIS | — | Funnel metrics, sold KPI, later DWH | Read models / exports — never sync on sale path | Analytics must not back-pressure a customer journey. Deferred: the pilot funnel is read from the audit store, not from a warehouse. `ADR-013` provisions **operational search**, which is a different thing |
| **#19 Configuration** (Administration & Config) | R0 **service** / R1 admin UI | **W0b** | See §3.0 — it is built first, not last | Config resolution + publish APIs; UI is the R1 Admin & Configuration BFF | Config-first absorbs compliance answers without re-architecture (ARCH-010) |

### 3.6 The platform tier beneath all of it — `ADR-009` … `ADR-013`

These are not services and own no bounded context. They are named here because a reviewer counting
boxes on the R0 rendering will find five things that were absent from the 2026-08-18 draft of this
pack, and the honest answer is that `CR-012` admitted them deliberately.

| Layer | What R0 provisions | The line it may not cross |
|---|---|---|
| **Bank connectivity** (`ADR-009`) | Transit Gateway in a new `network` account connecting to bank **EBS (Enterprise Service Bus) APIs for CBS / CIF** and Bank AD; Site-to-Site VPN from day one, Direct Connect primary when the circuit lands | `dev` may stub EBS/CBS and Bank AD; **`uat` and `prod` may not**. A journey evidenced against a stub is not evidence |
| **Perimeter & Edge Ingress** | **Cloudflare (Enterprise CDN/DDoS)** → **F5 BIG-IP / WAF (Bank Policy)** → **External ALB** → **Amazon API Gateway** (Proxy 1 of 2) → **Internal ALB** (Proxy 2 of 2) | Bank enterprise security standard. Edge TLS termination, L7 policy and rate limiting; no business logic |
| **Delivery & IaC** | **GitLab CI/CD** for multi-stage pipelines and **Terraform** for Infrastructure as Code (IaC) across environments | Standard bank pipeline and multi-environment provisioning baseline |
| **Governance & Logs** | **AWS CloudTrail** (account management and security audit trail) alongside **Amazon CloudWatch** (operational logs/metrics) | CloudTrail satisfies RBI management auditability; CloudWatch provides runtime telemetry |
| **Egress inspection** (`ADR-010`) | Inspection VPC per environment, AWS Network Firewall, domain allowlist, drop-by-default; the allowlisted Elastic IPs move behind it | It is **not a mesh** and does not replace `NetworkPolicy`. The 1SB mTLS session is passed intact, not decrypted |
| **Cache tier** (`ADR-011`) | One ElastiCache for Valkey replication group per environment: BFF sessions, an L2 read-through behind the in-process L1, per-principal rate-limit counters. Per-service ACL user and key prefix | **Never** idempotency, never a system of record, never a way to serve configuration past TTL. A miss is a read, never an error (`S-25`) |
| **Event backbone** (`ADR-012`) | Amazon MSK, 3 brokers, SASL/IAM per topic, Glue Schema Registry | The **transactional outbox remains the source of truth**. No regulatory evidence exists only in a topic (`FF-26`) |
| **Operational search** (`ADR-013`) | One VPC-only OpenSearch domain per environment; application, firewall, flow and broker logs; 30 d hot → delete at 90 d | It holds **no evidence** and satisfies **no gate**. The log pipeline has no permission on the audit store (`FF-28`) |

Three things were **not** admitted alongside them, and the distinction is the point: no service
mesh, no per-service database clusters, and no analytics warehouse. Each is a recorded rejection in
[`CR-012 §3`](../../governance/change-requests/CR-012-r0-platform-robustness.md), not an oversight.

---

## 4. Why we are not merging "a couple of services"

These are the merge proposals that come up in every review, with the rejection criteria.

| Proposed merge | Temptation | Why we reject it (for now) | Revisit only if |
|---|---|---|---|
| **Consent + Suitability** | "Both are pre-quote compliance" | Different legal artefacts, retention, OTP evidence vs rule evaluation; different CMK/access; one failing must not corrupt the other | Never for R0/R1 — controls C1 and C2 are independently reconstructable |
| **Quotation + Proposal** | "Both are sales" | Quote = short-lived bursty fan-out/poll; Proposal = long-lived UW case with documents. Different datastore fit, scaling, failure modes | Evidence that both share one lifecycle *and* one team for 12+ months |
| **Quotation + Integration Hub** | "Quote always goes to 1SB" | Hub is the replaceability seam; baking 1SB into Quotation re-creates the Beema redirect problem inside our own code | Never — violates SC-W3-5 |
| **Hub + 1SB Adapter** | "Only one provider today" | Phase B adds Direct adapters behind the same Hub contract; merge makes every new insurer a Quotation change | Second provider commitment arrives — still keep Hub; add adapter |
| **Payment + Policy** | "Both are fulfilment" | Payment reconciliation can be `UNCERTAIN` while policy must not issue; money vs contract lifecycle | Strong evidence of identical ops ownership *and* no reconciliation breaks |
| **Journey + BFF** | "Orchestration is just API glue" | Then every channel reimplements the saga; DIY/hybrid become N copies of state | Never — ARCH-005 |
| **Audit as library-only** | "We already have bank-common-audit" | Library defines shape; service provides durable immutable query store fed by outbox | Never for regulated reconstruction |
| **Customer + Opportunity** | "Both are CRM-ish" | Customer snapshot is CIF/CBS-sourced and is a *fact about a person*; Opportunity is the origination record that carries the accountable SP and the `lob`. Different SoR, different retention, and merging them would put the single origination point inside a lookup facade | R2+ only with Product+Data joint ADR |
| **Configuration into the services that read it** | "One fewer service; each service knows its own rules" | Then a compliance rule change is a deployment, which is the coupling `CF-1` exists to prevent, and it would be discovered as debt the first time Compliance changed a consent statement. This trade was actually made in an earlier revision and has been **withdrawn** | Never — `CF-5` |
| **All R0 domains in one modular monolith** | "We are a small team" | Acceptable *deployment* tactic (one repo, few pipelines) is not the same as one *write model*. Modular monolith may be an interim packaging choice; boundaries and schemas must still exist | Packaging ADR with Mahesh+Amit+Shivanshi; does **not** erase context boundaries |

### 4.1 What we *are* willing to keep thin

Thin is not the same as merged, and it is not the same as absent. Two entries in the 2026-08-18
draft of this list were wrong and are corrected here rather than quietly dropped:

- **Proposal** can stay thin — status tracking before full ACR depth.  
- **Notification** in R0 is OTP + payment link only.  
- **Product Catalogue** in R0 is the Term matrix only: Life, Group A, Term.  
- ~~**Lead** can stay thin; the journey may start from a Customer lookup~~ → **withdrawn.**
  `#5` **Opportunity** ships whole in W1 as the single origination point (`AC-8`, `AC-9`). A
  journey that starts from a lookup has no accountable SP, no `lob` and no origination record.
  What stays thin is *sales-management breadth on top of* origination — campaigns, bulk import,
  ageing rules — which adds no journey capability and is deferred to S13.  
- ~~**Admin** in R0 is versioned config artefacts, not a UI service~~ → **withdrawn.** `#19`
  Configuration is a W0b **service** with a store, a version model, an effective-dated resolution
  contract and seeds. Only its **UI** is deferred, and administrators having no interface changes
  nothing about the layer (`CF-5`).

Thin services still own their aggregate so R1 can deepen without a rewrite.

### 4.2 Modular-monolith packaging (honest option)

If delivery pressure argues for fewer **deployable units**, the honest architecture answer is:

> Keep **logical** bounded contexts, schema-per-context with IAM walls, and ArchUnit boundaries —
> package multiple contexts into fewer Spring Boot processes only as an interim **ops** decision,
> with a recorded unpack plan.

That is a packaging ADR, not a domain merge. Do not present it as "we merged Consent into
Quotation."

**On the persistence half, R0 has already taken this decision and it is not database-per-cluster.**
`ADR-008` amends `ARCH-004`: ownership per context is invariant — each context owns its own schema,
with its own credential and its own migration history, and no service reads another's tables — but
the **physical topology is not**. R0 runs **one Aurora cluster with a schema per context**, and the
first physical split follows the **LOB-cell / shared-platform seam**, not the service boundary. A
reviewer who reads "database-per-service" in an older document and "one cluster" in this one is
looking at `ARCH-004` before and after `ADR-008`, not at a contradiction.

---

## 5. Review question — why relational (and not MongoDB) for Quote and Proposal?

### 5.1 Short answer

We are **not** putting Quote and Proposal in the same engine, and we are **not** choosing MongoDB
as the default document store.

| Aggregate | Chosen engine | Why this, not MongoDB |
|---|---|---|
| **Quote / quote jobs / offers** | **DynamoDB** | Single-key access by `quoteId`/`jobId`, short TTL, high churn, poller-friendly — the same pattern already proven in WS-1. MongoDB would add ops surface without a query pattern we need |
| **Proposal / UW case / requirements / documents metadata** | **Aurora PostgreSQL** — the shared R0 cluster, `proposal` schema (`ADR-008`) | Multi-row relational integrity over days/weeks; foreign keys between proposal, requirements, document refs; reconciliation joins; ACID around status transitions. Document DB "flexibility" becomes a liability when UW requirements must remain consistent |
| **Idempotency records** | **The owning service's own store**, in the same transaction as the business write | Not the cache, and this is a rejection by name rather than an omission — see §6.1. `ADR-011` refuses to hold idempotency in the cache tier because a cache cannot be transactionally consistent with a database write, and idempotency that is only mostly right on the money path is worse than none (`FF-23`) |

### 5.2 Why not MongoDB for Proposal specifically

1. **Integrity over flexibility.** UW cases are constraint-heavy (status machine, required docs,
   consent/suitability refs). Relational constraints catch illegal states at write time.  
2. **Bank ops familiarity.** Aurora/PostgreSQL is already the WS-1 job-store engine and the bank's
   default operational skill set; MongoDB would be a second operational class for one aggregate.  
3. **No document-tree access pattern that wins.** Proposal reads are by `proposalId`, status,
   customer/journey refs — classic relational filters + joins, not deep nested document search.  
4. **Binary documents stay in S3.** PDFs/videos are object storage regardless of DB choice; MongoDB
   GridFS is not an advantage here.  
5. **Compliance reconstruction** prefers clear tabular evidence trails with append-only siblings
   (consent/audit), not mutable nested documents.

### 5.3 Why Quote is DynamoDB (document-*ish*) but still not MongoDB

Quote jobs mirror `IntegrationJobEntity`: create → poll → complete/fail/timeout. That is a
**key-value state machine**, not an analytical document collection. DynamoDB gives TTL, single-digit
millisecond gets, and an ops model already adjacent to our AWS target. MongoDB would be a third
way to store short-lived jobs we already know how to run on DynamoDB/Aurora patterns.

### 5.4 Decision references

- Datastore table: [`05-data-architecture.md`](../architecture-review/05-data-architecture.md)  
- Ownership per context: **ARCH-004**, **amended by `ADR-008`** — ownership is the invariant, cluster topology is a decision  
- Cache tier and what it may not hold: **`ADR-011`**  
- Quote/Proposal split rationale: [`02-target-microservices-architecture.md`](../architecture-review/02-target-microservices-architecture.md) "Why these boundaries"

> If a reviewer insists on MongoDB, the correct response is: **raise an ADR with Aarti** comparing
> Proposal-on-Mongo vs Aurora against integrity, ops skill, backup/PITR, and IRDAI reconstruction
> drills — do not silently swap engines in a slide.

---

## 6. Review question — caching (today and later)

> **The answer to this question changed on 2026-08-24 and the change is worth stating first.** The
> 2026-08-18 draft of this pack said "no shared cache tier in R0". `ADR-011` admitted one. What did
> **not** change is the part reviewers actually care about: **R0 still does not cache quote
> payloads**, and idempotency still does not live in a cache. A cache tier now exists; the list of
> things it is forbidden to hold is what makes it safe.

### 6.1 Today (R0 / 1SB path)

R0 provisions **one ElastiCache for Valkey replication group per environment** (`ADR-011`), with a
per-service ACL user and key prefix. Three uses, and a hard list of non-uses.

| Layer | Caching in R0? | Notes |
|---|---|---|
| **1SB middleware** | Yes (provider-side) | 1SB absorbs insurer fan-out latency; we benefit without owning insurer cache invalidation |
| **Quotation service** | **No response cache of insurer offers** | Unchanged by `ADR-011`. Offers are suitability-gated, time-bounded and commercially sensitive; serving a stale cached offer past a suitability or consent TTL is a compliance defect, not a latency win |
| **BFF session state** (`#2`) | **Yes — cache tier** | The session that makes the BFF stateless at pod level. This is why `ADR-011` exists at all: WS-2's accepted design already shipped a Redis session vault while `R0-LLD` preferred DynamoDB, and two session stores meant one of them was going to be rewritten. The DynamoDB `sessions` table is **withdrawn** |
| **Product Catalogue** (`#8`) | **Yes — L2 read-through** behind the in-process L1 | Low-write eligibility matrix; the one place a shared cache clearly earns its keep. A miss is a read, never an error (`S-25`) |
| **Configuration** (`#19`) | Read-through, **never past TTL** | Serving configuration past its TTL would let a withdrawn compliance rule stay live. Expiry means re-resolve, not serve-stale |
| **Rate limiting** | **Yes — per-principal counters** | The counters need to be shared across pods to mean anything |
| **Idempotency** | **No — rejected by name** | `ADR-011` refuses it and `FF-23` enforces the refusal: a static check that the idempotency port has no cache-backed implementation, plus a negative test asserting a cache-unavailable idempotency write still **refuses** rather than succeeding. The record is written in the same transaction as the business change, in the owning service's store, 24 h retention |
| **Any system of record** | **No** | A tier is a cache when losing all of it costs latency and nothing else. That is also why the cache is **deliberately absent from the DR region** — it rebuilds on first miss |
| **CBS customer snapshot** | Bounded freshness window only | Identity data: no unbounded stale fallback |

So the honest review line is:

> **R0 has a cache tier and deliberately does not cache quote payloads in it.** We rely on 1SB's
> caching for speed and enforce freshness via suitability assessment TTL + quote job TTL +
> poll-to-recover. What our cache holds is sessions, catalogue reads and rate-limit counters —
> things whose total loss costs latency. What it must never hold is idempotency, evidence, or a
> configuration value past its expiry.

### 6.2 Where caching logic will be added (planned seams)

| Seam | When | Where it lives | What may be cached | Invalidation / guard |
|---|---|---|---|---|
| In-process L1 | R0 | Inside each service | Hot catalogue and configuration reads | Short TTL; the L1 is what the L2 sits behind, not a competitor to it |
| Product Catalogue L2 | R0 | Valkey (`S-25`) | Product & eligibility reads | Admin publish version; short TTL; a miss falls through to the owning store |
| Configuration resolution | R0 | Valkey | `(lob, domain, version)` resolution results | **Never served past TTL** — expiry re-resolves |
| BFF session | R0 | Valkey | Opaque session → token custody state | Session lifetime; re-established by re-authentication after a total loss |
| Per-principal rate limits | R0 | Valkey | Counters | Window expiry |
| ~~Quotation idempotency~~ | — | **Owning service store** | Request fingerprint → response snapshot | 24 h; body mismatch → `409`. **Not a cache seam** — listed here only because the 2026-08-18 draft put it in this table by mistake |
| **Quote offer cache (platform-owned)** | **R1 candidate, only with Product+Compliance OK** | Behind Quotation | Normalised offers keyed by `(productId, ratingInputsHash, insurerId, assessmentId)` | Must include suitability id + expiry; never serve if the assessment expired; never cross customers |
| **Direct insurer adapter cache** | **Phase B/C (direct integrations)** | **Inside the adapter** | Per-insurer rate/quote fragments where the insurer SLA requires it | Not in Quotation; Hub contract stays canonical; TTL + insurer rate-card version |
| Journey read models | R1 | Projection store | BFF-facing projections | Event-updated; not SoR |
| Reporting cubes | S13 / R1 | Warehouse | Analytical only | Never on the sale path |

### 6.3 Design rule for any future quote cache

1. Cache key **must** bind `suitabilityAssessmentId` and its expiry.  
2. Cache miss or expiry ⇒ **re-quote**, never silent serve.  
3. **Quote content** lives behind Quotation or inside an adapter — never in the BFF. This is not in
   tension with the BFF holding its *session* in the cache tier: a session is per-principal state
   the BFF already owns, while a quote is another context's business decision, and a BFF that
   caches one is asserting a decision that is not its to make (`SC-W3-6`).  
4. PII in cache follows Shailja's rule: prefer references over raw PAN/Aadhaar in the shared tier.
   `FF-27` scans for regulated field patterns; the cache is not exempt from `FF-05`.  
5. Direct-insurer caching is an **adapter concern** so insurer-specific invalidation does not leak
   into domain services.  
6. Any new use of the tier declares its key prefix and gets its own ACL user (`FF-24`). A shared
   keyspace is how one service's eviction becomes another service's incident.

### 6.4 Why a fourteen-service pilot has three stateful managed tiers

This is the fair version of the reviewer's objection, and `CR-012 §6` states it rather than
minimising it: five layers add three stateful managed services, a sixth AWS account, an inspection
VPC per environment and two carrier circuits to a platform carrying **~100 journey starts per hour**.
The fixed cost of the estate now dominates its variable cost. Two risks are recorded for it —
`RISK-012` (fixed cost above the S09 budget line, envelope produced before first `apply` to `uat`)
and `RISK-014` (operational surface outrunning team maturity while `GATE-S08` is still open).

The defence is **not** capacity. It is that four of the five are decisions that get materially more
expensive to take later:

| Layer | Cost of deciding now | Cost of deciding after W0b / W4 |
|---|---|---|
| Routing (`ADR-009`) | An unwritten route table | Every route table, plus a carrier order and a bank firewall change — the two longest-lead items on the programme |
| Egress addressing (`ADR-010`) | An unallocated Elastic IP | Every external allowlist the bank and its providers hold |
| The session / cache port (`ADR-011`) | One port, unimplemented | The two things every single request touches |
| The publish contract (`ADR-012`) | One contract, unwritten | The one component that must not lose a record — changed during the vertical slice, because `03-solution-architecture-r0 §5.1`'s own revisit trigger ("a third distinct consumer class") already fires inside R0: audit, notification and compensation are three |

`ADR-013` is the exception and is argued as a gap rather than a cheap-now decision: the logs the
other four closures generate are not queryable by anything without it.

---

## 7. Future plans — direct insurer integrations (beyond 1SB)

### 7.1 Strategic stance (already ratified in product KB)

Integrations are **replaceable implementation details**. Domains and journeys stay stable.

```text
Quotation / Proposal / Payment / Policy
        │  bank-canonical only
        ▼
Integration Hub  ──►  Adapter: 1SB          (Phase A — now / R0)
                 ──►  Adapter: Direct-X     (Phase B — coexist)
                 ──►  Adapter: Direct-Y…    (Phase C — expand)
```

Source: [`08-integration-strategy.md`](../../au-bank-insurance-platform/knowledge-base/08-integration-strategy.md).

### 7.2 What changes when we go direct — and what must not

| Changes (adapter + Hub routing) | Must not change |
|---|---|
| New `adapter.<insurer>.*` module | Quotation/Proposal/Payment/Policy APIs |
| Hub `RoutingPolicy` rows (LOB/product → adapter) | Journey state machine meaning |
| Adapter-local caching, mTLS, insurer auth | Suitability/Consent hard gates |
| Raw payload archive per insurer in S3 | Bank-canonical offer/proposal model |
| Per-provider bulkheads & breakers | Attribution injection rules |
| A domain allowlist entry in the inspection VPC, and the Elastic IPs the insurer whitelists (`ADR-010`) | That **100% of egress is inspected** — a direct adapter does not get its own NAT gateway (`FF-22`) |

### 7.3 Why Hub-before-direct is mandatory in R0

Even though R0 only talks to 1SB, we still introduce **Integration Hub** in Wave 1 so that:

1. Domain services never compile against 1SB types (INV-ACL-01).  
2. Phase B is a routing + adapter change, not a platform rewrite.  
3. Per-provider isolation exists before a second dependency can take down all quotes.

Building "temporary" direct calls from Quotation to 1SB "until we need Hub" is how platforms become
permanently coupled to their first middleware.

### 7.4 Caching difference: 1SB vs direct

| Mode | Who caches insurer rates/quotes | Platform responsibility |
|---|---|---|
| **1SB (R0)** | 1SB | Idempotency + job state; optional later offer cache with suitability binding |
| **Direct (Phase B+)** | Often **us** (adapter-level) | Adapter cache + rate-card versioning + Hub timeouts/bulkheads; Quotation still speaks canonical |

### 7.5 Sequencing guardrails

- No direct adapter work until GATE-S08 and the S09 critical path are credible (foundation).
  The Direct Insurer Adapter is deferred to **S13** for exactly this reason — it is not on the
  critical path to one proven journey.  
- No second aggregator / multi-hub fantasy without commercial evidence (charter out-of-scope).  
- Direct adapters enter behind Hub only — never as a second parallel path from Flutter/BFF.  
- A new provider is a routing row, an adapter module and an allowlist entry. If it is also a
  change to a domain service, the Hub seam has failed and that is the finding.

---

## 8. Full platform scope — R0 / R1 / R2+ (release language)

Use this section with [`hdl.svg`](../../hdl.svg), the North Star rendering. Read it beside
[`r0-reference-architecture.svg`](../../architecture/r0-reference-architecture.svg): **same ten
boundaries, same context numbers, same names** — the two files differ only in what the colour
encodes (`HA-02b`).

| File | What its colour encodes | The question it answers |
|---|---|---|
| [`hdl.svg`](../../hdl.svg) | **Release** — an `R0` / `R1` / `R2` chip on every element, marking the release in which that element **first exists** | *Where is this going?* |
| [`r0-reference-architecture.svg`](../../architecture/r0-reference-architecture.svg) | **Build wave** — `W0b` … `W4`, the order R0 is built in | *What are we building now?* |

The one encoding you can read straight across is LOB ownership: the rose cell and the `LIFE` tag
mean the same thing in both files (`LB-R1`). A context that is `R0` on one file and `R1` on the
other is a **defect** in whichever file disagrees with
[`03-solution-architecture-r0.md §3`](./03-solution-architecture-r0.md) — not a difference of view.

> **`hdl.svg` is an architecture-intent artefact, not a licence to build R1+ now.** Only the R0
> band is inside WS-3's `current_scope`. Every `R1`–`RN` element is either an `out_of_scope` item
> with a `revisit_at`, or has not been triaged at all. It exists so that R0 is built with the right
> seams.

### 8.1 Context × release matrix

| # | Context | R0 (wave) | R1 | R2+ |
|---|---|---|---|---|
| 19 | **Configuration** (Administration & Config) | **service — store, versions, effective-dated resolution, seeds (W0b)** | Admin & Configuration BFF + UI | — |
| 5 | **Opportunity** | **the single origination point, whole (W1)** | durable demand object: `CROSS_SELL` and campaign origination | renewal / lapse creates a **new** Opportunity |
| 9 | Journey Orchestration | assisted state machine, steps resolved from `#19` (W1) | DIY + hybrid modes; Journey Registry / LOB Router split | servicing journeys |
| 14 | Integration Hub | route to 1SB (W1) | Group B redirect support | multi-adapter routing |
| 4 | Customer | ETB CBS lookup over the TGW (W1) | — | NTB / V-KYC hooks; Party generalisation |
| 8 | Product Catalogue | Life / Group A / Term (W1) | Group B + ULIP/Savings | non-life classes |
| 6 | Consent | OTP grants (W2) | more templates / vernacular | — |
| 7 | Suitability | Term hard-gate (W2) | ULIP / Savings models | other LOBs |
| 10 | Quotation | 1SB Term (W2) | richer compare/share; optional offer cache; per-LOB cell | multi-provider fan-out |
| 11 | Proposal & UW | thin submit/status (W3) | ACR / fraud depth; per-LOB cell | — |
| 12 | Payment | AU PG customer device (W3) | cheque / mandate breadth | — |
| 13 | Policy & Issuance | confirm + PDF visibility (W3) | servicing views | renewals |
| 16 | Audit & Compliance | append-only evidence (W3) | richer query/export | — |
| 2 | RM Workspace BFF | assisted UI (W4) | hybrid hand-off | — |
| — | Flutter RM application | the R0 UI — none exists today (W4) | DIY surface is a **different** app | — |
| 17 | Notification | OTP + pay-link (W4) | welcome calling, breadth | — |
| 3 | Identity & Access | workforce, WS-2 (consumed) | retail customer auth | partner / call-centre identities |
| 15 | 1SB Adapter | exists, WS-1 (consumed) | harden | coexist with direct |
| 1 | Customer BFF | **deferred S13** | DIY surface | deepen |
| 18 | Reporting & MIS | **deferred S13** — pilot funnel is read from the audit store | full MIS / DWH push | executive control tower |
| — | Direct Insurer Adapter | **deferred S13** | spike possible | Phase B/C production |
| — | Operations / Call-centre BFF | — | — | work queues, journey search, disposition |

Rows are ordered by R0 build wave, then by what is deferred — because "what do we build first" is
the question this table is actually asked. The count that matters is the one in §1.2: **fourteen
deployable services plus one app**, against a nineteen-context target.

### 8.2 What reviewers should *not* conclude from the R0 HLD

- That DIY is cancelled — it is **sequenced** to R1 after a proven assisted sale.  
- That origination is thin — `#5` **Opportunity** ships whole in W1 and is the *only* way into the
  funnel. What is deferred is campaign and bulk breadth on top of it.  
- That configuration is a document — `#19` is a **service**, and it is the first one built.  
- That direct integration is impossible — Hub exists so it is **inevitable without rewrite**.  
- That the customer is a user — the customer is **not an on-platform actor in R0**. Their device
  receives an OTP and a payment link and reaches no platform service.  
- That an empty band means a missing boundary — a band that is thin at R0 is **drawn and labelled
  as such**, never omitted (`LY-1`).  
- That 19 contexts must be staffed now — building them before S08/S09 repeats the original error
  at larger scale (realignment plan §4).

---

## 9. One-screen answers for live review

**Q: Why use Cloudflare and F5 BIG-IP instead of AWS CloudFront and AWS WAF?**  
A: Adopting **Cloudflare Enterprise** and **F5 BIG-IP / WAF** aligns our platform directly with AU Bank's existing enterprise perimeter contract and central InfoSec policies. Cloudflare provides carrier-grade DDoS mitigation and edge acceleration, while F5 enforces enterprise L7 inspection rules and custom iRules. Behind F5, an **External ALB** terminates ingress traffic and routes securely to **Amazon API Gateway**, which connects via private VPC Link to the **Internal ALB**.

**Q: How do we connect to Core Banking (CBS)?**  
A: All customer lookups and CIF queries route through `#4 Customer Service` calling standard bank **EBS (Enterprise Service Bus) APIs** over private Transit Gateway links, adhering to bank enterprise architecture standards and protecting the core mainframe.

**Q: Why are both AWS CloudTrail and Amazon CloudWatch mandatory?**  
A: CloudTrail provides immutable governance and audit logging for RBI cybersecurity compliance (who made what AWS API call or IAM change), while CloudWatch delivers real-time runtime metrics, container telemetry, and alarms for 24/7 operational reliability.

**Q: What are the enterprise standards for CI/CD and IaC?**  
A: **GitLab CI/CD** is the bank enterprise pipeline standard executing multi-stage test gates, and **Terraform** is the IaC baseline for declarative provisioning across all AWS accounts.

**Q: How many services is R0, exactly?**  
A: **Fourteen deployable services plus one app** — not nineteen. The delta between that list and
the nineteen-context target is the single most useful thing this pack can say to a programme that
reads "16 services missing" as its scope. See §1.2 and §8.1.

**Q: Why so many services?**  
A: Each owns one regulatory or commercial decision with its own lifecycle and failure domain.
Fewer boxes without those boundaries deletes controls, not complexity.

**Q: Which service is built first?**  
A: `#19` Configuration, in W0b — before anything that reads rules from it. Building it after its
readers is how hardcoded branches get written and never removed. See §3.0.

**Q: Why not merge Quote and Proposal?**  
A: Burst short-lived fan-out vs weeks-long UW case — different data, scaling, and ops. See §4.

**Q: Why not MongoDB for quotes/proposals?**  
A: Quotes → DynamoDB jobs; Proposals → Aurora for relational integrity. MongoDB wins neither
access pattern. See §5.

**Q: Where is quote caching?**  
A: In R0, at 1SB — not in our Quotation payload store. R0 *does* have a cache tier (`ADR-011`), and
it holds sessions, catalogue reads and rate-limit counters. A platform offer cache is an R1
candidate behind suitability TTL; a direct-insurer cache lives in the adapter in Phase B. See §6.

**Q: Is idempotency in Redis?**  
A: **No, and it is rejected by name.** The record is written in the same transaction as the
business change, in the owning service's store. A cache cannot be transactionally consistent with a
database write, and idempotency that is only mostly right on the money path is worse than none
(`ADR-011`, `FF-23`). See §6.1.

**Q: Why does a fourteen-service pilot need MSK, ElastiCache, OpenSearch, a firewall and a Transit
Gateway?**  
A: Not for capacity — at ~100 journey starts per hour it is not capacity. Four of the five are
decisions that get materially more expensive after W0b and W4: routing, egress addressing, the
session port and the publish contract. The cost is stated rather than minimised, as `RISK-012` and
`RISK-014`. See §3.6 and §6.4.

**Q: Is #5 still a thin Lead service?**  
A: No. It is **Opportunity**, it ships whole in W1, and it is the single origination point. A
journey that starts from a customer lookup has no accountable Specified Person, no `lob` and no
origination record. See §3.2 and §4.1.

**Q: How do we do direct insurer later?**  
A: New adapter behind Integration Hub; domain APIs unchanged. See §7.

**Q: Why does the diagram look R0-only?**  
A: Because [`r0-reference-architecture.svg`](../../architecture/r0-reference-architecture.svg) is
the R0 cut and R0 is the release we must make real first.
[`hdl.svg`](../../hdl.svg) is the same ten boundaries and the same context numbers with a release
chip on every element. Read them side by side. See §8.

**Q: What are we achieving?**  
A: A bank-owned, evidence-bearing, provider-replaceable distribution platform where one assisted
Term sale can complete lawfully — and the rest of the catalogue can grow without rewriting the
spine. See §1.

---

## 10. Authority and sign-off

| Role | Action |
|---|---|
| Mahesh (Architecture) | Owns this justification; human T4 signature outstanding |
| Aarti (Data) | Co-owns §5 datastore rationale; physical schema remains hers. Her `CR-012` verdict covers the cache tier and the shared cluster |
| Rajal (Product) | Owns R0/R1/R2 sequencing; this doc must not invent Product scope |
| Deepali (Security) | Owns trust-boundary consequences of BFF/Hub/adapter splits, and `TB-7` / the `ADR-010` interim IPS posture |
| Shivanshi (SRE) | Owns runtime implications of service count vs packaging ADR, and the operational surface recorded as `RISK-014` |
| Shailja (Compliance) | Owns permissibility of any future quote-content cache |

**This document records no approval and creates none.** `CR-012` is `PENDING RATIFICATION`, and
neither it nor `ADR-009`…`ADR-013` is an authority to provision. Nothing here satisfies the
mandatory human T4 Architecture sign-off, Deepali's S07-G3/G4 security sign-off, or Aarti's S07-G5
data sign-off — each of which remains separate and outstanding.

**Signed:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2)  
**signature_status:** `AI-DRAFTED — mandatory human signature outstanding`  
**Date:** 2026-08-18 · **aligned** 2026-08-25 (renderings, `CR-012` / `ADR-009`…`ADR-013`, `#5` Opportunity, `#19` Configuration, `ADR-008`)  
**Triage:** SUG-20260818-4c3
