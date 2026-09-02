-- DESIGN DDL — not a Flyway file. Do not run against production with a superuser.
-- Apply at S09 via IaC (database + roles) and per-service Flyway (objects).
--
-- Cluster: one Aurora PostgreSQL 15+ instance (ADR-008).
-- Database name is illustrative; the owning account may use the default postgres db
-- with schemas only. Do not create a second database for audit.

-- CREATE DATABASE bank_insurance_r0;

CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS bank_persistence;
CREATE SCHEMA IF NOT EXISTS customer;
CREATE SCHEMA IF NOT EXISTS opportunity;
CREATE SCHEMA IF NOT EXISTS consent;
CREATE SCHEMA IF NOT EXISTS suitability;
CREATE SCHEMA IF NOT EXISTS catalogue;
CREATE SCHEMA IF NOT EXISTS quotation;
CREATE SCHEMA IF NOT EXISTS proposal;
CREATE SCHEMA IF NOT EXISTS payment;
CREATE SCHEMA IF NOT EXISTS policy;
CREATE SCHEMA IF NOT EXISTS journey;
CREATE SCHEMA IF NOT EXISTS administration;

COMMENT ON SCHEMA identity IS 'WS-2 Identity & Access — roles, entitlements, certification. PDP SoR.';
COMMENT ON SCHEMA bank_persistence IS '1SB adapter job/correlation store + audit ingestion. Not a business-context store.';
COMMENT ON SCHEMA customer IS 'Customer profile snapshot SoR. CBS remains master for CIF.';
COMMENT ON SCHEMA opportunity IS 'Lead / opportunity SoR. Single RM origination (INV-LED-04).';
COMMENT ON SCHEMA consent IS 'Consent evidence SoR. Append-only evidence columns.';
COMMENT ON SCHEMA suitability IS 'Suitability assessment SoR. COMPLETED rows are not updated.';
COMMENT ON SCHEMA catalogue IS 'Product, insurer, eligibility SoR.';
COMMENT ON SCHEMA quotation IS 'Quote and offer SoR. Adapter jobs stay in bank_persistence.';
COMMENT ON SCHEMA proposal IS 'Proposal and underwriting case SoR. PII form values are references.';
COMMENT ON SCHEMA payment IS 'Payment SoR. No cardholder data. RECONCILED is the issuance gate.';
COMMENT ON SCHEMA policy IS 'Policy and issuance SoR. Document bytes live in S3.';
COMMENT ON SCHEMA journey IS 'Journey orchestration SoR. Stage and references only (INV-JRN-02).';
COMMENT ON SCHEMA administration IS 'Versioned configuration SoR. INSERT-only versions (INV-CFG-02).';
