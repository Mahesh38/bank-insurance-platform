package com.bank.common.test.fixtures;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class DomainFixturesTest {

    @Test
    void identifiers_arePrefixedAndUnique() {
        assertThat(DomainFixtures.journeyId()).hasSize(36);
        assertThat(DomainFixtures.journeyId()).isNotEqualTo(DomainFixtures.journeyId());
        assertThat(DomainFixtures.idempotencyKey("create")).startsWith("create-");
        assertThat(DomainFixtures.actorId()).startsWith("actor-");
        assertThat(DomainFixtures.jobId()).hasSize(36);
    }
}
