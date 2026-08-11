# 06 — Platform Team Request Forms

**Up:** [UAT environment plan](./README.md)

Copy-paste ticket text, one per phase. Each is self-contained: a platform engineer should be able
to action it without reading the rest of this folder, and each links back for the reasoning.

---

## Ticket 1 — Phase 1: UAT foundation & walking skeleton

```text
TITLE: Provision UAT environment — Phase 1 (foundation)

PRIORITY: P1 — blocks GATE-P4 criterion 4.3 (open gate on WS-1)
TARGET:   Week 4
BUDGET:   $450/month (estimate $327 optimised)
REGION:   ap-south-1 (Mumbai)
ACCOUNT:  new dedicated `uat` account under AWS Organizations

--- DAY 0, BEFORE ANYTHING ELSE ---
[ ] Allocate an Elastic IP and send it to the PO for 1SB whitelisting.
    Do this before the VPC exists. The whitelist takes 2-4 weeks and is the
    critical path; an EIP can be held unattached until the NAT Gateway is built.

--- NETWORK ---
[ ] VPC 10.60.0.0/16, 3 AZs   (confirm CIDR with bank network team first)
[ ] Private subnets /20 per AZ; public subnets /24 per AZ
[ ] ONE NAT Gateway (AZ-a) using the EIP allocated above — not one per AZ
[ ] S3 Gateway VPC Endpoint          <-- free, do not skip, biggest NAT saving
[ ] Internet-facing ALB via AWS Load Balancer Controller
[ ] Route 53 hosted zone + ACM certificate

--- COMPUTE ---
[ ] EKS 1.31, control-plane logging: api, audit, authenticator
[ ] Managed node group: m7g.large (ARM64/Graviton), Bottlerocket
    desired 3 / min 2 / max 6
[ ] gp3 root volumes, 50 GB, 3000 IOPS
[ ] Karpenter v1.x, consolidationPolicy: WhenEmptyOrUnderutilized
[ ] Namespaces: edge, core-sales, integration, platform, identity
[ ] PodDisruptionBudget on every Deployment
[ ] topologySpreadConstraints across AZs, whenUnsatisfiable: ScheduleAnyway

--- DATA ---
[ ] Aurora PostgreSQL Serverless v2, PG 16.4
    min_capacity: 0   <-- REQUIRED: this is what makes the DB follow the schedule
    max_capacity: 2, auto-pause after 10 min idle, Single-AZ, 7-day backup
[ ] One logical database + role per service. No cross-database grants.
    Each service owns its own Flyway history.
[ ] ElastiCache Valkey/Redis 7.x, cache.t4g.micro, single node
    (NOT ElastiCache Serverless — ~$90/mo minimum vs ~$12)
[ ] S3: uat-raw-payload, uat-documents, uat-artifacts
    30-day expiry lifecycle. DO NOT ENABLE OBJECT LOCK.
[ ] ECR: one repo per service; expire untagged 3d, keep last 10 tagged

--- PLATFORM ---
[ ] Argo CD (app-of-apps) — all workloads deploy from git, no manual kubectl apply
[ ] External Secrets Operator -> AWS Secrets Manager
[ ] EKS Pod Identity for service-to-AWS auth (not IRSA)
[ ] KMS: 2 CMKs (platform default, raw payload)
[ ] CloudWatch Logs with 7-DAY RETENTION SET AT LOG-GROUP CREATION
    Enforce via Config rule or IaC aspect — default is never-expire
[ ] Keycloak in `identity` namespace, backed by Aurora

--- COST CONTROLS (same sprint, not later) ---
[ ] Tag Policy: Environment, Workstream, Service, Owner, CostCentre, Schedule
[ ] py-kube-downscaler:
      downscaler/uptime: "Mon-Fri 08:30-21:00 Asia/Kolkata,Sat 09:00-14:00 Asia/Kolkata"
      downscaler/exclude: "true" on the platform namespace
[ ] EventBridge Scheduler -> Lambda: node group min/desired = 0 at 21:00, restore 08:15
      Lambda role scoped to the UAT account ONLY, with an account-ID assertion
[ ] SSM parameter /uat/scheduler/hold-until  (override; must auto-expire)
[ ] Health check at 08:45 -> page platform on-call if workloads are not Ready
[ ] AWS Budget $450 with alerts at 60/85/100%; Cost Anomaly Detection enabled

--- BUILD PIPELINE ---
[ ] MULTI-ARCH IMAGES: docker buildx --platform linux/amd64,linux/arm64
    Prerequisite for Graviton. Without it the ~20% compute saving is lost.

--- EXIT CRITERIA ---
[ ] Bank caller completes quote + proposal against UAT        (GATE-P4 4.3)
[ ] Flutter -> BFF -> Keycloak login, no OAuth token to client (GATE-IAM-P1 A.1)
[ ] 1SB EIP whitelisted; UAT credentials in Secrets Manager
[ ] Shutdown schedule runs one full week with no failed morning start
[ ] One month of Cost Explorer data captured to re-baseline the estimate

REFERENCE: docs/platform/uat-environment-plan/02-component-and-sizing-matrix.md
```

