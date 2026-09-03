package com.bank.insurance.onesb.lob.life;

import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.SubmitProposalCommand;
import com.bank.insurance.onesb.lob.life.payload.LifeProposalSubmitBody;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared Life proposal path helpers and typed submit-body factory (DRY for Term/Saving/ULIP).
 */
public final class LifeProposalSupport {

    private LifeProposalSupport() {}

    public static String schemaPath(String basePath, String productId, String manufacturerId, String version) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(basePath);
        if (StringUtils.hasText(productId)) {
            builder.queryParam("productId", productId);
        }
        if (StringUtils.hasText(manufacturerId)) {
            builder.queryParam("manufacturerId", manufacturerId);
        }
        if (StringUtils.hasText(version)) {
            builder.queryParam("version", version);
        }
        return builder.build().encode().toUriString();
    }

    public static String pollPath(String pollPathPrefix, String externalReqId) {
        return pollPathPrefix + externalReqId;
    }

    public static LifeProposalSubmitBody buildSubmitBody(
            SubmitProposalCommand command, SecretProvider secretProvider) {
        Map<String, Object> formFields = new LinkedHashMap<>();
        if (command.values() != null) {
            formFields.putAll(command.values());
        }
        // Never trust client distributor identity (COMP-004 / D7)
        formFields.remove("distributorId");
        formFields.remove("distributorID");
        formFields.remove("distributor");

        LifeProposalSubmitBody.Distributor distributor = new LifeProposalSubmitBody.Distributor(
                secretProvider.getDistributorId(),
                resolveAgentId(command),
                resolveChannelType(command)
        );

        return new LifeProposalSubmitBody(
                distributor,
                blankToNull(command.productCode()),
                blankToNull(command.manufacturerId()),
                blankToNull(command.version()),
                blankToNull(command.offerId()),
                blankToNull(command.schemaId()),
                formFields
        );
    }

    public static String resolveAgentId(SubmitProposalCommand command) {
        if (StringUtils.hasText(command.agentId())) {
            return command.agentId().trim();
        }
        if (command.distribution() != null && StringUtils.hasText(command.distribution().agentId())) {
            return command.distribution().agentId().trim();
        }
        return "";
    }

    private static String resolveChannelType(SubmitProposalCommand command) {
        if (command.distribution() != null
                && StringUtils.hasText(command.distribution().channelType())) {
            return command.distribution().channelType().trim();
        }
        return "B2B";
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
