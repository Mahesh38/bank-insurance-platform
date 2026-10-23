-- DESIGN DDL — Policy & Issuance SoR. Apply at S09 in the owning Policy service Flyway.
-- policy_number unique per insurer and immutable (INV-POL-02). Bytes live in S3.
--
-- 2026-10-21 (D8 / D-025, ADR-024 PROPOSAL.TAT, ADR-025, READINESS-REVIEW §3.5):
--   * The policy row is created when issuance tracking starts (after PaymentReconciled), so
--     policy_number and risk_commencement_date are NULL until the insurer issues
--     (ck_policy_issued_fields). Dates: issued_at, risk_commencement_date, maturity_date, closed_at.
--   * Insurer-decision TAT (IRDAI Master Circular 2024: 7 days — value lives in rule set
--     PROPOSAL.TAT.decisionDays, NOT here): decision_clock_started_at, decision_due_at,
--     tat_warning_at, tat_breached_at, plus the PROPOSAL.TAT version that computed them.
--     Shailja confirms the figure and the start event (proposal receipt vs payment).
--   * policy_status_history (append-only) records every 1SB status poll / callback change:
--     APPLICATION_RECEIVED, UNDER_REVIEW, REQUIREMENT_RAISED, ISSUED, DECLINED, REFUNDED.
--   * outbox_event (ADR-025) — policy emits PolicyIssued / ProposalDeclined and had no outbox.
--   * customer_id CHAR(26) ULID (D3); retain_until (RET-POLICY+7Y).

CREATE TABLE policy.policy (
    policy_id                   CHAR(26)                 PRIMARY KEY,
    lead_id                     CHAR(26),                               -- NULL only for OFF_PLATFORM (INV-POL-05)
    journey_id                  CHAR(26),
    customer_id                 CHAR(26)                 NOT NULL,      -- D3
    quote_id                    CHAR(26),                               -- D8
    proposal_id                 CHAR(26),
    payment_id                  CHAR(26),
    application_number          VARCHAR(50),                            -- D8: 1SB application number polled
    policy_number               VARCHAR(50),                            -- NULL until ISSUED
    insurer_code                VARCHAR(50)              NOT NULL,
    product_code                VARCHAR(100)             NOT NULL,
    sum_assured                 NUMERIC(15, 2)           NOT NULL,
    premium                     NUMERIC(15, 2)           NOT NULL,
    frequency                   VARCHAR(16)              NOT NULL,
    state                       VARCHAR(24)              NOT NULL,
    issued_at                   TIMESTAMP WITH TIME ZONE,               -- D8: issuance date
    risk_commencement_date      DATE,                                   -- D8: start date (NULL until ISSUED)
    maturity_date               DATE,                                   -- D8
    closed_at                   TIMESTAMP WITH TIME ZONE,               -- D8: closure (lapse / surrender / maturity / cancel)
    confirmed_at                TIMESTAMP WITH TIME ZONE,
    free_look_expires_at        TIMESTAMP WITH TIME ZONE,
    decision_clock_started_at   TIMESTAMP WITH TIME ZONE,               -- D8: PROPOSAL.TAT start event
    decision_due_at             TIMESTAMP WITH TIME ZONE,               -- D8: start + decisionDays
    tat_warning_at              TIMESTAMP WITH TIME ZONE,               -- D8: configured escalation point
    tat_breached_at             TIMESTAMP WITH TIME ZONE,               -- D8: set when due passes undecided
    tat_rule_set_version        VARCHAR(20),                            -- PROPOSAL.TAT version used
    source                      VARCHAR(16)              NOT NULL DEFAULT 'ON_PLATFORM',
    lob                         VARCHAR(16)              NOT NULL,
    product_class               VARCHAR(16),
    config_version_ref          VARCHAR(64)              NOT NULL,
    retain_until                DATE,                                   -- RET-POLICY+7Y
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version                     BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_policy_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_policy_source CHECK (source IN ('ON_PLATFORM', 'OFF_PLATFORM')),
    CONSTRAINT ck_policy_state CHECK (state IN (
        'PENDING_ISSUANCE', 'ISSUED', 'CONFIRMED', 'CONFIRMATION_OVERDUE',
        'ACTIVE', 'ISSUANCE_REJECTED', 'ISSUANCE_DISPUTED',
        'FREE_LOOK_CANCELLED', 'LAPSED', 'SURRENDERED', 'MATURED')),
    -- D8: once issued, number, issue time and start date are known.
    CONSTRAINT ck_policy_issued_fields CHECK (
        state IN ('PENDING_ISSUANCE', 'ISSUANCE_REJECTED')
        OR (policy_number IS NOT NULL AND issued_at IS NOT NULL
            AND risk_commencement_date IS NOT NULL)),
    CONSTRAINT ck_policy_closed CHECK (
        state NOT IN ('FREE_LOOK_CANCELLED', 'LAPSED', 'SURRENDERED', 'MATURED')
        OR closed_at IS NOT NULL),
    CONSTRAINT ck_policy_on_platform_refs CHECK (
        source = 'OFF_PLATFORM'
        OR (lead_id IS NOT NULL AND journey_id IS NOT NULL AND proposal_id IS NOT NULL
            AND payment_id IS NOT NULL)),
    CONSTRAINT ck_policy_tat CHECK (
        decision_due_at IS NULL OR decision_clock_started_at IS NOT NULL)
);

