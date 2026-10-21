# Istio ops addendum — R0 provision constraints (`ADR-023`)

**Status:** Binding companion to [`ADR-023`](../platform/architecture-review/08-architecture-decision-log.md) and [`R0-LLD.md`](./R0-LLD.md) §3  
**Audience:** Shivanshi / bank platform (Istio operators) · Amit (retry alignment) · Deepali (S1–S11 verify)  
**Purpose:** Close the ops gaps that the inbound product ADR does not size — enough to provision without inventing mesh policy at apply time.

---

## 1. Scope admitted

| In R0 | Out of R0 |
|-------|-----------|
| Istio Ingress Gateway | AWS App Mesh |
| Sidecar injection in application namespaces carrying business traffic | Ambient mesh (unless bank SPIKE writes it as the standard) |
| PeerAuthentication: PERMISSIVE → dated **STRICT** | Mesh as substitute for BFF session or PDP |
| Envoy request size + rate limits (S9) | Public Gateway routes to Keycloak / data plane |
| NetworkPolicy retained (S11) | Unbounded DestinationRule retries |

Namespaces (minimum): `edge`, `identity`, `shared-platform` / shared, `life-cell`, `integration`, `jobs` — exact labels follow bank platform chart; **`identity` must not expose Keycloak on the public Gateway**.

---

## 2. SPIKE answers still required from bank platform (D7)

Record under `DEP-20261006-igw` / SPIKE chase — do not invent versions in Terraform:

1. Istio **edition / version** and install path (operator vs helm vs bank shared chart)  
2. **Ambient vs sidecar** — R0 assumes sidecar until written otherwise  
3. Who operates control plane upgrades and on-call  
4. F5 egress **CIDR list** + refresh cadence (S2)  
5. Whether ACM terminates at **ALB** instead of NLB+Istio Gateway TLS  

Until (1)–(2) land, modules may draft against placeholders; non-dev apply waits on written answers.

---

## 3. Injection and STRICT cutover

| Step | Rule |
|------|------|
| Labels | `istio-injection=enabled` (or bank revision label) on admitted namespaces only |
| PERMISSIVE window | Named start/end dates in the runbook; no open-ended PERMISSIVE in prod |
| STRICT | After probes and Job/CronJob paths verified over mTLS |
| Probes | Prefer `rewriteAppHTTPProbers` / separate plain probe port per bank chart — document the chosen pattern before STRICT |
| Keycloak | Cluster-internal only; no `Gateway`/`VirtualService` host for public Keycloak |

---

## 4. Retries and timeouts (no double-fire)

| Layer | Owns |
|-------|------|
| Business / provider calls | Resilience4j (or equivalent) in Java — Hub → adapter → Apigee |
| Mesh DestinationRule | **Conservative** connect/request timeouts; retries **off** or capped for non-idempotent POSTs on journey/payment paths |
| Rule | A mutating business call has **one** retry owner. Mesh must not silently retry what the app already retries |

Amit publishes the per-seam matrix in the shared-lib DoD when services land; platform defaults fail closed (no broad `attempts: 3` on `*`).

---

## 5. Observability and evidence

| Signal | Pipe |
|--------|------|
| NLB flow / access | CloudWatch → Firehose → OpenSearch (`ADR-013`) |
| Envoy access logs | Same operational pipe (S10) — **not** regulatory evidence |
| Mesh control plane / dataplane | AMP/AMG dashboards; alert on Ingress 5xx, pilot push failures |
| Audit / WORM | Unchanged — mesh logs never satisfy a gate |

---

## 6. DR (D10 class)

Failover runbook step (manual in R0): re-point Cloudflare/F5 origin to **DR-region ingress NLB** + bring up Istio Ingress; do **not** look for a DR API Gateway. Mesh trust/roots restored from Secrets Manager replicas (D6/D7 in LLD §11.1).

---

## 7. Cost note (`RISK-012`)

NLB LCU + Istio sidecars (CPU/memory) replace API Gateway request pricing. Refresh the S09 cost envelope before first `uat` apply; do not drop S1–S11 controls to fit a pilot budget without Security + SRE verdict.

---

## 8. Verify before claiming “mesh live”

Checklist maps to [`2026-10-09-ADR-023-SECURITY-CONTROL-MAP.md`](./2026-10-09-ADR-023-SECURITY-CONTROL-MAP.md):

- [ ] Direct NLB without F5 secret/mTLS → 403 (S3)  
- [ ] NLB SG has no `0.0.0.0/0` (S2)  
- [ ] External scan: no Keycloak route (S5)  
- [ ] `/callbacks/pg/*` rejects non-PG source IPs (S8)  
- [ ] PeerAuthentication STRICT in prod after cutover date (S11)  
- [ ] NetworkPolicy still present (S11)  
