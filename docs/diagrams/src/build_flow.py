#!/usr/bin/env python3
"""
Behaviour and security diagrams for the architecture pre-approval pack v0.2:

    quote-path.svg            lookup → suitability (C1) → consent (C2) → quote, and every refusal
    journey-state-machine.svg every state, every legal transition, every terminal state
    trust-zones.svg           five zones, what may cross each boundary, and what is proven

    python3 docs/diagrams/src/build_flow.py docs/diagrams
"""
import os, sys
from svgkit import (Canvas, W1, W2, W3, W4, EXT, CUST, WARN, OK, NEUT, GOV,
                    INK, BODY, MUTED, FAINT, GHOST, HAIR, BAND, WASH, PAPER, DARK, RED, RED_D,
                    wrap, DRAFT)

M = 52
FOOT_R = "WS-3 · AU Bank Insurance Distribution Platform · R0"


# ═════════════════════════════════════════════════════════════════ 1. QUOTE PATH
def quote_path():
    c = Canvas(1900, 1330,
               "AU Bank Insurance Distribution Platform — R0 Quote Path",
               "customer lookup → need & suitability (C1) → consent (C2) → quote · the lawful-gate "
               "half of the journey, and the five ways it is refused")
    W = c.w - 2 * M

    c.section(M, 88, "The rule this half of the journey exists to enforce")
    c.rect(M, 96, W, 46, WASH, HAIR, rx=7, sw=1.2)
    c.text(M + 16, 116, "C1 — no quotation without a valid, unexpired suitability result. "
                        "C2 — no proposal without current consent.", 12, INK, weight="bold")
    c.text(M + 16, 133, "Both are hard gates enforced in the owning context, not checks the UI is "
                        "trusted to have done. A gate that can be skipped by calling the API "
                        "directly is not a gate — FF-08 and FF-09 assert exactly that.", 10, MUTED)

    LANES = [
        ("rm",   "RM · Flutter + #2 BFF", "assisted journey",     W4,   "W4"),
        ("pdp",  "Identity + PDP",        "default deny · 300 ms", EXT,  None),
        ("cus",  "#4 Customer",           "snapshot + freshness",  W1,   "W1"),
        ("cbs",  "Core Banking",          "external · golden",     EXT,  None),
        ("jrn",  "#9 Journey",            "stage + references",    W1,   "W1"),
        ("sui",  "#7 Suitability",        "the C1 gate",           W2,   "W2"),
        ("con",  "#6 Consent",            "the C2 gate",           W2,   "W2"),
        ("cat",  "#8 Catalogue",          "what may be sold",      W1,   "W1"),
        ("quo",  "#10 Quotation",         "offers + ordering",     W2,   "W2"),
        ("hub",  "#14 Hub → insurer",     "canonical contract",    W1,   "W1"),
    ]
    LANE_TOP, LANE_H = 168, 52
    BOX_W = 168
    LANE_W = W / len(LANES)
    X = {k: M + LANE_W * (i + .5) for i, (k, *_) in enumerate(LANES)}

    MSGS = [
        (1,  "rm",  "pdp", "RM signs in; permissions resolved server-side",
             "role · branch · hierarchy · insurer scope · certification — never from the device", "sync"),
        (2,  "rm",  "cus", "look up ETB customer (CIF / mobile / PAN)",
             "purpose recorded · narrow result set · rate limited — search abuse is a named threat", "sync"),
        (3,  "cus", "cbs", "fetch customer",
             "CBS stays the golden source; the platform keeps a timestamped snapshot (S-04 / S-05)", "sync"),
        (4,  "cus", "jrn", "open journey · stage := CUSTOMER_LINKED",
             "the journey now has an owner, a stage and a resumable reference", "sync"),
        (5,  "rm",  "sui", "capture need & submit suitability inputs",
             "16 inputs · SUIT-ALGO-LIFE-v1.1 · 62 rules (PR #54)", "sync"),
        (6,  "sui", "sui", "evaluate — eligibility-based, never a ranking or a recommendation",
             "records inputs, rule-pack version, outcome, reasons and a validity window", "self"),
        (7,  "sui", "jrn", "stage := SUITABILITY_PASSED + suitabilityRef",
             "#9 stores the reference; it never stores what suitability decided (SC-W3-6)", "sync"),
        (8,  "rm",  "con", "request customer consent",
             "42 consent rules · CONSENT-PACK · wording version pinned to the record", "sync"),
        (9,  "con", "rm",  "customer confirms on their own device via OTP",
             "the RM cannot confirm consent on the customer's behalf — attributable or invalid", "ret"),
        (10, "con", "jrn", "stage := CONSENT_CAPTURED + consentRef",
             "withdrawal is a first-class event, not an absence of a record", "sync"),
        (11, "rm",  "cat", "list eligible products",
             "R0: Term Life, Group A, one insurer · eligibility + channel + effective dates", "sync"),
        (12, "rm",  "quo", "request quotes",
             "refused with 403 unless a valid unexpired suitabilityRef is presented (C1 · FF-08)", "sync"),
        (13, "quo", "hub", "provider quote request",
             "translated to the canonical contract at the seam; no insurer format travels inward", "async"),
        (14, "hub", "quo", "offers → normalised to the bank's common shape",
             "expiry captured per offer; a stale offer cannot be selected", "ret"),
        (15, "quo", "rm",  "offers ordered by disclosed customer-relevant basis only",
             "commission and bank preference are not inputs, and no field exists to make them one (QR-07)", "ret"),
        (16, "rm",  "jrn", "customer selects an offer · stage := OFFER_SELECTED",
             "hands over to the money path — see r0-money-path.svg", "sync"),
    ]

    FIRST, DY = 268, 41
    LIFE_TOP = LANE_TOP + LANE_H
    LIFE_BOT = FIRST + (len(MSGS) - 1) * DY + 26

    for i, (k, title, sub, pal, tg) in enumerate(LANES):
        cx = X[k]
        if i % 2 == 1:
            c.rect(cx - LANE_W / 2, LIFE_TOP, LANE_W, LIFE_BOT - LIFE_TOP, BAND, rx=0, sw=0)
        c.rect(cx - BOX_W / 2, LANE_TOP, BOX_W, LANE_H, pal.fill, pal.stroke, rx=6, dash=pal.dash)
        c.text(cx, LANE_TOP + 22, title, 10.5, INK, "middle", "bold")
        c.text(cx, LANE_TOP + 37, sub, 9, FAINT, "middle")
        if tg:
            c.tag(cx - BOX_W / 2 + 6, LANE_TOP + 5, tg, pal.stroke)
        if k == "sui":
            c.ctrl(cx + BOX_W / 2 - 28, LANE_TOP + 5, "C1")
        if k == "con":
            c.ctrl(cx + BOX_W / 2 - 28, LANE_TOP + 5, "C2")
        c.line(cx, LIFE_TOP, cx, LIFE_BOT, HAIR, 1.4, dash="3 4")

    for idx, (n, src, dst, label, detail, kind) in enumerate(MSGS):
        y = FIRST + idx * DY
        x1, x2 = X[src], X[dst]
        gate = n in (6, 12)
        if kind == "self":
            c.path(f"M{x1},{y-8} h26 v16 h-26", BODY, sw=1.5, marker="arr")
            c.dot(x1 - 4, y - 20, n, RED if gate else DARK)
            c.text(x1 + 38, y - 1, label, 9.5, INK, weight="bold")
            c.text(x1 + 38, y + 12, detail, 9, FAINT)
            continue
        stroke = GHOST if kind in ("async", "ret") else BODY
        marker = "arrGray" if kind in ("async", "ret") else "arr"
        dash = "6 4" if kind in ("async", "ret") else None
        c.line(x1, y, x2, y, stroke, 1.5, dash, marker)
        c.dot(x1, y, n, RED if gate else DARK)
        lx = min(x1, x2) + (24 if x2 < x1 else 16)
        c.text(lx, y - 8, label, 9.5, INK, weight="bold")
        c.text(lx, y + 13, detail, 9, FAINT)

    gy = LIFE_BOT + 14
    c.status_banner(M, gy, W, "Handover — OFFER_SELECTED",
                    "Everything above is the lawful-gate half. The money path picks up here: "
                    "proposal → underwriting → payment → reconciliation → issuance → SOLD "
                    "(r0-money-path.svg).", OK, h=44)

    fy = gy + 62
    c.section(M, fy, "Refusals — what the platform must say no to, and what the RM sees when it does")
    PANELS = [
        ("X1", "Quote without suitability", "step 12",
         [("Trigger", "no suitabilityRef, or the reference does not resolve"),
          ("Response", "403 — the quote request is not attempted at the insurer"),
          ("RM sees", "'complete the suitability assessment first', with a link to it"),
          ("Proven by", "FF-08 asserts the API refuses, not just the UI"),
          ("Never", "a warning the RM can dismiss · an override flag · a supervisor bypass")]),
        ("X2", "Suitability expired", "step 12",
         [("Trigger", "suitability outside its validity window at quote time"),
          ("Response", "403 — a stale assessment is treated as no assessment"),
          ("RM sees", "'this assessment expired on <date>, reassess to continue'"),
          ("Proven by", "validity window is stored on the record, not computed at read"),
          ("Never", "silently re-use, extend, or round the window")]),
        ("X3", "Consent withdrawn or stale", "at proposal",
         [("Trigger", "consent withdrawn, or the wording version has been superseded"),
          ("Response", "proposal submission is blocked and the journey states why"),
          ("RM sees", "'consent was withdrawn on <date>' — never a generic error"),
          ("Proven by", "FF-09 · withdrawal is an event with its own record"),
          ("Never", "treat absence of a withdrawal as presence of consent")]),
        ("X4", "Product not eligible", "step 11",
         [("Trigger", "customer or channel falls outside the catalogue's eligibility"),
          ("Response", "the product is not listed — not listed-and-then-refused"),
          ("RM sees", "only products that can lawfully be sold to this customer"),
          ("Proven by", "eligibility evaluated in #8, not filtered in the client"),
          ("Never", "show it, take the input, then fail at submission")]),
        ("X5", "Offer expired at selection", "step 16",
         [("Trigger", "the selected offer's expiry has passed"),
          ("Response", "re-quote required; the expired offer cannot be carried forward"),
          ("RM sees", "'this quote expired — request a fresh one'"),
          ("Proven by", "expiry is a property of the offer record"),
          ("Never", "honour a lapsed premium because the journey is nearly complete")]),
    ]
    pw = (W - 4 * 12) / 5
    for i, (pid, t, meta, rows) in enumerate(PANELS):
        c.panel(M + i * (pw + 12), fy + 12, pw, 218, f"{pid} · {t}", rows, WARN, meta)

    c.legend(M, fy + 254, [("sync", "synchronous — caller waits, deadline enforced"),
                           ("async", "asynchronous — provider call, poll or callback"),
                           ("ctrl:C1", "non-waivable control"),
                           ("dot:6", "hard gate — fails closed")])
    c.text(M + 1080, fy + 258, "#n bounded context · S-nn / FF-nn / QR-07 as used in docs/hdl.svg",
           9.5, MUTED)
    c.footer(DRAFT, FOOT_R)
    return c


