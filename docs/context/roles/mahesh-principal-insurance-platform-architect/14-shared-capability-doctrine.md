# 14 — Mahesh Shared Capability Doctrine

## 1. Purpose

`VIN-001 §3` asks it plainly: *which capabilities should not be rebuilt three times?*

Getting this wrong is symmetric and both directions hurt. Share too little and the platform has
three consent models, three audit trails and three answers to who the customer is. Share too much
and every LOB release waits behind a shared component nobody owns, and a shared outage becomes a
total outage.

This file defines what qualifies as shared, in what **form** it is delivered, and what obligations
sharing creates.

---

## 2. What qualifies as shared

**Rule SC-01 — the qualification test.** A capability is shared when **any** of these holds:

| # | Qualifier | Example |
|---|---|---|
| 1 | Two consumers would otherwise produce **different answers to the same question about the same entity** | Party identity, consent, policy portfolio |
| 2 | Duplication would create **compliance, security or financial inconsistency** | Audit evidence, consent, attribution, reconciliation |
| 3 | It is **governance of what the bank may distribute** | Product Governance, configuration |
| 4 | It is **infrastructure-shaped**: mechanism-heavy, domain-light | Documents, notification delivery, observability, secrets |
| 5 | It is **money** | Payment |
| 6 | An enterprise standard mandates one implementation | Identity, authorization |

**Rule SC-02 — otherwise, local with a clean seam.** Where none holds, implement locally with a seam
that permits later extraction (`04 §7`). Extraction from two working implementations is cheap;
un-sharing a premature abstraction that three consumers already depend on is not.

**Rule SC-03 — two real consumers, not two planned consumers.** A "shared" component with one
consumer is a local component with extra ceremony and an owner who does not exist yet.

---

## 3. Three delivery forms

Sharing is not synonymous with "a service". Choosing the wrong form is a distinct failure from
choosing the wrong boundary.

| Form | Use when | Failure if misused | Examples here |
|---|---|---|---|
| **A — Platform service** | Shared **state**, shared **evidence** or shared **decisions** are required | A service for stateless helpers adds a network hop and an outage mode for nothing | Payment, Consent, Audit, Product Governance, Policy Portfolio, PDP |
| **B — Paved-road library** | Shared **mechanism and contract**, no shared state | A library holding business decisions makes every consumer's behaviour change on a version bump | `bank-common-error`, `-security`, `-audit` (event shape), `-observability`, `-secrets`, idempotency |
| **C — Versioned configuration / rule pack** | Shared **policy** that must change without a deployment | Policy compiled into code becomes a release for every business change | Consent pack, suitability pack, product matrix, routing policy, feature activation |

**Rule SC-04 — a shared library may carry contracts and plumbing, never business decisions**
(`ARCH-008`). The distinction is precise and load-bearing:

- `bank-common-audit` owns the **event shape**. `CAP-305` owns the **evidence store**.
- A library may define the `Idempotency-Key` contract. It may not decide whether a duplicate payment
  is permitted.

**Rule SC-05 — the paved road is consumed, not forked.** `IF-3` is built once in WS-3 and consumed by
WS-1, WS-2 and WS-3. A workstream that forks a paved-road library has created a second standard, and
the finding is `A1`.

---

## 4. Obligations created by sharing

**Rule SC-06 — nothing becomes shared without four things.** Sharing without them produces the
orphaned component every platform eventually has.

1. **A named owner** (a persona or team, not "the platform").
2. **A versioned contract** with a compatibility policy.
3. **A deprecation path** — how a consumer migrates off, and how long the old version lives.
4. **A stated availability posture** (§5).

**Rule SC-07 — a shared capability may not encode a consumer's specifics.** The moment
`CAP-301 Payment` contains a Life branch, or `CAP-302 Documents` knows what a medical report is,
sharing has failed. Consumers supply context; the shared capability applies mechanism.

The pattern to hold onto, from `VIN-001 §25`: **Life Proposal decides which document is required;
the Document platform decides how it is securely stored and retrieved.** The same shape applies to
Payment (`the LOB decides when and how much; Payment decides how`), Notification (`the domain emits
an intent; Notification decides delivery`) and Engagement (`the domain emits a fact; Engagement
decides whether to act on it`).

**Rule SC-08 — sharing concentrates risk.** Cell isolation (`LS-04`) does not reduce the availability
requirement on shared capabilities; it raises it. Every shared capability's NFR target is set by its
**most demanding** consumer, not its average one.

---

## 5. Availability posture per shared capability

**Rule SC-09 — every shared capability declares fail-open or fail-closed, and it is an architecture
property, not a runtime setting.**

