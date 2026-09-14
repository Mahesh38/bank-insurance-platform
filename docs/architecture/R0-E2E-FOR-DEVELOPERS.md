# R0 architecture for developers — end to end, every box, every refusal

Walk one RM sale through every hop: why that hop exists, whether it matches the AU bank estate, and what we refused.

**Audience:** engineers who write Java / Flutter and have not lived in bank landing zones.  
**Standing:** **Teaching compilation.** If this file disagrees with
[`03-solution-architecture-r0.md`](../platform/ws3-platform/03-solution-architecture-r0.md),
[`R0-LLD.md`](./R0-LLD.md), or an ADR, **those win** (`HA-02`).  
**Persona:** Mahesh — Principal Insurance Platform Architect.  
**Bank estate overlay:** [`VIN-003`](../au-bank-insurance-platform/references/2026-09-14-au-bank-enterprise-cloud-estate-notes.md)
and Board 1 draft [`ARCH-DEC-VIN003-B1`](./BOARD-1-REREVIEW-VIN003-ESTATE-2026-09-14.md).  
**Date:** 2026-09-14

This is the picture we are building **now** (R0 / H0): one RM sells one Life policy (Term or
Savings/ULIP) to one existing bank customer, with evidence. It is not the North Star diagram.

---

## 0. How to think about architecture if you mainly write code

A **service** is not "a class with a `@RestController`". It is a **lock on a business decision**.

| Word developers hear | What it actually means here |
|---|---|
| Bounded context (`#4`, `#10`…) | One team-sized piece of business that **owns its data and its rules**. Other services may *ask* it; they may not *write its tables*. |
| Deployable | A Spring Boot process (or the Flutter app). Context ≠ jar. We may later split or join jars; we must not mix two owners' tables. |
| Anti-corruption layer / adapter | A translation booth. 1SB's JSON never becomes our domain model. Same idea as mapping a third-party DTO at the edge of a module — except it is a **hard rule**, ArchUnit-enforced. |
| BFF | Backend-for-frontend: the only process the app talks to. Like an API gateway **for one channel**, plus **session and token custody**. |
| Spoke vs hub | We are a **new AWS account/VPC hanging off the bank's existing network**. We do not build a second FortiGate / second Direct Connect / second Transit Gateway. |
| Control (C1, C2, C4…) | A rule the **code structure** must make unbypassable. A UI hide is not a control. |

If you remember one sentence: **the Flutter app never talks to 1SB, never talks to the database, never holds an OAuth token, and never takes payment.**

---

## 1. One sale, as a request trace

Forget boxes. Follow one RM named Priya selling a Term policy to an ETB customer.

```text
Priya's laptop / phone
    │  HTTPS  (no OAuth token on the device — opaque session cookie/header)
    ▼
Cloudflare  →  F5-XC WAF  →  Amazon API Gateway  →  Internal ALB
    │
    ├─ static Flutter web  →  nip-web pod
    └─ /api/*              →  NIP BFF (#2)
                              │
                              ├─ "who is Priya and may she do this?"
                              │     identity-provider-adapter  →  Keycloak (later Bank AD)
                              │     identity-authorization PDP  →  allow / deny  (fail closed)
                              │
                              └─ use-case calls (never 1SB, never SQL of another service)
                                    Lead #5  →  create the only on-platform way in
                                    Customer #4  →  CIF lookup via bank EBS over Transit Gateway
                                    Journey #9  →  remember STAGE, not the suitability verdict
                                    Suitability #7  →  C1 gate (no quote without this id)
                                    Consent #6  →  OTP on CUSTOMER phone (C2)
                                    Quotation #10  →  Integration Hub #14  →  1SB adapter #15  →  1SB
                                    Proposal #11  →  Hub again
                                    Payment #12  →  AU Bank PG  **sends a link to the CUSTOMER device**
                                    Policy #13  →  only if payment is RECONCILED
                                    Audit #16  →  journey cannot become SOLD until this write lands
```

**Customer's phone** appears twice only: OTP, then payment. It never logs into NIP in R0.

That trace is the architecture. Everything below is "why that hop exists".

---

## 2. Layer 1 — devices and the app

### NIP-APP (one Flutter project → web + APK + IPA)

