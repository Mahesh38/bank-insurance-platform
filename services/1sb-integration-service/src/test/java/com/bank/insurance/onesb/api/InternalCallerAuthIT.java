package com.bank.insurance.onesb.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.common.security.internal.InternalCallerAuth;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class InternalCallerAuthIT {

  @Autowired MockMvc mockMvc;

  @Test
  void v1WithoutInternalKeyIsUnauthorized() throws Exception {
    mockMvc
        .perform(
            get("/v1/status/APP-1").header(InternalCallerAuth.HEADER, "not-a-configured-key-xxx"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void paymentWithEvilRedirectIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/v1/payments")
                .header(InternalCallerAuth.HEADER, "local-onesb-test-internal-key")
                .header("Idempotency-Key", "evil-redirect-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "lob": "TERM",
                      "applicationNumber": "APP-123",
                      "insurerCode": "HDFC",
                      "productCode": "T1",
                      "redirectUrl": "https://evil.example/phish",
                      "currency": "INR",
                      "amountToBePaid": 1000,
                      "premiumFrequency": "YEARLY",
                      "payer": {
                        "firstName": "A",
                        "lastName": "B",
                        "mobileNumber": "9876543210",
                        "email": "a@b.com"
                      }
                    }
                    """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void actuatorHealthNeedsNoKey() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
