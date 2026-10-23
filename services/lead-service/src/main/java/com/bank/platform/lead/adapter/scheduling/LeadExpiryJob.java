package com.bank.platform.lead.adapter.scheduling;

import com.bank.platform.lead.application.LeadApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ageing sweep (E7): moves overdue unfinished leads to {@code EXPIRED}. The horizon is rule-set
 * configuration ({@code LEAD.POLICY.expiryDays}, stamped as {@code expires_at} at create); only the
 * schedule and batch size are Spring properties. Runs on every replica: claims are {@code SKIP
 * LOCKED}, and each batch is one transaction.
 */
@Component
@ConditionalOnProperty(name = "lead.expiry.enabled", havingValue = "true", matchIfMissing = true)
public class LeadExpiryJob {

  private static final Logger log = LoggerFactory.getLogger(LeadExpiryJob.class);

  private final LeadApplicationService leads;
  private final int batchSize;
  private final int maxBatches;

  public LeadExpiryJob(
      LeadApplicationService leads,
      @Value("${lead.expiry.batch-size:200}") int batchSize,
      @Value("${lead.expiry.max-batches-per-run:50}") int maxBatches) {
    if (batchSize <= 0 || maxBatches <= 0) {
      throw new IllegalStateException("lead.expiry batch-size and max-batches-per-run must be > 0");
    }
    this.leads = leads;
    this.batchSize = batchSize;
    this.maxBatches = maxBatches;
  }

  @Scheduled(
      initialDelayString = "${lead.expiry.initial-delay:PT1M}",
      fixedDelayString = "${lead.expiry.interval:PT15M}")
  public void sweep() {
    int total = 0;
    for (int batch = 0; batch < maxBatches; batch++) {
      int expired = leads.expireOverdue(batchSize);
      total += expired;
      if (expired < batchSize) {
        break;
      }
    }
    if (total > 0) {
      log.info("lead expiry sweep moved {} lead(s) to EXPIRED", total);
    }
  }
}
