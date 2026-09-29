"""Decision briefs for the daily AIGEM sign-off report.

For every item waiting on a human, draft what a reviewer needs in order to decide: the context,
an AIGEM suggestion, the justification, reasons to approve, reasons to reject or hold, the
conditions or validation that should come with a yes, the evidence, and where the binding
signature is recorded.

Every suggestion comes from a stated rule applied to recorded evidence. The rules are in
this file, and each brief names its sources. Nothing here is a verdict. A brief is a drafted
position for the named human to adopt, amend or reject (09-AI_EXECUTION_RULES.md,
PERSONA-AUTHORITY-MATRIX.md). The human's tick on the pull request records their decision; the
signature itself is filed by that human in the file each brief names. An agent never files it.

Used by daily-governance-report.py. Tests: test_daily_governance_report.py.
"""

from __future__ import annotations

import datetime as dt
import re
import subprocess
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CR_DIR = ROOT / "docs/governance/change-requests"

# Links are written relative to docs/governance/autopilot/DAILY-SIGNOFF.md.
GOV = "../"

APPROVAL_EXPIRY_DAYS = 30   # RG-8 (CR-009 A4): approvals expire at 30 days or on changed context
ESCALATE_AFTER_DAYS = 7     # past this, a re-date alone is a second hope (DEP-3); R12 forces it
IN_USE_REFERENCES = 5       # an unratified CR cited this widely is already how the repo operates
MAX_CONDITIONS = 12         # per brief; the rest stay one click away in the verdict pack

# Authority named in required_approvers -> the persona who holds it (AUTHORITY-QUICK-CARD.md).
AUTHORITY_PERSONA = {
    "Architect": "Mahesh", "Engineering": "Amit", "QA": "Swapnali", "Security": "Deepali",
    "Operations": "Shivanshi", "Product": "Rajal", "Compliance": "Shailja", "Delivery": "Kalpana",
}
POSITION = re.compile(r"\b(APPROVE-WITH-MODIFICATION|APPROVE-WITH-CONDITIONS|APPROVE|REJECT|DEFER|ABSTAIN)\b")

OPTIONS_DECIDE = ["Approve", "Approve with conditions: …", "Approve after validation: …",
                  "Reject — reason: …", "Defer to (date): …"]
OPTIONS_COUNTERSIGN = ["Counter-sign", "Decline, and raise a CR to reverse it — reason: …"]
OPTIONS_RATIFY = ["Ratify", "Ratify with conditions: …", "Reject and revert — reason: …",
                  "Defer to (date): …"]
OPTIONS_CHASE = ["Re-date to (date), confirmed with the owner: …", "Escalate to: …",
                 "Record the refusal and re-plan", "Resolved — evidence: …"]
OPTIONS_STATE = ["Re-confirm unchanged", "Re-confirm with these changes: …",
                 "Hold the Governance Sync on (date): …"]


@dataclass
class Brief:
    group: str
    ref: str
    title: str
    owner: str
    suggestion: str
    context: str
    justification: str
    pros: list[str] = field(default_factory=list)
    cons: list[str] = field(default_factory=list)
    conditions: list[str] = field(default_factory=list)
    evidence: list[tuple[str, str]] = field(default_factory=list)
    record_in: str = ""
    options: list[str] = field(default_factory=list)


# --------------------------------------------------------------------------- helpers

def _short(text: str, limit: int = 220) -> str:
    text = re.sub(r"\s+", " ", re.sub(r"[*`]", "", text)).strip()
    text = re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", text)
    return text if len(text) <= limit else text[: limit - 1].rstrip() + "…"


def _date(value) -> dt.date | None:
    match = re.search(r"\d{4}-\d{2}-\d{2}", str(value or ""))
    return dt.date.fromisoformat(match.group()) if match else None


def _link(path: Path) -> str:
    """A change-request path as a link from the report's directory."""
    return GOV + "change-requests/" + path.relative_to(CR_DIR).as_posix()


