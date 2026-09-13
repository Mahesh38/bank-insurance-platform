package com.bank.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogPiiScrubberTest {

  @Test
  void scrubsPanAadhaarPhoneEmailAndHealth() {
    String raw =
        "PAN ABCDE1234F aadhaar 1234 5678 9012 mobile 9876543210 "
            + "email priya.sharma@example.com diagnosis=Type2Diabetes jobId=j-9";

    String scrubbed = LogPiiScrubber.scrub(raw);

    assertThat(scrubbed).doesNotContain("ABCDE1234F");
    assertThat(scrubbed).doesNotContain("1234 5678 9012");
    assertThat(scrubbed).doesNotContain("9876543210");
    assertThat(scrubbed).doesNotContain("priya.sharma@example.com");
    assertThat(scrubbed).doesNotContain("Type2Diabetes");
    assertThat(scrubbed).contains("*****1234F");
    assertThat(scrubbed).contains("****-****-9012");
    assertThat(scrubbed).contains("******3210");
    assertThat(scrubbed).contains("p***@e***.com");
    assertThat(scrubbed).contains("diagnosis=[REDACTED_HEALTH]");
    assertThat(scrubbed).contains("jobId=j-9");
  }

  @Test
  void nullAndEmptyPassThrough() {
    assertThat(LogPiiScrubber.scrub(null)).isNull();
    assertThat(LogPiiScrubber.scrub("")).isEmpty();
  }
}
