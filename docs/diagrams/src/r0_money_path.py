#!/usr/bin/env python3
"""
R0 money path — proposal → underwriting → payment → reconciliation → issuance → SOLD.

Companion dynamic view to docs/hdl.svg (the static R0 high-level design).
Reuses that diagram's design system and its ID vocabulary (#n bounded contexts,
C1/C2/C4 controls, S-nn specs, INV-* invariants, FF-nn fitness functions) so the
two read as one set.

    python3 docs/diagrams/src/r0_money_path.py > docs/diagrams/r0-money-path.svg

No dependencies. Deterministic output.
"""
import sys

# ---------------------------------------------------------------- design system
INK        = "#0f172a"   # titles
BODY       = "#334155"   # primary text / arrows
MUTED      = "#475569"   # secondary text
FAINT      = "#64748b"   # captions
GHOST      = "#94a3b8"   # async arrows
HAIR       = "#cbd5e1"   # lifelines, rules
BAND       = "#f8fafc"   # alternating band
PAPER      = "#ffffff"
DARK       = "#1f2937"   # ribbon chevrons

RED        = "#dc2626"   # non-waivable control
BLUE_F, BLUE_S     = "#dbeafe", "#2563eb"   # W1 journey spine
AMBER_F, AMBER_S   = "#fef3c7", "#d97706"   # W2 consent/suitability/quote
GREEN_F, GREEN_S   = "#dcfce7", "#16a34a"   # W3 money/policy/audit
PURPLE_F, PURPLE_S = "#f3e8ff", "#9333ea"   # W4 BFF/Flutter/notification
EXT_F, EXT_S       = "#ffffff", "#94a3b8"   # external systems (dashed)
CUST_F, CUST_S     = "#eef2ff", "#4f46e5"   # customer device
WARN_F, WARN_S     = "#fff7ed", "#ea580c"   # failure panels

W, H = 1900, 1452
out = []
def e(s): out.append(s)
def esc(t): return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

def text(x, y, t, size=9.5, fill=MUTED, anchor="start", weight=None, ls=None, style=None):
    a = f' text-anchor="{anchor}"' if anchor != "start" else ""
    w = f' font-weight="{weight}"' if weight else ""
    l = f' letter-spacing="{ls}"' if ls else ""
    st = f' font-style="{style}"' if style else ""
    e(f'<text x="{x}" y="{y}" font-size="{size}" fill="{fill}"{a}{w}{l}{st}>{esc(t)}</text>')

def box(x, y, w, h, fill, stroke, rx=6, dash=None, sw=1.4):
    d = f' stroke-dasharray="{dash}"' if dash else ""
    e(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" '
      f'stroke="{stroke}" stroke-width="{sw}"{d}/>')

def tag(x, y, label, fill):
    """Small square wave/context tag, as used in hdl.svg."""
    e(f'<rect x="{x}" y="{y}" width="19" height="13" rx="3" fill="{fill}"/>')
    text(x + 9.5, y + 9.7, label, 8.5, PAPER, "middle", "bold")

def ctrl(cx, cy, label):
    """Red non-waivable control pill (C1 / C2 / C4)."""
    e(f'<rect x="{cx}" y="{cy}" width="22" height="13" rx="6.5" fill="{RED}"/>')
    text(cx + 11, cy + 9.7, label, 8.5, PAPER, "middle", "bold")

def stepdot(cx, cy, n, fill=DARK):
    e(f'<circle cx="{cx}" cy="{cy}" r="8.5" fill="{fill}"/>')
    text(cx, cy + 3.2, str(n), 9, PAPER, "middle", "bold")

