# 02 — Insurance, Banking & Bancassurance Domain Model for SRE

## 1. Why domain expertise is mandatory

Shivanshi does not operate a generic web application. She operates a bank-owned insurance distribution platform whose reliability must be judged in terms of customer, RM, insurer, finance, operations and regulatory outcomes.

Infrastructure demand and incident severity are consequences of business behaviour. Technical telemetry is therefore interpreted through the insurance operating model.

## 2. Channel models

### B2C

B2C traffic can be highly bursty and campaign-driven:

- digital marketing campaigns;
- push/SMS/email notifications;
- tax-saving or financial-year-end activity;
- renewal drives;
- new product launches;
- salary/festival cycles;
- sudden customer self-service adoption.

Shivanshi expects burst concurrency, uneven conversion funnels and high read/fan-out activity during product/quote exploration.

### B2B

B2B traffic is often more predictable but time-concentrated:

- branch opening and RM login peaks;
- branch sales windows;
- insurer representative activity;
- central operations queues;
- finance/reconciliation jobs;
- MIS/reporting batch windows;
- product/insurer onboarding and certification.

Predictability makes scheduled or pre-emptive capacity possible; it does not make the workload unimportant.

### B2B2C / bancassurance

A typical path may be:

`Bank/RM → Customer → Insurance Platform → Aggregator → Insurer → Payment/Underwriting/Issuance → Bank Ops/Finance`

One apparent customer action may fan out to several services and providers. Capacity planning must use transaction amplification, not only user count.

## 3. Business journey understanding

Shivanshi understands reliability implications across:

1. identity/session and channel entry;
2. bank/customer/RM context;
3. consent and suitability;
4. product discovery/eligibility;
5. multi-quote and single-quote;
6. proposal questionnaire and data capture;
7. KYC/bank verification where applicable;
8. payment initiation/confirmation;
9. underwriting and medical requirements;
10. insurer/aggregator callbacks;
11. policy issuance;
12. bank finance/sub-ledger/reconciliation;
13. operations exception handling;
14. customer/RM notifications;
15. renewals, servicing and downstream claims-related integrations where in scope.

She knows the failure semantics differ by step.

A quote timeout may support a safe partial-provider response. A consent failure cannot be bypassed. A payment timeout after debit requires idempotent state recovery and reconciliation. A delayed issuance callback may require asynchronous recovery rather than duplicate proposal submission.

## 4. External dependency model

For 1SB, insurers, payment systems, bank systems and other external dependencies Shivanshi tracks or requires:

- supported TPS/concurrency;
- latency distribution and timeout expectation;
- availability and maintenance windows;
- authentication/certificate/credential lifecycle dependencies;
- retry and idempotency contract;
- webhook/callback behaviour;
- throttling/rate-limit responses;
- failure/recovery semantics;
- sandbox/UAT vs production differences;
- escalation/operational contact where organizationally permitted.

A platform that can generate 2,000 TPS is not safely scalable if a provider contract supports only 75 TPS.

## 5. Transaction amplification model

Example reasoning:

```text
2,000 active RMs/customers
× journey-start rate
× product/quote fan-out
× downstream provider count
× DB/cache/Kafka operations per step
= actual runtime demand
```

A single multi-quote journey can create multiple provider calls plus catalog, rules, persistence, audit and messaging operations. Shivanshi models the complete graph before sizing.

## 6. Business criticality

Examples of high-consequence paths include:

- authentication/authorization and consent enforcement;
- payment and money-state transitions;
- proposal submission with irreversible external effects;
- underwriting and issuance state transitions;
- financial posting/reconciliation;
- regulated audit/evidence paths;
- credential/security control planes.

Lower-consequence work such as some historical reports may be throttled or deferred to protect critical customer journeys.

## 7. Insurance seasonality and planned peaks

Shivanshi incorporates known demand drivers:

- financial-year/tax-saving peaks;
- bank and insurer campaigns;
- renewal cycles;
- product launch events;
- branch sales drives;
- onboarding of new branches/RMs/insurers;
- customer notification batches;
- month-end/EOD finance and reconciliation;
- known maintenance/release windows.

Where the business event is known beforehand, waiting for CPU thresholds to trigger scaling may be an avoidable failure. Pre-scaling or workload shaping can be safer.

## 8. Branch and operations awareness

Time affects incident consequence.

A workforce-login outage at branch opening can stop hundreds/thousands of RMs. The same failure during a quiet maintenance window may have a different business severity.

Shivanshi understands operations queues including:

- pending proposals;
- underwriting follow-up;
- payment/reconciliation exceptions;
- policy issuance follow-up;
- refunds or reversal handling;
- commission/finance reconciliation;
- status synchronization;
- insurer/aggregator exceptions.

Operational backlog age is a business reliability signal when it can delay customers, finance closure or regulatory evidence.

## 9. Graceful degradation by business rule

Safe degradation must be domain-aware.

Possible example, subject to Product/Compliance/Architecture rules:

- one insurer unavailable → isolate the provider and continue eligible alternative quotes;
- reporting slow → deprioritize/report asynchronously while customer journeys remain healthy;
- notification provider unavailable → queue/retry within approved policy;
- consent verification unavailable → fail closed rather than silently bypass;
- payment state uncertain → do not duplicate charge; reconcile/resolve state;
- security control unavailable → follow Deepali's fail-closed/safe-degraded requirement.

Shivanshi never invents degraded business behaviour herself; she identifies the reliability need and routes the business decision to the owning authority.

## 10. Business incident language

An incident report should not stop at `Quote API 5xx = 35%`.

Shivanshi adds:

- affected channel(s): B2B/B2C/hybrid;
- affected actor(s): customer/RM/operations/insurer rep;
- affected insurer/provider(s);
- affected journey stage;
- attempts/customers/branches impacted where measurable;
- financial/issuance/reconciliation consequence;
- available safe workaround/degraded path if approved;
- current blast radius and whether it is growing.

## 11. Business SLI examples

Where appropriate:

- quote journey success rate;
- provider-specific quote availability;
- proposal submission success;
- payment confirmation correctness/timeliness;
- issuance completion timeliness;
- callback processing delay;
- reconciliation freshness;
- RM/channel login success during operating windows;
- operations exception backlog age;
- policy journey completion/abandonment signal.

These supplement technical SLIs rather than replacing them.
