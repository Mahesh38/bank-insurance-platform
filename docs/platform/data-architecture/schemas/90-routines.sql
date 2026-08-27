-- DESIGN DDL — required routines. Not a business write path (DR-SP-01).
-- Attach triggers in the owning service's Flyway at S09 (or with the audit delta).
-- Purge procedures are designed now and applied at S09-E06-S06.

-- ---------------------------------------------------------------------------
-- Immutability
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION bank_persistence.fn_prevent_update_delete()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'IMMUTABLE_TABLE: % does not permit %', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$;

-- Use on raw_payload and audit_event (and any later archive table):
-- CREATE TRIGGER trg_raw_payload_immutable
--     BEFORE UPDATE OR DELETE ON bank_persistence.raw_payload
--     FOR EACH ROW EXECUTE FUNCTION bank_persistence.fn_prevent_update_delete();
-- CREATE TRIGGER trg_audit_event_immutable
--     BEFORE UPDATE OR DELETE ON bank_persistence.audit_event
--     FOR EACH ROW EXECUTE FUNCTION bank_persistence.fn_prevent_update_delete();

CREATE OR REPLACE FUNCTION consent.fn_protect_consent_evidence()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'IMMUTABLE_TABLE: consent evidence cannot be deleted'
            USING ERRCODE = 'restrict_violation';
    END IF;
    IF NEW.statement_text IS DISTINCT FROM OLD.statement_text
       OR NEW.statement_version IS DISTINCT FROM OLD.statement_version
       OR NEW.otp_txn_id IS DISTINCT FROM OLD.otp_txn_id
       OR NEW.captured_at IS DISTINCT FROM OLD.captured_at
       OR NEW.source_ip IS DISTINCT FROM OLD.source_ip
       OR NEW.contact_used_enc IS DISTINCT FROM OLD.contact_used_enc
       OR NEW.cif_enc IS DISTINCT FROM OLD.cif_enc
       OR NEW.purpose IS DISTINCT FROM OLD.purpose THEN
        RAISE EXCEPTION 'IMMUTABLE_COLUMNS: consent evidence columns cannot change'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$;

-- CREATE TRIGGER trg_consent_evidence
--     BEFORE UPDATE OR DELETE ON consent.consent
--     FOR EACH ROW EXECUTE FUNCTION consent.fn_protect_consent_evidence();

CREATE OR REPLACE FUNCTION opportunity.fn_accountable_sp_immutable()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.accountable_sp_id IS DISTINCT FROM OLD.accountable_sp_id THEN
        RAISE EXCEPTION 'INV-ACT-03: accountable_sp_id is immutable'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$;

-- CREATE TRIGGER trg_accountable_sp_immutable
--     BEFORE UPDATE ON opportunity.opportunity
--     FOR EACH ROW EXECUTE FUNCTION opportunity.fn_accountable_sp_immutable();
-- Repeat the same function body in journey if that service copies the column.

-- ---------------------------------------------------------------------------
-- Audit sequence (OPEN-I3)
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION bank_persistence.fn_next_audit_sequence(p_journey_id VARCHAR)
RETURNS INTEGER
LANGUAGE sql
AS $$
    SELECT COALESCE(MAX(sequence_no), 0) + 1
      FROM bank_persistence.audit_event
     WHERE journey_id = p_journey_id;
$$;

-- Call inside the same transaction as the INSERT. The unique index
-- ux_audit_journey_sequence is the race guard; retry on unique_violation.

-- ---------------------------------------------------------------------------
-- IPR visibility predicate (AC-4 / PII-07) — documentation as SQL
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION opportunity.fn_ipr_visible(
    p_need_analysis_state VARCHAR,
    p_insurer_id VARCHAR,
    p_partner_visible_from TIMESTAMP WITH TIME ZONE,
    p_principal_insurer_id VARCHAR
) RETURNS BOOLEAN
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT p_need_analysis_state = 'COMPLETED'
       AND p_insurer_id IS NOT NULL
       AND p_partner_visible_from IS NOT NULL
       AND p_insurer_id = p_principal_insurer_id;
$$;

-- Repositories MUST include AND opportunity.fn_ipr_visible(...) when the
-- principal is INSURER_PARTNER_REP. A method without that predicate must not exist (FF-17).

-- ---------------------------------------------------------------------------
-- S09 purge — designed, not scheduled
-- ---------------------------------------------------------------------------

CREATE OR REPLACE PROCEDURE opportunity.sp_purge_operational()
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM opportunity.idempotency_record
     WHERE expires_at < now();
END;
$$;

CREATE OR REPLACE PROCEDURE bank_persistence.sp_retention_sweep()
LANGUAGE plpgsql
AS $$
BEGIN
    -- Returns nothing and deletes nothing on evidence tables.
    -- The owning job selects candidates and writes a disposal audit row (PII-06)
    -- only after Object Lock permits it. This body is a placeholder that lists
    -- operational rows only.
    DELETE FROM bank_persistence.job_poll_attempt
     WHERE attempted_at < now() - INTERVAL '90 days';
END;
$$;
