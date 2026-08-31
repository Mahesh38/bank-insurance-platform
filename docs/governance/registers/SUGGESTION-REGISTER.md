# Suggestion Register

**Every input that could become work gets a row here — admitted, parked, rejected, or escalated.**
Nothing is dropped. Nothing is deleted.

**Owner:** whichever agent or person triaged the input
**ID format:** `SUG-<YYYYMMDD>-<3 chars from 0-9a-z>` — collision-resistant, no shared counter.
Rules: [../state/CURRENT-STATE.yaml](../state/CURRENT-STATE.yaml) `id_allocation`
**Rules:** [08-BACKLOG_RULES.md](../08-BACKLOG_RULES.md) · [09-AI_EXECUTION_RULES.md](../09-AI_EXECUTION_RULES.md)

---

## How to add a row

1. Mint an ID: `SUG-<today>-<3 random chars>`, e.g. `SUG-20260812-a1b`. No counter to
   increment and no merge conflict when two branches triage at once.
2. Run pipeline steps 2–5 ([09 §2](../09-AI_EXECUTION_RULES.md#2-the-mandatory-sequence)).
3. Add the summary row below.
4. For anything beyond a trivial reject, add a detail block in §3 using
   [../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).
5. **Check for duplicates first** (Rule CS-2). A repeat is linked and increments
   `recurrence_count` — it is not a new row.

---

## 1. Status vocabulary

| Status | Meaning |
|--------|---------|
| `ADMITTED` | Entered a backlog for the current stage |
| `ADMIT-BYPASS` | Implemented under a human override of the process ([09 §8](../09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process)) |
| `PARKED` | Real work, later stage — see [PARKED-BACKLOG.md](./PARKED-BACKLOG.md) |
| `ESCALATED` | Awaiting a human decision as `CR-###` |
| `REJECTED` | Will not be done; reason recorded |
| `DUPLICATE` | Already tracked; linked |
| `SUPERSEDED` | Overtaken by another decision; linked |
| `LAPSED` | Idea closed by aging (AS-3) |
| `CLOSED-DELIVERED` | Admitted and shipped; linked to the PR |

---

## 2. Register

| ID | Date | Source | Summary | SF | SC | Necessity | Type | P now / target | Action | Ref |
|----|------|--------|---------|----|----|-----------|------|----------------|--------|-----|
| SUG-20260818-4c3 | 2026-08-18 | human:Mahesh | Architecture justification pack: why service boundaries, merge rejection, datastore choices, caching, direct-insurer future, R0/R1/R2+ scope. **Aligned 2026-08-25** to `CR-012`/`ADR-008`…`ADR-013`; cites the canonical renderings rather than publishing a second pair (`HA-04`) | SF1 | SC0 | MUST | ARCH | P2 / P2 | ADMIT-BYPASS | [06-architecture-justification](../../platform/ws3-platform/06-architecture-justification-and-review-answers.md) |
| SUG-20260816-d8v | 2026-08-16 | human:Mahesh | Add Dilip AI executive-sponsor perspective for bancassurance business/value decisions and wire it into P0/R0 | SF2 | SC1 | SHOULD | GOV | P2 / P2 | ADMIT-BYPASS | [1SB backlog governance/decision-quality enablers](../../1sb-insurance-integration/service-ssot/PRODUCT-BACKLOG.md#governance--decision-quality-enablers) |
| SUG-20260816-ba7 | 2026-08-16 | human:Mahesh | Add a senior end-to-end bancassurance BA AI persona for existing R11 and link it to current personas/context | SF2 | SC1 | SHOULD | GOV | P2 / P2 | ADMIT-BYPASS | [Principal BA package](../../context/roles/principal-insurance-platform-business-analyst/README.md) |
| SUG-20260816-ap1 | 2026-08-16 | human:Mahesh | Build a reusable context module and safe evidence-driven autopilot; reconcile semantic governance drift and documentation structure | SF1 | SC1 | MUST | GOV | P2 / P2 | ADMITTED | [CR-010](../change-requests/CR-010-context-module-and-safe-autopilot.md) |
| SUG-20260816-w3s | 2026-08-16 | agent:claude | Extend `current-state.schema.json` with `depends_on` / `entry_conditions` on a workstream and `parent_workstream` / `delivers_bounded_contexts` on `lifecycle`, so workstream relationships are validated rather than held in comments | SF2 | SC1 | SHOULD | GOV | P3 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#1-parked--scheduled-work) |
| SUG-20260820-n5t | 2026-08-20 | human:Mahesh | Redraw `docs/hdl.svg` as the release-coded North Star HLD — boundary descriptions, LOB segregation, aggregation/provider layer, mature-platform capabilities, R0→RN phasing — and preserve the R0 view alongside it | SF2 | SC1 | SHOULD | DOC | P3 / P3 | ADMIT-BYPASS | [architecture diagrams](../../architecture/README.md) |
| SUG-20260820-hr0 | 2026-08-20 | human:Mahesh | HLD review round: correct the R0 actor model to two actors with SP as a certification attribute, gate and insurer-scope the Insurance Partner Representative, make the opportunity the single RM-only origination point, make LOB first-class from release 1 and make the configuration layer ship in R0 independently of any admin UI | SF0 | SC0 | MUST | ARCH | P1 / P1 | ADMIT-BYPASS | [ADR-004…007](../../platform/architecture-review/08-architecture-decision-log.md) |
| SUG-20260820-r1t | 2026-08-20 | agent:claude | Produce the R0 → R1 → R2 transition and dependency map the North Star does not answer: the order in which target components must appear and which are prerequisites for which | SF3 | SC1 | SHOULD | ARCH | P4 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#1-parked--scheduled-work) |
| SUG-20260820-al7 | 2026-08-20 | human:Mahesh | Reconcile the North Star and R0 diagrams: one naming and layer convention across both files, the R0 view redrawn on the North Star's boundary bands so it reads as a release-zero cut of the same picture, and the Life LOB cell visually separated from the shared platform | SF1 | SC1 | MUST | ARCH | P2 / P2 | ADMITTED | [detail](#sug-20260820-al7--hld-and-r0-diagram-alignment) |
| SUG-20260820-dc4 | 2026-08-20 | human:Mahesh | Resolve OPEN-A1 and OPEN-D10: physical database topology is an evidence-led decision, not a principle — R0 starts as one cluster with a schema per context and splits later along the LOB-cell / shared-platform seam; and context #5 is named Opportunity, because a lead is too thin to carry renewal, lapse and cross-sell demand | SF1 | SC1 | MUST | ARCH | P2 / P2 | ADMITTED | [ADR-008](../../platform/architecture-review/08-architecture-decision-log.md) · recurrence_count 2 (2026-08-25: rename Opportunity → Lead; not a new row — see [detail](#sug-20260820-dc4--data-topology-and-the-name-of-context-5)) |
| SUG-20260820-hl1 | 2026-08-20 | human:Mahesh | Act as Mahesh: turn the R0 reference architecture SVG into a detailed HLD (domain, boundary, communication, API, business logic, phases/waves/what-to-do-when) and an LLD for the CTO and AWS platform team (e2e components, services, AWS, VPC, reverse proxy, PVC, DB, cache) | SF1 | SC0 | MUST | ARCH | P2 / P1 | ADMIT-BYPASS | [R0-HLD](../../architecture/R0-HLD.md) · [R0-LLD](../../architecture/R0-LLD.md) |
| SUG-20260820-ls1 | 2026-08-20 | human:Mahesh | Create an SVG rendering of the R0 LLD for the CTO and AWS platform team | SF1 | SC0 | SHOULD | ARCH | P2 / P1 | ADMIT-BYPASS | [r0-lld.svg](../../architecture/r0-lld.svg) |
| SUG-20260820-pt9 | 2026-08-20 | human:Mahesh | Draw the AWS platform-team application view: what the application is, the service inventory, availability-zone placement, the DR bill of materials, the reverse-proxy and egress chain, and **when** each resource is needed — as a deployment topology in the style of a landing-zone request diagram | SF1 | SC0 | MUST | ARCH | P2 / P1 | ADMIT-BYPASS | [R0-LLD §2.1/§11.1/§12.1](../../architecture/R0-LLD.md) · rendering superseded by [SUG-20260820-ic3](#sug-20260820-ic3--icon-notation-generated-from-code) |
| SUG-20260820-ic3 | 2026-08-20 | human:Mahesh | Redraw the platform-team views in AWS / Kubernetes icon notation instead of labelled rectangles, and generate them from code rather than hand-authoring SVG | SF1 | SC0 | SHOULD | DOC | P3 / P2 | ADMIT-BYPASS | [diagrams/](../../architecture/diagrams/README.md) |
| SUG-20260820-lay4 | 2026-08-20 | human:Mahesh | Keep the icons, drop the layout engine: place every element on a chosen grid and route every connector orthogonally, so the views are aligned and the links are straight | SF1 | SC0 | SHOULD | DOC | P3 / P2 | ADMIT-BYPASS | [diagrams/](../../architecture/diagrams/README.md) |
| SUG-20260820-cm2 | 2026-08-20 | human:Mahesh | Close the context-architecture gap found by audit: 20 documents unreachable by any link and 96 more at 3+ hops, 22 persona-package files no card routed to, and no CI guard against either. Add a generated document-routing map (`DOC-MAP.yaml`) with a `find` query path, complete the persona `Load deeper` tables, consolidate the READMEs that carry no unique content, and fail CI on an unrouted document | SF1 | SC1 | MUST | GOV | P2 / P2 | ADMIT-BYPASS | [DOC-MAP](../../context/DOC-MAP.yaml) · [doc_routing](../../context/AGENT-CONTEXT-INDEX.yaml) · continues [CR-010](../change-requests/CR-010-context-module-and-safe-autopilot.md) |
| SUG-20260821-jx1 | 2026-08-21 | human:Mahesh | Produce an end-to-end Journey Execution Specification for R0: every actor use case, the hop-by-hop route of each request across edge/BFF/service/aggregate/persistence, the validation performed at each layer with its algorithm, every external API call, and every possible outcome — assembled for the dev and QA teams | SF1 | SC1 | SHOULD | DOC | P3 / P1 | ADMITTED | [detail](#sug-20260821-jx1--r0-journey-execution-specification) |
| SUG-20260821-jx2 | 2026-08-21 | human:Mahesh | Extend the Journey Execution Specification beyond R0 to the whole application — DIY/customer journey, hybrid mode switching, Group B insurers, ULIP/Savings, Health/Motor/Travel, renewals and servicing. Admin UI and Reporting/MIS carved into R0 by CR-013 | SF3 | SC2 | NOT-NOW | DOC | P5 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#1-parked--scheduled-work) · recurrence_count 3 (2026-08-25: admin/MIS timing overridden by CR-013; those surfaces left this parked bag) |
| SUG-20260824-gp1 | 2026-08-24 | human:Mahesh | Provision hybrid bank connectivity in R0 instead of deferring it: a Transit Gateway hub with Site-to-Site VPN from day one and Direct Connect as the primary path when the circuit lands, so CBS and Bank AD are reached over a private path in `uat` and `prod` rather than stubbed | SF1 | SC0 | MUST | INFRA | P1 / P1 | ADMIT-BYPASS | [ADR-009](../../platform/architecture-review/08-architecture-decision-log.md) · [CR-012](../change-requests/CR-012-r0-platform-robustness.md) · [detail](#sug-20260824-gp1--hybrid-bank-connectivity-in-r0) |
| SUG-20260824-gp2 | 2026-08-24 | human:Mahesh | Inspect egress in R0: a per-environment inspection VPC with AWS Network Firewall, domain allowlisting and IPS, with the 1SB/PG-allowlisted Elastic IPs moved behind it — closing unrestricted 443 egress from a platform holding PAN, income and health attributes | SF1 | SC0 | MUST | SEC | P1 / P1 | ADMIT-BYPASS | [ADR-010](../../platform/architecture-review/08-architecture-decision-log.md) · [CR-012](../change-requests/CR-012-r0-platform-robustness.md) · [detail](#sug-20260824-gp2--centralised-egress-inspection-in-r0) |
| SUG-20260824-gp3 | 2026-08-24 | human:Mahesh | Provision a managed cache tier in R0 (ElastiCache for Valkey) for BFF sessions, an L2 read-through layer and per-principal rate limiting — resolving the published DynamoDB-versus-Redis session contradiction and the per-pod configuration divergence, while refusing idempotency | SF1 | SC0 | MUST | INFRA | P2 / P1 | ADMIT-BYPASS | [ADR-011](../../platform/architecture-review/08-architecture-decision-log.md) · [CR-012](../change-requests/CR-012-r0-platform-robustness.md) · [detail](#sug-20260824-gp3--managed-cache-tier-in-r0) |
| SUG-20260824-gp4 | 2026-08-24 | human:Mahesh | Provision Amazon MSK as the R0 event backbone and **keep the transactional outbox as its source of truth**, because the previous decision's own revisit trigger (a third consumer class) fires inside R0 and adopting a broker mid-slice would change the audit path while it is being evidenced | SF1 | SC0 | MUST | ARCH | P2 / P1 | ADMIT-BYPASS | [ADR-012](../../platform/architecture-review/08-architecture-decision-log.md) · [CR-012](../change-requests/CR-012-r0-platform-robustness.md) · [detail](#sug-20260824-gp4--event-backbone-in-r0-with-the-outbox-retained) |
| SUG-20260824-gp5 | 2026-08-24 | human:Mahesh | Provision Amazon OpenSearch in R0 as the operational search and log-analytics pipe — with an explicit exclusion from the regulatory pipe — so the firewall, flow, TGW and broker logs the other closures generate are queryable during the first end-to-end journey | SF1 | SC0 | SHOULD | INFRA | P2 / P2 | ADMIT-BYPASS | [ADR-013](../../platform/architecture-review/08-architecture-decision-log.md) · [CR-012](../change-requests/CR-012-r0-platform-robustness.md) · [detail](#sug-20260824-gp5--operational-search-pipe-in-r0) |
| SUG-20260825-db1 | 2026-08-25 | human:Aarti-request | Create the Aarti DB design pack: rules, physical schema, tables, relationships, indexes, required routines, troubleshooting plan, and SQL scripts for each R0 schema | SF1 | SC0 | MUST | ARCH | P2 / P1 | ADMITTED | [DATA-001](../../platform/data-architecture/DATA-001.work-item.yaml) · [detail](#sug-20260825-db1--aarti-r0-physical-data-architecture-pack) |
| SUG-20260825-aln | 2026-08-25 | human:Aarti-request | Check whether the R0 physical DB pack is aligned with recent scope changes (CR-013) and what still needs to be created | SF1 | SC0 | MUST | ARCH | P2 / P1 | ADMITTED | [DATA-002](../../platform/data-architecture/DATA-002.work-item.yaml) · [alignment](../../platform/data-architecture/DATA-002-cr013-alignment.md) · [detail](#sug-20260825-aln--cr-013-physical-alignment) |
| SUG-20260825-lt1 | 2026-08-25 | human:Mahesh | After convert + payment, close the working Lead and keep 7-year evidence on Payment, Policy and issuance history — do not retain every Lead for 7 years | SF1 | SC0 | MUST | ARCH | P1 / P1 | ADMITTED | [CR-013](../change-requests/CR-013-r0-lead-mis-admin-scope.md) · [ADR-014](../../platform/architecture-review/08-architecture-decision-log.md) |
| SUG-20260825-of1 | 2026-08-25 | human:Mahesh | MIS upload of products sold offline or on insurer portals so the platform sees the full book and which products still need onboarding | SF1 | SC0 | MUST | FUNC | P1 / P1 | ADMITTED | [CR-013](../change-requests/CR-013-r0-lead-mis-admin-scope.md) · [ADR-014](../../platform/architecture-review/08-architecture-decision-log.md) |
| SUG-20260825-st1 | 2026-08-25 | human:Mahesh | Align issuance with STP, non-STP and Insta as first-class modes on Proposal/Policy — not as Lead states | SF1 | SC0 | MUST | ARCH | P1 / P1 | ADMITTED | [CR-013](../change-requests/CR-013-r0-lead-mis-admin-scope.md) · [ADR-014](../../platform/architecture-review/08-architecture-decision-log.md) |
| SUG-20260825-pp1 | 2026-08-25 | human:Mahesh | Confirm the R0 design against IRDAI Protection of Policyholders' Interests (PPHI) — interpretation is Board 6, not Architecture | SF1 | SC0 | MUST | COMP | P1 / P1 | ADMITTED | [CR-013](../change-requests/CR-013-r0-lead-mis-admin-scope.md) · condition C-PPHI-1 |
| SUG-20260825-wl1 | 2026-08-25 | human:Mahesh | Isolate operations / MIS workload from the RM and Lead transactional path | SF4 | SC0 | REJECT | ARCH | — / — | REJECTED | already the ratified design — [03-communication-patterns.md](../../platform/architecture-review/03-communication-patterns.md) · [05-data-architecture.md](../../platform/architecture-review/05-data-architecture.md) · [detail](#sug-20260825-wl1--oltp-vs-ops-isolation-already-decided) |
| SUG-20260825-df1 | 2026-08-25 | human:Mahesh | Authorise every persona to decide the lead-domain intake and write one decision file | SF1 | SC1 | MUST | DOC | P2 / P2 | ADMIT-BYPASS | [DEC-20260825-01](../DEC-20260825-01-lead-domain-decisions.md) · [detail](#sug-20260825-df1--one-decision-file) |
| SUG-20260825-r0s | 2026-08-25 | human:Mahesh | Stakeholder: include the lead-domain intake in R0 immediately; nothing parked; compliance calls only | SF0 | SC0 | MUST | ARCH | P1 / P1 | ADMIT-BYPASS | [CR-013](../change-requests/CR-013-r0-lead-mis-admin-scope.md) · [ADR-014](../../platform/architecture-review/08-architecture-decision-log.md) · recurrence_count 2 (2026-08-25: Admin/Config BFF + admin/ops actors for MIS — already D4) |
| SUG-20260825-pv1 | 2026-08-25 | human:Mahesh | Deploy the desktop web application on a Kubernetes PVC | SF4 | SC3 | REJECT | INFRA | — / — | REJECTED | [detail](#sug-20260825-pv1--no-pvc-for-the-web-app) |
| SUG-20260825-ld1 | 2026-08-25 | human:Mahesh | Make Lead LOB-specific | SF4 | SC3 | REJECT | ARCH | — / — | REJECTED | [detail](#sug-20260825-ld1--lead-is-not-lob-specific) |
| SUG-20260825-st2 | 2026-08-25 | human:Mahesh | Put the Flutter RM app on Play Store / Apple Store (and a customer store app) | SF1 | SC1 | MUST | ARCH | P2 / P1 | CLOSED-DELIVERED | Unparked by `ADR-015`: workforce distribution is EKS web + Play APK + App Store IPA. Customer store apps remain R1 (`#1`). Deepali owns store hardening. |
| SUG-20260827-err | 2026-08-27 | human:Mahesh | Mature the shared utility libraries, error handling first: a standard cross-service error contract that identifies the emitting and originating service, gives end users a safe message while developers and L1/L2 support get a complete diagnostic, replaces ad-hoc per-throw-site wording with a registry seeded from catalogue 04, and emits one consistently tagged series so error dashboards are buildable | SF1 | SC1 | MUST | ARCH | P2 / P1 | ADMIT | [07-PLATFORM-ERROR-CONTRACT](../../journey-execution/07-PLATFORM-ERROR-CONTRACT.md) · [ADR-017](../../platform/architecture-review/08-architecture-decision-log.md) · [detail](#sug-20260827-err--platform-error-contract) |
| SUG-20260825-ac1 | 2026-08-25 | human:Mahesh | Add admin and operations as R0 on-platform actors for the Admin & Configuration BFF, reports and MIS | SF1 | SC0 | MUST | ARCH | P2 / P1 | ADMITTED | [detail](#sug-20260825-ac1--admin-and-ops-actors-for-r0) · UI is NIP-APP roles (`SUG-20260825-nip`), not a second app |
| SUG-20260825-ll1 | 2026-08-25 | human:Mahesh | Reconcile R0-LLD and platform topology with ADR-014: Admin BFF, #18 MIS, desktop admin web, no PVC | SF1 | SC1 | MUST | DOC | P3 / P2 | CLOSED-DELIVERED | [detail](#sug-20260825-ll1--lldtopology-lag-behind-adr-014) · `admin-web` / `admin.{env}` retracted by `SUG-20260825-nip` |
| SUG-20260825-nip | 2026-08-25 | human:Mahesh | One NIP-APP (New Insurance Platform) Flutter client for web/iOS/Android; RM, ISR, admin and operations share it with role-based views; no separate admin/ops app now or later | SF1 | SC1 | MUST | ARCH | P2 / P1 | CLOSED-DELIVERED | [detail](#sug-20260825-nip--one-nip-app-role-based-not-a-second-admin-ui) · recorded as `ADR-015` (PROPOSED — human T4 outstanding) |
| SUG-20260825-arb | 2026-08-25 | human:Mahesh | Review with internal Architect team: Cloudflare instead of CloudFront (bank standard), F5 BIG-IP / WAF instead of AWS WAF (bank standard), External ALB before API Gateway, GitLab CI/CD for pipelines, EBS (Enterprise Service Bus) naming for Core Banking integration with CBS in brackets, Terraform IaC, CloudTrail and CloudWatch both mandatory | SF1 | SC0 | MUST | ARCH | P1 / P1 | ADMIT-BYPASS | [ARB-ARCHITECTURE-DOSSIER](../../architecture/ARB-ARCHITECTURE-DOSSIER.md) · [detail](#sug-20260825-arb--internal-architect-review-alignment-cloudflare-f5-external-alb-gitlab-ebscbs-terraform-cloudtrailcloudwatch) |
| SUG-20260827-tpo | 2026-08-27 | human:Mahesh | Platform Topology & LLD Alignment: replace Argo CD with GitLab CI/CD with logo, replace AWS Network Firewall with F5 BIG-IP / Firewall with logo, incorporate Ansible for automated DR drills / sanity testing, and emphasize Terraform IaC baseline | SF1 | SC0 | MUST | ARCH | P1 / P1 | CLOSED-DELIVERED | [r0-platform-topology](../../architecture/r0-platform-topology.svg) · [detail](#sug-20260827-tpo--platform-topology--lld-alignment-gitlab-cicd-f5-big-ip-ansible-terraform) |
| SUG-20260831-alb | 2026-08-31 | human:Mahesh | Correct two false perimeter assumptions against the existing AU Bank estate: (1) remove the External / public ALB in front of API Gateway; (2) Cloudflare and F5-XC are bank-enterprise SaaS, not AWS services and not in any platform VPC | SF1 | SC1 | MUST | ARCH | P1 / P1 | ADMIT | [ADR-018](../../platform/architecture-review/08-architecture-decision-log.md) · [detail](#sug-20260831-alb--correct-edge-ingress-no-public-alb-saas-outside-aws) |
| SUG-20260831-apg | 2026-08-31 | human:Mahesh | Existing bank estate routes all incoming and outgoing requests through Apigee. Decide whether Amazon API Gateway is still needed, and whether the R0 VPC / IGW / TGW pack must attach to (not duplicate) the existing network account | SF1 | SC1 | MUST | SPIKE | P1 / P1 | ADMIT · draw PARKED | [SPIKE-001](#sug-20260831-apg--apigee-is-the-bank-api-plane--do-not-add-a-second-amazon-api-gateway-until-confirmed) · [PARKED](./PARKED-BACKLOG.md) |

<!--
Row format:
| SUG-0001 | 2026-08-08 | agent:claude | Redis-backed idempotency store | SF2 | SC0 | SHOULD | NFR | P4 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#sug-0001) |
-->

---

## 3. Detail records

Detail blocks live here for every non-trivial triage. Format:
[../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).

### SUG-20260827-tpo · Platform Topology & LLD Alignment (GitLab CI/CD, F5 BIG-IP, Ansible, Terraform)

```yaml
id: SUG-20260827-tpo
raised_at: "2026-08-27"
raised_by: "human:Mahesh"
source: "User architectural alignment query on r0-platform-topology.svg"
input: >
  Update r0-platform-topology.svg: replace Argo CD with GitLab CI/CD with actual logo,
  replace AWS Network Firewall with F5 BIG-IP / Firewall with actual logo.
  Add Ansible for automated DR drills, network drills, and post-deployment sanity testing.
  Emphasize Terraform for Infrastructure as Code (IaC) automation across the platform.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 / S09 — Engineering & Platform Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: "Platform topology diagram alignment"

stage_fit:
  code: SF1
  rationale: >
    Directly serves S08/S09 platform architecture representation and matches enterprise bank
    infrastructure baseline (ADR-016, ARB dossier).

scope:
  code: SC0
  business_scope: "WS-3 Architecture and Infrastructure Baseline"
  serves: []
  failure_without_it: >
    Architecture diagrams contradict approved enterprise bank standards (GitLab, F5, Ansible, Terraform)
    and ARB presentation dossiers.
  minimal: true
  authority: "Architecture baseline + ADR-016 directive"

necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "S08/S09 Architecture Review"
  binds_when: "S09 Platform Provisioning and ARB Review"
  evidence_tier: E2
  confidence: C5

action: ADMIT-BYPASS
action_rationale: "Architecture alignment directive matching ratified bank enterprise standards."

classification:
  type: ARCH
  risk_tier: T2

priority:
  score_now: 16
  priority_now: P1
  priority_at_target: P1

breakdown:
  stories:
    - "Update r0_platform_views.py to replace Argo CD with GitLab CI/CD logo, replace AWS Network Firewall with F5, add Ansible DR/sanity automation, and highlight Terraform IaC"
    - "Re-render all platform SVG and PNG companion diagrams"
```

### SUG-20260831-alb · Correct edge ingress (no public ALB; Cloudflare + F5-XC are SaaS outside AWS)

```yaml
id: SUG-20260831-alb
raised_at: "2026-08-31"
raised_by: "human:Mahesh"
source: "Architecture correction against existing AU Bank application and central-network diagrams"
input: >
  Rebuild the architecture diagram. Two assumptions were wrong:
  1. Remove the external load balancer in front of API Gateway.
  2. Cloudflare and F5 are both SaaS — not on the AWS cloud and not in our VPC.
  Align with the existing banking application network (north-south via Cloudflare
  then F5-XC; east-west via the central network account TGW / EDGE VPC).
duplicate_of: null
conflicts:
  - SUG-20260825-arb (External ALB before API Gateway — retracted)
  - ADR-016 ingress hop 1 (External ALB clause — amended by ADR-018)
  - SUG-20260827-tpo (F5 BIG-IP appliance drawn inside the inspection VPC — retracted;
    F5 on this estate is F5-XC SaaS on the north-south path only)

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 / S09 — Engineering & Platform Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  state_as_of: "2026-08-10"
  state_provisional: false
  freshness: "WARN — state_as_of 21 days old; 04-STAGE_GATES.md 15d vs 14d limit"
  active_work_item: "Correct R0 perimeter against existing bank estate"

stage_fit:
  code: SF1
  rationale: >
    The S08/S09 architecture pack currently asserts a hop and a placement the existing
    AU Bank estate does not have. GATE-S09 platform provisioning cannot be requested
    from a diagram that invents a public ALB and puts SaaS products in the VPC.

scope:
  code: SC1
  business_scope: "WS-3 Architecture and Infrastructure Baseline"
  serves: ["SUG-20260825-arb", "ADR-016", "R0-LLD", "ARB-ARCHITECTURE-DOSSIER"]
  failure_without_it: >
    ARB and S09 packs show a public ALB in front of API Gateway and place Cloudflare
    and F5 inside AWS / the VPC. The existing bank application (v1.4, 9-July-2026)
    treats Cloudflare and F5-XC as external SaaS; the insurance platform's AWS entry
    is API Gateway, then the internal ALB. Shipping the wrong hop is an incorrect
    landing-zone request.
  minimal: true
  authority: "Human Architecture owner correction + existing AU Bank estate diagrams"

necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "S08/S09 Architecture Review"
  binds_when: "ARB presentation and S09 Terraform IaC provisioning"
  evidence_tier: E2
  confidence: C5
  evidence:
    - "Existing AU Bank application architecture v1.4 (9-July-2026) — Cloudflare and F5-XC as external SaaS; Public ALB is the current app's AWS entry, not the insurance platform's"
    - "Central Network Account Architecture V1 — EDGE VPC FortiGate NGFW, TGW, Direct Connect; no F5 appliance in the spoke VPC"
    - "Human Architecture owner: remove External ALB in front of API Gateway; Cloudflare and F5 are SaaS, not AWS, not in our VPC"

action: ADMIT
action_rationale: >
  Correction of in-scope, on-stage architecture artefacts that are currently wrong.
  Not a new perimeter product. ADR-018 drafts the amended hop; human T4 on ADR-016
  / ADR-018 remains outstanding. Deepali jointly owns the security outcome (A3).

classification:
  type: ARCH
  risk_tier: T2
  also: [DOC]

priority:
  score_now: 16
  priority_now: P1
  priority_at_target: P1

breakdown:
  stories:
    - "Amend ADR-016 ingress hop; draft ADR-018"
    - "Update authoritative sources (03-solution-architecture-r0, 04-security-architecture, R0-HLD, R0-LLD, ARB dossier)"
    - "Redraw generated platform views and hand-authored HLD/reference edge labels"
```

### SUG-20260831-apg · Apigee is the bank API plane — do not add a second Amazon API Gateway until confirmed

```yaml
id: SUG-20260831-apg
raised_at: "2026-08-31"
raised_by: "human:Mahesh"
source: "Follow-up after SUG-20260831-alb — bank existing architecture + new fact that all in/out traffic is routed through Apigee"
input: >
  As per the bank's existing architecture, do we need any more changes in our
  application architecture — VPC, Internet Gateway, Transit Gateway, and other
  things the existing bank applications already have? Also: all incoming and
  outgoing requests are routed through Apigee. If that is there, do I need to
  add Amazon API Gateway separately?
duplicate_of: null
conflicts:
  - ADR-018 (Amazon API Gateway as first AWS hop — contested if Apigee already is Proxy 1)
  - ADR-009 (a new network-account TGW — must mean attach to existing AU-CTO-NETWORK, not a second hub)
  - ADR-010 (platform inspection VPC + NAT EIPs — contested if Apigee is the outbound plane)

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 / S09 — Engineering & Platform Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  state_as_of: "2026-08-10"
  state_provisional: false
  freshness: "WARN — state_as_of 21 days old; 04-STAGE_GATES.md 15d vs 14d limit"
  active_work_item: SUG-20260831-alb

stage_fit:
  code: SF1
  rationale: >
    S09 P4 will provision an API hop. If the bank's Apigee already is that hop,
    requesting Amazon API Gateway is the same class of defect as the withdrawn
    public ALB — a hop with no named job.

scope:
  code: SC1
  business_scope: "WS-3 Architecture and Infrastructure Baseline"
  serves: ["ADR-018", "ADR-009", "ADR-010", "R0-LLD P1/P4"]
  failure_without_it: >
    The S09 landing-zone request asks for Amazon API Gateway, a platform TGW,
    and possibly an IGW / inspection VPC that duplicate the existing bank
    network and API plane.
  minimal: true
  authority: "Human Architecture owner question + existing AU Bank estate"

necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "S09 P4 Edge band"
  binds_when: "S09 Terraform request for the edge hop"
  evidence_tier: E5
  confidence: C2
  evidence:
    - "Verbal fact from Architecture owner: all incoming and outgoing requests route through Apigee"
    - "Apigee is not named anywhere in this repository today"
    - "Existing application architecture v1.4 and Central Network Account V1 do not label Apigee"
  assumptions: [ASM-013]
  anti_over_engineering:
    X1_named_consumer: false
    X3_cheap_later: false
    X5_stage_necessity: true
    X9_problem_observed: false

action: ADMIT
action_rationale: >
  Admit as SPIKE-001, not as a diagram change. Confidence is C2 — below C3 —
  so this is a confirmation spike, not an implementation. Human Architecture
  owner 2026-08-31: keep the candidate bank API plane off every architecture
  diagram until written answers exist. Until then Amazon API Gateway remains
  Proxy 1 (ADR-018) and the draw of that overlay is PARKED.

classification:
  type: SPIKE
  also: [ARCH]
  breakdown: SPIKE
  risk_tier: T2

priority:
  score_now: 16
  priority_now: P1
  priority_at_target: P1

breakdown:
  stories:
    - "SPIKE-001: confirm Apigee edition (X / hybrid / on-prem), whether NIP is a new Apigee product, and whether 1SB and PG callbacks already traverse Apigee"
    - "If confirmed: draft ADR amending ADR-018 — Apigee is Proxy 1; Amazon API Gateway withdrawn; Internal ALB remains"
    - "If refuted: keep ADR-018 Amazon API Gateway; record why the candidate bank API plane does not cover this platform"
```

> **Amended 2026-08-31 (human Architecture owner):** keep the candidate bank API plane **off every diagram** until SPIKE-001 returns. Amazon API Gateway stays. Network attach (existing TGW / DXGW, no workload IGW) proceeds independently of that spike.

### SUG-20260825-arb · Internal Architect Review Alignment (Cloudflare, F5, External ALB, GitLab, EBS/CBS, Terraform, CloudTrail/CloudWatch)

```yaml
id: SUG-20260825-arb
raised_at: "2026-08-25"
raised_by: "human:Mahesh"
source: "Internal Architecture Review Team feedback"
input: >
  Mahesh review with internal Architect team:
  - Use Cloudflare as bank has that enterprise standard
  - Use F5 BIG-IP instead of AWS WAF as bank enterprise standard
  - External ALB before API Gateway
  - GitLab CI/CD for all pipelines
  - Connect CBS via EBS APIs (Enterprise Service Bus); use EBS in diagram and in brackets add CBS
  - Terraform for Infrastructure as Code (IaC)
  - CloudTrail and CloudWatch both mandatory

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 / S09 — Engineering & Platform Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: "ARB architecture preparation"

stage_fit:
  code: SF1
  rationale: >
    Aligns enterprise bank infrastructure tooling and perimeter architecture directly with
    AU Bank standards for S09 provisioning and ARB presentation.

scope:
  code: SC0
  business_scope: "WS-3 Architecture and Infrastructure Baseline"
  serves: []
  failure_without_it: >
    ARB presentation will be rejected by bank enterprise security, networking, and platform teams
    if bank-standard tools (Cloudflare, F5, GitLab, EBS, Terraform) are omitted or contradicted.
  minimal: true
  authority: "Internal Architect review directive + Mahesh Board 1 Architecture"

necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "S07/S09 Architecture Review"
  binds_when: "ARB presentation and S09 Terraform IaC provisioning"
  evidence_tier: E2
  confidence: C5

action: ADMIT-BYPASS
action_rationale: "Enterprise architecture alignment directive from human Architecture owner."

classification:
  type: ARCH
  risk_tier: T2

priority:
  score_now: 16
  priority_now: P1
  priority_at_target: P1

breakdown:
  stories:
    - "Update ARB-ARCHITECTURE-DOSSIER.md to incorporate Cloudflare, F5, External ALB, GitLab CI/CD, EBS (CBS), Terraform IaC, and CloudTrail+CloudWatch"
```

### SUG-20260818-4c3 · Architecture justification pack + release-scoped HLD

```yaml
id: SUG-20260818-4c3
raised_at: "2026-08-18"
raised_by: "human:Mahesh"
source: "direct follow-up after R0 HLD review — prepare comprehensive justification for architecture review questions"
input: >
  Diagram is really good, but I was not able to justify why we need all these services,
  what functions/APIs/business logic each has, why we are not merging couple of services.
  Need a comprehensive document to understand why this architecture is built like this and
  what we are achieving. Review questions: why relational not MongoDB for quote and proposal;
  right now no quote caching; where will caching logic be added especially for direct insurer
  integrations; cannot see complete platform scope beyond R0 — use colour coding for R0/R1/R2+.
  Lets do this together.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: "SUG-20260818-4c3 architecture justification + release-map HLD"

stage_fit:
  code: SF1
  rationale: >
    Architecture justification and release-scoped HLD make the already-drafted S07 solution
    architecture reviewable and defendable; they do not admit new runtime services or change
    GATE-S08 criteria.

scope:
  code: SC0
  business_scope: "WS-3 ARCH — explicit in CURRENT-STATE routing and 03-solution-architecture-r0"
  serves: []
  failure_without_it: >
    Architecture reviews cannot answer service-boundary, datastore, caching or roadmap questions
    from a single artefact; R0 HLD alone looks incomplete.
  minimal: true
  authority: "direct user instruction + Mahesh Board 1 architecture lane"

necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "S07/S08 architecture communication — before Wave 1 service build"
  binds_when: "next architecture / stakeholder review of platform shape"
  evidence_tier: E2
  evidence:
    - "existing 02/03/05 architecture-review + ws3 03-solution-architecture-r0"
    - "WS-3 charter R0/R1/R2 revisit table"
    - "08-integration-strategy Phase A/B/C"
  confidence: C4
  assumptions: []
  anti_over_engineering:
    X1_named_consumer: true
    X3_cheap_later: false
    X5_stage_necessity: true
    X9_problem_observed: true

action: ADMIT-BYPASS
action_rationale: >
  Human Architecture owner directed immediate production of the justification pack and
  release-coloured HLD as review preparation; documentation-only, no runtime change.
duplicate_of: null
conflicts: []

classification:
  type: ARCH
  risk_tier: T2
  notes: "AI-drafted Mahesh artefact; human T4 Architecture sign-off remains outstanding"

priority:
  score_now: 14
  priority_now: P2
  priority_at_target: P2
  caps_applied: []

dependencies: []

breakdown:
  stories:
    - "Write 06-architecture-justification-and-review-answers.md"
    - "Publish a release-coloured full-platform HLD with R0/R1/R2+ colour language"
    - "Retain/refresh the R0 HLD visual"

delivery_note_2026_08_25: >
  Both diagram stories were satisfied by files that landed on main while this suggestion was open,
  not by the two SVG/PNG pairs originally drafted for it. SUG-20260820-n5t moved the R0 view to
  docs/architecture/r0-reference-architecture.svg and gave docs/hdl.svg the release-coded North
  Star; rule HA-04 in 16-hld-authoring-and-update-protocol.md then assigns one file per horizon.
  The drafted pair would have been a second answer for each of those two horizons, and both were
  stale against ADR-005 (#5 Opportunity), CF-5 (#19 as a W0b service) and ADR-009..ADR-013 (the
  five admitted layers, including "no Kafka in R0" printed on the R0 draft). They are therefore
  not published; 06-architecture-justification-and-review-answers.md cites the canonical
  renderings instead. No content the stories asked for is lost — the release colour language and
  the R0/R1/R2+ matrix live in that document's section 8.

alignment_2026_08_25:
  reconciled_against:
    - "CR-012 / ADR-009..ADR-013 — five infrastructure layers admitted into R0"
    - "ADR-008 — one Aurora cluster, schema per context, amending ARCH-004"
    - "ADR-005 / OPEN-D10 — #5 is Opportunity, W1, the single origination point (AC-8, AC-9)"
    - "CF-1..CF-5 — #19 Configuration is a W0b service; only its UI is deferred"
    - "03-solution-architecture-r0 section 3 — fourteen deployable services plus one app"
    - "03-solution-architecture-r0 section 2.1 — two actors; the IPR assists and never originates"
    - "16-hld-authoring-and-update-protocol HA-02, HA-04, HA-09, NC-1, LY-1, LB-R1"
  corrections_made_in_place:
    - "section 6.1 — 'idempotency in Redis' withdrawn; ADR-011 rejects it by name (FF-23)"
    - "section 4.1 — 'Lead can stay thin' and 'Admin is versioned artefacts' both struck, with the reason kept visible"
    - "section 1.2 — service count corrected from ~12 to fourteen plus one app"

outcome:
  registered_in: "SUGGESTION-REGISTER.md"
  work_item_id: SUG-20260818-4c3
  status: ADMIT-BYPASS
  closed_reason: null

resumed: null
```

### SUG-20260816-d8v · Dilip AI Executive Sponsor Perspective

```yaml
id: SUG-20260816-d8v
raised_at: "2026-08-16"
raised_by: "human:Mahesh"
source: "direct user instruction after accepting the executive-sponsor recommendation"
input: >
  Accept the recommendation, create the persona on the GitHub repository, push the code,
  put references wherever required, and make the perspective available before the remaining
  Phase/P0 stories. The requested AI should primarily provide the executive sponsor perspective
  and may give clarity/approval in bounded cases.

context:
  workstream: "cross-cutting Product/business context; WS-1 remains in Phase 4 hardening"
  current_phase: "WS-1 Phase 4 — Hardening & consumer enablement"
  canonical_stage: "L7 — Hardening for WS-1; AU platform R0/P0 requirements remain active business context"
  current_objective: "P4-UAT-SIGNOFF for WS-1; improve decision quality before remaining AU-platform P0 story refinement"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: "user-directed SUG-20260816-d8v documentation integration"

stage_fit:
  code: SF2
  rationale: >
    Small documentation/context integration with no runtime change, no new dependency and no
    gate-criterion change. It can be absorbed while making the sponsor perspective available
    before material P0/R0 business decisions.
  absorption_test:
    small: true
    no_new_dependency: true
    no_new_decision: true
    gate_neutral: true

scope:
  code: SC1
  business_scope: >
    Cross-cutting decision-quality enabler for Product/R0 rather than 1SB runtime functionality.
  serves:
    - "AU platform P0/R0 scope and business-value decisions"
    - "material Should-deferral and investment decisions"
    - "pilot success and benefits-realization definition"
  failure_without_it: >
    Executive sponsor reasoning remains implicit and future agents can miss the business-value,
    investment and measurable-outcome perspective before Product finalizes material P0 decisions.
  minimal: true
  authority: "direct user instruction + existing Rajal Product package extension model"

necessity:
  now: SHOULD
  future_necessity: SHOULD
  target_stage: "before material P0/R0 scope/value decisions"
  binds_when: "BRD/R0 sponsor-perspective trigger fires"
  evidence_tier: E1
  evidence:
    - "direct project-owner/user instruction"
    - "R0-SCOPE already contains a Business Sponsor sign-off slot"
    - "BRD-P0 previously allowed Should slip with sponsor OK but had no reusable AI sponsor lens"
  confidence: C5
  assumptions: []

action: ADMIT-BYPASS
action_rationale: >
  The user explicitly accepted the recommendation and directed immediate repository creation,
  references and push. The bypass is limited to documentation/context queue ordering; it does not
  bypass any Architecture, Security, Compliance, QA, SRE, Database or mandatory-human decision.
conflicts:
  - >
    CR-009 closes the canonical persona roster. Resolved by implementing Dilip as an auxiliary
    Product-side executive sponsor perspective under Rajal, not as a tenth canonical persona,
    new board or parallel authority.

classification:
  type: GOV
  also: [DOC]
  breakdown: STORY
  epic: null
  risk_tier: T1
  destination: "1SB PRODUCT-BACKLOG.md governance / decision-quality enablers"

priority:
  now: P2
  at_target: P2
  rationale: >
    Explicitly requested before remaining P0 business decisions, but it does not supersede open
    runtime hardening blockers or a P1 safety/regulatory override.

dependencies:
  edges: []
  state: READY
  enablement_count: 3
  earliest_start: "now by explicit user direction"
  cycles: none

breakdown:
  children: []
  completion_definition: >
    Dilip sponsor lens documented under Rajal; P0 BRD/R0/stakeholder references wired; governance
    trace recorded; branch pushed and draft PR opened.
  not_included:
    - "new AIGEM persona or board"
    - "change to canonical persona authority matrix"
    - "human sponsor impersonation/signature"
    - "runtime application changes"

outcome:
  registered_in: "registers/SUGGESTION-REGISTER.md + PRODUCT-BACKLOG.md"
  work_item_id: SUG-20260816-d8v
  plan_id: null
  status: CLOSED-DELIVERED
  closed_reason: null

resumed: "WS-1 P4-UAT-SIGNOFF remains the governing delivery objective after this documentation PR"
```

**Bypass risk:** this user-directed documentation work consumes repository capacity that could otherwise advance the open WS-1 `GATE-P4`, especially consumer/UAT enablement work. It does **not** change, waive or mark any gate criterion complete.

### SUG-20260816-ba7 · Principal Insurance Platform Business Analyst / R11

```yaml
id: SUG-20260816-ba7
raised_at: "2026-08-16"
raised_by: "human:Mahesh"
source: >
  Direct user instruction identifying the change as Mahesh's governance decision, explicitly
  requesting immediate bypass, a fresh branch, reference to the Mahesh/Deepali/Aarti/Shivanshi
  personas, and creation of an improved end-to-end bancassurance BA persona.
not_an_ai_suggestion: true

context:
  workstream: "cross-cutting governance/persona context; runtime workstreams unchanged"
  current_phase: "WS-1 Phase 4 hardening; WS-2 foundation"
  active_work_item: "user-directed R11 Principal BA persona integration"

stage_fit:
  code: SF2
  rationale: >
    Documentation/governance-context change only. It introduces no runtime/API/schema/configuration
    change and marks no delivery gate criterion complete.

scope:
  code: SC1
  serves:
    - "end-to-end bancassurance Product and requirement decision preparation"
    - "R11 requirement readiness, AC quality and traceability"
    - "business handoffs to Product, Architecture, Security, Database, SRE and other authorities"
  minimal: true

necessity:
  now: SHOULD
  target_stage: "before further material journey/requirement elaboration"
  evidence_tier: E1
  evidence:
    - "direct Mahesh/repository-owner instruction"
    - "existing AIGEM R11 role and Application Lifecycle business-analysis responsibilities"
  confidence: C5

action: ADMIT-BYPASS
action_rationale: >
  Mahesh explicitly instructed immediate governance creation and bypassed normal AI intake/queue
  ordering. The register ID is retained only because 09-AI_EXECUTION_RULES §8 requires a bypass
  record; it must not be represented as an AI-originated suggestion.
process_skipped:
  - "normal suggestion deferral and single-in-flight queue ordering"
  - "separate CR preparation before documentation"
authorised_by: "Mahesh / repository owner — direct instruction, 2026-08-16"

classification:
  type: GOV
  also: [DOC]
  risk_tier: T1

authority_effect:
  existing_role: "R11 — Business Analyst / Product delegate"
  new_board: false
  new_aigem_role: false
  named_human_roster_growth: false
  product_authority: "unchanged — Rajal"
  specialist_authority: "unchanged — canonical matrix owners"
  human_approval_impersonation: false

priority:
  now: P2
  at_target: P2
  rationale: "explicitly requested now; no P1 override or runtime gate closure"

outcome:
  decision: GOV-008
  destination: "docs/context/roles/principal-insurance-platform-business-analyst/"
  branch: "codex/principal-business-analyst-persona"
  status: CLOSED-DELIVERED

resumed: "WS-1 P4-UAT-SIGNOFF remains the governing runtime delivery objective after this docs-only branch"
```

**Bypass risk:** this governed documentation work consumes capacity while `GATE-P4` remains open.
The mitigation is bounded scope: one R11 package plus canonical links, with no application code,
stage state, gate evidence or specialist authority changed.

### SUG-20260816-ap1 · Reusable Context Module and Safe Autopilot

```yaml
id: SUG-20260816-ap1
raised_at: "2026-08-16"
raised_by: "human:Mahesh"
source: "direct acceptance of the 2026-08-16 governance/context validation recommendations"
input: >
  Synchronise main, accept the review recommendations, create an autopilot operating mode,
  remove proven documentation redundancy, improve folder abstraction, and make the context
  module reusable for projects with different problem statements and domains.

context:
  workstream: "cross-cutting governance/context; supports WS-1 and WS-2 and prepares proposed WS-3"
  current_phase: "WS-1 Phase 4 hardening; WS-2 Phase 1 foundation"
  canonical_stage: "L7 for WS-1; L4/L6 for WS-2"
  current_objective: "remove structural delivery blockers without auto-approving a stage or regulated decision"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: SUG-20260816-ap1

stage_fit:
  code: SF1
  rationale: >
    Application CI, correct routing, machine-verifiable evidence and non-blocking scheduling
    support current gates; reusable context packaging is absorbed as the same bounded control-plane change.

scope:
  code: SC1
  business_scope: "cross-cutting delivery-enablement and context portability"
  serves: ["GATE-P4 4.1/4.7", "GATE-IAM-P1", "future WS-3 foundation recovery"]
  failure_without_it: "semantic drift remains invisible and blocked work can stall the whole programme"
  minimal: true
  authority: "direct repository-owner instruction; specialist and stage-transition approvals remain separate"

necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "current governance hardening"
  binds_when: "before unattended/autopilot execution is enabled"
  evidence_tier: E1
  evidence:
    - "2026-08-16 validation: mechanical checks green while routing paths and semantic statements conflict"
    - "GATE-P4 0/7 and GATE-IAM-P1 0/6 criteria closed"
  confidence: C5
  assumptions: []

action: ADMIT
action_rationale: "The recommendation was raised in the previous turn and explicitly accepted by the human in this turn."
conflicts:
  - "Automation may prepare CANDIDATE evidence but cannot supply PASSED or mandatory human approvals."

classification:
  type: GOV
  also: [DOC, INFRA, QA]
  breakdown: EPIC
  epic: CR-010
  risk_tier: T3
  destination: "governance change request + existing workstream backlog"

priority:
  now: P2
  at_target: P2
  rationale: "High enablement value and current control-plane defects; no evidenced O1-O8 override."

dependencies:
  edges: []
  state: READY
  enablement_count: 5
  earliest_start: "now"
  cycles: none

breakdown:
  children:
    - "context framework and project manifest"
    - "semantic validation"
    - "safe autopilot evidence controller"
    - "application CI foundation"
    - "documentation consolidation"
  completion_definition: "PLAN-001 acceptance criteria pass with no automatic human approval or stage transition."
  not_included:
    - "marking a lifecycle stage PASSED"
    - "importing the full proposed Bible backlog into Jira"
    - "production deployment or risk acceptance"

outcome:
  registered_in: "SUGGESTION-REGISTER.md + CR-010 + PRODUCT-BACKLOG.md"
  work_item_id: SUG-20260816-ap1
  plan_id: PLAN-001
  status: ADMITTED
  closed_reason: null

resumed: null
```

### SUG-20260820-n5t · North Star HLD with release phasing

```yaml
id: SUG-20260820-n5t
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "direct user instruction, acting as the Board 1 Architecture persona"
input: >
  Update docs/hdl.svg to the final vision for the insurance platform, with reference to the
  recorded target-state design discussion. The SVG must carry a detailed description of each
  boundary, LOB segregation, the aggregation/provider layer and the capabilities a mature
  architecture must have, and must show the phase-wise release: what is in R0, R1, R2 and RN,
  RN being the final targeted system for now.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  current_gate: "GATE-S08 (OPEN)"
  state_as_of: "2026-08-10"
  state_provisional: false
  freshness_check: "exit 0 — FRESH"

stage_fit:
  code: SF2
  rationale: >
    A documentation artefact with no runtime change. Architecture intent at S08 is legitimate
    and cheap: R0 is being built now, and the value of drawing the target now is precisely that
    R0 leaves the right seams behind. It does not admit R1+ work, move a gate or create a
    dependency.
  absorption_test:
    small: true
    no_new_dependency: true
    no_new_decision: true
    gate_neutral: true

scope:
  code: SC1
  business_scope: >
    Architecture communication artefact for WS-3. Not a scope change: the diagram renders
    CURRENT-STATE's own in_scope / out_of_scope split rather than proposing a different one.
  serves:
    - "R0 build decisions that are cheap now and expensive later — the seams"
    - "stakeholder answer to 'why is R0 so small' and 'why build a registry for one product'"
    - "Delivery (R12) sequencing input and the follow-on transition map"
  failure_without_it: >
    R0 gets built without its seams, and adding Health becomes a redesign rather than a cell.
  minimal: true
  authority: >
    Board 1 Architecture owns the HLD. AI may draft and simulate Mahesh's reasoning; the
    mandatory human T4 Architecture sign-off in 11-REVIEW_GATES.md is NOT satisfied by this.

necessity:
  verdict: SHOULD
  evidence_tier: E5
  confidence: C4
  note: >
    Target-state content is drawn from the recorded design discussion plus the repository's own
    architecture registration and current state. Where the discussion and CURRENT-STATE disagree
    on release numbering, CURRENT-STATE wins — non-Life LOBs are 'R2+' there, which the diagram
    realises as R3 (Health) and R4 (General/Motor).

action:
  verdict: ADMIT-BYPASS
  rationale: >
    Implemented in the turn it was raised, which the one rule normally forbids. Recorded as
    ADMIT-BYPASS rather than ADMITTED because it was a direct human instruction from the owning
    authority for a documentation artefact, not an agent-originated suggestion.
  priority_now: P3
  priority_at_target: P3
  type: DOC
  risk_tier: T1

decisions_taken:
  - id: "keep both diagrams"
    decision: >
      docs/hdl.svg becomes the North Star; the previous R0 HLD is preserved unchanged at
      docs/architecture/r0-reference-architecture.svg.
    rationale: >
      The two answer different questions. Publishing only the target invites 'why has the team
      not built Health?'; publishing only R0 invites 'why are we building a Journey Registry for
      one product?'. Overwriting the R0 view would have destroyed the executable architecture.
  - id: "label the unadmitted"
    decision: >
      R1–R4 elements are stamped with their CURRENT-STATE out_of_scope revisit_at; RN elements
      are marked as having no governance record; a separate dashed band marks integrations
      (CKYC, V-KYC, e-sign, TPA, IRDAI/IIB reporting, channel vendors) as RN candidates that are
      explicitly NOT admitted scope.
    rationale: >
      A target diagram is the easiest artefact in a programme to misread as a plan. Naming the
      gaps is more useful than omitting them, but only if the diagram says they are gaps.

not_included:
  - "any change to CURRENT-STATE.yaml scope, stage or gate state"
  - "T4 Architecture sign-off, which remains outstanding and human"
  - "an ADR for any RN technology choice (event bus, direct insurer routes, analytics)"
  - "the R0 -> R1 -> R2 transition and dependency map (SUG-20260820-r1t, parked)"

outcome:
  registered_in: "SUGGESTION-REGISTER.md"
  work_item_id: SUG-20260820-n5t
  status: ADMIT-BYPASS
  evidence:
    - "docs/hdl.svg — North Star, 11 described boundaries, release-coded R0..RN"
    - "docs/architecture/r0-reference-architecture.svg — R0 view preserved"
    - "docs/architecture/README.md — which diagram answers which question"
    - "scripts/governance/ci-checks.py — PASSED, 24 checks"
  closed_reason: null

resumed: null
```

---

### SUG-20260820-hr0 · HLD review round — R0 actors, LOB boundary, configuration

```yaml
id: SUG-20260820-hr0
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "direct user instruction — HLD review comments issued by the owning Board 1 Architecture authority"
input: >
  Five review comments on the R0 HLD. (1) There are two actors, not one: the Bank RM is the
  certified Specified Person, and SP certification is an attribute on the RM, not a standalone
  actor row or channel; the Insurance Partner Representative is an insurer employee who assists the
  RM or the customer and is not an SP. (2) Lead/opportunity origination is RM-only; the opportunity
  is the single origination point that every downstream module consumes; the IPR has no create
  rights; no parallel origination path in MVP. (3) IPR visibility is gated — nothing is visible
  until the RM has created the opportunity and completed suitability and need analysis — and is
  scoped to the IPR's own insurer at the data/query layer, not the UI; because the IPR is not an
  SP their role must be assist-only, the RM stays the accountable SP, and every IPR action must be
  audit-logged and attributed separately so the solicitation trail is clean for IRDAI. (4) LOB
  segregation must be visible from day one: DB schema, entity model and config tables carry LOB as
  a first-class dimension from release 1, and product, journey, rules, commission and document
  requirements are all LOB-partitioned from the start. (5) Everything is configuration-driven with
  no exceptions, and this is independent of front-end availability — the configuration layer ships
  now, versioned and seedable in the backend, even if no admin panel is built in R1.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  current_gate: "GATE-S08 (OPEN)"
  state_as_of: "2026-08-10"
  state_provisional: false
  freshness_check: "exit 0 — FRESH"
  active_work_item: "none in flight; this instruction is the work"

stage_fit:
  code: SF0
  rationale: >
    Prerequisite, not adjacent. The artefacts corrected here — the domain model, information model,
    R0 solution architecture and security architecture — are the inputs S08 and S11 build from, and
    all four are AI-DRAFTED with human signature outstanding. Nothing is implemented, so this is
    the repair of an unsigned baseline rather than a change to a shipped one. Two of the five
    comments are structural dimensions (LOB, configuration) that are free to carry now and become a
    migration across every table on the sale path once a second line of business, a second insurer
    or a live rule pack exists. Correcting a design document at S08 is exactly the posture RUNBOOK
    section 8.3 prescribes for L4 Foundation: build the floor, and build it right.
  blocks:
    - "GATE-S08 criterion S08-G4 — ArchUnit and static analysis: FF-16 to FF-21 cannot be written against a wrong actor model"
    - "S11 entry — the R0 build order and the service set change"

scope:
  code: SC0
  business_scope: >
    Explicitly in scope. CURRENT-STATE.yaml WS-3 current_scope.in_scope already lists the Lead
    service (context #5), the suitability and consent contexts, the product catalogue and the audit
    store. No new capability is admitted here; the review corrects how the admitted ones are
    modelled.
  serves:
    - "R0 build decisions that are cheap now and expensive later — actors, LOB and configuration"
    - "IRDAI solicitation attribution: one accountable Specified Person per record"
    - "the Health and General onboarding that follows R0 on the same template"
  failure_without_it: >
    An uncertified insurer employee acquires a de-facto solicitation path with no separate
    attribution; LOB becomes a backfill across every table on the sale path plus an audit history
    that cannot be corrected; and every W1 to W4 service is written with hardcoded product and
    insurer branches that are never removed.
  minimal: true
  authority: >
    Board 1 Architecture owns the HLD. Three of the four decisions reach beyond that: ADR-004 is
    A3_JOINT_REVIEW with Deepali and carries a compliance threshold that is Shailja's, ADR-005
    changes the R0 build order and a Product-owned label and needs Rajal, and ADR-007 makes
    configuration an authorization-relevant asset. An AI may draft Mahesh's reasoning; the mandatory
    human T4 Architecture sign-off in 11-REVIEW_GATES.md is NOT satisfied by this record.

necessity:
  verdict: MUST
  evidence_tier: E5
  confidence: C4
  note: >
    The actor and IPR corrections are grounded in the repository's own material: business-problem-
    statement section 6 already names the Insurance Partner Representative as a distinct actor, and
    authentication-authorization README lines 33-34 already state that SP is an attribute and not a
    synonym for RM. The origination correction resolves a live contradiction between
    CURRENT-STATE.yaml in_scope and the deferral in ws3-platform/03 section 3, and the ratified
    state file wins. Confidence is C4 rather than C5 only because the exact assist-only action set
    is a compliance determination that has not been made (OPEN-D9); the gate ships default-deny
    until it is.

overrides_claimed:
  - id: O3
    claim: "Incorrect domain model"
    evidence: >
      ws3-platform/02 section 4.2 recorded lob = TERM, conflating the line of business with the
      product class, against ws3-platform/01 INV-QUO-01 which gates on lob and the S03 acceptance
      criterion AC-LEAD-010-1 which reads 'LOB LIFE'. Also 15 section 4 listed CERTIFIED_SP as an
      actorType and a channel while 15 ID-20 in the same file states that certification is an
      authorization attribute.
    status: >
      Recorded as evidence for necessity and priority, NOT used to interrupt an in-flight item —
      no item was in flight. Nothing is implemented, so the defect is in the model rather than in
      behaviour; stated here so the claim is auditable rather than inflated.

action:
  verdict: ADMIT-BYPASS
  rationale: >
    Implemented in the turn it was raised, which the one rule normally forbids. Recorded as
    ADMIT-BYPASS rather than ADMITTED because it was a direct instruction from the owning human
    authority for the artefact under review, following the precedent set by SUG-20260816-d8v and
    SUG-20260820-n5t. The bypass and its risk are stated: the risk is that four architecture
    decisions with Security, Compliance, Product and Database consequences are recorded without
    their boards having met. Each ADR names the approvals it still requires, and none of them
    becomes binding because this branch merges.
  priority_now: P1
  priority_at_target: P1
  type: ARCH
  risk_tier: T4
  score:
    N: 4
    S: 4
    B: 3
    R: 3
    D: 2
    E: 2
    formula: "2N + 2S + 2B + 2R + D - E"
    total: 28
    band: P1
    pri8_note: "SF0 sets the B floor at 2; B is 3 because S08-G4 is a gate criterion this blocks"

decisions_taken:
  - id: ADR-004
    decision: >
      Two R0 actors. Specified Person is certification state on the BANK_RM principal, evaluated at
      the action and not at login; CERTIFIED_SP is removed as an actor type and as a channel value.
      INSURER_PARTNER_REP is a partner-plane principal, assist-only, with the accountable SP
      immutable and always the originating RM, visibility gated on completed need analysis and
      suitability, insurer scoping applied as a mandatory persistence-layer predicate, out-of-scope
      records absent from result sets rather than refused by identifier, and every partner action
      audited with its acting capacity.
  - id: ADR-005
    decision: >
      The opportunity is the single origination point, creatable only by a BANK_RM. Context #5
      moves from deferred-to-S13 into R0 Wave 1, reconciling ws3-platform/03 with CURRENT-STATE
      in_scope. Every downstream aggregate carries the originating reference. Campaign and bulk
      sales-management breadth stays deferred.
  - id: ADR-006
    decision: >
      lob is mandatory and non-null on every business entity, configuration record, audit event and
      authorization request from the first migration; the vocabulary is frozen at LIFE, HEALTH and
      GENERAL; lob and productClass are separate dimensions. Partitioning is not forking — party,
      opportunity, consent evidence, journey identity, payment, documents, portfolio and audit stay
      shared.
  - id: ADR-007
    decision: >
      The configuration layer ships in R0 as a Wave 0b component — LOB-partitioned, append-only,
      versioned, effective-dated, seeded from source-controlled artefacts, resolved through a port,
      with no compiled-in fallback and no business branch on an insurer, product, LOB or channel
      literal. Explicitly independent of front-end availability; the admin UI stays deferred. This
      withdraws the earlier trade under which a rule-pack change required a deployment.

not_included:
  - "any change to CURRENT-STATE.yaml scope, stage, gate state or standing_constraints. Section 7.5 of the registration document carries the five constraint lines for the orchestrator to transcribe. The ONE edit made to that file is id_allocation.sequential.ADR, advanced from 1 to 8: ADR-001..003 already existed and ADR-004..007 are indexed in the decision register, which FreshnessCheck scans, so leaving the counter behind put the repository into HALT and blocked every agent. It is an ID-allocation correction, not a stage or scope edit"
  - "T4 Architecture sign-off, and the Security, Compliance, Product and Database approvals each ADR names"
  - "the assist-only threshold itself — which assistance actions stop short of solicitation (OPEN-D9, Shailja)"
  - "renaming context #5 from Lead to Opportunity in Product-owned artefacts (OPEN-D10, Rajal)"
  - "physical partitioning of the lob dimension per store (OPEN-I6, Aarti)"
  - "any code, migration or seed artefact — this is design; implementation is S08/S09 work under GATE-S08"
  - "a commission service. Commission is a reserved configuration namespace with no consumer until R1"

outcome:
  registered_in: "SUGGESTION-REGISTER.md"
  work_item_id: SUG-20260820-hr0
  status: ADMIT-BYPASS
  evidence:
    - "docs/platform/ws3-platform/01-domain-model-and-invariants.md — sections 2.4, 2.5, 2.6; INV-ACT-01..04, INV-LED-04..07, INV-CFG-01..03, INV-LOB-01/02; OPEN-D9..D11"
    - "docs/platform/ws3-platform/02-information-model.md — lob corrected to LIFE with productClass separated; opportunity, configuration and audit-attribution sheets"
    - "docs/platform/ws3-platform/03-solution-architecture-r0.md — sections 2.1, 2.2; Wave 0b configuration and Wave 1 opportunity; seams S-20..S-22; FF-16..FF-21"
    - "docs/platform/ws3-platform/04-security-architecture.md — four principal classes; partner gating, scoping and attribution controls"
    - "docs/platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md — SC-W3-8..SC-W3-12 and the section 7.5 transcription block"
    - "docs/platform/architecture-review/08-architecture-decision-log.md — ARCH-023..027 and ADR-004..ADR-007"
    - "docs/context/roles/mahesh-principal-insurance-platform-architect/15-actor-identity-and-authorization.md — ID-15a, ID-15b; CERTIFIED_SP removed from the actorType and channel enumerations"
    - "docs/architecture/r0-reference-architecture.svg — R0 view reconciled (HA-03, HA-06)"
    - "scripts/governance/ci-checks.py — PASSED"
    - "java scripts/governance/FreshnessCheck.java — FRESH"
  closed_reason: null

resumed: null
```

---

### SUG-20260820-al7 · HLD and R0 diagram alignment

```yaml
id: SUG-20260820-al7
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "direct user instruction, acting as the Board 1 Architecture persona"
input: >
  Verify that the HLD and the R0 diagram are in line — naming convention, nomenclature and the
  layer model already decided in the HLD. The R0 view should be a mirror image of the HLD that
  simply shows what release zero covers, and nothing more. Additionally, because delivery starts
  with the Life module only, the Life-specific modules must be grouped into a distinct colour or
  box so that it is visible which modules belong to a line of business and which are generic or
  shared.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  current_gate: "GATE-S08 (OPEN)"
  state_as_of: "2026-08-10"
  state_provisional: false
  freshness_check: "exit 0 — FRESH"

stage_fit:
  code: SF1
  rationale: >
    Not new architecture. The two diagrams and the authoring protocol that governs them were
    revised four days ago under SUG-20260820-n5t and SUG-20260820-hr0, and the verification
    finds defects introduced by those same two changes: the North Star carries release chips
    that contradict the ratified R0 scope, and the authoring protocol still documents a canvas
    contract for a file whose contents moved. HA-06 requires the R0 view to be reconciled in
    the same change as the target-state view; that reconciliation was partial. Correcting a
    defect in the current stage's own design artefact is on-stage work, not a new increment.

scope:
  code: SC1
  serves: [SUG-20260820-n5t, SUG-20260820-hr0]
  rationale: >
    The R0 reference architecture is an in-scope deliverable and it is unusable in its current
    form for the purpose it exists for. Two diagrams that disagree about which release a
    bounded context belongs to give the reader two answers with no way to tell which is
    current — the exact failure HA-02 and HA-06 exist to prevent. No new capability, service or
    scope is added by this item.

necessity:
  verdict: MUST
  evidence_tier: E2
  confidence: C5
  rationale: >
    The defects are objectively checkable against ratified sources, not matters of taste:
    03-solution-architecture-r0.md section 3 places #5 Opportunity in Wave 1 and #19
    Configuration in Wave 0b of R0, and section 7 defines FF-01..FF-21. The North Star
    contradicts all three.

action: ADMIT
priority:
  now: P2
  at_target: P2
  rationale: >
    Not a P1 override — nothing is broken at runtime and no gate criterion is blocked. P2
    because delivery reads these diagrams when sequencing S08/S09 work, and a wrong release
    chip on #5 and #19 is read as permission to defer two components that R0 depends on.

work_type: ARCH
risk_tier: T2

findings:
  - id: AL-1
    severity: HIGH
    where: "docs/hdl.svg — Boundary 4"
    finding: >
      #5 Lead -> Opportunity carries an R1 release chip. 03-solution-architecture-r0.md section 3
      un-deferred it into Wave 1 of R0 (AC-8, AC-9) because CURRENT-STATE in_scope already lists
      it, and the North Star's own R0 roadmap band lists "Lead (#5)". The diagram contradicts
      both its source and itself.
    resolution: "chip corrected to R0; the R1 text now names only the parts that are R1 — bulk upload, allocation, campaign management"
  - id: AL-2
    severity: HIGH
    where: "docs/hdl.svg — Boundary 10"
    finding: >
      The configuration plane carries an R1 chip and no context number. CF-5 and
      03-solution-architecture-r0.md section 3 ship context #19 as a Wave 0b service in R0 —
      only its admin UI is deferred. Drawn as R1, the layer every other R0 wave resolves its
      rules from appears to be next-release work.
    resolution: "chip corrected to R0, numbered #19, and the R0/R1 split stated on the element: layer in R0, maker-checker governance and admin UI at R1"
  - id: AL-3
    severity: MEDIUM
    where: "docs/hdl.svg — Boundary 10, CI/CD element"
    finding: "asserts 15 fitness functions; the catalogue is FF-01..FF-21 since SUG-20260820-hr0 added FF-16..FF-21"
    resolution: "corrected to 21"
  - id: AL-4
    severity: MEDIUM
    where: "both diagrams"
    finding: >
      Seven contexts are drawn under two different names with no mapping between them:
      #4 Customer / Party-Customer, #7 Suitability / Suitability framework, #8 Product Catalogue /
      Product Governance & Catalogue, #9 Journey Orchestration / Journey Registry, #10 Quotation /
      Life Quote, #11 Proposal & UW / Life Proposal & Case Mgmt, #13 Policy & Issuance / Policy
      Portfolio & Registry. Four of them carry no #n at all on the North Star element, so a reader
      cannot match the box to the register. The target-state names are deliberate — several
      contexts split or widen by RN — but an undeclared rename reads as a different service.
    resolution: >
      Naming rule NC-1 added to the authoring protocol: the #n is the identity and is mandatory on
      every element in both files; the canonical register name is always shown; where the target
      state renames or splits the context, it is rendered as "#n Canonical -> target name" so the
      evolution is explicit. Applied to both diagrams.
  - id: AL-5
    severity: HIGH
    where: "docs/architecture/r0-reference-architecture.svg — whole layout"
    finding: >
      The R0 view is organised by build wave and journey flow; the North Star is organised into
      ten described boundaries. They share a colour vocabulary and a context register but not a
      structure, so the R0 view cannot be read as a release-zero cut of the target picture — which
      is the one job the pair exists to do. Concretely: #10 and #11 sit in a flat row beside #6,
      #12 and #16, giving no hint that the North Star holds Quote and Proposal to be per-LOB and
      that boundary frozen.
    resolution: "R0 view redrawn on the North Star's boundary bands 1-10, each band carrying what R0 contains and an explicit note where a band is thin or empty in R0"
  - id: AL-6
    severity: HIGH
    where: "docs/architecture/r0-reference-architecture.svg"
    finding: >
      Nothing distinguishes LOB-owned execution from shared platform. LB-4 and LB-5 draw the line
      precisely — the rules are partitioned, the evidence is not — and the diagram renders every
      R0 service in wave colour, so a reader planning Health cannot see which boxes get a second
      instance and which never do.
    resolution: >
      Three-class LOB classification rendered: LIFE CELL (LOB-owned execution, #10 and #11, the
      frozen per-LOB boundary), LOB-partitioned shared services (shared code, configuration keyed
      by lob, per CF-2), and LOB-agnostic shared mechanics (single-instance for every LOB, per
      LB-5). New colour token added to the legend in the same edit (HA-08).
  - id: AL-7
    severity: MEDIUM
    where: "docs/context/roles/mahesh-principal-insurance-platform-architect/16-hld-authoring-and-update-protocol.md"
    finding: >
      Root cause of AL-1..AL-6. The authoring protocol still says docs/hdl.svg is horizon H0 —
      R0 as designed — and its canvas contract in section 4 documents the R0 geometry. Since
      SUG-20260820-n5t, hdl.svg holds the North Star and the R0 view lives at
      docs/architecture/r0-reference-architecture.svg. There has been no convention covering two
      files, which is why two files drifted. Its checklist also still ranges seams to S-19 and
      fitness functions to FF-15.
    resolution: >
      Protocol extended to a two-file family with a shared naming rule (NC-1), a shared layer
      model (LY-1: the ten boundaries are the layer vocabulary for every horizon), the LOB
      classification rule (LB-R1), corrected ranges, and a reconciliation checklist that fails
      when the two files disagree about a release chip or a context name.

not_included:
  - "the name of context #5 in Product-owned artefacts — Lead vs Opportunity is OPEN-D10 and Rajal's call. Both diagrams keep the dual form '#5 Opportunity (Lead)' until that decision lands"
  - >
    the contradiction between ARCH-004 database-per-service, which the R0 view asserts, and the
    North Star's Boundary 8 position that physical splitting is scale-driven and R0 may start as
    separate schemas in a shared cluster. Both are drawn as written and the divergence is flagged
    for Mahesh and Aarti; an agent does not pick between a ratified decision and a target-state
    position on a matter with cost, DR and DBA consequences. Raised as OPEN-A1 below
  - "T4 Architecture sign-off. The signature status on both diagrams is unchanged (HA-10)"
  - "any change to CURRENT-STATE.yaml, to scope, stage or gate state"
  - "any code, migration or seed artefact"

open_decisions:
  - id: OPEN-A1
    owner: "Mahesh (Architecture) with Aarti (Database)"
    question: >
      Does R0 start with one Aurora cluster holding a schema per context, or a cluster per
      service? ARCH-004 is Proposed and the R0 view asserts it as decided; the North Star
      asserts the opposite starting point. Until this is settled the two diagrams describe two
      different R0 data topologies.

outcome:
  registered_in: "SUGGESTION-REGISTER.md"
  work_item_id: SUG-20260820-al7
  status: ADMITTED
  evidence:
    - "docs/context/roles/mahesh-principal-insurance-platform-architect/16-hld-authoring-and-update-protocol.md — two-file artefact family, NC-1, LY-1, LB-R1, corrected ranges, reconciliation checklist"
    - "docs/hdl.svg — AL-1, AL-2, AL-3, AL-4 corrected"
    - "docs/architecture/r0-reference-architecture.svg — redrawn on boundary bands; AL-5, AL-6 resolved"
    - "docs/architecture/README.md — the convention and the LOB reading rule stated for readers"
    - "java scripts/governance/FreshnessCheck.java — FRESH"
  closed_reason: null

resumed: null
```

---

### SUG-20260820-dc4 · Data topology and the name of context #5

```yaml
id: SUG-20260820-dc4
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "direct user instruction, acting as the Board 1 Architecture persona"
input: >
  There is no database per service. As per the North Star's boundary, splitting is scale-driven,
  and R0 may start as a schema in one cluster; afterwards, based on the requirement, we can split
  the clusters for the line of business and the shared resources. Also lead or opportunity — I
  would go with opportunity, because a lead is too thin to identify, whereas an opportunity is
  something which can be converted for a new sale, for a renewal and for a lapse. It has a larger
  scope.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  current_gate: "GATE-S08 (OPEN)"
  state_as_of: "2026-08-10"
  freshness_check: "exit 0 — FRESH"

stage_fit:
  code: SF1
  rationale: >
    These resolve two open decisions raised against the current stage's own design artefacts —
    OPEN-A1 from SUG-20260820-al7 and OPEN-D10 from SUG-20260820-hr0 — on a design that is not yet
    signed and against which no service, migration or seed exists. Deciding the physical topology
    now is also the cheap moment: it is a documentation change today and a data migration across
    every table once R0 has run.

scope:
  code: SC1
  serves: [SUG-20260820-al7, SUG-20260820-hr0]
  rationale: >
    No capability, service or scope is added. Both items remove a contradiction inside deliverables
    already in scope: two diagrams asserting two different R0 data topologies, and one bounded
    context carrying two names.

necessity:
  verdict: MUST
  evidence_tier: E1
  confidence: C5
  rationale: >
    E1 — a decision by the accountable architect, and the one the repository was already waiting
    for. The topology half was drafted five days ago in 09-target-state-architecture-doctrine.md
    section 5.2 and listed as open item 1 in 10-north-star-capability-model.md; it needed a
    decision, not analysis. The naming half is the open question ADR-005 records as OPEN-D10.

action: ADMIT
priority: {now: P2, at_target: P2}
work_type: ARCH
risk_tier: T3

decisions:
  - id: OPEN-A1
    resolution: >
      ARCH-004 bundled three claims and only two of them are principles. One owner per
      authoritative datum with no cross-service table access, and separate credentials and schema
      ownership per service, remain INVARIANT and enforced. A separate physical cluster per service
      is a DECISION, evidence-led on scale, blast radius, security isolation, RTO/RPO and cost. R0
      starts with one Aurora cluster and a schema per context. The first split, when evidence
      justifies it, follows the LOB-cell / shared-platform seam — not the service boundary.
    recorded_as: ADR-008
    supersedes: "ARCH-004 (physical-topology half only; the ownership half is retained and restated)"
    note: >
      This is the reconciliation Mahesh had already written into
      09-target-state-architecture-doctrine.md section 5.2 and had deliberately not applied
      unilaterally. What the instruction adds beyond that draft is the split AXIS: LOB cell versus
      shared platform, which is what the North Star's boundary 8 already draws and what LB-5 makes
      the natural seam.
  - id: OPEN-D10
    resolution: >
      Context #5 is named Opportunity. The rationale is domain scope, not preference: a lead
      records that someone might buy, and dies at conversion. An opportunity is the durable demand
      object behind a new sale, a renewal, a lapse recovery, a cross-sell and an
      abandoned-journey recovery — which is exactly the R2 rule that a renewal or lapse creates a
      NEW opportunity and a NEW journey rather than reopening an old one. Naming the context Lead
      makes that rule read as a contradiction; naming it Opportunity makes it read as the model.
    recorded_as: "ADR-005, naming_resolution block"

not_included:
  - >
    CURRENT-STATE.yaml current_scope.in_scope line 85 still reads "Lead service (context #5) —
    create, resume, status", and WS-3-PLATFORM-CHARTER.md line 301 mirrors it. Both are
    human-owned scope text and an agent does not edit them (04 section 5). Flagged for Kalpana /
    R12 to transcribe, with Rajal's Product confirmation of the label
  - >
    identifier and register-ID renames. leadId, INV-LED-01..07 and CAP-102 keep their tokens: an ID
    is opaque, and rewriting seven invariant IDs across the corpus is churn that breaks every
    existing citation for no gain. The NAME changes; the IDs do not
  - "Aarti's Database approval of ADR-008 and Rajal's Product confirmation of the #5 label — required, and outstanding"
  - "T4 Architecture sign-off. Signature status on both ADRs and both diagrams is unchanged (HA-10)"
  - "any physical schema, migration or seed artefact — this is design; implementation is S09 work"

recurrence_count: 2
recurrence_20260825: >
  human:Mahesh asked again to change Opportunity to Lead because stakeholders and the team
  already use Lead. That is the inverse of OPEN-D10, not a new decision. Rule CS-2: no new
  row. Architecture position on the recurrence: do not reverse ADR-005 naming_resolution.
  The UI / RM language may say Lead — that is Rajal's, still outstanding. The bounded
  context stays Opportunity because a working pipeline object that is archived after
  conversion is exactly why Lead was judged too thin. The 2026-08-25 lifecycle request
  (SUG-20260825-lt1) uses that split; it does not reopen the name.

outcome:
  registered_in: "SUGGESTION-REGISTER.md"
  work_item_id: SUG-20260820-dc4
  status: ADMITTED
  evidence:
    - "docs/platform/architecture-review/08-architecture-decision-log.md — ADR-008 added; ARCH-004 qualified; ADR-005 naming_resolution; signature block extended"
    - "docs/platform/architecture-review/05-data-architecture.md — governing rule restated as ownership plus an evidence-led topology decision"
    - "docs/platform/ws3-platform/03-solution-architecture-r0.md — database row and build-order row updated"
    - "docs/platform/ws3-platform/04-security-architecture.md — threat I control restated without asserting physical separation"
    - "docs/context/roles/mahesh-principal-insurance-platform-architect/09-target-state-architecture-doctrine.md section 5.2 — reconciliation marked applied"
    - "docs/hdl.svg and docs/architecture/r0-reference-architecture.svg — boundary 8 reconciled; OPEN-A1 note removed; #5 renamed"
    - "scripts/governance/ci-checks.py — PASSED · java scripts/governance/FreshnessCheck.java — FRESH"
  closed_reason: null

resumed: null
```

---

### SUG-20260820-hl1 · R0 stakeholder HLD and AWS LLD pack

```yaml
id: SUG-20260820-hl1
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "direct user instruction — Act as Mahesh; use r0-reference-architecture.svg as the HLD reference; produce a detailed HLD and an LLD for the CTO and AWS platform team"
input: >
  Act as mahesh, use the R0 reference architecture SVG as HLD for R0, and create an HLD
  design document which will have detailed domain, boundary, communication, API details,
  business logic and understanding of the complete R0 and its phases, waves, what to do
  when. Use the HLD for R0 and create the LLD which will have e2e component, services,
  aws component, pvc, external proxy or reverse proxy, db, caching, designed so it is
  easy to give the CTO and AWS platform team the requirement for aws platform and
  services needed.
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: null
stage_fit:
  code: SF1
  rationale: >
    On-stage for WS-3. GATE-S08 is open and S09 is overlapped; the current deliverable
    includes IaC, environments, secrets and observability. A stakeholder HLD that walks
    the already-ratified R0 picture, and an LLD that is the S09 AWS bill of materials,
    are the artefacts that make that deliverable executable. Not SF3: this is not the
    parked R0→R1→R2 dependency map (SUG-20260820-r1t).
scope:
  code: SC0
  business_scope: "in scope — R0 architecture of the admitted WS-3 slice"
  serves: []
  failure_without_it: >
    S09 cannot be requested from the AWS platform team without a narrowed BOM; the SVG
    alone is not a provisioning contract, and architecture-review/04 still names Kafka,
    ElastiCache and per-service clusters that R0 has explicitly declined.
  minimal: true
  authority: "CURRENT-STATE.yaml WS-3 current_scope; 03-solution-architecture-r0.md; ADR-001; ADR-008"
necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "S09 — Platform & Environment Foundation"
  binds_when: "first Terraform apply against a non-dev account"
  evidence_tier: E2
  evidence:
    - "S09-E01 network, compute and data foundation stories"
    - "ADR-001 Terraform / ap-south-1 / Render.com boundary"
    - "ADR-008 one Aurora cluster, schema per context"
  confidence: C4
  assumptions: []
  anti_over_engineering:
    X1_named_consumer: true   # CTO + AWS platform team + Shivanshi S09
    X3_cheap_later: false     # provisioning the target-state estate now is the expensive mistake
    X5_stage_necessity: true
    X9_problem_observed: true # 04-aws-infrastructure-architecture.md still reads as the R0 BOM
action: ADMIT-BYPASS
action_rationale: >
  Direct instruction from the Board 1 Architecture authority to produce the artefacts in
  this turn. Bypass records that seven-board review of the *plan* was skipped; the
  documents themselves carry AI-DRAFTED status and name the outstanding human T4
  Architecture, Security, Database and SRE signatures. Risk of the bypass: an AWS LLD
  that will drive S09 provisioning has not yet had Deepali / Aarti / Shivanshi boards.
  No new architectural decision is asserted; Kafka, Redis-for-idempotency, per-service
  clusters and Istio remain out of R0 as already recorded.
duplicate_of: null
conflicts:
  - "architecture-review/04 names MSK, ElastiCache, Istio, per-service RDS — R0-LLD §1.3 lists them DO NOT PROVISION, citing 03 §5.1 and ADR-008"
classification:
  type: ARCH
  also: [DOC, INFRA]
  risk_tier: T4
  security_impact: trust-boundary-realisation   # LLD restates TB-1..TB-6; does not change them
  compliance_impact: residency-and-WORM-restated
  operational_impact: S09-provisioning-input
priority:
  now: P2
  at_target: P1
  factors: "S09 overlapped; GATE-S08 still the in-flight engineering gate"
  caps: []
dependencies:
  - "Authoritative sources already in ws3-platform/ 00–05 and ADR-001…008"
  - "Does not unpark SUG-20260820-r1t"
plan_files:
  - "docs/architecture/R0-HLD.md"
  - "docs/architecture/R0-LLD.md"
bypass:
  authorised_by: "human:Mahesh — direct instruction to act as Board 1 and produce the HLD and LLD"
  skipped: "implementation-plan template; seven-board review of the plan before drafting"
  risk: "AWS LLD may be cited as S09 input before Security, Database and SRE have signed"
  non_negotiable_touched: false   # no secrets, no public contract change, no data-integrity change — presentation of accepted decisions
```

### SUG-20260820-ls1 · R0 LLD SVG rendering

```yaml
id: SUG-20260820-ls1
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "follow-up on SUG-20260820-hl1 — create SVG for the LLD"
input: >
  can you create svg for LLD ?
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: SUG-20260820-hl1
stage_fit:
  code: SF1
  rationale: "Same on-stage S09 input as hl1. The LLD prose exists; this is its rendering (HA-03 source first, diagram second)."
scope:
  code: SC0
  business_scope: "in scope — R0 AWS deployment picture"
  serves: ["SUG-20260820-hl1"]
necessity:
  now: SHOULD
  future_necessity: MUST
  target_stage: "S09 — Platform & Environment Foundation"
  evidence_tier: E2
  confidence: C4
action: ADMIT-BYPASS
action_rationale: >
  Direct follow-up to produce the LLD picture in this turn, same executor lane as hl1.
  No new AWS service is named. Dashed nodes are the existing DO NOT PROVISION list.
duplicate_of: null
continues: SUG-20260820-hl1
classification:
  type: ARCH
  also: [DOC]
  risk_tier: T4
priority:
  now: P2
  at_target: P1
bypass:
  authorised_by: "human:Mahesh — follow-up instruction"
  skipped: "seven-board review of the plan"
  risk: "same as hl1 — SVG may be shown to AWS platform team before Security/Database/SRE sign"
  non_negotiable_touched: false
```

---

### SUG-20260820-pt9 · AWS platform-team application view — AZ, DR and sequence

```yaml
id: SUG-20260820-pt9
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "follow-up on SUG-20260820-hl1 / SUG-20260820-ls1, with a reference deployment diagram attached"
input: >
  Use the architecture diagram, HDLD, LDLD diagram we have for our application, and create a
  similar kind of application diagram for the platform team, so that the AWS platform team can
  know what kind of application we are building, what all services we need, in which availability
  zone we want, what DR services we want, how proxy services are required, and when.
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  state_provisional: false
  freshness_check: "exit 0 — FRESH, 2026-08-20"
  active_work_item: SUG-20260820-ls1
stage_fit:
  code: SF1
  rationale: >
    S09 — Platform & Environment Foundation is the next stage and is already overlapped into the
    current phase. S09-E01-S03 (network foundation across AZs), S09-E01-S05 (data foundation) and
    S09-E06-S03/S04 (backup and proven restore) are exactly the questions this asks. The request
    is the S09 entry artefact, not new scope.
scope:
  code: SC0
  business_scope: "in scope — R0 AWS deployment picture for the platform team"
  serves: ["SUG-20260820-hl1", "SUG-20260820-ls1"]
necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "S09 — Platform & Environment Foundation"
  evidence_tier: E2
  confidence: C4
  rationale: >
    S09 entry criterion "cloud account structure and budget approved" cannot be met without a
    request the platform team can price and provision from. R0-LLD.md answers what and how much,
    but three of the six questions asked here are not answered anywhere: per-resource
    availability-zone placement, the DR bill of materials as a resource list, and the order in
    which each resource is needed.
gap_analysis:
  already_answered:
    - "what the application is — R0-HLD.md §1-§3"
    - "service inventory — R0-LLD.md §12, 03-solution-architecture-r0.md §3"
    - "reverse proxy chain — R0-LLD.md §3 (two-hop: API Gateway then internal ALB)"
  not_answered_before_this_item:
    - "availability-zone placement per resource — sources say '3 AZs' and 'min 2 AZ', never which resource sits where"
    - "DR as a bill of materials — R0-LLD.md §11 states the posture, not the ap-south-2 resource list"
    - "when — no mapping from an AWS resource to the S09 story that builds it and the wave that first consumes it"
action: ADMIT-BYPASS
action_rationale: >
  Direct human follow-up in the same executor lane as hl1/ls1. HA-03 is honoured: the three gaps
  are closed in R0-LLD.md first (§2.1, §11.1, §12.1) and only then rendered. No AWS service is
  introduced that §1.1/§1.2 does not already name, and the DO NOT PROVISION list is carried
  through unchanged, so this cannot become scope drift.
duplicate_of: null
continues: SUG-20260820-ls1
classification:
  type: ARCH
  also: [DOC]
  risk_tier: T4
priority:
  now: P2
  at_target: P1
dependencies:
  blocks: ["S09-E01-S03 network foundation", "S09-E01-S05 data foundation", "S09 entry — cloud account structure approved"]
  blocked_by: ["Direct Connect / VPN / bank-proxy decision for CBS and Bank AD — Shivanshi + bank network (R0-LLD §14)"]
bypass:
  authorised_by: "human:Mahesh — direct instruction"
  skipped: "seven-board review of the plan"
  risk: >
    Same as hl1 and ls1 — the view may be handed to the AWS platform team before Deepali
    (Security), Aarti (Database) and Shivanshi (SRE) have signed. The AZ placement and DR resource
    list are architecture constraints, not sizing decisions; every SKU, instance class and
    Aurora-Global-versus-restore choice stays tagged DECIDE WITH.
  non_negotiable_touched: false
notes:
  - "Does not unpark SUG-20260820-r1t (the R0→R1→R2 transition map)"
  - "Does not alter GATE-S08; S08 remains the gate in flight"
```

---

### SUG-20260820-ic3 · Icon notation, generated from code

```yaml
id: SUG-20260820-ic3
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "review of SUG-20260820-pt9's rendering"
input: >
  I'm not looking for all those boxes. I'm looking for the actual images or the logos — when you
  are using a Kubernetes cluster it should show that this is the Kubernetes cluster, there are
  microservices communicating, there is CloudFront, there is an RDS service. Can you think of a
  better approach than SVG, without importing a lot of external images?
context:
  workstream: WS-3
  canonical_stage: "S08 — Engineering Foundation"
  active_work_item: SUG-20260820-pt9
  freshness_check: "exit 0 — FRESH, 2026-08-20"
stage_fit:
  code: SF1
  rationale: "Same S09 artefact as pt9. Notation change, not a content change."
scope:
  code: SC0
  serves: ["SUG-20260820-pt9"]
necessity:
  now: SHOULD
  future_necessity: MUST
  target_stage: "S09 — Platform & Environment Foundation"
  evidence_tier: E2
  confidence: C4
  rationale: >
    The audience reads AWS diagrams daily. Labelled rectangles make them translate before they can
    review, which is friction on the artefact whose whole purpose is to be reviewed by that team.
decision:
  chosen: "mingrammer/diagrams — Python, rendered through Graphviz"
  why: >
    The official AWS, Kubernetes, Argo and Flutter icon sets ship inside the pip wheel, so no image
    is vendored into this repository and nothing is fetched at render time. Being code, the picture
    changes in the same commit as its source and a reviewer sees which sentence changed — which is
    what HA-03 asks for and what a binary canvas file cannot give.
  rejected:
    - "draw.io / Lucid — right icons, but a binary-ish canvas: no useful diff, and the picture drifts from its source"
    - "Mermaid architecture-beta — icon packs resolve over the network at render time"
    - "D2 — icons are external URLs"
    - "hand-authored SVG with embedded base64 icons — vendors the icon set and is slow to change"
  output_format: >
    PNG. Graphviz can emit SVG but references icons by absolute local path, so the SVG is not
    portable. Recorded so nobody 'fixes' the format later.
action: ADMIT-BYPASS
action_rationale: >
  Direct human follow-up in the same executor lane as pt9. No architectural content changes: the
  five diagrams render R0-LLD.md §2.1 / §11.1 / §12.1, which pt9 already added and which remain the
  source of truth (HA-02).
duplicate_of: null
continues: SUG-20260820-pt9
supersedes_artefact: "docs/architecture/r0-platform-topology.svg (deleted — replaced, not kept alongside, to avoid two answers in the repository)"
classification:
  type: DOC
  also: [ARCH]
  risk_tier: T4
priority:
  now: P3
  at_target: P2
new_repository_dependency:
  runtime: "python3 + graphviz (dot) + pip diagrams==0.25.1"
  scope: "documentation build only — not a service dependency, not in any container image"
  recorded_at: "docs/architecture/diagrams/requirements.txt"
defect_found_and_fixed:
  what: "the first render placed an Aurora WRITER in all three AZs"
  cause: "zone test was `\"A\" in zone`, and \"A\" is a substring of \"AVAILABILITY\""
  why_it_matters: "a diagram asserting a Multi-AZ topology the design does not have is worse than no diagram"
bypass:
  authorised_by: "human:Mahesh — direct instruction"
  skipped: "seven-board review of the plan"
  risk: "same as pt9 — the views may be shown to the AWS platform team before Security, Database and SRE sign"
  non_negotiable_touched: false
notes:
  - "HA-10 added to the authoring protocol: notation follows the audience; generate rather than draw"
```

---

### SUG-20260820-lay4 · Deterministic orthogonal layout

```yaml
id: SUG-20260820-lay4
raised_at: "2026-08-20"
raised_by: "human:Mahesh"
source: "review of SUG-20260820-ic3's rendering"
input: >
  The designs look better now. The only problem is that they are not well aligned, not well
  positioned, and not correctly linked. The links move randomly here and there, crossing and
  curving. They should be straight lines, diverted at ninety degrees only, with the blocks and
  logos well balanced on the image.
context:
  workstream: WS-3
  canonical_stage: "S08 — Engineering Foundation"
  active_work_item: SUG-20260820-ic3
  freshness_check: "exit 0 — FRESH, 2026-08-20"
stage_fit: {code: SF1, rationale: "Same S09 artefact. Presentation change, not a content change."}
scope: {code: SC0, serves: ["SUG-20260820-ic3", "SUG-20260820-pt9"]}
necessity:
  now: SHOULD
  future_necessity: MUST
  target_stage: "S09 — Platform & Environment Foundation"
  evidence_tier: E2
  confidence: C4
  rationale: >
    A platform team reads placement in these views as a specification. Curved and crossing
    connectors make the reader re-derive which line goes where, which is the same friction the
    icon change was meant to remove.
decision:
  chosen: "hand-rolled svgcanvas.py — explicit coordinates, axis-aligned connector segments"
  supersedes: "the mingrammer/diagrams + Graphviz layout choice recorded under SUG-20260820-ic3"
  retained_from_ic3: >
    The icon assets. The diagrams wheel is still the dependency, but only as the source of the
    official AWS and Kubernetes art — none of its layout code is used.
  rejected:
    - what: "Graphviz splines=ortho"
      why: >
        TESTED, not assumed. It does emit 90-degree lines, but it detaches edge labels from their
        edges and routes connectors straight through cluster borders, and node positions remain the
        engine's choice rather than a deliberate grid.
    - what: "tuning the Graphviz ranks further"
      why: "a layered layout engine cannot be argued into a fixed grid; it re-ranks on every change"
    - what: "draw.io / Lucid"
      why: "already rejected under ic3 — no useful diff, and the picture drifts from its source"
  output_format: >
    SVG, with the icons embedded as base64 so the file is self-contained, plus a PNG companion for
    tools that will not take an SVG. This reverses ic3's PNG-only decision, which existed only
    because Graphviz's SVG referenced icons by absolute local path.
action: ADMIT-BYPASS
action_rationale: >
  Direct human follow-up in the same executor lane. No architectural content changed: the same five
  views render the same R0-LLD.md sections.
continues: SUG-20260820-ic3
classification: {type: DOC, also: [ARCH], risk_tier: T4}
priority: {now: P3, at_target: P2}
new_repository_dependency:
  runtime: "pip diagrams==0.25.1 (icon assets only) + cairosvg==2.7.1 (optional PNG companion)"
  removed: "graphviz — no longer needed, no layout engine is used"
  scope: "documentation build only — not a service dependency, not in any container image"
defects_found_and_fixed:
  - what: "vertical connectors were drawn straight through their own node's caption"
    cause: "a bottom port started at the icon edge, but the label hangs below the icon"
    fix: "Node.port('B') clears the label block — fixed in the canvas, not per diagram"
  - what: "edge labels rendered as white smears"
    cause: "the white halo relied on the SVG paint-order property"
    why_it_matters: >
      cairosvg and older librsvg ignore paint-order, so the labels would have failed in exactly
      the viewers a platform team is most likely to open the file in. Labels now sit on a real plate.
bypass:
  authorised_by: "human:Mahesh — direct instruction"
  skipped: "seven-board review of the plan"
  risk: "same as pt9 and ic3 — the views may be shown before Security, Database and SRE sign"
  non_negotiable_touched: false
notes:
  - "HA-10 extended: generating a diagram does not mean handing its layout to an engine."
```

### SUG-20260821-jx1 · R0 Journey Execution Specification

```yaml
id: SUG-20260821-jx1
raised_at: "2026-08-21"
raised_by: "human:Mahesh"
source: "direct user instruction — comprehensive end-to-end application document for the dev team"
input: >
  A comprehensive document holding all the use cases of each actor, each request, each
  response, how the request routes from one service to another and under what condition,
  what validation is done at each service layer, the final output, the external API calls
  — everything. Example given: RM login traverses CloudFront, then WAF, then the RM BFF,
  which routes to authentication, which calls the SSO; and inside that, every validation
  and check it performs. The same treatment for the whole application, end to end, with
  the possible outcomes and the algorithm each validation follows, so the dev team can
  build from it.
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: null
stage_fit:
  code: SF1
  rationale: >
    On-stage, and partly a backfill of stages already passed. The documentation canon
    names three of the requested artefacts as canonical and marks them absent: the S05
    service blueprint and the S05 error / empty / degraded-state catalogue are RED, and
    the S03 requirements traceability matrix is RED
    (05-DOCUMENTATION-CANON.md sections S03, S05). GATE-S08 criteria G6 (test
    infrastructure at every pyramid level), G8 (engineering standards adopted) and G10
    (a new engineer can build, test and ship in a week) all consume a per-request
    specification that does not exist today. It is the named input to S11 slice
    definition. Not SF3: the information exists now and is already ratified — this
    assembles it, it does not invent it.
scope:
  code: SC1
  business_scope: "in scope — the admitted R0 assisted term-sale slice only"
  serves:
    - "R0-ASSISTED-TERM-SALE"
    - "GATE-S08 criteria S08-G6, S08-G8, S08-G10"
    - "S11 vertical-slice definition and its E2E suite"
  failure_without_it: >
    The eight hard gates (C1 suitability, C2 consent, C3 distributorId, C4 payment device,
    C5 no PII in logs, C6 residency, C7 immutable evidence, C8 no inferred sale) are each
    stated in two or three separate authority documents, and no document states at WHICH
    layer each is enforced or with what algorithm. A developer implementing the quote
    endpoint today must infer whether C1 is checked at the BFF PEP, at the Quotation
    service, at the aggregate, or at all three — the HLD says "C1 via S-08", which is a
    seam reference, not an enforcement specification. The observed consequence is either
    a gate enforced only at the BFF (bypassable by any internal caller) or the same rule
    implemented three times with three different expiry semantics. Both are regulatory
    findings, not style problems.
  minimal: true
  scope_split: >
    The request as stated covers "the whole application". Most of the application is
    explicitly out of scope now: DIY / customer journey (R1), hybrid mode switching (R2),
    Group B insurers (R1), ULIP and Savings (R1), Customer BFF (R1), Health / Motor /
    Travel (R2+), renewals and servicing (R2+), admin UI (R1), reporting beyond the pilot
    funnel (R1). Writing their flows would manufacture design decisions Board 1 has not
    made. That half is split out as SUG-20260821-jx2 and parked.
  authority: >
    CURRENT-STATE.yaml WS-3 current_scope and out_of_scope_now;
    R0-HLD.md sections 4.2, 5.1, 5.3, 5.4, 6;
    ws3-platform/01-domain-model-and-invariants.md section 4;
    ws3-platform/03-solution-architecture-r0.md section 5;
    ws3-platform/04-security-architecture.md;
    platform/authentication-authorization/README.md sections 5, 8;
    05-DOCUMENTATION-CANON.md sections S03, S05, S11
necessity:
  now: SHOULD
  future_necessity: MUST
  target_stage: "S11 — Vertical Slice (MVP)"
  binds_when: "the first S11 story that implements a hard gate is picked up"
  evidence_tier: E2
  evidence:
    - "05-DOCUMENTATION-CANON.md S05 — service blueprint and degraded-state catalogue both RED"
    - "05-DOCUMENTATION-CANON.md S03 — requirements traceability matrix RED"
    - "05-DOCUMENTATION-CANON.md S11 — slice definition RED, E2E suite RED"
    - "GATE-S08 S08-G6, S08-G8, S08-G10 all OPEN"
  confidence: C4
  assumptions:
    - "ASM-jx1-a: the R0-HLD contract sketches are stable enough to specify against. They are AI-DRAFTED with human T4 Architecture sign-off outstanding, so the specification inherits that status and cannot be cited as approved until R0-HLD is signed."
  anti_over_engineering:
    X1_named_consumer: true   # the dev team building S11, and QA deriving the E2E suite
    X3_cheap_later: false     # the cost lands as rework in code, once each gate is built wrong
    X5_stage_necessity: true
    X9_problem_observed: true # the enforcement layer for C1-C8 is unstated in every current document
action: ADMITTED
action_rationale: >
  Admitted as a document set, not as one file, and pending the author's choice of shape —
  the user asked for a recommendation before development, which is also what Rule
  09-AI_EXECUTION_RULES requires. Nothing is written in the turn the suggestion is raised.
  The proposed pack restates no authoritative content: each flow cites the source that
  owns the fact and adds only the assembly — hop order, enforcement layer, algorithm,
  outcome set. Status on delivery is AI-DRAFTED with human T4 Architecture and Security
  sign-off outstanding, matching R0-HLD.
duplicate_of: null
conflicts:
  - >
    The user's worked example says the BFF routes to an authentication service which calls
    the SSO. In the ratified design there is no separate authentication service: the
    workforce-access-bff owns the login, callback, session and logout endpoints itself
    (authentication-authorization/README.md section 4.1) and calls
    identity-provider-adapter-service, which is a provider-neutral port in front of
    Keycloak, which in turn federates to bank AD. Authorization is a separate hop to
    identity-authorization-service as PDP. The specification must document the ratified
    chain, and the divergence is itself evidence that the document is needed.

---

### SUG-20260824-gp1 … gp5 · the R0 robustness round — shared context

Five items, raised together from one instruction, triaged individually because they have different
necessities and different owners. The shared half is recorded once here rather than five times.

```yaml
raised_at: "2026-08-24"
raised_by: "human:Mahesh"
source: >
  A comparison of the R0 design against an existing AU Bank production estate (prod-ibmb) was
  requested, and it named five layers that estate has and R0 did not. The instruction that
  followed was explicit: "I would still at R0 as well, I don't want those gaps — let's fill those
  gaps and make R0 more robust, and make sure we incorporate all the changes in all files wherever
  required so there is no inconsistency."
context:
  workstream: WS-3
  canonical_stage: "S08 — Engineering Foundation, S09 overlapped"
  current_objective: R0-ASSISTED-TERM-SALE
  state_as_of: "2026-08-10"
  freshness_check: "exit 0 — FRESH, 2026-08-24 (review_due 2026-09-09, 16 days remaining)"
  active_work_item: "SUG-20260820-cm2 (context-architecture round) — closed before this one started"
why_this_is_not_scope_drift: >
  Every one of the five is INFRASTRUCTURE UNDER an unchanged R0 slice. No bounded context is added,
  no service is added, no journey step changes, no gate criterion moves, and nothing in
  CURRENT-STATE.yaml `out_of_scope` for WS-3 is contradicted — the WS-3 out-of-scope list is about
  journeys, LOBs, channels and contexts, not about platform layers. What changes is the platform
  the same fourteen services run on.
what_was_deliberately_NOT_admitted: >
  The comparison also surfaced a service mesh (Istio), per-service database clusters and an
  analytics warehouse. All three stay refused, and the target-state review's
  "ElastiCache for idempotency" line is now rejected by name. Admitting five things is not a
  reason to admit seven.
cost_position: >
  This set materially raises R0 fixed cost — three stateful managed services, a sixth account, an
  inspection VPC per environment and two circuits, for ~100 journey starts an hour. That is
  recorded as RISK-012 (envelope) and RISK-014 (operational surface), priced by Shivanshi and
  Kalpana at S09, and bounded by the per-environment shapes in R0-LLD §1.4. It is not waved
  through as "robustness".
bypass:
  authorised_by: "human:Mahesh — direct instruction"
  skipped: "seven-board review before implementation"
  risk: >
    Documents and decisions were written in the turn the work was instructed. The five ADRs are
    AI-DRAFTED and each names approvals that are NOT notifications — Deepali accepts ADR-010,
    Shailja signs the two evidence exclusions, Shivanshi and Aarti own the tiers they operate, and
    Kalpana owns the external dependency and the envelope. No approval was created by writing them,
    and CR-012 records that explicitly.
  non_negotiable_touched: false
state_file_transcription_required: >
  WS-1's `out_of_scope` still reads "Kafka / event backbone — revisit at Integration architecture
  stage" and "Redis idempotency / multi-instance job ownership — Phase 5.4". Both remain TRUE for
  WS-1: the platform now runs a broker and a cache tier, but WS-1's adapter neither publishes to
  the broker nor moves its idempotency store in Phase 4. Scope text is human-owned (04 §5), so it
  is NOT edited here — CR-012 §7 carries the wording for Kalpana / R12 to transcribe.
```

#### SUG-20260824-gp1 · hybrid bank connectivity in R0

```yaml
id: SUG-20260824-gp1
stage_fit:
  code: SF1
  rationale: >
    S09 is the platform-foundation stage and this is a platform-foundation layer. It is also SF0 in
    one direction: W1's `#4` Customer cannot be evidenced without it, so it is a prerequisite of
    work already admitted rather than an addition to it.
scope: {code: SC0, serves: ["R0-ASSISTED-TERM-SALE", "S09-E01-S03", "WS-2 Phase 2"]}
necessity:
  now: MUST
  future_necessity: MUST
  evidence_tier: E2
  confidence: C4
  rationale: >
    The previous position deferred the two longest-lead items on the programme behind a decision
    with no owner and no date, and permitted a stub to become the only tested path. WS-1 gate 4.3
    already demonstrates how that ends — a criterion that cannot close because an external party
    was engaged too late.
classification: {type: INFRA, also: [ARCH, SEC], risk_tier: T4}
priority: {now: P1, at_target: P1}
dependencies:
  blocks: ["W1 #4 Customer evidenced against real CBS", "WS-2 Phase 2 AD federation", "NFR-NET-01", "NFR-NET-04"]
  blocked_by: ["DEP-20260824-dx1 — bank-side VPN termination, prefixes, firewall change, DX order"]
action: ADMIT-BYPASS
decision: ADR-009
notes:
  - "VPN first is the whole reason this is admissible now: it needs a bank firewall rule, not a carrier order"
  - "Does NOT unpark WS-2's 'Bank AD federation (OIDC/SAML/LDAP specifics)' — the path is provisioned, the protocol is still unconfirmed"
```

#### SUG-20260824-gp2 · centralised egress inspection in R0

```yaml
id: SUG-20260824-gp2
stage_fit:
  code: SF1
  rationale: >
    Egress routing is decided once, before the first subnet exists. Retrofitting it changes every
    workload route table and the entire published Elastic IP list — the definition of a layer that
    is cheap now and invasive later.
scope: {code: SC0, serves: ["control C6 posture", "S09-E07", "RBI cyber-security expectations already cited by C4"]}
necessity:
  now: MUST
  future_necessity: MUST
  evidence_tier: E2
  confidence: C4
  rationale: >
    A pod that can reach a NAT gateway can reach any address on 443, and R0 had no control and no
    log on that path. For a platform holding PAN, income and health attributes and calling an
    aggregator, a payment gateway and an SMS gateway, that is the exfiltration route with the
    shortest description.
classification: {type: SEC, also: [INFRA], risk_tier: T4}
priority: {now: P1, at_target: P1}
dependencies:
  blocks: ["W2 quotes (1SB allowlist)", "W3 payments", "NFR-NET-02", "NFR-NET-03"]
  blocked_by: ["gp1 — the inspection VPC hangs off the same Transit Gateway"]
action: ADMIT-BYPASS
decision: ADR-010
authority_note: >
  This is a security control, so Deepali ACCEPTS it rather than reviewing it, and two interim
  positions are hers alone: SEC-OPEN-7 (managed IPS in alert mode until prod) and SEC-OPEN-8 (no
  TLS inspection on the 1SB mTLS session). An agent may draft the reasoning; it may not accept the
  residual risk.
notes:
  - "The allowlisted Elastic IPs MOVE to the inspection VPC — DEP-20260824-eip exists because a stale allowlist is indistinguishable from none"
  - "Rejected within the same decision: a third-party NGFW appliance, which is what the existing AU estate runs. Rejected on operational surface, not capability"
```

#### SUG-20260824-gp3 · managed cache tier in R0

```yaml
id: SUG-20260824-gp3
stage_fit:
  code: SF1
  rationale: >
    The session port and the configuration-resolution port are both still unwritten. After W0b and
    W4 this becomes a change to the two things every request touches.
scope: {code: SC0, serves: ["WS-2 accepted session design", "S-21 configuration resolution", "per-principal rate limiting"]}
necessity:
  now: MUST
  future_necessity: MUST
  evidence_tier: E2
  confidence: C4
  rationale: >
    Two concrete defects, not a preference. A published contradiction — WS-2 specifies a Redis
    session vault and ships a Redis container, R0-LLD preferred DynamoDB — meant one workstream was
    going to be rewritten. And an in-process-only cache gives N pods N answers for the length of one
    TTL, on the configuration path of every regulated action.
classification: {type: INFRA, also: [ARCH, SEC], risk_tier: T3}
priority: {now: P2, at_target: P1}
dependencies:
  blocks: ["W0b #19 configuration resolution through L2", "W4 BFF sessions", "NFR-CAC-01..03"]
  blocked_by: []
closes: "the open DynamoDB-versus-Redis session-store decision in R0-LLD §14"
action: ADMIT-BYPASS
decision: ADR-011
notes:
  - "REFUSES idempotency in the cache. INV-IDM-01 and INV-PAY-04 need the record written in the same transaction as the business change"
  - "Does NOT close TD-010 or SUG-0001 (WS-1's in-memory idempotency). A platform cache tier existing does not make a WS-1 idempotency store correct — see PARKED-BACKLOG"
  - "Does NOT soften S-21: an expired L1 and L2 with an unreachable store still refuses the action"
```

#### SUG-20260824-gp4 · event backbone in R0, with the outbox retained

```yaml
id: SUG-20260824-gp4
stage_fit:
  code: SF1
  rationale: >
    The publish contract is free to shape while no outbox table, publisher or consumer exists. Once
    `#16` Audit is written against a direct outbox poll, moving it is a rewrite of the one component
    that must not lose a record.
scope: {code: SC0, serves: ["S-17", "S-18", "S-19", "#16 Audit", "#17 Notification"]}
necessity:
  now: MUST
  future_necessity: MUST
  evidence_tier: E2
  confidence: C4
  rationale: >
    The prior decision (03-solution-architecture-r0 §5.1) set its own revisit trigger at "a third
    distinct consumer class", and R0's design already has three: audit, notification and
    compensation. The trigger therefore fires DURING the vertical slice. Adopting a broker while the
    audit path is being evidenced for a gate is the worst of the three available moments.
classification: {type: ARCH, also: [INFRA], risk_tier: T4}
priority: {now: P2, at_target: P1}
dependencies:
  blocks: ["W1 first domain events", "W3 #16 Audit consumer", "NFR-EVT-01..04", "KEDA consumer-lag scaling"]
  blocked_by: []
supersedes: "03-solution-architecture-r0.md §5.1 'why there is no event backbone in R0' — the mechanism half is retained, the timing half is withdrawn"
action: ADMIT-BYPASS
decision: ADR-012
authority_note: >
  Shailja signs the rule that no regulatory evidence exists only in a topic. It is a licence
  position, not an architecture preference, and FF-26 exists to make it checkable.
notes:
  - "The outbox STAYS. Replacing it with direct publishing would reintroduce the dual-write bug"
  - "No MSK Replicator in DR: events replay from the outbox, which Aurora already replicates (LLD D14)"
  - "Revisit trigger runs both ways — one real consumer class and no replay used at the end of R0 makes the broker a cost to withdraw"
```

#### SUG-20260824-gp5 · operational search pipe in R0

```yaml
id: SUG-20260824-gp5
stage_fit:
  code: SF1
  rationale: >
    S11 is where correlated search first pays for itself, and a log pipeline is retrofitted by
    re-emitting rather than re-indexing. It is also the only one of the five that would survive
    being deferred — which is why its necessity is SHOULD and not MUST.
scope: {code: SC0, serves: ["S09-E05 observability", "the logs gp1/gp2/gp4 generate", "security architecture §9 event classes"]}
necessity:
  now: SHOULD
  future_necessity: MUST
  evidence_tier: E3
  confidence: C3
  rationale: >
    Honest about its own strength: CloudWatch Logs Insights does work. The argument is
    investigation cost at the moment an incident is live, and the fact that three of the other four
    closures generate logs that are otherwise unqueryable. That is an operability argument, not a
    correctness one, so it is a SHOULD.
classification: {type: INFRA, also: [OPS], risk_tier: T3}
priority: {now: P2, at_target: P2}
dependencies:
  blocks: ["NFR-OBS-01..03"]
  blocked_by: ["gp2 and gp4 produce most of what makes it worth having"]
action: ADMIT-BYPASS
decision: ADR-013
authority_note: >
  Shivanshi owns observability, so the domain shape, the ISM policy and the dashboards are hers.
  Shailja signs the exclusion — OpenSearch holds no evidence — which is what allows a new
  searchable store without reopening the retention position.
notes:
  - "Explicitly NOT the analytics warehouse: Glue ETL, Athena, Redshift and QuickSight stay out (S13)"
  - "Explicitly NOT a business search index, and explicitly NOT the audit store (FF-28)"
  - "Revisit trigger: the bank's enterprise SIEM/ELK becoming available to onboard onto supersedes this domain rather than extending it"
```

#### SUG-20260825-db1 · Aarti R0 physical data architecture pack

```yaml
id: SUG-20260825-db1
raised_at: "2026-08-25"
raised_by: "human:requested-as-Aarti"
source: "Act like Aarti — create all DB design documents, rules, schema, tables, relationships, indexes, required SP, troubleshooting plan, SQL scripts for each db"
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  freshness: "WARN — state_as_of is 15 days old; review_due 2026-09-09; proceed with disclosure"
  active_work_item: DATA-001
stage_fit:
  code: SF1
  rationale: >
    S07-E04-S01..S06 and OPEN-I1 / OPEN-I6 assign the physical design to Aarti.
    S07-G5 is still OPEN. The current increment overlaps S09, which cannot
    provision Aurora schemas from an attribute sheet. Design is on-stage;
    applying Flyway is S09 (parked).
scope:
  code: SC0
  serves: ["S07-E04", "S07-G5", "OPEN-I1", "OPEN-I6", "S09-E01-S05"]
necessity:
  now: MUST
  future_necessity: MUST
  evidence_tier: E2
  confidence: C4
  rationale: >
    Named failure: S07-G5 cannot be signed and S09 data foundation cannot be
    built without a physical pack. Logical model §1 forbids treating itself as
    a schema. Cheaper alternative (invent tables per service) fails integrity.
  anti_over_engineering:
    X1_named_consumer: true
    X3_cheap_later: false
    X5_stage_necessity: true
    X9_problem_observed: true
action: ADMIT
classification:
  type: ARCH
  also: [DOC]
  breakdown: STORY
  risk_tier: T3
priority:
  now: P2
  at_target: P1
  factors: { N: 4, S: 3, B: 2, R: 2, D: 2, E: 2 }
  score: 22
  matrix_default: P2
  consistency: OK
dependencies:
  state: READY
  requires: ["ws3-platform/02", "ws3-platform/01", "ADR-008"]
  enables: ["S09-E01-S05", "S09-E03-S04", "S07-G5 human review"]
outcome:
  work_item_id: DATA-001
  plan_id: PLAN-002
  status: ADMITTED
  registered_in: "docs/platform/data-architecture/"
notes:
  - "CRUD stored procedures refused (DR-SP-01). Triggers / sequence / visibility / S09 purge specified."
  - "DynamoDB for Journey/Quote/Audit rejected for R0 (DB-DEC-0001)."
  - "S07-G5 remains OPEN. No stage field edited."
  - "Apply, proven restore and purge implementation parked — see PARKED-BACKLOG."
```

#### SUG-20260825-aln · CR-013 physical alignment

```yaml
id: SUG-20260825-aln
raised_at: "2026-08-25"
raised_by: "human:requested-as-Aarti"
source: "Check if our db is aligned with recent scope changes? is there some additional things needs to be created."
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  freshness: "WARN — state_as_of is 15 days old; review_due 2026-09-09; proceed with disclosure"
  active_work_item: DATA-001
stage_fit:
  code: SF1
  rationale: >
    CR-013 pulled Lead archive, issuanceMode, off-platform Policy ingest and R0
    admin/MIS into current R0 scope. Physical design of those facts is S07-E04 /
    W1–W4 on-stage work. Flyway apply remains S09 (already parked).
scope:
  code: SC0
  serves: ["CR-013", "DEC-20260825-01 D2-D6", "ADR-014", "ADR-012", "S07-E04"]
necessity:
  now: MUST
  future_necessity: MUST
  evidence_tier: E2
  confidence: C4
  rationale: >
    Named failure: W1/W3/W4 cannot persist admitted R0 facts against DATA-001 DDL
    (no ARCHIVED, no issuance_mode, lead_id NOT NULL, no state history, Reporting
    declared out of R0, outbox only on identity). Cheaper alternative (ignore
    CR-013) is an incorrect domain model.
  anti_over_engineering:
    X1_named_consumer: true
    X3_cheap_later: false
    X5_stage_necessity: true
    X9_problem_observed: true
action: ADMIT
classification:
  type: ARCH
  also: [DOC]
  breakdown: STORY
  risk_tier: T3
priority:
  now: P2
  at_target: P1
  factors: { N: 4, S: 3, B: 2, R: 3, D: 2, E: 2 }
  score: 24
  matrix_default: P2
  consistency: OK
dependencies:
  state: READY
  requires: ["DATA-001", "origin/main CR-013", "DEC-20260825-01", "ADR-014", "ADR-012"]
  enables: ["W1 Lead schema", "W3 issuance+ingest", "W4 isolated MIS"]
outcome:
  work_item_id: DATA-002
  plan_id: PLAN-003
  status: ADMITTED
  registered_in: "docs/platform/data-architecture/"
notes:
  - "This turn published the check (DATA-002-cr013-alignment.md, DB-DEC-0002). DDL is not implemented in the raise turn."
  - "Archive mechanism (partition vs table vs dump) remains undecided — DEC §12 joint Aarti/Mahesh."
  - "S07-G5 remains OPEN. No stage field edited."
```

---

### SUG-20260825-lt1 · Lead lifecycle archive vs Payment/Policy retention

```yaml
id: SUG-20260825-lt1
raised_at: "2026-08-25"
raised_by: "human:Mahesh"
source: "direct user instruction, acting as the Board 1 Architecture persona"
input: >
  Make sure lead transitions are well aligned. If a lead is converted and payment is done,
  transit to some other phase which can be different from the lead. Lead is something we
  work on and might need to archive based on how it grows — we do not want to preserve
  all leads for 7 years. What we want to preserve for compliance is what happened to the
  payments made, was a policy issued, and if a policy is issued the historic state
  transition for that issued policy. This keeps the lead module lightweight and easy to
  access.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  current_gate: "GATE-S08 (OPEN) — 10 of 10 exit criteria still open"
  state_as_of: "2026-08-10"
  freshness_check: "exit 1 — WARN; state_as_of is 15 days old; review_due 2026-09-09"
  active_work_item: null

stage_fit:
  code: SF3
  rationale: >
    S08 builds the engineering floor (CI, coverage, ArchUnit, secrets, no-PII-in-logs).
    It does not persist an Opportunity/Lead aggregate. The domain model already has a
    terminal CONVERTED state (01-domain-model-and-invariants.md §4.1) and separate
    Payment and Policy machines. The new work is the retention/archive contract after
    those terminals — that binds when Wave 1 designs the Opportunity service, not when
    GATE-S08 is open.
  target_stage: "W1 — Opportunity aggregate design (after GATE-S08 / S09 critical path)"
  unpark_trigger: "GATE-S08 passed and Wave 1 Opportunity / Lead aggregate design starts"
  future_necessity: MUST

scope:
  code: SC1
  business_scope: "derived — Wave 1 origination and the 7-year evidence obligation"
  serves: ["W1 Opportunity / context #5", "INV-LED-01", "INV-POL-01", "INV-JRN-05", "control C7"]
  failure_without_it: >
    If W1 persists every Lead attribute as RET-7Y (02-information-model.md §4.2 today),
    the working pipeline store becomes the 7-year bag. Disposal of working Leads later
    is a migration and may be unlawful if Shailja has not approved the class split.
  minimal: true
  authority: >
    ws3-platform/01-domain-model-and-invariants.md §4.1 / §4.8 / INV-LED-* / INV-POL-* /
    INV-JRN-05; ws3-platform/02-information-model.md §2.2 and §4.2; ADR-005; ADR-008
    naming_resolution; BOOT.md WS-3 out_of_scope_now

necessity:
  now: NOT-NOW
  future_necessity: MUST
  target_stage: "W1 Opportunity design"
  binds_when: "the first Opportunity / Lead schema is designed"
  evidence_tier: E2
  confidence: C3
  evidence:
    - "02-information-model.md §4.2 currently assigns RET-7Y to leadId, state, lob"
    - "02-information-model.md §2.2 already has RET-7Y-IMMUTABLE, RET-7Y, RET-POLICY+7Y, RET-OPERATIONAL"
    - "01-domain-model-and-invariants.md §4.1 — CONVERTED is already terminal; Payment and Policy are other aggregates"
    - "INV-POL-01 — policy is not created before Payment is RECONCILED"
    - "INV-JRN-05 — SOLD requires ACTIVE policy + RECONCILED payment + issuance confirmation + audit"
  assumptions:
    - "Shailja has not yet approved a shorter class for a converted/expired/disqualified Lead"
    - "Stakeholder 'Lead' means the working pipeline object, not the 7-year SoT"
  anti_over_engineering:
    X1_named_consumer: false    # no Opportunity service exists at S08
    X3_cheap_later: false       # retention class after first persist is a migration
    X5_stage_necessity: false   # binds at W1, not S08
    X6_simplest_sufficient: true
    X9_problem_observed: false  # no Lead store in production; this is a design correction

action: ADMIT
action_rationale: >
  Originally PARK (SF3 at S08). Stakeholder override `SUG-20260825-r0s` / `CR-013`
  pulled the archive contract into R0. Compliance conditions C-RET-1 / C-RET-2 bind.
  Not a P1 incorrect-domain-model interrupt of GATE-S08 CI: the split of aggregates
  already existed; the retention class is now written on the information-model sheet.
duplicate_of: null
conflicts:
  - >
    02-information-model.md §4.2 says Lead is RET-7Y. This suggestion asks to stop keeping
    every Lead for 7 years. Resolution: do not edit the information model in this turn.
    Park the class split for W1; Shailja owns the horizon; Aarti owns the physical archive.

classification:
  type: ARCH
  also: [COMP]
  breakdown: STORY
  epic: null
  risk_tier: T3
  destination: "CR-013 / ADR-014"

priority:
  now: P1
  at_target: P1
  rationale: "Stakeholder R0 pull; binds at W1 Lead schema"

architecture_position_draft: >
  Mahesh (AI draft, not T4 sign-off). Converted + paid is not a later phase of Lead.
  Journey reaches SOLD only when Policy is ACTIVE and Payment is RECONCILED (INV-JRN-05).
  After QUALIFIED → CONVERTED the Lead aggregate is terminal (INV-LED-01). Working-pipeline
  access goes to Policy/Payment/Journey, not to an ever-growing Lead table. leadId remains
  the opaque origin reference on every downstream aggregate (INV-LED-06) — that pointer is
  what we keep, not the working inbox. A renewal or lapse creates a NEW opportunity and a
  NEW journey (ADR-005 naming_resolution), never reopens an archived Lead. Archive is not
  delete: ID-04, disposal writes an audit record, and Shailja sets the horizon. Aarti jointly
  reviews any physical archive/purge path (shared-DB / SoT change).

outcome:
  registered_in: "CR-013 / ADR-014"
  work_item_id: CR-013
  status: ADMITTED
  override: "SUG-20260825-r0s — parking withdrawn; nothing hanging"

resumed: GATE-S08
```

---

### SUG-20260825-of1 · Off-platform sale ingest

```yaml
id: SUG-20260825-of1
raised_at: "2026-08-25"
raised_by: "human:Mahesh"
source: "direct user instruction, acting as the Board 1 Architecture persona"
input: >
  Not all products will be on the platform from day one (insurer API not ready, 1SB not
  configured, and other reasons). The distributor bank will still sell those products
  offline or on insurer portals. MIS will later upload that lead or policy so we see
  how many products were sold on-platform vs off-platform, which products still need
  onboarding, and can generate reports and insights.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  current_gate: "GATE-S08 (OPEN)"
  state_as_of: "2026-08-10"
  freshness_check: "exit 1 — WARN"
  active_work_item: null

stage_fit:
  code: SF3
  rationale: >
    This is a second intake path plus Reporting/MIS beyond the pilot funnel. R0 is one
    RM, one ETB customer, one Term product, one insurer, through a real interface.
    BOOT.md lists Reporting and MIS (context #18) beyond the pilot funnel as
    out_of_scope_now until R1. ADR-005 forbids any parallel origination path before R1.
  target_stage: "R1 — Reporting & MIS / off-platform book capture"
  unpark_trigger: "R0 completes a real pilot sale, or R1 planning starts"
  future_necessity: SHOULD

scope:
  code: SC2
  business_scope: "adjacent — full-book visibility is real, nothing in R0 fails without it"
  serves: []
  failure_without_it: "no R0 acceptance criterion fails; the pilot Term path does not need off-platform upload"
  minimal: false
  authority: "CURRENT-STATE.yaml WS-3 out_of_scope_now; ADR-005; 03-solution-architecture-r0.md §2 and §3"

necessity:
  now: NOT-NOW
  future_necessity: SHOULD
  target_stage: "R1"
  binds_when: "the bank must report the whole distributed book, not only the platform funnel"
  evidence_tier: E4
  confidence: C3
  anti_over_engineering:
    X1_named_consumer: false
    X3_cheap_later: true
    X5_stage_necessity: false
    X9_problem_observed: false

action: ADMIT
action_rationale: >
  Originally PARK (SF3 / SC2 at S08). Stakeholder override `SUG-20260825-r0s` / `CR-013`
  pulled Policy ingest into R0. Architecture position unchanged: this is Policy ingest
  (context #13) plus a Reporting read-model (context #18), not `lead.create`. MIS is
  not a BANK_RM Specified Person (C-ING-1). Campaign/bulk Lead create stays out.
duplicate_of: null

classification:
  type: FUNC
  also: [ARCH]
  breakdown: EPIC
  risk_tier: T3
  destination: "CR-013 / ADR-014"

priority:
  now: P1
  at_target: P1
  rationale: "Stakeholder R0 pull; Policy ingest in W3"

outcome:
  registered_in: "CR-013 / ADR-014"
  work_item_id: CR-013
  status: ADMITTED
  override: "SUG-20260825-r0s — parking withdrawn; nothing hanging"

resumed: GATE-S08
```

---

### SUG-20260825-st1 · STP / non-STP / Insta issuance modes

```yaml
id: SUG-20260825-st1
raised_at: "2026-08-25"
raised_by: "human:Mahesh"
source: "direct user instruction, acting as the Board 1 Architecture persona"
input: >
  While doing this we need to make sure we are aligned with STP, non-STP and Insta.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  current_gate: "GATE-S08 (OPEN)"
  state_as_of: "2026-08-10"
  freshness_check: "exit 1 — WARN"
  active_work_item: null

stage_fit:
  code: SF3
  rationale: >
    Issuance mode is a Proposal / UW / Policy dimension, exercised in Wave 3. S08 does
    not build those services. The domain model already has UW tracking and an issuance
    saga (01-domain-model-and-invariants.md §4 and F-05). The missing work is naming the
    three modes as a first-class field, the way §2.2 named lob before a second LOB existed.
  target_stage: "W3 — Proposal, UW-Tracking, Payment, Policy"
  unpark_trigger: "Wave 3 Proposal / UW / Payment / Policy design starts, and Rajal names the R0 Term value"
  future_necessity: MUST

scope:
  code: SC1
  business_scope: "derived — R0 Term issuance must be a named mode, not an implied happy path"
  serves: ["W3 Proposal & UW", "W3 Policy & Issuance", "INV-POL-01", "INV-JRN-05"]
  failure_without_it: >
    If W3 ships an implicit STP-only saga, a non-STP Term (medical / requirements) or an
    Insta product cannot reuse the same orchestration without a contract change.
  minimal: true
  authority: "01-domain-model-and-invariants.md §4; 03-solution-architecture-r0.md §2.2 (dimension-now pattern)"

necessity:
  now: NOT-NOW
  future_necessity: MUST
  target_stage: "W3 Proposal & Issuance"
  binds_when: "the first Proposal / Policy schema is designed"
  evidence_tier: E4
  confidence: C3
  assumptions:
    - "STP / non-STP / Insta are issuance modes, not Lead pipeline states"
    - "R0 Term is one of these three; Product has not recorded which"
  anti_over_engineering:
    X1_named_consumer: false
    X3_cheap_later: false      # same reason lob is present from release 1
    X5_stage_necessity: false
    X9_problem_observed: false

action: ADMIT
action_rationale: >
  Originally PARK (SF3 at S08). Stakeholder override `SUG-20260825-r0s` / `CR-013`
  pulled `issuanceMode` into the R0 Proposal/Policy schema. Do not implement three
  issuance engines. STP and Insta still cannot skip C1/C2/C4/C7 (C-ISS-1). Rajal
  names the R0 Term value; Mahesh does not guess it.
duplicate_of: null

classification:
  type: ARCH
  also: []
  breakdown: STORY
  risk_tier: T2
  destination: "CR-013 / ADR-014"

priority:
  now: P1
  at_target: P1
  rationale: "Stakeholder R0 pull; field on W3 Proposal/Policy schema"

outcome:
  registered_in: "CR-013 / ADR-014"
  work_item_id: CR-013
  status: ADMITTED
  override: "SUG-20260825-r0s — parking withdrawn; nothing hanging"

resumed: GATE-S08
```

---

### SUG-20260825-pp1 · PPHI control mapping

```yaml
id: SUG-20260825-pp1
raised_at: "2026-08-25"
raised_by: "human:Mahesh"
source: "direct user instruction, acting as the Board 1 Architecture persona"
input: >
  We are also complied with PPHI.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  current_gate: "GATE-S08 (OPEN)"
  state_as_of: "2026-08-10"
  freshness_check: "exit 1 — WARN"
  active_work_item: null

stage_fit:
  code: SF3
  rationale: >
    PPHI permissibility is a Board 6 verdict, not an S08 engineering-foundation deliverable.
    The first regulated actions that PPHI bites are consent, suitability, solicitation
    conduct and issuance disclosure — Wave 2/W3. S07 already drafted the control posture;
    human Board 6 sign-off is outstanding.
  target_stage: "W2 Consent / first regulated action"
  unpark_trigger: "Shailja opens the PPHI control mapping, or Wave 2 Consent design starts"
  future_necessity: MUST

scope:
  code: SC0
  business_scope: "in scope — policyholder protection is already in the regulatory registry"
  serves: []
  failure_without_it: "an unmapped PPHI obligation can make the first regulated journey non-permissible"
  minimal: true
  authority: >
    docs/context/roles/shailja-s-compliance-risk-head/02-regulatory-registry.md §3
    (IRDAI Protection of Policyholder's Interests, Operations and Allied Matters of
    Insurers Regulations, 2024); Shailja card — regulatory interpretation is Board 6;
    PERSONA-AUTHORITY-MATRIX — Mahesh must not waive a Compliance conclusion

necessity:
  now: NOT-NOW
  future_necessity: MUST
  target_stage: "W2 Consent"
  binds_when: "the first regulated action on a customer is implemented"
  evidence_tier: E2
  confidence: C3
  evidence:
    - "regulatory-registry.md names IRDAI PPHI 2024 as applicable to solicitation, sale, servicing, claims and customer treatment"
    - "Standing constraints already encode suitability-before-quote, consent-before-proposal, customer-device payment, RECONCILED-before-issue, no PII in logs"
    - "No repository document uses the token PPHI; the mapping to control IDs is unwritten"
  anti_over_engineering:
    X1_named_consumer: false
    X5_stage_necessity: false
    X9_problem_observed: false

action: ADMIT
action_rationale: >
  Originally PARK (SF3 at S08). Stakeholder override `SUG-20260825-r0s` / `CR-013`
  made the PPHI control-to-seam map an R0 Compliance condition (C-PPHI-1), not a later
  park. Not a Mahesh compliance declaration. Shailja owns permissibility. Human T4
  Risk & Compliance sign-off stays human. AI must not emit TEMPORARY_EXCEPTION_APPROVED.
duplicate_of: null

classification:
  type: COMP
  also: [ARCH]
  breakdown: SPIKE
  risk_tier: T4
  destination: "CR-013 condition C-PPHI-1"

priority:
  now: P1
  at_target: P1
  rationale: "C-PPHI-1 before first regulated customer action"

outcome:
  registered_in: "CR-013 / ADR-014"
  work_item_id: CR-013
  status: ADMITTED
  override: "SUG-20260825-r0s — parking withdrawn; nothing hanging"
  named_owner: "Shailja S / Board 6"
  mahesh_must_not: "declare PPHI-compliant or waive a Board 6 finding with an A-rating"

resumed: GATE-S08
```

---

### SUG-20260825-wl1 · OLTP vs ops isolation already decided

```yaml
id: SUG-20260825-wl1
raised_at: "2026-08-25"
raised_by: "human:Mahesh"
source: "direct user instruction, acting as the Board 1 Architecture persona"
input: >
  While doing this all we need to make sure we are stable on our db, and work load.
  Operations work should not effect my RM systems and lead.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  current_gate: "GATE-S08 (OPEN)"
  state_as_of: "2026-08-10"
  freshness_check: "exit 1 — WARN"
  active_work_item: null

stage_fit:
  code: SF4
  rationale: >
    The isolation rule is already the ratified communication and data architecture.
    Re-proposing it as new S08 work would import production-tuning and a warehouse
    into foundation. Confirming the rule is useful; building a second isolation
    mechanism is stage-invalid.

scope:
  code: SC0
  business_scope: "in scope as an already-decided NFR architecture rule"
  authority: >
    platform/architecture-review/03-communication-patterns.md (domain → Reporting is
    async Kafka, Reporting-only consumer); 05-data-architecture.md (Reporting on
    S3/Redshift/Athena, never shared OLTP); 04-aws-infrastructure-architecture.md
    (batch on Spot, transactional core on on-demand); ADR-013 (OpenSearch is not
    the regulatory archive)

necessity:
  now: REJECT
  evidence_tier: E1
  confidence: C5
  rationale: "Already decided. Re-building it would violate X6 and A6."

action: REJECT
action_rationale: >
  Isolation of MIS/ops from the RM and Lead path is already a standing architecture
  rule, not a gap. Domain services do not serve Reporting synchronously. Reporting
  does not share Aurora with Opportunity/Lead. Batch/analytics sit on a different
  capacity class. ADR-013 already split operational search from the 7-year evidence
  pipe for the same reason. Do not add a sidecar, replica, or warehouse in S08 to
  re-prove it.
reason: >
  Already the design. A second isolation project at foundation would be feature
  breadth (L4 reject-on-sight) and unnecessary infrastructure (Board 1 A6).
reopen_if: >
  Measured evidence that a reporting, MIS-upload, reconciliation or admin job shares
  the Opportunity/Lead Aurora writer or blocks an RM request (E3). That becomes a
  Shivanshi + Aarti incident item, not a new bounded context.

outcome:
  registered_in: "SUGGESTION-REGISTER.md"
  status: REJECTED

resumed: GATE-S08
```

---

### SUG-20260825-df1 · One decision file

```yaml
id: SUG-20260825-df1
raised_at: "2026-08-25"
raised_by: "human:Mahesh"
source: "direct user instruction — I authorised you to use all the personas we have created to take decision on your own, just make one decision file which tells what decision you have taken and why"
input: >
  I authorised you to use all the personas we have created to take dession on your
  own, just make one dession file which tells what dession you have taken and why.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: R0-ASSISTED-TERM-SALE
  current_gate: "GATE-S08 (OPEN)"
  state_as_of: "2026-08-10"
  freshness_check: "exit 1 — WARN"
  active_work_item: GATE-S08
  prior_triage:
    - SUG-20260825-lt1
    - SUG-20260825-of1
    - SUG-20260825-st1
    - SUG-20260825-pp1
    - SUG-20260825-wl1

stage_fit:
  code: SF1
  rationale: >
    The human asked for the decision artefact now, as the close of the same intake.
    Writing the file does not build a service and does not pull parked work forward.

scope:
  code: SC1
  serves: [SUG-20260825-lt1, SUG-20260825-of1, SUG-20260825-st1, SUG-20260825-pp1]
  failure_without_it: "the parked items have no single recorded design to implement against when they unpark"
  authority: "09-AI_EXECUTION_RULES.md §8 — human override"

necessity:
  now: MUST
  evidence_tier: E1
  confidence: C5
  rationale: "Human instruction to produce the file."

action: ADMIT-BYPASS
action_rationale: >
  09 §8. Who authorised: human:Mahesh. What was skipped: waiting for human board
  signatures before recording a persona consensus. What was not skipped: T4 human
  sign-off (still outstanding, stated on the file); stage-state edits; scope CR;
  implementation of parked items.
bypass_risk: >
  These are AI persona verdicts, not human Board 1 / 4 / 6 signatures, and they do
  not move GATE-S08, change out_of_scope_now, or shorten a retention horizon in the
  information model.

classification:
  type: DOC
  also: [ARCH]
  breakdown: TASK
  risk_tier: T3
  destination: "docs/governance/DEC-20260825-01-lead-domain-decisions.md"

priority:
  now: P2
  at_target: P2

outcome:
  registered_in: "SUGGESTION-REGISTER.md"
  work_item_id: DEC-20260825-01
  status: ADMIT-BYPASS
  evidence:
    - "docs/governance/DEC-20260825-01-lead-domain-decisions.md"
    - "docs/governance/registers/DECISION-REGISTER.md §8"

resumed: GATE-S08
```

---

### SUG-20260825-pv1 · No PVC for the web app

```yaml
# schema: triage-record
id: SUG-20260825-pv1
raised_at: "2026-08-25"
raised_by: "human:Mahesh"
source: "architecture consult — Flutter store apps plus web application deployed in PVC for desktop browser"
input: >
  We will have flutter app developed on playstore or apple store but the web application
  needs to be deployed in PVC which can be access with web browser on desktop so we need that.
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  freshness_check: "exit 1 — WARN, state_as_of 15 days old; review_due 2026-09-09"
  active_work_item: "Mahesh architecture consult — channel, BFF, Lead LOB"
stage_fit:
  code: SF4
  rationale: >
    Kubernetes PersistentVolumeClaim on a business or UI workload contradicts ARCH-002 and
    R0-LLD §4.1: every WS-3 service and BFF is stateless at the pod; state lives in Aurora,
    DynamoDB or S3. A PVC here becomes an unreplicated source of truth — the same defect
    the LLD already forbids for Keycloak.
scope:
  code: SC3
  business_scope: "out of technical scope — refused hosting pattern"
  serves: []
  failure_without_it: "none — CloudFront / stateless pods already host Flutter assets and the admin UI"
  minimal: true
  authority: "docs/architecture/R0-LLD.md §4.1 · ARCH-002"
necessity:
  now: REJECT
  future_necessity: REJECT
  evidence_tier: E2
  evidence:
    - "R0-LLD.md §4.1 — no PVC on domain services, Hub, #2 BFF"
    - "R0-LLD.md §4.1 — Keycloak JDBC to Aurora, not a PVC as database"
    - "R0-LLD.md BOM #5 — CloudFront for API and (later) Flutter assets"
  confidence: C5
  anti_over_engineering:
    X1_named_consumer: false
    X3_cheap_later: false
    X5_stage_necessity: false
    X9_problem_observed: false
action: REJECT
action_rationale: >
  If the ask meant VPC (private network), that topology already exists: admin/ops desktop
  web sits on the internal ALB behind API Gateway, not on a volume. PVC as PersistentVolumeClaim
  is refused at every horizon for this class of workload.
duplicate_of: null
conflicts: []
outcome:
  status: REJECTED
  closed_reason: "PVC hosting of the web app contradicts R0-LLD §4.1 / ARCH-002"
  reopen_if: "A named stateful add-on has a documented need for EBS that S3 or Aurora cannot meet, jointly reviewed by Aarti and Shivanshi"
resumed: "Mahesh architecture consult — channel, BFF, Lead LOB"
```

---

### SUG-20260825-ld1 · Lead is not LOB-specific

```yaml
# schema: triage-record
id: SUG-20260825-ld1
raised_at: "2026-08-25"
raised_by: "human:Mahesh"
source: "architecture consult — do we see any reason to make lead LOB specific?"
input: >
  Also do we see any reason to make lead LOB specific?
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  active_work_item: "Mahesh architecture consult — channel, BFF, Lead LOB"
stage_fit:
  code: SF4
  rationale: >
    Forking Lead per LOB contradicts LB-5, LS-01 and CAP-102. Sales mechanics do not vary
    by line; lob is a routing attribute on a shared inbox. Quotation #10 and Proposal #11
    are the LOB-owned execution cells.
scope:
  code: SC3
  business_scope: "contradicts ratified LOB class of context #5"
  serves: []
  failure_without_it: "none — making Lead LOB-specific would be the defect"
  authority: "01-domain-model-and-invariants.md LB-5 · 11-line-of-business-segregation.md §2.2 · R0-HLD.md Boundary 4"
necessity:
  now: REJECT
  future_necessity: REJECT
  evidence_tier: E1
  evidence:
    - "R0-HLD.md Boundary 4 — #5 Lead is LOB-agnostic shared"
    - "11 §2.2 — Opportunity/Lead, assignment, queues never inside a cell"
    - "LS-02 default is shared; a field (lobInterest) is not a boundary"
    - "DEC-20260825-01 D6 — issuanceMode is not a Lead state and Lead does not change shape per mode"
  confidence: C5
action: REJECT
action_rationale: >
  No. A Life Lead and a Health Lead are the same working inbox with a different lob
  routing key. Forking it would duplicate origination, attribution and archive across
  cells and make a cross-sell or a second-line sale look like a different customer.
duplicate_of: null
outcome:
  status: REJECTED
  closed_reason: "Lead is LOB-agnostic shared; lob is an attribute, not a deployable"
  reopen_if: "An evidenced LS-03 isolation failure showing Life Lead writes contend with Health Lead writes after a second LOB cell exists — then it is a physical split along the already-declared LOB-cell / shared-platform seam (ADR-008), not a second Lead bounded context"
resumed: "Mahesh architecture consult — channel, BFF, Lead LOB"
```

---

### SUG-20260825-st2 · Flutter public-store distribution

```yaml
# schema: triage-record
id: SUG-20260825-st2
raised_at: "2026-08-25"
raised_by: "human:Mahesh"
source: "architecture consult — Flutter on Play Store / Apple Store"
input: >
  We will have flutter app developed on playstore or apple store
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  active_work_item: "Mahesh architecture consult — channel, BFF, Lead LOB"
stage_fit:
  code: SF3
  rationale: >
    R0 Flutter is the RM workspace client (#2). Customer-facing store apps require #1
    Customer BFF, which is out_of_scope until R1. Workforce RM distribution (MDM vs
    public store) is already recorded as an open LLD §14 decision at S11, owned by
    Rajal + Deepali. GATE-S08 does not need a store listing.
  target_stage: "S11 (RM MDM vs store) / R1 (customer DIY store apps)"
  unpark_trigger: "S11 Flutter hosting decision is taken, or DIY is unparked at R1"
scope:
  code: SC2
  business_scope: "adjacent — store distribution of a client, not the R0 assisted-sale path"
  serves: []
  authority: "R0-LLD.md §14 · CURRENT-STATE.yaml out_of_scope Customer BFF"
necessity:
  now: NOT-NOW
  future_necessity: SHOULD
  target_stage: "S11 / R1"
  binds_when: "a named channel owner confirms public-store distribution"
  evidence_tier: E2
  evidence:
    - "R0-LLD.md §14 — Flutter hosting (internal MDM vs public store) | Rajal + Deepali | S11"
    - "BOOT.md — Customer BFF and customer-facing Flutter surface revisit at R1"
  confidence: C4
recurrence_count: 2
action: ADMIT
action_rationale: >
  Unparked by ADR-015 (human:Mahesh taken decision). Workforce NIP-APP ships as
  one web artefact on EKS, one APK on Play Store, one IPA on the App Store.
  Deepali jointly owns store hardening, not the distribution channel.
  Customer store apps remain R1 (#1 Customer BFF) and are not this item.
priority:
  now: P5
  at_target: P3
  caps_applied: [PRI-2, PRI-5]
outcome:
  status: CLOSED-DELIVERED
  registered_in: "SUGGESTION-REGISTER.md"
  notes: >
    Unparked 2026-08-25 by ADR-015. Workforce NIP-APP distribution is EKS nip-web
    + Play Store APK + App Store IPA. Deepali still owns store hardening
    (pinning, attestation, no tokens on device). Customer store apps remain R1 (#1).
resumed: "ADR-015 took the workforce store-listing decision"
```

---

### SUG-20260825-ac1 · Admin and ops actors for R0

```yaml
# schema: triage-record
id: SUG-20260825-ac1
raised_at: "2026-08-25"
raised_by: "human:Mahesh"
source: "architecture consult — BFF handling admin and config; additional actor admin and operations for reports and MIS"
input: >
  also we need bff handling admin and config so there will be additional actor which is
  admin and operations for working on the reports and mis.
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  active_work_item: "Mahesh architecture consult — channel, BFF, Lead LOB"
stage_fit:
  code: SF1
  rationale: >
    CR-013 / ADR-014 already pulled Administration UI and MIS into R0 W4, and R0-HLD
    Boundary 3 already names the Admin & Configuration BFF. The R0 actor catalogue still
    closes at BANK_RM · INSURER_PARTNER_REP · SERVICE. Those surfaces cannot authorise
    without an on-platform workforce actor type. This is the missing actor half of an
    admitted R0 surface, not a new channel.
scope:
  code: SC0
  business_scope: "in scope — Administration UI (#19) and Reporting/MIS (#18) are in CURRENT-STATE.yaml current_scope.in_scope"
  serves:
    - "Administration UI (context #19)"
    - "Reporting and MIS (context #18)"
    - "ADR-014 W4 admin/MIS"
  failure_without_it: "Admin UI and MIS have no principal class; PDP cannot default-deny a role that does not exist"
  minimal: true
  authority: "CURRENT-STATE.yaml WS-3 in_scope · ADR-014 · DEC-20260825-01 D4"
necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "R0 W4"
  binds_when: "first admin or MIS session"
  evidence_tier: E2
  evidence:
    - "R0-HLD.md Boundary 3 — Admin & Configuration BFF in R0"
    - "R0-HLD.md §2.1 still says two on-platform human actors"
    - "15-actor-identity-and-authorization.md — BANK_EMPLOYEE already in the target actorType vocabulary"
    - "JS-08 — a new actor type is an authorization change, not an architecture change"
  confidence: C4
  anti_over_engineering:
    X1_named_consumer: true
    X3_cheap_later: false
    X5_stage_necessity: true
    X9_problem_observed: true
action: ADMITTED
action_rationale: >
  Admit the actor-model delta only. Do not add a third journey BFF, a call-centre BFF,
  or an ops microservice. Admin/ops are BANK_EMPLOYEE on the workforce plane (Bank AD),
  served by the already-named Admin & Configuration BFF. They never originate a Lead
  (ADR-005). MIS ingest is Policy, not lead.create (D3). Isolation from the Lead writer
  remains standing (D5 / C-ISO-1 / SUG-20260825-wl1 REJECTED as new work).
classification:
  type: ARCH
  also: [SEC]
  breakdown: STORY
  risk_tier: T3
  destination: "architecture actor catalogue + WS-2 PDP grants; Deepali joint on authz"
priority:
  now: P2
  at_target: P1
  factors: { N: 3, S: 2, B: 2, R: 2, D: 1, E: 1 }
  score: 18
  matrix_default: P2
  consistency: OK
  caps_applied: []
  rationale: "W4 admin/MIS cannot ship without a principal; SF1 MUST with B=2 because the admitted surface is blocked"
dependencies:
  edges: ["ADR-014", "DEC-20260825-01 D4", "WS-2 PDP"]
  state: READY
  earliest_start: "after this consult; before W4 UI stories"
outcome:
  registered_in: "SUGGESTION-REGISTER.md"
  work_item_id: null
  status: ADMITTED
  notes: >
    Not implemented in the turn it was raised. Actor catalogue, HLD §2.1 and
    journey-execution catalogue are the artefact updates. 2026-08-25: those
    actors login to NIP-APP (`SUG-20260825-nip`); they do not get a second web.
resumed: "Mahesh architecture consult — channel, BFF, Lead LOB"
```

---

### SUG-20260825-ll1 · LLD/topology lag behind ADR-014

```yaml
# schema: triage-record
id: SUG-20260825-ll1
raised_at: "2026-08-25"
raised_by: "agent:cursor-grok"
source: "architecture consult — look in LLD and topology as well"
input: >
  also look in LLD and topology as well.
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  active_work_item: "Mahesh architecture consult — channel, BFF, Lead LOB"
stage_fit:
  code: SF1
  rationale: >
    R0-LLD.md is the S09 AWS pack. After CR-013 the HLD and SVGs say Admin BFF and #18
    are R0 W4, but R0-LLD.md §1.2 still lists admin UI and Glue/Athena/Redshift as out
    of scope, §7 Z0 still shows only Flutter RM + IPR browser, and §3 still describes
    two BFFs without the Admin & Configuration BFF. HA-03 forbids a diagram that
    disagrees with its source; here the LLD source disagrees with ADR-014.
scope:
  code: SC1
  business_scope: "derived — S09 will provision the wrong estate without it"
  serves:
    - "ADR-014"
    - "S09 platform foundation (GATE-S08 next stage)"
    - "Administration UI (context #19)"
    - "Reporting and MIS (context #18)"
  failure_without_it: "platform team reads LLD §13 OUT OF SCOPE and does not provision the admin edge, replica/read path, or desktop web hosting"
  minimal: true
  authority: "architecture/README.md HA-03 · R0-LLD.md is the S09 pack"
necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "S09"
  binds_when: "S09 stories are picked from R0-LLD.md"
  evidence_tier: E2
  evidence:
    - "architecture/README.md 2026-08-25 revision — r0-lld.svg topology unchanged"
    - "R0-LLD.md §13 — admin UI, reporting warehouse still OUT OF SCOPE"
    - "R0-HLD.md Boundary 3 — Admin & Configuration BFF in R0"
  confidence: C5
  anti_over_engineering:
    X1_named_consumer: true
    X3_cheap_later: false
    X5_stage_necessity: true
    X9_problem_observed: true
action: ADMITTED
action_rationale: >
  Document reconciliation only. Do not invent a warehouse, a PVC, or a third public
  hostname in this item. The LLD must show: Admin BFF in ns:edge; desktop admin web as
  static assets (CloudFront or internal ALB), not a PVC; #18 on the isolated read path
  (replica/events — not Glue ETL in S08); Z0 gains an admin/ops browser. Shivanshi owns
  the operability of the resulting BOM.
classification:
  type: DOC
  also: [ARCH]
  breakdown: STORY
  risk_tier: T2
  destination: "docs/architecture/R0-LLD.md + diagrams/"
priority:
  now: P3
  at_target: P2
  factors: { N: 3, S: 1, B: 1, R: 2, D: 1, E: 1 }
  score: 14
  matrix_default: P2
  consistency: OK
  caps_applied: []
  rationale: "SF1 MUST documentation; score P3 is within one band of matrix P2"
dependencies:
  edges: ["ADR-014", "SUG-20260825-ac1"]
  state: READY
outcome:
  registered_in: "SUGGESTION-REGISTER.md"
  work_item_id: SUG-20260825-ll1
  status: CLOSED-DELIVERED
  notes: >
    R0-LLD.md §3.1 places rm-web and admin-web as image-baked pods in ns:edge
    on the internal ALB (no PVC). r0-lld.svg and generated topology show four
    edge pods, admin/ops in Z0, and #18 on ns:jobs. Warehouse stays out.
    2026-08-25: SUG-20260825-nip retracts admin-web and admin.{env}; one NIP-APP.
resumed: "Mahesh architecture consult — channel, BFF, Lead LOB"
```

---

### SUG-20260825-nip · One NIP-APP, role-based, not a second admin UI

```yaml
# schema: triage-record
id: SUG-20260825-nip
raised_at: "2026-08-25"
raised_by: "human:Mahesh"
source: "architecture consult — follow-up after PR #75; no separate admin/ops web"
input: >
  one important thing, we dont have or not in future will have different app or web
  for the admin and operations, it will be in the RM web application only, it just
  role based view will be shown. the application is enterprise application where RM,
  Insurance sales representative and admin, operations all will login, just based on
  the role everyone be able to see their perspective only. Also this APP in not RM app
  actually you can call it NIP - New Insurance Platform Web Application (NIP - APP).
  same app will be customised for andriod and ios and we will deploy on the stores.
context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE"
  state_as_of: "2026-08-10"
  active_work_item: "none — SUG-20260825-ll1 CLOSED-DELIVERED on PR #75"
stage_fit:
  code: SF1
  rationale: >
    The S09 pack just published (PR #75 / R0-LLD.md §3.1) draws a separate admin-web
    pod and admin.{env} hostname. That picture contradicts JS-08 / ID-22 (actors are
    authorization, not a second architecture) and the stakeholder channel model.
    Correcting the channel topology is on-stage for the overlapped S08/S09 pack.
    Public-store listing is not this item — that remains SUG-20260825-st2 (SF3).
scope:
  code: SC1
  business_scope: "derived — the R0 workforce channel and W4 admin/MIS UI are in scope; a second app is not"
  serves:
    - "SUG-20260825-ac1"
    - "ADR-014 W4 Administration UI / MIS"
    - "R0-LLD.md S09 pack"
    - "R0-HLD.md Boundary 1 and 3"
  failure_without_it: >
    S09 provisions admin-web and a second public hostname that Product will never
    have; HLD keeps calling the client an RM app; PDP stories split across two UIs
  minimal: true
  authority: "12-journey-segregation.md JS-08 JS-10 · 15-actor-identity-and-authorization.md ID-22 TI-15 · ADR-014"
necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "S09 platform pack + R0 W4 UI"
  binds_when: "next HLD/LLD revision; before any admin hostname is provisioned"
  evidence_tier: E1
  evidence:
    - "human:Mahesh — one enterprise app, role-based, no second admin/ops web now or later"
    - "JS-08 — a new actor type is an authorization change, not an architecture change"
    - "JS-10 / TI-15 — UI hide/show is usability; PDP is the control"
    - "ID-22 — the same capability serves every actor; no service per actor type"
    - "HLD Boundary 3 — one BFF per channel, never per channel × LOB"
  confidence: C5
  anti_over_engineering:
    X1_named_consumer: true
    X3_cheap_later: false
    X5_stage_necessity: true
    X9_problem_observed: true
action: ADMIT
action_rationale: >
  Collapse the channel to one Flutter client named NIP-APP (New Insurance Platform),
  delivered as web plus iOS/Android from the same codebase. BANK_RM, INSURER_PARTNER_REP
  (Insurance sales representative) and BANK_EMPLOYEE (admin/ops) all authenticate to
  it; perspective is role + PDP, not a second deployable. Remove admin-web, admin.{env}
  and a second public workforce BFF from HLD/LLD/topology. Admin/MIS functions stay
  in R0 W4 as role-gated routes on #2; C-ISO-1 still forbids Lead-writer use.
  Amit may keep admin aggregation as a module inside the one BFF process.
  Spoken name NIP-APP is recorded here; Rajal owns a conflicting official product
  name if the bank uses a different label. Public Play/Apple Store listing is NOT
  admitted — recurrence of SUG-20260825-st2, still S11 (Deepali + Rajal).
conflicts:
  - "R0-LLD.md §3.1 admin-web / admin.{env} from SUG-20260825-ll1 — retract on implement"
  - "R0-HLD.md Boundary 1 separate admin/ops hostname — retract on implement"
classification:
  type: ARCH
  also: [DOC, SEC]
  breakdown: STORY
  risk_tier: T2
  destination: "R0-HLD.md · R0-LLD.md · diagrams · actor catalogue (ac1)"
priority:
  now: P2
  at_target: P1
  factors: { N: 4, S: 3, B: 2, R: 2, D: 2, E: 1 }
  score: 23
  matrix_default: P2
  consistency: OK
  caps_applied: []
  rationale: "SF1 MUST; PRI-8 B floor 1, raised to 2 (ac1 destination + S09 pack). Score 23 is P2, matrix default P2"
dependencies:
  edges: ["SUG-20260825-ac1", "ADR-014", "JS-08"]
  state: READY
outcome:
  registered_in: "SUGGESTION-REGISTER.md"
  work_item_id: SUG-20260825-nip
  status: CLOSED-DELIVERED
  notes: "Implemented 2026-08-25 as ADR-015. HLD/LLD/topology: ns:edge is nip-web + #2 NIP BFF only. Human T4 Architecture sign-off outstanding."
resumed: "SUG-20260825-nip CLOSED-DELIVERED; ADR-015 drafted PROPOSED"
```

---

### SUG-20260827-err — platform error contract

```yaml
# schema: triage-record
id: SUG-20260827-err
raised_at: "2026-08-27"
raised_by: "human:Mahesh"
source: "direct requirement, spoken, 2026-08-27"
input: >
  Make the utilities mature enough to be used across multiple services, the error one especially.
  We should be able to identify error requests and error responses, understand which service has
  an error and from which service we got the error that caused the request to fail. There has to
  be a standard error response across services carrying the service name or number. Configure it
  so the end user does not understand the exact error and gets a plain response back, while dev
  users and L1/L2 support can understand exactly what is wrong. Different error classes —
  resource not found, authentication, authorization, validation, constraint failures — need
  customised messages; we should not randomly state the same error. Example: the request lands on
  the BFF, moves to customer consent, and the orchestrator hits a validation error — it must
  clearly state that this service's validation failed for this reason, not to the end user but to
  dev users and in logging. Errors must be clean, crisp and monitorable on the centralised logging
  system so we can build a dashboard or graph of which errors are populating more. This is the
  priority requirement; error handling has to be strong from day one for release zero, not after
  the application matures, so we do not spend a lot of time debugging what fails, where and how.
  The logs and error messages must answer why, how, when, where and what failed, and what to do
  about it — so that L1 support can have a manual saying in such a case perform this action.

context:
  workstream: WS-3
  current_phase: "Foundation Recovery Increment — S08 with S09 overlapped"
  canonical_stage: "S08 — Engineering Foundation"
  current_objective: "R0-ASSISTED-TERM-SALE — one RM sells one Term Life policy to one ETB customer end to end"
  state_as_of: "2026-08-10"
  state_provisional: false
  active_work_item: null

stage_fit:
  code: SF1
  rationale: >
    Shared-library hardening with three consumers today (1sb-integration, bank-persistence,
    workforce-access-bff), which is the SF1 condition in 03 section 7 for the shared-library
    family — SF3 applies only when fewer than two consumers exist. It serves the open S08 gate
    directly: S08-G7 (no PII in logs, proven by automated test) is provable by asserting over a
    finite error registry rather than over every log statement, and S08-G8 (engineering and
    secure coding standards published and adopted) is what this contract is. SF0 was considered
    and not claimed: G7 could in principle be satisfied by a log scrubber, so the gate is not
    strictly unexitable without this.

scope:
  code: SC1
  business_scope: "in scope — derived from the open S08 gate and the ratified R0 error catalogue"
  serves:
    - S08-G7
    - S08-G8
    - GATE-P4-4.4
    - GATE-P4-4.5
  failure_without_it: >
    Upstream 1SB text and internal routes are returned to bank callers verbatim
    (OneSbErrorNormaliser sets .detail(parsed.detail()); throw sites set
    .detail("1SB call failed: " + method + " " + path)), so the response body leaks vendor
    identity and internal topology. No error carries a service, origin or layer, so a failure
    observed at the BFF cannot be attributed to the service that produced it. The ~60 refusals
    ratified in journey-execution/04 are unimplemented — ErrorCodes has 24 codes that barely
    intersect the catalogue — so the catalogue is paper and the compliance-gate refusals it
    specifies as evidence are not emitted as such.
  minimal: true
  authority: "docs/journey-execution/04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md · CURRENT-STATE.yaml GATE-S08"

necessity:
  now: MUST
  future_necessity: MUST
  target_stage: "S09 — Platform & Environment Foundation"
  binds_when: "first bank caller consumes an error response contractually"
  failure_without_it: >
    A partner-consumed contract is retrofitted after publication, which is the one case 05 scores
    as high decay: ErrorCodes is documented as stable because it appears in partner responses.
  evidence_tier: E1
  evidence:
    - "Code: OneSbErrorNormaliser .detail(parsed.detail()) returns upstream text to the caller"
    - "Code: OneSbHttpClient .detail(\"1SB call failed: \" + method + \" \" + path) leaks internal routes"
    - "Code: ServiceErrorResponse has no service, origin, layer or incident field"
    - "Code: three divergent handlers — 1SB, persistence, and a bare Spring ProblemDetail at the BFF with no code at all"
    - "Doc: journey-execution/04 defines ~60 codes; ErrorCodes defines 24 with minimal overlap"
    - "Gate: GATE-S08 criteria G7 and G8 are OPEN"
  confidence: C4
  assumptions: []
  anti_over_engineering:
    X1_named_consumer: true
    X2_two_implementations: true
    X3_cheap_later: false
    X4_reversibility: true
    X5_stage_necessity: true
    X6_simplest_sufficient: true
    X7_runtime_cost: false
    X8_cognitive_cost: true
    X9_problem_observed: true
    X10_do_nothing: true

action: ADMIT
action_rationale: >
  SF1 x MUST = ADMIT at P1-P2, with SC1 satisfied on all three tests of 02 section 3.1 — named
  beneficiary (S08-G7, S08-G8), demonstrable failure (the leak and the attribution gap, both
  present in code today), and minimality (hardening two existing libs, additive only, no new
  dependency). X3 fails deliberately: this cannot be added cheaply later because ErrorCodes is a
  published partner contract, which is the argument for doing it now rather than the argument
  against doing it.
conflicts:
  - "S08 posture rejects generic frameworks on sight (BOOT section 4). Resolved: this hardens two
     existing libs with three existing consumers and adds no dependency, rather than introducing a
     framework. Recorded in ADR-017 alternatives as the rejected option."
  - "ci-checks reports 22 pre-existing schema failures in this register's older detail blocks.
     Not repaired here — out of this item's scope, and repairing them silently would hide drift
     that belongs to its own item."

classification:
  type: ARCH
  also: [SEC, COMP, NFR]
  breakdown: EPIC
  epic: EPIC-001
  risk_tier: T3
  rationale: >
    Four epic triggers fire (multiple stories, multiple services, multiple acceptance outcomes,
    multiple increments), and two force an epic. T3 by Rule RG-6: G9 is the close call because
    ErrorCodes is partner-consumed, so the contract is constrained to additive-only and any story
    proposing to change an existing value is T4 and stops. G2 does not fire because the change
    narrows exposure; G10 does not fire because catalogue 04's audit behaviour is carried, not
    redefined.
  destination: "docs/journey-execution/07-PLATFORM-ERROR-CONTRACT.md · ADR-017"

priority:
  now: P2
  at_target: P1
  factors: { N: 4, S: 3, B: 2, R: 2, D: 2, E: 3 }
  score: 21
  matrix_default: P2
  consistency: OK
  overrides_applied: []
  caps_applied: []
  rationale: >
    2(4) + 2(3) + 2(2) + 2(2) + 2 - 3 = 21, band P2, matching the matrix default of P2 for
    SF1 x MUST at its lower-urgency end. B=2 for the two open S08 gate criteria and the two
    WS-1 Phase 4 criteria it serves; B=3 was not claimed because it contributes to those criteria
    rather than solely blocking them. D=2 because ErrorCodes is a published partner contract and
    retrofitting it later is a breaking contract change. E=3 for five services plus two libs.
    A hard P1 override was considered and deliberately NOT claimed: O6 covers leaking in-flight
    data, and the leak here is upstream vendor text and internal route strings to an authenticated
    bank caller, not regulated customer data; O2 requires reachable and exploitable. Claiming
    either would be override inflation under 05 section 3, so this is scored normally and is P2.
    P1 at target: at S09 and first partner consumption the same item becomes gate-blocking.

dependencies:
  edges:
    - type: ARCHITECTURAL
      target: "docs/journey-execution/04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md"
      relation: requires
      state: DONE
    - type: DECISION
      target: ADR-017
      relation: decision_dependency
      state: OPEN
    - type: TECHNICAL
      target: S08-G7
      relation: enables
      state: OPEN
    - type: TECHNICAL
      target: S08-G8
      relation: enables
      state: OPEN
  state: READY
  enablement_count: 4
  earliest_start: "immediately — no blocking edge"
  cycles: none

breakdown:
  children: [ERR-001, ERR-002, ERR-003, ERR-004, ERR-005, ERR-006, ERR-007]
  completion_definition: >
    All five services return the section 4.2 envelope AND no response crossing L4 carries a
    diagnostic field, proven by test AND the registry and catalogue 04 agree, proven by CI AND
    bank.error.count is emitted with the section 7 tag set AND the S08-G7 PII test asserts over
    the registry.
  not_included:
    - "Any change to an existing ErrorCodes value — additive only, trigger G9"
    - "Grafana dashboards and alert rules — L9"
    - "Retry, circuit breaker and bulkhead policy — unchanged, 04 section 8"
    - "Error codes for the codeless degraded states in 04 section 7"
    - "Repair of the 22 pre-existing schema failures in this register"

outcome:
  registered_in: "registers/SUGGESTION-REGISTER.md"
  work_item_id: EPIC-001
  plan_id: "07-PLATFORM-ERROR-CONTRACT.md section 11"
  status: ADMITTED
  closed_reason: null

resumed: "EPIC-001 — no prior work item was in flight; this session opened with this input."
```

---

## 4. Seeded from existing artefacts

AIGEM was adopted mid-flight. Rather than backfilling every past decision
([19 §5](../19-PORTING_GUIDE.md#5-bootstrapping-into-an-existing-project-mid-flight)), the
already-deferred items in [TECH-DEBT.md](../../1sb-insurance-integration/service-ssot/TECH-DEBT.md)
were seeded directly into [PARKED-BACKLOG.md](./PARKED-BACKLOG.md) as pre-existing parked work.
They keep their `TD-###` IDs; no `SUG-####` was minted retrospectively.

**Do not re-triage or re-report these** — they are known
([01 §6](../01-CURRENT_STATE.md#6-known-open-debt-affecting-triage)).

---

## 5. Register row convention (machine-enforced)

> **A table row whose first cell is a bare ID is that ID's DEFINITION.** Exactly one definition
> may exist per ID, across every register. `FreshnessCheck` enforces this and halts on a
> duplicate — that is how a cross-branch ID collision is caught after a merge.
>
> Cross-reference rows — the same item shown again in another view, such as an external
> dependency also listed under its edge, or an open risk repeated under accepted risks — must
> **point at** the definition rather than restate the bare ID — for example a leading cell
> of `→ [DEP-002](./DEPENDENCY-REGISTER.md#1-edges)` instead of a bare `DEP-002`.
