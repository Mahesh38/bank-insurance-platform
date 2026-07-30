package com.bank.insurance.onesb.adapter.onesb.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("COMP-003")
class OneSbCallContextTest {

    @AfterEach
    void tearDown() {
        OneSbCallContext.clear();
    }

    @Test
    void setGetClear_roundTrip() {
        assertThat(OneSbCallContext.get()).isNull();

        OneSbCallContext.set("job-1", "TERM");
        assertThat(OneSbCallContext.get().jobId()).isEqualTo("job-1");
        assertThat(OneSbCallContext.get().lob()).isEqualTo("TERM");
        assertThat(OneSbCallContext.get().hasJobId()).isTrue();

        OneSbCallContext.clear();
        assertThat(OneSbCallContext.get()).isNull();
    }
}
