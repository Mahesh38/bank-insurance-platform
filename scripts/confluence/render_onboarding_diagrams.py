#!/usr/bin/env python3
"""Render the Confluence onboarding teaching diagrams as self-contained SVG.

These pictures are communication artefacts (SUG-20261005-cfp). They own no
decision. Where a diagram disagrees with the BRD, HLD, ADR or CURRENT-STATE,
the document wins (HA-02).

    python3 scripts/confluence/render_onboarding_diagrams.py
"""
from __future__ import annotations

import html
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs/confluence/onboarding/diagrams"

NAVY = "#0B1F3A"
BLUE = "#1B4F8A"
TEAL = "#0E7C7B"
GOLD = "#C9A227"
ROSE = "#9B3D4A"
GREEN = "#2E7D4F"
SLATE = "#4A5560"
INK = "#1A2330"
MUTED = "#5C6770"
LINE = "#C5CDD6"
BG = "#F6F8FA"
WHITE = "#FFFFFF"
SOFT = "#EEF3F8"
ROSE_BG = "#F8EEEE"
GREEN_BG = "#EEF6F1"
GOLD_BG = "#FBF6E8"
TEAL_BG = "#EAF6F5"
FONT = "DejaVu Sans, Liberation Sans, sans-serif"


def esc(text: object) -> str:
    return html.escape(str(text), quote=True)


def wrap(text: str, width: int) -> list[str]:
    words = text.split()
    lines: list[str] = []
    cur = ""
    for word in words:
        trial = word if not cur else f"{cur} {word}"
        if len(trial) <= width:
            cur = trial
        else:
            if cur:
                lines.append(cur)
            cur = word
    if cur:
        lines.append(cur)
    return lines or [""]


class Canvas:
    def __init__(self, width: int, height: int, title: str):
        self.width = width
        self.height = height
        self.title = title
        self.parts: list[str] = []

    def raw(self, snippet: str) -> None:
        self.parts.append(snippet)

    def rect(
        self,
        x: float,
        y: float,
        w: float,
        h: float,
        fill: str = WHITE,
        stroke: str = LINE,
        sw: float = 1.5,
        r: float = 10,
        opacity: float = 1.0,
    ) -> None:
        self.raw(
            f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{r}" '
            f'fill="{fill}" stroke="{stroke}" stroke-width="{sw}" opacity="{opacity}"/>'
        )

    def text(
        self,
        x: float,
        y: float,
        content: str,
        size: int = 13,
        fill: str = INK,
        weight: str = "500",
        anchor: str = "start",
        italic: bool = False,
    ) -> None:
        style = "italic" if italic else "normal"
        self.raw(
            f'<text x="{x:.1f}" y="{y:.1f}" fill="{fill}" font-size="{size}" '
            f'font-weight="{weight}" text-anchor="{anchor}" font-style="{style}" '
            f'font-family="{FONT}">{esc(content)}</text>'
        )

    def multilines(
        self,
        x: float,
        y: float,
        lines: list[str],
        size: int = 12,
        fill: str = INK,
        weight: str = "500",
        anchor: str = "middle",
        leading: float = 16,
    ) -> None:
        for i, line in enumerate(lines):
            self.text(x, y + i * leading, line, size=size, fill=fill, weight=weight, anchor=anchor)

    def arrow(
        self,
        x1: float,
        y1: float,
        x2: float,
        y2: float,
        color: str = BLUE,
        dashed: bool = False,
        sw: float = 1.8,
        marker: str = "url(#arrow)",
    ) -> None:
        dash = ' stroke-dasharray="6 5"' if dashed else ""
        self.raw(
            f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" '
            f'stroke="{color}" stroke-width="{sw}" marker-end="{marker}"{dash}/>'
        )

    def path(self, d: str, color: str = BLUE, sw: float = 1.8, dashed: bool = False, marker: str = "url(#arrow)") -> None:
        dash = ' stroke-dasharray="6 5"' if dashed else ""
        marker_attr = f' marker-end="{marker}"' if marker and marker != "none" else ""
        self.raw(
            f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{sw}"{marker_attr}{dash}/>'
        )

    def badge(self, x: float, y: float, w: float, h: float, label: str, fill: str, text: str = WHITE) -> None:
        self.rect(x, y, w, h, fill=fill, stroke=fill, r=6, sw=0)
        self.text(x + w / 2, y + h / 2 + 4, label, size=11, fill=text, weight="700", anchor="middle")

    def save(self, name: str) -> Path:
        OUT.mkdir(parents=True, exist_ok=True)
        path = OUT / name
        svg = f'''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="{self.width}" height="{self.height}"
     viewBox="0 0 {self.width} {self.height}" role="img" aria-labelledby="title">
  <title id="title">{esc(self.title)}</title>
  <defs>
    <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
      <path d="M 0 0 L 10 5 L 0 10 z" fill="{BLUE}"/>
    </marker>
    <marker id="arrow-rose" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
      <path d="M 0 0 L 10 5 L 0 10 z" fill="{ROSE}"/>
    </marker>
    <marker id="arrow-teal" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
      <path d="M 0 0 L 10 5 L 0 10 z" fill="{TEAL}"/>
    </marker>
    <marker id="arrow-green" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
      <path d="M 0 0 L 10 5 L 0 10 z" fill="{GREEN}"/>
    </marker>
    <marker id="arrow-navy" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
      <path d="M 0 0 L 10 5 L 0 10 z" fill="{NAVY}"/>
    </marker>
  </defs>
  <rect width="100%" height="100%" fill="{BG}"/>
  {"".join(self.parts)}
</svg>
'''
        path.write_text(svg, encoding="utf-8")
        return path


