# 01 — Process Gap Analysis

**Owner:** Platform Product Owner
**Date:** 2026-08-12
**Status:** Evidence pack for the war room — findings, not decisions
**Method:** Every claim below is sourced from this repository — git history (`git log --follow
--diff-filter=A`), the governance state file, and the PO's own programme documents. No claim
rests on recollection.

---

## 1. The claim being tested

The Product Owner's statement, in full:

> *"We started with deciding the problem statement. We didn't create the BRDs. We didn't create
> the PRDs. Then we started designing screens, then we decided the tech stack, then we started
> doing the development, and then we started creating BRDs. This is not the right approach."*

This document tests that claim against the repository. **It holds, with one correction and one
finding the statement did not anticipate.**

---

## 2. What the repository history actually shows

First-appearance date of each artefact class, from git (`--follow`, so the 2026-08-10 docs
restructure does not distort the dates):

| Day | Date | What landed | Canonical stage this belongs to |
|:---:|------|-------------|--------------------------------|
| **1** | 2026-07-30 | Repo init · 1SB knowledge base · **1SB technical architecture** · **`libs/` shared libraries** · **`1sb-integration-service`** · **`bank-persistence-service`** · `ACTION-PLAN.md` with Phases 0–6 | L3 → L4 → L5 |
| **2** | 2026-07-31 | Project charter · product vision · discovery backlog · journey canvas · Figma intake · **Working Decisions v1** · **BRD-OVERVIEW** · **BRD-P0** · **PRD-R0** · **R0-SCOPE** · PO drive (SWOT, gaps, programme TODO) · architecture review | **L0 → L1** |
| 5 | 2026-08-04 | **`business-problem-statement.md`** · knowledge-base synthesis of the client volumes | **L0** |
| 7 | 2026-08-06 | Workforce auth SSOT (342 lines) **and** three identity services — `workforce-access-bff`, `identity-provider-adapter-service`, `identity-authorization-service` — **in a single commit** (`cd40460`) | L3 **and** L4/L6, same commit |
| 8 | 2026-08-07 | AIGEM governance model adopted | L0 (process foundation) |
| 11 | 2026-08-10 | Docs restructure · current-state ratified (GOV-004) · CR-001 | — |

Read the "canonical stage" column downward. The build order was:

```text
ACTUAL      L3 architecture → L4 libs → L5 services → L0/L1 requirements → L0 problem statement
            → L0 governance
IDEAL       L0 discovery → L1 business design → L2 domain → L3 technical design → L4 foundation
            → L5 connectivity → L6 vertical slice → L7 hardening
```

**Finding F1 — confirmed.** The programme executed L3–L5 before L0–L1. The consolidated business
problem statement — the artefact that defines what problem we are solving — was written on day 5,
after two services and five shared libraries already existed.

### The single-commit finding

Commit `cd40460` (2026-08-06) added the workforce authN/authZ specification **and** its three
implementing services together. A specification that arrives in the same commit as its
implementation was never a specification — it is documentation of a decision already taken in
code. There was no point at which the Security Architect could have reviewed the design before
the design was built. That is the same defect as F1, expressed at commit granularity.

---

## 3. One correction to the PO's statement

> *"We didn't create the BRDs. We didn't create the PRDs."*

**This is not quite right, and the room will find out within thirty seconds of opening the repo,
so we state it ourselves.** The following exist:

| Artefact | State today | Honest description |
|----------|-------------|--------------------|
| [BRD-OVERVIEW.md](../../requirements/BRD-OVERVIEW.md) | Created 2026-07-31 | A **chapter map** — headings, not requirements |
| [BRD-P0-CAPABILITIES.md](../../requirements/BRD-P0-CAPABILITIES.md) | Created 2026-07-31 | Capability list, **not aligned** to the BRD overview sections |
| [PRD-R0-DISTRIBUTION-PLATFORM.md](../../requirements/PRD-R0-DISTRIBUTION-PLATFORM.md) | Created 2026-07-31 | Draft; **"review & approve"** still unticked in the programme TODO |
| [R0-SCOPE.md](../../requirements/R0-SCOPE.md) | Created 2026-07-31 | MVP one-pager; **sponsor sign-off open** |
| [Working Decisions v1](../../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) | Created 2026-07-31 | Marked **"Working, not sponsor-signed"** in its own header |

So the accurate statement — and the one to use in the war room — is:

> **We have a requirement skeleton with no acceptance criteria and no signature.** `GAP-008`
> ("BR templates lack AC") is still *In progress*. Not one requirement in this repository is
> written to the standard that a developer can build from, a tester can test against, or
> Compliance can audit. The documents exist; the **requirements** do not.

This correction strengthens the case rather than weakening it: "we never wrote them" invites a
document-count rebuttal, "they are unsigned and untestable" does not.

---

## 4. Are the five services the wrong scope?

