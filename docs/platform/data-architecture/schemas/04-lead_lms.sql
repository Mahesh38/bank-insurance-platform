-- DESIGN DDL — Lead LMS SoR. Apply at S09 in the owning Lead service Flyway.
-- Schema: lead_lms. Aggregate table: lead. Identifiers stay lead_id (INV-LED-*).
-- Spoken name is Lead. ADR-014 D1 allowed keeping schema opportunity; owner
-- direction 2026-10-09 (SUG-20261009-lms) chose lead_lms / lead.
-- E2e column sheet: ../03-lead-suitability-e2e.md
-- Created by workforce BANK_RM or INSURER_PARTNER_REP (INV-LED-04 / D-018).
-- accountable_sp_id is written once at first completed assignment (INV-ACT-03 / D-019).
--
-- 2026-10-21 reconciliation with the as-built Flyway V2/V3 (services/lead-service/.../db/migration;
-- READINESS-REVIEW §3.5). Per-column verdicts and the V4 the code needs are in
-- ../03-lead-suitability-e2e.md §4.6. Summary:
--   * customer_id CHAR(26) ULID (D3 / D-023) — code is VARCHAR(64), V4 narrows it.
--   * assigned_rm_id — ADOPTED from code (R0 lead holds RM and SP separately).
--   * exception_state (NONE|BLOCKED|HOLD_FOR_APPROVAL|PASSED) — REPLACED by the code / D2 vocabulary
--     exception_outcome (NOT_EVALUATED|PASS|BLOCK|APPROVAL_REQUIRED) + exception_hold, plus the
--     append-only lead_exception_evaluation / lead_exception_decision evidence (D2, ADR-024 §6).
--   * source, created_by_actor_type, lead_generator_principal_id, config_versions, expires_at,
--     archived_at, accountable_sp_cert_ref, exception_ref, ux_lead_dedupe_open — KEPT (design);
--     V4 adds them (EXPIRED is unreachable without expires_at).
--   * outbox_event gains schema_version + parked_at (ADR-025 §3).

