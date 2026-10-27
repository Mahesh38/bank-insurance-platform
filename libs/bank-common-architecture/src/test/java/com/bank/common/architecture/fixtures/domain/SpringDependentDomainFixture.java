package com.bank.common.architecture.fixtures.domain;

import org.springframework.util.StringUtils;

/** Violation fixture — domain depending on Spring. */
public final class SpringDependentDomainFixture {
  public boolean hasText(String value) {
    return StringUtils.hasText(value);
  }
}