def header(c: Canvas, title: str, subtitle: str) -> None:
    c.rect(0, 0, c.width, 64, fill=NAVY, stroke=NAVY, r=0, sw=0)
    c.text(28, 28, title, size=18, fill=WHITE, weight="700")
    c.text(28, 50, subtitle, size=12, fill="#D7E3F2", weight="400")


def footer(c: Canvas, note: str) -> None:
    c.text(28, c.height - 16, note, size=11, fill=MUTED, weight="400")


def box_card(c: Canvas, x, y, w, h, title, body_lines, accent=BLUE, fill=WHITE, title_fill=WHITE) -> None:
    c.rect(x, y, w, h, fill=fill, stroke=accent, sw=1.6, r=12)
    c.rect(x, y, w, 34, fill=accent, stroke=accent, r=12, sw=0)
    c.rect(x, y + 22, w, 12, fill=accent, stroke=accent, r=0, sw=0)
    c.text(x + 14, y + 23, title, size=13, fill=title_fill, weight="700")
    for i, line in enumerate(body_lines):
        c.text(x + 14, y + 56 + i * 18, line, size=12, fill=INK, weight="400")


# --------------------------------------------------------------------------- diagrams
def as_is_vs_target() -> None:
    c = Canvas(1100, 560, "As-is AU Beema redirect versus bank-owned platform")
    header(c, "The problem in one picture", "Today the bank loses the sale after redirect. The new platform keeps the whole journey.")
    box_card(
        c, 40, 92, 480, 360, "AS-IS — AU Beema Portal",
        [
            "1. RM or customer opens the Beema portal",
            "2. Basic details and a product list",
            "3. Customer is sent to the insurer site",
            "4. Bank cannot see drop-off, payment,",
            "   underwriting, or whether a policy issued",
            "",
            "Result: no conversion funnel, weak audit,",
            "commission and MIS reconstructed later.",
        ],
        accent=ROSE, fill=ROSE_BG,
    )
    box_card(
        c, 580, 92, 480, 360, "TARGET — Bank-owned platform",
        [
            "1. RM stays inside AU Bank's NIP-APP",
            "2. Suitability, consent, quote, proposal",
            "3. Customer pays on their own device",
            "4. Policy issued and reconciled in-platform",
            "5. Immutable audit trail for IRDAI",
            "",
            "Result: one bank-branded sale, replaceable",
            "insurer connectivity behind 1SB adapter.",
        ],
        accent=GREEN, fill=GREEN_BG,
    )
    c.path("M 528 272 L 572 272", color=NAVY, marker="url(#arrow-navy)")
    footer(c, "Source: docs/context/business-problem-statement.md §3 — teaching view, not SSOT.")
    c.save("01-as-is-vs-target.svg")


def r0_outcome() -> None:
    c = Canvas(1100, 420, "R0 outcome — one RM sells one Life policy")
    header(c, "What R0 must make true", "One certified RM · one ETB customer · one Group A Life product · end to end")
    steps = [
        ("Lead", "Find ETB\ncustomer"),
        ("Need", "Need analysis\n+ suitability"),
        ("Consent", "OTP on\ncustomer phone"),
        ("Quote", "Multi-insurer\ncompare"),
        ("Proposal", "Submit &\ntrack UW"),
        ("Pay", "Link to\ncustomer device"),
        ("Policy", "Issue only if\nRECONCILED"),
        ("Sold", "Audit\ncomplete"),
    ]
    x = 36
    for i, (title, body) in enumerate(steps):
        fill = GOLD if i == 7 else WHITE
        stroke = GOLD if i == 7 else BLUE
        c.rect(x, 120, 118, 150, fill=fill, stroke=stroke, sw=1.8, r=12)
        c.badge(x + 10, 134, 28, 22, str(i + 1), stroke)
        c.text(x + 59, 186, title, size=15, fill=NAVY, weight="700", anchor="middle")
        for j, line in enumerate(body.split("\n")):
            c.text(x + 59, 214 + j * 18, line, size=12, fill=SLATE, weight="400", anchor="middle")
        if i < len(steps) - 1:
            c.arrow(x + 122, 195, x + 136, 195, color=TEAL, marker="url(#arrow-teal)")
        x += 132
    c.rect(36, 300, 1028, 78, fill=SOFT, stroke=BLUE, r=10)
    c.text(56, 334, "Hard gates on this path", size=13, fill=NAVY, weight="700")
    c.text(
        56,
        356,
        "C1 no quote without suitability  |  C2 no proposal without consent OTP  |  C4 payment never on an RM device  |  Sold is not paid",
        size=13,
        fill=INK,
        weight="400",
    )
    footer(c, "Source: docs/architecture/R0-HLD.md §1 and CR-015 — Term and Savings/ULIP both in R0.")
    c.save("02-r0-outcome.svg")


