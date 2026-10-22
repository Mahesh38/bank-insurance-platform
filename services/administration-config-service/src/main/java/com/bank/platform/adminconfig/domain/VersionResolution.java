package com.bank.platform.adminconfig.domain;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Picks the version in force (ADR-024 section 5 point 4) among published versions of one domain.
 * A scope value of null on a version means "any"; a version scoped exactly to the requested lob /
 * productClass beats a wildcard one. Among equally specific candidates the latest {@code
 * effectiveFrom} wins, then the latest approval — so publishing a new version with a later start
 * supersedes the old one without touching the old (immutable) row.
 */
public final class VersionResolution {

  private VersionResolution() {}

  public static Optional<RuleSetVersion> inForce(
      List<RuleSetVersion> published, String lob, String productClass, OffsetDateTime asOf) {
    return published.stream()
        .filter(v -> v.inForceAt(asOf))
        .filter(v -> matches(v.snapshot().scopeLob(), lob))
        .filter(v -> matches(v.snapshot().scopeProductClass(), productClass))
        .max(
            Comparator.comparingInt((RuleSetVersion v) -> specificity(v))
                .thenComparing(v -> v.snapshot().effectiveFrom().toInstant())
                .thenComparing(v -> v.snapshot().approvedAt()));
  }

  private static boolean matches(String scoped, String requested) {
    return scoped == null || Objects.equals(scoped, requested);
  }

  private static int specificity(RuleSetVersion v) {
    return (v.snapshot().scopeLob() == null ? 0 : 1)
        + (v.snapshot().scopeProductClass() == null ? 0 : 1);
  }
}
