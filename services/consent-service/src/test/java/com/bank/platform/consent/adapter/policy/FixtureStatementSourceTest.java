package com.bank.platform.consent.adapter.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.platform.consent.domain.StatementDoc;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class FixtureStatementSourceTest {

  @Test
  void freshStatementIsDraftAndItsHashMatchesTheStoredText() throws Exception {
    StatementDoc doc = new FixtureStatementSource().current("CNS-DP", "en-IN");
    assertThat(doc.status()).isEqualTo("DRAFT");
    assertThat(doc.approved()).isFalse();
    assertThat(doc.statementId()).isEqualTo("CNS-DP-en-IN-v1.0");
    assertThat(doc.textHash()).isEqualTo(sha256(doc.text()));
    assertThat(doc.text()).doesNotContain("Approved");
  }

  @Test
  void statusCanBeMarkedApprovedForATest() {
    FixtureStatementSource source = new FixtureStatementSource();
    source.setStatus("CNS-SOL", "APPROVED");
    assertThat(source.current("CNS-SOL", "en-IN").approved()).isTrue();
    assertThat(source.current("CNS-DP", "en-IN").approved()).isFalse();
  }

  private static String sha256(String text) throws NoSuchAlgorithmException {
    byte[] digest =
        MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest);
  }
}
