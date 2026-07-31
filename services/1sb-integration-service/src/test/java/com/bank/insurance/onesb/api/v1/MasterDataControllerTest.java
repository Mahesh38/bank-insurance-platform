package com.bank.insurance.onesb.api.v1;

import com.bank.common.error.ErrorCodes;
import com.bank.insurance.onesb.api.GlobalExceptionHandler;
import com.bank.insurance.onesb.application.MasterDataService;
import com.bank.insurance.onesb.application.MasterLookupOutcome;
import com.bank.insurance.onesb.domain.model.LookupValue;
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

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@Tag("FUNC-001")
@WebMvcTest(controllers = MasterDataController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MasterDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MasterDataService masterDataService;

    @MockBean
    private IdempotencyPort idempotencyPort;

    @Test
    void lookup_200_normalised_withCacheHeaderMiss() throws Exception {
        when(masterDataService.lookup(anyString(), anyString(), anyList(), isNull()))
                .thenReturn(new MasterLookupOutcome(
                        "TERM",
                        Map.of("GENDER", List.of(LookupValue.of("M"), LookupValue.of("F"))),
                        false,
                        false));

        mockMvc.perform(post("/v1/master-data/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lob": "TERM",
                                  "lookUpCategory": "quote",
                                  "entityIds": ["GENDER"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(MasterDataController.CACHE_HEADER, "MISS"))
                .andExpect(jsonPath("$.lob", is("TERM")))
                .andExpect(jsonPath("$.lookups.GENDER[0].code", is("M")))
                .andExpect(jsonPath("$.lookups.GENDER[0].label", is("M")))
                .andExpect(jsonPath("$.lookups.GENDER[1].code", is("F")))
                .andExpect(jsonPath("$.cache.hit", is(false)))
                .andExpect(jsonPath("$.cache.stale", is(false)));
    }

    @Test
    void lookup_missingLob_returns422_validationError_withoutCallingService() throws Exception {
        mockMvc.perform(post("/v1/master-data/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entityIds": ["GENDER"]
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code", is(ErrorCodes.VALIDATION_ERROR)))
                .andExpect(jsonPath("$.status", is(422)));

        verify(masterDataService, never()).lookup(any(), any(), any(), any());
    }

    @Test
    void lookup_emptyEntityIds_returns422() throws Exception {
        mockMvc.perform(post("/v1/master-data/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lob": "TERM",
                                  "entityIds": []
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is(ErrorCodes.VALIDATION_ERROR)));

        verify(masterDataService, never()).lookup(any(), any(), any(), any());
    }

    @Test
    void lookup_staleOutcome_setsStaleHeader() throws Exception {
        when(masterDataService.lookup(anyString(), anyString(), anyList(), any()))
                .thenReturn(new MasterLookupOutcome(
                        "TERM",
                        Map.of("OCC", List.of(new LookupValue("SALARIED", "SALARIED"))),
                        false,
                        true));

        mockMvc.perform(post("/v1/master-data/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lob": "TERM",
                                  "entityIds": ["OCC"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(MasterDataController.CACHE_HEADER, "STALE"))
                .andExpect(jsonPath("$.cache.stale", is(true)));
    }
}
