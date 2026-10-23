-- INV-QUO-05 on H2, which has no partial indexes: a generated column that is NULL unless the offer
-- is SELECTED, uniquely indexed (NULLs are distinct). Same guarantee as the PostgreSQL index.
ALTER TABLE quotation.offer ADD COLUMN selected_quote_id CHAR(26)
    GENERATED ALWAYS AS (CASE WHEN offer_state = 'SELECTED' THEN quote_id END);
CREATE UNIQUE INDEX ux_offer_selected ON quotation.offer (selected_quote_id);
