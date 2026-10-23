# LOGIN MODULE - Repository Update Context

Accepted Login BRD v3.1 (17 October 2026). User-visible copy is the design text. Repository owner Mahesh accepted that copy on 23 October 2026 (`SUG-20261023-l31`). This file replaces Login BRD v1.0 as the module behaviour source (`DOC-005`).

## Context contract

- Authoritative source: the Login Module BRD v3.1 text accepted in this repository on 23 October 2026. The September docx (`Login_Module_BRD_Detailed.docx`, SHA-256 `6ac9339b60a4a921bb5484cad0c8f2266b7ee6667d876646034b790fe621c730`) is the superseded v1.0 source and is not the behaviour to build.
- Message rule: where v3.1 states both a catalogue sentence and an “as designed” sentence, the design sentence is the user-visible string. The catalogue IDs below already use that design sentence.
- Control rule: Sign in, Get OTP, Verify and Set Password stay disabled until their mandatory inputs are present. Blank-field sentences are not shown.
- Screen rule: a screen the v3.1 design notes say is absent is not required. Do not invent one.
- What this acceptance does not do: it does not build Captcha, OTP, lock, Unlock, Forgot Password or expiry on `workforce-access-bff`. That remainder stays parked at `SUG-20261008-otp`. It does not add mPIN. It does not create, reset or change a Bank RM network password.
- Approvals: Business, Information Security, Technology and QA rows are still blank. This acceptance is the repository-owner ratification of the text. It is not those four signatures.

## 1. Document control

| Attribute | Details |
|---|---|
| Project | New Digital Insurance Platform - Assisted Journey |
| Module | Login, Authentication and Account Recovery |
| Version | 3.1 |
| Status | Accepted as working module behaviour. Draft sign-off rows remain blank. |
| Prepared by | Product Team |
| Date | 17 October 2026 |
| Accepted in this repository | 23 October 2026, repository owner, design text wins |

| Version | Date | Description | Owner |
|---|---|---|---|
| 1.0 | 04-Sep-2026 | Original Login Module BRD. Superseded. | Product Team |
| 3.0 | 17-Oct-2026 | One-page credential entry. | Product Team |
| 3.1 | 17-Oct-2026 | Insurance RM Forgot Password uses its own Email ID and Captcha screen before OTP and password reset. | Product Team |
| 3.1-repo | 23-Oct-2026 | Design text accepted as the user-visible copy. v1.0 context replaced. | Repository owner |

## 2. Purpose

Both user types enter Login ID, Password and Captcha together on one login page. The platform then evaluates user status and routes the user to normal login, Unlock User, first-time password setup, expired-password reset, Forgot Password or an error state.

There is no separate Login ID screen and no separate Password screen.

## 3. Business objective

- One login page for both user types.
- OTP as the second factor for every successful login.
- Incorrect-password and inactivity locks.
- Unlock User for first-time activation and locked-account recovery.
- Insurance RMs recover a forgotten or expired password without mixing password reset with account unlock.
- Bank RM network-password ownership stays with the bank authentication system.

## 4. Scope

### 4.1 In scope

- User-type selection.
- One-page Login ID, Password and Captcha entry.
- Bank RM Employee ID and network-password validation.
- Insurance RM email ID and platform-password validation.
- OTP verification on every successful login.
- First-time Bank RM activation through Unlock User.
- First-time Insurance RM activation through Unlock User, then Set New Password.
- Account lock after three consecutive incorrect password attempts.
- Account lock after 30 days without a successful platform login.
- Unlock User for both user types.
- Insurance RM Forgot Password.
- Insurance RM password expiry after 60 days.
- Password specification and validation.
- Success, error, audit, session and integration requirements in this document.

### 4.2 Out of scope

- Bank RM network-password creation, reset or change.
- Maintenance of registered email ID and mobile number.
- User onboarding and master-data creation.
- Role and access administration.
- Dashboard features after login.
- Administration of OTP, SMS, email or Captcha providers.
- mPIN.
- A downloadable Unlock User guide.
- A screen for “already active and unlocked”.
- A dedicated service-error or no-internet screen.

