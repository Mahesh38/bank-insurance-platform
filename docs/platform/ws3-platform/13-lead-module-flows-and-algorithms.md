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
  if lead.exceptionHold:
    reject EXCEPTION_HOLD_ACTIVE                    # assign UI may still open; process-further blocked
  if lead.biGenerated:
    reject REASSIGN_AFTER_BI                        # VAL-016 for reassignment
  assignee = loadPrincipal(targetSpRmId)
  require assignee.actorType == BANK_RM
  require assignee.hasSpCert(lead.lob)              # INV-LED-10 / IRDAI SP
  require customerInEtbBook(assignee, lead.customerId)  # INV-LED-05
  history.append(...)
  lead.assignedRmId = assignee.id
  if lead.accountableSpId is null:
    lead.accountableSpId = assignee.id              # INV-ACT-03 write-once
  lead.state = ASSIGNED
  if meeting:
    require meeting.type in {ONLINE, IN_PERSON}
    require meeting.date >= today
    require meeting.time in [08:00, 20:00]
    if meeting.type == ONLINE: require meeting.link
    lead.meetingIntent = meeting                    # optional; BR-LEAD-003 allows omit
  emit AssignmentChanged
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

## 6. ALG-BI / ALG-CLOSE / ALG-CONVERT

Unchanged in intent from prior revision (`BR-BI-*`, `BR-CLOSE-*`, `ADR-014`). BI mark, close, convert still refuse terminal / held leads as appropriate.

---

## 7. Validation → platform errors

| Case | Code |
|---|---|
| Dedupe unfinished | `409 LEAD_DUPLICATE_UNFINISHED` |
| Validation direct block | `422 VALIDATION_BLOCKED` |
| Held for approval | `201` + `exceptionHold=true` (or `409 EXCEPTION_HOLD` on process-further) |
| Missing SP on process-further | `422 ASSIGNEE_SP_REQUIRED` |
| Meeting field errors | VAL-013…015 |

---

## 8. Done for this document

- [x] Create-then-assign algorithms (`D-019`)
- [x] Dedupe / validation / process-further gates
- [x] OPEN-LEAD-DUP-DELETE and OPEN-LEAD-VAL-TIMING explicit
- [ ] Rajal closes those two OPENs
