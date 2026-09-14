#!/usr/bin/env python3
"""
Structural diagrams for the architecture pre-approval pack v0.2:

    solution-vision.svg     what is broken, what changes, and where R0 stops
    capability-map.svg      every capability, its wave, its owner, its R0 position
    domain-ownership.svg    19 bounded contexts — what each owns and does NOT own
    system-context.svg      external systems, what crosses each edge, how it fails

Built on svgkit, which carries the design system of docs/hdl.svg.

    python3 docs/diagrams/src/build_structure.py docs/diagrams
"""
import os, sys
from svgkit import (Canvas, W1, W2, W3, W4, EXT, CUST, WARN, OK, NEUT, GOV,
                    INK, BODY, MUTED, FAINT, GHOST, HAIR, BAND, WASH, PAPER, DARK, RED,
                    wrap, DRAFT)

M = 52
FOOT_R = "WS-3 · AU Bank Insurance Distribution Platform · R0"


# ═══════════════════════════════════════════════════════════ 1. SOLUTION VISION
def solution_vision():
    c = Canvas(1900, 1180,
               "AU Bank Insurance Distribution Platform — Solution Vision",
               "what the bank loses today, what the platform changes, and exactly where R0 stops · "
               "scope per DEC-20260816-03 (Product), assisted-first")
    W = c.w - 2 * M

    # ---- what is broken today
    c.section(M, 92, "What is broken today — each line is a loss the bank can measure")
    probs = [
        ("Visibility ends at redirect", "the customer leaves for the insurer's site and the bank "
         "learns the outcome later, from a file"),
        ("No single journey status", "RM, operations and the customer each hold a different "
         "version of where the case is"),
        ("Evidence is scattered", "consent, suitability, payment and issuance proof sit in four "
         "systems with no common key"),
        ("Every insurer is bespoke", "adding a product means re-cutting the journey around a "
         "partner's message format"),
        ("Breaks need people", "a failure between quote, proposal, payment and issuance is found "
         "by a human reading a report"),
        ("Reporting is unowned", "no context is the stated golden source, so no number is "
         "defensible under challenge"),
    ]
    cw = (W - 5 * 14) / 6
    for i, (t, d) in enumerate(probs):
        x = M + i * (cw + 14)
        c.card(x, 106, cw, 92, t, [d], WARN, title_size=10)

    # ---- the platform band
    c.section(M, 234, "What the platform is — five commitments that do not bend for a release date")
    c.rect(M, 248, W, 92, WASH, HAIR, rx=8, sw=1.2)
    commits = [
        ("The bank owns the journey", "partner formats are translated at one boundary (#14); no "
         "insurer message shape reaches a business context"),
        ("Ownership before decomposition", "each context owns its rules and its golden data; no "
         "context reads another's store (ARCH-004)"),
        ("Controls are structural, not procedural", "C1 suitability, C2 consent and C4 payment "
         "isolation are enforced in code paths, not in training"),
        ("Sold means sold", "issuance ∧ reconciliation ∧ audit completeness (INV-JRN-05) — no "
         "other route reaches the state"),
        ("Smallest provable release", "one journey proven end to end beats six journeys "
         "half-built; expansion is earned with evidence"),
    ]
    bw = (W - 4 * 12) / 5
    for i, (t, d) in enumerate(commits):
        x = M + i * (bw + 12)
        c.text(x + bw / 2, 272, t, 10.5, INK, "middle", "bold")
        yy = 287
        for ln in wrap(d, bw - 16, 9):
            c.text(x + bw / 2, yy, ln, 9, MUTED, "middle"); yy += 11
        if i < 4:
            c.line(x + bw + 6, 258, x + bw + 6, 330, HAIR, 1)

    # ---- what changes, with a measure
    c.section(M, 372, "What changes — and the measure that proves it (baselines are Product's to set)")
    outs = [
        ("Continuous visibility", "journey stage is a single value", "% of journeys with a stage "
         "younger than 60 s", "baseline: n/a — no platform today"),
        ("Advice is provable", "C1/C2 evidence exists before the quote", "% of quotes with a valid "
         "unexpired suitability ref", "target: 100% — a gate, not a KPI"),
        ("Money is never assumed", "reconciliation decides, not a callback", "% of payments "
         "resolved without manual touch", "baseline to be set from PG history"),
        ("Insurers become pluggable", "one seam, one canonical contract", "elapsed days to add the "
         "second Group A insurer", "target set at R1 entry"),
        ("Failures are owned", "every break becomes a named task", "% of breaks with an owner "
         "inside SLA", "target: 100% of S-19 tasks"),
        ("Numbers are defensible", "one golden source per fact", "% of report fields traceable to "
         "an owning context", "target: 100%"),
    ]
    for i, (t, how, measure, base) in enumerate(outs):
        x = M + i * (cw + 14)
        c.card(x, 386, cw, 104, t, [how], OK, title_size=10)
        c.text(x + cw / 2, 452, measure, 8.8, INK, "middle", "bold")
        for j, ln in enumerate(wrap(base, cw - 16, 8.5)):
            c.text(x + cw / 2, 466 + j * 10, ln, 8.5, FAINT, "middle", style="italic")

    # ---- release boundary
    c.section(M, 528, "Where R0 stops — the boundary is a Product decision, not an architecture preference")
    rel = [
        ("R0 — prove one journey", W3, "DEC-20260816-03 · DECIDED by Product",
         ["1 RM · 1 ETB customer · 1 Term Life product · 1 Group A insurer",
          "RM-assisted only — the customer never self-serves in R0",
          "consent (C2) and suitability (C1) mandatory and evidenced",
          "payment on the customer's own device (C4), reconciled before sold",
          "policy issued, document retrievable, audit chain complete",
          "12 services + 1 Flutter app — not the 19-context target"]),
        ("R1 — earn the second journey", W2, "DEC-20260816-04 · GAP-023 re-scoped to R1 entry",
         ["self-service (DIY) journey revisits — needs a customer identity decision",
          "second Group A insurer — proves the seam was real",
          "Group B redirect + catalogue (R0-SCOPE A5) lands here",
          "Lead module beyond a customer lookup",
          "entry condition: GATE-S11 passed for the R0 Term journey"]),
        ("R2+ — widen", NEUT, "no decision recorded — do not design for it yet",
         ["hybrid journey (RM starts, customer completes)",
          "further life products, then further lines of business",
          "servicing, renewal, claims initiation",
          "multi-partner routing and campaign management",
          "customer BFF, reporting & MIS, admin UI"]),
    ]
    rw = (W - 2 * 16) / 3
    for i, (t, pal, cite, items) in enumerate(rel):
        x = M + i * (rw + 16)
        h = 200
        c.rect(x, 542, rw, h, pal.fill, pal.stroke, rx=8, sw=1.6)
        c.text(x + 14, 566, t, 13, INK, weight="bold")
        c.text(x + 14, 582, cite, 9, pal.deep, weight="bold")
        yy = 602
        for it in items:
            c.text(x + 14, yy, "•", 9, pal.stroke, weight="bold")
            for ln in wrap(it, rw - 40, 9.5):
                c.text(x + 26, yy, ln, 9.5, BODY); yy += 12
            yy += 3

    # ---- explicitly not in R0
    c.section(M, 776, "Explicitly NOT in R0 — written down so it is a decision, never a discovery")
    nots = ["broad self-service", "multiple lines of business", "full claims handling",
            "campaign management", "advanced MIS", "multi-partner routing",
            "Group B redirect", "admin UI (config is versioned artefacts)",
            "Lead management beyond lookup", "renewal & servicing"]
    nw = (W - 9 * 10) / 10
    for i, t in enumerate(nots):
        x = M + i * (nw + 10)
        c.rect(x, 790, nw, 40, PAPER, GHOST, rx=6, sw=1.3, dash="4 3")
        for j, ln in enumerate(wrap(t, nw - 14, 9)):
            c.text(x + nw / 2, 808 + j * 11 - (5 if len(wrap(t, nw - 14, 9)) > 1 else 0),
                   ln, 9, MUTED, "middle")

    # ---- the decision this document asks for
    c.section(M, 872, "What this document asks stakeholders to decide")
    asks = [
        ("V-01", "Confirm the target vision and the bank-owned-journey principle", "Rajal + Mahesh"),
        ("V-02", "Confirm the R0 boundary as stated (re-affirms DEC-20260816-03)", "Rajal"),
        ("V-03", "Confirm C1/C2/C4 and INV-JRN-05 as non-waivable by any authority", "Shailja + Deepali"),
        ("V-04", "Confirm phased delivery, and that R1 scope is not designed for in R0", "Rajal + Kalpana"),
        ("V-05", "Confirm the measures above are the ones the pilot is judged on", "Rajal + Dilip (sponsor seat — GAP-010)"),
    ]
    y = c.table(M, 886, ["ID", "Decision requested", "Named decision owner", "State"],
                [[a, b, cc, ("Pending — no signature", "#b45309", "bold")] for a, b, cc in asks],
                [70, 760, 480, 486], row_h=24)

    c.status_banner(M, y + 14, W, "Not approved · not authorised for development",
                    "A review comment is not an approval. This diagram records a proposal; every "
                    "decision above stays Pending until the named human signs it.", WARN, h=42)
    c.footer(DRAFT, FOOT_R)
    return c


