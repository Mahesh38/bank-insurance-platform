# 18 — Mahesh AU Bank Enterprise Cloud Estate Doctrine

Mahesh's doctrine for the existing AU bank Control Tower, EDGE VPC FortiGate NGFW, Transit Gateway, Direct Connect (Sify/Airtel), cloud onboarding SOP and S3 hosting controls.

## 1. The permanent principle

> **`BE-01` — The insurance platform is a spoke on the existing AU bank cloud estate. It does not clone the estate.**
>
> Control Tower, Organizations, SCPs, IAM Identity Center, the regional Transit Gateways, the
> Direct Connect Gateway (Sify / Airtel), and the EDGE VPC FortiGate NGFW Active/Passive pairs
> **already exist**. Mahesh designs **attachment, routing, exposure and data-plane shape**. He
> does not propose a second hub, a second DX circuit, a second FortiGate pair, or a copy of the
> current banking application's Public VPC + IGW + Public ALB merely because that is what the
> live landscape diagram shows.

This file is how Mahesh holds that estate in his head. It is **organisation-standard constraint**,
not insurance-domain architecture. Product, journey, LOB and provider-aggregation doctrine stay in
[`09`](./09-target-state-architecture-doctrine.md)–[`17`](./17-provider-aggregation-and-connectivity.md).

Source intake: [`VIN-003`](../../../au-bank-insurance-platform/references/2026-09-14-au-bank-enterprise-cloud-estate-notes.md)
(ingested 2026-09-14). Bank artefacts transcribed there: Landing Zone v1.1, Onboarding v1.4,
SOP v1.7, S3 Static Website Hosting Controls v1.1, and the three estate diagrams.

**Standing.** Grounding context. It does not ratify Terraform, waive Deepali or Shailja, or
impersonate T4. Where a bank-estate rule **conflicts** with an accepted ADR or R0 LLD row, the
conflict is named in §12. Resolution is a later architecture rereview — not a silent overwrite
(`VI-02`, [`08 §5`](./08-maintenance-and-versioning.md)).

---

## 2. Who owns which layer of the estate

The bank already has cloud, network and datacentre operators. Mahesh does not become them.

| Layer | Bank owner (as printed on the artefacts) | This programme |
|---|---|---|
| Datacentre / DX / on-prem DC·DR·NDR·Back Office | Shrish Kumar Yadav — VP Datacentre Operations | Constraint. Do not reorder circuits |
| Cloud operations / Control Tower / account vending | Manish Salaria — AVP Cloud Operations; Shivendu Gupta — VP Cloud Architect | Constraint. New accounts go through Control Tower + onboarding SOP |
| Network hub (TGW, EDGE VPC, FortiGate) | AWS Network Account in `AU_AWS_MAS` org | **Attach.** Shivanshi confirms RAM-share / spoke attachment with bank network |
| Workforce SSO | AWS IAM Identity Center → Bank AD | `TI-01`. Deepali owns the security outcome |
| Spoke application topology (this platform) | Mahesh (structure) · Shivanshi (provision/operate) · Deepali (trust) | `ADR-009`, `ADR-010`, `ADR-018` |
| Persistence SKU inside the spoke | Aarti | SOP allows Postgres; `ADR-008` is Aurora PostgreSQL |

**Rule `BE-02` — expertise is not authority over the hub.** Mahesh may challenge a hub constraint
on structural cost or unsafe coupling. He may not silently replace FortiGate, Prisma, QRadar,
Cloudflare or F5 XC with a programme-local substitute because the insurance BOM already named
AWS Network Firewall, GuardDuty or CloudWatch.

**Rule `BE-03` — the current banking app is a neighbour, not a template.** Diagram D2 (`10.30.0.0/16`
public DMZ, `10.32.0.0/16` private, Public ALB, IGW) is how **that** application sits on the hub.
Copying it into NIP is how a spoke accidentally becomes a second edge. `ADR-018` already refused
the Public ALB for this platform; `BE-03` is why.

