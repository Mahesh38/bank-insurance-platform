package com.bank.insurance.onesb.api;

import com.bank.insurance.onesb.api.dto.CreatePaymentRequest;
import com.bank.insurance.onesb.api.dto.CreatePaymentResponse;
import com.bank.insurance.onesb.domain.command.CreatePaymentCommand;
import com.bank.insurance.onesb.domain.model.PaymentSession;
import com.bank.insurance.onesb.domain.port.inbound.PaymentUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bank payment API — {@code POST /v1/payments} (FUNC-007).
 * Idempotency-Key required via {@code IdempotencyFilter}.
 */
@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    public static final String ACTOR_HEADER = "X-Actor-Id";
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final PaymentUseCase paymentUseCase;

    public PaymentController(PaymentUseCase paymentUseCase) {
        this.paymentUseCase = paymentUseCase;
    }

    @PostMapping
    public ResponseEntity<CreatePaymentResponse> createPaymentSession(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actorId) {

        CreatePaymentCommand command = toCommand(request, actorId);
        PaymentSession session = paymentUseCase.createPaymentSession(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(session));
    }

    static CreatePaymentResponse toResponse(PaymentSession session) {
        return new CreatePaymentResponse(
                session.sessionId(),
                session.paymentUrl(),
                session.expiresAt(),
                session.status()
        );
    }

    private static CreatePaymentCommand toCommand(CreatePaymentRequest request, String actorId) {
        CreatePaymentCommand.Payer payer = request.payer() != null
                ? new CreatePaymentCommand.Payer(
                        request.payer().firstName(),
                        request.payer().lastName(),
                        request.payer().mobileNumber(),
                        request.payer().email())
                : null;
        String rmEmployeeId = request.distribution() != null ? request.distribution().rmEmployeeId() : null;
        String agentId = request.distribution() != null ? request.distribution().agentId() : null;

        return new CreatePaymentCommand(
                request.lob(),
                request.applicationNumber(),
                request.jobId(),
                request.insurerCode(),
                request.productCode(),
                request.redirectUrl(),
                request.journeyId(),
                request.currency(),
                payer,
                request.amountToBePaid(),
                request.premiumFrequency(),
                rmEmployeeId,
                agentId,
                StringUtils.hasText(actorId) ? actorId : "system"
        );
    }
}
