# 11 — Multi-Agent Review Gates

**Layer:** L1 — generic
**Pipeline step:** 10 — Multi-Agent Review → Approval Gate
**Owner:** Architect (custodian) · each board's named role

---

## 1. The board

```text
                  IMPLEMENTATION PLAN
                         │
        ┌────────────────┼──────────────────┐
        ▼                ▼                  ▼
 Architecture          Product           Technical
   Reviewer            Reviewer           Reviewer

        ▼                ▼                  ▼
 Security              QA             Risk/Compliance
 Reviewer            Reviewer             Reviewer

                         ▼
                   Operations Reviewer
                         │
                         ▼
                  Approval Aggregator
                         │
                 ┌───────┴───────┐
                 ▼               ▼
              APPROVE          REWORK
```

Seven boards, one aggregator, one gate. Each board reviews **the plan**, not the diff, and each
answers only the questions in its own checklist — a Security reviewer commenting on naming is
noise, and noise is what makes teams stop reading reviews.

---

## 2. Who may sit on a board

| Reviewer type | Marker | May satisfy |
|---------------|--------|-------------|
| Human role owner | `reviewer_type: HUMAN` | Any board |
| AI agent simulating a board | `reviewer_type: AGENT` | T1–T2 fully; T3 provisionally; **never** the mandatory human sign-offs at T4 |

Rules:

- An agent authoring a plan **may** produce agent verdicts for it, but must mark them
  `self_review: true`. A self-reviewed T3 plan needs at least one human board.
- **Security** and **Risk & Compliance** verdicts at T4 require `HUMAN`. No exceptions, no
  aggregate override.
- An agent verdict of `APPROVED` with an empty `evidence[]` is **invalid** and is treated as
  `NOT_RUN` (Rule RG-3, §11).

---

## 3. Proportionality — which boards are mandatory

Rigour scales with risk. Running seven boards on a typo teaches everyone to rubber-stamp.

| Board | T1 Trivial | T2 Standard | T3 Significant | T4 Critical |
|-------|:----------:|:-----------:|:--------------:|:-----------:|
| Architecture | — | if boundaries touched | ✅ | ✅ human |
| Technical | ✅ | ✅ | ✅ | ✅ |
| Product | — | ✅ | ✅ | ✅ |
| QA | — | ✅ | ✅ | ✅ |
| Security | — | if security_impact ≠ none | ✅ | ✅ **human** |
| Risk & Compliance | — | if compliance_impact ≠ none | ✅ | ✅ **human** |
| Operations | — | if operational_impact ≠ none | ✅ | ✅ |

**Automatic T4 triggers** (any one): PII handling · secrets or credentials · authn/authz ·
cryptography · money movement · consent or retention · data migration or backfill · production
topology · a breaking public contract · anything a regulator can ask about.

---

## 4. Board 1 — Architecture

**Question:** *Does this belong here, shaped like this?*

### Named persona and accountable owner

For this repository, Board 1 uses **[Mahesh — Principal Insurance Platform Architect](../context/roles/mahesh-solution-architect.md)** as its single named Architecture persona and accountable Architecture Board owner. Mahesh's deeper authority, decision, evidence and exception model is modularized in the **[Mahesh Principal Architect package](../context/roles/mahesh-principal-insurance-platform-architect/README.md)**. Those files are part of the same Mahesh persona, not a separate role.

When an AI agent simulates Board 1 it should load Mahesh and the relevant modules from that package, apply his authority/decision framework, and translate the result into the canonical AIGEM verdict below. The AI simulation **does not** grant itself Mahesh's mandatory human signature; the T4 rule in §2 remains binding.

Architecture findings may use `A0`–`A3` severity internally. These labels must not be confused with AIGEM `P1`–`P5` delivery priority, Rajal's local Product `P0`–`P2` execution criticality, Deepali's `S0`–`S3` security severity, Shivanshi's `O0`–`O3` operational severity, or Shailja S `R0`–`R3` risk severity.

For consequential Product ↔ Architecture ↔ Compliance decisions, Boards 1, 3 and 6 use the shared **[Product ↔ Architecture ↔ Compliance Decision Protocol](../context/roles/shared/product-architecture-compliance-decision-protocol.md)**. For detailed architecture-control resolution, Boards 1 and 6 additionally use the **[Mahesh ↔ Shailja Architecture/Compliance Decision Protocol](../context/roles/shared/architect-compliance-decision-protocol.md)**. When trust boundaries, identity, public exposure, cryptography or another material security concern is affected, also involve **Deepali** through the **[Security Cross-Persona Decision Protocol](../context/roles/shared/security-cross-persona-decision-protocol.md)**. When operability, capacity, scaling, deployment, recovery or production topology is materially affected, involve **Shivanshi** through the **[SRE Cross-Persona Decision Protocol](../context/roles/shared/sre-cross-persona-decision-protocol.md)**. Mahesh owns architecture design/implementation; Deepali owns Security outcomes; Shivanshi owns Board 7/SRE operational posture; Board 6/Shailja owns compliance permissibility/control outcomes.

