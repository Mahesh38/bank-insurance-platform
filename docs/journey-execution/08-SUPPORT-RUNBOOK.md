# 08 — Support Runbook (`RB-*`)

**Every error the platform can return, and what L1 or L2 support does about it.**

> **GENERATED — do not hand-edit.** Produced from the error registry by
> `python3 scripts/support/build-error-runbook.py`, and checked against it by
> `ErrorRunbookParityTest`. Edit the guidance in the generator, not here: a support page
> maintained separately from the code describes last quarter's behaviour.

Status: `AI-DRAFTED` · Owner: Amit (Board 2) + Shivanshi (Board 7) · Origin: `ERR-007`

---

## 1. How to use this

Every error response carries an `incidentId` and a `code`. The RM can read both off their
screen.

1. Take the `incidentId` and search the log platform for it. That returns **every line of
   that failure, across every service** — the `code`, the service that emitted it, the
   `originService` that actually failed, and the engineer-facing `reason`.
2. Look the `code` up below. The five rows tell you what it means, whether it is a defect,
   what to do, when to escalate, and what never to do.

The caller is deliberately not shown the `reason`. That is not information being withheld
from support — it is information being withheld from a device, and the `incidentId` is how
you retrieve it.

> **The `Never` row is the one that matters.** Most rows describe a control working
> correctly. Clearing a control from a support seat is an audit finding, however reasonable
> it looks at the time.

---

## 2. Index by category

