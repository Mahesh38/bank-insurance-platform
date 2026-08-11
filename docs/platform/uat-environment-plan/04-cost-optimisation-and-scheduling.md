# 04 — Cost Optimisation & Scheduling

**Up:** [UAT environment plan](./README.md)

Two halves. First the **shutdown schedule** — the single biggest lever, worth ~$680/month at
Phase 3. Then **sixteen further levers**, several of which are worth more than the schedule and
cost nothing to apply.

---

# Part 1 — The shutdown schedule

## The schedule

All times **IST (`Asia/Kolkata`)**.

| Window | State | Hours/month |
|---|---|---:|
| Mon–Fri 08:30 – 21:00 | **UP** | 275 |
| Mon–Fri 21:00 – 08:30 | DOWN | — |
| Sat 09:00 – 14:00 | **UP** (defect-fix / regression window) | 21 |
| Sat 14:00 – Mon 08:30 | DOWN | — |
| Bank public holidays | DOWN | — |

**≈ 296 running hours against 730 — 41% uptime, 59% off.**

Three deliberate choices in that table:

- **Up at 08:30, not 09:00.** Nodes join, images pull, and Spring Boot starts in roughly 5–8
  minutes. Starting at 09:00 means the environment is unusable until 09:10 — and a UAT
  environment that isn't ready when testers arrive costs a squad-morning, which is worth far more
  than the 30 minutes of compute.
- **Down at 21:00, hard.** The evening tail is where "just one more test" quietly becomes 24×7.
  The override below exists precisely so the schedule can stay strict.
- **Saturday half-day, Sunday off entirely.** Defect-fix cycles genuinely happen on Saturdays
  during UAT. Sundays do not, and a full weekend shutdown is worth ~$95/month at Phase 3 on its
  own.

### If a stricter schedule is wanted

Mon–Fri only, 09:00–21:00 = **264 hours (36%)**. Saves a further ~$25/month at Phase 3. Not
recommended — it removes the Saturday defect window for a saving smaller than a single squad-hour.

---

## What actually stops, and what does not

| Resource | Stoppable? | Mechanism |
|---|---|---|
| EKS worker nodes | ✅ | Scale workloads to 0 → Karpenter consolidates nodes away |
| Managed node group | ✅ | `eks update-nodegroup-config --scaling-config minSize=0,desiredSize=0` |
| **Aurora Serverless v2** | ✅ **automatically** | `min_capacity: 0` auto-pauses after the idle timeout. **No cron needed** |
| DynamoDB (on-demand) | ✅ **automatically** | Pay-per-request — idle costs nothing but storage |
| SNS / SQS / Lambda / Athena | ✅ **automatically** | Pay-per-request |
| NAT data processing | ✅ automatically | Falls to near zero with no traffic |
| EKS control plane | ❌ | $73/mo fixed |
| NAT Gateway hourly | ⚠️ technically | Delete/recreate saves ~$24/mo but risks the 1SB-whitelisted EIP association. **Not worth it** |
| ALB | ⚠️ technically | ~$18/mo; delete/recreate churns DNS. Not worth it |
| **ElastiCache** | ❌ | Cannot be stopped, only deleted. Keep it small — this is why Phase 1 uses `t4g.micro` |
| **MSK** | ❌ | Cannot be stopped, only deleted. This is why it is deferred to Phase 3 |
| **Managed Grafana** | ❌ | Per-user, per-month |
| **GuardDuty / Security Hub / Config** | ❌ | Continuous by design — and switching off security monitoring to save $65 is a bad trade |

**The important consequence:** Aurora and DynamoDB — usually the hardest part of environment
scheduling — need no scheduling logic at all here, because of the Serverless v2 `min_capacity: 0`
and on-demand choices made in [02](./02-component-and-sizing-matrix.md). The scheduler only has
to deal with Kubernetes workloads.

---

## Implementation — use `py-kube-downscaler`, not a custom Lambda

The instinct is an EventBridge → Lambda → `kubectl scale` script. Resist it: it has to track
prior replica counts, handle partial failures, and stay in sync as services are added.

**Recommended:**

```
py-kube-downscaler (in-cluster)  →  scales Deployments/StatefulSets to 0
                                      ↓
Karpenter (consolidationPolicy: WhenEmptyOrUnderutilized)  →  removes the now-empty nodes
                                      ↓
EventBridge Scheduler → Lambda  →  managed node group min/desired = 0  (belt and braces)
                                      ↓
Aurora Serverless v2  →  auto-pauses on its own
```

