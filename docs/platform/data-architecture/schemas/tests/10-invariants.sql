-- Invariant assertions for the R0 design DDL pack (00..15, 90, 91) on PostgreSQL 16.
-- TEST HARNESS ONLY — run by scripts/db/validate-postgres.sh against a throw-away database, after
-- tests/00-test-roles.sql and the whole pack. Every assertion prints "PASS <label>"; the first
-- failure raises and (with ON_ERROR_STOP) stops the run with a non-zero exit code.
--
-- Section 0 attaches the triggers that 90-routines.sql designs but leaves commented for the owning
-- service's Flyway (attach statements copied verbatim from the comments there). Nothing in here
-- changes design intent; if an assertion fails, the design DDL is wrong, not the test.
--
-- SQLSTATEs: 23001 restrict_violation (immutability triggers), 23505 unique_violation,
-- 23514 check_violation, 42501 insufficient_privilege (91-grants.sql).

\set ON_ERROR_STOP 1
SET client_min_messages = notice;

CREATE TABLE pg_temp.result (label TEXT PRIMARY KEY);

-- Expect p_sql to fail with p_state, optionally while acting as p_role (SET LOCAL ROLE).
CREATE FUNCTION pg_temp.expect_fail(p_label TEXT, p_sql TEXT, p_state TEXT, p_role TEXT DEFAULT NULL)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    v_state TEXT;
    v_msg   TEXT;
BEGIN
    BEGIN
        IF p_role IS NOT NULL THEN
            EXECUTE format('SET LOCAL ROLE %I', p_role);
        END IF;
        EXECUTE p_sql;
    EXCEPTION WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS v_state = RETURNED_SQLSTATE, v_msg = MESSAGE_TEXT;
    END;
    RESET ROLE;
    IF v_state IS NULL THEN
        RAISE EXCEPTION 'FAIL %: statement succeeded, expected SQLSTATE %', p_label, p_state;
    ELSIF v_state <> p_state THEN
        RAISE EXCEPTION 'FAIL %: got SQLSTATE % (%), expected %', p_label, v_state, v_msg, p_state;
    END IF;
    INSERT INTO pg_temp.result VALUES (p_label);
    RAISE NOTICE 'PASS % (% %)', p_label, v_state, v_msg;
END;
$$;

-- Expect p_sql to succeed, optionally as p_role.
CREATE FUNCTION pg_temp.expect_ok(p_label TEXT, p_sql TEXT, p_role TEXT DEFAULT NULL)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF p_role IS NOT NULL THEN
        EXECUTE format('SET LOCAL ROLE %I', p_role);
    END IF;
    EXECUTE p_sql;
    RESET ROLE;
    INSERT INTO pg_temp.result VALUES (p_label);
    RAISE NOTICE 'PASS %', p_label;
END;
$$;

CREATE FUNCTION pg_temp.expect_true(p_label TEXT, p_cond BOOLEAN)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF p_cond IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'FAIL %: condition is %', p_label, p_cond;
    END IF;
    INSERT INTO pg_temp.result VALUES (p_label);
    RAISE NOTICE 'PASS %', p_label;
END;
$$;

-- 26-character ULID-shaped id (Crockford alphabet, test only).
CREATE FUNCTION pg_temp.u(p TEXT) RETURNS CHAR(26) LANGUAGE sql IMMUTABLE AS
$$ SELECT rpad(upper(p), 26, '0')::CHAR(26) $$;

-- ===========================================================================
-- 0. Attach the triggers 90-routines.sql designs (commented there for the owning Flyway)
-- ===========================================================================
CREATE TRIGGER trg_raw_payload_immutable
    BEFORE UPDATE OR DELETE ON bank_persistence.raw_payload
    FOR EACH ROW EXECUTE FUNCTION bank_persistence.fn_prevent_update_delete();
CREATE TRIGGER trg_audit_event_immutable
    BEFORE UPDATE OR DELETE ON bank_persistence.audit_event
    FOR EACH ROW EXECUTE FUNCTION bank_persistence.fn_prevent_update_delete();
CREATE TRIGGER trg_consent_record_immutable
    BEFORE UPDATE OR DELETE ON consent.consent_record
    FOR EACH ROW EXECUTE FUNCTION consent.fn_prevent_update_delete();
CREATE TRIGGER trg_suit_evaluation_immutable
    BEFORE UPDATE OR DELETE ON suitability.evaluation
    FOR EACH ROW EXECUTE FUNCTION suitability.fn_protect_answer_set();
CREATE TRIGGER trg_suit_answer_set_immutable
    BEFORE UPDATE OR DELETE ON suitability.suitability_answer_set
    FOR EACH ROW EXECUTE FUNCTION suitability.fn_protect_answer_set();
CREATE TRIGGER trg_rule_set_version_protect
    BEFORE UPDATE OR DELETE ON administration.rule_set_version
    FOR EACH ROW EXECUTE FUNCTION administration.fn_protect_rule_set_version();
CREATE TRIGGER trg_accountable_sp_immutable
    BEFORE UPDATE ON lead_lms.lead
    FOR EACH ROW EXECUTE FUNCTION lead_lms.fn_accountable_sp_immutable();
