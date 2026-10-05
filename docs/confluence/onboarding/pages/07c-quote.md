# Sequence — Quote via 1SB

> **Teaching pack** · `SUG-20261005-cfp` · not a source of truth.
>
> **Confluence parent:** Sequence diagrams  
> **This page:** grandchild 7.3

C1 is re-checked at quote entry. The BFF and UI speak **bank language**. 1SB wire format exists only behind the Integration Hub and `adapter.onesb.*`.

Partial insurer success is **success**. One manufacturer failing in a multi-quote must not fail the whole job.

![Quote sequence](../diagrams/12-seq-quote.png)

## Why the Hub exists

```
UI → BFF → Integration Hub → 1SB adapter → Apigee → 1SB → insurer
```

- No platform service calls an adapter directly.
- UI/BFF never receive 1SB `entityIds` / insurer wire codes.
- 1SB JSON is not the bank's journey payload. Raw bodies are an adapter/audit artefact only.
- Quote create is **async**: POST returns a `reqId`; the client polls until complete.

1SB is a **replaceable B2B gateway**, not the CRM, not the suitability engine, not CIF, and not the long-term carrier strategy.

```mermaid
sequenceDiagram
  autonumber
  actor RM as Bank RM
  participant BFF as NIP BFF
  participant Jrn as Journey #9
  participant Q as Quotation #10
  participant Hub as Integration Hub #14
  participant SB as 1SB adapter + 1SB

  RM->>BFF: POST /journeys/{id}/quotes
  BFF->>Jrn: stage + suitabilityRef
  Note over BFF,Q: C1 — valid unexpired suitability or 403
  BFF->>Q: create quote job (canonical)
  Q->>Hub: route Life provider
  Hub->>SB: translate; POST quote
  SB-->>Hub: reqId (async)
  RM->>BFF: GET poll
  Q->>Hub: poll
  Hub->>SB: GET quote poll
  Note over Q,SB: per-insurer errors do not fail the multi-quote
  RM->>BFF: POST selection
  BFF->>Jrn: QUOTE_SELECTED
```

Term path prefix at 1SB is `/insurance/lifeterm/v1`. Saving/ULIP share the life-style skeleton on `/insurance/lifesave/v1`. Those paths belong in the adapter, not in NIP-APP.

Sources: [`universal-lob-journey.md`](../../../1sb-insurance-integration/journeys/universal-lob-journey.md) · [`01-executive-overview.md`](../../../1sb-insurance-integration/01-executive-overview.md) · standing constraint UI→BFF→Hub.
