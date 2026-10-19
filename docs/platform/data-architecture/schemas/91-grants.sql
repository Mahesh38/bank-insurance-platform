-- DESIGN DDL — least-privilege grants. Applied at S09 with IaC-created roles.
-- Replace <env> and rotate secrets. Never grant a human standing write on evidence.

-- Example role names (create in 00 / IaC, not here):
--   app_identity, app_bank_persistence, app_customer, app_lead_lms,
--   app_consent, app_suitability, app_catalogue, app_quotation,
--   app_proposal, app_payment, app_policy, app_journey, app_administration,
--   migrator_<schema>, job_retention, ro_breakglass

-- Pattern per schema (repeat for each app_* role). Cross-schema GRANT is forbidden (DR-OWN-01).

GRANT USAGE ON SCHEMA identity TO app_identity;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA identity TO app_identity;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA identity TO app_identity;

GRANT USAGE ON SCHEMA bank_persistence TO app_bank_persistence;
GRANT SELECT, INSERT, UPDATE, DELETE ON bank_persistence.integration_job TO app_bank_persistence;
GRANT SELECT, INSERT, UPDATE, DELETE ON bank_persistence.integration_job_offer TO app_bank_persistence;
GRANT SELECT, INSERT, UPDATE, DELETE ON bank_persistence.job_poll_attempt TO app_bank_persistence;
GRANT SELECT, INSERT, UPDATE, DELETE ON bank_persistence.payment_session TO app_bank_persistence;
GRANT SELECT, INSERT ON bank_persistence.raw_payload TO app_bank_persistence;
GRANT SELECT, INSERT ON bank_persistence.audit_event TO app_bank_persistence;
REVOKE UPDATE, DELETE ON bank_persistence.raw_payload FROM app_bank_persistence;
REVOKE UPDATE, DELETE ON bank_persistence.audit_event FROM app_bank_persistence;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA bank_persistence TO app_bank_persistence;

GRANT USAGE ON SCHEMA lead_lms TO app_lead_lms;
GRANT SELECT, INSERT, UPDATE ON lead_lms.lead TO app_lead_lms;
REVOKE DELETE ON lead_lms.lead FROM app_lead_lms;
GRANT SELECT, INSERT ON lead_lms.lead_assignment TO app_lead_lms;
GRANT SELECT, INSERT ON lead_lms.lead_meeting TO app_lead_lms;
GRANT SELECT, INSERT ON lead_lms.lead_follow_up TO app_lead_lms;
REVOKE UPDATE, DELETE ON lead_lms.lead_assignment FROM app_lead_lms;
REVOKE UPDATE, DELETE ON lead_lms.lead_meeting FROM app_lead_lms;
REVOKE UPDATE, DELETE ON lead_lms.lead_follow_up FROM app_lead_lms;
GRANT SELECT, INSERT, UPDATE, DELETE ON lead_lms.idempotency_record TO app_lead_lms;
GRANT SELECT, INSERT, UPDATE ON lead_lms.outbox_event TO app_lead_lms;
REVOKE DELETE ON lead_lms.outbox_event FROM app_lead_lms;

GRANT USAGE ON SCHEMA suitability TO app_suitability;
GRANT SELECT, INSERT, UPDATE ON suitability.suitability TO app_suitability;
REVOKE DELETE ON suitability.suitability FROM app_suitability;
GRANT SELECT, INSERT ON suitability.suitability_answer_set TO app_suitability;
GRANT SELECT, INSERT, UPDATE ON suitability.suitability_mapping_run TO app_suitability;
REVOKE UPDATE, DELETE ON suitability.suitability_answer_set FROM app_suitability;
REVOKE DELETE ON suitability.suitability_mapping_run FROM app_suitability;
GRANT SELECT, INSERT, UPDATE, DELETE ON suitability.idempotency_record TO app_suitability;
GRANT SELECT, INSERT, UPDATE ON suitability.outbox_event TO app_suitability;
REVOKE DELETE ON suitability.outbox_event FROM app_suitability;

GRANT USAGE ON SCHEMA consent TO app_consent;
GRANT SELECT, INSERT, UPDATE ON consent.consent TO app_consent;
REVOKE DELETE ON consent.consent FROM app_consent;

GRANT USAGE ON SCHEMA quotation TO app_quotation;
GRANT SELECT, INSERT, UPDATE ON quotation.quote_working TO app_quotation;
REVOKE DELETE ON quotation.quote_working FROM app_quotation;
GRANT SELECT, INSERT ON quotation.quote TO app_quotation;
GRANT SELECT, INSERT ON quotation.offer TO app_quotation;
REVOKE UPDATE, DELETE ON quotation.quote FROM app_quotation;
REVOKE UPDATE, DELETE ON quotation.offer FROM app_quotation;
GRANT SELECT, INSERT, UPDATE, DELETE ON quotation.idempotency_record TO app_quotation;

