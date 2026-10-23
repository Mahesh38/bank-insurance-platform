-- DECISIONS-2026-10-22 E5: partner users are created directly by a bank admin (no approval step by
-- default). Creation, bulk import and approval of partner users are bank-admin-only, so the insurer
-- administrator role loses them. The evaluator additionally refuses these actions to every
-- INSURER_REPRESENTATIVE subject (identity.authorization.partner-representative.*).
delete from role_permission
 where role_code in ('PARTNER_SR', 'PARTNER_MANAGER', 'PARTNER_ADMIN')
   and permission_code in ('partner_user.create', 'partner_user.bulk_import', 'partner_user.approve');

update permission
   set description = 'Create a partner user (bank admin only; provisioned at creation, E5)'
 where code = 'partner_user.create';

-- Who created whom (E5 audit): the authenticated bank admin, kept on the row as well as in the
-- PARTNER_USER_CREATED outbox event.
alter table business_user add column created_by varchar(128);

-- DECISIONS-2026-10-22 E6: lead exceptions are approved only by bank employees in the SP hierarchy.
-- The catalogue (V2) has no branch-manager / approver role yet, so the strict seed is BANK_ADMIN
-- only; which hierarchy level approves which rule is LEAD.EXCEPTION `approverRoles`, checked
-- against this permission, branch-scoped. AI-DRAFTED, awaiting Rajal (product) / Deepali (security).
insert into permission(code, description, regulated) values
    ('lead.exception.approve', 'Approve a lead exception (bank SP hierarchy only, E6)', false);

insert into role_permission(role_code, permission_code) values
    ('BANK_ADMIN', 'lead.exception.approve');
