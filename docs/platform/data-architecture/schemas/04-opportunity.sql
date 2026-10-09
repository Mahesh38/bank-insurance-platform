-- DESIGN DDL — Opportunity (Lead) SoR. Apply at S09 in the owning Lead / Opportunity service Flyway.
-- Spoken name is Lead. Schema stays opportunity (ADR-014 D1). Identifiers stay lead_id.
-- E2e column sheet: ../03-lead-suitability-e2e.md
-- Created by workforce BANK_RM or INSURER_PARTNER_REP (INV-LED-04 / D-018).
-- accountable_sp_id is written once at first completed assignment (INV-ACT-03 / D-019).

CREATE TABLE opportunity.opportunity (
    lead_id                         CHAR(26)                 PRIMARY KEY,
    customer_id                     CHAR(26)                 NOT NULL,
    state                           VARCHAR(20)              NOT NULL,
    lob                             VARCHAR(16)              NOT NULL,
    product_class                   VARCHAR(16)              NOT NULL,
    source                          VARCHAR(16)              NOT NULL,
    created_by_actor_type           VARCHAR(32)              NOT NULL,
    created_by_principal_id         VARCHAR(64)              NOT NULL,
    lead_generator_principal_id     VARCHAR(64)              NOT NULL,
    assigned_sp_id                  VARCHAR(64),
    assigned_ipr_id                 VARCHAR(64),
    fulfiller_principal_id          VARCHAR(64),
    accountable_sp_id               VARCHAR(64),
    accountable_sp_cert_ref         JSONB,
    branch_id                       VARCHAR(64),
    need_analysis_state             VARCHAR(20)              NOT NULL,
    insurer_id                      VARCHAR(64),
    partner_visible_from            TIMESTAMP WITH TIME ZONE,
    exception_state                 VARCHAR(24)              NOT NULL DEFAULT 'NONE',
    exception_ref                   VARCHAR(64),
    activity_status                 VARCHAR(40),
    bi_generated                    BOOLEAN                  NOT NULL DEFAULT false,
    bi_generated_at                 TIMESTAMP WITH TIME ZONE,
    reporting_class                 VARCHAR(16)              NOT NULL DEFAULT 'DIARY',
    journey_id                      CHAR(26),
    converted_journey_id            CHAR(26),
    closed_reason                   VARCHAR(40),
    closed_remarks_enc              BYTEA,
    closed_remarks_key_id           VARCHAR(50),
    config_versions                 JSONB                    NOT NULL,
    expires_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    archived_at                     TIMESTAMP WITH TIME ZONE,
    retain_until                    TIMESTAMP WITH TIME ZONE,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version                         BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_opp_state CHECK (state IN (
        'NEW', 'ASSIGNED', 'CONTACTED', 'QUALIFIED',
        'CONVERTED', 'DISQUALIFIED', 'EXPIRED', 'ARCHIVED')),
    CONSTRAINT ck_opp_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_opp_product CHECK (product_class IN ('TERM', 'SAVINGS', 'ULIP')),
    CONSTRAINT ck_opp_source CHECK (source IN ('RM', 'IPR')),
    CONSTRAINT ck_opp_actor CHECK (created_by_actor_type IN (
        'BANK_RM', 'INSURER_PARTNER_REP')),
    CONSTRAINT ck_opp_need CHECK (need_analysis_state IN (
        'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT ck_opp_exception CHECK (exception_state IN (
        'NONE', 'BLOCKED', 'HOLD_FOR_APPROVAL', 'PASSED')),
    CONSTRAINT ck_opp_reporting CHECK (reporting_class IN ('DIARY', 'ELIGIBLE')),
    CONSTRAINT ck_opp_eligible CHECK (
        reporting_class <> 'ELIGIBLE' OR bi_generated = true),
    CONSTRAINT ck_opp_qualified CHECK (
        state <> 'QUALIFIED' OR bi_generated = true),
    CONSTRAINT ck_opp_sp_when_progressing CHECK (
        state IN ('NEW', 'DISQUALIFIED', 'EXPIRED', 'ARCHIVED')
        OR accountable_sp_id IS NOT NULL),
    CONSTRAINT ck_opp_archived CHECK (
        (state <> 'ARCHIVED' AND archived_at IS NULL)
        OR (state = 'ARCHIVED' AND archived_at IS NOT NULL)),
    CONSTRAINT ck_opp_cert_with_sp CHECK (
        accountable_sp_id IS NULL OR accountable_sp_cert_ref IS NOT NULL),
    CONSTRAINT ck_opp_closed_remarks CHECK (
        closed_remarks_enc IS NULL OR closed_remarks_key_id IS NOT NULL)
);

