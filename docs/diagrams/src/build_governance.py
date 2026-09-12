#!/usr/bin/env python3
"""
Governance diagrams for the architecture pre-approval pack v0.2:

    risk-register.svg    L×I exposure grid on the repository's own 1–3 scale, plus the criticals
    dependency-map.svg   every dependency with a named owner and a required-by date
    approval-model.svg   the actual AIGEM seven-board model — not a five-step CTO flow
    gate-coverage.svg    S06/S07 criteria: what is met, what is open, what needs a human

    python3 docs/diagrams/src/build_governance.py docs/diagrams
"""
import os, sys
from svgkit import (Canvas, W1, W2, W3, W4, EXT, CUST, WARN, OK, NEUT, GOV,
                    INK, BODY, MUTED, FAINT, GHOST, HAIR, BAND, WASH, PAPER, DARK, RED, RED_D,
                    wrap, DRAFT)

M = 52
FOOT_R = "WS-3 · AU Bank Insurance Distribution Platform · R0"


# ═══════════════════════════════════════════════════════════════ 1. RISK REGISTER
def risk_register():
    c = Canvas(1900, 1170,
               "AU Bank Insurance Distribution Platform — Architecture Risk Exposure",
               "scored on the repository's own scale (05-PRIORITY_MODEL: exposure = likelihood × "
               "impact, each 1–3) · every risk carries ONE named owner and an escalation trigger")
    W = c.w - 2 * M

    c.rect(M, 84, W, 44, WASH, HAIR, rx=7, sw=1.2)
    c.text(M + 16, 104, "These are architecture risks. They belong in RISK-REGISTER.md under "
                        "RISK-nnn IDs — this diagram is the view, not a second register.", 12, INK,
           weight="bold")
    c.text(M + 16, 121, "Response bands: 1–2 accept and monitor · 3–4 mitigate this stage · "
                        "6 mitigate now (P1/P2 work item) · 9 escalate to PO and Architect "
                        "immediately.", 10, MUTED)

    # ---- the grid
    GX, GY, CELL = M + 118, 190, 150
    LIK = ["1 · Unlikely in this stage", "2 · Plausible", "3 · Expected without action"]
    IMP = ["1 · Contained", "2 · Material", "3 · Severe"]
    RISKS = {  # (likelihood, impact): [(id, short)]
        (3, 3): [("AR-01", "consent/suitability signature"), ("AR-02", "external access delayed")],
        (2, 3): [("AR-03", "duplicate proposal/payment"), ("AR-04", "payment state uncertain"),
                 ("AR-05", "sold before reconciled"), ("AR-06", "PII in logs"),
                 ("AR-07", "retention/location wrong"), ("AR-08", "backup not restorable")],
        (3, 2): [("AR-09", "partner limits unknown"), ("AR-10", "self-service identity undecided")],
        (2, 2): [("AR-11", "partner latency breaks RM flow"), ("AR-12", "too many services"),
                 ("AR-13", "partner format leaks inward"), ("AR-14", "capacity without limits"),
                 ("AR-15", "rules change unversioned")],
        (1, 3): [("AR-16", "audit alterable")],
        (2, 1): [("AR-17", "config-only admin friction")],
        (1, 2): [("AR-18", "diagram/doc drift")],
    }
    BAND = {9: ("#fee2e2", RED), 6: ("#ffedd5", "#ea580c"), 4: ("#fef3c7", "#d97706"),
            3: ("#fef3c7", "#d97706"), 2: ("#ecfdf5", "#059669"), 1: ("#ecfdf5", "#059669")}

    for li in range(3):
        for im in range(3):
            L, I = li + 1, im + 1
            exp = L * I
            f, s = BAND[exp]
            x = GX + im * CELL
            y = GY + (2 - li) * CELL
            c.rect(x, y, CELL - 6, CELL - 6, f, s, rx=6, sw=1.4)
            c.text(x + 8, y + 17, f"exposure {exp}", 8.5, s, weight="bold")
            items = RISKS.get((L, I), [])
            yy = y + 32
            for (rid, short) in items:
                c.text(x + 8, yy, rid, 9, INK, weight="bold")
                for ln in wrap(short, CELL - 56, 8.3):
                    c.text(x + 40, yy, ln, 8.3, BODY); yy += 9.5
                yy += 3
    for im, t in enumerate(IMP):
        c.text(GX + im * CELL + (CELL - 6) / 2, GY - 12, t, 9.5, INK, "middle", "bold")
    for li, t in enumerate(LIK):
        y = GY + (2 - li) * CELL + (CELL - 6) / 2
        for j, ln in enumerate(wrap(t, 84, 9)):
            c.text(GX - 12, y - 4 + j * 11, ln, 9, INK, "end", "bold")
    c.text(GX + CELL * 1.5, GY + CELL * 3 + 16, "IMPACT →", 9.5, FAINT, "middle", weight="bold")

    # ---- criticals detail
    dx = GX + CELL * 3 + 40
    c.section(dx, 176, "Exposure 6 and above — mitigate now, with a named owner")
    CRIT = [
        ("AR-01", "Consent and suitability rule packs carry no human Compliance signature",
         "Shailja", "content exists (42 + 62 rules, PR #54); the E2 signature does not. "
         "S11 entry is bound on GAP-006/007 (DEC-20260816-06, non-waivable)",
         "GATE-S08 passes with both still open"),
        ("AR-02", "Core banking, payment or insurer access is not contracted in time",
         "Kalpana", "every dependency in the map has an owner; four have no required-by date "
         "agreed. A dependency without a date is not managed",
         "any D-item passes its date with no escalation raised"),
        ("AR-09", "Partner service limits and maintenance windows are unknown",
         "Shivanshi", "bulkheads are designed but sized against a guess. Capacity numbers derive "
         "from CAP-A1…A3, which are assumptions, not baselines",
         "load test scheduled before written limits arrive"),
        ("AR-10", "Customer identity for self-service is undecided",
         "Rajal + Deepali", "does not block R0 (assisted-only, DEC-20260816-03) but blocks R1 "
         "entry. Deciding it late forces a session redesign",
         "R1 planning starts with the decision still open"),
    ]
    yy = 192
    for (rid, t, owner, why, trigger) in CRIT:
        h = 122
        c.rect(dx, yy, W - (dx - M), h, "#fff7ed", "#ea580c", rx=7, sw=1.4)
        c.text(dx + 12, yy + 21, f"{rid} — {t}", 11, INK, weight="bold")
        c.text(dx + 12, yy + 37, f"Owner: {owner}", 9.5, "#9a3412", weight="bold")
        y2 = yy + 53
        for ln in wrap(why, W - (dx - M) - 24, 9.5):
            c.text(dx + 12, y2, ln, 9.5, BODY); y2 += 12
        c.text(dx + 12, yy + h - 12, f"ESCALATE IF: {trigger}", 9, RED_D, weight="bold")
        yy += h + 10

    # ---- what is NOT a risk any more
    ny = 748
    c.section(M, ny, "Closed since the v0.1 pack — restating these as open risks would be wrong")
    CLOSED = [
        ("R-13 was: 'delivery begins before automated checks work'",
         "CI now runs tests + coverage floors, CodeQL SAST, Trivy SCA, CycloneDX SBOM and "
         "gitleaks; ten SCA findings cleared 2026-08-17. Residual risk is gate coverage, not absence."),
        ("A-01 was: 'first release scope unconfirmed'",
         "DEC-20260816-03 — DECIDED by Product. Assisted-first; DIY at R1, hybrid at R2. "
         "Outstanding is the R0-SCOPE v0.4 republish (condition C4), not the decision."),
        ("'payment on customer device' was: Pending",
         "DEC-20260816-12 — enforced structurally in apps/rm-workspace-app: no interface method "
         "or widget can accept an instrument. 105 tests pass. The backend half remains to build."),
        ("'retention periods not approved'",
         "7-year write-once in ap-south-1 is named in CURRENT-STATE.yaml as an S09 deliverable; "
         "ASM-005 tracks the regime question. The gap is the signature, not the number."),
    ]
    cw = (W - 3 * 12) / 4
    for i, (t, d) in enumerate(CLOSED):
        x = M + i * (cw + 12)
        c.rect(x, ny + 14, cw, 118, OK.fill, OK.stroke, rx=6, sw=1.4)
        y2 = ny + 34
        for ln in wrap(t, cw - 24, 9.5):
            c.text(x + 12, y2, ln, 9.5, OK.deep, weight="bold"); y2 += 11
        y2 += 4
        for ln in wrap(d, cw - 24, 9):
            c.text(x + 12, y2, ln, 9, BODY); y2 += 11

    # ---- assumptions
    ay = 902
    c.section(M, ay, "Assumptions the architecture rests on — each with the consequence if it is false")
    rows = [
        ["ASM-A1", "Initial volume is modest; correctness matters more than throughput",
         "capacity and cost model change; bulkhead sizing revisited", "Rajal + Shivanshi",
         ("CAP-A1…A3 in 05-nfr-catalogue", INK, None), ("Unconfirmed", "#b45309", "bold")],
        ["ASM-A2", "The PG supplies authenticated callbacks AND a T+1 settlement file",
         "the entire reconciliation design changes; F1/F3 have no arbiter", "Deepali + Finance",
         "S-14 / S-15", ("Unconfirmed", "#b45309", "bold")],
        ["ASM-A3", "1SB supports status recovery after an uncertain submission",
         "duplicate-submission risk becomes unmitigable; AR-03 rises to exposure 9",
         "WS-1 owner", "S-12 · INV-ACL-01", ("Unconfirmed", "#b45309", "bold")],
        ["ASM-A4", "Protected data can be hosted and recovered entirely in India",
         "hosting and DR design change; cost model reopens", "Shailja + Shivanshi",
         "ap-south-1 / ap-south-2", ("Unconfirmed", "#b45309", "bold")],
        ["ASM-A5", "Workforce identity carries branch, role, insurer scope and certification",
         "the permission model needs a second source of truth", "Deepali + Rajal",
         "IF-2 · WS-2", ("Unconfirmed", "#b45309", "bold")],
        ["ASM-A6", "The customer can receive an OTP and a payment link on a personal device",
         "C2 and C4 both need a different mechanism — a journey redesign, not a tweak",
         "Rajal + Shivanshi", "C2 · C4", ("Unconfirmed", "#b45309", "bold")],
    ]
    c.table(M, ay + 14, ["ID", "Assumption", "Consequence if false", "Validation owner",
                         "Anchor", "State"],
            rows, [80, 430, 480, 240, 300, 266], row_h=25)

    c.footer(DRAFT, FOOT_R)
    return c


