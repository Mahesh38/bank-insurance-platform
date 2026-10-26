package com.bank.platform.payment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentApplicationTest {

  private final MockMvc mockMvc;

  @Autowired
  PaymentApplicationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void contextLoads() {}

  @Test
  void infoReportsTheSkeleton() throws Exception {
    mockMvc
        .perform(get("/internal/v1/payment/info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.service").value("payment-service"))
        .andExpect(jsonPath("$.status").value("SKELETON"));
  }
}
