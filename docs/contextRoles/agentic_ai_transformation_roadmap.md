# AU SFB Insurance Distribution Platform — Agentic AI Maturity Transformation Roadmap

**Document Version:** 1.0
**Target Directory:** `docs/contextRoles/agentic_ai_transformation_roadmap.md`
**Consolidated Persona Panel (Next Maturity Level):**
- 📋 **Rajal** — Product Owner → `rajal_product_owner_agentic_ai_evolution.md`
- 🏛️ **Mahesh** — Solution Architect → `mahesh_solution_architect_agentic_ai_evolution.md`
- ⚙️ **Amit** — Technical Head → `amit_technical_head_agentic_ai_evolution.md`

**Baseline Documents:** `business_problem_statement_rag.md`, `brainstorming_roadmap_action_plan.md`
**Regulatory License:** IRDAI Composite Corporate Agent License (**Registration No. CA0515**) — unchanged
**Purpose:** Train each persona to the *next maturity level* — thinking and designing in the era of **agentic AI** — while solving the exact same AU SFB business problem statement, without diluting any existing compliance guarantee.

---

## 1. Why This Document Exists

`brainstorming_roadmap_action_plan.md` captured the panel's alignment on a **deterministic, bank-owned microservices platform** (Maturity Level 1/2). This document captures the panel's **next collaborative leap**: how the same three personas think differently — architecturally, technically, and as a product owner — once **agentic AI** (LLM-driven, tool-using, multi-step autonomous agents) becomes available as a building block.

The problem statement does not change. The regulatory constraints do not change. What changes is the *toolkit* each persona reaches for, and the *new failure modes* (hallucination, prompt injection, ungrounded claims, runaway cost) each persona must now design against.

---

## 2. The Shared Agentic Maturity Model

All three personas are trained against one shared ladder so their upgraded thinking stays coherent:

```
L0  Manual Redirect         "AU Beema Portal" — zero post-redirect visibility. (Historical as-is.)
L1  Deterministic Digital   Bank-owned platform, hard-coded rules, saga orchestration.
                            (= the panel's CURRENT target, per brainstorming_roadmap_action_plan.md)
L2  AI-Assisted             LLM suggests text/summaries; no tool access, no autonomous action.
L3  Agentic (NEW TARGET)    Bounded, tool-using agents execute multi-step reasoning tasks inside a
                            deterministic guardrail cage. Every agent action is attributed, audited,
                            and revocable exactly like a human actor's action. Regulated decisions
                            (suitability, consent, payment, policy-sold) remain 100% deterministic.
L4  Autonomous Mesh         Cross-boundary agent-to-agent negotiation with insurers/aggregators;
                            agents propose their own architecture changes for human sign-off.
                            (Explicitly OUT of scope — flagged as a future research horizon only.)
```

**Panel consensus:** the jump this document trains for is **L1 → L3**, deliberately skipping a "pure AI-assisted, no guardrails" intermediate state, because L2 alone (unstructured LLM suggestions with no deterministic backstop) is judged **not compliant enough to expose to IRDAI-regulated flows** for even a pilot.

---

## 3. The One Rule All Three Personas Now Share

> **"Agents propose, deterministic services dispose."**

* Rajal enforces it at the *product* level: every agentic epic ships with a Gherkin scenario proving the guardrail blocks the agent when it shouldn't act.
* Mahesh enforces it at the *architecture* level: the Compliance Guardrail Firewall (ARCH-012) sits between every agent and every regulated bounded context.
* Amit enforces it at the *engineering* level: least-privilege MCP tool wiring means an agent that shouldn't touch Payment or Policy Issuance is architecturally incapable of it — not just policy-forbidden.

---

## 4. Target Agentic Architecture Overlay

