# DEC-20260829-02 — M3 readiness: seven board recommendations

**Status:** `AI-DRAFTED — RECOMMENDATIONS`. No decision is recorded here. Nothing below is approved.
**Date:** 2026-08-29 · **Workstream:** WS-3 · **Stage:** S08 with S09 overlapped
**Origin:** repository-owner request — *"help me decide the path and close M0, M1 and M2 so we can work on M3"*
**Convened:** Kalpana (`R12`) · Mahesh (Board 1) · Amit (`R3`) · Deepali (Board 4) · Swapnali (Board 5) · Shailja (Board 6) · Shivanshi (Board 7)
**Freshness:** `FreshnessCheck` exit `1` — `state_as_of` 19 days old, review due 2026-09-09.

---

## 1. The recommendation, before the detail

> **Do not try to close M1 and M2 before starting M3. The premise costs weeks and buys nothing.**
>
> **M0 is already closed.** **M1 cannot be closed by anyone in this room** — seven of twelve inputs
> are the bank's. **M2 cannot close** until a human rotates a key and another human looks at 23
> images. **Neither gates M3.**
>
> Measured against `GLM-001` §4, exactly **one** constraint reaches M3: `M1.6 (state standard) ──►
> M3.3`. It gates **one task of eleven**. `M0.3` is decided and `M1.2` is answered.
>
> **Recommendation: authorise a partial M3 start now — M3.1, M3.2, M3.4–M3.11 — and run the M1 and
> M2 tails in parallel.** That is ~29 of M3's ~31 agent-hours available today.

### 1.1 Why the sequencing matters more than the work

M2's open items gate **M5.2**, the first push to the bank estate. M5.2 is four phases away.
Serialising M3 behind them converts three independent waits into one additive wait:

```text
Serial   :  wait(bank M1) → wait(B rotation) → wait(binary review) → M3 → M4 → M5   ≈ 12+ weeks
Parallel :  M3 now ──────────────────────────────────────────────► M4 → M5         ≈ 6–9 weeks
            bank M1 ────────────────► (M3.3, M8)
            B rotation ─────────────────────────► (M5.2)
            binary review ──────────────────────► (M5.2)
```

The slow path in `GLM-001` §5.2 was costed at 12+ weeks. **Serialising is how you choose it.**

---

## 2. Board positions

### Kalpana — `R12` Delivery · `DL1`

> **Rule PA-1: I may compel a decision to happen. I may never supply its content.** Below are
> required-by dates and a framing challenge, not choices.

The request contains a waterfall premise — *close M0, M1, M2, then start M3* — and it is the
expensive one. I am obliged to say so and not to decide it.

**M1 has no closure event available to this team.** Seven inputs sit with the bank; `ASM-012`…
`ASM-022` expire 2026-09-19. "Closing M1" is not a decision anyone here can take, and treating it as
a milestone makes the programme's forecast a function of someone else's inbox. I am recording M1 as
a **standing dependency with expiry**, not a gate — and reporting `DECISION-BLOCKED` on it, as before.

**M2 has a closure event, and it is not ours either**: a key rotation and a human reviewing 23
images. I will not average either into green.

| Decision | Owner | Severity | Required by |
|---|---|---|---|
| **D1** Authorise partial M3 start (M3.1, M3.2, M3.4–M3.11) | Shivanshi + Mahesh + Amit | `DL1` | **2026-09-01** |
| **D2** M1.6 interim state-backend position, to unblock M3.3 | Shivanshi + Deepali | `DL1` | **2026-09-03** |
| **D3** `CR-016` CE control model | Deepali + Mahesh | `DL1` | **2026-09-05** — blocks M6, not M3 |
| **D4** M2.2 allowlists | Deepali | `DL2` | **2026-09-01** — cheap, and CI is red until it lands |
| **D5** Finding B rotation/retirement | Operator + Deepali | `DL1` | **2026-09-05** — blocks M5.2 |
| **D6** 23 binary assets reviewed | Shailja + a human | `DL2` | before M5.2 |
| **D7** Tag push with tag-write scope | Operator | `DL2` | before the freeze |

### Mahesh — Board 1 Architecture · `A2`

**Support D1.** M3 is module and configuration design, and configuration-driven design is
*precisely* the discipline that absorbs late-arriving facts. Waiting for inputs before designing the
thing whose purpose is to make inputs a data change inverts the method.

