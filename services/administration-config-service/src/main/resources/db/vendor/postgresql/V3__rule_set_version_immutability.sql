-- SUIT-R07 / ADR-024 section 5 point 1: a PUBLISHED rule-set version is immutable. Enforced here
-- independently of the application, so a direct UPDATE or DELETE by any role fails.
CREATE OR REPLACE FUNCTION administration.fn_rule_set_version_immutable()
RETURNS trigger AS $$
BEGIN
    IF OLD.status = 'PUBLISHED' THEN
        RAISE EXCEPTION 'rule_set_version % (%@%) is PUBLISHED and immutable',
            OLD.id, OLD.rule_set_id, OLD.version;
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_rule_set_version_immutable
    BEFORE UPDATE OR DELETE ON administration.rule_set_version
    FOR EACH ROW EXECUTE FUNCTION administration.fn_rule_set_version_immutable();
