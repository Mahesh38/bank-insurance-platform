# Extracted 1SB schemas

**Up:** [docs index](../../../README.md) → [1SB integration](../../README.md) → **extracted schemas**
**Source:** 1SB developer portal OpenAPI pages · portal URLs in [../SOURCE-LINKS.md](../SOURCE-LINKS.md)

---

## What these are

Raw request/response schema extracts, one file per 1SB operation. Filenames follow the
**portal doc slug**, not the HTTP path — which is why they look the way they do.

**Use these to check exact payload shapes.** For *which* fields are mandatory and *why*, read
the [field guides](../../field-guides/README.md) instead — these files are a reference dump,
not guidance.

⚠️ **Enums and proposal-form fields are dynamic.** Refresh them at runtime from Master Lookup
and Get Proposal Form. Treat anything enumerated here as an example, not a contract.

**Looking for an operation by journey step?** Start from the
[API catalog](../../api-catalog/README.md) — every row there links here.

---

## Term / general life

| Operation | Schema |
|-----------|--------|
| Get quote (consumer request) | [consumer-request](./consumer-request-insurance-v-1-consumer-insurance-post.md) |
| Quote poll (consumer response) | [get-consumer-response](./get-consumer-response-insurance-v-1-request-id-get.md) |
| Get gate criteria form | [get-gaecriteria-form](./get-gaecriteria-form-insurance-v-1-product-id-product-id-manufacturer-id-manufacturer-id-get.md) |
| Submit gate criteria form | [post-gatecriteria-form](./post-gatecriteria-form-insurance-v-1-product-id-product-id-manufacturer-id-manufacturer-id-pos.md) |
| Product UI data | [get-product-ui-data](./get-product-ui-data-insurance-v-1-product-id-product-id-manufacturer-id-manufacturer-id-get.md) |
| Get proposal form | [get-proposal-form](./get-proposal-form-insurance-v-1-product-id-product-id-manufacturer-id-manufacturer-id-version-version-get.md) |
| Submit proposal form | [submit-proposal-form](./submit-proposal-form.md) |
| Get proposal (poll) | [get-insurance-proposal](./get-insurance-proposal.md) |

## Health

| Operation | Schema |
|-----------|--------|
| Health quote (consumer request) | [health-consumer-request](./health-consumer-request-insurance-v-1-consumer-insurance-post.md) |
| Health CKYC consumer request | [health-ckyc-consumer-request](./health-ckyc-consumer-request-insurance-v-1-consumer-insurance-post.md) |
| Health payment URL | [health-payment-url](./health-payment-url-insurance-v-1-payment-url-post.md) |
| View plan details | [view-plan-details-request](./view-plan-details-request.md) |

## Motor

| Operation | Schema |
|-----------|--------|
| Motor quote (consumer request) | [motor-consumer-request](./motor-consumer-request-insurance-v-1-consumer-insurance-post.md) |
| Motor payment URL | [motor-payment-url](./motor-payment-url-insurance-v-1-payment-url-post.md) |
| Motor proposal status | [motor-proposal-status](./motor-proposal-status.md) |
| Motor download policy | [motor-download-policy-proposal](./motor-download-policy-proposal.md) |

## Building blocks (cross-LOB)

| Operation | Schema |
|-----------|--------|
| Master lookup | [master-consumer-request](./master-consumer-request-insurance-v-1-consumer-insurance-post.md) |
| Payment URL | [payment-url](./payment-url-insurance-v-1-payment-url-post.md) |
| Payment intimation | [payment-intimation](./payment-intimation-insurance-v-1-payment-url-post.md) |
| Application status | [application-status](./application-status-insurance-v-1-application-status-post.md) |
| Get requirements | [get-requirement](./get-requirement-insurance-v-1-getrequirement-post.md) |
| Document upload | [doc-upload](./doc-upload-insurance-v-1-docupload-post.md) |
| Document download | [doc-document](./doc-document-insurance-v-1-docdownload-post.md) |
| Send OTP | [get-otp](./get-otp-insurance-v-1-sendotp-post.md) |
| Validate OTP | [validate-otp](./validate-otp-insurance-v-1-otp-verify-post.md) |
| Customer info | [customer-info](./customer-info-insurance-v-1.md) |
| Penny drop (bank account check) | [penny-drop](./penny-drop-insurance-v-1.md) |
| Get SP data (agent/SP validation) | [get-sp-data](./get-sp-data-fff.md) |

---

## Coverage

28 operations extracted. Health and Motor are **partially covered** — their poll and proposal
operations have no local copy. The [API catalog](../../api-catalog/README.md) marks these
*not extracted* and points at the portal. Term and building blocks are complete.
