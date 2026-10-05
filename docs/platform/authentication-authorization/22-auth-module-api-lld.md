# 22 — Auth module API LLD (public BFF + cluster-private)

**Status:** `AI-DRAFTED` · T3 · human Board 1 / 4 outstanding  
**Origin:** `SUG-20261005-amp` · `EPIC-006` · `ARCH-032` · `PLAN-009`  
**Wire contracts:**  
[`workforce-access-bff.openapi.yaml`](./workforce-access-bff.openapi.yaml) ·  
[`identity-provider-adapter.openapi.yaml`](./identity-provider-adapter.openapi.yaml) ·  
[`identity-authorization.openapi.yaml`](./identity-authorization.openapi.yaml)  
**Reuse:** `ARCH-029` paths stay. This LLD **extends** them for the Login BRD catalogue. It does **not** add a login-password body (`OPEN-AUTH-CEREMONY`).

---

## 1. Purpose

Give engineers one mapping from Login BRD screens to HTTP operations. Flutter never calls internal paths. Persistence of authorization data stays on the PDP database (Aarti owns physical DDL).

---

## 2. Conventions (inherit platform)

| Topic | Rule |
|---|---|
| Style | REST, nouns, `application/json` |
| Errors | `application/problem+json` + ADR-017 `category` + `code` |
| Success | Bare resource — no `success/data/message` envelope |
| Secrets | Password, OTP, captcha answer, provider tokens **never** in logs or audit payloads |
| Idempotency | `Idempotency-Key` on unlock complete and partner set-password |
| Auth | Public: CSRF + pending/session cookie. Internal: mesh identity |
| PII | Masked mobile/email only on OTP screens (`SEC-003`) |

Public base: `/api/v1/auth`. Internal base: `/internal/v1`.

---

## 3. Public BFF → internal mapping

| Public (this pack / ARCH-029) | Internal | Notes |
|---|---|---|
| `GET /csrf` | — | Keep |
| `POST /login` | `POST /internal/v1/auth/authorization-uri` | URI only. No password |
| `GET /callback` | `token-exchange` + `identities/resolve` + `otp/sessions` | Yields `PENDING_OTP`, not dashboard |
| `POST /native-session` | vault | Keep; handle may be pending-otp |
| `GET /session` | vault + PDP snapshot | `sessionState`: `PENDING_OTP` \| `AUTHENTICATED` |
| `POST /logout` | `auth/revoke` | Keep |
| `POST /captcha` | `POST /internal/v1/captcha/challenges` | Issue image + id |
| `POST /otp:verify` | `otp/sessions/{id}:verify` | Login OTP |
| `POST /otp:resend` | `otp/sessions/{id}:resend` | 2 min / max 3 |
| `POST /unlock` | captcha + identity lookup + OTP | No password |
| `POST /unlock/otp:verify` | OTP + PDP unlock / password-intent | Outcome matrix §7.7 |
| `POST /partner/password` | `credential-actions` / set-password | Partner only; after unlock OTP |
| `GET /help` | content | `OPEN-AUTH-HELP` |
| `GET /unlock-guide` | content PDF | `OPEN-AUTH-HELP` |
| **Not published:** `POST /credentials` | AD-verify / partner verify | `OPEN-AUTH-CEREMONY` |
| **Not published:** `/forgot-password` | — | BRD removed it |

---

## 4. Public resources (additive)

### 4.1 `POST /api/v1/auth/captcha`

Issue a challenge. Refresh after fail or client refresh (BRD §7.1).

**Response:** `{ "captchaId", "imageMediaType", "imageBase64", "expiresInSeconds" }` — never log the answer.

### 4.2 `POST /api/v1/auth/otp:verify`

Body: `{ "otp": "dddddd" }` plus pending-otp cookie / handle.

**Guards:** `ALG-OTP-VERIFY`. Responses: `200` session upgraded · `400` VAL-011/012/013/014.

### 4.3 `POST /api/v1/auth/otp:resend`

**Guards:** 120 s since last send; `resendCount < 3`. `429` VAL-015.

### 4.4 `POST /api/v1/auth/unlock`

```json
{
  "userType": "BANK_RM",
  "loginId": "E12345",
  "captchaId": "…",
  "captchaAnswer": "…"
}
```

Partner: `userType=PARTNER`, `loginId` = corporate email. Optional `intent`: `UNLOCK` \| `RESET_PASSWORD` (table 14).

**Guards:** captcha, user exists, not DISABLED (`BR-UNLOCK-003`). Issues unlock OTP.

### 4.5 `POST /api/v1/auth/unlock/otp:verify`

Completes Bank RM unlock **or** returns `next=SET_PASSWORD` for partner first-time / forgot / expired.

### 4.6 `POST /api/v1/auth/partner/password`

```json
{
  "newPassword": "…",
  "confirmPassword": "…"
}
```

Requires `UNLOCK_PASSWORD` session. `ALG-PWD-POLICY`. On success: destroy unlock session; **do not** mint login (`BR-PWD-002`). Bank RM callers → `403 BANK_PASSWORD_CHANGE_FORBIDDEN`.

### 4.7 `GET /api/v1/auth/help` · `GET /api/v1/auth/unlock-guide`

IT-supplied. Empty content → `503` VAL-022, not a fake number.

### 4.8 Existing login / callback / session / logout

Unchanged request shapes from `ARCH-029`. Callback **semantics** change to `PENDING_OTP` (documented in `20` §5). `GET /session` grows `sessionState` and optional `maskedMobile` / `maskedEmail` when `PENDING_OTP`.