GRANT USAGE ON SCHEMA proposal TO app_proposal;
GRANT SELECT, INSERT, UPDATE ON proposal.proposal_working TO app_proposal;
REVOKE DELETE ON proposal.proposal_working FROM app_proposal;
GRANT SELECT, INSERT ON proposal.proposal TO app_proposal;
GRANT SELECT, INSERT ON proposal.uw_requirement TO app_proposal;
GRANT SELECT, INSERT ON proposal.uw_document_ref TO app_proposal;
REVOKE UPDATE, DELETE ON proposal.proposal FROM app_proposal;
REVOKE UPDATE, DELETE ON proposal.uw_requirement FROM app_proposal;
REVOKE UPDATE, DELETE ON proposal.uw_document_ref FROM app_proposal;
GRANT SELECT, INSERT, UPDATE, DELETE ON proposal.idempotency_record TO app_proposal;

GRANT USAGE ON SCHEMA payment TO app_payment;
GRANT SELECT, INSERT, UPDATE ON payment.payment TO app_payment;
REVOKE DELETE ON payment.payment FROM app_payment;
GRANT SELECT, INSERT ON payment.payment_attempt TO app_payment;
REVOKE UPDATE, DELETE ON payment.payment_attempt FROM app_payment;
GRANT SELECT, INSERT, UPDATE ON payment.refund TO app_payment;
REVOKE DELETE ON payment.refund FROM app_payment;
GRANT SELECT, INSERT, UPDATE ON payment.outbox_event TO app_payment;
REVOKE DELETE ON payment.outbox_event FROM app_payment;
GRANT SELECT, INSERT, UPDATE, DELETE ON payment.idempotency_record TO app_payment;

GRANT USAGE ON SCHEMA journey TO app_journey;
GRANT SELECT, INSERT, UPDATE ON journey.journey TO app_journey;
REVOKE DELETE ON journey.journey FROM app_journey;
GRANT SELECT, INSERT, UPDATE ON journey.journey_ref TO app_journey;
GRANT SELECT, INSERT ON journey.journey_transition TO app_journey;
REVOKE UPDATE, DELETE ON journey.journey_transition FROM app_journey;
GRANT SELECT, INSERT, UPDATE ON journey.outbox_event TO app_journey;
REVOKE DELETE ON journey.outbox_event FROM app_journey;
GRANT SELECT, INSERT, UPDATE, DELETE ON journey.idempotency_record TO app_journey;

GRANT USAGE ON SCHEMA administration TO app_administration;
GRANT SELECT, INSERT ON administration.configuration_record TO app_administration;
REVOKE UPDATE, DELETE ON administration.configuration_record FROM app_administration;

-- job_retention: execute purge procedures only, no evidence DELETE.
GRANT USAGE ON SCHEMA lead_lms TO job_retention;
GRANT EXECUTE ON PROCEDURE lead_lms.sp_purge_operational() TO job_retention;
GRANT USAGE ON SCHEMA suitability TO job_retention;
GRANT EXECUTE ON PROCEDURE suitability.sp_purge_operational() TO job_retention;
GRANT USAGE ON SCHEMA bank_persistence TO job_retention;
GRANT EXECUTE ON PROCEDURE bank_persistence.sp_retention_sweep() TO job_retention;
GRANT USAGE ON SCHEMA quotation TO job_retention;
GRANT EXECUTE ON PROCEDURE quotation.sp_purge_operational() TO job_retention;
GRANT USAGE ON SCHEMA proposal TO job_retention;
GRANT EXECUTE ON PROCEDURE proposal.sp_purge_operational() TO job_retention;
GRANT USAGE ON SCHEMA journey TO job_retention;
GRANT EXECUTE ON PROCEDURE journey.sp_purge_operational() TO job_retention;
GRANT USAGE ON SCHEMA payment TO job_retention;
GRANT EXECUTE ON PROCEDURE payment.sp_purge_operational() TO job_retention;

-- Break-glass read: time-bound via IAM, not a decrypt grant.
GRANT USAGE ON SCHEMA identity, bank_persistence, lead_lms, journey TO ro_breakglass;
GRANT SELECT ON ALL TABLES IN SCHEMA identity TO ro_breakglass;
GRANT SELECT ON ALL TABLES IN SCHEMA bank_persistence TO ro_breakglass;
-- Do not GRANT SELECT on customer.pii_enc / consent.contact_used_enc to ro_breakglass
-- unless the session cannot decrypt (application encryption). Prefer denying those tables.
