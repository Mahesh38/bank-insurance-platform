#!/usr/bin/env python3
"""Generate docs/journey-execution/08-SUPPORT-RUNBOOK.md from the error registry (ERR-007).

The runbook is generated so it cannot drift from the code that produces the errors it explains.
A hand-maintained support page describes last quarter's behaviour; this one describes the build.

  Regenerate:  python3 scripts/support/build-error-runbook.py
  Verified by: ErrorRunbookParityTest (libs/bank-common-error)
"""
import re
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
CATALOGUE = ROOT / "libs/bank-common-error/src/main/java/com/bank/common/error/ErrorCatalogue.java"
OUT = ROOT / "docs/journey-execution/08-SUPPORT-RUNBOOK.md"

PUT = re.compile(
    r'put\(m,\s*ErrorCodes\.(\w+),\s*ErrorCategory\.(\w+),\s*(\d+),\s*'
    r'(?:Retryability\.(\w+),\s*)?"((?:[^"\\]|\\.)*)",\s*"((?:[^"\\]|\\.)*)",\s*'
    r'(?:AuditDisposition\.(\w+),\s*Propagation\.(\w+),\s*)?"([^"]*)"\)'
)

# Default guidance per category. Deliberately concrete: "investigate" is not an L1 action.
BY_CATEGORY = {
    "VALIDATION": (
        "The request was rejected before any business rule ran. The caller sent something the API "
        "does not accept.",
        "No. This is the API refusing malformed input, which is what it is for.",
        "Read `errors[]` in the response — it names the field. Ask the RM to correct that field "
        "and retry.",
        "Only if `errors[]` names a field the RM cannot see or cannot change on their screen. That "
        "is a UI defect, not a data-entry problem.",
        "Never advise retrying the identical request. It will fail identically.",
    ),
    "AUTHENTICATION": (
        "The caller's identity was not established, or their session is no longer valid.",
        "No, in almost every case. It is the session lifecycle working.",
        "Ask the RM to sign in again. Draft input held locally is preserved.",
        "Escalate if sign-in fails repeatedly for one user while others succeed, or if the same "
        "user is signed out repeatedly within one shift.",
        "Never ask the RM for their password or OTP, and never read one back to them.",
    ),
    "AUTHORIZATION": (
        "The caller is known, and is not permitted to do this.",
        "No. Default-deny is the design.",
        "Confirm the RM's role and branch with their supervisor. Access changes go through the "
        "normal entitlement request, never through support.",
        "Escalate if the RM should have access by their role and does not — that is an entitlement "
        "or policy defect.",
        "Never work around it, and never ask an engineer to grant access directly.",
    ),
    "NOT_FOUND": (
        "The referenced record does not exist, or is not visible to this caller.",
        "Usually not. Most often the id is stale or belongs to another branch.",
        "Confirm the id the RM used and that the record belongs to their book. Ask them to "
        "re-open the record from the list rather than a saved link.",
        "Escalate if the record demonstrably exists and is in scope for that RM.",
        "Never infer that missing means deleted. A visibility rule and an absent row look "
        "identical from here, deliberately.",
    ),
    "CONFLICT": (
        "The request disagrees with the current state of the record.",
        "No. It is the platform refusing to apply a change that no longer makes sense.",
        "Ask the RM to refresh the screen and read the current state before retrying.",
        "Escalate if the state shown to the RM and the state in the response keep disagreeing "
        "after a refresh.",
        "Never retry blindly, and never advise a second attempt on anything touching money.",
    ),
    "COMPLIANCE_GATE": (
        "A regulator-mandated gate refused this. The refusal is itself the evidence a regulator "
        "can ask us to produce.",
        "No. This is a hard control working correctly.",
        "Tell the RM which step is missing and let them complete it. The response names the gate.",
        "Escalate only if the RM has demonstrably completed the missing step and the gate still "
        "refuses.",
        "**Never override, never bypass, and never ask an engineer to.** A compliance gate cleared "
        "by support is an audit finding.",
    ),
    "UPSTREAM": (
        "A system we depend on failed or did not answer. Not the caller's mistake.",
        "Not ours, usually — but it is our incident.",
        "Check whether the failure is widespread or affects one RM. Widespread means an incident, "
        "not a support ticket.",
        "Escalate on a sustained rate. `originService` in the logs names which dependency.",
        "Never advise repeated manual retries on a submit or a payment — that is how duplicates "
        "are created.",
    ),
    "CONFIG": (
        "The platform could not resolve its own configuration, and refused rather than guess.",
        "Yes. A platform defect, not a caller problem.",
        "Capture the incident id and escalate. There is no L1 remedy.",
        "Escalate immediately — this is fail-closed behaviour and it blocks the journey.",
        "Never advise a workaround. There is no safe one.",
    ),
    "RATE_LIMIT": (
        "The caller sent more requests than the route allows.",
        "No.",
        "Ask the RM to wait and retry once. Repeated rapid retries extend the block.",
        "Escalate if a normal working pattern triggers it — that is a limit set too low.",
        "Never advise retrying in a tight loop.",
    ),
    "INTERNAL": (
        "A defect in our own code. Not the caller's fault and not a dependency's.",
        "Yes, always. Every occurrence is a bug.",
        "Capture the incident id and escalate. There is no L1 remedy.",
        "Escalate immediately with the incident id.",
        "Never tell the caller it was their input.",
    ),
}

