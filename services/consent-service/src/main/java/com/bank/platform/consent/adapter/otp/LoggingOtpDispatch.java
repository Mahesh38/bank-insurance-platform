package com.bank.platform.consent.adapter.otp;

import com.bank.platform.consent.domain.OtpDispatch;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dispatch port. The log line names the challenge and nothing else. */
public final class LoggingOtpDispatch implements OtpDispatch {

  private static final Logger log = LoggerFactory.getLogger(LoggingOtpDispatch.class);

  @Override
  public void send(String challengeId, byte[] registeredContactRefEnc, String otp) {
    Objects.requireNonNull(registeredContactRefEnc, "contact reference");
    Objects.requireNonNull(otp, "otp");
    log.info("otp dispatched challenge={}", challengeId);
  }
}
