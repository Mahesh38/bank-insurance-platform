# S07 — Solution & Security Architecture

**AIGEM stage:** L3 — Technical / Solution Design
**Owner:** Mahesh (Architecture) + Deepali (Security), jointly
**Central question:** *How will it be built, and how will it be safe?*

---

## 1. Purpose

Decide how the domain model becomes a running system, and decide it **with security as a
first-class input rather than a later review**. Security architecture is folded into this stage
deliberately: a threat model produced after the architecture is settled can only object, not
shape.

## 2. Entry criteria

- [ ] GATE-S06 passed: contexts, aggregates, invariants, data model
- [ ] GATE-S02 passed: controls and data classification

## 3. Epics and stories

### S07-E01 — Service architecture · *Mahesh*

| ID | Story | Acceptance criteria |
|---|---|---|
| S07-E01-S01 | Map contexts to deployable services | A context may be a service, part of one, or several — with the reason stated |
| S07-E01-S02 | Define service responsibilities and boundaries | What each owns, what it must never do |
| S07-E01-S03 | Define the internal architecture pattern | Ports and adapters, layering rules, dependency direction — enforceable by ArchUnit |
| S07-E01-S04 | Define the edge architecture | BFF per client type; what the edge may and may not do |
| S07-E01-S05 | Define the build order | Which services R0 actually needs — **not all nineteen** |

### S07-E02 — Communication architecture · *Mahesh*

| ID | Story | Acceptance criteria |
|---|---|---|
| S07-E02-S01 | Decide synchronous vs asynchronous per interaction | Each choice justified by consistency and latency needs, not preference |
| S07-E02-S02 | Define API standards | REST conventions, versioning, pagination, error format, idempotency semantics |
| S07-E02-S03 | Define the event backbone, if any | Whether one is needed for R0; if not, when it becomes needed and what triggers it |
| S07-E02-S04 | Define resilience patterns | Timeouts, retries with backoff, circuit breakers, bulkheads — per dependency class |
| S07-E02-S05 | Define idempotency across mutating APIs | Key derivation, storage, retention, and behaviour on replay |

### S07-E03 — Security architecture · *Deepali*

| ID | Story | Acceptance criteria |
|---|---|---|
| S07-E03-S01 | **Threat model per trust boundary** | STRIDE per boundary; each threat has a mitigation or an accepted risk with an owner |
| S07-E03-S02 | Define the trust boundary and network model | Public, DMZ, internal, data zones; what may cross each boundary |
| S07-E03-S03 | Define the identity architecture | Workforce, customer and workload identity; token handling; the BFF token-hiding pattern |
| S07-E03-S04 | Define the authorization model | RBAC + ABAC, default deny, PDP placement, policy evaluation and caching |
| S07-E03-S05 | Define cryptography and key management | Algorithms, key hierarchy, KMS CMK ownership, rotation, and crypto agility |
| S07-E03-S06 | Define secrets management | Storage, retrieval, rotation, revocation; no secret in code or image |
| S07-E03-S07 | Define API and webhook security | Authentication, replay protection, payload integrity, partner trust contract |
| S07-E03-S08 | Define security logging and detection | What security events are emitted, where they go, what detects abuse |

### S07-E04 — Data architecture · *Aarti + Mahesh*

| ID | Story | Acceptance criteria |
|---|---|---|
| S07-E04-S01 | Select a datastore per context | Justified by access pattern; database-per-service boundary respected |
| S07-E04-S02 | Design the physical schema | Keys, constraints, indexes, partitioning where justified |
| S07-E04-S03 | Define the migration strategy | Tooling, ownership, rollback, and zero-downtime approach |
| S07-E04-S04 | Design encryption and access at the data layer | At-rest encryption, key ownership, PII field handling, DB access model |
| S07-E04-S05 | Design backup, PITR and recovery | RPO and RTO per store, with the restore procedure defined |
| S07-E04-S06 | Design retention and purge | Per data class, with immutability where regulation requires |

### S07-E05 — Non-functional requirements · *Mahesh + Rajal + Shivanshi*

| ID | Story | Acceptance criteria |
|---|---|---|
| S07-E05-S01 | **Set NFR numbers** | Latency (p50/p95/p99) per journey step, throughput at peak, availability target, RTO/RPO — actual numbers (**closes GAP-017**) |
| S07-E05-S02 | Derive NFRs from business need | Each number traces to a business reason, not a convention |
| S07-E05-S03 | Define how each NFR is verified | The test that proves it, at which stage |
| S07-E05-S04 | Define capacity assumptions | RM count, journeys per day, Q4 tax-season peak multiplier |

