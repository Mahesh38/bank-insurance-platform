#!/usr/bin/env python3
"""Generate bank-ARB reviewer artefacts from the markdown prerequisite pack.

Canonical facts stay in docs/architecture/*.md (HA-02). These files are the
formats a bank ARB can open in a meeting. They are not signatures.

    python3 scripts/architecture/build_arb_reviewer_pack.py

Requires: python-pptx, openpyxl, python-docx, reportlab. Optional: cairosvg
(embeds topology / payment / DR pictures in the deck).
"""

from __future__ import annotations

import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PACK = ROOT / "docs/architecture/ARB-PREREQUISITE-PACK.md"
CIS = ROOT / "docs/architecture/arb-prerequisites/CIS-CLASSIFICATION-PROPOSAL.md"
INTEG = ROOT / "docs/architecture/arb-prerequisites/INTEGRATION-AND-DEPENDENCY-MATRIX.md"
CLOUD = ROOT / "docs/architecture/arb-prerequisites/CLOUD-SHARED-RESPONSIBILITY.md"
LIFE = ROOT / "docs/architecture/arb-prerequisites/TECHNOLOGY-LIFECYCLE-AND-EXIT.md"
SBOM = ROOT / "docs/architecture/arb-prerequisites/SBOM-RUNTIME-INVENTORY.md"
OUT = ROOT / "docs/architecture/arb-prerequisites/exports"
STAMP = "2026-09-14"
DISCLAIMER = (
    "AI-DRAFTED evidence. Not ARB approval, not T4 Architecture, not Board 4, "
    "not Board 6 CIS registration, not a VA/PT pass. HA-02: ADR / LLD / NFR win."
)
FOOTER = f"AU Bank NIP R0 · ARB pack {STAMP} · {DISCLAIMER}"

NAVY = (0x00, 0x33, 0x66)
GOLD = (0xC4, 0xA3, 0x5A)
WHITE = (0xFF, 0xFF, 0xFF)
INK = (0x1E, 0x29, 0x3B)
MUTED = (0x47, 0x55, 0x69)
READY_C = (0x16, 0x65, 0x34)
PARTIAL_C = (0xB4, 0x53, 0x09)
HUMAN_C = (0xB9, 0x1C, 0x1C)
NOTYET_C = (0x4B, 0x55, 0x63)


def clean(text: str) -> str:
    text = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", text)
    text = text.replace("**", "").replace("`", "").replace("*", "")
    text = text.replace("<br/>", " ").replace("<br>", " ")
    return re.sub(r"\s+", " ", text).strip()


def parse_tables(path: Path) -> list[list[list[str]]]:
    tables: list[list[list[str]]] = []
    current: list[list[str]] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith("|"):
            cells = [clean(c) for c in line.strip().strip("|").split("|")]
            if cells and all(re.fullmatch(r"[:\-\s]+", c or "-") for c in cells):
                continue
            current.append(cells)
        elif current:
            tables.append(current)
            current = []
    if current:
        tables.append(current)
    return tables


def status_rgb(value: str) -> tuple[int, int, int]:
    u = value.upper()
    if "HUMAN" in u:
        return HUMAN_C
    if "NOT-YET" in u or "NOT YET" in u:
        return NOTYET_C
    if "PARTIAL" in u:
        return PARTIAL_C
    if "READY" in u:
        return READY_C
    return INK


def first_table_with(tables: list[list[list[str]]], header_cell: str) -> list[list[str]]:
    needle = header_cell.lower()
    for table in tables:
        if table and any(needle in cell.lower() for cell in table[0]):
            return table
    raise KeyError(f"no table whose header contains {header_cell!r}")


def load() -> dict:
    pack_tables = parse_tables(PACK)
    integ = parse_tables(INTEG)
    cloud = parse_tables(CLOUD)
    life = parse_tables(LIFE)
    cis = parse_tables(CIS)
    sbom = parse_tables(SBOM)
    return {
        "readiness": first_table_with(pack_tables, "Bank ARB prerequisite"),
        "flows": first_table_with(pack_tables, "Picture"),
        "signoff": first_table_with(pack_tables, "What they sign"),
        "integrations": first_table_with(integ, "Counterpart"),
        "third_parties": first_table_with(integ, "Contract posture"),
        "deps": first_table_with(integ, "What is blocked"),
        "attach": first_table_with(cloud, "Already exists"),
        "raci": first_table_with(cloud, "Control outcome"),
        "accounts": first_table_with(cloud, "If it is compromised"),
        "cis": first_table_with(cis, "Proposed value"),
        "lifecycle": first_table_with(life, "Support posture"),
        "exits": first_table_with(life, "What we keep"),
        "export": first_table_with(life, "Residual"),
        "sbom": first_table_with(sbom, "Coordinate"),
    }


def svg_png(svg: Path, dest: Path, width: int) -> Path | None:
    try:
        import cairosvg
    except ImportError:
        return None
    dest.parent.mkdir(parents=True, exist_ok=True)
    cairosvg.svg2png(url=str(svg), write_to=str(dest), output_width=width)
    return dest


# ---------------------------------------------------------------------------
# PowerPoint
# ---------------------------------------------------------------------------

