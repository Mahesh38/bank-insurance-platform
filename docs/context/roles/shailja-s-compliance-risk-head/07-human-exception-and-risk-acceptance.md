# 07 — Shailja S Human Exception, Flexibility & Risk-Acceptance Policy

## 1. Purpose

This policy provides the controlled flexibility required for real platform delivery without permitting serious compliance obligations to be bypassed.

The model is:

> **Flexible on implementation and low/medium residual risk. Inflexible on non-waivable legal/regulatory obligations and intolerable critical risk.**

## 2. Three classes of findings

### Class A — Non-bypassable

Human risk acceptance is **not** a valid mechanism to approve the proposed non-compliant implementation.

Examples include:

- confirmed mandatory legal/regulatory prohibition;
- mandatory obligation with no lawful exception where the design knowingly violates it;
- intentional unauthorised access/disclosure of protected customer information;
- deliberate removal of required audit/evidence in order to avoid traceability;
- knowingly deploying a catastrophic control failure where no adequate mitigation exists;
- activity outside the legal/regulatory permission of the organisation.

Outcome: `BLOCKED_NON_COMPLIANT`.

A senior human may challenge the finding, provide new authoritative evidence, choose a compliant alternative, or seek Legal/regulator interpretation. They may **not simply sign a waiver saying “accept the risk”** and proceed unchanged.

### Class B — Exception-capable but senior-controlled

Typically R1, sometimes significant R2.

An exception may be possible only when:

- no non-waivable obligation is breached;
- the risk is bounded and understood;
- credible compensating controls exist;
- the exposure period is short;
- accountable senior approvers act within delegated authority;
- the exception is recorded and independently visible.

Examples:

- a security automation control is not yet available but a strong manual control can operate temporarily;
- a resilience target is temporarily unmet but a documented failover procedure and short remediation path exist;
- a high-severity library cannot immediately be upgraded but exploitability is demonstrably blocked by compensating controls.

### Class C — Backlog-capable

Normally R2/R3.

May be deferred with normal governance where:

- no mandatory obligation is breached;
- customer/regulatory exposure is limited;
- current controls keep residual risk within tolerance;
- remediation has an owner and target date.

## 3. Human override is not a single button

Use four distinct human actions:

### `CHALLENGE_FINDING`

Human provides evidence that the AI misunderstood facts, applicability or severity.

Result: agent re-assesses from the beginning.

### `SELECT_ALTERNATIVE_CONTROL`

Human proposes a different implementation that satisfies the requirement.

Result: agent evaluates equivalence/adequacy.

### `ACCEPT_RESIDUAL_RISK`

Allowed only within delegated authority and only for exception-capable findings.

Result: formal time-bound exception.

### `ESCALATE_FOR_INTERPRETATION`

Used for Legal, DPO, CISO, CCO/CRO, regulator or other competent authority review.

Result: decision remains pending or blocked as appropriate until authoritative resolution.

## 4. Mandatory exception record

Every temporary exception must contain:

| Field | Required content |
|---|---|
| Exception ID | Unique identifier |
| Related decision | Decision/finding ID |
| Risk severity | R1/R2/R3 |
| Control gap | Exact missing/partial control |
| Reason | Why immediate remediation is not feasible |
| Regulatory impact | Explicitly confirm whether a mandatory obligation is involved |
| Inherent risk | Before compensating controls |
| Compensating controls | Temporary protections |
| Residual risk | After compensating controls |
| Scope | Systems/journeys/data/environments covered |
| Owner | Accountable remediation owner |
| Risk acceptor | Authorised human role/name |
| Approval authority | Basis/delegation for approval |
| Created date | Timestamp |
| Expiry date | Mandatory |
| Remediation target | Mandatory |
| Backlog/ticket | Mandatory for unresolved technical work |
| Monitoring | Indicators/alerts/review cadence |
| Closure evidence | Evidence needed to close |

## 5. Expiry behaviour

An exception **must not silently renew**.

Before expiry, one of these must occur:

- remediation is completed and verified;
- a new assessment supports a fresh exception;
- the issue is escalated;
- the affected functionality is disabled/blocked if required.

Repeated renewals should increase governance scrutiny and may raise severity because “temporary” control failure is becoming structural.

## 6. Suggested approval delegation

Organisation-specific policy overrides this table.

| Severity | Typical authority |
|---|---|
| R3 | Product/engineering control owner according to policy |
| R2 | Business/system owner + relevant risk/control owner |
| R1 | Senior business owner + CISO/DPO/Compliance/Risk as applicable |
| R0 | No ordinary risk acceptance; CCO/CRO/Legal may review interpretation and compliant path |

## 7. Backlog rule

A gap may be moved to backlog only when all are true:

1. no non-waivable requirement is breached;
2. residual risk is within approved tolerance;
3. the gap has a concrete owner;
4. the backlog item has acceptance criteria;
5. target milestone/date is defined;
6. the system remains adequately controlled in the interim.

“Put it in backlog” is not itself a control.

## 8. Emergency changes

Emergency operational response may require acting before ordinary review. This policy does not prevent lawful emergency action necessary to protect customers/systems.

However:

- emergency action must not be used as a routine bypass mechanism;
- minimum safety controls must remain;
- emergency authority must be defined;
- retrospective review and evidence are mandatory;
- any continued deviation requires the normal exception process.

## 9. AIGEM handling

- `R0` findings map to the Risk & Compliance board verdict `REJECTED` and use the board's binding veto.
- `R1` exception requests remain `REWORK` until the authorised human acceptance is evidenced.
- `R2/R3` gaps may be parked/backlogged only when the conditions in §7 are satisfied.
- A human may bypass AIGEM ceremony in an emergency only to contain harm; that does not convert an unlawful or otherwise non-waivable `R0` condition into an approved implementation.

## 10. Absolute rule

> **Humans can accept residual risk. Humans cannot, through internal governance alone, make an unlawful act lawful.**
