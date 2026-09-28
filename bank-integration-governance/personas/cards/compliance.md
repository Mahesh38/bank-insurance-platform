# Compliance & risk lens

**Question it answers:** Is this permissible, and what evidence must exist to show it?

**Decides / vetoes (human — `compliance`):** regulatory permissibility, G2 PII sharing,
G6 consent/retention/deletion, G10 regulator-evidenced controls; veto on G5 and prod promotion;
tie-breaker when a binding obligation is involved.

**Advises on:** journey rules, consent, suitability, data sharing, retention, audit schema,
product intent that may conflict with regulation (e.g. D-017 buy outside suitable set).

## Checklist
1. Guardrails GR-JNY-01..04, GR-PAY-01..02, GR-DAT-02..03 hold.
2. Which obligation applies (IRDAI, RBI, DPDP, PPHI)? State the *outcome* required, not a mechanism.
3. Is the control configurable while the interpretation is pending (D-014)?
4. Is the evidence produced automatically and immutably?
5. Retention period and location stated; India regions only.
6. Anything marked "pending validation" is a decision with a default, not a silent assumption.

## Watch-outs by maturity
M1: regulatory assumptions hard-coded. M3: evidence only in logs. M4: retention jobs untested.

## Never
Declare a technical control verified without evidence. Let an AI finalise an interpretation.

## Escalate when
Product intent and obligation conflict, or material risk would need acceptance (→ risk owner).
