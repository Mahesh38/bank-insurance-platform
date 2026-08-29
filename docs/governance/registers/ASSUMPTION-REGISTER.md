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
| ASM-012 | The bank will confirm the GitLab base URL, version and **edition/licence** in time for `GLM-001` M3.4 | CR-014, RISK-017, M3.4 | Written confirmation from the bank GitLab platform team | **2026-09-19** | OPEN | M3.4 module capability cannot be fixed; if Premium, `S08-G5`'s mechanism reopens and only Deepali may grant the exception |
| ASM-013 | The existing `insurance` parent group exists, its ID is obtainable, and this team is granted subgroup/project creation rights beneath it | CR-014, M1.3, M4.1 | Group ID supplied and a test subgroup created | **2026-09-19** | OPEN | M4 cannot run. The estate has no parent and `IMP-11` #3 (import, never create) has nothing to import |
| ASM-014 | Enterprise SSO/LDAP identity groups exist for the eleven logical teams and can be referenced by Terraform | CR-014, M1.4, M6.4 | Group names/IDs supplied | **2026-09-19** | OPEN | Memberships fall back to individual assignment, which the baseline §5.2 forbids as the normal mechanism; least-privilege intent degrades |
| ASM-015 | Runners are centrally managed and group/enterprise runners are available, including production-capable protected runners | CR-014, M1.5, M7.12 | Runner inventory and tags supplied | **2026-09-19** | OPEN | The programme acquires a runner build it did not scope; `S08-G9` cannot be measured; M7.12 slips |
| ASM-016 | A bank-approved remote Terraform state backend exists, supporting encryption, locking, versioning and recovery | CR-014, M1.6, M3.3 | Backend named and access granted | **2026-09-19** | OPEN | `backend.tf` cannot be written on a guess. M3.3 blocks; local state is prohibited and there is no fallback |
| ASM-017 | Container and Package Registry are available on the bank instance for the projects that need them | CR-014, M1.7, M6.7 | Feature availability confirmed | **2026-09-19** | OPEN | Image publication needs an external registry, and the baseline §9.1 image-naming and digest-promotion model needs re-designing |
| ASM-018 | AWS account and role conventions for OIDC/STS are published, and a bank AWS team can create the identity provider and roles | CR-014, M1.8, M8.3 | Conventions supplied, owner named | **2026-09-19** | OPEN | M8 stops at design. Static keys are not an accepted fallback (`C-SEC-7`) — the deployment path waits |
| ASM-019 | Retention and audit requirements for CI logs, artifacts and evidence can be obtained and enforced by configuration | CR-014, M1.9, `C-CMP-3` | Requirements supplied by Compliance + bank | **2026-09-19** | OPEN | Evidence lands in default-expiry artifacts. Seven-year evidence in a 30-day artifact is not retained, it is scheduled for deletion |
| ASM-020 | A dedicated least-privileged GitLab automation service account can be issued, distinct from the application deployment identity | CR-014, `C-SEC-5` | Account issued and scoped | **2026-09-19** | OPEN | The only alternative is a personal access token, which baseline §5.1 forbids and Deepali has made non-waivable |
| ASM-021 | The bank's GitLab platform or architecture authority will rule on the Appendix C exception for `governance/platform-governance` | CR-014 §6, `ADR-018`, `C-ARC-2` | Written acceptance or refusal | **2026-09-19** | OPEN | Fallback recorded in `DEC-20260829-01` §3.2 — the tree goes to `product/backend` under `governance/`, accepting the `S08-G10` and build-on-every-change costs |
| ASM-022 | The bank GitLab instance **and its storage** are in AWS India regions, or otherwise permissible under the residency standing constraint | CR-014, RISK-021, `C-CMP-1` | Written confirmation of physical residency, ruled by Board 6 | **2026-09-19** | OPEN | **The destination is invalid, not merely late.** No push proceeds. Raised as `CMP-F01`; `GLM-001` M1.2 never asked the question |

## 2. Validated

| ID | Assumption | Validated | Evidence |
|----|------------|-----------|----------|
| ASM-001 | WS-1 is in Phase 4 (Hardening); Phases 0–3 are complete | 2026-08-10 | Ratified by the Solution Architect — GOV-004 in the [decision register](./DECISION-REGISTER.md#2-governance-decisions) |

## 3. Invalidated

| ID | Assumption | Invalidated | Consequence taken |
|----|------------|-------------|-------------------|
| — | — | — | — |

---

## 4. Using assumptions in triage

An assumption may serve as evidence only at tier **E5** — expert reasoning with a named
mechanism ([16 §4](../16-DECISION_MODEL.md#4-evidence-standard)). Rule EV-1 therefore applies:

> A **MUST** claim resting only on an unvalidated assumption downgrades to **SHOULD** until the
> assumption is validated.

When an assumption is invalidated, [16 §7](../16-DECISION_MODEL.md#7-revalidation-triggers)
requires re-validating **every item and plan that cites it** — which is why `used_by` is
mandatory rather than nice to have.
