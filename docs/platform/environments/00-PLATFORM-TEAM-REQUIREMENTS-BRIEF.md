# AWS Platform Requirements Brief

**For:** Cloud Platform / Infrastructure Engineering team
**From:** Insurance Distribution Platform — Architecture & Engineering
**Version:** 1.0 · 18 August 2026
**Response requested by:** 25 August 2026
**Classification:** Internal

> **This document assumes no knowledge of our project.** It is written to be read cold. Section 2
> explains what the system does, Section 3 gives you the numbers to size against, and Sections 7–8
> are the actual requirements. Everything carries a stated reason — if a reason does not hold up,
> challenge it.

---

## 1. What we are asking for

We are building a system that lets a bank sell life insurance to its existing customers through
branch staff. The application is written and being tested; **it has nowhere to run.** Today it
exists only as containers on developer laptops and one demo host.

**We need you to build three AWS environments — development, UAT and production — as
infrastructure-as-code, in the Mumbai region, to a production-ready state by January 2027.**

Two things make this request different from a standard "give us an EKS cluster":

1. **The workload is genuinely small, and we want it sized that way.** Roughly two requests per
   second at peak. We are explicitly asking you *not* to build for scale. Section 3.
2. **The regulatory constraints are hard and non-negotiable**, and several of them are conditions
   of the bank's insurance licence rather than internal policy. Section 6.

### What we need back from you

| # | Response | By |
|---|---|---|
| 1 | A line-by-line response to Section 7 — **Accept / Query / Reject** against each requirement ID | 25 Aug 2026 |
| 2 | Your answers to the open questions in Section 12 | 25 Aug 2026 |
| 3 | A costed estimate against the budget envelope in Section 8.4 | 1 Sep 2026 |
| 4 | Named engineers and a start date for Phase 1 | 1 Sep 2026 |

**Please do not start building before the Section 12 questions are settled.** Two of them
(network addressing and bank-network connectivity) cannot be changed later without a rebuild.

---

## 2. What the system does

You do not need insurance domain knowledge, but you do need this much, because it drives nearly
every constraint in Section 7.

### 2.1 The business, in one paragraph

A bank's branch staff sell life insurance policies to people who already bank there. A member of
staff sits with a customer, works out what cover they need, gets the customer's consent, fetches
quotes from several insurers, submits an application, and sends the customer a payment link. Once
the payment clears and is reconciled against the insurer's records, the policy is issued. Because
this is a regulated financial sale, **every step must be evidenced and the evidence must be
tamper-proof for seven years.**

### 2.2 The journey the platform has to run

```
 1. Look up the customer in the bank's core banking system
 2. Assess what cover they need                        ← must exist before any quote is allowed
 3. Capture consent, verified by a one-time password   ← sent to the CUSTOMER's own phone
 4. Fetch quotes from insurers via an aggregator       ← outbound to a third-party SaaS
 5. Submit the application; track underwriting
 6. Send a payment link                                ← to the CUSTOMER's own phone
 7. Customer pays; we reconcile against the settlement file
 8. Policy is issued                                   ← only after reconciliation succeeds
 9. Complete audit evidence is written and sealed
```

### 2.3 The four facts from that journey that shape the infrastructure

| Fact | Infrastructure consequence |
|---|---|
| Steps 4 and 7 call **third parties over the internet** — an insurance aggregator and the bank's payment gateway | We need **one egress path with a fixed, stable public IP address**. The aggregator enforces IP allow-listing and will reject traffic from an unknown address. See PR-2.4 |
| Step 1 reads from the **bank's core banking system**, which sits inside the bank's own data centre | We need **private network connectivity from AWS into the bank network**. This has the longest lead time of anything in this document. See PR-2.7 and Section 11 |
| Steps 3 and 6 send OTPs and payment links to the **customer's personal phone**, and the customer pays on their own device | There is a **public internet-facing surface** that is used by members of the public, not just bank staff. It needs WAF and rate limiting. See PR-2.5 |
| Step 9 writes **evidence that must never be altered or deleted for seven years** | We need **object storage with immutability enforced by the platform**, not by application code. See PR-8.4 |

### 2.4 What we are handing you to run

Eighteen containerised services. All of them are **Java 21 / Spring Boot 3.3**, all stateless at the
process level, all exposing HTTP health endpoints at `/actuator/health/liveness` and
`/actuator/health/readiness`. Images are built by our CI pipeline and pushed to a registry.

| Group | Services | Notes for you |
|---|---|---|
| **Public/staff edge** | Workspace API (session facade for the staff mobile app) | The only service reachable from outside |
| **Identity** | Identity provider adapter · Authorisation service | The **authorisation service is called on every single request** with a 100 ms target and a 300 ms hard budget. If it is slow or cold, the entire platform stops. Treat it as the most latency-sensitive component in the system |
| **Sales journey** | Journey orchestration · Customer · Lead · Consent · Suitability · Product catalogue · Quotation · Proposal | Journey orchestration holds the state machine and is the busiest internal service |
| **Money & policy** | Payment · Policy issuance | Financial path. These have the least tolerance for data loss |
| **Integration** | Integration hub · Aggregator adapter · Persistence service | **All third-party traffic leaves through here and nowhere else.** This is an architectural rule we need enforced at the network layer, not just in code |
| **Platform** | Audit & compliance · Notification | Audit writes the seven-year evidence |

