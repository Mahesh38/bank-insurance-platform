package com.bank.platform.consent.domain;

/** Sends the code to the registered-contact reference. Implementations must not log the code. */
public interface OtpDispatch {

  void send(String challengeId, byte[] registeredContactRefEnc, String otp);
}
