# 02 — Guardrails

Guardrails are the **only** reasons work may be refused outright. They are few, stated as
invariants, owned by a named role, and — wherever possible — enforced by a machine rather than by
a reviewer's memory.

The machine-readable list is [`state/guardrails.yaml`](../state/guardrails.yaml). This page explains
how to use it.

## 1. Rules for guardrails themselves

| Rule | Why |
|---|---|
| **GR-1** A guardrail is an invariant about the *system or its data*, never about the *process* ("no PII in logs", not "a plan must be reviewed"). | Process rules belong in the flow model, where they can be tuned. |
| **GR-2** Every guardrail names an owner role and an enforcement: `code` (ArchUnit, test, policy-as-code), `ci` (pipeline check), `release` (checkpoint evidence) or `review` (human judgement). | A guardrail enforced only by `review` is a candidate for automation — the retro tracks the ratio. |
| **GR-3** Adding a guardrail is a Type-1 decision; removing or weakening one is a Type-1 decision **and** an `R2` control change. | Guardrails should be hard to add and harder to remove. |
| **GR-4** Guardrails are programme-wide or lane-scoped. There is no stage-scoped guardrail — stage-scoped limits become *release* checkpoints instead. | "Not yet" is a scheduling judgement; "never" is a guardrail. Mixing them is what froze AIGEM. |
| **GR-5** A breach found in existing code is an `Expedite` card, not a reason to stop other lanes. | Containment, not a programme freeze. |

## 2. Guardrail vs checkpoint vs horizon — which one is it?

Most of AIGEM's "out of scope now" list was a mixture of the three. Use this test:

```text
Would doing it EVER be wrong?               → guardrail   (state/guardrails.yaml)
Is it fine to build, but not to expose yet? → checkpoint  (docs/06 §5, per environment)
Is it just not the most valuable thing now? → horizon     (Later, with a review date)
```

Examples from the carried-over AIGEM lists:

| AIGEM item | New classification |
|---|---|
| "Payment executed on an RM or bank-employee device" | **Guardrail** `GR-PAY-01` |
| "Bank apps calling 1SB or the database directly" | **Guardrail** `GR-ARC-01` (ArchUnit) |
| "Flyway or JPA inside 1sb-integration-service" | **Guardrail** `GR-ARC-03` (ArchUnit) |
| "Kafka / event backbone — revisit at integration architecture stage" | **Not a guardrail.** ADR-012 already chose MSK for R0 — this is a **horizon/decision** item for the adapter lane |
| "Dashboards, alerting, SLOs — Phase 6" | **Checkpoint** — required before `prod`, buildable any time |
| "Health and Motor LOB handlers — Phase 5" | **Horizon** `Later` + open decision on the DEC-20260816-05 unfreeze coupling |
| "Customer self-service (DIY) journey — R1" | **Horizon** `Later` |
| "Redis idempotency — Phase 5.4" | **Checkpoint** — required before running >1 instance in any shared environment (`ASM-002`) |

## 3. How an agent uses guardrails

1. Before starting a card, read the guardrails whose `scope` matches the lane (the context loader
   includes them automatically).
2. If a change **would** violate a guardrail, stop that path, say which guardrail, and propose an
   alternative. Do not ask for an exception — exceptions to guardrails do not exist; changing a
   guardrail is a Type-1 decision for its owner.
3. If a change **touches** a guardrail's enforcement (the ArchUnit rule, the PII scrubber, the
   consent gate), the PR is `R2` ([06](./06-assurance.md)).