CREATE TRIGGER trg_lead_origination_immutable
    BEFORE UPDATE ON lead_lms.lead
    FOR EACH ROW EXECUTE FUNCTION lead_lms.fn_lead_origination_immutable();

-- ===========================================================================
-- 1. Bootstrap
-- ===========================================================================
SELECT pg_temp.expect_true('bootstrap: 13 context schemas + PROPOSED audit exist',
    (SELECT count(*) FROM pg_namespace WHERE nspname IN (
        'identity', 'bank_persistence', 'customer', 'lead_lms', 'consent', 'suitability',
        'catalogue', 'quotation', 'proposal', 'payment', 'policy', 'journey', 'administration',
        'audit')) = 14);

-- ===========================================================================
-- 2. Consent — append-only chain (D5 / D-024 / INV-CNS-01)
-- ===========================================================================
CREATE FUNCTION pg_temp.consent_sql(p_id TEXT, p_supersedes TEXT, p_chain TEXT, p_state TEXT,
                                    p_extra_cols TEXT DEFAULT '', p_extra_vals TEXT DEFAULT '',
                                    p_purpose TEXT DEFAULT 'ARRAY[''CNS-DP'',''CNS-QSH'']')
RETURNS TEXT LANGUAGE sql AS $$
SELECT format(
    'INSERT INTO consent.consent_record (consent_id, supersedes_consent_id, chain_id, customer_id,'
    ' checkpoint, purpose, statement_text, statement_version, channel, state, rule_set_id,'
    ' rule_set_version, lob, recorded_by, recorded_by_type%s)'
    ' VALUES (%L, %L, %L, %L, ''BEFORE_MULTI_QUOTE'', %s, ''stmt'', ''v1'', ''RM_ASSISTED'', %L,'
    ' ''CONSENT.POLICY'', ''1.0.0'', ''LIFE'', ''rm-1'', ''BANK_RM''%s)',
    p_extra_cols, p_id, p_supersedes, p_chain, pg_temp.u('CUST1'), p_purpose, p_state, p_extra_vals)
$$;

SELECT pg_temp.expect_ok('consent: REQUESTED root row without capture evidence is insertable',
    pg_temp.consent_sql(pg_temp.u('CNA'), NULL, pg_temp.u('CNA'), 'REQUESTED'));
SELECT pg_temp.expect_fail('consent: root row must have chain_id = consent_id',
    pg_temp.consent_sql(pg_temp.u('CNX'), NULL, pg_temp.u('CNA'), 'REQUESTED'), '23514');
SELECT pg_temp.expect_fail('consent: GRANTED without OTP/capture evidence rejected',
    pg_temp.consent_sql(pg_temp.u('CNB'), pg_temp.u('CNA'), pg_temp.u('CNA'), 'GRANTED'), '23514');
SELECT pg_temp.expect_fail('consent: unknown purpose code rejected',
    pg_temp.consent_sql(pg_temp.u('CNB'), pg_temp.u('CNA'), pg_temp.u('CNA'), 'REQUESTED',
        '', '', 'ARRAY[''CNS-XXX'']'), '23514');
SELECT pg_temp.expect_fail('consent: empty purpose rejected',
    pg_temp.consent_sql(pg_temp.u('CNB'), pg_temp.u('CNA'), pg_temp.u('CNA'), 'REQUESTED',
        '', '', 'ARRAY[]::TEXT[]'), '23514');
SELECT pg_temp.expect_fail('consent: NAMED_INSURER scope requires scope_insurer_id',
    pg_temp.consent_sql(pg_temp.u('CNB'), pg_temp.u('CNA'), pg_temp.u('CNA'), 'REQUESTED',
        ', scope', ', ''NAMED_INSURER'''), '23514');
SELECT pg_temp.expect_ok('consent: GRANTED with full capture evidence supersedes the root (as app_consent)',
    pg_temp.consent_sql(pg_temp.u('CNB'), pg_temp.u('CNA'), pg_temp.u('CNA'), 'GRANTED',
        ', otp_txn_id, captured_at, source_ip, contact_used_enc, encryption_key_id, valid_until',
        ', ''otp-1'', now(), ''10.0.0.1'', ''\x01''::bytea, ''key-1'', now() + interval ''90 days'''),
    'app_consent');
SELECT pg_temp.expect_fail('consent: a row is superseded at most once (ux_consent_superseded_once)',
    pg_temp.consent_sql(pg_temp.u('CNC'), pg_temp.u('CNA'), pg_temp.u('CNA'), 'ABANDONED'), '23505');
SELECT pg_temp.expect_fail('consent: WITHDRAWN without withdrawal_reason rejected',
    pg_temp.consent_sql(pg_temp.u('CNC'), pg_temp.u('CNB'), pg_temp.u('CNA'), 'WITHDRAWN'), '23514');
SELECT pg_temp.expect_ok('consent: withdrawal is a new WITHDRAWN row',
    pg_temp.consent_sql(pg_temp.u('CNC'), pg_temp.u('CNB'), pg_temp.u('CNA'), 'WITHDRAWN',
        ', withdrawal_reason', ', ''customer request'''));
