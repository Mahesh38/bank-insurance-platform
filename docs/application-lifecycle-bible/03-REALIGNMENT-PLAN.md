# 03 — Realignment Plan: Underpinning a Building That Is Already Seven Floors Up

**Author:** Rajal (Product Owner), with findings for Architecture, Security, Compliance, QA, SRE
and Delivery to ratify
**Depends on:** [`01-POSITION-ASSESSMENT.md`](./01-POSITION-ASSESSMENT.md)
**Status:** proposed under CR-010

---

## 1. The governing decision: underpin, do not demolish

In construction, when a standing building is found to have inadequate foundations, you do not
demolish it. You **underpin** it: excavate beneath the existing structure in controlled sections,
pour new foundations under the load-bearing points, transfer the load, and move to the next
section. The building stays up. People keep working in it. It costs more than building the
foundation first would have — and far less than starting again.

That is exactly the strategy here, and the reason is commercial, not sentimental. The 1SB
integration service is roughly 147 files of correct, boundary-enforced, architecturally sound
work. The governance framework is better than most enterprises manage. The discovery corpus took
months. **Demolition would destroy real value to solve a sequencing problem.**

> **The realignment principle:** *Nothing already built is deleted. Everything already built is
> re-parented under a foundation that should have existed first, and re-certified against gates
> that can actually be evidenced.*

---

## 2. The five moves

```
  MOVE 1              MOVE 2             MOVE 3            MOVE 4           MOVE 5
  Stop the      →     Survey       →    Underpin     →   Reconcile   →   Resume the
  ascent              the truth          S08 + S09        the model        ascent
  (1 week)            (2 weeks)          (8-10 weeks)     (parallel)       (S10 →)
```

### Move 1 — Stop the ascent · 1 week

Not a stand-down. A **change of direction of effort**.

| Action | Owner | Why |
|---|---|---|
| WS-1 Phase 5 (Health/Motor LOB expansion) does not start | Rajal + Kalpana | Adding LOBs to a quote path that lacks its lawful suitability gate multiplies a compliance defect across three lines of business |
| WS-1 Phase 4 criteria 4.1 and 4.7 re-stated as `BLOCKED` on a named dependency | Mahesh + Swapnali | They are currently `OPEN`, which implies effort will close them. No amount of effort closes "runs in CI" when there is no CI. `BLOCKED` names the real blocker and makes it schedulable |
| Foundation Recovery Increment authorised | Sponsor + Kalpana | Gives the work a budget line and a name, so it is not competing as "overhead" |
| No new feature merges into `services/` outside the recovery scope | Amit | Every new line added before S08 is another line entering the estate untested |

**Explicitly *not* stopped:** WS-2 workforce IAM foundation work, Phase 4 items 4.4 (compliance
review) and 4.5 (runbook) which need no CI, and all documentation/rule-pack work. Those are
foundation-shaped and continue.

### Move 2 — Survey the truth · 2 weeks, parallel with Move 3 start

You cannot underpin what you have not surveyed. Three deliverables:

| Deliverable | Content | Owner |
|---|---|---|
| **Build-and-test baseline** | Run the full Gradle build locally, record what actually passes, what fails, real coverage per module. First honest number this repository has had | Amit + Swapnali |
| **Estate inventory** | Every service, its bounded context, its true state (built / scaffolded / absent), its dependencies | Mahesh |
| **Evidence audit** | For every "Done" claim in every phase STATUS file, name the artefact that proves it. Claims without artefacts become recovery backlog items | Kalpana + Swapnali |

The evidence audit will be uncomfortable and it is the most valuable of the three. Its output is
the difference between the programme's believed state and its actual state, and that delta is the
recovery increment's real scope.

### Move 3 — Underpin: execute S08 and S09 · 8–10 weeks

The heart of the work. Full epic and story detail in
[`stages/S08-engineering-foundation.md`](./stages/S08-engineering-foundation.md) and
[`stages/S09-platform-foundation.md`](./stages/S09-platform-foundation.md). Sequenced by what
unblocks the most:

**Weeks 1–3 — the pipeline that should have existed on day one**

1. Application CI: build + unit + integration tests on every PR, for every module. This single
   item unblocks Phase 4 criterion 4.1 and turns every future "green" claim into an artefact.
2. Quality gates in CI: JaCoCo thresholds enforced (not merely configured), ArchUnit executed,
   build fails on breach. Closes QA-001's mechanism.
3. Secret scanning and dependency/SCA scanning, failing the build on critical findings.
4. Branch protection: no merge to `main` without a green pipeline.

**Weeks 3–6 — the ability to prove behaviour**

5. Test infrastructure: Testcontainers for PostgreSQL, WireMock for 1SB, a shared test-fixture
   library. Closes TD-014.
6. Contract tests across the integration ↔ persistence seam.
7. E2E harness with the Term path as its first suite — this is Phase 4 criterion 4.1 delivered
   properly rather than asserted.
8. SAST integrated with results triaged into the risk register.

**Weeks 4–9 — the ability to run it (S09, overlapping)**

9. IaC baseline: Terraform for VPC, subnets, security groups, KMS keys, ECR, and the EKS cluster —
   ap-south-1, because data residency is a regulatory obligation, not a preference.
10. Three real environments: dev, UAT, prod, with promotion between them and no manual drift.
11. Secrets management: AWS Secrets Manager wired for real, closing TD-006's stub.
12. Observability substrate: metrics, structured logs with correlation IDs, traces, and the
    PII-masking converter verified by test rather than by comment.