# ------------------------------------------------------------------ lifelines
# (key, title, subtitle, fill, stroke, tag-text, tag-fill, dashed?)
LANES = [
    ("cust",  "Customer Device",        "own device only",      CUST_F,  CUST_S,  None, None,     False),
    ("rm",    "RM · Flutter + #2 BFF",  "token-hiding session", PURPLE_F,PURPLE_S,"W4", PURPLE_S, False),
    ("jrn",   "#9 Journey Orchestration","stage + references",  BLUE_F,  BLUE_S,  "W1", BLUE_S,   False),
    ("prop",  "#11 Proposal & UW",      "submit · poll",        GREEN_F, GREEN_S, "W3", GREEN_S,  False),
    ("pay",   "#12 Payment",            "session · recon",      GREEN_F, GREEN_S, "W3", GREEN_S,  False),
    ("pg",    "AU Bank PG",             "external · 3-D Secure",EXT_F,   EXT_S,   None, None,     True),
    ("hub",   "#14 Hub → #15 1SB",      "all provider traffic", BLUE_F,  BLUE_S,  "W1", BLUE_S,   False),
    ("ins",   "Group A Insurer",        "external",             EXT_F,   EXT_S,   None, None,     True),
    ("pol",   "#13 Policy & Issuance",  "issues on RECONCILED", GREEN_F, GREEN_S, "W3", GREEN_S,  False),
    ("aud",   "#16 Audit · #17 Notify", "WORM · outbox",        GREEN_F, GREEN_S, "W3", GREEN_S,  False),
]

M          = 52                       # page margin
LANE_TOP   = 168                      # lifeline header box top
LANE_H     = 52
LIFE_TOP   = LANE_TOP + LANE_H        # lifelines start
FIRST_MSG  = 268
DY         = 41                       # vertical rhythm between messages
BOX_W      = 168
span       = (W - 2 * M)
LANE_W     = span / len(LANES)
X = {k: M + LANE_W * (i + 0.5) for i, (k, *_) in enumerate(LANES)}

# ------------------------------------------------------------------- messages
# (n, src, dst, label, detail, kind)   kind: sync | async | self | ret
MSGS = [
    (1,  "rm",   "prop", "submit proposal (quoteRef, consentRef, suitabilityRef)",
         "sync · idempotency key = proposalId", "sync"),
    (2,  "prop", "prop", "hard gate — C1 suitability valid & unexpired · C2 consent current",
         "either missing or stale → 403, journey does not advance (FF-08 / FF-09)", "self"),
    (3,  "prop", "jrn",  "stage := PROPOSAL_SUBMITTING",
         "references only — never another context's decision (SC-W3-6)", "sync"),
    (4,  "prop", "hub",  "submit proposal",
         "24 h idempotency contract (INV-ACL-01) · adapter.onesb.* only", "sync"),
    (5,  "hub",  "ins",  "provider submit",
         "per-provider bulkhead · one insurer cannot drain the pool", "async"),
    (6,  "ins",  "hub",  "ack + insurerRef",
         "reference is the recovery handle — never a blind resubmit (S-12)", "ret"),
    (7,  "prop", "hub",  "poll UW status → REQUIREMENTS | APPROVED",
         "async-poll (S-09 / S-11); no user connection held open", "async"),
    (8,  "prop", "jrn",  "stage := UW_APPROVED — unlocks payment",
         "payment is unreachable until this stage is recorded", "sync"),
    (9,  "jrn",  "pay",  "create payment session (amount = selected offer premium)",
         "charged amount must equal the quoted premium — else reject (S-13)", "sync"),
    (10, "pay",  "pg",   "create session",
         "returns paymentSessionId + one-time customer link", "sync"),
    (11, "pay",  "aud",  "dispatch payment link to the customer device",
         "#17 SMS/email to the customer only — never to an RM or bank device (C4)", "async"),
    (12, "aud",  "cust", "one-time link · short expiry · bound to the OTP-verified device",
         "no RM-device payment route exists in the interface (DEC-20260816-12)", "async"),
    (13, "cust", "pg",   "customer pays — 3-D Secure, on their own device",
         "the bank never sees the instrument", "sync"),
    (14, "pg",   "pay",  "authenticated callback",
         "signature + txn match + replay window — a callback is a claim, not proof (S-14)", "ret"),
    (15, "pay",  "pay",  "reconcile callback against the settlement file (T+1)",
         "reconciliation decides whether money moved — not the callback, not a DB restore (S-15)", "self"),
    (16, "pay",  "jrn",  "stage := PAYMENT_RECONCILED",
         "UNCERTAIN never becomes success by timeout, retry or optimism", "sync"),
    (17, "jrn",  "pol",  "request issuance",
         "issuance is attempted only against a RECONCILED payment (SC-W3-4)", "sync"),
    (18, "pol",  "hub",  "issue policy → policyNo + document reference",
         "via the same provider seam; no direct insurer call exists", "sync"),
    (19, "pol",  "jrn",  "stage := POLICY_ISSUED",
         "issued is still not sold", "sync"),
    (20, "jrn",  "aud",  "SOLD gate — issued ∧ reconciled ∧ audit complete (INV-JRN-05)",
         "audit completeness is a precondition, not a post-hoc report", "sync"),
]