SELECT pg_temp.expect_true('consent: current row of the chain is the one nobody supersedes (WITHDRAWN)',
    (SELECT string_agg(c.state, ',') FROM consent.consent_record c
      WHERE c.chain_id = pg_temp.u('CNA')
        AND NOT EXISTS (SELECT 1 FROM consent.consent_record s
                         WHERE s.supersedes_consent_id = c.consent_id)) = 'WITHDRAWN');
SELECT pg_temp.expect_fail('consent: UPDATE refused by trigger',
    $$UPDATE consent.consent_record SET state = 'GRANTED' WHERE consent_id = pg_temp.u('CNC')$$, '23001');
SELECT pg_temp.expect_fail('consent: DELETE refused by trigger',
    $$DELETE FROM consent.consent_record WHERE consent_id = pg_temp.u('CNC')$$, '23001');
SELECT pg_temp.expect_fail('consent: app_consent has no UPDATE grant',
    $$UPDATE consent.consent_record SET state = 'GRANTED' WHERE false$$, '42501', 'app_consent');
SELECT pg_temp.expect_fail('consent: app_consent has no DELETE grant',
    $$DELETE FROM consent.consent_record WHERE false$$, '42501', 'app_consent');

-- ===========================================================================
-- 3. Administration — rule_set_version publish immutability (ADR-024 §5)
-- ===========================================================================
CREATE FUNCTION pg_temp.rsv_sql(p_id TEXT, p_version TEXT, p_status TEXT, p_extra_cols TEXT DEFAULT '',
                                p_extra_vals TEXT DEFAULT '')
RETURNS TEXT LANGUAGE sql AS $$
SELECT format(
    'INSERT INTO administration.rule_set_version (rule_set_version_id, rule_set_id, version, domain,'
    ' scope_lob, classification, document, checksum, status, effective_from, engine_min_version,'
    ' maker_id%s) VALUES (%L, ''SUITABILITY.LIFE.TERM'', %L, ''SUITABILITY'', ''LIFE'', ''COMPLIANCE'','
    ' ''{"rules":[]}''::jsonb, ''sha256:00'', %L, now() - interval ''1 day'', ''1.0.0'', ''maker-1''%s)',
    p_extra_cols, p_id, p_version, p_status, p_extra_vals)
$$;

SELECT pg_temp.expect_ok('rule set: DRAFT insert (as app_administration)',
    pg_temp.rsv_sql(pg_temp.u('RSA'), '1.0.0', 'DRAFT'), 'app_administration');
SELECT pg_temp.expect_ok('rule set: DRAFT document is editable',
    $$UPDATE administration.rule_set_version SET document = '{"rules":[1]}', checksum = 'sha256:01'
       WHERE rule_set_version_id = pg_temp.u('RSA')$$);
SELECT pg_temp.expect_fail('rule set: PUBLISHED without checker rejected (maker-checker)',
    $$UPDATE administration.rule_set_version SET status = 'PUBLISHED', test_run_passed = true
       WHERE rule_set_version_id = pg_temp.u('RSA')$$, '23514');
SELECT pg_temp.expect_fail('rule set: checker = maker rejected',
    $$UPDATE administration.rule_set_version SET status = 'PUBLISHED', test_run_passed = true,
       checker_id = 'maker-1', checker_role = 'COMPLIANCE_APPROVER', decided_at = now()
       WHERE rule_set_version_id = pg_temp.u('RSA')$$, '23514');
SELECT pg_temp.expect_fail('rule set: PUBLISHED without a passed test run rejected',
    $$UPDATE administration.rule_set_version SET status = 'PUBLISHED',
       checker_id = 'checker-1', checker_role = 'COMPLIANCE_APPROVER', decided_at = now()
       WHERE rule_set_version_id = pg_temp.u('RSA')$$, '23514');
SELECT pg_temp.expect_ok('rule set: publish with distinct checker and passed tests',
    $$UPDATE administration.rule_set_version SET status = 'PUBLISHED', test_run_passed = true,
       checker_id = 'checker-1', checker_role = 'COMPLIANCE_APPROVER', decided_at = now()
       WHERE rule_set_version_id = pg_temp.u('RSA')$$);
SELECT pg_temp.expect_fail('rule set: published document is immutable',
    $$UPDATE administration.rule_set_version SET document = '{"rules":[2]}'
       WHERE rule_set_version_id = pg_temp.u('RSA')$$, '23001');
SELECT pg_temp.expect_fail('rule set: published checksum is immutable',
    $$UPDATE administration.rule_set_version SET checksum = 'sha256:02'
       WHERE rule_set_version_id = pg_temp.u('RSA')$$, '23001');
SELECT pg_temp.expect_fail('rule set: published effective_from is immutable',
    $$UPDATE administration.rule_set_version SET effective_from = now() - interval '2 days'
       WHERE rule_set_version_id = pg_temp.u('RSA')$$, '23001');
SELECT pg_temp.expect_fail('rule set: DELETE refused by trigger',
    $$DELETE FROM administration.rule_set_version WHERE rule_set_version_id = pg_temp.u('RSA')$$, '23001');
