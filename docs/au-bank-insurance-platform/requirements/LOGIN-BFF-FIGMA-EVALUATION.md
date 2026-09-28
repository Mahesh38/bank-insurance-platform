# Login — BFF evaluated against Figma (not an in-repo app)

**Date:** 2026-09-28  
**Work item:** `DOC-006` + Login first module (`Q7`)  
**Status:** Working evaluation. No NIP-APP source in this repository. No new password-collection API in this change.

| Source | Role |
|--------|------|
| [Login BRD v1.0](./brd-detailed/Login_Module_BRD_Detailed_CONTEXT.md) | Behaviour SSOT (`DOC-005`) |
| [`docs/figma/wireframe/auth-login-2fa-forgot-password-mpin.png`](../../figma/wireframe/auth-login-2fa-forgot-password-mpin.png) | Screen layout reference (`D-012`, `DOC-006`) |
| [`workforce-access-bff`](../../../services/workforce-access-bff/README.md) `/api/v1/auth/*` | Java contract to evaluate |
| [UC-01 RM login](../../journey-execution/flows/UC-01-rm-login.md) | Current architecture path (OIDC, token-hiding) |
| `ADR-015` | One NIP-APP client — **not overturned**; source lives elsewhere |

Figma is mixed MVP + concept (`D-012`). Where the PNG and the Login BRD disagree, **the BRD wins**.

---

## 1. What the Figma strip shows

The login wireframe is a mobile sprite strip. The filename still includes **2FA**, **Forgot Password** and **mPIN**. Visible frames include identifier entry, password, OTP / 2FA, recovery links and an mPIN-style path.

Do **not** implement from the filename:

| Figma cue | Login BRD | Action for BFF / NIP-APP |
|-----------|-----------|--------------------------|
| Forgot Password link on Bank RM and Partner | **Removed** for both modes. Recovery is Unlock User | No `/forgot-password` resource. Do not add a link on the contract. |
| Create Password on Bank RM screens | Bank RM password is the **bank system** password; not created here | No Bank RM password-create operation on this BFF. |
| mPIN | Not in the Login BRD | Out of this module. Do not add an mPIN endpoint. |
| OTP / 2FA after credentials | **Mandatory OTP on every login** to registered mobile **and** email | Required behaviour. Where it is enforced (IdP MFA vs platform OTP) is Mahesh + Deepali — see §4. |

---

## 2. Login BRD behaviour the BFF must eventually support

Dual actor types:

| Actor | Identifier | Password owner | After credentials |
|-------|------------|----------------|-------------------|
| Bank RM | Employee ID | Bank system (IdP / AD-verify) | Captcha → OTP → dashboard |
| Insurance Partner RM | Corporate Email ID | Platform-managed | Captcha → OTP → dashboard |

Shared rules (BRD):

- Captcha on the credential step; refresh after fail or explicit refresh.
- OTP: six digits, sent to registered mobile **and** email, every successful credential check.
- 3 consecutive failed **password** attempts → account locked → Unlock User.
- 30 days inactivity → account locked → Unlock User.
- 5 failed **OTP** attempts → terminate this login; **do not** lock the account.
- Forgot Password is out of scope. Unlock User covers partner first-time password, forgotten/expired partner password, and unlock for both types.
- Session timeout, Captcha complexity, concurrent login: Information Security (`SEC-009`, `SEC-010`) — not invented here.

Exact user-visible strings the contract should be able to return (Login BRD error catalogue):

- `Please enter Employee ID / Corporate Email ID.`
- `Please enter your password.`
- `Please enter the security Captcha.`
- `Your account has been locked due to multiple incorrect password attempts. Please use Unlock User.`
- `Please enter the six-digit OTP.`
- `You have exceeded the maximum OTP verification attempts. Please log in again.`

---

## 3. What `workforce-access-bff` exposes today

