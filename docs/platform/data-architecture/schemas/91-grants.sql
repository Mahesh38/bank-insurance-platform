-- DESIGN DDL — least-privilege grants. Applied at S09 with IaC-created roles.
-- Replace <env> and rotate secrets. Never grant a human standing write on evidence.

-- Example role names (create in 00 / IaC, not here):
--   app_identity, app_bank_persistence, app_customer, app_opportunity,
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

GRANT USAGE ON SCHEMA consent TO app_consent;
GRANT SELECT, INSERT, UPDATE ON consent.consent TO app_consent;
REVOKE DELETE ON consent.consent FROM app_consent;

GRANT USAGE ON SCHEMA administration TO app_administration;
GRANT SELECT, INSERT ON administration.configuration_record TO app_administration;
REVOKE UPDATE, DELETE ON administration.configuration_record FROM app_administration;

-- job_retention: execute purge procedures only, no evidence DELETE.
GRANT USAGE ON SCHEMA opportunity TO job_retention;
GRANT EXECUTE ON PROCEDURE opportunity.sp_purge_operational() TO job_retention;
GRANT USAGE ON SCHEMA bank_persistence TO job_retention;
GRANT EXECUTE ON PROCEDURE bank_persistence.sp_retention_sweep() TO job_retention;

-- Break-glass read: time-bound via IAM, not a decrypt grant.
GRANT USAGE ON SCHEMA identity, bank_persistence, opportunity, journey TO ro_breakglass;
GRANT SELECT ON ALL TABLES IN SCHEMA identity TO ro_breakglass;
GRANT SELECT ON ALL TABLES IN SCHEMA bank_persistence TO ro_breakglass;
-- Do not GRANT SELECT on customer.pii_enc / consent.contact_used_enc to ro_breakglass
-- unless the session cannot decrypt (application encryption). Prefer denying those tables.
