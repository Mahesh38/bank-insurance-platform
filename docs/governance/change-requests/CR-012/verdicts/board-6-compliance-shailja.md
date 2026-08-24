# Board 6 — Risk & Compliance · CR-012 · DRAFT

**Persona:** Shailja S — Compliance & Risk Head (Board 6 / `R9`)
**Authored by:** Architecture agent, **simulating** Board 6. This is not Shailja's verdict.
**Change request:** [CR-012 — R0 platform robustness](../../CR-012-r0-platform-robustness.md)
**Date:** 2026-08-24
**Status:** `AI-DRAFTED — no Compliance signature. Board 6 holds a binding veto, and a regulatory interpretation is never an architecture output.`

---

## 1. Decision requested

Two sentences. Both are licence positions rather than design preferences, which is why they are here
and not in the ADRs' own approvals alone.

| # | The sentence | Where it lives |
|---|---|---|
| 1 | **No regulatory evidence exists only in a topic.** The audit consumer's write to DynamoDB and the S3 WORM archive is the record; the MSK topic that carried it is transport | `ADR-012`, `FF-26`, `NFR-EVT-04` |
| 2 | **The OpenSearch domain holds no regulatory evidence.** Its ISM policy deletes indices at 90 days, and no gate, audit or regulatory query is answered from it | `ADR-013`, `FF-27`, `FF-28`, `NFR-OBS-03` |

## 2. What was reviewed

- `ADR-012` and `ADR-013` in [`08-architecture-decision-log.md`](../../../../platform/architecture-review/08-architecture-decision-log.md)
- [`R0-LLD.md`](../../../../architecture/R0-LLD.md) §6.2, §6.3, §10 (the two-pipe table), §11.1 (`D13`–`D16`)
- [`04-security-architecture.md`](../../../../platform/ws3-platform/04-security-architecture.md) §8 audit immutability, §9 logging
- `NFR-DAT-01/02/06/07`, `NFR-EVT-03/04`, `NFR-OBS-02/03`

## 3. Findings

**F-1 — the retention position is unchanged, and that is the whole basis for approving.** Two new
disposable stores enter the estate. Neither holds evidence. `RET-7Y-IMMUTABLE` still lives in the
audit event store and the Object Lock archive, and Object Lock is still applied before the first
evidence is written. If either new store held evidence, an ISM delete policy or a topic retention
window would be a retention violation dressed as an operational parameter — which is exactly the
failure both ADRs name and refuse.

**F-2 — the completion condition moved to the right place.** A journey reaches `SOLD` only when the
audit **write** confirms, not when the topic acknowledges the message (`INV-JRN-05`, `FF-26`). This
is the distinction that matters for a mis-selling review: an offset commit proves a consumer read
something, and proves nothing about what was recorded.

**F-3 — residency is unaffected and slightly improved.** All five layers are `ap-south-1` with
replicas only in `ap-south-2`. `ADR-009` additionally takes CIF and Bank AD traffic **off the public
internet**, which is a better posture than the position this CR replaces, and `ADR-010`'s inspection
logs carry destinations and metadata rather than payloads.

**F-4 — no PII enters the new tiers, and the enforcement is doubled rather than moved.** Masking
stays at emission (`FF-05`); `FF-27` checks the index as well, because a log pipeline is the
commonest route to an unclassified PII store. Event payloads follow the same rule as logs, and the
schema registry is what makes them reviewable rather than discoverable.

**F-5 — the disposal obligation is two-sided and is now met for the new store.** `NFR-OBS-03`
requires an ISM policy on every index and a disposal record at the horizon. An index with no
lifecycle policy is a retention breach in the other direction: data kept past its horizon.

**F-6 — the one thing that would change this position.** `ADR-013`'s revisit trigger contemplates
onboarding onto the bank's enterprise SIEM. If that happens, the exclusion in §1 must travel with
the change. A bank-wide log platform is exactly where "we already keep everything for seven years"
becomes an assumption nobody wrote down.

## 4. Draft verdict

`APPROVE-WITH-MODIFICATION` — drafted, unsigned.

Compliance has no objection to the layers and a direct interest in two of them: denied egress is now
an evidenced control, and CIF traffic leaves the public internet. The conditions are about keeping
the two exclusions true under pressure, not about the design.

## 5. Conditions (drafted)

1. **The two sentences in §1 are standing constraints, not ADR prose.** They belong on the standing
   constraint list at the next human transcription of `CURRENT-STATE.yaml`, because an ADR can be
   superseded by another ADR and a standing constraint cannot.
2. **`FF-26` and `FF-28` are gate evidence for `GATE-S09`**, and a red result blocks. They are the
   only mechanical difference between "evidence is in the right store" and "evidence was intended to
   be in the right store".
3. **A topic retention extension requires a Board 6 conversation**, even though it looks purely
   operational. It is the most plausible route to §1 becoming false: extend retention for
   convenience, then cite the topic during an investigation.
4. **`NFR-EVT-03`'s replay drill is compliance-relevant, not just SRE-relevant.** It is what
   demonstrates that no evidence was lost when a broker was rebuilt in DR. Board 6 wants the record,
   not the design (`ADR-012` / `D14`).
5. **The audit consumer's IAM stays `INSERT`-only** and the log pipeline's roles keep no permission
   on the audit table or archive (`FF-28`, both directions). Two pipes that can reach each other are
   one pipe.
6. **If the enterprise SIEM migration is proposed, this exclusion is re-signed** rather than
   inherited.

## 6. What this draft may not do

It may not sign the two exclusions. It may not interpret IRDAI or DPDP retention obligations — that
is a regulatory determination and an AI simulation of Board 6 does not make one. It may not accept
`RISK-015` (invariant erosion), which is the risk most likely to make this verdict obsolete.
