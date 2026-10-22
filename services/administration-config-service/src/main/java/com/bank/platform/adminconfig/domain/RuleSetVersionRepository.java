package com.bank.platform.adminconfig.domain;

import java.util.List;
import java.util.Optional;

/** Store of {@link RuleSetVersion}s, schema {@code administration}. */
public interface RuleSetVersionRepository {

  /**
   * Inserts a new version; a duplicate (ruleSetId, version) throws {@link
   * DuplicateVersionException}.
   */
  void insert(RuleSetVersion version);

  /**
   * Writes a status / document change. Refuses (with {@link RuleSetLifecycleException}) when the
   * stored row is no longer in {@code expectedStatus} — a concurrent decision — or is PUBLISHED.
   */
  void update(RuleSetVersion version, RuleSetVersionStatus expectedStatus);

  Optional<RuleSetVersion> find(String ruleSetId, String version);

  List<RuleSetVersion> listVersions(String ruleSetId);

  List<RuleSetVersion> listPublished(String domain);

  /** The (ruleSetId, version) pair already exists. */
  class DuplicateVersionException extends RuntimeException {
    public DuplicateVersionException(String message) {
      super(message);
    }
  }
}
