package com.bank.platform.consent.adapter.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsentLinkRepository extends JpaRepository<ConsentLinkEntity, String> {

  Optional<ConsentLinkEntity> findByTokenHash(String tokenHash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from ConsentLinkEntity c where c.tokenHash = :hash")
  Optional<ConsentLinkEntity> lockByTokenHash(@Param("hash") String hash);

  @Query(
      """
      select c from ConsentLinkEntity c
      where c.assessmentId = :assessmentId and c.state = 'PENDING'
      order by c.issuedAt desc
      """)
  List<ConsentLinkEntity> pending(@Param("assessmentId") String assessmentId);

  @Query(
      """
      select c from ConsentLinkEntity c
      where c.assessmentId = :assessmentId
      order by c.issuedAt desc
      """)
  List<ConsentLinkEntity> latest(@Param("assessmentId") String assessmentId);
}
