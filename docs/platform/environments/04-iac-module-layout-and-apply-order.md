# 04 — IaC Module Layout and Apply Order

**Companion to:** [`01-uat-phased-build-plan.md`](./01-uat-phased-build-plan.md) ·
[`02-aws-component-configuration.md`](./02-aws-component-configuration.md)

**Closes:** S09-E01-S01 (module standard) · S09-E01-S02 (remote state and locking) · S09-E02-S01
(same modules, different parameters) · S09-E01-S07 (policy-as-code in the pipeline) · S09-E01-S08
(drift detection)

S09 §6 records the current position as **🔴 Missing** — *"No `.tf`, no CloudFormation, no CDK, no
Helm chart, no Kubernetes manifest anywhere."* This document is the shape of what fills that gap.
It is a layout and a sequence, not the code; the code is Phase U0–U1 work.

---

## 1. Repository layout

**Recommendation: a separate `bank-insurance-platform-infra` repository**, not `infra/` inside this
monorepo. Three reasons, in order of weight:

1. **Different approvers and a different blast radius.** An application PR is approved by Amit; an
   IaC PR that touches prod IAM or a KMS key policy is approved by Deepali and Shivanshi. Separate
   CODEOWNERS on separate repositories makes that structural rather than procedural.
2. **Different cadence.** Application code merges many times a day; infrastructure changes are
   deliberate and reviewed. Mixing them makes the CI matrix and the branch-protection rules fight
   each other.
3. **Blast radius of a compromised token.** The CI role that can `terraform apply` to prod should
   never be reachable from a pipeline that builds application images.

The layout below is identical either way.

```text
bank-insurance-platform-infra/
├── modules/                          # reusable, versioned, no environment knowledge
│   ├── account-baseline/             # SCP attach, Config recorder, GuardDuty, CloudTrail, budgets
│   ├── network/                      # VPC, subnets, route tables, NAT|nat-instance, endpoints, flow logs
│   ├── connectivity/                 # Site-to-Site VPN / DX gateway to the bank DC (CBS)
│   ├── kms/                          # the 5 CMKs by data class + key policies
│   ├── eks-cluster/                  # cluster, OIDC provider, access entries, managed node group
│   ├── eks-addons/                   # Karpenter, ALB controller, External Secrets, KEDA, ADOT, Kyverno
│   ├── data-aurora/                  # Serverless v2 cluster + per-context databases, roles, IAM auth
│   ├── data-dynamodb/                # the 4 tables (+ global tables in prod)
│   ├── data-s3/                      # buckets incl. Object Lock, CRR, lifecycle
│   ├── secrets/                      # Secrets Manager entries + rotation Lambdas + SSM parameters
│   ├── irsa/                         # one IAM role per service, least-privilege policy documents
│   ├── edge/                         # ACM, WAF web ACL, CloudFront, Route 53 records
│   ├── observability/                # AMP workspace, alerting topics, log destinations
│   └── scheduling/                   # KEDA cron ScaledObjects, EventBridge schedules, dev destroy job
│
├── envs/
│   ├── _global/                      # org, Control Tower, SCPs, Terraform state backend  (U0)
│   ├── shared-services/              # ECR, Argo CD, CI OIDC roles, Route 53 parent zone  (U0)
│   ├── security/                     # log-archive + audit accounts                        (U0)
│   ├── dev/     └── terraform.tfvars # profile = "minimal"                                 (U1)
│   ├── uat/     └── terraform.tfvars # profile = "lean"                                    (U1)
│   └── prod/    └── terraform.tfvars # profile = "resilient"                               (U5)
│
├── policy/                           # policy-as-code, runs BEFORE every apply
│   ├── region-pinning.rego           # FF-08  — India regions only
│   ├── no-public-store.rego          # FF-09  — no public or unencrypted store
│   ├── no-wildcard-prod-iam.rego     # FF-09  — no wildcard prod IAM
│   ├── object-lock-required.rego     # FF-10  — audit buckets must carry Object Lock
│   └── tagging.rego                  #        — the six mandatory tags
│
├── charts/                           # Helm/Kustomize consumed by Argo CD
│   ├── platform/                     # cluster-wide components
│   └── services/<service>/           # one chart per bounded context, values-{dev,uat,prod}.yaml
│
└── .github/workflows/
    ├── plan.yml                      # on PR: fmt, validate, tflint, checkov, conftest, plan, Infracost
    ├── apply.yml                     # on merge: apply with approval for prod
    └── drift.yml                     # nightly: plan --detailed-exitcode, alert on drift (S09-E01-S08)
```

---

## 2. The one thing that makes the whole plan work: profiles

Every cost decision in [`03`](./03-cost-model-and-optimisation.md) and every prod/UAT difference in
[`02`](./02-aws-component-configuration.md) is **a variable, never a fork of the code**. That is not
an aesthetic preference — S09-G2 requires *"dev, UAT and production provisioned from the same
modules"* with the environment inventory and apply logs as evidence. If UAT and prod diverge into
separate module trees, the gate cannot be evidenced and the cheap environment stops predicting the
expensive one.

