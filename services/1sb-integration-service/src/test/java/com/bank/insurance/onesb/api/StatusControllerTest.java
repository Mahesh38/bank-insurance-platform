package com.bank.insurance.onesb.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformErrorAutoConfiguration;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import com.bank.common.domain.ApplicationStatus;
import com.bank.common.domain.BankApplicationStatus;
import com.bank.insurance.onesb.domain.port.inbound.StatusUseCase;
import com.bank.insurance.onesb.domain.port.outbound.IdempotencyPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("FUNC-009")
@WebMvcTest(controllers = StatusController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PlatformErrorAutoConfiguration.class)
class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatusUseCase statusUseCase;

    @MockBean
    private IdempotencyPort idempotencyPort;

    @Test
    void getStatus_happyPath_returnsBankStatus_omitsRawStatus() throws Exception {
        when(statusUseCase.getStatus(eq("APP-1"), any(), eq("HDFC"), eq("T1"), any()))
                .thenReturn(new ApplicationStatus(
                        "APP-1", BankApplicationStatus.UW_REQUIREMENTS, "REQUIREMENTS_PENDING",
                        "Income proof pending", null, Instant.parse("2026-08-04T10:00:00Z")));

        mockMvc.perform(get("/v1/status/APP-1")
                        .param("lob", "TERM")
                        .param("insurerCode", "HDFC")
                        .param("productCode", "T1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationNumber", is("APP-1")))
                .andExpect(jsonPath("$.bankStatus", is("UW_REQUIREMENTS")))
                .andExpect(jsonPath("$.manufacturerSubStatus", is("Income proof pending")))
                .andExpect(jsonPath("$.rawStatus").doesNotExist());
    }

    @Test
    void getStatus_notFound_returns404() throws Exception {
        when(statusUseCase.getStatus(eq("APP-missing"), any(), eq("HDFC"), any(), any()))
                .thenThrow(new ServiceException(ServiceErrorResponse.builder()
                        .title("Not Found")
                        .status(404)
                        .detail("No status found")
                        .code(ErrorCodes.RESOURCE_NOT_FOUND)
                        .retryable(false)
                        .build()));

        mockMvc.perform(get("/v1/status/APP-missing")
                        .param("lob", "TERM")
                        .param("insurerCode", "HDFC"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code", is(ErrorCodes.RESOURCE_NOT_FOUND)));
    }

    @Test
    void getStatus_missingInsurerCode_returns400() throws Exception {
        mockMvc.perform(get("/v1/status/APP-1")
                        .param("lob", "TERM"))
                .andExpect(status().isBadRequest());
    }
}
