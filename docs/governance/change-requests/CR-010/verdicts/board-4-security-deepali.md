# Board 4 — Security · Verdict on CR-010

**Change request:** [CR-010 — Reusable Context Module and Safe Autopilot Foundation](../../CR-010-context-module-and-safe-autopilot.md)
**Plan:** [PLAN-001](../../../plans/PLAN-001-context-module-and-safe-autopilot.md)
**Board:** 4 — Security (**veto authority**) · **Role:** R8
**Persona:** Deepali — Principal Insurance Platform Security Architect / Security Head
**Reviewer type:** `AGENT` · **Self-review:** false
**Review depth:** SECURITY-L3 for the automation control plane; SECURITY-L4 questions raised for
residency and scanning · **Change tier:** T4 · **Date:** 2026-08-16

> ### ⚠ AI-drafted simulation — the mandatory human Security signature is NOT satisfied
> Drafted by the Architecture agent (Mahesh persona) as an input to Board 4, using the persona
> package at [`deepali-principal-security-architect/`](../../../../context/roles/deepali-principal-security-architect/README.md).
> [`11-REVIEW_GATES.md §2`](../../../11-REVIEW_GATES.md): **Security verdicts at T4 require
> `reviewer_type: HUMAN`. No exceptions, no aggregate override.** This document is the AI security
> assessment only. It does not satisfy S07-G3, S07-G4, or the T4 Security sign-off on CR-010.
>
> Per [`08-security-review-release-and-exception-contract.md §7`](../../../../context/roles/deepali-principal-security-architect/08-security-review-release-and-exception-contract.md),
> an AI persona must never soften an evidence-based security conclusion to make a human decision
> look clean. Nothing below has been softened.

> ## Verdict: `APPROVE-WITH-MODIFICATION`
> **Security severity:** `S1` — high. Three S1 findings, four S2. **No S0 finding on CR-010's own
> change surface.** One pre-existing question (SEC-F03) is **capable of being S0** and cannot be
> resolved by Security alone.
>
> **`signature_status: AI-DRAFTED — mandatory human Security signature outstanding`**

---

## 1. Decision requested

CR-010 §4 asks Security to conclude on **automation permissions and the protected-path boundary**.
I extend that, within jurisdiction, to the two obligations this CR's own documents raise and cannot
leave unstated: **secret/SAST/SCA/image scanning** and **data residency**.

Scope note. CR-010 changes repository automation and documentation. It changes **no runtime trust
boundary, no production credential, no authentication or authorization path, no PII flow**. That is
the correct starting position and it is why this is not a rejection.

---

## 2. What I reviewed

- `.github/workflows/application-ci.yml` and `governance.yml` — full text: triggers, `permissions`
  blocks, third-party action references, dependency installation, artefact upload.
- `scripts/governance/autopilot.py` — full, with attention to the write path and the policy guard.
- `scripts/governance/test_autopilot.py` — all five tests.
- `scripts/governance/ci-checks.py` — checks 2, 3, 5, 6, 8.
- `.github/` directory listing (checking for `CODEOWNERS`).
- `.gitignore` — secret-adjacent exclusions.
- `docs/governance/state/GATE-EVIDENCE.yaml` `policy` block.
- `docs/application-lifecycle-bible/07-SECURITY-COMPLIANCE-CANON.md` §3 (C1–C10) and §4 (pipeline
  security gates).
- `docs/application-lifecycle-bible/stages/S08-engineering-foundation.md` §6 and
  `stages/S09-platform-foundation.md` §6 — the capability tables.
- `docs/platform/architecture-review/06-security-compliance-and-nfrs.md`.
- `docs/platform/ws3-platform/04-security-architecture.md` — the architecture-side security design
  submitted to this board.
- `render.yaml` presence and the S09 stage file's description of what is deployed.

Checks S1–S12 applied; results in §5.

---

## 3. Findings

Severity is Deepali's `S0`–`S3` and must not be read as AIGEM `P1`–`P5` or Mahesh's `A0`–`A3`.

### SEC-F01 · `S1` · The protected-path boundary is asserted, not enforced

