-- One history row per (proposal, triggering event): dedupe of re-delivered events (09-proposal.sql).
CREATE UNIQUE INDEX ux_prop_hist_event
    ON proposal.proposal_status_history (proposal_id, source_event_id)
    WHERE source_event_id IS NOT NULL;

-- Append-only (D8; 90-routines.sql "attach in the owning service's Flyway"): history and answers
-- are never updated or deleted. Grants (91-grants.sql) are the primary control; the trigger also
-- stops the owner role.
CREATE OR REPLACE FUNCTION proposal.fn_prevent_update_delete()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'IMMUTABLE_TABLE: proposal.% does not permit %', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER trg_proposal_status_history_immutable
    BEFORE UPDATE OR DELETE ON proposal.proposal_status_history
    FOR EACH ROW EXECUTE FUNCTION proposal.fn_prevent_update_delete();

CREATE TRIGGER trg_proposal_answer_immutable
    BEFORE UPDATE OR DELETE ON proposal.proposal_answer
    FOR EACH ROW EXECUTE FUNCTION proposal.fn_prevent_update_delete();