def build_pptx(data: dict, path: Path, pngs: dict[str, Path | None]) -> None:
    from pptx import Presentation
    from pptx.dml.color import RGBColor
    from pptx.enum.shapes import MSO_SHAPE
    from pptx.enum.text import PP_ALIGN
    from pptx.oxml.ns import qn
    from pptx.util import Emu, Inches, Pt
    from lxml import etree

    prs = Presentation()
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)
    blank = prs.slide_layouts[6]
    navy = RGBColor(*NAVY)
    gold = RGBColor(*GOLD)
    white = RGBColor(*WHITE)
    ink = RGBColor(*INK)
    muted = RGBColor(*MUTED)

    def rgb(t: tuple[int, int, int]) -> RGBColor:
        return RGBColor(*t)

    def set_run(run, text, size=14, bold=False, color=ink, font="Calibri"):
        run.text = text
        run.font.size = Pt(size)
        run.font.bold = bold
        run.font.color.rgb = color
        run.font.name = font

    def add_textbox(slide, l, t, w, h, text, size=14, bold=False, color=ink, align=PP_ALIGN.LEFT):
        box = slide.shapes.add_textbox(Inches(l), Inches(t), Inches(w), Inches(h))
        tf = box.text_frame
        tf.word_wrap = True
        p = tf.paragraphs[0]
        p.alignment = align
        set_run(p.add_run() if p.runs else p.runs[0] if False else _first_run(p), text, size, bold, color)
        return box

    def _first_run(p):
        if p.runs:
            return p.runs[0]
        return p.add_run()

    def fill_shape(shape, color: RGBColor):
        shape.fill.solid()
        shape.fill.fore_color.rgb = color
        shape.line.fill.background()

    def footer(slide, page: int, total: int):
        bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(7.22), Inches(13.333), Inches(0.28))
        fill_shape(bar, navy)
        box = slide.shapes.add_textbox(Inches(0.3), Inches(7.24), Inches(11.4), Inches(0.24))
        p = box.text_frame.paragraphs[0]
        set_run(_first_run(p), FOOTER[:140], 8, False, white)
        num = slide.shapes.add_textbox(Inches(11.9), Inches(7.24), Inches(1.2), Inches(0.24))
        p = num.text_frame.paragraphs[0]
        p.alignment = PP_ALIGN.RIGHT
        set_run(_first_run(p), f"{page} / {total}", 8, False, white)

    def header_bar(slide, title: str):
        bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(0.78))
        fill_shape(bar, navy)
        accent = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0.78), Inches(13.333), Inches(0.06))
        fill_shape(accent, gold)
        box = slide.shapes.add_textbox(Inches(0.4), Inches(0.18), Inches(12.5), Inches(0.5))
        p = box.text_frame.paragraphs[0]
        set_run(_first_run(p), title, 22, True, white)

    def new_slide(title: str):
        s = prs.slides.add_slide(blank)
        header_bar(s, title)
        return s

    def bullets(slide, items, l=0.45, t=1.05, w=12.4, h=5.9, size=16):
        box = slide.shapes.add_textbox(Inches(l), Inches(t), Inches(w), Inches(h))
        tf = box.text_frame
        tf.word_wrap = True
        for i, item in enumerate(items):
            p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
            p.level = 0
            p.space_after = Pt(8)
            set_run(_first_run(p), item, size, False, ink)

    def table(slide, rows, l, t, w, h, font=10, header=True, status_col=None):
        n_rows, n_cols = len(rows), len(rows[0])
        shp = slide.shapes.add_table(n_rows, n_cols, Inches(l), Inches(t), Inches(w), Inches(h))
        tbl = shp.table
        for r, row in enumerate(rows):
            for c, val in enumerate(row):
                cell = tbl.cell(r, c)
                cell.text = ""
                p = cell.text_frame.paragraphs[0]
                p.alignment = PP_ALIGN.LEFT
                color = ink
                bold = r == 0 and header
                if r == 0 and header:
                    color = white
                elif status_col is not None and c == status_col:
                    color = rgb(status_rgb(val))
                    bold = True
                set_run(_first_run(p), val, font, bold, color)
                cell.text_frame.word_wrap = True
                tc = cell._tc
                tcPr = tc.get_or_add_tcPr()
                fill = etree.SubElement(tcPr, qn("a:solidFill"))
                srgb = etree.SubElement(fill, qn("a:srgbClr"))
                if r == 0 and header:
                    srgb.set("val", "003366")
                elif r % 2 == 1:
                    srgb.set("val", "F1F5F9")
                else:
                    srgb.set("val", "FFFFFF")
        return shp

    # Build slides in a list of callables so we know total pages for footers.
    slides_built: list = []

    def finish(slide):
        slides_built.append(slide)
        return slide

    # 1 title
    s = prs.slides.add_slide(blank)
    bg = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(7.5))
    fill_shape(bg, navy)
    goldbar = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(2.55), Inches(13.333), Inches(0.08))
    fill_shape(goldbar, gold)
    box = s.shapes.add_textbox(Inches(0.7), Inches(1.3), Inches(12), Inches(1.1))
    p = box.text_frame.paragraphs[0]
    set_run(_first_run(p), "Architecture Review Board", 18, False, gold)
    box = s.shapes.add_textbox(Inches(0.7), Inches(2.75), Inches(12), Inches(1.4))
    p = box.text_frame.paragraphs[0]
    set_run(_first_run(p), "AU Bank Insurance Distribution Platform (NIP)", 28, True, white)
    p = box.text_frame.add_paragraph()
    set_run(_first_run(p), "R0 assisted Life — 19-item prerequisite walk-in", 20, False, white)
    box = s.shapes.add_textbox(Inches(0.7), Inches(4.5), Inches(12), Inches(1.8))
    tf = box.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    set_run(_first_run(p), "Horizon H0 / R0 only. H1–H3 are direction, not this submission.", 16, False, white)
    p = tf.add_paragraph()
    set_run(_first_run(p), DISCLAIMER, 13, False, gold)
    p = tf.add_paragraph()
    set_run(_first_run(p), "Draft: Mahesh (Board 1 / R2) · Work item ARB-PRE-2026-09-14 · SUG-20260914-xpt", 13, False, white)
    finish(s)

    s = new_slide("What to put in front of the reviewer")
    bullets(
        s,
        [
            "In the room: this PowerPoint (walk-in). Do not open Markdown.",
            "To circulate / print: AU-NIP-R0-ARB-Prerequisite-Pack PDF (or the Word file if they want comments).",
            "When they ask for matrices: AU-NIP-R0-ARB-Matrices.xlsx — integration, RACI, CIS, SBOM, lifecycle, exit.",
            "Ask of this sitting: review the design. Not production authority. Not a CIS register entry.",
            "Fair holds we already name: CIS class, Product volumes, Deepali Board 4, VA/PT date, Apigee IPs, human T4.",
        ],
        size=18,
    )
    finish(s)

    s = new_slide("Business ask (row 1) — PARTIAL until Rajal signs")
    bullets(
        s,
        [
            "One RM, certified Specified Person, sells a complete Life policy (Term or Savings/ULIP) to one ETB customer of one Group A insurer, end to end.",
            "Suitability, customer-device OTP consent, customer-device payment, reconciled issuance, immutable audit.",
            "Licence: IRDAI Composite Corporate Agency CA0515.",
            "Written: BOOT.md scope, CR-010, CR-015 (CANDIDATE transcribed; HUMAN T4 outstanding).",
            "Architecture will not invent a BRD signature, budget memo, or GOV-004 Product counter-sign.",
        ],
        size=17,
    )
    finish(s)

    ready = data["readiness"]
    # skip header, split 1-10 and 11-19; keep #, name, readiness, gap
    compact = [[r[0], r[1], r[2], r[4]] for r in ready]
    s = new_slide("Readiness dashboard — rows 1 to 10")
    table(s, compact[:11], 0.3, 1.0, 12.7, 5.95, font=10, status_col=2)
    finish(s)
    s = new_slide("Readiness dashboard — rows 11 to 19")
    table(s, [compact[0]] + compact[11:], 0.3, 1.0, 12.7, 5.95, font=10, status_col=2)
    finish(s)

    s = new_slide("Honest posture — review, not production authority")
    bullets(
        s,
        [
            "The design is internally consistent enough to REVIEW.",
            "It is not internally consistent enough to approve as production authority.",
            "HUMAN-REQUIRED: CIS register (Shailja + CISO). Architecture proposes Critical; the bank register is SoT.",
            "PARTIAL: business volumes (Rajal CAP-A*), security signature (Deepali), IAM vs bank PAM, CycloneDX CI.",
            "NOT-YET: VA/PT report. CI has Checkstyle, Spotless, ArchUnit, JaCoCo, lockfile SCA only.",
            "OPEN outside the room: DEP-20260914-apg (Apigee IPs), DEP-20260824-dx1 (VPN/DX), DEP-002 (UAT slot).",
        ],
        size=17,
    )
    finish(s)

    s = new_slide("Current state vs target state (rows 3–4)")
    bullets(
        s,
        [
            "Current = H0 / R0 as designed. Nothing is running in a bank prod account yet.",
            "SAD: 03-solution-architecture-r0.md · HLD · LLD · narrative dossier. Unsigned T4.",
            "Target = North Star capability model, horizons H1–H3 + hdl.svg. Do not read H1–H3 as admitted scope.",
            "Release strategy follows business maturity, not architecture completeness (HR-01…HR-05).",
            "Attach to the existing AU cloud estate (BE-01). Do not clone Control Tower, TGW, DX, or FortiGate.",
        ],
        size=17,
    )
    finish(s)

    s = new_slide("Split API plane — ADR-020")
    bullets(
        s,
        [
            "INBOUND: device → Cloudflare → F5-XC → Amazon API Gateway → Internal ALB → nip-web / NIP BFF.",
            "OUTBOUND: pod → TGW → NFW → Apigee → 1SB or private bank API.",
            "FORBIDDEN: Java → https://*.1silverbullet.tech",
            "FORBIDDEN: internal API → internet → Cloudflare → F5",
            "FORBIDDEN: publish spoke NAT EIPs to 1SB. 1SB allowlists Apigee IPs.",
            "Payment: session-create OUT via Apigee; 3-DS on the customer device; callback IN on a separate API Gateway route (TB-6).",
        ],
        size=16,
    )
    finish(s)

    if pngs.get("topology"):
        s = new_slide("Data-flow — R0 platform topology (row 5)")
        s.shapes.add_picture(str(pngs["topology"]), Inches(0.25), Inches(0.95), Inches(12.8), Inches(6.1))
        finish(s)
    if pngs.get("payment"):
        s = new_slide("Payment trust boundary TB-6")
        s.shapes.add_picture(str(pngs["payment"]), Inches(0.35), Inches(1.05), Inches(12.6), Inches(5.9))
        finish(s)

    integ = data["integrations"]
    slim = [[r[0], r[1], r[2], r[4], r[7]] for r in integ]
    s = new_slide("Integration matrix I-01 … I-06 (row 6) — full sheet in Excel")
    table(s, slim[:7], 0.3, 1.0, 12.7, 5.9, font=11)
    finish(s)
    s = new_slide("Integration matrix I-07 … I-12")
    table(s, [slim[0]] + slim[7:], 0.3, 1.0, 12.7, 5.9, font=11)
    finish(s)

    s = new_slide("Forbidden paths and open dependencies")
    bullets(
        s,
        [
            "Defect if drawn: Java → 1SB origin · internal API hairpin via Cloudflare/F5 · spoke NAT EIPs to 1SB · Flutter → Keycloak/Apigee/domain · cross-schema DB grants.",
            "DEP-20260914-apg OPEN — 1SB allowlist + AD-verify/EBS private proxies (Apigee team).",
            "DEP-20260824-eip — must NOT send spoke NAT EIPs to 1SB.",
            "DEP-20260824-dx1 OPEN — uat/prod CBS and AD-verify without stubs.",
            "DEP-002 OPEN — bank caller UAT. DEP-010 OPEN (overdue) — AD technology named; R0 uses AD-verify API, never LDAP.",
        ],
        size=16,
    )
    finish(s)

    s = new_slide("Capacity, availability, RTO / RPO (rows 7–8)")
    bullets(
        s,
        [
            "CAP-A* assumption (not Product-signed): ~100 journey starts/hour BAU, ~7/minute at Q4 peak.",
            "R0 is correctness-constrained, not throughput-constrained. Do not scale from CPU. Aurora connection budget is the one that bites.",
            "Sale-path availability 99.9% (NFR-AVL-01).",
            "RTO ≤ 1 hour. Transactional RPO ≤ 5 minutes. Audit RPO 0.",
            "Unproven until S09 timed restore drill (NFR-DR-04).",
        ],
        size=17,
    )
    finish(s)

    if pngs.get("dr"):
        s = new_slide("DR architecture — warm standby ap-south-2 (row 9)")
        s.shapes.add_picture(str(pngs["dr"]), Inches(0.4), Inches(1.0), Inches(12.5), Inches(6.05))
        finish(s)
    else:
        s = new_slide("DR architecture (row 9)")
        bullets(
            s,
            [
                "Warm standby in ap-south-2 (Hyderabad). Primary ap-south-1 (Mumbai).",
                "Cache / MSK / OpenSearch are NOT replicated (D13–D15). Sessions re-login; outbox is SoT; logs rebuild.",
                "Proof is a timed Ansible drill, not the picture.",
            ],
        )
        finish(s)

    s = new_slide("Security, VA/PT, SBOM (rows 10–12)")
    bullets(
        s,
        [
            "Security architecture is an INPUT to Board 4, not a Board 4 verdict. Deepali signs.",
            "ADR-010 remainder (pod → Apigee inspection) is Deepali’s acceptance, not Architecture’s.",
            "VA/PT: NOT-YET. Do not tell ARB a pentest passed. SAST/ArchUnit/JaCoCo/lockfiles are in CI now. DAST/VA/PT before UAT exit.",
            "SBOM today: 14 Gradle lockfiles, 137 unique runtimeClasspath coordinates. Java 21, Spring Boot 3.5.16.",
            "CycloneDX/SPDX CI artefact is S09. Flutter pubspec.lock is a second inventory, not merged.",
        ],
        size=16,
    )
    finish(s)

    s = new_slide("CIS classification — proposal only (row 2)")
    table(s, data["cis"], 0.35, 1.0, 12.6, 4.4, font=13)
    bullets(
        s,
        [
            "Mahesh does not classify the system into the bank CIS register. Shailja + CISO do.",
            "A lower class would be a written risk acceptance, not a design simplification.",
        ],
        l=0.45,
        t=5.5,
        h=1.5,
        size=15,
    )
    finish(s)

    s = new_slide("Data, IAM/PAM, logging (rows 13–15)")
    bullets(
        s,
        [
            "Classes: PUBLIC / INTERNAL / CONFIDENTIAL / RESTRICTED + [P][F][H][K]. India only (TI-08, FF-08).",
            "Primary ap-south-1, DR ap-south-2. No regulated data outside India.",
            "Workforce identity stays Bank AD (TI-01). R0 reaches it via AD-verify API through Apigee private. Never LDAP from EKS.",
            "AWS human privilege is bank PAM / IAM Identity Center. We consume it. We do not clone an IdP (BE-01).",
            "CloudTrail (who changed AWS) and CloudWatch (how the pod behaves) are both mandatory.",
            "OpenSearch = operational logs, never evidence (ADR-013). Audit is INSERT-only + S3 Object Lock (TI-07).",
        ],
        size=16,
    )
    finish(s)

    s = new_slide("Third parties (row 16) — no new subcontractor")
    tp = [[r[0], r[1], r[3], r[4]] for r in data["third_parties"]]
    table(s, tp, 0.3, 1.0, 12.7, 5.2, font=12)
    finish(s)

    s = new_slide("Five Control Tower accounts — attach, don’t clone (row 17)")
    table(s, data["accounts"], 0.35, 1.0, 12.6, 3.6, font=14)
    bullets(
        s,
        [
            "shared-services, security, network, uat, prod. No separate dev account. No CUG.",
            "UAT hosts vpc-dev + vpc-uat with two route tables so stubs cannot reach prod CBS.",
            "No workload VPC Internet Gateway. Shared-responsibility matrix is the Excel RACI sheet.",
        ],
        t=4.75,
        h=2.2,
        size=15,
    )
    finish(s)

    s = new_slide("Technology lifecycle (row 18) — pins as of 2026-09-14")
    life = [[r[0], r[1], r[3]] for r in data["lifecycle"][:9]]
    table(s, life, 0.3, 1.0, 12.7, 5.9, font=12)
    finish(s)

    s = new_slide("Exit, portability, decommission (row 19)")
    table(s, data["exits"], 0.3, 1.0, 12.7, 2.6, font=13)
    bullets(
        s,
        [
            "One-line exit: turn off the adapter, keep the evidence. 1SB JSON never enters a domain schema.",
            "Canonical model is bank-owned. PVC is not a system of record. Workforce identities stay in AD.",
            "S3 Object Lock retention cannot be shortened by decommission enthusiasm (Shailja).",
            "ARB is not being asked to approve a decommission. Exit is a designed property of R0.",
        ],
        t=3.8,
        h=3.1,
        size=16,
    )
    finish(s)

    s = new_slide("Who must still walk into the room")
    so = data["signoff"]
    table(s, so, 0.3, 1.0, 12.7, 5.5, font=13)
    finish(s)

    s = new_slide("Close — what we are asking ARB to do")
    bullets(
        s,
        [
            "Review the R0 architecture against the 19 bank prerequisites.",
            "Record named holds. Do not treat silence as approval.",
            "Do not convert this pack into production authority, a CIS register row, or a VA/PT pass.",
            "Next humans: Rajal (volumes), Shailja+CISO (CIS), Deepali (Board 4), Mahesh human T4, Shivanshi (Apigee IPs + DR drill record).",
        ],
        size=18,
    )
    finish(s)

    total = len(slides_built)
    for i, slide in enumerate(slides_built, 1):
        if i == 1:
            # title already full-bleed; add page number only
            num = slide.shapes.add_textbox(Inches(11.9), Inches(7.15), Inches(1.2), Inches(0.24))
            p = num.text_frame.paragraphs[0]
            p.alignment = PP_ALIGN.RIGHT
            set_run(_first_run(p), f"{i} / {total}", 9, False, white)
            continue
        footer(slide, i, total)

    prs.save(str(path))


