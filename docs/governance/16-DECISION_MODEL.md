# 16 — Decision Model: Necessity, Evidence, Confidence

**Layer:** L1 — generic
**Pipeline step:** 4 — Necessity Assessment (and the confidence gate that governs steps 9–14)
**Owner:** Product Owner (business necessity) · Architect (technical necessity)

---

## 1. The question this step answers

Stage fit asked *when*. Scope fit asked *whether it is ours*. Necessity asks:

> **What breaks if we never do this?**

If the honest answer is "nothing measurable", the item is COULD at best — regardless of how
elegant, standard, or widely recommended it is. "Best practice" is not a necessity argument; it
is a prior that still needs local evidence.

---

## 2. Necessity levels

| Level | Test — the item is X if… | Consequence of omission |
|-------|--------------------------|-------------------------|
| **MUST** | An in-scope deliverable is **incorrect, unsafe, non-compliant, unusable, or ungateable** without it | Deliverable fails, gate blocked, or obligation breached |
| **SHOULD** | It **materially** improves correctness, safety, cost, or speed, and the loss is quantifiable | Known, accepted, bounded cost |
| **COULD** | It improves something real but small; nothing depends on it | Nothing measurable |
| **NOT-NOW** | Necessity is genuine but **binds at a later stage** | Nothing now; something later — record what and when |
| **REJECT** | No stage will need it, or it contradicts an approved decision | Nothing, ever |

### The MUST test — all four must hold

1. **Named failure.** What concretely fails? An AC, a gate criterion, a regulation, a security
   control, a user journey. Name it.
2. **In-scope victim.** The thing that fails is in scope ([02](./02-PROJECT_SCOPE.md)).
3. **Now, not later.** The failure occurs at the *current* stage. If it only occurs later, the
   level is NOT-NOW with `future_necessity: MUST`.
4. **No cheaper sufficient alternative.** If a smaller change removes the failure, *that* is the
   MUST and this is a SHOULD.

Failing test 3 is the single most common misclassification, and it produces exactly the
behaviour this framework exists to stop: importing production-readiness work into domain design.

### NOT-NOW carries three fields

```yaml
necessity:
  now: NOT-NOW
  future_necessity: MUST
  target_stage: "Phase 6 — Production readiness"
  binds_when: "first production deployment"
```

NOT-NOW is not a soft REJECT. It is a **scheduled MUST**, and the scheduling is the point.

### REJECT requires an argument

Valid: contradicts a standing constraint · solves a problem the roadmap has decided not to
have · superseded by a recorded decision (cite it) · duplicate (link it).

Not valid: "unnecessary complexity" without saying what complexity and why it exceeds the
benefit; "not how we do things" without a principle; personal preference.

---

## 3. Necessity is not urgency and not value

Three independent axes, routinely conflated:

| Axis | Question | Where |
|------|----------|-------|
| **Necessity** | What breaks without it? | Here |
| **Urgency** | When does the breakage bite? | [05](./05-PRIORITY_MODEL.md) via stage fit + risk |
| **Value** | What do we gain? | Cost/value in scoring (§8) |

A MUST with distant urgency is P4. A COULD that is nearly free and unblocks three items may be
P3. Keeping the axes separate is what allows the model to schedule rather than merely rank.

---

## 4. Evidence standard

Every necessity claim carries evidence. Ranked, strongest first:

| Tier | Evidence | Example |
|------|----------|---------|
| **E1** | Reproducible failure | Failing test, failing gate job, reproduced defect |
| **E2** | Written obligation | Regulation, contract, approved AC, gate criterion, security policy |
| **E3** | Measurement | p95 latency, error rate, coverage number, load result |
| **E4** | Observed incident or usage | Production/UAT incident, consumer complaint, support ticket |
| **E5** | Expert reasoning with a named mechanism | "Two instances + in-memory idempotency ⇒ duplicate sessions on retry" |
| **E6** | Analogy / general practice | "Most services use a circuit breaker here" |
| **E7** | Preference | "Cleaner", "more modern", "I'd prefer" |

> **Rule EV-1 — MUST requires E1–E3.** MUST claimed on E5 is downgraded to SHOULD unless the
> mechanism is proven — the usual proof is a test, which is itself cheap.
> **Rule EV-2 — E6/E7 alone caps the item at COULD** and usually at SC2 (adjacent value).

Assumptions used as evidence must be registered:

```yaml
assumption:
  id: ASM-004
  statement: "The service will run multi-instance in UAT before Phase 5 exit"
  used_by: [SUG-0043, PLAN-011]
  validation: "confirm with Ops in the Phase 4 gate review"
  expiry: "Phase 4 gate"
  status: OPEN            # OPEN | VALIDATED | INVALIDATED
  if_invalidated: "TD-010 severity drops from P2 to P3; Redis work re-parks to Phase 6"
```

`if_invalidated` is what makes an assumption register useful rather than decorative: it
pre-computes the consequence, so invalidation triggers a known action instead of a debate.

---

## 5. Confidence levels

Confidence in the **classification**, not in the implementation.

