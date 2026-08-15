# S09 — Platform & Environment Foundation

**AIGEM stage:** L4 — Foundation · **Owner:** Shivanshi (SRE) + Deepali (Security)
**Central question:** *Can we run, observe and recover it safely?*

> **This stage is missing in this repository.** The architecture specifies AWS EKS, Aurora,
> DynamoDB, KMS and India-region residency. What exists is a `render.yaml` running two JVMs in one
> `starter`-plan container.

---

## 1. Purpose

Build the platform the application runs on: infrastructure as code, real environments, deployment
and rollback, secrets, observability, backup and recovery.

S08 makes code **provable**. S09 makes it **runnable**. Both are L4 Foundation, and collapsing
them into one stage — as most lifecycle models do — is how a programme ends up with neither, since
whichever is less visible loses to feature pressure.

For a regulated Indian financial platform this stage also carries **non-waivable regulatory
obligations**: data residency, 7-year immutable retention, and encryption with owned keys. Those
are not operational preferences; they are conditions of the licence.

## 2. Entry criteria

- [ ] GATE-S07 passed: architecture, security architecture, data architecture, NFR targets
- [ ] Cloud account structure and budget approved
- [ ] S08 in progress — the deployment pipeline extends the CI pipeline

## 3. Epics and stories

### S09-E01 — Infrastructure as code · *Shivanshi*

| ID | Story | Acceptance criteria |
|---|---|---|
| S09-E01-S01 | Establish the IaC repository and module standard | Terraform (or equivalent) with versioned, reviewed modules; no console-created production resource |
| S09-E01-S02 | Define remote state and locking | State stored securely, encrypted, locked against concurrent apply |
| S09-E01-S03 | Build the network foundation | VPC, subnets across AZs, routing, NAT, security groups — per the S07 trust boundary model |
| S09-E01-S04 | Build the compute foundation | EKS cluster (or approved alternative) with node management and workload isolation |
| S09-E01-S05 | Build the data foundation | Aurora PostgreSQL Multi-AZ, DynamoDB tables, S3 buckets — encrypted, private, least-privilege |
| S09-E01-S06 | **Enforce region pinning to India** | Every resource in `ap-south-1`; DR in `ap-south-2`; a policy check fails any other region |
| S09-E01-S07 | IaC scanning in the pipeline | Policy-as-code blocks public exposure, unencrypted stores and over-broad IAM before apply |
| S09-E01-S08 | Define drift detection | Scheduled comparison of actual state to code; drift alerts and is reconciled, not tolerated |

### S09-E02 — Environments · *Shivanshi*

| ID | Story | Acceptance criteria |
|---|---|---|
| S09-E02-S01 | Provision dev, UAT and production | Each from the same IaC modules with different parameters — not hand-built variants |
| S09-E02-S02 | Define the promotion model | An artefact built once is promoted; never rebuilt per environment |
| S09-E02-S03 | Enforce environment isolation | Separate accounts or equivalent; no shared credentials; no network path from lower to production data |
| S09-E02-S04 | Define environment configuration management | Config external to the image; per-environment values versioned and reviewed |
| S09-E02-S05 | Define ephemeral environments for testing | Created per pipeline run or per feature branch, destroyed after |
| S09-E02-S06 | **Prohibit production data in lower environments** | Technically enforced, not policy-only |

### S09-E03 — Deployment and rollback · *Shivanshi + Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S09-E03-S01 | Automated deployment to every environment | Single pipeline, no manual steps, full audit trail of who deployed what |
| S09-E03-S02 | Progressive delivery | Rolling, blue/green or canary — chosen per service and justified |
| S09-E03-S03 | **Automated, tested rollback** | Rollback executed successfully in UAT as an acceptance condition, not a documented intention |
| S09-E03-S04 | Database migration in the deployment path | Ordered, reversible or forward-fixable, safe under a rolling deploy with mixed versions live |
| S09-E03-S05 | Deployment gating | Production deploy requires the required approvals recorded; emergency path exists and is audited |
| S09-E03-S06 | Deployment observability | Deploy events correlate to metric changes so a bad release is visible immediately |

### S09-E04 — Secrets and key management · *Deepali + Shivanshi*

