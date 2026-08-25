# CR-012 — R0 platform robustness: admit five deferred infrastructure layers

**Date:** 2026-08-24
**Type:** ARCH (with INFRA, SEC and COMP consequences)
**Raised by:** Repository owner → Mahesh — Architecture owner (Board 1)
**Workstream:** WS-3 — AU Bank Insurance Distribution Platform
**Stage:** S08 — Engineering Foundation, with S09 overlapped
**Decision:** **PENDING RATIFICATION.** Nothing in this CR, and nothing in `ADR-009` … `ADR-013`,
is an approval to provision. See §8.
**Suggestions:** `SUG-20260824-gp1` … `SUG-20260824-gp5`
**Decisions:** [`ADR-009` … `ADR-013`](../../platform/architecture-review/08-architecture-decision-log.md)

---

## 1. Current position

R0's platform design deferred five infrastructure layers. Each deferral was individually defensible
and four of the five were recorded with reasoning. Read together against an existing AU Bank
production estate (`prod-ibmb`), they form a pattern: **the R0 estate was designed for a small,
correctness-constrained workload and was thin in exactly the places that are invasive to retrofit.**

| Layer | Position before this CR | Where it was recorded |
|---|---|---|
| Bank connectivity (CBS, Bank AD) | "Direct Connect / VPN / bank proxy — decided at S09 entry", stubs permitted in `dev` | `R0-LLD` §14 open decisions |
| Egress inspection | Security groups + Kubernetes `NetworkPolicy` only. "Service mesh — NetworkPolicy is enough" | `r0_platform_views.py`, `R0-LLD` §2 |
| Shared cache tier | Not in the BOM. In-process caches only; sessions "prefer DynamoDB" | `R0-LLD` §1.3, §6 |
| Event backbone | `DO NOT PROVISION`. Transactional outbox is the async mechanism | `R0-LLD` §1.3, `03-solution-architecture-r0` §5.1 |
| Operational search | CloudWatch Logs only; analytics deferred to S13 | `R0-LLD` BOM #17 |

Three of the five had a real defect behind them rather than merely a gap:

1. **An unowned decision on the critical path.** The connectivity pattern was deferred to "S09
   entry" with no date, which left a carrier order and a bank firewall change — the two longest-lead
   items on the programme — behind a decision nobody had been asked to take, while a `dev` stub
   quietly became the only tested path to CBS. WS-1 gate 4.3 is the working example of how that
   ends.
2. **A published contradiction.** WS-2's accepted design specifies a Redis session vault and ships a
   Redis container in `docker-compose.identity.yml`; `R0-LLD` preferred DynamoDB and treated
   ElastiCache as an exception to avoid. Two designs, two session stores, one of which was going to
   be rewritten.
3. **A revisit trigger that fires inside R0.** `03-solution-architecture-r0` §5.1 deferred the event
   backbone until "a third distinct consumer class". R0's design already has three — audit,
   notification and compensation — so the trigger fires during the vertical slice, which is the
   worst moment to change the audit path.

The other two are gaps rather than defects, and are argued as such: unrestricted 443 egress from a
platform holding PAN, income and health attributes had no control and no log on it; and the logs the
other closures generate are not queryable by anything.

## 2. Proposed change

Admit all five into R0, as **infrastructure beneath an unchanged R0 slice**.

| ADR | Change | Shape |
|---|---|---|
| `ADR-009` | Hybrid bank connectivity | Transit Gateway in a new `network` account, per-environment route tables; Site-to-Site VPN **first**, Direct Connect primary when the circuit is accepted; `uat`/`prod` may not use stubs |
| `ADR-010` | Centralised egress inspection | Inspection VPC per environment with AWS Network Firewall; domain allowlist, drop-by-default, IPS alert→drop before prod; the allowlisted **Elastic IPs move** behind it |
| `ADR-011` | Managed cache tier | ElastiCache for Valkey: BFF sessions, L2 read-through behind the in-process L1, per-principal rate limits. **Refuses** idempotency |
| `ADR-012` | Event backbone | Amazon MSK, 3 brokers, SASL/IAM per topic, Glue Schema Registry — **fed by the transactional outbox, which stays the source of truth** |
| `ADR-013` | Operational search | VPC-only Amazon OpenSearch domain, 30 d hot → delete at 90 d. **Holds no regulatory evidence** |

