-- DESIGN DDL — Quotation SoR. Apply at S09 in the owning Quotation service Flyway.
-- Adapter job/poll rows stay in bank_persistence. This schema is the bank-owned quote/offer.
--
-- DB-DEC-0005 (supersedes the DB-DEC-0004 store): pre-payment quote state lives in the
-- DynamoDB table {env}-quotation-working (see 20-dynamodb-working-payload.md). Structured
-- quote/offer rows below are INSERT-only after Payment RECONCILED (materialize, ON CONFLICT
-- DO NOTHING). idempotency_record here dedupes materialize events by eventId.
-- Journey never opens this schema.
--
-- 2026-10-21 (D8 / D-025, READINESS-REVIEW §3.6, schemas/20 §6 rows 3 and 5):
--   * customer_id CHAR(26) ULID (D3).
--   * selected_offer_id on quote (D8 "which one was selected").
--   * Quote stays SELECTED through payment and becomes CONVERTED at materialise (D8): the row is
--     inserted with state = 'CONVERTED'.
--   * offer_state vocabulary is the STORE vocabulary: QUOTED | SELECTED | NOT_SELECTED | INVALID.
--     The API value AVAILABLE is presentation only and maps to QUOTED before materialise; after
--     materialise every offer is SELECTED, NOT_SELECTED or INVALID (D8: invalid offers are kept).
--   * INVALID offers may carry NULL / non-positive premium or sum assured (ck_offer_positive relaxed).
--   * Column names premium_frequency / premium_paying_term / cover_term are canonical; the envelope
--     and API names frequency / ppt / term map onto them (mapping table in api-reference/data).
--   * retain_until for evidence retention (RET-7Y from policy end / journey terminal).

-- Structured SoR — populated only by materialize after Payment RECONCILED.
CREATE TABLE quotation.quote (
    quote_id            CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL,
    journey_id          CHAR(26)                 NOT NULL,
    customer_id         CHAR(26)                 NOT NULL,      -- D3: platform ULID
    suitability_id      CHAR(26)                 NOT NULL,
    evaluation_id       CHAR(26)                 NOT NULL,      -- D4: suitability.evaluation used by the gate
    consent_id          CHAR(26)                 NOT NULL,      -- D5: BEFORE_MULTI_QUOTE consent (CNS-QSH)
    product_class       VARCHAR(16)              NOT NULL,      -- D4: class the gate checked
    lob                 VARCHAR(16)              NOT NULL,
    mode                VARCHAR(16)              NOT NULL,
    category            VARCHAR(32)              NOT NULL,
    requested_cover     NUMERIC(15, 2)           NOT NULL,
    members_enc         BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    state               VARCHAR(24)              NOT NULL,
    selected_offer_id   CHAR(26),                               -- D8; logical FK to offer (inserted in same tx)
    valid_until         TIMESTAMP WITH TIME ZONE,
    provider            VARCHAR(20),
    provider_job_id     VARCHAR(36),
    config_version_ref  VARCHAR(64)              NOT NULL,
    materialized_event_id CHAR(26),                             -- PaymentReconciled eventId (ADR-025)
    materialized_at     TIMESTAMP WITH TIME ZONE,
    retain_until        DATE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version             BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_quote_class CHECK (product_class IN ('TERM', 'SAVINGS', 'ULIP')),
    -- D8: a materialised quote is CONVERTED and names its selected offer.
    CONSTRAINT ck_quote_converted_selected CHECK (
        state <> 'CONVERTED' OR selected_offer_id IS NOT NULL),
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
    premium_amount      NUMERIC(15, 2),                         -- NULL allowed only for INVALID
    premium_frequency   VARCHAR(16),                            -- API/envelope 'frequency'
    tax_amount          NUMERIC(15, 2)           NOT NULL DEFAULT 0,
    sum_assured         NUMERIC(15, 2),                         -- NULL allowed only for INVALID
    cover_term          INTEGER,
    premium_paying_term INTEGER,
    out_of_bound        BOOLEAN                  NOT NULL DEFAULT false,
    offer_state         VARCHAR(20)              NOT NULL,
    error_summary       VARCHAR(500),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_offer_state CHECK (offer_state IN (
        'QUOTED', 'SELECTED', 'NOT_SELECTED', 'INVALID')),
    -- READINESS-REVIEW §3.6 / schemas/20 §6 row 3: INVALID offers are kept (D8) with whatever the
    -- insurer returned; every other state needs a positive premium, sum assured and frequency.
    CONSTRAINT ck_offer_positive CHECK (
        offer_state = 'INVALID'
        OR (premium_amount > 0 AND sum_assured > 0 AND premium_frequency IS NOT NULL))
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
