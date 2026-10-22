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

-- Use on raw_payload and audit_event (and any later archive table). The PROPOSED audit schema
-- (ADR-026) has its own copy with the hash chain in 15-audit.sql:
-- CREATE TRIGGER trg_raw_payload_immutable
--     BEFORE UPDATE OR DELETE ON bank_persistence.raw_payload
--     FOR EACH ROW EXECUTE FUNCTION bank_persistence.fn_prevent_update_delete();
-- CREATE TRIGGER trg_audit_event_immutable
--     BEFORE UPDATE OR DELETE ON bank_persistence.audit_event
--     FOR EACH ROW EXECUTE FUNCTION bank_persistence.fn_prevent_update_delete();

-- D-024 / INV-CNS-01 (2026-10-21): consent_record is fully append-only — withdrawal is a new row,
-- so no column may change and the old column-level guard on consent.consent is retired.
CREATE OR REPLACE FUNCTION consent.fn_prevent_update_delete()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'IMMUTABLE_TABLE: consent.% does not permit %', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$;

-- CREATE TRIGGER trg_consent_record_immutable
--     BEFORE UPDATE OR DELETE ON consent.consent_record
--     FOR EACH ROW EXECUTE FUNCTION consent.fn_prevent_update_delete();

-- Append-only evidence added 2026-10-21 (D2, D4, D8). Each schema repeats the same body because
-- cross-schema function use is avoided (DR-OWN-01); attach in the owning service's Flyway:
--   lead_lms.lead_exception_evaluation, lead_lms.lead_exception_decision          (D2)
--   suitability.evaluation, evaluation_outcome, evaluation_status_event,
--   suitability.caution_ack, suitability.suitability_usage                       (D4, SUIT-R23/R37)
--   proposal.proposal_answer, proposal.proposal_status_history                   (D8)
--   policy.policy_status_history                                                 (D8)
-- e.g.
-- CREATE TRIGGER trg_suit_evaluation_immutable
--     BEFORE UPDATE OR DELETE ON suitability.evaluation
--     FOR EACH ROW EXECUTE FUNCTION suitability.fn_protect_answer_set();   -- same "refuse all" body

-- ADR-024 §5.1: a PUBLISHED rule-set document is immutable. Only status (PUBLISHED → SUPERSEDED /
-- WITHDRAWN) and effective_to (set once, by the newer version) may change after publish.
CREATE OR REPLACE FUNCTION administration.fn_protect_rule_set_version()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'IMMUTABLE_TABLE: rule_set_version cannot be deleted'
            USING ERRCODE = 'restrict_violation';
    END IF;
    IF OLD.status IN ('PUBLISHED', 'SUPERSEDED', 'WITHDRAWN') AND (
           NEW.document IS DISTINCT FROM OLD.document
        OR NEW.checksum IS DISTINCT FROM OLD.checksum
        OR NEW.version IS DISTINCT FROM OLD.version
        OR NEW.rule_set_id IS DISTINCT FROM OLD.rule_set_id
        OR NEW.effective_from IS DISTINCT FROM OLD.effective_from
        OR NEW.checker_id IS DISTINCT FROM OLD.checker_id
        OR (OLD.effective_to IS NOT NULL AND NEW.effective_to IS DISTINCT FROM OLD.effective_to)) THEN
        RAISE EXCEPTION 'ADR-024: published rule-set version %/% is immutable', OLD.rule_set_id, OLD.version
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$;

-- CREATE TRIGGER trg_rule_set_version_protect
--     BEFORE UPDATE OR DELETE ON administration.rule_set_version
--     FOR EACH ROW EXECUTE FUNCTION administration.fn_protect_rule_set_version();

CREATE OR REPLACE FUNCTION lead_lms.fn_accountable_sp_immutable()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    -- First assignment may write NULL → value. After that the column is frozen (INV-ACT-03 / D-019).
    IF OLD.accountable_sp_id IS NOT NULL
       AND NEW.accountable_sp_id IS DISTINCT FROM OLD.accountable_sp_id THEN
        RAISE EXCEPTION 'INV-ACT-03: accountable_sp_id is immutable'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$;

-- CREATE TRIGGER trg_accountable_sp_immutable
--     BEFORE UPDATE ON lead_lms.lead
--     FOR EACH ROW EXECUTE FUNCTION lead_lms.fn_accountable_sp_immutable();
-- Repeat the same function body in journey if that service copies the column.

CREATE OR REPLACE FUNCTION lead_lms.fn_lead_origination_immutable()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.lead_id IS DISTINCT FROM OLD.lead_id
       OR NEW.lob IS DISTINCT FROM OLD.lob
       OR NEW.product_class IS DISTINCT FROM OLD.product_class
       OR NEW.created_by_principal_id IS DISTINCT FROM OLD.created_by_principal_id
       OR NEW.created_by_actor_type IS DISTINCT FROM OLD.created_by_actor_type
       OR NEW.source IS DISTINCT FROM OLD.source THEN
        RAISE EXCEPTION 'IMMUTABLE_COLUMNS: lead origination columns cannot change'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$;

-- CREATE TRIGGER trg_lead_origination_immutable
--     BEFORE UPDATE ON lead_lms.lead
--     FOR EACH ROW EXECUTE FUNCTION lead_lms.fn_lead_origination_immutable();

CREATE OR REPLACE FUNCTION suitability.fn_protect_answer_set()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'IMMUTABLE_TABLE: suitability_answer_set does not permit %', TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$;