`py-kube-downscaler` is annotation-driven, so the schedule is declared per namespace and travels
with the manifests in git rather than living in a Lambda nobody maintains:

```yaml
# namespace annotation — applies to everything in it
metadata:
  annotations:
    downscaler/uptime: "Mon-Fri 08:30-21:00 Asia/Kolkata,Sat 09:00-14:00 Asia/Kolkata"
```

Exclude the platform namespace so Argo CD, External Secrets, and Karpenter survive the night:

```yaml
metadata:
  annotations:
    downscaler/exclude: "true"
```

Karpenter then removes empty nodes automatically — the node-level saving is a *consequence* of
the pod-level scale-down, not a second thing to orchestrate. The EventBridge → node-group Lambda
is a backstop for the managed node group's floor, which Karpenter does not own.

---

## The override — the part that makes the schedule survivable

A schedule with no escape hatch gets disabled within a fortnight, the first time someone needs an
overnight run. Build the override on day one:

```bash
# Hold the environment up until a specific time — overnight load test,
# joint 1SB testing window, month-end regression
aws ssm put-parameter --name /uat/scheduler/hold-until \
  --value "2026-08-14T06:00:00+05:30" --overwrite
```

Requirements for the override:

1. The down-job reads `/uat/scheduler/hold-until` and **no-ops** if it is in the future.
2. It **expires automatically**. A permanent "keep it up" flag is how UAT quietly becomes 24×7 —
   there is no un-timed override.
3. Exposed as a Slack/Teams command or a one-line script, so testers do not need AWS console
   access to use it.
4. Every use is logged and reviewed in the weekly cost review. Frequent overrides are a signal
   that the *schedule* is wrong, not that people are misbehaving.

---

## Guardrails — three failure modes to design against

| Failure | Consequence | Guardrail |
|---|---|---|
| **The scheduler runs against the wrong account** | A production outage caused by a cost optimisation | Scope the Lambda role to the UAT account only; hard-fail on an account-ID assertion. Never share the role across accounts |
| **The morning start fails silently** | A whole squad blocked at 09:00 | Health check at **08:45**. If workloads are not Ready, page platform on-call. Alert on the *absence* of a successful start, not just on errors |
| **Scale-down during an active test** | Lost test run, lost trust in the schedule | Down-job checks for a `hold-until` and posts a 15-minute warning to the UAT channel before scaling down |

That second one deserves emphasis: **a failed 08:30 start costs more than a month of the savings
it protects.** Monitor the start, not just the stop.

---

## What the schedule is worth

| Phase | 24×7 | Optimised | Saved/mo |
|---|---:|---:|---:|
| Phase 1 | $520 | $327 | **$193** |
| Phase 2 | $996 | $583 | **$413** |
| Phase 3 | $2,080 | $1,400 | **$680** |

~**$8,200/year** at steady state.

---

# Part 2 — Sixteen further levers

Ordered by value. Several are free and permanent, which makes them better than the schedule.

### Free, do in Phase 1

| # | Lever | Worth | Notes |
|---|---|---|---|
| 1 | **S3 Gateway VPC Endpoint** | $20–50/mo | **Free.** ECR image layers are served from S3, so this removes most NAT data processing. The highest ratio of value to effort in this document |
| 2 | **CloudWatch Logs 7-day retention, set at creation** | $50–150/mo by Phase 3 | Log groups default to *never expire*. Enforce via Config rule or IaC aspect. Retrofitting after six months means paying for six months first |
| 3 | **Single NAT Gateway, not per-AZ** | $82/mo | Acceptable risk in a test environment; also gives the single EIP that 1SB whitelists |
| 4 | **ECR lifecycle policies** | $10–25/mo | Untagged expire 3d, keep last 10 tagged. Registries grow without limit otherwise |
| 5 | **`gp3` not `gp2`** | ~20% of EBS | Also decouples IOPS from volume size |
| 6 | **30-day S3 lifecycle in UAT — and never Object Lock** | Compounding | The 7-year Object Lock retention is a *production* control. In UAT it creates undeletable objects and an undeletable bucket |
| 7 | **Right-size CPU requests to 250m** | ~40% of node count | JVM services in UAT are memory-bound. Requesting 500m–1000m halves pod density for no benefit |
| 8 | **No dev AWS environment** | ~$300/mo | `docker-compose.yml` already exists in this repo. Developers work locally through Phase 1 |

