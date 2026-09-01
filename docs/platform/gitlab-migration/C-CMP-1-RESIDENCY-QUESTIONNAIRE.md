# C-CMP-1 — Data residency questionnaire

**Status:** Drafted 2026-09-01 for bank infrastructure. **Not a Board 6 ruling.**
**Condition:** [`CR-014` Board 6 `C-CMP-1`](../../governance/change-requests/CR-014/verdicts/board-6-compliance-shailja.md)
**Owner of the ruling:** Shailja (Board 6). **Owner of the ask:** Shivanshi (SRE).
**Authorised:** [`DEC-20260901-01`](../../governance/DEC-20260901-01-owner-authorises-unblock-path.md)

A `.in` hostname is an indicator, not evidence (`ASM-022`). Send this as a closed questionnaire.
Do not push to GitLab until Shailja records `PERMISSIBLE` against the answers.

## Questions

Reply with location as **DC / city / AWS region** (or "not used"). "India" without a region or
site is not enough.

1. Where do the GitLab application servers run?
2. Where do repository disks (Gitaly / NFS / equivalent) live?
3. Where are CI job logs stored?
4. Where are CI job artifacts stored?
5. Where is Container Registry blob storage?
6. Where will Terraform state for `gitlab-bootstrap` live? (Must be **outside** this instance — `C-SEC-6` / `RISK-027`.)
7. Where do backups and replicas of (1)–(6) live?
8. Is any object store, CDN, or replica outside AWS India regions (`ap-south-1`) or an on-premises India DC?

## How Board 6 will rule

| Answers | Ruling |
|---|---|
| Self-managed in an India bank DC, or AWS `ap-south-1`, including backups | Eligible for `PERMISSIBLE` |
| GitLab.com, or disks / backups / registry / artifacts outside India | Destination **invalid**. Do not push. Do not "migrate first and relocate later" (`RISK-021`) |

## Evidence file (after the ruling)

When Shailja rules, place a **human-signed** file at `evidence/C-CMP-1.signed` (or
`${EVIDENCE_DIR}/C-CMP-1.signed`). The migrate script will not `PUSH=1` without it.
An agent must not create that file.
