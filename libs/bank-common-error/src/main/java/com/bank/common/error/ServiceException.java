package com.bank.common.error;

import java.util.List;

/**
 * Unchecked exception carrying a {@link ServiceErrorResponse} for propagation
 * from service/domain layers up to controller/exception-handler advice.
 *
 * <p>Usage example:
 * <pre>{@code
 * throw ServiceException.upstreamAuth("1SB returned 401 – check API key");
 * }</pre>
 */
public class ServiceException extends RuntimeException {

    private final ServiceErrorResponse errorResponse;

    public ServiceException(ServiceErrorResponse errorResponse) {
        super(errorResponse.getDetail());
        this.errorResponse = errorResponse;
    }

    public ServiceException(ServiceErrorResponse errorResponse, Throwable cause) {
        super(errorResponse.getDetail(), cause);
        this.errorResponse = errorResponse;
    }

    public ServiceErrorResponse getErrorResponse() {
        return errorResponse;
    }

    public int getHttpStatus() {
        return errorResponse.getStatus();
    }

    public boolean isRetryable() {
        return errorResponse.isRetryable();
    }

    // --- Factory shortcuts ---

    public static ServiceException validation(String detail, List<ServiceError> errors) {
        return new ServiceException(ServiceErrorResponse.validation(detail, errors));
    }

    public static ServiceException upstreamBusiness(String detail, String upstreamCode) {
        return new ServiceException(ServiceErrorResponse.upstreamBusiness(detail, upstreamCode));
    }

    public static ServiceException upstreamUnavailable(String detail, Throwable cause) {
        return new ServiceException(ServiceErrorResponse.upstreamUnavailable(detail), cause);
    }

    public static ServiceException upstreamAuth(String detail) {
        return new ServiceException(
            ServiceErrorResponse.builder()
                .title("Upstream Authentication Failure")
                .status(502)
                .detail(detail)
                .code(ErrorCodes.UPSTREAM_AUTH_FAILURE)
                .retryable(false)
                .build()
        );
    }

    public static ServiceException unauthorized() {
        return new ServiceException(ServiceErrorResponse.unauthorized());
    }

    public static ServiceException forbidden() {
        return new ServiceException(ServiceErrorResponse.forbidden());
    }

    public static ServiceException internal(String detail, Throwable cause) {
        return new ServiceException(ServiceErrorResponse.internalError(detail), cause);
    }
}
