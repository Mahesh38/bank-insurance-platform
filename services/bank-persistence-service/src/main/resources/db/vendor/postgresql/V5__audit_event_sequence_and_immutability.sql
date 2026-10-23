-- Gap detection (information model §5): one event per (journey, sequence). Multiple NULLs remain
-- allowed. Design: schemas/14-audit_event_delta.sql ux_audit_journey_sequence.
CREATE UNIQUE INDEX ux_audit_journey_sequence
    ON audit_event (journey_id, sequence_no)
    WHERE journey_id IS NOT NULL AND sequence_no IS NOT NULL;

-- Immutability (schemas/90-routines.sql fn_prevent_update_delete). Defence in depth behind the
-- revoked UPDATE / DELETE grant: even the owner role cannot rewrite or remove evidence. Unqualified
-- names: Flyway runs in this service's schema (search_path), like V1–V4.
CREATE OR REPLACE FUNCTION fn_prevent_update_delete()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'IMMUTABLE_TABLE: % does not permit %', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER trg_audit_event_immutable
    BEFORE UPDATE OR DELETE ON audit_event
    FOR EACH ROW EXECUTE FUNCTION fn_prevent_update_delete();