---

## 5. Adapter private resources (additive)

| Operation | Purpose |
|---|---|
| `POST /internal/v1/captcha/challenges` | Create challenge |
| `POST /internal/v1/captcha/challenges/{id}:verify` | Consume challenge |
| `POST /internal/v1/otp/sessions` | `{ purpose, businessUserId }` → hashed OTP, send both channels |
| `POST /internal/v1/otp/sessions/{id}:verify` | Compare, consume |
| `POST /internal/v1/otp/sessions/{id}:resend` | Invalidate previous; new OTP |
| `POST /internal/v1/auth/ad-verify` | `{ employeeId, password }` → AD-verify via Apigee. **BFF-only; OPEN-AUTH-CEREMONY to expose publicly** |
| existing `authorization-uri` / `token-exchange` / `refresh` / `revoke` | Keep |
| existing `identities` + `credential-actions` | Partner provision / `UPDATE_PASSWORD` |

OTP store: one-way hash + purpose + session id + expiry + attempt/resend counters. Not in `bank-persistence-service`. Not in Keycloak.

---

## 6. PDP private resources (additive)

| Operation | Purpose |
|---|---|
| existing `POST /authorization/decisions` | UC-05 — unchanged |
| existing `POST /identities/resolve` | After credential success |
| `GET /internal/v1/identities/{id}/account-state` | lock, last login, passwordExists, passwordExpiresAt, failedPasswordCount |
| `POST /internal/v1/identities/{id}/password-failures` | Increment / lock at 3 |
| `POST /internal/v1/identities/{id}/unlock` | Clear FAILED_PASSWORD / INACTIVITY overlay |
| `POST /internal/v1/identities/{id}/password-metadata` | `passwordExists`, `expiresAt=+60d` — **never the password** |

Inactivity lock is evaluated at resolve: if `now - lastSuccessfulLoginAt > 30d` treat as locked (`VAL-010`).

---

## 7. Error catalogue (BRD §8 → platform)

| VAL | HTTP | `code` | User `detail` (approved copy) |
|---|---|---|---|
| VAL-001 | 400 | `LOGIN_ID_REQUIRED` | Please enter Employee ID / Corporate Email ID. |
| VAL-002 | 400 | `PASSWORD_REQUIRED` | Please enter your password. *(ceremony host only)* |
| VAL-003 | 400 | `CAPTCHA_REQUIRED` | Please enter the security Captcha. |
| VAL-004 | 400 | `INVALID_EMPLOYEE_ID` | Invalid Employee ID. Please check and try again. |
| VAL-005 | 400 | `INVALID_ACCOUNT` | Invalid account. Please check and try again. |
| VAL-006 | 401 | `PASSWORD_INCORRECT` | The password entered is incorrect. You have 2 attempts remaining. |
| VAL-007 | 401 | `PASSWORD_INCORRECT` | The password entered is incorrect. You have 1 attempt remaining. |
| VAL-008 | 423 | `ACCOUNT_LOCKED_PASSWORD` | Your account has been locked due to multiple incorrect password attempts. Please use Unlock User. |
| VAL-009 | 400 | `CAPTCHA_WRONG` | Wrong Captcha. Please check and try again. |
| VAL-010 | 423 | `ACCOUNT_LOCKED_INACTIVITY` | Your account is locked. Please use Unlock User to continue. |
| VAL-011 | 400 | `OTP_REQUIRED` | Please enter the six-digit OTP. |
| VAL-012 | 401 | `OTP_INCORRECT` | The OTP entered is incorrect. *+ remaining* |
| VAL-013 | 400 | `OTP_EXPIRED` | The OTP has expired. Please request a new OTP. |
| VAL-014 | 401 | `OTP_ATTEMPTS_EXCEEDED` | You have exceeded the maximum OTP verification attempts. Please log in again. |
| VAL-015 | 429 | `OTP_RESEND_EXCEEDED` | You have reached the maximum OTP resend limit. Please return to login and try again. |
| VAL-016 | 503 | `OTP_DELIVERY_FAILED` | We could not send the OTP to your registered mobile number and email ID. Please try again. |
| VAL-017 | 409 | `ACCOUNT_ALREADY_ACTIVE` | Your account is already active. Please proceed to login. |
| VAL-018 | 403 | `ACCOUNT_NOT_ACTIVE` | Your account is not active. Please contact support. |
| VAL-019 | 400 | `PASSWORD_POLICY` | Password must be 8 to 20 characters and contain uppercase, lowercase, number and special character. |
| VAL-020 | 400 | `PASSWORD_MISMATCH` | New Password and Confirm Password do not match. |
| VAL-021 | 400 | `PASSWORD_CONTAINS_PII` | Password must not contain your name or corporate email ID. |
| VAL-022 | 503 | `SERVICE_UNAVAILABLE` | We are unable to process your request at this time. Please try again later. |

`SEC-007`: do not add existence-oracle fields beyond these strings.

---

## 8. What this LLD refuses

- Public login-password / ROPC on the BFF (`OPEN-AUTH-CEREMONY`).
- `/forgot-password` and mPIN resources.
- Bank RM `POST /partner/password` equivalent.
- Returning OTP, password, or provider tokens to Flutter.
- Treating Keycloak roles as PDP decisions (`ADR-022`).
- Auto-login after partner password set (`BR-PWD-002`).

---

## 9. Done for this document

- [x] BFF mapping table including BRD catalogue
- [x] VAL-* → HTTP/code table
- [x] Login-password path explicitly refused
- [ ] Human Board 1 / Security signatures
