-- INV-ACT-04: acting capacity is one of the two designed values (schemas/14-audit_event_delta.sql
-- ck_audit_capacity). Portable; the partial unique and the immutability trigger are vendor-specific
-- and live in db/vendor/{postgresql,h2}/V5.
ALTER TABLE audit_event
    ADD CONSTRAINT ck_audit_capacity
        CHECK (acting_capacity IS NULL OR acting_capacity IN ('SP_ACCOUNTABLE', 'ASSIST_ONLY'));
