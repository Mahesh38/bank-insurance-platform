# S14 — Production Readiness & Go-Live

**AIGEM stage:** L9 — Production Readiness · **Owner:** Shivanshi (SRE) + Kalpana (Delivery)
**Central question:** *Can we launch, and survive launch?*

---

## 1. Purpose

Move from "it works and is certified" to "it is live, watched, and recoverable". S14 is where the
operational readiness built incrementally since S07 is formally assessed, and where the
organisation — not just the software — is made ready: on-call, escalation, hypercare, support
paths, and the business processes around the exceptions the system cannot resolve itself.

> **Rule for this stage:** nothing here should be a surprise. If the Operational Readiness Review
> discovers a service with no metrics, the failure occurred at S11 and is only being detected now.

## 2. Entry criteria

- [ ] GATE-S12 passed: certified
- [ ] GATE-S13 passed for everything in the launch scope
- [ ] Production environment provisioned from IaC (GATE-S09)
- [ ] Business readiness underway: RM training, support model, operational procedures

## 3. Epics and stories

### S14-E01 — Production environment readiness · *Shivanshi*

| ID | Story | Acceptance criteria |
|---|---|---|
| S14-E01-S01 | Provision production from the same IaC as UAT | Parameter differences only; no bespoke production resource |
| S14-E01-S02 | Verify production credentials and connectivity | Production keys, IP allowlisting, distributor ID, TLS egress, per partner |
| S14-E01-S03 | Verify production data protection | Encryption, key ownership, residency, retention — attested in production, not inferred from UAT |
| S14-E01-S04 | Size production capacity | Per the capacity model, with launch headroom |
| S14-E01-S05 | Verify production access control | Least privilege; break-glass defined, audited and time-bound |

### S14-E02 — SLO and observability readiness · *Shivanshi + Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S14-E02-S01 | Publish SLIs and SLOs | Journey-level and component-level, agreed with Product |
| S14-E02-S02 | Agree the error budget policy | What happens at each consumption threshold, agreed before launch not after the first breach |
| S14-E02-S03 | Build production dashboards | Technical and business views; the business view usable by the sponsor |
| S14-E02-S04 | Configure production alerting | Every alert actionable, routed, runbook-linked, and tested by firing it |
| S14-E02-S05 | Verify end-to-end traceability in production | A real journey followed through metrics, logs and traces |

### S14-E03 — Operational readiness review · *Shivanshi*

| ID | Story | Acceptance criteria |
|---|---|---|
| S14-E03-S01 | Conduct the ORR against O1–O13 | Every check evidenced per [`08-SRE-READINESS-CANON §5`](../08-SRE-READINESS-CANON.md) |
| S14-E03-S02 | Establish on-call | Roster, rotation, escalation, compensation, and someone actually rostered on day one |
| S14-E03-S03 | Verify runbooks against production | Procedures valid for the production topology, not just UAT |
| S14-E03-S04 | Conduct an incident simulation | A realistic scenario run end to end with the actual on-call team |
| S14-E03-S05 | Establish the support model | Tier 1/2/3, business-hours coverage, RM support path, partner escalation |

### S14-E04 — Disaster recovery · *Shivanshi + Aarti*

| ID | Story | Acceptance criteria |
|---|---|---|
| S14-E04-S01 | Implement the DR configuration | Secondary region per the S07 architecture |
| S14-E04-S02 | **Execute a DR test** | Failover performed; RTO and RPO measured against target, not asserted |
| S14-E04-S03 | Execute a production-grade restore test | Restore from production backup to a working state, timed |
| S14-E04-S04 | Document and rehearse the DR decision | Who declares a disaster, on what criteria, and how the decision is communicated |

### S14-E05 — Launch management · *Kalpana + Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S14-E05-S01 | Build the go-live checklist | Every item owned, dated and signed |
| S14-E05-S02 | Define the launch approach | Pilot cohort, ramp criteria, and the decision points between waves |
| S14-E05-S03 | Define rollback and contingency | What triggers withdrawal; how in-flight journeys and issued policies are handled |
| S14-E05-S04 | Complete business readiness | RM training delivered, materials published, support briefed, operational procedures live |
| S14-E05-S05 | Plan hypercare | Duration, staffing, daily review cadence, and the criteria to exit hypercare |
| S14-E05-S06 | Obtain go-live approvals | Every required sign-off recorded before launch, not retrospectively |

