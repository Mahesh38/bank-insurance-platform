package com.bank.platform.lead.domain;

/** Another request changed the lead after it was read; the caller should re-read and retry. */
public class ConcurrentLeadUpdateException extends RuntimeException {

  public ConcurrentLeadUpdateException(String leadId) {
    super("lead " + leadId + " was modified concurrently");
  }
}