def actors() -> None:
    c = Canvas(1100, 520, "Who is on the platform in R0")
    header(c, "Two on-platform actors — the customer is a participant", "Specified Person is a certificate on the RM, not a separate login.")
    cards = [
        (40, "Bank RM", GREEN, GREEN_BG, [
            "Workforce identity (bank AD → Keycloak)",
            "Only person who may originate a Lead",
            "Every regulated action (suitability, quote,",
            "proposal) is checked against SP certificate",
            "at the moment of the action, not at login",
        ]),
        (390, "Insurance Partner Rep", TEAL, TEAL_BG, [
            "Partner identity, maker-checker provisioned",
            "Assist only: own-insurer view, annotations",
            "Never Specified Person, never origination",
            "Cannot see another insurer's records",
            "Visibility is a query predicate, not a button",
        ]),
        (740, "Customer (participant)", GOLD, GOLD_BG, [
            "No platform session in R0",
            "Receives consent OTP on own phone",
            "Pays premium on own device via bank PG",
            "Never on an RM or bank-employee device",
            "Does not call 1SB or any bank service",
        ]),
    ]
    for x, title, accent, fill, lines in cards:
        box_card(c, x, 100, 320, 250, title, lines, accent=accent, fill=fill)
    c.rect(40, 372, 1020, 100, fill=WHITE, stroke=ROSE, r=12)
    c.text(60, 404, "Never confuse these", size=14, fill=ROSE, weight="700")
    c.text(60, 428, "Flutter never receives OAuth tokens. Keycloak is not the business authorisation source of truth.", size=13, fill=INK)
    c.text(60, 450, "The BFF holds the session. identity-authorization-service is the PDP. Default deny.", size=13, fill=INK)
    footer(c, "Source: docs/journey-execution/02-ACTOR-AND-USE-CASE-CATALOGUE.md §1 · ADR-004.")
    c.save("03-actors.svg")


def sale_journey() -> None:
    c = Canvas(1100, 720, "Assisted Life sale journey")
    header(c, "Assisted Life sale — the bank-owned spine", "Journey Orchestration holds stage and references only. Each box owns its own decision.")
    rows = [
        ("Origination", BLUE, [("Search ETB customer", "#4 Customer"), ("Create Lead", "#5 Lead"), ("Start onboarding", "exception engine")]),
        ("Advice", TEAL, [("Need analysis", "#7 Suitability"), ("Suitability C1", "fail closed"), ("Consent OTP C2", "#6 Consent")]),
        ("Offer", GOLD, [("Create quote", "#10 Quotation"), ("Fan-out via Hub", "#14 + #15 1SB"), ("Select offer", "partial OK")]),
        ("Apply", ROSE, [("Proposal draft", "#11 Proposal"), ("Submit + poll UW", "no auto-retry"), ("Requirements", "docs / medical")]),
        ("Money", GREEN, [("Pay link to customer", "#12 + #17"), ("Customer pays on PG", "own device"), ("Reconcile", "never guess")]),
        ("Close", NAVY, [("Issue policy", "#13 Policy"), ("Documents", "COI vault"), ("SOLD + audit", "#16 Audit")]),
    ]
    y = 96
    for title, accent, cells in rows:
        c.rect(24, y, 160, 86, fill=accent, stroke=accent, r=10, sw=0)
        c.text(104, y + 50, title, size=14, fill=WHITE, weight="700", anchor="middle")
        x = 204
        for i, (name, owner) in enumerate(cells):
            c.rect(x, y, 270, 86, fill=WHITE, stroke=accent, r=10)
            c.text(x + 16, y + 34, name, size=14, fill=NAVY, weight="700")
            c.text(x + 16, y + 58, owner, size=12, fill=MUTED, weight="400")
            if i < 2:
                c.arrow(x + 274, y + 43, x + 294, y + 43, color=accent)
            x += 300
        y += 98
    footer(c, "Source: R0-HLD.md §1–§2.6 · universal LOB journey. DIY / hybrid / Health / Motor are later releases.")
    c.save("04-sale-journey.svg")


