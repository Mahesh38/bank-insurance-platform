#!/usr/bin/env python3
"""First ARB review kit — 1-hour DEV/UAT sitting (not production).

    python3 scripts/architecture/build_arb_first_review.py

Produces:
  AU-NIP-R0-ARB-First-Review-DEV-UAT-*.pptx   (presenter deck + speaker notes)
  AU-NIP-R0-ARB-First-Review-SCRIPT-*.docx/.pdf
  AU-NIP-R0-ARB-First-Review-FAQ-*.xlsx
  AU-NIP-R0-ARB-First-Review-Kit-*.zip
"""

from __future__ import annotations

import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs/architecture/arb-prerequisites/exports"
STAMP = "2026-09-14"
NAVY = (0x00, 0x33, 0x66)
GOLD = (0xC4, 0xA3, 0x5A)
WHITE = (0xFF, 0xFF, 0xFF)
INK = (0x1E, 0x29, 0x3B)

DISCLAIMER = (
    "FIRST REVIEW — DEV/UAT environment shape. Not production go-live, not DR-ready "
    "sign-off, not T4, not Board 4, not CIS register, not VA/PT."
)

OPENING = """Thank you. This is the first Architecture Review Board sitting for the Insurance Distribution Platform — NIP.

We are not here for production go-live, and we are not here for DR-ready sign-off. Those will be later, fuller sittings.

Today we ask you to review the architecture we intend to build, confirm it is aligned with bank platform standards, and allow us to stand up Dev and UAT — Dev lives inside the UAT AWS account — so engineering can proceed.

I will walk one complete sale, then the cloud and network shape, then security, identity, data, audit, and DR as designed. I will be explicit about what is already bank-standard, what is different and why, and what we are still waiting on from other teams.

If a question needs a measured answer we do not have in the room — for example an Apigee IP list — I will record it as an action rather than guess. That kind of item should not block the Dev/UAT environment shape.

Sixty minutes. Comments welcome. We will park anything that belongs to the production ARB."""

CLOSING = """To close: we are asking for a first-review observation on this architecture, and a non-objection to vendor Dev-inside-UAT and UAT VPCs attaching to the existing bank network.

We are not asking for production, DR-ready, CIS classification, or a VA/PT waiver.

Observations and improvements are welcome. Anything that needs numbers we do not have, we will take as a dated action. Thank you."""

PLAYS = [
    (
        "Play 1 — Cite and stop",
        "That decision is recorded in ADR-0XX. I will not re-litigate it from memory. Happy to walk the clause after the sitting if needed.",
    ),
    (
        "Play 2 — Name the owner",
        "The structure is Architecture's. The remaining acceptance sits with [Deepali / Shivanshi / bank network / Apigee team]. I will not speak for their residual.",
    ),
    (
        "Play 3 — Action, dated, non-blocking",
        "Fair question. I do not have a measured answer in the room and I will not invent one. Action: we return by [date] with [owner]. That item does not change the Dev/UAT VPC shape we are asking you to review today.",
    ),
    (
        "Play 4 — Wrong sitting",
        "That is a production / DR-readiness / VA-PT question. We will bring it to the second ARB with evidence. Today is the first review of the intended design so we can stand up Dev and UAT.",
    ),
    (
        "Play 5 — Never guess",
        "I will not guess an IP, a pentest result, a restore-drill time, or a CIS register class. Guessing those in this room would be the actual control failure.",
    ),
]


def svg_png(svg: Path, dest: Path, width: int) -> Path | None:
    try:
        import cairosvg
    except ImportError:
        return None
    dest.parent.mkdir(parents=True, exist_ok=True)
    cairosvg.svg2png(url=str(svg), write_to=str(dest), output_width=width)
    return dest


# ---------------------------------------------------------------------------
# Slide + FAQ content (canonical talking points; ADRs still win)
# ---------------------------------------------------------------------------

