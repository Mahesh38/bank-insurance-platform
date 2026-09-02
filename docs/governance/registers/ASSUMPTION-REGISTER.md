# Assumption Register

Beliefs the plan rests on that have not been verified. Every assumption has an expiry, a
validation method, and — critically — a **pre-computed consequence if it turns out to be
false**, so invalidation triggers a known action instead of a debate.

**Owner:** whoever relies on the assumption
**Model:** [16 §4](../16-DECISION_MODEL.md#4-evidence-standard)

---

## 1. Open assumptions

| ID | Assumption | Used by | Validation | Expiry | Status | If invalidated |
|----|------------|---------|------------|--------|--------|----------------|
| ASM-002 | The service runs single-instance through Phase 4 | TD-010, RISK-004 | Confirm with Ops at the gate review | Phase 4 gate | OPEN | TD-010 becomes P1, not P4; Redis work pulls into Phase 4 via CR |
| ASM-003 | 1SB sandbox is stable enough for CI-gated E2E | Gate 4.1 | First E2E run over one week | Gate 4.1 delivery | OPEN | Fall back to gated nightly (already sanctioned by ACTION-PLAN 4.1) |
| ASM-004 | At least one bank app team is available for UAT integration this stage | Gate 4.3, DEP-002 | PO confirms a named team and slot | Phase 4 gate | OPEN | 4.3 becomes externally blocked; gate needs a waiver or the criterion moves to Phase 5 |
| ASM-005 | 7-year retention for auth/admin events is the correct regime | WS-2 A.5, CMP-3 | Compliance confirmation | WS-2 Phase 1 gate | OPEN | Retention config changes; data already written may need remediation |
| ASM-006 | No AWS deployment target before Phase 6 | TD-006, RISK-005 | Platform roadmap confirmation | Phase 5 gate | OPEN | TD-006 jumps to P1; secrets provider work pulls forward |
| ASM-007 | Health and Motor reuse `QuoteService` orchestration unchanged | Phase 5 planning, TD-009 | First Health handler spike | Phase 5 entry | OPEN | TD-009 (domain ports) becomes a prerequisite, not a deferral; Phase 5 sizing grows |
| ASM-008 | Compliance will accept audit coverage limited to quote/proposal/payment paths | TD-023, gate 4.4 | Compliance review (gate 4.4) | Gate 4.4 | OPEN | TD-023 becomes P1 COMP work in Phase 4, not a Phase 5 deferral |
| ASM-009 | The bank will terminate a Site-to-Site VPN and publish CBS/AD prefixes **well before** the Direct Connect circuit is accepted, so `uat` can leave stubs behind without waiting for a carrier order | ADR-009, gp1, DEP-20260824-dx1, RISK-013 | Written confirmation from the bank network team, with a date | **2026-09-15**, or S09 network build start — whichever is first | OPEN | The VPN-first sequencing loses its point and `ADR-009` reverts to a single DX-dependent path: `uat` keeps stubs, `#4` Customer and WS-2 Phase 2 cannot be evidenced, and RISK-013 escalates to exposure 9 |
| ASM-010 | The starting shapes in `R0-LLD` §1.4 are adequate at CAP-A volumes — 2 cache nodes, 3 brokers, 3 firewall endpoints, a small search domain — because the platform is correctness-constrained rather than throughput-constrained | ADR-011, ADR-012, ADR-013, NFR-EVT-02, NFR-CAC-01 | Measured at S12 load test: eviction rate, consumer lag, firewall processing latency, index queue depth | S12 load test | OPEN | Scale **up** before out (the recorded order), and re-run the `NFR-OPEN-6` envelope — a resize is a cost change, and the shapes were chosen for availability, not headroom |
| ASM-011 | Consumers of the event backbone can be made idempotent on `eventId` and replay-tolerant, which is what makes "no broker in DR" a design rather than a gap | ADR-012, LLD D14, NFR-EVT-03 | The replay drill at S09 (`NFR-EVT-03`) — zero duplicates, zero gaps | **S09 (`S09-G7`)** | OPEN | The DR position reopens: either MSK Replicator enters the BOM with its cost, or the audit path is rebuilt to be replay-safe. Do not discover this at a failover |
| ASM-012 | R0 still owns a per-environment inspection / egress VPC with AWS Network Firewall and the 1SB/PG-allowlisted Elastic IPs (`ADR-010`). The existing AU Bank central network already inspects east-west and internet egress through the `AU-CTO-NETWORK` EDGE VPC (FortiGate NGFW, Active/Passive) and Transit Gateway. Those two planes coexist: we attach as a spoke; we do not place F5 appliances in our VPC | ADR-010, ADR-018, R0-LLD §2.3 | Written confirmation from the bank network team that the insurance spoke keeps its own inspection VPC (and EIPs) rather than sharing the existing EDGE FortiGate path only | **S09 network build start** | OPEN | If the bank mandates egress solely through the existing EDGE FortiGate, `ADR-010`'s inspection VPC is withdrawn and the allowlisted EIPs move to that EDGE; Shivanshi re-bases the Terraform modules before any EIP is published |
| ASM-013 | All inbound and outbound API traffic for this platform is required to traverse the bank's existing **Apigee** plane. Amazon API Gateway is therefore a second hop unless a named job remains that Apigee cannot do (PG-callback isolation, private target into the new spoke, or Apigee not covering this product) | SUG-20260831-apg, SPIKE-001, ADR-018 | Written confirmation from the bank API platform + network team: Apigee edition (X / hybrid / on-prem); NIP as a new Apigee product; whether 1SB and PG callbacks already go through Apigee; how Apigee reaches a new TGW spoke | **S09 P4 edge band start** | OPEN | If Apigee does not cover this platform, `ADR-018` stands (Amazon API Gateway remains Proxy 1). If it does, Amazon API Gateway is withdrawn and 1SB/PG allowlists move to Apigee's egress IPs, not our NAT EIPs |

## 2. Validated

| ID | Assumption | Validated | Evidence |
|----|------------|-----------|----------|
| ASM-001 | WS-1 is in Phase 4 (Hardening); Phases 0–3 are complete | 2026-08-10 | Ratified by the Solution Architect — GOV-004 in the [decision register](./DECISION-REGISTER.md#2-governance-decisions) |

## 3. Invalidated

| ID | Assumption | Invalidated | Consequence taken |
|----|------------|-------------|-------------------|
| *(unregistered · SUG-20260825-arb hop)* | An External / public ALB is required in front of Amazon API Gateway, and F5 is an appliance we place in AWS / our VPC | 2026-08-31 | Retracted by `ADR-018` / `SUG-20260831-alb`. Ingress is Cloudflare (SaaS) → F5-XC (SaaS) → API Gateway → Internal ALB. F5 on this estate is F5 Distributed Cloud, not an in-VPC BIG-IP |

---

## 4. Using assumptions in triage

An assumption may serve as evidence only at tier **E5** — expert reasoning with a named
mechanism ([16 §4](../16-DECISION_MODEL.md#4-evidence-standard)). Rule EV-1 therefore applies:

> A **MUST** claim resting only on an unvalidated assumption downgrades to **SHOULD** until the
> assumption is validated.

When an assumption is invalidated, [16 §7](../16-DECISION_MODEL.md#7-revalidation-triggers)
requires re-validating **every item and plan that cites it** — which is why `used_by` is
mandatory rather than nice to have.
