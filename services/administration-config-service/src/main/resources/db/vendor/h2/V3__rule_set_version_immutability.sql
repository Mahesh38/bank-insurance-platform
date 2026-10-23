-- H2 counterpart of db/vendor/postgresql/V3. H2 triggers are Java classes, so the trigger is
-- omitted locally; JdbcRuleSetVersionRepository never updates a PUBLISHED row (WHERE status <>
-- 'PUBLISHED'), and PostgreSQL enforces it in the database.
COMMENT ON TABLE administration.rule_set_version IS 'Published rows are immutable (ADR-024)';
