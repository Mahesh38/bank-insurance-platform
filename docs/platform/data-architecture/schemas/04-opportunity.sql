-- DESIGN DDL — Opportunity (Lead) SoR. Apply at S09 in the owning Opportunity service Flyway.
-- Created only by BANK_RM (INV-LED-04). accountable_sp_id is immutable (INV-ACT-03).

CREATE TABLE opportunity.opportunity (
    lead_id                     CHAR(26)                 PRIMARY KEY,
    customer_id                 CHAR(26)                 NOT NULL,
    state                       VARCHAR(20)              NOT NULL,
    lob                         VARCHAR(16)              NOT NULL,
    product_class               VARCHAR(16),
    created_by_actor_type       VARCHAR(20)              NOT NULL,
    accountable_sp_id           VARCHAR(64)              NOT NULL,
    accountable_sp_cert_ref     JSONB                    NOT NULL,
    need_analysis_state         VARCHAR(20)              NOT NULL,
    insurer_id                  VARCHAR(64),
    partner_visible_from        TIMESTAMP WITH TIME ZONE,
    config_versions             JSONB                    NOT NULL,
    source                      VARCHAR(16)              NOT NULL,
    assigned_rm_id              VARCHAR(64),
    expires_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    converted_journey_id        CHAR(26),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version                     BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_opp_state CHECK (state IN (
        'NEW', 'ASSIGNED', 'CONTACTED', 'QUALIFIED',
        'CONVERTED', 'DISQUALIFIED', 'EXPIRED')),
    CONSTRAINT ck_opp_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_opp_actor CHECK (created_by_actor_type = 'BANK_RM'),
    CONSTRAINT ck_opp_source CHECK (source = 'RM'),
    CONSTRAINT ck_opp_need CHECK (need_analysis_state IN (
        'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED'))
);

-- One journey may convert a lead (INV-LED-02). Partial unique allows many NULLs.
CREATE UNIQUE INDEX ux_opp_converted_journey
    ON opportunity.opportunity (converted_journey_id)
    WHERE converted_journey_id IS NOT NULL;

CREATE INDEX ix_opp_customer_lob ON opportunity.opportunity (customer_id, lob);
CREATE INDEX ix_opp_state_expires ON opportunity.opportunity (state, expires_at);
CREATE INDEX ix_opp_insurer_visible
    ON opportunity.opportunity (insurer_id, partner_visible_from, need_analysis_state)
    WHERE insurer_id IS NOT NULL;

CREATE TABLE opportunity.opportunity_assignment (
    assignment_id       CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL REFERENCES opportunity.opportunity (lead_id),
    rm_id               VARCHAR(64)              NOT NULL,
    assigned_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    assigned_by         VARCHAR(64)              NOT NULL,
    reason              VARCHAR(200)
);

CREATE INDEX ix_opp_assignment_lead ON opportunity.opportunity_assignment (lead_id, assigned_at);

CREATE TABLE opportunity.opportunity_follow_up (
    follow_up_id        CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL REFERENCES opportunity.opportunity (lead_id),
    note_enc            BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    created_by          VARCHAR(64)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE opportunity.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_opp_idemp_expiry ON opportunity.idempotency_record (expires_at);
