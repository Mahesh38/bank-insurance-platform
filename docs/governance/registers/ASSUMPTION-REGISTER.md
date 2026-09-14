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
| ASM-012 | R0 still owns a per-environment inspection / egress VPC with AWS Network Firewall. 1SB/PG **allowlisted IPs** were assumed to be those NAT EIPs (`ADR-010`). Hub FortiGate already inspects org egress. Human 2026-09-14 direction (`ASM-015`) says 1SB allowlists **Apigee** IPs, so spoke NAT EIPs may never be published to 1SB — the remaining question is whether a spoke firewall still sits on **pod → Apigee** | ADR-010, R0-LLD §2.3, SUG-20260914-egr | Written: (a) Apigee egress IP list for 1SB; (b) Deepali on remaining spoke NFW; (c) bank network if EDGE FortiGate is also on that hop | **S09 network build start** | OPEN | If Apigee is the only IP 1SB sees, do **not** publish spoke EIPs (`DEP-20260824-eip` rebases). If Deepali still requires spoke NFW on pod→Apigee, keep `ADR-010` topology without the 1SB-allowlist clause |
| ASM-014 | 1SB **quote-category** master entities are gateway-common (do not vary by insurer). **Proposal-category** master entities are insurer-specific via `manufacturerId`. Portal Get Master Details (2026-09-13): `lookUpCategory` quote\|proposal; `manufacturerId` “Manufacturer ID of which need to find the enum value for proposal.” Entity lists described as “supported by the system.” Not live-proven on sandbox (auth 500). | Hub masters, `SUG-20260913-hms` | Call 1SB lookup for `quote`+`GENDER` with and without `manufacturerId` on a recovered sandbox; compare lists | Gate 4.1 or first live master 200 | OPEN | If quote lists differ by manufacturer, Hub quote masters become insurer-keyed too — do not treat them as a single Hub list |
| ASM-015 | **Outbound** API calls from this platform (1SB, SMS, other leaving-the-building HTTPS) traverse the bank **Apigee** plane. `1sb-integration-service` calls an Apigee proxy, never the 1SB origin. 1SB IP-whitelists **Apigee egress IPs**. **Inbound** RM/mobile (and PG callbacks unless written otherwise) stay on Cloudflare → F5-XC → **Amazon API Gateway** (`ADR-018`) | SUG-20260914-egr, SPIKE-001, ADR-018 | Written from bank API platform: Apigee edition; NIP product; per-env egress IPs; per-1SB-path onboarding; private spoke→Apigee connectivity | **S09 P4 / first 1SB allowlist** | OPEN | If Apigee cannot proxy 1SB, revert outbound to `ADR-010` NAT EIPs and rebase 1SB allowlists. If Apigee is also mandated on **ingress**, amend `ADR-018` (do not guess) |
| ASM-016 | Internal bank APIs (AD-verify, EBS/CBS, similar) that go via Apigee stay on a **private** path. They must not hairpin the public internet (Cloudflare + F5) | SUG-20260914-egr, SUG-20260914-idp | Network + Apigee team: internal/private target, hostname, no public DNS | **S09 P4 / first AD-verify call from uat** | OPEN | If the only Apigee front door is public, do not send AD/CIF through it; use `ADR-009` TGW → FortiGate → DX until a private Apigee target exists |
| ASM-017 | `dev` is a **slice inside the UAT AWS account** (namespaces, schemas, prefixes, synthetic data), not a separate Control Tower account | SUG-20260914-uat, C-02 | LLD BOM #1 amended + Cloud vending form without a split `dev` account | **Before account-create pack (C-05)** | OPEN | If Cloud refuses co-tenancy, revert to C-02 option (a) documented exception and a separate `dev` account |
| ASM-018 | R0 does **not** need a CUG environment. Onboarding CUG, if still listed as mandatory, is waived rather than provisioned | SUG-20260914-uat, C-03 | Written waiver on the onboarding pack, or Cloud confirmation “UAT is the closed path” | **Before account-create pack (C-05)** | OPEN | If Cloud mandates CUG anyway, add a CUG account with a problem statement — do not invent a fourth production quietly |
| ASM-019 | Workforce (RM / bank employee) credentials are verified by an **existing bank API against AD**, reached via Apigee (`ASM-016`). The platform never binds LDAP to AD. Insurance sales partners are created in the IdP (bulk or one-by-one, maker-checker), never in AD. Keycloak is acceptable if NIP-APP / Fireframe is the only UI for login and for user/role/permission mapping; Keycloak chrome is not shown | SUG-20260914-idp, TI-01, ID-04, ID-12 | Deepali on password-in-NIP vs Fireframe SSO ceremony (`ID-11`); adapter talks to the bank verify API; partner provision APIs on PDP + adapter | **First workforce login story** | OPEN | If the bank forbids NIP handling AD passwords, login becomes Fireframe/SSO redirect (adapter still). If Keycloak theming cannot be hidden, replace IdP product behind the adapter — do not expose Keycloak to Flutter |

## 2. Validated

| ID | Assumption | Validated | Evidence |
|----|------------|-----------|----------|
| ASM-001 | WS-1 is in Phase 4 (Hardening); Phases 0–3 are complete | 2026-08-10 | Ratified by the Solution Architect — GOV-004 in the [decision register](./DECISION-REGISTER.md#2-governance-decisions) |

## 3. Invalidated

| ID | Assumption | Invalidated | Consequence taken |
|----|------------|-------------|-------------------|
| *(unregistered · SUG-20260825-arb hop)* | An External / public ALB is required in front of Amazon API Gateway, and F5 is an appliance we place in AWS / our VPC | 2026-08-31 | Retracted by `ADR-018` / `SUG-20260831-alb`. Ingress is Cloudflare (SaaS) → F5-XC (SaaS) → API Gateway → Internal ALB. F5 on this estate is F5 Distributed Cloud, not an in-VPC BIG-IP |
| ASM-013 | All inbound **and** outbound API traffic for this platform is required to traverse Apigee, so Amazon API Gateway is a second hop unless Apigee cannot cover the product | 2026-09-14 | Human Architecture owner split the plane (`SUG-20260914-egr`): **outbound** via Apigee (`ASM-015`); **inbound** RM/mobile keeps Amazon API Gateway (`ADR-018`). SPIKE-001 continues for written Apigee-team answers |

---

## 4. Using assumptions in triage

An assumption may serve as evidence only at tier **E5** — expert reasoning with a named
mechanism ([16 §4](../16-DECISION_MODEL.md#4-evidence-standard)). Rule EV-1 therefore applies:

> A **MUST** claim resting only on an unvalidated assumption downgrades to **SHOULD** until the
> assumption is validated.

When an assumption is invalidated, [16 §7](../16-DECISION_MODEL.md#7-revalidation-triggers)
requires re-validating **every item and plan that cites it** — which is why `used_by` is
mandatory rather than nice to have.