# Curated guidance where the category default is not good enough — the R0 journey's hard edges.
OVERRIDES = {
    "SUITABILITY_REQUIRED": (
        "A quote was refused because no valid, unexpired suitability assessment exists for this "
        "customer.",
        "No. This is compliance gate C1 working correctly.",
        "Ask the RM to complete or refresh the suitability assessment, then request the quote "
        "again.",
        "Escalate only if a current assessment exists and the quote is still refused.",
        "**Never override.** A quote produced without a valid suitability assessment is a "
        "regulatory breach, and the refusal is itself the evidence.",
    ),
    "CONSENT_REQUIRED": (
        "The proposal cannot proceed because the customer has not given consent on their own "
        "device.",
        "No. This is compliance gate C2.",
        "Ask the RM to re-run the consent step. The OTP goes to the customer's device, never the "
        "RM's.",
        "Escalate if the customer confirms they completed consent and the gate still refuses.",
        "**Never capture consent on the RM's device, and never accept the RM's word for it.**",
    ),
    "PAYMENT_DEVICE_ISOLATION": (
        "Something tried to open a payment on an RM or bank-employee device.",
        "Not a platform defect — but it may be a UI defect, because that control should not have "
        "been offered.",
        "Explain that the payment link goes to the customer's own device by design. There is no "
        "RM-side payment path.",
        "Escalate as a UI defect if the RM was shown a control that led here.",
        "**Never find a way to pay on the RM's device.** There is no approved path and creating "
        "one is a control failure.",
    ),
    "PAYMENT_NOT_RECONCILED": (
        "The policy cannot be issued because the payment has not been reconciled yet.",
        "No. This is the platform refusing to issue against unconfirmed money.",
        "Tell the RM the payment is still being confirmed and the policy will follow. Do not "
        "promise a time.",
        "Escalate if reconciliation has not completed well past its normal window — that is an "
        "operations task, not a support one.",
        "**Never override to issue the policy.** A policy issued against an unreconciled payment "
        "is a financial-control failure.",
    ),
    "PAYMENT_STATE_UNCERTAIN": (
        "The payment gateway never confirmed the outcome. The money state is genuinely unknown.",
        "No — and this is the most important row in this document.",
        "Tell the RM to wait. Do not start a new payment. Reconciliation resolves it.",
        "Escalate to operations if it has not resolved within the reconciliation window.",
        "**Never start a second payment attempt, and never guess.** Guessing here is how a "
        "customer is charged twice.",
    ),
    "PAYMENT_ALREADY_IN_PROGRESS": (
        "A payment attempt for this proposal is already under way.",
        "No. It is the double-charge guard.",
        "Show the RM the existing attempt. Ask them to wait for it to complete.",
        "Escalate if the existing attempt has been in progress far longer than normal.",
        "**Never start a second attempt to 'unstick' the first.**",
    ),
    "PREMIUM_MISMATCH": (
        "The premium the platform expected and the premium the insurer returned do not agree.",
        "Yes — treat it as a pricing-integrity incident, not a transient error.",
        "Stop. Do not advise a retry. Capture the incident id and escalate immediately.",
        "Escalate immediately, every occurrence.",
        "**Never retry and never proceed on either figure.** A mispriced policy is a customer "
        "and regulatory problem at once.",
    ),
    "AUTHENTICATION_FAILED": (
        "Sign-in did not succeed. The response deliberately does not say why.",
        "No.",
        "Ask the RM to try again, and confirm their employment status and branch assignment are "
        "current with their supervisor.",
        "Escalate if one user consistently fails while colleagues in the same branch succeed. The "
        "logs separate the four distinct causes; the response never does.",
        "**Never tell a caller which part failed.** Confirming whether a user exists is a "
        "user-enumeration oracle.",
    ),
    "SP_CERTIFICATION_REQUIRED": (
        "The RM's certification for this product line is missing or expired.",
        "No.",
        "Tell the RM which certification and line of business, and point them at renewal. "
        "Non-selling work stays available to them.",
        "Escalate if the certification is current in the source system but still refused.",
        "**Never let an uncertified RM sell.** Reassign to a certified RM instead.",
    ),
    "AUTHORIZATION_UNAVAILABLE": (
        "The authorization service did not answer, so the platform denied by default.",
        "Yes, in effect — this is fail-closed behaviour and a sustained rate is an incident.",
        "Ask the RM to retry once. If it persists across users, it is an incident.",
        "Escalate on any sustained rate. This is not normal background noise.",
        "Never treat it as a permissions problem for one user. It is a dependency failure.",
    ),
    "IDEMPOTENCY_CONFLICT": (
        "The same idempotency key arrived with different request details.",
        "Usually a client defect, not an RM error.",
        "Ask the RM to start the action again from a fresh screen rather than resubmitting.",
        "Escalate with the incident id — a repeating pattern is a client bug.",
        "Never advise re-sending the same request with edited fields.",
    ),
    "QUOTE_EXPIRED": (
        "The quote is past its validity window, or the quote job it referenced is gone.",
        "No.",
        "Ask the RM to produce a fresh quote. Do not re-use the old offer.",
        "Escalate if quotes expire far sooner than the configured window.",
        "**Never re-quote automatically.** The suitability gate must be re-checked, so the RM "
        "starts the quote again deliberately.",
    ),
    "ATTRIBUTION_NOT_CALLER_SUPPLIED": (
        "The request tried to supply its own attribution, which the platform never accepts from a "
        "caller.",
        "It is a client defect — and it should never be seen in production.",
        "Capture the incident id and escalate. There is no RM action.",
        "Escalate immediately. Treat any occurrence as a client defect or an intrusion attempt.",
        "Never ask the RM to change anything. They cannot cause or fix this.",
    ),
    "SERVICE_IDENTITY_REJECTED": (
        "An internal endpoint was called by something that is not a permitted service.",
        "It should never reach a human at all.",
        "Capture the incident id and escalate to security. There is no RM action.",
        "Escalate to security immediately.",
        "Never treat a leaked internal URL as an entry point.",
    ),
}