def hop_architecture() -> None:
    c = Canvas(1100, 720, "Request hop — UI to insurer")
    header(c, "How a request actually travels", "Bank apps never call 1SB or a database. UI speaks bank language only.")
    layers = [
        ("L0 Device", "NIP-APP (Flutter) — separate repository", NAVY),
        ("L1 Edge", "CloudFront + WAF — TLS, bot, OWASP. No authN / authZ.", BLUE),
        ("L2 Ingress", "API Gateway — request shape, size, throttle. First AWS hop.", TEAL),
        ("L3 Internal", "Internal ALB — host/path to the BFF. Only ALB in R0.", SLATE),
        ("L4 BFF", "NIP BFF / workforce-access-bff - session, CSRF, PEP to PDP.", GOLD),
        ("L5 Domain", "Lead, Journey, Quote, Payment... - re-check PDP on regulated actions.", GREEN),
        ("Provider hop", "Integration Hub -> 1SB adapter -> Apigee egress -> 1SB -> insurer.", ROSE),
    ]
    y = 96
    for i, (title, body, accent) in enumerate(layers):
        c.rect(220, y, 660, 62, fill=WHITE, stroke=accent, sw=1.8, r=10)
        c.rect(220, y, 8, 62, fill=accent, stroke=accent, r=0, sw=0)
        c.text(248, y + 26, title, size=14, fill=NAVY, weight="700")
        c.text(248, y + 48, body, size=12, fill=SLATE, weight="400")
        if i < len(layers) - 1:
            c.arrow(550, y + 64, 550, y + 78, color=NAVY, marker="url(#arrow-navy)")
        y += 80
    c.rect(28, y + 8, 1044, 40, fill=SOFT, stroke=BLUE, r=8)
    c.text(
        550,
        y + 33,
        "Flutter never gets OAuth tokens  ·  UI/BFF never see 1SB codes  ·  no service calls an adapter directly",
        size=12,
        fill=NAVY,
        weight="600",
        anchor="middle",
    )
    footer(c, "Source: journey-execution/01-REQUEST-LIFECYCLE-STANDARD.md · ADR-018 / ADR-020 · standing constraints.")
    c.save("05-hop-architecture.svg")


def bounded_contexts() -> None:
    c = Canvas(1100, 700, "R0 bounded contexts")
    header(c, "The building blocks of R0", "Numbers are identities (NC-1). Names are labels. #5 is spoken as Lead.")
    groups = [
        ("Edge", BLUE, [(2, "NIP BFF"), (3, "Identity / PDP")]),
        ("Party & origination", TEAL, [(4, "Customer"), (5, "Lead"), (6, "Consent"), (7, "Suitability")]),
        ("Sale execution", GOLD, [(8, "Catalogue"), (9, "Journey"), (10, "Quotation"), (11, "Proposal & UW")]),
        ("Money & policy", GREEN, [(12, "Payment"), (13, "Policy"), (17, "Notification")]),
        ("Platform", NAVY, [(14, "Integration Hub"), (15, "1SB Adapter"), (16, "Audit"), (19, "Configuration")]),
        ("Later / thin in R0", SLATE, [(1, "Customer BFF — R1"), (18, "Reporting — W4")]),
    ]
    y = 96
    for title, accent, items in groups:
        c.rect(28, y, 200, 86, fill=accent, stroke=accent, r=10, sw=0)
        c.text(128, y + 50, title, size=13, fill=WHITE, weight="700", anchor="middle")
        x = 248
        slot = 200
        for num, name in items:
            c.rect(x, y, slot - 12, 86, fill=WHITE, stroke=accent, r=10)
            c.badge(x + 12, y + 14, 36, 22, f"#{num}", accent)
            c.text(x + 16, y + 62, name, size=13, fill=NAVY, weight="700")
            x += slot
        y += 96
    footer(c, "Source: business-problem-statement.md §6 · R0-HLD.md. #10 and #11 are LOB-owned; Health will not share their field shape.")
    c.save("06-bounded-contexts.svg")


def workstreams() -> None:
    c = Canvas(1100, 430, "Three workstreams")
    header(c, "Three workstreams, one platform", "Independent lanes. Do not collapse them into one backlog.")
    streams = [
        (40, "WS-3 Platform", "S08 Foundation", GREEN, [
            "The bank-owned insurance platform",
            "R0: assisted Life sale (Term + ULIP/Savings)",
            "NIP BFF, Lead, Journey, Hub, identity",
            "Open gate GATE-S08 is CANDIDATE",
        ]),
        (390, "WS-1 1SB integration", "L7 Hardening", BLUE, [
            "Thin adapter to 1Silverbullet",
            "No DB of its own — HTTP to persistence",
            "Term path UAT + Life adapter coverage",
            "Open gate GATE-P4 is BLOCKED",
        ]),
        (740, "WS-2 Workforce IAM", "L4 / L6 slice", TEAL, [
            "Token-hiding BFF session",
            "Keycloak behind an adapter",
            "Authorization service is the PDP",
            "Open gate GATE-IAM-P1 is OPEN",
        ]),
    ]
    for x, title, stage, accent, lines in streams:
        box_card(c, x, 100, 320, 250, title, [stage, ""] + lines, accent=accent)
    footer(c, "Source: docs/context/BOOT.md generated state as of 2026-09-30. Stage fields are human-owned.")
    c.save("07-workstreams.svg")


