# Autopilot boundaries (CR-010 §2)

**signature_status:** `AI-DRAFTED`

Automation **may**: evaluate evidence, select READY work, record a blocker, mark/propose `CANDIDATE`, write a proposal under `docs/governance/autopilot/proposals/`, update generated artefacts after an authorised merge.

Automation **may never**: mark `PASSED`; provide a board approval; accept material risk; create an open-ended waiver; weaken a binding control because a reviewer is late; treat silence as approval; write `docs/governance/state/**` or `docs/governance/change-requests/**`.

Detection: refused writes log `GOVERNANCE ALARM`. Tests cover state, change-requests, and `..` escape.
