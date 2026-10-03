# 13 — Lead flows and algorithms (R0)

**Status:** `AI-DRAFTED` · T3 · human Board 1 outstanding  
**Origin:** `SUG-20260930-lmd` · `SUG-20261002-lfs` · `SUG-20261003-brf` · `EPIC-005` · `DOC-023` · `D-019`  
**Rule fidelity:** Lead BRD + Exception BRD win. Do not invent Product overrides of confirmed BRD rules.

---

## 1. ALG-CREATE — Save Lead (no exception evaluation)

```
function createLead(cmd, principal):
  require isAllowedWorkforceCreator(principal)      # INV-LED-04
  if principal.actorType == INSURER_PARTNER_REP:
    require featureFlag.iprLeadCreateEnabled        # OPEN-COMP-LEAD-IPR-CREATE
  require cmd.lob == LIFE
  require cmd.productClass in {TERM, SAVINGS, ULIP}

  # --- Dedupe (creator bucket) — Lead BRD §11 ---
  dup = ALG_DEDUPE(principal.id, cmd.customerId, cmd.productClass)
  if dup.blocked:
    if cmd.resumeExisting:
      return Resume(dup.existingLead)
    return Conflict(dup.existingLead)               # Continue | Cancel only (Table 18)
    # replaceExisting / soft-delete: FORBIDDEN — OPEN-LEAD-DUP-DELETE CLOSED

  # Exception BRD: Saving a lead does NOT evaluate rules
  leadId = newUlid()
  lead = Lead(
    leadId,
    customerId = cmd.customerId,
    lob = LIFE,
    productClass = cmd.productClass,
    createdBy = principal.id,                       # BR-OWN-002
    leadGenerator = resolveGenerator(principal, cmd),
    assignedRmId = null,                            # assign after exception clear
    accountableSpId = null,
    state = NEW,
    exceptionHold = false,
    exceptionEvaluated = false,
    biGenerated = false,
    reportingClass = DIARY,
    insurerId = null
  )
  journeyId = Journey.startFromLead(leadId)         # inert until Start Onboarding
  lead.journeyId = journeyId
  persist(lead)
  emit LeadCreated(...)
  return Created(lead)                              # next: Start Onboarding
```

---

## 2. ALG-DEDUPE — unfinished same creator + customer + product type

```
function ALG_DEDUPE(userId, customerId, productClass):
  unfinished = findLeads(
    createdOrOwnedBy = userId,                      # logged-in user — not system-wide
    customerId = customerId,
    productClass = productClass,                    # TERM | SAVINGS | ULIP — not Product ID
    biGenerated = false,
    state not in TERMINAL_CLOSED_SET
  )
  if unfinished is not empty:
    return Blocked(existingLead = newest(unfinished))
  return Allow
```

| UI option | Lead BRD Screen 6 Table 18 | Status |
|---|---|---|
| Continue with existing | Allowed | **Ship** |
| Cancel / Close | Allowed | **Ship** |
| Delete existing and create new | **Forbidden** when BI not generated | **CLOSED** — do not ship |

After `biGenerated=true`, dedupe does not block a fresh lead (`BR-DEDUPE`).

---

## 3. ALG-START-ONBOARDING — Exception / validation (after Save)

```
function startOnboarding(leadId, principal):
  lead = load(leadId)
  require not lead.isTerminal()
  # Exception BRD: evaluation runs at Start Onboarding, not on Save
  verdict = ValidationEngine.evaluate(
    lead.customerId, lead.productClass, principal)
  # verdict ∈ PASS | BLOCK | APPROVAL_REQUIRED {ruleIds}
  # Examples (config, not hard-coded): new CASA within 30d (EH-INT-001);
  #   policy count thresholds (EH-INT-004). Catalogue owned by Exception BRD.
  lead.exceptionEvaluated = true
  if verdict == BLOCK:
    lead.exceptionHold = false
    persist(lead)
    return Blocked(verdict.ruleIds)                 # progression locked
  if verdict == APPROVAL_REQUIRED:
    lead.exceptionHold = true
    lead.exceptionRuleIds = verdict.ruleIds
    persist(lead)
    emit LeadHeldForException(...)
    return Held(lead)                               # assign + Suitability gated
  lead.exceptionHold = false
  lead.exceptionRuleIds = []
  persist(lead)
  return Cleared(lead)                              # proceed to Assignment
```

