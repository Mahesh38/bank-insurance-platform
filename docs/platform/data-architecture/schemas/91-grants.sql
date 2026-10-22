-- DESIGN DDL — least-privilege grants. Applied at S09 with IaC-created roles.
-- Replace <env> and rotate secrets. Never grant a human standing write on evidence.

-- Example role names (create in 00 / IaC, not here):
--   app_identity, app_bank_persistence, app_customer, app_lead_lms,
--   app_consent, app_suitability, app_catalogue, app_quotation,
--   app_proposal, app_payment, app_policy, app_journey, app_administration,
--   migrator_<schema>, job_retention, ro_breakglass
--   app_audit (PROPOSED, ADR-026 — only if accepted)
--
-- 2026-10-21 (READINESS-REVIEW §3.5): grants added for app_customer, app_catalogue, app_policy and
-- for every table added by D2 / D4 / D5 / D8 / ADR-024 / ADR-025. Evidence tables are INSERT +
-- SELECT only; their rows carry retain_until (consent, suitability, payment, policy, quote,
-- proposal) and are removed only by the retention job after Object Lock / legal-hold checks.

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
-- D2: exception evidence is append-only.
GRANT SELECT, INSERT ON lead_lms.lead_exception_evaluation TO app_lead_lms;
GRANT SELECT, INSERT ON lead_lms.lead_exception_decision TO app_lead_lms;
REVOKE UPDATE, DELETE ON lead_lms.lead_exception_evaluation FROM app_lead_lms;
REVOKE UPDATE, DELETE ON lead_lms.lead_exception_decision FROM app_lead_lms;

GRANT USAGE ON SCHEMA customer TO app_customer;
GRANT SELECT, INSERT, UPDATE ON customer.customer TO app_customer;
REVOKE DELETE ON customer.customer FROM app_customer;
GRANT SELECT, INSERT ON customer.customer_snapshot TO app_customer;
-- journey_id is bound once on an unbound snapshot (column-level UPDATE only).
GRANT UPDATE (journey_id) ON customer.customer_snapshot TO app_customer;
REVOKE DELETE ON customer.customer_snapshot FROM app_customer;
GRANT SELECT, INSERT, UPDATE, DELETE ON customer.idempotency_record TO app_customer;
GRANT SELECT, INSERT, UPDATE ON customer.outbox_event TO app_customer;
REVOKE DELETE ON customer.outbox_event FROM app_customer;

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
-- D4: canonical evaluation record, append-only (SUIT-R23).
GRANT SELECT, INSERT ON suitability.evaluation TO app_suitability;
GRANT SELECT, INSERT ON suitability.evaluation_outcome TO app_suitability;
GRANT SELECT, INSERT ON suitability.evaluation_status_event TO app_suitability;
GRANT SELECT, INSERT ON suitability.caution_ack TO app_suitability;
GRANT SELECT, INSERT ON suitability.suitability_usage TO app_suitability;
REVOKE UPDATE, DELETE ON suitability.evaluation FROM app_suitability;
REVOKE UPDATE, DELETE ON suitability.evaluation_outcome FROM app_suitability;
REVOKE UPDATE, DELETE ON suitability.evaluation_status_event FROM app_suitability;
REVOKE UPDATE, DELETE ON suitability.caution_ack FROM app_suitability;
REVOKE UPDATE, DELETE ON suitability.suitability_usage FROM app_suitability;

GRANT USAGE ON SCHEMA consent TO app_consent;
-- D-024: append-only — no UPDATE (withdrawal is a new row).
GRANT SELECT, INSERT ON consent.consent_record TO app_consent;
REVOKE UPDATE, DELETE ON consent.consent_record FROM app_consent;
GRANT SELECT, INSERT, UPDATE, DELETE ON consent.idempotency_record TO app_consent;
GRANT SELECT, INSERT, UPDATE ON consent.outbox_event TO app_consent;
REVOKE DELETE ON consent.outbox_event FROM app_consent;

