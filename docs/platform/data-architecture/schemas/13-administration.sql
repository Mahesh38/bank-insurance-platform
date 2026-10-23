-- DESIGN DDL — Administration & Config SoR. Apply at S09 in the owning Administration service Flyway.
-- Versioned INSERT-only (INV-CFG-02, CF-3). A changed payload at the same version is an error.

CREATE TABLE administration.configuration_record (
    config_id           CHAR(26)                 PRIMARY KEY,
    domain              VARCHAR(40)              NOT NULL,
    lob                 VARCHAR(16)              NOT NULL,
    insurer_id          VARCHAR(64),
    product_code        VARCHAR(100),
    journey_type        VARCHAR(40),
    form_id             VARCHAR(64),
    field_id            VARCHAR(64),
    role_id             VARCHAR(80),
    actor_type          VARCHAR(40),
    version             INTEGER                  NOT NULL,
    payload             JSONB                    NOT NULL,
    effective_from      TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to        TIMESTAMP WITH TIME ZONE,
    status              VARCHAR(16)              NOT NULL,
    checksum            VARCHAR(64)              NOT NULL,
    seed_ref            VARCHAR(256),
    created_by          VARCHAR(64)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_config_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_config_status CHECK (status IN ('DRAFT', 'ACTIVE', 'SUPERSEDED', 'WITHDRAWN')),
    CONSTRAINT ck_config_version CHECK (version >= 1)
);

-- Seed idempotence: same (domain, partition, version, checksum) is a no-op at the app.
-- A different checksum at the same version must fail — unique on the partition+version.
CREATE UNIQUE INDEX ux_config_version
    ON administration.configuration_record (
        domain, lob,
        COALESCE(insurer_id, ''),
        COALESCE(product_code, ''),
        COALESCE(journey_type, ''),
        COALESCE(form_id, ''),
        COALESCE(field_id, ''),
        COALESCE(role_id, ''),
        COALESCE(actor_type, ''),
        version
    );

CREATE INDEX ix_config_resolve
    ON administration.configuration_record (domain, lob, status, effective_from, effective_to);