# ═══════════════════════════════════════════════════════ 2. JOURNEY STATE MACHINE
def journey_state_machine():
    c = Canvas(1900, 1062,
               "AU Bank Insurance Distribution Platform — R0 Journey State Machine",
               "every state #9 may hold, every legal transition, every terminal state, and what "
               "compensates when a transition fails · closes S06-G2 / S06-G3")
    W = c.w - 2 * M

    c.rect(M, 84, W, 46, WASH, HAIR, rx=7, sw=1.2)
    c.text(M + 16, 104, "A state not on this diagram does not exist. A transition not drawn here "
                        "is a defect, not a feature.", 12, INK, weight="bold")
    c.text(M + 16, 121, "#9 Journey Orchestration owns this machine. It holds the stage and the "
                        "references — never another context's decision (SC-W3-6). No terminal "
                        "state is reached by elapsed time, retry count or operator judgement.",
           10, MUTED)

    PHASES = [("Open", W1), ("Advise — C1 · C2", W2), ("Quote", W2),
              ("Propose & underwrite", W3), ("Pay", W3), ("Issue & close", W3)]
    colw = (W - 5 * 16) / 6
    colx = lambda i: M + i * (colw + 16)
    for i, (t, pal) in enumerate(PHASES):
        c.rect(colx(i), 148, colw, 30, pal.fill, pal.stroke, rx=6, sw=1.4)
        c.text(colx(i) + colw / 2, 168, t, 11.5, INK, "middle", "bold")

    # ---- the spine: the happy path only
    SPINE = [
        (0, 0, "OPENED",              "normal", "journey exists, owner assigned"),
        (0, 1, "CUSTOMER_LINKED",     "normal", "ETB snapshot taken, timestamped"),
        (1, 0, "NEEDS_CAPTURED",      "normal", "16 suitability inputs recorded"),
        (1, 1, "SUITABILITY_PASSED",  "gate",   "C1 · ref + validity window stored"),
        (1, 2, "CONSENT_CAPTURED",    "gate",   "C2 · customer-confirmed via OTP"),
        (2, 0, "QUOTE_REQUESTED",     "wait",   "at the insurer via #14 — async"),
        (2, 1, "QUOTED",              "normal", "offers normalised, expiry per offer"),
        (2, 2, "OFFER_SELECTED",      "normal", "selection is the customer's, recorded"),
        (3, 0, "PROPOSAL_SUBMITTING", "wait",   "idempotency key issued, 24 h contract"),
        (3, 1, "PROPOSAL_SUBMITTED",  "wait",   "insurerRef held — the recovery handle"),
        (3, 2, "UW_REQUIREMENTS",     "wait",   "insurer wants more — tracked, not decided"),
        (3, 3, "UW_APPROVED",         "normal", "unlocks payment; nothing else does"),
        (4, 0, "PAYMENT_PENDING",     "wait",   "link on the customer device only (C4)"),
        (4, 1, "PAYMENT_UNCERTAIN",   "wait",   "no callback — the honest state, not a failure"),
        (4, 2, "PAYMENT_RECONCILED",  "gate",   "settlement matched — the only truth"),
        (5, 0, "POLICY_ISSUED",       "normal", "issued — and still not sold"),
        (5, 1, "SOLD",                "terminal_good", "issued ∧ reconciled ∧ audit complete"),
    ]
    # ---- terminal states, in their own band, reached only by an explicit branch
    TERMINALS = [
        (1, "SUITABILITY_FAILED",  "not eligible — the journey ends honestly",
         "NEEDS_CAPTURED", "not eligible"),
        (3, "UW_DECLINED",         "the insurer's decision, recorded verbatim",
         "UW_REQUIREMENTS", "declined"),
        (4, "PAYMENT_FAILED",      "settled as failed — never assumed, always reconciled",
         "PAYMENT_UNCERTAIN", "settled failed"),
        (5, "ABANDONED",           "explicit, with the last stage retained",
         None, None),
        (2, "CANCELLED_IN_FLIGHT", "customer withdrew — compensation runs",
         None, None),
    ]

    KIND = {"normal": (W1.fill, W1.stroke, W1.deep, 1.4, None),
            "gate":   (W2.fill, RED, W2.deep, 2.0, None),
            "wait":   (NEUT.fill, GHOST, MUTED, 1.4, "4 3"),
            "terminal_good": (OK.fill, OK.stroke, OK.deep, 2.4, None),
            "terminal_bad":  ("#fef2f2", RED, RED_D, 1.6, None)}

    SH, ROW0, RDY, TERM_Y = 60, 196, 74, 528
    pos = {}

    def draw(name, kind, note, x, y):
        f, s, d, sw, dash = KIND[kind]
        c.rect(x, y, colw, SH, f, s, rx=6, sw=sw, dash=dash)
        c.text(x + colw / 2, y + 22, name, 10.5, INK, "middle", "bold")
        for j, ln in enumerate(wrap(note, colw - 16, 8.8)):
            c.text(x + colw / 2, y + 36 + j * 10, ln, 8.8, d, "middle")
        pos[name] = (x, y, colw, SH)
        if kind == "gate":
            c.ctrl(x + colw - 28, y + 5,
                   "C1" if "SUIT" in name else ("C2" if "CONSENT" in name else "C4"))

    for (col, row, name, kind, note) in SPINE:
        draw(name, kind, note, colx(col), ROW0 + row * RDY)
    for (col, name, note, _f, _l) in TERMINALS:
        draw(name, "terminal_bad", note, colx(col), TERM_Y)

    def down(a, b, label=None, kind="ok"):
        (ax, ay, aw, ah) = pos[a]; (bx, by, bw, bh) = pos[b]
        stroke = {"ok": BODY, "fail": RED, "wait": GHOST}[kind]
        marker = {"ok": "arr", "fail": "arrRed", "wait": "arrGray"}[kind]
        dash = "5 4" if kind == "wait" else None
        c.line(ax + aw / 2, ay + ah, bx + bw / 2, by, stroke, 1.5, dash, marker)
        if label:
            c.text(ax + aw / 2 + 7, ay + ah + 12, label, 8.5, stroke, weight="bold")

    def across(a, b, label=None, kind="ok"):
        (ax, ay, aw, ah) = pos[a]; (bx, by, bw, bh) = pos[b]
        stroke = {"ok": BODY, "fail": RED, "wait": GHOST}[kind]
        marker = {"ok": "arr", "fail": "arrRed", "wait": "arrGray"}[kind]
        y1, y2, mx = ay + ah / 2, by + bh / 2, ax + aw + 8
        c.path(f"M{ax+aw},{y1} H{mx} V{y2} H{bx}", stroke, sw=1.5, marker=marker)
        if label:
            c.text(mx + 3, min(y1, y2) - 6, label, 8.5, stroke, weight="bold")

    def branch(a, b, label):
        """Explicit branch off the spine into the terminal band."""
        (ax, ay, aw, ah) = pos[a]; (bx, by, bw, bh) = pos[b]
        # route down the inter-column gutter so the branch never crosses a state box
        sx = ax + aw + 8
        c.path(f"M{ax+aw},{ay+ah/2} H{sx} V{by-18} H{bx+bw/2} V{by}", RED, sw=1.5, dash="4 3",
               marker="arrRed")
        c.text(sx + 5, ay + ah / 2 - 6, label, 8.5, RED, weight="bold")

    down("OPENED", "CUSTOMER_LINKED")
    across("CUSTOMER_LINKED", "NEEDS_CAPTURED")
    down("NEEDS_CAPTURED", "SUITABILITY_PASSED", "eligible")
    down("SUITABILITY_PASSED", "CONSENT_CAPTURED", "C1 recorded")
    across("CONSENT_CAPTURED", "QUOTE_REQUESTED", "C2 recorded")
    down("QUOTE_REQUESTED", "QUOTED", "offers in", "wait")
    down("QUOTED", "OFFER_SELECTED")
    across("OFFER_SELECTED", "PROPOSAL_SUBMITTING")
    down("PROPOSAL_SUBMITTING", "PROPOSAL_SUBMITTED", "ack", "wait")
    down("PROPOSAL_SUBMITTED", "UW_REQUIREMENTS", "more needed", "wait")
    down("UW_REQUIREMENTS", "UW_APPROVED", "met")
    across("UW_APPROVED", "PAYMENT_PENDING", "payment unlocked")
    down("PAYMENT_PENDING", "PAYMENT_UNCERTAIN", "no callback", "wait")
    down("PAYMENT_UNCERTAIN", "PAYMENT_RECONCILED", "settlement matched")
    across("PAYMENT_RECONCILED", "POLICY_ISSUED", "issuance allowed")
    down("POLICY_ISSUED", "SOLD", "+ audit complete")
    branch("NEEDS_CAPTURED", "SUITABILITY_FAILED", "not eligible")
    branch("UW_REQUIREMENTS", "UW_DECLINED", "declined")
    branch("PAYMENT_UNCERTAIN", "PAYMENT_FAILED", "settled as failed")

    c.text(colx(2), TERM_Y - 14, "TERMINAL STATES — reached only by the explicit branch drawn "
                                 "into them", 9, FAINT, weight="bold", ls="0.5")
    c.text(colx(2), TERM_Y + 76, "ABANDONED and CANCELLED_IN_FLIGHT are reachable from any "
                                 "in-flight state. Both retain the last stage; CANCELLED_IN_FLIGHT "
                                 "after payment opens the refund compensation below.",
           8.8, MUTED)

    iy = 620
    c.section(M, iy, "Transition invariants — the transitions that must NOT exist")
    inv = [
        ("no PAYMENT_* → SOLD directly", "SOLD requires POLICY_ISSUED as well — paid is not sold"),
        ("no POLICY_ISSUED → SOLD without audit", "the third leg of INV-JRN-05 is not optional"),
        ("no PAYMENT_UNCERTAIN → RECONCILED by timeout", "only a settlement match moves it"),
        ("no QUOTED without SUITABILITY_PASSED", "C1 is enforced at the transition, not the screen"),
        ("no PROPOSAL_* without CONSENT_CAPTURED", "C2, same rule"),
        ("no state → OPENED", "a journey is never rewound; a new journey is a new journey"),
    ]
    iw = (W - 5 * 12) / 6
    for i, (t, d) in enumerate(inv):
        x = M + i * (iw + 12)
        c.rect(x, iy + 14, iw, 72, "#fef2f2", RED, rx=6, sw=1.4)
        yy = iy + 34
        for ln in wrap(t, iw - 22, 9.5):
            c.text(x + 11, yy, ln, 9.5, RED_D, weight="bold"); yy += 11
        yy += 3
        for ln in wrap(d, iw - 22, 9):
            c.text(x + 11, yy, ln, 9, BODY); yy += 11

    cy = iy + 110
    c.section(M, cy, "Compensation — what runs when a transition fails after an external effect (S-19)")
    comp = [
        ["PROPOSAL_SUBMITTING", "submission timed out, effect unknown at the insurer",
         "recover by insurerRef (S-12); never resubmit", "auto, then ops task", "#11 + #14"],
        ["PAYMENT_PENDING", "customer never paid, link expired",
         "expire the session; the journey stays resumable at UW_APPROVED", "auto", "#12"],
        ["PAYMENT_UNCERTAIN", "callback lost or delayed",
         "settlement file at T+1 arbitrates; unresolved → break task", "auto, then Finance",
         "#12 + Finance"],
        ["PAYMENT_RECONCILED, issuance fails", "money taken, no policy",
         "retry issuance; if terminal, the refund path opens and the journey never reads SOLD",
         "ops task, Finance owns the money", "#13 + Finance"],
        ["CANCELLED_IN_FLIGHT after payment", "customer withdrew post-payment",
         "refund compensation; consent withdrawal recorded as its own event", "ops task",
         "#12 + #6"],
        ["Audit write failed", "business effect happened, evidence did not land",
         "outbox retry (S-17); journey completion waits — it does not proceed",
         "auto, alert on threshold", "#16"],
    ]
    c.table(M, cy + 14, ["Failing transition", "What actually happened", "Compensating action",
                         "Who runs it", "Owning context"],
            comp, [300, 380, 560, 300, 256], row_h=26)

    c.legend(M, 946, [("swatch:#dbeafe:#2563eb", "in-flight state"),
                      ("swatch:#fef3c7:#dc2626", "gate state — a control is enforced on entry"),
                      ("dash", "waiting on something outside the platform"),
                      ("swatch:#ecfdf5:#059669", "terminal — success"),
                      ("swatch:#fef2f2:#dc2626", "terminal — not a success, recorded honestly")])
    c.footer(DRAFT, FOOT_R)
    return c


