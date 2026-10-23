-- Physical model for the consent contract (LLD §6). VARCHAR stands in for CHAR/JSONB/INET
-- so the same script runs on H2 (MODE=PostgreSQL) and PostgreSQL. Grants are V3, PostgreSQL only.
-- Evidence rows are insert-only in the application. The challenge is the only updated table.

CREATE SCHEMA IF NOT EXISTS consent;

CREATE TABLE consent.consent_record (
    consent_id               VARCHAR(26)  NOT NULL,
    record_version           INTEGER      NOT NULL,
    customer_id              VARCHAR(26)  NOT NULL,
    lead_id                  VARCHAR(26),
    journey_id               VARCHAR(26),
    lob                      VARCHAR(16)  NOT NULL,
    purpose                  VARCHAR(16)  NOT NULL,
    scope_insurer_code       VARCHAR(32),
    statement_id             VARCHAR(64)  NOT NULL,
    statement_version        VARCHAR(32)  NOT NULL,
    statement_text           VARCHAR(4000) NOT NULL,
    statement_text_hash      VARCHAR(64)  NOT NULL,
    language                 VARCHAR(8)   NOT NULL,
    channel                  VARCHAR(24)  NOT NULL,
    acknowledgement_method   VARCHAR(40)  NOT NULL,
    checkpoint               VARCHAR(48)  NOT NULL,
    state                    VARCHAR(20)  NOT NULL,
    captured_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    otp_verified_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    otp_txn_id               VARCHAR(64)  NOT NULL,
    source_ip                VARCHAR(64)  NOT NULL,
    contact_used_enc         BYTEA        NOT NULL,
    encryption_key_id        VARCHAR(50)  NOT NULL,
    captured_by_actor_id     VARCHAR(64)  NOT NULL,
    valid_from               TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until              TIMESTAMP WITH TIME ZONE,
    supersedes_consent_id    VARCHAR(26),
    withdrawn_at             TIMESTAMP WITH TIME ZONE,
    withdrawn_by             VARCHAR(64),
    withdrawal_channel       VARCHAR(24),
    withdrawal_reason_code   VARCHAR(40),
    withdrawal_otp_txn_id    VARCHAR(64),
    config_version_ref       VARCHAR(128) NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_consent_record PRIMARY KEY (consent_id, record_version),
    CONSTRAINT ck_consent_state CHECK (state IN ('GRANTED', 'WITHDRAWN', 'EXPIRED', 'SUPERSEDED')),
    CONSTRAINT ck_consent_purpose CHECK (purpose IN ('CNS-DP', 'CNS-SOL', 'CNS-QSH', 'CNS-SHR', 'CNS-COM')),
    CONSTRAINT ck_consent_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_consent_channel CHECK (channel IN ('RM_ASSISTED', 'SELF_SERVICE', 'HYBRID')),
    CONSTRAINT ck_consent_scope CHECK (
        (purpose <> 'CNS-SHR' AND scope_insurer_code IS NULL)
        OR (purpose = 'CNS-SHR' AND scope_insurer_code IS NOT NULL))
);

CREATE INDEX ix_consent_customer_purpose
    ON consent.consent_record (customer_id, purpose, scope_insurer_code, record_version);
CREATE INDEX ix_consent_journey ON consent.consent_record (journey_id);

CREATE TABLE consent.consent_challenge (
    challenge_id        VARCHAR(26)  NOT NULL PRIMARY KEY,
    kind                VARCHAR(16)  NOT NULL,
    state               VARCHAR(16)  NOT NULL,
    attempts            INTEGER      NOT NULL,
    otp_issue_count     INTEGER      NOT NULL,
    otp_expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    otp_txn_id          VARCHAR(64)  NOT NULL,
    otp_hash            VARCHAR(128) NOT NULL,
    purposes            VARCHAR(80)  NOT NULL,
    statement_refs      VARCHAR(4000) NOT NULL,
    contact_used_enc    BYTEA        NOT NULL,
    encryption_key_id   VARCHAR(50)  NOT NULL,
    customer_id         VARCHAR(26)  NOT NULL,
    journey_id          VARCHAR(26),
    lead_id             VARCHAR(26),
    lob                 VARCHAR(16)  NOT NULL,
    checkpoint          VARCHAR(48)  NOT NULL,
    insurer_code        VARCHAR(32),
    channel             VARCHAR(24)  NOT NULL,
    language            VARCHAR(8)   NOT NULL,
    target_consent_id   VARCHAR(26),
    actor_id            VARCHAR(64)  NOT NULL,
    config_version_ref  VARCHAR(128) NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_challenge_kind CHECK (kind IN ('CAPTURE', 'WITHDRAWAL')),
    CONSTRAINT ck_challenge_state CHECK (state IN ('OTP_PENDING', 'LOCKED', 'VERIFIED', 'ABANDONED'))
);

CREATE INDEX ix_challenge_customer ON consent.consent_challenge (customer_id, created_at);

CREATE TABLE consent.otp_issue (
    issue_id      VARCHAR(26) NOT NULL PRIMARY KEY,
    customer_id   VARCHAR(26) NOT NULL,
    purpose       VARCHAR(16) NOT NULL,
    issued_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_otp_issue_window ON consent.otp_issue (customer_id, purpose, issued_at);

CREATE TABLE consent.idempotency_record (
    idempotency_key  VARCHAR(128) NOT NULL PRIMARY KEY,
    request_hash     VARCHAR(64)  NOT NULL,
    http_status      INTEGER      NOT NULL,
    response_body    VARCHAR(8000) NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE consent.outbox_event (
    outbox_id        VARCHAR(26)  NOT NULL PRIMARY KEY,
    aggregate_type   VARCHAR(64)  NOT NULL,
    aggregate_id     VARCHAR(64)  NOT NULL,
    event_type       VARCHAR(96)  NOT NULL,
    schema_version   SMALLINT     NOT NULL DEFAULT 1,
    payload          VARCHAR(8000) NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at     TIMESTAMP WITH TIME ZONE,
    attempts         INTEGER      NOT NULL DEFAULT 0,
    last_error       VARCHAR(1000),
    parked_at        TIMESTAMP WITH TIME ZONE
);

CREATE TABLE consent.processed_event (
    event_id     VARCHAR(26) NOT NULL PRIMARY KEY,
    event_type   VARCHAR(96) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