CR-010 §2 is the security-relevant heart of this change: automation may never mark a stage `PASSED`,
provide board approval, accept material risk, or weaken a binding control.

**What is enforced.** `validate_policy()` raises `AutopilotRefusal` unless the policy is exactly
`{mode: proposal-only, human_pass_required: true, silence_approves: false, automatic_waivers:
false}`. `candidate_proposal()` emits `state: CANDIDATE` with `may_mark_passed: false` and refuses
on incomplete evidence. `test_autopilot.py` asserts both. That is a fail-closed control and it is
the right design.

**What is not enforced.** `propose-transition --output <Path>` calls
`args.output.write_text(rendered, encoding="utf-8")` with no destination constraint. The controller
that must never mark a stage `PASSED` **can overwrite the file in which stage state lives**.

```
autopilot.py propose-transition --workstream WS-1 \
    --output docs/governance/state/CURRENT-STATE.yaml
```

There is no `CODEOWNERS` file — `.github/` contains only `workflows/`. There is no protected-path
assertion in `ci-checks.py`. There is no test on the write path.

I want to be precise about the threat, because overstating it would be as unhelpful as missing it.
This is **not** a remote exploit and **not** a privilege-escalation path: it requires an actor who
already has repository write access and who runs the command with that argument. It is an
**integrity control gap** in the one control CR-010 exists to establish. Under my non-bypassable
list this is not an S0 condition; it is squarely S1 — *high, fix before the next release* — and the
remediation is small.

`security_finding`:

```yaml
id: SEC-FIND-CR010-01
severity: S1
asset: "docs/governance/state/CURRENT-STATE.yaml, GATE-EVIDENCE.yaml, change-requests/**"
trust_boundary: "repository automation → human-owned governance state"
threat: "Tampering with human-owned stage state through the proposal-only controller's write path"
preconditions: ["repository write access", "invocation with --output pointing at a protected path"]
exploitability: "Low — requires an authorised actor and deliberate argument"
impact: "Loss of integrity of the record that governs every stage transition; a governance control asserted but not enforced"
required_outcome: "Constrain the write destination; assert refusal in a test; add repository-level CODEOWNERS protection"
owner: "Amit / Engineering with Deepali / Security"
target: "before CR-010 binds"
```

### SEC-F02 · `S1` · No secret, SAST, SCA or image scanning on a regulated financial application

[`stages/S08 §6`](../../../../application-lifecycle-bible/stages/S08-engineering-foundation.md)
records all of these as **Absent**, and I verified it: neither workflow contains a scanning step,
and no SBOM is produced. The application handles PAN, Aadhaar references, income and health
attributes.

CR-010 does not claim to deliver these — PLAN-001 is honest about it — so this is not a defect *in*
CR-010. It is the security obligation CR-010's own acceptance criterion ("application CI exists")
makes newly actionable, and it is why I will not treat the arrival of application CI as closing
anything on the security side.

Per [`07-SECURITY-COMPLIANCE-CANON.md §4`](../../../../application-lifecycle-bible/07-SECURITY-COMPLIANCE-CANON.md),
the required gates are: secret scanning on every commit; SAST failing on new critical/high; SCA
failing on critical/high with a reachable path; image scanning blocking critical CVEs; SBOM per
build; IaC policy-as-code at S09; DAST nightly from S11.

**One of these is materially more urgent than the others**: a **historical** secret scan of the full
repository history. It has never been run. If a credential was ever committed, it is in the history
now, and every day it is unexamined is a day it may have been read. That is a one-off task with a
same-day cost.

### SEC-F03 · `S1`, capable of `S0` · Data residency is unverified on a live deployment

`render.yaml` defines a Render.com `starter`-plan service running the combined image.
[`stages/S09 §6`](../../../../application-lifecycle-bible/stages/S09-platform-foundation.md) records
the region as *"not chosen, not pinned, not attested"*, and raises the question directly: *if any
real customer PII has passed through the current deployment, that is a question for Compliance to
answer now rather than at audit.*

Control **C6 — data residency — is non-waivable at any tier by any authority**
([`04-GATE-AND-SIGNOFF-MODEL.md §8`](../../../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md)).

