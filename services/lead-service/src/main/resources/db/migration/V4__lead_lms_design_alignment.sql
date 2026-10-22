-- Aligns lead_lms.lead with the design DDL (docs/platform/data-architecture/schemas/04-lead_lms.sql)
-- where that is safe now (READINESS-REVIEW §3.5). Portable between PostgreSQL and H2: the partial
-- unique ux_lead_dedupe_open lives in db/vendor/{postgresql,h2}/V5.
--
-- Columns are NULLable here although the design says NOT NULL: no code path yet knows source /
-- actor type / generator / config versions / certificate reference, and inventing values would put
-- false attribution on RET-7Y rows. Each becomes NOT NULL in the migration that starts writing it.
--
-- config_versions and accountable_sp_cert_ref are JSON *text* (VARCHAR), not JSONB, so the same
-- migration runs on H2; the switch to JSONB is a PostgreSQL-only migration once H2 is retired for
-- migration tests (READINESS-REVIEW §3.5, Testcontainers).
--
-- customer_id stays VARCHAR(64) for now. D3 / D-023 (DECISIONS-2026-10-21): it becomes CHAR(26)
-- (platform ULID) once customer-service mints customerIds on first resolve.

ALTER TABLE lead_lms.lead ADD COLUMN source VARCHAR(16);
ALTER TABLE lead_lms.lead ADD COLUMN created_by_actor_type VARCHAR(32);
ALTER TABLE lead_lms.lead ADD COLUMN lead_generator_principal_id VARCHAR(64);
ALTER TABLE lead_lms.lead ADD COLUMN config_versions VARCHAR(4000);
-- Ageing horizon. Set at create from lead.expiry-days; NULL while that is unset because no source
-- states the value (BRD-P0-CAPABILITIES "expiry = configurable — confirm TTL"; OPEN-D4).
ALTER TABLE lead_lms.lead ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE lead_lms.lead ADD COLUMN accountable_sp_cert_ref VARCHAR(4000);
ALTER TABLE lead_lms.lead ADD COLUMN exception_ref VARCHAR(64);

ALTER TABLE lead_lms.lead ADD CONSTRAINT ck_lead_source
    CHECK (source IS NULL OR source IN ('RM', 'IPR'));
ALTER TABLE lead_lms.lead ADD CONSTRAINT ck_lead_actor
    CHECK (created_by_actor_type IS NULL
        OR created_by_actor_type IN ('BANK_RM', 'INSURER_PARTNER_REP'));

-- Ageing job scan (design ix_lead_state_expires).
CREATE INDEX ix_lead_state_expires ON lead_lms.lead (state, expires_at);
