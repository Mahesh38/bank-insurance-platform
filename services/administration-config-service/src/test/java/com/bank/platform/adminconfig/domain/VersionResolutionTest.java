package com.bank.platform.adminconfig.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.rules.engine.RuleSet;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VersionResolutionTest {

  private static RuleSetVersion published(
      String version, String lob, String pc, String from, String to, String approvedAt) {
    return RuleSetVersion.restore(
        new RuleSetVersion.Snapshot(
            Ulid.mint(),
            "X",
            version,
            "D",
            lob,
            pc,
            RuleSet.Classification.OPERATIONAL,
            RuleSetVersionStatus.PUBLISHED,
            "{}",
            "h",
            OffsetDateTime.parse(from),
            to == null ? null : OffsetDateTime.parse(to),
            "m",
            "c",
            Instant.parse(approvedAt),
            null,
            Instant.parse(approvedAt)));
  }

  private static final OffsetDateTime AT = OffsetDateTime.parse("2026-12-01T00:00:00Z");

  @Test
  void specificScopeBeatsWildcardAndLaterStartWins() {
    var wildcard =
        published("1.0.0", null, null, "2026-01-01T00:00:00Z", null, "2026-01-01T00:00:00Z");
    var term =
        published("1.0.1", "LIFE", "TERM", "2026-01-01T00:00:00Z", null, "2026-01-01T00:00:00Z");
    var later =
        published("1.0.2", "LIFE", "TERM", "2026-06-01T00:00:00Z", null, "2026-01-01T00:00:00Z");
    var ended =
        published(
            "1.0.3",
            "LIFE",
            "TERM",
            "2026-07-01T00:00:00Z",
            "2026-08-01T00:00:00Z",
            "2026-01-01T00:00:00Z");
    var otherClass =
        published("1.0.4", "LIFE", "ULIP", "2026-09-01T00:00:00Z", null, "2026-01-01T00:00:00Z");
    List<RuleSetVersion> all = List.of(wildcard, term, later, ended, otherClass);
    assertThat(VersionResolution.inForce(all, "LIFE", "TERM", AT)).contains(later);
    assertThat(VersionResolution.inForce(all, "LIFE", "SAVINGS", AT)).contains(wildcard);
    assertThat(
            VersionResolution.inForce(
                all, "LIFE", "TERM", OffsetDateTime.parse("2025-01-01T00:00:00Z")))
        .isEmpty();
  }

  @Test
  void lifecycleGuards() {
    var v = published("1.0.0", null, null, "2026-01-01T00:00:00Z", null, "2026-01-01T00:00:00Z");
    assertThatThrownBy(v::submit).isInstanceOf(RuleSetLifecycleException.class);
    assertThatThrownBy(() -> v.approve("z", Set.of("CONFIG_APPROVER"), null, Instant.now()))
        .isInstanceOf(RuleSetLifecycleException.class)
        .extracting(e -> ((RuleSetLifecycleException) e).kind())
        .isEqualTo(RuleSetLifecycleException.Kind.ILLEGAL_TRANSITION);
    assertThat(Ulid.isValid(v.id())).isTrue();
    assertThat(Ulid.isValid("short")).isFalse();
  }
}
