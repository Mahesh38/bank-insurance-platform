-- ux_lead_dedupe_open on H2, which has no partial indexes: a generated column that is NULL unless
-- the lead is open (no BI, non-terminal), uniquely indexed (NULLs are distinct). Same guarantee as
-- the PostgreSQL partial index in db/vendor/postgresql/V5. Local and test only.
ALTER TABLE lead_lms.lead ADD COLUMN dedupe_open_key VARCHAR(160)
    GENERATED ALWAYS AS (CASE
        WHEN bi_generated = FALSE
         AND state NOT IN ('DISQUALIFIED', 'EXPIRED', 'CONVERTED', 'ARCHIVED')
        THEN created_by_principal_id || '|' || customer_id || '|' || product_class
    END);
CREATE UNIQUE INDEX ux_lead_dedupe_open ON lead_lms.lead (dedupe_open_key);