Lead stores hold; **approver hierarchy and remarks** are Exception Handling — not Lead.

---

## 4. ALG-ASSIGN — after exception clear / release (+ optional meeting)

```
function assign(leadId, targetSpRmId, meeting?, actor):
  lead = load(leadId)
  require not lead.isTerminal()
  require lead.exceptionEvaluated                   # Start Onboarding must have run
  if lead.exceptionHold:
    reject EXCEPTION_HOLD_ACTIVE                    # assign after release
  if lead.biGenerated:
    reject REASSIGN_AFTER_BI                        # BR-REASSIGN-001, VAL-016
  require PDP.allows(actor, assign, lead)
  assignee = loadPrincipal(targetSpRmId)
  require assignee.actorType == BANK_RM
  require assignee.hasSpCert(lead.lob)              # INV-LED-03/10 / IRDAI SP
  require customerInEtbBook(assignee, lead.customerId)  # INV-LED-05
  history.append(old → new, actor, ts)
  lead.assignedRmId = assignee.id
  # OPEN-D1: SLA reset / conversion credit — provisional BR-OWN-003 (current ownership)
  if lead.accountableSpId is null:
    lead.accountableSpId = assignee.id              # INV-ACT-03 write-once
  lead.state = ASSIGNED
  if meeting:
    require meeting.type in {ONLINE, IN_PERSON}     # VAL-013…015
    require meeting.date >= today
    require meeting.time in [08:00, 20:00]
    if meeting.type == ONLINE: require meeting.link
    lead.meetingIntent = meeting                    # optional; BR-LEAD-003 allows omit
  emit AssignmentChanged
  audit(ASSIGNMENT)
  return lead
```

All leads **must** receive this SP assignment before Suitability / regulated processing (`D-019`).

---

## 5. ALG-PROCESS-FURTHER gate

```
function mayProcessFurther(principal, lead):
  if not lead.exceptionEvaluated: return false
  if lead.exceptionHold: return false
  if lead.assignedRmId is null or lead.accountableSpId is null: return false
  if principal.actorType == BANK_RM and principal.hasSpCert(lead.lob): return true
  if principal.actorType == INSURER_PARTNER_REP: return true  # assist path; regulated acts still INV-ACT-01/02
  return false
```

Creators who are Non-SP may create/Save, Start Onboarding, assign, and Save & Close but cannot run Suitability themselves.

---

## 6. ALG-BI — first Benefit Illustration

Handler for `POST /internal/v1/leads/{leadId}/bi-generated` (and the equivalent durable event).

```
function markBiGenerated(leadId, biReference, occurredAt):
  lead = load(leadId)
  require not lead.isTerminal()
  require not lead.exceptionHold                    # held leads cannot become Eligible
  if lead.biGenerated:
    linkBi(leadId, biReference)                     # BR-BI-003/005 — no new lead
    return IdempotentOk
  # Only successful BI counts (BR-BI-001/002)
  lead.biGenerated = true
  lead.reportingClass = ELIGIBLE                    # BR-BI-004 Diary→Eligible
  transition(lead, QUALIFIED)                       # domain; OPEN-LEAD-STAGE vs BRD "Quote Generated"
  emit LeadQualified
  audit(BI_GENERATED)
```

Reaching quote screen without BI response: **no** change (`BR-BI-002`).

---

## 7. ALG-ACTIVITY — disposition before BI

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

## 8. ALG-CLOSE