**Data model:** each service owns its own database and **no service may read another service's
database**. We need that enforced by credentials and IAM, not by convention.

**Messaging:** there is **no message broker**. Services publish domain events by writing to an
outbox table in their own database, which is polled and published. **Please do not propose Kafka or
Amazon MSK** — we evaluated it and the pattern we use does not need it. This is a deliberate
decision, not an oversight.

---

## 3. Workload profile — the numbers to size against

These are derived from the pilot rollout plan. They are modest, and that is the single most
important thing for you to internalise before designing anything.

| Measure | Value |
|---|---|
| Staff users (pilot) | **250**, across pilot branches |
| Concurrently active in a business hour | **50** (20% concurrency) |
| Sales journeys started | **100 per hour** ≈ 1.7 per minute |
| Peak (Jan–Mar tax season, ×4 multiplier) | **6.8 journeys per minute** |
| Outbound calls to insurers | 5 per quote ≈ **34 per minute at peak** |
| Business hours | **09:00–19:00 IST, weekdays** |
| Sustained throughput target | ≥ 10 journey starts/minute (peak + 45% headroom) |
| Peak request rate, whole platform | **roughly 2 requests per second** |
| Data volume, first year | Low tens of GB across all databases |

### What this means, stated plainly

**This is a small workload.** Two requests per second is less traffic than a single modest EC2
instance handles comfortably. The system is not throughput-constrained; it is
**correctness-, evidence- and recovery-constrained.**

**We are therefore asking you to optimise for cost and for provable recovery, not for scale.** If a
design decision trades headroom for cost, take the cost saving — provided it does not touch anything
in Section 6. If you find yourself specifying a multi-node cluster of anything, please check it
against these numbers first.

### Latency targets (measured server-side, 95th percentile)

| Operation | Target |
|---|---|
| Authorisation decision (every request) | **< 100 ms**, hard budget 300 ms, no retry, fails closed |
| Staff app screen reads | < 300 ms |
| Quote acknowledgement | < 600 ms |
| **Quote results returned end to end** | **< 5 seconds** — beyond this the sales conversation breaks down |
| Payment link creation (includes gateway round trip) | < 2.5 s |

### Availability and recovery targets

| Target | Value | Applies to |
|---|---|---|
| Availability | **99.9% monthly** (≈43 min/month) | Production only. Sales path services |
| Availability | 99.5% | Production. Catalogue and advisory services |
| **Recovery Time Objective** | **≤ 1 hour** | Production transactional data |
| **Recovery Point Objective** | **≤ 5 minutes** | Production transactional data |
| **Recovery Point Objective** | **Zero** | Audit evidence and raw third-party payloads — no loss window is acceptable |

UAT and development carry **no availability target at all**. This is deliberate and is what makes
the cost optimisation in Section 8 possible.

---

## 4. Environments requested

| Environment | Purpose | Availability expectation | Lifecycle we want |
|---|---|---|---|
| **Development** | Integration testing, throwaway experiments | None | **Destroyed and rebuilt weekly from code.** Created Monday morning, destroyed Friday evening |
| **UAT** | Business acceptance testing, security testing, load testing, all pre-production proof | None — business hours only | Infrastructure persists; **workloads scale to zero outside 08:30–20:30 IST on weekdays** |
| **Production** | Live pilot | 99.9%, multi-AZ, with disaster recovery | Always on. **No cost optimisation that affects resilience** |

**Development being ephemeral is intentional and is not a cost hack.** Rebuilding it weekly is
cheaper than leaving it running, and it continuously proves that the environment really can be
recreated from code — which is one of our acceptance criteria (Section 9). We would rather discover
that the code no longer builds a working environment on a Monday morning in September than during a
production incident in February.

---

## 5. Timeline and phases

Six phases. Dates assume a start on **24 August 2026**; every week of delay to mobilisation moves
the whole schedule one-for-one.

| Phase | Dates | What you deliver | Why this order |
|---|---|---|---|
| **1 — Landing zone** | 24 Aug – 4 Sep | AWS accounts, organisational guardrails, region and tagging policy, infrastructure-as-code pipeline with policy checks, budgets | Make it impossible to build the wrong thing before anything is built |
| **2 — UAT skeleton** | 7 – 25 Sep | Network, Kubernetes cluster, first five services deployed by the pipeline, reachable and observable | The narrowest vertical cut through the whole platform. Proves the pipeline, not the product |
| **3 — Data & secrets** | 28 Sep – 16 Oct | Databases, object storage, encryption keys, secrets management. **A backup restored and timed** | Nothing else can be trusted until a restore has actually been performed |
| **4 — Full journey** | 19 Oct – 13 Nov | Public edge with WAF, observability stack, network segmentation, all remaining services | The complete business journey runs in UAT for the first time |
| **5 — Proof** | 16 Nov – 4 Dec | Rollback drill, high-availability drill, disaster-recovery restore drill, data-residency attestation, credential rotation | This phase produces **evidence**, not features. It is the phase most often cut and the one we least want cut |
| **6 — Production** | 7 Dec – 8 Jan | Production environment built from the same code with one parameter change; disaster recovery standby; load testing; cutover rehearsal | Production should be boring by this point |
| **Go-live** | 11 – 29 Jan 2027 | Pilot launch | |

