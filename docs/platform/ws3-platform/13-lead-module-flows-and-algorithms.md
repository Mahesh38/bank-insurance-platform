# 13 — Lead flows and algorithms (R0)

**Status:** `AI-DRAFTED` · T3 · human Board 1 / Product outstanding  
**Origin:** `SUG-20260930-lmd` · `EPIC-005` · `DOC-023` · `PLAN-007`  
**Rule fidelity:** BRD rule IDs cited. Conflicts → OPEN ids. Do not invent Product decisions.

---

## 1. ALG-CREATE — lead creation (`D-018` / `ADR-021`)

```
function createLead(cmd, principal):
  require isAllowedWorkforceCreator(principal)      # INV-LED-04 — Bank SP, Non-SP, Insurance RM
  # IPR create: design allowed; runtime requires OPEN-COMP-LEAD-IPR-CREATE closed
  if principal.actorType == INSURER_PARTNER_REP:
    require featureFlag.iprLeadCreateEnabled        # Board 6 gate
  require cmd.assignedRmId present                  # INV-LED-10
  assignee = loadPrincipal(cmd.assignedRmId)
  require assignee.actorType == BANK_RM
  require assignee.hasSpCert(cmd.lob)               # INV-LED-03/10
  require customerInEtbBook(assignee, cmd.customerId)  # INV-LED-05 — book of accountable SP
  require cmd.lob == LIFE
  require cmd.productClass in {TERM, SAVINGS, ULIP}
  require cmd.productClass coveredByCert(assignee)

  dup = ALG_DEDUPE(principal.id, cmd.customerId, cmd.productClass)
  if dup.blocked:
    return Conflict(dup.existingLead)

  leadId = newUlid()
  lead = Lead(
    leadId,
    customerId = cmd.customerId,
    lob = LIFE,
    productClass = cmd.productClass,               # BR-LEAD-006 immutable
    createdBy = principal.id,                       # BR-OWN-002 — actual originator
    leadGenerator = resolveGenerator(principal, cmd),  # BRD §15 / BR-OWN
    fulfiller = resolveFulfiller(principal, cmd),
    assignedRmId = assignee.id,
    assignedSpId = cmd.assignedSpId,                # when distinct per BRD
    branchId = cmd.branchId,
    accountableSpId = assignee.id,                  # INV-ACT-03 — certified SP at create
    state = ASSIGNED,                               # create always assigns SP
    activityStatus = null,
    biGenerated = false,
    reportingClass = DIARY,                         # BRD §6.4
    insurerId = null,                               # BR-LEAD-005
    planId = null
  )
  journeyId = Journey.startFromLead(leadId)         # AC-8
  lead.journeyId = journeyId
  persist(lead)
  emit LeadCreated(...)
  audit(LEAD_CREATED without PII)
  return Created(lead)
```

**BRD:** `BR-LEAD-001`…`006`, `BR-OWN-002`, §8 role flows. **Product:** `D-018`.

---

## 2. ALG-DEDUPE — unfinished same principal + customer + product

Key (BRD §11.1): `(userId, customerId, productType)` where `userId` = creating principal.

```
function ALG_DEDUPE(userId, customerId, productClass):
  candidates = findLeads(
    createdOrOwnedBy = userId,
    customerId = customerId,
    productClass = productClass,
    state not in TERMINAL_CLOSED_SET
  )
  unfinished = [l for l in candidates if l.biGenerated == false]
  if unfinished is not empty:
    return Blocked(existingLead = newest(unfinished))   # VAL-012
  # biGenerated true → allow new lead (BR-DEDUPE table row 3)
  return Allow
```

| Situation | Outcome | Source |
|---|---|---|
| Same user + customer + TERM + BI=No | Block + show existing | BRD §11 |
| Same user + customer + TERM + BI=Yes | Allow new | BRD §11 |
| Same user + customer + different productClass | Allow | BR-DEDUPE-001 |
| Another user same customer + product | Allow create | BRD §11 |
| Closed lead same key | Allow (still run unfinished check) | BRD §11 |

Cross-RM **visibility** of the other user’s leadId to this caller: `OPEN-LEAD-XRM` (absent on search). Dedupe does not require seeing it.

Force override: **not supported** (`OPEN-LEAD-DUP`).

---

## 3. ALG-ASSIGN — assignment / reassignment

```
function assign(leadId, target, actor):
  lead = load(leadId)
  require not lead.isTerminal()
  if lead.biGenerated:
    reject REASSIGN_AFTER_BI                    # BR-REASSIGN-001, VAL-016
  require PDP.allows(actor, assign, lead)
  require targetHasValidSpCert(target, lead.lob)  # INV-LED-03 when target is RM/SP fulfiller
  history.append(old → new, actor, ts)
  applyOwnership(lead, target)                  # updates generator/fulfiller per Product
  # OPEN-D1: whether SLA resets / who gets conversion credit — provisional:
  # reporting credit follows current leadGenerator (BR-OWN-003)
  emit AssignmentChanged
  audit(ASSIGNMENT)
```

