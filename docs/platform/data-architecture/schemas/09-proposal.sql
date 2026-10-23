-- DESIGN DDL — Proposal & UW SoR. Apply at S09 in the owning Proposal service Flyway.
-- Form values containing PII live only inside encrypted payloads (INV-PRP-05).
--
-- DB-DEC-0005: pre-payment proposal state (form answers, nominee, UW requirements) lives in
-- the DynamoDB table {env}-proposal-working (see 20-dynamodb-working-payload.md). Structured
-- proposal / UW children below are INSERT-only after Payment RECONCILED. Journey never opens
-- this schema.
--
-- 2026-10-21 (D8 / D-025, READINESS-REVIEW §3.6 and §4, schemas/20 §6 rows 4 and 5):
--   * form_values_ref (NOT NULL, pointing at the frozen DynamoDB item that expires 30 days after
--     materialise) is REMOVED. Every question asked and every answer is materialised into
--     proposal.proposal_answer (one row per question, answer encrypted) — the durable record.
--   * Post-payment changes (CONVERTED, ISSUANCE_FAILED, REFUND_REQUIRED, …) are appended to
--     proposal.proposal_status_history; the proposal row stays INSERT-only and is inserted as PAID.
--     Current state = latest history row, else proposal.state.
--   * issuance_mode and config_version_ref are materialised from envelope control attributes.
--   * customer_id CHAR(26) ULID (D3).

-- Structured SoR — populated only by materialize after Payment RECONCILED.
CREATE TABLE proposal.proposal (
    proposal_id         CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL,
    journey_id          CHAR(26)                 NOT NULL,
    quote_id            CHAR(26)                 NOT NULL,
    offer_id            CHAR(26)                 NOT NULL,
    consent_id          CHAR(26)                 NOT NULL,
    customer_id         CHAR(26)                 NOT NULL,      -- D3: platform ULID
    application_number  VARCHAR(50),
    schema_id           VARCHAR(64)              NOT NULL,
    schema_version      VARCHAR(20)              NOT NULL,
    -- D8: form_values_ref removed; answers live in proposal.proposal_answer.
    state               VARCHAR(24)              NOT NULL,      -- state at materialise (PAID)
    issuance_mode       VARCHAR(24)              NOT NULL,      -- READINESS-REVIEW §3.6: had no column
    submitted_at        TIMESTAMP WITH TIME ZONE,               -- insurer receipt (PROPOSAL.TAT start candidate, D8)
    nominee_enc         BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    underwriting_state  VARCHAR(24),
    counter_offer       JSONB,
    lob                 VARCHAR(16)              NOT NULL,
    config_version_ref  VARCHAR(64)              NOT NULL,      -- now an envelope control attribute
    payment_id          CHAR(26),                               -- D8: the RECONCILED payment that materialised it
    materialized_event_id CHAR(26),                             -- PaymentReconciled eventId (ADR-025)
    materialized_at     TIMESTAMP WITH TIME ZONE,
    retain_until        DATE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version             BIGINT                   NOT NULL DEFAULT 0,
    -- INV-PRP-06 (01-domain-model-and-invariants.md): STP | NON_STP | INSTA.
    CONSTRAINT ck_proposal_issuance_mode CHECK (issuance_mode IN ('STP', 'NON_STP', 'INSTA')),
    CONSTRAINT ck_proposal_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_proposal_state CHECK (state IN (
        'DRAFT', 'SUBMITTED', 'SUBMISSION_FAILED', 'UNDER_WRITING',
        'REQUIREMENTS_PENDING', 'UW_APPROVED', 'UW_COUNTER_OFFER', 'UW_DECLINED',
        'CUSTOMER_DECLINED', 'AWAITING_PAYMENT', 'PAID', 'CONVERTED',
        'ISSUANCE_FAILED', 'REFUND_REQUIRED', 'WITHDRAWN', 'EXPIRED', 'ABANDONED'))
);

CREATE UNIQUE INDEX ux_proposal_application
    ON proposal.proposal (application_number)
    WHERE application_number IS NOT NULL;

CREATE INDEX ix_proposal_journey ON proposal.proposal (journey_id);
CREATE INDEX ix_proposal_lead ON proposal.proposal (lead_id, lob, state);

CREATE TABLE proposal.uw_requirement (
    requirement_id      CHAR(26)                 PRIMARY KEY,
    proposal_id         CHAR(26)                 NOT NULL REFERENCES proposal.proposal (proposal_id),
    requirement_type    VARCHAR(40)              NOT NULL,
    sub_type            VARCHAR(40),
    description         VARCHAR(500),
    status              VARCHAR(24)              NOT NULL,
    due_date            DATE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX ix_uw_req_proposal ON proposal.uw_requirement (proposal_id, status);

CREATE TABLE proposal.uw_document_ref (
    document_id         CHAR(26)                 PRIMARY KEY,
    proposal_id         CHAR(26)                 NOT NULL REFERENCES proposal.proposal (proposal_id),
    object_ref          VARCHAR(512)             NOT NULL,
    document_class      VARCHAR(40)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- D8: every question asked and every answer given, as shown. INSERT-only.
CREATE TABLE proposal.proposal_answer (
    proposal_id         CHAR(26)                 NOT NULL REFERENCES proposal.proposal (proposal_id),
    question_seq        INTEGER                  NOT NULL,      -- order shown
    question_id         VARCHAR(100)             NOT NULL,
    section_id          VARCHAR(100),
    question_text       TEXT                     NOT NULL,      -- as shown (not re-derived from schema)
    schema_version      VARCHAR(20)              NOT NULL,
    answer_enc          BYTEA                    NOT NULL,      -- ⚑ encrypted answer (INV-PRP-05)
    encryption_key_id   VARCHAR(50)              NOT NULL,
    answered_at         TIMESTAMP WITH TIME ZONE,
    answered_by         VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (proposal_id, question_id),
    CONSTRAINT ux_proposal_answer_seq UNIQUE (proposal_id, question_seq)
);

-- D8: post-payment proposal changes, append-only (never the frozen DynamoDB item, never an UPDATE).
CREATE TABLE proposal.proposal_status_history (
    status_id           CHAR(26)                 PRIMARY KEY,
    proposal_id         CHAR(26)                 NOT NULL REFERENCES proposal.proposal (proposal_id),
    from_state          VARCHAR(24),
    to_state            VARCHAR(24)              NOT NULL,
    reason_code         VARCHAR(64),
    source_event_id     CHAR(26),                               -- triggering event (dedupe)
    actor_id            VARCHAR(64)              NOT NULL,
    occurred_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_prop_hist_state CHECK (to_state IN (
        'PAID', 'CONVERTED', 'ISSUANCE_FAILED', 'REFUND_REQUIRED', 'WITHDRAWN', 'UW_DECLINED',
        'REQUIREMENTS_PENDING', 'UW_APPROVED', 'UW_COUNTER_OFFER'))
);

CREATE UNIQUE INDEX ux_prop_hist_event
    ON proposal.proposal_status_history (proposal_id, source_event_id)
    WHERE source_event_id IS NOT NULL;
CREATE INDEX ix_prop_hist_proposal ON proposal.proposal_status_history (proposal_id, occurred_at);

CREATE TABLE proposal.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);
