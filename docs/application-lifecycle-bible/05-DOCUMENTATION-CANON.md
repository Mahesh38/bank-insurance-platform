# 05 — Documentation Canon

**"What documentation should we ideally have created?"**

Owner: Rajal (business artefacts) · Mahesh (technical artefacts) · Kalpana (register hygiene)

---

## 1. The principle: documents earn their place by being consumed

This repository has ~250 documents and no application CI. That ratio is the warning this canon
exists to prevent repeating.

> **Rule DC-1 — Every canonical document names its consumer and its decision.**
> If you cannot state *who reads this* and *what decision it changes*, it is not a canonical
> artefact — it is a note. Notes are fine. They just do not go in the canon, do not get a gate
> criterion, and do not get maintained.

Three tiers:

| Tier | Meaning | Maintenance obligation |
|---|---|---|
| **Canonical** | Named in a stage gate. Something formally depends on it | Owner named, staleness limit enforced, changes via CR |
| **Working** | Useful, consulted, not gate-bearing | Owner named, best-effort freshness |
| **Archival** | Historical record. Correct as of its date, never updated | Frozen; supersession noted at the top |

---

## 2. The canon, by stage

Legend for **This repo**: 🟢 exists and is good · 🟡 exists, incomplete · 🔴 missing

### S00 — Ideation & Business Case

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| Business case (cost, benefit, payback, options) | Sponsor + Rajal | Canonical | Investment committee — fund or not | 🔴 |
| Project charter | Rajal | Canonical | Programme — mandate and boundaries | 🟢 `00-project-charter.md` |
| Product vision & target outcomes | Rajal | Canonical | Everyone — what success means | 🟢 `02-product-vision-and-outcomes.md` |
| Stakeholder map & RACI | Rajal | Canonical | Delivery — who decides what | 🟡 GAP-010 sponsor unnamed |
| Success metrics / KPI tree | Rajal | Canonical | S15 — what we measure in production | 🟡 GAP-021 |

### S01 — Discovery & Capability Definition

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| Business capability map | Rajal | Canonical | S06 — bounded-context derivation | 🟢 |
| Value stream & journey canvas | Rajal | Canonical | S03, S05 | 🟢 |
| Stakeholder catalogue | Rajal | Canonical | S05 personas, S02 obligations | 🟢 |
| As-is process catalogue | BA | Canonical | S03 — what changes | 🟢 |
| Glossary / ubiquitous language | Rajal + Mahesh | Canonical | Everything downstream | 🟢 |
| Discovery backlog (open questions) | Rajal | Working | Gap closure tracking | 🟢 |
| Gap register | Rajal | Canonical | S04 entry, S11 freeze condition | 🟢 |

### S02 — Regulatory, Risk & Compliance Framing

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| Regulatory registry (IRDAI, RBI, DPDP) | Shailja | Canonical | Every control decision | 🟢 |
| Control catalogue with obligation mapping | Shailja | Canonical | S12 certification evidence | 🟢 |
| Risk taxonomy & risk register | Shailja | Canonical | Prioritisation, waivers | 🟢 |
| **Consent rule pack** | Shailja + Rajal | Canonical | S11 — Consent service AC | 🔴 **GAP-006, P0** |
| **Suitability rule pack** | Shailja + Rajal | Canonical | S11 — Suitability hard-gate AC | 🔴 **GAP-007, P0** |
| Data classification & PII inventory | Shailja + Deepali | Canonical | S07 encryption, S09 residency | 🟡 |
| Retention & deletion schedule | Shailja + Aarti | Canonical | S09 Object Lock, S15 purge jobs | 🟡 |
| Evidence policy | Shailja | Canonical | Every gate | 🟢 |

### S03 — Business Requirements & Process Design

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| BRD with **testable acceptance criteria** | Rajal + BA | Canonical | S11 stories, S12 test basis | 🟡 **GAP-008 — AC missing** |
| Business process models (to-be) | BA | Canonical | S06 orchestration design | 🟢 |
| Business rules catalogue | Rajal | Canonical | S06 invariants | 🟡 GAP-012 quote rules |
| Information model & attribute sheets | BA + Aarti | Canonical | S06 logical model | 🟡 **GAP-016** |
| Requirements traceability matrix | Rajal | Canonical | Regulator: requirement → code → test | 🔴 |

