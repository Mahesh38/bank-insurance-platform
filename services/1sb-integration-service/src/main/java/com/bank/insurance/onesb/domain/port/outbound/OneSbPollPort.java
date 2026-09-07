package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.common.domain.QuoteOffer;

import java.util.List;

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
     * @param complete            true when upstream reports poll complete ({@code isPollComplete})
     * @param httpStatus          HTTP status from upstream (0 if transport failure)
     * @param errorMessage        optional error detail
     * @param offers              normalised offers when complete (empty for generic poll adapters)
     * @param applicationNumber   insurer application number when present (proposal polls)
     */
    record PollResult(boolean complete, int httpStatus, String errorMessage,
                      List<QuoteOffer> offers, String applicationNumber) {

        public PollResult(boolean complete, int httpStatus, String errorMessage) {
            this(complete, httpStatus, errorMessage, List.of(), null);
        }

        public PollResult(boolean complete, int httpStatus, String errorMessage, List<QuoteOffer> offers) {
            this(complete, httpStatus, errorMessage, offers, null);
        }

        public static PollResult of(boolean complete, int httpStatus) {
            return new PollResult(complete, httpStatus, null, List.of(), null);
        }

        public static PollResult of(boolean complete, int httpStatus, List<QuoteOffer> offers) {
            return new PollResult(complete, httpStatus, null,
                    offers != null ? List.copyOf(offers) : List.of(), null);
        }

        public static PollResult of(boolean complete, int httpStatus, List<QuoteOffer> offers,
                                    String applicationNumber) {
            return new PollResult(complete, httpStatus, null,
                    offers != null ? List.copyOf(offers) : List.of(), applicationNumber);
        }

        public static PollResult transportError(String message) {
            return new PollResult(false, 0, message, List.of(), null);
        }
    }
}
