package com.bank.persistence.adapter.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, String> {

  List<AuditEventEntity> findByResourceIdOrderByEventTimeAsc(String resourceId);
}