### S04 — Product Definition & Release Slicing

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| PRD per release | Rajal | Canonical | Build scope | 🟢 `PRD-R0-*.md` |
| Release scope with explicit out-of-scope | Rajal | Canonical | Triage scope-fit | 🟢 `R0-SCOPE.md` |
| Product backlog, prioritised | Rajal | Canonical | Sprint planning | 🟢 |
| Definition of Ready / Done | Rajal + Swapnali | Canonical | Story admission and closure | 🟢 |
| Product matrix (LOB × insurer × product) | Rajal | Canonical | Catalogue service | 🔴 GAP-013 |
| Roadmap & release plan | Kalpana | Canonical | Dependency sequencing | 🟢 |

### S05 — Experience Design & Service Blueprint

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| Service blueprint (front + back stage) | Digital + Rajal | Canonical | S06 orchestration | 🔴 |
| Journey wireframes mapped to requirement IDs | Digital | Canonical | S11 UI stories | 🔴 **GAP-009** |
| Design system / component library | Digital | Canonical | Flutter implementation | 🔴 |
| Accessibility standard & conformance target | Digital | Canonical | S12 accessibility testing | 🔴 |
| Content & disclosure copy deck | Rajal + Shailja | Canonical | Regulated disclosure wording | 🔴 |
| Error, empty and degraded-state catalogue | Digital + Rajal | Canonical | S11 failure-path stories | 🔴 |

### S06 — Domain & Information Architecture

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| Bounded context map with relationships | Mahesh | Canonical | Service decomposition | 🟡 19 named, relationships thin |
| Aggregate & state-machine models | Mahesh | Canonical | S11 implementation | 🔴 platform-wide |
| Domain invariants catalogue | Mahesh | Canonical | S12 negative testing | 🔴 |
| Logical data model | Aarti | Canonical | Physical schema | 🟡 |
| Canonical message/event model | Mahesh | Canonical | Contracts | 🟡 1SB only |
| Data ownership matrix | Mahesh + Aarti | Canonical | "Who may write this field" | 🔴 |

### S07 — Solution & Security Architecture

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| Target architecture (C4 or equivalent) | Mahesh | Canonical | Build structure | 🟢 |
| ADR log | Mahesh | Canonical | Why things are as they are | 🟢 |
| API contracts / OpenAPI per service | Mahesh + Amit | Canonical | Consumers, contract tests | 🟡 1SB only |
| **NFR sheet with numbers** | Mahesh + Rajal | Canonical | S12 performance gates | 🔴 **GAP-017** |
| Threat model (STRIDE per trust boundary) | Deepali | Canonical | S08 security tests, S12 pentest scope | 🟡 |
| Trust boundary & network topology | Deepali + Mahesh | Canonical | S09 IaC | 🟡 |
| Crypto & key management standard | Deepali | Canonical | S09 KMS | 🟢 |
| Availability & DR architecture (RTO/RPO) | Mahesh + Shivanshi | Canonical | S14 DR test | 🔴 |

### S08 — Engineering Foundation

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| Engineering standards (code, review, branching) | Amit | Canonical | Every PR | 🟡 in AGENTS.md |
| **CI pipeline definition** | Amit + Shivanshi | Canonical | Every gate needing E4 evidence | 🔴 **absent** |
| Test strategy & pyramid | Swapnali | Canonical | Test design | 🟢 |
| Coverage policy with thresholds | Swapnali | Canonical | Build failure conditions | 🟡 interim floor |
| Test data management & PII-safe fixtures | Swapnali + Shailja | Canonical | All testing | 🔴 |
| Secure coding standard | Deepali + Amit | Canonical | SAST rules | 🟡 |
| Dependency & supply-chain policy (SBOM) | Deepali | Canonical | SCA gate | 🔴 |
| Definition of Done (engineering) | Amit + Swapnali | Canonical | Story closure | 🟢 |

### S09 — Platform & Environment Foundation

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| **IaC repository & module standard** | Shivanshi | Canonical | All infrastructure | 🔴 **absent** |
| Environment topology & promotion model | Shivanshi | Canonical | Release path | 🔴 |
| Landing zone / account & network design | Shivanshi + Deepali | Canonical | Security posture | 🔴 |
| Observability standard (metrics, logs, traces) | Shivanshi | Canonical | S12/S15 evidence | 🟡 lib exists, no stack |
| Secrets management design | Deepali + Shivanshi | Canonical | Runtime credentials | 🟡 TD-006 stub |
| Deployment & rollback runbook | Shivanshi | Canonical | Every release | 🔴 |
| Backup, restore & retention design | Aarti + Shivanshi | Canonical | S14 restore test | 🔴 |
| Data residency attestation | Shivanshi + Shailja | Canonical | Regulatory | 🔴 |

