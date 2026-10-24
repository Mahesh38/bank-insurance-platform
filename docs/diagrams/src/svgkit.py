#!/usr/bin/env python3
"""
svgkit — the shared design system for AU Bank platform diagrams.

Every token here is lifted from docs/hdl.svg, the hand-authored R0 high-level
design, so that everything generated from this module reads as part of that one
set rather than as a second visual dialect.

The rules the palette encodes:

    W1 blue     journey spine — build first
    W2 amber    consent · suitability · quote
    W3 green    money · policy · audit
    W4 purple   BFF · Flutter · notification
    grey dash   external system — outside the bank's control
    indigo      customer device — its own trust zone
    red         non-waivable control (C1 · C2 · C4) and hard gates

Usage:
    from svgkit import Canvas, W1, W2, W3, W4, EXT, CUST
    c = Canvas(1900, 1200, "Title", "subtitle")
    c.card(60, 200, 240, 70, "#9 Journey Orchestration", ["stage + references only"], W1, tag="W1")
    print(c.render())
"""
from __future__ import annotations

# ------------------------------------------------------------------ ink
INK   = "#0f172a"   # titles
BODY  = "#334155"   # primary text, sync arrows
MUTED = "#475569"   # secondary text
FAINT = "#64748b"   # captions, section labels
GHOST = "#94a3b8"   # async arrows, external strokes
HAIR  = "#cbd5e1"   # rules, lifelines
BAND  = "#f8fafc"   # alternating band
WASH  = "#f1f5f9"   # panel wash
PAPER = "#ffffff"
DARK  = "#1f2937"   # ribbon chevrons, step dots
RED   = "#dc2626"   # non-waivable
RED_D = "#b91c1c"

class Pal:
    """A wave palette: fill, stroke, and a deep tone for text on light fill."""
    def __init__(self, fill, stroke, deep, dash=None):
        self.fill, self.stroke, self.deep, self.dash = fill, stroke, deep, dash

W1   = Pal("#dbeafe", "#2563eb", "#1e40af")            # journey spine
W2   = Pal("#fef3c7", "#d97706", "#92400e")            # consent/suitability/quote
W3   = Pal("#dcfce7", "#16a34a", "#065f46")            # money/policy/audit
W4   = Pal("#f3e8ff", "#9333ea", "#6b21a8")            # BFF/Flutter/notification
EXT  = Pal("#ffffff", "#94a3b8", "#475569", dash="5 3")  # external systems
CUST = Pal("#eef2ff", "#4f46e5", "#3730a3")            # customer device
WARN = Pal("#fff7ed", "#ea580c", "#9a3412")            # failure / attention
OK   = Pal("#ecfdf5", "#059669", "#065f46")            # satisfied / terminal-good
NEUT = Pal("#f1f5f9", "#cbd5e1", "#334155")            # scaffolding
GOV  = Pal("#f8fafc", "#64748b", "#334155")            # governance frames

# character-width factor for 9.5px Helvetica, used for wrapping
CW = 4.62


def esc(t: str) -> str:
    return (str(t).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))


def wrap(text: str, width_px: float, size: float = 9.0) -> list[str]:
    """Greedy wrap to a pixel width. Honours explicit ' // ' as a hard break."""
    factor = CW * (size / 9.5)
    out = []
    for hard in str(text).split(" // "):
        line, lines = "", []
        for word in hard.split():
            trial = (line + " " + word).strip()
            if len(trial) * factor > width_px and line:
                lines.append(line)
                line = word
            else:
                line = trial
        lines.append(line)
        out.extend(lines)
    return out


