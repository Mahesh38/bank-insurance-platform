-- DESIGN DDL — Product catalogue SoR. Apply at S09 in the owning Catalogue service Flyway.
-- Effective-dated reference data. Active rows are not updated in place; a change is a new version.

CREATE TABLE catalogue.insurer (
    insurer_id          VARCHAR(64)              PRIMARY KEY,
    name                VARCHAR(255)             NOT NULL,
    group_class         VARCHAR(8)               NOT NULL,
    active              BOOLEAN                  NOT NULL DEFAULT true,
    effective_from      TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to        TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_insurer_group CHECK (group_class IN ('A', 'B'))
);

CREATE TABLE catalogue.product (
    product_id          CHAR(26)                 PRIMARY KEY,
    insurer_id          VARCHAR(64)              NOT NULL REFERENCES catalogue.insurer (insurer_id),
    product_code        VARCHAR(100)             NOT NULL,
    product_name        VARCHAR(200)             NOT NULL,
    lob                 VARCHAR(16)              NOT NULL,
    product_class       VARCHAR(16)              NOT NULL,
    effective_from      TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to        TIMESTAMP WITH TIME ZONE,
    version             INTEGER                  NOT NULL,
    payload             JSONB                    NOT NULL,
    CONSTRAINT ck_product_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL')),
    CONSTRAINT ux_product_version UNIQUE (insurer_id, product_code, version)
);

CREATE INDEX ix_product_effective
    ON catalogue.product (lob, product_class, insurer_id, effective_from, effective_to);

CREATE TABLE catalogue.eligibility_band (
    band_id             CHAR(26)                 PRIMARY KEY,
    product_id          CHAR(26)                 NOT NULL REFERENCES catalogue.product (product_id),
    lob                 VARCHAR(16)              NOT NULL,
    min_age             SMALLINT,
    max_age             SMALLINT,
    min_sum_assured     NUMERIC(15, 2),
    max_sum_assured     NUMERIC(15, 2),
    occupation_band     VARCHAR(64),
    effective_from      TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to        TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_elig_lob CHECK (lob IN ('LIFE', 'HEALTH', 'GENERAL'))
);

CREATE INDEX ix_elig_product ON catalogue.eligibility_band (product_id, effective_from);