**One condition, and it is the architecturally important one.** `CR-016` established that CE cannot
enforce required approvals, CODEOWNERS approval or protected environments. The temptation in M3.4
will be to *not build* the `branch-governance` and `environments` modules, because CE cannot apply
them. That would encode a licensing accident as permanent architecture.

**Model the intent; gate the application.** The modules express the approved governance model — the
risk-based approval matrix, protected environments DEV→DR — and a capability flag decides what is
actually applied against this instance. Then an upgrade to Premium is a flag change, not a redesign,
and the gap is visible in code instead of absent from it. **`C-ARC-6` (proposed):** no M3 module may
be omitted because CE cannot apply it; unavailable capabilities are declared and skipped, not
deleted.

### Deepali — Board 4 Security · one new `S1`

**D4 — I can approve both allowlist entries now**, and recommend it. Both are exact-scope, both are
justified in writing, and the repository's own blocking secret-scan job is red on a false positive
until they land. A control that is red for a non-reason is a control people learn to ignore.

**D2 — and here I have a finding that changes the answer.**

> **`SEC-F07` · `S1` — GitLab-managed Terraform state is circular for the bootstrap.**
>
> M1.6 has an attractive CE-viable answer: GitLab-managed Terraform state, available in Free on
> self-managed. **For `delivery/infrastructure` state that is fine. For the *bootstrap* state it is
> not**, and the reason is structural rather than a matter of degree.
>
> The bootstrap state controls the groups and projects of the estate. If it is stored inside that
> estate, then a bootstrap apply that damages the estate **also damages its own state**, and recovery
> requires the thing that was just destroyed. `IMP-10` and `C-SEC-6` require bootstrap state on a
> separate backend with a separate key and a distinct identity; GitLab-managed state on the same
> instance satisfies none of the three.
>
> **Recommendation: bootstrap state goes to the enterprise S3/KMS backend, or an equivalent outside
> the GitLab instance. `delivery/infrastructure` state may use GitLab-managed state.** This makes
> M1.6 two questions, not one, and only the second is blocking for M3.3.

**D5 — I cannot rotate.** What I can do is specify what "formally retire" must evidence: the value
is absent from every environment variable in every deployment target; no `raw_payload` row decrypts
with it; and the decision is recorded with a named human. Absent all three, B stays
`COMPROMISED / POTENTIALLY LIVE`.

**D3 — not yet.** `CR-016` needs the instance verified, not the documentation. See §3.

### Amit — `R3` Engineering

**Support D1, with a sequencing recommendation that saves rework.** Split M3.4's modules by whether
`CR-016` can change them:

| Build now — CE-independent | Defer until `CR-016` (D3) |
|---|---|
| `gitlab-group`, `gitlab-project`, `labels`, `variables`, `job-token-scope`, `prevent_destroy` guards | `branch-governance` (approval rules), `environments` (protected environments), `memberships` (identity groups need M1.4) |

That is M3.1, M3.2, M3.5, M3.6, M3.7 and roughly half of M3.4 — comfortably the bulk of the phase —
with no risk of writing against a capability the instance lacks.

**M3.8 caution.** The bootstrap pipeline's *protected manual apply* leans on protected environments,
which CE lacks. Build the pipeline; implement the protection as a manual job plus a restricted
runner, and mark it as the compensating control it is rather than the equivalent it is not.

### Swapnali — Board 5 QA · `Q1` hold stands

**No objection to D1.** M3 produces no gate evidence, so a partial start risks no evidence claim.

**Two gaps I want closed before they age**, neither of which blocks M3:

1. **`C-ENG-5` is not satisfied.** M2.8's backend pass ran against the **monorepo**, not the split
   clone. It is roughly thirty minutes and it belongs to M2, not M5, because it is the check that
   proves the split is *buildable* rather than merely well-formed.
2. **Flutter stays `BLOCKED_ENVIRONMENT`.** Not a fail, not a pass, no run. It may be closed only by
   an executed run on a host with a toolchain — `C-QA-5` — and the frontend split is 2 commits, so
   the run is cheap wherever it happens.

**The 23 binary assets are evidence insufficiency, not housekeeping.** `CMP-F02` names screenshots
and diagrams as where this material hides. Unreviewed is not clean.

### Shailja — Board 6 Risk & Compliance · `R2`

**No objection to D1** — M3 writes Terraform in this repository and touches no bank system, so
nothing in it engages a regulatory obligation.

