# 06 — Risk-based assurance: review the change, gate the release

AIGEM reviewed **plans**, with up to seven boards, before work started. This model reviews the
**change** — the pull request — with reviewers chosen by what the change does, and puts evidence
requirements on **promotion between environments**.

## 1. Risk labels

Every PR carries exactly one label. The author (human or agent) proposes it; any reviewer may raise
it; only the lane lead may lower it, with a one-line reason.

| Label | What the change does | Required before merge | Advisors (AI, parallel, non-blocking) |
|---|---|---|---|
| **R0 routine** | Tests, docs, refactor under green tests, internal code with no contract/control change, dependency bumps without reachable control change | CI green + one review (human **or** an AI reviewer where the lane lead has enabled it for R0) | As relevant |
| **R1 notable** | New or changed internal contract, new dependency or component, schema change on non-evidence data, new service behaviour, cross-lane impact | CI green + **lane lead** (or delegate) review | Architecture, quality, plus any lens the change touches |
| **R2 control change** | Alters a G-control below, or the enforcement of a guardrail | CI green + **the named human owner(s) of each touched control** + author ≠ approver | All relevant, mandatory to run; their output is attached |

AIGEM's T1/T2 fold into R0/R1; T3's "significant" is R1 with more advisors; T4 becomes R2 and is
defined only by the change test below.

## 2. Control changes — the only thing that always needs named humans

Kept from AIGEM `11-REVIEW_GATES.md` §3 (RG-5) because it is the right test. A change is **R2**
when it **alters the behaviour, strength, coverage or trust boundary** of:

| # | Control | Human owner role |
|---|---|---|
| G1 | Who or what may access a resource, or how that decision is reached | security |
| G2 | Which PII / restricted / health / financial fields are collected, stored, logged, exported or shared, or who can see them | compliance + security |
| G3 | How secrets, credentials, keys or certificates are created, stored, transported, rotated or revoked | security |
| G4 | A cryptographic algorithm, mode, key length, key ownership or trust anchor | security |
| G5 | Money movement, financial correctness, limits, reconciliation, maker-checker | product + compliance |
| G6 | Consent capture, retention or deletion | compliance |
| G7 | A data migration or backfill against real data | data |
| G8 | Production topology, public exposure or a network trust boundary | reliability + security |
| G9 | A public contract already consumed by a bank caller or partner (breaking change) | architecture |
| G10 | A control a regulator can ask us to evidence | compliance |

**Working near a control is not changing it.** Logging, naming, tests for an existing control,
docs, runbooks and behaviour-preserving refactors are R0/R1. When genuinely ambiguous, label R1,
say which control you considered, and let the control's owner raise it — raising is one click, not
a CR.

## 3. How advisors work

- Advisors are AI persona lenses ([`personas/`](../personas/README.md)). They run **in parallel**
  on the diff, each answering only its own checklist.
- Output is **advice**: `ok`, `concern` (with a concrete failure scenario), or `blocker-candidate`
  (the advisor believes a guardrail or control is violated).
- A `blocker-candidate` routes to the human owner of that guardrail/control. It is not itself a
  block — an AI cannot block, just as it cannot approve.
- Advisors never comment outside their lens. A security advisor commenting on naming is noise.

## 4. Definition of Done by risk

| | R0 | R1 | R2 |
|---|---|---|---|
| CI green (incl. guardrail checks) | ✅ | ✅ | ✅ |
| Tests for new behaviour | ✅ | ✅ | ✅ incl. negative/abuse cases |
| Contract published/updated if changed | — | ✅ | ✅ |
| Advisor output attached | optional | ✅ | ✅ all relevant lenses |
| Runbook/observability updated if operational behaviour changed | — | ✅ | ✅ |
| Named human approval | — | lane lead | control owner(s), author ≠ approver |
| Evidence linked on the card | PR link | PR + test report | PR + test report + advisor output + approval |

**Definition of Ready** is four lines, not a checklist: outcome link · one acceptance example ·
contract or stub known · risk label guess. If those four cannot be written, the card is an
`Explore` card.

## 5. Release checkpoints

Stages are gone; environments remain. Evidence attaches to **promotion**, and each checkpoint is a
query over evidence that already exists — not a meeting, and not a signature on a stage.

| Promote to | Needs | Human sign-off |
|---|---|---|
| **dev** (synthetic data only) | Main is green. That is all. | none |
| **bank-UAT** (bank systems, test data) | Contracts published for everything the bank touches · security scans clean at HIGH+ · guardrail checks green · open decisions with `blocks_promotion_to: uat` resolved · external dependencies for this path satisfied or explicitly stubbed and declared | Lane lead; security owner if any G1–G4/G8 change since last UAT promotion |
| **prod** (real customers, money, PII) | Everything for UAT · operational readiness (SLOs, dashboards, alerts, runbooks, DR tested for the path) · compliance evidence for G2/G5/G6/G10 controls on the path · performance evidence at expected load · all `R2` changes approved · no open Expedite on the path | **Human only:** product + compliance + security + reliability owners. Never an AI. |

Checkpoints are **per capability path**, not per programme: the Term quote path can be
prod-ready while ULIP is still in dev. A checkpoint can be evaluated as often as anyone likes; the
answer is "ready" or a list of missing evidence.

The AIGEM gate criteria are not thrown away — they are re-expressed as checkpoint evidence for the
path they belong to ([`migration/carry-over.md`](../migration/carry-over.md)).