I classify this as **S1 today, escalating to S0 conditional on one fact I do not have**: whether real
customer or production-like PII has been processed through that deployment. That fact is knowable
today, and the escalation is automatic if the answer is yes.

**Two actions, and neither can wait for GATE-S09:**

1. Establish the answer. Deployment history, ingress logs, database contents. Same-day.
2. Establish the boundary regardless of the answer. ADR-001 does this and I support it: **Render.com
   is dev-preview only and never a PII data path.** Standing constraint SC-W3-7 in the WS-3
   registration makes it a triage-blocking rule. It needs to be true operationally, not only in a
   document — meaning no realistic test data, no CBS connection, no proposal payloads.

Regulatory permissibility is **Shailja's** call, not mine. I determine the technical security
posture; she determines whether a residency exposure is reportable. Both of us are needed here.

### SEC-F04 · `S2` · Third-party GitHub Actions pinned by tag, not by commit SHA

Both workflows reference `actions/checkout@v4`, `actions/setup-java@v4`,
`gradle/actions/setup-gradle@v4`, `actions/upload-artifact@v4`, `actions/setup-python@v5`.

A moveable tag can be repointed by its owner or by an account compromise, and the next run silently
executes different code inside a job that has repository read access and produces the artefacts a
regulated release will eventually be evidenced by. For a bank, SHA-pinning third-party actions with
a documented update cadence is the expected posture.

Mitigating context, which is why this is S2 and not S1: both workflows declare
`permissions: contents: read`, which is correct least privilege, and no workflow holds a deployment
credential or a package-write token today. The blast radius is currently bounded. It will not stay
bounded once S09 gives the pipeline deployment identity, and pinning is far cheaper to do now.

### SEC-F05 · `S2` · Unpinned dependency installation in the governance job

`governance.yml` runs `pip install --quiet pyyaml jsonschema` with no version constraint and no
hash pinning. The resulting job reads governance state and validates the routing model. A compromised
or typo-squatted release would execute in that context.

**Fix:** a pinned `requirements.txt` with hashes, installed with `pip install --require-hashes`.
Small change, and it also makes the governance job reproducible, which S08-E02-S04 wants anyway.

### SEC-F06 · `S2` · No `CODEOWNERS`

Independent of SEC-F01's code fix, `docs/governance/state/**`,
`docs/governance/change-requests/**` and `.github/workflows/**` should require review from a named
owner. Defence in depth: SEC-F01's constraint stops the tool, `CODEOWNERS` stops the pull request.
Neither is a substitute for the other.

### SEC-F07 · `S2` · The PII-masking control is unproven

Control C5 is 🟡: `PiiMaskingConverter` exists and nothing demonstrates it works. S08-VT-06 defines
the test — run the full suite, scan every emitted log for PAN, Aadhaar, phone, email and health
patterns, expect zero matches. Until that test runs, "no PII in logs" is an assertion about code,
and the standing constraint that says so is unenforced.

This is S2 rather than S1 because the converter does exist and the data volumes today are test data.
It becomes S1 the moment a realistic dataset enters any environment.

### SEC-F08 · `S3` · Security architecture input reviewed and broadly endorsed

I reviewed [`ws3-platform/04-security-architecture.md`](../../../../platform/ws3-platform/04-security-architecture.md),
submitted by Architecture as an input to this board. Assessment:

| Element | Position |
|---|---|
| Six trust boundaries with what must never cross | **Endorsed.** This is the per-boundary model S07-G3 requires and did not have |
| STRIDE per boundary, 34 threats with implementation state | **Endorsed as an artefact.** Ratification requires my human signature |
| Fail-closed PDP (seam S-02) | **Endorsed, and required.** An availability incident must never become an authorisation incident |
| Double enforcement at BFF and owning service | **Endorsed.** BFF-only checks are a broken-object-level-authorisation defect waiting for a direct-call path |
| Request-scoped PDP decision caching only | **Endorsed.** A longer cache converts a revocation into a delay |
| `RECONCILED`, not `CAPTURED`, as the issuance precondition | **Strongly endorsed.** A callback-only capture path means a forged callback issues a policy for money never received. This turns a trust assumption into a control and it is the best security decision in the document |
| CMK per data class | **Endorsed.** One platform key is one blast radius |
| PII-F — audited reads of restricted attributes | **Endorsed.** This is an addition to current repository practice and it is the right one |
| Customer principal undefined for self-service | **Open, and correctly stated rather than glossed.** Concurs with Board 1 A-F03 |
| Aadhaar tokenisation assumed with no tokenisation service | **Open.** The model depends on a capability that does not exist |

