-- Unique insurer application number (schemas/09-proposal.sql).
CREATE UNIQUE INDEX ux_proposal_application
    ON proposal.proposal (application_number)
    WHERE application_number IS NOT NULL;
