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
