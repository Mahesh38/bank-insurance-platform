package com.bank.common.error;

import com.bank.common.observability.ErrorMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/** ERR-002 / ERR-004 — the paths a happy-path test never reaches. */
class PlatformErrorHandlerEdgeCasesTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private final class Handler extends PlatformErrorHandler {
        Handler(Boundary boundary, ErrorMetrics metrics) {
            super("onesb", PlatformLayer.L5, boundary, metrics);
        }
        @Override protected int validationStatus() { return 422; }
    }

    private Handler handler() {
        return new Handler(PlatformErrorHandler.Boundary.INTERNAL, new ErrorMetrics(registry));
    }

    @SuppressWarnings("unused")
    private void target(String field) {}

    /** A bean with a real property, so {@code rejectValue} produces a field error not a global one. */
    public static final class QuoteRequest {
        private String lob;
        public String getLob() { return lob; }
        public void setLob(String lob) { this.lob = lob; }
    }

    private MethodArgumentNotValidException beanValidationFailure(boolean withMessage) throws Exception {
        BindingResult binding = new BeanPropertyBindingResult(new QuoteRequest(), "request");
        binding.rejectValue("lob", "code", withMessage ? "lob is required" : null);
        MethodParameter parameter = new MethodParameter(
            getClass().getDeclaredMethod("target", String.class), 0);
        return new MethodArgumentNotValidException(parameter, binding);
    }

    @Test
    void beanValidationFailuresBecomeFieldErrors() throws Exception {
        var body = handler().handleValidation(beanValidationFailure(true)).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(422);
        assertThat(body.getCode()).isEqualTo(ErrorCodes.VALIDATION_ERROR);
        assertThat(body.getCategory()).isEqualTo(ErrorCategory.VALIDATION);
        assertThat(body.getService()).isEqualTo("onesb");
        assertThat(body.getIncidentId()).isNotNull();
        assertThat(body.getErrors()).isNotEmpty();
    }

    @Test
    void aFieldErrorWithNoMessageStillProducesAnError() throws Exception {
        var body = handler().handleValidation(beanValidationFailure(false)).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getErrors()).allSatisfy(e -> assertThat(e.message()).isNotBlank());
    }

    @Test
    void anUnparseableBodyNeverEchoesTheParserMessage() {
        var body = handler().handleUnreadable(
            new HttpMessageNotReadableException(
                "Unexpected character at offset 42 in com.bank.QuoteRequest", null, null)).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(ErrorCodes.INVALID_REQUEST);
        assertThat(body.getDetail())
            .as("the parser names offsets and classes; that is a reason, not a detail")
            .doesNotContain("offset")
            .doesNotContain("com.bank");
        assertThat(body.getDiagnostic().getCauseChain()).isNotEmpty();
    }

    @Test
    void aResponseWithNoCategoryOrDiagnosticStillLogsAndCounts() {
        ServiceException bare = new ServiceException(ServiceErrorResponse.builder()
            .title("Odd").status(500).code(ErrorCodes.INTERNAL_ERROR).build());

        var body = handler().handleServiceException(bare).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getService()).isEqualTo("onesb");
        assertThat(registry.find(ErrorMetrics.ERROR_COUNT).counters()).isNotEmpty();
    }

    @Test
    void aPlatformFailureWithACauseIsLoggedWithIt() {
        ServiceException withCause = ServiceException.of(ErrorCodes.UPSTREAM_UNAVAILABLE)
            .service("onesb")
            .layer(PlatformLayer.L5)
            .cause(new IllegalStateException("connection reset"))
            .build();

        var body = handler().handleServiceException(withCause).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getCategory()).isEqualTo(ErrorCategory.UPSTREAM);
        assertThat(registry.find(ErrorMetrics.ERROR_COUNT)
            .tag("code", ErrorCodes.UPSTREAM_UNAVAILABLE).counter()).isNotNull();
    }

    @Test
    void aHandlerWithNoMetricsStillRespondsAndLogs() {
        var body = new Handler(PlatformErrorHandler.Boundary.INTERNAL, null)
            .handleServiceException(ServiceException.of(ErrorCodes.CONFLICT)
                .service("onesb").layer(PlatformLayer.L5).build())
            .getBody();

        assertThat(body)
            .as("a service must not lose its error responses because it cannot count them")
            .isNotNull();
        assertThat(body.getStatus()).isEqualTo(409);
    }

    @Test
    void theMetricsFactoryToleratesAnAbsentRegistry() {
        assertThat(PlatformErrorHandler.errorMetrics(null)).isNull();
        assertThat(PlatformErrorHandler.errorMetrics(new EmptyProvider<>())).isNull();
        assertThat(PlatformErrorHandler.errorMetrics(new PresentProvider<>(registry))).isNotNull();
    }

    @Test
    void identityStampsNeverOverwriteWhatIsAlreadyThere() {
        ServiceErrorResponse original = ServiceErrorResponse.builder()
            .title("t").status(409).code(ErrorCodes.CONFLICT)
            .service("persistence").incidentId("ID-1").correlationId("C-1")
            .build();

        assertThat(original.withService("onesb").getService()).isEqualTo("persistence");
        assertThat(original.withIncidentId("ID-2").getIncidentId()).isEqualTo("ID-1");
        assertThat(original.withCorrelationId("C-2").getCorrelationId()).isEqualTo("C-1");

        // ...and a null argument is a no-op rather than an erasure.
        assertThat(original.withService(null)).isSameAs(original);
        assertThat(original.withIncidentId(null)).isSameAs(original);
        assertThat(original.withCorrelationId(null)).isSameAs(original);
    }

    @Test
    void identityStampsFillWhatIsMissing() {
        ServiceErrorResponse bare = ServiceErrorResponse.builder()
            .title("t").status(409).code(ErrorCodes.CONFLICT).build();

        assertThat(bare.withService("onesb").getService()).isEqualTo("onesb");
        assertThat(bare.withIncidentId("ID-9").getIncidentId()).isEqualTo("ID-9");
        assertThat(bare.withCorrelationId("C-9").getCorrelationId()).isEqualTo("C-9");
    }

    @Test
    void aStatusOverrideMustNameTheDocumentThatRatifiedIt() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ServiceException.of(ErrorCodes.QUOTE_EXPIRED).statusOverride(410, null))
            .withMessageContaining("ratified");
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ServiceException.of(ErrorCodes.QUOTE_EXPIRED).statusOverride(410, "  "));
    }

    @Test
    void aStatusOverrideIsRecordedInTheDiagnostic() {
        ServiceException withReason = ServiceException.of(ErrorCodes.QUOTE_EXPIRED)
            .service("onesb")
            .reason("quote job is TIMEOUT")
            .statusOverride(410, "FUNC-004 AC-2")
            .build();

        assertThat(withReason.getHttpStatus()).isEqualTo(410);
        assertThat(withReason.getDiagnostic().getReason())
            .contains("quote job is TIMEOUT")
            .contains("status overridden to 410 per FUNC-004 AC-2");

        ServiceException withoutReason = ServiceException.of(ErrorCodes.QUOTE_EXPIRED)
            .statusOverride(410, "FUNC-004 AC-2")
            .build();

        assertThat(withoutReason.getDiagnostic().getReason())
            .isEqualTo("status overridden to 410 per FUNC-004 AC-2");
    }

    private static final class EmptyProvider<T> implements ObjectProvider<T> {
        @Override public T getObject() { throw new UnsupportedOperationException(); }
        @Override public T getObject(Object... args) { throw new UnsupportedOperationException(); }
        @Override public T getIfAvailable() { return null; }
        @Override public T getIfUnique() { return null; }
        @Override public Iterator<T> iterator() { return List.<T>of().iterator(); }
    }

    private static final class PresentProvider<T> implements ObjectProvider<T> {
        private final T value;
        PresentProvider(T value) { this.value = value; }
        @Override public T getObject() { return value; }
        @Override public T getObject(Object... args) { return value; }
        @Override public T getIfAvailable() { return value; }
        @Override public T getIfUnique() { return value; }
        @Override public Iterator<T> iterator() { return List.of(value).iterator(); }
    }
}