CREATE UNIQUE INDEX ux_policy_insurer_number
    ON policy.policy (insurer_code, policy_number)
    WHERE policy_number IS NOT NULL;
CREATE UNIQUE INDEX ux_policy_proposal
    ON policy.policy (proposal_id)
    WHERE proposal_id IS NOT NULL;
CREATE INDEX ix_policy_journey ON policy.policy (journey_id);
CREATE INDEX ix_policy_lead ON policy.policy (lead_id, lob);
-- D8: the poll / TAT scheduler scans undecided policies by due time.
CREATE INDEX ix_policy_tat_due
    ON policy.policy (decision_due_at)
    WHERE state = 'PENDING_ISSUANCE';

-- D8: append-only issuance status history. Never UPDATE the sold record; append a row.
CREATE TABLE policy.policy_status_history (
    status_id           CHAR(26)                 PRIMARY KEY,
    policy_id           CHAR(26)                 NOT NULL REFERENCES policy.policy (policy_id),
    status              VARCHAR(24)              NOT NULL,
    insurer_status_raw  VARCHAR(100),                               -- 1SB status code as received
    source              VARCHAR(16)              NOT NULL,          -- POLL (S-16) | CALLBACK | OPS
    source_event_id     CHAR(26),                                   -- dedupe for callbacks / events
    observed_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_by         VARCHAR(64)              NOT NULL,
    retain_until        DATE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_pol_hist_status CHECK (status IN (
        'APPLICATION_RECEIVED', 'UNDER_REVIEW', 'REQUIREMENT_RAISED',
        'ISSUED', 'DECLINED', 'REFUNDED')),
    CONSTRAINT ck_pol_hist_source CHECK (source IN ('POLL', 'CALLBACK', 'OPS'))
);

CREATE INDEX ix_pol_hist_policy ON policy.policy_status_history (policy_id, observed_at);
CREATE UNIQUE INDEX ux_pol_hist_event
    ON policy.policy_status_history (policy_id, source_event_id)
    WHERE source_event_id IS NOT NULL;

CREATE TABLE policy.policy_document_ref (
    document_id         CHAR(26)                 PRIMARY KEY,
    policy_id           CHAR(26)                 NOT NULL REFERENCES policy.policy (policy_id),
    object_ref          VARCHAR(512)             NOT NULL,
    document_type       VARCHAR(40)              NOT NULL,
    retain_until        DATE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE policy.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_policy_idemp_expiry ON policy.idempotency_record (expires_at);

-- ADR-025 §4: consumer dedupe by eventId (PaymentReconciled starts issuance tracking).
CREATE TABLE policy.processed_event (
    event_id            CHAR(26)                 PRIMARY KEY,
    consumer            VARCHAR(64)              NOT NULL,
    processed_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- ADR-025 §3.
CREATE TABLE policy.outbox_event (
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

CREATE INDEX ix_policy_outbox_unpublished
    ON policy.outbox_event (created_at)
    WHERE published_at IS NULL AND parked_at IS NULL;
