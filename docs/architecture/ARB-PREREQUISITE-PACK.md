# ARB prerequisite pack — R0 Insurance Distribution Platform (NIP)

**Document:** `ARB-PRE-2026-09-14`  
**Workstream:** WS-3 (platform) · WS-1 (1SB supplier) · WS-2 (workforce identity)  
**Horizon:** **H0 / R0** — the proven assisted Life slice. H1–H3 are direction, not this submission.  
**Owner (draft):** Mahesh — Principal Insurance Platform Architect (Board 1 / R2)  
**Origin:** human request to take the bank ARB with the listed prerequisites · triage `SUG-20260914-arb`  
**Status:** `AI-DRAFTED` evidence pack. **This file does not constitute ARB approval, T4 Architecture sign-off, Board 4 Security sign-off, or Board 6 Compliance sign-off.**

**Reviewer formats (do not walk into ARB with Markdown):** see
[`arb-prerequisites/exports/WHAT-TO-SEND.md`](./arb-prerequisites/exports/WHAT-TO-SEND.md).

| ARB asks for | File |
|---|---|
| **First sitting — open this in the room** | [`AU-NIP-R0-ARB-First-Review-DEV-UAT-2026-09-14.pptx`](./arb-prerequisites/exports/AU-NIP-R0-ARB-First-Review-DEV-UAT-2026-09-14.pptx) (`SUG-20260915-vis` · ~35 min visual walk) |
| Presenter script | [`AU-NIP-R0-ARB-First-Review-SCRIPT-2026-09-14.pdf`](./arb-prerequisites/exports/AU-NIP-R0-ARB-First-Review-SCRIPT-2026-09-14.pdf) · [Word](./arb-prerequisites/exports/AU-NIP-R0-ARB-First-Review-SCRIPT-2026-09-14.docx) |
| FAQ + deferral plays | [`AU-NIP-R0-ARB-First-Review-FAQ-2026-09-14.xlsx`](./arb-prerequisites/exports/AU-NIP-R0-ARB-First-Review-FAQ-2026-09-14.xlsx) |
| First-review kit zip | [`AU-NIP-R0-ARB-First-Review-Kit-2026-09-14.zip`](./arb-prerequisites/exports/AU-NIP-R0-ARB-First-Review-Kit-2026-09-14.zip) |
| 19-row dashboard (leave-behind, not the hour) | [`AU-NIP-R0-ARB-Walk-In-2026-09-14.pptx`](./arb-prerequisites/exports/AU-NIP-R0-ARB-Walk-In-2026-09-14.pptx) |
| Circulate / print | [`AU-NIP-R0-ARB-Prerequisite-Pack-2026-09-14.pdf`](./arb-prerequisites/exports/AU-NIP-R0-ARB-Prerequisite-Pack-2026-09-14.pdf) |
| Word comments | [`AU-NIP-R0-ARB-Prerequisite-Pack-2026-09-14.docx`](./arb-prerequisites/exports/AU-NIP-R0-ARB-Prerequisite-Pack-2026-09-14.docx) |
| Matrices / RACI / SBOM / CIS | [`AU-NIP-R0-ARB-Matrices-2026-09-14.xlsx`](./arb-prerequisites/exports/AU-NIP-R0-ARB-Matrices-2026-09-14.xlsx) |
| Leave-behind zip | [`AU-NIP-R0-ARB-Reviewer-Pack-2026-09-14.zip`](./arb-prerequisites/exports/AU-NIP-R0-ARB-Reviewer-Pack-2026-09-14.zip) |

Regenerate: `python3 scripts/architecture/build_arb_first_review.py` (first sitting) then `python3 scripts/architecture/build_arb_reviewer_pack.py` (19-row leave-behind). The generated files compile; ADRs / LLD / NFR still win (`HA-02`).

> **How to use this in the ARB room.** Each row below is one bank prerequisite. *Canonical source*
> is what the board should open. *This pack* only compiles, classifies readiness, and names the
> human who still has to sign. Rule `HA-02`: if a paragraph here disagrees with an ADR, LLD, NFR
> catalogue or security architecture, **that source wins**.