```
function close(leadId, reasonCode, remarks, actor):
  require reasonCode present                        # VAL-017
  if reasonCode == OTHER:
    require remarks non-empty                       # VAL-018
  require length(remarks) <= 250                    # VAL-019
  lead = load(leadId)
  require not lead.isTerminal()
  transition(lead, DISQUALIFIED)                    # or CLOSED label in projection
  lead.closedReason = reasonCode
  lead.remarks = remarks
  emit LeadClosed
  # BR-CLOSE-001: cannot reopen
```

---

## 9. ALG-CONVERT / ALG-ARCHIVE

```
function onJourneySold(leadId, journeyId, paymentId, policyId):
  lead = load(leadId)
  if lead.state == CONVERTED and lead.convertingJourneyId == journeyId:
    return IdempotentOk                             # INV-LED-02
  if lead.state == CONVERTED and journeyId differs:
    alert Integrity
    reject
  require lead.state == QUALIFIED
  lead.convertingJourneyId = journeyId
  transition(lead, CONVERTED)
  archiveWorkingInbox(lead)                         # ADR-014 → ARCHIVED
  emit LeadConverted, LeadArchived
```

Working columns become eligible for working-lead retention; attribution fields retain 7 years (`C-RET-1`).

---

## 10. ALG-RESUME

```
function resume(leadId, principal):
  lead = loadVisible(leadId, principal)
  require not lead.state in {ARCHIVED} for inbox actions
  point = Journey.resumePoint(lead.journeyId)       # Journey owns stage
  return ResumePayload(lead, point, prefills)
```

`BR-LEAD-004`.

---

## 11. Stage / status projection (`OPEN-LEAD-STAGE`)

| Dashboard label (BRD) | System of record | Lead field |
|---|---|---|
| New | Lead | state ∈ {NEW,ASSIGNED,CONTACTED} + activityStatus |
| Quote Generated | Lead | QUALIFIED / biGenerated |
| Proposal Form Pending … Policy Declined | Journey / Proposal / Policy | optional **read model** only — not Lead transitions |
| Closed | Lead | DISQUALIFIED terminal |
| Policy Issued (outcome) | Policy + JourneySold | then Lead CONVERTED/ARCHIVED |

Implementers must not add Lead transitions for insurer UW queue states.

---

## 12. Validation → platform errors

| Case / VAL | Algorithm / API |
|---|---|
| VAL-001…005 | Customer search (EPIC-003 / ARCH-025) |
| VAL-006 | create missing productClass |
| VAL-007…011 | assignment SP / cert checks (`D-018`/`D-019`, ALG-ASSIGN) |
| VAL-012 / `409 LEAD_DUPLICATE_UNFINISHED` | ALG-DEDUPE — Continue \| Cancel only |
| VAL-013…015 | Meeting intent fields on ALG-ASSIGN (optional) |
| VAL-016 / `422 REASSIGN_AFTER_BI` | ALG-ASSIGN after BI |
| VAL-017…019 | ALG-CLOSE |
| VAL-020 | create failed — no orphan leadId (transactional create) |
| `422 VALIDATION_BLOCKED` | Start Onboarding direct block |
| `409 EXCEPTION_HOLD_ACTIVE` | assign / Suitability while held |
| `422 ASSIGNEE_SP_REQUIRED` | ALG-PROCESS-FURTHER missing SP |
| `422 ONBOARDING_NOT_STARTED` | assign before Start Onboarding evaluate |

---

## 13. Done for this document

- [x] BRD-aligned create (Save) without exception eval
- [x] Dedupe key + Table 18 Continue\|Cancel (no delete)
- [x] Start Onboarding → exception → assign algorithms
- [x] ALG-BI / ACTIVITY / CLOSE / CONVERT / ARCHIVE / RESUME with BR-* trace
- [ ] Product confirmation of OPEN-LEAD-STAGE / OPEN-D1
