package com.bank.insurance.onesb.api;

import com.bank.insurance.onesb.api.dto.CreateQuoteRequest;
import com.bank.insurance.onesb.api.dto.UlipFundResponse;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.domain.model.UlipFundResult;
import com.bank.insurance.onesb.domain.port.inbound.UlipFundUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bank ULIP fund helpers — {@code POST /v1/ulip/funds/list} and
 * {@code POST /v1/ulip/funds/performance} (FUNC-027).
 * Idempotency-Key required via {@code IdempotencyFilter}.
 */
@RestController
@RequestMapping("/v1/ulip/funds")
public class UlipFundController {

    public static final String ACTOR_HEADER = "X-Actor-Id";
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final UlipFundUseCase ulipFundUseCase;

    public UlipFundController(UlipFundUseCase ulipFundUseCase) {
        this.ulipFundUseCase = ulipFundUseCase;
    }

    @PostMapping("/list")
    public ResponseEntity<UlipFundResponse> listFunds(
            @Valid @RequestBody CreateQuoteRequest request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actorId) {
        CreateQuoteCommand command = QuoteCommandMapper.toCommand(request, idempotencyKey, actorId);
        return ResponseEntity.ok(toResponse(ulipFundUseCase.listFunds(command)));
    }

    @PostMapping("/performance")
    public ResponseEntity<UlipFundResponse> fundPerformance(
            @Valid @RequestBody CreateQuoteRequest request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actorId) {
        CreateQuoteCommand command = QuoteCommandMapper.toCommand(request, idempotencyKey, actorId);
        return ResponseEntity.ok(toResponse(ulipFundUseCase.fundPerformance(command)));
    }

    static UlipFundResponse toResponse(UlipFundResult result) {
        return new UlipFundResponse(result.reqId(), result.data());
    }
}
