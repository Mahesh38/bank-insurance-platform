-- Lead activity, idempotency and outbox for the R0 slice (SUG-20261021-lft).
-- From docs/platform/data-architecture/schemas/04-lead_lms.sql. Portable between PostgreSQL and
-- H2 (MODE=PostgreSQL), as V2: the outbox payload is VARCHAR (JSON text) instead of JSONB, there
-- is no partial index on unpublished rows, and lead_assignment carries assigned_sp_id because the
-- R0 lead holds RM and SP separately (V2 has no assigned_ipr_id yet).

CREATE TABLE lead_lms.lead_assignment (
    assignment_id       CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL REFERENCES lead_lms.lead (lead_id),
    rm_id               VARCHAR(64)              NOT NULL,
    assigned_sp_id      VARCHAR(64),
    assigned_ipr_id     VARCHAR(64),
    assigned_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    assigned_by         VARCHAR(64)              NOT NULL,
    reason              VARCHAR(200),
    assignment_kind     VARCHAR(20)              NOT NULL,
    CONSTRAINT ck_lead_assignment_kind CHECK (assignment_kind IN ('CREATE_ASSIGN', 'REASSIGN'))
);

CREATE INDEX ix_lead_assignment_lead ON lead_lms.lead_assignment (lead_id, assigned_at);

CREATE TABLE lead_lms.lead_meeting (
    meeting_id          CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL REFERENCES lead_lms.lead (lead_id),
    meeting_type        VARCHAR(16)              NOT NULL,
    scheduled_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    meeting_link_enc    BYTEA,
    encryption_key_id   VARCHAR(50),
    created_by          VARCHAR(64)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_lead_meeting_type CHECK (meeting_type IN ('ONLINE', 'IN_PERSON')),
    CONSTRAINT ck_lead_meeting_link CHECK (
        (meeting_type <> 'ONLINE' AND meeting_link_enc IS NULL)
        OR (meeting_type = 'ONLINE' AND meeting_link_enc IS NOT NULL
            AND encryption_key_id IS NOT NULL))
);

CREATE INDEX ix_lead_meeting_lead ON lead_lms.lead_meeting (lead_id, scheduled_at);

CREATE TABLE lead_lms.lead_follow_up (
    follow_up_id        CHAR(26)                 PRIMARY KEY,
    lead_id             CHAR(26)                 NOT NULL REFERENCES lead_lms.lead (lead_id),
    note_enc            BYTEA                    NOT NULL,
    encryption_key_id   VARCHAR(50)              NOT NULL,
    created_by          VARCHAR(64)              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_lead_follow_up_lead ON lead_lms.lead_follow_up (lead_id, created_at);

CREATE TABLE lead_lms.idempotency_record (
    idempotency_key     VARCHAR(128)             PRIMARY KEY,
    request_hash        VARCHAR(64)              NOT NULL,
    response_ref        VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_lead_idemp_expiry ON lead_lms.idempotency_record (expires_at);

CREATE TABLE lead_lms.outbox_event (
    outbox_id           CHAR(26)                 PRIMARY KEY,
    aggregate_type      VARCHAR(64)              NOT NULL,
    aggregate_id        CHAR(26)                 NOT NULL,
    event_type          VARCHAR(96)              NOT NULL,
    payload             VARCHAR(4000)            NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at        TIMESTAMP WITH TIME ZONE,
    attempts            INTEGER                  NOT NULL DEFAULT 0,
    last_error          VARCHAR(1000)
);

CREATE INDEX ix_lead_outbox_unpublished ON lead_lms.outbox_event (published_at, created_at);
