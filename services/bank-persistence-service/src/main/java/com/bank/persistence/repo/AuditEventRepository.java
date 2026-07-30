package com.bank.persistence.repo;

import com.bank.persistence.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, String> {

    List<AuditEventEntity> findByResourceIdOrderByEventTimeAsc(String resourceId);
}
