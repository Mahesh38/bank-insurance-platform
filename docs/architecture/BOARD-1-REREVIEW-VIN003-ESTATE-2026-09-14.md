# Board 1 — Architecture rereview of R0 against the live AU bank cloud estate

**Decision id:** `ARCH-DEC-VIN003-B1`
**Workstream:** WS-3 · **Horizon:** H0 — R0 as designed · **Stage:** S08 with S09 overlapped
**Subject:** Does the R0 AWS / landing-zone shape still hold now that Mahesh has ingested the
bank's Control Tower, EDGE, SOP and onboarding artefacts (`VIN-003`)?
**Board:** 1 — Architecture · **Persona:** Mahesh — Principal Insurance Platform Architect (R2)
**Reviewer type:** `AGENT` · **Self-review:** false (the R0 LLD and ADRs pre-date this intake)
**Change tier:** T4 — G8 (production topology / trust boundary) already applies to `ADR-009` /
`ADR-010` / `ADR-018`; this rereview does not move those boundaries, it tests them against
organisation standard
**Date:** 2026-09-14
**Authority class:** `A3_JOINT_REVIEW` on remaining conditions · `A4_HUMAN_REQUIRED` for the
mandatory T4 Architecture signature

> ## Verdict: `APPROVED_WITH_CONDITIONS`
> **Architecture severity:** `A1` — one major open structural question (whether R0 still owns a
> per-environment AWS Network Firewall when the hub FortiGate already inspects), plus environment
> and onboarding mismatches. **Nothing is `A0`.** The spoke-not-clone principle is already in
> `ADR-009` and `ADR-018`. This rereview does not repeal them.
>
> **`signature_status: AI-DRAFTED — mandatory human T4 Architecture signature outstanding`**
> [`11-REVIEW_GATES.md §2`](../governance/11-REVIEW_GATES.md#2-who-may-sit-on-a-board) is binding.
> An agent may draft the reasoning. It may not manufacture Mahesh's human signature, Deepali's
> Security acceptance of `ADR-010`, or bank Cloud's account-vending approval.

This is a **Board 1 draft**. It is not an instruction to apply Terraform, not a waiver of
`ASM-012` / `ASM-013`, and not a change to stage state.

---

## 1. Decision requested

Walk the ten estate questions in
[`18 §11`](../context/roles/mahesh-principal-insurance-platform-architect/18-au-bank-enterprise-cloud-estate.md)
against the R0 spoke as specified in `R0-LLD.md`, `03-solution-architecture-r0.md` §4, and
`ADR-009` / `ADR-010` / `ADR-018`. For each: **preserved by construction**, **at risk with a
control**, or **open — named owner and date**.

What this rereview does **not** do: rewrite an ADR, invent FortiGate policy, classify the
application's RIA tier, or close `SPIKE-001`.

---

## 2. What I actually opened (Rule RG-3)

| Source | What it answered |
|---|---|
| [`mahesh-architecture.card.md`](../context/personas/mahesh-architecture.card.md) | Board 1 persona; rule 12 `BE-01` |
| [`11-REVIEW_GATES.md` §4 and §11](../governance/11-REVIEW_GATES.md#4-board-1--architecture) | A1–A10; verdict meanings |
| [`07-review-and-evidence-contract.md`](../context/roles/mahesh-principal-insurance-platform-architect/07-review-and-evidence-contract.md) | Response shape |
| [`18-au-bank-enterprise-cloud-estate.md`](../context/roles/mahesh-principal-insurance-platform-architect/18-au-bank-enterprise-cloud-estate.md) | `BE-01`–`BE-15`; X-1…X-6 |
| [`VIN-003` transcription](../au-bank-insurance-platform/references/2026-09-14-au-bank-enterprise-cloud-estate-notes.md) | Control Tower, EDGE, SOP v1.7, Onboarding v1.4, S3 hosting, three diagrams |
| [`ADR-009`](../platform/architecture-review/08-architecture-decision-log.md) | Attach to existing TGW / existing DXGW; no second hub |
| [`ADR-010`](../platform/architecture-review/08-architecture-decision-log.md) | Per-environment inspection VPC + AWS Network Firewall; FortiGate rejected for R0 **operational surface**, with revisit if bank NGFW is mandated |
| [`ADR-018`](../platform/architecture-review/08-architecture-decision-log.md) | Cloudflare → F5-XC (SaaS) → API Gateway → Internal ALB; no public ALB |
| [`R0-LLD.md` §1.4, §2, §2.2, §2.3, §3](./R0-LLD.md) | Env shapes, VPC, hybrid path, egress, reverse proxy |
| [`03-solution-architecture-r0.md` §4 deployment properties](../platform/ws3-platform/03-solution-architecture-r0.md) | Authoritative R0 deployment rows |
| [`DECISION-REGISTER.md`](../governance/registers/DECISION-REGISTER.md) ADR-009/010/016/018 rows | Binding decision index |
| [`ASSUMPTION-REGISTER.md`](../governance/registers/ASSUMPTION-REGISTER.md) `ASM-012`, `ASM-013`, `ASM-009` | Open assumptions this rereview must not pretend are closed |
| [`BOOT.md`](../context/BOOT.md) posture | S08 in progress, S09 next; standing constraints |

Not opened, and therefore **not assumed**: FortiGate rulebase, TGW route tables, subnet-schema
workbooks, Cloud service qualification document, IS baseline, the application's RIA ID.

---

## 3. Estate questions — the walk

| # | Question (`18 §11`) | Result | Severity |
|---|---|---|---|
| 1 | Spoke attachment or a second hub? | **Preserved.** `ADR-009` already says attach to `AU-CTO-NETWORK`; do not provision a second TGW or a second DX. Programme `network` account is RAM-share + inspection VPC, not a parallel hub | — |
| 2 | North-south still Cloudflare → F5-XC without a cloned Public ALB? | **Preserved.** `ADR-018` is the correct reading of diagrams D2/D3: the neighbour has a Public ALB; NIP does not copy it (`BE-03`, `BE-15`) | — |
| 3 | East-west / DC still TGW → FortiGate → DX? | **At risk until `ASM-012` closes.** Bank-directed traffic uses the hub (`ADR-009`). Internet egress is designed to a **spoke** AWS Network Firewall (`ADR-010`), which may double-inspect or bypass EDGE FortiGate depending on how the TGW is actually routed | `A1` · F-02 |
| 4 | Workforce entry still Bank AD / IAM Identity Center? | **Preserved in principle (`TI-01`).** Cloud-console SSO groups (`aws secure login`, `SSO-PLATFORM-USERS`) are operator entry, not NIP authorization. Federation **path** is the `ADR-009` attachment; IdP product remains deferred (BOOT WS-2) | — |
| 5 | Region-pinned resources stay in India? | **Preserved.** `TI-08` + org SCP geo-location. Primary `ap-south-1`, DR `ap-south-2` | — |
| 6 | Buckets private? Cloudflare-origin only via SOP exception? | **Preserved.** R0: no public S3 website; Flutter assets via nip-web through Cloudflare → F5-XC. WORM evidence stays private CMK. Do not invent a Cloudflare+SSE-S3 origin for R0 | — |
| 7 | Bank environments: is split `dev` justified? | **At risk.** Onboarding v1.4 default is Prod + CUG + UAT, with Dev **inside UAT**. `R0-LLD` BOM #1 provisions a separate `dev` account. That is a Cloud-team exception, not a bank default (`BE-12`) | `A1` · F-03 |
| 8 | Application tier declared, AZ/DR matching it? | **Partially preserved.** Multi-AZ in `ap-south-1` for `uat`/`prod` stateful stores meets SOP Multi-AZ for Tier 1/2. Region-level **Active-Active** in the SOP is **not** what R0 specifies (warm standby `ap-south-2`). Silent upgrade is out of scope | `A2` · F-05 |
| 9 | Unqualified new AWS/SaaS? | **No `A0`.** EKS, Aurora PostgreSQL, GitLab, Terraform, Cloudflare, F5-XC are already on the estate (S1, D3, SOP DB menu). AWS Network Firewall is the deliberate R0 alternative to another FortiGate pair (`ADR-010` alternatives). Qualification document itself was not supplied | `A2` |
| 10 | Deepali / Shivanshi / Aarti / bank Cloud named where the hub is touched? | **Named, not closed.** `ADR-010` still needs Deepali **acceptance**. `DEP-20260824-dx1` and `ASM-012` still need bank network. C-02 needs Cloud (Manish / Control Tower) | — |

---

## 4. Board 1 checklist A1–A10

Applied to the **R0 landing-zone shape as it stands after `VIN-003`**, not to a new feature.

| Check | Finding |
|---|---|
| **A1** Boundaries | Spoke vs hub is respected in `ADR-009` / `ADR-018`. Inspection-VPC vs EDGE FortiGate is the remaining boundary blur (F-02) |
| **A2** Responsibility | Mahesh owns spoke structure; bank Cloud owns Control Tower vending; bank network owns TGW/FortiGate/DX; Shivanshi attaches; Deepali owns the security outcome of egress inspection. No impersonation in this draft |
| **A3** Coupling | Attaching to the existing TGW is **justified, directional** coupling to the bank hub. Cloning the neighbour Public ALB would be unjustified coupling — already refused |
| **A4** Principles | `BE-01`, `TI-01`, `TI-08`, standing constraint "bank apps never call 1SB or a database directly" all hold. X-1 (env model) is the principle most at risk |
| **A5** ADR | The three topology ADRs exist and are the right ones. **No new ADR in this rereview.** Closing F-02 may later amend `ADR-010`; that is a subsequent change, not this verdict |
| **A6** Unnecessary infrastructure | Two candidates: (1) per-environment Network Firewall **if** bank mandates EDGE-only egress — open, not proven waste; (2) **reserved empty public subnets in workload VPCs** after Public ALB was withdrawn — leftover (F-01) |
| **A7** Migration | Attaching as a spoke is cheap to keep. A second TGW would be expensive to unwind — already forbidden. Publishing 1SB/PG allowlists from the wrong EIP set remains the load-bearing migration hazard (`ADR-010`, `DEP-20260824-eip`) |
| **A8** Stage fit | S08/S09 overlapped. This rereview belongs **before** first `apply` to a non-dev account. It does not import H1/H2 mechanisms |
| **A9** Smallest change | The smallest structural change that satisfies `VIN-003` is: **keep `ADR-009` and `ADR-018`; condition `ADR-010` on `ASM-012`; condition account vending on Cloud onboarding.** Not a redesign |
| **A10** Replacement cost | Replacing Amazon API Gateway with Apigee later is bounded (`ASM-013` / `SPIKE-001`). Replacing spoke Network Firewall with EDGE FortiGate is the `ADR-010` revisit trigger and is why C-01 exists **before** EIP publication |

---

## 5. Findings

### F-01 · `A2` · Workload-VPC public subnets are a leftover

`R0-LLD` §2 still reserves empty public /24s in every **workload** VPC "because an ALB cannot
exist in an AZ with no subnet". After `ADR-018` the only ALB is **internal** and belongs in
private-app subnets. The only VPC that still needs public subnets is the **inspection** VPC (NAT
+ IGW).

Bank SOP: a public subnet requires **IS approval**. Holding unused public subnets in the spoke
is unnecessary infrastructure (A6) and an avoidable IS conversation.

**Disposition:** `should_fix` — remove reserved public subnets from workload VPCs in the next
`R0-LLD` edit. Inspection-VPC public subnets stay.

### F-02 · `A1` · Spoke Network Firewall vs hub FortiGate (`ASM-012`)

`VIN-003` D1: FortiGate Active/Passive in EDGE VPC inspects east-west and internet egress for
the org. `ADR-010` still places AWS Network Firewall in a per-environment inspection VPC and
publishes **those** NAT EIPs to 1SB and the PG.

`ADR-010`'s own alternative already named FortiGate and rejected it for R0 on **operator
surface**, with revisit trigger *"a bank network standard mandating the enterprise NGFW
platform"* — which is exactly what `VIN-003` now documents as the hub standard.

This is **not** grounds to withdraw `ADR-010` from a persona file. Deepali owns acceptance of
that control. It **is** grounds to refuse Terraform that publishes EIPs until bank network
answers: share EDGE only, or keep spoke inspection **in addition**.

**Disposition:** condition C-01. `ASM-012` expiry (S09 network build start) is the date.

### F-03 · `A1` · Separate `dev` account vs bank onboarding default

Onboarding v1.4: Prod, CUG, UAT are mandatory; Dev/SIT **shall not** be provisioned separately
by default; they live in UAT with tags/RBAC unless Cloud approves a split.

`R0-LLD` §1.4 **needs** a weaker-than-prod `dev` shape (stubs allowed, single-AZ, MSK
Serverless). That is a real architecture reason. It is still a **Cloud-team exception**, not
something Mahesh can silently vendor through Control Tower.

**Disposition:** condition C-02. Two acceptable outcomes: (a) documented exception, split `dev`
kept; (b) `dev` shape hosted inside UAT with stubs fenced by account policy / tags, LLD amended.

### F-04 · `A2` · CUG is unnamed

Bank default includes a Closed User Group environment. R0 BOM has `dev` / `uat` / `prod` and no
CUG. CUG is not "uat with a flag".

**Disposition:** condition C-03. Either Cloud acknowledges "R0 has no CUG; UAT is the closed
path" or a CUG account is added with a problem statement. Do not invent a fourth production.

### F-05 · `A2` · Warm standby vs SOP Active-Active language

SOP: Tier 1/2 cloud apps are Active-Active; failover tests rather than classic DR. R0: Multi-AZ
in Mumbai plus warm standby in Hyderabad (`R0-LLD` §11). Multi-AZ **inside** `ap-south-1` already
satisfies the SOP Multi-AZ mandate. Cross-region Active-Active is a different system and a
different cost envelope (`RISK-012`).

**Disposition:** condition C-04. Obtain RIA tier before anyone "fixes" DR to Active-Active.

### F-06 · none · Aurora PostgreSQL is qualified enough

Landing-zone slide lists RDS MySQL and MongoDB as top consumed PaaS. SOP database menu includes
**Postgres**. Diagram D3 already runs Aurora PostgreSQL. `ADR-008` stands. X-5 in `18 §12` is
closed as **not a conflict**.

### Confirmed, not findings

- `ADR-009` attach-don't-clone matches D1. **Keep.**
- `ADR-018` no Public ALB matches `BE-03` / `BE-15`. **Keep.** Neighbour D2 remains a neighbour.
- No public S3 website matches SOP + S4. **Keep.**
- India regions only matches org SCP. **Keep.**

---

## 6. Conditions (become acceptance criteria)

These are **blocking for first `apply` to a non-dev account / for Control Tower vending**, not
for continuing S08 application engineering.

| ID | Condition | Owner | Close by |
|---|---|---|---|
| **C-01** | Written answer from bank network on `ASM-012`: spoke keeps its own inspection VPC + EIPs, **or** egress is EDGE FortiGate only (then `ADR-010` is amended and 1SB/PG allowlists rebase **before** any EIP is published). Deepali accepts whichever control remains | Shivanshi + bank network; Deepali accepts | S09 network build start (`ASM-012` expiry) |
| **C-02** | Cloud-team written exception for a split `dev` AWS account, **or** LLD amended so the `dev` shape lives inside UAT per Onboarding v1.4 | Shivanshi + bank Cloud (Manish / Control Tower) | Before the Control Tower account request |
| **C-03** | CUG named: waiver ("no CUG at R0") or an account with a problem statement | Shivanshi + Mahesh | Same gate as C-02 |
| **C-04** | Application RIA ID / tier recorded. Do not change R0 from warm-standby `ap-south-2` to Active-Active without that fact plus Aarti and Shivanshi | Application team + Cloud; Aarti on persistence | Before `GATE-S09` DR evidence |
| **C-05** | Onboarding pack exists: RIA ID, infra diagram (`R0-LLD` + topology SVGs), infra requirement sheet, cost approval to `@psg.cloud`, account-hardening against Prisma/SCP | Shivanshi | Before account create |

`ADR-018` and `ADR-009` need **no** amending condition. `SPIKE-001` / `ASM-013` (Apigee) stays
the existing parked spike — not restated as a new condition.

### should_fix (non-blocking)

| ID | Item | Owner |
|---|---|---|
| S-01 | Remove reserved empty **workload** public subnets from `R0-LLD` §2 / §2.1 (F-01) | Mahesh (LLD edit) + Shivanshi (IaC) |
| S-02 | UAT run-hours 12h/day vs CI / IdP assumptions — operating calendar, not a topology change | Shivanshi |
| S-03 | Say explicitly that Prisma + QRadar + 10-year CloudTrail are **inherited org controls**; programme GuardDuty/Security Hub/Config in the `security` account are additive. Deepali confirms overlap | Deepali |

No `must_fix` that would return this rereview as `REWORK`. The plan is the right **kind**.

---

## 7. What this draft may not do

- Impersonate human T4 Architecture sign-off.
- Accept `ADR-010` on Deepali's behalf, or withdraw it without her.
- Treat bank Cloud silence as approval of a split `dev` account.
- Upgrade R0 to Active-Active, add a CUG, or collapse `dev` into UAT in the same change as this
  document.
- Edit `CURRENT-STATE.yaml` stage fields.
- Close `ASM-012`, `ASM-013`, `DEP-20260824-dx1` or `DEP-20260824-eip` from a review file.

---

## 8. Next safe action

1. Human Mahesh adopts, amends or rejects this draft (`A4`).
2. Shivanshi takes C-01 / C-02 / C-05 to bank network and Cloud **before** Terraform that
   creates accounts or publishes EIPs.
3. S-01 (drop leftover public subnets) is a small LLD edit — do it in a follow-up, not by
   silently widening this file.
4. S08 application engineering continues; it does not need these conditions to write Java.

---

```yaml
# schema: review-verdict
board: ARCHITECTURE
plan: R0-LLD / ADR-009 / ADR-010 / ADR-018
work_item: ARCH-DEC-VIN003-B1
reviewer: "Mahesh — Principal Insurance Platform Architect"
reviewer_type: AGENT
self_review: false
date: "2026-09-14"
decision: APPROVED_WITH_CONDITIONS
architecture_severity: A1
authority_class: A4_HUMAN_REQUIRED
must_fix: []
conditions:
  - "C-01 ASM-012 closed in writing before any EIP is published (spoke NFW vs EDGE FortiGate); Deepali accepts the remaining control"
  - "C-02 Cloud-team exception for split dev account, or LLD hosts the dev shape inside UAT"
  - "C-03 CUG named (waiver or account) before Control Tower vending"
  - "C-04 RIA tier recorded; no silent Active-Active upgrade"
  - "C-05 Onboarding pack (RIA, diagram, cost sheet, hardening) before account create"
should_fix:
  - "S-01 Remove leftover public subnets from workload VPCs (F-01)"
  - "S-02 UAT 12h/day operating calendar"
  - "S-03 Prisma/QRadar/10y CloudTrail are inherited; GuardDuty/Security Hub are additive"
evidence:
  - "A1-A10 walked against ADR-009, ADR-010, ADR-018, R0-LLD §1.4 §2 §2.2 §2.3 §3, 03-solution-architecture-r0.md §4"
  - "18 §11 ten estate questions scored against VIN-003 (Landing Zone v1.1, Onboarding v1.4, SOP v1.7, S3 hosting v1.1, diagrams D1-D3)"
  - "ASM-012, ASM-013, DEP-20260824-dx1, DEP-20260824-eip confirmed still OPEN — not closed by this file"
  - "X-2 / ADR-018 Public ALB refusal confirmed (BE-15); X-5 Aurora PostgreSQL confirmed not a conflict"
notes: >
  Spoke-not-clone already encoded. Remaining A1 is inspection-plane ownership and
  environment vending against bank Cloud SOP. Draft only; T4 human signature outstanding.
```