| ID | Story | Acceptance criteria |
|---|---|---|
| S09-E04-S01 | Deploy real secrets management | AWS Secrets Manager wired end to end — **closes TD-006's stub provider** |
| S09-E04-S02 | Eliminate secrets from code, images and config | Verified by scanning; build fails on any occurrence |
| S09-E04-S03 | Build the KMS key hierarchy | CMKs per data class, with defined ownership, policy and rotation |
| S09-E04-S04 | Implement secret rotation | Rotation procedure for every credential class, **exercised at least once** |
| S09-E04-S05 | Implement emergency revocation | A credential or partner key can be revoked and replaced under incident conditions; exercised |
| S09-E04-S06 | Define workload identity | Services authenticate to cloud resources by role, never by static credential |

### S09-E05 — Observability substrate · *Shivanshi*

| ID | Story | Acceptance criteria |
|---|---|---|
| S09-E05-S01 | Deploy metrics collection and storage | Every service scraped; retention defined; queryable |
| S09-E05-S02 | Deploy log aggregation | Structured logs centralised, searchable, retained per policy, **with PII masking verified in the pipeline** |
| S09-E05-S03 | Deploy distributed tracing | Traces span every service hop and external call; trace ID correlates to logs |
| S09-E05-S04 | Build baseline dashboards | Per service: RED metrics, saturation, dependency health |
| S09-E05-S05 | Build the alerting platform | Routing, on-call schedule, escalation, and runbook links in the alert payload |
| S09-E05-S06 | Separate the audit event pipeline | Audit events do not flow through operational logging; immutable, separately retained |

### S09-E06 — Data protection, backup and retention · *Aarti + Shivanshi*

| ID | Story | Acceptance criteria |
|---|---|---|
| S09-E06-S01 | Enable encryption at rest everywhere | Every store, backup and archive, with owned CMKs |
| S09-E06-S02 | Enforce TLS in transit | TLS 1.3 externally; mutual TLS or equivalent internally; verified by scan |
| S09-E06-S03 | Implement automated backup | Per store, meeting the S07 RPO; backups encrypted and access-controlled |
| S09-E06-S04 | **Prove restore** | A restore performed to a working state, timed against RTO — a backup that has never been restored is a hypothesis |
| S09-E06-S05 | Implement 7-year immutable retention | S3 Object Lock for raw payloads and audit archives; immutability tested by attempting deletion |
| S09-E06-S06 | Implement retention purge | Data past its retention horizon is disposed of, with an audit record of disposal |
| S09-E06-S07 | **Attest data residency** | Evidence that every store, backup, log and archive is in an India region |

### S09-E07 — Platform security baseline · *Deepali*

| ID | Story | Acceptance criteria |
|---|---|---|
| S09-E07-S01 | Implement network segmentation | Trust zones enforced by security groups and network policy; default deny between zones |
| S09-E07-S02 | Implement least-privilege IAM | No wildcard production policies; access reviewed and time-bound |
| S09-E07-S03 | Harden the container platform | Non-root, read-only filesystems, no privileged containers, admission policy enforced |
| S09-E07-S04 | Deploy runtime security monitoring | Anomalous behaviour detected and alerted |
| S09-E07-S05 | Implement edge protection | WAF, rate limiting, DDoS protection on public endpoints |
| S09-E07-S06 | Implement security event logging | Security events to a separate, tamper-evident store |

## 4. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S09-VT-01 | Infrastructure is reproducible | Destroy and recreate a non-production environment entirely from code | Identical, working environment; no manual step |
| S09-VT-02 | Region pinning holds | Attempt to apply a resource outside India regions | Policy blocks it |
| S09-VT-03 | Deployment works unattended | Deploy through the pipeline with no manual intervention | Success, fully audited |
| S09-VT-04 | **Rollback works** | Deploy a deliberately broken version to UAT, roll back | Previous version restored; data intact; timed |
| S09-VT-05 | Secrets are absent from artefacts | Scan images and repository history | Zero secrets |
| S09-VT-06 | Rotation works | Rotate a credential in UAT | Services continue; no outage |
| S09-VT-07 | **Restore works** | Restore each store from backup to a working state | RPO and RTO met and measured |
| S09-VT-08 | Retention is immutable | Attempt to delete an object under Object Lock | Deletion refused |
| S09-VT-09 | Observability is complete | Trigger a request; follow it end to end | Metrics, logs and a trace all present and correlated |
| S09-VT-10 | PII never reaches aggregated logs | Query the log store for regulated field patterns after a full test run | Zero matches |
| S09-VT-11 | Network segmentation holds | Attempt a cross-zone connection that policy forbids | Refused |
| S09-VT-12 | Residency is attestable | Enumerate every resource, backup and log destination | 100% in India regions |
| S09-VT-13 | Lower environments cannot reach production data | Attempt access from dev to production stores | Refused at the infrastructure layer |

