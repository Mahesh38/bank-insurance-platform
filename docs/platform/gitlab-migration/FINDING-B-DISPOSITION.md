# Finding B — disposition pack (`C-SEC-2` / `RISK-026`)

**Status:** Evidence assembled 2026-09-01. **Not retired. Not rotated. Not signed.**
**Key:** `RAW_PAYLOAD_ENCRYPTION_KEY` — 44-char base64, `sha256[:12]=0651f16a1318`
**Exposure:** 2026-08-04 → 2026-08-14, three commits, ancestors of `main`
**HEAD today:** Dockerfile default **removed**. `ENV RAW_PAYLOAD_ENCRYPTION_KEY=""` and the
service fails closed without a 32-byte key.
**Authorised path:** formal **retirement with evidence**, or **rotate** if a live consumer or
decryptable row exists. [`DEC-20260901-01`](../../governance/DEC-20260901-01-owner-authorises-unblock-path.md)

The value is not reproduced here.

## 1. What an agent can see from this repository

| Check | Result | Source |
|---|---|---|
| Literal default still in HEAD `Dockerfile` | **No.** Empty ENV; fail-closed comment at lines 63–77 | `Dockerfile` |
| Test-fixture key (Finding C) | Packaged `bootJar` excludes `application-test.yml` (M2.7 PASS) | `m2-evidence/M2-EVIDENCE.md` §2.7 |
| Production / UAT database reachable from this environment | **Not reachable.** No evidence either way about Render or any bank DB | this pack |
| Any `raw_payload` row decrypts with B | **Not executed.** Aarti + Amit | — |

Absence of a database in the authoring environment is **not** evidence that no consumer ever ran
the old image. That is why retirement still needs the two human artefacts below.

## 2. Decision tree (unchanged)

1. Operator attests Render, local `.env`, UAT, prod: did any of them run the 2026-08-04 default?
2. Aarti: if persistence ever pointed at a real database, test whether any `raw_payload` row
   decrypts with B. Empty table / no database = that half is closed.
3. **Both empty → Deepali records `RETIRED`.** Then `evidence/finding-B.resolved`.
4. **Either not empty → rotate** (new key → secrets manager → consumers → verify → revoke old),
   then re-encrypt or dispose (Shailja on disposal).

Do **not** `filter-repo` the personal history into GitLab. Under `CR-017` the source graph is a
sealed offline bundle (`AC-8`).

## 3. Operator attestation (blank — human fills)

```text
I attest that the Dockerfile default RAW_PAYLOAD_ENCRYPTION_KEY
(sha256[:12]=0651f16a1318) was never the active key in:

  [ ] Render
  [ ] any committed or operator-held .env used against real data
  [ ] UAT
  [ ] production
  [ ] any other running container

Exceptions (name the environment): _______________________________

Name: ____________________  Date: __________  Role: operator
```

## 4. Aarti row check (blank — human fills)

```text
Database examined: _______________  Profile: local / uat / prod / none exists
raw_payload row count: ___________
Rows decryptable with Finding B: ___________

Name: ____________________  Date: __________  Role: Aarti / DBA delegate
```

## 5. Deepali disposition (blank — human fills)

```text
Disposition:  RETIRED / ROTATED
Evidence relied on: operator attestation + Aarti row check
Notes: ___________________________________________________________

Name: ____________________  Date: __________  Role: Board 4 Security
```

An agent must not write `evidence/finding-B.resolved`. That file is Deepali's close.