SELECT pg_temp.expect_fail('rule set: app_administration has no DELETE grant',
    $$DELETE FROM administration.rule_set_version WHERE false$$, '42501', 'app_administration');
SELECT pg_temp.expect_fail('rule set: second open PUBLISHED version rejected (ux_rsv_open_published)',
    pg_temp.rsv_sql(pg_temp.u('RSB'), '1.1.0', 'PUBLISHED',
        ', checker_id, checker_role, decided_at, test_run_passed',
        ', ''checker-1'', ''COMPLIANCE_APPROVER'', now(), true'), '23505');
SELECT pg_temp.expect_ok('rule set: newer version closes the old window (effective_to + SUPERSEDED)',
    $$UPDATE administration.rule_set_version SET effective_to = now(), status = 'SUPERSEDED'
       WHERE rule_set_version_id = pg_temp.u('RSA')$$);
SELECT pg_temp.expect_ok('rule set: newer version publishes once the old window is closed',
    pg_temp.rsv_sql(pg_temp.u('RSB'), '1.1.0', 'PUBLISHED',
        ', checker_id, checker_role, decided_at, test_run_passed',
        ', ''checker-1'', ''COMPLIANCE_APPROVER'', now(), true'));
SELECT pg_temp.expect_fail('rule set: effective_to is set once only',
    $$UPDATE administration.rule_set_version SET effective_to = now() + interval '1 day'
       WHERE rule_set_version_id = pg_temp.u('RSA')$$, '23001');
SELECT pg_temp.expect_fail('rule set: (rule_set_id, version) unique',
    pg_temp.rsv_sql(pg_temp.u('RSC'), '1.0.0', 'DRAFT'), '23505');
SELECT pg_temp.expect_fail('rule set: unknown classification rejected',
    replace(pg_temp.rsv_sql(pg_temp.u('RSC'), '2.0.0', 'DRAFT'), '''COMPLIANCE''', '''OTHER'''), '23514');
SELECT pg_temp.expect_fail('rule set: REJECTED needs decision_remarks',
    pg_temp.rsv_sql(pg_temp.u('RSC'), '2.0.0', 'REJECTED'), '23514');
SELECT pg_temp.expect_fail('rule set: effective_to must follow effective_from',
    pg_temp.rsv_sql(pg_temp.u('RSC'), '2.0.0', 'DRAFT', ', effective_to', ', now() - interval ''2 days'''),
    '23514');

-- ===========================================================================
-- 4. Administration — configuration_record versioning (INV-CFG-02)
-- ===========================================================================
CREATE FUNCTION pg_temp.cfg_sql(p_id TEXT, p_version INT, p_insurer TEXT)
RETURNS TEXT LANGUAGE sql AS $$
SELECT format(
    'INSERT INTO administration.configuration_record (config_id, domain, lob, insurer_id, version,'
    ' payload, effective_from, status, checksum, created_by) VALUES (%L, ''FORM'', ''LIFE'', %L, %s,'
    ' ''{}''::jsonb, now(), ''ACTIVE'', ''c'', ''admin-1'')', p_id, p_insurer, p_version)
$$;

SELECT pg_temp.expect_ok('config: insert v1 (as app_administration)',
    pg_temp.cfg_sql(pg_temp.u('CFA'), 1, NULL), 'app_administration');
SELECT pg_temp.expect_fail('config: same partition + version rejected even with NULL partition keys',
    pg_temp.cfg_sql(pg_temp.u('CFB'), 1, NULL), '23505');
SELECT pg_temp.expect_ok('config: same version for another insurer partition allowed',
    pg_temp.cfg_sql(pg_temp.u('CFB'), 1, 'INS-1'));
SELECT pg_temp.expect_fail('config: version must be >= 1',
    pg_temp.cfg_sql(pg_temp.u('CFC'), 0, 'INS-2'), '23514');
SELECT pg_temp.expect_fail('config: app_administration has no UPDATE grant (INSERT-only versions)',
    $$UPDATE administration.configuration_record SET status = 'WITHDRAWN' WHERE false$$, '42501',
    'app_administration');

-- ===========================================================================
-- 5. bank_persistence.audit_event — 14 delta + fn_next_audit_sequence (OPEN-I3)
-- ===========================================================================
CREATE FUNCTION pg_temp.bp_audit_sql(p_id TEXT, p_journey TEXT, p_seq TEXT, p_capacity TEXT DEFAULT NULL)
RETURNS TEXT LANGUAGE sql AS $$
SELECT format(
    'INSERT INTO bank_persistence.audit_event (event_id, action, actor_id, actor_type, resource_type,'
    ' resource_id, outcome, journey_id, sequence_no, acting_capacity) VALUES (%L, ''LEAD_CREATED'','
    ' ''rm-1'', ''BANK_RM'', ''LEAD'', ''L1'', ''SUCCESS'', %L, %s, %L)',
    p_id, p_journey, p_seq, p_capacity)
$$;

SELECT pg_temp.expect_ok('bp audit: INSERT with next sequence (as app_bank_persistence)',
    pg_temp.bp_audit_sql('ev-1', 'J1', 'bank_persistence.fn_next_audit_sequence(''J1'')'),
    'app_bank_persistence');
