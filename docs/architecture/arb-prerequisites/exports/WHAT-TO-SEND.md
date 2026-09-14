# What to send the ARB reviewer

Markdown in this repository is for the delivery team. **Do not walk into ARB with `.md` files.**

| If they ask for… | Give them this file |
|---|---|
| Walk-in slides / presentation | `AU-NIP-R0-ARB-Walk-In-2026-09-14.pptx` |
| Circulated pack / print / PDF | `AU-NIP-R0-ARB-Prerequisite-Pack-2026-09-14.pdf` |
| Word file they can comment on | `AU-NIP-R0-ARB-Prerequisite-Pack-2026-09-14.docx` |
| Matrices, RACI, SBOM, CIS table | `AU-NIP-R0-ARB-Matrices-2026-09-14.xlsx` |
| All four in one mail | `AU-NIP-R0-ARB-Reviewer-Pack-2026-09-14.zip` |

AI-DRAFTED evidence. Not ARB approval, not T4 Architecture, not Board 4, not Board 6 CIS registration, not a VA/PT pass. HA-02: ADR / LLD / NFR win.

Regenerate after the markdown pack changes:

```bash
python3 scripts/architecture/build_arb_reviewer_pack.py
```

HA-02: ADR / LLD / NFR still win if a generated cell disagrees.
