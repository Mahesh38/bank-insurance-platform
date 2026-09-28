---
name: load-context
description: Decide exactly what to read before working — resolve the product-repository paths you will change to a capability lane, then load that lane's context pack, only the guardrails in scope, and one persona card, within a 25 KB budget. Use at the start of any card, review, design question or incident, and whenever you are about to grep across docs or open a large document to find one fact.
---

# Load context

Source rules: `context/README.md` (CX-1…CX-6). Routing: `context/context-map.yaml`.

## Run
```bash
python3 scripts/validate.py route services/1sb-integration-service/src/main/java/Foo.java
# -> lane, pack, maker card, guardrail ids in scope
python3 scripts/validate.py route --keywords "consent retention"
```
If the script is unavailable, match paths against `routes[].paths` by hand (first match wins),
then keywords, then the fallback (Tier 0 only, and ask which capability).

## Load, in order
1. `AGENTS.md` (always).
2. The pack for the lane owning most of the change; for other lanes touched, only their
   `## Summary` section.
3. Guardrails whose `scope` is `programme` or `lane:<this lane>`.
4. One maker card (`routes[].maker`) — or the advisor card you were asked to wear.
5. Tier-3 sources **only** when a specific question needs them, via the pack's
   "Where truth lives" list. Read the section, not the whole file.

## Budget
Stop at 25 KB before Tier 3. If you need more, the pack is missing something — note it as a
`governance` Enabler card and carry on.

## Staleness
A pack past `verified_on` + 30 days is still usable; say so in your output and prefer generated
sources (contracts, tests, CI config) for the facts you rely on.
