package com.bank.insurance.onesb.lob.life;

import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.lob.life.payload.LifeQuoteRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared mapping from bank {@link CreateQuoteCommand} to typed {@link LifeQuoteRequest}.
 * LOB handlers supply product family token, optional savings filters, and 1SB paths (DRY).
 */
public final class LifeQuotePayloadFactory {

    private LifeQuotePayloadFactory() {}

    public static LifeQuoteRequest build(
            CreateQuoteCommand command,
            SecretProvider secrets,
            LifeQuoteRequest.Product product) {
        List<LifeQuoteRequest.IndividualDetail> individuals = new ArrayList<>();
        List<CreateQuoteCommand.MemberDetail> members =
                command.members() != null ? command.members() : List.of();
        int seq = 1;
        for (CreateQuoteCommand.MemberDetail member : members) {
            individuals.add(new LifeQuoteRequest.IndividualDetail(
                    member.role() != null ? mapMemberType(member.role()) : "Life Assured",
                    member.sequenceNumber() > 0 ? member.sequenceNumber() : seq,
                    mapGender(member.gender()),
                    member.dob(),
                    member.tobacco() ? "Yes" : "No",
                    member.annualIncome(),
                    blankToNull(member.pincode()),
                    command.sumAssured()
            ));
            seq++;
        }

        return new LifeQuoteRequest(
                "Multi-Quote",
                "Sum Assured",
                "withoutBI",
                "Yes",
                new LifeQuoteRequest.AdditionalSetup("INR", "IN"),
                new LifeQuoteRequest.Distributor(
                        secrets.getDistributorId(),
                        resolveAgentId(command),
                        resolveChannelType(command)
                ),
                new LifeQuoteRequest.PersonalInformation(List.copyOf(individuals)),
                product
        );
    }

    private static String resolveAgentId(CreateQuoteCommand command) {
        if (command.distribution() == null) {
            return "";
        }
        if (command.distribution().agentId() != null && !command.distribution().agentId().isBlank()) {
            return command.distribution().agentId();
        }
        return command.distribution().rmEmployeeId() != null ? command.distribution().rmEmployeeId() : "";
    }

    private static String resolveChannelType(CreateQuoteCommand command) {
        if (command.distribution() != null
                && command.distribution().channelType() != null
                && !command.distribution().channelType().isBlank()) {
            return command.distribution().channelType();
        }
        return "B2B";
    }

    private static String mapGender(String gender) {
        if (gender == null) {
            return "Male";
        }
        return switch (gender.trim().toUpperCase()) {
            case "F", "FEMALE" -> "Female";
            default -> "Male";
        };
    }

    private static String mapMemberType(String role) {
        if (role == null) {
            return "Life Assured";
        }
        return switch (role.trim().toUpperCase()) {
            case "PROPOSER" -> "Proposer";
            default -> "Life Assured";
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
