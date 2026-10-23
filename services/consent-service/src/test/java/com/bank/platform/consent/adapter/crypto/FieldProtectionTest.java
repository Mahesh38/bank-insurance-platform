package com.bank.platform.consent.adapter.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bank.platform.consent.adapter.otp.LoggingOtpDispatch;
import com.bank.platform.consent.adapter.security.LoggingSecurityEventSink;
import com.bank.platform.consent.config.ConsentConfiguration;
import com.bank.platform.consent.domain.ConsentCodes;
import com.bank.platform.consent.domain.ConsentRefusal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@Tag("unit")
class FieldProtectionTest {

  @Test
  void cipher_roundTripsAndRefusesTheDevKeyInProduction() {
    AesFieldCipher cipher = new AesFieldCipher(AesFieldCipher.DEV_KEY, "local-dev");
    byte[] encrypted = cipher.encrypt("CIF9000111");
    assertThat(new String(encrypted)).doesNotContain("CIF9000111");
    assertThat(cipher.decrypt(encrypted)).isEqualTo("CIF9000111");
    assertThat(cipher.keyId()).isEqualTo("local-dev");
    assertThatThrownBy(() -> cipher.decrypt(new byte[] {1, 2, 3}))
        .isInstanceOf(ConsentRefusal.class)
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED);
    assertThatThrownBy(
            () -> ConsentConfiguration.rejectDevKey(new String[] {"prod"}, AesFieldCipher.DEV_KEY))
        .isInstanceOf(IllegalStateException.class);
    ConsentConfiguration.rejectDevKey(new String[] {"local"}, AesFieldCipher.DEV_KEY);
  }

  @Test
  void hasher_matchesOnlyTheMintedCode() {
    HmacOtpHasher hasher = new HmacOtpHasher("pepper");
    String code = hasher.mintCode();
    assertThat(code).hasSize(6);
    String hash = hasher.hash(code);
    assertThat(hash).doesNotContain(code);
    assertThat(hasher.matches(code, hash)).isTrue();
    assertThat(hasher.matches("000000", hash)).isFalse();
    assertThat(hasher.matches(code, "zz")).isFalse();
  }

  @Test
  void dispatchLog_isAConstant() {
    Logger logger =
        (Logger) LoggerFactory.getLogger("com.bank.platform.consent.adapter.otp.DispatchTrace");
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      new LoggingOtpDispatch().send("challenge-1", new byte[] {1}, "135790");
      assertThat(appender.list).hasSize(1);
      assertThat(appender.list.get(0).getFormattedMessage())
          .isEqualTo("consent code dispatch accepted")
          .doesNotContain("135790", "challenge-1");
    } finally {
      logger.detachAppender(appender);
    }
  }

  @Test
  void dispatch_rejectsABlankCode() {
    assertThatThrownBy(() -> new LoggingOtpDispatch().send("challenge-1", new byte[] {1}, " "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void securityEvent_stripsLineBreaks() {
    Logger logger = (Logger) LoggerFactory.getLogger(LoggingSecurityEventSink.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      new LoggingSecurityEventSink()
          .subjectMismatch(
              "rm\n1", "jour\rney", "consent", "01ARZ3NDEKTSV4RRFFQ69G5FAV", "9876543210");
      assertThat(appender.list).hasSize(1);
      assertThat(appender.list.get(0).getFormattedMessage())
          .isEqualTo(
              "security_event=CONSENT_SUBJECT_MISMATCH actor=rm1 journey=journey consent=consent"
                  + " requestedSubject=01ARZ3NDEKTSV4RRFFQ69G5FAV recordedSubject=[redacted]")
          .doesNotContain("\n", "\r", "9876543210");
      new LoggingSecurityEventSink().subjectMismatch(" ", null, "", null, null);
      assertThat(appender.list.get(1).getFormattedMessage())
          .isEqualTo(
              "security_event=CONSENT_SUBJECT_MISMATCH actor=- journey=- consent=-"
                  + " requestedSubject=- recordedSubject=-");
    } finally {
      logger.detachAppender(appender);
    }
  }
}
