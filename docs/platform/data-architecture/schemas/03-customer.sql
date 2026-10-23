-- DESIGN DDL — Customer snapshot SoR. Apply at S09 in the owning Customer service Flyway.
-- CBS remains master for CIF. ⚑ attributes live only in encrypted payloads (PII-01).
-- INV-LOB-01: lob is mandatory on the journey-scoped snapshot, not on the living profile
-- (a customer is not a LOB). See DB-DEC-0001 observation 2.
--
-- D3 / D-023 (owner decision 2026-10-21, DECISIONS-2026-10-21.md §D3):
--   customer_id = platform ULID (CHAR(26)), minted by customer-service on first resolve. It is the
--   ONLY customer reference outside this schema (lead, journey, suitability, consent, quote,
--   proposal, payment, policy all carry customer_id CHAR(26)).
--   CIF = core-banking business key. It appears only here — encrypted (cif_enc) and as an HMAC
--   lookup hash (cif_lookup_hash, UNIQUE) — and in calls to CBS / 1SB adapters.
--   Resolve is idempotent: the same CIF always maps to the same customer_id (ux_customer_cif_hash).

CREATE TABLE customer.customer (
    customer_id         CHAR(26)                 PRIMARY KEY,           -- D3: platform ULID
    cif_lookup_hash     BYTEA                    NOT NULL,              -- D3: HMAC-SHA256(CIF)
    lookup_key_id       VARCHAR(50)              NOT NULL,              -- D3: HMAC key version (rotation re-hashes)
    cif_enc             BYTEA                    NOT NULL,              -- D3: CIF ciphertext, never plaintext
    pii_enc             BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    snapshot_taken_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    source_system       VARCHAR(16)              NOT NULL,
    retain_until        DATE,                                           -- READINESS-REVIEW §3.5: retention column
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version             BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_customer_source CHECK (source_system IN ('CBS')),
    CONSTRAINT ck_customer_id_ulid CHECK (customer_id ~ '^[0-9A-HJKMNP-TV-Z]{26}$'),  -- D3
    CONSTRAINT ux_customer_cif_hash UNIQUE (cif_lookup_hash)                          -- D3 / D-023
);

COMMENT ON COLUMN customer.customer.customer_id IS 'D-023: platform ULID minted on first resolve. The only customer id used outside schema customer.';
COMMENT ON COLUMN customer.customer.cif_lookup_hash IS 'D-023: HMAC-SHA256 of CIF with the lookup key (lookup_key_id). Never the CIF. UNIQUE: one customer_id per CIF.';
COMMENT ON COLUMN customer.customer.cif_enc IS 'D-023: encrypted CIF; decrypted only to call CBS / 1SB adapters.';
COMMENT ON COLUMN customer.customer.pii_enc IS 'Encrypted bundle: fullName, dob, gender, mobile, email, address, annualIncome, tobaccoUse, pan, aadhaarRef.';

-- Frozen copy used by one journey. Write-once.
-- Freshness (D3): a snapshot is reusable for a journey only while now() < fresh_until; after that
-- customer-service re-reads CBS and writes a NEW snapshot row (the old row is never updated).
-- The freshness window is configuration (rule set / config domain CUSTOMER.SNAPSHOT, ADR-024),
-- not a code constant; no default value is set here.
CREATE TABLE customer.customer_snapshot (
    snapshot_id         CHAR(26)                 PRIMARY KEY,
    customer_id         CHAR(26)                 NOT NULL REFERENCES customer.customer (customer_id),
    journey_id          CHAR(26),                                       -- NULL until a journey binds it
    lead_id             CHAR(26)                 NOT NULL,
    lob                 VARCHAR(16)              NOT NULL,
    pii_enc             BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    snapshot_taken_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    cbs_as_of           TIMESTAMP WITH TIME ZONE NOT NULL,              -- D3: CBS data timestamp
    fresh_until         TIMESTAMP WITH TIME ZONE NOT NULL,              -- D3: reuse window end
    payload_hash        VARCHAR(71)              NOT NULL,              -- 'sha256:'+hex; detects CBS change
    config_version_ref  VARCHAR(64)              NOT NULL,              -- freshness window version (ADR-024)
    source_system       VARCHAR(16)              NOT NULL,
    retain_until        DATE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_snapshot_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_snapshot_source CHECK (source_system IN ('CBS')),
    CONSTRAINT ck_snapshot_fresh CHECK (fresh_until > cbs_as_of)
);

-- One snapshot per journey once bound (partial: unbound snapshots may coexist).
CREATE UNIQUE INDEX ux_snapshot_journey
    ON customer.customer_snapshot (journey_id)
    WHERE journey_id IS NOT NULL;
CREATE INDEX ix_snapshot_customer ON customer.customer_snapshot (customer_id, created_at);
CREATE INDEX ix_snapshot_customer_fresh ON customer.customer_snapshot (customer_id, fresh_until);
CREATE INDEX ix_snapshot_lead_lob ON customer.customer_snapshot (lead_id, lob);

CREATE TABLE customer.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_customer_idemp_expiry ON customer.idempotency_record (expires_at);

-- ADR-025 §3: transactional outbox (CustomerResolved, CustomerSnapshotTaken; ids only, no PII).
CREATE TABLE customer.outbox_event (
    outbox_id           CHAR(26)                 PRIMARY KEY,           -- ADR-025 §6: ULID
    aggregate_type      VARCHAR(64)              NOT NULL,
    aggregate_id        CHAR(26)                 NOT NULL,
    event_type          VARCHAR(96)              NOT NULL,
    schema_version      SMALLINT                 NOT NULL DEFAULT 1,
    payload             JSONB                    NOT NULL,              -- full event-envelope/v1
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    published_at        TIMESTAMP WITH TIME ZONE,
    attempts            INTEGER                  NOT NULL DEFAULT 0,
    last_error          VARCHAR(1000),
    parked_at           TIMESTAMP WITH TIME ZONE                         -- ADR-025 §3: parked after 10 failures
);

CREATE INDEX ix_customer_outbox_unpublished
    ON customer.outbox_event (created_at)
    WHERE published_at IS NULL AND parked_at IS NULL;