# ══════════════════════════════════════════════════════════════ 2. CAPABILITY MAP
def capability_map():
    c = Canvas(1900, 992,
               "AU Bank Insurance Distribution Platform — Business Capability Map",
               "every capability the platform needs, the context that will own it, its build wave, "
               "and whether R0 actually delivers it")

    W = c.w - 2 * M
    GROUPS = [
        ("Sell", W2, [
            ("Customer & lead", "#4 Customer · #5 Lead", "R0: ETB lookup + prefill only. Lead "
             "module deferred to S13 — a journey can start from a lookup", "partial", "W1"),
            ("Advice & protection", "#6 Consent · #7 Suitability", "R0: FULL. C1 and C2 are hard "
             "gates — 42 consent rules, 62 suitability rules (PR #54 v1.1)", "in", "W2"),
            ("Product catalogue", "#8 Product Catalogue", "R0: Term Life only, one Group A "
             "insurer. Group B redirect is R1 (R0-SCOPE A5)", "partial", "W1"),
            ("Quotation", "#10 Quotation", "R0: FULL. Refuses without a valid suitability ref "
             "(403). Ordering by disclosed basis only (QR-07)", "in", "W2"),
        ]),
        ("Convert", W3, [
            ("Proposal & underwriting", "#11 Proposal & UW", "R0: submit + track. The insurer "
             "keeps the underwriting decision — the bank tracks it", "in", "W3"),
            ("Payment", "#12 Payment", "R0: FULL. Customer device only (C4); reconciliation, not "
             "the callback, decides", "in", "W3"),
            ("Policy & issuance", "#13 Policy & Issuance", "R0: FULL. Issues only against a "
             "RECONCILED payment (SC-W3-4)", "in", "W3"),
            ("Journey control", "#9 Journey Orchestration", "R0: FULL. Stage, resumption, "
             "compensation (S-19). Holds references, never another context's decision", "in", "W1"),
        ]),
        ("Connect", W1, [
            ("Partner integration", "#14 Hub → #15 1SB", "R0: one route. Hub exists to be "
             "built; the 1SB adapter EXISTS TODAY (WS-1)", "in", "W1"),
            ("Identity & access", "#3 Identity (workforce half)", "R0: workforce only, via WS-2. "
             "Customer identity for self-service is an R1 blocker", "partial", "W1"),
            ("Communications", "#17 Notification", "R0: transactional minimum — OTP and the "
             "payment link. A notification failure never blocks the journey", "in", "W4"),
            ("Administration", "Config artefacts, not a service", "R0: versioned artefacts "
             "consumed at startup. A rule change needs a deploy until S13 — a recorded trade", "partial", "—"),
        ]),
        ("Evidence & run", W3, [
            ("Audit & compliance", "#16 Audit & Compliance", "R0: FULL. Append-only, "
             "UPDATE/DELETE rejected (FF-10), 7-year WORM in ap-south-1", "in", "W3"),
            ("Operations work", "Ops queue on #9", "R0: minimum — journey and payment exceptions "
             "only. Every failed auto-recovery becomes an owned task", "partial", "W3"),
            ("Reporting & MIS", "#18 Reporting", "R0: pilot measures only, from approved copies. "
             "Reporting can never write to a transaction context", "partial", "—"),
            ("Platform operation", "S08 + S09 foundation", "REQUIRED BEFORE ANY OF THE ABOVE. "
             "CI, IaC, secrets, observability, retention — GATE-S08 is open", "gate", "W0"),
        ]),
        ("Later", NEUT, [
            ("Policy servicing", "#19 (not in R0)", "endorsement, nominee change, cancellation, "
             "refund — R2+", "out", "—"),
            ("Renewal", "not in R0", "renewal notice, quote, payment, confirmation — R2+", "out", "—"),
            ("Claims", "not in R0", "notification, documents, insurer hand-off — R2+. The claims "
             "boundary is an open Product decision", "out", "—"),
            ("Campaigns & bulk", "not in R0", "bulk lead load, campaign management — R2+", "out", "—"),
        ]),
    ]

    STATE = {"in":      ("R0 — delivered in full",  OK.fill,   OK.stroke,   OK.deep),
             "partial": ("R0 — deliberately thin",  W2.fill,   W2.stroke,   W2.deep),
             "gate":    ("Foundation — blocks all", WARN.fill, WARN.stroke, WARN.deep),
             "out":     ("Not in R0",               "#ffffff", GHOST,       MUTED)}

    c.section(M, 92, "Capability → owning bounded context → R0 position. "
                     "The third line is the one that matters: what R0 actually gives you")
    colw = (W - 4 * 14) / 5
    for gi, (gname, gpal, caps) in enumerate(GROUPS):
        x = M + gi * (colw + 14)
        c.rect(x, 106, colw, 34, gpal.fill, gpal.stroke, rx=7, sw=1.5)
        c.text(x + colw / 2, 128, gname, 13, INK, "middle", "bold")
        y = 150
        for (name, ctx, note, st, wave) in caps:
            label, fill, stroke, deep = STATE[st]
            body = wrap(note, colw - 20, 9)
            h = 56 + len(body) * 11
            c.rect(x, y, colw, h, fill, stroke, rx=6,
                   dash="4 3" if st == "out" else None, sw=1.4)
            c.text(x + 12, y + 20, name, 11, INK, weight="bold")
            c.text(x + 12, y + 34, ctx, 9, deep, weight="bold")
            yy = y + 48
            for ln in body:
                c.text(x + 12, yy, ln, 9, BODY); yy += 11
            if wave not in ("—",):
                c.tag(x + colw - 27, y + 7, wave, gpal.stroke if wave != "W0" else WARN.stroke)
            y += h + 12

    # ---- ownership: business owner vs technical owner
    ytop = 528
    c.section(M, ytop, "Business ownership — who owns the MEANING of the rule, "
                       "which is not who runs the service")
    rows = [
        ["Consent & suitability rules", "Rajal (Product) — business behaviour",
         "Shailja (Compliance) — permissibility, wording, calibration",
         ("CONSENT-PACK-v1.0 · SUIT-ALGO-LIFE-v1.1", INK, "bold"), "DEC-20260816-07"],
        ["Product & insurer matrix", "Rajal (Product)", "Partner management · Mahesh (seam)",
         "R0: Term, Group A, one insurer", "R0-SCOPE A4"],
        ["Payment & reconciliation", "Finance + Rajal (jointly)", "Shivanshi (run) · Deepali (callback trust)",
         ("Finance holds no signature seat today — GAP", "#b45309", "bold"), "F-55"],
        ["Audit & evidence model", "Shailja (Compliance)", "Aarti (integrity) · Deepali (immutability)",
         "7-year WORM · FF-10 deletion-refusal test", "S06-G7 (E3)"],
        ["Journey behaviour & states", "Rajal (Product)", "Mahesh (structure) · Swapnali (testability)",
         "no state machine published yet", "S06-G2/G3"],
        ["NFRs and capacity", "Mahesh (architecture)", "Shivanshi (feasibility) · Rajal (derivation)",
         "ws3-platform/05-nfr-catalogue.md", "S07-G6"],
        ["Data model & retention", "Aarti (Data/DBA)", "Shailja (retention basis) · Deepali (classification)",
         "7-year, ap-south-1, legal hold", "S07-G5"],
    ]
    c.table(M, ytop + 14, ["Capability area", "Business owner (owns the meaning)",
                           "Must be consulted / co-signs", "Artefact that carries it today",
                           "Gate or decision"],
            rows, [300, 330, 400, 490, 276], row_h=26)

    # ---- gaps needing a decision
    gy = 786
    c.section(M, gy, "Capability gaps that need a named human decision before the affected build starts")
    gaps = [
        ("G-01", "Customer identity for self-service", "blocks R1 entry, not R0",
         "Rajal + Deepali + Mahesh", WARN),
        ("G-02", "Claims boundary — track, start or manage?", "blocks any claims design",
         "Rajal", NEUT),
        ("G-03", "Payment settlement timing & break ownership", "blocks F1/F3 runbooks",
         "Finance + Rajal", WARN),
        ("G-04", "Retention & disposal schedule per data class", "blocks S07-G5 sign-off",
         "Shailja + Aarti", WARN),
        ("G-05", "Group B redirect model — who owns the journey after handover?",
         "blocks R1 catalogue", "Rajal + Mahesh", NEUT),
        ("G-06", "Product matrix beyond the first insurer", "blocks R1 sizing", "Rajal", NEUT),
    ]
    gw = (W - 5 * 12) / 6
    for i, (gid, t, blocks, who, pal) in enumerate(gaps):
        x = M + i * (gw + 12)
        c.rect(x, gy + 14, gw, 96, pal.fill, pal.stroke, rx=6, sw=1.4)
        c.text(x + 11, gy + 33, gid, 9.5, pal.deep, weight="bold")
        yy = gy + 47
        for ln in wrap(t, gw - 22, 9.5):
            c.text(x + 11, yy, ln, 9.5, INK, weight="bold"); yy += 11
        yy += 2
        for ln in wrap(blocks, gw - 22, 9):
            c.text(x + 11, yy, ln, 9, BODY); yy += 11
        c.text(x + 11, gy + 102, who, 8.5, FAINT, style="italic")

    c.legend(M, 944, [("swatch:#dcfce7:#16a34a", "R0 in full"),
                       ("swatch:#fef3c7:#d97706", "R0 deliberately thin"),
                       ("swatch:#fff7ed:#ea580c", "foundation — blocks everything"),
                       ("dash", "not in R0"),
                       ("text", "")])
    c.text(M + 980, 950, "#n bounded context · W0–W4 build wave · gate IDs per "
                          "application-lifecycle-bible/stages", 9.5, MUTED)
    c.footer(DRAFT, FOOT_R)
    return c


