-- ADR-024 rule-set store and ADR-025 outbox for administration-config-service.
-- Portable between PostgreSQL and H2 (MODE=PostgreSQL). The PostgreSQL immutability trigger for
-- PUBLISHED rows is in db/vendor/postgresql/V3; the application enforces the same rule on H2.

CREATE SCHEMA IF NOT EXISTS administration;

CREATE TABLE administration.rule_set_version (
    id                   CHAR(26)                 PRIMARY KEY,
    rule_set_id          VARCHAR(100)             NOT NULL,
    version              VARCHAR(32)              NOT NULL,
    domain               VARCHAR(40)              NOT NULL,
    scope_lob            VARCHAR(16),
    scope_product_class  VARCHAR(32),
    classification       VARCHAR(16)              NOT NULL,
    status               VARCHAR(20)              NOT NULL,
    document             TEXT                     NOT NULL,
    document_sha256      CHAR(64)                 NOT NULL,
    effective_from       TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to         TIMESTAMP WITH TIME ZONE,
    maker                VARCHAR(64)              NOT NULL,
    checker              VARCHAR(64),
    approved_at          TIMESTAMP WITH TIME ZONE,
    decision_remarks     VARCHAR(1000),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_rule_set_version UNIQUE (rule_set_id, version),
    CONSTRAINT ck_rsv_status CHECK (status IN (
        'DRAFT', 'PENDING_APPROVAL', 'PUBLISHED', 'REJECTED', 'RETIRED')),
    CONSTRAINT ck_rsv_classification CHECK (classification IN (
        'COMPLIANCE', 'PRODUCT', 'OPERATIONAL')),
    -- Maker-checker is a database fact for every published row (ADR-024 section 5 point 2).
    CONSTRAINT ck_rsv_published_checked CHECK (status <> 'PUBLISHED' OR (
        checker IS NOT NULL AND approved_at IS NOT NULL AND checker <> maker)),
    CONSTRAINT ck_rsv_effective CHECK (effective_to IS NULL OR effective_to > effective_from)
);

-- Resolve: published versions of a domain.
CREATE INDEX ix_rsv_resolve ON administration.rule_set_version (domain, status, effective_from);

-- ADR-025 transactional outbox (ULID ids; parked after 10 failed relays, never dropped).
CREATE TABLE administration.outbox_event (
    outbox_id       CHAR(26)                 PRIMARY KEY,
    aggregate_type  VARCHAR(40)              NOT NULL,
    aggregate_id    VARCHAR(100)             NOT NULL,
    event_type      VARCHAR(80)              NOT NULL,
    payload         TEXT                     NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at    TIMESTAMP WITH TIME ZONE,
    attempts        INTEGER                  NOT NULL DEFAULT 0,
    last_error      VARCHAR(1000),
    parked_at       TIMESTAMP WITH TIME ZONE
);

CREATE INDEX ix_outbox_unpublished ON administration.outbox_event (published_at, created_at);