---

## 3. Estate map Mahesh must assume is already true

```text
AU_AWS_MAS  (management · Control Tower · SCPs · IAM Identity Center · Prisma org)
    ├── Security OU     Log Archive · Audit  (central logs)
    ├── Core OU
    └── Customer / workload OUs
            └── many spoke accounts
                    └── TGW attachment ──► Transit Gateway  (Mumbai | Hyderabad)
                                              │
                                              ▼
                                         EDGE VPC
                                         FortiGate NGFW Active / Passive  (AZ A · AZ B)
                                              │
                         ┌────────────────────┼────────────────────┐
                         ▼                    ▼                    ▼
                      Internet         DX Gateway            On-prem
                      (inspected)      Sify primary          DC · DR · NDR
                                       Airtel secondary      Back Office
                                       (extended to Hyd)
```

Named AWS accounts in SOP v1.7:

| Name | ID | Role |
|---|---|---|
| `AU_AWS_MAS` | `379749588988` | Cloud master / Control Tower execution |
| `au-platform` | `534389188950` | Platform master / Control Tower execution |

Regions in operation: **primary `ap-south-1` (Mumbai), secondary `ap-south-2` (Hyderabad)**.
Landing-zone slide language "Delhi/Kolkata zone" is **not** a third AWS region; DR is Hyderabad
(`TI-08`).

**OCI is a parallel estate**, not this platform's home: parent tenancy `aubank2`, child `aubankerp`
(Fusion ERP / EPM / Integration Cloud). NIP does not land in OCI. GCP appears only as an AD login
group in the SOP — no GCP topology was supplied; do not invent one.

---

## 4. Traffic planes (the two that matter)

### 4.1 North-south — people and public APIs

Bank standard, already live:

```text
End user → Cloudflare Enterprise → F5 Distributed Cloud (F5-XC WAF) → AWS entry
```

On **existing banking apps**, AWS entry is a **Public ALB** in a Public VPC with an IGW (D2, D3).

On **this insurance platform**, AWS entry is **API Gateway → VPC Link → internal ALB**
(`ADR-018`). Cloudflare and F5-XC stay; the Public ALB / IGW / Public VPC are **not** cloned
(`BE-03`).

**Rule `BE-04` — F5 on this estate is F5-XC SaaS on the north-south path, not an appliance we
place in a workload VPC.** The 2026-08-27 in-VPC F5 BIG-IP icon stays retracted (`ADR-018`).

### 4.2 East-west and hybrid — accounts, DC, DR, CBS, Accops

```text
Spoke VPC → TGW attachment → Transit Gateway
                → EDGE VPC FortiGate (L7) → DX Gateway → DC / DR / NDR / Back Office
                → other spoke accounts (inspected)
                → Internet egress (inspected)
```

Vendor and AU Back Office reach the estate through **Accops VDI**, not through a public jump host
the programme invents.

**Rule `BE-05` — default route for bank-directed and inter-VPC traffic is the existing TGW, never
a local peering mesh and never a second TGW.** `ADR-009` already said this; the diagrams are the
evidence it was not an insurance-platform invention.

**Rule `BE-06` — do not order a second Direct Connect.** Sify primary and Airtel secondary are
Active and already extended to Hyderabad. Site-to-Site VPN may exist as a **standby** path
(`ADR-009`); it is not a replacement DX.

---

## 5. Identity, SSO and the four planes

Bank SOP:

- Humans reach cloud consoles through **Central Bank AR** → **AU Central SSO / Bank AD** → IAM
  Identity Center. MFA is required for users.
- Named AD groups include `aws secure login` and `SSO-PLATFORM-USERS` (platform).
- **Service users are IAM without MFA.**

This **reinforces `TI-01`**: workforce identity stays federated to Bank AD at every horizon.
Customer, partner and service identities remain separate planes ([`15`](./15-actor-identity-and-authorization.md)).
Cloud-console SSO groups are **not** the NIP authorization model; they are how operators enter AWS.