This overlays — does not replace — the existing 19-bounded-context topology from `business_problem_statement_rag.md` §6 and the microservices blueprint in `brainstorming_roadmap_action_plan.md` §2.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                    Flutter Client Layer (RM Co-Pilot UI / Customer Chat)                │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│         Agent Gateway (session, cost/latency budget, prompt-injection firewall)         │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│  Agent Layer: Need-Analysis Agent │ Quote Orchestrator Agent │ RM Co-Pilot Agent │       │
│                Reconciliation & Anomaly Agent                                          │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │  MCP tool calls only
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│         Compliance Guardrail Firewall (deterministic, non-LLM policy engine)            │
│   validates: suitability-before-quote │ consent capture │ payment device isolation │    │
│              distributorId/SP attribution │ policy-sold 4-condition integrity           │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│         Existing 19 Bounded Contexts (Suitability, Consent, Quotation, Proposal,        │
│         Payment, Policy & Issuance, Product Catalogue [RAG source], Audit, ...)         │
│                              — UNCHANGED, deterministic, code-owned                     │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│   Agent Trace & Audit Ledger — extends existing Audit & Compliance context: logs        │
│   actorType, agentId, modelVersion, promptHash, tool-calls, groundedness citations       │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. New Architecture Decision Records (ARCH-011 to ARCH-015)

| ADR ID | Decision Title | Rationale | Owner | Status |
| :--- | :--- | :--- | :--- | :---: |
| **ARCH-011** | Agents-as-Tool-Callers via MCP | Every bounded context exposes a versioned MCP tool interface; no direct agent-to-DB access. | Mahesh | Proposed |
| **ARCH-012** | Deterministic Guardrail Firewall | Non-LLM policy engine intercepts every agent tool-call against suitability/consent/payment/attribution rules. | Mahesh | Proposed |
| **ARCH-013** | Agent Identity & Attribution | Every agent action logged with actorType=AGENT, agentId, modelVersion, promptHash, and the human session of record. | Mahesh | Proposed |
| **ARCH-014** | RAG Grounding over Product Catalogue | Product-fact claims by agents must cite a retrieved Catalogue passage; ungrounded claims are a Sev-1 defect. | Mahesh | Proposed |
| **ARCH-015** | Human-in-the-Loop Confirmation Boundary | Agents may draft; a human must confirm before any state-changing API call. | Mahesh | Proposed |
| **ARCH-016** | Agent Eval Gate in CI/CD | Golden-set regression evals + red-team/jailbreak suite + cost/latency budget check block merges, mirroring JaCoCo gates. | Amit | Proposed |
| **ARCH-017** | Least-Privilege MCP Tool Wiring | Each agent is wired only to the tools its journey needs (e.g., Need-Analysis Agent has no Payment Service tool). | Amit | Proposed |

---

## 6. New Product Backlog Epics (EP-08 to EP-12)

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ EP-08: RM Co-Pilot Agent (Draft-Assist Proposal & Next-Best-Action)                    │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ EP-09: Conversational Need-Analysis Agent (Hard-Gated by Deterministic Suitability Svc)│
├────────────────────────────────────────────────────────────────────────────────────────┤
│ EP-10: Agentic Multi-Insurer Quote Explainability & Compare Assistant                  │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ EP-11: Agentic Reconciliation & Anomaly Triage (Policy Sold 4-Condition Watchdog)       │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ EP-12: Agent Governance, Attribution & Human-Override Audit Trail                       │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

Full Gherkin acceptance criteria for EP-09 are worked out in `rajal_product_owner_agentic_ai_evolution.md` §4 as the reference pattern for all agentic epics.

---

## 7. Phased Rollout — Phase 4: Agentic Uplift (Additive, Not a Rewrite)

