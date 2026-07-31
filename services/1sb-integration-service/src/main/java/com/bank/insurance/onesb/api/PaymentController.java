package com.bank.insurance.onesb.api;

import com.bank.insurance.onesb.api.dto.CreatePaymentRequest;
import com.bank.insurance.onesb.api.dto.CreatePaymentResponse;
import com.bank.insurance.onesb.config.PaymentProperties;
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
 * Idempotency-Key required on POST via {@code IdempotencyFilter}.
 */
@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    public static final String ACTOR_HEADER = "X-Actor-Id";
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final PaymentUseCase paymentUseCase;
    private final PaymentProperties paymentProperties;

    public PaymentController(PaymentUseCase paymentUseCase, PaymentProperties paymentProperties) {
        this.paymentUseCase = paymentUseCase;
        this.paymentProperties = paymentProperties;
    }

    @PostMapping
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actorId) {

        CreatePaymentCommand command = toCommand(request, idempotencyKey, actorId);
        PaymentSession session = paymentUseCase.createPayment(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreatePaymentResponse(
                        session.sessionId(),
                        session.paymentUrl(),
                        session.expiresAt()));
    }

    private CreatePaymentCommand toCommand(CreatePaymentRequest request,
                                           String idempotencyKey,
                                           String actorId) {
        String redirectUrl = StringUtils.hasText(request.redirectUrl())
                ? request.redirectUrl()
                : paymentProperties.defaultRedirectUrl();
        return new CreatePaymentCommand(
                request.applicationNumber(),
                request.proposalJobId(),
                redirectUrl,
                request.lob(),
                request.journeyId(),
                idempotencyKey,
                StringUtils.hasText(actorId) ? actorId : "system"
        );
    }
}
