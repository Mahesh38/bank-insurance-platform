# Bank RM and Insurance RM — authentication API stories

One user story for each API that implements the Login BRD v3.1 journey for a **Bank RM**
(`userType: BANK_RM`) and an **Insurance RM** (`userType: INSURANCE_RM`, including FLS).

Behaviour source: [Workforce auth SSOT](../README.md) §5.1 and
[LOGIN-BRD-V31-API-FLOWS.md](../../../journey-execution/flows/LOGIN-BRD-V31-API-FLOWS.md).
Story shape: [STORY.md](../../../application-lifecycle-bible/templates/STORY.md).

`WS2-LOGIN` is a story grouping for workstream WS-2. It is not a minted `EPIC-nnn`.

The workforce app calls only `workforce-access-bff`. Internal operations are called by the
BFF with `X-Internal-Service-Key`. They are never exposed to the app, and they never return
an OAuth access or refresh token.

## Public APIs — NIP calls these

| Story | Method and path | Who | Session |
|---|---|---|---|
| [WS2-LOGIN-S01](./WS2-LOGIN-S01-csrf.md) | `GET /api/v1/auth/csrf` | Web app, before any login POST | No |
| [WS2-LOGIN-S02](./WS2-LOGIN-S02-captcha.md) | `POST /api/v1/auth/captcha` | Bank RM and Insurance RM | No |
| [WS2-LOGIN-S03](./WS2-LOGIN-S03-login.md) | `POST /api/v1/auth/login` | Bank RM and Insurance RM | No |
| [WS2-LOGIN-S04](./WS2-LOGIN-S04-otp-verify.md) | `POST /api/v1/auth/otp/verify` | Bank RM and Insurance RM | Only when `outcome` is `AUTHENTICATED` |
| [WS2-LOGIN-S05](./WS2-LOGIN-S05-otp-resend.md) | `POST /api/v1/auth/otp/resend` | Bank RM and Insurance RM | No |
| [WS2-LOGIN-S06](./WS2-LOGIN-S06-unlock.md) | `POST /api/v1/auth/unlock` | Bank RM and Insurance RM | No |
| [WS2-LOGIN-S07](./WS2-LOGIN-S07-forgot-password.md) | `POST /api/v1/auth/forgot-password` | Insurance RM only | No |
| [WS2-LOGIN-S08](./WS2-LOGIN-S08-password-expired-send-otp.md) | `POST /api/v1/auth/password-expired/send-otp` | Insurance RM only | No |
| [WS2-LOGIN-S09](./WS2-LOGIN-S09-set-password.md) | `POST /api/v1/auth/password` | Insurance RM only | No |
| [WS2-LOGIN-S10](./WS2-LOGIN-S10-session.md) | `GET /api/v1/auth/session` | Authenticated Bank RM or Insurance RM | Reads the handle |
| [WS2-LOGIN-S11](./WS2-LOGIN-S11-logout.md) | `POST /api/v1/auth/logout` | Authenticated Bank RM or Insurance RM | Destroys it |

Web mutating calls also send cookie `WORKFORCE-XSRF` and header `X-XSRF-TOKEN` from S01.

## Internal APIs — the BFF calls these

| Story | Method and path | Used when |
|---|---|---|
| [WS2-LOGIN-S12](./WS2-LOGIN-S12-login-preview.md) | `POST /internal/v1/login/preview` | Before a password is counted |
| [WS2-LOGIN-S13](./WS2-LOGIN-S13-ad-verify.md) | `POST /internal/v1/auth/ad-verify` | Bank RM password check |
| [WS2-LOGIN-S14](./WS2-LOGIN-S14-bank-password-result.md) | `POST /internal/v1/login/bank-password-result` | After AD-verify returns |
| [WS2-LOGIN-S15](./WS2-LOGIN-S15-verify-platform-password.md) | `POST /internal/v1/login/verify-platform-password` | Insurance RM password check |
| [WS2-LOGIN-S16](./WS2-LOGIN-S16-login-complete.md) | `POST /internal/v1/login/complete` | Login OTP succeeded |
| [WS2-LOGIN-S17](./WS2-LOGIN-S17-unlock-assess.md) | `POST /internal/v1/login/unlock/assess` | Unlock started |
| [WS2-LOGIN-S18](./WS2-LOGIN-S18-unlock-complete.md) | `POST /internal/v1/login/unlock/complete` | Unlock OTP succeeded |
| [WS2-LOGIN-S19](./WS2-LOGIN-S19-forgot-assess.md) | `POST /internal/v1/login/forgot/assess` | Insurance RM forgot password |
| [WS2-LOGIN-S20](./WS2-LOGIN-S20-set-platform-password.md) | `POST /internal/v1/login/password` | Insurance RM saves a platform password |

## Not this journey

Insurer partner representative login is Keycloak OIDC (`UC-02`), not the Insurance RM toggle.
Those operations are implemented and are outside this story set:

- `POST /api/v1/auth/partner/authorization`
- `GET /api/v1/auth/callback`
- `POST /api/v1/auth/native-session`
- Adapter `POST /internal/v1/auth/authorization-uri`, `token-exchange`, `refresh`, `revoke`

Bank RM has no create, reset, or change-password API. mPIN is absent. A body that still sends
IAM-001 fields (`identitySource` / `employeeId` without the BRD fields) is `422 VALIDATION_ERROR`.

## Shared limits already recorded on the flow

These stories describe the code that is in the repository. They do not close the parked remainder
of `SUG-20261008-otp`:

- Captcha text is returned to the caller. An image vendor is not wired.
- OTP is hashed in the BFF process and is not sent by SMS or email. `prod` and `uat` refuse the
  `WORKFORCE_LOGIN_OTP_CODE` test hook.
- Captcha, OTP, and reset tokens are in-process. A restart or a second pod drops them.
- Login handlers do not write the §19 audit events.
