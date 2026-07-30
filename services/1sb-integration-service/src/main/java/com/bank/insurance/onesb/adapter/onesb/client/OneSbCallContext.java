package com.bank.insurance.onesb.adapter.onesb.client;

/**
 * Thread-local call context so outbound 1SB HTTP can associate REQ/RES capture with a job.
 * Set in QuoteService / ProposalService create paths (or tests); always {@link #clear()} in finally.
 */
public final class OneSbCallContext {

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private OneSbCallContext() {}

    public static void set(String jobId, String lob) {
        HOLDER.set(new Context(jobId, lob));
    }

    public static Context get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public record Context(String jobId, String lob) {
        public boolean hasJobId() {
            return jobId != null && !jobId.isBlank();
        }
    }
}
