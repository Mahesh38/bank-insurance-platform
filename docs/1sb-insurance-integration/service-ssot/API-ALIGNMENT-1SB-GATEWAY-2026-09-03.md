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

1. Exact Saving proposal poll URL template (portal slug is long; confirm against demo).
2. Whether Term quote body prefers nested `product.product` vs `product.productType` in live sandbox (we now emit **both** for Term: `productType` + legacy `product`).
3. Application-status path variants for Saving/ULIP (today Term prostat path).
4. Wire ULIP list/performance ports when fund UX is in scope.

---

## Requirement coverage vs stakeholder Life LOB ask

| Stakeholder ask | Portal capability | Adapter status |
|-----------------|-------------------|----------------|
| Term Life | Retail Term APIs | Implemented |
| Savings | Retail Saving APIs | Quote handler path fixed; proposal handlers still TODO |
| ULIP | Saving + ULIP filter (+ fund helpers) | Quote handler path fixed; fund list/perf ports TODO |
| Typed JSON | N/A (engineering) | Quote path typed; proposal still Map-based |
| Resilience | N/A (engineering) | Poll config + CB in place |
