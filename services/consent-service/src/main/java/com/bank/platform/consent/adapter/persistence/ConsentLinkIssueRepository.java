package com.bank.platform.consent.adapter.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsentLinkIssueRepository extends JpaRepository<ConsentLinkIssueEntity, String> {

  @Query(
      """
      select count(i) from ConsentLinkIssueEntity i
      where i.customerId = :customerId and i.issuedAt >= :since
      """)
  int countSince(@Param("customerId") String customerId, @Param("since") Instant since);
}