---

## 6. Non-negotiable constraints

Each of these is either a condition of the bank's insurance licence or a regulatory obligation. They
are not open to trade-off against cost, schedule or convenience. **If any requirement in Section 7
appears to conflict with one of these, the constraint wins and we need to talk.**

| # | Constraint | Why | If breached |
|---|---|---|---|
| **NN-1** | **All data, backups, logs and archives stay in AWS India regions.** Mumbai (`ap-south-1`) primary, Hyderabad (`ap-south-2`) for disaster recovery. No exceptions, including for logs and monitoring | Indian regulatory data-residency requirement; a licence condition | Regulatory breach. Not a fixable defect |
| **NN-2** | **Audit evidence is retained for seven years and cannot be altered or deleted** — by anyone, including administrators, including us | Insurance regulator's record-keeping requirement | Regulatory breach; the bank cannot evidence a sale it made |
| **NN-3** | **Encryption at rest everywhere using customer-managed keys** we control, and TLS in transit | Financial data protection; bank security policy | Security and audit finding |
| **NN-4** | **No production data in development or UAT — enforced technically, not by policy** | Personal data protection legislation | Data-protection breach |
| **NN-5** | **All services run in private subnets.** The only public entry point is the edge load balancer | Bank network security standard | Security finding, likely blocking go-live |
| **NN-6** | **No long-lived static credentials inside workloads.** Services authenticate to AWS by role | Bank credential-management policy | Security finding |

We will need a **signed attestation** for NN-1 at the end of Phase 5 — an enumeration of every
resource, backup, log destination and archive proving each is in an India region. Please build with
that evidence in mind rather than reconstructing it afterwards.

---

## 7. Requirements

Each requirement has an ID for your line-by-line response. **Priority:** `MUST` = we cannot go live
without it. `SHOULD` = we want it and will negotiate.

Where a requirement differs by environment, three values are given as **dev / UAT / production**.

### 7.1 Accounts and governance

| ID | Requirement | Level (dev / UAT / prod) | Why | Pri |
|---|---|---|---|---|
| **PR-1.1** | Separate AWS accounts per environment, under one organisation | 3 accounts + shared services + security + management | Production must be a hard account boundary, not a naming convention. It is what makes NN-4 provable | MUST |
| **PR-1.2** | Service control policy denying every region except Mumbai, Hyderabad and global services | org-wide | NN-1, enforced above anything an engineer can misconfigure | MUST |
| **PR-1.3** | Service control policy denying resource creation without tags: environment, service, owner, cost-centre, data-classification | org-wide | Cost attribution and data-classification tracking both become impossible retrospectively | MUST |
| **PR-1.4** | Organisation-wide audit trail of all API activity, written to a dedicated account, immutable | org-wide | Bank audit requirement; also our forensic record | MUST |
| **PR-1.5** | Production account created in Phase 6, not Phase 1 | — | An empty governed account still bills for compliance tooling. No reason to pay from August | SHOULD |
| **PR-1.6** | All infrastructure defined as code. **No resource created by hand in any environment** | all | Non-negotiable for us: we must be able to rebuild, and audit what exists | MUST |
| **PR-1.7** | Nightly drift detection comparing live infrastructure to code, alerting on divergence | all | A hand-edited resource is invisible until it breaks a rebuild | MUST |
| **PR-1.8** | Automated policy checks in the pipeline blocking: non-India regions, public storage, unencrypted storage, wildcard production permissions, missing immutability on audit storage — **before apply, not after** | all | Reviewing a plan that policy will reject anyway wastes the reviewer's attention | MUST |

### 7.2 Network

