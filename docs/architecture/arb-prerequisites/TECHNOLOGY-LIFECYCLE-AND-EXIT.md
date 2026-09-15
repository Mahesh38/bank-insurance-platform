# Technology lifecycle, end-of-support, exit and decommissioning — R0

**Pack:** [`ARB-PREREQUISITE-PACK.md`](../ARB-PREREQUISITE-PACK.md) rows 18 and 19  
**Status:** `AI-DRAFTED`. Pins below are **as of 2026-09-14** from this repository’s Gradle and LLD. Amit (engineering pins) and Shivanshi (platform pins) confirm at S09.  
**Rule:** a managed AWS service’s lifecycle is AWS’s; our obligation is to stay on an in-support major and to have a documented move.

---

## 1. Technology lifecycle and end-of-support

| Component | Pin in repo / LLD | Support posture (draft) | Owner | Exit / upgrade trigger |
|---|---|---|---|---|
| Java | 21 (LTS) | In support (Oracle/Eclipse Temurin LTS). Next LTS is the planned jump, not 22/23. | Amit | Java 21 standard-support end, or a CVE that has no 21 backport |
| Spring Boot | **3.5.16** BOM | 3.5.x OSS support line in use. CVE overrides today: Netty `4.1.137.Final`, PostgreSQL JDBC `42.7.12`, Tomcat embed `10.1.59` (`build.gradle.kts`) | Amit | Boot 3.5 OSS end; move inside 3.x before 4.x |
| Gradle | Wrapper in repo | Stay on a supported major; wrapper is the pin | Amit | Wrapper upgrade in the same change as a plugin that needs it |
| Amazon EKS | Kubernetes **1.30+** (LLD; “platform current”) | AWS EKS standard support ~14 months per minor. Do not run a version in extended-support without a dated waiver | Shivanshi | n-2 policy: always one minor in standard support |
| Amazon Aurora PostgreSQL | Engine chosen by Aarti at S09 | AWS + PG community major. One cluster, schema per context (`ADR-008`) | Aarti | PG major AWS has announced EOS; upgrade in a change window with PITR drill |
| Amazon ElastiCache for Valkey | R0 session / L2 / rate-limit | AWS managed. **Never** SoR (`ADR-011`) | Shivanshi | Engine deprecation; sessions re-established (D13) |
| Amazon MSK | 3 brokers, KRaft, SASL/IAM | AWS managed Kafka. Outbox is the SoT (`ADR-012`) | Shivanshi | Kafka major AWS EOS; replay from outbox |
| OpenSearch | VPC-only, ISM 30 d hot | Operational only (`ADR-013`) | Shivanshi | Domain upgrade; logs are not evidence so rebuild is allowed |
| Keycloak | Container pin in WS-2 repo | Community or bank-approved build. **Not** the UI chrome (`ADR-020` Fireframe / NIP-APP) | WS-2 + Deepali | Major Keycloak that breaks the adapter port — adapter absorbs it |
| Flutter / NIP-APP | One project, three artefacts | Client stores: Play / App Store / EKS image. Tokens never on device | Amit + channel | Flutter major; store policy |
| Terraform | Bank IaC standard | Pin in modules; no console drift | Shivanshi | Provider major |
| GitLab | Bank CI | Bank-owned lifecycle | Bank DevOps | — |
| Cloudflare / F5-XC / Apigee | Bank SaaS | Bank-owned lifecycle. We consume. | Bank edge / API platform | Hostname and product re-onboard |
| 1SilverBullet APIs | Versioned paths behind adapter | Provider route. Breaking 1SB change is an adapter change, not a domain change | WS-1 | Provider EOS → second adapter or insurer-direct (R1+) |

**Not in R0 (so not in this lifecycle):** Istio, per-service Aurora, Redshift/Athena warehouse, Cognito as IdP, MSK Replicator, self-managed Redis/Kafka/ELK.

**CVE process already in the repo:** BOM property overrides in `build.gradle.kts` with a comment naming the CVE and the removal trigger (“remove once Spring Boot pins …”). That is the lifecycle mechanism for transitive Java, not a spreadsheet.

---

## 2. Exit, portability and decommissioning

### 2.1 What “exit” means here

Three different exits are routinely mixed. They are not the same change.

| Exit | What we keep | What we turn off | Timebox (draft) |
|---|---|---|---|
| **Replace 1SB** | Hub contract, canonical model, audit, policies already issued | `#15` adapter module + Apigee 1SB product | Adapter-swap; no domain migration (`TI-04`) |
| **Leave AWS** (extreme) | Logical model, WORM export, Terraform as documentation of intent | Accounts, KMS, network | Multi-quarter; India residency still binds the destination |
| **Decommission NIP** | 7-year evidence (audit, consent, suitability, policy documents) | Compute, sessions, operational logs, IdP for *this* programme | Retention clock starts from event time / policy termination per information model §2.2 |

### 2.2 Portability properties already designed (so exit is not a rewrite)

1. **Canonical model is bank-owned.** Provider JSON dies at the adapter. A second aggregator is a new adapter package, not a new Journey service.
2. **`distributorId` is injected by the Hub**, never taken from a caller. Removing 1SB does not require hunting client-supplied ids.
3. **No PVC is a system of record.** Keycloak, Flutter web, and every sale-path pod are replaceable. Aurora schemas + S3 Object Lock + DynamoDB PITR are the portable state.
4. **IaC is Terraform.** Re-creating the spoke in another account is a `tfvars` + RAM-share problem, not a snowflake.
5. **IdP is behind `identity-provider-adapter-service`.** Keycloak → another OIDC provider is an adapter, not a BFF rewrite. Workforce credentials never leave Bank AD (`TI-01`).
6. **Session vault is disposable.** ElastiCache loss is a mass re-login (`ADR-011`), not a data-loss event.

### 2.3 Data export on decommission

| Store | Export | Residual |
|---|---|---|
| Aurora (per-context schemas) | Logical dump encrypted to bank-controlled S3 in India | After retention sweep (`sp_retention_sweep` designed, not run) |
| S3 Object Lock (audit-archive, raw, docs) | Stay until Compliance mode retention expires — **cannot be shortened by decommission enthusiasm** | Shailja |
| DynamoDB (journey/jobs) | PITR export then TTL/dispose per class | Working-state only |
| Valkey | Discard | Sessions |
| MSK | Discard topics; replay was never the archive | — |
| OpenSearch | Discard | Operational |
| Keycloak DB | Partner identities export if a successor IdP exists; else disable | Workforce identities stay in AD |

### 2.4 Decommission sequence (draft)

1. Freeze origination (`#5` refuses `create`). In-flight journeys complete or compensate.
2. Stop outbound Apigee products for 1SB and PG session-create.
3. Drain EKS; retain Aurora + S3 + KMS + CloudTrail.
4. Run retention / disposal with a disposal audit row (parked `SUG-20260825-db1` purge — **not done**).
5. Revoke IRSA, Apigee products, API Gateway APIs, GitLab deploy keys.
6. Detach TGW; delete inspection VPC last so the deny-egress story is still true on the way out.

ARB is not being asked to approve a decommission. It is being shown that **exit is a designed property of R0**, not a future project.
