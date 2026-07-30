package com.bank.insurance.onesb.lob.life.term;

import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.lob.LobQuoteHandler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Term LOB quote handler — maps bank {@link CreateQuoteCommand} to a minimal
 * 1SB Multi-Quote body for {@code POST /insurance/lifeterm/v1/quote}.
 * <p>
 * Poll path: {@code GET /insurance/lifeterm/v1/quote/poll/{reqId}}.
 */
@Component
public class TermQuoteHandler implements LobQuoteHandler {

    static final String SUBMIT_PATH = "/insurance/lifeterm/v1/quote";
    static final String POLL_PATH_PREFIX = "/insurance/lifeterm/v1/quote/poll/";

    private final SecretProvider secretProvider;

    public TermQuoteHandler(SecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    @Override
    public Lob supportedLob() {
        return Lob.TERM;
    }

    @Override
    public Object buildSubmitPayload(CreateQuoteCommand command) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("typeOfQuote", "Multi-Quote");
        root.put("quoteCategory", "Sum Assured");
        root.put("includeBI", "withoutBI");
        root.put("outOfBoundConfig", "Yes");

        Map<String, Object> additionalSetup = new LinkedHashMap<>();
        additionalSetup.put("currency", "INR");
        additionalSetup.put("userCountry", "IN");
        root.put("additionalSetup", additionalSetup);

        Map<String, Object> distributor = new LinkedHashMap<>();
        distributor.put("distributorID", secretProvider.getDistributorId());
        distributor.put("agentID", resolveAgentId(command));
        distributor.put("channelType", resolveChannelType(command));
        root.put("distributor", distributor);

        List<Map<String, Object>> individuals = new ArrayList<>();
        List<CreateQuoteCommand.MemberDetail> members =
                command.members() != null ? command.members() : List.of();
        int seq = 1;
        for (CreateQuoteCommand.MemberDetail member : members) {
            Map<String, Object> ind = new LinkedHashMap<>();
            ind.put("memberType", member.role() != null ? mapMemberType(member.role()) : "Life Assured");
            ind.put("memberSequenceNumber", member.sequenceNumber() > 0 ? member.sequenceNumber() : seq);
            ind.put("gender", mapGender(member.gender()));
            ind.put("dateOfBirth", member.dob());
            ind.put("tobacco", member.tobacco() ? "Yes" : "No");
            if (member.annualIncome() != null) {
                ind.put("annualIncome", member.annualIncome());
            }
            if (member.pincode() != null && !member.pincode().isBlank()) {
                ind.put("zipCode", member.pincode());
            }
            if (command.sumAssured() != null) {
                ind.put("quoteAmount", command.sumAssured());
            }
            individuals.add(ind);
            seq++;
        }
        Map<String, Object> personalInformation = new LinkedHashMap<>();
        personalInformation.put("individualDetails", individuals);
        root.put("personalInformation", personalInformation);

        Map<String, Object> product = new LinkedHashMap<>();
        product.put("product", "LifeTerm");
        root.put("product", product);

        return root;
    }

    @Override
    public String submitPath() {
        return SUBMIT_PATH;
    }

    @Override
    public String pollPath(String externalReqId) {
        return POLL_PATH_PREFIX + externalReqId;
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
}