def references(term: str) -> int:
    """How many tracked documents cite `term` — evidence that a change is already in use."""
    try:
        out = subprocess.run(["git", "grep", "-l", "-w", term, "--", "docs", "AGENTS.md"],
                             cwd=ROOT, capture_output=True, text=True, timeout=60).stdout
    except (OSError, subprocess.TimeoutExpired):
        return 0
    return len([line for line in out.splitlines() if line.strip()])


def section(text: str, heading: re.Pattern) -> str:
    """Body of the first `## ` section whose heading matches."""
    lines, grab = [], False
    for line in text.splitlines():
        if line.startswith("## "):
            if grab:
                break
            grab = bool(heading.search(line))
            continue
        if grab:
            lines.append(line)
    return "\n".join(lines)


def first_sentence(text: str, limit: int = 200) -> str:
    text = _short(text, 2000)
    match = re.match(r"(.+?[.;:])(\s|$)", text)
    return _short(match.group(1) if match else text, limit)


def conditions_in(text: str) -> list[str]:
    """Numbered or tabled conditions from a verdict or CR file's Conditions section.

    A numbered item wraps onto indented continuation lines, so lines are joined before the
    headline sentence is taken.
    """
    body = section(text, re.compile(r"condition", re.I))
    found, current = [], None
    for line in body.splitlines():
        numbered = re.match(r"^\d+\.\s+(.*)", line)
        tabled = re.match(r"^\|\s*\*{0,2}([A-Z]+-C\d+)\*{0,2}\s*\|\s*([^|]+)\|", line)
        if numbered:
            if current is not None:
                found.append(first_sentence(current))
            current = numbered.group(1)
        elif current is not None and line.startswith((" ", "\t")) and line.strip():
            current += " " + line.strip()
        else:
            if current is not None:
                found.append(first_sentence(current))
                current = None
            if tabled:
                found.append(f"{tabled.group(1)}: {first_sentence(tabled.group(2), 190)}")
    if current is not None:
        found.append(first_sentence(current))
    return found


AREA = {"dba": "Database", "sre": "SRE", "qa": "QA", "r12": "Delivery"}
# Seat words as they appear in approver cells -> the persona who holds the seat.
SEAT_PERSONA = [
    ("QA Lead", "Swapnali"), ("Product Owner", "Rajal"), ("Architecture", "Mahesh"),
    ("Architect", "Mahesh"), ("Product", "Rajal"), ("PO", "Rajal"), ("QA", "Swapnali"),
    ("Security", "Deepali"), ("Compliance", "Shailja"), ("Operations", "Shivanshi"),
    ("SRE", "Shivanshi"), ("Database", "Aarti"), ("Delivery", "Kalpana"), ("Engineering", "Amit"),
]
PEOPLE = ["Rajal", "Mahesh", "Amit", "Deepali", "Aarti", "Swapnali", "Shailja", "Shivanshi", "Kalpana"]
OWED = re.compile(r"outstanding|pending|required|owed", re.I)


def seat_name(stem: str) -> str:
    """board-4-security-deepali -> Deepali (Security); dba-aarti -> Aarti (Database)."""
    parts = [p for p in stem.split("-") if p and not p.isdigit() and p not in {"board"}]
    person = parts[-1].title()
    area = [AREA.get(p, p.title()) for p in parts[:-1] if not re.fullmatch(r"r\d+", p)]
    return f"{person} ({' '.join(area)})" if area else person


def who(owed: str) -> str:
    """Who still owes a signature, as 'Person (Seat)'.

    Approver cells also name people who already acted ("Mahesh approved preparation … Product
    Owner pending"). Only the clauses that say something is still owed are read.
    """
    clauses = [c for c in re.split(r"\s[—–]\s|;|\.\s", owed) if OWED.search(c)] or [owed]
    names: dict[str, str] = {}
    for clause in clauses:
        rest = clause
        for seat, person in SEAT_PERSONA:
            if re.search(rf"\b{re.escape(seat)}\b", rest):
                names.setdefault(person, seat)
                rest = re.sub(rf"\b{re.escape(seat)}\b", " ", rest)
        for person in PEOPLE:
            if re.search(rf"\b{person}\b", clause):
                names.setdefault(person, "")
    if not names:
        return _short(owed, 60)
    return ", ".join(f"{p} ({s})" if s else p for p, s in names.items())