class Canvas:
    def __init__(self, w, h, title=None, subtitle=None, family="Helvetica, Arial, sans-serif"):
        self.w, self.h, self.family = w, h, family
        self.o: list[str] = []
        self._defs_extra: list[str] = []
        if title:
            self.title(title, subtitle)

    # ------------------------------------------------------------- primitives
    def raw(self, s):
        self.o.append(s)

    def text(self, x, y, t, size=9.5, fill=MUTED, anchor="start", weight=None,
             ls=None, style=None, opacity=None):
        a = f' text-anchor="{anchor}"' if anchor != "start" else ""
        w = f' font-weight="{weight}"' if weight else ""
        l = f' letter-spacing="{ls}"' if ls else ""
        s = f' font-style="{style}"' if style else ""
        op = f' opacity="{opacity}"' if opacity else ""
        self.o.append(f'<text x="{x:.1f}" y="{y:.1f}" font-size="{size}" fill="{fill}"'
                      f'{a}{w}{l}{s}{op}>{esc(t)}</text>')

    def lines(self, x, y, items, size=9, fill=FAINT, lh=11.5, anchor="start", weight=None):
        for i, t in enumerate(items):
            self.text(x, y + i * lh, t, size, fill, anchor, weight)
        return y + len(items) * lh

    def rect(self, x, y, w, h, fill, stroke=None, rx=6, dash=None, sw=1.4, opacity=None):
        st = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ""
        d = f' stroke-dasharray="{dash}"' if dash else ""
        op = f' opacity="{opacity}"' if opacity else ""
        self.o.append(f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{rx}" '
                      f'fill="{fill}"{st}{d}{op}/>')

    def line(self, x1, y1, x2, y2, stroke=BODY, sw=1.5, dash=None, marker=None, opacity=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        m = f' marker-end="url(#{marker})"' if marker else ""
        op = f' opacity="{opacity}"' if opacity else ""
        self.o.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" '
                      f'stroke="{stroke}" stroke-width="{sw}"{d}{m}{op}/>')

    def path(self, d, stroke=BODY, fill="none", sw=1.5, dash=None, marker=None):
        da = f' stroke-dasharray="{dash}"' if dash else ""
        m = f' marker-end="url(#{marker})"' if marker else ""
        self.o.append(f'<path d="{d}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"{da}{m}/>')

    def circle(self, cx, cy, r, fill, stroke=None, sw=1.4, dash=None):
        st = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ""
        d = f' stroke-dasharray="{dash}"' if dash else ""
        self.o.append(f'<circle cx="{cx:.1f}" cy="{cy:.1f}" r="{r}" fill="{fill}"{st}{d}/>')

    def cylinder(self, x, y, w, h, fill=PAPER, stroke=BODY, sw=1.4):
        ry = 9
        self.o.append(
            f'<path d="M{x},{y+ry} a{w/2},{ry} 0 0 1 {w},0 v{h-2*ry} a{w/2},{ry} 0 0 1 -{w},0 z" '
            f'fill="{fill}" stroke="{stroke}" stroke-width="{sw}"/>')
        self.o.append(f'<ellipse cx="{x+w/2}" cy="{y+ry}" rx="{w/2}" ry="{ry}" fill="{fill}" '
                      f'stroke="{stroke}" stroke-width="{sw}"/>')

    # ------------------------------------------------------------- components
    def title(self, t, sub=None):
        self.text(self.w / 2, 36, t, 25, INK, "middle", "bold")
        if sub:
            self.text(self.w / 2, 60, sub, 12.5, MUTED, "middle")

    def section(self, x, y, label):
        """Small-caps grey section label, as used throughout hdl.svg."""
        self.text(x, y, label.upper(), 10, FAINT, weight="bold", ls="1")

    def tag(self, x, y, label, fill):
        self.rect(x, y, 19, 13, fill, rx=3, sw=0)
        self.text(x + 9.5, y + 9.7, label, 8.5, PAPER, "middle", "bold")

    def ctrl(self, x, y, label, fill=RED):
        self.rect(x, y, 22, 13, fill, rx=6.5, sw=0)
        self.text(x + 11, y + 9.7, label, 8.5, PAPER, "middle", "bold")

    def dot(self, cx, cy, n, fill=DARK, r=8.5):
        self.circle(cx, cy, r, fill)
        self.text(cx, cy + 3.2, str(n), 9, PAPER, "middle", "bold")

    def card(self, x, y, w, h, title, body=None, pal=NEUT, tag=None, ctrl=None,
             badge=None, rx=6, title_size=10.5, body_size=9, note=None):
        """The workhorse: a titled box whose body lines carry falsifiable detail."""
        self.rect(x, y, w, h, pal.fill, pal.stroke, rx=rx, dash=pal.dash)
        ty = y + (22 if not body else 20)
        self.text(x + w / 2, ty, title, title_size, INK, "middle", "bold")
        if body:
            yy = ty + 13
            for ln in body:
                for sub in wrap(ln, w - 18, body_size):
                    self.text(x + w / 2, yy, sub, body_size, FAINT, "middle")
                    yy += 11
        if tag:
            self.tag(x + 6, y + 5, tag, pal.stroke)
        if ctrl:
            self.ctrl(x + w - 28, y + 5, ctrl)
        if badge is not None:
            self.dot(x + w - 13, y + 12, badge)
        if note:
            self.text(x + w / 2, y + h + 12, note, 8.5, FAINT, "middle", style="italic")

    def chevron(self, x, y, w, h, label, fill=DARK, text_fill=PAPER, size=10.5):
        n = 12
        self.o.append(f'<polygon points="{x},{y} {x+w-n},{y} {x+w},{y+h/2} {x+w-n},{y+h} '
                      f'{x},{y+h} {x+n},{y+h/2}" fill="{fill}"/>')
        self.text(x + w / 2, y + h / 2 + 3.6, label, size, text_fill, "middle")

    def ribbon(self, x, y, w, steps, h=38, gap=13, fill=DARK):
        """The journey ribbon: numbered chevrons across the top."""
        sw = (w - gap * (len(steps) - 1)) / len(steps)
        for i, s in enumerate(steps):
            self.chevron(x + i * (sw + gap), y, sw, h, s, fill, PAPER, 10)
        return y + h

    def panel(self, x, y, w, h, title, rows, pal=WARN, meta=None, label_w=None):
        """Header-bar panel with label/value rows — used for failure branches,
        decisions, gate criteria. `rows` is a list of (label, value)."""
        self.rect(x, y, w, h, pal.fill, pal.stroke, rx=7)
        self.rect(x, y, w, 27, pal.stroke, rx=7, sw=0)
        self.rect(x, y + 18, w, 9, pal.stroke, rx=0, sw=0)
        self.text(x + 11, y + 18, title, 10.5, PAPER, weight="bold")
        if meta:
            self.text(x + w - 11, y + 18, meta, 8.5, "#ffedd5", "end")
        yy = y + 46
        for lab, val in rows:
            self.text(x + 11, yy, lab, 8.5, pal.deep, weight="bold")
            wrapped = wrap(val, w - 24, 9)
            for j, ln in enumerate(wrapped):
                self.text(x + 11, yy + 12 + j * 11, ln,
                          9, RED_D if lab.lower() == "never" else BODY)
            yy += 12 + len(wrapped) * 11 + 6
        return yy

    def table(self, x, y, cols, rows, widths, row_h=22, head_fill=WASH,
              zebra=True, size=9, head_size=9):
        """A real table — used where a table communicates better than boxes."""
        total = sum(widths)
        self.rect(x, y, total, row_h, head_fill, HAIR, rx=4, sw=1)
        cx = x
        for c, cw in zip(cols, widths):
            self.text(cx + 8, y + row_h / 2 + 3.4, c, head_size, INK, weight="bold")
            cx += cw
        yy = y + row_h
        for i, r in enumerate(rows):
            if zebra and i % 2 == 1:
                self.rect(x, yy, total, row_h, BAND, rx=0, sw=0)
            cx = x
            for cell, cw in zip(r, widths):
                fill, weight, t = BODY, None, cell
                if isinstance(cell, tuple):
                    t, fill = cell[0], cell[1]
                    weight = cell[2] if len(cell) > 2 else None
                self.text(cx + 8, yy + row_h / 2 + 3.4, t, size, fill, weight=weight)
                cx += cw
            self.line(x, yy + row_h, x + total, yy + row_h, HAIR, 0.8)
            yy += row_h
        self.rect(x, y, total, yy - y, "none", HAIR, rx=4, sw=1)
        return yy

    def legend(self, x, y, items, rule=True):
        """items: list of (kind, label). kind in
        sync|async|blocked|ctrl:X|dot:N|swatch:#hex|dash|text"""
        if rule:
            self.line(x, y - 12, self.w - x, y - 12, HAIR, 1)
        cx = x
        for kind, label in items:
            if kind == "sync":
                self.line(cx, y + 2, cx + 34, y + 2, BODY, 1.5, marker="arr"); cx += 42
            elif kind == "async":
                self.line(cx, y + 2, cx + 34, y + 2, GHOST, 1.5, dash="6 4", marker="arrGray")
                cx += 42
            elif kind == "blocked":
                self.line(cx, y + 2, cx + 34, y + 2, RED, 1.5, marker="arrRed"); cx += 42
            elif kind.startswith("ctrl:"):
                self.ctrl(cx, y - 6, kind.split(":")[1]); cx += 30
            elif kind.startswith("dot:"):
                self.dot(cx + 8, y + 1, kind.split(":")[1]); cx += 24
            elif kind.startswith("swatch:"):
                _, f, s = kind.split(":")
                self.rect(cx, y - 7, 16, 14, f, s, rx=3, sw=1.3); cx += 24
            elif kind == "dash":
                self.rect(cx, y - 7, 16, 14, PAPER, GHOST, rx=3, sw=1.3, dash="3 2"); cx += 24
            self.text(cx, y + 6, label, 9.5, MUTED)
            cx += len(label) * CW + 30

    def footer(self, left, right=None, y=None):
        y = y or self.h - 22
        self.text(52, y, left, 9.5, FAINT, weight="bold")
        if right:
            self.text(self.w - 52, y, right, 9.5, FAINT, "end")

    def status_banner(self, x, y, w, text_main, text_sub=None, pal=WARN, h=44):
        self.rect(x, y, w, h, pal.fill, pal.stroke, rx=7, sw=1.6)
        self.text(x + 16, y + (19 if text_sub else 26), text_main, 12, pal.deep, weight="bold")
        if text_sub:
            self.text(x + 16, y + 35, text_sub, 9.5, MUTED)

    # ----------------------------------------------------------------- render
    def render(self) -> str:
        defs = [
            f'<marker id="arr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" '
            f'markerHeight="7" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" '
            f'fill="{BODY}"/></marker>',
            f'<marker id="arrGray" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" '
            f'markerHeight="7" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" '
            f'fill="{GHOST}"/></marker>',
            f'<marker id="arrRed" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" '
            f'markerHeight="7" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" '
            f'fill="{RED}"/></marker>',
            f'<marker id="arrGreen" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" '
            f'markerHeight="7" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" '
            f'fill="#059669"/></marker>',
        ] + self._defs_extra
        head = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.w}" height="{self.h}" '
                f'viewBox="0 0 {self.w} {self.h}" font-family="{self.family}">')
        return "\n".join([head, "<defs>", *defs, "</defs>",
                          f'<rect width="{self.w}" height="{self.h}" fill="{PAPER}"/>',
                          *self.o, "</svg>", ""])


DRAFT = ("DRAFT FOR REVIEW · v0.2 · not approved, not authorised for development · "
         "AI-drafted; no human signature")