SLIDES = [
    {
        "kind": "title",
        "title": "First ARB review — Dev & UAT",
        "minutes": 3,
        "say": OPENING,
        "if_asked": "If someone asks 'is this production approval?' — No. Say it twice. Write it on the parking lot.",
        "do_not": "Do not say 'AI designed this'. Do not say 'we need production'. Do not apologise for the architecture.",
    },
    {
        "kind": "bullets",
        "title": "Why we are here — and what we are not asking",
        "minutes": 3,
        "bullets": [
            "This is the FIRST review. We are introducing NIP: a bank-owned insurance distribution platform for assisted Life.",
            "ASK TODAY: review the intended architecture; non-objection to stand up Dev (inside UAT) and UAT.",
            "NOT TODAY: production go-live · DR-ready sign-off · CIS register entry · VA/PT waiver · public ALB exception.",
            "A later, fuller ARB will cover production and DR evidence (timed restore, VA/PT, proven RTO/RPO).",
            "We want comments and improvements. Silence is not approval. A question is not a blocker unless you name it as one.",
        ],
        "say": "Please look at the two columns in your mind: today versus later. Today is design review plus Dev/UAT. Later is production and DR. If a comment is really a production control, I will park it for the second sitting so we do not spend the hour there.",
        "if_asked": "If they say 'we only ARB once' — we still cannot evidence DR or VA/PT today. We are asking for a staged review, which matches how landing zones are vended.",
        "do_not": "Do not accept 'approve everything now'. Do not accept 'come back when prod-ready' if that blocks Dev/UAT — that is the chicken-and-egg.",
    },
    {
        "kind": "table",
        "title": "60 minutes — how we will use them",
        "minutes": 1,
        "table": [
            ["Mins", "What you will see", "Why it is here"],
            ["0–8", "Ask + business + one sale", "So everyone is on the same journey"],
            ["8–25", "Front door, loading dock, accounts", "This is where Public ALB / TGW / NAT questions land"],
            ["25–40", "Security, identity, data, audit, logs, payment", "Bank NFR and trust boundaries"],
            ["40–47", "DR as designed + tech stack vs bank standard", "What is reused vs what is new"],
            ["47–55", "19 prerequisites traffic light + today's ask", "Honest holds, none should block Dev/UAT VPCs"],
            ["55–60", "Close, parking lot, actions", "Leave with a dated list, not a debate"],
        ],
        "say": "I will keep moving. Please park deep dives. Appendix slides at the back are glossary and FAQ — we open them only if needed.",
        "if_asked": "If they want to jump to network first — agree. Skip to the front-door slide. The sale trace can wait 2 minutes.",
        "do_not": "Do not read this table aloud line by line.",
    },
    {
        "kind": "bullets",
        "title": "Business — what NIP is (row 1 of the pack)",
        "minutes": 4,
        "bullets": [
            "AU Bank holds IRDAI Composite Corporate Agency licence CA0515.",
            "R0: one Relationship Manager, as certified Specified Person, sells Term or Savings/ULIP to one ETB customer of one Group A insurer — end to end.",
            "Non-bypassable: suitability (C1), customer-device OTP consent (C2), customer-device payment (C4), reconciled issuance, immutable 7-year audit.",
            "Why not redirect to an aggregator portal: we lose evidence, suitability, and the ability to replace the aggregator.",
            "Product volumes (CAP-A*) are still assumptions — Rajal. That does not change the Dev/UAT network shape.",
        ],
        "say": "This is an insurance distribution platform owned by the bank, not a 1SilverBullet skin. 1SB is a provider route we can replace. The RM never pays on the RM device. The customer pays on the customer's device. If we get that wrong, we do not have an IRDAI-safe journey.",
        "if_asked": "Health / Motor / DIY are later horizons. Do not design them in this hour. H1–H3 are direction, not this submission.",
        "do_not": "Do not invent a business BRD signature. Rajal owns Product sign-off.",
    },
    {
        "kind": "bullets",
        "title": "One sale, end to end — follow Priya",
        "minutes": 5,
        "bullets": [
            "Priya's device → Cloudflare → F5-XC → API Gateway → Internal ALB → Flutter web or NIP BFF.",
            "BFF holds tokens. Device holds an opaque session only. PDP fail-closed before regulated actions.",
            "Lead create is the only on-platform way in. CIF via bank EBS. Suitability before quote. OTP on the customer phone.",
            "Quote/proposal: Hub → 1SB adapter → Apigee → 1SB. 1SB JSON dies in the adapter.",
            "Payment link to the customer device. Callback on a separate API Gateway route. Policy only after RECONCILED + audit write.",
        ],
        "say": "Forget boxes for one minute. This trace IS the architecture. Flutter never calls 1SB, never calls RDS, never calls Apigee. Customer phone appears twice: OTP and pay. If someone proposes a shortcut that skips suitability, consent, customer-device pay, or audit-before-sold — that is not a simplification, that is a control failure.",
        "if_asked": "Why so many services? Because Health later must not rewrite Life. Quote and suitability are different systems of record. We will not merge them to look smaller on a slide.",
        "do_not": "Do not demo a god-service. Do not say Kafka is the audit log.",
    },
    {
        "kind": "bullets",
        "title": "Front door — inbound (ADR-018)  |  why not a public ALB",
        "minutes": 6,
        "bullets": [
            "Bank north-south SaaS already exists: Cloudflare Enterprise, then F5-XC WAF. We consume them. We do not clone them.",
            "First AWS hop: Amazon API Gateway (schema, throttle, no business logic) → VPC Link → Internal ALB only.",
            "No public / external ALB. No Internet Gateway on a workload VPC. You cannot curl a pod from the internet.",
            "Neighbour bank apps often enter AWS on a Public ALB. We did not copy that (Board 1 F-06). API Gateway is our AWS entry.",
            "Payment callbacks: a separate API Gateway route, IP-allowlisted to the PG — not on the RM session (TB-6).",
        ],
        "say": "This will attract the most questions. The one-line answer: a public ALB in front of API Gateway is an extra attack surface and an extra hop the insurance platform does not need. Cloudflare and F5 are already the bank perimeter, and they are SaaS — they do not sit in our VPC. Internal ALB is the only load balancer inside the VPC. If the room wants the neighbour app's Public ALB copied, that is a change to ADR-018, not a tweak. It would also require Deepali, not just Architecture.",
        "if_asked": "Use FAQ 1–3. If they insist, Play 2: Deepali owns the exposure outcome. Play 3: we will not redesign the front door in the last twenty minutes.",
        "do_not": "Do not agree to 'just a public ALB for Dev'. Dev that is public is a prod-shaped hole with synthetic data until someone puts a real CIF in it.",
    },
    {
        "kind": "picture",
        "picture": "topology",
        "title": "Where it runs — attach, do not clone",
        "minutes": 4,
        "say": "This is the R0 topology. Please notice three things. One: we attach as a spoke to the existing AU-CTO-NETWORK Transit Gateway — we do not build a second hub, a second Direct Connect, or a FortiGate pair in our VPC. Two: inbound is Cloudflare / F5 / API Gateway. Three: outbound leaves via Apigee. The picture is a rendering of the LLD. If it disagrees with an ADR, the ADR wins.",
        "if_asked": "If they cannot read the picture, go to the next two slides which are the same facts in words.",
        "do_not": "Do not apologise that it is busy. Offer the LLD after the meeting.",
        "bullets": [],
    },
    {
        "kind": "bullets",
        "title": "Loading dock — outbound (ADR-020)  |  why not NAT EIPs to 1SB",
        "minutes": 5,
        "bullets": [
            "Every call that leaves the building goes via bank Apigee: 1SB, SMS, PG session-create, AD-verify, EBS.",
            "Java never calls *.1silverbullet.tech. Adapter base URL = Apigee proxy. 1SB allowlists Apigee IPs.",
            "Internal bank APIs use Apigee private targets. Forbidden: hairpin through Cloudflare and F5.",
            "Spoke NAT Gateways still exist on the inspection path toward Apigee. Publishing those EIPs to 1SB is now the defect.",
            "Pod → TGW → Network Firewall → Apigee. Firewall remainder is Deepali's acceptance (ADR-010), not an allowlist.",
        ],
        "say": "Two different outsides. Mixing them is how CIF ends up on the public internet. Bank systems stay private. Internet partners go through Apigee. If someone says 'just whitelist our NAT IPs like last year' — that allowlists the wrong host. 1SB will see Apigee, not our NAT.",
        "if_asked": "Apigee edition and exact IPs are still with the Apigee team (DEP-20260914-apg). Play 3. Dev can use stubs; UAT needs the private path or a dated exception — Shivanshi.",
        "do_not": "Do not read out an IP. Do not promise the Apigee product exists today.",
    },
    {
        "kind": "table",
        "title": "Five accounts — Dev inside UAT — no CUG",
        "minutes": 3,
        "table": [
            ["Account", "Holds", "If compromised"],
            ["shared-services", "ECR, Terraform state pattern, runners", "Supply chain — not customer data"],
            ["security", "CloudTrail, GuardDuty, Security Hub, Config", "Detection — not the sale datastore"],
            ["network", "Inspection VPCs, attachments", "Routing — data does not live here"],
            ["uat", "vpc-dev (synthetic) + vpc-uat (masked)", "Two route tables: stubs cannot reach prod CBS"],
            ["prod", "Real ETB, real AD-verify, real 1SB", "CIS blast radius — not in scope today"],
        ],
        "say": "Bank onboarding default is Prod, CUG, UAT, with Dev inside UAT. We are not asking for a separate Dev account. We are not asking for CUG at R0 — waiver, do not provision. Isolation is two VPCs, namespaces, schemas, Valkey prefixes, Apigee products. That is how we keep cost down without mixing stub CBS and real CBS.",
        "if_asked": "If Cloud wants a split Dev account — that is a cost exception they own. We can consume it; we are not requesting it. CUG has no R0 consumer.",
        "do_not": "Do not offer to vendor prod in the same request 'while we are here'.",
    },
    {
        "kind": "bullets",
        "title": "Security architecture — seven trust boundaries",
        "minutes": 4,
        "bullets": [
            "TB-1 device→edge · TB-2 edge→BFF · TB-3 BFF→PDP · TB-4 app→data · TB-5 adapter→Apigee→1SB · TB-6 payment · TB-7 private bank.",
            "This document is Architecture's input to Board 4. Deepali signs the threat model. We are not signing it today.",
            "No PII in logs — pipeline fails. Secrets via IRSA / Secrets Manager, not in images.",
            "mTLS to 1SB is passed intact; we do not decrypt it at a firewall to 'inspect'.",
            "VA/PT is NOT-YET. SAST, ArchUnit, JaCoCo, lockfile SCA are in CI now. Pentest is a UAT-exit / production-ARB item.",
        ],
        "say": "If Security asks 'where is the VA/PT report?' the honest answer is: there is none, and we will not claim there is. Dev/UAT can be stood up with SAST in CI. Production ARB will not. Please do not hold Dev/UAT for a pentest of an environment that does not exist yet.",
        "if_asked": "Play 4 on pentest. Play 2 on ADR-010 firewall remainder.",
        "do_not": "Do not say 'we will pentest next week' unless Swapnali/Deepali have a date.",
    },
    {
        "kind": "bullets",
        "title": "Identity — IAM and PAM",
        "minutes": 3,
        "bullets": [
            "Workforce (RM): Bank AD remains source of truth (TI-01). We call the existing AD-verify API via Apigee private. Never LDAP from EKS.",
            "Partners / IPR: created in the private IdP (Keycloak is acceptable). Never in AD.",
            "NIP-APP / Fireframe is the UI chrome. Keycloak admin console is not shown to bank users.",
            "Pods: IRSA per deployable. Humans on AWS: bank IAM Identity Center / existing PAM. We consume PAM; we do not invent a second one.",
            "Flutter never talks to Keycloak or Apigee.",
        ],
        "say": "The sentence that must survive this slide: we are not binding LDAP from Kubernetes to Active Directory. That would be a new attack path the bank does not need. AD-verify is an API the bank already has.",
        "if_asked": "Password-in-NIP vs Fireframe SSO is a joint item with Deepali (ID-11). Play 2. It does not change VPC vending.",
        "do_not": "Do not offer to 'just LDAP for Dev'.",
    },
    {
        "kind": "bullets",
        "title": "Data, residency, audit vs logs",
        "minutes": 3,
        "bullets": [
            "Classes: PUBLIC / INTERNAL / CONFIDENTIAL / RESTRICTED. India only — ap-south-1 primary, ap-south-2 DR (TI-08).",
            "Aurora PostgreSQL, one cluster, schema per bounded context (ADR-008). No cross-schema grants.",
            "Audit is INSERT-only + S3 Object Lock. 7-year evidence. OpenSearch is operational logs, never evidence (ADR-013).",
            "CloudTrail (who changed AWS) and CloudWatch (how the pod behaves) are both mandatory.",
            "Cache / Kafka / search are not systems of record. Losing Valkey means re-login, not data loss.",
        ],
        "say": "Please do not let anyone treat Kibana as the audit pack. Search can be rebuilt. WORM cannot be shortened because we are in a hurry to decommission.",
        "if_asked": "CIS class is proposed Critical; Shailja + CISO register it. Architecture does not. Play 2. Dev/UAT still uses the same control set — we do not build a weaker Dev.",
        "do_not": "Do not classify the system into the bank CIS register from this slide.",
    },
    {
        "kind": "picture",
        "picture": "payment",
        "title": "Payment — customer device, not RM device (TB-6)",
        "minutes": 3,
        "say": "Session-create goes out through Apigee. The customer completes 3-D Secure on their own device. The callback comes in on a separate API Gateway route, not on Priya's session. We do not mark SOLD because a payment API returned 200. We wait for RECONCILED. If the callback is lost, state is UNCERTAIN and we block a second attempt. Timeouts do not mint money.",
        "if_asked": "PAN never on the RM device and never in our logs. PG is the bank's existing payment gateway — we do not replace it.",
        "do_not": "Do not accept 'RM can pay on behalf for UAT demo' as an architecture exception.",
        "bullets": [],
    },
    {
        "kind": "picture",
        "picture": "dr",
        "title": "DR — designed for later; not the ask today",
        "minutes": 3,
        "say": "Warm standby in Hyderabad. RTO ≤ 1 hour, transactional RPO ≤ 5 minutes, audit RPO 0. Cache, MSK and OpenSearch are not replicated — by design. A DR region with no path to CBS cannot sell. We have not run the timed restore drill. That drill is a production/DR ARB evidence item (NFR-DR-04). Today we are not claiming DR-ready.",
        "if_asked": "Why not active-active? Bank SOP language for some tiers is active-active. We do not 'fix' that without an RIA tier and Aarti/Shivanshi. Warm standby matches RTO 1 hour. Play 4.",
        "do_not": "Do not say DR is proven. Do not skip Hyderabad in the design 'to save money' without Aarti.",
        "bullets": [],
    },
    {
        "kind": "table",
        "title": "Bank standard we consume  vs  what is different",
        "minutes": 4,
        "table": [
            ["Item", "Bank standard?", "What we do"],
            ["Control Tower, IAM IC, SCPs, India pin", "Yes", "Join. Five accounts. No shadow org."],
            ["TGW, DX Gateway, EDGE FortiGate", "Yes", "Attach as spoke. Do not clone."],
            ["Cloudflare + F5-XC", "Yes", "Same north-south SaaS. Hostname for NIP."],
            ["GitLab + Terraform + EKS", "Yes", "Bank delivery path."],
            ["Public ALB as AWS entry", "Neighbour apps yes", "NO — API Gateway instead (ADR-018)."],
            ["Apigee outbound", "Bank API plane", "YES — 1SB and bank APIs (ADR-020)."],
            ["CUG environment", "Onboarding default", "NO at R0 — waiver, do not provision."],
            ["Separate Dev AWS account", "Sometimes", "NO — Dev inside UAT, two VPCs."],
        ],
        "say": "Read the red lines as the only things that might look 'new'. Public ALB is the neighbour pattern we refused. CUG we are not buying. Separate Dev account we are not asking for. Everything else is attach-don't-clone. If ARB can live with those three differences, Dev/UAT can be vended without an architecture rewrite.",
        "if_asked": "If they reject API Gateway inbound — that is a redesign, Deepali+Mahesh, not a hallway decision. Record it. Do not improvise a Public ALB 'for now'.",
        "do_not": "Do not present differences as rebellion. Present them as recorded ADRs with owners.",
    },
    {
        "kind": "table",
        "title": "Tech stack (pins) — why these, not a catalogue",
        "minutes": 3,
        "table": [
            ["Choice", "Why", "Why not"],
            ["Java 21 / Spring Boot 3.5", "Bank-familiar, LTS, in support", "Node/.NET rewrite of the same controls"],
            ["EKS", "Listed IaaS, live on this estate", "Lambda-for-everything; EC2 pets"],
            ["Aurora PostgreSQL", "One cluster, schema per context", "One database per microservice at R0"],
            ["Flutter (one project, 3 artefacts)", "RM web + APK + IPA", "Three native codebases"],
            ["MSK / outbox", "Reliable handoff, not the archive", "Kafka as 7-year audit"],
            ["Valkey (ElastiCache)", "Session / L2 / rate-limit", "Redis as SoR or idempotency"],
        ],
        "say": "We did not pick a fashion stack. We picked what the estate already runs and what keeps 1SB behind an adapter. A stack argument that does not change a control is a later conversation.",
        "if_asked": "FAQ on Lambda, Istio, Mongo, Kafka-as-audit. Play 1 with the cheat sheet.",
        "do_not": "Do not bash other teams' stacks. Do not agree to Istio 'because prod will need it'.",
    },
    {
        "kind": "table",
        "title": "19 ARB prerequisites — traffic light for THIS sitting",
        "minutes": 4,
        "table": [
            ["#", "Prerequisite", "Today", "Blocks Dev/UAT VPCs?"],
            ["1", "Business ask", "PARTIAL — Rajal volumes", "No"],
            ["2", "CIS classification", "HUMAN — proposal Critical", "No — same controls anyway"],
            ["3–5", "SAD / current-target / diagrams", "READY unsigned", "No — T4 later"],
            ["6", "Integration matrix", "READY — Apigee IPs OPEN", "Dev stubs OK; UAT dated"],
            ["7–9", "Capacity / RTO-RPO / DR design", "Designed, unproven", "No — proof is sitting 2"],
            ["10–11", "Security arch / VA-PT", "PARTIAL / NOT-YET", "No pentest of empty env"],
            ["12–15", "SBOM / data / IAM / logging", "Designed", "No"],
            ["16–19", "Third parties / RACI / lifecycle / exit", "Compiled", "No"],
        ],
        "say": "Nothing on this list is a reason to refuse two VPCs in the UAT account. Several are reasons to refuse production. Please keep those in different columns. The pack, PDF, Excel and this deck are the evidence index. Humans still sign CIS, Board 4, T4, VA/PT.",
        "if_asked": "If they want every row green before Dev — that is production-ARB logic applied too early. Play 4.",
        "do_not": "Do not mark VA/PT or CIS as done.",
    },
    {
        "kind": "bullets",
        "title": "What we need from ARB today",
        "minutes": 3,
        "bullets": [
            "1. Recorded first-review of the R0 architecture as the intended design for NIP.",
            "2. Non-objection to vendor: five accounts; UAT account with vpc-dev + vpc-uat; no CUG; no public ALB; no workload IGW.",
            "3. Confirmation we attach to existing TGW / DX Gateway / EDGE — we do not clone them.",
            "4. Observations and improvements, written. We will action them with owners and dates.",
            "5. Explicitly out of scope today: prod apply, DR drill sign-off, CIS register, VA/PT, Apigee IP numbers.",
        ],
        "say": CLOSING,
        "if_asked": "If they will only write 'approved' — ask them to qualify: 'first review, Dev/UAT vending, production ARB still required'. Unqualified 'approved' is how we get blamed later.",
        "do_not": "Do not leave without a written observation, even if it is 'no objection to Dev/UAT subject to Deepali on firewall remainder'.",
    },
    {
        "kind": "bullets",
        "title": "Parking lot — items that must not block Dev/UAT",
        "minutes": 2,
        "bullets": [
            "DEP-20260914-apg — Apigee product + IPs (Shivanshi / API platform). Dev uses stubs.",
            "DEP-20260824-dx1 — VPN then existing DX for UAT CBS. Dev stays on stubs.",
            "ADR-010 remainder — Deepali accepts pod→Apigee inspection.",
            "ID-11 — Fireframe SSO vs password-in-NIP (Deepali).",
            "CAP-A* — Rajal volumes. NFR-DR-04 drill — Shivanshi, sitting 2.",
            "Live capture in the room: ____________________________",
        ],
        "say": "I will write every new question here with an owner. If it does not change CIDRs, attachments, or 'no public ALB', it does not block vending.",
        "if_asked": "",
        "do_not": "Do not let the parking lot become a redesign workshop.",
    },
    {
        "kind": "bullets",
        "title": "Appendix — do not present unless asked",
        "minutes": 0,
        "bullets": [
            "Next slides: network glossary in plain language, then FAQ we expect.",
            "Leave-behind: PDF pack + Excel matrices + this deck.",
            "Presenter: use presenter view — every slide has speaker notes.",
            "If you do not know: Play 3. Never guess an IP or a pentest.",
        ],
        "say": "We stop the main sitting here unless you want the glossary.",
        "if_asked": "",
        "do_not": "",
    },
    {
        "kind": "table",
        "title": "Appendix A — network words in one sentence each",
        "minutes": 0,
        "table": [
            ["Term", "Plain meaning", "How NIP uses it"],
            ["Transit Gateway (TGW)", "The bank's existing network roundabout", "We attach a spoke. We do not build a second one."],
            ["Internet Gateway (IGW)", "A VPC's door to the public internet", "Only on the inspection VPC. Never on workload VPCs."],
            ["NAT Gateway", "Lets private pods start outbound with a fixed public IP", "Inspection VPC, toward Apigee. Not the 1SB allowlist."],
            ["Network Firewall (NFW)", "AWS firewall on that outbound hop", "May inspect pod→Apigee. Deepali accepts remainder."],
            ["FortiGate", "Bank hub next-gen firewall in EDGE VPC", "Already exists. We do not drop a pair in our VPC."],
            ["Internal ALB", "Load balancer with no public IP", "The only LB inside our VPC."],
            ["API Gateway", "AWS managed front door", "Inbound only. Not Apigee."],
            ["Apigee", "Bank outbound API plane", "1SB, SMS, AD-verify, EBS. Outside our VPC."],
        ],
        "say": "If a network SME quizzes you, stay on this table. You do not need to configure BGP in this meeting.",
        "if_asked": "If they go below this — Play 3, Shivanshi + bank network.",
        "do_not": "Do not draw BGP on a whiteboard unless Shivanshi is speaking.",
    },
    {
        "kind": "table",
        "title": "Appendix B — five plays when you do not know",
        "minutes": 0,
        "table": [
            ["Play", "Exact words in the room"],
            ["1 Cite and stop", "That is in ADR-0XX. I will not re-litigate it from memory."],
            ["2 Name the owner", "Structure is Architecture's. Residual sits with Deepali / Shivanshi / Apigee."],
            ["3 Action, dated, non-blocking", "I will not invent a number. Action by [date] with [owner]. Does not change vpc-dev."],
            ["4 Wrong sitting", "That is production / DR / VA-PT. We bring evidence to sitting 2."],
            ["5 Never guess", "I will not guess an IP, a pentest, a restore time, or a CIS class."],
        ],
        "say": "These five sentences are how you stay professional without blocking Dev/UAT. Memorise Play 3 and Play 5.",
        "if_asked": "",
        "do_not": "Do not read this slide in the main hour unless you are using a play live.",
    },
    {
        "kind": "table",
        "title": "Appendix C — questions we expect (answers in FAQ Excel)",
        "minutes": 0,
        "table": [
            ["They will ask", "One-line answer", "Cite"],
            ["Why not public ALB?", "API Gateway + Internal ALB. Neighbour pattern is not ours.", "ADR-018"],
            ["Why two API products?", "Inbound = API Gateway. Outbound = Apigee. Different jobs.", "ADR-020"],
            ["Give us 1SB IPs now", "I will not invent IPs. Dev uses stubs. DEP-20260914-apg.", "Play 3"],
            ["Why NFW not FortiGate in spoke?", "FortiGate already in EDGE. Do not clone.", "ADR-010"],
            ["Why Dev inside UAT?", "Cost. Two VPCs, two route tables. No CUG.", "ASM-017/018"],
            ["Where is VA/PT?", "Does not exist. Cannot pentest an unvended env.", "Pack row 11"],
            ["Is DR proven?", "No. Designed, not drilled. Sitting 2.", "NFR-DR-04"],
            ["Can RM pay on the iPad?", "No. Customer-device payment is C4, not UX.", "TB-6"],
        ],
        "say": "Flip here only if the room is circling. Full answers are in the FAQ workbook — 35 questions.",
        "if_asked": "",
        "do_not": "Do not invent a 36th answer on the whiteboard.",
    },
]