## 5. User and authentication model

| Requirement | Bank RM | Insurance RM |
|---|---|---|
| Login ID | Employee ID | Registered corporate email ID |
| Password | Existing bank network password | Platform-managed password |
| First screen | Employee ID + Password + Captcha | Email ID + Password + Captcha |
| Switch label | AU Bank RM | Insurance Partner |
| OTP on every login | Mandatory | Mandatory |
| OTP delivery | Registered mobile and email | Registered mobile and email |
| Unlock User | First time, incorrect-password lock, inactivity lock | First time, incorrect-password lock, inactivity lock |
| First-time outcome | Activate the platform account. Keep the network password. | Activate the account and set a new platform password. |
| Forgot Password | Not available | Available, on its own screen |
| Password expiry | Controlled by the bank system | 60 days |
| Password reset in the platform | Not allowed | Allowed |

## 6. Common login page

Title: **Sign in to Insurance Hub**.

| Control | Bank RM | Insurance RM | Requirement |
|---|---|---|---|
| User type | AU Bank RM | Insurance Partner | One type stays selected. The selected option is filled. |
| Subtitle | Use your AU Bank employee ID and password to continue | Use your registered corporate email ID and password to continue. | Changes with the selected user type. |
| Login ID | Employee ID | Corporate Email ID | Mandatory. |
| Password | Network Password | Platform Password | Mandatory. Masked by default. |
| Show / Hide Password | Available | Available | Toggles visibility without changing the value. |
| Captcha | Mandatory | Mandatory | Must match the current Captcha. Refresh is available. |
| Sign in | Available | Available | Disabled until Login ID, Password and Captcha are entered. Then enabled. Fixed at the bottom. |
| Unlock account | Always shown below Sign in | Same | Starts Unlock User. Also the primary button on the first-time and locked screens. |
| Forgot password | Not displayed | Link directly below the Password field | Opens Reset your password. Login-page entries are not used. |
| Back | Shown | Shown | Top-left, on every login, OTP, unlock, reset and status screen. |
| Get Help | Shown | Shown | Small pill with a phone icon, top-right, on every login, OTP, unlock, reset and status screen. |

Field errors sit below the field. The border and label turn red. A Login ID in error shows a clear (✕) icon.

### 6.1 Validation order

1. COMMON-01: Validate all mandatory fields by keeping the action disabled until they are entered.
2. COMMON-02: Validate Captcha.
3. COMMON-03: Validate Login ID format and existence.
4. COMMON-04: Validate active / inactive status.
5. COMMON-05: Check the First-Time Login indicator.
6. COMMON-06: Check platform lock status.
7. COMMON-07: Route first-time or locked users to Unlock User.
8. COMMON-08: For an active, unlocked, non-first-time user, validate Password.
9. COMMON-09: Apply password-expiry logic for Insurance RM.
10. COMMON-10: If credentials are valid, generate the login OTP.
11. COMMON-11: After successful OTP verification, create the session and redirect to the applicable dashboard.

- A Captcha failure does not count as an incorrect-password attempt.
- An invalid Login ID does not count as an incorrect-password attempt.
- First-time or locked routing happens before an entered password is counted as successful or unsuccessful.
- Repeated clicks do not create duplicate authentication or OTP requests.

## 7. Bank RM normal login

Sign in, then **Verify it's you**, then **Welcome back, \<first name\>**. The welcome screen has no buttons.

- BANK-LOGIN-01: Bank RM selects AU Bank RM.
- BANK-LOGIN-02: User enters Employee ID, network password and Captcha on the same page.
- BANK-LOGIN-03: User selects Sign in.
- BANK-LOGIN-04: Platform validates Captcha.
- BANK-LOGIN-05: Platform validates Employee ID and active employee status through CBS.
- BANK-LOGIN-06: Platform checks First-Time Login and platform-lock status.
- BANK-LOGIN-07: If the user is active, unlocked and not first-time, the network password is validated.
- BANK-LOGIN-08: If the password is valid, OTP is sent to the registered mobile and email.
- BANK-LOGIN-09: User verifies OTP.
- BANK-LOGIN-10: Platform creates the session, resets the failed-password count, records the last successful login and redirects to the Bank RM dashboard.