```hcl
# envs/uat/terraform.tfvars
profile = "lean"

# network
az_count             = 3
active_az_count      = 1
nat_strategy         = "gateway_single"   # minimal | gateway_single | gateway_per_az
interface_endpoints  = []                 # gateway endpoints only — see 02 §2.2
flow_log_sampling    = 0.1

# compute
system_node_type     = "t4g.medium"
karpenter_capacity   = ["spot"]
karpenter_arch       = ["arm64"]
default_replicas     = 1
pdb_min_available    = 0

# data
aurora_mode          = "serverless_v2"
aurora_min_acu       = 0                  # auto-pause
aurora_max_acu       = 4
aurora_multi_az      = false
aurora_global         = false
backup_retention_days = 7
dynamodb_global      = false
object_lock_days     = 1                  # mechanism proven; horizon is a prod obligation
s3_replication       = false

# observability
grafana_mode         = "oss_incluster"    # oss_incluster | amazon_managed
log_backend          = "loki_s3"          # loki_s3 | cloudwatch
log_retention_days   = 14
trace_sample_rate    = 0.05

# scheduling  — the single largest cost lever
scale_to_zero        = true
active_window_cron   = "30 8 * * 1-5"     # IST, via KEDA cron scaler
inactive_window_cron = "30 20 * * 1-5"
destroy_on_schedule  = false              # true only in dev
```

```hcl
# envs/prod/terraform.tfvars   — same modules, every safety dial turned up
profile = "resilient"
active_az_count = 3 ; nat_strategy = "gateway_per_az" ; flow_log_sampling = 1.0
default_replicas = 2 ; pdb_min_available = 1 ; karpenter_capacity = ["on-demand", "spot"]
aurora_min_acu = 1 ; aurora_max_acu = 8 ; aurora_multi_az = true ; aurora_global = true
backup_retention_days = 35 ; dynamodb_global = true
object_lock_days = 2555 ; s3_replication = true
grafana_mode = "amazon_managed" ; log_retention_days = 30 ; trace_sample_rate = 0.10
scale_to_zero = false
```

**Guardrail against a profile becoming a loophole.** A `conftest` policy asserts that when
`profile == "resilient"`: `aurora_multi_az`, `s3_replication`, `dynamodb_global` and
`aurora_global` are `true`; `object_lock_days >= 2555`; `default_replicas >= 2`;
`pdb_min_available >= 1`; `scale_to_zero == false`. Cost optimisation is thereby **structurally
incapable** of reaching production. That policy is worth more than any written standard.

---

## 3. Remote state

| Aspect | Configuration |
|---|---|
| Backend | S3 in `shared-services`, one bucket, key `env/<environment>/<layer>.tfstate` |
| Encryption | SSE-KMS with a dedicated CMK; bucket policy denies `s3:*` without `aws:SecureTransport` |
| Versioning | On, with `NoncurrentVersionExpiration` at 90 days |
| Locking | **S3 native state locking** (`use_lockfile = true`) — no DynamoDB lock table needed on current Terraform |
| Access | Cross-account `AssumeRole` from the GitHub OIDC provider. **Plan role is read-only; apply role is separate**, and the prod apply role requires a recorded approval to assume |
| Isolation | State per environment *and* per layer, so a network change cannot lock the data layer's apply |

---

## 4. Apply order and dependency graph

Layers are separate state files. The arrows are hard dependencies; anything not connected can run in
parallel.

```text
                      ┌──────────────────────┐
                      │ L0  org + guardrails │  U0   (management/security accounts)
                      │ Control Tower · SCPs │
                      │ CloudTrail · budgets │
                      └───────────┬──────────┘
                                  │
                      ┌───────────▼──────────┐
                      │ L1  shared-services  │  U0   ECR · TF state · Argo CD · OIDC roles
                      └───────────┬──────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
┌───────▼────────┐      ┌─────────▼─────────┐     ┌─────────▼─────────┐
│ L2  kms        │      │ L2  network       │     │ L2  account-      │   U1
│ 5 CMKs         │      │ VPC·NAT·EIP·SG    │     │     baseline      │
└───────┬────────┘      └─────────┬─────────┘     └───────────────────┘
        │                         │
        │              ┌──────────┴──────────┐
        │              │                     │
        │     ┌────────▼────────┐   ┌────────▼─────────┐
        │     │ L3 eks-cluster  │   │ L3 connectivity  │  U1 / U3
        │     └────────┬────────┘   │ VPN → CBS        │
        │              │            └──────────────────┘
        │     ┌────────▼────────┐
        │     │ L4 eks-addons   │  U1   Karpenter · ALB ctl · ESO · KEDA · ADOT · Kyverno
        │     └────────┬────────┘
        │              │
┌───────▼──────────────▼───────────────────────────────┐
│ L5  data: aurora · dynamodb · s3   +  secrets · irsa │  U2
└───────────────────────┬──────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
┌───────▼──────┐ ┌──────▼───────┐ ┌─────▼──────────┐
│ L6 edge      │ │ L6 observab. │ │ L6 scheduling  │  U3
│ ACM·WAF·CF   │ │ AMP·alerts   │ │ KEDA cron      │
└───────┬──────┘ └──────┬───────┘ └────────────────┘
        └───────┬───────┘
        ┌───────▼────────┐
        │ L7  workloads  │  U1 → U4, wave by wave, via Argo CD (GitOps, not Terraform)
        └────────────────┘
```