# =============================================================== render: header
e(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" '
  f'font-family="Helvetica, Arial, sans-serif">')
e('<defs>')
e(f'<marker id="arr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" '
  f'orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="{BODY}"/></marker>')
e(f'<marker id="arrGray" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" '
  f'orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="{GHOST}"/></marker>')
e(f'<marker id="arrRed" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" '
  f'orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="{RED}"/></marker>')
e('</defs>')
e(f'<rect width="{W}" height="{H}" fill="{PAPER}"/>')

text(W / 2, 36, "AU Bank Insurance Distribution Platform — R0 Money Path", 25, INK, "middle", "bold")
text(W / 2, 60,
     "proposal → underwriting → payment → reconciliation → issuance → SOLD · the twenty steps that "
     "move money and the five ways they fail · companion to docs/hdl.svg",
     12.5, MUTED, "middle")

# governing-invariant ribbon
text(M, 88, "THE INVARIANT THIS ENTIRE FLOW EXISTS TO PROTECT", 10, FAINT, weight="bold", ls="1")
box(M, 96, W - 2 * M, 46, "#f1f5f9", HAIR, rx=7, sw=1.2)
text(M + 16, 116, "INV-JRN-05 — a journey reaches SOLD only when policy issuance, payment "
                  "reconciliation and audit completeness are ALL confirmed.", 12, INK, weight="bold")
text(M + 16, 133, "Issued-but-unreconciled is not a sale. Paid-but-unissued is not a sale. "
                  "Complete-but-unevidenced is not a sale. Every failure branch below is a way one "
                  "of the three legs goes missing — and each is caught rather than assumed away.",
     10, MUTED)

# ============================================================ render: lifelines
LIFE_BOT = FIRST_MSG + (len(MSGS) - 1) * DY + 26

for i, (k, title, sub, fill, stroke, tg, tgf, dashed) in enumerate(LANES):
    cx = X[k]
    if i % 2 == 1:                                    # alternating readability band
        e(f'<rect x="{cx - LANE_W/2}" y="{LIFE_TOP}" width="{LANE_W}" height="{LIFE_BOT - LIFE_TOP}" '
          f'fill="{BAND}"/>')
    box(cx - BOX_W / 2, LANE_TOP, BOX_W, LANE_H, fill, stroke, rx=6,
        dash="5 3" if dashed else None)
    text(cx, LANE_TOP + 22, title, 10.5, INK, "middle", "bold")
    text(cx, LANE_TOP + 37, sub, 9, FAINT, "middle")
    if tg:
        tag(cx - BOX_W / 2 + 6, LANE_TOP + 5, tg, tgf)
    if k == "cust":
        ctrl(cx + BOX_W / 2 - 28, LANE_TOP + 5, "C4")
    e(f'<line x1="{cx}" y1="{LANE_TOP + LANE_H}" x2="{cx}" y2="{LIFE_BOT}" '
      f'stroke="{HAIR}" stroke-width="1.4" stroke-dasharray="3 4"/>')