| ID | Requirement | Level (dev / UAT / prod) | Why | Pri |
|---|---|---|---|---|
| **PR-2.1** | One isolated virtual network per environment, non-overlapping address ranges, coordinated with the bank's existing on-premises ranges | all | We will connect to the bank network (PR-2.7). Overlapping ranges cannot be fixed later without a rebuild |  MUST |
| **PR-2.2** | Three tiers of private subnet: public (load balancer and outbound gateway only), application, data | all | NN-5 |  MUST |
| **PR-2.3** | Availability zones: 2 / 3 provisioned but 1 actively used / 3 | dev 2 · UAT 3-provisioned-1-used · prod 3 | UAT has no availability target. Subnets in three zones cost nothing; running workloads in three does. See Section 8 |  MUST |
| **PR-2.4** | **A single outbound path with a static public IP address** | 1 shared / 1 / 3 (one per zone) | The insurance aggregator enforces IP allow-listing. **We need the UAT address allocated in week one of Phase 2 and given to us the same day** — registering it with the third party takes 3–6 weeks and blocks Phase 3 |  MUST |
| **PR-2.5** | Public edge: web application firewall with managed rule sets, rate limiting, and a content delivery layer for the customer-facing pages | none / full / full | Members of the public reach the payment and OTP pages. The UAT rules must be identical to production or we are not testing them |  MUST |
| **PR-2.6** | **A single shared load balancer for all services**, not one per service | all | Eighteen load balancers would cost roughly twenty times one, for no benefit at 2 requests/second |  MUST |
| **PR-2.7** | **Private connectivity from AWS to the bank data centre** for core-banking lookups. A site-to-site VPN is sufficient for UAT unless the bank's network team mandates otherwise | none (stubbed) / yes / yes | Step 1 of the journey. **This is the longest lead item in the entire programme — 6 to 10 weeks. Please raise it with the bank network team in week one, before anything else** |  MUST |
| **PR-2.8** | Default-deny network policy between service groups; explicit allows only where the architecture requires them | all | Sales and money services must have **no route to the internet at all** — all third-party traffic goes through the integration services. This must be true at the network layer, not just in code |  MUST |
| **PR-2.9** | Free-of-charge private routes to object and key-value storage | all | Saves data-processing charges on the outbound gateway for no effort |  SHOULD |
| **PR-2.10** | Network traffic logging: off / 10% sample / 100% | dev off · UAT 10% · prod 100% | Full flow logging in UAT costs money and answers no question we are asking there |  SHOULD |

### 7.3 Compute

| ID | Requirement | Level (dev / UAT / prod) | Why | Pri |
|---|---|---|---|---|
| **PR-3.1** | Managed Kubernetes, one cluster per environment | all | Chosen platform; the application is containerised and the team is Kubernetes-literate |  MUST |
| **PR-3.2** | Cluster API endpoint private, with allow-listed access for the deployment pipeline; **private-only in production** | all | NN-5 |  MUST |
| **PR-3.3** | **ARM-based instances** for all workloads | all | Roughly 20% better price/performance. Our images build for ARM — we will confirm this in week one so it is not discovered late |  SHOULD |
| **PR-3.4** | **Spot capacity for all UAT and development workloads**; on-demand for the production sales path, spot for production batch work | spot / spot / mixed | Interruption is irrelevant in UAT and unacceptable on the sales path |  SHOULD |
| **PR-3.5** | Automatic node provisioning that right-sizes and consolidates, rather than fixed node groups | all | At this workload size, a fixed node group is either oversized all week or undersized at peak |  SHOULD |
| **PR-3.6** | Replicas per service: 1 / 1 / **at least 2, spread across at least 2 availability zones** | 1 / 1 / ≥2 | The two-replica rule is an availability requirement and applies **only to production** |  MUST |
| **PR-3.7** | Disruption budgets guaranteeing at least one replica stays available during node rotation | none / none / yes | Otherwise routine node maintenance breaches the 99.9% target |  MUST |
| **PR-3.8** | Minimal, hardened container host OS; containers run non-root with read-only filesystems, no privileged containers | all | Bank container security standard |  MUST |
| **PR-3.9** | Admission control blocking unsigned images, `latest` tags, and workloads without resource limits. Audit mode in UAT, enforced in production | audit / audit / enforce | A resource-limit-free pod can starve the authorisation service, which fails the whole platform closed |  MUST |
| **PR-3.10** | **Scheduled scale-to-zero** for UAT workloads outside 08:30–20:30 IST, Monday–Friday | no / **yes** / never | Removes roughly 70% of UAT compute hours. See Section 8 |  MUST |
| **PR-3.11** | Sizing guidance: total UAT working set is approximately **4 vCPU and 15 GiB of memory requested** across all 18 services | — | So you can sanity-check any node proposal against it |  — |

> **One sizing note we would ask you to honour specifically.** The **authorisation service** is
> called on every request in the system, has a 300 ms hard budget, does not retry, and **denies
> access when it cannot answer in time**. A cold-started or CPU-throttled instance of it does not
> degrade the platform — it stops it. In production please keep at least two instances warm at all
> times and give it the same resource tier as the busiest service, regardless of what its average
> utilisation suggests.

### 7.4 Data