GRANT USAGE ON SCHEMA catalogue TO app_catalogue;
-- Effective-dated reference data: a change is a new version row; effective_to closes the old one.
GRANT SELECT, INSERT, UPDATE ON catalogue.insurer TO app_catalogue;
GRANT SELECT, INSERT, UPDATE ON catalogue.product TO app_catalogue;
GRANT SELECT, INSERT, UPDATE ON catalogue.eligibility_band TO app_catalogue;
REVOKE DELETE ON catalogue.insurer FROM app_catalogue;
REVOKE DELETE ON catalogue.product FROM app_catalogue;
REVOKE DELETE ON catalogue.eligibility_band FROM app_catalogue;
GRANT SELECT, INSERT, UPDATE ON catalogue.outbox_event TO app_catalogue;
REVOKE DELETE ON catalogue.outbox_event FROM app_catalogue;

GRANT USAGE ON SCHEMA quotation TO app_quotation;
-- quote working state: DynamoDB {env}-quotation-working, IAM via IRSA (DB-DEC-0005)
GRANT SELECT, INSERT ON quotation.quote TO app_quotation;
GRANT SELECT, INSERT ON quotation.offer TO app_quotation;
REVOKE UPDATE, DELETE ON quotation.quote FROM app_quotation;
REVOKE UPDATE, DELETE ON quotation.offer FROM app_quotation;
GRANT SELECT, INSERT, UPDATE, DELETE ON quotation.idempotency_record TO app_quotation;

GRANT USAGE ON SCHEMA proposal TO app_proposal;
-- proposal working state: DynamoDB {env}-proposal-working, IAM via IRSA (DB-DEC-0005)
GRANT SELECT, INSERT ON proposal.proposal TO app_proposal;
GRANT SELECT, INSERT ON proposal.uw_requirement TO app_proposal;
GRANT SELECT, INSERT ON proposal.uw_document_ref TO app_proposal;
REVOKE UPDATE, DELETE ON proposal.proposal FROM app_proposal;
REVOKE UPDATE, DELETE ON proposal.uw_requirement FROM app_proposal;
REVOKE UPDATE, DELETE ON proposal.uw_document_ref FROM app_proposal;
-- D8: answers and post-payment status changes, append-only.
GRANT SELECT, INSERT ON proposal.proposal_answer TO app_proposal;
GRANT SELECT, INSERT ON proposal.proposal_status_history TO app_proposal;
REVOKE UPDATE, DELETE ON proposal.proposal_answer FROM app_proposal;
REVOKE UPDATE, DELETE ON proposal.proposal_status_history FROM app_proposal;
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

GRANT USAGE ON SCHEMA policy TO app_policy;
-- policy row carries mutable lifecycle columns (state, dates, TAT); status changes are also
-- appended to policy_status_history (D8).
GRANT SELECT, INSERT, UPDATE ON policy.policy TO app_policy;
REVOKE DELETE ON policy.policy FROM app_policy;
GRANT SELECT, INSERT ON policy.policy_status_history TO app_policy;
GRANT SELECT, INSERT ON policy.policy_document_ref TO app_policy;
REVOKE UPDATE, DELETE ON policy.policy_status_history FROM app_policy;
REVOKE UPDATE, DELETE ON policy.policy_document_ref FROM app_policy;
GRANT SELECT, INSERT, UPDATE, DELETE ON policy.idempotency_record TO app_policy;
GRANT SELECT, INSERT ON policy.processed_event TO app_policy;
GRANT SELECT, INSERT, UPDATE ON policy.outbox_event TO app_policy;
REVOKE DELETE ON policy.outbox_event FROM app_policy;

