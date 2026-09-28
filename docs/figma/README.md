# Figma exports — frontend reference for BFF evaluation

**Owner:** Product / BA  
**Authority:** Reference only (`D-012`). Not behaviour SSOT.  
**Why this folder exists in a Java repository:** [`DOC-006`](../au-bank-insurance-platform/DECISION-LOG.md) — this git repository does not hold NIP-APP / Flutter source. Workforce BFF contracts are evaluated against these exports plus the module BRD.

Prototype (login-walled): <https://www.figma.com/proto/JyLGAaO88ELjnyVF2FQ3Bx/For-Client-Review?node-id=208-9666&page-id=208%3A2982>

Intake log: [`../au-bank-insurance-platform/05-figma-and-artefact-intake.md`](../au-bank-insurance-platform/05-figma-and-artefact-intake.md).

## How to use

1. Read the **module BRD** first (`docs/au-bank-insurance-platform/requirements/brd-detailed/`).
2. Use the PNG here to see screen layout, field order and states the BFF must support.
3. When Figma and the BRD disagree, **the BRD wins**. Do not implement a Figma-only control.

Login evaluation: [`../au-bank-insurance-platform/requirements/LOGIN-BFF-FIGMA-EVALUATION.md`](../au-bank-insurance-platform/requirements/LOGIN-BFF-FIGMA-EVALUATION.md).

## Inventory

| Path | Journey | Notes vs BRD |
|------|---------|--------------|
| `wireframe/auth-login-2fa-forgot-password-mpin.png` | Login / OTP / recovery | Filename still shows Forgot Password and mPIN. Login BRD **removed** Forgot Password; mPIN is not in the Login BRD. OTP is mandatory every login. |
| `wireframe/lead-creation-*.png` | Lead | Lead BRD is SSOT for assignment timing (`D-016`). |
| `wireframe/product-listing-*.png` | Savings listing / compare | Listing BRD §21 still has TBDs. |
| `wireframe/product-detail-page-savings-plan.png` | Product details | Toolkit / Sample BI lock superseded by `D-015` (final BI). |
| `Visual Design/*.png` | Visual layer | Not applied; layout reference only. |

`ADR-015` still stands: one NIP-APP client (web + APK + IPA). That source is **not** in this repository.