# ═══════════════════════════════════════════════════════════════ 3. TRUST ZONES
def trust_zones():
    c = Canvas(1900, 1250,
               "AU Bank Insurance Distribution Platform — Trust Zones & Boundary Controls",
               "five zones · what may cross each boundary, which control enforces it, and what "
               "proves the control works · input to the S07-G3 threat model, not a substitute for it")
    W = c.w - 2 * M

    c.status_banner(M, 84, W, "This is a zone model, not a threat model",
                    "S07-G3 requires a threat model per trust boundary with data-flow "
                    "decomposition and residual-risk ratings, signed by Deepali as a human. This "
                    "diagram gives that work its boundaries and its crossing rules — it does not "
                    "discharge it.", WARN, h=50)

    # zone frames — drawn so no label ever sits under a box
    ZONES = [
        ("Z0 · Untrusted", "the internet, the customer's own device, insurer networks",
         M, 158, W, 1000, "#f8fafc", GHOST, "5 4"),
        ("Z1 · Edge", "the single public entry point — WAF · CDN · API Gateway / ALB",
         M + 300, 246, W - 600, 880, "#fff7ed", "#ea580c", None),
        ("Z2 · Private service", "EKS private subnets · no route from the internet",
         M + 330, 330, W - 660, 690, "#eff6ff", "#2563eb", None),
        ("Z3 · Data", "one owner per store · encrypted · KMS CMK hierarchy",
         M + 360, 690, W - 720, 300, "#f0fdf4", "#16a34a", None),
    ]
    for (name, sub, x, y, w, h, f, s, dash) in ZONES:
        c.rect(x, y, w, h, f, s, rx=12, sw=1.8, dash=dash)
        c.text(x + 16, y + 22, name, 12.5, INK, weight="bold")
        c.text(x + 16, y + 38, sub, 9.5, MUTED)

    # Z0 residents
    c.card(M + 20, 210, 250, 96, "Customer device",
           ["consent OTP and premium payment happen HERE",
            "one-time link · short expiry · device-bound"], CUST, ctrl="C4")
    c.card(M + 20, 330, 250, 86, "RM device (Flutter)",
           ["opaque session only (S-01)", "no OAuth token ever reaches it",
            "no interface method can take an instrument"], W4, tag="W4")
    c.card(c.w - M - 270, 210, 250, 86, "Group A insurer / 1SB",
           ["reached only from the egress path", "mTLS · fixed outbound route"], EXT)
    c.card(c.w - M - 270, 320, 250, 86, "AU Bank PG",
           ["3-D Secure on the customer device", "signed callback + T+1 settlement"], EXT)
    c.card(c.w - M - 270, 430, 250, 86, "Bank IdP / CBS",
           ["federation (IF-2) · read-only CIF lookup", "fail closed if unavailable"], EXT)

    # Z1
    c.card(M + 330, 268, W - 660, 46, "Edge — WAF · CDN · API Gateway / ALB",
           ["the ONLY public entry point · TLS terminated here · rate limits and bot controls · "
            "session validated before anything reaches a service"], WARN, title_size=11.5)

    # Z2 residents
    svc = [("#2 RM Workspace BFF", W4, "holds the session; Flutter never sees a token"),
           ("Identity + PDP", W1, "default deny · 300 ms · no retry · FAIL CLOSED (S-02)"),
           ("#9 Journey", W1, "stage + references; no other context's decision"),
           ("#6 · #7 Consent · Suitability", W2, "C1 and C2 enforced here, not at the edge"),
           ("#10 · #11 Quote · Proposal", W2, "canonical shapes only"),
           ("#12 · #13 Payment · Policy", W3, "money path; reconciliation decides"),
           ("#16 Audit", W3, "insert-only path; no update verb is exposed"),
           ("#14 Integration Hub", W1, "the only egress to a provider")]
    sw_, sgap = (W - 700) / 4, 12
    for i, (t, pal, d) in enumerate(svc):
        col, row = i % 4, i // 4
        x = M + 350 + col * (sw_ + sgap)
        y = 390 + row * 90
        c.card(x, y, sw_ - sgap, 76, t, [d], pal, title_size=10)

    # Z3 residents
    stores = [("Aurora PostgreSQL", "one store per service · Multi-AZ · PITR"),
              ("DynamoDB", "journey state · quote jobs · PITR"),
              ("S3 + Object Lock", "raw payloads · policy docs · 7-yr WORM · replicated ap-south-2"),
              ("Secrets Manager + KMS", "per-service credentials · CMK hierarchy · rotation")]
    stw = (W - 780) / 4
    for i, (t, d) in enumerate(stores):
        x = M + 390 + i * (stw + 12)
        c.cylinder(x, 740, stw - 12, 84, PAPER, W3.stroke)
        c.text(x + (stw - 12) / 2, 776, t, 10, INK, "middle", "bold")
        for j, ln in enumerate(wrap(d, stw - 34, 8.8)):
            c.text(x + (stw - 12) / 2, 790 + j * 10, ln, 8.8, FAINT, "middle")
    c.text(M + 390, 852, "No cross-service database access. ArchUnit asserts it in the build; IAM "
                         "denies it at runtime; the S09 IaC scan proves the policy is deployed "
                         "(ARCH-004).", 9.5, W3.deep, weight="bold")

    c.text(M + 390, 900, "Lower environments never receive live customer data — generated or "
                         "masked only. Personal data, backups, logs and archives stay in "
                         "ap-south-1 / ap-south-2.", 9.5, MUTED)

    # ---- boundary crossing table
    by = 1010
    c.section(M, by, "Boundary crossings — the only ways in, and what proves each control")
    rows = [
        [("Z0 → Z1", INK, "bold"), "customer / RM HTTPS request",
         "TLS · WAF · rate limit · opaque session (S-01)",
         "session-forgery and rate-limit tests", ("Pending — Deepali", "#b45309", "bold")],
        [("Z1 → Z2", INK, "bold"), "authenticated, authorised request only",
         "PDP decision required; the owning service re-checks (default deny)",
         "FF-01 · negative access tests per role and branch", ("Pending — Deepali", "#b45309", "bold")],
        [("Z2 → Z2", INK, "bold"), "service-to-service call",
         "distinct service identity, least privilege, mTLS",
         "IAM policy test; ArchUnit boundary rules", ("Pending", "#b45309", "bold")],
        [("Z2 → Z3", INK, "bold"), "a service reaching ONLY its own store",
         "per-service credentials from KMS; no shared account",
         "cross-store access attempt must fail (ARCH-004)", ("Pending — Aarti", "#b45309", "bold")],
        [("Z2 → Z0 egress", INK, "bold"), "provider traffic, from #14 only",
         "fixed outbound route · mTLS · credential rotation with emergency removal",
         "rotation exercise; egress allow-list test", ("Pending — SEC-03", "#b45309", "bold")],
        [("Z0 → Z2 callback", INK, "bold"), "PG payment callback",
         "signature + transaction match + replay window (S-14)",
         "forged, replayed and delayed callback tests", ("Pending — SEC-05", "#b45309", "bold")],
        [("Any → Z3 audit", INK, "bold"), "evidence write",
         "insert-only; UPDATE and DELETE rejected at the store",
         "FF-10 deletion-refusal test, evidence retained", ("Pending — SEC-06", "#b45309", "bold")],
    ]
    c.table(M, by + 14, ["Boundary", "What may cross", "Control that enforces it",
                         "What proves it works", "Human sign-off"],
            rows, [140, 330, 490, 480, 356], row_h=25)

    c.footer(DRAFT, FOOT_R)
    return c


BUILD = {"quote-path": quote_path, "journey-state-machine": journey_state_machine,
         "trust-zones": trust_zones}

if __name__ == "__main__":
    outdir = sys.argv[1] if len(sys.argv) > 1 else "."
    only = sys.argv[2] if len(sys.argv) > 2 else None
    for name, fn in BUILD.items():
        if only and only != name:
            continue
        p = os.path.join(outdir, name + ".svg")
        open(p, "w").write(fn().render())
        print("wrote", p, os.path.getsize(p), "bytes")
