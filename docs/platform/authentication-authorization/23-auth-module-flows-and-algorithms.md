# 23 — Auth module flows and algorithms (R0)

**Status:** `AI-DRAFTED` · T3 · human Board 1 / Product / Security outstanding  
**Origin:** `SUG-20261005-amp` · `EPIC-006` · `DOC-024` · `PLAN-009`  
**Rule fidelity:** Login BRD rule IDs cited. Conflicts → OPEN ids. Do not invent Product or InfoSec decisions.

---

## 1. ALG-CAPTCHA

```
function issueCaptcha():
  challenge = CaptchaService.create()          # OPEN-AUTH-CAPTCHA owns complexity
  store(challenge.id, hash(answer), exp)
  return { captchaId, image, expiresInSeconds }

function verifyCaptcha(captchaId, answer):
  rec = consume(captchaId)                     # single use
  if rec is missing or expired:
    fail VAL-009
  if not matches(rec.hash, answer, rec.caseRule):
    fail VAL-009
  return Ok
```

**BRD:** §7.1, VAL-003/009, `SEC-010`.

---

## 2. ALG-CREDENTIAL-VERIFY (host-agnostic)

The **caller** is IdP chrome (option A) or BFF (option B). This algorithm is the same.

```
function verifyCredentials(userType, loginId, password, captchaId, captchaAnswer):
  require loginId present                         # VAL-001
  require password present                        # VAL-002
  verifyCaptcha(captchaId, captchaAnswer)         # VAL-003/009
  identity = lookup(userType, loginId)
  if identity is missing:
    if userType == BANK_RM: fail VAL-004
    else fail VAL-005                             # generic — no existence oracle
  if identity.lifecycle in {DISABLED, DEACTIVATED}:
    fail VAL-018                                  # BR-UNLOCK-003 also
  if isInactive30d(identity) or identity.lock in {INACTIVITY, FAILED_PASSWORD}:
    fail VAL-010
  if userType == BANK_RM:
    result = Adapter.adVerify(loginId, password)  # never persist password
  else:
    if not identity.passwordExists:
      fail redirect-hint UNLOCK first-time        # BR-LOGIN-005
    if identity.passwordExpiresAt < now:
      fail redirect-hint UNLOCK expired           # KBR-12
    result = Adapter.partnerPasswordVerify(loginId, password)
  if result.denied:
    return ALG_PASSWORD_FAILURE(identity)         # VAL-006/007/008
  # do not mint AUTHENTICATED here
  return PendingOtp(identity)
```

**BRD:** `BR-LOGIN-001`…`006`. **OPEN-AUTH-CEREMONY** decides which process invokes this. **Never log password.**

---

## 3. ALG-PASSWORD-FAILURE / ALG-LOCK

```
function ALG_PASSWORD_FAILURE(identity):
  n = identity.consecutiveIncorrectPassword + 1
  persist(n)
  audit(PASSWORD_FAILURE, attempt=n)              # no password
  if n == 1: return Fail(VAL-006, remaining=2)
  if n == 2: return Fail(VAL-007, remaining=1)
  identity.lock = FAILED_PASSWORD
  identity.lockedAt = now
  audit(ACCOUNT_LOCK, reason=FAILED_PASSWORD)
  # OPEN-AUTH-BANK-LOCK: optionally reflect to AD
  return Fail(VAL-008, remaining=0)

function isInactive30d(identity):
  if identity.lastSuccessfulLoginAt is null:
    return false                                  # first-time partner uses Unlock, not this
  return now - identity.lastSuccessfulLoginAt >= 30 days

function onAuthenticated(identity):
  identity.consecutiveIncorrectPassword = 0
  identity.lastSuccessfulLoginAt = now
  identity.lock = none
```

**BRD:** `BR-LOGIN-007/008/009`, table 9.

---

## 4. ALG-OTP-ISSUE / VERIFY / RESEND

