-- DESIGN DDL — Journey orchestration SoR. Apply at S09 in the owning Journey service Flyway.
-- Stage and references only. No eligibility, premium or UW decision columns (INV-JRN-02).
--
-- 2026-10-21: customer_id is the customer-service ULID (D3 / D-023). party_snapshot_ref is NULL
-- while the journey holds at INITIATED (READINESS-REVIEW §3.5) and required from NEED_ANALYSIS on.
-- outbox_event moved to the ADR-025 shape (outbox_id, aggregate_*, schema_version, parked_at).

CREATE TABLE journey.journey (
    journey_id                      CHAR(26)                 PRIMARY KEY,
    lead_id                         CHAR(26)                 NOT NULL,
    customer_id                     CHAR(26)                 NOT NULL,
    rm_id                           VARCHAR(64)              NOT NULL,
    accountable_sp_id               VARCHAR(64)              NOT NULL,
    stage                           VARCHAR(32)              NOT NULL,
    channel                         VARCHAR(20)              NOT NULL,
    lob                             VARCHAR(16)              NOT NULL,
    current_assisting_actor_id      VARCHAR(64),
    current_assisting_actor_type    VARCHAR(32),
    partner_insurer_id              VARCHAR(64),
    partner_visible_from            TIMESTAMP WITH TIME ZONE,
    party_snapshot_ref              CHAR(26),                              -- NULL only at INITIATED
    next_action_due_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    compensation                    JSONB,
    config_version_ref              VARCHAR(64)              NOT NULL,
    retain_until                    DATE,
    archived_at                     TIMESTAMP WITH TIME ZONE,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version                         BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_journey_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_journey_snapshot CHECK (
        stage IN ('INITIATED', 'ABANDONED') OR party_snapshot_ref IS NOT NULL),
    CONSTRAINT ck_journey_channel CHECK (channel IN ('RM_ASSISTED', 'SELF_SERVICE', 'HYBRID')),
    CONSTRAINT ck_journey_stage CHECK (stage IN (
        'INITIATED', 'NEED_ANALYSIS', 'SUITABILITY_COMPLETE', 'CONSENT_CAPTURED',
        'QUOTING', 'QUOTE_SELECTED', 'PROPOSAL_IN_PROGRESS', 'UNDERWRITING',
        'PAYMENT_PENDING', 'PAYMENT_SETTLED', 'ISSUANCE_PENDING', 'ISSUED',
        'SOLD', 'ABANDONED', 'DECLINED', 'COMPENSATING', 'COMPENSATED',
        'MANUAL_INTERVENTION'))
);

CREATE UNIQUE INDEX ux_journey_lead_open
    ON journey.journey (lead_id)
    WHERE stage NOT IN ('SOLD', 'ABANDONED', 'DECLINED', 'COMPENSATED');

CREATE INDEX ix_journey_lead ON journey.journey (lead_id);
CREATE INDEX ix_journey_stage_due ON journey.journey (stage, next_action_due_at);
CREATE INDEX ix_journey_partner
    ON journey.journey (partner_insurer_id, partner_visible_from)
    WHERE partner_insurer_id IS NOT NULL;
-- Unconverted journeys (no paymentId) leave OLTP after retain_until (DATA-005 / SUG-20261019-txn).
CREATE INDEX ix_journey_archive_due
    ON journey.journey (retain_until)
    WHERE archived_at IS NOT NULL;

-- External and domain refs as rows so INV-JRN-02 stays a schema assertion (no decision columns).
CREATE TABLE journey.journey_ref (
    journey_id          CHAR(26)                 NOT NULL REFERENCES journey.journey (journey_id),
    ref_type            VARCHAR(32)              NOT NULL,
    ref_value           VARCHAR(64)              NOT NULL,
    PRIMARY KEY (journey_id, ref_type),
    CONSTRAINT ck_journey_ref_type CHECK (ref_type IN (
        'suitabilityId', 'consentId', 'quoteId', 'offerId',
        'proposalId', 'paymentId', 'policyId',
        'applicationNumber', 'policyNumber', 'provider'))
);

-- Append-only stage history. Not a decision store (INV-JRN-02). DATA-005.
CREATE TABLE journey.journey_transition (
    transition_id       CHAR(26)                 PRIMARY KEY,
    journey_id          CHAR(26)                 NOT NULL REFERENCES journey.journey (journey_id),
    from_stage          VARCHAR(32)              NOT NULL,
    to_stage            VARCHAR(32)              NOT NULL,
    trigger_kind        VARCHAR(16)              NOT NULL,
    trigger_type        VARCHAR(64)              NOT NULL,
    actor_id            VARCHAR(64),
    occurred_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    correlation_id      VARCHAR(64),
    CONSTRAINT ck_jrn_trig CHECK (trigger_kind IN ('COMMAND', 'EVENT'))
);

CREATE INDEX ix_jrn_transition ON journey.journey_transition (journey_id, occurred_at);

-- ADR-025 §3 / §6 shape. aggregate_id = journey_id (aggregate_type 'Journey').
CREATE TABLE journey.outbox_event (
    outbox_id           CHAR(26)                 PRIMARY KEY,
    aggregate_type      VARCHAR(64)              NOT NULL DEFAULT 'Journey',
    aggregate_id        CHAR(26)                 NOT NULL REFERENCES journey.journey (journey_id),
    event_type          VARCHAR(96)              NOT NULL,
    schema_version      SMALLINT                 NOT NULL DEFAULT 1,
    payload             JSONB                    NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    published_at        TIMESTAMP WITH TIME ZONE,
    attempts            INTEGER                  NOT NULL DEFAULT 0,
    last_error          VARCHAR(1000),
    parked_at           TIMESTAMP WITH TIME ZONE
);

CREATE INDEX ix_jrn_outbox_unpublished
    ON journey.outbox_event (created_at)
    WHERE published_at IS NULL AND parked_at IS NULL;
CREATE INDEX ix_jrn_outbox_aggregate ON journey.outbox_event (aggregate_id);

-- ADR-025 §4: consumer dedupe by eventId (journey consumes most context events).
CREATE TABLE journey.processed_event (
    event_id            CHAR(26)                 PRIMARY KEY,
    consumer            VARCHAR(64)              NOT NULL,
    processed_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE journey.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);