# ---------------------------------------------------------------------------
# Excel
# ---------------------------------------------------------------------------

def build_xlsx(data: dict, path: Path) -> None:
    from openpyxl import Workbook
    from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
    from openpyxl.utils import get_column_letter

    wb = Workbook()
    header_fill = PatternFill("solid", fgColor="003366")
    header_font = Font(name="Calibri", bold=True, color="FFFFFF", size=11)
    wrap = Alignment(wrap_text=True, vertical="center")
    thin = Border(
        left=Side(style="thin", color="CBD5E1"),
        right=Side(style="thin", color="CBD5E1"),
        top=Side(style="thin", color="CBD5E1"),
        bottom=Side(style="thin", color="CBD5E1"),
    )
    fills = {
        "READY": PatternFill("solid", fgColor="DCFCE7"),
        "PARTIAL": PatternFill("solid", fgColor="FEF3C7"),
        "HUMAN-REQUIRED": PatternFill("solid", fgColor="FEE2E2"),
        "NOT-YET": PatternFill("solid", fgColor="E2E8F0"),
    }

    def write_sheet(name: str, rows: list[list[str]], widths=None, status_col=None):
        ws = wb.create_sheet(name[:31])
        for r, row in enumerate(rows, 1):
            for c, val in enumerate(row, 1):
                cell = ws.cell(r, c, val)
                cell.alignment = wrap
                cell.border = thin
                cell.font = header_font if r == 1 else Font(name="Calibri", size=10)
                if r == 1:
                    cell.fill = header_fill
                elif status_col and c == status_col + 1:
                    key = next((k for k in fills if k in str(val).upper()), None)
                    if key:
                        cell.fill = fills[key]
                        cell.font = Font(name="Calibri", size=10, bold=True)
        ws.freeze_panes = "A2"
        ws.auto_filter.ref = ws.dimensions
        ws.row_dimensions[1].height = 22
        if widths:
            for i, w in enumerate(widths, 1):
                ws.column_dimensions[get_column_letter(i)].width = w
        else:
            for i in range(1, len(rows[0]) + 1):
                ws.column_dimensions[get_column_letter(i)].width = 28
        ws.sheet_properties.pageSetUpPr.fitToPage = True
        ws.page_setup.orientation = "landscape"
        ws.page_setup.fitToWidth = 1
        ws.page_setup.fitToHeight = 0
        ws.oddHeader.left.text = "AU Bank NIP R0 ARB matrices"
        ws.oddFooter.left.text = DISCLAIMER[:90]
        return ws

    readme = wb.active
    readme.title = "00_How_to_use"
    notes = [
        ["AU Bank NIP R0 — ARB matrix workbook", ""],
        ["Generated", STAMP],
        ["Work item", "ARB-PRE-2026-09-14 / SUG-20260914-xpt"],
        ["Status", DISCLAIMER],
        ["", ""],
        ["Sheet", "Give this to ARB when they ask for…"],
        ["01_Readiness", "The 19-row prerequisite dashboard"],
        ["02_CIS_Proposal", "Critical Information System classification (proposal only)"],
        ["03_Data_Flows", "Inbound / outbound / payment / trust-boundary index"],
        ["04_Integrations", "Runtime integration matrix I-01…I-12"],
        ["05_Third_Parties", "Subcontractors and already-procured bank platforms"],
        ["06_Open_Dependencies", "IDs that can halt ARB or S09"],
        ["07_Shared_Responsibility", "Cloud RACI (AWS / bank / NIP / Apigee / 1SB)"],
        ["08_Accounts", "Five Control Tower accounts and blast radius"],
        ["09_Lifecycle", "Technology pins and end-of-support"],
        ["10_Exit", "Replace 1SB / leave AWS / decommission NIP"],
        ["11_Decommission_Export", "Store-by-store residual on exit"],
        ["12_SBOM", "137 runtimeClasspath coordinates from Gradle lockfiles"],
        ["13_Sign_off", "Who must still walk into the room"],
        ["", ""],
        ["Rule HA-02", "If a cell disagrees with an ADR, LLD or NFR catalogue, that source wins."],
        ["Do not", "Treat a filled cell as a signature. Agents draft; they do not sign."],
    ]
    for r, row in enumerate(notes, 1):
        readme.cell(r, 1, row[0]).font = Font(name="Calibri", bold=True, color="003366", size=12)
        readme.cell(r, 2, row[1]).alignment = wrap
    readme.column_dimensions["A"].width = 28
    readme.column_dimensions["B"].width = 88

    write_sheet("01_Readiness", data["readiness"], [6, 42, 22, 48, 48], status_col=2)
    write_sheet("02_CIS_Proposal", data["cis"], [28, 90])
    write_sheet("03_Data_Flows", data["flows"], [28, 90])
    write_sheet("04_Integrations", data["integrations"], [8, 28, 22, 16, 42, 32, 22, 28])
    write_sheet("05_Third_Parties", data["third_parties"], [28, 28, 40, 36, 36])
    write_sheet("06_Open_Dependencies", data["deps"], [22, 55, 40])
    write_sheet("07_Shared_Responsibility", data["raci"], [36, 16, 22, 28, 22, 22])
    write_sheet("08_Accounts", data["accounts"], [22, 50, 50])
    write_sheet("09_Lifecycle", data["lifecycle"], [28, 32, 48, 18, 40])
    write_sheet("10_Exit", data["exits"], [28, 48, 40, 32])
    write_sheet("11_Decommission_Export", data["export"], [36, 55, 40])
    write_sheet("12_SBOM", data["sbom"], [55, 70])
    write_sheet("13_Sign_off", data["signoff"], [28, 50, 50])
    wb.save(path)


