#!/usr/bin/env python3
"""First ARB sitting — visual 35-minute walk (problem → outcome → how).

    python3 scripts/architecture/build_arb_first_review.py

Talk is ~35 minutes so a 60-minute slot has time for the board.
Slides are billboards. Speaker notes and the FAQ workbook hold the density.
HA-02: ADR / LLD / NFR still win.
"""

from __future__ import annotations

import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs/architecture/arb-prerequisites/exports"
STAMP = "2026-09-14"

import sys as _sys
_sys.path.insert(0, str(Path(__file__).resolve().parent))
from arb_first_review_visuals import render_all  # noqa: E402

NAVY = (0x00, 0x33, 0x66)
GOLD = (0xC4, 0xA3, 0x5A)
WHITE = (0xFF, 0xFF, 0xFF)
INK = (0x1E, 0x29, 0x3B)
MUTED = (0x57, 0x65, 0x7A)
ICE = (0xF4, 0xF7, 0xFB)
TEAL = (0x0E, 0x74, 0x90)
LINE = (0xE2, 0xE8, 0xF0)

DISCLAIMER = "First review · Dev & UAT · not production · not DR-ready · not VA/PT"

OPENING = """Good morning. This is the first architecture review of the Insurance Distribution Platform.

Today we walk the intended design: the problem, one sale as it will actually run, the runtime, and the stack — so Dev and UAT can be stood up.

We are not claiming production evidence. Working logs, security operations, compliance proofs and a proven day-one estate are what we bring after UAT, to a later sitting. A longer copy of this deck is not that sitting.

I will take about thirty-five minutes. The rest of the hour is yours."""

CLOSING = """Please record this as the intended R0 design and allow Dev and UAT on this shape. When UAT has run, we return with evidence — logging, security ops, compliance, services on the path — not with a thicker version of these pictures. I will stop here so we can discuss."""

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
        "Play 4 — Later sitting",
        "That is a production, DR-readiness or VA-PT question. We will bring evidence to the second ARB. Today is the intended design, so Dev and UAT can be stood up.",
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
# Talk slides — short copy on the glass; notes carry the argument
# ---------------------------------------------------------------------------

