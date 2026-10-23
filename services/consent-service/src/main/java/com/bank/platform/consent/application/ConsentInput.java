package com.bank.platform.consent.application;

import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.Ulids;
import java.util.Set;

/** Shared request checks. Use cases call these; they are not a port. */
final class ConsentInput {

  static final Set<String> PURPOSES = Set.of("CNS-DP", "CNS-SOL", "CNS-QSH", "CNS-SHR", "CNS-COM");
  static final Set<String> CHANNELS = Set.of("RM_ASSISTED", "SELF_SERVICE", "HYBRID");
  static final Set<String> LOBS = Set.of("LIFE", "HEALTH", "GENERAL");
  static final String ACK = "OTP_SMS_CUSTOMER_DEVICE";

  private ConsentInput() {}

  static void requireUlid(String field, String value) {
    if (!Ulids.isValid(value)) {
      throw new ConsentRefusal("VALIDATION_ERROR", field + " must be a ULID");
    }
  }

  static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