| Category | What it means for support | Codes |
|---|---|---|
| **AUTHENTICATION** | Identity not established | [`UNAUTHORIZED`](#rb-unauthorized) · [`SESSION_INVALID`](#rb-session_invalid) · [`SESSION_EXPIRED`](#rb-session_expired) · [`SESSION_REVOKED`](#rb-session_revoked) · [`AUTHENTICATION_FAILED`](#rb-authentication_failed) · [`STEP_UP_REQUIRED`](#rb-step_up_required) · [`INVALID_STATE`](#rb-invalid_state) · [`CODE_ALREADY_CONSUMED`](#rb-code_already_consumed) · [`RETURN_LOCATION_NOT_ALLOWED`](#rb-return_location_not_allowed) |
| **AUTHORIZATION** | Known caller, not permitted | [`FORBIDDEN`](#rb-forbidden) · [`CSRF_REJECTED`](#rb-csrf_rejected) · [`DEFAULT_DENY`](#rb-default_deny) · [`EXPLICIT_DENY`](#rb-explicit_deny) · [`ACCOUNT_SUSPENDED`](#rb-account_suspended) · [`ORIGINATION_RM_ONLY`](#rb-origination_rm_only) · [`ASSIST_ONLY_ACTOR`](#rb-assist_only_actor) · [`CROSS_INSURER_DENIED`](#rb-cross_insurer_denied) · [`OUT_OF_BRANCH_SCOPE`](#rb-out_of_branch_scope) · [`BREAK_GLASS_INVALID`](#rb-break_glass_invalid) · [`AUTHORIZATION_UNAVAILABLE`](#rb-authorization_unavailable) · [`SERVICE_IDENTITY_REJECTED`](#rb-service_identity_rejected) · [`CUSTOMER_NOT_IN_BOOK`](#rb-customer_not_in_book) |
| **COMPLIANCE_GATE** | A regulator-mandated refusal — never override | [`SP_CERTIFICATION_REQUIRED`](#rb-sp_certification_required) · [`SUITABILITY_REQUIRED`](#rb-suitability_required) · [`CONSENT_REQUIRED`](#rb-consent_required) · [`ATTRIBUTION_NOT_CALLER_SUPPLIED`](#rb-attribution_not_caller_supplied) · [`PAYMENT_DEVICE_ISOLATION`](#rb-payment_device_isolation) · [`PAYMENT_NOT_RECONCILED`](#rb-payment_not_reconciled) · [`RM_NOT_CERTIFIED`](#rb-rm_not_certified) |
| **CONFIG** | Platform could not configure itself — fail closed | [`CONFIG_VERSION_REQUIRED`](#rb-config_version_required) · [`CONFIGURATION_UNRESOLVABLE`](#rb-configuration_unresolvable) |
| **CONFLICT** | State disagrees | [`QUOTE_EXPIRED`](#rb-quote_expired) · [`PROPOSAL_NOT_PAYABLE`](#rb-proposal_not_payable) · [`CONFLICT`](#rb-conflict) · [`IDEMPOTENCY_CONFLICT`](#rb-idempotency_conflict) · [`REQUEST_IN_PROGRESS`](#rb-request_in_progress) · [`ILLEGAL_TRANSITION`](#rb-illegal_transition) · [`ASSESSMENT_IMMUTABLE`](#rb-assessment_immutable) · [`WITHDRAWAL_NOT_PERMITTED`](#rb-withdrawal_not_permitted) · [`PAYMENT_ALREADY_IN_PROGRESS`](#rb-payment_already_in_progress) · [`PAYMENT_STATE_UNCERTAIN`](#rb-payment_state_uncertain) · [`PREMIUM_MISMATCH`](#rb-premium_mismatch) |
| **INTERNAL** | Our defect | [`INTERNAL_ERROR`](#rb-internal_error) |
| **NOT_FOUND** | No such record for this caller | [`RESOURCE_NOT_FOUND`](#rb-resource_not_found) |
| **RATE_LIMIT** | Throttled | [`RATE_LIMITED`](#rb-rate_limited) |
| **UPSTREAM** | A dependency failed | [`UPSTREAM_BUSINESS_ERROR`](#rb-upstream_business_error) · [`UPSTREAM_AUTH_FAILURE`](#rb-upstream_auth_failure) · [`UPSTREAM_UNAVAILABLE`](#rb-upstream_unavailable) · [`UPSTREAM_TIMEOUT`](#rb-upstream_timeout) · [`UPSTREAM_BAD_RESPONSE`](#rb-upstream_bad_response) · [`QUOTE_TIMEOUT`](#rb-quote_timeout) · [`PROPOSAL_REJECTED`](#rb-proposal_rejected) · [`IDENTITY_PROVIDER_UNAVAILABLE`](#rb-identity_provider_unavailable) |
| **VALIDATION** | The caller sent something invalid | [`VALIDATION_ERROR`](#rb-validation_error) · [`INVALID_REQUEST`](#rb-invalid_request) · [`MISSING_REQUIRED_FIELD`](#rb-missing_required_field) · [`MISSING_IDEMPOTENCY_KEY`](#rb-missing_idempotency_key) · [`UNSUPPORTED_LOB`](#rb-unsupported_lob) · [`AGENT_ATTRIBUTION_MISSING`](#rb-agent_attribution_missing) · [`PAYLOAD_TOO_LARGE`](#rb-payload_too_large) · [`SCHEMA_INVALID`](#rb-schema_invalid) · [`LOB_REQUIRED`](#rb-lob_required) · [`OPPORTUNITY_REQUIRED`](#rb-opportunity_required) · [`INVALID_OFFER_REFERENCE`](#rb-invalid_offer_reference) |

---

## 3. Pages

### RB-AUTHENTICATION_FAILED

`AUTHENTICATION_FAILED` · **HTTP 401** · AUTHENTICATION · retry: AFTER_REAUTH · AUDIT · source: 04 section 3

> The caller sees: **Sign-in unsuccessful** — The sign-in could not be completed.

| | |
|---|---|
| **What it means** | Sign-in did not succeed. The response deliberately does not say why. |
| **Is it a defect?** | No. |
| **L1 action** | Ask the RM to try again, and confirm their employment status and branch assignment are current with their supervisor. |
| **L2 escalation** | Escalate if one user consistently fails while colleagues in the same branch succeed. The logs separate the four distinct causes; the response never does. |
| **Never** | **Never tell a caller which part failed.** Confirming whether a user exists is a user-enumeration oracle. |

### RB-CODE_ALREADY_CONSUMED

`CODE_ALREADY_CONSUMED` · **HTTP 400** · AUTHENTICATION · retry: NO · SECURITY_EVENT · source: 04 section 3

> The caller sees: **Sign-in unsuccessful** — Please start the sign-in again.

| | |
|---|---|
| **What it means** | The caller's identity was not established, or their session is no longer valid. |
| **Is it a defect?** | No, in almost every case. It is the session lifecycle working. |
| **L1 action** | Ask the RM to sign in again. Draft input held locally is preserved. |
| **L2 escalation** | Escalate if sign-in fails repeatedly for one user while others succeed, or if the same user is signed out repeatedly within one shift. |
| **Never** | Never ask the RM for their password or OTP, and never read one back to them. |

### RB-INVALID_STATE

`INVALID_STATE` · **HTTP 400** · AUTHENTICATION · retry: NO · SECURITY_EVENT · source: 04 section 3

> The caller sees: **Sign-in unsuccessful** — Please start the sign-in again.

| | |
|---|---|
| **What it means** | The caller's identity was not established, or their session is no longer valid. |
| **Is it a defect?** | No, in almost every case. It is the session lifecycle working. |
| **L1 action** | Ask the RM to sign in again. Draft input held locally is preserved. |
| **L2 escalation** | Escalate if sign-in fails repeatedly for one user while others succeed, or if the same user is signed out repeatedly within one shift. |
| **Never** | Never ask the RM for their password or OTP, and never read one back to them. |

### RB-RETURN_LOCATION_NOT_ALLOWED

`RETURN_LOCATION_NOT_ALLOWED` · **HTTP 400** · AUTHENTICATION · retry: NO · SECURITY_EVENT · source: 04 section 3

> The caller sees: **Sign-in unsuccessful** — Please start the sign-in again.

| | |
|---|---|
| **What it means** | The caller's identity was not established, or their session is no longer valid. |
| **Is it a defect?** | No, in almost every case. It is the session lifecycle working. |
| **L1 action** | Ask the RM to sign in again. Draft input held locally is preserved. |
| **L2 escalation** | Escalate if sign-in fails repeatedly for one user while others succeed, or if the same user is signed out repeatedly within one shift. |
| **Never** | Never ask the RM for their password or OTP, and never read one back to them. |

### RB-SESSION_EXPIRED

`SESSION_EXPIRED` · **HTTP 401** · AUTHENTICATION · retry: AFTER_REAUTH · AUDIT · source: 04 section 3

> The caller sees: **Session expired** — Please sign in again to continue.

| | |
|---|---|
| **What it means** | The caller's identity was not established, or their session is no longer valid. |
| **Is it a defect?** | No, in almost every case. It is the session lifecycle working. |
| **L1 action** | Ask the RM to sign in again. Draft input held locally is preserved. |
| **L2 escalation** | Escalate if sign-in fails repeatedly for one user while others succeed, or if the same user is signed out repeatedly within one shift. |
| **Never** | Never ask the RM for their password or OTP, and never read one back to them. |

### RB-SESSION_INVALID

`SESSION_INVALID` · **HTTP 401** · AUTHENTICATION · retry: AFTER_REAUTH · AUDIT · source: 04 section 3

> The caller sees: **Sign-in required** — Please sign in and try again.

| | |
|---|---|
| **What it means** | The caller's identity was not established, or their session is no longer valid. |
| **Is it a defect?** | No, in almost every case. It is the session lifecycle working. |
| **L1 action** | Ask the RM to sign in again. Draft input held locally is preserved. |
| **L2 escalation** | Escalate if sign-in fails repeatedly for one user while others succeed, or if the same user is signed out repeatedly within one shift. |
| **Never** | Never ask the RM for their password or OTP, and never read one back to them. |

### RB-SESSION_REVOKED

`SESSION_REVOKED` · **HTTP 401** · AUTHENTICATION · retry: NO · SECURITY_EVENT · source: 04 section 3

> The caller sees: **Signed out** — Your session was ended. Please sign in again.

| | |
|---|---|
| **What it means** | The caller's identity was not established, or their session is no longer valid. |
| **Is it a defect?** | No, in almost every case. It is the session lifecycle working. |
| **L1 action** | Ask the RM to sign in again. Draft input held locally is preserved. |
| **L2 escalation** | Escalate if sign-in fails repeatedly for one user while others succeed, or if the same user is signed out repeatedly within one shift. |
| **Never** | Never ask the RM for their password or OTP, and never read one back to them. |

### RB-STEP_UP_REQUIRED

`STEP_UP_REQUIRED` · **HTTP 401** · AUTHENTICATION · retry: AFTER_REMEDIATION · AUDIT · source: 04 section 3

> The caller sees: **Additional verification required** — Please complete verification to continue.

| | |
|---|---|
| **What it means** | The caller's identity was not established, or their session is no longer valid. |
| **Is it a defect?** | No, in almost every case. It is the session lifecycle working. |
| **L1 action** | Ask the RM to sign in again. Draft input held locally is preserved. |
| **L2 escalation** | Escalate if sign-in fails repeatedly for one user while others succeed, or if the same user is signed out repeatedly within one shift. |
| **Never** | Never ask the RM for their password or OTP, and never read one back to them. |

### RB-UNAUTHORIZED

`UNAUTHORIZED` · **HTTP 401** · AUTHENTICATION · source: pre-catalogue

> The caller sees: **Sign-in required** — Please sign in and try again.

| | |
|---|---|
| **What it means** | The caller's identity was not established, or their session is no longer valid. |
| **Is it a defect?** | No, in almost every case. It is the session lifecycle working. |
| **L1 action** | Ask the RM to sign in again. Draft input held locally is preserved. |
| **L2 escalation** | Escalate if sign-in fails repeatedly for one user while others succeed, or if the same user is signed out repeatedly within one shift. |
| **Never** | Never ask the RM for their password or OTP, and never read one back to them. |

### RB-ACCOUNT_SUSPENDED

`ACCOUNT_SUSPENDED` · **HTTP 403** · AUTHORIZATION · retry: NO · AUDIT · source: 04 section 4

> The caller sees: **Account suspended** — Please contact your supervisor.

| | |
|---|---|
| **What it means** | The caller is known, and is not permitted to do this. |
| **Is it a defect?** | No. Default-deny is the design. |
| **L1 action** | Confirm the RM's role and branch with their supervisor. Access changes go through the normal entitlement request, never through support. |
| **L2 escalation** | Escalate if the RM should have access by their role and does not — that is an entitlement or policy defect. |
| **Never** | Never work around it, and never ask an engineer to grant access directly. |

### RB-ASSIST_ONLY_ACTOR

`ASSIST_ONLY_ACTOR` · **HTTP 403** · AUTHORIZATION · retry: NO · COMPLIANCE_EVENT · source: 04 section 4

> The caller sees: **Not permitted** — Your role can assist with this but not perform it.

| | |
|---|---|
| **What it means** | The caller is known, and is not permitted to do this. |
| **Is it a defect?** | No. Default-deny is the design. |
| **L1 action** | Confirm the RM's role and branch with their supervisor. Access changes go through the normal entitlement request, never through support. |
| **L2 escalation** | Escalate if the RM should have access by their role and does not — that is an entitlement or policy defect. |
| **Never** | Never work around it, and never ask an engineer to grant access directly. |

### RB-AUTHORIZATION_UNAVAILABLE

`AUTHORIZATION_UNAVAILABLE` · **HTTP 403** · AUTHORIZATION · retry: YES · AUDIT · source: 04 section 4

> The caller sees: **Temporarily unavailable** — Please try again shortly.

| | |
|---|---|
| **What it means** | The authorization service did not answer, so the platform denied by default. |
| **Is it a defect?** | Yes, in effect — this is fail-closed behaviour and a sustained rate is an incident. |
| **L1 action** | Ask the RM to retry once. If it persists across users, it is an incident. |
| **L2 escalation** | Escalate on any sustained rate. This is not normal background noise. |
| **Never** | Never treat it as a permissions problem for one user. It is a dependency failure. |

### RB-BREAK_GLASS_INVALID

`BREAK_GLASS_INVALID` · **HTTP 403** · AUTHORIZATION · retry: NO · SECURITY_EVENT · source: 04 section 4

> The caller sees: **Not permitted** — You do not have access to this action.

| | |
|---|---|
| **What it means** | The caller is known, and is not permitted to do this. |
| **Is it a defect?** | No. Default-deny is the design. |
| **L1 action** | Confirm the RM's role and branch with their supervisor. Access changes go through the normal entitlement request, never through support. |
| **L2 escalation** | Escalate if the RM should have access by their role and does not — that is an entitlement or policy defect. |
| **Never** | Never work around it, and never ask an engineer to grant access directly. |

### RB-CROSS_INSURER_DENIED

`CROSS_INSURER_DENIED` · **HTTP 403** · AUTHORIZATION · retry: NO · COMPLIANCE_EVENT · source: 04 section 4

> The caller sees: **Not permitted** — You do not have access to this action.

| | |
|---|---|
| **What it means** | The caller is known, and is not permitted to do this. |
| **Is it a defect?** | No. Default-deny is the design. |
| **L1 action** | Confirm the RM's role and branch with their supervisor. Access changes go through the normal entitlement request, never through support. |
| **L2 escalation** | Escalate if the RM should have access by their role and does not — that is an entitlement or policy defect. |
| **Never** | Never work around it, and never ask an engineer to grant access directly. |

### RB-CSRF_REJECTED

`CSRF_REJECTED` · **HTTP 403** · AUTHORIZATION · retry: AFTER_FIX · SECURITY_EVENT · source: 04 section 3

> The caller sees: **Request could not be verified** — Please refresh the page and try again.

| | |
|---|---|
| **What it means** | The caller is known, and is not permitted to do this. |
| **Is it a defect?** | No. Default-deny is the design. |
| **L1 action** | Confirm the RM's role and branch with their supervisor. Access changes go through the normal entitlement request, never through support. |
| **L2 escalation** | Escalate if the RM should have access by their role and does not — that is an entitlement or policy defect. |
| **Never** | Never work around it, and never ask an engineer to grant access directly. |

### RB-CUSTOMER_NOT_IN_BOOK

`CUSTOMER_NOT_IN_BOOK` · **HTTP 422** · AUTHORIZATION · retry: NO · AUDIT · source: 04 section 6

> The caller sees: **Customer not available** — This customer is not in your book.

| | |
|---|---|
| **What it means** | The caller is known, and is not permitted to do this. |
| **Is it a defect?** | No. Default-deny is the design. |
| **L1 action** | Confirm the RM's role and branch with their supervisor. Access changes go through the normal entitlement request, never through support. |
| **L2 escalation** | Escalate if the RM should have access by their role and does not — that is an entitlement or policy defect. |
| **Never** | Never work around it, and never ask an engineer to grant access directly. |

### RB-DEFAULT_DENY

`DEFAULT_DENY` · **HTTP 403** · AUTHORIZATION · retry: NO · AUDIT · source: 04 section 4

> The caller sees: **Not permitted** — You do not have access to this action.

| | |
|---|---|
| **What it means** | The caller is known, and is not permitted to do this. |
| **Is it a defect?** | No. Default-deny is the design. |
| **L1 action** | Confirm the RM's role and branch with their supervisor. Access changes go through the normal entitlement request, never through support. |
| **L2 escalation** | Escalate if the RM should have access by their role and does not — that is an entitlement or policy defect. |
| **Never** | Never work around it, and never ask an engineer to grant access directly. |

### RB-EXPLICIT_DENY

`EXPLICIT_DENY` · **HTTP 403** · AUTHORIZATION · retry: NO · AUDIT · source: 04 section 4

> The caller sees: **Not permitted** — You do not have access to this action.

| | |
|---|---|
| **What it means** | The caller is known, and is not permitted to do this. |
| **Is it a defect?** | No. Default-deny is the design. |
| **L1 action** | Confirm the RM's role and branch with their supervisor. Access changes go through the normal entitlement request, never through support. |
| **L2 escalation** | Escalate if the RM should have access by their role and does not — that is an entitlement or policy defect. |
| **Never** | Never work around it, and never ask an engineer to grant access directly. |

### RB-FORBIDDEN

`FORBIDDEN` · **HTTP 403** · AUTHORIZATION · source: pre-catalogue

> The caller sees: **Not permitted** — You do not have access to this action.

| | |
|---|---|
| **What it means** | The caller is known, and is not permitted to do this. |
| **Is it a defect?** | No. Default-deny is the design. |
| **L1 action** | Confirm the RM's role and branch with their supervisor. Access changes go through the normal entitlement request, never through support. |
| **L2 escalation** | Escalate if the RM should have access by their role and does not — that is an entitlement or policy defect. |
| **Never** | Never work around it, and never ask an engineer to grant access directly. |

### RB-ORIGINATION_RM_ONLY

`ORIGINATION_RM_ONLY` · **HTTP 403** · AUTHORIZATION · retry: NO · COMPLIANCE_EVENT · source: 04 section 4

> The caller sees: **Not permitted** — Only a relationship manager can start this.

| | |
|---|---|
| **What it means** | The caller is known, and is not permitted to do this. |
| **Is it a defect?** | No. Default-deny is the design. |
| **L1 action** | Confirm the RM's role and branch with their supervisor. Access changes go through the normal entitlement request, never through support. |
| **L2 escalation** | Escalate if the RM should have access by their role and does not — that is an entitlement or policy defect. |
| **Never** | Never work around it, and never ask an engineer to grant access directly. |

### RB-OUT_OF_BRANCH_SCOPE

`OUT_OF_BRANCH_SCOPE` · **HTTP 403** · AUTHORIZATION · retry: NO · AUDIT · source: 04 section 4

> The caller sees: **Outside your branch scope** — This record belongs to another branch.

| | |
|---|---|
| **What it means** | The caller is known, and is not permitted to do this. |
| **Is it a defect?** | No. Default-deny is the design. |
| **L1 action** | Confirm the RM's role and branch with their supervisor. Access changes go through the normal entitlement request, never through support. |
| **L2 escalation** | Escalate if the RM should have access by their role and does not — that is an entitlement or policy defect. |
| **Never** | Never work around it, and never ask an engineer to grant access directly. |

### RB-SERVICE_IDENTITY_REJECTED

`SERVICE_IDENTITY_REJECTED` · **HTTP 403** · AUTHORIZATION · retry: NO · SECURITY_EVENT · source: 04 section 4

> The caller sees: **Not permitted** — You do not have access to this action.

| | |
|---|---|
| **What it means** | An internal endpoint was called by something that is not a permitted service. |
| **Is it a defect?** | It should never reach a human at all. |
| **L1 action** | Capture the incident id and escalate to security. There is no RM action. |
| **L2 escalation** | Escalate to security immediately. |
| **Never** | Never treat a leaked internal URL as an entry point. |

### RB-ATTRIBUTION_NOT_CALLER_SUPPLIED

`ATTRIBUTION_NOT_CALLER_SUPPLIED` · **HTTP 422** · COMPLIANCE_GATE · retry: AFTER_FIX · SECURITY_EVENT · source: 04 section 5

> The caller sees: **Request could not be accepted** — This request could not be accepted.

| | |
|---|---|
| **What it means** | The request tried to supply its own attribution, which the platform never accepts from a caller. |
| **Is it a defect?** | It is a client defect — and it should never be seen in production. |
| **L1 action** | Capture the incident id and escalate. There is no RM action. |
| **L2 escalation** | Escalate immediately. Treat any occurrence as a client defect or an intrusion attempt. |
| **Never** | Never ask the RM to change anything. They cannot cause or fix this. |

### RB-CONSENT_REQUIRED

`CONSENT_REQUIRED` · **HTTP 403** · COMPLIANCE_GATE · retry: AFTER_REMEDIATION · COMPLIANCE_EVENT · source: 04 section 5

> The caller sees: **Customer consent required** — The customer must give consent on their own device before this can continue.

| | |
|---|---|
| **What it means** | The proposal cannot proceed because the customer has not given consent on their own device. |
| **Is it a defect?** | No. This is compliance gate C2. |
| **L1 action** | Ask the RM to re-run the consent step. The OTP goes to the customer's device, never the RM's. |
| **L2 escalation** | Escalate if the customer confirms they completed consent and the gate still refuses. |
| **Never** | **Never capture consent on the RM's device, and never accept the RM's word for it.** |

### RB-PAYMENT_DEVICE_ISOLATION

`PAYMENT_DEVICE_ISOLATION` · **HTTP 403** · COMPLIANCE_GATE · retry: NO · COMPLIANCE_EVENT · source: 04 section 5

> The caller sees: **Payment happens on the customer's device** — The payment link goes to the customer. There is no payment path here, by design.

| | |
|---|---|
| **What it means** | Something tried to open a payment on an RM or bank-employee device. |
| **Is it a defect?** | Not a platform defect — but it may be a UI defect, because that control should not have been offered. |
| **L1 action** | Explain that the payment link goes to the customer's own device by design. There is no RM-side payment path. |
| **L2 escalation** | Escalate as a UI defect if the RM was shown a control that led here. |
| **Never** | **Never find a way to pay on the RM's device.** There is no approved path and creating one is a control failure. |

### RB-PAYMENT_NOT_RECONCILED

`PAYMENT_NOT_RECONCILED` · **HTTP 409** · COMPLIANCE_GATE · retry: AFTER_REMEDIATION · COMPLIANCE_EVENT · source: 04 section 5

> The caller sees: **Payment not yet confirmed** — The policy can be issued once the payment is confirmed.

| | |
|---|---|
| **What it means** | The policy cannot be issued because the payment has not been reconciled yet. |
| **Is it a defect?** | No. This is the platform refusing to issue against unconfirmed money. |
| **L1 action** | Tell the RM the payment is still being confirmed and the policy will follow. Do not promise a time. |
| **L2 escalation** | Escalate if reconciliation has not completed well past its normal window — that is an operations task, not a support one. |
| **Never** | **Never override to issue the policy.** A policy issued against an unreconciled payment is a financial-control failure. |

### RB-RM_NOT_CERTIFIED

`RM_NOT_CERTIFIED` · **HTTP 422** · COMPLIANCE_GATE · retry: AFTER_REMEDIATION · COMPLIANCE_EVENT · source: 04 section 6

> The caller sees: **Certification required** — This must be assigned to a certified relationship manager.

| | |
|---|---|
| **What it means** | A regulator-mandated gate refused this. The refusal is itself the evidence a regulator can ask us to produce. |
| **Is it a defect?** | No. This is a hard control working correctly. |
| **L1 action** | Tell the RM which step is missing and let them complete it. The response names the gate. |
| **L2 escalation** | Escalate only if the RM has demonstrably completed the missing step and the gate still refuses. |
| **Never** | **Never override, never bypass, and never ask an engineer to.** A compliance gate cleared by support is an audit finding. |

### RB-SP_CERTIFICATION_REQUIRED

`SP_CERTIFICATION_REQUIRED` · **HTTP 403** · COMPLIANCE_GATE · retry: AFTER_REMEDIATION · COMPLIANCE_EVENT · source: 04 section 4

> The caller sees: **Certification required** — A current certification is needed to sell this product.

| | |
|---|---|
| **What it means** | The RM's certification for this product line is missing or expired. |
| **Is it a defect?** | No. |
| **L1 action** | Tell the RM which certification and line of business, and point them at renewal. Non-selling work stays available to them. |
| **L2 escalation** | Escalate if the certification is current in the source system but still refused. |
| **Never** | **Never let an uncertified RM sell.** Reassign to a certified RM instead. |

### RB-SUITABILITY_REQUIRED

`SUITABILITY_REQUIRED` · **HTTP 403** · COMPLIANCE_GATE · retry: AFTER_REMEDIATION · COMPLIANCE_EVENT · source: 04 section 5

> The caller sees: **Suitability assessment required** — A completed suitability assessment is needed before this quote can be produced.

| | |
|---|---|
| **What it means** | A quote was refused because no valid, unexpired suitability assessment exists for this customer. |
| **Is it a defect?** | No. This is compliance gate C1 working correctly. |
| **L1 action** | Ask the RM to complete or refresh the suitability assessment, then request the quote again. |
| **L2 escalation** | Escalate only if a current assessment exists and the quote is still refused. |
| **Never** | **Never override.** A quote produced without a valid suitability assessment is a regulatory breach, and the refusal is itself the evidence. |

### RB-CONFIGURATION_UNRESOLVABLE

`CONFIGURATION_UNRESOLVABLE` · **HTTP 422** · CONFIG · retry: NO · source: 04 section 8

> The caller sees: **Temporarily unavailable** — Please try again shortly.

| | |
|---|---|
| **What it means** | The platform could not resolve its own configuration, and refused rather than guess. |
| **Is it a defect?** | Yes. A platform defect, not a caller problem. |
| **L1 action** | Capture the incident id and escalate. There is no L1 remedy. |
| **L2 escalation** | Escalate immediately — this is fail-closed behaviour and it blocks the journey. |
| **Never** | Never advise a workaround. There is no safe one. |

### RB-CONFIG_VERSION_REQUIRED

`CONFIG_VERSION_REQUIRED` · **HTTP 422** · CONFIG · retry: NO · source: 04 section 5

> The caller sees: **Something went wrong** — Please try again. Quote the incident id if it persists.

| | |
|---|---|
| **What it means** | The platform could not resolve its own configuration, and refused rather than guess. |
| **Is it a defect?** | Yes. A platform defect, not a caller problem. |
| **L1 action** | Capture the incident id and escalate. There is no L1 remedy. |
| **L2 escalation** | Escalate immediately — this is fail-closed behaviour and it blocks the journey. |
| **Never** | Never advise a workaround. There is no safe one. |

### RB-ASSESSMENT_IMMUTABLE

`ASSESSMENT_IMMUTABLE` · **HTTP 409** · CONFLICT · retry: AFTER_REMEDIATION · AUDIT · source: 04 section 6

> The caller sees: **Assessment cannot be changed** — Create a new assessment instead.

| | |
|---|---|
| **What it means** | The request disagrees with the current state of the record. |
| **Is it a defect?** | No. It is the platform refusing to apply a change that no longer makes sense. |
| **L1 action** | Ask the RM to refresh the screen and read the current state before retrying. |
| **L2 escalation** | Escalate if the state shown to the RM and the state in the response keep disagreeing after a refresh. |
| **Never** | Never retry blindly, and never advise a second attempt on anything touching money. |

### RB-CONFLICT

`CONFLICT` · **HTTP 409** · CONFLICT · source: pre-catalogue

> The caller sees: **Conflict** — This action conflicts with the current state.

| | |
|---|---|
| **What it means** | The request disagrees with the current state of the record. |
| **Is it a defect?** | No. It is the platform refusing to apply a change that no longer makes sense. |
| **L1 action** | Ask the RM to refresh the screen and read the current state before retrying. |
| **L2 escalation** | Escalate if the state shown to the RM and the state in the response keep disagreeing after a refresh. |
| **Never** | Never retry blindly, and never advise a second attempt on anything touching money. |

### RB-IDEMPOTENCY_CONFLICT

`IDEMPOTENCY_CONFLICT` · **HTTP 409** · CONFLICT · retry: NO · AUDIT · source: pre-catalogue (04 section 2 names it IDEMPOTENCY_KEY_CONFLICT — section 13 row 1)

> The caller sees: **Duplicate request** — This request was already submitted with different details.

| | |
|---|---|
| **What it means** | The same idempotency key arrived with different request details. |
| **Is it a defect?** | Usually a client defect, not an RM error. |
| **L1 action** | Ask the RM to start the action again from a fresh screen rather than resubmitting. |
| **L2 escalation** | Escalate with the incident id — a repeating pattern is a client bug. |
| **Never** | Never advise re-sending the same request with edited fields. |

### RB-ILLEGAL_TRANSITION

`ILLEGAL_TRANSITION` · **HTTP 409** · CONFLICT · retry: AFTER_FIX · AUDIT · source: 04 section 6

> The caller sees: **Out of date** — This view is out of date. Please refresh and try again.

| | |
|---|---|
| **What it means** | The request disagrees with the current state of the record. |
| **Is it a defect?** | No. It is the platform refusing to apply a change that no longer makes sense. |
| **L1 action** | Ask the RM to refresh the screen and read the current state before retrying. |
| **L2 escalation** | Escalate if the state shown to the RM and the state in the response keep disagreeing after a refresh. |
| **Never** | Never retry blindly, and never advise a second attempt on anything touching money. |

### RB-PAYMENT_ALREADY_IN_PROGRESS

`PAYMENT_ALREADY_IN_PROGRESS` · **HTTP 409** · CONFLICT · retry: NO · AUDIT · source: 04 section 6

> The caller sees: **Payment already in progress** — A payment attempt is already under way.

| | |
|---|---|
| **What it means** | A payment attempt for this proposal is already under way. |
| **Is it a defect?** | No. It is the double-charge guard. |
| **L1 action** | Show the RM the existing attempt. Ask them to wait for it to complete. |
| **L2 escalation** | Escalate if the existing attempt has been in progress far longer than normal. |
| **Never** | **Never start a second attempt to 'unstick' the first.** |

### RB-PAYMENT_STATE_UNCERTAIN

`PAYMENT_STATE_UNCERTAIN` · **HTTP 409** · CONFLICT · retry: NO · COMPLIANCE_EVENT · source: 04 section 6

> The caller sees: **Payment being confirmed** — Please wait while the payment is confirmed. Do not retry.

| | |
|---|---|
| **What it means** | The payment gateway never confirmed the outcome. The money state is genuinely unknown. |
| **Is it a defect?** | No — and this is the most important row in this document. |
| **L1 action** | Tell the RM to wait. Do not start a new payment. Reconciliation resolves it. |
| **L2 escalation** | Escalate to operations if it has not resolved within the reconciliation window. |
| **Never** | **Never start a second payment attempt, and never guess.** Guessing here is how a customer is charged twice. |

### RB-PREMIUM_MISMATCH

`PREMIUM_MISMATCH` · **HTTP 422** · CONFLICT · retry: NO · COMPLIANCE_EVENT · source: 04 section 6

> The caller sees: **Premium could not be confirmed** — Please start again from a fresh quote. Do not retry.

| | |
|---|---|
| **What it means** | The premium the platform expected and the premium the insurer returned do not agree. |
| **Is it a defect?** | Yes — treat it as a pricing-integrity incident, not a transient error. |
| **L1 action** | Stop. Do not advise a retry. Capture the incident id and escalate immediately. |
| **L2 escalation** | Escalate immediately, every occurrence. |
| **Never** | **Never retry and never proceed on either figure.** A mispriced policy is a customer and regulatory problem at once. |

### RB-PROPOSAL_NOT_PAYABLE

`PROPOSAL_NOT_PAYABLE` · **HTTP 409** · CONFLICT · retry: NO · source: pre-catalogue

> The caller sees: **Payment not available yet** — This proposal cannot be paid for in its current state.

| | |
|---|---|
| **What it means** | The request disagrees with the current state of the record. |
| **Is it a defect?** | No. It is the platform refusing to apply a change that no longer makes sense. |
| **L1 action** | Ask the RM to refresh the screen and read the current state before retrying. |
| **L2 escalation** | Escalate if the state shown to the RM and the state in the response keep disagreeing after a refresh. |
| **Never** | Never retry blindly, and never advise a second attempt on anything touching money. |

### RB-QUOTE_EXPIRED

`QUOTE_EXPIRED` · **HTTP 409** · CONFLICT · retry: AFTER_REMEDIATION · source: 04 section 6

> The caller sees: **Quote expired** — This quote is no longer valid. Please request a new one.

| | |
|---|---|
| **What it means** | The quote is past its validity window, or the quote job it referenced is gone. |
| **Is it a defect?** | No. |
| **L1 action** | Ask the RM to produce a fresh quote. Do not re-use the old offer. |
| **L2 escalation** | Escalate if quotes expire far sooner than the configured window. |
| **Never** | **Never re-quote automatically.** The suitability gate must be re-checked, so the RM starts the quote again deliberately. |

### RB-REQUEST_IN_PROGRESS

`REQUEST_IN_PROGRESS` · **HTTP 409** · CONFLICT · retry: YES · source: 04 section 2

> The caller sees: **Already in progress** — This request is still being processed.

| | |
|---|---|
| **What it means** | The request disagrees with the current state of the record. |
| **Is it a defect?** | No. It is the platform refusing to apply a change that no longer makes sense. |
| **L1 action** | Ask the RM to refresh the screen and read the current state before retrying. |
| **L2 escalation** | Escalate if the state shown to the RM and the state in the response keep disagreeing after a refresh. |
| **Never** | Never retry blindly, and never advise a second attempt on anything touching money. |

### RB-WITHDRAWAL_NOT_PERMITTED

`WITHDRAWAL_NOT_PERMITTED` · **HTTP 409** · CONFLICT · retry: NO · AUDIT · source: 04 section 6

> The caller sees: **Cannot be withdrawn** — This cannot be withdrawn while payment is in progress.

| | |
|---|---|
| **What it means** | The request disagrees with the current state of the record. |
| **Is it a defect?** | No. It is the platform refusing to apply a change that no longer makes sense. |
| **L1 action** | Ask the RM to refresh the screen and read the current state before retrying. |
| **L2 escalation** | Escalate if the state shown to the RM and the state in the response keep disagreeing after a refresh. |
| **Never** | Never retry blindly, and never advise a second attempt on anything touching money. |

### RB-INTERNAL_ERROR

`INTERNAL_ERROR` · **HTTP 500** · INTERNAL · retry: NO · source: pre-catalogue

> The caller sees: **Something went wrong** — Please try again. Quote the incident id if it persists.

| | |
|---|---|
| **What it means** | A defect in our own code. Not the caller's fault and not a dependency's. |
| **Is it a defect?** | Yes, always. Every occurrence is a bug. |
| **L1 action** | Capture the incident id and escalate. There is no L1 remedy. |
| **L2 escalation** | Escalate immediately with the incident id. |
| **Never** | Never tell the caller it was their input. |

### RB-RESOURCE_NOT_FOUND

`RESOURCE_NOT_FOUND` · **HTTP 404** · NOT_FOUND · source: pre-catalogue

> The caller sees: **Not found** — The requested item could not be found.

| | |
|---|---|
| **What it means** | The referenced record does not exist, or is not visible to this caller. |
| **Is it a defect?** | Usually not. Most often the id is stale or belongs to another branch. |
| **L1 action** | Confirm the id the RM used and that the record belongs to their book. Ask them to re-open the record from the list rather than a saved link. |
| **L2 escalation** | Escalate if the record demonstrably exists and is in scope for that RM. |
| **Never** | Never infer that missing means deleted. A visibility rule and an absent row look identical from here, deliberately. |

### RB-RATE_LIMITED

`RATE_LIMITED` · **HTTP 429** · RATE_LIMIT · retry: YES · source: 04 section 2

> The caller sees: **Too many requests** — Please wait a moment and try again.

| | |
|---|---|
| **What it means** | The caller sent more requests than the route allows. |
| **Is it a defect?** | No. |
| **L1 action** | Ask the RM to wait and retry once. Repeated rapid retries extend the block. |
| **L2 escalation** | Escalate if a normal working pattern triggers it — that is a limit set too low. |
| **Never** | Never advise retrying in a tight loop. |

### RB-IDENTITY_PROVIDER_UNAVAILABLE

`IDENTITY_PROVIDER_UNAVAILABLE` · **HTTP 503** · UPSTREAM · retry: YES · AUDIT · source: 04 section 3

> The caller sees: **Sign-in temporarily unavailable** — Please try again shortly.

| | |
|---|---|
| **What it means** | A system we depend on failed or did not answer. Not the caller's mistake. |
| **Is it a defect?** | Not ours, usually — but it is our incident. |
| **L1 action** | Check whether the failure is widespread or affects one RM. Widespread means an incident, not a support ticket. |
| **L2 escalation** | Escalate on a sustained rate. `originService` in the logs names which dependency. |
| **Never** | Never advise repeated manual retries on a submit or a payment — that is how duplicates are created. |

### RB-PROPOSAL_REJECTED

`PROPOSAL_REJECTED` · **HTTP 422** · UPSTREAM · retry: NO · AUDIT · source: pre-catalogue

> The caller sees: **Proposal declined** — The proposal was not accepted.

| | |
|---|---|
| **What it means** | A system we depend on failed or did not answer. Not the caller's mistake. |
| **Is it a defect?** | Not ours, usually — but it is our incident. |
| **L1 action** | Check whether the failure is widespread or affects one RM. Widespread means an incident, not a support ticket. |
| **L2 escalation** | Escalate on a sustained rate. `originService` in the logs names which dependency. |
| **Never** | Never advise repeated manual retries on a submit or a payment — that is how duplicates are created. |

### RB-QUOTE_TIMEOUT

`QUOTE_TIMEOUT` · **HTTP 504** · UPSTREAM · retry: YES · source: pre-catalogue (reports 04 section 7 TIMED_OUT)

> The caller sees: **Quote took too long** — No insurer answered in time. You can request a new quote.

| | |
|---|---|
| **What it means** | A system we depend on failed or did not answer. Not the caller's mistake. |
| **Is it a defect?** | Not ours, usually — but it is our incident. |
| **L1 action** | Check whether the failure is widespread or affects one RM. Widespread means an incident, not a support ticket. |
| **L2 escalation** | Escalate on a sustained rate. `originService` in the logs names which dependency. |
| **Never** | Never advise repeated manual retries on a submit or a payment — that is how duplicates are created. |

### RB-UPSTREAM_AUTH_FAILURE

`UPSTREAM_AUTH_FAILURE` · **HTTP 502** · UPSTREAM · retry: NO · SECURITY_EVENT · source: pre-catalogue

> The caller sees: **Service temporarily unavailable** — Please try again shortly.

| | |
|---|---|
| **What it means** | A system we depend on failed or did not answer. Not the caller's mistake. |
| **Is it a defect?** | Not ours, usually — but it is our incident. |
| **L1 action** | Check whether the failure is widespread or affects one RM. Widespread means an incident, not a support ticket. |
| **L2 escalation** | Escalate on a sustained rate. `originService` in the logs names which dependency. |
| **Never** | Never advise repeated manual retries on a submit or a payment — that is how duplicates are created. |

### RB-UPSTREAM_BAD_RESPONSE

`UPSTREAM_BAD_RESPONSE` · **HTTP 502** · UPSTREAM · retry: YES · source: pre-catalogue · FUNC-007 AC

> The caller sees: **Service temporarily unavailable** — Please try again shortly.

| | |
|---|---|
| **What it means** | A system we depend on failed or did not answer. Not the caller's mistake. |
| **Is it a defect?** | Not ours, usually — but it is our incident. |
| **L1 action** | Check whether the failure is widespread or affects one RM. Widespread means an incident, not a support ticket. |
| **L2 escalation** | Escalate on a sustained rate. `originService` in the logs names which dependency. |
| **Never** | Never advise repeated manual retries on a submit or a payment — that is how duplicates are created. |

### RB-UPSTREAM_BUSINESS_ERROR

`UPSTREAM_BUSINESS_ERROR` · **HTTP 422** · UPSTREAM · retry: NO · source: pre-catalogue

> The caller sees: **Request declined** — The request could not be completed at this time.

| | |
|---|---|
| **What it means** | A system we depend on failed or did not answer. Not the caller's mistake. |
| **Is it a defect?** | Not ours, usually — but it is our incident. |
| **L1 action** | Check whether the failure is widespread or affects one RM. Widespread means an incident, not a support ticket. |
| **L2 escalation** | Escalate on a sustained rate. `originService` in the logs names which dependency. |
| **Never** | Never advise repeated manual retries on a submit or a payment — that is how duplicates are created. |

### RB-UPSTREAM_TIMEOUT

`UPSTREAM_TIMEOUT` · **HTTP 504** · UPSTREAM · retry: YES · source: pre-catalogue

> The caller sees: **Service temporarily unavailable** — Please try again shortly.

| | |
|---|---|
| **What it means** | A system we depend on failed or did not answer. Not the caller's mistake. |
| **Is it a defect?** | Not ours, usually — but it is our incident. |
| **L1 action** | Check whether the failure is widespread or affects one RM. Widespread means an incident, not a support ticket. |
| **L2 escalation** | Escalate on a sustained rate. `originService` in the logs names which dependency. |
| **Never** | Never advise repeated manual retries on a submit or a payment — that is how duplicates are created. |

### RB-UPSTREAM_UNAVAILABLE

`UPSTREAM_UNAVAILABLE` · **HTTP 503** · UPSTREAM · retry: YES · source: pre-catalogue

> The caller sees: **Service temporarily unavailable** — Please try again shortly.

| | |
|---|---|
| **What it means** | A system we depend on failed or did not answer. Not the caller's mistake. |
| **Is it a defect?** | Not ours, usually — but it is our incident. |
| **L1 action** | Check whether the failure is widespread or affects one RM. Widespread means an incident, not a support ticket. |
| **L2 escalation** | Escalate on a sustained rate. `originService` in the logs names which dependency. |
| **Never** | Never advise repeated manual retries on a submit or a payment — that is how duplicates are created. |

### RB-AGENT_ATTRIBUTION_MISSING

`AGENT_ATTRIBUTION_MISSING` · **HTTP 422** · VALIDATION · source: pre-catalogue

> The caller sees: **Attribution missing** — The request is missing required attribution.

| | |
|---|---|
| **What it means** | The request was rejected before any business rule ran. The caller sent something the API does not accept. |
| **Is it a defect?** | No. This is the API refusing malformed input, which is what it is for. |
| **L1 action** | Read `errors[]` in the response — it names the field. Ask the RM to correct that field and retry. |
| **L2 escalation** | Only if `errors[]` names a field the RM cannot see or cannot change on their screen. That is a UI defect, not a data-entry problem. |
| **Never** | Never advise retrying the identical request. It will fail identically. |

### RB-INVALID_OFFER_REFERENCE

`INVALID_OFFER_REFERENCE` · **HTTP 422** · VALIDATION · source: 04 section 6

> The caller sees: **Offer no longer valid** — Please select again from a current quote.

| | |
|---|---|
| **What it means** | The request was rejected before any business rule ran. The caller sent something the API does not accept. |
| **Is it a defect?** | No. This is the API refusing malformed input, which is what it is for. |
| **L1 action** | Read `errors[]` in the response — it names the field. Ask the RM to correct that field and retry. |
| **L2 escalation** | Only if `errors[]` names a field the RM cannot see or cannot change on their screen. That is a UI defect, not a data-entry problem. |
| **Never** | Never advise retrying the identical request. It will fail identically. |

### RB-INVALID_REQUEST

`INVALID_REQUEST` · **HTTP 422** · VALIDATION · source: pre-catalogue

> The caller sees: **Invalid request** — The request could not be read.

| | |
|---|---|
| **What it means** | The request was rejected before any business rule ran. The caller sent something the API does not accept. |
| **Is it a defect?** | No. This is the API refusing malformed input, which is what it is for. |
| **L1 action** | Read `errors[]` in the response — it names the field. Ask the RM to correct that field and retry. |
| **L2 escalation** | Only if `errors[]` names a field the RM cannot see or cannot change on their screen. That is a UI defect, not a data-entry problem. |
| **Never** | Never advise retrying the identical request. It will fail identically. |

### RB-LOB_REQUIRED

`LOB_REQUIRED` · **HTTP 422** · VALIDATION · source: 04 section 2

> The caller sees: **Product line required** — A line of business must be selected.

| | |
|---|---|
| **What it means** | The request was rejected before any business rule ran. The caller sent something the API does not accept. |
| **Is it a defect?** | No. This is the API refusing malformed input, which is what it is for. |
| **L1 action** | Read `errors[]` in the response — it names the field. Ask the RM to correct that field and retry. |
| **L2 escalation** | Only if `errors[]` names a field the RM cannot see or cannot change on their screen. That is a UI defect, not a data-entry problem. |
| **Never** | Never advise retrying the identical request. It will fail identically. |

### RB-MISSING_IDEMPOTENCY_KEY

`MISSING_IDEMPOTENCY_KEY` · **HTTP 400** · VALIDATION · source: 04 section 2

> The caller sees: **Missing idempotency key** — This request must carry an idempotency key.

| | |
|---|---|
| **What it means** | The request was rejected before any business rule ran. The caller sent something the API does not accept. |
| **Is it a defect?** | No. This is the API refusing malformed input, which is what it is for. |
| **L1 action** | Read `errors[]` in the response — it names the field. Ask the RM to correct that field and retry. |
| **L2 escalation** | Only if `errors[]` names a field the RM cannot see or cannot change on their screen. That is a UI defect, not a data-entry problem. |
| **Never** | Never advise retrying the identical request. It will fail identically. |

### RB-MISSING_REQUIRED_FIELD

`MISSING_REQUIRED_FIELD` · **HTTP 422** · VALIDATION · source: pre-catalogue

> The caller sees: **Missing required detail** — A required detail was not supplied.

| | |
|---|---|
| **What it means** | The request was rejected before any business rule ran. The caller sent something the API does not accept. |
| **Is it a defect?** | No. This is the API refusing malformed input, which is what it is for. |
| **L1 action** | Read `errors[]` in the response — it names the field. Ask the RM to correct that field and retry. |
| **L2 escalation** | Only if `errors[]` names a field the RM cannot see or cannot change on their screen. That is a UI defect, not a data-entry problem. |
| **Never** | Never advise retrying the identical request. It will fail identically. |

### RB-OPPORTUNITY_REQUIRED

`OPPORTUNITY_REQUIRED` · **HTTP 422** · VALIDATION · source: 04 section 6

> The caller sees: **Lead required** — This action needs an existing lead.

| | |
|---|---|
| **What it means** | The request was rejected before any business rule ran. The caller sent something the API does not accept. |
| **Is it a defect?** | No. This is the API refusing malformed input, which is what it is for. |
| **L1 action** | Read `errors[]` in the response — it names the field. Ask the RM to correct that field and retry. |
| **L2 escalation** | Only if `errors[]` names a field the RM cannot see or cannot change on their screen. That is a UI defect, not a data-entry problem. |
| **Never** | Never advise retrying the identical request. It will fail identically. |

### RB-PAYLOAD_TOO_LARGE

`PAYLOAD_TOO_LARGE` · **HTTP 413** · VALIDATION · source: 04 section 2

> The caller sees: **Request too large** — The request is larger than allowed.

| | |
|---|---|
| **What it means** | The request was rejected before any business rule ran. The caller sent something the API does not accept. |
| **Is it a defect?** | No. This is the API refusing malformed input, which is what it is for. |
| **L1 action** | Read `errors[]` in the response — it names the field. Ask the RM to correct that field and retry. |
| **L2 escalation** | Only if `errors[]` names a field the RM cannot see or cannot change on their screen. That is a UI defect, not a data-entry problem. |
| **Never** | Never advise retrying the identical request. It will fail identically. |

### RB-SCHEMA_INVALID

`SCHEMA_INVALID` · **HTTP 400** · VALIDATION · source: 04 section 2

> The caller sees: **Invalid request** — The request does not match the expected format.

| | |
|---|---|
| **What it means** | The request was rejected before any business rule ran. The caller sent something the API does not accept. |
| **Is it a defect?** | No. This is the API refusing malformed input, which is what it is for. |
| **L1 action** | Read `errors[]` in the response — it names the field. Ask the RM to correct that field and retry. |
| **L2 escalation** | Only if `errors[]` names a field the RM cannot see or cannot change on their screen. That is a UI defect, not a data-entry problem. |
| **Never** | Never advise retrying the identical request. It will fail identically. |

### RB-UNSUPPORTED_LOB

`UNSUPPORTED_LOB` · **HTTP 422** · VALIDATION · source: pre-catalogue

> The caller sees: **Product line not supported** — This line of business is not available.

| | |
|---|---|
| **What it means** | The request was rejected before any business rule ran. The caller sent something the API does not accept. |
| **Is it a defect?** | No. This is the API refusing malformed input, which is what it is for. |
| **L1 action** | Read `errors[]` in the response — it names the field. Ask the RM to correct that field and retry. |
| **L2 escalation** | Only if `errors[]` names a field the RM cannot see or cannot change on their screen. That is a UI defect, not a data-entry problem. |
| **Never** | Never advise retrying the identical request. It will fail identically. |

### RB-VALIDATION_ERROR

`VALIDATION_ERROR` · **HTTP 422** · VALIDATION · source: pre-catalogue

> The caller sees: **Validation failed** — Some of the details supplied are not valid.

| | |
|---|---|
| **What it means** | The request was rejected before any business rule ran. The caller sent something the API does not accept. |
| **Is it a defect?** | No. This is the API refusing malformed input, which is what it is for. |
| **L1 action** | Read `errors[]` in the response — it names the field. Ask the RM to correct that field and retry. |
| **L2 escalation** | Only if `errors[]` names a field the RM cannot see or cannot change on their screen. That is a UI defect, not a data-entry problem. |
| **Never** | Never advise retrying the identical request. It will fail identically. |

---

## 4. Codes with no page

There are none, and that is enforced: `ErrorRunbookParityTest` fails the build if a code is
registered without a page here, or a page exists for a code that no longer does. A support
runbook with gaps is worse than none, because the gap is only discovered mid-incident.

The degraded states in
[`04 §7`](./04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md#7-degraded-states--the-ones-with-no-error-code)
deliberately have no error code and therefore no page here. They are journey states, not
refusals, and they are resolved by the operations procedures that file names.