## 5. Exit gate — GATE-S09

| # | Criterion | Level | Evidence artefact |
|---|---|---|---|
| S09-G1 | All infrastructure defined as code; no console-created production resource | E4 | IaC repository + drift report |
| S09-G2 | Dev, UAT and production provisioned from the same modules | E4 | Environment inventory + apply logs |
| S09-G3 | Environment recreatable from code | E3 | Destroy-and-recreate record |
| S09-G4 | Automated deployment with **tested rollback** | E3 | Deploy and rollback execution record |
| S09-G5 | Secrets management operational; TD-006 closed | E4 | Config evidence + rotation exercise record |
| S09-G6 | Observability operational: metrics, logs, traces correlated | E4 | Dashboard links + a walked trace |
| S09-G7 | Backup automated and **restore proven** against RTO/RPO | E3 | Restore test record with measured times |
| S09-G8 | 7-year immutable retention implemented | E4 | Object Lock config + deletion-refusal test |
| S09-G9 | **Data residency attested** | E2 | Residency attestation signed by SRE and Compliance |
| S09-G10 | Encryption at rest and in transit verified | E4 | Scan results |
| S09-G11 | Network segmentation and least-privilege IAM enforced | E4 | Policy config + connectivity test results |
| S09-G12 | IaC scanning in the pipeline | E4 | Scan report from a run |
| S09-G13 | No PII in aggregated logs | E4 | Log-store query result |

**Approvers:** Shivanshi (AP, B) · Deepali (AP, B, **human**) · Mahesh (AP) · Aarti (AP, backup
and DR) · Shailja (AP, residency and retention) · Amit (RV) · Swapnali (RV) · Kalpana (RV)

## 6. Current position in this repository — 🔴 Missing

| Capability | State | Evidence |
|---|---|---|
| Infrastructure as code | **Absent** | No `.tf`, no CloudFormation, no CDK, no Helm chart, no Kubernetes manifest anywhere |
| EKS / Kubernetes | **Absent** | — |
| Environments (dev/UAT/prod) | **Absent** | Spring profiles `uat` and `prod` exist in config; no environments exist to run them |
| Deployment automation | **Absent** | `render.yaml` — a manual dashboard Blueprint apply |
| Rollback | **Absent** | Never designed, never tested |
| Secrets management | **Stub** | TD-006 open: the AWS Secrets Manager provider is a stub |
| KMS key hierarchy | **Absent** | — |
| Observability stack | **Absent** | `bank-common-observability` exists as a library; there is no metrics store, log aggregation or tracing backend to send to |
| Backup and restore | **Absent** | Never configured, never tested |
| 7-year immutable retention | **Absent** | S3 Object Lock not configured. This is a **statutory obligation with no technical control** |
| Data residency | **Unverified** | Render.com region not chosen, not pinned, not attested |
| Network segmentation | **Absent** | Two JVMs in one container communicating over localhost |
| WAF / edge protection | **Absent** | — |

**What is actually deployed.** `render.yaml` defines one `starter`-plan Render.com web service
running the combined Docker image, with `1sb-integration-service` on 8080 and
`bank-persistence-service` on 8081 reached over localhost inside the same container. The file's
own comments note the plan's RAM is probably insufficient for two JVMs.

That is a legitimate and sensible **demo deployment**. It is not a banking platform, and the gap
between it and the documented AWS target architecture is the entirety of this stage.

**The regulatory items are the urgent ones.** Data residency (S09-G9) and 7-year immutable
retention (S09-G8) are conditions of operating under the IRDAI licence. They are currently
unimplemented and unattested, and no amount of application hardening substitutes for them. If any
real customer PII has passed through the current deployment, that is a question for Compliance to
answer now rather than at audit.

## 7. Premature at this stage

Autoscaling policy tuning · multi-region active-active · cost optimisation · chaos engineering ·
service mesh.

Build the platform that runs one journey reliably in one region. S14 sizes it for launch; S15
optimises it. A service mesh installed before there are services to mesh is complexity with no
counterparty.