| ID | Requirement | Level (dev / UAT / prod) | Why | Pri |
|---|---|---|---|---|
| **PR-4.1** | Managed PostgreSQL, version 16, **auto-scaling serverless capacity** | all | Capacity tracks a workload that is idle 70% of the week |  MUST |
| **PR-4.2** | **One database cluster per environment**, with **one logical database and one separate credential per service** | all | Our rule is *no service may read another service's database*. Separate credentials with scoped grants enforce that. Eighteen separate clusters would cost roughly eight times more and enforce nothing additional |  MUST |
| **PR-4.3** | Capacity floor **zero (pause when idle)** in UAT and development; **minimum one unit** in production | 0 / 0 / 1 | UAT resumes in about 15 seconds, which nobody notices. Production cannot afford a cold start |  MUST |
| **PR-4.4** | Single availability zone in UAT and development; **multi-AZ with a standby in production** | single / single / multi | UAT has no availability target |  MUST |
| **PR-4.5** | Services authenticate to the database **by IAM role, not password** | all | NN-6. Removes an entire credential class |  MUST |
| **PR-4.6** | Backup retention and point-in-time recovery: 1 day / 7 days / **35 days** | 1 / 7 / 35 | Production retention is set by the bank's recovery policy |  MUST |
| **PR-4.7** | Managed key-value store for journey state and job tracking, on-demand pricing, point-in-time recovery enabled | all | On-demand pricing at 1.7 journeys/minute costs a few dollars and needs no capacity planning |  MUST |
| **PR-4.8** | Connection-budget check: total connections across all services at maximum scale must stay **below 60% of the database limit** | all | Scaling pods must never be able to collapse the database. Please compute and share this figure before Phase 4 |  MUST |
| **PR-4.9** | **No managed cache tier** in the first release | — | Our idempotency requirement is served by the key-value store and caching is in-process. A managed cache has a ~$90/month floor for a workload doing 1.7 journeys/minute. Revisit when we have evidence of cache pressure |  SHOULD |

### 7.5 Security, keys and secrets

| ID | Requirement | Level | Why | Pri |
|---|---|---|---|---|
| **PR-5.1** | Customer-managed encryption keys, **organised by data class rather than per service** — five keys: personal data, financial data, immutable audit, secrets, logs | all | NN-3. Five keys instead of eighteen, with no loss of separation |  MUST |
| **PR-5.2** | Separate key sets per environment. A UAT key must not be able to decrypt production data | all | Half the answer to NN-4 |  MUST |
| **PR-5.3** | Automatic annual key rotation | all | Bank policy |  MUST |
| **PR-5.4** | Managed secrets store, **one consolidated secret per service** rather than one per credential | all | Same isolation, roughly a quarter of the cost |  MUST |
| **PR-5.5** | Non-secret configuration in a separate parameter store, not the secrets store | all | The secrets store charges per secret; configuration does not need to live there |  SHOULD |
| **PR-5.6** | **One IAM role per service**, assumed by the workload identity, least-privilege, **no wildcards in production policies** | all | NN-6 |  MUST |
| **PR-5.7** | **The audit service must be denied delete permissions on audit storage at both the permission and storage-policy layer** | all | NN-2. One control the application can bypass is not a control. We want two independent ones |  MUST |
| **PR-5.8** | Credential rotation exercised at least once in UAT with no service outage, before go-live | UAT | An unrotated credential is an untested procedure |  MUST |
| **PR-5.9** | Emergency revocation procedure for a third-party credential, exercised once | UAT | If the aggregator's key leaks, we need to know we can pull it under incident conditions |  MUST |
| **PR-5.10** | Threat detection and container runtime monitoring enabled in all accounts | all | Bank security standard |  MUST |
| **PR-5.11** | Image vulnerability scanning on push; pipeline blocks on critical findings | all | Supply-chain control |  MUST |

### 7.6 Observability

| ID | Requirement | Level (dev / UAT / prod) | Why | Pri |
|---|---|---|---|---|
| **PR-6.1** | Metrics collection from every service, queryable, with dashboards showing request rate, errors, latency and saturation per service | all | Baseline operability |  MUST |
| **PR-6.2** | Centralised, searchable logs with **verified masking of personal data** | all | We must be able to prove **zero** personal-data matches in the aggregated log store after a full test run. This is tested, not assumed |  MUST |
| **PR-6.3** | Log retention: none / 14 days / 30 days | — / 14 / 30 | Operational logs are not the audit record and must not be confused with it |  MUST |
| **PR-6.4** | Distributed tracing across every service hop and third-party call, with the trace identifier correlated into log lines. Sampling 5% in UAT, 10% in production, **100% on error** | all | A quote fans out to five insurers; without tracing, "the quote was slow" is unanswerable |  MUST |
| **PR-6.5** | **The audit evidence pipeline must be entirely separate from operational logging** | all | Operational logs are retained 14 days and are mutable; audit evidence is retained seven years and is not. Mixing them contaminates both. NN-2 |  MUST |
| **PR-6.6** | Alert routing with a runbook link in every alert payload; on-call escalation in production only | all | An alert without a runbook is a notification |  MUST |
| **PR-6.7** | Self-hosted dashboards acceptable in UAT; managed dashboarding in production only | OSS / OSS / managed | Per-user dashboard licensing in UAT buys an audit trail nobody is auditing |  SHOULD |

### 7.7 Deployment and rollback

