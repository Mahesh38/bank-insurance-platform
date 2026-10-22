-- Consent evidence, statements, OTP challenges, idempotency and retention.
-- This database is owned only by consent-service, so the tables are not schema-qualified.
-- Evidence is insert-only at the application boundary. OTP rows are operational and may advance.
-- BYTEA is used for ciphertext (H2 PostgreSQL mode and Aurora). VARCHAR lengths suit Hibernate validate.

CREATE TABLE published_statement (
    statement_id    VARCHAR(64)  NOT NULL,
    version         VARCHAR(16)  NOT NULL,
    language        VARCHAR(8)   NOT NULL,
    statement_text  VARCHAR(8000) NOT NULL,
    text_hash       VARCHAR(64)  NOT NULL,
    change_class    VARCHAR(8)   NOT NULL,
    classified_by   VARCHAR(64)  NOT NULL,
    published_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (statement_id, version, language)
);

CREATE TABLE consent_evidence (
    evidence_id              VARCHAR(26)  NOT NULL PRIMARY KEY,
    root_consent_id          VARCHAR(26)  NOT NULL,
    record_version           INTEGER      NOT NULL,
    supersedes_evidence_id   VARCHAR(26),
    customer_id              VARCHAR(64)  NOT NULL,
    consent_code             VARCHAR(16)  NOT NULL,
    recorded_state           VARCHAR(16)  NOT NULL,
    valid_from               TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until              TIMESTAMP WITH TIME ZONE,
    counterparty_id          VARCHAR(64),
    statement_id             VARCHAR(64)  NOT NULL,
    statement_version        VARCHAR(16)  NOT NULL,
    statement_text           VARCHAR(8000) NOT NULL,
    statement_text_hash      VARCHAR(64)  NOT NULL,
    language                 VARCHAR(8)   NOT NULL,
    cif_enc                  BYTEA        NOT NULL,
    encryption_key_id        VARCHAR(64)  NOT NULL,
    contact_ref_enc          BYTEA        NOT NULL,
    lead_id                  VARCHAR(64)  NOT NULL,
    journey_id               VARCHAR(64)  NOT NULL,
    channel                  VARCHAR(16)  NOT NULL,
    otp_transaction_id       VARCHAR(26)  NOT NULL,
    otp_verified_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    captured_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    source_ip                VARCHAR(64),
    captured_by_actor_id     VARCHAR(64)  NOT NULL,
    captured_by_agent_id     VARCHAR(64),
    distributor_id           VARCHAR(64)  NOT NULL,
    lob                      VARCHAR(16)  NOT NULL,
    config_version_ref       VARCHAR(64)  NOT NULL,
    withdrawal_reason_code   VARCHAR(64),
    ops_task_hint            VARCHAR(80),
    acknowledgement_method   VARCHAR(40)  NOT NULL,
    CONSTRAINT uq_consent_chain UNIQUE (root_consent_id, record_version),
    CONSTRAINT ck_consent_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_consent_recorded CHECK (recorded_state IN ('GRANTED', 'WITHDRAWN')),
    CONSTRAINT ck_consent_id_len CHECK (CHAR_LENGTH(evidence_id) = 26 AND CHAR_LENGTH(root_consent_id) = 26)
);

CREATE INDEX ix_consent_customer ON consent_evidence (customer_id, consent_code);
CREATE INDEX ix_consent_journey ON consent_evidence (journey_id);
CREATE INDEX ix_consent_root ON consent_evidence (root_consent_id, record_version);

CREATE TABLE otp_challenge (
    challenge_id             VARCHAR(26)  NOT NULL PRIMARY KEY,
    purpose                  VARCHAR(16)  NOT NULL,
    root_consent_id          VARCHAR(26)  NOT NULL,
    customer_id              VARCHAR(64)  NOT NULL,
    consent_code             VARCHAR(16)  NOT NULL,
    otp_hash                 VARCHAR(64)  NOT NULL,
    expires_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts                 INTEGER      NOT NULL,
    status                   VARCHAR(16)  NOT NULL,
    cif_enc                  BYTEA        NOT NULL,
    encryption_key_id        VARCHAR(64)  NOT NULL,
    contact_ref_enc          BYTEA        NOT NULL,
    lead_id                  VARCHAR(64),
    journey_id               VARCHAR(64),
    statement_id             VARCHAR(64)  NOT NULL,
    statement_version        VARCHAR(16)  NOT NULL,
    statement_text           VARCHAR(8000) NOT NULL,
    statement_text_hash      VARCHAR(64)  NOT NULL,
    language                 VARCHAR(8)   NOT NULL,
    channel                  VARCHAR(16)  NOT NULL,
    captured_by_actor_id     VARCHAR(64),
    captured_by_agent_id     VARCHAR(64),
    distributor_id           VARCHAR(64)  NOT NULL,
    counterparty_id          VARCHAR(64),
    lob                      VARCHAR(16)  NOT NULL,
    config_version_ref       VARCHAR(64),
    withdrawal_reason_code   VARCHAR(64),
    proposal_submitted       BOOLEAN      NOT NULL,
    source_ip                VARCHAR(64),
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_otp_issue ON otp_challenge (customer_id, consent_code, created_at);

CREATE TABLE idempotency_record (
    idempotency_key VARCHAR(200) NOT NULL PRIMARY KEY,
    request_hash    VARCHAR(64)  NOT NULL,
    http_status     INTEGER      NOT NULL,
    response_body   VARCHAR(8000) NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE retention_anchor (
    root_consent_id VARCHAR(26) NOT NULL PRIMARY KEY,
    terminal_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    retain_until    TIMESTAMP WITH TIME ZONE NOT NULL,
    anchored_at     TIMESTAMP WITH TIME ZONE NOT NULL
);
