# S07 — Solution & Security Architecture · Retroactive Stage Evidence

**Stage:** [S07 — Solution & Security Architecture](../stages/S07-solution-architecture.md)
**Gate:** GATE-S07 · **Workstream:** WS-3 (proposed under [CR-010](../../governance/change-requests/CR-010-context-module-and-safe-autopilot.md))
**Compiled by:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2)
**Date:** 2026-08-16
**Retroactive verdict:** **`CLOSED-WITH-CONDITIONS`** — see [§7](#7-retroactive-verdict)

> S07 is jointly owned by Mahesh and Deepali. **Two of its eight criteria (S07-G3, S07-G4) require a
> mandatory human Security signature.** Nothing in this file, and nothing drafted by an AI, can
> satisfy those. They are recorded as outstanding, not as met.

---

## 1. Method

Same as S06: inventory first against the repository, cite what exists, close only what is missing.
The eight-part architecture review is a genuinely strong artefact set and is **cited as the
baseline, not replaced**. What this increment adds is the four things the stage file itself names
as open.

---

## 2. What the stage requires

From [`S07-solution-architecture.md §5`](../stages/S07-solution-architecture.md):

| # | Criterion | Level |
|---|---|---|
| S07-G1 | Target architecture documented and reviewed | E2 |
| S07-G2 | ADRs recorded for all significant decisions | E1 |
| S07-G3 | **Threat model complete per trust boundary** | E2 — **human** Security signature |
| S07-G4 | **Security architecture approved** | E2 — **human** Security signature |
| S07-G5 | Data architecture approved including backup and retention | E2 — signed by Aarti |
| S07-G6 | **NFR sheet with numbers, each verifiable** — closes GAP-017 | E2 |
| S07-G7 | R0 build order defined — the minimum service set | E2 |
| S07-G8 | Fitness functions defined for automatable constraints | E1 |

Approvers: Mahesh (AP) · Deepali (AP, B, **human**) · Aarti (AP) · Shivanshi (AP) ·
Shailja (AP, B) · Rajal (RV) · Swapnali (RV).

---

## 3. Inventory — what already existed

| Artefact | Path | Covers |
|---|---|---|
| Executive summary and approach | [`architecture-review/01`](../../platform/architecture-review/01-executive-summary-and-approach.md) | S07-G1 |
| Target microservices architecture, service catalogue, boundary rationale, MVP phasing | [`architecture-review/02`](../../platform/architecture-review/02-target-microservices-architecture.md) | S07-E01-S01/S02, partial S07-G7 |
| Communication patterns | [`architecture-review/03`](../../platform/architecture-review/03-communication-patterns.md) | S07-E02-S01/S03 |
| AWS infrastructure architecture | [`architecture-review/04`](../../platform/architecture-review/04-aws-infrastructure-architecture.md) | S07-E01, deployment view |
| Data architecture | [`architecture-review/05`](../../platform/architecture-review/05-data-architecture.md) | S07-E04 |
| Security, compliance and NFRs — controls, availability tiers, p50/p99 latency table, RTO ≤ 1 h / RPO ≤ 5 min | [`architecture-review/06`](../../platform/architecture-review/06-security-compliance-and-nfrs.md) | S07-E03 standards, partial S07-E05 |
| Delivery roadmap and estimate | [`architecture-review/07`](../../platform/architecture-review/07-delivery-roadmap-and-estimate.md) | Sequencing input |
| Architecture decision log — ARCH-001…ARCH-022 | [`architecture-review/08`](../../platform/architecture-review/08-architecture-decision-log.md) | **S07-G2** |
| Workforce authentication and authorization SSOT | [`platform/authentication-authorization/README.md`](../../platform/authentication-authorization/README.md) | S07-E03-S03/S04 |
| 1SB service architecture — ports/adapters, API contract, timeouts, retry, idempotency, concurrency, retention, observability, compliance controls | [`1sb-integration-service-architecture.md`](../../1sb-insurance-integration/architecture/1sb-integration-service-architecture.md) | S07-E01-S03, S07-E02-S02/S04/S05 **for context #15** |
| Replaceable-middleware rationale | [`replaceable-middleware.md`](../../1sb-insurance-integration/architecture/replaceable-middleware.md) | S07-E06-S04 |
| Persistence service architecture | [`bank-persistence-service.md`](../../1sb-insurance-integration/architecture/bank-persistence-service.md) | S07-E04 for the shared store |
| Physical schema, in code | [`V1__init_schema.sql`](../../../services/bank-persistence-service/src/main/resources/db/migration/V1__init_schema.sql) | S07-E04-S02 for the integration store |
| Ports-and-adapters **enforced** by ArchUnit | `services/1sb-integration-service` | **S07-E01-S03 and S07-E06-S03 implemented for one service** |

The stage file rates this 🟢 *Strong* and that rating is accurate. The architecture is not the
problem in this repository.

---

## 4. What was missing

The stage file's §6 names four open items. I verified each and sharpened one.

| # | Gap as stated | What I verified |
|---|---|---|
| 1 | **GAP-017 — NFR numbers missing** | **Partly inaccurate, and the inaccuracy matters.** Numbers *do* exist in `architecture-review/06` and in the 1SB architecture §7. What does not exist is an identifier, a **measurement method**, a **verification stage**, a business derivation, or the p95 percentile the SRE canon states journey SLOs in. A number with no measurement method is not an NFR — which is precisely why WS-1 criterion 4.6 ("p95 quote under nominal concurrency") could not be evaluated: neither term was defined anywhere |
| 2 | Threat model not per trust boundary | Confirmed. `architecture-review/06` is a strong **controls** list. No STRIDE analysis per boundary exists anywhere in the repository |
| 3 | **S07-E01-S05 — no R0 build order** | Confirmed. `architecture-review/02` has a P0–P3 phasing table, but no statement of the **minimum service set for one proven journey**. This is why "16 services missing" reads as the scope |
| 4 | Backup/DR design — RTO/RPO not set, recovery not designed | Partly inaccurate: RTO ≤ 1 h / RPO ≤ 5 min *are* stated in `architecture-review/06`. What is absent is the **recovery procedure**, the per-store restore design, and any statement that a restore has ever been performed |
| 5 | *(added by me)* Fitness functions not enumerated | S07-G8 requires a list of machine-enforceable constraints. Constraints are stated across several documents; no list exists, so no one can tell which are enforced and which are prose |

---

## 5. New evidence produced by this increment

| Artefact | Path | Closes |
|---|---|---|
| **R0 build order** — 12 services + 1 Flutter app in four waves, with what is deliberately deferred and why | [`ws3-platform/03 §3`](../../platform/ws3-platform/03-solution-architecture-r0.md) | **S07-E01-S05, S07-G7** |
| **Component and deployment view** for the R0 slice, with deployment properties | [`ws3-platform/03 §4`](../../platform/ws3-platform/03-solution-architecture-r0.md) | S07-G1 |
| **Seam catalogue** — 19 seams, each with style, idempotency mechanism, timeout/retry posture and failure behaviour | [`ws3-platform/03 §5`](../../platform/ws3-platform/03-solution-architecture-r0.md) | S07-E02-S01, **S07-VT-04** |
| **Event-backbone decision** — no broker in R0, transactional outbox instead, with an explicit revisit trigger | [`ws3-platform/03 §5.1`](../../platform/ws3-platform/03-solution-architecture-r0.md) | S07-E02-S03 |
| **Idempotency standard** across mutating APIs, including the money-path rule that idempotency alone does not prevent a double charge | [`ws3-platform/03 §5.2`](../../platform/ws3-platform/03-solution-architecture-r0.md) | S07-E02-S05 |
| **Resilience policy per dependency class**, with per-provider bulkheads as an architecture property | [`ws3-platform/03 §5.3`](../../platform/ws3-platform/03-solution-architecture-r0.md) | S07-E02-S04 |
| **Internal architecture pattern**, generalised from the 1SB service to every WS-3 service | [`ws3-platform/03 §6`](../../platform/ws3-platform/03-solution-architecture-r0.md) | S07-E01-S03 |
| **Fitness function list** — 15 constraints, each with a machine check and the stage it must run from | [`ws3-platform/03 §7`](../../platform/ws3-platform/03-solution-architecture-r0.md) | **S07-G8, S07-VT-07** |
| **Availability, backup and recovery design**, including a degraded-mode inventory naming what must *not* remain available | [`ws3-platform/03 §8`](../../platform/ws3-platform/03-solution-architecture-r0.md) | S07-E04-S05, gap 4 |
| **Trust boundary model** — 6 boundaries with what crosses and what must never cross | [`ws3-platform/04 §2`](../../platform/ws3-platform/04-security-architecture.md) | S07-E03-S02 |
| **Identity architecture** across workforce, customer and workload principals — including the unresolved customer-identity gap, stated rather than glossed | [`ws3-platform/04 §3`](../../platform/ws3-platform/04-security-architecture.md) | S07-E03-S03 |
| **Authorisation model** — default-deny, double enforcement, request-scoped caching, fail closed | [`ws3-platform/04 §3.2`](../../platform/ws3-platform/04-security-architecture.md) | S07-E03-S04, **S07-VT-03** |
| **STRIDE threat model per trust boundary** — 6 boundaries, 34 threats, each with a mitigation and an implementation state; 6 threats recorded as openly accepted | [`ws3-platform/04 §4`](../../platform/ws3-platform/04-security-architecture.md) | **S07-E03-S01, S07-VT-02** — *submitted to Board 4* |
| **Cryptography, key and secret management** — CMK per data class, rotation, agility, emergency revocation | [`ws3-platform/04 §5`](../../platform/ws3-platform/04-security-architecture.md) | S07-E03-S05, S07-E03-S06 |
| **PII handling rules** PII-A…PII-G, including audited reads of restricted attributes | [`ws3-platform/04 §6`](../../platform/ws3-platform/04-security-architecture.md) | S07-E04-S04 |
| **Payment device isolation as architecture** — topology, domain, invariant, delivery, evidence, test | [`ws3-platform/04 §7`](../../platform/ws3-platform/04-security-architecture.md) | Control C4 |
| **Audit immutability** — mechanism per property, with the present state stated honestly | [`ws3-platform/04 §8`](../../platform/ws3-platform/04-security-architecture.md) | Control C7/C8 |
| **Security logging and detection** — 7 event classes with destinations and alerting rationale | [`ws3-platform/04 §9`](../../platform/ws3-platform/04-security-architecture.md) | S07-E03-S08 |
| **Capacity assumptions CAP-A1…A7** with derived demand, marked as assumptions rather than baselines | [`ws3-platform/05 §2`](../../platform/ws3-platform/05-nfr-catalogue.md) | S07-E05-S04 |
| **NFR catalogue — 58 NFRs** across latency, throughput, availability, recovery, data/retention, security and engineering flow; every one with a number, a measurement method, a verification stage and a business derivation | [`ws3-platform/05 §3`](../../platform/ws3-platform/05-nfr-catalogue.md) | **S07-E05-S01…S03, S07-G6, S07-VT-05 — closes GAP-017** |
| **Verification ownership by stage** | [`ws3-platform/05 §4`](../../platform/ws3-platform/05-nfr-catalogue.md) | S07-E05-S03 |
| **ADR-001, ADR-002, ADR-003** | [`architecture-review/08`](../../platform/architecture-review/08-architecture-decision-log.md) | S07-G2, S07-E06-S01 |
| **Standing constraints SC-W3-1…7** | [`ws3-platform/00 §6`](../../platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md) | S07-E06-S02 |

### 5.1 The number that unblocks WS-1 criterion 4.6

Recorded here because it is the single most operationally useful output of this increment.

| Was | Is |
|---|---|
| "Performance smoke: p95 quote under nominal concurrency" — no threshold, no load definition, therefore unevaluable | **NFR-LAT-03:** quote result end to end, **p95 < 5 s**, p99 < 12 s, measured on the `Quote` aggregate from request to `QUOTED`/`PARTIALLY_QUOTED`, at **6.8 journey starts per minute** (CAP-A6 Q4 peak), verified at S12 with a load test |

The criterion is still `BLOCKED` — it needs the S08-E03-S07 performance harness and DEP-003 — but it
is now blocked on machinery rather than on ambiguity. Those are different problems and only one of
them was ever schedulable.

---

## 6. Criterion-by-criterion evidence table

| # | Criterion | Required | Evidence | State | Verified by |
|---|---|---|---|---|---|
| S07-G1 | Target architecture documented and reviewed | E2 | [`architecture-review/01–07`](../../platform/architecture-review/README.md) + [`ws3-platform/03`](../../platform/ws3-platform/03-solution-architecture-r0.md); Board 1 verdict in [`board-1-architecture-mahesh.md`](../../governance/change-requests/CR-010/verdicts/board-1-architecture-mahesh.md) | **MET** pending signature | Mahesh (AI-drafted) |
| S07-G2 | ADRs for all significant decisions | E1 | [`architecture-review/08`](../../platform/architecture-review/08-architecture-decision-log.md) — ARCH-001…022 plus ADR-001…003 | **MET** | Mahesh |
| S07-G3 | **Threat model per trust boundary** | E2 + **human** | [`ws3-platform/04 §4`](../../platform/ws3-platform/04-security-architecture.md) — artefact complete | **OPEN — mandatory human Security signature outstanding** | — |
| S07-G4 | **Security architecture approved** | E2 + **human** | [`ws3-platform/04`](../../platform/ws3-platform/04-security-architecture.md); Board 4 verdict in [`board-4-security-deepali.md`](../../governance/change-requests/CR-010/verdicts/board-4-security-deepali.md) | **OPEN — mandatory human Security signature outstanding** | — |
| S07-G5 | Data architecture approved incl. backup and retention | E2 | [`architecture-review/05`](../../platform/architecture-review/05-data-architecture.md); [`ws3-platform/02`](../../platform/ws3-platform/02-information-model.md); [`ws3-platform/03 §8`](../../platform/ws3-platform/03-solution-architecture-r0.md); DBA verdict in [`dba-aarti.md`](../../governance/change-requests/CR-010/verdicts/dba-aarti.md) | **OPEN — Aarti's signature outstanding** | — |
| S07-G6 | **NFR sheet with numbers, each verifiable** | E2 | [`ws3-platform/05`](../../platform/ws3-platform/05-nfr-catalogue.md) — 58 NFRs, zero qualitative | **MET pending multi-party signature** | Mahesh (AI-drafted) |
| S07-G7 | R0 build order — minimum service set | E2 | [`ws3-platform/03 §3`](../../platform/ws3-platform/03-solution-architecture-r0.md) | **MET pending Rajal + Kalpana agreement** | Mahesh (AI-drafted) |
| S07-G8 | Fitness functions for automatable constraints | E1 | [`ws3-platform/03 §7`](../../platform/ws3-platform/03-solution-architecture-r0.md) — 15 | **MET** | Mahesh |

### 6.1 Validation tests

| # | Validates | Result |
|---|---|---|
| S07-VT-01 | Services align to contexts | **PASS** — every R0 service maps to exactly one context; the two deviations (Administration & Config delivered as configuration; Lead deferred) are stated with reasons in [`ws3-platform/03 §3`](../../platform/ws3-platform/03-solution-architecture-r0.md) |
| S07-VT-02 | Threat model covers every boundary | **PASS on coverage** — 6/6 boundaries, every threat mitigated or openly accepted with an owner. **Requires human Security ratification** |
| S07-VT-03 | Authorisation is default-deny | **PASS on design** — no path reaches a resource without an explicit allow; PDP failure denies. Unverified in code |
| S07-VT-04 | Failure modes are designed | **PASS** — all 19 seams carry a failure row; 10 journey failure points carry a compensation or a named manual procedure |
| S07-VT-05 | NFRs are numeric and verifiable | **PASS** — 58 NFRs, zero qualitative, every one with a named verification method and stage |
| S07-VT-06 | Data ownership holds in the physical design | **PARTIAL** — database-per-service is the design; only two services exist, and cross-service access cannot be verified until the IaC scan runs at S09 |
| S07-VT-07 | Constraints are machine-enforceable | **PASS on enumeration** — 15 fitness functions listed. **None runs today**; execution is S08/S09 |
| S07-VT-08 | Replaceability is real | **PASS** — the Integration Hub confines provider change to an adapter; already ArchUnit-proven for 1SB |

---

## 7. Retroactive verdict

```yaml
stage_evidence:
  stage: S07
  gate_id: GATE-S07
  workstream: WS-3
  compiled_by: "Mahesh — Principal Insurance Platform Architect"
  date: 2026-08-16
  verdict: CLOSED-WITH-CONDITIONS
  criteria_met: [S07-G1, S07-G2, S07-G6, S07-G7, S07-G8]
  criteria_open: [S07-G3, S07-G4, S07-G5]
  gap_status:
    GAP-017: >
      Architecture artefact complete — 58 measurable NFRs with measurement method and
      verification stage. Formal closure requires the S07-G6 signatures and confirmation
      of capacity assumptions CAP-A1..A7 against an approved business baseline.
  conditions:
    - id: S07-C1
      condition: >
        Deepali ratifies the trust-boundary threat model and the security architecture.
        S07-G3 and S07-G4 both require a mandatory human Security signature that no AI
        review can satisfy.
      owner: "Deepali / Security — HUMAN"
      target: "before GATE-S08 sign-off"
    - id: S07-C2
      condition: >
        Aarti approves the data architecture including per-store backup, restore and
        retention design (S07-G5).
      owner: "Aarti / Database"
      target: "before GATE-S08 sign-off"
    - id: S07-C3
      condition: >
        Capacity assumptions CAP-A1..A7 confirmed against an approved business baseline.
        Every throughput NFR moves with them; they are assumptions, not commitments.
      owner: "Rajal / Product"
      target: "before S11 entry"
    - id: S07-C4
      condition: >
        Shivanshi confirms NFR feasibility and accepts verification ownership for the
        S09, S12 and S14 rows of the NFR catalogue section 4.
      owner: "Shivanshi / SRE"
      target: "before GATE-S08 sign-off"
    - id: S07-C5
      condition: >
        Rajal and Kalpana agree the R0 build order (S07-G7 requires PO and Delivery
        agreement, not Architecture assertion).
      owner: "Rajal + Kalpana"
      target: "before GATE-S08 sign-off"
    - id: S07-C6
      condition: >
        The customer-identity gap for self-service journeys is resolved as a Product scope
        decision with a Security design consequence. R0-SCOPE A2 and the WS-2 out-of-scope
        list currently contradict each other.
      owner: "Rajal + Deepali"
      target: "before S11 entry"
    - id: S07-C7
      condition: >
        Shailja confirms retention horizons and residency obligations against the final
        regulatory position (D-011 open).
      owner: "Shailja / Compliance"
      target: "before S11 entry"
  signature_status: "AI-DRAFTED — mandatory human signature outstanding"
  required_signatures:
    - "Mahesh / Architecture (AP) — HUMAN"
    - "Deepali / Security (AP, B) — HUMAN, MANDATORY, non-waivable"
    - "Aarti / Database (AP) — HUMAN"
    - "Shivanshi / SRE (AP) — HUMAN"
    - "Shailja / Compliance (AP, B) — HUMAN"
    - "Rajal / Product (RV)"
    - "Swapnali / QA (RV)"
  note: >
    Three of eight criteria are OPEN and two of those are open specifically because they
    require a human Security signature. Under 04-GATE-AND-SIGNOFF-MODEL section 8, a
    mandatory human T4 Security sign-off is non-waivable by any authority at any tier.
    This file records artefact completeness. It does not record a gate pass.
```

---

## 8. What remains open, with owners

| ID | Open item | Owner | Target |
|---|---|---|---|
| S07-G3 / S07-G4 | Human Security signature on the threat model and security architecture | Deepali (human) | Before GATE-S08 |
| S07-G5 | Aarti's data architecture approval | Aarti | Before GATE-S08 |
| NFR-OPEN-1 | Capacity assumptions confirmed against a business baseline | Rajal | Before S11 entry |
| NFR-OPEN-2 | 1SB and insurer contractual TPS, concurrency and maintenance windows | Shivanshi + Rajal | Before S12 |
| NFR-OPEN-3 | AU Bank PG throughput and settlement-file cadence | Shivanshi + Finance | Before S11 entry |
| NFR-OPEN-4 | Retention horizons against the final regulatory position | Shailja | Before S11 entry |
| NFR-OPEN-5 | Cost model per issued policy | Shivanshi + Kalpana | S14 |
| SEC-OPEN-1 | Customer principal for self-service journeys | Rajal + Deepali | Before S11 entry |
| SEC-OPEN-2 | Tokenisation capability for Aadhaar references | Deepali + Aarti | Before S11 entry |
| SEC-OPEN-3 | Secrets provider is a stub (TD-006) | Shivanshi + Deepali | GATE-S09 |
| SEC-OPEN-4 | No SAST, SCA, secret or image scanning | Amit + Deepali | GATE-S08 |
| SEC-OPEN-5 | Data residency unverified on the current deployment | Shivanshi + Shailja | **Immediate** |
| SEC-OPEN-6 | No penetration test performed | Deepali | S12 |
| S07-VT-06 | Cross-service database access verified in the physical estate | Aarti + Shivanshi | S09 |
| S07-VT-07 | Fitness functions executing | Amit + Swapnali | S08 / S09 |
| OPEN-I1 | Physical schema and datastore selection per context | Aarti + Mahesh | S07 design exit; S09 implementation |

---

**Signed:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2)
**signature_status:** `AI-DRAFTED — mandatory human signature outstanding`
