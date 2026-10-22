-- TEST HARNESS ONLY — not design DDL. Run by scripts/db/validate-postgres.sh before 91-grants.sql.
-- In every real environment these roles are created by IaC (91-grants.sql header, DR-MIG-04).
-- Roles are cluster-wide, so they are created NOLOGIN and only if missing; the validator never drops
-- them (another database on the same cluster may hold grants to them).
DO $$
DECLARE
    r TEXT;
BEGIN
    FOREACH r IN ARRAY ARRAY[
        'app_identity', 'app_bank_persistence', 'app_customer', 'app_lead_lms',
        'app_consent', 'app_suitability', 'app_catalogue', 'app_quotation',
        'app_proposal', 'app_payment', 'app_policy', 'app_journey', 'app_administration',
        'app_audit', 'job_retention', 'ro_breakglass']
    LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = r) THEN
            EXECUTE format('CREATE ROLE %I NOLOGIN', r);
        END IF;
    END LOOP;
END;
$$;
