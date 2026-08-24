# Board 7 — Operations / SRE · CR-012 · DRAFT

**Persona:** Shivanshi — Principal Insurance Platform SRE / Reliability Engineering Head (Board 7 / `R10`)
**Authored by:** Architecture agent, **simulating** Board 7. This is not Shivanshi's verdict.
**Change request:** [CR-012 — R0 platform robustness](../../CR-012-r0-platform-robustness.md)
**Date:** 2026-08-24
**Status:** `AI-DRAFTED — no SRE signature. Board 7 owns four of these five layers operationally and carries the on-call surface they create.`

---

## 1. Decision requested

Not "are these good layers". The question is narrower and harder: **can this team operate three new
stateful managed services, a firewall rule set and two circuits, starting from a position where
`GATE-S08` is still open and no service has yet run in a real environment?**

Board 7 also inherits four concrete deliverables it did not have yesterday: a carrier order, a
firewall rule set with a curator, a broker upgrade path, and an ISM policy — plus three drills.

## 2. What was reviewed

- [`R0-LLD.md`](../../../../architecture/R0-LLD.md) — §1.1 BOM rows 25–34, §1.4 per-environment shapes, §2.1 AZ placement, §2.2/§2.3, §4.2 cluster shape, §6, §11.1 `D13`–`D16`, §12.1 bands `P0`–`P8`
- `ADR-009` … `ADR-013`
- `NFR-NET-01…04`, `NFR-CAC-01…03`, `NFR-EVT-01…04`, `NFR-OBS-01…03`
- `RISK-012`, `RISK-013`, `RISK-014`

## 3. Findings

**F-1 — nothing is self-hosted, and that is the single most important property of this CR.** Every
new tier is a managed service, and `R0-LLD` §4.1 explicitly refuses Kafka, Valkey, OpenSearch and
Prometheus as StatefulSets in the cluster. A self-hosted broker or search cluster at this maturity
would be a rejection from this board; managed ones are an accepted operational cost with a
predictable failure profile.

**F-2 — the shapes are sized for availability, and the file says so.** Three brokers is a quorum
floor carrying tens of messages a minute; two cache nodes hold a working set that fits in a pod's
heap. Board 7's usual objection — "this is provisioned for a load we do not have" — is pre-answered
rather than hidden, and `ADR-011`'s recorded order (scale **up** before out) is the right default.

**F-3 — `dev` is not production-shaped, and that is what makes the cost defensible.** §1.4 caps the
multiplication: MSK Serverless or one broker, one cache node, one search node, one firewall endpoint
in alert mode, VPN only, stubs permitted. Three production estates would have been a rejection.

**F-4 — the firewall is now on the critical path for every provider call, and that cuts both ways.**
Egress inspection is a control this platform needed. It is also a new mandatory hop where a rule
error is a total egress outage, and the quote path notices first. Endpoints per AZ (§2.1) and a named
runbook are the mitigation; neither exists yet.

**F-5 — `D13`–`D15` are the DR rows I would have asked for.** Cache, broker and search are
deliberately absent from `ap-south-2`, with reconstruction rather than replication as the mechanism.
`D16` matters more: a warm standby that cannot reach CBS or Bank AD can start pods and answer
nothing, and provisioning the DR Transit Gateway and VPN attachment in the same change as the
primaries is what makes the standby real.

**F-6 — the drills are the part that will slip, and they are the part that is load-bearing.**
`NFR-NET-01` (DX→VPN failover), `NFR-EVT-03` (outbox replay), `NFR-CAC-01/02` (cache failover with
sessions held). Each converts a claim into a capability, and all three land in the `P8` proof band
where nothing can be produced in the week the gate is reviewed.

**F-7 — the external dependency is mine and it is the real schedule risk.** `DEP-20260824-dx1`: the
bank must terminate a VPN, publish prefixes, open a firewall and accept a DX order. VPN-first is the
right sequencing because it needs a firewall rule rather than a carrier order — but the bank's
change window is not something this programme controls. `RISK-013` is correctly stated.

**F-8 — the Elastic IP move is an operational hazard disguised as an improvement.** Fewer, more
stable addresses is genuinely better. But from 1SB's side, an allowlist populated from the old design
is indistinguishable from an allowlist that was never populated, and this is the failure that
surfaces as "quotes work in dev and time out in UAT". `DEP-20260824-eip` exists for it.

## 4. Draft verdict

`APPROVE-WITH-MODIFICATION` — drafted, unsigned.

Operable, on conditions, and better than the alternative: four of these five layers are cheaper to
build now than to retrofit, and two of them (inspection, connectivity) are things Board 7 would have
raised as gaps at the S09 review anyway.

## 5. Conditions (drafted)

1. **A runbook per tier before it reaches `uat`.** Broker (lag, broker loss, replay), cache
   (failover, mass-logout handling), search (index pressure, ISM failure), firewall (rule rollback —
   the one that is an outage), connectivity (DX→VPN failover and back). No tier arrives in `uat`
   without one; that is `RISK-014`'s mitigation and it is not optional.
2. **The three drills are scheduled at the start of the S09 build, not the end.** `NFR-NET-01`,
   `NFR-EVT-03`, `NFR-CAC-01/02`. A drill scheduled after the gate date is a drill that will not
   happen.
3. **`NFR-OPEN-6` cost envelope before the first `apply` to `uat`.** Board 7 produces it with
   Kalpana; `GATE-S09` entry already requires "cloud account structure and budget approved" and this
   CR materially changes the second half.
4. **The Elastic IP list is published from `R0-LLD` §2.3 and confirmed address-by-address in
   writing** by both 1SB and the AU Bank PG (`NFR-NET-03`) before UAT opens.
5. **Alert routing exists for the four new signals before W1**: outbox age, consumer lag, firewall
   drop rate, cache failover. A new tier with no alert is a tier nobody notices failing.
6. **Broker and cache upgrade windows are agreed with Amit before W3**, because a broker minor-version
   upgrade during the money-path wave is an avoidable self-inflicted incident.
7. **`RISK-014` stays open until every tier has run through one incident or one drill.** Board 7 does
   not accept an operational-maturity risk on the basis of a design document.

## 6. What this draft may not do

It may not commit a carrier order, a bank change window or a budget. It may not accept `RISK-012` or
`RISK-014`. It may not sign the S09 gate criteria, and it may not treat sizing recorded as a
"starting point" in the LLD as a Board 7 sizing decision — those remain Shivanshi's with Aarti.
