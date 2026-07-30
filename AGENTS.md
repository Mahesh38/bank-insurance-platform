# AGENTS.md

Guidance for cloud agents working in this repository.

## Repository status

This repository currently contains only a placeholder `README.md` (`# one-silver-bullet`). There is no application source code, dependency manifests, Docker configuration, or CI workflows yet.

When product code is added, update this file with service-specific startup, lint, test, and build commands.

## Cursor Cloud specific instructions

### Services

| Service | Required? | Notes |
|---------|-----------|-------|
| *(none)* | — | No runnable services exist in the repo today. |

### System tooling (VM)

The cloud VM includes common development runtimes that are ready for future project scaffolding:

- **Node.js** v22.x with **npm** and **pnpm**
- **Python** 3.12
- **Go** 1.22
- **Git** 2.43

Docker is not installed in this environment.

### Lint / test / run

There are no project-specific lint, test, or run scripts until dependency manifests (for example `package.json`, `pyproject.toml`, or `Makefile`) are added.

To sanity-check the VM after setup, run:

```bash
git status
node -v && python3 --version && go version
```

### Update script

The VM update script is a no-op because the repository has no dependencies to install. After adding a package manager lockfile or install script, update the update script accordingly (for example `npm install` or `pnpm install`).
