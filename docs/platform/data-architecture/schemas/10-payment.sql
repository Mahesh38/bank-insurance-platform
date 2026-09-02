-- DESIGN DDL — Payment SoR. Apply at S09 in the owning Payment service Flyway.
-- No cardholder data. Attempts are append-only. RECONCILED is the issuance gate (INV-POL-01).

CREATE TABLE payment.payment (
    payment_id          CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL,
    journey_id          CHAR(26)                 NOT NULL,
    proposal_id         CHAR(26)                 NOT NULL,
    amount              NUMERIC(15, 2)           NOT NULL,
    currency            CHAR(3)                  NOT NULL DEFAULT 'INR',
    state               VARCHAR(28)              NOT NULL,
    device_channel      VARCHAR(20)              NOT NULL,
    link_issued_to_enc  BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    link_expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    pg_txn_id           VARCHAR(100),
    authorised_at       TIMESTAMP WITH TIME ZONE,
    captured_at         TIMESTAMP WITH TIME ZONE,
    settlement_ref      VARCHAR(100),
    reconciled_at       TIMESTAMP WITH TIME ZONE,
    lob                 VARCHAR(16)              NOT NULL,
    config_version_ref  VARCHAR(64)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version             BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_payment_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_payment_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_channel CHECK (device_channel IN ('SMS_LINK', 'EMAIL_LINK', 'QR_SCAN')),
    CONSTRAINT ck_payment_state CHECK (state IN (
        'INITIATED', 'LINK_ISSUED', 'AWAITING_AUTHORISATION', 'AUTHORISED',
        'CAPTURED', 'UNCERTAIN', 'RECONCILED', 'RECONCILIATION_BREAK',
        'REFUND_INITIATED', 'REFUNDED', 'DECLINED', 'EXPIRED', 'REJECTED'))
);

-- INV-PAY-02: at most one non-terminal payment per proposal.
CREATE UNIQUE INDEX ux_payment_active_per_proposal
    ON payment.payment (proposal_id)
    WHERE state NOT IN ('RECONCILED', 'REFUNDED', 'DECLINED', 'EXPIRED', 'REJECTED');

CREATE INDEX ix_payment_journey ON payment.payment (journey_id);
CREATE INDEX ix_payment_pg_txn ON payment.payment (pg_txn_id)
    WHERE pg_txn_id IS NOT NULL;

CREATE TABLE payment.payment_attempt (
    attempt_id          CHAR(26)                 PRIMARY KEY,
    payment_id          CHAR(26)                 NOT NULL REFERENCES payment.payment (payment_id),
    attempted_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    outcome             VARCHAR(24)              NOT NULL,
    pg_txn_id           VARCHAR(100),
    detail              VARCHAR(200)
);

CREATE INDEX ix_payment_attempt ON payment.payment_attempt (payment_id, attempted_at);

CREATE TABLE payment.refund (
    refund_id           CHAR(26)                 PRIMARY KEY,
    payment_id          CHAR(26)                 NOT NULL REFERENCES payment.payment (payment_id),
    amount              NUMERIC(15, 2)           NOT NULL,
    reason              VARCHAR(200)             NOT NULL,
    initiated_by        VARCHAR(64)              NOT NULL,
    approved_by         VARCHAR(64),
    decided_at          TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_refund_amount CHECK (amount > 0)
);

CREATE TABLE payment.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);
