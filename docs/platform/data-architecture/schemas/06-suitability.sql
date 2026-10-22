-- DESIGN DDL — Suitability SoR. Apply at S09 in the owning Suitability service Flyway.
-- E2e column sheet: ../03-lead-suitability-e2e.md
-- D-022: every product class — Term, Savings and ULIP — has a row; Term has its own protection
-- question set (supersedes the earlier "Term leads have no row (BR-SUIT-002)" header).
-- Header is mutable until LOCKED (Suitability BRD §14–15). Answer sets, mapping runs, evaluations,
-- evaluation outcomes, caution acknowledgements and usage rows are INSERT-only (SUIT-R23).
-- OPEN-SUI-IMMUTABLE vs INV-SUI-01 is recorded in 03-lead-suitability-e2e.md.
--
-- D4 (owner decision 2026-10-21, DECISIONS-2026-10-21.md §D4): the canonical record is the
-- per-class outcome record — suitability.evaluation + suitability.evaluation_outcome (one row per
-- product class: SUITABLE | SUITABLE_WITH_CAUTION | NOT_SUITABLE | INSUFFICIENT_DATA, reason codes
-- in rule order, derived values) with the rule-set id/version that produced it (ADR-024 §6).
-- The single ELIGIBLE / NOT_ELIGIBLE header outcome is retired. INV-QUO-01 reads
-- evaluation_outcome for the quoted class against SUITABILITY.GATE.allowedOutcomes (configured).
-- consent_id is NULLABLE on evaluation: D5 moves consent after suitability (Compliance to confirm,
-- SUIT-R21 / CNS-R37 open); when CONSENT.POLICY places CNS-SOL at BEFORE_SUITABILITY it is filled.

