package com.bank.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bank.common.observability.ErrorMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * <b>S08-VT-06 / S08-G7</b> — PII never reaches logs.
 *
 * <p>Unlike {@link NoPiiInErrorOutputTest} (finite catalogue templates) and {@code PiiMaskerTest}
 * (masker unit behaviour), this test drives the realistic error-recording path, captures
 * <em>every</em> Logback event emitted by {@link Slf4jErrorRecorder}, and asserts zero matches for
 * PAN, Aadhaar, phone, email and health patterns — and that scrubbed forms appear instead.
 */
class NoPiiInEmittedLogsTest {

  private static final String PAN = "ABCDE1234F";
  private static final String AADHAAR = "1234 5678 9012";
  private static final String PHONE = "9876543210";
  private static final String EMAIL = "priya.sharma@example.com";
  private static final String HEALTH_VALUE = "Type2Diabetes";

  private ListAppender<ILoggingEvent> appender;
  private Logger recorderLogger;

  @BeforeEach
  void attachListAppender() {
    recorderLogger = (Logger) LoggerFactory.getLogger(Slf4jErrorRecorder.class);
    appender = new ListAppender<>();
    appender.start();
    recorderLogger.addAppender(appender);
  }

  @AfterEach
  void detachListAppender() {
    recorderLogger.detachAppender(appender);
    appender.stop();
  }

  @Test
  void platformErrorPathDoesNotEmitRegulatedPatternsInLogs() {
    PlatformErrorHandler handler =
        new PlatformErrorHandler(
            ErrorHandlingSettings.builder("onesb")
                .layer(PlatformLayer.L5)
                .boundary(TrustBoundary.PUBLIC)
                .build(),
            new Slf4jErrorRecorder(PlatformLayer.L5, new ErrorMetrics(new SimpleMeterRegistry())));

    String leakyReason =
        "upstream rejected customer PAN "
            + PAN
            + " aadhaar "
            + AADHAAR
            + " mobile "
            + PHONE
            + " email "
            + EMAIL
            + " diagnosis="
            + HEALTH_VALUE
            + " for jobId=job-42";

    IllegalStateException leakyCause =
        new IllegalStateException(
            "1SB body echo pan=" + PAN + " phone=" + PHONE + " diagnosis=" + HEALTH_VALUE);

    ServiceException ex =
        ServiceException.of(ErrorCodes.UPSTREAM_BUSINESS_ERROR)
            .service("onesb")
            .layer(PlatformLayer.L5)
            .component("OneSbProposalAdapter")
            .operation("POST /v1/proposals")
            .upstream("1SB", "ERR_CUSTOMER_DATA", 422)
            .reason(leakyReason)
            .cause(leakyCause)
            .build();

    handler.handleServiceException(ex);

    assertThat(appender.list)
        .as("Slf4jErrorRecorder must emit at least one log line for the failure")
        .isNotEmpty();

    String allLogs = capturedLogText(appender.list);

    assertNoMatch(allLogs, LogPiiScrubber.PAN, "PAN");
    assertNoMatch(allLogs, LogPiiScrubber.AADHAAR, "Aadhaar");
    assertNoMatch(allLogs, LogPiiScrubber.PHONE, "phone");
    assertNoMatch(allLogs, LogPiiScrubber.EMAIL, "email");
    assertThat(allLogs)
        .as("raw health attribute value must not appear in emitted logs")
        .doesNotContain(HEALTH_VALUE);
    assertThat(
            LogPiiScrubber.HEALTH
                .matcher(allLogs)
                .results()
                .anyMatch(m -> HEALTH_VALUE.equalsIgnoreCase(m.group(2))))
        .as("health pattern must not retain the clinical value")
        .isFalse();

    assertThat(allLogs).contains("*****1234F");
    assertThat(allLogs).contains("****-****-9012");
    assertThat(allLogs).contains("******3210");
    assertThat(allLogs).contains("p***@e***.com");
    assertThat(allLogs).contains("diagnosis=[REDACTED_HEALTH]");
    assertThat(allLogs).contains("jobId=job-42");

    // In-memory diagnostic retained for support search is unchanged — scrubbing is log-only.
    assertThat(ex.getDiagnostic().getReason()).contains(PAN).contains(EMAIL);
  }

  @Test
  void throwableAttachedToErrorLogIsScrubbed() {
    Slf4jErrorRecorder recorder =
        new Slf4jErrorRecorder(PlatformLayer.L5, new ErrorMetrics(new SimpleMeterRegistry()));

    ServiceErrorResponse response =
        ServiceException.of(ErrorCodes.UPSTREAM_UNAVAILABLE)
            .service("onesb")
            .layer(PlatformLayer.L5)
            .reason("upstream timeout for jobId=job-99")
            .build()
            .getErrorResponse();

    recorder.record(
        response,
        new IllegalStateException(
            "upstream body pan=" + PAN + " email=" + EMAIL + " diagnosis=" + HEALTH_VALUE));

    String allLogs = capturedLogText(appender.list);
    assertThat(allLogs).isNotBlank();
    assertNoMatch(allLogs, LogPiiScrubber.PAN, "PAN");
    assertNoMatch(allLogs, LogPiiScrubber.EMAIL, "email");
    assertThat(allLogs).doesNotContain(HEALTH_VALUE);
    assertThat(allLogs).contains("*****1234F");
    assertThat(allLogs).contains("diagnosis=[REDACTED_HEALTH]");
  }

  @Test
  void clientCausedWarnPathIsAlsoScrubbed() {
    PlatformErrorHandler handler =
        new PlatformErrorHandler(
            ErrorHandlingSettings.builder("bff")
                .layer(PlatformLayer.L4)
                .boundary(TrustBoundary.PUBLIC)
                .build(),
            new Slf4jErrorRecorder(PlatformLayer.L4, null));

    ServiceException ex =
        ServiceException.of(ErrorCodes.VALIDATION_ERROR)
            .service("bff")
            .layer(PlatformLayer.L4)
            .reason("invalid field email=" + EMAIL + " mobile=" + PHONE)
            .build();

    handler.handleServiceException(ex);

    String allLogs = capturedLogText(appender.list);
    assertThat(allLogs).isNotBlank();
    assertNoMatch(allLogs, LogPiiScrubber.EMAIL, "email");
    assertNoMatch(allLogs, LogPiiScrubber.PHONE, "phone");
    assertThat(allLogs).contains("p***@e***.com");
    assertThat(allLogs).contains("******3210");
  }

  private static String capturedLogText(List<ILoggingEvent> events) {
    return events.stream()
        .map(
            e -> {
              StringBuilder sb = new StringBuilder(e.getFormattedMessage());
              if (e.getThrowableProxy() != null) {
                sb.append('\n').append(e.getThrowableProxy().getMessage());
                if (e.getThrowableProxy().getClassName() != null) {
                  sb.append('\n').append(e.getThrowableProxy().getClassName());
                }
              }
              e.getMDCPropertyMap()
                  .forEach((k, v) -> sb.append('\n').append(k).append('=').append(v));
              return sb.toString();
            })
        .collect(Collectors.joining("\n"));
  }

  private static void assertNoMatch(String logs, Pattern pattern, String label) {
    assertThat(pattern.matcher(logs).find())
        .as("emitted logs must not contain an unmasked %s pattern; logs were:%n%s", label, logs)
        .isFalse();
  }
}
