# CLAUDE.md

**[`AGENTS.md`](./AGENTS.md) is the agent entry point for this repository.** Read it, then follow
the routing below. This file exists so a Claude Code session lands in the same place as any other
agent — it adds no separate rules.

## Every session, in order

```bash
java scripts/governance/FreshnessCheck.java                        # 0 fresh · 1 warn · 2 do NOT admit new work
cat docs/context/BOOT.md                                           # tier 0 — the ten facts
python3 scripts/context/context-load.py resolve "<the request>"    # -> the exact files to read
```

Then read exactly what the capsule lists. `docs/` holds ~4.3 MB across 427 files; exploring it
costs more context than the work does, and the index already knows the answer.

## Four rules that override the instinct to be helpful immediately

1. **Triage before implementing.** A suggestion is never implemented in the turn it is raised —
   use the [`aigem-triage`](./.claude/skills/aigem-triage/SKILL.md) skill, record it, then return to
   the work item you were on and say so out loud.
2. **Load a persona card, not a persona package.** Cards are 3–6 KB and live in
   [`docs/context/personas/`](./docs/context/personas/README.md); packages reach 244 KB. Open a
   package file only when a card's *Load deeper* row matches.
3. **Never manufacture an approval.** Agents do not edit stage state, approve change requests, or
   satisfy a mandatory human T4 sign-off. Draft the reasoning and assemble the evidence instead.
4. **Cite what you read**, by path and anchor. An uncited conclusion is an assumption.

## Skills

| Skill | Use it for |
|---|---|
| [`aigem-triage`](./.claude/skills/aigem-triage/SKILL.md) | Any incoming requirement, bug, suggestion, review comment or scan finding |
| [`context-load`](./.claude/skills/context-load/SKILL.md) | Deciding what to read before doing anything else |

Everything else — the non-negotiable rules, the persona table, build and test commands — is in
[`AGENTS.md`](./AGENTS.md).
