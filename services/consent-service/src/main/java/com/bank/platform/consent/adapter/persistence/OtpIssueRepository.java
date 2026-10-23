package com.bank.platform.consent.adapter.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpIssueRepository extends JpaRepository<OtpIssueEntity, String> {

  @Query(
      """
      select count(i) from OtpIssueEntity i
      where i.customerId = :customerId and i.purpose = :purpose and i.issuedAt >= :since
      """)
  int countSince(
      @Param("customerId") String customerId,
      @Param("purpose") String purpose,
      @Param("since") Instant since);
}
