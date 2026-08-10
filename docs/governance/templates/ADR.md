# Template — Architecture Decision Record

Index new ADRs in [../registers/DECISION-REGISTER.md](../registers/DECISION-REGISTER.md) and,
for this repository, in
[architecture-review/08-architecture-decision-log.md](../../architecture-review/08-architecture-decision-log.md).

An ADR is required when: a boundary, contract, or topology changes · a runtime component is
added · a standing constraint changes · a debt item becomes permanent
([15 §4](../15-TECH_DEBT_POLICY.md#4-expiry-enforcement)) · a review-board conflict is resolved.

---

```markdown
# ADR-0XX — <decision in one line>

**Status:** Proposed | Accepted | Superseded by ADR-0YY | Deprecated
**Date:** YYYY-MM-DD
**Deciders:** Architect, Tech Lead, <others>
**Workstream:** WS-1 | WS-2 | platform
**Stage:** the lifecycle stage at which this was decided
**Origin:** SUG-#### / PLAN-### / CR-### / board escalation

## Context

What forces this decision now. Include the constraints that are real (regulatory, contractual,
existing architecture) and the ones that are merely current (team size, present load) — the
distinction determines how durable the decision is.

State what is *not* known. Decisions made under uncertainty should say so, so a later reader
can tell whether new information invalidates them.

## Decision

The decision, in the active voice: "We will …".

Be specific about scope: what it applies to, and what it explicitly does not.

## Alternatives considered

| Option | Why not |
|--------|---------|
| … | … |

Include "do nothing" — if it was never a real option, say why.

## Consequences

**Positive**
- …

**Negative / accepted costs**
- …

**Constrains future work**
- Which future proposals this decision now makes SF4/REJECT
- Which parked items it unblocks or invalidates

## Reversibility

| Question | Answer |
|----------|--------|
| Cost to reverse | low / medium / high |
| What makes it expensive | data written, contracts published, consumers migrated |
| Point of no return | the event after which reversal is impractical |

## Revalidation triggers

Conditions under which this decision should be revisited
([16 §7](../16-DECISION_MODEL.md#7-revalidation-triggers)):

- …

## Compliance and security impact

- Regulatory obligations touched:
- Security posture change:
- Audit or attribution implications:
```

---

## Notes

| Rule | Detail |
|------|--------|
| One decision per ADR | Bundled ADRs cannot be superseded independently |
| ADRs are immutable once Accepted | Change means a **new** ADR that supersedes this one — the history is the value |
| Every ADR names what it forbids | This is what makes it usable during triage: "ADR-004 forbids this" ends an argument |
| A deferral is a decision | "We will defer the production IdP choice behind an adapter" is an ADR, not an absence of one |
