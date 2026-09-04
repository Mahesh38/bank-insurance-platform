# Operating model

## The loop

```
requirement / ticket -> merge request -> review -> plan -> approved apply
                     -> GitLab configuration change -> audit evidence -> drift monitoring
```

Once Terraform owns a setting, routine manual UI configuration stops. Emergency
manual changes are legitimate during an incident and must be **reconciled back
into configuration afterwards** — otherwise desired and actual state diverge
permanently and the next plan proposes to undo the fix.

## Who decides what

| Change | Decides |
|---|---|
| A new project, subgroup or label | Config change, MR review, SRE apply |
| Branch protection or approval rules | SRE proposes · **Deepali** (Board 4) on the security outcome |
| Protected environments, production access | SRE proposes · Deepali + Shailja |
| Anything touching a service boundary | **Mahesh** (Board 1) — a repository is not a bounded context |
| Provider upgrade | SRE, reviewed plan |
| Deleting a group or project | Never routine. `prevent_destroy` is deliberate friction |

## Applies

`apply` runs from a protected pipeline job, manually, by an authorised operator.
Never from a feature branch, never locally, never automatically (baseline §11.1).

Fully automated apply to the GitLab control plane is introduced only if it matches
the bank SRE operating model and carries equivalent protection. It does not today.

## Standing constraints

- `GITLAB_TOKEN` is a dedicated automation account, never a personal token.
- No secret value passes through Terraform (`C-SEC-9`).
- The `insurance` parent group is read, never managed.
- Bootstrap state lives outside the instance it controls (`SEC-F07`).
- `prevent_destroy` is not removed to make a plan succeed.