FAQ = [
    ("Why not a public / external ALB like other bank apps?",
     "Neighbour apps enter AWS on a Public ALB. NIP's AWS entry is API Gateway, then an Internal ALB only (ADR-018). Cloudflare and F5-XC already are the bank perimeter. A public ALB in front of API Gateway adds a hop and a public target group we do not need. Copying the neighbour pattern is a change to ADR-018 and a Deepali exposure decision — not a Dev convenience.",
     "ADR-018", False, "Deepali if challenged"),
    ("Why not put a public ALB in Dev only?",
     "A public Dev is a production-shaped hole. Workload VPCs have no IGW in every environment. Internal ALB + API Gateway is the same shape in Dev, UAT and Prod. We will not invent a second ingress just to go faster.",
     "ADR-018 / LLD §2", False, ""),
    ("Why API Gateway and Apigee — two API products?",
     "They do different jobs. API Gateway is the inbound AWS front door for RM/mobile and PG callbacks. Apigee is the bank outbound loading dock for 1SB and internal bank APIs (ADR-020). Flutter never calls Apigee. Java never calls 1SB. One product doing both would hairpin internal APIs through the public edge or force 1SB to allowlist the wrong IPs.",
     "ADR-020", False, ""),
    ("Why not Apigee on the front door instead of API Gateway?",
     "Human Architecture owner split the plane on 2026-09-14. Inbound stays API Gateway. Drawing Apigee on ingress would put RM traffic on the outbound plane and fight the existing Cloudflare→F5 path. We will not redesign that in this sitting.",
     "ADR-020", False, "Mahesh T4"),
    ("Why no Internet Gateway on the workload VPC?",
     "If a workload VPC has an IGW, someone will eventually attach a public IP to a pod or an ALB. Zero-trust language on the landing-zone slide means the only IGW is on the inspection/EDGE path. You cannot curl a pod from the internet. That is intentional.",
     "LLD §2 / ADR-010", False, ""),
    ("What is a Transit Gateway and why do we need it?",
     "Think of TGW as the bank's existing roundabout that already connects applications, EDGE firewalls and Direct Connect. We attach one spoke. We do not build an insurance-only roundabout. VPC peering meshes do not scale and would bypass hub inspection.",
     "Estate 18 / BE-01 / ADR-009", False, "Shivanshi + bank network"),
    ("Why not a second TGW / second Direct Connect for insurance?",
     "The bank already has AU-CTO-NETWORK TGW and DX Gateway. Cloning them is an A0 architecture defect (Board 1). Attach, do not clone (BE-01).",
     "BE-01", False, ""),
    ("What is NAT? Why do we still have NAT if we use Apigee?",
     "NAT lets private pods start outbound connections. It still sits on the inspection VPC for the hop toward Apigee and remaining internet. It is not what 1SB allowlists. 1SB allowlists Apigee's egress IPs. Publishing our NAT Elastic IPs to 1SB allowlists the wrong host (ADR-020).",
     "ADR-010 remainder / ADR-020", False, "Deepali / Apigee team"),
    ("Please give us the IP list for 1SB now.",
     "I will not invent IPs in this room. Action: Apigee team writes product + per-env IPs (DEP-20260914-apg). Dev uses stubs. That does not block vpc-dev.",
     "DEP-20260914-apg", True, "Shivanshi / API platform"),
    ("Why Network Firewall instead of a FortiGate pair in our VPC?",
     "FortiGate already exists in the hub EDGE VPC. Dropping another pair in the spoke would clone firewall operations the bank already staffs. Spoke NFW may inspect pod→Apigee; Deepali accepts that remainder. Hub FortiGate still inspects org paths.",
     "ADR-010 / ASM-012", True, "Deepali"),
    ("Why F5 Distributed Cloud, not BIG-IP in AWS?",
     "On this estate F5 is F5-XC SaaS, outside AWS. An in-VPC BIG-IP icon was a diagram mistake and was retracted in ADR-018.",
     "ADR-018", False, ""),
    ("Why Cloudflare, not CloudFront?",
     "Cloudflare Enterprise is the bank's north-south CDN/DDoS standard. CloudFront would be a second CDN the bank does not operate for this perimeter.",
     "ADR-018", False, ""),
    ("Why Internal ALB, not NLB or a public Kubernetes LoadBalancer?",
     "We need HTTP path routing: / to nip-web, /api to BFF. That is ALB. A Service type LoadBalancer with a public IP is exactly the exposure we refused.",
     "LLD §3", False, ""),
    ("Why EKS, not ECS or Lambda everywhere?",
     "EKS is listed IaaS on this estate and already used. Lambda-for-everything does not match long-running insurance journeys, private VPC-only data stores, or the service boundaries we need for a second LOB later. We are not anti-Lambda; we are anti-rewriting the platform as functions to look modern.",
     "LLD BOM / AP-09", False, ""),
    ("Why Java 21 / Spring Boot, not Node or .NET?",
     "LTS, already in the programme, Spring Boot 3.5 in support, CVE overrides pinned in Gradle. A language war does not change a trust boundary. We will not restart R0 on a new runtime.",
     "Lifecycle paper", False, "Amit"),
    ("Why not one InsuranceService monolith?",
     "Every rule change would be a release, and Health later becomes a rewrite. Suitability and quote are different systems of record. Boundaries are the point of R0, not premature micro-max.",
     "03 SAD / R0-E2E §5", False, ""),
    ("Why Flutter?",
     "One project, three artefacts: web, APK, IPA. Tokens never on the device. Three native apps would triple the control surface.",
     "R0-E2E §2", False, "Amit / channel"),
    ("Why Keycloak if we have Active Directory?",
     "AD is workforce only (TI-01). Partners must not enter AD. Keycloak (or equivalent) holds partner identities. Workforce verification is the existing AD-verify API, never LDAP from EKS. Fireframe/NIP-APP is the UI chrome.",
     "ADR-020 / TI-01", False, "Deepali ID-11"),
    ("Why not LDAP from EKS to AD? It is simpler.",
     "LDAP from a workload cluster to AD is a new credential path, a new secret, and a new blast radius. The bank already has an AD-verify API. We call it privately through Apigee.",
     "ADR-020", False, ""),
    ("Why is Dev inside UAT? That is not isolation.",
     "It is cost, with isolation by two VPCs, two route tables, namespaces, schemas and prefixes (ADR-020). Stubs cannot use the prod CBS route. A second AWS account is a Cloud exception we are not requesting.",
     "SUG-20260914-uat / ASM-017", False, "Shivanshi"),
    ("Why no CUG?",
     "No R0 consumer for CUG. Waiver, do not provision (ASM-018). We will not pay for an empty class of environment.",
     "ASM-018", False, ""),
    ("Why warm standby, not active-active?",
     "NFR RTO is 1 hour. Warm standby in ap-south-2 matches that. Active-active is a different money and data problem (RIA tier, Aarti, Shivanshi). We will not 'fix' SOP language in this hour without that evidence. DR proof is sitting 2.",
     "NFR-DR / LLD §11", True, "Aarti + Shivanshi"),
    ("Is DR proven?",
     "No. Designed, not drilled (NFR-DR-04). I will not claim a restore time we have not measured. That is exactly why this is not the production ARB.",
     "NFR-DR-04", False, "Shivanshi"),
    ("Is this a Critical Information System?",
     "Architecture proposes CIS because of CIF, KYC, health/suitability, payment orchestration and 7-year WORM audit. The bank CIS register is Shailja + CISO. We do not self-classify. Controls are already sized as if we were CIS — a lower class would be a written risk acceptance, not a cheaper Dev.",
     "CIS proposal", True, "Shailja + CISO"),
    ("Where is the VA/PT report?",
     "It does not exist. CI has Checkstyle, Spotless, ArchUnit, JaCoCo, lockfile SCA. DAST/VA/PT is before UAT exit / production ARB. We will not pentest an environment we have not been allowed to vendor.",
     "Pack row 11", False, "Deepali + Swapnali"),
    ("Why isn't OpenSearch / Kafka the audit store?",
     "Search retention is not a 7-year licence evidence. Kafka topic retention is not WORM. Audit is INSERT-only + S3 Object Lock (TI-07, ADR-013). Operational logs can be rebuilt. Evidence cannot.",
     "ADR-013 / TI-07", False, "Aarti"),
    ("Why can't the RM pay on the iPad for the customer?",
     "C4 — customer-device payment. That is regulatory, not UX. UAT demos use a customer-device step, not an RM card entry.",
     "FF-14 / TB-6", False, "Rajal / Deepali"),
    ("Why 1SB at all? Why not call insurers directly?",
     "R0 uses 1SB as a provider route behind an adapter (TI-04). Direct insurer APIs are an R1+ sibling adapter. The Hub contract does not change. We are not locked to 1SB JSON in the domain.",
     "TI-04 / ADR-020", False, ""),
    ("Are we compliant with the bank landing-zone / SOP?",
     "We attach to Control Tower, TGW, DX, EDGE, GitLab, Terraform, IAM IC, India SCPs. Differences are recorded: API Gateway inbound not Public ALB; no CUG; Dev-inside-UAT; no workload IGW. Those are ADRs, not silent deviations. Human T4 still outstanding.",
     "Estate 18 / ADR-018/020", True, "Mahesh T4 + Shivanshi"),
    ("Can we put Flutter on public S3?",
     "No. SOP: no public buckets. Authenticated app is not a brochure site. Static assets come through nip-web on the same ingress chain.",
     "LLD BOM / SOP", False, ""),
    ("Why not Istio / service mesh in R0?",
     "Not in R0. We already have NetworkPolicy, PDP, and two reverse proxies. A mesh is a new control plane with no named R0 problem (AP-09).",
     "Lifecycle 'not in R0'", False, ""),
    ("Can we start Dev without Direct Connect?",
     "Yes. Dev uses stubs for CBS/AD-verify. UAT needs VPN first, then existing DX as primary (ADR-009). Do not block vpc-dev on a carrier order.",
     "ADR-009 / DEP-20260824-dx1", False, "Bank network"),
    ("Who signs T4 Architecture?",
     "A human Mahesh, not this deck. We draft; we do not sign. Same for Deepali Board 4 and Shailja CIS.",
     "Authority card", False, "Human Mahesh"),
    ("Why Hyderabad, not another region?",
     "India residency (TI-08) plus bank DR region already in use (ap-south-2). Data does not leave India.",
     "TI-08 / FF-08", False, ""),
    ("What if ARB wants a full production pack today?",
     "We cannot evidence timed DR, VA/PT, or CIS registration today. Forcing those now either invents evidence or delays Dev/UAT indefinitely. We ask for a staged review. Production ARB will be scheduled with that evidence.",
     "This sitting's ask", False, "Kalpana to schedule sitting 2"),
]


