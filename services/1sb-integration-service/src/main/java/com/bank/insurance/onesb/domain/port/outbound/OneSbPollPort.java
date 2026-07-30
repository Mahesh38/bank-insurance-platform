package com.bank.insurance.onesb.domain.port.outbound;

/**
 * Minimal outbound port for polling 1SB async job status.
 * Implemented by {@code adapter.onesb.polling.OneSbHttpClientPollAdapter} via {@code OneSbHttpClient}.
 */
public interface OneSbPollPort {

    /**
     * GET the given relative poll path against 1SB.
     *
     * @param path relative path including leading slash (e.g. {@code /insurance/lifeterm/v1/quote/poll/REQ})
     * @return poll outcome; never null
     */
    PollResult poll(String path);

    /**
     * @param complete    true when upstream reports poll complete ({@code isPollComplete})
     * @param httpStatus  HTTP status from upstream (0 if transport failure)
     * @param errorMessage optional error detail
     */
    record PollResult(boolean complete, int httpStatus, String errorMessage) {
        public static PollResult of(boolean complete, int httpStatus) {
            return new PollResult(complete, httpStatus, null);
        }

        public static PollResult transportError(String message) {
            return new PollResult(false, 0, message);
        }
    }
}
