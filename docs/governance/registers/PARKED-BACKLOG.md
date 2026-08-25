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
| → [SUG-20260820-r1t](./SUGGESTION-REGISTER.md#2-register) | R0 → R1 → R2 transition and dependency map — the order in which North Star components must appear, and which are prerequisites for which | WS-3 | S08 | S13 | R0 completes a real pilot sale, or R1 planning starts | SHOULD | P4 / P2 | The North Star ([hdl.svg](../../hdl.svg)) shows the destination and what lands per release, but sequencing is a Delivery (R12) question that needs real R0 evidence. Drawing the order now would be guessing |
| → [SUG-20260821-jx2](./SUGGESTION-REGISTER.md#2-register) | Journey Execution Specification for the non-R0 surfaces — DIY / customer journey, hybrid mode switching, Group B insurers, ULIP and Savings, Health / Motor / Travel, renewals and servicing, admin UI, operations and reporting | WS-3 | S08 | S13 | R0 completes a real pilot sale, or R1 planning starts and Board 1 has ratified the target design for the surface being specified | SHOULD | P5 / P2 | Every one of these surfaces is in `out_of_scope_now`. There is no ratified design to specify against, so writing their request flows would manufacture architecture decisions rather than document them. The R0 pack ([SUG-20260821-jx1](./SUGGESTION-REGISTER.md#sug-20260821-jx1--r0-journey-execution-specification)) establishes the template each later surface reuses |
| TD-007 | ArchUnit `allowEmptyShould(true)` | WS-1 | Phase 1 | Phase 5 | Packages populated by LOB expansion | SHOULD | P5 / P3 | Rules cannot tighten against empty packages |
| E12 | Saving / Annuity / Pension LOBs | WS-1 | Backlog | Phase 6+ | Term + Health + Motor stable in production | SHOULD | P5 / P3 | P2 backlog by PO decision |
| E13 | Replaceability proof (fake adapter / routing flag) | WS-1 | Backlog | Phase 6+ | Post-GA | COULD | P5 / P4 | Architecture is proven by ArchUnit today |
| → [SUG-20260816-w3s](./SUGGESTION-REGISTER.md#2-register) | Extend `current-state.schema.json` so workstream relationships (`depends_on`, `entry_conditions`, `parent_workstream`, `delivers_bounded_contexts`) are validated instead of held in comments | WS-3 | S08 | S09 | A third relationship type is needed, or a validator must reason over the WS-3↔WS-1↔WS-2 topology | SHOULD | P3 / P2 | CR-010 registered WS-3 against `additionalProperties: false`; comments plus the authority documents carry the relationships correctly today |
| → [SUG-20260825-db1](./SUGGESTION-REGISTER.md#sug-20260825-db1--aarti-r0-physical-data-architecture-pack) apply | Copy the Aarti design DDL into per-service Flyway and run it on Aurora (new contexts + `14-audit_event_delta`) | WS-3 | S08 | S09 | S09-E01-S05 / S09-E03-S04 opened, and an owning service exists for the schema | MUST | P4 / P1 | Design is DATA-001; implementation is S09 per OPEN-I1. Applying now would be a migration without an owner |
| → [SUG-20260825-db1](./SUGGESTION-REGISTER.md#sug-20260825-db1--aarti-r0-physical-data-architecture-pack) restore | Prove Aurora PITR restore against the RPO 5 min / RTO 30 min design targets | WS-3 | S08 | S09 | S09-E06-S04 / S09-VT-07 started | MUST | P4 / P1 | A backup that has never been restored is a hypothesis. Design targets are in `02-operations-and-troubleshooting.md` |
| → [SUG-20260825-db1](./SUGGESTION-REGISTER.md#sug-20260825-db1--aarti-r0-physical-data-architecture-pack) purge | Implement `sp_retention_sweep` / `sp_purge_operational` as a scheduled job with a disposal audit row | WS-3 | S08 | S09 | S09-E06-S06 started | MUST | P4 / P1 | Routines are designed in `90-routines.sql`; running them now has no Object Lock and no job role |

> ⚠️ **TD-014's trigger has fired.** It is listed here for the record; the next gate sweep
> should promote it into the Phase 4 backlog alongside criterion 4.1, or re-park it with a
> reason.

> **2026-08-24 — what the R0 robustness round did and did not unpark.** `CR-012` admitted a
> platform cache tier (`ADR-011`) and an event backbone (`ADR-012`) into WS-3's R0 estate. Neither
> unparks a row above, and the distinction matters because it is the obvious mistake to make:
>
> - **`TD-010` / `SUG-0001` (Redis idempotency, WS-1) stay parked.** `ADR-011` explicitly refuses to
>   hold idempotency in the cache — the record must be written in the same transaction as the
>   business change (`INV-IDM-01`, `INV-PAY-04`). A shared cache existing does not make a
>   cache-backed idempotency store correct, and WS-1's remaining blocker is still the horizontal
>   scale-out decision (`DEP-006`), not the availability of infrastructure. `RISK-004` is unchanged.
> - **`TD-009` (missing domain ports) stays parked**, on its original trigger. A broker does not
>   supply a second implementation of anything.
> - **WS-2's "Bank AD federation (OIDC / SAML / LDAP specifics)" (§2) stays parked.** `ADR-009`
>   provisions the *path* to Bank AD; the *protocol* is still unconfirmed by the bank, which is what
>   that row is waiting on (`DEP-010`, `RISK-003`). A private circuit to an unconfirmed protocol
>   changes nothing about the design.
>
> What did change for §2's WS-1 rows: "Dashboards, alerts" and "Disaster-recovery testing" now have
> a platform to run on sooner than Phase 6 assumed, because WS-3's S09 builds the observability and
> DR layers. They stay parked for WS-1 — a platform existing is not a WS-1 work item — but a sweep
> should check whether the WS-1 effort shrank.

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
| — | *empty* | — | — | — |

## 4. Sweep log

| Date | Gate / trigger | Items swept | Promoted | Re-parked | Closed |
|------|----------------|-------------|----------|-----------|--------|
| 2026-08-07 | AIGEM adoption — initial seeding | 9 + 9 | 0 | — | 0 |
| 2026-08-24 | Approved scope change — `CR-012` R0 robustness round | 4 examined (TD-010, SUG-0001, TD-009, WS-2 AD federation) | 0 | 4 — reasons in §1 | 0 |

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
