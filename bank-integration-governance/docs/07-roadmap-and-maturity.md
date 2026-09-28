# 07 — Roadmap and capability maturity

Two things AIGEM fused into "current stage" are separated here:

- **Where we are going** → an outcome roadmap (Now / Next / Later).
- **How mature each capability is** → a maturity grid, reported, never used to refuse work.

## 1. Outcome roadmap

[`state/outcomes.yaml`](../state/outcomes.yaml) holds outcomes, not features. An outcome is a
measurable change in what someone can do. The product owner owns the order; lane leads own the
cards beneath.

| Horizon | Rule |
|---|---|
| **Now** | ≤ 2 outcomes per lane. Each has a measurable done signal. |
| **Next** | Ordered. May be started in `Explore` mode (spikes, contracts) at any time. |
| **Later** | Carries `review_by`. Nothing waits here indefinitely. |

Releases (R0, R1, R2) remain as **business commitments** — a bundle of outcomes the bank signs up
to — not as a gate on work. R1 work that is dependency-safe and useful may start as soon as
someone has capacity; it simply does not get promoted to prod before R0's commitments are met if
the PO says so. That is a *release* rule, set at the checkpoint, not an intake rule.

## 2. Capability maturity grid

Each capability is scored on the same five levels. Different capabilities are expected to be at
different levels at the same time — that is the point.

| Level | Name | Meaning (evidence-based) |
|---|---|---|
| M0 | Idea | Named on the capability map; no agreed behaviour |
| M1 | Specified | Behaviour, rules and acceptance examples written; contract drafted |
| M2 | Built | Implemented behind stubs/flags; tests and contract tests green in CI |
| M3 | Integrated | Running against real providers / bank systems in UAT with evidence |
| M4 | Operable | Prod checkpoint evidence complete: SLOs, runbooks, DR, compliance evidence |
| M5 | Proven | Live, measured against its outcome, incidents learned from |

Mapping from the old models — used once for migration, then retired:

| AIGEM L | Lifecycle Bible S | New |
|---|---|---|
| L0–L1 | S00–S05 | M0–M1 |
| L2–L3 | S06–S07 | M1 (design is part of specifying) |
| L4 | S08–S09 | Not a capability level — **platform** lane outcomes |
| L5–L6 | S10–S11 | M2–M3 |
| L7 | S12 | M3 → M4 |
| L8 | S13 | New capabilities entering at M1+ with reuse |
| L9 | S14 | M4 |
| L10 | S15 | M5 |

## 3. Using maturity without re-creating stage gates

- **Maturity is computed from evidence** (checkpoint queries, CI, contract tests), not declared.
- **Maturity never refuses intake.** An M1 capability can have an `Explore` card for prod
  observability; an M4 capability can take a new feature.
- **Maturity shapes advice.** The persona cards carry a "watch-out by maturity" line — e.g. at M1,
  the architecture advisor challenges premature infrastructure; at M3, the reliability advisor
  insists on runbooks. This keeps AIGEM's valuable insight ("a good idea at the wrong time") as
  *advice to the doer*, not a gate.

## 4. Posture by maturity — for advisors

| Level | Bias towards | Challenge (advice, not refusal) |
|---|---|---|
| M0–M1 | Written rules, examples, contracts, open questions | Code that hard-wires unsettled behaviour — suggest configuration or a flag |
| M2 | One path end to end, contract tests, stubs | Generic frameworks, abstractions with one implementation |
| M3 | Real integration evidence, error handling, idempotency | New scope on the path before it is integrated |
| M4 | Observability, DR, runbooks, performance evidence | Unmeasured scaling ("more pods" is not a diagnosis) |
| M5 | Outcome measurement, incident learning | Rewrites without evidence |
