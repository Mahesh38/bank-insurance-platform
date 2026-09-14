# 01 — UAT Environment: Phased Build Plan (Aug 2026 → Jan 2027)

**Stage:** S09 — Platform & Environment Foundation (overlapped with S08, per
[CURRENT-STATE.yaml](../../governance/state/CURRENT-STATE.yaml) `current_phase`)
**Workstream:** WS-3 (primary) · consumes WS-1 (1SB adapter) and WS-2 (identity)
**Owners:** Shivanshi (SRE — platform/IaC/runtime) · Deepali (Security — baseline, keys, exposure) ·
Aarti (Database — stores, backup, restore) · Amit (Engineering — pipeline) · Kalpana (Delivery — critical path)
**Target:** production-ready **January 2027**
**Status:** `AI-DRAFTED` — planning input for S09. No gate is marked passed by this document.

---

## 0. Triage record (AGENTS.md §5 — triage before writing)

```text
SUG-UATPHASE · "Build UAT phase-wise on AWS, cost-optimised, production-ready by Jan 2027"
Stage: S08 + S09 overlapped — FITS (S09-E02 "Environments" is the named deliverable)
Scope: in_scope — "Foundation Recovery Increment: S08 engineering foundation and S09 platform foundation"
Necessity: MUST      Verdict: ADMIT
Priority: P1 now (S09 is the critical path to any UAT) · P1 at target (S14 needs UAT evidence)
Recorded: this document; register rows proposed in §9 for R12/SRE to enter
Constraint honoured: agents never edit stage state; nothing below marks a gate PASSED.
```

---

## 1. What the architecture actually demands (read of `docs/hdl.svg`)

The R0 HDL is not a large system. It is a **small, heavily-controlled** one. Six properties drive
every sizing and cost decision below:

| # | What the diagram says | What it means for UAT |
|---|---|---|
| 1 | *"AWS ap-south-1 — EKS, private subnets · every service stateless at pod level · ≥ 2 pods across ≥ 2 AZs"* | The ≥2-pod/≥2-AZ rule is a **production** availability rule (NFR-AVL-01, 99.9%). UAT runs **1 pod per service** except during the deliberate HA/DR drill in Phase U4. This alone halves UAT compute. |
| 2 | *"transactional outbox (S-17, at-least-once, **no Kafka in R0**)"* | **No MSK in UAT or prod for R0.** [`04-aws-infrastructure-architecture.md`](../architecture-review/04-aws-infrastructure-architecture.md) still lists Amazon MSK; the HDL supersedes it for R0. This removes the single largest line item (~$400–500/month per environment) and a whole operational surface. Raised as **ADR-CAND-01** in §9. |
| 3 | *"ALL provider traffic routes here (SC-W3-5)"* + 1SB IP whitelisting | Exactly **one** egress path with a **fixed EIP**. One NAT Gateway in UAT, not three. The EIP must be allocated in Phase U1 and sent to 1SB immediately — it is on the critical path (§7). |
| 4 | *"database-per-service (ARCH-004) · no cross-service DB access (ArchUnit + IAM)"* | The control is **no cross-service access**, not *no shared cluster*. UAT runs **one Aurora Serverless v2 cluster, one logical database + one IAM-authenticated role per service**. Isolation is enforced by IAM and PostgreSQL role grants, which is what the fitness function actually tests. |
| 5 | *"S3 + Object Lock · 7-yr WORM"* | The **mechanism** is proven in UAT, the **horizon** is not. UAT buckets use Object Lock in COMPLIANCE mode with a **1-day** default retention — deletion is still refused (S09-VT-08 passes) and you are not paying to store test data until 2034. |
| 6 | Capacity model: 50 concurrent RMs, ~1.7 journey starts/min BAU, 6.8/min at Q4 peak ([NFR §2.1](../ws3-platform/05-nfr-catalogue.md)) | *"This is a small workload, and saying so is the useful architectural finding."* UAT does not need to be big. It needs to be **correct, observable and recoverable**. |

