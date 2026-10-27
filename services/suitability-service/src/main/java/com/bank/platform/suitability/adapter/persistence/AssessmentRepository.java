package com.bank.platform.suitability.adapter.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssessmentRepository extends JpaRepository<AssessmentEntity, String> {

  boolean existsByLeadId(String leadId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from AssessmentEntity a where a.assessmentId = :id")
  Optional<AssessmentEntity> lockById(@Param("id") String id);
}
