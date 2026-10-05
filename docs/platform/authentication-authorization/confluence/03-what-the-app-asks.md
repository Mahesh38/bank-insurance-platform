# What the app asks the backend

**Audience:** new joiners, testers, and anyone mapping a screen to a request.  
**Purpose:** list what the workforce sales application is allowed to ask, in business language. You do not need a programming background.  
**Companion:** [start here](./00-start-here.md) · [Journeys](./02-journeys.md)

The sales application talks **only** to the workforce access service. Everything else (bank directory, Keycloak, one-time-code sending, permission checks) happens behind that door.

The public sign-in start request **does not include a password**. That is deliberate until security decides whether the password is typed in our app or on a bank-hosted page.

---

## Sign-in and session

| What the person is doing | What the app asks | What comes back |
|---|---|---|
| Open the sign-in screen (browser) | Ask for a form-protection token | A token the browser must send with later submits |
| Choose Bank or Insurance partner and continue | Start sign-in, with where to return afterwards | A sign-in **address** only — never a password check result, never an access token |
| Return from the bank-hosted or partner sign-in page | Finish sign-in with the one-time authorisation code | Waiting-for-OTP session, **or** an error. Not the home screen yet |
| Phone app completing sign-in | Exchange a one-time completion code | An opaque session handle for the phone’s secure store. Not a JSON web token. Not an access token |
| Check “am I still signed in?” | Read session | Who they are to the business, and whether they are still waiting for the six-digit code |
| Sign out | End session | Signed out. Cookie expired |

---

## Visual check, one-time code, Unlock User, partner password

| What the person is doing | What the app asks | What comes back |
|---|---|---|
| Load or refresh captcha | Issue a visual check | An image and an id. The answer is never logged |
| Enter the six-digit sign-in code | Check the code | Signed in (home screen allowed), or remaining tries, or “start again” |
| Wait two minutes and tap Resend | Resend the sign-in code | New code sent, old code dead. After three resends, they must start again |
| Tap Unlock User | Start unlock with user type, employee ID or email, and captcha | Waiting for unlock code, with masked mobile and email |
| Enter the unlock six-digit code | Check the unlock code | “Go back to sign-in”, or “now create a password” (partners only) |
| Partner creates or resets password | New password and confirm | Success, then **sign-in screen** — they are not signed in |
| Tap Get Help | Read help | Support contact supplied by IT, or “try again later” if content is missing |
| Tap Download Unlock Guide | Download a PDF | The guide supplied by IT, or “try again later” |

Bank staff who call “set password” are refused. That request is partner-only.

There is **no** “forgot password” request. There is **no** mPIN request.

---

## Behind the door (not called by the app)

These exist so the access service can do its job. The sales application never calls them.

| Private request | Everyday meaning |
|---|---|
| Build sign-in address / exchange code / refresh / revoke | Talk to the partner identity product without the app seeing tokens |
| Check bank employee ID and password | Call the bank’s existing directory check through the bank’s private gateway |
| Issue and check captcha | Visual check |
| Issue, check, and resend a six-digit code | One-time password, hashed at rest, never returned in a response |
| Create, enable, disable partner identity; ask for “update password” | After an administrator has approved the partner |
| “Who is this person to the business?” | Map a Keycloak or bank subject to the platform identity |
| “May they do this?” | Allow or deny, with a reason and a policy version. Target: answered within 300 milliseconds, no retry, default deny |
| Record a wrong password / lock / unlock / “a partner password now exists” | Account state. The password itself is never sent here |

---

## Words the person sees when something is wrong

These sentences are the approved wording. Do not invent friendlier or more specific messages that would tell an attacker whether an account exists.

| Situation | What they see |
|---|---|
| Identifier left blank | Please enter Employee ID / Corporate Email ID. |
| Password left blank | Please enter your password. |
| Captcha left blank | Please enter the security Captcha. |
| Bank employee ID not recognised | Invalid Employee ID. Please check and try again. |
| Partner account not recognised | Invalid account. Please check and try again. |
| Wrong password, first time | The password entered is incorrect. You have 2 attempts remaining. |
| Wrong password, second time | The password entered is incorrect. You have 1 attempt remaining. |
| Wrong password, third time | Your account has been locked due to multiple incorrect password attempts. Please use Unlock User. |
| Wrong captcha | Wrong Captcha. Please check and try again. |
| Locked after 30 days unused | Your account is locked. Please use Unlock User to continue. |
| One-time code blank | Please enter the six-digit OTP. |
| Wrong one-time code | The OTP entered is incorrect. (Show how many tries remain.) |
| One-time code too old | The OTP has expired. Please request a new OTP. |
| Five wrong one-time codes | You have exceeded the maximum OTP verification attempts. Please log in again. |
| Three resends already used | You have reached the maximum OTP resend limit. Please return to login and try again. |
| SMS and email both failed | We could not send the OTP to your registered mobile number and email ID. Please try again. |
| Unlock when already active | Your account is already active. Please proceed to login. |
| Disabled or deactivated | Your account is not active. Please contact support. |
| New password does not meet the rules | Password must be 8 to 20 characters and contain uppercase, lowercase, number and special character. |
| New and confirm do not match | New Password and Confirm Password do not match. |
| Password contains their name or email | Password must not contain your name or corporate email ID. |
| Something downstream is down | We are unable to process your request at this time. Please try again later. |

On success, keep the wording equally specific:

| Situation | What they see |
|---|---|
| Signed in after the six-digit code | You are logged in. Taking you to your dashboard. |
| Bank account unlocked | Your account has been unlocked successfully. Please log in using your existing bank credentials. |
| Partner unlocked, password unchanged | Your account has been unlocked successfully. Please log in using your existing password. |
| Partner password created or reset | Your password has been created successfully. Please log in using your corporate email ID and new password. |

---

## What we refuse to publish as a public request

- Sign-in with a password in the request body (until the open security decision is closed)
- Forgot password
- mPIN
- Bank staff create-password
- Any request that would return an access token, a refresh token, a raw one-time code, or a password to the device