---

## 4. What I am explicitly not doing

| Not doing | Why |
|---|---|
| Satisfying the T4 human Security sign-off | An AI cannot. Rule, not preference |
| Closing S07-G3 or S07-G4 | Both require a human Security signature |
| Ruling on whether a residency exposure is reportable | Shailja's jurisdiction. I state the technical posture |
| Accepting residual risk on SEC-F01, F02 or F03 | S1 acceptance belongs to my accountable human; S0 to the accountable risk owner. Never to a persona and never to an agent |
| Blocking CR-010 | No S0 finding exists on its own change surface. A block here would be disproportionate and would delay the CI that makes the rest of the security programme possible |
| Ruling on Board 7 operational posture | Shivanshi's |

---

## 5. Checklist S1–S12

| # | Check | Result |
|---|---|---|
| S1 | Changes who/what can do what? | **No runtime change.** Repository automation only. Workflows are `contents: read` |
| S2 | PII introduced, moved, logged, persisted, exported? | **No** by this CR. Pre-existing PII exposure questions stand: SEC-F03, SEC-F07 |
| S3 | Secrets stored, retrieved, rotated, revoked through approved mechanisms? | **Not in scope of this CR.** TD-006 records the secrets provider as a stub; S09 obligation |
| S4 | Data protected in transit and at rest? | Unchanged by this CR |
| S5 | External/untrusted input validated at the boundary? | **Yes for the controller** — JSON-schema validation of gate evidence plus a policy guard, both failing closed. **No for `--output`** (SEC-F01) |
| S6 | Attack surface growth? | **None externally.** Internally, one unconstrained write path (SEC-F01) |
| S7 | Applicable abuse classes on changed paths? | Path traversal on `--output`; supply chain on unpinned actions and unpinned pip (SEC-F04, F05) |
| S8 | Security events attributable and auditable? | **Partially.** Automation actions are traceable through git history; there is no separate security event stream, which is an S09 obligation |
| S9 | New dependencies, images, IaC, artefacts — known vulnerabilities, provenance, remediation? | **This is the gap.** No SCA, no SBOM, no image scan, no pinning (SEC-F02, F04, F05) |
| S10 | Does the protected path fail closed? | **Yes for policy and evidence validation** — refuses rather than degrades. **Not applicable** to the write path, which has no guard at all |
| S11 | Partner/1SB/insurer trust contract explicit? | Unchanged by this CR. The architecture input documents TB-5 adequately |
| S12 | Blast radius if this workload or credential is compromised? | **Currently bounded** — read-only workflows, no deployment identity, no production credential. **This changes at S09** and the pinning and least-privilege posture must be settled before it does |

---

## 6. Conditions

