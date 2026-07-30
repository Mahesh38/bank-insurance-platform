package com.bank.common.observability;

import org.slf4j.MDC;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Helper for safely managing SLF4J {@link MDC} context within a request or task scope.
 *
 * <p>Use {@link #with(Map, Runnable)} or {@link #supply(Map, Supplier)} to ensure
 * MDC entries are always cleaned up, even on exception.
 *
 * <p>Example:
 * <pre>{@code
 * MdcContext.with(
 *     Map.of(MdcKeys.JOB_ID, jobId, MdcKeys.LOB, lob),
 *     () -> processJob(jobId)
 * );
 * }</pre>
 */
public final class MdcContext {

    private MdcContext() {}

    /**
     * Puts a single key-value pair into MDC.
     * Caller is responsible for cleanup via {@link #clear(String...)}.
     */
    public static void put(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }

    /**
     * Removes the specified keys from MDC.
     */
    public static void clear(String... keys) {
        Arrays.stream(keys).forEach(MDC::remove);
    }

    /**
     * Runs {@code action} with the given MDC entries set, then restores the
     * previous MDC state (including removal of any keys added by this call).
     *
     * @param context entries to add to MDC for the duration of the action
     * @param action  the code to execute
     */
    public static void with(Map<String, String> context, Runnable action) {
        Map<String, String> previous = saveAndSet(context);
        try {
            action.run();
        } finally {
            restore(context, previous);
        }
    }

    /**
     * Same as {@link #with(Map, Runnable)} but returns a value.
     */
    public static <T> T supply(Map<String, String> context, Supplier<T> supplier) {
        Map<String, String> previous = saveAndSet(context);
        try {
            return supplier.get();
        } finally {
            restore(context, previous);
        }
    }

    private static Map<String, String> saveAndSet(Map<String, String> context) {
        Map<String, String> previous = new HashMap<>();
        context.forEach((k, v) -> {
            previous.put(k, MDC.get(k));
            if (v != null) {
                MDC.put(k, v);
            } else {
                MDC.remove(k);
            }
        });
        return previous;
    }

    private static void restore(Map<String, String> context, Map<String, String> previous) {
        context.keySet().forEach(k -> {
            String prev = previous.get(k);
            if (prev != null) {
                MDC.put(k, prev);
            } else {
                MDC.remove(k);
            }
        });
    }
}