```
function issueOtp(identity, purpose, parentSessionId):
  # purpose in {LOGIN, UNLOCK}
  contacts = loadRegisteredMobileAndEmail(identity)
  require both present                            # BRD assumption §14
  otp = secureRandomDigits(6)
  rec = {
    id: newUlid(),
    purpose, parentSessionId, identityId,
    hash: hash(otp),
    exp: now + 10 minutes,
    attempts: 0,
    resends: 0,
    lastSentAt: now
  }
  persist(rec)
  sendSameOtp(contacts.mobile, contacts.email, otp)  # never log otp
  zeroize(otp)
  audit(OTP_GENERATED, purpose, channels, deliveryResult)
  if both channels failed:
    fail VAL-016
  return { rec.id, maskedMobile, maskedEmail }

function verifyOtp(sessionId, entered, expectedPurpose):
  rec = load(sessionId)
  if rec is missing: fail VAL-013
  if rec.purpose != expectedPurpose: fail VAL-013
  if entered blank or not 6 digits: fail VAL-011
  if now > rec.exp: fail VAL-013
  if rec.hash != hash(entered):
    rec.attempts += 1
    if rec.attempts >= 5:
      destroy(rec)                                # account NOT locked
      audit(OTP_VERIFY, fail, attempts=5)
      fail VAL-014
    fail VAL-012 with remaining = 5 - rec.attempts
  destroy(rec)                                    # success → immediately invalid
  audit(OTP_VERIFY, success)
  return Ok(rec.identityId, rec.purpose)

function resendOtp(sessionId):
  rec = load(sessionId)
  if rec.resends >= 3: fail VAL-015
  if now < rec.lastSentAt + 2 minutes: fail not-yet (UI disabled)
  newOtp = secureRandomDigits(6)
  rec.hash = hash(newOtp)
  rec.exp = now + 10 minutes
  rec.resends += 1
  rec.lastSentAt = now
  rec.attempts = 0                                # new code; previous invalid (table 11 row 8)
  sendSameOtp(..., newOtp)
  zeroize(newOtp)
  audit(OTP_RESEND, rec.resends)
```

**BRD:** table 10–11, `KBR-10`, `SEC-005`, AC-006…009. OTP is mapped to user **and** verification session.

---

## 5. ALG-LOGIN-COMPLETE

```
function completeLogin(pendingOtpSession, enteredOtp):
  identityId = verifyOtp(pendingOtpSession.otpId, enteredOtp, LOGIN)
  identity = load(identityId)
  require identity.lifecycle == ACTIVE
  onAuthenticated(identity)
  session = mintAuthenticatedSession(identity)    # vault provider tokens; opaque to client
  snapshotCertsOntoSession(identity)              # VR-017 — do not gate
  audit(LOGIN_SUCCEEDED, userType, sessionId)
  return Session(userType, returnUri)
```

**BRD:** `SEC-006`, AC-015. Dashboard *widgets* are out (BRD §4.2); route hint is `userType`.

---

## 6. ALG-UNLOCK

```
function startUnlock(userType, loginId, captcha, intent?):
  verifyCaptcha(...)
  identity = lookup(userType, loginId)
  if identity missing or lifecycle in {DISABLED, DEACTIVATED}:
    fail VAL-018                                  # BR-UNLOCK-003 — do not unlock
  otp = issueOtp(identity, UNLOCK, newUnlockSession)
  return PendingUnlock(otp, identity)

function completeUnlockOtp(unlockSession, otp):
  identityId = verifyOtp(..., UNLOCK)
  identity = load(identityId)
  condition = classify(identity, unlockSession.intent)
  # classify uses BRD table 13:
  # BANK_RM locked → UNLOCK_ONLY
  # BANK_RM password issue → UNLOCK_ONLY (tell user to use bank process)
  # PARTNER first-time (passwordExists=false) → SET_PASSWORD
  # PARTNER forgot / expired / intent=RESET_PASSWORD → SET_PASSWORD
  # PARTNER locked retaining password / intent=UNLOCK → UNLOCK_ONLY
  # already ACTIVE and no reset required → VAL-017
  # DISABLED → VAL-018 (should have been stopped at start)
  if condition == UNLOCK_ONLY:
    PDP.unlock(identity, reasons={FAILED_PASSWORD, INACTIVITY})
    audit(UNLOCK_SUCCEEDED)
    return ReturnToLogin(message by userType)     # table 17
  if condition == SET_PASSWORD:
    require identity.userType == INSURER_REPRESENTATIVE
    mark unlockSession.state = UNLOCK_PASSWORD
    return RequirePassword
  if condition == ALREADY_ACTIVE:
    fail VAL-017
```

