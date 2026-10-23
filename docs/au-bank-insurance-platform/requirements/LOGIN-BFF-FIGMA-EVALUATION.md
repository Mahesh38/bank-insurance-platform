# Login — BFF evaluated against Figma (not an in-repo app)

**Date:** 2026-09-28  
**Work item:** `DOC-006` + Login first module (`Q7`)  
**Status:** Working evaluation. No NIP-APP source in this repository. `IAM-001` (2026-10-08) added Bank RM password collection on `POST /api/v1/auth/login`. Login BRD v3.1 sign-in, Captcha, OTP, Unlock User, Insurance RM Forgot Password and expiry are on the BFF journey endpoints. External Captcha image and SMS/email delivery remain unnamed (`SUG-20261008-otp` remainder).

| Source | Role |
|--------|------|
| [Login BRD v3.1](./brd-detailed/Login_Module_BRD_Detailed_CONTEXT.md) | Behaviour SSOT (`DOC-005`). User-visible copy is the design text, accepted 23 October 2026. |
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
| Forgot Password link on Bank RM | **Not available.** Bank RM network-password reset stays with the bank. | No Bank RM forgot-password operation. |
| Forgot Password link on Insurance RM | **Required.** Own Email ID and Captcha screen. Login values are not carried. Password reset does not unlock. | `POST /api/v1/auth/forgot-password`. In-process Captcha. No SMS/email provider. |
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
- Insurance RM Forgot Password is in scope and starts on its own Email ID and Captcha screen. Unlock User does not replace it. A locked Insurance RM still unlocks before the new password can be used to log in.
- Insurance RM password expiry is 60 days and has its own OTP and Set New Password path.
- Password rules are 8 to 16 characters, no spaces, not `123456`, must not contain the full email, and must differ from the previous password.
- Session timeout, Captcha complexity, concurrent login: Information Security (`SEC-009`, `SEC-010`) — not invented here.

Exact user-visible strings the contract should be able to return (Login BRD v3.1 design text):

- `Wrong captcha. Try again.`
- `We couldn't find a partner account with this email ID.`
- `Incorrect password. You have 2 attempts remaining.`
- `Your account has been locked. You entered an incorrect password 3 times. Please use Unlock User to verify with an OTP and continue.`
- `Too many incorrect attempts. Maximum attempts exceeded. Please restart the journey. Your account has not been locked.`
- Blank Login ID, Password, Captcha and incomplete OTP do not show a message. The action stays disabled.

---

## 3. What `workforce-access-bff` exposes today

Code: `services/workforce-access-bff/src/main/java/com/bank/workforce/bff/api/AuthenticationController.java`.

| Method | Path | What it does | Login BRD fit |
|--------|------|----------------|---------------|
| `GET` | `/api/v1/auth/csrf` | CSRF token for POSTs | Keep. Browser NIP-APP needs this. |
| `POST` | `/api/v1/auth/login` | `BANK_AD`: `employeeId` + `password` → `{ authenticated }` + HttpOnly cookie / native handle. `PARTNER`: authorization URI only. | Bank RM credential check is `IAM-001`. Captcha / OTP still absent. |
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
| Identifier + password fields on the BFF | **Met for Bank RM** (`IAM-001`). Partner still OIDC | Owner selected option B for `BANK_AD` on 2026-10-08. Partner password collection is not in `IAM-001`. |
| Captcha | `POST /api/v1/auth/captcha` returns a single-use text value | Image Captcha and provider admin remain with Infosec (`SEC-009`). |
| Platform OTP after credentials | `POST /api/v1/auth/otp/verify`. Session only after login OTP | The code is generated in-process. It is not delivered by SMS or email. |
| Lock after 3 password failures / 30-day inactivity | Account state is in identity-authorization. The BFF shows the design sentence | Password failures and inactivity are counted on the platform user, not in the BFF. |
| Unlock User + Insurance RM password create, Forgot Password and 60-day expiry | `/auth/unlock`, `/auth/password`, `/auth/forgot-password`, `/auth/password-expired/send-otp` | These calls do not create a workforce session. |
| mPIN | Must stay absent | Not in Login BRD v3.1. |

### Open architecture question (do not assume)

How should NIP-APP collect Employee ID / Corporate Email + password + Captcha **without** sending OAuth tokens to the client?

- **A — IdP chrome:** BFF returns an authorization URI; Fireframe/IdP hosts credential + MFA screens. **Still the partner path.**
- **B — BFF collects credentials and calls AD-verify:** closer to the Login BRD field catalogue; passwords still must not be logged; tokens still must not return to the client.

**Decision 2026-10-08 (`IAM-001`):** **B for Bank RM** on `POST /auth/login`. Partner OIDC remains A.

**Decision 2026-10-23 (owner, `SUG-20261008-otp`):** the v3.1 journey is **B** as well, on `/auth/sign-in` and the recovery endpoints. `POST /auth/login` is unchanged. External Captcha and OTP delivery are still unnamed.

---

## 5. Honest BFF ⇄ Figma ⇄ BRD score for Login

| Concern | Result |
|---------|--------|
| Token-hiding session | **Met** on current BFF |
| Dual Bank RM / Partner identity source | **Partial** (`IdentitySource` on `POST /login`) |
| BRD field catalogue (Captcha, OTP, lock, Unlock User) | **Met in-process** on the v3.1 endpoints. Delivery to a handset or inbox is not met |
| Insurance RM Forgot Password | **Met** on `POST /auth/forgot-password`. Password reset does not clear a lock |
| mPIN | **Correctly absent** from BFF |
| In-repo Flutter Login UI | **Removed** (`DOC-006`) |

Remaining Login increment: Infosec still has to name the Captcha image provider, the SMS/email gateway and the session-timeout numbers (`SUG-20261008-otp` remainder). The journey code must not be described as that approval. mPIN stays out. Challenge state is single-instance.
