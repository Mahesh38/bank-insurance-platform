# M2.4 — `docs/` PII, NDA and credential sweep

**Executed:** 2026-08-29 · **Run by:** Shivanshi (SRE) · **Rules owner:** Shailja (Board 6)
**Condition:** `C-CMP-2` · **Origin:** `CMP-F02` on [`CR-014`](../../../governance/change-requests/CR-014-gitlab-estate-migration.md)
**Scope:** 441 tracked files under `docs/`, ~16 MB, swept against the six classes Board 6 named.

> **Reporting only. Nothing was redacted, edited or removed** — per operator instruction of
> 2026-08-29. Disposition is Board 6's. This document gives locations and classifications; it does
> not reproduce matched values.

## Result

**63 raw pattern hits. No credentials. No real customer data identified.** Three items need a Board 6
ruling and two of them were introduced by this migration workstream, not inherited.

| Class | Hits | SRE technical read |
|---|---:|---|
| `CREDENTIAL_DOC` — passwords, keys, tokens, JWTs, private keys | **0** | Clean. The `docs/` tree carries no credential-shaped assignment |
| `PII_GOVT_ID` — PAN / Aadhaar shapes | 4 | **All benign.** 2 are the conventional dummy PAN (`ABCDE`+sequential digits) — one in an API schema example, one in an explicit *masking test case*. 2 are false positives: SVG and Figma coordinate floats matching a 12-digit shape |
| `PII_CONTACT` — emails, mobile numbers | 4 | 1 masking test case. **2 need a ruling** (§1). 1 is self-inflicted by this workstream (§2) |
| `MONETARY_SAMPLE` — premium / sum-assured literals | 3 | **Benign.** Round synthetic figures (₹1 crore, ₹50 lakh) in API examples. No policy, proposal or customer reference attached |
| `PROVIDER_NDA` — 1SB material | 19 | **17 are public vendor-portal URLs** (`docs.1silverbullet.tech`) and the demo base URL. **2 are commercial-relationship documents** and are the ones worth Board 6's attention (§3) |
| `BANK_INTERNAL` — hostnames, RFC1918, AWS account ids | 10 | 4 illustrative/placeholder hostnames, explicitly marked as such. 2 false positives (SVG coordinates). **4 are real bank hostnames added today** (§2) |
| `BINARY_ASSET` | 23 | **Not machine-checkable.** Requires visual inspection (§4) |

---

## 1. Needs a Board 6 ruling — inherited

**A personal email address in document metadata**, in two reference notes:

| File | Line | Context |
|---|---|---|
| `docs/au-bank-insurance-platform/references/2026-08-20-insurance-aggregation-and-provider-connectivity-notes.md` | 5 | `**Provided by:** Repository owner (Mahesh38 · <personal email>)` |
| `docs/au-bank-insurance-platform/references/2026-08-20-north-star-architecture-brainstorming-notes.md` | 5 | same |

It is the repository owner's own address, so this is not third-party PII, and it already appears in
every commit's author metadata. The question for Board 6 is narrow: **does a personal (non-corporate)
address belong in document provenance inside a bank estate**, or should attribution move to a role
("Repository owner") without the address. Low severity, cheap to change, and it is a policy call
rather than a technical one.

---

## 2. Needs a ruling — **introduced by this workstream today**

Reporting against myself, because the sweep does not get to skip its own author.

**Real bank hostnames are now in the repository.** `gitlab-ce.au.bank.in` was recorded on 2026-08-29
in four places as part of answering `ASM-023`:

`CR-016` §1 · `ASSUMPTION-REGISTER` `ASM-023` · `DECISION-REGISTER` §3 · `GLM-001` M1.2

Before today the repository's bank hostnames were **illustrative placeholders**, and said so —
`R0-HLD.md:359` carries *"name is illustrative; DNS is S09"*, and `bank.internal` / `aubank.local`
are pattern examples. This is the first real internal infrastructure hostname committed.

**It is almost certainly fine** — a hostname is not a credential, the instance is behind the bank
perimeter, and recording the answer to an enterprise input is the point of the register. But it is a
change in kind from placeholder to real, it happened without anyone deciding it, and `C-CMP-2` exists
precisely to catch that. Board 6 rules.

**Also noted, no action proposed:** `M2-1-gitleaks-full-history.json` embeds committer email
addresses in its `Email` fields. Those are already in the commit metadata of every commit in the
repository, so it adds no exposure. Recorded for completeness.

---

## 3. Needs a ruling — provider relationship material

17 of the 19 `PROVIDER_NDA` hits are links to 1SB's **public** documentation portal and the demo base
URL `demo.api.1silverbullet.tech`. Those are vendor-published and carry no obvious restriction.

Two are different in kind — they are the bank's side of a commercial relationship, not vendor docs:

| File | What it is |
|---|---|
| `…/service-ssot/phase-0/EMAIL-DRAFT-1SB-ONBOARDING-REQUEST.md` | A drafted onboarding request to the provider |
| `…/service-ssot/phase-0/CONFIRM-06-1sb-pune-visit-agenda.md` | An agenda for a meeting at the provider's site |

Neither contains a credential — `CONFIRM-03` correctly shows `distributorId` as *"loaded from
application config / secret store at startup"* rather than a value. The question is whether
correspondence and meeting material with a named provider should migrate into the bank estate at
all, or stay in the workstream's own records. Board 6 and Product own that.

---

## 4. The part a regex cannot do

**23 binary assets** — PNG, JPG, SVG and PDF — were flagged as *unreviewable by pattern*, not as
findings. `CMP-F02` explicitly names screenshots and diagrams as where this material hides, and it
is right: a Figma export or a screenshot of a working UI can carry a real customer name, a real
premium or a real policy number in a way no text scan will ever see.

**This is the residual risk in M2.4 and it is not closed.** Closing it needs a human to look at 23
images. That is roughly an hour, it cannot be delegated to a pattern, and it should happen before
the first push rather than after.

---

## 5. Method

Patterns, per class, are recorded in the sweep script output at
`M2-4-raw-signals.json`. Deliberate design choices:

- **Email matching excludes** `example.`, `test.`, `noreply`, `users.noreply` and `bank.local` — the
  placeholder domains — so the 4 hits are candidates, not noise.
- **Credential matching excludes** values starting `<`, `{`, `$`, `xxx`, `placeholder`, `redacted`,
  `your`, `example`, `changeme`, `*` — the documentation-placeholder shapes.
- **12-digit matching is deliberately naive**, which is why SVG coordinates matched. Reported as
  false positives rather than tuned away, so the tuning is visible and reviewable.

**A regex sweep is a filter, not a verdict.** It finds shapes. Everything above is a candidate for
Board 6, and §4 is the honest statement of what this method cannot reach.
