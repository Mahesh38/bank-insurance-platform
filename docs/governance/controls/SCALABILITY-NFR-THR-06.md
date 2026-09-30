# NFR-THR-06 recomputation (CR-012 Aarti condition 2)

**signature_status:** `AI-DRAFTED — spreadsheet, not a production measurement`

Rule: `Σ(pods × pool size) ≤ 60%` of `max_connections`, **including** outbox publisher and event consumers at **KEDA maximum**, not steady state. If the ceiling binds, lower KEDA max — do not enlarge the pool.

## Working numbers (R0 planning assumptions)

Assumptions are labelled. They are not a load test.

| Deployment | Steady pods | KEDA max | Pool / pod | Steady conns | Max conns |
|---|---|---|---|---|---|
| Journey / quote / proposal / payment / policy (5) | 2 | 6 | 10 | 100 | 300 |
| BFF | 2 | 4 | 10 | 20 | 40 |
| Integration hub | 2 | 4 | 10 | 20 | 40 |
| Persistence | 2 | 4 | 20 | 40 | 80 |
| Outbox publisher | 1 | 2 | 5 | 5 | 10 |
| Consumers (audit + 2 others) | 3 | 12 | 5 | 15 | 60 |
| **Total** | | | | **200** | **530** |

If `max_connections` is 1000 (typical Aurora starting point), 60% = 600. **530 < 600** at KEDA max under these assumptions. Headroom is thin; adding search-indexers or MIS batch must be subtracted from KEDA max, not added to pool size.

Unknowns: actual `max_connections`, real pool settings, KEDA specs. Recalculate at S09 design review with Aarti + Shivanshi before first `uat` apply.
