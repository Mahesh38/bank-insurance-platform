package com.bank.platform.consent.domain;

import java.util.Optional;

/** Insert-only statement versions. */
public interface StatementStore {

  void insert(PublishedStatement statement);

  Optional<PublishedStatement> find(String statementId, String version, String language);

  boolean englishSourceExists(String statementId, String version);
}
