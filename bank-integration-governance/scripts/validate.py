#!/usr/bin/env python3
"""FLOW governance validator.

    python3 scripts/validate.py              # check the repository; exit 1 on errors
    python3 scripts/validate.py route PATH…  # resolve product-repo paths to lane/pack/card/guardrails
    python3 scripts/validate.py route --keywords "consent retention"

Errors fail the build: size budgets, broken structure, dangling ids, broken relative links.
Staleness only ever warns (context/README.md, rule CX-5) — it never blocks work.
"""
import datetime
import fnmatch
import os
import re
import sys

try:
    import yaml
except ImportError:  # pragma: no cover
    sys.exit("validate.py needs PyYAML: pip install pyyaml")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TOTAL_BUDGET = 200 * 1024
STALE_DAYS = 30
CARD_SECTIONS = ["**Question it answers:**", "## Never", "## Escalate when"]
PACK_SECTIONS = ["## Summary", "## Outcome now", "## Invariants in play", "## Open decisions",
                 "## Where truth lives", "## Gotchas"]
ENFORCEMENT = {"code", "ci", "release", "review"}
PROMOTION = {"dev", "uat", "prod", "none"}

errors, warnings = [], []


def rel(path):
    return os.path.relpath(path, ROOT)


def load(path):
    with open(os.path.join(ROOT, path), encoding="utf-8") as f:
        return yaml.safe_load(f)


def size(path):
    return os.path.getsize(os.path.join(ROOT, path))


def read(path):
    with open(os.path.join(ROOT, path), encoding="utf-8") as f:
        return f.read()


def front_matter(text):
    m = re.match(r"^---\n(.*?)\n---\n", text, re.S)
    return yaml.safe_load(m.group(1)) if m else None


def check_budgets(cmap):
    b = cmap["budgets_bytes"]
    if size("AGENTS.md") > b["tier0"]:
        errors.append(f"AGENTS.md is {size('AGENTS.md')} B > tier0 budget {b['tier0']} B")
    for name in sorted(os.listdir(os.path.join(ROOT, "personas/cards"))):
        p = f"personas/cards/{name}"
        if size(p) > b["card"]:
            errors.append(f"{p} is {size(p)} B > card budget {b['card']} B")
    for name in sorted(os.listdir(os.path.join(ROOT, "context/packs"))):
        p = f"context/packs/{name}"
        if size(p) > b["pack"]:
            errors.append(f"{p} is {size(p)} B > pack budget {b['pack']} B")
    total = 0
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames if d != ".git"]
        for fn in filenames:
            if fn.endswith((".md", ".yaml", ".yml")):
                total += os.path.getsize(os.path.join(dirpath, fn))
    if total > TOTAL_BUDGET:
        errors.append(f"governance text is {total} B > {TOTAL_BUDGET} B — delete or simplify a rule (GOV-2)")
    return total


def check_roster():
    roster = load("personas/roster.yaml")
    roles = set(roster["roles"])
    unconfirmed = [r for r, v in roster["roles"].items() if not v.get("confirmed")]
    if unconfirmed:
        warnings.append(f"roster: {len(unconfirmed)} role(s) unconfirmed (migration H2): {', '.join(unconfirmed)}")
    return roles


def check_cards(cmap):
    cards = set()
    for name in sorted(os.listdir(os.path.join(ROOT, "personas/cards"))):
        text = read(f"personas/cards/{name}")
        cards.add(name[:-3])
        for s in CARD_SECTIONS:
            if s not in text:
                errors.append(f"personas/cards/{name}: missing section {s!r}")
    for r in cmap["routes"]:
        if r["maker"] not in cards:
            errors.append(f"context-map route {r['lane']}: maker card {r['maker']!r} does not exist")
    return cards


def check_guardrails(roles):
    g = load("state/guardrails.yaml")
    ids = set()
    for item in g["guardrails"]:
        gid = item.get("id")
        if gid in ids:
            errors.append(f"guardrails: duplicate id {gid}")
        ids.add(gid)
        for k in ("rule", "scope", "owner", "enforcement"):
            if not item.get(k):
                errors.append(f"guardrails {gid}: missing {k}")
        if item.get("owner") not in roles:
            errors.append(f"guardrails {gid}: owner {item.get('owner')!r} not a roster role")
        if item.get("enforcement") not in ENFORCEMENT:
            errors.append(f"guardrails {gid}: enforcement must be one of {sorted(ENFORCEMENT)}")
    review_only = sum(1 for i in g["guardrails"] if i.get("enforcement") == "review")
    warnings.append(f"guardrails: {review_only}/{len(ids)} enforced by review only — automation candidates (GR-2)")
    return ids, g["guardrails"]