### S10 — Integration & Connectivity

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| Integration catalogue (system, protocol, owner, SLA) | Mahesh | Canonical | Dependency management | 🟡 1SB only |
| Per-integration contract & error taxonomy | Amit | Canonical | Client implementation | 🟢 for 1SB |
| Third-party security assessment | Deepali | Canonical | Partner trust | 🟡 |
| Resilience policy (timeout, retry, breaker) | Shivanshi + Amit | Canonical | S12 failure tests | 🟡 |
| Sandbox & test-account inventory | Kalpana | Working | Test execution | 🟢 |

### S11 — Vertical Slice

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| Slice definition: one journey, named actors and boundaries | Rajal | Canonical | Scope control | 🔴 |
| Story set with AC traced to BRD IDs | Rajal | Canonical | Build + test | 🟡 1SB only |
| E2E test suite for the slice | Swapnali | Canonical | Gate evidence | 🔴 |
| Demo script & UAT plan | Rajal + Swapnali | Canonical | Business acceptance | 🔴 |

### S12 — Hardening & Certification

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| Test execution report & defect summary | Swapnali | Canonical | Quality exit | 🔴 |
| Penetration test report & remediation plan | Deepali | Canonical | Security sign-off | 🔴 |
| Compliance certification pack | Shailja | Canonical | Regulator | 🟡 in progress |
| Performance & load test report | Swapnali + Shivanshi | Canonical | NFR verification | 🔴 |
| Operational runbook | Shivanshi | Canonical | On-call | 🔴 P4 item 4.5 |
| Traceability matrix: requirement → code → test → evidence | Swapnali | Canonical | Audit | 🔴 |

### S13 — Expansion & Scale

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| Expansion pattern / reuse guide | Mahesh | Canonical | Adding LOB N+1 cheaply | 🟡 |
| Per-LOB delta specification | Rajal | Canonical | Scope per expansion | 🟡 |
| Regression suite covering all shipped journeys | Swapnali | Canonical | Protecting what works | 🔴 |
| Capacity model | Shivanshi | Canonical | S14 sizing | 🔴 |

### S14 — Production Readiness & Go-Live

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| Operational Readiness Review record | Shivanshi | Canonical | Launch decision | 🔴 |
| Go-live checklist, signed | Kalpana | Canonical | Launch decision | 🔴 |
| SLI/SLO definition & error budget policy | Shivanshi + Rajal | Canonical | S15 operation | 🔴 |
| Incident response & escalation plan | Shivanshi | Canonical | On-call | 🔴 |
| DR test record (RTO/RPO achieved) | Shivanshi + Aarti | Canonical | Regulatory + launch | 🔴 |
| Rollback & contingency plan | Shivanshi + Kalpana | Canonical | Launch safety | 🔴 |
| Hypercare plan | Kalpana | Canonical | Post-launch weeks | 🔴 |

### S15 — Operate, Evolve & Continuous Assurance

| Artefact | Owner | Tier | Consumer / decision | This repo |
|---|---|---|---|---|
| Service catalogue entry & ownership | Shivanshi | Canonical | Who is called at 3am | 🔴 |
| SLO report & error budget ledger | Shivanshi | Canonical | Feature-vs-reliability decisions | 🔴 |
| Incident postmortems | Shivanshi | Canonical | Learning | 🔴 |
| Tech debt ledger | Amit | Canonical | Debt repayment | 🟢 |
| Periodic control attestation | Shailja | Canonical | Continuing compliance | 🔴 |
| Architecture evolution review | Mahesh | Working | Drift control | 🟢 |

---

## 3. Staleness limits

High-decay artefacts do not merely age — they actively mislead, because people trust them.

| Artefact class | Limit | On breach |
|---|---|---|
| `CURRENT-STATE.yaml`, position banner | 30 days | Agents warn; triage halts past `review_due` |
| Risk register, gap register | 30 days | Delivery escalates |
| Runbooks | 90 days or any topology change | Owner re-validates |
| Threat model | Per release or any trust-boundary change | Security re-runs |
| ADRs | Never — superseded, not updated | Mark `SUPERSEDED BY ADR-nnn` |
| BRD/PRD | Per release | Re-baseline |
| Test strategy | 6 months | QA reviews |

---

## 4. What *not* to write

A canon is as much about restraint as coverage.

- **No document that only restates another.** Link instead. Duplication guarantees divergence.
- **No status document a tool could generate.** If CI knows, ask CI.
- **No design document for a decision not being made.** That is a spike, and its output is an
  ADR, not a document set.
- **No per-service copy of a platform standard.** One standard, referenced.
- **No document without an owner.** Unowned documents rot in place and mislead on the way.

> **Rule DC-2 — Prefer one maintained document to three plausible ones.** The failure mode this
> repository is closest to is not missing documentation. It is documentation whose volume implies
> a delivery maturity that the code does not have.