---

## Ticket 2 — Phase 2: Core sale path

> ⚠️ **Do not action until the architecture review is approved** by PO/Compliance/Sponsor.
> Phase 2 is costed but not authorised — see [governance position](./README.md#governance-position).

```text
TITLE: Scale UAT environment — Phase 2 (core sale path)

PRIORITY: P2 — gated on architecture-review approval
TARGET:   Weeks 5-14
BUDGET:   $750/month (estimate $583 optimised)
PRECONDITION: Phase 1 exit criteria met; architecture review approved

--- COMPUTE ---
[ ] Node group -> 3 x m7g.xlarge, min 2 / max 6
[ ] Karpenter Spot NodePool: m7g.large-2xlarge, capacity-type spot, max 6
    -> CI runners, batch, burst
[ ] gp3 60 GB
[ ] CONSIDER: Spot-first NodePool (spot weighted first, on-demand fallback)
    Additional ~$107/mo saving at Phase 3 scale. Keep Keycloak + Prometheus
    on on-demand. Adopt now that Phase 1 has proven pods restart cleanly.

--- DATA ---
[ ] Split Aurora into TWO clusters:
      core       — 0-4 ACU, platform-default CMK
      regulated  — 0-2 ACU, DEDICATED CMK (Consent + Payment)
                   append-only grants: no UPDATE/DELETE to the service role
[ ] ElastiCache -> cache.t4g.small
[ ] DynamoDB, PAY_PER_REQUEST: journey-state, quote-jobs, sessions, hub-routing

--- EDGE & ASYNC ---
[ ] API Gateway HTTP API in front of both BFFs (HTTP API, not REST — ~70% cheaper)
[ ] AWS WAF: 1 regional web ACL + OWASP managed rule groups
[ ] SNS + SQS: notification, document-generation, reconciliation queues
    DO NOT PROVISION MSK IN THIS PHASE — see 01-phase-plan-and-scope.md
[ ] Second ALB (internal)

--- OBSERVABILITY ---
[ ] kube-prometheus-stack + Loki, in-cluster (stops with the nodes — no AWS charge)
[ ] OpenCost for per-namespace cost showback

--- EXIT CRITERIA ---
[ ] Full Term journey end to end in UAT, RM-assisted
[ ] Consent/Payment encrypted under their own CMK; append-only proven by a failed UPDATE
[ ] Idempotency replay test passes against ElastiCache
[ ] Spot interruption during a test run does not lose a journey
[ ] Per-namespace cost showback visible to squads

REFERENCE: docs/platform/uat-environment-plan/02-component-and-sizing-matrix.md
```

---

## Ticket 3 — Phase 3: Compliance, scale & launch readiness

> ⚠️ Same gate as Ticket 2. Additionally: **if Phase 3 slips, slip MSK provisioning with it.**
> MSK cannot be stopped, so standing it up early "so it's ready" costs $147/month for nothing.

```text
TITLE: Scale UAT environment — Phase 3 (compliance, scale, launch readiness)

PRIORITY: P2 — gated on architecture-review approval
TARGET:   Weeks 15-24
BUDGET:   $1,750/month (estimate $1,400 optimised)

--- COMPUTE ---
[ ] Node group -> 4 x m7g.xlarge, min 3 / max 8
[ ] Karpenter Spot: + load-generator pool (burst during test windows only)
[ ] gp3 80 GB

--- EVENT BACKBONE ---
[ ] Amazon MSK: 3 x kafka.t3.small, 100 GB EBS per broker, 3 AZs
    PROVISIONED, NOT SERVERLESS (~$147/mo vs ~$547/mo)
    Provision only when Audit/Notification/Reporting consumers are ready to deploy.

--- SERVICE MESH ---
[ ] Istio in AMBIENT MODE — not sidecar mode
    34 sidecars at 100m/128Mi is ~3.4 vCPU / 4.3 GiB of overhead (~a whole node)

--- DATA ---
[ ] Third Aurora cluster: integration
[ ] core cluster max_capacity -> 8 ACU (load-test headroom; scale back after)
[ ] Reader instance on `regulated` — makes Multi-AZ failover tested, not assumed
[ ] ElastiCache -> cache.t4g.medium x2, Multi-AZ, encryption in transit + AUTH
[ ] DynamoDB -> 7 tables; PITR on audit-events ONLY ($0.22/GB-mo)

--- OBSERVABILITY & EDGE ---
[ ] Amazon Managed Prometheus + Amazon Managed Grafana (3 editors, 10 viewers)
    Per-user pricing — audit the user list quarterly
[ ] CloudFront + edge WAF
[ ] Athena + Glue over the S3 data lake for MIS
    DO NOT PROVISION REDSHIFT SERVERLESS — 8-RPU floor is ~$2,100/mo

--- SECURITY & DR ---
[ ] GuardDuty, Security Hub, AWS Config, org-wide CloudTrail
[ ] AWS Backup with cross-region copy to ap-south-2
    Backup only — NOT a warm standby

--- EXIT CRITERIA ---
[ ] Load test meets NFR targets: quote ack <300ms p50 / <800ms p99;
    quote poll <100ms p50 / <300ms p99   (architecture-review/06)
[ ] Full journey reconstructable from the Audit service (compliance evidence)
[ ] DR drill: restore `regulated` from cross-region backup; RTO measured
[ ] Security review / VAPT completed against the UAT endpoint
[ ] Forced node rotation with PDBs + Karpenter consolidation loses no journey

REFERENCE: docs/platform/uat-environment-plan/02-component-and-sizing-matrix.md
```

---

## Standing request — cost governance

```text
TITLE: UAT cost governance — standing operating discipline

[ ] Tag Policy enforced: Environment, Workstream, Service, Owner, CostCentre, Schedule
[ ] AWS Budgets per phase, alerts at 60/85/100% to SA + Platform Lead
[ ] Cost Anomaly Detection on the UAT account
    (in an environment idle 59% of the time, anything running at 03:00 Sunday is an anomaly)
[ ] Weekly 10-minute cost review in the platform stand-up:
      - Cost Explorer week over week
      - hold-until override usage (frequent use = the schedule is wrong, not the people)
      - anomaly alerts
[ ] RE-BASELINE 03-cost-estimate.md with real Cost Explorer data at the end of Phase 1,
    BEFORE Phase 2 is approved. One month of measured spend beats any further estimating.
[ ] Quarterly: Managed Grafana user audit; unattached EBS sweep; ECR registry size check
```

---

## One-page summary for the sponsor

| | Phase 1 | Phase 2 | Phase 3 / steady |
|---|---|---|---|
| Weeks | 1–4 | 5–14 | 15–24 |
| Services hosted | 8–10 | 14 | 19 |
| EKS nodes | 3 × `m7g.large` | 3 × `m7g.xlarge` | 4 × `m7g.xlarge` + Spot |
| Aurora | 1 cluster, 0–2 ACU | 2 clusters, 0–4 ACU | 3 clusters + reader, 0–8 ACU |
| Redis | `t4g.micro` | `t4g.small` | `t4g.medium` × 2 Multi-AZ |
| Event backbone | — | SNS/SQS | + MSK |
| **Cost (optimised)** | **$327/mo** | **$583/mo** | **$1,400/mo** |
| *Cost if left 24×7* | *$520/mo* | *$996/mo* | *$2,080/mo* |

**Build-out weeks 1–24:** ~$4,900 optimised vs ~$7,600 unoptimised.
**Steady state:** ~$16.8k/year vs ~$25k — the schedule alone is worth **~$8.2k/year**.

**Authorisation status:** Phase 1 is gate-blocking and can start immediately.
Phases 2–3 require architecture-review approval before provisioning.