| ID | Requirement | Level | Why | Pri |
|---|---|---|---|---|
| **PR-7.1** | Fully automated deployment to every environment, no manual steps, full audit trail of who deployed what | all | Bank change-control requirement |  MUST |
| **PR-7.2** | **Build once, promote the same artefact.** The image that passed UAT is byte-for-byte the image that runs in production — never rebuilt per environment | all | Otherwise UAT tested something that no longer exists |  MUST |
| **PR-7.3** | Configuration external to the image; per-environment values versioned and reviewed | all | Same image, different environment |  MUST |
| **PR-7.4** | **Automated rollback, executed and timed in UAT as an acceptance condition** | UAT proof | An untested rollback is not a rollback. We want a deliberately broken version deployed and rolled back, with the time recorded |  MUST |
| **PR-7.5** | Database migrations in the deployment path, safe with the previous version still serving traffic | all | Makes both rolling deployment and rollback survivable |  MUST |
| **PR-7.6** | Production deployment requires recorded approval from two reviewers; an audited emergency path exists | prod | Bank change-control requirement |  MUST |
| **PR-7.7** | Progressive rollout for the sales-path services in production; rolling updates elsewhere | prod | Limits blast radius where a bad release costs a sale |  SHOULD |
| **PR-7.8** | Deployment events correlated onto monitoring dashboards | all | A bad release should be visible immediately, not diagnosed |  SHOULD |

### 7.8 Backup, retention and disaster recovery

| ID | Requirement | Level | Why | Pri |
|---|---|---|---|---|
| **PR-8.1** | Automated backup of every data store, meeting a 5-minute recovery point in production | all | Recovery target |  MUST |
| **PR-8.2** | **A restore actually performed to a working state and timed against the 1-hour recovery target** — in UAT, before go-live | UAT proof | *A backup that has never been restored is a hypothesis.* This is our single most important acceptance criterion |  MUST |
| **PR-8.3** | Object storage for raw third-party payloads, policy documents and audit evidence — private, encrypted, versioned | all | Evidence store |  MUST |
| **PR-8.4** | **Immutability enforced on audit storage.** Retention horizon: **1 day in UAT, seven years in production** | 1 day / 1 day / **7 years** | NN-2. A one-day lock **refuses deletion identically** to a seven-year one, so the mechanism is fully proven — but UAT test data does not become undeletable until 2034. The seven-year horizon is a production obligation and is set there |  MUST |
| **PR-8.5** | A deletion attempt against immutable storage must be **refused**, demonstrated as a test | UAT proof | Proving the control, not the configuration |  MUST |
| **PR-8.6** | Retention purge: data past its horizon disposed of, **with an audit record of the disposal** | all | Retention is a two-sided obligation — keeping data too long is also a breach |  MUST |
| **PR-8.7** | Cross-region replication of audit evidence and raw payloads to Hyderabad, production only | prod | Zero recovery point for audit data (Section 3) |  MUST |
| **PR-8.8** | Database and key-value replication to Hyderabad, production only | prod | 5-minute recovery point |  MUST |
| **PR-8.9** | **A disaster-recovery restore drill executed in UAT into the second region, timed, then torn down** | UAT proof | We want the runbook proven without paying to keep a standby running for five months |  MUST |
| **PR-8.10** | A warm standby in Hyderabad — **production only, built in Phase 6** | prod | The only standing standby in the programme |  MUST |

### 7.9 Cost and optimisation

See Section 8 for the full mandate. These are the enforceable requirements.

| ID | Requirement | Level | Why | Pri |
|---|---|---|---|---|
| **PR-9.1** | Per-account monthly budgets with alerts at 50%, 80% and 100% | all | Early warning |  MUST |
| **PR-9.2** | A budget action that **automatically scales down non-production compute at 120%** of budget. Production budgets alert but never act automatically | all | Runaway non-production spend should stop itself |  MUST |
| **PR-9.3** | **Estimated cost impact posted automatically on every infrastructure change request**, before merge | all | A change that adds $500/month should say so at review time, not at month end |  MUST |
| **PR-9.4** | Cost anomaly detection with alerting | all | Catches the mistakes budgets miss |  SHOULD |
| **PR-9.5** | **A weekly automated check that UAT actually scaled to zero and development was actually destroyed** | all | A scale-down that silently stops working is the classic way an optimised environment quietly reverts to full price. We want this monitored, not trusted |  MUST |
| **PR-9.6** | **A policy check asserting that production cannot inherit any non-production cost optimisation** — see Section 8.3 | all | The single most important control in this document. Elaborated below |  MUST |
| **PR-9.7** | No multi-year commitments or reserved capacity purchased until the production baseline has been **measured**, in Phase 6 | prod | Committing against a forecast rather than a measurement is how organisations buy the wrong shape of capacity for a year |  MUST |

### 7.10 Access and ways of working

| ID | Requirement | Level | Why | Pri |
|---|---|---|---|---|
| **PR-10.1** | **No bastion hosts.** Administrative access via session-managed access with full session logging | all | Cheaper, and a bastion is an always-on public target |  MUST |
| **PR-10.2** | Break-glass administrative role with alerting on every use | all | Emergency access that is not alerted is just access |  MUST |
| **PR-10.3** | Infrastructure code in a **separate repository** from application code, with separate approvers | all | Different blast radius, different reviewers, different change cadence |  SHOULD |
| **PR-10.4** | Read access for our engineers to all non-production environments; read-only monitoring access to production | all | We operate the application; you operate the platform |  MUST |
| **PR-10.5** | Runbooks for: environment rebuild, restore, rollback, credential rotation, disaster-recovery failover | all | Handed over as deliverables, not as tribal knowledge |  MUST |

---

## 8. Optimisation mandate

