-- DESIGN DDL — Suitability SoR. Apply at S09 in the owning Suitability service Flyway.
-- COMPLETED rows are not updated; a correction inserts a new assessment (INV-SUI-01).
-- Answers are RESTRICTED and live only in the encrypted child (PII-01).

CREATE TABLE suitability.suitability (
    suitability_id          CHAR(26)                 PRIMARY KEY,
    lead_id                 CHAR(26)                 NOT NULL,
    journey_id              CHAR(26),
    customer_id             CHAR(26)                 NOT NULL,
    lob                     VARCHAR(16)              NOT NULL,
    questionnaire_version   VARCHAR(20)              NOT NULL,
    outcome                 VARCHAR(16),
    recommended_products    TEXT[],
    evaluated_at            TIMESTAMP WITH TIME ZONE,
    valid_until             TIMESTAMP WITH TIME ZONE,
    state                   VARCHAR(20)              NOT NULL,
    override_actor_id       VARCHAR(64),
    override_reason         VARCHAR(500),
    override_at             TIMESTAMP WITH TIME ZONE,
    evidence_document_ref   VARCHAR(512),
    config_version_ref      VARCHAR(64)              NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version                 BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_suit_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_suit_state CHECK (state IN (
        'IN_PROGRESS', 'COMPLETED', 'OVERRIDDEN', 'SUPERSEDED', 'EXPIRED', 'ABANDONED')),
    CONSTRAINT ck_suit_outcome CHECK (outcome IS NULL OR outcome IN ('ELIGIBLE', 'NOT_ELIGIBLE')),
    CONSTRAINT ck_suit_override CHECK (
        (state <> 'OVERRIDDEN')
        OR (override_actor_id IS NOT NULL AND override_reason IS NOT NULL AND override_at IS NOT NULL))
);

CREATE INDEX ix_suit_customer_lob_state
    ON suitability.suitability (customer_id, lob, state, valid_until);
CREATE INDEX ix_suit_journey ON suitability.suitability (journey_id);
CREATE INDEX ix_suit_lead ON suitability.suitability (lead_id);

CREATE TABLE suitability.suitability_answer_enc (
    suitability_id      CHAR(26)                 PRIMARY KEY
        REFERENCES suitability.suitability (suitability_id),
    answers_enc         BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL
);

CREATE TABLE suitability.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);
