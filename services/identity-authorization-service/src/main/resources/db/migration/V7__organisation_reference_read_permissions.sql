-- SUG-20261027-iah: BFF organisation and reference catalogue reads require explicit PDP grants.
insert into permission(code, description, regulated) values
    ('organisation.read', 'Read workforce organisation directory (branches, verticals, RMs, SPs)', false),
    ('reference.read', 'Read workforce reference catalogue (country codes, product classes, mobile validation)', false);

insert into role_permission(role_code, permission_code) values
    ('GENERAL_BANK_USER', 'organisation.read'),
    ('GENERAL_BANK_USER', 'reference.read'),
    ('RELATIONSHIP_MANAGER', 'organisation.read'),
    ('RELATIONSHIP_MANAGER', 'reference.read'),
    ('BANK_ADMIN', 'organisation.read'),
    ('BANK_ADMIN', 'reference.read');