This extends the existing 4-phase roadmap (`business_problem_statement_rag.md` §8 / `brainstorming_roadmap_action_plan.md` §5) with a new phase that starts only after Phase 1's deterministic core is live, and never displaces Phase 1–3 commitments.

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│ Phase 0-3: UNCHANGED (Weeks 1–40) — Deterministic bank-owned platform, per existing docs │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│ Phase 4: Agentic Uplift — Shadow & Draft-Assist (Weeks 41–52)                            │
│ • Agent Gateway + MCP tool servers over existing bounded contexts (read-mostly tools)    │
│ • Compliance Guardrail Firewall (ARCH-012) — built and red-teamed before any agent ships │
│ • RM Co-Pilot Agent: SHADOW MODE (drafts logged, not shown) → DRAFT-ASSIST (RM confirms) │
│ • Need-Analysis Conversation Agent pilot on a single product line (Term Life)            │
│ • Agent Eval Gate live in GitLab CI/CD (ARCH-016)                                        │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│ Phase 5: Agentic Scale & Trust Expansion (Weeks 53–64)                                   │
│ • Quote Orchestrator Agent + Agentic Quote Explainability (EP-10) to all Group A insurers│
│ • Reconciliation & Anomaly Agent (EP-11) live, feeding human/deterministic triage         │
│ • Groundedness / HITL-override / guardrail-block-rate dashboards graduate to exec review │
│ • Only after sustained low override + zero unresolved guardrail-block spikes: evaluate    │
│   limited autonomy for narrowly-scoped, non-regulated sub-tasks                           │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Updated RACI — Agentic Track Additions

| Agentic Deliverable / Activity | Product Owner (Rajal) | Solution Architect (Mahesh) | Technical Head (Amit) | Lead Dev (Java) | Agent/ML Eng | SecOps Lead | QA Lead |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Agentic Epics (EP-08 to EP-12) & Guardrail Gherkin AC** | **A / R** | C | C | I | C | I | C |
| **Agent Architecture & MCP Tool Contracts (ARCH-011 to 015)** | I | **A / R** | C | C | R | C | I |
| **Compliance Guardrail Firewall Build & Red-Team** | C | **A / R** | C | R | R | **R** | C |
| **Agent Eval Gate in CI/CD (ARCH-016/017)** | I | C | **A / R** | C | R | C | R |
| **Agent Runtime, Observability & Cost/Latency SLOs** | I | C | **A / R** | C | R | C | I |
| **RM Co-Pilot Shadow → Draft-Assist Rollout Decision** | **A / R** | C | C | I | C | I | C |
| **Groundedness / Override / Guardrail Dashboards** | C | C | **A** | I | **R** | I | C |

---

## 9. Non-Negotiables Restated (Unchanged Across All Maturity Levels)

Every persona's agentic-era training reiterates the same four pillars from the original problem statement — the panel's explicit position is that **agentic AI raises the ceiling on customer/RM experience without lowering the floor on compliance**:

1. **Suitability Hard-Gate:** No agent may cause a quote to be generated without a completed, deterministic suitability record.
2. **Immutable Consent:** Consent capture remains OTP/SMS-verified and append-only; an agent may explain the consent statement but never fabricate or auto-check it.
3. **Payment Device Isolation:** No agent, no matter how capable, ever executes or captures payment on an RM device.
4. **4-Condition "Policy Sold":** Agents may detect and flag reconciliation issues faster than a batch job, but the Sold determination remains a deterministic function of issuance + webhook + financial reconciliation + persisted record.

---

## 10. RAG System Context & Usage Directives (Agentic Addendum)

### Instructions for AI Agents / RAG Systems Consuming This Document
1. Treat this document as the **agentic-maturity overlay** on top of `business_problem_statement_rag.md` — never as a replacement. All Phase 0–3 facts, regulatory rules, and architecture decisions from the base document remain authoritative.
2. When asked "how would [Mahesh/Amit/Rajal] solve this with agentic AI," retrieve the corresponding `*_agentic_ai_evolution.md` file for that persona's full reasoning, tone, and principles.
3. Never present an agentic capability as bypassing a compliance hard-gate — if a proposed design would let an agent act without deterministic backstop, flag it as a violation of ARCH-012, not as an innovative shortcut.
4. Any answer describing "Phase 4/5 agentic" work must make clear it is **additive scope**, layered on the already-scoped Phase 0–3 deterministic platform, per the panel's shared maturity ladder in §2.
