package com.bank.insurance.persistence.persistence.repo;

import com.bank.insurance.persistence.persistence.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, String> {

    List<AuditEventEntity> findByResourceIdOrderByEventTimeAsc(String resourceId);
}
