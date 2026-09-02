#!/usr/bin/env python3
"""A very small deterministic diagram canvas that emits self-contained SVG.

Why this exists instead of a layout engine
------------------------------------------
Graphviz decides positions for you. That is the whole appeal, and it is also why
its output drifts: nodes land where the ranking algorithm puts them, edges take
curved or diagonal paths, and `splines=ortho` — its 90-degree mode — detaches
edge labels and routes lines straight through cluster borders.

Here, every element is placed at a coordinate the author chose, and every
connector is a sequence of axis-aligned segments the author chose. Nothing is
inferred. That is more typing and it is the only way to get alignment that holds
still between renders.

Icons come from the `diagrams` pip package (official AWS, Kubernetes, Flutter and
Argo art) and are embedded as base64, so the SVG is self-contained: no external
request, no vendored image file in this repository.

Conventions
-----------
* A node's (x, y) is the centre of its ICON. Labels hang below it and never
  participate in routing.
* Ports are named by compass side: 'T', 'B', 'L', 'R'.
* `link()` emits axis-aligned segments only. If a route needs a corridor, name
  the corridor with `lane=` rather than letting anything guess.
"""
from __future__ import annotations

import base64
import functools
import html
import os
import pathlib

# ---------------------------------------------------------------- icon lookup
def _icon_root() -> pathlib.Path:
    env = os.environ.get("DIAGRAM_ICON_ROOT")
    if env:
        return pathlib.Path(env)
    import diagrams  # the wheel ships resources/ next to the package
    return pathlib.Path(diagrams.__file__).resolve().parent.parent / "resources"


@functools.lru_cache(maxsize=None)
def icon(rel: str) -> str:
    """`icon("aws/network/route-53.png")` -> a base64 data URI."""
    path = _icon_root() / rel
    if not path.is_file():
        raise FileNotFoundError("icon not found: %s" % path)
    return "data:image/png;base64," + base64.b64encode(path.read_bytes()).decode("ascii")


FONT = "'Helvetica Neue',Helvetica,Arial,sans-serif"


def _esc(t: str) -> str:
    return html.escape(str(t), quote=True)


# ------------------------------------------------------------------- elements
class Port:
    __slots__ = ("x", "y", "side")

    def __init__(self, x: float, y: float, side: str):
        self.x, self.y, self.side = float(x), float(y), side


class Node:
    def __init__(self, x, y, size, label_bottom=None):
        self.x, self.y, self.size = float(x), float(y), float(size)
        # A caption hangs below the icon. A link leaving the bottom must start
        # below it, or the line is drawn straight through the node's own text.
        self.label_bottom = label_bottom

    def port(self, side: str, at: float | None = None) -> Port:
        h = self.size / 2.0
        if side == "T":
            return Port(self.x if at is None else at, self.y - h, "T")
        if side == "B":
            y = self.y + h
            if self.label_bottom is not None:
                y = max(y, self.label_bottom)
            return Port(self.x if at is None else at, y, "B")
        if side == "L":
            return Port(self.x - h, self.y if at is None else at, "L")
        if side == "R":
            return Port(self.x + h, self.y if at is None else at, "R")
        raise ValueError(side)


class Group:
    def __init__(self, x, y, w, h):
        self.x, self.y, self.w, self.h = float(x), float(y), float(w), float(h)

    @property
    def cx(self): return self.x + self.w / 2.0

    @property
    def cy(self): return self.y + self.h / 2.0

    def port(self, side: str, at: float | None = None) -> Port:
        if side == "T":
            return Port(self.cx if at is None else at, self.y, "T")
        if side == "B":
            return Port(self.cx if at is None else at, self.y + self.h, "B")
        if side == "L":
            return Port(self.x, self.cy if at is None else at, "L")
        if side == "R":
            return Port(self.x + self.w, self.cy if at is None else at, "R")
        raise ValueError(side)