# ============================================================= render: messages
for idx, (n, src, dst, label, detail, kind) in enumerate(MSGS):
    y = FIRST_MSG + idx * DY
    x1, x2 = X[src], X[dst]

    if kind == "self":
        # self-call: bracket to the right of the lifeline
        w = 26
        e(f'<path d="M{x1},{y - 8} h{w} v16 h-{w}" fill="none" stroke="{BODY}" '
          f'stroke-width="1.5" marker-end="url(#arr)"/>')
        stepdot(x1 - 4, y - 20, n, RED if n == 2 else DARK)
        text(x1 + w + 12, y - 1, label, 9.5, INK, weight="bold")
        text(x1 + w + 12, y + 12, detail, 9, FAINT)
        continue

    stroke = GHOST if kind in ("async", "ret") else BODY
    marker = "arrGray" if kind in ("async", "ret") else "arr"
    dash = ' stroke-dasharray="6 4"' if kind in ("async", "ret") else ""
    e(f'<line x1="{x1}" y1="{y}" x2="{x2}" y2="{y}" stroke="{stroke}" stroke-width="1.5"'
      f'{dash} marker-end="url(#{marker})"/>')

    stepdot(x1, y, n, DARK)
    # Label sits above the line, always anchored at the left end so long text runs
    # into open space. On a right-to-left message that end carries the arrowhead,
    # so start further in to clear it.
    lx = min(x1, x2) + (24 if x2 < x1 else 16)
    text(lx, y - 8, label, 9.5, INK, weight="bold")
    text(lx, y + 13, detail, 9, FAINT)

# ------------------------------------------------------------------- SOLD gate
gy = LIFE_BOT + 14
box(M, gy, W - 2 * M, 44, "#ecfdf5", "#059669", rx=7, sw=1.6)
text(M + 16, gy + 19, "SOLD — and only here", 12, "#065f46", weight="bold")
text(M + 150, gy + 19, "POLICY_ISSUED  ∧  PAYMENT_RECONCILED  ∧  AUDIT_COMPLETE", 11, "#065f46",
     weight="bold")
text(M + 16, gy + 35,
     "Any one leg missing leaves the journey in a named non-terminal state with an owned operations "
     "task (S-19). There is no state transition that reaches SOLD by elapsed time, by retry count, "
     "or by an operator's judgement.", 9.5, MUTED)

# ================================================= render: failure branch panels
fy = gy + 62
text(M, fy, "FAILURE BRANCHES — EACH ONE IS A WAY A LEG OF THE INVARIANT GOES MISSING", 10, FAINT,
     weight="bold", ls="1")

PANELS = [
    ("F1", "Callback never arrives", "after step 14",
     [("Detect",  "no authenticated callback inside the session window"),
      ("State",   "payment := UNCERTAIN — never SUCCESS, never FAILED"),
      ("Resolve", "T+1 settlement file is the arbiter (S-15)"),
      ("If unresolved", "ops break task (S-19); journey blocked from SOLD"),
      ("Never",   "assume success · re-charge · let the RM 'confirm' it")]),
    ("F2", "Duplicate submission", "steps 1, 4, 9",
     [("Detect",  "same idempotency key seen inside the 24 h contract"),
      ("State",   "unchanged — the first result is replayed verbatim"),
      ("Resolve", "identical body → original response (INV-ACL-01)"),
      ("If changed", "409 — a changed replay is a defect, not an update"),
      ("Never",   "create a second proposal or a second charge")]),
    ("F3", "Issued but unreconciled", "after step 18",
     [("Detect",  "policy exists at the insurer; payment ≠ RECONCILED"),
      ("State",   "journey stays NOT SOLD — visibly, in the RM workspace"),
      ("Resolve", "finance break queue; refund or recover to reconciled"),
      ("If persists", "escalates as a money-movement exception, not a bug"),
      ("Never",   "report the sale because the policy number exists")]),
    ("F4", "Insurer silent / timing out", "steps 5–7, 18",
     [("Detect",  "no ack, or poll deadline exceeded on insurerRef"),
      ("State",   "journey resumable at its recorded stage (S-12)"),
      ("Resolve", "status recovered by reference — submission is never repeated"),
      ("If degraded", "per-provider bulkhead contains it to one insurer"),
      ("Never",   "blind resubmit · hold a user connection open")]),
    ("F5", "Audit write fails", "any step",
     [("Detect",  "outbox row unconsumed past its threshold (S-17)"),
      ("State",   "business action retained and retried, at-least-once"),
      ("Resolve", "journey completion waits for required evidence"),
      ("If lost",  "SOLD is unreachable — the third leg cannot be waived"),
      ("Never",   "complete a regulated journey on partial evidence")]),
]