## 8. Insurance RM normal login

Same sequence as Bank RM. The OTP screen shows the masked corporate email ID.

- IRM-LOGIN-01: Insurance RM selects Insurance Partner.
- IRM-LOGIN-02: User enters the registered email ID, platform password and Captcha on the same page.
- IRM-LOGIN-03: User selects Sign in.
- IRM-LOGIN-04: Platform validates Captcha and email ID.
- IRM-LOGIN-05: Platform checks active status, First-Time Login and platform-lock status.
- IRM-LOGIN-06: If active, unlocked and not first-time, the platform password is validated.
- IRM-LOGIN-07: If the password is not expired and is valid, OTP is sent to the registered mobile and email.
- IRM-LOGIN-08: User verifies OTP.
- IRM-LOGIN-09: Platform creates the session, resets the failed-password count, records the last successful login and redirects to the Insurance RM dashboard.

## 9. First-time login

Shown after Sign in when First-Time Login = Yes, for both user types.

| | |
|---|---|
| Title | Activate your account |
| Message | This is your first login to the Insurance Platform. Please use Unlock User to activate your account and continue. |
| Primary | Unlock account |
| Secondary | Back to sign in |

Normal login stops. The entered password is not completed as a login.

### 9.1 First-time Bank RM

After the Unlock OTP the user sees **Account activated** and returns to Sign in after three seconds. There is no Create Password step.

- BANK-FIRST-01 to BANK-FIRST-05: Employee ID, network password and Captcha are entered. Captcha, Employee ID and active CBS status are validated. First-Time Login = Yes stops normal login and asks for Unlock User.
- BANK-FIRST-06 to BANK-FIRST-08: OTP is sent to the registered mobile and email. On success the account is activated and First-Time Login becomes No.
- BANK-FIRST-09: The network password is unchanged.
- BANK-FIRST-10 to BANK-FIRST-12: The user returns to login and completes a normal login, including login OTP.

Success copy: **Account activated** — Sign in with your employee ID and your existing network password. Redirecting to sign in in 3s…

### 9.2 First-time Insurance RM

After the Unlock OTP the user goes to **Create new password**, then **Account unlocked**.

- IRM-FIRST-01 to IRM-FIRST-07: Email ID, current or default password and Captcha are entered. Captcha, email ID and active status are validated. First-Time Login = Yes stops normal login. OTP is sent and verified.
- IRM-FIRST-08 to IRM-FIRST-10: Set New Password is shown. The platform validates and saves the password and sets First-Time Login to No.
- IRM-FIRST-11 to IRM-FIRST-13: The user returns to login and completes a normal login with the new password, including login OTP.

Success copy: **Account unlocked** — Your new password is set. Sign in with your corporate email ID and new password. Redirecting to sign in in 3s…

## 10. Account-lock rules

| Trigger | Bank RM | Insurance RM | Resolution |
|---|---|---|---|
| Three consecutive incorrect passwords | Platform account locked | Platform account locked | Unlock User |
| 30 days without a successful platform login | Platform account locked | Platform account locked | Unlock User |
| Five incorrect OTP attempts | OTP session terminated. Account not locked. | Same | Restart login |
| Inactive or deactivated user | Access denied | Access denied | Contact support. No Unlock and no Forgot Password. |

The 30-day duration is backend configurable.

### 10.1 Incorrect-password counter

The insurance platform counts incorrect password responses for both user types. For Bank RM, the bank authentication service returns success or failure and the platform keeps its own counter.

- First failure: Incorrect password. You have 2 attempts remaining.
- Second failure: Incorrect password. You have 1 attempt remaining.
- Third consecutive failure: the locked screen in §10.3.
- A successful complete login resets the counter.
- Unlock User resets the counter.
- Captcha failure, invalid Login ID, OTP failure and first-time routing do not increment the counter.

