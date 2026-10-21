-- DESIGN DDL — Suitability SoR. Apply at S09 in the owning Suitability service Flyway.
-- E2e column sheet: ../03-lead-suitability-e2e.md
-- Savings/ULIP questionnaire only (BR-SUIT-001). Term leads have no row (BR-SUIT-002).
-- Header is mutable until LOCKED (Suitability BRD §14–15). Answer sets and mapping runs
-- are INSERT-only. OPEN-SUI-IMMUTABLE vs INV-SUI-01 is recorded in 03-lead-suitability-e2e.md.

CREATE TABLE suitability.suitability (
    suitability_id              CHAR(26)                 PRIMARY KEY,
    lead_id                     CHAR(26)                 NOT NULL,
    journey_id                  CHAR(26),
    customer_id                 CHAR(26)                 NOT NULL,
    lob                         VARCHAR(16)              NOT NULL,
    product_class               VARCHAR(16)              NOT NULL,
    questionnaire_version       VARCHAR(20)              NOT NULL,
    mapping_rule_version        VARCHAR(20),
    outcome                     VARCHAR(16),
    state                       VARCHAR(24)              NOT NULL,
    current_answer_set_version  INTEGER                  NOT NULL DEFAULT 0,
    current_mapping_run_id      CHAR(26),
    selected_product_code       VARCHAR(100),
    last_screen_id              VARCHAR(40),
    edit_lock_actor_id          VARCHAR(64),
    edit_lock_until             TIMESTAMP WITH TIME ZONE,
    last_updated_by             VARCHAR(64),
    bi_locked_at                TIMESTAMP WITH TIME ZONE,
    locked_quote_id             CHAR(26),
    override_actor_id           VARCHAR(64),
    override_reason             VARCHAR(500),
    override_at                 TIMESTAMP WITH TIME ZONE,
    evidence_document_ref       VARCHAR(512),
    config_version_ref          VARCHAR(64)              NOT NULL,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version                     BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_suit_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_suit_product CHECK (product_class IN ('SAVINGS', 'ULIP')),
    CONSTRAINT ck_suit_state CHECK (state IN (
        'IN_PROGRESS', 'COMPLETED', 'NO_PRODUCT_FOUND', 'LOCKED',
        'OVERRIDDEN', 'SUPERSEDED', 'EXPIRED', 'ABANDONED')),
    CONSTRAINT ck_suit_outcome CHECK (outcome IS NULL OR outcome IN (
        'ELIGIBLE', 'NOT_ELIGIBLE')),
    CONSTRAINT ck_suit_override CHECK (
        (state <> 'OVERRIDDEN')
        OR (override_actor_id IS NOT NULL AND override_reason IS NOT NULL
            AND override_at IS NOT NULL)),
    CONSTRAINT ck_suit_locked CHECK (
        (state <> 'LOCKED')
        OR (bi_locked_at IS NOT NULL AND locked_quote_id IS NOT NULL)),
    CONSTRAINT ck_suit_no_product CHECK (
        state <> 'NO_PRODUCT_FOUND' OR outcome = 'NOT_ELIGIBLE'),
    CONSTRAINT ck_suit_completed_outcome CHECK (
        state NOT IN ('COMPLETED', 'LOCKED') OR outcome IS NOT NULL)
);

-- One current assessment per lead. SUPERSEDED / ABANDONED may coexist if INV-SUI-01
-- is applied later without a schema change (OPEN-SUI-IMMUTABLE).
CREATE UNIQUE INDEX ux_suit_lead_current
    ON suitability.suitability (lead_id)
    WHERE state NOT IN ('SUPERSEDED', 'ABANDONED');

CREATE INDEX ix_suit_customer_lob_state
    ON suitability.suitability (customer_id, lob, state);
CREATE INDEX ix_suit_journey ON suitability.suitability (journey_id)
    WHERE journey_id IS NOT NULL;