def parse():
    text = CATALOGUE.read_text(encoding="utf-8")
    body = text[text.index("private static Map<String, ErrorDefinition> buildRegistry()"):]
    out = []
    for m in PUT.finditer(body):
        code, category, status, retry, title, detail, audit, prop, ref = m.groups()
        out.append({
            "code": code, "category": category, "status": int(status),
            "retryability": retry or "", "title": title, "detail": detail,
            "audit": audit or "", "propagation": prop or "", "ref": ref,
        })
    return out


def render(entries):
    lines = [
        "# 08 — Support Runbook (`RB-*`)",
        "",
        "**Every error the platform can return, and what L1 or L2 support does about it.**",
        "",
        "> **GENERATED — do not hand-edit.** Produced from the error registry by",
        "> `python3 scripts/support/build-error-runbook.py`, and checked against it by",
        "> `ErrorRunbookParityTest`. Edit the guidance in the generator, not here: a support page",
        "> maintained separately from the code describes last quarter's behaviour.",
        "",
        "Status: `AI-DRAFTED` · Owner: Amit (Board 2) + Shivanshi (Board 7) · Origin: `ERR-007`",
        "",
        "---",
        "",
        "## 1. How to use this",
        "",
        "Every error response carries an `incidentId` and a `code`. The RM can read both off their",
        "screen.",
        "",
        "1. Take the `incidentId` and search the log platform for it. That returns **every line of",
        "   that failure, across every service** — the `code`, the service that emitted it, the",
        "   `originService` that actually failed, and the engineer-facing `reason`.",
        "2. Look the `code` up below. The five rows tell you what it means, whether it is a defect,",
        "   what to do, when to escalate, and what never to do.",
        "",
        "The caller is deliberately not shown the `reason`. That is not information being withheld",
        "from support — it is information being withheld from a device, and the `incidentId` is how",
        "you retrieve it.",
        "",
        "> **The `Never` row is the one that matters.** Most rows describe a control working",
        "> correctly. Clearing a control from a support seat is an audit finding, however reasonable",
        "> it looks at the time.",
        "",
        "---",
        "",
        "## 2. Index by category",
        "",
    ]

    by_cat = {}
    for e in entries:
        by_cat.setdefault(e["category"], []).append(e)

    lines.append("| Category | What it means for support | Codes |")
    lines.append("|---|---|---|")
    summary = {
        "VALIDATION": "The caller sent something invalid",
        "AUTHENTICATION": "Identity not established",
        "AUTHORIZATION": "Known caller, not permitted",
        "NOT_FOUND": "No such record for this caller",
        "CONFLICT": "State disagrees",
        "COMPLIANCE_GATE": "A regulator-mandated refusal — never override",
        "UPSTREAM": "A dependency failed",
        "CONFIG": "Platform could not configure itself — fail closed",
        "RATE_LIMIT": "Throttled",
        "INTERNAL": "Our defect",
    }
    for cat in sorted(by_cat):
        codes = " · ".join(f"[`{e['code']}`](#rb-{e['code'].lower()})" for e in by_cat[cat])
        lines.append(f"| **{cat}** | {summary.get(cat, '')} | {codes} |")

    lines += ["", "---", "", "## 3. Pages", ""]

    for e in sorted(entries, key=lambda x: (x["category"], x["code"])):
        guidance = OVERRIDES.get(e["code"]) or BY_CATEGORY[e["category"]]
        means, defect, l1, l2, never = guidance
        lines += [
            f"### RB-{e['code']}",
            "",
            f"`{e['code']}` · **HTTP {e['status']}** · {e['category']}"
            + (f" · retry: {e['retryability']}" if e["retryability"] else "")
            + (f" · {e['audit']}" if e["audit"] and e["audit"] != "NONE" else "")
            + f" · source: {e['ref']}",
            "",
            f"> The caller sees: **{e['title']}** — {e['detail']}",
            "",
            "| | |",
            "|---|---|",
            f"| **What it means** | {means} |",
            f"| **Is it a defect?** | {defect} |",
            f"| **L1 action** | {l1} |",
            f"| **L2 escalation** | {l2} |",
            f"| **Never** | {never} |",
            "",
        ]

    lines += [
        "---",
        "",
        "## 4. Codes with no page",
        "",
        "There are none, and that is enforced: `ErrorRunbookParityTest` fails the build if a code is",
        "registered without a page here, or a page exists for a code that no longer does. A support",
        "runbook with gaps is worse than none, because the gap is only discovered mid-incident.",
        "",
        "The degraded states in",
        "[`04 §7`](./04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md#7-degraded-states--the-ones-with-no-error-code)",
        "deliberately have no error code and therefore no page here. They are journey states, not",
        "refusals, and they are resolved by the operations procedures that file names.",
        "",
    ]
    return "\n".join(lines) + "\n"


def main():
    entries = parse()
    if len(entries) < 50:
        print(f"parsed only {len(entries)} codes — the registry regex is probably stale", file=sys.stderr)
        return 1
    OUT.write_text(render(entries), encoding="utf-8")
    print(f"wrote {OUT.relative_to(ROOT)} — {len(entries)} runbook pages")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
