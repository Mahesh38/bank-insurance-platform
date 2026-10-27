package com.bank.platform.suitability.adapter.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MappingProductRepository
    extends JpaRepository<MappingProductEntity, MappingProductEntity.Key> {

  @Query(
      """
      select p from MappingProductEntity p, MappingRunEntity r
      where p.runId = r.runId and r.assessmentId = :assessmentId and r.runNo = :runNo
      """)
  List<MappingProductEntity> forRun(
      @Param("assessmentId") String assessmentId, @Param("runNo") int runNo);
}
