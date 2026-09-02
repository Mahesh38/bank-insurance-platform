-- DESIGN DDL — Quotation SoR. Apply at S09 in the owning Quotation service Flyway.
-- Adapter job/poll rows stay in bank_persistence. This schema is the bank-owned quote/offer.

CREATE TABLE quotation.quote (
    quote_id            CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL,
    journey_id          CHAR(26)                 NOT NULL,
    customer_id         CHAR(26)                 NOT NULL,
    suitability_id      CHAR(26)                 NOT NULL,
    consent_id          CHAR(26)                 NOT NULL,
    lob                 VARCHAR(16)              NOT NULL,
    mode                VARCHAR(16)              NOT NULL,
    category            VARCHAR(32)              NOT NULL,
    requested_cover     NUMERIC(15, 2)           NOT NULL,
    members_enc         BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    state               VARCHAR(24)              NOT NULL,
    valid_until         TIMESTAMP WITH TIME ZONE,
    provider            VARCHAR(20),
    provider_job_id     VARCHAR(36),
    config_version_ref  VARCHAR(64)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version             BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_quote_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_quote_mode CHECK (mode IN ('SINGLE', 'MULTI')),
    CONSTRAINT ck_quote_state CHECK (state IN (
        'REQUESTED', 'IN_PROGRESS', 'QUOTED', 'PARTIALLY_QUOTED', 'SELECTED',
        'CONVERTED', 'EXPIRED', 'FAILED', 'TIMED_OUT', 'REJECTED'))
);

CREATE INDEX ix_quote_journey ON quotation.quote (journey_id);
CREATE INDEX ix_quote_lead_lob ON quotation.quote (lead_id, lob, state);

CREATE TABLE quotation.offer (
    offer_id            CHAR(26)                 PRIMARY KEY,
    quote_id            CHAR(26)                 NOT NULL REFERENCES quotation.quote (quote_id),
    insurer_code        VARCHAR(50)              NOT NULL,
    product_code        VARCHAR(100)             NOT NULL,
    product_name        VARCHAR(200)             NOT NULL,
    premium_amount      NUMERIC(15, 2)           NOT NULL,
    premium_frequency   VARCHAR(16)              NOT NULL,
    tax_amount          NUMERIC(15, 2)           NOT NULL DEFAULT 0,
    sum_assured         NUMERIC(15, 2)           NOT NULL,
    cover_term          INTEGER,
    premium_paying_term INTEGER,
    out_of_bound        BOOLEAN                  NOT NULL DEFAULT false,
    offer_state         VARCHAR(20)              NOT NULL,
    error_summary       VARCHAR(500),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_offer_state CHECK (offer_state IN (
        'QUOTED', 'SELECTED', 'NOT_SELECTED', 'INVALID')),
    CONSTRAINT ck_offer_positive CHECK (premium_amount > 0 AND sum_assured > 0)
);

-- INV-QUO-05: at most one SELECTED offer per quote.
CREATE UNIQUE INDEX ux_offer_selected
    ON quotation.offer (quote_id)
    WHERE offer_state = 'SELECTED';

CREATE INDEX ix_offer_quote_state ON quotation.offer (quote_id, offer_state);

CREATE TABLE quotation.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);
