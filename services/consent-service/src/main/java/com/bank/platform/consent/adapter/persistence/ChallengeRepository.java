package com.bank.platform.consent.adapter.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeRepository extends JpaRepository<ChallengeEntity, String> {

  List<ChallengeEntity> findByStateInAndOtpExpiresAtBefore(List<String> states, Instant cutoff);
}