SELECT pg_temp.expect_ok('bp audit: INSERT with next sequence again',
    pg_temp.bp_audit_sql('ev-2', 'J1', 'bank_persistence.fn_next_audit_sequence(''J1'')'));
SELECT pg_temp.expect_true('bp audit: sequence is 1, 2 per journey',
    (SELECT array_agg(sequence_no ORDER BY sequence_no) FROM bank_persistence.audit_event
      WHERE journey_id = 'J1') = ARRAY[1, 2]);
SELECT pg_temp.expect_fail('bp audit: duplicate (journey_id, sequence_no) rejected',
    pg_temp.bp_audit_sql('ev-3', 'J1', '2'), '23505');
SELECT pg_temp.expect_ok('bp audit: NULL journey rows do not collide',
    pg_temp.bp_audit_sql('ev-4', NULL, '1') || '; ' || pg_temp.bp_audit_sql('ev-5', NULL, '1'));
SELECT pg_temp.expect_fail('bp audit: unknown acting_capacity rejected',
    pg_temp.bp_audit_sql('ev-6', 'J2', '1', 'BOSS'), '23514');
SELECT pg_temp.expect_fail('bp audit: UPDATE refused by trigger',
    $$UPDATE bank_persistence.audit_event SET outcome = 'FAILURE' WHERE event_id = 'ev-1'$$, '23001');
SELECT pg_temp.expect_fail('bp audit: DELETE refused by trigger',
    $$DELETE FROM bank_persistence.audit_event WHERE event_id = 'ev-1'$$, '23001');
SELECT pg_temp.expect_fail('bp audit: app_bank_persistence has no UPDATE grant',
    $$UPDATE bank_persistence.audit_event SET outcome = 'X' WHERE false$$, '42501', 'app_bank_persistence');
SELECT pg_temp.expect_fail('bp raw_payload: app_bank_persistence has no DELETE grant',
    $$DELETE FROM bank_persistence.raw_payload WHERE false$$, '42501', 'app_bank_persistence');

-- ===========================================================================
-- 6. audit.audit_event — PROPOSED (ADR-026): hash chain, uniques, INSERT-only
-- ===========================================================================
CREATE FUNCTION pg_temp.audit_sql(p_event TEXT, p_journey TEXT, p_seq TEXT)
RETURNS TEXT LANGUAGE sql AS $$
SELECT format(
    'INSERT INTO audit.audit_event (event_id, source, occurred_at, action, resource_type, resource_id,'
    ' outcome, actor_id, actor_type, journey_id, sequence_no, new_state, retain_until)'
    ' VALUES (%L, ''lead-service'', ''2026-10-22T10:00:00Z'', ''LEAD_CREATED'', ''LEAD'', ''L1'','
    ' ''SUCCESS'', ''rm-1'', ''BANK_RM'', %L, %s, ''{"state":"NEW"}''::jsonb, DATE ''2033-10-22'')',
    p_event, p_journey, p_seq)
$$;

SELECT pg_temp.expect_ok('audit: genesis row', pg_temp.audit_sql(pg_temp.u('AE1'), pg_temp.u('JA'), '1'));
SELECT pg_temp.expect_ok('audit: second row', pg_temp.audit_sql(pg_temp.u('AE2'), pg_temp.u('JA'), '2'));
SELECT pg_temp.expect_true('audit: genesis prev_hash is 64 zeros',
    (SELECT prev_hash FROM audit.audit_event WHERE event_id = pg_temp.u('AE1')) = repeat('0', 64));
SELECT pg_temp.expect_true('audit: prev_hash links to the previous row_hash',
    (SELECT prev_hash FROM audit.audit_event WHERE event_id = pg_temp.u('AE2'))
  = (SELECT row_hash FROM audit.audit_event WHERE event_id = pg_temp.u('AE1')));
