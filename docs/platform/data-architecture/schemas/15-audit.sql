-- DESIGN DDL — Business audit store. STATUS: **PROPOSED** (ADR-026, D7) — not accepted.
-- Becomes applicable only on the owner's explicit yes to ADR-026; Shailja (retention) and Deepali
-- (tamper evidence) review. Until then bank_persistence.audit_event (02 + 14) remains the audit
-- table and is NOT retired; this file must not be copied into any Flyway migration.
--
-- Owner if accepted: audit-compliance-service (schema audit). Writers never INSERT directly: every
-- service emits AuditRecorded (event-envelope/v1, ADR-025) through its outbox, and the audit
-- consumer inserts here. Idempotent consume: UNIQUE (event_id). Gap detection:
-- UNIQUE (journey_id, sequence_no). Tamper evidence: row_hash = sha256(prev_hash ‖ canonical(row)).
-- Daily WORM export to S3 Object Lock (compliance mode, RET-7Y / RET-POLICY+7Y) with the chain head.
-- Database-activity audit (pgaudit, Database Activity Streams) is separate and unchanged.

CREATE SCHEMA IF NOT EXISTS audit;   -- PROPOSED (ADR-026)
COMMENT ON SCHEMA audit IS 'PROPOSED (ADR-026): business audit evidence. INSERT-only, hash-chained, WORM-archived.';

CREATE TABLE audit.audit_event (
    audit_seq           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,  -- chain order
    event_id            CHAR(26)                 NOT NULL,      -- envelope eventId (ULID)
    event_type          VARCHAR(96)              NOT NULL DEFAULT 'AuditRecorded',
    schema_version      SMALLINT                 NOT NULL DEFAULT 1,
    source              VARCHAR(64)              NOT NULL,      -- producing service
    occurred_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    action              VARCHAR(100)             NOT NULL,
    resource_type       VARCHAR(50)              NOT NULL,
    resource_id         VARCHAR(100)             NOT NULL,
    outcome             VARCHAR(20)              NOT NULL,
    actor_id            VARCHAR(100)             NOT NULL,
    actor_type          VARCHAR(32)              NOT NULL,
    acting_capacity     VARCHAR(20),                            -- INV-ACT-04
    assisted_actor_id   VARCHAR(64),
    actor_insurer_id    VARCHAR(64),
    lob                 VARCHAR(16),
    journey_id          CHAR(26),
    lead_id             CHAR(26),
    customer_id         CHAR(26),                               -- D3 ULID; never CIF / PII
    sequence_no         INTEGER,                                -- per-journey sequence (INV-JRN-05)
    prior_state         JSONB,
    new_state           JSONB,
    config_version_ref  VARCHAR(64),                            -- rule-set version that decided (ADR-024)
    trace_id            VARCHAR(64),
    correlation_id      VARCHAR(64),
    prev_hash           CHAR(64)                 NOT NULL,      -- hex sha256 of previous row (genesis = 64 × '0')
    row_hash            CHAR(64)                 NOT NULL,      -- hex sha256(prev_hash ‖ canonical(row))
    retain_until        DATE                     NOT NULL,
    CONSTRAINT ux_audit_event_id UNIQUE (event_id),
    CONSTRAINT ck_audit_capacity CHECK (
        acting_capacity IS NULL OR acting_capacity IN ('SP_ACCOUNTABLE', 'ASSIST_ONLY')),
    CONSTRAINT ck_audit_lob CHECK (lob IS NULL OR lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ck_audit_seq_with_journey CHECK (sequence_no IS NULL OR journey_id IS NOT NULL)
);

CREATE UNIQUE INDEX ux_audit_journey_sequence
    ON audit.audit_event (journey_id, sequence_no)
    WHERE journey_id IS NOT NULL AND sequence_no IS NOT NULL;
CREATE INDEX ix_audit_resource ON audit.audit_event (resource_type, resource_id, occurred_at);
CREATE INDEX ix_audit_actor ON audit.audit_event (actor_id, occurred_at);
CREATE INDEX ix_audit_journey ON audit.audit_event (journey_id, occurred_at)
    WHERE journey_id IS NOT NULL;
CREATE INDEX ix_audit_customer ON audit.audit_event (customer_id, occurred_at)
    WHERE customer_id IS NOT NULL;

-- Chain head per daily WORM export: the archive is verifiable from the stored head.
CREATE TABLE audit.archive_export (
    export_id           CHAR(26)                 PRIMARY KEY,
    from_audit_seq      BIGINT                   NOT NULL,
    to_audit_seq        BIGINT                   NOT NULL,
    chain_head_hash     CHAR(64)                 NOT NULL,
    object_ref          VARCHAR(512)             NOT NULL,      -- S3 Object Lock key
    object_lock_until   DATE                     NOT NULL,
    exported_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_export_range CHECK (to_audit_seq >= from_audit_seq),
    CONSTRAINT ux_export_range UNIQUE (from_audit_seq, to_audit_seq)
);

-- ADR-026 §1: consumer dedupe is ux_audit_event_id itself (INSERT … ON CONFLICT (event_id) DO NOTHING).

-- Hash chain. The consumer is single-writer per partition; the trigger serialises with an
-- advisory lock so prev_hash is the hash of the row with the highest audit_seq.
CREATE OR REPLACE FUNCTION audit.fn_chain_hash()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_prev CHAR(64);
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('audit.audit_event.chain'));
    SELECT row_hash INTO v_prev FROM audit.audit_event ORDER BY audit_seq DESC LIMIT 1;
    NEW.prev_hash := COALESCE(v_prev, repeat('0', 64));
    NEW.row_hash := encode(sha256(convert_to(
        NEW.prev_hash || '|' || NEW.event_id || '|' || NEW.source || '|' ||
        to_char(NEW.occurred_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"') || '|' ||
        NEW.action || '|' || NEW.resource_type || '|' || NEW.resource_id || '|' ||
        NEW.outcome || '|' || NEW.actor_id || '|' || COALESCE(NEW.acting_capacity, '') || '|' ||
        COALESCE(NEW.journey_id, '') || '|' || COALESCE(NEW.sequence_no::text, '') || '|' ||
        COALESCE(NEW.prior_state::text, '') || '|' || COALESCE(NEW.new_state::text, '') || '|' ||
        COALESCE(NEW.config_version_ref, ''), 'UTF8')), 'hex');
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_audit_chain
    BEFORE INSERT ON audit.audit_event
    FOR EACH ROW EXECUTE FUNCTION audit.fn_chain_hash();

CREATE OR REPLACE FUNCTION audit.fn_prevent_update_delete()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'IMMUTABLE_TABLE: % does not permit %', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER trg_audit_event_immutable
    BEFORE UPDATE OR DELETE ON audit.audit_event
    FOR EACH ROW EXECUTE FUNCTION audit.fn_prevent_update_delete();
CREATE TRIGGER trg_audit_event_no_truncate
    BEFORE TRUNCATE ON audit.audit_event
    FOR EACH STATEMENT EXECUTE FUNCTION audit.fn_prevent_update_delete();
CREATE TRIGGER trg_archive_export_immutable
    BEFORE UPDATE OR DELETE ON audit.archive_export
    FOR EACH ROW EXECUTE FUNCTION audit.fn_prevent_update_delete();

-- Grants (PROPOSED): INSERT + SELECT only; see 91-grants.sql (app_audit section).
-- Migration (if accepted): copy bank_persistence.audit_event rows in event_time order through the
-- same chain trigger, verify counts and chain, then revoke INSERT on bank_persistence.audit_event.
-- bank_persistence keeps adapter evidence only (jobs, raw payloads).