TALK = [
    {
        "id": "title",
        "kind": "title",
        "minutes": 1,
        "say": OPENING,
        "if_asked": "If someone asks whether this is production approval — no. Say it once, park it, keep walking.",
        "do_not": "Do not say the architecture was designed by AI. Do not claim logs, VA/PT or DR drills as done.",
    },
    {
        "id": "sittings",
        "kind": "hero",
        "hero": "sittings",
        "title": "Two sittings — design now, evidence later",
        "minutes": 2,
        "say": "Left is today: intended design, Dev and UAT, your comments. Right is after UAT: a production sitting with evidence that logging is capturing, security ops are running, compliance is in the pack, and the day-one services actually work. We will not pretend those proofs exist this morning.",
        "if_asked": "If they want one ARB for everything — we still cannot evidence a pentest of an unvended estate. Staged review matches how landing zones are vended.",
        "do_not": "Do not promise a production date. Kalpana schedules sitting two after UAT.",
    },
    {
        "id": "problem",
        "kind": "hero",
        "hero": "problem",
        "title": "Why this platform exists",
        "minutes": 2,
        "say": "Four pictures, one point: a licensed corporate agent must own the evidence of a Life sale. Suitability on the RM glass, OTP and premium on the customer phone, a policy only when money and issuance agree. Redirecting the customer to an aggregator site does not produce that pack.",
        "if_asked": "Health, Motor and DIY are later horizons. Do not design them here.",
        "do_not": "Do not invent a Product signature.",
    },
    {
        "id": "journey",
        "kind": "hero",
        "hero": "journey",
        "title": "How one sale actually runs",
        "minutes": 5,
        "say": "Walk the ribbon left to right. The RM stands with NIP-APP. Suitability is a gate on that device — no quote without it. Consent and premium are the customer's phone, not the iPad. Quote and proposal go Hub then adapter. Policy is last, and only after reconciled. The gold line is an orchestrated saga: Journey Orchestration is the one record of where the sale is. Flutter never calls the aggregator, the database, or Apigee.",
        "if_asked": "Why not one InsuranceService? Health later must not rewrite Life. Quote and suitability are different systems of record.",
        "do_not": "Do not accept RM-pays-for-the-customer as a UAT exception. Do not call Kafka the audit log.",
    },
    {
        "id": "inbound",
        "kind": "hero",
        "hero": "inbound",
        "title": "How a session reaches the application",
        "minutes": 3,
        "say": "You cannot curl a pod from the internet. If asked why not a public ALB — it would put a public target in front of a front door we already have. ADR-018. That is a Security exposure decision, not a Dev convenience.",
        "if_asked": "FAQ on public ALB. Play 2 if they insist.",
        "do_not": "Do not agree to a public ALB just for Dev.",
    },
    {
        "id": "outbound",
        "kind": "hero",
        "hero": "outbound",
        "title": "How the platform calls a partner",
        "minutes": 2,
        "say": "The adapter base URL is the Apigee proxy, never the aggregator host. I will not read IPs. That action does not block vpc-dev.",
        "if_asked": "Play 3, DEP-20260914-apg. Dev uses stubs.",
        "do_not": "Do not invent IPs.",
    },
    {
        "id": "topology",
        "kind": "picture",
        "picture": "topology",
        "title": "Where a request runs",
        "minutes": 2,
        "say": "Attach as a spoke. Inbound as you saw. Outbound through Apigee. I will pause here if you want the drawing; otherwise I protect discussion time. If this picture disagrees with an ADR, the ADR wins.",
        "if_asked": "Previous two slides are the same facts in sequence.",
        "do_not": "Do not walk every box.",
    },
    {
        "id": "envs",
        "kind": "hero",
        "hero": "envs",
        "title": "How Dev and UAT are isolated",
        "minutes": 1,
        "say": "Five accounts. Dev is a VPC inside UAT, not a sixth account. Stubs cannot ride a production CBS route. Production is drawn so you see blast radius — we are not vending it today.",
        "if_asked": "Split Dev account is a Cloud exception we are not requesting.",
        "do_not": "Do not offer to vendor production while we are here.",
    },
    {
        "id": "services",
        "kind": "hero",
        "hero": "services",
        "title": "What we run — edge, AWS, operate",
        "minutes": 3,
        "say": "Four clusters, not a shopping list. Edge is Cloudflare, F5-XC, Apigee — they do not sit in our VPC. Compute is EKS behind API Gateway and an internal ALB. Data is Aurora PostgreSQL — one cluster, schema per context — not a public RDS. Events on MSK, fed by an outbox. Keys in KMS, secrets in Secrets Manager, telemetry in CloudWatch and CloudTrail. OpenSearch is operational search, not the audit pack.",
        "if_asked": "Why not RDS? Aurora PostgreSQL is the recorded pin (ADR-008). Why not self-managed Kafka on EKS? MSK is the managed backbone; the outbox remains source of truth.",
        "do_not": "Do not add Istio, Cognito, or a warehouse to look more complete. They are not R0.",
    },
    {
        "id": "stack",
        "kind": "hero",
        "hero": "stack",
        "title": "How we build it",
        "minutes": 3,
        "say": "Experience is Flutter — one project, web, APK, IPA. Services are Java 21 and Spring Boot 3.5 on EKS. Coordination is an orchestrated saga, not a choreography: Journey Orchestration is queryable when a sale stops. Outbox then MSK. 1SB JSON dies in the adapter. Tokens never reach the glass.",
        "if_asked": "They said Jetty — we run Java 21 LTS. Spring Boot 3.5 is the BOM in repo. Saga is recorded in the domain model §5, not a slide invention.",
        "do_not": "Do not start a language war. Do not claim a saga framework product — it is the Journey service plus state, not a third-party BPM.",
    },
    {
        "id": "micro",
        "kind": "hero",
        "hero": "micro",
        "title": "How the services sit",
        "minutes": 2,
        "say": "Five lanes. Flutter talks only to the BFF. The sale spine owns lead, saga, suitability, consent. Product owns catalogue, quote, proposal. Fulfilment owns payment, policy, WORM audit. Integration is Hub, 1SB adapter, Apigee. That is why Health later does not rewrite Life.",
        "if_asked": "Not every name is a pod today. R0 is the assisted Life cut of these lanes.",
        "do_not": "Do not list twenty class names. Stay on the lanes.",
    },
    {
        "id": "payment",
        "kind": "picture",
        "picture": "payment",
        "title": "How premium is collected",
        "minutes": 2,
        "say": "Session-create through Apigee. Customer completes 3-D Secure on their phone. Callback on a separate API Gateway route. Sold waits for reconciled. Uncertain blocks a second attempt. Timeouts do not mint money.",
        "if_asked": "PAN never on the RM device, never in logs.",
        "do_not": "Do not accept RM pays on behalf for a demo.",
    },
    {
        "id": "dr",
        "kind": "kpis",
        "title": "Continuity — designed, not yet drilled",
        "minutes": 1,
        "kpis": [
            ("≤ 1 h", "Recovery time"),
            ("≤ 5 min", "Transactional RPO"),
            ("0", "Audit RPO"),
        ],
        "caption": "Warm standby in Hyderabad. Proof is a timed drill — later sitting.",
        "picture": "dr",
        "say": "Designed, not measured. That is why this is not the production ARB.",
        "if_asked": "Play 4. NFR-DR-04.",
        "do_not": "Do not claim DR is proven.",
    },
    {
        "id": "ask",
        "kind": "ask",
        "title": "Decision requested",
        "minutes": 2,
        "cards": [
            ("Review", "Record this as the intended R0 design."),
            ("Proceed", "Allow Dev and UAT on this shape — two VPCs in the UAT account."),
            ("Return", "Production sitting after UAT, with evidence, not with a thicker deck."),
        ],
        "later": "After UAT — logs, security ops, compliance pack, VA/PT, timed restore, day-one services",
        "say": CLOSING,
        "if_asked": "If they write only 'approved', qualify: first review, Dev/UAT vending, production ARB still required.",
        "do_not": "Do not leave without a written observation.",
    },
]

APPENDIX = [
    {
        "id": "glossary",
        "kind": "glossary",
        "title": "Appendix — network terms",
        "minutes": 0,
        "rows": [
            ("Transit Gateway", "The estate roundabout. We attach a spoke."),
            ("Internet gateway", "Public door of a VPC. Not on the workload."),
            ("NAT gateway", "Lets private pods start outbound. Not the 1SB allowlist."),
            ("Network Firewall", "Inspection on the outbound hop. Security accepts remainder."),
            ("Internal ALB", "The only load balancer inside the VPC."),
            ("API Gateway", "Inbound AWS front door. Not Apigee."),
            ("Apigee", "Outbound API plane for 1SB and bank APIs."),
        ],
        "say": "Open only if a network SME quizzes terms. You do not need BGP in this sitting.",
        "if_asked": "Below this table — Play 3, with SRE and bank network.",
        "do_not": "Do not draw BGP on a whiteboard unless SRE is speaking.",
    },
    {
        "id": "plays",
        "kind": "plays",
        "title": "Appendix — if you do not have the number",
        "minutes": 0,
        "say": "Memorise Play 3 and Play 5. Put them on a card in your pocket.",
        "if_asked": "",
        "do_not": "Do not present this slide in the main walk.",
    },
]