### 10.2 Inactivity lock

The platform stores the last successful platform-login date and time. Only a successful password-plus-OTP login resets the inactivity counter. Unlock User, Forgot Password, password setting or OTP generation alone does not reset it.

### 10.3 Lock and account-status screens

| Screen | Title | Message | Actions |
|---|---|---|---|
| Three incorrect passwords | Your account has been locked | You entered an incorrect password 3 times. Please use Unlock User to verify with an OTP and continue. | Unlock account. Back to sign in. |
| 30 days inactive | Your account is locked due to inactivity | You haven't signed in for 30 days. Please use Unlock User to verify with an OTP and continue. | Unlock account. Back to sign in. |
| Inactive or deactivated | Your account is not active | Your account is not active. Please contact support to get access to Insurance Hub. | Contact support. Back to sign in. |
| Five incorrect OTP attempts | Too many incorrect attempts | Maximum attempts exceeded. Please restart the journey. Your account has not been locked. | Back to sign in. Get help. |

## 11. Unlock User

Screen **Unlock your account** — We'll send an OTP to your registered mobile number and email to verify it's you.

The screen shows the user-type switch, the Login ID (Employee ID or corporate email ID) and a Captcha. Get OTP stays disabled until both are entered. The Login ID is entered again. It is not carried from the login page. After Get OTP the user goes to **Verify it's you**.

- UNLOCK-01 to UNLOCK-04: The platform identifies that Unlock User is required. The user selects it, receives an OTP on the registered mobile and email, and verifies it.
- UNLOCK-05: Incorrect-password or inactivity lock: the platform removes the lock and resets the failed-password counter.
- UNLOCK-06: First-Time Login: the platform activates the user and updates First-Time Login.
- UNLOCK-07: Bank RM returns to login. The network password is unchanged.
- UNLOCK-08: A first-time Insurance RM goes to Set New Password, then returns to login.
- UNLOCK-09: Unlock User does not log the user in.

### 11.1 Outcomes

| Condition | After OTP | Password | Screen |
|---|---|---|---|
| First-time Bank RM | Activate. Return to login. | Network password unchanged | Account activated — Sign in with your employee ID and your existing network password. 3s. |
| Locked Bank RM | Remove the lock. Return to login. | Network password unchanged | Account unlocked — Sign in with your employee ID and your existing network password. 3s. |
| First-time Insurance RM | Activate. Open Set New Password. | New platform password | After the password step: Account unlocked — Your new password is set. Sign in with your corporate email ID and new password. 3s. |
| Locked Insurance RM | Remove the lock. Return to login. | Existing platform password unchanged | Account unlocked — Sign in with your corporate email ID and your existing password. No password step. 3s. |
| Already active and unlocked | No screen. No status change. No password change. | No change | Not in the accepted design. Do not add a screen. |
| Inactive or deactivated | Stop | No change | Your account is not active (§10.3). |

## 12. Insurance RM Forgot Password

Forgot Password is a separate Insurance RM journey. It opens from the Forgot password link on the Insurance RM login page. The new screen asks for the registered corporate email ID and Captcha. The password, email and Captcha from the login page are not carried and are not validated for this journey. Password is not on the initiation screen.

Bank RM has no Forgot Password link.

### 12.1 Initiation — Reset your password

We'll send an OTP to your registered mobile number and email to verify it's you. Fields start empty.

| Control | Mandatory | Requirement |
|---|---|---|
| Corporate Email ID | Yes | Registered Insurance RM email ID. |
| Captcha | Yes | Must match. Refresh is available. |
| Get OTP | — | Disabled until Email ID and Captcha are entered. Then enabled. Fixed at the bottom. |
| Back to sign in | No | Underlined link below Get OTP. Returns to the Insurance RM login page. The earlier frame without this link is superseded. |
| Get Help | No | Approved support information. |

Account not found: the field turns red and shows **We could not find an active Insurance RM account with this email ID.**

### 12.2 Flow