| | |
|---|---|
| **What it is** | The RM / IPR / admin **user interface**. One codebase. Role changes the screens, not the binary (`ADR-015`). |
| **Why** | Three apps would triplicate every gate (suitability, consent, payment isolation) and drift. |
| **Use** | Capture RM actions; never decide them. Tokens stay in the BFF. |
| **Bank standard?** | Bank digital apps are already web + mobile behind Cloudflare + F5. **Same perimeter idea.** They often ship separate web stacks; we chose one Flutter project to keep one evidence path. |
| **Not chosen** | Separate RM app + admin app + IPR app. Customer Flutter app (`#1` BFF) — **R1**, not R0. Native three codebases. |

### Customer device

Not an "actor" on the platform. Receives OTP and payment URL. If payment ran on Priya's laptop, an RM could pay with a customer PAN — that is the mis-selling hole **C4** exists to close.

### nip-web (EKS pod)

The **web build** of Flutter, baked into a container. No PVC, no writable volume, no public S3 website.

| Bank already supports | Why we didn't use it for R0 UI |
|---|---|
| S3 static website through Cloudflare + SSE-S3 (SOP) | Allowed for **dumb static assets**. Authenticated JSON and a logged-in RM UI are not "static assets". Public S3 websites are forbidden; the SOP exception is Cloudflare-origin from a **private** bucket. We still prefer image-baked `nip-web` so the UI version is the same artefact CI signed. |

---

## 3. Layer 2 — getting into AWS (the front door)

Think of this as **servlet filters you do not write**, in a fixed order.

### Cloudflare Enterprise (SaaS)

| | |
|---|---|
| **Use** | CDN, DDoS, TLS 1.3 at the edge. First hop the internet sees. **Not in our VPC.** |
| **Bank standard?** | **Yes — this is the bank's north-south standard** (Landing Zone + live apps). |
| **Not chosen** | CloudFront (would be a second CDN the bank does not operate). Putting Cloudflare "inside AWS" on a diagram — it is not an AWS box. |

### F5 Distributed Cloud / F5-XC (SaaS WAF)

| | |
|---|---|
| **Use** | WAF, OWASP, bot, L7 rate limits — **bank InfoSec policy**. Still not in our VPC. |
| **Bank standard?** | **Yes.** Live apps: Cloudflare → F5-XC → then AWS. |
| **Not chosen** | F5 **BIG-IP appliance in our VPC** (a 2026-08-27 diagram mistake; retracted `ADR-018`). AWS WAF as a substitute for F5-XC — would skip the bank's WAF operators. AWS Network Firewall — that is **egress**, not this hop. |

### Amazon API Gateway

| | |
|---|---|
| **Use** | First **AWS** hop. Request size, schema, throttle. **No business logic.** VPC Link into the internal ALB. |
| **Bank standard?** | **Partial.** Existing banking apps enter AWS on a **Public ALB**. We deliberately did **not** copy that (`ADR-018`, Board 1 rereview F-06). A candidate bank **Apigee** plane exists (`ASM-013`); we **do not draw it** until `SPIKE-001` answers. Until then API Gateway is Proxy 1. |
| **Not chosen** | Public ALB in front of API Gateway (extra hop, extra attack surface). Exposing EKS with a public NLB. Letting Flutter hit pods. |

### Internal Application Load Balancer

| | |
|---|---|
| **Use** | The **only** load balancer **inside** the VPC. `GET /*` → nip-web; `/api/*` → BFF. |
| **Bank standard?** | Bank apps also use ALB, often **public**. Ours is **internal** because API Gateway already took the public contract. |
| **Not chosen** | Nginx/Envoy sidecar farm; Kubernetes `LoadBalancer` with a public IP; second "admin ALB". |

### What we refuse at this layer

Workload VPCs have **no Internet Gateway**. You cannot `curl` a pod from the internet. That matches Zero Trust language on the bank landing-zone slide, even though the **neighbour** app still has a Public VPC + IGW.

---

## 4. Layer 3 — BFF and identity (the session)

### NIP BFF (#2)

| | |
|---|---|
| **Use** | Holds OAuth access/refresh tokens. Gives Flutter an opaque session. Aggregates 3–4 service calls into one screen payload. Calls the PDP before regulated actions. |
| **Why a BFF at all?** | If Flutter called `quotation-service` directly, every phone would hold a bearer token, talk to 16 DNS names, and skip PDP on a "shortcut" API. That is how SPA leaks become incident reports. |
| **Bank-ish?** | Token-hiding BFF is **our** control (`Flutter never receives OAuth tokens` — standing constraint). Bank console login is IAM Identity Center + AD — **operators**, not the RM app. |
| **Not chosen** | Flutter + Keycloak public client. One BFF per role (RM BFF, admin BFF) — `ADR-015` said roles are not applications. Customer BFF — R1. |