def check_decisions(roles):
    d = load("state/decisions.yaml")
    ids = set()
    today = datetime.date.today()
    for item in d.get("open") or []:
        did = item.get("id")
        ids.add(did)
        for k in ("question", "owner", "type", "needed_by", "default_while_pending", "blocks_promotion_to"):
            if item.get(k) in (None, ""):
                errors.append(f"decisions {did}: missing {k} (DC-1 needs a default and a promotion target)")
        if item.get("owner") not in roles:
            errors.append(f"decisions {did}: owner {item.get('owner')!r} not a roster role")
        if item.get("type") not in (1, 2):
            errors.append(f"decisions {did}: type must be 1 or 2")
        if item.get("blocks_promotion_to") not in PROMOTION:
            errors.append(f"decisions {did}: blocks_promotion_to must be one of {sorted(PROMOTION)}")
        due = item.get("needed_by")
        if due and datetime.date.fromisoformat(str(due)) < today:
            warnings.append(f"decisions {did}: past needed_by {due} — decision clinic (DC-2)")
    for item in d.get("decided") or []:
        ids.add(item.get("id"))
    return ids


def check_packs(cmap, guardrail_ids, decision_ids, roles):
    known_decisions = decision_ids | {"DB-DEC-0001", "DB-DEC-0002"}
    for r in cmap["routes"]:
        if r["pack"] and not os.path.exists(os.path.join(ROOT, "context", r["pack"])):
            errors.append(f"context-map route {r['lane']}: pack {r['pack']} missing")
    today = datetime.date.today()
    for name in sorted(os.listdir(os.path.join(ROOT, "context/packs"))):
        if name.startswith("_"):
            continue
        p = f"context/packs/{name}"
        text = read(p)
        fm = front_matter(text)
        if not fm:
            errors.append(f"{p}: missing front matter (lane, owner, verified_on)")
            continue
        if fm.get("owner") not in roles:
            errors.append(f"{p}: owner {fm.get('owner')!r} not a roster role")
        age = (today - datetime.date.fromisoformat(str(fm["verified_on"]))).days
        if age > STALE_DAYS:
            warnings.append(f"{p}: verified {age} days ago — owner to re-verify (CX-5, warning only)")
        for s in PACK_SECTIONS:
            if s not in text:
                errors.append(f"{p}: missing section {s!r}")
        for gid in set(re.findall(r"\bGR-[A-Z]{3}-\d{2}\b", text)):
            if gid not in guardrail_ids:
                errors.append(f"{p}: unknown guardrail {gid}")
        for did in set(re.findall(r"\bDEC-OPEN-[A-Z0-9-]+\b", text)):
            if did not in known_decisions:
                errors.append(f"{p}: unknown decision {did} (add it to state/decisions.yaml)")


LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)\)")


def check_links():
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames if d != ".git"]
        for fn in filenames:
            if not fn.endswith(".md"):
                continue
            path = os.path.join(dirpath, fn)
            for target in LINK.findall(read(rel(path))):
                if re.match(r"^[a-z]+:", target) or target.startswith("#"):
                    continue
                file_part, _, anchor = target.partition("#")
                dest = os.path.normpath(os.path.join(dirpath, file_part))
                if not os.path.exists(dest):
                    errors.append(f"{rel(path)}: broken link {target}")
                elif anchor and dest.endswith(".md") and anchor not in anchors(dest):
                    errors.append(f"{rel(path)}: missing anchor #{anchor} in {rel(dest)}")


def anchors(path):
    out = set()
    with open(path, encoding="utf-8") as f:
        for line in f:
            m = re.match(r"^#{1,6}\s+(.*)", line)
            if m:
                slug = re.sub(r"[^\w\- ]", "", m.group(1).strip().lower()).replace(" ", "-")
                out.add(slug)
    return out


def route(argv):
    cmap = load("context/context-map.yaml")
    guardrails = load("state/guardrails.yaml")["guardrails"]
    lanes = []
    if argv and argv[0] == "--keywords":
        words = " ".join(argv[1:]).lower()
        for r in cmap["routes"]:
            if any(k in words for k in r.get("keywords", [])):
                lanes.append(r)
    else:
        for p in argv:
            for r in cmap["routes"]:
                if any(fnmatch.fnmatch(p, g) for g in r["paths"]):
                    if r not in lanes:
                        lanes.append(r)
                    break
    if not lanes:
        print(f"no route: {cmap['fallback']['note']}")
        return 0
    for i, r in enumerate(lanes):
        scope = {"programme", f"lane:{r['lane']}"}
        ids = [g["id"] for g in guardrails if g["scope"] in scope]
        role = "primary" if i == 0 else "summary-only"
        print(f"lane={r['lane']} ({role})  pack=context/{r['pack']}  maker=personas/cards/{r['maker']}.md")
        print(f"  guardrails: {', '.join(ids)}")
    return 0


def main():
    if len(sys.argv) > 1 and sys.argv[1] == "route":
        return route(sys.argv[2:])
    cmap = load("context/context-map.yaml")
    roles = check_roster()
    total = check_budgets(cmap)
    check_cards(cmap)
    guardrail_ids, _ = check_guardrails(roles)
    decision_ids = check_decisions(roles)
    check_packs(cmap, guardrail_ids, decision_ids, roles)
    check_links()
    for w in warnings:
        print(f"WARN  {w}")
    for e in errors:
        print(f"ERROR {e}")
    print(f"\n{len(errors)} error(s), {len(warnings)} warning(s); governance text {total // 1024} KB / {TOTAL_BUDGET // 1024} KB")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
