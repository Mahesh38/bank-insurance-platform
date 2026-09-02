-- DESIGN DDL — Journey orchestration SoR. Apply at S09 in the owning Journey service Flyway.
-- Stage and references only. No eligibility, premium or UW decision columns (INV-JRN-02).

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
    party_snapshot_ref              CHAR(26)                 NOT NULL,
    next_action_due_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    compensation                    JSONB,
    config_version_ref              VARCHAR(64)              NOT NULL,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version                         BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT ck_journey_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
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

CREATE TABLE journey.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);