def verdict_pack(cr_id: str) -> list[dict]:
    """Drafted board positions and conditions for a CR, one entry per verdict file."""
    pack = CR_DIR / cr_id / "verdicts"
    if not pack.is_dir():
        return []
    out = []
    for path in sorted(pack.glob("*.md")):
        if path.name == "README.md":
            continue
        text = path.read_text(encoding="utf-8")
        match = POSITION.search(text)
        out.append({
            "seat": seat_name(path.stem),
            "position": match.group(1) if match else "NO POSITION",
            "conditions": conditions_in(text),
            "path": path,
        })
    return out


def cr_file(cr_id: str) -> Path | None:
    hits = sorted(CR_DIR.glob(f"{cr_id}-*.md"))
    return hits[0] if hits else None


def next_gate_blockers(next_stage: str, dependency_text: str) -> list[str]:
    """Open dependency edges that block entry to the stage a gate approval would open."""
    code = re.match(r"\s*(S\d{2})\b", next_stage or "")
    if not code:
        return []
    gate = f"GATE-{code.group(1)}"
    rows = []
    for line in dependency_text.splitlines():
        if line.startswith("| DEP") and gate in line and "| OPEN |" in line:
            rows.append(_short(line.split("|")[1], 40))
    return rows


# --------------------------------------------------------------------------- rules

