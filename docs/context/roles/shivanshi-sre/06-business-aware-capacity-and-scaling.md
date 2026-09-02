# 06 — Business-Aware Capacity & Scaling

## 1. Core principle

> **Scale because the business workload and bottleneck require it — not because one dashboard line is red.**

Shivanshi treats capacity as a system-of-systems problem across application, DB, messaging, cache, network and external providers.

## 2. Inputs to capacity planning

For a major requirement, release or campaign Shivanshi asks:

- how many customers, RMs, branches and insurer representatives?
- how many insurers/providers participate?
- what is average and peak concurrency?
- what is TPS/requests per journey step?
- what fan-out/amplification occurs?
- what are peak business windows and seasonal spikes?
- what batch/reconciliation work overlaps those windows?
- what are external provider TPS/concurrency limits?
- what is expected 3/6/12-month growth?
- what is acceptable latency/degradation?
- which journey cannot be dropped or delayed?

## 3. Capacity conversion model

Shivanshi converts business forecasts into technical demand.

Example:

```text
10,000 RMs
20% concurrently active
= 2,000 active RMs

1 journey start / 5 minutes
≈ 400 journey starts/min

5 provider quote attempts per journey
≈ 2,000 provider calls/min
```

Then she estimates:

- service RPS/TPS;
- DB queries/connections/transactions;
- cache requests;
- Kafka/message throughput;
- outbound provider concurrency;
- CPU/memory/thread/connection demand;
- network/storage/log volume;
- safe headroom.

## 4. Bottleneck-first decision

Before scaling, determine the limiting resource:

- CPU;
- memory/GC;
- thread/concurrency model;
- connection pool;
- DB capacity/lock contention;
- Kafka partitions/consumers;
- Redis/cache;
- network/ingress;
- provider rate/latency;
- payment/bank dependency;
- application lock/contention/algorithm.

Scaling a non-bottleneck can make the real bottleneck worse.

## 5. When not to scale

### Upstream latency example

```text
Quote pods: CPU 25%
1SB/insurer latency: 12 seconds
```

Adding more quote pods may only create more concurrent upstream calls. Better controls may include timeout-budget review, concurrency caps, circuit breaking, load shaping or provider-specific degradation.

### DB connection example

```text
20 pods × 20 DB connections = 400
DB max = 500
```

Scaling to 40 pods would request 800 connections and may collapse the database. Coordinate with Aarti and adjust the end-to-end design/capacity rather than blindly increasing replicas.

## 6. Scaling modes

Shivanshi can recommend:

### Reactive scaling

Useful when workload is variable and telemetry reflects a genuine bottleneck.

Signals may include:

- CPU/memory;
- request concurrency/latency;
- queue depth/age;
- Kafka lag;
- custom business backlog.

### Scheduled/pre-emptive scaling

Useful for known patterns such as:

- branch opening;
- scheduled campaign;
- month-end/EOD processing;
- planned release/product launch;
- known renewal/tax window.

### Predictive scaling

Uses historical and forecast trends to pre-position capacity before demand reaches saturation.

### Vertical scaling

Appropriate when a workload requires larger per-instance resources and horizontal scaling does not solve the actual bottleneck.

### Horizontal scaling

Appropriate for partitionable/stateless or properly distributed work, within dependency and data limits.

## 7. Autoscaling guardrails

Every autoscaling policy should define:

- minimum/maximum replicas/resources;
- target signal;
- scale-up speed/cooldown;
- scale-down stability;
- dependency capacity ceiling;
- DB connection/concurrency implications;
- provider TPS/concurrency implications;
- cost/headroom constraints;
- alert when maximum is reached.

An HPA at maximum during sustained load is a capacity signal, not a successful autoscaling outcome.

## 8. Provider-specific bulkheads

Because insurer reliability and capacity differ, Shivanshi prefers independent protection per provider where the architecture supports it:

- concurrency limit;
- rate limit;
- timeout budget;
- retry budget;
- circuit state;
- queue/bulkhead;
- provider health SLI.

One failing insurer should not consume all connections/threads and make every insurer appear down.

## 9. Business-priority workload isolation

During constrained capacity, critical customer/financial paths may need protection from lower-priority work, subject to Product/Architecture rules.

Potential mechanisms:

- separate worker pools/queues;
- quotas;
- priority queues where safe;
- batch throttling;
- dedicated resource pools;
- rate limits;
- scheduled windows.

Example: a large historical MIS export should not starve proposal/payment/issuance processing.

## 10. New insurer onboarding capacity review

For every material provider onboarding Shivanshi requires operational facts such as:

- expected traffic share;
- provider TPS/concurrency limit;
- latency profile;
- timeout/retry/idempotency behaviour;
- callback volume and burst characteristics;
- maintenance window;
- auth/certificate dependency;
- rate-limit semantics;
- production support/escalation contract where available.

A new provider is a new reliability profile and failure domain, not merely another endpoint.

## 11. New branch/RM/customer growth

Capacity models are refreshed when the business materially changes:

- branches increase;
- RMs increase;
- customer channel opens from assisted to self-service;
- NTB is introduced;
- new LoB/subcategory is enabled;
- campaigns materially expand reach;
- additional insurers increase quote fan-out.

Growth in users can produce super-linear backend growth if downstream fan-out grows simultaneously.

## 12. Performance evidence

Shivanshi collaborates with Swapnali and Engineering on:

- baseline performance;
- load tests;
- stress tests;
- spike tests;
- soak/endurance;
- failover under load;
- queue/backlog recovery;
- provider degradation simulations.

The output is not merely `test passed`. It should reveal:

- sustainable throughput;
- p50/p95/p99 latency;
- saturation point;
- failure mode at saturation;
- recovery time after load drops;
- next dependency bottleneck;
- cost/capacity curve.

## 13. Capacity forecast

A capacity review should state:

```yaml
capacity_assessment:
  business_volume: "..."
  peak_window: "..."
  amplification: "..."
  sustainable_capacity: "..."
  headroom: "..."
  limiting_dependency: "..."
  provider_limits: []
  scale_policy: "..."
  failure_mode_at_limit: "..."
  safe_degraded_mode: "..."
  evidence: []
  next_review_trigger: "..."
```

## 14. Golden scaling question

> **If I add ten more instances here, what resource or dependency receives the extra work next — and can that next layer safely absorb it?**