def hard_gates() -> None:
    c = Canvas(1100, 520, "Compliance hard gates")
    header(c, "Fail closed — these are not preferences", "A missing check at the BFF is not enforcement. The aggregate / store must refuse.")
    gates = [
        ("C1", "Suitability before quote", "No quote without a valid, unexpired suitability assessment id."),
        ("C2", "Consent before proposal", "No proposal without an unexpired customer-device OTP grant."),
        ("C3", "Attribution", "distributorId is server-injected. Caller-supplied values are rejected."),
        ("C4", "Customer-device payment", "No API path may issue a payment link into an RM session."),
        ("C8", "Sold is earned", "SOLD requires issued policy + RECONCILED payment + audit complete."),
        ("ID", "Token hiding", "Flutter never receives OAuth tokens. Keycloak is not the PDP."),
    ]
    positions = [(40, 100), (390, 100), (740, 100), (40, 280), (390, 280), (740, 280)]
    for (x, y), (code, title, body) in zip(positions, gates):
        c.rect(x, y, 320, 150, fill=WHITE, stroke=ROSE, r=12)
        c.badge(x + 16, y + 16, 44, 26, code, ROSE)
        c.text(x + 72, y + 34, title, size=14, fill=NAVY, weight="700")
        c.multilines(x + 160, y + 70, wrap(body, 28), size=13, fill=INK, weight="400", leading=18)
    footer(c, "Source: BOOT.md standing constraints · R0-HLD.md §1 · domain invariants INV-* .")
    c.save("08-hard-gates.svg")


def page_tree() -> None:
    c = Canvas(1100, 980, "Confluence page tree")
    header(c, "How to nest the pages in Confluence", "Create the gold bar page first. Every other page is a child or grandchild of it.")
    c.rect(48, 92, 1004, 52, fill=GOLD, stroke=GOLD, r=10, sw=0)
    c.text(550, 124, "MAIN PAGE  ·  AU Bank Insurance Platform — Start here", size=16, fill=NAVY, weight="700", anchor="middle")

    children = [
        (1, "Why we are building this", None),
        (2, "What we are building now (R0)", None),
        (3, "Who uses it", None),
        (4, "Use cases", None),
        (5, "The assisted Life sale journey", None),
        (6, "Architecture", ["6.1 How a request travels", "6.2 Bounded contexts and services"]),
        (7, "Sequence diagrams", [
            "7.1 RM login",
            "7.2 Lead create and assign",
            "7.3 Quote via 1SB",
            "7.4 Payment and issuance",
        ]),
        (8, "Rules you must never break", None),
        (9, "What lives in this repository", None),
        (10, "Where to read next", None),
    ]
    y = 168
    trunk_x = 72
    for num, name, subs in children:
        block_h = 44 if not subs else 44 + 8 + 36 * len(subs)
        c.rect(112, y, 500, 40, fill=WHITE, stroke=BLUE, r=8)
        c.badge(124, y + 8, 36, 24, str(num), BLUE)
        c.text(172, y + 26, name, size=14, fill=NAVY, weight="600")
        c.path(f"M {trunk_x} 118 L {trunk_x} {y + 20} L 112 {y + 20}", color=BLUE, marker="none", sw=1.5)
        if subs:
            for j, sub in enumerate(subs):
                sy = y + 52 + j * 36
                c.rect(640, sy, 412, 30, fill=SOFT, stroke=TEAL, r=8)
                c.text(656, sy + 20, sub, size=12, fill=NAVY, weight="500")
                c.path(f"M 612 {y + 20} L 612 {sy + 15} L 640 {sy + 15}", color=TEAL, marker="none", sw=1.4)
        y += 52 if not subs else 52 + 36 * len(subs) + 12
    c.text(
        28,
        960,
        "In Confluence: page menu -> Move -> set parent. Do not leave 6.1/6.2 or 7.1-7.4 as siblings of the home page.",
        size=11,
        fill=MUTED,
        weight="400",
    )
    c.save("00-page-tree.svg")


def policy_sold() -> None:
    c = Canvas(1100, 360, "When a policy is Sold")
    header(c, "Sold is a four-part fact", "Quote, proposal or payment alone never counts as a sale.")
    parts = [
        ("1", "Insurer issued\nthe contract", BLUE),
        ("2", "Bank received\nissuance confirm", TEAL),
        ("3", "Premium is\nRECONCILED", GOLD),
        ("4", "Ops + audit\nrecord is complete", GREEN),
    ]
    x = 70
    for i, (num, label, accent) in enumerate(parts):
        c.rect(x, 120, 200, 140, fill=WHITE, stroke=accent, sw=2, r=12)
        c.badge(x + 78, 140, 44, 28, num, accent)
        c.multilines(x + 100, 196, label.split("\n"), size=14, fill=NAVY, weight="600")
        if i < 3:
            c.text(x + 214, 188, "+", size=28, fill=GOLD, weight="700", anchor="middle")
        x += 250
    footer(c, "Source: business-problem-statement.md §4.3 · standing constraint. F-07 reconciliation break stays out of SOLD.")
    c.save("09-policy-sold.svg")


