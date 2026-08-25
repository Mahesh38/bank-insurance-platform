-- DESIGN DDL — Consent evidence SoR. Apply at S09 in the owning Consent service Flyway.
-- Evidence columns are write-once (INV-CNS-01). Withdrawal updates state columns only.

CREATE TABLE consent.consent (
    consent_id          CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL,
    journey_id          CHAR(26),
    customer_id         CHAR(26)                 NOT NULL,
    cif_enc             BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    purpose             TEXT[]                   NOT NULL,
    statement_text      TEXT                     NOT NULL,
    statement_version   VARCHAR(20)              NOT NULL,
    channel             VARCHAR(24)              NOT NULL,
    otp_txn_id          VARCHAR(64)              NOT NULL,
    captured_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    source_ip           INET                     NOT NULL,
    contact_used_enc    BYTEA                    NOT NULL,
    state               VARCHAR(20)              NOT NULL,
    withdrawn_at        TIMESTAMP WITH TIME ZONE,
    withdrawn_by        VARCHAR(64),
    withdrawal_reason   VARCHAR(200),
    valid_until         TIMESTAMP WITH TIME ZONE NOT NULL,
    config_version_ref  VARCHAR(64)              NOT NULL,
    lob                 VARCHAR(16)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_consent_state CHECK (state IN (
        'REQUESTED', 'OTP_PENDING', 'GRANTED', 'WITHDRAWN', 'EXPIRED', 'ABANDONED')),
    CONSTRAINT ck_consent_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_consent_channel CHECK (channel IN ('RM_ASSISTED', 'SELF_SERVICE'))
);

CREATE INDEX ix_consent_customer_state ON consent.consent (customer_id, state, valid_until);
CREATE INDEX ix_consent_journey ON consent.consent (journey_id);
CREATE INDEX ix_consent_lead ON consent.consent (lead_id);

CREATE TABLE consent.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);