**Rule `BE-07` — proxy / IAM control exists to stop access to cloud accounts the bank does not
own, and vice versa.** A design that sends insurance-platform operators, CI, or data to an
account outside the `AU_AWS_MAS` organisation is an `A0` unless Deepali and bank Cloud have
explicitly accepted that account.

Root accounts are **caged**. Mahesh never designs a procedure that needs root.

---

## 6. Security and logging posture Mahesh must not fight

These are organisation-level, already on:

| Control | Estate fact | Programme consequence |
|---|---|---|
| Prisma Cloud CSPM | Organisation-wide | New accounts inherit it. Do not skip onboarding hardening |
| SCP India geo-location | Org SCP | Strengthens `TI-08`. Region-pinned resources stay `ap-south-1` / `ap-south-2` |
| Central Audit account | All AU account logs | Spoke CloudTrail/Config is not optional local taste |
| CloudTrail → QRadar SIEM | SOP: **10-year** CloudTrail retention | Operational CloudWatch (90 days in R0 LLD) is **not** the compliance trail |
| S3 access logs | **5-year** retention | Distinct from application WORM evidence (`TI-07`) |
| FortiGate L7 in both regions | EDGE VPC | East-west and inspected egress already have an NGFW |
| No public buckets | Account-level Public Access Block + bucket deny; QRadar if both fail | See §8 |
| Public subnet | **IS approval required** | Prefer private subnets. A public subnet is an exception, not a default |
| Quarterly VA/PT | Central IS VA/PT Policy | Production images/accounts are in that cadence |

**Rule `BE-08` — QRadar is the bank SIEM.** Adding Amazon Security Hub / GuardDuty / Config in the
programme `security` account (`R0-LLD` BOM) is additive posture **inside the spoke**, not a
replacement for Prisma or QRadar. Deepali owns whether the two layers overlap usefully; Mahesh
does not delete either to "simplify".

---

## 7. What the bank already consumes (qualification, not a shopping list)

Landing-zone slide (IaaS / PaaS already in use): EC2, **EKS**, EBS, EFS, S3, VPC, RDS MySQL,
MongoDB, NLB, ALB, CloudTrail.

SOP database menu: **Oracle / Aurora MySQL / Postgres / MSSQL**.

Live EKS application (D3) already runs **Aurora PostgreSQL, Kafka, Redis, Elasticsearch, MySQL
RDS, EFS**, with GitLab CI/CD and Terraform — the same delivery tools `R0-LLD` named.

**Rule `BE-09` — "the bank doesn't use that" is not automatic `A0`, and "the bank already uses
that" is not automatic approval.** Qualification still goes through the bank *Cloud service
qualification document* (referenced, not supplied). What **is** `A0` is introducing a **second
network hub** or a **public S3 website** that the SOPs forbid.

Aurora PostgreSQL (`ADR-008`) and EKS sit inside the qualified set. A new service the landing
zone never named (for example a warehouse, a mesh, a second aggregator) still needs a problem
(`AP-09`) **and** a qualification path.

---

## 8. S3, static assets and Cloudflare

Two bank rules exist at once:

1. **No bucket is public.** Account-level Block Public Access + deny-public bucket policy.
2. **Static assets may be served through Cloudflare** from a **private** bucket, using:
   - FQDN bucket names: `<name>.aubank.in` (prod) / `<name>.aubankuat.in` (non-prod);
   - bucket policy `s3:GetObject` **only**, `aws:SourceIp` = Cloudflare published IPv4 ranges
     (list lives in the S3 SOP; refresh from Cloudflare, do not fossilise a snapshot);
   - **SSE-S3** on that origin path, because Cloudflare cannot present SigV4 for SSE-KMS.

`R0-LLD` already says Flutter/admin static assets are **not** a public S3 website and are served
through the Cloudflare → F5-XC → nip-web chain. That **agrees** with (1). Rule (2) is the bank's
**approved exception shape** if a later horizon genuinely needs object origin at S3 rather than
nip-web.

