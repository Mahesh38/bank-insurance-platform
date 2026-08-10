# 07 — Delivery Roadmap & Timeline Estimate

## Caveat first

No existing document in this repository commits to a delivery timeline — the business SSOT deliberately scopes *what* (MVP = Life only, ETB, Group A+B, RM+self+hybrid) and leaves *when* open pending sponsor validation. The estimate below is this review's own engineering judgment given the target architecture in [02](./02-target-microservices-architecture.md)–[06](./06-security-compliance-and-nfrs.md), the number of services, and typical enterprise banking delivery velocity. Treat it as a planning input, not a committed date — it should be pressure-tested against actual team size once staffed.

## Assumptions the estimate depends on

- **Team shape:** 4–5 cross-functional squads (backend + QA + a shared DevOps/platform squad), each squad ~6–8 people, running in parallel from Phase P1 onward. Fewer squads stretch the timeline roughly linearly; this is the single biggest lever on the number below.
- **Foundation work happens once, up front**, not repeated per squad (landing zone, EKS cluster, CI/CD, shared libraries, security baseline).
- **The existing `1sb-integration-service` and shared libraries (`bank-common-*`) are reused as-is** as the Integration Hub's first adapter — not rewritten. This materially shortens P0.
- **Compliance items still marked "pending" in `DECISION-LOG.md`** (consent model, IRDAI/RBI mapping, PII/audit retention) are assumed to resolve **during** Phase P0/P1, not blocking kickoff — if they slip, they push P2 (compliance hardening) directly.
- Estimate excludes UAT sign-off cycles with the bank's own compliance/risk committees, which are organizationally-paced, not engineering-paced — add buffer per the bank's actual governance calendar.

## Phased plan and duration

| Phase | Scope | Duration | Depends on |
|-------|-------|----------|-------------|
| **P0 — Foundation** | AWS landing zone (multi-account, VPC, EKS clusters per env), CI/CD (ECR + CodePipeline/GitHub Actions + Argo CD), shared libraries hardened for multi-service reuse, service mesh + observability stack stood up, Identity & Access + Customer + Product Catalogue (Term only) + Journey Orchestration + Integration Hub wired to the existing 1SB Adapter, both BFFs | **6–8 weeks** | Nothing — this is day one |
| **P1 — Core sale path (Group A)** | Suitability & Recommendation, Consent, Quotation, Proposal & UW-Tracking, Payment, Policy & Issuance — full Term Life journey, RM-assisted first, then self-service/hybrid modes on the same journey engine | **10–14 weeks** | P0 complete; 1SB UAT credentials provisioned (external dependency called out in `R0-SCOPE.md` §6) |
| **P2 — Compliance & ops hardening** | Audit & Compliance service live end to end, Notification, Administration & Config, full security review, DR drill, load test to declared NFR targets | **8–10 weeks** | P1's events exist to audit; can start in parallel with the tail of P1 |
| **P3 — Extend & harden for launch** | Lead module, ULIP/Savings-Investment LOB variants, Group B redirect flow, Reporting/MIS dashboards, UAT with real RM/branch users, go/no-go | **6–8 weeks** | P1+P2 stable |

**Rough total: ~30–40 weeks (≈ 7–9 months) to MVP go-live**, from a standing start with the foundation phase, assuming the parallel-squad staffing above and no major compliance surprises.

## What moves this number

| Factor | Effect |
|--------|--------|
| Fewer than 4 squads | Add roughly 1 week of calendar time per week of squad-capacity shortfall past P0 — this is the dominant variable |
| 1SB UAT/production credential delays (external, bank + 1SB relationship, not engineering) | Directly extends P1 start; this is the same external dependency already flagged in `R0-SCOPE.md` |
| IRDAI/RBI compliance mapping resolved late | Pushes P2 out; consent/audit design was built config-first specifically to absorb this without a rebuild, but sign-off still gates go-live |
| Direct-insurer adapter (Phase B replaceability) pulled into MVP scope | Currently P3+/post-MVP; pulling it earlier adds a full adapter-build cycle, roughly 4–6 weeks |
| Branch kiosk journey (pending business decision per `DECISION-LOG.md`) | Not estimated here — out of scope until decided |

## Recommended next steps (in order)

1. Staff the foundation squad and start **P0** immediately — none of it is blocked by open business/compliance questions.
2. In parallel, push the **pending compliance items** (consent model, data residency, retention periods) to closure — they gate P2, not P0/P1, but the longer they stay open the more P2 risk compounds.
3. Confirm 1SB UAT/production distributor credentials are provisioned early — it's on the critical path for P1 and outside engineering's control to accelerate.
4. Revisit this estimate once squads are actually staffed — the number above is a planning input, not a commitment.
