# 13 — Lead flows and algorithms (R0)

**Status:** `AI-DRAFTED` · T3 · human Board 1 / Product outstanding  
**Origin:** `SUG-20260930-lmd` · `SUG-20261002-lfs` · `EPIC-005` · `DOC-023` · `D-019`  
**Rule fidelity:** BRD rule IDs cited. Conflicts → OPEN ids. Do not invent Product decisions.

---

## 1. ALG-CREATE — create Lead (`D-019`: SP assignment is a later step)

```
function createLead(cmd, principal):
  require isAllowedWorkforceCreator(principal)      # INV-LED-04
  if principal.actorType == INSURER_PARTNER_REP:
    require featureFlag.iprLeadCreateEnabled        # OPEN-COMP-LEAD-IPR-CREATE
  require cmd.lob == LIFE
  require cmd.productClass in {TERM, SAVINGS, ULIP}

  # --- Dedupe (creator bucket) ---
  dup = ALG_DEDUPE(principal.id, cmd.customerId, cmd.productClass)
  if dup.blocked:
    if cmd.resumeExisting:
      return Resume(dup.existingLead)
    if cmd.replaceExisting:                         # OPEN-LEAD-DUP-DELETE
      require OPEN_LEAD_DUP_DELETE_ENABLED
      closeOrArchive(dup.existingLead, reason=REPLACED_BY_CREATOR)
    else:
      return Conflict(dup.existingLead)             # Continue | Cancel UI

  # --- Validation / Exception engine ---
  # OPEN-LEAD-VAL-TIMING: Product wants this at create; Exception BRD prefers Start Onboarding
  verdict = ValidationEngine.evaluate(customerId, productClass, principal)
  # verdict ∈ PASS | BLOCK | APPROVAL_REQUIRED {ruleIds}
  # Examples (config, not hard-coded): new CASA within 30d (EH-INT-001);
  #   policy count thresholds (EH-INT-004). Catalogue owned by Exception BRD.
  if verdict == BLOCK:
    return Blocked(verdict.ruleIds)                 # no leadId minted
  # APPROVAL_REQUIRED → still mint lead, mark hold

  leadId = newUlid()
  lead = Lead(
    leadId,
    customerId = cmd.customerId,
    lob = LIFE,
    productClass = cmd.productClass,
    createdBy = principal.id,                       # BR-OWN-002
    leadGenerator = resolveGenerator(principal, cmd),
    assignedRmId = null,                            # D-019 — assign on next screen
    accountableSpId = null,
    state = NEW,
    exceptionHold = (verdict == APPROVAL_REQUIRED),
    exceptionRuleIds = verdict.ruleIds or [],
    biGenerated = false,
    reportingClass = DIARY,
    insurerId = null
  )
  journeyId = Journey.startFromLead(leadId)         # may be inert while held
  lead.journeyId = journeyId
  persist(lead)
  if lead.exceptionHold:
    emit LeadHeldForException(...)
  else:
    emit LeadCreated(...)
  return Created(lead)                              # client → Assignment screen
```

---

## 2. ALG-DEDUPE — unfinished same creator + customer + product

```
function ALG_DEDUPE(userId, customerId, productClass):
  unfinished = findLeads(
    createdOrOwnedBy = userId,
    customerId = customerId,
    productClass = productClass,
    biGenerated = false,
    state not in TERMINAL_CLOSED_SET
  )
  if unfinished is not empty:
    return Blocked(existingLead = newest(unfinished))
  return Allow
```

| UI option (Product `D-019`) | Lead BRD Screen 6 Table 18 | Status |
|---|---|---|
| Continue with existing | Allowed | **Ship** |
| Cancel / Close | Allowed | **Ship** |
| Delete existing and create new | **Forbidden** when BI not generated | `OPEN-LEAD-DUP-DELETE` — do not ship until Rajal confirms override |

After `biGenerated=true`, dedupe does not block a fresh lead (`BR-DEDUPE`).

---

## 3. ALG-VALIDATE — Exception / validation outcomes

```
function applyValidation(customerId, productClass, principal):
  rules = ExceptionConfig.activeRules(lob=LIFE)     # AUBIMA owns catalogue
  triggered = []
  for r in rules where inputsAvailable(r, customerId):
    if r.breached(customerId):
      triggered.append(r)
  if triggered empty: return PASS
  if any DirectBlock: return BLOCK(triggered)
  if any ApprovalEnabled: return APPROVAL_REQUIRED(triggered)
  return BLOCK(triggered)                           # fail closed if misconfigured
```

Lead stores hold; **approver hierarchy and remarks** are Exception Handling — not Lead.

---

## 4. ALG-ASSIGN — post-create mandatory SP (+ optional meeting)

```
function assign(leadId, targetSpRmId, meeting?, actor):
  lead = load(leadId)
  require not lead.isTerminal()
  # exceptionHold does NOT block assignment — only process-further (see ALG-PROCESS-FURTHER)
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

All leads **must** receive this SP assignment before suitability / regulated processing (`D-019`).

---

## 5. ALG-PROCESS-FURTHER gate

```
function mayProcessFurther(principal, lead):
  if lead.exceptionHold: return false
  if lead.assignedRmId is null or lead.accountableSpId is null: return false
  if principal.actorType == BANK_RM and principal.hasSpCert(lead.lob): return true
  if principal.actorType == INSURER_PARTNER_REP: return true  # assist path; regulated acts still INV-ACT-01/02
  return false
```

Creators who are Non-SP may create/assign/Save & Close but cannot run suitability themselves.

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
| VAL-012 / `409 LEAD_DUPLICATE_UNFINISHED` | ALG-DEDUPE |
| VAL-013…015 | Meeting intent fields on ALG-ASSIGN (optional) |
| VAL-016 / `422 REASSIGN_AFTER_BI` | ALG-ASSIGN after BI |
| VAL-017…019 | ALG-CLOSE |
| VAL-020 | create failed — no orphan leadId (transactional create) |
| `422 VALIDATION_BLOCKED` | Exception engine direct block |
| `201` + `exceptionHold=true` | APPROVAL_REQUIRED at create |
| `409 EXCEPTION_HOLD_ACTIVE` | process-further / BI while held |
| `422 ASSIGNEE_SP_REQUIRED` | ALG-PROCESS-FURTHER missing SP |

---

## 13. Done for this document

- [x] Create-then-assign algorithms (`D-019`)
- [x] Dedupe / validation / process-further gates
- [x] ALG-BI / ACTIVITY / CLOSE / CONVERT / ARCHIVE / RESUME with BR-* trace
- [x] OPEN-LEAD-DUP-DELETE and OPEN-LEAD-VAL-TIMING explicit
- [ ] Rajal closes those two OPENs (+ OPEN-LEAD-STAGE / OPEN-D1)
