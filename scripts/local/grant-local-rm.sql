-- LOCAL / UAT-COMPOSE ONLY. Never run against a shared or production authorization database.
--
-- Since SUG-20261021-gap the workforce BFF asks the PDP before lead create and assign. A bank
-- user created just-in-time at first login has no role, so the PDP answers DEFAULT_DENY (403)
-- until a role and a branch are granted. In production that is the admin console's job; locally
-- run this AFTER the first login:
--
--   docker compose -f docker-compose.identity.yml exec -T authorization-db \
--     psql -U identity_authorization -d identity_authorization \
--     -v username="'<your-login-username>'" -v branch="'BR-MUM-001'" < scripts/local/grant-local-rm.sql
--
-- Idempotent: re-running changes nothing.

insert into branch(code, name, region_code, active)
values (:branch, 'Local test branch ' || :branch, 'LOCAL', true)
on conflict (code) do nothing;

insert into user_role(id, user_id, role_code, valid_from, assigned_by, created_at)
select gen_random_uuid(), u.id, 'RELATIONSHIP_MANAGER', now(), 'scripts/local/grant-local-rm.sql', now()
  from business_user u
 where u.username = :username
   and not exists (select 1 from user_role r
                    where r.user_id = u.id and r.role_code = 'RELATIONSHIP_MANAGER'
                      and (r.valid_until is null or r.valid_until > now()));

insert into user_branch_assignment(id, user_id, branch_code, assignment_source, valid_from, created_at)
select gen_random_uuid(), u.id, :branch, 'LOCAL_SCRIPT', now(), now()
  from business_user u
 where u.username = :username
   and not exists (select 1 from user_branch_assignment b
                    where b.user_id = u.id and b.branch_code = :branch
                      and (b.valid_until is null or b.valid_until > now()));

select u.username, r.role_code, b.branch_code
  from business_user u
  left join user_role r on r.user_id = u.id
  left join user_branch_assignment b on b.user_id = u.id
 where u.username = :username;
