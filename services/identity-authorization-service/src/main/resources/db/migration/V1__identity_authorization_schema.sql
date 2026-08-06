create table insurer (
    code varchar(64) primary key,
    name varchar(255) not null,
    active boolean not null default true
);

create table branch (
    code varchar(64) primary key,
    name varchar(255) not null,
    region_code varchar(64),
    active boolean not null default true
);

create table business_user (
    id uuid primary key,
    provider varchar(32) not null,
    provider_subject varchar(255),
    user_type varchar(40) not null,
    status varchar(24) not null,
    username varchar(255) not null unique,
    email varchar(320),
    first_name varchar(160),
    last_name varchar(160),
    employee_id varchar(128),
    insurer_code varchar(64),
    policy_version bigint not null default 1,
    valid_from timestamp with time zone not null,
    valid_until timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0,
    constraint fk_business_user_insurer foreign key (insurer_code) references insurer(code),
    constraint ck_business_user_type check (user_type in ('BANK_EMPLOYEE', 'INSURER_REPRESENTATIVE')),
    constraint ck_business_user_status check (status in ('PENDING', 'ACTIVE', 'SUSPENDED', 'DISABLED', 'EXPIRED'))
);

create unique index ux_business_user_provider_subject
    on business_user(provider, provider_subject);

create table user_branch_assignment (
    id uuid primary key,
    user_id uuid not null references business_user(id),
    branch_code varchar(64) not null references branch(code),
    assignment_source varchar(32) not null,
    valid_from timestamp with time zone not null,
    valid_until timestamp with time zone,
    created_at timestamp with time zone not null,
    unique(user_id, branch_code, valid_from)
);

create table organization_relationship (
    id uuid primary key,
    manager_user_id uuid not null references business_user(id),
    subordinate_user_id uuid not null references business_user(id),
    relationship_type varchar(32) not null,
    valid_from timestamp with time zone not null,
    valid_until timestamp with time zone,
    constraint ck_no_self_manager check (manager_user_id <> subordinate_user_id)
);

create table certification (
    id uuid primary key,
    user_id uuid not null references business_user(id),
    certification_type varchar(64) not null,
    certificate_number varchar(128),
    source varchar(32) not null,
    status varchar(24) not null,
    mandatory_for_selling boolean not null default false,
    valid_from timestamp with time zone not null,
    valid_until timestamp with time zone,
    created_at timestamp with time zone not null
);

create table role (
    code varchar(80) primary key,
    name varchar(160) not null,
    privileged boolean not null default false
);

create table permission (
    code varchar(120) primary key,
    description varchar(512) not null,
    regulated boolean not null default false
);

create table user_role (
    id uuid primary key,
    user_id uuid not null references business_user(id),
    role_code varchar(80) not null references role(code),
    valid_from timestamp with time zone not null,
    valid_until timestamp with time zone,
    assigned_by varchar(128) not null,
    created_at timestamp with time zone not null,
    unique(user_id, role_code, valid_from)
);

create table role_permission (
    role_code varchar(80) not null references role(code),
    permission_code varchar(120) not null references permission(code),
    primary key (role_code, permission_code)
);

create table entitlement (
    id uuid primary key,
    user_id uuid not null references business_user(id),
    effect varchar(12) not null,
    permission_code varchar(120) not null,
    scope_type varchar(20) not null,
    scope_value varchar(320),
    reason varchar(512) not null,
    valid_from timestamp with time zone not null,
    valid_until timestamp with time zone,
    approved_by varchar(128),
    created_at timestamp with time zone not null,
    constraint ck_entitlement_effect check (effect in ('GRANT', 'DENY')),
    constraint ck_entitlement_scope check (scope_type in ('GLOBAL', 'BRANCH', 'INSURER', 'RESOURCE'))
);

create table bulk_import (
    id uuid primary key,
    insurer_code varchar(64) references insurer(code),
    status varchar(24) not null,
    object_key varchar(512) not null,
    checksum varchar(128) not null,
    maker_id varchar(128) not null,
    checker_id varchar(128),
    submitted_at timestamp with time zone not null,
    decided_at timestamp with time zone,
    unique(checksum)
);

create table bulk_import_row (
    id uuid primary key,
    import_id uuid not null references bulk_import(id),
    row_number integer not null,
    status varchar(24) not null,
    external_reference varchar(255),
    error_code varchar(80),
    error_detail varchar(1000),
    unique(import_id, row_number)
);

create table approval_request (
    id uuid primary key,
    request_type varchar(64) not null,
    target_id uuid not null,
    status varchar(24) not null,
    maker_id varchar(128) not null,
    checker_id varchar(128),
    requested_at timestamp with time zone not null,
    decided_at timestamp with time zone,
    constraint ck_approval_status check (status in ('PENDING', 'APPROVED', 'REJECTED'))
);

create table outbox_event (
    id uuid primary key,
    aggregate_type varchar(64) not null,
    aggregate_id uuid not null,
    event_type varchar(96) not null,
    payload text not null,
    created_at timestamp with time zone not null,
    published_at timestamp with time zone,
    attempts integer not null default 0,
    last_error varchar(1000)
);

create index ix_outbox_unpublished on outbox_event(published_at, created_at);
