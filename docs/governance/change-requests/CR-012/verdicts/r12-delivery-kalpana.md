# R12 — Delivery Control · CR-012 · DRAFT

**Persona:** Kalpana — Delivery Head (`R12`)
**Authored by:** Architecture agent, **simulating** R12. This is not Kalpana's verdict.
**Change request:** [CR-012 — R0 platform robustness](../../CR-012-r0-platform-robustness.md)
**Date:** 2026-08-24
**Status:** `AI-DRAFTED — no Delivery signature. R12 may make a decision's timing binding; it may never convert missing evidence into approval.`

---

## 1. Decision requested

Whether the S09 critical path still holds with five layers added to it, and on what conditions. Plus
two items that are R12's by definition: the external dependency, and whether the cost envelope is
produced before it is committed.

## 2. What was reviewed

- [`R0-LLD.md`](../../../../architecture/R0-LLD.md) §12.1 (bands `P0`–`P8` and the wave-precondition table), §1.4, §2.2, §2.3
- `DEP-20260824-dx1`, `DEP-20260824-eip`, `DEP-20260824-cst`, `DEP-20260824-evd`
- `RISK-012`, `RISK-013`, `RISK-014`
- CR-012 §7 (the state-file transcription this CR leaves to R12)

## 3. Findings

**F-1 — the critical path gets longer at exactly one point, and it is the point that was already the
longest.** Band `P1` (network) absorbs the Transit Gateway, the inspection VPC, the firewall, the VPN
and the DX order. `P3` gains the cache and the broker; `P6` gains the search domain. The wave order
`W0b → W1 → W2 → W3 → W4` is unchanged, and no wave gains a new predecessor that was not already
implied.

**F-2 — the external dependency count goes from one to two parties on `P1`.** It was "publish the
Elastic IPs to 1SB and the PG". It is now that **plus** the bank network team terminating a VPN,
publishing prefixes, opening a firewall and accepting a DX order (`DEP-20260824-dx1`). This is the
single largest delivery consequence of the CR and the one I would report upward.

**F-3 — the sequencing choice inside `ADR-009` is a delivery decision made correctly by
Architecture.** VPN before Direct Connect means the bank path needs a firewall rule rather than a
carrier order, so no gate waits on a circuit. Had it been DX-only, `uat` would have been blocked
behind a procurement lead time with no fallback, and I would be rejecting this CR on that basis
alone.

**F-4 — the Elastic IP move is a re-work item, not a new item, and it has a deadline.** Any allowlist
conversation already started with 1SB or the PG must be re-based on the inspection-VPC addresses
(`DEP-20260824-eip`). The cost of missing it is not a delay in `P1` — it is W2 quotes and W3 payments
timing out in UAT with code that is finished, which is the most expensive shape of late.

**F-5 — the cost envelope is a `GATE-S09` entry condition that this CR makes materially harder.**
"Cloud account structure and budget approved" already exists as an S09 entry criterion. Three
stateful services, a sixth account, an inspection VPC per environment and two circuits change the
second half of it, and nobody has priced it (`NFR-OPEN-6`, `DEP-20260824-cst`).

**F-6 — the drills are on my plan, not only on Shivanshi's.** Seven new NFR verifications, three of
them wall-clock drills in the `P8` proof band. `P8` is the band that cannot be compressed: nothing in
it can be produced in the week the gate is reviewed. Adding to it without adding to the schedule is
how a gate slips at the last moment.

**F-7 — the audit-path ordering matters more than it looks (`DEP-20260824-evd`).** If `#16` Audit is
built against a direct outbox poll and moved to a consumer later, that is a rewrite of the component
that must not lose a record — during the money wave. Broker and topics have to exist before W1 emits
its first event, which is why they sit in `P3` and not in a later band.

**F-8 — one thing I am not empowered to trade.** `RISK-012` will produce pressure to lower a `prod`
shape. `R0-LLD` §1.4 and the Security verdict both say a `prod` control is not lowered for cost
without Security and SRE verdicts. Delivery urgency does not alter specialist authority, and this
verdict does not create an exception for a cost conversation.

## 4. Draft verdict

`APPROVE-WITH-MODIFICATION` — drafted, unsigned.

The plan absorbs this if two dates are set this week. It does not absorb it if the bank engagement
and the envelope are treated as S09 activities to start once S09 starts.

## 5. Conditions (drafted)

1. **`DEP-20260824-dx1` gets a bank-side contact and a first response date by 2026-08-28**, and is
   reported at every Governance Sync until the VPN half is confirmed. It is the one item on the
   programme that cannot be accelerated by working harder, and it is now a `RISK-013` trigger.
2. **The Elastic IP list is re-published and confirmed in writing before any UAT date is
   communicated** (`NFR-NET-03`). A UAT date announced against a stale allowlist is a date that will
   move.
3. **`NFR-OPEN-6` — the cost envelope — is produced before the first `apply` to `uat`**, not before
   the gate review. A budget discovered at the gate is a re-plan, not a finding.
4. **The three drills are on the S09 plan with dates before the build starts**, not appended to the
   `P8` band as intent.
5. **CR-012 §7's transcription is mine to execute** with Rajal's confirmation: WS-1's `out_of_scope`
   wording for the broker and for Redis idempotency. Both remain true; only the appearance of a
   contradiction is being removed, and no other scope, stage or gate field is touched.
6. **No wave starts early to absorb the added `P1` duration.** `W0b`–`W4` still begin after
   `GATE-S08` and the S09 critical path. Compressing the foundation to protect a wave date is how the
   foundation ends up half-built and the gate ends up unevidenced.
7. **This CR is reported as a scope *increase in the platform*, not as hardening.** Five layers with
   a permanent run-rate arrived; recording it as "robustness" would understate it to the people who
   have to fund it.

## 6. What this draft may not do

It may not approve the envelope, commit the bank's schedule, or set a UAT date. It may not accept
`RISK-012`, `RISK-013` or `RISK-014`. It may not convert Security, Compliance, SRE or Database
outstanding approvals into "proceed at risk" — R12 may make a decision's timing binding, never its
content.
