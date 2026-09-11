# 1SB Insurance Gateway — API alignment verification

**Date:** 2026-09-03  
**Source:** [Insurance Gateway API](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/insurance-gateway-api) + retail LOB pages (sitemap) + existing [`SOURCE-LINKS.md`](../reference/SOURCE-LINKS.md) / [`api-catalog/README.md`](../api-catalog/README.md)  
**Work:** `EPIC-002` / `DOC-020` / `CR-014`

The gateway hub page is an overview (auth + capability list). Concrete paths live on per-operation retail pages under `/docs/insurance/retail/apiDocs/…`.

---

## Verdict

| Area | Before this check | Portal / catalog | After fix |
|------|-------------------|------------------|-----------|
| Term quote / poll | `/insurance/lifeterm/v1/quote` (+ poll) | Match | Unchanged — **aligned** |
| Term proposal GET schema | `/insurance/lifeterm/v1/proposal/form?productCode=` | `GET /insurance/lifeterm/v1/proposal?productId=` | **Fixed** |
| Term proposal POST / poll | `/proposal` + `/proposal/poll/{id}` | Match (poll patterned) | Unchanged — **aligned** |
| Saving quote / poll | `/insurance/lifesaving/…` + `LifeSaving` | `/insurance/lifesave/…` + `LifeSave` | **Fixed** |
| ULIP quote | Invented `/insurance/lifeulip/…` | Saving API + `savingsProductType=ULIP` | **Fixed** |
| Auth | Basic API key/secret | Match | Aligned |
| Masters / payment URL | `/v1/master/lookup`, `/v1/payment/url` | Match (SOURCE-LINKS) | Aligned |
| Application status | `POST /LifeTerm/prostat/` | Documented life path; LOB variants TBD | Known debt (unchanged) |

---

## Portal Life LOB shape (retail)

```text
Term     → /insurance/lifeterm/v1/...
Health   → /insurance/lifehealth/v1/...
Motor    → /insurance/motor/v1/...
Saving   → /insurance/lifesave/v1/...
ULIP     → same lifesave paths + savingsProductType=["ULIP"]
           (+ ulip-list / ulip-performance helpers)
Annuity  → annuity-* pages (out of EPIC-002)
Pension  → pension-* pages (out of EPIC-002)
```

Saving quote schema requires `productType` and `savingsProductType[]` ∈ {`nonParticipating`,`Participating`,`ULIP`}.

---

## Code changes applied

- `SavingQuoteHandler` → `lifesave` + `LifeSave` + default `nonParticipating`
- `UlipQuoteHandler` → `lifesave` + `LifeSave` + `ULIP` filter (no `lifeulip` path)
- `LifeQuoteRequest.Product` → `productType` + `savingsProductType`
- `TermProposalHandler` → schema GET `/proposal` with `productId` query param
- Field guides `savings-quote.md` / `ulip-quote.md` rewritten to portal truth

---

## Remaining open confirmations (sandbox)

1. Exact Saving proposal poll URL template (portal slug is long; confirm against demo). Adapter uses `GET /insurance/lifesave/v1/proposal/poll/{id}` (mirrors Term).
2. Whether Term quote body prefers nested `product.product` vs `product.productType` in live sandbox (we now emit **both** for Term: `productType` + legacy `product`).
3. Application-status path variants for Saving/ULIP (today Term prostat path).
4. Wire ULIP list/performance ports when fund UX is in scope.
5. Saving/ULIP gate-criteria GET/POST — portal ops exist; no Java handler for **any** Life LOB including Term (not an EPIC-002 quote/proposal AC).
6. Extracted-schema markdown dumps for Saving consumer-request / proposal (portal pages are linked from field guides; Term dumps exist under `extracted-schemas/`).

---

## Requirement coverage vs stakeholder Life LOB ask

| Stakeholder ask | Portal capability | Adapter status |
|-----------------|-------------------|----------------|
| Term Life | Retail Term APIs | Implemented (`TermQuoteHandler` / `TermProposalHandler`) |
| Savings | Retail Saving APIs | Implemented (`SavingQuoteHandler` / `SavingProposalHandler` → `/insurance/lifesave/v1/…`) |
| ULIP | Saving + ULIP filter (+ fund helpers) | Quote + proposal implemented (`UlipQuoteHandler` / `UlipProposalHandler`, `savingsProductType=["ULIP"]`). Fund list/performance ports **not** wired |
| Typed JSON | N/A (engineering) | Quote and proposal typed (`LifeQuoteRequest` / `LifeProposalSubmitBody`) |
| Resilience | N/A (engineering) | Poll config + CB in place (`NFR-007` / `NFR-004`) |

`EPIC-002` / `QA-012` evidence: `LifeLobRegressionIT` (Term non-regression, Saving/ULIP quote + proposal WireMock, Saving schema GET, HEALTH → `UNSUPPORTED_LOB`).
