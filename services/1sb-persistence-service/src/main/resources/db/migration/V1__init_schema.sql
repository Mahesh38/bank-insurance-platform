-- V1 — Initial schema for 1SB Integration Service
-- Compatible with PostgreSQL (production) and H2 in MODE=PostgreSQL (CI/test).
--
-- Tables:
--   integration_job          — primary job/correlation record per bank-initiated operation
--   integration_job_offer    — individual quote offers from a quote job
--   job_poll_attempt         — audit trail of each poll call made to 1SB
--   raw_payload              — encrypted store for full 1SB request/response bodies
--   audit_event              — immutable compliance audit log (INSERT-only for service account)
--   payment_session          — payment session records
--
-- H2 compatibility notes (Phase 1 CI):
--   TIMESTAMP WITH TIME ZONE is used instead of TIMESTAMPTZ (H2 compatible)
--   BYTEA is supported in H2 2.x PostgreSQL mode
--   BIGSERIAL is supported in H2 2.x PostgreSQL mode
--   now() is supported in H2 2.x PostgreSQL mode
--   metadata column uses TEXT instead of JSONB for H2 compatibility

-- ============================================================
-- integration_job
-- ============================================================
CREATE TABLE integration_job (
    job_id              VARCHAR(36)              PRIMARY KEY,
    job_type            VARCHAR(20)              NOT NULL,
    lob                 VARCHAR(20)              NOT NULL,
    status              VARCHAR(20)              NOT NULL,
    failure_reason      VARCHAR(100),
    journey_id          VARCHAR(36),
    application_number  VARCHAR(50),
    policy_number       VARCHAR(50),
    external_req_id     VARCHAR(100),
    external_provider   VARCHAR(20)              NOT NULL DEFAULT 'ONE_SB',
    idempotency_key     VARCHAR(128)             UNIQUE,
    result_blob_id      VARCHAR(36),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    completed_at        TIMESTAMP WITH TIME ZONE,
    owned_by_instance   VARCHAR(100),
    version             BIGINT                   NOT NULL DEFAULT 0,
    created_by_actor    VARCHAR(100)             NOT NULL
);

CREATE INDEX idx_job_journey     ON integration_job (journey_id);
CREATE INDEX idx_job_status      ON integration_job (status, created_at);
CREATE INDEX idx_job_application ON integration_job (application_number);

-- ============================================================
-- integration_job_offer
-- ============================================================
CREATE TABLE integration_job_offer (
    offer_id            VARCHAR(36)              PRIMARY KEY,
    job_id              VARCHAR(36)              NOT NULL REFERENCES integration_job (job_id),
    insurer_code        VARCHAR(50),
    product_code        VARCHAR(100),
    product_name        VARCHAR(200),
    premium_amount      NUMERIC(15, 2),
    premium_frequency   VARCHAR(10),
    sum_assured         NUMERIC(15, 2),
    out_of_bound        BOOLEAN                  DEFAULT FALSE,
    offer_status        VARCHAR(20),
    error_summary       VARCHAR(500),
    raw_offer_blob_id   VARCHAR(36),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_offer_job ON integration_job_offer (job_id);

-- ============================================================
-- job_poll_attempt
-- ============================================================
CREATE TABLE job_poll_attempt (
    attempt_id          BIGSERIAL                PRIMARY KEY,
    job_id              VARCHAR(36)              NOT NULL REFERENCES integration_job (job_id),
    attempt_number      SMALLINT                 NOT NULL,
    attempted_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    http_status         SMALLINT,
    is_complete         BOOLEAN,
    duration_ms         INTEGER,
    error_message       VARCHAR(500)
);

CREATE INDEX idx_poll_job ON job_poll_attempt (job_id, attempt_number);

-- ============================================================
-- raw_payload
-- Encrypted store for full 1SB request/response bodies (7-year retention)
-- ============================================================
CREATE TABLE raw_payload (
    payload_id          VARCHAR(36)              PRIMARY KEY,
    job_id              VARCHAR(36)              NOT NULL,
    direction           VARCHAR(3)               NOT NULL CHECK (direction IN ('REQ', 'RES')),
    operation           VARCHAR(100)             NOT NULL,
    lob                 VARCHAR(20)              NOT NULL,
    payload_enc         BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    http_status         SMALLINT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    retain_until        DATE                     NOT NULL
);

CREATE INDEX idx_payload_job ON raw_payload (job_id, direction, created_at);

-- ============================================================
-- audit_event
-- Immutable compliance audit log — service account has INSERT only
-- metadata uses TEXT for H2 compatibility (JSONB in real PostgreSQL)
-- ============================================================
CREATE TABLE audit_event (
    event_id            VARCHAR(36)              PRIMARY KEY,
    event_time          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    action              VARCHAR(100)             NOT NULL,
    actor_id            VARCHAR(100)             NOT NULL,
    actor_type          VARCHAR(20)              NOT NULL,
    resource_type       VARCHAR(50)              NOT NULL,
    resource_id         VARCHAR(100)             NOT NULL,
    outcome             VARCHAR(20)              NOT NULL,
    lob                 VARCHAR(20),
    journey_id          VARCHAR(36),
    distributor_id      VARCHAR(50),
    agent_id            VARCHAR(50),
    metadata            TEXT,
    trace_id            VARCHAR(64)
);

CREATE INDEX idx_audit_resource ON audit_event (resource_type, resource_id, event_time);
CREATE INDEX idx_audit_actor    ON audit_event (actor_id, event_time);
CREATE INDEX idx_audit_journey  ON audit_event (journey_id, event_time);

-- ============================================================
-- payment_session
-- ============================================================
CREATE TABLE payment_session (
    session_id          VARCHAR(36)              PRIMARY KEY,
    job_id              VARCHAR(36)              REFERENCES integration_job (job_id),
    application_number  VARCHAR(50)              NOT NULL,
    lob                 VARCHAR(20)              NOT NULL,
    payment_url         TEXT                     NOT NULL,
    redirect_url        TEXT                     NOT NULL,
    status              VARCHAR(30)              NOT NULL,
    external_txn_id     VARCHAR(100),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by_actor    VARCHAR(100)             NOT NULL
);
