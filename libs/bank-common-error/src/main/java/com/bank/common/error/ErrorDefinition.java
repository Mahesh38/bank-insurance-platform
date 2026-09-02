package com.bank.common.error;

import java.util.Objects;

/**
 * Everything the platform decides <em>once</em> about one error code.
 *
 * <p>The root cause this closes: today a code's status, wording, retryability and audit behaviour
 * are re-decided at every throw site, so the same condition produces three different responses in
 * three services. A throw site supplies only what the registry cannot know — the identifiers, the
 * developer reason, and the cause. It never re-states the status, the wording or the retryability.
 *
 * <p>{@code publicTitle} and {@code publicDetail} are the <strong>only</strong> text that may reach
 * an end user. They are fixed strings, so they cannot accidentally carry an upstream body, an
 * internal route or a customer attribute.
 *
 * @param code          the stable wire code, matching {@link ErrorCodes}
 * @param category      how callers must treat it
 * @param httpStatus    the status this code always returns
 * @param retryability  whether and when a repeat can succeed
 * @param publicTitle   safe, end-user-facing summary
 * @param publicDetail  safe, end-user-facing explanation
 * @param audit         what evidence the refusal must leave
 * @param propagation   how a calling service re-presents it
 * @param runbook       the L1/L2 support page id, {@code RB-<CODE>}
 * @param catalogueRef  the section of catalogue 04 this came from
 */
public record ErrorDefinition(
        String code,
        ErrorCategory category,
        int httpStatus,
        Retryability retryability,
        String publicTitle,
        String publicDetail,
        AuditDisposition audit,
        Propagation propagation,
        String runbook,
        String catalogueRef
) {

    public ErrorDefinition {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(publicTitle, "publicTitle must not be null");
        Objects.requireNonNull(publicDetail, "publicDetail must not be null");
        retryability = retryability != null ? retryability : category.defaultRetryability();
        audit = audit != null ? audit : category.defaultAudit();
        propagation = propagation != null ? propagation : Propagation.PROPAGATE;
        httpStatus = httpStatus > 0 ? httpStatus : category.defaultHttpStatus();
        runbook = runbook != null ? runbook : "RB-" + code;
    }

    /** True when a sustained rate of this error is an operational signal. */
    public boolean alertable() {
        return category.alertable();
    }
}