COMMENT ON COLUMN opportunity.opportunity.lead_id IS 'RET-7Y attribution (C-RET-1). Never RET-WORKING-LEAD.';
COMMENT ON COLUMN opportunity.opportunity.accountable_sp_id IS 'RET-7Y. Written once at first completed assignment (INV-ACT-03).';
COMMENT ON COLUMN opportunity.opportunity.converted_journey_id IS 'RET-7Y. At most one converting journey (INV-LED-02).';
COMMENT ON COLUMN opportunity.opportunity.lob IS 'RET-7Y. Immutable after insert.';
COMMENT ON COLUMN opportunity.opportunity.closed_remarks_enc IS 'RESTRICTED ⚑. Present when closure reason requires remarks.';

-- One journey may convert a lead (INV-LED-02). Partial unique allows many NULLs.
CREATE UNIQUE INDEX ux_opp_converted_journey
    ON opportunity.opportunity (converted_journey_id)
    WHERE converted_journey_id IS NOT NULL;

-- Same creator + customer + product class, unfinished, no BI (Lead BRD Table 20).
CREATE UNIQUE INDEX ux_opp_dedupe_open
    ON opportunity.opportunity (created_by_principal_id, customer_id, product_class)
    WHERE bi_generated = false
      AND state NOT IN ('DISQUALIFIED', 'EXPIRED', 'CONVERTED', 'ARCHIVED');

CREATE INDEX ix_opp_customer_lob ON opportunity.opportunity (customer_id, lob);
CREATE INDEX ix_opp_state_expires ON opportunity.opportunity (state, expires_at);
CREATE INDEX ix_opp_inbox_sp
    ON opportunity.opportunity (assigned_sp_id, state, updated_at)
    WHERE assigned_sp_id IS NOT NULL AND state <> 'ARCHIVED';
CREATE INDEX ix_opp_inbox_ipr
    ON opportunity.opportunity (assigned_ipr_id, state, updated_at)
    WHERE assigned_ipr_id IS NOT NULL AND state <> 'ARCHIVED';
CREATE INDEX ix_opp_insurer_visible
    ON opportunity.opportunity (insurer_id, partner_visible_from, need_analysis_state)
    WHERE insurer_id IS NOT NULL;
CREATE INDEX ix_opp_journey ON opportunity.opportunity (journey_id)
    WHERE journey_id IS NOT NULL;

CREATE TABLE opportunity.opportunity_assignment (
    assignment_id       CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL REFERENCES opportunity.opportunity (lead_id),
    rm_id               VARCHAR(64)              NOT NULL,
    assigned_ipr_id     VARCHAR(64),
    assigned_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    assigned_by         VARCHAR(64)              NOT NULL,
    reason              VARCHAR(200),
    assignment_kind     VARCHAR(20)              NOT NULL,
    CONSTRAINT ck_opp_assignment_kind CHECK (assignment_kind IN (
        'CREATE_ASSIGN', 'REASSIGN'))
);

CREATE INDEX ix_opp_assignment_lead ON opportunity.opportunity_assignment (lead_id, assigned_at);

CREATE TABLE opportunity.opportunity_meeting (
    meeting_id          CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL REFERENCES opportunity.opportunity (lead_id),
    meeting_type        VARCHAR(16)              NOT NULL,
    scheduled_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    meeting_link_enc    BYTEA,
    encryption_key_id   VARCHAR(50),
    created_by          VARCHAR(64)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_opp_meeting_type CHECK (meeting_type IN ('ONLINE', 'IN_PERSON')),
    CONSTRAINT ck_opp_meeting_link CHECK (
        (meeting_type <> 'ONLINE' AND meeting_link_enc IS NULL)
        OR (meeting_type = 'ONLINE' AND meeting_link_enc IS NOT NULL
            AND encryption_key_id IS NOT NULL))
);

CREATE INDEX ix_opp_meeting_lead ON opportunity.opportunity_meeting (lead_id, scheduled_at);

CREATE TABLE opportunity.opportunity_follow_up (
    follow_up_id        CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL REFERENCES opportunity.opportunity (lead_id),
    note_enc            BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    created_by          VARCHAR(64)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX ix_opp_follow_up_lead ON opportunity.opportunity_follow_up (lead_id, created_at);

CREATE TABLE opportunity.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_opp_idemp_expiry ON opportunity.idempotency_record (expires_at);

CREATE TABLE opportunity.outbox_event (
    outbox_id           CHAR(26)                 PRIMARY KEY,
    aggregate_type      VARCHAR(64)              NOT NULL,
    aggregate_id        CHAR(26)                 NOT NULL,
    event_type          VARCHAR(96)              NOT NULL,
    payload             JSONB                    NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    published_at        TIMESTAMP WITH TIME ZONE,
    attempts            INTEGER                  NOT NULL DEFAULT 0,
    last_error          VARCHAR(1000)
);

CREATE INDEX ix_opp_outbox_unpublished
    ON opportunity.outbox_event (created_at)
    WHERE published_at IS NULL;