FAQ = [
    ("Why not a public / external ALB like other bank apps?",
     "NIP's AWS entry is API Gateway, then an Internal ALB only (ADR-018). A public ALB in front of that front door adds a public target we do not need. Changing it is a Security exposure decision — not a Dev convenience.",
     "ADR-018", False, "Deepali if challenged"),
    ("Why not put a public ALB in Dev only?",
     "A public Dev is a production-shaped hole. The workload has no internet gateway in every environment. We will not invent a second ingress to go faster.",
     "ADR-018 / LLD §2", False, ""),
    ("Why API Gateway and Apigee — two API products?",
     "They do different jobs. API Gateway is inbound for RM/mobile and payment callbacks. Apigee is outbound for 1SB and internal bank APIs (ADR-020). One product doing both hairpins internal APIs through the public edge or allowlists the wrong IPs at 1SB.",
     "ADR-020", False, ""),
    ("Why not Apigee on the front door instead of API Gateway?",
     "Inbound stays API Gateway. Drawing Apigee on ingress puts RM traffic on the outbound plane. We will not redesign that in this sitting.",
     "ADR-020", False, "Mahesh T4"),
    ("Why no Internet Gateway on the workload VPC?",
     "If a workload VPC has an IGW, someone will eventually attach a public IP. The only IGW is on the inspection path. You cannot curl a pod from the internet.",
     "LLD §2 / ADR-010", False, ""),
    ("What is a Transit Gateway and why do we need it?",
     "The estate roundabout that already connects applications, edge firewalls and Direct Connect. We attach one spoke. We do not build an insurance-only hub.",
     "Estate 18 / BE-01 / ADR-009", False, "Shivanshi + bank network"),
    ("Why not a second TGW / second Direct Connect for insurance?",
     "The estate already has them. Cloning them is an architecture defect. Attach, do not clone (BE-01).",
     "BE-01", False, ""),
    ("What is NAT? Why do we still have NAT if we use Apigee?",
     "NAT lets private pods start outbound. It still sits on the inspection path toward Apigee. It is not what 1SB allowlists. 1SB allowlists Apigee's egress IPs (ADR-020).",
     "ADR-010 remainder / ADR-020", False, "Deepali / Apigee team"),
    ("Please give us the IP list for 1SB now.",
     "I will not invent IPs in this room. Action: Apigee team writes product + per-env IPs (DEP-20260914-apg). Dev uses stubs. That does not block vpc-dev.",
     "DEP-20260914-apg", True, "Shivanshi / API platform"),
    ("Why Network Firewall instead of a FortiGate pair in our VPC?",
     "FortiGate already exists in the hub edge VPC. Dropping another pair in the spoke clones firewall operations. Spoke NFW may inspect pod→Apigee; Security accepts that remainder.",
     "ADR-010 / ASM-012", True, "Deepali"),
    ("Why F5 Distributed Cloud, not BIG-IP in AWS?",
     "On this estate F5 is F5-XC SaaS, outside AWS. An in-VPC BIG-IP icon was a diagram mistake and was retracted in ADR-018.",
     "ADR-018", False, ""),
    ("Why Cloudflare, not CloudFront?",
     "Cloudflare Enterprise is the north-south CDN/DDoS standard for this perimeter. CloudFront would be a second CDN for the same job.",
     "ADR-018", False, ""),
    ("Why Internal ALB, not NLB or a public Kubernetes LoadBalancer?",
     "We need HTTP path routing: / to nip-web, /api to BFF. That is ALB. A Service type LoadBalancer with a public IP is the exposure we refused.",
     "LLD §3", False, ""),
    ("Why EKS, not ECS or Lambda everywhere?",
     "EKS is listed IaaS on this estate. Lambda-for-everything does not match long-running insurance journeys or private VPC-only data stores. We are not anti-Lambda; we are anti-rewriting the platform as functions.",
     "LLD BOM / AP-09", False, ""),
    ("Why Java 21 / Spring Boot, not Node or .NET?",
     "LTS, already in the programme, Spring Boot 3.5 in support. A language war does not change a trust boundary. We will not restart R0 on a new runtime.",
     "Lifecycle paper", False, "Amit"),
    ("Is that Jetty 21?",
     "No. The runtime pin is Java 21 LTS with Spring Boot 3.5. Jetty is not the recorded server pin.",
     "Lifecycle paper / Gradle BOM", False, "Amit"),
    ("Are you using Postgres RDS?",
     "Aurora PostgreSQL — one cluster, schema per bounded context (ADR-008). Not a public RDS instance, not one database per microservice at R0.",
     "ADR-008 / LLD BOM", False, "Aarti"),
    ("Why Saga? Is this a BPM product?",
     "Orchestrated saga owned by Journey Orchestration (#9). Not choreography, not a third-party BPM. One queryable record of where the sale is and why it stopped. Domain model §5.",
     "01-domain-model §5 / HLD 2.6", False, ""),
    ("When do we see production logging and security evidence?",
     "After UAT. This sitting is intended design for Dev/UAT. Sitting two brings working logs, security ops, compliance pack, VA/PT and a timed restore. We will not invent those proofs today.",
     "This sitting's ask", False, "Kalpana to schedule sitting 2"),
    ("Why not one InsuranceService monolith?",
     "Every rule change would be a release, and Health later becomes a rewrite. Suitability and quote are different systems of record.",
     "03 SAD / R0-E2E §5", False, ""),
    ("Why Flutter?",
     "One project, three artefacts: web, APK, IPA. Tokens never on the device. Three native apps would triple the control surface.",
     "R0-E2E §2", False, "Amit / channel"),
    ("Why Keycloak if we have Active Directory?",
     "AD is workforce only (TI-01). Partners must not enter AD. Workforce verification is the AD-verify API, never LDAP from EKS.",
     "ADR-020 / TI-01", False, "Deepali ID-11"),
    ("Why not LDAP from EKS to AD? It is simpler.",
     "LDAP from a workload cluster to AD is a new credential path and a new blast radius. The AD-verify API already exists. We call it privately through Apigee.",
     "ADR-020", False, ""),
    ("Why is Dev inside UAT? That is not isolation.",
     "Isolation is two VPCs, two route tables, namespaces, schemas and prefixes. Stubs cannot use the production CBS route. A second AWS account is a Cloud exception we are not requesting.",
     "SUG-20260914-uat / ASM-017", False, "Shivanshi"),
    ("Why no CUG?",
     "No R0 consumer for CUG. Waiver, do not provision (ASM-018).",
     "ASM-018", False, ""),
    ("Why warm standby, not active-active?",
     "NFR RTO is 1 hour. Warm standby in ap-south-2 matches that. Active-active is a different money and data problem. DR proof is sitting 2.",
     "NFR-DR / LLD §11", True, "Aarti + Shivanshi"),
    ("Is DR proven?",
     "No. Designed, not drilled (NFR-DR-04). I will not claim a restore time we have not measured.",
     "NFR-DR-04", False, "Shivanshi"),
    ("Is this a Critical Information System?",
     "Architecture proposes CIS because of CIF, KYC, suitability, payment orchestration and 7-year WORM. The register is Compliance + CISO. We do not self-classify.",
     "CIS proposal", True, "Shailja + CISO"),
    ("Where is the VA/PT report?",
     "It does not exist. CI has Checkstyle, Spotless, ArchUnit, JaCoCo, lockfile SCA. DAST/VA/PT is before UAT exit / production ARB. We will not pentest an environment we have not been allowed to vendor.",
     "Pack row 11", False, "Deepali + Swapnali"),
    ("Why isn't OpenSearch / Kafka the audit store?",
     "Search retention is not a 7-year licence evidence. Kafka topic retention is not WORM. Audit is INSERT-only + S3 Object Lock (TI-07, ADR-013).",
     "ADR-013 / TI-07", False, "Aarti"),
    ("Why can't the RM pay on the iPad for the customer?",
     "Customer-device payment is a control, not a UX preference. UAT demos use a customer-device step.",
     "FF-14 / TB-6", False, "Rajal / Deepali"),
    ("Why 1SB at all? Why not call insurers directly?",
     "R0 uses 1SB as a provider route behind an adapter (TI-04). Direct insurer APIs are a later sibling adapter. The Hub contract does not change.",
     "TI-04 / ADR-020", False, ""),
    ("Are we compliant with the landing-zone / SOP?",
     "We attach to Control Tower, TGW, DX, EDGE, GitLab, Terraform, IAM IC, India SCPs. Differences are recorded ADRs: API Gateway inbound; no CUG; Dev-inside-UAT; no workload IGW. Human T4 still outstanding.",
     "Estate 18 / ADR-018/020", True, "Mahesh T4 + Shivanshi"),
    ("Can we put Flutter on public S3?",
     "No. No public buckets. Authenticated app is not a brochure site. Static assets come through nip-web on the same ingress chain.",
     "LLD BOM / SOP", False, ""),
    ("Why not Istio / service mesh in R0?",
     "Not in R0. We already have NetworkPolicy, PDP, and two reverse proxies. A mesh is a new control plane with no named R0 problem.",
     "Lifecycle 'not in R0'", False, ""),
    ("Can we start Dev without Direct Connect?",
     "Yes. Dev uses stubs. UAT needs VPN first, then existing DX as primary (ADR-009). Do not block vpc-dev on a carrier order.",
     "ADR-009 / DEP-20260824-dx1", False, "Bank network"),
    ("Who signs T4 Architecture?",
     "A human Mahesh, not this deck. We draft; we do not sign. Same for Security Board 4 and Compliance CIS.",
     "Authority card", False, "Human Mahesh"),
    ("Why Hyderabad, not another region?",
     "India residency (TI-08) plus the DR region already in use (ap-south-2). Data does not leave India.",
     "TI-08 / FF-08", False, ""),
    ("What if ARB wants a full production pack today?",
     "We cannot evidence timed DR, VA/PT, or CIS registration today. Forcing those now either invents evidence or delays Dev/UAT indefinitely. We ask for a staged review.",
     "This sitting's ask", False, "Kalpana to schedule sitting 2"),
]


