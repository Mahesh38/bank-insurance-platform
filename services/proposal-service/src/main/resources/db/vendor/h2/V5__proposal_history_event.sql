-- H2 has no partial indexes; a plain unique index is equivalent because NULLs are distinct.
-- (The PostgreSQL append-only triggers have no H2 counterpart; H2 is for unit tests only.)
CREATE UNIQUE INDEX ux_prop_hist_event
    ON proposal.proposal_status_history (proposal_id, source_event_id);
