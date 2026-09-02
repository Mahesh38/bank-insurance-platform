# R0 design DDL

PostgreSQL scripts that implement [`01-physical-design.md`](../01-physical-design.md).
They are **design artefacts**. Existing runtime migrations stay in the owning service
modules. Apply these at S09 by copying into a versioned Flyway file — do not execute
this folder as a superuser against production (`DR-MIG-04`).

| File | Schema / purpose |
|---|---|
| [`00-cluster-bootstrap.sql`](./00-cluster-bootstrap.sql) | Schemas on the one Aurora cluster |
| [`01-identity.sql`](./01-identity.sql) | WS-2 identity (wraps existing Flyway) |
| [`02-bank_persistence.sql`](./02-bank_persistence.sql) | Job store + audit ingest (wraps existing Flyway) |
| [`03-customer.sql`](./03-customer.sql) | Customer snapshot |
| [`04-opportunity.sql`](./04-opportunity.sql) | Lead / opportunity |
| [`05-consent.sql`](./05-consent.sql) | Consent evidence |
| [`06-suitability.sql`](./06-suitability.sql) | Suitability |
| [`07-catalogue.sql`](./07-catalogue.sql) | Product catalogue |
| [`08-quotation.sql`](./08-quotation.sql) | Quote and offer |
| [`09-proposal.sql`](./09-proposal.sql) | Proposal and UW |
| [`10-payment.sql`](./10-payment.sql) | Payment |
| [`11-policy.sql`](./11-policy.sql) | Policy |
| [`12-journey.sql`](./12-journey.sql) | Journey orchestration |
| [`13-administration.sql`](./13-administration.sql) | Versioned configuration |
| [`14-audit_event_delta.sql`](./14-audit_event_delta.sql) | OPEN-I3 / OPEN-I5 columns |
| [`90-routines.sql`](./90-routines.sql) | Triggers, sequence, visibility, S09 purge |
| [`91-grants.sql`](./91-grants.sql) | Least-privilege grants |
