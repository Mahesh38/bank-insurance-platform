package com.bank.insurance.onesb.api;

import com.bank.insurance.onesb.api.dto.CreateQuoteRequest;
import com.bank.insurance.onesb.api.dto.CreateQuoteResponse;
import com.bank.insurance.onesb.api.dto.QuoteJobResponse;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.QuoteJob;
import com.bank.insurance.onesb.domain.port.inbound.QuoteUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bank quote API — {@code POST /v1/quotes} (FUNC-002) and thin {@code GET} for poll demos.
 */
@RestController
@RequestMapping("/v1/quotes")
public class QuoteController {

    public static final String ACTOR_HEADER = "X-Actor-Id";
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final QuoteUseCase quoteUseCase;

    public QuoteController(QuoteUseCase quoteUseCase) {
        this.quoteUseCase = quoteUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateQuoteResponse> createQuote(
            @Valid @RequestBody CreateQuoteRequest request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actorId) {

        CreateQuoteCommand command = toCommand(request, idempotencyKey, actorId);
        String jobId = quoteUseCase.createQuote(command);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new CreateQuoteResponse(jobId, JobStatus.PENDING));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<QuoteJobResponse> getQuote(@PathVariable String jobId) {
        QuoteJob job = quoteUseCase.getQuoteResult(jobId);
        return ResponseEntity.ok(toResponse(job));
    }

    private static CreateQuoteCommand toCommand(CreateQuoteRequest request,
                                                String idempotencyKey,
                                                String actorId) {
        List<CreateQuoteCommand.MemberDetail> members = request.members().stream()
                .map(m -> new CreateQuoteCommand.MemberDetail(
                        m.role(),
                        m.sequenceNumber() != null ? m.sequenceNumber() : 0,
                        m.dob(),
                        m.gender(),
                        Boolean.TRUE.equals(m.tobacco()),
                        m.annualIncome(),
                        m.pincode()
                ))
                .toList();

        CreateQuoteCommand.DistributionContext distribution = null;
        if (request.distribution() != null) {
            distribution = new CreateQuoteCommand.DistributionContext(
                    request.distribution().rmEmployeeId(),
                    request.distribution().agentId(),
                    request.distribution().channelType()
            );
        }

        return new CreateQuoteCommand(
                request.lob(),
                request.mode(),
                request.category(),
                request.sumAssured(),
                request.premiumAmount(),
                members,
                request.preferences(),
                distribution,
                request.journeyId(),
                request.sessionId(),
                idempotencyKey,
                StringUtils.hasText(actorId) ? actorId : "system"
        );
    }

    private static QuoteJobResponse toResponse(QuoteJob job) {
        return new QuoteJobResponse(
                job.jobId(),
                job.status(),
                job.lob(),
                job.journeyId(),
                job.offers(),
                job.partialErrors(),
                job.createdAt(),
                job.completedAt()
        );
    }
}
