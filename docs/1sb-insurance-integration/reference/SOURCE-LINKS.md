# Reference — source links & extracted schemas

## Primary documentation

| Resource | URL |
|----------|-----|
| Insurance Gateway intro | https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/insurance-gateway-api |
| Retail Term category | https://docs.1silverbullet.tech/docs/category/insurance/retail/apiDocs/term |
| Retail Health category | https://docs.1silverbullet.tech/docs/category/insurance/retail/apiDocs/health |
| Retail Motor category | https://docs.1silverbullet.tech/docs/category/insurance/retail/apiDocs/motor |
| Building blocks hub | https://docs.1silverbullet.tech/docs/insurance/building-blocks/apiDocs/insurance-gateway-api |
| Group hub | https://docs.1silverbullet.tech/docs/insurance/group/apiDocs/insurance-gateway-api |
| Infosec FAQ | https://docs.1silverbullet.tech/docs/faq/infosec/ |
| 1SB marketing / insurers overview | https://1silverbullet.tech/insurers/ |

## Confirmed demo endpoints (sample)

| Operation | Endpoint |
|-----------|----------|
| Term quote | `POST https://demo.api.1silverbullet.tech/insurance/lifeterm/v1/quote` |
| Term quote poll | `GET https://demo.api.1silverbullet.tech/insurance/lifeterm/v1/quote/poll/:requestId` |
| Term gate criteria | `GET https://demo.api.1silverbullet.tech/insurance/lifeterm/v1/quote/gateCriteria` |
| Term product UI | `GET https://demo.api.1silverbullet.tech/insurance/lifeterm/v1/master/getproductuidata` |
| Term proposal | `GET/POST https://demo.api.1silverbullet.tech/insurance/lifeterm/v1/proposal` |
| Health quote | `POST https://demo.api.1silverbullet.tech/insurance/lifehealth/v1/quote` |
| Saving quote | `POST https://demo.api.1silverbullet.tech/insurance/lifesave/v1/quote` |
| Saving quote poll | `GET https://demo.api.1silverbullet.tech/insurance/lifesave/v1/quote/poll/:requestId` |
| ULIP quote | Same Saving quote URL with `product.savingsProductType=["ULIP"]` (no `/lifeulip` prefix) |
| Motor quote | `POST https://demo.api.1silverbullet.tech/insurance/motor/v1/quote` |
| Master lookup | `POST https://demo.api.1silverbullet.tech/v1/master/lookup` |
| Payment URL | `POST https://demo.api.1silverbullet.tech/v1/payment/url` |
| Application status (life) | `POST https://demo.api.1silverbullet.tech/LifeTerm/prostat/` |
| Get requirements | `POST https://demo.api.1silverbullet.tech/insurance/:apiId/getReq` |

## Extracted schemas folder

`extracted-schemas/` contains machine-extracted field lists from the public portal pages (markdown + JSON). Use them as a searchable index; always re-validate against the live portal/sandbox because proposal forms and enums change by insurer/product.

Notable files:

- `consumer-request-insurance-v-1-consumer-insurance-post.md` — Term quote fields
- `health-consumer-request-...md` — Health quote fields
- `motor-consumer-request-...md` — Motor quote fields
- `get-proposal-form-...md` — Term proposal schema dump
- `payment-url-...md`, `payment-intimation-...md`
- `application-status-...md`
- `master-consumer-request-...md`
