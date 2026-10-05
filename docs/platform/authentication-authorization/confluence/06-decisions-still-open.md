# Decisions that are still open

**Audience:** stakeholders, security, IT, product, and new joiners who need to know what is **not** decided yet.  
**Purpose:** name the open questions so nobody invents an answer in a meeting or in code.  
**Companion:** [start here](./00-start-here.md)

Until the named owner answers, implementers follow the **interim** column. Do not pick a favourite interpretation because it is easier to build.

---

## Where does the person type the password?

**The question.** The login business requirements show an employee ID or email field, a password field, and a captcha **on our screens**. The current public backend starts sign-in by returning an address for a **bank-hosted or partner identity page**, and does not accept a password.

**Why it is hard.** The sales application must never receive access tokens. Putting a password on our screen is allowed only if the password is sent to the workforce access service and **stops there**. Putting the password on a bank-hosted page is closer to how the backend works today, but the fields the business described live on our wireframes.

**Who decides.** Security, jointly with architecture. This is a trust-boundary choice, not a styling choice.

**Until then.** Do not add a public “sign in with password” request. Both journeys still send our six-digit code to mobile and email before the home screen. See [Journeys](./02-journeys.md).

---

## Is bank or partner multi-factor also required on top of our six-digit code?

**The question.** Information Security may require an extra factor at the identity product (for example a bank authenticator app).

**Interim.** Our six-digit code to **mobile and email**, with the timings in [Rules the system follows](./04-rules-the-system-follows.md), is required. A generic identity-product one-time code is **not** a substitute, because it would not give us “same code on both channels, ten minutes, five tries, two-minute resend, three resends, and do not lock after five wrong codes”. Extra multi-factor, if required, is **in addition**.

**Who decides.** Security / Information Security.

---

## How hard is the captcha, and how long does it last?

**The question.** The login business requirements require a captcha and a refresh. They leave complexity and expiry to Information Security and the captcha product.

**Interim.** Issue, verify once, refresh on failure. Do not hard-code a house style.

**Who decides.** Information Security.

---

## How long may a session sit idle, and may two devices be signed in?

**The question.** After sign-out, Back must not show a signed-in page. Idle time, maximum length, and concurrent sessions follow the **approved bank Information Security standard**, which is not numbered in the login business requirements.

**Interim.** Sessions live in the server vault and can be destroyed. Do not invent minutes.

**Who decides.** Information Security.

---

## When a bank employee is locked here, must the bank directory also lock them?

**The question.** Three wrong bank passwords lock the person in **this** application. The business requirements also say the bank authentication integration should reflect locked status.

**Interim.** This application **must** refuse sign-in when it has locked the person. Writing the lock back to the bank directory is a seam, not assumed.

**Who decides.** IT and Security.

---

## What is the support phone or email, and where is the Unlock User PDF?

**The question.** Get Help and Download Unlock Guide are required. Content is supplied and maintained by IT.

**Interim.** The requests exist. If content is missing, show “We are unable to process your request at this time. Please try again later.” Do not invent a phone number.

**Who decides.** IT.

---

## Which home screen opens after sign-in?

**The question.** Bank staff and partner staff land on different home screens. Exact layout and widgets are product.

**Interim.** After a successful six-digit code, send the person to the return location allowed for their type. Do not build the lead pipeline or dashboard widgets inside sign-in.

**Who decides.** Product.

---

## What is not open — do not re-argue these

These are already decided. A wireframe filename or a vendor screenshot does not reopen them.

- The sales application never holds access tokens.
- Keycloak is not the permissions brain.
- There is no Forgot password link.
- There is no mPIN in this module.
- This application never creates or resets a **bank** password.
- Unlock User is the only recovery action on the sign-in screen.
- A partner is not automatically signed in after creating a password.
- Five wrong one-time codes do not lock the account.
- Disabled people are not “unlocked” back into service.
- Customer self-service sign-in is out of this module.
