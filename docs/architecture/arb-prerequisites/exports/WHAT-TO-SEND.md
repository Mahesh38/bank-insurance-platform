# What to send the ARB reviewer

Markdown in this repository is for the delivery team. **Do not walk into ARB with `.md` files.**

This sitting is the **first review** (Dev / UAT environment shape). It is **not** production go-live and **not** DR-ready sign-off.

**How to run the hour:** talk **~35 minutes**, leave **~25 minutes** for the board. Slides are billboards — the argument is in presenter notes and the FAQ workbook.

| If they ask for… | Give them this file |
|---|---|
| **Open this in the room** (presenter deck + speaker notes) | `AU-NIP-R0-ARB-First-Review-DEV-UAT-2026-09-14.pptx` |
| Presenter script (print / second screen) | `AU-NIP-R0-ARB-First-Review-SCRIPT-2026-09-14.pdf` (Word: `.docx`) |
| FAQ + five deferral plays | `AU-NIP-R0-ARB-First-Review-FAQ-2026-09-14.xlsx` |
| First-review kit in one mail | `AU-NIP-R0-ARB-First-Review-Kit-2026-09-14.zip` |
| Leave-behind 19-row pack (print / PDF) | `AU-NIP-R0-ARB-Prerequisite-Pack-2026-09-14.pdf` |
| Word file they can comment on | `AU-NIP-R0-ARB-Prerequisite-Pack-2026-09-14.docx` |
| Matrices, RACI, SBOM, CIS table | `AU-NIP-R0-ARB-Matrices-2026-09-14.xlsx` |
| 19-row dashboard slides (do **not** present as the hour) | `AU-NIP-R0-ARB-Walk-In-2026-09-14.pptx` |
| Leave-behind zip | `AU-NIP-R0-ARB-Reviewer-Pack-2026-09-14.zip` |

AI-DRAFTED evidence. Not ARB approval, not T4 Architecture, not Board 4, not Board 6 CIS registration, not a VA/PT pass. HA-02: ADR / LLD / NFR win.

Regenerate:

```bash
python3 scripts/architecture/build_arb_first_review.py     # first-sitting PPT + script + FAQ
python3 scripts/architecture/build_arb_reviewer_pack.py    # 19-row leave-behind
```

HA-02: ADR / LLD / NFR still win if a generated cell disagrees.