| Capability | Posture on unavailability | Why |
|---|---|---|
| `CAP-502` Authorization (PDP) | **Fail closed — everything stops** | Default-deny is not degradable (`S-02`) |
| `CAP-104` Consent | **Fail closed** | No consent evidence, no personal-data use |
| `CAP-202` Suitability gate | **Fail closed** — a cache miss is a refusal | `C1`; `S-08` |
| `CAP-301` Payment | **No degraded mode on the money path** | `C4` |
| `CAP-105` Product Governance | Degraded: read cache within a bounded freshness window | Stale eligibility is bounded and detectable |
| `CAP-101` Party & Customer | Degraded: snapshot within freshness window; otherwise fail | No stale-unbounded fallback on identity data (`S-05`) |
| `CAP-305` Audit | Buffered by outbox; **journey blocked from `SOLD`** | `INV-JRN-05` — business continues, completion does not |
| `CAP-304` Notification | **Never blocks business state**; raises an operations task | `VIN-001 §26`; `S-18` |
| `CAP-302` Documents | Blocks the domain step that needs the document; nothing else | Locality of failure |
| `CAP-306` Engagement / `CAP-307` Timeline | Degrade silently; no business impact | Read models and cadence, not controls |

Two clusters, and the split is the point: **controls fail closed, conveniences fail soft.** A design
that fails a control open to preserve availability has inverted the platform's purpose.

---

## 6. Integration boundaries

`VIN-001 §16` separates two things that are routinely merged:

```text
Insurance Platform  ──▶  CAP-401 Bank Integration    ──▶  CBS / bank systems
LOB Cell            ──▶  CAP-205 Provider Integration ──▶  1SB / insurers
```

**Rule SC-10 — bank integration and provider integration are different capabilities.** Different
change rates, different credentials, different owners, different failure semantics. Merging them
means a CBS upgrade lands inside Life Quote code.

**Rule SC-11 — no business service implements an external protocol.** LOB services must not
implement CBS, mobile-banking or CRM protocols anywhere. `TI-02` and `TI-04` are the same rule
pointed at the two directions.

**Rule SC-12 — share the control plane, isolate the data plane.** Per `VIN-001 §17`, sharpened by
`VIN-002 §17` and reconciled at [`09 §5.1`](./09-target-state-architecture-doctrine.md):

| Shared **control plane** (`CAP-402`) | Isolated **data plane** per cell (`CAP-205`) |
|---|---|
| Canonical contracts (`IF-1`) | Provider protocol implementation |
| Provider capability registry | Which providers this cell actually calls |
| Authentication framework, credential handling, certificates | Provider credentials in use |
| Timeout, retry, breaker and bulkhead **policy** | Bulkhead **budgets** and connection pools |
| Error model and taxonomy | Provider error mapping |
| Idempotency contract | Provider idempotency keys |
| Observability contract | Provider-specific metrics |
| Routing policy model | Route execution |

**H0–H1: one Integration Hub, `SC-W3-5` unchanged.** A per-cell split requires an ADR and evidence
(`09 §5.1`).

**Rule SC-12a — this is the doctrine's worked example.** `CAP-403` is shared *as a control plane* and
isolated *as a runtime* per cell. Three runtimes on one versioned framework is isolation; three
frameworks is three platforms (`SC-05`, `PR-37`). Full doctrine:
[`17 §14`](./17-provider-aggregation-and-connectivity.md).

**`CAP-401` does not exist yet.** CBS access sits inside `#4 Customer`. That is acceptable at H0 with
one consumer; **the revisit trigger is the second capability that needs bank data**, and it is
recorded rather than assumed.

---

## 7. Configuration

**Rule SC-13 — configuration is a capability, not a properties file.** `VIN-001 §33` is right that
this becomes one of the largest capabilities on the platform. Insurance distribution is
configuration-heavy: insurers, products, channels, branches, eligibility, provider routes, feature
activation, effective dates, product versions.

**Rule SC-14 — governed configuration has six properties.** Anything lacking them is not
configuration, it is undocumented runtime behaviour:

1. **Versioned** — every change produces a new version, nothing is edited in place;
2. **Effective-dated** — changes take effect at a stated time, not at deploy time;
3. **Auditable** — who changed what, when, and why (`CAP-305`);
4. **Maker-checker where required** — Shailja determines which changes require it;
5. **Reversible** — a rollback path that does not require a release;
6. **Validated** — schema and business validation before activation.

**Rule SC-15 — in-flight processes keep the version they started under** (`OR-22`). Effective dating
without this is a compliance defect, not a feature.

**Rule SC-16 — configuration-driven is not configuration-uncontrolled.** A change to which insurers
the bank distributes is a business decision with a governance path, not a self-service toggle. The
mechanism being config does not lower the approval bar.

**R0 position:** Administration & Config is delivered as **versioned configuration artefacts consumed
at startup**, not a service with an admin UI — an explicit recorded trade that accepts a deployment
per rule-pack change until S13.