# ---------------------------------------------------------------------------
# Word + PDF
# ---------------------------------------------------------------------------

def iter_pack_sections() -> list[tuple[str, list[str]]]:
    """Light prose for the leave-behind, taken from the pack (not a second SAD)."""
    return [
        (
            "How to use this document",
            [
                "This is the bank ARB leave-behind for the R0 Insurance Distribution Platform (NIP).",
                "Open the PowerPoint in the room. Use this PDF/Word to circulate. Use the Excel workbook for matrices.",
                DISCLAIMER,
                "Rule HA-02: if a paragraph here disagrees with an ADR, the LLD, the NFR catalogue or the security architecture, that source wins.",
            ],
        ),
        (
            "1. Business ask and sign-off — PARTIAL",
            [
                "Ask (admitted objective): one RM, acting as certified Specified Person, sells a complete Life policy (Term or Savings/ULIP) to one ETB customer of one Group A insurer, end to end, with suitability, customer-device OTP consent, customer-device payment, reconciled issuance and immutable audit (R0-ASSISTED-LIFE-SALE).",
                "Licence: IRDAI Composite Corporate Agency CA0515. Scope is in BOOT.md. CR-010 registered the workstream. CR-015 restates Savings/ULIP (CANDIDATE transcribed; HUMAN T4 outstanding).",
                "This pack does not fill a bank new-application signature block, a budget memo, or a Product Owner counter-signature on GOV-004. Rajal owns that process.",
            ],
        ),
        (
            "2. Critical Information System classification — HUMAN-REQUIRED",
            [
                "Architecture proposes class Critical Information System (CIS). Processing in AWS ap-south-1 with DR replicas in ap-south-2 only. No regulated data outside India.",
                "Shailja S (Board 6) and bank Information Security / CISO enter the bank CIS register. Mahesh does not. A lower class would be a written risk acceptance, not a design simplification.",
            ],
        ),
        (
            "3–4. Solution architecture · current vs target — READY (unsigned)",
            [
                "SAD: docs/platform/ws3-platform/03-solution-architecture-r0.md plus R0-HLD.md and R0-LLD.md. Human T4 Architecture is outstanding.",
                "Current-state is H0/R0 as designed, not as running in prod. Target-state is the North Star at horizons H1–H3. Do not read H1–H3 as admitted scope.",
            ],
        ),
        (
            "5. Data-flow and trust boundaries — READY (unsigned)",
            [
                "Inbound: device → Cloudflare → F5-XC → Amazon API Gateway → Internal ALB → nip-web / NIP BFF (ADR-018).",
                "Outbound: Hub → adapter → Apigee → 1SB (ADR-020). 1SB allowlists Apigee IPs, not spoke NAT EIPs.",
                "Internal bank API: Apigee private → AD-verify / EBS. Forbidden: Cloudflare/F5 hairpin.",
                "Payment: session-create outbound via Apigee; customer 3-DS on own device; callback inbound on a separate API Gateway route (TB-6).",
                "Trust boundaries TB-1…TB-7 live in 04-security-architecture.md. Deepali still owns the threat-model signature.",
            ],
        ),
        (
            "6–9. Integration, capacity, availability, DR",
            [
                "Runtime integrations I-01…I-12 and third parties are in the Excel workbook (sheets 04–06).",
                "Capacity (CAP-A*, unapproved by Product): ~100 journey starts/hour BAU, ~7/minute at Q4 peak. R0 is correctness-constrained. Do not scale from CPU.",
                "Sale-path availability 99.9%. RTO ≤ 1 hour. Transactional RPO ≤ 5 minutes. Audit RPO 0. Unproven until S09 drills (NFR-DR-04).",
                "DR: warm standby ap-south-2. Cache/MSK/OpenSearch are not replicated. Proof is a timed drill, not the picture.",
            ],
        ),
        (
            "10–12. Security, VA/PT, SBOM",
            [
                "Security architecture is an input to Board 4, not a Board 4 verdict. ADR-010 remainder is Deepali’s acceptance.",
                "VA/PT is NOT-YET. CI currently runs Checkstyle, Spotless, ArchUnit, JaCoCo and lockfile SCA. Do not tell ARB a pentest passed.",
                "SBOM: 14 Gradle lockfiles, 137 unique runtimeClasspath coordinates (Excel sheet 12). Java 21, Spring Boot 3.5.16. CycloneDX CI is still S09. Flutter pubspec.lock is not merged.",
            ],
        ),
        (
            "13–15. Data classification, IAM/PAM, logging",
            [
                "PUBLIC / INTERNAL / CONFIDENTIAL / RESTRICTED + [P][F][H][K]. India residency TI-08 / FF-08.",
                "Workforce identity stays Bank AD (TI-01), reached via the AD-verify API through Apigee private. Never LDAP from EKS.",
                "Privileged AWS access is bank PAM / IAM Identity Center. Application IAM is IRSA per deployable.",
                "CloudTrail and CloudWatch are both mandatory. OpenSearch holds operational logs, never evidence (ADR-013). Audit is INSERT-only + S3 Object Lock (TI-07).",
            ],
        ),
        (
            "16–19. Third parties, cloud RACI, lifecycle, exit",
            [
                "No new subcontractor: R0 attaches to already-procured bank platforms (Cloudflare, F5-XC, Apigee, GitLab, AWS, 1SB). 1SB is a provider route (TI-04), not a domain dependency.",
                "Five Control Tower accounts: shared-services, security, network, uat, prod. No separate dev. No CUG. Attach to AU-CTO-NETWORK.",
                "Java 21 and Spring Boot 3.5 are in support. Keycloak / Flutter / EKS pins confirmed by Amit + Shivanshi at S09.",
                "Exit: turn off the adapter, keep the evidence. Three exits are not the same change (replace 1SB / leave AWS / decommission NIP). Kubernetes PVC is not a system of record.",
            ],
        ),
        (
            "Named holds that would be fair",
            [
                "CIS register (row 2). Product volume sign-off (row 7). Deepali Board 4 (row 10). VA/PT plan date (row 11). Apigee written IPs (row 6). Human T4 Architecture (rows 3–4).",
                "Agents draft. They do not sign.",
            ],
        ),
    ]