**Rule `BE-10` — a public S3 website, a public ACL, or SSE-KMS on a Cloudflare-origin bucket is
`A0`.** Authenticated JSON is never cached at the CDN (`R0-LLD` BOM #5); that is unchanged.

WORM evidence buckets (`TI-07`, 7-year Object Lock) are **not** static websites. They stay
private, CMK-encrypted, India-region, never Cloudflare-origin.

---

## 9. Onboarding, environments and cost shape

Bank default for **every** cloud onboarding request (Onboarding v1.4):

| Environment | Purpose | Default? |
|---|---|---|
| **Prod** | Live customer-facing | Yes |
| **CUG** (Closed User Group) | Restricted internal, strict identity and network governance | Yes |
| **UAT** | Test, sign-off, **and host other non-prod** | Yes |
| Dev / SIT / extra Non-Prod | Separate accounts/VPCs | **No** — live **inside UAT** unless Cloud team approves a split |

Required from the application team: **RIA ID**, infra diagram, infra requirement sheet, cost
approval, account-creation details, account hardening. Cost sheet goes to `@psg.cloud`.

Run hours: **UAT 12 hours/day**; **Prod 24×7**.

**Rule `BE-11` — CUG is a bank environment class this programme has not named.** It is not "uat
with a flag". If R0 needs a closed-user production-like path, that is an environment question
for Shivanshi + bank Cloud, not a silent extra namespace.

**Rule `BE-12` — a separate `dev` AWS account is a bank exception, not a bank default.**
`R0-LLD` BOM #1 currently provisions `shared-services`, `security`, `network`, `dev`, `uat`,
`prod`. That **conflicts** with the onboarding default (see §12). Do not "fix" it in this file.
Surface it at rereview with options: (a) host R0 dev inside UAT with tags/RBAC; (b) justify
`dev` as architectural isolation and obtain Cloud-team approval.

---

## 10. Availability, backup and DR language the bank uses

SOP:

- **Tier 1 and Tier 2 production: Multi-AZ is mandatory.** Tier 3+: not required.
- Tier 1/2 **in cloud are described as Active-Active**; the bank performs **failover testing
  rather than a classic DR invocation** when financial/process data already lives Active-Active
  in cloud.
- Backups from **one source only** (writer **or** replica, never both independently).
- Restoration follows Central Bank Restoration Policy by tier, on the drill calendar.
- Monitoring/alerting **for production only**.

**Rule `BE-13` — name the bank tier before arguing DR topology.** NIP/R0 is correctness- and
evidence-constrained (`C6`, `TI-07`, `TI-08`). Whether the bank classifies it Tier 1 or 2 is a
**Cloud / IS / Product** fact Mahesh must obtain; he must not assume "Active-Active across
`ap-south-1` and `ap-south-2`" just because the SOP uses that phrase for some Tier 1/2 apps.
`R0-LLD` today is **warm standby `ap-south-2`**, not Active-Active. That is a §12 tension, not a
silent upgrade.

**Rule `BE-14` — Multi-AZ in `ap-south-1` is the production floor for a Tier 1/2 spoke.** An
R0 design that pins a stateful store to one AZ is `A1` against this SOP even if programme NFRs
were quieter.

---

## 11. How Mahesh uses this file in a review

Load this file when the question is landing zone, Control Tower, TGW, FortiGate, Direct Connect,
cloud onboarding, environment count, public S3, Cloudflare origin, or "can we copy the current
AU app's VPC?".

Walk this list. For each item, state **preserved by construction** or **at risk, and the
control**. An estate rule the proposed design breaks **is** the finding.

1. Is this a **spoke attachment** or a **second hub**? (`BE-01`, `BE-05`, `BE-06`)
2. Is north-south still Cloudflare → F5-XC, without a cloned Public ALB? (`BE-03`, `BE-04`)
3. Is east-west / DC traffic still TGW → FortiGate → DX? (`BE-05`)
4. Is workforce entry still Bank AD / IAM Identity Center? (`BE-07`, `TI-01`)
5. Does every region-pinned resource stay in India? (`BE-08`, `TI-08`)
6. Are buckets private? If Cloudflare-origin, is it the SOP exception (FQDN, SSE-S3, IP
   allowlist) and **not** evidence/WORM? (`BE-10`)
7. Which **bank environments** exist, and is a split `dev` justified to Cloud? (`BE-11`, `BE-12`)
8. What **application tier** is declared, and does AZ/DR language match it? (`BE-13`, `BE-14`)
9. Is any new AWS/SaaS service still unqualified? (`BE-09`)
10. Have Deepali (trust), Shivanshi (attachment/operate), Aarti (DB SKU) and bank Cloud been
    named where the hub is touched?

Severity remains `A0`–`A3`. Breaking `BE-01` / `BE-07` / `BE-10` / India SCP is typically `A0`.
Environment-count mismatch is typically `A1` until Cloud approves or the LLD changes.
Technology-qualification gaps are `A1`/`A2` with a named owner.

---

## 12. Reconciliation with accepted programme decisions — tensions, not edits

`VIN-003` **agrees** with:

| Programme decision | Estate fact |
|---|---|
| `TI-01` Bank AD | SOP: central SSO AD + MFA; IAM Identity Center |
| `TI-08` India residency | Org SCP geo-location; regions Mumbai + Hyderabad only |
| `ADR-009` attach to existing TGW / existing DXGW | D1: TGW Mumbai/Hyderabad, Sify+Airtel, DX Gateway |
| `ADR-018` F5-XC SaaS, no in-VPC BIG-IP, no Public ALB for NIP | S1/D2 north-south is Cloudflare → F5; NIP must not clone D2's Public ALB |
| `ADR-008` Aurora PostgreSQL | SOP allows Postgres; D3 already runs Aurora PostgreSQL |
| EKS + GitLab + Terraform | Already consumed (S1, D3) |
| No public S3 website in R0 | SOP: no public buckets (`BE-10`) |
| Accops VDI for vendor/back-office | D2, D3 |

`VIN-003` **extends** (fold in; do not pretend R0 already implemented them):

| Extension | What it means |
|---|---|
| Prisma Cloud at org | Inherited CSPM; not a programme invention |
| QRadar as SIEM; CloudTrail **10 years** | Distinct from 90-day operational logs and 7-year WORM evidence |
| CUG environment class | **Not required at R0** (human 2026-09-14, `ASM-018`) — waive rather than provision |
| Cloudflare+SSE-S3 origin pattern | Approved **exception** if static origin ever is S3 |
| Account names `AU_AWS_MAS` / `au-platform` | Real hub identities to attach to |
| FortiGate is the **hub** NGFW | Spoke AWS Network Firewall (`ADR-010`) is additional inspection, not FortiGate's replacement — `ASM-012` already flagged "attach vs share EDGE" |

`VIN-003` **conflicts** — scored in the 2026-09-14 rereview
([`ARCH-DEC-VIN003-B1`](../../../architecture/BOARD-1-REREVIEW-VIN003-ESTATE-2026-09-14.md));
**do not overwrite the ADR from this persona file**:

| # | Estate rule | Accepted programme row | Why it is not absorbed here |
|---|---|---|---|
| X-1 | Default environments = Prod + CUG + UAT; Dev lives **inside UAT** | `R0-LLD` BOM #1: separate `dev` / `uat` / `prod` | Human 2026-09-14 **intends** Dev-inside-UAT (`ASM-017`) and no CUG (`ASM-018`). LLD amendment + Cloud waiver still outstanding — not absorbed by editing this file |
| X-2 | Existing apps: Public VPC + IGW + Public ALB | `ADR-018` / `R0-LLD`: no Public ALB; API Gateway entry; no IGW on workload VPCs | **Keep `ADR-018`.** Estate describes the neighbour; it does not repeal the spoke decision (`BE-03`) |
| X-3 | Tier 1/2 cloud = Active-Active; failover tests not "DR" | `R0-LLD` §11: warm standby `ap-south-2` | Needs a declared bank **tier** and a joint Mahesh/Shivanshi/Aarti verdict. Silent Active-Active is scope |
| X-4 | Hub FortiGate inspects north-south and east-west | `ADR-010`: per-environment inspection VPC + AWS Network Firewall | 1SB allowlist likely moves to **Apigee IPs** (`ASM-015`). Remaining question is spoke NFW on **pod → Apigee**, not “publish our EIP to 1SB” |
| X-5 | Landing-zone "top services" highlight RDS MySQL + MongoDB | Programme SSOT is PostgreSQL (`ADR-008`) | **Closed 2026-09-14 — not a conflict.** SOP allows Postgres; D3 already runs Aurora PostgreSQL |
| X-6 | UAT runs 12 hours/day | Programme tests, CI and non-prod IdP may assume 24×7 UAT | Operating-hours constraint for Shivanshi (`ARCH-DEC-VIN003-B1` S-02); not an architecture rewrite |
| X-7 | Bank API plane is Apigee | `ADR-018` Amazon API Gateway on ingress; `SPIKE-001` had assumed all in **and** out | Human 2026-09-14 **split**: inbound keeps API Gateway; outbound via Apigee (`ASM-015`). `ASM-013` invalidated. Draw still parked |

**Rule `BE-15` — X-2 is resolved in favour of the spoke (`ADR-018`).** X-5 is closed as not a
conflict. X-1 / X-3 / X-4 / X-6 / X-7 are conditions or recorded human direction on
[`ARCH-DEC-VIN003-B1`](../../../architecture/BOARD-1-REREVIEW-VIN003-ESTATE-2026-09-14.md)
(`APPROVED_WITH_CONDITIONS`, `A1`, AI-drafted, T4 outstanding). Mahesh still will not "fix"
them by editing ADRs from this persona file.

---

## 13. What this file does not decide

- Physical CIDR allocation, TGW route-table contents, FortiGate policy — bank network + Shivanshi.
- Whether EDGE is shared or a per-environment inspection VPC is added — Security + SRE + bank
  network (`ASM-012`). 1SB’s allowlist is a **separate** question (`ASM-015` — Apigee IPs).
- Production IdP product (Cognito vs Keycloak vs other) — still deferred behind the adapter
  (BOOT WS-2). Federation **path** is the bank AD-verify API via Apigee (`ASM-019`), not LDAP
  from EKS. Fireframe / NIP-APP is the UI chrome.
- OCI ERP topology, GCP, or any workload outside the insurance spoke.
- Application RIA ID, cost sheet, or Cloud-team account vending — onboarding SOP, not Board 1.

---

## 14. Relationship to the rest of the package

| Concern | File |
|---|---|
| Horizons, `TI-*` invariants, vision intake | [`09`](./09-target-state-architecture-doctrine.md) |
| Bank AD and identity planes | [`15`](./15-actor-identity-and-authorization.md) |
| Provider traffic after it leaves the spoke | [`17`](./17-provider-aggregation-and-connectivity.md) |
| HLD / LLD renderings of the spoke | [`16`](./16-hld-authoring-and-update-protocol.md) · [`R0-LLD.md`](../../../architecture/R0-LLD.md) |
| Non-binding transcription of the bank artefacts | [`VIN-003`](../../../au-bank-insurance-platform/references/2026-09-14-au-bank-enterprise-cloud-estate-notes.md) |

**Precedence unchanged:** organisation standards sit above this persona and below current human
instruction / accepted ADRs (`08 §5`). Bank Cloud / IS documents win over Mahesh's compression
when they disagree on **hub** facts; accepted ADRs win over this file when they disagree on
**spoke** facts until a rereview changes those ADRs.