| Level | Meaning | Permitted action |
|-------|---------|------------------|
| **C5** | Certain — E1/E2 evidence, unambiguous stage and scope | Any |
| **C4** | High — strong evidence, minor unknowns | Any |
| **C3** | Adequate — reasoning is sound, unknowns are bounded and named | Any; name the unknowns in the plan's `assumptions` |
| **C2** | Low — material unknowns; the classification could flip | **SPIKE only.** No implementation |
| **C1** | Guess — no reliable basis | Ask a human. Do not classify |

> **Rule CF-1 (= PRI-6) — Confidence < C3 forbids implementation.** Convert to a `SPIKE`. The
> spike may itself be P1.
> **Rule CF-2 — State confidence explicitly** in every triage record. An unstated confidence is
> read as C1 by reviewers, which is usually not what the author meant.
> **Rule CF-3 — Low confidence is not a reason to delay a *decision*;** it is a reason to change
> *which* decision you make. "Spike it" is a decision.

---

## 6. Anti-over-engineering tests

Run these before admitting anything that adds structure, abstraction, dependency, or
infrastructure. **Any NO is a park or reject signal.**

| # | Test | Fail ⇒ |
|---|------|--------|
| **X1 Named consumer** | Is there a consumer *today* that needs it? | Speculative → SC2/park |
| **X2 Two implementations** | For an abstraction: do ≥ 2 real implementations exist or are they scheduled? | One implementation → inline it. No interfaces "for testability" alone |
| **X3 Cheap later** | Can this be added later without migration, backfill, or a breaking contract change? | Cheap later ⇒ **do it later**. Expensive later ⇒ raises `D` in scoring |
| **X4 Reversibility** | If wrong, what does it cost to undo? | Hard to undo ⇒ needs an ADR and a higher tier |
| **X5 Stage necessity** | Does the *current* stage need it, or a future one? | Future ⇒ SF3 |
| **X6 Simplest sufficient** | Is this the smallest change that removes the named failure? | No ⇒ split; admit only the minimal part |
| **X7 Runtime cost** | Does it add a service, broker, cache, or daemon to run? | Yes ⇒ Architecture + Ops boards, always |
| **X8 Cognitive cost** | Does a new joiner need to learn a new concept to work here? | Yes ⇒ needs proportionate benefit and documentation |
| **X9 Evidence of the problem** | Has the problem it solves ever occurred here? | Never occurred + no obligation ⇒ COULD at best |
| **X10 Do nothing** | What actually happens if we never do it? | "Nothing measurable" ⇒ REJECT or Ideas |

Classic failures these catch: an interface with one implementation "for flexibility"; a caching
layer without a measured latency problem; a message broker for two services that call each
other synchronously today; a plugin architecture for one plugin; retry logic for a call that has
never failed; generic frameworks built during a vertical slice.

**X3 deserves emphasis.** Most premature work is not wrong in kind, only in time — and the
correct response to "we'll need this eventually" is almost always *"then we'll add it
eventually"*, unless adding it later requires a migration. That single distinction — cleanup
cost versus migration cost — settles most stage-fit arguments.

---

## 7. Revalidation triggers

A decision is valid for the context it was made in. Re-open when the context moves:

| Trigger | Re-validate |
|---------|-------------|
| Stage transition | All parked items; all READY priorities |
| Scope change (CR approved) | Items rejected on scope grounds |
| Assumption invalidated | Every item and plan citing that `ASM-###` |
| Dependency resolved or removed | Items in `PARKED-DEPENDENT` |
| Recurrence count ≥ 3 | The rejected or parked item — independent rediscovery is evidence |
| New regulation or security finding | Compliance and security rejections |
| Measurement contradicts an estimate | NFR classifications built on that estimate |
| Two rework rounds on one plan | The item's *classification*, not just the plan |
| An incident occurs | Every item that would have prevented it — including rejected ones |

The last row is the most valuable feedback loop the model has: after any incident, search the
suggestion register for items that would have prevented it. A rejection that an incident proves
wrong is not an embarrassment — it is calibration data for [18](./18-GOVERNANCE_METRICS.md).

---

## 8. Cost vs value

For SHOULD/COULD items where the decision is genuinely close:

```text
VALUE  = (necessity weight × blast radius) + enablement + risk reduction
COST   = build effort + operational cost + cognitive cost + reversibility cost
DECIDE = VALUE clearly > COST ?  admit at scored priority
                              :  park to the stage where VALUE rises or COST falls
```

Note what makes VALUE rise later: reaching the stage where the item's failure mode actually
occurs. This is why parking is so often the right answer rather than a compromise — the same
item genuinely is worth more later.

---

## 9. Necessity block in the triage record

```yaml
necessity:
  now: NOT-NOW
  future_necessity: MUST
  target_stage: "Phase 6 — Production readiness"
  binds_when: "first production deployment"
  failure_without_it: "no verified recovery path for a regional outage"
  evidence_tier: E2                    # go-live checklist obligation
  evidence:
    - "ACTION-PLAN Phase 6.3/6.5 — backup/restore and rollback plan required"
  confidence: C4
  anti_over_engineering:
    X1_named_consumer: false           # no consumer today
    X3_cheap_later: true               # no migration cost in deferring
    X5_stage_necessity: false          # future stage
    X9_problem_observed: false
  verdict_input: "PARK — scheduled MUST, not a rejection"
```
