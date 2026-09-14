package com.bank.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Review fix: {@link Slf4jErrorRecorder#forLogging(Throwable)} must keep the cause chain (and scrub
 * every message) so operators still see the root failure under "Caused by:".
 */
class Slf4jErrorRecorderCauseChainTest {

  private static final String PAN = "ABCDE1234F";

  @Test
  void forLoggingPreservesScrubbedCauseChainAndStack() {
    IllegalArgumentException root = new IllegalArgumentException("root pan=" + PAN);
    IllegalStateException mid = new IllegalStateException("mid failure", root);
    RuntimeException top = new RuntimeException("top failure", mid);

    Throwable logged = Slf4jErrorRecorder.forLogging(top);

    assertThat(logged.getMessage()).startsWith("java.lang.RuntimeException:");
    assertThat(logged.getMessage()).doesNotContain(PAN);
    assertThat(logged.getStackTrace()).isEqualTo(top.getStackTrace());

    assertThat(logged.getCause()).isNotNull();
    assertThat(logged.getCause().getMessage()).startsWith("java.lang.IllegalStateException:");
    assertThat(logged.getCause().getStackTrace()).isEqualTo(mid.getStackTrace());

    assertThat(logged.getCause().getCause()).isNotNull();
    assertThat(logged.getCause().getCause().getMessage())
        .startsWith("java.lang.IllegalArgumentException:")
        .contains("*****1234F")
        .doesNotContain(PAN);
    assertThat(logged.getCause().getCause().getCause()).isNull();
  }

  @Test
  void forLoggingPreservesScrubbedSuppressedAndBreaksCycles() {
    RuntimeException cyclic = new RuntimeException("cyclic pan=" + PAN);
    // Reflectively create a self-cause where possible is awkward; use suppressed + shared child.
    IllegalStateException child = new IllegalStateException("child aadhaar=1234 5678 9012");
    cyclic.addSuppressed(child);

    Throwable logged = Slf4jErrorRecorder.forLogging(cyclic);

    assertThat(logged.getMessage()).doesNotContain(PAN);
    assertThat(logged.getSuppressed()).hasSize(1);
    assertThat(logged.getSuppressed()[0].getMessage())
        .contains("****-****-9012")
        .doesNotContain("1234 5678 9012");
  }
}
