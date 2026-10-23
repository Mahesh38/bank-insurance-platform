-- D8 / D-025 structured record (docs/platform/data-architecture/schemas/09-proposal.sql, 2026-10-21):
--   * proposal: issuance_mode (INV-PRP-06), submitted_at, payment_id, materialised event id / time,
--     retain_until; form_values_ref RETIRED — it pointed at the frozen DynamoDB item, which
--     expires 30 days after materialise. Answers now live in proposal_answer.
--   * proposal_answer: every question asked, its text as shown, schema version, encrypted answer.
--   * proposal_status_history: post-payment changes, append-only; first row PAID at materialise.
-- Portable between PostgreSQL and H2 (MODE=PostgreSQL). The partial unique index on
-- (proposal_id, source_event_id) is vendor-specific: db/vendor/*/V5__proposal_history_event.sql.

-- No backfill: a proposal materialised before this migration has no recorded issuance mode, and
-- inventing one would falsify the record. SET NOT NULL fails loudly if such a row exists.
ALTER TABLE proposal.proposal ADD COLUMN issuance_mode VARCHAR(24);
ALTER TABLE proposal.proposal ALTER COLUMN issuance_mode SET NOT NULL;
ALTER TABLE proposal.proposal ADD CONSTRAINT ck_proposal_issuance_mode
    CHECK (issuance_mode IN ('STP', 'NON_STP', 'INSTA'));

ALTER TABLE proposal.proposal ADD COLUMN submitted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE proposal.proposal ADD COLUMN payment_id CHAR(26);
ALTER TABLE proposal.proposal ADD COLUMN materialized_event_id CHAR(26);
ALTER TABLE proposal.proposal ADD COLUMN materialized_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE proposal.proposal ADD COLUMN retain_until DATE;

ALTER TABLE proposal.proposal DROP COLUMN form_values_ref;

CREATE TABLE proposal.proposal_answer (
    proposal_id         CHAR(26)                 NOT NULL REFERENCES proposal.proposal (proposal_id),
    question_seq        INTEGER                  NOT NULL,
    question_id         VARCHAR(100)             NOT NULL,
    section_id          VARCHAR(100),
    question_text       TEXT                     NOT NULL,
    schema_version      VARCHAR(20)              NOT NULL,
    answer_enc          BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    answered_at         TIMESTAMP WITH TIME ZONE,
    answered_by         VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (proposal_id, question_id),
    CONSTRAINT ux_proposal_answer_seq UNIQUE (proposal_id, question_seq)
);

CREATE TABLE proposal.proposal_status_history (
    status_id           CHAR(26)                 PRIMARY KEY,
    proposal_id         CHAR(26)                 NOT NULL REFERENCES proposal.proposal (proposal_id),
    from_state          VARCHAR(24),
    to_state            VARCHAR(24)              NOT NULL,
    reason_code         VARCHAR(64),
    source_event_id     CHAR(26),
    actor_id            VARCHAR(64)              NOT NULL,
    occurred_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_prop_hist_state CHECK (to_state IN (
        'PAID', 'CONVERTED', 'ISSUANCE_FAILED', 'REFUND_REQUIRED', 'WITHDRAWN', 'UW_DECLINED',
        'REQUIREMENTS_PENDING', 'UW_APPROVED', 'UW_COUNTER_OFFER'))
);

CREATE INDEX ix_prop_hist_proposal ON proposal.proposal_status_history (proposal_id, occurred_at);
