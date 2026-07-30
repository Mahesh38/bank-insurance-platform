# CONFIRM-02 — Term Products & Insurer Allow-List

**Phase:** 0.2  
**Status:** `PENDING` — awaiting Product + 1SB RM confirmation  
**Owner:** Product Owner + 1SB Relationship Manager  
**SSOT link:** [ACTION-PLAN.md row 0.2](../ACTION-PLAN.md) · [PRODUCT-BACKLOG.md COMP-004](../PRODUCT-BACKLOG.md)

---

## Purpose

Before any code is written the team needs at least one confirmed, sandbox-quotable Term product to develop and test against.  
This document records what must be confirmed, who confirms it, and how the catalog is loaded so products can change without a code deployment.

---

## What must be confirmed

| # | Item | Owner | Status |
|---|------|-------|--------|
| C2-1 | `manufacturerId` for at least one Term insurer enabled in sandbox | Product + 1SB RM | **PENDING** |
| C2-2 | `productCode` for that insurer's Term product enabled for this distributor | Product + 1SB RM | **PENDING** |
| C2-3 | Distributor agreement covers the product (no extra enablement step) | Product + 1SB RM | **PENDING** |
| C2-4 | Quote endpoint returns non-empty `offers[]` for a synthetic test customer | Eng (after C2-1/2) | **PENDING** — verify in sandbox |
| C2-5 | Second insurer (optional but preferred for multi-quote test) | Product + 1SB RM | **PENDING** |
| C2-6 | Populate `config/catalog/term-products.yaml` with confirmed values | Eng | **PENDING** — blocked on C2-1/2 |

**Exit criterion (0.2):** At least one `enabled: true` entry in `term-products.yaml` that produces a non-empty quote response in sandbox.

---

## Configurable product catalog

### Config file

```
config/catalog/term-products.example.yaml   ← committed template with placeholders
config/catalog/term-products.yaml           ← gitignored; populated with real values
```

The example file at [`config/catalog/term-products.example.yaml`](../../../../config/catalog/term-products.example.yaml) is the canonical template.

### How it is loaded (Spring Boot)

```java
@ConfigurationProperties(prefix = "insurance.catalog.term")
@Validated
public class TermProductCatalogProperties {
    private boolean enabled;
    private List<ProductEntry> products;
    // ... validated at context refresh
}
```

Spring loads `config/catalog/term-products.yaml` (or via `spring.config.import`) at startup.  
If `require-at-least-one-enabled: true` and no product is enabled, the service **refuses to start** — fast-fail avoids silent misconfiguration.

### Adding or disabling a product — zero code change

1. Edit `config/catalog/term-products.yaml`: set `enabled: true/false` or add a new block.
2. Restart the service (or trigger a live-refresh if Spring Cloud Config is in use).
3. No Java compilation, no PR touching business logic needed.

### Replaceability guarantee

`manufacturerId` and `productCode` exist **only** in this catalog file. Business logic references `ProductEntry` objects from `TermProductCatalogProperties`; it never contains literal insurer codes. When 1SB updates product codes, only this file changes.

---

## Checklist (sign-off required before Phase 1)

- [ ] C2-1: `manufacturerId` confirmed and documented in `term-products.yaml`
- [ ] C2-2: `productCode` confirmed and documented
- [ ] C2-3: Distributor agreement coverage confirmed
- [ ] C2-4: Sandbox quote returns non-empty `offers[]`
- [ ] C2-5: (Optional) Second insurer added for multi-quote coverage
- [ ] C2-6: `term-products.yaml` committed to environment secret/config store (not git)
- [ ] Eng: Startup validation test added (reject boot if catalog empty)

---

## Notes

- Do **not** hardcode `manufacturerId` / `productCode` in Java source as string constants.
- The `sandbox-only: true` flag must be set until prod routing is confirmed — the validator blocks accidental prod routing.
- If 1SB RM provides a product list spreadsheet, map it to the YAML format in this catalog; do not embed it as free-form notes.
