-- DESIGN DDL — Consent evidence SoR. Apply at S09 in the owning Consent service Flyway.
--
-- 2026-10-21 (READINESS-REVIEW §3.5; D5 / D-024; ADR-024; ADR-025):
--   * consent_record is APPEND-ONLY. No UPDATE, no DELETE (grant + trigger). Every state change
--     (REQUESTED → OTP_PENDING → GRANTED, WITHDRAWN, EXPIRED, ABANDONED) is a NEW row that points at
--     the row it replaces through supersedes_consent_id. The current consent for a chain is the row
--     nobody supersedes (ux_consent_superseded_once + "NOT EXISTS successor" in reads).
--     Withdrawal = a new WITHDRAWN row (INV-CNS-01). The previous table consent.consent is retired.
--   * REQUESTED / OTP_PENDING rows are insertable: otp_txn_id, captured_at, source_ip and
--     contact_used_enc are NULL until the consent is captured (ck_consent_captured).
--   * Purposes include CNS-QSH (D5: share quote inputs with the insurers quoted through 1SB).
--   * customer_id CHAR(26) ULID (D3 / D-023). The CIF is not stored here (removed cif_enc).
--   * Checkpoint, validity and purposes come from rule set CONSENT.POLICY; every row records the
--     rule-set id + version that decided it (ADR-024 §6). valid_until is computed from the
--     checkpoint's validityDays (seed 90 / 30 / null) — never a code constant.
--   * outbox_event with ADR-025 columns (consent emits ConsentGranted / ConsentWithdrawn /
--     ConsentExpired and had no outbox).
--   * Compliance (Shailja) must still confirm the D5 ordering under the DPDP Act — see D5 note.

CREATE TABLE consent.consent_record (
    consent_id              CHAR(26)                 PRIMARY KEY,
    supersedes_consent_id   CHAR(26)                 REFERENCES consent.consent_record (consent_id),
    chain_id                CHAR(26)                 NOT NULL,          -- consent_id of the first row of the chain
    customer_id             CHAR(26)                 NOT NULL,          -- D3: platform ULID
    lead_id                 CHAR(26),                                   -- context of capture, not a binding (bindTo = customerId)
    journey_id              CHAR(26),
    checkpoint              VARCHAR(32)              NOT NULL,          -- D5: CONSENT.POLICY checkpoint
    purpose                 TEXT[]                   NOT NULL,
    scope                   VARCHAR(20)              NOT NULL DEFAULT 'ALL_INSURERS',
    scope_insurer_id        VARCHAR(64),                                -- D5: BEFORE_PROPOSAL_SUBMIT is NAMED_INSURER
    statement_text          TEXT                     NOT NULL,
    statement_version       VARCHAR(20)              NOT NULL,
    language                VARCHAR(8)               NOT NULL DEFAULT 'en',
    channel                 VARCHAR(24)              NOT NULL,
    state                   VARCHAR(20)              NOT NULL,
    otp_txn_id              VARCHAR(64),                                -- NULL until captured
    captured_at             TIMESTAMP WITH TIME ZONE,                   -- NULL until captured
    source_ip               INET,
    contact_used_enc        BYTEA,
    encryption_key_id       VARCHAR(50),
    valid_until             TIMESTAMP WITH TIME ZONE,                   -- NULL = no expiry (validityDays null)
    withdrawal_reason       VARCHAR(200),
    rule_set_id             VARCHAR(64)              NOT NULL,          -- ADR-024: 'CONSENT.POLICY'
    rule_set_version        VARCHAR(20)              NOT NULL,
    lob                     VARCHAR(16)              NOT NULL,
    recorded_by             VARCHAR(64)              NOT NULL,          -- actor id (RM / customer / system)
    recorded_by_type        VARCHAR(32)              NOT NULL,
    retain_until            DATE,                                       -- evidence retention (RET-7Y class)
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_consent_state CHECK (state IN (
        'REQUESTED', 'OTP_PENDING', 'GRANTED', 'WITHDRAWN', 'EXPIRED', 'ABANDONED')),
    CONSTRAINT ck_consent_checkpoint CHECK (checkpoint IN (
        'BEFORE_SUITABILITY', 'BEFORE_MULTI_QUOTE', 'BEFORE_PROPOSAL_SUBMIT',
        'BEFORE_FIRST_COMMUNICATION')),
    -- D5: CNS-QSH added. Purpose codes are the consent rule pack's catalogue.
    CONSTRAINT ck_consent_purpose CHECK (
        cardinality(purpose) >= 1
        AND purpose <@ ARRAY['CNS-DP', 'CNS-SOL', 'CNS-QSH', 'CNS-SHR', 'CNS-COM']::TEXT[]),
    CONSTRAINT ck_consent_scope CHECK (
        (scope = 'ALL_INSURERS' AND scope_insurer_id IS NULL)
        OR (scope = 'NAMED_INSURER' AND scope_insurer_id IS NOT NULL)),
    CONSTRAINT ck_consent_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_consent_channel CHECK (channel IN ('RM_ASSISTED', 'SELF_SERVICE')),
    -- REQUESTED / OTP_PENDING / ABANDONED may lack capture evidence; GRANTED must carry all of it.
    CONSTRAINT ck_consent_captured CHECK (
        state <> 'GRANTED'
        OR (otp_txn_id IS NOT NULL AND captured_at IS NOT NULL AND source_ip IS NOT NULL
            AND contact_used_enc IS NOT NULL AND encryption_key_id IS NOT NULL)),
    CONSTRAINT ck_consent_contact_key CHECK (
        contact_used_enc IS NULL OR encryption_key_id IS NOT NULL),
    CONSTRAINT ck_consent_withdrawn CHECK (
        state <> 'WITHDRAWN' OR (supersedes_consent_id IS NOT NULL AND withdrawal_reason IS NOT NULL)),
    CONSTRAINT ck_consent_chain_root CHECK (
        (supersedes_consent_id IS NULL AND chain_id = consent_id)
        OR (supersedes_consent_id IS NOT NULL AND chain_id <> consent_id))
);

