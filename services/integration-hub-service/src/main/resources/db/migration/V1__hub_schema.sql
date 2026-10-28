-- Hub LLD §7 (FUNC-032 / HUB-1) — provider_request + outbox_event.
-- TIMESTAMP WITH TIME ZONE / TEXT: H2 MODE=PostgreSQL compatible (same as bank-persistence).
CREATE SCHEMA IF NOT EXISTS hub;

CREATE TABLE hub.provider_request (
    provider_request_id  CHAR(26)     PRIMARY KEY,
    idempotency_key      VARCHAR(128) NOT NULL UNIQUE,
    request_hash         VARCHAR(64)  NOT NULL,
    operation            VARCHAR(32)  NOT NULL,
    lob                  VARCHAR(16)  NOT NULL,
    product_class        VARCHAR(16)  NOT NULL,
    mode                 VARCHAR(16)  NOT NULL,
    caller_context       VARCHAR(32)  NOT NULL,
    caller_aggregate_id  CHAR(26)     NOT NULL,
    journey_id           CHAR(26),
    provider             VARCHAR(32)  NOT NULL,
    routing_version      VARCHAR(64)  NOT NULL,
    adapter_job_id       VARCHAR(64),
    state                VARCHAR(24)  NOT NULL,
    failure_code         VARCHAR(64),
    failure_reason       VARCHAR(512),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE hub.outbox_event (
    outbox_id       CHAR(26)     PRIMARY KEY,
    aggregate_type  VARCHAR(64)  NOT NULL,
    aggregate_id    VARCHAR(64)  NOT NULL,
    event_type      VARCHAR(64)  NOT NULL,
    schema_version  VARCHAR(16)  NOT NULL,
    payload_json    TEXT         NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    published_at    TIMESTAMP WITH TIME ZONE
);