def first_run(p):
    return p.runs[0] if p.runs else p.add_run()


def build_pptx(path: Path, pngs: dict) -> None:
    from lxml import etree
    from pptx import Presentation
    from pptx.dml.color import RGBColor
    from pptx.enum.shapes import MSO_SHAPE
    from pptx.enum.text import PP_ALIGN
    from pptx.oxml.ns import qn
    from pptx.util import Inches, Pt

    prs = Presentation()
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)
    blank = prs.slide_layouts[6]
    navy, gold, white, ink = RGBColor(*NAVY), RGBColor(*GOLD), RGBColor(*WHITE), RGBColor(*INK)

    def set_run(run, text, size=14, bold=False, color=ink):
        run.text = text
        run.font.size = Pt(size)
        run.font.bold = bold
        run.font.color.rgb = color
        run.font.name = "Calibri"

    def fill(shape, color):
        shape.fill.solid()
        shape.fill.fore_color.rgb = color
        shape.line.fill.background()

    def header(slide, title):
        bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(0.78))
        fill(bar, navy)
        acc = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0.78), Inches(13.333), Inches(0.06))
        fill(acc, gold)
        box = slide.shapes.add_textbox(Inches(0.35), Inches(0.16), Inches(12.6), Inches(0.52))
        set_run(first_run(box.text_frame.paragraphs[0]), title, 20, True, white)

    def footer(slide, i, n):
        bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(7.22), Inches(13.333), Inches(0.28))
        fill(bar, navy)
        box = slide.shapes.add_textbox(Inches(0.3), Inches(7.24), Inches(11.3), Inches(0.24))
        set_run(first_run(box.text_frame.paragraphs[0]), DISCLAIMER[:120], 8, False, white)
        num = slide.shapes.add_textbox(Inches(11.7), Inches(7.24), Inches(1.4), Inches(0.24))
        num.text_frame.paragraphs[0].alignment = PP_ALIGN.RIGHT
        set_run(first_run(num.text_frame.paragraphs[0]), f"{i} / {n}", 8, False, white)

    def notes(slide, spec):
        ns = slide.notes_slide.notes_text_frame
        mins = spec.get("minutes") or 0
        parts = [
            f"TIMEBOX: {mins} min" if mins else "APPENDIX — open only if asked",
            "",
            "SAY:",
            spec.get("say") or "",
        ]
        if spec.get("if_asked"):
            parts += ["", "IF ASKED:", spec["if_asked"]]
        if spec.get("do_not"):
            parts += ["", "DO NOT:", spec["do_not"]]
        ns.text = "\n".join(parts)

    def add_table(slide, rows, l, t, w, h, font=12):
        shp = slide.shapes.add_table(len(rows), len(rows[0]), Inches(l), Inches(t), Inches(w), Inches(h))
        tbl = shp.table
        for r, row in enumerate(rows):
            for c, val in enumerate(row):
                cell = tbl.cell(r, c)
                cell.text = ""
                p = cell.text_frame.paragraphs[0]
                color = white if r == 0 else ink
                set_run(first_run(p), val, font, r == 0, color)
                cell.text_frame.word_wrap = True
                tcPr = cell._tc.get_or_add_tcPr()
                fl = etree.SubElement(tcPr, qn("a:solidFill"))
                srgb = etree.SubElement(fl, qn("a:srgbClr"))
                if r == 0:
                    srgb.set("val", "003366")
                elif r % 2:
                    srgb.set("val", "F1F5F9")
                else:
                    srgb.set("val", "FFFFFF")
        return shp

    def bullets(slide, items, size=16):
        box = slide.shapes.add_textbox(Inches(0.45), Inches(1.05), Inches(12.4), Inches(5.95))
        tf = box.text_frame
        tf.word_wrap = True
        for i, item in enumerate(items):
            p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
            p.space_after = Pt(10)
            set_run(first_run(p), item, size, False, ink)

    built = []
    # title
    s = prs.slides.add_slide(blank)
    bg = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(7.5))
    fill(bg, RGBColor(*NAVY))
    fill(s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(2.5), Inches(13.333), Inches(0.08)), gold)
    box = s.shapes.add_textbox(Inches(0.7), Inches(1.15), Inches(12), Inches(1.2))
    set_run(first_run(box.text_frame.paragraphs[0]), "Architecture Review Board — first sitting", 18, False, gold)
    box = s.shapes.add_textbox(Inches(0.7), Inches(2.7), Inches(12), Inches(1.8))
    tf = box.text_frame
    tf.word_wrap = True
    set_run(first_run(tf.paragraphs[0]), "AU Bank Insurance Distribution Platform (NIP)", 26, True, white)
    p = tf.add_paragraph()
    set_run(first_run(p), "R0 assisted Life  ·  Dev & UAT environment review", 20, False, white)
    box = s.shapes.add_textbox(Inches(0.7), Inches(4.7), Inches(12), Inches(2.2))
    tf = box.text_frame
    tf.word_wrap = True
    set_run(first_run(tf.paragraphs[0]), "Not production. Not DR-ready. Not a CIS register entry. Not VA/PT.", 16, False, gold)
    p = tf.add_paragraph()
    set_run(first_run(p), "Ask: review the intended architecture and allow Dev-inside-UAT + UAT to be stood up.", 16, False, white)
    p = tf.add_paragraph()
    set_run(first_run(p), f"60 minutes  ·  {STAMP}  ·  Draft: Mahesh, Board 1 / R2", 14, False, white)
    notes(s, SLIDES[0])
    built.append(s)

    for spec in SLIDES[1:]:
        s = prs.slides.add_slide(blank)
        header(s, spec["title"])
        if spec["kind"] == "bullets":
            bullets(s, spec.get("bullets") or [])
        elif spec["kind"] == "table":
            add_table(s, spec["table"], 0.3, 1.0, 12.7, 5.95, font=11 if len(spec["table"][0]) > 3 else 13)
        elif spec["kind"] == "picture":
            png = pngs.get(spec["picture"])
            if png:
                s.shapes.add_picture(str(png), Inches(0.3), Inches(0.95), Inches(12.7), Inches(6.1))
            else:
                bullets(s, ["Diagram unavailable in this build — see R0-LLD renderings."])
        notes(s, spec)
        built.append(s)

    n = len(built)
    for i, slide in enumerate(built, 1):
        if i == 1:
            num = slide.shapes.add_textbox(Inches(11.7), Inches(7.15), Inches(1.4), Inches(0.24))
            num.text_frame.paragraphs[0].alignment = PP_ALIGN.RIGHT
            set_run(first_run(num.text_frame.paragraphs[0]), f"{i} / {n}", 9, False, white)
        else:
            footer(slide, i, n)
    prs.save(str(path))