SELECT pg_temp.expect_true('audit: row_hash is reproducible from the canonical row',
    (SELECT row_hash = encode(sha256(convert_to(
        prev_hash || '|' || event_id || '|' || source || '|' ||
        to_char(occurred_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"') || '|' ||
        action || '|' || resource_type || '|' || resource_id || '|' || outcome || '|' || actor_id || '|' ||
        COALESCE(acting_capacity, '') || '|' || COALESCE(journey_id, '') || '|' ||
        COALESCE(sequence_no::text, '') || '|' || COALESCE(prior_state::text, '') || '|' ||
        COALESCE(new_state::text, '') || '|' || COALESCE(config_version_ref, ''), 'UTF8')), 'hex')
       FROM audit.audit_event WHERE event_id = pg_temp.u('AE2')));
SELECT pg_temp.expect_fail('audit: duplicate event_id rejected (ux_audit_event_id)',
    pg_temp.audit_sql(pg_temp.u('AE1'), pg_temp.u('JB'), '1'), '23505');
SELECT pg_temp.expect_ok('audit: idempotent consume via ON CONFLICT (event_id) DO NOTHING',
    pg_temp.audit_sql(pg_temp.u('AE1'), pg_temp.u('JB'), '1') || ' ON CONFLICT (event_id) DO NOTHING');
SELECT pg_temp.expect_true('audit: redelivery added no row', (SELECT count(*) FROM audit.audit_event) = 2);
SELECT pg_temp.expect_fail('audit: duplicate (journey_id, sequence_no) rejected',
    pg_temp.audit_sql(pg_temp.u('AE3'), pg_temp.u('JA'), '2'), '23505');
SELECT pg_temp.expect_fail('audit: sequence_no without journey_id rejected',
    pg_temp.audit_sql(pg_temp.u('AE3'), NULL, '1'), '23514');
SELECT pg_temp.expect_fail('audit: UPDATE refused',
    $$UPDATE audit.audit_event SET outcome = 'FAILURE' WHERE event_id = pg_temp.u('AE1')$$, '23001');
SELECT pg_temp.expect_fail('audit: DELETE refused',
    $$DELETE FROM audit.audit_event WHERE event_id = pg_temp.u('AE1')$$, '23001');
SELECT pg_temp.expect_fail('audit: TRUNCATE refused', $$TRUNCATE audit.audit_event$$, '23001');
SELECT pg_temp.expect_ok('audit: archive export head',
    $$INSERT INTO audit.archive_export (export_id, from_audit_seq, to_audit_seq, chain_head_hash,
       object_ref, object_lock_until)
      SELECT pg_temp.u('EXA'), min(audit_seq), max(audit_seq), (array_agg(row_hash ORDER BY audit_seq DESC))[1],
             's3://audit/2026-10-22.jsonl', DATE '2033-10-22' FROM audit.audit_event$$);
SELECT pg_temp.expect_fail('audit: archive export range must be ordered',
    $$INSERT INTO audit.archive_export (export_id, from_audit_seq, to_audit_seq, chain_head_hash,
       object_ref, object_lock_until) VALUES (pg_temp.u('EXB'), 9, 1, repeat('0', 64), 'x', DATE '2033-10-22')$$,
    '23514');
SELECT pg_temp.expect_fail('audit: archive export UPDATE refused',
    $$UPDATE audit.archive_export SET object_ref = 'y' WHERE export_id = pg_temp.u('EXA')$$, '23001');

-- ===========================================================================
-- 7. Lead — partial uniques, CHECKs, frozen columns (INV-ACT-03, INV-LED-02)
-- ===========================================================================
CREATE FUNCTION pg_temp.lead_sql(p_id TEXT, p_state TEXT, p_extra_cols TEXT DEFAULT '', p_extra_vals TEXT DEFAULT '')
RETURNS TEXT LANGUAGE sql AS $$
SELECT format(
    'INSERT INTO lead_lms.lead (lead_id, customer_id, state, lob, product_class, source,'
    ' created_by_actor_type, created_by_principal_id, lead_generator_principal_id,'
    ' need_analysis_state, config_versions, expires_at%s) VALUES (%L, %L, %L, ''LIFE'', ''TERM'','
    ' ''RM'', ''BANK_RM'', ''rm-1'', ''rm-1'', ''NOT_STARTED'', ''{}''::jsonb, now() + interval ''30 days''%s)',
    p_extra_cols, p_id, pg_temp.u('CUST1'), p_state, p_extra_vals)
$$;

SELECT pg_temp.expect_ok('lead: NEW lead (as app_lead_lms)', pg_temp.lead_sql(pg_temp.u('LDA'), 'NEW'), 'app_lead_lms');
SELECT pg_temp.expect_fail('lead: second open lead for same creator/customer/class rejected (ux_lead_dedupe_open)',
    pg_temp.lead_sql(pg_temp.u('LDB'), 'NEW'), '23505');
SELECT pg_temp.expect_ok('lead: closed lead does not block a new one',
    $$UPDATE lead_lms.lead SET state = 'DISQUALIFIED' WHERE lead_id = pg_temp.u('LDA')$$);
SELECT pg_temp.expect_ok('lead: new open lead after disqualification', pg_temp.lead_sql(pg_temp.u('LDB'), 'NEW'));
SELECT pg_temp.expect_fail('lead: ASSIGNED without accountable SP rejected',
    $$UPDATE lead_lms.lead SET state = 'ASSIGNED' WHERE lead_id = pg_temp.u('LDB')$$, '23514');
SELECT pg_temp.expect_fail('lead: QUALIFIED requires bi_generated',
    $$UPDATE lead_lms.lead SET state = 'QUALIFIED', accountable_sp_id = 'sp-1',
       accountable_sp_cert_ref = '{}' WHERE lead_id = pg_temp.u('LDB')$$, '23514');
SELECT pg_temp.expect_ok('lead: first accountable SP assignment',
    $$UPDATE lead_lms.lead SET state = 'ASSIGNED', accountable_sp_id = 'sp-1',
       accountable_sp_cert_ref = '{"cert":"c1"}' WHERE lead_id = pg_temp.u('LDB')$$);
SELECT pg_temp.expect_fail('lead: accountable_sp_id is immutable once set',
    $$UPDATE lead_lms.lead SET accountable_sp_id = 'sp-2' WHERE lead_id = pg_temp.u('LDB')$$, '23001');
SELECT pg_temp.expect_fail('lead: origination columns are immutable',
    $$UPDATE lead_lms.lead SET lob = 'HEALTH' WHERE lead_id = pg_temp.u('LDB')$$, '23001');
SELECT pg_temp.expect_fail('lead: exception_hold only with APPROVAL_REQUIRED',
    $$UPDATE lead_lms.lead SET exception_hold = true WHERE lead_id = pg_temp.u('LDB')$$, '23514');
SELECT pg_temp.expect_ok('lead: converted_journey_id set once',
    $$UPDATE lead_lms.lead SET converted_journey_id = pg_temp.u('JC') WHERE lead_id = pg_temp.u('LDB')$$);
SELECT pg_temp.expect_fail('lead: one journey converts at most one lead (ux_lead_converted_journey)',
    $$UPDATE lead_lms.lead SET converted_journey_id = pg_temp.u('JC') WHERE lead_id = pg_temp.u('LDA')$$, '23505');
SELECT pg_temp.expect_fail('lead: app_lead_lms has no DELETE grant',
    $$DELETE FROM lead_lms.lead WHERE false$$, '42501', 'app_lead_lms');
SELECT pg_temp.expect_fail('lead: exception evaluation evidence is INSERT-only for app_lead_lms',
    $$UPDATE lead_lms.lead_exception_evaluation SET lead_id = lead_id WHERE false$$, '42501', 'app_lead_lms');

-- ===========================================================================
-- 8. Suitability — canonical evaluation is INSERT-only (D4, SUIT-R23)
-- ===========================================================================
SELECT pg_temp.expect_ok('suitability: header IN_PROGRESS',
    $$INSERT INTO suitability.suitability (suitability_id, lead_id, customer_id, lob, product_class,
       questionnaire_id, questionnaire_version, state, config_version_ref)
      VALUES (pg_temp.u('SUA'), pg_temp.u('LDB'), pg_temp.u('CUST1'), 'LIFE', 'TERM', 'Q-TERM', '1', 'IN_PROGRESS', 'cfg-1')$$);
SELECT pg_temp.expect_fail('suitability: one current assessment per lead (ux_suit_lead_current)',
    $$INSERT INTO suitability.suitability (suitability_id, lead_id, customer_id, lob, product_class,
       questionnaire_id, questionnaire_version, state, config_version_ref)
      VALUES (pg_temp.u('SUB'), pg_temp.u('LDB'), pg_temp.u('CUST1'), 'LIFE', 'TERM', 'Q-TERM', '1', 'IN_PROGRESS', 'cfg-1')$$,
    '23505');
SELECT pg_temp.expect_fail('suitability: COMPLETED requires current_evaluation_id',
    $$UPDATE suitability.suitability SET state = 'COMPLETED' WHERE suitability_id = pg_temp.u('SUA')$$, '23514');
SELECT pg_temp.expect_ok('suitability: evaluation insert (as app_suitability)',
    $$INSERT INTO suitability.evaluation (evaluation_id, suitability_id, answer_set_version, customer_id, lob,
       questionnaire_id, questionnaire_version, rule_set_id, rule_set_version, engine_version, inputs_hash,
       derived_values, underwriting_risk_flag, pdf_artefact_ref, evaluated_by_actor_id, evaluated_at,
       valid_until, gate_rule_set_version)
      VALUES (pg_temp.u('EVA'), pg_temp.u('SUA'), 1, pg_temp.u('CUST1'), 'LIFE', 'Q-TERM', '1',
       'SUITABILITY.LIFE.TERM', '1.1.0', '1.0.0', 'sha256:00', '{}', false, 's3://suit/eva.pdf', 'rm-1',
       now(), now() + interval '30 days', '1.0.0')$$, 'app_suitability');
SELECT pg_temp.expect_fail('suitability: caution_ack_required only with SUITABLE_WITH_CAUTION',
    $$INSERT INTO suitability.evaluation_outcome (evaluation_id, product_class, outcome, fired_rules,
       rule_set_id, rule_set_version, caution_ack_required)
      VALUES (pg_temp.u('EVA'), 'TERM', 'SUITABLE', '[]', 'SUITABILITY.LIFE.TERM', '1.1.0', true)$$, '23514');
SELECT pg_temp.expect_fail('suitability: INVALIDATED status needs a known reason',
    $$INSERT INTO suitability.evaluation_status_event (status_event_id, evaluation_id, status, reason, recorded_by)
      VALUES (pg_temp.u('SEA'), pg_temp.u('EVA'), 'INVALIDATED', 'BORED', 'sys')$$, '23514');
SELECT pg_temp.expect_fail('suitability: evaluation UPDATE refused by trigger',
    $$UPDATE suitability.evaluation SET lob = 'HEALTH' WHERE evaluation_id = pg_temp.u('EVA')$$, '23001');
SELECT pg_temp.expect_fail('suitability: app_suitability has no UPDATE on evaluation',
    $$UPDATE suitability.evaluation SET lob = lob WHERE false$$, '42501', 'app_suitability');
SELECT pg_temp.expect_fail('suitability: app_suitability has no UPDATE on evaluation_outcome',
    $$UPDATE suitability.evaluation_outcome SET outcome = outcome WHERE false$$, '42501', 'app_suitability');

-- ===========================================================================
-- 9. Payment — one active payment per proposal (INV-PAY-02)
-- ===========================================================================
CREATE FUNCTION pg_temp.pay_sql(p_id TEXT, p_state TEXT, p_amount TEXT DEFAULT '1000.00')
RETURNS TEXT LANGUAGE sql AS $$
SELECT format(
    'INSERT INTO payment.payment (payment_id, lead_id, journey_id, customer_id, quote_id, proposal_id,'
    ' amount, state, device_channel, link_issued_to_enc, encryption_key_id, link_expires_at, lob,'
    ' config_version_ref) VALUES (%L, %L, %L, %L, %L, %L, %s, %L, ''SMS_LINK'', ''\x01''::bytea, ''k1'','
    ' now() + interval ''1 day'', ''LIFE'', ''cfg-1'')',
    p_id, pg_temp.u('LDB'), pg_temp.u('JC'), pg_temp.u('CUST1'), pg_temp.u('QA'), pg_temp.u('PRA'),
    p_amount, p_state)
$$;

SELECT pg_temp.expect_ok('payment: INITIATED (as app_payment)', pg_temp.pay_sql(pg_temp.u('PYA'), 'INITIATED'), 'app_payment');
SELECT pg_temp.expect_fail('payment: second active payment for the proposal rejected',
    pg_temp.pay_sql(pg_temp.u('PYB'), 'LINK_ISSUED'), '23505');
SELECT pg_temp.expect_ok('payment: terminal payment frees the proposal',
    $$UPDATE payment.payment SET state = 'EXPIRED' WHERE payment_id = pg_temp.u('PYA')$$);
SELECT pg_temp.expect_ok('payment: new attempt after expiry', pg_temp.pay_sql(pg_temp.u('PYB'), 'INITIATED'));
SELECT pg_temp.expect_fail('payment: amount must be positive', pg_temp.pay_sql(pg_temp.u('PYC'), 'EXPIRED', '0'), '23514');
SELECT pg_temp.expect_fail('payment: unknown state rejected', pg_temp.pay_sql(pg_temp.u('PYC'), 'PAID'), '23514');
SELECT pg_temp.expect_fail('payment: app_payment has no DELETE grant',
    $$DELETE FROM payment.payment WHERE false$$, '42501', 'app_payment');

-- ===========================================================================
-- 10. Remaining INSERT-only grants (D8) and purge procedures
-- ===========================================================================
SELECT pg_temp.expect_fail('quotation: offer is INSERT-only for app_quotation',
    $$UPDATE quotation.offer SET quote_id = quote_id WHERE false$$, '42501', 'app_quotation');
SELECT pg_temp.expect_fail('proposal: proposal_answer is INSERT-only for app_proposal',
    $$UPDATE proposal.proposal_answer SET proposal_id = proposal_id WHERE false$$, '42501', 'app_proposal');
SELECT pg_temp.expect_fail('proposal: status history is INSERT-only for app_proposal',
    $$DELETE FROM proposal.proposal_status_history WHERE false$$, '42501', 'app_proposal');
SELECT pg_temp.expect_fail('policy: status history is INSERT-only for app_policy',
    $$UPDATE policy.policy_status_history SET policy_id = policy_id WHERE false$$, '42501', 'app_policy');
SELECT pg_temp.expect_fail('journey: transitions are INSERT-only for app_journey',
    $$DELETE FROM journey.journey_transition WHERE false$$, '42501', 'app_journey');
SELECT pg_temp.expect_fail('cross-schema: app_quotation cannot read consent (DR-OWN-01)',
    $$SELECT 1 FROM consent.consent_record$$, '42501', 'app_quotation');
SELECT pg_temp.expect_fail('job_retention: no direct DELETE on evidence',
    $$DELETE FROM consent.consent_record WHERE false$$, '42501', 'job_retention');
SELECT pg_temp.expect_fail('purge: app roles cannot EXECUTE purge procedures (EXECUTE revoked from PUBLIC)',
    $$CALL consent.sp_purge_operational()$$, '42501', 'app_consent');
SELECT pg_temp.expect_ok('purge: job_retention runs every purge procedure (SECURITY DEFINER)',
    $$CALL lead_lms.sp_purge_operational(); CALL suitability.sp_purge_operational();
      CALL bank_persistence.sp_retention_sweep(); CALL quotation.sp_purge_operational();
      CALL proposal.sp_purge_operational(); CALL journey.sp_purge_operational();
      CALL payment.sp_purge_operational(); CALL consent.sp_purge_operational();
      CALL policy.sp_purge_operational()$$, 'job_retention');
SELECT pg_temp.expect_true('purge: evidence untouched by the purge run',
    (SELECT count(*) FROM consent.consent_record) = 3);

SELECT format('ALL INVARIANT TESTS PASSED (%s assertions)', count(*)) AS result FROM pg_temp.result;
