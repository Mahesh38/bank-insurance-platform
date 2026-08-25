-- DESIGN DDL — target-state wrapper for the already-applied identity-authorization Flyway.
-- Runtime truth today (public schema, H2-compatible):
--   services/identity-authorization-service/src/main/resources/db/migration/V1__identity_authorization_schema.sql
--   services/identity-authorization-service/src/main/resources/db/migration/V2__seed_role_permission_catalog.sql
-- Do not replace those files with this script. S09 may move objects into schema identity
-- with search_path + a contract-preserving migration.

CREATE TABLE IF NOT EXISTS identity.insurer (
    code varchar(64) PRIMARY KEY,
    name varchar(255) NOT NULL,
    active boolean NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS identity.branch (
    code varchar(64) PRIMARY KEY,
    name varchar(255) NOT NULL,
    region_code varchar(64),
    active boolean NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS identity.business_user (
    id uuid PRIMARY KEY,
    provider varchar(32) NOT NULL,
    provider_subject varchar(255),
    user_type varchar(40) NOT NULL,
    status varchar(24) NOT NULL,
    username varchar(255) NOT NULL UNIQUE,
    email varchar(320),
    first_name varchar(160),
    last_name varchar(160),
    employee_id varchar(128),
    insurer_code varchar(64),
    policy_version bigint NOT NULL DEFAULT 1,
    valid_from timestamp with time zone NOT NULL,
    valid_until timestamp with time zone,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_business_user_insurer FOREIGN KEY (insurer_code) REFERENCES identity.insurer(code),
    CONSTRAINT ck_business_user_type CHECK (user_type IN ('BANK_EMPLOYEE', 'INSURER_REPRESENTATIVE')),
    CONSTRAINT ck_business_user_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'DISABLED', 'EXPIRED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_business_user_provider_subject
    ON identity.business_user(provider, provider_subject);

CREATE TABLE IF NOT EXISTS identity.user_branch_assignment (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES identity.business_user(id),
    branch_code varchar(64) NOT NULL REFERENCES identity.branch(code),
    assignment_source varchar(32) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_until timestamp with time zone,
    created_at timestamp with time zone NOT NULL,
    UNIQUE (user_id, branch_code, valid_from)
);

CREATE TABLE IF NOT EXISTS identity.organization_relationship (
    id uuid PRIMARY KEY,
    manager_user_id uuid NOT NULL REFERENCES identity.business_user(id),
    subordinate_user_id uuid NOT NULL REFERENCES identity.business_user(id),
    relationship_type varchar(32) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_until timestamp with time zone,
    CONSTRAINT ck_no_self_manager CHECK (manager_user_id <> subordinate_user_id)
);

CREATE TABLE IF NOT EXISTS identity.certification (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES identity.business_user(id),
    certification_type varchar(64) NOT NULL,
    certificate_number varchar(128),
    source varchar(32) NOT NULL,
    status varchar(24) NOT NULL,
    mandatory_for_selling boolean NOT NULL DEFAULT false,
    valid_from timestamp with time zone NOT NULL,
    valid_until timestamp with time zone,
    created_at timestamp with time zone NOT NULL
);

-- Recommended S09 addition — PDP hot path (INV-ACT-01). Not in V1.
CREATE INDEX IF NOT EXISTS ix_certification_user_validity
    ON identity.certification (user_id, status, valid_until);

CREATE TABLE IF NOT EXISTS identity.role (
    code varchar(80) PRIMARY KEY,
    name varchar(160) NOT NULL,
    privileged boolean NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS identity.permission (
    code varchar(120) PRIMARY KEY,
    description varchar(512) NOT NULL,
    regulated boolean NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS identity.user_role (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES identity.business_user(id),
    role_code varchar(80) NOT NULL REFERENCES identity.role(code),
    valid_from timestamp with time zone NOT NULL,
    valid_until timestamp with time zone,
    assigned_by varchar(128) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    UNIQUE (user_id, role_code, valid_from)
);

CREATE TABLE IF NOT EXISTS identity.role_permission (
    role_code varchar(80) NOT NULL REFERENCES identity.role(code),
    permission_code varchar(120) NOT NULL REFERENCES identity.permission(code),
    PRIMARY KEY (role_code, permission_code)
);

CREATE TABLE IF NOT EXISTS identity.entitlement (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES identity.business_user(id),
    effect varchar(12) NOT NULL,
    permission_code varchar(120) NOT NULL,
    scope_type varchar(20) NOT NULL,
    scope_value varchar(320),
    reason varchar(512) NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_until timestamp with time zone,
    approved_by varchar(128),
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT ck_entitlement_effect CHECK (effect IN ('GRANT', 'DENY')),
    CONSTRAINT ck_entitlement_scope CHECK (scope_type IN ('GLOBAL', 'BRANCH', 'INSURER', 'RESOURCE'))
);

CREATE INDEX IF NOT EXISTS ix_entitlement_user_validity
    ON identity.entitlement (user_id, valid_until);

CREATE TABLE IF NOT EXISTS identity.bulk_import (
    id uuid PRIMARY KEY,
    insurer_code varchar(64) REFERENCES identity.insurer(code),
    status varchar(24) NOT NULL,
    object_key varchar(512) NOT NULL,
    checksum varchar(128) NOT NULL,
    maker_id varchar(128) NOT NULL,
    checker_id varchar(128),
    submitted_at timestamp with time zone NOT NULL,
    decided_at timestamp with time zone,
    UNIQUE (checksum)
);

CREATE TABLE IF NOT EXISTS identity.bulk_import_row (
    id uuid PRIMARY KEY,
    import_id uuid NOT NULL REFERENCES identity.bulk_import(id),
    row_number integer NOT NULL,
    status varchar(24) NOT NULL,
    external_reference varchar(255),
    error_code varchar(80),
    error_detail varchar(1000),
    UNIQUE (import_id, row_number)
);

CREATE TABLE IF NOT EXISTS identity.approval_request (
    id uuid PRIMARY KEY,
    request_type varchar(64) NOT NULL,
    target_id uuid NOT NULL,
    status varchar(24) NOT NULL,
    maker_id varchar(128) NOT NULL,
    checker_id varchar(128),
    requested_at timestamp with time zone NOT NULL,
    decided_at timestamp with time zone,
    CONSTRAINT ck_approval_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS identity.outbox_event (
    id uuid PRIMARY KEY,
    aggregate_type varchar(64) NOT NULL,
    aggregate_id uuid NOT NULL,
    event_type varchar(96) NOT NULL,
    payload text NOT NULL,
    created_at timestamp with time zone NOT NULL,
    published_at timestamp with time zone,
    attempts integer NOT NULL DEFAULT 0,
    last_error varchar(1000)
);

CREATE INDEX IF NOT EXISTS ix_outbox_unpublished
    ON identity.outbox_event (published_at, created_at);
