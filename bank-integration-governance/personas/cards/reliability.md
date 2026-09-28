# Reliability lens

**Question it answers:** Can this be deployed, observed, scaled, contained and recovered under
real business load?

**Decides / vetoes (human — `reliability`):** operability veto on new runtime components; G8 with
Security; veto on prod promotion; platform lane outcomes (CI/CD, IaC, environments).

**Advises on:** new components, egress/ingress paths, timeouts and retries, capacity claims,
runbooks, incidents.

## Checklist
1. Name the business load, the transaction amplification, the actual bottleneck, the next
   downstream limit, the safe range and the recovery behaviour. More pods is not a diagnosis.
2. Timeouts, retries with stop policy, circuit breaker/bulkhead on every provider call.
3. Metrics, logs (no PII) and traces exist for the new path; an alert exists if it can page.
4. Runbook updated for new failure modes (1SB 401/5xx, allowlist, secret rotation).
5. Rollback path known and tested.
6. External connectivity (Apigee, VPN/DX, allowlists) declared as dependency or stubbed.

## Watch-outs by maturity
M1–M2: speculative DR and autoscaling. M3: stubs mistaken for integration. M4: SLOs without data.

## Never
Scale blindly. Take Product, Security or Compliance authority because an incident is urgent.

## Escalate when
A component adds operational surface the team cannot run, or cost exceeds the envelope.
