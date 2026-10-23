package com.bank.platform.consent.adapter.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsentRecordRepository
    extends JpaRepository<ConsentRecordEntity, ConsentRecordKey> {

  @Query(
      """
      select c from ConsentRecordEntity c
      where c.customerId = :customerId and c.purpose = :purpose
        and ((:insurer is null and c.insurerCode is null) or c.insurerCode = :insurer)
        and c.recordVersion = (
          select max(h.recordVersion) from ConsentRecordEntity h where h.consentId = c.consentId)
      order by c.createdAt desc
      """)
  List<ConsentRecordEntity> heads(
      @Param("customerId") String customerId,
      @Param("purpose") String purpose,
      @Param("insurer") String insurer);

  @Query(
      """
      select c from ConsentRecordEntity c
      where c.consentId = :consentId
        and c.recordVersion = (
          select max(h.recordVersion) from ConsentRecordEntity h where h.consentId = :consentId)
      """)
  List<ConsentRecordEntity> headOf(@Param("consentId") String consentId);

  List<ConsentRecordEntity> findByConsentIdOrderByRecordVersionAsc(String consentId);

  @Query("select distinct c.consentId from ConsentRecordEntity c where c.customerId = :customerId")
  List<String> chainIdsForCustomer(@Param("customerId") String customerId);

  @Query("select distinct c.consentId from ConsentRecordEntity c where c.journeyId = :journeyId")
  List<String> chainIdsForJourney(@Param("journeyId") String journeyId);

  @Query(
      """
      select c from ConsentRecordEntity c
      where c.state = 'GRANTED' and c.validUntil is not null and c.validUntil <= :asOf
        and c.recordVersion = (
          select max(h.recordVersion) from ConsentRecordEntity h where h.consentId = c.consentId)
      """)
  List<ConsentRecordEntity> grantedPastValidity(@Param("asOf") Instant asOf);
}