Session store: **ElastiCache for Valkey** (`ADR-011`). Not DynamoDB (withdrawn). Not the browser.

### identity-provider-adapter + Keycloak (WS-2)

| | |
|---|---|
| **Use** | Authenticate the **workforce**. Adapter in front so we can change IdP later. |
| **Bank standard?** | Workforce identity **must** end at **Bank AD** (`TI-01`). SOP: cloud users go through Central SSO AD + MFA. |
| **Why Keycloak first?** | Production IdP (Cognito vs Keycloak vs other) is **deliberately deferred**. Keycloak is a **temporary private IdP** behind the adapter. Federation to AD rides the private TGW path in `uat`/`prod`, never the internet. |
| **Not chosen** | Flutter → Cognito directly. Putting customers into AD. Treating Keycloak groups as business authorization (the **PDP** owns that). |

### identity-authorization (PDP)

| | |
|---|---|
| **Use** | Policy Decision Point: *may this RM, with this SP certificate, on this LOB, do `quote.create` **right now**?* Fail closed in 300 ms, no retry (`S-02`). |
| **Why not Spring Security annotations alone?** | Annotations on one controller do not stop another service from being called. PDP is checked at the BFF **and** on regulated domain APIs. |
| **Bank standard?** | Maker-checker / AR process is the **cloud console** analogue. Our PDP is the **application** analogue. Different planes. |

Specified Person is a **certificate attribute evaluated per action**, not "logged in as SP for the day".

---

## 5. Layer 4 — domain services (the business)

Each of these is a Spring Boot app you will recognise. The split is **ownership**, not "microservices for CV points". If two services share a table, we have already failed.

Pattern every service already used in `1sb-integration-service`:

```text
api/          HTTP only
application/  use cases — no SQL, no 1SB types
domain/       aggregates — no Spring
adapter/      DB, HTTP clients, 1SB — the only place foreign JSON lives
```

### Shared platform (built once, every LOB uses them)

| # | Service | Owns | Why it is separate | Bank analogue |
|---|---|---|---|---|
| **#5 Lead** | Why we contacted this person; accountable RM/SP; `lob` | Without a Lead, a quote has no SP on the record (mis-selling review fails). RM-only create | CRM / lead system — **ours**, not CBS |
| **#4 Customer** | Journey-scoped ETB **snapshot** | CBS is the bank's customer master. We **cache a snapshot**, we do not become CBS | **EBS APIs** to CBS/CIF over TGW |
| **#9 Journey** | Stage + pointers (`quoteId`, `consentId`…) | So a review can ask "where did it stop?" without grepping ten logs. **Must not copy** suitability or payment decisions | Orchestration, not a BPM suite in R0 |
| **#6 Consent** | Append-only grant + OTP evidence | C2. Mutable consent is a licence problem | Consent / OTP patterns in other bank apps; **ours must be WORM-like** |
| **#7 Suitability** | Eligible / not; recommended set | C1. Quote without this id is unlawful | Need-analysis; rules in `#19`, not hardcoded |
| **#8 Catalogue** | What we may sell (R0: Life, Group A, Term/Savings/ULIP) | Suitability and quote both need the same matrix | Product master — bank-owned config, not 1SB's list leaking in |
| **#12 Payment** | Session + reconciliation state | C4. Never card data. Never RM device | **AU Bank Payment Gateway** (3-D Secure on customer device) |
| **#13 Policy** | Issued policy + documents | Issues only if payment `RECONCILED` | Policy admin is **insurer**; we hold the **distributor record** |
| **#16 Audit** | Append-only "who did what" | Not CloudWatch. Journey cannot `SOLD` until audit **write** confirms | Distinct from QRadar / CloudTrail (those are **AWS/org** trails) |
| **#17 Notification** | OTP + payment link only in R0 | Failure must **not** block the sale; ops task instead | SMS/email gateways the bank already uses |
| **#19 Config** | Versioned, effective-dated rules | If rules live in `if` statements, Compliance cannot change a consent clause without a deploy (`CF-1`) | Not "Spring `application.yml`" |
| **#18 MIS** | Funnel / sold reports on a **read replica / events**, never Lead writer | Mixing MIS queries onto the sale writer is how year-end kills origination (`C-ISO-1`) | Bank MIS — isolated |

### Life execution cell (R0 only)