# ═════════════════════════════════════════════════════════════ 2. DEPENDENCY MAP
def dependency_map():
    c = Canvas(1900, 1000,
               "AU Bank Insurance Distribution Platform — Dependency Map & Critical Path",
               "a dependency without a required-by date and one named owner is not managed · "
               "dates below are PROPOSED and become real when Kalpana sets them at the review")
    W = c.w - 2 * M

    c.status_banner(M, 84, W,
                    "Every date on this diagram is a proposal, and every one of them is empty in "
                    "the v0.1 pack",
                    "The v0.1 register listed fifteen dependencies with providers and no dates, "
                    "and owners expressed as three-way groups. Both are corrected here: one name, "
                    "one date, one escalation trigger. Delivery owns the dates — architecture owns "
                    "only the shape.", WARN, h=50)

    LANES = [("Foundation — blocks everything", WARN, 168),
             ("External access & contracts", EXT, 400),
             ("Product & compliance decisions", W2, 632)]
    for (t, pal, y) in LANES:
        c.rect(M, y, W, 26, pal.fill, pal.stroke, rx=5, sw=1.3)
        c.text(M + 12, y + 18, t, 11, INK, weight="bold")

    # (id, what, owner, needed-for, proposed date, state)
    DEPS = [
        # foundation
        ("D-01", "CI with enforced quality, security and architecture gates", "Amit",
         "every downstream E3/E4 evidence claim", "GATE-S08", "In progress", 0, 0),
        ("D-02", "IaC, environments, secrets, KMS hierarchy", "Shivanshi",
         "any deployable service", "S09 critical path", "In progress", 0, 1),
        ("D-03", "Observability substrate + 7-year WORM retention in ap-south-1", "Shivanshi",
         "audit evidence, SLO measurement", "S09", "Not started", 0, 2),
        ("D-04", "India-based dev / UAT / prod environments", "Shivanshi",
         "everything that must stay in-country", "S09", "Not started", 0, 3),
        # external
        ("D-05", "Workforce identity + PDP interface and test realm (WS-2)", "Deepali",
         "any authenticated journey step", "before W1 build", "Interface agreed", 1, 0),
        ("D-06", "Core banking customer lookup — interface, access, test data", "CBS owner",
         "step 2 of the quote path", "before W1 build", "Not contracted", 1, 1),
        ("D-07", "1SB / insurer access — contract, credentials, limits, sandbox", "WS-1 owner",
         "quote, proposal, issuance", "before W2 build", "Adapter exists; limits unknown", 1, 2),
        ("D-08", "AU Bank PG — contract, callback signing, settlement format", "Finance",
         "the entire money path", "before W3 build", ("BLOCKING — no Finance seat", RED_D), 1, 3),
        ("D-09", "Messaging provider — sender approval, test account", "Ops",
         "OTP and payment-link delivery", "before W3 build", "Not started", 1, 4),
        # product & compliance
        ("D-10", "Consent rule pack — Compliance signature at E2", "Shailja",
         "C2 gate; S11 entry (non-waivable)", "before W2 build", ("Content ready, unsigned", "#b45309"), 2, 0),
        ("D-11", "Suitability rule pack — Compliance signature at E2", "Shailja",
         "C1 gate; S11 entry (non-waivable)", "before W2 build", ("Content ready, unsigned", "#b45309"), 2, 1),
        ("D-12", "R0 product & insurer matrix confirmed", "Rajal",
         "catalogue, suitability, quote", "before W1 build", "Term/Group A stated, not signed", 2, 2),
        ("D-13", "Data classification & retention schedule", "Shailja + Aarti",
         "S07-G5 sign-off, storage design", "before W3 build", "Not started", 2, 3),
        ("D-14", "R0 acceptance criteria and traceability", "Rajal + R11 BA",
         "QA scope, gate evidence", "before W2 build", "Partial", 2, 4),
        ("D-15", "Operations ownership, runbooks, response targets", "Shivanshi",
         "break handling, F1–F5", "before W3 build", "Not started", 2, 5),
    ]
    cw = (W - 4 * 12) / 5
    for (did, what, owner, needed, when, state, lane, idx) in DEPS:
        base_y = [200, 432, 664][lane]
        x = M + idx * (cw + 12)
        h = 108
        blocking = isinstance(state, tuple)
        pal = WARN if blocking else NEUT
        c.rect(x, base_y, cw, h, pal.fill, pal.stroke, rx=6, sw=1.6 if blocking else 1.3)
        c.text(x + 11, base_y + 19, did, 9.5, pal.deep, weight="bold")
        c.text(x + cw - 11, base_y + 19, when, 8.8, INK, "end", "bold")
        yy = base_y + 34
        for ln in wrap(what, cw - 22, 9.5):
            c.text(x + 11, yy, ln, 9.5, INK, weight="bold"); yy += 11
        yy += 2
        for ln in wrap("needed for " + needed, cw - 22, 8.8):
            c.text(x + 11, yy, ln, 8.8, MUTED); yy += 10
        c.text(x + 11, base_y + h - 22, owner, 9, BODY, weight="bold")
        st, col = (state[0], state[1]) if blocking else (state, FAINT)
        for j, ln in enumerate(wrap(st, cw - 22, 8.8)):
            c.text(x + 11, base_y + h - 10 + j * 10, ln, 8.8, col, weight="bold" if blocking else None)

    # ---- critical path
    py = 800
    c.section(M, py, "Critical path to the first proven journey — the shortest honest sequence")
    STEPS = ["GATE-S08 · CI enforced", "S09 · platform + retention", "W1 · journey spine + Hub",
             "W2 · C1/C2 gates + quote", "W3 · money path + audit", "W4 · Flutter + BFF",
             "GATE-S11 · one proven journey"]
    c.ribbon(M, py + 14, W, STEPS, h=40, gap=13)
    c.text(M, py + 74, "Nothing in W1 or later starts before GATE-S08 passes and the S09 critical "
                       "path lands (03-solution-architecture-r0.md §3). Two dependencies sit off "
                       "this path and can still stop it: D-08 has no Finance owner seated, and "
                       "D-10 / D-11 are non-waivable S11 entry conditions under DEC-20260816-06.",
           9.5, MUTED)

    c.legend(M, 928, [("swatch:#fff7ed:#ea580c", "blocking or unowned — needs a decision at the review"),
                      ("swatch:#f1f5f9:#cbd5e1", "tracked, owner named"),
                      ("text", "")])
    c.text(M + 700, 934, "Dates shown are PROPOSED. They become commitments only when Delivery "
                         "sets them and the provider agrees — an architecture document cannot "
                         "create a delivery date.", 9.5, MUTED)
    c.footer(DRAFT, FOOT_R)
    return c


