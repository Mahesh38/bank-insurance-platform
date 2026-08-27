-- DESIGN DDL — Customer snapshot SoR. Apply at S09 in the owning Customer service Flyway.
-- CBS remains master for CIF. ⚑ attributes live only in encrypted payloads (PII-01).
-- INV-LOB-01: lob is mandatory on the journey-scoped snapshot, not on the living profile
-- (a customer is not a LOB). See DB-DEC-0001 observation 2.

CREATE TABLE customer.customer (
    customer_id         CHAR(26)                 PRIMARY KEY,
    cif_lookup_hash     BYTEA                    NOT NULL,
    cif_enc             BYTEA                    NOT NULL,
    pii_enc             BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    snapshot_taken_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    source_system       VARCHAR(16)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version             BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_customer_source CHECK (source_system IN ('CBS')),
    CONSTRAINT ux_customer_cif_hash UNIQUE (cif_lookup_hash)
);

COMMENT ON COLUMN customer.customer.cif_lookup_hash IS 'HMAC-SHA256 of CIF with the lookup key. Never the CIF.';
COMMENT ON COLUMN customer.customer.pii_enc IS 'Encrypted bundle: fullName, dob, gender, mobile, email, address, annualIncome, tobaccoUse, pan, aadhaarRef.';

-- Frozen copy used by one journey. Write-once.
CREATE TABLE customer.customer_snapshot (
    snapshot_id         CHAR(26)                 PRIMARY KEY,
    customer_id         CHAR(26)                 NOT NULL REFERENCES customer.customer (customer_id),
    journey_id          CHAR(26)                 NOT NULL,
    lead_id             CHAR(26)                 NOT NULL,
    lob                 VARCHAR(16)              NOT NULL,
    pii_enc             BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    snapshot_taken_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    source_system       VARCHAR(16)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_snapshot_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_snapshot_source CHECK (source_system IN ('CBS')),
    CONSTRAINT ux_snapshot_journey UNIQUE (journey_id)
);

CREATE INDEX ix_snapshot_customer ON customer.customer_snapshot (customer_id, created_at);
CREATE INDEX ix_snapshot_lead_lob ON customer.customer_snapshot (lead_id, lob);
