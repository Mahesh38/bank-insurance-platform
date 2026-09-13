package com.bank.insurance.onesb.api;

import com.bank.common.error.PlatformErrorAutoConfiguration;
import com.bank.insurance.onesb.domain.model.ProductUiData;
import com.bank.insurance.onesb.domain.port.inbound.ProductUiDataUseCase;
import com.bank.insurance.onesb.domain.port.outbound.IdempotencyPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("FUNC-027")
@WebMvcTest(controllers = ProductUiDataController.class)
@Import(PlatformErrorAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductUiDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductUiDataUseCase productUiDataUseCase;

    @MockBean
    private IdempotencyPort idempotencyPort;

    @Test
    void getProductUiData_returnsPassThroughPayload() throws Exception {
        when(productUiDataUseCase.getProductUiData("345", "BALIC"))
                .thenReturn(new ProductUiData("345", "BALIC", "REQ-UI", Map.of("screens", "ok")));

        mockMvc.perform(get("/v1/products/ui-data")
                        .param("productId", "345")
                        .param("manufacturerId", "BALIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is("345")))
                .andExpect(jsonPath("$.manufacturerId", is("BALIC")))
                .andExpect(jsonPath("$.reqId", is("REQ-UI")))
                .andExpect(jsonPath("$.data.screens", is("ok")));

        verify(productUiDataUseCase).getProductUiData("345", "BALIC");
    }

    @Test
    void getProductUiData_missingProductId_returns400() throws Exception {
        mockMvc.perform(get("/v1/products/ui-data")
                        .param("manufacturerId", "BALIC"))
                .andExpect(status().isBadRequest());
    }
}