- FORGOT-01 to FORGOT-03: Insurance RM selects Forgot password. The separate screen opens. The user enters email ID and Captcha.
- FORGOT-04 to FORGOT-05: The platform validates Captcha, then that the email exists and belongs to an active Insurance RM.
- FORGOT-06 to FORGOT-08: A valid first-time user and a valid existing user may both continue. OTP goes to the registered mobile and the registered email.
- FORGOT-09 to FORGOT-13: After OTP, Set New Password is shown. The platform checks the password rules and that the two values match, saves the password, and sets First-Time Login to No where that applies.
- FORGOT-14 to FORGOT-15: Success, then the Insurance RM login page. The user completes a normal login with the new password, including login OTP.

The Forgot Password OTP session is separate from Unlock User and from the normal-login OTP session.

### 12.3 Locked and inactive during Forgot Password

| Condition | Title | Message | Actions |
|---|---|---|---|
| Locked | Unlock your account first | Your account is locked. Unlock it with an OTP first. Resetting your password won't unlock your account. | Unlock account. Back to sign in. |
| Inactive or deactivated | Your account is not active | We can't reset the password for this account. Please contact support. | Contact support. Back to sign in. |

Password reset does not remove an account lock. A locked Insurance RM unlocks before using the recovered password to log in.

Success copy: **Password changed** — Your password has been updated. Sign in again with your new password. 3s.

## 13. Insurance RM password expiry

After Sign in, when the password is expired:

| | |
|---|---|
| Title | Your password has expired |
| Message | Insurance Hub passwords expire every 60 days. Verify it's you with an OTP to set a new password. |
| Primary | Send OTP |
| Secondary | Back to sign in |

OTP is sent when the user taps Send OTP. After OTP, **Set a new password** — Your password has expired. Create a new one — it will be valid for the next 60 days. The checklist is visible before typing starts.

- PWD-EXP-01 to PWD-EXP-04: Email, password and Captcha are validated, including the entered password. Expiry after 60 days stops normal login.
- PWD-EXP-05 to PWD-EXP-08: OTP, then a compliant new password.
- PWD-EXP-09 to PWD-EXP-10: Expiry resets for the next 60 days. The user returns to login and completes a normal login.

Success copy: **Password updated** — Your new password is valid for the next 60 days. Sign in again to continue. 3s.

## 14. Set New Password

**Create new password** — Choose a strong password that's different from your previous one. Used for first-time setup and Forgot Password. Expiry uses the title and subtitle in §13.

| Control | Mandatory | Requirement |
|---|---|---|
| New Password | Yes | Masked. Validated as the user types. |
| Confirm Password | Yes | Must match New Password exactly. |
| Show / Hide | No | On both fields. |
| Checklist | No | Nine items, in the order below. Met items show a green tick. Unmet items show a grey ✕. The list appears once typing starts, except on the expiry screen, where it is shown from the start. |
| Strength | No | Three segments: Weak (one red), Medium (two orange), Strong (three green). |
| Match | — | Green tick and “Passwords match”, or the Confirm field turns red with “Both passwords must match”. |
| Previous password | — | Checked when Set password is tapped. The New Password field turns red with “New password must differ from the previous password.” and the last checklist item shows ✕. |
| Set password | — | Disabled until every checklist item is met and the two values match. Fixed at the bottom. |

Checklist order:

1. 8 to 16 characters
2. At least one uppercase letter
3. At least one lowercase letter
4. At least one number
5. At least one special character
6. No spaces
7. Doesn't contain your email ID
8. Not a default password (e.g. 123456)
9. Different from your previous password

### 14.1 Password specification

| Rule | Requirement |
|---|---|
| Length | 8 to 16 characters |
| Uppercase | At least one |
| Lowercase | At least one |
| Number | At least one |
| Special character | At least one |
| Spaces | Not allowed |
| Default password | Must not be 123456 |
| Email ID | Must not contain the complete registered email ID |
| History | Must differ from the immediately previous password |
| Confirm | Must match |
| Expiry | 60 days for a platform password |
| Storage | Never store or log the password in plain text |

v1.0’s 8-to-20 length, username ban, and “a previous password may be reused” are withdrawn.