---

## 8. Observability as a shared capability

**Rule SC-17 — correlation is mandatory and its dimension set is fixed.** `VIN-001 §31`: every
transaction traceable via `correlationId`, `traceId`, `journeyId`, `opportunityId`, `proposalId`,
`paymentId`, `policyId`, plus **LOB, provider, channel and actorType**.

The last four are what make the observability capability *architectural* rather than operational:
they are the same dimensions as the variation axes (`VA-1`, `VA-3`, `VA-4`, and actor type from
`VA-5`). An architecture whose telemetry cannot be sliced by its own variation axes cannot prove any
of its isolation claims.

**Rule SC-18 — technical metrics must connect to business metrics.** Health Quote latency · Life
Proposal success rate · per-insurer timeout rate · B2C abandonment · call-centre recovery
conversion. A dashboard that shows pod restarts but not quote conversion cannot support an
architecture decision.

**Rule SC-19 — per-LOB dashboards and SLOs** (`VIN-001 §31`). Shared dashboards hide exactly the
divergence that cell isolation exists to create, and they make `LS-03` unverifiable.

**Rule SC-20 — no PII in logs**, proven by an automated test over a full suite run (`INV-LOG-01`,
`FF-05`). Observability is the most common accidental PII exfiltration path on a platform like this.

---

## 9. Data ownership versus physical separation

The `TI-05` split, restated where engineers will look for it (full reconciliation at
[`09 §5.2`](./09-target-state-architecture-doctrine.md)):

| Claim | Standing | Enforcement |
|---|---|---|
| One owner per authoritative datum | **Invariant** | Capability contracts (`file 10`) |
| No service reads another service's tables | **Invariant** | ArchUnit + IAM; verified in the S09 IaC scan |
| Separate credentials and schema ownership | **Invariant** | Secrets/KMS per service |
| Separate physical cluster per service | **Decision** | Evidence: scale, blast radius, security isolation, RTO/RPO, cost — **Aarti** |

**Rule SC-21 — sharing a capability never means sharing a database.** `CAP-301 Payment` is shared
because it exposes one contract, not because three cells write to one schema.

**Rule SC-22 — read models are not ownership.** `CAP-307 Interaction Timeline` and Reporting consume
events and hold derived state. They are never the source of truth, and a transactional service must
never query them synchronously — a slow analytics query must not be able to back-pressure a
customer-facing journey.

---

## 10. Anti-patterns

| Anti-pattern | Consequence | Correct move |
|---|---|---|
| Shared database as the sharing mechanism | Coupling with none of the benefits of a contract | `SC-21`, `TI-05` |
| Business logic in a shared library | Every consumer's behaviour changes on a version bump | `SC-04` |
| A shared service with one consumer | Ceremony without reuse | `SC-03` |
| Shared capability that knows its consumers | LOB logic in a platform capability | `SC-07` |
| Notification that decides when to engage | Cadence rules where nobody looks for them | `NS-08`, `JS-17` |
| A business service that knows which provider answered | The provider becomes a domain dependency; replaceability is lost silently | `TI-19`, [`17`](./17-provider-aggregation-and-connectivity.md) |
| A shared Policy service implementing insurer issuance protocols | Provider coupling in the most sensitive place | `VIN-001 §19`; issuance stays in the cell |
| Cell isolation without raising shared-capability availability | Risk relocated, not reduced | `SC-08` |
| Failing a control open to preserve availability | The control is absent exactly when it matters | `SC-09` |
| Config as an ungoverned toggle | Untracked change to what the bank distributes | `SC-14`, `SC-16` |
| Retroactive config applied to in-flight journeys | Evidence no longer matches what the customer saw | `SC-15` |
| Dashboards that cannot slice by LOB/provider/channel/actor | Isolation claims unverifiable | `SC-17`, `SC-19` |
| Forking a paved-road library per workstream | Two standards, one of them unmaintained | `SC-05` |

---

## 11. Authority

| Decision | Authority |
|---|---|
| Whether a capability is shared, and in which form | `A1_AUTONOMOUS` — Mahesh, ADR when durable |
| Extracting a shared library or platform service | `A2_NOTIFY` + `SC-06` obligations recorded |
| Changing a shared capability's availability posture | `A3_JOINT_REVIEW` — Shivanshi; Shailja/Deepali where it is a control |
| Physical data topology, source-of-truth changes | `A3_JOINT_REVIEW` — **Aarti** |
| Which configuration changes require maker-checker | **Shailja** |
| Credential handling and certificate management in `CAP-402` | `A3_JOINT_REVIEW` — **Deepali** |
| Retiring a shared capability with live consumers | `A4_HUMAN_REQUIRED` |
