# Disaster recovery references (CR-012)

**signature_status:** `AI-DRAFTED — references only; no DR test executed`

| Topic | Binding reference |
|---|---|
| Aurora restore / RPO | `NFR-DR-01` / `NFR-DR-02`; outbox is in the same cluster so events replay rather than replicate the broker (`ADR-012` D14) |
| Cache, broker, search in DR | Reconstruct, do not replicate to `ap-south-2` (`R0-LLD` D13–D15) |
| Warm standby without CBS/AD | Useless; provision DR TGW/VPN with primaries (D16) — blocked on DEP-20260824-dx1 |
| NFR-DR-03 restatement (DB-C3) | RPO 0 same-region; ≤ 60 s cross-region; compensating control is atomic business-transaction-plus-outbox. **Unachievable targets will not be signed.** |
| Uncertain payment state | Reconciliation, not database restore (DB-C9 / DB-R5) |
| Replay drill | `NFR-EVT-03` at start of S09; proves `ASM-011` |

S09 restore exercise (DB-C7 / DB-R1–R5) is not this PR.
