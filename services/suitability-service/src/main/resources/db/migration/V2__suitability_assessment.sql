CREATE SCHEMA IF NOT EXISTS suitability;

CREATE TABLE suitability.assessment (
    assessment_id         VARCHAR(26)   NOT NULL PRIMARY KEY,
    lead_id               VARCHAR(26)   NOT NULL,
    journey_id            VARCHAR(26)   NOT NULL,
    customer_id           VARCHAR(26)   NOT NULL,
    product_type          VARCHAR(32)   NOT NULL,
    proposer_name         VARCHAR(120)  NOT NULL,
    proposer_dob          DATE          NOT NULL,
    status                VARCHAR(40)   NOT NULL,
    last_screen           VARCHAR(40)   NOT NULL,
    encryption_key_id     VARCHAR(50)   NOT NULL,
    mobile_enc            BYTEA         NOT NULL,
    email_enc             BYTEA         NOT NULL,
    mobile_masked         VARCHAR(32)   NOT NULL,
    email_masked          VARCHAR(80)   NOT NULL,
    sp_actor_id           VARCHAR(64)   NOT NULL,
    rm_actor_id           VARCHAR(64)   NOT NULL,
    lock_actor_id         VARCHAR(64),
    lock_until            TIMESTAMP WITH TIME ZONE,
    premium_inr           BIGINT,
    premium_frequency     VARCHAR(16),
    premium_paying_term   VARCHAR(16),
    policy_term           VARCHAR(16),
    derivation_rule_id    VARCHAR(40),
    consent_state         VARCHAR(16)   NOT NULL,
    awaiting_consent      BOOLEAN       NOT NULL,
    mapping_version       VARCHAR(40),
    mapping_run_no        INTEGER       NOT NULL,
    show_products         BOOLEAN       NOT NULL,
    selected_product_id   VARCHAR(64),
    bi_reference          VARCHAR(64),
    response_version      INTEGER       NOT NULL,
    answers               VARCHAR(8000) NOT NULL,
    notice                VARCHAR(400),
    created_by            VARCHAR(64)   NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by            VARCHAR(64)   NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ux_suitability_assessment_lead UNIQUE (lead_id),
    CONSTRAINT ck_suitability_status CHECK (
        status IN (
            'NOT_STARTED',
            'IN_PROGRESS',
            'COMPLETED',
            'NO_SUITABLE_PRODUCT_FOUND',
            'COMPLETED_AND_LOCKED')),
    CONSTRAINT ck_suitability_product CHECK (product_type IN ('SAVINGS_PLAN', 'ULIP'))
);

CREATE TABLE suitability.answer_audit (
    audit_id       VARCHAR(26)   NOT NULL PRIMARY KEY,
    assessment_id  VARCHAR(26)   NOT NULL,
    screen         VARCHAR(40)   NOT NULL,
    answers        VARCHAR(8000) NOT NULL,
    actor_id       VARCHAR(64)   NOT NULL,
    recorded_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE suitability.mapping_run (
    run_id         VARCHAR(26)  NOT NULL PRIMARY KEY,
    assessment_id  VARCHAR(26)  NOT NULL,
    run_no         INTEGER      NOT NULL,
    rule_version   VARCHAR(40)  NOT NULL,
    outcome        VARCHAR(40)  NOT NULL,
    actor_id       VARCHAR(64)  NOT NULL,
    recorded_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE suitability.mapping_product (
    run_id        VARCHAR(26)  NOT NULL,
    product_id    VARCHAR(64)  NOT NULL,
    insurer_code  VARCHAR(32)  NOT NULL,
    product_name  VARCHAR(160) NOT NULL,
    product_type  VARCHAR(32)  NOT NULL,
    PRIMARY KEY (run_id, product_id)
);
