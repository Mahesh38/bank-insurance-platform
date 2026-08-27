-- DESIGN DDL — OPEN-I3 / OPEN-I5 additive columns on audit_event.
-- Apply as the next bank-persistence-service Flyway version (suggested name
-- V3__audit_event_reconstruction_columns.sql). Do not edit V1.
--
-- H2 note: JSONB is not H2-safe. If CI still uses H2 for this module, store
-- prior_state / new_state as TEXT (same compromise as metadata) and switch
-- to JSONB when the module's tests move to PostgreSQL.

ALTER TABLE audit_event
    ADD COLUMN IF NOT EXISTS prior_state            TEXT,
    ADD COLUMN IF NOT EXISTS new_state              TEXT,
    ADD COLUMN IF NOT EXISTS consent_ref            VARCHAR(36),
    ADD COLUMN IF NOT EXISTS suitability_ref        VARCHAR(36),
    ADD COLUMN IF NOT EXISTS event_schema_version   SMALLINT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS sequence_no            INTEGER,
    ADD COLUMN IF NOT EXISTS acting_capacity        VARCHAR(20),
    ADD COLUMN IF NOT EXISTS actor_insurer_id       VARCHAR(64),
    ADD COLUMN IF NOT EXISTS assisted_actor_id      VARCHAR(64),
    ADD COLUMN IF NOT EXISTS config_version_ref     VARCHAR(64);

ALTER TABLE audit_event
    ADD CONSTRAINT ck_audit_capacity
        CHECK (acting_capacity IS NULL OR acting_capacity IN ('SP_ACCOUNTABLE', 'ASSIST_ONLY'));

-- Gap detection (information model §5). Multiple NULL journey_id rows remain allowed.
CREATE UNIQUE INDEX IF NOT EXISTS ux_audit_journey_sequence
    ON audit_event (journey_id, sequence_no)
    WHERE journey_id IS NOT NULL AND sequence_no IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_consent
    ON audit_event (consent_ref)
    WHERE consent_ref IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_suitability
    ON audit_event (suitability_ref)
    WHERE suitability_ref IS NOT NULL;

-- After apply: new writers populate sequence_no via bank_persistence.fn_next_audit_sequence
-- (90-routines.sql). Existing rows stay NULL; reconstruction of historic journeys remains OPEN-D8.
