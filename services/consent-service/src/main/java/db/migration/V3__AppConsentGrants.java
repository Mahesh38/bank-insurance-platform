package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * CNS-R16 on PostgreSQL: {@code app_consent} may insert and select evidence, and may update only
 * the challenge. H2 has no equivalent role, so this migration records nothing there.
 */
public class V3__AppConsentGrants extends BaseJavaMigration {

  public static final String ROLE = "app_consent";

  /** Evidence is insert/select only. The challenge is the table that may be updated. */
  public static final String EVIDENCE_GRANT =
      "GRANT INSERT, SELECT ON consent.consent_record TO " + ROLE;

  public static final String CHALLENGE_GRANT =
      "GRANT INSERT, SELECT, UPDATE ON consent.consent_challenge TO " + ROLE;

  @Override
  public void migrate(Context context) throws Exception {
    String product = context.getConnection().getMetaData().getDatabaseProductName();
    if (product == null || !product.toLowerCase().contains("postgres")) {
      return;
    }
    try (var statement = context.getConnection().createStatement()) {
      statement.execute(
          "DO $$ BEGIN CREATE ROLE "
              + ROLE
              + "; EXCEPTION WHEN duplicate_object THEN NULL; END $$");
      statement.execute("GRANT USAGE ON SCHEMA consent TO " + ROLE);
      statement.execute(EVIDENCE_GRANT);
      statement.execute(CHALLENGE_GRANT);
      statement.execute("GRANT INSERT, SELECT ON consent.otp_issue TO " + ROLE);
      statement.execute("GRANT INSERT, SELECT ON consent.idempotency_record TO " + ROLE);
      statement.execute("GRANT INSERT, SELECT, UPDATE ON consent.outbox_event TO " + ROLE);
      statement.execute("GRANT INSERT, SELECT ON consent.processed_event TO " + ROLE);
    }
  }
}
