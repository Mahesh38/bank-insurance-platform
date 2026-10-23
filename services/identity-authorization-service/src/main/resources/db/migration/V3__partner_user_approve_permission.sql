-- READINESS-REVIEW §3.7: approving a partner user was open to any caller that could reach the
-- service. Approval is now a PDP decision on partner_user.approve, alongside partner_user.create.
-- Role mapping mirrors partner_user.create (V2); maker <> checker is still enforced in code.
-- AI-DRAFTED role mapping, awaiting Rajal (product) / Deepali (security) review.
insert into permission(code, description, regulated) values
    ('partner_user.approve', 'Approve (checker) a partner-user creation request', false);

insert into role_permission(role_code, permission_code) values
    ('PARTNER_ADMIN', 'partner_user.approve'),
    ('BANK_ADMIN', 'partner_user.approve');
