package com.bank.insurance.onesb.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformErrorAutoConfiguration;
import com.bank.insurance.onesb.domain.model.UlipFundResult;
import com.bank.insurance.onesb.domain.port.inbound.UlipFundUseCase;
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

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("FUNC-027")
@WebMvcTest(controllers = UlipFundController.class)
@Import(PlatformErrorAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class UlipFundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UlipFundUseCase ulipFundUseCase;

    @MockBean
    private IdempotencyPort idempotencyPort;

    @Test
    void listFunds_valid_returnsData() throws Exception {
        when(ulipFundUseCase.listFunds(any()))
                .thenReturn(new UlipFundResult("REQ-L", Map.of("funds", "ok")));

        mockMvc.perform(post("/v1/ulip/funds/list")
                        .header("Idempotency-Key", "idem-1")
                        .header("X-Actor-Id", "rm-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reqId", is("REQ-L")))
                .andExpect(jsonPath("$.data.funds", is("ok")));

        verify(ulipFundUseCase).listFunds(any());
        verify(ulipFundUseCase, never()).fundPerformance(any());
    }

    @Test
    void listFunds_missingMembers_returns422_noUseCase() throws Exception {
        mockMvc.perform(post("/v1/ulip/funds/list")
                        .header("Idempotency-Key", "idem-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "lob": "ULIP", "sumAssured": 500000, "members": [] }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is(ErrorCodes.VALIDATION_ERROR)));

        verify(ulipFundUseCase, never()).listFunds(any());
    }

    @Test
    void fundPerformance_valid_returnsData() throws Exception {
        when(ulipFundUseCase.fundPerformance(any()))
                .thenReturn(new UlipFundResult("REQ-P", Map.of("performance", "ok")));

        mockMvc.perform(post("/v1/ulip/funds/performance")
                        .header("Idempotency-Key", "idem-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reqId", is("REQ-P")));

        verify(ulipFundUseCase).fundPerformance(any());
    }

    private static String validBody(boolean pin) {
        String selection = pin
                ? """
                  ,
                  "selection": { "insurerCode": "BALIC", "productCodes": ["345"] }
                  """
                : "";
        return """
                {
                  "lob": "ULIP",
                  "sumAssured": 500000,
                  "members": [{ "dob": "1990-01-15", "gender": "M" }],
                  "distribution": { "agentId": "109337" }%s
                }
                """.formatted(selection);
    }
}
