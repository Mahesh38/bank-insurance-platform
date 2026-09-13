package com.bank.insurance.onesb.api;

import com.bank.insurance.onesb.api.dto.CreateQuoteRequest;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Shared bank {@link CreateQuoteRequest} → {@link CreateQuoteCommand} mapping.
 */
public final class QuoteCommandMapper {

    private QuoteCommandMapper() {}

    public static CreateQuoteCommand toCommand(CreateQuoteRequest request,
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

        CreateQuoteCommand.ProductSelection selection = null;
        if (request.selection() != null) {
            selection = new CreateQuoteCommand.ProductSelection(
                    request.selection().insurerCode(),
                    request.selection().productCodes(),
                    request.selection().planOption(),
                    request.selection().coverOption(),
                    request.selection().deathBenefitOption(),
                    request.selection().policyTerm(),
                    request.selection().premiumPaymentTerm(),
                    request.selection().premiumFrequency(),
                    request.selection().premiumPaymentOption()
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
                StringUtils.hasText(actorId) ? actorId : "system",
                selection
        );
    }
}
