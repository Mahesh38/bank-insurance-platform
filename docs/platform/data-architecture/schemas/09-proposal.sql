-- DESIGN DDL — Proposal & UW SoR. Apply at S09 in the owning Proposal service Flyway.
-- Form values are a reference to the encrypted payload store only (INV-PRP-05).

CREATE TABLE proposal.proposal (
    proposal_id         CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL,
    journey_id          CHAR(26)                 NOT NULL,
    quote_id            CHAR(26)                 NOT NULL,
    offer_id            CHAR(26)                 NOT NULL,
    consent_id          CHAR(26)                 NOT NULL,
    customer_id         CHAR(26)                 NOT NULL,
    application_number  VARCHAR(50),
    schema_id           VARCHAR(64)              NOT NULL,
    schema_version      VARCHAR(20)              NOT NULL,
    form_values_ref     VARCHAR(512)             NOT NULL,
    state               VARCHAR(24)              NOT NULL,
    nominee_enc         BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    underwriting_state  VARCHAR(24),
    counter_offer       JSONB,
    lob                 VARCHAR(16)              NOT NULL,
    config_version_ref  VARCHAR(64)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version             BIGINT                   NOT NULL DEFAULT 0,
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

CREATE TABLE proposal.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);