R0 create path typically self-assigns the creating RM (NEW may skip ASSIGNED or auto-transition NEW→ASSIGNED).

---

## 4. ALG-BI — first Benefit Illustration

Handler for `POST /internal/v1/leads/{leadId}/bi-generated` (and the equivalent durable event).

```
function markBiGenerated(leadId, biReference, occurredAt):
  lead = load(leadId)
  require not lead.isTerminal()
  if lead.biGenerated:
    linkBi(leadId, biReference)                 # BR-BI-003/005 — no new lead
    return IdempotentOk
  # Only successful BI counts (BR-BI-001/002)
  lead.biGenerated = true
  lead.reportingClass = ELIGIBLE                # BR-BI-004 Diary→Eligible
  transition(lead, QUALIFIED)                   # domain; OPEN-LEAD-STAGE vs BRD "Quote Generated"
  emit LeadQualified
  audit(BI_GENERATED)
```

Reaching quote screen without BI response: **no** change (`BR-BI-002`).

---

## 5. ALG-ACTIVITY — disposition before BI

```
function setActivityStatus(leadId, statusCode, actor):
  lead = load(leadId)
  require lead.biGenerated == false
  require statusCode in ConfigurableStatusMaster
  lead.activityStatus = statusCode
  if statusCode == CUSTOMER_NOT_INTERESTED:
    return close(leadId, reason=NOT_INTERESTED, actor)
  audit(STATUS_CHANGE)
```

Master is configuration (`#19`), not code deploy (`BRD §14.2`).

---

## 6. ALG-CLOSE

```
function close(leadId, reasonCode, remarks, actor):
  require reasonCode present                    # VAL-017
  if reasonCode == OTHER:
    require remarks non-empty                   # VAL-018
  require length(remarks) <= 250                # VAL-019
  lead = load(leadId)
  require not lead.isTerminal()
  transition(lead, DISQUALIFIED)                # or CLOSED label in projection
  lead.closedReason = reasonCode
  lead.remarks = remarks
  emit LeadClosed
  # BR-CLOSE-001: cannot reopen
```

---

## 7. ALG-CONVERT / ALG-ARCHIVE

```
function onJourneySold(leadId, journeyId, paymentId, policyId):
  lead = load(leadId)
  if lead.state == CONVERTED and lead.convertingJourneyId == journeyId:
    return IdempotentOk                         # INV-LED-02
  if lead.state == CONVERTED and journeyId differs:
    alert Integrity
    reject
  require lead.state == QUALIFIED
  lead.convertingJourneyId = journeyId
  transition(lead, CONVERTED)
  archiveWorkingInbox(lead)                     # ADR-014 → ARCHIVED
  emit LeadConverted, LeadArchived
```

Working columns become eligible for working-lead retention; attribution fields retain 7 years.

---

## 8. ALG-RESUME

```
function resume(leadId, principal):
  lead = loadVisible(leadId, principal)
  require not lead.state in {ARCHIVED} for inbox actions
  point = Journey.resumePoint(lead.journeyId)   # Journey owns stage
  return ResumePayload(lead, point, prefills)
```

`BR-LEAD-004`.

---

## 9. Stage / status projection (`OPEN-LEAD-STAGE`)

| Dashboard label (BRD) | System of record | Lead field |
|---|---|---|
| New | Lead | state ∈ {NEW,ASSIGNED,CONTACTED} + activityStatus |
| Quote Generated | Lead | QUALIFIED / biGenerated |
| Proposal Form Pending … Policy Declined | Journey / Proposal / Policy | optional **read model** only — not Lead transitions |
| Closed | Lead | DISQUALIFIED terminal |
| Policy Issued (outcome) | Policy + JourneySold | then Lead CONVERTED/ARCHIVED |

Implementers must not add Lead transitions for insurer UW queue states.

---

## 10. Validation → platform errors

| VAL | Algorithm / API |
|---|---|
| VAL-001…005 | Customer search (EPIC-003 / ARCH-025) |
| VAL-006 | create missing productClass |
| VAL-007…011 | assignment mapping at create (mandatory SP / RM per BRD §8, `D-018`) |
| VAL-012 | ALG-DEDUPE |
| VAL-013…015 | Meeting — deferred |
| VAL-016 | ALG-ASSIGN biGenerated guard |
| VAL-017…019 | ALG-CLOSE |
| VAL-020 | create failed — no orphan leadId (transactional create) |

---

## 11. Done for this document

- [x] Deterministic algorithms with BR-* trace
- [x] OPEN conflicts untouched as decisions
- [x] OPEN-LEAD-ACTOR closed (`D-018`); IPR runtime gated (`OPEN-COMP-LEAD-IPR-CREATE`)
- [ ] Product confirmation of OPEN-LEAD-STAGE / OPEN-D1