def build_faq_xlsx(path: Path) -> None:
    from openpyxl import Workbook
    from openpyxl.styles import Alignment, Font, PatternFill, Border, Side
    from openpyxl.utils import get_column_letter

    wb = Workbook()
    ws = wb.active
    ws.title = "FAQ"
    header = ["#", "Likely question", "Answer to use in the room", "Cite", "May defer?", "Owner if deferred"]
    fill = PatternFill("solid", fgColor="003366")
    hfont = Font(name="Calibri", bold=True, color="FFFFFF", size=11)
    wrap = Alignment(wrap_text=True, vertical="top")
    thin = Border(
        left=Side(style="thin", color="CBD5E1"),
        right=Side(style="thin", color="CBD5E1"),
        top=Side(style="thin", color="CBD5E1"),
        bottom=Side(style="thin", color="CBD5E1"),
    )
    for c, h in enumerate(header, 1):
        cell = ws.cell(1, c, h)
        cell.fill, cell.font, cell.alignment, cell.border = fill, hfont, wrap, thin
    for i, (q, a, cite, defer, owner) in enumerate(FAQ, 1):
        row = [i, q, a, cite, "Yes — Play 3" if defer else "No — answer from ADR", owner]
        for c, val in enumerate(row, 1):
            cell = ws.cell(i + 1, c, val)
            cell.alignment, cell.border = wrap, thin
            cell.font = Font(name="Calibri", size=10)
    widths = [6, 42, 78, 22, 22, 28]
    for i, w in enumerate(widths, 1):
        ws.column_dimensions[get_column_letter(i)].width = w
    ws.freeze_panes = "A2"
    ws.auto_filter.ref = ws.dimensions
    ws.row_dimensions[1].height = 22
    for r in range(2, len(FAQ) + 2):
        ws.row_dimensions[r].height = 72

    plays = wb.create_sheet("Deferral_plays")
    plays["A1"], plays["B1"] = "Play", "Exact words"
    plays["A1"].fill = plays["B1"].fill = fill
    plays["A1"].font = plays["B1"].font = hfont
    for i, (name, words) in enumerate(PLAYS, 2):
        plays.cell(i, 1, name).alignment = wrap
        plays.cell(i, 2, words).alignment = wrap
        plays.row_dimensions[i].height = 48
    plays.column_dimensions["A"].width = 28
    plays.column_dimensions["B"].width = 100
    ws2 = wb.create_sheet("Opening_and_close")
    ws2["A1"] = "Opening (memorise)"
    ws2["A2"] = OPENING
    ws2["A4"] = "Close (memorise)"
    ws2["A5"] = CLOSING
    ws2["A2"].alignment = ws2["A5"].alignment = wrap
    ws2.column_dimensions["A"].width = 110
    ws2.row_dimensions[2].height = 160
    ws2.row_dimensions[5].height = 80
    wb.save(path)