| # | Service | Owns | Why not "just put it in Journey" |
|---|---|---|---|
| **#10 Quotation** | Price discovery, offers, selection | Life quote shape ≠ Health quote. Hub/1SB are **how**, not **what** | |
| **#11 Proposal & UW tracking** | Application capture; **track** insurer UW, do not **decide** it | Bank distributes; insurer underwrites (`TI-03`) | |

Health later gets **its own** `#10`/`#11` cell — not `if (lob == HEALTH)` inside Life.

### Integration (the translation booth)

| # | Service | Owns | Why |
|---|---|---|---|
| **#14 Integration Hub** | Canonical bank request → **which provider** → adapter. Credentials, retries, bulkheads | If Quotation called 1SB, 1SB becomes a domain dependency. Tomorrow a direct HDFC adapter would rewrite Quote (`TI-19`, `PR-01`) | |
| **#15 1SB Adapter** | 1SB wire protocol, jobs, mTLS | **Exists today** (`1sb-integration-service`). ArchUnit: `adapter.onesb.*` only. **No Flyway, no JPA** in this service — job store via HTTP to `bank-persistence-service` | |

No WS-3 service calls `#15` directly. Hop is always Hub → adapter.

`distributorId` is **injected by us**. A body field from Flutter is rejected.

### Why we will not merge "a couple of services"

This is the question every coding team asks after counting jars. The full defence is
[`06-architecture-justification-and-review-answers.md` §4](../platform/ws3-platform/06-architecture-justification-and-review-answers.md).
Short version: **a merge of deployables is allowed; a merge of write models is not.**

| Proposed merge | Temptation | Why we refuse it |
|---|---|---|
| Consent + Suitability | "Both are pre-quote compliance" | Different legal artefacts and CMKs. C1 and C2 must reconstruct independently |
| Quotation + Proposal | "Both are sales" | Quote is short-lived fan-out; Proposal is a long UW case with documents |
| Quotation + Hub | "Quote always goes to 1SB" | Then 1SB is baked into domain code — the Beema-redirect problem inside our repo |
| Hub + 1SB Adapter | "Only one provider today" | A second adapter would rewrite Hub. Adapter owns wire; Hub owns routing |
| Payment + Policy | "Both are fulfilment" | Payment can be `UNCERTAIN`; Policy must not issue. Money ≠ contract |
| Journey + BFF | "Orchestration is API glue" | Then every channel reimplements the saga. DIY becomes N copies of state |
| Customer + Lead | "Both are CRM" | CIF snapshot is a fact about a person; Lead is the SP-accountable origination |
| Config into each service | "Each service knows its rules" | A consent-clause change becomes a deploy (`CF-1`) |
| Audit as a library only | "We have bank-common-audit" | Library shapes the event; the service is the 7-year query store |
| One InsuranceService god jar | "Small team" | Every rule change is a release; Health later is a rewrite |

**Honest packaging option:** several contexts in fewer Spring Boot processes, **if** schemas, credentials and ArchUnit walls stay. That is an ops ADR, not "we merged Consent into Quotation."

---

## 6. Layer 5 — data stores (where bytes live)

Developers default to "one Postgres and Redis for everything". We did not.

| Store | For | Never for | Bank already runs? | Why this, not the alternative |
|---|---|---|---|---|
| **Aurora PostgreSQL** — **one cluster, schema per context** (`ADR-008`) | Relational aggregates (Lead, Customer, Consent, Proposal, Payment, Policy, Config, PDP) | Cross-schema joins; a second service's tables | SOP allows Oracle / Aurora MySQL / **Postgres** / MSSQL. Live EKS apps already use Aurora PostgreSQL | **Not** Mongo (landing-zone "top services" list) — we need transactions and constraints. **Not** one database per microservice at R0 — ops cost; first split is LOB-cell vs shared, not every jar |
| **DynamoDB** | Journey state machine, quote poll jobs, audit event rows | Configuration source of truth; evidence that must be SQL-reported | Not a bank "standard DB"; allowed as AWS PaaS | Key-value, TTL, high write for jobs. Audit also archives to S3 WORM |
| **S3 + Object Lock (Compliance / WORM)** | Raw provider payloads, policy PDFs, 7-year evidence | Public website; logs of PII | S3 is standard. **No public buckets.** Object Lock is how we meet retention, not "a folder" |
| **ElastiCache Valkey** (`ADR-011`) | BFF sessions, L2 cache, rate-limit counters | **Idempotency** (stays in the SQL/Dynamo write). System of record. Serving config when DB is down | Redis already on live EKS diagrams | Valkey = Redis-protocol managed. **Not** using cache as the source of truth after a TTL |
| **Amazon MSK (Kafka)** (`ADR-012`) | Fan-out of domain events **after** the outbox row commits | The audit record itself. "The topic is the evidence" | Kafka already on live EKS diagrams | We **almost didn't** put a broker in R0. Kept **transactional outbox** in the service DB as source of truth; MSK is the van, not the vault |
| **OpenSearch** (`ADR-013`) | Operational logs / firewall logs | Evidence, PII search, compliance queries | Elasticsearch on live apps | 90-day ops. **QRadar** is the bank SIEM for **AWS/org** security events — different plane |
| **KMS + Secrets Manager** | CMKs, DB creds, 1SB keys | Secrets in Git, in pods as files | Standard AWS | India CMKs only |

