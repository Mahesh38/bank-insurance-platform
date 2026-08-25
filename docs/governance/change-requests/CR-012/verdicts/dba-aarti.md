# Database — specialist verdict · CR-012 · DRAFT

**Persona:** Aarti — Principal Insurance Data & Database Architect (specialist, via existing boards)
**Authored by:** Architecture agent, **simulating** the Database specialist. This is not Aarti's verdict.
**Change request:** [CR-012 — R0 platform robustness](../../CR-012-r0-platform-robustness.md)
**Date:** 2026-08-24
**Status:** `AI-DRAFTED — no Database signature. An architect's decision is not a DBA's sign-off (the ADR-008 formulation applies unchanged here).`

---

## 1. Decision requested

Three questions, all about boundaries rather than about products.

1. Does any **system of record** move? (`ADR-011` and `ADR-012` both say no.)
2. Is the **cache/store boundary** drawn correctly, and is the cache sizing sane at CAP-A volumes?
3. Does the event backbone change anything about **recovery** — the restore, the RTO, or what has to
   be replicated to `ap-south-2`?

## 2. What was reviewed

- `ADR-011`, `ADR-012` (and `ADR-013` for its exclusion), against `ADR-008` which this must not disturb
- [`R0-LLD.md`](../../../../architecture/R0-LLD.md) §5.1–5.3, §6.1, §6.2, §11, §11.1 (`D13`, `D14`), §12
- [`03-solution-architecture-r0.md`](../../../../platform/ws3-platform/03-solution-architecture-r0.md) §5.2 idempotency, §8 availability and recovery
- `NFR-CAC-01…03`, `NFR-EVT-01…03`, `NFR-THR-06`

## 3. Findings

**F-1 — no system of record moves, and the two places it could have are both refused explicitly.**
Idempotency stays in the owning service's store, written in the same transaction as the business
change (`ADR-011`). Evidence stays in the audit event store and the WORM archive, not in a topic
(`ADR-012`). Both refusals are machine-checked (`FF-23`, `FF-26`) rather than asserted, which is the
difference between a boundary and an intention.

**F-2 — the idempotency refusal is the correct call and worth being explicit about.** A cache cannot
participate in the database transaction. A cache-backed idempotency record on the money path fails
exactly when it matters: an eviction or a failover between the business write and the key write
produces a second authorised attempt against `INV-PAY-04`. The target-state review names ElastiCache
for idempotency; the target-state review is wrong about that, and this CR says so by name rather than
leaving it to be rediscovered.

**F-3 — `ADR-008` is undisturbed.** One Aurora cluster, schema per bounded context, per-context
credentials, no cross-schema grants. The new tiers sit beside it, and the cache ACL model mirrors the
schema ownership rule, which keeps one mental model instead of two.

**F-4 — the DynamoDB `sessions` table withdrawal is a net simplification.** One session store instead
of two candidate designs, and it resolves a contradiction between two published documents rather than
creating one.

**F-5 — the cache shape is right for the working set, and the scaling order is stated correctly.**
Cluster mode disabled, primary plus replica across two AZs. At ~100 journey starts an hour the
working set is thousands of session and configuration keys; sharding would add resharding complexity
to something that fits in one node's memory many times over. `ADR-011`'s recorded order — scale up
before out, and only on measured eviction or CPU — is the right default and is the sentence I would
otherwise have had to add.

**F-6 — recovery gets slightly simpler, not harder, and `D14` is why.** Because the outbox is a table
in Aurora, it is already inside the `NFR-DR-01`/`02` restore path. Events are therefore reproducible
by replay rather than needing broker replication, which keeps the DR footprint and the restore
procedure unchanged. That is a real argument, and it rests entirely on `ASM-011` — consumers being
idempotent and replay-tolerant — which is validated by drill (`NFR-EVT-03`), not by design.

**F-7 — one thing to watch that nobody has measured.** The outbox is now a write on **every**
business transaction and a hot read for the publisher: an append-heavy table with a moving read
frontier in the same cluster as the business schemas. At R0 volumes this is nothing. Its index and
vacuum behaviour still need to be designed rather than inherited, because it is the one table whose
access pattern differs from every other table in the cluster.

**F-8 — `NFR-THR-06` gains a consumer.** The Aurora connection budget (`Σ(pods × pool size) ≤ 60%`
of `max_connections`) must now include the publisher and consumer deployments, and KEDA can scale
consumers on lag. A lag spike that scales consumers into the connection ceiling is a database
incident caused by a messaging control — the interaction to check at S09 design review.

## 4. Draft verdict

`APPROVE-WITH-MODIFICATION` — drafted, unsigned.

The boundaries are right and the refusals are the valuable part. Conditions are about the outbox
table's physical design and the connection-budget interaction, neither of which is decided by an ADR.

## 5. Conditions (drafted)

1. **The outbox table's physical design is mine, before the first migration**: primary key, the
   partial or covering index the publisher polls, the archival or deletion policy for published rows,
   and its vacuum/autovacuum settings. It is the one table in the cluster with an append-and-drain
   access pattern.
2. **`NFR-THR-06` is recomputed with the publisher and consumers included**, at their KEDA maximum,
   not their steady state. If the ceiling binds, the KEDA maximum is what changes — not the pool size.
3. **Cache eviction policy, TTL discipline and maxmemory are recorded before `uat`**, with the L1/L2
   TTLs equal by design. An L2 whose TTL outlives the L1's is a stale-configuration source that looks
   like a caching bug.
4. **`ASM-011` is proven by the `NFR-EVT-03` replay drill before `GATE-S09` closes.** The entire
   "no broker in DR" position depends on it, and this is the one assumption in the set whose failure
   would be discovered at a failover.
5. **No business query is ever served from OpenSearch.** `ADR-013` already excludes it; recorded here
   because "we already index it, let us just query it" is the specific way an operational index
   becomes an accidental read replica of a system of record.
6. **`ADR-008` is not reopened by this CR.** One cluster remains the R0 topology, and the physical
   split still follows the LOB-cell seam when evidence justifies it. My approval of `ADR-008` remains
   separately outstanding.

## 6. What this draft may not do

It may not sign `ADR-008` or `ADR-011`. It may not set instance classes, `max_connections`, node
sizes or the Aurora-Global-versus-restore choice for DR — all of which remain Aarti's with Shivanshi.