def build_script_docx(path: Path) -> None:
    from docx import Document
    from docx.shared import Inches, Pt, RGBColor
    from docx.enum.text import WD_ALIGN_PARAGRAPH

    doc = Document()
    sec = doc.sections[0]
    sec.page_width, sec.page_height = Inches(8.27), Inches(11.69)
    sec.left_margin = sec.right_margin = Inches(0.7)

    doc.add_heading("NIP first ARB review — presenter script", 0)
    p = doc.add_paragraph()
    r = p.add_run(DISCLAIMER)
    r.bold = True
    r.font.color.rgb = RGBColor(185, 28, 28)
    doc.add_paragraph(
        "Use PowerPoint presenter view. Each slide's notes match this document. "
        "Do not walk in with Markdown. Print this Word/PDF and keep the FAQ Excel beside you."
    )
    doc.add_heading("Before you enter (5 minutes)", 1)
    for line in [
        "Open the First-Review PPTX, not the 19-row walk-in deck. That walk-in is the leave-behind.",
        "Print the FAQ Excel or keep it on a second screen.",
        "Write today's date and 'Shivanshi / Deepali / Apigee team' on a notepad for Play 3.",
        "Decide the return date you will offer (for example 'five working days') before you are asked.",
        "Never say you used AI to invent the architecture. Say: design is recorded in ADR-018 and ADR-020, LLD and SAD.",
        "If you freeze on a network term, go to Appendix A in the deck. Read the one-sentence row.",
    ]:
        doc.add_paragraph(line, style="List Number")

    doc.add_heading("Opening — 60 seconds (memorise)", 1)
    doc.add_paragraph(OPENING)
    doc.add_heading("Five professional plays when you do not know", 1)
    for name, words in PLAYS:
        doc.add_heading(name, 2)
        doc.add_paragraph(words)

    doc.add_heading("Slide-by-slide", 1)
    t = 0
    for i, spec in enumerate(SLIDES, 1):
        mins = spec.get("minutes") or 0
        t += mins
        heading = f"Slide {i}. {spec['title']}"
        if mins:
            heading += f"  ({mins} min · running {t})"
        else:
            heading += "  (appendix)"
        doc.add_heading(heading, 2)
        if spec.get("bullets"):
            for b in spec["bullets"]:
                doc.add_paragraph(b, style="List Bullet")
        doc.add_paragraph().add_run("SAY").bold = True
        doc.add_paragraph(spec.get("say") or "")
        if spec.get("if_asked"):
            doc.add_paragraph().add_run("IF ASKED").bold = True
            doc.add_paragraph(spec["if_asked"])
        if spec.get("do_not"):
            doc.add_paragraph().add_run("DO NOT").bold = True
            doc.add_paragraph(spec["do_not"])

    doc.add_heading("Close — 45 seconds (memorise)", 1)
    doc.add_paragraph(CLOSING)
    doc.add_heading("FAQ (same as the Excel)", 1)
    for i, (q, a, cite, defer, owner) in enumerate(FAQ, 1):
        doc.add_heading(f"Q{i}. {q}", 2)
        doc.add_paragraph(a)
        meta = f"Cite: {cite}."
        if defer:
            meta += f" You may defer (Play 3) to {owner or 'named owner'}."
        else:
            meta += " Answer from the ADR — do not defer this one."
        doc.add_paragraph(meta)

    doc.core_properties.title = "NIP first ARB — presenter script"
    doc.core_properties.author = "Mahesh — Principal Insurance Platform Architect (draft)"
    doc.save(str(path))