GRANT USAGE ON SCHEMA journey TO app_journey;
GRANT SELECT, INSERT, UPDATE ON journey.journey TO app_journey;
REVOKE DELETE ON journey.journey FROM app_journey;
GRANT SELECT, INSERT, UPDATE ON journey.journey_ref TO app_journey;
GRANT SELECT, INSERT ON journey.journey_transition TO app_journey;
REVOKE UPDATE, DELETE ON journey.journey_transition FROM app_journey;
GRANT SELECT, INSERT, UPDATE ON journey.outbox_event TO app_journey;
REVOKE DELETE ON journey.outbox_event FROM app_journey;
GRANT SELECT, INSERT, UPDATE, DELETE ON journey.idempotency_record TO app_journey;
GRANT SELECT, INSERT ON journey.processed_event TO app_journey;

GRANT USAGE ON SCHEMA administration TO app_administration;
GRANT SELECT, INSERT ON administration.configuration_record TO app_administration;
REVOKE UPDATE, DELETE ON administration.configuration_record FROM app_administration;
-- ADR-024: status transitions and effective_to on rule_set_version; the document itself is frozen
-- after publish by administration.fn_protect_rule_set_version (90-routines.sql).
GRANT SELECT, INSERT, UPDATE ON administration.rule_set_version TO app_administration;
REVOKE DELETE ON administration.rule_set_version FROM app_administration;
GRANT SELECT, INSERT, UPDATE, DELETE ON administration.idempotency_record TO app_administration;
GRANT SELECT, INSERT, UPDATE ON administration.outbox_event TO app_administration;
REVOKE DELETE ON administration.outbox_event FROM app_administration;

-- PROPOSED (ADR-026) — apply only if the owner accepts it:
-- GRANT USAGE ON SCHEMA audit TO app_audit;
-- GRANT SELECT, INSERT ON audit.audit_event TO app_audit;
-- GRANT SELECT, INSERT ON audit.archive_export TO app_audit;
-- REVOKE UPDATE, DELETE, TRUNCATE ON audit.audit_event FROM app_audit;
-- REVOKE UPDATE, DELETE, TRUNCATE ON audit.archive_export FROM app_audit;

-- job_retention: execute purge procedures only, no evidence DELETE.
-- The procedures are SECURITY DEFINER (90-routines.sql), so PostgreSQL's default EXECUTE-to-PUBLIC
-- must be revoked first or any role could run a purge with the owner's rights.
REVOKE EXECUTE ON PROCEDURE lead_lms.sp_purge_operational() FROM PUBLIC;
REVOKE EXECUTE ON PROCEDURE suitability.sp_purge_operational() FROM PUBLIC;
REVOKE EXECUTE ON PROCEDURE bank_persistence.sp_retention_sweep() FROM PUBLIC;
REVOKE EXECUTE ON PROCEDURE quotation.sp_purge_operational() FROM PUBLIC;
REVOKE EXECUTE ON PROCEDURE proposal.sp_purge_operational() FROM PUBLIC;
REVOKE EXECUTE ON PROCEDURE journey.sp_purge_operational() FROM PUBLIC;
REVOKE EXECUTE ON PROCEDURE payment.sp_purge_operational() FROM PUBLIC;
REVOKE EXECUTE ON PROCEDURE consent.sp_purge_operational() FROM PUBLIC;
REVOKE EXECUTE ON PROCEDURE policy.sp_purge_operational() FROM PUBLIC;
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
GRANT USAGE ON SCHEMA consent TO job_retention;
GRANT EXECUTE ON PROCEDURE consent.sp_purge_operational() TO job_retention;
GRANT USAGE ON SCHEMA policy TO job_retention;
GRANT EXECUTE ON PROCEDURE policy.sp_purge_operational() TO job_retention;

-- Break-glass read: time-bound via IAM, not a decrypt grant.
GRANT USAGE ON SCHEMA identity, bank_persistence, lead_lms, journey TO ro_breakglass;
GRANT SELECT ON ALL TABLES IN SCHEMA identity TO ro_breakglass;
GRANT SELECT ON ALL TABLES IN SCHEMA bank_persistence TO ro_breakglass;
-- Do not GRANT SELECT on customer.pii_enc / consent.contact_used_enc to ro_breakglass
-- unless the session cannot decrypt (application encryption). Prefer denying those tables.
