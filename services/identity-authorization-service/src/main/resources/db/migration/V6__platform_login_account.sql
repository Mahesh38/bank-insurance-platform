alter table business_user add column first_login boolean not null default false;
alter table business_user add column failed_password_count integer not null default 0;
alter table business_user add column locked_reason varchar(32);
alter table business_user add column locked_at timestamp with time zone;
alter table business_user add column lock_cleared_at timestamp with time zone;
alter table business_user add column last_successful_login_at timestamp with time zone;
alter table business_user add column password_hash varchar(255);
alter table business_user add column password_changed_at timestamp with time zone;
alter table business_user add column mobile_number varchar(32);

alter table business_user add constraint ck_business_user_locked_reason
    check (locked_reason is null or locked_reason in ('PASSWORD', 'INACTIVITY'));
