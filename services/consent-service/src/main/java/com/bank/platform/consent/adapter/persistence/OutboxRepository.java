package com.bank.platform.consent.adapter.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEntity, String> {

  List<OutboxEntity> findByAggregateIdOrderByCreatedAtAsc(String aggregateId);
}