**Indicative first-apply times** (useful for planning the U1 week, and for the S09-VT-01
destroy-and-recreate exercise):

| Layer | First apply | Destroy |
|---|---|---|
| L2 network | ~4 min (NAT Gateway dominates) | ~3 min |
| L3 eks-cluster | **~12–15 min** (control plane) | ~12 min |
| L4 eks-addons | ~6 min | ~4 min |
| L5 data | ~12 min (Aurora cluster dominates) | ~8 min |
| L6 edge/observability | ~5 min (+ up to 20 min if CloudFront is in the path) | ~5 min |
| **Full environment** | **~40 min** | **~30 min** |

A ~70-minute round trip is what makes the ephemeral dev environment practical: the weekly
destroy-and-recreate fits comfortably inside a scheduled job, and **that job is the S09-VT-01
evidence** — *"destroy and recreate a non-production environment entirely from code; identical,
working environment, no manual step."*

---

## 5. Pipeline

### `plan.yml` — on every IaC pull request

```text
terraform fmt -check
terraform validate
tflint
checkov            (fails on CKV_AWS_* HIGH/CRITICAL)
conftest test      (policy/*.rego — region, public exposure, wildcard IAM, Object Lock, tags)
terraform plan -out=tfplan
infracost diff     → PR comment with the monthly delta
```

The order matters: **policy runs before plan output is reviewed**, so a reviewer never spends
attention on a plan that policy will reject anyway. S09-E01-S07's acceptance criterion is
*"policy-as-code blocks public exposure, unencrypted stores and over-broad IAM **before apply**"* —
before, not after.

### `apply.yml` — on merge to `main`

| Environment | Approval | Notes |
|---|---|---|
| dev | none | Also runs on the weekly recreate schedule |
| uat | 1 reviewer (SRE) | |
| prod | **2 reviewers, one of whom is Deepali or Shivanshi**, GitHub environment protection | S09-E03-S05: recorded approvals; an audited emergency path exists separately |

### `drift.yml` — nightly

`terraform plan -detailed-exitcode` per environment. Exit code `2` (drift) raises an alert with the
diff. S09-E01-S08's wording is exact and worth honouring: drift *"alerts and is reconciled, not
tolerated."* A drift that is left open for a week is a console-created resource by another name, and
S09-G1's evidence is the drift report.

---

## 6. Workload delivery — Argo CD, not Terraform

Terraform builds *infrastructure*; Argo CD deploys *workloads*. The boundary is where an IRSA role
meets a ServiceAccount annotation.

| Concern | Owner |
|---|---|
| VPC, EKS, Aurora, S3, IAM, KMS, WAF | Terraform |
| Karpenter/ALB-controller/ESO/KEDA installation | Terraform (Helm provider) — they are cluster infrastructure |
| The 18 R0 services, their config, their scaling | **Argo CD** `ApplicationSet`, one per environment |
| Database migrations | Flyway as an Argo pre-sync hook, expand/contract only |
| Rollback | `argocd app rollback` to the previous **digest** |

Promotion is a **digest** moving through `values-dev.yaml → values-uat.yaml → values-prod.yaml`,
never a rebuild (S09-E02-S02). The image that passed UAT is byte-for-byte the image that runs in
production; anything else means UAT tested something that no longer exists.

---

## 7. First-week checklist (Phase U0, from 2026-08-24)

Ordered so that the long-lead items start on day one rather than being discovered in week three.

- [ ] **Confirm the bank's on-premises CIDR allocation** before any VPC is applied — a VPC CIDR cannot be changed later
- [ ] **Raise DEP-2 (CBS connectivity) with the bank network team.** 6–10-week lead; it is the critical path to January
- [ ] Create the AWS Organization; enable Control Tower with home region `ap-south-1`
- [ ] Create 6 accounts (prod deferred to U5); attach `DenyNonIndiaRegions` and `DenyUntaggedCreate` SCPs
- [ ] Stand up the Terraform state bucket with KMS and native locking
- [ ] Wire GitHub OIDC → per-account plan and apply roles (plan role read-only)
- [ ] Land `policy/*.rego` and the `plan.yml` workflow **before** the first `network` module is written
- [ ] Set per-account budgets with alerts and the 120% non-prod budget action
- [ ] Enable Infracost on the infra repository
- [ ] **Verify the application images build for `linux/arm64`** — Graviton underpins lever 5, and finding out in U1 is expensive
- [ ] Confirm the DNS subdomain delegation from the bank's DNS (DEP-4)
- [ ] Open the 1SB and AU Bank PG credential requests (DEP-1, DEP-3) — before the EIP exists, so only the IP itself is outstanding

---

**signature_status:** `AI-DRAFTED — mandatory human review outstanding (Shivanshi, Amit, Deepali).`
**Date:** 2026-08-18
