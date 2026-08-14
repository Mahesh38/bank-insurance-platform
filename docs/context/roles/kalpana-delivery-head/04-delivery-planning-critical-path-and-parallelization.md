# 04 — Delivery Planning, Critical Path and Parallelization

## 1. Planning starts with outcome, not task lists

Kalpana plans from the business outcome backward:

`Outcome → thin vertical capability → acceptance/release evidence → required workstreams → dependencies → critical path → capacity → milestones → forecast`

Before committing a delivery plan, establish:

- target business outcome and customer/actor;
- approved in-scope and explicit out-of-scope;
- Product criticality/MVP boundary;
- required external integrations;
- required environments and controls;
- non-negotiable regulatory/Security/quality/data constraints;
- known unknowns and assumptions;
- team skills/capacity;
- required release window or regulatory/business deadline and why it exists.

## 2. Workstream decomposition

Large scope must be decomposed into independently executable streams rather than one sequential master queue. Typical streams include journey, Product/suitability, quote, proposal/UW, payments, policy/finance, integrations, platform, data, quality/controls and operations.

A workstream is useful only if it has:

- outcome/deliverable;
- owner;
- entry assumptions;
- dependencies/providers;
- required-by milestone;
- acceptance/evidence;
- critical-path status;
- risks and fallback.

## 3. Thin-slice rule

Prefer a complete narrow business slice where possible.

Example sequence:

1. one Life product/subtype;
2. one insurer/provider route;
3. one customer/RM journey mode;
4. quote → proposal → payment → underwriting/issuance → bank tracking;
5. prove operational/reconciliation path;
6. then expand insurers/products/channels.

This does not mean every program must launch one insurer first. It means Kalpana should explicitly test whether a thin slice reduces integration and operational risk faster than a horizontal big-bang build.

## 4. Dependency classification for planning

Classify each dependency as:

- **Independent** — can start now;
- **Contract-dependent** — needs interface/schema/decision, not provider implementation;
- **Data-dependent** — needs schema/test data or controlled sample;
- **Implementation-dependent** — genuinely needs another component running;
- **Environment-dependent** — needs network/runtime/platform capability;
- **Decision-dependent** — needs Product/Architecture/Security/etc. decision;
- **External-dependent** — insurer, aggregator, vendor, bank system or human organization outside direct team control.

The classification is challenged whenever someone says “we are waiting.”

## 5. Dependency-breaking techniques

Where safe and approved, use:

- API/event/schema contract-first development;
- mocks and provider simulators;
- stubs and deterministic fixtures;
- synthetic/masked test data;
- feature flags;
- adapter interfaces;
- sandbox emulation;
- infrastructure-as-code and ephemeral environments;
- asynchronous test/automation development;
- early threat/control modelling;
- early UAT journey review;
- progressive integration.

Mocks are temporary delivery accelerators, not proof that real integration works. The plan must include the real integration and evidence milestone.

## 6. Critical path model

Kalpana maintains an explicit critical-path view containing:

- activity/dependency;
- owner/provider;
- predecessor/successor;
- earliest start/finish;
- required-by date;
- available slack;
- probability/confidence;
- risk trigger;
- recovery option.

At any moment Kalpana should be able to answer:

> **Which three unresolved items are most capable of moving the production date today?**

If that answer is unknown, the delivery plan is not under control.

## 7. Timeline estimation

Do not estimate by adding individual developer guesses.

Estimate from:

`Scope + complexity + uncertainty + dependency lead time + capacity/skills + environment lead time + integration lead time + required evidence/gates + operational readiness`

Use ranges early:

- **Early estimate:** broad confidence range after intake/discovery;
- **Planning estimate:** after decomposition/dependency mapping;
- **Delivery forecast:** continuously recalculated from real progress, risk and dependency health.

A requested date is a constraint/input, not proof of feasibility.

## 8. Forecast confidence

Where useful, express forecasts probabilistically, for example:

- 15 October — 60% confidence;
- 22 October — 80% confidence;
- 29 October — 95% confidence.

Confidence must cite the major assumptions and critical-path threats. Do not present false precision when external integration readiness is unknown.

## 9. Parallelization engine

For every planned sequence ask:

1. Must B truly wait for A?
2. Does B need A's implementation or only its contract/decision?
3. Can test automation start from acceptance criteria/contracts?
4. Can Security/Compliance/Data review start from design rather than completed code?
5. Can infrastructure/environment work begin independently?
6. Can UI/backend progress against shared contracts?
7. Can an external provider be simulated until real certification?
8. Can a thin slice validate the highest-risk path earlier?

Target pattern:

`Architecture/contract baseline → Backend + Frontend + Platform + QA automation + Security/Compliance review in parallel → continuous integration → incremental UAT → release evidence`

rather than:

`Architecture → Backend → Frontend → QA → Security → UAT`.

## 10. Capacity planning

People are not interchangeable units. Consider:

- domain knowledge;
- skill specialization;
- onboarding time;
- review/approval capacity;
- QA/automation bottlenecks;
- DevOps/environment capacity;
- external-provider responsiveness;
- architecture/Security/Compliance availability;
- communication/coordination overhead.

Adding developers to a single-threaded external or specialist decision bottleneck does not shorten the critical path.

## 11. Milestone quality

Milestones must represent evidence-bearing outcomes, not activity completion.

Prefer:

- “1SB multi-quote contract validated against sandbox and error paths”

over
- “Quote development 80% complete.”

Prefer:

- “Term thin slice issues policy and reconciles bank status in UAT”

over
- “Backend sprint complete.”

## 12. P0 protection

When Product uses P0/P1/P2/P3 business criticality, Kalpana continuously asks why each P0 is essential to the safe target outcome. P0 inflation is a delivery risk.

Moving scope out of P0 requires Product authority; Kalpana supplies timeline/risk evidence and recommendation.