# ══════════════════════════════════════════════════════════════ 3. APPROVAL MODEL
def approval_model():
    c = Canvas(1900, 1080,
               "AU Bank Insurance Distribution Platform — How This Pack Actually Gets Approved",
               "the AIGEM seven-board model with one aggregator and one gate · replaces the "
               "five-step CTO flow in v0.1, which described an authority the matrix does not define")
    W = c.w - 2 * M

    c.status_banner(M, 84, W, "Correction carried from the v0.1 review (finding F-56)",
                    "v0.1 drew a five-step flow placing a 'Chief Technology Officer' review ahead "
                    "of the mandatory approvers. PERSONA-AUTHORITY-MATRIX v1.6 defines no CTO "
                    "persona; strategic technology sits with Architecture (A/AP) and funding with "
                    "an Executive Sponsor who does not exist (GAP-010). The real model is below.",
                    WARN, h=54)

    # ---- the board row
    c.section(M, 168, "Seven boards · each answers only its own checklist · silence is never assent")
    BOARDS = [
        ("Board 1 — Architecture", "Mahesh", W1, "boundaries, decomposition, ADRs, drift",
         "AP · B", "AGENT ok at T3"),
        ("Board 2 — Technical", "Amit", W1, "buildability, engineering standards, feasibility",
         "RV", "AGENT ok"),
        ("Board 3 — Product", "Rajal", W2, "scope, journey behaviour, acceptance, KPI semantics",
         "AP", "AGENT ok at T3"),
        ("Board 4 — Security", "Deepali", "RED", "trust boundaries, controls, residual risk",
         "AP · B", "HUMAN — no exception"),
        ("Board 5 — QA", "Swapnali", W3, "evidence sufficiency, testability, release risk",
         "AP", "AGENT ok"),
        ("Board 6 — Risk & Compliance", "Shailja", "RED", "permissibility, mandatory outcomes, evidence",
         "AP · B", "HUMAN — no exception"),
        ("Board 7 — Operations", "Shivanshi", W3, "operability, capacity, recovery, ORR",
         "AP", "AGENT ok"),
    ]
    bw = (W - 6 * 12) / 7
    for i, (name, who, pal, scope, rights, human) in enumerate(BOARDS):
        x = M + i * (bw + 12)
        is_human = pal == "RED"
        f, s, d = ("#fef2f2", RED, RED_D) if is_human else (pal.fill, pal.stroke, pal.deep)
        c.rect(x, 182, bw, 126, f, s, rx=7, sw=2.0 if is_human else 1.4)
        c.text(x + bw / 2, 204, name, 10, INK, "middle", "bold")
        c.text(x + bw / 2, 219, who, 10.5, d, "middle", "bold")
        yy = 236
        for ln in wrap(scope, bw - 20, 8.8):
            c.text(x + bw / 2, yy, ln, 8.8, MUTED, "middle"); yy += 10
        c.rect(x + 10, 274, bw - 20, 15, PAPER, s, rx=4, sw=1)
        c.text(x + bw / 2, 285, rights, 8.5, d, "middle", "bold")
        c.text(x + bw / 2, 302, human, 8.5, RED_D if is_human else FAINT, "middle",
               weight="bold" if is_human else None)

    # ---- aggregator
    ay = 336
    c.rect(M + W / 2 - 300, ay, 600, 56, WASH, "#64748b", rx=8, sw=1.6)
    c.text(M + W / 2, ay + 23, "Approval Aggregator", 13, INK, "middle", "bold")
    c.text(M + W / 2, ay + 40, "one gate · records to approval-gate.schema.json · max two rounds, "
                               "then ESCALATED", 9.5, MUTED, "middle")
    for i in range(7):
        x = M + i * (bw + 12) + bw / 2
        c.line(x, 316, M + W / 2 - 300 + 600 * (i + .5) / 7, ay, GHOST, 1.3, dash="4 3",
               marker="arrGray")

    for i, (t, pal, sub) in enumerate([
            ("APPROVED", OK, "conditions folded into acceptance criteria"),
            ("REWORK", WARN, "round 2 only — a third round is not permitted"),
            ("REJECTED", NEUT, "may not be re-litigated without new evidence"),
            ("ESCALATED", NEUT, "unresolved after two rounds, or a standing veto")]):
        x = M + W / 2 - 480 + i * 250
        c.rect(x, ay + 82, 230, 48, pal.fill, pal.stroke, rx=6, sw=1.4)
        c.text(x + 115, ay + 102, t, 11, INK, "middle", "bold")
        for j, ln in enumerate(wrap(sub, 210, 8.5)):
            c.text(x + 115, ay + 116 + j * 10, ln, 8.5, MUTED, "middle")
        c.line(M + W / 2, ay + 56, x + 115, ay + 82, GHOST, 1.3, dash="4 3", marker="arrGray")

    # ---- the rules that bite
    ry = 540
    c.section(M, ry, "The rules that decide whether this pack can be approved at all")
    RULES = [
        ("This pack is T4", "It changes, or asserts, the behaviour of authorization (G1), "
         "restricted-data handling (G2), secrets (G3), money movement and reconciliation (G5), "
         "consent capture and retention (G6) and production topology (G8). Any one fires T4."),
        ("Two verdicts must be human", "Security (Board 4) and Risk & Compliance (Board 6) at T4 "
         "require reviewer_type: HUMAN. No aggregate override exists, and no AI simulation — "
         "including this pack's own review record — can satisfy them."),
        ("An agent verdict with empty evidence is invalid", "Rule RG-3: it is treated as NOT_RUN, "
         "not as approval. Every board verdict must cite a path a reader can open."),
        ("Self-review must be declared", "An agent that authored the plan may produce verdicts for "
         "it, marked self_review: true — and a self-reviewed T3+ plan still needs at least one "
         "human board."),
        ("Approval expires", "At the next stage boundary (11 §14). An approval carried across a "
         "gate is not an approval, it is an assumption."),
        ("Compliance debt is never accepted", "CMP-6. Unlike technical debt, there is no "
         "'accept with expiry' path for a compliance control."),
    ]
    rw = (W - 5 * 12) / 6
    for i, (t, d) in enumerate(RULES):
        x = M + i * (rw + 12)
        c.rect(x, ry + 14, rw, 150, PAPER, HAIR, rx=6, sw=1.3)
        yy = ry + 34
        for ln in wrap(t, rw - 24, 10):
            c.text(x + 12, yy, ln, 10, INK, weight="bold"); yy += 12
        yy += 4
        for ln in wrap(d, rw - 24, 9):
            c.text(x + 12, yy, ln, 9, MUTED); yy += 11

    # ---- who signs what
    sy = 740
    c.section(M, sy, "Signature ledger — one table, used identically in all eight documents")
    rows = [
        ["Architecture", "Mahesh — Principal Insurance Platform Architect", "AP · B",
         "AI-drafted, self-review declared", ("Pending", "#b45309", "bold")],
        ["Product", "Rajal — Principal Insurance Platform Product Owner", "AP",
         "scope re-affirms DEC-20260816-03", ("Pending", "#b45309", "bold")],
        ["Business Analysis", "Principal Insurance Platform BA (R11)", "RV",
         "traceability and testable expression", ("Pending", "#b45309", "bold")],
        ["Engineering", "Amit — Technical Head", "RV",
         "buildability against the existing estate", ("Pending", "#b45309", "bold")],
        ["Security", "Deepali — Principal Security Architect", "AP · B · HUMAN",
         "cannot be satisfied without the S07-G3 threat model", ("Pending — HUMAN", RED_D, "bold")],
        ["Data & Database", "Aarti — Principal Data & Database Architect", "AP",
         "S07-G5: data architecture incl. backup and retention", ("Pending", "#b45309", "bold")],
        ["Quality", "Swapnali — QA Lead", "AP",
         "evidence levels and control-to-test coverage", ("Pending", "#b45309", "bold")],
        ["Risk & Compliance", "Shailja — Compliance & Risk Head", "AP · B · HUMAN",
         "permissibility; GAP-006/007 remain open", ("Pending — HUMAN", RED_D, "bold")],
        ["Operations", "Shivanshi — Principal SRE / Reliability Head", "AP",
         "SLOs, capacity, recovery, ORR", ("Pending", "#b45309", "bold")],
        ["Delivery", "Kalpana — Delivery Head (R12)", "RV",
         "dependency dates and sequence", ("Pending", "#b45309", "bold")],
        ["Finance", ("SEAT NOT FILLED — owns reconciliation in this pack", RED_D, "bold"), "—",
         "settlement timing, break ownership, refund accounting", ("Blocked", RED_D, "bold")],
        ["Executive Sponsor", ("SEAT NOT FILLED — GAP-010", RED_D, "bold"), "—",
         "FRI-001 funding line has no approver", ("Blocked", RED_D, "bold")],
    ]
    c.table(M, sy + 14, ["Board / seat", "Named human", "Rights", "What they are actually signing",
                         "State"],
            rows, [200, 470, 160, 660, 306], row_h=23)

    c.footer(DRAFT, FOOT_R)
    return c


