-- ux_audit_journey_sequence on H2, which has no partial indexes: a unique index over the two
-- columns gives the same guarantee, because H2 treats rows with any NULL key column as distinct.
--
-- The immutability trigger (fn_prevent_update_delete, PostgreSQL plpgsql) is deliberately omitted
-- here: H2 triggers are Java classes. Locally the insert-only controller is the guard; PostgreSQL
-- (db/vendor/postgresql/V5) enforces it in the database.
CREATE UNIQUE INDEX ux_audit_journey_sequence ON audit_event (journey_id, sequence_no);