CREATE TABLE suitability.suitability (
    suitability_id              CHAR(26)                 PRIMARY KEY,
    lead_id                     CHAR(26)                 NOT NULL,
    journey_id                  CHAR(26),
    customer_id                 CHAR(26)                 NOT NULL,
    lob                         VARCHAR(16)              NOT NULL,
    product_class               VARCHAR(16)              NOT NULL,
    questionnaire_id            VARCHAR(64)              NOT NULL,      -- SUIT-R01d: question set id per (lob, class)
    questionnaire_version       VARCHAR(32)              NOT NULL,      -- widened (GAP-007 / SUIT-R01e)
    mapping_rule_version        VARCHAR(32),
    current_evaluation_id       CHAR(26),                               -- D4: latest suitability.evaluation
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
    CONSTRAINT ck_suit_product CHECK (product_class IN ('TERM', 'SAVINGS', 'ULIP')),  -- D-022: Term has its own protection question set
    CONSTRAINT ck_suit_state CHECK (state IN (
        'IN_PROGRESS', 'COMPLETED', 'NO_PRODUCT_FOUND', 'LOCKED',
        'OVERRIDDEN', 'SUPERSEDED', 'EXPIRED', 'ABANDONED')),
    CONSTRAINT ck_suit_override CHECK (
        (state <> 'OVERRIDDEN')
        OR (override_actor_id IS NOT NULL AND override_reason IS NOT NULL
            AND override_at IS NOT NULL)),
    CONSTRAINT ck_suit_locked CHECK (
        (state <> 'LOCKED')
        OR (bi_locked_at IS NOT NULL AND locked_quote_id IS NOT NULL)),
    -- D4: a completed / locked / no-product header must point at the evaluation that decided it.
    CONSTRAINT ck_suit_completed_evaluation CHECK (
        state NOT IN ('COMPLETED', 'LOCKED', 'NO_PRODUCT_FOUND') OR current_evaluation_id IS NOT NULL)
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
    mapping_rule_version    VARCHAR(32)              NOT NULL,
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

-- ---------------------------------------------------------------------------
-- D4 — canonical evaluation record (rule pack §5, ADR-024 §6). INSERT-only (SUIT-R23).
-- Status transitions (SUIT-R29…R32) are rows in evaluation_status_event, never UPDATEs.
-- ---------------------------------------------------------------------------
CREATE TABLE suitability.evaluation (
    evaluation_id               CHAR(26)                 PRIMARY KEY,   -- the id the quote gate checks (SUIT-R20)
    suitability_id              CHAR(26)                 NOT NULL
        REFERENCES suitability.suitability (suitability_id),
    answer_set_version          INTEGER                  NOT NULL,      -- answers evaluated (SUIT-R02 / R01e)
    customer_id                 CHAR(26)                 NOT NULL,      -- D3: ULID, never the CIF (SUIT-R36 scope)
    journey_id                  CHAR(26),
    lob                         VARCHAR(16)              NOT NULL,
    questionnaire_id            VARCHAR(64)              NOT NULL,
    questionnaire_version       VARCHAR(32)              NOT NULL,
    rule_set_id                 VARCHAR(64)              NOT NULL,      -- ADR-024: e.g. SUITABILITY.LIFE.TERM
    rule_set_version            VARCHAR(20)              NOT NULL,      -- SUIT-R07 (algorithmVersion)
    engine_version              VARCHAR(20)              NOT NULL,      -- libs/bank-rule-engine version
    inputs_hash                 VARCHAR(71)              NOT NULL,      -- 'sha256:'+hex; SUIT-R05 reproducibility
    derived_values              JSONB                    NOT NULL,      -- evaluation-wide derived values (ids/numbers only)
    underwriting_risk_flag      BOOLEAN                  NOT NULL,
    data_inconsistency_flags    TEXT[]                   NOT NULL DEFAULT '{}',
    consent_id                  CHAR(26),                               -- D5: CNS-SOL consent if captured before
    pdf_artefact_ref            VARCHAR(512)             NOT NULL,      -- SUIT-R22: S3 object (Object Lock)
    evaluated_by_actor_id       VARCHAR(64)              NOT NULL,
    evaluated_by_agent_id       VARCHAR(64),                            -- SP licence (assisted)
    evaluated_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until                 TIMESTAMP WITH TIME ZONE NOT NULL,      -- evaluated_at + SUITABILITY.GATE validity days
    gate_rule_set_version       VARCHAR(20)              NOT NULL,      -- SUITABILITY.GATE version that set valid_until
    retain_until                DATE,                                   -- SUIT-R24: 7y from journey terminal
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_eval_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_eval_valid CHECK (valid_until > evaluated_at)
);

CREATE INDEX ix_eval_customer ON suitability.evaluation (customer_id, evaluated_at);
CREATE INDEX ix_eval_suitability ON suitability.evaluation (suitability_id, evaluated_at);
CREATE INDEX ix_eval_journey ON suitability.evaluation (journey_id)
    WHERE journey_id IS NOT NULL;

COMMENT ON TABLE suitability.evaluation IS 'D4: canonical suitability evaluation. One row per run of the rule sets; outcomes per class in evaluation_outcome.';

-- D4: one row per product class. Derived values optional per class (ULIP has no protectionGap).
CREATE TABLE suitability.evaluation_outcome (
    evaluation_id       CHAR(26)                 NOT NULL
        REFERENCES suitability.evaluation (evaluation_id),
    product_class       VARCHAR(16)              NOT NULL,
    outcome             VARCHAR(24)              NOT NULL,
    reason_codes        TEXT[]                   NOT NULL DEFAULT '{}', -- SUIT-R19a: rule-id order
    fired_rules         JSONB                    NOT NULL,              -- engine trace [{ruleId,outcome,reasonCode}]
    derived_values      JSONB,                                          -- per-class, optional (D4)
    rule_set_id         VARCHAR(64)              NOT NULL,              -- per-class rule set (SUITABILITY.LIFE.<CLASS>)
    rule_set_version    VARCHAR(20)              NOT NULL,
    caution_ack_required BOOLEAN                 NOT NULL DEFAULT false, -- SUIT-R41 per SUITABILITY.GATE
    PRIMARY KEY (evaluation_id, product_class),
    CONSTRAINT ck_eval_out_class CHECK (product_class IN ('TERM', 'SAVINGS', 'ULIP')),
    CONSTRAINT ck_eval_out_outcome CHECK (outcome IN (
        'SUITABLE', 'SUITABLE_WITH_CAUTION', 'NOT_SUITABLE', 'INSUFFICIENT_DATA')),
    CONSTRAINT ck_eval_out_caution CHECK (
        caution_ack_required = false OR outcome = 'SUITABLE_WITH_CAUTION')
);

-- SUIT-R29…R32: status as appended events. Current status = latest row (ACTIVE implied if none).
CREATE TABLE suitability.evaluation_status_event (
    status_event_id     CHAR(26)                 PRIMARY KEY,
    evaluation_id       CHAR(26)                 NOT NULL
        REFERENCES suitability.evaluation (evaluation_id),
    status              VARCHAR(16)              NOT NULL,
    reason              VARCHAR(40),
    occurred_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    recorded_by         VARCHAR(64)              NOT NULL,
    CONSTRAINT ck_eval_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'INVALIDATED', 'SUPERSEDED')),
    CONSTRAINT ck_eval_status_reason CHECK (
        status <> 'INVALIDATED' OR reason IN ('CONSENT_WITHDRAWN', 'MODEL_VERSION_SUPERSEDED'))
);

