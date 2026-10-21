# Scenario 06 — Start Onboarding, exception check and lead assignment

[← API reference home](../README.md) · Prev: [05](./05-lead-creation-and-dedupe.md) · Next: [07](./07-journey-start-and-suitability.md)

**Status:** IMPLEMENTED — one BFF call (`POST /api/v1/leads/{leadId}/assignment`) runs Start
Onboarding if needed and then assigns. The exception engine is a deterministic stub and SP
certification a fixture.

**Why it matters:** the regulated sale may only proceed under a **certified Specified Person**. The
first completed assignment fixes the `accountableSpId` forever (`INV-ACT-03`) — that is who is
accountable in a mis-selling review.

## 1. Sequence

```mermaid
sequenceDiagram
    autonumber
    participant App as NIP-APP
    participant BFF as BFF (LeadFacade)
    participant LS as lead-service
    participant EX as Exception engine (stub today; AUBIMA target)
    participant SP as SP certification (fixture today; IRDAI / bank register target)
    App->>BFF: GET /api/v1/branches · /branches/{b}/specified-persons · /verticals/{v}/relationship-managers
    BFF-->>App: assignment pickers (fixture directory)
    App->>BFF: POST /api/v1/leads/{leadId}/assignment {assignedRmId: "SP-1001", assignedSpId?}
    BFF->>LS: GET /internal/v1/leads/{leadId}
    alt exceptionOutcome = NOT_EVALUATED
        BFF->>LS: POST /internal/v1/leads/{leadId}/onboarding
        LS->>EX: evaluate(customerId, productClass)
        EX-->>LS: PASS | APPROVAL_REQUIRED | BLOCK
        alt BLOCK
            LS-->>BFF: 409 ILLEGAL_TRANSITION (outcome saved, progression locked)
            BFF-->>App: 409
        else APPROVAL_REQUIRED
            LS-->>BFF: 200 lead (exceptionHold = true)
        end
    end
    alt on hold
        BFF-->>App: 200 lead unchanged (exceptionRequired = true) → app shows "Raise exception"
    else clear
        BFF->>LS: POST /internal/v1/leads/{leadId}/assignments
        LS->>LS: not BI-generated · onboarding done · no hold · assignee present
        LS->>SP: holdsValidCertification(SP-1001)
        alt not certified
            LS-->>BFF: 403 SP_CERTIFICATION_REQUIRED
        else certified
            LS->>LS: assignedRmId, assignedSpId; accountableSpId (first time only); state ASSIGNED
            LS-->>BFF: 200 lead
        end
        BFF-->>App: 200 / 403
    end
```

## 2. Exception path (held lead)

Held leads wait for the AUBIMA exception workflow: the raiser submits an exception with remarks,
an approver approves or rejects, AUBIMA calls Lead
`POST /internal/v1/leads/{leadId}/exception-hold {action: RELEASE|REJECT, ruleIds}` (CONTRACTED).
After release the RM repeats the assignment call. Lead stores only the hold state; it does not
implement approval hierarchies.

## 3. Errors

| HTTP | Code | When | App |
|---|---|---|---|
| 409 | `ILLEGAL_TRANSITION` | Onboarding blocked; assign before onboarding; reassign after BI | Show reason; no retry |
| 409 | `CONFLICT` | Exception hold active | Show "exception pending" |
| 403 | `SP_CERTIFICATION_REQUIRED` | Assignee not a certified SP for Life | Pick another assignee |
| 422 | `MISSING_REQUIRED_FIELD` / `VALIDATION_ERROR` | No `assignedRmId` | Inline |
| 404 | `RESOURCE_NOT_FOUND` | Lead not in the actor's book | Back to pipeline |

## 4. Other lead actions in this phase

| Action | API | Status |
|---|---|---|
| Close a pre-BI lead (reason, remarks) | `POST /internal/v1/leads/{id}/close` → `DISQUALIFIED` (no reopen) | IMPLEMENTED internal; BFF route CONTRACTED |
| Reassign (pre-BI) | same assignment call; history appended; `accountableSpId` unchanged | IMPLEMENTED |
| Activity status / disposition | `POST /internal/v1/leads/{id}/activity-status` | CONTRACTED |
| Optional meeting (online / in person) | part of assignment body (target) → `lead_meeting` | CONTRACTED |
| Save & Close | no API — back to pipeline (diary lead) | — |

## 5. Next

Only an AU employee **SP-certified** Bank RM (or Insurance RM/FLS under assist rules) with the
exception cleared may proceed to suitability (`INV-ACT-01`, `INV-LED-10`, `D-019`).

## 6. Pages

[BFF §3.7](../services/workforce-access-bff.md#37-post-apiv1leadsleadidassignment) · [lead-service §3.4–§3.5](../services/lead-service.md#34-post-internalv1leadsleadidonboarding--start-onboarding) · [BFF to Lead](../interactions/bff-to-lead.md) · [Lead sequences §3–§5](../../ws3-platform/11-lead-module-sequences.md)