### S07-E06 — Architecture decisions · *Mahesh*

| ID | Story | Acceptance criteria |
|---|---|---|
| S07-E06-S01 | Record an ADR per significant decision | Context, options, decision, consequences; superseded rather than edited |
| S07-E06-S02 | Define architecture principles and standing constraints | The rules that make later triage fast |
| S07-E06-S03 | Define fitness functions | Which constraints are machine-enforceable, and how |
| S07-E06-S04 | Define the replaceability strategy | What must remain swappable — aggregator, IdP, payment gateway — and how that is proven |

## 4. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S07-VT-01 | Services align to contexts | Compare the service list to the context map | Every deviation justified in an ADR |
| S07-VT-02 | Threat model covers every boundary | Traverse boundaries against the model | 100% covered; every threat mitigated or accepted with an owner |
| S07-VT-03 | Authorization is default-deny | Design review of the PDP | No path reaches a resource without an explicit allow |
| S07-VT-04 | Failure modes are designed | Walk each dependency failure | Every one has a defined behaviour and a user-visible outcome |
| S07-VT-05 | NFRs are numeric and verifiable | Traverse the NFR sheet | Zero qualitative NFRs; every one has a named verification test |
| S07-VT-06 | Data ownership holds in the physical design | Check for cross-service database access | None |
| S07-VT-07 | Constraints are machine-enforceable | Count fitness functions vs constraints | Every automatable constraint has one |
| S07-VT-08 | Replaceability is real | Design walkthrough: replace 1SB with another aggregator | Change is confined to the adapter layer |

## 5. Exit gate — GATE-S07

| # | Criterion | Evidence level | Evidence artefact |
|---|---|---|---|
| S07-G1 | Target architecture documented and reviewed | E2 | Architecture document + review verdict |
| S07-G2 | ADRs recorded for all significant decisions | E1 | ADR log |
| S07-G3 | **Threat model complete per trust boundary** | E2 | Threat model, signed by Security (**human**) |
| S07-G4 | Security architecture approved | E2 | Security sign-off (**human**) |
| S07-G5 | Data architecture approved including backup and retention | E2 | Signed by Aarti |
| S07-G6 | **NFR sheet with numbers, each verifiable** | E2 | Signed NFR sheet — **closes GAP-017** |
| S07-G7 | R0 build order defined — the minimum service set | E2 | Build order, PO and Delivery agreed |
| S07-G8 | Fitness functions defined for automatable constraints | E1 | Fitness function list |

**Approvers:** Mahesh (AP) · Deepali (AP, B, **human**) · Aarti (AP) · Shivanshi (AP) ·
Shailja (AP, B) · Rajal (RV) · Swapnali (RV)

## 6. Current position in this repository — 🟢 Strong, with two real gaps

**Present and good:** the eight-part architecture review is a genuinely strong artefact set —
target microservices, communication patterns, AWS infrastructure, data architecture, security and
NFRs, delivery roadmap, and an architecture decision log. The 1SB service architecture, the
replaceable-middleware rationale, and the authentication/authorization SSOT are all well argued.
Ports and adapters with ArchUnit enforcement means S07-E01-S03 and S07-E06-S03 are not just
designed but enforced — a real strength.

**Open:**

| Item | Detail |
|---|---|
| **GAP-017** | **NFR numbers missing.** No latency, throughput, availability, RTO or RPO targets. S07-G6 fails, and consequently S12's performance gate has no threshold to test against |
| Threat model | Security architecture is strong at the standards level; a per-trust-boundary STRIDE model is not evidenced |
| S07-E01-S05 | No R0 build order. The 19 contexts read as a target state with no minimum viable subset named — which is why "16 services missing" feels like the scope |
| Backup/DR design | RTO/RPO not set; recovery procedure not designed |

**Note on GAP-017's downstream cost.** WS-1 Phase 4 criterion 4.6 requires "p95 quote under
nominal concurrency". Neither "p95 target" nor "nominal concurrency" has a number. The criterion
cannot pass because it cannot be evaluated, not because the work is hard. That is an S07 gap
surfacing as an S12 blocker, four stages later.

## 7. Premature at this stage

Production capacity planning · autoscaling policy · multi-region topology · cost optimisation ·
implementation.

S07 sets targets and structure. S09 builds the platform, S14 sizes it for launch. Designing
autoscaling before a single service runs is designing against imagined load.
