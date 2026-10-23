package com.bank.platform.customer.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("FUNC-031")
class CustomerSearchControllerTest {

  @Autowired MockMvc mockMvc;

  @Test
  void searchWithoutActorIsRefused() throws Exception {
    mockMvc
        .perform(get("/internal/v1/customers/search").param("by", "PAN").param("q", "ABCDE1234F"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("MISSING_REQUIRED_FIELD"))
        .andExpect(jsonPath("$.errors[0].field").value("X-Actor-Id"));
  }

  @Test
  void getWithoutActorIsRefused() throws Exception {
    mockMvc
        .perform(get("/internal/v1/customers/CUST-3210"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("MISSING_REQUIRED_FIELD"));
  }

  @Test
  void noMatchIsTwoHundredWithEmptyItems() throws Exception {
    mockMvc
        .perform(
            get("/internal/v1/customers/search")
                .header("X-Actor-Id", "rm-1")
                .param("by", "CUSTOMER_ID")
                .param("q", "NONE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty());
  }

  @Test
  void malformedPanIsAValidationErrorOnQ() throws Exception {
    mockMvc
        .perform(
            get("/internal/v1/customers/search")
                .header("X-Actor-Id", "rm-1")
                .param("by", "PAN")
                .param("q", "NOTAPAN"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("q"));
  }

  @Test
  void validSearchReturnsMaskedHit() throws Exception {
    mockMvc
        .perform(
            get("/internal/v1/customers/search")
                .header("X-Actor-Id", "rm-1")
                .param("by", "MOBILE")
                .param("q", "9876543210")
                .param("countryCode", "IN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].maskedMobile").value("XXXXXX3210"));
  }
}
