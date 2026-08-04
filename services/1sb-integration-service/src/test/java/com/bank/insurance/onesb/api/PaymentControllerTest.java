package com.bank.insurance.onesb.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.PaymentSession;
import com.bank.insurance.onesb.domain.model.PaymentStatus;
import com.bank.insurance.onesb.domain.port.inbound.PaymentUseCase;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("FUNC-007")
@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentUseCase paymentUseCase;

    @MockBean
    private IdempotencyPort idempotencyPort;

    private static final String VALID_BODY = """
            {
              "lob": "TERM",
              "applicationNumber": "APP-123",
              "insurerCode": "HDFC",
              "productCode": "T1",
              "redirectUrl": "https://bank.com/return",
              "journeyId": "j-1",
              "payer": {
                "firstName": "Anil",
                "lastName": "Kumar",
                "mobileNumber": "9876543210",
                "email": "anil@example.com"
              },
              "amountToBePaid": 12300,
              "premiumFrequency": "YEARLY",
              "distribution": { "rmEmployeeId": "E123", "agentId": "AGT-1" }
            }
            """;

    @Test
    void createPaymentSession_valid_returns201WithSessionRefOnly() throws Exception {
        when(paymentUseCase.createPaymentSession(any())).thenReturn(new PaymentSession(
                "sess-1", null, "APP-123", Lob.TERM,
                "https://pay.example.com/abc", "https://bank.com/return",
                PaymentStatus.AWAITING_PAYMENT, "TXN-1",
                Instant.parse("2026-08-04T10:00:00Z"), Instant.parse("2026-08-04T11:00:00Z")));

        mockMvc.perform(post("/v1/payments")
                        .header("Idempotency-Key", "idem-1")
                        .header("X-Actor-Id", "rm-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentSessionId", is("sess-1")))
                .andExpect(jsonPath("$.paymentUrl", is("https://pay.example.com/abc")))
                .andExpect(jsonPath("$.status", is("AWAITING_PAYMENT")));
    }

    @Test
    void createPaymentSession_missingPayer_returns422_noUseCaseCall() throws Exception {
        mockMvc.perform(post("/v1/payments")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lob": "TERM",
                                  "applicationNumber": "APP-123",
                                  "insurerCode": "HDFC",
                                  "productCode": "T1",
                                  "redirectUrl": "https://bank.com/return",
                                  "amountToBePaid": 12300,
                                  "premiumFrequency": "YEARLY"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is(ErrorCodes.VALIDATION_ERROR)));

        verify(paymentUseCase, never()).createPaymentSession(any());
    }

    @Test
    void createPaymentSession_notPayable_returns409() throws Exception {
        when(paymentUseCase.createPaymentSession(any())).thenThrow(new ServiceException(
                ServiceErrorResponse.builder()
                        .title("Proposal Not Payable")
                        .status(409)
                        .detail("Proposal job is not payable")
                        .code(ErrorCodes.PROPOSAL_NOT_PAYABLE)
                        .retryable(false)
                        .build()));

        mockMvc.perform(post("/v1/payments")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code", is(ErrorCodes.PROPOSAL_NOT_PAYABLE)));
    }
}
