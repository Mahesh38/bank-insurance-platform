package com.bank.platform.consent.domain;

import java.time.Instant;
import java.util.Optional;

/** Operational OTP transactions. Replacing a row advances attempts; it is not consent evidence. */
public interface OtpChallengeStore {

  void save(OtpChallenge challenge);

  Optional<OtpChallenge> find(String challengeId);

  int issuesSince(String customerId, ConsentCode code, Instant since);
}
