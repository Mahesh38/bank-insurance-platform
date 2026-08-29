# Rollback

## The anchor

`pre-gitlab-migration` -> `b8027751738b04d00dbe071a77b2aba56828a2cd`

**Currently local-only.** GitHub refused the tag push on the session credential
(`HTTP 403`, tag refs out of scope). `RISK-025`: a rollback anchor that exists
only in a container is not an anchor. It must be on the remote before the freeze.

## Rollback is tested by executing it

`C-OPS-2`. Not by reading this file.

```
clone from the anchor -> verify the tree -> confirm the build
```

If that has not been done, the freeze does not lift and GitLab is not declared
authoritative (`M9.5` gates `M9.4`).

## What rolls back, and what does not

| Layer | Reversible? |
|---|---|
| GitLab groups and projects | Yes — `prevent_destroy` means removal is deliberate |
| Repository content | Yes, from the anchor |
| A pushed history rewrite | **No.** This is why `filter-repo` is barred until finding B is resolved and the anchor is on the remote |
| Terraform state | Only from backend versioning — untested restore is a belief |
| An archived GitHub origin | Awkward. Hence `AC-4`: read-only at cutover, restorable 14 days, archived only after the custody disposition |

## Freeze expiry

If the ≤48h window expires with the migration incomplete: **roll back to the anchor
and re-schedule** (`C-OPS-3`). Not "keep both writable for a bit" — that is how a
migration acquires two permanent sources of truth. The answer is cheaper than dual
write only because it was decided in advance.