This section tells you **how hard to optimise, where, and where absolutely not**. Please treat it as
a requirement rather than as advice — the level is different per environment by design.

### 8.1 The three levels

| Environment | Optimisation level | Plain instruction |
|---|---|---|
| **Development** | **Maximum** | Optimise aggressively. Nothing here matters beyond "does it work". Destroy it when unused |
| **UAT** | **Aggressive, with one exception** | Optimise everything except the things being *tested*. The public edge rules, the deployment path and the security controls must match production exactly, because that is what UAT exists to test. Everything else — availability, redundancy, capacity, retention — should be minimal |
| **Production** | **None on any resilience axis** | Optimise the *shape* of production (right-sizing, ARM, committed-use discounts once measured). **Do not optimise availability, redundancy, backup retention, encryption, logging or disaster recovery.** If a saving touches any of those, do not take it |

### 8.2 The levers we have already identified

We have modelled these. You are welcome to challenge any of them, and to add to the list — the
numbers below are our estimates and yours will be better.

| Lever | Where | Estimated monthly saving (UAT) |
|---|---|---|
| No message broker (outbox pattern instead) | all | **~$450** |
| One auto-scaling database cluster that pauses when idle, instead of many always-on clusters | dev, UAT | **~$3,150** |
| Scale workloads to zero outside business hours | dev, UAT | ~$90 |
| One shared load balancer instead of one per service | all | ~$320 |
| ARM instances on spot capacity | dev, UAT | ~$140 |
| One outbound gateway instead of one per zone | dev, UAT | ~$100 |
| Self-hosted logging and dashboards instead of fully managed | dev, UAT | ~$60 |
| Development destroyed and rebuilt weekly instead of always on | dev | ~$130 |
| No managed cache tier | all | ~$90 |
| No API gateway layer (edge firewall covers rate limiting) | all | ~$20 |
| Consolidated secrets and keys by data class | all | ~$30 |
| Short immutability horizon in non-production | dev, UAT | compounding storage |

**Estimated result: roughly $310 per month for UAT, against roughly $4,700 for a UAT built as a
faithful mirror of production.** Both satisfy the architecture. Only one is proportionate to two
requests per second.

### 8.3 The guardrail that matters most — PR-9.6

Every optimisation above is expressed as a **parameter** in the infrastructure code, never as a
separate copy of it. All three environments must be built from the same modules with different
parameter files. We are asking for this for two reasons — one is that it is good practice, and the
other is that it is one of our acceptance criteria.

**On top of that, we want an automated policy check that refuses to apply a production configuration
unless:**

- multi-AZ is enabled on the database
- cross-region replication is enabled
- the immutability horizon is at least seven years
- the minimum replica count is at least two
- disruption budgets require at least one available replica
- scale-to-zero is disabled
- the database capacity floor is above zero

**This makes cost optimisation structurally incapable of reaching production.** It is worth more
than any written standard, and it is the requirement we would least like to see negotiated away.

### 8.4 Budget envelope

Our own modelling, at list price for the Mumbai region, before any enterprise discount the bank may
already hold. **We are asking you to validate these, not to accept them** — treat them as the shape
of the answer.

| Period | Environments | Modelled | **Ask, including 25% contingency** |
|---|---|---|---|
| Sep 2026 – Jan 2027 (build) | dev + UAT + shared | ~$450/month | **~$560/month** |
| Feb 2027 onward (live) | + production + disaster recovery | ~$1,915/month | **~$2,400/month** |

The largest single uncertainty is **PR-2.7, bank-network connectivity**. If the bank mandates a
dedicated circuit rather than a VPN, that alone could add several hundred dollars a month and a
significant lead time. Please price both options.

---

## 9. Acceptance criteria

This is how we will judge the platform complete. Each is a **demonstration**, not a document —
please plan the evidence in rather than reconstructing it at the end.

| # | We will ask you to | Pass condition |
|---|---|---|
| **A1** | Destroy a non-production environment entirely and recreate it from code | Identical, working environment. No manual step |
| **A2** | Attempt to create a resource outside the India regions | Blocked by policy |
| **A3** | Deploy through the pipeline with no manual intervention | Succeeds, fully audited |
| **A4** | **Deploy a deliberately broken version to UAT and roll it back** | Previous version restored, data intact, time recorded |
| **A5** | Scan all images and code history for embedded credentials | Zero found |
| **A6** | Rotate a credential in UAT while services are running | Services continue, no outage |
| **A7** | **Restore each data store from backup to a working state** | Recovery time and recovery point met and measured |
| **A8** | Attempt to delete an object from immutable audit storage | **Deletion refused** |
| **A9** | Follow one request end to end through the system | Metrics, logs and a trace all present and correlated |
| **A10** | Query the log store for personal-data patterns after a full test run | **Zero matches** |
| **A11** | Attempt a network connection that policy forbids | Refused |
| **A12** | Enumerate every resource, backup, log destination and archive | **100% in India regions** — signed attestation |
| **A13** | Attempt to reach production data from a development environment | Refused at the infrastructure layer |
| **A14** | Execute a disaster-recovery restore into the second region | Recovery target met, time recorded |
| **A15** | Show that a production configuration missing any resilience setting fails to apply | Blocked by policy check (PR-9.6) |