## 15. OTP verification

One **Verify it's you** screen for every OTP purpose.

Enter the 6-digit OTP sent to your registered mobile number xx1234 and email ro••••@aubank.in. The numeric keypad opens automatically. Digits display as dots. The active box has a dark border and a cursor.

| Rule | Requirement |
|---|---|
| Format | Six numeric digits |
| Delivery | The same OTP to the registered mobile and the registered email |
| Validity | 10 minutes. Display: OTP valid for 09:59, counting down. |
| Attempts | Maximum five |
| After the fifth failure | End the OTP session and return to login. Do not lock the account. |
| Resend delay | Two minutes. Display: Didn't receive OTP? Resend in 01:59, then the link Resend OTP. A frame that says Resend in 00:30 is superseded. |
| Maximum resends | Three per session |
| New OTP | Invalidates the previous OTP |
| Successful OTP | Invalidated immediately |
| Display | Masked mobile and email only |
| Verify | Disabled until all six digits are entered. Then enabled. A three-dot loader while verifying. |
| Incorrect OTP | All six boxes turn red. Incorrect OTP. Please check and try again. Verify is disabled again. |
| Expired OTP | Boxes turn red. The OTP has expired. Please request a new OTP. Resend OTP stays available while resends remain. |

| Limit screen | Title | Message | Actions |
|---|---|---|---|
| Maximum attempts | Too many incorrect attempts | Maximum attempts exceeded. Please restart the journey. Your account has not been locked. | Back to sign in. Get help. |
| Maximum resends | Resend limit reached | Maximum resend limit reached. Please restart the journey to get a new OTP. | Back to sign in. Get help. |

OTP purposes: Bank RM login, Insurance RM login, first-time Bank RM unlock, first-time Insurance RM unlock, locked Bank RM unlock, locked Insurance RM unlock, Insurance RM Forgot Password, Insurance RM password expiry.

## 16. Success and redirection

Success screens are full-screen, with a green tick, a title, a message and a countdown. None of them has a button.

| Scenario | Title | Message | Redirect |
|---|---|---|---|
| Successful login | Welcome back, \<first name\> | You're signed in. Taking you to your dashboard. | Dashboard after 2 seconds |
| First-time Bank RM | Account activated | Sign in with your employee ID and your existing network password. | Sign in after 3 seconds |
| Bank RM unlock | Account unlocked | Sign in with your employee ID and your existing network password. | Sign in after 3 seconds |
| Insurance RM lock removal | Account unlocked | Sign in with your corporate email ID and your existing password. | Sign in after 3 seconds |
| First-time Insurance RM | Account unlocked | Your new password is set. Sign in with your corporate email ID and new password. | Sign in after 3 seconds |
| Forgot Password | Password changed | Your password has been updated. Sign in again with your new password. | Sign in after 3 seconds |
| Expired password | Password updated | Your new password is valid for the next 60 days. Sign in again to continue. | Sign in after 3 seconds |

## 17. Accepted message catalogue

Design text is the string. A withdrawn row is not shown.