The PO's concern: *"we have already built five services which might not be the real phase one or
phase zero scope."* Tested against [R0-SCOPE](../../requirements/R0-SCOPE.md) and the epic list
in [03-PROGRAMME-TODO](../03-PROGRAMME-TODO.md) Wave 2:

| Service | Maps to R0 epic | In R0 scope? | Traceable to a requirement ID? |
|---------|-----------------|:------------:|:------------------------------:|
| `1sb-integration-service` | `E-HUB` Integration Hub Phase A | ✅ Yes | ❌ No |
| `bank-persistence-service` | Cross-cutting enabler for `E-HUB` / `E-AUDIT` | ✅ Yes | ❌ No |
| `workforce-access-bff` | `E-ID` Identity & RM session | ✅ Yes | ❌ No |
| `identity-provider-adapter-service` | `E-ID` | ✅ Yes | ❌ No |
| `identity-authorization-service` | `E-ID` | ✅ Yes | ❌ No |

**Finding F4 — the reassuring half.** Nothing built is outside R0. Every service maps to an epic
the programme had already identified. There is no write-off, and no argument for parking any of
it.

**Finding F4 — the expensive half.** The right-hand column is empty. Not one service, story, or
test in this repository cites a `BR-` or PRD requirement ID. The engineering backlog runs on
`FUNC-001…009` / `TECH-` / `COMP-` identifiers that were minted inside the module SSOT and were
never derived from a business requirement. The chain is broken at its first link:

```text
WHAT WE HAVE     FUNC-001 … FUNC-009  →  code  →  tests  →  coverage gate
WHAT WE NEED     BR-xxx  →  PRD story  →  AC (Given/When/Then)  →  FUNC-xxx  →  code  →  test
                 └───────────────── this half does not exist ─────────────────┘
```

**Why this costs real money, not process points:**

1. **Compliance.** AU Bank distributes under IRDAI Composite Corporate Agency licence CA0515.
   When Compliance asks *"show me the requirement that mandated this consent behaviour and the
   test that proves it"*, the answer today is a code path, not a requirement.
2. **UAT sign-off.** Gate criterion 4.3 requires a bank caller to exercise the Term path. A
   caller cannot sign off against a specification that does not exist; they can only say "it did
   something".
3. **Change cost.** Without a requirement ID, no one can say whether a change is a defect, a
   variance, or new scope — which is exactly the argument that stalls a delivery for a week.

> **This is why the recovery is "retro-fit traceability", not "park and rebuild".** The code is
> probably right. We cannot currently prove it, and unprovable is where regulated programmes get
> stopped.

---

## 5. The gates that were skipped