**Companions already in the repository (do not duplicate):**

| Role in the room | File |
|---|---|
| Narrative dossier | [`ARB-ARCHITECTURE-DOSSIER.md`](./ARB-ARCHITECTURE-DOSSIER.md) |
| Solution architecture (SAD) | [`../platform/ws3-platform/03-solution-architecture-r0.md`](../platform/ws3-platform/03-solution-architecture-r0.md) |
| Stakeholder HLD | [`R0-HLD.md`](./R0-HLD.md) |
| AWS LLD / BOM | [`R0-LLD.md`](./R0-LLD.md) |
| Target-state doctrine | [`../context/roles/mahesh-principal-insurance-platform-architect/09-target-state-architecture-doctrine.md`](../context/roles/mahesh-principal-insurance-platform-architect/09-target-state-architecture-doctrine.md) |
| Security architecture + threat model | [`../platform/ws3-platform/04-security-architecture.md`](../platform/ws3-platform/04-security-architecture.md) |
| NFR numbers | [`../platform/ws3-platform/05-nfr-catalogue.md`](../platform/ws3-platform/05-nfr-catalogue.md) |
| Data classification | [`../platform/ws3-platform/02-information-model.md`](../platform/ws3-platform/02-information-model.md) §2 |
| Identity / IAM (application) | [`../platform/authentication-authorization/README.md`](../platform/authentication-authorization/README.md) · `ADR-020` |
| Live bank estate (attach, don't clone) | [`../context/roles/mahesh-principal-insurance-platform-architect/18-au-bank-enterprise-cloud-estate.md`](../context/roles/mahesh-principal-insurance-platform-architect/18-au-bank-enterprise-cloud-estate.md) · `BE-01` |

**Gap-fill papers in this folder** (content that was not previously indexed for ARB):

| Paper | Prerequisite it closes |
|---|---|
| [`arb-prerequisites/CIS-CLASSIFICATION-PROPOSAL.md`](./arb-prerequisites/CIS-CLASSIFICATION-PROPOSAL.md) | Critical Information System classification — **proposal only** |
| [`arb-prerequisites/INTEGRATION-AND-DEPENDENCY-MATRIX.md`](./arb-prerequisites/INTEGRATION-AND-DEPENDENCY-MATRIX.md) | Integration and dependency matrix |
| [`arb-prerequisites/CLOUD-SHARED-RESPONSIBILITY.md`](./arb-prerequisites/CLOUD-SHARED-RESPONSIBILITY.md) | Cloud architecture and shared-responsibility matrix |
| [`arb-prerequisites/TECHNOLOGY-LIFECYCLE-AND-EXIT.md`](./arb-prerequisites/TECHNOLOGY-LIFECYCLE-AND-EXIT.md) | Technology lifecycle / EOS · exit, portability, decommissioning |
| [`arb-prerequisites/SBOM-RUNTIME-INVENTORY.md`](./arb-prerequisites/SBOM-RUNTIME-INVENTORY.md) | Software Bill of Materials / dependency inventory (from Gradle lockfiles) |

---

## 0. Readiness dashboard

Legend: **READY** = source exists and is internally consistent, still AI-drafted pending the named human. **PARTIAL** = source exists but a named gap would be a fair ARB hold. **HUMAN-REQUIRED** = architecture cannot complete this; a bank process or sign-off is missing. **NOT-YET** = not claimed; planned at a named stage.

| # | Bank ARB prerequisite | Readiness | Canonical source | Named gap / owner |
|---|---|---|---|---|
| 1 | Business ask and sign-off for new technical or application requirement | **PARTIAL** | `R0-ASSISTED-LIFE-SALE` in [`BOOT.md`](../context/BOOT.md) §5 · [`CR-010`](../governance/change-requests/CR-010-context-module-and-safe-autopilot.md) · [`CR-015`](../governance/change-requests/CR-015-ws3-r0-savings-ulip-journey.md) · IRDAI CA `CA0515` (dossier §1) | **Rajal** Product sign-off of CAP-A* volumes and CR-015 T4. PO counter-signature on GOV-004 still outstanding. This pack does **not** invent a business BRD signature. |
| 2 | Critical Information System classification | **HUMAN-REQUIRED** | [CIS proposal](./arb-prerequisites/CIS-CLASSIFICATION-PROPOSAL.md) | **Shailja + bank InfoSec / CISO**. Architecture proposes **Critical**. The bank CIS register is the SoT. |
| 3 | Solution Architecture Document | **READY** (unsigned) | [`03-solution-architecture-r0.md`](../platform/ws3-platform/03-solution-architecture-r0.md) · HLD · LLD · dossier | Human T4 Architecture. |
| 4 | Current-state and target-state architecture | **READY** (unsigned) | Current = R0 HLD/LLD/`hdl.svg` R0 cut. Target = [`09`](../context/roles/mahesh-principal-insurance-platform-architect/09-target-state-architecture-doctrine.md) H0–H3 + [`hdl.svg`](../hdl.svg) | Do not read H1–H3 as admitted scope. |
| 5 | Data-flow and trust-boundary diagrams | **READY** (unsigned) | [`04-security-architecture.md`](../platform/ws3-platform/04-security-architecture.md) §2 TB-1…TB-7 · [`r0-platform-topology.svg`](./r0-platform-topology.svg) · [`r0-platform-payment.svg`](./r0-platform-payment.svg) · [`R0-E2E-FOR-DEVELOPERS.md`](./R0-E2E-FOR-DEVELOPERS.md) | Split plane `ADR-020` is now on topology/payment. Security mermaid in §2 of this change aligns TB-5/TB-7 to Apigee. Deepali still owns the threat-model signature. |
| 6 | Integration and dependency matrix | **READY** (compiled) | [matrix](./arb-prerequisites/INTEGRATION-AND-DEPENDENCY-MATRIX.md) · [`DEPENDENCY-REGISTER.md`](../governance/registers/DEPENDENCY-REGISTER.md) · LLD §8 | External rows OPEN: Apigee onboard (`DEP-20260914-apg`), VPN/DX (`DEP-20260824-dx1`), bank UAT slot (`DEP-002`). |
| 7 | Infrastructure sizing and capacity assessment | **PARTIAL** | [`05-nfr-catalogue.md`](../platform/ws3-platform/05-nfr-catalogue.md) §2 CAP-A* · LLD §1.4 shapes | CAP-A1…A7 are **assumptions**, not an approved business baseline (`NFR-OPEN-1`, owner Rajal). R0 is correctness-constrained, not throughput-constrained. |
| 8 | Availability, RTO and RPO of base product | **READY** (unsigned) | NFR-AVL-01…07 · NFR-DR-01…06 | Sale-path **99.9%**. RTO ≤ 1 h, RPO ≤ 5 min transactional, **RPO 0** audit. **Unproven** until S09 drills (`NFR-DR-04`). |
| 9 | DR architecture and failover mechanism | **READY** (unsigned) | LLD §11 · [`r0-platform-dr.svg`](./r0-platform-dr.svg) · `ADR-009` D16 | Warm standby `ap-south-2`. Cache/MSK/OpenSearch **not** replicated (D13–D15). Proof is a timed Ansible drill, not the picture. |
| 10 | Security architecture (as per Security NFR) | **PARTIAL** | [`04-security-architecture.md`](../platform/ws3-platform/04-security-architecture.md) · [`architecture-review/06-security-compliance-and-nfrs.md`](../platform/architecture-review/06-security-compliance-and-nfrs.md) · NFR-SEC | **Deepali** Board 4 human. `ADR-010` remainder (pod→Apigee inspection) is her acceptance, not Architecture's. |
| 11 | VA/PT and secure-code-review | **NOT-YET** | GitLab CI: Checkstyle, Spotless, ArchUnit, JaCoCo; Trivy/lockfiles (`build.gradle.kts` S08-E04) | No VA/PT report exists. Swapnali + Deepali: SAST in pipeline now; DAST/VA/PT **before UAT exit / S11–S12**. Do not tell ARB a pentest passed. |
| 12 | SBOM / software dependency inventory | **PARTIAL** | [lockfile inventory](./arb-prerequisites/SBOM-RUNTIME-INVENTORY.md) · 14 lockfiles, 137 runtime coordinates | CycloneDX/SPDX **CI artefact** still to be wired (S09). Flutter `pubspec.lock` is a second inventory. |
| 13 | Data classification and data residency | **READY** (unsigned) | Information model §2 · TI-08 · `FF-08` · India `ap-south-1` / DR `ap-south-2` only | **Shailja** S02-G5/G6. GAP-016 residual on attribute-level BA ratification. |
| 14 | IAM / PAM design | **PARTIAL** | AuthN/AuthZ SSOT · LLD IRSA · `ADR-020` AD-verify · bank IAM Identity Center (estate `18`) | Application IAM is designed. **Privileged Access Management** for AWS console / break-glass is bank PAM + Deepali; not a second IdP we invent. |
| 15 | Logging and monitoring design | **READY** (unsigned) | LLD P6 · `ADR-013` OpenSearch · CloudTrail vs CloudWatch (dossier §9.3) · NFR-OBS | Operational logs `RET-OPERATIONAL` 90 d. Audit is **not** logging (`TI-07`). |
| 16 | Third-party and subcontractor details | **READY** (compiled) | [matrix §2](./arb-prerequisites/INTEGRATION-AND-DEPENDENCY-MATRIX.md#2-third-parties-and-subcontractors) | 1SB is a **provider route**, not a domain dependency (`TI-04`). Insurers underwrite (`TI-03`). |
| 17 | Cloud architecture and shared-responsibility matrix | **READY** (compiled) | [matrix](./arb-prerequisites/CLOUD-SHARED-RESPONSIBILITY.md) · LLD · `ADR-018`/`ADR-020` · estate `18` | Attach to `AU-CTO-NETWORK`; five Control Tower accounts; no CUG. |
| 18 | Technology lifecycle and end-of-support assessment | **READY** (compiled) | [lifecycle](./arb-prerequisites/TECHNOLOGY-LIFECYCLE-AND-EXIT.md) | Java 21 and Spring Boot 3.5 are in support. Keycloak / Flutter / EKS pin to be confirmed by Amit + Shivanshi at S09. |
| 19 | Exit, portability and decommissioning strategy | **READY** (compiled) | [same paper §2](./arb-prerequisites/TECHNOLOGY-LIFECYCLE-AND-EXIT.md#2-exit-portability-and-decommissioning) | 1SB replaceable at the adapter. Data exits as Aurora + S3 WORM in India. Kubernetes PVC is not a SoR. |

**Honest ARB posture:** the design is internally consistent enough to **review**. It is **not** internally consistent enough to **approve as production authority**. The holds that would be fair: CIS register (row 2), Product volume sign-off (row 7), Deepali Board 4 (row 10), VA/PT plan date (row 11), Apigee written IPs (row 6), human T4 Architecture.

---

## 1. Business ask and sign-off

**Ask (admitted objective):** one RM, acting as certified Specified Person, sells a complete Life policy (Term or Savings/ULIP) to one ETB customer of one Group A insurer, end to end, with suitability, customer-device OTP consent, customer-device payment, reconciled issuance and immutable audit (`R0-ASSISTED-LIFE-SALE`, CR-015 transcribed).

**What is already a written ask:**

- Licence context: IRDAI Composite Corporate Agency `CA0515` ([dossier](./ARB-ARCHITECTURE-DOSSIER.md) §1).
- Scope in / out: [`BOOT.md`](../context/BOOT.md) §5 (generated from `CURRENT-STATE.yaml`).
- Workstream registration: CR-010. Savings/ULIP restatement: CR-015 (**CANDIDATE transcribed; HUMAN T4 outstanding**).

**What this pack does not do:** fill a bank “new application requirement” signature block, a budget memo, or a Product Owner counter-signature on GOV-004. Rajal owns that process. Architecture will not stamp it.

---

## 2–4. CIS, SAD, current vs target

See the dashboard. Current-state is **H0/R0 as designed** (not “as running in prod” — nothing is in a bank prod account yet). Target-state is the North Star capability model at horizons H1–H3; release strategy follows business maturity, not architecture completeness (`09` HR-01…HR-05, `VIN-001` §36).

---

## 5. Data-flow and trust-boundary diagrams (index)

| Flow | Picture |
|---|---|
| RM / NIP-APP inbound | Cloudflare → F5-XC → API Gateway → Internal ALB → nip-web / NIP BFF (`ADR-018`) |
| Provider outbound | Hub → `#15` adapter → Apigee → 1SB (`ADR-020`). 1SB allowlists **Apigee IPs**. |
| Internal bank API | Adapter / Customer → Apigee **private** → AD-verify / EBS. Forbidden: Cloudflare/F5 hairpin. |
| Payment | Session-create **out** via Apigee; customer 3-DS on own device; callback **in** on a separate API Gateway route (`TB-6`). |
| Trust boundaries | TB-1…TB-7 in [`04-security-architecture.md`](../platform/ws3-platform/04-security-architecture.md) §2 |

---

## 6–9. Integration, capacity, availability, DR

Indexed in the dashboard. Capacity finding that ARB should hear first: **~100 journey starts/hour BAU, ~7/minute at Q4 peak** (CAP-A*). Three MSK brokers, a cache pair and a search domain are sized for **availability and evidence**, not throughput. Do not scale from CPU (`AGENTS.md` / NFR-THR-06 Aurora connection budget is the one that bites).

---

## 10–12. Security, VA/PT, SBOM

Security architecture is an **input to Board 4**, not a Board 4 verdict ([`04-security-architecture.md`](../platform/ws3-platform/04-security-architecture.md) header). VA/PT is **NOT-YET**. SBOM inventory is generated from the lockfiles that already exist so Trivy can see the Java estate.

---

## 13–15. Data, IAM/PAM, logging

Classification vocabulary (`PUBLIC` / `INTERNAL` / `CONFIDENTIAL` / `RESTRICTED` + `[P][F][H][K]`) and residency (India only, `TI-08`) are in the information model. Workforce identity stays Bank AD (`TI-01`); R0 reaches it via the existing AD-verify API through Apigee private (`ADR-020`), never LDAP from EKS. PAM for human privilege on AWS is the bank’s existing PAM / IAM Identity Center — we consume it, we do not clone it (`BE-01`).

Logging: CloudTrail (who changed AWS) and CloudWatch (how the pod behaves) are both mandatory. OpenSearch holds **operational** logs, never evidence (`ADR-013`). Audit is INSERT-only + S3 Object Lock (`TI-07`).

---

## 16–19. Third parties, cloud RACI, lifecycle, exit

See the four papers in `arb-prerequisites/`. One-line exit: **turn off the adapter, keep the evidence**. 1SB JSON never enters a domain schema. WORM archives stay in India until retention expires.

---

## Sign-off routing (who must still walk into ARB)

| Seat | What they sign | What they must not be asked to sign for Architecture |
|---|---|---|
| Rajal (R1) | Business ask, CAP-A* baseline, CR-015 product acceptance | Technology choice |
| Mahesh (R2) **human T4** | SAD / HLD / LLD / this pack as architecture evidence | Deepali’s security residual, Shailja’s CIS class |
| Deepali (R8) **human** | Board 4, `ADR-010` remainder, VA/PT scope, PAM pattern | Product behaviour |
| Shailja (R9) **human** | CIS class, classification/retention, residency attestation | A pentest result |
| Shivanshi (R10) | S09 vending, Apigee onboard, DR drill **records** | Architecture structure |
| Aarti | Physical model / PITR / WORM restore | Service boundaries |
| Swapnali (R7) | Evidence sufficiency of VA/PT and SAST | Waiving a non-waivable control |
| Kalpana (R12) | Whether ARB is on the critical path this increment | The content of a specialist verdict |

Agents draft; they do not sign.
