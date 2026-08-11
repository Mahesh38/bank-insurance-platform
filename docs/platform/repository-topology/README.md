# Repository Topology — monorepo → federated multi-repo

**Parent:** [`docs/platform/README.md`](../README.md)
**Status:** ⚠️ **Proposal under change control — `CR-002` is PENDING.**
Nothing in this folder is approved, and no repository may be created until `CR-002` is decided by
the PO + Architect ([14-CHANGE_CONTROL §1](../../governance/14-CHANGE_CONTROL.md)).

---

## Why this folder exists

The Solution Architect asked for a migration plan to split this monorepo into one repository per
microservice, with a **single parent repository** holding the documentation and governance that
are common across services — so every service repo, and every AI agent working in one, reads the
same rules from the same place.

That is a platform-topology change. It is outside the current scope of both workstreams, it
changes where `docs/governance/**` lives, and it touches the build, CI, and coverage gates that
the WS-1 Phase 4 exit criteria depend on. Under AIGEM, an agent may **raise** that change and
must produce its impact analysis; it may never approve it
([Rule CC-1](../../governance/14-CHANGE_CONTROL.md)).

So this folder is the impact analysis required by
[14-CHANGE_CONTROL §3 step 2](../../governance/14-CHANGE_CONTROL.md) — the plan approvers need in
order to say yes or no, written as if the change were approved.

---

## Contents

| Document | What it answers |
|----------|-----------------|
| **[01-TARGET-TOPOLOGY.md](./01-TARGET-TOPOLOGY.md)** | Which repositories exist, what each owns, who owns it, and why the shared libraries are *one* repo rather than five |
| **[02-GOVERNANCE-FEDERATION.md](./02-GOVERNANCE-FEDERATION.md)** | How one parent repo serves documentation + governance to every service repo, what stays central vs. local, and the exact agent boot sequence |
| **[03-MIGRATION-PLAN.md](./03-MIGRATION-PLAN.md)** | The waves, their entry/exit conditions, what breaks and must be built first, rollback per wave, and the risks |
| **[04-DRAFT-ADR-ARCH-023.md](./04-DRAFT-ADR-ARCH-023.md)** | The draft architecture decision. Enters [08-architecture-decision-log.md](../architecture-review/08-architecture-decision-log.md) as `ARCH-023` **only if `CR-002` is approved** |

---

## The three things an approver should decide

1. **Do we split at all?** The plan argues *yes eventually, no today* — see
   [03 §1](./03-MIGRATION-PLAN.md#1-the-recommendation-in-one-paragraph).
2. **If yes, when?** The plan proposes the WS-1 Phase 4 gate as the earliest safe start, because
   three of that gate's open criteria (4.1 CI E2E, 4.2 OpenAPI publication, 4.7 coverage gates)
   run through the build the split would rewrite.
3. **Central or copied governance?** The plan proposes **one parent, pinned by tag, consumed as a
   submodule** rather than the copy-per-repo model in
   [19-PORTING_GUIDE §2](../../governance/19-PORTING_GUIDE.md). That choice is the difference
   between one governance state and N drifting ones — see
   [02 §2](./02-GOVERNANCE-FEDERATION.md#2-what-is-common-and-what-is-not).

---

## Traceability

| Record | Where |
|--------|-------|
| Triage record | [`SUG-20260811-r7k`](../../governance/registers/SUGGESTION-REGISTER.md) |
| Change request | [`CR-002`](../../governance/registers/DECISION-REGISTER.md) |
| Draft decision | [`ARCH-023` (draft)](./04-DRAFT-ADR-ARCH-023.md) |
