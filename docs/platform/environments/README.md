# Environments — AWS platform, UAT phasing and cost model

**Parent:** [`docs/platform/README.md`](../README.md)
**Stage:** S09 — Platform & Environment Foundation ([stage definition](../../application-lifecycle-bible/stages/S09-platform-foundation.md))
**Owners:** Shivanshi (SRE) · Deepali (Security) · Aarti (Database) · Kalpana (Delivery)
**Status:** ⚠️ **AI-DRAFTED planning input.** Nothing here marks a gate passed. GATE-S09 approval
requires Shivanshi (AP, B), Deepali (AP, B, **human**), Mahesh (AP), Aarti (AP), Shailja (AP).

---

## Why this folder exists

S09 §6 records the platform position as **🔴 Missing** — no IaC, no Kubernetes, no environments, no
proven restore, no attested residency. [`docs/hdl.svg`](../../hdl.svg) specifies the target: EKS in
`ap-south-1`, private subnets, database-per-service, 7-year WORM audit evidence, DR in `ap-south-2`.

These four documents are the bridge: **how the UAT environment is built phase by phase between
August 2026 and January 2027, what each AWS component is configured as, what it costs, and how the
infrastructure code is laid out** — sized to the workload the NFR catalogue actually derives
(≈1.7 journey starts per minute, ≈6.8 at Q4 peak), not to the diagram's silhouette.

---

## Contents

| Document | Answers |
|---|---|
| **[00 — Platform team requirements brief](./00-PLATFORM-TEAM-REQUIREMENTS-BRIEF.md)** | **The externally-shareable version.** A self-contained requirements document for a cloud/infrastructure team with no knowledge of this project or its vocabulary: what the system does, the workload numbers to size against, 60 numbered requirements each with its rationale, the optimisation mandate per environment, acceptance criteria, dependencies and a glossary. Contains no internal terminology, stage codes, gate IDs or persona names |
| **[01 — UAT phased build plan](./01-uat-phased-build-plan.md)** | Six phases (U0–U5) with dates, what each builds, what each deliberately does not, the GATE-S09 evidence each produces, and the external dependencies that actually govern the January date |
| **[02 — AWS component configuration](./02-aws-component-configuration.md)** | Every component with its exact dev/UAT/prod parameters: accounts, CIDR plan, EKS and node strategy, pod sizing for the 18 R0 services, Aurora/DynamoDB/S3, KMS hierarchy, IRSA, observability, deployment |
| **[03 — Cost model and optimisation](./03-cost-model-and-optimisation.md)** | Line-by-line arithmetic for UAT (~$308/mo) and prod (~$1,450/mo), the fourteen optimisation levers ranked by rupees saved, the ~$4,700 naive comparator, cost guardrails, and where the estimate is most likely to be wrong |
| **[04 — IaC module layout and apply order](./04-iac-module-layout-and-apply-order.md)** | Terraform module tree, the three tfvars profiles, remote state, the layered apply graph with timings, the policy-as-code that stops a cost optimisation reaching production, and the Phase U0 first-week checklist |

---

## The five decisions that carry the most weight

1. **No Amazon MSK in R0.** The HDL states *"transactional outbox (S-17, at-least-once, no Kafka in
   R0)"*; the earlier architecture review still lists MSK. Raised as **ADR-CAND-01** so the omission
   is a decision rather than a gap. Removes ~$450/month per environment.
2. **One Aurora Serverless v2 cluster with auto-pause**, database-per-service isolation enforced by
   IAM and PostgreSQL roles rather than by separate clusters (**ADR-CAND-02**). ARCH-004's stated
   control is *no cross-service access*, and that is what is enforced and tested.
3. **UAT scales to zero outside 08:30–20:30 IST weekdays; dev is destroyed and recreated weekly.**
   The cheapest option is also the strongest evidence — the weekly recreate *is* S09-VT-01.
4. **Object Lock mechanism proven in UAT with a 1-day horizon; the 7-year horizon is a production
   obligation.** Deletion is refused identically either way; UAT test data does not become
   undeletable until 2034.
5. **One set of modules, three tfvars profiles**, with policy-as-code asserting that the
   `resilient` profile cannot carry a cost optimisation. S09-G2 evidence and the cost saving are the
   same artefact.

---

## What governs what, on conflict

| Source | Wins on |
|---|---|
| [`CURRENT-STATE.yaml`](../../governance/state/CURRENT-STATE.yaml) `never` list | Residency, retention, encryption, consent, suitability, payment device — non-negotiable |
| [`docs/hdl.svg`](../../hdl.svg) | R0 target-state shape; supersedes the architecture review where they differ (e.g. Kafka) |
| [`ws3-platform/05-nfr-catalogue.md`](../ws3-platform/05-nfr-catalogue.md) | Every number these documents size against |
| [`stages/S09-platform-foundation.md`](../../application-lifecycle-bible/stages/S09-platform-foundation.md) | The gate criteria and validation tests |
| [`architecture-review/04-aws-infrastructure-architecture.md`](../architecture-review/04-aws-infrastructure-architecture.md) | Target-state service mapping — a **recommendation**, superseded by the HDL for R0 |
| These documents | Nothing. They are a planning input awaiting human review |