**Also changed, as consequences rather than additions:** a sixth AWS account (`network`); KEDA
becomes permitted for consumer-lag scaling only; the DynamoDB `sessions` table is withdrawn; seams
`S-23` … `S-26`; fitness functions `FF-22` … `FF-28`; NFRs `NFR-NET-*`, `NFR-CAC-*`, `NFR-EVT-*`,
`NFR-OBS-*`; trust boundary `TB-7`; DR rows `D13` … `D16`.

## 3. What this CR deliberately does not admit

The same comparison surfaced three more layers. Admitting five is not a reason to admit eight.

| Not admitted | Why |
|---|---|
| **Service mesh (Istio / App Mesh)** | An Envoy sidecar per pod for mTLS and retries that `NetworkPolicy` + IRSA + Resilience4j already provide at this scale. `ADR-010` covers **egress** inspection and does not pretend to be a mesh. S14 conversation |
| **Per-service database clusters** | `ADR-008` stands. One cluster, schema per context, first split along the LOB-cell seam |
| **Analytics warehouse (Glue ETL, Athena, Redshift, QuickSight)** | `#18` Reporting & MIS is S13. `ADR-013` provisions operational search, which is a different thing. The Glue **Schema Registry** is in; Glue ETL is not |
| **ElastiCache as an idempotency store** | Named in the target-state review and **rejected by name** in `ADR-011`. Idempotency must be atomic with the business write |
| **MSK Replicator to `ap-south-2`** | The outbox is in Aurora and Aurora is replicated, so events replay. A broker replica copies something already recoverable (`D14`) |
| **Third-party NGFW appliance** | What the existing AU estate runs, and a defensible answer. Rejected for R0 on operational surface, not capability — and named as `ADR-010`'s revisit trigger if the bank's network standard requires it |

## 4. Driver

Direct instruction from the repository owner, after a comparison of the R0 design against the
existing AU Bank estate:

> *"I would still at R0 as well, I don't want those gaps — let's fill those gaps and make R0 more
> robust, and make sure we incorporate all the changes in all files wherever required so there is no
> inconsistency."*

The architectural driver is narrower than "robustness", and worth stating precisely: **four of the
five are decisions that get cheaper to take now and materially more expensive to take later.**
Routing, egress addressing, the session/cache port and the publish contract are all currently
unwritten. After W0b and W4 they are, respectively, every route table, every external allowlist,
the two things every request touches, and the one component that must not lose a record.

## 5. Scope-fit and stage-fit assessment

**Stage fit `SF1` — on-stage.** S09 is the Platform & Environment Foundation stage and these are
platform-foundation layers. `ADR-009` is arguably `SF0`: W1's `#4` Customer cannot be evidenced
without it, so it is a prerequisite of already-admitted work.

**Scope fit `SC0` — explicit.** WS-3's `current_scope.in_scope` names "S09 platform foundation".
Nothing in WS-3's `out_of_scope` is contradicted: that list is about journeys, LOBs, channels,
actors and bounded contexts, and this CR adds none of those. **No bounded context, no service, no
journey step, no gate criterion and no actor changes.** The service count stays at fourteen plus one
app.

**What this CR does change that is not infrastructure:** the cost envelope (§6), the S09 critical
path (a longer P1 network band), and the number of approvals outstanding.

## 6. Cost and operational consequence — stated, not minimised

This set adds three stateful managed services, a sixth account, an inspection VPC per environment
and two carrier circuits to a platform carrying **~100 journey starts per hour** (CAP-A). The fixed
cost of the estate now dominates its variable cost.

| Recorded as | What it says |
|---|---|
| `RISK-012` | The layers raise R0 fixed cost above what the S09 budget line assumed. Envelope is `NFR-OPEN-6`, produced **before** first `apply` to `uat` |
| `RISK-014` | Operational surface outruns team maturity: three stateful tiers arrive while `GATE-S08` is still open. Mitigated by managed-only services, availability-shaped sizing, and a drill per tier |
| `RISK-013` | Bank-side work may not land, leaving `uat` on stubs |
| `RISK-015` | Invariant erosion under incident pressure — cache as evidence store, topic as audit record, index as source of truth. Mitigated by `FF-23` … `FF-28` rather than by convention |
| `R0-LLD` §1.4 | Per-environment shapes. `dev` is deliberately not production-shaped; building production three times is the cheapest way to make this set unaffordable |

