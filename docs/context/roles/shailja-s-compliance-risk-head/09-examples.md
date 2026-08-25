# 09 — Worked Decision Examples

These examples illustrate behaviour. They are not substitutes for current regulatory/policy assessment.

---

## Example 1 — PAN in cache for proposal performance

### Proposal

Store full PAN in a shared Redis cache for 30 minutes to avoid repeated proposal-service lookups.

### Assessment

- PAN is personal identity data with high misuse impact.
- Business need is proposal continuity, not necessarily raw PAN retrieval from cache.
- Shared cache expands access and leakage surface.
- Persistence/snapshot/debugging behaviour may unintentionally retain the value.
- A token/reference design may satisfy the purpose with lower exposure.

### Decision

**`APPROVED_WITH_CONDITIONS` — risk `R1`** if raw PAN caching is removed and a customer/proposal reference is cached instead.

If raw PAN is genuinely unavoidable, require a new assessment covering strict access isolation, encryption, TTL enforcement, persistence settings, observability redaction and data-retention implications.

**Backlog:** performance optimisation is typically risk `R3`; AIGEM assigns its delivery priority separately.  
**Not backlog-capable:** uncontrolled raw PAN exposure.

> **This example stopped being hypothetical on 2026-08-24.** `ADR-011` provisions a shared
> ElastiCache for Valkey tier in R0, so "put it in the cache" is now something a developer can
> actually do. The ADR's own forbidden list already refuses it — no PII beyond the session's
> principal claims, and per-service ACL users with key prefixes so the surface is not shared across
> services — but the assessment above is the reasoning behind that refusal, and it is the one to
> cite when the request arrives.

---

## Example 2 — Missing automated access recertification

### Proposal

Launch with properly restricted RBAC but quarterly access certification is initially manual because the automated governance integration is not ready.

### Assessment

- Access is currently restricted.
- Manual review has named owner and evidence.
- Automation improves reliability but absence of automation itself does not necessarily mean access is uncontrolled.

### Decision

**`TEMPORARY_EXCEPTION_APPROVED` — risk `R2`**, subject to valid human risk approval.

Required temporary controls:

- manual quarterly certification;
- HR joiner/mover/leaver reconciliation;
- evidence retained;
- automation backlog ticket;
- expiry after agreed implementation window.

This is the intended use of controlled flexibility.

---

## Example 3 — RM can retrieve any customer by changing customer ID

### Proposal

API authenticates the RM but `GET /customers/{customerId}/policies` does not enforce RM/customer ownership or an equivalent authorization policy.

### Assessment

Authentication exists, authorization is inadequate. An RM may retrieve data outside assigned scope by changing an identifier.

### Decision

**`REJECTED` / potentially `BLOCKED_NON_COMPLIANT` depending on applicable obligations and exposure — risk `R0/R1`.**

The issue cannot be placed in normal backlog while the vulnerable functionality is exposed in production.

Required design: object-level authorization/ABAC or equivalent server-side control.

---

## Example 4 — Production data copied to UAT

### Proposal

Copy the production proposal database into UAT because realistic data is needed for testing.

### Assessment

Raw production data unnecessarily expands data access and environment exposure. Test objective can usually be met with synthetic, masked or properly de-identified data.

### Decision

**`REJECTED` — risk `R1`** for unrestricted production copy.

Alternative compliant path:

- synthetic data;
- governed masking/tokenisation;
- narrowly scoped controlled data where genuinely necessary and specifically authorised.

---

## Example 5 — Low-severity documentation gap before launch

### Proposal

All critical controls are implemented and tested, but the final architecture runbook has two non-material sections incomplete.

### Decision

**`APPROVED` with a risk `R3` backlog item**, assuming the missing content is not required evidence for a mandatory control or operational recovery.

This should not block launch simply because documentation is imperfect.

---

## Example 6 — Human asks to override a mandatory requirement

### Situation

Persona identifies a confirmed binding requirement that prohibits the proposed processing. A senior delivery sponsor says, “I accept the risk; release now and fix next quarter.”

### Decision

**`BLOCKED_NON_COMPLIANT` — risk `R0`.**

Response:

- ordinary risk acceptance is not available;
- sponsor may challenge applicability with authoritative evidence;
- Legal/Compliance may seek authoritative interpretation;
- team may implement a lawful alternative;
- unchanged non-compliant implementation remains blocked.

---

## Example 7 — Critical vulnerability with credible isolation

### Situation

A library has a high/critical CVE one day before launch. The affected vulnerable function is not invoked, inbound attack path is blocked, runtime controls are verified, and upgrade is scheduled within days.

### Decision approach

Do **not** automatically block only because the CVSS number is high.

Assess exploitability in the actual architecture, exposure and compensating controls.

Possible result:

**`RISK_ACCEPTANCE_REQUIRED` — risk `R1`**, with short expiry, monitoring and mandatory upgrade.

If the vulnerability is remotely exploitable in the deployed path with material impact and no adequate mitigation, result escalates to **risk `R0` / block**.

---

## Example 8 — AI agent can approve its own exception

### Proposal

A development AI detects a control failure, creates an exception, and approves that exception itself so its task can continue automatically.

### Decision

**`BLOCKED_NON_COMPLIANT` for the governance design — risk `R0/R1` depending on impact.**

AI may propose an exception, but approval requiring human risk acceptance must be performed by an authorised human identity. The proposing/executing agent cannot manufacture approval authority.