class Sequence(Canvas):
    def __init__(self, width: int, height: int, title: str, actors: list[tuple[str, str]]):
        super().__init__(width, height, title)
        self.actors = actors
        self.xs = []
        gap = (width - 80) / max(len(actors), 1)
        for i, _ in enumerate(actors):
            self.xs.append(40 + gap / 2 + i * gap)
        self.top = 118
        self.y = 172
        self.row = 42

    def draw_actors(self) -> None:
        bottom = self.height - 48
        for x, (short, long) in zip(self.xs, self.actors):
            self.raw(
                f'<line x1="{x:.1f}" y1="{self.top + 28}" x2="{x:.1f}" y2="{bottom}" '
                f'stroke="{LINE}" stroke-width="1.4" stroke-dasharray="3 7"/>'
            )
            self.rect(x - 70, self.top - 22, 140, 44, fill=NAVY, stroke=NAVY, r=8, sw=0)
            self.text(x, self.top + 6, short, size=12, fill=WHITE, weight="700", anchor="middle")
            self.rect(x - 70, bottom - 8, 140, 32, fill=NAVY, stroke=NAVY, r=8, sw=0)
            self.text(x, bottom + 13, short, size=11, fill=WHITE, weight="600", anchor="middle")

    def _x(self, name: str) -> float:
        for x, (short, _) in zip(self.xs, self.actors):
            if short == name:
                return x
        raise KeyError(name)

    def call(self, src: str, dst: str, label: str, dashed: bool = False, color: str = BLUE) -> None:
        if src == dst:
            self.self_call(name=src, label=label)
            return
        x1, x2 = self._x(src), self._x(dst)
        marker = "url(#arrow)"
        if color == ROSE:
            marker = "url(#arrow-rose)"
        elif color == TEAL:
            marker = "url(#arrow-teal)"
        elif color == GREEN:
            marker = "url(#arrow-green)"
        elif color == NAVY:
            marker = "url(#arrow-navy)"
        self.arrow(x1, self.y, x2, self.y, color=color, dashed=dashed, marker=marker)
        mid = (x1 + x2) / 2
        self.text(mid, self.y - 8, label, size=11, fill=INK, weight="500", anchor="middle")
        self.y += self.row

    def note(self, src: str, dst: str, text: str, fill: str = GOLD_BG, stroke: str = GOLD) -> None:
        x1, x2 = self._x(src), self._x(dst)
        left, right = min(x1, x2) - 10, max(x1, x2) + 10
        self.rect(left, self.y - 12, right - left, 28, fill=fill, stroke=stroke, r=6)
        self.text((left + right) / 2, self.y + 6, text, size=11, fill=NAVY, weight="600", anchor="middle")
        self.y += self.row + 4

    def alt_start(self, title: str, x0: float, x1: float) -> float:
        self.rect(x0, self.y - 8, x1 - x0, 18, fill=SOFT, stroke=TEAL, r=4, sw=1)
        self.text(x0 + 10, self.y + 6, title, size=11, fill=TEAL, weight="700")
        start = self.y + 12
        self.y += 24
        return start

    def self_call(self, name: str, label: str) -> None:
        x = self._x(name)
        self.path(
            f"M {x} {self.y} L {x + 40} {self.y} L {x + 40} {self.y + 18} L {x} {self.y + 18}",
            color=SLATE,
        )
        self.text(x + 50, self.y + 5, label, size=11, fill=INK, weight="500")
        self.y += self.row + 6


def seq_login() -> None:
    s = Sequence(
        1100,
        860,
        "RM login — token-hiding BFF",
        [("RM", "RM device"), ("BFF", "workforce-access-bff"), ("Adapter", "IdP adapter"), ("Keycloak", "Keycloak"), ("PDP", "identity-authorization")],
    )
    header(s, "Sequence — RM login", "The device never talks to Keycloak's token endpoint and never receives an OAuth token.")
    s.draw_actors()
    s.call("RM", "BFF", "POST /api/v1/auth/login")
    s.call("BFF", "Adapter", "POST authorization-uri")
    s.call("Adapter", "Keycloak", "Build provider URI")
    s.call("BFF", "RM", "Return URI only — no token", dashed=True)
    s.call("RM", "Keycloak", "Bank-controlled login ceremony")
    s.call("Keycloak", "RM", "302 to BFF callback", dashed=True, color=TEAL)
    s.call("RM", "BFF", "GET /callback?code&state")
    s.call("BFF", "Adapter", "token-exchange + PKCE verifier")
    s.call("Adapter", "Keycloak", "Redeem code, validate iss/aud/exp")
    s.call("BFF", "PDP", "Resolve provider subject to business identity")
    s.self_call("BFF", "Store session; encrypt provider tokens")
    s.call("BFF", "RM", "Cookie / opaque handle — never the access token", dashed=True, color=GREEN)
    s.note("RM", "PDP", "SP certificate is NOT checked at login — it is checked on each regulated action")
    footer(s, "Source: docs/journey-execution/flows/UC-01-rm-login.md. IPR login is the same endpoints, partner realm.")
    s.save("10-seq-rm-login.svg")


