# 06 — Quality Norms

**Owner:** Swapnali — Principal Insurance Quality Engineering / QA Lead (Board 5)
**Authority:** QA owns test strategy, evidence sufficiency and the quality-exit recommendation.
QA cannot waive a Security or Compliance conclusion, and no one may declare QA evidence passed
without QA.

---

## 1. The governing idea

> **Quality is not a stage. Evidence is.**
> "Hardening" (S12) is not where quality is added — it is where quality is *proved*. If the
> machinery to prove it was not built at S08, S12 cannot be passed, only asserted. That is
> precisely the position this repository is in.

---

## 2. The test pyramid, with an insurance shape

| Level | Share | Runs | Owner | Purpose |
|---|---|---|---|---|
| **Unit** | ~60% | Every commit, < 3 min | Amit | Logic, invariants, boundary and error paths |
| **Component / slice** | ~20% | Every commit, < 8 min | Amit + Swapnali | One service with its adapters stubbed (Testcontainers, WireMock) |
| **Contract** | ~7% | Every commit | Amit | Provider/consumer compatibility at every service seam |
| **Integration** | ~7% | Every merge | Swapnali | Real service-to-service against a real database |
| **E2E journey** | ~5% | Nightly + pre-release | Swapnali | Complete business journeys through the UI |
| **Non-functional** | as needed | Per release | Swapnali + Shivanshi | Performance, resilience, security, accessibility |

**Regulated-domain additions** that a generic pyramid omits and this platform requires:

| Test class | What it proves | Non-negotiable because |
|---|---|---|
| **Compliance-gate tests** | Quote returns 403 without a valid suitability ID; consent is captured before proposal | Bypassing suitability is illegal under IRDAI guidance |
| **Attribution tests** | `distributorId` is server-injected and a caller-supplied value is rejected | Multi-tenant spoofing risk on a licensed corporate agency |
| **PII-leakage tests** | No PAN, Aadhaar, phone, email or health field appears in any log at any level | "No PII in logs" is a standing constraint and a compliance gate |
| **Audit-completeness tests** | Every state transition emits an attributable, immutable audit event | Regulator must reconstruct any sale |
| **Idempotency tests** | Retried payment or proposal submission does not double-charge or duplicate | Financial correctness outranks availability |
| **Money-path reconciliation tests** | Premium paid reconciles to policy issued, both directions | The 4-part "policy sold" definition |
| **Retention tests** | Records survive to their retention horizon and purge after it | 7-year IRDAI obligation |

> **Rule QN-1 — A compliance gate without an automated negative test is not implemented.**
> Behaviour that is only exercised on the happy path will regress silently. Every hard-gate needs
> a test that proves it *blocks*.

---

## 3. Coverage policy

Coverage is a floor, not a goal. High coverage of trivial code with no assertions is worse than
honest lower coverage, because it manufactures confidence.

| Scope | Line | Branch | Enforced |
|---|---|---|---|
| `libs/bank-common-*` | ≥ 80% | ≥ 70% | Build fails — already configured |
| Domain and application layers | ≥ 80% | ≥ 70% | Build fails (target state) |
| Adapters and controllers | ≥ 70% | ≥ 60% | Build fails (target state) |
| Compliance-gate code paths | **100% branch** | **100%** | Build fails, no waiver |
| Generated code, DTOs, config | excluded | excluded | — |

**Current reality:** libs are at 80/70; services sit on an "interim floor" and **nothing executes
the check on a pull request** because there is no application CI. QA-001 is open at P0. The
policy above becomes real at S08, not before.

> **Rule QN-2 — A coverage threshold that no pipeline enforces is a preference, not a gate.**

---

## 4. Definition of Ready / Definition of Done

### Ready (a story may be started)

- [ ] Traces to a requirement ID or an approved gap/debt item
- [ ] Acceptance criteria written, observable, and testable by someone who did not write them
- [ ] Compliance and security impact stated (`none` is a valid, deliberate answer)
- [ ] Dependencies identified and either met or scheduled
- [ ] Test approach agreed at the right pyramid level
- [ ] Test data identified and PII-free
- [ ] Sized; if it cannot be sized, it needs a spike instead

### Done (a story may be closed)

