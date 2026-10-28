package com.bank.platform.integration.hub.domain;

/** Outbox for AuditRecorded-style events (Hub LLD §12 / ADR-025 shape). */
public interface HubOutbox {

  void append(
      String aggregateType,
      String aggregateId,
      String eventType,
      String schemaVersion,
      String payloadJson);
}
