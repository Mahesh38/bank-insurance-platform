-- LEAD.EXCEPTION evidence and the exception decision (D2, E6, ADR-024 §6; SUG-20261022-rdy).
-- From docs/platform/data-architecture/schemas/04-lead_lms.sql (lead_exception_evaluation,
-- lead_exception_decision, lead.exception_evaluation_id). Portable between PostgreSQL and H2 like
-- V3/V4: reason_codes, fired_rules, messages and evidence are JSON *text* instead of TEXT[] / JSONB.
-- Deviations from the design DDL, both additive: messages + evidence (the engine's canonical
-- result, so a decision is reproducible from the row alone) and decided_by_role (which configured
-- approverRoles entry the approver acted under, E6).
-- Append-only: the application never updates or deletes these rows; on PostgreSQL vendor V7 adds
-- triggers that refuse UPDATE and DELETE.

ALTER TABLE lead_lms.lead ADD COLUMN exception_evaluation_id CHAR(26);

CREATE TABLE lead_lms.lead_exception_evaluation (
    evaluation_id       CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL REFERENCES lead_lms.lead (lead_id),
    checkpoint          VARCHAR(24)              NOT NULL,
    rule_set_id         VARCHAR(64),
    rule_set_version    VARCHAR(20),
    engine_version      VARCHAR(20),
    inputs_hash         VARCHAR(71),
    outcome             VARCHAR(24)              NOT NULL,
    reason_codes        VARCHAR(4000)            NOT NULL,
    fired_rules         VARCHAR(32000)           NOT NULL,
    messages            VARCHAR(32000)           NOT NULL,
    evidence            VARCHAR(65000),
    evaluated_by        VARCHAR(64)              NOT NULL,
    evaluated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_lead_exc_checkpoint CHECK (checkpoint IN ('START_ONBOARDING', 'PREMIUM_CAPTURE')),
    CONSTRAINT ck_lead_exc_outcome CHECK (outcome IN ('PASS', 'BLOCK', 'APPROVAL_REQUIRED')),
    -- A real evaluation always names the rule-set version that decided (ADR-024 §6).
    CONSTRAINT ck_lead_exc_version CHECK (
        (rule_set_id IS NULL AND rule_set_version IS NULL)
        OR (rule_set_id IS NOT NULL AND rule_set_version IS NOT NULL
            AND engine_version IS NOT NULL AND inputs_hash IS NOT NULL))
);

CREATE INDEX ix_lead_exc_eval_lead ON lead_lms.lead_exception_evaluation (lead_id, evaluated_at);

CREATE TABLE lead_lms.lead_exception_decision (
    decision_id         CHAR(26)                 PRIMARY KEY,
    evaluation_id       CHAR(26)                 NOT NULL
        REFERENCES lead_lms.lead_exception_evaluation (evaluation_id),
    lead_id             CHAR(26)                 NOT NULL REFERENCES lead_lms.lead (lead_id),
    decision            VARCHAR(16)              NOT NULL,
    exception_ref       VARCHAR(64),
    remarks_enc         BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    decided_by          VARCHAR(64)              NOT NULL,
    decided_by_role     VARCHAR(64)              NOT NULL,
    decided_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_lead_exc_decision CHECK (decision IN ('APPROVED', 'REJECTED')),
    CONSTRAINT ux_lead_exc_decision_eval UNIQUE (evaluation_id)
);

CREATE INDEX ix_lead_exc_decision_lead ON lead_lms.lead_exception_decision (lead_id, decided_at);

-- Design ck_lead_exception_hold: a hold exists only for APPROVAL_REQUIRED.
ALTER TABLE lead_lms.lead ADD CONSTRAINT ck_lead_exception_hold
    CHECK (exception_hold = FALSE OR exception_outcome = 'APPROVAL_REQUIRED');
