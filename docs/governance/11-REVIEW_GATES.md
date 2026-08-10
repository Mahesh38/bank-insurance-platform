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

| # | Check |
|---|-------|
| P1 | Does it satisfy the requirement? |
| P2 | Does the behaviour match product expectation? |
| P3 | **Are we adding unrequested behaviour?** |
| P4 | Are the acceptance criteria correct and complete? |
| P5 | Does it change the customer or RM experience — and is that intended? |
| P6 | **Does it introduce scope creep?** |
| P7 | Is `out_of_scope` honest, or does it omit what the plan quietly includes? |

P3 and P6 are the board's real job. Product is the primary defence against gold-plating.

## 7. Board 4 — Security

**Question:** *What does this expose?* — **veto power**

Authentication · authorization · PII · secrets · encryption · input validation · attack surface ·
OWASP · auditability · data exposure · dependency vulnerabilities.

| # | Check |
|---|-------|
| S1 | Does it change who can do what? |
| S2 | Is any PII introduced, moved, logged, or persisted? |
| S3 | Are secrets handled through the secrets SPI — never in code, config, or logs? |
| S4 | Is data encrypted at rest and in transit where required? |
| S5 | Is all external input validated at the boundary? |
| S6 | Does the attack surface grow? Is the growth necessary? |
| S7 | OWASP Top 10 relevance for the changed paths |
| S8 | Are security-relevant events auditable and attributable? |
| S9 | New or updated dependencies: known vulnerabilities, provenance |
| S10 | Failure mode: does it fail **closed**? |

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
  reviewer: "Security Architect"
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
    - "checked S1–S10 against plan §security_impact and §files_expected"
    - "PaymentSessionController audit path reviewed"
  notes: "No new attack surface; existing masking rules cover the new field."
  date: 2026-08-12
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
| Architecture `REWORK` | Overridable only by a recorded ADR signed by a human architect |
| A mandatory board did not respond | Gate is **not** approved. Silence is never assent |
| Boards conflict (e.g. Security wants X, Product wants not-X) | Escalate to Architect + PO; the resolution is recorded as an ADR |

---

## 13. Rework loop

```text
Round 1  REWORK → author revises → re-review by the objecting boards only
Round 2  REWORK → author revises → re-review
Round 3  ── not permitted ──► ESCALATE to a human (Architect + PO)
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
  1. Load only that board's checklist.
  2. Answer each numbered check against the plan — cite the plan section or file.
  3. Emit the verdict with evidence[] listing the checks actually performed.
  4. Do not carry the previous board's conclusion into the next.
```

Where the repository already has role personas
([docs/context/roles/](../context/roles/README.md)), an agent should adopt the matching persona for that
board: it produces sharper, more consistent verdicts than a generic reviewer voice.
