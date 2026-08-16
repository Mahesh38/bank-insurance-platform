# 10 — Actor, Epic and Story Map

**Owner:** Rajal (Product)

**Purpose:** Name every application actor, define its authority boundary, and point to the Bible
epics and stories that make the actor real.

---

## 1. Product journey sequence

| Wave | Journey | Rule |
|---|---|---|
| **A — Assisted-first** | RM-assisted Term journey | S11 proves one complete sale with supporting Admin, insurer representative, Operations and management controls |
| **B — DIY** | Customer self-service | S13-E03-S01 starts only after the assisted lifecycle is stable and customer identity is available |
| **C — Hybrid** | RM/customer hand-off | S13-E03-S02 starts only after assisted and DIY are independently stable |

DIY and hybrid remain intended product journeys; they are not part of the first releasable slice.

## 2. Human application actors

| Actor | Authority boundary | Canonical epics/stories |
|---|---|---|
| **Relationship Manager (RM)** | Own/assist authorised leads; regulated actions require branch scope and valid qualification | S11-E02, S11-E03, S11-E04, S11-E06 |
| **ETB Customer** | Gives consent and declarations, selects an offer, pays on personal device; primary DIY actor later | S11-E03-S05, S11-E05-S01, S13-E03-S01 |
| **Platform / Configuration Admin** | Maintains approved configuration; cannot self-approve privileged changes or perform selling actions by implication | S11-E08; S13-E04-S04 |
| **Insurer Representative** | Own-insurer, authorised-branch, explicitly shared cases only; cannot become the bank RM or mark Sold | S11-E09 |
| **Operations Executive** | Owns exceptions, reconciliation, underwriting escalation and post-issuance tracking; cannot manufacture regulated evidence | S11-E10; S15-E02-S03 |
| **Compliance Officer** | Reviews consent, suitability, attribution and control evidence; approval rights follow the authority matrix | S11-E03, S11-E07 |
| **Audit User** | Read-only retrieval of immutable journey evidence; retrieval itself attributable | S11-E07-S02 |

## 3. Management actors

| Actor | Allowed | Never implied |
|---|---|---|
| **Branch Manager** | Branch pipeline visibility, authorised assignment/reassignment and escalation | RM selling authority or qualification |
| **Regional Manager** | Aggregated branch performance, ageing and escalation | Routine customer or proposal mutation |
| **Sales Head** | Funnel, conversion, issuance and insurer-performance oversight | Case-level access merely because aggregate reports are available |
| **Insurance Business Head** | Portfolio outcomes and approved business configuration decisions | Override of consent, suitability, underwriting, reconciliation or audit evidence |

Canonical stories: **S11-E11-S01…S04**. The separation test is **S11-VT-16**.

## 4. External and system actors

| Actor | Lifecycle responsibility |
|---|---|
| **Bank Identity / SSO** | Authenticates workforce users and supplies stable identity context |
| **CBS / Customer System** | Supplies trusted ETB customer information |
| **1SilverBullet** | Current Group A integration layer |
| **Partner Insurer** | Calculates offers, underwrites and issues the policy |
| **Insurer Underwriter / Medical Team** | Returns decisions and evidence requirements |
| **Group B Insurer Platform** | Hosts the controlled redirected purchase journey in S13 |
| **AU Bank Payment Gateway** | Processes payment on the customer's device and returns status |
| **Bank Communication Service** | Sends approved customer and workforce notifications |
| **Reporting / Reconciliation Systems** | Support financial and lifecycle reconciliation and outcome reporting |

## 5. Cross-actor invariants

1. Every material action has a stable actor ID and audit correlation.
2. Access is default-deny and intersects role, branch, insurer, ownership and explicit sharing.
3. Admin, management, insurer-representative and Operations roles never silently inherit RM rights.
4. Only a separately authorised and qualified RM may perform RM selling actions.
5. Customer consent, suitability evidence and declarations are never manufactured by another actor.
6. The customer completes payment on the customer's device.
7. The insurer is the source of policy issuance truth.
8. Sold requires issuance, confirmation, reconciliation and persistence.
9. Exceptions are resolved with additive evidence; original evidence is not overwritten.
10. Hybrid hand-off preserves state, ownership, permissions and attribution.