# --------------------------------------------------------------------- canvas
class Canvas:
    def __init__(self, width, height, title, subtitle="", bg="#ffffff"):
        self.w, self.h, self.bg = float(width), float(height), bg
        self._defs_colors: set[str] = set()
        self._layers = {"group": [], "edge": [], "node": [], "over": []}
        self._title, self._subtitle = title, subtitle

    # ---- primitives
    def _add(self, layer, markup):
        self._layers[layer].append(markup)

    def rect(self, x, y, w, h, fill="none", stroke="none", width=1.5,
             dash=None, radius=14, layer="group", opacity=1.0):
        d = ' stroke-dasharray="%s"' % dash if dash else ""
        self._add(layer, '<rect x="%.1f" y="%.1f" width="%.1f" height="%.1f" rx="%d" '
                         'fill="%s" fill-opacity="%.2f" stroke="%s" stroke-width="%.1f"%s/>'
                  % (x, y, w, h, radius, fill, opacity, stroke, width, d))

    def text(self, x, y, s, size=13, color="#0f172a", anchor="middle", bold=False,
             layer="node", halo=None, italic=False, spacing=None):
        st = ' font-weight="700"' if bold else ""
        it = ' font-style="italic"' if italic else ""
        ls = ' letter-spacing="%.2f"' % spacing if spacing else ""
        halo_attr = ""
        if halo:
            halo_attr = (' stroke="%s" stroke-width="4.5" stroke-linejoin="round" '
                         'paint-order="stroke"' % halo)
        self._add(layer, '<text x="%.1f" y="%.1f" font-family="%s" font-size="%.1f" '
                         'fill="%s" text-anchor="%s"%s%s%s%s>%s</text>'
                  % (x, y, FONT, size, color, anchor, st, it, ls, halo_attr, _esc(s)))

    def lines(self, x, y, rows, size=12.5, color="#0f172a", anchor="middle",
              leading=None, bold_first=False, layer="node", halo=None):
        """Stacked text rows. `y` is the baseline of the first row."""
        lead = leading if leading is not None else size + 3.5
        for i, row in enumerate(rows):
            if not row:
                continue
            self.text(x, y + i * lead, row, size=size, color=color, anchor=anchor,
                      bold=(bold_first and i == 0), layer=layer, halo=halo)

    # ---- elements
    def group(self, label, x, y, w, h, stroke="#475569", fill="#f8fafc",
              dash=None, label_size=16, sub=None, radius=16, opacity=1.0,
              label_color=None, width=2.0, label_align="left"):
        self.rect(x, y, w, h, fill=fill, stroke=stroke, width=width, dash=dash,
                  radius=radius, layer="group", opacity=opacity)
        lc = label_color or stroke
        lx, anch = (x + 18, "start") if label_align == "left" else (x + w - 18, "end")
        if label:
            self.text(lx, y + label_size + 12, label, size=label_size,
                      color=lc, anchor=anch, bold=True, layer="group", halo=None)
        if sub:
            self.text(lx, y + label_size * 2 + 14, sub, size=label_size - 3.5,
                      color=lc, anchor=anch, layer="group", halo=None)
        return Group(x, y, w, h)

    def node(self, rel_icon, x, y, rows=(), size=62, label_size=12.5,
             bold_first=False, color="#0f172a", ghost=False):
        h = size / 2.0
        if ghost:
            self.rect(x - h - 6, y - h - 6, size + 12, size + 12, fill="#ffffff",
                      stroke="#cbd5e1", width=1.6, dash="6 5", radius=10, layer="node")
        self._add("node", '<image href="%s" x="%.1f" y="%.1f" width="%.1f" '
                          'height="%.1f" preserveAspectRatio="xMidYMid meet"/>'
                  % (icon(rel_icon), x - h, y - h, size, size))
        bottom = None
        if rows:
            lead = label_size + 3.5
            self.lines(x, y + h + 17, rows, size=label_size, color=color,
                       bold_first=bold_first)
            bottom = y + h + 17 + (len(rows) - 1) * lead + 7
        return Node(x, y, size, label_bottom=bottom)

    def ghost(self, x, y, w, h, rows, label_size=12.5):
        self.rect(x - w / 2, y - h / 2, w, h, fill="#ffffff", stroke="#cbd5e1",
                  width=1.8, dash="7 6", radius=10, layer="node")
        self.lines(x, y - (len(rows) - 1) * 8 + 4, rows, size=label_size,
                   color="#94a3b8", halo=None)

    # ---- connectors
    def link(self, a: Port, b: Port, color="#334155", width=2.6, dash=None,
             label=None, lane=None, arrow=True, label_at=0.5, label_dx=0.0,
             label_dy=-9.0, via=None, label_anchor="middle", label_size=12.5,
             start_dot=False, label_seg=None):
        pts = via if via else self._route(a, b, lane)
        d = " ".join(("M" if i == 0 else "L") + "%.1f,%.1f" % p for i, p in enumerate(pts))
        self._defs_colors.add(color)
        da = ' stroke-dasharray="%s"' % dash if dash else ""
        mk = ' marker-end="url(#ar%s)"' % color.lstrip("#") if arrow else ""
        self._add("edge", '<path d="%s" fill="none" stroke="%s" stroke-width="%.1f" '
                          'stroke-linejoin="miter" stroke-linecap="butt"%s%s/>'
                  % (d, color, width, da, mk))
        if start_dot:
            self._add("edge", '<circle cx="%.1f" cy="%.1f" r="%.1f" fill="%s"/>'
                      % (pts[0][0], pts[0][1], width + 1.4, color))
        if label:
            lx, ly = self._label_point(pts, label_at, label_seg)
            rows = label if isinstance(label, (list, tuple)) else [label]
            self._label_plate(lx + label_dx, ly + label_dy, rows, label_size,
                              label_anchor)
            self.lines(lx + label_dx, ly + label_dy, rows, size=label_size,
                       color=color, anchor=label_anchor, layer="over")
        return pts

    def _label_plate(self, x, y, rows, size, anchor, pad=5.0):
        """A white plate behind edge text. Cheaper and far more portable than
        relying on paint-order, which cairosvg and older librsvg ignore."""
        lead = size + 3.5
        w = max(len(r) for r in rows) * size * 0.545 + pad * 2
        h = len(rows) * lead + pad
        x0 = {"middle": x - w / 2, "start": x - pad, "end": x - w + pad}[anchor]
        self.rect(x0, y - size - pad + 2, w, h, fill="#ffffff", stroke="none",
                  radius=3, layer="over", opacity=0.94)

    @staticmethod
    def _route(a: Port, b: Port, lane):
        ax, ay, bx, by = a.x, a.y, b.x, b.y
        horiz_a, horiz_b = a.side in "LR", b.side in "LR"
        if horiz_a and horiz_b:
            if abs(ay - by) < 0.5:
                return [(ax, ay), (bx, ay)]
            lx = lane if lane is not None else (ax + bx) / 2.0
            return [(ax, ay), (lx, ay), (lx, by), (bx, by)]
        if (not horiz_a) and (not horiz_b):
            if abs(ax - bx) < 0.5:
                return [(ax, ay), (ax, by)]
            ly = lane if lane is not None else (ay + by) / 2.0
            return [(ax, ay), (ax, ly), (bx, ly), (bx, by)]
        if horiz_a:                      # side-out, then top/bottom-in
            return [(ax, ay), (bx, ay), (bx, by)]
        return [(ax, ay), (ax, by), (bx, by)]    # top/bottom-out, then side-in

    @staticmethod
    def _label_point(pts, at, seg=None):
        """Longest segment by default — the only place a label reliably fits.
        Pass `seg` to put it on a specific one."""
        if seg is not None:
            best = (pts[seg], pts[seg + 1])
        else:
            best, blen = (pts[0], pts[1]), -1.0
            for p, q in zip(pts, pts[1:]):
                ln = abs(p[0] - q[0]) + abs(p[1] - q[1])
                if ln > blen:
                    best, blen = (p, q), ln
        (px, py), (qx, qy) = best
        return px + (qx - px) * at, py + (qy - py) * at

    # ---- output
    def save(self, path):
        defs = "".join(
            '<marker id="ar%s" viewBox="0 0 10 10" refX="9.2" refY="5" '
            'markerWidth="9" markerHeight="9" markerUnits="userSpaceOnUse" '
            'orient="auto"><path d="M0.5,0.8 L9.6,5 L0.5,9.2 z" fill="%s"/></marker>'
            % (c.lstrip("#"), c) for c in sorted(self._defs_colors))
        body = "".join(self._layers["group"] + self._layers["edge"] +
                       self._layers["node"] + self._layers["over"])
        head = ""
        if self._title:
            head += ('<text x="%.1f" y="58" font-family="%s" font-size="30" '
                     'font-weight="700" fill="#0f172a" text-anchor="middle">%s</text>'
                     % (self.w / 2, FONT, _esc(self._title)))
        if self._subtitle:
            head += ('<text x="%.1f" y="92" font-family="%s" font-size="16.5" '
                     'fill="#64748b" text-anchor="middle">%s</text>'
                     % (self.w / 2, FONT, _esc(self._subtitle)))
        svg = ('<svg xmlns="http://www.w3.org/2000/svg" '
               'xmlns:xlink="http://www.w3.org/1999/xlink" width="%.0f" height="%.0f" '
               'viewBox="0 0 %.0f %.0f"><defs>%s</defs>'
               '<rect width="100%%" height="100%%" fill="%s"/>%s%s</svg>'
               % (self.w, self.h, self.w, self.h, defs, self.bg, head, body))
        pathlib.Path(path).write_text(svg, encoding="utf-8")
        return path
