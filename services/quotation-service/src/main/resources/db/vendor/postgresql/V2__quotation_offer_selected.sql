-- INV-QUO-05: at most one SELECTED offer per quote (schemas/08-quotation.sql).
CREATE UNIQUE INDEX ux_offer_selected
    ON quotation.offer (quote_id)
    WHERE offer_state = 'SELECTED';