**Nothing here is sized for throughput.** Three brokers is an AZ-availability floor carrying tens of
messages a minute; two cache nodes hold a working set that fits in a pod's heap; the search domain
duplicates a store CloudWatch already has. Each is justified by a failure mode or an evidence gap,
and every one of those justifications is written into its ADR's negatives rather than its positives.

## 7. State-file transcription required — human-owned, not done here

`CURRENT-STATE.yaml` scope text is human-owned (`04` §5), so this CR does **not** edit it. Two WS-1
rows read differently after this change and need Kalpana / R12 to transcribe, with Rajal's
confirmation:

```yaml
# WS-1 current_scope.out_of_scope — proposed wording
- item: "Kafka / event backbone in the 1SB adapter"
  revisit_at: "Phase 5 — the WS-3 platform backbone exists from R0 (ADR-012), but the adapter
               neither publishes to it nor consumes from it in Phase 4"
- item: "Redis idempotency / multi-instance job ownership"
  revisit_at: "Phase 5.4 — unchanged. ADR-011 provisions a platform cache tier and explicitly
               refuses to hold idempotency in it, so TD-010 is not closed by it"
```

Both statements remain **true today**; the transcription only removes the appearance of a
contradiction between WS-1's deferral and WS-3's estate. `id_allocation` counters were advanced in
the same change as the new IDs — an ID-allocation correction only, touching no stage, scope or gate
field, exactly as the file's own ADR-counter note describes.

## 8. Boards and the approvals that are not notifications

**Every verdict below is a draft.** Drafts are in [`CR-012/verdicts/`](./CR-012/verdicts/README.md).
An agent may assemble evidence and draft reasoning; it may not sign, and it may not accept residual
risk on a named human's behalf.

| Board / role | Person | What they decide here | State |
|---|---|---|---|
| Board 1 — Architecture | Mahesh | Is this the R0 estate, and only the R0 estate? **Human T4** | `AI-DRAFTED` |
| Board 4 — Security | Deepali | **Accepts `ADR-010`** as a control, not reviews it. Owns `SEC-OPEN-7` (IPS alert mode until prod) and `SEC-OPEN-8` (no TLS inspection on the 1SB mTLS session). Plus `TB-7`, the session vault, per-topic IAM | **Required, outstanding** |
| Board 6 — Compliance & Risk | Shailja | The two evidence exclusions: no evidence exists only in a topic (`ADR-012`), OpenSearch holds none (`ADR-013`). Both are licence positions | **Required, outstanding** |
| Board 7 — SRE / Operations | Shivanshi | Owns four of the five layers operationally: circuits, firewall rule set, broker, search domain, and the on-call surface in `RISK-014` | **Required, outstanding** |
| Database (specialist) | Aarti | Cache/store boundary, cache sizing and eviction, and the confirmation that no system of record moves | **Required, outstanding** |
| R12 — Delivery | Kalpana | `DEP-20260824-dx1` on the critical path, the `RISK-012` envelope, and the §7 transcription | **Required, outstanding** |
| R3 — Engineering | Amit | L1/L2 cache port and publisher/consumer shapes as shared libraries, written once | Notify |
| Board 3 — Product | Rajal | Nothing in the journey changes; the interest is the pilot cost envelope | Notify |
| Board 5 — QA | Swapnali | Seven new drills and seven new fitness functions land in the S09 evidence pack | Notify |

## 9. Rejection would look like this

Recorded so that "no" is as actionable as "yes". If this CR is rejected in whole:

- `R0-LLD` §1.3 regains the five `DO NOT PROVISION` rows and the BOM returns to 24 + 3.
- `ADR-009` … `ADR-013` become `REJECTED` with the reason, and are not re-proposed without new
  evidence (`14` §6).
- The three defects in §1 return and need answers on their own: the connectivity decision needs an
  owner and a date, the session-store contradiction needs one of the two workstreams corrected, and
  §5.1's revisit trigger needs re-stating so it does not fire mid-slice.

Partial rejection is coherent and is the most likely outcome. `ADR-013` (search) is the least
load-bearing — it is a `SHOULD`, its argument is operability, and dropping it changes no other
decision. `ADR-009` and `ADR-010` are the most: `ADR-010`'s inspection path runs over `ADR-009`'s
hub, so rejecting the hub rejects the inspection design with it.

---

**Prepared by:** Mahesh — Principal Insurance Platform Architect (Board 1), AI-drafted
**signature_status:** `AI-DRAFTED — no board verdict recorded; mandatory human T4 Architecture, Security and Risk & Compliance sign-offs outstanding`
