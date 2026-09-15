"""Illustrated scenes for the first ARB deck. Geometric symbols, not photographs.

HA-02: pictures explain recorded design. They do not invent services.
Pin: Aurora PostgreSQL (not public RDS). Java 21 / Spring Boot 3.5 (not Jetty 21).
Saga is orchestrated by Journey Orchestration #9, not a BPM product.
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

NAVY = (0x00, 0x33, 0x66)
GOLD = (0xC4, 0xA3, 0x5A)
WHITE = (255, 255, 255)
INK = (0x1E, 0x29, 0x3B)
MUTED = (0x57, 0x65, 0x7A)
ICE = (0xF4, 0xF7, 0xFB)
TEAL = (0x0E, 0x74, 0x90)
LINE = (0xE2, 0xE8, 0xF0)
SKIN = (0xE8, 0xC4, 0xA8)
SOFT_TEAL = (0xE6, 0xF4, 0xF7)
SOFT_GOLD = (0xFB, 0xF4, 0xE4)
SOFT_NAVY = (0xE8, 0xEE, 0xF5)
CRIMSON = (0xB9, 0x1C, 0x1C)


def _font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    name = "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"
    path = Path("/usr/share/fonts/truetype/dejavu") / name
    try:
        return ImageFont.truetype(str(path), size)
    except OSError:
        return ImageFont.load_default()


def _rr(d: ImageDraw.ImageDraw, xy, r, fill, outline=None, width=3):
    d.rounded_rectangle(xy, radius=r, fill=fill, outline=outline, width=width)


def _center(d, xy, text, font, fill):
    x0, y0, x1, y1 = xy
    bbox = d.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    d.text((x0 + (x1 - x0 - tw) / 2, y0 + (y1 - y0 - th) / 2 - 2), text, font=font, fill=fill)


def _wrap(d, text, font, max_w):
    words = text.split()
    lines, cur = [], ""
    for w in words:
        t = (cur + " " + w).strip()
        if d.textlength(t, font=font) <= max_w:
            cur = t
        else:
            if cur:
                lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    return lines


def _arrow(d, x0, y, x1, color=GOLD, width=8):
    d.line([(x0, y), (x1 - 14, y)], fill=color, width=width)
    d.polygon([(x1, y), (x1 - 18, y - 12), (x1 - 18, y + 12)], fill=color)


def _cloud(d, cx, cy, s, fill=TEAL):
    r = s / 3
    d.ellipse([cx - s * 0.55, cy - r * 0.2, cx - s * 0.05, cy + r * 1.15], fill=fill)
    d.ellipse([cx - s * 0.15, cy - r * 0.2, cx + s * 0.55, cy + r * 1.15], fill=fill)
    d.ellipse([cx - r * 0.85, cy - r * 1.05, cx + r * 0.85, cy + r * 0.35], fill=fill)


def _shield(d, cx, cy, s, fill=NAVY, stroke=GOLD):
    pts = [
        (cx, cy - s * 0.55),
        (cx + s * 0.42, cy - s * 0.28),
        (cx + s * 0.38, cy + s * 0.18),
        (cx, cy + s * 0.55),
        (cx - s * 0.38, cy + s * 0.18),
        (cx - s * 0.42, cy - s * 0.28),
    ]
    d.polygon(pts, fill=fill, outline=stroke)
    d.line([(cx - s * 0.12, cy + 4), (cx - 2, cy + s * 0.18), (cx + s * 0.18, cy - s * 0.12)], fill=stroke, width=5)


def _hex(d, cx, cy, r, fill, outline=None):
    pts = [
        (cx + r * 0.87, cy + r * 0.5),
        (cx, cy + r),
        (cx - r * 0.87, cy + r * 0.5),
        (cx - r * 0.87, cy - r * 0.5),
        (cx, cy - r),
        (cx + r * 0.87, cy - r * 0.5),
    ]
    d.polygon(pts, fill=fill, outline=outline)


def _building(d, x, y, w, h, fill=NAVY):
    _rr(d, [x, y, x + w, y + h], 8, fill)
    d.rectangle([x + w * 0.38, y + h * 0.62, x + w * 0.62, y + h], fill=SOFT_NAVY)
    for row in range(3):
        for col in range(3):
            if row == 2 and col == 1:
                continue
            wx = x + 12 + col * (w - 24) / 3
            wy = y + 14 + row * (h * 0.18)
            d.rectangle([wx, wy, wx + 18, wy + 16], fill=GOLD if (row + col) % 2 == 0 else WHITE)


def _person(d, cx, feet_y, h, cloth, device="tablet"):
    """Standing figure. feet_y is baseline. h is full height."""
    head_r = int(h * 0.10)
    head_cy = int(feet_y - h + head_r + h * 0.02)
    d.ellipse([cx - head_r, head_cy - head_r, cx + head_r, head_cy + head_r], fill=SKIN)
    d.ellipse([cx - head_r + 4, head_cy - head_r - 4, cx + head_r - 4, head_cy - 2], fill=NAVY if cloth == NAVY else TEAL)
    tw, th = int(h * 0.28), int(h * 0.32)
    top = head_cy + head_r + int(h * 0.02)
    _rr(d, [cx - tw // 2, top, cx + tw // 2, top + th], int(h * 0.06), cloth)
    ly = top + th - 4
    lw, lh = int(h * 0.09), int(h * 0.28)
    gap = int(h * 0.04)
    _rr(d, [cx - gap - lw, ly, cx - gap, ly + lh], 10, cloth)
    _rr(d, [cx + gap, ly, cx + gap + lw, ly + lh], 10, cloth)
    d.ellipse([cx - gap - lw - 4, ly + lh - 8, cx - gap + 6, ly + lh + 10], fill=NAVY)
    d.ellipse([cx + gap - 6, ly + lh - 8, cx + gap + lw + 4, ly + lh + 10], fill=NAVY)
    if device == "tablet":
        dx = cx + tw // 2 + 6
        dy = top + int(th * 0.08)
        dw, dh = int(h * 0.22), int(h * 0.30)
        _rr(d, [dx, dy, dx + dw, dy + dh], 10, INK)
        _rr(d, [dx + 6, dy + 8, dx + dw - 6, dy + dh - 10], 6, WHITE)
        for i, done in enumerate((True, True, False)):
            yy = dy + 18 + i * int(dh * 0.22)
            d.rounded_rectangle([dx + 14, yy, dx + 26, yy + 12], 3, outline=TEAL, width=2)
            if done:
                d.line([(dx + 16, yy + 6), (dx + 20, yy + 10), (dx + 24, yy + 3)], fill=TEAL, width=2)
            d.rectangle([dx + 32, yy + 3, dx + dw - 14, yy + 9], fill=LINE)
        d.line([(cx + tw // 2 - 4, top + 20), (dx + 8, dy + dh // 2)], fill=SKIN, width=10)
    elif device == "phone":
        dx = cx + tw // 2 + 4
        dy = top + int(th * 0.15)
        dw, dh = int(h * 0.12), int(h * 0.22)
        _rr(d, [dx, dy, dx + dw, dy + dh], 8, INK)
        _rr(d, [dx + 4, dy + 8, dx + dw - 4, dy + dh - 8], 4, WHITE)
        d.ellipse([dx + dw // 2 - 4, dy + 2, dx + dw // 2 + 4, dy + 8], outline=GOLD, width=2)
        d.line([(cx + tw // 2 - 2, top + 24), (dx + 4, dy + dh // 2)], fill=SKIN, width=9)


def _icon_plate(d, x, y, s, kind, ring=None):
    """Draw a recognisable symbol in a circle. ring highlights a gate."""
    d.ellipse([x, y, x + s, y + s], fill=WHITE, outline=ring or TEAL, width=8 if ring else 5)
    cx, cy, r = x + s / 2, y + s / 2, s * 0.32
    if kind == "cloud":
        _cloud(d, cx, cy, s * 0.55)
    elif kind == "id":
        _rr(d, [cx - r, cy - r * 0.7, cx + r, cy + r * 0.9], 8, SOFT_NAVY, NAVY, 3)
        d.ellipse([cx - r * 0.35, cy - r * 0.45, cx + r * 0.05, cy + r * 0.05], fill=NAVY)
        d.polygon(
            [(cx - r * 0.55, cy + r * 0.7), (cx + r * 0.25, cy + r * 0.7), (cx - r * 0.15, cy + r * 0.15)],
            fill=NAVY,
        )
        d.rectangle([cx + r * 0.2, cy - r * 0.2, cx + r * 0.75, cy - r * 0.05], fill=GOLD)
        d.rectangle([cx + r * 0.2, cy + r * 0.05, cx + r * 0.75, cy + r * 0.2], fill=LINE)
    elif kind == "suit":
        _rr(d, [cx - r * 0.7, cy - r * 0.9, cx + r * 0.7, cy + r * 0.9], 8, WHITE, TEAL, 3)
        d.polygon(
            [
                (cx - r * 0.15, cy - r * 0.95),
                (cx + r * 0.35, cy - r * 0.7),
                (cx + r * 0.15, cy - r * 0.55),
                (cx - r * 0.35, cy - r * 0.8),
            ],
            fill=GOLD,
        )
        for i, ok in enumerate((1, 1, 1, 0)):
            yy = cy - r * 0.35 + i * r * 0.38
            d.rounded_rectangle([cx - r * 0.45, yy, cx - r * 0.22, yy + r * 0.22], 3, outline=TEAL, width=2)
            if ok:
                d.line(
                    [(cx - r * 0.42, yy + r * 0.1), (cx - r * 0.35, yy + r * 0.18), (cx - r * 0.25, yy + r * 0.04)],
                    fill=TEAL,
                    width=3,
                )
            d.rectangle([cx - r * 0.12, yy + r * 0.04, cx + r * 0.5, yy + r * 0.16], fill=LINE)
    elif kind == "otp":
        _rr(d, [cx - r * 0.45, cy - r * 0.9, cx + r * 0.45, cy + r * 0.9], 14, INK)
        _rr(d, [cx - r * 0.35, cy - r * 0.7, cx + r * 0.35, cy + r * 0.65], 6, WHITE)
        for i in range(4):
            xx = cx - r * 0.28 + i * r * 0.18
            d.ellipse([xx, cy - r * 0.05, xx + r * 0.12, cy + r * 0.07], fill=TEAL if i < 3 else LINE)
    elif kind == "quote":
        _rr(d, [cx - r * 0.7, cy - r * 0.9, cx + r * 0.65, cy + r * 0.9], 8, WHITE, NAVY, 3)
        d.polygon(
            [(cx + r * 0.25, cy - r * 0.9), (cx + r * 0.65, cy - r * 0.9), (cx + r * 0.65, cy - r * 0.5)],
            fill=GOLD,
        )
        for i in range(3):
            d.rectangle(
                [cx - r * 0.45, cy - r * 0.25 + i * r * 0.28, cx + r * 0.4, cy - r * 0.12 + i * r * 0.28],
                fill=LINE,
            )
        d.ellipse([cx + r * 0.15, cy + r * 0.35, cx + r * 0.7, cy + r * 0.9], fill=TEAL)
        _center(d, [cx + r * 0.15, cy + r * 0.35, cx + r * 0.7, cy + r * 0.9], "Rs", _font(int(s * 0.12), True), WHITE)
    elif kind == "proposal":
        _rr(d, [cx - r * 0.75, cy - r * 0.85, cx + r * 0.75, cy + r * 0.85], 8, WHITE, TEAL, 3)
        d.rectangle([cx - r * 0.5, cy - r * 0.6, cx + r * 0.5, cy - r * 0.4], fill=NAVY)
        for i in range(4):
            d.rectangle([cx - r * 0.5, cy - r * 0.2 + i * r * 0.22, cx + r * 0.5, cy - r * 0.1 + i * r * 0.22], fill=LINE)
        d.ellipse([cx + r * 0.2, cy + r * 0.15, cx + r * 0.85, cy + r * 0.8], outline=GOLD, width=4)
        d.line([(cx + r * 0.32, cy + r * 0.5), (cx + r * 0.48, cy + r * 0.65), (cx + r * 0.75, cy + r * 0.28)], fill=GOLD, width=4)
    elif kind == "pay":
        _rr(d, [cx - r * 0.85, cy - r * 0.55, cx + r * 0.85, cy + r * 0.55], 12, NAVY)
        d.rectangle([cx - r * 0.85, cy - r * 0.25, cx + r * 0.85, cy - r * 0.05], fill=GOLD)
        d.rectangle([cx - r * 0.55, cy + r * 0.15, cx + r * 0.15, cy + r * 0.32], fill=WHITE)
        d.ellipse([cx + r * 0.35, cy + r * 0.05, cx + r * 0.7, cy + r * 0.4], outline=WHITE, width=3)
    elif kind == "policy":
        _rr(d, [cx - r * 0.7, cy - r * 0.9, cx + r * 0.7, cy + r * 0.85], 8, SOFT_GOLD, GOLD, 4)
        d.polygon([(cx, cy - r * 1.05), (cx + r * 0.35, cy - r * 0.55), (cx - r * 0.35, cy - r * 0.55)], fill=NAVY)
        d.ellipse([cx - r * 0.35, cy - r * 0.1, cx + r * 0.35, cy + r * 0.6], outline=TEAL, width=4)
        d.polygon(
            [
                (cx, cy - r * 0.05),
                (cx + r * 0.12, cy + r * 0.22),
                (cx + r * 0.38, cy + r * 0.22),
                (cx + r * 0.16, cy + r * 0.38),
                (cx + r * 0.25, cy + r * 0.62),
                (cx, cy + r * 0.46),
                (cx - r * 0.25, cy + r * 0.62),
                (cx - r * 0.16, cy + r * 0.38),
                (cx - r * 0.38, cy + r * 0.22),
                (cx - r * 0.12, cy + r * 0.22),
            ],
            fill=GOLD,
        )
    elif kind == "logs":
        _rr(d, [cx - r * 0.7, cy - r * 0.85, cx + r * 0.7, cy + r * 0.85], 8, WHITE, TEAL, 3)
        for i in range(5):
            d.rectangle([cx - r * 0.5, cy - r * 0.6 + i * r * 0.28, cx + r * 0.5, cy - r * 0.48 + i * r * 0.28], fill=TEAL if i % 2 == 0 else LINE)
    elif kind == "lock":
        _rr(d, [cx - r * 0.55, cy - r * 0.05, cx + r * 0.55, cy + r * 0.75], 10, NAVY)
        d.arc([cx - r * 0.38, cy - r * 0.7, cx + r * 0.38, cy + r * 0.15], 0, 180, fill=GOLD, width=6)
        d.ellipse([cx - 8, cy + r * 0.22, cx + 8, cy + r * 0.42], fill=GOLD)
    elif kind == "ctrl":
        _shield(d, cx, cy, s * 0.7, NAVY, GOLD)
    elif kind == "runtime":
        _hex(d, cx, cy, r, TEAL, NAVY)
        _center(d, [cx - r, cy - r * 0.4, cx + r, cy + r * 0.4], "OK", _font(int(s * 0.16), True), WHITE)
    elif kind == "globe":
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=NAVY, width=4)
        d.ellipse([cx - r * 0.4, cy - r, cx + r * 0.4, cy + r], outline=TEAL, width=3)
        d.arc([cx - r, cy - r * 0.35, cx + r, cy + r * 0.35], 0, 360, fill=NAVY, width=3)
    elif kind == "db":
        d.ellipse([cx - r * 0.7, cy - r * 0.85, cx + r * 0.7, cy - r * 0.45], fill=TEAL)
        d.rectangle([cx - r * 0.7, cy - r * 0.65, cx + r * 0.7, cy + r * 0.55], fill=TEAL)
        d.ellipse([cx - r * 0.7, cy + r * 0.35, cx + r * 0.7, cy + r * 0.75], fill=NAVY)
    elif kind == "bucket":
        d.polygon(
            [(cx - r * 0.7, cy - r * 0.4), (cx + r * 0.7, cy - r * 0.4), (cx + r * 0.5, cy + r * 0.8), (cx - r * 0.5, cy + r * 0.8)],
            fill=GOLD,
            outline=NAVY,
        )
        d.rectangle([cx - r * 0.55, cy - r * 0.15, cx + r * 0.55, cy], fill=NAVY)
    elif kind == "cache":
        for i, col in enumerate((TEAL, NAVY, GOLD)):
            yy = cy - r * 0.6 + i * r * 0.45
            d.ellipse([cx - r * 0.65, yy, cx + r * 0.65, yy + r * 0.35], fill=col)
    elif kind == "stream":
        for i in range(3):
            yy = cy - r * 0.55 + i * r * 0.45
            d.polygon(
                [(cx - r * 0.75, yy + r * 0.12), (cx - r * 0.25, yy + r * 0.22), (cx - r * 0.75, yy + r * 0.32)],
                fill=TEAL,
            )
            d.rectangle([cx - r * 0.2, yy + r * 0.14, cx + r * 0.7, yy + r * 0.28], fill=NAVY)
    elif kind == "key":
        d.ellipse([cx - r * 0.7, cy - r * 0.35, cx - r * 0.05, cy + r * 0.35], outline=GOLD, width=6)
        d.rectangle([cx - r * 0.1, cy - r * 0.12, cx + r * 0.75, cy + r * 0.12], fill=NAVY)
        d.rectangle([cx + r * 0.35, cy + r * 0.12, cx + r * 0.5, cy + r * 0.4], fill=NAVY)
        d.rectangle([cx + r * 0.55, cy + r * 0.12, cx + r * 0.7, cy + r * 0.3], fill=NAVY)
    elif kind == "watch":
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=NAVY, width=5)
        d.line([(cx, cy), (cx, cy - r * 0.55)], fill=TEAL, width=4)
        d.line([(cx, cy), (cx + r * 0.4, cy + r * 0.15)], fill=GOLD, width=4)
        d.ellipse([cx - 6, cy - 6, cx + 6, cy + 6], fill=NAVY)
    elif kind == "ecr":
        _rr(d, [cx - r * 0.7, cy - r * 0.4, cx + r * 0.7, cy + r * 0.6], 8, TEAL)
        d.polygon([(cx - r * 0.7, cy - r * 0.4), (cx, cy - r * 0.85), (cx + r * 0.7, cy - r * 0.4)], fill=NAVY)
    elif kind == "alb":
        d.rectangle([cx - r * 0.85, cy - r * 0.12, cx + r * 0.85, cy + r * 0.12], fill=TEAL)
        d.ellipse([cx - r * 0.85, cy - r * 0.55, cx - r * 0.35, cy - r * 0.05], fill=NAVY)
        d.ellipse([cx + r * 0.35, cy - r * 0.55, cx + r * 0.85, cy - r * 0.05], fill=NAVY)
        d.ellipse([cx - r * 0.25, cy + r * 0.15, cx + r * 0.25, cy + r * 0.65], fill=GOLD)
    elif kind == "api":
        _rr(d, [cx - r * 0.75, cy - r * 0.55, cx + r * 0.75, cy + r * 0.55], 10, NAVY)
        _center(d, [cx - r, cy - r * 0.3, cx + r, cy + r * 0.3], "API", _font(int(s * 0.16), True), WHITE)
    elif kind == "search":
        d.ellipse([cx - r * 0.55, cy - r * 0.65, cx + r * 0.25, cy + r * 0.15], outline=NAVY, width=5)
        d.line([(cx + r * 0.15, cy + r * 0.05), (cx + r * 0.65, cy + r * 0.65)], fill=GOLD, width=6)


def _station(d, x, y, kind, title, sub, plate=168, highlight=False):
    _icon_plate(d, x + 16, y, plate, kind, ring=GOLD if highlight else None)
    f1, f2 = _font(22, True), _font(16)
    tw = d.textlength(title, font=f1)
    d.text((x + (200 - tw) / 2, y + plate + 16), title, font=f1, fill=NAVY)
    tw = d.textlength(sub, font=f2)
    d.text((x + (200 - tw) / 2, y + plate + 46), sub, font=f2, fill=MUTED)
    if highlight:
        badge = [x + 40, y - 36, x + 160, y - 4]
        _rr(d, badge, 10, GOLD)
        _center(d, badge, "GATE", _font(14, True), NAVY)


def render_journey(path: Path) -> Path:
    W, H = 2400, 1100
    im = Image.new("RGB", (W, H), ICE)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 900, W, H], fill=SOFT_NAVY)
    _rr(d, [40, 24, 2360, 118], 16, WHITE, LINE, 2)
    d.text((64, 40), "One assisted Life sale", font=_font(34, True), fill=NAVY)
    d.text((64, 82), "Read left to right. Suitability is a gate. Consent and premium are the customer's phone.", font=_font(18), fill=MUTED)

    _rr(d, [40, 150, 310, 880], 20, WHITE, NAVY, 3)
    d.rectangle([40, 150, 310, 210], fill=NAVY)
    _center(d, [40, 150, 310, 210], "RM  ·  NIP-APP", _font(16, True), WHITE)
    _person(d, 175, 820, 520, NAVY, "tablet")
    d.text((78, 840), "Specified Person", font=_font(16, True), fill=NAVY)

    _rr(d, [340, 150, 2050, 620], 24, WHITE, GOLD, 4)
    stations = [
        ("id", "Identify", "CIF on bank path", False),
        ("suit", "Suitability", "No quote without it", True),
        ("otp", "Consent", "OTP on customer phone", False),
        ("quote", "Quote", "Hub, then adapter", False),
        ("proposal", "Proposal", "Application + UW", False),
        ("pay", "Premium", "Customer device", False),
        ("policy", "Policy", "Only when reconciled", False),
    ]
    x0, gap, y = 360, 238, 190
    for i, (kind, title, sub, hi) in enumerate(stations):
        x = x0 + i * gap
        _station(d, x, y, kind, title, sub, plate=150, highlight=hi)
        if i < len(stations) - 1:
            _arrow(d, x + 178, y + 75, x + gap - 4)
        if kind in ("otp", "pay"):
            _person(d, x + 100, 860, 210, TEAL, "phone")

    _rr(d, [2090, 150, 2360, 880], 20, WHITE, TEAL, 3)
    d.rectangle([2090, 150, 2360, 210], fill=TEAL)
    _center(d, [2090, 150, 2360, 210], "Customer", _font(16, True), WHITE)
    _person(d, 2225, 820, 480, TEAL, "phone")
    d.text((2124, 840), "OTP  ·  3-D Secure", font=_font(15, True), fill=TEAL)

    _rr(d, [340, 930, 2050, 1068], 16, WHITE, GOLD, 3)
    d.text((370, 958), "Orchestrated saga", font=_font(20, True), fill=GOLD)
    d.text(
        (640, 958),
        "Journey Orchestration (#9) is the one record of where the sale is.  HTTP 200 is not a policy.",
        font=_font(18),
        fill=NAVY,
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, "PNG")
    return path


def render_two_sittings(path: Path) -> Path:
    W, H = 2400, 1100
    im = Image.new("RGB", (W, H), ICE)
    d = ImageDraw.Draw(im)

    _rr(d, [40, 40, 1160, 1060], 28, WHITE, TEAL, 5)
    d.rectangle([40, 40, 1160, 150], fill=TEAL)
    _center(d, [40, 40, 1160, 150], "This sitting  —  intended design", _font(30, True), WHITE)
    d.ellipse([250, 190, 690, 630], outline=GOLD, width=14)
    _center(d, [250, 230, 690, 470], "35", _font(96, True), GOLD)
    _center(d, [250, 470, 690, 560], "min walk", _font(22), MUTED)
    d.ellipse([620, 430, 980, 790], outline=TEAL, width=10)
    _center(d, [620, 470, 980, 680], "25", _font(64, True), TEAL)
    _center(d, [620, 660, 980, 740], "min board", _font(18), MUTED)

    tiles = [
        (90, 830, "id", "Assisted Life"),
        (430, 830, "quote", "Dev + UAT"),
        (770, 830, "proposal", "Comments"),
    ]
    for x, y, kind, label in tiles:
        d.ellipse([x, y, x + 88, y + 88], fill=SOFT_TEAL, outline=TEAL, width=3)
        _icon_plate(d, x + 8, y + 8, 72, kind)
        d.text((x + 100, y + 28), label, font=_font(20, True), fill=NAVY)

    _rr(d, [1240, 40, 2360, 1060], 28, WHITE, NAVY, 5)
    d.rectangle([1240, 40, 2360, 150], fill=NAVY)
    _center(d, [1240, 40, 2360, 150], "After UAT  —  production sitting", _font(28, True), WHITE)
    stamps = [
        (1320, 200, "logs", "Logs", "Capturing, scrubbed, retained"),
        (1840, 200, "lock", "Security", "Ops live  ·  VA/PT dated"),
        (1320, 540, "ctrl", "Controls", "Compliance in the pack"),
        (1840, 540, "runtime", "Runtime", "Day-one services proven"),
    ]
    for x, y, kind, title, sub in stamps:
        _rr(d, [x, y, x + 460, y + 280], 20, SOFT_NAVY, GOLD, 3)
        _icon_plate(d, x + 156, y + 24, 148, kind)
        _center(d, [x, y + 180, x + 460, y + 228], title, _font(24, True), NAVY)
        _center(d, [x, y + 220, x + 460, y + 268], sub, _font(16), MUTED)
    d.text((1320, 860), "We do not bring that evidence today.", font=_font(22, True), fill=NAVY)
    d.text((1320, 910), "A longer version of this deck is not a production ARB.", font=_font(18), fill=MUTED)
    d.text((1320, 960), "Kalpana schedules sitting two after UAT has run.", font=_font(16), fill=MUTED)
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, "PNG")
    return path


def render_problem(path: Path) -> Path:
    W, H = 2400, 1100
    im = Image.new("RGB", (W, H), ICE)
    d = ImageDraw.Draw(im)
    tiles = [
        ("suit", "Own the evidence", "Suitability, consent, payment, issuance — bank-held"),
        ("otp", "No shortcuts", "A missing OTP is a failed sale, not paperwork"),
        ("pay", "Customer pays", "Premium never on a staff device"),
        ("policy", "Not a redirect", "An aggregator portal is not an audit pack"),
    ]
    w = 540
    x = 40
    for kind, title, sub in tiles:
        _rr(d, [x, 60, x + w, 1040], 24, WHITE, LINE, 3)
        _icon_plate(d, x + 150, 140, 240, kind)
        _center(d, [x, 430, x + w, 520], title, _font(28, True), NAVY)
        yy = 560
        for line in _wrap(d, sub, _font(20), 460):
            tw = d.textlength(line, font=_font(20))
            d.text((x + (w - tw) / 2, yy), line, font=_font(20), fill=MUTED)
            yy += 36
        cues = {
            "suit": "On the RM glass",
            "otp": "On the customer phone",
            "pay": "Never a staff device",
            "policy": "Bank-held pack",
        }
        _rr(d, [x + 40, 880, x + w - 40, 980], 14, SOFT_GOLD, GOLD, 2)
        _center(d, [x + 40, 880, x + w - 40, 980], cues[kind], _font(18, True), NAVY)
        x += w + 30
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, "PNG")
    return path


def render_inbound(path: Path) -> Path:
    W, H = 2400, 1100
    im = Image.new("RGB", (W, H), ICE)
    d = ImageDraw.Draw(im)
    d.text((48, 28), "How a session reaches the application", font=_font(32, True), fill=NAVY)
    d.text((48, 74), "You cannot curl a pod from the internet. No public load balancer on the workload.", font=_font(18), fill=MUTED)
    hops = [
        ("Device", "RM or customer"),
        ("Cloudflare", "CDN / DDoS"),
        ("F5-XC", "WAF  ·  SaaS"),
        ("API Gateway", "Schema, throttle"),
        ("Internal ALB", "Private, in-VPC"),
        ("NIP BFF", "Tokens stay here"),
    ]
    y = 220
    x = 50
    gap = 390
    for i, (title, sub) in enumerate(hops):
        _rr(d, [x, y, x + 330, y + 520], 22, WHITE, TEAL if i < 5 else NAVY, 4)
        cx, cy = x + 165, y + 170
        if i == 0:
            _person(d, cx, y + 300, 220, NAVY, "tablet")
        elif i == 1:
            _cloud(d, cx, cy, 110)
        elif i == 2:
            _shield(d, cx, cy + 10, 150)
        elif i == 3:
            _rr(d, [cx - 70, cy - 50, cx + 70, cy + 80], 8, NAVY)
            _center(d, [cx - 70, cy - 20, cx + 70, cy + 50], "API", _font(22, True), WHITE)
        elif i == 4:
            d.rectangle([cx - 80, cy - 20, cx + 80, cy + 40], fill=TEAL)
            _center(d, [cx - 80, cy - 20, cx + 80, cy + 40], "ALB", _font(22, True), WHITE)
        else:
            _hex(d, cx, cy, 70, NAVY, GOLD)
            _center(d, [cx - 60, cy - 24, cx + 60, cy + 24], "BFF", _font(18, True), WHITE)
        d.text((x + 24, y + 380), title, font=_font(22, True), fill=NAVY)
        d.text((x + 24, y + 430), sub, font=_font(16), fill=MUTED)
        if i < len(hops) - 1:
            _arrow(d, x + 338, y + 250, x + gap - 8)
        x += gap
    _rr(d, [50, 820, 2350, 1040], 18, SOFT_GOLD, GOLD, 3)
    d.text((80, 860), "ADR-018", font=_font(22, True), fill=GOLD)
    d.text((220, 862), "Payment callbacks use a separate API Gateway route. Flutter never calls Apigee.", font=_font(20), fill=NAVY)
    d.text((80, 930), "A public ALB in Dev is still a public target. We will not invent a second ingress to go faster.", font=_font(18), fill=MUTED)
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, "PNG")
    return path


def render_outbound(path: Path) -> Path:
    W, H = 2400, 1100
    im = Image.new("RGB", (W, H), ICE)
    d = ImageDraw.Draw(im)
    d.text((48, 28), "How the platform calls a partner", font=_font(32, True), fill=NAVY)
    d.text((48, 74), "1SB allowlists Apigee, not our NAT. Internal bank APIs stay private.", font=_font(18), fill=MUTED)
    hops = [
        ("EKS pod", "Adapter in cluster"),
        ("Transit Gateway", "Spoke attachment"),
        ("Network Firewall", "Inspection hop"),
        ("Apigee", "Outbound API plane"),
        ("Partner", "1SB or bank API"),
    ]
    y = 220
    x = 80
    gap = 460
    for i, (title, sub) in enumerate(hops):
        _rr(d, [x, y, x + 390, y + 500], 22, WHITE, GOLD if i == 3 else TEAL, 4)
        cx, cy = x + 195, y + 180
        if i == 0:
            _hex(d, cx, cy, 80, TEAL, NAVY)
            _center(d, [cx - 70, cy - 22, cx + 70, cy + 22], "pod", _font(18, True), WHITE)
        elif i == 1:
            d.ellipse([cx - 90, cy - 90, cx + 90, cy + 90], outline=NAVY, width=10)
            d.ellipse([cx - 30, cy - 30, cx + 30, cy + 30], fill=GOLD)
        elif i == 2:
            _rr(d, [cx - 80, cy - 70, cx + 80, cy + 80], 6, NAVY)
            d.rectangle([cx - 80, cy - 20, cx + 80, cy], fill=CRIMSON)
        elif i == 3:
            _building(d, cx - 70, cy - 80, 140, 160, TEAL)
        else:
            _cloud(d, cx - 40, cy, 70, MUTED)
            _rr(d, [cx + 10, cy - 40, cx + 90, cy + 50], 8, SOFT_NAVY, NAVY, 3)
            _center(d, [cx + 10, cy - 20, cx + 90, cy + 30], "1SB", _font(16, True), NAVY)
        d.text((x + 28, y + 360), title, font=_font(22, True), fill=NAVY)
        d.text((x + 28, y + 410), sub, font=_font(16), fill=MUTED)
        if i < len(hops) - 1:
            _arrow(d, x + 400, y + 240, x + gap - 10)
        x += gap
    _rr(d, [80, 820, 2320, 1040], 18, WHITE, CRIMSON, 3)
    d.text((110, 860), "FORBIDDEN", font=_font(20, True), fill=CRIMSON)
    d.text((320, 862), "Java  →  https://*.1silverbullet.tech     ·     pod  →  internet  →  Cloudflare  →  bank API", font=_font(18), fill=NAVY)
    d.text((110, 930), "Adapter base URL is the Apigee proxy. I will not read IPs in the room. Dev uses stubs.", font=_font(18), fill=MUTED)
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, "PNG")
    return path


def render_envs(path: Path) -> Path:
    W, H = 2400, 1100
    im = Image.new("RGB", (W, H), ICE)
    d = ImageDraw.Draw(im)
    d.text((48, 24), "How Dev and UAT are isolated", font=_font(32, True), fill=NAVY)
    d.text((48, 70), "Five Control Tower accounts. Isolation is two VPCs, not a sixth account. Production is drawn only for blast radius.", font=_font(18), fill=MUTED)
    accounts = [
        (60, "Shared", "Images · state · runners", TEAL, False),
        (520, "Security", "Trail · detection · config", TEAL, False),
        (980, "Network", "Inspection · attachments", TEAL, False),
        (1900, "Production", "Out of scope today", MUTED, True),
    ]
    for x, title, sub, col, dim in accounts:
        _rr(d, [x, 140, x + 420, 430], 18, WHITE, col, 3)
        _building(d, x + 150, 170, 120, 130, col if not dim else LINE)
        _center(d, [x, 310, x + 420, 370], title, _font(22, True), MUTED if dim else NAVY)
        _center(d, [x, 365, x + 420, 415], sub, _font(15), MUTED)

    _rr(d, [60, 480, 1840, 1040], 22, WHITE, TEAL, 5)
    d.rectangle([60, 480, 1840, 560], fill=TEAL)
    _center(d, [60, 480, 1840, 560], "UAT account  —  two VPCs, two route tables, two namespaces", _font(22, True), WHITE)
    _rr(d, [100, 600, 900, 980], 18, SOFT_TEAL, TEAL, 3)
    _center(d, [100, 620, 900, 700], "vpc-dev", _font(32, True), NAVY)
    _center(d, [100, 710, 900, 770], "Synthetic  ·  stubs", _font(20), MUTED)
    _center(d, [100, 800, 900, 860], "Cannot ride a production CBS route", _font(16), MUTED)
    _rr(d, [980, 600, 1780, 980], 18, SOFT_GOLD, GOLD, 3)
    _center(d, [980, 620, 1780, 700], "vpc-uat", _font(32, True), NAVY)
    _center(d, [980, 710, 1780, 770], "Masked  ·  bank UAT path", _font(20), MUTED)
    _center(d, [980, 800, 1780, 860], "VPN first, then existing Direct Connect", _font(16), MUTED)

    _rr(d, [1900, 480, 2340, 1040], 18, ICE, LINE, 3)
    d.text((1940, 620), "Not vending", font=_font(22, True), fill=MUTED)
    d.text((1940, 680), "production", font=_font(22, True), fill=MUTED)
    d.text((1940, 760), "in this sitting.", font=_font(18), fill=MUTED)
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, "PNG")
    return path


def render_services(path: Path) -> Path:
    """Four clusters from R0-LLD §1 BOM — not a shopping list of unused products."""
    W, H = 2400, 1100
    im = Image.new("RGB", (W, H), ICE)
    d = ImageDraw.Draw(im)
    d.text((48, 18), "What we run  —  edge, AWS, operate", font=_font(30, True), fill=NAVY)
    d.text((48, 58), "Pinned from LLD §1. Aurora PostgreSQL, not public RDS. OpenSearch is search, not the 7-year audit pack.", font=_font(16), fill=MUTED)
    groups = [
        (
            TEAL,
            "Edge — not in our VPC",
            [
                ("cloud", "Cloudflare"),
                ("ctrl", "F5-XC WAF"),
                ("api", "Apigee outbound"),
                ("policy", "1SB adapter path"),
                ("pay", "Bank payment GW"),
                ("id", "AD-verify API"),
            ],
        ),
        (
            NAVY,
            "Ingress & compute",
            [
                ("globe", "Route 53"),
                ("api", "API Gateway"),
                ("alb", "Internal ALB"),
                ("runtime", "Amazon EKS"),
                ("ecr", "Amazon ECR"),
                ("ctrl", "Transit Gateway"),
            ],
        ),
        (
            GOLD,
            "Data & events",
            [
                ("db", "Aurora PostgreSQL"),
                ("cache", "DynamoDB"),
                ("bucket", "S3 Object Lock"),
                ("cache", "ElastiCache Valkey"),
                ("stream", "Amazon MSK"),
                ("search", "OpenSearch"),
            ],
        ),
        (
            TEAL,
            "Security & operate",
            [
                ("key", "AWS KMS"),
                ("lock", "Secrets Manager"),
                ("id", "IRSA / IAM"),
                ("watch", "CloudWatch"),
                ("logs", "CloudTrail"),
                ("ctrl", "GuardDuty"),
            ],
        ),
    ]
    gw = 560
    x = 40
    for col, title, items in groups:
        _rr(d, [x, 100, x + gw, 1060], 20, WHITE, col, 4)
        d.rectangle([x, 100, x + gw, 178], fill=col)
        _center(d, [x, 100, x + gw, 178], title, _font(20, True), WHITE if col != GOLD else NAVY)
        yy = 200
        for kind, item in items:
            _rr(d, [x + 24, yy, x + gw - 24, yy + 120], 14, ICE, LINE, 2)
            _icon_plate(d, x + 40, yy + 16, 88, kind)
            d.text((x + 150, yy + 40), item, font=_font(22, True), fill=NAVY)
            yy += 136
        x += gw + 20
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, "PNG")
    return path


def render_stack(path: Path) -> Path:
    W, H = 2400, 1100
    im = Image.new("RGB", (W, H), ICE)
    d = ImageDraw.Draw(im)
    d.text((48, 18), "How we build it", font=_font(30, True), fill=NAVY)
    d.text((48, 58), "One Flutter project. Java 21 LTS, Spring Boot 3.5, EKS. Orchestrated saga — not choreography, not a BPM product.", font=_font(16), fill=MUTED)

    # Experience
    _rr(d, [40, 110, 760, 820], 24, WHITE, TEAL, 5)
    d.rectangle([40, 110, 760, 200], fill=TEAL)
    _center(d, [40, 110, 760, 200], "Experience", _font(28, True), WHITE)
    _rr(d, [120, 250, 250, 480], 18, INK)
    _rr(d, [132, 272, 238, 452], 10, WHITE)
    d.ellipse([168, 256, 200, 268], outline=GOLD, width=2)
    _rr(d, [300, 230, 520, 500], 16, INK)
    _rr(d, [314, 250, 506, 470], 10, WHITE)
    for i, done in enumerate((True, True, False)):
        yy = 290 + i * 50
        d.rounded_rectangle([340, yy, 362, yy + 22], 4, outline=TEAL, width=2)
        if done:
            d.line([(344, yy + 12), (350, yy + 18), (358, yy + 6)], fill=TEAL, width=3)
        d.rectangle([378, yy + 6, 480, yy + 16], fill=LINE)
    _rr(d, [560, 270, 700, 430], 10, NAVY)
    d.rectangle([572, 292, 688, 316], fill=GOLD)
    d.rectangle([580, 340, 640, 352], fill=WHITE)
    _center(d, [80, 540, 720, 600], "Flutter", _font(32, True), NAVY)
    _center(d, [80, 600, 720, 650], "One project  ·  Web  ·  APK  ·  IPA", _font(18), MUTED)
    _center(d, [80, 700, 720, 760], "Device holds a session, never a token", _font(16), TEAL)

    # Services
    _rr(d, [820, 110, 1540, 820], 24, WHITE, NAVY, 5)
    d.rectangle([820, 110, 1540, 200], fill=NAVY)
    _center(d, [820, 110, 1540, 200], "Services", _font(28, True), WHITE)
    d.ellipse([1040, 250, 1320, 530], fill=SOFT_TEAL, outline=NAVY, width=6)
    _center(d, [1040, 320, 1320, 400], "21", _font(64, True), NAVY)
    _center(d, [1040, 400, 1320, 470], "Java LTS", _font(22, True), TEAL)
    for hx, label, fill in ((960, "Boot 3.5", TEAL), (1180, "EKS", NAVY), (1400, "Keycloak", TEAL)):
        _hex(d, hx, 620, 52, fill, GOLD)
        tw = d.textlength(label, font=_font(14, True))
        d.text((hx - tw / 2, 690), label, font=_font(14, True), fill=NAVY)
    _center(d, [840, 740, 1520, 785], "Spring Boot 3.5  ·  microservices on EKS", _font(18), MUTED)
    _center(d, [840, 785, 1520, 820], "Not Jetty 21. Not Cognito. Not a warehouse in R0.", _font(16), MUTED)

    # Coordination
    _rr(d, [1600, 110, 2360, 820], 24, WHITE, GOLD, 5)
    d.rectangle([1600, 110, 2360, 200], fill=GOLD)
    _center(d, [1600, 110, 2360, 200], "Coordination", _font(28, True), NAVY)
    xs = [1720, 1980, 2240]
    labs = ["Saga", "Outbox", "Adapter"]
    for i, (cx, lab) in enumerate(zip(xs, labs)):
        d.ellipse([cx - 90, 280, cx + 90, 460], fill=WHITE, outline=NAVY, width=6)
        _center(d, [cx - 90, 300, cx + 90, 430], lab, _font(22, True), NAVY)
        if i < 2:
            _arrow(d, cx + 96, 370, xs[i + 1] - 96, NAVY, 6)
    d.text((1660, 520), "Orchestrated, not choreographed", font=_font(18, True), fill=NAVY)
    d.text((1660, 570), "Journey #9 owns the queryable record", font=_font(16), fill=MUTED)
    d.text((1660, 620), "Outbox is source of truth  ·  MSK is fan-out", font=_font(16), fill=MUTED)
    d.text((1660, 670), "1SB JSON dies in the adapter / ACL", font=_font(16), fill=MUTED)
    d.text((1660, 720), "Token-hiding BFF  ·  schema-per-context Aurora", font=_font(16), fill=MUTED)

    _rr(d, [40, 860, 2360, 1060], 18, WHITE, NAVY, 3)
    d.text((70, 900), "Primary patterns on the sale path", font=_font(20, True), fill=NAVY)
    d.text(
        (70, 950),
        "Saga (orchestrated)   ·   Transactional outbox then Amazon MSK   ·   Anti-corruption adapter   ·   Token-hiding BFF   ·   Aurora, one cluster, schema per context",
        font=_font(18),
        fill=INK,
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, "PNG")
    return path


def render_microservices(path: Path) -> Path:
    W, H = 2400, 1100
    im = Image.new("RGB", (W, H), ICE)
    d = ImageDraw.Draw(im)
    d.text((48, 24), "How the services sit", font=_font(32, True), fill=NAVY)
    d.text((48, 70), "Flutter talks only to the BFF. The Hub is the only door to a provider. Health later must not rewrite Life.", font=_font(18), fill=MUTED)
    lanes = [
        (NAVY, "Channel", ["NIP-APP  Flutter", "NIP BFF", "PDP / AuthZ"]),
        (TEAL, "Sale spine", ["Lead", "Journey saga", "Suitability", "Consent"]),
        (GOLD, "Product", ["Catalogue", "Quote", "Proposal"]),
        (NAVY, "Fulfilment", ["Payment", "Policy", "Audit WORM"]),
        (TEAL, "Integration", ["Hub", "1SB adapter", "Apigee"]),
    ]
    y = 130
    for col, name, chips in lanes:
        _rr(d, [40, y, 2360, y + 160], 18, WHITE, col, 3)
        d.rectangle([40, y, 300, y + 160], fill=col)
        _center(d, [40, y, 300, y + 160], name, _font(22, True), WHITE if col != GOLD else NAVY)
        n = len(chips)
        chip_w = 360 if n >= 4 else 400
        step = 380 if n >= 4 else 420
        x = 340
        glyph = {
            "NIP-APP  Flutter": "otp",
            "NIP BFF": "runtime",
            "PDP / AuthZ": "lock",
            "Lead": "id",
            "Journey saga": "stream",
            "Suitability": "suit",
            "Consent": "otp",
            "Catalogue": "search",
            "Quote": "quote",
            "Proposal": "proposal",
            "Payment": "pay",
            "Policy": "policy",
            "Audit WORM": "bucket",
            "Hub": "alb",
            "1SB adapter": "api",
            "Apigee": "globe",
        }
        for chip in chips:
            _rr(d, [x, y + 28, x + chip_w, y + 132], 14, ICE, col, 3)
            _icon_plate(d, x + 16, y + 42, 76, glyph.get(chip, "runtime"))
            d.text((x + 110, y + 62), chip, font=_font(20, True), fill=NAVY)
            x += step
        y += 185
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, "PNG")
    return path


def render_all(dest: Path) -> dict[str, Path]:
    dest.mkdir(parents=True, exist_ok=True)
    return {
        "journey": render_journey(dest / "journey.png"),
        "sittings": render_two_sittings(dest / "sittings.png"),
        "problem": render_problem(dest / "problem.png"),
        "inbound": render_inbound(dest / "inbound.png"),
        "outbound": render_outbound(dest / "outbound.png"),
        "envs": render_envs(dest / "envs.png"),
        "services": render_services(dest / "services.png"),
        "stack": render_stack(dest / "stack.png"),
        "micro": render_microservices(dest / "micro.png"),
    }