# ═══════════════════════════════════════════════════════════════ 4. GATE COVERAGE
def gate_coverage():
    c = Canvas(1900, 900,
               "AU Bank Insurance Distribution Platform — Gate Criteria Coverage",
               "what this pack actually contributes to S06 and S07, criterion by criterion · "
               "replaces the v0.1 'drafting 100% complete' dashboard, which measured the wrong thing")
    W = c.w - 2 * M

    c.status_banner(M, 84, W, "Drafting percentage is not a measure of anything",
                    "v0.1 reported '100% architecture drafting complete'. A pack is not measured "
                    "by how much of it was written; it is measured by which gate criteria it can "
                    "close, and at what evidence level. Below is that view — including the four "
                    "criteria this pack cannot close no matter how well it is written.", WARN, h=52)

    STATE = {"met":    ("Met — evidence cited",       OK.fill,   OK.stroke,  OK.deep),
             "part":   ("Partial — needs one more thing", W2.fill, W2.stroke, W2.deep),
             "open":   ("Open — this pack does not close it", "#fef2f2", RED, RED_D),
             "human":  ("Needs a HUMAN signature — unclosable by any AI", "#fef2f2", RED, RED_D)}

    GATES = [
        ("S06 — Domain & Information Architecture", [
            ("S06-G1", "Bounded context map with relationships and justifications", "E2", "met",
             "domain-ownership.svg + hdl.svg · 19 contexts, owns / never-owns per context"),
            ("S06-G2", "Aggregates with state machines and invariants", "E2", "met",
             "journey-state-machine.svg · 17 states, 6 forbidden transitions"),
            ("S06-G3", "Journey saga designed including compensations", "E2", "met",
             "journey-state-machine.svg compensation table · S-19 tasks per failing transition"),
            ("S06-G4", "Logical data model per context", "E2", "open",
             "NOT in this pack — Aarti's artefact; ownership ≠ a data model"),
            ("S06-G5", "Data ownership matrix complete", "E1", "met",
             "domain-ownership.svg · golden source and 'never owns' for all 19"),
            ("S06-G6", "Compliance hard-gate enforcement points named", "E2", "met",
             "quote-path.svg · C1 at #7/#10, C2 at #6/#11, with FF-08 / FF-09"),
            ("S06-G7", "Audit model proven to reconstruct a journey", "E3", "open",
             "E3 means EXECUTED. A diagram cannot close it — a reconstruction run can"),
            ("S06-G8", "Canonical model is provider-neutral", "E2", "part",
             "asserted at the #14 seam; ArchUnit provider-isolation rule exists, needs citing"),
        ]),
        ("S07 — Solution & Security Architecture", [
            ("S07-G1", "Target architecture documented and reviewed", "E2", "part",
             "documented (hdl.svg + this pack); the review verdict is what is missing"),
            ("S07-G2", "ADRs recorded for all significant decisions", "E1", "open",
             "12 decisions in doc 04 carry no ADR IDs; ARCH-001…022 exist and are not extended"),
            ("S07-G3", "Threat model complete per trust boundary", "E2", "human",
             "trust-zones.svg gives the boundaries; the threat model per boundary is Deepali's"),
            ("S07-G4", "Security architecture approved", "E2", "human",
             "no AI output can satisfy this — including this pack"),
            ("S07-G5", "Data architecture approved incl. backup and retention", "E2", "open",
             "needs the logical model, classification and the retention schedule — then Aarti"),
            ("S07-G6", "NFR sheet with numbers, each verifiable", "E2", "part",
             "ws3-platform/05-nfr-catalogue.md exists with IDs and methods; unsigned"),
            ("S07-G7", "R0 build order defined — minimum service set", "E2", "met",
             "12 services + 1 app, waved W1–W4 · capability-map.svg + dependency-map.svg"),
            ("S07-G8", "Fitness functions defined for automatable constraints", "E1", "part",
             "FF-01…FF-15 referenced throughout; the consolidated list belongs in one place"),
        ]),
    ]

    y = 170
    for (gname, crits) in GATES:
        c.section(M, y, gname)
        rows = []
        for (cid, what, level, st, note) in crits:
            label, f, s, d = STATE[st]
            rows.append([(cid, INK, "bold"), what, (level, INK, "bold"),
                         (label, d, "bold"), note])
        y = c.table(M, y + 14, ["Criterion", "What it requires", "Level", "State after this pack",
                                "Evidence, or what is still missing"],
                    rows, [90, 470, 70, 330, 836], row_h=25) + 30

    # ---- honest tally
    ty = y + 4
    c.section(M, ty, "The honest tally")
    TALLY = [("6 of 16", "criteria this pack meets", OK),
             ("4 of 16", "partial — one identified thing away", W2),
             ("4 of 16", "open — belong to Aarti, or need an executed run", NEUT),
             ("2 of 16", "unclosable by any AI — S07-G3 and S07-G4 need Deepali's human signature", WARN)]
    tw = (W - 3 * 14) / 4
    for i, (n, t, pal) in enumerate(TALLY):
        x = M + i * (tw + 14)
        c.rect(x, ty + 14, tw, 74, pal.fill, pal.stroke, rx=7, sw=1.5)
        c.text(x + tw / 2, ty + 44, n, 20, INK, "middle", "bold")
        for j, ln in enumerate(wrap(t, tw - 24, 9.5)):
            c.text(x + tw / 2, ty + 62 + j * 11, ln, 9.5, pal.deep, "middle")

    c.footer(DRAFT, FOOT_R)
    return c


BUILD = {"risk-register": risk_register, "dependency-map": dependency_map,
         "approval-model": approval_model, "gate-coverage": gate_coverage}

if __name__ == "__main__":
    outdir = sys.argv[1] if len(sys.argv) > 1 else "."
    only = sys.argv[2] if len(sys.argv) > 2 else None
    for name, fn in BUILD.items():
        if only and only != name:
            continue
        p = os.path.join(outdir, name + ".svg")
        open(p, "w").write(fn().render())
        print("wrote", p, os.path.getsize(p), "bytes")