CREATE TABLE lead_lms.lead (
    lead_id                         CHAR(26)                 PRIMARY KEY,
    customer_id                     CHAR(26)                 NOT NULL,   -- D3 / D-023: platform ULID
    state                           VARCHAR(20)              NOT NULL,
    lob                             VARCHAR(16)              NOT NULL,
    product_class                   VARCHAR(16)              NOT NULL,
    source                          VARCHAR(16)              NOT NULL,
    created_by_actor_type           VARCHAR(32)              NOT NULL,
    created_by_principal_id         VARCHAR(64)              NOT NULL,
    lead_generator_principal_id     VARCHAR(64)              NOT NULL,
    assigned_rm_id                  VARCHAR(64),                         -- adopted from Flyway V2
    assigned_sp_id                  VARCHAR(64),
    assigned_ipr_id                 VARCHAR(64),
    fulfiller_principal_id          VARCHAR(64),
    accountable_sp_id               VARCHAR(64),
    accountable_sp_cert_ref         JSONB,
    branch_id                       VARCHAR(64),
    need_analysis_state             VARCHAR(20)              NOT NULL,
    insurer_id                      VARCHAR(64),
    partner_visible_from            TIMESTAMP WITH TIME ZONE,
    exception_outcome               VARCHAR(24)              NOT NULL DEFAULT 'NOT_EVALUATED', -- D2 (code vocabulary)
    exception_hold                  BOOLEAN                  NOT NULL DEFAULT false,           -- D2: APPROVAL_REQUIRED awaiting AUBIMA
    exception_evaluation_id         CHAR(26),                                                  -- D2: latest lead_exception_evaluation
    exception_ref                   VARCHAR(64),                                               -- AUBIMA case id
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
    CONSTRAINT ck_lead_state CHECK (state IN (
        'NEW', 'ASSIGNED', 'CONTACTED', 'QUALIFIED',
        'CONVERTED', 'DISQUALIFIED', 'EXPIRED', 'ARCHIVED')),
    CONSTRAINT ck_lead_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_lead_product CHECK (product_class IN ('TERM', 'SAVINGS', 'ULIP')),
    CONSTRAINT ck_lead_source CHECK (source IN ('RM', 'IPR')),
    CONSTRAINT ck_lead_actor CHECK (created_by_actor_type IN (
        'BANK_RM', 'INSURER_PARTNER_REP')),
    CONSTRAINT ck_lead_need CHECK (need_analysis_state IN (
        'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    -- D2: outcomeRank [BLOCK, APPROVAL_REQUIRED, PASS] of rule set LEAD.EXCEPTION (ADR-024).
    CONSTRAINT ck_lead_exception CHECK (exception_outcome IN (
        'NOT_EVALUATED', 'PASS', 'BLOCK', 'APPROVAL_REQUIRED')),
    -- D2 / BRD §4.2: a hold exists only for APPROVAL_REQUIRED; a later PASS must not clear it
    -- without a lead_exception_decision (application guard; this CHECK keeps the pair coherent).
    CONSTRAINT ck_lead_exception_hold CHECK (
        exception_hold = false OR exception_outcome = 'APPROVAL_REQUIRED'),
    CONSTRAINT ck_lead_reporting CHECK (reporting_class IN ('DIARY', 'ELIGIBLE')),
    CONSTRAINT ck_lead_eligible CHECK (
        reporting_class <> 'ELIGIBLE' OR bi_generated = true),
    CONSTRAINT ck_lead_qualified CHECK (
        state <> 'QUALIFIED' OR bi_generated = true),
    CONSTRAINT ck_lead_sp_when_progressing CHECK (
        state IN ('NEW', 'DISQUALIFIED', 'EXPIRED', 'ARCHIVED')
        OR accountable_sp_id IS NOT NULL),
    CONSTRAINT ck_lead_archived CHECK (
        (state <> 'ARCHIVED' AND archived_at IS NULL)
        OR (state = 'ARCHIVED' AND archived_at IS NOT NULL)),
    CONSTRAINT ck_lead_cert_with_sp CHECK (
        accountable_sp_id IS NULL OR accountable_sp_cert_ref IS NOT NULL),
    CONSTRAINT ck_lead_closed_remarks CHECK (
        closed_remarks_enc IS NULL OR closed_remarks_key_id IS NOT NULL)
);

COMMENT ON COLUMN lead_lms.lead.lead_id IS 'RET-7Y attribution (C-RET-1). Never RET-WORKING-LEAD.';
COMMENT ON COLUMN lead_lms.lead.accountable_sp_id IS 'RET-7Y. Written once at first completed assignment (INV-ACT-03).';
COMMENT ON COLUMN lead_lms.lead.converted_journey_id IS 'RET-7Y. At most one converting journey (INV-LED-02).';
COMMENT ON COLUMN lead_lms.lead.lob IS 'RET-7Y. Immutable after insert.';
COMMENT ON COLUMN lead_lms.lead.customer_id IS 'D-023: customer-service ULID. Never the CIF.';
COMMENT ON COLUMN lead_lms.lead.exception_outcome IS 'D2: winning outcome of rule set LEAD.EXCEPTION at the last evaluation. Missing threshold => BLOCK (EH-CONFIG-MISSING).';
COMMENT ON COLUMN lead_lms.lead.closed_remarks_enc IS 'RESTRICTED ⚑. Present when closure reason requires remarks.';

-- One journey may convert a lead (INV-LED-02). Partial unique allows many NULLs.
CREATE UNIQUE INDEX ux_lead_converted_journey
    ON lead_lms.lead (converted_journey_id)
    WHERE converted_journey_id IS NOT NULL;

-- Same creator + customer + product class, unfinished, no BI (Lead BRD Table 20).
CREATE UNIQUE INDEX ux_lead_dedupe_open
    ON lead_lms.lead (created_by_principal_id, customer_id, product_class)
    WHERE bi_generated = false
      AND state NOT IN ('DISQUALIFIED', 'EXPIRED', 'CONVERTED', 'ARCHIVED');

CREATE INDEX ix_lead_customer_lob ON lead_lms.lead (customer_id, lob);
CREATE INDEX ix_lead_assigned_rm ON lead_lms.lead (assigned_rm_id)            -- adopted from V2
    WHERE assigned_rm_id IS NOT NULL;
CREATE INDEX ix_lead_state_expires ON lead_lms.lead (state, expires_at);
CREATE INDEX ix_lead_inbox_sp
    ON lead_lms.lead (assigned_sp_id, state, updated_at)
    WHERE assigned_sp_id IS NOT NULL AND state <> 'ARCHIVED';
CREATE INDEX ix_lead_inbox_ipr
    ON lead_lms.lead (assigned_ipr_id, state, updated_at)
    WHERE assigned_ipr_id IS NOT NULL AND state <> 'ARCHIVED';
CREATE INDEX ix_lead_insurer_visible
    ON lead_lms.lead (insurer_id, partner_visible_from, need_analysis_state)
    WHERE insurer_id IS NOT NULL;
CREATE INDEX ix_lead_journey ON lead_lms.lead (journey_id)
    WHERE journey_id IS NOT NULL;

CREATE TABLE lead_lms.lead_assignment (
    assignment_id       CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL REFERENCES lead_lms.lead (lead_id),
    rm_id               VARCHAR(64)              NOT NULL,
    assigned_sp_id      VARCHAR(64),                                    -- adopted from Flyway V3
    assigned_ipr_id     VARCHAR(64),
    assigned_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    assigned_by         VARCHAR(64)              NOT NULL,
    reason              VARCHAR(200),
    assignment_kind     VARCHAR(20)              NOT NULL,
    CONSTRAINT ck_lead_assignment_kind CHECK (assignment_kind IN (
        'CREATE_ASSIGN', 'REASSIGN'))
);

CREATE INDEX ix_lead_assignment_lead ON lead_lms.lead_assignment (lead_id, assigned_at);

CREATE TABLE lead_lms.lead_meeting (
    meeting_id          CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL REFERENCES lead_lms.lead (lead_id),
    meeting_type        VARCHAR(16)              NOT NULL,
    scheduled_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    meeting_link_enc    BYTEA,
    encryption_key_id   VARCHAR(50),
    created_by          VARCHAR(64)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_lead_meeting_type CHECK (meeting_type IN ('ONLINE', 'IN_PERSON')),
    CONSTRAINT ck_lead_meeting_link CHECK (
        (meeting_type <> 'ONLINE' AND meeting_link_enc IS NULL)
        OR (meeting_type = 'ONLINE' AND meeting_link_enc IS NOT NULL
            AND encryption_key_id IS NOT NULL))
);

CREATE INDEX ix_lead_meeting_lead ON lead_lms.lead_meeting (lead_id, scheduled_at);

CREATE TABLE lead_lms.lead_follow_up (
    follow_up_id        CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL REFERENCES lead_lms.lead (lead_id),
    note_enc            BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    created_by          VARCHAR(64)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX ix_lead_follow_up_lead ON lead_lms.lead_follow_up (lead_id, created_at);

-- D2 / ADR-024 §6: every LEAD.EXCEPTION evaluation is evidence. Append-only.
-- checkpoint START_ONBOARDING (D-019) evaluates EH-BR-RULE-001…015; PREMIUM_CAPTURE re-runs 012–013.
CREATE TABLE lead_lms.lead_exception_evaluation (
    evaluation_id       CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL REFERENCES lead_lms.lead (lead_id),
    checkpoint          VARCHAR(24)              NOT NULL,
    rule_set_id         VARCHAR(64)              NOT NULL,              -- 'LEAD.EXCEPTION'
    rule_set_version    VARCHAR(20)              NOT NULL,
    engine_version      VARCHAR(20)              NOT NULL,
    inputs_hash         VARCHAR(71)              NOT NULL,              -- 'sha256:'+hex (ADR-024 §6)
    outcome             VARCHAR(24)              NOT NULL,
    reason_codes        TEXT[]                   NOT NULL DEFAULT '{}', -- in rule order (collectReasonCodes)
    fired_rules         JSONB                    NOT NULL,              -- engine trace: [{ruleId,outcome,reasonCode}]
    evaluated_by        VARCHAR(64)              NOT NULL,
    evaluated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_lead_exc_checkpoint CHECK (checkpoint IN ('START_ONBOARDING', 'PREMIUM_CAPTURE')),
    CONSTRAINT ck_lead_exc_outcome CHECK (outcome IN ('PASS', 'BLOCK', 'APPROVAL_REQUIRED'))
);

CREATE INDEX ix_lead_exc_eval_lead ON lead_lms.lead_exception_evaluation (lead_id, evaluated_at);

-- D2 / BRD §24.3 column J: approval or rejection of an APPROVAL_REQUIRED hold; remarks mandatory.
CREATE TABLE lead_lms.lead_exception_decision (
    decision_id         CHAR(26)                 PRIMARY KEY,
    evaluation_id       CHAR(26)                 NOT NULL
        REFERENCES lead_lms.lead_exception_evaluation (evaluation_id),
    lead_id             CHAR(26)                 NOT NULL REFERENCES lead_lms.lead (lead_id),
    decision            VARCHAR(16)              NOT NULL,
    exception_ref       VARCHAR(64),                                    -- AUBIMA case id
    remarks_enc         BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    decided_by          VARCHAR(64)              NOT NULL,
    decided_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_lead_exc_decision CHECK (decision IN ('APPROVED', 'REJECTED')),
    CONSTRAINT ux_lead_exc_decision_eval UNIQUE (evaluation_id)
);

CREATE TABLE lead_lms.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_lead_idemp_expiry ON lead_lms.idempotency_record (expires_at);

CREATE TABLE lead_lms.outbox_event (
    outbox_id           CHAR(26)                 PRIMARY KEY,
    aggregate_type      VARCHAR(64)              NOT NULL,
    aggregate_id        CHAR(26)                 NOT NULL,
    event_type          VARCHAR(96)              NOT NULL,
    schema_version      SMALLINT                 NOT NULL DEFAULT 1,    -- ADR-025 §1
    payload             JSONB                    NOT NULL,              -- event-envelope/v1; V3 uses VARCHAR(4000) for H2
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    published_at        TIMESTAMP WITH TIME ZONE,
    attempts            INTEGER                  NOT NULL DEFAULT 0,
    last_error          VARCHAR(1000),
    parked_at           TIMESTAMP WITH TIME ZONE                         -- ADR-025 §3
);

CREATE INDEX ix_lead_outbox_unpublished
    ON lead_lms.outbox_event (created_at)
    WHERE published_at IS NULL AND parked_at IS NULL;
