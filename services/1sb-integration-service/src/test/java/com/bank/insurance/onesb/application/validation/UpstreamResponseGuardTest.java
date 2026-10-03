package com.bank.insurance.onesb.application.validation;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.TestErrors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("FUNC-028")
@Tag("unit")
class UpstreamResponseGuardTest {

    @Test
    void requireJson_acceptsObject() {
        assertThat(UpstreamResponseGuard.requireJson(
                "{\"reqId\":\"R1\"}", MediaType.APPLICATION_JSON, "quote", TestErrors.ONESB)
                .path("reqId").asText()).isEqualTo("R1");
    }

    @Test
    void requireJson_rejectsHtmlAndEmpty() {
        assertThatThrownBy(() -> UpstreamResponseGuard.requireJson(
                "<html><body>load balancer</body></html>",
                MediaType.TEXT_HTML, "poll", TestErrors.ONESB))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.UPSTREAM_BAD_RESPONSE));

        assertThatThrownBy(() -> UpstreamResponseGuard.requireJson(
                "   ", MediaType.APPLICATION_JSON, "poll", TestErrors.ONESB))
                .isInstanceOf(ServiceException.class);
    }
}