| # | Condition | Severity | Owner | Required by |
|---|---|---|---|---|
| **SEC-C1** | Constrain `autopilot.py --output` to a proposals directory; reject `..` and symlink escapes; add a test asserting refusal on `docs/governance/state/**` and `docs/governance/change-requests/**`; wire it into the governance workflow. | `S1` | Amit + Deepali | **Before CR-010 binds** |
| **SEC-C2** | Add `CODEOWNERS` covering `docs/governance/state/**`, `docs/governance/change-requests/**` and `.github/workflows/**`. | `S2` | Amit | **Before CR-010 binds** |
| **SEC-C3** | Run a **historical secret scan over full repository history**, once, and remediate anything found with rotation, not deletion. This is the highest-value single security action available today and it does not depend on GATE-S08. | `S1` | Amit + Deepali | **Within 5 working days of CR-010 ratification** |
| **SEC-C4** | Establish whether real customer or production-like PII has been processed through the Render.com deployment, and give the answer to Shailja. Escalates to `S0` if yes. | `S1→S0` | Shivanshi + Shailja | **Immediate** |
| **SEC-C5** | Make ADR-001's Render boundary operationally true: no realistic test data, no CBS connection, no proposal payloads on that deployment, and no gate may cite it as evidence. | `S1` | Shivanshi + Mahesh | With CR-010 ratification |
| **SEC-C6** | Pin third-party GitHub Actions to commit SHAs with a documented update cadence. **Mandatory before any workflow is granted deployment identity at S09.** | `S2` | Amit + Shivanshi | Before S09 pipeline identity |
| **SEC-C7** | Pin and hash-lock Python dependencies in the governance workflow (`--require-hashes`). | `S2` | Amit | With CR-010 |
| **SEC-C8** | Deliver secret scanning, SAST, SCA, image scanning and SBOM in the pipeline, failing the build per the security canon §4 thresholds. Route findings to the risk register with the S0–S3 remediation SLA. | `S1` | Amit + Deepali | **GATE-S08 (S08-G5) — non-negotiable for that gate** |
| **SEC-C9** | Execute the PII-in-logs test (S08-VT-06) and publish the result. Until then, control C5 remains 🟡 and no gate may cite it as met. | `S2` | Swapnali + Deepali | GATE-S08 (S08-G7) |
| **SEC-C10** | Obtain the **human** Security signature on the trust-boundary threat model and security architecture (S07-G3, S07-G4). Not waivable by any authority at any tier. | — | Deepali (human) | Before GATE-S08 |
| **SEC-C11** | Resolve the customer-identity gap (SEC-OPEN-1) and the Aadhaar tokenisation dependency (SEC-OPEN-2) before S11 entry. | `S2` | Rajal + Deepali + Aarti | Before S11 entry |

---

## 7. Record

```yaml
security_review:
  board: SECURITY
  persona: Deepali
  reviewer_type: AGENT
  ai_simulated: true
  drafted_by: "Architecture agent (Mahesh persona)"
  self_review: false
  change_request: CR-010
  change_tier: T4
  review_depth: SECURITY-L3
  decision: APPROVED_WITH_CONDITIONS
  severity: S1
  must_fix:
    - "SEC-FIND-CR010-01 — constrain the autopilot write path and prove refusal by test"
    - "CODEOWNERS on governance state, change requests and workflows"
  conditions: [SEC-C1, SEC-C2, SEC-C3, SEC-C4, SEC-C5, SEC-C6, SEC-C7, SEC-C8, SEC-C9, SEC-C10, SEC-C11]
  recommendations:
    - "SHA-pin actions now while the blast radius is still bounded; it is far cheaper than after S09"
    - "Treat the historical secret scan as a same-week action, independent of any gate"
  evidence:
    - "read both workflow files in full including permissions blocks and action references"
    - "read autopilot.py end to end; confirmed the unconstrained --output write at the CLI layer"
    - "confirmed validate_policy() and candidate_proposal() fail closed, asserted by tests"
    - "confirmed .github/ contains no CODEOWNERS"
    - "confirmed governance.yml installs pyyaml and jsonschema unpinned"
    - "verified S08 and S09 stage capability tables against the repository: no SAST, SCA, secret, image scanning, SBOM, or IaC"
    - "reviewed ws3-platform/04-security-architecture.md against checks S1-S12"
  residual_risk: >
    Bounded today: repository automation with read-only workflow permissions, no deployment
    identity, no production credential, no runtime trust-boundary change. Two pre-existing S1
    exposures dominate the actual risk position and neither originates in CR-010: the absence of
    any security scanning on a regulated financial application, and an unverified data-residency
    position on a live deployment. The second is capable of being S0 and the fact that resolves it
    is knowable today.
  human_signature_required: true
  human_signature_status: OUTSTANDING
  note: >
    Per contract section 7, this AI assessment is preserved separately from any human governance
    decision. A human risk acceptance does not rewrite this technical finding.
  date: 2026-08-16
```

---

**Persona:** Deepali — Principal Insurance Platform Security Architect · Board 4 / R8
**Drafted by:** Architecture agent under the Mahesh persona, as an input to Board 4
**signature_status:** `AI-DRAFTED — mandatory human Security signature outstanding`
