# NIP-APP → edge → BFF (seam S-01)

[← API reference home](../README.md)

| | |
|---|---|
| **From → to** | NIP-APP (Flutter web / APK / IPA) → Cloudflare → F5-XC WAF → Ingress NLB → Istio Ingress Gateway → `workforce-access-bff` |
| **Style** | Synchronous HTTPS; 5 s edge budget |
| **Auth** | Opaque BFF session (cookie, `X-Session-Handle`, or `Bearer <handle>`) — never an OAuth token |
| **Failure** | Typed `problem+json`; journey stage unchanged |
| **Decision records** | `ADR-023` (NLB + Istio; API Gateway withdrawn), `ADR-015` (one app, role-based), `ADR-017` (error contract) |

## 1. The path, hop by hop

| # | Hop | What it does | What it refuses |
|---|---|---|---|
| 1 | **Cloudflare Enterprise** (bank SaaS, not in AWS) | CDN, DDoS, TLS 1.3 | Non-HTTPS; volumetric attacks |
| 2 | **F5 Distributed Cloud (F5-XC)** (bank SaaS WAF) | OWASP rules, bot defence, L7 rate limits | Injection, bots, oversized requests |
| 3 | **Ingress NLB** (public/DMZ subnets) | First AWS hop; security group = F5 / Cloudflare egress CIDRs only | Any other source IP |
| 4 | **Istio Ingress Gateway** | Path split: `GET /*` → `nip-web` (static Flutter web), `/api/*` → BFF, `/callbacks/pg/*` → Payment (PG IP allowlist); Envoy size/throttle; mesh mTLS east–west | Routes to Keycloak or domain pods |
| 5 | **BFF** | Session, CSRF, validation, aggregation, error redaction | Anything without a valid session on `/api/v1/**` (except `/auth/**`) |

No domain service, Keycloak, Aurora or Valkey is reachable from the internet. Apigee is
**outbound only** — Flutter never calls it.

## 2. What the app must send

| Item | Rule |
|---|---|
| Base URL | `https://{env}-insurance.aubank.in/api/v1` (illustrative; DNS is S09) |
| Session | Web: cookie set by login; native: `X-Session-Handle` from the login / native-session response |
| CSRF (web) | `GET /api/v1/auth/csrf` once, then `X-XSRF-TOKEN` on every mutation |
| `X-Correlation-Id` | A UUID per user action (contracted as required) |
| `Idempotency-Key` | A UUID per **mutating** user action; reuse it on a retry of the same action |
| Body | `application/json`; unknown fields such as `distributorId` are rejected |

## 3. What comes back

- Success: the resource itself (no `{success, data}` wrapper).
- Error: public `problem+json` with `code`, `category`, `retryable`, `incidentId`, `correlationId`.
  Never a stack trace, upstream text, vendor name or internal path — the BFF redacts at L4.
- `401 SESSION_INVALID` / `SESSION_EXPIRED` → app returns to login.
- `403` compliance codes (`SUITABILITY_REQUIRED`, `CONSENT_REQUIRED`, …) → show the action the RM
  must take; do not retry.
- `503` → show the dependency is down; retry later with the **same** `Idempotency-Key`.

## 4. Sources

- [R0 for developers §3](../../../architecture/R0-E2E-FOR-DEVELOPERS.md) · [Request lifecycle](../../../journey-execution/01-REQUEST-LIFECYCLE-STANDARD.md) · [ADR-023 security control map](../../../architecture/2026-10-09-ADR-023-SECURITY-CONTROL-MAP.md)
