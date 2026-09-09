# GitLab Phase 1 — collaboration brief (max 4 pages)

**Shareable Word (send this):** [`AU-SFB-NIP-GitLab-Phase-1-Collaboration-Brief.docx`](./AU-SFB-NIP-GitLab-Phase-1-Collaboration-Brief.docx)  
**Later phases (do not send as the Phase 1 ask):** [`AU-SFB-NIP-GitLab-DevOps-Work-Order.docx`](./AU-SFB-NIP-GitLab-DevOps-Work-Order.docx) · [`GITLAB-BANK-DEVOPS-PROVISIONING.md`](./GITLAB-BANK-DEVOPS-PROVISIONING.md)

Regenerate: `python3 scripts/platform/generate-gitlab-phase1-brief.py`

This markdown is the agent copy of the four-page brief. Bank DevOps should receive the Word file.

## Phase 1 ask

Create **three empty GitLab projects**, grant Developer access, protect `main`, attach a runner. The application team will commit via merge request. Do **not** start Terragrunt, Terraform, AWS, CD, or extra microservice shells.

| Phase | Goal | DevOps | Application team |
|---|---|---|---|
| **1 Now** | Start collaboration | Group + 3 projects + access + protect `main` + Java 21 / Flutter runners | First MRs with code |
| **2 Next** | Foundation gates | CI templates, secret/SAST/SCA/SBOM, coverage blockers, SonarQube | Keep pipelines green |
| **3 Later** | CD and cloud | Terragrunt/Terraform, AWS India, digest promotion | Hand over digests only |

## Create

Top group: `au-bank-insurance-platform` (Internal).

| Path | Purpose |
|---|---|
| `backend/nip-backend` | Java 21 Gradle monorepo |
| `frontend/nip-app` | One Flutter NIP-APP |
| `platform/nip-governance` | Programme docs |

Protect `main`: MR required, ≥1 approval, pipeline must succeed. Phase 1 jobs: `java:test-and-coverage`, `flutter:test`, `governance:ci-checks`.

## Acceptance

1. Group URL exists, not public.  
2. Three project URLs.  
3. Named developers have Developer.  
4. `main` cannot be pushed directly.  
5. One named developer can clone and branch.  
6. Runners live, or a dated plan.

Work item: `SUG-20260907-gdv`.
