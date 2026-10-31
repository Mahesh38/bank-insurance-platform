# Login BRD v3.1 — screen reference

**Received:** 31 October 2026, with `D-029`.  
**Behaviour:** [Login BRD v3.1](../../au-bank-insurance-platform/requirements/brd-detailed/Login_Module_BRD_Detailed_CONTEXT.md). When a picture and that BRD disagree, the BRD wins (`D-012`).  
**Users:** two only. Bank employee (screen label **AU Bank RM**) and non-bank employee (screen label **Insurance Partner**).

Values typed on these screens are design samples. They are not accounts to create and not passwords to configure.

The older export [`../wireframe/auth-login-2fa-forgot-password-mpin.png`](../wireframe/auth-login-2fa-forgot-password-mpin.png) is not this set. mPIN is not in the Login BRD.

| File | What it shows |
|---|---|
| [01-bank-employee-sign-in-empty.png](./01-bank-employee-sign-in-empty.png) | AU Bank RM. Employee ID, password, captcha. No Forgot password link. Sign in disabled. |
| [02-non-bank-employee-sign-in-empty.png](./02-non-bank-employee-sign-in-empty.png) | Insurance Partner. Corporate email, password, captcha, Forgot password. Sign in disabled. |
| [03-bank-employee-sign-in-filled.png](./03-bank-employee-sign-in-filled.png) | Bank employee ID and password entered. Captcha still empty. Sign in disabled. |
| [04-bank-employee-sign-in-ready.png](./04-bank-employee-sign-in-ready.png) | Bank employee fields complete, including captcha. Sign in enabled. |
| [05-non-bank-employee-sign-in-ready.png](./05-non-bank-employee-sign-in-ready.png) | Non-bank employee fields complete, including captcha. Sign in enabled. Forgot password remains available. |
| [06-bank-employee-invalid-employee-id.png](./06-bank-employee-invalid-employee-id.png) | Unknown employee ID. “Invalid Employee ID. Please check and try again.” |
| [07-non-bank-employee-account-not-found.png](./07-non-bank-employee-account-not-found.png) | Unknown corporate email. “We couldn't find a partner account with this email ID.” |
| [08-bank-employee-wrong-captcha.png](./08-bank-employee-wrong-captcha.png) | Captcha does not match. “Wrong captcha. Try again.” |
| [09-login-states-overview.png](./09-login-states-overview.png) | Six groups: account status, unlock outcomes, OTP limits, set-password rules, Insurance RM expiry, Insurance RM forgot password. The last group is labelled “missing states” on the export. Those states remain specified by the BRD. |
| [10-bank-employee-journey.png](./10-bank-employee-journey.png) | AU Bank RM board. Frames shown are sign-in, invalid ID, wrong password, lock, welcome, unlock, and account unlocked. The board title also says “Forgot password”. The Login BRD gives the bank employee no Forgot Password screen. Do not add one from the title. |
| [11-non-bank-employee-journey.png](./11-non-bank-employee-journey.png) | Insurance Partner board: sign-in, account not found, welcome, unlock, first-time set password, forgot password, expiry, and password changed. |