def seq_lead() -> None:
    s = Sequence(
        1100,
        860,
        "Lead create — product, dedupe, save, onboard, assign",
        [("User", "Workforce user"), ("APP", "NIP-APP"), ("BFF", "NIP BFF"), ("Lead", "Lead #5"), ("PDP", "AuthZ PDP")],
    )
    header(s, "Sequence — Lead create and assign", "Canonical order (D-019): product, dedupe, Save, Start Onboarding, assign SP.")
    s.draw_actors()
    s.call("User", "APP", "Search customer + select productClass")
    s.call("APP", "BFF", "GET /customers:search (lead-first, then CBS)")
    s.call("BFF", "Lead", "Unfinished duplicate for user+customer+product?")
    s.note("APP", "Lead", "If duplicate and BI not generated: Continue existing or Cancel — no Delete")
    s.call("APP", "BFF", "POST /leads  (Save — does not evaluate exceptions)")
    s.call("BFF", "Lead", "POST /internal/v1/leads  state=NEW, assignedRmId=null")
    s.call("Lead", "BFF", "201 leadId", dashed=True, color=TEAL)
    s.call("User", "APP", "Start Onboarding")
    s.call("APP", "BFF", "POST /leads/{id}:start-onboarding")
    s.self_call("Lead", "Exception engine: PASS | BLOCK | APPROVAL_REQUIRED")
    s.call("User", "APP", "Assignment screen — certified-SP AU Bank RM")
    s.call("APP", "BFF", "POST /leads/{id}/assignments")
    s.call("Lead", "PDP", "Target has SP cert for LIFE?")
    s.call("PDP", "Lead", "allow / deny", dashed=True, color=GREEN)
    s.note("APP", "PDP", "Suitability and downstream regulated steps: AU SP or Insurance RM/FLS only")
    footer(s, "Source: docs/platform/ws3-platform/11-lead-module-sequences.md · D-019 · ADR-021.")
    s.save("11-seq-lead.svg")


def seq_quote() -> None:
    s = Sequence(
        1100,
        860,
        "Quote via Integration Hub and 1SB",
        [("RM", "Bank RM"), ("BFF", "NIP BFF"), ("Jrn", "Journey #9"), ("Quote", "Quotation #10"), ("Hub", "Hub #14"), ("1SB", "Adapter + 1SB")],
    )
    header(s, "Sequence — Quote via 1SB", "C1 is re-checked at quote entry. Partial insurer success is success.")
    s.draw_actors()
    s.call("RM", "BFF", "POST /journeys/{id}/quotes")
    s.call("BFF", "Jrn", "Load stage + suitabilityRef + party snapshot")
    s.note("BFF", "Quote", "C1: suitability id present, unexpired, PASS/OVERRIDE — else 403")
    s.call("BFF", "Quote", "Create quote job (canonical bank contract)")
    s.call("Quote", "Hub", "Route to Life provider — never from BFF")
    s.call("Hub", "1SB", "Translate to 1SB wire; POST quote")
    s.call("1SB", "Hub", "reqId — async accepted", dashed=True, color=TEAL)
    s.call("RM", "BFF", "GET …/quotes/{quoteId}  (poll)")
    s.call("Quote", "Hub", "Poll until complete")
    s.call("Hub", "1SB", "GET quote poll")
    s.note("Quote", "1SB", "Partial product/insurer errors do not fail the whole multi-quote")
    s.call("RM", "BFF", "POST …/quotes/{quoteId}/selection")
    s.call("BFF", "Jrn", "Stage becomes QUOTE_SELECTED")
    footer(s, "Source: universal-lob-journey.md · R0-HLD.md §1 step 4 · standing constraint UI→BFF→Hub.")
    s.save("12-seq-quote.svg")


