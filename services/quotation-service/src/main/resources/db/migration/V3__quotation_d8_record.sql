-- D8 / D-025 structured record (docs/platform/data-architecture/schemas/08-quotation.sql, 2026-10-21):
--   * quote: evaluation_id (D4), product_class (D4), selected_offer_id (D8), materialised event
--     id / time, retain_until; CONVERTED rows must name their selected offer.
--   * offer: INVALID offers are kept (D8 "invalid kept"), so premium_amount, sum_assured and
--     premium_frequency are nullable and ck_offer_positive applies to every other state only.
-- Portable between PostgreSQL and H2 (MODE=PostgreSQL).

ALTER TABLE quotation.quote ADD COLUMN evaluation_id CHAR(26);
-- The contract's suitabilityId is the evaluation id the gate used (quotation openapi.yaml).
UPDATE quotation.quote SET evaluation_id = suitability_id WHERE evaluation_id IS NULL;
ALTER TABLE quotation.quote ALTER COLUMN evaluation_id SET NOT NULL;

-- No backfill: a quote materialised before this migration has no recorded class, and inventing
-- one would falsify the record. SET NOT NULL fails loudly if such a row exists.
ALTER TABLE quotation.quote ADD COLUMN product_class VARCHAR(16);
ALTER TABLE quotation.quote ALTER COLUMN product_class SET NOT NULL;
ALTER TABLE quotation.quote ADD CONSTRAINT ck_quote_class
    CHECK (product_class IN ('TERM', 'SAVINGS', 'ULIP'));

ALTER TABLE quotation.quote ADD COLUMN selected_offer_id CHAR(26);
UPDATE quotation.quote q SET selected_offer_id = (
    SELECT o.offer_id FROM quotation.offer o
    WHERE o.quote_id = q.quote_id AND o.offer_state = 'SELECTED')
WHERE q.selected_offer_id IS NULL;
ALTER TABLE quotation.quote ADD CONSTRAINT ck_quote_converted_selected
    CHECK (state <> 'CONVERTED' OR selected_offer_id IS NOT NULL);

ALTER TABLE quotation.quote ADD COLUMN materialized_event_id CHAR(26);
ALTER TABLE quotation.quote ADD COLUMN materialized_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE quotation.quote ADD COLUMN retain_until DATE;

ALTER TABLE quotation.offer DROP CONSTRAINT ck_offer_positive;
ALTER TABLE quotation.offer ALTER COLUMN premium_amount DROP NOT NULL;
ALTER TABLE quotation.offer ALTER COLUMN premium_frequency DROP NOT NULL;
ALTER TABLE quotation.offer ALTER COLUMN sum_assured DROP NOT NULL;
ALTER TABLE quotation.offer ADD CONSTRAINT ck_offer_positive CHECK (
    offer_state = 'INVALID'
    OR (premium_amount > 0 AND sum_assured > 0 AND premium_frequency IS NOT NULL));