CREATE INDEX ix_suit_lead ON suitability.suitability (lead_id);
CREATE INDEX ix_suit_lock_expiry ON suitability.suitability (edit_lock_until)
    WHERE edit_lock_until IS NOT NULL;

CREATE TABLE suitability.suitability_answer_set (
    answer_set_id       CHAR(26)                 PRIMARY KEY,
    suitability_id      CHAR(26)                 NOT NULL
        REFERENCES suitability.suitability (suitability_id),
    answer_set_version  INTEGER                  NOT NULL,
    answers_enc         BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    last_screen_id      VARCHAR(40),
    created_by          VARCHAR(64)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_suit_answer_version CHECK (answer_set_version >= 1),
    CONSTRAINT ux_suit_answer_version UNIQUE (suitability_id, answer_set_version)
);

COMMENT ON COLUMN suitability.suitability_answer_set.answers_enc IS
    'RESTRICTED ⚑ JSON ciphertext. Logical keys: policyFor, lifeAssured, lifeStage, riskPreference, primaryGoal, occupation, education, annualIncomeInr, tobacco, medicalCondition, existingLifeCover, existingCoverAmountInr, tentativePremiumInr, premiumAmountInr, premiumFrequency, premiumPayingTerm, policyTerm. Never indexed.';

CREATE TABLE suitability.suitability_mapping_run (
    mapping_run_id          CHAR(26)                 PRIMARY KEY,
    suitability_id          CHAR(26)                 NOT NULL
        REFERENCES suitability.suitability (suitability_id),
    answer_set_version      INTEGER                  NOT NULL,
    mapping_rule_version    VARCHAR(20)              NOT NULL,
    proposer_age_years      SMALLINT                 NOT NULL,
    premium_amount_inr      NUMERIC(15, 2),
    premium_frequency       VARCHAR(16),
    premium_paying_term     SMALLINT,
    policy_term             SMALLINT,
    mapping_result          VARCHAR(16)              NOT NULL,
    recommended_products    TEXT[]                   NOT NULL DEFAULT '{}',
    evaluated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    evaluated_by            VARCHAR(64)              NOT NULL,
    run_state               VARCHAR(16)              NOT NULL,
    CONSTRAINT ck_suit_map_result CHECK (mapping_result IN ('PRODUCTS', 'NONE')),
    CONSTRAINT ck_suit_map_run_state CHECK (run_state IN (
        'CURRENT', 'SUPERSEDED', 'INVALID')),
    CONSTRAINT ck_suit_map_none CHECK (
        mapping_result <> 'NONE' OR cardinality(recommended_products) = 0),
    CONSTRAINT ck_suit_map_products CHECK (
        mapping_result <> 'PRODUCTS' OR cardinality(recommended_products) >= 1),
    CONSTRAINT ck_suit_map_freq CHECK (
        premium_frequency IS NULL OR premium_frequency IN ('ANNUAL', 'SINGLE'))
);

CREATE INDEX ix_suit_map_assessment
    ON suitability.suitability_mapping_run (suitability_id, evaluated_at);
CREATE UNIQUE INDEX ux_suit_map_current
    ON suitability.suitability_mapping_run (suitability_id)
    WHERE run_state = 'CURRENT';

CREATE TABLE suitability.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_suit_idemp_expiry ON suitability.idempotency_record (expires_at);

CREATE TABLE suitability.outbox_event (
    outbox_id           CHAR(26)                 PRIMARY KEY,
    aggregate_type      VARCHAR(64)              NOT NULL,
    aggregate_id        CHAR(26)                 NOT NULL,
    event_type          VARCHAR(96)              NOT NULL,
    payload             JSONB                    NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    published_at        TIMESTAMP WITH TIME ZONE,
    attempts            INTEGER                  NOT NULL DEFAULT 0,
    last_error          VARCHAR(1000)
);

CREATE INDEX ix_suit_outbox_unpublished
    ON suitability.outbox_event (created_at)
    WHERE published_at IS NULL;