| ID | When | Accepted message |
|---|---|---|
| VAL-001 | Login ID blank | Not shown. Sign in stays disabled. |
| VAL-002 | Password blank | Not shown. Sign in stays disabled. |
| VAL-003 | Captcha blank | Not shown. Sign in stays disabled. |
| VAL-004 | Wrong Captcha | Wrong captcha. Try again. |
| VAL-005 | Invalid Employee ID | Invalid Employee ID. Please check and try again. |
| VAL-006 | Insurance email not found on login | We couldn't find a partner account with this email ID. |
| VAL-007 | Inactive user on login | Your account is not active. Please contact support to get access to Insurance Hub. |
| VAL-008 | Incorrect password, first failure | Incorrect password. You have 2 attempts remaining. |
| VAL-009 | Incorrect password, second failure | Incorrect password. You have 1 attempt remaining. |
| VAL-010 | Third incorrect password | Your account has been locked. You entered an incorrect password 3 times. Please use Unlock User to verify with an OTP and continue. |
| VAL-011 | Inactivity lock | Your account is locked due to inactivity. You haven't signed in for 30 days. Please use Unlock User to verify with an OTP and continue. |
| VAL-012 | First-time login | Activate your account. This is your first login to the Insurance Platform. Please use Unlock User to activate your account and continue. |
| VAL-013 | OTP incomplete | Not shown. Verify stays disabled until six digits are entered. |
| VAL-014 | Incorrect OTP | Incorrect OTP. Please check and try again. |
| VAL-015 | OTP expired | The OTP has expired. Please request a new OTP. |
| VAL-016 | Maximum OTP attempts | Too many incorrect attempts. Maximum attempts exceeded. Please restart the journey. Your account has not been locked. |
| VAL-017 | Maximum resends | Resend limit reached. Maximum resend limit reached. Please restart the journey to get a new OTP. |
| VAL-018 | Password policy | The nine checklist items in §14. Set password stays disabled until all are met. |
| VAL-019 | Password mismatch | Both passwords must match |
| VAL-020 | Previous password | New password must differ from the previous password. |
| VAL-021 | Forgot Password email blank | Not shown. Get OTP stays disabled. |
| VAL-022 | Forgot Password Captcha blank | Not shown. Get OTP stays disabled. |
| VAL-023 | Forgot Password account not found | We could not find an active Insurance RM account with this email ID. |
| VAL-024 | Service unavailable | We are unable to process your request. Please try again later. No dedicated screen in the accepted design. |

Forgot Password locked and inactive use the sentences in §12.3, not VAL-007.

## 18. Security and session

- SEC-001: Encrypted transport for every authentication page.
- SEC-002: Never log passwords or OTP values.
- SEC-003: Mask email and mobile details.
- SEC-004: Tie an OTP to one user, one purpose and one session.
- SEC-005: Prevent duplicate submission.
- SEC-006: Create an authenticated session only after a successful login OTP.
- SEC-007: Do not expose authenticated pages after logout or session expiry.
- SEC-008: Follow the approved session timeout, concurrent-login and Captcha standards. Information Security still owns the provider administration and the numeric timeout values.
- SEC-009: Expire recovery sessions after completion or termination.

## 19. Audit logging

| Event | Minimum data |
|---|---|
| Login attempt | User type, protected Login ID, time, result, failure category, correlation ID |
| Password failure | Attempt number, time and result. Never the password. |
| First-time detection | User type, Login ID reference, time and routing result |
| Account lock | Reason, time, failed count or inactivity date |
| Unlock User | Reason, OTP result, old and new status, time |
| OTP events | Purpose, generation, resend, verification result and counts. Never the OTP. |
| Password create or reset | First-time, Forgot or Expired reason, user type and status, time and result. Never the password. |
| Successful login | User, role, time, channel and session ID |
| Logout or session expiry | User, session, time and reason |

## 20. Integration

| Integration | Purpose | Expectation |
|---|---|---|
| CBS / bank authentication | Bank RM Employee ID, active status and network password | Return status. Do not expose the password. |
| Insurance RM authentication | Email, password, first-time, expiry and status | Platform password and 60-day expiry. |
| Platform account service | First-time, lock, failed count, last login | Authoritative for platform lock and activation. |
| Contact service | Email and mobile | Both channels required. Masked data to the UI. |
| OTP service | Generate and validate OTP | All OTP rules and purposes in this document. |
| SMS / email gateways | Deliver OTP | Return delivery status. |
| Captcha service | Generate and validate Captcha | Refresh and the VAL-004 error. |
| Audit service | Immutable events | Exclude password and OTP values. |

## 21. Non-functional requirements

| Area | Requirement |
|---|---|
| Usability | One-page credential entry, the messages in §17, and one next action. |
| Responsive design | Approved desktop, tablet and mobile layouts. |
| Accessibility | Labels, keyboard order, focus and accessible errors. |
| Performance | Platform standards, subject to dependent systems. |
| Availability | Align with the authentication services. |
| Security | Bank Information Security. |
| Privacy | Mask contact data. Protect credentials. |
| Reliability | No duplicate OTP. Handle partial delivery failure. |
| Configurability | Lock days, attempts, OTP rules and expiry values are configurable. |

