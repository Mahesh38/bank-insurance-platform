package com.bank.platform.lead.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LeadRepository {

  /**
   * Inserts a new lead (version 0) or updates an existing one. An update whose stored version no
   * longer matches {@link Lead#version()} must fail with {@link ConcurrentLeadUpdateException}.
   */
  void save(Lead lead);

  Optional<Lead> findById(String leadId);

  List<Lead> findVisibleTo(String principalId);

  Optional<Lead> findUnfinished(
      String creatorPrincipalId, String customerId, ProductClass productClass);

  /**
   * Up to {@code limit} unfinished leads whose {@code expires_at} is at or before {@code now},
   * locked for the caller's transaction. Rows another sweeper holds are skipped ({@code FOR UPDATE
   * SKIP LOCKED} on PostgreSQL), so parallel replicas never expire the same lead twice.
   */
  List<Lead> lockOverdue(Instant now, int limit);
}