pw, pgap = (W - 2 * M - 4 * 12) / 5, 12
pt, ph = fy + 10, 208
for i, (pid, title, where, rows) in enumerate(PANELS):
    px = M + i * (pw + pgap)
    box(px, pt, pw, ph, WARN_F, WARN_S, rx=7, sw=1.4)
    e(f'<rect x="{px}" y="{pt}" width="{pw}" height="27" rx="7" fill="{WARN_S}"/>')
    e(f'<rect x="{px}" y="{pt + 18}" width="{pw}" height="9" fill="{WARN_S}"/>')
    text(px + 11, pt + 18, f"{pid} · {title}", 10.5, PAPER, weight="bold")
    text(px + pw - 11, pt + 18, where, 8.5, "#ffedd5", "end")
    ry = pt + 46
    for lab, val in rows:
        text(px + 11, ry, lab, 8.5, "#9a3412", weight="bold")
        # wrap the value to the panel width
        words, line, lines = val.split(), "", []
        for word in words:
            trial = (line + " " + word).strip()
            if len(trial) * 4.55 > pw - 24:
                lines.append(line); line = word
            else:
                line = trial
        lines.append(line)
        for j, ln in enumerate(lines):
            text(px + 11, ry + 12 + j * 11, ln, 9, BODY if lab != "Never" else "#b91c1c")
        ry += 12 + len(lines) * 11 + 6

# --------------------------------------------------------------------- legend
ly = pt + ph + 20
e(f'<line x1="{M}" y1="{ly - 12}" x2="{W - M}" y2="{ly - 12}" stroke="{HAIR}" stroke-width="1"/>')
e(f'<line x1="{M}" y1="{ly + 2}" x2="{M + 34}" y2="{ly + 2}" stroke="{BODY}" stroke-width="1.5" '
  f'marker-end="url(#arr)"/>')
text(M + 42, ly + 6, "synchronous call — caller waits, deadline enforced", 9.5, MUTED)
e(f'<line x1="{M + 340}" y1="{ly + 2}" x2="{M + 374}" y2="{ly + 2}" stroke="{GHOST}" '
  f'stroke-width="1.5" stroke-dasharray="6 4" marker-end="url(#arrGray)"/>')
text(M + 382, ly + 6, "asynchronous — outbox event, poll, or provider callback", 9.5, MUTED)
ctrl(M + 700, ly - 6, "C4")
text(M + 730, ly + 6, "non-waivable control", 9.5, MUTED)
stepdot(M + 862, ly + 1, 2, RED)
text(M + 876, ly + 6, "hard gate — fails closed", 9.5, MUTED)
text(M + 1040, ly + 6,
     "#n bounded context (business-problem-statement §6) · S-nn / INV-* / FF-nn as used in "
     "docs/hdl.svg · W1–W4 build waves", 9.5, MUTED)

text(M, H - 22, "DRAFT FOR REVIEW · v0.1 · not approved, not authorised for development · "
                "AI-drafted companion to the hand-authored R0 HLD", 9.5, FAINT, weight="bold")
text(W - M, H - 22, "WS-3 · S07 solution architecture · gate criteria S07-G1 / S07-G3", 9.5, FAINT,
     "end")

e('</svg>')
sys.stdout.write("\n".join(out) + "\n")