| # | Check |
|---|-------|
| A1 | Does it respect module, service, and bounded-context boundaries? |
| A2 | Is the responsibility in the correct component? |
| A3 | Does it introduce coupling — and is that coupling justified and directional? |
| A4 | Does it violate an architectural principle or standing constraint ([01 §5](./01-CURRENT_STATE.md#5-standing-constraints-apply-to-every-triage-in-this-repo))? |
| A5 | Does an ADR exist if an architectural decision is changing? |
| A6 | Are we introducing unnecessary infrastructure? |
| A7 | Does it create a future migration problem (data, contract, topology)? |
| A8 | Does it fit the current stage, or is it importing a later stage's complexity? |
| A9 | Is this the **smallest** structural change that meets the objective? |
| A10 | If we later need to replace this, what is the cost? |

Project-specific hard checks (WS-1): 1SB types confined to `adapter.onesb.*` · no Flyway/JPA in
the integration service · persistence reached over HTTP only · bank apps never reach 1SB or the
DB directly. (WS-2): Keycloak behind the adapter · no tokens to Flutter · PDP owns business
authorization.

```yaml
architecture:
  decision: APPROVED
  issues: []
  recommendations:
    - "Consider event publishing later during the integration stage."
```

## 5. Board 2 — Technical

**Question:** *Will this work, and can we live with it?*

Implementation feasibility · code organisation · API contracts · error handling · data handling ·
backward compatibility · concurrency · transaction boundaries · framework usage ·
maintainability · complexity.

| # | Check |
|---|-------|
| T1 | Is the approach feasible with the current stack and skills? |
| T2 | Are error paths defined, not just the happy path? |
| T3 | Are transaction and concurrency boundaries explicit? |
| T4 | Is backward compatibility stated and correct? |
| T5 | Is the complexity proportional to the problem? |
| T6 | Does it duplicate something that exists (DRY) — or wrongly unify things that differ? |
| T7 | Are `files_expected` plausible and complete? |
| T8 | Is the rollback real? |

## 6. Board 3 — Product

**Question:** *Is this the thing we asked for — and only that?*

### Named persona and Product authority

For this repository, Board 3 uses **[Rajal — Principal Insurance Platform Product Owner](../context/roles/principal-insurance-platform-product-owner/README.md)** as its named Product reasoning persona.

Rajal owns **WHAT / WHY / FOR WHOM / Product behaviour / scope / priority / acceptance / outcome**. She does not own technical architecture, regulatory permissibility, Security exceptions or material human risk acceptance.

When an AI agent simulates Board 3 it should load Rajal's canonical package in the order defined by its README, apply the Product authority and decision framework, and then translate the result into the canonical AIGEM verdict below. The persona does not grant itself authority that AIGEM or organisational policy reserves for humans.

Rajal's local `P0`–`P2` labels are Product execution criticality **within admitted scope**. They must not replace AIGEM `P1`–`P5` delivery priority.

For Product decisions with material Architecture or Compliance impact, Board 3 uses the shared **[Product ↔ Architecture ↔ Compliance Decision Protocol](../context/roles/shared/product-architecture-compliance-decision-protocol.md)**. Where Security is material, it also invokes Deepali through the Security Cross-Persona Decision Protocol. Where reliability/operability/capacity or degraded production behaviour is material, it involves Shivanshi through the SRE Cross-Persona Decision Protocol. Product may challenge another board's assumptions, but cannot silently override its binding domain decision.

| # | Check |
|---|-------|
| P1 | Does it satisfy the requirement and approved business objective? |
| P2 | Does the behaviour match the approved Product/journey expectation? |
| P3 | **Are we adding unrequested behaviour?** |
| P4 | Are the acceptance criteria correct, observable and complete? |
| P5 | Does it change the customer, RM, operations or channel experience — and is that intended? |
| P6 | **Does it introduce scope creep or import later-stage functionality?** |
| P7 | Is `out_of_scope` honest, or does it omit what the plan quietly includes? |
| P8 | Are actor, LoB/product, journey/capability and business state explicit where material? |
| P9 | Does the plan preserve canonical bank Product behaviour rather than leaking provider/aggregator API shape into the Product model without justification? |
| P10 | Is this the smallest Product change that delivers the approved outcome, with P1/P2 improvements parked rather than silently bundled? |
| P11 | Are material failure, abandonment, resume and exception outcomes defined from the Product perspective? |
| P12 | Is the intended outcome measurable through an agreed KPI/evidence path? |

P3, P6, P9 and P10 are central defences against gold-plating, provider-driven Product distortion and uncontrolled scope expansion.

## 7. Board 4 — Security

**Question:** *What does this expose, what can be abused, and are the required security controls/evidence sufficient?* — **veto power**

### Named persona and Security authority

For this repository, Board 4 uses **[Deepali — Principal Insurance Platform Security Architect / Security Head](../context/roles/deepali-principal-security-architect/README.md)** as its named Security reasoning persona.

Deepali owns Security outcomes within her jurisdiction: trust boundaries, public/private exposure, IAM security, authentication/authorization controls, cryptography, keys/secrets/certificates, application/API security, cloud/Kubernetes/container security, third-party trust, DevSecOps/supply-chain security, threat modelling, vulnerability security severity and incident containment recommendations.

Deepali uses local `S0`–`S3` **security severity**. It must not be confused with AIGEM `P1`–`P5` delivery priority or other persona-local severity models.

When an AI agent simulates Board 4, it loads Deepali's package and emits the canonical Security verdict with evidence. At **T4**, that AI review cannot satisfy the mandatory human Security sign-off in §2.

Deepali is not authorised to redefine Product behaviour, replace Mahesh's overall architecture authority, replace Engineering/SRE/DBA/QA execution authority, reinterpret regulation on Shailja's behalf or accept material organisational risk for an accountable human.

For cross-persona security decisions use **[Security Cross-Persona Decision Protocol](../context/roles/shared/security-cross-persona-decision-protocol.md)** and the canonical **[Persona Authority Matrix](./PERSONA-AUTHORITY-MATRIX.md)**.

Authentication · authorization · PII/restricted data · secrets · encryption · key/certificate lifecycle · input validation · attack surface · OWASP/security abuse classes · auditability · dependency/supply-chain vulnerabilities · detection/incident readiness.

| # | Check |
|---|-------|
| S1 | Does it change who/what can do what? Are authentication, authorization and resource ownership explicit? |
| S2 | Is any PII/restricted/health/financial data introduced, moved, logged, persisted, exported or shared? Is every field necessary? |
| S3 | Are secrets, credentials, keys and certificates stored, retrieved, rotated and revoked through approved mechanisms — never hard-coded or leaked? |
| S4 | Is data protected in transit and at rest where required, with correct key/certificate ownership and cryptographic agility? |
| S5 | Is all external/untrusted input validated at the boundary, including callbacks, uploads and provider payloads? |
| S6 | Does the public/east-west/third-party attack surface grow? Is that growth necessary and bounded? |
| S7 | Which applicable application/API/security-abuse classes affect the changed paths, including object-level authorization, injection, SSRF, replay and sensitive-data exposure? |
| S8 | Are security-relevant events attributable, auditable and detectable without leaking secrets or restricted payloads? |
| S9 | New/updated dependencies, images, IaC or artifacts: known vulnerabilities, reachability, provenance and remediation? |
| S10 | Does the protected path fail closed or use an explicitly approved safe-degraded mode? |
| S11 | For partner/1SB/insurer communication, is the trust contract explicit: identity, authorization, network path, payload, replay/integrity, credential rotation and emergency revoke? |
| S12 | What is the blast radius if this workload, credential, user or partner is compromised, and how is compromise contained? |

## 8. Board 5 — QA

**Question:** *How will we know it works — and know when it breaks?*

| # | Check |
|---|-------|
| Q1 | Is each acceptance criterion observable and testable? |
| Q2 | Are unit, integration, and (where relevant) E2E levels appropriate? |
| Q3 | Are negative, boundary, and error cases covered — not only the happy path? |
| Q4 | Do coverage gates still hold ([COVERAGE.md](../1sb-insurance-integration/service-ssot/COVERAGE.md))? |
| Q5 | Is regression risk to existing journeys identified? |
| Q6 | Is test data realistic and PII-free? |
| Q7 | Is the change demonstrable to a PO? |
| Q8 | Does it follow [TESTING-RULES.md](../1sb-insurance-integration/service-ssot/TESTING-RULES.md)? |

## 9. Board 6 — Risk & Compliance

**Question:** *Can we defend this to a regulator?* — **veto power**

Regulatory requirements · consent · audit · data retention · legal · financial controls ·
operational risk · traceability.

For this repository, Board 6 uses **[Shailja S — Compliance & Risk Head](../context/roles/shailja-s-compliance-risk-head/README.md)** as its named reasoning persona. Shailja's package supplements this checklist with obligation classification, evidence, risk severity and human-exception rules; it never replaces the T4 human sign-off rule.

Where a control is materially security-specific, Board 6 should consult Deepali rather than treating Compliance as a substitute for technical Security authority. Where business continuity, recovery evidence, operational resilience or production incident control is material, Board 6 should consult Shivanshi rather than treating Compliance as a substitute for SRE/Operations evidence. Deepali determines technical security posture; Shivanshi determines Board 7 operational posture; Shailja determines regulatory/compliance permissibility and mandatory control outcome.

| # | Check |
|---|-------|
| R1 | Does any regulatory obligation apply (IRDAI, banking, data protection)? |
| R2 | Is consent captured, referenced, and enforceable where required? |
| R3 | Is the action auditable with actor attribution? |
| R4 | Are retention and deletion rules satisfied? |
| R5 | Are financial controls (maker-checker, limits, reconciliation) preserved? |
| R6 | What is the operational risk if this misbehaves in production? |
| R7 | Is the change traceable from requirement to evidence? |
| R8 | Does it create a reporting or disclosure obligation? |

## 10. Board 7 — Operations

**Question:** *Can we run, observe, and recover this?*

### Named persona and SRE / Operations authority

For this repository, Board 7 uses **[Shivanshi — Principal Insurance Platform SRE / Reliability Engineering Head](../context/roles/shivanshi-sre/README.md)** as its named Operations reasoning persona and canonical identity for existing **R10 — DevOps / SRE**.

This is a **merge/maturity of the existing role**, not a replacement and not an eighth board. The canonical O1–O8 checklist below remains unchanged in meaning. Shivanshi's modular package adds insurance/banking/bancassurance business context, B2B/B2C/B2B2C workload reasoning, platform engineering, infrastructure/CI-CD, SLI/SLO/error budgets, incident/resilience/DR, business-aware capacity/scaling and developer-experience evidence around those controls.

When an AI agent simulates Board 7 it should load Shivanshi in the order defined by her package, apply [`08-operations-review-release-and-exception-contract.md`](../context/roles/shivanshi-sre/08-operations-review-release-and-exception-contract.md), and translate the result into the canonical AIGEM verdict below. The persona does not grant the agent destructive production authority, material risk acceptance or any other persona's decision rights.

Shivanshi may use local `O0`–`O3` **operational severity** internally. It must not be confused with AIGEM `P1`–`P5`, incident severity or other persona-local severity models.

For consequential Operations/SRE decisions use the **[SRE Cross-Persona Decision Protocol](../context/roles/shared/sre-cross-persona-decision-protocol.md)** and the canonical **[Persona Authority Matrix](./PERSONA-AUTHORITY-MATRIX.md)**.

| # | Check |
|---|-------|
| O1 | Deployability: config, env vars, secrets, migrations, ordering |
| O2 | Observability: metrics, logs, traces, correlation IDs |
| O3 | Alerting: what pages someone, and on what threshold? |
| O4 | Failure modes and blast radius |
| O5 | Rollback: tested, and sufficient given data written |
| O6 | Capacity and cost impact |
| O7 | Runbook updates needed |
| O8 | Backward compatibility during a rolling deploy |

---

## 11. Verdicts

Each board returns exactly one:

| Verdict | Meaning | Effect |
|---------|---------|--------|
| `APPROVED` | No objections | Counts toward the gate |
| `APPROVED_WITH_CONDITIONS` | Approved provided the listed conditions are met | **Conditions become acceptance criteria**, tracked to closure |
| `REWORK` | Must change before approval | Plan returns to the author with `must_fix[]` |
| `REJECTED` | The plan is wrong in kind, not in detail | Back to triage — re-examine stage/scope/necessity |
| `NOT_APPLICABLE` | This board has no interest in this change | Recorded, with a one-line reason |

Verdict record ([templates/REVIEW-VERDICT.md](./templates/REVIEW-VERDICT.md)):

```yaml
review:
  board: SECURITY
  reviewer: "Deepali / Security Architect"
  reviewer_type: HUMAN
  self_review: false
  plan: PLAN-011
  decision: APPROVED_WITH_CONDITIONS
  must_fix: []
  conditions:
    - "paymentUrl must not appear in audit payloads — assert in test"
  should_fix:
    - "consider rotating the session key on privilege change"
  evidence:
    - "checked S1–S12 against plan security impact and files_expected"
    - "PaymentSessionController audit path reviewed"
  notes: "No new unbounded attack surface; existing masking rules cover the new field."
  date: 2026-08-14
```

> **Rule RG-3 — No evidence, no verdict.** `APPROVED` with an empty `evidence[]` is recorded as
> `NOT_RUN`. This is the anti-rubber-stamp rule, and it applies to humans and agents alike.

---

## 12. Aggregation

```text
APPROVED  ⇔  every mandatory board for the tier returned APPROVED or
             APPROVED_WITH_CONDITIONS (or a justified NOT_APPLICABLE)
         AND no board returned REWORK or REJECTED
         AND every T4 human sign-off is present
         AND all conditions are recorded as acceptance criteria
```

| Situation | Outcome |
|-----------|---------|
| Any mandatory board `REWORK` | **REWORK** — plan returns with the union of all `must_fix[]` |
| Any board `REJECTED` | **REJECTED** — back to pipeline step 2 (stage/scope/necessity were probably wrong) |
| Security or Risk & Compliance `REWORK`/`REJECTED` | **Binding veto.** No aggregate or majority override |
| Architecture `REWORK` | Overridable only by a recorded ADR signed by a human architect where AIGEM permits; never overrides a separate binding Security/Compliance conclusion |
| Product `REWORK` | Product behaviour/scope/acceptance must be corrected or consciously changed by the authorised Product owner; Engineering/Architecture cannot silently override it |
| A mandatory board did not respond | Gate is **not** approved. Silence is never assent |
| Boards conflict | Use the relevant shared protocol; identify each domain owner, resolve outcome-vs-implementation separately, and persist the final decision. No majority voting |

For material Security conflict, use the Security Cross-Persona Decision Protocol. For material SRE/Operations conflict, use the SRE Cross-Persona Decision Protocol. For material Product ↔ Architecture ↔ Risk/Compliance conflict, use the shared Product ↔ Architecture ↔ Compliance protocol. For a material Mahesh/Architecture ↔ Shailja/Risk & Compliance conflict, use the bilateral Mahesh ↔ Shailja protocol. If conflict remains after one substantive alternatives/redesign cycle, escalate to accountable humans; an AI agent does not arbitrate residual material risk or mandatory sign-off.

---

## 13. Rework loop

```text
Round 1  REWORK → author revises → re-review by the objecting boards only
Round 2  REWORK → author revises → re-review
Round 3  ── not permitted ──► ESCALATE to accountable humans (Product + Architecture + other binding domain owner as applicable)
```

Two rounds is the limit. A third is a signal that the *problem*, not the plan, is misunderstood:
usually the item needs splitting, a spike, or re-triage. Rework counts feed
[18](./18-GOVERNANCE_METRICS.md); a rising average means plans are being written too early.

---

## 14. Post-approval

| Condition | Handling |
|-----------|----------|
| `APPROVED_WITH_CONDITIONS` conditions | Appended to the plan's `acceptance_criteria`; verified at DoD ([13](./13-DEFINITION_OF_DONE.md)) |
| `should_fix` items | Triaged as fresh `SUG-####` — they are suggestions, and get the same treatment as any other |
| Plan changes materially after approval | Re-review by the affected boards ([14 §4](./14-CHANGE_CONTROL.md#4-changing-an-approved-plan)) |
| Approval age > one stage | Expired. Re-run the boards — the context that justified it has changed |

---

## 15. Running the board as a single agent

A solo agent simulating seven boards must review **in role, sequentially**, not as one blended
pass — the value is in the different questions, and blending them loses exactly that.

```text
For each mandatory board:
  1. Load only that board's checklist and its named persona when one is defined.
     - Architecture → Mahesh — Principal Insurance Platform Architect
     - Product → Rajal / Principal Insurance Platform Product Owner
     - Security → Deepali — Principal Insurance Platform Security Architect / Security Head
     - QA → Swapnali — Principal Insurance Quality Engineering / QA Lead
     - Risk & Compliance → Shailja S
     - Operations → Shivanshi — Principal Insurance Platform SRE / Reliability Engineering Head
  2. Answer each numbered check against the plan — cite the plan section or file.
  3. Emit the verdict with evidence[] listing the checks actually performed.
  4. Do not carry the previous board's conclusion into the next.
```

Where the repository already has role personas
([docs/context/roles/](../context/roles/README.md)), an agent should adopt the matching persona for that
board: it produces sharper, more consistent verdicts than a generic reviewer voice. Persona authority never expands the reviewer's AIGEM authority or removes mandatory human sign-offs.