**Idempotency:** client sends `Idempotency-Key` at the BFF; internally we derive keys from `quoteId` / `proposalId`. Stored **with the business write**, not in Redis. A Redis blip must not allow a double proposal.

**`bank-persistence-service`:** the **platform** DB owner for integration jobs/audit HTTP. Business contexts get **their own schemas** on Aurora; we do not grow persistence-service into a god ORM.

---

## 7. Layer 6 — how a request leaves the building

Two different "outsides". Mixing them is how CIF data ends up on the public internet.

### A. Bank systems (CBS, Bank AD) — private

```text
Pod → TGW attachment ENI → existing AU-CTO-NETWORK Transit Gateway
    → EDGE FortiGate (hub NGFW) → Direct Connect Gateway (Sify primary, Airtel secondary)
    → DC / CBS / AD
```

Site-to-Site VPN is provisioned **first** (so UAT is not stuck on a carrier order). DX becomes primary; VPN stays standby (`ADR-009`).

| Bank standard? | **Yes — this is the Central Network Account V1 diagram.** We attach as a **spoke**. |
| Not chosen | Second TGW "for insurance". Second DX circuit. VPC peering mesh. Bank reverse proxy over the internet with mTLS for CIF (puts customer data on the public path). Stubs in **UAT** (dev-only). |

Customer lookup goes through **EBS (Enterprise Service Bus)** APIs — bank integration standard — not a JDBC URL to CBS.

### B. Internet partners (1SB, SMS) — inspected egress

```text
Pod → TGW → (spoke) AWS Network Firewall → NAT + Elastic IP → IGW → 1SB
```

1SB allowlists **those NAT EIPs**. Workload VPCs have no NAT and no IGW.

| Bank standard? | Hub already inspects via **FortiGate**. Our extra Network Firewall is **R0's** domain-allowlist control (`ADR-010`) because Security Groups cannot say "only 1SB hostname on 443". |
| Tension | Board 1 `C-01` / `ASM-012`: bank may insist egress **only** through EDGE FortiGate. **Do not publish EIPs until that answer exists.** |
| Not chosen | Open 443 from nodes. FortiGate **pair inside our VPC** (licence + HA we are not staffed to run). Decrypt 1SB mTLS at the firewall (would break mutual TLS). One inspection VPC shared by dev and prod. |

### C. Money — not our VPC at all

```text
Payment service → AU Bank PG "create session"
Notification → payment URL → customer's phone
Customer pays on PG (3-D Secure)
PG callback → our API Gateway (separate route, TB-6)
Payment service → RECONCILED only after settlement file, not after "success" JSON
Policy service → issue
```

If the PG callback is lost: state `UNCERTAIN`, **block a second attempt**, wait for settlement. Timeouts do not mint money.

---

## 8. Layer 7 — the factory (build and run)

| Component | Use | Bank standard? | Not chosen |
|---|---|---|---|
| **EKS** | All microservices, private API endpoint | **Yes — listed IaaS, live prod diagrams** | ECS, Lambda-for-everything, EC2 pets, Render.com as a **data** path (dev-preview only, never PII) |
| **ECR** | Images, scan on push, replicate to Hyderabad | Standard | Docker Hub as runtime |
| **GitLab CI + Terraform** | Bank delivery + IaC | **Yes — live apps** | GitHub Actions as the production apply path; console clicking |
| **Control Tower / `AU_AWS_MAS`** | Account vending, SCPs, Prisma | **Yes — we join, we do not replace** | A shadow org; root user day-to-day |
| **IAM Identity Center** | Human AWS console SSO via AD | **Yes** | Long-lived IAM users for people |
| **IRSA** | Pods assume IAM roles | AWS standard | Access keys in Secrets that pods cat |
| **CloudWatch / X-Ray or ADOT** | App logs/metrics/traces | Bank also has Prometheus/Grafana on some apps | Logging PAN. Using OpenSearch as the audit store |
| **GuardDuty / Security Hub / Config** | Spoke account posture | **Additive.** Org already has **Prisma + QRadar + 10-year CloudTrail** | Replacing QRadar with CloudWatch |