Code: `services/workforce-access-bff/src/main/java/com/bank/workforce/bff/api/AuthenticationController.java`.

| Method | Path | What it does | Login BRD fit |
|--------|------|----------------|---------------|
| `GET` | `/api/v1/auth/csrf` | CSRF token for POSTs | Keep. Browser NIP-APP needs this. |
| `POST` | `/api/v1/auth/login` | Body: `clientType`, `identitySource`, `returnUri`, optional `loginHint`. Returns **authorization URI only**. No password. | Partial. Distinguishes identity source (Bank vs Partner). Does **not** accept Employee ID / email / password / Captcha. |
| `GET` | `/api/v1/auth/callback` | OIDC `code` + `state` → HttpOnly cookie (web) or native completion | Fits token-hiding (`BOOT` standing constraint). Not a BRD OTP field. |
| `POST` | `/api/v1/auth/native-session` | One-time completion code → opaque session handle | Needed for native NIP-APP (`ADR-015` APK/IPA). No tokens on device. |
| `GET` | `/api/v1/auth/session` | Session status from cookie / `X-Session-Handle` | Keep. |
| `POST` | `/api/v1/auth/logout` | Revoke + expire cookie | Keep. |

Standing constraint that this evaluation does **not** relax: the client never receives OAuth access or refresh tokens. The BFF holds them.

Covered by Slice 1 / JES, **not** by the Login BRD catalogue: PKCE, return-URI allow-list, encrypted session vault, PDP resolve after authentication.

---

## 4. Gaps — recorded, not coded

| BRD / Figma need | On BFF today | Why it is not implemented in this change |
|------------------|--------------|------------------------------------------|
| Identifier + password fields on the BFF | No. Login starts OIDC | UC-01 and Fireframe/IdP ceremony (`SUG-20260914-idp`) already own the password step. Changing that is **Mahesh + Deepali**, not a Flutter fake. |
| Captcha | No endpoint | Login BRD: Infosec owns Captcha admin (`SEC-009`). |
| Platform OTP after credentials | No. UC-01 places MFA at the IdP | Whether OTP is IdP MFA or a platform resource is a trust-boundary decision. |
| Lock after 3 password failures / 30-day inactivity | Not in BFF | Account state belongs with identity / IdP, not a client fake directory. |
| Unlock User + partner password create/expiry | No | Separate operations; partner password is platform-managed. Needs identity service design. |
| Figma Forgot Password / mPIN | Must stay absent | BRD removed Forgot Password; mPIN is not in the Login BRD. |

### Open architecture question (do not assume)

How should NIP-APP collect Employee ID / Corporate Email + password + Captcha **without** sending OAuth tokens to the client?

- **A — IdP chrome (current UC-01):** BFF returns an authorization URI; Fireframe/IdP hosts credential + MFA screens. NIP-APP login tabs are entry chrome (`identitySource` + `loginHint`). Closest to code today.
- **B — BFF collects credentials and calls AD-verify / partner IdP:** closer to the Login BRD field catalogue; passwords still must not be logged; tokens still must not return to the client. Needs Deepali on credential handling and Mahesh on whether this replaces UC-01.

This evaluation does **not** pick A or B.

---

## 5. Honest BFF ⇄ Figma ⇄ BRD score for Login

| Concern | Result |
|---------|--------|
| Token-hiding session | **Met** on current BFF |
| Dual Bank RM / Partner identity source | **Partial** (`IdentitySource` on `POST /login`) |
| BRD field catalogue (Captcha, OTP, lock, Unlock User) | **Not met** |
| Figma Forgot Password / mPIN | **Correctly absent** from BFF (do not add) |
| In-repo Flutter Login UI | **Removed** (`DOC-006`) |

Next Login implementation increment (Java only): wait for Mahesh on §4 A vs B, then add OpenAPI + tests on `workforce-access-bff` (and identity services if B). Infosec still owns Captcha/OTP gateway and session timeout.