def first_run(p):
    return p.runs[0] if p.runs else p.add_run()


def build_pptx(path: Path, pngs: dict) -> int:
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
    muted, ice, teal, line = (
        RGBColor(*MUTED), RGBColor(*ICE), RGBColor(*TEAL), RGBColor(*LINE),
    )

    def rgb(tup):
        return RGBColor(*tup)

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

    def fill_line(shape, color, line_color=None):
        shape.fill.solid()
        shape.fill.fore_color.rgb = color
        if line_color is None:
            shape.line.fill.background()
        else:
            shape.line.color.rgb = line_color
            shape.line.width = Pt(1)

    def anchor_ctr(tf):
        body = tf._txBody.find(qn("a:bodyPr"))
        if body is not None:
            body.set("anchor", "ctr")

    def write_shape(shape, parts, align=PP_ALIGN.LEFT, pad=0.08):
        tf = shape.text_frame
        tf.word_wrap = True
        tf.margin_left = Inches(pad)
        tf.margin_right = Inches(pad)
        tf.margin_top = Inches(0.06)
        tf.margin_bottom = Inches(0.06)
        for i, part in enumerate(parts):
            text, size, bold, color = part
            p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
            p.alignment = align
            p.space_after = Pt(4)
            set_run(first_run(p), text, size, bold, color)
        return tf

    def header(slide, title):
        bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(0.72))
        fill(bar, navy)
        acc = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0.72), Inches(13.333), Inches(0.07))
        fill(acc, gold)
        box = slide.shapes.add_textbox(Inches(0.4), Inches(0.14), Inches(12.5), Inches(0.48))
        set_run(first_run(box.text_frame.paragraphs[0]), title, 22, True, white)

    def footer(slide, i, n):
        bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(7.22), Inches(13.333), Inches(0.28))
        fill(bar, navy)
        box = slide.shapes.add_textbox(Inches(0.35), Inches(7.24), Inches(11.2), Inches(0.24))
        set_run(first_run(box.text_frame.paragraphs[0]), DISCLAIMER, 9, False, white)
        num = slide.shapes.add_textbox(Inches(11.6), Inches(7.24), Inches(1.45), Inches(0.24))
        num.text_frame.paragraphs[0].alignment = PP_ALIGN.RIGHT
        set_run(first_run(num.text_frame.paragraphs[0]), f"{i}  /  {n}", 9, False, gold)

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

    def card(slide, l, t, w, h, fill_c=white, line_c=line):
        shp = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(l), Inches(t), Inches(w), Inches(h))
        fill_line(shp, fill_c, line_c)
        shp.adjustments[0] = 0.08
        return shp

    def badge(slide, l, t, text, fill_c=gold, font=12):
        ov = slide.shapes.add_shape(MSO_SHAPE.OVAL, Inches(l), Inches(t), Inches(0.42), Inches(0.42))
        fill(ov, fill_c)
        write_shape(ov, [(text, font, True, white)], PP_ALIGN.CENTER, 0.02)
        anchor_ctr(ov.text_frame)

    def caption_bar(slide, text):
        bar = card(slide, 0.4, 6.55, 12.5, 0.52, ice, line)
        write_shape(bar, [(text, 13, False, muted)], PP_ALIGN.LEFT, 0.16)
        anchor_ctr(bar.text_frame)

    def flow(slide, items, top=1.7):
        n = len(items)
        left, right = 0.35, 12.98
        arrow_w, gap = 0.26, 0.12
        usable = right - left
        box_w = (usable - (n - 1) * (gap + arrow_w)) / n
        x = left
        for i, (title, sub) in enumerate(items):
            shp = card(slide, x, top, box_w, 1.55, white, teal)
            write_shape(
                shp,
                [(title, 15, True, navy), (sub, 11, False, muted)],
                PP_ALIGN.CENTER,
                0.08,
            )
            anchor_ctr(shp.text_frame)
            if i < n - 1:
                ar = slide.shapes.add_shape(
                    MSO_SHAPE.RIGHT_ARROW,
                    Inches(x + box_w + 0.02),
                    Inches(top + 0.58),
                    Inches(arrow_w),
                    Inches(0.38),
                )
                fill(ar, gold)
            x += box_w + gap + arrow_w

    def steps(slide, items):
        n = len(items)
        left, right = 0.4, 12.95
        gap = 0.18
        w = (right - left - (n - 1) * gap) / n
        x = left
        for num, title, sub in items:
            shp = card(slide, x, 1.55, w, 3.55, white, line)
            badge(slide, x + w / 2 - 0.28, 1.78, num, teal, 14)
            tb = slide.shapes.add_textbox(Inches(x + 0.08), Inches(2.4), Inches(w - 0.16), Inches(2.4))
            tf = tb.text_frame
            tf.word_wrap = True
            p = tf.paragraphs[0]
            p.alignment = PP_ALIGN.CENTER
            set_run(first_run(p), title, 16, True, navy)
            p2 = tf.add_paragraph()
            p2.alignment = PP_ALIGN.CENTER
            set_run(first_run(p2), sub, 12, False, muted)
            x += w + gap

    slides_out = []

    # --- title ---
    spec = TALK[0]
    s = prs.slides.add_slide(blank)
    fill(s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(7.5)), navy)
    fill(s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(0.18), Inches(7.5)), gold)
    box = s.shapes.add_textbox(Inches(0.7), Inches(1.55), Inches(11.8), Inches(0.4))
    set_run(first_run(box.text_frame.paragraphs[0]), "ARCHITECTURE REVIEW BOARD  ·  FIRST SITTING", 14, False, gold)
    box = s.shapes.add_textbox(Inches(0.7), Inches(2.05), Inches(11.8), Inches(1.6))
    tf = box.text_frame
    tf.word_wrap = True
    set_run(first_run(tf.paragraphs[0]), "Insurance Distribution Platform", 32, True, white)
    p = tf.add_paragraph()
    set_run(first_run(p), "Assisted Life  ·  intended design for Dev and UAT", 18, False, white)
    # three chips
    chips = [("35 min", "Walkthrough"), ("25 min", "Discussion"), ("Not today", "Production")]
    x = 0.7
    for a, b in chips:
        shp = card(s, x, 4.35, 3.5, 1.15, rgb((0x00, 0x2A, 0x55)), gold)
        write_shape(shp, [(a, 22, True, gold), (b, 13, False, white)], PP_ALIGN.CENTER, 0.1)
        anchor_ctr(shp.text_frame)
        x += 3.75
    notes(s, spec)
    slides_out.append(s)

    for spec in TALK[1:] + APPENDIX:
        s = prs.slides.add_slide(blank)
        fill(s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(7.5)), ice)
        kind = spec["kind"]
        if kind not in ("hero", "picture"):
            header(s, spec["title"])

        if kind == "clock":
            # two large clocks
            for (l, val, lab, col) in (
                (1.1, "35", "minutes to walk the design", teal),
                (7.2, "25", "minutes for the board", gold),
            ):
                ov = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(l), Inches(1.15), Inches(5.0), Inches(5.0))
                fill_line(ov, white, col)
                write_shape(
                    ov,
                    [(val, 72, True, col), (lab, 16, False, muted)],
                    PP_ALIGN.CENTER,
                    0.2,
                )
                anchor_ctr(ov.text_frame)

        elif kind == "cards":
            coords = [(0.4, 1.05), (6.85, 1.05), (0.4, 3.85), (6.85, 3.85)]
            for (l, t), (num, title, body) in zip(coords, spec["cards"]):
                shp = card(s, l, t, 6.1, 2.55)
                badge(s, l + 0.22, t + 0.22, num, teal, 11)
                tb = s.shapes.add_textbox(Inches(l + 0.78), Inches(t + 0.22), Inches(5.05), Inches(2.15))
                tf = tb.text_frame
                tf.word_wrap = True
                set_run(first_run(tf.paragraphs[0]), title, 18, True, navy)
                p = tf.add_paragraph()
                p.space_before = Pt(8)
                set_run(first_run(p), body, 14, False, ink)

        elif kind == "outcome":
            shp = card(s, 0.4, 1.15, 12.5, 2.35, white, gold)
            write_shape(shp, [(spec["headline"], 20, True, navy)], PP_ALIGN.LEFT, 0.28)
            anchor_ctr(shp.text_frame)
            pills = spec["pills"]
            n = len(pills)
            gap = 0.16
            w = (12.5 - (n - 1) * gap) / n
            x = 0.4
            for pill in pills:
                pshp = card(s, x, 3.85, w, 2.35, teal, teal)
                write_shape(pshp, [(pill, 15, True, white)], PP_ALIGN.CENTER, 0.1)
                anchor_ctr(pshp.text_frame)
                x += w + gap

        elif kind == "steps":
            steps(s, spec["steps"])
            caption_bar(s, spec["caption"])

        elif kind == "flow":
            flow(s, spec["items"], top=2.15)
            caption_bar(s, spec["caption"])

        elif kind == "hero":
            key = spec.get("hero")
            png = pngs.get(key) if key else None
            png_path = Path(png) if png else None
            if png_path is not None and png_path.exists():
                s.shapes.add_picture(str(png_path), Inches(0), Inches(0), Inches(13.333), Inches(7.22))
            else:
                tb = s.shapes.add_textbox(Inches(0.5), Inches(2.5), Inches(12), Inches(1))
                set_run(
                    first_run(tb.text_frame.paragraphs[0]),
                    f"Illustration missing ({key}).",
                    16,
                    False,
                    muted,
                )
                raise FileNotFoundError(f"hero illustration missing: {key} -> {png}")
        elif kind == "picture":
            png = pngs.get(spec.get("picture"))
            if png:
                s.shapes.add_picture(str(png), Inches(0), Inches(0), Inches(13.333), Inches(7.22))
            else:
                tb = s.shapes.add_textbox(Inches(0.5), Inches(2.5), Inches(12), Inches(1))
                set_run(first_run(tb.text_frame.paragraphs[0]), "Diagram unavailable in this build — see R0-LLD.", 16, False, muted)

        elif kind == "envs":
            # five account cards; UAT is wide with two inner rooms
            labels = [
                (0.4, 1.2, 2.3, "Shared", "Images, state, runners", False),
                (2.85, 1.2, 2.3, "Security", "Trail, detection, config", False),
                (5.3, 1.2, 2.3, "Network", "Inspection, attachments", False),
                (10.55, 1.2, 2.4, "Production", "Out of scope today", True),
            ]
            for l, t, w, title, sub, dim in labels:
                shp = card(s, l, t, w, 2.4, rgb((0xE2, 0xE8, 0xF0)) if dim else white, line)
                write_shape(shp, [(title, 16, True, muted if dim else navy), (sub, 12, False, muted)], PP_ALIGN.CENTER, 0.1)
                anchor_ctr(shp.text_frame)
            uat = card(s, 0.4, 3.85, 12.5, 2.55, white, teal)
            write_shape(uat, [("UAT account", 14, True, teal)], PP_ALIGN.LEFT, 0.2)
            # two inner rooms
            for l, title, sub in (
                (0.7, "vpc-dev", "Synthetic  ·  stubs"),
                (6.85, "vpc-uat", "Masked  ·  bank UAT path"),
            ):
                inner = card(s, l, 4.45, 5.7, 1.7, ice, teal)
                write_shape(inner, [(title, 18, True, navy), (sub, 13, False, muted)], PP_ALIGN.CENTER, 0.1)
                anchor_ctr(inner.text_frame)

        elif kind == "three":
            w = 4.0
            x = 0.4
            for title, bullets in spec["cols"]:
                shp = card(s, x, 1.05, w, 5.95)
                head = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(x), Inches(1.05), Inches(w), Inches(0.62))
                fill(head, navy)
                write_shape(head, [(title, 16, True, white)], PP_ALIGN.CENTER, 0.08)
                anchor_ctr(head.text_frame)
                tb = s.shapes.add_textbox(Inches(x + 0.2), Inches(1.85), Inches(w - 0.4), Inches(4.9))
                tf = tb.text_frame
                tf.word_wrap = True
                for i, b in enumerate(bullets):
                    p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
                    p.space_after = Pt(14)
                    set_run(first_run(p), "▸  " + b, 13, False, ink)
                x += w + 0.22

        elif kind == "kpis":
            x = 0.4
            for val, lab in spec["kpis"]:
                ov = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x), Inches(1.15), Inches(3.55), Inches(3.55))
                fill_line(ov, white, teal)
                write_shape(ov, [(val, 36, True, teal), (lab, 14, False, muted)], PP_ALIGN.CENTER, 0.1)
                anchor_ctr(ov.text_frame)
                x += 3.75
            png = pngs.get(spec.get("picture"))
            if png:
                s.shapes.add_picture(str(png), Inches(0.4), Inches(4.85), Inches(12.5), Inches(2.2))
            caption = spec.get("caption")
            if caption:
                tb = s.shapes.add_textbox(Inches(0.45), Inches(4.85), Inches(12.4), Inches(0.5))
                # caption sits above picture if we have both — put caption as overlay bar
            # place caption as a thin bar over the top of the picture area if picture exists
            if spec.get("caption") and not png:
                caption_bar(s, spec["caption"])

        elif kind == "ask":
            coords = [(0.4, 1.1), (4.7, 1.1), (9.0, 1.1)]
            for (l, t), (title, body) in zip(coords, spec["cards"]):
                shp = card(s, l, t, 3.95, 4.35)
                head = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(l), Inches(t), Inches(3.95), Inches(0.7))
                fill(head, teal)
                write_shape(head, [(title, 18, True, white)], PP_ALIGN.CENTER, 0.08)
                anchor_ctr(head.text_frame)
                tb = s.shapes.add_textbox(Inches(l + 0.22), Inches(t + 1.0), Inches(3.5), Inches(3.0))
                tf = tb.text_frame
                tf.word_wrap = True
                set_run(first_run(tf.paragraphs[0]), body, 16, False, ink)
            later = card(s, 0.4, 5.65, 12.5, 1.35, rgb((0xFE, 0xF3, 0xC7)), gold)
            write_shape(later, [(spec["later"], 16, True, navy)], PP_ALIGN.CENTER, 0.16)
            anchor_ctr(later.text_frame)

        elif kind == "glossary":
            y = 1.05
            for term, meaning in spec["rows"]:
                tcard = card(s, 0.4, y, 3.3, 0.72, navy, navy)
                write_shape(tcard, [(term, 13, True, white)], PP_ALIGN.CENTER, 0.08)
                anchor_ctr(tcard.text_frame)
                mcard = card(s, 3.85, y, 9.05, 0.72, white, line)
                write_shape(mcard, [(meaning, 14, False, ink)], PP_ALIGN.LEFT, 0.18)
                anchor_ctr(mcard.text_frame)
                y += 0.82

        elif kind == "plays":
            y = 1.05
            for name, words in PLAYS:
                tcard = card(s, 0.4, y, 3.5, 1.05, teal, teal)
                write_shape(tcard, [(name.replace("Play ", ""), 13, True, white)], PP_ALIGN.CENTER, 0.08)
                anchor_ctr(tcard.text_frame)
                mcard = card(s, 4.05, y, 8.85, 1.05, white, line)
                write_shape(mcard, [(words, 12, False, ink)], PP_ALIGN.LEFT, 0.16)
                anchor_ctr(mcard.text_frame)
                y += 1.15

        notes(s, spec)
        slides_out.append(s)

    n = len(slides_out)
    for i, slide in enumerate(slides_out, 1):
        if i == 1:
            num = slide.shapes.add_textbox(Inches(11.6), Inches(7.15), Inches(1.45), Inches(0.24))
            num.text_frame.paragraphs[0].alignment = PP_ALIGN.RIGHT
            set_run(first_run(num.text_frame.paragraphs[0]), f"{i}  /  {n}", 10, False, gold)
        else:
            footer(slide, i, n)
    prs.save(str(path))
    return n


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
        row = [i, q, a, cite, "Yes — Play 3" if defer else "No — answer from the ADR", owner]
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

    timing = wb.create_sheet("How_to_run_the_hour")
    timing["A1"] = "The slot is 60 minutes. You talk ~35. The board talks ~25."
    timing["A1"].font = Font(name="Calibri", bold=True, size=14, color="003366")
    timing["A3"] = "Do not fill the hour. After the topology picture, pause once. After Decision requested, stop talking."
    timing["A5"] = OPENING
    timing["A7"] = CLOSING
    timing["A5"].alignment = timing["A7"].alignment = Alignment(wrap_text=True, vertical="top")
    timing.column_dimensions["A"].width = 110
    timing.row_dimensions[5].height = 140
    timing.row_dimensions[7].height = 80
    wb.save(path)


