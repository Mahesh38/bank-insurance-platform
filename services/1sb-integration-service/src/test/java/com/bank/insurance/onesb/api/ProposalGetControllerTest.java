package com.bank.insurance.onesb.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformErrorAutoConfiguration;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import com.bank.common.domain.JobStatus;
import com.bank.common.domain.Lob;
import com.bank.common.domain.QuoteJob;
import com.bank.insurance.onesb.domain.port.inbound.ProposalUseCase;
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
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FUNC-006: GET /v1/proposals/{jobId} — status + applicationNumber, 404, no fabricated applicationNo.
 */
@Tag("FUNC-006")
@WebMvcTest(controllers = ProposalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PlatformErrorAutoConfiguration.class)
class ProposalGetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProposalUseCase proposalUseCase;

    @MockBean
    private IdempotencyPort idempotencyPort;

    @Test
    void ac1_completed_returnsStatusAndApplicationNumber() throws Exception {
        when(proposalUseCase.getProposalResult("job-done")).thenReturn(new QuoteJob(
                "job-done", JobStatus.COMPLETED, null, Lob.TERM, "j-1",
                List.of(), List.of(),
                Instant.parse("2026-07-30T12:00:00Z"),
                Instant.parse("2026-07-30T12:01:00Z"),
                "APP-123"
        ));

        mockMvc.perform(get("/v1/proposals/job-done"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId", is("job-done")))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.applicationNumber", is("APP-123")))
                .andExpect(jsonPath("$.offers", hasSize(0)))
                .andExpect(jsonPath("$.failureReason").doesNotExist());
    }

    @Test
    void ac3_inProgress_returnsStatusWithoutApplicationNumber() throws Exception {
        when(proposalUseCase.getProposalResult("job-run")).thenReturn(new QuoteJob(
                "job-run", JobStatus.RUNNING, null, Lob.TERM, "j-1",
                List.of(), List.of(),
                Instant.parse("2026-07-30T12:00:00Z"), null, null
        ));

        mockMvc.perform(get("/v1/proposals/job-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId", is("job-run")))
                .andExpect(jsonPath("$.status", is("RUNNING")))
                .andExpect(jsonPath("$.applicationNumber").doesNotExist())
                .andExpect(jsonPath("$.offers", hasSize(0)));
    }

    @Test
    void ac3_pending_neverFabricatesApplicationNumber_evenIfStoreHadOne() throws Exception {
        when(proposalUseCase.getProposalResult("job-pend")).thenReturn(new QuoteJob(
                "job-pend", JobStatus.PENDING, null, Lob.TERM, "j-1",
                List.of(), List.of(),
                Instant.parse("2026-07-30T12:00:00Z"), null, "SHOULD-NOT-APPEAR"
        ));

        mockMvc.perform(get("/v1/proposals/job-pend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.applicationNumber").doesNotExist())
                .andExpect(jsonPath("$.offers", hasSize(0)));
    }

    @Test
    void ac2_unknownJobId_returns404_resourceNotFound() throws Exception {
        when(proposalUseCase.getProposalResult("missing")).thenThrow(new ServiceException(
                ServiceErrorResponse.builder()
                        .title("Not Found")
                        .status(404)
                        .detail("Proposal job not found: missing")
                        .code(ErrorCodes.RESOURCE_NOT_FOUND)
                        .retryable(false)
                        .build()));

        mockMvc.perform(get("/v1/proposals/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code", is(ErrorCodes.RESOURCE_NOT_FOUND)));
    }

    @Test
    void timeout_returns200WithFailureReason_noApplicationNumber() throws Exception {
        when(proposalUseCase.getProposalResult("job-to")).thenReturn(new QuoteJob(
                "job-to", JobStatus.TIMEOUT, "POLL_TIMEOUT", Lob.TERM, "j-1",
                List.of(), List.of(),
                Instant.parse("2026-07-30T12:00:00Z"),
                Instant.parse("2026-07-30T12:05:00Z"),
                null
        ));

        mockMvc.perform(get("/v1/proposals/job-to"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId", is("job-to")))
                .andExpect(jsonPath("$.status", is("TIMEOUT")))
                .andExpect(jsonPath("$.failureReason", is("POLL_TIMEOUT")))
                .andExpect(jsonPath("$.applicationNumber").doesNotExist())
                .andExpect(jsonPath("$.offers", hasSize(0)));
    }
}
