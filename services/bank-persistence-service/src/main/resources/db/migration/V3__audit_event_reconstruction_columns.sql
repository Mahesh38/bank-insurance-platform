-- CR-010 DB-C1: reconstruction columns on audit_event before any regulated journey writes.
-- Additive and nullable (except event_schema_version, which defaults). Do not edit V1.
-- H2 (MODE=PostgreSQL) local/test: TEXT rather than JSONB for prior_state / new_state.

ALTER TABLE audit_event ADD COLUMN prior_state TEXT;
ALTER TABLE audit_event ADD COLUMN new_state TEXT;
ALTER TABLE audit_event ADD COLUMN consent_ref VARCHAR(36);
ALTER TABLE audit_event ADD COLUMN suitability_ref VARCHAR(36);
ALTER TABLE audit_event ADD COLUMN event_schema_version SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE audit_event ADD COLUMN retain_until TIMESTAMP WITH TIME ZONE;
ALTER TABLE audit_event ADD COLUMN sequence_no INTEGER;
ALTER TABLE audit_event ADD COLUMN acting_capacity VARCHAR(20);
ALTER TABLE audit_event ADD COLUMN actor_insurer_id VARCHAR(64);
ALTER TABLE audit_event ADD COLUMN assisted_actor_id VARCHAR(64);
ALTER TABLE audit_event ADD COLUMN config_version_ref VARCHAR(64);

CREATE INDEX idx_audit_journey_sequence ON audit_event (journey_id, sequence_no);
CREATE INDEX idx_audit_consent ON audit_event (consent_ref);
CREATE INDEX idx_audit_suitability ON audit_event (suitability_ref);
CREATE INDEX idx_audit_retain_until ON audit_event (retain_until);
