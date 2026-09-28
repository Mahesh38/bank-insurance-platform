# Concept map — AIGEM 1.5 → FLOW

**K** keep (same substance) · **R** replace · **X** retire.

| AIGEM concept | Source | | FLOW | Where |
|---|---|---|---|---|
| "A suggestion is never implemented in the turn it is raised" | README §2 | R | Fold-in rule for tiny same-change items; everything else becomes a card; nothing lost | docs/03 §1 |
| 10-step pipeline, 60-second agent loop | README §5–6 | R | Four-question intake | docs/03 |
| Stage fit SF0–SF5, action matrix | 00 §6, 03 §3 | X | Guardrail vs checkpoint vs horizon test | docs/02 §2 |
| Scope fit SC0–SC4 | 02 | R | Outcome link + guardrails; SC4 (externally mandated) → Type-1 decision | docs/03, docs/05 |
| Necessity MUST/SHOULD/COULD/NOT-NOW | 16 §2 | R | Horizon + "not now because value / information / capacity" | docs/03 §2–3 |
| Evidence tiers E1–E7, confidence C1–C5 | 16 §4–5 | R | Low confidence → `Explore` card; evidence scaled by risk label | docs/04 §2, docs/06 §4 |
| Anti-over-engineering tests | 16 §6 | K | Architecture and engineering card checklists; "posture by maturity" | personas/cards, docs/07 §4 |
| Priority P1–P5, now and at target | 05 | R | Class of service + Next order | docs/04 §2 |
| Hard P1 overrides | 05 §3 | K | Expedite class | docs/04 §2 |
| Lifecycle L0–L10 | 03 §2 | R | Capability maturity M0–M5, reported not gating | docs/07 §2 |
| Lifecycle Bible S00–S15 | application-lifecycle-bible | R | Same maturity grid; Bible archived | docs/07 §2 |
| Workstreams WS-1/2/3 | CURRENT-STATE.yaml | R | Capability lanes | state/lanes.yaml |
| Stage gates, CANDIDATE/PASSED, freeze rule | 04 | R | Release checkpoints per capability path, per environment | docs/06 §5 |
| Stage-state edits human-only | 04 §5 | R | No stage state exists; approvals human-only remain | docs/05 §4 |
| Seven review boards, plan review | 11 | R | Risk labels R0/R1/R2 on PRs + parallel AI advisors | docs/06 §1–3 |
| T1–T4 tiers | 11 §3 | R | R0/R1/R2 | docs/06 §1 |
| G1–G10 change test (RG-5, RG-6) | 11 §3 | K | Defines R2 | docs/06 §2 |
| RG-9 evidenced-blocker relief | 11 | X | Unneeded: no tier inflation by subject matter to relieve | — |
| Board response clock (RG-7), approval expiry (RG-8) | 11 | R | Decision SLA + automatic escalation (DC-2) | docs/05 §3 |
| Definition of Ready / Done | 12, 13 | R | 4-line DoR; DoD by risk label | docs/06 §4 |
| Dependency model DEP-1..4 | 07 | R | Five dependency kinds, each with a standard unblock; only "genuinely hard" blocks | docs/04 §4 |
| Parked backlog + unpark triggers (BR-5) | 08 | R | `Later` + `review_by` + optional `pull_when` | docs/03 §2 |
| Six buckets / registers as markdown | 08 §2 | R | Tracker for work; YAML for decisions, dependencies, guardrails, outcomes | state/ |
| Suggestion register (279 KB) | registers | X | Archived read-only; no successor ledger | migration/artefact-disposition |
| Change control CR-### | 14 | R | Type-1 decision record; scope reorder is Type-2 by PO | docs/05, docs/04 §5 |
| Persona roster closed at nine (CC-2/CC-3) | 14 §1.1 | R | GOV-3: lenses added only if a card cannot absorb them | docs/09 §1 |
| Tech-debt policy with expiry | 15 | R | Enabler class with ≥ 20% allocation; debt ids listed per pack | docs/04 §2, context/packs |
| Drift control | 17 | R | Fold-in limits + one-card-per-session + "Captured as … Continuing …" | docs/03 §4 |
| Governance metrics, GM-1 gate criteria/week | 18 | R | Flow + quality + governance-cost metrics | docs/08 §2 |
| Porting guide L1/L2/L3 layers | 19, README §3 | R | Generic docs vs `state/` + `context/packs/` | repo layout |
| GC-1 governance competes for capacity | 00 §9 | K | Governance lane, Enabler allocation | docs/09 §3 |
| CURRENT-STATE.yaml + CS-1 halt | state/ | R | outcomes/lanes/decisions/dependencies YAML; staleness warns only | state/, context/README CX-5 |
| BOOT.md generated capsule | docs/context | R | AGENTS.md tier 0 (≤ 4 KB) | AGENTS.md |
| AGENT-CONTEXT-INDEX (41 ids), DOC-MAP | docs/context | R | Path-routed context-map + packs | context/ |
| Persona packages (40–316 KB) | docs/context/roles | R | Lens cards (≤ 2.5 KB) + knowledge in packs | personas/, context/packs |
| Persona authority matrix (40 KB) | PERSONA-AUTHORITY-MATRIX | R | One decision-rights table | personas/decision-rights.md |
| Bilateral cross-persona protocols (5) | roles/shared | R | One handoff shape + one conflict ladder | docs/05 §6–7 |
| PA-1 R12 forces timing not content | matrix §12 | K | Delivery decides timing only | personas/decision-rights.md |
| PA-2 named deadlock tie-breaker | matrix §16.1 | K | Conflict ladder step 4 | docs/05 §6 |
| Delivery Control System D0–D6 | DELIVERY-CONTROL-SYSTEM | R | Flow model + checkpoints; §11 parallelisation promoted to the default | docs/04 |
| Safe autopilot (`next`, `propose-transition`) | scripts/governance/autopilot.py | R | Flow steward "what can an agent pull now" | personas/cards/flow-steward.md |
| RUNBOOK role cards and ceremonies | RUNBOOK | R | Cadence table + persona cards | docs/08 §1 |
| Standing constraints / "never" lists | CURRENT-STATE.yaml | K | Guardrails | state/guardrails.yaml |
| "Out of scope now" lists | CURRENT-STATE.yaml | R | Split into guardrail / checkpoint / Later | docs/02 §2, state/outcomes.yaml |
| ADMIT-BYPASS | 09 §8 | R | Bypass counted as a governance-cost metric; > 10% → change the rule | docs/08 §2 |