-- CREATE TRIGGER trg_suit_answer_set_immutable
--     BEFORE UPDATE OR DELETE ON suitability.suitability_answer_set
--     FOR EACH ROW EXECUTE FUNCTION suitability.fn_protect_answer_set();

CREATE OR REPLACE FUNCTION suitability.fn_protect_mapping_run()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'IMMUTABLE_TABLE: suitability_mapping_run cannot be deleted'
            USING ERRCODE = 'restrict_violation';
    END IF;
    IF NEW.suitability_id IS DISTINCT FROM OLD.suitability_id
       OR NEW.answer_set_version IS DISTINCT FROM OLD.answer_set_version
       OR NEW.mapping_rule_version IS DISTINCT FROM OLD.mapping_rule_version
       OR NEW.mapping_result IS DISTINCT FROM OLD.mapping_result
       OR NEW.recommended_products IS DISTINCT FROM OLD.recommended_products
       OR NEW.evaluated_at IS DISTINCT FROM OLD.evaluated_at
       OR NEW.evaluated_by IS DISTINCT FROM OLD.evaluated_by
       OR NEW.proposer_age_years IS DISTINCT FROM OLD.proposer_age_years THEN
        RAISE EXCEPTION 'IMMUTABLE_COLUMNS: mapping run evidence cannot change'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$;

-- CREATE TRIGGER trg_suit_mapping_run_protect
--     BEFORE UPDATE OR DELETE ON suitability.suitability_mapping_run
--     FOR EACH ROW EXECUTE FUNCTION suitability.fn_protect_mapping_run();
-- run_state may move CURRENT → INVALID / SUPERSEDED on a saved edit (Suitability BRD §14.2).

-- DB-DEC-0005: working quote/proposal state moved to DynamoDB; the freeze rule is a
-- ConditionExpression attribute_not_exists(materializedAt) on every update (see 20-dynamodb-working-payload.md).

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

CREATE OR REPLACE FUNCTION lead_lms.fn_ipr_visible(
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

-- Repositories MUST include AND lead_lms.fn_ipr_visible(...) when the
-- principal is INSURER_PARTNER_REP. A method without that predicate must not exist (FF-17).

-- ---------------------------------------------------------------------------
-- S09 purge — designed, not scheduled
-- ---------------------------------------------------------------------------

CREATE OR REPLACE PROCEDURE lead_lms.sp_purge_operational()
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM lead_lms.idempotency_record
     WHERE expires_at < now();
END;
$$;

CREATE OR REPLACE PROCEDURE suitability.sp_purge_operational()
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM suitability.idempotency_record
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

-- DB-DEC-0005: never-paid working state ages out by DynamoDB TTL, not here.
-- Structured quote / offer / proposal / UW stay.

CREATE OR REPLACE PROCEDURE quotation.sp_purge_operational()
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM quotation.idempotency_record
     WHERE expires_at < now();
END;
$$;

CREATE OR REPLACE PROCEDURE proposal.sp_purge_operational()
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM proposal.idempotency_record
     WHERE expires_at < now();
END;
$$;

CREATE OR REPLACE PROCEDURE journey.sp_purge_operational()
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM journey.idempotency_record
     WHERE expires_at < now();

    -- Unconverted only: never reached payment. Converted / paid journeys are
    -- RET-7Y and are not deleted here (SUG-20261019-txn).
    DELETE FROM journey.outbox_event o
     USING journey.journey j
     WHERE o.aggregate_id = j.journey_id   -- ADR-025 outbox shape
       AND j.archived_at IS NOT NULL
       AND j.retain_until IS NOT NULL
       AND j.retain_until < CURRENT_DATE
       AND NOT EXISTS (
           SELECT 1 FROM journey.journey_ref r
            WHERE r.journey_id = j.journey_id AND r.ref_type = 'paymentId');

    DELETE FROM journey.journey_transition t
     USING journey.journey j
     WHERE t.journey_id = j.journey_id
       AND j.archived_at IS NOT NULL
       AND j.retain_until IS NOT NULL
       AND j.retain_until < CURRENT_DATE
       AND NOT EXISTS (
           SELECT 1 FROM journey.journey_ref r
            WHERE r.journey_id = j.journey_id AND r.ref_type = 'paymentId');

    DELETE FROM journey.journey_ref r
     USING journey.journey j
     WHERE r.journey_id = j.journey_id
       AND j.archived_at IS NOT NULL
       AND j.retain_until IS NOT NULL
       AND j.retain_until < CURRENT_DATE
       AND NOT EXISTS (
           SELECT 1 FROM journey.journey_ref p
            WHERE p.journey_id = j.journey_id AND p.ref_type = 'paymentId');

    DELETE FROM journey.journey j
     WHERE j.archived_at IS NOT NULL
       AND j.retain_until IS NOT NULL
       AND j.retain_until < CURRENT_DATE
       AND NOT EXISTS (
           SELECT 1 FROM journey.journey_ref r
            WHERE r.journey_id = j.journey_id AND r.ref_type = 'paymentId');
END;
$$;

CREATE OR REPLACE PROCEDURE consent.sp_purge_operational()
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM consent.idempotency_record WHERE expires_at < now();
END;
$$;

CREATE OR REPLACE PROCEDURE policy.sp_purge_operational()
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM policy.idempotency_record WHERE expires_at < now();
END;
$$;

CREATE OR REPLACE PROCEDURE payment.sp_purge_operational()
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM payment.idempotency_record
     WHERE expires_at < now();
END;
$$;
