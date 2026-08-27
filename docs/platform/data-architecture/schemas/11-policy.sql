-- DESIGN DDL — Policy & Issuance SoR. Apply at S09 in the owning Policy service Flyway.
-- policy_number unique per insurer and immutable (INV-POL-02). Bytes live in S3.

CREATE TABLE policy.policy (
    policy_id               CHAR(26)                 PRIMARY KEY,
    lead_id                 CHAR(26)                 NOT NULL,
    journey_id              CHAR(26)                 NOT NULL,
    customer_id             CHAR(26)                 NOT NULL,
    proposal_id             CHAR(26)                 NOT NULL,
    payment_id              CHAR(26)                 NOT NULL,
    policy_number           VARCHAR(50)              NOT NULL,
    insurer_code            VARCHAR(50)              NOT NULL,
    product_code            VARCHAR(100)             NOT NULL,
    sum_assured             NUMERIC(15, 2)           NOT NULL,
    premium                 NUMERIC(15, 2)           NOT NULL,
    frequency               VARCHAR(16)              NOT NULL,
    risk_commencement_date  DATE                     NOT NULL,
    maturity_date           DATE,
    state                   VARCHAR(24)              NOT NULL,
    issued_at               TIMESTAMP WITH TIME ZONE,
    confirmed_at            TIMESTAMP WITH TIME ZONE,
    free_look_expires_at    TIMESTAMP WITH TIME ZONE,
    lob                     VARCHAR(16)              NOT NULL,
    product_class           VARCHAR(16),
    config_version_ref      VARCHAR(64)              NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version                 BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_policy_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_policy_state CHECK (state IN (
        'PENDING_ISSUANCE', 'ISSUED', 'CONFIRMED', 'CONFIRMATION_OVERDUE',
        'ACTIVE', 'ISSUANCE_REJECTED', 'ISSUANCE_DISPUTED',
        'FREE_LOOK_CANCELLED', 'LAPSED', 'SURRENDERED', 'MATURED')),
    CONSTRAINT ux_policy_insurer_number UNIQUE (insurer_code, policy_number)
);

CREATE INDEX ix_policy_journey ON policy.policy (journey_id);
CREATE INDEX ix_policy_lead ON policy.policy (lead_id, lob);

CREATE TABLE policy.policy_document_ref (
    document_id         CHAR(26)                 PRIMARY KEY,
    policy_id           CHAR(26)                 NOT NULL REFERENCES policy.policy (policy_id),
    object_ref          VARCHAR(512)             NOT NULL,
    document_type       VARCHAR(40)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE policy.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);