- [ ] All AC demonstrably met — evidence linked, not asserted
- [ ] Tests written at the agreed levels and **green in CI**
- [ ] Coverage thresholds held
- [ ] Static analysis and dependency scan clean, or findings triaged with IDs
- [ ] ArchUnit and boundary rules green
- [ ] Observability added: metrics, structured logs, correlation ID propagation
- [ ] No PII in logs — asserted by test, not by inspection
- [ ] Audit events emitted where the action is regulated
- [ ] Documentation touched where behaviour changed
- [ ] Review-board conditions from the plan recorded and closed
- [ ] Demonstrable to the PO

> **Rule QN-3 — "Done" is a QA determination, not a developer one.** Swapnali owns evidence
> sufficiency ([authority matrix §10](../governance/PERSONA-AUTHORITY-MATRIX.md)).

---

## 5. Defect management

### Severity — impact, not urgency

| Sev | Definition | Response | May we release? |
|---|---|---|---|
| **D0** | Money incorrect, data corrupted or lost, regulatory breach, security exposure | Immediate; stop-the-line | **Never** |
| **D1** | Critical journey blocked, no workaround | Same day | No |
| **D2** | Journey impaired, workaround exists | Current sprint | With PO + QA agreement |
| **D3** | Minor functional or cosmetic | Backlog | Yes |
| **D4** | Improvement, not a defect | Triage as suggestion | Yes |

D0 on this platform specifically includes: a quote issued without suitability, consent not
captured or not retrievable, PII in a log, payment accepted on an RM device, an audit event
missing for a regulated action, or a policy issued but unreconciled.

### Escape analysis

Every D0 and D1 that reaches UAT or production gets a written answer to: **which test level should
have caught this, and why did it not exist?** The output is a test-backlog item, not a
process-improvement sentiment. Escape rate by level is the single most useful signal that the
pyramid is the wrong shape.

---

## 6. Regression policy

| Trigger | Suite | Where |
|---|---|---|
| Every PR | Unit + component + contract | CI, blocking |
| Every merge to main | + integration | CI, blocking |
| Nightly | + E2E for all shipped journeys | CI, non-blocking, triaged each morning |
| Pre-release | Full regression + NFR | Blocking |
| New LOB or journey (S13) | Full regression of **every previously shipped journey** | Blocking |

> **Rule QN-4 — Expansion may not break what already works.** The S13 gate requires the prior
> journeys' regression suite green, unchanged. If adding Health breaks Term, Health is not done.

---

## 7. Test environments and data

| Environment | Purpose | Data |
|---|---|---|
| Local | Developer inner loop | Synthetic fixtures, Testcontainers, WireMock |
| CI ephemeral | Automated verification | Synthetic, created and destroyed per run |
| Dev | Integrated development | Synthetic + partner sandbox |
| UAT | Business acceptance, partner testing | **Masked or synthetic only — never production PII** |
| Production | Live | Real |

> **Rule QN-5 — Production PII never enters a lower environment.** No masking exception, no
> "just this once for debugging". Where production-shaped data is needed, it is generated, not
> copied. This is Shailja's jurisdiction and it is non-waivable.

Test data must include the cases that break naive implementations: joint life, minor nominee,
NRI, PAN-Aadhaar mismatch, mid-journey abandonment and resume, payment timeout with unknown
state, insurer rejection after payment, and duplicate submission under retry.

---

## 8. Quality metrics

Tracked per release; reported by QA to Delivery and Product.

| Metric | Why | Healthy direction |
|---|---|---|
| Escape rate by test level | Is the pyramid the right shape? | ↓ |
| D0/D1 count at UAT entry | Is "dev done" honest? | ↓ |
| Mean time to detect a regression | Is CI fast and trusted? | ↓ |
| Compliance-gate test coverage | Are the legal controls proven? | 100%, flat |
| Flaky test rate | Is CI trusted, or routinely re-run? | < 1% |
| Coverage trend by module | Is new code better tested than old? | ↑ |
| Requirements traced to a test | Can we answer a regulator? | → 100% |

A flaky suite is a quality emergency, not an inconvenience: the moment people re-run red builds
by reflex, every gate downstream of CI silently loses its evidence value.