def build_script_docx(path: Path) -> None:
    from docx import Document
    from docx.shared import Inches, RGBColor

    doc = Document()
    sec = doc.sections[0]
    sec.page_width, sec.page_height = Inches(8.27), Inches(11.69)
    sec.left_margin = sec.right_margin = Inches(0.7)

    doc.add_heading("NIP first ARB — 35-minute walk", 0)
    p = doc.add_paragraph()
    r = p.add_run("Talk ~35 minutes. Leave ~25 for the board. Do not fill the hour.")
    r.bold = True
    r.font.color.rgb = RGBColor(185, 28, 28)
    doc.add_paragraph(
        "Use presenter view. Slides are billboards — do not read them. "
        "The argument is in these notes. Print this and keep the FAQ Excel beside you."
    )
    doc.add_heading("Before you enter", 1)
    for line in [
        "Open the First-Review PPTX, not the 19-row leave-behind.",
        "Write a return date (for example five working days) on a card for Play 3.",
        "Never say the architecture was invented by AI. Cite ADR-018, ADR-020, LLD, SAD.",
        "If a network term appears, skip to the appendix row. Do not bluff BGP.",
        "After the topology picture, pause. After Decision requested, stop.",
    ]:
        doc.add_paragraph(line, style="List Number")

    doc.add_heading("Opening — 45 seconds", 1)
    doc.add_paragraph(OPENING)
    doc.add_heading("Five plays when you do not know", 1)
    for name, words in PLAYS:
        doc.add_heading(name, 2)
        doc.add_paragraph(words)

    doc.add_heading("Slide-by-slide", 1)
    t = 0
    for i, spec in enumerate(TALK + APPENDIX, 1):
        mins = spec.get("minutes") or 0
        t += mins
        heading = f"Slide {i}. {spec.get('title', 'Title')}"
        heading += f"  ({mins} min · running {t})" if mins else "  (appendix)"
        doc.add_heading(heading, 2)
        doc.add_paragraph().add_run("SAY").bold = True
        doc.add_paragraph(spec.get("say") or "")
        if spec.get("if_asked"):
            doc.add_paragraph().add_run("IF ASKED").bold = True
            doc.add_paragraph(spec["if_asked"])
        if spec.get("do_not"):
            doc.add_paragraph().add_run("DO NOT").bold = True
            doc.add_paragraph(spec["do_not"])

    doc.add_heading("Close — 30 seconds", 1)
    doc.add_paragraph(CLOSING)
    doc.add_heading("FAQ (same as the Excel — for the 25-minute discussion)", 1)
    for i, (q, a, cite, defer, owner) in enumerate(FAQ, 1):
        doc.add_heading(f"Q{i}. {q}", 2)
        doc.add_paragraph(a)
        meta = f"Cite: {cite}."
        if defer:
            meta += f" You may defer (Play 3) to {owner or 'named owner'}."
        else:
            meta += " Answer from the ADR — do not defer this one."
        doc.add_paragraph(meta)

    doc.core_properties.title = "NIP first ARB — 35-minute walk"
    doc.core_properties.author = "Mahesh — Principal Insurance Platform Architect (draft)"
    doc.save(str(path))