def seq_payment() -> None:
    s = Sequence(
        1100,
        900,
        "Payment on the customer device and issuance",
        [("RM", "Bank RM"), ("Pay", "Payment #12"), ("Cust", "Customer device"), ("PG", "AU Bank PG"), ("Pol", "Policy #13"), ("Aud", "Audit #16")],
    )
    header(s, "Sequence — Payment and issuance", "C4: the payment link is sent to the customer. The RM device is not on the money path.")
    s.draw_actors()
    s.call("RM", "Pay", "POST /journeys/{id}/payments")
    s.note("RM", "Cust", "C4 — platform refuses if the destination is an RM or bank-employee session")
    s.call("Pay", "Cust", "SMS / link to customer's own device", color=GOLD)
    s.call("Cust", "PG", "Customer pays on hosted bank PG page")
    s.call("PG", "Pay", "Authorisation callback (separate API GW route, IP allowlist)", color=TEAL)
    s.self_call("Pay", "State = CAPTURED / UNCERTAIN — never guess")
    s.call("Pay", "Pay", "Settlement file → RECONCILED", dashed=True)
    s.note("Pay", "Pol", "Policy issue is blocked until payment is RECONCILED (SC-W3-4)")
    s.call("Pay", "Pol", "Issue policy against RECONCILED payment", color=GREEN)
    s.call("Pol", "Aud", "Append-only evidence + outbox")
    s.call("Aud", "Pol", "Required events acknowledged", dashed=True, color=GREEN)
    s.note("RM", "Aud", "Journey may reach SOLD only when policy ACTIVE + payment RECONCILED + audit complete")
    footer(s, "Source: R0-HLD.md §1 steps 6–9 · INV-PAY-04 · INV-JRN-05. F-07 reconciliation break is a manual finance procedure.")
    s.save("13-seq-payment.svg")


def repo_map() -> None:
    c = Canvas(1100, 520, "What this git repository holds")
    header(c, "This repository vs the client app", "DOC-006: Java services, shared libraries and docs only. NIP-APP lives elsewhere.")
    box_card(c, 40, 100, 500, 300, "In this git repo", [
        "services/1sb-integration-service     :8080",
        "services/bank-persistence-service    :8081",
        "services/workforce-access-bff        :8084",
        "libs/  bank-common-error, security,",
        "       audit, secrets",
        "docs/  BRD, HLD, governance, this pack",
    ], accent=GREEN, fill=GREEN_BG)
    box_card(c, 560, 100, 500, 300, "Not in this git repo", [
        "NIP-APP / Flutter source (separate repo)",
        "Customer-facing DIY app (R1)",
        "Insurer underwriting engines",
        "AU Bank CBS, AD, Payment Gateway",
        "1SB itself — it is a vendor we call",
    ], accent=ROSE, fill=ROSE_BG)
    footer(c, "Evaluate BFF contracts against docs/figma/ as the frontend reference. Behaviour SSOT remains the detailed BRDs.")
    c.save("14-repo-boundary.svg")


def use_case_map() -> None:
    c = Canvas(1100, 640, "Use case groups")
    header(c, "R0 use cases at a glance", "35 use cases. Access/session (UC-01…05) are specified. The rest follow the same ladder.")
    groups = [
        ("A Access", "UC-01…06", "Login, session, logout, PDP"),
        ("B Origination", "UC-07…09", "Lead, CBS lookup, start journey"),
        ("C Advisory", "UC-10…12", "Need analysis, suitability, override"),
        ("D Consent", "UC-13…14", "OTP challenge, verify grant"),
        ("E Quotation", "UC-15…18", "Create, poll, select, catalogue"),
        ("F Proposal", "UC-19…21", "Draft, submit, UW track"),
        ("G Payment", "UC-22…26", "Link, pay, callback, recon"),
        ("H Policy", "UC-27…29", "Issue, documents, SOLD"),
        ("I Cross-cut", "UC-30…35", "Config, audit, notify, IPR read"),
    ]
    for i, (title, uc, body) in enumerate(groups):
        col, row = i % 3, i // 3
        x, y = 40 + col * 350, 110 + row * 150
        c.rect(x, y, 330, 130, fill=WHITE, stroke=BLUE, r=12)
        c.text(x + 20, y + 36, title, size=16, fill=NAVY, weight="700")
        c.badge(x + 210, y + 18, 100, 24, uc, TEAL)
        c.text(x + 20, y + 78, body, size=13, fill=INK, weight="400")
        who = "RM + IPR" if i == 0 else ("RM only origination" if i == 1 else "see actor matrix")
        c.text(x + 20, y + 104, who, size=12, fill=MUTED, weight="400")
    footer(c, "Source: docs/journey-execution/02-ACTOR-AND-USE-CASE-CATALOGUE.md. IPR cannot originate or advise.")
    c.save("15-use-cases.svg")


def main() -> None:
    drawers = [
        page_tree,
        as_is_vs_target,
        r0_outcome,
        actors,
        sale_journey,
        hop_architecture,
        bounded_contexts,
        workstreams,
        hard_gates,
        policy_sold,
        seq_login,
        seq_lead,
        seq_quote,
        seq_payment,
        repo_map,
        use_case_map,
    ]
    for fn in drawers:
        fn()
        print("wrote", fn.__name__)
    try:
        import cairosvg
    except ImportError:
        print("cairosvg not installed — SVG only")
        print("output", OUT)
        return
    for svg in sorted(OUT.glob("*.svg")):
        png = svg.with_suffix(".png")
        cairosvg.svg2png(url=str(svg), write_to=str(png), scale=2)
        print("png", png.name)
    print("output", OUT)


if __name__ == "__main__":
    main()
