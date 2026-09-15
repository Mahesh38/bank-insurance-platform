# Integration and dependency matrix — R0

**Pack:** [`ARB-PREREQUISITE-PACK.md`](../ARB-PREREQUISITE-PACK.md) rows 6 and 16  
**Status:** `AI-DRAFTED` compilation of LLD §8, `ADR-018`/`ADR-020`, and [`DEPENDENCY-REGISTER.md`](../../governance/registers/DEPENDENCY-REGISTER.md).  
**Rule:** a row here does not create a new integration. If it disagrees with an ADR or the LLD, those win.

---

## 1. Runtime integrations (who talks to whom)

| # | Counterpart | Direction | Protocol | Path (R0) | Data on the wire | Owner | R0 status |
|---|---|---|---|---|---|---|---|
| I-01 | NIP-APP (Flutter web/APK/IPA) | Inbound | HTTPS TLS 1.3 | Device → Cloudflare → F5-XC → Amazon API Gateway → Internal ALB → nip-web / `#2` NIP BFF | Opaque session only. **No OAuth tokens on the device.** | Amit + WS-2 | Designed. App not in this repo. |
| I-02 | Amazon API Gateway (inbound) | Inbound | HTTPS | First AWS hop. Separate **PG-callback** route, IP-allowlisted (`TB-6`) | Request schema, throttle. No business logic. | Shivanshi | `ADR-018`. Not provisioned. |
| I-03 | 1SilverBullet | Outbound | HTTPS mTLS | `#14` Hub → `#15` adapter → **Apigee** → 1SB. Adapter base URL = Apigee proxy. **Never** `*.1silverbullet.tech` from EKS | Bank-canonical in; 1SB JSON terminates in `adapter.onesb.*` | WS-1 / Shivanshi + Apigee team | Code exists. Path `ADR-020`. IPs `DEP-20260914-apg` **OPEN**. |
| I-04 | AU Bank Payment Gateway | Outbound session-create | HTTPS | `#12` Payment → **Apigee** → PG | Payment session, no PAN on RM device | Payments + Shivanshi | Designed. |
| I-05 | AU Bank Payment Gateway | Inbound callback | HTTPS | PG → **API Gateway callback route** (not Apigee, not RM session) | Signed callback; `#12` verifies | Deepali + Payments | Designed (`TB-6`). |
| I-06 | AU Bank PG settlement | Inbound / file | File or S3 drop | Out-of-band from the API path | Settlement; reconcile never auto-resolves | Aarti + Finance | Designed. |
| I-07 | EBS / CBS / CIF | Outbound | Bank HTTPS via EBS | `#4` Customer → **Apigee private** → EBS. Stubs in `dev` only | CIF snapshot. No production CIF in UAT | Bank network + `#4` | Path designed. VPN/DX `DEP-20260824-dx1` **OPEN**. |
| I-08 | Bank AD-verify API | Outbound | HTTPS | Identity adapter → **Apigee private** → existing AD-verify. **Never LDAP from EKS** | Workforce credential verify. AD remains SoR (`TI-01`) | WS-2 + bank API platform | `ADR-020`. `DEP-010` AD technology confirmation **OPEN** (overdue). |
| I-09 | SMS / email gateway | Outbound | HTTPS | `#17` Notification → Apigee / inspected egress | OTP and pay-link only. Failure never blocks the journey (`S-18`) | Bank comms | Designed. Beyond OTP/pay-link is out of R0. |
| I-10 | Insurers (Group A) | Via I-03 | — | Platform never calls an insurer origin in R0 | Underwriting stays with the insurer (`TI-03`) | — | Direct insurer APIs are R1+ (`#15` sibling adapter exists as a seam, unused). |
| I-11 | GitLab CI/CD | Delivery | HTTPS | Build, ArchUnit, JaCoCo, Terraform apply | No PII in CI logs | Shivanshi + Amit | Bank standard (`ADR-016`). |
| I-12 | AWS (ap-south-1 / ap-south-2) | Platform | AWS APIs via IRSA / roles | See LLD BOM | Encrypted; CloudTrail on every management call | Shivanshi | Not vended. |

**Forbidden paths (architecture defects if drawn):**

- Java → 1SB origin.
- Internal API → internet → Cloudflare → F5.
- Spoke NAT Elastic IPs published to 1SB.
- Flutter → Keycloak, Apigee, or a domain service.
- Cross-schema database grants.

---

## 2. Third parties and subcontractors

| Party | Role | Data they may see | Contract posture | Exit |
|---|---|---|---|---|
| **1SilverBullet** | Insurance aggregator (provider route) | Quote/proposal payloads required to underwrite; not bank SoR | Bank–1SB commercial + IP allowlist of **Apigee** | Replace adapter; Hub contract unchanged (`TI-04`) |
| **Group A insurers** | Underwriters | Via 1SB in R0 | Insurer terms; bank does not underwrite | Catalogue row + adapter route |
| **Cloudflare, Inc.** | Edge CDN / DDoS (SaaS) | TLS at edge; authenticated JSON **not** cached | Bank enterprise already in use | `ADR-018` hop 1; not in our VPC |
| **F5 (Distributed Cloud / XC)** | Edge WAF (SaaS) | HTTP at WAF | Bank enterprise already in use | Not an in-VPC BIG-IP |
| **Google (Apigee)** *or bank-operated Apigee* | Outbound API plane | 1SB and internal API calls as proxy | Bank API platform product (`DEP-20260914-apg`) | Adapter base URL change |
| **Amazon Web Services** | IaaS/PaaS India | All hosted data, encrypted with bank CMKs | Bank Control Tower already exists (`BE-01`) | See exit paper |
| **GitLab** | SCM + CI | Source and pipeline logs (no PII) | Bank standard | — |

No new subcontractor is introduced by R0 beyond attaching this programme to **already-procured** bank platforms (Cloudflare, F5-XC, Apigee, GitLab, AWS, 1SB). Architecture does not sign commercial papers.

---

## 3. Open dependencies that can halt ARB or S09

Copied from the dependency register so ARB sees the same IDs the delivery lead chases. State is **OPEN** unless the register says otherwise.

| ID | What is blocked | External party |
|---|---|---|
| `DEP-20260914-apg` | 1SB allowlist + AD-verify/EBS private proxies | Apigee team / bank API platform |
| `DEP-20260824-eip` | Must **not** send spoke NAT EIPs to 1SB | Same; rebase onto Apigee IPs |
| `DEP-20260824-dx1` | `uat`/`prod` CBS and AD-verify without stubs | Bank network (VPN then existing DXGW) |
| `DEP-20260824-cst` | S09 budget for the five 2026-08-24 layers | Shivanshi + Kalpana |
| `DEP-002` | WS-1 gate 4.3 bank caller UAT | Rajal / bank app team |
| `DEP-010` | WS-2 Phase 2 AD technology confirmation | Overdue; R0 uses AD-verify API instead of LDAP, but the bank must still name the API |

---

## 4. Internal seams (platform, not third parties)

Documented as `S-01`…`S-26` in [`03-solution-architecture-r0.md`](../../platform/ws3-platform/03-solution-architecture-r0.md) §5. ARB does not need to re-litigate them; the hard gates that matter in the room are C1 suitability, C2 consent, C4 customer-device payment, C7/C8 audit-before-sold.