def build_docx(data: dict, path: Path) -> None:
    from docx import Document
    from docx.enum.text import WD_ALIGN_PARAGRAPH
    from docx.oxml.ns import qn
    from docx.oxml import OxmlElement
    from docx.shared import Inches, Pt, RGBColor

    doc = Document()
    sec = doc.sections[0]
    sec.page_width = Inches(8.27)
    sec.page_height = Inches(11.69)
    sec.left_margin = Inches(0.7)
    sec.right_margin = Inches(0.7)
    sec.top_margin = Inches(0.7)
    sec.bottom_margin = Inches(0.8)

    def shade_header(cell, fill="003366"):
        tc = cell._tc
        tcPr = tc.get_or_add_tcPr()
        shd = OxmlElement("w:shd")
        shd.set(qn("w:fill"), fill)
        shd.set(qn("w:val"), "clear")
        tcPr.append(shd)

    def add_table(rows):
        table = doc.add_table(rows=len(rows), cols=len(rows[0]))
        table.style = "Table Grid"
        for r, row in enumerate(rows):
            for c, val in enumerate(row):
                cell = table.cell(r, c)
                cell.text = val
                for p in cell.paragraphs:
                    p.paragraph_format.space_after = Pt(0)
                    for run in p.runs:
                        run.font.size = Pt(8)
                        run.font.name = "Calibri"
                        if r == 0:
                            run.bold = True
                            run.font.color.rgb = RGBColor(255, 255, 255)
                if r == 0:
                    shade_header(cell)
        doc.add_paragraph()

    h = doc.add_heading("AU Bank Insurance Distribution Platform (NIP)", 0)
    h.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p = doc.add_paragraph()
    run = p.add_run("Architecture Review Board — R0 prerequisite pack (leave-behind)")
    run.bold = True
    p = doc.add_paragraph()
    run = p.add_run(DISCLAIMER)
    run.italic = True
    run.font.color.rgb = RGBColor(185, 28, 28)
    doc.add_paragraph(f"Document ARB-PRE-2026-09-14 · {STAMP} · Draft owner: Mahesh, Board 1 / R2")
    doc.add_paragraph(
        "Companion files: AU-NIP-R0-ARB-Walk-In.pptx (room) · AU-NIP-R0-ARB-Matrices.xlsx (tables)."
    )

    for title, paras in iter_pack_sections():
        doc.add_heading(title, 1)
        for para in paras:
            doc.add_paragraph(para)

    doc.add_heading("Readiness dashboard", 1)
    add_table(data["readiness"])
    doc.add_heading("CIS proposal", 1)
    add_table(data["cis"])
    doc.add_heading("Data flows", 1)
    add_table(data["flows"])
    doc.add_heading("Sign-off routing", 1)
    add_table(data["signoff"])
    doc.add_heading("Accounts", 1)
    add_table(data["accounts"])
    doc.add_heading("Exit scenarios", 1)
    add_table(data["exits"])
    p = doc.add_paragraph()
    p.add_run(
        "Full integration matrix, RACI, lifecycle and the 137-coordinate SBOM are in the Excel workbook — they do not fit a Word page without losing the numbers ARB asked for."
    ).italic = True

    doc.core_properties.title = "AU Bank NIP R0 ARB Prerequisite Pack"
    doc.core_properties.author = "Mahesh — Principal Insurance Platform Architect (draft)"
    doc.core_properties.subject = DISCLAIMER
    doc.save(str(path))


