# Workforce login — Postman collection

**Work item:** `SUG-20261025-pmn` · **Contract:** [Login BRD v3.1 API flows](../../../docs/journey-execution/flows/LOGIN-BRD-V31-API-FLOWS.md)

This collection tests `/api/v1/auth/**` on `workforce-access-bff`. It covers Bank RM and Insurance
RM/FLS sign-in, captcha, OTP, unlock, forgot and reset password, password expiry, session and logout.
FLS is the `INSURANCE_RM` login. This is a test tool. It is not gate evidence and not a Security sign-off.

| File | What it is |
|---|---|
| `workforce-login.postman_collection.json` | 13 folders, 91 requests. Import it into Postman. |
| `environments/{local,dev,uat,prod}.postman_environment.json` | One file per environment |
| `data/set-password-policy.csv` | Rows for folder 10. Each row is one invalid password and its expected code. |
| `build-collection.js` | Generates the JSON above. Change this file and regenerate. Do not hand-edit the JSON. |

## Run it

In Postman, import the collection and an environment, select that environment, and run the collection.

With Newman:

```bash
npx newman@6.2.1 run services/workforce-access-bff/postman/workforce-login.postman_collection.json -e services/workforce-access-bff/postman/environments/local.postman_environment.json
```

To run folder 10 once per row of the password-policy data file:

```bash
npx newman@6.2.1 run services/workforce-access-bff/postman/workforce-login.postman_collection.json -e services/workforce-access-bff/postman/environments/local.postman_environment.json --folder "10 [D] Set-password policy (data-driven)" -d services/workforce-access-bff/postman/data/set-password-policy.csv
```

On the command line, `--env-var baseUrl=...` (or any other variable) overrides the environment file.

The local environment needs the `dev` profile on all three services. That profile supplies the stub
AD-verify, the seeded accounts and the OTP pin `482913`. Start them in this order: adapter `8082`,
identity-authorization `8083`, then the BFF `8084`. The steps are in [the BFF README](../README.md).

## What the scripts do for you

| Concern | How |
|---|---|
| CSRF | The collection pre-request fetches `GET /auth/csrf` once. It sends `X-XSRF-TOKEN` and the `WORKFORCE-XSRF` cookie on every POST that has no session header. A `403` clears the token, so the next request fetches a new one. |
| Captcha | A request body that contains `{{captchaId}}` gets a new captcha first. The script reads `display` from the response and puts it in `captchaValue`. |
| OTP | Requests use `{{otpCode}}`. When `otpCode` is empty, the request skips itself and says why. |
| Flow state | `challengeId`, `resetToken`, `expiryTicket`, `sessionHandle` and `businessUserId` are captured from responses. When a step skips, the flow state is cleared, so later steps in that flow skip as well. |
| Session | `NATIVE` reads `sessionHandle` from the body. `WEB` reads it from `Set-Cookie: WORKFORCE_SESSION` and checks the `HttpOnly`, `Secure` and `SameSite=Strict` flags. Later calls send `X-Session-Handle`. |
| Generated data | Folder 09 generates a temporary password that meets the policy. Unknown IDs use `{{$randomInt}}`. |
| Correlation | Every request carries a new `X-Correlation-Id`. |

Every response is checked for:

- the response time limit (`maxResponseMs`)
- a JSON body
- no OAuth token, password or OTP echoed back
- on an error: the platform problem shape with no diagnostic detail
- on a journey response: an outcome from the v3.1 set

Each request also asserts its own outcome, code, field and copy.

## Requests that change accounts

A request whose name starts with `[D]` changes account state: the password counter, a lock, the
password hash or first-time activation. These run only when `allowDestructive=true`. They never run
when `envName=prod`.

The run leaves the dev accounts as it found them:

- Folder 00 unlocks the accounts if an earlier run was interrupted.
- The lockout folders end with an unlock.
- Folder 09 restores the original FLS password.
- Folder 08 accepts `NOT_REQUIRED` once the first-time account has been activated.

## Environments

| Env | `otpCode` | `allowDestructive` | Accounts | Notes |
|---|---|---|---|---|
| local | `482913` | true | Dev seed: `EMP001`, `fls.rm@example.test`, first-time `fls.new@example.test` | Fully runnable |
| dev | `482913` | true | Dev seed | Set `baseUrl`. The pin works only if dev runs the `dev` profile. |
| uat | empty | false | Fill in locally. Do not commit them. | The OTP is not returned or delivered yet, so OTP steps skip unless a tester enters the code. |
| prod | empty | false (enforced) | Fill in locally. Do not commit them. | Set `baseUrl`. `[D]` requests are refused. |

Folder 11 (password expiry) runs only when you set `flsExpiredEmail`, `flsExpiredPassword` and
`flsExpiredNewPassword` to an account whose password is more than 60 days old.

The email rule (a password that contains the account's email) is not in the data file.
`fls.rm@example.test` is longer than 16 characters, so the length rule fails first.

## Known failures (2026-10-26, local `dev` stack)

The full run made 122 requests and 582 assertions. Seven assertions failed, all in the two rows
below, after the authorization read timeout was raised for that run (see the first row of the next
table). Folder 10 passed all 10 data rows. A second run against the same state gave the same result.

| Request | Expected (contract) | Actual | Status |
|---|---|---|---|
| 01.3 Captcha without CSRF | `403` problem with `CSRF_REJECTED` ([scenario 01 §6](../../../docs/platform/api-reference/scenarios/01-bank-rm-login.md#6-errors-the-app-must-handle)) | `403` with the Spring default body and no `code` | `SUG-20261026-csr` |
| 06.1–06.6 Bank RM lockout | `VAL-008`, `VAL-009`, `VAL-010`, then unlock | `VAL-007` on every wrong password. The account never locks. | Already open: the bank wrong-password defect from the 2026-10-24 review |

With the default configuration, the following also fails on this machine:

| Request | Actual | Status |
|---|---|---|
| 09.x and 10.x set-password | `VAL-024` after about 2 s. Authorization takes about 2.4 s for set-password, and the BFF read timeout to authorization is 2 s. Authorization can still commit the change after the BFF has returned `VAL-024`. | `SUG-20261026-pto` |