---

## 10. Division of responsibility

| We provide | You provide |
|---|---|
| Container images built and pushed by our pipeline, ARM-compatible | Registry, cluster, and the deployment mechanism |
| Application configuration values and secrets content | Configuration and secrets **infrastructure**, and the delivery of both into workloads |
| Health check endpoints, metrics, structured logs, trace instrumentation | The collection, storage, dashboards and alerting |
| Database schemas and migrations | The database platform, backup, restore and replication |
| The list of third-party endpoints we call | Outbound path, fixed IP address, and its registration with the bank network team |
| Load-test scripts and the target numbers | The environment to run them against and capacity to support them |
| Business acceptance testers and their schedule | The UAT environment being available during business hours |

---

## 11. Dependencies and lead times

Ordered by risk to the January date. **The first two are not engineering tasks and cannot be
accelerated by working harder.**

| # | Dependency | Needed by | Lead time | Please act |
|---|---|---|---|---|
| **D1** | **Private connectivity from AWS to the bank data centre** (PR-2.7) | 19 Oct 2026 | **6–10 weeks** | **Week one.** This is the critical path. If it is not moving by end of September, January is not recoverable |
| **D2** | AWS account creation and budget approval through bank procurement | 24 Aug 2026 | 2–4 weeks | Immediately — everything else waits behind it |
| **D3** | **Outbound IP address registered with the insurance aggregator** | 28 Sep 2026 | 3–6 weeks | Allocate the address in week one of Phase 2 and send it to us the same day |
| **D4** | Payment gateway sandbox credentials and callback URL allow-listing | 19 Oct 2026 | 4–6 weeks | We are chasing this; you will need to supply the callback address |
| **D5** | Confirmed non-overlapping network address ranges from the bank network team | **Before any network is built** | 1–2 weeks | **Before Phase 1 apply.** This cannot be changed later without a rebuild |
| **D6** | DNS subdomain delegation from the bank's DNS | Phase 2 | 1–2 weeks | |

---

## 12. Open questions for you

Please answer these in your Phase 1 response. The first two block the build.

| # | Question | Why it blocks |
|---|---|---|
| **Q1** | What address ranges may we use, and what does the bank already use on-premises? | A virtual network's address range cannot be changed after creation |
| **Q2** | Site-to-site VPN or dedicated circuit for bank connectivity — which will the network team accept, and what is the realistic lead time for each? | Determines both the schedule and a material part of the budget |
| **Q3** | Does the bank hold an existing AWS enterprise agreement or discount we should price against? | Our estimates are list price and may be 5–20% high |
| **Q4** | Is there an existing landing zone, account structure or set of mandatory guardrails we must adopt rather than build? | We would rather inherit yours than impose ours |
| **Q5** | Which team operates production out of hours, and what is the escalation path? | Determines the alerting design in PR-6.6 |
| **Q6** | Are there mandated tooling standards — infrastructure-as-code language, deployment tooling, monitoring platform — that override our proposals? | We have preferences, not requirements, on tooling |
| **Q7** | Is enhanced DDoS protection required by the bank's risk position? It is roughly $3,000/month and we have not budgeted it | Material budget line; needs a security decision, not a default |
| **Q8** | What is the bank's approved process for the signed data-residency attestation (A12)? | We need to know the format before we generate the evidence |

---

## 13. Glossary

Terms used above that are specific to this system rather than to AWS.

| Term | Meaning |
|---|---|
| **Aggregator** | A third-party SaaS platform that fronts multiple insurance companies behind one API. We call it over the internet; it enforces IP allow-listing |
| **Core banking system** | The bank's system of record for customers and accounts. Lives in the bank's own data centre |
| **Relationship manager** | Bank branch staff member who conducts the sale. The primary user of the system |
| **Journey** | One end-to-end sale attempt, from customer lookup to policy issued. The unit we measure volume in |
| **Quote** | A price from an insurer for a given customer and cover. One quote request fans out to five insurers |
| **Proposal** | The formal insurance application submitted to the insurer after a quote is accepted |
| **Reconciliation** | Matching a customer's payment against the payment gateway's settlement file. A policy is only issued after this succeeds |
| **Suitability assessment** | A regulated needs assessment. No quote may be produced without a valid one — this is enforced in the application, not the platform |
| **Consent** | Customer permission, captured with a one-time password sent to the customer's own phone. Once recorded it cannot be altered |
| **Audit evidence** | The immutable seven-year record of every regulated action. The reason for most of Section 8 |
| **Outbox pattern** | Services record events in a table in their own database within the same transaction as the business change; a poller publishes them. Gives us reliable event delivery without a message broker |
| **Pilot / first release** | 250 staff, one product type, one insurer group, existing bank customers only. The scope everything here is sized for |

---

**Prepared by:** Insurance Distribution Platform — Architecture & Engineering
**Contact for questions on this document:** *(add before circulating)*
**Document status:** Draft for platform team review. Not yet approved by bank security or compliance.
