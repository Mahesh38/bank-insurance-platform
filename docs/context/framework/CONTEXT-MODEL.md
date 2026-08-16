# Context Model

## Design principles

1. **Context explains; authority decides.** Every conclusion points to a binding source.
2. **Framework and instance are separate.** Reuse structure and method, never copied conclusions.
3. **Load by decision, not by file count.** A profile names only the layers and roles needed.
4. **One canonical package per role.** Aliases redirect and contain no independently maintained policy.
5. **Unknown is explicit.** A missing fact becomes an assumption or dependency, not invented context.
6. **Project state stays outside context.** Delivery state, approvals and evidence remain in their SSOT.

## Manifest model

`context-manifest.yaml` is the module entry point. It declares:

- the project and problem statement;
- precedence and canonical authority;
- layers and whether they are portable;
- role packages, aliases and governance status;
- loading profiles;
- scaffold and validation commands.

Paths are repository-root relative. The validator rejects missing paths, duplicate IDs, duplicate
aliases and profile references to unknown layers or roles.

## Extension points

A different domain normally adds:

- `domain/README.md` for vocabulary and capability boundaries;
- domain-specific role modules under `roles/`;
- shared protocols only when two or more roles repeatedly exchange the same decision package;
- a new loading profile for a recurring decision class.

Do not fork the framework to add domain facts. Extend the project manifest and project overlay.
