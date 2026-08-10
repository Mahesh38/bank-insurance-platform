# Parked Backlog

**Parked is not deleted.** Every entry names the stage that unparks it and the observable
trigger that fires. Items without both are invalid ([03 §3](../03-LIFECYCLE.md#sf3-carries-three-mandatory-fields)).

**Owner:** Delivery Lead (sweeps) · Tech Lead (technical items)
**Swept at:** every stage gate, every approved scope change, and on the aging rules in
[05 §7](../05-PRIORITY_MODEL.md#7-anti-starvation)

---

## 1. Parked — scheduled work

Real work, wrong stage. Each returns to **full re-triage** at its trigger — never auto-admitted
([08 §5](../08-BACKLOG_RULES.md#5-unparking)).

| ID | Item | WS | Parked at | Target stage | Unpark trigger | Future necessity | P now / target | Parked because |
|----|------|----|-----------|--------------|----------------|------------------|----------------|----------------|
| TD-022 | FUNC-008 payment intimation | WS-1 | Phase 4 | Phase 5.3 | Phase 4 gate PASSED | MUST | P4 / P2 | Term path closed without intimation; port stubbed |
| TD-010 | Redis idempotency / cache adapter | WS-1 | Phase 2 | Phase 5.4 | Before horizontal scale-out | MUST | P4 / P2 | In-memory is correct for single-instance UAT |
| TD-014 | WireMock / full E2E for integration ↔ persistence | WS-1 | Phase 1 | Phase 4 | **Now eligible** — overlaps gate criterion 4.1 | MUST | P2 / P2 | ⚠️ Trigger has fired — sweep at next triage |
| TD-009 | Missing domain ports (Proposal/Status/Master/Audit/Idempotency) | WS-1 | Phase 1 | Phase 5 | Second LOB requires the abstraction | SHOULD | P4 / P3 | Ports without a second implementation fail test X2 |
| TD-006 | AWS Secrets Manager provider is a stub | WS-1 | Phase 1 | Phase 6 | First non-local deployment using AWS secrets | MUST | P4 / P1 | Prod profile fails fast today; no AWS target yet |
| TD-023 | Raw payload capture for status / master-data calls | WS-1 | Phase 4 | Phase 5 | Compliance review outcome (gate 4.4) | SHOULD | P4 / P2 | COMP-003 covered the quote/proposal/payment paths |
| TD-007 | ArchUnit `allowEmptyShould(true)` | WS-1 | Phase 1 | Phase 5 | Packages populated by LOB expansion | SHOULD | P5 / P3 | Rules cannot tighten against empty packages |
| E12 | Saving / Annuity / Pension LOBs | WS-1 | Backlog | Phase 6+ | Term + Health + Motor stable in production | SHOULD | P5 / P3 | P2 backlog by PO decision |
| E13 | Replaceability proof (fake adapter / routing flag) | WS-1 | Backlog | Phase 6+ | Post-GA | COULD | P5 / P4 | Architecture is proven by ArchUnit today |

> ⚠️ **TD-014's trigger has fired.** It is listed here for the record; the next gate sweep
> should promote it into the Phase 4 backlog alongside criterion 4.1, or re-park it with a
> reason.

## 2. Parked — stage-deferred by nature

Work every platform needs, deliberately scheduled to Production Readiness. Listed so agents
recognise them as *already decided*, not as gaps to re-report.

| Item | WS | Target stage | Unpark trigger | Future necessity | P now / target |
|------|----|--------------|----------------|------------------|----------------|
| Dashboards, alerts (auth failure, poll timeout, upstream 5xx, p95) | WS-1 | Phase 6.2 | Phase 5 gate PASSED | MUST | P4 / P1 |
| Retention job for raw payloads; backup/restore verification | WS-1 | Phase 6.3 | Phase 5 gate PASSED | MUST | P4 / P1 |
| Disaster-recovery testing | WS-1 | Phase 6 | Production readiness entry | MUST | P4 / P1 |
| Production autoscaling configuration | WS-1 | Phase 6 | Production readiness entry | MUST | P4 / P1 |
| Prod credentials, IP whitelist, TLS egress verification | WS-1 | Phase 6.1 | Phase 5 gate PASSED | MUST | P4 / P1 |
| Hypercare: error budget, escalation contacts, rollback plan | WS-1 | Phase 6.5 | Go-live checklist opened | MUST | P4 / P1 |
| Bank AD federation (OIDC / SAML / LDAP specifics) | WS-2 | Phase 2 | AD technology confirmed by the bank | MUST | P4 / P1 |
| Production IdP selection | WS-2 | Phase 2 | Phase 1 gate PASSED | MUST | P4 / P1 |
| Retail-customer identity | WS-2 | Phase 3 | Business decision to open the context | MUST | P5 / P1 |

## 3. Ideas — no committed stage

Outside scope (SC2), plausible value, nothing depends on them. Reviewed at gate sweeps; closed
as `LAPSED` after three gates (AS-3).

| ID | Idea | Raised | Why not now | Revisit if |
|----|------|--------|-------------|------------|
| → [SUG-20260810-k2m](./SUGGESTION-REGISTER.md#3-detail-records) | **"Role-contract v1"** — replace prose personas with a compact structured contract (purpose, owns / does not own, decision authority, required sources, outputs, verification, handoff triggers, escalation) | 2026-08-10 | SC2: no named beneficiary. The four board personas admitted as [SUG-20260810-b4d](./SUGGESTION-REGISTER.md#3-detail-records) can be written in the existing format, so the format overhaul is separable — and a format's cost only bites at scale. SF3 at L7: restructuring non-binding context is not hardening work | A **sixth** persona is proposed, or two reviewers disagree on where a role's authority ends |
| → [SUG-20260810-p9q](./SUGGESTION-REGISTER.md#3-detail-records) | **Persona evals** — 8–15 scored prompts per persona (correct response, refusal/escalation, cross-role handoff, stale-context detection, ambiguous authorization, adversarial instruction) | 2026-08-10 | SC2: an eval harness for non-binding documents, proposed during hardening. This is precisely the instinct [RUNBOOK §8.3](../RUNBOOK.md#83-how-the-agents-thinking-must-change-at-each-stage) names for L7 — "while hardening, let me also add…". X9 fails: no verdict-quality problem has been observed, only hypothesised | A board verdict is measurably wrong or inconsistent (E3), or agentic-roadmap Phase 4 opens and personas start driving automated decisions |
| → [SUG-20260810-z8n](./SUGGESTION-REGISTER.md#3-detail-records) | **Personas for Team Lead, Java Developer, Agent/ML Engineer** | 2026-08-10 | SC2: these three of the assessment's seven titles map to **no review board** ([11 §1](../11-REVIEW_GATES.md#1-the-board)), so they have no named consumer — X1 fails. The four that do map to a board were admitted separately | Team Lead / Java Developer: a second human joins delivery and role handoffs become real. Agent/ML Engineer: agentic-roadmap Phase 4 becomes active |

## 4. Sweep log

| Date | Gate / trigger | Items swept | Promoted | Re-parked | Closed |
|------|----------------|-------------|----------|-----------|--------|
| 2026-08-07 | AIGEM adoption — initial seeding | 9 + 9 | 0 | — | 0 |

---

## 5. Sweep procedure

At every trigger, for each candidate:

```text
1. Re-run pipeline steps 2–7 against the CURRENT state — do not auto-admit.
2. Outcome:
     still SF3     → re-park with a NEW target stage and a stated reason
     now SF0/SF1   → ADMIT: score fresh, plan, review, route to a backlog
     now SF4/SC3   → close as SUPERSEDED or WONT-DO, with the reason
3. Record the outcome in §4 and update the item's row.
```

An item re-parked **twice** is force-reviewed by the PO: either it is genuinely a later-stage
must, or it is an idea wearing a schedule.
