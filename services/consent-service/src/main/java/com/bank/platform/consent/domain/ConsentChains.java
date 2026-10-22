package com.bank.platform.consent.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Derives the head of an append-only chain. Older rows stay stored as they were written. */
public final class ConsentChains {

  private ConsentChains() {}

  public static List<ConsentEvidence> ordered(List<ConsentEvidence> rows) {
    List<ConsentEvidence> copy = new ArrayList<>(rows);
    copy.sort(Comparator.comparingInt(ConsentEvidence::recordVersion));
    return List.copyOf(copy);
  }

  public static ConsentEvidence head(List<ConsentEvidence> rows) {
    return rows.stream().max(Comparator.comparingInt(ConsentEvidence::recordVersion)).orElse(null);
  }

  /**
   * Head for one customer, one code and one counterparty. A null counterparty matches only rows
   * that also have none, so a sharing grant cannot satisfy data-processing.
   */
  public static ConsentEvidence selectHead(
      List<ConsentEvidence> rows, ConsentCode code, String counterpartyId) {
    String party = blank(counterpartyId);
    List<ConsentEvidence> matched =
        rows.stream()
            .filter(row -> row.code() == code)
            .filter(row -> Objects.equals(blank(row.counterpartyId()), party))
            .toList();
    return newestHead(matched);
  }

  /** Newest head across roots for one customer and consent type. */
  public static ConsentEvidence newestHead(List<ConsentEvidence> rows) {
    Map<String, ConsentEvidence> heads = new HashMap<>();
    for (ConsentEvidence row : rows) {
      ConsentEvidence current = heads.get(row.rootConsentId());
      if (current == null || row.recordVersion() > current.recordVersion()) {
        heads.put(row.rootConsentId(), row);
      }
    }
    return heads.values().stream()
        .max(Comparator.comparing(ConsentEvidence::capturedAt))
        .orElse(null);
  }

  public static boolean isHead(ConsentEvidence row, List<ConsentEvidence> chain) {
    ConsentEvidence head = head(chain);
    return head != null && head.evidenceId().equals(row.evidenceId());
  }

  private static String blank(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }
}