-- ---------------------------------------------------------------------------
-- ADR-024 (D1, owner decision 2026-10-21) — rule sets as versioned JSON configuration.
-- §5 lifecycle: DRAFT → PENDING_APPROVAL → PUBLISHED (immutable, SUIT-R07); reject → DRAFT;
-- a newer PUBLISHED version sets effective_to on the previous one (the only permitted UPDATE on a
-- published row besides status SUPERSEDED — enforced by trigger, see 90-routines.sql).
-- Maker ≠ checker; COMPLIANCE needs COMPLIANCE_APPROVER, PRODUCT needs PRODUCT_APPROVER,
-- OPERATIONAL needs CONFIG_APPROVER (role check in administration-config-service).
-- Publish is refused unless every testCase passed (test_run_* columns, RULE_SET_TEST_FAILED).
-- ---------------------------------------------------------------------------
CREATE TABLE administration.rule_set_version (
    rule_set_version_id CHAR(26)                 PRIMARY KEY,
    rule_set_id         VARCHAR(64)              NOT NULL,      -- e.g. SUITABILITY.LIFE.TERM, CONSENT.POLICY
    version             VARCHAR(20)              NOT NULL,      -- semver '1.0.0'
    format              VARCHAR(20)              NOT NULL DEFAULT 'rule-set/v1',
    domain              VARCHAR(40)              NOT NULL,      -- SUITABILITY | CONSENT | LEAD | PROPOSAL | HUB
    scope_lob           VARCHAR(16),
    scope_product_class VARCHAR(16),
    scope_insurer_id    VARCHAR(64),
    classification      VARCHAR(16)              NOT NULL,
    pinning             VARCHAR(16)              NOT NULL DEFAULT 'NONE',  -- ADR-024 §5.5
    document            JSONB                    NOT NULL,      -- the whole rule-set/v1 document
    checksum            VARCHAR(71)              NOT NULL,      -- 'sha256:'+hex of canonical document
    status              VARCHAR(20)              NOT NULL,
    effective_from      TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to        TIMESTAMP WITH TIME ZONE,
    engine_min_version  VARCHAR(20)              NOT NULL,      -- operators the document needs
    maker_id            VARCHAR(64)              NOT NULL,
    submitted_at        TIMESTAMP WITH TIME ZONE,
    checker_id          VARCHAR(64),
    checker_role        VARCHAR(32),
    decided_at          TIMESTAMP WITH TIME ZONE,
    decision_remarks    VARCHAR(1000),
    test_run_at         TIMESTAMP WITH TIME ZONE,
    test_run_passed     BOOLEAN,
    test_run_report     JSONB,                                  -- per testCase result
    seed_ref            VARCHAR(256),                           -- rule pack / BRD citation for seeds
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_rsv_status CHECK (status IN (
        'DRAFT', 'PENDING_APPROVAL', 'PUBLISHED', 'REJECTED', 'SUPERSEDED', 'WITHDRAWN')),
    CONSTRAINT ck_rsv_classification CHECK (classification IN ('COMPLIANCE', 'PRODUCT', 'OPERATIONAL')),
    CONSTRAINT ck_rsv_pinning CHECK (pinning IN ('NONE', 'JOURNEY')),
    CONSTRAINT ck_rsv_format CHECK (format = 'rule-set/v1'),
    CONSTRAINT ck_rsv_lob CHECK (scope_lob IS NULL OR scope_lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_rsv_effective CHECK (effective_to IS NULL OR effective_to > effective_from),
    -- §5.2: maker-checker.
    CONSTRAINT ck_rsv_checker CHECK (
        status NOT IN ('PUBLISHED', 'SUPERSEDED')
        OR (checker_id IS NOT NULL AND checker_id <> maker_id AND decided_at IS NOT NULL
            AND checker_role IS NOT NULL)),
    -- §5.3: publish refused unless every test case passed. IS TRUE, not = true: test_run_passed is
    -- nullable and a NULL CHECK result passes, so "= true" let a never-tested version publish
    -- (found by tests/10-invariants.sql on PostgreSQL 16, 2026-10-22).
    CONSTRAINT ck_rsv_tests CHECK (
        status NOT IN ('PUBLISHED', 'SUPERSEDED') OR test_run_passed IS TRUE),
    CONSTRAINT ck_rsv_rejected_remarks CHECK (
        status <> 'REJECTED' OR decision_remarks IS NOT NULL),
    CONSTRAINT ux_rsv_version UNIQUE (rule_set_id, version)
);

-- §5.4 resolve(domain, lob, productClass, asOf): the PUBLISHED version whose window covers asOf.
CREATE INDEX ix_rsv_resolve
    ON administration.rule_set_version
       (domain, scope_lob, scope_product_class, status, effective_from, effective_to);
-- At most one PUBLISHED version per rule set with an open window.
CREATE UNIQUE INDEX ux_rsv_open_published
    ON administration.rule_set_version (rule_set_id)
    WHERE status = 'PUBLISHED' AND effective_to IS NULL;

COMMENT ON TABLE administration.rule_set_version IS 'ADR-024: versioned, effective-dated rule-set documents. A PUBLISHED document is immutable; change = new version.';

CREATE TABLE administration.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ADR-025 §3: RuleSetPublished (cache invalidation, ADR-024 §5.4) and ConfigurationPublished.
CREATE TABLE administration.outbox_event (
    outbox_id           CHAR(26)                 PRIMARY KEY,
    aggregate_type      VARCHAR(64)              NOT NULL,
    aggregate_id        CHAR(26)                 NOT NULL,
    event_type          VARCHAR(96)              NOT NULL,
    schema_version      SMALLINT                 NOT NULL DEFAULT 1,
    payload             JSONB                    NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    published_at        TIMESTAMP WITH TIME ZONE,
    attempts            INTEGER                  NOT NULL DEFAULT 0,
    last_error          VARCHAR(1000),
    parked_at           TIMESTAMP WITH TIME ZONE
);

CREATE INDEX ix_admin_outbox_unpublished
    ON administration.outbox_event (created_at)
    WHERE published_at IS NULL AND parked_at IS NULL;