13. Deployment pipeline: image build, sign, scan, deploy, rollback — with rollback exercised.
14. S3 Object Lock configured for the 7-year IRDAI retention obligation.

**Weeks 8–10 — re-certification**

15. Re-run every WS-1 Phase 4 criterion against the new machinery. Criteria that were assertions
    become artefacts. Criteria that fail become backlog, honestly.

### Move 4 — Reconcile the governance model · parallel throughout

Four changes, all requiring CR-010:

| # | Change | Why it matters |
|---|---|---|
| 1 | **Register WS-3 — AU Bank Insurance Distribution Platform** in `CURRENT-STATE.yaml`, current stage S08 | This is the fix for [GAP-D](./01-POSITION-ASSESSMENT.md#gap-d--the-platform-is-not-a-governed-workstream--structural). Until the platform is a workstream, platform foundation work triages as out-of-scope and the model keeps excluding the very thing that is missing |
| 2 | **Re-parent WS-1 as a supplier workstream** feeding WS-3's Integration Hub (context #14) and 1SB Adapter (context #15) | WS-1 stops being the programme and becomes a component of it. Its L7 status stays true — for a component |
| 3 | **Add routing entries** for the new work types the platform generates (UI/UX, IaC, pipeline) | AIGEM's `routing` map has no home for a Flutter story or a Terraform module today, so such work has nowhere legitimate to land |
| 4 | **Bind the S11 entry condition** on GAP-006 and GAP-007 closure | Makes the "build freeze" label on those P0 gaps actually freeze something |

### Move 5 — Resume the ascent

With S08 and S09 passed, the ladder is climbed in order, and for the first time the gates mean
what they say:

```
S10 Integration  →  S11 Vertical Slice  →  S12 Hardening  →  S14 Go-Live
   (CBS, PG,          (ONE complete           (evidence         (with a
    AD, 1SB)           RM-assisted Term        that now          runbook that
                       journey, incl. UI)      exists)           was exercised)
```

**S11 is the stage this programme has never actually attempted**, and it is the one that proves
the business case. One RM, one ETB customer, one Term product, one insurer, end to end: lead →
need analysis → suitability → consent → quote → proposal → payment on the customer's device →
issued policy → reconciled → audited. Through a real UI. That single journey is worth more than
three more LOBs on an adapter.

---

## 3. What this costs, and what it costs not to do it

**Cost of realignment:** roughly 8–10 weeks of foundation work before feature delivery resumes,
plus the retrofit penalty of adding tests and CI to code written without them.

**Cost of continuing as-is** — each of these is already latent in the estate:

| Risk | Consequence |
|---|---|
| Quote path ships without the suitability hard-gate | IRDAI breach on a licensed corporate agency activity. Our own baseline calls bypassing suitability illegal |
| No CI on a regulated financial application | Cannot evidence change control to an auditor. Every release is a manual assertion |
| No IaC, no defined environments | Production deploy is unrepeatable. There is no rollback that has ever been tested |
| Data residency unverified | Potential breach of a hard regulatory boundary, discovered at audit rather than at design |
| 7-year retention not implemented | A statutory obligation with no technical control behind it |
| LOB expansion over a thin test base | Defect rate compounds; each new LOB inherits every unproven assumption |

The realignment is not a delay to delivery. **It is the removal of the reason delivery cannot be
certified.**

---

## 4. Sequencing constraint that must not be violated

> **Do not build the sixteen missing bounded contexts before S08 and S09 pass.**

The instinct on seeing "16 of 19 contexts missing" is to start writing services. That would
repeat the original error at four times the scale: sixteen more services with no CI, no
environments, no test infrastructure, and no deployment path.

Order is: **foundation → one journey through few services → then breadth.** The nineteen contexts
are a target architecture, not a build order. S11 needs perhaps six of them thinly implemented —
Lead, Consent, Suitability, Journey Orchestration, plus the existing Quote path and Identity — to
prove the business case end to end. The other ten come at S13, justified by the working slice
rather than by the diagram.

---

## 5. Decision register entries this plan generates

To be recorded in [`registers/DECISION-REGISTER.md`](../governance/registers/DECISION-REGISTER.md)
on ratification:

| ID | Decision | Owner |
|---|---|---|
| CR-010 | Adopt the 16-stage lifecycle model and this realignment plan | Mahesh + Rajal |
| GOV-00x | Register WS-3 as the primary workstream; re-parent WS-1 as supplier | Mahesh |
| GOV-00x | Foundation Recovery Increment authorised; feature freeze on `services/` outside its scope | Kalpana + Sponsor |
| ADR-001 | IaC on Terraform targeting AWS ap-south-1; Render.com is dev-preview only, never a data path for PII | Mahesh + Shivanshi + Deepali |
| RISK-01x | Suitability hard-gate absent from a delivered quote path — regulatory exposure until S11 | Shailja |
| RISK-01x | No CI on application code — change-control evidence gap until S08 | Deepali + Swapnali |

---

## 6. The one-sentence version

> **Stop climbing, register the platform as the thing we are actually building, pour the two
> missing foundation floors underneath the work that already exists, close the two P0 compliance
> gaps that make the built path unshippable, and then prove one complete business journey end to
> end before adding a single further line of business.**
