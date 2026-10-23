package com.bank.platform.consent.adapter.otp;

import com.bank.platform.consent.domain.OtpDispatch;
import java.util.Objects;

/**
 * Dispatch port. The code and the contact never reach a log call. The trace line is a constant, so
 * a challenge id cannot be used to forge a log record either.
 */
public final class LoggingOtpDispatch implements OtpDispatch {

  @Override
  public void send(String challengeId, byte[] registeredContactRefEnc, String otp) {
    Objects.requireNonNull(challengeId, "challenge");
    Objects.requireNonNull(registeredContactRefEnc, "contact reference");
    if (otp == null || otp.isBlank()) {
      throw new IllegalArgumentException("otp");
    }
    DispatchTrace.accepted();
  }
}
