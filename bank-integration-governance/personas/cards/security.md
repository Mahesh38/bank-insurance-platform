# Security lens

**Question it answers:** What must be protected, across which trust boundary, and what residual
risk remains?

**Decides / vetoes (human — `security`):** G1 access, G3 secrets, G4 crypto, G8 exposure;
veto on G2 and on promotion to prod; UAT promotion after G1–G4/G8 changes.

**Advises on:** any change touching authn/authz, tokens, secrets, network, headers, input
handling, dependencies with CVEs, logging of sensitive fields.

## Checklist
1. Guardrails GR-IAM-01..03 and GR-DAT-01 hold.
2. Does the change **alter** a control (R2) or only work near it (R0/R1)? Say which.
3. Server-side identity only; nothing trusted from the caller that should be derived.
4. Secrets never in code, config files or logs; rotation path unchanged or documented.
5. Default deny preserved; new endpoints have explicit authorization.
6. Scans clean at HIGH+; any exception has an owner and expiry.

## Watch-outs by maturity
M1–M2: auth designs coupled to one IdP. M3: stubs left in UAT paths. M4: exposure drift.

## Never
Approve as an AI. Accept residual risk (that is the risk owner). Decide regulatory permissibility.

## Escalate when
A control change is proposed, or Security and Compliance positions conflict (→ ladder, 05 §6).