-- A row is superseded at most once: the chain is linear, so "current" is unambiguous.
CREATE UNIQUE INDEX ux_consent_superseded_once
    ON consent.consent_record (supersedes_consent_id)
    WHERE supersedes_consent_id IS NOT NULL;

-- D5: GET /internal/v1/consents:check?customerId&checkpoint — valid GRANTED rows per customer.
CREATE INDEX ix_consent_check
    ON consent.consent_record (customer_id, checkpoint, state, valid_until);
CREATE INDEX ix_consent_chain ON consent.consent_record (chain_id, created_at);
CREATE INDEX ix_consent_journey ON consent.consent_record (journey_id)
    WHERE journey_id IS NOT NULL;
CREATE INDEX ix_consent_lead ON consent.consent_record (lead_id)
    WHERE lead_id IS NOT NULL;

COMMENT ON TABLE consent.consent_record IS 'D-024 / INV-CNS-01: append-only consent evidence. A state change is a new row with supersedes_consent_id; UPDATE and DELETE are refused by trigger and grant.';
COMMENT ON COLUMN consent.consent_record.purpose IS 'Subset of CNS-DP, CNS-SOL, CNS-QSH (D5), CNS-SHR, CNS-COM. Which purposes a checkpoint needs is rule set CONSENT.POLICY.';
COMMENT ON COLUMN consent.consent_record.valid_until IS 'captured_at + CONSENT.POLICY checkpoints[checkpoint].validityDays; NULL when the rule set says null.';

CREATE TABLE consent.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_consent_idemp_expiry ON consent.idempotency_record (expires_at);

-- ADR-025 §3: transactional outbox. Payload = event-envelope/v1, ids and states only, no PII.
CREATE TABLE consent.outbox_event (
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

CREATE INDEX ix_consent_outbox_unpublished
    ON consent.outbox_event (created_at)
    WHERE published_at IS NULL AND parked_at IS NULL;
