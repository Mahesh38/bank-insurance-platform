# Rules the system follows

**Audience:** new joiners, testers, and developers implementing the behaviour.  
**Purpose:** the lock, one-time-code, unlock and password rules as procedures you can follow.  
**Companion:** [start here](./00-start-here.md) · [Journeys](./02-journeys.md)

Passwords and one-time codes are **never** written to logs. When a procedure says “store”, it means a one-way hash or a metadata flag — never the secret itself.

---

## Visual security check

1. Create a challenge and remember a one-way hash of the answer, with an expiry.
2. Show the image to the person.
3. On submit, **use the challenge once**. If it is missing, expired, or wrong, say “Wrong Captcha. Please check and try again.”
4. Refresh the image after a failure, and whenever the person asks.

How hard the captcha is, whether it is case-sensitive, and how long it lives are decided by Information Security together with the captcha product. Do not invent those numbers in code.

---

## Checking identifier and password

The **same** checks apply whether the password is typed in our app or on a bank-hosted page. Only the *place* that collects the password is still open.

1. Identifier required. Password required. Captcha must pass.
2. Look up the person.
   - Bank, unknown employee ID → “Invalid Employee ID. Please check and try again.”
   - Partner, unknown account → “Invalid account. Please check and try again.” (Do not confirm whether the email exists in a different way.)
3. Disabled or deactivated → “Your account is not active. Please contact support.” Stop. Do not unlock later either.
4. Locked, or unused for 30 days → “Your account is locked. Please use Unlock User to continue.”
5. Partner who has never created a password → they cannot sign in yet; they must use Unlock User.
6. Partner whose password is older than 60 days → they must use Unlock User to create a new one.
7. Bank staff: check the password with the bank’s existing directory check. **Do not store it.**
8. Partner staff: check the password with the partner identity product. **Do not store it.**
9. Wrong password → follow **Wrong password and lock** below. Do not send a one-time code.
10. Right password → **do not** open the home screen. Start a one-time-code attempt.

---

## Wrong password and lock

1. Add one to the consecutive-wrong counter.
2. 1 wrong → “The password entered is incorrect. You have 2 attempts remaining.”
3. 2 wrong → “The password entered is incorrect. You have 1 attempt remaining.”
4. 3 wrong → lock the account for “too many wrong passwords”. “Your account has been locked due to multiple incorrect password attempts. Please use Unlock User.”
5. Write an audit record of the attempt number and the lock — never the password.

A **signed-in** session (after a correct six-digit code) sets the counter back to zero and stores the time of that sign-in.

**Thirty days** since last successful sign-in: treat as locked for inactivity. First-time partners who have never signed in are handled by Unlock User / create password, not by this inactivity rule.

Whether a bank employee lock must also be written into the bank directory is still an open IT question. Until that is answered, this application must still **block sign-in** when it has locked the person.

---

## One-time code

### Send

1. Both a registered mobile and a registered email must exist. If either is missing, this is an operations problem, not something the person can fix on the screen.
2. Create six random digits.
3. Store a one-way hash, a purpose (`sign-in` or `unlock`), who it belongs to, expiry = now + 10 minutes, tries = 0, resends = 0.
4. Send **the same digits** to mobile and email.
5. Forget the digits from memory. Never put them in a log or a response.
6. If **both** channels fail, tell the person the code could not be sent. Do not pretend they can continue.

### Check

1. The code must be exactly six digits.
2. If it is missing, expired, for the wrong purpose, or for a different person or attempt → refuse.
3. If it does not match:
   - add one try
   - if tries reach 5, **destroy** this attempt, send them back to sign-in, **do not lock the account**
   - otherwise tell them it is incorrect and how many tries remain
4. If it matches: **destroy** this attempt immediately (it cannot be reused) and continue (open a signed-in session, or complete unlock).

### Resend

1. If three resends have already happened → stop. They must start sign-in again.
2. If fewer than two minutes have passed since the last send → the button stays disabled.
3. Create **new** digits. The previous code is immediately useless. Reset the wrong-try counter for this new code. Keep the resend count. Restart the 10-minute clock.

