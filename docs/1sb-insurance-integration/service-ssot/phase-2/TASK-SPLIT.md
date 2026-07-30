# Phase 2 — Task Split (Dev A / Dev B)

See [TL-KICKOFF.md](./TL-KICKOFF.md) for full AC.

| Dev | Tasks (order) | File ownership |
|-----|---------------|----------------|
| **Dev A** | P2-A1 COMP-002 → P2-A2 TECH-004 → P2-A3 TECH-005 → P2-A4 COMP-001 | `observability/Pii*`, `adapter/onesb/client|error|config` |
| **Dev B** | P2-B1 NFR-001 → P2-B2 TECH-006 → P2-B3 TECH-007 | `idempotency/**`, `adapter/onesb/polling`, `adapter/persistence/*`, `bank-persistence-service` poll-attempt API |

**Sync interface for B3:** Dev A must expose a minimal poll/GET capability on `OneSbHttpClient` (e.g. `get(path, Class)`) before Dev B finishes P2-B3. Dev B may code against the client class or a thin `OneSbPollPort` if introduced by Dev A in P2-A2.

**Status file:** Update [STATUS.md](./STATUS.md) as tasks complete.