## 4. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S14-VT-01 | Production matches the certified system | Compare production config to UAT and to IaC | Differences are parameters only, all justified |
| S14-VT-02 | Alerts fire and route | Trigger each alert condition in production | All fire, route correctly, runbook reachable |
| S14-VT-03 | On-call can respond | Unannounced incident simulation | Response within target; runbook sufficient |
| S14-VT-04 | **DR works** | Execute failover | RTO and RPO met and measured |
| S14-VT-05 | **Restore works in production** | Restore from a production backup | Working state, timed within RTO |
| S14-VT-06 | Rollback works in production | Rehearse rollback | Previous version restored; no data loss; in-flight journeys resolvable |
| S14-VT-07 | The pilot cohort can transact | First real sale by a real RM to a real customer | Policy issued, confirmed, reconciled, audited |
| S14-VT-08 | Business is ready | Survey trained RMs | Confident, and able to complete the journey without support |
| S14-VT-09 | Support paths work | Raise a test issue through each support tier | Correctly routed and resolved |

## 5. Exit gate — GATE-S14

| # | Criterion | Level | Evidence artefact |
|---|---|---|---|
| S14-G1 | Production provisioned from IaC and verified | E4 | IaC apply record + verification |
| S14-G2 | Production credentials and partner connectivity verified | E3 | Connectivity evidence per partner |
| S14-G3 | SLOs published; error budget policy agreed | E2 | Signed SLO document |
| S14-G4 | Dashboards and alerting live and tested | E4 | Dashboards + alert-fire evidence |
| S14-G5 | ORR passed against O1–O13 | E2 | Signed ORR record |
| S14-G6 | On-call staffed with escalation defined | E1 | Roster + escalation matrix |
| S14-G7 | **DR test executed; RTO/RPO met** | E3 | DR test record with measured times |
| S14-G8 | **Production restore proven** | E3 | Restore record |
| S14-G9 | Rollback rehearsed in production | E3 | Rehearsal record |
| S14-G10 | Incident simulation completed | E3 | Simulation record with findings closed |
| S14-G11 | Go-live checklist signed by security, compliance and product | E2 | Signed checklist (**human** signatures) |
| S14-G12 | Business readiness complete | E2 | Training records + support model sign-off |
| S14-G13 | Hypercare plan agreed and staffed | E1 | Plan with named people |

**Approvers:** Shivanshi (AP, B) · Kalpana (AP) · Deepali (AP, B, **human**) ·
Shailja (AP, B, **human**) · Rajal (AP) · Mahesh (AP) · Swapnali (AP, B) · Aarti (AP)

## 6. Current position in this repository — ⚪ Not reached

Nothing in this stage has been started, which is correct for the programme's actual position.

Drafted future criteria exist as WS-1 Phase 6 in `04-STAGE_GATES.md` — production credentials,
dashboards and alerts, retention job and backup/restore, signed go-live checklist, hypercare plan,
DR test. Those six are a sound skeleton and map onto S14-G2, G4, G8, G11, G13 and G7 respectively.

**One structural observation.** WS-1 Phase 6 is scoped as production readiness for *the
integration service*. Go-live is a **platform** event: you launch a business journey, not a
component. When WS-3 is registered, this stage belongs to the platform, and the integration
service's production readiness becomes one input to it rather than the whole of it.

**The DR and restore items deserve early attention** despite being S14 gates. Both depend on S09
infrastructure that does not exist, and both have long lead times — a DR region is not
provisioned in a sprint. They should be designed at S07 and built at S09 so that S14 is an
*exercise* rather than a construction project.

## 7. Premature at this stage

New features · new LOBs · optimisation · architectural change.

The last weeks before launch are the worst possible moment for anything that invalidates
certification evidence. Everything not required for launch waits for S15.