Environments the **bank** vendors by default: **Prod, CUG, UAT** (Dev lives **inside UAT** unless Cloud approves a split). Our LLD still draws a separate `dev` — **exception to ask for**, not a right (`ARCH-DEC-VIN003-B1` C-02).

---

## 9. Layer 8 — DR and regions, in one paragraph

- **Must live in India** (SCP + `TI-08`): Mumbai primary, Hyderabad standby.
- **Multi-AZ in Mumbai** for Tier-ish production stores: SOP says Tier 1/2 **must** be Multi-AZ. Our `uat`/`prod` Aurora/cache/brokers already span AZs.
- **Warm standby in Hyderabad** is **not** the SOP's "Active-Active cloud Tier 1/2" sentence. Do not "fix" that without an RIA tier and Aarti/Shivanshi (`C-04`).
- A DR region with **no TGW path to CBS/AD** is a cluster that cannot sell.

---

## 10. Cheat sheet — "why didn't we just…"

| Temptation | Why not |
|---|---|
| Flutter → 1SB | 1SB schema in the UI; cannot replace 1SB; no C1/C2/C4 |
| Flutter → RDS | Tokens + PII on the device path; no PDP |
| One "InsuranceService" god jar | Every rule change is a release; Health later becomes a rewrite |
| Journey service stores "suitabilityPassed=true" copied from #7 | Two sources of truth; FF-04 fails |
| Quote 1SB job id as our quote id | Provider id becomes our primary key (`TI-22`) |
| Pay on RM iPad | C4 / standing constraint |
| `SOLD` because payment API returned 200 | Must be RECONCILED + policy ACTIVE + audit write |
| Public S3 for Flutter | SOP: no public buckets; authenticated app is not a brochure site |
| Second TGW | Bank already has the hub; Board 1 `A0` if we clone it |
| Decrypt 1SB at the firewall | Breaks mTLS; outage dressed as security |
| Kafka as the audit log | Topic retention ≠ 7-year WORM licence evidence |
| Redis for idempotency | Cache eviction = double submit |
| Hardcode suitability rules | Compliance cannot change them without engineering (`#19`) |
| Customer in Bank AD | AD is workforce only (`TI-01`) |

---

## 11. What is still open (so you do not code as if it were closed)

From the Board 1 rereview — **not** excuses to invent a fourth architecture:

1. **Egress:** spoke Network Firewall vs EDGE FortiGate only (`ASM-012`) — before publishing 1SB EIPs.
2. **Apigee:** might replace API Gateway (`SPIKE-001`) — do not draw it, do not call it from Java.
3. **Split `dev` account** vs Dev-inside-UAT — Cloud team.
4. **CUG** environment — unnamed in R0.
5. **Keycloak vs final IdP** — adapter stays; product can change.

You can still write services. You cannot assume a public ALB, a second TGW, or payment on the RM session.

---

## 12. Where to read next (when a box in this file is not enough)

| If you are building… | Read |
|---|---|
| The sale path / gates | [`R0-HLD.md`](./R0-HLD.md) §1–§2 · [`03-solution-architecture-r0.md`](../platform/ws3-platform/03-solution-architecture-r0.md) §5 seams |
| A Gradle module | [`backend-service-catalog.yaml`](../platform/engineering/backend-service-catalog.yaml) · `api/application/domain/adapter` in `03` §6 |
| VPC / EKS / TGW | [`R0-LLD.md`](./R0-LLD.md) §2 |
| 1SB | [`17-provider-aggregation-and-connectivity.md`](../context/roles/mahesh-principal-insurance-platform-architect/17-provider-aggregation-and-connectivity.md) |
| Bank hub / FortiGate / SOP | [`18-au-bank-enterprise-cloud-estate.md`](../context/roles/mahesh-principal-insurance-platform-architect/18-au-bank-enterprise-cloud-estate.md) |
| "May I add Redis/Kafka/a new service?" | `AP-09`: only with a named problem; then this file's cheat sheet |
