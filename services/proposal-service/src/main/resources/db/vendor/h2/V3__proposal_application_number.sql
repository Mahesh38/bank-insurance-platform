-- H2 has no partial indexes; a plain unique index is equivalent here because NULLs are distinct.
CREATE UNIQUE INDEX ux_proposal_application ON proposal.proposal (application_number);
