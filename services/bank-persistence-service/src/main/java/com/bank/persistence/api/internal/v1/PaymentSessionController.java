package com.bank.persistence.api.internal.v1;

import com.bank.persistence.api.internal.v1.dto.CreatePaymentSessionRequest;
import com.bank.persistence.api.internal.v1.dto.PaymentSessionResponse;
import com.bank.persistence.entity.PaymentSessionEntity;
import com.bank.persistence.repo.PaymentSessionRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/payment-sessions")
public class PaymentSessionController {

    private final PaymentSessionRepository paymentSessionRepository;

    public PaymentSessionController(PaymentSessionRepository paymentSessionRepository) {
        this.paymentSessionRepository = paymentSessionRepository;
    }

    @PostMapping
    public ResponseEntity<PaymentSessionResponse> create(
            @Valid @RequestBody CreatePaymentSessionRequest request) {
        Instant now = Instant.now();
        PaymentSessionEntity entity = new PaymentSessionEntity();
        entity.setSessionId(request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString());
        entity.setJobId(request.jobId());
        entity.setApplicationNumber(request.applicationNumber());
        entity.setLob(request.lob());
        entity.setPaymentUrl(request.paymentUrl());
        entity.setRedirectUrl(request.redirectUrl());
        entity.setStatus(request.status());
        entity.setExternalTxnId(request.externalTxnId());
        entity.setCreatedAt(now);
        entity.setExpiresAt(request.expiresAt());
        entity.setUpdatedAt(now);
        entity.setCreatedByActor(request.createdByActor());

        PaymentSessionEntity saved = paymentSessionRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping("/{sessionId}")
    public PaymentSessionResponse get(@PathVariable String sessionId) {
        return paymentSessionRepository.findById(sessionId)
                .map(PaymentSessionController::toResponse)
                .orElseThrow(() -> NotFound.of("Payment session", sessionId));
    }

    static PaymentSessionResponse toResponse(PaymentSessionEntity e) {
        return new PaymentSessionResponse(
                e.getSessionId(),
                e.getJobId(),
                e.getApplicationNumber(),
                e.getLob(),
                e.getPaymentUrl(),
                e.getRedirectUrl(),
                e.getStatus(),
                e.getExternalTxnId(),
                e.getCreatedAt(),
                e.getExpiresAt(),
                e.getUpdatedAt(),
                e.getCreatedByActor()
        );
    }
}