Mapped against the canonical lifecycle in [03-LIFECYCLE.md §2](../../../governance/03-LIFECYCLE.md#2-canonical-lifecycle-l1):

| Stage | Should have produced | Actually produced | Verdict |
|-------|---------------------|-------------------|---------|
| **L0 Discovery** | Signed problem statement, capability map, stakeholder map, named sponsor | Problem statement on day 5; sponsor **still unnamed** (`GAP-010`) | ⚠️ Late & incomplete |
| **L1 Business design** | Journeys, business rules, **acceptance criteria** | Journey canvas; **no AC** (`GAP-008`); consent (`GAP-006`) and suitability (`GAP-007`) rule packs open | ❌ **Skipped** |
| **L2 Domain design** | Aggregates, state models, invariants, ubiquitous language | Canonical model exists **for the 1SB adapter only** — not for the platform's Lead / Consent / Suitability / Quote / Policy aggregates | ❌ **Skipped at platform level** |
| **L3 Technical design** | Contracts, boundaries, ADRs, NFR targets | 1SB architecture (day 1) and target architecture (day 2) — both **before** L1/L2 existed; NFR numbers still missing (`GAP-017`) | ⚠️ Inverted |
| **L4 Foundation** | Scaffold, CI, arch tests, secrets | Delivered and sound | ✅ Done |
| **L5 Connectivity** | Clients, auth, error normalisation, async infra | Delivered | ✅ Done |
| **L6 Vertical slice** | One journey end to end | Term quote→proposal path delivered | ✅ Done (adapter scope) |
| **L7 Hardening** | E2E evidence, compliance review, runbook, perf smoke | **In progress** — 5 of 7 gate criteria open | 🟠 Current |

**Three stages were skipped: L1, L2, and L3-in-order.** Everything downstream of them was built
well. That is precisely the situation the PO described — good engineering standing on an
unwritten foundation.

---

## 6. Finding F2 — we are building without the authorisation to build

From the PO's own [03-PROGRAMME-TODO.md](../03-PROGRAMME-TODO.md), Wave 0, verbatim:

> **Rule:** No delivery sprint commit until Wave 0 exit criteria met

Wave 0 exit criteria, and their state today:

| Wave 0 exit criterion | State |
|---|---|
| Sponsor sign-off on Working Decisions + R0 scope | ❌ Open |
| Compliance provisional OK on consent/suitability approach | ❌ Open |
| Architecture options workshop scheduled | ❌ Open |

Meanwhile [CURRENT-STATE.yaml](../../../governance/state/CURRENT-STATE.yaml) records WS-1 at
**Phase 4 — Hardening** with Phases 0–3 complete, and WS-2 at **Phase 1 — Foundation**.

**We are four delivery phases into a build that Wave 0 never authorised, against a rule the
programme wrote for itself.** This is the strongest single statement of the problem, and it
comes from our own document, not from a retrospective opinion.

---

## 7. Finding F3 — the product is not a tracked workstream

[CURRENT-STATE.yaml](../../../governance/state/CURRENT-STATE.yaml) declares exactly two
workstreams:

| ID | Name | Nature |
|----|------|--------|
| WS-1 | 1SB Insurance Integration | Engineering — one adapter |
| WS-2 | Workforce Authentication & Authorization | Engineering — one capability |

The AU Bank Insurance Distribution Platform — layer **L2** in the PO's own
[project view](../00-PO-PROJECT-VIEW.md#2-the-complete-project-three-layers), the thing the bank
is actually buying — **has no workstream, no lifecycle stage, no objective, no gate and no
scope list.**

The consequence is mechanical and it is currently working against us. Governance evaluates every
new input against a workstream ([03 §5, Rule LC-1](../../../governance/03-LIFECYCLE.md#5-multi-workstream-evaluation)),
and [02 §3](../../../governance/02-PROJECT_SCOPE.md#3-scope-fit-codes-l1-generic) states that an
input mapping to **no** workstream is *"SC2 at best"* — which forces a PARK, *"never ADMIT,
whatever the necessity"*.

> **Today, if anyone raises "write acceptance criteria for the lead-creation journey", the
> governance model is obliged to park it.** The framework is not broken; it is faithfully
> reflecting a programme that registered its adapters and forgot to register its product. Until
> a platform workstream exists, every corrective action in this pack is unadmittable.

**This is the first thing the war room must fix**, because nothing else in the proposal can be
admitted until it is.

---

## 8. What is *not* wrong

Stating this plainly matters — the room must not conclude that the last two weeks were wasted.

| Strength | Evidence |
|----------|----------|
| Engineering quality is high | Ports/adapters enforced by ArchUnit; no JPA/Flyway leakage; coverage gates; Docker packaging; OpenAPI published |
| Architectural boundaries are sound | Replaceable-middleware pattern means 1SB can be swapped without touching channel apps |
| The 1SB adapter is genuinely mature | Term vertical slice delivered end to end; 5 of 7 hardening criteria remain, all of them evidence work |
| Governance now exists and is enforced | AIGEM adopted 2026-08-07; `FreshnessCheck` runs green; drift is now detectable — this analysis was possible *because* of it |
| Business analysis depth is real | Knowledge base synthesises six client volumes and five phase documents; the gap register is honest about its own gaps |

**The programme does not have a competence problem. It has a sequencing problem.** Those are
repaired very differently, and confusing the two is how a realignment turns into a blame
exercise.

---

## 9. Cost of doing nothing

| Horizon | If we change nothing |
|---------|---------------------|
| **2 weeks** | WS-1 reaches the Phase 4 gate. Criterion 4.3 (bank caller UAT) stalls — no signed specification for a caller to accept against. Criterion 4.4 (compliance review) stalls for the same reason. |
| **6 weeks** | WS-2 identity services need business roles, entitlements and RM hierarchy that only `E-ID` requirements can define. Engineering invents them. They become the de-facto specification, and every later correction is a rework. |
| **3 months** | Lead, Consent, Suitability, Quote and Policy are built adapter-first — shaped by 1SB's model rather than the bank's canonical model. This directly contradicts D-003's "1SB is replaceable, not the product" and quietly makes the aggregator un-replaceable. |
| **At audit** | IRDAI CA0515 evidence request meets a repository with no requirement-to-test trace. This is the failure mode that is cheap today and very expensive in month four. |

The third row is the one that should worry the room most: **the longer the build runs ahead of
the business design, the more the bank's product converges on the aggregator's product.** That
is the strategic loss, and it happens silently.

---

## 10. Conclusion

1. **F1 confirmed** — the build ran ahead of the requirements; the history is unambiguous.
2. **Correction** — BRD/PRD documents exist but are skeletal, unsigned, and carry no acceptance
   criteria. Say it this way, not "we have none".
3. **F2** — we are four phases into a build that our own Wave 0 rule did not authorise.
4. **F3** — the platform is not a tracked workstream, so corrective work is currently
   unadmittable. Fix this first.
5. **F4** — nothing built is out of scope; what is missing is traceability. Retro-fit it; do not
   park working services.
6. **Recovery is cheap now.** Five services, three weeks of history, one adapter at hardening.
   The same repair in month four costs an order of magnitude more.

→ The proposed response: [02-REALIGNMENT-PROPOSAL.md](./02-REALIGNMENT-PROPOSAL.md)
