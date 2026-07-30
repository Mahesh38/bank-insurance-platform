package com.bank.insurance.onesb.api;

import com.bank.insurance.onesb.api.dto.SubmitProposalRequest;
import com.bank.insurance.onesb.api.dto.SubmitProposalResponse;
import com.bank.insurance.onesb.domain.command.SubmitProposalCommand;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.ProposalSchema;
import com.bank.insurance.onesb.domain.model.ProposalSubmitResult;
import com.bank.insurance.onesb.domain.port.inbound.ProposalUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bank proposal API — {@code GET /v1/proposals/schema} (FUNC-004),
 * {@code POST /v1/proposals} (FUNC-005).
 * Idempotency-Key required on POST via {@code IdempotencyFilter}.
 */
@RestController
@RequestMapping("/v1/proposals")
public class ProposalController {

    public static final String ACTOR_HEADER = "X-Actor-Id";
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final ProposalUseCase proposalUseCase;

    public ProposalController(ProposalUseCase proposalUseCase) {
        this.proposalUseCase = proposalUseCase;
    }

    @GetMapping("/schema")
    public ResponseEntity<ProposalSchema> getSchema(
            @RequestParam Lob lob,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String manufacturerId,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String quoteJobId) {

        ProposalSchema schema = proposalUseCase.getSchema(
                lob, productCode, manufacturerId, version, quoteJobId);
        return ResponseEntity.ok(schema);
    }

    @PostMapping
    public ResponseEntity<SubmitProposalResponse> submit(
            @Valid @RequestBody SubmitProposalRequest request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actorId) {

        SubmitProposalCommand command = toCommand(request, idempotencyKey, actorId);
        ProposalSubmitResult result = proposalUseCase.submit(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SubmitProposalResponse(result.proposalJobId(), result.status()));
    }

    private static SubmitProposalCommand toCommand(SubmitProposalRequest request,
                                                   String idempotencyKey,
                                                   String actorId) {
        SubmitProposalCommand.DistributionContext distribution = null;
        if (request.distribution() != null) {
            distribution = new SubmitProposalCommand.DistributionContext(
                    request.distribution().rmEmployeeId(),
                    request.distribution().agentId(),
                    request.distribution().channelType()
            );
        }
        return new SubmitProposalCommand(
                request.lob(),
                request.schemaId(),
                request.offerId(),
                request.productCode(),
                request.manufacturerId(),
                request.version(),
                request.values(),
                request.consentRef(),
                request.agentId(),
                distribution,
                request.journeyId(),
                request.sessionId(),
                idempotencyKey,
                StringUtils.hasText(actorId) ? actorId : "system"
        );
    }
}
