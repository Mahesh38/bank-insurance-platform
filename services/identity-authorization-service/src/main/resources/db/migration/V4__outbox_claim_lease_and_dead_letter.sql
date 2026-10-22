-- READINESS-REVIEW §3.7: the provisioning dispatcher could double-provision with more than one pod
-- and silently stopped retrying after ten attempts. Rows are now leased while in flight
-- (claimed_until) and parked for an operator once attempts are exhausted (dead_lettered_at).
alter table outbox_event add column claimed_until timestamp with time zone;
alter table outbox_event add column dead_lettered_at timestamp with time zone;

create index ix_outbox_due on outbox_event(event_type, dead_lettered_at, claimed_until, created_at);
