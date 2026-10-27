package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * BRD 9.10 secure link. OTP columns become nullable because a link grant has no OTP transaction.
 * The link row may be updated; the consent evidence row stays insert-only.
 */
public class V4__ConsentLink extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    String product = context.getConnection().getMetaData().getDatabaseProductName();
    boolean postgres = product != null && product.toLowerCase().contains("postgres");
    try (var statement = context.getConnection().createStatement()) {
      if (postgres) {
        statement.execute(
            "ALTER TABLE consent.consent_record ALTER COLUMN otp_verified_at DROP NOT NULL");
        statement.execute(
            "ALTER TABLE consent.consent_record ALTER COLUMN otp_txn_id DROP NOT NULL");
      } else {
        statement.execute(
            "ALTER TABLE consent.consent_record ALTER COLUMN otp_verified_at SET NULL");
        statement.execute("ALTER TABLE consent.consent_record ALTER COLUMN otp_txn_id SET NULL");
      }
      statement.execute(
          """
          CREATE TABLE consent.consent_link (
              link_id              VARCHAR(26)  NOT NULL PRIMARY KEY,
              token_hash           VARCHAR(64)  NOT NULL,
              customer_id          VARCHAR(26)  NOT NULL,
              lead_id              VARCHAR(26)  NOT NULL,
              journey_id           VARCHAR(26),
              assessment_id        VARCHAR(26)  NOT NULL,
              encryption_key_id    VARCHAR(50)  NOT NULL,
              mobile_enc           BYTEA        NOT NULL,
              email_enc            BYTEA        NOT NULL,
              mobile_masked        VARCHAR(32)  NOT NULL,
              email_masked         VARCHAR(80)  NOT NULL,
              state                VARCHAR(16)  NOT NULL,
              expires_at           TIMESTAMP WITH TIME ZONE NOT NULL,
              issued_at            TIMESTAMP WITH TIME ZONE NOT NULL,
              decided_at           TIMESTAMP WITH TIME ZONE,
              source_ip            VARCHAR(64),
              consent_id           VARCHAR(26),
              actor_id             VARCHAR(64)  NOT NULL,
              config_version_ref   VARCHAR(128) NOT NULL,
              CONSTRAINT ux_consent_link_token UNIQUE (token_hash),
              CONSTRAINT ck_consent_link_state CHECK (
                  state IN ('PENDING', 'SUPERSEDED', 'AGREED', 'DISAGREED', 'EXPIRED'))
          )
          """);
      statement.execute(
          """
          CREATE INDEX ix_consent_link_assessment
              ON consent.consent_link (assessment_id, issued_at)
          """);
      statement.execute(
          """
          CREATE TABLE consent.consent_link_issue (
              issue_id        VARCHAR(26) NOT NULL PRIMARY KEY,
              customer_id     VARCHAR(26) NOT NULL,
              assessment_id   VARCHAR(26) NOT NULL,
              issued_at       TIMESTAMP WITH TIME ZONE NOT NULL
          )
          """);
      statement.execute(
          """
          CREATE INDEX ix_consent_link_issue_customer
              ON consent.consent_link_issue (customer_id, issued_at)
          """);
      if (postgres) {
        statement.execute(
            "GRANT INSERT, SELECT, UPDATE ON consent.consent_link TO " + V3__AppConsentGrants.ROLE);
        statement.execute(
            "GRANT INSERT, SELECT ON consent.consent_link_issue TO " + V3__AppConsentGrants.ROLE);
      }
    }
  }
}