def build_pdf(data: dict, path: Path) -> None:
    from reportlab.lib import colors
    from reportlab.lib.enums import TA_LEFT
    from reportlab.lib.pagesizes import A4
    from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
    from reportlab.lib.units import mm
    from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

    navy = colors.HexColor("#003366")
    gold = colors.HexColor("#C4A35A")
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(name="CoverH", parent=styles["Title"], textColor=navy, fontSize=18, leading=22, spaceAfter=8))
    styles.add(ParagraphStyle(name="Warn", parent=styles["Normal"], textColor=colors.HexColor("#B91C1C"), fontSize=9, leading=12, spaceAfter=8))
    styles.add(ParagraphStyle(name="H", parent=styles["Heading1"], textColor=navy, fontSize=13, spaceBefore=10, spaceAfter=6))
    styles.add(ParagraphStyle(name="B", parent=styles["Normal"], fontSize=9.5, leading=13, spaceAfter=6))
    styles.add(ParagraphStyle(name="Cell", parent=styles["Normal"], fontSize=7, leading=9))
    styles.add(ParagraphStyle(name="CellH", parent=styles["Normal"], fontSize=7, leading=9, textColor=colors.white, fontName="Helvetica-Bold"))

    def P(text, style="B"):
        return Paragraph(text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"), styles[style])

    def pdf_table(rows, col_widths):
        body = []
        for r, row in enumerate(rows):
            styled = []
            for val in row:
                styled.append(Paragraph(val.replace("&", "&amp;"), styles["CellH" if r == 0 else "Cell"]))
            body.append(styled)
        t = Table(body, colWidths=col_widths, repeatRows=1)
        cmds = [
            ("BACKGROUND", (0, 0), (-1, 0), navy),
            ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
            ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("GRID", (0, 0), (-1, -1), 0.3, colors.HexColor("#94A3B8")),
            ("LEFTPADDING", (0, 0), (-1, -1), 3),
            ("RIGHTPADDING", (0, 0), (-1, -1), 3),
            ("TOPPADDING", (0, 0), (-1, -1), 3),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
        ]
        for i, row in enumerate(rows[1:], 1):
            bg = colors.HexColor("#F8FAFC") if i % 2 else colors.white
            cmds.append(("BACKGROUND", (0, i), (-1, i), bg))
            if len(row) >= 3:
                tone = status_rgb(row[2] if len(row) > 2 else "")
                # only colour readiness column when it looks like a status
                if any(k in row[2].upper() for k in ("READY", "PARTIAL", "HUMAN", "NOT-YET", "NOT YET")):
                    cmds.append(("TEXTCOLOR", (2, i), (2, i), colors.Color(tone[0] / 255, tone[1] / 255, tone[2] / 255)))
        t.setStyle(TableStyle(cmds))
        return t

    story = []
    story.append(P("AU Bank Insurance Distribution Platform (NIP)", "CoverH"))
    story.append(P("Architecture Review Board — R0 prerequisite pack (leave-behind)", "B"))
    story.append(P(DISCLAIMER, "Warn"))
    story.append(P(f"Document ARB-PRE-2026-09-14 · {STAMP} · Draft: Mahesh, Board 1 / R2", "B"))
    story.append(P("Room file: AU-NIP-R0-ARB-Walk-In.pptx · Matrices: AU-NIP-R0-ARB-Matrices.xlsx", "B"))
    story.append(Spacer(1, 6))

    for title, paras in iter_pack_sections():
        story.append(P(title, "H"))
        for para in paras:
            story.append(P(para, "B"))

    story.append(P("Readiness dashboard (19 bank prerequisites)", "H"))
    slim = [[r[0], r[1], r[2], r[4]] for r in data["readiness"]]
    story.append(pdf_table(slim, [18 * mm, 52 * mm, 32 * mm, 78 * mm]))
    story.append(P("CIS classification proposal (not a register entry)", "H"))
    story.append(pdf_table(data["cis"], [45 * mm, 135 * mm]))
    story.append(P("Data-flow index", "H"))
    story.append(pdf_table(data["flows"], [45 * mm, 135 * mm]))
    story.append(P("Sign-off routing", "H"))
    story.append(pdf_table(data["signoff"], [40 * mm, 70 * mm, 70 * mm]))
    story.append(P("Control Tower accounts", "H"))
    story.append(pdf_table(data["accounts"], [40 * mm, 70 * mm, 70 * mm]))
    story.append(P("Exit scenarios", "H"))
    story.append(pdf_table(data["exits"], [40 * mm, 50 * mm, 50 * mm, 40 * mm]))
    story.append(Spacer(1, 8))
    story.append(
        P(
            "Integration I-01…I-12, cloud RACI, lifecycle pins and the 137-coordinate SBOM are in the Excel workbook.",
            "B",
        )
    )

    def on_page(canvas, doc_):
        canvas.saveState()
        canvas.setFillColor(navy)
        canvas.rect(0, A4[1] - 12 * mm, A4[0], 12 * mm, fill=1, stroke=0)
        canvas.setFillColor(gold)
        canvas.rect(0, A4[1] - 13.2 * mm, A4[0], 1.2 * mm, fill=1, stroke=0)
        canvas.setFillColor(colors.white)
        canvas.setFont("Helvetica-Bold", 8)
        canvas.drawString(12 * mm, A4[1] - 8 * mm, "AU Bank NIP R0 · ARB prerequisite pack")
        canvas.setFillColor(navy)
        canvas.rect(0, 0, A4[0], 10 * mm, fill=1, stroke=0)
        canvas.setFillColor(colors.white)
        canvas.setFont("Helvetica", 6.5)
        canvas.drawString(12 * mm, 4 * mm, "AI-DRAFTED. Not ARB / T4 / Board 4 / CIS / VA-PT approval. HA-02: ADR/LLD/NFR win.")
        canvas.drawRightString(A4[0] - 12 * mm, 4 * mm, f"{doc_.page}")
        canvas.restoreState()

    doc = SimpleDocTemplate(
        str(path),
        pagesize=A4,
        leftMargin=12 * mm,
        rightMargin=12 * mm,
        topMargin=18 * mm,
        bottomMargin=14 * mm,
        title="AU Bank NIP R0 ARB Prerequisite Pack",
        author="Mahesh — Principal Insurance Platform Architect (draft)",
        subject=DISCLAIMER,
    )
    doc.build(story, onFirstPage=on_page, onLaterPages=on_page)


def write_handover(path: Path, files: dict[str, Path]) -> None:
    lines = [
        "# What to send the ARB reviewer",
        "",
        "Markdown in this repository is for the delivery team. **Do not walk into ARB with `.md` files.**",
        "",
        "| If they ask for… | Give them this file |",
        "|---|---|",
        f"| Walk-in slides / presentation | `{files['pptx'].name}` |",
        f"| Circulated pack / print / PDF | `{files['pdf'].name}` |",
        f"| Word file they can comment on | `{files['docx'].name}` |",
        f"| Matrices, RACI, SBOM, CIS table | `{files['xlsx'].name}` |",
        f"| All four in one mail | `{files['zip'].name}` |",
        "",
        DISCLAIMER,
        "",
        "Regenerate after the markdown pack changes:",
        "",
        "```bash",
        "python3 scripts/architecture/build_arb_reviewer_pack.py",
        "```",
        "",
        "HA-02: ADR / LLD / NFR still win if a generated cell disagrees.",
        "",
    ]
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    data = load()
    # sanity: readiness has header + 19 rows
    ready_rows = len(data["readiness"]) - 1
    if ready_rows != 19:
        raise SystemExit(f"expected 19 readiness rows, got {ready_rows}")
    sbom_rows = len(data["sbom"]) - 1
    if sbom_rows != 137:
        raise SystemExit(f"expected 137 SBOM coordinates, got {sbom_rows}")

    OUT.mkdir(parents=True, exist_ok=True)
    tmp = OUT / ".generated-png"
    pngs = {
        "topology": svg_png(ROOT / "docs/architecture/r0-platform-topology.svg", tmp / "topology.png", 1800),
        "payment": svg_png(ROOT / "docs/architecture/r0-platform-payment.svg", tmp / "payment.png", 1600),
        "dr": svg_png(ROOT / "docs/architecture/r0-platform-dr.svg", tmp / "dr.png", 1600),
    }

    files = {
        "pptx": OUT / f"AU-NIP-R0-ARB-Walk-In-{STAMP}.pptx",
        "xlsx": OUT / f"AU-NIP-R0-ARB-Matrices-{STAMP}.xlsx",
        "docx": OUT / f"AU-NIP-R0-ARB-Prerequisite-Pack-{STAMP}.docx",
        "pdf": OUT / f"AU-NIP-R0-ARB-Prerequisite-Pack-{STAMP}.pdf",
        "zip": OUT / f"AU-NIP-R0-ARB-Reviewer-Pack-{STAMP}.zip",
    }
    build_pptx(data, files["pptx"], pngs)
    build_xlsx(data, files["xlsx"])
    build_docx(data, files["docx"])
    build_pdf(data, files["pdf"])
    write_handover(OUT / "WHAT-TO-SEND.md", files)

    with zipfile.ZipFile(files["zip"], "w", zipfile.ZIP_DEFLATED) as zf:
        for key in ("pptx", "xlsx", "docx", "pdf"):
            zf.write(files[key], files[key].name)
        zf.write(OUT / "WHAT-TO-SEND.md", "WHAT-TO-SEND.md")

    import shutil

    if tmp.exists():
        shutil.rmtree(tmp)
    print("wrote:")
    for p in files.values():
        print(f"  {p.relative_to(ROOT)}  ({p.stat().st_size:,} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