**BRD:** `BR-UNLOCK-001/002/003`, table 13–14, AC-010…012/017.

---

## 7. ALG-PWD-POLICY / ALG-SET-PARTNER-PASSWORD

```
function ALG_PWD_POLICY(newPassword, confirm, identity):
  if newPassword != confirm: fail VAL-020
  if length not in 8..20: fail VAL-019
  if missing upper or lower or digit or special: fail VAL-019
  haystack = lower(newPassword)
  if identity.displayName.lower() in haystack: fail VAL-021
  if identity.email.localPart.lower() in haystack: fail VAL-021
  if identity.email.lower() in haystack: fail VAL-021
  # history: no restriction (table 16 row 10)
  # sequential values: allowed (row 12)
  return Ok

function setPartnerPassword(unlockSession, newPassword, confirm):
  require unlockSession.state == UNLOCK_PASSWORD
  require unlockSession.userType == PARTNER
  identity = load(unlockSession.identityId)
  ALG_PWD_POLICY(newPassword, confirm, identity)
  Adapter.setPartnerPassword(identity.providerSubjectId, newPassword)  # Keycloak; no platform plaintext
  zeroize(newPassword, confirm)
  PDP.passwordMetadata(identity, exists=true, expiresAt=now+60d)
  PDP.unlock(identity, reasons={FAILED_PASSWORD, INACTIVITY, EXPIRED_PASSWORD})
  destroy(unlockSession)                          # BR-PWD-002 — not authenticated
  audit(PARTNER_PASSWORD_SET, purpose, success)
  return ReturnToPartnerLogin
```

**BRD:** `BR-PWD-001/002`, table 15–16, `KBR-11/12`, AC-013/014. Bank RM must never enter this function.

---

## 8. ALG-AUTHORIZE (pointer)

Normative PDP algorithm remains [`UC-05-authorization-decision.md`](../../journey-execution/flows/UC-05-authorization-decision.md). This pack does not fork it. Login success is **not** an authorization grant (`ID-20`).

---

## 9. Audit events (BRD §10)

| Event | Minimum fields | Forbid |
|---|---|---|
| Login attempt | userType, loginId ref, ts, channel, result, failure category, correlation | password |
| Password failure | attempt number, ts, result | password |
| Account lock | reason, ts, source | — |
| OTP generation / resend / verify | purpose, counts, delivery result | OTP value |
| Successful login | ts, userType/role, channel, sessionId | tokens |
| Unlock start / success | userType, loginId ref, previous/new status | OTP |
| Partner password set | purpose, ts, result | password |
| Password expiry | expiry date, action, ts | password |
| Logout / session expiry | ts, reason, sessionId | tokens |

---

## 10. Validation → algorithms

| VAL | Algorithm |
|---|---|
| VAL-001…005 | ALG-CREDENTIAL-VERIFY lookup |
| VAL-006…008 | ALG-PASSWORD-FAILURE |
| VAL-009 | ALG-CAPTCHA |
| VAL-010 | isInactive30d / lock overlay |
| VAL-011…016 | ALG-OTP-* |
| VAL-017…018 | ALG-UNLOCK classify |
| VAL-019…021 | ALG-PWD-POLICY |
| VAL-022 | gateway / content down |

---

## 11. Done for this document

- [x] Deterministic algorithms with BR-* / VAL-* / KBR-* trace
- [x] OPEN conflicts untouched as decisions
- [x] Platform OTP + partner password; login-password host left OPEN
- [ ] Product / Security confirmation of OPEN-AUTH-* rows
