package com.bank.common.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class MdcContextTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void with_setsMdcKeys_duringExecution() {
        AtomicReference<String> capturedJobId = new AtomicReference<>();
        AtomicReference<String> capturedLob   = new AtomicReference<>();

        MdcContext.with(
            Map.of(MdcKeys.JOB_ID, "job-001", MdcKeys.LOB, "TERM"),
            () -> {
                capturedJobId.set(MDC.get(MdcKeys.JOB_ID));
                capturedLob.set(MDC.get(MdcKeys.LOB));
            }
        );

        assertThat(capturedJobId.get()).isEqualTo("job-001");
        assertThat(capturedLob.get()).isEqualTo("TERM");
    }

    @Test
    void with_clearsMdcKeys_afterExecution() {
        MdcContext.with(
            Map.of(MdcKeys.JOB_ID, "job-002"),
            () -> {}
        );

        assertThat(MDC.get(MdcKeys.JOB_ID)).isNull();
    }

    @Test
    void with_clearsMdcKeys_evenOnException() {
        assertThatThrownBy(() ->
            MdcContext.with(
                Map.of(MdcKeys.JOB_ID, "job-003"),
                () -> { throw new RuntimeException("boom"); }
            )
        ).isInstanceOf(RuntimeException.class);

        assertThat(MDC.get(MdcKeys.JOB_ID)).isNull();
    }

    @Test
    void supply_returnsValue_andCleansUp() {
        String result = MdcContext.supply(
            Map.of(MdcKeys.ACTOR_ID, "EMP001"),
            () -> MDC.get(MdcKeys.ACTOR_ID) + "-done"
        );

        assertThat(result).isEqualTo("EMP001-done");
        assertThat(MDC.get(MdcKeys.ACTOR_ID)).isNull();
    }

    @Test
    void with_restoresPreviousValue_afterExecution() {
        MDC.put(MdcKeys.JOB_ID, "outer-job");

        MdcContext.with(
            Map.of(MdcKeys.JOB_ID, "inner-job"),
            () -> assertThat(MDC.get(MdcKeys.JOB_ID)).isEqualTo("inner-job")
        );

        assertThat(MDC.get(MdcKeys.JOB_ID)).isEqualTo("outer-job");
    }

    @Test
    void put_doesNothing_whenValueIsNull() {
        MdcContext.put(MdcKeys.LOB, null);
        assertThat(MDC.get(MdcKeys.LOB)).isNull();
    }

    @Test
    void put_setsValue_whenNonNull() {
        MdcContext.put(MdcKeys.LOB, "HEALTH");
        assertThat(MDC.get(MdcKeys.LOB)).isEqualTo("HEALTH");
    }

    @Test
    void with_nullContextValue_removesKeyForDuration() {
        MDC.put(MdcKeys.JOB_ID, "pre-existing");
        Map<String, String> context = new HashMap<>();
        context.put(MdcKeys.JOB_ID, null);

        MdcContext.with(context, () -> assertThat(MDC.get(MdcKeys.JOB_ID)).isNull());

        assertThat(MDC.get(MdcKeys.JOB_ID)).isEqualTo("pre-existing");
    }

    @Test
    void clear_removesSpecifiedKeys() {
        MDC.put(MdcKeys.JOB_ID, "j1");
        MDC.put(MdcKeys.LOB,    "TERM");

        MdcContext.clear(MdcKeys.JOB_ID);

        assertThat(MDC.get(MdcKeys.JOB_ID)).isNull();
        assertThat(MDC.get(MdcKeys.LOB)).isEqualTo("TERM");
    }
}