def gate_brief(signoff: dict, today: dt.date, dependency_text: str) -> Brief:
    """A gate at CANDIDATE: suggestion depends on the criteria, evidence and its age.

    APPROVE                   every criterion MET or WAIVED with evidence, none older than 30 days
    APPROVE WITH CONDITIONS   as above, but evidence is below E4 or ages out of RG-8 soon
    APPROVE AFTER VALIDATION  a criterion lacks evidence, or verification is past 30 days (RG-8)
    DO NOT APPROVE YET        a criterion is not MET or WAIVED (the CANDIDATE is wrong)
    """
    stream = signoff["stream"]
    criteria = stream.get("criteria", [])
    not_met = [c["id"] for c in criteria if c.get("state") not in {"MET", "WAIVED"}]
    no_evidence = [c["id"] for c in criteria if not c.get("evidence")]
    waived = [c["id"] for c in criteria if c.get("state") == "WAIVED"]
    verified = [d for d in (_date(c.get("last_verified_at")) for c in criteria) if d]
    oldest = min(verified) if verified else None
    age = (today - oldest).days if oldest else None
    expires = oldest + dt.timedelta(days=APPROVAL_EXPIRY_DAYS) if oldest else None
    weak = [f"{c['id']} ({c.get('required_evidence_level')}, {c.get('verifier')})" for c in criteria
            if c.get("required_evidence_level", "E4") < "E4" or c.get("verifier") == "human-review"]
    by_ci = sum(1 for c in criteria if c.get("verifier") == "ci")
    blocked_next = next_gate_blockers(signoff["to"], dependency_text)

    conditions = []
    if expires:
        conditions.append(f"Sign by **{expires}** (30 days after the oldest verification, RG-8), "
                          f"or re-run the verifiers first.")
    if weak:
        conditions.append("Read the evidence lines yourself for the criteria verified by human review "
                          "or below E4: " + ", ".join(weak) + ".")
    if waived:
        conditions.append("Confirm each waiver still has an owner and an unexpired date: " + ", ".join(waived) + ".")

    if not_met:
        suggestion = "DO NOT APPROVE YET"
    elif no_evidence or (age is not None and age > APPROVAL_EXPIRY_DAYS):
        suggestion = "APPROVE AFTER VALIDATION"
    elif conditions:
        suggestion = "APPROVE WITH CONDITIONS"
    else:
        suggestion = "APPROVE"

    pros = [f"{len(criteria) - len(not_met)} of {len(criteria)} exit criteria are MET or WAIVED, "
            f"{by_ci} of them verified by CI."]
    if not no_evidence:
        pros.append("Every criterion carries recorded evidence.")
    if not waived:
        pros.append("No criterion is waived; nothing is being accepted on a promise.")
    cons = []
    if not_met:
        cons.append("Not met: " + ", ".join(not_met) + ".")
    if no_evidence:
        cons.append("No evidence recorded for: " + ", ".join(no_evidence) + ".")
    if age is not None:
        cons.append(f"The oldest verification is {age} days old ({oldest}).")
    if blocked_next:
        cons.append(f"Approving opens {signoff['to']}, but its entry is blocked by "
                    + ", ".join(blocked_next) + ". The approval is still valid; the next stage "
                    "cannot start until that clears.")

    per_authority = []
    for authority in signoff["missing"]:
        persona = AUTHORITY_PERSONA.get(authority, authority)
        owned = [c["id"] for c in criteria if persona.lower() in str(c.get("owner", "")).lower()]
        per_authority.append(f"**{authority} ({persona})**: "
                             + (f"check {', '.join(owned)}" if owned else "no criterion owned; review the whole gate"))

    return Brief(
        group="gate", ref=signoff["gate"],
        title=f"{signoff['workstream']} stage gate: {signoff['from']} → {signoff['to']}",
        owner=", ".join(signoff["missing"]),
        suggestion=suggestion,
        context=(f"`{signoff['gate']}` is at CANDIDATE. Human verdicts are owed by "
                 f"{', '.join(signoff['missing'])}. What each authority should check: "
                 + "; ".join(per_authority) + "."),
        justification=(f"Rule: every criterion MET with evidence gives APPROVE. Evidence below E4, or "
                       f"approaching the {APPROVAL_EXPIRY_DAYS}-day expiry, adds conditions. Missing or "
                       f"expired evidence means validate first. Result here: {suggestion}."),
        pros=pros, cons=cons, conditions=conditions,
        evidence=[("GATE-EVIDENCE.yaml", GOV + "state/GATE-EVIDENCE.yaml"),
                  ("04-STAGE_GATES.md", GOV + "04-STAGE_GATES.md")],
        record_in=("each approver's entry under `approvals` in `GATE-EVIDENCE.yaml` "
                   "(`reviewer_type: HUMAN`), then a row in DECISION-REGISTER §4"),
        options=OPTIONS_DECIDE,
    )