# ═══════════════════════════════════════════════════════════ 3. DOMAIN OWNERSHIP
def domain_ownership():
    c = Canvas(1900, 1240,
               "AU Bank Insurance Distribution Platform — Domain & Ownership Model",
               "19 bounded contexts · what each one owns, what it must NEVER own, and the single "
               "rule that keeps the boundary real")

    W = c.w - 2 * M
    c.rect(M, 84, W, 46, WASH, HAIR, rx=7, sw=1.2)
    c.text(M + 16, 104, "ARCH-004 — one owner per data store. A context reaches another context's "
                        "data over an HTTP contract, never by reading its store.", 12, INK,
           weight="bold")
    c.text(M + 16, 121, "The 'does not own' column is the load-bearing one: most integration "
                        "defects are a context quietly taking a decision that belongs to its "
                        "neighbour. Enforced by ArchUnit + IAM, verified in the S09 IaC scan.",
           10, MUTED)

    CTX = [
        # (n, name, pal, owns, never owns, wave)
        (3,  "Identity & Access", W1, "workforce identity links, roles, branch and insurer scope, certification status",
         "customer, policy or payment data — an IdP claim is never a business authorization", "W1"),
        (2,  "RM Workspace BFF", W4, "server-held session and screen state; the token-hiding seam",
         "any insurance business decision — it renders decisions, it does not take them", "W4"),
        (4,  "Customer", W1, "the insurance-use customer snapshot, its source and its freshness",
         "the core banking customer master — CBS remains golden for the customer record", "W1"),
        (5,  "Lead", W1, "lead, assignment, source, lead status",
         "customer master or journey state — a lead is a sales artefact, not the journey", "S13"),
        (6,  "Consent", W2, "consent wording version, customer confirmation, timestamp, withdrawal state",
         "suitability or proposal rules — it records permission, it does not judge advice", "W2"),
        (7,  "Suitability", W2, "inputs, rule-pack version, outcome, reasons, validity window",
         "the product catalogue or the insurer's underwriting decision", "W2"),
        (8,  "Product Catalogue", W1, "product, insurer, eligibility, channel, effective dates",
         "live quote or proposal state — it defines what may be sold, not what is being sold", "W1"),
        (9,  "Journey Orchestration", W1, "journey stage, references, next action, failure state, compensation tasks",
         "another context's business decision or its detailed records (SC-W3-6)", "W1"),
        (10, "Quotation", W2, "quote request, offers, expiry, selected offer, ordering basis (QR-07)",
         "the suitability result or the partner's product definition", "W2"),
        (11, "Proposal & UW Tracking", W3, "proposal, documents, submission state, insurer requirements",
         "the insurer's underwriting decision — it tracks it, it never makes it", "W3"),
        (12, "Payment & Reconciliation", W3, "payment session, result, settlement match, reconciliation state",
         "policy issuance — and it never treats a callback as proof of settlement", "W3"),
        (13, "Policy & Issuance", W3, "policy record, issue status, document references, sold-status evidence",
         "payment settlement — it consumes RECONCILED, it does not decide it", "W3"),
        (14, "Integration Hub", W1, "provider routing, canonical translation, request status, technical correlation",
         "bank business rules or journey meaning — no policy logic lives at the seam", "W1"),
        (15, "1SB Adapter", EXT, "provider protocol, 24 h idempotency contract (INV-ACL-01)",
         "anything beyond adapter.onesb.* — EXISTS TODAY, owned by WS-1 as supplier", "WS-1"),
        (16, "Audit & Compliance", W3, "actor, action, time, purpose, before/after meaning, evidence chain",
         "operational business decisions — it is the record, never the decider", "W3"),
        (17, "Notification", W4, "message request, channel, delivery status, template version",
         "payment or journey decisions — a delivery failure never blocks a journey", "W4"),
        (18, "Reporting & MIS", NEUT, "approved copies, measures, calculation versions",
         "source transaction records — it has NO write path into any transaction context", "S13"),
        (19, "Administration & Config", NEUT, "approved configuration, rule publication, effective dates",
         "business approval of a product or a compliance rule — it publishes, it does not approve", "S13"),
        (1,  "Customer BFF", NEUT, "(not in R0) customer-facing session for self-service",
         "deferred with the DIY journey to R1 — no design commitment made in R0", "S13"),
    ]

    c.section(M, 150, "Context · owns (golden source) · must never own")
    colw = (W - 3 * 14) / 4
    rows_per_col = 5
    y0 = 166
    for i, (n, name, pal, owns, never, wave) in enumerate(CTX):
        col, row = i // rows_per_col, i % rows_per_col
        x = M + col * (colw + 14)
        ow = wrap("OWNS · " + owns, colw - 24, 9)
        nw = wrap("NEVER · " + never, colw - 24, 9)
        h = 40 + (len(ow) + len(nw)) * 11 + 8
        y = y0 + row * 122
        c.rect(x, y, colw, h, pal.fill, pal.stroke, rx=6, dash=pal.dash, sw=1.4)
        c.text(x + 12, y + 21, f"#{n} {name}", 11, INK, weight="bold")
        c.tag(x + colw - 30, y + 7, wave, pal.stroke if wave.startswith("W") else GHOST)
        yy = y + 38
        for ln in ow:
            c.text(x + 12, yy, ln, 9, pal.deep); yy += 11
        yy += 4
        for ln in nw:
            c.text(x + 12, yy, ln, 9, "#b91c1c"); yy += 11

    # ---- interaction rules
    ry = 790
    c.section(M, ry, "Interaction rules — each one is testable, and each has a fitness function or an invariant")
    rules = [
        ("Every protected context re-checks the caller", "the gateway checking is not enough — "
         "the owning context checks again (default deny, FAIL CLOSED, S-02)", "FF-01"),
        ("Quotation cannot proceed without valid suitability", "no valid, unexpired suitability "
         "ref → 403. Not a warning, not an override", "C1 · FF-08"),
        ("Proposal cannot proceed without current consent", "withdrawn or stale consent stops the "
         "submission and the journey says why", "C2 · FF-09"),
        ("All insurer traffic passes through #14", "no context holds an insurer credential or "
         "speaks a provider format", "SC-W3-5"),
        ("Journey holds references, never decisions", "#9 records that suitability passed — it "
         "never stores what suitability decided", "SC-W3-6"),
        ("Issuance requires a reconciled payment", "and SOLD requires issuance ∧ reconciliation ∧ "
         "audit completeness", "SC-W3-4 · INV-JRN-05"),
        ("Audit is append-only", "UPDATE and DELETE are rejected at the store; the refusal is "
         "itself a test", "FF-10"),
        ("Reporting reads approved copies only", "no write path exists from #18 into a "
         "transaction context — absence proven, not promised", "FF-15"),
    ]
    rw = (W - 3 * 12) / 4
    for i, (t, d, fid) in enumerate(rules):
        x = M + (i % 4) * (rw + 12)
        y = ry + 14 + (i // 4) * 88
        c.rect(x, y, rw, 76, PAPER, HAIR, rx=6, sw=1.3)
        c.text(x + 12, y + 20, t, 10, INK, weight="bold")
        yy = y + 35
        for ln in wrap(d, rw - 120, 9):
            c.text(x + 12, yy, ln, 9, MUTED); yy += 11
        c.rect(x + rw - 104, y + 8, 96, 17, WASH, HAIR, rx=4, sw=1)
        c.text(x + rw - 56, y + 20, fid, 8.5, INK, "middle", "bold")

    # ---- boundary decisions
    by = 990
    c.section(M, by, "Boundary decisions that need a signature — proposed position and who must review it")
    dec = [
        ["B-01", "Journey Orchestration owns progress and references only",
         "Mahesh (AP) · Rajal (RV) · Amit (RV)", "Proposed", "SC-W3-6"],
        ["B-02", "All insurer and provider traffic passes through the Hub seam",
         "Mahesh (AP) · Deepali (AP security) · Shivanshi (RV)", "Proposed", "SC-W3-5"],
        ["B-03", "One owner per data store; cross-context DB access is prohibited",
         "Mahesh (A/AP) · Aarti (RV/AP) · Deepali (RV)", "Proposed", "ARCH-004"],
        ["B-04", "bank-persistence-service stays scoped to the integration job store and audit "
                 "ingestion — it is NOT extended to the R0 business contexts",
         "Mahesh (AP) · Aarti (AP) · Amit (C)", ("Needs an explicit ADR", "#b45309", "bold"),
         "reconciles ARCH-004"],
        ["B-05", "RM devices cannot perform premium payment — no interface method exists",
         "Rajal · Deepali (AP) · Shailja (AP) · Mahesh", "Enforced in code", "C4 · DEC-20260816-12"],
        ["B-06", "Regulated evidence cannot be altered or deleted inside its retention period",
         "Shailja (AP) · Deepali (AP) · Aarti (AP)", "Proposed", "FF-10 · S06-G7"],
        ["B-07", "Reporting uses approved copies and holds no write path",
         "Mahesh (AP) · Aarti (RV) · Rajal (C)", "Proposed", "FF-15"],
    ]
    c.table(M, by + 14, ["ID", "Proposed boundary position", "Required reviewers (authority matrix)",
                         "State", "Anchor"],
            dec, [70, 700, 520, 260, 246], row_h=25)

    c.footer(DRAFT, FOOT_R)
    return c


# ═══════════════════════════════════════════════════════════ 4. SYSTEM CONTEXT
def system_context():
    c = Canvas(1900, 1080,
               "AU Bank Insurance Distribution Platform — System Context",
               "every system the platform touches · what crosses the edge, under which contract, "
               "and what happens when it does not answer")

    W = c.w - 2 * M
    CX, CY = c.w / 2, 560

    # platform core
    c.rect(CX - 300, CY - 130, 600, 260, W1.fill, W1.stroke, rx=12, sw=2)
    c.text(CX, CY - 100, "AU Bank Insurance Platform", 17, INK, "middle", "bold")
    c.text(CX, CY - 80, "WS-3 · 12 services + 1 Flutter app at R0", 10, W1.deep, "middle",
           weight="bold")
    inner = [("#9 Journey", W1), ("#6/#7 Consent · Suitability", W2), ("#10/#11 Quote · Proposal", W2),
             ("#12/#13 Payment · Policy", W3), ("#14 Integration Hub", W1), ("#16 Audit (WORM)", W3)]
    for i, (t, pal) in enumerate(inner):
        col, row = i % 2, i // 2
        x = CX - 285 + col * 288
        y = CY - 62 + row * 46
        c.rect(x, y, 276, 38, pal.fill, pal.stroke, rx=5, sw=1.3)
        c.text(x + 138, y + 23, t, 10, INK, "middle", "bold")
    c.text(CX, CY + 108, "ap-south-1 · private subnets · one public entry point · "
                         "DR warm standby ap-south-2", 9.5, MUTED, "middle")

    # externals: (label, sub, x, y, pal, what crosses, contract, failure)
    EXTS = [
        ("Customer", "own device — its own trust zone", M, 140, CUST,
         "consent OTP · payment link · policy document",
         "one-time link, short expiry, device-bound",
         "no link delivery → ops task (S-19); the journey never auto-completes"),
        ("Relationship Manager", "Flutter app · token-hiding session", M, 330, W4,
         "the assisted journey — every action attributed to a named RM",
         "opaque session (S-01); tokens never reach the device",
         "session lost → resume at the recorded stage; nothing is re-submitted"),
        ("Branch & insurance ops", "exception and break handling", M, 520, W3,
         "work items, reconciliation breaks, manual recovery",
         "every failed auto-recovery becomes an owned task (S-19)",
         "unowned break → escalation; a break is never closed by ageing"),
        ("Bank workforce identity", "Keycloak-first, provider-neutral · WS-2", M, 710, EXT,
         "who the RM is",
         "federation via identity-provider-adapter (IF-2)",
         "cannot confirm identity → access denied, FAIL CLOSED"),
        ("Bank authorization (PDP)", "RBAC + ABAC · default deny", M, 880, EXT,
         "what this RM may do, for this customer, for this insurer",
         "300 ms budget, no retry (S-02)",
         "no decision returned → denied. Silence is never assent"),

        ("Core Banking (CBS)", "system of record for the customer", c.w - M - 340, 140, EXT,
         "CIF · ETB customer identity and prefill",
         "read-only lookup (S-04 / S-05); CBS stays golden",
         "unavailable → recent approved snapshot, else the journey waits"),
        ("AU Bank Payment Gateway", "3-D Secure · settlement file", c.w - M - 340, 330, EXT,
         "payment session, authenticated callback, T+1 settlement",
         "signature + txn match + replay window (S-14 / S-15)",
         "no callback → UNCERTAIN; the settlement file is the arbiter, never optimism"),
        ("1SilverBullet (aggregator)", "Group A insurers · EXISTS TODAY", c.w - M - 340, 520, EXT,
         "quote, proposal, underwriting status, issuance",
         "canonical contract at the Hub; 24 h idempotency (INV-ACL-01)",
         "silent → recover by insurerRef (S-12); never a blind resubmit"),
        ("Messaging provider", "SMS · email", c.w - M - 340, 710, EXT,
         "OTP and payment-link delivery to the customer only",
         "sender approval; delivery status tracked",
         "delivery failure → ops task; the message is never silently dropped"),
        ("Compliance · audit · risk", "reads evidence, does not write it", c.w - M - 340, 880, EXT,
         "reconstruction of any journey, on demand",
         "append-only chain, 7-year WORM retention",
         "incomplete evidence → the journey cannot reach SOLD (INV-JRN-05)"),
    ]

    for (name, sub, x, y, pal, crosses, contract, fail) in EXTS:
        w, h = 340, 150
        c.rect(x, y, w, h, pal.fill, pal.stroke, rx=8, dash=pal.dash, sw=1.5)
        c.text(x + 14, y + 24, name, 12, INK, weight="bold")
        c.text(x + 14, y + 39, sub, 9, pal.deep, weight="bold")
        yy = y + 57
        for lab, val, col in (("CROSSES", crosses, BODY), ("CONTRACT", contract, MUTED),
                              ("ON FAILURE", fail, "#b91c1c")):
            c.text(x + 14, yy, lab, 8, FAINT, weight="bold", ls="0.5")
            yy += 11
            for ln in wrap(val, w - 28, 9):
                c.text(x + 14, yy, ln, 9, col); yy += 10.5
            yy += 3
        # connector to the core
        left = x < CX
        ex = x + w if left else x
        c.line(ex, y + h / 2, CX - 300 if left else CX + 300,
               CY - 60 + (y - 140) * 0.30, GHOST, 1.4, dash="6 4", marker="arrGray")

    c.section(M, 108, "The bank's side")
    c.section(c.w - M - 340, 108, "Systems the platform depends on but does not control")

    c.status_banner(M, 1000, W,
                    "Every external edge fails closed, and every failure has a named landing place",
                    "No edge above degrades into an assumption. Identity that cannot be confirmed "
                    "denies access; a payment that cannot be reconciled stays UNCERTAIN; an "
                    "insurer that does not answer is recovered by reference. The platform never "
                    "converts silence into success.", OK, h=52)
    c.footer(DRAFT, FOOT_R)
    return c


BUILD = {"solution-vision": solution_vision, "capability-map": capability_map,
         "domain-ownership": domain_ownership, "system-context": system_context}

if __name__ == "__main__":
    outdir = sys.argv[1] if len(sys.argv) > 1 else "."
    only = sys.argv[2] if len(sys.argv) > 2 else None
    for name, fn in BUILD.items():
        if only and only != name:
            continue
        p = os.path.join(outdir, name + ".svg")
        open(p, "w").write(fn().render())
        print("wrote", p, os.path.getsize(p), "bytes")