CREATE INDEX ix_eval_status ON suitability.evaluation_status_event (evaluation_id, occurred_at);

-- SUIT-R41…R43: caution acknowledgement captured on the customer's device with OTP.
CREATE TABLE suitability.caution_ack (
    caution_ack_id      CHAR(26)                 PRIMARY KEY,
    evaluation_id       CHAR(26)                 NOT NULL
        REFERENCES suitability.evaluation (evaluation_id),
    product_class       VARCHAR(16)              NOT NULL,
    reason_codes        TEXT[]                   NOT NULL,              -- codes acknowledged
    disclosure_text     TEXT                     NOT NULL,
    disclosure_version  VARCHAR(20)              NOT NULL,
    otp_txn_id          VARCHAR(64)              NOT NULL,
    acknowledged_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    channel             VARCHAR(24)              NOT NULL,
    retain_until        DATE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_caution_class CHECK (product_class IN ('TERM', 'SAVINGS', 'ULIP')),
    CONSTRAINT ck_caution_codes CHECK (cardinality(reason_codes) >= 1),
    CONSTRAINT ux_caution_eval_class UNIQUE (evaluation_id, product_class),
    CONSTRAINT fk_caution_outcome FOREIGN KEY (evaluation_id, product_class)
        REFERENCES suitability.evaluation_outcome (evaluation_id, product_class)
);

-- SUIT-R35 / R37: each journey that relies on an evaluation appends a usage row.
CREATE TABLE suitability.suitability_usage (
    usage_id            CHAR(26)                 PRIMARY KEY,
    evaluation_id       CHAR(26)                 NOT NULL
        REFERENCES suitability.evaluation (evaluation_id),
    journey_id          CHAR(26)                 NOT NULL,
    product_class       VARCHAR(16)              NOT NULL,
    used_for            VARCHAR(24)              NOT NULL,              -- QUOTE_GATE | PROPOSAL | REUSE
    quote_id            CHAR(26),
    actor_id            VARCHAR(64)              NOT NULL,
    used_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_usage_class CHECK (product_class IN ('TERM', 'SAVINGS', 'ULIP')),
    CONSTRAINT ck_usage_for CHECK (used_for IN ('QUOTE_GATE', 'PROPOSAL', 'REUSE'))
);

CREATE INDEX ix_usage_evaluation ON suitability.suitability_usage (evaluation_id, used_at);
CREATE INDEX ix_usage_journey ON suitability.suitability_usage (journey_id);

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
    schema_version      SMALLINT                 NOT NULL DEFAULT 1,    -- ADR-025 §1
    payload             JSONB                    NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    published_at        TIMESTAMP WITH TIME ZONE,
    attempts            INTEGER                  NOT NULL DEFAULT 0,
    last_error          VARCHAR(1000),
    parked_at           TIMESTAMP WITH TIME ZONE                         -- ADR-025 §3
);

CREATE INDEX ix_suit_outbox_unpublished
    ON suitability.outbox_event (created_at)
    WHERE published_at IS NULL AND parked_at IS NULL;