**But do not let the partial start bury `C-CMP-1`.** Residency remains the only open item that can
invalidate the **destination** rather than the schedule, and it is the longest-lead question in the
programme. It gates M5.2, not M3 — so it must be escalated *because* M3 proceeding removes the
pressure that would otherwise have chased it.

**M2.4 — two of the three I can rule on cheaply**, and I recommend: attribute document provenance to
a role rather than a personal address; and accept the bank hostname in the registers as necessary
record-keeping, while noting that the placeholder-to-real transition happened without a decision and
should not recur silently. **The 1SB correspondence needs Product**, not me alone — it is a
commercial-relationship judgement.

### Shivanshi — Board 7 Operations · `O1`

**I own M3 and I can start on D1's approval.** ~29 of 31 agent-hours are available today.

Three things I will not do without their gates: no `filter-repo` (barred on two conditions, neither
met); no `terraform apply` of any kind; and no `backend.tf` until D2 resolves — Deepali's `SEC-F07`
makes writing it now actively wrong rather than merely premature.

**The anchor remains my open operational risk** (`RISK-025`). A rollback anchor that exists only in a
container is not an anchor. It needs a credential with tag-write scope before the freeze.

---

## 3. The one thing every board wants and nobody has

**`CR-016`'s capability table was verified against GitLab's published documentation, not against
`https://gitlab-ce.au.bank.in/`.** The tier structure is durable; **v19.1.2 is newer than the
documentation reachable from this session**, and the instance is authoritative over any published
matrix.

Before D3 is decided, someone with access should confirm on the instance itself:

| # | Check | Decides |
|---|---|---|
| 1 | Do project **merge-request approval rules** exist in Settings → Merge requests? | §6.3, `C-ARC-5` |
| 2 | Does a `CODEOWNERS` file produce a required approver? | §6.2, §6.4 |
| 3 | Does Settings → CI/CD show **Protected environments**? | §9.3, M6.6 |
| 4 | Is **"Pipelines must succeed"** present under Merge checks? | **`S08-G2` — the one that must be true** |
| 5 | Do the `Security/*.gitlab-ci.yml` templates resolve? | `S08-G5` mechanism |
| 6 | Is **Package Registry** enabled? | `ASM-017` second half |
| 7 | Can this team **create a subgroup** under group `820`? | `ASM-013` second half, M4.2 |
| 8 | Is GitLab-managed Terraform state available and where is it stored? | D2, `SEC-F07` |

**Thirty minutes on the instance closes more open questions than a week of waiting.** It is the
single highest-value action available, and it is not an agent's to perform.

---

## 4. Consolidated recommendation

| # | Recommendation | Owner | Blocks |
|---|---|---|---|
| **R1** | **Authorise the partial M3 start** — M3.1, M3.2, M3.5, M3.6, M3.7, CE-independent half of M3.4, M3.9, M3.10 | Shivanshi + Mahesh + Amit | nothing — start today |
| **R2** | **Run the eight instance checks** in §3 | anyone with instance access | D3, D2, M4.2 |
| **R3** | **Approve the two M2.2 allowlists** and re-scan clean | Deepali | red CI job |
| **R4** | **Split M1.6 in two**: bootstrap state **outside** the instance (`SEC-F07`); `infrastructure` state may be GitLab-managed | Deepali + Shivanshi | M3.3 |
| **R5** | **Rotate or formally retire finding B** against Deepali's three evidence tests | Operator + Deepali | M5.2, `filter-repo` |
| **R6** | **Review the 23 binary assets** — ~1 hour, human | Shailja + a reviewer | M5.2 |
| **R7** | **Push the anchor tag** from a credential with tag-write scope | Operator | the freeze |
| **R8** | **Run `./gradlew test` from the split clone** (`C-ENG-5`) and a Flutter run wherever a toolchain exists | Amit + Swapnali | M2 close |
| **R9** | **Stop treating M1 as closeable.** Track it as a standing dependency with expiry 2026-09-19 | Kalpana | the forecast |

**M0: closed. M1: not closeable here — reframe. M2: closes on R5, R6, R8. M3: can start on R1, today.**

---

## 5. What no board decided

`CR-016` · finding B's disposition beyond what is already ruled · residency · the bank Appendix C
exception · any `GATE-S08` criterion. Each remains with its named owner, and no recommendation in
§4 substitutes for one.