A one-time code is bound to **this person and this attempt**. It is not valid for anyone else, and not valid after success.

---

## Finishing sign-in

After a correct sign-in one-time code:

1. Confirm the person is still active.
2. Reset the wrong-password counter; record last successful sign-in; clear a lock if it was only for failed passwords or inactivity.
3. Create the signed-in session in the server vault. Encrypt any provider tokens there.
4. Give the device only a cookie or a phone handle.
5. Snapshot certificate facts onto the session for later display if needed — **do not** refuse sign-in because a Specified Person certificate is expired. Refuse the **sale** later.
6. Write an audit record of successful sign-in (who, when, channel, session). No tokens, no password, no OTP.

Then send them to the home screen that matches Bank versus Insurance partner. Exact dashboard layout is a product decision.

---

## Unlock User

### Start

1. Captcha must pass.
2. Look up the person. Missing, disabled, or deactivated → “Your account is not active. Please contact support.” Do **not** unlock a disabled person.
3. Send an unlock one-time code (same OTP rules as sign-in).

### After a correct unlock code

Decide the outcome from who they are and why they came:

| Who and situation | Next step |
|---|---|
| Bank staff, locked | Unlock. Bank password unchanged. Return to bank sign-in. |
| Bank staff, password problem | Unlock only. Tell them to use bank password processes. Never show Create password. |
| Partner, never created a password | Create password. |
| Partner, forgot password or asked to reset | Create password. |
| Partner, password older than 60 days | Create password. |
| Partner, locked but still knows the password | Unlock. Password unchanged. Return to partner sign-in. |
| Already active, nothing to do | “Your account is already active. Please proceed to login.” |
| Disabled | Already stopped at start. |

---

## Partner password create or reset

Bank staff never enter this procedure.

1. They must be in the “partner must set password” state from Unlock User. Otherwise refuse.
2. New password and confirm must match.
3. Length 8 to 20 characters.
4. At least one uppercase letter, one lowercase letter, one number, one special character.
5. Must not contain their name or corporate email (including the part before `@`).
6. A previous password **may** be reused. Sequential characters (`abcd`, `1234`) are allowed if the other rules pass.
7. Strength labels (Weak / Medium / Strong) are optional hints. Passing the mandatory rules is what enables Save.
8. Send the new password to the partner identity product. **Do not** keep it in our database. Forget it from memory.
9. Remember only: “a password exists”, and “it expires in 60 days”.
10. Clear lock if they were locked.
11. Destroy the unlock session. **Do not** create a signed-in session.
12. “Your password has been created successfully. Please log in using your corporate email ID and new password.”

---

## Asking permission for a business action

When any feature is about to do something that matters:

1. Ask the access decision service: this person, this action, this record, this channel.
2. Apply, in this order: global suspension → explicit deny → explicit grant → role grant → **default deny**.
3. A partner from insurer A never sees insurer B, even if they share a branch.
4. Branch on a role **intersects** with the person’s mapped branches; it never expands them.
5. Missing or expired Specified Person certificate denies **selling** actions, not all work.
6. If the decision service is slow or down, **deny**. Do not retry immediately. Do not allow “because we could not check”.
7. Keep the decision, the reason, and the policy version for audit.

Signing in yesterday is not an answer to “may they submit this proposal now?”

---

## What every security event must record

Record the event. Never record the secret.

| Event | Record at least | Never record |
|---|---|---|
| Sign-in attempt | User type, a protected reference to the identifier, time, channel, result, why it failed | Password |
| Wrong password | Attempt number, time, result | Password |
| Account lock | Reason, time, which system locked them | — |
| One-time code sent or resent | Purpose, time, which channels, whether delivery worked | The digits |
| One-time code checked | How many tries, time, success or failure | The digits |
| Successful sign-in | Time, role, channel, session | Tokens |
| Unlock started or succeeded | User type, identifier reference, previous and new status, time | One-time code |
| Partner password created | Purpose, time, success or failure | Password |
| Password expired | Expiry date, what we did, time | Password |
| Sign-out or session expired | Time, reason, session | Tokens |