def build_script_pdf(path: Path) -> None:
    from reportlab.lib import colors
    from reportlab.lib.pagesizes import A4
    from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
    from reportlab.lib.units import mm
    from reportlab.platypus import Paragraph, SimpleDocTemplate

    navy = colors.HexColor("#003366")
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(name="H", parent=styles["Heading1"], textColor=navy, fontSize=13, spaceBefore=8, spaceAfter=4))
    styles.add(ParagraphStyle(name="H2", parent=styles["Heading2"], textColor=navy, fontSize=11, spaceBefore=6, spaceAfter=3))
    styles.add(ParagraphStyle(name="B", parent=styles["Normal"], fontSize=9, leading=12, spaceAfter=4))
    styles.add(ParagraphStyle(name="W", parent=styles["Normal"], textColor=colors.HexColor("#B91C1C"), fontSize=9, leading=12, spaceAfter=6))

    def P(text, style="B"):
        return Paragraph(text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>"), styles[style])

    story = [
        P("NIP first ARB — 35-minute walk", "H"),
        P("Talk ~35 minutes. Leave ~25 for the board. Do not fill the hour.", "W"),
        P("Opening (45 seconds)", "H2"),
        P(OPENING),
        P("Five plays when you do not know", "H2"),
    ]
    for name, words in PLAYS:
        story.append(P(name, "H2"))
        story.append(P(words))
    t = 0
    for i, spec in enumerate(TALK + APPENDIX, 1):
        mins = spec.get("minutes") or 0
        t += mins
        label = f"Slide {i}. {spec.get('title', 'Title')}" + (f" ({mins} min · running {t})" if mins else " (appendix)")
        story.append(P(label, "H2"))
        story.append(P("SAY: " + (spec.get("say") or "")))
        if spec.get("if_asked"):
            story.append(P("IF ASKED: " + spec["if_asked"]))
        if spec.get("do_not"):
            story.append(P("DO NOT: " + spec["do_not"]))
    story.append(P("Close (30 seconds)", "H2"))
    story.append(P(CLOSING))
    story.append(P("FAQ — for the discussion window", "H"))
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
        canvas.drawString(12 * mm, A4[1] - 6.5 * mm, "NIP first ARB — 35-minute walk (not production approval)")
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
        title="NIP first ARB 35-minute walk",
        author="Mahesh (draft)",
    )
    doc.build(story, onFirstPage=on_page, onLaterPages=on_page)


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    tmp = Path("/tmp/arb-first-review-pngs")
    if tmp.exists():
        shutil.rmtree(tmp)
    tmp.mkdir(parents=True, exist_ok=True)
    pngs = render_all(tmp / "scenes")
    pngs.update(
        {
            "topology": svg_png(ROOT / "docs/architecture/r0-platform-topology.svg", tmp / "topology.png", 1800),
            "payment": svg_png(ROOT / "docs/architecture/r0-platform-payment.svg", tmp / "payment.png", 1600),
            "dr": svg_png(ROOT / "docs/architecture/r0-platform-dr.svg", tmp / "dr.png", 1600),
        }
    )
    print("png map:", {k: str(v) if v else None for k, v in pngs.items()})
    missing = [k for k, v in pngs.items() if not v or not Path(v).exists()]
    if missing:
        raise FileNotFoundError(f"generated PNG missing: {missing}")
    files = {
        "pptx": OUT / f"AU-NIP-R0-ARB-First-Review-DEV-UAT-{STAMP}.pptx",
        "script_docx": OUT / f"AU-NIP-R0-ARB-First-Review-SCRIPT-{STAMP}.docx",
        "script_pdf": OUT / f"AU-NIP-R0-ARB-First-Review-SCRIPT-{STAMP}.pdf",
        "faq": OUT / f"AU-NIP-R0-ARB-First-Review-FAQ-{STAMP}.xlsx",
        "zip": OUT / f"AU-NIP-R0-ARB-First-Review-Kit-{STAMP}.zip",
    }
    n = build_pptx(files["pptx"], pngs)
    media = [name for name in zipfile.ZipFile(files["pptx"]).namelist() if name.startswith("ppt/media/")]
    if len(media) < 12:
        raise RuntimeError(f"expected >=12 embedded pictures, got {len(media)}: {media}")
    build_script_docx(files["script_docx"])
    build_script_pdf(files["script_pdf"])
    build_faq_xlsx(files["faq"])
    with zipfile.ZipFile(files["zip"], "w", zipfile.ZIP_DEFLATED) as zf:
        for key in ("pptx", "script_docx", "script_pdf", "faq"):
            zf.write(files[key], files[key].name)
    talk_min = sum(s.get("minutes") or 0 for s in TALK)
    print(f"slides={n} talk_minutes={talk_min} media={len(media)}")
    print("wrote:")
    for p in files.values():
        print(f"  {p.relative_to(ROOT)}  ({p.stat().st_size:,} bytes)")
    print(f"scenes kept at {tmp}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
