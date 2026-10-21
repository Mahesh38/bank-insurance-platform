package com.bank.platform.proposal.adapter.dynamodb;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Fixed-width ISO-8601 UTC with milliseconds. {@link Instant#toString()} drops zero fractions, and
 * then {@code validUntil > :now} — a string comparison inside DynamoDB — would order wrongly.
 */
final class DynamoTimes {

  private static final DateTimeFormatter FORMAT =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  private DynamoTimes() {}

  static String format(Instant instant) {
    return FORMAT.format(instant);
  }

  static Instant parse(String value) {
    return value == null ? null : Instant.from(FORMAT.parse(value));
  }
}
