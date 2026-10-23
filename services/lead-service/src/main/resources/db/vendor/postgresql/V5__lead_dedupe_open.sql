-- Same creator + customer + product class, unfinished, no BI (Lead BRD Table 20; design DDL
-- schemas/04-lead_lms.sql ux_lead_dedupe_open). Backs the application dedupe check against a
-- concurrent double create; a violation surfaces as 409 CONFLICT.
CREATE UNIQUE INDEX ux_lead_dedupe_open
    ON lead_lms.lead (created_by_principal_id, customer_id, product_class)
    WHERE bi_generated = false
      AND state NOT IN ('DISQUALIFIED', 'EXPIRED', 'CONVERTED', 'ARCHIVED');
