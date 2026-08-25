# Board 4 — Security · CR-012 · DRAFT

**Persona:** Deepali — Principal Security Architect (Board 4 / `R8`)
**Authored by:** Architecture agent, **simulating** Board 4. This is not Deepali's verdict.
**Change request:** [CR-012 — R0 platform robustness](../../CR-012-r0-platform-robustness.md)
**Date:** 2026-08-24
**Status:** `AI-DRAFTED — no Security signature. Board 4 holds a binding veto and two risk acceptances that only Deepali can make.`

---

## 1. Decision requested

Four distinct things, and they are not the same kind of ask. Conflating them is how a security
acceptance gets treated as a review comment.

| # | Ask | Kind |
|---|---|---|
| 1 | `ADR-010` — centralised egress inspection | **Acceptance of a control you own.** Not a review of someone else's design |
| 2 | `SEC-OPEN-7` — managed IPS rule groups run in **alert** mode, not drop, until production | **Residual-risk acceptance** |
| 3 | `SEC-OPEN-8` — the 1SB mTLS session is **not** TLS-inspected | **Residual-risk acceptance** |
| 4 | `TB-7`, the Valkey session vault, MSK per-topic IAM, the OpenSearch domain | Security review of new assets and one new boundary |

## 2. What was reviewed

- `ADR-009` … `ADR-013` in [`08-architecture-decision-log.md`](../../../../platform/architecture-review/08-architecture-decision-log.md)
- [`04-security-architecture.md`](../../../../platform/ws3-platform/04-security-architecture.md) — `TB-7`, the new TB-4 threat rows, `SEC-OPEN-7`/`8`, §9 event classes
- [`R0-LLD.md`](../../../../architecture/R0-LLD.md) §2.2, §2.3, §4.3, §6, §12
- `FF-22` … `FF-28` in [`03-solution-architecture-r0.md`](../../../../platform/ws3-platform/03-solution-architecture-r0.md) §7

## 3. Findings

**F-1 — the egress gap was real, and the closure is the right shape.** A pod that could reach a NAT
gateway could reach any host on 443, with no allowlist and no log. For a platform holding PAN, income
and health attributes and calling an aggregator, a payment gateway and an SMS gateway, that is the
shortest-description exfiltration path in the estate. Domain allowlisting with drop-by-default and
retained alert logs closes it. `NFR-NET-02`/`FF-22` make coverage checkable rather than claimed,
which is the part that matters: a control with one bypassed route table is not a control.

**F-2 — `TB-7` is the first boundary where traffic originates outside AWS, and its attack surface is
a routing table.** The per-environment Transit Gateway route tables are the control, and they are
asserted in the IaC scan. Two statements in the design need to stay stated because they are the
common errors: a private circuit is not encryption, and being inside the circuit is not an
authorisation. Both are written into `TB-7`.

**F-3 — the session vault is an authentication asset, not a cache.** Read access to it is read
access to live sessions, including the provider tokens the BFF holds so that Flutter never does. The
per-service ACL user with a key prefix is the right model and mirrors the schema-ownership rule in
`ADR-008`. **The token-hiding invariant is unaffected** — the tokens move from a per-service store to
a shared one, and the device still never receives them.

**F-4 — `ADR-011` refusing idempotency in the cache is a security-relevant refusal, not only a
correctness one.** A cache-backed idempotency record on the money path is a double-charge waiting for
an eviction. `FF-23` checks it, which is better than a paragraph.

**F-5 — the two new stores each create an aggregation target, and each is contained differently.**
MSK per-topic IAM prevents a consumer group reading a topic it was not granted; the DLQ topics need
scoping, or they become an unmanaged copy of every payload that ever failed. OpenSearch aggregates
operational data across every service, which is valuable to an attacker — VPC-only placement,
fine-grained access control and audited human access are the containment, and `FF-27`/`FF-28` are the
two checks that keep PII out of it and keep it away from the evidence store.

**F-6 — the closure gives Board 4 something it did not have: visibility.** Security architecture §9
already required denied-authorisation, attribution-rejection and device-isolation events to be
alertable. **Denied egress could not be an event class before this change**, because unrestricted
egress produces nothing to log. It is now one, and `ADR-013` is what makes it queryable.

## 4. Draft verdict

`APPROVE-WITH-MODIFICATION` — drafted, unsigned.

The direction is right and two of the five closures are straightforwardly security-positive. What
prevents an unconditional position is that this CR asks Board 4 to accept two interim postures, and
an interim posture with no expiry is a permanent one.

## 5. Conditions (drafted)

1. **`SEC-OPEN-7` carries a date, not a stage.** "Alert mode until prod" is acceptable only with a
   named review date and a named owner for the alert→drop transition. An IPS in alert mode is a log
   source, not a control, and the gap between the two must not be open-ended.
2. **`SEC-OPEN-8` is scoped to destinations, not to a pattern.** The TLS-inspection exemption applies
   to enumerated mTLS destinations (1SB today) recorded in the rule set. It must not be expressible
   as "mutually authenticated traffic is exempt", which is a general bypass with a technical-sounding
   name.
3. **The firewall allowlist has a named curator** and every change arrives as a pull request in the
   same change as the code that needs the destination. `ADR-010` says this; Board 4 requires it as a
   condition, because a rule set nobody owns decays to permit-any within two incidents.
4. **DLQ retention and access are specified before W3.** Per-topic IAM covers the topics; the DLQs
   need the same treatment and a retention window, or they become the copy of record for failures.
5. **Valkey AUTH credential rotation is exercised in the same `NFR-SEC-07` rotation drill** as the
   database and provider credentials. A session vault whose credential has never been rotated is the
   next `TD-006`.
6. **`FF-22` … `FF-28` are S09 gate evidence, not backlog items.** Five layers were admitted; each is
   bounded by a machine check. If the checks slip, the boundaries are prose and this verdict does not
   apply to that estate.
7. **No control is lowered in `prod` to fit the `RISK-012` cost conversation** without returning to
   this board. `R0-LLD` §1.4 already says it; Board 4 restates it because cost conversations are
   where controls quietly become optional.

## 6. What this draft may not do

It may not accept `SEC-OPEN-7` or `SEC-OPEN-8`. It may not sign S07-G3 or S07-G4. It may not treat
`ADR-010` as approved because it is well argued — the control is Deepali's to accept, and an
architecture agent writing a persuasive case for it does not change whose signature is required.