## 22. Acceptance criteria

- AC-001: Both users see Login ID, Password and Captcha on one login page.
- AC-002: No separate identifier screen and no separate password screen.
- AC-003: Captcha is validated before credential processing.
- AC-004: First-time status is checked for both users after Sign in.
- AC-005: A first-time Bank RM is directed to Unlock User and does not set a password.
- AC-006: A first-time Insurance RM is directed to Unlock User, then Set New Password.
- AC-007: A locked user is directed to Unlock User.
- AC-008: An active existing user with a valid password receives a login OTP.
- AC-009: OTP is mandatory on every successful login.
- AC-010: Three incorrect passwords lock the platform account.
- AC-011: Thirty days of inactivity lock the platform account.
- AC-012: Captcha failure and an invalid Login ID do not consume password attempts.
- AC-013: Unlock User does not log the user in.
- AC-014: Forgot Password is available only to Insurance RM and opens a separate Email ID and Captcha screen.
- AC-015: A first-time Insurance RM and an existing active Insurance RM can both continue through Forgot Password after Email ID and Captcha validation.
- AC-016: Forgot Password sends OTP to the registered mobile and email, then shows New Password and Confirm Password.
- AC-017: An expired Insurance RM password requires OTP and a password reset.
- AC-018: The §14 rules are enforced for creation and reset.
- AC-019: Five wrong OTPs end only the OTP session.
- AC-020: Audited events contain no password and no OTP value.
- AC-021: Every user-visible string matches §17. The withdrawn v1.0 strings and the pre-design catalogue strings are not shown.

## 23. Screen set

| Screen | Required content |
|---|---|
| Common login | User type, Login ID, Password, show/hide, Captcha, Sign in, Unlock account, Get Help, Back. Forgot password only for Insurance RM. |
| Forgot Password initiation | Insurance RM email ID, Captcha, Get OTP, Back to sign in, Get Help. No password field. |
| First-time / locked state | The §9 and §10.3 message and the Unlock account action. |
| OTP | Six digits, masked destinations, 10-minute timer, 2-minute resend, Verify. |
| Set New Password | New Password, Confirm Password, the nine-item checklist, strength, Set password. |
| Success | The §16 result and the automatic redirect. No button. |

Not in this screen set: already-active unlock, service-error, no-internet, Bank RM password create, Bank RM Forgot Password, mPIN, Unlock User PDF guide.

## 24. Key business rules

- KBR-01: ID, Password and Captcha are entered together for both users.
- KBR-02: First-Time Login is detected for both users after Sign in.
- KBR-03: Both first-time users must use Unlock User.
- KBR-04: Bank RM Unlock does not change the network password.
- KBR-05: A first-time Insurance RM sets a new platform password after the Unlock OTP.
- KBR-06: Every normal login requires OTP.
- KBR-07: Both accounts lock after three incorrect passwords.
- KBR-08: Both accounts lock after 30 days without a successful login.
- KBR-09: Unlock User is separate from Forgot Password.
- KBR-10: Forgot Password is Insurance RM only and starts on a separate Email ID and Captcha screen.
- KBR-11: An Insurance RM password expires after 60 days.
- KBR-12: A locked Insurance RM unlocks before the recovered password can be used to log in. Password reset itself does not unlock.
- KBR-13: OTP and password rules are the rules in §14 and §15.
- KBR-14: Authentication events are audited without password or OTP content.
- KBR-15: The user-visible string is the design sentence in §17.

## 25. Assumptions

- CBS validates Bank RM Employee ID, active status and network password.
- The insurance platform maintains first-time, lock, failed-attempt and last-login data.
- The Insurance RM master contains email, mobile, active status and password data.
- A registered mobile and email exist for both user types.
- SMS and email OTP delivery are operational.
- Information Security still confirms Captcha complexity, session timeout and concurrent login. Those numbers are not invented here.