def cr_brief(cr: dict, today: dt.date) -> Brief:
    """A change request awaiting ratification or a signature.

    COUNTER-SIGN             already APPROVED; only a counter-signature is owed
    REJECT OR REWORK         a drafted board position is REJECT
    APPROVE WITH CONDITIONS  drafted positions exist, none reject, conditions attached
    RATIFY OR REVERT         already transcribed or widely cited, yet unratified
    APPROVE AFTER VALIDATION no drafted positions and not yet in use: ask the boards first
    """
    path = cr_file(cr["id"])
    pack = verdict_pack(cr["id"])
    raised = _date(cr.get("date"))
    age = (today - raised).days if raised else None
    decision = cr["decision"]
    in_use = references(cr["id"])
    evidence = [(path.name, _link(path))] if path else []
    evidence.append(("DECISION-REGISTER §3", GOV + "registers/DECISION-REGISTER.md"))
    own_conditions = conditions_in(path.read_text(encoding="utf-8")) if path else []
    stale = (f"RG-8: this context is {age} days old. Before signing, confirm nothing it depends on "
             f"has changed since {raised}.") if age is not None and age > APPROVAL_EXPIRY_DAYS else ""

    pros, cons, conditions = [], [], list(own_conditions[:MAX_CONDITIONS])
    if decision.upper().startswith("APPROVED"):
        suggestion = "COUNTER-SIGN"
        options = OPTIONS_COUNTERSIGN
        justification = ("The decision is already APPROVED and in force; only the counter-signature is "
                         "missing. An approval missing a required counter-signature leaves the record "
                         "ambiguous for every later gate that cites it.")
        pros.append(f"In force since {raised} ({age} days) with no recorded objection.")
        cons.append("Withhold only if you disagree with the substance. In that case raise a CR to reverse "
                    "it; declining to sign does not undo a decision already in force.")
    elif pack:
        tally: dict[str, int] = {}
        for draft in pack:
            tally[draft["position"]] = tally.get(draft["position"], 0) + 1
        seats = "; ".join(f"{d['seat']}: {d['position']}" for d in pack)
        drafted = [f"**{d['seat']}**: {c}" for d in pack for c in d["conditions"]]
        per_seat = max(2, MAX_CONDITIONS // len(pack))  # every seat gets heard, not just the first file
        shown = [f"**{d['seat']}**: {c}" for d in pack for c in d["conditions"][:per_seat]]
        conditions = shown + conditions
        extra = len(drafted) - len(shown)
        if extra > 0:
            conditions.append(f"…and {extra} more drafted conditions in the verdict pack.")
        if tally.get("REJECT"):
            suggestion = "REJECT OR REWORK"
            justification = f"At least one drafted board position is REJECT ({seats})."
        else:
            suggestion = "APPROVE WITH CONDITIONS" if drafted or any("WITH" in p for p in tally) else "APPROVE"
            justification = (f"{len(pack)} drafted board positions, none REJECT ({seats}). Every draft "
                             f"attaches conditions, so the suggestion carries them.")
        pros.append(f"Independent drafted review exists for {len(pack)} seats, and none rejects.")
        cons.append("Each draft is AI-authored, simulating the seat. It is a position to adopt, amend "
                    "or reject, not that person's verdict.")
        if drafted:
            cons.append(f"{len(drafted)} conditions in total. A yes means owning them with dates.")
        evidence.append(("verdict pack", _link(CR_DIR / cr["id"] / "verdicts" / "README.md")))
        options = OPTIONS_DECIDE
    elif re.search(r"transcribed|bypass", decision, re.I) or in_use >= IN_USE_REFERENCES:
        suggestion = "RATIFY WITH CONDITIONS"
        options = OPTIONS_RATIFY
        how = "transcribed into artefacts" if re.search("transcribed|bypass", decision, re.I) else "in use"
        justification = (f"The change is already how the repository operates ({how}, cited by {in_use} "
                         f"documents) and nothing on record rejects it. Leaving it unratified is the "
                         f"worst outcome: agents follow it while every gate that cites it rests on an "
                         f"unsigned change. Ratify it, with each owed authority confirming its own part.")
        pros.append(f"In use for {age} days, cited by {in_use} documents, with no recorded REJECT.")
        pros.append("Reverting later costs more the longer it stays in use.")
        cons.append("No drafted board verdicts exist, so the signer has no independent assessment on file.")
        conditions.insert(0, f"Each owed authority ({who(cr['owed'])}) confirms the part in its own "
                             "domain before the ratification is recorded.")
        conditions.insert(1, "If any of them objects, reject and revert through a CR. Do not leave the "
                             "change half-ratified.")
    else:
        suggestion = "APPROVE AFTER VALIDATION"
        options = OPTIONS_DECIDE
        justification = ("There are no drafted board positions and the change is not yet in use. Ask the "
                         "owed boards for their drafts before signing.")
        cons.append("No independent assessment on file yet.")
    if stale:
        conditions.append(stale)

    return Brief(
        group="cr", ref=cr["id"], title=cr["summary"], owner=who(cr["owed"]),
        suggestion=suggestion,
        context=f"Raised {raised} ({age} days ago). Current status: {decision}. Owed: {cr['owed']}",
        justification=justification, pros=pros, cons=cons, conditions=conditions,
        evidence=evidence,
        record_in=("the CR file's Status line and its row in DECISION-REGISTER §3, by the signer" if path
                   else "its row in DECISION-REGISTER §3 (there is no separate CR file), by the signer"),
        options=options,
    )


def dependency_brief(dep: dict) -> Brief:
    """An external dependency past its date: re-date once, escalate after a week (DEP-3)."""
    escalate = dep["days"] > ESCALATE_AFTER_DAYS
    suggestion = "ESCALATE" if escalate else "RE-DATE"
    return Brief(
        group="dependency", ref=dep["id"], title=dep["what"], owner=dep["owner"],
        suggestion=suggestion,
        context=f"Required by {dep['due']}, now **{dep['days']} days** overdue. Chase owner: {dep['owner']}.",
        justification=(f"Rule DEP-3: a past date is a hope, not a tracked dependency. Up to "
                       f"{ESCALATE_AFTER_DAYS} days overdue, re-date it with the owner. Past that, a "
                       f"second date without escalation repeats the miss, so R12 forces a decision "
                       f"(CR-009). Result here: {suggestion}."),
        pros=[f"Impact if late: {dep['impact']}"] if dep.get("impact") else [],
        cons=(["Escalation spends goodwill with an external party; do it with the owner, not around them."]
              if escalate else ["A re-date without a named contact on the other side is still a hope."]),
        conditions=["Whatever you choose, write the new date and a named counterpart into the row."],
        evidence=[("DEPENDENCY-REGISTER §2", GOV + "registers/DEPENDENCY-REGISTER.md")],
        record_in="the row in DEPENDENCY-REGISTER §1 and §2, by the chase owner",
        options=OPTIONS_CHASE,
    )


def blocker_brief(blk: dict) -> Brief:
    """A gate-criterion blocker past its follow-up date; weighted by what the criterion unblocks."""
    escalate = blk["days"] > ESCALATE_AFTER_DAYS and blk.get("priority") in {"P1", "P2"}
    suggestion = "ESCALATE" if escalate else "RE-DATE"
    return Brief(
        group="blocker", ref=f"{blk['gate']} {blk['criterion']} · {blk['blocker']}", title=blk.get("description") or blk["blocker"],
        owner=blk["owner"], suggestion=suggestion,
        context=(f"Blocker `{blk['blocker']}` ({blk.get('type') or 'unspecified'}) on criterion "
                 f"{blk['criterion']}. Follow-up was {blk['follow_up']}, now **{blk['days']} days** late."),
        justification=(f"The criterion is {blk.get('priority') or 'unprioritised'} and enables "
                       f"{blk.get('enables', 0)} others. A {blk.get('priority')} blocker more than "
                       f"{ESCALATE_AFTER_DAYS} days late is escalated rather than re-dated. "
                       f"Result here: {suggestion}."),
        pros=[f"Clearing it unblocks {blk.get('enables', 0)} further criteria."],
        cons=["If the blocker is external, escalation still cannot make the answer arrive sooner. It "
              "makes the wait visible and owned."],
        conditions=["Set a new follow-up date and a named owner on the blocker entry."],
        evidence=[("GATE-EVIDENCE.yaml", GOV + "state/GATE-EVIDENCE.yaml")],
        record_in="the blocker's `follow_up` and `owner` in GATE-EVIDENCE.yaml, by the criterion owner",
        options=OPTIONS_CHASE,
    )


def state_briefs(state: dict, today: dt.date, crs: list[dict], warns: list[str]) -> list[Brief]:
    """State refresh, the PO counter-signature, and each stale artefact."""
    out = []
    as_of = _date(state["state_as_of"])
    due = _date(state["review_due"])
    age = (today - as_of).days
    if age > 7:
        newer = [f"{c['id']} ({c['date']})" for c in crs if (_date(c.get("date")) or dt.date.min) > as_of]
        suggestion = "RE-CONFIRM WITH CONDITIONS" if newer else "RE-CONFIRM"
        out.append(Brief(
            group="state", ref="CURRENT-STATE.yaml", title="Re-confirm the governing state",
            owner="Kalpana / R12 (stage fields: Architect + PO)", suggestion=suggestion,
            context=f"`state_as_of` is {as_of} ({age} days). `review_due` is {due} ({(due - today).days} days).",
            justification=("The weekly Governance Sync re-confirms the state. Change requests raised after "
                           "`state_as_of` may have moved scope, so they are named as conditions. "
                           f"Result here: {suggestion}."),
            pros=["Agents keep admitting work only while the state is fresh (Rule CS-1). "
                  "Re-confirming protects throughput."],
            cons=(["These change requests post-date the state and may have changed scope: " + ", ".join(newer)]
                  if newer else []),
            conditions=([f"Walk {', '.join(newer)} against `current_scope` before re-confirming."] if newer else [])
            + ["Stage fields (`current_phase`, `stage_status`) change only with Architect + PO; "
               "a refresh does not move them."],
            evidence=[("CURRENT-STATE.yaml", GOV + "state/CURRENT-STATE.yaml"),
                      ("RUNBOOK §3 Weekly", GOV + "RUNBOOK.md")],
            record_in="`state_as_of` (and `review_due` when re-ratifying) in CURRENT-STATE.yaml, by R12",
            options=OPTIONS_STATE,
        ))
    ratified = str(state.get("ratified_by", ""))
    if re.search(r"\b(outstanding|pending)\b", ratified, re.I):
        since = _date(ratified)
        out.append(Brief(
            group="state", ref="PO counter-signature", title="Counter-sign the state ratification",
            owner="Rajal / Product", suggestion="COUNTER-SIGN",
            context=f"Ratified by: {_short(ratified, 300)}",
            justification=("Architecture ratified the state and has re-confirmed it since. Only Product's "
                           "counter-signature is missing, and every stage, scope and objective value it "
                           "covers has been re-confirmed unchanged."),
            pros=[f"Outstanding since {since} ({(today - since).days} days)." if since else "Outstanding."],
            cons=["Sign only if the objective and scope in BOOT.md section 5 are still the product "
                  "you are accountable for. If not, amend first."],
            evidence=[("CURRENT-STATE.yaml", GOV + "state/CURRENT-STATE.yaml"),
                      ("BOOT.md section 5", "../../../docs/context/BOOT.md")],
            record_in="`ratified_by` in CURRENT-STATE.yaml, by Rajal",
            options=OPTIONS_COUNTERSIGN,
        ))
    for warn in warns:
        if "state_as_of" in warn:
            continue  # covered by the re-confirm brief above
        owner = re.search(r"owner:\s*([^)]+)", warn)
        target = warn.split()[0]
        out.append(Brief(
            group="state", ref=target, title=f"Stale artefact: {target}",
            owner=owner.group(1).strip() if owner else "document owner", suggestion="REVIEW AND TOUCH",
            context=_short(warn, 200),
            justification=("Freshness limits exist so an agent never triages against a stale register. "
                           "Review it: update what moved, or record that nothing did."),
            pros=["A reviewed-no-change commit clears the warning honestly (CR-009)."],
            cons=["Touching the file without reviewing it hides staleness instead of fixing it."],
            conditions=["If this is DEPENDENCY-REGISTER.md, the dependency briefs below are the review."],
            evidence=[(target.split("/")[-1], "../../../" + target)],
            record_in=f"a commit to `{target}` by its owner",
            options=["Reviewed, updated", "Reviewed, no change needed", "Delegate to: …"],
        ))
    return out


def build(today: dt.date, state: dict, signoffs: list[dict], crs: list[dict], deps: list[dict],
          blockers: list[dict], warns: list[str], dependency_text: str) -> list[Brief]:
    briefs = [gate_brief(s, today, dependency_text) for s in signoffs]
    briefs += [cr_brief(c, today) for c in crs]
    briefs += [dependency_brief(d) for d in deps]
    briefs += [blocker_brief(b) for b in blockers]
    briefs += state_briefs(state, today, crs, warns)
    return briefs


# --------------------------------------------------------------------------- render

GROUP_TITLES = [
    ("gate", "Stage gates"),
    ("cr", "Change requests"),
    ("dependency", "External dependencies"),
    ("blocker", "Gate-criterion blockers"),
    ("state", "State file, ratification and stale artefacts"),
]


def render(briefs: list[Brief], number: str, compact: bool = False) -> list[str]:
    """Markdown for section `number`. `compact` keeps the suggestion, reason and tick-boxes and
    points to the full briefs in the report file (used when a PR body would be too long)."""
    L = [f"## {number}. Decision briefs — an AIGEM suggestion for every item in section 2", ""]
    L.append("Each brief gives the context, a suggested decision, why, reasons both ways, and the "
             "conditions a yes should carry. **Suggestions are drafts, not signatures.** Tick your "
             "decision in the brief. Then file the binding signature where the brief says: that step "
             "is yours, and no agent or workflow performs it.")
    L.append("")
    if compact:
        L.append("> Compact view: this pull request body is near GitHub's size limit. The full briefs, "
                 "with reasons both ways, conditions and evidence, are in "
                 "`docs/governance/autopilot/DAILY-SIGNOFF.md` in this PR's Files tab.")
        L.append("")
    counts: dict[str, int] = {}
    for b in briefs:
        counts[b.suggestion] = counts.get(b.suggestion, 0) + 1
    if counts:
        L.append("| Suggestion | Items |")
        L.append("|---|---|")
        for suggestion, n in sorted(counts.items(), key=lambda kv: -kv[1]):
            L.append(f"| {suggestion} | {n} |")
        L.append("")
    for group, heading in GROUP_TITLES:
        items = [b for b in briefs if b.group == group]
        if not items:
            continue
        L.append(f"### {heading}")
        L.append("")
        for b in items:
            L.extend(render_compact(b) if compact else render_one(b))
    return L


def render_compact(b: Brief) -> list[str]:
    L = [f"<details><summary><b>{b.ref}</b> → <b>{b.suggestion}</b> · owed by: {_short(b.owner, 120)}</summary>", ""]
    L.append(_short(b.justification, 300))
    L.append("")
    L.extend(f"- [ ] {o}" for o in b.options)
    L.append("")
    L.append(f"**File the signature in:** {b.record_in}.")
    L.append("")
    L.append("</details>")
    L.append("")
    return L


def render_one(b: Brief) -> list[str]:
    L = [f"<details><summary><b>{b.ref}</b> · {_short(b.title, 90)} → AIGEM suggests "
         f"<b>{b.suggestion}</b> · owed by: {_short(b.owner, 160)}</summary>", ""]
    L.append(f"**Context.** {b.context}")
    L.append("")
    L.append(f"**AIGEM suggestion (draft): {b.suggestion}.** {b.justification}")
    L.append("")
    if b.pros:
        L.append("**Reasons to approve / act:**")
        L.extend(f"- {p}" for p in b.pros)
        L.append("")
    if b.cons:
        L.append("**Reasons to reject, hold or be careful:**")
        L.extend(f"- {c}" for c in b.cons)
        L.append("")
    if b.conditions:
        L.append("**Conditions or validation to attach to a yes:**")
        L.extend(f"{i}. {c}" for i, c in enumerate(b.conditions, 1))
        L.append("")
    if b.evidence:
        L.append("**Evidence:** " + " · ".join(f"[{label}]({href})" for label, href in b.evidence))
        L.append("")
    L.append(f"**Your decision** ({_short(b.owner, 80)}). Tick one, and add your name:")
    L.append("")
    L.extend(f"- [ ] {o}" for o in b.options)
    L.append("")
    L.append(f"**File the signature in:** {b.record_in}.")
    L.append("")
    L.append("</details>")
    L.append("")
    return L
