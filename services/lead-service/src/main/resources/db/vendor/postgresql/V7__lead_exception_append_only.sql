-- Exception evidence is append-only (D2 / ADR-024 §6, Exception BRD §19 audit): PostgreSQL refuses
-- UPDATE and DELETE on lead_exception_evaluation and lead_exception_decision whoever issues them.
-- H2 (local/test) has no equivalent without Java triggers; the application issues no such statement.
CREATE FUNCTION lead_lms.refuse_exception_evidence_change() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'lead_lms.% is append-only (%)', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'insufficient_privilege';
END;
$$;

CREATE TRIGGER tr_lead_exc_eval_append_only
    BEFORE UPDATE OR DELETE ON lead_lms.lead_exception_evaluation
    FOR EACH ROW EXECUTE FUNCTION lead_lms.refuse_exception_evidence_change();

CREATE TRIGGER tr_lead_exc_decision_append_only
    BEFORE UPDATE OR DELETE ON lead_lms.lead_exception_decision
    FOR EACH ROW EXECUTE FUNCTION lead_lms.refuse_exception_evidence_change();

-- Expiry sweep scan (LeadRepository.lockOverdue): only unfinished leads with a horizon.
CREATE INDEX ix_lead_overdue_open
    ON lead_lms.lead (expires_at)
    WHERE bi_generated = false
      AND state NOT IN ('DISQUALIFIED', 'EXPIRED', 'CONVERTED', 'ARCHIVED')
      AND expires_at IS NOT NULL;
