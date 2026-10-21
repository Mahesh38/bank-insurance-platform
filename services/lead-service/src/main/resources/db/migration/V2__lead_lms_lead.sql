-- Lead working store for the R0 slice (gap fix SUG-20261021-gap; replaces the in-memory repository).
-- Runtime subset of docs/platform/data-architecture/schemas/04-lead_lms.sql: the columns the Lead
-- aggregate holds today. The remaining DBA-pack columns (activity status, meetings, archive and
-- retention fields, IPR visibility) arrive with the features that use them, as later migrations.
-- Portable between PostgreSQL and H2 (MODE=PostgreSQL): no partial indexes, no JSONB.

CREATE SCHEMA IF NOT EXISTS lead_lms;

CREATE TABLE lead_lms.lead (
    lead_id                  CHAR(26)                 PRIMARY KEY,
    customer_id              VARCHAR(64)              NOT NULL,
    lob                      VARCHAR(16)              NOT NULL,
    product_class            VARCHAR(16)              NOT NULL,
    branch_id                VARCHAR(64),
    created_by_principal_id  VARCHAR(64)              NOT NULL,
    state                    VARCHAR(20)              NOT NULL,
    assigned_rm_id           VARCHAR(64),
    assigned_sp_id           VARCHAR(64),
    accountable_sp_id        VARCHAR(64),
    exception_hold           BOOLEAN                  NOT NULL DEFAULT FALSE,
    exception_outcome        VARCHAR(24)              NOT NULL,
    bi_generated             BOOLEAN                  NOT NULL DEFAULT FALSE,
    reporting_class          VARCHAR(16)              NOT NULL,
    journey_id               CHAR(26),
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    version                  BIGINT                   NOT NULL,
    CONSTRAINT ck_lead_state CHECK (state IN (
        'NEW', 'ASSIGNED', 'CONTACTED', 'QUALIFIED',
        'CONVERTED', 'DISQUALIFIED', 'EXPIRED', 'ARCHIVED')),
    CONSTRAINT ck_lead_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_lead_product CHECK (product_class IN ('TERM', 'SAVINGS', 'ULIP')),
    CONSTRAINT ck_lead_exception CHECK (exception_outcome IN (
        'NOT_EVALUATED', 'PASS', 'BLOCK', 'APPROVAL_REQUIRED')),
    CONSTRAINT ck_lead_reporting CHECK (reporting_class IN ('DIARY', 'ELIGIBLE')),
    CONSTRAINT ck_lead_qualified CHECK (state <> 'QUALIFIED' OR bi_generated = TRUE),
    CONSTRAINT ck_lead_sp_when_progressing CHECK (
        state IN ('NEW', 'DISQUALIFIED', 'EXPIRED', 'ARCHIVED') OR accountable_sp_id IS NOT NULL)
);

-- Book queries (creator / assigned RM / assigned SP) and dedupe by creator + customer + class.
CREATE INDEX ix_lead_creator ON lead_lms.lead (created_by_principal_id, customer_id, product_class);
CREATE INDEX ix_lead_assigned_rm ON lead_lms.lead (assigned_rm_id);
CREATE INDEX ix_lead_assigned_sp ON lead_lms.lead (assigned_sp_id);
