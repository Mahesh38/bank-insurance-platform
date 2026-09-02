package com.bank.common.error;

/**
 * Writes the engineer-facing half of a failure — the log line and the metric.
 *
 * <p>Separated from {@link PlatformErrorHandler} because they answer to different people and change
 * for different reasons: the handler owns the HTTP contract and changes when the API does, while
 * recording owns what operations can see and changes when a dashboard or a log index does. Folding
 * both into one class also made recording untestable without standing up a handler and an
 * exception.
 *
 * <p>Implementations must not throw. A failure to record a failure must never replace the response
 * the caller was owed — losing a log line is recoverable, losing the error response is not.
 */
@FunctionalInterface
public interface ErrorRecorder {

    /** Records one failure. {@code cause} is null when there is no exception behind it. */
    void record(ServiceErrorResponse response, Throwable cause);

    /** Records nothing. The default when a service has no logging or metrics wired yet. */
    ErrorRecorder NONE = (response, cause) -> { };
}