def build_script_pdf(path: Path) -> None:
    from reportlab.lib import colors
    from reportlab.lib.pagesizes import A4
    from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
    from reportlab.lib.units import mm
    from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer

    navy = colors.HexColor("#003366")
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(name="H", parent=styles["Heading1"], textColor=navy, fontSize=13, spaceBefore=8, spaceAfter=4))
    styles.add(ParagraphStyle(name="H2", parent=styles["Heading2"], textColor=navy, fontSize=11, spaceBefore=6, spaceAfter=3))
    styles.add(ParagraphStyle(name="B", parent=styles["Normal"], fontSize=9, leading=12, spaceAfter=4))
    styles.add(ParagraphStyle(name="W", parent=styles["Normal"], textColor=colors.HexColor("#B91C1C"), fontSize=9, leading=12, spaceAfter=6))

    def P(text, style="B"):
        return Paragraph(text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>"), styles[style])

    story = [
        P("NIP first ARB review — presenter script", "H"),
        P(DISCLAIMER, "W"),
        P("Opening (memorise)", "H2"),
        P(OPENING),
        P("Five plays when you do not know", "H2"),
    ]
    for name, words in PLAYS:
        story.append(P(name, "H2"))
        story.append(P(words))
    t = 0
    for i, spec in enumerate(SLIDES, 1):
        mins = spec.get("minutes") or 0
        t += mins
        label = f"Slide {i}. {spec['title']}" + (f" ({mins} min)" if mins else " (appendix)")
        story.append(P(label, "H2"))
        story.append(P("SAY: " + (spec.get("say") or "")))
        if spec.get("if_asked"):
            story.append(P("IF ASKED: " + spec["if_asked"]))
        if spec.get("do_not"):
            story.append(P("DO NOT: " + spec["do_not"]))
    story.append(P("Close (memorise)", "H2"))
    story.append(P(CLOSING))
    story.append(P("FAQ", "H"))
    for i, (q, a, cite, defer, owner) in enumerate(FAQ, 1):
        story.append(P(f"Q{i}. {q}", "H2"))
        story.append(P(a))
        extra = "Defer OK. " + (owner or "") if defer else "Do not defer — answer from the ADR."
        story.append(P(f"Cite: {cite}. {extra}"))

    def on_page(canvas, doc_):
        canvas.saveState()
        canvas.setFillColor(navy)
        canvas.rect(0, A4[1] - 10 * mm, A4[0], 10 * mm, fill=1, stroke=0)
        canvas.setFillColor(colors.white)
        canvas.setFont("Helvetica-Bold", 8)
        canvas.drawString(12 * mm, A4[1] - 6.5 * mm, "NIP first ARB — presenter script (not production approval)")
        canvas.setFillColor(navy)
        canvas.rect(0, 0, A4[0], 8 * mm, fill=1, stroke=0)
        canvas.setFillColor(colors.white)
        canvas.setFont("Helvetica", 7)
        canvas.drawString(12 * mm, 3 * mm, "AI-DRAFTED. First review Dev/UAT. HA-02: ADR/LLD/NFR win.")
        canvas.drawRightString(A4[0] - 12 * mm, 3 * mm, str(doc_.page))
        canvas.restoreState()

    doc = SimpleDocTemplate(
        str(path),
        pagesize=A4,
        leftMargin=14 * mm,
        rightMargin=14 * mm,
        topMargin=16 * mm,
        bottomMargin=12 * mm,
        title="NIP first ARB presenter script",
        author="Mahesh (draft)",
    )
    doc.build(story, onFirstPage=on_page, onLaterPages=on_page)


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    tmp = OUT / ".generated-png-first"
    pngs = {
        "topology": svg_png(ROOT / "docs/architecture/r0-platform-topology.svg", tmp / "topology.png", 1800),
        "payment": svg_png(ROOT / "docs/architecture/r0-platform-payment.svg", tmp / "payment.png", 1600),
        "dr": svg_png(ROOT / "docs/architecture/r0-platform-dr.svg", tmp / "dr.png", 1600),
    }
    files = {
        "pptx": OUT / f"AU-NIP-R0-ARB-First-Review-DEV-UAT-{STAMP}.pptx",
        "script_docx": OUT / f"AU-NIP-R0-ARB-First-Review-SCRIPT-{STAMP}.docx",
        "script_pdf": OUT / f"AU-NIP-R0-ARB-First-Review-SCRIPT-{STAMP}.pdf",
        "faq": OUT / f"AU-NIP-R0-ARB-First-Review-FAQ-{STAMP}.xlsx",
        "zip": OUT / f"AU-NIP-R0-ARB-First-Review-Kit-{STAMP}.zip",
    }
    build_pptx(files["pptx"], pngs)
    build_script_docx(files["script_docx"])
    build_script_pdf(files["script_pdf"])
    build_faq_xlsx(files["faq"])
    with zipfile.ZipFile(files["zip"], "w", zipfile.ZIP_DEFLATED) as zf:
        for key in ("pptx", "script_docx", "script_pdf", "faq"):
            zf.write(files[key], files[key].name)
    if tmp.exists():
        shutil.rmtree(tmp)
    print("wrote:")
    for p in files.values():
        print(f"  {p.relative_to(ROOT)}  ({p.stat().st_size:,} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