**The 18 R0 services** (from the HDL bounded contexts): RM Workspace BFF (#2), identity-provider-adapter
and identity-authorization/PDP (#3), Customer (#4), Lead (#5), Consent (#6), Suitability (#7), Product
Catalogue (#8), Journey Orchestration (#9), Quotation (#10), Proposal & UW (#11), Payment (#12), Policy
& Issuance (#13), Integration Hub (#14), 1SB Adapter (#15), Audit & Compliance (#16), Notification (#17),
plus the existing `bank-persistence-service`.

**Five of those exist in this repository today** — `1sb-integration-service`, `bank-persistence-service`,
`identity-provider-adapter-service`, `identity-authorization-service`, `workforce-access-bff`. The
phasing below is built so the platform is *ready before the services are*, and each application wave
lands on an environment that already works.

---

## 2. Delivery reality check before the plan

The roadmap in [`07-delivery-roadmap-and-estimate.md`](../architecture-review/07-delivery-roadmap-and-estimate.md)
estimates **30–40 weeks** from a standing start to MVP. From 2026-08-18 to a January 2027 go-live
there are **~22 weeks**, GATE-S08 has **10 open criteria**, and S09 is recorded as **🔴 Missing** —
no `.tf`, no Helm chart, no Kubernetes manifest anywhere in the repository.

**January 2027 is achievable for the *platform* (S09) and for a *pilot-scoped* R0, and only if:**

1. The platform track (this document) runs **fully in parallel** with S08 and the application waves —
   it is not sequenced after them. The plan below assumes that.
2. The four external dependencies in §7 (AWS accounts, bank-network connectivity to CBS, 1SB UAT
   credentials + IP whitelisting, AU Bank PG sandbox) are unblocked in **August**, not October. These
   are procurement- and bank-network-paced, not engineering-paced, and they are the single biggest
   risk to the date.
3. Go-live scope is the R0 journey only — one RM, one Term product, one Group A insurer, ETB only.
   Anything in `out_of_scope` staying out.

This is Kalpana's (R12) call to make time-bound, not this document's to declare. **The plan is built
to hit January; the honest confidence is "achievable with the §7 dependencies closed in August,
at risk otherwise."** §8 gives the descope ladder if they slip.

---

## 3. Environment strategy — the three-line summary that saves the most money

| Environment | Lifecycle | Why |
|---|---|---|
| **dev** | **Ephemeral.** Created from the same Terraform modules on a schedule (Mon 08:00 IST) and **destroyed** (Fri 20:00 IST), plus per-PR namespaces. | An always-on third cluster costs ~$130/month to prove nothing. Destroying and recreating it weekly costs ~$40/month **and produces S09-VT-01 / S09-G3 evidence every single week** instead of once. The cheapest option is also the strongest evidence. |
| **uat** | **Persistent infrastructure, scheduled workloads.** Cluster and stores persist; pods scale to zero outside 08:30–20:30 IST weekdays; Aurora Serverless v2 auto-pauses. | UAT must hold state across days (journey state, audit evidence, reconciliation windows). But it does not need to *run* for the 118 hours a week nobody is testing. ~70% of compute hours removed. |
| **prod** | **Always-on, Multi-AZ, warm DR standby in ap-south-2.** | NFR-AVL-01 (99.9%), NFR-DR-01 (RTO ≤ 1h), NFR-DR-02 (RPO ≤ 5 min). No optimisation is allowed to touch these. |

**One set of Terraform modules, three tfvars profiles** (`minimal`, `lean`, `resilient`) — which is
exactly what S09-E02-S01 and S09-G2 require: *"each from the same IaC modules with different
parameters — not hand-built variants."* The cost optimisation and the gate evidence are the same
artefact.

---

## 4. The phases

Each phase states: what is built, what is **deliberately not** built, the exit evidence mapped to
GATE-S09, and the monthly run-rate the environment reaches at the end of it.

```text
2026        Aug        Sep            Oct            Nov          Dec         2027 Jan
            │  U0   │    U1     │      U2      │    U3     │   U4    │    U5     │ cutover
            ├───────┼───────────┼──────────────┼───────────┼─────────┼───────────┼─────────
S08 ════════╪═══════╪═══════════╡ (gate closes)
Wave W1     │       │           ╞══════════════╡
Wave W2/W3  │       │           │              ╞═══════════╡
Wave W4     │       │           │              │           ╞═════════╡
```

---

### Phase U0 — Landing zone and guardrails · 2026-08-24 → 2026-09-04 (2 weeks)

**Objective:** make it *impossible* to build the wrong thing, before anything is built.

**AWS components created**

| Component | Configuration |
|---|---|
| AWS Organizations + Control Tower | Home region **ap-south-1**. OUs: `Workloads` (dev, uat, prod), `Infrastructure` (shared-services), `Security` (log-archive, audit) |
| Accounts | 6: `management`, `security-log-archive`, `security-audit`, `shared-services`, `nonprod-dev`, `nonprod-uat`. **`prod` account is created in U5**, not now — an empty governed account still accrues Config/GuardDuty charges |
| SCPs | `DenyNonIndiaRegions` (allow only `ap-south-1`, `ap-south-2` + global services) — this is **FF-08 enforced at the org boundary**, above Terraform · `DenyRootUser` · `DenyCloudTrailDisable` · `DenyUntaggedCreate` (requires `env`, `workstream`, `service`, `owner`, `cost-centre`, `data-class`) |
| CloudTrail | Org-wide trail → `security-log-archive` S3, KMS-encrypted, **Object Lock enabled**. Management events only in non-prod (data events are the expensive ones) |
| Terraform remote state | S3 bucket in `shared-services`, versioned, KMS-CMK-encrypted, S3 native state locking, per-env key prefix, cross-account assume-role from CI |
| Cost guardrails | AWS Budgets per account (₹ threshold, alerts at 50/80/100%) + **budget action** that detaches the non-prod node scaling policy at 120% · Cost Anomaly Detection · Cost allocation tags activated |
| Pipeline policy-as-code | `checkov` + `conftest`/OPA in the IaC pipeline: region pinning (FF-08), no public/unencrypted store and no wildcard prod IAM (FF-09), Object Lock present on audit buckets (FF-10). **Infracost** posting a cost diff on every IaC PR |

**Deliberately not built:** any VPC, any cluster, the prod account, Transit Gateway (there is one VPC
per environment and no inter-VPC traffic in R0 — a TGW is ~$36/month of attachments plus data charges
for a topology that does not exist yet).

**Exit evidence:** S09-G12 (IaC scanning in the pipeline, E4) · S09-VT-02 (region pinning blocks an
`ap-southeast-1` apply) · budget and tagging policy demonstrably enforced by a rejected apply.

**Run-rate at end of phase:** ~**$45/month** (Control Tower's Config + GuardDuty across 6 accounts;
Control Tower itself is free, its guardrails are not).

---

### Phase U1 — UAT skeleton: network, cluster, first workload · 2026-09-07 → 2026-09-25 (3 weeks)

**Objective:** one real service, deployed by the pipeline, reachable, observable — end to end. The
narrowest possible vertical cut through the whole platform.

**AWS components created (in `nonprod-uat`)**

| Component | UAT configuration | Prod differs by |
|---|---|---|
| VPC | `10.40.0.0/16`, 3 AZs. Public `/24` ×3 (NAT + ALB only), private-app `/20` ×3, private-data `/24` ×3 | Same shape — the CIDR plan is allocated for all three environments in U0 (§`02` doc) |
| NAT Gateway | **1**, in AZ-a, with a **static EIP** | 3 (one per AZ) |
| VPC endpoints | **Gateway** endpoints for S3 and DynamoDB — free, and they keep store traffic off the NAT meter | + Interface endpoints for ECR/STS/Secrets Manager once 3-AZ NAT data charges justify them |
| EKS | 1 cluster, **v1.31**, control-plane logging: `api`, `audit`, `authenticator`. Private API endpoint + CIDR-allowlisted public access for CI | Private-only endpoint; CI via a runner inside the VPC |
| Node capacity | System MNG: **1 × t4g.medium On-Demand** (Karpenter, CoreDNS, ALB controller). Workloads: **Karpenter**, Graviton, **Spot-first**, consolidation on | On-Demand + 1-year Compute Savings Plan for the always-on baseline; Spot only for batch |
| Platform add-ons | Karpenter · AWS Load Balancer Controller (**single shared ALB via `IngressGroup`**) · External Secrets Operator (IRSA) · KEDA (used as the **cron scaler that scales UAT to zero**) · metrics-server · Argo CD (GitOps, in `shared-services`, targeting all clusters) | + PodDisruptionBudgets enforced by admission policy |
| Edge | 1 ALB (internet-facing, shared), ACM cert, Route 53 record `uat.<subdomain>` | + CloudFront + WAF (U3) |
| Access | **No bastion.** SSM Session Manager + `eks access entries`; break-glass role with CloudTrail alerting | Same, plus time-bound access approval |

**Workloads deployed:** the five services that already exist —
`identity-provider-adapter-service`, `identity-authorization-service`, `workforce-access-bff`,
`bank-persistence-service`, `1sb-integration-service` — plus Keycloak (in-cluster, UAT-only, backed
by the Aurora cluster from U2; until then a UAT-only local realm).

**Critical action, day 1 of this phase:** allocate the NAT EIP and send it to 1SB and to the bank
network team for whitelisting. This has a multi-week lead time and blocks Phase U2 (§7, DEP-1).

**Deliberately not built:** WAF/CloudFront (U3), Aurora (U2), DR anything (U4), a second AZ's NAT,
service mesh (S09 §7 explicitly names a service mesh as *premature at this stage* — mTLS is delivered
by ALB→pod TLS and NetworkPolicy in R0).

**Exit evidence:** S09-G1 (no console-created resource; drift report clean) · S09-G3 partial
(the **dev** environment is destroyed and recreated from the same modules — S09-VT-01) ·
S09-VT-03 (unattended pipeline deploy) · a request walked end to end producing a metric, a log line
and a trace (S09-G6 partial).

**Run-rate at end of phase:** ~**$185/month** (UAT) + ~$40 (ephemeral dev) + ~$45 (org).

---

### Phase U2 — Data, secrets and keys · 2026-09-28 → 2026-10-16 (3 weeks)

**Objective:** the stores exist, they are encrypted with owned keys, no service holds a static
credential, and **a restore has been performed and timed**.

**AWS components created**

| Component | UAT configuration | Prod differs by |
|---|---|---|
| Aurora PostgreSQL | **Serverless v2**, engine 16.x, **single writer, Single-AZ**, ACU **min 0 (auto-pause) / max 4**. One cluster; **one logical database + one IAM-auth role per bounded context**; `rds_iam` grants only. Backup retention **7 days**, PITR on | Multi-AZ (writer + reader), ACU min 1 / max 8, retention 35 days, **Aurora Global Database** → ap-south-2, deletion protection, IAM auth + rotation |
| DynamoDB | 4 tables — `journey-state`, `quote-jobs`, `idempotency`, `outbox-cursor`. **On-demand**, PITR on, CMK-encrypted, TTL configured | + Global Tables → ap-south-2 |
| S3 | `raw-payloads` (**Object Lock, COMPLIANCE, default 1 day** in UAT), `policy-docs`, `audit-archive` (**Object Lock**), `loki-logs`, `tf-state` (shared-services). All: block-public-all, CMK-SSE, versioning, lifecycle → IA at 30d | Object Lock default **2,555 days (7 y)**, Cross-Region Replication → ap-south-2, replication-time control |
| KMS | **5 CMKs by data class**, not per service: `k-pii`, `k-financial`, `k-audit-immutable`, `k-secrets`, `k-logs`. Annual rotation on. Key policies grant only the IRSA roles of the services in that class | Same hierarchy, separate keys per environment, multi-Region key for the audit class |
| Secrets Manager | **One consolidated JSON secret per service** (not one per credential — $0.40/secret/month adds up), read via External Secrets Operator + IRSA. Rotation Lambda for the Aurora master and for the 1SB credential | Same + automatic rotation schedules enforced, emergency-revocation runbook exercised |
| SSM Parameter Store | All **non-secret** config (base URLs, feature flags, timeouts). Standard tier = free | Same |
| ElastiCache | **Not provisioned.** R0 idempotency and the 24-h 1SB contract are served by the DynamoDB `idempotency` table; catalogue caching is in-process (Caffeine). ElastiCache Serverless has a ~1 GB floor (~$90/month) for a workload doing 1.7 journeys/minute | Revisit at R1 volumes only, with evidence |

**Workloads deployed:** Wave 1 — the journey spine (Journey Orchestration #9, Customer #4, Product
Catalogue #8, Integration Hub #14) as they land from S08.

**Deliberately not built:** per-service Aurora clusters (see §1 row 4 and ADR-CAND-02), read replicas,
7-year retention horizons in UAT, cross-region anything.

**Exit evidence:** S09-G5 + S09-VT-05/VT-06 (secrets management operational; a credential rotated in
UAT with no outage — closes **TD-006**) · S09-G7 + **S09-VT-07 (restore performed to a working state
and *timed*)** · S09-G8 + S09-VT-08 (deletion under Object Lock **refused**) · S09-G10 (encryption at
rest and in transit) · NFR-THR-06 computed: `Σ(pods × pool size) ≤ 60%` of the Aurora connection limit
at max replicas, asserted in the IaC review.

**Run-rate at end of phase:** ~**$255/month** (UAT).

---

### Phase U3 — Full R0 journey, edge and observability · 2026-10-19 → 2026-11-13 (4 weeks)

**Objective:** the whole R0 journey runs in UAT, and when it breaks you can see why in under a minute.

**AWS components created**

| Component | UAT configuration |
|---|---|
| AWS WAF | Web ACL on the ALB: AWS managed core rule set + known-bad-inputs + **rate-based rule (2,000 req/5 min per IP)** + a geo rule restricting to IN. Same rules as prod so the ruleset is actually tested |
| CloudFront | In front of the ALB for the customer-device surfaces (payment link, OTP page). Origin access restricted to CloudFront via a shared secret header + WAF rule |
| Amazon Managed Service for Prometheus (AMP) | Managed ingestion from the ADOT collector; 30-day retention. **Grafana OSS runs as a pod** — Amazon Managed Grafana bills $9/editor/month and buys nothing UAT needs |
| Logs | **Loki with an S3 backend, in-cluster** for application logs (14-day retention in UAT). CloudWatch Logs carries only EKS control-plane and Lambda logs. At ap-south-1's ~$0.67/GB ingestion, routing 20 GB/month of JSON app logs into CloudWatch costs more than the entire Loki setup |
| Tracing | ADOT Collector → AWS X-Ray, **5% head sampling** in UAT with 100% on error. `trace_id` propagated into every log line (`bank-common-observability`) |
| Audit pipeline | **Separate from operational logging** (S09-E05-S06): outbox → Audit service → `audit-archive` bucket under Object Lock. It never transits Loki or CloudWatch |
| Alerting | Alertmanager → SNS → email/Slack. Every alert payload carries a runbook link (S09-E05-S05) |
| Network policy | Default-deny NetworkPolicy per namespace; explicit allows matching the HDL seams only. `ns`: `edge`, `core-sales`, `fulfilment`, `integration`, `platform`, `identity` |

**Workloads deployed:** Wave 2 (Consent #6, Suitability #7, Quotation #10) and Wave 3 (Payment #12,
Policy #13, Audit & Compliance #16, Proposal & UW #11).

**Integrations activated:** 1SB UAT sandbox through the whitelisted EIP · AU Bank PG sandbox
(payment session + callback + settlement file) · CBS ETB lookup over the bank-network path (DEP-2).

**Deliberately not built:** DR region, load-test infrastructure, autoscaling *tuning* (S09 §7 lists it
as premature — HPA exists with sane defaults; it is tuned at S12 against measured load).

**Exit evidence:** S09-G6 + S09-VT-09 (metrics, logs and a trace correlated for one walked request) ·
S09-G11 + S09-VT-11 (a forbidden cross-zone connection refused) · S09-G13 + S09-VT-10 (**zero** PII
matches in the log store after a full suite run — NFR-SEC-01) · S09-VT-13 (dev cannot reach UAT/prod
stores) · the R0 journey completes end to end in UAT for the first time.

**Run-rate at end of phase:** ~**$308/month** (UAT), plus ~**$37** once the bank-network VPN (DEP-2) is live.

---

### Phase U4 — Evidence, resilience and residency · 2026-11-16 → 2026-12-04 (3 weeks)

**Objective:** close GATE-S09. This phase produces **evidence artefacts**, not features. It is also
the only phase where UAT is deliberately made prod-like — and then taken back down.

**Activities and what they cost**

| Activity | What is temporarily provisioned | Duration | Cost of the exercise |
|---|---|---|---|
| **Rollback drill** (S09-VT-04, NFR-DR-05) | Nothing extra. Deploy a deliberately broken version, roll back via Argo CD, confirm previous version restored and data intact, **timed** | 2 days | $0 |
| **HA drill** — the `≥2 pods across ≥2 AZs` rule | Scale UAT to prod topology: 2 replicas × 18 services, 2nd NAT GW, Aurora Multi-AZ. Kill an AZ's nodes; confirm no journey is lost | 5 days | ~**$60** for the window, then scaled back down |
| **DR restore drill** (S09-G7, NFR-DR-01/02) | **Cold** restore into `ap-south-2`: Aurora snapshot restore, DynamoDB PITR restore, S3 replica read. Measure wall-clock RTO and the RPO window. **Destroy immediately after** | 4 days | ~**$90** for the window |
| **Residency attestation** (S09-G9, S09-VT-12, NFR-DAT-06) | Nothing. An automated enumeration of every resource, backup, log destination and archive across all accounts, signed by SRE + Compliance | 3 days | $0 |
| **Retention purge** (NFR-DAT-07) | Nothing. Prove data past its horizon is disposed of **with an audit record of disposal** | 2 days | $0 |
| **Emergency revocation** (S09-E04-S05) | Nothing. Revoke and replace the 1SB partner credential under simulated incident conditions | 1 day | $0 |

A warm-standby DR environment is **not** built in UAT. The DR *runbook* is proven by a timed cold
restore; the warm standby is a **production** construct and is built once, in U5, in the prod account.

**Workloads deployed:** Wave 4 — RM Workspace BFF (#2), Notification (#17), and the Flutter RM
application pointed at UAT.

**Exit evidence:** S09-G4 (tested rollback) · S09-G7 (restore proven, times measured) · **S09-G9
(residency attested — E2, signed)** · S09-G2 (environment inventory across dev/uat + apply logs) ·
NFR-DR-01…05, NFR-DAT-01/02/06/07, NFR-SEC-07 all verified — which is precisely the S09 row of the
NFR catalogue's §4 verification-ownership table.

→ **GATE-S09 evidence pack complete. Approval is Shivanshi + Deepali (human) + Mahesh + Aarti +
Shailja; this document cannot and does not mark it passed.**

**Run-rate:** ~$345/month steady (incl. the bank VPN) + ~$150 one-off for the drill windows.

---

### Phase U5 — Performance, prod build and cutover rehearsal · 2026-12-07 → 2027-01-08 (5 weeks)

**Objective:** prove the numbers, then build production from the same modules with one tfvars change.

| Track | Detail |
|---|---|
| **Load and stress (S12 verification)** | Against UAT scaled to prod topology for the test window. Targets: NFR-THR-01 ≥ 10 journey starts/min sustained 30 min · NFR-LAT-03 **p95 quote result < 5 s** · NFR-THR-03 bulkhead (one insurer held at max latency must not degrade the others) · NFR-THR-04 saturation point **identified and documented** · NFR-THR-05 recovery < 5 min after load removal. Driver: k6 on Fargate Spot, ~$15 per full run |
| **Production account build** | Create the `prod` account, apply the **same modules** with `profile = resilient`: 3 AZ, 3 NAT, Aurora Serverless v2 Multi-AZ + Global Database → ap-south-2, DynamoDB Global Tables, S3 CRR + **7-year** Object Lock, WAF + CloudFront + Shield Standard, PodDisruptionBudgets, 1-year Compute Savings Plan on the measured always-on baseline |
| **Warm standby DR** | ap-south-2: minimal EKS + Aurora Global secondary + replicated DynamoDB/S3. Route 53 health-check failover. This is the only warm standby in the programme |
| **Cutover rehearsal** | Full deploy to prod from the pipeline with prod credentials and **synthetic data only** — `S09-E02-S06: production data in lower environments is technically prohibited`, and the converse discipline applies at cutover |
| **Buffer** | Two weeks of this phase overlap the December holiday period. That is intentional slack, not planning optimism |

**Exit:** S14 production-readiness inputs complete. Go/no-go on the R0 pilot, **2027-01-19 → 01-29**.

**Run-rate at go-live:** UAT ~$345 + dev ~$40 + org/shared ~$80 + **prod ~$1,450** ≈
**$1,900/month (~₹1.6 lakh/month)**. Full working in [`03-cost-model-and-optimisation.md`](./03-cost-model-and-optimisation.md).

---

## 5. Phase summary

| Phase | Dates | Weeks | Ends with | UAT run-rate |
|---|---|---|---|---|
| **U0** Landing zone & guardrails | 08-24 → 09-04 | 2 | Region/tag/cost policy enforced org-wide | $45 (org) |
| **U1** Network, EKS, first workload | 09-07 → 09-25 | 3 | 5 existing services live, pipeline-deployed | $185 |
| **U2** Data, secrets, keys | 09-28 → 10-16 | 3 | **Restore proven and timed**; TD-006 closed | $255 |
| **U3** Full journey, edge, observability | 10-19 → 11-13 | 4 | R0 journey runs end to end in UAT | $308 (+$37 VPN) |
| **U4** Evidence, resilience, residency | 11-16 → 12-04 | 3 | **GATE-S09 evidence pack complete** | $345 |
| **U5** Performance, prod build, rehearsal | 12-07 → 01-08 | 5 | Prod built from the same modules | + prod $1,450 |
| **Cutover** | 01-11 → 01-29 | 3 | R0 pilot live | — |

---

## 6. Cost headline

| Approach | UAT $/month | Basis |
|---|---|---|
| **Naive prod-mirror UAT** | **~$4,700** | 3 NAT · 8 provisioned Multi-AZ Aurora clusters · MSK 3-broker · one ALB per service · 6 On-Demand x86 nodes · Managed Grafana · all logs to CloudWatch |
| **This plan** | **~$308** (~$345 with the bank-network VPN) | Detailed in [`03-cost-model-and-optimisation.md`](./03-cost-model-and-optimisation.md) |
| **Saving** | **~93%** | And the plan is *closer* to the gate criteria, not further — the ephemeral dev environment and the scheduled UAT window generate recreate evidence the always-on version never produces |

The eight levers doing the work, in order of rupees saved: **(1)** no MSK — the HDL already says no
Kafka in R0 · **(2)** one Aurora Serverless v2 cluster with auto-pause instead of eight provisioned
Multi-AZ clusters · **(3)** scale-to-zero outside the 08:30–20:30 IST weekday window · **(4)** one
shared ALB via `IngressGroup` instead of one per service · **(5)** Graviton + Spot via Karpenter ·
**(6)** one NAT Gateway plus free S3/DynamoDB gateway endpoints · **(7)** Loki-on-S3 and Grafana OSS
instead of CloudWatch-ingest and Managed Grafana · **(8)** ephemeral dev instead of a third
always-on cluster.

---

## 7. External dependencies — the actual critical path

None of these is an engineering task, and every one of them can move the January date.

| ID | Dependency | Needed by | Lead time | Owner | Action now |
|---|---|---|---|---|---|
| **DEP-1** | **1SB UAT credentials + egress-IP whitelisting** of the UAT NAT EIP | U2 start (09-28) | 3–6 weeks | Kalpana + Rajal | Allocate the EIP in U1 week 1 and send it the same day. Already flagged in `R0-SCOPE.md` §6 and the roadmap doc as an external dependency on P1 |
| **DEP-2** | **Bank-network connectivity to CBS** for ETB lookup (S-04/S-05). AWS Site-to-Site VPN (~$40/month) is sufficient for UAT; Direct Connect only if the bank mandates it | U3 start (10-19) | **6–10 weeks** — bank network + security review | Shivanshi + bank network team | **Start in August.** This is the longest pole and the most commonly underestimated |
| **DEP-3** | **AU Bank PG sandbox** credentials, callback URL whitelisting, settlement-file drop | U3 (10-19) | 4–6 weeks | Kalpana + Finance | Also resolves NFR-OPEN-3 (settlement cadence) |
| **DEP-4** | **AWS accounts + budget approval** through bank procurement; DNS subdomain delegation | U0 start (08-24) | 2–4 weeks | Kalpana | If this slips, everything slips one-for-one |
| **DEP-5** | GATE-S08 closure (10 open criteria) | U2 workload deployment | — | Amit + Swapnali | Platform phases U0–U2 do **not** block on it; application waves do |
| **DEP-6** | Compliance sign-off on residency and 7-year retention position (D-011, NFR-OPEN-4) | U4 (11-16) | — | Shailja | S09-G9 is an **E2 signed attestation**; it cannot be AI-drafted |

---

## 8. If a dependency slips — the descope ladder

Applied in this order, because each step costs less than the one after it:

1. **Compress U5's prod build into U4** by building prod in parallel with the UAT evidence drills.
   Costs money (two environments live three weeks earlier), buys ~2 weeks.
2. **Defer the warm-standby DR region to post-go-live**, running prod single-region with a proven
   cold-restore runbook. NFR-DR-01 (RTO ≤ 1h) becomes *at risk* — this needs Shailja and Shivanshi to
   accept explicitly, and is a Board 6/7 decision, not a delivery one.
3. **Reduce the pilot** — fewer RMs, one branch — so the go-live is a controlled pilot rather than a
   full R0 rollout. Rajal's call.
4. **Move the date.** If DEP-2 (CBS connectivity) is not closed by end-September, January is not
   recoverable by engineering effort, and saying so in October is worth more than discovering it in
   January.

**What must never be descoped**, because they are licence conditions and appear under `never` in
`CURRENT-STATE.yaml`: India-region residency, 7-year immutable audit retention, encryption with
owned keys, consent-with-OTP, the suitability hard gate, payment on the customer device only.

---

## 9. Proposed register entries and candidate ADRs

Agents do not edit governance registers. These rows are drafted for R12/SRE to enter.

| Proposed ID | Type | Content |
|---|---|---|
| **ADR-CAND-01** | Architecture decision | **No Amazon MSK in R0.** The HDL states *"transactional outbox (S-17, at-least-once, no Kafka in R0)"*; `04-aws-infrastructure-architecture.md` still lists MSK as the event backbone. The HDL is the later artefact. Decide explicitly so the omission is a decision, not a gap. Owner: Mahesh + Shivanshi |
| **ADR-CAND-02** | Architecture decision | **One Aurora cluster per environment with database-per-service isolation enforced by IAM + PostgreSQL roles**, rather than one cluster per bounded context. Honours ARCH-004's stated control (*no cross-service DB access*); residual risk is shared blast radius and noisy-neighbour at scale. Revisit at R1 volumes. Owner: Aarti + Mahesh + Deepali |
| **ADR-CAND-03** | Architecture decision | **ALB + WAF as the public edge for R0; Amazon API Gateway deferred.** The architecture review specifies API Gateway for throttling and partner API-key management; R0 has no partner consumers, and WAF rate-based rules cover throttling. Owner: Mahesh + Deepali |
| **DEP-CAND-1…6** | Dependency register | The six rows in §7, with owner and required-by date, so dependency ageing is tracked by the Delivery Control System |
| **ASM-CAND-1** | Assumption register | All costs are **list-price estimates for ap-south-1 as of 2026-08**, before any EDP/private-pricing discount the bank may hold. They must be re-derived in the AWS Pricing Calculator before budget submission |
| **RISK-CAND-1** | Risk register | **CBS connectivity (DEP-2) is the critical path to January 2027.** 6–10-week lead, owned outside engineering. Mitigation: start in August; escalate weekly from September |
| **RISK-CAND-2** | Risk register | UAT scale-to-zero means UAT is **not** a 24×7 availability rehearsal. NFR-AVL-01 is verified at S14 in prod-like conditions, not in UAT steady state |

---

## 10. Companion documents

| Document | Answers |
|---|---|
| [`02-aws-component-configuration.md`](./02-aws-component-configuration.md) | Every AWS component with its exact UAT vs prod parameters, pod sizing, CIDR plan, IAM/IRSA model |
| [`03-cost-model-and-optimisation.md`](./03-cost-model-and-optimisation.md) | Line-by-line cost arithmetic, the optimisation levers with their savings, and the cost guardrails |
| [`04-iac-module-layout-and-apply-order.md`](./04-iac-module-layout-and-apply-order.md) | Terraform module layout, tfvars profiles, apply order and dependency graph, policy-as-code |

---

**Drafted by:** AI agent under AIGEM, simulating Shivanshi (SRE/platform) with Mahesh (architecture),
Aarti (data), Deepali (security) and Kalpana (delivery) inputs.
**signature_status:** `AI-DRAFTED — mandatory human review outstanding. GATE-S09 approval requires
Shivanshi (AP, B), Deepali (AP, B, human), Mahesh (AP), Aarti (AP), Shailja (AP), Amit/Swapnali/Kalpana (RV).`
**Date:** 2026-08-18