### Cheap, do in Phase 1–2

| # | Lever | Worth | Notes |
|---|---|---|---|
| 9 | **Graviton (`m7g`)** | ~20% of compute | Requires multi-arch images — a Phase 1 platform-team task, not an afterthought |
| 10 | **Aurora Serverless v2 `min_capacity: 0`** | ~60% of DB spend | The schedule applies to the database for free. Requires PG 13.15+/14.12+/15.7+/16.3+ |
| 11 | **DynamoDB on-demand, not provisioned** | Most of DDB spend | Zero cost while the environment is off, with no scheduling logic |
| 12 | **Spot-first for everything interruption-tolerant** | ~65% of that compute | See the aggressive option below |
| 13 | **OpenCost for per-namespace showback** | Indirect but large | Free. Squads that can see their own spend right-size without being asked |

### Phase 3 choices that avoid large bills

| # | Lever | Worth | Notes |
|---|---|---|---|
| 14 | **Athena/Glue instead of Redshift Serverless** | ~$2,100/mo avoided | The 8-RPU floor would more than double this environment's entire cost |
| 15 | **Istio ambient mode instead of sidecars** | ~1 node (~$125/mo) | 34 sidecars × 100m/128Mi ≈ 3.4 vCPU / 4.3 GiB of pure overhead |
| 16 | **Provisioned MSK instead of MSK Serverless** | ~$400/mo | $147 vs $547 at UAT volume |

---

## The aggressive option: Spot-first UAT

Worth a decision rather than a default. In **production**, Spot is for interruption-tolerant
workloads only. In **UAT**, that constraint is much weaker — a pod restarting mid-test is an
inconvenience, not an incident, and it doubles as a free chaos test of the pod-level statelessness
the NFRs already require.

Configure a Karpenter NodePool with `capacity-type: ["spot", "on-demand"]`, Spot weighted first,
on-demand as fallback, and Spot-to-Spot consolidation enabled:

| Phase 3 compute | Cost/mo |
|---|---:|
| All on-demand (optimised schedule) | $206 |
| 80% Spot / 20% on-demand | **~$99** |
| **Additional saving** | **~$107/mo** |

**The trade:** occasional pod restarts as Spot capacity is reclaimed. Keep the data-tier-adjacent
and stateful pods (Keycloak, Prometheus) on on-demand; put the stateless services on Spot. If a
UAT sign-off window is running, use the `hold-until` override to pin on-demand capacity for the
duration.

**Recommendation: adopt Spot-first from Phase 2**, once the platform has proven that pods restart
cleanly. Doing it in Phase 1 conflates two new variables while the environment is still being
stabilised.

---

## Cost governance — the operating discipline

Optimisation decays without a routine. Four things, all cheap:

**1. Mandatory cost allocation tags**, enforced by an AWS Tag Policy:

| Tag | Values |
|---|---|
| `Environment` | `uat` |
| `Workstream` | `WS-1` \| `WS-2` \| `platform` |
| `Service` | service name |
| `Owner` | squad or individual |
| `CostCentre` | finance code |
| `Schedule` | `uat-bh` \| `always-on` |

Without these, cost attribution is guesswork and every optimisation conversation stalls on "whose
is that?". `Schedule` in particular is what lets the scheduler be tag-driven rather than
hard-coded to a resource list that goes stale.

**2. AWS Budgets** at 60/85/100% per phase (thresholds in
[03](./03-cost-estimate.md#recommended-budget-lines)), plus **Cost Anomaly Detection**. In an
environment designed to be idle 59% of the time, an anomaly detector is unusually powerful:
anything running at 03:00 on a Sunday is by definition worth a page.

**3. Weekly cost review** — 10 minutes in the platform stand-up. Cost Explorer week-over-week, any
override usage, any anomaly alerts.

**4. Re-baseline at the end of Phase 1.** One month of real Cost Explorer data beats any amount of
further estimating. Update [03](./03-cost-estimate.md) with measured figures before Phase 2 is
approved — that is what makes the Phase 2/3 forecast credible to a sponsor.

---

**Next:** [05-lead-times-and-dependencies.md](./05-lead-times-and-dependencies.md) — what to start in week 0.
